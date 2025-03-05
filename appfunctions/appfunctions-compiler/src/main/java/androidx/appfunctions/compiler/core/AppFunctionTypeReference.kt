/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.appfunctions.compiler.core

import androidx.appfunctions.compiler.core.AppFunctionTypeReference.AppFunctionSupportedTypeCategory.PRIMITIVE_ARRAY
import androidx.appfunctions.compiler.core.AppFunctionTypeReference.AppFunctionSupportedTypeCategory.PRIMITIVE_LIST
import androidx.appfunctions.compiler.core.AppFunctionTypeReference.AppFunctionSupportedTypeCategory.PRIMITIVE_SINGULAR
import androidx.appfunctions.compiler.core.AppFunctionTypeReference.AppFunctionSupportedTypeCategory.SERIALIZABLE_LIST
import androidx.appfunctions.compiler.core.AppFunctionTypeReference.AppFunctionSupportedTypeCategory.SERIALIZABLE_SINGULAR
import androidx.appfunctions.metadata.AppFunctionPrimitiveTypeMetadata
import com.google.devtools.ksp.symbol.KSTypeReference
import com.squareup.kotlinpoet.LIST
import com.squareup.kotlinpoet.TypeName

/** Represents a type that is supported by AppFunction and AppFunctionSerializable. */
class AppFunctionTypeReference(val selfTypeReference: KSTypeReference) {

    /**
     * The category of this reference type.
     *
     * The category of a type is determined by its underlying type. For example, a type reference to
     * a list of strings will have a category of PRIMITIVE_LIST.
     */
    val typeCategory: AppFunctionSupportedTypeCategory by lazy {
        when {
            selfTypeReference.asStringWithoutNullQualifier() in SUPPORTED_SINGLE_PRIMITIVE_TYPES ->
                PRIMITIVE_SINGULAR
            selfTypeReference.asStringWithoutNullQualifier() in SUPPORTED_ARRAY_PRIMITIVE_TYPES ->
                PRIMITIVE_ARRAY
            isSupportedPrimitiveListType(selfTypeReference) -> PRIMITIVE_LIST
            isAppFunctionSerializableListType(selfTypeReference) -> SERIALIZABLE_LIST
            isAppFunctionSerializableType(selfTypeReference) -> SERIALIZABLE_SINGULAR
            else ->
                throw ProcessingException(
                    "Unsupported type reference ${selfTypeReference.ensureQualifiedTypeName().asString()}",
                    selfTypeReference,
                )
        }
    }

    /**
     * If this type is nullable.
     *
     * @return true if the type is nullable, false otherwise.
     */
    val isNullable: Boolean by lazy { selfTypeReference.toTypeName().isNullable }

    /**
     * The type reference of the list element if the type reference is a list.
     *
     * @return the type reference of the list element if the type reference is a list.
     * @throws IllegalArgumentException if used for a non-list type.
     */
    val itemTypeReference: KSTypeReference by lazy { ->
        require(selfTypeReference.isOfType(LIST)) { "Type reference is not a list" }
        selfTypeReference.resolveListParameterizedType()
    }

    /**
     * The type reference itself or the type reference of the list element if the type reference is
     * a list. For example, if the type reference is List<String>, then the selfOrItemTypeReference
     * will be String.
     */
    val selfOrItemTypeReference: KSTypeReference by lazy {
        if (selfTypeReference.isOfType(LIST)) {
            itemTypeReference
        } else {
            selfTypeReference
        }
    }

    /**
     * Checks if the type reference is of the given category.
     *
     * @param category The category to check.
     * @return true if the type reference is of the given category, false otherwise.
     */
    fun isOfTypeCategory(category: AppFunctionSupportedTypeCategory): Boolean {
        return this.typeCategory == category
    }

    /**
     * The category of types that are supported by app functions.
     *
     * The category of a type is determined by its underlying type. For example, a type reference to
     * a list of strings will have a category of PRIMITIVE_LIST.
     */
    enum class AppFunctionSupportedTypeCategory {
        PRIMITIVE_SINGULAR,
        PRIMITIVE_ARRAY,
        PRIMITIVE_LIST,
        SERIALIZABLE_SINGULAR,
        SERIALIZABLE_LIST,
    }

