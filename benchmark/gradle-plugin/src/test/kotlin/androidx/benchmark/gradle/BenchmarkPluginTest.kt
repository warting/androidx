/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.benchmark.gradle

import androidx.testutils.gradle.ProjectSetupRule
import java.io.File
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.UnexpectedBuildFailure
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

private val PLUGINS_HEADER =
    """
    plugins {
        id('androidx.benchmark')
        id('com.android.library')
    }
"""
        .trimIndent()

private val REGISTER_ON_VARIANT_PRINT_PROPERTIES =
    """
    abstract class PrintMapPropertiesForVariantTask extends DefaultTask {
        @Input abstract MapProperty<String, Object> getProps()
        @TaskAction void exec() {
            props.get().forEach { k,v -> logger.warn( k + "=" + v)  }
        }
    }
    androidComponents {
        onVariants(selector()) { variant ->
            tasks.register(variant.name + "Props", PrintMapPropertiesForVariantTask) { t ->
                t.props.set(variant.experimentalProperties)
            }
        }
    }
"""
        .trimIndent()

@RunWith(JUnit4::class)
class BenchmarkPluginTest {

    @get:Rule val projectSetup = ProjectSetupRule()

    private lateinit var versionPropertiesFile: File
    private lateinit var gradleRunner: GradleRunner

    @Before
    fun setUp() {
        versionPropertiesFile = File(projectSetup.rootDir, "version.properties")
        versionPropertiesFile.createNewFile()

        File("src/test/test-data", "app-project").copyRecursively(projectSetup.rootDir)

        gradleRunner = GradleRunner.create().withProjectDir(projectSetup.rootDir)
        // Run tests using Gradle 8.14 to support AGP version used for the tests, b/431847401
        projectSetup.setUpGradleVersion(gradleRunner, "8.14")

        projectSetup.testProjectDir
            .newFile("settings.gradle")
            .writeText(
                """
            buildscript {
                repositories {
                    ${projectSetup.allRepositoryPaths.joinToString("\n") { """ maven { url "$it" } """ }}
                }
                dependencies {
                    classpath "com.android.tools.build:gradle:8.1.1"
                    classpath "androidx.benchmark:androidx.benchmark.gradle.plugin:+"
                }
            }
        """
                    .trimIndent()
            )
    }

    @Test
    fun applyPluginAndroidLibProject() {
        projectSetup.writeDefaultBuildGradle(
            prefix =
                """
                plugins {
                    id('com.android.library')
                    id('androidx.benchmark')
                }
            """
                    .trimIndent(),
            suffix =
                """
            android {
                namespace = "androidx.benchmark.gradle.test"
            }
            dependencies {
                androidTestImplementation "androidx.benchmark:benchmark:1.0.0-alpha01"
            }
            """
                    .trimIndent(),
        )

        val output = gradleRunner.withArguments("tasks", "--stacktrace").build()
        assertTrue { output.output.contains("lockClocks - ") }
        assertTrue { output.output.contains("unlockClocks - ") }
    }

    @Test
    fun applyPluginNonAndroidProject() {
        projectSetup.buildFile.writeText(
            """
            plugins {
                id('java')
                id('androidx.benchmark')
            }

            repositories {
                ${projectSetup.defaultRepoLines}
            }

            dependencies {
                testImplementation "androidx.benchmark:benchmark:1.0.0-alpha01"
            }
            """
                .trimIndent()
        )

        assertFailsWith(UnexpectedBuildFailure::class) { gradleRunner.withArguments("jar").build() }
    }

    @Test
    fun applyPluginNonBenchmarkProject() {
        projectSetup.writeDefaultBuildGradle(
            prefix = PLUGINS_HEADER,
            suffix =
                """
                android {
                    namespace = "androidx.benchmark.gradle.test"
                }
            """
                    .trimIndent(),
        )

        val output = gradleRunner.withArguments("tasks", "--stacktrace").build()
        assertTrue { output.output.contains("lockClocks - ") }
        assertTrue { output.output.contains("unlockClocks - ") }
    }

    @Test
    fun applyPluginBeforeAndroid() {
        projectSetup.writeDefaultBuildGradle(
            prefix = PLUGINS_HEADER,
            suffix =
                """
            android {
                namespace = "androidx.benchmark.gradle.test"
            }
            dependencies {
                androidTestImplementation "androidx.benchmark:benchmark:1.0.0-alpha01"
            }
            """
                    .trimIndent(),
        )

        val output = gradleRunner.withArguments("tasks", "--stacktrace").build()
        assertTrue { output.output.contains("lockClocks - ") }
        assertTrue { output.output.contains("unlockClocks - ") }
    }

