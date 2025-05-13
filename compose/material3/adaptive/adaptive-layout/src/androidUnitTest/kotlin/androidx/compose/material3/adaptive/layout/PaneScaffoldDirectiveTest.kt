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

package androidx.compose.material3.adaptive.layout

import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.HingeInfo
import androidx.compose.material3.adaptive.Posture
import androidx.compose.material3.adaptive.WindowAdaptiveInfo
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowSizeClass
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@RunWith(JUnit4::class)
class PaneScaffoldDirectiveTest {
    @Test
    fun test_calculateStandardPaneScaffoldDirective_compactWidth() {
        val scaffoldDirective =
            calculatePaneScaffoldDirective(WindowAdaptiveInfo(WindowSizeClass(0, 0), Posture()))

        assertThat(scaffoldDirective.maxHorizontalPartitions).isEqualTo(1)
        assertThat(scaffoldDirective.maxVerticalPartitions).isEqualTo(1)
        assertThat(scaffoldDirective.horizontalPartitionSpacerSize).isEqualTo(0.dp)
        assertThat(scaffoldDirective.verticalPartitionSpacerSize).isEqualTo(0.dp)
        assertThat(scaffoldDirective.defaultPanePreferredWidth).isEqualTo(360.dp)
        assertThat(scaffoldDirective.defaultPanePreferredHeight).isEqualTo(420.dp)
    }

    @Test
    fun test_calculateStandardPaneScaffoldDirective_compactWidthAndMediumHeight() {
        val scaffoldDirective =
            calculatePaneScaffoldDirective(
                WindowAdaptiveInfo(
                    WindowSizeClass(0, WindowSizeClass.HEIGHT_DP_MEDIUM_LOWER_BOUND),
                    Posture()
                )
            )

        assertThat(scaffoldDirective.maxHorizontalPartitions).isEqualTo(1)
        assertThat(scaffoldDirective.maxVerticalPartitions).isEqualTo(1)
        assertThat(scaffoldDirective.horizontalPartitionSpacerSize).isEqualTo(0.dp)
        assertThat(scaffoldDirective.verticalPartitionSpacerSize).isEqualTo(0.dp)
        assertThat(scaffoldDirective.defaultPanePreferredWidth).isEqualTo(360.dp)
        assertThat(scaffoldDirective.defaultPanePreferredHeight).isEqualTo(420.dp)
    }

    @Test
    fun test_calculateStandardPaneScaffoldDirective_mediumWidthAndExpandedHeight() {
        val scaffoldDirective =
            calculatePaneScaffoldDirective(
                WindowAdaptiveInfo(
                    WindowSizeClass(
                        WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND,
                        WindowSizeClass.HEIGHT_DP_EXPANDED_LOWER_BOUND
                    ),
                    Posture()
                )
            )

        assertThat(scaffoldDirective.maxHorizontalPartitions).isEqualTo(1)
        assertThat(scaffoldDirective.maxVerticalPartitions).isEqualTo(2)
        assertThat(scaffoldDirective.horizontalPartitionSpacerSize).isEqualTo(0.dp)
        assertThat(scaffoldDirective.verticalPartitionSpacerSize).isEqualTo(24.dp)
        assertThat(scaffoldDirective.defaultPanePreferredWidth).isEqualTo(360.dp)
        assertThat(scaffoldDirective.defaultPanePreferredHeight).isEqualTo(420.dp)
    }

    @Test
    fun test_calculateStandardPaneScaffoldDirective_expandedWidthAndExpandedHeight() {
        val scaffoldDirective =
            calculatePaneScaffoldDirective(
                WindowAdaptiveInfo(
                    WindowSizeClass(
                        WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND,
                        WindowSizeClass.HEIGHT_DP_EXPANDED_LOWER_BOUND
                    ),
                    Posture()
                )
            )

        assertThat(scaffoldDirective.maxHorizontalPartitions).isEqualTo(2)
        assertThat(scaffoldDirective.maxVerticalPartitions).isEqualTo(1)
        assertThat(scaffoldDirective.horizontalPartitionSpacerSize).isEqualTo(24.dp)
        assertThat(scaffoldDirective.verticalPartitionSpacerSize).isEqualTo(0.dp)
        assertThat(scaffoldDirective.defaultPanePreferredWidth).isEqualTo(360.dp)
        assertThat(scaffoldDirective.defaultPanePreferredHeight).isEqualTo(420.dp)
    }

