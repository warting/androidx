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

@file:Suppress("DEPRECATION")

package androidx.wear.compose.material

import androidx.annotation.FloatRange
import androidx.compose.animation.core.Easing
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.runtime.Stable
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.util.lerp
import androidx.wear.compose.foundation.lazy.ScaleAndAlpha
import androidx.wear.compose.foundation.lazy.inverseLerp
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Parameters to control the scaling of the contents of a [ScalingLazyColumn].
 *
 * Items in the ScalingLazyColumn have scaling and alpha effects applied to them depending on their
 * position in the viewport. The closer to the edge (top or bottom) of the viewport that they are
 * the greater the down scaling and transparency that is applied. Note that scaling and transparency
 * effects are applied from the center of the viewport (nearest to full size and normal
 * transparency) towards the edge (items can be smaller and more transparent).
 *
 * Deciding how much scaling and alpha to apply is based on the position and size of the item and on
 * a series of properties that are used to determine the transition area for each item.
 *
 * The transition area is defined by the edge of the screen and a transition line which is
 * calculated for each item in the list. There are two symmetrical transition lines/areas one at the
 * top of the viewport and one at the bottom.
 *
 * The items transition line is based upon its size with the potential for larger list items to
 * start their transition earlier (further from the edge they are transitioning towards) than
 * smaller items.
 *
 * It is possible for the transition line to be closer to the edge that the list item is moving away
 * from, i.e. the opposite side of the center line of the viewport. This may seem counter-intuitive
 * at first as it means that the transition lines can appear inverted. But as the two transition
 * lines interact with the opposite edges of the list item top with bottom, bottom with top it is
 * often desirable to have inverted transition lines for large list items.
 *
 * [minTransitionArea] and [maxTransitionArea] are both in the range [0f..1f] and are the fraction
 * of the distance between the edges of the viewport. E.g. a value of 0.2f for minTransitionArea and
 * 0.75f for maxTransitionArea determines that all transition lines will fall between 1/5th (20%)
 * and 3/4s (75%) of the height of the viewport.
 *
 * The size of the each item is used to determine where within the transition area range
 * minTransitionArea..maxTransitionArea the actual transition line will be. [minElementHeight] and
 * [maxElementHeight] are used along with the item height (as a fraction of the viewport height in
 * the range [0f..1f]) to find the transition line. So if the items size is 0.25f (25%) of way
 * between minElementSize..maxElementSize then the transition line will be 0.25f (25%) of the way
 * between minTransitionArea..maxTransitionArea.
 *
 * A list item smaller than minElementHeight is rounded up to minElementHeight and larger than
 * maxElementHeight is rounded down to maxElementHeight. Whereabouts the items height sits between
 * minElementHeight..maxElementHeight is then used to determine where the transition line sits
 * between minTransitionArea..maxTransition area.
 *
 * If an item is smaller than or equal to minElementSize its transition line with be at
 * minTransitionArea and if it is larger than or equal to maxElementSize its transition line will be
 * at maxTransitionArea.
 *
 * For example, if we take the default values for minTransitionArea = 0.2f and maxTransitionArea =
 * 0.6f and minElementSize = 0.2f and maxElementSize= 0.8f then an item with a height of 0.4f (40%)
 * of the viewport height is one third of way between minElementSize and maxElementSize, (0.4f -
 * 0.2f) / (0.8f - 0.2f) = 0.33f. So its transition line would be one third of way between 0.2f and
 * 0.6f, transition line = 0.2f + (0.6f - 0.2f) * 0.33f = 0.33f.
 *
 * Once the position of the transition line is established we now have a transition area for the
 * item, e.g. in the example above the item will start/finish its transitions when it is 0.33f (33%)
 * of the distance from the edge of the viewport and will start/finish its transitions at the
 * viewport edge.
 *
 * The scaleInterpolator is used to determine how much of the scaling and alpha to apply as the item
 * transits through the transition area.
 *
 * The edge of the item furthest from the edge of the screen is used as a scaling trigger point for
 * each item.
 */