    @Test
    fun applyPluginOnAgp36() {
        projectSetup.writeDefaultBuildGradle(
            prefix = PLUGINS_HEADER,
            suffix =
                """
            android {
                namespace = "androidx.benchmark.gradle.test"
                defaultConfig {
                    testInstrumentationRunnerArguments additionalTestOutputDir: "/fake_path/files"
                }
            }

            dependencies {
                androidTestImplementation "androidx.benchmark:benchmark:1.0.0-alpha01"
            }

            tasks.register("printTestBuildType") {
                println android.testBuildType
            }
            """
                    .trimIndent(),
        )

        projectSetup.gradlePropertiesFile.appendText("android.enableAdditionalTestOutput=true")
        versionPropertiesFile.writeText("buildVersion=3.6.0-alpha05")

        val output = gradleRunner.withArguments("tasks", "--stacktrace").build()
        assertTrue { output.output.contains("lockClocks - ") }
        assertTrue { output.output.contains("unlockClocks - ") }

        // Should depend on AGP to pull benchmark reports via additionalTestOutputDir.
        assertFalse { output.output.contains("benchmarkReport - ") }

        val testBuildTypeOutput =
            gradleRunner.withArguments("printTestBuildType", "--stacktrace").build()
        assertTrue { testBuildTypeOutput.output.contains("release") }
    }

    @Test
    fun applyPluginOnAgp35() {
        projectSetup.writeDefaultBuildGradle(
            prefix = PLUGINS_HEADER,
            suffix =
                """
            android {
                namespace = "androidx.benchmark.gradle.test"
                defaultConfig {
                    testInstrumentationRunnerArguments.remove("additionalTestOutputDir")
                }
            }

            dependencies {
                androidTestImplementation "androidx.benchmark:benchmark:1.0.0-alpha01"
            }

            tasks.register("printInstrumentationArgs") {
                println android.defaultConfig.testInstrumentationRunnerArguments
            }

            tasks.register("printTestBuildType") {
                println android.testBuildType
            }
            """
                    .trimIndent(),
        )

        versionPropertiesFile.writeText("buildVersion=3.5.0-rc03")

        val output = gradleRunner.withArguments("tasks", "--stacktrace").build()
        assertTrue { output.output.contains("lockClocks - ") }
        assertTrue { output.output.contains("unlockClocks - ") }

        // Should try to pull benchmark reports via legacy BenchmarkPlugin code path.
        assertTrue { output.output.contains("benchmarkReport - ") }

        val argsOutput = gradleRunner.withArguments("printInstrumentationArgs").build()
        assertTrue { argsOutput.output.contains("no-isolated-storage:1") }

        val testBuildTypeOutput = gradleRunner.withArguments("printTestBuildType").build()
        assertTrue { testBuildTypeOutput.output.contains("release") }
    }

    @Test
    fun applyPluginDefaultAgpProperties() {
        projectSetup.writeDefaultBuildGradle(
            prefix = "import com.android.build.gradle.TestedExtension\n$PLUGINS_HEADER",
            suffix =
                """
            android {
                namespace = "androidx.benchmark.gradle.test"
            }
            dependencies {
                androidTestImplementation "androidx.benchmark:benchmark:1.0.0-alpha01"
            }

            tasks.register("printTestInstrumentationRunner") {
                println android.defaultConfig.testInstrumentationRunner
            }

            tasks.register("printTestCoverageEnabled") {
                def extension = project.extensions.getByType(TestedExtension)
                println extension.buildTypes.getByName("debug").testCoverageEnabled
            }
            """
                    .trimIndent(),
        )

        val runnerOutput = gradleRunner.withArguments("printTestInstrumentationRunner").build()
        assertTrue {
            runnerOutput.output.contains("androidx.benchmark.junit4.AndroidBenchmarkRunner")
        }

        val codeCoverageOutput = gradleRunner.withArguments("printTestCoverageEnabled").build()
        assertTrue { codeCoverageOutput.output.contains("false") }
    }

    @Test
    fun applyPluginOverrideAgpProperties() {
        projectSetup.writeDefaultBuildGradle(
            prefix = "import com.android.build.gradle.TestedExtension\n$PLUGINS_HEADER",
            suffix =
                """
            android {
                namespace = "androidx.benchmark.gradle.test"
                defaultConfig {
                    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
                }

                buildTypes {
                    debug {
                        testCoverageEnabled = true
                    }
                }
            }

            dependencies {
                androidTestImplementation "androidx.benchmark:benchmark:1.0.0-alpha01"
            }

            tasks.register("printTestInstrumentationRunner") {
                println android.defaultConfig.testInstrumentationRunner
            }

            tasks.register("printTestCoverageEnabled") {
                def extension = project.extensions.getByType(TestedExtension)
                println extension.buildTypes.getByName("debug").testCoverageEnabled
            }
            """
                    .trimIndent(),
        )

        val runnerOutput = gradleRunner.withArguments("printTestInstrumentationRunner").build()
        assertTrue { runnerOutput.output.contains("androidx.test.runner.AndroidJUnitRunner") }

        val codeCoverageOutput = gradleRunner.withArguments("printTestCoverageEnabled").build()
        assertTrue { codeCoverageOutput.output.contains("true") }
    }

