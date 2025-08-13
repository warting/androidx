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

package androidx.pdf.annotation.draftstate

import android.graphics.RectF
import androidx.pdf.annotation.models.EditId
import androidx.pdf.annotation.models.PathPdfObject
import androidx.pdf.annotation.models.StampAnnotation
import androidx.pdf.annotation.models.StampAnnotationTest.Companion.getSampleStampAnnotation
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MemoryAnnotationEditsDraftStateTest {

    private lateinit var draftState: MemoryAnnotationEditsDraftState

    @Before
    fun setUp() {
        draftState = MemoryAnnotationEditsDraftState()
    }

    @Test
    fun getEdits_forPageWithNoAnnotations_returnsEmptyList() {
        val savedAnnotations = draftState.getEdits(5)
        assertThat(savedAnnotations).isEmpty()
    }

    @Test(expected = NoSuchElementException::class)
    fun removeEdit_nonExistingEditIdOnExistingPage_throwsNoSuchElementException() {
        val pageNum = 0
        // Add an annotation to ensure the page exists in editState
        draftState.addEdit(getSampleStampAnnotation(pageNum))
        val nonExistentEditId = EditId(pageNum, "non-existent-id")

        // Should throw NoSuchElementException
        draftState.removeEdit(nonExistentEditId)
    }

    @Test(expected = NoSuchElementException::class)
    fun removeEdit_editIdForNonExistingPage_throwsNoSuchElementException() {
        val pageNum = 100
        // Page 100 doesn't exist and have no edits.
        val nonExistentEditId = EditId(pageNum, "id-on-non-existent-page")

        // Should throw NoSuchElementException
        draftState.removeEdit(nonExistentEditId)
    }

    @Test(expected = NoSuchElementException::class)
    fun updateEdit_nonExistingEditIdOnExistingPage_throwsNoSuchElementException() {
        val pageNum = 0
        // Add an annotation to ensure the page exists in editState
        draftState.addEdit(getSampleStampAnnotation(pageNum))

        val nonExistentEditId = EditId(pageNum, "non-existent-id")
        val updateAnnotation = getSampleStampAnnotation(pageNum)

        // Should throw NoSuchElementException
        draftState.updateEdit(nonExistentEditId, updateAnnotation)
    }

    @Test(expected = NoSuchElementException::class)
    fun updateEdit_editIdForNonExistingPage_throwsNoSuchElementException() {
        val pageNum = 100
        // Page 100 doesn't exist and have no edits.
        val nonExistentEditId = EditId(pageNum, "id-on-non-existent-page")
        val updateDataAnnotation = getSampleStampAnnotation(pageNum)

        // Should throw NoSuchElementException
        draftState.updateEdit(nonExistentEditId, updateDataAnnotation)
    }

    @Test
    fun addEdit_addAnAnnotation_returnsCorrectAnnotation() {
        val expectedPageNum = 1
        val expectedAnnotations = getSampleStampAnnotation(expectedPageNum)
        val editId = draftState.addEdit(expectedAnnotations)

        val pageEdits = draftState.getPageEditsForId(editId)
        val actualAnnotation = pageEdits[editId]
        assertThat(actualAnnotation).isNotNull()
        assertThat(actualAnnotation?.annotation).isInstanceOf(StampAnnotation::class.java)
        if (actualAnnotation?.annotation is StampAnnotation) {
            assertStampAnnotationEquals(expectedAnnotations, actualAnnotation.annotation)
        }
    }

    @Test
    fun addEdit_addMultipleAnnotationsOnDifferentPages_storesInCorrectPageMaps() {
        val bounds1 = RectF(5f, 5f, 15f, 15f)
        val bounds2 = RectF(30f, 30f, 40f, 40f)
        val bounds3 = RectF(25f, 25f, 35f, 35f)
        val expectedAnnotation1 = getSampleStampAnnotation(0, bounds1)
        val expectedAnnotation2 = getSampleStampAnnotation(0, bounds2)
        val expectedAnnotation3 = getSampleStampAnnotation(1, bounds3)

        val editId1 = draftState.addEdit(expectedAnnotation1)
        val editId2 = draftState.addEdit(expectedAnnotation2)
        val editId3 = draftState.addEdit(expectedAnnotation3)

        val page0Edits = draftState.getPageEditsForId(editId1)
        assertThat(page0Edits).isNotNull()

        assertThat(page0Edits).hasSize(2)
        val actualAnnotation1 = page0Edits[editId1]
        val actualAnnotation2 = page0Edits[editId2]
        assertThat(actualAnnotation1).isNotNull()
        assertThat(actualAnnotation2).isNotNull()
        assertThat(actualAnnotation1!!.annotation).isInstanceOf(StampAnnotation::class.java)
        assertThat(actualAnnotation2!!.annotation).isInstanceOf(StampAnnotation::class.java)
        assertStampAnnotationEquals(
            expectedAnnotation1,
            actualAnnotation1.annotation as StampAnnotation,
        )
        assertStampAnnotationEquals(
            expectedAnnotation2,
            actualAnnotation2.annotation as StampAnnotation,
        )

        val page1Edits = draftState.getPageEditsForId(editId3)
        assertThat(page1Edits).isNotNull()
        assertThat(page1Edits).hasSize(1)
        val actualAnnotation3 = page1Edits[editId3]
        assertThat(actualAnnotation3).isNotNull()
        assertThat(actualAnnotation3!!.annotation).isInstanceOf(StampAnnotation::class.java)
        assertStampAnnotationEquals(
            expectedAnnotation3,
            actualAnnotation3.annotation as StampAnnotation,
        )
    }

    @Test
    fun removeEdit_existingAnnotation_removesAndReturnsCorrectSavedAnnotation() {
        val pageNum = 0
        val annotationBounds = RectF(1f, 2f, 3f, 4f)
        val annotation = getSampleStampAnnotation(pageNum, annotationBounds)
        val editId = draftState.addEdit(annotation)
        val editId2 = draftState.addEdit(getSampleStampAnnotation(pageNum))

        // Ensure it's there before removal
        assertThat(draftState.getPageEditsForId(editId).containsKey(editId)).isTrue()

        val removedAnnotation = draftState.removeEdit(editId)
        assertThat(removedAnnotation).isNotNull()
        assertThat(removedAnnotation).isInstanceOf(StampAnnotation::class.java)
        assertStampAnnotationEquals(annotation, removedAnnotation as StampAnnotation)

        val pageEditsMap = draftState.getPageEditsForId(editId2)
        // The map itself might still exist if other edits are on the page, or be null/empty
        assertThat(pageEditsMap.containsKey(editId)).isFalse()
    }

    @Test
    fun updateEdit_existingAnnotation_updatesAndReturnsNewSavedAnnotation() {
        val pageNum = 0
        val initialBounds = RectF(10f, 20f, 30f, 40f)
        val initialAnnotation = getSampleStampAnnotation(pageNum, initialBounds)
        val editId = draftState.addEdit(initialAnnotation)

        val expectedBounds = RectF(50f, 60f, 70f, 80f)
        val expectedAnnotation = getSampleStampAnnotation(pageNum, expectedBounds)

        val actualAnnotation = draftState.updateEdit(editId, expectedAnnotation)
        assertThat(actualAnnotation).isNotNull()
        assertThat(actualAnnotation).isInstanceOf(StampAnnotation::class.java)
        assertStampAnnotationEquals(expectedAnnotation, actualAnnotation as StampAnnotation)

        val retrievedAnnotationAfterUpdate = draftState.getPageEditsForId(editId)[editId]
        assertThat(retrievedAnnotationAfterUpdate).isNotNull()
        assertThat(retrievedAnnotationAfterUpdate!!.annotation)
            .isInstanceOf(StampAnnotation::class.java)
        assertStampAnnotationEquals(
            expectedAnnotation,
            retrievedAnnotationAfterUpdate.annotation as StampAnnotation,
        )
    }

    @Test
    fun getEdits_forPageWithAnnotations_returnsCorrectSavedAnnotations() {
        val pageNum = 2
        val expectedBounds1 = RectF(1f, 1f, 2f, 2f)
        val expectedAnnotation1 = getSampleStampAnnotation(pageNum, expectedBounds1)

        val expectedBounds2 = RectF(3f, 3f, 4f, 4f)
        val expectedAnnotation2 = getSampleStampAnnotation(pageNum, expectedBounds2)

        draftState.addEdit(expectedAnnotation1)
        draftState.addEdit(expectedAnnotation2)

        val actualAnnotations = draftState.getEdits(pageNum)
        assertThat(actualAnnotations).hasSize(2)

        // Since order is not guaranteed to be same, it is best to do find
        val actualAnnotation1 =
            actualAnnotations.find { (it.annotation as StampAnnotation).bounds == expectedBounds1 }
        val actualAnnotation2 =
            actualAnnotations.find { (it.annotation as StampAnnotation).bounds == expectedBounds2 }

        assertThat(actualAnnotation1).isNotNull()
        assertThat(actualAnnotation2).isNotNull()

        assertStampAnnotationEquals(
            expectedAnnotation1,
            actualAnnotation1!!.annotation as StampAnnotation,
        )
        assertStampAnnotationEquals(
            expectedAnnotation2,
            actualAnnotation2!!.annotation as StampAnnotation,
        )
    }

    private fun assertStampAnnotationEquals(
        expectedAnnotation: StampAnnotation,
        actualAnnotation: StampAnnotation,
    ) {
        assertThat(actualAnnotation.bounds).isEqualTo(expectedAnnotation.bounds)
        assertThat(actualAnnotation.pageNum).isEqualTo(expectedAnnotation.pageNum)
        assertThat(actualAnnotation.pdfObjects).hasSize(expectedAnnotation.pdfObjects.size)
        for (i in expectedAnnotation.pdfObjects.indices) {
            val actualPathPdfObject = actualAnnotation.pdfObjects[i] as PathPdfObject
            val expectedPathPdfObject = expectedAnnotation.pdfObjects[i] as PathPdfObject
            assertThat(actualPathPdfObject.brushColor).isEqualTo(expectedPathPdfObject.brushColor)
            assertThat(actualPathPdfObject.inputs).hasSize(expectedPathPdfObject.inputs.size)
            for (j in actualPathPdfObject.inputs.indices) {
                val actualPathInput = actualPathPdfObject.inputs[j]
                val expectedPathInput = expectedPathPdfObject.inputs[j]
                assertThat(actualPathInput.x).isEqualTo(expectedPathInput.x)
                assertThat(actualPathInput.y).isEqualTo(expectedPathInput.y)
            }
        }
    }
}
