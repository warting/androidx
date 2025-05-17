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

package androidx.compose.material3.demos

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledIconToggleButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilledTonalIconToggleButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.IconButtonDefaults.IconButtonWidthOption.Companion.Narrow
import androidx.compose.material3.IconButtonDefaults.IconButtonWidthOption.Companion.Wide
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.OutlinedIconToggleButton
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun IconButtonMeasurementsDemo() {
    val rowScrollState = rememberScrollState()
    Row(modifier = Modifier.horizontalScroll(rowScrollState)) {
        val columnScrollState = rememberScrollState()
        val padding = 16.dp
        Column(
            modifier = Modifier.padding(horizontal = padding).verticalScroll(columnScrollState)
        ) {
            Spacer(modifier = Modifier.height(padding + 48.dp))
            Text("XSmall", modifier = Modifier.height(48.dp + padding))
            Text("Small", modifier = Modifier.height(48.dp + padding))
            Text("Medium", modifier = Modifier.height(56.dp + padding))
            Text("Large", modifier = Modifier.height(96.dp + padding))
            Text("XLarge", modifier = Modifier.height(136.dp + padding))
        }

        // Default
        Column(
            modifier =
                Modifier.padding(horizontal = padding)
                    .width(136.dp)
                    .verticalScroll(columnScrollState),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(padding),
        ) {
            Text("Default", modifier = Modifier.height(48.dp))
            // XSmall uniform round icon button
            FilledIconButton(
                onClick = { /* doSomething() */ },
                modifier = Modifier.size(IconButtonDefaults.extraSmallContainerSize()),
                shape = IconButtonDefaults.extraSmallRoundShape,
            ) {
                Icon(
                    Icons.Outlined.Lock,
                    contentDescription = "Localized description",
                    modifier = Modifier.size(IconButtonDefaults.extraSmallIconSize),
                )
            }

            // Small uniform round icon button
            FilledIconButton(
                onClick = { /* doSomething() */ },
                modifier = Modifier.size(IconButtonDefaults.smallContainerSize()),
                shape = IconButtonDefaults.smallRoundShape,
            ) {
                Icon(
                    Icons.Outlined.Lock,
                    contentDescription = "Localized description",
                    modifier = Modifier.size(IconButtonDefaults.smallIconSize),
                )
            }

            // Medium uniform round icon button
            FilledIconButton(
                onClick = { /* doSomething() */ },
                modifier = Modifier.size(IconButtonDefaults.mediumContainerSize()),
                shape = IconButtonDefaults.mediumRoundShape,
            ) {
                Icon(
                    Icons.Outlined.Lock,
                    contentDescription = "Localized description",
                    modifier = Modifier.size(IconButtonDefaults.mediumIconSize),
                )
            }

            // Large uniform round icon button
            FilledIconButton(
                onClick = { /* doSomething() */ },
                modifier = Modifier.size(IconButtonDefaults.largeContainerSize()),
                shape = IconButtonDefaults.largeRoundShape,
            ) {
                Icon(
                    Icons.Outlined.Lock,
                    contentDescription = "Localized description",
                    modifier = Modifier.size(IconButtonDefaults.largeIconSize),
                )
            }

            // XLarge uniform round icon button
            FilledIconButton(
                onClick = { /* doSomething() */ },
                modifier = Modifier.size(IconButtonDefaults.extraLargeContainerSize()),
                shape = IconButtonDefaults.extraLargeRoundShape,
            ) {
                Icon(
                    Icons.Outlined.Lock,
                    contentDescription = "Localized description",
                    modifier = Modifier.size(IconButtonDefaults.extraLargeIconSize),
                )
            }
        }

        // Narrow
        Column(
            modifier =
                Modifier.padding(horizontal = padding)
                    .width(104.dp)
                    .verticalScroll(columnScrollState),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(padding),
        ) {
            Text("Narrow", modifier = Modifier.height(48.dp))

            // XSmall narrow round icon button
            FilledIconButton(
                onClick = { /* doSomething() */ },
                modifier = Modifier.size(IconButtonDefaults.extraSmallContainerSize(Narrow)),
                shape = IconButtonDefaults.extraSmallRoundShape,
            ) {
                Icon(
                    Icons.Outlined.Lock,
                    contentDescription = "Localized description",
                    modifier = Modifier.size(IconButtonDefaults.extraSmallIconSize),
                )
            }

            // Small narrow round icon button
            FilledIconButton(
                onClick = { /* doSomething() */ },
                modifier = Modifier.size(IconButtonDefaults.smallContainerSize(Narrow)),
                shape = IconButtonDefaults.smallRoundShape,
            ) {
                Icon(
                    Icons.Outlined.Lock,
                    contentDescription = "Localized description",
                    modifier = Modifier.size(IconButtonDefaults.smallIconSize),
                )
            }

            // Medium narrow round icon button
            FilledIconButton(
                onClick = { /* doSomething() */ },
                modifier = Modifier.size(IconButtonDefaults.mediumContainerSize(Narrow)),
                shape = IconButtonDefaults.mediumRoundShape,
            ) {
                Icon(
                    Icons.Outlined.Lock,
                    contentDescription = "Localized description",
                    modifier = Modifier.size(IconButtonDefaults.mediumIconSize),
                )
            }

            // Large narrow round icon button
            FilledIconButton(
                onClick = { /* doSomething() */ },
                modifier = Modifier.size(IconButtonDefaults.largeContainerSize(Narrow)),
                shape = IconButtonDefaults.largeRoundShape,
            ) {
                Icon(
                    Icons.Outlined.Lock,
                    contentDescription = "Localized description",
                    modifier = Modifier.size(IconButtonDefaults.largeIconSize),
                )
            }

            // XLarge narrow round icon button
            FilledIconButton(
                onClick = { /* doSomething() */ },
                modifier = Modifier.size(IconButtonDefaults.extraLargeContainerSize(Narrow)),
                shape = IconButtonDefaults.extraLargeRoundShape,
            ) {
                Icon(
                    Icons.Outlined.Lock,
                    contentDescription = "Localized description",
                    modifier = Modifier.size(IconButtonDefaults.extraLargeIconSize),
                )
            }
        }

        // Wide
        Column(
            modifier =
                Modifier.padding(horizontal = padding)
                    .width(184.dp)
                    .verticalScroll(columnScrollState),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(padding),
        ) {
            Text("Wide", modifier = Modifier.height(48.dp))

            // XSmall wide round icon button
            FilledIconButton(
                onClick = { /* doSomething() */ },
                modifier = Modifier.size(IconButtonDefaults.extraSmallContainerSize(Wide)),
                shape = IconButtonDefaults.extraSmallRoundShape,
            ) {
                Icon(
                    Icons.Outlined.Lock,
                    contentDescription = "Localized description",
                    modifier = Modifier.size(IconButtonDefaults.extraSmallIconSize),
                )
            }
            // Small wide round icon button
            FilledIconButton(
                onClick = { /* doSomething() */ },
                modifier = Modifier.size(IconButtonDefaults.smallContainerSize(Wide)),
                shape = IconButtonDefaults.smallRoundShape,
            ) {
                Icon(
                    Icons.Outlined.Lock,
                    contentDescription = "Localized description",
                    modifier = Modifier.size(IconButtonDefaults.smallIconSize),
                )
            }

            // medium wide round icon button
            FilledIconButton(
                onClick = { /* doSomething() */ },
                modifier = Modifier.size(IconButtonDefaults.mediumContainerSize(Wide)),
                shape = IconButtonDefaults.mediumRoundShape,
            ) {
                Icon(
                    Icons.Outlined.Lock,
                    contentDescription = "Localized description",
                    modifier = Modifier.size(IconButtonDefaults.mediumIconSize),
                )
            }

            // Large wide round icon button
            FilledIconButton(
                onClick = { /* doSomething() */ },
                modifier = Modifier.size(IconButtonDefaults.largeContainerSize(Wide)),
                shape = IconButtonDefaults.largeRoundShape,
            ) {
                Icon(
                    Icons.Outlined.Lock,
                    contentDescription = "Localized description",
                    modifier = Modifier.size(IconButtonDefaults.largeIconSize),
                )
            }

            // XLarge wide round icon button
            FilledIconButton(
                onClick = { /* doSomething() */ },
                modifier = Modifier.size(IconButtonDefaults.extraLargeContainerSize(Wide)),
                shape = IconButtonDefaults.extraLargeRoundShape,
            ) {
                Icon(
                    Icons.Outlined.Lock,
                    contentDescription = "Localized description",
                    modifier = Modifier.size(IconButtonDefaults.extraLargeIconSize),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun IconButtonCornerRadiusDemo() {
    Column {
        val rowScrollState = rememberScrollState()
        val padding = 16.dp
        // uniform round row
        Row(
            modifier =
                Modifier.height(150.dp)
                    .horizontalScroll(rowScrollState)
                    .padding(horizontal = padding),
            horizontalArrangement = Arrangement.spacedBy(padding),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // extra small round icon button
            OutlinedIconButton(
                onClick = { /* doSomething() */ },
                modifier = Modifier.size(IconButtonDefaults.extraSmallContainerSize()),
                shape = IconButtonDefaults.extraSmallRoundShape,
            ) {
                Icon(
                    Icons.Outlined.Lock,
                    contentDescription = "Localized description",
                    modifier = Modifier.size(IconButtonDefaults.extraSmallIconSize),
                )
            }

            // Small round icon button
            OutlinedIconButton(
                onClick = { /* doSomething() */ },
                modifier = Modifier.size(IconButtonDefaults.smallContainerSize()),
                shape = IconButtonDefaults.smallRoundShape,
            ) {
                Icon(
                    Icons.Outlined.Lock,
                    contentDescription = "Localized description",
                    modifier = Modifier.size(IconButtonDefaults.smallIconSize),
                )
            }

            // Medium round icon button
            OutlinedIconButton(
                onClick = { /* doSomething() */ },
                modifier =
                    Modifier.minimumInteractiveComponentSize()
                        .size(IconButtonDefaults.mediumContainerSize()),
                shape = IconButtonDefaults.mediumRoundShape,
            ) {
                Icon(
                    Icons.Outlined.Lock,
                    contentDescription = "Localized description",
                    modifier = Modifier.size(IconButtonDefaults.mediumIconSize),
                )
            }

            // Large uniform round icon button
            OutlinedIconButton(
                onClick = { /* doSomething() */ },
                modifier =
                    Modifier.minimumInteractiveComponentSize()
                        .size(IconButtonDefaults.largeContainerSize()),
                shape = IconButtonDefaults.largeRoundShape,
            ) {
                Icon(
                    Icons.Outlined.Lock,
                    contentDescription = "Localized description",
                    modifier = Modifier.size(IconButtonDefaults.largeIconSize),
                )
            }

            // XLarge uniform round icon button
            OutlinedIconButton(
                onClick = { /* doSomething() */ },
                modifier =
                    Modifier.minimumInteractiveComponentSize()
                        .size(IconButtonDefaults.extraLargeContainerSize()),
                shape = IconButtonDefaults.extraLargeRoundShape,
            ) {
                Icon(
                    Icons.Outlined.Lock,
                    contentDescription = "Localized description",
                    modifier = Modifier.size(IconButtonDefaults.extraLargeIconSize),
                )
            }
        }

        // uniform square row
        Row(
            modifier =
                Modifier.height(150.dp)
                    .horizontalScroll(rowScrollState)
                    .padding(horizontal = padding),
            horizontalArrangement = Arrangement.spacedBy(padding),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // extra small square icon button
            OutlinedIconButton(
                onClick = { /* doSomething() */ },
                modifier =
                    Modifier
                        // .minimumInteractiveComponentSize()
                        .size(IconButtonDefaults.extraSmallContainerSize()),
                shape = IconButtonDefaults.extraSmallSquareShape,
            ) {
                Icon(
                    Icons.Outlined.Lock,
                    contentDescription = "Localized description",
                    modifier = Modifier.size(IconButtonDefaults.extraSmallIconSize),
                )
            }

            // Small round icon button
            OutlinedIconButton(
                onClick = { /* doSomething() */ },
                modifier =
                    Modifier
                        // .minimumInteractiveComponentSize()
                        .size(IconButtonDefaults.smallContainerSize()),
                shape = IconButtonDefaults.smallSquareShape,
            ) {
                Icon(
                    Icons.Outlined.Lock,
                    contentDescription = "Localized description",
                    modifier = Modifier.size(IconButtonDefaults.smallIconSize),
                )
            }

            // Medium round icon button
            OutlinedIconButton(
                onClick = { /* doSomething() */ },
                modifier =
                    Modifier.minimumInteractiveComponentSize()
                        .size(IconButtonDefaults.mediumContainerSize()),
                shape = IconButtonDefaults.mediumSquareShape,
            ) {
                Icon(
                    Icons.Outlined.Lock,
                    contentDescription = "Localized description",
                    modifier = Modifier.size(IconButtonDefaults.mediumIconSize),
                )
            }

            // Large uniform round icon button
            OutlinedIconButton(
                onClick = { /* doSomething() */ },
                modifier =
                    Modifier.minimumInteractiveComponentSize()
                        .size(IconButtonDefaults.largeContainerSize()),
                shape = IconButtonDefaults.largeSquareShape,
            ) {
                Icon(
                    Icons.Outlined.Lock,
                    contentDescription = "Localized description",
                    modifier = Modifier.size(IconButtonDefaults.largeIconSize),
                )
            }

            // XLarge uniform round icon button
            OutlinedIconButton(
                onClick = { /* doSomething() */ },
                modifier =
                    Modifier.minimumInteractiveComponentSize()
                        .size(IconButtonDefaults.extraLargeContainerSize()),
                shape = IconButtonDefaults.extraLargeSquareShape,
            ) {
                Icon(
                    Icons.Outlined.Lock,
                    contentDescription = "Localized description",
                    modifier = Modifier.size(IconButtonDefaults.extraLargeIconSize),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun IconButtonAndToggleButtonsDemo() {
    Column {
        val rowScrollState = rememberScrollState()
        val padding = 16.dp

        Row(
            modifier =
                Modifier.height(150.dp)
                    .horizontalScroll(rowScrollState)
                    .padding(horizontal = padding),
            horizontalArrangement = Arrangement.spacedBy(padding),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Spacer(Modifier.width(72.dp))
            Text("Filled")
            Text("Tonal")
            Text("Outline")
            Text("Standard")
        }
        // icon buttons
        Row(
            modifier =
                Modifier.height(150.dp)
                    .horizontalScroll(rowScrollState)
                    .padding(horizontal = padding),
            horizontalArrangement = Arrangement.spacedBy(padding),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Spacer(Modifier.width(76.dp))

            FilledIconButton(onClick = {}, shapes = IconButtonDefaults.shapes()) {
                Icon(Icons.Outlined.Edit, contentDescription = "Localized description")
            }

            FilledTonalIconButton(onClick = {}, shapes = IconButtonDefaults.shapes()) {
                Icon(Icons.Outlined.Edit, contentDescription = "Localized description")
            }

            OutlinedIconButton(onClick = {}, shapes = IconButtonDefaults.shapes()) {
                Icon(Icons.Outlined.Edit, contentDescription = "Localized description")
            }

            IconButton(onClick = {}, shapes = IconButtonDefaults.shapes()) {
                Icon(Icons.Outlined.Edit, contentDescription = "Localized description")
            }
        }

        // unselected icon toggle buttons
        Row(
            modifier =
                Modifier.height(150.dp)
                    .horizontalScroll(rowScrollState)
                    .padding(horizontal = padding),
            horizontalArrangement = Arrangement.spacedBy(padding),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            var checked by remember { mutableStateOf(false) }
            Text(
                text =
                    if (!checked) {
                        "Unselected"
                    } else {
                        "Selected"
                    },
                modifier = Modifier.defaultMinSize(minWidth = 76.dp),
            )

            FilledIconToggleButton(
                checked = checked,
                onCheckedChange = { checked = it },
                shapes = IconButtonDefaults.toggleableShapes(),
            ) {
                IconFor(checked)
            }

            FilledTonalIconToggleButton(
                checked = checked,
                onCheckedChange = { checked = it },
                shapes = IconButtonDefaults.toggleableShapes(),
            ) {
                IconFor(checked)
            }

            OutlinedIconToggleButton(
                checked = checked,
                onCheckedChange = { checked = it },
                shapes = IconButtonDefaults.toggleableShapes(),
            ) {
                IconFor(checked)
            }

            IconToggleButton(
                checked = checked,
                onCheckedChange = { checked = it },
                shapes = IconButtonDefaults.toggleableShapes(),
            ) {
                IconFor(checked)
            }
        }

        // selected icon toggle buttons
        Row(
            modifier =
                Modifier.height(150.dp)
                    .horizontalScroll(rowScrollState)
                    .padding(horizontal = padding),
            horizontalArrangement = Arrangement.spacedBy(padding),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            var checked by remember { mutableStateOf(true) }

            Text(
                text =
                    if (!checked) {
                        "Unselected"
                    } else {
                        "Selected"
                    },
                modifier = Modifier.defaultMinSize(minWidth = 76.dp),
            )

            FilledIconToggleButton(
                checked = checked,
                onCheckedChange = { checked = it },
                shapes = IconButtonDefaults.toggleableShapes(),
            ) {
                IconFor(checked)
            }

            FilledTonalIconToggleButton(
                checked = checked,
                onCheckedChange = { checked = it },
                shapes = IconButtonDefaults.toggleableShapes(),
            ) {
                IconFor(checked)
            }

            OutlinedIconToggleButton(
                checked = checked,
                onCheckedChange = { checked = it },
                shapes = IconButtonDefaults.toggleableShapes(),
            ) {
                IconFor(checked)
            }

            IconToggleButton(
                checked = checked,
                onCheckedChange = { checked = it },
                shapes = IconButtonDefaults.toggleableShapes(),
            ) {
                IconFor(checked)
            }
        }
    }
}

@Composable
private fun IconFor(checked: Boolean) {
    if (checked) {
        Icon(Icons.Filled.Edit, contentDescription = "Localized description")
    } else {
        Icon(Icons.Outlined.Edit, contentDescription = "Localized description")
    }
}
