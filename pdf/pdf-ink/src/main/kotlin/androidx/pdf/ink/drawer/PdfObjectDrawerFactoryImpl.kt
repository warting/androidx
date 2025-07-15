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

package androidx.pdf.ink.drawer

import androidx.pdf.annotation.drawer.PdfObjectDrawer
import androidx.pdf.annotation.drawer.PdfObjectDrawerFactory
import androidx.pdf.annotation.models.PathPdfObject
import androidx.pdf.annotation.models.PdfObject

/**
 * A factory implementation for creating [PdfObjectDrawer] instances specifically for ink-based
 * implementations
 */
internal object PdfObjectDrawerFactoryImpl : PdfObjectDrawerFactory {

    /**
     * Creates a [PdfObjectDrawer] for the given [pdfObject].
     *
     * @param pdfObject The [PdfObject] for which to create a drawer.
     * @return A [PdfObjectDrawer] capable of drawing the given [pdfObject].
     */
    override fun create(pdfObject: PdfObject): PdfObjectDrawer<out PdfObject> {
        return when (pdfObject) {
            is PathPdfObject -> PathPdfObjectDrawer
        }
    }
}