    @Test
    fun test_calculateStandardPaneScaffoldDirective_extraLargeWidth() {
        val scaffoldDirective =
            calculatePaneScaffoldDirective(
                WindowAdaptiveInfo(
                    WindowSizeClass(1600, WindowSizeClass.HEIGHT_DP_EXPANDED_LOWER_BOUND),
                    Posture()
                )
            )

        assertThat(scaffoldDirective.maxHorizontalPartitions).isEqualTo(3)
        assertThat(scaffoldDirective.maxVerticalPartitions).isEqualTo(1)
        assertThat(scaffoldDirective.horizontalPartitionSpacerSize).isEqualTo(24.dp)
        assertThat(scaffoldDirective.verticalPartitionSpacerSize).isEqualTo(0.dp)
        assertThat(scaffoldDirective.defaultPanePreferredWidth).isEqualTo(412.dp)
    }

    @Test
    fun test_calculateStandardPaneScaffoldDirective_tabletop() {
        val scaffoldDirective =
            calculatePaneScaffoldDirective(
                WindowAdaptiveInfo(
                    WindowSizeClass(
                        WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND,
                        WindowSizeClass.HEIGHT_DP_EXPANDED_LOWER_BOUND
                    ),
                    Posture(isTabletop = true)
                )
            )

        assertThat(scaffoldDirective.maxHorizontalPartitions).isEqualTo(1)
        assertThat(scaffoldDirective.maxVerticalPartitions).isEqualTo(2)
        assertThat(scaffoldDirective.horizontalPartitionSpacerSize).isEqualTo(0.dp)
        assertThat(scaffoldDirective.verticalPartitionSpacerSize).isEqualTo(24.dp)
        assertThat(scaffoldDirective.defaultPanePreferredWidth).isEqualTo(360.dp)
        assertThat(scaffoldDirective.defaultPanePreferredHeight).isEqualTo(420.dp)
    }

    @Test
    fun test_calculateDensePaneScaffoldDirective_compactWidth() {
        val scaffoldDirective =
            calculatePaneScaffoldDirectiveWithTwoPanesOnMediumWidth(
                WindowAdaptiveInfo(
                    WindowSizeClass(0, WindowSizeClass.HEIGHT_DP_MEDIUM_LOWER_BOUND),
                    Posture()
                )
            )

        assertThat(scaffoldDirective.maxHorizontalPartitions).isEqualTo(1)
        assertThat(scaffoldDirective.maxVerticalPartitions).isEqualTo(1)
        assertThat(scaffoldDirective.horizontalPartitionSpacerSize).isEqualTo(0.dp)
        assertThat(scaffoldDirective.verticalPartitionSpacerSize).isEqualTo(0.dp)
        assertThat(scaffoldDirective.defaultPanePreferredWidth).isEqualTo(360.dp)
    }

    @Test
    fun test_calculateDensePaneScaffoldDirective_mediumWidthAndExpandedHeight() {
        val scaffoldDirective =
            calculatePaneScaffoldDirectiveWithTwoPanesOnMediumWidth(
                WindowAdaptiveInfo(
                    WindowSizeClass(
                        WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND,
                        WindowSizeClass.HEIGHT_DP_EXPANDED_LOWER_BOUND
                    ),
                    Posture()
                )
            )

        assertThat(scaffoldDirective.maxHorizontalPartitions).isEqualTo(2)
        assertThat(scaffoldDirective.maxVerticalPartitions).isEqualTo(1)
        assertThat(scaffoldDirective.horizontalPartitionSpacerSize).isEqualTo(24.dp)
        assertThat(scaffoldDirective.verticalPartitionSpacerSize).isEqualTo(0.dp)
        assertThat(scaffoldDirective.defaultPanePreferredWidth).isEqualTo(360.dp)
    }

