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

package androidx.wear.compose.material3

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.TouchInjectionScope
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertLeftPositionInRootIsEqualTo
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.BasicSwipeToDismissBox
import androidx.wear.compose.foundation.GestureInclusion
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnState
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.foundation.rememberSwipeToDismissBoxState
import androidx.wear.compose.material3.RevealActionType.Companion.None
import androidx.wear.compose.material3.RevealActionType.Companion.PrimaryAction
import androidx.wear.compose.material3.RevealActionType.Companion.SecondaryAction
import androidx.wear.compose.material3.RevealDirection.Companion.Bidirectional
import androidx.wear.compose.material3.RevealDirection.Companion.RightToLeft
import androidx.wear.compose.material3.RevealState.SingleSwipeCoordinator
import androidx.wear.compose.material3.RevealValue.Companion.Covered
import androidx.wear.compose.material3.RevealValue.Companion.LeftRevealed
import androidx.wear.compose.material3.RevealValue.Companion.LeftRevealing
import androidx.wear.compose.material3.RevealValue.Companion.RightRevealed
import androidx.wear.compose.material3.RevealValue.Companion.RightRevealing
import androidx.wear.compose.material3.SwipeToRevealDefaults.SingleActionAnchorWidth
import androidx.wear.compose.material3.SwipeToRevealDefaults.bidirectionalGestureInclusion
import androidx.wear.compose.material3.SwipeToRevealDefaults.gestureInclusion
import androidx.wear.compose.materialcore.CustomTouchSlopProvider
import com.google.common.truth.Truth.assertThat
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test

class SwipeToRevealTest {
    @get:Rule val rule = createComposeRule()

    @Before
    fun setUp() {
        SingleSwipeCoordinator.lastUpdatedState.set(null)
    }

    @Test
    fun onStateChangeToRevealed_performsHaptics() {
        val results = mutableMapOf<HapticFeedbackType, Int>()
        val haptics = hapticFeedback(collectResultsFromHapticFeedback(results))
        lateinit var revealState: RevealState
        lateinit var coroutineScope: CoroutineScope

        rule.setContent {
            revealState = rememberRevealState(initialValue = RightRevealing)
            coroutineScope = rememberCoroutineScope()
            CompositionLocalProvider(LocalHapticFeedback provides haptics) {
                SwipeToRevealWithDefaults(
                    modifier = Modifier.testTag(TEST_TAG),
                    revealState = revealState,
                )
            }
        }

        rule.runOnIdle { assertThat(results).isEmpty() }

        rule.runOnIdle { coroutineScope.launch { revealState.animateTo(RightRevealed) } }

        rule.runOnIdle {
            assertThat(results).hasSize(1)
            assertThat(results).containsKey(HapticFeedbackType.GestureThresholdActivate)
            assertThat(results[HapticFeedbackType.GestureThresholdActivate]).isEqualTo(1)
        }
    }

    @Test
    fun onStateChangeToLeftRevealed_performsHaptics() {
        val results = mutableMapOf<HapticFeedbackType, Int>()
        val haptics = hapticFeedback(collectResultsFromHapticFeedback(results))
        lateinit var revealState: RevealState
        lateinit var coroutineScope: CoroutineScope

        rule.setContent {
            CompositionLocalProvider(LocalHapticFeedback provides haptics) {
                revealState = rememberRevealState(initialValue = LeftRevealing)
                coroutineScope = rememberCoroutineScope()
                SwipeToRevealWithDefaults(
                    modifier = Modifier.testTag(TEST_TAG),
                    revealState = revealState,
                    revealDirection = Bidirectional,
                )
            }
        }

        rule.runOnIdle { assertThat(results).isEmpty() }

        rule.runOnIdle { coroutineScope.launch { revealState.animateTo(LeftRevealed) } }

        rule.runOnIdle {
            assertThat(results).hasSize(1)
            assertThat(results).containsKey(HapticFeedbackType.GestureThresholdActivate)
            assertThat(results[HapticFeedbackType.GestureThresholdActivate]).isEqualTo(1)
        }
    }

    @Test
    fun onStart_defaultState_keepsContentToRight() {
        rule.setContent {
            SwipeToRevealWithDefaults { DefaultContent(modifier = Modifier.testTag(TEST_TAG)) }
        }

        rule.onNodeWithTag(TEST_TAG).assertLeftPositionInRootIsEqualTo(0.dp)
    }

    @Test
    fun onCovered_doesNotDrawActions() {
        rule.setContent {
            SwipeToRevealWithDefaults(
                primaryAction = {
                    DefaultPrimaryActionButton(modifier = Modifier.testTag(TEST_TAG))
                }
            )
        }

        rule.onNodeWithTag(TEST_TAG).assertDoesNotExist()
    }

    @Test
    fun onRightRevealing_drawsAction() {
        rule.setContent {
            SwipeToRevealWithDefaults(
                primaryAction = {
                    DefaultPrimaryActionButton(modifier = Modifier.testTag(TEST_TAG))
                },
                revealState = rememberRevealState(initialValue = RightRevealing),
            )
        }

        rule.onNodeWithTag(TEST_TAG).assertExists()
    }

    @Test
    fun onRightRevealing_twoActions_drawsAction() {
        rule.setContent {
            SwipeToRevealWithDefaults(
                primaryAction = {
                    DefaultPrimaryActionButton(modifier = Modifier.testTag(PRIMARY_ACTION_TAG))
                },
                secondaryAction = {
                    DefaultSecondaryActionButton(modifier = Modifier.testTag(SECONDARY_ACTION_TAG))
                },
                revealState = rememberRevealState(initialValue = RightRevealing),
            )
        }

        rule.onNodeWithTag(PRIMARY_ACTION_TAG).assertExists()
        rule.onNodeWithTag(SECONDARY_ACTION_TAG).assertExists()
    }

