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

package androidx.room.compiler.processing.util

import androidx.room.compiler.processing.ExperimentalProcessingApi
import androidx.room.compiler.processing.SyntheticJavacProcessor
import androidx.room.compiler.processing.SyntheticKspProcessor
import androidx.room.compiler.processing.XElement
import androidx.room.compiler.processing.XProcessingEnv
import androidx.room.compiler.processing.XProcessingStep
import androidx.room.compiler.processing.javac.JavacBasicAnnotationProcessor
import androidx.room.compiler.processing.ksp.KspBasicAnnotationProcessor
import androidx.room.compiler.processing.util.compiler.KotlinCliRunner
import androidx.room.compiler.processing.util.compiler.TestCompilationArguments
import androidx.room.compiler.processing.util.compiler.compile
import com.google.common.truth.Truth.assertThat
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.CodeBlock
import com.squareup.javapoet.JavaFile
import com.squareup.javapoet.TypeSpec
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.KModifier
import java.net.URLClassLoader
import java.nio.file.Files
import javax.lang.model.element.Modifier
import javax.tools.Diagnostic
import org.junit.Ignore
import org.junit.Test

@OptIn(ExperimentalProcessingApi::class)
class TestRunnerTest {
    @Test
    fun compileFilesForClasspath() {
        val kotlinSource =
            Source.kotlin(
                "Foo.kt",
                """
            class KotlinClass1
            class KotlinClass2
            """
                    .trimIndent(),
            )
        val javaSource =
            Source.java(
                "foo.bar.JavaClass1",
                """
            package foo.bar;
            public class JavaClass1 {}
            """
                    .trimIndent(),
            )

        val kspProcessorProvider =
            SyntheticKspProcessor.Provider { invocation ->
                if (invocation.processingEnv.findTypeElement("gen.GeneratedKotlin") == null) {
                    invocation.processingEnv.filer.write(
                        FileSpec.builder("gen", "KotlinGen")
                            .addType(
                                com.squareup.kotlinpoet.TypeSpec.classBuilder("GeneratedKotlin")
                                    .build()
                            )
                            .build()
                    )
                }
            }

        val javaProcessor = SyntheticJavacProcessor { invocation ->
            if (invocation.processingEnv.findTypeElement("gen.GeneratedJava") == null) {
                invocation.processingEnv.filer.write(
                    JavaFile.builder("gen", TypeSpec.classBuilder("GeneratedJava").build()).build()
                )
            }
        }

        val classpaths =
            compile(
                    workingDir = Files.createTempDirectory("test-runner").toFile(),
                    arguments =
                        TestCompilationArguments(
                            sources = listOf(kotlinSource, javaSource),
                            symbolProcessorProviders = listOf(kspProcessorProvider),
                            kaptProcessors = listOf(javaProcessor),
                        ),
                )
                .outputClasspath
        val classLoader =
            URLClassLoader.newInstance(classpaths.map { it.toURI().toURL() }.toTypedArray())

        // try loading generated classes. If any of them fails, it will throw and fail the test
        classLoader.loadClass("KotlinClass1")
        classLoader.loadClass("KotlinClass2")
        classLoader.loadClass("foo.bar.JavaClass1")
        classLoader.loadClass("gen.GeneratedKotlin")
        classLoader.loadClass("gen.GeneratedJava")
    }

    @Test fun generatedBadCode_expected() = generatedBadCode(assertFailure = true)

    @Test(expected = AssertionError::class)
    fun generatedBadCode_unexpected() = generatedBadCode(assertFailure = false)

    @Test
    fun options() {
        val testOptions = mapOf("a" to "b", "c" to "d")
        val handler: (XTestInvocation) -> Unit = {
            assertThat(it.processingEnv.options).containsAtLeastEntriesIn(testOptions)
        }
        runJavaProcessorTest(sources = emptyList(), options = testOptions, handler = handler)
        runKaptTest(sources = emptyList(), options = testOptions, handler = handler)
        runKspTest(sources = emptyList(), options = testOptions, handler = handler)
    }

    private fun generatedBadCode(assertFailure: Boolean) {
        val badCode =
            TypeSpec.classBuilder("Foo").apply { addStaticBlock(CodeBlock.of("bad code")) }.build()
        val badGeneratedFile = JavaFile.builder("foo", badCode).build()
        runProcessorTest {
            if (it.processingEnv.findTypeElement("foo.Foo") == null) {
                it.processingEnv.filer.write(badGeneratedFile)
            }
            if (assertFailure) {
                it.assertCompilationResult {
                    compilationDidFail()
                    hasErrorContaining("';' expected")
                        .onSource(Source.java("foo.Foo", badGeneratedFile.toString()))
                }
            }
        }
    }

