/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.wear.phone.interactions.authentication

import android.content.Context
import android.net.Uri
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.SdkSuppress
import androidx.wear.phone.interactions.WearPhoneInteractionsTestRunner
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument
import org.robolectric.shadows.ShadowPackageManager

/** Unit tests for [OAuthRequest] and [OAuthResponse] */
@RunWith(WearPhoneInteractionsTestRunner::class)
@DoNotInstrument
@Config(minSdk = 28)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
public class OAuthRequestResponseTest {
    internal companion object {
        private val context: Context = ApplicationProvider.getApplicationContext()
        private val packageManager: ShadowPackageManager = Shadows.shadowOf(context.packageManager)
        private const val authProviderUrl = "http://account.myapp.com/auth"
        private const val clientId = "iamtheclient"
        private val appPackageName = context.packageName
        private const val redirectUrl = OAuthRequest.WEAR_REDIRECT_URL_PREFIX
        private val redirectUrlWithPackageName = "$redirectUrl$appPackageName"
        private val redirectUrlWithPackageName_cn =
            "${OAuthRequest.WEAR_REDIRECT_URL_PREFIX_CN}$appPackageName"
        private const val customRedirectUrl = "https://app.example.com/oauth2redirect"
        private val customRedirectUrlWithPackageName = "$customRedirectUrl/$appPackageName"
        private val responseUrl = Uri.parse("http://myresponseurl")
    }

    @Before
    fun setUp() {
        // We need to make sure that context.packageName is not empty as it can lead to passing
        // tests when they shouldn't.
        assertThat(context.packageName).isNotEmpty()
    }

    private fun setSystemFeatureChina(value: Boolean) {
        packageManager.setSystemFeature("cn.google", value)
    }

    private fun checkBuildSuccess(
        builder: OAuthRequest.Builder,
        expectedAuthProviderUrl: String,
        expectedClientId: String,
        expectedRedirectUri: String,
        expectedCodeChallenge: String,
    ) {
        var request: OAuthRequest? = null
        try {
            request = builder.build()
        } catch (e: Exception) {
            fail("The build shall succeed and this line will not be executed")
        }

        val requestUrl = request!!.requestUrl
        assertThat(requestUrl.toString().indexOf(expectedAuthProviderUrl)).isEqualTo(0)
        assertThat(requestUrl.getQueryParameter("redirect_uri")).isEqualTo(expectedRedirectUri)
        assertThat(requestUrl.getQueryParameter("client_id")).isEqualTo(expectedClientId)
        assertThat(requestUrl.getQueryParameter("response_type")).isEqualTo("code")
        assertThat(requestUrl.getQueryParameter("code_challenge")).isEqualTo(expectedCodeChallenge)
        assertThat(requestUrl.getQueryParameter("code_challenge_method")).isEqualTo("S256")
    }

    private fun checkBuildFailure(builder: OAuthRequest.Builder, errorMsg: String) {
        try {
            builder.build()
            fail("should fail without providing correct/adequate info for building request")
        } catch (e: Exception) {
            assertThat(e.message).isEqualTo(errorMsg)
        }
    }

    @Test
    public fun testRequestBuildSuccessWithSetters() {
        setSystemFeatureChina(false)
        val codeChallenge = CodeChallenge(CodeVerifier())
        val requestBuilder =
            OAuthRequest.Builder(context)
                .setAuthProviderUrl(Uri.parse(authProviderUrl))
                .setClientId(clientId)
                .setCodeChallenge(codeChallenge)

        checkBuildSuccess(
            requestBuilder,
            authProviderUrl,
            clientId,
            redirectUrlWithPackageName,
            codeChallenge.value,
        )
    }

    @Test
    public fun testRequestBuildSuccessWithSetters_cn() {
        setSystemFeatureChina(true)
        val codeChallenge = CodeChallenge(CodeVerifier())
        val requestBuilder =
            OAuthRequest.Builder(context)
                .setAuthProviderUrl(Uri.parse(authProviderUrl))
                .setClientId(clientId)
                .setCodeChallenge(codeChallenge)

        checkBuildSuccess(
            requestBuilder,
            authProviderUrl,
            clientId,
            redirectUrlWithPackageName_cn,
            codeChallenge.value,
        )
    }

