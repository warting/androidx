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

package androidx.wear.compose.material3

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.testutils.assertAgainstGolden
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.LayoutDirection
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.test.screenshot.AndroidXScreenshotTestRule
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestName
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 35, maxSdkVersion = 35)
class CardScreenshotTest {

    @get:Rule val rule = createComposeRule()

    @get:Rule val screenshotRule = AndroidXScreenshotTestRule(SCREENSHOT_GOLDEN_PATH)

    @get:Rule val testName = TestName()

    @Test fun card_ltr() = verifyScreenshot(layoutDirection = LayoutDirection.Ltr) { TestCard() }

    @Test
    fun card_disabled() =
        verifyScreenshot(layoutDirection = LayoutDirection.Ltr) { TestCard(enabled = false) }

    @Test fun card_rtl() = verifyScreenshot(layoutDirection = LayoutDirection.Rtl) { TestCard() }

    @Test
    fun card_image_background() = verifyScreenshot {
        TestCardWithContainerPainter(
            image =
                painterResource(
                    id = androidx.wear.compose.material3.test.R.drawable.backgroundimage1
                ),
            sizeToIntrinsics = false,
        )
    }

    @Test
    fun card_image_background_with_intrinsic_size() = verifyScreenshot {
        TestCardWithContainerPainter(
            image =
                painterResource(
                    id = androidx.wear.compose.material3.test.R.drawable.backgroundimage1
                ),
            sizeToIntrinsics = true,
        )
    }

    @Test
    fun outlined_card_ltr() =
        verifyScreenshot(layoutDirection = LayoutDirection.Ltr) {
            OutlinedCard(
                onClick = {},
                modifier = Modifier.testTag(TEST_TAG).width(IntrinsicSize.Max),
            ) {
                Text("Outlined Card: Some body content")
            }
        }

    @Test
    fun outlined_card_disabled() =
        verifyScreenshot(layoutDirection = LayoutDirection.Ltr) {
            OutlinedCard(
                onClick = {},
                modifier = Modifier.testTag(TEST_TAG).width(IntrinsicSize.Max),
                enabled = false,
            ) {
                Text("Outlined Card: Some body content")
            }
        }

    @Test
    fun outlined_card_rtl() =
        verifyScreenshot(layoutDirection = LayoutDirection.Rtl) {
            OutlinedCard(
                onClick = {},
                modifier = Modifier.testTag(TEST_TAG).width(IntrinsicSize.Max),
            ) {
                Text("Outlined Card: Some body content")
            }
        }

    @Test
    fun app_card_ltr() = verifyScreenshot(layoutDirection = LayoutDirection.Ltr) { TestAppCard() }

    @Test
    fun app_card_disabled() =
        verifyScreenshot(layoutDirection = LayoutDirection.Ltr) { TestAppCard(enabled = false) }

    @Test
    fun app_card_rtl() = verifyScreenshot(layoutDirection = LayoutDirection.Rtl) { TestAppCard() }

    @Test
    fun title_card_ltr() =
        verifyScreenshot(layoutDirection = LayoutDirection.Ltr) { TestTitleCard() }

    @Test
    fun title_card_disabled() =
        verifyScreenshot(layoutDirection = LayoutDirection.Ltr) { TestTitleCard(enabled = false) }

    @Test
    fun title_card_rtl() =
        verifyScreenshot(layoutDirection = LayoutDirection.Rtl) { TestTitleCard() }

    @Test
    fun title_card_with_time_and_subtitle_ltr() =
        verifyScreenshot(layoutDirection = LayoutDirection.Ltr) {
            TestTitleCardWithTimeAndSubtitle()
        }

    @Test
    fun title_card_without_time_and_with_subtitle_ltr() =
        verifyScreenshot(layoutDirection = LayoutDirection.Ltr) {
            TitleCard(
                enabled = true,
                onClick = {},
                title = { Text("TitleCard") },
                subtitle = { Text("Subtitle") },
                modifier = Modifier.testTag(TEST_TAG),
            )
        }

    @Test
    fun title_card_with_time_and_subtitle_disabled() =
        verifyScreenshot(layoutDirection = LayoutDirection.Ltr) {
            TestTitleCardWithTimeAndSubtitle(enabled = false)
        }

    @Test
    fun title_card_with_time_and_subtitle_rtl() =
        verifyScreenshot(layoutDirection = LayoutDirection.Rtl) {
            TestTitleCardWithTimeAndSubtitle()
        }

    @Test
    fun title_card_with_content_time_and_subtitle_ltr() =
        verifyScreenshot(layoutDirection = LayoutDirection.Ltr) {
            TestTitleCardWithContentTimeAndSubtitle()
        }

    @Test
    fun title_card_with_content_time_and_subtitle_disabled() =
        verifyScreenshot(layoutDirection = LayoutDirection.Ltr) {
            TestTitleCardWithContentTimeAndSubtitle(enabled = false)
        }