@Stable
@Deprecated(
    "Was moved to androidx.wear.compose.foundation.lazy package. " + "Please use it instead"
)
public interface ScalingParams {
    /**
     * What fraction of the full size of the item to scale it by when most scaled, e.g. at the edge
     * of the viewport. A value between [0f,1f], so a value of 0.2f means to scale an item to 20% of
     * its normal size.
     */
    @get:FloatRange(from = 0.0, to = 1.0) public val edgeScale: Float

    /**
     * What fraction of the full transparency of the item to draw it with when closest to the edge
     * of the screen. A value between [0f,1f], so a value of 0.2f means to set the alpha of an item
     * to 20% of its normal value.
     */
    @get:FloatRange(from = 0.0, to = 1.0) public val edgeAlpha: Float

    /**
     * The maximum element height as a ratio of the viewport size to use for determining the
     * transition point within ([minTransitionArea], [maxTransitionArea]) that a given content item
     * will start to be transitioned. Items larger than [maxElementHeight] will be treated as if
     * [maxElementHeight]. Must be greater than or equal to [minElementHeight].
     */
    @get:FloatRange(from = 0.0, to = 1.0) public val minElementHeight: Float

    /**
     * The maximum element height as a ratio of the viewport size to use for determining the
     * transition point within ([minTransitionArea], [maxTransitionArea]) that a given content item
     * will start to be transitioned. Items larger than [maxElementHeight] will be treated as if
     * [maxElementHeight]. Must be greater than or equal to [minElementHeight].
     */
    @get:FloatRange(from = 0.0, to = 1.0) public val maxElementHeight: Float

    /**
     * The lower bound of the transition line area, closest to the edge of the viewport. Defined as
     * a fraction (value between 0f..1f) of the distance between the viewport edges. Must be less
     * than or equal to [maxTransitionArea].
     *
     * For transition lines a value of 0f means that the transition line is at the viewport edge,
     * e.g. no transition, a value of 0.25f means that the transition line is 25% of the screen size
     * away from the viewport edge. A value of .5f or greater will place the transition line in the
     * other half of the screen to the edge that the item is transitioning towards.
     *
     * [minTransitionArea] and [maxTransitionArea] provide an area in which transition lines for all
     * list items exist. Depending on the size of the list item the specific point in the area is
     * calculated.
     */
    @get:FloatRange(from = 0.0, to = 1.0) public val minTransitionArea: Float

    /**
     * The upper bound of the transition line area, closest to the center of the viewport. The
     * fraction (value between 0f..1f) of the distance between the viewport edges. Must be greater
     * than or equal to [minTransitionArea].
     *
     * For transition lines a value of 0f means that the transition line is at the viewport edge,
     * e.g. no transition, a value of 0.25f means that the transition line is 25% of the screen size
     * away from the viewport edge. A value of .5f or greater will place the transition line in the
     * other half of the screen to the edge that the item is transitioning towards.
     *
     * [minTransitionArea] and [maxTransitionArea] provide an area in which transition lines for all
     * list items exist. Depending on the size of the list item the specific point in the area is
     * calculated.
     */
    @get:FloatRange(from = 0.0, to = 1.0) public val maxTransitionArea: Float

    /**
     * An interpolator to use to determine how to apply scaling as a item transitions across the
     * scaling transition area.
     */
    public val scaleInterpolator: Easing

    /**
     * The additional padding to consider above and below the viewport of a [ScalingLazyColumn] when
     * considering which items to draw in the viewport. If set to 0 then no additional padding will
     * be provided and only the items which would appear in the viewport before any scaling is
     * applied will be considered for drawing, this may leave blank space at the top and bottom of
     * the viewport where the next available item could have been drawn once other items have been
     * scaled down in size. The larger this value is set to will allow for more content items to be
     * considered for drawing in the viewport, however there is a performance cost associated with
     * materializing items that are subsequently not drawn. The higher/more extreme the scaling
     * parameters that are applied to the [ScalingLazyColumn] the more padding may be needed to
     * ensure there are always enough content items available to be rendered. By default will be 20%
     * of the maxHeight of the viewport above and below the content.
     *
     * @param viewportConstraints the viewports constraints
     */
    public fun resolveViewportVerticalOffset(viewportConstraints: Constraints): Int
}

