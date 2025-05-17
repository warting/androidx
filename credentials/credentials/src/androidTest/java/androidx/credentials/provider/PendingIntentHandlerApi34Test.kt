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

import android.content.Intent
import android.content.pm.SigningInfo
import android.credentials.CredentialOption
import android.os.Binder
import android.os.Bundle
import android.service.credentials.CallingAppInfo
import android.service.credentials.CreateCredentialRequest
import android.service.credentials.GetCredentialRequest
import androidx.credentials.CreateCustomCredentialResponse
import androidx.credentials.CreatePasswordResponse
import androidx.credentials.GetCredentialResponse
import androidx.credentials.PasswordCredential
import androidx.credentials.assertEquals
import androidx.credentials.equals
import androidx.credentials.exceptions.CreateCredentialInterruptedException
import androidx.credentials.exceptions.GetCredentialInterruptedException
import androidx.credentials.exceptions.domerrors.ConstraintError
import androidx.credentials.exceptions.publickeycredential.CreatePublicKeyCredentialDomException
import androidx.credentials.provider.PendingIntentHandler.Companion.setCreateCredentialResponse
import androidx.credentials.setUpCreatePasswordRequest
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
@SdkSuppress(minSdkVersion = 34, codeName = "UpsideDownCake")
class PendingIntentHandlerApi34Test {
    companion object {
        private val GET_CREDENTIAL_OPTION =
            CredentialOption.Builder("type", Bundle(), Bundle()).build()

        private val GET_CREDENTIAL_REQUEST =
            GetCredentialRequest(
                CallingAppInfo("package_name", SigningInfo()),
                ArrayList(setOf(GET_CREDENTIAL_OPTION)),
            )

        private const val BIOMETRIC_AUTHENTICATOR_TYPE = 1

        private const val BIOMETRIC_AUTHENTICATOR_ERROR_CODE = 5

        private const val BIOMETRIC_AUTHENTICATOR_ERROR_MSG = "error"

        private const val FRAMEWORK_EXPECTED_CONSTANT_ERROR_CODE =
            "androidx.credentials.provider.BIOMETRIC_AUTH_ERROR_CODE"

        private const val FRAMEWORK_EXPECTED_CONSTANT_ERROR_MESSAGE =
            "androidx.credentials.provider.BIOMETRIC_AUTH_ERROR_MESSAGE"

        private const val FRAMEWORK_EXPECTED_CONSTANT_AUTH_RESULT =
            "androidx.credentials.provider.BIOMETRIC_AUTH_RESULT"

        private val context = InstrumentationRegistry.getInstrumentation().context
    }

    @Test
    fun test_constantsMatchFrameworkExpectations_success() {
        assertThat(AuthenticationResult.EXTRA_BIOMETRIC_AUTH_RESULT_TYPE)
            .isEqualTo(FRAMEWORK_EXPECTED_CONSTANT_AUTH_RESULT)
        assertThat(AuthenticationError.EXTRA_BIOMETRIC_AUTH_ERROR)
            .isEqualTo(FRAMEWORK_EXPECTED_CONSTANT_ERROR_CODE)
        assertThat(AuthenticationError.EXTRA_BIOMETRIC_AUTH_ERROR_MESSAGE)
            .isEqualTo(FRAMEWORK_EXPECTED_CONSTANT_ERROR_MESSAGE)
    }

    @Test
    fun test_retrieveProviderCreateCredReqWithSuccessBpAuthJetpack_retrieveJetpackResult() {
        for (jetpackResult in AuthenticationResult.biometricFrameworkToJetpackResultMap.values) {
            val biometricPromptResult = BiometricPromptResult(AuthenticationResult(jetpackResult))
            val request = setUpCreatePasswordRequest()
            val intent = prepareIntentWithCreateRequest(request, biometricPromptResult)

            val retrievedRequest =
                PendingIntentHandler.retrieveProviderCreateCredentialRequest(intent)

            Assert.assertNotNull(request)
            equals(request, retrievedRequest!!)
            Assert.assertNotNull(biometricPromptResult.authenticationResult)
            Assert.assertEquals(
                retrievedRequest.biometricPromptResult!!.authenticationResult!!.authenticationType,
                jetpackResult,
            )
        }
    }