    @Test
    public fun testRequestBuildSuccessWithCustomRedirectUri() {
        setSystemFeatureChina(false)
        val codeChallenge = CodeChallenge(CodeVerifier())
        val requestBuilder =
            OAuthRequest.Builder(context)
                .setAuthProviderUrl(Uri.parse(authProviderUrl))
                .setClientId(clientId)
                .setRedirectUrl(Uri.parse(customRedirectUrl))
                .setCodeChallenge(codeChallenge)

        checkBuildSuccess(
            requestBuilder,
            authProviderUrl,
            clientId,
            customRedirectUrlWithPackageName,
            codeChallenge.value,
        )
    }

    @Test
    public fun testRequestBuildSuccessWithCustomRedirectUri_cn() {
        setSystemFeatureChina(true)
        val codeChallenge = CodeChallenge(CodeVerifier())
        val requestBuilder =
            OAuthRequest.Builder(context)
                .setAuthProviderUrl(Uri.parse(authProviderUrl))
                .setClientId(clientId)
                .setRedirectUrl(Uri.parse(customRedirectUrl))
                .setCodeChallenge(codeChallenge)

        checkBuildSuccess(
            requestBuilder,
            authProviderUrl,
            clientId,
            customRedirectUrlWithPackageName,
            codeChallenge.value,
        )
    }

    @Test
    public fun testRequestBuildSuccessWithCompleteUrl() {
        setSystemFeatureChina(false)
        val codeChallenge = CodeChallenge(CodeVerifier())
        val requestBuilder =
            OAuthRequest.Builder(context)
                .setAuthProviderUrl(
                    Uri.parse(
                        "$authProviderUrl?client_id=$clientId" +
                            "&redirect_uri=$redirectUrlWithPackageName" +
                            "&response_type=code" +
                            "&code_challenge=${codeChallenge.value}" +
                            "&code_challenge_method=S256"
                    )
                )

        checkBuildSuccess(
            requestBuilder,
            authProviderUrl,
            clientId,
            redirectUrlWithPackageName,
            codeChallenge.value,
        )
    }

    @Test
    public fun testRequestBuildFailureWithoutAuthProviderUrl() {
        setSystemFeatureChina(false)
        val builder =
            OAuthRequest.Builder(context)
                .setClientId(clientId)
                .setCodeChallenge(CodeChallenge(CodeVerifier()))

        checkBuildFailure(builder, "The request requires the auth provider url to be provided.")
    }

    @Test
    public fun testRequestBuildFailureWithoutClientId() {
        setSystemFeatureChina(false)
        val builder =
            OAuthRequest.Builder(context)
                .setAuthProviderUrl(Uri.parse(authProviderUrl))
                .setCodeChallenge(CodeChallenge(CodeVerifier()))

        checkBuildFailure(
            builder,
            "The use of Proof Key for Code Exchange is required for authentication, " +
                "please provide client_id in the request.",
        )
    }

    @Test
    public fun testRequestBuildFailureWithConflictedClientId() {
        setSystemFeatureChina(false)
        val builder =
            OAuthRequest.Builder(context)
                .setAuthProviderUrl(Uri.parse("$authProviderUrl?client_id=XXX"))
                .setClientId(clientId)
                .setCodeChallenge(CodeChallenge(CodeVerifier()))

        checkBuildFailure(
            builder,
            "The 'client_id' query param already exists in the authProviderUrl, " +
                "expect to have the value of '$clientId', but " +
                "'XXX' is given. Please correct it,  or leave it out " +
                "to allow the request builder to append it automatically.",
        )
    }

    @Test
    public fun testRequestBuildSuccessWithDuplicatedClientId() {
        setSystemFeatureChina(false)
        val codeChallenge = CodeChallenge(CodeVerifier())
        val builder =
            OAuthRequest.Builder(context)
                .setAuthProviderUrl(Uri.parse("$authProviderUrl?client_id=$clientId"))
                .setClientId(clientId)
                .setCodeChallenge(codeChallenge)

        checkBuildSuccess(
            builder,
            authProviderUrl,
            clientId,
            redirectUrlWithPackageName,
            codeChallenge.value,
        )
    }

    @Test
    public fun testRequestBuildFailureWithoutCodeChallenge() {
        setSystemFeatureChina(false)
        val builder =
            OAuthRequest.Builder(context)
                .setAuthProviderUrl(Uri.parse(authProviderUrl))
                .setClientId(clientId)

        checkBuildFailure(
            builder,
            "The use of Proof Key for Code Exchange is required for authentication, " +
                "please provide code_challenge in the request.",
        )
    }

