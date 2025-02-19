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
package androidx.appsearch.compiler.annotationwrapper

import androidx.appsearch.compiler.IntrospectionHelper
import com.squareup.javapoet.ClassName
import javax.lang.model.type.TypeMirror

/** An instance of the `@Document.DocumentProperty` annotation. */
data class DocumentPropertyAnnotation(
    override val name: String,
    override val isRequired: Boolean,

    /** Specifies whether fields in the nested document should be indexed. */
    val shouldIndexNestedProperties: Boolean,

    /**
     * The list of nested properties to index for the nested document other than the properties
     * inherited from the type's parent.
     */
    val indexableNestedPropertiesList: List<String>,

    /**
     * Specifies whether to inherit the parent class's definition for the indexable nested
     * properties list.
     */
    val shouldInheritIndexableNestedPropertiesFromSuperClass: Boolean,
) :
    DataPropertyAnnotation(
        className = CLASS_NAME,
        configClassName = CONFIG_CLASS,
        genericDocGetterName = "getPropertyDocument",
        genericDocArrayGetterName = "getPropertyDocumentArray",
        genericDocSetterName = "setPropertyDocument",
    ) {
    companion object {
        val CLASS_NAME: ClassName =
            IntrospectionHelper.DOCUMENT_ANNOTATION_CLASS.nestedClass("DocumentProperty")

        @JvmField
        val CONFIG_CLASS: ClassName =
            IntrospectionHelper.APPSEARCH_SCHEMA_CLASS.nestedClass("DocumentPropertyConfig")

        /**
         * @param defaultName The name to use for the annotated property in case the annotation
         *   params do not mention an explicit name.
         */
        fun parse(
            annotationParams: Map<String?, Any?>,
            defaultName: String
        ): DocumentPropertyAnnotation {
            val name = annotationParams["name"] as? String
            val indexableNestedPropertiesList = mutableListOf<String>()
            val indexableList = annotationParams["indexableNestedPropertiesList"]
            if (indexableList is List<*>) {
                for (property in indexableList) {
                    indexableNestedPropertiesList.add(property.toString())
                }
            }
            return DocumentPropertyAnnotation(
                name = if (name.isNullOrEmpty()) defaultName else name,
                isRequired = annotationParams["required"] as Boolean,
                shouldIndexNestedProperties = annotationParams["indexNestedProperties"] as Boolean,
                indexableNestedPropertiesList = indexableNestedPropertiesList,
                shouldInheritIndexableNestedPropertiesFromSuperClass =
                    annotationParams["inheritIndexableNestedPropertiesFromSuperclass"] as Boolean
            )
        }
    }

    override val dataPropertyKind
        get() = Kind.DOCUMENT_PROPERTY

    override fun getUnderlyingTypeWithinGenericDoc(helper: IntrospectionHelper): TypeMirror =
        helper.mGenericDocumentType
}
