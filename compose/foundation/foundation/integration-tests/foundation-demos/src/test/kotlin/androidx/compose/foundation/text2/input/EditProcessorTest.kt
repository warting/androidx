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

package androidx.compose.foundation.text2.input

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TextInputSession
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.reset
import org.mockito.kotlin.times
import org.mockito.kotlin.verify

@RunWith(JUnit4::class)
class EditProcessorTest {

    @Test
    fun test_new_state_and_edit_commands() {
        val proc = EditProcessor()
        val tis: TextInputSession = mock()

        val model = TextFieldValue("ABCDE", TextRange.Zero)
        proc.reset(model, tis)

        assertThat(proc.value).isEqualTo(model)
        val captor = argumentCaptor<TextFieldValue>()
        verify(tis, times(1)).updateState(
            eq(TextFieldValue("", TextRange.Zero)),
            captor.capture()
        )
        assertThat(captor.allValues.size).isEqualTo(1)
        assertThat(captor.firstValue.text).isEqualTo("ABCDE")
        assertThat(captor.firstValue.selection.min).isEqualTo(0)
        assertThat(captor.firstValue.selection.max).isEqualTo(0)

        reset(tis)

        val newState = proc.update(
            listOf(
                CommitTextCommand("X", 1)
            )
        )

        assertThat(newState.text).isEqualTo("XABCDE")
        assertThat(newState.selection.min).isEqualTo(1)
        assertThat(newState.selection.max).isEqualTo(1)
        // onEditCommands should not fire onStateUpdated since need to pass it to developer first.
        verify(tis, never()).updateState(any(), any())
    }

    @Test
    fun testNewState_bufferNotUpdated_ifSameModelStructurally() {
        val processor = EditProcessor()
        val textInputSession = mock<TextInputSession>()

        val initialBuffer = processor.mBuffer
        processor.reset(
            TextFieldValue("qwerty", TextRange.Zero, TextRange.Zero),
            textInputSession
        )
        assertThat(processor.mBuffer).isNotEqualTo(initialBuffer)

        val updatedBuffer = processor.mBuffer
        processor.reset(
            TextFieldValue("qwerty", TextRange.Zero, TextRange.Zero),
            textInputSession
        )
        assertThat(processor.mBuffer).isEqualTo(updatedBuffer)
    }

    @Test
    fun testNewState_new_buffer_created_if_text_is_different() {
        val processor = EditProcessor()
        val textInputSession = mock<TextInputSession>()

        val textFieldValue = TextFieldValue("qwerty", TextRange.Zero, TextRange.Zero)
        processor.reset(
            textFieldValue,
            textInputSession
        )
        val initialBuffer = processor.mBuffer

        val newTextFieldValue = textFieldValue.copy("abc")
        processor.reset(
            newTextFieldValue,
            textInputSession
        )

        assertThat(processor.mBuffer).isNotEqualTo(initialBuffer)
    }

    @Test
    fun testNewState_buffer_not_recreated_if_selection_is_different() {
        val processor = EditProcessor()
        val textInputSession = mock<TextInputSession>()
        val textFieldValue = TextFieldValue("qwerty", TextRange.Zero, TextRange.Zero)
        processor.reset(
            textFieldValue,
            textInputSession
        )
        val initialBuffer = processor.mBuffer

        val newTextFieldValue = textFieldValue.copy(selection = TextRange(1))
        processor.reset(
            newTextFieldValue,
            textInputSession
        )

        assertThat(processor.mBuffer).isEqualTo(initialBuffer)
        assertThat(newTextFieldValue.selection.start).isEqualTo(processor.mBuffer.selectionStart)
        assertThat(newTextFieldValue.selection.end).isEqualTo(processor.mBuffer.selectionEnd)
    }

    @Test
    fun testNewState_buffer_not_recreated_if_composition_is_different() {
        val processor = EditProcessor()
        val textInputSeson = mock<TextInputSession>()
        val textFieldValue = TextFieldValue("qwerty", TextRange.Zero, TextRange(1))
        processor.reset(
            textFieldValue,
            textInputSeson
        )
        val initialBuffer = processor.mBuffer

        // composition can not be set from app, IME owns it.
        assertThat(EditingBuffer.NOWHERE).isEqualTo(initialBuffer.compositionStart)
        assertThat(EditingBuffer.NOWHERE).isEqualTo(initialBuffer.compositionEnd)

        val newTextFieldValue = textFieldValue.copy(composition = null)
        processor.reset(
            newTextFieldValue,
            textInputSeson
        )

        assertThat(processor.mBuffer).isEqualTo(initialBuffer)
        assertThat(EditingBuffer.NOWHERE).isEqualTo(processor.mBuffer.compositionStart)
        assertThat(EditingBuffer.NOWHERE).isEqualTo(processor.mBuffer.compositionEnd)
    }

