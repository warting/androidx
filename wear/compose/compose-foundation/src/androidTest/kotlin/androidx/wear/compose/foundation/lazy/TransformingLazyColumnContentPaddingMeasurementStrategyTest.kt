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

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.GraphicsContext
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.layout.AlignmentLine
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import androidx.wear.compose.foundation.lazy.layout.LazyLayoutItemAnimator
import androidx.wear.compose.foundation.lazy.layout.LazyLayoutKeyIndexMap
import com.google.common.truth.Truth.assertThat
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.test.Test
import kotlinx.coroutines.CoroutineScope
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class TransformingLazyColumnContentPaddingMeasurementStrategyTest {
    private val screenHeight = 100
    private val screenWidth = 120
    private val density = Density(1f)

    private val containerConstraints =
        Constraints(
            minWidth = screenWidth,
            maxWidth = screenWidth,
            minHeight = screenHeight,
            maxHeight = screenHeight,
        )

    @Test
    fun emptyList_emptyResult() {
        val result = strategy.measure(listOf())

        assertThat(result.visibleItems).isEmpty()
    }

    @Test
    fun fullScreenItem_takesFullHeight() {
        val result = strategy.measure(listOf(screenHeight))

        assertThat(result.visibleItems.size).isEqualTo(1)

        assertThat(result.visibleItems.first().index).isEqualTo(0)
        assertThat(result.visibleItems.first().offset).isEqualTo(0)
        assertThat(result.visibleItems.first().measuredHeight).isEqualTo(screenHeight)
        assertThat(result.visibleItems.first().transformedHeight).isEqualTo(screenHeight)
    }

    @Test
    fun fullScreenItem_scrollsBackToCenter() {
        val result =
            strategy.measure(
                listOf(screenHeight),
                // Scroll is ignored as the item constrained by the screen.
                scrollToBeConsumed = 25f,
            )

        assertThat(result.visibleItems.size).isEqualTo(1)

        assertThat(result.visibleItems.first().index).isEqualTo(0)
        assertThat(result.visibleItems.first().offset).isEqualTo(0)
        assertThat(result.visibleItems.first().measuredHeight).isEqualTo(screenHeight)
        assertThat(result.visibleItems.first().transformedHeight).isEqualTo(screenHeight)
    }

    @Test
    fun halfScreenItem_takesHalfHeightAndTopAligned() {
        val result = strategy.measure(listOf(screenHeight / 2))

        assertThat(result.visibleItems.size).isEqualTo(1)

        assertThat(result.visibleItems.first().index).isEqualTo(0)
        assertThat(result.visibleItems.first().offset).isEqualTo(0)
        assertThat(result.visibleItems.first().measuredHeight).isEqualTo(screenHeight / 2)
        assertThat(result.visibleItems.first().transformedHeight).isEqualTo(screenHeight / 2)
    }

    @Test
    fun twoItemsWithFirstTopAligned_measuredWithCorrectOffsets() {
        val result = strategy.measure(listOf(screenHeight / 2, screenHeight / 2))

        assertThat(result.visibleItems.size).isEqualTo(2)

        assertThat(result.visibleItems.map { it.offset }).isEqualTo(listOf(0, screenHeight / 2))
    }

    @Test
    fun twoItemsWithFirstTopAlignedWithPadding_measuredWithCorrectOffsets() {
        val topPadding = 5.dp
        val topPaddingPx = with(density) { topPadding.roundToPx() }
        val strategyWithTopPadding = measurementStrategy(PaddingValues(top = topPadding))

        val result = strategyWithTopPadding.measure(listOf(screenHeight / 2, screenHeight / 2))

        assertThat(result.visibleItems.size).isEqualTo(2)

        assertThat(result.visibleItems.map { it.offset })
            .isEqualTo(listOf(0 + topPaddingPx, screenHeight / 2 + topPaddingPx))
        assertThat(result.beforeContentPadding).isEqualTo(topPaddingPx)
    }

    @Test
    fun twoItemsWithLastOneAlignedWithPadding_measuredWithCorrectOffsets() {
        val bottomPadding = 5.dp
        val bottomPaddingPx = with(density) { bottomPadding.roundToPx() }
        val strategyWithBottomPadding = measurementStrategy(PaddingValues(bottom = bottomPadding))

        val result = strategyWithBottomPadding.measure(listOf(screenHeight / 2, screenHeight / 2))

        assertThat(result.visibleItems.size).isEqualTo(2)

        assertThat(result.visibleItems.map { it.offset }).isEqualTo(listOf(0, screenHeight / 2))
        assertThat(result.afterContentPadding).isEqualTo(bottomPaddingPx)
    }

    @Test
    fun threeHalfScreenItemsWithFirstOneTopAligned_pushesLastItemOffscreen() {
        val result =
            strategy.measure(
                listOf(
                    // Is centered.
                    screenHeight / 2,
                    screenHeight / 2,
                    // Offscreen item.
                    screenHeight / 2,
                )
            )

        assertThat(result.visibleItems.size).isEqualTo(2)
    }

    @Test
    fun threeItemsWithSecondOneCentered_measuredWithCorrectOffsets() {
        val result =
            strategy.measure(
                listOf(
                    screenHeight / 2,
                    // Is centered.
                    screenHeight / 2,
                    // Offscreen item.
                    screenHeight / 2,
                ),
                anchorItemIndex = 1,
            )

        assertThat(result.visibleItems.size).isEqualTo(3)
        assertThat(result.visibleItems.map { it.offset })
            .isEqualTo(listOf(-screenHeight / 4, screenHeight / 4, screenHeight * 3 / 4))
    }

    @Test
    fun threeItemsWithSecondOneCenteredAndOffset_measuredWithCorrectOffsets() {
        val tinyOffset = 5
        val result =
            strategy.measure(
                listOf(
                    screenHeight / 2,
                    // Is centered with the offset.
                    screenHeight / 2,
                    screenHeight / 2,
                ),
                anchorItemIndex = 1,
                anchorItemScrollOffset = tinyOffset,
            )

        assertThat(result.visibleItems.size).isEqualTo(3)
        assertThat(result.visibleItems.map { it.offset })
            .isEqualTo(
                listOf(
                    -screenHeight / 4 + tinyOffset,
                    screenHeight / 4 + tinyOffset,
                    screenHeight * 3 / 4 + tinyOffset,
                )
            )
    }

    @Test
    fun threeItemsWithSecondOneCenteredAndScrolled_measuredWithCorrectOffsets() {
        val scrollAmount = 5
        val result =
            strategy.measure(
                listOf(
                    screenHeight / 2,
                    // Is centered with the scroll.
                    screenHeight / 2,
                    // Offscreen item.
                    screenHeight / 2,
                ),
                anchorItemIndex = 1,
                scrollToBeConsumed = scrollAmount.toFloat(),
            )

        assertThat(result.visibleItems.size).isEqualTo(3)
        assertThat(result.visibleItems.map { it.offset })
            .isEqualTo(
                listOf(
                    -screenHeight / 4 + scrollAmount,
                    screenHeight / 4 + scrollAmount,
                    screenHeight * 3 / 4 + scrollAmount,
                )
            )
    }

    @Test
    fun fullScreenItemWithTransformedHeight_takesHalfOfHeight() {
        val result =
            strategy.measure(
                listOf(
                    // Center item that appears half of the size.
                    screenHeight
                ),
                transformedHeight = { measuredHeight, _ -> measuredHeight / 2 },
            )

        assertThat(result.canScrollForward).isFalse()
        assertThat(result.canScrollBackward).isFalse()
        assertThat(result.visibleItems.size).isEqualTo(1)
        assertThat(result.visibleItems.first().offset).isEqualTo(0)
        assertThat(result.visibleItems.first().measuredHeight).isEqualTo(screenHeight)
        assertThat(result.visibleItems.first().transformedHeight).isEqualTo(screenHeight / 2)
    }

    @Test
    fun renderContentSmallerThanTheScreen_hasNoScrolling() {
        val result =
            strategy.measure(
                listOf(
                    // Centered item.
                    screenHeight / 5,
                    screenHeight / 5,
                    screenHeight / 5,
                )
            )

        assertThat(result.canScrollForward).isFalse()
        assertThat(result.canScrollBackward).isFalse()
        assertThat(result.visibleItems.size).isEqualTo(3)
    }

    @Test
    fun renderContentOnTopOfList_hasNoBackwardScrolling() {
        val result =
            strategy.measure(
                listOf(
                    // Centered item.
                    screenHeight / 2,
                    screenHeight / 2,
                    screenHeight / 2,
                )
            )

        assertThat(result.canScrollForward).isTrue()
        assertThat(result.canScrollBackward).isFalse()
        assertThat(result.visibleItems.size).isEqualTo(2)
    }

    @Test
    fun renderContentOnBottomOfList_hasNoForwardScrolling() {
        val result =
            strategy.measure(
                listOf(
                    screenHeight / 2,
                    screenHeight / 2,
                    // Centered item.
                    screenHeight / 2,
                ),
                anchorItemIndex = 2,
            )

        assertThat(result.canScrollForward).isFalse()
        assertThat(result.canScrollBackward).isTrue()
        assertThat(result.visibleItems.size).isEqualTo(2)
    }

    @Test
    fun dynamicHeightItems_measuredWithCorrectOffsets() {
        val result =
            strategy.measure(
                listOf(
                    // Will be half of the size and therefore placed at 0 offset.
                    screenHeight / 2,
                    // Centered.
                    screenHeight / 2,
                ),
                anchorItemIndex = 1,
                transformedHeight = { measuredHeight, scrollProgression ->
                    if (scrollProgression.topOffsetFraction < 0.25f) {
                        measuredHeight / 2
                    } else measuredHeight
                },
            )

        assertThat(result.visibleItems.size).isEqualTo(2)
        assertThat(result.visibleItems.map { it.offset }).isEqualTo(listOf(0, screenHeight / 4))
    }

    @Test
    fun flingBackwards_restoresLayoutCorrectly() {
        val itemSize = screenHeight / 4

        val result =
            strategy.measure(
                listOf(
                    // Items visible before the fling.
                    itemSize,
                    itemSize,
                    itemSize,
                    itemSize,
                    // Items visible after the fling.
                    itemSize,
                    itemSize,
                    itemSize,
                    itemSize,
                ),
                scrollToBeConsumed = -10 * screenHeight.toFloat(),
            )
        assertThat(result.visibleItems.map { it.index }).isEqualTo(listOf(4, 5, 6, 7))
        assertThat(result.visibleItems.map { it.offset })
            .isEqualTo(listOf(0, screenHeight / 4, screenHeight / 2, screenHeight * 3 / 4))
    }

    @Test
    fun flingForward_restoresLayoutCorrectly() {
        val itemSize = screenHeight / 4

        val result =
            strategy.measure(
                listOf(
                    // Items visible after the fling.
                    itemSize,
                    itemSize,
                    itemSize,
                    itemSize,
                    // Items visible before the fling.
                    itemSize,
                    itemSize,
                    itemSize,
                    itemSize,
                ),
                anchorItemIndex = 4,
                scrollToBeConsumed = 10 * screenHeight.toFloat(),
            )
        assertThat(result.visibleItems.map { it.index }).isEqualTo(listOf(0, 1, 2, 3))
        assertThat(result.visibleItems.map { it.offset })
            .isEqualTo(listOf(0, screenHeight / 4, screenHeight / 2, screenHeight * 3 / 4))
    }

    @Test
    fun fullSizeBottomContentPadding_doesNotCrash() {
        val strategy =
            measurementStrategy(
                // Padding takes the full size.
                PaddingValues(bottom = with(density) { screenHeight.toDp() })
            )

        val itemSize = screenHeight / 4

        val result = strategy.measure(listOf(itemSize, itemSize))
        assertThat(result.visibleItems.size).isEqualTo(2)
    }

    @Test
    fun fullSizeTopContentPadding_doesNotCrash() {
        val strategy =
            measurementStrategy(
                // Padding takes the full size.
                PaddingValues(top = with(density) { screenHeight.toDp() })
            )

        val itemSize = screenHeight / 4

        val result = strategy.measure(listOf(itemSize, itemSize))
        assertThat(result.visibleItems.size).isEqualTo(2)
    }

    private val mockGraphicContext =
        object : GraphicsContext {
            override fun createGraphicsLayer(): GraphicsLayer {
                TODO("Not yet implemented")
            }

            override fun releaseGraphicsLayer(layer: GraphicsLayer) {
                TODO("Not yet implemented")
            }
        }

    private val mockItemAnimator = LazyLayoutItemAnimator<TransformingLazyColumnMeasuredItem>()

    private fun measurementStrategy(contentPadding: PaddingValues) =
        TransformingLazyColumnContentPaddingMeasurementStrategy(
            contentPadding,
            density = density,
            layoutDirection = LayoutDirection.Ltr,
            mockGraphicContext,
            mockItemAnimator,
        )

    private val strategy = measurementStrategy(PaddingValues())

    private fun TransformingLazyColumnMeasurementStrategy.measure(
        itemHeights: List<Int>,
        transformedHeight: ((Int, TransformingLazyColumnItemScrollProgress) -> Int)? = null,
        itemSpacing: Int = 0,
        anchorItemIndex: Int = 0,
        anchorItemScrollOffset: Int = 0,
        lastMeasuredAnchorItemHeight: Int = Int.MIN_VALUE,
        scrollToBeConsumed: Float = 0f,
    ): TransformingLazyColumnMeasureResult =
        measure(
            itemsCount = itemHeights.size,
            measuredItemProvider = makeMeasuredItemProvider(itemHeights, transformedHeight),
            keyIndexMap = LazyLayoutKeyIndexMap.Empty,
            itemSpacing = itemSpacing,
            containerConstraints = containerConstraints,
            anchorItemIndex = anchorItemIndex,
            anchorItemScrollOffset = anchorItemScrollOffset,
            lastMeasuredAnchorItemHeight = lastMeasuredAnchorItemHeight,
            scrollToBeConsumed = scrollToBeConsumed,
            coroutineScope = CoroutineScope(EmptyCoroutineContext),
            density = density,
            layout = { width, height, _ ->
                object : MeasureResult {
                    override val width = width
                    override val height = height
                    override val alignmentLines
                        get() = TODO("Not yet implemented")

                    override fun placeChildren() {}
                }
            },
        )

    private class EmptyPlaceable(
        width: Int,
        height: Int,
        val transformedHeight: ((Int, TransformingLazyColumnItemScrollProgress) -> Int)?,
    ) : Placeable() {
        init {
            measuredSize = IntSize(width, height)
        }

        override fun get(alignmentLine: AlignmentLine): Int = AlignmentLine.Unspecified

        override fun placeAt(
            position: IntOffset,
            zIndex: Float,
            layerBlock: (GraphicsLayerScope.() -> Unit)?,
        ) {}

        override val parentData: Any?
            get() = transformedHeight?.let { TransformingLazyColumnParentData(it) }
    }

    private fun makeMeasuredItemProvider(
        itemHeights: List<Int>,
        transformedHeight: ((Int, TransformingLazyColumnItemScrollProgress) -> Int)? = null,
    ) = MeasuredItemProvider { index, offset, measurementDirection, progressProvider ->
        TransformingLazyColumnMeasuredItem(
            index = index,
            offset = offset,
            placeable =
                EmptyPlaceable(
                    width = screenWidth,
                    height = itemHeights[index],
                    transformedHeight = transformedHeight,
                ),
            containerConstraints = containerConstraints,
            spacing = 8,
            leftPadding = 0,
            rightPadding = 0,
            measureScrollProgress = progressProvider(itemHeights[index]),
            measurementDirection = measurementDirection,
            horizontalAlignment = Alignment.CenterHorizontally,
            layoutDirection = LayoutDirection.Ltr,
            key = index,
            contentType = null,
        )
    }
}