    @Test fun reportedError_expected() = reportedError(assertFailure = true)

    @Test(expected = AssertionError::class)
    fun reportedError_unexpected() = reportedError(assertFailure = false)

    private fun reportedError(assertFailure: Boolean) {
        runProcessorTest {
            it.processingEnv.messager.printMessage(
                kind = Diagnostic.Kind.ERROR,
                msg = "reported error",
            )
            if (assertFailure) {
                it.assertCompilationResult { hasError("reported error") }
            }
        }
    }

    @Test
    fun accessGeneratedCode() {
        val kotlinSource =
            Source.kotlin(
                "KotlinSubject.kt",
                """
                val x: ToBeGeneratedKotlin? = null
                val y: ToBeGeneratedJava? = null
            """
                    .trimIndent(),
            )
        val javaSource =
            Source.java(
                "JavaSubject",
                """
                public class JavaSubject {
                    public static ToBeGeneratedKotlin x;
                    public static ToBeGeneratedJava y;
                }
            """
                    .trimIndent(),
            )
        runProcessorTest(sources = listOf(kotlinSource, javaSource)) { invocation ->
            invocation.processingEnv.findTypeElement("ToBeGeneratedJava").let {
                if (it == null) {
                    invocation.processingEnv.filer.write(
                        JavaFile.builder(
                                "",
                                TypeSpec.classBuilder("ToBeGeneratedJava")
                                    .apply { addModifiers(Modifier.PUBLIC) }
                                    .build(),
                            )
                            .build()
                    )
                }
            }
            invocation.processingEnv.findTypeElement("ToBeGeneratedKotlin").let {
                if (it == null) {
                    invocation.processingEnv.filer.write(
                        FileSpec.builder("", "Foo")
                            .addType(
                                com.squareup.kotlinpoet.TypeSpec.classBuilder("ToBeGeneratedKotlin")
                                    .apply { addModifiers(KModifier.PUBLIC) }
                                    .build()
                            )
                            .build()
                    )
                }
            }
        }
    }

    @Test
    fun syntacticErrorsAreVisibleInTheErrorMessage_java() {
        val src =
            Source.java(
                "test.Foo",
                """
            package test;
            // static here is invalid, causes a Java syntax error
            public static class Foo {}
            """
                    .trimIndent(),
            )
        val errorMessage = "modifier static not allowed here"
        val javapResult = runCatching {
            runJavaProcessorTest(sources = listOf(src), classpath = emptyList()) {}
        }
        assertThat(javapResult.exceptionOrNull()).hasMessageThat().contains(errorMessage)

        val kaptResult = runCatching { runKaptTest(sources = listOf(src)) {} }
        assertThat(kaptResult.exceptionOrNull()).hasMessageThat().contains(errorMessage)

        if (CompilationTestCapabilities.canTestWithKsp) {
            val kspResult = runCatching { runKspTest(sources = listOf(src)) {} }
            assertThat(kspResult.exceptionOrNull()).hasMessageThat().contains(errorMessage)
        }
    }

    @Test
    fun syntacticErrorsAreVisibleInTheErrorMessage_kotlin() {
        val src =
            Source.kotlin(
                "Foo.kt",
                """
            package foo;
            bad code
            """
                    .trimIndent(),
            )
        val errorMessage = "Expecting a top level declaration"
        val kaptResult = runCatching { runKaptTest(sources = listOf(src)) {} }
        assertThat(kaptResult.exceptionOrNull()).hasMessageThat().contains(errorMessage)

        if (CompilationTestCapabilities.canTestWithKsp) {
            val kspResult = runCatching { runKspTest(sources = listOf(src)) {} }
            assertThat(kspResult.exceptionOrNull()).hasMessageThat().contains(errorMessage)
        }
    }

    @Test
    fun javacArguments() {
        val src =
            Source.java(
                "Foo",
                """
            public class Foo {
            }
            """
                    .trimIndent(),
            )
        runProcessorTest(sources = listOf(src), javacArguments = listOf("-Werror")) { invocation ->
            invocation.processingEnv.messager.printMessage(Diagnostic.Kind.WARNING, "some warning")
            invocation.assertCompilationResult {
                if (invocation.isKsp) {
                    // warning happens during ksp but Werror is only passed into javac so this
                    // shouldn't fail
                } else {
                    compilationDidFail()
                }
            }
        }
    }

