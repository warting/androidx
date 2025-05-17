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

package androidx.pdf

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.PointF
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.os.Build
import android.view.InputDevice
import android.view.MotionEvent
import androidx.annotation.RequiresExtension
import androidx.core.os.OperationCanceledException
import androidx.fragment.app.testing.FragmentScenario
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.lifecycle.Lifecycle
import androidx.pdf.FragmentUtils.scenarioLoadDocument
import androidx.pdf.TestUtils.waitFor
import androidx.pdf.actions.SelectionViewActions
import androidx.pdf.actions.clickOnPdfPoint
import androidx.pdf.matchers.SearchViewAssertions
import androidx.pdf.util.Preconditions
import androidx.pdf.view.PdfPoint
import androidx.pdf.view.PdfView
import androidx.pdf.view.fastscroll.FastScrollDrawer
import androidx.pdf.view.fastscroll.FastScroller
import androidx.pdf.viewer.fragment.R as PdfR
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.action.GeneralClickAction
import androidx.test.espresso.action.GeneralSwipeAction
import androidx.test.espresso.action.Press
import androidx.test.espresso.action.Swipe
import androidx.test.espresso.action.Tap
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.swipeDown
import androidx.test.espresso.action.ViewActions.swipeUp
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intending
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.espresso.matcher.RootMatchers
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiSelector
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 35)
@RequiresExtension(extension = Build.VERSION_CODES.S, version = 13)
class PdfViewerFragmentV2TestSuite {

    private lateinit var scenario: FragmentScenario<TestPdfViewerFragment>

    @Before
    fun setup() {
        Intents.init()
        scenario =
            launchFragmentInContainer<TestPdfViewerFragment>(
                themeResId =
                    com.google.android.material.R.style.Theme_Material3_DayNight_NoActionBar,
                initialState = Lifecycle.State.INITIALIZED,
            )
        scenario.onFragment { fragment ->
            // Register idling resource
            IdlingRegistry.getInstance()
                .register(fragment.pdfLoadingIdlingResource.countingIdlingResource)
            IdlingRegistry.getInstance()
                .register(fragment.pdfScrollIdlingResource.countingIdlingResource)
            IdlingRegistry.getInstance()
                .register(fragment.pdfSearchViewVisibleIdlingResource.countingIdlingResource)
        }
    }

    @After
    fun cleanup() {
        scenario.onFragment { fragment ->
            // Un-register idling resource
            IdlingRegistry.getInstance()
                .unregister(fragment.pdfLoadingIdlingResource.countingIdlingResource)
            IdlingRegistry.getInstance()
                .unregister(fragment.pdfScrollIdlingResource.countingIdlingResource)
            IdlingRegistry.getInstance()
                .unregister(fragment.pdfSearchViewVisibleIdlingResource.countingIdlingResource)
        }
        scenario.close()
        Intents.release()
    }

