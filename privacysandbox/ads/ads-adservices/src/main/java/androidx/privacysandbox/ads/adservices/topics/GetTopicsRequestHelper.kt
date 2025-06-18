/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.privacysandbox.ads.adservices.topics

import android.os.Build
import android.os.ext.SdkExtensions
import androidx.annotation.RequiresExtension
import androidx.annotation.RestrictTo

/** Helper class to consolidate conversion logic for GetTopicsRequest. */
@RestrictTo(RestrictTo.Scope.LIBRARY)
object GetTopicsRequestHelper {
    @RequiresExtension(extension = SdkExtensions.AD_SERVICES, version = 5)
    @RequiresExtension(extension = Build.VERSION_CODES.S, version = 9)
    internal fun convertRequestWithRecordObservation(
        request: GetTopicsRequest
    ): android.adservices.topics.GetTopicsRequest {
        return android.adservices.topics.GetTopicsRequest.Builder()
            .setAdsSdkName(request.adsSdkName)
            .setShouldRecordObservation(request.shouldRecordObservation)
            .build()
    }

    @RequiresExtension(extension = SdkExtensions.AD_SERVICES, version = 4)
    @RequiresExtension(extension = Build.VERSION_CODES.S, version = 9)
    internal fun convertRequestWithoutRecordObservation(
        request: GetTopicsRequest
    ): android.adservices.topics.GetTopicsRequest {
        return android.adservices.topics.GetTopicsRequest.Builder()
            .setAdsSdkName(request.adsSdkName)
            .build()
    }
}