    @Test
    fun onLeftRevealing_drawsAction() {
        rule.setContent {
            SwipeToRevealWithDefaults(
                primaryAction = {
                    DefaultPrimaryActionButton(modifier = Modifier.testTag(TEST_TAG))
                },
                revealDirection = Bidirectional,
                revealState = rememberRevealState(initialValue = LeftRevealing),
            )
        }

        rule.onNodeWithTag(TEST_TAG).assertExists()
    }

    @Test
    fun onLeftRevealing_twoActions_drawsAction() {
        rule.setContent {
            SwipeToRevealWithDefaults(
                primaryAction = {
                    DefaultPrimaryActionButton(modifier = Modifier.testTag(PRIMARY_ACTION_TAG))
                },
                secondaryAction = {
                    DefaultSecondaryActionButton(modifier = Modifier.testTag(SECONDARY_ACTION_TAG))
                },
                revealDirection = Bidirectional,
                revealState = rememberRevealState(initialValue = LeftRevealing),
            )
        }

        rule.onNodeWithTag(PRIMARY_ACTION_TAG).assertExists()
        rule.onNodeWithTag(SECONDARY_ACTION_TAG).assertExists()
    }

    @Test
    fun onSwipe_drawsAction() {
        rule.setContent {
            SwipeToRevealWithDefaults(
                modifier = Modifier.testTag(TEST_TAG),
                primaryAction = {
                    DefaultPrimaryActionButton(modifier = Modifier.testTag(PRIMARY_ACTION_TAG))
                },
                enableTouchSlop = false,
            )
        }

        rule.onNodeWithTag(TEST_TAG).performTouchInput {
            down(center)
            // Move the pointer by quarter of the screen width, don't move up the pointer
            moveBy(delta = Offset(x = -(centerX / 4), y = 0f))
        }

        rule.waitForIdle()
        rule.onNodeWithTag(PRIMARY_ACTION_TAG).assertExists()
    }

    @Test
    fun onSwipe_twoActions_drawsAction() {
        rule.setContent {
            SwipeToRevealWithDefaults(
                modifier = Modifier.testTag(TEST_TAG),
                primaryAction = {
                    DefaultPrimaryActionButton(modifier = Modifier.testTag(PRIMARY_ACTION_TAG))
                },
                secondaryAction = {
                    DefaultSecondaryActionButton(modifier = Modifier.testTag(SECONDARY_ACTION_TAG))
                },
                enableTouchSlop = false,
            )
        }

        rule.onNodeWithTag(TEST_TAG).performTouchInput {
            down(center)
            // Move the pointer by quarter of the screen width, don't move up the pointer
            moveBy(delta = Offset(x = -(centerX / 4), y = 0f))
        }

        rule.waitForIdle()
        rule.onNodeWithTag(PRIMARY_ACTION_TAG).assertExists()
        rule.onNodeWithTag(SECONDARY_ACTION_TAG).assertExists()
    }

    @Test
    fun onSwipeRight_drawsAction() {
        rule.setContent {
            SwipeToRevealWithDefaults(
                modifier = Modifier.testTag(TEST_TAG),
                primaryAction = {
                    DefaultPrimaryActionButton(modifier = Modifier.testTag(PRIMARY_ACTION_TAG))
                },
                revealDirection = Bidirectional,
                enableTouchSlop = false,
            )
        }

        rule.onNodeWithTag(TEST_TAG).performTouchInput {
            down(center)
            // Move the pointer by quarter of the screen width, don't move up the pointer
            moveBy(delta = Offset(x = centerX / 4, y = 0f))
        }

        rule.waitForIdle()
        rule.onNodeWithTag(PRIMARY_ACTION_TAG).assertExists()
    }

    @Test
    fun onSwipeRight_twoActions_drawsAction() {
        rule.setContent {
            SwipeToRevealWithDefaults(
                modifier = Modifier.testTag(TEST_TAG),
                primaryAction = {
                    DefaultPrimaryActionButton(modifier = Modifier.testTag(PRIMARY_ACTION_TAG))
                },
                secondaryAction = {
                    DefaultSecondaryActionButton(modifier = Modifier.testTag(SECONDARY_ACTION_TAG))
                },
                revealDirection = Bidirectional,
                enableTouchSlop = false,
            )
        }

        rule.onNodeWithTag(TEST_TAG).performTouchInput {
            down(center)
            // Move the pointer by quarter of the screen width, don't move up the pointer
            moveBy(delta = Offset(x = centerX / 4, y = 0f))
        }

        rule.waitForIdle()
        rule.onNodeWithTag(PRIMARY_ACTION_TAG).assertExists()
        rule.onNodeWithTag(SECONDARY_ACTION_TAG).assertExists()
    }

    @Test
    fun onFullSwipe_drawsAction() {
        rule.setContent {
            SwipeToRevealWithDefaults(
                modifier = Modifier.testTag(TEST_TAG),
                undoPrimaryAction = {
                    DefaultUndoActionButton(modifier = Modifier.testTag(UNDO_PRIMARY_ACTION_TAG))
                },
                enableTouchSlop = false,
            )
        }

        rule.onNodeWithTag(TEST_TAG).performTouchInput { swipeLeft() }

        rule.waitForIdle()
        rule.onNodeWithTag(UNDO_PRIMARY_ACTION_TAG).assertExists()
    }

