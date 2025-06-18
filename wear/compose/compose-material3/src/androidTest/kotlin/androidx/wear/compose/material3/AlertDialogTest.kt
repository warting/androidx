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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.testutils.assertContainsColor
import androidx.compose.testutils.assertIsEqualTo
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.test.filters.SdkSuppress
import junit.framework.TestCase.assertEquals
import org.junit.Assert
import org.junit.Rule
import org.junit.Test

class AlertDialogTest {
    @get:Rule val rule = createComposeRule()

    @Test
    fun dialog_supports_testtag_with_bottomButton() {
        rule.setContentWithTheme {
            AlertDialog(
                visible = true,
                onDismissRequest = {},
                modifier = Modifier.testTag(TEST_TAG),
                title = {},
                edgeButton = {
                    AlertDialogDefaults.EdgeButton(
                        onClick = {},
                        modifier = Modifier.testTag(ConfirmButtonTestTag),
                    )
                },
            )
        }
        rule.onNodeWithTag(TEST_TAG).assertExists()
        rule.onNodeWithTag(ConfirmButtonTestTag).assertExists()
    }

    @Test
    fun dialog_supports_testtag_with_no_buttons() {
        rule.setContentWithTheme {
            AlertDialog(
                visible = true,
                onDismissRequest = {},
                modifier = Modifier.testTag(TEST_TAG),
                title = {},
            )
        }
        rule.onNodeWithTag(TEST_TAG).assertExists()
    }

    @Test
    fun dialog_supports_testtag_with_confirmDismissButtons() {
        rule.setContentWithTheme {
            AlertDialog(
                visible = true,
                onDismissRequest = {},
                modifier = Modifier.testTag(TEST_TAG),
                title = {},
                confirmButton = {
                    AlertDialogDefaults.ConfirmButton(
                        onClick = {},
                        modifier = Modifier.testTag(ConfirmButtonTestTag),
                    )
                },
                dismissButton = {
                    AlertDialogDefaults.DismissButton(
                        onClick = {},
                        modifier = Modifier.testTag(DismissButtonTestTag),
                    )
                },
            )
        }
        rule.onNodeWithTag(TEST_TAG).assertExists()
        rule.onNodeWithTag(ConfirmButtonTestTag).assertExists()
        rule.onNodeWithTag(DismissButtonTestTag).assertExists()
    }

    @Test
    fun content_supports_testtag_with_bottomButton() {
        rule.setContentWithTheme {
            AlertDialogContent(
                modifier = Modifier.testTag(TEST_TAG),
                title = {},
                edgeButton = {
                    AlertDialogDefaults.EdgeButton(
                        onClick = {},
                        modifier = Modifier.testTag(ConfirmButtonTestTag),
                    )
                },
            )
        }
        rule.onNodeWithTag(TEST_TAG).assertExists()
        rule.onNodeWithTag(ConfirmButtonTestTag).assertExists()
    }

    @Test
    fun content_supports_testtag_with_no_buttons() {
        rule.setContentWithTheme {
            AlertDialogContent(modifier = Modifier.testTag(TEST_TAG), title = {})
        }
        rule.onNodeWithTag(TEST_TAG).assertExists()
    }

    @Test
    fun content_supports_testtag_with_confirmDismissButtons() {
        rule.setContentWithTheme {
            AlertDialogContent(
                modifier = Modifier.testTag(TEST_TAG),
                title = {},
                confirmButton = {
                    AlertDialogDefaults.ConfirmButton(
                        onClick = {},
                        modifier = Modifier.testTag(ConfirmButtonTestTag),
                    )
                },
                dismissButton = {
                    AlertDialogDefaults.DismissButton(
                        onClick = {},
                        modifier = Modifier.testTag(DismissButtonTestTag),
                    )
                },
            )
        }
        rule.onNodeWithTag(TEST_TAG).assertExists()
        rule.onNodeWithTag(ConfirmButtonTestTag).assertExists()
        rule.onNodeWithTag(DismissButtonTestTag).assertExists()
    }

