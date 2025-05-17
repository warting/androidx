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

package androidx.wear.compose.material3.demos

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.integration.demos.common.Centralize
import androidx.wear.compose.integration.demos.common.ComposableDemo
import androidx.wear.compose.integration.demos.common.Material3DemoCategory
import androidx.wear.compose.material3.AngularDirection
import androidx.wear.compose.material3.ArcProgressIndicator
import androidx.wear.compose.material3.ArcProgressIndicatorDefaults
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.CircularProgressIndicator
import androidx.wear.compose.material3.CircularProgressIndicatorDefaults
import androidx.wear.compose.material3.IconButtonDefaults
import androidx.wear.compose.material3.LinearProgressIndicator
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ProgressIndicatorDefaults
import androidx.wear.compose.material3.SegmentedCircularProgressIndicator
import androidx.wear.compose.material3.Slider
import androidx.wear.compose.material3.SliderDefaults
import androidx.wear.compose.material3.Stepper
import androidx.wear.compose.material3.StepperDefaults
import androidx.wear.compose.material3.SwitchButton
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.samples.CircularProgressIndicatorCustomAnimationSample
import androidx.wear.compose.material3.samples.FullScreenProgressIndicatorSample
import androidx.wear.compose.material3.samples.IndeterminateProgressArcSample
import androidx.wear.compose.material3.samples.IndeterminateProgressIndicatorSample
import androidx.wear.compose.material3.samples.LinearProgressIndicatorSample
import androidx.wear.compose.material3.samples.MediaButtonProgressIndicatorSample
import androidx.wear.compose.material3.samples.OverflowProgressIndicatorSample
import androidx.wear.compose.material3.samples.SegmentedProgressIndicatorBinarySample
import androidx.wear.compose.material3.samples.SegmentedProgressIndicatorSample
import androidx.wear.compose.material3.samples.SmallSegmentedProgressIndicatorBinarySample
import androidx.wear.compose.material3.samples.SmallSegmentedProgressIndicatorSample
import androidx.wear.compose.material3.samples.SmallValuesProgressIndicatorSample

val ProgressIndicatorDemos =
    listOf(
        Material3DemoCategory(
            title = "Circular progress",
            listOf(
                ComposableDemo("Full screen") {
                    Centralize { FullScreenProgressIndicatorSample() }
                },
                ComposableDemo("Media button wrapping") {
                    Centralize { MediaButtonProgressIndicatorSample() }
                },
                ComposableDemo("Overflow progress (>100%)") {
                    Centralize { OverflowProgressIndicatorSample() }
                },
                ComposableDemo("Small sized indicator") {
                    Box(
                        modifier =
                            Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
                    ) {
                        Button({ /* No op */ }, modifier = Modifier.align(Alignment.Center)) {
                            Text(
                                "Loading...",
                                modifier = Modifier.align(Alignment.CenterVertically),
                            )
                            Spacer(modifier = Modifier.size(10.dp))
                            CircularProgressIndicator(
                                progress = { 0.75f },
                                modifier = Modifier.size(IconButtonDefaults.DefaultButtonSize),
                                startAngle = 120f,
                                endAngle = 60f,
                                strokeWidth = CircularProgressIndicatorDefaults.smallStrokeWidth,
                                colors =
                                    ProgressIndicatorDefaults.colors(indicatorColor = Color.Red),
                            )
                        }
                    }
                },
                ComposableDemo("Small progress values") {
                    Centralize { SmallValuesProgressIndicatorSample() }
                },
                ComposableDemo("Indeterminate progress") {
                    Centralize { IndeterminateProgressIndicatorSample() }
                },
                ComposableDemo("Customize") {
                    Centralize { CircularProgressCustomisableFullScreenDemo() }
                },
                ComposableDemo("Custom animation") {
                    Centralize { CircularProgressIndicatorCustomAnimationSample() }
                },
            ),
        ),
        Material3DemoCategory(
            title = "Segmented progress",
            listOf(
                ComposableDemo("Full screen") { Centralize { SegmentedProgressIndicatorSample() } },
                ComposableDemo("Binary") {
                    Centralize { SegmentedProgressIndicatorBinarySample() }
                },
                ComposableDemo("Small size") {
                    Centralize { SmallSegmentedProgressIndicatorSample() }
                },
                ComposableDemo("Small size binary") {
                    Centralize { SmallSegmentedProgressIndicatorBinarySample() }
                },
                ComposableDemo("Binary with switch") {
                    Centralize { SegmentedProgressIndicatorBinarySwitchDemo() }
                },
                ComposableDemo("Customize") {
                    Centralize { SegmentedProgressCustomisableFullScreenDemo() }
                },
            ),
        ),
        Material3DemoCategory(
            title = "Linear progress",
            listOf(
                ComposableDemo("Linear Samples") {
                    Centralize { LinearProgressIndicatorSamples() }
                },
                ComposableDemo("Animation") { Centralize { LinearProgressIndicatorAnimatedDemo() } },
            ),
        ),
        Material3DemoCategory(
            title = "Arc Progress Indicator",
            listOf(
                ComposableDemo("Indeterminate arc") {
                    Centralize { IndeterminateProgressArcSample() }
                },
                ComposableDemo("Custom indeterminate arc") {
                    Centralize { ArcProgressCustomisableFullScreenDemo() }
                },
            ),
        ),
    )