    @Test
    fun test_retrieveProviderGetCredReqWithSuccessBpAuthJetpack_retrieveJetpackResult() {
        for (jetpackResult in AuthenticationResult.biometricFrameworkToJetpackResultMap.values) {
            val biometricPromptResult = BiometricPromptResult(AuthenticationResult(jetpackResult))
            val intent = prepareIntentWithGetRequest(GET_CREDENTIAL_REQUEST, biometricPromptResult)

            val request = PendingIntentHandler.retrieveProviderGetCredentialRequest(intent)

            Assert.assertNotNull(request)
            equals(GET_CREDENTIAL_REQUEST, request!!)
            Assert.assertEquals(biometricPromptResult, request.biometricPromptResult)
            Assert.assertEquals(
                request.biometricPromptResult!!.authenticationResult!!.authenticationType,
                jetpackResult,
            )
        }
    }

    // While possible to test non-conversion logic, that would equate functionally to the normal
    // jetpack tests as there is no validation.
    @Test
    fun test_retrieveProviderCreateCredReqWithSuccessBpAuthFramework_correctlyConvertedResult() {
        for (frameworkResult in AuthenticationResult.biometricFrameworkToJetpackResultMap.keys) {
            val biometricPromptResult =
                BiometricPromptResult(
                    AuthenticationResult.createFrom(
                        uiAuthenticationType = frameworkResult,
                        isFrameworkBiometricPrompt = true,
                    )
                )
            val request = setUpCreatePasswordRequest()
            val expectedResult =
                AuthenticationResult.biometricFrameworkToJetpackResultMap[frameworkResult]
            val intent = prepareIntentWithCreateRequest(request, biometricPromptResult)

            val retrievedRequest =
                PendingIntentHandler.retrieveProviderCreateCredentialRequest(intent)

            Assert.assertNotNull(request)
            equals(request, retrievedRequest!!)
            Assert.assertNotNull(biometricPromptResult.authenticationResult)
            Assert.assertEquals(
                retrievedRequest.biometricPromptResult!!.authenticationResult!!.authenticationType,
                expectedResult,
            )
        }
    }

    // While possible to test non-conversion logic, that would equate functionally to the normal
    // jetpack tests as there is no validation.
    @Test
    fun test_retrieveProviderGetCredReqWithSuccessBpAuthFramework_correctlyConvertedResult() {
        for (frameworkResult in AuthenticationResult.biometricFrameworkToJetpackResultMap.keys) {
            val biometricPromptResult =
                BiometricPromptResult(
                    AuthenticationResult.createFrom(
                        uiAuthenticationType = frameworkResult,
                        isFrameworkBiometricPrompt = true,
                    )
                )
            val expectedResult =
                AuthenticationResult.biometricFrameworkToJetpackResultMap[frameworkResult]
            val intent = prepareIntentWithGetRequest(GET_CREDENTIAL_REQUEST, biometricPromptResult)

            val request = PendingIntentHandler.retrieveProviderGetCredentialRequest(intent)

            Assert.assertNotNull(request)
            equals(GET_CREDENTIAL_REQUEST, request!!)
            Assert.assertEquals(biometricPromptResult, request.biometricPromptResult)
            Assert.assertEquals(
                request.biometricPromptResult!!.authenticationResult!!.authenticationType,
                expectedResult,
            )
        }
    }

    @Test
    fun test_retrieveProviderCreateCredReqWithFailureBpAuthJetpack_retrieveJetpackError() {
        for (jetpackError in AuthenticationError.biometricFrameworkToJetpackErrorMap.values) {
            val biometricPromptResult =
                BiometricPromptResult(
                    AuthenticationError(jetpackError, BIOMETRIC_AUTHENTICATOR_ERROR_MSG)
                )
            val request = setUpCreatePasswordRequest()
            val intent = prepareIntentWithCreateRequest(request, biometricPromptResult)

            val retrievedRequest =
                PendingIntentHandler.retrieveProviderCreateCredentialRequest(intent)

            Assert.assertNotNull(retrievedRequest)
            equals(request, retrievedRequest!!)
            Assert.assertNotNull(retrievedRequest.biometricPromptResult!!.authenticationError)
            Assert.assertEquals(
                retrievedRequest.biometricPromptResult!!.authenticationError!!.errorCode,
                jetpackError,
            )
        }
    }

