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

package androidx.wear.compose.material3.samples

import androidx.annotation.Sampled
import androidx.compose.runtime.Composable
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.TimeText
import androidx.wear.compose.material3.TimeTextDefaults

@Sampled
@Composable
fun TimeTextClockOnly() {
    // TimeText displays the current time by default.
    TimeText()
}

@Sampled
@Composable
fun TimeTextWithStatus() {
    val primaryStyle =
        TimeTextDefaults.timeTextStyle(color = MaterialTheme.colorScheme.primaryContainer)
    TimeText {
        text("ETA 12:48", style = primaryStyle)
        separator()
        time()
    }
}