    @Test
    fun onFullSwipeRight_drawsAction() {
        rule.setContent {
            SwipeToRevealWithDefaults(
                modifier = Modifier.testTag(TEST_TAG),
                undoPrimaryAction = {
                    DefaultUndoActionButton(modifier = Modifier.testTag(UNDO_PRIMARY_ACTION_TAG))
                },
                revealDirection = Bidirectional,
                enableTouchSlop = false,
            )
        }

        rule.onNodeWithTag(TEST_TAG).performTouchInput { swipeRight() }

        rule.waitForIdle()
        rule.onNodeWithTag(UNDO_PRIMARY_ACTION_TAG).assertExists()
    }

    @Test
    fun onFullSwipeRight_noSwipe() {
        verifyGesture(expectedRevealValue = Covered) { swipeRight() }
    }

    @Test
    fun onFullSwipeRight_bidirectionalGestureInclusion_noSwipe() {
        verifyGesture(expectedRevealValue = Covered, bidirectionalGestureInclusion = true) {
            swipeRight()
        }
    }

    @Test
    fun onFullSwipeLeft_stateToSwiped() {
        verifyGesture(expectedRevealValue = RightRevealed) { swipeLeft() }
    }

    @Test
    fun onFullSwipeLeft_bidirectionalGestureInclusion_stateToSwiped() {
        verifyGesture(expectedRevealValue = RightRevealed, bidirectionalGestureInclusion = true) {
            swipeLeft()
        }
    }

    @Test
    fun onFullSwipeRight_bidirectionalNonBidirectionalGestureInclusion_noSwipe() {
        verifyGesture(
            expectedRevealValue = Covered,
            revealDirection = Bidirectional,
            bidirectionalGestureInclusion = false,
        ) {
            swipeRight()
        }
    }

    @Test
    fun onFullSwipeRight_bidirectional_stateToSwiped() {
        verifyGesture(expectedRevealValue = LeftRevealed, revealDirection = Bidirectional) {
            swipeRight()
        }
    }

    @Test
    fun onPartialSwipeRight_bidirectionalNonBidirectionalGestureInclusion_stateToSwiped() {
        verifyGesture(
            expectedRevealValue = LeftRevealed,
            revealDirection = Bidirectional,
            bidirectionalGestureInclusion = false,
        ) {
            swipeRight(startX = width / 2f)
        }
    }

    @Test
    fun onPartialSwipeRight_bidirectional_stateToSwiped() {
        verifyGesture(expectedRevealValue = LeftRevealed, revealDirection = Bidirectional) {
            swipeRight(startX = width / 2f)
        }
    }

    @Test
    fun onFullSwipeLeft_bidirectionalNonBidirectionalGestureInclusion_stateToSwiped() {
        verifyGesture(
            expectedRevealValue = RightRevealed,
            revealDirection = Bidirectional,
            bidirectionalGestureInclusion = false,
        ) {
            swipeLeft()
        }
    }

    @Test
    fun onFullSwipeLeft_bidirectional_stateToSwiped() {
        verifyGesture(expectedRevealValue = RightRevealed, revealDirection = Bidirectional) {
            swipeLeft()
        }
    }

    @Test
    fun onAboveVelocityThresholdSmallDistanceSwipe_stateToRevealing() {
        verifyGesture(expectedRevealValue = RightRevealing, enableTouchSlop = false) {
            swipeLeft(endX = right - 65, durationMillis = 30L)
        }
    }

    @Test
    fun onBelowVelocityThresholdSmallDistanceSwipe_noSwipe() {
        verifyGesture(expectedRevealValue = Covered, enableTouchSlop = false) {
            swipeLeft(endX = right - 65, durationMillis = 1000L)
        }
    }

    @Test
    fun onAboveVelocityThresholdLongDistanceSwipe_stateToRevealing() {
        verifyGesture(expectedRevealValue = RightRevealing, enableTouchSlop = false) {
            swipeLeft(endX = right - 150, durationMillis = 30L)
        }
    }

    @Test
    fun onBelowVelocityThresholdLongDistanceSwipe_stateToRevealing() {
        verifyGesture(expectedRevealValue = RightRevealing, enableTouchSlop = false) {
            swipeLeft(endX = right - 150, durationMillis = 1000L)
        }
    }

    @Ignore("b/419229763")
    @Test
    fun onPartialSwipe_lastStateRevealing_resetsLastState() {
        verifyStateMultipleSwipeToReveal(
            actions = { revealStateOne, revealStateTwo, density ->
                // swipe the first S2R to Revealing state
                rule.onNodeWithTag(SWIPE_TO_REVEAL_TAG).performTouchInput {
                    swipeLeftToRevealing(density)
                }

                // swipe the second S2R to Revealing state
                rule.onNodeWithTag(SWIPE_TO_REVEAL_SECOND_TAG).performTouchInput {
                    swipeLeftToRevealing(density)
                }
            },
            assertions = { revealStateOne, revealStateTwo ->
                assertEquals(Covered, revealStateOne.currentValue)
                assertEquals(RightRevealing, revealStateTwo.currentValue)
            },
        )
    }

    @Ignore("b/419229763")
    @Test
    fun onPartialSwipe_whenLastStateRevealed_doesNotReset() {
        verifyStateMultipleSwipeToReveal(
            actions = { revealStateOne, revealStateTwo, density ->
                // swipe the first S2R to Revealed (full screen swipe)
                rule.onNodeWithTag(SWIPE_TO_REVEAL_TAG).performTouchInput { swipeLeft() }

                // swipe the second S2R to Revealing state
                rule.onNodeWithTag(SWIPE_TO_REVEAL_SECOND_TAG).performTouchInput {
                    swipeLeftToRevealing(density)
                }
            },
            assertions = { revealStateOne, revealStateTwo ->
                // assert that state does not reset
                assertEquals(RightRevealed, revealStateOne.currentValue)
                assertEquals(RightRevealing, revealStateTwo.currentValue)
            },
        )
    }