    @Test
    fun test_calculateDensePaneScaffoldDirective_expandedWidthAndExpandedHeight() {
        val scaffoldDirective =
            calculatePaneScaffoldDirectiveWithTwoPanesOnMediumWidth(
                WindowAdaptiveInfo(
                    WindowSizeClass(
                        WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND,
                        WindowSizeClass.HEIGHT_DP_EXPANDED_LOWER_BOUND
                    ),
                    Posture()
                )
            )

        assertThat(scaffoldDirective.maxHorizontalPartitions).isEqualTo(2)
        assertThat(scaffoldDirective.maxVerticalPartitions).isEqualTo(1)
        assertThat(scaffoldDirective.horizontalPartitionSpacerSize).isEqualTo(24.dp)
        assertThat(scaffoldDirective.verticalPartitionSpacerSize).isEqualTo(0.dp)
        assertThat(scaffoldDirective.defaultPanePreferredWidth).isEqualTo(360.dp)
    }

    @Test
    fun test_calculateDensePaneScaffoldDirective_extraLargeWidth() {
        val scaffoldDirective =
            calculatePaneScaffoldDirectiveWithTwoPanesOnMediumWidth(
                WindowAdaptiveInfo(
                    WindowSizeClass(1600, WindowSizeClass.HEIGHT_DP_EXPANDED_LOWER_BOUND),
                    Posture()
                )
            )

        assertThat(scaffoldDirective.maxHorizontalPartitions).isEqualTo(3)
        assertThat(scaffoldDirective.maxVerticalPartitions).isEqualTo(1)
        assertThat(scaffoldDirective.horizontalPartitionSpacerSize).isEqualTo(24.dp)
        assertThat(scaffoldDirective.verticalPartitionSpacerSize).isEqualTo(0.dp)
        assertThat(scaffoldDirective.defaultPanePreferredWidth).isEqualTo(412.dp)
    }

    @Test
    fun test_calculateDensePaneScaffoldDirective_tabletop() {
        val scaffoldDirective =
            calculatePaneScaffoldDirectiveWithTwoPanesOnMediumWidth(
                WindowAdaptiveInfo(
                    WindowSizeClass(
                        WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND,
                        WindowSizeClass.HEIGHT_DP_EXPANDED_LOWER_BOUND
                    ),
                    Posture(isTabletop = true)
                )
            )

        assertThat(scaffoldDirective.maxHorizontalPartitions).isEqualTo(2)
        assertThat(scaffoldDirective.maxVerticalPartitions).isEqualTo(2)
        assertThat(scaffoldDirective.horizontalPartitionSpacerSize).isEqualTo(24.dp)
        assertThat(scaffoldDirective.verticalPartitionSpacerSize).isEqualTo(24.dp)
        assertThat(scaffoldDirective.defaultPanePreferredWidth).isEqualTo(360.dp)
    }

    @Test
    fun test_calculateStandardPaneScaffoldDirective_alwaysAvoidHinge() {
        val scaffoldDirective =
            calculatePaneScaffoldDirective(
                WindowAdaptiveInfo(
                    WindowSizeClass(
                        WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND,
                        WindowSizeClass.HEIGHT_DP_EXPANDED_LOWER_BOUND
                    ),
                    Posture(hingeList = hingeList)
                ),
                HingePolicy.AlwaysAvoid
            )

        assertThat(scaffoldDirective.excludedBounds).isEqualTo(hingeList.getBounds())
    }

    @Test
    fun test_calculateStandardPaneScaffoldDirective_avoidOccludingHinge() {
        val scaffoldDirective =
            calculatePaneScaffoldDirective(
                WindowAdaptiveInfo(
                    WindowSizeClass(
                        WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND,
                        WindowSizeClass.HEIGHT_DP_EXPANDED_LOWER_BOUND
                    ),
                    Posture(hingeList = hingeList)
                ),
                HingePolicy.AvoidOccluding
            )

        assertThat(scaffoldDirective.excludedBounds).isEqualTo(hingeList.subList(0, 2).getBounds())
    }

