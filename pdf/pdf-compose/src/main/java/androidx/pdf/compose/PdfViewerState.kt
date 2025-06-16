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

package androidx.pdf.compose

import android.graphics.PointF
import android.graphics.RectF
import android.util.SparseArray
import androidx.annotation.IntRange
import androidx.annotation.RestrictTo
import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.MutatorMutex
import androidx.compose.runtime.Composable
import androidx.compose.runtime.annotation.FrequentlyChangingValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.core.util.keyIterator
import androidx.pdf.view.Highlight
import androidx.pdf.view.PdfPoint
import androidx.pdf.view.PdfView
import androidx.pdf.view.Selection
import kotlin.math.roundToInt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch

/** Creates a [PdfViewerState] that is remembered across compositions using [remember] */
@Composable
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun rememberPdfViewerState(): PdfViewerState {
    val coroutineScope = rememberCoroutineScope()
    return remember { PdfViewerState(coroutineScope) }
}

/** Scope used for suspending scroll blocks */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public interface PdfZoomScrollScope {
    /**
     * Attempts to scroll forward by [delta] px.
     *
     * @return the amount of requested scroll that was consumed (i.e. how far it scrolled)
     */
    public fun scrollBy(delta: Offset): Offset

    /** Instantly sets the zoom level to [zoomLevel] */
    public fun zoomTo(zoomLevel: Float)
}

