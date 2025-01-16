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

package androidx.pdf.view

import android.graphics.Point
import android.graphics.PointF
import android.graphics.Rect
import android.graphics.RectF
import android.util.Range
import android.util.SparseArray
import androidx.annotation.VisibleForTesting
import androidx.pdf.PdfDocument
import kotlin.math.roundToInt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Owns and updates all pagination-related state, including the range of pages that are visible in
 * the viewport, the position of each page in the viewport, and the dimensions of each page in
 * content coordinates.
 *
 * Not thread safe
 */
internal class PageLayoutManager(
    private val pdfDocument: PdfDocument,
    private val backgroundScope: CoroutineScope,
    private val pagePrefetchRadius: Int,
    // TODO(b/376299551) - Make page margin configurable via XML attribute
    pageSpacingPx: Int = DEFAULT_PAGE_SPACING_PX,
    internal val paginationModel: PaginationModel =
        PaginationModel(pageSpacingPx, pdfDocument.pageCount)
) {
    /** The 0-indexed maximum page number whose dimensions are known to this model */
    val reach
        get() = paginationModel.reach

    private val _dimensions = MutableSharedFlow<Pair<Int, Point>>(replay = pdfDocument.pageCount)

    /**
     * A [SharedFlow] of PDF page dimensions, represented by a [Pair] whose first value is the page
     * number and whose second value is a [Point] representing the page's dimensions in PDF points
     */
    val dimensions: SharedFlow<Pair<Int, Point>>
        get() = _dimensions

    /**
     * The [Range] of pages that are currently visible in the window.
     *
     * Values in the range are 0-indexed.
     */
    var visiblePages = Range<Int>(0, 0)
        private set

    /**
     * True if this manager is actively laying out pages to reach the current view position, and
     * [visiblePages] and other values represent best guesses while page dimensions are loading
     */
    var layingOutPages: Boolean = false
        private set

    /**
     * The range of pages that are current visible in the window
     *
     * Values in the range are 0-indexed.
     */
    var fullyVisiblePages = Range<Int>(0, 0)
        private set

    /** The portions of each visible page that are visible, in page coordinate space */
    var visiblePageAreas = SparseArray<Rect>(1)
        private set

    /** The locations of each visible page, in content coordinate space */
    @VisibleForTesting
    internal var pageLocations = SparseArray<Rect>(1)
        private set

    /** The 0-indexed maximum page whose dimensions have been requested */
    private var requestedReach: Int = paginationModel.reach

    /**
     * The current [Job] that is handling dimensions loading work
     *
     * In order to ensure dimensions are loaded sequentially, this [Job] is always [Job.join]ed at
     * the beginning of any coroutine to load new dimensions
     */
    private var currentDimensionsJob: Job? = null

    init {
        // If we received a PaginationModel that already has some dimensions, emit those to the View
        // This is the restored instanceState case
        if (paginationModel.reach >= 0) {
            for (i in 0..paginationModel.reach) {
                _dimensions.tryEmit(i to paginationModel.getPageSize(i))
            }
        }

        increaseReach(pagePrefetchRadius - 1)
    }

    /** Returns the current content coordinate location of a 0-indexed [pageNum] */
    fun getPageLocation(pageNum: Int, viewport: Rect): Rect {
        return pageLocations.get(pageNum) ?: paginationModel.getPageLocation(pageNum, viewport)
    }

    /** Returns the size of the page at [pageNum], or null if we don't know that page's size yet */
    fun getPageSize(pageNum: Int): Point? {
        val size = paginationModel.getPageSize(pageNum)
        if (size == PaginationModel.UNKNOWN_SIZE) return null
        return size
    }

    /**
     * Returns the [PdfPoint] that exists at [contentCoordinates], or null if no page content is
     * laid out at [contentCoordinates].
     *
     * @param contentCoordinates the content coordinates to check (View coordinates that are scaled
     *   up or down by the current zoom level)
     * @param viewport the current viewport in content coordinates
     * @param scanAllPages true to scan pages outside the viewport for the [PdfPoint] at
     *   [contentCoordinates]
     */
    fun getPdfPointAt(
        contentCoordinates: PointF,
        viewport: Rect,
        scanAllPages: Boolean = false
    ): PdfPoint? {
        for (pageIndex in visiblePages.lower..visiblePages.upper) {
            findPointOnPage(pageIndex, viewport, contentCoordinates)?.let {
                return it
            }
        }
        if (!scanAllPages) return null
        for (pageIndex in 0..paginationModel.reach) {
            if (pageIndex in visiblePages.lower..visiblePages.upper) continue
            findPointOnPage(pageIndex, viewport, contentCoordinates)?.let {
                return it
            }
        }
        return null
    }

    private fun findPointOnPage(
        pageNum: Int,
        viewport: Rect,
        contentCoordinates: PointF
    ): PdfPoint? {
        val pageBounds = getPageLocation(pageNum, viewport)
        if (
            pageBounds.contains(
                contentCoordinates.x.roundToInt(),
                contentCoordinates.y.roundToInt()
            )
        ) {
            return PdfPoint(
                pageNum,
                PointF(
                    contentCoordinates.x - pageBounds.left,
                    contentCoordinates.y - pageBounds.top,
                )
            )
        }
        return null
    }

    /**
     * Returns a View-relative [RectF] corresponding to a page-relative [PdfRect], or null if the
     * page hasn't been laid out
     */
    fun getViewRect(pdfRect: PdfRect, viewport: Rect): RectF? {
        if (pdfRect.pageNum > paginationModel.reach) return null
        val pageBounds = getPageLocation(pdfRect.pageNum, viewport)
        val out = RectF(pdfRect.pageRect)
        out.offset(pageBounds.left.toFloat(), pageBounds.top.toFloat())
        return out
    }

    /**
     * Updates properties on viewport changes, namely [visiblePages], [fullyVisiblePages],
     * [pageLocations], and [visiblePageAreas]
     *
     * @param viewport the visible region of [PdfView] in unscaled / content coordinates
     */
    fun onViewportChanged(viewport: Rect): Boolean {
        // Order of operations is important, each of these computations depends on the previous one
        val visiblePagesChanged = computeVisiblePages(viewport)
        val pageLocationsChanged = computePageLocationsInView(viewport)
        val pageAreasChanged = computeVisiblePageAreas(viewport)
        return visiblePagesChanged || pageLocationsChanged || pageAreasChanged
    }

    /**
     * Sequentially enqueues requests for any pages up to [untilPage] that we haven't requested
     * dimensions for
     */
    fun increaseReach(untilPage: Int) {
        if (untilPage < requestedReach) return

        for (i in requestedReach + 1..minOf(untilPage, paginationModel.numPages - 1)) {
            loadPageDimensions(i)
        }
    }

    /**
     * Updates [visiblePages] and [fullyVisiblePages] on viewport changes, and returns true if the
     * value of either one did change
     */
    private fun computeVisiblePages(viewport: Rect): Boolean {
        val prevVisible = visiblePages
        val prevFullyVisible = fullyVisiblePages
        val newPagesInViewport = paginationModel.getPagesInViewport(viewport.top, viewport.bottom)
        visiblePages = newPagesInViewport.pages
        layingOutPages = newPagesInViewport.layoutInProgress
        fullyVisiblePages =
            paginationModel
                .getPagesInViewport(viewport.top, viewport.bottom, includePartial = false)
                .pages
        if (prevVisible != visiblePages) {
            // If new pages are visible, start laying out more pages
            increaseReach(
                minOf(visiblePages.upper + pagePrefetchRadius, paginationModel.numPages - 1)
            )
        }
        return prevVisible != visiblePages || prevFullyVisible != fullyVisiblePages
    }

    /**
     * Updates [pageLocations] on viewport changes, and returns true if the value did change
     *
     * [visiblePages] must be updated first
     */
    private fun computePageLocationsInView(viewport: Rect): Boolean {
        val prevLocations = pageLocations
        val pageLocations = SparseArray<Rect>(visiblePages.upper - visiblePages.lower + 1)
        for (i in visiblePages.lower..visiblePages.upper) {
            pageLocations.put(i, paginationModel.getPageLocation(i, viewport))
        }
        this.pageLocations = pageLocations
        return !prevLocations.contentEquals(this@PageLayoutManager.pageLocations)
    }

    /**
     * Updates [visiblePageAreas] on viewport changes, and returns true if the value did change
     *
     * [visiblePages] and [pageLocations] must be updated first
     */
    private fun computeVisiblePageAreas(viewport: Rect): Boolean {
        val prevAreas = visiblePageAreas
        val visibleAreas = SparseArray<Rect>(visiblePages.upper - visiblePages.lower + 1)
        for (i in visiblePages.lower..visiblePages.upper) {
            val pageLocation = pageLocations.get(i)
            val pageWidth = pageLocation.right - pageLocation.left
            val pageHeight = pageLocation.bottom - pageLocation.top
            val area =
                Rect(
                    maxOf(viewport.left - pageLocation.left, 0),
                    maxOf(viewport.top - pageLocation.top, 0),
                    minOf(viewport.right - pageLocation.left, pageWidth),
                    minOf(viewport.bottom - pageLocation.top, pageHeight),
                )
            visibleAreas.put(i, area)
        }
        visiblePageAreas = visibleAreas
        return !prevAreas.contentEquals(visiblePageAreas)
    }

    /** Waits for any outstanding dimensions to be loaded, then loads dimensions for [pageNum] */
    private fun loadPageDimensions(pageNum: Int) {
        requestedReach = pageNum
        val previousDimensionsJob = currentDimensionsJob
        currentDimensionsJob =
            backgroundScope.launch {
                previousDimensionsJob?.join()
                val pageMetadata = pdfDocument.getPageInfo(pageNum)
                val size = Point(pageMetadata.width, pageMetadata.height)
                // Add the value to the model before emitting, and on the main thread
                withContext(Dispatchers.Main) { paginationModel.addPage(pageNum, size) }
                _dimensions.emit(pageNum to Point(pageMetadata.width, pageMetadata.height))
            }
    }

    companion object {
        private const val DEFAULT_PAGE_SPACING_PX: Int = 20
    }
}
