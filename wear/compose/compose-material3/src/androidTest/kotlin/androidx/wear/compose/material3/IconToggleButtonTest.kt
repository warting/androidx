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

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.testutils.assertContainsColor
import androidx.compose.testutils.assertShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsToggleable
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.filters.SdkSuppress
import org.junit.Assert
import org.junit.Rule
import org.junit.Test

class IconToggleButtonTest {

    @get:Rule val rule = createComposeRule()

    @Test
    fun supports_testTag() {
        rule.setContentWithTheme {
            IconToggleButton(
                checked = true,
                onCheckedChange = {},
                content = { TestImage() },
                modifier = Modifier.testTag(TEST_TAG),
            )
        }
        rule.waitForIdle()

        rule.onNodeWithTag(TEST_TAG).assertExists()
    }

    @Test
    fun has_clickAction_when_enabled() {
        rule.setContentWithTheme {
            IconToggleButton(
                enabled = true,
                checked = true,
                onCheckedChange = {},
                content = { TestImage() },
                modifier = Modifier.testTag(TEST_TAG),
            )
        }
        rule.waitForIdle()

        rule.onNodeWithTag(TEST_TAG).assertHasClickAction()
    }

    @Test
    fun has_clickAction_when_disabled() {
        rule.setContentWithTheme {
            IconToggleButton(
                enabled = false,
                checked = true,
                onCheckedChange = {},
                content = { TestImage() },
                modifier = Modifier.testTag(TEST_TAG),
            )
        }
        rule.waitForIdle()

        rule.onNodeWithTag(TEST_TAG).assertHasClickAction()
    }

    @Test
    fun is_toggleable() {
        rule.setContentWithTheme {
            IconToggleButton(
                enabled = true,
                checked = true,
                onCheckedChange = {},
                content = { TestImage() },
                modifier = Modifier.testTag(TEST_TAG),
            )
        }
        rule.waitForIdle()

        rule.onNodeWithTag(TEST_TAG).assertIsToggleable()
    }

    @Test
    fun is_correctly_enabled() {
        rule.setContentWithTheme {
            IconToggleButton(
                enabled = true,
                checked = true,
                onCheckedChange = {},
                content = { TestImage() },
                modifier = Modifier.testTag(TEST_TAG),
            )
        }
        rule.waitForIdle()

        rule.onNodeWithTag(TEST_TAG).assertIsEnabled()
    }

    @Test
    fun is_correctly_disabled() {
        rule.setContentWithTheme {
            IconToggleButton(
                enabled = false,
                checked = true,
                onCheckedChange = {},
                content = { TestImage() },
                modifier = Modifier.testTag(TEST_TAG),
            )
        }
        rule.waitForIdle()

        rule.onNodeWithTag(TEST_TAG).assertIsNotEnabled()
    }

    @Test
    fun is_on_when_checked() {
        rule.setContentWithTheme {
            IconToggleButton(
                checked = true,
                onCheckedChange = {},
                content = { TestImage() },
                modifier = Modifier.testTag(TEST_TAG),
            )
        }
        rule.waitForIdle()

        rule.onNodeWithTag(TEST_TAG).assertIsOn()
    }

    @Test
    fun is_off_when_unchecked() {
        rule.setContentWithTheme {
            IconToggleButton(
                checked = false,
                onCheckedChange = {},
                content = { TestImage() },
                modifier = Modifier.testTag(TEST_TAG),
            )
        }
        rule.waitForIdle()

        rule.onNodeWithTag(TEST_TAG).assertIsOff()
    }

    @Test
    fun responds_to_toggle_on() {
        rule.setContentWithTheme {
            val (checked, onCheckedChange) = remember { mutableStateOf(false) }
            IconToggleButton(
                checked = checked,
                onCheckedChange = onCheckedChange,
                content = { TestImage() },
                modifier = Modifier.testTag(TEST_TAG),
            )
        }
        rule.waitForIdle()

        rule.onNodeWithTag(TEST_TAG).assertIsOff().performClick().assertIsOn()
    }

