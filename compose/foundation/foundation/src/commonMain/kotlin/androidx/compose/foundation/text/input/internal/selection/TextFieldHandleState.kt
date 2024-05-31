/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.compose.foundation.text.input.internal.selection

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.style.ResolvedTextDirection

/** Defines how to render a selection or cursor handle on a TextField. */
internal data class TextFieldHandleState(
    val visible: Boolean,
    val position: Offset,
    val direction: ResolvedTextDirection,
    val handlesCrossed: Boolean
) {
    companion object {
        val Hidden =
            TextFieldHandleState(
                visible = false,
                position = Offset.Unspecified,
                direction = ResolvedTextDirection.Ltr,
                handlesCrossed = false
            )
    }
}
