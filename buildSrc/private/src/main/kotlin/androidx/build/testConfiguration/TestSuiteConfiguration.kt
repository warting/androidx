/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.build.testConfiguration

import androidx.build.AndroidXExtension
import androidx.build.AndroidXImplPlugin
import androidx.build.AndroidXImplPlugin.Companion.FINALIZE_TEST_CONFIGS_WITH_APKS_TASK
import androidx.build.asFilenamePrefix
import androidx.build.dependencyTracker.AffectedModuleDetector
import androidx.build.getFileInTestConfigDirectory
import androidx.build.getPrivacySandboxFilesDirectory
import androidx.build.getSupportRootFolder
import androidx.build.hasBenchmarkPlugin
import androidx.build.isMacrobenchmark
import androidx.build.isPresubmitBuild
import androidx.build.multiplatformExtension
import com.android.build.api.artifact.Artifacts
import com.android.build.api.artifact.SingleArtifact
import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.KotlinMultiplatformAndroidTarget
import com.android.build.api.variant.AndroidComponentsExtension
import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import com.android.build.api.variant.HasDeviceTests
import com.android.build.api.variant.KotlinMultiplatformAndroidComponentsExtension
import com.android.build.api.variant.LibraryAndroidComponentsExtension
import com.android.build.api.variant.TestAndroidComponentsExtension
import com.android.build.api.variant.Variant
import com.android.build.gradle.BaseExtension
import com.android.build.gradle.TestExtension
import com.android.build.gradle.TestedExtension
import com.android.build.gradle.internal.attributes.VariantAttr
import com.android.build.gradle.internal.publishing.AndroidArtifacts
import com.android.build.gradle.internal.publishing.AndroidArtifacts.ArtifactType
import org.gradle.api.Project
import org.gradle.api.attributes.Usage
import org.gradle.api.file.FileCollection
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.named
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinAndroidTarget

/**
 * Creates and configures the test config generation task for a project. Configuration includes
 * populating the task with relevant data from the first 4 params, and setting whether the task is
 * enabled.
 */
fun Project.createTestConfigurationGenerationTask(
    variantName: String,
    artifacts: Artifacts,
    minSdk: Int,
    testRunner: Provider<String>,
    instrumentationRunnerArgs: Provider<Map<String, String>>
) {
    val xmlName = "${path.asFilenamePrefix()}$variantName.xml"
    val jsonName = "_${path.asFilenamePrefix()}$variantName.json"
    rootProject.tasks.named<ModuleInfoGenerator>("createModuleInfo").configure {
        it.testModules.add(
            TestModule(
                name = xmlName,
                path = listOf(projectDir.toRelativeString(getSupportRootFolder()))
            )
        )
    }
    val generateTestConfigurationTask =
        tasks.register(
            "${AndroidXImplPlugin.GENERATE_TEST_CONFIGURATION_TASK}$variantName",
            GenerateTestConfigurationTask::class.java
        ) { task ->
            val androidXExtension = extensions.getByType<AndroidXExtension>()
            if (isPrivacySandboxEnabled()) {
                // TODO (b/309610890): Replace for dependency on AGP artifact.
                val extractedPrivacySandboxSdkApksDir = layout.buildDirectory.dir(
                    "intermediates/extracted_apks_from_privacy_sandbox_sdks"
                )
                task.privacySandboxSdkApks.from(
                    files(extractedPrivacySandboxSdkApksDir) {
                        it.builtBy("buildPrivacySandboxSdkApksForDebug")
                    }
                )
                // TODO (b/309610890): Replace for dependency on AGP artifact.
                val usesSdkSplitDir = layout.buildDirectory.dir(
                    "intermediates/uses_sdk_library_split_for_local_deployment"
                )
                task.privacySandboxUsesSdkSplit.from(
                    files(usesSdkSplitDir) {
                        it.builtBy("generateDebugAdditionalSplitForPrivacySandboxDeployment")
                    }
                )
                task.outputPrivacySandboxFilenamesPrefix.set(
                    "${path.asFilenamePrefix()}-$variantName"
                )
                task.outputPrivacySandboxFiles.set(
                    getPrivacySandboxFilesDirectory().map {
                        it.dir("${path.asFilenamePrefix()}-$variantName")
                    }
                )
            }

            task.testFolder.set(artifacts.get(SingleArtifact.APK))
            task.testLoader.set(artifacts.getBuiltArtifactsLoader())
            task.outputTestApk.set(
                getFileInTestConfigDirectory("${path.asFilenamePrefix()}-$variantName.apk")
            )
            task.additionalApkKeys.set(androidXExtension.additionalDeviceTestApkKeys)
            task.additionalTags.set(androidXExtension.additionalDeviceTestTags)
            task.outputXml.set(getFileInTestConfigDirectory(xmlName))
            task.outputJson.set(getFileInTestConfigDirectory(jsonName))
            task.presubmit.set(isPresubmitBuild())
            task.instrumentationArgs.putAll(instrumentationRunnerArgs)
            task.minSdk.set(minSdk)
            task.hasBenchmarkPlugin.set(hasBenchmarkPlugin())
            task.macrobenchmark.set(isMacrobenchmark())
            task.testRunner.set(testRunner)
            // Skip task if getTestSourceSetsForAndroid is empty, even if
            //  androidXExtension.deviceTests.enabled is set to true
            task.androidTestSourceCodeCollection.from(getTestSourceSetsForAndroid())
            task.enabled = androidXExtension.deviceTests.enabled
            AffectedModuleDetector.configureTaskGuard(task)
        }
    rootProject.tasks
        .findByName(FINALIZE_TEST_CONFIGS_WITH_APKS_TASK)!!
        .dependsOn(generateTestConfigurationTask)
}