    @Test
    fun applyPluginAndroidOldRunner36() {
        projectSetup.writeDefaultBuildGradle(
            prefix = PLUGINS_HEADER,
            suffix =
                """
            android {
                defaultConfig {
                    testInstrumentationRunner "androidx.benchmark.AndroidBenchmarkRunner"
                    testInstrumentationRunnerArguments additionalTestOutputDir: "/fake_path/files"
                }
            }

            dependencies {
                androidTestImplementation "androidx.benchmark:benchmark:1.0.0-alpha04"
            }
            """
                    .trimIndent(),
        )
        projectSetup.gradlePropertiesFile.appendText("android.enableAdditionalTestOutput=true")

        assertFailsWith(UnexpectedBuildFailure::class) {
            gradleRunner.withArguments("assemble").build()
        }
    }

    @Test
    fun applyPluginAndroidOldRunner35() {
        projectSetup.writeDefaultBuildGradle(
            prefix = PLUGINS_HEADER,
            suffix =
                """
            android {
                defaultConfig {
                    testInstrumentationRunner "androidx.benchmark.AndroidBenchmarkRunner"
                    testInstrumentationRunnerArguments.remove("additionalTestOutputDir")
                }
            }

            dependencies {
                androidTestImplementation "androidx.benchmark:benchmark:1.0.0-alpha04"
            }
            """
                    .trimIndent(),
        )

        assertFailsWith(UnexpectedBuildFailure::class) {
            gradleRunner.withArguments("assemble").build()
        }
    }

    @Test
    fun applyPluginSigningConfig() {
        projectSetup.writeDefaultBuildGradle(
            prefix = "import com.android.build.gradle.TestedExtension\n$PLUGINS_HEADER",
            suffix =
                """
            android {
                namespace = "androidx.benchmark.gradle.test"
                defaultConfig {
                    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
                }
            }

            dependencies {
                androidTestImplementation "androidx.benchmark:benchmark-junit4:1.0.0"
            }

            tasks.register("printReleaseSigningConfig") {
                def extension = project.extensions.getByType(TestedExtension)
                def signingConfigName = extension.buildTypes.getByName("release").signingConfig.name
                println "BenchmarkPluginTestKt_applyPluginSigningConfig_${"$"}signingConfigName"
            }
            """
                    .trimIndent(),
        )

        val releaseTask = gradleRunner.withArguments("printReleaseSigningConfig").build()
        assertTrue(
            releaseTask.output
                .lines()
                .contains("BenchmarkPluginTestKt_applyPluginSigningConfig_debug")
        )
    }
}

@RunWith(JUnit4::class)
class BenchmarkPluginWithCurrentAgpTest {

    companion object {
        private val DEFAULT_BUILD_GRADLE =
            """
            android {
                defaultConfig {
                    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
                }
                namespace = "com.example"
            }
            dependencies {
                androidTestImplementation "androidx.benchmark:benchmark-junit4:1.0.0"
            }
            $REGISTER_ON_VARIANT_PRINT_PROPERTIES
        """
                .trimIndent()
    }

    @get:Rule val projectSetup = ProjectSetupRule()

    private lateinit var gradleRunner: GradleRunner

    @Before
    fun setUp() {
        File("src/test/test-data", "app-project").copyRecursively(projectSetup.rootDir)
        gradleRunner = GradleRunner.create().withProjectDir(projectSetup.rootDir)
        projectSetup.testProjectDir
            .newFile("settings.gradle")
            .writeText(
                """
            buildscript {
                repositories {
                    ${projectSetup.allRepositoryPaths.joinToString("\n") { """ maven { url "$it" } """ }}
                }
                dependencies {
                    classpath "${projectSetup.props.agpDependency}"
                    classpath "androidx.benchmark:androidx.benchmark.gradle.plugin:+"
                }
            }
        """
                    .trimIndent()
            )
    }

    @Test
    fun enableAotForMicroBenchmarks() {
        projectSetup.writeDefaultBuildGradle(prefix = PLUGINS_HEADER, suffix = DEFAULT_BUILD_GRADLE)
        gradleRunner.withArguments("releaseProps").build().output.lines().apply {
            assertTrue(contains("android.experimental.force-aot-compilation=true"))
        }
    }

    @Test
    fun disableAotForMicroBenchmarksWhenPropertyIsFalse() {
        projectSetup.writeDefaultBuildGradle(prefix = PLUGINS_HEADER, suffix = DEFAULT_BUILD_GRADLE)
        gradleRunner
            .withArguments("releaseProps", "-Pandroidx.benchmark.forceaotcompilation=false")
            .build()
            .output
            .lines()
            .apply { assertFalse(contains("android.experimental.force-aot-compilation=true")) }
    }
}
