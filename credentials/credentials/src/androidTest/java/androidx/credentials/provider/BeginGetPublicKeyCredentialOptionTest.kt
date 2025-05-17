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

import android.os.Bundle
import androidx.credentials.GetPublicKeyCredentialOption
import androidx.credentials.PublicKeyCredential
import androidx.credentials.equals
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class BeginGetPublicKeyCredentialOptionTest {
    companion object {
        private const val BUNDLE_ID_KEY =
            "android.service.credentials.BeginGetCredentialOption.BUNDLE_ID_KEY"
        private const val BUNDLE_ID = "id"
    }

    @Test
    fun constructor_emptyJson_throwsIllegalArgumentException() {
        Assert.assertThrows(
            "Expected empty Json to throw error",
            IllegalArgumentException::class.java,
        ) {
            BeginGetPublicKeyCredentialOption(Bundle(), "", "")
        }
    }

    @Test
    fun constructor_invalidJson_throwsIllegalArgumentException() {
        Assert.assertThrows(
            "Expected invalid Json to throw error",
            IllegalArgumentException::class.java,
        ) {
            BeginGetPublicKeyCredentialOption(Bundle(), "", "invalid")
        }
    }

    @Test
    fun constructor_success() {
        BeginGetPublicKeyCredentialOption(
            Bundle(),
            BUNDLE_ID,
            "{\"hi\":{\"there\":{\"lol\":\"Value\"}}}",
            "client_data_hash".toByteArray(),
        )
    }

    @Test
    fun getter_clientDataHash_success() {
        val testClientDataHashExpected = "client_data_hash".toByteArray()

        val beginGetPublicKeyCredentialOpt =
            BeginGetPublicKeyCredentialOption(
                Bundle(),
                BUNDLE_ID,
                "{\"hi\":{\"there\":{\"lol\":\"Value\"}}}",
                testClientDataHashExpected,
            )

        val testClientDataHashActual = beginGetPublicKeyCredentialOpt.clientDataHash
        assertThat(testClientDataHashActual).isEqualTo(testClientDataHashExpected)
    }

    @Test
    fun getter_requestJson_success() {
        val testJsonExpected = "{\"hi\":{\"there\":{\"lol\":\"Value\"}}}"

        val getPublicKeyCredentialOpt =
            BeginGetPublicKeyCredentialOption(Bundle(), BUNDLE_ID, testJsonExpected)

        val testJsonActual = getPublicKeyCredentialOpt.requestJson
        assertThat(testJsonActual).isEqualTo(testJsonExpected)
    }

    @Test
    fun getter_frameworkProperties_success() {
        val requestJsonExpected = "{\"hi\":{\"there\":{\"lol\":\"Value\"}}}"
        val clientDataHash = "client_data_hash".toByteArray()
        val expectedData = Bundle()
        expectedData.putString(
            PublicKeyCredential.BUNDLE_KEY_SUBTYPE,
            GetPublicKeyCredentialOption.BUNDLE_VALUE_SUBTYPE_GET_PUBLIC_KEY_CREDENTIAL_OPTION,
        )
        expectedData.putString(
            GetPublicKeyCredentialOption.BUNDLE_KEY_REQUEST_JSON,
            requestJsonExpected,
        )
        expectedData.putByteArray(
            GetPublicKeyCredentialOption.BUNDLE_KEY_CLIENT_DATA_HASH,
            clientDataHash,
        )

        val option = BeginGetPublicKeyCredentialOption(expectedData, BUNDLE_ID, requestJsonExpected)

        expectedData.putString(BUNDLE_ID_KEY, BUNDLE_ID)
        assertThat(option.type).isEqualTo(PublicKeyCredential.TYPE_PUBLIC_KEY_CREDENTIAL)
        assertThat(equals(option.candidateQueryData, expectedData)).isTrue()
    }

    @Test
    fun createFrom_success() {
        val requestJsonExpected = "{\"hi\":{\"there\":{\"lol\":\"Value\"}}}"
        val clientDataHash = "client_data_hash".toByteArray()
        val bundle = Bundle()
        bundle.putString(
            PublicKeyCredential.BUNDLE_KEY_SUBTYPE,
            GetPublicKeyCredentialOption.BUNDLE_VALUE_SUBTYPE_GET_PUBLIC_KEY_CREDENTIAL_OPTION,
        )
        bundle.putString(GetPublicKeyCredentialOption.BUNDLE_KEY_REQUEST_JSON, requestJsonExpected)
        bundle.putByteArray(
            GetPublicKeyCredentialOption.BUNDLE_KEY_CLIENT_DATA_HASH,
            clientDataHash,
        )

        val option = BeginGetPublicKeyCredentialOption.createFrom(bundle, "id")
        assertThat(option.id).isEqualTo("id")
    }

    @Test
    fun createFromEntrySlice_success() {
        val requestJsonExpected = "{\"hi\":{\"there\":{\"lol\":\"Value\"}}}"
        val clientDataHash = "client_data_hash".toByteArray()
        val bundle = Bundle()
        bundle.putString(
            PublicKeyCredential.BUNDLE_KEY_SUBTYPE,
            GetPublicKeyCredentialOption.BUNDLE_VALUE_SUBTYPE_GET_PUBLIC_KEY_CREDENTIAL_OPTION,
        )
        bundle.putString(GetPublicKeyCredentialOption.BUNDLE_KEY_REQUEST_JSON, requestJsonExpected)
        bundle.putByteArray(
            GetPublicKeyCredentialOption.BUNDLE_KEY_CLIENT_DATA_HASH,
            clientDataHash,
        )

        val option = BeginGetPublicKeyCredentialOption.createFromEntrySlice(bundle, "id")
        assertThat(option.id).isEqualTo("id")
    }
}
