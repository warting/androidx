/*
 * Copyright (C) 2020 The Android Open Source Project
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

package androidx.room.processing.javac

import androidx.room.processing.XTypeElement
import com.squareup.javapoet.ClassName
import javax.lang.model.element.ElementKind
import javax.lang.model.element.TypeElement
import javax.lang.model.type.TypeKind

internal class JavacTypeElement(
    env: JavacProcessingEnv,
    override val element: TypeElement
) : JavacElement(env, element), XTypeElement {

    override val qualifiedName by lazy {
        element.qualifiedName.toString()
    }

    override val className: ClassName by lazy {
        ClassName.get(element)
    }

    override fun isInterface() = element.kind == ElementKind.INTERFACE

    override val type: JavacDeclaredType by lazy {
        env.wrap<JavacDeclaredType>(element.asType())
    }

    override val superType: JavacType? by lazy {
        // javac models non-existing types as TypeKind.NONE but we prefer to make it nullable.
        // just makes more sense and safer as we don't need to check for none.

        // The result value is a JavacType instead of JavacDeclaredType to gracefully handle
        // cases where super is an error type.
        val superClass = element.superclass
        if (superClass.kind == TypeKind.NONE) {
            null
        } else {
            env.wrap<JavacType>(superClass)
        }
    }

    override val equalityItems: Array<out Any?> by lazy {
        arrayOf(element)
    }
}
