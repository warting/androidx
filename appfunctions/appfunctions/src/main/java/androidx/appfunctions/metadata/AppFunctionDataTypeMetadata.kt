/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.appfunctions.metadata

import android.annotation.SuppressLint
import androidx.annotation.IntDef
import androidx.annotation.RestrictTo
import androidx.appsearch.annotation.Document

@IntDef(
    AppFunctionDataTypeMetadata.TYPE_UNIT,
    AppFunctionDataTypeMetadata.TYPE_BOOLEAN,
    AppFunctionDataTypeMetadata.TYPE_BYTES,
    AppFunctionDataTypeMetadata.TYPE_OBJECT,
    AppFunctionDataTypeMetadata.TYPE_DOUBLE,
    AppFunctionDataTypeMetadata.TYPE_FLOAT,
    AppFunctionDataTypeMetadata.TYPE_LONG,
    AppFunctionDataTypeMetadata.TYPE_INT,
    AppFunctionDataTypeMetadata.TYPE_STRING,
    AppFunctionDataTypeMetadata.TYPE_ARRAY,
    AppFunctionDataTypeMetadata.TYPE_REFERENCE,
    AppFunctionDataTypeMetadata.TYPE_ALL_OF,
    AppFunctionDataTypeMetadata.TYPE_PENDING_INTENT,
)
@Retention(AnnotationRetention.SOURCE)
internal annotation class AppFunctionDataType

/** Base class for defining the schema of an input or output type. */
public abstract class AppFunctionDataTypeMetadata
internal constructor(
    /** Whether the data type is nullable. */
    public val isNullable: Boolean,
    /** A description of the data type and its intended use. */
    public val description: String,
) {
    /** Converts this [AppFunctionDataTypeMetadata] to an [AppFunctionDataTypeMetadataDocument]. */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public abstract fun toAppFunctionDataTypeMetadataDocument(): AppFunctionDataTypeMetadataDocument

    public companion object {
        /** Void type. */
        internal const val TYPE_UNIT: Int = 0
        /** Boolean type. */
        internal const val TYPE_BOOLEAN: Int = 1
        /** Byte array type. */
        internal const val TYPE_BYTES: Int = 2
        /**
         * Object type. The schema of the object is defined in a [AppFunctionObjectTypeMetadata].
         */
        internal const val TYPE_OBJECT: Int = 3
        /** Double type. */
        internal const val TYPE_DOUBLE: Int = 4
        /** Float type. */
        internal const val TYPE_FLOAT: Int = 5
        /** Long type. */
        internal const val TYPE_LONG: Int = 6
        /** Integer type. */
        internal const val TYPE_INT: Int = 7
        /** String type. */
        internal const val TYPE_STRING: Int = 8
        /** Array type. The schema of the array is defined in a [AppFunctionArrayTypeMetadata] */
        internal const val TYPE_ARRAY: Int = 10
        /**
         * Reference type. The schema of the reference is defined in a
         * [AppFunctionReferenceTypeMetadata]
         */
        internal const val TYPE_REFERENCE: Int = 11
        /**
         * All of type. The schema of the all of type is defined in a [AppFunctionAllOfTypeMetadata]
         */
        internal const val TYPE_ALL_OF: Int = 12
        /** Pending Intent type. */
        internal const val TYPE_PENDING_INTENT: Int = 13
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AppFunctionDataTypeMetadata

        if (description != other.description) return false
        return isNullable == other.isNullable
    }

    override fun hashCode(): Int {
        return 31 * description.hashCode() + isNullable.hashCode()
    }

    override fun toString(): String {
        return "AppFunctionDataTypeMetadata(isNullable=$isNullable, description=$description)"
    }
}