    @Test
    fun displays_icon_with_bottomButton() {
        rule.setContentWithTheme {
            AlertDialogContent(icon = { TestImage(TEST_TAG) }, title = {}, edgeButton = {})
        }
        rule.onNodeWithTag(TEST_TAG).assertExists()
    }

    @Test
    fun displays_icon_with_confirmDismissButtons() {
        rule.setContentWithTheme {
            AlertDialogContent(
                icon = { TestImage(TEST_TAG) },
                title = {},
                confirmButton = {},
                dismissButton = {},
            )
        }
        rule.onNodeWithTag(TEST_TAG).assertExists()
    }

    @Test
    fun displays_title_with_bottomButton() {
        rule.setContentWithTheme {
            AlertDialogContent(
                title = { Text("Text", modifier = Modifier.testTag(TEST_TAG)) },
                edgeButton = {},
            )
        }
        rule.onNodeWithTag(TEST_TAG).assertExists()
    }

    @Test
    fun displays_title_with_confirmDismissButtons() {
        rule.setContentWithTheme {
            AlertDialogContent(
                title = { Text("Text", modifier = Modifier.testTag(TEST_TAG)) },
                confirmButton = {},
                dismissButton = {},
            )
        }
        rule.onNodeWithTag(TEST_TAG).assertExists()
    }

    @Test
    fun displays_messageText_with_bottomButton() {
        rule.setContentWithTheme {
            AlertDialogContent(
                title = {},
                text = { Text("Text", modifier = Modifier.testTag(TEST_TAG)) },
                edgeButton = {},
            )
        }
        rule.onNodeWithTag(TEST_TAG).assertExists()
    }

    @Test
    fun displays_messageText_with_confirmDismissButtons() {
        rule.setContentWithTheme {
            AlertDialogContent(
                title = {},
                text = { Text("Text", modifier = Modifier.testTag(TEST_TAG)) },
                confirmButton = {},
                dismissButton = {},
            )
        }
        rule.onNodeWithTag(TEST_TAG).assertExists()
    }

    @Test
    fun displays_content_with_bottomButton() {
        rule.setContentWithTheme {
            AlertDialogContent(title = {}, edgeButton = {}) {
                item { Text("Text", modifier = Modifier.testTag(TEST_TAG)) }
            }
        }
        rule.onNodeWithTag(TEST_TAG).assertExists()
    }

    @Test
    fun displays_content_with_confirmDismissButtons() {
        rule.setContentWithTheme {
            AlertDialogContent(title = {}, confirmButton = {}, dismissButton = {}) {
                item { Text("Text", modifier = Modifier.testTag(TEST_TAG)) }
            }
        }
        rule.onNodeWithTag(TEST_TAG).assertExists()
    }

    @Test
    fun displays_confirmButton() {
        rule.setContentWithTheme {
            AlertDialogContent(
                title = {},
                confirmButton = { Button(onClick = {}, modifier = Modifier.testTag(TEST_TAG)) {} },
                dismissButton = {},
            )
        }
        rule.onNodeWithTag(TEST_TAG).assertExists()
    }

    @Test
    fun displays_dismissButton() {
        rule.setContentWithTheme {
            AlertDialogContent(
                title = {},
                confirmButton = {},
                dismissButton = { Button(onClick = {}, modifier = Modifier.testTag(TEST_TAG)) {} },
            )
        }
        rule.onNodeWithTag(TEST_TAG).assertExists()
    }

    @Test
    fun displays_bottomButton() {
        rule.setContentWithTheme {
            AlertDialogContent(
                title = {},
                edgeButton = { Button(onClick = {}, modifier = Modifier.testTag(TEST_TAG)) {} },
            )
        }
        rule.onNodeWithTag(TEST_TAG).assertExists()
    }

