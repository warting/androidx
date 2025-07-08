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
package androidx.compose.material3

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.testutils.assertAgainstGolden
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.test.screenshot.AndroidXScreenshotTestRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 35, maxSdkVersion = 35)
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
class ShapesScreenshotTest {

    @get:Rule val rule = createComposeRule()

    @get:Rule val screenshotRule = AndroidXScreenshotTestRule(GOLDEN_MATERIAL3)

    @Test
    fun extraSmallShape() {
        rule.setMaterialContent(lightColorScheme()) { TestCard(MaterialTheme.shapes.extraSmall) }
        assertAgainstGolden("shapes_extraSmall")
    }

    @Test
    fun smallShape() {
        rule.setMaterialContent(lightColorScheme()) { TestCard(MaterialTheme.shapes.small) }
        assertAgainstGolden("shapes_small")
    }

    @Test
    fun mediumShape() {
        rule.setMaterialContent(lightColorScheme()) { TestCard(MaterialTheme.shapes.small) }
        assertAgainstGolden("shapes_medium")
    }

    @Test
    fun largeShape() {
        rule.setMaterialContent(lightColorScheme()) { TestCard(MaterialTheme.shapes.large) }
        assertAgainstGolden("shapes_large")
    }

    @Test
    fun largeIncreasedShape() {
        rule.setMaterialContent(lightColorScheme()) {
            TestCard(MaterialTheme.shapes.largeIncreased)
        }
        assertAgainstGolden("shapes_largeIncreased")
    }

    @Test
    fun extraLargeShape() {
        rule.setMaterialContent(lightColorScheme()) { TestCard(MaterialTheme.shapes.extraLarge) }
        assertAgainstGolden("shapes_extraLarge")
    }

    @Test
    fun extraLargeIncreasedShape() {
        rule.setMaterialContent(lightColorScheme()) {
            TestCard(MaterialTheme.shapes.extraLargeIncreased)
        }
        assertAgainstGolden("shapes_extraLargeIncreased")
    }

    @Test
    fun extraExtraLargeShape() {
        rule.setMaterialContent(lightColorScheme()) {
            TestCard(MaterialTheme.shapes.extraExtraLarge)
        }
        assertAgainstGolden("shapes_extraExtraLarge")
    }

    private fun assertAgainstGolden(goldenName: String) {
        rule.onNodeWithTag(Tag).captureToImage().assertAgainstGolden(screenshotRule, goldenName)
    }

    private val Tag = "Shapes"

    @Composable
    private fun TestCard(shape: Shape) =
        Card(modifier = Modifier.testTag(Tag), shape = shape) { Box(Modifier.size(200.dp)) }
}
