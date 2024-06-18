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

package androidx.compose.material3.adaptive.layout

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.DefaultPaneMotion.Companion.AnimateBounds
import androidx.compose.material3.adaptive.layout.DefaultPaneMotion.Companion.EnterFromLeft
import androidx.compose.material3.adaptive.layout.DefaultPaneMotion.Companion.EnterFromLeftDelayed
import androidx.compose.material3.adaptive.layout.DefaultPaneMotion.Companion.EnterFromRight
import androidx.compose.material3.adaptive.layout.DefaultPaneMotion.Companion.EnterFromRightDelayed
import androidx.compose.material3.adaptive.layout.DefaultPaneMotion.Companion.EnterWithExpand
import androidx.compose.material3.adaptive.layout.DefaultPaneMotion.Companion.ExitToLeft
import androidx.compose.material3.adaptive.layout.DefaultPaneMotion.Companion.ExitToRight
import androidx.compose.material3.adaptive.layout.DefaultPaneMotion.Companion.ExitWithShrink
import androidx.compose.material3.adaptive.layout.DefaultPaneMotion.Companion.NoMotion
import androidx.compose.material3.adaptive.layout.PaneAdaptedValue.Companion.Expanded
import androidx.compose.material3.adaptive.layout.PaneAdaptedValue.Companion.Hidden
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@RunWith(JUnit4::class)
class PaneMotionTest {
    @Test
    fun test_allThreePaneMotions() {
        for (from in ExpectedThreePaneMotions.indices) {
            for (to in ExpectedThreePaneMotions.indices) {
                val fromValue = from.toThreePaneScaffoldValue()
                val toValue = to.toThreePaneScaffoldValue()
                assertWithMessage("From $fromValue to $toValue: ")
                    .that(calculatePaneMotion(fromValue, toValue, MockThreePaneOrder))
                    .isEqualTo(ExpectedThreePaneMotions[from][to])
            }
        }
    }

    @Test
    fun test_allDefaultPaneMotionTransitions() {
        NoMotion.assertTransitions(EnterTransition.None, ExitTransition.None)
        EnterFromLeft.assertTransitions(mockEnterFromLeftTransition, ExitTransition.None)
        EnterFromRight.assertTransitions(mockEnterFromRightTransition, ExitTransition.None)
        EnterFromLeftDelayed.assertTransitions(
            mockEnterFromLeftDelayedTransition,
            ExitTransition.None
        )
        EnterFromRightDelayed.assertTransitions(
            mockEnterFromRightDelayedTransition,
            ExitTransition.None
        )
        ExitToLeft.assertTransitions(EnterTransition.None, mockExitToLeftTransition)
        ExitToRight.assertTransitions(EnterTransition.None, mockExitToRightTransition)
        EnterWithExpand.assertTransitions(mockEnterWithExpandTransition, ExitTransition.None)
        ExitWithShrink.assertTransitions(EnterTransition.None, mockExitWithShrinkTransition)
    }

    private fun DefaultPaneMotion.assertTransitions(
        expectedEnterTransition: EnterTransition,
        expectedExitTransition: ExitTransition
    ) {
        // Can't compare equality directly because of lambda. Check string representation instead
        assertWithMessage("Enter transition of $this: ")
            .that(mockPaneScaffoldMotionScope.enterTransition.toString())
            .isEqualTo(expectedEnterTransition.toString())
        assertWithMessage("Exit transition of $this: ")
            .that(mockPaneScaffoldMotionScope.exitTransition.toString())
            .isEqualTo(expectedExitTransition.toString())
    }

    @Test
    fun slideInFromLeftOffset_noEnterFromLeftPane_equalsZero() {
        mockPaneScaffoldMotionScope.paneMotions =
            listOf(EnterFromRight, EnterFromRight, EnterFromRight)
        assertThat(mockPaneScaffoldMotionScope.slideInFromLeftOffset).isEqualTo(0)
    }

    @Test
    fun slideInFromLeftOffset_withEnterFromLeftPane_useTheRightestEdge() {
        mockPaneScaffoldMotionScope.paneMotions =
            listOf(EnterFromLeft, EnterFromLeft, EnterFromRight)
        assertThat(mockPaneScaffoldMotionScope.slideInFromLeftOffset)
            .isEqualTo(
                -mockPaneScaffoldMotionScope.targetPanePositions[1].x -
                    mockPaneScaffoldMotionScope.targetPaneSizes[1].width
            )
    }