    @Test
    fun supports_swipeToDismiss_confirmDismissButtons() {
        var dismissCounter = 0
        rule.setContentWithTheme {
            var showDialog by remember { mutableStateOf(true) }
            AlertDialog(
                modifier = Modifier.testTag(TEST_TAG),
                title = {},
                dismissButton = {},
                confirmButton = {},
                onDismissRequest = {
                    showDialog = false
                    dismissCounter++
                },
                visible = showDialog,
            )
        }

        rule.onNodeWithTag(TEST_TAG).performTouchInput({ swipeRight() })
        rule.onNodeWithTag(TEST_TAG).assertDoesNotExist()
        Assert.assertEquals(1, dismissCounter)
    }

    @Test
    fun supports_swipeToDismiss_bottomButton() {
        var dismissCounter = 0
        rule.setContentWithTheme {
            var showDialog by remember { mutableStateOf(true) }
            AlertDialog(
                modifier = Modifier.testTag(TEST_TAG),
                title = {},
                edgeButton = {},
                onDismissRequest = {
                    showDialog = false
                    dismissCounter++
                },
                visible = showDialog,
            )
        }

        rule.onNodeWithTag(TEST_TAG).performTouchInput({ swipeRight() })
        rule.onNodeWithTag(TEST_TAG).assertDoesNotExist()
        Assert.assertEquals(1, dismissCounter)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun onDismissRequest_not_called_when_hidden() {
        val show = mutableStateOf(true)
        var dismissCounter = 0
        rule.setContentWithTheme {
            AlertDialog(
                modifier = Modifier.testTag(TEST_TAG),
                title = {},
                edgeButton = {},
                onDismissRequest = { dismissCounter++ },
                visible = show.value,
            )
        }
        rule.waitForIdle()
        show.value = false
        rule.waitUntilDoesNotExist(hasTestTag(TEST_TAG))
        Assert.assertEquals(0, dismissCounter)
    }

    @Test
    fun hides_dialog_when_show_false() {
        rule.setContentWithTheme {
            AlertDialog(
                modifier = Modifier.testTag(TEST_TAG),
                title = {},
                edgeButton = {},
                onDismissRequest = {},
                visible = false,
            )
        }
        rule.onNodeWithTag(TEST_TAG).assertDoesNotExist()
    }

    @Test
    fun applies_correct_titleProperties() {
        var expectedContentColor: Color = Color.Unspecified
        var expectedTextStyle: TextStyle = TextStyle.Default
        var expectedTextAlign: TextAlign? = null
        var expectedTextMaxLines = 0

        var actualContentColor: Color = Color.Unspecified
        var actualTextStyle: TextStyle = TextStyle.Default
        var actualTextAlign: TextAlign? = TextAlign.Unspecified
        var actualTextMaxLines = 0

        rule.setContentWithTheme {
            expectedContentColor = MaterialTheme.colorScheme.onBackground
            expectedTextStyle = MaterialTheme.typography.titleMedium
            expectedTextAlign = TextAlign.Center
            expectedTextMaxLines = AlertTitleMaxLines
            AlertDialogContent(
                modifier = Modifier.testTag(TEST_TAG),
                title = {
                    Text("Title")
                    actualContentColor = LocalContentColor.current
                    actualTextStyle = LocalTextStyle.current
                    actualTextAlign = LocalTextConfiguration.current.textAlign
                    actualTextMaxLines = LocalTextConfiguration.current.maxLines
                },
                edgeButton = {},
            )
        }

        assertEquals(expectedContentColor, actualContentColor)
        assertEquals(expectedTextStyle, actualTextStyle)
        assertEquals(expectedTextAlign, actualTextAlign)
        assertEquals(expectedTextMaxLines, actualTextMaxLines)
    }

    @Test
    fun applies_correct_textMessage_properties() {
        var expectedContentColor: Color = Color.Unspecified
        var expectedTextStyle: TextStyle = TextStyle.Default
        var expectedTextAlign: TextAlign? = null

        var actualContentColor: Color = Color.Unspecified
        var actualTextStyle: TextStyle = TextStyle.Default
        var actualTextAlign: TextAlign? = TextAlign.Unspecified

        rule.setContentWithTheme {
            expectedContentColor = MaterialTheme.colorScheme.onBackground
            expectedTextStyle = MaterialTheme.typography.bodyMedium
            expectedTextAlign = TextAlign.Center
            AlertDialogContent(
                modifier = Modifier.testTag(TEST_TAG),
                title = {},
                text = {
                    Text("Text")
                    actualContentColor = LocalContentColor.current
                    actualTextStyle = LocalTextStyle.current
                    actualTextAlign = LocalTextConfiguration.current.textAlign
                },
                edgeButton = {},
            )
        }

        assertEquals(expectedContentColor, actualContentColor)
        assertEquals(expectedTextStyle, actualTextStyle)
        assertEquals(expectedTextAlign, actualTextAlign)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.P)
    @Test
    fun override_customTitleColor() {
        var expectedContentColor: Color = Color.Unspecified

        rule.setContentWithTheme {
            expectedContentColor = Color.Yellow
            AlertDialogContent(
                modifier = Modifier.testTag(TEST_TAG),
                title = { Text("Title", color = expectedContentColor) },
                edgeButton = {},
            )
        }
        rule.onNodeWithTag(TEST_TAG).captureToImage().assertContainsColor(expectedContentColor)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.P)
    @Test
    fun override_customTextMessage_color() {
        var expectedContentColor: Color = Color.Unspecified

        rule.setContentWithTheme {
            expectedContentColor = Color.Yellow
            AlertDialogContent(
                modifier = Modifier.testTag(TEST_TAG),
                title = {},
                text = { Text("Text", color = expectedContentColor) },
                edgeButton = {},
            )
        }
        rule.onNodeWithTag(TEST_TAG).captureToImage().assertContainsColor(expectedContentColor)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.P)
    @Test
    fun override_customBackground_color() {
        var expectedBackgroundColor: Color = Color.Unspecified

        rule.setContentWithTheme {
            expectedBackgroundColor = Color.Yellow
            AlertDialogContent(
                modifier = Modifier.testTag(TEST_TAG).background(expectedBackgroundColor),
                title = {},
                edgeButton = {},
            )
        }
        rule.onNodeWithTag(TEST_TAG).captureToImage().assertContainsColor(expectedBackgroundColor)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.P)
    @Test
    fun override_confirmDismissButton_colors() {
        var expectedConfirmColor: Color = Color.Unspecified
        var expectedDismissColor: Color = Color.Unspecified

        rule.setContentWithTheme {
            expectedConfirmColor = Color.Yellow
            expectedDismissColor = Color.Red
            AlertDialogContent(
                modifier = Modifier.testTag(TEST_TAG),
                title = {},
                confirmButton = {
                    AlertDialogDefaults.ConfirmButton(
                        onClick = {},
                        colors =
                            IconButtonDefaults.filledIconButtonColors(
                                containerColor = expectedConfirmColor
                            ),
                    )
                },
                dismissButton = {
                    AlertDialogDefaults.DismissButton(
                        onClick = {},
                        colors =
                            IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = expectedDismissColor
                            ),
                    )
                },
            )
        }
        rule
            .onNodeWithTag(TEST_TAG)
            .captureToImage()
            .assertContainsColor(expectedConfirmColor)
            .assertContainsColor(expectedDismissColor)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.P)
    @Test
    fun override_edgeButton_color() {
        var expectedEdgeButtonColor: Color = Color.Unspecified

        rule.setContentWithTheme {
            expectedEdgeButtonColor = Color.Yellow
            AlertDialogContent(
                modifier = Modifier.testTag(TEST_TAG),
                title = {},
                edgeButton = {
                    AlertDialogDefaults.EdgeButton(
                        onClick = {},
                        colors =
                            ButtonDefaults.buttonColors(containerColor = expectedEdgeButtonColor),
                    )
                },
            )
        }
        rule.onNodeWithTag(TEST_TAG).captureToImage().assertContainsColor(expectedEdgeButtonColor)
    }

