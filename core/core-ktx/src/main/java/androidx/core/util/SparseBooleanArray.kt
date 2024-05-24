/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

@file:Suppress("NOTHING_TO_INLINE") // Aliases to public API.

package androidx.core.util

import android.util.SparseBooleanArray

/** Returns the number of key/value pairs in the collection. */
public inline val SparseBooleanArray.size: Int
    get() = size()

/** Returns true if the collection contains [key]. */
public inline operator fun SparseBooleanArray.contains(key: Int): Boolean = indexOfKey(key) >= 0

/** Allows the use of the index operator for storing values in the collection. */
public inline operator fun SparseBooleanArray.set(key: Int, value: Boolean): Unit = put(key, value)

/** Creates a new collection by adding or replacing entries from [other]. */
public operator fun SparseBooleanArray.plus(other: SparseBooleanArray): SparseBooleanArray {
    val new = SparseBooleanArray(size() + other.size())
    new.putAll(this)
    new.putAll(other)
    return new
}

/** Returns true if the collection contains [key]. */
public inline fun SparseBooleanArray.containsKey(key: Int): Boolean = indexOfKey(key) >= 0

/** Returns true if the collection contains [value]. */
public inline fun SparseBooleanArray.containsValue(value: Boolean): Boolean =
    indexOfValue(value) >= 0

/** Return the value corresponding to [key], or [defaultValue] when not present. */
public inline fun SparseBooleanArray.getOrDefault(key: Int, defaultValue: Boolean): Boolean =
    get(key, defaultValue)

/** Return the value corresponding to [key], or from [defaultValue] when not present. */
public inline fun SparseBooleanArray.getOrElse(key: Int, defaultValue: () -> Boolean): Boolean =
    indexOfKey(key).let { if (it >= 0) valueAt(it) else defaultValue() }

/** Return true when the collection contains no elements. */
public inline fun SparseBooleanArray.isEmpty(): Boolean = size() == 0

/** Return true when the collection contains elements. */
public inline fun SparseBooleanArray.isNotEmpty(): Boolean = size() != 0

/** Removes the entry for [key] only if it is mapped to [value]. */
public fun SparseBooleanArray.remove(key: Int, value: Boolean): Boolean {
    val index = indexOfKey(key)
    if (index >= 0 && value == valueAt(index)) {
        // Delete by key because of https://issuetracker.google.com/issues/70934959.
        delete(key)
        return true
    }
    return false
}

/** Update this collection by adding or replacing entries from [other]. */
public fun SparseBooleanArray.putAll(other: SparseBooleanArray): Unit = other.forEach(::put)

/** Performs the given [action] for each key/value entry. */
public inline fun SparseBooleanArray.forEach(action: (key: Int, value: Boolean) -> Unit) {
    for (index in 0 until size()) {
        action(keyAt(index), valueAt(index))
    }
}

/** Return an iterator over the collection's keys. */
public fun SparseBooleanArray.keyIterator(): IntIterator =
    object : IntIterator() {
        var index = 0

        override fun hasNext() = index < size()

        override fun nextInt() = keyAt(index++)
    }

/** Return an iterator over the collection's values. */
public fun SparseBooleanArray.valueIterator(): BooleanIterator =
    object : BooleanIterator() {
        var index = 0

        override fun hasNext() = index < size()

        override fun nextBoolean() = valueAt(index++)
    }