    @Test
    fun testPdfViewerFragment_setDocumentUri() {
        scenarioLoadDocument(
            scenario = scenario,
            filename = TEST_DOCUMENT_FILE,
            nextState = Lifecycle.State.STARTED,
            orientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT,
        ) {
            // Loading view assertion
            onView(withId(PdfR.id.pdfLoadingProgressBar)).check(matches(isDisplayed()))
        }

        onView(withId(PdfR.id.pdfLoadingProgressBar))
            .check(matches(withEffectiveVisibility(ViewMatchers.Visibility.GONE)))
        scenario.onFragment {
            Preconditions.checkArgument(
                it.documentLoaded,
                "Unable to load document due to ${it.documentError?.message}",
            )
        }

        // Swipe actions
        onView(withId(PdfR.id.pdfView)).perform(swipeUp())
        scenario.onFragment { it.pdfScrollIdlingResource.increment() }

        // Espresso will wait on the idling resource on the next action performed hence adding a
        // click which is essentially a no-op
        onView(withId(PdfR.id.pdfView)).perform(click())
        // Check if the scrubber is visible
        withPdfView(scenario) { _, _, fastScrollThumb ->
            assertTrue(fastScrollThumb.alpha == FastScrollDrawer.VISIBLE_ALPHA)
        }

        // Scrubber should auto hide after animation and delay ends
        val totalTimeForScubberToHide =
            FastScroller.HIDE_ANIMATION_DURATION_MILLIS + FastScroller.HIDE_DELAY_MS
        onView(isRoot()).perform(waitFor(totalTimeForScubberToHide))
        withPdfView(scenario) { _, _, fastScrollThumb ->
            assertTrue(fastScrollThumb.alpha == FastScrollDrawer.GONE_ALPHA)
        }

        // Go back up and assert that the scrubber is visible again
        onView(withId(PdfR.id.pdfView)).perform(swipeDown())
        scenario.onFragment { it.pdfScrollIdlingResource.increment() }

        // Espresso will wait on the idling resource on the next action performed hence adding a
        // click which is essentially a no-op
        onView(withId(PdfR.id.pdfView)).perform(click())
        withPdfView(scenario) { _, _, fastScrollThumb ->
            assertTrue(fastScrollThumb.alpha == FastScrollDrawer.VISIBLE_ALPHA)
        }

        // Actions for scrolling by the scrubber
        var fastScrollScrubberClick: GeneralClickAction? = null
        var fastScrollScrubberSwipe: GeneralSwipeAction? = null

        // Used to compute the location of scrubber on view and set the gesture values
        withPdfView(scenario) { _, pdfView, fastScrollThumb ->
            val location = IntArray(2)
            pdfView.getLocationOnScreen(location) // Get location of the PDF View on the screen

            val thumbBounds = fastScrollThumb.bounds
            val thumbCenterX =
                location[0] +
                    pdfView.left +
                    thumbBounds.left +
                    thumbBounds.width() / 2 // X coordinate of the center
            val thumbCenterY =
                location[1] +
                    pdfView.top +
                    thumbBounds.top +
                    thumbBounds.height() / 2 // Y coordinate of the center

            fastScrollScrubberClick =
                GeneralClickAction(
                    Tap.SINGLE,
                    { floatArrayOf(thumbCenterX.toFloat(), thumbCenterY.toFloat()) },
                    Press.THUMB,
                    InputDevice.SOURCE_UNKNOWN,
                    MotionEvent.BUTTON_PRIMARY,
                )

            fastScrollScrubberSwipe =
                GeneralSwipeAction(
                    Swipe.FAST,
                    { floatArrayOf(thumbCenterX.toFloat(), thumbCenterY.toFloat()) },
                    { view ->
                        val endY = view.height + thumbCenterY.toFloat()
                        val endX = thumbCenterX.toFloat()
                        floatArrayOf(endX, endY)
                    },
                    Press.FINGER,
                )

            assertPageIndicatorLabel(
                actualLabel = pdfView.currentPageIndicatorLabel.trim(),
                expectedPage = 1,
                expectedTotalPages = 3,
            )
        }

        onView(isRoot()).perform(fastScrollScrubberClick!!)
        onView(isRoot()).perform(fastScrollScrubberSwipe!!)

        withPdfView(scenario) { _, pdfView, _ ->
            assertPageIndicatorLabel(
                actualLabel = pdfView.currentPageIndicatorLabel.trim(),
                expectedPage = 3,
                expectedTotalPages = 3,
            )
        }
    }