    @Ignore("b/419229763")
    @Test
    fun onPartialSwipeRight_lastStateRevealing_resetsLastState() {
        verifyStateMultipleSwipeToReveal(
            actions = { revealStateOne, revealStateTwo, density ->
                // swipe the first S2R to Revealing state
                rule.onNodeWithTag(SWIPE_TO_REVEAL_TAG).performTouchInput {
                    swipeRightToRevealing(density)
                }

                // swipe the second S2R to Revealing state
                rule.onNodeWithTag(SWIPE_TO_REVEAL_SECOND_TAG).performTouchInput {
                    swipeRightToRevealing(density)
                }
            },
            revealDirection = Bidirectional,
            assertions = { revealStateOne, revealStateTwo ->
                assertEquals(Covered, revealStateOne.currentValue)
                assertEquals(LeftRevealing, revealStateTwo.currentValue)
            },
        )
    }

    @Ignore("b/419229763")
    @Test
    fun onPartialSwipeRight_whenLastStateRevealed_doesNotReset() {
        verifyStateMultipleSwipeToReveal(
            actions = { revealStateOne, revealStateTwo, density ->
                // swipe the first S2R to Revealed (full screen swipe)
                rule.onNodeWithTag(SWIPE_TO_REVEAL_TAG).performTouchInput { swipeRight() }

                // swipe the second S2R to Revealing state
                rule.onNodeWithTag(SWIPE_TO_REVEAL_SECOND_TAG).performTouchInput {
                    swipeRightToRevealing(density)
                }
            },
            revealDirection = Bidirectional,
            assertions = { revealStateOne, revealStateTwo ->
                // assert that state does not reset
                assertEquals(LeftRevealed, revealStateOne.currentValue)
                assertEquals(LeftRevealing, revealStateTwo.currentValue)
            },
        )
    }

    @Ignore("b/419229763")
    @Test
    fun onPartialSwipeRightAndLeft_differentComponents_lastOneGetsReset() {
        verifyStateMultipleSwipeToReveal(
            actions = { revealStateOne, revealStateTwo, density ->
                // swipe the first S2R to Revealing state
                rule.onNodeWithTag(SWIPE_TO_REVEAL_TAG).performTouchInput {
                    swipeRightToRevealing(density)
                }

                // swipe the second S2R to Revealing state
                rule.onNodeWithTag(SWIPE_TO_REVEAL_SECOND_TAG).performTouchInput {
                    swipeLeftToRevealing(density)
                }
            },
            revealDirection = Bidirectional,
            assertions = { revealStateOne, revealStateTwo ->
                assertEquals(Covered, revealStateOne.currentValue)
                assertEquals(RightRevealing, revealStateTwo.currentValue)
            },
        )
    }

    @Ignore("b/419229763")
    @Test
    fun onPartialSwipeLeftAndRight_differentComponents_lastOneGetsReset() {
        verifyStateMultipleSwipeToReveal(
            actions = { revealStateOne, revealStateTwo, density ->
                // swipe the first S2R to Revealing state
                rule.onNodeWithTag(SWIPE_TO_REVEAL_TAG).performTouchInput {
                    swipeLeftToRevealing(density)
                }

                // swipe the second S2R to Revealing state
                rule.onNodeWithTag(SWIPE_TO_REVEAL_SECOND_TAG).performTouchInput {
                    swipeRightToRevealing(density)
                }
            },
            revealDirection = Bidirectional,
            assertions = { revealStateOne, revealStateTwo ->
                assertEquals(Covered, revealStateOne.currentValue)
                assertEquals(LeftRevealing, revealStateTwo.currentValue)
            },
        )
    }

    @Ignore("b/419229763")
    @Test
    fun onMultiSnap_differentComponents_lastOneGetsReset() {
        verifyStateMultipleSwipeToReveal(
            actionsSuspended = { revealStateOne, revealStateTwo, density ->
                // First change
                revealStateOne.snapTo(RightRevealing)
                // Second change, in a different component
                revealStateTwo.snapTo(RightRevealing)
            },
            assertions = { revealStateOne, revealStateTwo ->
                assertEquals(Covered, revealStateOne.currentValue)
                assertEquals(RightRevealing, revealStateTwo.currentValue)
            },
        )
    }

    @Test
    fun onMultiSnap_sameComponents_doesNotReset() {
        val lastValue = RightRevealed
        verifyStateMultipleSwipeToReveal(
            actionsSuspended = { revealStateOne, revealStateTwo, density ->
                revealStateOne.snapTo(RightRevealing) // First change
                revealStateOne.snapTo(lastValue) // Second change, same component
            },
            assertions = { revealStateOne, revealStateTwo ->
                assertEquals(lastValue, revealStateOne.currentValue)
            },
        )
    }

    @Ignore("b/419229763")
    @Test
    fun onMultiSnapRight_differentComponents_lastOneGetsReset() {
        verifyStateMultipleSwipeToReveal(
            actionsSuspended = { revealStateOne, revealStateTwo, density ->
                // First change
                revealStateOne.snapTo(LeftRevealing)
                // Second change, in a different component
                revealStateTwo.snapTo(LeftRevealing)
            },
            revealDirection = Bidirectional,
            assertions = { revealStateOne, revealStateTwo ->
                assertEquals(Covered, revealStateOne.currentValue)
                assertEquals(LeftRevealing, revealStateTwo.currentValue)
            },
        )
    }

