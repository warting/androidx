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

package androidx.health.connect.client.records

import android.annotation.SuppressLint
import android.net.Uri
import androidx.annotation.RestrictTo
import androidx.health.connect.client.feature.withPhrFeatureCheck
import androidx.health.connect.client.impl.platform.records.PlatformMedicalDataSource
import androidx.health.connect.client.impl.platform.records.PlatformMedicalDataSourceBuilder
import java.time.Instant

/**
 * Captures the data source information of medical data. Each [FhirResource] is associated with a
 * `MedicalDataSource`.
 *
 * The medical data is represented using the Fast Healthcare Interoperability Resources
 * ([FHIR](https://hl7.org/fhir/)) standard.
 *
 * This feature is dependent on the version of HealthConnect installed on the device. To check if
 * it's available call [HealthConnectFeatures.getFeatureStatus] and pass
 * [HealthConnectFeatures.FEATURE_PERSONAL_HEALTH_RECORD] as an argument.
 */
// TODO(b/382278995): remove @RestrictTo and internal to unhide PHR APIs
@RestrictTo(RestrictTo.Scope.LIBRARY)
class MedicalDataSource(
    /**
     * The unique identifier of this `MedicalDataSource`, assigned by the Android Health Platform at
     * insertion time.
     */
    val id: String,
    /**
     * The package name of the contributing package. Auto-populated by the platform at source
     * creation time.
     */
    val packageName: String,
    /** The fhir base URI of this `MedicalDataSource`. */
    val fhirBaseUri: Uri,
    /** The display name that describes this `MedicalDataSource`. */
    val displayName: String,
    val fhirVersion: FhirVersion,
    /**
     * The time [FhirResource]s linked to this `MedicalDataSource` were last updated in Health
     * Connect.
     */
    val lastDataUpdateTime: Instant?
) {
    @SuppressLint("NewApi") // already checked with a feature availability check
    internal val platformMedicalDataSource: PlatformMedicalDataSource =
        withPhrFeatureCheck(this::class) {
            PlatformMedicalDataSourceBuilder(
                    id,
                    packageName,
                    fhirBaseUri,
                    displayName,
                    fhirVersion.platformFhirVersion
                )
                .setLastDataUpdateTime(lastDataUpdateTime)
                .build()
        }

    override fun toString() =
        toString(
            this,
            mapOf(
                "id" to id,
                "packageName" to packageName,
                "fhirBaseUri" to fhirBaseUri,
                "displayName" to displayName,
                "fhirVersion" to fhirVersion,
                "lastDataUpdateTime" to lastDataUpdateTime,
            )
        )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MedicalDataSource

        if (id != other.id) return false
        if (packageName != other.packageName) return false
        if (fhirBaseUri != other.fhirBaseUri) return false
        if (displayName != other.displayName) return false
        if (fhirVersion != other.fhirVersion) return false
        if (lastDataUpdateTime != other.lastDataUpdateTime) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + packageName.hashCode()
        result = 31 * result + fhirBaseUri.hashCode()
        result = 31 * result + displayName.hashCode()
        result = 31 * result + fhirVersion.hashCode()
        result = 31 * result + (lastDataUpdateTime?.hashCode() ?: 0)
        return result
    }
}
