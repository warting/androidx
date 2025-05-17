/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.compose.foundation.lazy.list

import androidx.compose.foundation.AutoTestFrameClock
import androidx.compose.foundation.ComposeFoundationFlags.isAutomaticNestedPrefetchEnabled
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.lazy.LazyListLayoutInfo
import androidx.compose.foundation.lazy.LazyListPrefetchScope
import androidx.compose.foundation.lazy.LazyListPrefetchStrategy
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.layout.NestedPrefetchScope
import androidx.compose.foundation.lazy.layout.PrefetchScheduler
import androidx.compose.foundation.lazy.layout.TestPrefetchScheduler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Constraints
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@OptIn(ExperimentalFoundationApi::class)
@LargeTest
@RunWith(Parameterized::class)
class LazyListNestedPrefetchingTest(val config: Config) :
    BaseLazyListTestWithOrientation(config.orientation) {

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun initParameters(): Array<Any> =
            arrayOf(Config(Orientation.Vertical), Config(Orientation.Horizontal))

        class Config(val orientation: Orientation) {
            override fun toString() = "orientation=$orientation"
        }

        @JvmStatic
        @BeforeClass
        fun setUp() {
            isAutomaticNestedPrefetchEnabled = false
        }
    }

    sealed interface Action {
        data class Compose(val index: Int, val nestedIndex: Int? = null) : Action

        data class Measure(val index: Int, val nestedIndex: Int? = null) : Action
    }

    private val itemsSizePx = 30
    private val itemsSizeDp = with(rule.density) { itemsSizePx.toDp() }
    private val activeNodes = mutableSetOf<String>()
    private val scheduler = TestPrefetchScheduler()

    @OptIn(ExperimentalFoundationApi::class)
    private val strategy =
        object : LazyListPrefetchStrategy by LazyListPrefetchStrategy() {
            override val prefetchScheduler: PrefetchScheduler = scheduler
        }

    @OptIn(ExperimentalFoundationApi::class)
    private fun createState(): LazyListState = LazyListState(prefetchStrategy = strategy)

    @Test
    fun nestedPrefetchingForwardAfterSmallScroll() {
        val state = createState()
        composeList(state)

        val prefetchIndex = 2
        val actions = trackingActions {
            rule.runOnIdle { runBlocking { state.scrollBy(5f) } }

            waitForPrefetch()
        }

        // We want to make sure nested children were precomposed before the parent was premeasured
        // (which would force them all to compose in a single block of work in premeasure)
        assertThat(actions)
            .containsExactly(
                Action.Compose(prefetchIndex),
                Action.Compose(prefetchIndex, 0),
                Action.Compose(prefetchIndex, 1),
                Action.Measure(prefetchIndex),
                Action.Measure(prefetchIndex, 0),
                Action.Measure(prefetchIndex, 1),
            )
            .inOrder()

        rule.onNodeWithTag(tagFor(prefetchIndex)).assertExists()
        rule.onNodeWithTag(tagFor(2, 0)).assertExists()
        rule.onNodeWithTag(tagFor(2, 1)).assertExists()
        rule.onNodeWithTag(tagFor(2, 2)).assertDoesNotExist()
    }

    @Test
    fun cancelingPrefetchCancelsItsNestedPrefetches() {
        val state = createState()
        composeList(state)

        rule.runOnIdle {
            runBlocking {
                // this will move the viewport so items 1-2 are visible
                // and schedule a prefetching for 3
                state.scrollBy(itemsSizePx.toFloat())
            }
        }

        waitForPrefetch()

        rule.runOnIdle {
            assertThat(activeNodes).contains(tagFor(3))
            assertThat(activeNodes).contains(tagFor(3, 0))
            assertThat(activeNodes).contains(tagFor(3, 1))
        }

        rule.runOnIdle {
            runBlocking(AutoTestFrameClock()) {
                // move viewport by screen size to items 4-5, so item 3 is just behind
                // the first visible item
                state.scrollBy(itemsSizePx * 3f)

                // move scroll further to items 5-6, so item 3 is reused
                state.scrollBy(itemsSizePx.toFloat())
            }
        }

        waitForPrefetch()

        rule.runOnIdle {
            runBlocking(AutoTestFrameClock()) {
                // scroll again to ensure item 3 was dropped
                state.scrollBy(itemsSizePx * 100f)
            }
        }

        rule.runOnIdle {
            assertThat(activeNodes).doesNotContain(tagFor(3))
            assertThat(activeNodes).doesNotContain(tagFor(3, 0))
            assertThat(activeNodes).doesNotContain(tagFor(3, 1))
        }
    }

    @OptIn(ExperimentalFoundationApi::class)
    @Test
    fun overridingNestedPrefetchCountIsRespected() {
        val state = createState()
        composeList(
            state,
            createNestedLazyListState = {
                LazyListState(prefetchStrategy = LazyListPrefetchStrategy(1))
            },
        )

        val prefetchIndex = 2
        val actions = trackingActions {
            rule.runOnIdle { runBlocking { state.scrollBy(5f) } }

            waitForPrefetch()
        }

        // Since the nested prefetch count on the strategy is 1, we only expect index 0 to be
        // precomposed before measure
        assertThat(actions)
            .containsExactly(
                Action.Compose(prefetchIndex),
                Action.Compose(prefetchIndex, 0),
                Action.Measure(prefetchIndex),
                Action.Measure(prefetchIndex, 0),
                Action.Compose(prefetchIndex, 1),
                Action.Measure(prefetchIndex, 1),
            )
            .inOrder()
    }

    @OptIn(ExperimentalFoundationApi::class)
    @Test
    fun nestedPrefetchIsMeasuredWithProvidedConstraints() {
        val nestedConstraints =
            Constraints(minWidth = 20, minHeight = 20, maxWidth = 20, maxHeight = 20)
        val state = createState()
        composeList(
            state,
            createNestedLazyListState = {
                LazyListState(
                    prefetchStrategy = NestedPrefetchWithConstraintsStrategy(nestedConstraints)
                )
            },
        )

        val prefetchIndex = 2
        val actions = trackingActions {
            rule.runOnIdle { runBlocking { state.scrollBy(5f) } }

            waitForPrefetch()
        }

        assertThat(actions)
            .containsExactly(
                Action.Compose(prefetchIndex),
                Action.Compose(prefetchIndex, 0),
                Action.Measure(prefetchIndex, 0),
                Action.Compose(prefetchIndex, 1),
                Action.Measure(prefetchIndex, 1),
                Action.Measure(prefetchIndex),
                // Extra measure calls here since we didn't actually provide the right Constraints
                Action.Measure(prefetchIndex, 0),
                Action.Measure(prefetchIndex, 1),
            )
            .inOrder()
    }

    @Test
    fun nestedPrefetchStartsFromFirstVisibleItemIndex() {
        val state = createState()
        composeList(state, createNestedLazyListState = { LazyListState(firstVisibleItemIndex = 5) })

        val prefetchIndex = 2
        val actions = trackingActions {
            rule.runOnIdle { runBlocking { state.scrollBy(5f) } }

            waitForPrefetch()
        }

        assertThat(actions)
            .containsExactly(
                Action.Compose(prefetchIndex),
                Action.Compose(prefetchIndex, 5),
                Action.Compose(prefetchIndex, 6),
                Action.Measure(prefetchIndex),
                Action.Measure(prefetchIndex, 5),
                Action.Measure(prefetchIndex, 6),
            )
            .inOrder()
    }

    @Test
    fun automaticNestedPrefetchingBasedOnNumberOfVisibleItems() {
        isAutomaticNestedPrefetchEnabled = true

        val state = createState() // using the default strategy
        composeList(
            state,
            createNestedLazyListState = {
                LazyListState(
                    prefetchStrategy = LazyListPrefetchStrategy(1),
                    firstVisibleItemIndex = 5,
                )
            },
        )

        val prefetchIndex = 2
        val actions = trackingActions {
            rule.runOnIdle { runBlocking { state.scrollBy(5f) } }

            waitForPrefetch()
        }

        // First scroll will initialize the nested prefetch count to 2, but use 1 as the initial
        // nested prefetch count
        assertThat(actions)
            .containsExactly(
                Action.Compose(prefetchIndex),
                Action.Compose(prefetchIndex, 5),
                Action.Measure(prefetchIndex),
                Action.Measure(prefetchIndex, 5),
                Action.Compose(prefetchIndex, 6),
                Action.Measure(prefetchIndex, 6),
            )
            .inOrder()

        // jump ahead
        rule.runOnIdle { runBlocking { state.scrollToItem(10) } }

        val newActions = trackingActions {
            rule.runOnIdle { runBlocking { state.scrollBy(5f) } }

            waitForPrefetch()
        }

        // Second scroll will use the initialized nested prefetch count
        assertThat(newActions)
            .containsExactly(
                Action.Compose(prefetchIndex + 10),
                Action.Compose(prefetchIndex + 10, 5),
                Action.Compose(prefetchIndex + 10, 6),
                Action.Measure(prefetchIndex + 10),
                Action.Measure(prefetchIndex + 10, 5),
                Action.Measure(prefetchIndex + 10, 6),
            )
            .inOrder()
    }

    private var actions: MutableList<Action>? = null

    /** Returns the list of Actions performed during block() */
    private fun trackingActions(block: () -> Unit): List<Action> {
        return mutableListOf<Action>().apply {
            actions = this
            block()
            actions = null
        }
    }

    private fun waitForPrefetch() {
        rule.runOnIdle { scheduler.executeActiveRequests() }
    }

    fun tagFor(index: Int, nestedIndex: Int? = null): String {
        return if (nestedIndex == null) {
            "$index"
        } else {
            "$index:$nestedIndex"
        }
    }

    private fun composeList(
        lazyListState: LazyListState,
        createNestedLazyListState: (index: Int) -> LazyListState = { LazyListState() },
    ) {
        rule.setContent {
            LazyColumnOrRow(
                modifier = Modifier.mainAxisSize(itemsSizeDp * 1.5f),
                state = lazyListState,
            ) {
                items(100, contentType = { "NESTED_LIST" }) { index ->
                    TrackActiveNodesEffect(index)
                    val nestedState = remember(index) { createNestedLazyListState(index) }
                    LazyColumnOrRow(
                        modifier =
                            Modifier.crossAxisSize(itemsSizeDp * 1.5f)
                                .mainAxisSize(itemsSizeDp)
                                .testTag(tagFor(index))
                                .trackWhenMeasured(index),
                        state = nestedState,
                        isCrossAxis = true,
                    ) {
                        items(100) { nestedIndex ->
                            TrackActiveNodesEffect(index, nestedIndex)
                            Spacer(
                                Modifier.mainAxisSize(itemsSizeDp)
                                    .crossAxisSize(itemsSizeDp)
                                    .testTag(tagFor(index, nestedIndex))
                                    .trackWhenMeasured(index, nestedIndex)
                            )
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun TrackActiveNodesEffect(index: Int, nestedIndex: Int? = null) {
        val tag = tagFor(index, nestedIndex)
        DisposableEffect(tag) {
            activeNodes.add(tag)
            actions?.add(Action.Compose(index, nestedIndex))
            onDispose { activeNodes.remove(tag) }
        }
    }

    private fun Modifier.trackWhenMeasured(index: Int, nestedIndex: Int? = null): Modifier {
        return this then
            Modifier.layout { measurable, constraints ->
                actions?.add(Action.Measure(index, nestedIndex))
                val placeable = measurable.measure(constraints)
                layout(placeable.width, placeable.height) { placeable.place(0, 0) }
            }
    }

    @OptIn(ExperimentalFoundationApi::class)
    private class NestedPrefetchWithConstraintsStrategy(
        private val childConstraints: Constraints,
        private val initialNestedPrefetchItemCount: Int = 2,
    ) : LazyListPrefetchStrategy {
        override fun LazyListPrefetchScope.onScroll(delta: Float, layoutInfo: LazyListLayoutInfo) {}

        override fun LazyListPrefetchScope.onVisibleItemsUpdated(layoutInfo: LazyListLayoutInfo) {}

        override fun NestedPrefetchScope.onNestedPrefetch(firstVisibleItemIndex: Int) {
            val resolvedNestedPrefetchCount =
                if (nestedPrefetchItemCount == -1) {
                    initialNestedPrefetchItemCount
                } else {
                    nestedPrefetchItemCount
                }
            repeat(resolvedNestedPrefetchCount) { i ->
                schedulePrecompositionAndPremeasure(firstVisibleItemIndex + i, childConstraints)
            }
        }
    }
}