    @Test
    fun testPdfViewerFragment_isTextSearchActive_toggleMenu() {
        val uiDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        scenarioLoadDocument(
            scenario = scenario,
            filename = TEST_DOCUMENT_FILE,
            nextState = Lifecycle.State.STARTED,
            orientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT,
        ) {
            // Loading view assertion
            onView(withId(PdfR.id.pdfLoadingProgressBar)).check(matches(isDisplayed()))
        }

        onView(withId(PdfR.id.pdfLoadingProgressBar))
            .check(matches(withEffectiveVisibility(ViewMatchers.Visibility.GONE)))
        scenario.onFragment {
            Preconditions.checkArgument(
                it.documentLoaded,
                "Unable to load document due to ${it.documentError?.message}",
            )
        }

        // Toggle search menu
        val searchViewAssertion = SearchViewAssertions()
        scenario.onFragment { it.isTextSearchActive = true }
        onView(withId(PdfR.id.pdfSearchView)).check(matches(isDisplayed()))

        onView(withId(R.id.searchQueryBox)).perform(typeText(SEARCH_QUERY))
        onView(withId(R.id.matchStatusTextView)).check(matches(isDisplayed()))
        onView(withId(R.id.matchStatusTextView)).check(searchViewAssertion.extractAndMatch())

        // Prev/next search results
        onView(withId(R.id.findPrevButton)).perform(click())
        // TODO: Cleanup when idling resource is added
        onView(isRoot()).perform(waitFor(50))

        val keyboard = uiDevice.findObject(UiSelector().descriptionContains(KEYBOARD_CONTENT_DESC))
        // Assert keyboard is dismissed on clicking prev/next
        assertFalse(keyboard.exists())
        onView(withId(R.id.matchStatusTextView)).check(searchViewAssertion.matchPrevious())
        onView(withId(R.id.findNextButton)).perform(click())
        onView(withId(R.id.matchStatusTextView)).check(searchViewAssertion.matchNext())
        onView(withId(R.id.findNextButton)).perform(click())
        onView(withId(R.id.matchStatusTextView)).check(searchViewAssertion.matchNext())

        // Assert for keyboard collapse
        onView(withId(R.id.searchQueryBox)).perform(click())
        onView(withId(R.id.closeButton)).perform(click())
        onView(withId(R.id.searchQueryBox))
            .check(matches(withEffectiveVisibility(ViewMatchers.Visibility.GONE)))
    }

    @Test
    fun testPdfViewerFragment_onLoadDocumentError_corruptPdf() {
        scenarioLoadDocument(
            scenario = scenario,
            filename = TEST_CORRUPTED_DOCUMENT_FILE,
            nextState = Lifecycle.State.STARTED,
            orientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT,
        ) {
            // Loading view assertion
            onView(withId(PdfR.id.pdfLoadingProgressBar)).check(matches(isDisplayed()))
        }

        onView(withId(R.id.errorTextView)).check(matches(isDisplayed()))
        scenario.onFragment { fragment ->
            Preconditions.checkArgument(
                fragment.documentError is RuntimeException,
                "Exception is of incorrect type ${fragment.documentError}",
            )
            Preconditions.checkArgument(
                fragment.documentError
                    ?.message
                    .equals(fragment.resources.getString(R.string.pdf_error)),
                "Incorrect exception returned ${fragment.documentError?.message}",
            )
        }
    }

