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

package androidx.wear.compose.material3.demos

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.ChildButton
import androidx.wear.compose.material3.FilledTonalButton
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.OutlinedButton
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.samples.ButtonSample
import androidx.wear.compose.material3.samples.ChildButtonSample
import androidx.wear.compose.material3.samples.FilledTonalButtonSample
import androidx.wear.compose.material3.samples.OutlinedButtonSample
import androidx.wear.compose.material3.samples.SimpleButtonSample
import androidx.wear.compose.material3.samples.SimpleChildButtonSample
import androidx.wear.compose.material3.samples.SimpleFilledTonalButtonSample
import androidx.wear.compose.material3.samples.SimpleOutlinedButtonSample

@Composable
fun ButtonDemo() {
    ScalingLazyColumn(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        item {
            ListHeader {
                Text("1 slot button")
            }
        }
        item {
            SimpleButtonSample()
        }
        item {
            Button(
                onClick = { /* Do something */ },
                label = { Text("Button") },
                enabled = false
            )
        }
        item {
            ListHeader {
                Text("3 slot button")
            }
        }
        item {
            ButtonSample()
        }
        item {
            Button(
                onClick = { /* Do something */ },
                label = { Text("Button") },
                secondaryLabel = { Text("Secondary label") },
                icon = {
                    Icon(
                        Icons.Filled.Favorite,
                        contentDescription = "Favorite icon",
                        modifier = Modifier.size(ButtonDefaults.IconSize)
                    )
                },
                enabled = false
            )
        }
    }
}

@Composable
fun FilledTonalButtonDemo() {
    ScalingLazyColumn(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        item {
            ListHeader {
                Text("1 slot button")
            }
        }
        item {
            SimpleFilledTonalButtonSample()
        }
        item {
            FilledTonalButton(
                onClick = { /* Do something */ },
                label = { Text("Filled Tonal Button") },
                enabled = false
            )
        }
        item {
            ListHeader {
                Text("3 slot button")
            }
        }
        item {
            FilledTonalButtonSample()
        }
        item {
            FilledTonalButton(
                onClick = { /* Do something */ },
                label = { Text("Filled Tonal Button") },
                secondaryLabel = { Text("Secondary label") },
                icon = {
                    Icon(
                        Icons.Filled.Favorite,
                        contentDescription = "Favorite icon",
                        modifier = Modifier.size(ButtonDefaults.IconSize)
                    )
                },
                enabled = false
            )
        }
    }
}

@Composable
fun OutlinedButtonDemo() {
    ScalingLazyColumn(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        item {
            ListHeader {
                Text("1 slot button")
            }
        }
        item {
            SimpleOutlinedButtonSample()
        }
        item {
            OutlinedButton(
                onClick = { /* Do something */ },
                label = { Text("Outlined Button") },
                enabled = false
            )
        }
        item {
            ListHeader {
                Text("3 slot button")
            }
        }
        item {
            OutlinedButtonSample()
        }
        item {
            OutlinedButton(
                onClick = { /* Do something */ },
                label = { Text("Outlined Button") },
                secondaryLabel = { Text("Secondary label") },
                icon = {
                    Icon(
                        Icons.Filled.Favorite,
                        contentDescription = "Favorite icon",
                        modifier = Modifier.size(ButtonDefaults.IconSize)
                    )
                },
                enabled = false
            )
        }
    }
}

@Composable
fun ChildButtonDemo() {
    ScalingLazyColumn(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        item {
            ListHeader {
                Text("1 slot button")
            }
        }
        item {
            SimpleChildButtonSample()
        }
        item {
            ChildButton(
                onClick = { /* Do something */ },
                label = { Text("Child Button") },
                enabled = false
            )
        }
        item {
            ListHeader {
                Text("3 slot button")
            }
        }
        item {
            ChildButtonSample()
        }
        item {
            ChildButton(
                onClick = { /* Do something */ },
                label = { Text("Child Button") },
                secondaryLabel = { Text("Secondary label") },
                icon = {
                    Icon(
                        Icons.Filled.Favorite,
                        contentDescription = "Favorite icon",
                        modifier = Modifier.size(ButtonDefaults.IconSize)
                    )
                },
                enabled = false
            )
        }
    }
}

@Composable
fun MultilineButtonDemo() {
    ScalingLazyColumn(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        item {
            ListHeader {
                Text("3 line label")
            }
        }
        item {
            MultilineButton(enabled = true)
        }
        item {
            MultilineButton(enabled = false)
        }
        item {
            MultilineButton(enabled = true, icon = { StandardIcon() })
        }
        item {
            MultilineButton(enabled = false, icon = { StandardIcon() })
        }
        item {
            ListHeader {
                Text("5 line button")
            }
        }
        item {
            Multiline3SlotButton(enabled = true)
        }
        item {
            Multiline3SlotButton(enabled = false)
        }
        item {
            Multiline3SlotButton(enabled = true, icon = { StandardIcon() })
        }
        item {
            Multiline3SlotButton(enabled = false, icon = { StandardIcon() })
        }
    }
}

@Composable
fun AvatarButtonDemo() {
    ScalingLazyColumn(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        item {
            ListHeader {
                Text("Label + Avatar")
            }
        }
        item {
            AvatarButton(enabled = true)
        }
        item {
            AvatarButton(enabled = false)
        }
        item {
            ListHeader {
                Text("Primary/Secondary + Avatar")
            }
        }
        item {
            Avatar3SlotButton(enabled = true)
        }
        item {
            Avatar3SlotButton(enabled = false)
        }
    }
}

@Composable
private fun AvatarButton(enabled: Boolean) =
    MultilineButton(
        enabled = enabled, icon = { AvatarIcon() }, label = { Text("Primary text") }
    )

@Composable
private fun Avatar3SlotButton(enabled: Boolean) =
    Multiline3SlotButton(
        enabled = enabled,
        icon = { AvatarIcon() },
        label = { Text("Primary text") },
        secondaryLabel = { Text("Secondary label") }
    )

@Composable
private fun MultilineButton(
    enabled: Boolean,
    icon: (@Composable BoxScope.() -> Unit)? = null,
    label: @Composable RowScope.() -> Unit = {
        Text(
            text = "Multiline label that include a lot of text and stretches to third line",
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )
    },
) {
    Button(
        onClick = { /* Do something */ },
        icon = icon,
        label = label,
        enabled = enabled
    )
}

@Composable
private fun Multiline3SlotButton(
    enabled: Boolean,
    icon: (@Composable BoxScope.() -> Unit)? = null,
    label: @Composable RowScope.() -> Unit = {
        Text(
            text = "Multiline label that include a lot of text and stretches to third line",
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )
    },
    secondaryLabel: @Composable RowScope.() -> Unit = {
        Text(
            text = "Secondary label over two lines",
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    },
) {
    Button(
        onClick = { /* Do something */ },
        icon = icon,
        label = label,
        secondaryLabel = secondaryLabel,
        enabled = enabled
    )
}

@Composable
private fun StandardIcon() {
    Icon(
        Icons.Filled.Favorite,
        contentDescription = "Favorite icon",
        modifier = Modifier.size(ButtonDefaults.IconSize)
    )
}

@Composable
private fun AvatarIcon() {
    Icon(
        Icons.Filled.AccountCircle,
        contentDescription = "Account",
        modifier = Modifier.size(ButtonDefaults.LargeIconSize)
    )
}