/*
 * Copyright 2021 The Android Open Source Project
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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.testutils.assertIsEqualTo
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertLeftPositionInRootIsEqualTo
import androidx.compose.ui.test.assertTopPositionInRootIsEqualTo
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onParent
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
/** Test for [NavigationRail] and [NavigationRailItem]. */
class NavigationRailTest {
    @get:Rule val rule = createComposeRule()

    @Test
    fun defaultSemantics() {
        rule.setMaterialContent(lightColorScheme()) {
            NavigationRail {
                NavigationRailItem(
                    modifier = Modifier.testTag("item"),
                    icon = { Icon(Icons.Filled.Favorite, null) },
                    label = { Text("ItemText") },
                    selected = true,
                    onClick = {}
                )
            }
        }

        rule
            .onNodeWithTag("item")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab))
            .assertIsSelected()
            .assertIsEnabled()
            .assertHasClickAction()

        rule
            .onNodeWithTag("item")
            .onParent()
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.SelectableGroup))
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.IsTraversalGroup))
    }

    @Test
    fun disabledSemantics() {
        rule.setMaterialContent(lightColorScheme()) {
            NavigationRail {
                NavigationRailItem(
                    enabled = false,
                    modifier = Modifier.testTag("item"),
                    icon = { Icon(Icons.Filled.Favorite, null) },
                    label = { Text("ItemText") },
                    selected = true,
                    onClick = {}
                )
            }
        }

        rule
            .onNodeWithTag("item")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab))
            .assertIsSelected()
            .assertIsNotEnabled()
            .assertHasClickAction()
    }

    @Test
    fun navigationRailItem_clearsIconSemantics_whenLabelIsPresent() {
        rule.setMaterialContent(lightColorScheme()) {
            NavigationRail {
                NavigationRailItem(
                    modifier = Modifier.testTag("item1"),
                    icon = { Icon(Icons.Filled.Favorite, "Favorite") },
                    label = { Text("Favorite") },
                    selected = true,
                    alwaysShowLabel = false,
                    onClick = {}
                )
                NavigationRailItem(
                    modifier = Modifier.testTag("item2"),
                    icon = { Icon(Icons.Filled.Favorite, "Favorite") },
                    label = { Text("Favorite") },
                    selected = false,
                    alwaysShowLabel = false,
                    onClick = {}
                )
                NavigationRailItem(
                    modifier = Modifier.testTag("item3"),
                    icon = { Icon(Icons.Filled.Favorite, "Favorite") },
                    selected = false,
                    onClick = {}
                )
            }
        }

        val node1 = rule.onNodeWithTag("item1").fetchSemanticsNode()
        val node2 = rule.onNodeWithTag("item2").fetchSemanticsNode()
        val node3 = rule.onNodeWithTag("item3").fetchSemanticsNode()

        Truth.assertThat(node1.config.getOrNull(SemanticsProperties.ContentDescription)).isNull()
        Truth.assertThat(node2.config.getOrNull(SemanticsProperties.ContentDescription))
            .isEqualTo(listOf("Favorite"))
        Truth.assertThat(node3.config.getOrNull(SemanticsProperties.ContentDescription))
            .isEqualTo(listOf("Favorite"))
    }

    @Test
    fun navigationRail_width() {
        val defaultWidth = NavigationRailItemWidth
        rule
            .setMaterialContentForSizeAssertions {
                val items = listOf("Home", "Search", "Settings")
                val icons = listOf(Icons.Filled.Home, Icons.Filled.Search, Icons.Filled.Settings)
                NavigationRail {
                    items.forEachIndexed { index, item ->
                        NavigationRailItem(
                            icon = { Icon(icons[index], contentDescription = item) },
                            label = { Text(item) },
                            selected = index == 0,
                            onClick = { /* do something */ }
                        )
                    }
                }
            }
            .assertWidthIsEqualTo(defaultWidth)
    }

    @Test
    fun navigationRail_respectsContentPadding() {
        rule.setMaterialContentForSizeAssertions {
            NavigationRail(windowInsets = WindowInsets(13.dp, 13.dp, 13.dp, 13.dp)) {
                Box(Modifier.fillMaxSize().testTag("content"))
            }
        }
        rule
            .onNodeWithTag("content")
            .assertTopPositionInRootIsEqualTo(13.dp + NavigationRailVerticalPadding)
            .assertLeftPositionInRootIsEqualTo(13.dp)
    }

    @Test
    fun navigationRailItem_sizeAndPositions() {
        val itemCoords = mutableMapOf<Int, LayoutCoordinates>()
        rule.setMaterialContent(lightColorScheme()) {
            Box {
                NavigationRail {
                    repeat(4) { index ->
                        NavigationRailItem(
                            icon = { Icon(Icons.Filled.Favorite, null) },
                            label = { Text("Item $index") },
                            selected = index == 0,
                            onClick = {},
                            modifier =
                                Modifier.onGloballyPositioned { coords ->
                                    itemCoords[index] = coords
                                }
                        )
                    }
                }
            }
        }

        rule.runOnIdleWithDensity {
            val expectedItemWidthPx = NavigationRailItemWidth.roundToPx()
            val expectedItemHeightPx = NavigationRailItemHeight.roundToPx()
            val navigationRailPadding = NavigationRailItemVerticalPadding.roundToPx()

            itemCoords.forEach { (index, coord) ->
                Truth.assertThat(coord.size.width).isEqualTo(expectedItemWidthPx)
                Truth.assertThat(coord.size.height).isEqualTo(expectedItemHeightPx)
                // Height of all items + paddings above (index 0 has padding as well).
                val expectedY = expectedItemHeightPx * index + navigationRailPadding * (index + 1)
                Truth.assertThat(coord.positionInWindow().y).isEqualTo(expectedY.toFloat())
            }
        }
    }

    @Test
    fun navigationRailItem_withLongLabel_automaticallyResizesHeight() {
        val defaultHeight = NavigationRailItemHeight

        rule.setMaterialContent(lightColorScheme()) {
            NavigationRail {
                NavigationRailItem(
                    icon = { Icon(Icons.Filled.Favorite, null) },
                    label = { Text("Long\nLabel\nMultiple\nLines") },
                    selected = true,
                    onClick = {},
                    modifier = Modifier.testTag("TAG"),
                )
            }
        }

        Truth.assertThat(
                rule.onNodeWithTag("TAG", useUnmergedTree = true).getUnclippedBoundsInRoot().height
            )
            .isGreaterThan(defaultHeight)
    }

    @Test
    fun navigationRailItemContent_withLabel_sizeAndPosition() {
        rule.setMaterialContent(lightColorScheme()) {
            Box {
                NavigationRail {
                    NavigationRailItem(
                        modifier = Modifier.testTag("item"),
                        icon = { Icon(Icons.Filled.Favorite, null, Modifier.testTag("icon")) },
                        label = { Text("ItemText", Modifier.testTag("label")) },
                        selected = true,
                        onClick = {}
                    )
                }
            }
        }

        val itemBounds = rule.onNodeWithTag("item").getUnclippedBoundsInRoot()
        val iconBounds =
            rule.onNodeWithTag("icon", useUnmergedTree = true).getUnclippedBoundsInRoot()
        val textBounds =
            rule.onNodeWithTag("label", useUnmergedTree = true).getUnclippedBoundsInRoot()

        // Distance from the container bottom to the text bottom, and from the top of the icon to
        // the top of the item
        val verticalPadding = NavigationRailItemVerticalPadding

        val textBottom = textBounds.bottom

        val itemBottom = itemBounds.height + itemBounds.top
        // Text bottom should be `verticalPadding` from the bottom of the item
        textBottom.assertIsEqualTo(itemBottom - verticalPadding)

        rule
            .onNodeWithTag("icon", useUnmergedTree = true)
            // The icon should be centered in the item
            .assertLeftPositionInRootIsEqualTo((itemBounds.width - iconBounds.width) / 2)
            // The top of the icon is `verticalPadding` below the top of the item
            .assertTopPositionInRootIsEqualTo(itemBounds.top + verticalPadding)
    }

    @Test
    fun navigationRailItemContent_withLabel_unselected_sizeAndPosition() {
        rule.setMaterialContent(lightColorScheme()) {
            Box {
                NavigationRail {
                    NavigationRailItem(
                        modifier = Modifier.testTag("item"),
                        icon = { Icon(Icons.Filled.Favorite, null, Modifier.testTag("icon")) },
                        label = { Text("ItemText") },
                        selected = false,
                        onClick = {},
                        alwaysShowLabel = false
                    )
                }
            }
        }

        // The text should not be placed, since the item is not selected and alwaysShowLabels
        // is false
        rule.onNodeWithText("ItemText", useUnmergedTree = true).assertIsNotDisplayed()

        val itemBounds = rule.onNodeWithTag("item").getUnclippedBoundsInRoot()
        val iconBounds =
            rule.onNodeWithTag("icon", useUnmergedTree = true).getUnclippedBoundsInRoot()

        rule
            .onNodeWithTag("icon", useUnmergedTree = true)
            .assertLeftPositionInRootIsEqualTo((itemBounds.width - iconBounds.width) / 2)
            // The item's container has a `NavigationRailItemVerticalPadding` top-padding
            .assertTopPositionInRootIsEqualTo(
                NavigationRailItemVerticalPadding + (itemBounds.height - iconBounds.height) / 2
            )
    }

    @Test
    fun navigationRailItemContent_withoutLabel_sizeAndPosition() {
        rule.setMaterialContent(lightColorScheme()) {
            Box {
                NavigationRail {
                    NavigationRailItem(
                        modifier = Modifier.testTag("item"),
                        icon = { Icon(Icons.Filled.Favorite, null, Modifier.testTag("icon")) },
                        label = null,
                        selected = false,
                        onClick = {}
                    )
                }
            }
        }

        val itemBounds = rule.onNodeWithTag("item").getUnclippedBoundsInRoot()
        val iconBounds =
            rule.onNodeWithTag("icon", useUnmergedTree = true).getUnclippedBoundsInRoot()

        // The icon should be centered in the item, as there is no text placeable provided
        rule
            .onNodeWithTag("icon", useUnmergedTree = true)
            .assertLeftPositionInRootIsEqualTo((itemBounds.width - iconBounds.width) / 2)
            // The item's container has a `NavigationRailItemVerticalPadding` top-padding
            .assertTopPositionInRootIsEqualTo(
                NavigationRailItemVerticalPadding + (itemBounds.height - iconBounds.height) / 2
            )
    }

    @Test
    fun navigationRail_selectNewItem() {
        rule.setMaterialContent(lightColorScheme()) {
            var selectedItem by remember { mutableStateOf(0) }
            val items = listOf("Home", "Search", "Settings")
            val icons = listOf(Icons.Filled.Home, Icons.Filled.Search, Icons.Filled.Settings)
            NavigationRail {
                items.forEachIndexed { index, item ->
                    NavigationRailItem(
                        icon = { Icon(icons[index], contentDescription = item) },
                        label = { Text(item) },
                        selected = selectedItem == index,
                        onClick = { selectedItem = index }
                    )
                }
            }
        }

        // Find all items and ensure there are 3
        rule
            .onAllNodes(isSelectable())
            .assertCountEquals(3)
            // Ensure semantics match for selected state of the items
            .apply {
                get(0).assertIsSelected()
                get(1).assertIsNotSelected()
                get(2).assertIsNotSelected()
            }
            // Click the last item
            .apply { get(2).performClick() }
            .apply {
                get(0).assertIsNotSelected()
                get(1).assertIsNotSelected()
                get(2).assertIsSelected()
            }
    }

    @Test
    fun disabled_noClicks() {
        var clicks = 0
        rule.setMaterialContent(lightColorScheme()) {
            NavigationRail {
                NavigationRailItem(
                    enabled = false,
                    modifier = Modifier.testTag("item"),
                    icon = { Icon(Icons.Filled.Favorite, null) },
                    label = { Text("ItemText") },
                    selected = true,
                    onClick = { clicks++ }
                )
            }
        }

        rule.onNodeWithTag("item").performClick()

        rule.runOnIdle { Truth.assertThat(clicks).isEqualTo(0) }
    }
}