/** Defines the schema of an array data type. */
public class AppFunctionArrayTypeMetadata
@JvmOverloads
constructor(
    /** The type of items in the array. */
    public val itemType: AppFunctionDataTypeMetadata,
    /** Whether this data type is nullable. */
    isNullable: Boolean,
    /** A description of the data type and its intended use. */
    description: String = "",
) : AppFunctionDataTypeMetadata(isNullable = isNullable, description = description) {
    override fun equals(other: Any?): Boolean {
        if (!super.equals(other)) return false
        if (other !is AppFunctionArrayTypeMetadata) return false
        return itemType == other.itemType
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + itemType.hashCode()
        return result
    }

    override fun toString(): String {
        return "AppFunctionArrayTypeMetadataDocument(" +
            "itemType=$itemType, " +
            "isNullable=$isNullable," +
            "description=$description" +
            ")"
    }

    /** Converts this [AppFunctionArrayTypeMetadata] to an [AppFunctionDataTypeMetadataDocument]. */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    override fun toAppFunctionDataTypeMetadataDocument(): AppFunctionDataTypeMetadataDocument {
        return AppFunctionDataTypeMetadataDocument(
            itemType = itemType.toAppFunctionDataTypeMetadataDocument(),
            type = TYPE,
            isNullable = isNullable,
            description = description.ifEmpty { null },
        )
    }

    public companion object {
        /** Array type. The schema of the array is defined in a [AppFunctionArrayTypeMetadata] */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) public const val TYPE: Int = TYPE_ARRAY
    }
}

/**
 * Defines the schema of a single object data type that is a composition of all of the other types
 * in the [matchAll] list.
 *
 * An object of this type must match all of the [AppFunctionDataTypeMetadata] in the [matchAll]
 * list. [matchAll] takes an array of object definitions that are composed together to a single
 * object to form this type. Note that while this composition offers object type extensibility, it
 * does not imply a hierarchy between the objects matched in [matchAll] i.e. the resulting single
 * object is a flattened representation of all the other matched objects.
 *
 * For example, consider the following objects:
 * ```
 * open class Address (
 *     open val street: String,
 *     open val city: String,
 *     open val state: String,
 *     open val zipCode: String,
 * )
 *
 * class PersonWithAddress (
 *     override val street: String,
 *     override val city: String,
 *     override val state: String,
 *     override val zipCode: String,
 *     val name: String,
 *     val age: Int,
 * ) : Address(street, city, state, zipCode)
 * ```
 *
 * The following [AppFunctionAllOfTypeMetadata] can be used to define a data type that matches
 * PersonWithAddress.
 *
 * ```
 * val personWithAddressType = AppFunctionAllOfTypeMetadata(
 *     qualifiedName = "androidx.appfunctions.metadata.PersonWithAddress",
 *     matchAll = listOf(
 *         AppFunctionObjectTypeMetadata(
 *             properties = mapOf(
 *                 "street" to AppFunctionStringTypeMetadata(...),
 *                 "city" to AppFunctionStringTypeMetadata(...),
 *                 "state" to AppFunctionStringTypeMetadata(...),
 *                 "zipCode" to AppFunctionStringTypeMetadata(...),
 *             ),
 *             required = listOf("street", "city", "state", "zipCode"),
 *             qualifiedName = "androidx.appfunctions.metadata.Address",
 *             isNullable = false,
 *         ),
 *         AppFunctionObjectTypeMetadata(
 *             properties = mapOf(
 *                 "name" to AppFunctionStringTypeMetadata(...),
 *                 "age" to AppFunctionIntTypeMetadata(...),
 *             ),
 *             required = listOf("name", "age"),
 *             qualifiedName = "androidx.appfunctions.metadata.PersonWithAddress",
 *             isNullable = false,
 *         ),
 *     ),
 *     isNullable = false,
 * )
 * ```
 *
 * This data type can be used to define the schema of an input or output type.
 */
