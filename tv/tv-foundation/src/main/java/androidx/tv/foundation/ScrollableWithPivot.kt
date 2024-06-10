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

package androidx.tv.foundation

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.BringIntoViewSpec
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.debugInspectorInfo
import androidx.compose.ui.platform.inspectable
import kotlin.math.abs

/* Copied from
compose/foundation/foundation/src/commonMain/kotlin/androidx/compose/foundation/gestures/
Scrollable.kt and modified */

/**
 * Configure touch scrolling and flinging for the UI element in a single [Orientation].
 *
 * Users should update their state themselves using default [ScrollableState] and its
 * `consumeScrollDelta` callback or by implementing [ScrollableState] interface manually and reflect
 * their own state in UI when using this component.
 *
 * @param state [ScrollableState] state of the scrollable. Defines how scroll events will be
 *   interpreted by the user land logic and contains useful information about on-going events.
 * @param orientation orientation of the scrolling
 * @param pivotOffsets offsets of child element within the parent and starting edge of the child
 *   from the pivot defined by the parentOffset.
 * @param enabled whether or not scrolling in enabled
 * @param reverseDirection reverse the direction of the scroll, so top to bottom scroll will behave
 *   like bottom to top and left to right will behave like right to left. drag events when this
 *   scrollable is being dragged.
 */
@Deprecated(
    "scrollableWithPivot has been deprecated. Construct a custom bringIntoViewSpec to scroll with an offset. To learn how you can control offset during scrolling, refer PivotBringIntoViewSpec: https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:compose/foundation/foundation/src/androidMain/kotlin/androidx/compose/foundation/gestures/BringIntoViewSpec.android.kt;l=48;drc=dcaa116fbfda77e64a319e1668056ce3b032469f",
    replaceWith =
        ReplaceWith(
            "scrollable(" +
                "state = state, " +
                "orientation = orientation, " +
                "enabled = enabled, " +
                "reverseDirection = reverseDirection" +
                ")"
        )
)
@OptIn(ExperimentalFoundationApi::class)
@ExperimentalTvFoundationApi
@Suppress("DEPRECATION")
fun Modifier.scrollableWithPivot(
    state: ScrollableState,
    orientation: Orientation,
    pivotOffsets: PivotOffsets,
    enabled: Boolean = true,
    reverseDirection: Boolean = false
): Modifier =
    this then
        Modifier.inspectable(
            debugInspectorInfo {
                name = "scrollableWithPivot"
                properties["orientation"] = orientation
                properties["state"] = state
                properties["enabled"] = enabled
                properties["reverseDirection"] = reverseDirection
                properties["pivotOffsets"] = pivotOffsets
            }
        ) {
            Modifier.scrollable(
                state = state,
                orientation = orientation,
                enabled = enabled,
                reverseDirection = reverseDirection,
                overscrollEffect = null,
                bringIntoViewSpec = TvBringIntoViewSpec(pivotOffsets, enabled)
            )
        }

@OptIn(ExperimentalFoundationApi::class)
private class TvBringIntoViewSpec(val pivotOffsets: PivotOffsets, val userScrollEnabled: Boolean) :
    BringIntoViewSpec {

    override fun calculateScrollDistance(offset: Float, size: Float, containerSize: Float): Float {
        if (!userScrollEnabled) return 0f
        val leadingEdgeOfItemRequestingFocus = offset
        val trailingEdgeOfItemRequestingFocus = offset + size

        val sizeOfItemRequestingFocus =
            abs(trailingEdgeOfItemRequestingFocus - leadingEdgeOfItemRequestingFocus)
        val childSmallerThanParent = sizeOfItemRequestingFocus <= containerSize
        val initialTargetForLeadingEdge =
            pivotOffsets.parentFraction * containerSize -
                (pivotOffsets.childFraction * sizeOfItemRequestingFocus)
        val spaceAvailableToShowItem = containerSize - initialTargetForLeadingEdge

        val targetForLeadingEdge =
            if (childSmallerThanParent && spaceAvailableToShowItem < sizeOfItemRequestingFocus) {
                containerSize - sizeOfItemRequestingFocus
            } else {
                initialTargetForLeadingEdge
            }

        return leadingEdgeOfItemRequestingFocus - targetForLeadingEdge
    }

    override fun hashCode(): Int {
        var result = pivotOffsets.hashCode()
        result = 31 * result + userScrollEnabled.hashCode()
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (other !is TvBringIntoViewSpec) return false
        return pivotOffsets == other.pivotOffsets && userScrollEnabled == other.userScrollEnabled
    }
}
