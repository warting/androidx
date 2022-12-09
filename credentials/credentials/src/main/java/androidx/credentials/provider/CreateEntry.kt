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
package androidx.credentials.provider

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.app.slice.Slice
import android.app.slice.SliceSpec
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.annotation.VisibleForTesting
import androidx.credentials.CredentialManager
import androidx.credentials.provider.Action.Companion.toSlice
import java.util.Collections

/**
 * An entry to be shown on the selector during a create flow initiated when an app calls
 * [CredentialManager.executeCreateCredential]
 *
 * A [CreateEntry] points to a location such as an account, or a group where the credential can be
 * registered. When user selects this entry, the corresponding [PendingIntent] is fired, and the
 * credential creation can be completed.
 *
 * @property accountName the name of the account where the credential
 * will be registered
 * @property pendingIntent the [PendingIntent] to be fired when this
 * [CreateEntry] is selected
 * @property lastUsedTimeMillis the last used time of the account/group underlying this entry
 * @property credentialCountInformation a list of count information per credential type
 * @throws IllegalArgumentException If [accountName] is empty
 *
 * @hide
 */
@RequiresApi(34)
class CreateEntry internal constructor(
    val accountName: CharSequence,
    val pendingIntent: PendingIntent,
    val icon: Icon?,
    val lastUsedTimeMillis: Long,
    val credentialCountInformationList: List<CredentialCountInformation>
    ) {

    init {
        require(accountName.isNotEmpty()) { "accountName must not be empty" }
    }

    /**
     * A builder for [CreateEntry]
     *
     * @property accountName the name of the account where the credential will be registered
     * @property pendingIntent the [PendingIntent] that will be fired when the user selects
     * this entry
     *
     * @hide
     */
    class Builder constructor(
        private val accountName: CharSequence,
        private val pendingIntent: PendingIntent
        ) {

        private var credentialCountInformationList: MutableList<CredentialCountInformation> =
            mutableListOf()
        private var icon: Icon? = null
        private var lastUsedTimeMillis: Long = 0

        /** Adds a [CredentialCountInformation] denoting a given credential
         * type and the count of credentials that the provider has stored for that
         * credential type.
         *
         * This information will be displayed on the [CreateEntry] to help the user
         * make a choice.
         */
        @Suppress("MissingGetterMatchingBuilder")
        fun addCredentialCountInformation(info: CredentialCountInformation): Builder {
            credentialCountInformationList.add(info)
            return this
        }

        /** Sets a list of [CredentialCountInformation]. Each item in the list denotes a given
         * credential type and the count of credentials that the provider has stored of that
         * credential type.
         *
         * This information will be displayed on the [CreateEntry] to help the user
         * make a choice.
         */
        fun setCredentialCountInformationList(infoList: List<CredentialCountInformation>): Builder {
            credentialCountInformationList = infoList as MutableList<CredentialCountInformation>
            return this
        }

        /** Sets an icon to be displayed with the entry on the UI */
        fun setIcon(icon: Icon?): Builder {
            this.icon = icon
            return this
        }

        /** Sets the last time this account was used */
        fun setLastUsedTimeMillis(lastUsedTimeMillis: Long): Builder {
            this.lastUsedTimeMillis = lastUsedTimeMillis
            return this
        }

        /**
         * Builds an instance of [CreateEntry]
         *
         * @throws IllegalArgumentException If [accountName] is empty
         */
        fun build(): CreateEntry {
            return CreateEntry(accountName, pendingIntent, icon, lastUsedTimeMillis,
                credentialCountInformationList)
        }
    }

    companion object {
        private const val TAG = "CreateEntry"
        @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        internal const val SLICE_HINT_ACCOUNT_NAME =
            "androidx.credentials.provider.createEntry.SLICE_HINT_USER_PROVIDER_ACCOUNT_NAME"
        @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        internal const val SLICE_HINT_ICON =
            "androidx.credentials.provider.createEntry.SLICE_HINT_PROFILE_ICON"
        @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        internal const val SLICE_HINT_CREDENTIAL_COUNT_INFORMATION =
            "androidx.credentials.provider.createEntry.SLICE_HINT_CREDENTIAL_COUNT_INFORMATION"
        @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        internal const val SLICE_HINT_LAST_USED_TIME_MILLIS =
            "androidx.credentials.provider.createEntry.SLICE_HINT_LAST_USED_TIME_MILLIS"
        @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        internal const val SLICE_HINT_PENDING_INTENT =
            "androidx.credentials.provider.createEntry.SLICE_HINT_PENDING_INTENT"

        @JvmStatic
        fun toSlice(createEntry: CreateEntry): Slice {
            // TODO("Use the right type and revision")
            val sliceBuilder = Slice.Builder(Uri.EMPTY, SliceSpec("type", 1))
            sliceBuilder.addText(createEntry.accountName, /*subType=*/null,
                listOf(SLICE_HINT_ACCOUNT_NAME))
                .addLong(createEntry.lastUsedTimeMillis, /*subType=*/null, listOf(
                    SLICE_HINT_LAST_USED_TIME_MILLIS))
            if (createEntry.icon != null) {
                sliceBuilder.addIcon(createEntry.icon, /*subType=*/null,
                    listOf(SLICE_HINT_ICON))
            }

                val credentialCountBundle = convertCredentialCountInfoToBundle(
                    createEntry.credentialCountInformationList)
                if (credentialCountBundle != null) {
                    sliceBuilder.addBundle(convertCredentialCountInfoToBundle(
                        createEntry.credentialCountInformationList), null, listOf(
                        SLICE_HINT_CREDENTIAL_COUNT_INFORMATION))
                }
            sliceBuilder.addAction(createEntry.pendingIntent,
                Slice.Builder(sliceBuilder)
                    .addHints(Collections.singletonList(SLICE_HINT_PENDING_INTENT))
                    .build(),
                /*subType=*/null)
            return sliceBuilder.build()
        }

        /**
         * Returns an instance of [CreateEntry] derived from a [Slice] object.
         *
         * @param slice the [Slice] object constructed through [toSlice]
         */
        @SuppressLint("WrongConstant") // custom conversion between jetpack and framework
        @JvmStatic
        fun fromSlice(slice: Slice): CreateEntry? {
            // TODO("Put the right spec and version value")
            var accountName: CharSequence = ""
            var icon: Icon? = null
            var pendingIntent: PendingIntent? = null
            var credentialCountInfo: List<CredentialCountInformation> = listOf()
            var lastUsedTimeMillis: Long = 0

            slice.items.forEach {
                if (it.hasHint(SLICE_HINT_ACCOUNT_NAME)) {
                    accountName = it.text
                } else if (it.hasHint(SLICE_HINT_ICON)) {
                    icon = it.icon
                } else if (it.hasHint(SLICE_HINT_PENDING_INTENT)) {
                    pendingIntent = it.action
                } else if (it.hasHint(SLICE_HINT_CREDENTIAL_COUNT_INFORMATION)) {
                    credentialCountInfo = convertBundleToCredentialCountInfo(it.bundle)
                } else if (it.hasHint(SLICE_HINT_LAST_USED_TIME_MILLIS)) {
                    lastUsedTimeMillis = it.long
                }
            }

            return try {
                CreateEntry(accountName, pendingIntent!!, icon,
                    lastUsedTimeMillis, credentialCountInfo)
            } catch (e: Exception) {
                Log.i(TAG, "fromSlice failed with: " + e.message)
                null
            }
        }

        @JvmStatic
        internal fun convertBundleToCredentialCountInfo(bundle: Bundle?):
            List<CredentialCountInformation> {
            val credentialCountList = ArrayList<CredentialCountInformation>()
            if (bundle == null) {
                return credentialCountList
            }
            bundle.keySet().forEach {
                try {
                    credentialCountList.add(
                        CredentialCountInformation(it, bundle.getInt(it)))
                } catch (e: Exception) {
                    Log.i(TAG, "Issue unpacking credential count info bundle: " + e.message)
                }
            }
            return credentialCountList
        }

        @JvmStatic
        internal fun convertCredentialCountInfoToBundle(
            credentialCountInformationList: List<CredentialCountInformation>
        ): Bundle? {
            if (credentialCountInformationList.isEmpty()) {
                return null
            }
            val bundle = Bundle()
            credentialCountInformationList.forEach {
                bundle.putInt(it.type, it.count)
            }
            return bundle
        }

        @JvmStatic
        internal fun toFrameworkClass(createEntry: CreateEntry):
            android.service.credentials.CreateEntry {
            return android.service.credentials.CreateEntry(
                toSlice(createEntry),
                createEntry.pendingIntent)
        }
    }
}