@Composable
fun LinearProgressIndicatorSamples() {
    Box(modifier = Modifier.background(MaterialTheme.colorScheme.background).fillMaxSize()) {
        ScalingLazyColumn(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            item { ListHeader { Text("Progress 0%") } }
            item { LinearProgressIndicatorSample(progress = { 0f }) }
            item { ListHeader { Text("Progress 1%") } }
            item { LinearProgressIndicatorSample(progress = { 0.01f }) }
            item { ListHeader { Text("Progress 50%") } }
            item { LinearProgressIndicatorSample(progress = { 0.5f }) }
            item { ListHeader { Text("Progress 100%") } }
            item { LinearProgressIndicatorSample(progress = { 1f }) }
            item { ListHeader { Text("Disabled 0%") } }
            item { LinearProgressIndicatorSample(progress = { 0f }, enabled = false) }
            item { ListHeader { Text("Disabled 50%") } }
            item { LinearProgressIndicatorSample(progress = { 0.5f }, enabled = false) }
            item { ListHeader { Text("Disabled 100%") } }
            item { LinearProgressIndicatorSample(progress = { 1f }, enabled = false) }
        }
    }
}

@Composable
fun CircularProgressCustomisableFullScreenDemo() {
    val progress = remember { mutableFloatStateOf(0f) }
    val startAngle = remember { mutableFloatStateOf(360f) }
    val endAngle = remember { mutableFloatStateOf(360f) }
    val enabled = remember { mutableStateOf(true) }
    val overflowAllowed = remember { mutableStateOf(true) }
    val hasLargeStroke = remember { mutableStateOf(true) }
    val hasCustomColors = remember { mutableStateOf(false) }
    val colors =
        if (hasCustomColors.value) {
            ProgressIndicatorDefaults.colors(
                indicatorColor = Color.Green,
                trackColor = Color.Yellow,
                overflowTrackColor = Color.Red,
            )
        } else {
            ProgressIndicatorDefaults.colors()
        }
    val strokeWidth =
        if (hasLargeStroke.value) CircularProgressIndicatorDefaults.largeStrokeWidth
        else CircularProgressIndicatorDefaults.smallStrokeWidth

    Box(
        modifier =
            Modifier.background(MaterialTheme.colorScheme.background)
                .padding(CircularProgressIndicatorDefaults.FullScreenPadding)
                .fillMaxSize()
    ) {
        ProgressIndicatorCustomizer(
            progress = progress,
            startAngle = startAngle,
            endAngle = endAngle,
            enabled = enabled,
            overflowAllowed = overflowAllowed,
            hasLargeStroke = hasLargeStroke,
            hasCustomColors = hasCustomColors,
        )

        CircularProgressIndicator(
            progress = { progress.value },
            startAngle = startAngle.value,
            endAngle = endAngle.value,
            enabled = enabled.value,
            allowProgressOverflow = overflowAllowed.value,
            strokeWidth = strokeWidth,
            colors = colors,
        )
    }
}