    @Test
    fun testPdfViewerFragment_whenFindInFileIsVisible_scrubberShouldBeInvisible() {
        scenarioLoadDocument(
            scenario = scenario,
            filename = TEST_DOCUMENT_FILE,
            nextState = Lifecycle.State.STARTED,
            orientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT,
        ) {
            // Loading view assertion
            onView(withId(PdfR.id.pdfLoadingProgressBar)).check(matches(isDisplayed()))
        }

        onView(withId(PdfR.id.pdfLoadingProgressBar))
            .check(matches(withEffectiveVisibility(ViewMatchers.Visibility.GONE)))
        scenario.onFragment {
            Preconditions.checkArgument(
                it.documentLoaded,
                "Unable to load document due to ${it.documentError?.message}",
            )
        }

        // Check if the scrubber is initially visible
        onView(withId(PdfR.id.pdfView)).perform(swipeUp())
        scenario.onFragment { it.pdfScrollIdlingResource.increment() }

        // Espresso will wait on the idling resource on the next action performed hence adding a
        // click which is essentially a no-op
        onView(withId(PdfR.id.pdfView)).perform(click())

        withPdfView(scenario) { _, _, fastScrollThumb ->
            assertTrue(fastScrollThumb.alpha == FastScrollDrawer.VISIBLE_ALPHA)
        }
        withPdfView(scenario) { _, _, fastScrollPageIndicator ->
            assertTrue(fastScrollPageIndicator.alpha == FastScrollDrawer.VISIBLE_ALPHA)
        }

        // Enable FindInFile and verify the fast scroller visibility (i.e. should be hidden)
        scenario.onFragment { it.pdfSearchViewVisibleIdlingResource.increment() }
        scenario.onFragment { it.isTextSearchActive = true }
        onView(withId(PdfR.id.pdfSearchView)).check(matches(isDisplayed()))
        onView(withId(R.id.searchQueryBox)).perform(typeText(SEARCH_QUERY))

        // Check if the scrubber is invisible
        val totalTimeForScubberToHide =
            FastScroller.HIDE_ANIMATION_DURATION_MILLIS + FastScroller.HIDE_DELAY_MS
        onView(isRoot()).perform(waitFor(totalTimeForScubberToHide))
        withPdfView(scenario) { _, _, fastScrollThumb ->
            assertTrue(fastScrollThumb.alpha == FastScrollDrawer.GONE_ALPHA)
        }
        withPdfView(scenario) { _, _, fastScrollPageIndicator ->
            assertTrue(fastScrollPageIndicator.alpha == FastScrollDrawer.GONE_ALPHA)
        }

        // Re-assert that the fast scroller remains invisible after scrolling
        withPdfView(scenario) { _, _, fastScrollThumb ->
            assertTrue(fastScrollThumb.alpha == FastScrollDrawer.GONE_ALPHA)
        }
        withPdfView(scenario) { _, _, fastScrollPageIndicator ->
            assertTrue(fastScrollPageIndicator.alpha == FastScrollDrawer.GONE_ALPHA)
        }

        // Disable FindInFile and verify the fast scroller visibility (i.e. should be shown)
        onView(withId(R.id.searchQueryBox)).perform(click())
        onView(withId(R.id.closeButton)).perform(click())
        onView(withId(R.id.searchQueryBox))
            .check(matches(withEffectiveVisibility(ViewMatchers.Visibility.GONE)))
    }

    /**
     * This test verifies the behavior of the Pdf viewer in immersive mode, specifically the
     * visibility of the toolbox
     */
    @Test
    fun testPdfViewerFragment_immersiveMode_toggleMenu() {
        scenarioLoadDocument(
            scenario = scenario,
            filename = TEST_DOCUMENT_FILE,
            nextState = Lifecycle.State.STARTED,
            orientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT,
        ) {
            // Loading view assertion
            onView(withId(PdfR.id.pdfLoadingProgressBar)).check(matches(isDisplayed()))
        }

        onView(withId(PdfR.id.pdfLoadingProgressBar))
            .check(matches(withEffectiveVisibility(ViewMatchers.Visibility.GONE)))
        scenario.onFragment {
            it.setIsAnnotationIntentResolvable(true)
            Preconditions.checkArgument(
                it.documentLoaded,
                "Unable to load document due to ${it.documentError?.message}",
            )
        }

        // Show the toolbox and check visibility
        scenario.onFragment { it.isToolboxVisible = true }
        onView(withId(R.id.edit_fab)).check(matches(isDisplayed()))

        // Hide the toolbox and check visibility
        scenario.onFragment { it.isToolboxVisible = false }

        onView(withId(R.id.edit_fab))
            .check(matches(withEffectiveVisibility(ViewMatchers.Visibility.GONE)))

        // Show the toolbox and check visibility
        scenario.onFragment { it.isToolboxVisible = true }
        onView(withId(R.id.edit_fab)).check(matches(isDisplayed()))

        // Swipe down to hide the toolbox and check visibility
        onView(withId(PdfR.id.pdfView)).perform(swipeUp())
        scenario.onFragment { it.pdfScrollIdlingResource.increment() }
        onView(withId(R.id.edit_fab))
            .check(matches(withEffectiveVisibility(ViewMatchers.Visibility.GONE)))

        // Swipe up to top of pdf show the toolbox and check visibility
        onView(withId(PdfR.id.pdfView)).perform(swipeDown())
        scenario.onFragment { it.pdfScrollIdlingResource.increment() }
        onView(withId(R.id.edit_fab)).check(matches(isDisplayed()))

        // Enter immersive mode and check visibility
        scenario.onFragment { it.onRequestImmersiveMode(true) }
        onView(withId(R.id.edit_fab))
            .check(matches(withEffectiveVisibility(ViewMatchers.Visibility.GONE)))

        // Exit immersive mode and check visibility
        scenario.onFragment { it.onRequestImmersiveMode(false) }
        onView(withId(R.id.edit_fab)).check(matches(isDisplayed()))

        // Click the host app search button and check visibility
        scenario.onFragment {
            it.pdfSearchViewVisibleIdlingResource.increment()
            it.isTextSearchActive = true
        }
        onView(withId(PdfR.id.pdfSearchView)).check(matches(isDisplayed()))
        onView(withId(R.id.edit_fab))
            .check(matches(withEffectiveVisibility(ViewMatchers.Visibility.GONE)))
    }

