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

package androidx.wear.compose.foundation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.ViewConfiguration

@Composable
internal fun CustomTouchSlopProvider(newTouchSlop: Float, content: @Composable () -> Unit) {
    CompositionLocalProvider(
        value =
            LocalViewConfiguration provides
                CustomTouchSlop(newTouchSlop, LocalViewConfiguration.current),
        content = content,
    )
}

private class CustomTouchSlop(
    private val customTouchSlop: Float,
    currentConfiguration: ViewConfiguration,
) : ViewConfiguration by currentConfiguration {
    override val touchSlop: Float
        get() = customTouchSlop
}
