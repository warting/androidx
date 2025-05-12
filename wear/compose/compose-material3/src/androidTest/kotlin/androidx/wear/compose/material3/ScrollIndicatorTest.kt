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

import android.os.Build
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberOverscrollEffect
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.testutils.assertContainsColor
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.wear.compose.foundation.lazy.AutoCenteringParams
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.ScalingLazyListState
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnState
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import com.google.common.truth.Truth
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class ScrollIndicatorTest {
    @get:Rule val rule = createComposeRule()

    private var itemSizePx: Int = 50
    private var itemSizeDp: Dp = Dp.Infinity
    private var itemSpacingPx = 6
    private var itemSpacingDp: Dp = Dp.Infinity
    private var viewportSizeDp = Dp.Infinity

    @Before
    fun before() {
        with(rule.density) {
            itemSizeDp = itemSizePx.toDp()
            itemSpacingDp = itemSpacingPx.toDp()
            viewportSizeDp = itemSizeDp * 3f + itemSpacingDp * 2f
        }
    }

    @Test
    fun scalingLazyColumnStateAdapter_veryLongContent() {
        verifySlcPositionAndSize(
            expectedIndicatorPosition = 0f,
            expectedIndicatorSize = 0.3f,
            itemsCount = 40
        )
    }

    @Test
    fun scalingLazyColumnStateAdapter_longContent() {
        verifySlcPositionAndSize(
            expectedIndicatorPosition = 0f,
            expectedIndicatorSize = 0.3f,
            itemsCount = 15
        )
    }

    @Test
    fun scalingLazyColumnStateAdapter_mediumContent() {
        verifySlcPositionAndSize(
            expectedIndicatorPosition = 0f,
            expectedIndicatorSize = 0.5f,
            itemsCount = 6
        )
    }

    @Test
    fun scalingLazyColumnStateAdapter_shortContent() {
        verifySlcPositionAndSize(
            expectedIndicatorPosition = 0f,
            expectedIndicatorSize = 0.7f,
            itemsCount = 3
        )
    }

    @Test
    fun scalingLazyColumnStateAdapter_veryShortContent() {
        verifySlcPositionAndSize(
            expectedIndicatorPosition = 0f,
            expectedIndicatorSize = 0.7f,
            itemsCount = 1
        )
    }

    @Test
    fun scalingLazyColumnStateAdapter_mediumContent_withContentPadding() {
        val itemsCount = 6
        val contentPadding = itemSizeDp + itemSpacingDp

        // We can get an approximate indicator size by dividing viewPort size by the length of all
        // items - including visible (top) content padding.
        val expectedIndicatorSize =
            viewportSizeDp /
                (itemSizeDp * itemsCount + itemSpacingDp * (itemsCount - 1) + contentPadding)

        verifySlcPositionAndSize(
            expectedIndicatorPosition = {
                // As we centered the list at the 1st item and added a contentPadding - we
                // expect indicator value to be larger than 0.
                Truth.assertThat(it).isAtLeast(0.1f)
            },
            expectedIndicatorSize = {
                Truth.assertThat(it).isWithin(0.05f).of(expectedIndicatorSize)
            },
            autoCentering = null,
            initialCenterItemIndex = 1,
            itemsCount = itemsCount,
            contentPaddingDp = contentPadding
        )
    }

    @Test
    fun scalingLazyColumnStateAdapter_veryLongContent_scrolled() {
        verifySlcScrollToCenter(expectedIndicatorSize = 0.3f, itemsCount = 40)
    }

    @Test
    fun scalingLazyColumnStateAdapter_longContent_scrolled() {
        verifySlcScrollToCenter(expectedIndicatorSize = 0.3f, itemsCount = 16)
    }

    @Test
    fun scalingLazyColumnStateAdapter_mediumContent_scrolled() {
        verifySlcScrollToCenter(expectedIndicatorSize = 0.5f, itemsCount = 6)
    }

    @Test
    fun scalingLazyColumnStateAdapter_shortContent_scrolled() {
        val itemsCount = 4

        // By default in this test we use SLC with AutoCentering at 0th item. Last item will also be
        // centered. Knowing that, the size of the screen and size of the item, we can say that the
        // top and bottom paddings should be equal to itemSizeDp + itemSpacingDp.
        val autoCenteringPadding = itemSizeDp + itemSpacingDp
        // We can get an approximate indicator size by dividing viewPort size by the length of all
        // items - including visible (top) auto centering padding.
        val expectedIndicatorSize =
            viewportSizeDp /
                (itemSizeDp * itemsCount + itemSpacingDp * (itemsCount - 1) + autoCenteringPadding)

        verifySlcScrollToCenter(expectedIndicatorSize = expectedIndicatorSize, itemsCount = 4)
    }

    @Test
    fun scalingLazyColumnStateAdapter_mediumContent_reversed() {
        // ScrollIndicators state isn't affected by reverseLayout flag, only its
        // representation - that's why indicatorPosition remains 0.
        verifySlcPositionAndSize(
            expectedIndicatorPosition = 0f,
            expectedIndicatorSize = 0.5f,
            reverseLayout = true,
            itemsCount = 6
        )
    }

    @Test
    fun scalingLazyColumnStateAdapter_overscroll() {
        val itemsCount = 6
        val contentPadding = itemSizeDp + itemSpacingDp

        lateinit var state: ScalingLazyListState
        lateinit var indicatorState: IndicatorState
        rule.setContent {
            state = rememberScalingLazyListState()
            ScreenScaffold(scrollState = state) {
                indicatorState =
                    ScalingLazyColumnStateAdapter(
                        state,
                        rememberOverscrollEffect() as OffsetOverscrollEffect,
                        false
                    )
                ScalingLazyColumn(
                    state = state,
                    contentPadding = PaddingValues(contentPadding),
                    modifier = Modifier.testTag(TEST_TAG).requiredSize(viewportSizeDp),
                ) {
                    items(itemsCount) {
                        Box(Modifier.requiredSize(itemSizeDp).background(Color.Red)) { Text("$it") }
                    }
                }
            }
        }
        val expectedIndicatorSize =
            indicatorState.sizeFraction - ScrollIndicatorDefaults.overscrollShrinkSizeFraction

        rule.onNodeWithTag(TEST_TAG).performTouchInput {
            down(center)
            moveTo(Offset(center.x, center.y + 2000))
            // We don't lift the finger as otherwise overscroll will be reset
        }
        rule.runOnIdle {
            Truth.assertThat(indicatorState.positionFraction).isWithin(0.05f).of(0f)
            // Size fraction should be fully shrinked - equal to minimum possible size fraction
            Truth.assertThat(indicatorState.sizeFraction).isWithin(0.05f).of(expectedIndicatorSize)
        }
    }

    @Test
    fun lazyColumnStateAdapter_veryLongContent() {
        verifyLazyColumnPositionAndSize(
            expectedIndicatorPosition = 0f,
            expectedIndicatorSize = 0.3f,
            itemsCount = 40
        )
    }

    @Test
    fun lazyColumnStateAdapter_longContent() {
        verifyLazyColumnPositionAndSize(
            expectedIndicatorPosition = 0f,
            expectedIndicatorSize = 0.3f,
            itemsCount = 15
        )
    }

    @Test
    fun lazyColumnStateAdapter_mediumContent() {
        verifyLazyColumnPositionAndSize(
            expectedIndicatorPosition = 0f,
            expectedIndicatorSize = 0.5f,
            itemsCount = 6
        )
    }

    @Test
    fun lazyColumnStateAdapter_veryLongContent_scrolled() {
        verifyLazyColumnScrollToCenter(expectedIndicatorSize = 0.3f, itemsCount = 40)
    }

    @Test
    fun lazyColumnStateAdapter_longContent_scrolled() {
        verifyLazyColumnScrollToCenter(expectedIndicatorSize = 0.3f, itemsCount = 16)
    }

    @Test
    fun lazyColumnStateAdapter_mediumContent_scrolled() {
        verifyLazyColumnScrollToCenter(expectedIndicatorSize = 0.5f, itemsCount = 6)
    }

    @Test
    fun lazyColumnStateAdapter_shortContent_scrolled() {
        verifyLazyColumnScrollToCenter(expectedIndicatorSize = 0.7f, itemsCount = 4)
    }

    @Test
    fun lazyColumnStateAdapter_mediumContent_reversed() {
        // ScrollIndicators state isn't affected by reverseLayout flag, only its
        // representation - that's why indicatorPosition remains 0.
        verifyLazyColumnPositionAndSize(
            expectedIndicatorPosition = 0f,
            expectedIndicatorSize = 0.5f,
            reverseLayout = true,
            itemsCount = 6
        )
    }

    @Test
    fun lazyColumnStateAdapter_overscroll() {
        val itemsCount = 6
        val contentPadding = itemSizeDp + itemSpacingDp

        lateinit var state: LazyListState
        lateinit var indicatorState: IndicatorState
        rule.setContent {
            state = rememberLazyListState()
            ScreenScaffold(scrollState = state) {
                indicatorState =
                    LazyColumnStateAdapter(
                        state,
                        rememberOverscrollEffect() as OffsetOverscrollEffect,
                        false
                    )
                LazyColumn(
                    state = state,
                    contentPadding = PaddingValues(contentPadding),
                    modifier = Modifier.testTag(TEST_TAG).requiredSize(viewportSizeDp),
                ) {
                    items(itemsCount) {
                        Box(Modifier.requiredSize(itemSizeDp).background(Color.Red)) { Text("$it") }
                    }
                }
            }
        }
        val expectedIndicatorSize =
            indicatorState.sizeFraction - ScrollIndicatorDefaults.overscrollShrinkSizeFraction

        rule.onNodeWithTag(TEST_TAG).performTouchInput {
            down(center)
            moveTo(Offset(center.x, center.y + 2000))
            // We don't lift the finger as otherwise overscroll will be reset
        }
        rule.runOnIdle {
            Truth.assertThat(indicatorState.positionFraction).isWithin(0.05f).of(0f)
            // Size fraction should be fully shrinked - equal to minimum possible size fraction
            Truth.assertThat(indicatorState.sizeFraction).isWithin(0.05f).of(expectedIndicatorSize)
        }
    }

    @Test
    fun transformingLazyColumnStateAdapter_veryLongContent() {
        verifyTransformingLazyColumnPositionAndSize(
            expectedIndicatorPosition = 0f,
            expectedIndicatorSize = 0.3f,
            itemsCount = 40
        )
    }

    @Test
    fun transformingLazyColumnStateAdapter_longContent() {
        verifyTransformingLazyColumnPositionAndSize(
            expectedIndicatorPosition = 0f,
            expectedIndicatorSize = 0.3f,
            itemsCount = 15
        )
    }

    @Test
    fun transformingLazyColumnStateAdapter_mediumContent() {
        verifyTransformingLazyColumnPositionAndSize(
            expectedIndicatorPosition = 0f,
            expectedIndicatorSize = 0.5f,
            itemsCount = 6
        )
    }

    @Test
    fun transformingLazyColumnStateAdapter_mediumContent_withContentPadding() {
        val itemsCount = 6
        val contentPadding = itemSizeDp + itemSpacingDp

        // As TLC is centered at the centre of 0th item, we expect the list to have the top at
        // position 0f, and the bottom at position 2f. To calculate the indicator size we need to
        // divide their difference by the size of the list.
        val expectedIndicatorSize = 2f / itemsCount

        verifyTransformingLazyColumnPositionAndSize(
            expectedIndicatorPosition = {
                // By default TLC is centered around the 0th item, and content padding is equal to
                // exactly 1 item in height. That means that our TLC is at the very top of the list.
                Truth.assertThat(it).isEqualTo(0.0f)
            },
            expectedIndicatorSize = {
                Truth.assertThat(it).isWithin(0.05f).of(expectedIndicatorSize)
            },
            itemsCount = itemsCount,
            contentPaddingDp = contentPadding
        )
    }

    @Test
    fun transformingLazyColumnStateAdapter_overscroll() {
        val itemsCount = 6
        val contentPadding = itemSizeDp + itemSpacingDp

        lateinit var state: TransformingLazyColumnState
        lateinit var indicatorState: IndicatorState
        rule.setContent {
            state = rememberTransformingLazyColumnState()
            ScreenScaffold(scrollState = state) {
                indicatorState =
                    TransformingLazyColumnStateAdapter(
                        state,
                        rememberOverscrollEffect() as OffsetOverscrollEffect,
                        false
                    )
                TransformingLazyColumn(
                    state = state,
                    contentPadding = PaddingValues(contentPadding),
                    modifier = Modifier.testTag(TEST_TAG).requiredSize(viewportSizeDp),
                ) {
                    items(itemsCount) {
                        Box(Modifier.requiredSize(itemSizeDp).background(Color.Red)) { Text("$it") }
                    }
                }
            }
        }
        val expectedIndicatorSize =
            indicatorState.sizeFraction - ScrollIndicatorDefaults.overscrollShrinkSizeFraction

        rule.onNodeWithTag(TEST_TAG).performTouchInput {
            // TODO(b/416503918): Overscroll is not triggered during the first touch. This behavior
            //  was only observed in this test - in real use case it works as expected.
            // Triggering the first touch as a workaround for this issue
            down(center)
            moveTo(Offset(center.x, center.y + 100))
            up()

            down(center)
            moveTo(Offset(center.x, center.y + 2000))
            // We don't lift the finger as otherwise overscroll will be reset
        }
        rule.runOnIdle {
            Truth.assertThat(indicatorState.positionFraction).isWithin(0.05f).of(0f)
            // Size fraction should be fully shrinked - equal to minimum possible size fraction
            Truth.assertThat(indicatorState.sizeFraction).isWithin(0.05f).of(expectedIndicatorSize)
        }
    }

    @Test
    fun columnStateAdapter_veryLongContent() {
        verifyColumnPositionAndSize(
            expectedIndicatorPosition = 0f,
            expectedIndicatorSize = 0.3f,
            itemsCount = 40
        )
    }

    @Test
    fun columnStateAdapter_longContent() {
        verifyColumnPositionAndSize(
            expectedIndicatorPosition = 0f,
            expectedIndicatorSize = 0.3f,
            itemsCount = 15
        )
    }

    @Test
    fun columnStateAdapter_mediumContent() {
        verifyColumnPositionAndSize(
            expectedIndicatorPosition = 0f,
            expectedIndicatorSize = 0.5f,
            itemsCount = 6
        )
    }

    @Test
    fun columnStateAdapter_veryLongContent_scrolled() {
        verifyColumnScrollToCenter(expectedIndicatorSize = 0.3f, itemsCount = 40)
    }

    @Test
    fun columnStateAdapter_longContent_scrolled() {
        verifyColumnScrollToCenter(expectedIndicatorSize = 0.3f, itemsCount = 16)
    }

    @Test
    fun columnStateAdapter_mediumContent_scrolled() {
        verifyColumnScrollToCenter(expectedIndicatorSize = 0.5f, itemsCount = 6)
    }

    @Test
    fun columnStateAdapter_shortContent_scrolled() {
        verifyColumnScrollToCenter(expectedIndicatorSize = 0.7f, itemsCount = 4)
    }

    @Test
    fun columnStateAdapter_mediumContent_reversed() {
        // ScrollIndicators state isn't affected by reverseLayout flag, only its
        // representation - that's why indicatorPosition remains 0.
        verifyColumnPositionAndSize(
            expectedIndicatorPosition = 0f,
            expectedIndicatorSize = 0.5f,
            reverseLayout = true,
            itemsCount = 6
        )
    }

    @Test
    fun columnStateAdapter_overscroll() {
        val itemsCount = 10
        lateinit var state: ScrollState
        lateinit var indicatorState: IndicatorState
        var viewPortSize = IntSize.Zero

        rule.setContent {
            state = rememberScrollState()
            ScreenScaffold(
                modifier =
                    Modifier.onSizeChanged { viewPortSize = it }.requiredSize(viewportSizeDp),
                scrollState = state
            ) {
                indicatorState =
                    ScrollStateAdapter(
                        scrollState = state,
                        overscrollEffect = rememberOverscrollEffect() as OffsetOverscrollEffect,
                        false
                    ) {
                        viewPortSize
                    }

                Column(Modifier.testTag(TEST_TAG).verticalScroll(state = state)) {
                    for (it in 0 until itemsCount) {
                        Box(Modifier.requiredSize(itemSizeDp).background(Color.Red)) { Text("$it") }
                    }
                }
            }
        }
        val expectedIndicatorSize =
            indicatorState.sizeFraction - ScrollIndicatorDefaults.overscrollShrinkSizeFraction

        rule.onNodeWithTag(TEST_TAG).performTouchInput {
            down(center)
            moveTo(Offset(center.x, center.y + 2000))
            // We don't lift the finger as otherwise overscroll will be reset
        }
        rule.runOnIdle {
            Truth.assertThat(indicatorState.positionFraction).isWithin(0.05f).of(0f)
            // Size fraction should be fully shrinked - equal to minimum possible size fraction
            Truth.assertThat(indicatorState.sizeFraction).isWithin(0.05f).of(expectedIndicatorSize)
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun gives_indicator_custom_color() {
        val customIndicatorColor = Color.Red
        rule.setContentWithTheme {
            Box {
                val state = rememberScalingLazyListState()
                ScalingLazyColumn(
                    state = state,
                    contentPadding = PaddingValues(100.dp),
                    autoCentering = null,
                    modifier = Modifier.background(Color.Black)
                ) {
                    items(10) {
                        Text("item $it", modifier = Modifier.height(100.dp), color = Color.Blue)
                    }
                }
                ScrollIndicator(
                    state = state,
                    modifier = Modifier.align(Alignment.CenterEnd).testTag(TEST_TAG),
                    colors = ScrollIndicatorDefaults.colors(indicatorColor = customIndicatorColor)
                )
            }
        }

        rule.onNodeWithTag(TEST_TAG).captureToImage().assertContainsColor(customIndicatorColor)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun gives_track_custom_color() {
        val customTrackColor = Color.Red
        rule.setContentWithTheme {
            Box {
                val state = rememberScalingLazyListState()
                ScalingLazyColumn(
                    state = state,
                    contentPadding = PaddingValues(100.dp),
                    autoCentering = null,
                    modifier = Modifier.background(Color.Black)
                ) {
                    items(10) {
                        Text("item $it", modifier = Modifier.height(100.dp), color = Color.Blue)
                    }
                }
                ScrollIndicator(
                    state = state,
                    modifier = Modifier.align(Alignment.CenterEnd).testTag(TEST_TAG),
                    colors = ScrollIndicatorDefaults.colors(trackColor = customTrackColor)
                )
            }
        }

        rule.onNodeWithTag(TEST_TAG).captureToImage().assertContainsColor(customTrackColor)
    }

    private fun verifySlcScrollToCenter(expectedIndicatorSize: Float, itemsCount: Int) {
        verifySlcPositionAndSize(
            expectedIndicatorPosition = { Truth.assertThat(it).isWithin(0.05f).of(0.5f) },
            expectedIndicatorSize = {
                Truth.assertThat(it).isWithin(0.05f).of(expectedIndicatorSize)
            },
            initialCenterItemIndex = 0,
            // Scrolling by half of the total list height, minus original central position of the
            // list, which is 0.5th item.
            scrollByItems = itemsCount / 2f - 0.5f,
            itemsCount = itemsCount
        )
    }

    private fun verifySlcPositionAndSize(
        expectedIndicatorPosition: Float,
        expectedIndicatorSize: Float,
        reverseLayout: Boolean = false,
        itemsCount: Int = 0,
    ) {
        verifySlcPositionAndSize(
            expectedIndicatorPosition = {
                Truth.assertThat(it).isWithin(0.05f).of(expectedIndicatorPosition)
            },
            expectedIndicatorSize = {
                Truth.assertThat(it).isWithin(0.05f).of(expectedIndicatorSize)
            },
            reverseLayout = reverseLayout,
            itemsCount = itemsCount
        )
    }

    private fun verifySlcPositionAndSize(
        expectedIndicatorPosition: (actual: Float) -> Unit,
        expectedIndicatorSize: (actual: Float) -> Unit,
        verticalArrangement: Arrangement.Vertical =
            Arrangement.spacedBy(space = itemSpacingDp, alignment = Alignment.Bottom),
        reverseLayout: Boolean = false,
        initialCenterItemIndex: Int = 1,
        autoCentering: AutoCenteringParams? =
            AutoCenteringParams(itemIndex = initialCenterItemIndex),
        contentPaddingDp: Dp = 0.dp,
        scrollByItems: Float = 0f,
        itemsCount: Int = 0,
    ) {
        lateinit var state: ScalingLazyListState
        lateinit var indicatorState: IndicatorState
        rule.setContent {
            state = rememberScalingLazyListState(initialCenterItemIndex)
            indicatorState = ScalingLazyColumnStateAdapter(state, null, false)
            ScalingLazyColumn(
                state = state,
                verticalArrangement = verticalArrangement,
                reverseLayout = reverseLayout,
                modifier = Modifier.requiredSize(viewportSizeDp),
                autoCentering = autoCentering,
                contentPadding = PaddingValues(contentPaddingDp)
            ) {
                items(itemsCount) { Box(Modifier.requiredSize(itemSizeDp).background(Color.Red)) }
            }
        }

        rule.runOnIdle {
            if (scrollByItems != 0f) {
                runBlocking {
                    state.scrollBy((itemSizePx.toFloat() + itemSpacingPx.toFloat()) * scrollByItems)
                }
            }
        }

        rule.runOnIdle {
            expectedIndicatorPosition(indicatorState.positionFraction)
            expectedIndicatorSize(indicatorState.sizeFraction)
        }
    }

    private fun verifyLazyColumnScrollToCenter(expectedIndicatorSize: Float, itemsCount: Int) {
        verifyLazyColumnPositionAndSize(
            expectedIndicatorPosition = 0.5f,
            expectedIndicatorSize = expectedIndicatorSize,
            // Scrolling by half of the list height, minus original central position of the list,
            // which is 1.5th item.
            scrollByItems = itemsCount / 2f - 1.5f,
            itemsCount = itemsCount
        )
    }

    private fun verifyLazyColumnPositionAndSize(
        expectedIndicatorPosition: Float,
        expectedIndicatorSize: Float,
        verticalArrangement: Arrangement.Vertical =
            Arrangement.spacedBy(space = itemSpacingDp, alignment = Alignment.Bottom),
        reverseLayout: Boolean = false,
        scrollByItems: Float = 0f,
        itemsCount: Int = 0,
    ) {
        lateinit var state: LazyListState
        lateinit var indicatorState: IndicatorState
        rule.setContent {
            state = rememberLazyListState()
            indicatorState = LazyColumnStateAdapter(state, null, false)
            LazyColumn(
                state = state,
                verticalArrangement = verticalArrangement,
                reverseLayout = reverseLayout,
                modifier = Modifier.requiredSize(viewportSizeDp),
            ) {
                items(itemsCount) {
                    Box(Modifier.requiredSize(itemSizeDp).background(Color.Red)) { Text("$it") }
                }
            }
        }

        rule.runOnIdle {
            if (scrollByItems != 0f) {
                runBlocking {
                    state.scrollBy((itemSizePx.toFloat() + itemSpacingPx.toFloat()) * scrollByItems)
                }
            }
        }

        rule.runOnIdle {
            Truth.assertThat(indicatorState.positionFraction)
                .isWithin(0.05f)
                .of(expectedIndicatorPosition)
            Truth.assertThat(indicatorState.sizeFraction).isWithin(0.05f).of(expectedIndicatorSize)
        }
    }

    private fun verifyColumnScrollToCenter(expectedIndicatorSize: Float, itemsCount: Int) {
        verifyColumnPositionAndSize(
            expectedIndicatorPosition = 0.5f,
            expectedIndicatorSize = expectedIndicatorSize,
            // Scrolling by half of the list height, minus original central position of the list,
            // which is 1.5th item.
            scrollByItems = itemsCount / 2f - 1.5f,
            itemsCount = itemsCount
        )
    }

    private fun verifyColumnPositionAndSize(
        expectedIndicatorPosition: Float,
        expectedIndicatorSize: Float,
        verticalArrangement: Arrangement.Vertical =
            Arrangement.spacedBy(space = itemSpacingDp, alignment = Alignment.Bottom),
        reverseLayout: Boolean = false,
        scrollByItems: Float = 0f,
        itemsCount: Int = 0,
    ) {
        lateinit var state: ScrollState
        lateinit var indicatorState: IndicatorState
        var viewPortSize = IntSize.Zero
        rule.setContent {
            state = rememberScrollState()
            indicatorState = ScrollStateAdapter(state, null, false) { viewPortSize }
            Box(
                modifier = Modifier.onSizeChanged { viewPortSize = it }.requiredSize(viewportSizeDp)
            ) {
                Column(
                    verticalArrangement = verticalArrangement,
                    modifier =
                        Modifier.verticalScroll(state = state, reverseScrolling = reverseLayout)
                ) {
                    for (it in 0 until itemsCount) {
                        Box(Modifier.requiredSize(itemSizeDp).background(Color.Red)) { Text("$it") }
                    }
                }
            }
        }

        rule.runOnIdle {
            if (scrollByItems != 0f) {
                runBlocking {
                    state.scrollBy((itemSizePx.toFloat() + itemSpacingPx.toFloat()) * scrollByItems)
                }
            }
        }
        rule.runOnIdle {
            Truth.assertThat(indicatorState.positionFraction)
                .isWithin(0.05f)
                .of(expectedIndicatorPosition)
            Truth.assertThat(indicatorState.sizeFraction).isWithin(0.05f).of(expectedIndicatorSize)
        }
    }

    private fun verifyTransformingLazyColumnPositionAndSize(
        expectedIndicatorPosition: Float,
        expectedIndicatorSize: Float,
        itemsCount: Int = 0,
    ) {
        verifyTransformingLazyColumnPositionAndSize(
            expectedIndicatorPosition = {
                Truth.assertThat(it).isWithin(0.05f).of(expectedIndicatorPosition)
            },
            expectedIndicatorSize = {
                Truth.assertThat(it).isWithin(0.05f).of(expectedIndicatorSize)
            },
            itemsCount = itemsCount
        )
    }

    private fun verifyTransformingLazyColumnPositionAndSize(
        expectedIndicatorPosition: (actual: Float) -> Unit,
        expectedIndicatorSize: (actual: Float) -> Unit,
        verticalArrangement: Arrangement.Vertical =
            Arrangement.spacedBy(space = itemSpacingDp, alignment = Alignment.Bottom),
        contentPaddingDp: Dp = 0.dp,
        scrollByItems: Float = 0f,
        itemsCount: Int = 0,
    ) {
        lateinit var state: TransformingLazyColumnState
        lateinit var indicatorState: IndicatorState
        rule.setContent {
            state = rememberTransformingLazyColumnState()
            indicatorState = TransformingLazyColumnStateAdapter(state, null, false)
            TransformingLazyColumn(
                state = state,
                contentPadding = PaddingValues(contentPaddingDp),
                verticalArrangement = verticalArrangement,
                modifier = Modifier.requiredSize(viewportSizeDp),
            ) {
                items(itemsCount) {
                    Box(Modifier.requiredSize(itemSizeDp).background(Color.Red)) { Text("$it") }
                }
            }
        }

        rule.runOnIdle {
            if (scrollByItems != 0f) {
                runBlocking {
                    state.scrollBy((itemSizePx.toFloat() + itemSpacingPx.toFloat()) * scrollByItems)
                }
            }
        }

        rule.runOnIdle {
            expectedIndicatorPosition(indicatorState.positionFraction)
            expectedIndicatorSize(indicatorState.sizeFraction)
        }
    }
}