/**
 * A state object that can be hoisted to observe and control [PdfViewer] zoom, scroll, and content
 * position.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class PdfViewerState(
    /**
     * Composition-scoped [CoroutineScope] used to bridge non-suspending callbacks from the
     * [PdfView] underlying [PdfViewer] with suspend scrolling utilities like [MutatorMutex]
     */
    private val coroutineScope: CoroutineScope
) {
    private val zoomScrollMutex = MutatorMutex()

    internal var pdfView: PdfView? = null
        set(value) {
            if (field === value) return
            pdfViewObserver?.let {
                field?.removeOnViewportChangedListener(it)
                field?.removeOnGestureStateChangedListener(it)
                field?.removeOnSelectionChangedListener(it)
            }
            firstVisiblePage = pdfView?.firstVisiblePage ?: 0
            visiblePagesCount = pdfView?.visiblePagesCount ?: 0
            gestureState = pdfView?.gestureState ?: GESTURE_STATE_IDLE
            zoom = pdfView?.zoom ?: PdfView.DEFAULT_INIT_ZOOM
            pageOffsetsState.clear()
            currentSelection = pdfView?.currentSelection
            // Cancel any in-progress mutations to release the mutex. MutatorMutex only supports
            // cancellation by enqueuing higher-priority mutations.
            coroutineScope.launch {
                zoomScrollMutex.mutate(priority = MutatePriority.PreventUserInput) {}
            }
            field = value
            field?.let { pdfView ->
                pdfViewObserver =
                    PdfViewObserver().also { observer ->
                        pdfView.addOnViewportChangedListener(observer)
                        pdfView.addOnGestureStateChangedListener(observer)
                        pdfView.addOnSelectionChangedListener(observer)
                    }
                pdfViewPositioner = PdfViewPositioner(pdfView)
            }
        }

    private var pdfViewPositioner: PdfViewPositioner? = null
    private var pdfViewObserver: PdfViewObserver? = null

    /** The first page in the viewport, including partially-visible pages. 0-indexed. */
    @get:FrequentlyChangingValue
    public var firstVisiblePage: Int by mutableIntStateOf(0)
        private set

    /** The number of pages visible in the viewport, including partially visible pages. */
    @get:FrequentlyChangingValue
    public var visiblePagesCount: Int by mutableIntStateOf(0)
        private set

    /**
     * The zoom level of this view, as a factor of the content's natural size with when 1 pixel is
     * equal to 1 PDF point.
     */
    @get:FrequentlyChangingValue
    public var zoom: Float by mutableFloatStateOf(PdfView.DEFAULT_INIT_ZOOM)
        private set

    /** The [Offset] of each visible page, keyed by 0-indexed page number. */
    // No mutableState*Of infrastructure for primitive collection types
    @Suppress("PrimitiveInCollection")
    public val pageOffsets: Map<Int, Offset>
        @FrequentlyChangingValue get() = pageOffsetsState

    /**
     * State regarding whether the user is interacting with the PDF, one of [GESTURE_STATE_IDLE],
     * [GESTURE_STATE_SETTLING], or [GESTURE_STATE_INTERACTING]
     */
    public var gestureState: Int by mutableIntStateOf(GESTURE_STATE_IDLE)
        private set

    /** The currently-selected content in the PDF, or null if nothing is selected */
    public var currentSelection: Selection? by mutableStateOf(null)
        private set

    // No mutableState*Of infrastructure for primitive collection types
    @Suppress("PrimitiveInCollection")
    private val pageOffsetsState = mutableStateMapOf<Int, Offset>()

    /**
     * Returns the [PdfPoint] corresponding to [offset] in Compose coordinates, or null if no PDF
     * content has been laid out at this Offset.
     *
     * Returns null if this [PdfViewerState] is not yet associated with a [PdfViewer], or if the
     * [PdfViewer] is not associated with a [androidx.pdf.PdfDocument]
     */
    public fun toPdfPoint(offset: Offset): PdfPoint? {
        return pdfView?.viewToPdfPoint(PointF(offset.x, offset.y))
    }

    /**
     * Returns the View coordinate location of [pdfPoint], or null if that PDF content has not been
     * laid out yet.
     *
     * Returns [Offset.Unspecified] if this [PdfViewerState] is not yet associated with a
     * [PdfViewer], or if the [PdfViewer] is not associated with a [androidx.pdf.PdfDocument]
     */
    public fun toOffset(pdfPoint: PdfPoint): Offset? {
        return pdfView?.pdfToViewPoint(pdfPoint)?.toOffset() ?: Offset.Unspecified
    }

    /** Centers the page at [pageNum] in the viewport. */
    public suspend fun scrollToPage(@IntRange(from = 0) pageNum: Int) {
        zoomScrollMutex.mutate { pdfView?.scrollToPage(pageNum) }
    }

    /** Centers the location described by [position] in the viewport */
    public suspend fun scrollToPosition(position: PdfPoint) {
        zoomScrollMutex.mutate { pdfView?.scrollToPosition(position) }
    }

    /**
     * Call this function to take control of zoom and scroll, and gain the ability to send zoom and
     * / or scroll events via [PdfZoomScrollScope]. All actions that change the logical zoom and /
     * or scroll position must be performed within a [zoomScroll] block, even if they don't call
     * other methods on this object in order to guarantee that mutual exclusion is enforced.
     *
     * If [zoomScroll] is called from elsewhere, this will be cancelled.
     */
    public suspend fun zoomScroll(block: PdfZoomScrollScope.() -> Unit) {
        pdfViewPositioner?.let { zoomScrollMutex.mutateWith(it) { block() } }
    }

    /** Clears the current selection, if one exists. No-op if there is no current [Selection] */
    public fun clearSelection() {
        pdfView?.clearSelection()
    }

    /**
     * Applies a set of [Highlight] to be drawn over this PDF. Each [Highlight] may be a different
     * color. This overrides any previous highlights, there is no merging of new and previous
     * values. [highlights] are defensively copied and the list or its contents may be modified
     * after providing it here.
     */
    public fun setHighlights(highlights: List<Highlight>) {
        pdfView?.setHighlights(highlights)
    }

    /** Listens to [PdfView] state to update the containing PdfViewerState. */
    private inner class PdfViewObserver() :
        PdfView.OnViewportChangedListener,
        PdfView.OnGestureStateChangedListener,
        PdfView.OnSelectionChangedListener {
        private var interactionSession: UserInteractionSession? = null

        override fun onViewportChanged(
            firstVisiblePage: Int,
            visiblePagesCount: Int,
            pageLocations: SparseArray<RectF>,
            zoomLevel: Float,
        ) {
            this@PdfViewerState.firstVisiblePage = firstVisiblePage
            this@PdfViewerState.visiblePagesCount = visiblePagesCount
            zoom = zoomLevel

            // Clear no longer visible pages
            for (page in pageOffsetsState.keys) {
                if (!pageLocations.contains(page)) {
                    pageOffsetsState.remove(page)
                }
            }
            // Add or update new or existing pages
            for (page in pageLocations.keyIterator()) {
                pageOffsetsState.put(page, pageLocations.get(page).toOffset())
            }
        }

        override fun onGestureStateChanged(newState: Int) {
            when (newState) {
                PdfView.GESTURE_STATE_IDLE -> {
                    interactionSession?.close()
                    gestureState = GESTURE_STATE_IDLE
                }
                PdfView.GESTURE_STATE_INTERACTING -> {
                    gestureState = GESTURE_STATE_INTERACTING
                    interactionSession = UserInteractionSession()
                    coroutineScope.launch {
                        // Lock out Default priority mutations while the user is interacting
                        // This will cancel any ongoing Default-priority mutations as well as any
                        // ongoing UserInput mutations in case a previous UserInteractionSession
                        // was not properly closed.
                        zoomScrollMutex.mutate(priority = MutatePriority.UserInput) {
                            interactionSession?.awaitClose()
                        }
                    }
                }
                PdfView.GESTURE_STATE_SETTLING -> {
                    // GESTURE_STATE_SETTLING is an intermediate value and we don't need to take
                    // any action other than updating our own state. Other values are unexpected.
                    gestureState = GESTURE_STATE_SETTLING
                }
            }
        }

        override fun onSelectionChanged(previousSelection: Selection?, newSelection: Selection?) {
            currentSelection = newSelection
        }
    }

    public companion object {
        /**
         * [PdfViewer] is not currently being affected by an outside input, e.g. user touch
         *
         * @see [PdfViewerState.gestureState]
         */
        public const val GESTURE_STATE_IDLE: Int = PdfView.GESTURE_STATE_IDLE

        /**
         * [PdfViewer] is currently being affected by an outside input, e.g. user touch
         *
         * @see [PdfViewerState.gestureState]
         */
        public const val GESTURE_STATE_INTERACTING: Int = PdfView.GESTURE_STATE_INTERACTING

        /**
         * [PdfViewer] is currently animating to a final position while not under outside control,
         * e.g. settling on a final position following a fling gesture.
         *
         * @see [PdfViewerState.gestureState]
         */
        public const val GESTURE_STATE_SETTLING: Int = PdfView.GESTURE_STATE_SETTLING
    }
}

/** [PdfZoomScrollScope] implementation that uses the [PdfView] underlying [PdfViewer] */
private class PdfViewPositioner(private val pdfView: PdfView) : PdfZoomScrollScope {
    override fun scrollBy(delta: Offset): Offset {
        val before = pdfView.scrollX to pdfView.scrollY
        pdfView.scrollBy(delta.x.roundToInt(), delta.y.roundToInt())
        val after = pdfView.scrollX to pdfView.scrollY
        return Offset(
            (after.first - before.first).toFloat(),
            (after.second - before.second).toFloat(),
        )
    }

    override fun zoomTo(zoomLevel: Float) {
        pdfView.zoom = zoomLevel
    }
}

/**
 * Enables higher level code to track a user interaction session i.e. a duration of time during
 * which the user is interacting with the PDF.
 */
private class UserInteractionSession {
    private val channel = Channel<Unit>()

    fun close() {
        channel.trySend(Unit)
    }

    suspend fun awaitClose() {
        channel.receive()
    }
}

private fun PointF.toOffset() = Offset(this.x, this.y)

private fun RectF.toOffset() = Offset(this.left, this.top)
