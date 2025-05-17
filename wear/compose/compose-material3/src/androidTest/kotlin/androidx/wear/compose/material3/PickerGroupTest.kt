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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class PickerGroupTest {
    @get:Rule val rule = createComposeRule()

    @Test
    fun supports_test_tag() {
        rule.setContentWithTheme {
            PickerGroup(modifier = Modifier.testTag(TEST_TAG_1)) {
                addPickerColumns(count = 1, selectedColumn = 0)
            }
        }

        rule.onNodeWithTag(TEST_TAG_1).assertExists()
    }

    @Test
    fun state_returns_initially_selected_index_at_start() {
        val initiallySelectedColumn = 1
        var selectedIndex = initiallySelectedColumn
        rule.setContentWithTheme {
            PickerGroup { addPickerColumns(count = 2, selectedColumn = selectedIndex) }
        }

        rule.waitForIdle()

        assertThat(selectedIndex).isEqualTo(initiallySelectedColumn)
    }

    @Test
    fun pickers_are_added_to_picker_group() {
        rule.setContentWithTheme {
            PickerGroup {
                addPickerColumnWithTag(TEST_TAG_1, isSelected = true)
                addPickerColumnWithTag(TEST_TAG_2, isSelected = false)
            }
        }

        rule.onNodeWithTag(TEST_TAG_1).assertExists()
        rule.onNodeWithTag(TEST_TAG_2).assertExists()
    }

    @Test
    fun picker_changes_focus_when_clicked() {
        lateinit var selectedIndex: MutableState<Int>
        val talkBackOff = overrideTalkBackState(touchExplorationServiceState = false)

        rule.setContentWithTheme {
            selectedIndex = remember { mutableStateOf(0) }
            CompositionLocalProvider(LocalTouchExplorationStateProvider provides talkBackOff) {
                PickerGroup {
                    addPickerColumnWithTag(
                        TEST_TAG_1,
                        isSelected = selectedIndex.value == 0,
                        onSelected = { selectedIndex.value = 0 },
                    )
                    addPickerColumnWithTag(
                        TEST_TAG_2,
                        isSelected = selectedIndex.value == 1,
                        onSelected = { selectedIndex.value = 1 },
                    )
                }
            }
        }

        rule.onNodeWithTag(TEST_TAG_2).performClick()
        rule.waitForIdle()

        assertThat(selectedIndex.value).isEqualTo(1)
    }

    @Composable
    private fun PickerGroupScope.addPickerColumns(count: Int, selectedColumn: Int) =
        repeat(count) {
            PickerGroupItem(
                pickerState = PickerState(10),
                selected = selectedColumn == it,
                onSelected = {},
            ) { index: Int, _: Boolean ->
                Box(modifier = Modifier.size(100.dp)) { Text(text = "$index") }
            }
        }

    @Composable
    private fun PickerGroupScope.addPickerColumnWithTag(
        tag: String,
        isSelected: Boolean,
        onSelected: () -> Unit = {},
    ) =
        PickerGroupItem(
            selected = isSelected,
            pickerState = PickerState(10),
            modifier = Modifier.testTag(tag),
            onSelected = onSelected,
        ) { _: Int, _: Boolean ->
            Box(modifier = Modifier.size(20.dp))
        }

    private fun overrideTalkBackState(
        touchExplorationServiceState: Boolean
    ): TouchExplorationStateProvider {
        return TouchExplorationStateProvider { rememberUpdatedState(touchExplorationServiceState) }
    }
}

private const val TEST_TAG_1 = "random string 1"
private const val TEST_TAG_2 = "random string 2"
