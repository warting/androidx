/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.room.writer

import androidx.room.compiler.codegen.XCodeBlock
import androidx.room.ext.RoomTypeNames
import androidx.room.vo.DataClass
import androidx.room.vo.ShortcutEntity

class EntityUpsertAdapterWriter
private constructor(val tableName: String, val dataClass: DataClass) {
    companion object {
        fun create(entity: ShortcutEntity): EntityUpsertAdapterWriter {
            return EntityUpsertAdapterWriter(
                tableName = entity.tableName,
                dataClass = entity.dataClass,
            )
        }
    }

    fun createConcrete(entity: ShortcutEntity, typeWriter: TypeWriter): XCodeBlock {
        val upsertAdapter = RoomTypeNames.UPSERT_ADAPTER.parametrizedBy(dataClass.typeName)
        val insertHelper = EntityInsertAdapterWriter.create(entity, "").createAnonymous(typeWriter)
        val updateHelper = EntityUpdateAdapterWriter.create(entity, "").createAnonymous(typeWriter)
        return XCodeBlock.ofNewInstance(
            typeName = upsertAdapter,
            argsFormat = "%L, %L",
            args = arrayOf(insertHelper, updateHelper),
        )
    }
}
