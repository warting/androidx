/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.compose.ui

import android.graphics.Rect
import android.graphics.Region
import android.os.Build
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.P
import android.os.Build.VERSION_CODES.R
import android.os.Bundle
import android.text.SpannableString
import android.view.View
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityEvent.CONTENT_CHANGE_TYPE_UNDEFINED
import android.view.accessibility.AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED
import android.view.accessibility.AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED
import android.view.accessibility.AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
import android.view.accessibility.AccessibilityNodeInfo.EXTRA_DATA_TEXT_CHARACTER_LOCATION_KEY
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.AndroidComposeView
import androidx.compose.ui.platform.AndroidComposeViewAccessibilityDelegateCompat
import androidx.compose.ui.platform.AndroidComposeViewAccessibilityDelegateCompat.Companion.ExtraDataShapeRectCornersKey
import androidx.compose.ui.platform.AndroidComposeViewAccessibilityDelegateCompat.Companion.ExtraDataShapeRectKey
import androidx.compose.ui.platform.AndroidComposeViewAccessibilityDelegateCompat.Companion.ExtraDataShapeRegionKey
import androidx.compose.ui.platform.AndroidComposeViewAccessibilityDelegateCompat.Companion.ExtraDataShapeTypeKey
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testClipEntry
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.ScrollAxisRange
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.accessibilityClassName
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.collapse
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.copyText
import androidx.compose.ui.semantics.cutText
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.dismiss
import androidx.compose.ui.semantics.editableText
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.expand
import androidx.compose.ui.semantics.focused
import androidx.compose.ui.semantics.getTextLayoutResult
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.horizontalScrollAxisRange
import androidx.compose.ui.semantics.isEditable
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.maxTextLength
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.password
import androidx.compose.ui.semantics.pasteText
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.semanticsId
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.setSelection
import androidx.compose.ui.semantics.setText
import androidx.compose.ui.semantics.shape
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.semantics.text
import androidx.compose.ui.semantics.textSelectionRange
import androidx.compose.ui.test.TestActivity
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.core.view.ViewCompat
import androidx.core.view.accessibility.AccessibilityEventCompat.CONTENT_CHANGE_TYPE_CONTENT_DESCRIPTION
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat.ACTION_CLICK
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat.ACTION_COLLAPSE
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat.ACTION_DISMISS
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat.ACTION_EXPAND
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat.ACTION_PASTE
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat.AccessibilityActionCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat.RangeInfoCompat.RANGE_TYPE_FLOAT
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.FlakyTest
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Correspondence
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class AndroidComposeViewAccessibilityDelegateCompatTest {
    @get:Rule val rule = createAndroidComposeRule<TestActivity>()

    private val tag = "tag"
    private lateinit var androidComposeView: AndroidComposeView
    private val dispatchedAccessibilityEvents = mutableListOf<AccessibilityEvent>()
    private val accessibilityEventLoopIntervalMs = 100L

    @Test
    fun testPopulateAccessibilityNodeInfoProperties_general() {
        // Arrange.
        val clickActionLabel = "click"
        val dismissActionLabel = "dismiss"
        val expandActionLabel = "expand"
        val collapseActionLabel = "collapse"
        val state = "checked"
        rule.setContentWithAccessibilityEnabled {
            Box(
                Modifier.size(10.dp).semantics {
                    stateDescription = "checked"
                    testTag = tag
                    testTagsAsResourceId = true
                    heading()
                    onClick(clickActionLabel) { true }
                    dismiss(dismissActionLabel) { true }
                    expand(expandActionLabel) { true }
                    collapse(collapseActionLabel) { true }
                }
            )
        }
        val virtualViewId = rule.onNodeWithTag(tag).semanticsId()

        // Act.
        val info = rule.runOnIdle { androidComposeView.createAccessibilityNodeInfo(virtualViewId) }

        // Assert.
        rule.runOnIdle {
            assertThat(info.className).isEqualTo("android.view.View")
            assertThat(info.actionList)
                .comparingElementsUsing(IdAndLabel)
                .containsAtLeast(
                    AccessibilityActionCompat(ACTION_CLICK, clickActionLabel),
                    AccessibilityActionCompat(ACTION_DISMISS, dismissActionLabel),
                    AccessibilityActionCompat(ACTION_EXPAND, expandActionLabel),
                    AccessibilityActionCompat(ACTION_COLLAPSE, collapseActionLabel),
                )
            assertThat(info.stateDescription).isEqualTo(state)
            assertThat(info.viewIdResourceName).isEqualTo(tag)
            assertThat(info.isHeading).isTrue()
            assertThat(info.isClickable).isTrue()
            assertThat(info.isVisibleToUser).isTrue()
            assertThat(info.isImportantForAccessibility).isTrue()
        }
    }

    @Test
    fun testPopulateAccessibilityNodeInfoProperties_screenReaderFocusable_mergingDescendants() {
        // Arrange.
        rule.setContentWithAccessibilityEnabled {
            Box(Modifier.size(10.dp).semantics(mergeDescendants = true) { testTag = tag })
        }
        val virtualViewId = rule.onNodeWithTag(tag).semanticsId()

        // Act.
        val info = rule.runOnIdle { androidComposeView.createAccessibilityNodeInfo(virtualViewId) }

        // Assert.
        rule.runOnIdle { assertThat(info.isScreenReaderFocusable).isTrue() }
    }

    @Test
    fun testPopulateAccessibilityNodeInfoProperties_screenReaderFocusable_notMergingDescendants() {
        // Arrange.
        rule.setContentWithAccessibilityEnabled {
            Box(Modifier.size(10.dp).semantics(mergeDescendants = false) { testTag = tag })
        }
        val virtualViewId = rule.onNodeWithTag(tag).semanticsId()

        // Act.
        val info = rule.runOnIdle { androidComposeView.createAccessibilityNodeInfo(virtualViewId) }

        // Assert.
        rule.runOnIdle { assertThat(info.isScreenReaderFocusable).isFalse() }
    }

    @Test
    fun testPopulateAccessibilityNodeInfoProperties_screenReaderFocusable_speakable() {
        // Arrange.
        rule.setContentWithAccessibilityEnabled {
            Box(
                Modifier.size(10.dp).semantics(mergeDescendants = false) {
                    testTag = tag
                    text = AnnotatedString("Example text")
                }
            )
        }
        val virtualViewId = rule.onNodeWithTag(tag).semanticsId()

        // Act.
        val info = rule.runOnIdle { androidComposeView.createAccessibilityNodeInfo(virtualViewId) }

        // Assert.
        rule.runOnIdle { assertThat(info.isScreenReaderFocusable).isTrue() }
    }

    @Test
    fun testPopulateAccessibilityNodeInfoProperties_disabled() {
        // Arrange.
        rule.setContentWithAccessibilityEnabled {
            val clipboard = LocalClipboard.current
            rememberCoroutineScope().launch(start = CoroutineStart.UNDISPATCHED) {
                clipboard.setClipEntry(testClipEntry("test"))
            }
            Box(
                Modifier.size(10.dp).semantics(mergeDescendants = true) {
                    testTag = tag
                    disabled()
                    editableText = AnnotatedString("text")
                    horizontalScrollAxisRange = ScrollAxisRange({ 0f }, { 5f })
                    onClick { true }
                    onLongClick { true }
                    copyText { true }
                    pasteText { true }
                    cutText { true }
                    setText { true }
                    setSelection { _, _, _ -> true }
                    dismiss { true }
                    expand { true }
                    collapse { true }
                }
            )
        }
        val virtualViewId = rule.onNodeWithTag(tag).semanticsId()

        // Act.
        val info = rule.runOnIdle { androidComposeView.createAccessibilityNodeInfo(virtualViewId) }

        // Assert.
        rule.runOnIdle {
            assertThat(info.isClickable).isTrue()
            assertThat(info.isLongClickable).isTrue()
            assertThat(info.actionList)
                .comparingElementsUsing(IdAndLabel)
                .containsNoneOf(
                    AccessibilityActionCompat.ACTION_CLICK,
                    AccessibilityActionCompat.ACTION_COLLAPSE,
                    AccessibilityActionCompat.ACTION_CUT,
                    AccessibilityActionCompat.ACTION_DISMISS,
                    AccessibilityActionCompat.ACTION_EXPAND,
                    AccessibilityActionCompat.ACTION_LONG_CLICK,
                    AccessibilityActionCompat.ACTION_PAGE_DOWN,
                    AccessibilityActionCompat.ACTION_PAGE_LEFT,
                    AccessibilityActionCompat.ACTION_PAGE_RIGHT,
                    AccessibilityActionCompat.ACTION_PAGE_UP,
                    AccessibilityActionCompat.ACTION_PASTE,
                    AccessibilityActionCompat.ACTION_SET_TEXT,
                    AccessibilityActionCompat.ACTION_SCROLL_FORWARD,
                    AccessibilityActionCompat.ACTION_SCROLL_RIGHT,
                )
            assertThat(info.actionList)
                .comparingElementsUsing(IdAndLabel)
                .containsAtLeast(
                    AccessibilityActionCompat.ACTION_COPY,
                    // This is the default ACTION_SET_SELECTION.
                    AccessibilityActionCompat.ACTION_SET_SELECTION,
                )
        }
    }

    @Test
    fun testPopulateAccessibilityNodeInfoProperties_buttonRole() {
        // Arrange.
        rule.setContentWithAccessibilityEnabled {
            Box(
                Modifier.size(10.dp).semantics(mergeDescendants = true) {
                    testTag = tag
                    role = Role.Button
                }
            )
        }
        val virtualViewId = rule.onNodeWithTag(tag).semanticsId()

        // Act.
        val info = rule.runOnIdle { androidComposeView.createAccessibilityNodeInfo(virtualViewId) }

        // Assert.
        rule.runOnIdle { assertThat(info.className).isEqualTo("android.widget.Button") }
    }

    @Test
    fun testPopulateAccessibilityNodeInfoProperties_switchRole() {
        // Arrange.
        rule.setContentWithAccessibilityEnabled {
            Box(
                Modifier.size(10.dp).semantics(mergeDescendants = true) {
                    testTag = tag
                    role = Role.Switch
                }
            )
        }
        val virtualViewId = rule.onNodeWithTag(tag).semanticsId()

        // Act.
        val info = rule.runOnIdle { androidComposeView.createAccessibilityNodeInfo(virtualViewId) }

        // Assert.
        rule.runOnIdle { assertThat(info.className).isEqualTo("android.view.View") }
    }

    @Test
    fun testPopulateAccessibilityNodeInfoProperties_switchRoleDescription() {
        // Arrange.
        rule.setContentWithAccessibilityEnabled {
            Box(
                Modifier.size(10.dp).semantics(mergeDescendants = true) {
                    testTag = tag
                    role = Role.Switch
                }
            )
        }
        val virtualViewId = rule.onNodeWithTag(tag).semanticsId()

        // Act.
        val info = rule.runOnIdle { androidComposeView.createAccessibilityNodeInfo(virtualViewId) }

        // Assert.
        rule.runOnIdle { assertThat(info.roleDescription).isEqualTo("Switch") }
    }

    @Test
    fun testPopulateAccessibilityNodeInfoProperties_checkBoxRole() {
        // Arrange.
        rule.setContentWithAccessibilityEnabled {
            Box(
                Modifier.size(10.dp).semantics(mergeDescendants = true) {
                    testTag = tag
                    role = Role.Checkbox
                }
            )
        }
        val virtualViewId = rule.onNodeWithTag(tag).semanticsId()

        // Act.
        val info = rule.runOnIdle { androidComposeView.createAccessibilityNodeInfo(virtualViewId) }

        // Assert.
        rule.runOnIdle { assertThat(info.className).isEqualTo("android.widget.CheckBox") }
    }

    @Test
    fun testPopulateAccessibilityNodeInfoProperties_radioButtonRole() {
        // Arrange.
        rule.setContentWithAccessibilityEnabled {
            Box(
                Modifier.size(10.dp).semantics(mergeDescendants = true) {
                    testTag = tag
                    role = Role.RadioButton
                }
            )
        }
        val virtualViewId = rule.onNodeWithTag(tag).semanticsId()

        // Act.
        val info = rule.runOnIdle { androidComposeView.createAccessibilityNodeInfo(virtualViewId) }

        // Assert.
        rule.runOnIdle { assertThat(info.className).isEqualTo("android.widget.RadioButton") }
    }

    @Test
    fun testPopulateAccessibilityNodeInfoProperties_tabRole() {
        // Arrange.
        rule.setContentWithAccessibilityEnabled {
            Box(
                Modifier.size(10.dp).semantics(mergeDescendants = true) {
                    testTag = tag
                    role = Role.Tab
                }
            )
        }
        val virtualViewId = rule.onNodeWithTag(tag).semanticsId()

        // Act.
        val info = rule.runOnIdle { androidComposeView.createAccessibilityNodeInfo(virtualViewId) }

        // Assert.
        rule.runOnIdle { assertThat(info.roleDescription).isEqualTo("Tab") }
    }

    @Test
    fun testPopulateAccessibilityNodeInfoProperties_imageRole() {
        // Arrange.
        rule.setContentWithAccessibilityEnabled {
            Box(
                Modifier.size(10.dp).semantics(mergeDescendants = true) {
                    testTag = tag
                    role = Role.Image
                }
            )
        }
        val virtualViewId = rule.onNodeWithTag(tag).semanticsId()

        // Act.
        val info = rule.runOnIdle { androidComposeView.createAccessibilityNodeInfo(virtualViewId) }

        // Assert.
        rule.runOnIdle { assertThat(info.className).isEqualTo("android.widget.ImageView") }
    }

    @Test
    @SdkSuppress(minSdkVersion = 24)
    fun testIsNotImportant_empty() {
        // Arrange.
        rule.setContentWithAccessibilityEnabled {
            Box(Modifier.size(10.dp).semantics(mergeDescendants = false) { testTag = tag })
        }
        val virtualViewId = rule.onNodeWithTag(tag).semanticsId()

        // Act.
        val info = rule.runOnIdle { androidComposeView.createAccessibilityNodeInfo(virtualViewId) }

        // Assert.
        rule.runOnIdle { assertThat(info.isImportantForAccessibility).isFalse() }
    }

    @Test
    @SdkSuppress(minSdkVersion = 24)
    fun testIsImportant_emptyMerging() {
        // Arrange.
        rule.setContentWithAccessibilityEnabled {
            Box(Modifier.size(10.dp).semantics(mergeDescendants = true) { testTag = tag })
        }
        val virtualViewId = rule.onNodeWithTag(tag).semanticsId()

        // Act.
        val info = rule.runOnIdle { androidComposeView.createAccessibilityNodeInfo(virtualViewId) }

        // Assert.
        rule.runOnIdle { assertThat(info.isImportantForAccessibility).isTrue() }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Test
    @SdkSuppress(minSdkVersion = 24)
    fun testIsNotImportant_testOnlyProperties() {
        // Arrange.
        rule.setContentWithAccessibilityEnabled {
            Box(
                Modifier.size(10.dp).semantics(mergeDescendants = false) {
                    testTag = tag
                    testTagsAsResourceId = true
                    hideFromAccessibility()
                }
            )
        }
        val virtualViewId = rule.onNodeWithTag(tag).semanticsId()

        // Act.
        val info = rule.runOnIdle { androidComposeView.createAccessibilityNodeInfo(virtualViewId) }

        // Assert.
        rule.runOnIdle { assertThat(info.isImportantForAccessibility).isFalse() }
    }

    @Test
    @SdkSuppress(minSdkVersion = 24)
    fun testIsImportant_accessibilitySpeakableProperties_stateDescription() {
        // Arrange.
        rule.setContentWithAccessibilityEnabled {
            Box(
                Modifier.size(10.dp).semantics(mergeDescendants = false) {
                    testTag = tag
                    stateDescription = "stateDescription"
                    heading()
                }
            )
        }
        val virtualViewId = rule.onNodeWithTag(tag).semanticsId()

        // Act.
        val info = rule.runOnIdle { androidComposeView.createAccessibilityNodeInfo(virtualViewId) }

        // Assert.
        rule.runOnIdle { assertThat(info.isImportantForAccessibility).isTrue() }
    }

    @Test
    @SdkSuppress(minSdkVersion = 24)
    fun testIsImportant_accessibilitySpeakableProperties_onClick() {
        // Arrange.
        rule.setContentWithAccessibilityEnabled {
            Box(
                Modifier.size(10.dp).semantics(mergeDescendants = false) {
                    testTag = tag
                    onClick("clickLabel") { true }
                }
            )
        }
        val virtualViewId = rule.onNodeWithTag(tag).semanticsId()

        // Act.
        val info = rule.runOnIdle { androidComposeView.createAccessibilityNodeInfo(virtualViewId) }

        // Assert.
        rule.runOnIdle { assertThat(info.isImportantForAccessibility).isTrue() }
    }

    private val PickedDateKey = SemanticsPropertyKey<Long>("PickedDate")
    private var SemanticsPropertyReceiver.pickedDate by PickedDateKey

    @Test
    @SdkSuppress(minSdkVersion = 24)
    fun testIsNotImportant_customSemanticsProperty() {
        // Arrange.
        rule.setContentWithAccessibilityEnabled {
            Box(
                Modifier.size(10.dp).semantics(mergeDescendants = false) {
                    testTag = tag
                    pickedDate = 1445378400 // 2015-10-21
                }
            )
        }
        val virtualViewId = rule.onNodeWithTag(tag).semanticsId()

        // Act.
        val info = rule.runOnIdle { androidComposeView.createAccessibilityNodeInfo(virtualViewId) }

        // Assert.
        rule.runOnIdle { assertThat(info.isImportantForAccessibility).isFalse() }
    }

    @Test
    @SdkSuppress(minSdkVersion = 24)
    fun testIsNotImportant_clearedWithTestTag() {
        // Arrange.
        rule.setContentWithAccessibilityEnabled {
            Box(
                Modifier.size(10.dp)
                    .clearAndSetSemantics { testTag = tag }
                    .semantics(mergeDescendants = true) { stateDescription = "stateDescription" }
            ) {
                Box(Modifier.semantics { text = AnnotatedString("foo") })
            }
        }
        val virtualViewId = rule.onNodeWithTag(tag).semanticsId()

        // Act.
        val info = rule.runOnIdle { androidComposeView.createAccessibilityNodeInfo(virtualViewId) }

        // Assert.
        rule.runOnIdle { assertThat(info.isImportantForAccessibility).isFalse() }
    }

    @Test
    fun textNode_withRoleButton_className_button() {
        // Arrange.
        rule.setContentWithAccessibilityEnabled {
            Box(
                Modifier.size(10.dp).semantics(mergeDescendants = true) {
                    testTag = tag
                    text = AnnotatedString("text") // makes it a text node
                    role = Role.Button
                }
            )
        }
        val virtualViewId = rule.onNodeWithTag(tag).semanticsId()

        // Act.
        val info = rule.runOnIdle { androidComposeView.createAccessibilityNodeInfo(virtualViewId) }

        // Assert.
        rule.runOnIdle { assertThat(info.className).isEqualTo("android.widget.Button") }
    }

    @Test
    fun textFieldNode_withRoleButton_className_button() {
        // Arrange.
        rule.setContentWithAccessibilityEnabled {
            Box(
                Modifier.size(10.dp).semantics(mergeDescendants = true) {
                    testTag = tag
                    editableText = AnnotatedString("")
                    setText { true } // makes it a text field node
                    role = Role.Button
                }
            )
        }
        val virtualViewId = rule.onNodeWithTag(tag).semanticsId()

        // Act.
        val info = rule.runOnIdle { androidComposeView.createAccessibilityNodeInfo(virtualViewId) }

        // Assert.
        rule.runOnIdle { assertThat(info.className).isEqualTo("android.widget.Button") }
    }

    @Test
    fun className_overwrittenBy_explicitaccessibilityClassNameProperty() {
        // Arrange.
        rule.setContentWithAccessibilityEnabled {
            Box(
                Modifier.size(10.dp).semantics(mergeDescendants = true) {
                    accessibilityClassName = "asdf"
                    testTag = tag
                    role = Role.Image
                }
            )
        }
        val virtualViewId = rule.onNodeWithTag(tag).semanticsId()

        // Act.
        val info = rule.runOnIdle { androidComposeView.createAccessibilityNodeInfo(virtualViewId) }

        // Assert.
        rule.runOnIdle { assertThat(info.className).isEqualTo("asdf") }
    }

    @Test
    fun explicitaccessibilityClassNameProperty_childrenNotMergeable() {
        // Arrange.
        rule.setContentWithAccessibilityEnabled {
            Box(Modifier.size(10.dp).semantics(mergeDescendants = true) { testTag = tag }) {
                Box(Modifier.size(10.dp).semantics { accessibilityClassName = "a" })
                Box(Modifier.size(10.dp).semantics { accessibilityClassName = "b" })
            }
        }
        val virtualViewId = rule.onNodeWithTag(tag).semanticsId()

        // Act.
        val info = rule.runOnIdle { androidComposeView.createAccessibilityNodeInfo(virtualViewId) }

        // Assert.
        rule.runOnIdle {
            assertThat(info.className)
                .isEqualTo(AndroidComposeViewAccessibilityDelegateCompat.ClassName)
        }
    }

    @Test
    fun nodeWithTextAndLayoutResult_className_textView() {
        // Arrange.
        rule.setContentWithAccessibilityEnabled {
            Box(
                Modifier.size(10.dp).semantics(mergeDescendants = true) {
                    testTag = tag
                    text = AnnotatedString("")
                }
            )
        }
        val virtualViewId = rule.onNodeWithTag(tag).semanticsId()

        // Act.
        val info = rule.runOnIdle { androidComposeView.createAccessibilityNodeInfo(virtualViewId) }

        // Assert.
        rule.runOnIdle { assertThat(info.className).isEqualTo("android.widget.TextView") }
    }

    @Test
    fun testPopulateAccessibilityNodeInfoProperties_liveRegion() {
        // Arrange.
        val testTag1 = "TestTag1"
        val testTag2 = "TestTag2"
        rule.setContentWithAccessibilityEnabled {
            Row {
                Box(
                    Modifier.size(10.dp).semantics(mergeDescendants = true) {
                        testTag = testTag1
                        liveRegion = LiveRegionMode.Polite
                    }
                )
                Box(
                    Modifier.size(10.dp).semantics(mergeDescendants = true) {
                        testTag = testTag2
                        liveRegion = LiveRegionMode.Assertive
                    }
                )
            }
        }
        val virtualViewId1 = rule.onNodeWithTag(testTag1).semanticsId()
        val virtualViewId2 = rule.onNodeWithTag(testTag2).semanticsId()

        // Act.
        lateinit var info1: AccessibilityNodeInfoCompat
        lateinit var info2: AccessibilityNodeInfoCompat
        rule.runOnIdle {
            info1 = androidComposeView.createAccessibilityNodeInfo(virtualViewId1)
            info2 = androidComposeView.createAccessibilityNodeInfo(virtualViewId2)
        }

        // Assert.
        rule.runOnIdle {
            assertThat(info1.liveRegion).isEqualTo(ViewCompat.ACCESSIBILITY_LIVE_REGION_POLITE)
            assertThat(info2.liveRegion).isEqualTo(ViewCompat.ACCESSIBILITY_LIVE_REGION_ASSERTIVE)
        }
    }

    @FlakyTest(bugId = 403310024)
    @Test
    fun testPopulateAccessibilityNodeInfoProperties_liveRegionUpdate() {
        // Arrange.
        var liveRegionMode by mutableStateOf(LiveRegionMode.Polite)
        rule.setContentWithAccessibilityEnabled {
            Box(
                Modifier.size(10.dp).semantics(mergeDescendants = true) {
                    testTag = tag
                    liveRegion = liveRegionMode
                }
            )
        }
        val virtualViewId = rule.onNodeWithTag(tag).semanticsId()
        val info1 = rule.runOnIdle { androidComposeView.createAccessibilityNodeInfo(virtualViewId) }
        dispatchedAccessibilityEvents.clear()

        // Act.
        rule.runOnIdle { liveRegionMode = LiveRegionMode.Assertive }
        val info2 = rule.runOnIdle { androidComposeView.createAccessibilityNodeInfo(virtualViewId) }

        // Assert.
        rule.runOnIdle {
            assertThat(info1.liveRegion).isEqualTo(ViewCompat.ACCESSIBILITY_LIVE_REGION_POLITE)
            assertThat(dispatchedAccessibilityEvents)
                .comparingElementsUsing(AccessibilityEventComparator)
                .containsExactly(
                    AccessibilityEvent().apply {
                        eventType = TYPE_WINDOW_CONTENT_CHANGED
                        contentChangeTypes = CONTENT_CHANGE_TYPE_UNDEFINED
                    }
                )
            assertThat(info2.liveRegion).isEqualTo(ViewCompat.ACCESSIBILITY_LIVE_REGION_ASSERTIVE)
        }
    }

    @Test
    fun testPopulateAccessibilityNodeInfoProperties_SeekBar() {
        // Arrange.
        val setProgressActionLabel = "setProgress"
        rule.mainClock.autoAdvance = false
        rule.setContentWithAccessibilityEnabled {
            Box(
                Modifier.size(10.dp).semantics(mergeDescendants = true) {
                    testTag = tag
                    progressBarRangeInfo = ProgressBarRangeInfo(0.5f, 0f..1f, 6)
                    setProgress(setProgressActionLabel) { true }
                }
            )
        }
        val virtualViewId = rule.onNodeWithTag(tag).semanticsId()

        // Act.
        val info = rule.runOnIdle { androidComposeView.createAccessibilityNodeInfo(virtualViewId) }

        // Assert.
        rule.runOnIdle {
            assertThat(info.className).isEqualTo("android.widget.SeekBar")
            assertThat(info.rangeInfo.type).isEqualTo(RANGE_TYPE_FLOAT)
            assertThat(info.rangeInfo.current).isEqualTo(0.5f)
            assertThat(info.rangeInfo.min).isEqualTo(0f)
            assertThat(info.rangeInfo.max).isEqualTo(1f)
            if (SDK_INT >= Build.VERSION_CODES.N) {
                assertThat(info.actionList)
                    .comparingElementsUsing(IdAndLabel)
                    .contains(
                        AccessibilityActionCompat(
                            android.R.id.accessibilityActionSetProgress,
                            setProgressActionLabel,
                        )
                    )
            }
        }
    }

    @Test
    fun testPopulateAccessibilityNodeInfoProperties_textField() {
        // Arrange.
        val setSelectionActionLabel = "setSelection"
        val setTextActionLabel = "setText"
        val text = "hello"
        rule.setContentWithAccessibilityEnabled {
            Box(
                Modifier.size(10.dp).semantics(mergeDescendants = true) {
                    testTag = tag
                    editableText = AnnotatedString(text)
                    textSelectionRange = TextRange(1)
                    focused = true
                    maxTextLength = 100
                    isEditable = true
                    getTextLayoutResult { true }
                    setText(setTextActionLabel) { true }
                    setSelection(setSelectionActionLabel) { _, _, _ -> true }
                }
            )
        }
        val virtualViewId = rule.onNodeWithTag(tag).semanticsId()

        // Act.
        val info = rule.runOnIdle { androidComposeView.createAccessibilityNodeInfo(virtualViewId) }

        // Assert.
        rule.runOnIdle {
            assertThat(info.className).isEqualTo("android.widget.EditText")
            assertThat(info.text).isEqualTo(SpannableString(text))
            assertThat(info.isFocusable).isTrue()
            assertThat(info.isFocused).isTrue()
            assertThat(info.isEditable).isTrue()
            assertThat(info.maxTextLength).isEqualTo(100)
            assertThat(info.actionList)
                .comparingElementsUsing(IdAndLabel)
                .containsAtLeast(
                    AccessibilityActionCompat(
                        AccessibilityNodeInfoCompat.ACTION_SET_SELECTION,
                        setSelectionActionLabel,
                    ),
                    AccessibilityActionCompat(
                        AccessibilityNodeInfoCompat.ACTION_SET_TEXT,
                        setTextActionLabel,
                    ),
                    AccessibilityActionCompat.ACTION_NEXT_AT_MOVEMENT_GRANULARITY,
                    AccessibilityActionCompat.ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY,
                )
            if (SDK_INT >= 26) {
                assertThat(info.unwrap().availableExtraData)
                    .containsExactly(
                        "androidx.compose.ui.semantics.id",
                        "androidx.compose.ui.semantics.testTag",
                        EXTRA_DATA_TEXT_CHARACTER_LOCATION_KEY,
                    )
            }
        }
    }

    @Test
    fun testPopulateAccessibilityNodeInfoProperties_textField_no_maxTextLength() {
        // Arrange.
        val setSelectionActionLabel = "setSelection"
        val setTextActionLabel = "setText"
        val text = "hello"
        rule.setContentWithAccessibilityEnabled {
            Box(
                Modifier.size(10.dp).semantics(mergeDescendants = true) {
                    testTag = tag
                    editableText = AnnotatedString(text)
                    textSelectionRange = TextRange(1)
                    focused = true
                    isEditable = true
                    getTextLayoutResult { true }
                    setText(setTextActionLabel) { true }
                    setSelection(setSelectionActionLabel) { _, _, _ -> true }
                }
            )
        }
        val virtualViewId = rule.onNodeWithTag(tag).semanticsId()

        // Act.
        val info = rule.runOnIdle { androidComposeView.createAccessibilityNodeInfo(virtualViewId) }

        // Assert.
        rule.runOnIdle { assertThat(info.maxTextLength).isEqualTo(-1) }
    }

    @Test
    fun testMovementGranularities_textField_focused() {
        // Arrange.
        rule.setContentWithAccessibilityEnabled {
            Box(
                Modifier.size(10.dp).semantics(mergeDescendants = true) {
                    testTag = tag
                    editableText = AnnotatedString("text")
                    textSelectionRange = TextRange(1)
                    focused = true
                    getTextLayoutResult { true }
                    setText { true }
                    setSelection { _, _, _ -> true }
                }
            )
        }
        val virtualViewId = rule.onNodeWithTag(tag).semanticsId()

        // Act.
        val info = rule.runOnIdle { androidComposeView.createAccessibilityNodeInfo(virtualViewId) }

        // Assert.
        rule.runOnIdle {
            assertThat(info.movementGranularities)
                .isEqualTo(
                    AccessibilityNodeInfoCompat.MOVEMENT_GRANULARITY_CHARACTER or
                        AccessibilityNodeInfoCompat.MOVEMENT_GRANULARITY_WORD or
                        AccessibilityNodeInfoCompat.MOVEMENT_GRANULARITY_PARAGRAPH or
                        AccessibilityNodeInfoCompat.MOVEMENT_GRANULARITY_LINE or
                        AccessibilityNodeInfoCompat.MOVEMENT_GRANULARITY_PAGE
                )
        }
    }

    @Test
    fun testMovementGranularities_textField_notFocused() {
        // Arrange.
        rule.setContentWithAccessibilityEnabled {
            Box(
                Modifier.size(10.dp).semantics(mergeDescendants = true) {
                    testTag = tag
                    editableText = AnnotatedString("text")
                    textSelectionRange = TextRange(1)
                    getTextLayoutResult { true }
                    setText { true }
                    setSelection { _, _, _ -> true }
                }
            )
        }
        val virtualViewId = rule.onNodeWithTag(tag).semanticsId()

        // Act.
        val info = rule.runOnIdle { androidComposeView.createAccessibilityNodeInfo(virtualViewId) }

        // Assert.
        rule.runOnIdle {
            assertThat(info.movementGranularities)
                .isEqualTo(
                    AccessibilityNodeInfoCompat.MOVEMENT_GRANULARITY_CHARACTER or
                        AccessibilityNodeInfoCompat.MOVEMENT_GRANULARITY_WORD or
                        AccessibilityNodeInfoCompat.MOVEMENT_GRANULARITY_PARAGRAPH
                )
        }
    }

    @Test
    fun testPopulateAccessibilityNodeInfoProperties_setContentInvalid_customDescription() {
        // Arrange.
        val errorDescription = "Invalid format"
        rule.setContentWithAccessibilityEnabled {
            Box(
                Modifier.size(10.dp).semantics(mergeDescendants = true) {
                    testTag = tag
                    error(errorDescription)
                }
            )
        }
        val virtualViewId = rule.onNodeWithTag(tag).semanticsId()

        // Act.
        val info = rule.runOnIdle { androidComposeView.createAccessibilityNodeInfo(virtualViewId) }

        // Assert.
        rule.runOnIdle {
            assertThat(info.isContentInvalid).isTrue()
            assertThat(info.error).isEqualTo(errorDescription)
        }
    }

    @Test
    fun testPopulateAccessibilityNodeInfoProperties_setContentInvalid_emptyDescription() {
        // Arrange.
        rule.setContentWithAccessibilityEnabled {
            Box(
                Modifier.size(10.dp).semantics(mergeDescendants = true) {
                    testTag = tag
                    error("")
                }
            )
        }
        val virtualViewId = rule.onNodeWithTag(tag).semanticsId()

        // Act.
        val info = rule.runOnIdle { androidComposeView.createAccessibilityNodeInfo(virtualViewId) }

        // Assert.
        rule.runOnIdle {
            assertThat(info.isContentInvalid).isTrue()
            assertThat(info.error.isEmpty()).isTrue()
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = 26)
    fun testPopulateShapeExtras_shapeSet_setsAllAvailableShapeExtraKeys() {
        // Arrange.
        rule.setContentWithAccessibilityEnabled {
            Box(
                Modifier.size(10.dp).semantics {
                    testTag = tag
                    shape = RectangleShape
                }
            )
        }
        val virtualViewId = rule.onNodeWithTag(tag).semanticsId()

        // Act.
        val info = rule.runOnIdle { androidComposeView.createAccessibilityNodeInfo(virtualViewId) }

        // Assert.
        rule.runOnIdle {
            assertThat(info.availableExtraData)
                .containsAtLeast(
                    ExtraDataShapeTypeKey,
                    ExtraDataShapeRectKey,
                    ExtraDataShapeRectCornersKey,
                    ExtraDataShapeRegionKey,
                )
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = 26)
    fun testPopulateShapeExtras_shapeUnset_doesNotSetShapeExtraKeys() {
        // Arrange.
        rule.setContentWithAccessibilityEnabled {
            Box(Modifier.size(10.dp).semantics { testTag = tag })
        }
        val virtualViewId = rule.onNodeWithTag(tag).semanticsId()

        // Act.
        val info = rule.runOnIdle { androidComposeView.createAccessibilityNodeInfo(virtualViewId) }

        // Assert.
        rule.runOnIdle {
            assertThat(info.availableExtraData)
                .containsNoneOf(
                    ExtraDataShapeTypeKey,
                    ExtraDataShapeRectKey,
                    ExtraDataShapeRectCornersKey,
                    ExtraDataShapeRegionKey,
                )
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = 26)
    fun testPopulateShapeExtras_requestShapeTypeExtra_rectangle() {
        // Arrange.
        rule.setContentWithAccessibilityEnabled {
            Box(
                Modifier.size(10.dp).semantics {
                    testTag = tag
                    shape = RectangleShape
                }
            )
        }
        val virtualViewId = rule.onNodeWithTag(tag).semanticsId()
        val info = rule.runOnIdle { androidComposeView.createAccessibilityNodeInfo(virtualViewId) }

        // Act.
        androidComposeView.composeAccessibilityDelegate
            .getAccessibilityNodeProvider(androidComposeView)
            .addExtraDataToAccessibilityNodeInfo(
                virtualViewId,
                info,
                ExtraDataShapeTypeKey,
                Bundle(),
            )

        // Assert.
        rule.runOnIdle {
            assertThat(info.extras.containsKey(ExtraDataShapeTypeKey)).isTrue()
            assertThat(info.extras.getInt(ExtraDataShapeTypeKey)).isEqualTo(0)

            assertThat(info.extras.containsKey(ExtraDataShapeRectKey)).isTrue()
            with(rule.density) {
                @Suppress("DEPRECATION")
                assertThat(info.extras.getParcelable<Rect>(ExtraDataShapeRectKey))
                    .isEqualTo(
                        Rect(
                            /* left = */ 0,
                            /* top = */ 0,
                            /* right = */ 10.dp.roundToPx(),
                            /* bottom = */ 10.dp.roundToPx(),
                        )
                    )
            }

            assertThat(info.extras.containsKey(ExtraDataShapeRectCornersKey)).isFalse()
            assertThat(info.extras.containsKey(ExtraDataShapeRegionKey)).isFalse()
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = 26)
    fun testPopulateShapeExtras_requestShapeExtra_rectangle() {
        // Arrange.
        rule.setContentWithAccessibilityEnabled {
            Box(
                Modifier.size(10.dp).semantics {
                    testTag = tag
                    shape = RectangleShape
                }
            )
        }
        val virtualViewId = rule.onNodeWithTag(tag).semanticsId()
        val info = rule.runOnIdle { androidComposeView.createAccessibilityNodeInfo(virtualViewId) }

        // Act.
        androidComposeView.composeAccessibilityDelegate
            .getAccessibilityNodeProvider(androidComposeView)
            .addExtraDataToAccessibilityNodeInfo(
                virtualViewId,
                info,
                ExtraDataShapeRectKey,
                Bundle(),
            )

        // Assert.
        rule.runOnIdle {
            assertThat(info.extras.containsKey(ExtraDataShapeRectKey)).isTrue()
            with(rule.density) {
                @Suppress("DEPRECATION")
                assertThat(info.extras.getParcelable<Rect>(ExtraDataShapeRectKey))
                    .isEqualTo(
                        Rect(
                            /* left = */ 0,
                            /* top = */ 0,
                            /* right = */ 10.dp.roundToPx(),
                            /* bottom = */ 10.dp.roundToPx(),
                        )
                    )
            }

            assertThat(info.extras.containsKey(ExtraDataShapeTypeKey)).isFalse()
            assertThat(info.extras.containsKey(ExtraDataShapeRectCornersKey)).isFalse()
            assertThat(info.extras.containsKey(ExtraDataShapeRegionKey)).isFalse()
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = 26)
    fun testPopulateShapeExtras_requestShapeTypeExtra_roundedCornerRectangle() {
        // Arrange.
        rule.setContentWithAccessibilityEnabled {
            Box(
                Modifier.size(10.dp).semantics {
                    testTag = tag
                    shape = RoundedCornerShape(1.dp, 2.dp, 3.dp, 4.dp)
                }
            )
        }
        val virtualViewId = rule.onNodeWithTag(tag).semanticsId()
        val info = rule.runOnIdle { androidComposeView.createAccessibilityNodeInfo(virtualViewId) }

        // Act.
        androidComposeView.composeAccessibilityDelegate
            .getAccessibilityNodeProvider(androidComposeView)
            .addExtraDataToAccessibilityNodeInfo(
                virtualViewId,
                info,
                ExtraDataShapeTypeKey,
                Bundle(),
            )

        // Assert.
        rule.runOnIdle {
            assertThat(info.extras.containsKey(ExtraDataShapeTypeKey)).isTrue()
            assertThat(info.extras.getInt(ExtraDataShapeTypeKey)).isEqualTo(1)

            assertThat(info.extras.containsKey(ExtraDataShapeRectKey)).isTrue()
            with(rule.density) {
                @Suppress("DEPRECATION")
                assertThat(info.extras.getParcelable<Rect>(ExtraDataShapeRectKey))
                    .isEqualTo(
                        Rect(
                            /* left = */ 0,
                            /* top = */ 0,
                            /* right = */ 10.dp.roundToPx(),
                            /* bottom = */ 10.dp.roundToPx(),
                        )
                    )
            }

            assertThat(info.extras.containsKey(ExtraDataShapeRectCornersKey)).isTrue()
            val corners = info.extras.getFloatArray(ExtraDataShapeRectCornersKey)!!
            with(rule.density) {
                assertThat(corners)
                    .isEqualTo(
                        floatArrayOf(
                            1.dp.toPx(),
                            1.dp.toPx(),
                            2.dp.toPx(),
                            2.dp.toPx(),
                            3.dp.toPx(),
                            3.dp.toPx(),
                            4.dp.toPx(),
                            4.dp.toPx(),
                        )
                    )
            }

            assertThat(info.extras.containsKey(ExtraDataShapeRegionKey)).isFalse()
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = 26)
    fun testPopulateShapeExtras_requestShapeExtras_roundedCornerRectangle() {
        // Arrange.
        rule.setContentWithAccessibilityEnabled {
            Box(
                Modifier.size(10.dp).semantics {
                    testTag = tag
                    shape = RoundedCornerShape(1.dp, 2.dp, 3.dp, 4.dp)
                }
            )
        }
        val virtualViewId = rule.onNodeWithTag(tag).semanticsId()
        val info = rule.runOnIdle { androidComposeView.createAccessibilityNodeInfo(virtualViewId) }

        // Act 1.
        androidComposeView.composeAccessibilityDelegate
            .getAccessibilityNodeProvider(androidComposeView)
            .addExtraDataToAccessibilityNodeInfo(
                virtualViewId,
                info,
                ExtraDataShapeRectKey,
                Bundle(),
            )

        // Assert 1.
        rule.runOnIdle {
            assertThat(info.extras.containsKey(ExtraDataShapeRectKey)).isTrue()
            with(rule.density) {
                @Suppress("DEPRECATION")
                assertThat(info.extras.getParcelable<Rect>(ExtraDataShapeRectKey))
                    .isEqualTo(
                        Rect(
                            /* left = */ 0,
                            /* top = */ 0,
                            /* right = */ 10.dp.roundToPx(),
                            /* bottom = */ 10.dp.roundToPx(),
                        )
                    )
            }

            assertThat(info.extras.containsKey(ExtraDataShapeTypeKey)).isFalse()
            assertThat(info.extras.containsKey(ExtraDataShapeRectCornersKey)).isFalse()
            assertThat(info.extras.containsKey(ExtraDataShapeRegionKey)).isFalse()
        }

        // Act 2.
        androidComposeView.composeAccessibilityDelegate
            .getAccessibilityNodeProvider(androidComposeView)
            .addExtraDataToAccessibilityNodeInfo(
                virtualViewId,
                info,
                ExtraDataShapeRectCornersKey,
                Bundle(),
            )

        // Assert 2.
        rule.runOnIdle {
            assertThat(info.extras.containsKey(ExtraDataShapeRectKey)).isTrue()
            assertThat(info.extras.containsKey(ExtraDataShapeRectCornersKey)).isTrue()
            val corners = info.extras.getFloatArray(ExtraDataShapeRectCornersKey)!!
            with(rule.density) {
                assertThat(corners)
                    .isEqualTo(
                        floatArrayOf(
                            1.dp.toPx(),
                            1.dp.toPx(),
                            2.dp.toPx(),
                            2.dp.toPx(),
                            3.dp.toPx(),
                            3.dp.toPx(),
                            4.dp.toPx(),
                            4.dp.toPx(),
                        )
                    )
            }

            assertThat(info.extras.containsKey(ExtraDataShapeTypeKey)).isFalse()
            assertThat(info.extras.containsKey(ExtraDataShapeRegionKey)).isFalse()
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = 26)
    fun testPopulateShapeExtras_requestShapeTypeExtra_genericShape() {
        // Arrange.
        rule.setContentWithAccessibilityEnabled {
            Box(
                Modifier.size(10.dp).semantics {
                    testTag = tag
                    shape = TriangleShape()
                }
            )
        }
        val virtualViewId = rule.onNodeWithTag(tag).semanticsId()
        val info = rule.runOnIdle { androidComposeView.createAccessibilityNodeInfo(virtualViewId) }

        // Act.
        androidComposeView.composeAccessibilityDelegate
            .getAccessibilityNodeProvider(androidComposeView)
            .addExtraDataToAccessibilityNodeInfo(
                virtualViewId,
                info,
                ExtraDataShapeTypeKey,
                Bundle(),
            )

        // Assert.
        rule.runOnIdle {
            assertThat(info.extras.containsKey(ExtraDataShapeTypeKey)).isTrue()
            assertThat(info.extras.getInt(ExtraDataShapeTypeKey)).isEqualTo(2)

            assertThat(info.extras.containsKey(ExtraDataShapeRegionKey)).isTrue()
            @Suppress("DEPRECATION")
            val region = info.extras.getParcelable<Region>(ExtraDataShapeRegionKey)!!
            assertThat(region.isEmpty).isFalse()
            assertThat(region.isRect).isFalse()
            assertThat(region.isComplex).isTrue()
            with(rule.density) {
                assertThat(region.bounds.left).isEqualTo(0)
                assertThat(region.bounds.top).isAtMost(1) // Region is an approximation
                assertThat(region.bounds.right).isEqualTo(10.dp.roundToPx())
                assertThat(region.bounds.bottom).isEqualTo(10.dp.roundToPx())

                assertThat(region.contains(5.dp.roundToPx(), 5.dp.roundToPx())).isTrue()
                assertThat(region.contains(3.dp.roundToPx(), 9.dp.roundToPx())).isTrue()
                assertThat(region.contains(7.dp.roundToPx(), 9.dp.roundToPx())).isTrue()
                assertThat(region.contains(3.dp.roundToPx(), 1.dp.roundToPx())).isFalse()
                assertThat(region.contains(7.dp.roundToPx(), 1.dp.roundToPx())).isFalse()
            }

            assertThat(info.extras.containsKey(ExtraDataShapeRectKey)).isFalse()
            assertThat(info.extras.containsKey(ExtraDataShapeRectCornersKey)).isFalse()
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = 26)
    fun testPopulateShapeExtras_requestShapeExtra_genericShape() {
        // Arrange.
        rule.setContentWithAccessibilityEnabled {
            Box(
                Modifier.size(10.dp).semantics {
                    testTag = tag
                    shape = TriangleShape()
                }
            )
        }
        val virtualViewId = rule.onNodeWithTag(tag).semanticsId()
        val info = rule.runOnIdle { androidComposeView.createAccessibilityNodeInfo(virtualViewId) }

        // Act.
        androidComposeView.composeAccessibilityDelegate
            .getAccessibilityNodeProvider(androidComposeView)
            .addExtraDataToAccessibilityNodeInfo(
                virtualViewId,
                info,
                ExtraDataShapeRegionKey,
                Bundle(),
            )

        // Assert.
        rule.runOnIdle {
            assertThat(info.extras.containsKey(ExtraDataShapeRegionKey)).isTrue()
            @Suppress("DEPRECATION")
            val region = info.extras.getParcelable<Region>(ExtraDataShapeRegionKey)!!
            assertThat(region.isEmpty).isFalse()
            assertThat(region.isRect).isFalse()
            assertThat(region.isComplex).isTrue()
            with(rule.density) {
                assertThat(region.bounds.left).isEqualTo(0)
                assertThat(region.bounds.top).isAtMost(1) // Region is an approximation
                assertThat(region.bounds.right).isEqualTo(10.dp.roundToPx())
                assertThat(region.bounds.bottom).isEqualTo(10.dp.roundToPx())

                assertThat(region.contains(5.dp.roundToPx(), 5.dp.roundToPx())).isTrue()
                assertThat(region.contains(3.dp.roundToPx(), 9.dp.roundToPx())).isTrue()
                assertThat(region.contains(7.dp.roundToPx(), 9.dp.roundToPx())).isTrue()
                assertThat(region.contains(3.dp.roundToPx(), 1.dp.roundToPx())).isFalse()
                assertThat(region.contains(7.dp.roundToPx(), 1.dp.roundToPx())).isFalse()
            }

            assertThat(info.extras.containsKey(ExtraDataShapeTypeKey)).isFalse()
            assertThat(info.extras.containsKey(ExtraDataShapeRectKey)).isFalse()
            assertThat(info.extras.containsKey(ExtraDataShapeRectCornersKey)).isFalse()
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = 26)
    fun testPopulateShapeExtras_requestWrongShapeExtra_rectangle_doesNotSetExtra() {
        // Arrange.
        rule.setContentWithAccessibilityEnabled {
            Box(
                Modifier.size(10.dp).semantics {
                    testTag = tag
                    shape = RectangleShape
                }
            )
        }
        val virtualViewId = rule.onNodeWithTag(tag).semanticsId()
        val info = rule.runOnIdle { androidComposeView.createAccessibilityNodeInfo(virtualViewId) }

        // Act 1.
        androidComposeView.composeAccessibilityDelegate
            .getAccessibilityNodeProvider(androidComposeView)
            .addExtraDataToAccessibilityNodeInfo(
                virtualViewId,
                info,
                ExtraDataShapeRectCornersKey,
                Bundle(),
            )

        // Assert 1.
        rule.runOnIdle {
            assertThat(info.extras.containsKey(ExtraDataShapeRectCornersKey)).isFalse()
        }

        // Act 2.
        androidComposeView.composeAccessibilityDelegate
            .getAccessibilityNodeProvider(androidComposeView)
            .addExtraDataToAccessibilityNodeInfo(
                virtualViewId,
                info,
                ExtraDataShapeRegionKey,
                Bundle(),
            )

        // Assert 2.
        rule.runOnIdle { assertThat(info.extras.containsKey(ExtraDataShapeRegionKey)).isFalse() }
    }

    @Test
    @SdkSuppress(minSdkVersion = 26)
    fun testPopulateShapeExtras_requestWrongShapeExtra_roundedCornerRectangle_doesNotSetExtra() {
        // Arrange.
        rule.setContentWithAccessibilityEnabled {
            Box(
                Modifier.size(10.dp).semantics {
                    testTag = tag
                    shape = RoundedCornerShape(1.dp, 2.dp, 3.dp, 4.dp)
                }
            )
        }
        val virtualViewId = rule.onNodeWithTag(tag).semanticsId()
        val info = rule.runOnIdle { androidComposeView.createAccessibilityNodeInfo(virtualViewId) }

        // Act.
        androidComposeView.composeAccessibilityDelegate
            .getAccessibilityNodeProvider(androidComposeView)
            .addExtraDataToAccessibilityNodeInfo(
                virtualViewId,
                info,
                ExtraDataShapeRegionKey,
                Bundle(),
            )

        // Assert.
        rule.runOnIdle { assertThat(info.extras.containsKey(ExtraDataShapeRegionKey)).isFalse() }
    }

    @Test
    @SdkSuppress(minSdkVersion = 26)
    fun testPopulateShapeExtras_requestWrongShapeExtra_genericShape_doesNotSetExtra() {
        // Arrange.
        rule.setContentWithAccessibilityEnabled {
            Box(
                Modifier.size(10.dp).semantics {
                    testTag = tag
                    shape = TriangleShape()
                }
            )
        }
        val virtualViewId = rule.onNodeWithTag(tag).semanticsId()
        val info = rule.runOnIdle { androidComposeView.createAccessibilityNodeInfo(virtualViewId) }

        // Act 1.
        androidComposeView.composeAccessibilityDelegate
            .getAccessibilityNodeProvider(androidComposeView)
            .addExtraDataToAccessibilityNodeInfo(
                virtualViewId,
                info,
                ExtraDataShapeRectKey,
                Bundle(),
            )

        // Assert 1.
        rule.runOnIdle { assertThat(info.extras.containsKey(ExtraDataShapeRectKey)).isFalse() }

        // Act 2.
        androidComposeView.composeAccessibilityDelegate
            .getAccessibilityNodeProvider(androidComposeView)
            .addExtraDataToAccessibilityNodeInfo(
                virtualViewId,
                info,
                ExtraDataShapeRectCornersKey,
                Bundle(),
            )

        // Assert 2.
        rule.runOnIdle {
            assertThat(info.extras.containsKey(ExtraDataShapeRectCornersKey)).isFalse()
        }
    }

    private class TriangleShape : Shape {
        override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density) =
            Outline.Generic(
                Path().apply {
                    moveTo(size.width / 2f, 0f)
                    lineTo(size.width, size.height)
                    lineTo(0f, size.height)
                    close()
                }
            )
    }

    @Test
    fun test_PasteAction_ifFocused() {
        // Arrange.
        rule.setContentWithAccessibilityEnabled {
            val clipboard = LocalClipboard.current
            rememberCoroutineScope().launch(start = CoroutineStart.UNDISPATCHED) {
                clipboard.setClipEntry(testClipEntry("test"))
            }
            Box(
                Modifier.size(10.dp).semantics(mergeDescendants = true) {
                    testTag = tag
                    focused = true
                    pasteText { true }
                }
            )
        }
        val virtualViewId = rule.onNodeWithTag(tag).semanticsId()

        // Act.
        val info = rule.runOnIdle { androidComposeView.createAccessibilityNodeInfo(virtualViewId) }

        // Assert.
        rule.runOnIdle {
            assertThat(info.isFocused).isTrue()
            assertThat(info.actionList)
                .comparingElementsUsing(IdAndLabel)
                .contains(AccessibilityActionCompat(ACTION_PASTE, null))
        }
    }

    @Test
    fun test_noPasteAction_ifUnfocused() {
        // Arrange.
        rule.setContentWithAccessibilityEnabled {
            val clipboard = LocalClipboard.current
            rememberCoroutineScope().launch(start = CoroutineStart.UNDISPATCHED) {
                clipboard.setClipEntry(testClipEntry("test"))
            }
            Box(
                Modifier.size(10.dp).semantics(mergeDescendants = true) {
                    testTag = tag
                    pasteText { true }
                }
            )
        }
        val virtualViewId = rule.onNodeWithTag(tag).semanticsId()

        // Act.
        val info = rule.runOnIdle { androidComposeView.createAccessibilityNodeInfo(virtualViewId) }

        // Assert.
        rule.runOnIdle {
            assertThat(info.isFocused).isFalse()
            assertThat(info.actionList)
                .comparingElementsUsing(IdAndLabel)
                .doesNotContain(AccessibilityActionCompat(ACTION_PASTE, null))
        }
    }

    @Test
    fun testActionCanBeNull() {
        // Arrange.
        val actionLabel = "send"
        rule.setContentWithAccessibilityEnabled {
            Box(
                Modifier.size(10.dp).semantics(mergeDescendants = true) {
                    testTag = tag
                    onClick(label = actionLabel, action = null)
                }
            )
        }
        val virtualViewId = rule.onNodeWithTag(tag).semanticsId()

        // Act.
        val info = rule.runOnIdle { androidComposeView.createAccessibilityNodeInfo(virtualViewId) }

        // Assert.
        // When action is null here, should we still think it is clickable? Should we add the action
        // to AccessibilityNodeInfo?
        rule.runOnIdle {
            assertThat(info.isClickable).isTrue()
            assertThat(info.actionList)
                .comparingElementsUsing(IdAndLabel)
                .contains(AccessibilityActionCompat(ACTION_CLICK, actionLabel))
        }
    }

    @Test
    fun testUncoveredNodes_notPlacedNodes_notIncluded() {
        // Arrange.
        rule.setContentWithAccessibilityEnabled {
            Row(Modifier.size(10.toDp())) {
                Box(Modifier.size(10.toDp()).semantics {})
                Box(Modifier.size(10.toDp()).semantics {})
                Box(Modifier.size(10.toDp()).semantics {})
            }
        }

        // Act.
        val info = rule.runOnIdle { androidComposeView.createAccessibilityNodeInfo() }

        // Assert.
        rule.runOnIdle { assertThat(info.childCount).isEqualTo(1) }
    }

    @SdkSuppress(maxSdkVersion = 33) // b/321824038
    @Test
    fun testGetBoundsInScreen_translation() {
        rule.setContentWithAccessibilityEnabled { Box(Modifier.width(30.toDp()).height(70.toDp())) }
        rule.runOnUiThread {
            androidComposeView.translationX = 2f
            androidComposeView.translationY = 3f
        }

        val bounds = Rect(-1, -1, -1, -1)
        rule.runOnIdle {
            val info = androidComposeView.createAccessibilityNodeInfo()
            info.getBoundsInScreen(bounds)
        }

        rule.runOnIdle { assertThat(bounds).isEqualTo(Rect(2, 3, 30 + 2, 70 + 3)) }
    }

    @SdkSuppress(maxSdkVersion = 33) // b/321824038
    @Test
    fun testGetBoundsInScreen_scale() {
        rule.setContentWithAccessibilityEnabled { Box(Modifier.width(30.toDp()).height(70.toDp())) }
        rule.runOnUiThread {
            androidComposeView.scaleX = 2f
            androidComposeView.scaleY = 3f
            androidComposeView.pivotX = 0f
            androidComposeView.pivotY = 0f
        }

        val bounds = Rect(-1, -1, -1, -1)
        rule.runOnIdle {
            val info = androidComposeView.createAccessibilityNodeInfo()
            info.getBoundsInScreen(bounds)
        }

        rule.runOnIdle { assertThat(bounds).isEqualTo(Rect(0, 0, 30 * 2, 70 * 3)) }
    }

    @SdkSuppress(maxSdkVersion = 33) // b/321824038
    @Test
    fun testGetBoundsInScreen_rotation() {
        rule.setContentWithAccessibilityEnabled { Box(Modifier.width(30.toDp()).height(70.toDp())) }
        rule.runOnUiThread {
            androidComposeView.rotation = 90f
            androidComposeView.pivotX = 0f
            androidComposeView.pivotY = 0f
        }

        val bounds = Rect(-1, -1, -1, -1)
        rule.runOnIdle {
            val info = androidComposeView.createAccessibilityNodeInfo()
            info.getBoundsInScreen(bounds)
        }

        rule.runOnIdle { assertThat(bounds).isEqualTo(Rect(-70, 0, 0, 30)) }
    }

    @SdkSuppress(maxSdkVersion = 33) // b/321824038
    @Test
    fun testUncoveredNodes_zeroBoundsRoot_included() {
        // Arrange.
        val bounds = Rect(-1, -1, -1, -1)
        rule.setContentWithAccessibilityEnabled { Box {} }

        // Act.
        rule.runOnIdle {
            val info = androidComposeView.createAccessibilityNodeInfo()
            info.getBoundsInScreen(bounds)
        }

        // Assert.
        rule.runOnIdle { assertThat(bounds).isEqualTo(Rect(0, 0, 0, 0)) }
    }

    @Test
    fun testContentDescriptionCastSuccess() {
        // Arrange.
        var hasContentDescription by mutableStateOf(false)
        rule.mainClock.autoAdvance = false
        rule.setContentWithAccessibilityEnabled {
            Box(
                Modifier.size(10.dp).semantics(mergeDescendants = true) {
                    if (hasContentDescription) {
                        contentDescription = "Hello" // To trigger content description casting.
                    }
                }
            )
        }

        // Act.
        rule.runOnIdle { hasContentDescription = true }
        rule.mainClock.advanceTimeBy(accessibilityEventLoopIntervalMs)

        // Assert.
        rule.runOnIdle {
            assertThat(dispatchedAccessibilityEvents)
                .comparingElementsUsing(AccessibilityEventComparator)
                .containsExactly(
                    AccessibilityEvent().apply {
                        eventType = TYPE_WINDOW_CONTENT_CHANGED
                        contentChangeTypes = CONTENT_CHANGE_TYPE_CONTENT_DESCRIPTION
                        contentDescription = "Hello"
                    }
                )
        }
    }

    // TODO(b/272068594): Asserting that a list does not contain an element can be an incorrect test
    //  because this would pass even if the event was present, (For example when isEnabled = false).
    //  Keeping this here to show parity for code review. This can be removed because the test
    //  passwordVisibilityToggle_fromInvisibleToVisible_sendTwoSelectionEvents covers this case.
    @Test
    fun passwordVisibilityToggle_fromInvisibleToVisible_doNotSendTextChangeEvent() {
        // Arrange.
        var passwordVisible by mutableStateOf(false)
        rule.mainClock.autoAdvance = false
        rule.setContentWithAccessibilityEnabled {
            Box(
                Modifier.size(10.dp).semantics(mergeDescendants = true) {
                    setText { true }
                    if (passwordVisible) password()
                    textSelectionRange = TextRange(4)
                    editableText = AnnotatedString(if (passwordVisible) "1234" else "****")
                }
            )
        }

        // Act.
        rule.runOnIdle { passwordVisible = true }
        rule.mainClock.advanceTimeBy(accessibilityEventLoopIntervalMs)

        // Assert.
        rule.runOnIdle {
            assertThat(dispatchedAccessibilityEvents)
                .comparingElementsUsing(AccessibilityEventComparator)
                .doesNotContain(AccessibilityEvent().apply { eventType = TYPE_VIEW_TEXT_CHANGED })
        }
    }

    // TODO(b/272068594): Asserting that a list does not contain an element can be an incorrect test
    //  because this would pass even if the event was present, (For example when isEnabled = false).
    //  Keeping this here to show parity for code review. This can be removed because the test
    //  passwordVisibilityToggle_fromVisibleToInvisible_sendTwoSelectionEvents covers this case.
    @Test
    fun passwordVisibilityToggle_fromVisibleToInvisible_doNotSendTextChangeEvent() {
        // Arrange.
        var passwordVisible by mutableStateOf(true)
        rule.mainClock.autoAdvance = false
        rule.setContentWithAccessibilityEnabled {
            Box(
                Modifier.size(10.dp).semantics(mergeDescendants = true) {
                    setText { true }
                    if (passwordVisible) password()
                    textSelectionRange = TextRange(4)
                    editableText = AnnotatedString(if (passwordVisible) "1234" else "****")
                }
            )
        }

        // Act.
        rule.runOnIdle { passwordVisible = false }
        rule.mainClock.advanceTimeBy(accessibilityEventLoopIntervalMs)

        // Assert.
        rule.runOnIdle {
            assertThat(dispatchedAccessibilityEvents)
                .comparingElementsUsing(AccessibilityEventComparator)
                .doesNotContain(AccessibilityEvent().apply { eventType = TYPE_VIEW_TEXT_CHANGED })
        }
    }

    @Test
    fun passwordVisibilityToggle_fromInvisibleToVisible_sendTwoSelectionEvents() {
        // Arrange.
        var passwordVisible by mutableStateOf(false)
        rule.mainClock.autoAdvance = false
        rule.setContentWithAccessibilityEnabled {
            Box(
                Modifier.size(10.dp).semantics(mergeDescendants = true) {
                    setText { true }
                    if (passwordVisible) password()
                    textSelectionRange = TextRange(4)
                    editableText = AnnotatedString(if (passwordVisible) "1234" else "****")
                }
            )
        }

        // Act.
        rule.runOnIdle { passwordVisible = true }
        rule.mainClock.advanceTimeBy(accessibilityEventLoopIntervalMs)

        // Assert.
        rule.runOnIdle {
            assertThat(dispatchedAccessibilityEvents)
                .comparingElementsUsing(AccessibilityEventComparator)
                .containsExactly(
                    AccessibilityEvent().apply {
                        eventType = TYPE_VIEW_TEXT_SELECTION_CHANGED
                        className = "android.widget.EditText"
                        text.add("1234")
                        itemCount = 4
                        fromIndex = 4
                        toIndex = 4
                        isPassword = true
                    },
                    AccessibilityEvent().apply {
                        eventType = TYPE_VIEW_TEXT_SELECTION_CHANGED
                        className = "android.widget.EditText"
                        text.add("1234")
                        itemCount = 4
                        fromIndex = 4
                        toIndex = 4
                        isPassword = true
                    },
                    AccessibilityEvent().apply {
                        eventType = TYPE_WINDOW_CONTENT_CHANGED
                        isPassword = true
                    },
                )
        }
    }

    @Test
    fun passwordVisibilityToggle_fromVisibleToInvisible_sendTwoSelectionEvents() {
        // Arrange.
        var passwordVisible by mutableStateOf(true)
        rule.mainClock.autoAdvance = false
        rule.setContentWithAccessibilityEnabled {
            Box(
                Modifier.size(10.dp).semantics(mergeDescendants = true) {
                    setText { true }
                    if (passwordVisible) password()
                    textSelectionRange = TextRange(4)
                    editableText = AnnotatedString(if (passwordVisible) "1234" else "****")
                }
            )
        }

        // Act.
        rule.runOnIdle { passwordVisible = false }
        rule.mainClock.advanceTimeBy(accessibilityEventLoopIntervalMs)

        // Assert.
        rule.runOnIdle {
            assertThat(dispatchedAccessibilityEvents)
                .comparingElementsUsing(AccessibilityEventComparator)
                .containsExactly(
                    AccessibilityEvent().apply {
                        eventType = TYPE_VIEW_TEXT_SELECTION_CHANGED
                        className = "android.widget.EditText"
                        text.add("****")
                        itemCount = 4
                        fromIndex = 4
                        toIndex = 4
                    },
                    AccessibilityEvent().apply {
                        eventType = TYPE_VIEW_TEXT_SELECTION_CHANGED
                        className = "android.widget.EditText"
                        text.add("****")
                        itemCount = 4
                        fromIndex = 4
                        toIndex = 4
                    },
                    AccessibilityEvent().apply { eventType = TYPE_WINDOW_CONTENT_CHANGED },
                )
        }
    }

    @Test
    fun textChanged_sendTextChangeEvent() {
        // Arrange.
        var textChanged by mutableStateOf(false)
        rule.mainClock.autoAdvance = false
        rule.setContentWithAccessibilityEnabled {
            Box(
                Modifier.size(10.dp).semantics(mergeDescendants = true) {
                    testTag = tag
                    setText { true }
                    textSelectionRange = TextRange(4)
                    editableText = AnnotatedString(if (!textChanged) "1234" else "1235")
                }
            )
        }
        val virtualId = rule.onNodeWithTag(tag).semanticsId()

        // Act.
        rule.runOnIdle { textChanged = true }
        rule.mainClock.advanceTimeBy(accessibilityEventLoopIntervalMs)

        // Assert.
        rule.runOnIdle {
            assertThat(dispatchedAccessibilityEvents)
                .comparingElementsUsing(AccessibilityEventComparator)
                .containsExactly(
                    AccessibilityEvent().apply {
                        eventType = TYPE_VIEW_TEXT_CHANGED
                        className = "android.widget.EditText"
                        text.add("1235")
                        beforeText = "1234"
                        fromIndex = 3
                        addedCount = 1
                        removedCount = 1
                        setSource(androidComposeView, virtualId)
                    }
                )
        }
    }

    @Test
    fun textChanged_passwordNode_sendTextChangeEvent() {
        // Arrange.
        var textChanged by mutableStateOf(false)
        rule.mainClock.autoAdvance = false
        rule.setContentWithAccessibilityEnabled {
            Box(
                Modifier.size(10.dp).semantics(mergeDescendants = true) {
                    setText { true }
                    password()
                    textSelectionRange = TextRange(4)
                    editableText = AnnotatedString(if (!textChanged) "1234" else "1235")
                }
            )
        }

        // Act.
        rule.runOnIdle { textChanged = true }
        rule.mainClock.advanceTimeBy(accessibilityEventLoopIntervalMs)

        // Assert.
        rule.runOnIdle {
            assertThat(dispatchedAccessibilityEvents)
                .comparingElementsUsing(AccessibilityEventComparator)
                .containsExactly(
                    AccessibilityEvent().apply {
                        eventType = TYPE_VIEW_TEXT_CHANGED
                        className = "android.widget.EditText"
                        text.add("1235")
                        beforeText = "1234"
                        fromIndex = 3
                        addedCount = 1
                        removedCount = 1
                        isPassword = true
                    }
                )
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = 24)
    fun drawingOrder_defaultOrder() {
        // Arrange.
        rule.setContentWithAccessibilityEnabled {
            Column(
                Modifier.semantics {
                    testTag = "container"
                    text = AnnotatedString("container")
                }
            ) {
                Box(
                    Modifier.size(10.dp).semantics {
                        testTag = "item0"
                        text = AnnotatedString("item0")
                    }
                )
                Box(
                    Modifier.size(10.dp).semantics {
                        testTag = "item1"
                        text = AnnotatedString("item1")
                    }
                )
                Box(
                    Modifier.size(10.dp).semantics {
                        testTag = "item2"
                        text = AnnotatedString("item2")
                    }
                )
            }
        }
        val containerId = rule.onNodeWithTag("container").semanticsId()
        val item0Id = rule.onNodeWithTag("item0").semanticsId()
        val item1Id = rule.onNodeWithTag("item1").semanticsId()
        val item2Id = rule.onNodeWithTag("item2").semanticsId()

        // Act.
        rule.runOnIdle { androidComposeView.createAccessibilityNodeInfo(containerId) }
        val info0 = rule.runOnIdle { androidComposeView.createAccessibilityNodeInfo(item0Id) }
        val info1 = rule.runOnIdle { androidComposeView.createAccessibilityNodeInfo(item1Id) }
        val info2 = rule.runOnIdle { androidComposeView.createAccessibilityNodeInfo(item2Id) }

        // Assert.
        rule.runOnIdle {
            assertThat(info0.drawingOrder).isEqualTo(0)
            assertThat(info1.drawingOrder).isEqualTo(1)
            assertThat(info2.drawingOrder).isEqualTo(2)
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = 24)
    fun drawingOrder_customZIndex() {
        // Arrange.
        rule.setContentWithAccessibilityEnabled {
            Column(
                Modifier.semantics {
                    testTag = "container"
                    text = AnnotatedString("container")
                }
            ) {
                Box(
                    Modifier.zIndex(100f).size(10.dp).semantics {
                        testTag = "item0"
                        text = AnnotatedString("item0")
                    }
                )
                Box(
                    Modifier.size(10.dp).semantics {
                        testTag = "item1"
                        text = AnnotatedString("item1")
                    }
                )
                Box(
                    Modifier.size(10.dp).semantics {
                        testTag = "item2"
                        text = AnnotatedString("item2")
                    }
                )
            }
        }
        val containerId = rule.onNodeWithTag("container").semanticsId()
        val item0Id = rule.onNodeWithTag("item0").semanticsId()
        val item1Id = rule.onNodeWithTag("item1").semanticsId()
        val item2Id = rule.onNodeWithTag("item2").semanticsId()

        // Act.
        rule.runOnIdle { androidComposeView.createAccessibilityNodeInfo(containerId) }
        val info0 = rule.runOnIdle { androidComposeView.createAccessibilityNodeInfo(item0Id) }
        val info1 = rule.runOnIdle { androidComposeView.createAccessibilityNodeInfo(item1Id) }
        val info2 = rule.runOnIdle { androidComposeView.createAccessibilityNodeInfo(item2Id) }

        // Assert.
        rule.runOnIdle {
            assertThat(info0.drawingOrder).isEqualTo(2)
            assertThat(info1.drawingOrder).isEqualTo(0)
            assertThat(info2.drawingOrder).isEqualTo(1)
        }
    }

    @Test
    fun nodeAddition_sendsSubtreeChangedEvent() {
        // Arrange.
        var hasContentDescription by mutableStateOf(false)
        rule.mainClock.autoAdvance = false
        rule.setContentWithAccessibilityEnabled {
            Box(
                Modifier.size(10.dp).semantics {
                    if (hasContentDescription) {
                        contentDescription = "Test"
                    }
                }
            )
        }

        // Act.
        rule.runOnIdle { hasContentDescription = !hasContentDescription }
        rule.mainClock.advanceTimeBy(accessibilityEventLoopIntervalMs)

        // Assert.
        rule.runOnIdle {
            assertThat(dispatchedAccessibilityEvents)
                .comparingElementsUsing(AccessibilityEventComparator)
                .containsExactly(
                    AccessibilityEvent().apply {
                        eventType = TYPE_WINDOW_CONTENT_CHANGED
                        contentChangeTypes = CONTENT_CHANGE_TYPE_CONTENT_DESCRIPTION
                        contentDescription = "Test"
                    }
                )
        }
    }

    @Test
    fun nodeRemoval_sendsSubtreeChangedEvent() {
        // Arrange.
        var hasContentDescription by mutableStateOf(true)
        rule.mainClock.autoAdvance = false
        rule.setContentWithAccessibilityEnabled {
            Box(
                Modifier.size(10.dp).semantics {
                    if (hasContentDescription) {
                        contentDescription = "Test"
                    }
                }
            )
        }

        // Act.
        rule.runOnIdle { hasContentDescription = !hasContentDescription }
        rule.mainClock.advanceTimeBy(accessibilityEventLoopIntervalMs)

        // Assert.
        rule.runOnIdle {
            assertThat(dispatchedAccessibilityEvents)
                .comparingElementsUsing(AccessibilityEventComparator)
                .containsExactly(
                    AccessibilityEvent().apply {
                        eventType = TYPE_WINDOW_CONTENT_CHANGED
                        contentDescription = null
                    }
                )
        }
    }

    private fun Int.toDp(): Dp = with(rule.density) { this@toDp.toDp() }

    private fun ComposeContentTestRule.setContentWithAccessibilityEnabled(
        content: @Composable () -> Unit
    ) {
        setContent {
            androidComposeView = LocalView.current as AndroidComposeView
            with(androidComposeView.composeAccessibilityDelegate) {
                accessibilityForceEnabledForTesting = true
                onSendAccessibilityEvent = {
                    dispatchedAccessibilityEvents += it
                    false
                }
            }
            content()
        }

        // Advance the clock past the first accessibility event loop, and clear the initial
        // events as we are want the assertions to check the events that were generated later.
        runOnIdle { mainClock.advanceTimeBy(accessibilityEventLoopIntervalMs) }
        runOnIdle { dispatchedAccessibilityEvents.clear() }
    }

    private fun AndroidComposeView.createAccessibilityNodeInfo(
        semanticsId: Int
    ): AccessibilityNodeInfoCompat {
        onSemanticsChange()
        val accNodeInfo = accessibilityNodeProvider.createAccessibilityNodeInfo(semanticsId)
        checkNotNull(accNodeInfo) { "Could not find semantics node with id = $semanticsId" }
        return AccessibilityNodeInfoCompat.wrap(accNodeInfo)
    }

    companion object {
        internal val IdAndLabel =
            Correspondence.from<AccessibilityActionCompat, AccessibilityActionCompat>(
                { actual, expected ->
                    actual != null &&
                        expected != null &&
                        actual.id == expected.id &&
                        actual.label == expected.label
                },
                "has same id and label as",
            )

        internal val AccessibilityEventComparator =
            Correspondence.from<AccessibilityEvent, AccessibilityEvent>(
                { actual, expected ->
                    actual != null &&
                        expected != null &&
                        actual.eventType == expected.eventType &&
                        actual.eventTime == expected.eventTime &&
                        actual.packageName == expected.packageName &&
                        actual.movementGranularity == expected.movementGranularity &&
                        actual.action == expected.action &&
                        actual.contentChangeTypes == expected.contentChangeTypes &&
                        (SDK_INT < P || actual.windowChanges == expected.windowChanges) &&
                        actual.className.contentEquals(expected.className) &&
                        actual.text.toString() == expected.text.toString() &&
                        actual.contentDescription.contentEquals(expected.contentDescription) &&
                        actual.itemCount == expected.itemCount &&
                        actual.currentItemIndex == expected.currentItemIndex &&
                        actual.isEnabled == expected.isEnabled &&
                        actual.isPassword == expected.isPassword &&
                        actual.isChecked == expected.isChecked &&
                        actual.isFullScreen == expected.isFullScreen &&
                        actual.isScrollable == expected.isScrollable &&
                        actual.beforeText.contentEquals(expected.beforeText) &&
                        actual.fromIndex == expected.fromIndex &&
                        actual.toIndex == expected.toIndex &&
                        actual.scrollX == expected.scrollX &&
                        actual.scrollY == expected.scrollY &&
                        actual.maxScrollX == expected.maxScrollX &&
                        actual.maxScrollY == expected.maxScrollY &&
                        (SDK_INT < P || actual.scrollDeltaX == expected.scrollDeltaX) &&
                        (SDK_INT < P || actual.scrollDeltaY == expected.scrollDeltaY) &&
                        actual.addedCount == expected.addedCount &&
                        actual.removedCount == expected.removedCount &&
                        actual.parcelableData == expected.parcelableData &&
                        actual.recordCount == expected.recordCount
                },
                "has same properties as",
            )
    }

    private val View.composeAccessibilityDelegate: AndroidComposeViewAccessibilityDelegateCompat
        get() =
            ViewCompat.getAccessibilityDelegate(this)
                as AndroidComposeViewAccessibilityDelegateCompat

    // TODO(b/304359126): Move this to AccessibilityEventCompat and use it wherever we use obtain().
    private fun AccessibilityEvent(): AccessibilityEvent =
        if (SDK_INT >= R) {
                android.view.accessibility.AccessibilityEvent()
            } else {
                @Suppress("DEPRECATION") AccessibilityEvent.obtain()
            }
            .apply {
                packageName = "androidx.compose.ui.test"
                className = "android.view.View"
                isEnabled = true
            }
}
