/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.binarycompatibilityvalidator

import com.google.common.truth.Truth.assertThat
import kotlin.test.assertFailsWith
import org.jetbrains.kotlin.library.abi.AbiClassKind
import org.jetbrains.kotlin.library.abi.AbiCompoundName
import org.jetbrains.kotlin.library.abi.AbiModality
import org.jetbrains.kotlin.library.abi.AbiProperty
import org.jetbrains.kotlin.library.abi.AbiQualifiedName
import org.jetbrains.kotlin.library.abi.AbiSignatureVersion
import org.jetbrains.kotlin.library.abi.ExperimentalLibraryAbiReader
import org.jetbrains.kotlin.library.abi.LibraryAbi
import org.junit.Test

@OptIn(ExperimentalLibraryAbiReader::class)
class KlibDumpParserTest {

    private val collectionDump = getJavaResource("collection.txt").readText()
    private val datastoreCoreDump = getJavaResource("datastore.txt").readText()
    private val annotationDump = getJavaResource("annotation.txt").readText()
    private val datastorePreferencesDump = getJavaResource("datastore-preferences.txt").readText()
    private val uniqueTargetDump = getJavaResource("unique_targets.txt").readText()

    @Test
    fun parseASimpleClass() {
        val input =
            "final class <#A: kotlin/Any?, #B: kotlin/Any?> " +
                "androidx.collection/MutableScatterMap : androidx.collection/ScatterMap<#A, #B>"
        val parsed = KlibDumpParser(input).parseClass()
        assertThat(parsed).isNotNull()

        assertThat(parsed.qualifiedName.toString())
            .isEqualTo("androidx.collection/MutableScatterMap")
    }

    @Test
    fun parseAClassWithTwoSuperTypes() {
        val input =
            "final class <#A: kotlin/Any?> androidx.collection/ArraySet : " +
                "kotlin.collections/MutableCollection<#A>, kotlin.collections/MutableSet<#A>"
        val parsed = KlibDumpParser(input).parseClass()
        assertThat(parsed).isNotNull()

        assertThat(parsed.qualifiedName.toString()).isEqualTo("androidx.collection/ArraySet")
        assertThat(parsed.superTypes).hasSize(2)
    }

    @Test
    fun parseAClassWithTypeParams() {
        val input =
            "final class <#A: kotlin/Any?, #B: kotlin/Any?> androidx.collection/" +
                "MutableScatterMap : androidx.collection/ScatterMap<#A, #B>"
        val parsed = KlibDumpParser(input).parseClass()
        assertThat(parsed).isNotNull()

        assertThat(parsed.qualifiedName.toString())
            .isEqualTo("androidx.collection/MutableScatterMap")
        assertThat(parsed.typeParameters).hasSize(2)
        parsed.typeParameters.forEach {
            assertThat(it.upperBounds.single().className?.toString()).isEqualTo("kotlin/Any")
        }
    }

    @Test
    fun parseAClassWithATypeArg() {
        val input = "final class my.lib/MySubClass : my.lib/MyClass<kotlin/Int>"
        val parsed = KlibDumpParser(input).parseClass()

        assertThat(parsed.typeParameters).isEmpty()
        assertThat(parsed.superTypes).hasSize(1)
        val superType = parsed.superTypes.single()
        assertThat(superType.arguments?.single()?.type?.classNameOrTag).isEqualTo("kotlin/Int")
    }

    @Test
    fun parseAClassBug() {
        val input =
            """
            abstract interface androidx.graphics.shapes/MutablePoint { // androidx.graphics.shapes/MutablePoint|null[0]
                abstract var x // androidx.graphics.shapes/MutablePoint.x|{}x[0]
                    abstract fun <get-x>(): kotlin/Float // androidx.graphics.shapes/MutablePoint.x.<get-x>|<get-x>(){}[0]
                    abstract fun <set-x>(kotlin/Float) // androidx.graphics.shapes/MutablePoint.x.<set-x>|<set-x>(kotlin.Float){}[0]
                abstract var y // androidx.graphics.shapes/MutablePoint.y|{}y[0]
                    abstract fun <get-y>(): kotlin/Float // androidx.graphics.shapes/MutablePoint.y.<get-y>|<get-y>(){}[0]
                    abstract fun <set-y>(kotlin/Float) // androidx.graphics.shapes/MutablePoint.y.<set-y>|<set-y>(kotlin.Float){}[0]
            }
        """
                .trimIndent()
        val parsed = KlibDumpParser(input).parseClass()
        assertThat(parsed.declarations.filterIsInstance<AbiProperty>()).hasSize(2)
    }

    @Test
    fun parseAnAnnotationClass() {
        val input = "open annotation class my.lib/MyClass : kotlin/Annotation"
        val parsed = KlibDumpParser(input).parseClass()
        assertThat(parsed).isNotNull()

        assertThat(parsed.qualifiedName.toString()).isEqualTo("my.lib/MyClass")
        assertThat(parsed.kind).isEqualTo(AbiClassKind.ANNOTATION_CLASS)
    }