    @Test
    fun title_card_with_content_time_and_subtitle_rtl() =
        verifyScreenshot(layoutDirection = LayoutDirection.Rtl) {
            TestTitleCardWithContentTimeAndSubtitle()
        }

    @Test
    fun title_card_image_background() = verifyScreenshot {
        TestTitleCardWithContainerPainter(
            image =
                painterResource(
                    id = androidx.wear.compose.material3.test.R.drawable.backgroundimage1
                ),
            sizeToIntrinsics = false,
        )
    }

    @Composable
    private fun TestCard(
        enabled: Boolean = true,
        colors: CardColors = CardDefaults.cardColors(),
        contentPadding: PaddingValues = CardDefaults.ContentPadding,
    ) {
        Card(
            enabled = enabled,
            onClick = {},
            colors = colors,
            contentPadding = contentPadding,
            modifier = Modifier.testTag(TEST_TAG).width(IntrinsicSize.Max),
        ) {
            Text("Card: Some body content")
        }
    }

    @Composable
    private fun TestAppCard(
        enabled: Boolean = true,
        colors: CardColors = CardDefaults.cardColors(),
    ) {
        AppCard(
            enabled = enabled,
            onClick = {},
            appName = { Text("AppName") },
            appImage = { TestIcon() },
            title = { Text("AppCard") },
            colors = colors,
            time = { Text("now") },
            modifier = Modifier.testTag(TEST_TAG).width(IntrinsicSize.Max),
        ) {
            Text("Some body content and some more body content")
        }
    }

    @Composable
    private fun TestTitleCard(
        enabled: Boolean = true,
        contentPadding: PaddingValues = CardDefaults.ContentPadding,
        colors: CardColors = CardDefaults.cardColors(),
    ) {
        TitleCard(
            enabled = enabled,
            onClick = {},
            title = { Text("TitleCard") },
            time = { Text("now") },
            colors = colors,
            contentPadding = contentPadding,
            modifier = Modifier.testTag(TEST_TAG).width(IntrinsicSize.Max),
        ) {
            Text("Some body content and some more body content")
        }
    }

    @Composable
    fun TestCardWithContainerPainter(
        image: Painter,
        sizeToIntrinsics: Boolean,
        enabled: Boolean = true,
        contentPadding: PaddingValues = CardDefaults.CardWithContainerPainterContentPadding,
        colors: CardColors = CardDefaults.cardWithContainerPainterColors(),
    ) {
        Card(
            containerPainter =
                CardDefaults.containerPainter(image = image, sizeToIntrinsics = sizeToIntrinsics),
            enabled = enabled,
            onClick = {},
            colors = colors,
            contentPadding = contentPadding,
            modifier = Modifier.testTag(TEST_TAG).width(IntrinsicSize.Max),
        ) {
            Text("Card: Some body content")
        }
    }

    @Composable
    fun TestTitleCardWithContainerPainter(
        image: Painter,
        sizeToIntrinsics: Boolean,
        title: String = "TitleCard",
        time: String = "now",
        enabled: Boolean = true,
        contentPadding: PaddingValues = CardDefaults.CardWithContainerPainterContentPadding,
        colors: CardColors = CardDefaults.cardWithContainerPainterColors(),
    ) {
        TitleCard(
            containerPainter =
                CardDefaults.containerPainter(image = image, sizeToIntrinsics = sizeToIntrinsics),
            title = { Text(title) },
            time = { Text(time) },
            enabled = enabled,
            onClick = {},
            colors = colors,
            contentPadding = contentPadding,
            modifier = Modifier.testTag(TEST_TAG).width(IntrinsicSize.Max),
        ) {
            Text("Some body content and some more body content")
        }
    }

    @Composable
    private fun TestTitleCardWithTimeAndSubtitle(enabled: Boolean = true) {
        TitleCard(
            enabled = enabled,
            onClick = {},
            time = { Text("XXm") },
            title = { Text("TitleCard") },
            subtitle = { Text("Subtitle") },
            modifier = Modifier.testTag(TEST_TAG),
        )
    }

    @Composable
    private fun TestTitleCardWithContentTimeAndSubtitle(enabled: Boolean = true) {
        TitleCard(
            enabled = enabled,
            onClick = {},
            time = { Text("XXm") },
            title = { Text("TitleCard") },
            subtitle = { Text("Subtitle") },
            modifier = Modifier.testTag(TEST_TAG),
        ) {
            Text("Card content")
        }
    }

    private fun verifyScreenshot(
        layoutDirection: LayoutDirection = LayoutDirection.Ltr,
        content: @Composable () -> Unit,
    ) {
        rule.setContentWithTheme {
            CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                Box(
                    modifier =
                        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
                ) {
                    content()
                }
            }
        }

        rule
            .onNodeWithTag(TEST_TAG)
            .captureToImage()
            .assertAgainstGolden(screenshotRule, testName.methodName)
    }
}
