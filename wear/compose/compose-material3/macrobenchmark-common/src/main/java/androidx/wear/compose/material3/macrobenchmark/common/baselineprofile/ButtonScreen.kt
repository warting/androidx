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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.wear.compose.material3.IconButtonDefaults
import androidx.wear.compose.material3.macrobenchmark.common.MacrobenchmarkScreen
import androidx.wear.compose.material3.macrobenchmark.common.R
import androidx.wear.compose.material3.macrobenchmark.common.scrollDown
import androidx.wear.compose.material3.samples.ButtonSample
import androidx.wear.compose.material3.samples.ButtonWithImageSample
import androidx.wear.compose.material3.samples.ChildButtonSample
import androidx.wear.compose.material3.samples.CompactButtonSample
import androidx.wear.compose.material3.samples.FilledTonalButtonSample
import androidx.wear.compose.material3.samples.FilledTonalCompactButtonSample
import androidx.wear.compose.material3.samples.FilledVariantButtonSample
import androidx.wear.compose.material3.samples.IconButtonWithImageSample
import androidx.wear.compose.material3.samples.OutlinedButtonSample
import androidx.wear.compose.material3.samples.OutlinedCompactButtonSample

val ButtonScreen =
    object : MacrobenchmarkScreen {
        override val content: @Composable BoxScope.() -> Unit
            get() = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    ButtonSample()
                    FilledTonalButtonSample()
                    FilledVariantButtonSample()
                    OutlinedButtonSample()
                    ChildButtonSample()
                    ButtonWithImageSample()
                    CompactButtonSample()
                    FilledTonalCompactButtonSample()
                    OutlinedCompactButtonSample()
                    IconButtonWithImageSample(
                        painterResource(R.drawable.backgroundimage),
                        enabled = true,
                        shapes = IconButtonDefaults.animatedShapes(),
                    )
                }
            }

        override val exercise: MacrobenchmarkScope.() -> Unit
            get() = { device.scrollDown() }
    }
