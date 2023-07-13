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

package androidx.wear.compose.material3.samples

import androidx.annotation.Sampled
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.IconToggleButton

@Sampled
@Composable
fun IconToggleButtonSample() {
    var checked by remember { mutableStateOf(true) }
    IconToggleButton(
        checked = checked,
        onCheckedChange = { checked = !checked }
    ) {
        Icon(
            imageVector = Icons.Filled.Favorite,
            contentDescription = "Flight Mode"
        )
    }
}
