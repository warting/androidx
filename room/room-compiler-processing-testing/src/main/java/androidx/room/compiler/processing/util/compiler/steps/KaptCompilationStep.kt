/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.room.compiler.processing.util.compiler.steps

import androidx.room.compiler.processing.util.FileResource
import androidx.room.compiler.processing.util.compiler.KotlinCliRunner
import androidx.room.compiler.processing.util.compiler.toSourceSet
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.ObjectOutputStream
import java.util.Base64
import javax.annotation.processing.Processor
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.com.intellij.util.PathUtil
import org.jetbrains.kotlin.kapt.base.AptMode
import org.jetbrains.kotlin.kapt.cli.CliToolOption
import org.jetbrains.kotlin.kapt.cli.KaptCliOption

/** Runs KAPT to run Java annotation processors. */
internal class KaptCompilationStep(
    private val annotationProcessors: List<Processor>,
    private val processorOptions: Map<String, String>,
) : KotlinCompilationStep {
    override val name = "kapt"

    init {
        check(annotationProcessors.size <= PROCESSOR_DELEGATES_SIZE) {
            "Only $PROCESSOR_DELEGATES_SIZE annotation processor can be loaded for test " +
                "compilation for now, but requested ${annotationProcessors.size}. " +
                "Tell Dany to support more! Existing processors: [\n    " +
                annotationProcessors
                    .map { it.javaClass.canonicalName }
                    .sorted()
                    .joinToString(",\n    ") +
                "]"
        }
    }

    override fun execute(
        workingDir: File,
        arguments: CompilationStepArguments,
    ): CompilationStepResult {
        if (annotationProcessors.isEmpty()) {
            return CompilationStepResult.skip(arguments)
        }

        val kaptArgs = buildList {
            // Both 'kotlin-annotation-processing.jar' and
            // 'kotlin-annotation-processing-embeddable.jar' contain the KAPT plugin that must
            // specified so we use the `CliToolOption` class just as `KaptCli` does.
            val pathToAnnotationJar = PathUtil.getJarPathForClass(CliToolOption::class.java)
            add("-Xplugin=$pathToAnnotationJar")
            createKaptCliOptions(workingDir, arguments).forEach { (option, value) ->
                add("-P")
                add("plugin:$KAPT_PLUGIN_ID:${option.optionName}=$value")
            }
        }
        val argumentsWithKapt =
            arguments.copy(kotlincArguments = arguments.kotlincArguments + kaptArgs)
        val result =
            try {
                delegateProcessors.set(annotationProcessors)
                KotlinCliRunner.runKotlinCli(
                    arguments = argumentsWithKapt,
                    destinationDir = workingDir.resolve(CLASS_OUT_FOLDER_NAME),
                )
            } finally {
                delegateProcessors.remove()
            }
        val generatedSources =
            listOfNotNull(
                workingDir.resolve(JAVA_SRC_OUT_FOLDER_NAME).toSourceSet(),
                workingDir.resolve(KOTLIN_SRC_OUT_FOLDER_NAME).toSourceSet(),
            )
        val diagnostics =
            resolveDiagnostics(
                diagnostics = result.diagnostics,
                sourceSets = arguments.sourceSets + generatedSources,
            )
        val outputResources = workingDir.resolve(RESOURCES_OUT_FOLDER_NAME)
        val outputClasspath = listOf(result.compiledClasspath) + outputResources
        val generatedResources =
            outputResources
                .walkTopDown()
                .filter { it.isFile }
                .map { FileResource(it.relativeTo(outputResources).path, it) }
                .toList()
        return CompilationStepResult(
            success = result.exitCode == ExitCode.OK,
            generatedSourceRoots = generatedSources,
            diagnostics = diagnostics,
            nextCompilerArguments =
                arguments.copy(sourceSets = arguments.sourceSets + generatedSources),
            outputClasspath = outputClasspath,
            generatedResources = generatedResources,
        )
    }

    private fun createKaptCliOptions(
        workingDir: File,
        arguments: CompilationStepArguments,
    ): List<Pair<KaptCliOption, String>> = buildList {
        add(KaptCliOption.APT_MODE_OPTION to AptMode.STUBS_AND_APT.stringValue)
        add(
            KaptCliOption.SOURCE_OUTPUT_DIR_OPTION to
                workingDir.resolve(JAVA_SRC_OUT_FOLDER_NAME).absolutePath
        )
        // Compiled classes don't end up here but generated resources do.
        add(
            KaptCliOption.CLASS_OUTPUT_DIR_OPTION to
                workingDir.resolve(RESOURCES_OUT_FOLDER_NAME).absolutePath
        )
        add(
            KaptCliOption.STUBS_OUTPUT_DIR_OPTION to
                workingDir.resolve(STUBS_OUT_FOLDER_NAME).absolutePath
        )

        // 'apclasspath' is not used since FQN are specified in 'processors', but if left unset
        // KAPT does not try to load processors at all.
        add(KaptCliOption.ANNOTATION_PROCESSOR_CLASSPATH_OPTION to "empty")

        for (index in annotationProcessors.indices) {
            add(
                KaptCliOption.ANNOTATION_PROCESSORS_OPTION to
                    TestDelegateProcessor.KaptTestDelegateAP0::class.java.name.dropLast(1) + index
            )
        }

        val apOptionsMap = buildMap {
            // Kotlin generated source output location is specified through this special
            // annotation processor option.
            put(
                "kapt.kotlin.generated",
                workingDir.resolve(KOTLIN_SRC_OUT_FOLDER_NAME).also { it.mkdirs() }.canonicalPath,
            )
            putAll(processorOptions)
        }
        // We *need* to use the deprecated 'apoptions' since it supports multiple values as
        // opposed to 'apOption' that only accepts one value and also is
        // allowMultipleOccurrences = false
        @Suppress("DEPRECATION")
        add(KaptCliOption.APT_OPTIONS_OPTION to apOptionsMap.base64Encoded())

        val javacOptionsMap =
            arguments.javacArguments.associate { rawArg ->
                val keyValuePair = rawArg.split('=', limit = 2).takeIf { it.size == 2 }
                if (keyValuePair != null) {
                    keyValuePair[0] to keyValuePair[1]
                } else {
                    rawArg to ""
                }
            }
        // We *need* to use the deprecated 'javacArguments' since it supports multiple values as
        // opposed to 'javacOption' that only accepts one value and also is
        // allowMultipleOccurrences = false
        @Suppress("DEPRECATION")
        add(KaptCliOption.JAVAC_CLI_OPTIONS_OPTION to javacOptionsMap.base64Encoded())

        // NOTE: This does not work very well until the following bug is fixed
        //  https://youtrack.jetbrains.com/issue/KT-47934
        add(KaptCliOption.MAP_DIAGNOSTIC_LOCATIONS_OPTION to "true")
    }

    // As suggested by https://kotlinlang.org/docs/kapt.html#ap-javac-options-encoding
    private fun Map<String, String>.base64Encoded(): String {
        val os = ByteArrayOutputStream()
        val oos = ObjectOutputStream(os)
        oos.writeInt(size)
        for ((key, value) in entries) {
            oos.writeUTF(key)
            oos.writeUTF(value)
        }
        oos.flush()
        return Base64.getEncoder().encodeToString(os.toByteArray())
    }

    companion object {
        private const val JAVA_SRC_OUT_FOLDER_NAME = "kapt-java-src-out"
        private const val KOTLIN_SRC_OUT_FOLDER_NAME = "kapt-kotlin-src-out"
        private const val STUBS_OUT_FOLDER_NAME = "kapt-stubs-out"
        private const val RESOURCES_OUT_FOLDER_NAME = "kapt-classes-out"
        private const val CLASS_OUT_FOLDER_NAME = "class-out"
        private const val KAPT_PLUGIN_ID = "org.jetbrains.kotlin.kapt3"
    }
}

