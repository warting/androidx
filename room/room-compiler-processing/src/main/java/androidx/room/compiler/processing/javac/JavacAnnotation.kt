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

package androidx.room.compiler.processing.javac

import androidx.room.compiler.processing.InternalXAnnotation
import androidx.room.compiler.processing.XAnnotationValue
import androidx.room.compiler.processing.XNullability
import androidx.room.compiler.processing.XType
import com.google.auto.common.AnnotationMirrors
import com.google.auto.common.MoreTypes
import javax.lang.model.element.AnnotationMirror

internal class JavacAnnotation(val env: JavacProcessingEnv, val mirror: AnnotationMirror) :
    InternalXAnnotation() {

    override val name: String
        get() = mirror.annotationType.asElement().simpleName.toString()

    override val qualifiedName: String
        get() = MoreTypes.asTypeElement(mirror.annotationType).qualifiedName.toString()

    override val type: XType by lazy {
        JavacDeclaredType(env, mirror.annotationType, XNullability.NONNULL)
    }

    override val declaredAnnotationValues: List<XAnnotationValue> by lazy {
        // getElementValues returns values of this annotation's element, only those elements with
        // values explicitly present in the annotation are included, not those that are implicitly
        // assuming their default values.
        val explicitValues = mirror.getElementValues().keys.map { it.simpleName.toString() }
        annotationValues.filter { explicitValues.contains(it.name) }
    }

    override val defaultValues: List<XAnnotationValue> by lazy {
        annotationValues.mapNotNull {
            val method = (it as JavacAnnotationValue).method
            method.element.getDefaultValue()?.let { value ->
                JavacAnnotationValue(env, method, value)
            }
        }
    }

    override val annotationValues: List<XAnnotationValue> by lazy {
        AnnotationMirrors.getAnnotationValuesWithDefaults(mirror).map {
            (executableElement, annotationValue) ->
            JavacAnnotationValue(
                env,
                env.wrapExecutableElement(executableElement) as JavacMethodElement,
                annotationValue,
            )
        }
    }
}
