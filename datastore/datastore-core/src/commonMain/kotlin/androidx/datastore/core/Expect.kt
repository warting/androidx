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
package androidx.datastore.core

import kotlinx.coroutines.CoroutineDispatcher

/** Common IOException to be defined in jvm and native code. */
expect open class IOException(message: String?, cause: Throwable?) : Exception {
    constructor(message: String?)
}

internal expect class AtomicInt {
    constructor(initialValue: Int = 0)

    fun getAndIncrement(): Int

    fun incrementAndGet(): Int

    fun decrementAndGet(): Int

    fun get(): Int
}

internal expect class AtomicBoolean {
    constructor(initialValue: Boolean)

    fun get(): Boolean

    fun set(value: Boolean)
}

internal expect fun ioDispatcher(): CoroutineDispatcher
