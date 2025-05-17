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

package androidx.wear.compose.material3.macrobenchmark.common

import android.os.SystemClock
import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.EdgeButton
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import kotlinx.coroutines.launch

val ScalingLazyColumnBenchmark =
    object : MacrobenchmarkScreen {
        override val content: @Composable (BoxScope.() -> Unit)
            get() = {
                val state = rememberScalingLazyListState()
                val coroutineScope = rememberCoroutineScope()
                AppScaffold {
                    ScreenScaffold(
                        state,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 20.dp),
                        edgeButton = {
                            EdgeButton(
                                onClick = { coroutineScope.launch { state.scrollToItem(1) } }
                            ) {
                                Text("To top")
                            }
                        },
                    ) { contentPadding ->
                        ScalingLazyColumn(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            contentPadding = contentPadding,
                            state = state,
                            modifier =
                                Modifier.background(MaterialTheme.colorScheme.background)
                                    .semantics { contentDescription = CONTENT_DESCRIPTION },
                        ) {
                            items(5000) {
                                Text(
                                    "Item $it",
                                    color = MaterialTheme.colorScheme.onSurface,
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier.fillMaxWidth().padding(10.dp),
                                )
                            }
                        }
                    }
                }
            }

        override val exercise: MacrobenchmarkScope.() -> Unit
            get() = {
                val swipeStartY = device.displayHeight * 9 / 10 // scroll up
                val swipeEndY = device.displayHeight / 2
                val midX = device.displayWidth / 2
                repeat(20) {
                    device.swipe(midX, swipeStartY, midX, swipeEndY, 5)
                    device.waitForIdle()
                    SystemClock.sleep(500)
                }
            }
    }
