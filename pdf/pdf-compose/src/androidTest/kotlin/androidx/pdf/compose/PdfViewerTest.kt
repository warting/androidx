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

import android.content.Context
import android.graphics.Point
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.pdf.view.PdfView
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class PdfViewerTest {
    @get:Rule val rule = createComposeRule()

    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test
    fun pdfViewerState_noDocument_defaults() = runTest {
        lateinit var pdfViewerState: PdfViewerState
        rule.setContent {
            pdfViewerState = rememberPdfViewerState()
            PdfViewer(
                state = pdfViewerState,
                pdfDocument = null,
            )
        }

        assertThat(pdfViewerState.firstVisiblePage).isEqualTo(0)
        assertThat(pdfViewerState.visiblePagesCount).isEqualTo(0)
        assertThat(pdfViewerState.zoom).isEqualTo(PdfView.DEFAULT_INIT_ZOOM)
        assertThat(pdfViewerState.pageOffsets.keys).isEmpty()
    }

    @Test
    fun pdfViewerState_onePagePdf_defaults() = runTest {
        val pdfDocument = FakePdfDocument(List(10) { Point(425, 550) })

        lateinit var pdfViewerState: PdfViewerState
        rule.setContent {
            pdfViewerState = rememberPdfViewerState()
            PdfViewer(
                modifier = Modifier.size(width = 850.toDp(context), height = 1100.toDp(context)),
                state = pdfViewerState,
                pdfDocument = pdfDocument,
            )
        }

        assertThat(pdfViewerState.firstVisiblePage).isEqualTo(0)
        assertThat(pdfViewerState.visiblePagesCount).isEqualTo(1)
        // By default, PdfViewer will fit the content to its own width. Since we configured the View
        // to be exactly twice as wide as the Composable, we expect zoom to be 2.0
        assertThat(pdfViewerState.zoom).isEqualTo(2.0F)
        assertThat(pdfViewerState.pageOffsets.keys).containsExactly(0)
    }

    @Test
    fun pdfViewerState_scrollUpAndDown() {
        val pdfDocument = FakePdfDocument(List(10) { Point(425, 550) })
        val pageZeroTopPositions = FloatArray(3)

        lateinit var pdfViewerState: PdfViewerState
        rule.setContent {
            pdfViewerState = rememberPdfViewerState()
            PdfViewer(
                modifier =
                    Modifier.size(width = 850.toDp(context), height = 550.toDp(context))
                        .testTag(PDF_VIEW_TAG),
                state = pdfViewerState,
                pdfDocument = pdfDocument,
            )
        }
        pageZeroTopPositions[0] = requireNotNull(pdfViewerState.pageOffsets[0]).y

        rule.onNodeWithTag(PDF_VIEW_TAG).performTouchInput {
            swipeUp()
            // Click will stop a fling, if one starts, i.e. to make it easier to reason about where
            // we expect the page to be
            click()
        }
        pageZeroTopPositions[1] = requireNotNull(pdfViewerState.pageOffsets[0]).y

        rule.onNodeWithTag(PDF_VIEW_TAG).performTouchInput {
            swipeDown()
            // Click will stop a fling, if one starts, i.e. to make it easier to reason about where
            // we expect the page to be
            click()
        }
        pageZeroTopPositions[2] = requireNotNull(pdfViewerState.pageOffsets[0]).y

        // Swipe up -> page 0 has a negative offset (i.e. above the top of the Composable)
        assertThat(pageZeroTopPositions[1]).isLessThan(pageZeroTopPositions[0])
        // Swipe down -> page 0 offset becomes more positive (i.e. moves towards the bottom of the
        // Composable)
        assertThat(pageZeroTopPositions[2]).isGreaterThan(pageZeroTopPositions[1])
    }

    @Test fun pdfViewerState_scrollLeftAndRight() {}

    @Test fun pdfViewerState_coordinateTranslation() {}
}

const val PDF_VIEW_TAG = "PdfView"

fun Int.toDp(context: Context): Dp {
    return (this.toFloat() / context.resources.displayMetrics.density).dp
}
