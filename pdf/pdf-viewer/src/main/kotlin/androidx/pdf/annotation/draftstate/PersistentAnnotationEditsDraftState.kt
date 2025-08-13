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

import android.os.ParcelFileDescriptor
import androidx.annotation.RestrictTo
import androidx.pdf.annotation.models.EditId
import androidx.pdf.annotation.models.PdfAnnotation
import androidx.pdf.annotation.models.PdfAnnotationData

/**
 * Responsible for persisting draft edits to a [ParcelFileDescriptor].
 *
 * @property bufferPfd The [ParcelFileDescriptor] used for persistence.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class PersistentAnnotationEditsDraftState(public val bufferPfd: ParcelFileDescriptor) :
    MemoryAnnotationEditsDraftState() {
    // TODO: Implement persistence logic using the parcelFileDescriptor

    override fun getEdits(pageNum: Int): List<PdfAnnotationData> {
        return super.getEdits(pageNum)
    }

    override fun addEdit(annotation: PdfAnnotation): EditId {
        return super.addEdit(annotation)
    }

    override fun removeEdit(editId: EditId): PdfAnnotation {
        return super.removeEdit(editId)
    }

    override fun updateEdit(editId: EditId, annotation: PdfAnnotation): PdfAnnotation {
        return super.updateEdit(editId, annotation)
    }
}
