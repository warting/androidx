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

package androidx.privacysandbox.sdkruntime.integration.testapp

import android.os.Bundle
import androidx.privacysandbox.sdkruntime.core.LoadSdkCompatException
import androidx.privacysandbox.sdkruntime.integration.callDoSomething
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import androidx.testutils.assertThrows
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class SdkLoadingIntegrationTest {

    @get:Rule val testSetup = IntegrationTestSetupRule()

    @Test
    fun loadSdkFromApp_successTest() = runTest {
        val testAppApi = testSetup.testAppApi()
        assertThat(testAppApi.getSandboxedSdks()).hasSize(0)

        testAppApi.loadTestSdk()

        assertThat(testAppApi.getSandboxedSdks()).hasSize(1)
    }

    @Test
    fun loadSdkFromApp_failTest() = runTest {
        val testAppApi = testSetup.testAppApi()

        val params = Bundle()
        params.putBoolean("needFail", true)
        assertThrows<LoadSdkCompatException> { testAppApi.loadTestSdk(params) }
        assertThat(testAppApi.getSandboxedSdks()).hasSize(0)
    }

    @Test
    fun unloadSdkTest() = runTest {
        val testAppApi = testSetup.testAppApi()
        testAppApi.loadTestSdk()
        assertThat(testAppApi.getSandboxedSdks()).hasSize(1)

        testAppApi.unloadTestSdk()
        assertThat(testAppApi.getSandboxedSdks()).hasSize(0)
    }

    @Test
    fun getLoadedSdksFromAppTest() = runTest {
        val testAppApi = testSetup.testAppApi()
        testAppApi.loadTestSdk()

        val loadedSdks = testAppApi.getSandboxedSdks()

        assertThat(loadedSdks).hasSize(1)
        assertThat(loadedSdks.first().sdkName).isEqualTo(TestAppApi.TEST_SDK_NAME)
    }

    @Test
    fun getLoadedSdksFromSdkTest() = runTest {
        val testAppApi = testSetup.testAppApi()

        val loadedSdks = testAppApi.loadTestSdk().getSandboxedSdks()

        assertThat(loadedSdks).hasSize(1)
        assertThat(loadedSdks.first().sdkName).isEqualTo(TestAppApi.TEST_SDK_NAME)
    }

    @Test
    fun callSdkMethodFromAppTest() = runTest {
        val testAppApi = testSetup.testAppApi()

        val resultFromLoadedSdk = testAppApi.loadTestSdk().doSomething("42")
        assertThat(resultFromLoadedSdk).isEqualTo("TestSdk result is 42")

        val resultFromGetSandboxedSdks =
            callDoSomething(testAppApi.getSandboxedSdks().first().sdkInterface, "42")
        assertThat(resultFromGetSandboxedSdks).isEqualTo("TestSdk result is 42")
    }

    @Test
    fun callSdkMethodFromSdkTest() = runTest {
        val testAppApi = testSetup.testAppApi()

        val result = testAppApi.loadTestSdk().callDoSomethingOnSandboxedSdks("42")

        assertThat(result).containsExactly("TestSdk result is 42")
    }
}
