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
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.PaneAdaptedValue.Companion.Expanded
import androidx.compose.material3.adaptive.layout.PaneAdaptedValue.Companion.Hidden
import androidx.compose.material3.adaptive.layout.PaneMotion.Companion.AnimateBounds
import androidx.compose.material3.adaptive.layout.PaneMotion.Companion.EnterFromLeft
import androidx.compose.material3.adaptive.layout.PaneMotion.Companion.EnterFromLeftDelayed
import androidx.compose.material3.adaptive.layout.PaneMotion.Companion.EnterFromRight
import androidx.compose.material3.adaptive.layout.PaneMotion.Companion.EnterFromRightDelayed
import androidx.compose.material3.adaptive.layout.PaneMotion.Companion.EnterWithExpand
import androidx.compose.material3.adaptive.layout.PaneMotion.Companion.ExitToLeft
import androidx.compose.material3.adaptive.layout.PaneMotion.Companion.ExitToRight
import androidx.compose.material3.adaptive.layout.PaneMotion.Companion.ExitWithShrink
import androidx.compose.material3.adaptive.layout.PaneMotion.Companion.NoMotion
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
            ExitTransition.None,
        )
        EnterFromRightDelayed.assertTransitions(
            mockEnterFromRightDelayedTransition,
            ExitTransition.None,
        )
        ExitToLeft.assertTransitions(EnterTransition.None, mockExitToLeftTransition)
        ExitToRight.assertTransitions(EnterTransition.None, mockExitToRightTransition)
        EnterWithExpand.assertTransitions(mockEnterWithExpandTransition, ExitTransition.None)
        ExitWithShrink.assertTransitions(EnterTransition.None, mockExitWithShrinkTransition)
    }

    private fun PaneMotion.assertTransitions(
        expectedEnterTransition: EnterTransition,
        expectedExitTransition: ExitTransition,
    ) {
        mockPaneScaffoldMotionDataProvider.updateMotions(this, NoMotion, NoMotion)
        // Can't compare equality directly because of lambda. Check string representation instead
        assertWithMessage("Enter transition of $this: ")
            .that(
                mockPaneScaffoldMotionDataProvider
                    .calculateDefaultEnterTransition(ThreePaneScaffoldRole.Primary)
                    .toString()
            )
            .isEqualTo(expectedEnterTransition.toString())
        assertWithMessage("Exit transition of $this: ")
            .that(
                mockPaneScaffoldMotionDataProvider
                    .calculateDefaultExitTransition(ThreePaneScaffoldRole.Primary)
                    .toString()
            )
            .isEqualTo(expectedExitTransition.toString())
    }

    @Test
    fun slideInFromLeftOffset_noEnterFromLeftPane_equalsZero() {
        mockPaneScaffoldMotionDataProvider.updateMotions(
            EnterFromRight,
            EnterFromRight,
            EnterFromRight,
        )
        assertThat(mockPaneScaffoldMotionDataProvider.slideInFromLeftOffset).isEqualTo(0)
    }

    @Test
    fun slideInFromLeftOffset_withEnterFromLeftPane_useTheLeftEdgeOfPanesEnteringFromRight() {
        mockPaneScaffoldMotionDataProvider.updateMotions(
            EnterFromLeft,
            EnterFromLeft,
            EnterFromRight,
        )
        assertThat(mockPaneScaffoldMotionDataProvider.slideInFromLeftOffset)
            .isEqualTo(-mockPaneScaffoldMotionDataProvider[2].targetLeft)
    }

    @Test
    fun slideInFromLeftOffset_withEnterFromLeftPane_useTheLeftEdgeOfPanesShown() {
        mockPaneScaffoldMotionDataProvider.updateMotions(
            EnterFromLeft,
            AnimateBounds,
            AnimateBounds,
        )
        assertThat(mockPaneScaffoldMotionDataProvider.slideInFromLeftOffset)
            .isEqualTo(-mockPaneScaffoldMotionDataProvider[1].targetLeft)
    }

    @Test
    fun slideInFromLeftOffset_withNoEnteringFromRightOrShownPane_useTheRightestEdge() {
        mockPaneScaffoldMotionDataProvider.updateMotions(EnterFromLeft, EnterFromLeft, ExitToRight)
        assertThat(mockPaneScaffoldMotionDataProvider.slideInFromLeftOffset)
            .isEqualTo(-mockPaneScaffoldMotionDataProvider[1].targetRight)
    }

    @Test
    fun slideInFromLeftOffset_withEnterFromLeftDelayedPane_useTheSameEdge() {
        mockPaneScaffoldMotionDataProvider.updateMotions(
            EnterFromLeft,
            EnterFromLeftDelayed,
            EnterFromRight,
        )
        assertThat(mockPaneScaffoldMotionDataProvider.slideInFromLeftOffset)
            .isEqualTo(-mockPaneScaffoldMotionDataProvider[2].targetLeft)
    }

    @Test
    fun slideInFromRightOffset_noEnterFromRightPane_equalsZero() {
        mockPaneScaffoldMotionDataProvider.updateMotions(
            EnterFromLeft,
            EnterFromLeft,
            EnterFromLeft,
        )
        assertThat(mockPaneScaffoldMotionDataProvider.slideInFromRightOffset).isEqualTo(0)
    }

    @Test
    fun slideInFromRightOffset_withEnterFromRightPane_useTheRightEdgeOfPanesEnteringFromLeft() {
        mockPaneScaffoldMotionDataProvider.updateMotions(
            EnterFromLeft,
            EnterFromRight,
            EnterFromRight,
        )
        assertThat(mockPaneScaffoldMotionDataProvider.slideInFromRightOffset)
            .isEqualTo(
                mockPaneScaffoldMotionDataProvider.scaffoldSize.width -
                    mockPaneScaffoldMotionDataProvider[0].targetRight
            )
    }

    @Test
    fun slideInFromRightOffset_withEnterFromRightPane_useTheRightEdgeOfPanesShown() {
        mockPaneScaffoldMotionDataProvider.updateMotions(
            AnimateBounds,
            AnimateBounds,
            EnterFromRight,
        )
        assertThat(mockPaneScaffoldMotionDataProvider.slideInFromRightOffset)
            .isEqualTo(
                mockPaneScaffoldMotionDataProvider.scaffoldSize.width -
                    mockPaneScaffoldMotionDataProvider[1].targetRight
            )
    }

    @Test
    fun slideInFromRightOffset_withNoEnteringFromLeftOrShownPane_useTheLeftestEdge() {
        mockPaneScaffoldMotionDataProvider.updateMotions(ExitToLeft, ExitToLeft, EnterFromRight)
        assertThat(mockPaneScaffoldMotionDataProvider.slideInFromRightOffset)
            .isEqualTo(
                mockPaneScaffoldMotionDataProvider.scaffoldSize.width -
                    mockPaneScaffoldMotionDataProvider[2].targetLeft
            )
    }

    @Test
    fun slideInFromRightOffset_withEnterFromRightDelayedPane_useTheSameEdge() {
        mockPaneScaffoldMotionDataProvider.updateMotions(
            EnterFromLeft,
            EnterFromRightDelayed,
            EnterFromRight,
        )
        assertThat(mockPaneScaffoldMotionDataProvider.slideInFromRightOffset)
            .isEqualTo(
                mockPaneScaffoldMotionDataProvider.scaffoldSize.width -
                    mockPaneScaffoldMotionDataProvider[0].targetRight
            )
    }

    @Test
    fun slideOutToLeftOffset_noExitToLeftPane_equalsZero() {
        mockPaneScaffoldMotionDataProvider.updateMotions(
            EnterFromRight,
            EnterFromRight,
            EnterFromRight,
        )
        assertThat(mockPaneScaffoldMotionDataProvider.slideOutToLeftOffset).isEqualTo(0)
    }

    @Test
    fun slideOutToLeftOffset_withExitToLeftPane_useTheLeftEdgeOfPaneExitingToRight() {
        mockPaneScaffoldMotionDataProvider.updateMotions(ExitToLeft, ExitToLeft, ExitToRight)
        assertThat(mockPaneScaffoldMotionDataProvider.slideOutToLeftOffset)
            .isEqualTo(-mockPaneScaffoldMotionDataProvider[2].currentLeft)
    }

    @Test
    fun slideOutToLeftOffset_withExitToLeftPane_useTheLeftEdgeOfPaneShown() {
        mockPaneScaffoldMotionDataProvider.updateMotions(ExitToLeft, AnimateBounds, AnimateBounds)
        assertThat(mockPaneScaffoldMotionDataProvider.slideOutToLeftOffset)
            .isEqualTo(-mockPaneScaffoldMotionDataProvider[1].currentLeft)
    }

    @Test
    fun slideOutToLeftOffset_withNoExitToRightOrShownPane_useTheRightestEdge() {
        mockPaneScaffoldMotionDataProvider.updateMotions(ExitToLeft, ExitToLeft, EnterFromRight)
        assertThat(mockPaneScaffoldMotionDataProvider.slideOutToLeftOffset)
            .isEqualTo(-mockPaneScaffoldMotionDataProvider[1].currentRight)
    }

    @Test
    fun slideOutToRightOffset_noExitToRightPane_equalsZero() {
        mockPaneScaffoldMotionDataProvider.updateMotions(
            EnterFromRight,
            EnterFromRight,
            EnterFromRight,
        )
        assertThat(mockPaneScaffoldMotionDataProvider.slideOutToRightOffset).isEqualTo(0)
    }

    @Test
    fun slideOutToRightOffset_withExitToRightPane_useTheRightEdgeOfPaneExitingToLeft() {
        mockPaneScaffoldMotionDataProvider.updateMotions(ExitToLeft, ExitToRight, ExitToRight)
        assertThat(mockPaneScaffoldMotionDataProvider.slideOutToRightOffset)
            .isEqualTo(
                mockPaneScaffoldMotionDataProvider.scaffoldSize.width -
                    mockPaneScaffoldMotionDataProvider[0].currentRight
            )
    }

    @Test
    fun slideOutToRightOffset_withExitToRightPane_useTheRightEdgeOfPaneShown() {
        mockPaneScaffoldMotionDataProvider.updateMotions(AnimateBounds, AnimateBounds, ExitToRight)
        assertThat(mockPaneScaffoldMotionDataProvider.slideOutToRightOffset)
            .isEqualTo(
                mockPaneScaffoldMotionDataProvider.scaffoldSize.width -
                    mockPaneScaffoldMotionDataProvider[1].currentRight
            )
    }

    @Test
    fun slideOutToRightOffset_withNoExitToLeftOrShownPane_useTheLeftestEdge() {
        mockPaneScaffoldMotionDataProvider.updateMotions(EnterFromLeft, ExitToRight, ExitToRight)
        assertThat(mockPaneScaffoldMotionDataProvider.slideOutToRightOffset)
            .isEqualTo(
                mockPaneScaffoldMotionDataProvider.scaffoldSize.width -
                    mockPaneScaffoldMotionDataProvider[1].currentLeft
            )
    }

    @Test
    fun hiddenPaneCurrentLeft_useRightEdgeOfLeftShownPane() {
        mockPaneScaffoldMotionDataProvider.updateMotions(
            ExitToLeft,
            EnterFromRight,
            EnterWithExpand,
        )
        assertThat(
                mockPaneScaffoldMotionDataProvider.getHiddenPaneCurrentLeft(
                    ThreePaneScaffoldRole.Tertiary
                )
            )
            .isEqualTo(mockPaneScaffoldMotionDataProvider[0].currentRight)
    }

    @Test
    fun hidingPaneTargetLeft_useRightEdgeOfLeftShowingPane() {
        mockPaneScaffoldMotionDataProvider.updateMotions(EnterFromLeft, ExitToRight, ExitWithShrink)
        assertThat(
                mockPaneScaffoldMotionDataProvider.getHidingPaneTargetLeft(
                    ThreePaneScaffoldRole.Tertiary
                )
            )
            .isEqualTo(mockPaneScaffoldMotionDataProvider[0].targetRight)
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
        ThreePaneScaffoldRole.Tertiary,
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
private val mockPaneScaffoldMotionDataProvider =
    ThreePaneScaffoldMotionDataProvider().apply {
        update(ThreePaneMotion(ExitToLeft, EnterFromRight, EnterFromRight), MockThreePaneOrder)
        scaffoldSize = IntSize(1000, 1000)
        this[0].apply {
            motion = ExitToLeft
            originSize = IntSize(1, 2)
            originPosition = IntOffset(3, 4)
            targetSize = IntSize(3, 4)
            targetPosition = IntOffset(5, 6)
        }
        this[1].apply {
            motion = ExitToLeft
            originSize = IntSize(3, 4)
            originPosition = IntOffset(5, 6)
            targetSize = IntSize(5, 6)
            targetPosition = IntOffset(7, 8)
        }
        this[2].apply {
            motion = ExitToLeft
            originSize = IntSize(5, 6)
            originPosition = IntOffset(7, 8)
            targetSize = IntSize(7, 8)
            targetPosition = IntOffset(9, 0)
        }
    }

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
private fun ThreePaneScaffoldMotionDataProvider.updateMotions(
    primaryPaneMotion: PaneMotion,
    secondaryPaneMotion: PaneMotion,
    tertiaryPaneMotion: PaneMotion,
) {
    update(
        ThreePaneMotion(primaryPaneMotion, secondaryPaneMotion, tertiaryPaneMotion),
        MockThreePaneOrder,
    )
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
private val mockEnterFromLeftTransition =
    slideInHorizontally(PaneMotionDefaults.OffsetAnimationSpec) {
        mockPaneScaffoldMotionDataProvider.slideInFromLeftOffset
    }

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
private val mockEnterFromRightTransition =
    slideInHorizontally(PaneMotionDefaults.OffsetAnimationSpec) {
        mockPaneScaffoldMotionDataProvider.slideInFromRightOffset
    }

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
private val mockEnterFromLeftDelayedTransition =
    slideInHorizontally(PaneMotionDefaults.DelayedOffsetAnimationSpec) {
        mockPaneScaffoldMotionDataProvider.slideInFromLeftOffset
    }

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
private val mockEnterFromRightDelayedTransition =
    slideInHorizontally(PaneMotionDefaults.DelayedOffsetAnimationSpec) {
        mockPaneScaffoldMotionDataProvider.slideInFromRightOffset
    }

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
private val mockExitToLeftTransition =
    slideOutHorizontally(PaneMotionDefaults.OffsetAnimationSpec) {
        mockPaneScaffoldMotionDataProvider.slideOutToLeftOffset
    }

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
private val mockExitToRightTransition =
    slideOutHorizontally(PaneMotionDefaults.OffsetAnimationSpec) {
        mockPaneScaffoldMotionDataProvider.slideOutToRightOffset
    }

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
private val mockEnterWithExpandTransition =
    expandHorizontally(PaneMotionDefaults.SizeAnimationSpec, Alignment.CenterHorizontally) +
        slideInHorizontally(PaneMotionDefaults.OffsetAnimationSpec)

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
private val mockExitWithShrinkTransition =
    shrinkHorizontally(PaneMotionDefaults.SizeAnimationSpec, Alignment.CenterHorizontally) +
        slideOutHorizontally(PaneMotionDefaults.OffsetAnimationSpec)
