/*
 * Copyright (C) 2025 The Android Open Source Project
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
import androidx.annotation.RestrictTo
import androidx.health.connect.client.HealthConnectFeatures
import androidx.health.connect.client.feature.withPhrFeatureCheck
import androidx.health.connect.client.impl.platform.records.PlatformMedicalResourceId
import androidx.health.connect.client.impl.platform.records.toPlatformFhirResourceType
import androidx.health.connect.client.impl.platform.records.toSdkMedicalResourceId
import androidx.health.connect.client.records.FhirResource.Companion.FhirResourceType

/**
 * A class to represent a unique identifier of a medical resource.
 *
 * This class contains a set of properties that together represent a unique identifier of a medical
 * resource.
 *
 * The medical resource data representation follows the
 * [Fast Healthcare Interoperability Resources (FHIR)](https://hl7.org/fhir/) standard.
 *
 * This feature is dependent on the version of HealthConnect installed on the device. To check if
 * it's available call [HealthConnectFeatures.getFeatureStatus] and pass
 * [HealthConnectFeatures.FEATURE_PERSONAL_HEALTH_RECORD] as an argument.
 */
// TODO(b/382278995): remove @RestrictTo to unhide PHR APIs
@RestrictTo(RestrictTo.Scope.LIBRARY)
class MedicalResourceId(
    /** Represents ID of a medical data source where the data comes from. */
    val dataSourceId: String,

    /** A [FhirResourceType] indicating the type of the FHIR resource. */
    @FhirResourceType val fhirResourceType: Int,

    /** Represents ID of the FHIR resource. This must be unique per medical data source. */
    val fhirResourceId: String
) {
    @SuppressLint("NewApi") // already checked with a feature availability check
    internal val platformMedicalResourceId: PlatformMedicalResourceId =
        withPhrFeatureCheck(MedicalResourceId::class) {
            PlatformMedicalResourceId(
                dataSourceId,
                fhirResourceType.toPlatformFhirResourceType(),
                fhirResourceId
            )
        }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is MedicalResourceId) return false

        if (fhirResourceType != other.fhirResourceType) return false
        if (dataSourceId != other.dataSourceId) return false
        if (fhirResourceId != other.fhirResourceId) return false

        return true
    }

    override fun hashCode(): Int {
        var result = fhirResourceType
        result = 31 * result + dataSourceId.hashCode()
        result = 31 * result + fhirResourceId.hashCode()
        return result
    }

    override fun toString() =
        toString(
            this,
            mapOf(
                "dataSourceId" to dataSourceId,
                "fhirResourceType" to fhirResourceType,
                "fhirResourceId" to fhirResourceId
            )
        )

    companion object {
        /**
         * Creates a [MedicalResourceId] instance from `dataSourceId` and `fhirReference`.
         *
         * This method is dependent on the version of HealthConnect installed on the device. To
         * check if it's available call [HealthConnectFeatures.getFeatureStatus] and pass
         * [HealthConnectFeatures.FEATURE_PERSONAL_HEALTH_RECORD] as an argument.
         *
         * @param dataSourceId Represents ID of a medical data source where the data comes from.
         * @param fhirReference The FHIR reference string typically extracted from the "reference"
         *   field in one FHIR resource (source), pointing to another FHIR resource (target) within
         *   the same data source, for example "Patient/034AB16".
         * @throws IllegalArgumentException if the provided `dataSourceId` is not a valid ID, the
         *   referenced resource type is not a valid [FhirResource] type supported by Health
         *   Connect, or `fhirReference` does not match with the pattern of
         *   `$fhir_resource_type/$fhir_resource_id`, where the FHIR resource type should align with
         *   the resource list in
         *   [the official FHIR website](https://build.fhir.org/resourcelist.html), and the FHIR
         *   resource ID should also follow the pattern described in
         *   [the official FHIR datatypes](https://build.fhir.org/datatypes.html#id).
         */
        @SuppressLint("NewApi") // checked with feature availability
        fun fromFhirReference(dataSourceId: String, fhirReference: String): MedicalResourceId =
            withPhrFeatureCheck(MedicalResourceId::class, "fromFhirReference") {
                PlatformMedicalResourceId.fromFhirReference(dataSourceId, fhirReference)
                    .toSdkMedicalResourceId()
            }
    }
}
