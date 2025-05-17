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

package androidx.room.processor.cache

import androidx.room.compiler.processing.XElement
import androidx.room.compiler.processing.XTypeElement
import androidx.room.processor.PropertyProcessor
import androidx.room.vo.BuiltInConverterFlags
import androidx.room.vo.DataClass
import androidx.room.vo.EmbeddedProperty
import androidx.room.vo.Entity
import androidx.room.vo.Warning

/**
 * A cache key can be used to avoid re-processing elements.
 *
 * Each context has a cache variable that uses the same backing storage as the Root Context but adds
 * current adapters and warning suppression list to the key.
 */
class Cache(
    val parent: Cache?,
    val converters: Set<XTypeElement>,
    val suppressedWarnings: Set<Warning>,
    val builtInConverterFlags: BuiltInConverterFlags,
) {
    val entities: Bucket<EntityKey, Entity> = Bucket(parent?.entities)
    val dataClasses: Bucket<DataClassKey, DataClass> = Bucket(parent?.dataClasses)

    inner class Bucket<K, T>(source: Bucket<K, T>?) {
        private val entries: MutableMap<FullKey<K>, T> = source?.entries ?: mutableMapOf()

        fun get(key: K, calculate: () -> T): T {
            val fullKey = FullKey(converters, suppressedWarnings, builtInConverterFlags, key)
            return entries.getOrPut(fullKey) { calculate() }
        }
    }

    /** Key for Entity cache */
    data class EntityKey(val element: XElement)

    /** Key for data class cache */
    data class DataClassKey(
        val element: XElement,
        val scope: PropertyProcessor.BindingScope,
        val parent: EmbeddedProperty?,
    )

    /**
     * Internal key representation with adapters & warnings included.
     *
     * Converters are kept in a linked set since the order is important for the TypeAdapterStore.
     */
    private data class FullKey<T>(
        val converters: Set<XTypeElement>,
        val suppressedWarnings: Set<Warning>,
        val builtInConverterFlags: BuiltInConverterFlags,
        val key: T,
    )
}
