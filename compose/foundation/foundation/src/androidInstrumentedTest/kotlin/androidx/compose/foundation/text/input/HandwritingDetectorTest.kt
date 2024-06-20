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

package androidx.compose.foundation.text.input

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.text.handwriting.handwritingDetector
import androidx.compose.foundation.text.handwriting.isStylusHandwritingSupported
import androidx.compose.foundation.text.performStylusClick
import androidx.compose.foundation.text.performStylusHandwriting
import androidx.compose.foundation.text.performStylusLongClick
import androidx.compose.foundation.text.performStylusLongPressAndDrag
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Assume
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.Ignore
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
internal class HandwritingDetectorTest {
    @get:Rule val rule = createComposeRule()

    @get:Rule val immRule = ComposeInputMethodManagerTestRule()

    private val imm = FakeInputMethodManager()

    private val detectorTag = "detector"
    private val insideSpacerTag = "inside"
    private val outsideSpacerTag = "outside"

    private var callbackCount = 0

    @Before
    fun setup() {
        // Test is only meaningful when stylus handwriting is supported.
        Assume.assumeTrue(isStylusHandwritingSupported)

        immRule.setFactory { imm }

        callbackCount = 0

        rule.setContent {
            Column(Modifier.safeContentPadding()) {
                Spacer(
                    modifier =
                        Modifier.fillMaxWidth()
                            .height(40.dp)
                            .handwritingDetector { callbackCount++ }
                            .testTag(detectorTag)
                )
                // This spacer is within the extended handwriting bounds of the detector
                Spacer(modifier = Modifier.fillMaxWidth().height(10.dp).testTag(insideSpacerTag))
                // This spacer is outside the extended handwriting bounds of the detector
                Spacer(modifier = Modifier.fillMaxWidth().height(10.dp).testTag(outsideSpacerTag))
            }
        }
    }

    @Test
    fun detector_handwriting_preparesDelegation() {
        rule.onNodeWithTag(detectorTag).performStylusHandwriting()

        assertHandwritingDelegationPrepared()
    }

    // Extended bounds is reverted due to b/346850837 will enable it when we support extended
    // bounds for handwriting again.
    @Test
    @Ignore
    fun detector_handwritingInExtendedBounds_preparesDelegation() {
        // This spacer is within the extended handwriting bounds of the detector
        rule.onNodeWithTag(insideSpacerTag).performStylusHandwriting()

        assertHandwritingDelegationPrepared()
    }

    @Test
    @Ignore
    fun detector_handwritingOutsideExtendedBounds_notPreparesDelegation() {
        // This spacer is outside the extended handwriting bounds of the detector
        rule.onNodeWithTag(outsideSpacerTag).performStylusHandwriting()

        assertHandwritingDelegationNotPrepared()
    }

    @Test
    fun detector_click_notPreparesDelegation() {
        rule.onNodeWithTag(detectorTag).performStylusClick()

        assertHandwritingDelegationNotPrepared()
    }

    @Test
    fun detector_longClick_notPreparesDelegation() {
        rule.onNodeWithTag(detectorTag).performStylusLongClick()

        assertHandwritingDelegationNotPrepared()
    }

    @Test
    fun detector_longPressAndDrag_notPreparesDelegation() {
        rule.onNodeWithTag(detectorTag).performStylusLongPressAndDrag()

        assertHandwritingDelegationNotPrepared()
    }

    private fun assertHandwritingDelegationPrepared() {
        rule.runOnIdle {
            assertThat(callbackCount).isEqualTo(1)
            imm.expectCall("prepareStylusHandwritingDelegation")
            imm.expectNoMoreCalls()
        }
    }

    private fun assertHandwritingDelegationNotPrepared() {
        rule.runOnIdle {
            assertThat(callbackCount).isEqualTo(0)
            imm.expectNoMoreCalls()
        }
    }
}
