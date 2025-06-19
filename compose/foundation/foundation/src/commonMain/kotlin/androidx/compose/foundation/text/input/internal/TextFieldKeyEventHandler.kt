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

package androidx.compose.foundation.text.input.internal

import androidx.collection.MutableLongSet
import androidx.compose.foundation.ComposeFoundationFlags
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.text.DeadKeyCombiner
import androidx.compose.foundation.text.KeyCommand
import androidx.compose.foundation.text.appendCodePointX
import androidx.compose.foundation.text.cancelsTextSelection
import androidx.compose.foundation.text.input.internal.selection.SelectionMovementDeletionContext
import androidx.compose.foundation.text.input.internal.selection.TextFieldPreparedSelectionState
import androidx.compose.foundation.text.input.internal.selection.TextFieldSelectionState
import androidx.compose.foundation.text.isTypedEvent
import androidx.compose.foundation.text.platformDefaultKeyMapping
import androidx.compose.foundation.text.showCharacterPalette
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.SoftwareKeyboardController
import kotlin.jvm.JvmInline

/** Factory function to create a platform specific [TextFieldKeyEventHandler]. */
internal expect fun createTextFieldKeyEventHandler(): TextFieldKeyEventHandler

/** Returns whether this key event is created by the software keyboard. */
internal expect val KeyEvent.isFromSoftKeyboard: Boolean

/**
 * Handles KeyEvents coming to a BasicTextField. This is mostly to support hardware keyboard but any
 * KeyEvent can also be sent by the IME or other platform systems.
 *
 * This class is left abstract to make sure that each platform extends from it. Platforms can decide
 * to extend or completely override KeyEvent actions defined here.
 */
internal abstract class TextFieldKeyEventHandler {
    private val preparedSelectionState = TextFieldPreparedSelectionState()
    private val deadKeyCombiner = DeadKeyCombiner()
    private val keyMapping = platformDefaultKeyMapping

    /**
     * We hold a reference to the all key down events that we receive and consume so that we can
     * also consume the corresponding key up events. Otherwise the up events get sent to the
     * ancestor nodes where the behavior is unpredictable. Please refer to b/353554186 for more
     * information.
     */
    // TODO(b/307580000) Factor this state out into a class to manage key inputs.
    private var currentlyConsumedDownKeys: MutableLongSet? = null

    open fun onPreKeyEvent(
        event: KeyEvent,
        textFieldState: TransformedTextFieldState,
        textFieldSelectionState: TextFieldSelectionState,
        focusManager: FocusManager,
        keyboardController: SoftwareKeyboardController,
    ): Boolean {
        val selection = textFieldState.visualText.selection
        return if (!selection.collapsed && event.cancelsTextSelection()) {
            textFieldSelectionState.deselect()
            true
        } else {
            false
        }
    }

    open fun onKeyEvent(
        event: KeyEvent,
        textFieldState: TransformedTextFieldState,
        textLayoutState: TextLayoutState,
        textFieldSelectionState: TextFieldSelectionState,
        clipboardKeyCommandsHandler: ClipboardKeyCommandsHandler,
        keyboardController: SoftwareKeyboardController,
        editable: Boolean,
        singleLine: Boolean,
        onSubmit: () -> Boolean,
    ): Boolean {
        val keyCode = event.key.keyCode

        if (event.type == KeyEventType.KeyUp) {
            if (currentlyConsumedDownKeys?.contains(keyCode) == true) {
                currentlyConsumedDownKeys?.remove(keyCode)
                return true
            } else {
                return false
            }
        }

        if (event.type == KeyEventType.Unknown && !event.isTypedEvent) {
            return false
        }

        val consumed =
            processKeyDownEvent(
                event = event,
                textFieldState = textFieldState,
                textLayoutState = textLayoutState,
                clipboardKeyCommandsHandler = clipboardKeyCommandsHandler,
                keyboardController = keyboardController,
                editable = editable,
                singleLine = singleLine,
                onSubmit = onSubmit,
            )

        if (consumed) {
            // initialize if it hasn't been initialized yet.
            val currentlyConsumedDownKeys =
                currentlyConsumedDownKeys
                    ?: MutableLongSet(initialCapacity = 3).also { currentlyConsumedDownKeys = it }
            currentlyConsumedDownKeys += keyCode
        }

        return consumed
    }

