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

package androidx.pdf.viewer.scuba

import android.content.pm.ActivityInfo
import android.os.Build
import androidx.annotation.RequiresExtension
import androidx.fragment.app.testing.FragmentScenario
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.lifecycle.Lifecycle
import androidx.pdf.util.Preconditions
import androidx.pdf.viewer.FragmentUtils.scenarioLoadDocument
import androidx.pdf.viewer.TestPdfViewerFragment
import androidx.pdf.viewer.fragment.R
import androidx.pdf.viewer.scuba.ScubaConstants.FAST_SCROLLER_AND_FAB_SHOWN_ON_SCROLL_TO_TOP
import androidx.pdf.viewer.scuba.ScubaConstants.FAST_SCROLLER_HIDDEN_ON_LOAD
import androidx.pdf.viewer.scuba.ScubaConstants.FAST_SCROLLER_SHOWN_IN_IMMERSIVE_MODE
import androidx.pdf.viewer.scuba.ScubaConstants.SCREENSHOT_GOLDEN_DIRECTORY
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.action.ViewActions.swipeDown
import androidx.test.espresso.action.ViewActions.swipeUp
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.test.screenshot.AndroidXScreenshotTestRule
import org.junit.After
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private const val TEST_DOCUMENT_FILE = "sample.pdf"

@RequiresExtension(extension = Build.VERSION_CODES.S, version = 13)
@SdkSuppress(minSdkVersion = 35)
@RunWith(AndroidJUnit4::class)
@LargeTest
class PdfFastScrollerTest {

    @get:Rule val screenshotRule = AndroidXScreenshotTestRule(SCREENSHOT_GOLDEN_DIRECTORY)

    private lateinit var scenario: FragmentScenario<TestPdfViewerFragment>

    @Before
    fun setup() {
        scenario =
            launchFragmentInContainer<TestPdfViewerFragment>(
                themeResId =
                    com.google.android.material.R.style.Theme_Material3_DayNight_NoActionBar,
                initialState = Lifecycle.State.INITIALIZED
            )
        scenario.onFragment { fragment ->
            // Register idling resource
            IdlingRegistry.getInstance()
                .register(fragment.pdfLoadingIdlingResource.countingIdlingResource)
        }
    }

    @After
    fun cleanup() {
        scenario.onFragment { fragment ->
            // Un-register idling resource
            IdlingRegistry.getInstance()
                .unregister(fragment.pdfLoadingIdlingResource.countingIdlingResource)
        }
        scenario.close()
    }

    @Ignore("TODO(b/414504429): Re-enable when metrics API for page rendering is available.")
    @Test
    fun pdfFragment_fastScroller_hiddenOnLoad() {
        scenarioLoadDocument(
            scenario = scenario,
            filename = TEST_DOCUMENT_FILE,
            nextState = Lifecycle.State.STARTED,
            orientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT,
        ) {
            // Loading view assertion
            onView(withId(R.id.pdfLoadingProgressBar)).check(matches(isDisplayed()))
        }

        onView(withId(R.id.pdfLoadingProgressBar))
            .check(matches(withEffectiveVisibility(ViewMatchers.Visibility.GONE)))

        assertScreenshot(screenshotRule, FAST_SCROLLER_HIDDEN_ON_LOAD)
    }

    @Test
    fun pdfFragment_fastScroller_shownInImmersiveMode() {
        scenarioLoadDocument(
            scenario = scenario,
            filename = TEST_DOCUMENT_FILE,
            nextState = Lifecycle.State.STARTED,
            orientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT,
        ) {
            // Loading view assertion
            onView(withId(R.id.pdfLoadingProgressBar)).check(matches(isDisplayed()))
        }

        onView(withId(R.id.pdfLoadingProgressBar))
            .check(matches(withEffectiveVisibility(ViewMatchers.Visibility.GONE)))

        // Swipe actions
        onView(withId(R.id.pdfView)).perform(swipeUp())
        assertScreenshot(screenshotRule, FAST_SCROLLER_SHOWN_IN_IMMERSIVE_MODE)
    }

    @Test
    fun pdfFragment_fastScroller_and_fab_shownOnScrollToTop() {
        scenarioLoadDocument(
            scenario = scenario,
            filename = TEST_DOCUMENT_FILE,
            nextState = Lifecycle.State.STARTED,
            orientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT,
        ) {
            // Loading view assertion
            onView(withId(R.id.pdfLoadingProgressBar)).check(matches(isDisplayed()))
        }

        onView(withId(R.id.pdfLoadingProgressBar))
            .check(matches(withEffectiveVisibility(ViewMatchers.Visibility.GONE)))

        scenario.onFragment {
            Preconditions.checkArgument(
                it.documentLoaded,
                "Unable to load document due to ${it.documentError?.message}"
            )
            it.setIsAnnotationIntentResolvable(true)
            it.isToolboxVisible = true
        }

        // Swipe actions
        onView(withId(R.id.pdfView)).perform(swipeUp())
        onView(withId(R.id.pdfView)).perform(swipeDown())

        assertScreenshot(screenshotRule, FAST_SCROLLER_AND_FAB_SHOWN_ON_SCROLL_TO_TOP)
    }
}
