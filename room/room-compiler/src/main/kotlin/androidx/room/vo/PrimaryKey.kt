/*
 * Copyright (C) 2017 The Android Open Source Project
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
package androidx.room.vo

import androidx.room.compiler.processing.XElement
import androidx.room.migration.bundle.PrimaryKeyBundle

/** Represents a PrimaryKey for an Entity. */
data class PrimaryKey(
    val declaredIn: XElement?,
    override val properties: Properties,
    val autoGenerateId: Boolean,
) : HasSchemaIdentity, HasProperties {
    companion object {
        val MISSING = PrimaryKey(null, Properties(), false)
    }

    fun toHumanReadableString(): String {
        return "PrimaryKey[${properties.joinToString(separator = ", ", transform = Property::getPath) }]"
    }

    fun toBundle(): PrimaryKeyBundle = PrimaryKeyBundle(autoGenerateId, columnNames)

    override fun getIdKey() = "$autoGenerateId-$columnNames"
}