    @Ignore("b/419229763")
    @Test
    fun onMultiSnapRight_sameComponents_doesNotReset() {
        val lastValue = LeftRevealed
        verifyStateMultipleSwipeToReveal(
            actionsSuspended = { revealStateOne, revealStateTwo, density ->
                revealStateOne.snapTo(LeftRevealing) // First change
                revealStateOne.snapTo(lastValue) // Second change, same component
            },
            revealDirection = Bidirectional,
            assertions = { revealStateOne, revealStateTwo ->
                assertEquals(lastValue, revealStateOne.currentValue)
            },
        )
    }

    @Ignore("b/419229763")
    @Test
    fun onMultiSnapRightAndLeft_differentComponents_lastOneGetsReset() {
        verifyStateMultipleSwipeToReveal(
            actionsSuspended = { revealStateOne, revealStateTwo, density ->
                // First change
                revealStateOne.snapTo(RightRevealing)
                // Second change, in a different component
                revealStateTwo.snapTo(LeftRevealing)
            },
            revealDirection = Bidirectional,
            assertions = { revealStateOne, revealStateTwo ->
                assertEquals(Covered, revealStateOne.currentValue)
                assertEquals(LeftRevealing, revealStateTwo.currentValue)
            },
        )
    }

    @Ignore("b/419229763")
    @Test
    fun onMultiSnapLeftAndRight_differentComponents_lastOneGetsReset() {
        verifyStateMultipleSwipeToReveal(
            actionsSuspended = { revealStateOne, revealStateTwo, density ->
                // First change
                revealStateOne.snapTo(LeftRevealing)
                // Second change, in a different component
                revealStateTwo.snapTo(RightRevealing)
            },
            revealDirection = Bidirectional,
            assertions = { revealStateOne, revealStateTwo ->
                assertEquals(Covered, revealStateOne.currentValue)
                assertEquals(RightRevealing, revealStateTwo.currentValue)
            },
        )
    }

    @Test
    fun onSecondaryActionClick_setsLastClickAction() =
        verifyLastClickAction(
            expectedClickType = SecondaryAction,
            initialRevealValue = RightRevealing,
            nodeTagToPerformClick = SECONDARY_ACTION_TAG,
        )

    @Test
    fun onPrimaryActionClick_setsLastClickAction() =
        verifyLastClickAction(
            expectedClickType = PrimaryAction,
            initialRevealValue = RightRevealing,
        )

    @Test
    fun onUndoActionClick_setsLastClickAction() =
        verifyLastClickAction(
            expectedClickType = None,
            initialRevealValue = RightRevealed,
            nodeTagToPerformClick = UNDO_PRIMARY_ACTION_TAG,
        )

    @Test
    fun onUndoActionClick_setsCorrectCurrentValue() {
        lateinit var revealState: RevealState
        rule.setContent {
            revealState = rememberRevealState(RightRevealed)
            SwipeToRevealWithDefaults(
                revealState = revealState,
                undoPrimaryAction = {
                    DefaultUndoActionButton(modifier = Modifier.testTag(UNDO_PRIMARY_ACTION_TAG))
                },
            )
        }
        rule.onNodeWithTag(UNDO_PRIMARY_ACTION_TAG).performClick()
        rule.runOnIdle { assertEquals(Covered, revealState.currentValue) }
    }

    @Test
    fun onFullSwipeRight_wrappedInSwipeToDismissBox_navigationSwipe() {
        verifyGesture(
            expectedRevealValue = Covered,
            wrappedInSwipeToDismissBox = true,
            expectedSwipeToDismissBoxDismissed = true,
        ) {
            swipeRight()
        }
    }

    @Test
    fun onFullSwipeRight_wrappedInSTDBBidirectionalGI_noSwipe() {
        verifyGesture(
            expectedRevealValue = Covered,
            bidirectionalGestureInclusion = true,
            wrappedInSwipeToDismissBox = true,
        ) {
            swipeRight()
        }
    }

    @Test
    fun onFullSwipeRight_wrappedInSwipeToDismissBoxAndRightRevealing_stateToCovered() {
        verifyGesture(
            initialRevealValue = RightRevealing,
            expectedRevealValue = Covered,
            wrappedInSwipeToDismissBox = true,
        ) {
            swipeRight()
        }
    }

    @Test
    fun onSwipeRightFromOutsideEdge_wrappedInSwipeToDismissBoxAndRightRevealing_stateToCovered() {
        verifyGesture(
            initialRevealValue = RightRevealing,
            expectedRevealValue = Covered,
            wrappedInSwipeToDismissBox = true,
            enableTouchSlop = false,
        ) {
            swipeRight(startX = width / 2f)
        }
    }

    @Test
    fun onRecreation_withInitialState_stateIsRestored() {
        // Given a SwipeToReveal with an initial state.
        val restorationTester = StateRestorationTester(rule)

        lateinit var state: RevealState

        restorationTester.setContent {
            state = rememberRevealState(initialValue = Covered)

            SwipeToRevealWithDefaults(revealState = state)
        }

        val stateBeforeSavedInstanceStateRestore = state

        // When the state is restored.
        restorationTester.emulateSavedInstanceStateRestore()

        // Then the state is restored correctly.
        assertRevealStateIsRestored(stateBeforeSavedInstanceStateRestore, state)
    }

    @Ignore("b/419229763")
    @Test
    fun onRecreation_afterSnapTo_stateIsRestored() {
        // Given a SwipeToReveal in Covered state.
        val restorationTester = StateRestorationTester(rule)

        lateinit var state: RevealState
        lateinit var scope: CoroutineScope

        restorationTester.setContent {
            state = rememberRevealState(initialValue = Covered)

            SwipeToRevealWithDefaults(revealState = state)

            scope = rememberCoroutineScope()
        }

        // And the component is snapped to the RightRevealing state.
        scope.launch { state.snapTo(RightRevealing) }
        rule.waitForIdle()

        val stateBeforeSavedInstanceStateRestore = state

        // When the state is restored
        restorationTester.emulateSavedInstanceStateRestore()

        // Then the state is restored correctly
        assertRevealStateIsRestored(stateBeforeSavedInstanceStateRestore, state)
    }

