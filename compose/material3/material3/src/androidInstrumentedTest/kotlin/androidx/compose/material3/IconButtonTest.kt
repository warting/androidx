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

import android.os.Build
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.tokens.FilledIconButtonTokens
import androidx.compose.material3.tokens.FilledTonalIconButtonTokens
import androidx.compose.material3.tokens.OutlinedIconButtonTokens
import androidx.compose.material3.tokens.SmallIconButtonTokens
import androidx.compose.material3.tokens.StandardIconButtonTokens
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.testutils.assertShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertLeftPositionInRootIsEqualTo
import androidx.compose.ui.test.assertTopPositionInRootIsEqualTo
import androidx.compose.ui.test.assertTouchHeightIsEqualTo
import androidx.compose.ui.test.assertTouchWidthIsEqualTo
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@LargeTest
@RunWith(AndroidJUnit4::class)
/** Tests for icon buttons. */
class IconButtonTest {
    @get:Rule val rule = createComposeRule()

    @Test
    fun iconButton_xsmall_visualBounds() {
        val expectedWidth =
            with(rule.density) { IconButtonDefaults.extraSmallContainerSize().width.roundToPx() }
        val expectedHeight =
            with(rule.density) { IconButtonDefaults.extraSmallContainerSize().height.roundToPx() }
        val expectedSize = IntSize(expectedWidth, expectedHeight)

        assertVisualBounds(
            {
                IconButton(
                    onClick = { /* doSomething() */ },
                    modifier =
                        Modifier.minimumInteractiveComponentSize()
                            .size(IconButtonDefaults.extraSmallContainerSize())
                            .testTag(IconButtonTestTag),
                    shape = IconButtonDefaults.smallRoundShape,
                ) {
                    Icon(Icons.Filled.Favorite, contentDescription = "Localized description")
                }
            },
            expectedSize,
        )
    }

    @Test
    fun iconButton_xSmall_semantic_bounds() {
        rule
            .setMaterialContentForSizeAssertions {
                IconButton(
                    onClick = { /* doSomething() */ },
                    modifier =
                        Modifier.minimumInteractiveComponentSize()
                            .size(IconButtonDefaults.extraSmallContainerSize()),
                ) {
                    Icon(Icons.Filled.Favorite, contentDescription = "Localized description")
                }
            }
            .assertTouchWidthIsEqualTo(IconButtonAccessibilitySize)
            .assertTouchHeightIsEqualTo(IconButtonAccessibilitySize)
    }

    @Test
    fun iconButton_small_visualBounds() {
        val expectedWidth =
            with(rule.density) { IconButtonDefaults.smallContainerSize().width.roundToPx() }
        val expectedHeight =
            with(rule.density) { IconButtonDefaults.smallContainerSize().height.roundToPx() }
        val expectedSize = IntSize(expectedWidth, expectedHeight)

        val size = IconButtonDefaults.smallContainerSize()

        assertVisualBounds(
            {
                IconButton(
                    onClick = { /* doSomething() */ },
                    modifier =
                        Modifier.minimumInteractiveComponentSize()
                            .size(size)
                            .testTag(IconButtonTestTag),
                    shape = IconButtonDefaults.smallRoundShape,
                ) {
                    Icon(Icons.Filled.Favorite, contentDescription = "Localized description")
                }
            },
            expectedSize,
        )
    }

    @Test
    fun iconButton_small_semantic_bounds() {
        rule
            .setMaterialContentForSizeAssertions {
                IconButton(
                    onClick = { /* doSomething() */ },
                    modifier =
                        Modifier.minimumInteractiveComponentSize()
                            .size(IconButtonDefaults.smallContainerSize()),
                ) {
                    Icon(Icons.Filled.Favorite, contentDescription = "Localized description")
                }
            }
            .assertTouchWidthIsEqualTo(IconButtonAccessibilitySize)
            .assertTouchHeightIsEqualTo(IconButtonAccessibilitySize)
    }

    @Test
    fun iconButton_medium_size() {
        rule
            .setMaterialContentForSizeAssertions {
                IconButton(onClick = { /* doSomething() */ }) {
                    Icon(Icons.Filled.Favorite, contentDescription = "Localized description")
                }
            }
            .assertWidthIsEqualTo(IconButtonAccessibilitySize)
            .assertHeightIsEqualTo(IconButtonAccessibilitySize)
            .assertTouchWidthIsEqualTo(IconButtonAccessibilitySize)
            .assertTouchHeightIsEqualTo(IconButtonAccessibilitySize)
    }

    @Test
    fun iconButton_large_size() {
        var size = DpSize.Zero
        rule
            .setMaterialContentForSizeAssertions {
                size = IconButtonDefaults.largeContainerSize()
                IconButton(onClick = { /* doSomething() */ }, modifier = Modifier.size(size)) {
                    Icon(Icons.Filled.Favorite, contentDescription = "Localized description")
                }
            }
            .assertWidthIsEqualTo(size.width)
            .assertHeightIsEqualTo(size.height)
            .assertTouchWidthIsEqualTo(size.width)
            .assertTouchHeightIsEqualTo(size.height)
    }

    @Test
    fun iconButton_xlarge_size() {
        var size = DpSize.Zero
        rule
            .setMaterialContentForSizeAssertions {
                size = IconButtonDefaults.extraLargeContainerSize()
                IconButton(onClick = { /* doSomething() */ }, modifier = Modifier.size(size)) {
                    Icon(Icons.Filled.Favorite, contentDescription = "Localized description")
                }
            }
            .assertWidthIsEqualTo(size.width)
            .assertHeightIsEqualTo(size.height)
            .assertTouchWidthIsEqualTo(size.width)
            .assertTouchHeightIsEqualTo(size.height)
    }

