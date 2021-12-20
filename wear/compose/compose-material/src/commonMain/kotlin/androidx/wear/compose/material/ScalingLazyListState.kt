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

package androidx.wear.compose.material

import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.LazyListLayoutInfo
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import kotlin.math.roundToInt

/**
 * Creates a [ScalingLazyListState] that is remembered across compositions.
 *
 * @param initialCenterItemIndex the initial value for [ScalingLazyListState.centerItemIndex]
 * @param initialCenterItemScrollOffset the initial value for
 * [ScalingLazyListState.centerItemScrollOffset] in pixels
 */
@Composable
public fun rememberScalingLazyListState(
    initialCenterItemIndex: Int = 0,
    initialCenterItemScrollOffset: Int = 0
): ScalingLazyListState {
    return rememberSaveable(saver = ScalingLazyListState.Saver) {
        ScalingLazyListState(
            initialCenterItemIndex,
            initialCenterItemScrollOffset
        )
    }
}

/**
 * A state object that can be hoisted to control and observe scrolling.
 *
 * In most cases, this will be created via [rememberScalingLazyListState].
 *
 * @param initialCenterItemIndex the initial value for [ScalingLazyListState.centerItemIndex]
 * @param initialCenterItemScrollOffset the initial value for
 * [ScalingLazyListState.centerItemScrollOffset]
 */
