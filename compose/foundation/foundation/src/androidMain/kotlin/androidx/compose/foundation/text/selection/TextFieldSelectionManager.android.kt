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

package androidx.compose.foundation.text.selection

import android.os.Build
import androidx.compose.foundation.PlatformMagnifierFactory
import androidx.compose.foundation.contextmenu.ContextMenuScope
import androidx.compose.foundation.contextmenu.ContextMenuState
import androidx.compose.foundation.isPlatformMagnifierSupported
import androidx.compose.foundation.magnifier
import androidx.compose.foundation.text.MenuItemsAvailability
import androidx.compose.foundation.text.TextContextMenuItems
import androidx.compose.foundation.text.TextContextMenuItems.Autofill
import androidx.compose.foundation.text.TextContextMenuItems.Copy
import androidx.compose.foundation.text.TextContextMenuItems.Cut
import androidx.compose.foundation.text.TextContextMenuItems.Paste
import androidx.compose.foundation.text.TextContextMenuItems.SelectAll
import androidx.compose.foundation.text.TextItem
import androidx.compose.foundation.text.contextmenu.builder.TextContextMenuBuilderScope
import androidx.compose.foundation.text.contextmenu.modifier.addTextContextMenuComponentsWithResources
import androidx.compose.foundation.text.textItem
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch

internal actual val PointerEvent.isShiftPressed: Boolean
    get() = false

// We use composed{} to read a local, but don't provide inspector info because the underlying
// magnifier modifier provides more meaningful inspector info.
internal actual fun Modifier.textFieldMagnifier(manager: TextFieldSelectionManager): Modifier {
    // Avoid tracking animation state on older Android versions that don't support magnifiers.
    if (!isPlatformMagnifierSupported()) {
        return this
    }

    return composed {
        val density = LocalDensity.current
        var magnifierSize by remember { mutableStateOf(IntSize.Zero) }
        animatedSelectionMagnifier(
            magnifierCenter = { calculateSelectionMagnifierCenterAndroid(manager, magnifierSize) },
            platformMagnifier = { center ->
                Modifier.magnifier(
                    sourceCenter = { center() },
                    onSizeChanged = { size ->
                        magnifierSize =
                            with(density) {
                                IntSize(size.width.roundToPx(), size.height.roundToPx())
                            }
                    },
                    useTextDefault = true,
                    platformMagnifierFactory = PlatformMagnifierFactory.getForCurrentPlatform(),
                )
            },
        )
    }
}

internal actual fun Modifier.addBasicTextFieldTextContextMenuComponents(
    manager: TextFieldSelectionManager,
    coroutineScope: CoroutineScope,
): Modifier = addTextContextMenuComponentsWithResources { resources ->
    fun TextContextMenuBuilderScope.textFieldItem(
        item: TextContextMenuItems,
        enabled: Boolean,
        closePredicate: (() -> Boolean)? = null,
        onClick: () -> Unit,
    ) {
        textItem(resources, item, enabled) {
            onClick()
            if (closePredicate?.invoke() ?: true) close()
        }
    }

    fun TextContextMenuBuilderScope.textFieldSuspendItem(
        item: TextContextMenuItems,
        enabled: Boolean,
        onClick: suspend () -> Unit,
    ) {
        textFieldItem(item, enabled) {
            coroutineScope.launch(start = CoroutineStart.UNDISPATCHED) { onClick() }
        }
    }

    with(manager) {
        separator()
        textFieldSuspendItem(Cut, enabled = canCut()) { cut() }
        textFieldSuspendItem(Copy, enabled = canCopy()) { copy(cancelSelection = textToolbarShown) }
        textFieldSuspendItem(Paste, enabled = canPaste()) { paste() }
        textFieldItem(SelectAll, enabled = canSelectAll(), closePredicate = { !textToolbarShown }) {
            selectAll()
        }
        if (Build.VERSION.SDK_INT >= 26) {
            textFieldItem(Autofill, enabled = canAutofill()) { autofill() }
        }
        separator()
    }
}

internal fun TextFieldSelectionManager.contextMenuBuilder(
    contextMenuState: ContextMenuState,
    itemsAvailability: State<MenuItemsAvailability>,
): ContextMenuScope.() -> Unit = {
    fun textFieldItem(label: TextContextMenuItems, enabled: Boolean, operation: () -> Unit) {
        TextItem(contextMenuState, label, enabled, operation)
    }

    val availability: MenuItemsAvailability = itemsAvailability.value
    textFieldItem(Cut, enabled = availability.canCut) { cut() }
    textFieldItem(Copy, enabled = availability.canCopy) { copy(cancelSelection = false) }
    textFieldItem(Paste, enabled = availability.canPaste) { paste() }
    textFieldItem(SelectAll, enabled = availability.canSelectAll) { selectAll() }
    if (Build.VERSION.SDK_INT >= 26) {
        textFieldItem(Autofill, enabled = availability.canAutofill) { autofill() }
    }
}