    @Test
    fun test_retrieveProviderGetCredReqWithFailureBpAuthJetpack_retrieveJetpackError() {
        for (jetpackError in AuthenticationError.biometricFrameworkToJetpackErrorMap.values) {
            val biometricPromptResult =
                BiometricPromptResult(
                    AuthenticationError(jetpackError, BIOMETRIC_AUTHENTICATOR_ERROR_MSG)
                )
            val intent = prepareIntentWithGetRequest(GET_CREDENTIAL_REQUEST, biometricPromptResult)

            val retrievedRequest = PendingIntentHandler.retrieveProviderGetCredentialRequest(intent)

            Assert.assertNotNull(retrievedRequest)
            equals(GET_CREDENTIAL_REQUEST, retrievedRequest!!)
            Assert.assertNotNull(retrievedRequest.biometricPromptResult!!.authenticationError)
            Assert.assertEquals(
                retrievedRequest.biometricPromptResult!!.authenticationError!!.errorCode,
                jetpackError,
            )
        }
    }

    @Test
    fun test_retrieveProviderCreateCredReqWithFailureBpAuthFramework_correctlyConvertedError() {
        for (frameworkError in AuthenticationError.biometricFrameworkToJetpackErrorMap.keys) {
            val biometricPromptResult =
                BiometricPromptResult(
                    AuthenticationError.createFrom(
                        uiErrorCode = frameworkError,
                        uiErrorMessage = BIOMETRIC_AUTHENTICATOR_ERROR_MSG,
                        isFrameworkBiometricPrompt = true,
                    )
                )
            val expectedErrorCode =
                AuthenticationError.biometricFrameworkToJetpackErrorMap[frameworkError]
            val request = setUpCreatePasswordRequest()
            val intent = prepareIntentWithCreateRequest(request, biometricPromptResult)

            val retrievedRequest =
                PendingIntentHandler.retrieveProviderCreateCredentialRequest(intent)

            Assert.assertNotNull(retrievedRequest)
            equals(request, retrievedRequest!!)
            Assert.assertNotNull(retrievedRequest.biometricPromptResult!!.authenticationError)
            Assert.assertEquals(
                retrievedRequest.biometricPromptResult!!.authenticationError!!.errorCode,
                expectedErrorCode,
            )
        }
    }

    @Test
    fun test_retrieveProviderGetCredReqWithFailureBpAuthFramework_correctlyConvertedError() {
        for (frameworkError in AuthenticationError.biometricFrameworkToJetpackErrorMap.keys) {
            val biometricPromptResult =
                BiometricPromptResult(
                    AuthenticationError.createFrom(
                        uiErrorCode = frameworkError,
                        uiErrorMessage = BIOMETRIC_AUTHENTICATOR_ERROR_MSG,
                        isFrameworkBiometricPrompt = true,
                    )
                )
            val expectedErrorCode =
                AuthenticationError.biometricFrameworkToJetpackErrorMap[frameworkError]
            val intent = prepareIntentWithGetRequest(GET_CREDENTIAL_REQUEST, biometricPromptResult)

            val retrievedRequest = PendingIntentHandler.retrieveProviderGetCredentialRequest(intent)

            Assert.assertNotNull(retrievedRequest)
            equals(GET_CREDENTIAL_REQUEST, retrievedRequest!!)
            Assert.assertNotNull(retrievedRequest.biometricPromptResult!!.authenticationError)
            Assert.assertEquals(
                retrievedRequest.biometricPromptResult!!.authenticationError!!.errorCode,
                expectedErrorCode,
            )
        }
    }

    @Test
    fun createCredentialException_success() {
        val intent = Intent()
        val expected = CreateCredentialInterruptedException("message")

        PendingIntentHandler.setCreateCredentialException(intent, expected)

        val actual = PendingIntentHandler.retrieveCreateCredentialException(intent)!!
        assertThat(actual).isInstanceOf(expected::class.java)
        assertThat(actual.type).isEqualTo(expected.type)
        assertThat(actual.errorMessage).isEqualTo(expected.errorMessage)
    }

