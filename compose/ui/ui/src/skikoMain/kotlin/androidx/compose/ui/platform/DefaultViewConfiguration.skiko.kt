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

package androidx.compose.ui.platform

import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp

class DefaultViewConfiguration(private val density: Density) : ViewConfiguration {
    override val longPressTimeoutMillis: Long
        get() = 500

    override val doubleTapTimeoutMillis: Long
        get() = 300

    override val doubleTapMinTimeMillis: Long
        get() = 40

    override val touchSlop: Float
        get() = with(density) { 18.dp.toPx() }

    override val handwritingSlop: Float
        get() = with(density) { 2.dp.toPx() }
}
