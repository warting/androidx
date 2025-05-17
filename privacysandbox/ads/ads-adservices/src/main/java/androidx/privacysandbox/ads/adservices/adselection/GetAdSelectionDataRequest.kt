/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.privacysandbox.ads.adservices.adselection

import android.annotation.SuppressLint
import android.net.Uri
import android.os.Build
import android.os.ext.SdkExtensions
import androidx.annotation.RequiresExtension
import androidx.annotation.RestrictTo
import androidx.privacysandbox.ads.adservices.common.AdTechIdentifier
import androidx.privacysandbox.ads.adservices.common.ExperimentalFeatures
import androidx.privacysandbox.ads.adservices.internal.AdServicesInfo

/**
 * Represent input parameters to the [AdSelectionManager#getAdSelectionData] API.
 *
 * @param seller AdTechIdentifier of the seller, for example "www.example-ssp.com".
 * @param coordinatorOriginUri The coordinator origin Uri from which GetAdSelectionData API should
 *   fetch the decryption key. The origin must use HTTPS URI. The origin will only contain the
 *   scheme, hostname and port of the URL. If the origin is not provided or is null, PPAPI will use
 *   the default coordinator URI. The origin must belong to a list of pre-approved coordinator
 *   origins. Otherwise, [AdSelectionManager#getAdSelectionData] will throw an
 *   [IllegalArgumentException]. See <a
 *   href="https://developers.google.com/privacy-sandbox/relevance/aggregation-service#coordinator">
 *   Developer Guide</a> for more details.
 * @param sellerConfiguration the seller ad tech's requested payload configuration, set by the
 *   calling SDK, to optimize the payload.
 */
@OptIn(ExperimentalFeatures.Ext12OptIn::class, ExperimentalFeatures.Ext14OptIn::class)
@ExperimentalFeatures.Ext10OptIn
class GetAdSelectionDataRequest
@JvmOverloads
public constructor(
    val seller: AdTechIdentifier,
    // Note: public experimental properties are not allowed because the accessors will not appear
    // experimental to Java clients. There are public accessors for these properties below.
    @property:ExperimentalFeatures.Ext12OptIn private val coordinatorOriginUri: Uri? = null,
    @property:ExperimentalFeatures.Ext14OptIn
    private val sellerConfiguration: SellerConfiguration? = null,
) {
    /** Checks whether two [GetAdSelectionDataRequest] objects contain the same information. */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GetAdSelectionDataRequest) return false
        return this.seller == other.seller &&
            this.coordinatorOriginUri == other.coordinatorOriginUri &&
            this.sellerConfiguration == other.sellerConfiguration
    }

    /** Returns the hash of the [GetAdSelectionDataRequest] object's data. */
    override fun hashCode(): Int {
        var hash = seller.hashCode()
        hash = 31 * hash + coordinatorOriginUri.hashCode()
        hash = 31 * hash + sellerConfiguration.hashCode()
        return hash
    }

    /** Overrides the toString method. */
    override fun toString(): String {
        return "GetAdSelectionDataRequest: seller=$seller, " +
            "coordinatorOriginUri=$coordinatorOriginUri, " +
            "sellerConfiguration=$sellerConfiguration"
    }

    /**
     * Gets the coordinator origin Uri from which GetAdSelectionData API should fetch the decryption
     * key.
     */
    @ExperimentalFeatures.Ext12OptIn
    fun getCoordinatorOriginUri(): Uri? {
        return coordinatorOriginUri
    }

    /**
     * Gets the seller ad tech's requested payload configuration, set by the calling SDK, to
     * optimize the payload.
     */
    @ExperimentalFeatures.Ext12OptIn
    fun getSellerConfiguration(): SellerConfiguration? {
        return sellerConfiguration
    }

    @SuppressLint("NewApi")
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @RequiresExtension(extension = SdkExtensions.AD_SERVICES, version = 10)
    @RequiresExtension(extension = Build.VERSION_CODES.S, version = 10)
    internal fun convertToAdServices(): android.adservices.adselection.GetAdSelectionDataRequest {
        if (
            AdServicesInfo.adServicesVersion() >= 14 || AdServicesInfo.extServicesVersionS() >= 14
        ) {
            return Ext14Impl.convertGetAdSelectionDataRequest(this)
        } else if (
            AdServicesInfo.adServicesVersion() >= 12 || AdServicesInfo.extServicesVersionS() >= 12
        ) {
            return Ext12Impl.convertGetAdSelectionDataRequest(this)
        }
        return Ext10Impl.convertGetAdSelectionDataRequest(this)
    }

    @RequiresExtension(extension = Build.VERSION_CODES.S, version = 14)
    @RequiresExtension(extension = SdkExtensions.AD_SERVICES, version = 14)
    private class Ext14Impl private constructor() {
        companion object {
            fun convertGetAdSelectionDataRequest(
                request: GetAdSelectionDataRequest
            ): android.adservices.adselection.GetAdSelectionDataRequest {
                return android.adservices.adselection.GetAdSelectionDataRequest.Builder()
                    .setSeller(request.seller.convertToAdServices())
                    .setCoordinatorOriginUri(request.coordinatorOriginUri)
                    .setSellerConfiguration(request.sellerConfiguration?.convertToAdServices())
                    .build()
            }
        }
    }

    @RequiresExtension(extension = SdkExtensions.AD_SERVICES, version = 12)
    @RequiresExtension(extension = Build.VERSION_CODES.S, version = 12)
    private class Ext12Impl private constructor() {
        companion object {
            fun convertGetAdSelectionDataRequest(
                request: GetAdSelectionDataRequest
            ): android.adservices.adselection.GetAdSelectionDataRequest {
                return android.adservices.adselection.GetAdSelectionDataRequest.Builder()
                    .setSeller(request.seller.convertToAdServices())
                    .setCoordinatorOriginUri(request.coordinatorOriginUri)
                    .build()
            }
        }
    }

    @RequiresExtension(extension = SdkExtensions.AD_SERVICES, version = 10)
    @RequiresExtension(extension = Build.VERSION_CODES.S, version = 10)
    private class Ext10Impl private constructor() {
        companion object {
            fun convertGetAdSelectionDataRequest(
                request: GetAdSelectionDataRequest
            ): android.adservices.adselection.GetAdSelectionDataRequest {
                return android.adservices.adselection.GetAdSelectionDataRequest.Builder()
                    .setSeller(request.seller.convertToAdServices())
                    .build()
            }
        }
    }
}
