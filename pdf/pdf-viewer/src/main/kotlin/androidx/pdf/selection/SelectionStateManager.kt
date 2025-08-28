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

package androidx.pdf.selection

import android.graphics.Point
import android.graphics.PointF
import android.graphics.RectF
import android.os.DeadObjectException
import android.util.SparseArray
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import androidx.annotation.IntDef
import androidx.annotation.VisibleForTesting
import androidx.core.util.forEach
import androidx.pdf.PdfDocument
import androidx.pdf.PdfPoint
import androidx.pdf.content.PageSelection
import androidx.pdf.exceptions.RequestFailedException
import androidx.pdf.exceptions.RequestMetadata
import androidx.pdf.util.CONTENT_SELECTION_REQUEST_NAME
import androidx.pdf.view.PageMetadataLoader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Owns and updates all mutable state related to content selection in [PdfView] */
internal class SelectionStateManager(
    private val pdfDocument: PdfDocument,
    private val backgroundScope: CoroutineScope,
    private val handleTouchTargetSizePx: Int,
    private val errorFlow: MutableSharedFlow<Throwable>,
    private val pageMetadataLoader: PageMetadataLoader?,
    initialSelection: SelectionModel? = null,
) {
    /** The current [Selection] */
    @VisibleForTesting val _selectionModel = MutableStateFlow<SelectionModel?>(initialSelection)

    val selectionModel: StateFlow<SelectionModel?>
        get() = _selectionModel

    /** Replay at few values in case of an UI signal issued while [PdfView] is not collecting */
    private val _selectionUiSignalBus = MutableSharedFlow<SelectionUiSignal>(replay = 3)

    /**
     * This [SharedFlow] serves as an event bus of sorts to signal our host [PdfView] to update its
     * UI in a decoupled way
     */
    val selectionUiSignalBus: SharedFlow<SelectionUiSignal>
        get() = _selectionUiSignalBus

    private var setSelectionJob: Job? = null

    private var draggingState: DraggingState? = null

    /**
     * Potentially updates the location of a drag handle given the [action] and [location] of a
     * [MotionEvent] within the [androidx.pdf.view.PdfView]. If a drag handle is moved, the current
     * selection is updated asynchronously.
     *
     * @param currentZoom is used only to scale the size of the drag handle's touch target based on
     *   the zoom factor
     */
    fun maybeDragSelection(
        action: Int,
        location: PdfPoint?,
        currentZoom: Float,
        isSourceMouse: Boolean,
    ): Boolean {
        return when (action) {
            MotionEvent.ACTION_DOWN -> {
                location ?: return false // We can't handle an ACTION_DOWN without a location
                if (isSourceMouse) handleActionDownForMouse(location, currentZoom)
                else maybeHandleActionDown(location, currentZoom)
            }
            MotionEvent.ACTION_MOVE -> {
                maybeHandleActionMove(location)
            }
            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_CANCEL -> maybeHandleGestureEnd()
            else -> false
        }
    }

    /** Asynchronously attempts to select the nearest block of text to [pdfPoint] */
    fun maybeSelectWordAtPoint(pdfPoint: PdfPoint) {
        _selectionUiSignalBus.tryEmit(SelectionUiSignal.ToggleActionMode(show = false))
        _selectionUiSignalBus.tryEmit(
            SelectionUiSignal.PlayHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        )
        updateRangeSelectionAsync(pdfPoint, pdfPoint)
    }

    /** Synchronously resets all state of this manager */
    fun clearSelection() {
        draggingState = null
        setSelectionJob?.cancel()
        setSelectionJob = null
        _selectionUiSignalBus.tryEmit(SelectionUiSignal.ToggleActionMode(show = false))
        _selectionUiSignalBus.tryEmit(SelectionUiSignal.Invalidate)
        // tryEmit will always succeed for StateFlow
        _selectionModel.tryEmit(null)
    }

    fun maybeShowActionMode() {
        if (selectionModel.value != null) {
            _selectionUiSignalBus.tryEmit(SelectionUiSignal.ToggleActionMode(show = true))
        }
    }

    fun maybeHideActionMode() {
        _selectionUiSignalBus.tryEmit(SelectionUiSignal.ToggleActionMode(show = false))
    }

    /** Updates the selection to include all text on the 0-indexed [pageNum]. */
    // TODO(b/386398335) Update this to accept a range of pages for select all, once we support
    // multi-page selections
    // TODO(b/386417152) Update this to use index-based selection once that's supported by
    // PdfDocument
    fun selectAllTextOnPageAsync(pageNum: Int) {
        updateAllSelectionAsync(pageNum)
    }

    /**
     * Calculates the touch target bounding boxes for the start and end selection handles, scaled by
     * the [currentZoom].
     *
     * @param currentZoom The current zoom level.
     * @return A [Pair] of [RectF] for the start and end handle bounds, or `null` if no selection.
     */
    fun getSelectionHandleBounds(currentZoom: Float): Pair<RectF, RectF>? {
        val currentSelection = selectionModel.value ?: return null

        // TODO(b/441196273): Drag handles for modifying a selection are currently only supported
        // for selections that consist exclusively of text. For mixed-content or non-text
        // selections, the handles are disabled as this functionality is not yet supported.
        if (currentSelection.documentSelection.containsOnlyTextSelections) {
            val touchTargetContentSize = handleTouchTargetSizePx / currentZoom

            val start = currentSelection.startBoundary.location
            val startTarget =
                RectF(
                    start.x - touchTargetContentSize,
                    start.y,
                    start.x,
                    start.y + touchTargetContentSize,
                )

            val end = currentSelection.endBoundary.location
            val endTarget =
                RectF(end.x, end.y, end.x + touchTargetContentSize, end.y + touchTargetContentSize)

            return Pair(startTarget, endTarget)
        }
        return null
    }

    @HandlePositionDef
    fun getHandleForTouchPoint(location: PdfPoint, currentZoom: Float): Int {
        val currentSelection = selectionModel.value ?: return SelectionHandle.NONE
        val start = currentSelection.startBoundary.location
        val end = currentSelection.endBoundary.location

        val (startTarget, endTarget) =
            getSelectionHandleBounds(currentZoom) ?: return SelectionHandle.NONE

        // Check for start handle
        if (location.pageNum == start.pageNum && startTarget.contains(location.x, location.y)) {
            return SelectionHandle.START
        }

        // Check for end handle
        if (location.pageNum == end.pageNum && endTarget.contains(location.x, location.y)) {
            return SelectionHandle.END
        }

        return SelectionHandle.NONE
    }

    private fun handleActionDownForMouse(location: PdfPoint, currentZoom: Float): Boolean {
        if (getHandleForTouchPoint(location, currentZoom) != SelectionHandle.NONE) {
            return maybeHandleActionDown(location, currentZoom)
        }
        // A new drag starts, clear previous selection and hide action mode.
        clearSelection()
        val boundary = UiSelectionBoundary(location, isRtl = false)
        draggingState =
            DraggingState(
                fixed = boundary,
                dragging = boundary,
                downPoint = PointF(location.x, location.y),
            )
        return true
    }

    private fun maybeHandleActionDown(location: PdfPoint, currentZoom: Float): Boolean {
        val currentSelection = selectionModel.value ?: return false
        val startBoundary = currentSelection.startBoundary
        val endBoundary = currentSelection.endBoundary

        draggingState =
            when (getHandleForTouchPoint(location, currentZoom)) {
                SelectionHandle.START ->
                    DraggingState(endBoundary, startBoundary, PointF(location.x, location.y))
                SelectionHandle.END ->
                    DraggingState(startBoundary, endBoundary, PointF(location.x, location.y))
                else -> return false
            }

        _selectionUiSignalBus.tryEmit(
            SelectionUiSignal.PlayHapticFeedback(HapticFeedbackConstants.TEXT_HANDLE_MOVE)
        )

        return true
    }

    private fun maybeHandleActionMove(location: PdfPoint?): Boolean {
        val prevDraggingState = draggingState ?: return false
        // location == null means the user dragged the handle just outside the bounds of any PDF
        // page.
        if (location == null) {
            // When the user drags outside the page, or to another page, we should still "capture"
            // the gesture (i.e. return true) to prevent spurious scrolling while the user is
            // attempting to adjust the selection. Return false if no drag is in progress.
            // See b/385291020
            return draggingState != null
        }
        val dx = location.x - prevDraggingState.downPoint.x
        val dy = location.y - prevDraggingState.downPoint.y
        val newEndPoint =
            if (location.pageNum == prevDraggingState.dragging.location.pageNum)
                prevDraggingState.dragging.location.translateBy(dx, dy)
            else PdfPoint(location.pageNum, PointF(location.x, location.y))

        updateRangeSelectionAsync(
            fixedPoint = prevDraggingState.fixed.location,
            draggedPoint = newEndPoint,
        )

        // Hide the action mode while the user is actively dragging the handles
        _selectionUiSignalBus.tryEmit(SelectionUiSignal.ToggleActionMode(show = false))
        return true
    }

    private fun maybeHandleGestureEnd(): Boolean {
        val result = draggingState != null
        draggingState = null
        // If this gesture actually ended a handle drag operation, trigger haptic feedback and
        // reveal the action mode
        if (result) {
            _selectionUiSignalBus.tryEmit(
                SelectionUiSignal.PlayHapticFeedback(HapticFeedbackConstants.TEXT_HANDLE_MOVE)
            )
            _selectionUiSignalBus.tryEmit(SelectionUiSignal.ToggleActionMode(show = true))
        }
        return result
    }

    private fun PdfPoint.translateBy(dx: Float, dy: Float): PdfPoint {
        return PdfPoint(this.pageNum, PointF(this.x + dx, this.y + dy))
    }

    private fun updateAllSelectionAsync(pageNum: Int) {
        updateSelectionAsync(pageNum..pageNum) {
            val newPageSelection =
                pdfDocument.getSelectAllSelectionBounds(pageNum) ?: return@updateSelectionAsync null

            SelectionModel.getCombinedSelectionModel(
                selectionModel.value?.documentSelection ?: DocumentSelection(SparseArray()),
                listOf(newPageSelection),
            )
        }
    }

    private fun updateRangeSelectionAsync(fixedPoint: PdfPoint, draggedPoint: PdfPoint) {
        val oldSelectionModel = selectionModel.value
        if (oldSelectionModel == null || fixedPoint.pageNum == draggedPoint.pageNum) {
            return updateSinglePageSelection(fixedPoint, draggedPoint)
        }
        updateMultiplePageSelection(fixedPoint, draggedPoint)
    }

    private fun updateMultiplePageSelection(fixedPoint: PdfPoint, draggedPoint: PdfPoint) {
        val prevSelectionModel = selectionModel.value ?: return
        val prevStart = prevSelectionModel.startBoundary.location
        val prevEnd = prevSelectionModel.endBoundary.location
        val pageRange =
            if (draggedPoint.pageNum < fixedPoint.pageNum) draggedPoint.pageNum..fixedPoint.pageNum
            else fixedPoint.pageNum..draggedPoint.pageNum
        updateSelectionAsync(pageRange) {
            val newPageSelections =
                if (draggedPoint.pageNum < fixedPoint.pageNum) {
                        // Extending selection in the upwards direction
                        getBoundsExtendingUpwards(draggedPoint, prevStart, prevEnd)
                    } else {
                        // Extending selection in the downwards direction
                        getBoundsExtendingDownwards(draggedPoint, prevStart, prevEnd)
                    }
                    .takeIf { it.isNotEmpty() } ?: return@updateSelectionAsync null

            SelectionModel.getCombinedSelectionModel(
                getOldSelectionBetweenPageRange(prevSelectionModel, pageRange),
                newPageSelections,
            )
        }
    }

    private suspend fun getBoundsExtendingUpwards(
        draggedPoint: PdfPoint,
        prevStart: PdfPoint,
        prevEnd: PdfPoint,
    ): List<PageSelection?> {

        val newPageSize = pageMetadataLoader?.getPageSize(draggedPoint.pageNum) ?: Point(0, 0)
        // Find selection bounds for all the skipped pages
        val intermediateSelection =
            getPageSelectionsForRange(draggedPoint.pageNum + 1, prevStart.pageNum - 1)
        return mutableListOf(
            // Find selection bounds of the page where dragged handles starts
            pdfDocument.getSelectionBounds(
                draggedPoint.pageNum,
                PointF(draggedPoint.x, draggedPoint.y),
                PointF(newPageSize.x.toFloat(), newPageSize.y.toFloat()),
            ),

            // Find selection bounds of the page where dragged handle stops
            getBoundsForFirstSelectedPage(prevStart, prevEnd, draggedPoint),
        ) + intermediateSelection
    }

    private suspend fun getBoundsExtendingDownwards(
        draggedPoint: PdfPoint,
        prevStart: PdfPoint,
        prevEnd: PdfPoint,
    ): List<PageSelection?> {

        // Find selection bounds for all the skipped pages
        val intermediateSelection =
            getPageSelectionsForRange(prevEnd.pageNum + 1, draggedPoint.pageNum - 1)
        return mutableListOf(
            // Find selection bounds of the page where dragged handles stops
            pdfDocument.getSelectionBounds(
                draggedPoint.pageNum,
                PointF(0f, 0f),
                PointF(draggedPoint.x, draggedPoint.y),
            ),

            // Find selection bounds of the page where dragged handle starts
            getBoundsForLastSelectedPage(prevStart, prevEnd, draggedPoint),
        ) + intermediateSelection
    }

    private suspend fun getBoundsForFirstSelectedPage(
        prevStart: PdfPoint,
        prevEnd: PdfPoint,
        draggedPoint: PdfPoint,
    ): PageSelection? {
        return if (prevStart.pageNum == prevEnd.pageNum) {
            pdfDocument.getSelectionBounds(
                prevEnd.pageNum,
                PointF(0f, 0f),
                PointF(prevEnd.x, prevEnd.y),
            )
        } else if (prevStart.pageNum > draggedPoint.pageNum) {
            pdfDocument.getSelectAllSelectionBounds(prevStart.pageNum)
        } else {
            null
        }
    }

    private suspend fun getBoundsForLastSelectedPage(
        prevStart: PdfPoint,
        prevEnd: PdfPoint,
        draggedPoint: PdfPoint,
    ): PageSelection? {
        return if (prevStart.pageNum == prevEnd.pageNum) {
            val prevPageSize = pageMetadataLoader?.getPageSize(prevEnd.pageNum) ?: Point(0, 0)
            pdfDocument.getSelectionBounds(
                prevEnd.pageNum,
                PointF(prevStart.x, prevStart.y),
                PointF(prevPageSize.x.toFloat(), prevPageSize.y.toFloat()),
            )
        } else if (prevEnd.pageNum < draggedPoint.pageNum) {
            pdfDocument.getSelectAllSelectionBounds(prevEnd.pageNum)
        } else {
            null
        }
    }

    private suspend fun getPageSelectionsForRange(
        startPage: Int,
        endPage: Int,
    ): List<PageSelection?> {
        val selections = mutableListOf<PageSelection?>()
        for (currentPage in startPage..endPage) {
            selections.add(pdfDocument.getSelectAllSelectionBounds(currentPage))
        }
        return selections
    }

    private fun updateSinglePageSelection(startPoint: PdfPoint, endPoint: PdfPoint) {
        updateSelectionAsync(startPoint.pageNum..endPoint.pageNum) {
            val newPageSelection =
                pdfDocument.getSelectionBounds(
                    endPoint.pageNum,
                    PointF(startPoint.x, startPoint.y),
                    PointF(endPoint.x, endPoint.y),
                ) ?: return@updateSelectionAsync null

            SelectionModel.getCombinedSelectionModel(
                DocumentSelection(SparseArray()),
                listOf(newPageSelection),
            )
        }
    }

    private fun getOldSelectionBetweenPageRange(
        oldSelectionModel: SelectionModel?,
        pageRange: IntRange,
    ): DocumentSelection {

        val selectedContents =
            oldSelectionModel?.documentSelection?.selectedContents ?: SparseArray()
        val keysToRemove = mutableListOf<Int>()
        selectedContents.forEach { pageNum, _ ->
            if (pageNum !in pageRange) keysToRemove.add(pageNum)
        }
        keysToRemove.forEach { selectedContents.remove(it) }

        return DocumentSelection(selectedContents)
    }

    private fun updateSelectionAsync(
        pageRange: IntRange,
        getNewSelectionModel: suspend () -> SelectionModel?,
    ) {
        val prevJob = setSelectionJob
        setSelectionJob =
            backgroundScope
                .launch {
                    prevJob?.cancelAndJoin()
                    try {
                        val newSelectionModel = getNewSelectionModel() ?: return@launch

                        _selectionModel.update { newSelectionModel }
                        _selectionUiSignalBus.tryEmit(SelectionUiSignal.Invalidate)
                        // Show the action mode if the user is not actively dragging the handles
                        if (draggingState == null) {
                            _selectionUiSignalBus.emit(
                                SelectionUiSignal.ToggleActionMode(show = true)
                            )
                        }
                    } catch (e: DeadObjectException) {
                        val exception =
                            RequestFailedException(
                                requestMetadata =
                                    RequestMetadata(
                                        requestName = CONTENT_SELECTION_REQUEST_NAME,
                                        pageRange = pageRange,
                                    ),
                                throwable = e,
                                // Non-critical failure, user can retry the operation.
                                showError = false,
                            )
                        errorFlow.emit(exception)
                    }
                }
                .also { it.invokeOnCompletion { setSelectionJob = null } }
    }

    /**
     * Returns true if this [PageSelection] has selected content with bounds, and if its start and
     * end boundaries include their location. Any selection without this information cannot be
     * displayed in the UI, and we expect this information to be present.
     *
     * [androidx.pdf.content.SelectionBoundary] is overloaded as both an input to selection and an
     * output from it, and here we are interacting with it as an output. In the output case, it
     * should always specify its [androidx.pdf.content.SelectionBoundary.point]
     */
    private val PageSelection.hasBounds: Boolean
        get() {
            return this.selectedContents.any { it.bounds.isNotEmpty() } &&
                this.start.point != null &&
                this.stop.point != null
        }
}

