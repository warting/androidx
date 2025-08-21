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

package androidx.camera.integration.core

import android.content.Context
import android.content.pm.PackageManager
import androidx.annotation.OptIn
import androidx.camera.camera2.Camera2Config
import androidx.camera.camera2.pipe.integration.CameraPipeConfig
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraXConfig
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ExperimentalCameraProviderConfiguration
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.testing.impl.LabTestRule
import androidx.camera.testing.impl.fakes.FakeLifecycleOwner
import androidx.concurrent.futures.await
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@LargeTest
@RunWith(Parameterized::class)
@OptIn(ExperimentalCameraProviderConfiguration::class)
class CameraXInitTest(private val implName: String, private val cameraXConfig: CameraXConfig) {

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data() =
            listOf(
                arrayOf(Camera2Config::class.simpleName, Camera2Config.defaultConfig()),
                arrayOf(CameraPipeConfig::class.simpleName, CameraPipeConfig.defaultConfig()),
            )
    }

    // Don't use CameraUtil.grantCameraPermissionAndPreTest. This test verifies the CameraX
    // initialization can be successfully done on a real device.
    @get:Rule
    val permissionRule: GrantPermissionRule =
        GrantPermissionRule.grant(android.Manifest.permission.CAMERA)

    @get:Rule val labTest: LabTestRule = LabTestRule()

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val packageManager = context.packageManager
    private lateinit var cameraProvider: ProcessCameraProvider
    private lateinit var lifecycleOwner: FakeLifecycleOwner

    @Before
    fun setup() {
        // Only test the device when it has at least 1 camera. Don't use CameraUtil
        // .deviceHasCamera() to check the camera, it might ignore the test if the camera device
        // is in a bad state.
        assumeTrue(
            packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA) ||
                packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FRONT)
        )
        lifecycleOwner = FakeLifecycleOwner()
    }

    @After
    fun tearDown() {
        if (::cameraProvider.isInitialized) {
            cameraProvider.shutdownAsync()[10, TimeUnit.SECONDS]
        }
    }

    @Test
    fun initOnDevice(): Unit = runBlocking {
        withTimeout(10000) {
            ProcessCameraProvider.configureInstance(cameraXConfig)
            cameraProvider = ProcessCameraProvider.getInstance(context).await()
        }
    }

    /**
     * Verifies that after initialization, the provider's APIs are functional. It checks that for
     * every camera CameraX reports as available, we can query its status and bind a use case to it
     * successfully.
     */
    @LabTestRule.LabTestOnly
    @Test
    fun availableCamerasCanBeUsed(): Unit = runBlocking {
        // 1. Initialize CameraX and wait for it to be ready.
        withTimeout(10000) {
            ProcessCameraProvider.configureInstance(cameraXConfig)
            cameraProvider = ProcessCameraProvider.getInstance(context).await()
        }
        lifecycleOwner.startAndResume()

        // 2. Get the list of all available cameras from the provider.
        val availableCameraInfos = cameraProvider.availableCameraInfos
        assertThat(availableCameraInfos).isNotEmpty()

        // 3. Check default cameras if the system feature is present.
        if (packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
            assertThat(cameraProvider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA)).isTrue()
        }
        if (packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FRONT)) {
            assertThat(cameraProvider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)).isTrue()
        }

        // 4. Iterate through each available camera to verify its APIs.
        for (cameraInfo in availableCameraInfos) {
            val selector = cameraInfo.cameraSelector

            // Verify hasCamera() works for the specific selector of this camera.
            assertThat(cameraProvider.hasCamera(selector)).isTrue()

            // Verify a Preview use case can be bound to this camera.
            val instrumentation = InstrumentationRegistry.getInstrumentation()
            val preview = Preview.Builder().build()
            try {

                var camera: Camera? = null
                instrumentation.runOnMainSync {
                    camera = cameraProvider.bindToLifecycle(lifecycleOwner, selector, preview)
                }
                assertThat(camera).isNotNull()
                assertThat(camera!!.cameraInfo.cameraIdentifier)
                    .isEqualTo(cameraInfo.cameraIdentifier)
            } finally {
                // Clean up bindings for the next iteration or test completion.
                instrumentation.runOnMainSync { cameraProvider.unbindAll() }
            }
        }
    }

    @Test
    fun configImplTypeIsCorrect(): Unit = runBlocking {
        withTimeout(10000) {
            ProcessCameraProvider.configureInstance(cameraXConfig)
            cameraProvider = ProcessCameraProvider.getInstance(context).await()

            assertThat(cameraProvider.configImplType)
                .isEqualTo(
                    if (implName == CameraPipeConfig::class.simpleName) {
                        CameraXConfig.CAMERAX_CONFIG_IMPL_TYPE_PIPE
                    } else {
                        CameraXConfig.CAMERAX_CONFIG_IMPL_TYPE_CAMERA_CAMERA2
                    }
                )
        }
    }
}
