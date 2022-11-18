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

package androidx.tv.foundation.lazy.grid

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.lazy.layout.LazyLayoutPrefetchState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collection.mutableVectorOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.layout.Remeasurement
import androidx.compose.ui.layout.RemeasurementModifier
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.util.fastForEach
import androidx.tv.foundation.lazy.layout.animateScrollToItem
import androidx.tv.foundation.lazy.list.AwaitFirstLayoutModifier
import kotlin.math.abs

/**
 * Creates a [TvLazyGridState] that is remembered across compositions.
 *
 * Changes to the provided initial values will **not** result in the state being recreated or
 * changed in any way if it has already been created.
 *
 * @param initialFirstVisibleItemIndex the initial value for [TvLazyGridState.firstVisibleItemIndex]
 * @param initialFirstVisibleItemScrollOffset the initial value for
 * [TvLazyGridState.firstVisibleItemScrollOffset]
 */
@Composable
fun rememberTvLazyGridState(
    initialFirstVisibleItemIndex: Int = 0,
    initialFirstVisibleItemScrollOffset: Int = 0
): TvLazyGridState {
    return rememberSaveable(saver = TvLazyGridState.Saver) {
        TvLazyGridState(
            initialFirstVisibleItemIndex,
            initialFirstVisibleItemScrollOffset
        )
    }
}

/**
 * A state object that can be hoisted to control and observe scrolling.
 *
 * In most cases, this will be created via [rememberTvLazyGridState].
 *
 * @param firstVisibleItemIndex the initial value for [TvLazyGridState.firstVisibleItemIndex]
 * @param firstVisibleItemScrollOffset the initial value for
 * [TvLazyGridState.firstVisibleItemScrollOffset]
 */
