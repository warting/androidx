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

package androidx.pdf.view

/**
 * Performs actions in response to keyboard shortcuts detected by [PdfViewExternalInputManager]
 *
 * @param pdfView The view on which the actions are to be taken.
 */
internal class PdfViewKeyboardActionHandler(pdfView: PdfView) :
    PdfViewExternalInputHandler(pdfView) {

    override val verticalScrollFactor = VERTICAL_SCROLL_FACTOR
    override val horizontalScrollFactor = HORIZONTAL_SCROLL_FACTOR

    fun zoomIn() {
        val pivotX = (pdfView.left + pdfView.right) / 2f
        val pivotY = pdfView.top.toFloat()
        zoomIn(pivotX, pivotY)
    }

    fun zoomOut() {
        val pivotX = (pdfView.left + pdfView.right) / 2f
        val pivotY = pdfView.top.toFloat()
        zoomOut(pivotX, pivotY)
    }

    private companion object {
        const val VERTICAL_SCROLL_FACTOR = 20
        const val HORIZONTAL_SCROLL_FACTOR = 20
    }
}
