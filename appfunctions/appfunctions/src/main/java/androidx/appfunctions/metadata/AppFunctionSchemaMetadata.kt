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

/**
 * Represents a predefined AppFunction schema.
 *
 * A schema defines a function's input parameters and output. This class holds identifying
 * information about a specific, SDK-provided schema.
 */
public class AppFunctionSchemaMetadata(
    /**
     * Specifies the category of the schema used by this function. This allows for logical grouping
     * of schemas. For instance, all schemas related to email functionality would be categorized as
     * 'email'.
     */
    public val category: String,
    /** The unique name of the schema within its category. */
    public val name: String,
    /** The version of the schema. This is used to track the changes to the schema over time. */
    public val version: Long
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AppFunctionSchemaMetadata

        if (version != other.version) return false
        if (category != other.category) return false
        if (name != other.name) return false

        return true
    }

    override fun hashCode(): Int {
        var result = version.hashCode()
        result = 31 * result + category.hashCode()
        result = 31 * result + name.hashCode()
        return result
    }

    override fun toString(): String {
        return "AppFunctionSchemaMetadata(category='$category', name='$name', version=$version)"
    }
}
