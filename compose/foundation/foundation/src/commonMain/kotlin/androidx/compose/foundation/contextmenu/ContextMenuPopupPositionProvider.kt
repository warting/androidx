/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.compose.foundation.contextmenu

import androidx.annotation.VisibleForTesting
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider

/**
 * A [PopupPositionProvider] which places the context menu fully within the window, aligning a
 * corner of the popup menu with the cursor. For each x/y dimension it will:
 * * In cases where the window length is too small for the popup length, it aligns the start edges
 *   of the popup and the window.
 * * In cases where there is enough length between the click position and the end of the window for
 *   entire popup length, it will align the start edge of the popup with the result of the
 *   [anchorPositionBlock].
 * * In cases where there is not enough length between the click position and the end of the window
 *   for entire popup length, but there is between the click position and the start of the window,
 *   it will align the end edge of the popup with the result of the [anchorPositionBlock].
 * * In the final case: the window length is wide enough for the popup, but there isn't enough
 *   length on either side of the result of the [anchorPositionBlock] to fit an edge of the popup
 *   with the resulting position. It will align the end edges of the popup and window.
 *
 * @param anchorPositionBlock A function that provides the [IntOffset] to align to. This should be
 *   in the same coordinates that the [Popup] is anchored to. In the case of a right-click context
 *   menu, this would be the position of the right-click.
 * @param onPositionCalculated A snapshot-aware callback that is invoked after the position is
 *   calculated. It provides the result of the [anchorPositionBlock] in its first parameter and the
 *   bounds of the resulting popup in its second parameter. Changes to state read during this lambda
 *   will result in the popup re-calculating its position. This callback is useful for situations
 *   where you need to vary your popup based on the resulting position of the popup.
 */
// TODO(b/332764757) Consider making public.
internal class ContextMenuPopupPositionProvider(
    private val anchorPositionBlock: () -> IntOffset,
    private val onPositionCalculated: ((position: IntOffset, menuBounds: IntRect) -> Unit)? = null,
) : PopupPositionProvider {
    constructor(
        anchorPosition: IntOffset,
        onPositionCalculated: ((position: IntOffset, menuBounds: IntRect) -> Unit)? = null,
    ) : this({ anchorPosition }, onPositionCalculated)

    // TODO(b/256233441) anchorBounds should be positioned within the window that
    //  windowSize is derived from. However, it seems that windowSize's
    //  bounds do not include the top decoration, while the window anchorBounds
    //  is derived from does include the top decoration. This causes the
    //  resulting calculation to be off when approaching the bottom of the screen.

    // Note: This is snapshot aware.
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset {
        val anchorPosition = anchorPositionBlock()
        val resultPosition =
            IntOffset(
                x =
                    alignPopupAxis(
                        position = anchorBounds.left + anchorPosition.x,
                        popupLength = popupContentSize.width,
                        windowLength = windowSize.width,
                        closeAffinity = layoutDirection == LayoutDirection.Ltr,
                    ),
                y =
                    alignPopupAxis(
                        position = anchorBounds.top + anchorPosition.y,
                        popupLength = popupContentSize.height,
                        windowLength = windowSize.height,
                    ),
            )
        onPositionCalculated?.invoke(anchorPosition, IntRect(resultPosition, popupContentSize))
        return resultPosition
    }
}

/**
 * Align the popup to the position along an axis.
 *
 * @param position The position to align to along the window's axis
 * @param popupLength The length of the popup along this axis
 * @param windowLength The length of the window along this axis
 * @param closeAffinity Whether the start side is the close edge (`0`) or the far edge
 *   ([windowLength])
 * @return the coordinate along this axis that best aligns the popup to the position
 */
@VisibleForTesting
internal fun alignPopupAxis(
    position: Int,
    popupLength: Int,
    windowLength: Int,
    closeAffinity: Boolean = true,
): Int =
    when {
        popupLength >= windowLength -> alignStartEdges(popupLength, windowLength, closeAffinity)
        popupFitsBetweenPositionAndEndEdge(position, popupLength, windowLength, closeAffinity) ->
            alignPopupStartEdgeToPosition(position, popupLength, closeAffinity)
        popupFitsBetweenPositionAndStartEdge(position, popupLength, windowLength, closeAffinity) ->
            alignPopupEndEdgeToPosition(position, popupLength, closeAffinity)
        else -> alignEndEdges(popupLength, windowLength, closeAffinity)
    }

private fun popupFitsBetweenPositionAndStartEdge(
    position: Int,
    popupLength: Int,
    windowLength: Int,
    closeAffinity: Boolean,
): Boolean =
    if (closeAffinity) {
        popupLength <= position
    } else {
        windowLength - popupLength > position
    }

private fun popupFitsBetweenPositionAndEndEdge(
    position: Int,
    popupLength: Int,
    windowLength: Int,
    closeAffinity: Boolean,
): Boolean =
    popupFitsBetweenPositionAndStartEdge(position, popupLength, windowLength, !closeAffinity)

private fun alignPopupStartEdgeToPosition(
    position: Int,
    popupLength: Int,
    closeAffinity: Boolean,
): Int = if (closeAffinity) position else position - popupLength

private fun alignPopupEndEdgeToPosition(
    position: Int,
    popupLength: Int,
    closeAffinity: Boolean,
): Int = alignPopupStartEdgeToPosition(position, popupLength, !closeAffinity)

private fun alignStartEdges(popupLength: Int, windowLength: Int, closeAffinity: Boolean): Int =
    if (closeAffinity) 0 else windowLength - popupLength

private fun alignEndEdges(popupLength: Int, windowLength: Int, closeAffinity: Boolean): Int =
    alignStartEdges(popupLength, windowLength, !closeAffinity)
