/*
 * Copyright 2020 The Android Open Source Project
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

@file:Suppress("NOTHING_TO_INLINE")

package androidx.compose.runtime

// NOTE: rotateRight, marked @ExperimentalStdlibApi is also marked inline-only,
// which makes this usage stable.
internal inline infix fun Int.ror(other: Int) = this.rotateRight(other)

// NOTE: rotateRight, marked @ExperimentalStdlibApi is also marked inline-only,
// which makes this usage stable.
internal inline infix fun Long.ror(other: Int) = this.rotateRight(other)

// NOTE: rotateLeft, marked @ExperimentalStdlibApi is also marked inline-only,
// which makes this usage stable.
internal inline infix fun Int.rol(other: Int) = this.rotateLeft(other)

// NOTE: rotateLeft, marked @ExperimentalStdlibApi is also marked inline-only,
// which makes this usage stable.
internal inline infix fun Long.rol(other: Int) = this.rotateLeft(other)
