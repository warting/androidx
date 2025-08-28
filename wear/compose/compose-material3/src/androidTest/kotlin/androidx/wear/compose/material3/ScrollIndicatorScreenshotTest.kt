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

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.testutils.assertAgainstGolden
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.test.screenshot.AndroidXScreenshotTestRule
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestName
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 35, maxSdkVersion = 35)
class ScrollIndicatorScreenshotTest {

    @get:Rule val rule = createComposeRule()

    @get:Rule val screenshotRule = AndroidXScreenshotTestRule(SCREENSHOT_GOLDEN_PATH)

    @get:Rule val testName = TestName()

    @Test
    fun position_indicator_round_size_small_position_top() =
        position_indicator_position_test(
            size = 0.2f,
            position = 0.1f,
            ltr = true,
            roundScreen = true,
            goldenIdentifier = testName.methodName,
        )

    @Test
    fun position_indicator_round_size_small_position_bottom() =
        position_indicator_position_test(
            size = 0.2f,
            position = 1f,
            ltr = true,
            roundScreen = true,
            goldenIdentifier = testName.methodName,
        )

    @Test
    fun position_indicator_round_size_medium_position_top() =
        position_indicator_position_test(
            size = 0.5f,
            position = 0.1f,
            ltr = true,
            roundScreen = true,
            goldenIdentifier = testName.methodName,
        )

    @Test
    fun position_indicator_round_size_medium_position_mid() =
        position_indicator_position_test(
            size = 0.5f,
            position = 0.5f,
            ltr = true,
            roundScreen = true,
            goldenIdentifier = testName.methodName,
        )

    @Test
    fun position_indicator_round_size_large_position_mid() =
        position_indicator_position_test(
            size = 0.8f,
            position = 0.5f,
            ltr = true,
            roundScreen = true,
            goldenIdentifier = testName.methodName,
        )

    @Test
    fun position_indicator_round_size_small_position_top_rtl() =
        position_indicator_position_test(
            size = 0.2f,
            position = 0.1f,
            ltr = false,
            roundScreen = true,
            goldenIdentifier = testName.methodName,
        )

    @Test
    fun position_indicator_round_size_medium_position_bottom_rtl() =
        position_indicator_position_test(
            size = 0.5f,
            position = 1f,
            ltr = false,
            roundScreen = true,
            goldenIdentifier = testName.methodName,
        )

    @Test
    fun position_indicator_round_with_slcAndContentPadding() {
        rule.setContentWithTheme {
            ScreenConfiguration(SCREEN_SIZE_LARGE, isRound = true) {
                val state = rememberScalingLazyListState()
                ScalingLazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    state = state,
                    contentPadding = PaddingValues(100.dp),
                    autoCentering = null,
                ) {
                    items(6) { Text("item $it", modifier = Modifier.height(70.dp)) }
                }
                ScrollIndicator(state = state, modifier = Modifier.testTag(TEST_TAG))
            }
        }

        rule.waitForIdle()

        rule.verifyScreenshot(testName, screenshotRule)
    }

    @Test
    fun position_indicator_round_with_slc_reverseLayout() {
        rule.setContentWithTheme {
            ScreenConfiguration(SCREEN_SIZE_LARGE, isRound = true) {
                val state = rememberScalingLazyListState()
                ScalingLazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    state = state,
                    reverseLayout = true,
                ) {
                    items(20) { Text("item $it", modifier = Modifier.height(50.dp)) }
                }
                ScrollIndicator(state = state, modifier = Modifier.testTag(TEST_TAG))
            }
        }

        rule.waitForIdle()

        rule.verifyScreenshot(testName, screenshotRule)
    }

    @Test
    fun position_indicator_round_with_lc_reverseLayout() {
        rule.setContentWithTheme {
            ScreenConfiguration(SCREEN_SIZE_LARGE, isRound = true) {
                val state = rememberLazyListState()
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    state = state,
                    reverseLayout = true,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    items(20) { Text("item $it", modifier = Modifier.height(50.dp)) }
                }
                ScrollIndicator(state = state, modifier = Modifier.testTag(TEST_TAG))
            }
        }

        rule.waitForIdle()

        rule.verifyScreenshot(testName, screenshotRule)
    }

    private fun position_indicator_position_test(
        size: Float,
        position: Float,
        goldenIdentifier: String,
        roundScreen: Boolean,
        screenSizeDp: Int = 250,
        ltr: Boolean = true,
    ) {
        rule.setContentWithTheme {
            val actualLayoutDirection = if (ltr) LayoutDirection.Ltr else LayoutDirection.Rtl

            val currentConfig = LocalConfiguration.current
            val updatedConfig =
                Configuration().apply {
                    setTo(currentConfig)
                    screenWidthDp = screenSizeDp
                    screenHeightDp = screenSizeDp
                    screenLayout =
                        if (roundScreen) Configuration.SCREENLAYOUT_ROUND_YES
                        else Configuration.SCREENLAYOUT_ROUND_NO
                }
            CompositionLocalProvider(
                LocalLayoutDirection provides actualLayoutDirection,
                LocalConfiguration provides updatedConfig,
            ) {
                IndicatorImpl(
                    state =
                        object : IndicatorState {
                            override val positionFraction: Float
                                get() = position

                            override val sizeFraction: Float
                                get() = size
                        },
                    indicatorHeight = 50.dp,
                    indicatorWidth = 4.dp,
                    paddingHorizontal = 2.dp,
                    modifier = Modifier.testTag(TEST_TAG).background(Color.Black),
                )
            }
        }

        rule.waitForIdle()

        rule
            .onNodeWithTag(TEST_TAG)
            .captureToImage()
            .assertAgainstGolden(screenshotRule, goldenIdentifier)
    }
}
