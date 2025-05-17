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

@file:JvmName("ActualJvm_jvmKt")
@file:JvmMultifileClass

package androidx.compose.runtime

import androidx.compose.runtime.platform.SynchronizedObject
import kotlin.DeprecationLevel.HIDDEN

@PublishedApi
@JvmName("synchronized")
@Deprecated(
    level = HIDDEN,
    message = "not expected to be referenced directly as the old version had to be inlined",
)
internal inline fun <R> oldSynchronized(lock: SynchronizedObject, block: () -> R): R =
    androidx.compose.runtime.platform.synchronized(lock, block)
