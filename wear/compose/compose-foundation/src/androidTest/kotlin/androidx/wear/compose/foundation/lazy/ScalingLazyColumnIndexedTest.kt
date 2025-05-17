/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.wear.compose.foundation.lazy

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertTopPositionInRootIsEqualTo
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import org.junit.Rule
import org.junit.Test

class ScalingLazyColumnIndexedTest {

    @get:Rule val rule = createComposeRule()

    @Test
    fun scalingLazyColumnShowsIndexedItems() {
        lateinit var state: ScalingLazyListState
        val items = (1..4).map { it.toString() }
        val viewPortHeight = 100.dp
        val itemHeight = 51.dp
        val itemWidth = 50.dp
        val gapBetweenItems = 2.dp

        rule.setContent {
            ScalingLazyColumn(
                state =
                    rememberScalingLazyListState(initialCenterItemIndex = 0).also { state = it },
                modifier = Modifier.height(viewPortHeight),
                verticalArrangement = Arrangement.spacedBy(gapBetweenItems),
                scalingParams =
                    ScalingLazyColumnDefaults.scalingParams(
                        edgeScale = 1.0f,
                        // Create some extra composables to check that extraPadding works.
                        viewportVerticalOffsetResolver = { (it.maxHeight / 10f).toInt() },
                    ),
            ) {
                itemsIndexed(items) { index, item ->
                    Spacer(Modifier.height(itemHeight).width(itemWidth).testTag("$index-$item"))
                }
            }
        }

        // TODO(b/210654937): Remove the waitUntil once we no longer need 2 stage initialization
        rule.waitUntil { state.initialized.value }
        // Fully visible
        rule.onNodeWithTag("0-1").assertIsDisplayed()

        // Partially visible
        rule.onNodeWithTag("1-2").assertIsDisplayed()

        // Will have been composed but should not be visible
        rule.onNodeWithTag("2-3").assertIsNotDisplayed()

        // Should not have been composed
        rule.onNodeWithTag("3-4").assertDoesNotExist()
    }

    @Test
    fun columnWithIndexesComposedWithCorrectIndexAndItem() {
        lateinit var state: ScalingLazyListState
        val items = (0..1).map { it.toString() }

        rule.setContent {
            ScalingLazyColumn(
                state =
                    rememberScalingLazyListState(initialCenterItemIndex = 0).also { state = it },
                modifier = Modifier.height(200.dp),
                autoCentering = null,
                scalingParams = ScalingLazyColumnDefaults.scalingParams(edgeScale = 1.0f),
            ) {
                itemsIndexed(items) { index, item ->
                    BasicText("${index}x$item", Modifier.requiredHeight(100.dp))
                }
            }
        }

        // TODO(b/210654937): Remove the waitUntil once we no longer need 2 stage initialization
        rule.waitUntil { state.initialized.value }
        rule.onNodeWithText("0x0").assertTopPositionInRootIsEqualTo(0.dp)

        rule.onNodeWithText("1x1").assertTopPositionInRootIsEqualTo(104.dp)
    }

    @Test
    fun columnWithIndexesComposedWithCorrectIndexAndItemWithAutoCentering() {
        lateinit var state: ScalingLazyListState
        val items = (0..1).map { it.toString() }
        val viewPortHeight = 100.dp
        val itemHeight = 50.dp
        val gapBetweenItems = 2.dp
        rule.setContent {
            ScalingLazyColumn(
                state =
                    rememberScalingLazyListState(initialCenterItemIndex = 0).also { state = it },
                modifier = Modifier.height(viewPortHeight),
                autoCentering = AutoCenteringParams(itemIndex = 0),
                verticalArrangement = Arrangement.spacedBy(gapBetweenItems),
                // No scaling as we are doing maths with expected item sizes
                scalingParams = ScalingLazyColumnDefaults.scalingParams(edgeScale = 1.0f),
            ) {
                itemsIndexed(items) { index, item ->
                    BasicText("${index}x$item", Modifier.requiredHeight(itemHeight))
                }
            }
        }

        // TODO(b/210654937): Remove the waitUntil once we no longer need 2 stage initialization
        rule.waitUntil { state.initialized.value }
        // Check that first item is in the center of the viewport
        val firstItemStart = viewPortHeight / 2f - itemHeight / 2f
        rule.onNodeWithText("0x0").assertTopPositionInRootIsEqualTo(firstItemStart)

        // And that the second item is item height + gap between items below it
        rule
            .onNodeWithText("1x1")
            .assertTopPositionInRootIsEqualTo(firstItemStart + itemHeight + gapBetweenItems)
    }
}
