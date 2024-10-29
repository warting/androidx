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

@file:JvmName("SavedStateReaderKt")
@file:JvmMultifileClass
@file:Suppress("NOTHING_TO_INLINE")

package androidx.savedstate

import kotlin.jvm.JvmInline
import kotlin.jvm.JvmMultifileClass
import kotlin.jvm.JvmName

@JvmInline
public actual value class SavedStateReader
@PublishedApi
internal actual constructor(
    @PublishedApi internal actual val source: SavedState,
) {

    actual inline fun getBoolean(key: String): Boolean {
        if (key !in this) keyNotFoundError(key)
        return source.map[key] as? Boolean ?: DEFAULT_BOOLEAN
    }

    actual inline fun getBooleanOrElse(key: String, defaultValue: () -> Boolean): Boolean {
        if (key !in this) defaultValue()
        return source.map[key] as? Boolean ?: defaultValue()
    }

    actual inline fun getChar(key: String): Char {
        if (key !in this) keyNotFoundError(key)
        return source.map[key] as? Char ?: DEFAULT_CHAR
    }

    actual inline fun getCharOrElse(key: String, defaultValue: () -> Char): Char {
        if (key !in this) defaultValue()
        return source.map[key] as? Char ?: defaultValue()
    }

    actual inline fun getDouble(key: String): Double {
        if (key !in this) keyNotFoundError(key)
        return source.map[key] as? Double ?: DEFAULT_DOUBLE
    }

    actual inline fun getDoubleOrElse(key: String, defaultValue: () -> Double): Double {
        if (key !in this) defaultValue()
        return source.map[key] as? Double ?: defaultValue()
    }

    actual inline fun getFloat(key: String): Float {
        if (key !in this) keyNotFoundError(key)
        return source.map[key] as? Float ?: DEFAULT_FLOAT
    }

    actual inline fun getFloatOrElse(key: String, defaultValue: () -> Float): Float {
        if (key !in this) defaultValue()
        return source.map[key] as? Float ?: defaultValue()
    }

    actual inline fun getInt(key: String): Int {
        if (key !in this) keyNotFoundError(key)
        return source.map[key] as? Int ?: DEFAULT_INT
    }

    actual inline fun getIntOrElse(key: String, defaultValue: () -> Int): Int {
        if (key !in this) defaultValue()
        return source.map[key] as? Int ?: defaultValue()
    }

    actual inline fun getLong(key: String): Long {
        if (key !in this) keyNotFoundError(key)
        return source.map[key] as? Long ?: DEFAULT_LONG
    }

    actual inline fun getLongOrElse(key: String, defaultValue: () -> Long): Long {
        if (key !in this) defaultValue()
        return source.map[key] as? Long ?: defaultValue()
    }

    actual inline fun getString(key: String): String {
        if (key !in this) keyNotFoundError(key)
        return source.map[key] as? String ?: valueNotFoundError(key)
    }

    actual inline fun getStringOrElse(key: String, defaultValue: () -> String): String {
        if (key !in this) defaultValue()
        return source.map[key] as? String ?: defaultValue()
    }

    @Suppress("UNCHECKED_CAST")
    actual inline fun getIntList(key: String): List<Int> {
        if (key !in this) keyNotFoundError(key)
        return source.map[key] as? List<Int> ?: valueNotFoundError(key)
    }

    @Suppress("UNCHECKED_CAST")
    actual inline fun getIntListOrElse(key: String, defaultValue: () -> List<Int>): List<Int> {
        if (key !in this) defaultValue()
        return source.map[key] as? List<Int> ?: defaultValue()
    }

    @Suppress("UNCHECKED_CAST")
    actual inline fun getStringList(key: String): List<String> {
        if (key !in this) keyNotFoundError(key)
        return source.map[key] as? List<String> ?: valueNotFoundError(key)
    }

    @Suppress("UNCHECKED_CAST")
    actual inline fun getStringListOrElse(
        key: String,
        defaultValue: () -> List<String>
    ): List<String> {
        if (key !in this) defaultValue()
        return source.map[key] as? List<String> ?: defaultValue()
    }

    actual inline fun getCharArray(key: String): CharArray {
        if (key !in this) keyNotFoundError(key)
        return source.map[key] as? CharArray ?: valueNotFoundError(key)
    }

    actual inline fun getCharArrayOrElse(key: String, defaultValue: () -> CharArray): CharArray {
        if (key !in this) defaultValue()
        return source.map[key] as? CharArray ?: defaultValue()
    }

    actual inline fun getBooleanArray(key: String): BooleanArray {
        if (key !in this) keyNotFoundError(key)
        return source.map[key] as? BooleanArray ?: valueNotFoundError(key)
    }

    actual inline fun getBooleanArrayOrElse(
        key: String,
        defaultValue: () -> BooleanArray
    ): BooleanArray {
        if (key !in this) defaultValue()
        return source.map[key] as? BooleanArray ?: defaultValue()
    }

    actual inline fun getDoubleArray(key: String): DoubleArray {
        if (key !in this) keyNotFoundError(key)
        return source.map[key] as? DoubleArray ?: valueNotFoundError(key)
    }

    actual inline fun getDoubleArrayOrElse(
        key: String,
        defaultValue: () -> DoubleArray,
    ): DoubleArray {
        if (key !in this) defaultValue()
        return source.map[key] as? DoubleArray ?: defaultValue()
    }

    actual inline fun getFloatArray(key: String): FloatArray {
        if (key !in this) keyNotFoundError(key)
        return source.map[key] as? FloatArray ?: valueNotFoundError(key)
    }

    actual inline fun getFloatArrayOrElse(key: String, defaultValue: () -> FloatArray): FloatArray {
        if (key !in this) defaultValue()
        return source.map[key] as? FloatArray ?: defaultValue()
    }

    actual inline fun getIntArray(key: String): IntArray {
        if (key !in this) keyNotFoundError(key)
        return source.map[key] as? IntArray ?: valueNotFoundError(key)
    }

    actual inline fun getIntArrayOrElse(key: String, defaultValue: () -> IntArray): IntArray {
        if (key !in this) defaultValue()
        return source.map[key] as? IntArray ?: defaultValue()
    }

    actual inline fun getLongArray(key: String): LongArray {
        if (key !in this) keyNotFoundError(key)
        return source.map[key] as? LongArray ?: valueNotFoundError(key)
    }

    actual inline fun getLongArrayOrElse(key: String, defaultValue: () -> LongArray): LongArray {
        if (key !in this) defaultValue()
        return source.map[key] as? LongArray ?: defaultValue()
    }

    @Suppress("UNCHECKED_CAST")
    actual inline fun getStringArray(key: String): Array<String> {
        if (key !in this) keyNotFoundError(key)
        return source.map[key] as? Array<String> ?: valueNotFoundError(key)
    }

    @Suppress("UNCHECKED_CAST")
    actual inline fun getStringArrayOrElse(
        key: String,
        defaultValue: () -> Array<String>
    ): Array<String> {
        if (key !in this) defaultValue()
        return source.map[key] as? Array<String> ?: defaultValue()
    }

    actual inline fun getSavedState(key: String): SavedState {
        if (key !in this) keyNotFoundError(key)
        return source.map[key] as? SavedState ?: valueNotFoundError(key)
    }

    actual inline fun getSavedStateOrElse(key: String, defaultValue: () -> SavedState): SavedState {
        if (key !in this) defaultValue()
        return source.map[key] as? SavedState ?: defaultValue()
    }

    actual inline fun size(): Int = source.map.size

    actual inline fun isEmpty(): Boolean = source.map.isEmpty()

    actual inline fun isNull(key: String): Boolean = contains(key) && source.map[key] == null

    actual inline operator fun contains(key: String): Boolean = source.map.containsKey(key)

    actual fun contentDeepEquals(other: SavedState): Boolean {
        // Map implements `equals` as a content deep, there is no need to do anything else.
        return source.map == other.map
    }

    actual fun toMap(): Map<String, Any?> {
        return buildMap(capacity = source.map.size) {
            for (key in source.map.keys) {
                put(key, source.map[key])
            }
        }
    }
}
