/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.compose.foundation.demos

import androidx.compose.foundation.Interaction
import androidx.compose.foundation.InteractionState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayout
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.preferredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyColumnFor
import androidx.compose.foundation.lazy.LazyColumnForIndexed
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.LazyRowFor
import androidx.compose.foundation.lazy.LazyRowForIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.integration.demos.common.ComposableDemo
import androidx.compose.material.AmbientContentColor
import androidx.compose.material.AmbientTextStyle
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Providers
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.AmbientDensity
import androidx.compose.ui.platform.AmbientLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.paging.compose.demos.PagingDemos
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlin.random.Random

val LazyListDemos = listOf(
    ComposableDemo("Simple column") { LazyColumnDemo() },
    ComposableDemo("Add/remove items") { ListAddRemoveItemsDemo() },
    ComposableDemo("Hoisted state") { ListHoistedStateDemo() },
    ComposableDemo("Horizontal list") { LazyRowItemsDemo() },
    ComposableDemo("List with indexes") { ListWithIndexSample() },
    ComposableDemo("Pager-like list") { PagerLikeDemo() },
    ComposableDemo("Rtl list") { RtlListDemo() },
    ComposableDemo("LazyColumn DSL") { LazyColumnScope() },
    ComposableDemo("LazyRow DSL") { LazyRowScope() },
    PagingDemos
)

@Composable
private fun LazyColumnDemo() {
    LazyColumnFor(
        items = listOf(
            "Hello,", "World:", "It works!", "",
            "this one is really long and spans a few lines for scrolling purposes",
            "these", "are", "offscreen"
        ) + (1..100).map { "$it" }
    ) {
        Text(text = it, fontSize = 80.sp)

        if (it.contains("works")) {
            Text("You can even emit multiple components per item.")
        }
    }
}

@Composable
private fun ListAddRemoveItemsDemo() {
    var numItems by remember { mutableStateOf(0) }
    var offset by remember { mutableStateOf(0) }
    Column {
        Row {
            val buttonModifier = Modifier.padding(8.dp)
            Button(modifier = buttonModifier, onClick = { numItems++ }) { Text("Add") }
            Button(modifier = buttonModifier, onClick = { numItems-- }) { Text("Remove") }
            Button(modifier = buttonModifier, onClick = { offset++ }) { Text("Offset") }
        }
        LazyColumnFor(
            (1..numItems).map { it + offset }.toList(),
            Modifier.fillMaxWidth()
        ) {
            Text("$it", style = AmbientTextStyle.current.copy(fontSize = 40.sp))
        }
    }
}

@OptIn(ExperimentalLayout::class)
@Composable
private fun ListHoistedStateDemo() {
    val interactionState = remember { InteractionState() }
    val state = rememberLazyListState(interactionState = interactionState)
    var lastScrollDescription: String by remember { mutableStateOf("") }
    Column {
        FlowRow {
            val buttonModifier = Modifier.padding(8.dp)
            val density = AmbientDensity.current
            val coroutineScope = rememberCoroutineScope()
            Button(
                modifier = buttonModifier,
                onClick = {
                    coroutineScope.launch {
                        state.snapToItemIndex(state.firstVisibleItemIndex - 1)
                    }
                }
            ) {
                Text("Previous")
            }
            Button(
                modifier = buttonModifier,
                onClick = {
                    coroutineScope.launch {
                        state.snapToItemIndex(state.firstVisibleItemIndex + 1)
                    }
                }
            ) {
                Text("Next")
            }
            Button(
                modifier = buttonModifier,
                onClick = {
                    with(density) {
                        coroutineScope.launch {
                            val requestedScroll = 3000.dp.toPx()
                            lastScrollDescription = try {
                                val actualScroll = state.smoothScrollBy(requestedScroll)
                                "$actualScroll/$requestedScroll px"
                            } catch (_: CancellationException) {
                                "Interrupted!"
                            }
                        }
                    }
                }
            ) {
                Text("Scroll")
            }
        }
        Column {
            Text(
                "First item: ${state.firstVisibleItemIndex}, Last scroll: $lastScrollDescription",
                fontSize = 20.sp
            )
            Text(
                "Dragging: ${interactionState.contains(Interaction.Dragged)}, " +
                    "Flinging: ${state.isAnimationRunning}",
                fontSize = 20.sp
            )
        }
        LazyColumnFor(
            (0..1000).toList(),
            Modifier.fillMaxWidth(),
            state = state
        ) {
            Text("$it", style = AmbientTextStyle.current.copy(fontSize = 40.sp))
        }
    }
}

@Composable
fun Button(modifier: Modifier, onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(
        modifier
            .clickable(onClick = onClick)
            .background(Color(0xFF6200EE), RoundedCornerShape(4.dp))
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Providers(AmbientContentColor provides Color.White, content = content)
    }
}

@Composable
private fun LazyRowItemsDemo() {
    LazyRowFor(items = (1..1000).toList()) {
        Square(it)
    }
}

@Composable
private fun Square(index: Int) {
    val width = remember { Random.nextInt(50, 150).dp }
    Box(
        Modifier.preferredWidth(width).fillMaxHeight().background(colors[index % colors.size]),
        contentAlignment = Alignment.Center
    ) {
        Text(index.toString())
    }
}

@Composable
private fun ListWithIndexSample() {
    val friends = listOf("Alex", "John", "Danny", "Sam")
    Column {
        LazyRowForIndexed(friends, Modifier.fillMaxWidth()) { index, friend ->
            Text("$friend at index $index", Modifier.padding(16.dp))
        }
        LazyColumnForIndexed(friends, Modifier.fillMaxWidth()) { index, friend ->
            Text("$friend at index $index", Modifier.padding(16.dp))
        }
    }
}

@Composable
private fun RtlListDemo() {
    Providers(AmbientLayoutDirection provides LayoutDirection.Rtl) {
        LazyRowForIndexed((0..100).toList(), Modifier.fillMaxWidth()) { index, item ->
            Text(
                "$item",
                Modifier
                    .size(100.dp)
                    .background(if (index % 2 == 0) Color.LightGray else Color.Transparent)
                    .padding(16.dp)
            )
        }
    }
}

@Composable
private fun PagerLikeDemo() {
    val pages = listOf(Color.LightGray, Color.White, Color.DarkGray)
    LazyRowFor(pages) {
        Spacer(Modifier.fillParentMaxSize().background(it))
    }
}

private val colors = listOf(
    Color(0xFFffd7d7.toInt()),
    Color(0xFFffe9d6.toInt()),
    Color(0xFFfffbd0.toInt()),
    Color(0xFFe3ffd9.toInt()),
    Color(0xFFd0fff8.toInt())
)

@Composable
private fun LazyColumnScope() {
    LazyColumn {
        items((1..10).toList()) {
            Text("$it", fontSize = 40.sp)
        }

        item {
            Text("Single item", fontSize = 40.sp)
        }

        val items = listOf("A", "B", "C")
        itemsIndexed(items) { index, item ->
            Text("Item $item has index $index", fontSize = 40.sp)
        }
    }
}

@Composable
private fun LazyRowScope() {
    LazyRow {
        items((1..10).toList()) {
            Text("$it", fontSize = 40.sp)
        }

        item {
            Text("Single item", fontSize = 40.sp)
        }

        val items = listOf(Color.Cyan, Color.Blue, Color.Magenta)
        itemsIndexed(items) { index, item ->
            Box(
                modifier = Modifier.background(item).size(40.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("$index", fontSize = 30.sp)
            }
        }
    }
}