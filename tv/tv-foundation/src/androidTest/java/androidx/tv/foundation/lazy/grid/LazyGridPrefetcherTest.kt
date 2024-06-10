/*
 * Copyright 2022 The Android Open Source Project
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

@file:Suppress("DEPRECATION")

package androidx.tv.foundation.lazy.grid

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Remeasurement
import androidx.compose.ui.layout.RemeasurementModifier
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.filters.LargeTest
import androidx.tv.foundation.lazy.AutoTestFrameClock
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@LargeTest
@RunWith(Parameterized::class)
class LazyGridPrefetcherTest(orientation: Orientation) :
    BaseLazyGridTestWithOrientation(orientation) {

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun initParameters(): Array<Any> =
            arrayOf(
                Orientation.Vertical,
                Orientation.Horizontal,
            )
    }

    val itemsSizePx = 30
    val itemsSizeDp = with(rule.density) { itemsSizePx.toDp() }

    lateinit var state: TvLazyGridState

    @Test
    fun notPrefetchingForwardInitially() {
        composeGrid()

        rule.onNodeWithTag("4").assertDoesNotExist()
    }

    @Test
    fun notPrefetchingBackwardInitially() {
        composeGrid(firstItem = 4)

        rule.onNodeWithTag("0").assertDoesNotExist()
    }

    @Test
    fun prefetchingForwardAfterSmallScroll() {
        composeGrid()

        rule.runOnIdle { runBlocking { state.scrollBy(5f) } }

        waitForPrefetch(4)
        waitForPrefetch(5)

        rule.onNodeWithTag("4").assertExists()
        rule.onNodeWithTag("5").assertExists()
        rule.onNodeWithTag("6").assertDoesNotExist()
    }

    @Test
    fun prefetchingBackwardAfterSmallScroll() {
        composeGrid(firstItem = 4, itemOffset = 10)

        rule.runOnIdle { runBlocking { state.scrollBy(-5f) } }

        waitForPrefetch(2)
        waitForPrefetch(3)

        rule.onNodeWithTag("2").assertExists()
        rule.onNodeWithTag("3").assertExists()
        rule.onNodeWithTag("0").assertDoesNotExist()
    }

    @Test
    fun prefetchingForwardAndBackward() {
        composeGrid(firstItem = 2)

        rule.runOnIdle { runBlocking { state.scrollBy(5f) } }

        waitForPrefetch(6)
        waitForPrefetch(7)

        rule.onNodeWithTag("6").assertExists()
        rule.onNodeWithTag("7").assertExists()
        rule.onNodeWithTag("0").assertDoesNotExist()

        rule.runOnIdle {
            runBlocking {
                state.scrollBy(-2f)
                state.scrollBy(-1f)
            }
        }

        waitForPrefetch(0)
        waitForPrefetch(1)

        rule.onNodeWithTag("0").assertExists()
        rule.onNodeWithTag("1").assertExists()
        rule.onNodeWithTag("6").assertDoesNotExist()
    }

    @Test
    fun prefetchingForwardTwice() {
        composeGrid()

        rule.runOnIdle { runBlocking { state.scrollBy(5f) } }

        waitForPrefetch(4)

        rule.runOnIdle {
            runBlocking {
                state.scrollBy(itemsSizePx / 2f)
                state.scrollBy(itemsSizePx / 2f)
            }
        }

        waitForPrefetch(6)

        rule.onNodeWithTag("4").assertIsDisplayed()
        rule.onNodeWithTag("6").assertExists()
        rule.onNodeWithTag("8").assertDoesNotExist()
    }

    @Test
    fun prefetchingBackwardTwice() {
        composeGrid(firstItem = 8)

        rule.runOnIdle { runBlocking { state.scrollBy(-5f) } }

        waitForPrefetch(4)

        rule.runOnIdle {
            runBlocking {
                state.scrollBy(-itemsSizePx / 2f)
                state.scrollBy(-itemsSizePx / 2f)
            }
        }

        waitForPrefetch(2)

        rule.onNodeWithTag("4").assertIsDisplayed()
        rule.onNodeWithTag("6").assertIsDisplayed()
        rule.onNodeWithTag("2").assertExists()
        rule.onNodeWithTag("0").assertDoesNotExist()
    }

    @Test
    fun prefetchingForwardAndBackwardReverseLayout() {
        composeGrid(firstItem = 2, reverseLayout = true)

        rule.runOnIdle { runBlocking { state.scrollBy(5f) } }

        waitForPrefetch(6)
        waitForPrefetch(7)

        rule.onNodeWithTag("6").assertExists()
        rule.onNodeWithTag("7").assertExists()
        rule.onNodeWithTag("0").assertDoesNotExist()
        rule.onNodeWithTag("1").assertDoesNotExist()

        rule.runOnIdle {
            runBlocking {
                state.scrollBy(-2f)
                state.scrollBy(-1f)
            }
        }

        waitForPrefetch(0)
        waitForPrefetch(1)

        rule.onNodeWithTag("0").assertExists()
        rule.onNodeWithTag("1").assertExists()
        rule.onNodeWithTag("6").assertDoesNotExist()
        rule.onNodeWithTag("7").assertDoesNotExist()
    }

    @Test
    fun prefetchingForwardAndBackwardWithContentPadding() {
        val halfItemSize = itemsSizeDp / 2f
        composeGrid(
            firstItem = 4,
            itemOffset = 5,
            contentPadding = PaddingValues(mainAxis = halfItemSize)
        )

        rule.onNodeWithTag("2").assertIsDisplayed()
        rule.onNodeWithTag("4").assertIsDisplayed()
        rule.onNodeWithTag("6").assertIsDisplayed()
        rule.onNodeWithTag("0").assertDoesNotExist()
        rule.onNodeWithTag("8").assertDoesNotExist()

        rule.runOnIdle { runBlocking { state.scrollBy(5f) } }

        waitForPrefetch(6)

        rule.onNodeWithTag("8").assertExists()
        rule.onNodeWithTag("0").assertDoesNotExist()

        rule.runOnIdle { runBlocking { state.scrollBy(-2f) } }

        waitForPrefetch(0)

        rule.onNodeWithTag("0").assertExists()
    }

    @Test
    fun disposingWhilePrefetchingScheduled() {
        var emit = true
        lateinit var remeasure: Remeasurement
        rule.setContent {
            SubcomposeLayout(
                modifier =
                    object : RemeasurementModifier {
                        override fun onRemeasurementAvailable(remeasurement: Remeasurement) {
                            remeasure = remeasurement
                        }
                    }
            ) { constraints ->
                val placeable =
                    if (emit) {
                        subcompose(Unit) {
                                state = rememberTvLazyGridState()
                                LazyGrid(
                                    2,
                                    Modifier.mainAxisSize(itemsSizeDp * 1.5f),
                                    state,
                                ) {
                                    items(1000) { Spacer(Modifier.mainAxisSize(itemsSizeDp)) }
                                }
                            }
                            .first()
                            .measure(constraints)
                    } else {
                        null
                    }
                layout(constraints.maxWidth, constraints.maxHeight) { placeable?.place(0, 0) }
            }
        }

        rule.runOnIdle {
            // this will schedule the prefetching
            runBlocking(AutoTestFrameClock()) { state.scrollBy(itemsSizePx.toFloat()) }
            // then we synchronously dispose LazyColumn
            emit = false
            remeasure.forceRemeasure()
        }

        rule.runOnIdle {}
    }

    @Test
    fun scrollingByListSizeCancelsPreviousPrefetch() {
        composeGrid()

        // now we have items 0-3 visible
        rule.runOnIdle {
            runBlocking(AutoTestFrameClock()) {
                // this will move the viewport so items 2-5 are visible
                // and schedule a prefetching for 6-7
                state.scrollBy(itemsSizePx.toFloat())

                // move viewport by screen size to items 8-11, so item 6 is just behind
                // the first visible item
                state.scrollBy(itemsSizePx * 3f)

                // move scroll further to items 10-13, so item 6 is reused
                state.scrollBy(itemsSizePx.toFloat())
            }
        }

        waitForPrefetch(13)

        rule.runOnIdle {
            runBlocking(AutoTestFrameClock()) {
                // scroll again to ensure item 6 was dropped
                state.scrollBy(itemsSizePx * 100f)
            }
        }

        rule.runOnIdle { assertThat(activeNodes).doesNotContain(6) }
    }

    private fun waitForPrefetch(index: Int) {
        rule.waitUntil { activeNodes.contains(index) && activeMeasuredNodes.contains(index) }
    }

    private val activeNodes = mutableSetOf<Int>()
    private val activeMeasuredNodes = mutableSetOf<Int>()

    private fun composeGrid(
        firstItem: Int = 0,
        itemOffset: Int = 0,
        reverseLayout: Boolean = false,
        contentPadding: PaddingValues = PaddingValues(0.dp)
    ) {
        rule.setContent {
            state =
                rememberTvLazyGridState(
                    initialFirstVisibleItemIndex = firstItem,
                    initialFirstVisibleItemScrollOffset = itemOffset
                )
            LazyGrid(
                2,
                Modifier.mainAxisSize(itemsSizeDp * 1.5f),
                state,
                reverseLayout = reverseLayout,
                contentPadding = contentPadding
            ) {
                items(100) {
                    DisposableEffect(it) {
                        activeNodes.add(it)
                        onDispose {
                            activeNodes.remove(it)
                            activeMeasuredNodes.remove(it)
                        }
                    }
                    Spacer(
                        Modifier.mainAxisSize(itemsSizeDp).testTag("$it").layout {
                            measurable,
                            constraints ->
                            val placeable = measurable.measure(constraints)
                            activeMeasuredNodes.add(it)
                            layout(placeable.width, placeable.height) { placeable.place(0, 0) }
                        }
                    )
                }
            }
        }
    }
}