@Stable
internal class DefaultScalingParams(
    override val edgeScale: Float,
    override val edgeAlpha: Float,
    override val minElementHeight: Float,
    override val maxElementHeight: Float,
    override val minTransitionArea: Float,
    override val maxTransitionArea: Float,
    override val scaleInterpolator: Easing,
    val viewportVerticalOffsetResolver: (Constraints) -> Int,
) : ScalingParams {

    init {
        check(minElementHeight <= maxElementHeight) {
            "minElementHeight must be less than or equal to maxElementHeight"
        }
        check(minTransitionArea <= maxTransitionArea) {
            "minTransitionArea must be less than or equal to maxTransitionArea"
        }
    }

    override fun resolveViewportVerticalOffset(viewportConstraints: Constraints): Int {
        return viewportVerticalOffsetResolver(viewportConstraints)
    }

    override fun toString(): String {
        return "ScalingParams(edgeScale=$edgeScale, edgeAlpha=$edgeAlpha, " +
            "minElementHeight=$minElementHeight, maxElementHeight=$maxElementHeight, " +
            "minTransitionArea=$minTransitionArea, maxTransitionArea=$maxTransitionArea)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null) return false
        if (this::class != other::class) return false

        other as DefaultScalingParams

        if (edgeScale != other.edgeScale) return false
        if (edgeAlpha != other.edgeAlpha) return false
        if (minElementHeight != other.minElementHeight) return false
        if (maxElementHeight != other.maxElementHeight) return false
        if (minTransitionArea != other.minTransitionArea) return false
        if (maxTransitionArea != other.maxTransitionArea) return false
        if (scaleInterpolator != other.scaleInterpolator) return false
        if (viewportVerticalOffsetResolver !== other.viewportVerticalOffsetResolver) return false

        return true
    }

    override fun hashCode(): Int {
        var result = edgeScale.hashCode()
        result = 31 * result + edgeAlpha.hashCode()
        result = 31 * result + minElementHeight.hashCode()
        result = 31 * result + maxElementHeight.hashCode()
        result = 31 * result + minTransitionArea.hashCode()
        result = 31 * result + maxTransitionArea.hashCode()
        result = 31 * result + scaleInterpolator.hashCode()
        result = 31 * result + viewportVerticalOffsetResolver.hashCode()
        return result
    }
}

/**
 * Calculate the scale and alpha to apply for an item based on the start and end position of the
 * component's viewport in pixels and top and bottom position of the item, also in pixels.
 *
 * Firstly work out if the component is above or below the viewport's center-line which determines
 * whether the item's top or bottom will be used as the trigger for scaling/alpha.
 *
 * Uses the scalingParams to determine where the scaling transition line is for the component.
 *
 * Then determines if the component is inside the scaling area, and if so what scaling/alpha effects
 * to apply.
 *
 * @param viewPortStartPx The start position of the component's viewport in pixels
 * @param viewPortEndPx The end position of the component's viewport in pixels
 * @param itemTopPx The top of the content item in pixels.
 * @param itemBottomPx The bottom of the content item in pixels.
 * @param scalingParams The parameters that determine where the item's scaling transition line is,
 *   how scaling and transparency to apply.
 */
