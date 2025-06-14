/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.credentials.playservices.controllers.identitycredentials.signalcredentialstate

import androidx.credentials.SignalUnknownCredentialStateRequest
import androidx.credentials.playservices.TestCredentialsActivity
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
@SdkSuppress(minSdkVersion = 28)
class SignalCredentialStateControllerTest {

    @Test
    fun convertRequestToPlayServices_success() {
        val requestJson = "{\"json\" : \"value\"}"
        val request = SignalUnknownCredentialStateRequest(requestJson = requestJson, "origin")
        val activityScenario = ActivityScenario.launch(TestCredentialsActivity::class.java)

        activityScenario.onActivity { activity: TestCredentialsActivity? ->
            val convertedRequest =
                SignalCredentialStateController(activity!!).convertRequestToPlayServices(request)

            Truth.assertThat(convertedRequest.origin).isEqualTo(request.origin)
            Truth.assertThat(
                    convertedRequest.requestData.getString(
                        SignalCredentialStateController.SIGNAL_REQUEST_JSON_KEY
                    )
                )
                .isEqualTo(requestJson)
            Truth.assertThat(convertedRequest.type).isEqualTo(request.type)
        }
    }
}