// TODO (b/193792848): Add snap support.
@Stable
class ScalingLazyListState constructor(
    private val initialCenterItemIndex: Int = 0,
    private val initialCenterItemScrollOffset: Int = 0
) : ScrollableState {

    internal var lazyListState: LazyListState = LazyListState(0, 0)
    internal val extraPaddingPx = mutableStateOf<Int?>(null)
    internal val beforeContentPaddingPx = mutableStateOf<Int?>(null)
    internal val scalingParams = mutableStateOf<ScalingParams?>(null)
    internal val gapBetweenItemsPx = mutableStateOf<Int?>(null)
    internal val viewportHeightPx = mutableStateOf<Int?>(null)
    internal val reverseLayout = mutableStateOf<Boolean?>(null)
    internal val anchorType = mutableStateOf<ScalingLazyListAnchorType?>(null)
    internal val initialized = mutableStateOf<Boolean>(false)

    /**
     * The index of the item positioned closest to the viewport center
     */
    public val centerItemIndex: Int
        get() = (layoutInfo as? DefaultScalingLazyListLayoutInfo)?.centerItemIndex ?: 0

    /**
     * The offset of the item closest to the viewport center. Depending on the [ScalingLazyListAnchorType] of the
     * [ScalingLazyColumn] the offset will be relative to either items Edge or Center.
     */
    public val centerItemScrollOffset: Int
        get() = (layoutInfo as? DefaultScalingLazyListLayoutInfo)?.centerItemScrollOffset ?: 0

    /**
     * The object of [ScalingLazyListLayoutInfo] calculated during the last layout pass. For
     * example, you can use it to calculate what items are currently visible.
     */
    public val layoutInfo: ScalingLazyListLayoutInfo by derivedStateOf {
        if (extraPaddingPx.value == null || scalingParams.value == null ||
            gapBetweenItemsPx.value == null || viewportHeightPx.value == null ||
            anchorType.value == null || reverseLayout.value == null ||
            beforeContentPaddingPx.value == null
        ) {
            EmptyScalingLazyListLayoutInfo
        } else {
            val visibleItemsInfo = mutableListOf<ScalingLazyListItemInfo>()
            var newCenterItemIndex = -1
            var newCenterItemScrollOffset = 0

            if (lazyListState.layoutInfo.visibleItemsInfo.isNotEmpty()) {
                val verticalAdjustment =
                    lazyListState.layoutInfo.viewportStartOffset + extraPaddingPx.value!!

                // Find the item in the middle of the viewport
                val centralItem =
                    findItemNearestCenter(viewportHeightPx.value!!, verticalAdjustment)!!

                // Place the center item
                val centerItemInfoAndOffsetDelta = calculateItemInfo(
                    centralItem.offset,
                    centralItem,
                    verticalAdjustment,
                    viewportHeightPx.value!!,
                    scalingParams.value!!,
                    beforeContentPaddingPx.value!!,
                    anchorType.value!!,
                    initialized.value
                )
                visibleItemsInfo.add(
                    centerItemInfoAndOffsetDelta.itemInfo
                )

                newCenterItemIndex = centralItem.index
                newCenterItemScrollOffset = - centerItemInfoAndOffsetDelta.itemInfo.offset

                // Go Up
                var nextItemBottomNoPadding =
                    centerItemInfoAndOffsetDelta.offsetAdjusted() - gapBetweenItemsPx.value!!
                val minIndex =
                    lazyListState.layoutInfo.visibleItemsInfo.minOf { it.index }
                (newCenterItemIndex - 1 downTo minIndex).forEach { ix ->
                    val currentItem =
                        lazyListState.layoutInfo.findItemInfoWithIndex(ix)!!
                    val itemInfoAndOffset = calculateItemInfo(
                        nextItemBottomNoPadding - currentItem.size,
                        currentItem,
                        verticalAdjustment,
                        viewportHeightPx.value!!,
                        scalingParams.value!!,
                        beforeContentPaddingPx.value!!,
                        anchorType.value!!,
                        initialized.value
                    )
                    // If the item is visible in the viewport insert it at the start of the
                    // list
                    if (
                        (itemInfoAndOffset.offsetAdjusted() + itemInfoAndOffset.itemInfo.size) >
                        verticalAdjustment) {
                        // Insert the item info at the front of the list
                        visibleItemsInfo.add(0, itemInfoAndOffset.itemInfo)
                    }
                    nextItemBottomNoPadding =
                        itemInfoAndOffset.offsetAdjusted() - gapBetweenItemsPx.value!!
                }
                // Go Down
                var nextItemTopNoPadding =
                    centerItemInfoAndOffsetDelta.offsetAdjusted() +
                        centerItemInfoAndOffsetDelta.itemInfo.size +
                        gapBetweenItemsPx.value!!
                val maxIndex =
                    lazyListState.layoutInfo.visibleItemsInfo.maxOf { it.index }
                (newCenterItemIndex + 1..maxIndex).forEach { ix ->
                    val currentItem =
                        lazyListState.layoutInfo.findItemInfoWithIndex(ix)!!
                    val itemInfoAndOffset = calculateItemInfo(
                        nextItemTopNoPadding,
                        currentItem,
                        verticalAdjustment,
                        viewportHeightPx.value!!,
                        scalingParams.value!!,
                        beforeContentPaddingPx.value!!,
                        anchorType.value!!,
                        initialized.value
                    )
                    // If the item is visible in the viewport insert it at the end of the
                    // list
                    if ((itemInfoAndOffset.offsetAdjusted() - verticalAdjustment) <
                        viewportHeightPx.value!!) {
                        visibleItemsInfo.add(itemInfoAndOffset.itemInfo)
                    }
                    nextItemTopNoPadding =
                        itemInfoAndOffset.offsetAdjusted() + itemInfoAndOffset.itemInfo.size +
                            gapBetweenItemsPx.value!!
                }
            }

            DefaultScalingLazyListLayoutInfo(
                visibleItemsInfo = visibleItemsInfo,
                totalItemsCount = lazyListState.layoutInfo.totalItemsCount,
                viewportStartOffset = lazyListState.layoutInfo.viewportStartOffset +
                    extraPaddingPx.value!!,
                viewportEndOffset = lazyListState.layoutInfo.viewportEndOffset -
                    extraPaddingPx.value!!,
                centerItemIndex = if (initialized.value) newCenterItemIndex else 0,
                centerItemScrollOffset = if (initialized.value) newCenterItemScrollOffset else 0
            )
        }
    }

    private fun findItemNearestCenter(
        viewportHeightPx: Int,
        verticalAdjustment: Int
    ): LazyListItemInfo? {
        val centerLine = viewportHeightPx / 2
        var result: LazyListItemInfo? = null
        // Find the item in the middle of the viewport
        for (item in lazyListState.layoutInfo.visibleItemsInfo) {
            val rawItemStart = item.offset - verticalAdjustment
            val rawItemEnd = rawItemStart + item.size
            result = item
            if (rawItemEnd > centerLine) {
                break
            }
        }
        return result
    }

    companion object {
        /**
         * The default [Saver] implementation for [ScalingLazyListState].
         */
        val Saver = listSaver<ScalingLazyListState, Int>(
            save = {
                listOf(
                    it.centerItemIndex,
                    it.centerItemScrollOffset,
                )
            },
            restore = {
                val scalingLazyColumnState = ScalingLazyListState(it[0], it[1])
                scalingLazyColumnState
            }
        )
    }

    override val isScrollInProgress: Boolean
        get() {
            return lazyListState.isScrollInProgress
        }

    override fun dispatchRawDelta(delta: Float): Float {
        return lazyListState.dispatchRawDelta(delta)
    }

    override suspend fun scroll(
        scrollPriority: MutatePriority,
        block: suspend ScrollScope.() -> Unit
    ) {
        lazyListState.scroll(scrollPriority = scrollPriority, block = block)
    }

    /**
     * Instantly brings the item at [index] to the center of the viewport and positions it based on
     * the [anchorType] and applies the [scrollOffset] pixels.
     *
     * @param index the index to which to scroll. Must be non-negative.
     * @param scrollOffset the offset that the item should end up after the scroll. Note that
     * positive offset refers to forward scroll, so in a top-to-bottom list, positive offset will
     * scroll the item further upward (taking it partly offscreen).
     */
    public suspend fun scrollToItem(
        /*@IntRange(from = 0)*/
        index: Int,
        /*@IntRange(from = 0)*/
        scrollOffset: Int = 0
    ) {
        val offsetToCenterOfViewport =
            beforeContentPaddingPx.value!! - (viewportHeightPx.value!! / 2)
        if (anchorType.value == ScalingLazyListAnchorType.ItemStart) {
            val offset = offsetToCenterOfViewport + scrollOffset
            return lazyListState.scrollToItem(index, offset)
        } else {
            var item = lazyListState.layoutInfo.findItemInfoWithIndex(index)
            if (item == null) {
                // Scroll the item into the middle of the viewport so that we know it is visible
                lazyListState.scrollToItem(
                    index,
                    offsetToCenterOfViewport
                )
                // Now we know that the item is visible find it and fine tune our position
                item = lazyListState.layoutInfo.findItemInfoWithIndex(index)
            }
            if (item != null) {
                val offset = offsetToCenterOfViewport + (item.size / 2) + scrollOffset
                return lazyListState.scrollToItem(index, offset)
            }
        }
        return
    }

    internal suspend fun scrollToInitialItem() {
        if (!initialized.value) {
            initialized.value = true
            scrollToItem(initialCenterItemIndex, initialCenterItemScrollOffset)
        }
        return
    }

    /**
     * Animate (smooth scroll) the given item at [index] to the center of the viewport and position
     * it based on the [anchorType] and applies the [scrollOffset] pixels.
     *
     * @param index the index to which to scroll. Must be non-negative.
     * @param scrollOffset the offset that the item should end up after the scroll (same as
     * [scrollToItem]) - note that positive offset refers to forward scroll, so in a
     * top-to-bottom list, positive offset will scroll the item further upward (taking it partly
     * offscreen)
     */
    public suspend fun animateScrollToItem(
        /*@IntRange(from = 0)*/
        index: Int,
        /*@IntRange(from = 0)*/
        scrollOffset: Int = 0
    ) {
        val offsetToCenterOfViewport =
            beforeContentPaddingPx.value!! - (viewportHeightPx.value!! / 2)
        if (anchorType.value == ScalingLazyListAnchorType.ItemStart) {
            val offset = offsetToCenterOfViewport + scrollOffset
            return lazyListState.animateScrollToItem(index, offset)
        } else {
            var item = lazyListState.layoutInfo.findItemInfoWithIndex(index)
            var sizeEstimate = 0
            if (item == null) {
                // Guess the size of the item so that we can try and position it correctly
                sizeEstimate = lazyListState.layoutInfo.averageItemSize()
                // Scroll the item towards the middle of the viewport so that we know it is visible
                lazyListState.animateScrollToItem(
                    index,
                    offsetToCenterOfViewport + (sizeEstimate / 2) + scrollOffset
                )
                // Now we know that the item is visible find it and fine tune our position
                item = lazyListState.layoutInfo.findItemInfoWithIndex(index)
            }
            // Determine if a second adjustment is needed
            if (item != null && item.size != sizeEstimate) {
                val offset = offsetToCenterOfViewport + (item.size / 2) + scrollOffset
                return lazyListState.animateScrollToItem(index, offset)
            }
        }
        return
    }
}

private fun LazyListLayoutInfo.findItemInfoWithIndex(index: Int): LazyListItemInfo? {
    return this.visibleItemsInfo.find { it.index == index }
}

private fun LazyListLayoutInfo.averageItemSize(): Int {
    var totalSize = 0
    visibleItemsInfo.forEach { totalSize += it.size }
    return if (visibleItemsInfo.isNotEmpty())
        (totalSize.toFloat() / visibleItemsInfo.size).roundToInt()
    else 0
}

private object EmptyScalingLazyListLayoutInfo : ScalingLazyListLayoutInfo {
    override val visibleItemsInfo = emptyList<ScalingLazyListItemInfo>()
    override val viewportStartOffset = 0
    override val viewportEndOffset = 0
    override val totalItemsCount = 0
}
