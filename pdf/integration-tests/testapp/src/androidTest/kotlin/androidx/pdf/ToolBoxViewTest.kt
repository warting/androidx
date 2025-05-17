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

import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Build
import androidx.annotation.RequiresExtension
import androidx.fragment.app.testing.FragmentScenario
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.lifecycle.Lifecycle
import androidx.pdf.TestUtils.waitFor
import androidx.pdf.util.AnnotationUtils
import androidx.pdf.view.ToolBoxView
import androidx.pdf.view.ToolBoxView.Companion.EXTRA_STARTING_PAGE
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.espresso.intent.matcher.IntentMatchers.hasExtra
import androidx.test.espresso.intent.matcher.IntentMatchers.hasFlags
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import org.hamcrest.core.AllOf.allOf
import org.junit.After
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 35)
@RequiresExtension(extension = Build.VERSION_CODES.S, version = 13)
class ToolBoxViewTest {

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
            IdlingRegistry.getInstance()
                .register(fragment.pdfLoadingIdlingResource.countingIdlingResource)
            IdlingRegistry.getInstance()
                .register(fragment.pdfSearchFocusIdlingResource.countingIdlingResource)
        }
    }

    @After
    fun cleanup() {
        scenario.onFragment { fragment ->
            // Un-register idling resource
            IdlingRegistry.getInstance()
                .unregister(fragment.pdfLoadingIdlingResource.countingIdlingResource)
            IdlingRegistry.getInstance()
                .unregister(fragment.pdfSearchFocusIdlingResource.countingIdlingResource)
        }
        Intents.release()
        scenario.close()
    }

    private fun scenarioLoadDocument(
        filename: String,
        nextState: Lifecycle.State,
        orientation: Int,
    ): FragmentScenario<TestPdfViewerFragment> {
        val context = InstrumentationRegistry.getInstrumentation().context
        val inputStream = context.assets.open(filename)

        scenario.moveToState(nextState)
        scenario.onFragment { it.requireActivity().requestedOrientation = orientation }

        // Load the document in the fragment
        scenario.onFragment { fragment ->
            fragment.documentUri = TestUtils.saveStream(inputStream, fragment.requireContext())
        }

        return scenario
    }

    @Ignore("Annotation intent resolution will fail with file scheme. Convert to content scheme")
    @Test
    fun testEditButtonOnClickListener() {
        scenarioLoadDocument(
            TEST_DOCUMENT_FILE,
            Lifecycle.State.STARTED,
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT,
        )

        // TODO(b/387444890): Remove this once IdlingResources is used
        onView(isRoot()).perform(waitFor(2000))

        // Ensure the toolbox view is displayed
        onView(withId(androidx.pdf.viewer.fragment.R.id.toolBoxView)).check(matches(isDisplayed()))

        onView(withId(androidx.pdf.viewer.fragment.R.id.toolBoxView)).perform(click())

        // Verify that the action was performed (e.g., intent was launched)
        val expectedIntent =
            allOf(
                hasAction(AnnotationUtils.ACTION_ANNOTATE_PDF),
                hasFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION),
            )
        intended(expectedIntent)
    }

    @Ignore("Annotation intent resolution will fail with file scheme. Convert to content scheme")
    @Test
    fun testEditButtonOnClickListener_onSpecificPage() {
        scenarioLoadDocument(
            TEST_DOCUMENT_FILE,
            Lifecycle.State.STARTED,
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT,
        )

        // TODO(b/387444890): Remove this once IdlingResources is used
        onView(isRoot()).perform(waitFor(2000))

        val pageNum = 3
        scenario.onFragment { fragment ->
            fragment.view?.let {
                var toolBoxView: ToolBoxView? =
                    it.findViewById(androidx.pdf.viewer.fragment.R.id.toolBoxView) as ToolBoxView
                toolBoxView?.setOnCurrentPageRequested { pageNum }
            }
        }

        // Ensure the toolbox view is displayed
        onView(withId(androidx.pdf.viewer.fragment.R.id.toolBoxView)).check(matches(isDisplayed()))

        onView(withId(androidx.pdf.viewer.fragment.R.id.toolBoxView)).perform(click())

        // Verify that the action was performed (e.g., intent was launched)
        val expectedIntent =
            allOf(
                hasAction(AnnotationUtils.ACTION_ANNOTATE_PDF),
                hasFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION),
                hasExtra(EXTRA_STARTING_PAGE, pageNum),
            )
        intended(expectedIntent)
    }

    companion object {
        private const val TEST_DOCUMENT_FILE = "sample.pdf"
        private const val SEARCH_QUERY = "ipsum"
    }
}