    @OptIn(ExperimentalFoundationApi::class)
    private fun processKeyDownEvent(
        event: KeyEvent,
        textFieldState: TransformedTextFieldState,
        textLayoutState: TextLayoutState,
        clipboardKeyCommandsHandler: ClipboardKeyCommandsHandler,
        keyboardController: SoftwareKeyboardController,
        editable: Boolean,
        singleLine: Boolean,
        onSubmit: () -> Boolean,
    ): Boolean {
        if (event.isTypedEvent) {
            val codePoint = deadKeyCombiner.consume(event)
            if (codePoint != null) {
                val text = StringBuilder(2).appendCodePointX(codePoint).toString()
                return if (editable) {
                    textFieldState.replaceSelectedText(
                        newText = text,
                        clearComposition = true,
                        restartImeIfContentChanges = !event.isFromSoftKeyboard,
                    )
                    preparedSelectionState.resetCachedX()
                    true
                } else {
                    false
                }
            }
        }

        val command = keyMapping.map(event)
        if (command == null || (command.editsText && !editable)) {
            return false
        }

        val layoutResult = textLayoutState.layoutResult
        val visibleTextLayoutHeight = textLayoutState.getVisibleTextLayoutHeight()
        SelectionMovementDeletionContext(
                state = textFieldState,
                textLayoutResult = layoutResult,
                isFromSoftKeyboard = event.isFromSoftKeyboard,
                visibleTextLayoutHeight = visibleTextLayoutHeight,
                textPreparedSelectionState = preparedSelectionState,
            )
            .run {
                // By default we assume that the event will be consumed if it made its way here. Any
                // branch that decides that the command should not be consumed, should explicitly
                // set
                // `consumed` to false.
                var consumed = true
                when (command) {
                    KeyCommand.COPY,
                    KeyCommand.PASTE,
                    KeyCommand.CUT -> clipboardKeyCommandsHandler.handler(command)
                    KeyCommand.LEFT_CHAR -> collapseLeftOr { moveCursorLeftByChar() }
                    KeyCommand.RIGHT_CHAR -> collapseRightOr { moveCursorRightByChar() }
                    KeyCommand.LEFT_WORD -> moveCursorLeftByWord()
                    KeyCommand.RIGHT_WORD -> moveCursorRightByWord()
                    KeyCommand.PREV_PARAGRAPH -> moveCursorPrevByParagraph()
                    KeyCommand.NEXT_PARAGRAPH -> moveCursorNextByParagraph()
                    KeyCommand.UP -> moveCursorUpByLine()
                    KeyCommand.DOWN -> moveCursorDownByLine()
                    KeyCommand.PAGE_UP -> moveCursorUpByPage()
                    KeyCommand.PAGE_DOWN -> moveCursorDownByPage()
                    KeyCommand.LINE_START -> moveCursorToLineStart()
                    KeyCommand.LINE_END -> moveCursorToLineEnd()
                    KeyCommand.LINE_LEFT -> moveCursorToLineLeftSide()
                    KeyCommand.LINE_RIGHT -> moveCursorToLineRightSide()
                    KeyCommand.HOME -> moveCursorToHome()
                    KeyCommand.END -> moveCursorToEnd()
                    KeyCommand.DELETE_PREV_CHAR ->
                        moveCursorPrevByCodePointOrEmoji().deleteMovement()
                    KeyCommand.DELETE_NEXT_CHAR -> moveCursorNextByChar().deleteMovement()
                    KeyCommand.DELETE_PREV_WORD -> moveCursorPrevByWord().deleteMovement()
                    KeyCommand.DELETE_NEXT_WORD -> moveCursorNextByWord().deleteMovement()
                    KeyCommand.DELETE_FROM_LINE_START -> moveCursorToLineStart().deleteMovement()
                    KeyCommand.DELETE_TO_LINE_END -> moveCursorToLineEnd().deleteMovement()
                    KeyCommand.NEW_LINE -> {
                        if (!singleLine) {
                            textFieldState.replaceSelectedText(
                                newText = "\n",
                                clearComposition = true,
                                restartImeIfContentChanges = !event.isFromSoftKeyboard,
                            )
                        } else {
                            consumed = onSubmit()
                        }
                    }
                    KeyCommand.TAB -> {
                        if (!singleLine) {
                            textFieldState.replaceSelectedText(
                                newText = "\t",
                                clearComposition = true,
                                restartImeIfContentChanges = !event.isFromSoftKeyboard,
                            )
                        } else {
                            consumed = false // let propagate to focus system
                        }
                    }
                    KeyCommand.SELECT_ALL -> selectAll()
                    KeyCommand.SELECT_LEFT_CHAR -> moveCursorLeftByChar().selectMovement()
                    KeyCommand.SELECT_RIGHT_CHAR -> moveCursorRightByChar().selectMovement()
                    KeyCommand.SELECT_LEFT_WORD -> moveCursorLeftByWord().selectMovement()
                    KeyCommand.SELECT_RIGHT_WORD -> moveCursorRightByWord().selectMovement()
                    KeyCommand.SELECT_PREV_PARAGRAPH -> moveCursorPrevByParagraph().selectMovement()
                    KeyCommand.SELECT_NEXT_PARAGRAPH -> moveCursorNextByParagraph().selectMovement()
                    KeyCommand.SELECT_LINE_START -> moveCursorToLineStart().selectMovement()
                    KeyCommand.SELECT_LINE_END -> moveCursorToLineEnd().selectMovement()
                    KeyCommand.SELECT_LINE_LEFT -> moveCursorToLineLeftSide().selectMovement()
                    KeyCommand.SELECT_LINE_RIGHT -> moveCursorToLineRightSide().selectMovement()
                    KeyCommand.SELECT_UP -> moveCursorUpByLine().selectMovement()
                    KeyCommand.SELECT_DOWN -> moveCursorDownByLine().selectMovement()
                    KeyCommand.SELECT_PAGE_UP -> moveCursorUpByPage().selectMovement()
                    KeyCommand.SELECT_PAGE_DOWN -> moveCursorDownByPage().selectMovement()
                    KeyCommand.SELECT_HOME -> moveCursorToHome().selectMovement()
                    KeyCommand.SELECT_END -> moveCursorToEnd().selectMovement()
                    KeyCommand.DESELECT -> deselect()
                    KeyCommand.UNDO -> {
                        textFieldState.undo()
                    }
                    KeyCommand.REDO -> {
                        textFieldState.redo()
                    }
                    KeyCommand.CHARACTER_PALETTE -> {
                        showCharacterPalette()
                    }
                    KeyCommand.CENTER -> {
                        // Only consume this event if the fix flag is enabled.
                        if (ComposeFoundationFlags.isTextFieldDpadNavigationEnabled) {
                            keyboardController.show()
                        } else {
                            consumed = false
                        }
                    }
                }
                if (ComposeFoundationFlags.isTextFieldDpadNavigationEnabled) {
                    // evaluate movement events to check whether they were actually consumed.
                    if (
                        command == KeyCommand.UP ||
                            command == KeyCommand.DOWN ||
                            command == KeyCommand.LEFT_CHAR ||
                            command == KeyCommand.RIGHT_CHAR
                    ) {
                        // If selection did not change, the movement event was not consumed.
                        consumed = initialValue.selection != selection
                    }
                }

                // selection changes are applied atomically at the end of context evaluation
                if (selection != initialValue.selection) {
                    textFieldState.selectCharsIn(selection)
                }

                if (wedgeAffinity != null) {
                    wedgeAffinity?.let { wedgeAffinity ->
                        if (textFieldState.untransformedText.selection.collapsed) {
                            textFieldState.selectionWedgeAffinity =
                                SelectionWedgeAffinity(wedgeAffinity)
                        } else {
                            textFieldState.selectionWedgeAffinity =
                                initialWedgeAffinity.copy(endAffinity = wedgeAffinity)
                        }
                    }
                }
                return consumed
            }
    }

    /**
     * Returns the current viewport height of TextField to help calculate where cursor should travel
     * when page down and up events are received. If the text layout is not calculated, returns
     * [Float.NaN].
     */
    private fun TextLayoutState.getVisibleTextLayoutHeight(): Float {
        return textLayoutNodeCoordinates
            ?.takeIf { it.isAttached }
            ?.let { textLayoutCoordinates ->
                decoratorNodeCoordinates
                    ?.takeIf { it.isAttached }
                    ?.localBoundingBoxOf(textLayoutCoordinates)
            }
            ?.size
            ?.height ?: Float.NaN
    }
}

@JvmInline internal value class ClipboardKeyCommandsHandler(val handler: (KeyCommand) -> Unit)
