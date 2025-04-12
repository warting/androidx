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

package androidx.xr.runtime.internal

import androidx.annotation.RestrictTo

/** The dimensions of a UI element in pixels. These are always two dimensional. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public class PixelDimensions(@JvmField public val width: Int, @JvmField public val height: Int) {

    override fun toString(): String {
        return super.toString() + ": w $width x h $height"
    }
}