    @Test
    fun parseAFunction() {
        val input =
            "final inline fun <#A1: kotlin/Any?> " +
                "fold(#A1, kotlin/Function2<#A1, #A, #A1>): #A1"
        val parentQName =
            AbiQualifiedName(AbiCompoundName("androidx.collection"), AbiCompoundName("ObjectList"))
        val parsed = KlibDumpParser(input).parseFunction(parentQName)
        assertThat(parsed).isNotNull()

        assertThat(parsed.qualifiedName.toString()).isEqualTo("androidx.collection/ObjectList.fold")
    }

    @Test
    fun parseAFunctionWithTypeArgsOnParams() {
        val input =
            "final fun <#A: kotlin/Any?> " +
                "androidx.collection/arraySetOf(kotlin/Array<out #A>...): " +
                "androidx.collection/ArraySet<#A>"
        val parsed = KlibDumpParser(input).parseFunction()
        assertThat(parsed).isNotNull()

        assertThat(parsed.qualifiedName.toString()).isEqualTo("androidx.collection/arraySetOf")
        val param = parsed.valueParameters.single()
        assertThat(param.type.arguments).isNotEmpty()
    }

    @Test
    fun parseAGetterFunction() {
        val input = "final inline fun <get-indices>(): kotlin.ranges/IntRange"
        val parentQName =
            AbiQualifiedName(AbiCompoundName("androidx.collection"), AbiCompoundName("ObjectList"))
        val parsed = KlibDumpParser(input).parseFunction(parentQName, isGetterOrSetter = true)
        assertThat(parsed.qualifiedName.toString())
            .isEqualTo("androidx.collection/ObjectList.<get-indices>")
    }

    @Test
    fun parseAGetterFunctionWithReceiver() {
        val input =
            "final inline fun <#A1: kotlin/Any?> " +
                "(androidx.collection/LongSparseArray<#A1>).<get-size>(): kotlin/Int"
        val parentQName =
            AbiQualifiedName(AbiCompoundName("androidx.collection"), AbiCompoundName("ObjectList"))
        val parsed = KlibDumpParser(input).parseFunction(parentQName, isGetterOrSetter = true)
        assertThat(parsed.hasExtensionReceiverParameter).isTrue()
    }

    @Test
    fun parseAFunctionWithTypeArgAsReceiver() {
        val input =
            "final inline fun <#A: androidx.datastore.core/Closeable, #B: kotlin/Any?> " +
                "(#A).androidx.datastore.core/use(kotlin/Function1<#A, #B>): #B"
        val parsed = KlibDumpParser(input).parseFunction()
        assertThat(parsed.hasExtensionReceiverParameter).isTrue()
        assertThat(parsed.typeParameters).hasSize(2)
    }

    @Test
    fun parseAComplexFunction() {
        val input =
            "final inline fun <#A: kotlin/Any, #B: kotlin/Any> androidx.collection/" +
                "lruCache(kotlin/Int, crossinline kotlin/Function2<#A, #B, kotlin/Int> =..., " +
                "crossinline kotlin/Function1<#A, #B?> =..., " +
                "crossinline kotlin/Function4<kotlin/Boolean, #A, #B, #B?, kotlin/Unit> =...): " +
                "androidx.collection/LruCache<#A, #B>"
        val parsed = KlibDumpParser(input).parseFunction()
        assertThat(parsed.modality).isEqualTo(AbiModality.FINAL)
        assertThat(parsed.typeParameters).hasSize(2)
        assertThat(parsed.qualifiedName.toString()).isEqualTo("androidx.collection/lruCache")
        assertThat(parsed.valueParameters).hasSize(4)
    }

    @Test
    fun parseAComplexFunctionWithK2Formatting() {
        val input =
            "final inline fun <#A: kotlin/Any, #B: kotlin/Any> androidx.collection/" +
                "lruCache(kotlin/Int, crossinline kotlin/Function2<#A, #B, kotlin/Int> = ..., " +
                "crossinline kotlin/Function1<#A, #B?> = ..., " +
                "crossinline kotlin/Function4<kotlin/Boolean, #A, #B, #B?, kotlin/Unit> = ...): " +
                "androidx.collection/LruCache<#A, #B>"
        val parsed = KlibDumpParser(input).parseFunction()
        assertThat(parsed.modality).isEqualTo(AbiModality.FINAL)
        assertThat(parsed.typeParameters).hasSize(2)
        assertThat(parsed.qualifiedName.toString()).isEqualTo("androidx.collection/lruCache")
        assertThat(parsed.valueParameters).hasSize(4)
    }

