/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.privacysandbox.tools.core.generator

import androidx.privacysandbox.tools.core.generator.SpecNames.contextClass
import androidx.privacysandbox.tools.core.generator.SpecNames.contextPropertyName
import androidx.privacysandbox.tools.core.generator.SpecNames.uiAdapterToSpecs
import androidx.privacysandbox.tools.core.generator.UiAdapterSpecs.Companion.sandboxedUiAdapterSpecs
import androidx.privacysandbox.tools.core.generator.UiAdapterSpecs.Companion.sharedUiAdapterSpecs
import androidx.privacysandbox.tools.core.model.AnnotatedInterface
import androidx.privacysandbox.tools.core.model.AnnotatedValue
import androidx.privacysandbox.tools.core.model.Parameter
import androidx.privacysandbox.tools.core.model.Type
import androidx.privacysandbox.tools.core.model.Types
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec

/** [ParameterSpec] equivalent to this parameter. */
fun Parameter.poetSpec(): ParameterSpec {
    return ParameterSpec.builder(name, type.poetTypeName()).build()
}

/** [TypeName] equivalent to this type. */
fun Type.poetTypeName(): TypeName {
    val typeName =
        if (typeParameters.isEmpty()) poetClassName()
        else poetClassName().parameterizedBy(typeParameters.map { it.poetTypeName() })
    if (isNullable) return typeName.copy(nullable = true)
    return typeName
}

/** [ClassName] equivalent to this type. */
fun Type.poetClassName() = ClassName(packageName, simpleName)

fun AnnotatedValue.converterNameSpec() = ClassName(type.packageName, "${type.simpleName}Converter")

fun AnnotatedValue.parcelableNameSpec() =
    ClassName(type.packageName, "Parcelable${type.simpleName}")

fun AnnotatedInterface.clientProxyNameSpec() =
    ClassName(type.packageName, "${type.simpleName}ClientProxy")

fun AnnotatedInterface.stubDelegateNameSpec() =
    ClassName(type.packageName, "${type.simpleName}StubDelegate")

fun AnnotatedInterface.aidlInterfaceNameSpec() =
    ClassName(type.packageName, aidlType().innerType.simpleName)

/**
 * [UiAdapterSpecs] of the UI adapter [annotatedInterface] extends. Throws an error if called for an
 * interface that does not extend any of the UI adapters.
 */
fun getUiAdapterSpecForInterface(annotatedInterface: AnnotatedInterface) =
    uiAdapterToSpecs[annotatedInterface.superTypes.intersect(Types.uiAdapters).single()]!!

/**
 * Defines the primary constructor of this type with the given list of properties.
 *
 * @param modifiers extra modifiers added to the constructor
 */
fun TypeSpec.Builder.primaryConstructor(
    properties: List<PropertySpec>,
    vararg modifiers: KModifier,
) {
    val propertiesWithInitializer = properties.map { it.toBuilder().initializer(it.name).build() }
    primaryConstructor(
        FunSpec.constructorBuilder().build {
            addParameters(propertiesWithInitializer.map { ParameterSpec(it.name, it.type) })
            addModifiers(*modifiers)
        }
    )
    addProperties(propertiesWithInitializer)
}

/** Builds a [TypeSpec] using the given builder block. */
fun TypeSpec.Builder.build(block: TypeSpec.Builder.() -> Unit): TypeSpec {
    block()
    return build()
}

fun CodeBlock.Builder.build(block: CodeBlock.Builder.() -> Unit): CodeBlock {
    block()
    return build()
}

/** Builds a [FunSpec] using the given builder block. */
fun FunSpec.Builder.build(block: FunSpec.Builder.() -> Unit): FunSpec {
    block()
    return build()
}

/** Builds a [FileSpec] using the given builder block. */
fun FileSpec.Builder.build(block: FileSpec.Builder.() -> Unit): FileSpec {
    block()
    return build()
}

fun FileSpec.Builder.addCommonSettings() {
    indent("    ")
    addKotlinDefaultImports(includeJvm = false, includeJs = false)
}

fun FunSpec.Builder.addCode(block: CodeBlock.Builder.() -> Unit) {
    addCode(CodeBlock.builder().build { block() })
}

fun FunSpec.Builder.addStatement(block: CodeBlock.Builder.() -> Unit) {
    addCode(CodeBlock.builder().build { addStatement(block) })
}

/** Auto-closing control flow construct and its code. */
fun CodeBlock.Builder.addControlFlow(
    controlFlow: String,
    vararg args: Any?,
    block: CodeBlock.Builder.() -> Unit,
) {
    beginControlFlow(controlFlow, *args)
    block()
    endControlFlow()
}

/** Auto-closing statement block. Useful for adding multiple [CodeBlock]s in a single statement. */
fun CodeBlock.Builder.addStatement(builderBlock: CodeBlock.Builder.() -> Unit) {
    add("«")
    builderBlock()
    add("\n»")
}

