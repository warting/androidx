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

package androidx.room.compiler.processing.javac

import androidx.room.compiler.processing.XDeclaredType
import androidx.room.compiler.processing.XMethodElement
import androidx.room.compiler.processing.XMethodType
import androidx.room.compiler.processing.XVariableElement
import androidx.room.compiler.processing.javac.kotlin.KmFunction
import com.google.auto.common.MoreElements
import com.google.auto.common.MoreTypes
import javax.lang.model.element.ElementKind
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.Modifier
import javax.lang.model.element.TypeElement

internal class JavacMethodElement(
    env: JavacProcessingEnv,
    containing: JavacTypeElement,
    element: ExecutableElement
) : JavacExecutableElement(
    env,
    containing,
    element
), XMethodElement {
    init {
        check(element.kind == ElementKind.METHOD) {
            "Method element is constructed with invalid type: $element"
        }
    }
    override val kotlinMetadata: KmFunction? by lazy {
        (enclosingElement as? JavacTypeElement)?.kotlinMetadata?.getFunctionMetadata(element)
    }

    override val executableType: JavacMethodType by lazy {
        val asMemberOf = env.typeUtils.asMemberOf(containing.type.typeMirror, element)
        JavacMethodType(
            env = env,
            element = this,
            executableType = MoreTypes.asExecutable(asMemberOf)
        )
    }

    override val returnType: JavacType by lazy {
        val asMember = env.typeUtils.asMemberOf(containing.type.typeMirror, element)
        val asExec = MoreTypes.asExecutable(asMember)
        env.wrap<JavacType>(
            typeMirror = asExec.returnType,
            kotlinType = if (isSuspendFunction()) {
                // Don't use Kotlin metadata for suspend functions since we want the Java
                // perspective. In Java, a suspend function returns Object and contains an extra
                // parameter of type Continuation<? extends T> where T is the actual return type as
                // declared in the Kotlin source.
                null
            } else {
                kotlinMetadata?.returnType
            },
            elementNullability = element.nullability
        )
    }

    override fun asMemberOf(other: XDeclaredType): XMethodType {
        return if (containing.type.isSameType(other)) {
            executableType
        } else {
            check(other is JavacDeclaredType)
            val asMemberOf = env.typeUtils.asMemberOf(other.typeMirror, element)
            JavacMethodType(
                env = env,
                element = this,
                executableType = MoreTypes.asExecutable(asMemberOf)
            )
        }
    }

    override fun isJavaDefault() = element.modifiers.contains(Modifier.DEFAULT)

    override fun isSuspendFunction() = kotlinMetadata?.isSuspend() == true

    override fun findKotlinDefaultImpl(): XMethodElement? {
        fun paramsMatch(
            ourParams: List<XVariableElement>,
            theirParams: List<XVariableElement>
        ): Boolean {
            if (ourParams.size != theirParams.size - 1) {
                return false
            }
            ourParams.forEachIndexed { i, variableElement ->
                // Plus 1 to their index because their first param is a self object.
                if (!theirParams[i + 1].type.isSameType(
                        variableElement.type
                    )
                ) {
                    return false
                }
            }
            return true
        }
        return kotlinDefaultImplClass?.getDeclaredMethods()?.find {
            it.name == this.name && paramsMatch(parameters, it.parameters)
        }
    }

    @Suppress("UnstableApiUsage")
    private val kotlinDefaultImplClass by lazy {
        val parent = element.enclosingElement as? TypeElement
        val defaultImplElement = parent?.enclosedElements?.find {
            MoreElements.isType(it) && it.simpleName.contentEquals(DEFAULT_IMPLS_CLASS_NAME)
        } as? TypeElement
        defaultImplElement?.let {
            env.wrapTypeElement(it)
        }
    }
}