    @Test
    fun onRecreationInLazyList_afterScroll_showsAction() {
        // Given a SwipeToReveal in Covered state, in a lazy list.
        lateinit var stateOne: RevealState
        lateinit var tlcState: TransformingLazyColumnState
        lateinit var scope: CoroutineScope
        val tlcTestTag = "TLC"
        val tlcTotalItems = 100

        rule.setContent {
            stateOne = rememberRevealState(initialValue = Covered)
            tlcState = rememberTransformingLazyColumnState()

            TransformingLazyColumn(modifier = Modifier.testTag(tlcTestTag), state = tlcState) {
                item {
                    SwipeToRevealWithDefaults(
                        modifier = Modifier.testTag(SWIPE_TO_REVEAL_TAG),
                        primaryAction =
                            @Composable {
                                DefaultPrimaryActionButton(
                                    modifier = Modifier.testTag(PRIMARY_ACTION_TAG)
                                )
                            },
                        revealState = stateOne,
                    )
                }
                items(tlcTotalItems - 1) { SwipeToRevealWithDefaults() }
            }

            scope = rememberCoroutineScope()
        }

        // When the component is snapped to the RightRevealing state to show the primary action.
        scope.launch { stateOne.snapTo(RightRevealing) }
        rule.waitForIdle()

        // And the list is scrolled to the bottom so that the component is not visible on the
        // screen.
        rule.runOnIdle { runBlocking { tlcState.scrollToItem(tlcTotalItems - 1) } }
        rule.onNodeWithTag(SWIPE_TO_REVEAL_TAG).assertDoesNotExist()

        // And the list is scrolled to the top so that the component is visible on the screen again.
        rule.runOnIdle { runBlocking { tlcState.scrollToItem(0) } }
        rule.onNodeWithTag(SWIPE_TO_REVEAL_TAG).assertIsDisplayed()

        // Then the SwipeToReveal should still be displaying the primary action.
        rule.onNodeWithTag(PRIMARY_ACTION_TAG).assertIsDisplayed()
    }

    @Test
    fun onRecreationInLazyList_afterScrollAndDifferentComponentSnapped_stateIsReset() {
        // Given a SwipeToReveal in Covered state, in a lazy list.
        lateinit var stateOne: RevealState
        lateinit var stateTwo: RevealState
        lateinit var tlcState: TransformingLazyColumnState
        lateinit var scope: CoroutineScope
        val tlcTestTag = "TLC"
        val tlcTotalItems = 100

        rule.setContent {
            stateOne = rememberRevealState(initialValue = Covered)
            stateTwo = rememberRevealState(initialValue = Covered)
            tlcState = rememberTransformingLazyColumnState()

            TransformingLazyColumn(modifier = Modifier.testTag(tlcTestTag), state = tlcState) {
                item {
                    SwipeToRevealWithDefaults(
                        modifier = Modifier.testTag(SWIPE_TO_REVEAL_TAG),
                        primaryAction =
                            @Composable {
                                DefaultPrimaryActionButton(
                                    modifier = Modifier.testTag(PRIMARY_ACTION_TAG)
                                )
                            },
                        revealState = stateOne,
                    )
                }
                item {
                    SwipeToRevealWithDefaults(
                        modifier = Modifier.testTag(SWIPE_TO_REVEAL_SECOND_TAG),
                        revealState = stateTwo,
                    )
                }
                items(tlcTotalItems - 2) { SwipeToRevealWithDefaults() }
            }

            scope = rememberCoroutineScope()
        }

        // When the component is snapped to the RightRevealing state to show the primary action.
        scope.launch { stateOne.snapTo(RightRevealing) }
        rule.waitForIdle()

        // And the list is scrolled to the bottom so that the component is not visible on the
        // screen.
        rule.runOnIdle { runBlocking { tlcState.scrollToItem(tlcTotalItems - 1) } }
        rule.onNodeWithTag(SWIPE_TO_REVEAL_TAG).assertDoesNotExist()

        // And the list is scrolled to the top so that the component is visible on the screen again.
        rule.runOnIdle { runBlocking { tlcState.scrollToItem(0) } }
        rule.onNodeWithTag(SWIPE_TO_REVEAL_TAG).assertIsDisplayed()
        rule.onNodeWithTag(PRIMARY_ACTION_TAG).assertIsDisplayed()

        // And a different component is snapped to the RightRevealing state to show the primary
        // action.
        scope.launch { stateTwo.snapTo(RightRevealing) }
        rule.waitForIdle()

        // Then the first component should not display the action anymore.
        rule.onNodeWithTag(PRIMARY_ACTION_TAG).assertDoesNotExist()
    }

    @Test
    fun onPrimaryActionClick_doesNotTriggerOnSwipePrimaryAction() {
        var onPrimaryActionClick = false
        var onSwipePrimaryAction = false
        lateinit var revealState: RevealState
        var density = 0f
        rule.setContent {
            with(LocalDensity.current) { density = this.density }
            revealState = rememberRevealState(Covered)
            SwipeToRevealWithDefaults(
                modifier = Modifier.testTag(TEST_TAG),
                onSwipePrimaryAction = { onSwipePrimaryAction = true },
                primaryAction = {
                    DefaultPrimaryActionButton(
                        modifier = Modifier.testTag(PRIMARY_ACTION_TAG),
                        onClick = { onPrimaryActionClick = true },
                    )
                },
                revealState = revealState,
            )
        }
        rule.onNodeWithTag(TEST_TAG).performTouchInput { swipeLeftToRevealing(density) }
        rule.onNodeWithTag(PRIMARY_ACTION_TAG).performClick()

        rule.runOnIdle {
            assertTrue(onPrimaryActionClick)
            assertFalse(onSwipePrimaryAction)
        }
    }

