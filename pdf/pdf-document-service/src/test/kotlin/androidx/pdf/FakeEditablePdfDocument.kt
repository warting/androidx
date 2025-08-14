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

package androidx.pdf

import android.graphics.PointF
import android.graphics.Rect
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.SparseArray
import androidx.pdf.annotation.EditablePdfDocument
import androidx.pdf.annotation.models.AnnotationResult
import androidx.pdf.annotation.models.EditId
import androidx.pdf.annotation.models.EditsResult
import androidx.pdf.annotation.models.PdfAnnotation
import androidx.pdf.annotation.models.PdfAnnotationData
import androidx.pdf.annotation.models.PdfEdit
import androidx.pdf.annotation.models.PdfEditEntry
import androidx.pdf.annotation.models.PdfEdits
import androidx.pdf.content.PageMatchBounds
import androidx.pdf.content.PageSelection
import androidx.pdf.models.FormEditRecord
import androidx.pdf.models.FormWidgetInfo
import java.util.UUID

/** Fake implementation of [androidx.pdf.annotation.EditablePdfDocument] for testing. */
internal class FakeEditablePdfDocument(
    override val uri: Uri,
    override val pageCount: Int,
    override val isLinearized: Boolean = false,
    override val formType: Int = -1,
    override val formEditRecords: List<FormEditRecord> = listOf(),
) : EditablePdfDocument() {
    private val annotationsByPage = mutableMapOf<Int, MutableList<PdfEditEntry<out PdfEdit>>>()

    val getAnnotationsForPageCallCount = mutableMapOf<Int, Int>()

    fun addAnnotationToPage(pageNum: Int, annotation: PdfAnnotation) {
        val editId = EditId(pageNum, UUID.randomUUID().toString())
        val data = PdfAnnotationData(editId, annotation)
        annotationsByPage.getOrPut(pageNum) { mutableListOf() }.add(data)
    }

    override suspend fun <T : PdfEditEntry<out PdfEdit>> getEditsForPage(pageNum: Int): List<T> {
        getAnnotationsForPageCallCount[pageNum] =
            getAnnotationsForPageCallCount.getOrDefault(pageNum, 0) + 1
        return (annotationsByPage[pageNum] ?: emptyList()) as List<T>
    }

    override suspend fun applyEdits(annotations: List<PdfAnnotationData>): AnnotationResult {
        TODO("Not yet implemented")
    }

    override suspend fun applyEdits(sourcePfd: ParcelFileDescriptor): AnnotationResult {
        TODO("Not yet implemented")
    }

    override fun addEdit(edit: PdfEdit): EditId {
        TODO("Not yet implemented")
    }

    override fun removeEdit(editId: EditId) {
        TODO("Not yet implemented")
    }

    override fun updateEdit(editId: EditId, edit: PdfEdit) {
        TODO("Not yet implemented")
    }

    override fun commitEdits(): EditsResult {
        TODO("Not yet implemented")
    }

    override fun getAllEdits(): PdfEdits {
        TODO("Not yet implemented")
    }

    override suspend fun getPageInfo(pageNumber: Int): PdfDocument.PageInfo {
        TODO("Not yet implemented")
    }

    override suspend fun getPageInfo(
        pageNumber: Int,
        pageInfoFlags: PdfDocument.PageInfoFlags,
    ): PdfDocument.PageInfo {
        TODO("Not yet implemented")
    }

    override suspend fun getPageInfos(pageRange: IntRange): List<PdfDocument.PageInfo> {
        TODO("Not yet implemented")
    }

    override suspend fun getPageInfos(
        pageRange: IntRange,
        pageInfoFlags: PdfDocument.PageInfoFlags,
    ): List<PdfDocument.PageInfo> {
        TODO("Not yet implemented")
    }

    override suspend fun searchDocument(
        query: String,
        pageRange: IntRange,
    ): SparseArray<List<PageMatchBounds>> {
        TODO("Not yet implemented")
    }

    override suspend fun getSelectionBounds(
        pageNumber: Int,
        start: PointF,
        stop: PointF,
    ): PageSelection? {
        TODO("Not yet implemented")
    }

    override suspend fun getSelectAllSelectionBounds(pageNumber: Int): PageSelection? {
        TODO("Not yet implemented")
    }

    override suspend fun getPageContent(pageNumber: Int): PdfDocument.PdfPageContent? {
        TODO("Not yet implemented")
    }

    override suspend fun getPageLinks(pageNumber: Int): PdfDocument.PdfPageLinks {
        TODO("Not yet implemented")
    }

    override fun getPageBitmapSource(pageNumber: Int): PdfDocument.BitmapSource {
        TODO("Not yet implemented")
    }

    override suspend fun getFormWidgetInfos(pageNum: Int): List<FormWidgetInfo> {
        TODO("Not yet implemented")
    }

    override suspend fun getFormWidgetInfos(pageNum: Int, types: IntArray): List<FormWidgetInfo> {
        TODO("Not yet implemented")
    }

    override suspend fun applyEdit(pageNum: Int, record: FormEditRecord): List<Rect> {
        TODO("Not yet implemented")
    }

    override suspend fun write(destination: ParcelFileDescriptor) {
        TODO("Not yet implemented")
    }

    override fun close() {
        TODO("Not yet implemented")
    }
}
