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
import androidx.appsearch.compiler.ProcessingException
import com.google.auto.common.MoreTypes
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.TypeName
import javax.lang.model.element.TypeElement
import javax.lang.model.type.TypeMirror

/** An instance of the `@Document.StringProperty` annotation. */
data class StringPropertyAnnotation(
    override val name: String,
    override val isRequired: Boolean,

    /** Specifies how tokens should be extracted from this property. */
    val tokenizerType: Int,

    /** Specifies how a property should be indexed. */
    val indexingType: Int,

    /** Specifies how a property should be processed so that the document can be joined. */
    val joinableValueType: Int,

    /**
     * An optional [androidx.appsearch.app.StringSerializer].
     *
     * This is specified in the annotation when the annotated getter/field is of some custom type
     * that should boil down to a String in the database.
     *
     * @see androidx.appsearch.annotation.Document.StringProperty.serializer
     */
    val customSerializer: SerializerClass?,
) :
    DataPropertyAnnotation(
        className = CLASS_NAME,
        configClassName = CONFIG_CLASS,
        genericDocGetterName = "getPropertyString",
        genericDocArrayGetterName = "getPropertyStringArray",
        genericDocSetterName = "setPropertyString",
    ) {
    companion object {
        val CLASS_NAME: ClassName =
            IntrospectionHelper.DOCUMENT_ANNOTATION_CLASS.nestedClass("StringProperty")

        @JvmField
        val CONFIG_CLASS: ClassName =
            IntrospectionHelper.APPSEARCH_SCHEMA_CLASS.nestedClass("StringPropertyConfig")

        private val DEFAULT_SERIALIZER_CLASS: ClassName =
            CLASS_NAME.nestedClass("DefaultSerializer")

        /**
         * @param defaultName The name to use for the annotated property in case the annotation
         *   params do not mention an explicit name.
         * @throws ProcessingException If the annotation points to an Illegal serializer class.
         */
        @Throws(ProcessingException::class)
        fun parse(
            annotationParams: Map<String, Any?>,
            defaultName: String,
        ): StringPropertyAnnotation {
            val name = annotationParams["name"] as? String
            val serializerInAnnotation = annotationParams["serializer"] as TypeMirror
            val typeName = TypeName.get(serializerInAnnotation).toString()
            val customSerializer: SerializerClass? =
                if (typeName == DEFAULT_SERIALIZER_CLASS.canonicalName()) {
                    null
                } else {
                    SerializerClass.create(
                        MoreTypes.asElement(serializerInAnnotation) as TypeElement,
                        SerializerClass.Kind.STRING_SERIALIZER,
                    )
                }
            return StringPropertyAnnotation(
                name = if (name.isNullOrEmpty()) defaultName else name,
                isRequired = annotationParams["required"] as Boolean,
                tokenizerType = annotationParams["tokenizerType"] as Int,
                indexingType = annotationParams["indexingType"] as Int,
                joinableValueType = annotationParams["joinableValueType"] as Int,
                customSerializer = customSerializer,
            )
        }
    }

    override val dataPropertyKind
        get() = Kind.STRING_PROPERTY

    override fun getUnderlyingTypeWithinGenericDoc(helper: IntrospectionHelper): TypeMirror =
        helper.stringType
}