public class AppFunctionAllOfTypeMetadata
@JvmOverloads
constructor(
    /** The list of data types that are composed. */
    public val matchAll: List<AppFunctionDataTypeMetadata>,
    /**
     * The composed object's qualified name if available. For example,
     * "androidx.appfunctions.metadata.PersonWithAddress".
     *
     * Use this value to set [androidx.appfunctions.AppFunctionData.qualifiedName] when trying to
     * build the parameters for [androidx.appfunctions.ExecuteAppFunctionRequest].
     */
    public val qualifiedName: String?,
    /** Whether this data type is nullable. */
    isNullable: Boolean,
    /** A description of the data type and its intended use. */
    description: String = "",
) : AppFunctionDataTypeMetadata(isNullable = isNullable, description = description) {

    override fun equals(other: Any?): Boolean {
        if (!super.equals(other)) return false
        if (other !is AppFunctionAllOfTypeMetadata) return false
        if (qualifiedName != other.qualifiedName) return false
        return matchAll == other.matchAll
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + matchAll.hashCode()
        if (qualifiedName != null) {
            result = 31 * result + qualifiedName.hashCode()
        }
        return result
    }

    override fun toString(): String {
        return "AppFunctionAllOfTypeMetadata(matchAll=$matchAll, isNullable=$isNullable, description=$description)"
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    override fun toAppFunctionDataTypeMetadataDocument(): AppFunctionDataTypeMetadataDocument {
        val allOfDocuments = matchAll.map { it.toAppFunctionDataTypeMetadataDocument() }
        return AppFunctionDataTypeMetadataDocument(
            type = TYPE,
            allOf = allOfDocuments,
            isNullable = isNullable,
            objectQualifiedName = qualifiedName,
            description = description.ifEmpty { null },
        )
    }

    /** Gets a pseudo [AppFunctionObjectTypeMetadata] by merging the [matchAll] data types. */
    internal fun getPseudoObjectTypeMetadata(
        componentsMetadata: AppFunctionComponentsMetadata
    ): AppFunctionObjectTypeMetadata {
        val allProperties = mutableMapOf<String, AppFunctionDataTypeMetadata>()
        val allRequired = mutableSetOf<String>()
        for (type in matchAll) {
            when (type) {
                is AppFunctionReferenceTypeMetadata -> {
                    val resolvedType = componentsMetadata.dataTypes[type.referenceDataType]
                    if (resolvedType == null) {
                        throw IllegalStateException(
                            "Unable to resolve the ${type.referenceDataType}"
                        )
                    }
                    if (resolvedType !is AppFunctionObjectTypeMetadata) {
                        continue
                    }
                    allProperties.putAll(resolvedType.properties)
                    allRequired.addAll(resolvedType.required)
                }
                is AppFunctionObjectTypeMetadata -> {
                    allProperties.putAll(type.properties)
                    allRequired.addAll(type.required)
                }
                is AppFunctionAllOfTypeMetadata -> {
                    val pseudoObjectType = type.getPseudoObjectTypeMetadata(componentsMetadata)
                    allProperties.putAll(pseudoObjectType.properties)
                    allRequired.addAll(pseudoObjectType.required)
                }
            }
        }
        return AppFunctionObjectTypeMetadata(
            properties = allProperties,
            required = allRequired.toList(),
            qualifiedName = null,
            isNullable = false,
            description = "",
        )
    }

    public companion object {
        /**
         * All Of type.
         *
         * The [AppFunctionAllOfTypeMetadata] is used to define a component only data type object
         * that is a composition of all of the types in the list.
         *
         * The [AppFunctionAllOfTypeMetadata] can contain either:
         * * Top level [AppFunctionObjectTypeMetadata]
         * * An [AppFunctionReferenceTypeMetadata] to an outer object metadata.
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) public const val TYPE: Int = TYPE_ALL_OF
    }
}

/** Defines the schema of a object type. */
public class AppFunctionObjectTypeMetadata
@JvmOverloads
constructor(
    /** The schema of the properties of the object. */
    public val properties: Map<String, AppFunctionDataTypeMetadata>,
    /** A list of required properties' names. */
    public val required: List<String>,
    /**
     * The object's qualified name if available.
     *
     * Use this value to set [androidx.appfunctions.AppFunctionData.qualifiedName] when trying to
     * build the parameters for [androidx.appfunctions.ExecuteAppFunctionRequest].
     */
    public val qualifiedName: String?,
    /** Whether this data type is nullable. */
    isNullable: Boolean,
    /** A description of the data type and its intended use. */
    description: String = "",
) : AppFunctionDataTypeMetadata(isNullable = isNullable, description = description) {
    override fun equals(other: Any?): Boolean {
        if (!super.equals(other)) return false
        if (other !is AppFunctionObjectTypeMetadata) return false

        if (properties != other.properties) return false
        if (required != other.required) return false
        if (qualifiedName != other.qualifiedName) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + properties.hashCode()
        result = 31 * result + required.hashCode()
        if (qualifiedName != null) {
            result = 31 * result + qualifiedName.hashCode()
        }
        return result
    }

    override fun toString(): String {
        return "AppFunctionObjectTypeMetadata(" +
            "properties=$properties, " +
            "required=$required, " +
            "qualifiedName=$qualifiedName, " +
            "isNullable=$isNullable," +
            "description=$description" +
            ")"
    }

    /**
     * Converts this [AppFunctionObjectTypeMetadata] to an [AppFunctionDataTypeMetadataDocument].
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    override fun toAppFunctionDataTypeMetadataDocument(): AppFunctionDataTypeMetadataDocument {
        val properties =
            properties.map { (name, dataType) ->
                AppFunctionNamedDataTypeMetadataDocument(
                    name = checkNotNull(name),
                    dataTypeMetadata = dataType.toAppFunctionDataTypeMetadataDocument(),
                )
            }
        return AppFunctionDataTypeMetadataDocument(
            type = TYPE,
            properties = properties,
            required = required,
            objectQualifiedName = qualifiedName,
            isNullable = isNullable,
            description = description.ifEmpty { null },
        )
    }

    public companion object {
        /**
         * Object type. The schema of the object is defined in a [AppFunctionObjectTypeMetadata].
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) public const val TYPE: Int = TYPE_OBJECT
    }
}

/**
 * Represents a type that reference a data type that is defined in [AppFunctionComponentsMetadata].
 */
public class AppFunctionReferenceTypeMetadata
@JvmOverloads
constructor(
    /** The string referencing a data type defined in [AppFunctionComponentsMetadata]. */
    public val referenceDataType: String,
    /** Whether the data type is nullable. */
    isNullable: Boolean,
    /** A description of the data type and its intended use. */
    description: String = "",
) : AppFunctionDataTypeMetadata(isNullable = isNullable, description = description) {
    override fun equals(other: Any?): Boolean {
        if (!super.equals(other)) return false
        if (other !is AppFunctionReferenceTypeMetadata) return false
        return referenceDataType == other.referenceDataType
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + referenceDataType.hashCode()
        return result
    }

    override fun toString(): String {
        return "AppFunctionReferenceTypeMetadata(" +
            "referenceDataType=$referenceDataType, " +
            "isNullable=$isNullable," +
            "description=$description" +
            ")"
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    override fun toAppFunctionDataTypeMetadataDocument(): AppFunctionDataTypeMetadataDocument {
        return AppFunctionDataTypeMetadataDocument(
            type = TYPE,
            dataTypeReference = referenceDataType,
            isNullable = isNullable,
            description = description.ifEmpty { null },
        )
    }

    public companion object {
        /**
         * Object type. The schema of the object is defined in a [AppFunctionObjectTypeMetadata].
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) public const val TYPE: Int = TYPE_REFERENCE
    }
}

/**
 * Defines the schema of a int data type.
 *
 * Corresponds to a [kotlin.Int].
 */
public class AppFunctionIntTypeMetadata
@JvmOverloads
constructor(
    /** Whether the data type is nullable. */
    isNullable: Boolean,
    /** A description of the data type and its intended use. */
    description: String = "",
) : AppFunctionDataTypeMetadata(isNullable = isNullable, description = description) {

    /** Converts this [AppFunctionIntTypeMetadata] to an [AppFunctionDataTypeMetadataDocument]. */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    override fun toAppFunctionDataTypeMetadataDocument(): AppFunctionDataTypeMetadataDocument {
        return AppFunctionDataTypeMetadataDocument(
            type = TYPE_INT,
            isNullable = isNullable,
            description = description.ifEmpty { null },
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AppFunctionIntTypeMetadata) return false
        return super.equals(other)
    }

    override fun hashCode(): Int {
        return super.hashCode()
    }

    override fun toString(): String {
        return "AppFunctionIntTypeMetadata(isNullable=$isNullable, description=$description)"
    }
}

/**
 * Defines the schema of a long data type.
 *
 * Corresponds to a [kotlin.Long].
 */
public class AppFunctionLongTypeMetadata
@JvmOverloads
constructor(
    /** Whether the data type is nullable. */
    isNullable: Boolean,
    /** A description of the data type and its intended use. */
    description: String = "",
) : AppFunctionDataTypeMetadata(isNullable = isNullable, description = description) {

    /** Converts this [AppFunctionLongTypeMetadata] to an [AppFunctionDataTypeMetadataDocument]. */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    override fun toAppFunctionDataTypeMetadataDocument(): AppFunctionDataTypeMetadataDocument {
        return AppFunctionDataTypeMetadataDocument(
            type = TYPE_LONG,
            isNullable = isNullable,
            description = description.ifEmpty { null },
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AppFunctionLongTypeMetadata) return false
        return super.equals(other)
    }

    override fun hashCode(): Int {
        return super.hashCode()
    }

    override fun toString(): String {
        return "AppFunctionLongTypeMetadata(isNullable=$isNullable, description=$description)"
    }
}

/**
 * Defines the schema of a float data type.
 *
 * Corresponds to a [kotlin.Float].
 */
public class AppFunctionFloatTypeMetadata
@JvmOverloads
constructor(
    /** Whether the data type is nullable. */
    isNullable: Boolean,
    /** A description of the data type and its intended use. */
    description: String = "",
) : AppFunctionDataTypeMetadata(isNullable = isNullable, description = description) {

    /** Converts this [AppFunctionFloatTypeMetadata] to an [AppFunctionDataTypeMetadataDocument]. */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    override fun toAppFunctionDataTypeMetadataDocument(): AppFunctionDataTypeMetadataDocument {
        return AppFunctionDataTypeMetadataDocument(
            type = TYPE_FLOAT,
            isNullable = isNullable,
            description = description.ifEmpty { null },
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AppFunctionFloatTypeMetadata) return false
        return super.equals(other)
    }

    override fun hashCode(): Int {
        return super.hashCode()
    }

    override fun toString(): String {
        return "AppFunctionFloatTypeMetadata(isNullable=$isNullable, description=$description)"
    }
}

/**
 * Defines the schema of a unit data type.
 *
 * Corresponds to [kotlin.Unit].
 */
public class AppFunctionUnitTypeMetadata
@JvmOverloads
constructor(
    // Unit types are inherently not nullable in the same way other types are,
    // but the `isNullable` property is part of the base class.
    // Typically, for Unit, this would be false.
    isNullable: Boolean = false,
    /** A description of the data type and its intended use. */
    description: String = "",
) : AppFunctionDataTypeMetadata(isNullable = isNullable, description = description) {

    /** Converts this [AppFunctionUnitTypeMetadata] to an [AppFunctionDataTypeMetadataDocument]. */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    override fun toAppFunctionDataTypeMetadataDocument(): AppFunctionDataTypeMetadataDocument {
        return AppFunctionDataTypeMetadataDocument(
            type = TYPE_UNIT,
            isNullable = isNullable,
            description = description.ifEmpty { null },
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AppFunctionUnitTypeMetadata) return false
        return super.equals(other)
    }

    override fun hashCode(): Int {
        return super.hashCode()
    }

    override fun toString(): String {
        return "AppFunctionUnitTypeMetadata(isNullable=$isNullable, description=$description)"
    }
}

/**
 * Defines the schema of a boolean data type.
 *
 * Corresponds to [kotlin.Boolean].
 */
public class AppFunctionBooleanTypeMetadata
@JvmOverloads
constructor(
    /** Whether the data type is nullable. */
    isNullable: Boolean,
    /** A description of the data type and its intended use. */
    description: String = "",
) : AppFunctionDataTypeMetadata(isNullable = isNullable, description = description) {

    /**
     * Converts this [AppFunctionBooleanTypeMetadata] to an [AppFunctionDataTypeMetadataDocument].
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    override fun toAppFunctionDataTypeMetadataDocument(): AppFunctionDataTypeMetadataDocument {
        return AppFunctionDataTypeMetadataDocument(
            type = TYPE_BOOLEAN,
            isNullable = isNullable,
            description = description.ifEmpty { null },
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AppFunctionBooleanTypeMetadata) return false
        return super.equals(other)
    }

    override fun hashCode(): Int {
        return super.hashCode()
    }

    override fun toString(): String {
        return "AppFunctionBooleanTypeMetadata(isNullable=$isNullable, description=$description)"
    }
}

/**
 * Defines the schema of a byte array data type.
 *
 * Corresponds to [kotlin.ByteArray].
 */
public class AppFunctionBytesTypeMetadata
@JvmOverloads
constructor(
    /** Whether the data type is nullable. */
    isNullable: Boolean,
    /** A description of the data type and its intended use. */
    description: String = "",
) : AppFunctionDataTypeMetadata(isNullable = isNullable, description = description) {

    /** Converts this [AppFunctionBytesTypeMetadata] to an [AppFunctionDataTypeMetadataDocument]. */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    override fun toAppFunctionDataTypeMetadataDocument(): AppFunctionDataTypeMetadataDocument {
        return AppFunctionDataTypeMetadataDocument(
            type = TYPE_BYTES,
            isNullable = isNullable,
            description = description.ifEmpty { null },
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AppFunctionBytesTypeMetadata) return false
        return super.equals(other)
    }

    override fun hashCode(): Int {
        return super.hashCode()
    }

    override fun toString(): String {
        return "AppFunctionBytesTypeMetadata(isNullable=$isNullable, description=$description)"
    }
}

/**
 * Defines the schema of a double data type.
 *
 * Corresponds to [kotlin.Double] or a 64-bit floating-point number.
 */
public class AppFunctionDoubleTypeMetadata
@JvmOverloads
constructor(
    /** Whether the data type is nullable. */
    isNullable: Boolean,
    /** A description of the data type and its intended use. */
    description: String = "",
) : AppFunctionDataTypeMetadata(isNullable = isNullable, description = description) {

    /**
     * Converts this [AppFunctionDoubleTypeMetadata] to an [AppFunctionDataTypeMetadataDocument].
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    override fun toAppFunctionDataTypeMetadataDocument(): AppFunctionDataTypeMetadataDocument {
        return AppFunctionDataTypeMetadataDocument(
            type = TYPE_DOUBLE,
            isNullable = isNullable,
            description = description.ifEmpty { null },
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AppFunctionDoubleTypeMetadata) return false
        return super.equals(other)
    }

    override fun hashCode(): Int {
        return super.hashCode()
    }

    override fun toString(): String {
        return "AppFunctionDoubleTypeMetadata(isNullable=$isNullable, description=$description)"
    }
}

/**
 * Defines the schema of a string data type.
 *
 * Corresponds to [kotlin.String].
 */
public class AppFunctionStringTypeMetadata
@JvmOverloads
constructor(
    /** Whether the data type is nullable. */
    isNullable: Boolean,
    /** A description of the data type and its intended use. */
    description: String = "",
) : AppFunctionDataTypeMetadata(isNullable = isNullable, description = description) {

    /**
     * Converts this [AppFunctionStringTypeMetadata] to an [AppFunctionDataTypeMetadataDocument].
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    override fun toAppFunctionDataTypeMetadataDocument(): AppFunctionDataTypeMetadataDocument {
        return AppFunctionDataTypeMetadataDocument(
            type = TYPE_STRING,
            isNullable = isNullable,
            description = description.ifEmpty { null },
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AppFunctionStringTypeMetadata) return false
        return super.equals(other)
    }

    override fun hashCode(): Int {
        return super.hashCode()
    }

    override fun toString(): String {
        return "AppFunctionStringTypeMetadata(isNullable=$isNullable, description=$description)"
    }
}

/**
 * Defines the schema of a PendingIntent data type.
 *
 * Corresponds to [android.app.PendingIntent].
 */
public class AppFunctionPendingIntentTypeMetadata
@JvmOverloads
constructor(
    /** Whether the data type is nullable. */
    isNullable: Boolean,
    /** A description of the data type and its intended use. */
    description: String = "",
) : AppFunctionDataTypeMetadata(isNullable = isNullable, description = description) {

    /**
     * Converts this [AppFunctionPendingIntentTypeMetadata] to an
     * [AppFunctionDataTypeMetadataDocument].
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    override fun toAppFunctionDataTypeMetadataDocument(): AppFunctionDataTypeMetadataDocument {
        return AppFunctionDataTypeMetadataDocument(
            type = TYPE_PENDING_INTENT,
            isNullable = isNullable,
            description = description.ifEmpty { null },
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AppFunctionPendingIntentTypeMetadata) return false
        return super.equals(other)
    }

    override fun hashCode(): Int {
        return super.hashCode()
    }

    override fun toString(): String {
        return "AppFunctionPendingIntentTypeMetadata(isNullable=$isNullable, description=$description)"
    }
}

/** Represents the persistent storage format of the schema of a data type and its name. */
@Document
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public data class AppFunctionNamedDataTypeMetadataDocument(
    @Document.Namespace public val namespace: String = APP_FUNCTION_NAMESPACE,
    /** The id of the data type. */
    @Document.Id public val id: String = APP_FUNCTION_ID_EMPTY,
    /** The name of the data type. */
    @Document.StringProperty public val name: String,
    /** The data type metadata. */
    @Document.DocumentProperty public val dataTypeMetadata: AppFunctionDataTypeMetadataDocument,
)

/** Represents the persistent storage format of [AppFunctionDataTypeMetadata]. */
@Document
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public data class AppFunctionDataTypeMetadataDocument(
    @Document.Namespace public val namespace: String = APP_FUNCTION_NAMESPACE,
    /** The id of the data type. */
    @Document.Id public val id: String = APP_FUNCTION_ID_EMPTY,
    /** The data type. */
    @Document.LongProperty @AppFunctionDataType public val type: Int,

    /**
     * If the [type] is [AppFunctionDataTypeMetadata.TYPE_ARRAY], this specifies the array content
     * data type.
     */
    @Document.DocumentProperty public val itemType: AppFunctionDataTypeMetadataDocument? = null,
    /**
     * If the [type] is [AppFunctionDataTypeMetadata.TYPE_OBJECT], this specified the object's
     * properties.
     */
    @Document.DocumentProperty
    public val properties: List<AppFunctionNamedDataTypeMetadataDocument> = emptyList(),

    /**
     * If the [type] is [AppFunctionDataTypeMetadata.TYPE_ALL_OF], this specified the object's
     * properties.
     */
    @Document.DocumentProperty
    public val allOf: List<AppFunctionDataTypeMetadataDocument> = emptyList(),

    /**
     * If the [type] is [AppFunctionDataTypeMetadata.TYPE_OBJECT], this specified the object's
     * required properties' names.
     */
    @Document.StringProperty public val required: List<String> = emptyList(),
    /**
     * If the [type] is [AppFunctionDataTypeMetadata.TYPE_REFERENCE], this specified the reference.
     */
    @Document.StringProperty public val dataTypeReference: String? = null,
    /** Whether the type is nullable. */
    @Document.BooleanProperty public val isNullable: Boolean = false,
    /**
     * If the [type] is [AppFunctionDataTypeMetadata.TYPE_OBJECT], this specified the object's
     * qualified name if available.
     */
    @Document.StringProperty public val objectQualifiedName: String? = null,
    /** A description of the data type and its intended use. */
    @Document.StringProperty public val description: String? = null,
) {
    @SuppressLint(
        // When doesn't handle @IntDef correctly.
        "WrongConstant"
    )
    public fun toAppFunctionDataTypeMetadata(): AppFunctionDataTypeMetadata =
        when (type) {
            AppFunctionDataTypeMetadata.TYPE_ARRAY -> {
                val itemType = checkNotNull(itemType) { "Item type must be present for array type" }
                AppFunctionArrayTypeMetadata(
                    itemType = itemType.toAppFunctionDataTypeMetadata(),
                    isNullable = isNullable,
                    description = description ?: "",
                )
            }
            AppFunctionDataTypeMetadata.TYPE_OBJECT -> {
                check(properties.isNotEmpty()) {
                    "Properties must be present for object type can't be empty"
                }
                val propertiesMap =
                    properties.associate {
                        it.name to it.dataTypeMetadata.toAppFunctionDataTypeMetadata()
                    }
                AppFunctionObjectTypeMetadata(
                    properties = propertiesMap,
                    required = required,
                    qualifiedName = objectQualifiedName,
                    isNullable = isNullable,
                    description = description ?: "",
                )
            }
            AppFunctionDataTypeMetadata.TYPE_REFERENCE ->
                AppFunctionReferenceTypeMetadata(
                    referenceDataType =
                        checkNotNull(dataTypeReference) {
                            "Data type reference must be present for reference type"
                        },
                    isNullable = isNullable,
                    description = description ?: "",
                )
            AppFunctionDataTypeMetadata.TYPE_ALL_OF ->
                AppFunctionAllOfTypeMetadata(
                    matchAll = allOf.map { it.toAppFunctionDataTypeMetadata() },
                    qualifiedName = objectQualifiedName,
                    isNullable = isNullable,
                    description = description ?: "",
                )
            AppFunctionDataTypeMetadata.TYPE_INT ->
                AppFunctionIntTypeMetadata(isNullable = isNullable, description = description ?: "")
            AppFunctionDataTypeMetadata.TYPE_LONG ->
                AppFunctionLongTypeMetadata(
                    isNullable = isNullable,
                    description = description ?: "",
                )
            AppFunctionDataTypeMetadata.TYPE_FLOAT ->
                AppFunctionFloatTypeMetadata(
                    isNullable = isNullable,
                    description = description ?: "",
                )
            AppFunctionDataTypeMetadata.TYPE_UNIT ->
                AppFunctionUnitTypeMetadata(
                    isNullable = isNullable, // Or false if you want to enforce non-null for Unit
                    description = description ?: "",
                )
            AppFunctionDataTypeMetadata.TYPE_BOOLEAN ->
                AppFunctionBooleanTypeMetadata(
                    isNullable = isNullable,
                    description = description ?: "",
                )
            AppFunctionDataTypeMetadata.TYPE_BYTES ->
                AppFunctionBytesTypeMetadata(
                    isNullable = isNullable,
                    description = description ?: "",
                )
            AppFunctionDataTypeMetadata.TYPE_DOUBLE ->
                AppFunctionDoubleTypeMetadata(
                    isNullable = isNullable,
                    description = description ?: "",
                )
            AppFunctionDataTypeMetadata.TYPE_STRING ->
                AppFunctionStringTypeMetadata(
                    isNullable = isNullable,
                    description = description ?: "",
                )
            AppFunctionDataTypeMetadata.TYPE_PENDING_INTENT ->
                AppFunctionPendingIntentTypeMetadata(
                    isNullable = isNullable,
                    description = description ?: "",
                )
            else -> throw IllegalArgumentException("Unknown type: $type")
        }
}