@Composable
fun SegmentedProgressCustomisableFullScreenDemo() {
    val progress = remember { mutableFloatStateOf(0f) }
    val startAngle = remember { mutableFloatStateOf(0f) }
    val endAngle = remember { mutableFloatStateOf(0f) }
    val enabled = remember { mutableStateOf(true) }
    val overflowAllowed = remember { mutableStateOf(true) }
    val hasCustomColors = remember { mutableStateOf(false) }
    val hasLargeStroke = remember { mutableStateOf(true) }
    val numSegments = remember { mutableIntStateOf(5) }
    val colors =
        if (hasCustomColors.value) {
            ProgressIndicatorDefaults.colors(
                indicatorColor = Color.Green,
                trackColor = Color.Yellow,
                overflowTrackColor = Color.Red,
            )
        } else {
            ProgressIndicatorDefaults.colors()
        }
    val strokeWidth =
        if (hasLargeStroke.value) CircularProgressIndicatorDefaults.largeStrokeWidth
        else CircularProgressIndicatorDefaults.smallStrokeWidth

    Box(
        modifier =
            Modifier.background(MaterialTheme.colorScheme.background)
                .padding(CircularProgressIndicatorDefaults.FullScreenPadding)
                .fillMaxSize()
    ) {
        ProgressIndicatorCustomizer(
            progress = progress,
            startAngle = startAngle,
            endAngle = endAngle,
            enabled = enabled,
            hasLargeStroke = hasLargeStroke,
            hasCustomColors = hasCustomColors,
            numSegments = numSegments,
            overflowAllowed = overflowAllowed,
        )

        SegmentedCircularProgressIndicator(
            segmentCount = numSegments.value,
            progress = { progress.value },
            startAngle = startAngle.value,
            endAngle = endAngle.value,
            enabled = enabled.value,
            allowProgressOverflow = overflowAllowed.value,
            strokeWidth = strokeWidth,
            colors = colors,
        )
    }
}

@Composable
fun ArcProgressCustomisableFullScreenDemo() {
    val startAngle = remember {
        mutableFloatStateOf(ArcProgressIndicatorDefaults.IndeterminateStartAngle)
    }
    val endAngle = remember {
        mutableFloatStateOf(ArcProgressIndicatorDefaults.IndeterminateEndAngle)
    }
    val defaultDiameter = ArcProgressIndicatorDefaults.recommendedIndeterminateDiameter
    val diameter = remember { mutableFloatStateOf(defaultDiameter.value) }
    val strokeWidth = remember {
        mutableFloatStateOf(ArcProgressIndicatorDefaults.IndeterminateStrokeWidth.value)
    }
    val angularDirection = remember { mutableStateOf(AngularDirection.CounterClockwise) }
    val hasCustomColors = remember { mutableStateOf(false) }
    val colors =
        if (hasCustomColors.value) {
            ProgressIndicatorDefaults.colors(
                indicatorColor = Color.Green,
                trackColor = Color.Green.copy(alpha = 0.5f),
                overflowTrackColor = Color.Green.copy(alpha = 0.7f),
            )
        } else {
            ProgressIndicatorDefaults.colors()
        }

    Box(
        modifier =
            Modifier.background(MaterialTheme.colorScheme.background)
                .padding(CircularProgressIndicatorDefaults.FullScreenPadding)
                .fillMaxSize()
    ) {
        ArcIndicatorCustomizer(
            startAngle = startAngle,
            endAngle = endAngle,
            diameter = diameter,
            strokeWidth = strokeWidth,
            angularDirection = angularDirection,
            hasCustomColors = hasCustomColors,
        )

        Centralize {
            ArcProgressIndicator(
                startAngle = startAngle.floatValue,
                endAngle = endAngle.floatValue,
                strokeWidth = strokeWidth.floatValue.dp,
                angularDirection = angularDirection.value,
                colors = colors,
                modifier = Modifier.size(diameter.floatValue.dp),
            )
        }
    }
}

