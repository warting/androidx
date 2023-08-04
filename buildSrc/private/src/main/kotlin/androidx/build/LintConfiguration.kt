/*
 * Copyright 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.build

import com.android.build.api.dsl.Lint
import com.android.build.gradle.AppPlugin
import com.android.build.gradle.BaseExtension
import com.android.build.gradle.LibraryPlugin
import com.android.build.gradle.internal.lint.AndroidLintAnalysisTask
import com.android.build.gradle.internal.lint.AndroidLintTask
import com.android.build.gradle.internal.lint.LintModelWriterTask
import com.android.build.gradle.internal.lint.VariantInputs
import java.io.File
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.kotlin.dsl.findByType
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.plugin.KotlinBasePlugin
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.tooling.core.withClosure

/**
 * Single entry point to Android Lint configuration.
 */
fun Project.configureLint() {
    project.plugins.all { plugin ->
        when (plugin) {
            is AppPlugin -> configureAndroidProjectForLint(isLibrary = false)
            is LibraryPlugin -> configureAndroidProjectForLint(isLibrary = true)
            // Only configure non-multiplatform Java projects via JavaPlugin. Multiplatform
            // projects targeting Java (e.g. `jvm { withJava() }`) are configured via
            // KotlinBasePlugin.
            is JavaPlugin -> if (project.multiplatformExtension == null) {
                configureNonAndroidProjectForLint()
            }
            // Only configure non-Android multiplatform projects via KotlinBasePlugin.
            // Multiplatform projects targeting Android (e.g. `id("com.android.library")`) are
            // configured via AppPlugin or LibraryPlugin.
            is KotlinBasePlugin -> if (
                project.multiplatformExtension != null &&
                !project.plugins.hasPlugin(AppPlugin::class.java) &&
                !project.plugins.hasPlugin(LibraryPlugin::class.java)
            ) {
                configureNonAndroidProjectForLint()
            }
        }
    }
}

/**
 * Android Lint configuration entry point for Android projects.
 */
private fun Project.configureAndroidProjectForLint(
    isLibrary: Boolean
) = androidExtension.finalizeDsl { extension ->
    // The lintAnalyze task is used by `androidx-studio-integration-lint.sh`.
    tasks.register("lintAnalyze") { task -> task.enabled = false }

    configureLint(extension.lint, isLibrary)

    // We already run lintDebug, we don't need to run lint on the release variant.
    tasks.named("lint").configure { task -> task.enabled = false }

    afterEvaluate {
        registerLintDebugIfNeededAfterEvaluate()

        if (extension.buildFeatures.aidl == true) {
            configureLintForAidlAfterEvaluate()
        }
    }
}

/**
 * Android Lint configuration entry point for non-Android projects.
 */
private fun Project.configureNonAndroidProjectForLint() = afterEvaluate {
    // The lint plugin expects certain configurations and source sets which are only added by
    // the Java and Android plugins. If this is a multiplatform project targeting JVM, we'll
    // need to manually create these configurations and source sets based on their multiplatform
    // JVM equivalents.
    addSourceSetsForMultiplatformAfterEvaluate()

    // For Android projects, the Android Gradle Plugin is responsible for applying the lint plugin;
    // however, we need to apply it ourselves for non-Android projects.
    apply(mapOf("plugin" to "com.android.lint"))

    // Create task aliases matching those creates by AGP for Android projects, since those are what
    // developers expect to invoke. Redirect them to the "real" lint task.
    val lintTask = tasks.named("lint")
    tasks.register("lintDebug") {
        it.dependsOn(lintTask)
        it.enabled = false
    }
    tasks.register("lintRelease") {
        it.dependsOn(lintTask)
        it.enabled = false
    }

    // The lintAnalyzeDebug task is used by `androidx-studio-integration-lint.sh`.
    tasks.register("lintAnalyzeDebug") { it.enabled = false }

    addToBuildOnServer(lintTask)

    // For Android projects, we can run lint configuration last using `DslLifecycle.finalizeDsl`;
    // however, we need to run it using `Project.afterEvaluate` for non-Android projects.
    configureLint(project.extensions.getByType(), isLibrary = true)
}

/**
 * Registers the `lintDebug` task if there are debug variants present.
 *
 * This method *must* run after evaluation.
 */
