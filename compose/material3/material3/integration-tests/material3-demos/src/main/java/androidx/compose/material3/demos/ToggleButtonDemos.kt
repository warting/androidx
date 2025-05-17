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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedToggleButton
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedToggleButton
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.TonalToggleButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ToggleButtonDemos() {
    val horizontalScrollState = rememberScrollState()
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxSize()) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(horizontal = 4.dp),
        ) {
            Spacer(Modifier.height(48.dp))
            Box(
                Modifier.heightIn(ToggleButtonDefaults.MinHeight),
                contentAlignment = Alignment.Center,
            ) {
                Text("XSmall")
            }
            Box(
                Modifier.heightIn(ToggleButtonDefaults.MinHeight),
                contentAlignment = Alignment.Center,
            ) {
                Text("Small")
            }
            Box(
                Modifier.heightIn(ButtonDefaults.MediumContainerHeight),
                contentAlignment = Alignment.Center,
            ) {
                Text("Medium")
            }
            Box(
                Modifier.heightIn(ButtonDefaults.LargeContainerHeight),
                contentAlignment = Alignment.Center,
            ) {
                Text("Large")
            }
            Box(
                Modifier.heightIn(ButtonDefaults.ExtraLargeContainerHeight),
                contentAlignment = Alignment.Center,
            ) {
                Text("XLarge")
            }
        }
        Row(modifier = Modifier.horizontalScroll(horizontalScrollState)) {
            ToggleButtons()
            ElevatedToggleButtons()
            TonalToggleButtons()
            OutlinedToggleButtons()
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ToggleButtons() {
    val checked = remember { mutableStateListOf(false, false, false, false, false) }
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(horizontal = 2.dp),
    ) {
        Text("Filled", modifier = Modifier.height(48.dp))

        ToggleButton(
            checked = checked[0],
            onCheckedChange = { checked[0] = it },
            modifier = Modifier.heightIn(extraSmall),
            shapes = ToggleButtonDefaults.shapesFor(extraSmall),
            contentPadding = ButtonDefaults.contentPaddingFor(extraSmall),
        ) {
            Icon(
                if (checked[0]) Icons.Filled.Edit else Icons.Outlined.Edit,
                contentDescription = "Localized description",
                modifier = Modifier.size(ButtonDefaults.iconSizeFor(extraSmall)),
            )
            Spacer(Modifier.size(ButtonDefaults.iconSpacingFor(extraSmall)))
            Text("Label", style = ButtonDefaults.textStyleFor(extraSmall))
        }

        ToggleButton(
            checked = checked[1],
            onCheckedChange = { checked[1] = it },
            modifier = Modifier.heightIn(small),
            shapes = ToggleButtonDefaults.shapesFor(small),
            contentPadding = ButtonDefaults.contentPaddingFor(small),
        ) {
            Icon(
                if (checked[1]) Icons.Filled.Edit else Icons.Outlined.Edit,
                contentDescription = "Localized description",
                modifier = Modifier.size(ButtonDefaults.iconSizeFor(small)),
            )
            Spacer(Modifier.size(ButtonDefaults.iconSpacingFor(small)))
            Text("Label", style = ButtonDefaults.textStyleFor(small))
        }

        ToggleButton(
            checked = checked[2],
            onCheckedChange = { checked[2] = it },
            modifier = Modifier.heightIn(medium),
            shapes = ToggleButtonDefaults.shapesFor(medium),
            contentPadding = ButtonDefaults.contentPaddingFor(medium),
        ) {
            Icon(
                if (checked[2]) Icons.Filled.Edit else Icons.Outlined.Edit,
                contentDescription = "Localized description",
                modifier = Modifier.size(ButtonDefaults.iconSizeFor(medium)),
            )
            Spacer(Modifier.size(ButtonDefaults.iconSpacingFor(medium)))
            Text("Label", style = ButtonDefaults.textStyleFor(medium))
        }

        ToggleButton(
            checked = checked[3],
            onCheckedChange = { checked[3] = it },
            modifier = Modifier.heightIn(large),
            shapes = ToggleButtonDefaults.shapesFor(large),
            contentPadding = ButtonDefaults.contentPaddingFor(large),
        ) {
            Icon(
                if (checked[3]) Icons.Filled.Edit else Icons.Outlined.Edit,
                contentDescription = "Localized description",
                modifier = Modifier.size(ButtonDefaults.iconSizeFor(large)),
            )
            Spacer(Modifier.size(ButtonDefaults.iconSpacingFor(large)))
            Text("Label", style = ButtonDefaults.textStyleFor(large))
        }

        ToggleButton(
            checked = checked[4],
            onCheckedChange = { checked[4] = it },
            modifier = Modifier.heightIn(extraLarge),
            shapes = ToggleButtonDefaults.shapesFor(extraLarge),
            contentPadding = ButtonDefaults.contentPaddingFor(extraLarge),
        ) {
            Icon(
                if (checked[4]) Icons.Filled.Edit else Icons.Outlined.Edit,
                contentDescription = "Localized description",
                modifier = Modifier.size(ButtonDefaults.iconSizeFor(extraLarge)),
            )
            Spacer(Modifier.size(ButtonDefaults.iconSpacingFor(extraLarge)))
            Text("Label", style = ButtonDefaults.textStyleFor(extraLarge))
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ElevatedToggleButtons() {
    val checked = remember { mutableStateListOf(false, false, false, false, false) }
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(horizontal = 2.dp),
    ) {
        Text("Elevated", modifier = Modifier.height(48.dp))

        ElevatedToggleButton(
            checked = checked[0],
            onCheckedChange = { checked[0] = it },
            modifier = Modifier.heightIn(extraSmall),
            shapes = ToggleButtonDefaults.shapesFor(extraSmall),
            contentPadding = ButtonDefaults.contentPaddingFor(extraSmall),
        ) {
            Icon(
                if (checked[0]) Icons.Filled.Edit else Icons.Outlined.Edit,
                contentDescription = "Localized description",
                modifier = Modifier.size(ButtonDefaults.iconSizeFor(extraSmall)),
            )
            Spacer(Modifier.size(ButtonDefaults.iconSpacingFor(extraSmall)))
            Text("Label", style = ButtonDefaults.textStyleFor(extraSmall))
        }

        ElevatedToggleButton(
            checked = checked[1],
            onCheckedChange = { checked[1] = it },
            modifier = Modifier.heightIn(small),
            shapes = ToggleButtonDefaults.shapesFor(small),
            contentPadding = ButtonDefaults.contentPaddingFor(small),
        ) {
            Icon(
                if (checked[1]) Icons.Filled.Edit else Icons.Outlined.Edit,
                contentDescription = "Localized description",
                modifier = Modifier.size(ButtonDefaults.iconSizeFor(small)),
            )
            Spacer(Modifier.size(ButtonDefaults.iconSpacingFor(small)))
            Text("Label", style = ButtonDefaults.textStyleFor(small))
        }

        ElevatedToggleButton(
            checked = checked[2],
            onCheckedChange = { checked[2] = it },
            modifier = Modifier.heightIn(medium),
            shapes = ToggleButtonDefaults.shapesFor(medium),
            contentPadding = ButtonDefaults.contentPaddingFor(medium),
        ) {
            Icon(
                if (checked[2]) Icons.Filled.Edit else Icons.Outlined.Edit,
                contentDescription = "Localized description",
                modifier = Modifier.size(ButtonDefaults.iconSizeFor(medium)),
            )
            Spacer(Modifier.size(ButtonDefaults.iconSpacingFor(medium)))
            Text("Label", style = ButtonDefaults.textStyleFor(medium))
        }

        ElevatedToggleButton(
            checked = checked[3],
            onCheckedChange = { checked[3] = it },
            modifier = Modifier.heightIn(large),
            shapes = ToggleButtonDefaults.shapesFor(large),
            contentPadding = ButtonDefaults.contentPaddingFor(large),
        ) {
            Icon(
                if (checked[3]) Icons.Filled.Edit else Icons.Outlined.Edit,
                contentDescription = "Localized description",
                modifier = Modifier.size(ButtonDefaults.iconSizeFor(large)),
            )
            Spacer(Modifier.size(ButtonDefaults.iconSpacingFor(large)))
            Text("Label", style = ButtonDefaults.textStyleFor(large))
        }

        ElevatedToggleButton(
            checked = checked[4],
            onCheckedChange = { checked[4] = it },
            modifier = Modifier.heightIn(extraLarge),
            shapes = ToggleButtonDefaults.shapesFor(extraLarge),
            contentPadding = ButtonDefaults.contentPaddingFor(extraLarge),
        ) {
            Icon(
                if (checked[4]) Icons.Filled.Edit else Icons.Outlined.Edit,
                contentDescription = "Localized description",
                modifier = Modifier.size(ButtonDefaults.iconSizeFor(extraLarge)),
            )
            Spacer(Modifier.size(ButtonDefaults.iconSpacingFor(extraLarge)))
            Text("Label", style = ButtonDefaults.textStyleFor(extraLarge))
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun TonalToggleButtons() {
    val checked = remember { mutableStateListOf(false, false, false, false, false) }
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(horizontal = 2.dp),
    ) {
        Text("Tonal", modifier = Modifier.height(48.dp))

        TonalToggleButton(
            checked = checked[0],
            onCheckedChange = { checked[0] = it },
            modifier = Modifier.heightIn(extraSmall),
            shapes = ToggleButtonDefaults.shapesFor(extraSmall),
            contentPadding = ButtonDefaults.contentPaddingFor(extraSmall),
        ) {
            Icon(
                if (checked[0]) Icons.Filled.Edit else Icons.Outlined.Edit,
                contentDescription = "Localized description",
                modifier = Modifier.size(ButtonDefaults.iconSizeFor(extraSmall)),
            )
            Spacer(Modifier.size(ButtonDefaults.iconSpacingFor(extraSmall)))
            Text("Label", style = ButtonDefaults.textStyleFor(extraSmall))
        }

        TonalToggleButton(
            checked = checked[1],
            onCheckedChange = { checked[1] = it },
            modifier = Modifier.heightIn(small),
            shapes = ToggleButtonDefaults.shapesFor(small),
            contentPadding = ButtonDefaults.contentPaddingFor(small),
        ) {
            Icon(
                if (checked[1]) Icons.Filled.Edit else Icons.Outlined.Edit,
                contentDescription = "Localized description",
                modifier = Modifier.size(ButtonDefaults.iconSizeFor(small)),
            )
            Spacer(Modifier.size(ButtonDefaults.iconSpacingFor(small)))
            Text("Label", style = ButtonDefaults.textStyleFor(small))
        }

        TonalToggleButton(
            checked = checked[2],
            onCheckedChange = { checked[2] = it },
            modifier = Modifier.heightIn(medium),
            shapes = ToggleButtonDefaults.shapesFor(medium),
            contentPadding = ButtonDefaults.contentPaddingFor(medium),
        ) {
            Icon(
                if (checked[2]) Icons.Filled.Edit else Icons.Outlined.Edit,
                contentDescription = "Localized description",
                modifier = Modifier.size(ButtonDefaults.iconSizeFor(medium)),
            )
            Spacer(Modifier.size(ButtonDefaults.iconSpacingFor(medium)))
            Text("Label", style = ButtonDefaults.textStyleFor(medium))
        }

        TonalToggleButton(
            checked = checked[3],
            onCheckedChange = { checked[3] = it },
            modifier = Modifier.heightIn(large),
            shapes = ToggleButtonDefaults.shapesFor(large),
            contentPadding = ButtonDefaults.contentPaddingFor(large),
        ) {
            Icon(
                if (checked[3]) Icons.Filled.Edit else Icons.Outlined.Edit,
                contentDescription = "Localized description",
                modifier = Modifier.size(ButtonDefaults.iconSizeFor(large)),
            )
            Spacer(Modifier.size(ButtonDefaults.iconSpacingFor(large)))
            Text("Label", style = ButtonDefaults.textStyleFor(large))
        }

        TonalToggleButton(
            checked = checked[4],
            onCheckedChange = { checked[4] = it },
            modifier = Modifier.heightIn(extraLarge),
            shapes = ToggleButtonDefaults.shapesFor(extraLarge),
            contentPadding = ButtonDefaults.contentPaddingFor(extraLarge),
        ) {
            Icon(
                if (checked[4]) Icons.Filled.Edit else Icons.Outlined.Edit,
                contentDescription = "Localized description",
                modifier = Modifier.size(ButtonDefaults.iconSizeFor(extraLarge)),
            )
            Spacer(Modifier.size(ButtonDefaults.iconSpacingFor(extraLarge)))
            Text("Label", style = ButtonDefaults.textStyleFor(extraLarge))
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun OutlinedToggleButtons() {
    val checked = remember { mutableStateListOf(false, false, false, false, false) }
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(horizontal = 2.dp),
    ) {
        Text("Outlined", modifier = Modifier.height(48.dp))

        OutlinedToggleButton(
            checked = checked[0],
            onCheckedChange = { checked[0] = it },
            modifier = Modifier.heightIn(extraSmall),
            shapes = ToggleButtonDefaults.shapesFor(extraSmall),
            contentPadding = ButtonDefaults.contentPaddingFor(extraSmall),
        ) {
            Icon(
                if (checked[0]) Icons.Filled.Edit else Icons.Outlined.Edit,
                contentDescription = "Localized description",
                modifier = Modifier.size(ButtonDefaults.iconSizeFor(extraSmall)),
            )
            Spacer(Modifier.size(ButtonDefaults.iconSpacingFor(extraSmall)))
            Text("Label", style = ButtonDefaults.textStyleFor(extraSmall))
        }

        OutlinedToggleButton(
            checked = checked[1],
            onCheckedChange = { checked[1] = it },
            modifier = Modifier.heightIn(small),
            shapes = ToggleButtonDefaults.shapesFor(small),
            contentPadding = ButtonDefaults.contentPaddingFor(small),
        ) {
            Icon(
                if (checked[1]) Icons.Filled.Edit else Icons.Outlined.Edit,
                contentDescription = "Localized description",
                modifier = Modifier.size(ButtonDefaults.iconSizeFor(small)),
            )
            Spacer(Modifier.size(ButtonDefaults.iconSpacingFor(small)))
            Text("Label", style = ButtonDefaults.textStyleFor(small))
        }

        OutlinedToggleButton(
            checked = checked[2],
            onCheckedChange = { checked[2] = it },
            modifier = Modifier.heightIn(medium),
            shapes = ToggleButtonDefaults.shapesFor(medium),
            contentPadding = ButtonDefaults.contentPaddingFor(medium),
        ) {
            Icon(
                if (checked[2]) Icons.Filled.Edit else Icons.Outlined.Edit,
                contentDescription = "Localized description",
                modifier = Modifier.size(ButtonDefaults.iconSizeFor(medium)),
            )
            Spacer(Modifier.size(ButtonDefaults.iconSpacingFor(medium)))
            Text("Label", style = ButtonDefaults.textStyleFor(medium))
        }

        OutlinedToggleButton(
            checked = checked[3],
            onCheckedChange = { checked[3] = it },
            modifier = Modifier.heightIn(large),
            shapes = ToggleButtonDefaults.shapesFor(large),
            contentPadding = ButtonDefaults.contentPaddingFor(large),
        ) {
            Icon(
                if (checked[3]) Icons.Filled.Edit else Icons.Outlined.Edit,
                contentDescription = "Localized description",
                modifier = Modifier.size(ButtonDefaults.iconSizeFor(large)),
            )
            Spacer(Modifier.size(ButtonDefaults.iconSpacingFor(large)))
            Text("Label", style = ButtonDefaults.textStyleFor(large))
        }

        OutlinedToggleButton(
            checked = checked[4],
            onCheckedChange = { checked[4] = it },
            modifier = Modifier.heightIn(extraLarge),
            shapes = ToggleButtonDefaults.shapesFor(extraLarge),
            contentPadding = ButtonDefaults.contentPaddingFor(extraLarge),
        ) {
            Icon(
                if (checked[4]) Icons.Filled.Edit else Icons.Outlined.Edit,
                contentDescription = "Localized description",
                modifier = Modifier.size(ButtonDefaults.iconSizeFor(extraLarge)),
            )
            Spacer(Modifier.size(ButtonDefaults.iconSpacingFor(extraLarge)))
            Text("Label", style = ButtonDefaults.textStyleFor(extraLarge))
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
val extraSmall = ButtonDefaults.ExtraSmallContainerHeight
val small = ButtonDefaults.MinHeight
@OptIn(ExperimentalMaterial3ExpressiveApi::class) val medium = ButtonDefaults.MediumContainerHeight
@OptIn(ExperimentalMaterial3ExpressiveApi::class) val large = ButtonDefaults.LargeContainerHeight
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
val extraLarge = ButtonDefaults.ExtraLargeContainerHeight