    @Test
    fun test_calculateStandardPaneScaffoldDirective_avoidSeparatingHinge() {
        val scaffoldDirective =
            calculatePaneScaffoldDirective(
                WindowAdaptiveInfo(
                    WindowSizeClass(
                        WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND,
                        WindowSizeClass.HEIGHT_DP_EXPANDED_LOWER_BOUND
                    ),
                    Posture(hingeList = hingeList)
                ),
                HingePolicy.AvoidSeparating
            )

        assertThat(scaffoldDirective.excludedBounds).isEqualTo(hingeList.subList(2, 3).getBounds())
    }

    @Test
    fun test_calculateStandardPaneScaffoldDirective_neverAvoidHinge() {
        val scaffoldDirective =
            calculatePaneScaffoldDirective(
                WindowAdaptiveInfo(
                    WindowSizeClass(
                        WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND,
                        WindowSizeClass.HEIGHT_DP_EXPANDED_LOWER_BOUND
                    ),
                    Posture(hingeList = hingeList)
                ),
                HingePolicy.NeverAvoid
            )

        assertThat(scaffoldDirective.excludedBounds).isEmpty()
    }

    @Test
    fun test_calculateDensePaneScaffoldDirective_alwaysAvoidHinge() {
        val scaffoldDirective =
            calculatePaneScaffoldDirectiveWithTwoPanesOnMediumWidth(
                WindowAdaptiveInfo(
                    WindowSizeClass(
                        WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND,
                        WindowSizeClass.HEIGHT_DP_EXPANDED_LOWER_BOUND
                    ),
                    Posture(hingeList = hingeList)
                ),
                HingePolicy.AlwaysAvoid
            )

        assertThat(scaffoldDirective.excludedBounds).isEqualTo(hingeList.getBounds())
    }

    @Test
    fun test_calculateDensePaneScaffoldDirective_avoidOccludingHinge() {
        val scaffoldDirective =
            calculatePaneScaffoldDirectiveWithTwoPanesOnMediumWidth(
                WindowAdaptiveInfo(
                    WindowSizeClass(
                        WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND,
                        WindowSizeClass.HEIGHT_DP_EXPANDED_LOWER_BOUND
                    ),
                    Posture(hingeList = hingeList)
                ),
                HingePolicy.AvoidOccluding
            )

        assertThat(scaffoldDirective.excludedBounds).isEqualTo(hingeList.subList(0, 2).getBounds())
    }

    @Test
    fun test_calculateDensePaneScaffoldDirective_avoidSeparatingHinge() {
        val scaffoldDirective =
            calculatePaneScaffoldDirectiveWithTwoPanesOnMediumWidth(
                WindowAdaptiveInfo(
                    WindowSizeClass(
                        WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND,
                        WindowSizeClass.HEIGHT_DP_EXPANDED_LOWER_BOUND
                    ),
                    Posture(hingeList = hingeList)
                ),
                HingePolicy.AvoidSeparating
            )

        assertThat(scaffoldDirective.excludedBounds).isEqualTo(hingeList.subList(2, 3).getBounds())
    }

    @Test
    fun test_calculateDensePaneScaffoldDirective_neverAvoidHinge() {
        val scaffoldDirective =
            calculatePaneScaffoldDirectiveWithTwoPanesOnMediumWidth(
                WindowAdaptiveInfo(
                    WindowSizeClass(
                        WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND,
                        WindowSizeClass.HEIGHT_DP_EXPANDED_LOWER_BOUND
                    ),
                    Posture(hingeList = hingeList)
                ),
                HingePolicy.NeverAvoid
            )

        assertThat(scaffoldDirective.excludedBounds).isEmpty()
    }
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
private val hingeList =
    listOf(
        HingeInfo(
            bounds = Rect(0F, 0F, 1F, 1F),
            isFlat = true,
            isVertical = true,
            isSeparating = false,
            isOccluding = true
        ),
        HingeInfo(
            bounds = Rect(1F, 1F, 2F, 2F),
            isFlat = false,
            isVertical = true,
            isSeparating = false,
            isOccluding = true
        ),
        HingeInfo(
            bounds = Rect(2F, 2F, 3F, 3F),
            isFlat = true,
            isVertical = true,
            isSeparating = true,
            isOccluding = false
        ),
    )

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
private fun List<HingeInfo>.getBounds(): List<Rect> {
    return map { it.bounds }
}
