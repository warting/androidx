/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.wear.compose.integration.demos

import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.PositionIndicator
import androidx.wear.compose.material.Stepper
import androidx.wear.compose.material.StepperDefaults
import androidx.wear.compose.material.Text

@Composable
fun StepperDemo() {
    var value by remember { mutableFloatStateOf(2f) }
    Stepper(
        value = value,
        onValueChange = { value = it },
        valueRange = 1f..4f,
        increaseIcon = { Icon(StepperDefaults.Increase, "Increase") },
        decreaseIcon = { Icon(StepperDefaults.Decrease, "Decrease") },
        steps = 2,
    ) {
        Text("Value: $value")
    }
}

@Composable
fun StepperWithIntegerDemo() {
    var value by remember { mutableIntStateOf(2) }
    Stepper(
        value = value,
        onValueChange = { value = it },
        increaseIcon = { Icon(StepperDefaults.Increase, "Increase") },
        decreaseIcon = { Icon(StepperDefaults.Decrease, "Decrease") },
        valueProgression = 1..10,
    ) {
        Text("Value: $value")
    }
}

@Composable
fun StepperWithScrollBarDemo() {
    var valueState by remember { mutableFloatStateOf(4f) }
    val range = 0f..10f

    Stepper(
        value = valueState,
        onValueChange = { valueState = it },
        increaseIcon = { Icon(StepperDefaults.Increase, "Increase") },
        decreaseIcon = { Icon(StepperDefaults.Decrease, "Decrease") },
        valueRange = range,
        steps = 9,
    ) {
        Chip(
            onClick = { valueState = if (valueState == 0f) 4f else 0f },
            modifier = Modifier.width(146.dp),
            colors = ChipDefaults.secondaryChipColors(),
            label = { Text("Volume : $valueState") },
            icon = {
                Icon(
                    painter =
                        painterResource(
                            id =
                                if (valueState > 0) R.drawable.ic_volume_up_24px
                                else R.drawable.ic_volume_off_24px
                        ),
                    contentDescription = null,
                )
            },
        )
    }

    PositionIndicator(value = { valueState }, range = range)
}

@Composable
fun StepperWithCustomColors() {
    var valueState by remember { mutableFloatStateOf(4f) }
    val range = 0f..10f

    Stepper(
        value = valueState,
        onValueChange = { valueState = it },
        valueRange = range,
        increaseIcon = { Icon(StepperDefaults.Increase, "Increase") },
        decreaseIcon = { Icon(StepperDefaults.Decrease, "Decrease") },
        steps = 9,
        contentColor = AlternatePrimaryColor2,
    ) {
        Text("Volume : $valueState")
    }

    PositionIndicator(value = { valueState }, range = range)
}
