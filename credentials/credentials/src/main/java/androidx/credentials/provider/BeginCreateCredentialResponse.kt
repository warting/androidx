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

package androidx.credentials.provider

import android.os.Build
import android.os.Bundle
import androidx.annotation.RequiresApi
import androidx.credentials.provider.CreateEntry.Companion.marshall
import androidx.credentials.provider.CreateEntry.Companion.unmarshallCreateEntries
import androidx.credentials.provider.RemoteEntry.Companion.marshall
import androidx.credentials.provider.RemoteEntry.Companion.unmarshallRemoteEntry
import androidx.credentials.provider.utils.BeginCreateCredentialUtil

/**
 * Response to [BeginCreateCredentialRequest].
 *
 * Credential providers must add a list of [CreateEntry], and an optional [RemoteEntry] to this
 * response.
 *
 * Each [CreateEntry] is displayed to the user on the account selector, as an account option where
 * the given credential can be stored. A [RemoteEntry] is an entry on the selector, through which
 * user can choose to create the credential on a remote device.
 */
class BeginCreateCredentialResponse
constructor(val createEntries: List<CreateEntry> = listOf(), val remoteEntry: RemoteEntry? = null) {

    /** Builder for [BeginCreateCredentialResponse]. */
    class Builder {
        private var createEntries: MutableList<CreateEntry> = mutableListOf()
        private var remoteEntry: RemoteEntry? = null

        /**
         * Sets the list of create entries to be shown on the UI.
         *
         * @throws IllegalArgumentException If [createEntries] is empty.
         * @throws NullPointerException If [createEntries] is null, or any of its elements are null.
         */
        fun setCreateEntries(createEntries: List<CreateEntry>): Builder {
            this.createEntries = createEntries.toMutableList()
            return this
        }

        /**
         * Adds an entry to the list of create entries to be shown on the UI.
         *
         * @throws NullPointerException If [createEntry] is null.
         */
        fun addCreateEntry(createEntry: CreateEntry): Builder {
            createEntries.add(createEntry)
            return this
        }

        /**
         * Sets a remote create entry to be shown on the UI. Provider must set this entry if they
         * wish to create the credential on a different device.
         *
         * <p> When constructing the {@link CreateEntry] object, the pending intent must be set such
         * that it leads to an activity that can provide UI to fulfill the request on a remote
         * device. When user selects this [remoteEntry], the system will invoke the pending intent
         * set on the [CreateEntry].
         *
         * <p> Once the remote credential flow is complete, the [android.app.Activity] result should
         * be set to [android.app.Activity#RESULT_OK] and an extra with the
         * [CredentialProviderService#EXTRA_CREATE_CREDENTIAL_RESPONSE] key should be populated with
         * a [android.credentials.CreateCredentialResponse] object.
         *
         * <p> Note that as a provider service you will only be able to set a remote entry if :
         * - Provider service possesses the [android.Manifest.permission.PROVIDE_REMOTE_CREDENTIALS]
         *   permission.
         * - Provider service is configured as the provider that can provide remote entries.
         *
         * If the above conditions are not met, setting back [BeginCreateCredentialResponse] on the
         * callback from [CredentialProviderService#onBeginCreateCredential] will throw a
         * [SecurityException].
         */
        fun setRemoteEntry(remoteEntry: RemoteEntry?): Builder {
            this.remoteEntry = remoteEntry
            return this
        }

        /**
         * Builds a new instance of [BeginCreateCredentialResponse].
         *
         * @throws IllegalArgumentException If [createEntries] is empty
         */
        fun build(): BeginCreateCredentialResponse {
            return BeginCreateCredentialResponse(createEntries.toList(), remoteEntry)
        }
    }

    @RequiresApi(34)
    private object Api34Impl {
        private const val REQUEST_KEY =
            "androidx.credentials.provider.BeginCreateCredentialResponse"

        @JvmStatic
        fun asBundle(bundle: Bundle, response: BeginCreateCredentialResponse) {
            bundle.putParcelable(
                REQUEST_KEY,
                BeginCreateCredentialUtil.convertToFrameworkResponse(response),
            )
        }

        @JvmStatic
        fun fromBundle(bundle: Bundle): BeginCreateCredentialResponse? {
            val frameworkResponse =
                bundle.getParcelable(
                    REQUEST_KEY,
                    android.service.credentials.BeginCreateCredentialResponse::class.java,
                )
            if (frameworkResponse != null) {
                return BeginCreateCredentialUtil.convertToJetpackResponse(frameworkResponse)
            }
            return null
        }
    }

    @RequiresApi(23)
    private object Api23Impl {

        @JvmStatic
        fun asBundle(bundle: Bundle, response: BeginCreateCredentialResponse) {
            response.createEntries.marshall(bundle)
            response.remoteEntry?.marshall(bundle)
        }

        @JvmStatic
        fun fromBundle(bundle: Bundle): BeginCreateCredentialResponse? {
            val createEntries = bundle.unmarshallCreateEntries()
            val remoteEntry = bundle.unmarshallRemoteEntry()
            return if (createEntries.isEmpty() && remoteEntry == null) {
                null
            } else {
                BeginCreateCredentialResponse(createEntries, remoteEntry)
            }
        }
    }

    companion object {
        /**
         * Helper method to convert the class to a parcelable [Bundle], in case the class instance
         * needs to be sent across a process. Consumers of this method should use [fromBundle] to
         * reconstruct the class instance back from the bundle returned here.
         */
        @JvmStatic
        fun asBundle(response: BeginCreateCredentialResponse): Bundle {
            val bundle = Bundle()
            if (Build.VERSION.SDK_INT >= 34) { // Android U
                Api34Impl.asBundle(bundle, response)
            } else if (Build.VERSION.SDK_INT >= 23) {
                Api23Impl.asBundle(bundle, response)
            }
            return bundle
        }

        /**
         * Helper method to convert a [Bundle] retrieved through [asBundle], back to an instance of
         * [BeginGetCredentialResponse].
         */
        @JvmStatic
        fun fromBundle(bundle: Bundle): BeginCreateCredentialResponse? {
            return if (Build.VERSION.SDK_INT >= 34) { // Android U
                Api34Impl.fromBundle(bundle)
            } else if (Build.VERSION.SDK_INT >= 23) {
                Api23Impl.fromBundle(bundle)
            } else {
                null
            }
        }
    }
}