    @Test
    fun onFullSwipe_doesNotTriggerPrimaryActionClick() {
        var onPrimaryActionClick = false
        var onSwipePrimaryAction = false
        lateinit var revealState: RevealState
        rule.setContent {
            revealState = rememberRevealState(Covered)
            SwipeToRevealWithDefaults(
                modifier = Modifier.testTag(TEST_TAG),
                onSwipePrimaryAction = { onSwipePrimaryAction = true },
                primaryAction = {
                    DefaultPrimaryActionButton(onClick = { onPrimaryActionClick = true })
                },
                revealState = revealState,
            )
        }
        rule.onNodeWithTag(TEST_TAG).performTouchInput { swipeLeft() }

        rule.runOnIdle {
            assertTrue(onSwipePrimaryAction)
            assertFalse(onPrimaryActionClick)
        }
    }

    private fun assertRevealStateIsRestored(previousState: RevealState, currentState: RevealState) {
        rule.runOnIdle {
            assertThat(previousState).isNotSameInstanceAs(currentState)
            assertThat(previousState.currentValue).isEqualTo(currentState.currentValue)
            assertThat(previousState.lastActionType).isEqualTo(currentState.lastActionType)
            assertThat(previousState.offset).isEqualTo(currentState.offset)
            assertThat(previousState.revealThreshold).isEqualTo(currentState.revealThreshold)
            assertThat(previousState.width).isEqualTo(currentState.width)
        }
    }

    private fun verifyLastClickAction(
        expectedClickType: RevealActionType,
        initialRevealValue: RevealValue,
        nodeTagToPerformClick: String = PRIMARY_ACTION_TAG,
    ) {
        lateinit var revealState: RevealState
        rule.setContent {
            revealState = rememberRevealState(initialRevealValue)
            SwipeToRevealWithDefaults(
                primaryAction = {
                    DefaultPrimaryActionButton(
                        modifier = Modifier.testTag(PRIMARY_ACTION_TAG),
                        onClick = {},
                    )
                },
                revealState = revealState,
                secondaryAction = {
                    DefaultSecondaryActionButton(
                        modifier = Modifier.testTag(SECONDARY_ACTION_TAG),
                        onClick = {},
                    )
                },
                undoPrimaryAction = {
                    DefaultUndoActionButton(
                        modifier = Modifier.testTag(UNDO_PRIMARY_ACTION_TAG),
                        onClick = {},
                    )
                },
                undoSecondaryAction = {
                    DefaultUndoActionButton(
                        modifier = Modifier.testTag(UNDO_SECONDARY_ACTION_TAG),
                        onClick = {},
                    )
                },
            )
        }
        rule.onNodeWithTag(nodeTagToPerformClick).performClick()
        rule.runOnIdle { assertEquals(expectedClickType, revealState.lastActionType) }
    }

    private fun verifyGesture(
        initialRevealValue: RevealValue = Covered,
        expectedRevealValue: RevealValue,
        expectedFullSwipeTriggered: Boolean =
            (expectedRevealValue == RightRevealed || expectedRevealValue == LeftRevealed),
        revealDirection: RevealDirection = RightToLeft,
        bidirectionalGestureInclusion: Boolean = revealDirection == Bidirectional,
        enableTouchSlop: Boolean = true,
        wrappedInSwipeToDismissBox: Boolean = false,
        expectedSwipeToDismissBoxDismissed: Boolean = false,
        gesture: TouchInjectionScope.() -> Unit,
    ) {
        var onFullSwipeTriggerCounter = 0
        var onSwipeToDismissBoxDismissed = false
        lateinit var revealState: RevealState

        rule.setContent {
            revealState = rememberRevealState(initialValue = initialRevealValue)

            val content =
                @Composable {
                    SwipeToRevealWithDefaults(
                        modifier = Modifier.testTag(TEST_TAG),
                        onSwipePrimaryAction = { onFullSwipeTriggerCounter++ },
                        revealState = revealState,
                        revealDirection = revealDirection,
                        gestureInclusion =
                            if (bidirectionalGestureInclusion) {
                                SwipeToRevealDefaults.bidirectionalGestureInclusion
                            } else {
                                gestureInclusion(revealState)
                            },
                        enableTouchSlop = enableTouchSlop,
                    )
                }

            if (!wrappedInSwipeToDismissBox) {
                content()
            } else {
                BasicSwipeToDismissBox(
                    onDismissed = { onSwipeToDismissBoxDismissed = true },
                    state = rememberSwipeToDismissBoxState(),
                ) { isBackground ->
                    if (isBackground) {
                        Box(modifier = Modifier.fillMaxSize().background(Color.Red))
                    } else {
                        Box(contentAlignment = Alignment.Center) { content() }
                    }
                }
            }
        }

        rule.onNodeWithTag(TEST_TAG).performTouchInput(block = gesture)

        rule.runOnIdle { assertEquals(expectedRevealValue, revealState.currentValue) }

        assertEquals(if (expectedFullSwipeTriggered) 1 else 0, onFullSwipeTriggerCounter)
        assertEquals(expectedSwipeToDismissBoxDismissed, onSwipeToDismissBoxDismissed)
    }