internal fun calculateScaleAndAlpha(
    viewPortStartPx: Int,
    viewPortEndPx: Int,
    itemTopPx: Int,
    itemBottomPx: Int,
    scalingParams: ScalingParams,
): ScaleAndAlpha {
    var scaleToApply = 1.0f
    var alphaToApply = 1.0f

    val itemHeightPx = (itemBottomPx - itemTopPx).toFloat()
    val viewPortHeightPx = (viewPortEndPx - viewPortStartPx).toFloat()

    if (viewPortHeightPx > 0) {
        val itemEdgeDistanceFromViewPortEdge =
            min(viewPortEndPx - itemTopPx, itemBottomPx - viewPortStartPx)
        val itemEdgeAsFractionOfViewPort = itemEdgeDistanceFromViewPortEdge / viewPortHeightPx
        val heightAsFractionOfViewPort = itemHeightPx / viewPortHeightPx

        // Work out the scaling line based on size, this is a value between 0.0..1.0
        val sizeRatio =
            inverseLerp(
                scalingParams.minElementHeight,
                scalingParams.maxElementHeight,
                heightAsFractionOfViewPort,
            )

        val scalingLineAsFractionOfViewPort =
            lerp(scalingParams.minTransitionArea, scalingParams.maxTransitionArea, sizeRatio)

        if (itemEdgeAsFractionOfViewPort < scalingLineAsFractionOfViewPort) {
            // We are scaling
            val scalingProgressRaw =
                1f - itemEdgeAsFractionOfViewPort / scalingLineAsFractionOfViewPort
            val scalingInterpolated = scalingParams.scaleInterpolator.transform(scalingProgressRaw)

            scaleToApply = lerp(1.0f, scalingParams.edgeScale, scalingInterpolated)
            alphaToApply = lerp(1.0f, scalingParams.edgeAlpha, scalingInterpolated)
        }
    }
    return ScaleAndAlpha(scaleToApply, alphaToApply)
}

/**
 * Create a [ScalingLazyListItemInfo] given an unscaled start and end position for an item.
 *
 * @param itemStart the x-axis position of a list item. The x-axis position takes into account any
 *   adjustment to the original position based on the scaling of other list items.
 * @param item the original item info used to provide the pre-scaling position and size information
 *   for the item.
 * @param verticalAdjustment the amount of vertical adjustment to apply to item positions to allow
 *   for content padding in order to determine the adjusted position of the item within the viewport
 *   in order to correctly calculate the scaling to apply.
 * @param viewportHeightPx the height of the viewport in pixels
 * @param viewportCenterLinePx the center line of the viewport in pixels
 * @param scalingParams the scaling params to use for determining the scaled size of the item
 * @param beforeContentPaddingPx the number of pixels of padding before the first item
 * @param anchorType the type of pivot to use for the center item when calculating position and
 *   offset
 * @param visible a flag to determine whether the list items should be visible or transparent. Items
 *   are normally visible, but can be drawn transparently when the list is not yet initialized,
 *   unless we are in preview (LocalInspectionModel) mode.
 */
internal fun calculateItemInfo(
    itemStart: Int,
    item: LazyListItemInfo,
    verticalAdjustment: Int,
    viewportHeightPx: Int,
    viewportCenterLinePx: Int,
    scalingParams: ScalingParams,
    beforeContentPaddingPx: Int,
    anchorType: ScalingLazyListAnchorType,
    autoCentering: AutoCenteringParams?,
    visible: Boolean,
): ScalingLazyListItemInfo {
    val adjustedItemStart = itemStart - verticalAdjustment
    val adjustedItemEnd = itemStart + item.size - verticalAdjustment

    val scaleAndAlpha =
        calculateScaleAndAlpha(
            viewPortStartPx = 0,
            viewPortEndPx = viewportHeightPx,
            itemTopPx = adjustedItemStart,
            itemBottomPx = adjustedItemEnd,
            scalingParams = scalingParams,
        )

    val isAboveLine = (adjustedItemEnd + adjustedItemStart) < viewportHeightPx
    val scaledHeight = (item.size * scaleAndAlpha.scale).roundToInt()
    val scaledItemTop =
        if (!isAboveLine) {
            itemStart
        } else {
            itemStart + item.size - scaledHeight
        }

    val offset =
        convertToCenterOffset(
            anchorType = anchorType,
            itemScrollOffset = scaledItemTop,
            viewportCenterLinePx = viewportCenterLinePx,
            beforeContentPaddingInPx = beforeContentPaddingPx,
            itemSizeInPx = scaledHeight,
        )
    val unadjustedOffset =
        convertToCenterOffset(
            anchorType = anchorType,
            itemScrollOffset = item.offset,
            viewportCenterLinePx = viewportCenterLinePx,
            beforeContentPaddingInPx = beforeContentPaddingPx,
            itemSizeInPx = item.size,
        )
    return DefaultScalingLazyListItemInfo(
        // Adjust index to take into account the Spacer before the first list item
        index = if (autoCentering != null) item.index - 1 else item.index,
        key = item.key,
        unadjustedOffset = unadjustedOffset,
        offset = offset,
        size = scaledHeight,
        scale = scaleAndAlpha.scale,
        alpha = if (visible) scaleAndAlpha.alpha else 0f,
        unadjustedSize = item.size,
    )
}

