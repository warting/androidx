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
import com.google.devtools.ksp.symbol.KSTypeReference
import com.squareup.kotlinpoet.LIST
import com.squareup.kotlinpoet.TypeName

/** Represents a type that is supported by AppFunction and AppFunctionSerializable. */
class AppFunctionTypeReference(val ksTypeReference: KSTypeReference) {

    val typeCategory: AppFunctionSupportedTypeCategory by lazy {
        val typeName = ksTypeReference.toTypeName().ignoreNullable().toString()
        if (typeName in SUPPORTED_COLLECTION_TYPES) {
            PRIMITIVE_LIST
        } else if (typeName in SUPPORTED_SINGLE_PRIMITIVE_TYPES) {
            PRIMITIVE_SINGULAR
        } else if (typeName in SUPPORTED_ARRAY_PRIMITIVE_TYPES) {
            PRIMITIVE_ARRAY
        } else if (isAppFunctionSerializableListType(ksTypeReference)) {
            SERIALIZABLE_LIST
        } else if (isAppFunctionSerializableType(ksTypeReference)) {
            SERIALIZABLE_SINGULAR
        } else {
            throw ProcessingException(
                "Unsupported type reference ${ksTypeReference.ensureQualifiedTypeName().asString()}",
                ksTypeReference
            )
        }
    }

    val isNullable: Boolean by lazy { ksTypeReference.toTypeName().isNullable }

    fun isOfTypeCategory(category: AppFunctionSupportedTypeCategory): Boolean {
        return this.typeCategory == category
    }

    /** The category of parameter type that is supported by app functions. */
    enum class AppFunctionSupportedTypeCategory {
        PRIMITIVE_SINGULAR,
        PRIMITIVE_ARRAY,
        PRIMITIVE_LIST,
        SERIALIZABLE_SINGULAR,
        SERIALIZABLE_LIST
    }

    companion object {
        /**
         * Checks if the type reference is a supported type.
         *
         * A supported type is a primitive type, a type annotated as @AppFunctionSerializable, or a
         * list of a supported type.
         */
        fun isSupportedType(typeReferenceArgument: KSTypeReference): Boolean {
            return SUPPORTED_TYPES.contains(
                typeReferenceArgument.toTypeName().ignoreNullable().toString()
            ) ||
                isAppFunctionSerializableListType(typeReferenceArgument) ||
                isAppFunctionSerializableType(typeReferenceArgument)
        }

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
            )

        const val STRING_LIST_TYPE = "kotlin.collections.List<kotlin.String>"
        const val BYTE_ARRAY_LIST_TYPE = "kotlin.collections.List<kotlin.ByteArray>"

        private val SUPPORTED_COLLECTION_TYPES = setOf(STRING_LIST_TYPE, BYTE_ARRAY_LIST_TYPE)

        private val SUPPORTED_TYPES =
            SUPPORTED_SINGLE_PRIMITIVE_TYPES +
                SUPPORTED_ARRAY_PRIMITIVE_TYPES +
                SUPPORTED_COLLECTION_TYPES

        val SUPPORTED_TYPES_STRING: String = SUPPORTED_TYPES.joinToString(",\n")
    }
}
