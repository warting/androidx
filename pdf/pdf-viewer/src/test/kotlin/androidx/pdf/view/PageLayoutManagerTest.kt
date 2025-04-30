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
import android.graphics.Rect
import android.util.Range
import android.util.SparseArray
import androidx.core.util.keyIterator
import androidx.pdf.PdfDocument
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class PageLayoutManagerTest {
    private val pdfDocument =
        mock<PdfDocument> {
            on { pageCount } doReturn TOTAL_PAGES
            onBlocking { getPageInfo(any()) } doAnswer
                { invocationOnMock ->
                    PdfDocument.PageInfo(
                        pageNum = invocationOnMock.getArgument(0),
                        height = PAGE_HEIGHT,
                        width = PAGE_WIDTH,
                    )
                }
        }

    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher)
    private lateinit var paginationManager: PageLayoutManager
    private val errorFlow = MutableSharedFlow<Throwable>()

    @Before
    fun setup() {
        // Required because loadPageDimensions jumps to the main thread to update PaginationModel
        Dispatchers.setMain(testDispatcher)
        paginationManager = PageLayoutManager(pdfDocument, testScope, errorFlow = errorFlow)
    }

    @Test
    fun onViewportChanged_updateVisiblePages() = runTest {
        val visiblePageValues = mutableListOf<Range<Int>>()
        // Add some pages to the manager and wait for their dimensions to load
        paginationManager.increaseReach(20)
        testScope.testScheduler.runCurrent()
        assertThat(paginationManager.reach).isEqualTo(20)
        visiblePageValues.add(paginationManager.visiblePages)

        // Change the viewport twice
        val changed1 = paginationManager.onViewportChanged(Rect(0, 0, 500, 1000))
        assertThat(changed1).isTrue()
        visiblePageValues.add(paginationManager.visiblePages)
        val changed2 = paginationManager.onViewportChanged(Rect(0, 1000, 500, 2000))
        assertThat(changed2).isTrue()
        visiblePageValues.add(paginationManager.visiblePages)

        // We expect 3 unique values: the default [0, 0] value and two updates based on two viewport
        // changes
        assertThat(visiblePageValues.toSet().size).isEqualTo(3)
        assertThat(visiblePageValues[0]).isEqualTo(Range(0, 0))
        // These assertions are deliberately coarse so as not to test the implementation details of
        // PaginationModel here
        assertThat(visiblePageValues[1].upper).isGreaterThan(0)
        assertThat(visiblePageValues[2].upper).isGreaterThan(visiblePageValues[1].upper)
    }

    @Test
    fun onViewportChanged_updateVisiblePages_noChange() = runTest {
        val visiblePageValues = mutableListOf<Range<Int>>()
        // Add some pages to the manager and wait for their dimensions to load
        paginationManager.increaseReach(20)
        testScope.testScheduler.runCurrent()
        assertThat(paginationManager.reach).isEqualTo(20)
        visiblePageValues.add(paginationManager.visiblePages)

        // Change the viewport twice, but to the same value
        val changed1 = paginationManager.onViewportChanged(Rect(0, 0, 500, 1000))
        assertThat(changed1).isTrue()
        visiblePageValues.add(paginationManager.visiblePages)
        val changed2 = paginationManager.onViewportChanged(Rect(0, 0, 500, 1000))
        assertThat(changed2).isFalse()
        visiblePageValues.add(paginationManager.visiblePages)

        // We expect 2 unique values: the default [0, 0] value and one update based on one viewport
        // change
        assertThat(visiblePageValues.toSet().size).isEqualTo(2)
        assertThat(visiblePageValues[0]).isEqualTo(Range(0, 0))
        // This assertion is deliberately coarse so as not to test the implementation details of
        // PaginationModel here
        assertThat(visiblePageValues[1].upper).isGreaterThan(0)
    }

    @Test
    fun onViewportChanged_prefetchPages() = runTest {
        // Add some pages to the manager and wait for their dimensions to load
        paginationManager.increaseReach(5)
        testScope.testScheduler.runCurrent()
        assertThat(paginationManager.reach).isEqualTo(5)

        // Update the viewport, for simplicity's sake to a value that fits all the currently
        // measured pages
        paginationManager.onViewportChanged(Rect(0, 0, 0, 1200))

        // Make sure we've fetched all currently measured & visible pages + the page prefetch radius
        testScope.testScheduler.runCurrent()
        assertThat(paginationManager.reach).isEqualTo(5 + PageLayoutManager.DEFAULT_PREFETCH_RADIUS)
    }

    @Test
    fun onViewportChanged_updateFullyVisiblePages() = runTest {
        val fullyVisibleValues = mutableListOf<Range<Int>>()
        // Add some pages to the manager and wait for their dimensions to load
        paginationManager.increaseReach(20)
        testScope.testScheduler.runCurrent()
        assertThat(paginationManager.reach).isEqualTo(20)
        fullyVisibleValues.add(paginationManager.fullyVisiblePages)

        // Change the viewport twice
        val changed1 = paginationManager.onViewportChanged(Rect(0, 0, 500, 1000))
        assertThat(changed1).isTrue()
        fullyVisibleValues.add(paginationManager.fullyVisiblePages)
        val changed2 = paginationManager.onViewportChanged(Rect(0, 500, 500, 1500))
        assertThat(changed2).isTrue()
        fullyVisibleValues.add(paginationManager.fullyVisiblePages)

        // We expect 3 unique values: the default [0, 0] value and two updates based on two viewport
        // changes
        assertThat(fullyVisibleValues.toSet().size).isEqualTo(3)
        assertThat(fullyVisibleValues[0]).isEqualTo(Range(0, 0))
        // These assertions are deliberately coarse so as not to test the implementation details of
        // PaginationModel here
        assertThat(fullyVisibleValues[1].upper).isGreaterThan(0)
        assertThat(fullyVisibleValues[2].upper).isGreaterThan(fullyVisibleValues[1].upper)
    }

    @Test
    fun onViewportChanged_updateFullyVisiblePages_noChange() = runTest {
        val fullyVisibleValues = mutableListOf<Range<Int>>()
        // Add some pages to the manager and wait for their dimensions to load
        paginationManager.increaseReach(20)
        testScope.testScheduler.runCurrent()
        assertThat(paginationManager.reach).isEqualTo(20)
        fullyVisibleValues.add(paginationManager.fullyVisiblePages)

        // Change the viewport twice, to the same value
        val changed1 = paginationManager.onViewportChanged(Rect(0, 0, 500, 1000))
        assertThat(changed1).isTrue()
        fullyVisibleValues.add(paginationManager.fullyVisiblePages)
        val changed2 = paginationManager.onViewportChanged(Rect(0, 0, 500, 1000))
        assertThat(changed2).isFalse()
        fullyVisibleValues.add(paginationManager.fullyVisiblePages)

        // We expect 2 unique values: the default [0, 0] value and one update based on one viewport
        // change
        assertThat(fullyVisibleValues.toSet().size).isEqualTo(2)
        assertThat(fullyVisibleValues[0]).isEqualTo(Range(0, 0))
        // These assertions are deliberately coarse so as not to test the implementation details of
        // PaginationModel here
        assertThat(fullyVisibleValues[1].upper).isGreaterThan(0)
    }

    @Test
    fun onViewportChanged_updateVisiblePageAreas() = runTest {
        val visiblePageAreas = mutableListOf<SparseArray<Rect>>()
        // Add some pages to the manager and wait for their dimensions to load
        paginationManager.increaseReach(20)
        testScope.testScheduler.runCurrent()
        assertThat(paginationManager.reach).isEqualTo(20)
        visiblePageAreas.add(paginationManager.visiblePageAreas)

        // Change the viewport twice
        val changed1 = paginationManager.onViewportChanged(Rect(0, 0, 500, 1000))
        assertThat(changed1).isTrue()
        visiblePageAreas.add(paginationManager.visiblePageAreas)
        val changed2 = paginationManager.onViewportChanged(Rect(0, 500, 500, 1500))
        assertThat(changed2).isTrue()
        visiblePageAreas.add(paginationManager.visiblePageAreas)

        // We expect 3 unique values: the default empty value and two updates based on two viewport
        // changes
        assertThat(visiblePageAreas.size).isEqualTo(3)
        val firstAreas = visiblePageAreas[1]
        assertThat(firstAreas.contentEquals(visiblePageAreas[2])).isFalse()
        // Before we learn the viewport, we don't know what's visible
        assertThat(visiblePageAreas[0].size()).isEqualTo(0)
        // First viewport should include all of page 0 through part of page 5
        assertThat(firstAreas.size()).isEqualTo(5)
        assertThat(firstAreas.keyAt(0)).isEqualTo(0)
        assertThat(firstAreas.keyAt(4)).isEqualTo(4)
        assertThat(firstAreas.get(0).area).isEqualTo(PAGE_WIDTH * PAGE_HEIGHT)
        assertThat(firstAreas.get(4).area).isLessThan(PAGE_WIDTH * PAGE_HEIGHT)
        // Second viewport should include part of page 3 through part of page 7
        val secondAreas = visiblePageAreas[2]
        assertThat(secondAreas.size()).isEqualTo(5)
        assertThat(secondAreas.keyAt(0)).isEqualTo(2)
        assertThat(secondAreas.keyAt(4)).isEqualTo(6)
        assertThat(secondAreas.get(2).area).isLessThan(PAGE_WIDTH * PAGE_HEIGHT)
        assertThat(secondAreas.get(6).area).isLessThan(PAGE_WIDTH * PAGE_HEIGHT)
    }

    @Test
    fun onViewportChanged_updateVisiblePageAreas_noChange() = runTest {
        val visiblePageAreas = mutableListOf<SparseArray<Rect>>()
        // Add some pages to the manager and wait for their dimensions to load
        paginationManager.increaseReach(20)
        testScope.testScheduler.runCurrent()
        assertThat(paginationManager.reach).isEqualTo(20)
        visiblePageAreas.add(paginationManager.visiblePageAreas)

        // Change the viewport twice, but to the same value
        val changed1 = paginationManager.onViewportChanged(Rect(0, 0, 500, 1000))
        assertThat(changed1).isTrue()
        visiblePageAreas.add(paginationManager.visiblePageAreas)
        val changed2 = paginationManager.onViewportChanged(Rect(0, 0, 500, 1000))
        assertThat(changed2).isFalse()
        visiblePageAreas.add(paginationManager.visiblePageAreas)

        // We expect 2 unique values: the default empty value and one update based on one viewport
        // change
        assertThat(visiblePageAreas.size).isEqualTo(3)
        val firstAreas = visiblePageAreas[1]
        assertThat(firstAreas.contentEquals(visiblePageAreas[2])).isTrue()
        // Before we learn the viewport, we don't know what's visible
        assertThat(visiblePageAreas[0].size()).isEqualTo(0)
        // First viewport should include all of page 0 through part of page 5
        assertThat(firstAreas.size()).isEqualTo(5)
        assertThat(firstAreas.keyAt(0)).isEqualTo(0)
        assertThat(firstAreas.keyAt(4)).isEqualTo(4)
        assertThat(firstAreas.get(0).area).isEqualTo(PAGE_WIDTH * PAGE_HEIGHT)
        assertThat(firstAreas.get(4).area).isLessThan(PAGE_WIDTH * PAGE_HEIGHT)
    }

    @Test
    fun onViewportChanged_updatePageLocations() = runTest {
        val pageLocations = mutableListOf<SparseArray<Rect>>()
        // Add some pages to the manager and wait for their dimensions to load
        paginationManager.increaseReach(20)
        testScope.testScheduler.runCurrent()
        assertThat(paginationManager.reach).isEqualTo(20)
        pageLocations.add(paginationManager.pageLocations)

        // Change the viewport twice
        val changed1 = paginationManager.onViewportChanged(Rect(0, 0, 500, 1000))
        assertThat(changed1).isTrue()
        pageLocations.add(paginationManager.pageLocations)
        val changed2 = paginationManager.onViewportChanged(Rect(0, 500, 500, 1500))
        assertThat(changed2).isTrue()
        pageLocations.add(paginationManager.pageLocations)

        // We expect 3 unique values: the default empty value and two updates based on two viewport
        // changes
        assertThat(pageLocations.size).isEqualTo(3)
        val firstLocations = pageLocations[1]
        assertThat(firstLocations.contentEquals(pageLocations[2])).isFalse()
        // Before we learn the viewport, we don't know what's visible
        assertThat(pageLocations[0].size()).isEqualTo(0)
        // First viewport should include pages 0-5
        assertThat(firstLocations.size()).isEqualTo(5)
        assertThat(firstLocations.keyAt(0)).isEqualTo(0)
        assertThat(firstLocations.keyAt(4)).isEqualTo(4)
        var lastPageTop = -1
        for (page in firstLocations.keyIterator()) {
            val thisPageTop = firstLocations.get(page).top
            assertThat(thisPageTop).isGreaterThan(lastPageTop)
            lastPageTop = thisPageTop
        }
        // Second viewport should include pages 3-7
        val secondLocations = pageLocations[2]
        assertThat(secondLocations.size()).isEqualTo(5)
        assertThat(secondLocations.keyAt(0)).isEqualTo(2)
        assertThat(secondLocations.keyAt(4)).isEqualTo(6)
        lastPageTop = 0
        for (page in secondLocations.keyIterator()) {
            val thisPageTop = secondLocations.get(page).top
            assertThat(thisPageTop).isGreaterThan(lastPageTop)
            lastPageTop = thisPageTop
        }
    }

    @Test
    fun onViewportChanged_updatePageLocations_noChange() = runTest {
        val pageLocations = mutableListOf<SparseArray<Rect>>()
        // Add some pages to the manager and wait for their dimensions to load
        paginationManager.increaseReach(20)
        testScope.testScheduler.runCurrent()
        assertThat(paginationManager.reach).isEqualTo(20)
        pageLocations.add(paginationManager.pageLocations)

        // Change the viewport twice
        val changed1 = paginationManager.onViewportChanged(Rect(0, 0, 500, 1000))
        assertThat(changed1).isTrue()
        pageLocations.add(paginationManager.pageLocations)
        val changed2 = paginationManager.onViewportChanged(Rect(0, 0, 500, 1000))
        assertThat(changed2).isFalse()
        pageLocations.add(paginationManager.pageLocations)

        // We expect 2 unique values: the default empty value and one update based on one viewport
        // change
        assertThat(pageLocations.size).isEqualTo(3)
        val firstLocations = pageLocations[1]
        assertThat(firstLocations.contentEquals(pageLocations[2])).isTrue()
        // Before we learn the viewport, we don't know what's visible
        assertThat(pageLocations[0].size()).isEqualTo(0)
        // First viewport should include pages 0-5
        assertThat(firstLocations.size()).isEqualTo(5)
        assertThat(firstLocations.keyAt(0)).isEqualTo(0)
        assertThat(firstLocations.keyAt(4)).isEqualTo(4)
        var lastPageTop = -1
        for (page in firstLocations.keyIterator()) {
            val thisPageTop = firstLocations.get(page).top
            assertThat(thisPageTop).isGreaterThan(lastPageTop)
            lastPageTop = thisPageTop
        }
    }

    @Test
    fun increaseReach_belowCurrentReach() = runTest {
        // Start collecting from PaginationManager#dimensions
        val dimensionsValues = mutableListOf<Pair<Int, Point>>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            paginationManager.dimensions.toList(dimensionsValues)
        }

        // Increase reach to 10 and make sure we requested and collected 11 values
        paginationManager.increaseReach(10)
        testScope.testScheduler.runCurrent()
        verify(pdfDocument, times(11)).getPageInfo(any())
        assertThat(paginationManager.reach).isEqualTo(10)
        assertThat(dimensionsValues.size).isEqualTo(11)

        // Decrease reach to 8 and make sure we didn't request or collect any *more* values, and
        // that our reach remains 10
        paginationManager.increaseReach(8)
        testScope.testScheduler.runCurrent()
        verify(pdfDocument, times(11)).getPageInfo(any())
        assertThat(paginationManager.reach).isEqualTo(10)
        assertThat(dimensionsValues.size).isEqualTo(11)
    }

    @Test
    fun increaseReach_aboveCurrentReach() = runTest {
        // Start collecting from PaginationManager#dimensions
        val dimensionsValues = mutableListOf<Pair<Int, Point>>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            paginationManager.dimensions.toList(dimensionsValues)
        }

        // Increase reach to 10 and make sure we requested and collected 11 values
        paginationManager.increaseReach(10)
        testScope.testScheduler.runCurrent()
        verify(pdfDocument, times(11)).getPageInfo(any())
        assertThat(paginationManager.reach).isEqualTo(10)
        assertThat(dimensionsValues.size).isEqualTo(11)

        // Increase reach to 20 and make sure we requested and collected 21 total values
        paginationManager.increaseReach(20)
        testScope.testScheduler.runCurrent()
        verify(pdfDocument, times(21)).getPageInfo(any())
        assertThat(paginationManager.reach).isEqualTo(20)
        assertThat(dimensionsValues.size).isEqualTo(21)
    }

    @Test
    fun increaseReach_toEnd() = runTest {
        // Start collecting from PaginationManager#dimensions
        val dimensionsValues = mutableListOf<Pair<Int, Point>>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            paginationManager.dimensions.toList(dimensionsValues)
        }

        // Increase reach beyond the end of the document, and make sure we requested and collected
        // only the appropriate number of values
        paginationManager.increaseReach(TOTAL_PAGES + 10)
        testScope.testScheduler.runCurrent()
        verify(pdfDocument, times(TOTAL_PAGES)).getPageInfo(any())
        assertThat(paginationManager.reach).isEqualTo(TOTAL_PAGES - 1)
        assertThat(dimensionsValues.size).isEqualTo(TOTAL_PAGES)
    }
}

private val Rect.area: Int
    get() = width() * height()

private const val TOTAL_PAGES = 100
private const val PAGE_WIDTH = 100
private const val PAGE_HEIGHT = 200
