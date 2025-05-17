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

package androidx.navigation.safe.args.generator

import androidx.navigation.safe.args.generator.kotlin.KotlinCodeFile
import androidx.navigation.safe.args.generator.kotlin.KotlinNavWriter
import androidx.navigation.safe.args.generator.kotlin.toCodeFile
import androidx.navigation.safe.args.generator.models.Action
import androidx.navigation.safe.args.generator.models.Argument
import androidx.navigation.safe.args.generator.models.Destination
import androidx.navigation.safe.args.generator.models.ResReference
import androidx.room.compiler.processing.util.Source
import androidx.room.compiler.processing.util.compiler.TestCompilationArguments
import androidx.room.compiler.processing.util.compiler.compile
import com.google.common.truth.Truth.assertThat
import com.squareup.javapoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.TypeSpec
import java.nio.charset.Charset
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class KotlinNavWriterTest {
    @field:Rule @JvmField val temporaryFolder = TemporaryFolder()

    private fun generateDirectionsCodeFile(
        destination: Destination,
        parentDirectionsFileList: List<KotlinCodeFile>,
        useAndroidX: Boolean,
    ) =
        KotlinNavWriter(useAndroidX)
            .generateDirectionsCodeFile(destination, parentDirectionsFileList)

    private fun generateDirectionsTypeSpec(action: Action, useAndroidX: Boolean) =
        KotlinNavWriter(useAndroidX).generateDirectionTypeSpec(action)

    private fun generateArgsCodeFile(destination: Destination, useAndroidX: Boolean) =
        KotlinNavWriter(useAndroidX).generateArgsCodeFile(destination)

    private fun id(id: String) = ResReference("a.b", "id", id)

    private fun wrappedInnerClass(spec: TypeSpec): KotlinCodeFile =
        FileSpec.builder("a.b", "BoringWrapper").addType(spec).build().toCodeFile()

    private fun assertGeneratedCode(code: String, fullClassName: String) {
        val goldenFile = loadSourceFile(fullClassName, "expected/kotlin_nav_writer_test", "kt")
        if (System.getProperty("update_golden_files")?.toBoolean() == true) {
            goldenFile.writer().use { it.write(code) }
        }
        assertThat(code).isEqualTo(goldenFile.readText(Charset.defaultCharset()))
    }

    private fun assertCompilesWithoutError(codeFile: KotlinCodeFile, packageName: String = "a.b") {
        val compilation =
            compile(
                temporaryFolder.root,
                arguments =
                    TestCompilationArguments(
                        sources =
                            listOf(
                                Source.java(
                                    "$packageName.R",
                                    loadSourceString(
                                        "$packageName.R",
                                        packageName.replace(".", "/"),
                                        "java",
                                    ),
                                ),
                                Source.kotlin(codeFile.fileName() + ".kt", codeFile.toString()),
                            )
                    ),
            )
        assertThat(compilation.success).isTrue()
    }

    @Test
    fun testDirectionClassGeneration() {
        val action =
            Action(
                id("next"),
                id("destA"),
                listOf(
                    Argument("main", StringType),
                    Argument("mainInt", IntType),
                    Argument("optional", StringType, StringValue("bla")),
                    Argument("optionalInt", IntType, IntValue("239")),
                    Argument(
                        "optionalParcelable",
                        ObjectType("android.content.pm.ActivityInfo"),
                        NullValue,
                        true,
                    ),
                    Argument("parcelable", ObjectType("android.content.pm.ActivityInfo")),
                    Argument(
                        "innerData",
                        ObjectType("android.content.pm.ActivityInfo\$WindowLayout"),
                    ),
                ),
            )
        val actual = generateDirectionsTypeSpec(action, false)
        assertGeneratedCode(wrappedInnerClass(actual).toString(), "a.b.Next")
        assertCompilesWithoutError(wrappedInnerClass(actual))
    }

    @Test
    fun testDirectionsClassGeneration() {
        val nextAction =
            Action(
                id("next"),
                id("destA"),
                listOf(
                    Argument("main", StringType),
                    Argument("optional", StringType, StringValue("bla")),
                ),
            )

        val prevAction = Action(id("previous"), id("destB"), emptyList())

        val dest =
            Destination(
                null,
                ClassName.get("a.b", "MainFragment"),
                "fragment",
                listOf(),
                listOf(prevAction, nextAction),
            )

        val actual = generateDirectionsCodeFile(dest, emptyList(), false)
        assertGeneratedCode(actual.toString(), "a.b.MainFragmentDirections")
        assertCompilesWithoutError(actual)
    }

    @Test
    fun testDirectionsClassGenerationDefaultParamOrder() {
        val nextAction =
            Action(
                id("next"),
                id("destA"),
                listOf(
                    Argument("optional", StringType, StringValue("bla")),
                    Argument("optionalFloat", FloatType, FloatValue("0.1")),
                    Argument("main", StringType),
                    Argument("optionalInt", IntType, IntValue("1")),
                ),
            )

        val prevAction = Action(id("previous"), id("destB"), emptyList())

        val dest =
            Destination(
                null,
                ClassName.get("a.b", "MainFragmentDefaultParam"),
                "fragment",
                listOf(),
                listOf(prevAction, nextAction),
            )

        val actual = generateDirectionsCodeFile(dest, emptyList(), false)
        assertGeneratedCode(actual.toString(), "a.b.MainFragmentDefaultParamDirections")
    }

    @Test
    fun testDirectionsClassGeneration_withKeywordId() {
        val funAction = Action(ResReference("fun.is.in", "id", "next"), id("destA"), listOf())

        val dest =
            Destination(
                null,
                ClassName.get("a.b", "FunFragment"),
                "fragment",
                listOf(),
                listOf(funAction),
            )

        val actual = generateDirectionsCodeFile(dest, emptyList(), false)
        assertGeneratedCode(actual.toString(), "a.b.FunFragmentDirections")
        assertCompilesWithoutError(actual, "fun.is.in")
    }

    @Test
    fun testDirectionsClassGeneration_longPackage() {
        val funAction =
            Action(
                ResReference(
                    "a.b.secondreallyreallyreallyreally" +
                        "reallyreallyreallyreallyreallyreallyreallyreallyreallyreallyreallyreally" +
                        "longpackage",
                    "id",
                    "next",
                ),
                id("destA"),
                listOf(),
            )

        val dest =
            Destination(
                null,
                ClassName.get(
                    "a.b.reallyreallyreallyreally" +
                        "reallyreallyreallyreallyreallyreallyreallyreallyreallyreallyreallyreally" +
                        "longpackage",
                    "LongPackageFragment",
                ),
                "fragment",
                listOf(),
                listOf(funAction),
            )

        val actual = generateDirectionsCodeFile(dest, emptyList(), false)
        assertGeneratedCode(
            actual.toString(),
            "a.b.reallyreallyreallyreallyreally" +
                "reallyreallyreallyreallyreallyreallyreallyreallyreallyreallyreally" +
                "longpackage.LongPackageFragmentDirections",
        )
        assertCompilesWithoutError(
            actual,
            "a.b.secondreallyreallyreallyreallyreallyreally" +
                "reallyreallyreallyreallyreallyreallyreallyreallyreallyreallylongpackage",
        )
    }

    @Test
    fun testArgumentsClassGeneration() {
        val dest =
            Destination(
                null,
                ClassName.get("a.b", "MainFragment"),
                "fragment",
                listOf(
                    Argument("main", StringType),
                    Argument("optional", IntType, IntValue("-1")),
                    Argument(
                        "reference",
                        ReferenceType,
                        ReferenceValue(ResReference("a.b", "drawable", "background")),
                    ),
                    Argument("referenceZeroDefaultValue", ReferenceType, IntValue("0")),
                    Argument("floatArg", FloatType, FloatValue("1")),
                    Argument("floatArrayArg", FloatArrayType),
                    Argument(
                        "objectArrayArgument",
                        ObjectArrayType("android.content.pm.ActivityInfo"),
                    ),
                    Argument("boolArg", BoolType, BooleanValue("true")),
                    Argument(
                        "optionalParcelable",
                        ObjectType("android.content.pm.ActivityInfo"),
                        NullValue,
                        true,
                    ),
                    Argument(
                        "enumArg",
                        ObjectType("java.nio.file.AccessMode"),
                        EnumValue(ObjectType("java.nio.file.AccessMode"), "READ"),
                        false,
                    ),
                ),
                listOf(),
            )

        val actual = generateArgsCodeFile(dest, false)
        assertGeneratedCode(actual.toString(), "a.b.MainFragmentArgs")
        assertCompilesWithoutError(actual)
    }

    @Test
    fun testArgumentClassGeneration_longArgumentName() {
        val dest =
            Destination(
                null,
                ClassName.get(
                    "a.b",
                    "ReallyReallyReallyReallyReallyReallyReallyReallyReallyReallyReallyReallyReally" +
                        "ReallyLongNameFragment",
                ),
                "fragment",
                listOf(Argument("main", StringType)),
                listOf(),
            )

        val actual = generateArgsCodeFile(dest, false)
        assertGeneratedCode(
            actual.toString(),
            "a.b.ReallyReallyReallyReallyReally" +
                "ReallyReallyReallyReallyReallyReallyReallyReallyReallyLongNameMainFragmentArgs",
        )
        assertCompilesWithoutError(
            actual,
            "a.b.secondreallyreallyreallyreallyreallyreally" +
                "reallyreallyreallyreallyreallyreallyreallyreallyreallyreallylongpackage",
        )
    }
}