object SpecNames {
    const val contextPropertyName = "context"

    // Kotlin coroutines
    val resumeMethod = MemberName("kotlin.coroutines", "resume", isExtension = true)
    val resumeWithExceptionMethod =
        MemberName("kotlin.coroutines", "resumeWithException", isExtension = true)
    val cancellationExceptionClass =
        ClassName("kotlin.coroutines.cancellation", "CancellationException")

    // KotlinX coroutines
    val coroutineScopeClass = ClassName("kotlinx.coroutines", "CoroutineScope")
    val dispatchersMainClass = ClassName("kotlinx.coroutines", "Dispatchers", "Main")
    val launchMethod = MemberName("kotlinx.coroutines", "launch", isExtension = true)
    val suspendCancellableCoroutineMethod =
        MemberName("kotlinx.coroutines", "suspendCancellableCoroutine", isExtension = true)

    // Java
    val stackTraceElementClass = ClassName("java.lang", "StackTraceElement")

    // Android
    val iBinderClass = ClassName("android.os", "IBinder")
    val bundleClass = ClassName("android.os", "Bundle")
    val contextClass = ClassName("android.content", "Context")
    val viewClass = ClassName("android.view", "View")

    // Privacy Sandbox UI
    val uiCoreLibInfoPropertyName: String = "coreLibInfo"
    val toCoreLibInfoMethod = MemberName("androidx.privacysandbox.ui.provider", "toCoreLibInfo")

    val uiAdapterToSpecs =
        hashMapOf<Type, UiAdapterSpecs>(
            Types.sandboxedUiAdapter to sandboxedUiAdapterSpecs,
            Types.sharedUiAdapter to sharedUiAdapterSpecs,
        )
}

interface UiAdapterSpecs {
    val type: Type
    val adapterPropertyName: String
    val adapterFactoryClass: ClassName
    val openSessionSpec: FunSpec
    val toCoreLibInfoExpression: String

    companion object {
        val sandboxedUiAdapterSpecs =
            object : UiAdapterSpecs {
                override val type: Type = Types.sandboxedUiAdapter
                override val adapterPropertyName: String = "sandboxedUiAdapter"
                override val adapterFactoryClass: ClassName =
                    ClassName("androidx.privacysandbox.ui.client", "SandboxedUiAdapterFactory")
                override val openSessionSpec: FunSpec =
                    FunSpec.builder("openSession").build {
                        addModifiers(KModifier.PUBLIC, KModifier.OVERRIDE)
                        addParameters(
                            listOf(
                                ParameterSpec(contextPropertyName, contextClass),
                                ParameterSpec(
                                    "sessionData",
                                    ClassName("androidx.privacysandbox.ui.core", "SessionData"),
                                ),
                                ParameterSpec("initialWidth", Types.int.poetClassName()),
                                ParameterSpec("initialHeight", Types.int.poetClassName()),
                                ParameterSpec("isZOrderOnTop", Types.boolean.poetClassName()),
                                ParameterSpec(
                                    "clientExecutor",
                                    ClassName("java.util.concurrent", "Executor"),
                                ),
                                ParameterSpec(
                                    "client",
                                    ClassName(
                                            "androidx.privacysandbox.ui.core",
                                            "SandboxedUiAdapter",
                                        )
                                        .nestedClass("SessionClient"),
                                ),
                            )
                        )
                        addStatement(
                            "${adapterPropertyName}.openSession(%N, sessionData, initialWidth, " +
                                "initialHeight, isZOrderOnTop, clientExecutor, client)",
                            contextPropertyName,
                        )
                    }
                override val toCoreLibInfoExpression: String = "%toCoreLibInfo:M(%context:N)"
            }
        val sharedUiAdapterSpecs =
            object : UiAdapterSpecs {
                override val type: Type = Types.sharedUiAdapter
                override val adapterPropertyName: String = "sharedUiAdapter"
                override val adapterFactoryClass: ClassName =
                    ClassName("androidx.privacysandbox.ui.client", "SharedUiAdapterFactory")
                override val openSessionSpec: FunSpec =
                    FunSpec.builder("openSession").build {
                        addModifiers(KModifier.PUBLIC, KModifier.OVERRIDE)
                        addParameters(
                            listOf(
                                ParameterSpec(
                                    "clientExecutor",
                                    ClassName("java.util.concurrent", "Executor"),
                                ),
                                ParameterSpec(
                                    "client",
                                    ClassName("androidx.privacysandbox.ui.core", "SharedUiAdapter")
                                        .nestedClass("SessionClient"),
                                ),
                            )
                        )
                        addStatement("${adapterPropertyName}.openSession(clientExecutor, client)")
                    }
                override val toCoreLibInfoExpression: String = "%toCoreLibInfo:M()"
            }
    }
}