/**
 * Further configures the test config generation task for a project. This only gets called when
 * there is a test app in addition to the instrumentation app, and the only thing it configures is
 * the location of the testapp.
 */
fun Project.addAppApkToTestConfigGeneration(androidXExtension: AndroidXExtension) {

    fun outputAppApkFile(
        variant: Variant,
        appProjectPath: String,
        instrumentationProjectPath: String?
    ): Provider<RegularFile> {
        var filename = appProjectPath.asFilenamePrefix()
        if (instrumentationProjectPath != null) {
            filename += "_for_${instrumentationProjectPath.asFilenamePrefix()}"
        }
        filename += "-${variant.name}.apk"
        return getFileInTestConfigDirectory(filename)
    }

    // For application modules, the instrumentation apk is generated in the module itself
    extensions.findByType(ApplicationAndroidComponentsExtension::class.java)?.apply {
        onVariants(selector().withBuildType("debug")) { variant ->
            tasks.named(
                "${AndroidXImplPlugin.GENERATE_TEST_CONFIGURATION_TASK}${variant.name}AndroidTest",
                GenerateTestConfigurationTask::class.java
            ) { task ->
                task.appFolder.set(variant.artifacts.get(SingleArtifact.APK))
                task.appLoader.set(variant.artifacts.getBuiltArtifactsLoader())

                // The target project is the same being evaluated
                task.outputAppApk.set(outputAppApkFile(variant, path, null))
            }
        }
    }

    // Migrate away when b/280680434 is fixed.
    // For tests modules, the instrumentation apk is pulled from the <variant>TestedApks
    // configuration. Note that also the associated test configuration task name is different
    // from the application one.
    extensions.findByType(TestAndroidComponentsExtension::class.java)?.apply {
        onVariants(selector().all()) { variant ->
            tasks.named(
                "${AndroidXImplPlugin.GENERATE_TEST_CONFIGURATION_TASK}${variant.name}",
                GenerateTestConfigurationTask::class.java
            ) { task ->
                task.appLoader.set(variant.artifacts.getBuiltArtifactsLoader())

                // The target app path is defined in the targetProjectPath field in the android
                // extension of the test module
                val targetProjectPath =
                    project.extensions.getByType(TestExtension::class.java).targetProjectPath
                        ?: throw IllegalStateException(
                            """
                        Module `$path` does not have a targetProjectPath defined.
                    """
                                .trimIndent()
                        )
                task.outputAppApk.set(outputAppApkFile(variant, targetProjectPath, path))

                task.appFileCollection.from(
                    configurations
                        .named("${variant.name}TestedApks")
                        .get()
                        .incoming
                        .artifactView {
                            it.attributes { container ->
                                container.attribute(
                                    AndroidArtifacts.ARTIFACT_TYPE,
                                    ArtifactType.APK.type
                                )
                            }
                        }
                        .files
                )
            }
        }
    }

    // For library modules we only look at the build type debug. The target app project can be
    // specified through the androidX extension, through: targetAppProjectForInstrumentationTest
    // and targetAppProjectVariantForInstrumentationTest.
    extensions.findByType(LibraryAndroidComponentsExtension::class.java)?.apply {
        onVariants(selector().withBuildType("debug")) { variant ->
            val targetAppProject =
                androidXExtension.deviceTests.targetAppProject ?: return@onVariants
            val targetAppProjectVariant = androidXExtension.deviceTests.targetAppVariant

            // Recreate the same configuration existing for test modules to pull the artifact
            // from the application module specified in the deviceTests extension.
            @Suppress("UnstableApiUsage") // Incubating dependencyFactory APIs
            val configuration =
                configurations.create("${variant.name}TestedApks") { config ->
                    config.isCanBeResolved = true
                    config.isCanBeConsumed = false
                    config.attributes {
                        it.attribute(VariantAttr.ATTRIBUTE, objects.named(targetAppProjectVariant))
                        it.attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.JAVA_RUNTIME))
                    }
                    config.dependencies.add(project.dependencyFactory.create(targetAppProject))
                }

            tasks.named(
                "${AndroidXImplPlugin.GENERATE_TEST_CONFIGURATION_TASK}${variant.name}AndroidTest",
                GenerateTestConfigurationTask::class.java
            ) { task ->
                task.appLoader.set(variant.artifacts.getBuiltArtifactsLoader())

                // The target app path is defined in the androidx extension
                task.outputAppApk.set(outputAppApkFile(variant, targetAppProject.path, path))

                task.appFileCollection.from(
                    configuration.incoming
                        .artifactView { view ->
                            view.attributes {
                                it.attribute(AndroidArtifacts.ARTIFACT_TYPE, ArtifactType.APK.type)
                            }
                        }
                        .files
                )
            }
        }
    }
}

