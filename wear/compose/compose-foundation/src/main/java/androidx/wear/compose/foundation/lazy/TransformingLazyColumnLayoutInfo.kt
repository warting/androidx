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

import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.util.packFloats
import androidx.compose.ui.util.unpackFloat1
import androidx.compose.ui.util.unpackFloat2

/**
 * Scroll progress of an item in a [TransformingLazyColumn] before any modifications to the item's
 * height are applied (using [TransformingLazyColumnItemScope.transformedHeight] modifier).
 */
@JvmInline
public value class TransformingLazyColumnItemScrollProgress
internal constructor(private val packedValue: Long) {
    /**
     * The top offset (between the top of the list container and the top of the item) as a fraction
     * of the height of the list container. Is within (0, 1) when item is inside the screen and
     * could be negative if the top of the item is off the screen. Value is calculated from the top
     * of the container. This value is calculated before any height modifications are applied (using
     * [TransformingLazyColumnItemScope.transformedHeight] modifier). This returns [Float.NaN] if
     * the progress was [Unspecified].
     */
    public val topOffsetFraction: Float
        get() = unpackFloat1(packedValue)

    /**
     * The bottom offset (between the top of the list container and the bottom of the item) as a
     * fraction of the height of the list container. Is within (0, 1) when item is inside the screen
     * and could exceed 1 when the bottom of item is off the screen. Value is calculated from the
     * top of the container. This value is calculated before any height modifications are applied
     * (using [TransformingLazyColumnItemScope.transformedHeight] modifier). This returns
     * [Float.NaN] if the progress was [Unspecified].
     */
    public val bottomOffsetFraction: Float
        get() = unpackFloat2(packedValue)

    /** `true` when this is [TransformingLazyColumnItemScrollProgress.Unspecified]. */
    public val isUnspecified: Boolean
        get() = packedValue == UnspecifiedPackedFloats

    /** `false` when this is [TransformingLazyColumnItemScrollProgress.Unspecified]. */
    public val isSpecified: Boolean
        get() = packedValue != UnspecifiedPackedFloats

    /**
     * Constructs a [TransformingLazyColumnItemScrollProgress] with two offset fraction [Float]
     * values.
     *
     * @param topOffsetFraction The top offset (between the top of the list container and the top of
     *   the item) as a fraction of the height of the list container.
     * @param bottomOffsetFraction The bottom offset (between the top of the list container and the
     *   bottom of the item) as a fraction of the height of the list container.
     */
    public constructor(
        topOffsetFraction: Float,
        bottomOffsetFraction: Float,
    ) : this(packFloats(topOffsetFraction, bottomOffsetFraction))

    public companion object {
        /**
         * Represents an unspecified [TransformingLazyColumnItemScrollProgress] value, usually a
         * replacement for `null` when a primitive value is desired.
         */
        public val Unspecified: TransformingLazyColumnItemScrollProgress =
            TransformingLazyColumnItemScrollProgress(UnspecifiedPackedFloats)

        internal fun downwardMeasuredItemScrollProgress(
            offset: Int,
            height: Int,
            containerHeight: Int,
        ): TransformingLazyColumnItemScrollProgress =
            TransformingLazyColumnItemScrollProgress(
                topOffsetFraction = offset.toFloat() / containerHeight.toFloat(),
                bottomOffsetFraction = (offset + height).toFloat() / containerHeight.toFloat(),
            )

        internal fun upwardMeasuredItemScrollProgress(
            offset: Int,
            height: Int,
            containerHeight: Int,
        ): TransformingLazyColumnItemScrollProgress =
            TransformingLazyColumnItemScrollProgress(
                topOffsetFraction = (offset - height).toFloat() / containerHeight.toFloat(),
                bottomOffsetFraction = offset / containerHeight.toFloat(),
            )
    }
}

/** Represents an item that is visible in the [TransformingLazyColumn] component. */
public sealed interface TransformingLazyColumnVisibleItemInfo {
    /** The index of the item in the underlying data source. */
    public val index: Int

    /** The offset of the item from the start of the visible area. */
    public val offset: Int

    /** The height of the item after applying height transformation. */
    public val transformedHeight: Int

    /**
     * The height of the item returned during the measurement phase and before height transformation
     * is applied.
     */
    public val measuredHeight: Int

    /** The scroll progress of the item, indicating its position within the visible area. */
    public val scrollProgress: TransformingLazyColumnItemScrollProgress

    /** The key of the item which was passed to the item() or items() function. */
    public val key: Any

    /** The content type of the item which was passed to the item() or items() function. */
    public val contentType: Any?
}

/** Holds the layout information for a [TransformingLazyColumn]. */
public sealed interface TransformingLazyColumnLayoutInfo {
    /**
     * A list of [TransformingLazyColumnVisibleItemInfo] objects representing the visible items in
     * the list.
     */
    public val visibleItems: List<TransformingLazyColumnVisibleItemInfo>

    /** The total count of items passed to [TransformingLazyColumn]. */
    public val totalItemsCount: Int

    /** The size of the viewport in pixels. */
    public val viewportSize: IntSize

    /**
     * The content padding in pixels applied before the first item in the direction of scrolling.
     */
    public val beforeContentPadding: Int

    /** The content padding in pixels applied after the last item in the direction of scrolling. */
    public val afterContentPadding: Int
}

internal const val UnspecifiedPackedFloats = 0x7fc00000_7fc00000L // NaN_NaN
