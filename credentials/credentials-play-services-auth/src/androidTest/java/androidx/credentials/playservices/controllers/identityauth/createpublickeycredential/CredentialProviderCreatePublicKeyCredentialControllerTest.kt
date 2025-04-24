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

package androidx.credentials.playservices.controllers.identityauth.createpublickeycredential

import android.content.Context
import androidx.credentials.CreatePublicKeyCredentialRequest
import androidx.credentials.playservices.TestCredentialsActivity
import androidx.credentials.playservices.TestUtils
import androidx.credentials.playservices.controllers.utils.CreatePublicKeyCredentialControllerTestUtils
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.common.truth.Truth
import org.json.JSONException
import org.json.JSONObject
import org.junit.Assert
import org.junit.Assume
import org.junit.Test
import org.junit.function.ThrowingRunnable
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class CredentialProviderCreatePublicKeyCredentialControllerTest {
    @Test
    fun convertRequestToPlayServices_correctRequiredOnlyRequest_success() {
        val activityScenario = ActivityScenario.launch(TestCredentialsActivity::class.java)
        activityScenario.onActivity { activity: TestCredentialsActivity? ->
            try {
                val expectedJson =
                    JSONObject(
                        CreatePublicKeyCredentialControllerTestUtils.Companion
                            .MAIN_CREATE_JSON_ALL_REQUIRED_FIELDS_PRESENT
                    )

                val actualResponse =
                    CredentialProviderCreatePublicKeyCredentialController.Companion.getInstance(
                            activity!!
                        )
                        .convertRequestToPlayServices(
                            CreatePublicKeyCredentialRequest(
                                CreatePublicKeyCredentialControllerTestUtils.Companion
                                    .MAIN_CREATE_JSON_ALL_REQUIRED_FIELDS_PRESENT
                            )
                        )
                val actualJson =
                    CreatePublicKeyCredentialControllerTestUtils.Companion
                        .createJsonObjectFromPublicKeyCredentialCreationOptions(actualResponse)
                val requiredKeys =
                    JSONObject(
                        CreatePublicKeyCredentialControllerTestUtils.Companion
                            .ALL_REQUIRED_FIELDS_SIGNATURE
                    )

                Truth.assertThat(
                        TestUtils.Companion.isSubsetJson(expectedJson, actualJson, requiredKeys)
                    )
                    .isTrue()
                // TODO("Add remaining tests in detail after discussing ideal form")
            } catch (e: JSONException) {
                throw RuntimeException(e)
            }
        }
    }

    @Test
    fun convertRequestToPlayServices_correctRequiredAndOptionalRequest_success() {
        val activityScenario = ActivityScenario.launch(TestCredentialsActivity::class.java)
        activityScenario.onActivity { activity: TestCredentialsActivity? ->
            try {
                val expectedJson =
                    JSONObject(
                        CreatePublicKeyCredentialControllerTestUtils.Companion
                            .MAIN_CREATE_JSON_ALL_REQUIRED_AND_OPTIONAL_FIELDS_PRESENT
                    )

                val actualResponse =
                    CredentialProviderCreatePublicKeyCredentialController.Companion.getInstance(
                            activity!!
                        )
                        .convertRequestToPlayServices(
                            CreatePublicKeyCredentialRequest(
                                CreatePublicKeyCredentialControllerTestUtils.Companion
                                    .MAIN_CREATE_JSON_ALL_REQUIRED_AND_OPTIONAL_FIELDS_PRESENT
                            )
                        )
                val actualJson =
                    CreatePublicKeyCredentialControllerTestUtils.Companion
                        .createJsonObjectFromPublicKeyCredentialCreationOptions(actualResponse)
                val requiredKeys =
                    JSONObject(
                        CreatePublicKeyCredentialControllerTestUtils.Companion
                            .ALL_REQUIRED_AND_OPTIONAL_SIGNATURE
                    )

                Truth.assertThat(
                        TestUtils.Companion.isSubsetJson(expectedJson, actualJson, requiredKeys)
                    )
                    .isTrue()
                // TODO("Add remaining tests in detail after discussing ideal form")
            } catch (e: JSONException) {
                throw java.lang.RuntimeException(e)
            }
        }
    }

    @Test
    fun convertRequestToPlayServices_missingRequired_throws() {
        val activityScenario = ActivityScenario.launch(TestCredentialsActivity::class.java)
        activityScenario.onActivity { activity: TestCredentialsActivity? ->
            Assume.assumeFalse(deviceHasGMS(ApplicationProvider.getApplicationContext()))
            Assert.assertThrows(
                "Expected bad required json to throw",
                JSONException::class.java,
                ThrowingRunnable {
                    CredentialProviderCreatePublicKeyCredentialController.Companion.getInstance(
                            activity!!
                        )
                        .convertRequestToPlayServices(
                            CreatePublicKeyCredentialRequest(
                                CreatePublicKeyCredentialControllerTestUtils.Companion
                                    .MAIN_CREATE_JSON_MISSING_REQUIRED_FIELD
                            )
                        )
                }
            )
        }
    }

    @Test
    fun convertRequestToPlayServices_emptyRequired_throws() {
        val activityScenario = ActivityScenario.launch(TestCredentialsActivity::class.java)
        activityScenario.onActivity { activity: TestCredentialsActivity? ->
            Assume.assumeFalse(deviceHasGMS(ApplicationProvider.getApplicationContext()))
            Assert.assertThrows(
                "Expected bad required json to throw",
                JSONException::class.java,
                ThrowingRunnable {
                    CredentialProviderCreatePublicKeyCredentialController.Companion.getInstance(
                            activity!!
                        )
                        .convertRequestToPlayServices(
                            CreatePublicKeyCredentialRequest(
                                CreatePublicKeyCredentialControllerTestUtils.Companion
                                    .MAIN_CREATE_JSON_REQUIRED_FIELD_EMPTY
                            )
                        )
                }
            )
        }
    }

    @Test
    fun convertRequestToPlayServices_missingOptionalRequired_throws() {
        val activityScenario = ActivityScenario.launch(TestCredentialsActivity::class.java)
        activityScenario.onActivity { activity: TestCredentialsActivity? ->
            Assume.assumeFalse(deviceHasGMS(ApplicationProvider.getApplicationContext()))
            Assert.assertThrows(
                "Expected bad required json to throw",
                JSONException::class.java,
                ThrowingRunnable {
                    CredentialProviderCreatePublicKeyCredentialController.Companion.getInstance(
                            activity!!
                        )
                        .convertRequestToPlayServices(
                            CreatePublicKeyCredentialRequest(
                                CreatePublicKeyCredentialControllerTestUtils.Companion
                                    .OPTIONAL_FIELD_MISSING_REQUIRED_SUBFIELD
                            )
                        )
                }
            )
        }
    }

    @Test
    fun convertRequestToPlayServices_emptyOptionalRequired_throws() {
        val activityScenario = ActivityScenario.launch(TestCredentialsActivity::class.java)
        activityScenario.onActivity { activity: TestCredentialsActivity? ->
            Assume.assumeFalse(deviceHasGMS(ApplicationProvider.getApplicationContext()))
            Assert.assertThrows(
                "Expected bad required json to throw",
                JSONException::class.java,
                ThrowingRunnable {
                    CredentialProviderCreatePublicKeyCredentialController.Companion.getInstance(
                            activity!!
                        )
                        .convertRequestToPlayServices(
                            CreatePublicKeyCredentialRequest(
                                CreatePublicKeyCredentialControllerTestUtils.Companion
                                    .OPTIONAL_FIELD_WITH_EMPTY_REQUIRED_SUBFIELD
                            )
                        )
                }
            )
        }
    }

    @Test
    fun convertRequestToPlayServices_missingOptionalNotRequired_success() {
        val activityScenario = ActivityScenario.launch(TestCredentialsActivity::class.java)
        activityScenario.onActivity { activity: TestCredentialsActivity? ->
            try {
                val expectedJson =
                    JSONObject(
                        CreatePublicKeyCredentialControllerTestUtils.Companion
                            .OPTIONAL_FIELD_MISSING_OPTIONAL_SUBFIELD
                    )

                val actualResponse =
                    CredentialProviderCreatePublicKeyCredentialController.Companion.getInstance(
                            activity!!
                        )
                        .convertRequestToPlayServices(
                            CreatePublicKeyCredentialRequest(
                                CreatePublicKeyCredentialControllerTestUtils.Companion
                                    .OPTIONAL_FIELD_MISSING_OPTIONAL_SUBFIELD
                            )
                        )
                val actualJson =
                    CreatePublicKeyCredentialControllerTestUtils.Companion
                        .createJsonObjectFromPublicKeyCredentialCreationOptions(actualResponse)
                val requiredKeys =
                    JSONObject(
                        CreatePublicKeyCredentialControllerTestUtils.Companion
                            .OPTIONAL_FIELD_MISSING_OPTIONAL_SUBFIELD_SIGNATURE
                    )

                Truth.assertThat(
                        TestUtils.Companion.isSubsetJson(expectedJson, actualJson, requiredKeys)
                    )
                    .isTrue()
            } catch (e: JSONException) {
                throw java.lang.RuntimeException(e)
            }
        }
    }

    private fun deviceHasGMS(context: Context): Boolean =
        GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context) ==
            ConnectionResult.SUCCESS
}