    @Test
    fun slideInFromLeftOffset_withEnterFromLeftDelayedPane_useTheRightestEdge() {
        mockPaneScaffoldMotionScope.paneMotions =
            listOf(EnterFromLeft, EnterFromLeftDelayed, EnterFromRight)
        assertThat(mockPaneScaffoldMotionScope.slideInFromLeftOffset)
            .isEqualTo(
                -mockPaneScaffoldMotionScope.targetPanePositions[1].x -
                    mockPaneScaffoldMotionScope.targetPaneSizes[1].width
            )
    }

    @Test
    fun slideInFromRightOffset_noEnterFromRightPane_equalsZero() {
        mockPaneScaffoldMotionScope.paneMotions =
            listOf(EnterFromLeft, EnterFromLeft, EnterFromLeft)
        assertThat(mockPaneScaffoldMotionScope.slideInFromRightOffset).isEqualTo(0)
    }

    @Test
    fun slideInFromRightOffset_withEnterFromRightPane_useTheLeftestEdge() {
        mockPaneScaffoldMotionScope.paneMotions =
            listOf(EnterFromLeft, EnterFromRight, EnterFromRight)
        assertThat(mockPaneScaffoldMotionScope.slideInFromRightOffset)
            .isEqualTo(
                mockPaneScaffoldMotionScope.scaffoldSize.width -
                    mockPaneScaffoldMotionScope.targetPanePositions[1].x
            )
    }

    @Test
    fun slideInFromRightOffset_withEnterFromRightDelayedPane_useTheLeftestEdge() {
        mockPaneScaffoldMotionScope.paneMotions =
            listOf(EnterFromLeft, EnterFromRightDelayed, EnterFromRight)
        assertThat(mockPaneScaffoldMotionScope.slideInFromRightOffset)
            .isEqualTo(
                mockPaneScaffoldMotionScope.scaffoldSize.width -
                    mockPaneScaffoldMotionScope.targetPanePositions[1].x
            )
    }

    @Test
    fun slideOutToLeftOffset_noExitToLeftPane_equalsZero() {
        mockPaneScaffoldMotionScope.paneMotions =
            listOf(EnterFromRight, EnterFromRight, EnterFromRight)
        assertThat(mockPaneScaffoldMotionScope.slideOutToLeftOffset).isEqualTo(0)
    }

    @Test
    fun slideOutToLeftOffset_withExitToLeftPane_useTheRightestEdge() {
        mockPaneScaffoldMotionScope.paneMotions = listOf(ExitToLeft, ExitToLeft, ExitToRight)
        assertThat(mockPaneScaffoldMotionScope.slideOutToLeftOffset)
            .isEqualTo(
                -mockPaneScaffoldMotionScope.currentPanePositions[1].x -
                    mockPaneScaffoldMotionScope.currentPaneSizes[1].width
            )
    }

    @Test
    fun slideOutToRightOffset_noExitToRightPane_equalsZero() {
        mockPaneScaffoldMotionScope.paneMotions =
            listOf(EnterFromRight, EnterFromRight, EnterFromRight)
        assertThat(mockPaneScaffoldMotionScope.slideOutToRightOffset).isEqualTo(0)
    }

