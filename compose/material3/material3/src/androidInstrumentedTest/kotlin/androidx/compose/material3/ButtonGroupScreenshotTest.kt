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

package androidx.compose.material3

import android.os.Build
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.testutils.assertAgainstGolden
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.test.screenshot.AndroidXScreenshotTestRule
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
class ButtonGroupScreenshotTest {
    @get:Rule val rule = createComposeRule()

    @get:Rule val screenshotRule = AndroidXScreenshotTestRule(GOLDEN_MATERIAL3)

    private val wrapperTestTag = "WrapperTestTag"
    private val aButton = "AButton"
    private val bButton = "BButton"
    private val cButton = "CButton"
    private val dButton = "DButton"
    private val eButton = "EButton"

    @Test
    fun buttonGroup_lightTheme() {
        rule.setMaterialContent(lightColorScheme()) {
            Box(Modifier.testTag(wrapperTestTag)) {
                ButtonGroup {
                    Button(onClick = {}) { Text("A") }
                    Button(onClick = {}) { Text("B") }
                    Button(onClick = {}) { Text("C") }
                    Button(onClick = {}) { Text("D") }
                    Button(onClick = {}) { Text("E") }
                }
            }
        }

        assertAgainstGolden("buttonGroup_lightTheme")
    }

    @Test
    fun buttonGroup_darkTheme() {
        rule.setMaterialContent(darkColorScheme()) {
            Box(Modifier.testTag(wrapperTestTag)) {
                ButtonGroup {
                    Button(onClick = {}) { Text("A") }
                    Button(onClick = {}) { Text("B") }
                    Button(onClick = {}) { Text("C") }
                    Button(onClick = {}) { Text("D") }
                    Button(onClick = {}) { Text("E") }
                }
            }
        }

        assertAgainstGolden("buttonGroup_darkTheme")
    }

    @Test
    fun connectedButtonGroup_lightTheme() {
        rule.setMaterialContent(lightColorScheme()) {
            Box(Modifier.testTag(wrapperTestTag)) {
                Row(
                    horizontalArrangement =
                        Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)
                ) {
                    ToggleButton(
                        checked = false,
                        onCheckedChange = { /* Do nothing */ },
                        shapes = ButtonGroupDefaults.connectedLeadingButtonShapes()
                    ) {
                        Text("Work")
                    }
                    ToggleButton(
                        checked = false,
                        onCheckedChange = { /* Do nothing */ },
                        shapes = ButtonGroupDefaults.connectedMiddleButtonShapes()
                    ) {
                        Text("Restaurant")
                    }
                    ToggleButton(
                        checked = false,
                        onCheckedChange = { /* Do nothing */ },
                        shapes = ButtonGroupDefaults.connectedTrailingButtonShapes()
                    ) {
                        Text("Coffee")
                    }
                }
            }
        }