/**
 * Signals to [androidx.pdf.view.PdfView] to update the UI in regards to a change in selection state
 */
internal sealed interface SelectionUiSignal {
    /** [androidx.pdf.view.PdfView] should invalidate itself to reflect a change in selection */
    object Invalidate : SelectionUiSignal

    /**
     * [androidx.pdf.view.PdfView] should play haptic feedback to indicate the start or end of a
     * change in selection
     *
     * @param level should be a value from [HapticFeedbackConstants] indicating the type of haptic
     *   feedback to play
     */
    class PlayHapticFeedback(val level: Int) : SelectionUiSignal

    /** [androidx.pdf.view.PdfView] should show or hide the selection action mode */
    class ToggleActionMode(val show: Boolean) : SelectionUiSignal
}

/** Value class to hold state related to dragging a selection handle */
private data class DraggingState(
    val fixed: UiSelectionBoundary,
    val dragging: UiSelectionBoundary,
    val downPoint: PointF,
)

/** Defines integer constants to represent relative position of selection handles */
private object SelectionHandle {
    const val NONE = 0
    const val START = 1
    const val END = 2
}

@IntDef(SelectionHandle.NONE, SelectionHandle.START, SelectionHandle.END)
@Retention(AnnotationRetention.SOURCE)
private annotation class HandlePositionDef