    @Test
    fun testPdfViewerFragment_dismissPasswordDialog() {

        scenarioLoadDocument(
            scenario = scenario,
            filename = TEST_PROTECTED_DOCUMENT_FILE,
            nextState = Lifecycle.State.STARTED,
            orientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT,
        ) {
            // Loading view assertion
            onView(withId(PdfR.id.pdfLoadingProgressBar)).check(matches(isDisplayed()))
        }
        scenario.onFragment { fragment ->
            fragment.pdfLoadingIdlingResource.decrement()
            assertNull(fragment.documentError)
        }

        onView(ViewMatchers.withText(CANCEL)).inRoot(RootMatchers.isDialog()).perform(click())

        scenario.onFragment { fragment ->
            assert(fragment.documentError is OperationCanceledException)
        }
    }

    @Test
    fun testPdfViewerFragment_whenSelectAllClicked_allContentShouldBeSelected() {
        // Load the document and assert loading view is displayed
        scenarioLoadDocument(
            scenario = scenario,
            filename = TEST_DOCUMENT_SELECT,
            nextState = Lifecycle.State.STARTED,
            orientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT,
        ) {
            onView(withId(PdfR.id.pdfLoadingProgressBar)).check(matches(isDisplayed()))
        }

        // Assert loading progress bar is gone and document is loaded
        onView(withId(PdfR.id.pdfLoadingProgressBar))
            .check(matches(withEffectiveVisibility(ViewMatchers.Visibility.GONE)))
        scenario.onFragment {
            Preconditions.checkArgument(
                it.documentLoaded,
                "Unable to load document due to ${it.documentError?.message}",
            )
        }

        // The exact View position of any piece of text will vary by device, scroll position, zoom
        // level, etc. Act on an absolute PDF coordinate that's known to contain text instead.
        val pdfPointWithText = PdfPoint(pageNum = 0, pagePoint = PointF(297.22455F, 619.1273F))
        onView(withId(androidx.pdf.viewer.fragment.R.id.pdfView))
            .perform(clickOnPdfPoint(pdfPointWithText, Tap.LONG))
        onView(ViewMatchers.withText(SELECT_ALL))
            .inRoot(RootMatchers.isPlatformPopup())
            .perform(click())

        // Get the PdfView instance and assert selection
        var pdfView: PdfView? = null
        val expectedSelectionBoundsSize = 34
        scenario.onFragment { fragment -> pdfView = fragment.getPdfViewInstance() }
        assertNotNull(pdfView)
        val selection = pdfView?.currentSelection
        assertNotNull(selection)
        assertNotNull(selection?.bounds)
        assert(selection?.bounds?.size == expectedSelectionBoundsSize)
    }