/** The list of processors to delegate to during the test compilation. */
private val delegateProcessors = ThreadLocal<List<Processor>>()

/** The max amount of processors we support during test is based on the amount of delegates */
private val PROCESSOR_DELEGATES_SIZE = TestDelegateProcessor::class.java.declaredClasses.size

/**
 * These delegate classes may seem unused but will be instantiated by KAPT via reflection and
 * through their no-arg constructor, and we use them to delegate to actual processors provided for
 * the test compilation. Note that the processor to delegate to is index based and obtained from the
 * thread local [delegateProcessors] that is set before compiler invocation.
 */
@Suppress("UNUSED")
sealed class TestDelegateProcessor(val delegate: Processor) : Processor by delegate {
    class KaptTestDelegateAP0 : TestDelegateProcessor(checkNotNull(delegateProcessors.get())[0])

    class KaptTestDelegateAP1 : TestDelegateProcessor(checkNotNull(delegateProcessors.get())[1])

    class KaptTestDelegateAP2 : TestDelegateProcessor(checkNotNull(delegateProcessors.get())[2])

    class KaptTestDelegateAP3 : TestDelegateProcessor(checkNotNull(delegateProcessors.get())[3])

    class KaptTestDelegateAP4 : TestDelegateProcessor(checkNotNull(delegateProcessors.get())[4])

    class KaptTestDelegateAP5 : TestDelegateProcessor(checkNotNull(delegateProcessors.get())[5])

    class KaptTestDelegateAP6 : TestDelegateProcessor(checkNotNull(delegateProcessors.get())[6])

    class KaptTestDelegateAP7 : TestDelegateProcessor(checkNotNull(delegateProcessors.get())[7])

    class KaptTestDelegateAP8 : TestDelegateProcessor(checkNotNull(delegateProcessors.get())[8])

