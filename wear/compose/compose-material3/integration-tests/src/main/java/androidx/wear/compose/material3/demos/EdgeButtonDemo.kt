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

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberOverscrollEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.ScalingLazyListScope
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.integration.demos.common.AdaptiveScreen
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.Card
import androidx.wear.compose.material3.EdgeButton
import androidx.wear.compose.material3.EdgeButtonDefaults
import androidx.wear.compose.material3.EdgeButtonSize
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.RadioButton
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.TextButton
import androidx.wear.compose.material3.TextButtonDefaults
import androidx.wear.compose.material3.samples.icons.CheckIcon

fun listOfLabels(): List<String> {
    return listOf(
        "Hi",
        "Hello World",
        "Hello world again?",
        "More content as we add stuff",
        "I don't know if this will fit now, testing",
        "Really long text that it's going to take multiple lines",
        "And now we are really pushing it because the screen is really small",
    )
}

@Composable
fun EdgeButtonBelowLazyColumnDemo(reverseLayout: Boolean) {
    val labels = listOfLabels()
    val selectedLabel = remember { mutableIntStateOf(0) }
    AdaptiveScreen {
        val state = rememberLazyListState()
        ScreenScaffold(
            scrollState = state,
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 20.dp),
            edgeButton = {
                EdgeButton(
                    modifier =
                        // In case user starts scrolling from the EdgeButton.
                        Modifier.scrollable(
                            state,
                            orientation = Orientation.Vertical,
                            reverseDirection = !reverseLayout,
                            // An overscroll effect should be applied to the EdgeButton for proper
                            // scrolling behavior.
                            overscrollEffect = rememberOverscrollEffect(),
                        ),
                    onClick = {},
                    buttonSize = EdgeButtonSize.Large,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
                ) {
                    Text(labels[selectedLabel.intValue], color = Color.White)
                }
            },
        ) { contentPadding ->
            LazyColumn(
                state = state,
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                contentPadding = contentPadding,
                horizontalAlignment = Alignment.CenterHorizontally,
                reverseLayout = reverseLayout,
            ) {
                items(labels.size) {
                    Card(
                        onClick = { selectedLabel.intValue = it },
                        modifier = Modifier.fillMaxWidth(0.9f),
                    ) {
                        Text(labels[it])
                    }
                }
            }
        }
    }
}

@Composable
fun EdgeButtonBelowScalingLazyColumnDemo(reverseLayout: Boolean) {
    val labels = listOfLabels()
    val selectedLabel = remember { mutableIntStateOf(0) }

    AdaptiveScreen {
        val state = rememberScalingLazyListState()
        ScreenScaffold(
            scrollState = state,
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 20.dp),
            edgeButton = {
                EdgeButton(
                    modifier =
                        // In case user starts scrolling from the EdgeButton.
                        Modifier.scrollable(
                            state,
                            orientation = Orientation.Vertical,
                            reverseDirection = !reverseLayout,
                            // An overscroll effect should be applied to the EdgeButton for proper
                            // scrolling behavior.
                            overscrollEffect = rememberOverscrollEffect(),
                        ),
                    onClick = {},
                    buttonSize = EdgeButtonSize.Large,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
                ) {
                    Text(labels[selectedLabel.intValue], color = Color.White)
                }
            },
        ) { contentPadding ->
            ScalingLazyColumn(
                state = state,
                modifier = Modifier.fillMaxSize(),
                autoCentering = null,
                contentPadding = contentPadding,
                horizontalAlignment = Alignment.CenterHorizontally,
                reverseLayout = reverseLayout,
            ) {
                items(labels.size) {
                    Card(
                        onClick = { selectedLabel.intValue = it },
                        modifier = Modifier.fillMaxWidth(0.9f),
                    ) {
                        Text(labels[it])
                    }
                }
            }
        }
    }
}

@Composable
fun EdgeButtonBelowTransformingLazyColumnDemo() {
    val labels = listOfLabels()
    val selectedLabel = remember { mutableIntStateOf(0) }
    AdaptiveScreen {
        val state = rememberTransformingLazyColumnState()
        ScreenScaffold(
            scrollState = state,
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 20.dp),
            edgeButton = {
                EdgeButton(
                    modifier =
                        // In case user starts scrolling from the EdgeButton.
                        Modifier.scrollable(
                            state,
                            orientation = Orientation.Vertical,
                            reverseDirection = true,
                            // An overscroll effect should be applied to the EdgeButton for proper
                            // scrolling behavior.
                            overscrollEffect = rememberOverscrollEffect(),
                        ),
                    onClick = {},
                    buttonSize = EdgeButtonSize.Large,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
                ) {
                    Text(labels[selectedLabel.intValue], color = Color.White)
                }
            },
        ) { contentPadding ->
            TransformingLazyColumn(
                state = state,
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                contentPadding = contentPadding,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                items(labels.size) {
                    Card(
                        onClick = { selectedLabel.intValue = it },
                        modifier = Modifier.fillMaxWidth(0.9f),
                    ) {
                        Text(labels[it])
                    }
                }
            }
        }
    }
}

