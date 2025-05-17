/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.wear.compose.material

import android.os.Build
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.SaveableStateHolder
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.TouchInjectionScope
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.test.swipeWithVelocity
import androidx.test.filters.SdkSuppress
import androidx.wear.compose.foundation.SwipeToDismissBoxState
import androidx.wear.compose.foundation.SwipeToDismissValue
import androidx.wear.compose.foundation.edgeSwipeToDismiss
import androidx.wear.compose.foundation.rememberSwipeToDismissBoxState
import com.google.common.truth.Truth.assertThat
import java.lang.Math.sin
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@SdkSuppress(maxSdkVersion = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
class SwipeToDismissBoxTest {
    @get:Rule val rule = createComposeRule()

    @Test
    fun supports_testtag() {
        rule.setContentWithTheme {
            val state = rememberSwipeToDismissBoxState()
            SwipeToDismissBox(state = state, modifier = Modifier.testTag(TEST_TAG)) {
                Text("Testing")
            }
        }

        rule.onNodeWithTag(TEST_TAG).assertExists()
    }

    @Test
    fun dismisses_when_swiped_right() =
        verifySwipe(gesture = { swipeRight() }, expectedToDismiss = true)

    @Test
    fun does_not_dismiss_when_swiped_left() =
        // Swipe left is met with resistance and is not a swipe-to-dismiss.
        verifySwipe(gesture = { swipeLeft() }, expectedToDismiss = false)

    @Test
    fun does_not_dismiss_when_swipe_right_incomplete() =
        // Execute a partial swipe over a longer-than-default duration so that there
        // is insufficient velocity to perform a 'fling'.
        verifySwipe(
            gesture = {
                swipeWithVelocity(
                    start = Offset(0f, centerY),
                    end = Offset(centerX / 2f, centerY),
                    endVelocity = 1.0f,
                )
            },
            expectedToDismiss = false,
        )

    @Test
    fun does_not_display_background_without_swipe() {
        rule.setContentWithTheme {
            val state = rememberSwipeToDismissBoxState()
            SwipeToDismissBox(state = state, modifier = Modifier.testTag(TEST_TAG)) { isBackground
                ->
                if (isBackground) Text(BACKGROUND_MESSAGE) else messageContent()
            }
        }

        rule.onNodeWithText(BACKGROUND_MESSAGE).assertDoesNotExist()
    }

    @Test
    fun does_not_dismiss_if_has_background_is_false() {
        var dismissed = false
        rule.setContentWithTheme {
            val state = rememberSwipeToDismissBoxState()
            LaunchedEffect(state.currentValue) {
                dismissed = state.currentValue == SwipeToDismissValue.Dismissed
            }
            SwipeToDismissBox(
                state = state,
                modifier = Modifier.testTag(TEST_TAG),
                hasBackground = false,
            ) {
                Text(CONTENT_MESSAGE, color = MaterialTheme.colors.onPrimary)
            }
        }

        rule.onNodeWithTag(TEST_TAG).performTouchInput({ swipeRight() })

        rule.runOnIdle { assertEquals(false, dismissed) }
    }

    @Test
    fun remembers_saved_state() {
        val showCounterForContent = mutableStateOf(true)
        rule.setContentWithTheme {
            val state = rememberSwipeToDismissBoxState()
            val holder = rememberSaveableStateHolder()
            LaunchedEffect(state.currentValue) {
                if (state.currentValue == SwipeToDismissValue.Dismissed) {
                    showCounterForContent.value = !showCounterForContent.value
                    state.snapTo(SwipeToDismissValue.Default)
                }
            }
            SwipeToDismissBox(
                state = state,
                modifier = Modifier.testTag(TEST_TAG),
                backgroundKey = if (showCounterForContent.value) TOGGLE_SCREEN else COUNTER_SCREEN,
                contentKey = if (showCounterForContent.value) COUNTER_SCREEN else TOGGLE_SCREEN,
                content = { isBackground ->
                    if (showCounterForContent.value xor isBackground) counterScreen(holder)
                    else toggleScreen(holder)
                },
            )
        }

        // Start with foreground showing Counter screen.
        rule.onNodeWithTag(COUNTER_SCREEN).assertTextContains("0")
        rule.onNodeWithTag(COUNTER_SCREEN).performClick()
        rule.waitForIdle()
        rule.onNodeWithTag(COUNTER_SCREEN).assertTextContains("1")

        // Swipe to switch to Toggle screen
        rule.onNodeWithTag(TEST_TAG).performTouchInput({ swipeRight() })
        rule.waitForIdle()
        rule.onNodeWithTag(TOGGLE_SCREEN).assertIsOff()
        rule.onNodeWithTag(TOGGLE_SCREEN).performClick()
        rule.waitForIdle()
        rule.onNodeWithTag(TOGGLE_SCREEN).assertIsOn()

        // Swipe back to Counter screen
        rule.onNodeWithTag(TEST_TAG).performTouchInput({ swipeRight() })
        rule.waitForIdle()
        rule.onNodeWithTag(COUNTER_SCREEN).assertTextContains("1")

        // Swipe back to Toggle screen
        rule.onNodeWithTag(TEST_TAG).performTouchInput({ swipeRight() })
        rule.waitForIdle()
        rule.onNodeWithTag(TOGGLE_SCREEN).assertIsOn()
    }

    @Test
    fun gives_top_swipe_box_gestures_when_nested() {
        var outerDismissed = false
        var innerDismissed = false
        rule.setContentWithTheme {
            val outerState = rememberSwipeToDismissBoxState()
            LaunchedEffect(outerState.currentValue) {
                outerDismissed = outerState.currentValue == SwipeToDismissValue.Dismissed
            }
            SwipeToDismissBox(
                state = outerState,
                modifier = Modifier.testTag("OUTER"),
                hasBackground = true,
            ) {
                Text("Outer", color = MaterialTheme.colors.onPrimary)
                val innerState = rememberSwipeToDismissBoxState()
                LaunchedEffect(innerState.currentValue) {
                    innerDismissed = innerState.currentValue == SwipeToDismissValue.Dismissed
                }
                SwipeToDismissBox(
                    state = innerState,
                    modifier = Modifier.testTag("INNER"),
                    hasBackground = true,
                ) {
                    Text(
                        text = "Inner",
                        color = MaterialTheme.colors.onPrimary,
                        modifier = Modifier.fillMaxWidth().testTag(TEST_TAG),
                    )
                }
            }
        }

        rule.onNodeWithTag(TEST_TAG).performTouchInput({ swipeRight() })

        rule.runOnIdle {
            assertEquals(true, innerDismissed)
            assertEquals(false, outerDismissed)
        }
    }

    @Composable
    fun toggleScreen(saveableStateHolder: SaveableStateHolder) {
        saveableStateHolder.SaveableStateProvider(TOGGLE_SCREEN) {
            var toggle by rememberSaveable { mutableStateOf(false) }
            ToggleButton(
                checked = toggle,
                onCheckedChange = { toggle = !toggle },
                content = { Text(text = if (toggle) TOGGLE_ON else TOGGLE_OFF) },
                modifier = Modifier.testTag(TOGGLE_SCREEN),
            )
        }
    }

    @Composable
    fun counterScreen(saveableStateHolder: SaveableStateHolder) {
        saveableStateHolder.SaveableStateProvider(COUNTER_SCREEN) {
            var counter by rememberSaveable { mutableStateOf(0) }
            Button(onClick = { ++counter }, modifier = Modifier.testTag(COUNTER_SCREEN)) {
                Text(text = "" + counter)
            }
        }
    }

    @Test
    fun displays_background_during_swipe() =
        verifyPartialSwipe(expectedMessage = BACKGROUND_MESSAGE)

    @Test
    fun displays_content_during_swipe() = verifyPartialSwipe(expectedMessage = CONTENT_MESSAGE)

    @Test
    fun calls_ondismissed_after_swipe_when_supplied() {
        var dismissed = false
        rule.setContentWithTheme {
            SwipeToDismissBox(
                onDismissed = { dismissed = true },
                modifier = Modifier.testTag(TEST_TAG),
            ) {
                Text(CONTENT_MESSAGE, color = MaterialTheme.colors.onPrimary)
            }
        }

        rule.onNodeWithTag(TEST_TAG).performTouchInput({ swipeRight() })

        rule.runOnIdle { assertEquals(true, dismissed) }
    }

    @Test
    fun edgeswipe_modifier_edge_swiped_right_dismissed() {
        verifyEdgeSwipeWithNestedScroll(gesture = { swipeRight() }, expectedToDismiss = true)
    }

    @Test
    fun edgeswipe_non_edge_swiped_right_with_offset_not_dismissed() {
        verifyEdgeSwipeWithNestedScroll(
            gesture = { swipeRight(200f, 400f) },
            expectedToDismiss = false,
            initialScrollState = 200,
        )
    }

    @Test
    fun edgeswipe_non_edge_swiped_right_without_offset_not_dismissed() {
        verifyEdgeSwipeWithNestedScroll(
            gesture = { swipeRight(200f, 400f) },
            expectedToDismiss = false,
            initialScrollState = 0,
        )
    }

    @Test
    fun edgeswipe_edge_swiped_left_not_dismissed() {
        verifyEdgeSwipeWithNestedScroll(
            gesture = { swipeLeft(20f, -40f) },
            expectedToDismiss = false,
        )
    }

    @Test
    fun edgeswipe_non_edge_swiped_left_not_dismissed() {
        verifyEdgeSwipeWithNestedScroll(
            gesture = { swipeLeft(200f, 0f) },
            expectedToDismiss = false,
        )
    }

    @Test
    fun edgeswipe_swipe_edge_content_was_not_swiped_right() {
        val initialScrollState = 200
        lateinit var horizontalScrollState: ScrollState
        rule.setContentWithTheme {
            val state = rememberSwipeToDismissBoxState()
            horizontalScrollState = rememberScrollState(initialScrollState)

            SwipeToDismissBox(state = state, modifier = Modifier.testTag(TEST_TAG)) {
                nestedScrollContent(state, horizontalScrollState)
            }
        }

        rule.onNodeWithTag(TEST_TAG).performTouchInput { swipeRight(0f, 200f) }
        rule.runOnIdle { assertThat(horizontalScrollState.value == initialScrollState).isTrue() }
    }

    @Test
    fun edgeswipe_swipe_non_edge_content_was_swiped_right() {
        val initialScrollState = 200
        lateinit var horizontalScrollState: ScrollState
        rule.setContentWithTheme {
            val state = rememberSwipeToDismissBoxState()
            horizontalScrollState = rememberScrollState(initialScrollState)

            SwipeToDismissBox(state = state, modifier = Modifier.testTag(TEST_TAG)) {
                nestedScrollContent(state, horizontalScrollState)
            }
        }

        rule.onNodeWithTag(TEST_TAG).performTouchInput { swipeRight(200f, 400f) }
        rule.runOnIdle { assertThat(horizontalScrollState.value < initialScrollState).isTrue() }
    }

    @Test
    fun edgeswipe_swipe_edge_content_right_then_left_no_scroll() {
        testBothDirectionScroll(
            initialTouch = 10,
            duration = 2000,
            amplitude = 100,
            startLeft = false,
        ) { scrollState ->
            assertEquals(scrollState.value, 200)
        }
    }

    @Test
    fun edgeswipe_fling_edge_content_right_then_left_no_scroll() {
        testBothDirectionScroll(
            initialTouch = 10,
            duration = 100,
            amplitude = 100,
            startLeft = false,
        ) { scrollState ->
            assertEquals(scrollState.value, 200)
        }
    }

    @Test
    fun edgeswipe_swipe_edge_content_left_then_right_with_scroll() {
        testBothDirectionScroll(
            initialTouch = 10,
            duration = 2000,
            amplitude = 100,
            startLeft = true,
        ) { scrollState ->
            // After scrolling to the left, successful scroll to the right
            // reduced scrollState
            assertThat(scrollState.value < 200).isTrue()
        }
    }

    @Test
    fun edgeswipe_fling_edge_content_left_then_right_with_scroll() {
        testBothDirectionScroll(
            initialTouch = 10,
            duration = 100,
            amplitude = 100,
            startLeft = true,
        ) { scrollState ->
            // Fling right to the start (0)
            assertEquals(scrollState.value, 0)
        }
    }

    private fun testBothDirectionScroll(
        initialTouch: Long,
        duration: Long,
        amplitude: Long,
        startLeft: Boolean,
        testScrollState: (ScrollState) -> Unit,
    ) {
        val initialScrollState = 200
        lateinit var horizontalScrollState: ScrollState
        rule.setContentWithTheme {
            val state = rememberSwipeToDismissBoxState()
            horizontalScrollState = rememberScrollState(initialScrollState)

            SwipeToDismissBox(state = state, modifier = Modifier.testTag(TEST_TAG)) {
                nestedScrollContent(state, horizontalScrollState)
            }
        }

        rule.onNodeWithTag(TEST_TAG).performTouchInput {
            swipeBothDirections(
                startLeft = startLeft,
                startX = initialTouch,
                amplitude = amplitude,
                duration = duration,
            )
        }
        rule.runOnIdle { testScrollState(horizontalScrollState) }
    }

    private fun verifySwipe(gesture: TouchInjectionScope.() -> Unit, expectedToDismiss: Boolean) {
        var dismissed = false
        rule.setContentWithTheme {
            val state = rememberSwipeToDismissBoxState()
            LaunchedEffect(state.currentValue) {
                dismissed = state.currentValue == SwipeToDismissValue.Dismissed
            }
            SwipeToDismissBox(state = state, modifier = Modifier.testTag(TEST_TAG)) {
                messageContent()
            }
        }

        rule.onNodeWithTag(TEST_TAG).performTouchInput(gesture)

        rule.runOnIdle { assertEquals(expectedToDismiss, dismissed) }
    }

    private fun verifyEdgeSwipeWithNestedScroll(
        gesture: TouchInjectionScope.() -> Unit,
        expectedToDismiss: Boolean,
        initialScrollState: Int = 200,
    ) {
        var dismissed = false
        rule.setContentWithTheme {
            val state = rememberSwipeToDismissBoxState()
            val horizontalScrollState = rememberScrollState(initialScrollState)

            LaunchedEffect(state.currentValue) {
                dismissed = state.currentValue == SwipeToDismissValue.Dismissed
            }
            SwipeToDismissBox(state = state, modifier = Modifier.testTag(TEST_TAG)) {
                nestedScrollContent(state, horizontalScrollState)
            }
        }

        rule.onNodeWithTag(TEST_TAG).performTouchInput(gesture)

        rule.runOnIdle { assertEquals(expectedToDismiss, dismissed) }
    }

    private fun verifyPartialSwipe(expectedMessage: String) {
        rule.setContentWithTheme {
            val state = rememberSwipeToDismissBoxState()
            SwipeToDismissBox(state = state, modifier = Modifier.testTag(TEST_TAG)) { isBackground
                ->
                if (isBackground) Text(BACKGROUND_MESSAGE) else messageContent()
            }
        }

        // Click down and drag across 1/4 of the screen to start a swipe,
        // but don't release the finger, so that the screen can be inspected
        // (note that swipeRight would release the finger and does not pause time midway).
        rule
            .onNodeWithTag(TEST_TAG)
            .performTouchInput({
                down(Offset(x = 0f, y = height / 2f))
                moveTo(Offset(x = width / 4f, y = height / 2f))
            })

        rule.onNodeWithText(expectedMessage).assertExists()
    }

    @Composable
    private fun messageContent() {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(CONTENT_MESSAGE, color = MaterialTheme.colors.onPrimary)
        }
    }

    @Composable
    private fun nestedScrollContent(
        swipeToDismissState: SwipeToDismissBoxState,
        horizontalScrollState: ScrollState,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Text(
                modifier =
                    Modifier.align(Alignment.Center)
                        .edgeSwipeToDismiss(swipeToDismissState)
                        .horizontalScroll(horizontalScrollState),
                text =
                    "This text can be scrolled horizontally - to dismiss, swipe " +
                        "right from the left edge of the screen (called Edge Swiping)",
            )
        }
    }

    private fun TouchInjectionScope.swipeBothDirections(
        startLeft: Boolean,
        startX: Long,
        amplitude: Long,
        duration: Long = 200,
    ) {
        val sign = if (startLeft) -1 else 1
        // By using sin function for range 0.. 3pi/2 , we can achieve 0 -> 1 and 1 -> -1  values
        swipe(
            curve = { time ->
                val x =
                    startX +
                        sign *
                            sin(time.toFloat() / duration.toFloat() * 3 * Math.PI / 2).toFloat() *
                            amplitude
                Offset(x = x, y = centerY)
            },
            durationMillis = duration,
        )
    }
}

private const val BACKGROUND_MESSAGE = "The Background"
private const val CONTENT_MESSAGE = "The Content"
private const val TOGGLE_SCREEN = "Toggle"
private const val COUNTER_SCREEN = "Counter"
private const val TOGGLE_ON = "On"
private const val TOGGLE_OFF = "Off"