    @Test
    fun with_title_confirmDismissButtons_positioning() {
        rule.setContentWithThemeForSizeAssertions(useUnmergedTree = true) {
            ScreenConfiguration(AlertScreenSize) {
                AlertDialogContent(
                    title = { Text("Title", modifier = Modifier.testTag(TitleTestTag)) },
                    confirmButton = {
                        Button(onClick = {}, modifier = Modifier.testTag(ConfirmButtonTestTag)) {}
                    },
                    dismissButton = {
                        Button(onClick = {}, modifier = Modifier.testTag(DismissButtonTestTag)) {}
                    },
                    verticalArrangement =
                        Arrangement.spacedBy(space = 0.dp, alignment = Alignment.CenterVertically),
                    modifier = Modifier.size(AlertScreenSize.dp).testTag(TEST_TAG),
                )
            }
        }

        val titleBottom = rule.onNodeWithTag(TitleTestTag).getUnclippedBoundsInRoot().bottom
        val confirmButtonTop =
            rule.onNodeWithTag(ConfirmButtonTestTag).getUnclippedBoundsInRoot().top
        confirmButtonTop.assertIsEqualTo(titleBottom + ConfirmDismissButtonsTopSpacing)
    }

    @Test
    fun with_title_noBottomButton_positioning() {
        rule.setContentWithThemeForSizeAssertions(useUnmergedTree = true) {
            ScreenConfiguration(SmallScreenSize) {
                AlertDialogContent(
                    title = { Text("Title") },
                    verticalArrangement =
                        Arrangement.spacedBy(space = 0.dp, alignment = Alignment.CenterVertically),
                    modifier = Modifier.size(SmallScreenSize.dp).testTag(TEST_TAG),
                ) {
                    item {
                        Text(
                            "ContentText",
                            // We set height larger than the screen size to be sure that the list
                            // will be scrollable
                            modifier =
                                Modifier.size(width = 100.dp, height = (SmallScreenSize + 50).dp)
                                    .testTag(ContentTestTag),
                        )
                    }
                }
            }
        }
        rule.onNodeWithTag(TEST_TAG).performTouchInput { swipeUp() }

        val contentBottom = rule.onNodeWithTag(ContentTestTag).getUnclippedBoundsInRoot().bottom
        val alertDialogBottom = rule.onNodeWithTag(TEST_TAG).getUnclippedBoundsInRoot().bottom
        // Assert that there is a proper padding between the bottom of the content and the bottom of
        // the dialog.
        contentBottom.assertIsEqualTo(
            alertDialogBottom * (1 - AlertDialogDefaults.noEdgeButtonBottomPaddingFraction),
            tolerance = Dp(0.55f),
        )
    }

