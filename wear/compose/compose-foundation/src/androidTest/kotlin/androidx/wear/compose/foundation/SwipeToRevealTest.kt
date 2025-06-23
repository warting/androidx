/*
 * Copyright 2023 The Android Open Source Project
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

@file:OptIn(ExperimentalWearFoundationApi::class)
@file:Suppress("DEPRECATION")

package androidx.wear.compose.foundation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.TouchInjectionScope
import androidx.compose.ui.test.assertLeftPositionInRootIsEqualTo
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.unit.dp
import junit.framework.TestCase.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertThrows
import org.junit.Rule
import org.junit.Test

class SwipeToRevealTest {
    @get:Rule val rule = createComposeRule()

    @Test
    fun supports_testTag() {
        rule.setContent { swipeToRevealWithDefaults(modifier = Modifier.testTag(TEST_TAG)) }

        rule.onNodeWithTag(TEST_TAG).assertExists()
    }

    @Test
    fun onStartWithDefaultState_keepsContentToRight() {
        rule.setContent {
            swipeToRevealWithDefaults(
                content = { getBoxContent(modifier = Modifier.testTag(TEST_TAG)) }
            )
        }

        rule.onNodeWithTag(TEST_TAG).assertLeftPositionInRootIsEqualTo(0.dp)
    }

    @Test
    fun onZeroOffset_doesNotDrawActions() {
        rule.setContent {
            swipeToRevealWithDefaults(
                primaryAction = { actionContent(modifier = Modifier.testTag(TEST_TAG)) }
            )
        }

        rule.onNodeWithTag(TEST_TAG).assertDoesNotExist()
    }

    @Test
    fun onRevealing_drawsAction() {
        rule.setContent {
            swipeToRevealWithDefaults(
                state = rememberRevealState(initialValue = RevealValue.RightRevealing),
                primaryAction = { actionContent(modifier = Modifier.testTag(TEST_TAG)) },
            )
        }

        rule.onNodeWithTag(TEST_TAG).assertExists()
    }

    @Test
    fun onSwipe_drawsAction() {
        val s2rTag = "S2RTag"
        rule.setContent {
            swipeToRevealWithDefaults(
                modifier = Modifier.testTag(s2rTag),
                primaryAction = { actionContent(modifier = Modifier.testTag(TEST_TAG)) },
            )
        }

        rule.onNodeWithTag(s2rTag).performTouchInput {
            down(center)
            // Move the pointer by quarter of the screen width, don't move up the pointer
            moveBy(delta = Offset(x = -(centerX / 4), y = 0f))
        }

        rule.waitForIdle()
        rule.onNodeWithTag(TEST_TAG).assertExists()
    }

    @Test
    fun noSwipe_onFullSwipeRight() {
        var onFullSwipeTriggered = false

        verifyGesture(
            revealValue = RevealValue.Covered,
            onFullSwipe = { onFullSwipeTriggered = true },
            gesture = { swipeRight() },
        )

        assertEquals(false, onFullSwipeTriggered)
    }

    @Test
    fun stateToSwiped_onFullSwipeLeft() {
        var onFullSwipeTriggered = false
        verifyGesture(
            revealValue = RevealValue.RightRevealed,
            onFullSwipe = { onFullSwipeTriggered = true },
            gesture = { swipeLeft() },
        )

        assertEquals(true, onFullSwipeTriggered)
    }

    @Test
    fun stateToSwiped_onFullSwipeRight() {
        var onFullSwipeTriggered = false
        verifyGesture(
            revealValue = RevealValue.LeftRevealed,
            onFullSwipe = { onFullSwipeTriggered = true },
            revealDirection = RevealDirection.Both,
            gesture = { swipeRight() },
        )

        assertEquals(true, onFullSwipeTriggered)
    }

    @Test
    fun stateToRevealing_onAboveVelocityThresholdSmallDistanceSwipe() {
        verifyGesture(
            revealValue = RevealValue.RightRevealing,
            gesture = { swipeLeft(endX = right - 65, durationMillis = 30L) },
        )
    }

    @Test
    fun noSwipe_onBelowVelocityThresholdSmallDistanceSwipe() {
        verifyGesture(
            revealValue = RevealValue.Covered,
            gesture = { swipeLeft(endX = right - 65, durationMillis = 1000L) },
        )
    }

    @Test
    fun stateToRevealing_onAboveVelocityThresholdLongDistanceSwipe() {
        verifyGesture(
            revealValue = RevealValue.RightRevealing,
            gesture = { swipeLeft(endX = right - 300, durationMillis = 100L) },
        )
    }

    @Test
    fun stateToRevealing_onBelowVelocityThresholdLongDistanceSwipe() {
        verifyGesture(
            revealValue = RevealValue.RightRevealing,
            gesture = { swipeLeft(endX = right - 300, durationMillis = 1000L) },
        )
    }

    @Test
    fun noSwipe_singleDirectionSwipeOnTheEdgeDisabled_onFullSwipeRight() {
        var onFullSwipeTriggered = false
        verifyGesture(
            revealValue = RevealValue.Covered,
            onFullSwipe = { onFullSwipeTriggered = true },
            gesture = { swipeRight() },
            bidirectionalGestureInclusion = false,
        )

        assertFalse(onFullSwipeTriggered)
    }

    @Test
    fun noSwipe_bothDirectionsSwipeOnTheEdgeDisabled_onFullSwipeRight() {
        var onFullSwipeTriggered = false
        verifyGesture(
            revealValue = RevealValue.Covered,
            onFullSwipe = { onFullSwipeTriggered = true },
            revealDirection = RevealDirection.Both,
            gesture = { swipeRight() },
            bidirectionalGestureInclusion = false,
        )

        assertFalse(onFullSwipeTriggered)
    }

    @Test
    fun stateToSwiped_bothDirectionsSwipeOnTheEdgeDisabled_onPartialSwipeRight() {
        verifyGesture(
            revealValue = RevealValue.LeftRevealing,
            revealDirection = RevealDirection.Both,
            gesture = { swipeRight(startX = width / 2f, endX = width.toFloat()) },
            bidirectionalGestureInclusion = false,
        )
    }

    @Test
    fun navigationSwipe_singleDirectionSwipeOnTheEdgeDisabled_onFullSwipeRight() {
        var onSwipeToDismissBoxDismissed = false
        verifyGesture(
            initialValue = RevealValue.Covered,
            revealValue = RevealValue.Covered,
            gesture = { swipeRight() },
            bidirectionalGestureInclusion = false,
            wrappedInSwipeToDismissBox = true,
            onSwipeToDismissBoxDismissed = { onSwipeToDismissBoxDismissed = true },
        )

        assertTrue(onSwipeToDismissBoxDismissed)
    }

    @Test
    fun stateToCovered_singleDirectionRevealingSwipeOnTheEdgeDisabled_onFullSwipeRight() {
        var onSwipeToDismissBoxDismissed = false

        verifyGesture(
            initialValue = RevealValue.RightRevealing,
            revealValue = RevealValue.Covered,
            gesture = { swipeRight() },
            bidirectionalGestureInclusion = false,
            wrappedInSwipeToDismissBox = true,
            onSwipeToDismissBoxDismissed = { onSwipeToDismissBoxDismissed = true },
        )

        assertFalse(onSwipeToDismissBoxDismissed)
    }

    @Test
    fun stateToCovered_singleDirectionRevealingSwipeOnTheEdgeDisabled_onPartialSwipeRight() {
        var onSwipeToDismissBoxDismissed = false

        verifyGesture(
            initialValue = RevealValue.RightRevealing,
            revealValue = RevealValue.Covered,
            gesture = { swipeRight(startX = width / 2f, endX = width.toFloat()) },
            bidirectionalGestureInclusion = false,
            wrappedInSwipeToDismissBox = true,
            onSwipeToDismissBoxDismissed = { onSwipeToDismissBoxDismissed = true },
        )

        assertFalse(onSwipeToDismissBoxDismissed)
    }

    @Test
    fun stateToCovered_singleDirectionRevealing_onFullSwipeRight() {
        var onSwipeToDismissBoxDismissed = false

        verifyGesture(
            initialValue = RevealValue.RightRevealing,
            revealValue = RevealValue.Covered,
            gesture = { swipeRight() },
            bidirectionalGestureInclusion = true,
            wrappedInSwipeToDismissBox = true,
            onSwipeToDismissBoxDismissed = { onSwipeToDismissBoxDismissed = true },
        )

        assertFalse(onSwipeToDismissBoxDismissed)
    }

    @Test
    fun stateToIconsVisible_onPartialSwipeLeft() {
        verifyGesture(
            revealValue = RevealValue.RightRevealing,
            gesture = { swipeLeft(startX = width / 2f, endX = 0f) },
        )
    }

    @Test
    fun onSwipe_whenNotAllowed_doesNotSwipe() {
        lateinit var revealState: RevealState
        rule.setContent {
            revealState =
                rememberRevealState(
                    confirmValueChange = { revealValue ->
                        revealValue != RevealValue.RightRevealing
                    }
                )
            swipeToRevealWithDefaults(state = revealState, modifier = Modifier.testTag(TEST_TAG))
        }

        rule.onNodeWithTag(TEST_TAG).performTouchInput { swipeLeft(startX = width / 2f, endX = 0f) }

        rule.runOnIdle { assertEquals(RevealValue.Covered, revealState.currentValue) }
    }

    @Test
    fun onMultiSwipe_whenNotAllowed_doesNotReset() {
        lateinit var revealStateOne: RevealState
        lateinit var revealStateTwo: RevealState
        val testTagOne = "testTagOne"
        val testTagTwo = "testTagTwo"
        rule.setContent {
            revealStateOne = rememberRevealState()
            revealStateTwo =
                rememberRevealState(
                    confirmValueChange = { revealValue ->
                        revealValue != RevealValue.RightRevealing
                    }
                )
            Column {
                swipeToRevealWithDefaults(
                    state = revealStateOne,
                    modifier = Modifier.testTag(testTagOne),
                )
                swipeToRevealWithDefaults(
                    state = revealStateTwo,
                    modifier = Modifier.testTag(testTagTwo),
                )
            }
        }

        // swipe the first S2R
        rule.onNodeWithTag(testTagOne).performTouchInput {
            swipeLeft(startX = width / 2f, endX = 0f)
        }

        // swipe the second S2R to a reveal value which is not allowed
        rule.onNodeWithTag(testTagTwo).performTouchInput {
            swipeLeft(startX = width / 2f, endX = 0f)
        }

        rule.runOnIdle {
            assertEquals(RevealValue.RightRevealing, revealStateOne.currentValue)
            assertEquals(RevealValue.Covered, revealStateTwo.currentValue)
        }
    }

    @Test
    fun onMultiSwipe_whenAllowed_resetsLastState() {
        lateinit var revealStateOne: RevealState
        lateinit var revealStateTwo: RevealState
        val testTagOne = "testTagOne"
        val testTagTwo = "testTagTwo"
        rule.setContent {
            revealStateOne = rememberRevealState()
            revealStateTwo = rememberRevealState()
            Column {
                swipeToRevealWithDefaults(
                    state = revealStateOne,
                    modifier = Modifier.testTag(testTagOne),
                )
                swipeToRevealWithDefaults(
                    state = revealStateTwo,
                    modifier = Modifier.testTag(testTagTwo),
                )
            }
        }

        // swipe the first S2R to Revealing state
        rule.onNodeWithTag(testTagOne).performTouchInput {
            swipeLeft(startX = width / 2f, endX = 0f)
        }

        // swipe the second S2R to a reveal value
        rule.onNodeWithTag(testTagTwo).performTouchInput {
            swipeLeft(startX = width / 2f, endX = 0f)
        }

        rule.runOnIdle {
            assertEquals(RevealValue.Covered, revealStateOne.currentValue)
            assertEquals(RevealValue.RightRevealing, revealStateTwo.currentValue)
        }
    }

    @Test
    fun onMultiSwipe_whenLastStateRevealed_doesNotReset() {
        lateinit var revealStateOne: RevealState
        lateinit var revealStateTwo: RevealState
        val testTagOne = "testTagOne"
        val testTagTwo = "testTagTwo"
        rule.setContent {
            revealStateOne = rememberRevealState()
            revealStateTwo = rememberRevealState()
            Column {
                swipeToRevealWithDefaults(
                    state = revealStateOne,
                    modifier = Modifier.testTag(testTagOne),
                )
                swipeToRevealWithDefaults(
                    state = revealStateTwo,
                    modifier = Modifier.testTag(testTagTwo),
                )
            }
        }

        // swipe the first S2R to Revealed (full screen swipe)
        rule.onNodeWithTag(testTagOne).performTouchInput {
            swipeLeft(startX = width.toFloat(), endX = 0f)
        }

        // swipe the second S2R to a reveal value
        rule.onNodeWithTag(testTagTwo).performTouchInput {
            swipeLeft(startX = width / 2f, endX = 0f)
        }

        rule.runOnIdle {
            // assert that state does not reset
            assertEquals(RevealValue.RightRevealed, revealStateOne.currentValue)
            assertEquals(RevealValue.RightRevealing, revealStateTwo.currentValue)
        }
    }

    @Test
    fun onSnapForDifferentStates_lastOneGetsReset() {
        lateinit var revealStateOne: RevealState
        lateinit var revealStateTwo: RevealState
        rule.setContent {
            revealStateOne = rememberRevealState()
            revealStateTwo = rememberRevealState()
            swipeToRevealWithDefaults(state = revealStateOne)
            swipeToRevealWithDefaults(state = revealStateTwo)

            val coroutineScope = rememberCoroutineScope()
            coroutineScope.launch {
                // First change
                revealStateOne.snapTo(RevealValue.RightRevealing)
                // Second change, in a different state
                revealStateTwo.snapTo(RevealValue.RightRevealing)
            }
        }

        rule.runOnIdle { assertEquals(RevealValue.Covered, revealStateOne.currentValue) }
    }

    @Test
    fun onMultiSnapOnSameState_doesNotReset() {
        lateinit var revealStateOne: RevealState
        lateinit var revealStateTwo: RevealState
        val lastValue = RevealValue.RightRevealed
        rule.setContent {
            revealStateOne = rememberRevealState()
            revealStateTwo = rememberRevealState()
            swipeToRevealWithDefaults(state = revealStateOne)
            swipeToRevealWithDefaults(state = revealStateTwo)

            val coroutineScope = rememberCoroutineScope()
            coroutineScope.launch {
                revealStateOne.snapTo(RevealValue.RightRevealing) // First change
                revealStateOne.snapTo(lastValue) // Second change, same state
            }
        }

        rule.runOnIdle { assertEquals(lastValue, revealStateOne.currentValue) }
    }

    @Test
    fun onSecondaryActionClick_setsLastClickAction() =
        verifyLastClickAction(
            expectedClickType = RevealActionType.SecondaryAction,
            initialRevealValue = RevealValue.RightRevealing,
            secondaryActionModifier = Modifier.testTag(TEST_TAG),
        )

    @Test
    fun onPrimaryActionClick_setsLastClickAction() =
        verifyLastClickAction(
            expectedClickType = RevealActionType.PrimaryAction,
            initialRevealValue = RevealValue.RightRevealing,
            primaryActionModifier = Modifier.testTag(TEST_TAG),
        )

    @Test
    fun onUndoActionClick_setsLastClickAction() =
        verifyLastClickAction(
            expectedClickType = RevealActionType.UndoAction,
            initialRevealValue = RevealValue.RightRevealed,
            undoActionModifier = Modifier.testTag(TEST_TAG),
        )

    @Test
    fun onRightSwipe_dispatchEventsToParent() {
        var onPreScrollDispatch = 0f
        rule.setContent {
            val nestedScrollConnection = remember {
                object : NestedScrollConnection {
                    override fun onPreScroll(
                        available: Offset,
                        source: NestedScrollSource,
                    ): Offset {
                        onPreScrollDispatch = available.x
                        return available
                    }
                }
            }
            Box(modifier = Modifier.nestedScroll(nestedScrollConnection)) {
                swipeToRevealWithDefaults(modifier = Modifier.testTag(TEST_TAG))
            }
        }

        rule.onNodeWithTag(TEST_TAG).performTouchInput { swipeRight() }

        assert(onPreScrollDispatch > 0)
    }

    @Test
    fun onLeftSwipe_dispatchEventsToParent() {
        var onPreScrollDispatch = 0f
        rule.setContent {
            val nestedScrollConnection = remember {
                object : NestedScrollConnection {
                    override fun onPreScroll(
                        available: Offset,
                        source: NestedScrollSource,
                    ): Offset {
                        onPreScrollDispatch = available.x
                        return available
                    }
                }
            }
            Box(modifier = Modifier.nestedScroll(nestedScrollConnection)) {
                swipeToRevealWithDefaults(modifier = Modifier.testTag(TEST_TAG))
            }
        }

        rule.onNodeWithTag(TEST_TAG).performTouchInput { swipeLeft() }

        assert(onPreScrollDispatch < 0) // Swiping left means the dispatch will be negative
    }

    @Test()
    fun onRtl_animateToLeftRevealing() {
        verifyIllegalAnimateTo(RevealValue.LeftRevealing)
    }

    @Test()
    fun onRtl_animateToLeftRevealed() {
        verifyIllegalAnimateTo(RevealValue.LeftRevealed)
    }

    private fun verifyIllegalAnimateTo(targetValue: RevealValue) {
        lateinit var revealState: RevealState
        rule.setContent {
            revealState = rememberRevealState()
            swipeToRevealWithDefaults(state = revealState)
        }

        assertThrows(IllegalStateException::class.java) {
            // If use coroutineScope.launch, below block will run in parallel with test code, and we
            // won't able to catch exception.
            runBlocking { revealState.animateTo(targetValue) }
        }
    }

    private fun verifyLastClickAction(
        expectedClickType: RevealActionType,
        initialRevealValue: RevealValue,
        primaryActionModifier: Modifier = Modifier,
        secondaryActionModifier: Modifier = Modifier,
        undoActionModifier: Modifier = Modifier,
    ) {
        lateinit var revealState: RevealState
        rule.setContent {
            revealState = rememberRevealState(initialRevealValue)
            val coroutineScope = rememberCoroutineScope()
            swipeToRevealWithDefaults(
                state = revealState,
                primaryAction = {
                    actionContent(
                        modifier =
                            primaryActionModifier.clickable {
                                coroutineScope.launch {
                                    revealState.snapTo(RevealValue.Covered)
                                    revealState.lastActionType = RevealActionType.PrimaryAction
                                }
                            }
                    )
                },
                secondaryAction = {
                    actionContent(
                        modifier =
                            secondaryActionModifier.clickable {
                                coroutineScope.launch {
                                    revealState.snapTo(RevealValue.Covered)
                                    revealState.lastActionType = RevealActionType.SecondaryAction
                                }
                            }
                    )
                },
                undoAction = {
                    actionContent(
                        modifier =
                            undoActionModifier.clickable {
                                coroutineScope.launch {
                                    revealState.animateTo(RevealValue.Covered)
                                    revealState.lastActionType = RevealActionType.UndoAction
                                }
                            }
                    )
                },
            )
        }
        rule.onNodeWithTag(TEST_TAG).performClick()
        rule.runOnIdle { assertEquals(expectedClickType, revealState.lastActionType) }
    }

    private fun verifyGesture(
        initialValue: RevealValue = RevealValue.Covered,
        revealValue: RevealValue,
        gesture: TouchInjectionScope.() -> Unit,
        onFullSwipe: () -> Unit = {},
        revealDirection: RevealDirection = RevealDirection.RightToLeft,
        bidirectionalGestureInclusion: Boolean = true,
        wrappedInSwipeToDismissBox: Boolean = false,
        onSwipeToDismissBoxDismissed: () -> Unit = {},
    ) {
        lateinit var revealState: RevealState
        rule.setContent {
            revealState =
                rememberRevealState(
                    initialValue = initialValue,
                    anchors = createRevealAnchors(revealDirection = revealDirection),
                )
            if (!wrappedInSwipeToDismissBox) {
                swipeToRevealWithDefaults(
                    state = revealState,
                    onFullSwipe = onFullSwipe,
                    modifier = Modifier.testTag(TEST_TAG),
                    bidirectionalGestureInclusion = bidirectionalGestureInclusion,
                )
            } else {
                BasicSwipeToDismissBox(
                    onDismissed = onSwipeToDismissBoxDismissed,
                    state = rememberSwipeToDismissBoxState(),
                ) { isBackground ->
                    if (isBackground) {
                        Box(modifier = Modifier.fillMaxSize().background(Color.Red))
                    } else {
                        Box(contentAlignment = Alignment.Center) {
                            swipeToRevealWithDefaults(
                                state = revealState,
                                onFullSwipe = onFullSwipe,
                                modifier = Modifier.testTag(TEST_TAG),
                                bidirectionalGestureInclusion = bidirectionalGestureInclusion,
                            )
                        }
                    }
                }
            }
        }

        rule.onNodeWithTag(TEST_TAG).performTouchInput(gesture)

        rule.runOnIdle { assertEquals(revealValue, revealState.currentValue) }
    }

    @Composable
    private fun swipeToRevealWithDefaults(
        primaryAction: @Composable () -> Unit = { getAction() },
        state: RevealState = rememberRevealState(),
        modifier: Modifier = Modifier,
        secondaryAction: (@Composable () -> Unit)? = null,
        undoAction: (@Composable () -> Unit)? = null,
        onFullSwipe: () -> Unit = {},
        bidirectionalGestureInclusion: Boolean = true,
        content: @Composable () -> Unit = { getBoxContent() },
    ) {
        SwipeToReveal(
            primaryAction = primaryAction,
            state = state,
            modifier = modifier,
            secondaryAction = secondaryAction,
            undoAction = undoAction,
            onFullSwipe = onFullSwipe,
            gestureInclusion =
                if (bidirectionalGestureInclusion) {
                    SwipeToRevealDefaults.bidirectionalGestureInclusion
                } else {
                    SwipeToRevealDefaults.gestureInclusion(state)
                },
            content = content,
        )
    }

    @Composable
    private fun getBoxContent(onClick: () -> Unit = {}, modifier: Modifier = Modifier) {
        Box(modifier = modifier.size(width = 200.dp, height = 50.dp).clickable { onClick() }) {}
    }

    @Composable
    private fun actionContent(modifier: Modifier = Modifier) {
        Box(modifier = modifier.size(50.dp)) {}
    }

    @Composable
    private fun getAction(
        onClick: () -> Unit = {},
        modifier: Modifier = Modifier,
        content: @Composable () -> Unit = { actionContent(modifier) },
    ) {
        Box(modifier = modifier.clickable { onClick() }) { content() }
    }
}
