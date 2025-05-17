/*
 * Copyright 2023 The Android Open Source Project
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
package androidx.appsearch.compiler

import javax.lang.model.element.ElementKind
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.Modifier
import javax.lang.model.type.DeclaredType

/**
 * A constructor or static method used to create a class annotated with `@Document` aka document
 * class.
 *
 * Takes in N input params, each corresponding to a value for an [AnnotatedGetterOrField].
 *
 * Moreover, may return the document class itself or a builder. All of the following are examples of
 * valid creation methods:
 * <pre>
 * @Document
 * class MyEntity {
 *     static MyEntity create(String id, String namespace, int someProp);
 * //                  ^^^^^^
 *
 *     MyEntity() {...}
 * //  ^^^^^^^^
 *
 *     MyEntity(String id, String namespace, int someProp) {...}
 * //  ^^^^^^^^
 *
 *     @Document.BuilderProducer
 *     static Builder newBuilder() {...}
 * //                 ^^^^^^^^^^
 *
 *     @Document.BuilderProducer
 *     static class Builder {
 *         Builder() {...}
 * //      ^^^^^^^
 *
 *         Builder(String id, String namespace, int someProp) {...}
 * //      ^^^^^^^
 *     }
 * }
 * </pre>
 */
data class CreationMethod(
    /** The constructor/static method element. */
    val element: ExecutableElement,

    /** The [AnnotatedGetterOrField]s that each input param corresponds to (order sensitive). */
    val paramAssociations: List<AnnotatedGetterOrField>,

    /** Whether the creation method returns the document class itself instead of a builder. */
    val returnsDocumentClass: Boolean,
) {
    companion object {
        /**
         * Infers which annotated getter/field each param corresponds to and creates a
         * [CreationMethod].
         *
         * @param method The creation method element.
         * @param gettersAndFields The annotated getters/fields of the document class.
         * @param returnsDocumentClass Whether the `method` returns the document class itself. If
         *   not, it is assumed that it returns a builder for the document class.
         * @throws ProcessingException If the method is not invocable or the association for a param
         *   could not be deduced.
         */
        @Throws(ProcessingException::class)
        fun inferParamAssociationsAndCreate(
            method: ExecutableElement,
            gettersAndFields: Collection<AnnotatedGetterOrField>,
            returnsDocumentClass: Boolean,
        ): CreationMethod {
            if (method.modifiers.contains(Modifier.PRIVATE)) {
                throw ProcessingException(
                    ("Method cannot be used to create a " +
                        (if (returnsDocumentClass) "document class" else "builder") +
                        ": private visibility"),
                    method,
                )
            }

            if (
                method.kind == ElementKind.CONSTRUCTOR &&
                    method.enclosingElement.modifiers.contains(Modifier.ABSTRACT)
            ) {
                throw ProcessingException(
                    ("Method cannot be used to create a " +
                        (if (returnsDocumentClass) "document class" else "builder") +
                        ": abstract constructor"),
                    method,
                )
            }

            val normalizedNameToGetterOrField = mutableMapOf<String, AnnotatedGetterOrField>()
            for (getterOrField in gettersAndFields) {
                normalizedNameToGetterOrField[getterOrField.normalizedName] = getterOrField
            }

            val paramAssociations = mutableListOf<AnnotatedGetterOrField>()
            for (param in method.parameters) {
                val paramName = param.simpleName.toString()
                val correspondingGetterOrField: AnnotatedGetterOrField =
                    normalizedNameToGetterOrField[paramName]
                        ?: throw ProcessingException(
                            "Parameter \"$paramName\" is not an AppSearch parameter; " +
                                "don't know how to supply it.",
                            method,
                        )
                paramAssociations.add(correspondingGetterOrField)
            }

            return CreationMethod(method, paramAssociations, returnsDocumentClass)
        }
    }

    /** The enclosing class that the constructor/static method is a part of. */
    val enclosingClass: DeclaredType
        get() = element.enclosingElement.asType() as DeclaredType

    /** The static method's return type/constructor's enclosing class. */
    val returnType: DeclaredType
        get() =
            if (isConstructor) {
                element.enclosingElement.asType() as DeclaredType
            } else {
                element.returnType as DeclaredType
            }

    /** The static method/constructor element's name. */
    val jvmName: String
        get() = element.simpleName.toString()

    /** Whether the creation method is a constructor. */
    val isConstructor: Boolean
        get() = element.kind == ElementKind.CONSTRUCTOR

    /** Whether the creation method returns a builder instead of the document class itself. */
    val returnsBuilder: Boolean
        get() = !returnsDocumentClass
}