@OptIn(ExperimentalFoundationApi::class)
@Stable
class TvLazyGridState constructor(
    firstVisibleItemIndex: Int = 0,
    firstVisibleItemScrollOffset: Int = 0
) : ScrollableState {
    /**
     * The holder class for the current scroll position.
     */
    private val scrollPosition =
        LazyGridScrollPosition(firstVisibleItemIndex, firstVisibleItemScrollOffset)

    /**
     * The index of the first item that is visible.
     *
     * Note that this property is observable and if you use it in the composable function it will
     * be recomposed on every change causing potential performance issues.
     */
    val firstVisibleItemIndex: Int get() = scrollPosition.index.value

    /**
     * The scroll offset of the first visible item. Scrolling forward is positive - i.e., the
     * amount that the item is offset backwards
     */
    val firstVisibleItemScrollOffset: Int get() = scrollPosition.scrollOffset

    /** Backing state for [layoutInfo] */
    private val layoutInfoState = mutableStateOf<TvLazyGridLayoutInfo>(EmptyTvLazyGridLayoutInfo)

    /**
     * The object of [TvLazyGridLayoutInfo] calculated during the last layout pass. For example,
     * you can use it to calculate what items are currently visible.
     *
     * Note that this property is observable and is updated after every scroll or remeasure.
     * If you use it in the composable function it will be recomposed on every change causing
     * potential performance issues including infinity recomposition loop.
     * Therefore, avoid using it in the composition.
     */
    val layoutInfo: TvLazyGridLayoutInfo get() = layoutInfoState.value

    /**
     * [InteractionSource] that will be used to dispatch drag events when this
     * grid is being dragged. If you want to know whether the fling (or animated scroll) is in
     * progress, use [isScrollInProgress].
     */
    val interactionSource: InteractionSource get() = internalInteractionSource

    internal val internalInteractionSource: MutableInteractionSource = MutableInteractionSource()

    /**
     * The amount of scroll to be consumed in the next layout pass.  Scrolling forward is negative
     * - that is, it is the amount that the items are offset in y
     */
    internal var scrollToBeConsumed = 0f
        private set

    /**
     * Needed for [animateScrollToItem]. Updated on every measure.
     */
    internal var slotsPerLine: Int by mutableStateOf(0)

    /**
     * Needed for [animateScrollToItem]. Updated on every measure.
     */
    internal var density: Density by mutableStateOf(Density(1f, 1f))

    /**
     * Needed for [notifyPrefetch].
     */
    internal var isVertical: Boolean by mutableStateOf(true)

    /**
     * The ScrollableController instance. We keep it as we need to call stopAnimation on it once
     * we reached the end of the grid.
     */
    private val scrollableState = ScrollableState { -onScroll(-it) }

    /**
     * Only used for testing to confirm that we're not making too many measure passes
     */
    /*@VisibleForTesting*/
    internal var numMeasurePasses: Int = 0
        private set

    /**
     * Only used for testing to disable prefetching when needed to test the main logic.
     */
    /*@VisibleForTesting*/
    internal var prefetchingEnabled: Boolean = true

    /**
     * The index scheduled to be prefetched (or the last prefetched index if the prefetch is done).
     */
    private var lineToPrefetch = -1

    /**
     * The list of handles associated with the items from the [lineToPrefetch] line.
     */
    private val currentLinePrefetchHandles =
        mutableVectorOf<LazyLayoutPrefetchState.PrefetchHandle>()

    /**
     * Keeps the scrolling direction during the previous calculation in order to be able to
     * detect the scrolling direction change.
     */
    private var wasScrollingForward = false

    /**
     * The [Remeasurement] object associated with our layout. It allows us to remeasure
     * synchronously during scroll.
     */
    private var remeasurement: Remeasurement? by mutableStateOf(null)

    /**
     * The modifier which provides [remeasurement].
     */
    internal val remeasurementModifier = object : RemeasurementModifier {
        override fun onRemeasurementAvailable(remeasurement: Remeasurement) {
            this@TvLazyGridState.remeasurement = remeasurement
        }
    }

    /**
     * Provides a modifier which allows to delay some interactions (e.g. scroll)
     * until layout is ready.
     */
    internal val awaitLayoutModifier = AwaitFirstLayoutModifier()

    /**
     * Finds items on a line and their measurement constraints. Used for prefetching.
     */
    internal var prefetchInfoRetriever: (line: LineIndex) -> List<Pair<Int, Constraints>> by
    mutableStateOf({ emptyList() })

    internal var placementAnimator by mutableStateOf<LazyGridItemPlacementAnimator?>(null)

    private val animateScrollScope = LazyGridAnimateScrollScope(this)

    /**
     * Instantly brings the item at [index] to the top of the viewport, offset by [scrollOffset]
     * pixels.
     *
     * @param index the index to which to scroll. Must be non-negative.
     * @param scrollOffset the offset that the item should end up after the scroll. Note that
     * positive offset refers to forward scroll, so in a top-to-bottom list, positive offset will
     * scroll the item further upward (taking it partly offscreen).
     */
    suspend fun scrollToItem(
        /*@IntRange(from = 0)*/
        index: Int,
        scrollOffset: Int = 0
    ) {
        scroll {
            snapToItemIndexInternal(index, scrollOffset)
        }
    }

    internal fun snapToItemIndexInternal(index: Int, scrollOffset: Int) {
        scrollPosition.requestPosition(ItemIndex(index), scrollOffset)
        // placement animation is not needed because we snap into a new position.
        placementAnimator?.reset()
        remeasurement?.forceRemeasure()
    }

    /**
     * Call this function to take control of scrolling and gain the ability to send scroll events
     * via [ScrollScope.scrollBy]. All actions that change the logical scroll position must be
     * performed within a [scroll] block (even if they don't call any other methods on this
     * object) in order to guarantee that mutual exclusion is enforced.
     *
     * If [scroll] is called from elsewhere, this will be canceled.
     */
    override suspend fun scroll(
        scrollPriority: MutatePriority,
        block: suspend ScrollScope.() -> Unit
    ) {
        awaitLayoutModifier.waitForFirstLayout()
        scrollableState.scroll(scrollPriority, block)
    }

    override fun dispatchRawDelta(delta: Float): Float =
        scrollableState.dispatchRawDelta(delta)

    override val isScrollInProgress: Boolean
        get() = scrollableState.isScrollInProgress

    override var canScrollForward: Boolean by mutableStateOf(false)
        private set
    override var canScrollBackward: Boolean by mutableStateOf(false)
        private set

    // TODO: Coroutine scrolling APIs will allow this to be private again once we have more
    //  fine-grained control over scrolling
    /*@VisibleForTesting*/
    internal fun onScroll(distance: Float): Float {
        if (distance < 0 && !canScrollForward || distance > 0 && !canScrollBackward) {
            return 0f
        }
        check(abs(scrollToBeConsumed) <= 0.5f) {
            "entered drag with non-zero pending scroll: $scrollToBeConsumed"
        }
        scrollToBeConsumed += distance

        // scrollToBeConsumed will be consumed synchronously during the forceRemeasure invocation
        // inside measuring we do scrollToBeConsumed.roundToInt() so there will be no scroll if
        // we have less than 0.5 pixels
        if (abs(scrollToBeConsumed) > 0.5f) {
            val preScrollToBeConsumed = scrollToBeConsumed
            remeasurement?.forceRemeasure()
            if (prefetchingEnabled) {
                notifyPrefetch(preScrollToBeConsumed - scrollToBeConsumed)
            }
        }

        // here scrollToBeConsumed is already consumed during the forceRemeasure invocation
        if (abs(scrollToBeConsumed) <= 0.5f) {
            // We consumed all of it - we'll hold onto the fractional scroll for later, so report
            // that we consumed the whole thing
            return distance
        } else {
            val scrollConsumed = distance - scrollToBeConsumed
            // We did not consume all of it - return the rest to be consumed elsewhere (e.g.,
            // nested scrolling)
            scrollToBeConsumed = 0f // We're not consuming the rest, give it back
            return scrollConsumed
        }
    }

    private fun notifyPrefetch(delta: Float) {
        val prefetchState = prefetchState
        if (!prefetchingEnabled) {
            return
        }
        val info = layoutInfo
        if (info.visibleItemsInfo.isNotEmpty()) {
            val scrollingForward = delta < 0
            val lineToPrefetch: Int
            val closestNextItemToPrefetch: Int
            if (scrollingForward) {
                lineToPrefetch = 1 + info.visibleItemsInfo.last().let {
                    if (isVertical) it.row else it.column
                }
                closestNextItemToPrefetch = info.visibleItemsInfo.last().index + 1
            } else {
                lineToPrefetch = -1 + info.visibleItemsInfo.first().let {
                    if (isVertical) it.row else it.column
                }
                closestNextItemToPrefetch = info.visibleItemsInfo.first().index - 1
            }
            if (lineToPrefetch != this.lineToPrefetch &&
                closestNextItemToPrefetch in 0 until info.totalItemsCount
            ) {
                if (wasScrollingForward != scrollingForward) {
                    // the scrolling direction has been changed which means the last prefetched
                    // is not going to be reached anytime soon so it is safer to dispose it.
                    // if this line is already visible it is safe to call the method anyway
                    // as it will be no-op
                    currentLinePrefetchHandles.forEach { it.cancel() }
                }
                this.wasScrollingForward = scrollingForward
                this.lineToPrefetch = lineToPrefetch
                currentLinePrefetchHandles.clear()
                prefetchInfoRetriever(LineIndex(lineToPrefetch)).fastForEach {
                    currentLinePrefetchHandles.add(
                        prefetchState.schedulePrefetch(it.first, it.second)
                    )
                }
            }
        }
    }

    private fun cancelPrefetchIfVisibleItemsChanged(info: TvLazyGridLayoutInfo) {
        if (lineToPrefetch != -1 && info.visibleItemsInfo.isNotEmpty()) {
            val expectedLineToPrefetch = if (wasScrollingForward) {
                info.visibleItemsInfo.last().let {
                    if (isVertical) it.row else it.column
                } + 1
            } else {
                info.visibleItemsInfo.first().let {
                    if (isVertical) it.row else it.column
                } - 1
            }
            if (lineToPrefetch != expectedLineToPrefetch) {
                lineToPrefetch = -1
                currentLinePrefetchHandles.forEach { it.cancel() }
                currentLinePrefetchHandles.clear()
            }
        }
    }

    internal val prefetchState = LazyLayoutPrefetchState()

    /**
     * Animate (smooth scroll) to the given item.
     *
     * @param index the index to which to scroll. Must be non-negative.
     * @param scrollOffset the offset that the item should end up after the scroll. Note that
     * positive offset refers to forward scroll, so in a top-to-bottom list, positive offset will
     * scroll the item further upward (taking it partly offscreen).
     */
    suspend fun animateScrollToItem(
        /*@IntRange(from = 0)*/
        index: Int,
        scrollOffset: Int = 0
    ) {
        animateScrollScope.animateScrollToItem(index, scrollOffset)
    }

    /**
     *  Updates the state with the new calculated scroll position and consumed scroll.
     */
    internal fun applyMeasureResult(result: TvLazyGridMeasureResult) {
        scrollPosition.updateFromMeasureResult(result)
        scrollToBeConsumed -= result.consumedScroll
        layoutInfoState.value = result

        canScrollForward = result.canScrollForward
        canScrollBackward = (result.firstVisibleLine?.index?.value ?: 0) != 0 ||
            result.firstVisibleLineScrollOffset != 0

        numMeasurePasses++

        cancelPrefetchIfVisibleItemsChanged(result)
    }

    /**
     * When the user provided custom keys for the items we can try to detect when there were
     * items added or removed before our current first visible item and keep this item
     * as the first visible one even given that its index has been changed.
     */
    internal fun updateScrollPositionIfTheFirstItemWasMoved(itemProvider: LazyGridItemProvider) {
        scrollPosition.updateScrollPositionIfTheFirstItemWasMoved(itemProvider)
    }

    companion object {
        /**
         * The default [Saver] implementation for [TvLazyGridState].
         */
        val Saver: Saver<TvLazyGridState, *> = listSaver(
            save = { listOf(it.firstVisibleItemIndex, it.firstVisibleItemScrollOffset) },
            restore = {
                TvLazyGridState(
                    firstVisibleItemIndex = it[0],
                    firstVisibleItemScrollOffset = it[1]
                )
            }
        )
    }
}

private object EmptyTvLazyGridLayoutInfo : TvLazyGridLayoutInfo {
    override val visibleItemsInfo = emptyList<TvLazyGridItemInfo>()
    override val viewportStartOffset = 0
    override val viewportEndOffset = 0
    override val totalItemsCount = 0
    override val viewportSize = IntSize.Zero
    override val orientation = Orientation.Vertical
    override val reverseLayout = false
    override val beforeContentPadding: Int = 0
    override val afterContentPadding: Int = 0
}