internal class DefaultScalingLazyListLayoutInfo(
    internal val internalVisibleItemsInfo: List<ScalingLazyListItemInfo>,
    override val viewportStartOffset: Int,
    override val viewportEndOffset: Int,
    override val totalItemsCount: Int,
    val centerItemIndex: Int,
    val centerItemScrollOffset: Int,
    override val reverseLayout: Boolean,
    override val orientation: Orientation,
    override val viewportSize: IntSize,
    override val beforeContentPadding: Int,
    override val afterContentPadding: Int,
    override val beforeAutoCenteringPadding: Int,
    override val afterAutoCenteringPadding: Int,
    // Flag to indicate that we are ready to handle scrolling as the list becomes visible. This is
    // used to either move to the initialCenterItemIndex/Offset or complete any
    // scrollTo/animatedScrollTo calls that were incomplete due to the component not being visible.
    internal val readyForInitialScroll: Boolean,
    // Flag to indicate that initialization is complete and initial scroll index and offset have
    // been set.
    internal val initialized: Boolean,
) : ScalingLazyListLayoutInfo {
    override val visibleItemsInfo: List<ScalingLazyListItemInfo>
        // Do not report visible items until initialization is complete and the items are
        // actually visible and correctly positioned.
        get() = if (initialized) internalVisibleItemsInfo else emptyList()
}

internal class DefaultScalingLazyListItemInfo(
    override val index: Int,
    override val key: Any,
    override val unadjustedOffset: Int,
    override val offset: Int,
    override val size: Int,
    override val scale: Float,
    override val alpha: Float,
    override val unadjustedSize: Int,
) : ScalingLazyListItemInfo {
    override fun toString(): String {
        return "DefaultScalingLazyListItemInfo(index=$index, key=$key, " +
            "unadjustedOffset=$unadjustedOffset, offset=$offset, size=$size, " +
            "unadjustedSize=$unadjustedSize, scale=$scale, alpha=$alpha)"
    }
}

/**
 * Calculate the offset from the viewport center line of the Start|Center of an items unadjusted or
 * scaled size. The for items with an height that is an odd number and that have
 * ScalingLazyListAnchorType.Center the offset will be rounded down. E.g. An item which is 19 pixels
 * in height will have a center offset of 9 pixes.
 *
 * @param anchorType the anchor type of the ScalingLazyColumn
 * @param itemScrollOffset the LazyListItemInfo offset of the item
 * @param viewportCenterLinePx the value to use as the center line of the viewport
 * @param beforeContentPaddingInPx any content padding that has been applied before the contents
 * @param itemSizeInPx the size of the item
 */
internal fun convertToCenterOffset(
    anchorType: ScalingLazyListAnchorType,
    itemScrollOffset: Int,
    viewportCenterLinePx: Int,
    beforeContentPaddingInPx: Int,
    itemSizeInPx: Int,
): Int {
    return itemScrollOffset - viewportCenterLinePx +
        beforeContentPaddingInPx +
        if (anchorType == ScalingLazyListAnchorType.ItemStart) {
            0
        } else {
            itemSizeInPx / 2
        }
}

/** Find the start offset of the list item w.r.t. the */
internal fun ScalingLazyListItemInfo.startOffset(anchorType: ScalingLazyListAnchorType) =
    offset -
        if (anchorType == ScalingLazyListAnchorType.ItemCenter) {
            (size / 2f)
        } else {
            0f
        }

/**
 * Find the start position of the list item from its unadjusted offset w.r.t. the ScalingLazyColumn
 * center of viewport offset = 0 coordinate model.
 */
internal fun ScalingLazyListItemInfo.unadjustedStartOffset(anchorType: ScalingLazyListAnchorType) =
    unadjustedOffset -
        if (anchorType == ScalingLazyListAnchorType.ItemCenter) {
            (unadjustedSize / 2f)
        } else {
            0f
        }
