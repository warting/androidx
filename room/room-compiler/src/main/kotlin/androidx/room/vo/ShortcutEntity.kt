/*
 * Copyright 2019 The Android Open Source Project
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

/** Represents a shortcut function parameter entity. */
data class ShortcutEntity(
    private val entity: Entity, // the actual entity
    private val partialEntity: DataClass?, // the partial entity
) {
    val tableName = entity.tableName
    val entityClassName = entity.className
    val entityTypeName = entity.typeName
    val primaryKey by lazy {
        if (partialEntity == null) {
            entity.primaryKey
        } else {
            val partialEntityPrimaryKeyFields =
                entity.primaryKey.properties.mapNotNull {
                    partialEntity.findPropertyByColumnName(it.columnName)
                }
            entity.primaryKey.copy(properties = Properties(partialEntityPrimaryKeyFields))
        }
    }
    val dataClass = partialEntity ?: entity
    val isPartialEntity = partialEntity != null
}
