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

package androidx.credentials

import android.content.ComponentName
import androidx.credentials.GetCredentialRequest.Companion.createFrom
import androidx.credentials.GetCredentialRequest.Companion.getRequestMetadataBundle
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalDigitalCredentialApi::class)
@RunWith(AndroidJUnit4::class)
@SmallTest
class GetCredentialRequestTest {
    companion object Constant {
        private const val TEST_JSON = "{\"user\":{\"name\":{\"lol\":\"Value\"}}}"
    }

    @Test
    fun constructor_emptyCredentialOptions_throws() {
        assertThrows(IllegalArgumentException::class.java) { GetCredentialRequest(ArrayList()) }
    }

    @Test
    fun constructor_mixedUseOfGetRestoreCredentialOption_throws() {
        assertThrows(IllegalArgumentException::class.java) {
            val credentialOptions = ArrayList<CredentialOption>()
            credentialOptions.add(GetRestoreCredentialOption(TEST_JSON))
            credentialOptions.add(GetPasswordOption())
            GetCredentialRequest(credentialOptions)
        }
    }

    @Test
    fun constructor_singleUseOfGetRestoreCredentialOption_doesNotThrow() {
        val credentialOptions = ArrayList<CredentialOption>()
        credentialOptions.add(GetRestoreCredentialOption(TEST_JSON))
        GetCredentialRequest(credentialOptions)
    }

    @Test
    fun constructor_mixedUseOfDigitalCredentialOption_throws() {
        assertThrows(IllegalArgumentException::class.java) {
            val credentialOptions = ArrayList<CredentialOption>()
            credentialOptions.add(GetDigitalCredentialOption(TEST_JSON))
            credentialOptions.add(GetPasswordOption())
            GetCredentialRequest(credentialOptions)
        }
    }

    @Test
    fun constructor_singleUseOfDigitalCredentialOption_doesNotThrow() {
        val credentialOptions = ArrayList<CredentialOption>()
        credentialOptions.add(GetDigitalCredentialOption(TEST_JSON))
        credentialOptions.add(GetDigitalCredentialOption(TEST_JSON))
        GetCredentialRequest(credentialOptions)
    }

    @Test
    fun constructor() {
        val expectedCredentialOptions = ArrayList<CredentialOption>()
        expectedCredentialOptions.add(GetPasswordOption())
        expectedCredentialOptions.add(GetPublicKeyCredentialOption(TEST_JSON))
        val origin = "origin"

        val request = GetCredentialRequest(expectedCredentialOptions, origin)

        assertThat(request.credentialOptions).hasSize(expectedCredentialOptions.size)
        for (i in expectedCredentialOptions.indices) {
            assertThat(request.credentialOptions[i]).isEqualTo(expectedCredentialOptions[i])
        }
        assertThat(request.origin).isEqualTo(origin)
        assertThat(request.preferIdentityDocUi).isFalse()
        assertThat(request.preferImmediatelyAvailableCredentials).isFalse()
        assertThat(request.preferUiBrandingComponentName).isNull()
    }

    @Test
    fun constructor_defaultAutoSelect() {
        val options = ArrayList<CredentialOption>()
        options.add(GetPasswordOption())
        val origin = "origin"

        val request = GetCredentialRequest(options, origin)

        assertThat(request.credentialOptions[0].isAutoSelectAllowed).isFalse()
        assertThat(request.origin).isEqualTo(origin)
        assertThat(request.preferIdentityDocUi).isFalse()
    }

    @Test
    fun constructor_nonDefaultPreferUiBrandingComponentName() {
        val options = java.util.ArrayList<CredentialOption>()
        options.add(GetPasswordOption())
        val expectedComponentName = ComponentName("test pkg", "test cls")

        val request =
            GetCredentialRequest(
                options,
                /*origin=*/ null,
                /*preferIdentityDocUi=*/ false,
                expectedComponentName,
            )

        assertThat(request.credentialOptions[0].isAutoSelectAllowed).isFalse()
        assertThat(request.preferUiBrandingComponentName).isEqualTo(expectedComponentName)
    }