    @Test
    fun testNewState_reversedSelection_setsTheSelection() {
        val processor = EditProcessor()
        val textInputSession = mock<TextInputSession>()
        val initialSelection = TextRange(2, 1)
        val textFieldValue = TextFieldValue("qwerty", initialSelection, TextRange(1))

        // set the initial selection to be reversed
        processor.reset(
            textFieldValue,
            textInputSession
        )
        val initialBuffer = processor.mBuffer

        assertThat(initialSelection.min).isEqualTo(initialBuffer.selectionStart)
        assertThat(initialSelection.max).isEqualTo(initialBuffer.selectionEnd)

        val updatedSelection = TextRange(3, 0)
        val newTextFieldValue = textFieldValue.copy(selection = updatedSelection)
        // set the new selection
        processor.reset(
            newTextFieldValue,
            textInputSession
        )

        assertThat(processor.mBuffer).isEqualTo(initialBuffer)
        assertThat(updatedSelection.min).isEqualTo(initialBuffer.selectionStart)
        assertThat(updatedSelection.max).isEqualTo(initialBuffer.selectionEnd)
    }

    @Test
    fun compositionIsCleared_when_textChanged() {
        val processor = EditProcessor()
        val textInputSession = mock<TextInputSession>()

        // set the initial value
        processor.update(
            listOf(
                CommitTextCommand("ab", 0),
                SetComposingRegionCommand(0, 2)
            )
        )

        // change the text
        val newValue = processor.value.copy(text = "cd")
        processor.reset(newValue, textInputSession)

        assertThat(processor.value.text).isEqualTo(newValue.text)
        assertThat(processor.value.composition).isNull()
    }

    @Test
    fun compositionIsNotCleared_when_textIsSame() {
        val processor = EditProcessor()
        val textInputSession = mock<TextInputSession>()
        val composition = TextRange(0, 2)

        // set the initial value
        processor.update(
            listOf(
                CommitTextCommand("ab", 0),
                SetComposingRegionCommand(composition.start, composition.end)
            )
        )

        // use the same TextFieldValue
        val newValue = processor.value.copy()
        processor.reset(newValue, textInputSession)

        assertThat(processor.value.text).isEqualTo(newValue.text)
        assertThat(processor.value.composition).isEqualTo(composition)
    }

    @Test
    fun compositionIsCleared_when_compositionReset() {
        val processor = EditProcessor()
        val textInputSession = mock<TextInputSession>()

        // set the initial value
        processor.update(
            listOf(
                CommitTextCommand("ab", 0),
                SetComposingRegionCommand(-1, -1)
            )
        )

        // change the composition
        val newValue = processor.value.copy(composition = TextRange(0, 2))
        processor.reset(newValue, textInputSession)

        assertThat(processor.value.text).isEqualTo(newValue.text)
        assertThat(processor.value.composition).isNull()
    }

    @Test
    fun compositionIsCleared_when_compositionChanged() {
        val processor = EditProcessor()
        val textInputSession = mock<TextInputSession>()

        // set the initial value
        processor.update(
            listOf(
                CommitTextCommand("ab", 0),
                SetComposingRegionCommand(0, 2)
            )
        )

        // change the composition
        val newValue = processor.value.copy(composition = TextRange(0, 1))
        processor.reset(newValue, textInputSession)

        assertThat(processor.value.text).isEqualTo(newValue.text)
        assertThat(processor.value.composition).isNull()
    }

    @Test
    fun compositionIsNotCleared_when_onlySelectionChanged() {
        val processor = EditProcessor()
        val textInputSession = mock<TextInputSession>()
        val composition = TextRange(0, 2)
        val selection = TextRange(0, 2)

        // set the initial value
        processor.update(
            listOf(
                CommitTextCommand("ab", 0),
                SetComposingRegionCommand(composition.start, composition.end),
                SetSelectionCommand(selection.start, selection.end)
            )
        )

        // change selection
        val newSelection = TextRange(1)
        val newValue = processor.value.copy(selection = newSelection)
        processor.reset(newValue, textInputSession)

        assertThat(processor.value.text).isEqualTo(newValue.text)
        assertThat(processor.value.composition).isEqualTo(composition)
        assertThat(processor.value.selection).isEqualTo(newSelection)
    }

    // removed descriptive message test because EditCommand a sealed interface now.
}