private fun Project.registerLintDebugIfNeededAfterEvaluate() {
    val variantNames = project.agpVariants.map { it.name }
    if (!variantNames.contains("debug")) {
        tasks.register("lintDebug") { task ->
            // The lintDebug tasks depends on lint tasks for all debug variants.
            variantNames
                .filter { it.contains("debug", ignoreCase = true) }
                .map { tasks.named("lint${it.camelCase()}") }
                .forEach { task.dependsOn(it) }
        }
    }
}

private fun String.camelCase() =
    replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }

/**
 * If the project is targeting Android and using the AIDL build feature, installs AIDL source
 * directories on lint tasks.
 *
 * Adapted from AndroidXComposeImplPlugin's `configureLintForMultiplatformLibrary` extension
 * function. See b/189250111 for AGP feature request.
 *
 * The `UnstableAidlAnnotationDetector` check from `lint-checks` requires that _only_ unstable AIDL
 * files are passed to Lint, e.g. files in the AGP-defined `aidl` source set but not files in the
 * Stable AIDL plugin-defined `stableAidl` source set. If we decide to lint Stable AIDL files, we'll
 * need some other way to distinguish stable from unstable AIDL.
 *
 * This method *must* run after evaluation.
 */
private fun Project.configureLintForAidlAfterEvaluate() {
    // BaseExtension needed to access resolved source files on `aidl`.
    val extension = project.extensions.findByType<BaseExtension>() ?: return
    val mainAidl = extension.sourceSets.getByName("main").aidl.getSourceFiles()

    /** Helper function to add the missing sourcesets to this [VariantInputs] */
    fun VariantInputs.addSourceSets() {
        // Each variant has a source provider for the variant (such as debug) and the 'main'
        // variant. The actual files that Lint will run on is both of these providers
        // combined - so we can just add the dependencies to the first we see.
        val variantAidl = extension.sourceSets.getByName(name.get()).aidl.getSourceFiles()
        val sourceProvider = sourceProviders.get().firstOrNull() ?: return
        sourceProvider.javaDirectories.withChangesAllowed { from(mainAidl, variantAidl) }
    }

    // Lint for libraries is split into two tasks - analysis, and reporting. We need to
    // add the new sources to both, so all parts of the pipeline are aware.
    project.tasks.withType<AndroidLintAnalysisTask>().configureEach {
        it.variantInputs.addSourceSets()
    }

    project.tasks.withType<AndroidLintTask>().configureEach {
        it.variantInputs.addSourceSets()
    }

    // Also configure the model writing task, so that we don't run into mismatches between
    // analyzed sources in one module and a downstream module
    project.tasks.withType<LintModelWriterTask>().configureEach {
        it.variantInputs.addSourceSets()
    }
}

/**
 * If the project is using multiplatform, adds configurations and source sets expected by the lint
 * plugin, which allows it to configure itself when running against a non-Android multiplatform
 * project.
 *
 * The version of lint that we're using does not directly support Kotlin multiplatform, but we can
 * synthesize the necessary configurations and source sets from existing `jvm` configurations and
 * `kotlinSourceSets`, respectively.
 *
 * This method *must* run after evaluation.
 */
private fun Project.addSourceSetsForMultiplatformAfterEvaluate() {
    val kmpTargets = project.multiplatformExtension?.targets ?: return

    // Synthesize target configurations based on multiplatform configurations.
    val kmpApiElements = kmpTargets.map { it.apiElementsConfigurationName }
    val kmpRuntimeElements = kmpTargets.map { it.runtimeElementsConfigurationName }
    listOf(
        kmpRuntimeElements to "runtimeElements",
        kmpApiElements to "apiElements"
    ).forEach { (kmpConfigNames, targetConfigName) ->
        project.configurations.maybeCreate(targetConfigName).apply {
            kmpConfigNames
                .mapNotNull { configName -> project.configurations.findByName(configName) }
                .forEach { config -> extendsFrom(config) }
        }
    }

    // Synthesize source sets based on multiplatform source sets.
    val javaExtension = project.extensions.findByType(JavaPluginExtension::class.java)
        ?: throw GradleException("Failed to find extension of type 'JavaPluginExtension'")
    listOf(
        "main" to "main",
        "test" to "test"
    ).forEach { (kmpCompilationName, targetSourceSetName) ->
        javaExtension.sourceSets.maybeCreate(targetSourceSetName).apply {
            kmpTargets
                .mapNotNull { target -> target.compilations.findByName(kmpCompilationName) }
                .flatMap { compilation -> compilation.kotlinSourceSets }
                .flatMap { sourceSet -> sourceSet.kotlin.srcDirs }
                .forEach { srcDirs -> java.srcDirs += srcDirs }
        }
    }
}

