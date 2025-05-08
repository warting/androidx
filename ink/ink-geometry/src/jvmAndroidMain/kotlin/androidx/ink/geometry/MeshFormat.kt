/*
 * Copyright (C) 2024 The Android Open Source Project
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

package androidx.ink.geometry

import androidx.annotation.RestrictTo
import androidx.ink.nativeloader.NativeLoader
import androidx.ink.nativeloader.UsedByNative

/** Determines how the raw data of a [Mesh] is represented. */
@Suppress("NotCloseable") // Finalize is only used to free the native peer.
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // NonPublicApi
public class MeshFormat
public constructor(
    /**
     * Only for use within the ink library. Returns the native address held by this [MeshFormat].
     */
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) public val nativePointer: Long
) {

    /** Returns whether this format and [other] mesh format have the same packed representation. */
    public fun isPackedEquivalent(other: MeshFormat): Boolean {
        return this === other ||
            this.nativePointer == other.nativePointer ||
            MeshFormatNative.isPackedEquivalent(this.nativePointer, other.nativePointer)
    }

    /**
     * Returns whether this format and [other] mesh format have the same unpacked representation.
     */
    public fun isUnpackedEquivalent(other: MeshFormat): Boolean {
        return this === other ||
            this.nativePointer == other.nativePointer ||
            MeshFormatNative.isUnpackedEquivalent(this.nativePointer, other.nativePointer)
    }

    protected fun finalize() {
        MeshFormatNative.free(nativePointer)
    }

    public companion object {
        init {
            NativeLoader.load()
        }

        /**
         * Construct a [MeshFormat] from an unowned heap-allocated native pointer to a C++
         * `MeshFormat`.
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public fun wrapNative(unownedNativePointer: Long): MeshFormat =
            MeshFormat(unownedNativePointer)
    }
}

/** Singleton wrapper around native JNI calls. */
@UsedByNative
private object MeshFormatNative {
    init {
        NativeLoader.load()
    }

    @UsedByNative
    external fun isPackedEquivalent(nativePointer: Long, otherNativePointer: Long): Boolean

    @UsedByNative
    external fun isUnpackedEquivalent(nativePointer: Long, otherNativePointer: Long): Boolean

    @UsedByNative external fun free(nativePointer: Long)
}
