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

package androidx.pdf.annotation.models

import android.annotation.SuppressLint
import android.os.Parcel
import android.os.Parcelable
import androidx.annotation.RestrictTo

/**
 * Represents the result of an Add PdfEdit operation.
 *
 * @property success A list of [JetpackAospIdPair] representing the jetpack and aosp ids
 * @property failures A list of [EditId] representing the jetpackIds that failed to be processed.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
@SuppressLint("BanParcelableUsage")
public class AddEditResult(
    public val success: List<JetpackAospIdPair>,
    public val failures: List<EditId>,
) : Parcelable {

    /** Default implementation for [Parcelable.describeContents], returning 0. */
    override fun describeContents(): Int = 0

    /** Flattens this object in to a Parcel. */
    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeInt(success.size)
        success.forEach { jetpackAospIdPair ->
            jetpackAospIdPair.jetpackId.writeToParcel(dest, flags)
            jetpackAospIdPair.aospId.writeToParcel(dest, flags)
        }
        dest.writeInt(failures.size)
        failures.forEach { it.writeToParcel(dest, flags) }
    }

    /** Companion object for creating [AddEditResult] instances from a [Parcel]. */
    public companion object {
        @JvmField
        public val CREATOR: Parcelable.Creator<AddEditResult> =
            object : Parcelable.Creator<AddEditResult> {
                /**
                 * Creates an [AddEditResult] instance from a [Parcel].
                 *
                 * @param parcel The parcel to read the object's data from.
                 * @return A new instance of [AddEditResult], or null if creation fails.
                 */
                override fun createFromParcel(parcel: Parcel): AddEditResult? {
                    val successSize = parcel.readInt()
                    val success = mutableListOf<JetpackAospIdPair>()
                    for (i in 0 until successSize) {
                        val jetpackId = EditId.CREATOR.createFromParcel(parcel)
                        val aospId = EditId.CREATOR.createFromParcel(parcel)
                        success.add(JetpackAospIdPair(jetpackId, aospId))
                    }
                    val failuresSize = parcel.readInt()
                    val failures = mutableListOf<EditId>()
                    for (i in 0 until failuresSize) {
                        EditId.CREATOR.createFromParcel(parcel)?.let { editId ->
                            failures.add(editId)
                        }
                    }
                    return AddEditResult(success, failures)
                }

                /**
                 * Creates a new array of [AddEditResult].
                 *
                 * @param size The size of the array.
                 * @return An array of [AddEditResult] of the specified size, with all elements
                 *   initialized to null.
                 */
                override fun newArray(size: Int): Array<AddEditResult?> {
                    return arrayOfNulls(size)
                }
            }
    }
}

/**
 * Represents a pair of Jetpack and AOSP Edit IDs.
 *
 * @property jetpackId The Jetpack [EditId].
 * @property aospId The AOSP [EditId].
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public data class JetpackAospIdPair(val jetpackId: EditId, val aospId: EditId)
