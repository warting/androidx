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

package androidx.window.core.layout

import kotlin.jvm.JvmField
import kotlin.jvm.JvmStatic

/**
 * [WindowSizeClass] represents breakpoints for a viewport. Designers should design around the
 * different combinations of width and height buckets. Developers should use the different buckets
 * to specify the layouts. Ideally apps will work well in each bucket and by extension work well
 * across multiple devices. If two devices are in similar buckets they should behave similarly.
 *
 * This class is meant to be a common definition that can be shared across different device types.
 * Application developers can use [WindowSizeClass] to have standard window buckets and design the
 * UI around those buckets. Library developers can use these buckets to create different UI with
 * respect to each bucket. This will help with consistency across multiple device types.
 *
 * A library developer use-case can be creating some navigation UI library. For a size class with
 * the [WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND] width it might be more reasonable to have a
 * side navigation.
 *
 * An application use-case can be applied for apps that use a list-detail pattern. The app can use
 * the [WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND] to determine if there is enough space to show
 * the list and the detail side by side. If all apps follow this guidance then it will present a
 * very consistent user experience.
 *
 * In some cases developers or UI systems may decide to create their own break points. A developer
 * might optimize for a window that is smaller than the supported break points or larger. A UI
 * system might find that some break points are better suited than the recommended break points. In
 * these cases developers may wish to specify their own custom break points and match using a `when`
 * statement.
 *
 * To process a [WindowSizeClass] use the methods [isAtLeastBreakpoint], [isWidthAtLeastBreakpoint],
 * [isHeightAtLeastBreakpoint] methods. Note these methods are order dependent as the smaller
 * [minWidthDp] and [minHeightDp] would match all the breakpoints that are larger. Therefore when
 * processing the selection should normally be ordered from larger to smaller breakpoints.
 *
 * @see WindowWidthSizeClass
 * @see WindowHeightSizeClass
 */
