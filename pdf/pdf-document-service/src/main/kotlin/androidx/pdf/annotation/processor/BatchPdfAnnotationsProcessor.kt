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

package androidx.pdf.annotation.processor

import android.os.Parcel
import android.os.Parcelable
import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import androidx.pdf.PdfDocumentRemote
import androidx.pdf.annotation.models.AddEditResult
import androidx.pdf.annotation.models.AnnotationResult
import androidx.pdf.annotation.models.EditId
import androidx.pdf.annotation.models.ModifyEditResult
import androidx.pdf.annotation.models.PdfAnnotationData
import androidx.pdf.annotation.models.PdfEditResult

/**
 * A processor for handling a list of [PdfAnnotationData] objects by batching them and applying the
 * edits to a remote PDF document.
 *
 * @property remoteDocument The [PdfDocumentRemote] interface used to apply the annotation edits.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class BatchPdfAnnotationsProcessor(private val remoteDocument: PdfDocumentRemote) :
    PdfAnnotationsProcessor {

    /**
     * Processes a list of annotations by applying them to the remote PDF document in batches.
     *
     * This method prevents large lists of annotations from causing [TransactionTooLargeException]
     * when sent over an AIDL connection. It splits the list into smaller batches based on a maximum
     * size limit and processes each batch individually. The results from each batch are then
     * combined into a single [AnnotationResult].
     *
     * @param annotations The list of [PdfAnnotationData] objects to be applied.
     * @return An [AnnotationResult] containing the combined list of successfully applied
     *   annotations and the list of failed annotations.
     */
    override fun process(annotations: List<PdfAnnotationData>): AnnotationResult {
        return processInBatches(
            annotations,
            remoteDocument::applyEdits,
            { success, failures -> AnnotationResult(success, failures) },
        )
    }

    override fun processAddEdits(annotations: List<PdfAnnotationData>): AddEditResult {
        return processInBatches(
            annotations,
            remoteDocument::addEdit,
            { success, failures -> AddEditResult(success, failures) },
        )
    }

    override fun processUpdateEdits(annotations: List<PdfAnnotationData>): ModifyEditResult {
        return processInBatches(
            annotations,
            remoteDocument::updateEdit,
            { success, failures -> ModifyEditResult(success, failures) },
        )
    }

    override fun processRemoveEdits(editIds: List<EditId>): ModifyEditResult {
        return processInBatches(
            editIds,
            remoteDocument::removeEdit,
            { success, failures -> ModifyEditResult(success, failures) },
        )
    }

    private fun <T : Parcelable, S, F, R> processInBatches(
        items: List<T>,
        compute: (List<T>) -> R,
        resultFactory: (success: List<S>, failures: List<F>) -> R,
    ): R where R : PdfEditResult<S, F> {

        val emptyResult = resultFactory(emptyList(), emptyList())
        if (items.isEmpty()) {
            return emptyResult
        }

        val batches = items.unflatten(MAX_BATCH_SIZE_IN_BYTES)

        // The operation here applies each annotation batch and the result of each operation is
        // folded into a single [AnnotationResult].
        return batches.fold(emptyResult) { accumulator, batch ->
            val newResult = compute(batch)

            // Combine the previous results with the new results.
            val combinedSuccesses = accumulator.success + newResult.success
            val combinedFailures = accumulator.failures + newResult.failures
            resultFactory(combinedSuccesses, combinedFailures)
        }
    }

    public companion object {
        public const val MAX_BATCH_SIZE_IN_BYTES: Int = 1000000

        /**
         * Used to expand a 1D list to a 2D list of annotations based on [maxSizeInBytes].
         *
         * If the accumulated size if greater or equal to the [maxSizeInBytes] then the item is
         * added to a new sublist else it is added to the existing one.
         *
         * @param maxSizeInBytes max size limit for each sublist
         * @return 2D list divided into list of sublists
         */
        @VisibleForTesting
        internal fun <T : Parcelable> List<T>.unflatten(maxSizeInBytes: Int): List<List<T>> {
            return this.fold(emptyList()) { acc, edits ->
                val lastSublist = acc.lastOrNull() ?: listOf()
                val editsSize = edits.parcelSizeInBytes()

                val newListSizeInBytes = lastSublist.sumOf { it.parcelSizeInBytes() } + editsSize

                if (newListSizeInBytes <= maxSizeInBytes) {
                    // Add the item to the last sublist
                    val newLastSublist = lastSublist + edits
                    acc.dropLast(1) + listOf(newLastSublist)
                } else if (editsSize > maxSizeInBytes) {
                    acc
                } else {
                    acc + listOf(listOf(edits))
                }
            }
        }

        /**
         * Calculates the size of a [Parcelable] object when flattened into a [Parcel].
         *
         * @return The size in bytes of the `Parcelable` object when written to a [Parcel].
         */
        @VisibleForTesting
        internal fun Parcelable.parcelSizeInBytes(): Int {
            val parcel = Parcel.obtain()
            this.writeToParcel(parcel, 0)
            val size = parcel.dataSize()
            parcel.recycle()
            return size
        }
    }
}
