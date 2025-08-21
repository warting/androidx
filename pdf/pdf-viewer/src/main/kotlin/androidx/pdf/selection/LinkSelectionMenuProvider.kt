/*
 * Copyright 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package androidx.pdf.selection

import android.content.Context
import androidx.pdf.util.ClipboardUtils

/** Provides default menu items for link selections. */
internal object LinkSelectionMenuProvider {
    internal fun getDefaultMenuItems(context: Context): List<ContextMenuComponent> {
        val defaultMenuItems =
            listOf<ContextMenuComponent>(
                DefaultSelectionMenuComponent(
                    key = PdfSelectionMenuKeys.CopyKey,
                    label = context.getString(android.R.string.copy),
                ) { pdfView ->
                    // We can't copy the current selection if no text is selected
                    val text = (pdfView.currentSelection as? LinkSelection)?.linkText
                    if (text != null) ClipboardUtils.copyToClipboard(context, text.toString())
                    // close the context menu upon copy action
                    close()
                    // After completion of action the selection should be cleared.
                    pdfView.clearSelection()
                },
                DefaultSelectionMenuComponent(
                    key = PdfSelectionMenuKeys.SelectAllKey,
                    label = context.getString(android.R.string.selectAll),
                ) { pdfView ->
                    val page = pdfView.currentSelection?.bounds?.first()?.pageNum
                    // We can't select all if we don't know what page the selection is on, or if
                    // we don't know the size of that page
                    if (page != null) {
                        // Action mode for old selection should be closed which will be triggered
                        // after select all is completed with current selection.
                        close()
                        pdfView.selectAllTextOnPage(page)
                    }
                },
            )
        return defaultMenuItems
    }
}