    @Test
    fun slideOutToRightOffset_withExitToRightPane_useTheLeftestEdge() {
        mockPaneScaffoldMotionScope.paneMotions = listOf(ExitToLeft, ExitToRight, ExitToRight)
        assertThat(mockPaneScaffoldMotionScope.slideOutToRightOffset)
            .isEqualTo(
                mockPaneScaffoldMotionScope.scaffoldSize.width -
                    mockPaneScaffoldMotionScope.currentPanePositions[1].x
            )
    }
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
private fun Int.toThreePaneScaffoldValue(): ThreePaneScaffoldValue {
    return when (this) {
        0 -> ThreePaneScaffoldValue(Hidden, Hidden, Hidden)
        1 -> ThreePaneScaffoldValue(Expanded, Hidden, Hidden)
        2 -> ThreePaneScaffoldValue(Hidden, Expanded, Hidden)
        3 -> ThreePaneScaffoldValue(Hidden, Hidden, Expanded)
        4 -> ThreePaneScaffoldValue(Expanded, Expanded, Hidden)
        5 -> ThreePaneScaffoldValue(Expanded, Hidden, Expanded)
        6 -> ThreePaneScaffoldValue(Hidden, Expanded, Expanded)
        7 -> ThreePaneScaffoldValue(Expanded, Expanded, Expanded)
        else -> throw AssertionError("Unexpected scaffold value: $this")
    }
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
private val MockThreePaneOrder =
    ThreePaneScaffoldHorizontalOrder(
        ThreePaneScaffoldRole.Primary,
        ThreePaneScaffoldRole.Secondary,
        ThreePaneScaffoldRole.Tertiary
    )

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
private val ExpectedThreePaneMotions =
    listOf(
        // From H, H, H
        listOf(
            listOf(NoMotion, NoMotion, NoMotion), // To H, H, H
            listOf(EnterFromRight, NoMotion, NoMotion), // To V, H, H
            listOf(NoMotion, EnterFromRight, NoMotion), // To H, V, H
            listOf(NoMotion, NoMotion, EnterFromRight), // To H, H, V
            listOf(EnterFromRight, EnterFromRight, NoMotion), // To V, V, H
            listOf(EnterFromRight, NoMotion, EnterFromRight), // To V, H, V
            listOf(NoMotion, EnterFromRight, EnterFromRight), // To H, V, V
            listOf(EnterFromRight, EnterFromRight, EnterFromRight), // To V, V, V
        ),
        // From V, H, H
        listOf(
            listOf(ExitToRight, NoMotion, NoMotion), // To H, H, H
            listOf(AnimateBounds, NoMotion, NoMotion), // To V, H, H
            listOf(ExitToLeft, EnterFromRight, NoMotion), // To H, V, H
            listOf(ExitToLeft, NoMotion, EnterFromRight), // To H, H, V
            listOf(AnimateBounds, EnterFromRight, NoMotion), // To V, V, H
            listOf(AnimateBounds, NoMotion, EnterFromRight), // To V, H, V
            listOf(ExitToLeft, EnterFromRight, EnterFromRight), // To H, V, V
            listOf(AnimateBounds, EnterFromRight, EnterFromRight), // To V, V, V
        ),
        // From H, V, H
        listOf(
            listOf(NoMotion, ExitToRight, NoMotion), // To H, H, H
            listOf(EnterFromLeft, ExitToRight, NoMotion), // To V, H, H
            listOf(NoMotion, AnimateBounds, NoMotion), // To H, V, H
            listOf(NoMotion, ExitToLeft, EnterFromRight), // To H, H, V
            listOf(EnterFromLeft, AnimateBounds, NoMotion), // To V, V, H
            listOf(EnterFromLeft, ExitToRight, EnterFromRightDelayed), // To V, H, V
            listOf(NoMotion, AnimateBounds, EnterFromRight), // To H, V, V
            listOf(EnterFromLeft, AnimateBounds, EnterFromRight), // To V, V, V
        ),
        // From H, H, V
        listOf(
            listOf(NoMotion, NoMotion, ExitToRight), // To H, H, H
            listOf(EnterFromLeft, NoMotion, ExitToRight), // To V, H, H
            listOf(NoMotion, EnterFromLeft, ExitToRight), // To H, V, H
            listOf(NoMotion, NoMotion, AnimateBounds), // To H, H, V
            listOf(EnterFromLeft, EnterFromLeft, ExitToRight), // To V, V, H
            listOf(EnterFromLeft, NoMotion, AnimateBounds), // To V, H, V
            listOf(NoMotion, EnterFromLeft, AnimateBounds), // To H, V, V
            listOf(EnterFromLeft, EnterFromLeft, AnimateBounds), // To V, V, V
        ),
        // From V, V, H
        listOf(
            listOf(ExitToRight, ExitToRight, NoMotion), // To H, H, H
            listOf(AnimateBounds, ExitToRight, NoMotion), // To V, H, H
            listOf(ExitToLeft, AnimateBounds, NoMotion), // To H, V, H
            listOf(ExitToLeft, ExitToLeft, EnterFromRight), // To H, H, V
            listOf(AnimateBounds, AnimateBounds, NoMotion), // To V, V, H
            listOf(AnimateBounds, ExitToRight, EnterFromRightDelayed), // To V, H, V
            listOf(ExitToLeft, AnimateBounds, EnterFromRight), // To H, V, V
            listOf(AnimateBounds, AnimateBounds, EnterFromRight), // To V, V, V
        ),
        // From V, H, V
        listOf(
            listOf(ExitToRight, NoMotion, ExitToRight), // To H, H, H
            listOf(AnimateBounds, NoMotion, ExitToRight), // To V, H, H
            listOf(ExitToLeft, EnterFromRightDelayed, ExitToRight), // To H, V, H
            listOf(ExitToLeft, NoMotion, AnimateBounds), // To H, H, V
            listOf(AnimateBounds, EnterFromRightDelayed, ExitToRight), // To V, V, H
            listOf(AnimateBounds, NoMotion, AnimateBounds), // To V, H, V
            listOf(ExitToLeft, EnterFromLeftDelayed, AnimateBounds), // To H, V, V
            listOf(AnimateBounds, EnterWithExpand, AnimateBounds), // To V, V, V
        ),
        // From H, V, V
        listOf(
            listOf(NoMotion, ExitToRight, ExitToRight), // To H, H, H
            listOf(EnterFromLeft, ExitToRight, ExitToRight), // To V, H, H
            listOf(NoMotion, AnimateBounds, ExitToRight), // To H, V, H
            listOf(NoMotion, ExitToLeft, AnimateBounds), // To H, H, V
            listOf(EnterFromLeft, AnimateBounds, ExitToRight), // To V, V, H
            listOf(EnterFromLeftDelayed, ExitToLeft, AnimateBounds), // To V, H, V
            listOf(NoMotion, AnimateBounds, AnimateBounds), // To H, V, V
            listOf(EnterFromLeft, AnimateBounds, AnimateBounds), // To V, V, V
        ),
        // From V, V, V
        listOf(
            listOf(ExitToRight, ExitToRight, ExitToRight), // To H, H, H
            listOf(AnimateBounds, ExitToRight, ExitToRight), // To V, H, H
            listOf(ExitToLeft, AnimateBounds, ExitToRight), // To H, V, H
            listOf(ExitToLeft, ExitToLeft, AnimateBounds), // To H, H, V
            listOf(AnimateBounds, AnimateBounds, ExitToRight), // To V, V, H
            listOf(AnimateBounds, ExitWithShrink, AnimateBounds), // To V, H, V
            listOf(ExitToLeft, AnimateBounds, AnimateBounds), // To H, V, V
            listOf(AnimateBounds, AnimateBounds, AnimateBounds), // To V, V, V
        ),
    )

@Suppress("PrimitiveInCollection") // No way to get underlying Long of IntSize or IntOffset
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
private val mockPaneScaffoldMotionScope =
    object : PaneScaffoldMotionScope {
        override val positionAnimationSpec: FiniteAnimationSpec<IntOffset> = tween()
        override val sizeAnimationSpec: FiniteAnimationSpec<IntSize> = spring()
        override val delayedPositionAnimationSpec: FiniteAnimationSpec<IntOffset> = snap()
        override val scaffoldSize: IntSize = IntSize(1000, 1000)
        override var currentPaneSizes: List<IntSize> =
            listOf(IntSize(1, 2), IntSize(3, 4), IntSize(5, 6))
        override var currentPanePositions: List<IntOffset> =
            listOf(IntOffset(3, 4), IntOffset(5, 6), IntOffset(7, 8))
        override var targetPaneSizes: List<IntSize> =
            listOf(IntSize(3, 4), IntSize(5, 6), IntSize(7, 8))
        override var targetPanePositions: List<IntOffset> =
            listOf(IntOffset(5, 6), IntOffset(7, 8), IntOffset(9, 0))
        override var paneMotions: List<PaneMotion> =
            listOf(ExitToLeft, EnterFromRight, EnterFromRight)
        override val motionProgress = 0.5F
    }

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
private val mockEnterFromLeftTransition =
    slideInHorizontally(mockPaneScaffoldMotionScope.positionAnimationSpec) {
        mockPaneScaffoldMotionScope.slideInFromLeftOffset
    }

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
private val mockEnterFromRightTransition =
    slideInHorizontally(mockPaneScaffoldMotionScope.positionAnimationSpec) {
        mockPaneScaffoldMotionScope.slideInFromRightOffset
    }

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
private val mockEnterFromLeftDelayedTransition =
    slideInHorizontally(mockPaneScaffoldMotionScope.delayedPositionAnimationSpec) {
        mockPaneScaffoldMotionScope.slideInFromLeftOffset
    }

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
private val mockEnterFromRightDelayedTransition =
    slideInHorizontally(mockPaneScaffoldMotionScope.delayedPositionAnimationSpec) {
        mockPaneScaffoldMotionScope.slideInFromLeftOffset
    }

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
private val mockExitToLeftTransition =
    slideOutHorizontally(mockPaneScaffoldMotionScope.positionAnimationSpec) {
        mockPaneScaffoldMotionScope.slideOutToLeftOffset
    }

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
private val mockExitToRightTransition =
    slideOutHorizontally(mockPaneScaffoldMotionScope.positionAnimationSpec) {
        mockPaneScaffoldMotionScope.slideOutToRightOffset
    }

private val mockEnterWithExpandTransition =
    expandHorizontally(mockPaneScaffoldMotionScope.sizeAnimationSpec, Alignment.CenterHorizontally)

private val mockExitWithShrinkTransition =
    shrinkHorizontally(mockPaneScaffoldMotionScope.sizeAnimationSpec, Alignment.CenterHorizontally)