@Composable
fun ProgressIndicatorCustomizer(
    progress: MutableState<Float>,
    startAngle: MutableState<Float>,
    endAngle: MutableState<Float>,
    enabled: MutableState<Boolean>,
    hasLargeStroke: MutableState<Boolean>,
    hasCustomColors: MutableState<Boolean>,
    overflowAllowed: MutableState<Boolean>,
    numSegments: MutableState<Int>? = null,
) {
    ScalingLazyColumn(
        modifier = Modifier.fillMaxSize().padding(12.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        item { Text(String.format("Progress: %.0f%%", progress.value * 100)) }
        item {
            Slider(
                value = progress.value,
                onValueChange = { progress.value = it },
                valueRange = 0f..2f,
                steps = 9,
                colors =
                    SliderDefaults.sliderColors(
                        containerColor = MaterialTheme.colorScheme.background
                    ),
                segmented = false,
            )
        }
        if (numSegments != null) {
            item { Text("Segments: ${numSegments.value}") }
            item {
                Slider(
                    value = numSegments.value.toFloat(),
                    onValueChange = { numSegments.value = it.toInt() },
                    valueRange = 1f..12f,
                    steps = 10,
                    colors =
                        SliderDefaults.sliderColors(
                            containerColor = MaterialTheme.colorScheme.background
                        ),
                )
            }
        }
        item { Text("Start Angle: ${startAngle.value.toInt()}") }
        item {
            Slider(
                value = startAngle.value,
                onValueChange = { startAngle.value = it },
                valueRange = 0f..360f,
                steps = 7,
                segmented = false,
                colors =
                    SliderDefaults.sliderColors(
                        containerColor = MaterialTheme.colorScheme.background
                    ),
            )
        }
        item { Text("End angle: ${endAngle.value.toInt()}") }
        item {
            Slider(
                value = endAngle.value,
                onValueChange = { endAngle.value = it },
                valueRange = 0f..360f,
                steps = 7,
                segmented = false,
                colors =
                    SliderDefaults.sliderColors(
                        containerColor = MaterialTheme.colorScheme.background
                    ),
            )
        }
        item {
            SwitchButton(
                modifier = Modifier.fillMaxWidth().padding(6.dp),
                checked = enabled.value,
                onCheckedChange = { enabled.value = it },
                label = { Text("Enabled") },
            )
        }
        item {
            SwitchButton(
                modifier = Modifier.fillMaxWidth().padding(6.dp),
                checked = hasLargeStroke.value,
                onCheckedChange = { hasLargeStroke.value = it },
                label = { Text("Large stroke") },
            )
        }
        item {
            SwitchButton(
                modifier = Modifier.fillMaxWidth().padding(6.dp),
                checked = hasCustomColors.value,
                onCheckedChange = { hasCustomColors.value = it },
                label = { Text("Custom colors") },
            )
        }
        item {
            SwitchButton(
                modifier = Modifier.fillMaxWidth().padding(6.dp),
                checked = overflowAllowed.value,
                onCheckedChange = { overflowAllowed.value = it },
                label = { Text("Overflow") },
            )
        }
    }
}

@Composable
fun SegmentedProgressIndicatorBinarySwitchDemo() {
    var isEven by remember { mutableStateOf(true) }
    Box(
        modifier =
            Modifier.background(MaterialTheme.colorScheme.background)
                .padding(CircularProgressIndicatorDefaults.FullScreenPadding)
                .fillMaxSize()
    ) {
        SwitchButton(
            modifier = Modifier.align(Alignment.Center),
            checked = isEven,
            onCheckedChange = { isEven = it },
            label = { Text(if (isEven) "Even" else "Odd") },
        )
        SegmentedCircularProgressIndicator(
            segmentCount = 6,
            segmentValue = if (isEven) { it -> it % 2 != 0 } else { it -> it % 2 != 1 },
        )
    }
}

@Composable
fun ArcIndicatorCustomizer(
    startAngle: MutableState<Float>,
    endAngle: MutableState<Float>,
    diameter: MutableState<Float>,
    strokeWidth: MutableState<Float>,
    angularDirection: MutableState<AngularDirection>,
    hasCustomColors: MutableState<Boolean>,
) {
    ScalingLazyColumn(
        modifier = Modifier.fillMaxSize().padding(12.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        item { Text("Start Angle: ${startAngle.value.toInt()}") }
        item {
            Slider(
                value = startAngle.value,
                onValueChange = { startAngle.value = it },
                valueRange = 0f..360f,
                steps = 35,
                segmented = false,
                colors =
                    SliderDefaults.sliderColors(
                        containerColor = MaterialTheme.colorScheme.background
                    ),
            )
        }
        item { Text("End angle: ${endAngle.value.toInt()}") }
        item {
            Slider(
                value = endAngle.value,
                onValueChange = { endAngle.value = it },
                valueRange = 0f..360f,
                steps = 35,
                segmented = false,
                colors =
                    SliderDefaults.sliderColors(
                        containerColor = MaterialTheme.colorScheme.background
                    ),
            )
        }
        item { Text("Diameter: ${diameter.value.toInt()}") }
        item {
            Slider(
                value = diameter.value,
                onValueChange = { diameter.value = it },
                valueRange = 10f..400f,
                steps = 38,
                segmented = false,
                colors =
                    SliderDefaults.sliderColors(
                        containerColor = MaterialTheme.colorScheme.background
                    ),
            )
        }
        item { Text("StrokeWidth: ${strokeWidth.value}") }
        item {
            Slider(
                value = strokeWidth.value,
                onValueChange = { strokeWidth.value = it },
                valueRange = 1f..20f,
                steps = 18,
                segmented = false,
                colors =
                    SliderDefaults.sliderColors(
                        containerColor = MaterialTheme.colorScheme.background
                    ),
            )
        }
        item {
            SwitchButton(
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                checked = angularDirection.value == AngularDirection.Clockwise,
                onCheckedChange = {
                    angularDirection.value =
                        if (it) AngularDirection.Clockwise else AngularDirection.CounterClockwise
                },
                label = { Text("Clockwise") },
            )
        }
        item {
            SwitchButton(
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                checked = hasCustomColors.value,
                onCheckedChange = { hasCustomColors.value = it },
                label = { Text("Custom colors") },
            )
        }
    }
}

@Composable
fun LinearProgressIndicatorAnimatedDemo() {
    var progress by remember { mutableFloatStateOf(0.25f) }
    val valueRange = 0f..1f

    Box(modifier = Modifier.fillMaxSize()) {
        Stepper(
            value = progress,
            onValueChange = { progress = it },
            valueRange = valueRange,
            steps = 3,
            decreaseIcon = { DecreaseIcon(StepperDefaults.IconSize) },
            increaseIcon = { IncreaseIcon(StepperDefaults.IconSize) },
        ) {
            ScalingLazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                item { Text(String.format("Progress: %.0f%%", progress * 100)) }
                item {
                    LinearProgressIndicator(
                        modifier = Modifier.padding(top = 8.dp),
                        progress = { progress },
                    )
                }
            }
        }
    }
}