/**
 * If the project is using multiplatform targeted to Android, adds source sets directly to lint
 * tasks, which allows it to run against Android multiplatform projects.
 *
 * Lint is not aware of MPP, and MPP doesn't configure Lint. There is no built-in API to adjust the
 * default Lint task's sources, so we use this hack to manually add sources for MPP source sets. In
 * the future, with the new Kotlin Project Model (https://youtrack.jetbrains.com/issue/KT-42572) and
 * an AGP / MPP integration plugin, this will no longer be needed. See also b/195329463.
 */
private fun Project.addSourceSetsForAndroidMultiplatformAfterEvaluate() {
    val multiplatformExtension = project.multiplatformExtension ?: return
    multiplatformExtension.targets.findByName("android") ?: return

    val androidMain = multiplatformExtension.sourceSets.findByName("androidMain")
        ?: throw GradleException("Failed to find source set with name 'androidMain'")

    // Get all the source sets androidMain transitively / directly depends on.
    val dependencySourceSets = androidMain.withClosure(KotlinSourceSet::dependsOn)

    /**
     * Helper function to add the missing sourcesets to this [VariantInputs]
     */
    fun VariantInputs.addSourceSets() {
        // Each variant has a source provider for the variant (such as debug) and the 'main'
        // variant. The actual files that Lint will run on is both of these providers
        // combined - so we can just add the dependencies to the first we see.
        val sourceProvider = sourceProviders.get().firstOrNull() ?: return
        dependencySourceSets.forEach { sourceSet ->
            sourceProvider.javaDirectories.withChangesAllowed {
                from(sourceSet.kotlin.sourceDirectories)
            }
        }
    }

    // Lint for libraries is split into two tasks - analysis, and reporting. We need to
    // add the new sources to both, so all parts of the pipeline are aware.
    project.tasks.withType<AndroidLintAnalysisTask>().configureEach {
        it.variantInputs.addSourceSets()
    }

    project.tasks.withType<AndroidLintTask>().configureEach { it.variantInputs.addSourceSets() }

    // Also configure the model writing task, so that we don't run into mismatches between
    // analyzed sources in one module and a downstream module
    project.tasks.withType<LintModelWriterTask>().configureEach {
        it.variantInputs.addSourceSets()
    }
}

