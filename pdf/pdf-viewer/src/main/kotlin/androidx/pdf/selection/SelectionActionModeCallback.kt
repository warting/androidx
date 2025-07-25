/*
 * Copyright 2025 The Android Open Source Project
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
package androidx.pdf.selection

import android.graphics.Rect
import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.pdf.view.PdfView
import kotlin.math.roundToInt

internal class SelectionActionModeCallback(
    private val pdfView: PdfView,
    private val menuItems: List<ContextMenuComponent>,
) : ActionMode.Callback2(), SelectionMenuSession {
    internal var actionMode: ActionMode? = null
        private set

    private lateinit var selectionMenuItems: MutableList<ContextMenuComponent>

    override fun close() {
        actionMode?.finish()
        actionMode = null
    }

    override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {
        return false
    }

    override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
        actionMode = mode
        // Start afresh with the default menu items
        selectionMenuItems = menuItems.toMutableList()
        pdfView.selectionMenuItemPreparer?.onPrepareSelectionMenuItems(selectionMenuItems)
        selectionMenuItems.forEachIndexed { index, component ->
            when (component) {
                is DefaultSelectionMenuComponent -> {
                    val menuItem =
                        menu?.add(
                            /* groupId = */ Menu.NONE,
                            /* itemId = */ Menu.NONE,
                            /* order = */ Menu.NONE,
                            /* title = */ component.label,
                        )
                    component.contentDescription?.let { menuItem?.contentDescription = it }
                    menuItem?.setOnMenuItemClickListener {
                        component.onClick(this, pdfView)
                        true
                    }
                }
                is SelectionMenuComponent -> {
                    val menuItem =
                        menu?.add(
                            /* groupId = */ Menu.NONE,
                            /* itemId = */ Menu.NONE,
                            /* order = */ Menu.NONE,
                            /* title = */ component.label,
                        )
                    component.contentDescription?.let { menuItem?.contentDescription = it }
                    menuItem?.setOnMenuItemClickListener {
                        component.onClick(this)
                        true
                    }
                }
                is SmartSelectionMenuComponent -> {
                    // We have to use android.R.id.textAssist as item id to make action mode show
                    // icon for floating action bar.
                    val itemId: Int = if (index == 0) android.R.id.textAssist else Menu.NONE
                    val menuItem =
                        menu?.add(
                            /* groupId = */ Menu.NONE,
                            /* itemId = */ itemId,
                            /* order = */ Menu.NONE,
                            /* title = */ component.label,
                        )
                    component.contentDescription?.let { menuItem?.contentDescription = it }
                    component.leadingIcon?.let { menuItem?.icon = it }
                    menuItem?.setOnMenuItemClickListener {
                        component.onClick(this, pdfView)
                        true
                    }
                }
            }
        }
        return true
    }

    override fun onDestroyActionMode(mode: ActionMode?) {
        // No-op
    }

    override fun onGetContentRect(mode: ActionMode?, view: View?, outRect: Rect?) {
        // If we don't know about page layout, defer to the default implementation
        val localPageLayoutManager =
            pdfView.pageMetadataLoader ?: return super.onGetContentRect(mode, view, outRect)
        val viewport = pdfView.getVisibleAreaInContentCoords()
        val firstSelection = pdfView.currentSelection?.bounds?.firstOrNull()
        val lastSelection = pdfView.currentSelection?.bounds?.lastOrNull()
        // Try to position the context menu near the first selection if it's visible
        if (firstSelection != null) {
            // Copy bounds to avoid mutating the real data
            val boundsInView = localPageLayoutManager.getViewRect(firstSelection, viewport)
            if (
                boundsInView?.let { viewport.intersects(it.left, it.top, it.right, it.bottom) } ==
                    true
            ) {
                outRect?.set(pdfView.toViewRect(boundsInView))
                return
            }
        }
        // Else, try to position the context menu near the last selection if it's visible
        if (lastSelection != null) {
            // Copy bounds to avoid mutating the real data
            val boundsInView = localPageLayoutManager.getViewRect(lastSelection, viewport)
            if (
                boundsInView?.let { viewport.intersects(it.left, it.top, it.right, it.bottom) } ==
                    true
            ) {
                outRect?.set(pdfView.toViewRect(boundsInView))
                return
            }
        }
        // Else, center the context menu in view
        val centerX = (pdfView.x + pdfView.width / 2).roundToInt()
        val centerY = (pdfView.y + pdfView.height / 2).roundToInt()
        outRect?.set(centerX, centerY, centerX + 1, centerY + 1)
    }

    override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean {
        return false
    }
}
