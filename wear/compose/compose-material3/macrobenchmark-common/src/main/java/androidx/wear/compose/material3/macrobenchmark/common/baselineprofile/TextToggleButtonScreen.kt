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

package androidx.wear.compose.material3.macrobenchmark.common.baselineprofile

import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.TextToggleButton
import androidx.wear.compose.material3.TextToggleButtonDefaults
import androidx.wear.compose.material3.macrobenchmark.common.FIND_OBJECT_TIMEOUT_MS
import androidx.wear.compose.material3.macrobenchmark.common.MacrobenchmarkScreen
import androidx.wear.compose.material3.samples.LargeTextToggleButtonSample
import androidx.wear.compose.material3.samples.TextToggleButtonSample

@OptIn(ExperimentalLayoutApi::class)
val TextToggleButtonScreen =
    object : MacrobenchmarkScreen {
        override val content: @Composable (BoxScope.() -> Unit)
            get() = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    FlowRow {
                        TextToggleButtonSample()
                        LargeTextToggleButtonSample()
                        val checked = remember { mutableStateOf(false) }
                        TextToggleButton(
                            onCheckedChange = { checked.value = !checked.value },
                            shapes = TextToggleButtonDefaults.animatedShapes(),
                            checked = checked.value,
                            modifier =
                                Modifier.semantics { contentDescription = ToggleButtonDescription },
                        ) {
                            Text(text = "ABC")
                        }
                        TextToggleButton(
                            onCheckedChange = { checked.value = !checked.value },
                            shapes = TextToggleButtonDefaults.variantAnimatedShapes(),
                            checked = checked.value,
                        ) {
                            Text(text = "ABC")
                        }
                    }
                }
            }

        override val exercise: MacrobenchmarkScope.() -> Unit
            get() = {
                device.wait(
                    Until.findObject(By.desc(ToggleButtonDescription)),
                    FIND_OBJECT_TIMEOUT_MS,
                )
                device.findObject(By.desc(ToggleButtonDescription)).click()
                device.waitForIdle()
            }
    }

private const val ToggleButtonDescription = "ToggleButtonDescription"
