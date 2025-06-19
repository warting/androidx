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

package androidx.wear.compose.foundation.lazy

import androidx.collection.FloatList
import androidx.collection.MutableFloatList
import androidx.collection.mutableFloatSetOf
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.util.fastForEach
import kotlinx.coroutines.CoroutineScope

/** The result of the measure pass of the [TransformingLazyColumn]. */
internal class TransformingLazyColumnMeasureResult(
    /** MeasureResult defining the layout. */
    measureResult: MeasureResult,
    /** The key of the item that should be considered as an anchor during scrolling. */
    val anchorItemKey: Any,
    /** The index of the item that should be considered as an anchor during scrolling. */
    val anchorItemIndex: Int,
    /** The offset of the anchor item from the top of screen. */
    val anchorItemScrollOffset: Int,
    /** Last known height for the anchor item or negative number if it hasn't been measured. */
    val lastMeasuredItemHeight: Int,
    /** Scope for animations. */
    val coroutineScope: CoroutineScope,
    /** Layout information for the visible items. */
    override val visibleItems: List<TransformingLazyColumnVisibleItemInfo>,
    /** see [TransformingLazyColumnLayoutInfo.totalItemsCount] */
    override val totalItemsCount: Int,
    /** The spacing between items in the direction of scrolling. */
    val itemSpacing: Int,
    /** Constraints used to measure children. */
    val childConstraints: Constraints,
    /** Density of the last measure. */
    val density: Density,
    override val beforeContentPadding: Int,
    override val afterContentPadding: Int,
    /** True if there is some space available to continue scrolling in the forward direction. */
    var canScrollForward: Boolean,
    /** True if there is some space available to continue scrolling in the backward direction. */
    var canScrollBackward: Boolean,
) : TransformingLazyColumnLayoutInfo, MeasureResult by measureResult {
    /** see [TransformingLazyColumnLayoutInfo.viewportSize] */
    override val viewportSize: IntSize
        get() = IntSize(width = width, height = height)
}

internal fun TransformingLazyColumnMeasureResult.checkLayoutIsCorrect() {

    with(
        visibleItems.fastMapToFloatList {
            it.transformedHeight.toFloat() / it.measuredHeight.toFloat()
        }
    ) {
        check(hasMonotonicIncreaseAndDecrease()) {
            "Incorrect layout: Measured items height rates are not correct $this"
        }
    }
    with(
        visibleItems.fastMapToFloatList {
            it.scrollProgress.bottomOffsetFraction - it.scrollProgress.topOffsetFraction
        }
    ) {
        check(!any { it <= 0f }) {
            "Incorrect layout: Items could not have zero height or negative height $this"
        }
    }

    with(visibleItems.fastMapToFloatList { it.scrollProgress.topOffsetFraction }) {
        check(isDistinct() && isMonotonicallyIncreasing()) {
            "Incorrect layout: scrollProgress top offset fraction should be increating $this"
        }
    }

    with(visibleItems.fastMapToFloatList { it.scrollProgress.bottomOffsetFraction }) {
        check(isDistinct() && isMonotonicallyIncreasing()) {
            "Incorrect layout: scrollProgress bottom offset fraction should be increating $this"
        }
    }
}

private fun <T> List<T>.fastMapToFloatList(transform: (T) -> Float): FloatList =
    MutableFloatList(size).also { list -> fastForEach { list.add(transform(it)) } }

private fun FloatList.isDistinct(): Boolean =
    size ==
        fold(mutableFloatSetOf()) { acc, value ->
                acc.add(value)
                acc
            }
            .size

private fun FloatList.isMonotonicallyIncreasing(): Boolean =
    (1 until size).all { this[it] >= this[it - 1] }

// Confirms that the values array consists of two monotonic functions.
private fun FloatList.hasMonotonicIncreaseAndDecrease(): Boolean {
    val firstDownIndex = (1 until size).firstOrNull { this[it] < this[it - 1] }
    return firstDownIndex == null || (firstDownIndex + 1 until size).all { this[it] < this[it - 1] }
}