    @Test
    fun kotlincArguments() {
        val src =
            Source.kotlin(
                "Foo.kt",
                """
            class Foo
            """
                    .trimIndent(),
            )
        runProcessorTest(
            sources = listOf(src),
            // TODO(b/314151707): We got warning: "K2 kapt is in Alpha. Use with caution."
            kotlincArguments = listOf("-Werror") + KOTLINC_LANGUAGE_1_9_ARGS,
            javacArguments = listOf("-Werror"), // needed for kapt as it uses javac,
        ) { invocation ->
            invocation.processingEnv.messager.printMessage(Diagnostic.Kind.WARNING, "some warning")
            invocation.assertCompilationResult {
                // either kapt or ksp, compilation should still fail due to the warning printed
                // by the processor
                compilationDidFail()
            }
        }
    }

    @Test
    fun jvmModuleName() {
        val src =
            Source.kotlin(
                "Foo.kt",
                """
            class Foo {
                internal fun f() {}
            }
            """
                    .trimIndent(),
            )
        val lib =
            Source.kotlin(
                "Bar.kt",
                """
            class Bar {
                internal fun f() {}
            }
            """
                    .trimMargin(),
            )
        val classpath =
            compileFiles(
                listOf(lib),
                // To workaround https://github.com/google/ksp/issues/2105.
                kotlincArguments = listOf("-module-name", "lib"),
            )
        runProcessorTest(sources = listOf(src), classpath = classpath) { invocation ->
            invocation.processingEnv.requireTypeElement("Foo").let { cls ->
                assertThat(cls.getDeclaredMethods().single().jvmName).isEqualTo("f\$main")
            }
            invocation.processingEnv.requireTypeElement("Bar").let { cls ->
                assertThat(cls.getDeclaredMethods().single().jvmName).isEqualTo("f\$lib")
            }
        }
        runProcessorTest(
            sources = listOf(src),
            classpath = classpath,
            kotlincArguments = listOf("-module-name", "test"),
        ) { invocation ->
            invocation.processingEnv.requireTypeElement("Foo").let { cls ->
                assertThat(cls.getDeclaredMethods().single().jvmName).isEqualTo("f\$test")
            }
            invocation.processingEnv.requireTypeElement("Bar").let { cls ->
                assertThat(cls.getDeclaredMethods().single().jvmName).isEqualTo("f\$lib")
            }
        }
    }

    @Test
    fun correctErrorTypes() {
        val subjectSrc =
            Source.kotlin(
                "Foo.kt",
                """
            class Foo {
                val errorField : DoesNotExist = TODO()
            }
            """
                    .trimIndent(),
            )

        runKaptTest(sources = listOf(subjectSrc)) { invocation ->
            val field =
                invocation.processingEnv.requireTypeElement("Foo").getDeclaredFields().single()
            assertThat(field.type.typeName.toString()).isEqualTo("error.NonExistentClass")
            invocation.assertCompilationResult { this.hasErrorContaining("Unresolved reference") }
        }

        runKaptTest(
            sources = listOf(subjectSrc),
            kotlincArguments =
                listOf("-P", "plugin:org.jetbrains.kotlin.kapt3:correctErrorTypes=true"),
        ) { invocation ->
            val field =
                invocation.processingEnv.requireTypeElement("Foo").getDeclaredFields().single()
            assertThat(field.type.typeName.toString()).isEqualTo("DoesNotExist")
            invocation.assertCompilationResult { this.hasErrorContaining("Unresolved reference") }
        }
    }