    class KaptTestDelegateAP9 : TestDelegateProcessor(checkNotNull(delegateProcessors.get())[9])

    class KaptTestDelegateAP10 : TestDelegateProcessor(checkNotNull(delegateProcessors.get())[10])

    class KaptTestDelegateAP11 : TestDelegateProcessor(checkNotNull(delegateProcessors.get())[11])

    class KaptTestDelegateAP12 : TestDelegateProcessor(checkNotNull(delegateProcessors.get())[12])

    class KaptTestDelegateAP13 : TestDelegateProcessor(checkNotNull(delegateProcessors.get())[13])

    class KaptTestDelegateAP14 : TestDelegateProcessor(checkNotNull(delegateProcessors.get())[14])

    class KaptTestDelegateAP15 : TestDelegateProcessor(checkNotNull(delegateProcessors.get())[15])

    class KaptTestDelegateAP16 : TestDelegateProcessor(checkNotNull(delegateProcessors.get())[16])

    class KaptTestDelegateAP17 : TestDelegateProcessor(checkNotNull(delegateProcessors.get())[17])

    class KaptTestDelegateAP18 : TestDelegateProcessor(checkNotNull(delegateProcessors.get())[18])

    class KaptTestDelegateAP19 : TestDelegateProcessor(checkNotNull(delegateProcessors.get())[19])

    class KaptTestDelegateAP20 : TestDelegateProcessor(checkNotNull(delegateProcessors.get())[20])

    class KaptTestDelegateAP21 : TestDelegateProcessor(checkNotNull(delegateProcessors.get())[21])

    class KaptTestDelegateAP22 : TestDelegateProcessor(checkNotNull(delegateProcessors.get())[22])

    class KaptTestDelegateAP23 : TestDelegateProcessor(checkNotNull(delegateProcessors.get())[23])

    class KaptTestDelegateAP24 : TestDelegateProcessor(checkNotNull(delegateProcessors.get())[24])

    class KaptTestDelegateAP25 : TestDelegateProcessor(checkNotNull(delegateProcessors.get())[25])

    class KaptTestDelegateAP26 : TestDelegateProcessor(checkNotNull(delegateProcessors.get())[26])

    class KaptTestDelegateAP27 : TestDelegateProcessor(checkNotNull(delegateProcessors.get())[27])

    class KaptTestDelegateAP28 : TestDelegateProcessor(checkNotNull(delegateProcessors.get())[28])

    class KaptTestDelegateAP29 : TestDelegateProcessor(checkNotNull(delegateProcessors.get())[29])

    class KaptTestDelegateAP30 : TestDelegateProcessor(checkNotNull(delegateProcessors.get())[30])

    class KaptTestDelegateAP31 : TestDelegateProcessor(checkNotNull(delegateProcessors.get())[31])

    class KaptTestDelegateAP32 : TestDelegateProcessor(checkNotNull(delegateProcessors.get())[32])

    class KaptTestDelegateAP33 : TestDelegateProcessor(checkNotNull(delegateProcessors.get())[33])

    class KaptTestDelegateAP34 : TestDelegateProcessor(checkNotNull(delegateProcessors.get())[34])

    class KaptTestDelegateAP35 : TestDelegateProcessor(checkNotNull(delegateProcessors.get())[35])

    class KaptTestDelegateAP36 : TestDelegateProcessor(checkNotNull(delegateProcessors.get())[36])

    class KaptTestDelegateAP37 : TestDelegateProcessor(checkNotNull(delegateProcessors.get())[37])

    class KaptTestDelegateAP38 : TestDelegateProcessor(checkNotNull(delegateProcessors.get())[38])

    class KaptTestDelegateAP39 : TestDelegateProcessor(checkNotNull(delegateProcessors.get())[39])

    class KaptTestDelegateAP40 : TestDelegateProcessor(checkNotNull(delegateProcessors.get())[40])

    class KaptTestDelegateAP41 : TestDelegateProcessor(checkNotNull(delegateProcessors.get())[41])

    class KaptTestDelegateAP42 : TestDelegateProcessor(checkNotNull(delegateProcessors.get())[42])

    class KaptTestDelegateAP43 : TestDelegateProcessor(checkNotNull(delegateProcessors.get())[43])

    class KaptTestDelegateAP44 : TestDelegateProcessor(checkNotNull(delegateProcessors.get())[44])

    class KaptTestDelegateAP45 : TestDelegateProcessor(checkNotNull(delegateProcessors.get())[45])

    class KaptTestDelegateAP46 : TestDelegateProcessor(checkNotNull(delegateProcessors.get())[46])

    class KaptTestDelegateAP47 : TestDelegateProcessor(checkNotNull(delegateProcessors.get())[47])

    class KaptTestDelegateAP48 : TestDelegateProcessor(checkNotNull(delegateProcessors.get())[48])

    class KaptTestDelegateAP49 : TestDelegateProcessor(checkNotNull(delegateProcessors.get())[49])
}
