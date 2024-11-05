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

package androidx.room.compiler.codegen.kotlin

import androidx.room.compiler.codegen.KTypeSpecBuilder
import androidx.room.compiler.codegen.VisibilityModifier
import androidx.room.compiler.codegen.XAnnotationSpec
import androidx.room.compiler.codegen.XFunSpec
import androidx.room.compiler.codegen.XPropertySpec
import androidx.room.compiler.codegen.XTypeName
import androidx.room.compiler.codegen.XTypeSpec
import androidx.room.compiler.processing.XElement
import androidx.room.compiler.processing.addOriginatingElement
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.javapoet.KTypeSpec

internal class KotlinTypeSpec(internal val actual: KTypeSpec) : KotlinLang(), XTypeSpec {

    override val name: String? = actual.name

    override fun toString() = actual.toString()

    internal class Builder(internal val actual: KTypeSpecBuilder) :
        KotlinLang(), XTypeSpec.Builder {
        override fun superclass(typeName: XTypeName) = apply { actual.superclass(typeName.kotlin) }

        override fun addSuperinterface(typeName: XTypeName) = apply {
            actual.addSuperinterface(typeName.kotlin)
        }

        override fun addAnnotation(annotation: XAnnotationSpec) = apply {
            require(annotation is KotlinAnnotationSpec)
            actual.addAnnotation(annotation.actual)
        }

        override fun addProperty(propertySpec: XPropertySpec) = apply {
            require(propertySpec is KotlinPropertySpec)
            actual.addProperty(propertySpec.actual)
        }

        override fun addFunction(functionSpec: XFunSpec) = apply {
            require(functionSpec is KotlinFunSpec)
            actual.addFunction(functionSpec.actual)
        }

        override fun addType(typeSpec: XTypeSpec) = apply {
            require(typeSpec is KotlinTypeSpec)
            actual.addType(typeSpec.actual)
        }

        override fun setPrimaryConstructor(functionSpec: XFunSpec) = apply {
            require(functionSpec is KotlinFunSpec)
            actual.primaryConstructor(functionSpec.actual)
            functionSpec.actual.delegateConstructorArguments.forEach {
                actual.addSuperclassConstructorParameter(it)
            }
        }

        override fun setVisibility(visibility: VisibilityModifier) = apply {
            actual.addModifiers(visibility.toKotlinVisibilityModifier())
        }

        override fun addAbstractModifier(): XTypeSpec.Builder = apply {
            actual.addModifiers(KModifier.ABSTRACT)
        }

        override fun addOriginatingElement(element: XElement) = apply {
            actual.addOriginatingElement(element)
        }

        override fun build() = KotlinTypeSpec(actual.build())
    }
}