    @Ignore // This is for Kotlin 2.1.0+.
    @Test
    fun testK2Flags() {
        val src =
            Source.kotlin(
                "Foo.kt",
                """
                annotation class Test
                @Test
                class Cat {
                    fun purr() {
                        println("Purr purr")
                    }
                }
                fun petAnimal(animal: Any) {
                    val isCat = animal is Cat
                    if (isCat) {
                        // This smart cast is a K2 feature. Example was taken from the Kotlin doc.
                        animal.purr()
                    }
                }
                """
                    .trimIndent(),
            )
        // Should succeed with K2 compiler.
        runProcessorTest(
            sources = listOf(src),
            kotlincArguments = listOf("-language-version=2.0", "-api-version=2.0"),
        ) { invocation ->
            invocation.assertCompilationResult { hasErrorCount(0) }
        }
        // Should fail with K1 compiler.
        runProcessorTest(
            sources = listOf(src),
            kotlincArguments = listOf("-language-version=1.9", "-api-version=1.9"),
        ) { invocation ->
            invocation.assertCompilationResult {
                hasErrorCount(1)
                hasErrorContaining("Unresolved reference: purr")
            }
        }
        // We use K2 compiler by default so this should succeed. We also use K1 KAPT by default.
        // This will also be the case for Kotlin 2.1:
        // https://youtrack.jetbrains.com/issue/KT-70879/Kapt-check-that-Kotlin-2.1-language-features-are-ignored-correctly-by-K1-kapt
        runProcessorTest(sources = listOf(src)) { invocation ->
            invocation.assertCompilationResult { hasErrorCount(0) }
        }
        // We can enable K2 KAPT with an extra flag.
        runProcessorTest(sources = listOf(src), kotlincArguments = listOf("-Xuse-k2-kapt")) {
            invocation ->
            invocation.assertCompilationResult { hasErrorCount(0) }
        }
        // K2 KAPT doesn't work with K1 compiler. It's always K1 KAPT when language-version < 2.
        runProcessorTest(
            sources = listOf(src),
            kotlincArguments = listOf("-language-version=1.9", "-api-version=1.9", "-Xuse-k2-kapt"),
        ) { invocation ->
            invocation.assertCompilationResult {
                hasWarning("-Xuse-k2-kapt flag can be only used with language version 2.0+.")
                hasErrorContaining("Unresolved reference: purr")
            }
        }
        // Our test infra uses KSP2 by default.
        runKspTest(sources = listOf(src)) { invocation -> assertThat(invocation.isKsp2).isTrue() }
        // Users can switch between KSP1 and KSP2 based on language-version.
        runKspTest(
            sources = listOf(src),
            kotlincArguments = listOf("-language-version=2.0", "-api-version=2.0"),
        ) { invocation ->
            assertThat(invocation.isKsp2).isTrue()
        }
        // When setting language-version < 2 it's always KSP1 and K1 KAPT.
        runKspTest(
            sources = listOf(src),
            kotlincArguments = listOf("-language-version=1.9", "-api-version=1.9"),
        ) { invocation ->
            assertThat(invocation.isKsp2).isFalse()
            invocation.assertCompilationResult { hasErrorContaining("Unresolved reference: purr") }
        }
    }

    @Test
    fun testPluginOptions() {
        KotlinCliRunner.getPluginOptions(
                "org.jetbrains.kotlin.kapt3",
                listOf("-P", "plugin:org.jetbrains.kotlin.kapt3:correctErrorTypes=true"),
            )
            .let { options -> assertThat(options).containsExactly("correctErrorTypes", "true") }

        // zero args
        KotlinCliRunner.getPluginOptions("org.jetbrains.kotlin.kapt3", emptyList()).let { options ->
            assertThat(options).isEmpty()
        }

        // odd number of args
        KotlinCliRunner.getPluginOptions(
                "org.jetbrains.kotlin.kapt3",
                listOf("-P", "plugin:org.jetbrains.kotlin.kapt3:correctErrorTypes=true", "-verbose"),
            )
            .let { options -> assertThat(options).containsExactly("correctErrorTypes", "true") }

        // illegal format (missing "=")
        KotlinCliRunner.getPluginOptions(
                "org.jetbrains.kotlin.kapt3",
                listOf("-P", "plugin:org.jetbrains.kotlin.kapt3:correctErrorTypestrue"),
            )
            .let { options -> assertThat(options).isEmpty() }

        // illegal format (missing "-P")
        KotlinCliRunner.getPluginOptions(
                "org.jetbrains.kotlin.kapt3",
                listOf("plugin:org.jetbrains.kotlin.kapt3:correctErrorTypestrue"),
            )
            .let { options -> assertThat(options).isEmpty() }

        // illegal format (wrong plugin id)
        KotlinCliRunner.getPluginOptions(
                "org.jetbrains.kotlin.kapt3",
                listOf("-P", "plugin:abc:correctErrorTypes=true"),
            )
            .let { options -> assertThat(options).isEmpty() }

        KotlinCliRunner.getPluginOptions(
                "org.jetbrains.kotlin.kapt3",
                listOf(
                    "-P",
                    "plugin:org.jetbrains.kotlin.kapt3:correctErrorTypes=true",
                    "-P",
                    "plugin:org.jetbrains.kotlin.kapt3:sources=build/kapt/sources",
                ),
            )
            .let { options ->
                assertThat(options)
                    .containsExactly("correctErrorTypes", "true", "sources", "build/kapt/sources")
            }
    }

