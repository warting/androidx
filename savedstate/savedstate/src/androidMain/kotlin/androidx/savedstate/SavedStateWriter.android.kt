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

@file:JvmName("SavedStateWriterKt")
@file:JvmMultifileClass
@file:Suppress("NOTHING_TO_INLINE")

package androidx.savedstate

import android.os.IBinder
import android.os.Parcelable
import android.util.Size
import android.util.SizeF
import android.util.SparseArray
import java.io.Serializable

@JvmInline
actual value class SavedStateWriter
@PublishedApi
internal actual constructor(
    @PublishedApi internal actual val source: SavedState,
) {

    /**
     * Stores an [IBinder] value associated with the specified key in the [IBinder].
     *
     * @param key The key to associate the value with.
     * @param value The [IBinder] value to store.
     */
    inline fun putBinder(key: String, value: IBinder) {
        source.putBinder(key, value)
    }

    actual inline fun putBoolean(key: String, value: Boolean) {
        source.putBoolean(key, value)
    }

    actual inline fun putChar(key: String, value: Char) {
        source.putChar(key, value)
    }

    actual inline fun putCharSequence(key: String, value: CharSequence) {
        source.putCharSequence(key, value)
    }

    actual inline fun putDouble(key: String, value: Double) {
        source.putDouble(key, value)
    }

    actual inline fun putFloat(key: String, value: Float) {
        source.putFloat(key, value)
    }

    actual inline fun putInt(key: String, value: Int) {
        source.putInt(key, value)
    }

    actual inline fun putLong(key: String, value: Long) {
        source.putLong(key, value)
    }

    actual inline fun putNull(key: String) {
        source.putString(key, null)
    }

    /**
     * Stores an [Parcelable] value associated with the specified key in the [SavedState].
     *
     * @param key The key to associate the value with.
     * @param value The [Parcelable] value to store.
     */
    inline fun <reified T : Parcelable> putParcelable(key: String, value: T) {
        source.putParcelable(key, value)
    }

    /**
     * Stores an [Serializable] value associated with the specified key in the [Serializable].
     *
     * @param key The key to associate the value with.
     * @param value The [Serializable] value to store.
     */
    inline fun <reified T : Serializable> putSerializable(key: String, value: T) {
        source.putSerializable(key, value)
    }

    /**
     * Stores an [Size] value associated with the specified key in the [Size].
     *
     * @param key The key to associate the value with.
     * @param value The [Size] value to store.
     */
    inline fun putSize(key: String, value: Size) {
        source.putSize(key, value)
    }

    /**
     * Stores an [SizeF] value associated with the specified key in the [SizeF].
     *
     * @param key The key to associate the value with.
     * @param value The [SizeF] value to store.
     */
    inline fun putSizeF(key: String, value: SizeF) {
        source.putSizeF(key, value)
    }

    actual inline fun putString(key: String, value: String) {
        source.putString(key, value)
    }

    actual inline fun putIntList(key: String, values: List<Int>) {
        source.putIntegerArrayList(key, values.toArrayListUnsafe())
    }

    actual inline fun putCharSequenceList(key: String, values: List<CharSequence>) {
        source.putCharSequenceArrayList(key, values.toArrayListUnsafe())
    }

    actual inline fun putStringList(key: String, values: List<String>) {
        source.putStringArrayList(key, values.toArrayListUnsafe())
    }

    /**
     * Stores a [List] of elements of [Parcelable] associated with the specified key in the
     * [SavedState].
     *
     * @param key The key to associate the value with.
     * @param values The [List] of elements to store.
     */
    inline fun <reified T : Parcelable> putParcelableList(key: String, values: List<T>) {
        source.putParcelableArrayList(key, values.toArrayListUnsafe())
    }

    actual inline fun putBooleanArray(key: String, values: BooleanArray) {
        source.putBooleanArray(key, values)
    }

    actual inline fun putCharArray(key: String, values: CharArray) {
        source.putCharArray(key, values)
    }

    actual inline fun putCharSequenceArray(
        key: String,
        @Suppress("ArrayReturn") values: Array<CharSequence>
    ) {
        source.putCharSequenceArray(key, values)
    }

    actual inline fun putDoubleArray(key: String, values: DoubleArray) {
        source.putDoubleArray(key, values)
    }

    actual inline fun putFloatArray(key: String, values: FloatArray) {
        source.putFloatArray(key, values)
    }

    actual inline fun putIntArray(key: String, values: IntArray) {
        source.putIntArray(key, values)
    }

    actual inline fun putLongArray(key: String, values: LongArray) {
        source.putLongArray(key, values)
    }

    actual inline fun putStringArray(key: String, values: Array<String>) {
        source.putStringArray(key, values)
    }

    /**
     * Stores a [Array] of elements of [Parcelable] associated with the specified key in the
     * [SavedState].
     *
     * @param key The key to associate the value with.
     * @param values The [Array] of elements to store.
     */
    inline fun <reified T : Parcelable> putParcelableArray(
        key: String,
        @Suppress("ArrayReturn") values: Array<T>
    ) {
        source.putParcelableArray(key, values)
    }

    /**
     * Stores a [SparseArray] of elements of [Parcelable] associated with the specified key in the
     * [SavedState].
     *
     * @param key The key to associate the value with.
     * @param values The [SparseArray] of elements to store.
     */
    inline fun <reified T : Parcelable> putSparseParcelableArray(
        key: String,
        values: SparseArray<T>
    ) {
        source.putSparseParcelableArray(key, values)
    }

    actual inline fun putSavedState(key: String, value: SavedState) {
        source.putBundle(key, value)
    }

    actual inline fun putAll(values: SavedState) {
        source.putAll(values)
    }

    actual inline fun remove(key: String) {
        source.remove(key)
    }

    actual inline fun clear() {
        source.clear()
    }
}

@Suppress("UNCHECKED_CAST", "ConcreteCollection")
@PublishedApi
internal inline fun <reified T : Any> Collection<*>.toArrayListUnsafe(): ArrayList<T> {
    return if (this is ArrayList<*>) this as ArrayList<T> else ArrayList(this as Collection<T>)
}