private fun getOrCreateMediaTestConfigTask(
    project: Project
): TaskProvider<GenerateMediaTestConfigurationTask> {
    val parentProject = project.parent!!
    if (
        !parentProject.tasks
            .withType(GenerateMediaTestConfigurationTask::class.java)
            .names
            .contains("support-media-test${AndroidXImplPlugin.GENERATE_TEST_CONFIGURATION_TASK}")
    ) {
        val task =
            parentProject.tasks.register(
                "support-media-test${AndroidXImplPlugin.GENERATE_TEST_CONFIGURATION_TASK}",
                GenerateMediaTestConfigurationTask::class.java
            ) { task ->
                AffectedModuleDetector.configureTaskGuard(task)
            }
        project.rootProject.tasks.findByName(FINALIZE_TEST_CONFIGS_WITH_APKS_TASK)!!.dependsOn(task)
        return task
    } else {
        return parentProject.tasks
            .withType(GenerateMediaTestConfigurationTask::class.java)
            .named("support-media-test${AndroidXImplPlugin.GENERATE_TEST_CONFIGURATION_TASK}")
    }
}

fun Project.createOrUpdateMediaTestConfigurationGenerationTask(
    variantName: String,
    artifacts: Artifacts,
    minSdk: Int,
    testRunner: Provider<String>,
) {
    val mediaTask = getOrCreateMediaTestConfigTask(this)

    fun getJsonName(clientToT: Boolean, serviceToT: Boolean, clientTests: Boolean): String {
        return "_mediaClient${
            if (clientToT) "ToT" else "Previous"
        }Service${
            if (serviceToT) "ToT" else "Previous"
        }${
            if (clientTests) "Client" else "Service"
        }Tests$variantName.json"
    }

    fun ModuleInfoGenerator.addTestModule(clientToT: Boolean, serviceToT: Boolean) {
        // We don't test the combination of previous versions of service and client as that is not
        // useful data. We always want at least one tip of tree project.
        if (!clientToT && !serviceToT) return
        testModules.add(
            TestModule(
                name =
                    getJsonName(clientToT = clientToT, serviceToT = serviceToT, clientTests = true),
                path = listOf(projectDir.toRelativeString(getSupportRootFolder()))
            )
        )
        testModules.add(
            TestModule(
                name =
                    getJsonName(
                        clientToT = clientToT,
                        serviceToT = serviceToT,
                        clientTests = false
                    ),
                path = listOf(projectDir.toRelativeString(getSupportRootFolder()))
            )
        )
    }
    val isClient = this.name.contains("client")
    val isPrevious = this.name.contains("previous")

    rootProject.tasks.named<ModuleInfoGenerator>("createModuleInfo").configure {
        if (isClient) {
            it.addTestModule(clientToT = !isPrevious, serviceToT = false)
            it.addTestModule(clientToT = !isPrevious, serviceToT = true)
        } else {
            it.addTestModule(clientToT = true, serviceToT = !isPrevious)
            it.addTestModule(clientToT = false, serviceToT = !isPrevious)
        }
    }
    mediaTask.configure {
        if (isClient) {
            if (isPrevious) {
                it.clientPreviousFolder.set(artifacts.get(SingleArtifact.APK))
                it.clientPreviousLoader.set(artifacts.getBuiltArtifactsLoader())
            } else {
                it.clientToTFolder.set(artifacts.get(SingleArtifact.APK))
                it.clientToTLoader.set(artifacts.getBuiltArtifactsLoader())
            }
        } else {
            if (isPrevious) {
                it.servicePreviousFolder.set(artifacts.get(SingleArtifact.APK))
                it.servicePreviousLoader.set(artifacts.getBuiltArtifactsLoader())
            } else {
                it.serviceToTFolder.set(artifacts.get(SingleArtifact.APK))
                it.serviceToTLoader.set(artifacts.getBuiltArtifactsLoader())
            }
        }
        it.jsonClientPreviousServiceToTClientTests.set(
            getFileInTestConfigDirectory(
                getJsonName(clientToT = false, serviceToT = true, clientTests = true)
            )
        )
        it.jsonClientPreviousServiceToTServiceTests.set(
            getFileInTestConfigDirectory(
                getJsonName(clientToT = false, serviceToT = true, clientTests = false)
            )
        )
        it.jsonClientToTServicePreviousClientTests.set(
            getFileInTestConfigDirectory(
                getJsonName(clientToT = true, serviceToT = false, clientTests = true)
            )
        )
        it.jsonClientToTServicePreviousServiceTests.set(
            getFileInTestConfigDirectory(
                getJsonName(clientToT = true, serviceToT = false, clientTests = false)
            )
        )
        it.jsonClientToTServiceToTClientTests.set(
            getFileInTestConfigDirectory(
                getJsonName(clientToT = true, serviceToT = true, clientTests = true)
            )
        )
        it.jsonClientToTServiceToTServiceTests.set(
            getFileInTestConfigDirectory(
                getJsonName(clientToT = true, serviceToT = true, clientTests = false)
            )
        )
        it.totClientApk.set(getFileInTestConfigDirectory("mediaClientToT$variantName.apk"))
        it.previousClientApk.set(
            getFileInTestConfigDirectory("mediaClientPrevious$variantName.apk")
        )
        it.totServiceApk.set(
            getFileInTestConfigDirectory("mediaServiceToT$variantName.apk")
        )
        it.previousServiceApk.set(
            getFileInTestConfigDirectory("mediaServicePrevious$variantName.apk")
        )
        it.minSdk.set(minSdk)
        it.testRunner.set(testRunner)
        it.presubmit.set(isPresubmitBuild())
        AffectedModuleDetector.configureTaskGuard(it)
    }
}