    @Test
    fun createCredentialException_domException_success() {
        val intent = Intent()
        val expected = CreatePublicKeyCredentialDomException(ConstraintError(), "Error msg")

        PendingIntentHandler.setCreateCredentialException(intent, expected)

        val actual = PendingIntentHandler.retrieveCreateCredentialException(intent)!!
        assertThat(actual).isInstanceOf(expected::class.java)
        assertThat(actual.type).isEqualTo(expected.type)
        assertThat(actual.errorMessage).isEqualTo(expected.errorMessage)
        val actualConverted = actual as CreatePublicKeyCredentialDomException
        assertThat(actualConverted.domError).isInstanceOf((expected.domError)::class.java)
    }

    @Test
    fun createCredentialException_emptyIntent_returnsNull() {
        val intent = Intent()

        assertThat(PendingIntentHandler.retrieveCreateCredentialException(intent)).isNull()
    }

    @Test
    fun test_retrieveProviderCreateCredReqWithSuccessfulBpAuth() {
        val biometricPromptResult =
            BiometricPromptResult(AuthenticationResult(BIOMETRIC_AUTHENTICATOR_TYPE))
        val request = setUpCreatePasswordRequest()
        val intent = prepareIntentWithCreateRequest(request, biometricPromptResult)

        val retrievedRequest = PendingIntentHandler.retrieveProviderCreateCredentialRequest(intent)

        Assert.assertNotNull(request)
        equals(request, retrievedRequest!!)
        Assert.assertNotNull(biometricPromptResult.authenticationResult)
    }

    @Test
    fun test_retrieveProviderCreateCredReqWithFailureBpAuth() {
        val biometricPromptResult =
            BiometricPromptResult(
                AuthenticationError(
                    BIOMETRIC_AUTHENTICATOR_ERROR_CODE,
                    BIOMETRIC_AUTHENTICATOR_ERROR_MSG,
                )
            )
        val request = setUpCreatePasswordRequest()
        val intent = prepareIntentWithCreateRequest(request, biometricPromptResult)

        val retrievedRequest = PendingIntentHandler.retrieveProviderCreateCredentialRequest(intent)

        Assert.assertNotNull(retrievedRequest)
        equals(request, retrievedRequest!!)
        Assert.assertEquals(biometricPromptResult, retrievedRequest.biometricPromptResult)
    }

    @Test
    fun test_retrieveProviderGetCredReqWithSuccessfulBpAuth() {
        val biometricPromptResult =
            BiometricPromptResult(AuthenticationResult(BIOMETRIC_AUTHENTICATOR_TYPE))
        val intent = prepareIntentWithGetRequest(GET_CREDENTIAL_REQUEST, biometricPromptResult)

        val request = PendingIntentHandler.retrieveProviderGetCredentialRequest(intent)

        Assert.assertNotNull(request)
        equals(GET_CREDENTIAL_REQUEST, request!!)
        Assert.assertEquals(biometricPromptResult, request.biometricPromptResult)
    }

    @Test
    fun test_retrieveProviderGetCredReqWithFailingBpAuth() {
        val biometricPromptResult =
            BiometricPromptResult(
                AuthenticationError(
                    BIOMETRIC_AUTHENTICATOR_ERROR_CODE,
                    BIOMETRIC_AUTHENTICATOR_ERROR_MSG,
                )
            )
        val intent = prepareIntentWithGetRequest(GET_CREDENTIAL_REQUEST, biometricPromptResult)

        val request = PendingIntentHandler.retrieveProviderGetCredentialRequest(intent)

        Assert.assertNotNull(request)
        equals(GET_CREDENTIAL_REQUEST, request!!)
        Assert.assertEquals(biometricPromptResult, request.biometricPromptResult)
    }

    private fun prepareIntentWithGetRequest(
        request: GetCredentialRequest,
        biometricPromptResult: BiometricPromptResult,
    ): Intent {
        val intent = Intent()
        intent.putExtra(
            android.service.credentials.CredentialProviderService.EXTRA_GET_CREDENTIAL_REQUEST,
            request,
        )
        prepareIntentWithBiometricResult(intent, biometricPromptResult)
        return intent
    }