    @Test
    public fun testRequestBuildFailureWithConflictedCodeChallenge() {
        setSystemFeatureChina(false)
        val codeChallenge = CodeChallenge(CodeVerifier())
        val builder =
            OAuthRequest.Builder(context)
                .setAuthProviderUrl(Uri.parse("$authProviderUrl?code_challenge=XXX"))
                .setClientId(clientId)
                .setCodeChallenge(codeChallenge)

        checkBuildFailure(
            builder,
            "The 'code_challenge' query param already exists in the authProviderUrl, " +
                "expect to have the value of '${codeChallenge.value}', but " +
                "'XXX' is given. Please correct it,  or leave it out " +
                "to allow the request builder to append it automatically.",
        )
    }

    @Test
    public fun testRequestBuildSuccessWithDuplicatedCodeChallenge() {
        setSystemFeatureChina(false)
        val codeChallenge = CodeChallenge(CodeVerifier())
        val builder =
            OAuthRequest.Builder(context)
                .setAuthProviderUrl(
                    Uri.parse("$authProviderUrl?code_challenge=${codeChallenge.value}")
                )
                .setClientId(clientId)
                .setCodeChallenge(codeChallenge)

        checkBuildSuccess(
            builder,
            authProviderUrl,
            clientId,
            redirectUrlWithPackageName,
            codeChallenge.value,
        )
    }

    @Test
    public fun testRequestBuildFailureWithWrongResponseType() {
        setSystemFeatureChina(false)
        val builder =
            OAuthRequest.Builder(context)
                .setAuthProviderUrl(Uri.parse("$authProviderUrl?response_type=XXX"))
                .setClientId(clientId)
                .setCodeChallenge(CodeChallenge(CodeVerifier()))

        checkBuildFailure(
            builder,
            "The 'response_type' query param already exists in the authProviderUrl, " +
                "expect to have the value of 'code', but " +
                "'XXX' is given. Please correct it,  or leave it out " +
                "to allow the request builder to append it automatically.",
        )
    }

    @Test
    public fun testRequestBuildFailureWithDuplicatedResponseType() {
        setSystemFeatureChina(false)
        val codeChallenge = CodeChallenge(CodeVerifier())
        val builder =
            OAuthRequest.Builder(context)
                .setAuthProviderUrl(Uri.parse("$authProviderUrl?response_type=code"))
                .setClientId(clientId)
                .setCodeChallenge(codeChallenge)

        checkBuildSuccess(
            builder,
            authProviderUrl,
            clientId,
            redirectUrlWithPackageName,
            codeChallenge.value,
        )
    }

    @Test
    public fun testRequestBuildFailureWithWrongCodeChallengeMethod() {
        setSystemFeatureChina(false)
        val builder =
            OAuthRequest.Builder(context)
                .setAuthProviderUrl(Uri.parse("$authProviderUrl?code_challenge_method=PLAIN"))
                .setClientId(clientId)
                .setCodeChallenge(CodeChallenge(CodeVerifier()))

        checkBuildFailure(
            builder,
            "The 'code_challenge_method' query param already exists in the authProviderUrl, " +
                "expect to have the value of 'S256', but " +
                "'PLAIN' is given. Please correct it,  or leave it out " +
                "to allow the request builder to append it automatically.",
        )
    }

    @Test
    public fun testRequestBuildFailureWithDuplicatedCodeChallengeMethod() {
        setSystemFeatureChina(false)
        val codeChallenge = CodeChallenge(CodeVerifier())
        val builder =
            OAuthRequest.Builder(context)
                .setAuthProviderUrl(Uri.parse("$authProviderUrl?code_challenge_method=S256"))
                .setClientId(clientId)
                .setCodeChallenge(codeChallenge)

        checkBuildSuccess(
            builder,
            authProviderUrl,
            clientId,
            redirectUrlWithPackageName,
            codeChallenge.value,
        )
    }

