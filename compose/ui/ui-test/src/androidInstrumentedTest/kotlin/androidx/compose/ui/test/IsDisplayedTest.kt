/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.compose.ui.test

import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.util.BoundaryNode
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.ViewInteraction
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withParent
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertFailsWith
import org.hamcrest.CoreMatchers.allOf
import org.hamcrest.CoreMatchers.not
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@MediumTest
@RunWith(Parameterized::class)
class IsDisplayedTest(val config: TestConfig) {
    data class TestConfig(val activityClass: Class<out ComponentActivity>)

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun createTestSet(): List<TestConfig> =
            listOf(
                TestConfig(ComponentActivity::class.java),
                TestConfig(CustomComposeHostActivity::class.java),
            )
    }

    @get:Rule val rule = createAndroidComposeRule(config.activityClass)

    private val colors = listOf(Color.Red, Color.Green, Color.Blue)

    @Composable
    private fun Item(i: Int, width: Dp? = null, height: Dp? = null) {
        BoundaryNode("item$i") {
            Box(
                modifier =
                    with(Modifier) { width?.let { requiredWidth(it) } ?: fillMaxWidth() }
                        .let {
                            with(it) { height?.let { requiredHeight(it) } ?: fillMaxHeight() }
                                .background(colors[i % colors.size])
                        }
            )
        }
    }

    @Composable
    fun PlaceConditionally(place: Boolean, content: @Composable () -> Unit) {
        Layout(content = content) { measurables, constraints ->
            if (place) {
                val placeable = measurables[0].measure(constraints)
                layout(placeable.width, placeable.height) { placeable.placeRelative(0, 0) }
            } else {
                layout(0, 0) {}
            }
        }
    }

    @Test
    fun componentInScrollable_isDisplayed() {
        setContent {
            Column(modifier = Modifier.requiredSize(100.dp).verticalScroll(rememberScrollState())) {
                repeat(10) { Item(it, height = 30.dp) }
            }
        }

        rule.onNodeWithTag("item0").assertIsDisplayed()
    }

    @Test
    fun componentInScrollable_isNotDisplayed() {
        setContent {
            Column(modifier = Modifier.requiredSize(100.dp).verticalScroll(rememberScrollState())) {
                repeat(10) { Item(it, height = 30.dp) }
            }
        }

        rule.onNodeWithTag("item4").assertIsNotDisplayed()
    }

    @Test
    fun togglePlacement() {
        var place by mutableStateOf(true)

        setContent {
            PlaceConditionally(place) {
                // Item instead of BoundaryNode because we need non-zero size
                Item(0)
            }
        }

        rule.onNodeWithTag("item0").assertIsDisplayed()

        rule.runOnIdle { place = false }

        rule.onNodeWithTag("item0").assertIsNotDisplayed()
    }

    @Test
    fun toggleParentPlacement() {
        var place by mutableStateOf(true)

        setContent {
            PlaceConditionally(place) {
                Box {
                    // Item instead of BoundaryNode because we need non-zero size
                    Item(0)
                }
            }
        }

        rule.onNodeWithTag("item0").assertIsDisplayed()

        rule.runOnIdle { place = false }

        rule.onNodeWithTag("item0").assertIsNotDisplayed()
    }

    @Test
    fun rowTooSmall() {
        setContent {
            Row(modifier = Modifier.requiredSize(100.dp)) { repeat(10) { Item(it, width = 30.dp) } }
        }

        rule.onNodeWithTag("item9").assertIsNotDisplayed()
    }

    @Test
    fun viewVisibility_androidComposeView() {
        lateinit var androidComposeView: View
        rule.activityRule.scenario.onActivity { activity ->
            // FrameLayout(id=100, w=100, h=100)
            // '- AndroidComposeView
            androidComposeView =
                ComposeView(activity)
                    .apply {
                        id = 100
                        layoutParams = ViewGroup.MarginLayoutParams(100, 100)
                        activity.setContentView(this)
                        setContent { Item(0) }
                    }
                    .getChildAt(0)
        }

        fun onComposeView(): ViewInteraction {
            return onView(allOf(withParent(withId(100))))
        }

        onComposeView().check(matches(isDisplayed()))
        rule.onNodeWithTag("item0").assertIsDisplayed()

        rule.runOnIdle { androidComposeView.visibility = View.GONE }

        onComposeView().check(matches(not(isDisplayed())))
        rule.onNodeWithTag("item0").assertIsNotDisplayed()
    }

    @Test
    fun viewVisibility_parentView() {
        lateinit var composeContainer: View
        rule.activityRule.scenario.onActivity { activity ->
            // FrameLayout
            // '- FrameLayout(id=100, w=100, h=100) -> composeContainer
            //    '- AndroidComposeView
            composeContainer =
                ComposeView(activity).apply {
                    id = 100
                    layoutParams = ViewGroup.MarginLayoutParams(100, 100)
                    activity.setContentView(FrameLayout(activity).also { it.addView(this) })
                    setContent { Item(0) }
                }
        }

        fun onComposeView(): ViewInteraction {
            return onView(allOf(withParent(withId(100))))
        }

        onComposeView().check(matches(isDisplayed()))
        rule.onNodeWithTag("item0").assertIsDisplayed()

        rule.runOnIdle { composeContainer.visibility = View.GONE }

        onComposeView().check(matches(not(isDisplayed())))
        rule.onNodeWithTag("item0").assertIsNotDisplayed()
    }

    @Test
    fun isDisplayed_throws_whenMultipleNodesMatch() {
        setContent {
            Item(0)
            Item(0)
        }

        val interaction = rule.onNodeWithTag("item0")

        assertFailsWith<AssertionError> { interaction.isDisplayed() }
    }

    @Test
    fun isNotDisplayed_throws_whenMultipleNodesMatch() {
        setContent {
            Item(0)
            Item(0)
        }

        val interaction = rule.onNodeWithTag("item0")

        assertFailsWith<AssertionError> { interaction.isNotDisplayed() }
    }

    @Test
    fun isDisplayed_returnsFalse_whenNodeNotFound() {
        setContent {}

        assertThat(rule.onNodeWithTag("doesn't exist").isDisplayed()).isFalse()
    }

    @Test
    fun isNotDisplayed_returnsTrue_whenNodeNotFound() {
        setContent {}

        assertThat(rule.onNodeWithTag("doesn't exist").isNotDisplayed()).isTrue()
    }

    @Test
    fun isDisplayed_returnsFalse_whenNodeIsFullyHidden() {
        setContent { Box(Modifier.requiredSize(0.dp)) { Item(0, width = 10.dp, height = 10.dp) } }

        assertThat(rule.onNodeWithTag("item0").isDisplayed()).isFalse()
    }

    @Test
    fun isNotDisplayed_returnsTrue_whenNodeIsFullyHidden() {
        setContent { Box(Modifier.requiredSize(0.dp)) { Item(0, width = 10.dp, height = 10.dp) } }

        assertThat(rule.onNodeWithTag("item0").isNotDisplayed()).isTrue()
    }

    @Test
    fun isDisplayed_returnsTrue_whenNodeIsPartiallyVisible() {
        setContent { Box(Modifier.requiredSize(5.dp)) { Item(0, width = 10.dp, height = 10.dp) } }

        assertThat(rule.onNodeWithTag("item0").isDisplayed()).isTrue()
    }

    @Test
    fun isNotDisplayed_returnsFalse_whenNodeIsPartiallyVisible() {
        setContent { Box(Modifier.requiredSize(5.dp)) { Item(0, width = 10.dp, height = 10.dp) } }

        assertThat(rule.onNodeWithTag("item0").isNotDisplayed()).isFalse()
    }

    @Test
    fun isDisplayed_returnsTrue_whenNodeIsFullyVisible() {
        setContent { Item(0) }

        assertThat(rule.onNodeWithTag("item0").isDisplayed()).isTrue()
    }

    @Test
    fun isNotDisplayed_returnsFalse_whenNodeIsFullyVisible() {
        setContent { Item(0) }

        assertThat(rule.onNodeWithTag("item0").isNotDisplayed()).isFalse()
    }

    @Test
    fun isDisplayed_throws_whenNodeIsNotVisible() {
        setContent { Item(0) }
        val interaction = rule.onNodeWithTag("item1")
        val message = "Assert failed: The component with TestTag = 'item1' is not displayed!"
        val error = assertFailsWith<AssertionError> { interaction.assertIsDisplayed() }
        assertThat(error.message).isEqualTo(message)
    }

    @Test
    fun isNotDisplayed_throws_whenNodeIsVisible() {
        setContent { Item(0) }
        val interaction = rule.onNodeWithTag("item0")
        val message = "Assert failed: The component with TestTag = 'item0' is displayed!"
        val error = assertFailsWith<AssertionError> { interaction.assertIsNotDisplayed() }
        assertThat(error.message).isEqualTo(message)
    }

    @Test
    fun isDisplayed_throws_whenNodeIsNotVisibleWithText() {
        setContent { BasicText(text = "item0") }
        val interaction = rule.onNodeWithText("item1")
        val message =
            "Assert failed: The component with Text + InputText + EditableText contains 'item1' (ignoreCase: false) is not displayed!"
        val error = assertFailsWith<AssertionError> { interaction.assertIsDisplayed() }
        assertThat(error.message).isEqualTo(message)
    }

    @Test
    fun isNotDisplayed_throws_whenNodeIsVisibleWithText() {
        setContent { BasicText(text = "item0") }
        val interaction = rule.onNodeWithText("item0")
        val message =
            "Assert failed: The component with Text + InputText + EditableText contains 'item0' (ignoreCase: false) is displayed!"
        val error = assertFailsWith<AssertionError> { interaction.assertIsNotDisplayed() }
        assertThat(error.message).isEqualTo(message)
    }

    private fun setContent(content: @Composable () -> Unit) {
        when (val activity = rule.activity) {
            is CustomComposeHostActivity -> activity.setContent(content)
            else -> rule.setContent(content)
        }
    }
}