    @Test
    fun with_icon_title_confirmDismissButtons_positioning() {
        rule.setContentWithThemeForSizeAssertions(useUnmergedTree = true) {
            ScreenConfiguration(AlertScreenSize) {
                AlertDialogContent(
                    icon = { TestImage(IconTestTag) },
                    title = { Text("Title", modifier = Modifier.testTag(TitleTestTag)) },
                    confirmButton = {
                        Button(onClick = {}, modifier = Modifier.testTag(ConfirmButtonTestTag)) {}
                    },
                    dismissButton = {
                        Button(onClick = {}, modifier = Modifier.testTag(DismissButtonTestTag)) {}
                    },
                    verticalArrangement =
                        Arrangement.spacedBy(space = 0.dp, alignment = Alignment.CenterVertically),
                    modifier = Modifier.size(AlertScreenSize.dp).testTag(TEST_TAG),
                )
            }
        }

        val iconBottom = rule.onNodeWithTag(IconTestTag).getUnclippedBoundsInRoot().bottom
        val titleTop = rule.onNodeWithTag(TitleTestTag).getUnclippedBoundsInRoot().top
        val titleBottom = rule.onNodeWithTag(TitleTestTag).getUnclippedBoundsInRoot().bottom
        val confirmButtonTop =
            rule.onNodeWithTag(ConfirmButtonTestTag).getUnclippedBoundsInRoot().top

        titleTop.assertIsEqualTo(iconBottom + AlertIconBottomSpacing)
        confirmButtonTop.assertIsEqualTo(titleBottom + ConfirmDismissButtonsTopSpacing)
    }