    @Test
    fun iconButton_sizeWithoutMinTargetEnforcement() {
        rule
            .setMaterialContentForSizeAssertions {
                CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
                    IconButton(onClick = { /* doSomething() */ }) {
                        Icon(Icons.Filled.Favorite, contentDescription = "Localized description")
                    }
                }
            }
            .assertWidthIsEqualTo(IconButtonSize)
            .assertHeightIsEqualTo(IconButtonSize)
            .assertTouchWidthIsEqualTo(IconButtonSize)
            .assertTouchHeightIsEqualTo(IconButtonSize)
    }

    @Test
    fun iconButton_sizeWithCustomMinInteractiveComponentSize() {
        val customTouchTargetSize = 44.dp
        rule
            .setMaterialContentForSizeAssertions {
                CompositionLocalProvider(
                    LocalMinimumInteractiveComponentSize provides customTouchTargetSize
                ) {
                    IconButton(onClick = { /* doSomething() */ }) {
                        Icon(
                            Icons.Outlined.FavoriteBorder,
                            contentDescription = "Localized description",
                        )
                    }
                }
            }
            .assertWidthIsEqualTo(customTouchTargetSize)
            .assertHeightIsEqualTo(customTouchTargetSize)
            .assertTouchWidthIsEqualTo(customTouchTargetSize)
            .assertTouchHeightIsEqualTo(customTouchTargetSize)
    }

    @Test
    fun iconButton_defaultSemantics() {
        rule.setMaterialContent(lightColorScheme()) {
            IconButton(onClick = { /* doSomething() */ }) {
                Icon(Icons.Filled.Favorite, contentDescription = "Localized description")
            }
        }
        rule.onNode(hasClickAction()).apply {
            assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            assertIsEnabled()
        }
    }

    @Test
    fun iconButton_disabledSemantics() {
        rule.setMaterialContent(lightColorScheme()) { IconButton(onClick = {}, enabled = false) {} }
        rule.onNode(hasClickAction()).apply {
            assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            assertIsNotEnabled()
        }
    }

    @Test
    fun iconButton_materialIconSize_iconPositioning() {
        rule.setMaterialContent(lightColorScheme()) {
            Box { IconButton(onClick = {}) { Box(Modifier.size(IconSize).testTag(IconTestTag)) } }
        }

        // Icon should be centered inside the IconButton
        rule
            .onNodeWithTag(IconTestTag, useUnmergedTree = true)
            .assertLeftPositionInRootIsEqualTo((IconButtonAccessibilitySize - IconSize) / 2)
            .assertTopPositionInRootIsEqualTo((IconButtonAccessibilitySize - IconSize) / 2)
    }

    @Test
    fun iconButton_customIconSize_iconPositioning() {
        val width = 36.dp
        val height = 14.dp
        rule.setMaterialContent(lightColorScheme()) {
            Box {
                IconButton(onClick = {}) { Box(Modifier.size(width, height).testTag(IconTestTag)) }
            }
        }

        // Icon should be centered inside the IconButton
        rule
            .onNodeWithTag(IconTestTag, useUnmergedTree = true)
            .assertLeftPositionInRootIsEqualTo((IconButtonAccessibilitySize - width) / 2)
            .assertTopPositionInRootIsEqualTo((IconButtonAccessibilitySize - height) / 2)
    }

    @Test
    fun iconButton_defaultLocalContentColors() {
        rule.setMaterialContent(lightColorScheme()) {
            CompositionLocalProvider(LocalContentColor provides Color.Blue) {
                Truth.assertThat(IconButtonDefaults.iconButtonColors())
                    .isEqualTo(
                        IconButtonColors(
                            containerColor = Color.Transparent,
                            contentColor = LocalContentColor.current,
                            disabledContainerColor = Color.Transparent,
                            disabledContentColor =
                                Color.Blue.copy(alpha = StandardIconButtonTokens.DisabledOpacity),
                        )
                    )
            }
        }
    }

    @Test
    fun iconButton_defaultVibrantColors() {
        rule.setMaterialContent(lightColorScheme()) {
            Truth.assertThat(IconButtonDefaults.iconButtonVibrantColors())
                .isEqualTo(
                    IconButtonColors(
                        containerColor = Color.Transparent,
                        contentColor = StandardIconButtonTokens.Color.value,
                        disabledContainerColor = Color.Transparent,
                        disabledContentColor =
                            StandardIconButtonTokens.DisabledColor.value.copy(
                                alpha = StandardIconButtonTokens.DisabledOpacity
                            ),
                    )
                )
        }
    }

    @Test
    fun iconButtonColors_useLocalContentColor() {
        rule.setMaterialContent(lightColorScheme()) {
            CompositionLocalProvider(LocalContentColor provides Color.Blue) {
                val colors = IconButtonDefaults.iconButtonColors()
                assert(colors.contentColor == Color.Blue)
            }

            CompositionLocalProvider(LocalContentColor provides Color.Red) {
                val colors =
                    IconButtonDefaults.iconButtonColors().copy(containerColor = Color.Green)
                assert(colors.containerColor == Color.Green)
                assert(colors.contentColor == Color.Red)
                assert(
                    colors.disabledContentColor ==
                        Color.Red.copy(StandardIconButtonTokens.DisabledOpacity)
                )
            }
        }
    }

    @Test
    fun iconButtonVibrantColors_ignoreLocalContentColor() {
        rule.setMaterialContent(lightColorScheme()) {
            CompositionLocalProvider(LocalContentColor provides Color.Red) {
                val colors =
                    IconButtonDefaults.iconButtonVibrantColors(
                        containerColor = Color.Blue,
                        contentColor = Color.Green,
                    )
                assert(colors.containerColor == Color.Blue)
                assert(colors.contentColor == Color.Green)
                assert(
                    colors.disabledContentColor ==
                        Color.Green.copy(StandardIconButtonTokens.DisabledOpacity)
                )
            }
        }
    }

    @Test
    fun iconButtonColors_customValues() {
        rule.setMaterialContent(lightColorScheme()) {
            CompositionLocalProvider(LocalContentColor provides Color.Red) {
                val colors =
                    IconButtonDefaults.iconButtonVibrantColors(
                        containerColor = Color.Blue,
                        contentColor = Color.Green,
                    )
                assert(colors.containerColor == Color.Blue)
                assert(colors.contentColor == Color.Green)
                assert(
                    colors.disabledContentColor ==
                        Color.Green.copy(StandardIconButtonTokens.DisabledOpacity)
                )
            }
        }
    }

    @Test
    fun iconButtonColors_copy() {
        rule.setMaterialContent(lightColorScheme()) {
            val colors = IconButtonDefaults.iconButtonVibrantColors().copy()
            assert(colors == IconButtonDefaults.iconButtonVibrantColors())
        }
    }

    @Test
    fun iconToggleButton_size() {
        rule
            .setMaterialContentForSizeAssertions {
                IconToggleButton(checked = true, onCheckedChange = { /* doSomething() */ }) {
                    Icon(Icons.Filled.Favorite, contentDescription = "Localized description")
                }
            }
            .assertWidthIsEqualTo(IconButtonAccessibilitySize)
            .assertHeightIsEqualTo(IconButtonAccessibilitySize)
            .assertTouchWidthIsEqualTo(IconButtonAccessibilitySize)
            .assertTouchHeightIsEqualTo(IconButtonAccessibilitySize)
    }

    @Test
    fun iconToggleButton_sizeWithoutMinTargetEnforcement() {
        rule
            .setMaterialContentForSizeAssertions {
                CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
                    IconToggleButton(checked = true, onCheckedChange = { /* doSomething() */ }) {
                        Icon(Icons.Filled.Favorite, contentDescription = "Localized description")
                    }
                }
            }
            .assertWidthIsEqualTo(IconButtonSize)
            .assertHeightIsEqualTo(IconButtonSize)
            .assertTouchWidthIsEqualTo(IconButtonSize)
            .assertTouchHeightIsEqualTo(IconButtonSize)
    }

    @Test
    fun iconToggleButton_defaultSemantics() {
        rule.setMaterialContent(lightColorScheme()) {
            var checked by remember { mutableStateOf(false) }
            IconToggleButton(checked = checked, onCheckedChange = { checked = it }) {
                Icon(Icons.Filled.Favorite, contentDescription = "Localized description")
            }
        }
        rule.onNode(isToggleable()).apply {
            assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Checkbox))
            assertIsEnabled()
            assertIsOff()
            performClick()
            assertIsOn()
        }
    }

    @Test
    fun iconToggleButton_disabledSemantics() {
        rule.setMaterialContent(lightColorScheme()) {
            IconToggleButton(checked = false, onCheckedChange = {}, enabled = false) {}
        }
        rule.onNode(isToggleable()).apply {
            assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Checkbox))
            assertIsNotEnabled()
            assertIsOff()
        }
    }

    @Test
    fun iconToggleButton_materialIconSize_iconPositioning() {
        rule.setMaterialContent(lightColorScheme()) {
            Box {
                IconToggleButton(checked = false, onCheckedChange = {}) {
                    Box(Modifier.size(IconSize).testTag(IconTestTag))
                }
            }
        }

        // Icon should be centered inside the IconToggleButton
        rule
            .onNodeWithTag(IconTestTag, useUnmergedTree = true)
            .assertLeftPositionInRootIsEqualTo((IconButtonAccessibilitySize - IconSize) / 2)
            .assertTopPositionInRootIsEqualTo((IconButtonAccessibilitySize - IconSize) / 2)
    }

    @Test
    fun iconToggleButton_customIconSize_iconPositioning() {
        val width = 36.dp
        val height = 14.dp
        rule.setMaterialContent(lightColorScheme()) {
            Box {
                IconToggleButton(checked = false, onCheckedChange = {}) {
                    Box(Modifier.size(width, height).testTag(IconTestTag))
                }
            }
        }

        // Icon should be centered inside the IconToggleButton
        rule
            .onNodeWithTag(IconTestTag, useUnmergedTree = true)
            .assertLeftPositionInRootIsEqualTo((IconButtonAccessibilitySize - width) / 2)
            .assertTopPositionInRootIsEqualTo((IconButtonAccessibilitySize - height) / 2)
    }

    @Test
    fun iconToggleButton_clickInMinimumTouchTarget(): Unit =
        with(rule.density) {
            var checked by mutableStateOf(false)
            rule.setMaterialContent(lightColorScheme()) {
                // Box is needed because otherwise the control will be expanded to fill its parent
                Box(Modifier.fillMaxSize()) {
                    IconToggleButton(
                        checked = checked,
                        onCheckedChange = { checked = it },
                        modifier =
                            Modifier.align(Alignment.Center)
                                .requiredSize(2.dp)
                                .testTag(IconButtonTestTag),
                    ) {
                        Box(Modifier.size(2.dp))
                    }
                }
            }
            rule
                .onNodeWithTag(IconButtonTestTag)
                .assertIsOff()
                .assertWidthIsEqualTo(2.dp)
                .assertHeightIsEqualTo(2.dp)
                .assertTouchWidthIsEqualTo(48.dp)
                .assertTouchHeightIsEqualTo(48.dp)
                .performTouchInput { click(position = Offset(-1f, -1f)) }
                .assertIsOn()
        }

    @Test
    fun iconToggleButton_defaultLocalContentColors() {
        rule.setMaterialContent(lightColorScheme()) {
            val localContentColor = LocalContentColor.current
            Truth.assertThat(IconButtonDefaults.iconToggleButtonColors())
                .isEqualTo(
                    IconToggleButtonColors(
                        containerColor = Color.Transparent,
                        contentColor = localContentColor,
                        disabledContainerColor = Color.Transparent,
                        disabledContentColor =
                            localContentColor.copy(
                                alpha = StandardIconButtonTokens.DisabledOpacity
                            ),
                        checkedContainerColor = Color.Transparent,
                        checkedContentColor = StandardIconButtonTokens.SelectedColor.value,
                    )
                )
        }
    }

    @Test
    fun iconToggleButton_defaultVibrantColors() {
        rule.setMaterialContent(lightColorScheme()) {
            Truth.assertThat(IconButtonDefaults.iconToggleButtonVibrantColors())
                .isEqualTo(
                    IconToggleButtonColors(
                        containerColor = Color.Transparent,
                        contentColor = StandardIconButtonTokens.UnselectedColor.value,
                        disabledContainerColor = Color.Transparent,
                        disabledContentColor =
                            StandardIconButtonTokens.DisabledColor.value.copy(
                                alpha = StandardIconButtonTokens.DisabledOpacity
                            ),
                        checkedContainerColor = Color.Transparent,
                        checkedContentColor = StandardIconButtonTokens.SelectedColor.value,
                    )
                )
        }
    }

    @Test
    fun filledIconButton_xsmall_visualBounds() {
        val expectedWidth =
            with(rule.density) { IconButtonDefaults.extraSmallContainerSize().width.roundToPx() }
        val expectedHeight =
            with(rule.density) { IconButtonDefaults.extraSmallContainerSize().height.roundToPx() }
        val expectedSize = IntSize(expectedWidth, expectedHeight)

        // The bounds of a testTag on a box that contains the progress indicator are not affected
        // by the padding added on the layout of the progress bar.
        assertVisualBounds(
            {
                val size = IconButtonDefaults.extraSmallContainerSize()
                FilledIconButton(
                    onClick = { /* doSomething() */ },
                    modifier =
                        Modifier.minimumInteractiveComponentSize()
                            .size(size)
                            .testTag(IconButtonTestTag),
                    shape = IconButtonDefaults.extraSmallRoundShape,
                ) {
                    Icon(Icons.Filled.Favorite, contentDescription = "Localized description")
                }
            },
            expectedSize,
        )
    }

    @Test
    fun filledIconButton_xSmall_semantic_bounds() {
        rule
            .setMaterialContentForSizeAssertions {
                FilledIconButton(
                    onClick = { /* doSomething() */ },
                    modifier =
                        Modifier.minimumInteractiveComponentSize()
                            .size(IconButtonDefaults.extraSmallContainerSize()),
                    shape = IconButtonDefaults.extraSmallRoundShape,
                ) {
                    Icon(Icons.Filled.Favorite, contentDescription = "Localized description")
                }
            }
            .assertTouchWidthIsEqualTo(IconButtonAccessibilitySize)
            .assertTouchHeightIsEqualTo(IconButtonAccessibilitySize)
    }

    @Test
    fun filledIconButton_small_visualBounds() {
        val expectedWidth =
            with(rule.density) { IconButtonDefaults.smallContainerSize().width.roundToPx() }
        val expectedHeight =
            with(rule.density) { IconButtonDefaults.smallContainerSize().height.roundToPx() }
        val expectedSize = IntSize(expectedWidth, expectedHeight)

        // The bounds of a testTag on a box that contains the progress indicator are not affected
        // by the padding added on the layout of the progress bar.
        assertVisualBounds(
            {
                val size = IconButtonDefaults.smallContainerSize()
                FilledIconButton(
                    onClick = { /* doSomething() */ },
                    modifier =
                        Modifier.minimumInteractiveComponentSize()
                            .size(size)
                            .testTag(IconButtonTestTag),
                    shape = IconButtonDefaults.smallRoundShape,
                ) {
                    Icon(Icons.Filled.Favorite, contentDescription = "Localized description")
                }
            },
            expectedSize,
        )
    }

    @Test
    fun filledIconButton_small_semantic_bounds() {
        rule
            .setMaterialContentForSizeAssertions {
                FilledIconButton(
                    onClick = { /* doSomething() */ },
                    modifier =
                        Modifier.minimumInteractiveComponentSize()
                            .size(IconButtonDefaults.smallContainerSize()),
                ) {
                    Icon(Icons.Filled.Favorite, contentDescription = "Localized description")
                }
            }
            .assertTouchWidthIsEqualTo(IconButtonAccessibilitySize)
            .assertTouchHeightIsEqualTo(IconButtonAccessibilitySize)
    }

    @Test
    fun filledIconButton_medium_size() {
        rule
            .setMaterialContentForSizeAssertions {
                FilledIconButton(onClick = { /* doSomething() */ }) {
                    Icon(Icons.Filled.Favorite, contentDescription = "Localized description")
                }
            }
            .assertWidthIsEqualTo(IconButtonAccessibilitySize)
            .assertHeightIsEqualTo(IconButtonAccessibilitySize)
            .assertTouchWidthIsEqualTo(IconButtonAccessibilitySize)
            .assertTouchHeightIsEqualTo(IconButtonAccessibilitySize)
    }

    @Test
    fun filledIconButton_large_size() {
        var size = DpSize.Zero
        rule
            .setMaterialContentForSizeAssertions {
                size = IconButtonDefaults.largeContainerSize()
                FilledIconButton(
                    onClick = { /* doSomething() */ },
                    modifier = Modifier.size(size),
                ) {
                    Icon(Icons.Filled.Favorite, contentDescription = "Localized description")
                }
            }
            .assertWidthIsEqualTo(size.width)
            .assertHeightIsEqualTo(size.height)
            .assertTouchWidthIsEqualTo(size.width)
            .assertTouchHeightIsEqualTo(size.height)
    }

    @Test
    fun filledIconButton_xlarge_size() {
        var size = DpSize.Zero
        rule
            .setMaterialContentForSizeAssertions {
                size = IconButtonDefaults.extraLargeContainerSize()
                FilledIconButton(
                    onClick = { /* doSomething() */ },
                    modifier = Modifier.size(size),
                ) {
                    Icon(Icons.Filled.Favorite, contentDescription = "Localized description")
                }
            }
            .assertWidthIsEqualTo(size.width)
            .assertHeightIsEqualTo(size.height)
            .assertTouchWidthIsEqualTo(size.width)
            .assertTouchHeightIsEqualTo(size.height)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun filledIconButton_medium_squareShape() {
        var shape: Shape = CircleShape
        val background = Color.Yellow
        val iconButtonColor = Color.Blue
        rule.setMaterialContent(lightColorScheme()) {
            shape = IconButtonDefaults.mediumSquareShape
            Surface(color = background) {
                Box {
                    FilledIconButton(
                        onClick = { /* doSomething() */ },
                        modifier =
                            Modifier.semantics(mergeDescendants = true) {}
                                .testTag(IconTestTag)
                                .size(IconButtonDefaults.mediumContainerSize()),
                        shape = shape,
                        colors =
                            IconButtonDefaults.iconButtonVibrantColors(
                                containerColor = iconButtonColor
                            ),
                    ) {}
                }
            }
        }

        rule
            .onNodeWithTag(IconTestTag)
            .captureToImage()
            .assertShape(
                density = rule.density,
                shape = shape,
                shapeColor = iconButtonColor,
                backgroundColor = background,
                antiAliasingGap = with(rule.density) { 1.dp.toPx() },
            )
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun filledIconButton_medium_pressedShape() {
        lateinit var shape: Shape
        val backgroundColor = Color.Yellow
        val shapeColor = Color.Blue
        rule.setMaterialContent(lightColorScheme()) {
            shape = IconButtonDefaults.mediumPressedShape
            Surface(color = backgroundColor) {
                FilledIconButton(
                    onClick = { /* doSomething() */ },
                    shapes =
                        IconButtonShapes(
                            shape = IconButtonDefaults.mediumRoundShape,
                            pressedShape = IconButtonDefaults.mediumPressedShape,
                        ),
                    modifier =
                        Modifier.testTag(IconTestTag)
                            .size(IconButtonDefaults.mediumContainerSize()),
                    colors =
                        IconButtonDefaults.iconButtonVibrantColors(
                            containerColor = shapeColor,
                            contentColor = shapeColor,
                        ),
                ) {}
            }
        }
        rule.onNodeWithTag(IconTestTag).performTouchInput { down(center) }

        rule
            .onNodeWithTag(IconTestTag)
            .captureToImage()
            .assertShape(
                density = rule.density,
                shape = shape,
                shapeColor = shapeColor,
                backgroundColor = backgroundColor,
                antiAliasingGap = with(rule.density) { 1.dp.toPx() },
            )
    }

    @Test
    fun filledIconButton_sizeWithoutMinTargetEnforcement() {
        rule
            .setMaterialContentForSizeAssertions {
                CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
                    FilledIconButton(onClick = { /* doSomething() */ }) {
                        Icon(Icons.Filled.Favorite, contentDescription = "Localized description")
                    }
                }
            }
            .assertWidthIsEqualTo(IconButtonSize)
            .assertHeightIsEqualTo(IconButtonSize)
            .assertTouchWidthIsEqualTo(IconButtonSize)
            .assertTouchHeightIsEqualTo(IconButtonSize)
    }

    @Test
    fun filledIconButton_defaultSemantics() {
        rule.setMaterialContent(lightColorScheme()) {
            FilledIconButton(onClick = { /* doSomething() */ }) {
                Icon(Icons.Filled.Favorite, contentDescription = "Localized description")
            }
        }
        rule.onNode(hasClickAction()).apply {
            assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            assertIsEnabled()
        }
    }

    @Test
    fun filledIconButton_disabledSemantics() {
        rule.setMaterialContent(lightColorScheme()) {
            FilledIconButton(onClick = {}, enabled = false) {}
        }
        rule.onNode(hasClickAction()).apply {
            assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            assertIsNotEnabled()
        }
    }

    @Test
    fun filledIconButton_materialIconSize_iconPositioning() {
        rule.setMaterialContent(lightColorScheme()) {
            Box {
                FilledIconButton(onClick = {}) { Box(Modifier.size(IconSize).testTag(IconTestTag)) }
            }
        }

        // Icon should be centered inside the IconButton
        rule
            .onNodeWithTag(IconTestTag, useUnmergedTree = true)
            .assertLeftPositionInRootIsEqualTo((IconButtonAccessibilitySize - IconSize) / 2)
            .assertTopPositionInRootIsEqualTo((IconButtonAccessibilitySize - IconSize) / 2)
    }

    @Test
    fun filledIconButton_customIconSize_iconPositioning() {
        val width = 36.dp
        val height = 14.dp
        rule.setMaterialContent(lightColorScheme()) {
            Box {
                FilledIconButton(onClick = {}) {
                    Box(Modifier.size(width, height).testTag(IconTestTag))
                }
            }
        }

        // Icon should be centered inside the FilledIconButton
        rule
            .onNodeWithTag(IconTestTag, useUnmergedTree = true)
            .assertLeftPositionInRootIsEqualTo((IconButtonAccessibilitySize - width) / 2)
            .assertTopPositionInRootIsEqualTo((IconButtonAccessibilitySize - height) / 2)
    }

    @Test
    fun filledIconButton_defaultColors() {
        rule.setMaterialContent(lightColorScheme()) {
            Truth.assertThat(IconButtonDefaults.filledIconButtonColors())
                .isEqualTo(
                    IconButtonColors(
                        containerColor = FilledIconButtonTokens.ContainerColor.value,
                        contentColor = contentColorFor(FilledIconButtonTokens.ContainerColor.value),
                        disabledContainerColor =
                            FilledIconButtonTokens.DisabledContainerColor.value.copy(
                                alpha = FilledIconButtonTokens.DisabledContainerOpacity
                            ),
                        disabledContentColor =
                            FilledIconButtonTokens.DisabledColor.value.copy(
                                alpha = FilledIconButtonTokens.DisabledOpacity
                            ),
                    )
                )
        }
    }

    @Test
    fun filledTonalIconButton_defaultSemantics() {
        rule.setMaterialContent(lightColorScheme()) {
            FilledTonalIconButton(onClick = { /* doSomething() */ }) {
                Icon(Icons.Filled.Favorite, contentDescription = "Localized description")
            }
        }
        rule.onNode(hasClickAction()).apply {
            assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            assertIsEnabled()
        }
    }

    @Test
    fun filledTonalIconButton_disabledSemantics() {
        rule.setMaterialContent(lightColorScheme()) {
            FilledTonalIconButton(onClick = {}, enabled = false) {
                Icon(Icons.Filled.Favorite, contentDescription = "Localized description")
            }
        }
        rule.onNode(hasClickAction()).apply {
            assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            assertIsNotEnabled()
        }
    }

    @Test
    fun filledTonalIconButton_defaultColors() {
        rule.setMaterialContent(lightColorScheme()) {
            Truth.assertThat(IconButtonDefaults.filledTonalIconButtonColors())
                .isEqualTo(
                    IconButtonColors(
                        containerColor = FilledTonalIconButtonTokens.ContainerColor.value,
                        contentColor =
                            contentColorFor(FilledTonalIconButtonTokens.ContainerColor.value),
                        disabledContainerColor =
                            FilledTonalIconButtonTokens.DisabledContainerColor.value.copy(
                                alpha = FilledTonalIconButtonTokens.DisabledContainerOpacity
                            ),
                        disabledContentColor =
                            FilledTonalIconButtonTokens.DisabledColor.value.copy(
                                alpha = FilledTonalIconButtonTokens.DisabledOpacity
                            ),
                    )
                )
        }
    }

    @Test
    fun filledIconToggleButton_size() {
        rule
            .setMaterialContentForSizeAssertions {
                FilledIconToggleButton(checked = true, onCheckedChange = { /* doSomething() */ }) {
                    Icon(Icons.Filled.Favorite, contentDescription = "Localized description")
                }
            }
            .assertWidthIsEqualTo(IconButtonAccessibilitySize)
            .assertHeightIsEqualTo(IconButtonAccessibilitySize)
            .assertTouchWidthIsEqualTo(IconButtonAccessibilitySize)
            .assertTouchHeightIsEqualTo(IconButtonAccessibilitySize)
    }

    @Test
    fun filledIconToggleButton_sizeWithoutMinTargetEnforcement() {
        rule
            .setMaterialContentForSizeAssertions {
                CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
                    FilledIconToggleButton(
                        checked = true,
                        onCheckedChange = { /* doSomething() */ },
                    ) {
                        Icon(Icons.Filled.Favorite, contentDescription = "Localized description")
                    }
                }
            }
            .assertWidthIsEqualTo(IconButtonSize)
            .assertHeightIsEqualTo(IconButtonSize)
            .assertTouchWidthIsEqualTo(IconButtonSize)
            .assertTouchHeightIsEqualTo(IconButtonSize)
    }

    @Test
    fun filledIconToggleButton_defaultSemantics() {
        rule.setMaterialContent(lightColorScheme()) {
            var checked by remember { mutableStateOf(false) }
            FilledIconToggleButton(checked = checked, onCheckedChange = { checked = it }) {
                Icon(Icons.Filled.Favorite, contentDescription = "Localized description")
            }
        }
        rule.onNode(isToggleable()).apply {
            assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Checkbox))
            assertIsEnabled()
            assertIsOff()
            performClick()
            assertIsOn()
        }
    }

    @Test
    fun filledIconToggleButton_disabledSemantics() {
        rule.setMaterialContent(lightColorScheme()) {
            FilledIconToggleButton(checked = false, onCheckedChange = {}, enabled = false) {}
        }
        rule.onNode(isToggleable()).apply {
            assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Checkbox))
            assertIsNotEnabled()
            assertIsOff()
        }
    }

    @Test
    fun filledIconToggleButton_materialIconSize_iconPositioning() {
        rule.setMaterialContent(lightColorScheme()) {
            Box {
                FilledIconToggleButton(checked = false, onCheckedChange = {}) {
                    Box(Modifier.size(IconSize).testTag(IconTestTag))
                }
            }
        }

        // Icon should be centered inside the FilledIconToggleButton
        rule
            .onNodeWithTag(IconTestTag, useUnmergedTree = true)
            .assertLeftPositionInRootIsEqualTo((IconButtonAccessibilitySize - IconSize) / 2)
            .assertTopPositionInRootIsEqualTo((IconButtonAccessibilitySize - IconSize) / 2)
    }

    @Test
    fun filledIconToggleButton_customIconSize_iconPositioning() {
        val width = 36.dp
        val height = 14.dp
        rule.setMaterialContent(lightColorScheme()) {
            Box {
                FilledIconToggleButton(checked = false, onCheckedChange = {}) {
                    Box(Modifier.size(width, height).testTag(IconTestTag))
                }
            }
        }

        // Icon should be centered inside the FilledIconToggleButton
        rule
            .onNodeWithTag(IconTestTag, useUnmergedTree = true)
            .assertLeftPositionInRootIsEqualTo((IconButtonAccessibilitySize - width) / 2)
            .assertTopPositionInRootIsEqualTo((IconButtonAccessibilitySize - height) / 2)
    }

    @Test
    fun filledIconToggleButton_defaultColors() {
        rule.setMaterialContent(lightColorScheme()) {
            Truth.assertThat(IconButtonDefaults.filledIconToggleButtonColors())
                .isEqualTo(
                    IconToggleButtonColors(
                        containerColor = FilledIconButtonTokens.UnselectedContainerColor.value,
                        // TODO(b/228455081): Using contentColorFor here will return
                        // OnSurfaceVariant,
                        //  while the token value is Primary.
                        contentColor = FilledIconButtonTokens.UnselectedColor.value,
                        disabledContainerColor =
                            FilledIconButtonTokens.DisabledContainerColor.value.copy(
                                alpha = FilledIconButtonTokens.DisabledContainerOpacity
                            ),
                        disabledContentColor =
                            FilledIconButtonTokens.DisabledColor.value.copy(
                                alpha = FilledIconButtonTokens.DisabledOpacity
                            ),
                        checkedContainerColor = FilledIconButtonTokens.SelectedContainerColor.value,
                        checkedContentColor =
                            contentColorFor(FilledIconButtonTokens.SelectedContainerColor.value),
                    )
                )
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun filledIconToggleButton_checked_medium_squareShape() {
        var shape: Shape = CircleShape
        val background = Color.Yellow
        val iconButtonColor = Color.Blue
        rule.setMaterialContent(lightColorScheme()) {
            shape = IconButtonDefaults.mediumSquareShape
            Surface(color = background) {
                Box {
                    FilledIconToggleButton(
                        checked = true,
                        onCheckedChange = { /* doSomething() */ },
                        shapes =
                            IconToggleButtonShapes(
                                shape = IconButtonDefaults.mediumSquareShape,
                                pressedShape = IconButtonDefaults.mediumPressedShape,
                                checkedShape = IconButtonDefaults.mediumSquareShape,
                            ),
                        modifier =
                            Modifier.semantics(mergeDescendants = true) {}
                                .testTag(IconTestTag)
                                .size(IconButtonDefaults.mediumContainerSize()),
                        colors =
                            IconButtonDefaults.iconToggleButtonVibrantColors(
                                checkedContainerColor = iconButtonColor
                            ),
                    ) {}
                }
            }
        }

        rule
            .onNodeWithTag(IconTestTag)
            .captureToImage()
            .assertShape(
                density = rule.density,
                shape = shape,
                shapeColor = iconButtonColor,
                backgroundColor = background,
                antiAliasingGap = with(rule.density) { 1.dp.toPx() },
            )
    }

    @Test
    fun filledTonalIconToggleButton_defaultColors() {
        rule.setMaterialContent(lightColorScheme()) {
            Truth.assertThat(IconButtonDefaults.filledTonalIconToggleButtonColors())
                .isEqualTo(
                    IconToggleButtonColors(
                        containerColor = FilledTonalIconButtonTokens.UnselectedContainerColor.value,
                        contentColor =
                            contentColorFor(
                                FilledTonalIconButtonTokens.UnselectedContainerColor.value
                            ),
                        disabledContainerColor =
                            FilledTonalIconButtonTokens.DisabledContainerColor.value.copy(
                                alpha = FilledTonalIconButtonTokens.DisabledContainerOpacity
                            ),
                        disabledContentColor =
                            FilledTonalIconButtonTokens.DisabledColor.value.copy(
                                alpha = FilledTonalIconButtonTokens.DisabledOpacity
                            ),
                        checkedContainerColor =
                            FilledTonalIconButtonTokens.SelectedContainerColor.value,
                        checkedContentColor = FilledTonalIconButtonTokens.SelectedColor.value,
                    )
                )
        }
    }

    @Test
    fun outlinedIconButton_size() {
        rule
            .setMaterialContentForSizeAssertions {
                OutlinedIconButton(onClick = { /* doSomething() */ }) {
                    Icon(
                        Icons.Outlined.FavoriteBorder,
                        contentDescription = "Localized description",
                    )
                }
            }
            .assertWidthIsEqualTo(IconButtonAccessibilitySize)
            .assertHeightIsEqualTo(IconButtonAccessibilitySize)
            .assertTouchWidthIsEqualTo(IconButtonAccessibilitySize)
            .assertTouchHeightIsEqualTo(IconButtonAccessibilitySize)
    }

    @Test
    fun outlinedIconButton_sizeWithoutMinTargetEnforcement() {
        rule
            .setMaterialContentForSizeAssertions {
                CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
                    OutlinedIconButton(onClick = { /* doSomething() */ }) {
                        Icon(
                            Icons.Outlined.FavoriteBorder,
                            contentDescription = "Localized description",
                        )
                    }
                }
            }
            .assertWidthIsEqualTo(IconButtonSize)
            .assertHeightIsEqualTo(IconButtonSize)
            .assertTouchWidthIsEqualTo(IconButtonSize)
            .assertTouchHeightIsEqualTo(IconButtonSize)
    }

    @Test
    fun outlinedIconButton_defaultSemantics() {
        rule.setMaterialContent(lightColorScheme()) {
            OutlinedIconButton(onClick = { /* doSomething() */ }) {
                Icon(Icons.Outlined.FavoriteBorder, contentDescription = "Localized description")
            }
        }
        rule.onNode(hasClickAction()).apply {
            assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            assertIsEnabled()
        }
    }

    @Test
    fun outlinedIconButton_disabledSemantics() {
        rule.setMaterialContent(lightColorScheme()) {
            OutlinedIconButton(onClick = {}, enabled = false) {}
        }
        rule.onNode(hasClickAction()).apply {
            assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            assertIsNotEnabled()
        }
    }

    @Test
    fun outlinedIconButton_materialIconSize_iconPositioning() {
        rule.setMaterialContent(lightColorScheme()) {
            Box {
                OutlinedIconButton(onClick = {}) {
                    Box(Modifier.size(IconSize).testTag(IconTestTag))
                }
            }
        }

        // Icon should be centered inside the IconButton
        rule
            .onNodeWithTag(IconTestTag, useUnmergedTree = true)
            .assertLeftPositionInRootIsEqualTo((IconButtonAccessibilitySize - IconSize) / 2)
            .assertTopPositionInRootIsEqualTo((IconButtonAccessibilitySize - IconSize) / 2)
    }

    @Test
    fun outlinedIconButton_customIconSize_iconPositioning() {
        val width = 36.dp
        val height = 14.dp
        rule.setMaterialContent(lightColorScheme()) {
            Box {
                OutlinedIconButton(onClick = {}) {
                    Box(Modifier.size(width, height).testTag(IconTestTag))
                }
            }
        }

        // Icon should be centered inside the OutlinedIconButton
        rule
            .onNodeWithTag(IconTestTag, useUnmergedTree = true)
            .assertLeftPositionInRootIsEqualTo((IconButtonAccessibilitySize - width) / 2)
            .assertTopPositionInRootIsEqualTo((IconButtonAccessibilitySize - height) / 2)
    }

    @Test
    fun outlinedIconToggleButton_size() {
        rule
            .setMaterialContentForSizeAssertions {
                OutlinedIconToggleButton(
                    checked = true,
                    onCheckedChange = { /* doSomething() */ },
                ) {
                    Icon(
                        Icons.Outlined.FavoriteBorder,
                        contentDescription = "Localized description",
                    )
                }
            }
            .assertWidthIsEqualTo(IconButtonAccessibilitySize)
            .assertHeightIsEqualTo(IconButtonAccessibilitySize)
            .assertTouchWidthIsEqualTo(IconButtonAccessibilitySize)
            .assertTouchHeightIsEqualTo(IconButtonAccessibilitySize)
    }

    @Test
    fun outlinedIconToggleButton_sizeWithoutMinTargetEnforcement() {
        rule
            .setMaterialContentForSizeAssertions {
                CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
                    OutlinedIconToggleButton(
                        checked = true,
                        onCheckedChange = { /* doSomething() */ },
                    ) {
                        Icon(
                            Icons.Outlined.FavoriteBorder,
                            contentDescription = "Localized description",
                        )
                    }
                }
            }
            .assertWidthIsEqualTo(IconButtonSize)
            .assertHeightIsEqualTo(IconButtonSize)
            .assertTouchWidthIsEqualTo(IconButtonSize)
            .assertTouchHeightIsEqualTo(IconButtonSize)
    }

    @Test
    fun outlinedIconToggleButton_defaultSemantics() {
        rule.setMaterialContent(lightColorScheme()) {
            var checked by remember { mutableStateOf(false) }
            OutlinedIconToggleButton(checked = checked, onCheckedChange = { checked = it }) {
                Icon(Icons.Outlined.FavoriteBorder, contentDescription = "Localized description")
            }
        }
        rule.onNode(isToggleable()).apply {
            assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Checkbox))
            assertIsEnabled()
            assertIsOff()
            performClick()
            assertIsOn()
        }
    }

    @Test
    fun outlinedIconToggleButton_disabledSemantics() {
        rule.setMaterialContent(lightColorScheme()) {
            OutlinedIconToggleButton(checked = false, onCheckedChange = {}, enabled = false) {}
        }
        rule.onNode(isToggleable()).apply {
            assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Checkbox))
            assertIsNotEnabled()
            assertIsOff()
        }
    }

    @Test
    fun outlinedIconToggleButton_materialIconSize_iconPositioning() {
        rule.setMaterialContent(lightColorScheme()) {
            Box {
                OutlinedIconToggleButton(checked = false, onCheckedChange = {}) {
                    Box(Modifier.size(IconSize).testTag(IconTestTag))
                }
            }
        }

        // Icon should be centered inside the OutlinedIconToggleButton
        rule
            .onNodeWithTag(IconTestTag, useUnmergedTree = true)
            .assertLeftPositionInRootIsEqualTo((IconButtonAccessibilitySize - IconSize) / 2)
            .assertTopPositionInRootIsEqualTo((IconButtonAccessibilitySize - IconSize) / 2)
    }

    @Test
    fun outlinedIconToggleButton_customIconSize_iconPositioning() {
        val width = 36.dp
        val height = 14.dp
        rule.setMaterialContent(lightColorScheme()) {
            Box {
                OutlinedIconToggleButton(checked = false, onCheckedChange = {}) {
                    Box(Modifier.size(width, height).testTag(IconTestTag))
                }
            }
        }

        // Icon should be centered inside the OutlinedIconToggleButton
        rule
            .onNodeWithTag(IconTestTag, useUnmergedTree = true)
            .assertLeftPositionInRootIsEqualTo((IconButtonAccessibilitySize - width) / 2)
            .assertTopPositionInRootIsEqualTo((IconButtonAccessibilitySize - height) / 2)
    }

    @Test
    fun outlinedIconButton_defaultLocalContentColors() {
        rule.setMaterialContent(lightColorScheme()) {
            val localContentColor = LocalContentColor.current
            Truth.assertThat(IconButtonDefaults.outlinedIconButtonColors())
                .isEqualTo(
                    IconButtonColors(
                        containerColor = Color.Transparent,
                        contentColor = localContentColor,
                        disabledContainerColor = Color.Transparent,
                        disabledContentColor =
                            localContentColor.copy(alpha = OutlinedIconButtonTokens.DisabledOpacity),
                    )
                )
        }
    }

    @Test
    fun outlinedIconButton_defaultVibrantColors() {
        rule.setMaterialContent(lightColorScheme()) {
            Truth.assertThat(IconButtonDefaults.outlinedIconButtonVibrantColors())
                .isEqualTo(
                    IconButtonColors(
                        containerColor = Color.Transparent,
                        contentColor = OutlinedIconButtonTokens.Color.value,
                        disabledContainerColor = Color.Transparent,
                        disabledContentColor =
                            OutlinedIconButtonTokens.DisabledColor.value.copy(
                                alpha = OutlinedIconButtonTokens.DisabledOpacity
                            ),
                    )
                )
        }
    }

    @Test
    fun outlinedIconToggleButton_useLocalContentColors() {
        rule.setMaterialContent(lightColorScheme()) {
            CompositionLocalProvider(LocalContentColor provides Color.Blue) {
                val colors = IconButtonDefaults.outlinedIconButtonColors()
                assert(colors.contentColor == Color.Blue)
            }

            CompositionLocalProvider(LocalContentColor provides Color.Red) {
                val colors =
                    IconButtonDefaults.outlinedIconButtonColors().copy(containerColor = Color.Green)
                assert(colors.containerColor == Color.Green)
                assert(colors.contentColor == Color.Red)
                assert(
                    colors.disabledContentColor ==
                        Color.Red.copy(OutlinedIconButtonTokens.DisabledOpacity)
                )
            }
        }
    }

    @Test
    fun outlinedIconToggleButton_defaultVibrantColors() {
        rule.setMaterialContent(lightColorScheme()) {
            Truth.assertThat(IconButtonDefaults.outlinedIconToggleButtonVibrantColors())
                .isEqualTo(
                    IconToggleButtonColors(
                        containerColor = Color.Transparent,
                        contentColor = OutlinedIconButtonTokens.UnselectedColor.value,
                        disabledContainerColor = Color.Transparent,
                        disabledContentColor =
                            OutlinedIconButtonTokens.DisabledColor.value.copy(
                                alpha = OutlinedIconButtonTokens.DisabledOpacity
                            ),
                        checkedContainerColor =
                            OutlinedIconButtonTokens.SelectedContainerColor.value,
                        checkedContentColor = OutlinedIconButtonTokens.SelectedColor.value,
                    )
                )
        }
    }

    @Test
    fun outlinedIconButton_borderStroke_defaultLocalContentColor() {
        rule.setMaterialContent(lightColorScheme()) {
            CompositionLocalProvider(LocalContentColor provides Color.Blue) {
                Truth.assertThat(IconButtonDefaults.outlinedIconButtonBorder(enabled = true))
                    .isEqualTo(BorderStroke(SmallIconButtonTokens.OutlinedOutlineWidth, Color.Blue))
            }
        }
    }

    @Test
    fun outlinedIconToggleButton_borderStroke_defaultVibrantColor() {
        rule.setMaterialContent(lightColorScheme()) {
            val outlineColor = OutlinedIconButtonTokens.OutlineColor.value
            Truth.assertThat(
                    IconButtonDefaults.outlinedIconToggleButtonVibrantBorder(
                        enabled = false,
                        checked = false,
                    )
                )
                .isEqualTo(
                    BorderStroke(
                        SmallIconButtonTokens.OutlinedOutlineWidth,
                        outlineColor.copy(alpha = OutlinedIconButtonTokens.DisabledOpacity),
                    )
                )
        }
    }

    private fun assertVisualBounds(composable: @Composable () -> Unit, expectedSize: IntSize) {
        // The bounds of a testTag on a box that contains the progress indicator are not affected
        // by the padding added on the layout of the progress bar.
        rule.setContent { composable() }

        val node =
            rule
                .onNodeWithTag(IconButtonTestTag)
                .fetchSemanticsNode(
                    errorMessageOnFail = "couldn't find node with tag $IconButtonTestTag"
                )
        val nodeBounds = node.boundsInRoot

        // Check that the visual bounds of an xsmall icon button are the expected visual size.
        assertEquals(expectedSize.width.toFloat(), nodeBounds.width)
        assertEquals(expectedSize.height.toFloat(), nodeBounds.height)
    }

    private val IconButtonAccessibilitySize = 48.0.dp
    private val IconButtonSize = 40.0.dp
    private val IconSize = 24.0.dp
    private val IconTestTag = "icon"
    private val IconButtonTestTag = "iconButton"
}