@Suppress("UnstableApiUsage") // usage of HasDeviceTests
fun Project.configureTestConfigGeneration(baseExtension: BaseExtension) {
    extensions.getByType(AndroidComponentsExtension::class.java).apply {
        onVariants { variant ->
            when {
                variant is HasDeviceTests -> {
                    variant.deviceTests.forEach { deviceTest ->
                        when {
                            path.contains("media:version-compat-tests:") -> {
                                createOrUpdateMediaTestConfigurationGenerationTask(
                                    deviceTest.name,
                                    deviceTest.artifacts,
                                    // replace minSdk after b/328495232 is fixed
                                    baseExtension.defaultConfig.minSdk!!,
                                    deviceTest.instrumentationRunner,
                                )
                            }
                            else -> {
                                createTestConfigurationGenerationTask(
                                    deviceTest.name,
                                    deviceTest.artifacts,
                                    // replace minSdk after b/328495232 is fixed
                                    baseExtension.defaultConfig.minSdk!!,
                                    deviceTest.instrumentationRunner,
                                    deviceTest.instrumentationRunnerArguments
                                )
                            }
                        }
                    }
                }
                project.plugins.hasPlugin("com.android.test") -> {
                    createTestConfigurationGenerationTask(
                        variant.name,
                        variant.artifacts,
                        // replace minSdk after b/328495232 is fixed
                        baseExtension.defaultConfig.minSdk!!,
                        provider { baseExtension.defaultConfig.testInstrumentationRunner!! },
                        provider { baseExtension.defaultConfig.testInstrumentationRunnerArguments }
                    )
                }
            }
        }
    }
}