    private fun verifyStateMultipleSwipeToReveal(
        actions:
            ((revealStateOne: RevealState, revealStateTwo: RevealState, density: Float) -> Unit)? =
            null,
        actionsSuspended:
            (suspend (
                revealStateOne: RevealState, revealStateTwo: RevealState, density: Float,
            ) -> Unit)? =
            null,
        revealDirection: RevealDirection = RightToLeft,
        assertions: (revealStateOne: RevealState, revealStateTwo: RevealState) -> Unit,
    ) {
        lateinit var revealStateOne: RevealState
        lateinit var revealStateTwo: RevealState
        var density = 0f
        rule.setContent {
            with(LocalDensity.current) { density = this.density }
            revealStateOne = rememberRevealState()
            revealStateTwo = rememberRevealState()
            CustomTouchSlopProvider(newTouchSlop = 0f) {
                Column {
                    SwipeToRevealWithDefaults(
                        modifier = Modifier.testTag(SWIPE_TO_REVEAL_TAG),
                        revealState = revealStateOne,
                        revealDirection = revealDirection,
                    )
                    SwipeToRevealWithDefaults(
                        modifier = Modifier.testTag(SWIPE_TO_REVEAL_SECOND_TAG),
                        revealState = revealStateTwo,
                        revealDirection = revealDirection,
                    )
                }
            }

            val coroutineScope = rememberCoroutineScope()
            coroutineScope.launch {
                actionsSuspended?.invoke(revealStateOne, revealStateTwo, density)
            }
        }

        actions?.invoke(revealStateOne, revealStateTwo, density)

        rule.runOnIdle { assertions(revealStateOne, revealStateTwo) }
    }

    @Composable
    fun SwipeToRevealWithDefaults(
        modifier: Modifier = Modifier,
        onSwipePrimaryAction: () -> Unit = {},
        primaryAction: @Composable SwipeToRevealScope.() -> Unit =
            @Composable { DefaultPrimaryActionButton(onClick = onSwipePrimaryAction) },
        secondaryAction: (@Composable SwipeToRevealScope.() -> Unit)? = null,
        undoPrimaryAction: (@Composable SwipeToRevealScope.() -> Unit)? = null,
        undoSecondaryAction: (@Composable SwipeToRevealScope.() -> Unit)? = null,
        revealState: RevealState = rememberRevealState(),
        revealDirection: RevealDirection = RightToLeft,
        hasPartiallyRevealedState: Boolean = true,
        gestureInclusion: GestureInclusion =
            if (revealDirection == Bidirectional) {
                bidirectionalGestureInclusion
            } else {
                gestureInclusion(revealState)
            },
        enableTouchSlop: Boolean = true,
        content: @Composable () -> Unit = @Composable { DefaultContent() },
    ) {
        val swipeToRevealContent =
            @Composable {
                SwipeToReveal(
                    primaryAction = primaryAction,
                    onSwipePrimaryAction = onSwipePrimaryAction,
                    modifier = modifier,
                    secondaryAction = secondaryAction,
                    undoPrimaryAction = undoPrimaryAction,
                    undoSecondaryAction = undoSecondaryAction,
                    revealState = revealState,
                    revealDirection = revealDirection,
                    hasPartiallyRevealedState = hasPartiallyRevealedState,
                    gestureInclusion = gestureInclusion,
                    content = content,
                )
            }
        if (enableTouchSlop) {
            swipeToRevealContent()
        } else {
            CustomTouchSlopProvider(newTouchSlop = 0f, content = swipeToRevealContent)
        }
    }

    @Composable
    private fun SwipeToRevealScope.DefaultPrimaryActionButton(
        modifier: Modifier = Modifier,
        onClick: () -> Unit = {},
    ) {
        PrimaryActionButton(
            onClick = onClick,
            icon = { Icon(Icons.Outlined.Delete, contentDescription = "Delete") },
            text = { Text("Delete") },
            modifier = modifier,
        )
    }

    @Composable
    private fun SwipeToRevealScope.DefaultSecondaryActionButton(
        modifier: Modifier = Modifier,
        onClick: () -> Unit = {},
    ) {
        SecondaryActionButton(
            onClick = onClick,
            icon = { Icon(Icons.Outlined.MoreVert, contentDescription = "More") },
            modifier = modifier,
        )
    }

    @Composable
    private fun SwipeToRevealScope.DefaultUndoActionButton(
        modifier: Modifier = Modifier,
        onClick: () -> Unit = {},
    ) {
        UndoActionButton(onClick = onClick, text = { Text("Undo Delete") }, modifier = modifier)
    }

    @Composable
    private fun DefaultContent(modifier: Modifier = Modifier) {
        Button({}, modifier.fillMaxWidth()) { Text("Swipe me!") }
    }

    private fun TouchInjectionScope.swipeLeftToRevealing(density: Float) {
        val singleActionAnchorWidthPx = SingleActionAnchorWidth.value * density
        swipeLeft(startX = right, endX = right - (singleActionAnchorWidthPx * 0.75f))
    }

    private fun TouchInjectionScope.swipeRightToRevealing(density: Float) {
        val singleActionAnchorWidthPx = SingleActionAnchorWidth.value * density
        swipeRight(startX = left, endX = left + (singleActionAnchorWidthPx * 0.75f))
    }

    companion object {
        private const val SWIPE_TO_REVEAL_TAG = TEST_TAG
        private const val SWIPE_TO_REVEAL_SECOND_TAG = "SWIPE_TO_REVEAL_SECOND_TAG"
        private const val PRIMARY_ACTION_TAG = "PRIMARY_ACTION_TAG"
        private const val SECONDARY_ACTION_TAG = "SECONDARY_ACTION_TAG"
        private const val UNDO_PRIMARY_ACTION_TAG = "UNDO_PRIMARY_ACTION_TAG"
        private const val UNDO_SECONDARY_ACTION_TAG = "UNDO_SECONDARY_ACTION_TAG"
    }
}
