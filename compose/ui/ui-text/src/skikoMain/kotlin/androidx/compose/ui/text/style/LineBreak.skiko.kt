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

package androidx.compose.ui.text.style

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable

@Immutable
@JvmInline
actual value class LineBreak private constructor(
    internal val mask: Int
) {
    actual companion object {
        @Stable
        actual val Simple: LineBreak = LineBreak(1)

        @Stable
        actual val Heading: LineBreak = LineBreak(2)

        @Stable
        actual val Paragraph: LineBreak = LineBreak(3)

        @Stable
        actual val Unspecified: LineBreak = LineBreak(Int.MIN_VALUE)
    }
}
