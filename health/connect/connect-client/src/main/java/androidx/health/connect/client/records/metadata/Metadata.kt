/*
 * Copyright 2022 The Android Open Source Project
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
package androidx.health.connect.client.records.metadata

import androidx.annotation.IntDef
import androidx.annotation.RestrictTo
import androidx.health.connect.client.records.Record
import java.time.Instant

/** Set of shared metadata fields for [Record]. */
@SuppressWarnings("NewApi") // Temporary until we can enable java8 desugaring effectively.
public class Metadata
internal constructor(
    /**
     * Client supplied data recording method to help to understand how the data was recorded.
     *
     * It should be one of the following: [RECORDING_METHOD_UNKNOWN],
     * [RECORDING_METHOD_ACTIVELY_RECORDED], [RECORDING_METHOD_AUTOMATICALLY_RECORDED] and
     * [RECORDING_METHOD_MANUAL_ENTRY].
     */
    @param:RecordingMethod @property:RecordingMethod @get:RecordingMethod val recordingMethod: Int,

    /**
     * Unique identifier of this data, assigned by Health Connect at insertion time. When [Record]
     * is created before insertion, this takes a sentinel value, any assigned value will be ignored.
     */
    public val id: String = EMPTY_ID,

    /**
     * Where the data comes from, such as application information originally generated this data.
     * When [Record] is created before insertion, this contains a sentinel value, any assigned value
     * will be ignored. After insertion, this will be populated with inserted application.
     */
    public val dataOrigin: DataOrigin = DataOrigin(""),

    /**
     * Automatically populated to when data was last modified (or originally created). When [Record]
     * is created before inserted, this contains a sentinel value, any assigned value will be
     * ignored.
     */
    public val lastModifiedTime: Instant = Instant.EPOCH,

    /**
     * Optional client supplied record unique data identifier associated with the data.
     *
     * There is guaranteed a single entry for any type of data with same client provided identifier
     * for a given client. Any new insertions with the same client provided identifier will either
     * replace or be ignored depending on associated [clientRecordVersion].
     *
     * @see clientRecordVersion
     */
    public val clientRecordId: String? = null,

    /**
     * Optional client supplied version associated with the data.
     *
     * This determines conflict resolution outcome when there are multiple insertions of the same
     * [clientRecordId]. Data with the highest [clientRecordVersion] takes precedence.
     * [clientRecordVersion] starts with 0.
     *
     * @see clientRecordId
     */
    public val clientRecordVersion: Long = 0,

    /** Optional client supplied device information associated with the data. */
    public val device: Device? = null,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Metadata) return false

        if (id != other.id) return false
        if (dataOrigin != other.dataOrigin) return false
        if (lastModifiedTime != other.lastModifiedTime) return false
        if (clientRecordId != other.clientRecordId) return false
        if (clientRecordVersion != other.clientRecordVersion) return false
        if (device != other.device) return false
        if (recordingMethod != other.recordingMethod) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + dataOrigin.hashCode()
        result = 31 * result + lastModifiedTime.hashCode()
        result = 31 * result + (clientRecordId?.hashCode() ?: 0)
        result = 31 * result + clientRecordVersion.hashCode()
        result = 31 * result + (device?.hashCode() ?: 0)
        result = 31 * result + recordingMethod.hashCode()
        return result
    }

    override fun toString(): String {
        return "Metadata(id='$id', dataOrigin=$dataOrigin, lastModifiedTime=$lastModifiedTime, clientRecordId=$clientRecordId, clientRecordVersion=$clientRecordVersion, device=$device, recordingMethod=$recordingMethod)"
    }

    companion object {
        internal const val EMPTY_ID: String = ""

        /** Unknown recording method. */
        const val RECORDING_METHOD_UNKNOWN = 0

        /**
         * For data actively recorded by the user.
         *
         * For e.g. An exercise session actively recorded by the user using a phone or a watch
         * device.
         *
         * [device] must be specified when using this recording method.
         */
        const val RECORDING_METHOD_ACTIVELY_RECORDED = 1

        /**
         * For data recorded passively by a device without user explicitly initiating the recording,
         * or whenever it cannot be determined.
         *
         * For e.g. Steps data recorded by a watch or phone without the user starting a session.
         *
         * [device] must be specified when using this recording method.
         */
        const val RECORDING_METHOD_AUTOMATICALLY_RECORDED = 2

        /**
         * For data manually entered by the user.
         *
         * For e.g. Nutrition or weight data entered by the user.
         */
        const val RECORDING_METHOD_MANUAL_ENTRY = 3

        /** List of possible Recording method for the [Record]. */
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        @IntDef(
            RECORDING_METHOD_UNKNOWN,
            RECORDING_METHOD_ACTIVELY_RECORDED,
            RECORDING_METHOD_AUTOMATICALLY_RECORDED,
            RECORDING_METHOD_MANUAL_ENTRY,
        )
        @Retention(AnnotationRetention.SOURCE)
        annotation class RecordingMethod

        /**
         * Creates Metadata for an actively recorded record.
         *
         * [RECORDING_METHOD_ACTIVELY_RECORDED] is auto populated.
         *
         * @param device The [Device] associated with the record.
         */
        @JvmStatic
        fun activelyRecorded(device: Device): Metadata =
            Metadata(recordingMethod = RECORDING_METHOD_ACTIVELY_RECORDED, device = device)

        /**
         * Creates Metadata for an actively recorded record with the provided client ID.
         *
         * [RECORDING_METHOD_ACTIVELY_RECORDED] is auto populated.
         *
         * @param device The [Device] associated with the record.
         * @param clientRecordId The client ID of the record.
         * @param clientRecordVersion The client version of the record (default: 0).
         */
        @JvmStatic
        @JvmOverloads
        fun activelyRecorded(
            device: Device,
            clientRecordId: String,
            clientRecordVersion: Long = 0,
        ): Metadata =
            Metadata(
                recordingMethod = RECORDING_METHOD_ACTIVELY_RECORDED,
                device = device,
                clientRecordId = clientRecordId,
                clientRecordVersion = clientRecordVersion,
            )

        /**
         * Creates Metadata to update a record with an existing UUID.
         *
         * [RECORDING_METHOD_ACTIVELY_RECORDED] is auto populated.
         *
         * Use this only when there's no client ID or version associated with the record.
         *
         * @param id The existing UUID of the record.
         * @param device The [Device] associated with the record.
         */
        @JvmStatic
        fun activelyRecordedWithId(id: String, device: Device): Metadata =
            Metadata(recordingMethod = RECORDING_METHOD_ACTIVELY_RECORDED, id = id, device = device)

        /**
         * Creates Metadata for an automatically recorded record.
         *
         * [RECORDING_METHOD_AUTOMATICALLY_RECORDED] is auto populated.
         *
         * @param device The [Device] associated with the record.
         */
        @JvmStatic
        fun autoRecorded(device: Device): Metadata =
            Metadata(recordingMethod = RECORDING_METHOD_AUTOMATICALLY_RECORDED, device = device)

        /**
         * Creates Metadata for an automatically recorded record with the provided client ID.
         *
         * [RECORDING_METHOD_AUTOMATICALLY_RECORDED] is auto populated.
         *
         * @param device The [Device] associated with the record.
         * @param clientRecordId The client ID of the record.
         * @param clientRecordVersion The client version of the record (default: 0).
         */
        @JvmStatic
        @JvmOverloads
        fun autoRecorded(
            device: Device,
            clientRecordId: String,
            clientRecordVersion: Long = 0,
        ): Metadata =
            Metadata(
                recordingMethod = RECORDING_METHOD_AUTOMATICALLY_RECORDED,
                device = device,
                clientRecordId = clientRecordId,
                clientRecordVersion = clientRecordVersion,
            )

        /**
         * Creates Metadata to update a record with an existing UUID.
         *
         * [RECORDING_METHOD_AUTOMATICALLY_RECORDED] is auto populated.
         *
         * Use this only when there's no client ID or version associated with the record.
         *
         * @param id The existing UUID of the record.
         * @param device The [Device] associated with the record.
         */
        @JvmStatic
        fun autoRecordedWithId(id: String, device: Device): Metadata =
            Metadata(
                recordingMethod = RECORDING_METHOD_AUTOMATICALLY_RECORDED,
                id = id,
                device = device,
            )

        /**
         * Creates Metadata for a manually entered record. Developers can provide optional device
         * information.
         *
         * [RECORDING_METHOD_MANUAL_ENTRY] is auto populated.
         *
         * @param device The optional [Device] associated with the record.
         */
        @JvmStatic
        @JvmOverloads
        fun manualEntry(device: Device? = null): Metadata =
            Metadata(recordingMethod = RECORDING_METHOD_MANUAL_ENTRY, device = device)

        /**
         * Creates Metadata for a manually entered record with the provided client ID. Developers
         * can provide optional device information.
         *
         * [RECORDING_METHOD_MANUAL_ENTRY] is auto populated.
         *
         * @param clientRecordId The client ID of the record.
         * @param clientRecordVersion The client version of the record (default: 0).
         * @param device The optional [Device] associated with the record.
         */
        @JvmStatic
        @JvmOverloads
        fun manualEntry(
            clientRecordId: String,
            clientRecordVersion: Long = 0,
            device: Device? = null,
        ): Metadata =
            Metadata(
                recordingMethod = RECORDING_METHOD_MANUAL_ENTRY,
                device = device,
                clientRecordId = clientRecordId,
                clientRecordVersion = clientRecordVersion,
            )

        /**
         * Creates Metadata to update a record with an existing UUID.
         *
         * [RECORDING_METHOD_MANUAL_ENTRY] is auto populated.
         *
         * Use this only when there's no client ID or version associated with the record.
         *
         * @param id The existing UUID of the record.
         * @param device The optional [Device] associated with the recording.
         */
        @JvmStatic
        @JvmOverloads
        fun manualEntryWithId(id: String, device: Device? = null): Metadata =
            Metadata(recordingMethod = RECORDING_METHOD_MANUAL_ENTRY, id = id, device = device)

        /**
         * Creates Metadata with unknown recording method.
         *
         * [RECORDING_METHOD_UNKNOWN] is auto populated.
         *
         * This should only be used in the case when the recording method is geniously unknown.
         *
         * @param device The optional [Device] associated with the record.
         */
        @JvmStatic
        @JvmOverloads
        fun unknownRecordingMethod(device: Device? = null): Metadata =
            Metadata(recordingMethod = RECORDING_METHOD_UNKNOWN, device = device)

        /**
         * Creates Metadata with unknown recording method with the provided client ID.
         *
         * [RECORDING_METHOD_UNKNOWN] is auto populated.
         *
         * This should only be used in the case when the recording method is geniously unknown.
         *
         * @param clientRecordId The client ID of the record.
         * @param clientRecordVersion The client version of the record (default: 0).
         * @param device The optional [Device] associated with the recording.
         */
        @JvmStatic
        @JvmOverloads
        fun unknownRecordingMethod(
            clientRecordId: String,
            clientRecordVersion: Long = 0,
            device: Device? = null,
        ): Metadata =
            Metadata(
                recordingMethod = RECORDING_METHOD_UNKNOWN,
                device = device,
                clientRecordId = clientRecordId,
                clientRecordVersion = clientRecordVersion,
            )

        /**
         * Creates Metadata to update a record with an existing UUID.
         *
         * [RECORDING_METHOD_UNKNOWN] is auto populated.
         *
         * This should only be used in the case when the recording method is geniously unknown.
         *
         * @param id The existing UUID of the record.
         * @param device The optional [Device] associated with the record.
         */
        @JvmStatic
        @JvmOverloads
        fun unknownRecordingMethodWithId(id: String, device: Device? = null): Metadata =
            Metadata(recordingMethod = RECORDING_METHOD_UNKNOWN, id = id, device = device)
    }
}