    @Test
    fun testPdfViewerFragment_customLinkHandler_isCalledAndOverridesDefault() {
        scenario.onFragment { fragment -> fragment.shouldOverrideLinkHandling = true }

        scenarioLoadDocument(
            scenario = scenario,
            filename = TEST_SAMPLE_LINKS_FILE,
            nextState = Lifecycle.State.STARTED,
            orientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT,
        ) {
            // Loading view assertion
            onView(withId(PdfR.id.pdfLoadingProgressBar)).check(matches(isDisplayed()))
        }

        onView(withId(PdfR.id.pdfLoadingProgressBar))
            .check(matches(withEffectiveVisibility(ViewMatchers.Visibility.GONE)))
        scenario.onFragment {
            Preconditions.checkArgument(
                it.documentLoaded,
                "Unable to load document due to ${it.documentError?.message}",
            )
        }

        val selectionViewActions = SelectionViewActions()

        // PDF Link coordinates for sample link PDF
        val linkBounds = RectF(89.0f, 311.0f, 236.0f, 327.0f)
        onView(withId(PdfR.id.pdfView)).perform(selectionViewActions.tapOnPosition(linkBounds))

        onView(withText("Handled by custom link handler"))
            .inRoot(isDialog())
            .check(matches(isDisplayed()))
    }

    @Test
    fun testPdfViewerFragment_defaultLinkHandling_launchesBrowserIntent() {
        scenario.onFragment { fragment -> fragment.shouldOverrideLinkHandling = false }

        scenarioLoadDocument(
            scenario = scenario,
            filename = TEST_SAMPLE_LINKS_FILE,
            nextState = Lifecycle.State.STARTED,
            orientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT,
        ) {
            // Loading view assertion
            onView(withId(PdfR.id.pdfLoadingProgressBar)).check(matches(isDisplayed()))
        }

        onView(withId(PdfR.id.pdfLoadingProgressBar))
            .check(matches(withEffectiveVisibility(ViewMatchers.Visibility.GONE)))
        scenario.onFragment {
            Preconditions.checkArgument(
                it.documentLoaded,
                "Unable to load document due to ${it.documentError?.message}",
            )
        }

        intending(hasAction(Intent.ACTION_VIEW))
            .respondWith(Instrumentation.ActivityResult(Activity.RESULT_OK, null))

        val selectionViewActions = SelectionViewActions()

        // PDF Link coordinates for sample link PDF
        val linkBounds = RectF(89.0f, 311.0f, 236.0f, 327.0f)
        onView(withId(PdfR.id.pdfView)).perform(selectionViewActions.tapOnPosition(linkBounds))

        val capturedIntent = Intents.getIntents().firstOrNull()
        assertNotNull(
            "Expected an external link intent to be launched, but it was null.",
            capturedIntent,
        )
    }

    private fun withPdfView(
        scenario: FragmentScenario<TestPdfViewerFragment>,
        callback: (TestPdfViewerFragment, PdfView, Drawable) -> Unit,
    ) {
        scenario.onFragment { fragment ->
            assertNotNull(
                "Fast scroll thumb cannot be null",
                fragment.getPdfViewInstance().fastScrollVerticalThumbDrawable,
            )
            val fastScrollThumb = fragment.getPdfViewInstance().fastScrollVerticalThumbDrawable!!
            assertNotNull("Fast scroll thumbnail cannot be null", fastScrollThumb)
            callback(fragment, fragment.getPdfViewInstance(), fastScrollThumb)
        }
    }

    companion object {
        private const val TEST_DOCUMENT_FILE = "sample.pdf"
        private const val TEST_PROTECTED_DOCUMENT_FILE = "sample-protected.pdf"
        private const val TEST_CORRUPTED_DOCUMENT_FILE = "corrupted.pdf"
        private const val TEST_DOCUMENT_SELECT = "sample-select.pdf"
        private const val TEST_SAMPLE_LINKS_FILE = "sample_links.pdf"

        private const val SELECT_ALL = "Select all"
        private const val CANCEL = "Cancel"
        private const val SEARCH_QUERY = "ipsum"
        private const val KEYBOARD_CONTENT_DESC = "keyboard"

        private fun assertPageIndicatorLabel(
            actualLabel: String,
            expectedPage: Int,
            expectedTotalPages: Int,
        ) {
            TestUtils.extractFromLabel(actualLabel) { currentPage, totalPages ->
                assertTrue(
                    "Actual page $currentPage does not match expected $expectedPage",
                    currentPage == expectedPage,
                )
                assertTrue(
                    "Actual total pages $totalPages does not match expected $expectedTotalPages",
                    currentPage == expectedPage,
                )
            }
        }
    }
}