    @Test
    fun responds_to_toggle_off() {
        rule.setContentWithTheme {
            val (checked, onCheckedChange) = remember { mutableStateOf(true) }
            IconToggleButton(
                checked = checked,
                onCheckedChange = onCheckedChange,
                content = { TestImage() },
                modifier = Modifier.testTag(TEST_TAG),
            )
        }
        rule.waitForIdle()

        rule.onNodeWithTag(TEST_TAG).assertIsOn().performClick().assertIsOff()
    }

    @Test
    fun does_not_toggle_when_disabled() {
        rule.setContentWithTheme {
            val (checked, onCheckedChange) = remember { mutableStateOf(false) }
            IconToggleButton(
                enabled = false,
                checked = checked,
                onCheckedChange = onCheckedChange,
                content = { TestImage() },
                modifier = Modifier.testTag(TEST_TAG),
            )
        }
        rule.waitForIdle()

        rule.onNodeWithTag(TEST_TAG).assertIsOff().performClick().assertIsOff()
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun is_circular_under_ltr() =
        rule.isShape(
            shape = CircleShape,
            layoutDirection = LayoutDirection.Ltr,
            shapeColorComposable = { shapeColor() },
        ) {
            IconToggleButton(
                enabled = true,
                checked = true,
                onCheckedChange = {},
                content = {},
                modifier = Modifier.testTag(TEST_TAG),
            )
        }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun is_circular_under_rtl() =
        rule.isShape(
            shape = CircleShape,
            layoutDirection = LayoutDirection.Rtl,
            shapeColorComposable = { shapeColor() },
        ) {
            IconToggleButton(
                enabled = true,
                checked = true,
                onCheckedChange = {},
                content = {},
                modifier = Modifier.testTag(TEST_TAG),
            )
        }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun allows_shape_overrides() =
        rule.isShape(
            shape = RectangleShape,
            layoutDirection = LayoutDirection.Ltr,
            shapeColorComposable = { shapeColor() },
        ) {
            IconToggleButton(
                enabled = true,
                checked = true,
                shapes = IconToggleButtonDefaults.shapes(RectangleShape),
                onCheckedChange = {},
                content = {},
                modifier = Modifier.testTag(TEST_TAG),
            )
        }

    @Test
    fun gives_default_correct_tapSize() =
        rule.verifyTapSize(52.dp) {
            IconToggleButton(
                enabled = true,
                checked = true,
                onCheckedChange = {},
                content = {},
                modifier = it.touchTargetAwareSize(IconToggleButtonDefaults.Size),
            )
        }

    @Test
    fun gives_small_correct_tapSize() =
        rule.verifyTapSize(48.dp) {
            IconToggleButton(
                enabled = true,
                checked = true,
                onCheckedChange = {},
                content = {},
                modifier = it.touchTargetAwareSize(IconToggleButtonDefaults.SmallSize),
            )
        }

    @Test
    fun gives_large_correct_tapSize() =
        rule.verifyTapSize(60.dp) {
            IconToggleButton(
                enabled = true,
                checked = true,
                onCheckedChange = {},
                content = {},
                modifier = it.touchTargetAwareSize(IconToggleButtonDefaults.LargeSize),
            )
        }

    @Test
    fun gives_extraLarge_correct_tapSize() =
        rule.verifyTapSize(72.dp) {
            IconToggleButton(
                enabled = true,
                checked = true,
                onCheckedChange = {},
                content = {},
                modifier = it.touchTargetAwareSize(IconToggleButtonDefaults.ExtraLargeSize),
            )
        }

    @Test
    fun gives_default_correct_size() =
        rule.verifyActualSize(52.dp) {
            IconToggleButton(
                enabled = true,
                checked = true,
                onCheckedChange = {},
                content = {},
                modifier = it.touchTargetAwareSize(IconToggleButtonDefaults.Size),
            )
        }

    @Test
    fun gives_small_correct_size() =
        rule.verifyActualSize(48.dp) {
            IconToggleButton(
                enabled = true,
                checked = true,
                onCheckedChange = {},
                content = {},
                modifier = it.touchTargetAwareSize(IconToggleButtonDefaults.SmallSize),
            )
        }

    @Test
    fun gives_extraLarge_correct_size() =
        rule.verifyActualSize(72.dp) {
            IconToggleButton(
                enabled = true,
                checked = true,
                onCheckedChange = {},
                content = {},
                modifier = it.touchTargetAwareSize(IconToggleButtonDefaults.ExtraLargeSize),
            )
        }

    @Test
    fun gives_large_correct_size() =
        rule.verifyActualSize(60.dp) {
            IconToggleButton(
                enabled = true,
                checked = true,
                onCheckedChange = {},
                content = {},
                modifier = it.touchTargetAwareSize(IconToggleButtonDefaults.LargeSize),
            )
        }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun gives_checked_primary_colors() =
        rule.verifyIconToggleButtonColors(
            status = Status.Enabled,
            checked = true,
            colors = { IconToggleButtonDefaults.colors() },
            containerColor = { MaterialTheme.colorScheme.primary },
            contentColor = { MaterialTheme.colorScheme.onPrimary },
        )

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun gives_unchecked_surface_colors() =
        rule.verifyIconToggleButtonColors(
            status = Status.Enabled,
            checked = false,
            colors = { IconToggleButtonDefaults.colors() },
            containerColor = { MaterialTheme.colorScheme.surfaceContainer },
            contentColor = { MaterialTheme.colorScheme.onSurfaceVariant },
        )

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun gives_disabled_unchecked_surface_colors_with_alpha() =
        rule.verifyIconToggleButtonColors(
            status = Status.Disabled,
            checked = false,
            colors = { IconToggleButtonDefaults.colors() },
            containerColor = {
                MaterialTheme.colorScheme.onSurface.toDisabledColor(DisabledContainerAlpha)
            },
            contentColor = { MaterialTheme.colorScheme.onSurface.toDisabledColor() },
        )

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun gives_disabled_primary_checked_contrasting_content_color() =
        rule.verifyIconToggleButtonColors(
            status = Status.Disabled,
            checked = true,
            colors = { IconToggleButtonDefaults.colors() },
            containerColor = {
                MaterialTheme.colorScheme.onSurface.toDisabledColor(DisabledContainerAlpha)
            },
            contentColor = { MaterialTheme.colorScheme.onSurface.toDisabledColor() },
        )

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun allows_custom_checked_background_override() {
        val overrideColor = Color.Yellow

        rule.verifyIconToggleButtonColors(
            status = Status.Enabled,
            checked = true,
            colors = { IconToggleButtonDefaults.colors(checkedContainerColor = overrideColor) },
            containerColor = { overrideColor },
            contentColor = { MaterialTheme.colorScheme.onPrimary },
        )
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun allows_custom_checked_content_override() {
        val overrideColor = Color.Green

        rule.verifyIconToggleButtonColors(
            status = Status.Enabled,
            checked = true,
            colors = { IconToggleButtonDefaults.colors(checkedContentColor = overrideColor) },
            containerColor = { MaterialTheme.colorScheme.primary },
            contentColor = { overrideColor },
        )
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun allows_custom_unchecked_background_override() {
        val overrideColor = Color.Red

        rule.verifyIconToggleButtonColors(
            status = Status.Enabled,
            checked = false,
            colors = { IconToggleButtonDefaults.colors(uncheckedContainerColor = overrideColor) },
            containerColor = { overrideColor },
            contentColor = { MaterialTheme.colorScheme.onSurfaceVariant },
        )
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun allows_custom_unchecked_content_override() {
        val overrideColor = Color.Green

        rule.verifyIconToggleButtonColors(
            status = Status.Enabled,
            checked = false,
            colors = { IconToggleButtonDefaults.colors(uncheckedContentColor = overrideColor) },
            containerColor = { MaterialTheme.colorScheme.surfaceContainer },
            contentColor = { overrideColor },
        )
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun allows_custom_checked_disabled_background_override() {
        val overrideColor = Color.Yellow

        rule.verifyIconToggleButtonColors(
            status = Status.Disabled,
            checked = true,
            colors = {
                IconToggleButtonDefaults.colors(
                    // Apply the content color override for the content alpha to be applied
                    disabledCheckedContainerColor = overrideColor
                )
            },
            containerColor = { overrideColor },
            contentColor = { MaterialTheme.colorScheme.onSurface.toDisabledColor() },
        )
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun allows_custom_checked_disabled_content_override() {
        val overrideColor = Color.Green

        rule.verifyIconToggleButtonColors(
            status = Status.Disabled,
            checked = true,
            colors = {
                IconToggleButtonDefaults.colors(
                    // Apply the content color override for the content alpha to be applied
                    disabledCheckedContentColor = overrideColor
                )
            },
            containerColor = {
                MaterialTheme.colorScheme.onSurface.toDisabledColor(DisabledContainerAlpha)
            },
            contentColor = { overrideColor },
        )
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun allows_custom_unchecked_disabled_background_override() {
        val overrideColor = Color.Red

        rule.verifyIconToggleButtonColors(
            status = Status.Disabled,
            checked = false,
            colors = {
                IconToggleButtonDefaults.colors(
                    // Apply the content color override for the content alpha to be applied
                    disabledUncheckedContainerColor = overrideColor
                )
            },
            containerColor = { overrideColor },
            contentColor = { MaterialTheme.colorScheme.onSurface.toDisabledColor() },
        )
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun allows_custom_unchecked_disabled_content_override() {
        val overrideColor = Color.Green

        rule.verifyIconToggleButtonColors(
            status = Status.Disabled,
            checked = false,
            colors = {
                IconToggleButtonDefaults.colors(
                    // Apply the content color override for the content alpha to be applied
                    disabledUncheckedContentColor = overrideColor
                )
            },
            contentColor = { overrideColor },
            containerColor = {
                MaterialTheme.colorScheme.onSurface.toDisabledColor(DisabledContainerAlpha)
            },
        )
    }

    @Test
    fun default_role_checkbox() {
        rule.setContentWithTheme {
            IconToggleButton(
                checked = false,
                onCheckedChange = {},
                enabled = false,
                content = { TestImage() },
                modifier = Modifier.testTag(TEST_TAG),
            )
        }

        rule
            .onNodeWithTag(TEST_TAG)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Checkbox))
    }

    @Test
    fun allows_custom_role() {
        val overrideRole = Role.Button

        rule.setContentWithTheme {
            IconToggleButton(
                checked = false,
                onCheckedChange = {},
                enabled = false,
                content = { TestImage() },
                modifier = Modifier.testTag(TEST_TAG).semantics { role = overrideRole },
            )
        }

        rule
            .onNodeWithTag(TEST_TAG)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, overrideRole))
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun animates_corners_to_75_percent_on_click() {
        val uncheckedShape = RoundedCornerShape(20.dp)
        val checkedShape = RoundedCornerShape(10.dp)
        val pressedShape = RoundedCornerShape(0.dp)
        // Ignore the color transition from unchecked to checked color
        val colors =
            IconToggleButtonColors(
                Color.Black,
                Color.Black,
                Color.Black,
                Color.Black,
                Color.Black,
                Color.Black,
                Color.Black,
                Color.Black,
            )

        rule.verifyRoundedButtonTapAnimationEnd(
            uncheckedShape,
            pressedShape,
            0.75f,
            8,
            color = { colors.checkedContainerColor },
        ) { modifier ->
            IconToggleButton(
                checked = false,
                onCheckedChange = {},
                modifier = modifier,
                shapes = IconToggleButtonShapes(uncheckedShape, checkedShape, pressedShape),
                colors = colors,
            ) {}
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun changes_unchecked_to_checked_shape_on_click() {
        val uncheckedShape = RoundedCornerShape(20.dp)
        val checkedShape = RoundedCornerShape(10.dp)
        val pressedShape = RoundedCornerShape(0.dp)
        rule.verifyRoundedButtonTapAnimationEnd(
            uncheckedShape,
            checkedShape,
            1f,
            100,
            color = { shapeColor(checked = true) },
            antiAliasingGap = 4f,
        ) { modifier ->
            var checked by remember { mutableStateOf(false) }
            IconToggleButton(
                checked = checked,
                onCheckedChange = { checked = !checked },
                modifier = modifier,
                shapes = IconToggleButtonShapes(uncheckedShape, checkedShape, pressedShape),
            ) {}
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun changes_checked_to_unchecked_shape_on_click() {
        val uncheckedShape = RoundedCornerShape(10.dp)
        val checkedShape = RoundedCornerShape(20.dp)
        val pressedShape = RoundedCornerShape(0.dp)
        rule.verifyRoundedButtonTapAnimationEnd(
            checkedShape,
            uncheckedShape,
            1f,
            100,
            color = { shapeColor(checked = false) },
            antiAliasingGap = 4f,
        ) { modifier ->
            var checked by remember { mutableStateOf(true) }
            IconToggleButton(
                checked = checked,
                onCheckedChange = { checked = !checked },
                modifier = modifier,
                shapes =
                    IconToggleButtonShapes(uncheckedShape, checkedShape, pressedShape, pressedShape),
            ) {}
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun changes_to_unchecked_pressed_shape_when_pressed_on_unchecked() {
        val uncheckedShape = RoundedCornerShape(20.dp)
        val checkedShape = RoundedCornerShape(10.dp)
        val uncheckedPressedShape = RoundedCornerShape(0.dp)
        val checkedPressedShape = RoundedCornerShape(5.dp)

        rule.verifyRoundedButtonTapAnimationEnd(
            uncheckedShape,
            uncheckedPressedShape,
            1f,
            100,
            color = { shapeColor(checked = false) },
            releaseAfterTap = false,
        ) { modifier ->
            CompositionLocalProvider(LocalContentColor provides shapeColor(checked = false)) {
                IconToggleButton(
                    checked = false,
                    onCheckedChange = {},
                    modifier = modifier,
                    shapes =
                        IconToggleButtonShapes(
                            uncheckedShape,
                            checkedShape,
                            uncheckedPressedShape,
                            checkedPressedShape,
                        ),
                ) {}
            }
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun changes_to_checked_pressed_shape_when_pressed_on_checked() {
        val uncheckedShape = RoundedCornerShape(10.dp)
        val checkedShape = RoundedCornerShape(20.dp)
        val uncheckedPressedShape = RoundedCornerShape(5.dp)
        val checkedPressedShape = RoundedCornerShape(0.dp)

        rule.verifyRoundedButtonTapAnimationEnd(
            checkedShape,
            checkedPressedShape,
            1f,
            100,
            color = { shapeColor(checked = true) },
            releaseAfterTap = false,
        ) { modifier ->
            CompositionLocalProvider(LocalContentColor provides shapeColor(checked = true)) {
                IconToggleButton(
                    checked = true,
                    onCheckedChange = {},
                    modifier = modifier,
                    shapes =
                        IconToggleButtonShapes(
                            uncheckedShape,
                            checkedShape,
                            uncheckedPressedShape,
                            checkedPressedShape,
                        ),
                ) {}
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun ComposeContentTestRule.verifyIconToggleButtonColors(
        status: Status,
        checked: Boolean,
        colors: @Composable () -> IconToggleButtonColors,
        containerColor: @Composable () -> Color,
        contentColor: @Composable () -> Color,
    ) {
        verifyColors(
            expectedContainerColor = containerColor,
            expectedContentColor = contentColor,
            content = {
                var actualContentColor = Color.Transparent
                IconToggleButton(
                    onCheckedChange = {},
                    enabled = status.enabled(),
                    checked = checked,
                    colors = colors(),
                    modifier = Modifier.testTag(TEST_TAG),
                ) {
                    actualContentColor = LocalContentColor.current
                }
                return@verifyColors actualContentColor
            },
        )
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun changes_unchecked_to_checked_shape_when_checked_changed() {
        val uncheckedShape = RoundedCornerShape(20.dp)
        val checkedShape = RoundedCornerShape(10.dp)
        val pressedShape = RoundedCornerShape(0.dp)
        val checked = mutableStateOf(false)

        rule.verifyCheckedStateChange(
            updateState = { checked.value = !checked.value },
            startShape = uncheckedShape,
            endShape = checkedShape,
            uncheckedColorComposable = { shapeColor(checked = false) },
            checkedColorComposable = { shapeColor(checked = true) },
            content = {
                IconToggleButton(
                    checked = checked.value,
                    onCheckedChange = { checked.value = !checked.value },
                    shapes = IconToggleButtonShapes(uncheckedShape, checkedShape, pressedShape),
                    modifier = Modifier.testTag(TEST_TAG),
                ) {}
            },
        )
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun changes_checked_to_unchecked_shape_when_checked_changed() {
        val uncheckedShape = RoundedCornerShape(20.dp)
        val checkedShape = RoundedCornerShape(10.dp)
        val pressedShape = RoundedCornerShape(0.dp)
        val checked = mutableStateOf(false)

        rule.verifyCheckedStateChange(
            updateState = { checked.value = !checked.value },
            startShape = uncheckedShape,
            endShape = checkedShape,
            uncheckedColorComposable = { shapeColor(checked = false) },
            checkedColorComposable = { shapeColor(checked = true) },
            content = {
                IconToggleButton(
                    checked = checked.value,
                    onCheckedChange = { checked.value = !checked.value },
                    shapes = IconToggleButtonShapes(uncheckedShape, checkedShape, pressedShape),
                    modifier = Modifier.testTag(TEST_TAG),
                ) {}
            },
        )
    }

    @Composable
    private fun shapeColor(checked: Boolean = true): Color {
        return IconToggleButtonDefaults.colors()
            .containerColor(enabled = true, checked = checked)
            .value
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun ComposeContentTestRule.isShape(
        shape: Shape = CircleShape,
        layoutDirection: LayoutDirection,
        padding: Dp = 0.dp,
        backgroundColor: Color = Color.Red,
        shapeColorComposable: @Composable () -> Color,
        content: @Composable () -> Unit,
    ) {
        var shapeColor = Color.Transparent
        setContentWithTheme {
            shapeColor = shapeColorComposable.invoke()
            CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                Box(Modifier.padding(padding).background(backgroundColor)) { content() }
            }
        }

        this.waitForIdle()
        onNodeWithTag(TEST_TAG)
            .captureToImage()
            .assertShape(
                density = density,
                shape = shape,
                horizontalPadding = padding,
                verticalPadding = padding,
                backgroundColor = backgroundColor,
                antiAliasingGap = 2.0f,
                shapeColor = shapeColor,
            )
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun ComposeContentTestRule.verifyColors(
        expectedContainerColor: @Composable () -> Color,
        expectedContentColor: @Composable () -> Color,
        content: @Composable () -> Color,
    ) {
        val testBackgroundColor = Color.White
        var finalExpectedContainerColor = Color.Transparent
        var finalExpectedContent = Color.Transparent
        var actualContentColor = Color.Transparent
        setContentWithTheme {
            finalExpectedContainerColor =
                expectedContainerColor().compositeOver(testBackgroundColor)
            finalExpectedContent = expectedContentColor()
            Box(Modifier.fillMaxSize().background(testBackgroundColor)) {
                actualContentColor = content()
            }
        }
        Assert.assertEquals(finalExpectedContent, actualContentColor)
        onNodeWithTag(TEST_TAG).captureToImage().assertContainsColor(finalExpectedContainerColor)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun ComposeContentTestRule.verifyCheckedStateChange(
        updateState: () -> Unit,
        startShape: Shape,
        endShape: Shape,
        padding: Dp = 0.dp,
        backgroundColor: Color = Color.White,
        antiAliasingGap: Float = 2f,
        uncheckedColorComposable: @Composable () -> Color,
        checkedColorComposable: @Composable () -> Color,
        content: @Composable (Modifier) -> Unit,
    ) {
        var uncheckedColor = Color.Transparent
        var checkedColor = Color.Transparent
        rule.setContentWithTheme {
            uncheckedColor = uncheckedColorComposable()
            checkedColor = checkedColorComposable()
            Box(Modifier.padding(padding).background(backgroundColor)) {
                content(Modifier.testTag(TEST_TAG))
            }
        }
        this.waitForIdle()

        // Confirm that the start state is correct
        rule
            .onNodeWithTag(TEST_TAG)
            .captureToImage()
            .assertShape(
                density = rule.density,
                horizontalPadding = padding,
                verticalPadding = padding,
                shapeColor = uncheckedColor,
                backgroundColor = backgroundColor,
                antiAliasingGap = antiAliasingGap,
                shape = startShape,
            )

        // Update state
        updateState()
        this.waitForIdle()

        // Confirm that the end state is correct
        rule
            .onNodeWithTag(TEST_TAG)
            .captureToImage()
            .assertShape(
                density = rule.density,
                horizontalPadding = padding,
                verticalPadding = padding,
                shapeColor = checkedColor,
                backgroundColor = backgroundColor,
                antiAliasingGap = antiAliasingGap,
                shape = endShape,
            )
    }
}