    @Test
    fun with_icon_title_textMessage_confirmDismissButtons_positioning() {
        rule.setContentWithThemeForSizeAssertions(useUnmergedTree = true) {
            ScreenConfiguration(AlertScreenSize) {
                AlertDialogContent(
                    icon = { TestImage(IconTestTag) },
                    title = { Text("Title", modifier = Modifier.testTag(TitleTestTag)) },
                    text = { Text("Text", modifier = Modifier.testTag(TextTestTag)) },
                    confirmButton = {
                        Button(onClick = {}, modifier = Modifier.testTag(ConfirmButtonTestTag)) {}
                    },
                    dismissButton = {
                        Button(onClick = {}, modifier = Modifier.testTag(DismissButtonTestTag)) {}
                    },
                    verticalArrangement =
                        Arrangement.spacedBy(space = 0.dp, alignment = Alignment.CenterVertically),
                    modifier = Modifier.size(AlertScreenSize.dp).testTag(TEST_TAG),
                )
            }
        }

        val iconBottom = rule.onNodeWithTag(IconTestTag).getUnclippedBoundsInRoot().bottom
        val titleTop = rule.onNodeWithTag(TitleTestTag).getUnclippedBoundsInRoot().top
        val titleBottom = rule.onNodeWithTag(TitleTestTag).getUnclippedBoundsInRoot().bottom
        val textTop = rule.onNodeWithTag(TextTestTag).getUnclippedBoundsInRoot().top
        val textBottom = rule.onNodeWithTag(TextTestTag).getUnclippedBoundsInRoot().bottom
        val confirmButtonTop =
            rule.onNodeWithTag(ConfirmButtonTestTag).getUnclippedBoundsInRoot().top

        titleTop.assertIsEqualTo(iconBottom + AlertIconBottomSpacing)
        textTop.assertIsEqualTo(titleBottom + AlertTextMessageTopSpacing)
        confirmButtonTop.assertIsEqualTo(textBottom + ConfirmDismissButtonsTopSpacing)
    }