    @Test
    public fun testRequestBuildFailureWithConflictedRedirectUri() {
        setSystemFeatureChina(false)
        val codeChallenge = CodeChallenge(CodeVerifier())
        val builder =
            OAuthRequest.Builder(context)
                .setAuthProviderUrl(
                    Uri.parse("$authProviderUrl?redirect_uri=$redirectUrlWithPackageName")
                )
                .setRedirectUrl(Uri.parse(customRedirectUrl))
                .setClientId(clientId)
                .setCodeChallenge(codeChallenge)

        checkBuildFailure(
            builder,
            "The 'redirect_uri' query param already exists in the authProviderUrl, " +
                "expect to have the value of '$customRedirectUrlWithPackageName', but " +
                "'$redirectUrlWithPackageName' is given. Please correct it,  or leave it out " +
                "to allow the request builder to append it automatically.",
        )
    }

    @Test
    public fun testNoErrorResponseBuild() {
        val response = OAuthResponse.Builder().setResponseUrl(responseUrl).build()

        assertThat(response.errorCode).isEqualTo(RemoteAuthClient.NO_ERROR)
        assertThat(response.responseUrl).isEqualTo(responseUrl)
    }

    @Test
    public fun testErrorResponseBuild() {
        val response1 =
            OAuthResponse.Builder().setErrorCode(RemoteAuthClient.ERROR_UNSUPPORTED).build()

        assertThat(response1.errorCode).isEqualTo(RemoteAuthClient.ERROR_UNSUPPORTED)
        assertThat(response1.responseUrl).isNull()

        val response2 =
            OAuthResponse.Builder().setErrorCode(RemoteAuthClient.ERROR_PHONE_UNAVAILABLE).build()

        assertThat(response2.errorCode).isEqualTo(RemoteAuthClient.ERROR_PHONE_UNAVAILABLE)
        assertThat(response2.responseUrl).isNull()
    }

    @Test
    public fun testGetRedirectUrl() {
        setSystemFeatureChina(false)
        val codeChallenge = CodeChallenge(CodeVerifier())
        val builder =
            OAuthRequest.Builder(context)
                .setAuthProviderUrl(Uri.parse(authProviderUrl))
                .setClientId(clientId)
                .setCodeChallenge(codeChallenge)

        // Building should always be successful and it's already tested in the previous tests, so
        // no need for try-catch block as in #checkBuildFailure.
        val request = builder.build()
        assertThat(request.redirectUrl).isEqualTo(redirectUrlWithPackageName)
    }

    @Test
    public fun testGetRedirectUrl_cn() {
        setSystemFeatureChina(true)
        val codeChallenge = CodeChallenge(CodeVerifier())
        val builder =
            OAuthRequest.Builder(context)
                .setAuthProviderUrl(Uri.parse(authProviderUrl))
                .setClientId(clientId)
                .setCodeChallenge(codeChallenge)

        // Building should always be successful and it's already tested in the previous tests, so
        // no need for try-catch block as in #checkBuildFailure.
        val request = builder.build()
        assertThat(request.redirectUrl).isEqualTo(redirectUrlWithPackageName_cn)
    }

    @Test
    public fun testGetRedirectUrlWithCustomRedirectUri() {
        setSystemFeatureChina(false)
        val codeChallenge = CodeChallenge(CodeVerifier())
        val builder =
            OAuthRequest.Builder(context)
                .setAuthProviderUrl(Uri.parse(authProviderUrl))
                .setClientId(clientId)
                .setRedirectUrl(Uri.parse(customRedirectUrl))
                .setCodeChallenge(codeChallenge)

        // Building should always be successful and it's already tested in the previous tests, so
        // no need for try-catch block as in #checkBuildFailure.
        val request = builder.build()
        assertThat(request.redirectUrl).isEqualTo(customRedirectUrlWithPackageName)
    }

    @Test
    public fun testGetRedirectUrl_builtWithConstructor() {
        val requestUrl =
            Uri.parse(authProviderUrl)
                .buildUpon()
                .appendQueryParameter(OAuthRequest.REDIRECT_URI_KEY, customRedirectUrl)
                .build()
        val request = OAuthRequest(appPackageName, requestUrl)

        assertThat(request.redirectUrl).isEqualTo(customRedirectUrl)
    }

    @Test
    public fun testGetRedirectUrl_builtWithConstructor_empty() {
        val requestUrl = Uri.parse(authProviderUrl)
        val request = OAuthRequest(appPackageName, requestUrl)

        assertThat(request.redirectUrl).isEmpty()
    }
}