    @Test
    fun constructor_nonDefaultPreferImmediatelyAvailableCredentials() {
        val options = java.util.ArrayList<CredentialOption>()
        options.add(GetPasswordOption())
        val expectedPreferImmediatelyAvailableCredentials = true

        val request =
            GetCredentialRequest(
                options,
                origin = null,
                preferIdentityDocUi = false,
                preferUiBrandingComponentName = null,
                expectedPreferImmediatelyAvailableCredentials,
            )

        assertThat(request.credentialOptions[0].isAutoSelectAllowed).isFalse()
        assertThat(request.preferImmediatelyAvailableCredentials)
            .isEqualTo(expectedPreferImmediatelyAvailableCredentials)
    }

    @Test
    fun builder_setPreferImmediatelyAvailableCredentials() {
        val expectedCredentialOptions = java.util.ArrayList<CredentialOption>()
        expectedCredentialOptions.add(GetPasswordOption())
        expectedCredentialOptions.add(GetPublicKeyCredentialOption(TEST_JSON))
        val expectedPreferImmediatelyAvailableCredentials = true

        val request =
            GetCredentialRequest.Builder()
                .setCredentialOptions(expectedCredentialOptions)
                .setPreferImmediatelyAvailableCredentials(
                    expectedPreferImmediatelyAvailableCredentials
                )
                .build()

        assertThat(request.credentialOptions).hasSize(expectedCredentialOptions.size)
        for (i in expectedCredentialOptions.indices) {
            assertThat(request.credentialOptions[i]).isEqualTo(expectedCredentialOptions[i])
        }
        assertThat(request.preferImmediatelyAvailableCredentials)
            .isEqualTo(expectedPreferImmediatelyAvailableCredentials)
    }

    @Test
    fun builder_addCredentialOption() {
        val expectedCredentialOptions = ArrayList<CredentialOption>()
        expectedCredentialOptions.add(GetPasswordOption())
        expectedCredentialOptions.add(GetPublicKeyCredentialOption(TEST_JSON))

        val request =
            GetCredentialRequest.Builder()
                .addCredentialOption(expectedCredentialOptions[0])
                .addCredentialOption(expectedCredentialOptions[1])
                .build()

        assertThat(request.credentialOptions).hasSize(expectedCredentialOptions.size)
        for (i in expectedCredentialOptions.indices) {
            assertThat(request.credentialOptions[i]).isEqualTo(expectedCredentialOptions[i])
        }
    }

    @Test
    fun builder_setCredentialOptions() {
        val expectedCredentialOptions = ArrayList<CredentialOption>()
        expectedCredentialOptions.add(GetPasswordOption())
        expectedCredentialOptions.add(GetPublicKeyCredentialOption(TEST_JSON))

        val request =
            GetCredentialRequest.Builder().setCredentialOptions(expectedCredentialOptions).build()

        assertThat(request.credentialOptions).hasSize(expectedCredentialOptions.size)
        for (i in expectedCredentialOptions.indices) {
            assertThat(request.credentialOptions[i]).isEqualTo(expectedCredentialOptions[i])
        }
        assertThat(request.preferIdentityDocUi).isFalse()
        assertThat(request.preferImmediatelyAvailableCredentials).isFalse()
        assertThat(request.preferUiBrandingComponentName).isNull()
    }

    @Test
    fun builder_setPreferIdentityDocUis() {
        val expectedCredentialOptions = ArrayList<CredentialOption>()
        expectedCredentialOptions.add(GetPasswordOption())
        expectedCredentialOptions.add(GetPublicKeyCredentialOption(TEST_JSON))

        val request =
            GetCredentialRequest.Builder()
                .setCredentialOptions(expectedCredentialOptions)
                .setPreferIdentityDocUi(true)
                .build()

        assertThat(request.credentialOptions).hasSize(expectedCredentialOptions.size)
        for (i in expectedCredentialOptions.indices) {
            assertThat(request.credentialOptions[i]).isEqualTo(expectedCredentialOptions[i])
        }
        assertThat(request.preferIdentityDocUi).isTrue()
    }