    @Test
    fun generatedSourceSubject() {
        runProcessorTest { invocation ->
            if (invocation.processingEnv.findTypeElement("Subject") == null) {
                invocation.processingEnv.filer.write(
                    JavaFile.builder("", TypeSpec.classBuilder("Subject").build()).build()
                )
            }
            invocation.assertCompilationResult {
                generatedSourceFileWithPath("Subject.java").contains("class Subject")
            }
        }
    }

    @Test
    fun actualProcessors() {
        val src =
            Source.kotlin(
                "Foo.kt",
                """
            annotation class Annotated

            @Annotated
            class Foo
            """
                    .trimIndent(),
            )
        class TestStep : XProcessingStep {
            var rounds = 0

            override fun annotations(): Set<String> {
                return setOf("Annotated")
            }

            override fun process(
                env: XProcessingEnv,
                elementsByAnnotation: Map<String, Set<XElement>>,
                isLastRound: Boolean,
            ): Set<XElement> {
                if (rounds == 0) {
                    val javaFile =
                        JavaFile.builder(
                                "",
                                TypeSpec.classBuilder("GenClass")
                                    .addAnnotation(ClassName.get("", "Annotated"))
                                    .build(),
                            )
                            .build()
                    env.filer.write(javaFile)
                }
                rounds++
                return emptySet()
            }
        }
        val javacStep = TestStep()
        val testJavacProcessor =
            object : JavacBasicAnnotationProcessor() {
                override fun processingSteps() = listOf(javacStep)
            }
        val kspStep = TestStep()
        val testKspProcessorProvider = SymbolProcessorProvider { environment ->
            object : KspBasicAnnotationProcessor(environment) {
                override fun processingSteps() = listOf(kspStep)
            }
        }
        var onCompilationResultInvoked = 0
        runProcessorTest(
            sources = listOf(src),
            javacProcessors = listOf(testJavacProcessor),
            symbolProcessorProviders = listOf(testKspProcessorProvider),
        ) {
            onCompilationResultInvoked++
            it.hasErrorCount(0)
        }
        assertThat(onCompilationResultInvoked).isEqualTo(2) // 2 backends
        assertThat(javacStep.rounds).isEqualTo(3) // 2 rounds + final
        assertThat(kspStep.rounds).isEqualTo(3) // 2 rounds + final
    }

    @Test
    fun kspOnlyProcessor() {
        val src =
            Source.kotlin(
                "Foo.kt",
                """
            annotation class Annotated

            @Annotated
            class Foo
            """
                    .trimIndent(),
            )
        var processingRounds = 0
        val testKspProcessorProvider = SymbolProcessorProvider { environment ->
            object : KspBasicAnnotationProcessor(environment) {
                override fun processingSteps() =
                    listOf(
                        object : XProcessingStep {
                            override fun annotations() = setOf("Annotated")

                            override fun process(
                                env: XProcessingEnv,
                                elementsByAnnotation: Map<String, Set<XElement>>,
                                isLastRound: Boolean,
                            ): Set<XElement> {
                                if (env.findTypeElement("GenClass") == null) {
                                    val typeSpec =
                                        TypeSpec.classBuilder("GenClass")
                                            .addAnnotation(ClassName.get("", "Annotated"))
                                            .build()
                                    env.filer.write(JavaFile.builder("", typeSpec).build())
                                }
                                processingRounds++
                                return emptySet()
                            }
                        }
                    )
            }
        }
        var onCompilationResultInvoked = 0
        runKspProcessorTest(
            sources = listOf(src),
            symbolProcessorProviders = listOf(testKspProcessorProvider),
        ) {
            onCompilationResultInvoked++
            it.hasErrorCount(0)
        }
        assertThat(onCompilationResultInvoked).isEqualTo(1) // 1 backend
        assertThat(processingRounds).isEqualTo(3) // 2 rounds + final
    }
}