class WindowSizeClass(
    /** Returns the lower bound for the width of the size class in dp. */
    val minWidthDp: Int,
    /** Returns the lower bound for the height of the size class in dp. */
    val minHeightDp: Int,
) {

    /** A convenience constructor that will truncate to ints. */
    constructor(widthDp: Float, heightDp: Float) : this(widthDp.toInt(), heightDp.toInt())

    init {
        require(minWidthDp >= 0) {
            "Expected minWidthDp to be at least 0, minWidthDp: $minWidthDp."
        }
        require(minHeightDp >= 0) {
            "Expected minHeightDp to be at least 0, minHeightDp: $minHeightDp."
        }
    }

    @Suppress("DEPRECATION")
    @Deprecated(
        "Use either isWidthAtLeastBreakpoint or isAtLeastBreakpoint to check matching bounds."
    )
    /** Returns the [WindowWidthSizeClass] that corresponds to the widthDp of the window. */
    val windowWidthSizeClass: WindowWidthSizeClass
        get() = WindowWidthSizeClass.compute(minWidthDp.toFloat())

    @Suppress("DEPRECATION")
    @Deprecated(
        "Use either isHeightAtLeastBreakpoint or isAtLeastBreakpoint to check matching bounds."
    )
    /** Returns the [WindowHeightSizeClass] that corresponds to the heightDp of the window. */
    val windowHeightSizeClass: WindowHeightSizeClass
        get() = WindowHeightSizeClass.compute(minHeightDp.toFloat())

    /**
     * Returns `true` when [minWidthDp] is greater than or equal to [widthDpBreakpoint], `false`
     * otherwise. When processing a [WindowSizeClass] note that this method is order dependent.
     * Selection should go from largest to smallest breakpoints.
     *
     * @sample androidx.window.core.samples.layout.processWindowSizeClassWidthOnly
     */
    fun isWidthAtLeastBreakpoint(widthDpBreakpoint: Int): Boolean {
        return minWidthDp >= widthDpBreakpoint
    }

    /**
     * Returns `true` when [minHeightDp] is greater than or equal to [heightDpBreakpoint], `false`
     * otherwise. When processing a [WindowSizeClass] note that this method is order dependent.
     * Selection should go from largest to smallest breakpoints.
     */
    fun isHeightAtLeastBreakpoint(heightDpBreakpoint: Int): Boolean {
        return minHeightDp >= heightDpBreakpoint
    }

    /**
     * Returns `true` when [minWidthDp] is greater than or equal to [widthDpBreakpoint] and
     * [minHeightDp] is greater than or equal to [heightDpBreakpoint], `false` otherwise. When
     * processing a [WindowSizeClass] note that this method is order dependent. Selection should go
     * from largest to smallest breakpoints.
     */
    fun isAtLeastBreakpoint(widthDpBreakpoint: Int, heightDpBreakpoint: Int): Boolean {
        return isWidthAtLeastBreakpoint(widthDpBreakpoint) &&
            isHeightAtLeastBreakpoint(heightDpBreakpoint)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as WindowSizeClass

        if (minWidthDp != other.minWidthDp) return false
        if (minHeightDp != other.minHeightDp) return false

        return true
    }

    override fun hashCode(): Int {
        var result = minWidthDp
        result = 31 * result + minHeightDp
        return result
    }

    override fun toString(): String {
        return "WindowSizeClass(minWidthDp=$minWidthDp, minHeightDp=$minHeightDp)"
    }

    companion object {
        /** A lower bound for a size class with Medium width in dp. */
        const val WIDTH_DP_MEDIUM_LOWER_BOUND = 600

        /** A lower bound for a size class with Expanded width in dp. */
        const val WIDTH_DP_EXPANDED_LOWER_BOUND = 840

        /** A lower bound for a size class with Large width in dp. */
        const val WIDTH_DP_LARGE_LOWER_BOUND = 1200

        /** A lower bound for a size class width Extra Large width in dp. */
        const val WIDTH_DP_EXTRA_LARGE_LOWER_BOUND = 1600

        /** A lower bound for a size class with Medium height in dp. */
        const val HEIGHT_DP_MEDIUM_LOWER_BOUND = 480

        /** A lower bound for a size class with Expanded height in dp. */
        const val HEIGHT_DP_EXPANDED_LOWER_BOUND = 900

        private val WIDTH_DP_BREAKPOINTS_V1 =
            listOf(0, WIDTH_DP_MEDIUM_LOWER_BOUND, WIDTH_DP_EXPANDED_LOWER_BOUND)

        private val WIDTH_DP_BREAKPOINTS_V2 =
            WIDTH_DP_BREAKPOINTS_V1 +
                listOf(WIDTH_DP_LARGE_LOWER_BOUND, WIDTH_DP_EXTRA_LARGE_LOWER_BOUND)

        private val HEIGHT_DP_BREAKPOINTS_V1 =
            listOf(0, HEIGHT_DP_MEDIUM_LOWER_BOUND, HEIGHT_DP_EXPANDED_LOWER_BOUND)

        private val HEIGHT_DP_BREAKPOINTS_V2 = HEIGHT_DP_BREAKPOINTS_V1

        private fun createBreakpointSet(
            widthBreakpoints: List<Int>,
            heightBreakpoints: List<Int>,
        ): Set<WindowSizeClass> {
            return widthBreakpoints
                .flatMap { widthBp ->
                    heightBreakpoints.map { heightBp ->
                        WindowSizeClass(minWidthDp = widthBp, minHeightDp = heightBp)
                    }
                }
                .toSet()
        }

        /**
         * The recommended breakpoints for window size classes.
         *
         * @sample androidx.window.core.samples.layout.calculateWindowSizeClass
         */
        @JvmField
        val BREAKPOINTS_V1 = createBreakpointSet(WIDTH_DP_BREAKPOINTS_V1, HEIGHT_DP_BREAKPOINTS_V1)

        /**
         * The recommended breakpoints for window size classes. This includes all the breakpoints
         * from [BREAKPOINTS_V1] plus new breakpoints to account for the Large and Extra Large width
         * breakpoints.
         *
         * @sample androidx.window.core.samples.layout.calculateWindowSizeClass
         */
        @JvmField
        val BREAKPOINTS_V2 = createBreakpointSet(WIDTH_DP_BREAKPOINTS_V2, HEIGHT_DP_BREAKPOINTS_V2)

        /**
         * Computes the recommended [WindowSizeClass] for the given width and height in DP.
         *
         * @param dpWidth width of a window in DP.
         * @param dpHeight height of a window in DP.
         * @return [WindowSizeClass] that is recommended for the given dimensions.
         * @throws IllegalArgumentException if [dpWidth] or [dpHeight] is negative.
         */
        @JvmStatic
        @Deprecated(
            "Use computeWindowSizeClass instead.",
            ReplaceWith(
                "BREAKPOINTS_V1.computeWindowSizeClass(widthDp = dpWidth, heightDp = dpHeight)",
                "androidx.window.core.layout.computeWindowSizeClass",
            ),
        )
        fun compute(dpWidth: Float, dpHeight: Float): WindowSizeClass {
            val widthDp =
                when {
                    dpWidth >= WIDTH_DP_EXPANDED_LOWER_BOUND -> WIDTH_DP_EXPANDED_LOWER_BOUND
                    dpWidth >= WIDTH_DP_MEDIUM_LOWER_BOUND -> WIDTH_DP_MEDIUM_LOWER_BOUND
                    else -> 0
                }
            val heightDp =
                when {
                    dpHeight >= HEIGHT_DP_EXPANDED_LOWER_BOUND -> HEIGHT_DP_EXPANDED_LOWER_BOUND
                    dpHeight >= HEIGHT_DP_MEDIUM_LOWER_BOUND -> HEIGHT_DP_MEDIUM_LOWER_BOUND
                    else -> 0
                }
            return WindowSizeClass(widthDp, heightDp)
        }
    }
}