    private fun prepareIntentWithCreateRequest(
        request: CreateCredentialRequest,
        biometricPromptResult: BiometricPromptResult,
    ): Intent {
        val intent = Intent()
        intent.putExtra(
            android.service.credentials.CredentialProviderService.EXTRA_CREATE_CREDENTIAL_REQUEST,
            request,
        )
        prepareIntentWithBiometricResult(intent, biometricPromptResult)
        return intent
    }

    private fun prepareIntentWithBiometricResult(
        intent: Intent,
        biometricPromptResult: BiometricPromptResult,
    ) {
        if (biometricPromptResult.isSuccessful) {
            Assert.assertNotNull(biometricPromptResult.authenticationResult)
            var extraResultKey = AuthenticationResult.EXTRA_BIOMETRIC_AUTH_RESULT_TYPE
            intent.putExtra(
                extraResultKey,
                biometricPromptResult.authenticationResult!!.authenticationType,
            )
        } else {
            Assert.assertNotNull(biometricPromptResult.authenticationError)
            var extraErrorKey = AuthenticationError.EXTRA_BIOMETRIC_AUTH_ERROR
            var extraErrorMessageKey = AuthenticationError.EXTRA_BIOMETRIC_AUTH_ERROR_MESSAGE
            intent.putExtra(extraErrorKey, biometricPromptResult.authenticationError!!.errorCode)
            intent.putExtra(
                extraErrorMessageKey,
                biometricPromptResult.authenticationError!!.errorMsg,
            )
        }
    }

    @Test
    fun test_credentialException() {
        val intent = Intent()
        val initialException = GetCredentialInterruptedException("message")

        PendingIntentHandler.setGetCredentialException(intent, initialException)

        val finalException = intent.getGetCredentialException()
        assertThat(finalException).isNotNull()
        assertThat(finalException!!.type).isEqualTo(initialException.type)
        assertThat(finalException.message).isEqualTo(initialException.message)
    }

    @Test
    fun test_credentialException_nullWhenEmptyIntent() {
        val intent = Intent()

        assertThat(intent.getGetCredentialException()).isNull()
    }

    @Test
    fun test_beginGetResponse() {
        val intent = Intent()
        val initialResponse = BeginGetCredentialResponse.Builder().build()

        PendingIntentHandler.setBeginGetCredentialResponse(intent, initialResponse)

        val finalResponse = intent.getBeginGetResponse()
        assertThat(finalResponse).isNotNull()
        assertEquals(context, finalResponse!!, initialResponse)
    }

    @Test
    fun test_beginGetResponse_nullWhenEmptyIntent() {
        val intent = Intent()

        assertThat(intent.getBeginGetResponse()).isNull()
    }

    @Test
    fun test_credentialResponse() {
        val intent = Intent()
        val credential = PasswordCredential("a", "b")
        val initialResponse = GetCredentialResponse(credential)

        PendingIntentHandler.setGetCredentialResponse(intent, initialResponse)

        val finalResponse = intent.getGetCredentialResponse()
        assertThat(finalResponse).isNotNull()
        assertEquals(finalResponse!!, initialResponse)
    }

    @Test
    fun test_credentialResponse_nullWhenEmptyIntent() {
        val intent = Intent()

        assertThat(intent.getGetCredentialResponse()).isNull()
    }

    @Test
    fun createCredentialCredentialResponse_passwordResponse_success() {
        val intent = Intent()
        val expected = CreatePasswordResponse()

        PendingIntentHandler.setCreateCredentialResponse(intent, expected)

        val actual = PendingIntentHandler.retrieveCreateCredentialResponse(expected.type, intent)!!
        assertEquals(actual, expected)
    }

    @Test
    fun setCreateCredentialResponse_customResponse_success() {
        val intent = Intent()
        val customData = Bundle()
        customData.putString("k1", "text")
        customData.putBinder("k2", Binder())
        val expected = CreateCustomCredentialResponse("type", customData)

        setCreateCredentialResponse(intent, expected)

        val actual = PendingIntentHandler.retrieveCreateCredentialResponse(expected.type, intent)!!
        assertEquals(actual, expected)
    }

    @Test
    fun retrieveCreateCredentialResponse_emptyResponse_returnsNull() {
        val actual = PendingIntentHandler.retrieveCreateCredentialResponse("type", Intent())

        assertThat(actual).isNull()
    }
}