private fun Project.configureLint(lint: Lint, isLibrary: Boolean) {
    val extension = project.androidXExtension
    val isMultiplatform = project.multiplatformExtension != null
    val lintChecksProject =
        project.rootProject.findProject(":lint-checks")
            ?: if (allowMissingLintProject()) {
                return
            } else {
                throw GradleException("Project :lint-checks does not exist")
            }

    project.dependencies.add("lintChecks", lintChecksProject)

    afterEvaluate {
        addSourceSetsForAndroidMultiplatformAfterEvaluate()
    }

    // The purpose of this specific project is to test that lint is running, so
    // it contains expected violations that we do not want to trigger a build failure
    val isTestingLintItself = (project.path == ":lint-checks:integration-tests")

    lint.apply {
        // Skip lintVital tasks on assemble. We explicitly run lintRelease for libraries.
        checkReleaseBuilds = false
    }

    tasks.withType(AndroidLintTask::class.java).configureEach { task ->
        // Remove the lint and column attributes from generated lint baseline XML.
        if (task.name.startsWith("updateLintBaseline")) {
            task.doLast {
                task.projectInputs.lintOptions.baseline.orNull?.asFile?.let { file ->
                    if (file.exists()) {
                        file.writeText(removeLineAndColumnAttributes(file.readText()))
                    }
                }
            }
        }
    }

    // Lint is configured entirely in finalizeDsl so that individual projects cannot easily
    // disable individual checks in the DSL for any reason.
    lint.apply {
        if (!isTestingLintItself) {
            abortOnError = true
        }
        ignoreWarnings = true

        // Run lint on tests. Uses top-level lint.xml to specify checks.
        checkTestSources = true

        // Write output directly to the console (and nowhere else).
        textReport = true
        htmlReport = false

        // Format output for convenience.
        explainIssues = true
        noLines = false
        quiet = true

        // We run lint on each library, so we don't want transitive checking of each dependency
        checkDependencies = false

        if (extension.type.allowCallingVisibleForTestsApis) {
            // Test libraries are allowed to call @VisibleForTests code
            disable.add("VisibleForTests")
        } else {
            fatal.add("VisibleForTests")
        }

        if (isMultiplatform) {
            // Disable classfile-based checks because lint cannot find the class files for
            // multiplatform projects and `SourceSet.java.classesDirectory` is not configurable.
            // This is not ideal, but it's better than having no lint checks at all.
            disable.add("LintError")
        }

        // Reenable after b/238892319 is resolved
        disable.add("NotificationPermission")

        // Disable dependency checks that suggest to change them. We want libraries to be
        // intentional with their dependency version bumps.
        disable.add("KtxExtensionAvailable")
        disable.add("GradleDependency")

        // Disable a check that's only relevant for real apps. For our test apps we're not
        // concerned with drawables potentially being a little bit blurry
        disable.add("IconMissingDensityFolder")

        // Disable until it works for our projects, b/171986505
        disable.add("JavaPluginLanguageLevel")

        // Explicitly disable StopShip check (see b/244617216)
        disable.add("StopShip")

        // Broken in 7.0.0-alpha15 due to b/180408990
        disable.add("RestrictedApi")

        // Disable until ag/19949626 goes in (b/261918265)
        disable.add("MissingQuantity")

        // Provide stricter enforcement for project types intended to run on a device.
        if (extension.type.compilationTarget == CompilationTarget.DEVICE) {
            fatal.add("Assert")
            fatal.add("NewApi")
            fatal.add("ObsoleteSdkInt")
            fatal.add("NoHardKeywords")
            fatal.add("UnusedResources")
            fatal.add("KotlinPropertyAccess")
            fatal.add("LambdaLast")
            fatal.add("UnknownNullness")

            // Too many Kotlin features require synthetic accessors - we want to rely on R8 to
            // remove these accessors
            disable.add("SyntheticAccessor")

            // Only check for missing translations in finalized (beta and later) modules.
            if (extension.mavenVersion?.isFinalApi() == true) {
                fatal.add("MissingTranslation")
            } else {
                disable.add("MissingTranslation")
            }
        } else {
            disable.add("BanUncheckedReflection")
        }

        // Broken in 7.0.0-alpha15 due to b/187343720
        disable.add("UnusedResources")

        // Disable NullAnnotationGroup check for :compose:ui:ui-text (b/233788571)
        if (isLibrary && project.group == "androidx.compose.ui" && project.name == "ui-text") {
            disable.add("NullAnnotationGroup")
        }

        if (extension.type == LibraryType.SAMPLES) {
            // TODO: b/190833328 remove if / when AGP will analyze dependencies by default
            //  This is needed because SampledAnnotationDetector uses partial analysis, and
            //  hence requires dependencies to be analyzed.
            checkDependencies = true
        }

        // Only run certain checks where API tracking is important.
        if (extension.type.checkApi is RunApiTasks.No) {
            disable.add("IllegalExperimentalApiUsage")
        }

        // If the project has not overridden the lint config, set the default one.
        if (lintConfig == null) {
            val lintXmlPath =
                if (extension.type == LibraryType.SAMPLES) {
                    "buildSrc/lint_samples.xml"
                } else {
                    "buildSrc/lint.xml"
                }
            // suppress warnings more specifically than issue-wide severity (regexes)
            // Currently suppresses warnings from baseline files working as intended
            lintConfig = File(project.getSupportRootFolder(), lintXmlPath)
        }

        baseline = lintBaseline.get().asFile
    }
}

/**
 * Lint uses [ConfigurableFileCollection.disallowChanges] during initialization, which prevents
 * modifying the file collection separately (there is no time to configure it before AGP has
 * initialized and disallowed changes). This uses reflection to temporarily allow changes, and apply
 * [block].
 */
private fun ConfigurableFileCollection.withChangesAllowed(
    block: ConfigurableFileCollection.() -> Unit
) {
    val disallowChanges = this::class.java.getDeclaredField("disallowChanges")
    disallowChanges.isAccessible = true
    disallowChanges.set(this, false)
    block()
    disallowChanges.set(this, true)
}

private val Project.lintBaseline: RegularFileProperty
    get() = project.objects.fileProperty().fileValue(File(projectDir, "lint-baseline.xml"))
