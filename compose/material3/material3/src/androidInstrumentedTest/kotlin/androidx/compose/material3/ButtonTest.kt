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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.tokens.ElevatedButtonTokens
import androidx.compose.material3.tokens.FilledButtonTokens
import androidx.compose.material3.tokens.FilledTonalButtonTokens
import androidx.compose.material3.tokens.OutlinedButtonTokens
import androidx.compose.material3.tokens.TextButtonTokens
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.testutils.assertShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsEqualTo
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class ButtonTest {

    @get:Rule val rule = createComposeRule()

    @Test
    fun defaultSemantics() {
        rule.setMaterialContent(lightColorScheme()) {
            Box {
                Button(modifier = Modifier.testTag(ButtonTestTag), onClick = {}) {
                    Text("myButton")
                }
            }
        }

        rule
            .onNodeWithTag(ButtonTestTag)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .assertIsEnabled()
    }

    @Test
    fun disabledSemantics() {
        rule.setMaterialContent(lightColorScheme()) {
            Box {
                Button(modifier = Modifier.testTag(ButtonTestTag), onClick = {}, enabled = false) {
                    Text("myButton")
                }
            }
        }

        rule
            .onNodeWithTag(ButtonTestTag)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .assertIsNotEnabled()
    }

    @Test
    fun findByTextAndClick() {
        var counter = 0
        val onClick: () -> Unit = { ++counter }
        val text = "myButton"

        rule.setMaterialContent(lightColorScheme()) {
            Box {
                Button(onClick = onClick, modifier = Modifier.testTag(ButtonTestTag)) { Text(text) }
            }
        }

        // TODO(b/129400818): this actually finds the text, not the button as
        // merge semantics aren't implemented yet
        rule
            .onNodeWithTag(ButtonTestTag)
            // remove this and the todo
            //    rule.onNodeWithText(text)
            .performClick()

        rule.runOnIdle { assertThat(counter).isEqualTo(1) }
    }

    @Test
    fun canBeDisabled() {
        rule.setMaterialContent(lightColorScheme()) {
            var enabled by remember { mutableStateOf(true) }
            val onClick = { enabled = false }
            Box {
                Button(
                    modifier = Modifier.testTag(ButtonTestTag),
                    onClick = onClick,
                    enabled = enabled,
                ) {
                    Text("Hello")
                }
            }
        }
        rule
            .onNodeWithTag(ButtonTestTag)
            // Confirm the button starts off enabled, with a click action
            .assertHasClickAction()
            .assertIsEnabled()
            .performClick()
            // Then confirm it's disabled with click action after clicking it
            .assertHasClickAction()
            .assertIsNotEnabled()
    }

    @Test
    fun clickIsIndependentBetweenButtons() {
        var button1Counter = 0
        val button1OnClick: () -> Unit = { ++button1Counter }
        val button1Tag = "button1"

        var button2Counter = 0
        val button2OnClick: () -> Unit = { ++button2Counter }
        val button2Tag = "button2"

        val text = "myButton"

        rule.setMaterialContent(lightColorScheme()) {
            Column {
                Button(modifier = Modifier.testTag(button1Tag), onClick = button1OnClick) {
                    Text(text)
                }
                Button(modifier = Modifier.testTag(button2Tag), onClick = button2OnClick) {
                    Text(text)
                }
            }
        }

        rule.onNodeWithTag(button1Tag).performClick()

        rule.runOnIdle {
            assertThat(button1Counter).isEqualTo(1)
            assertThat(button2Counter).isEqualTo(0)
        }

        rule.onNodeWithTag(button2Tag).performClick()

        rule.runOnIdle {
            assertThat(button1Counter).isEqualTo(1)
            assertThat(button2Counter).isEqualTo(1)
        }
    }

    @Test
    fun button_positioning() {
        rule.setMaterialContent(lightColorScheme()) {
            Button(onClick = { /* Do something! */ }, modifier = Modifier.testTag(ButtonTestTag)) {
                Text(
                    "Button",
                    modifier = Modifier.testTag(TextTestTag).semantics(mergeDescendants = true) {},
                )
            }
        }

        val buttonBounds = rule.onNodeWithTag(ButtonTestTag).getUnclippedBoundsInRoot()
        val textBounds = rule.onNodeWithTag(TextTestTag).getUnclippedBoundsInRoot()

        (textBounds.left - buttonBounds.left).assertIsEqualTo(
            24.dp,
            "padding between the start of the button and the start of the text.",
        )

        (buttonBounds.right - textBounds.right).assertIsEqualTo(
            24.dp,
            "padding between the end of the text and the end of the button.",
        )
        buttonBounds.height.assertIsEqualTo(ButtonDefaults.MinHeight, "height of button.")
    }

    @Test
    fun button_withIcon_positioning() {
        rule.setMaterialContent(lightColorScheme()) {
            Button(
                onClick = { /* Do something! */ },
                contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
                modifier = Modifier.testTag(ButtonTestTag),
            ) {
                Icon(
                    Icons.Filled.Favorite,
                    contentDescription = "Localized description",
                    modifier =
                        Modifier.size(ButtonDefaults.IconSize).testTag(IconTestTag).semantics(
                            mergeDescendants = true
                        ) {},
                )
                Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                Text(
                    "Like",
                    modifier = Modifier.testTag(TextTestTag).semantics(mergeDescendants = true) {},
                )
            }
        }

        val textBounds = rule.onNodeWithTag(TextTestTag).getUnclippedBoundsInRoot()
        val iconBounds = rule.onNodeWithTag(IconTestTag).getUnclippedBoundsInRoot()
        val buttonBounds = rule.onNodeWithTag(ButtonTestTag).getUnclippedBoundsInRoot()

        (iconBounds.left - buttonBounds.left).assertIsEqualTo(
            16.dp,
            "Padding between start of button and start of icon.",
        )

        (textBounds.left - iconBounds.right).assertIsEqualTo(
            ButtonDefaults.IconSpacing,
            "Padding between end of icon and start of text.",
        )

        (buttonBounds.right - textBounds.right).assertIsEqualTo(
            24.dp,
            "padding between end of text and end of button.",
        )
    }

    @Test
    fun text_button_withIcon_positioning() {
        rule.setMaterialContent(lightColorScheme()) {
            TextButton(
                onClick = { /* Do something! */ },
                contentPadding = ButtonDefaults.TextButtonWithIconContentPadding,
                modifier = Modifier.testTag(ButtonTestTag),
            ) {
                Icon(
                    Icons.Filled.Favorite,
                    contentDescription = "Localized description",
                    modifier =
                        Modifier.size(ButtonDefaults.IconSize).testTag(IconTestTag).semantics(
                            mergeDescendants = true
                        ) {},
                )
                Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                Text(
                    "Like",
                    modifier = Modifier.testTag(TextTestTag).semantics(mergeDescendants = true) {},
                )
            }
        }

        val textBounds = rule.onNodeWithTag(TextTestTag).getUnclippedBoundsInRoot()
        val iconBounds = rule.onNodeWithTag(IconTestTag).getUnclippedBoundsInRoot()
        val buttonBounds = rule.onNodeWithTag(ButtonTestTag).getUnclippedBoundsInRoot()

        (iconBounds.left - buttonBounds.left).assertIsEqualTo(
            12.dp,
            "Padding between start of text button and start of icon.",
        )

        (textBounds.left - iconBounds.right).assertIsEqualTo(
            ButtonDefaults.IconSpacing,
            "Padding between end of icon and start of text.",
        )

        (buttonBounds.right - textBounds.right).assertIsEqualTo(
            16.dp,
            "padding between end of text and end of text button.",
        )
    }

    @Test
    fun button_defaultColors() {
        rule.setMaterialContent(lightColorScheme()) {
            assertThat(
                    ButtonDefaults.buttonColors(
                        containerColor = Color.Unspecified,
                        contentColor = Color.Unspecified,
                        disabledContainerColor = Color.Unspecified,
                        disabledContentColor = Color.Unspecified,
                    )
                )
                .isEqualTo(
                    ButtonColors(
                        containerColor = FilledButtonTokens.ContainerColor.value,
                        contentColor = FilledButtonTokens.LabelTextColor.value,
                        disabledContainerColor =
                            FilledButtonTokens.DisabledContainerColor.value.copy(
                                FilledButtonTokens.DisabledContainerOpacity
                            ),
                        disabledContentColor =
                            FilledButtonTokens.DisabledLabelTextColor.value.copy(
                                alpha = FilledButtonTokens.DisabledLabelTextOpacity
                            ),
                    )
                )
        }
    }

    @Test
    fun filledTonalButton_defaultColors() {
        rule.setMaterialContent(lightColorScheme()) {
            assertThat(
                    ButtonDefaults.filledTonalButtonColors(
                        containerColor = Color.Unspecified,
                        contentColor = Color.Unspecified,
                        disabledContainerColor = Color.Unspecified,
                        disabledContentColor = Color.Unspecified,
                    )
                )
                .isEqualTo(
                    ButtonColors(
                        containerColor = FilledTonalButtonTokens.ContainerColor.value,
                        contentColor = FilledTonalButtonTokens.LabelTextColor.value,
                        disabledContainerColor =
                            FilledTonalButtonTokens.DisabledContainerColor.value.copy(
                                alpha = FilledTonalButtonTokens.DisabledContainerOpacity
                            ),
                        disabledContentColor =
                            FilledTonalButtonTokens.DisabledLabelTextColor.value.copy(
                                alpha = FilledTonalButtonTokens.DisabledLabelTextOpacity
                            ),
                    )
                )
        }
    }

    @Test
    fun elevatedButton_defaultColors() {
        rule.setMaterialContent(lightColorScheme()) {
            assertThat(
                    ButtonDefaults.elevatedButtonColors(
                        containerColor = Color.Unspecified,
                        contentColor = Color.Unspecified,
                        disabledContainerColor = Color.Unspecified,
                        disabledContentColor = Color.Unspecified,
                    )
                )
                .isEqualTo(
                    ButtonColors(
                        containerColor = ElevatedButtonTokens.ContainerColor.value,
                        contentColor = ElevatedButtonTokens.LabelTextColor.value,
                        disabledContainerColor =
                            ElevatedButtonTokens.DisabledContainerColor.value.copy(
                                alpha = ElevatedButtonTokens.DisabledContainerOpacity
                            ),
                        disabledContentColor =
                            ElevatedButtonTokens.DisabledLabelTextColor.value.copy(
                                alpha = ElevatedButtonTokens.DisabledLabelTextOpacity
                            ),
                    )
                )
        }
    }

    @Test
    fun outlinedButton_defaultColors() {
        rule.setMaterialContent(lightColorScheme()) {
            assertThat(
                    ButtonDefaults.outlinedButtonColors(
                        containerColor = Color.Unspecified,
                        contentColor = Color.Unspecified,
                        disabledContainerColor = Color.Unspecified,
                        disabledContentColor = Color.Unspecified,
                    )
                )
                .isEqualTo(
                    ButtonColors(
                        containerColor = Color.Transparent,
                        contentColor = OutlinedButtonTokens.LabelTextColor.value,
                        disabledContainerColor = Color.Transparent,
                        disabledContentColor =
                            OutlinedButtonTokens.DisabledLabelTextColor.value.copy(
                                alpha = OutlinedButtonTokens.DisabledLabelTextOpacity
                            ),
                    )
                )
        }
    }

    @Test
    fun textButton_defaultColors() {
        rule.setMaterialContent(lightColorScheme()) {
            assertThat(
                    ButtonDefaults.textButtonColors(
                        containerColor = Color.Unspecified,
                        contentColor = Color.Unspecified,
                        disabledContainerColor = Color.Unspecified,
                        disabledContentColor = Color.Unspecified,
                    )
                )
                .isEqualTo(
                    ButtonColors(
                        containerColor = Color.Transparent,
                        // TODO change this back to the TextButtonTokens.LabelColor once the tokens
                        // are updated
                        contentColor = MaterialTheme.colorScheme.primary,
                        disabledContainerColor = Color.Transparent,
                        disabledContentColor =
                            TextButtonTokens.DisabledLabelColor.value.copy(
                                alpha = TextButtonTokens.DisabledLabelOpacity
                            ),
                    )
                )
        }
    }

    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    @Test
    fun button_xSmall_positioning() {
        var expectedStartPadding: Dp = 0.dp
        var expectedEndPadding: Dp = 0.dp
        val size = ButtonDefaults.ExtraSmallContainerHeight
        rule.setMaterialContent(lightColorScheme()) {
            val layoutDirection = LocalLayoutDirection.current
            expectedStartPadding =
                ButtonDefaults.ExtraSmallContentPadding.calculateStartPadding(layoutDirection)
            expectedEndPadding =
                ButtonDefaults.ExtraSmallContentPadding.calculateEndPadding(layoutDirection)
            Box {
                Button(
                    onClick = { /* Do something! */ },
                    modifier = Modifier.heightIn(size).testTag(ButtonTestTag),
                    contentPadding = ButtonDefaults.contentPaddingFor(size),
                ) {
                    Icon(
                        Icons.Filled.Edit,
                        contentDescription = "Localized description",
                        modifier =
                            Modifier.size(ButtonDefaults.iconSizeFor(size))
                                .testTag(IconTestTag)
                                .semantics(mergeDescendants = true) {},
                    )
                    Spacer(Modifier.size(ButtonDefaults.iconSpacingFor(size)))
                    Text(
                        "Label",
                        modifier =
                            Modifier.testTag(TextTestTag).semantics(mergeDescendants = true) {},
                    )
                }
            }
        }

        val buttonBounds = rule.onNodeWithTag(ButtonTestTag).getUnclippedBoundsInRoot()
        val iconBounds = rule.onNodeWithTag(IconTestTag).getUnclippedBoundsInRoot()
        val textBounds = rule.onNodeWithTag(TextTestTag).getUnclippedBoundsInRoot()

        (iconBounds.left - buttonBounds.left).assertIsEqualTo(
            expectedStartPadding,
            "padding between start of button and start of icon",
        )
        (textBounds.left - iconBounds.right).assertIsEqualTo(
            ButtonDefaults.ExtraSmallIconSpacing,
            "spacing between icon and label",
        )
        (buttonBounds.right - textBounds.right).assertIsEqualTo(
            expectedEndPadding,
            "padding between end of label and end of button",
        )
        buttonBounds.height.assertIsEqualTo(
            ButtonDefaults.ExtraSmallContainerHeight,
            "height of button",
        )
    }

    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    @Test
    fun button_medium_positioning() {
        var expectedStartPadding: Dp = 0.dp
        var expectedEndPadding: Dp = 0.dp
        val size = ButtonDefaults.MediumContainerHeight
        rule.setMaterialContent(lightColorScheme()) {
            val layoutDirection = LocalLayoutDirection.current
            expectedStartPadding =
                ButtonDefaults.MediumContentPadding.calculateStartPadding(layoutDirection)
            expectedEndPadding =
                ButtonDefaults.MediumContentPadding.calculateEndPadding(layoutDirection)
            Box {
                Button(
                    onClick = { /* Do something! */ },
                    modifier = Modifier.heightIn(size).testTag(ButtonTestTag),
                    contentPadding = ButtonDefaults.contentPaddingFor(size),
                ) {
                    Icon(
                        Icons.Filled.Edit,
                        contentDescription = "Localized description",
                        modifier =
                            Modifier.size(ButtonDefaults.iconSizeFor(size))
                                .testTag(IconTestTag)
                                .semantics(mergeDescendants = true) {},
                    )
                    Spacer(Modifier.size(ButtonDefaults.iconSpacingFor(size)))
                    Text(
                        "Label",
                        modifier =
                            Modifier.testTag(TextTestTag).semantics(mergeDescendants = true) {},
                    )
                }
            }
        }

        val buttonBounds = rule.onNodeWithTag(ButtonTestTag).getUnclippedBoundsInRoot()
        val iconBounds = rule.onNodeWithTag(IconTestTag).getUnclippedBoundsInRoot()
        val textBounds = rule.onNodeWithTag(TextTestTag).getUnclippedBoundsInRoot()

        (iconBounds.left - buttonBounds.left).assertIsEqualTo(
            expectedStartPadding,
            "padding between start of button and start of icon",
        )
        (textBounds.left - iconBounds.right).assertIsEqualTo(
            ButtonDefaults.MediumIconSpacing,
            "spacing between icon and label",
        )
        (buttonBounds.right - textBounds.right).assertIsEqualTo(
            expectedEndPadding,
            "padding between end of label and end of button",
        )
        buttonBounds.height.assertIsEqualTo(
            ButtonDefaults.MediumContainerHeight,
            "height of button",
        )
    }

    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    @Test
    fun button_large_positioning() {
        var expectedStartPadding: Dp = 0.dp
        var expectedEndPadding: Dp = 0.dp
        val size = ButtonDefaults.LargeContainerHeight
        rule.setMaterialContent(lightColorScheme()) {
            val layoutDirection = LocalLayoutDirection.current
            expectedStartPadding =
                ButtonDefaults.LargeContentPadding.calculateStartPadding(layoutDirection)
            expectedEndPadding =
                ButtonDefaults.LargeContentPadding.calculateEndPadding(layoutDirection)
            Box {
                Button(
                    onClick = { /* Do something! */ },
                    modifier = Modifier.heightIn(size).testTag(ButtonTestTag),
                    contentPadding = ButtonDefaults.contentPaddingFor(size),
                ) {
                    Icon(
                        Icons.Filled.Edit,
                        contentDescription = "Localized description",
                        modifier =
                            Modifier.size(ButtonDefaults.iconSizeFor(size))
                                .testTag(IconTestTag)
                                .semantics(mergeDescendants = true) {},
                    )
                    Spacer(Modifier.size(ButtonDefaults.iconSpacingFor(size)))
                    Text(
                        "Label",
                        modifier =
                            Modifier.testTag(TextTestTag).semantics(mergeDescendants = true) {},
                    )
                }
            }
        }

        val buttonBounds = rule.onNodeWithTag(ButtonTestTag).getUnclippedBoundsInRoot()
        val iconBounds = rule.onNodeWithTag(IconTestTag).getUnclippedBoundsInRoot()
        val textBounds = rule.onNodeWithTag(TextTestTag).getUnclippedBoundsInRoot()

        (iconBounds.left - buttonBounds.left).assertIsEqualTo(
            expectedStartPadding,
            "padding between start of button and start of icon",
        )
        (textBounds.left - iconBounds.right).assertIsEqualTo(
            ButtonDefaults.LargeIconSpacing,
            "spacing between icon and label",
        )
        (buttonBounds.right - textBounds.right).assertIsEqualTo(
            expectedEndPadding,
            "padding between end of label and end of button",
        )
        buttonBounds.height.assertIsEqualTo(ButtonDefaults.LargeContainerHeight, "height of button")
    }

    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    @Test
    fun button_xLarge_positioning() {
        var expectedStartPadding: Dp = 0.dp
        var expectedEndPadding: Dp = 0.dp
        val size = ButtonDefaults.ExtraLargeContainerHeight
        rule.setMaterialContent(lightColorScheme()) {
            val layoutDirection = LocalLayoutDirection.current
            expectedStartPadding =
                ButtonDefaults.ExtraLargeContentPadding.calculateStartPadding(layoutDirection)
            expectedEndPadding =
                ButtonDefaults.ExtraLargeContentPadding.calculateEndPadding(layoutDirection)
            Box {
                Button(
                    onClick = { /* Do something! */ },
                    modifier = Modifier.heightIn(size).testTag(ButtonTestTag),
                    contentPadding = ButtonDefaults.contentPaddingFor(size),
                ) {
                    Icon(
                        Icons.Filled.Edit,
                        contentDescription = "Localized description",
                        modifier =
                            Modifier.size(ButtonDefaults.iconSizeFor(size))
                                .testTag(IconTestTag)
                                .semantics(mergeDescendants = true) {},
                    )
                    Spacer(Modifier.size(ButtonDefaults.iconSpacingFor(size)))
                    Text(
                        "Label",
                        modifier =
                            Modifier.testTag(TextTestTag).semantics(mergeDescendants = true) {},
                    )
                }
            }
        }

        val buttonBounds = rule.onNodeWithTag(ButtonTestTag).getUnclippedBoundsInRoot()
        val iconBounds = rule.onNodeWithTag(IconTestTag).getUnclippedBoundsInRoot()
        val textBounds = rule.onNodeWithTag(TextTestTag).getUnclippedBoundsInRoot()

        (iconBounds.left - buttonBounds.left).assertIsEqualTo(
            expectedStartPadding,
            "padding between start of button and start of icon",
        )
        (textBounds.left - iconBounds.right).assertIsEqualTo(
            ButtonDefaults.ExtraLargeIconSpacing,
            "spacing between icon and label",
        )
        (buttonBounds.right - textBounds.right).assertIsEqualTo(
            expectedEndPadding,
            "padding between end of label and end of button",
        )
        buttonBounds.height.assertIsEqualTo(
            ButtonDefaults.ExtraLargeContainerHeight,
            "height of button",
        )
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    @Test
    fun button_withAnimatedShape_defaultShape() {
        lateinit var shape: Shape
        val backgroundColor = Color.Yellow
        val shapeColor = Color.Blue
        rule.setMaterialContent(lightColorScheme()) {
            shape = ButtonDefaults.shape
            Surface(color = backgroundColor) {
                Button(
                    onClick = {},
                    shapes = ButtonDefaults.shapes(),
                    modifier = Modifier.testTag(ButtonTestTag),
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = shapeColor,
                            contentColor = shapeColor,
                        ),
                ) {
                    Text("Button")
                }
            }
        }

        rule
            .onNodeWithTag(ButtonTestTag)
            .captureToImage()
            .assertShape(
                density = rule.density,
                shapeColor = shapeColor,
                backgroundColor = backgroundColor,
                shape = shape,
            )
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    @Test
    fun button_withAnimatedShape_pressedShape() {
        lateinit var shape: Shape
        val backgroundColor = Color.Yellow
        val shapeColor = Color.Blue
        rule.setMaterialContent(lightColorScheme()) {
            shape = ButtonDefaults.pressedShape
            Surface(color = backgroundColor) {
                Button(
                    onClick = {},
                    shapes = ButtonDefaults.shapes(),
                    modifier = Modifier.testTag(ButtonTestTag),
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = shapeColor,
                            contentColor = shapeColor,
                        ),
                ) {
                    Text("Button")
                }
            }
        }

        rule.onNodeWithTag(ButtonTestTag).performTouchInput { down(center) }

        rule
            .onNodeWithTag(ButtonTestTag)
            .captureToImage()
            .assertShape(
                density = rule.density,
                shapeColor = shapeColor,
                backgroundColor = backgroundColor,
                shape = shape,
            )
    }
}

private const val ButtonTestTag = "button"
private const val IconTestTag = "icon"
private const val TextTestTag = "text"