    @Test
    fun parseANestedValProperty() {
        val input = "final val size\n        final fun <get-size>(): kotlin/Int"
        val parsed =
            KlibDumpParser(input)
                .parseProperty(
                    AbiQualifiedName(
                        AbiCompoundName("androidx.collection"),
                        AbiCompoundName("ScatterMap")
                    )
                )
        assertThat(parsed.getter).isNotNull()
        assertThat(parsed.setter).isNull()
    }

    @Test
    fun parseANestedVarProperty() {
        val input =
            "final var keys\n" +
                "        final fun <get-keys>(): kotlin/Array<kotlin/Any?>\n" +
                "        final fun <set-keys>(kotlin/Array<kotlin/Any?>)"
        val parsed =
            KlibDumpParser(input)
                .parseProperty(
                    AbiQualifiedName(
                        AbiCompoundName("androidx.collection"),
                        AbiCompoundName("ScatterMap")
                    )
                )
        assertThat(parsed.getter).isNotNull()
        assertThat(parsed.setter).isNotNull()
    }

    @Test
    fun parseAPropertyWithStarParamsInReceiver() {
        val input =
            """
            final val androidx.compose.animation.core/isFinished
                final fun (androidx.compose.animation.core/AnimationState<*, *>).<get-isFinished>(): kotlin/Boolean
        """
                .trimIndent()
        val parsed = KlibDumpParser(input).parseProperty()
        assertThat(parsed.getter).isNotNull()
        assertThat(parsed.getter?.hasExtensionReceiverParameter).isTrue()
    }

    @Test
    fun parseAnEnumEntry() {
        val input = "enum entry GROUP_ID // androidx.annotation/RestrictTo.Scope.GROUP_ID|null[0]"
        val parsed =
            KlibDumpParser(input)
                .parseEnumEntry(
                    AbiQualifiedName(
                        AbiCompoundName("androidx.annotation"),
                        AbiCompoundName("RestrictTo.Scope")
                    )
                )
        assertThat(parsed.qualifiedName.toString())
            .isEqualTo("androidx.annotation/RestrictTo.Scope.GROUP_ID")
    }

    @Test
    fun parseAnInvalidDeclaration() {
        val input =
            """
            final class my.lib/MyClass {
                invalid
            }
        """
                .trimIndent()
        val e = assertFailsWith<ParseException> { KlibDumpParser(input, "current.txt").parse() }
        assertThat(e.message)
            .isEqualTo("Failed to parse unknown declaration at current.txt:1:4: 'invalid'")
    }

    @Test
    fun parsesSignatureVersion() {
        val parsed = KlibDumpParser(exampleMetadata).parse()
        assertThat(parsed).isNotNull()
        assertThat(parsed.keys).hasSize(1)
        val abi: LibraryAbi = parsed.values.single()
        assertThat(abi.signatureVersions)
            .containsExactly(AbiSignatureVersion.resolveByVersionNumber(2))
    }

    @Test
    fun parseFullCollectionKlibDumpSucceeds() {
        val parsed = KlibDumpParser(collectionDump).parse()
        assertThat(parsed).isNotNull()
    }

    @Test
    fun parseFullDatastoreKlibDumpSucceeds() {
        val parsed = KlibDumpParser(datastoreCoreDump).parse()
        assertThat(parsed).isNotNull()
    }

    @Test
    fun parseFullAnnotationKlibDumpSucceeds() {
        val parsed = KlibDumpParser(annotationDump).parse()
        assertThat(parsed).isNotNull()
    }

    @Test
    fun parseFullDatastorePreferencesKlibDumpSucceeds() {
        val parsed = KlibDumpParser(datastorePreferencesDump).parse()
        assertThat(parsed).isNotNull()
    }

    @Test
    fun parseUniqueTargetsSucceeds() {
        val parsed = KlibDumpParser(uniqueTargetDump).parse()
        assertThat(parsed).isNotNull()
        assertThat(parsed.keys).hasSize(2)
        assertThat(parsed.keys).containsExactly("iosX64", "linuxX64")
        val iosQNames =
            parsed["iosX64"]?.topLevelDeclarations?.declarations?.map {
                it.qualifiedName.toString()
            }
        val linuxQNames =
            parsed["linuxX64"]?.topLevelDeclarations?.declarations?.map {
                it.qualifiedName.toString()
            }
        assertThat(iosQNames).containsExactly("my.lib/myIosFun", "my.lib/commonFun")
        assertThat(linuxQNames).containsExactly("my.lib/myLinuxFun", "my.lib/commonFun")
    }

    companion object {
        private val exampleMetadata =
            """
            // KLib ABI Dump
            // Targets: [linuxX64]
            // Rendering settings:
            // - Signature version: 2
            // - Show manifest properties: true
            // - Show declarations: true
            // Library unique name: <androidx:library>
        """
                .trimIndent()
    }
}
