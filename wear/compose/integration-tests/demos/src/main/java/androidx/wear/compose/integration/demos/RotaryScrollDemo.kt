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

package androidx.wear.compose.integration.demos

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.overscroll
import androidx.compose.foundation.rememberOverscrollEffect
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.HierarchicalFocusCoordinator
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.rememberActiveFocusRequester
import androidx.wear.compose.foundation.rotary.RotaryScrollableDefaults
import androidx.wear.compose.foundation.rotary.rotaryScrollable
import androidx.wear.compose.material.Text

@Composable
public fun NestedScrollPagerDemo() {
    // Creates a 3-pager vertical pager with nested scroll
    val state = androidx.compose.foundation.pager.rememberPagerState(initialPage = 1) { 3 }
    androidx.compose.foundation.pager.VerticalPager(
        state = state,
        modifier = Modifier.fillMaxSize(),
    ) { page ->
        HierarchicalFocusCoordinator(requiresFocus = { page == state.currentPage }) {
            when (page) {
                0 -> {
                    val scrollableState = rememberScrollState()
                    Box(
                        Modifier.fillMaxSize()
                            .rotaryScrollable(
                                RotaryScrollableDefaults.behavior(scrollableState),
                                rememberActiveFocusRequester()
                            )
                            .verticalScroll(scrollableState)
                            .padding(30.dp),
                        Alignment.Center
                    ) {
                        Text("Scroll down to snap to the next page", textAlign = TextAlign.Center)
                    }
                }
                1 -> {
                    val scrollableState = rememberScrollState()
                    Column(
                        Modifier.fillMaxSize()
                            .rotaryScrollable(
                                RotaryScrollableDefaults.behavior(scrollableState),
                                rememberActiveFocusRequester()
                            )
                            .verticalScroll(scrollableState)
                            .background(Color.Gray)
                            .padding(30.dp)
                    ) {
                        Box(Modifier.fillMaxSize()) {
                            Text(
                                text = "Scroll up to snap to the previous page",
                                textAlign = TextAlign.Center
                            )
                        }
                        repeat(20) { innerIndex ->
                            Box(Modifier.height(38.dp).fillMaxWidth(), Alignment.Center) {
                                Text(
                                    text = "Item $innerIndex",
                                )
                            }
                        }
                        Box(Modifier.fillMaxSize(), Alignment.Center) {
                            Text(
                                text = "Scroll down to snap to the next page",
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
                2 -> {
                    val scrollableState = rememberScrollState()
                    Box(
                        Modifier.fillMaxSize()
                            .rotaryScrollable(
                                RotaryScrollableDefaults.behavior(scrollableState),
                                rememberActiveFocusRequester()
                            )
                            .verticalScroll(scrollableState)
                            .padding(30.dp),
                        Alignment.Center
                    ) {
                        Text("Scroll up to snap to the previous page", textAlign = TextAlign.Center)
                    }
                }
            }
        }
    }
}

@Composable
public fun NestedScrollLazyColumnDemo(reverseLayout: Boolean) {
    // Creates a LazyColumn with nested scroll
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        reverseLayout = reverseLayout,
        contentPadding = PaddingValues(vertical = 50.dp)
    ) {
        item { TopText() }
        item {
            HierarchicalFocusCoordinator(requiresFocus = { true }) {
                val state = rememberLazyListState()
                val overscrollEffect = rememberOverscrollEffect()

                LazyColumn(
                    modifier =
                        Modifier.rotaryScrollable(
                                RotaryScrollableDefaults.behavior(state),
                                overscrollEffect = overscrollEffect,
                                focusRequester = rememberActiveFocusRequester(),
                                reverseDirection = reverseLayout
                            )
                            .overscroll(overscrollEffect)
                            .size(150.dp)
                            .background(Color.Yellow)
                            .padding(PaddingValues(30.dp)),
                    state = state,
                    reverseLayout = reverseLayout
                ) {
                    items(20) { index -> ItemText(index) }
                }
            }
        }
        item { BottomText() }
    }
}

@Composable
public fun NestedScrollTLCDemo() {
    // Creates a TLC with nested scroll
    TransformingLazyColumn(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        contentPadding = PaddingValues(vertical = 50.dp),
        // Disable rotary scroll in the parent as rotary is going to be handled by the child
        rotaryScrollableBehavior = null
    ) {
        item { TopText() }
        item {
            HierarchicalFocusCoordinator(requiresFocus = { true }) {
                TransformingLazyColumn(
                    Modifier.size(150.dp).background(Color.Yellow).padding(PaddingValues(30.dp)),
                ) {
                    items(20) { index -> ItemText(index) }
                }
            }
        }
        item { BottomText() }
    }
}

@Composable
public fun NestedScrollSLCDemo() {
    // Creates a SLC with nested scroll
    ScalingLazyColumn(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        contentPadding = PaddingValues(vertical = 50.dp),
        // Disable rotary scroll in the parent as rotary is going to be handled by the child
        rotaryScrollableBehavior = null
    ) {
        item { TopText() }
        item {
            HierarchicalFocusCoordinator(requiresFocus = { true }) {
                ScalingLazyColumn(
                    Modifier.size(150.dp).background(Color.Yellow).padding(PaddingValues(30.dp))
                ) {
                    items(20) { index -> ItemText(index) }
                }
            }
        }
        item { BottomText() }
    }
}

@Composable
private fun ItemText(index: Int) {
    Box(
        Modifier.height(38.dp).fillMaxWidth().background(Color.Magenta),
        contentAlignment = Alignment.Center
    ) {
        BasicText(
            text = "Item $index",
            modifier = Modifier,
        )
    }
}

@Composable
private fun TopText() {
    Box(Modifier.fillMaxWidth().background(Color.Magenta), contentAlignment = Alignment.Center) {
        Text(
            modifier = Modifier.fillMaxWidth().height(40.dp),
            textAlign = TextAlign.Center,
            text = "This is a top text",
        )
    }
}

@Composable
private fun BottomText() {
    Box(Modifier.fillMaxWidth().background(Color.Magenta), contentAlignment = Alignment.Center) {
        Text(
            modifier = Modifier.fillMaxWidth().height(40.dp),
            textAlign = TextAlign.Center,
            text = "This is a bottom text",
        )
    }
}