        assertAgainstGolden("connectedButtonGroup_lightTheme")
    }

    @Test
    fun connectedButtonGroup_darkTheme() {
        rule.setMaterialContent(darkColorScheme()) {
            Box(Modifier.testTag(wrapperTestTag)) {
                Row(
                    horizontalArrangement =
                        Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)
                ) {
                    ToggleButton(
                        checked = false,
                        onCheckedChange = { /* Do nothing */ },
                        shapes = ButtonGroupDefaults.connectedLeadingButtonShapes()
                    ) {
                        Text("Work")
                    }
                    ToggleButton(
                        checked = false,
                        onCheckedChange = { /* Do nothing */ },
                        shapes = ButtonGroupDefaults.connectedMiddleButtonShapes()
                    ) {
                        Text("Restaurant")
                    }
                    ToggleButton(
                        checked = false,
                        onCheckedChange = { /* Do nothing */ },
                        shapes = ButtonGroupDefaults.connectedTrailingButtonShapes()
                    ) {
                        Text("Coffee")
                    }
                }
            }
        }

        assertAgainstGolden("connectedButtonGroup_darkTheme")
    }

    @Test
    fun connectedButtonGroup_startSelected_lightTheme() {
        rule.setMaterialContent(lightColorScheme()) {
            Box(Modifier.testTag(wrapperTestTag)) {
                Row(
                    horizontalArrangement =
                        Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)
                ) {
                    ToggleButton(
                        checked = true,
                        onCheckedChange = { /* Do nothing */ },
                        shapes = ButtonGroupDefaults.connectedLeadingButtonShapes()
                    ) {
                        Text("Work")
                    }
                    ToggleButton(
                        checked = false,
                        onCheckedChange = { /* Do nothing */ },
                        shapes = ButtonGroupDefaults.connectedMiddleButtonShapes()
                    ) {
                        Text("Restaurant")
                    }
                    ToggleButton(
                        checked = false,
                        onCheckedChange = { /* Do nothing */ },
                        shapes = ButtonGroupDefaults.connectedTrailingButtonShapes()
                    ) {
                        Text("Coffee")
                    }
                }
            }
        }

        assertAgainstGolden("connectedButtonGroup_startSelected_lightTheme")
    }

    @Test
    fun connectedButtonGroup_middleSelected_lightTheme() {
        rule.setMaterialContent(lightColorScheme()) {
            Box(Modifier.testTag(wrapperTestTag)) {
                Row(
                    horizontalArrangement =
                        Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)
                ) {
                    ToggleButton(
                        checked = false,
                        onCheckedChange = { /* Do nothing */ },
                        shapes = ButtonGroupDefaults.connectedLeadingButtonShapes()
                    ) {
                        Text("Work")
                    }
                    ToggleButton(
                        checked = true,
                        onCheckedChange = { /* Do nothing */ },
                        shapes = ButtonGroupDefaults.connectedMiddleButtonShapes()
                    ) {
                        Text("Restaurant")
                    }
                    ToggleButton(
                        checked = false,
                        onCheckedChange = { /* Do nothing */ },
                        shapes = ButtonGroupDefaults.connectedTrailingButtonShapes()
                    ) {
                        Text("Coffee")
                    }
                }
            }
        }

        assertAgainstGolden("connectedButtonGroup_middleSelected_lightTheme")
    }

    @Test
    fun connectedButtonGroup_endSelected_lightTheme() {
        rule.setMaterialContent(lightColorScheme()) {
            Box(Modifier.testTag(wrapperTestTag)) {
                Row(
                    horizontalArrangement =
                        Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)
                ) {
                    ToggleButton(
                        checked = false,
                        onCheckedChange = { /* Do nothing */ },
                        shapes = ButtonGroupDefaults.connectedLeadingButtonShapes()
                    ) {
                        Text("Work")
                    }
                    ToggleButton(
                        checked = false,
                        onCheckedChange = { /* Do nothing */ },
                        shapes = ButtonGroupDefaults.connectedMiddleButtonShapes()
                    ) {
                        Text("Restaurant")
                    }
                    ToggleButton(
                        checked = true,
                        onCheckedChange = { /* Do nothing */ },
                        shapes = ButtonGroupDefaults.connectedTrailingButtonShapes()
                    ) {
                        Text("Coffee")
                    }
                }
            }
        }

        assertAgainstGolden("connectedButtonGroup_endSelected_lightTheme")
    }

    @Test
    fun connectedButtonGroup_allSelected_lightTheme() {
        rule.setMaterialContent(lightColorScheme()) {
            Box(Modifier.testTag(wrapperTestTag)) {
                Row(
                    horizontalArrangement =
                        Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)
                ) {
                    ToggleButton(
                        checked = true,
                        onCheckedChange = { /* Do nothing */ },
                        shapes = ButtonGroupDefaults.connectedLeadingButtonShapes()
                    ) {
                        Text("Work")
                    }
                    ToggleButton(
                        checked = true,
                        onCheckedChange = { /* Do nothing */ },
                        shapes = ButtonGroupDefaults.connectedMiddleButtonShapes()
                    ) {
                        Text("Restaurant")
                    }
                    ToggleButton(
                        checked = true,
                        onCheckedChange = { /* Do nothing */ },
                        shapes = ButtonGroupDefaults.connectedTrailingButtonShapes()
                    ) {
                        Text("Coffee")
                    }
                }
            }
        }

        assertAgainstGolden("connectedButtonGroup_allSelected_lightTheme")
    }

    @Ignore
    @Test
    fun buttonGroup_firstPressed_lightTheme() {
        val interactionSources = List(5) { MutableInteractionSource() }
        rule.setMaterialContent(lightColorScheme()) {
            Box(Modifier.testTag(wrapperTestTag)) {
                ButtonGroup {
                    Button(
                        modifier = Modifier.animateWidth(interactionSources[0]).testTag(aButton),
                        interactionSource = interactionSources[0],
                        onClick = {}
                    ) {
                        Text("A")
                    }
                    Button(
                        modifier = Modifier.animateWidth(interactionSources[1]).testTag(bButton),
                        interactionSource = interactionSources[1],
                        onClick = {}
                    ) {
                        Text("B")
                    }
                    Button(
                        modifier = Modifier.animateWidth(interactionSources[2]).testTag(cButton),
                        interactionSource = interactionSources[2],
                        onClick = {}
                    ) {
                        Text("C")
                    }
                    Button(
                        modifier = Modifier.animateWidth(interactionSources[3]).testTag(dButton),
                        interactionSource = interactionSources[3],
                        onClick = {}
                    ) {
                        Text("D")
                    }
                    Button(
                        modifier = Modifier.animateWidth(interactionSources[4]).testTag(eButton),
                        interactionSource = interactionSources[4],
                        onClick = {}
                    ) {
                        Text("E")
                    }
                }
            }
        }

        rule.mainClock.autoAdvance = false
        rule.onNodeWithTag(aButton).performTouchInput { down(center) }

        rule.mainClock.advanceTimeByFrame()
        rule.waitForIdle() // Wait for measure
        rule.mainClock.advanceTimeBy(milliseconds = 200)

        // Ripples are drawn on the RenderThread, not the main (UI) thread, so we can't wait for
        // synchronization. Instead just wait until after the ripples are finished animating.
        Thread.sleep(300)

        assertAgainstGolden("buttonGroup_firstPressed_lightTheme")
    }

    @Ignore("b/355413615")
    @Test
    fun buttonGroup_secondPressed_lightTheme() {
        val interactionSources = List(5) { MutableInteractionSource() }
        rule.setMaterialContent(lightColorScheme()) {
            Box(Modifier.testTag(wrapperTestTag)) {
                ButtonGroup {
                    Button(
                        modifier = Modifier.animateWidth(interactionSources[0]).testTag(aButton),
                        interactionSource = interactionSources[0],
                        onClick = {}
                    ) {
                        Text("A")
                    }
                    Button(
                        modifier = Modifier.animateWidth(interactionSources[1]).testTag(bButton),
                        interactionSource = interactionSources[1],
                        onClick = {}
                    ) {
                        Text("B")
                    }
                    Button(
                        modifier = Modifier.animateWidth(interactionSources[2]).testTag(cButton),
                        interactionSource = interactionSources[2],
                        onClick = {}
                    ) {
                        Text("C")
                    }
                    Button(
                        modifier = Modifier.animateWidth(interactionSources[3]).testTag(dButton),
                        interactionSource = interactionSources[3],
                        onClick = {}
                    ) {
                        Text("D")
                    }
                    Button(
                        modifier = Modifier.animateWidth(interactionSources[4]).testTag(eButton),
                        interactionSource = interactionSources[4],
                        onClick = {}
                    ) {
                        Text("E")
                    }
                }
            }
        }

        rule.mainClock.autoAdvance = false
        rule.onNodeWithTag(bButton).performTouchInput { down(center) }

        rule.mainClock.advanceTimeByFrame()
        rule.waitForIdle() // Wait for measure
        rule.mainClock.advanceTimeBy(milliseconds = 200)

        // Ripples are drawn on the RenderThread, not the main (UI) thread, so we can't wait for
        // synchronization. Instead just wait until after the ripples are finished animating.
        Thread.sleep(300)

        assertAgainstGolden("buttonGroup_secondPressed_lightTheme")
    }

    @Ignore("b/355413615")
    @Test
    fun buttonGroup_thirdPressed_lightTheme() {
        val interactionSources = List(5) { MutableInteractionSource() }
        rule.setMaterialContent(lightColorScheme()) {
            Box(Modifier.testTag(wrapperTestTag)) {
                ButtonGroup {
                    Button(
                        modifier = Modifier.animateWidth(interactionSources[0]).testTag(aButton),
                        interactionSource = interactionSources[0],
                        onClick = {}
                    ) {
                        Text("A")
                    }
                    Button(
                        modifier = Modifier.animateWidth(interactionSources[1]).testTag(bButton),
                        interactionSource = interactionSources[1],
                        onClick = {}
                    ) {
                        Text("B")
                    }
                    Button(
                        modifier = Modifier.animateWidth(interactionSources[2]).testTag(cButton),
                        interactionSource = interactionSources[2],
                        onClick = {}
                    ) {
                        Text("C")
                    }
                    Button(
                        modifier = Modifier.animateWidth(interactionSources[3]).testTag(dButton),
                        interactionSource = interactionSources[3],
                        onClick = {}
                    ) {
                        Text("D")
                    }
                    Button(
                        modifier = Modifier.animateWidth(interactionSources[4]).testTag(eButton),
                        interactionSource = interactionSources[4],
                        onClick = {}
                    ) {
                        Text("E")
                    }
                }
            }
        }

        rule.mainClock.autoAdvance = false
        rule.onNodeWithTag(cButton).performTouchInput { down(center) }

        rule.mainClock.advanceTimeByFrame()
        rule.waitForIdle() // Wait for measure
        rule.mainClock.advanceTimeBy(milliseconds = 200)

        // Ripples are drawn on the RenderThread, not the main (UI) thread, so we can't wait for
        // synchronization. Instead just wait until after the ripples are finished animating.
        Thread.sleep(300)

        assertAgainstGolden("buttonGroup_thirdPressed_lightTheme")
    }

    @Ignore("b/355413615")
    @Test
    fun buttonGroup_fourthPressed_lightTheme() {
        val interactionSources = List(5) { MutableInteractionSource() }
        rule.setMaterialContent(lightColorScheme()) {
            Box(Modifier.testTag(wrapperTestTag)) {
                ButtonGroup {
                    Button(
                        modifier = Modifier.animateWidth(interactionSources[0]).testTag(aButton),
                        interactionSource = interactionSources[0],
                        onClick = {}
                    ) {
                        Text("A")
                    }
                    Button(
                        modifier = Modifier.animateWidth(interactionSources[1]).testTag(bButton),
                        interactionSource = interactionSources[1],
                        onClick = {}
                    ) {
                        Text("B")
                    }
                    Button(
                        modifier = Modifier.animateWidth(interactionSources[2]).testTag(cButton),
                        interactionSource = interactionSources[2],
                        onClick = {}
                    ) {
                        Text("C")
                    }
                    Button(
                        modifier = Modifier.animateWidth(interactionSources[3]).testTag(dButton),
                        interactionSource = interactionSources[3],
                        onClick = {}
                    ) {
                        Text("D")
                    }
                    Button(
                        modifier = Modifier.animateWidth(interactionSources[4]).testTag(eButton),
                        interactionSource = interactionSources[4],
                        onClick = {}
                    ) {
                        Text("E")
                    }
                }
            }
        }

        rule.mainClock.autoAdvance = false
        rule.onNodeWithTag(dButton).performTouchInput { down(center) }

        rule.mainClock.advanceTimeByFrame()
        rule.waitForIdle() // Wait for measure
        rule.mainClock.advanceTimeBy(milliseconds = 200)

        // Ripples are drawn on the RenderThread, not the main (UI) thread, so we can't wait for
        // synchronization. Instead just wait until after the ripples are finished animating.
        Thread.sleep(300)

        assertAgainstGolden("buttonGroup_fourthPressed_lightTheme")
    }

    @Ignore("b/355413615")
    @Test
    fun buttonGroup_fifthPressed_lightTheme() {
        val interactionSources = List(5) { MutableInteractionSource() }
        rule.setMaterialContent(lightColorScheme()) {
            Box(Modifier.testTag(wrapperTestTag)) {
                ButtonGroup {
                    Button(
                        modifier = Modifier.animateWidth(interactionSources[0]).testTag(aButton),
                        interactionSource = interactionSources[0],
                        onClick = {}
                    ) {
                        Text("A")
                    }
                    Button(
                        modifier = Modifier.animateWidth(interactionSources[1]).testTag(bButton),
                        interactionSource = interactionSources[1],
                        onClick = {}
                    ) {
                        Text("B")
                    }
                    Button(
                        modifier = Modifier.animateWidth(interactionSources[2]).testTag(cButton),
                        interactionSource = interactionSources[2],
                        onClick = {}
                    ) {
                        Text("C")
                    }
                    Button(
                        modifier = Modifier.animateWidth(interactionSources[3]).testTag(dButton),
                        interactionSource = interactionSources[3],
                        onClick = {}
                    ) {
                        Text("D")
                    }
                    Button(
                        modifier = Modifier.animateWidth(interactionSources[4]).testTag(eButton),
                        interactionSource = interactionSources[4],
                        onClick = {}
                    ) {
                        Text("E")
                    }
                }
            }
        }

        rule.mainClock.autoAdvance = false
        rule.onNodeWithTag(eButton).performTouchInput { down(center) }

        rule.mainClock.advanceTimeByFrame()
        rule.waitForIdle() // Wait for measure
        rule.mainClock.advanceTimeBy(milliseconds = 200)

        // Ripples are drawn on the RenderThread, not the main (UI) thread, so we can't wait for
        // synchronization. Instead just wait until after the ripples are finished animating.
        Thread.sleep(300)

        assertAgainstGolden("buttonGroup_fifthPressed_lightTheme")
    }

    private fun assertAgainstGolden(goldenName: String) {
        rule
            .onNodeWithTag(wrapperTestTag)
            .captureToImage()
            .assertAgainstGolden(screenshotRule, goldenName)
    }
}