    companion object {
        /**
         * Checks if the type reference is a supported type.
         *
         * A supported type is a primitive type, a type annotated as @AppFunctionSerializable, or a
         * list of a supported type.
         */
        fun isSupportedType(typeReferenceArgument: KSTypeReference): Boolean {
            return typeReferenceArgument.asStringWithoutNullQualifier() in SUPPORTED_TYPES ||
                isSupportedPrimitiveListType(typeReferenceArgument) ||
                isAppFunctionSerializableType(typeReferenceArgument) ||
                isAppFunctionSerializableListType(typeReferenceArgument)
        }

        /**
         * Converts a type reference to an AppFunction data type.
         *
         * @return The AppFunction data type.
         * @throws ProcessingException If the type reference is not a supported type.
         */
        fun KSTypeReference.toAppFunctionDatatype(): Int {
            return when (this.toTypeName().ignoreNullable().toString()) {
                String::class.ensureQualifiedName() -> AppFunctionPrimitiveTypeMetadata.TYPE_STRING
                Int::class.ensureQualifiedName() -> AppFunctionPrimitiveTypeMetadata.TYPE_INT
                Long::class.ensureQualifiedName() -> AppFunctionPrimitiveTypeMetadata.TYPE_LONG
                Float::class.ensureQualifiedName() -> AppFunctionPrimitiveTypeMetadata.TYPE_FLOAT
                Double::class.ensureQualifiedName() -> AppFunctionPrimitiveTypeMetadata.TYPE_DOUBLE
                Boolean::class.ensureQualifiedName() ->
                    AppFunctionPrimitiveTypeMetadata.TYPE_BOOLEAN
                Unit::class.ensureQualifiedName() -> AppFunctionPrimitiveTypeMetadata.TYPE_UNIT
                Byte::class.ensureQualifiedName() -> AppFunctionPrimitiveTypeMetadata.TYPE_BYTES
                IntArray::class.ensureQualifiedName() -> AppFunctionPrimitiveTypeMetadata.TYPE_INT
                LongArray::class.ensureQualifiedName() -> AppFunctionPrimitiveTypeMetadata.TYPE_LONG
                FloatArray::class.ensureQualifiedName() ->
                    AppFunctionPrimitiveTypeMetadata.TYPE_FLOAT
                DoubleArray::class.ensureQualifiedName() ->
                    AppFunctionPrimitiveTypeMetadata.TYPE_DOUBLE
                BooleanArray::class.ensureQualifiedName() ->
                    AppFunctionPrimitiveTypeMetadata.TYPE_BOOLEAN
                ByteArray::class.ensureQualifiedName() ->
                    AppFunctionPrimitiveTypeMetadata.TYPE_BYTES
                ANDROID_PENDING_INTENT -> AppFunctionPrimitiveTypeMetadata.TYPE_PENDING_INTENT
                else ->
                    throw ProcessingException(
                        "Unsupported type reference " + this.ensureQualifiedTypeName().asString(),
                        this,
                    )
            }
        }

        private fun isSupportedPrimitiveListType(typeReferenceArgument: KSTypeReference) =
            typeReferenceArgument.isOfType(LIST) &&
                typeReferenceArgument
                    .resolveListParameterizedType()
                    .asStringWithoutNullQualifier() in SUPPORTED_PRIMITIVE_TYPES_IN_LIST

        private fun isAppFunctionSerializableListType(
            typeReferenceArgument: KSTypeReference
        ): Boolean {
            return typeReferenceArgument.isOfType(LIST) &&
                isAppFunctionSerializableType(typeReferenceArgument.resolveListParameterizedType())
        }

        private fun isAppFunctionSerializableType(typeReferenceArgument: KSTypeReference): Boolean {
            return typeReferenceArgument
                .resolve()
                .declaration
                .annotations
                .findAnnotation(IntrospectionHelper.AppFunctionSerializableAnnotation.CLASS_NAME) !=
                null
        }

        private fun TypeName.ignoreNullable(): TypeName {
            return copy(nullable = false)
        }

        private fun KSTypeReference.asStringWithoutNullQualifier(): String =
            toTypeName().ignoreNullable().toString()

        // Android Only primitives
        private const val ANDROID_PENDING_INTENT = "android.app.PendingIntent"

        private val SUPPORTED_ARRAY_PRIMITIVE_TYPES =
            setOf(
                IntArray::class.ensureQualifiedName(),
                LongArray::class.ensureQualifiedName(),
                FloatArray::class.ensureQualifiedName(),
                DoubleArray::class.ensureQualifiedName(),
                BooleanArray::class.ensureQualifiedName(),
                ByteArray::class.ensureQualifiedName(),
            )

        private val SUPPORTED_SINGLE_PRIMITIVE_TYPES =
            setOf(
                Int::class.ensureQualifiedName(),
                Long::class.ensureQualifiedName(),
                Float::class.ensureQualifiedName(),
                Double::class.ensureQualifiedName(),
                Boolean::class.ensureQualifiedName(),
                String::class.ensureQualifiedName(),
                Unit::class.ensureQualifiedName(),
                ANDROID_PENDING_INTENT
            )

        private val SUPPORTED_PRIMITIVE_TYPES_IN_LIST = setOf(String::class.ensureQualifiedName())

        private val SUPPORTED_TYPES =
            SUPPORTED_SINGLE_PRIMITIVE_TYPES + SUPPORTED_ARRAY_PRIMITIVE_TYPES

        val SUPPORTED_TYPES_STRING: String =
            SUPPORTED_TYPES.joinToString(",\n") +
                "\nLists of ${SUPPORTED_PRIMITIVE_TYPES_IN_LIST.joinToString(", ")}"
    }
}