    @Test
    fun with_icon_title_textMessage_content_confirmDismissButtons_positioning() {
        rule.setContentWithThemeForSizeAssertions(useUnmergedTree = true) {
            ScreenConfiguration(AlertScreenSize) {
                AlertDialogContent(
                    icon = { TestImage(IconTestTag) },
                    title = { Text("Title", modifier = Modifier.testTag(TitleTestTag)) },
                    text = { Text("Text", modifier = Modifier.testTag(TextTestTag)) },
                    confirmButton = {
                        Button(onClick = {}, modifier = Modifier.testTag(ConfirmButtonTestTag)) {}
                    },
                    dismissButton = {
                        Button(onClick = {}, modifier = Modifier.testTag(DismissButtonTestTag)) {}
                    },
                    verticalArrangement =
                        Arrangement.spacedBy(space = 0.dp, alignment = Alignment.CenterVertically),
                    modifier = Modifier.size(AlertScreenSize.dp).testTag(TEST_TAG),
                ) {
                    item { Text("ContentText", modifier = Modifier.testTag(ContentTestTag)) }
                }
            }
        }

        val iconBottom = rule.onNodeWithTag(IconTestTag).getUnclippedBoundsInRoot().bottom
        val titleTop = rule.onNodeWithTag(TitleTestTag).getUnclippedBoundsInRoot().top
        val titleBottom = rule.onNodeWithTag(TitleTestTag).getUnclippedBoundsInRoot().bottom
        val textTop = rule.onNodeWithTag(TextTestTag).getUnclippedBoundsInRoot().top
        val textBottom = rule.onNodeWithTag(TextTestTag).getUnclippedBoundsInRoot().bottom
        val contentTop = rule.onNodeWithTag(ContentTestTag).getUnclippedBoundsInRoot().top
        val contentBottom = rule.onNodeWithTag(ContentTestTag).getUnclippedBoundsInRoot().bottom
        val confirmButtonTop =
            rule.onNodeWithTag(ConfirmButtonTestTag).getUnclippedBoundsInRoot().top

        titleTop.assertIsEqualTo(iconBottom + AlertIconBottomSpacing)
        textTop.assertIsEqualTo(titleBottom + AlertTextMessageTopSpacing)
        contentTop.assertIsEqualTo(textBottom + AlertContentTopSpacing)
        confirmButtonTop.assertIsEqualTo(contentBottom + ConfirmDismissButtonsTopSpacing)
    }

    @Test
    fun with_icon_title_content_confirmDismissButtons_positioning() {
        rule.setContentWithThemeForSizeAssertions(useUnmergedTree = true) {
            ScreenConfiguration(AlertScreenSize) {
                AlertDialogContent(
                    icon = { TestImage(IconTestTag) },
                    title = { Box(modifier = Modifier.size(3.dp).testTag(TitleTestTag)) },
                    confirmButton = {
                        Button(onClick = {}, modifier = Modifier.testTag(ConfirmButtonTestTag)) {}
                    },
                    dismissButton = {
                        Button(onClick = {}, modifier = Modifier.testTag(DismissButtonTestTag)) {}
                    },
                    verticalArrangement =
                        Arrangement.spacedBy(space = 0.dp, alignment = Alignment.CenterVertically),
                    modifier = Modifier.size(AlertScreenSize.dp).testTag(TEST_TAG),
                ) {
                    item { Text("ContentText", modifier = Modifier.testTag(ContentTestTag)) }
                }
            }
        }

        val iconBottom = rule.onNodeWithTag(IconTestTag).getUnclippedBoundsInRoot().bottom
        val titleTop = rule.onNodeWithTag(TitleTestTag).getUnclippedBoundsInRoot().top
        val titleBottom = rule.onNodeWithTag(TitleTestTag).getUnclippedBoundsInRoot().bottom
        val contentTop = rule.onNodeWithTag(ContentTestTag).getUnclippedBoundsInRoot().top
        val contentBottom = rule.onNodeWithTag(ContentTestTag).getUnclippedBoundsInRoot().bottom
        val confirmButtonTop =
            rule.onNodeWithTag(ConfirmButtonTestTag).getUnclippedBoundsInRoot().top

        titleTop.assertIsEqualTo(iconBottom + AlertIconBottomSpacing)
        contentTop.assertIsEqualTo(titleBottom + AlertContentTopSpacing)
        confirmButtonTop.assertIsEqualTo(contentBottom + ConfirmDismissButtonsTopSpacing)
    }

    // TODO: add more positioning tests for EdgeButton.
}

private const val IconTestTag = "icon"
private const val TitleTestTag = "title"
private const val TextTestTag = "text"
private const val ContentTestTag = "content"
private const val ConfirmButtonTestTag = "confirmButton"
private const val DismissButtonTestTag = "dismissButton"
private const val AlertScreenSize = 400
private const val SmallScreenSize = 100