    @Test
    fun builder_setPreferUiBrandingComponentName() {
        val expectedCredentialOptions = java.util.ArrayList<CredentialOption>()
        expectedCredentialOptions.add(GetPasswordOption())
        expectedCredentialOptions.add(GetPublicKeyCredentialOption(TEST_JSON))
        val expectedComponentName = ComponentName("test pkg", "test cls")

        val request =
            GetCredentialRequest.Builder()
                .setCredentialOptions(expectedCredentialOptions)
                .setPreferUiBrandingComponentName(expectedComponentName)
                .build()

        assertThat(request.credentialOptions).hasSize(expectedCredentialOptions.size)
        for (i in expectedCredentialOptions.indices) {
            assertThat(request.credentialOptions[i]).isEqualTo(expectedCredentialOptions[i])
        }
        assertThat(request.preferUiBrandingComponentName).isEqualTo(expectedComponentName)
    }

    @Test
    fun builder_defaultAutoSelect() {
        val request =
            GetCredentialRequest.Builder().addCredentialOption(GetPasswordOption()).build()

        assertThat(request.credentialOptions[0].isAutoSelectAllowed).isFalse()
    }

    @Test
    fun frameworkConversion() {
        val options = java.util.ArrayList<CredentialOption>()
        options.add(GetPasswordOption())
        val expectedPreferImmediatelyAvailableCredentials = true
        val expectedComponentName = ComponentName("test pkg", "test cls")
        val expectedPreferIdentityDocUi = true
        val expectedOrigin = "origin"
        val request =
            GetCredentialRequest(
                options,
                expectedOrigin,
                expectedPreferIdentityDocUi,
                expectedComponentName,
                expectedPreferImmediatelyAvailableCredentials,
            )

        val convertedRequest =
            createFrom(options, request.origin, getRequestMetadataBundle(request))

        assertThat(convertedRequest.origin).isEqualTo(expectedOrigin)
        assertThat(convertedRequest.preferIdentityDocUi).isEqualTo(expectedPreferIdentityDocUi)
        assertThat(convertedRequest.preferUiBrandingComponentName).isEqualTo(expectedComponentName)
        assertThat(convertedRequest.preferImmediatelyAvailableCredentials)
            .isEqualTo(expectedPreferImmediatelyAvailableCredentials)
    }

    @SdkSuppress(minSdkVersion = 34)
    @Test
    fun frameworkConversion_frameworkClass_success() {
        val options = java.util.ArrayList<CredentialOption>()
        options.add(GetPasswordOption())
        val expectedPreferImmediatelyAvailableCredentials = true
        val expectedComponentName = ComponentName("test pkg", "test cls")
        val expectedPreferIdentityDocUi = true
        val expectedOrigin = "origin"
        val request =
            GetCredentialRequest(
                options,
                expectedOrigin,
                expectedPreferIdentityDocUi,
                expectedComponentName,
                expectedPreferImmediatelyAvailableCredentials,
            )

        val convertedRequest =
            createFrom(
                android.credentials.GetCredentialRequest.Builder(getRequestMetadataBundle(request))
                    .setOrigin(expectedOrigin)
                    .setCredentialOptions(
                        options.map {
                            android.credentials.CredentialOption.Builder(
                                    it.type,
                                    it.requestData,
                                    it.candidateQueryData,
                                )
                                .setAllowedProviders(it.allowedProviders)
                                .setIsSystemProviderRequired(it.isSystemProviderRequired)
                                .build()
                        }
                    )
                    .build()
            )

        assertThat(convertedRequest.origin).isEqualTo(expectedOrigin)
        assertThat(convertedRequest.preferIdentityDocUi).isEqualTo(expectedPreferIdentityDocUi)
        assertThat(convertedRequest.preferUiBrandingComponentName).isEqualTo(expectedComponentName)
        assertThat(convertedRequest.preferImmediatelyAvailableCredentials)
            .isEqualTo(expectedPreferImmediatelyAvailableCredentials)
    }
}