@Suppress("PrimitiveInCollection")
@Composable
fun EdgeButtonMultiDemo() {
    val sizes =
        listOf(
            EdgeButtonSize.ExtraSmall,
            EdgeButtonSize.Small,
            EdgeButtonSize.Medium,
            EdgeButtonSize.Large,
        )
    val sizeNames = listOf("XS", "S", "M", "L")
    var size by remember { mutableIntStateOf(0) }

    val colors =
        listOf(
            ButtonDefaults.buttonColors(),
            ButtonDefaults.filledVariantButtonColors(),
            ButtonDefaults.filledTonalButtonColors(),
            ButtonDefaults.outlinedButtonColors(),
            ButtonDefaults.buttonColors(),
        )
    val colorNames = listOf("F", "FV", "FT", "O", "D")
    var color by remember { mutableIntStateOf(0) }

    AdaptiveScreen {
        Column(
            Modifier.align(Alignment.TopCenter).fillMaxSize().padding(top = 0.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row { Spacer(modifier = Modifier.height(10.dp)) }
                Row {
                    ListHeader {
                        Text(
                            "Sizes",
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center,
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    repeat(sizeNames.size) {
                        TextButton(
                            onClick = { size = it },
                            modifier = Modifier.size(TextButtonDefaults.SmallButtonSize),
                        ) {
                            Text(sizeNames[it])
                        }
                    }
                }
                Row {
                    ListHeader {
                        Text(
                            "Colors",
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center,
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    repeat(colorNames.size) {
                        TextButton(
                            onClick = { color = it },
                            modifier = Modifier.size(TextButtonDefaults.SmallButtonSize),
                        ) {
                            Text(colorNames[it])
                        }
                    }
                }
            }
            EdgeButton(
                onClick = {},
                enabled = colorNames[color] != "D",
                buttonSize = sizes[size],
                colors = colors[color],
                border =
                    if (colorNames[color] == "O")
                        ButtonDefaults.outlinedButtonBorder(enabled = true)
                    else null,
            ) {
                CheckIcon(modifier = Modifier.size(EdgeButtonDefaults.iconSizeFor(sizes[size])))
            }
        }
    }
}

@Composable
fun EdgeButtonConfigurableDemo() {
    val sizes =
        listOf(
            "Extra Small" to EdgeButtonSize.ExtraSmall,
            "Small" to EdgeButtonSize.Small,
            "Medium" to EdgeButtonSize.Medium,
            "Large" to EdgeButtonSize.Large,
        )
    var selectedSize by remember { mutableIntStateOf(0) }
    val colors =
        listOf(
            "Filled" to ButtonDefaults.buttonColors(),
            "Filled Variant" to ButtonDefaults.filledVariantButtonColors(),
            "Filled Tonal" to ButtonDefaults.filledTonalButtonColors(),
            "Outlined" to ButtonDefaults.outlinedButtonColors(),
            "Disabled" to ButtonDefaults.buttonColors(),
        )
    var selectedColor by remember { mutableIntStateOf(0) }
    val types = listOf("Icon only" to 0, "Small Text" to 1, "Long Text" to 2)
    var selectedType by remember { mutableIntStateOf(0) }
    var reverseLayout by remember { mutableStateOf(false) }
    AdaptiveScreen {
        val state = rememberScalingLazyListState()
        ScreenScaffold(
            scrollState = state,
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 20.dp),
            edgeButton = {
                EdgeButton(
                    modifier =
                        // In case user starts scrolling from the EdgeButton.
                        Modifier.scrollable(
                            state,
                            orientation = Orientation.Vertical,
                            reverseDirection = !reverseLayout,
                            // An overscroll effect should be applied to the EdgeButton for proper
                            // scrolling behavior.
                            overscrollEffect = rememberOverscrollEffect(),
                        ),
                    onClick = {},
                    buttonSize = sizes[selectedSize].second,
                    colors = colors[selectedColor].second,
                    border =
                        if (colors[selectedColor].first == "Outlined")
                            ButtonDefaults.outlinedButtonBorder(true)
                        else null,
                    enabled = colors[selectedColor].first != "Disabled",
                ) {
                    when (selectedType) {
                        0 -> {
                            val iconSize =
                                EdgeButtonDefaults.iconSizeFor(sizes[selectedSize].second)
                            // Remove extra spacing around the icon so it integrates better into the
                            // scroll.
                            CheckIcon(
                                modifier =
                                    Modifier.size(iconSize * 2 / 3)
                                        .wrapContentSize(unbounded = true)
                                        .size(iconSize)
                            )
                        }
                        1 -> Text("Ok")
                        else -> Text("Long Text to show max lines and overflow")
                    }
                }
            },
        ) { contentPadding ->
            ScalingLazyColumn(
                state = state,
                modifier = Modifier.fillMaxSize(),
                autoCentering = null,
                contentPadding = contentPadding,
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
                reverseLayout = reverseLayout,
            ) {
                selection(
                    listOf("Normal" to false, "Reversed" to true),
                    selected = { reverseLayout == (it == 1) },
                    onSelect = { reverseLayout = it == 1 },
                    label = "Layout",
                    reverseLayout,
                )
                selection(
                    sizes,
                    selected = { selectedSize == it },
                    onSelect = { selectedSize = it },
                    label = "Size",
                    reverseLayout,
                )
                selection(
                    colors,
                    selected = { selectedColor == it },
                    onSelect = { selectedColor = it },
                    label = "Color",
                    reverseLayout,
                )
                selection(
                    types,
                    selected = { selectedType == it },
                    onSelect = { selectedType = it },
                    label = "Content",
                    reverseLayout,
                )
            }
        }
    }
}

private fun <T> ScalingLazyListScope.selection(
    items: List<Pair<String, T>>,
    selected: (Int) -> Boolean,
    onSelect: (Int) -> Unit,
    label: String,
    reverseLayout: Boolean,
) {
    if (!reverseLayout) item { Text(label) }
    items(items.size) { ix ->
        RadioButton(
            selected = selected(ix),
            onSelect = { onSelect(ix) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                items[ix].first,
                textAlign = TextAlign.Center,
                modifier = Modifier.align(Alignment.CenterVertically),
            )
        }
    }
    if (reverseLayout) item { Text(label) }
}
