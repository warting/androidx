/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.compose.ui.demos.focus

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRestorer

@Composable
fun FocusRestorationInLazyListDemo() {
    LazyColumn(Modifier.focusRestorer()) {
        items(count = 30) { rowIndex ->
            key(rowIndex) {
                LazyRow(Modifier.focusRestorer()) {
                    items(count = 30) { columnIndex ->
                        key(rowIndex, columnIndex) {
                            Button(onClick = {}) { Text(text = "($rowIndex, $columnIndex)") }
                        }
                    }
                }
            }
        }
    }
}