fun Project.configureTestConfigGeneration(
    kotlinMultiplatformAndroidTarget: KotlinMultiplatformAndroidTarget,
    componentsExtension: KotlinMultiplatformAndroidComponentsExtension
) {
    componentsExtension.onVariant { variant ->
        @Suppress("UnstableApiUsage") // usage of HasDeviceTests
        variant.deviceTests.forEach { deviceTest ->
            createTestConfigurationGenerationTask(
                deviceTest.name,
                deviceTest.artifacts,
                // replace minSdk after b/328495232 is fixed
                kotlinMultiplatformAndroidTarget.minSdk!!,
                deviceTest.instrumentationRunner,
                deviceTest.instrumentationRunnerArguments,
            )
        }
    }
}

private fun Project.getTestSourceSetsForAndroid(): List<FileCollection> {
    val testSourceFileCollections = mutableListOf<FileCollection>()
    // com.android.test modules keep test code in main sourceset
    extensions.findByType(TestExtension::class.java)?.let { testExtension ->
        testExtension.sourceSets.find { it.name == "main" }?.let { sourceSet ->
            testSourceFileCollections.addAll(sourceSet.java.getSourceDirectoryTrees())
        }
        // Add kotlin-android main source set
        extensions
            .findByType(KotlinAndroidProjectExtension::class.java)
            ?.sourceSets
            ?.find { it.name == "main" }
            ?.let { testSourceFileCollections.add(it.kotlin.sourceDirectories) }
        // Note, don't have to add kotlin-multiplatform as it is not compatible with
        // com.android.test modules
    }

    // Add Java androidTest source set
    extensions
        .findByType(TestedExtension::class.java)
        ?.sourceSets
        ?.find { it.name == "androidTest" }
        ?.let { sourceSet ->
            testSourceFileCollections.addAll(sourceSet.java.getSourceDirectoryTrees())
        }

    // Add kotlin-android androidTest source set
    extensions
        .findByType(KotlinAndroidProjectExtension::class.java)
        ?.sourceSets
        ?.find { it.name == "androidTest" }
        ?.let { testSourceFileCollections.add(it.kotlin.sourceDirectories) }

    // Add kotlin-multiplatform androidInstrumentedTest target source sets
    multiplatformExtension?.targets
        ?.filterIsInstance<KotlinAndroidTarget>()
        ?.mapNotNull { it.compilations.find { it.name == "debugAndroidTest" } }
        ?.flatMap { it.allKotlinSourceSets }
        ?.mapTo(testSourceFileCollections) { it.kotlin.sourceDirectories }
    return testSourceFileCollections
}

fun Project.isPrivacySandboxEnabled(): Boolean =
    extensions.findByType(ApplicationExtension::class.java)
        ?.privacySandbox
        ?.enable
        ?: false
