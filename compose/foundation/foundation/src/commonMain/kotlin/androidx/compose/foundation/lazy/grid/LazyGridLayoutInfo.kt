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

package androidx.compose.foundation.lazy.grid

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.ui.unit.IntSize
import kotlin.math.max

/**
 * Contains useful information about the currently displayed layout state of lazy grids like
 * [LazyVerticalGrid]. For example you can get the list of currently displayed items.
 *
 * Use [LazyGridState.layoutInfo] to retrieve this
 */
sealed interface LazyGridLayoutInfo {
    /** The list of [LazyGridItemInfo] representing all the currently visible items. */
    val visibleItemsInfo: List<LazyGridItemInfo>

    /**
     * The start offset of the layout's viewport in pixels. You can think of it as a minimum offset
     * which would be visible. Usually it is 0, but it can be negative if non-zero
     * [beforeContentPadding] was applied as the content displayed in the content padding area is
     * still visible.
     *
     * You can use it to understand what items from [visibleItemsInfo] are fully visible.
     */
    val viewportStartOffset: Int

    /**
     * The end offset of the layout's viewport in pixels. You can think of it as a maximum offset
     * which would be visible. It is the size of the lazy grid layout minus [beforeContentPadding].
     *
     * You can use it to understand what items from [visibleItemsInfo] are fully visible.
     */
    val viewportEndOffset: Int

    /** The total count of items passed to [LazyVerticalGrid]. */
    val totalItemsCount: Int

    /**
     * The size of the viewport in pixels. It is the lazy grid layout size including all the content
     * paddings.
     */
    val viewportSize: IntSize

    /** The orientation of the lazy grid. */
    val orientation: Orientation

    /** True if the direction of scrolling and layout is reversed. */
    val reverseLayout: Boolean

    /**
     * The content padding in pixels applied before the first row/column in the direction of
     * scrolling. For example it is a top content padding for LazyVerticalGrid with reverseLayout
     * set to false.
     */
    val beforeContentPadding: Int

    /**
     * The content padding in pixels applied after the last row/column in the direction of
     * scrolling. For example it is a bottom content padding for LazyVerticalGrid with reverseLayout
     * set to false.
     */
    val afterContentPadding: Int

    /** The spacing between lines in the direction of scrolling. */
    val mainAxisItemSpacing: Int

    /**
     * The max line span an item can occupy. This will be the number of columns in vertical grids or
     * the number of rows in horizontal grids.
     *
     * For example if [LazyVerticalGrid] has 3 columns this value will be 3 for each cell.
     */
    val maxSpan: Int
}

internal fun LazyGridLayoutInfo.visibleLinesAverageMainAxisSize(): Int {
    val isVertical = orientation == Orientation.Vertical
    val visibleItems = visibleItemsInfo
    fun lineOf(index: Int): Int =
        if (isVertical) visibleItemsInfo[index].row else visibleItemsInfo[index].column

    var totalLinesMainAxisSize = 0
    var linesCount = 0

    var lineStartIndex = 0
    while (lineStartIndex < visibleItems.size) {
        val currentLine = lineOf(lineStartIndex)
        if (currentLine == -1) {
            // Filter out exiting items.
            ++lineStartIndex
            continue
        }

        var lineMainAxisSize = 0
        var lineEndIndex = lineStartIndex
        while (lineEndIndex < visibleItems.size && lineOf(lineEndIndex) == currentLine) {
            lineMainAxisSize =
                max(
                    lineMainAxisSize,
                    if (isVertical) {
                        visibleItems[lineEndIndex].size.height
                    } else {
                        visibleItems[lineEndIndex].size.width
                    },
                )
            ++lineEndIndex
        }

        totalLinesMainAxisSize += lineMainAxisSize
        ++linesCount

        lineStartIndex = lineEndIndex
    }

    return totalLinesMainAxisSize / linesCount + mainAxisItemSpacing
}

internal val LazyGridLayoutInfo.singleAxisViewportSize: Int
    get() = if (orientation == Orientation.Vertical) viewportSize.height else viewportSize.width
