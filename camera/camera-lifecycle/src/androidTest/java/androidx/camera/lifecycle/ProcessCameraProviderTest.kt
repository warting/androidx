/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.camera.lifecycle

import android.app.Application
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import androidx.annotation.OptIn
import androidx.annotation.RequiresApi
import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraSelector.LENS_FACING_BACK
import androidx.camera.core.CameraSelector.LENS_FACING_FRONT
import androidx.camera.core.CameraXConfig
import androidx.camera.core.Preview
import androidx.camera.core.UseCaseGroup
import androidx.camera.core.concurrent.ConcurrentCameraConfig
import androidx.camera.core.concurrent.SingleCameraConfig
import androidx.camera.core.impl.CameraFactory
import androidx.camera.core.impl.utils.executor.CameraXExecutors.mainThreadExecutor
import androidx.camera.testing.fakes.FakeAppConfig
import androidx.camera.testing.fakes.FakeCamera
import androidx.camera.testing.fakes.FakeCameraCoordinator
import androidx.camera.testing.fakes.FakeCameraDeviceSurfaceManager
import androidx.camera.testing.fakes.FakeCameraFactory
import androidx.camera.testing.fakes.FakeCameraInfoInternal
import androidx.camera.testing.fakes.FakeLifecycleOwner
import androidx.camera.testing.fakes.FakeSurfaceEffect
import androidx.camera.testing.fakes.FakeSurfaceProcessor
import androidx.camera.testing.fakes.FakeUseCaseConfigFactory
import androidx.concurrent.futures.await
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import androidx.testutils.assertThrows
import com.google.common.truth.Truth.assertThat
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Test

@SmallTest
@SdkSuppress(minSdkVersion = 21)
class ProcessCameraProviderTest {

    private val context = ApplicationProvider.getApplicationContext() as Context
    private val lifecycleOwner0 = FakeLifecycleOwner()
    private val lifecycleOwner1 = FakeLifecycleOwner()

    private lateinit var provider: ProcessCameraProvider

    @After
    fun tearDown() {
        runBlocking(MainScope().coroutineContext) {
            try {
                val provider = ProcessCameraProvider.getInstance(context).await()
                provider.shutdown().await()
            } catch (e: IllegalStateException) {
                // ProcessCameraProvider may not be configured. Ignore.
            }
        }
    }

    @Test
    fun bindUseCaseGroupWithEffect_effectIsSetOnUseCase() {
        // Arrange.
        ProcessCameraProvider.configureInstance(FakeAppConfig.create())
        val surfaceProcessor = FakeSurfaceProcessor(mainThreadExecutor())
        val effect = FakeSurfaceEffect(
            mainThreadExecutor(),
            surfaceProcessor
        )
        val preview = Preview.Builder().setSessionOptionUnpacker { _, _ -> }.build()
        val useCaseGroup = UseCaseGroup.Builder().addUseCase(preview).addEffect(effect).build()

        runBlocking(MainScope().coroutineContext) {
            // Act.
            provider = ProcessCameraProvider.getInstance(context).await()
            provider.bindToLifecycle(
                lifecycleOwner0, CameraSelector.DEFAULT_BACK_CAMERA,
                useCaseGroup
            )

            // Assert.
            assertThat(preview.effect).isEqualTo(effect)
            assertThat(provider.isConcurrentCameraModeOn).isFalse()
        }
    }

    @Test
    fun canGetInstance_fromMetaData(): Unit = runBlocking {
        // Check the static invocation count for the test CameraXConfig.Provider which is defined
        // in the instrumentation test's AndroidManfiest.xml. It should be incremented after
        // retrieving the ProcessCameraProvider.
        val initialInvokeCount = TestMetaDataConfigProvider.invokeCount
        val contextWrapper = TestAppContextWrapper(context)
        provider = ProcessCameraProvider.getInstance(contextWrapper).await()
        assertThat(provider).isNotNull()
        assertThat(TestMetaDataConfigProvider.invokeCount).isGreaterThan(initialInvokeCount)
    }

    @OptIn(ExperimentalCameraProviderConfiguration::class)
    @Test
    fun configuredGetInstance_doesNotUseMetaData() {
        ProcessCameraProvider.configureInstance(FakeAppConfig.create())
        runBlocking {
            // Check the static invocation count for the test CameraXConfig.Provider which is defined
            // in the instrumentation test's AndroidManfiest.xml. It should NOT be incremented after
            // retrieving the ProcessCameraProvider since the ProcessCameraProvider is explicitly
            // configured.
            val initialInvokeCount = TestMetaDataConfigProvider.invokeCount
            val contextWrapper = TestAppContextWrapper(context)
            provider = ProcessCameraProvider.getInstance(contextWrapper).await()
            assertThat(provider).isNotNull()
            assertThat(TestMetaDataConfigProvider.invokeCount).isEqualTo(initialInvokeCount)
        }
    }

    @OptIn(ExperimentalCameraProviderConfiguration::class)
    @Test
    fun configuredGetInstance_doesNotUseApplication() {
        ProcessCameraProvider.configureInstance(FakeAppConfig.create())
        runBlocking {
            // Wrap the context with a TestAppContextWrapper and provide a context with an
            // Application that implements CameraXConfig.Provider. Because the
            // ProcessCameraProvider is already configured, this Application should not be used.
            val testApp = TestApplication(context.packageManager)
            val contextWrapper = TestAppContextWrapper(context, testApp)
            provider = ProcessCameraProvider.getInstance(contextWrapper).await()
            assertThat(provider).isNotNull()
            assertThat(testApp.providerUsed).isFalse()
        }
    }

    @Test
    fun unconfiguredGetInstance_usesApplicationProvider(): Unit = runBlocking {
        val testApp = TestApplication(context.packageManager)
        val contextWrapper = TestAppContextWrapper(context, testApp)
        provider = ProcessCameraProvider.getInstance(contextWrapper).await()
        assertThat(provider).isNotNull()
        assertThat(testApp.providerUsed).isTrue()
    }

    @OptIn(ExperimentalCameraProviderConfiguration::class)
    @Test
    fun multipleConfigureInstance_throwsISE() {
        val config = FakeAppConfig.create()
        ProcessCameraProvider.configureInstance(config)
        assertThrows<IllegalStateException> {
            ProcessCameraProvider.configureInstance(config)
        }
    }

    @OptIn(ExperimentalCameraProviderConfiguration::class)
    @Test
    fun configuredGetInstance_returnsProvider() {
        ProcessCameraProvider.configureInstance(FakeAppConfig.create())
        runBlocking {
            provider = ProcessCameraProvider.getInstance(context).await()
            assertThat(provider).isNotNull()
        }
    }

    @OptIn(ExperimentalCameraProviderConfiguration::class)
    @Test
    fun configuredGetInstance_usesConfiguredExecutor() {
        var executeCalled = false
        val config =
            CameraXConfig.Builder.fromConfig(FakeAppConfig.create()).setCameraExecutor { runnable ->
                run {
                    executeCalled = true
                    Dispatchers.Default.asExecutor().execute(runnable)
                }
            }.build()
        ProcessCameraProvider.configureInstance(config)
        runBlocking {
            ProcessCameraProvider.getInstance(context).await()
            assertThat(executeCalled).isTrue()
        }
    }

    @OptIn(ExperimentalCameraProviderConfiguration::class)
    @Test
    fun canRetrieveCamera_withZeroUseCases() {
        ProcessCameraProvider.configureInstance(FakeAppConfig.create())
        runBlocking(MainScope().coroutineContext) {
            provider = ProcessCameraProvider.getInstance(context).await()
            val camera =
                provider.bindToLifecycle(lifecycleOwner0, CameraSelector.DEFAULT_BACK_CAMERA)
            assertThat(camera).isNotNull()
            assertThat(provider.isConcurrentCameraModeOn).isFalse()
        }
    }

    @Test
    fun bindUseCase_isBound() {
        ProcessCameraProvider.configureInstance(FakeAppConfig.create())

        runBlocking(MainScope().coroutineContext) {
            provider = ProcessCameraProvider.getInstance(context).await()
            val useCase = Preview.Builder().setSessionOptionUnpacker { _, _ -> }.build()

            provider.bindToLifecycle(
                lifecycleOwner0, CameraSelector.DEFAULT_BACK_CAMERA,
                useCase
            )

            assertThat(provider.isBound(useCase)).isTrue()
            assertThat(provider.isConcurrentCameraModeOn).isFalse()
        }
    }

    @Test
    fun bindSecondUseCaseToDifferentLifecycle_firstUseCaseStillBound() {
        ProcessCameraProvider.configureInstance(FakeAppConfig.create())

        runBlocking(MainScope().coroutineContext) {
            provider = ProcessCameraProvider.getInstance(context).await()

            val useCase0 = Preview.Builder().setSessionOptionUnpacker { _, _ -> }.build()
            val useCase1 = Preview.Builder().setSessionOptionUnpacker { _, _ -> }.build()

            provider.bindToLifecycle(
                lifecycleOwner0, CameraSelector.DEFAULT_BACK_CAMERA,
                useCase0
            )
            provider.bindToLifecycle(
                lifecycleOwner1, CameraSelector.DEFAULT_BACK_CAMERA,
                useCase1
            )

            // TODO(b/158595693) Add check on whether or not camera for fakeUseCase0 should be
            //  exist or not
            // assertThat(fakeUseCase0.camera).isNotNull() (or isNull()?)
            assertThat(provider.isBound(useCase0)).isTrue()
            assertThat(useCase1.camera).isNotNull()
            assertThat(provider.isBound(useCase1)).isTrue()
            assertThat(provider.isConcurrentCameraModeOn).isFalse()
        }
    }

    @Test
    fun isNotBound_afterUnbind() {
        ProcessCameraProvider.configureInstance(FakeAppConfig.create())

        runBlocking(MainScope().coroutineContext) {
            provider = ProcessCameraProvider.getInstance(context).await()

            val useCase = Preview.Builder().setSessionOptionUnpacker { _, _ -> }.build()

            provider.bindToLifecycle(
                lifecycleOwner0,
                CameraSelector.DEFAULT_BACK_CAMERA,
                useCase
            )

            provider.unbind(useCase)

            assertThat(provider.isBound(useCase)).isFalse()
            assertThat(provider.isConcurrentCameraModeOn).isFalse()
        }
    }

    @Test
    fun unbindFirstUseCase_secondUseCaseStillBound() {
        ProcessCameraProvider.configureInstance(FakeAppConfig.create())

        runBlocking(MainScope().coroutineContext) {
            provider = ProcessCameraProvider.getInstance(context).await()

            val useCase0 = Preview.Builder().setSessionOptionUnpacker { _, _ -> }.build()
            val useCase1 = Preview.Builder().setSessionOptionUnpacker { _, _ -> }.build()

            provider.bindToLifecycle(
                lifecycleOwner0, CameraSelector.DEFAULT_BACK_CAMERA,
                useCase0, useCase1
            )

            provider.unbind(useCase0)

            assertThat(useCase0.camera).isNull()
            assertThat(provider.isBound(useCase0)).isFalse()
            assertThat(useCase1.camera).isNotNull()
            assertThat(provider.isBound(useCase1)).isTrue()
            assertThat(provider.isConcurrentCameraModeOn).isFalse()
        }
    }

    @Test
    fun unbindAll_unbindsAllUseCasesFromCameras() {
        ProcessCameraProvider.configureInstance(FakeAppConfig.create())

        runBlocking(MainScope().coroutineContext) {
            provider = ProcessCameraProvider.getInstance(context).await()

            val useCase = Preview.Builder().setSessionOptionUnpacker { _, _ -> }.build()

            provider.bindToLifecycle(
                lifecycleOwner0, CameraSelector.DEFAULT_BACK_CAMERA, useCase
            )

            provider.unbindAll()

            assertThat(useCase.camera).isNull()
            assertThat(provider.isBound(useCase)).isFalse()
            assertThat(provider.isConcurrentCameraModeOn).isFalse()
        }
    }

    @Test
    fun bindMultipleUseCases() {
        ProcessCameraProvider.configureInstance(FakeAppConfig.create())

        runBlocking(MainScope().coroutineContext) {
            provider = ProcessCameraProvider.getInstance(context).await()

            val useCase0 = Preview.Builder().setSessionOptionUnpacker { _, _ -> }.build()
            val useCase1 = Preview.Builder().setSessionOptionUnpacker { _, _ -> }.build()

            provider.bindToLifecycle(
                lifecycleOwner0, CameraSelector.DEFAULT_BACK_CAMERA, useCase0, useCase1
            )

            assertThat(provider.isBound(useCase0)).isTrue()
            assertThat(provider.isBound(useCase1)).isTrue()
            assertThat(provider.isConcurrentCameraModeOn).isFalse()
        }
    }

    @Test
    fun bind_createsDifferentLifecycleCameras_forDifferentLifecycles() {
        ProcessCameraProvider.configureInstance(FakeAppConfig.create())

        runBlocking(MainScope().coroutineContext) {
            provider = ProcessCameraProvider.getInstance(context).await()

            val useCase0 = Preview.Builder().setSessionOptionUnpacker { _, _ -> }.build()
            val camera0 = provider.bindToLifecycle(
                lifecycleOwner0,
                CameraSelector.DEFAULT_BACK_CAMERA, useCase0
            )

            val useCase1 = Preview.Builder().setSessionOptionUnpacker { _, _ -> }.build()
            val camera1 = provider.bindToLifecycle(
                lifecycleOwner1,
                CameraSelector.DEFAULT_BACK_CAMERA, useCase1
            )

            assertThat(camera0).isNotEqualTo(camera1)
            assertThat(provider.isConcurrentCameraModeOn).isFalse()
        }
    }

    @Test
    fun exception_withDestroyedLifecycle() {
        ProcessCameraProvider.configureInstance(FakeAppConfig.create())

        runBlocking(MainScope().coroutineContext) {
            provider = ProcessCameraProvider.getInstance(context).await()

            lifecycleOwner0.destroy()

            assertThrows<IllegalArgumentException> {
                provider.bindToLifecycle(lifecycleOwner0, CameraSelector.DEFAULT_BACK_CAMERA)
            }
            assertThat(provider.isConcurrentCameraModeOn).isFalse()
        }
    }

    @Test
    fun bind_returnTheSameCameraForSameSelectorAndLifecycleOwner() {
        ProcessCameraProvider.configureInstance(FakeAppConfig.create())

        runBlocking(MainScope().coroutineContext) {
            provider = ProcessCameraProvider.getInstance(context).await()
            val useCase0 = Preview.Builder().setSessionOptionUnpacker { _, _ -> }.build()
            val useCase1 = Preview.Builder().setSessionOptionUnpacker { _, _ -> }.build()

            val camera0 = provider.bindToLifecycle(
                lifecycleOwner0,
                CameraSelector
                    .DEFAULT_BACK_CAMERA,
                useCase0
            )
            val camera1 = provider.bindToLifecycle(
                lifecycleOwner0,
                CameraSelector
                    .DEFAULT_BACK_CAMERA,
                useCase1
            )

            assertThat(camera0).isSameInstanceAs(camera1)
            assertThat(provider.isConcurrentCameraModeOn).isFalse()
        }
    }

    @Test
    fun bindUseCases_withDifferentLensFacingButSameLifecycleOwner() {
        ProcessCameraProvider.configureInstance(FakeAppConfig.create())

        runBlocking(MainScope().coroutineContext) {
            provider = ProcessCameraProvider.getInstance(context).await()

            val useCase0 = Preview.Builder().setSessionOptionUnpacker { _, _ -> }.build()
            val useCase1 = Preview.Builder().setSessionOptionUnpacker { _, _ -> }.build()

            provider.bindToLifecycle(lifecycleOwner0, CameraSelector.DEFAULT_BACK_CAMERA, useCase0)

            assertThrows<IllegalArgumentException> {
                provider.bindToLifecycle(
                    lifecycleOwner0,
                    CameraSelector.DEFAULT_FRONT_CAMERA,
                    useCase1
                )
            }
            assertThat(provider.isConcurrentCameraModeOn).isFalse()
        }
    }

    @Test
    fun bindUseCases_withDifferentLensFacingAndLifecycle() {
        ProcessCameraProvider.configureInstance(FakeAppConfig.create())

        runBlocking(MainScope().coroutineContext) {
            provider = ProcessCameraProvider.getInstance(context).await()

            val useCase0 = Preview.Builder().setSessionOptionUnpacker { _, _ -> }.build()
            val useCase1 = Preview.Builder().setSessionOptionUnpacker { _, _ -> }.build()

            val camera0 = provider.bindToLifecycle(
                lifecycleOwner0,
                CameraSelector
                    .DEFAULT_BACK_CAMERA,
                useCase0
            )

            val camera1 = provider.bindToLifecycle(
                lifecycleOwner1,
                CameraSelector
                    .DEFAULT_FRONT_CAMERA,
                useCase1
            )

            assertThat(camera0).isNotEqualTo(camera1)
            assertThat(provider.isConcurrentCameraModeOn).isFalse()
        }
    }

    @Test
    fun bindUseCases_withNotExistedLensFacingCamera() {
        val cameraFactoryProvider =
            CameraFactory.Provider { _, _, _ ->
                val cameraFactory = FakeCameraFactory()
                cameraFactory.insertCamera(
                    CameraSelector.LENS_FACING_BACK,
                    "0"
                ) {
                    FakeCamera(
                        "0", null,
                        FakeCameraInfoInternal(
                            "0", 0,
                            CameraSelector.LENS_FACING_BACK
                        )
                    )
                }
                cameraFactory.cameraCoordinator = FakeCameraCoordinator()
                cameraFactory
            }

        val appConfigBuilder = CameraXConfig.Builder()
            .setCameraFactoryProvider(cameraFactoryProvider)
            .setDeviceSurfaceManagerProvider { _, _, _ -> FakeCameraDeviceSurfaceManager() }
            .setUseCaseConfigFactoryProvider { FakeUseCaseConfigFactory() }

        ProcessCameraProvider.configureInstance(appConfigBuilder.build())

        runBlocking(MainScope().coroutineContext) {
            provider = ProcessCameraProvider.getInstance(context).await()

            val useCase = Preview.Builder().setSessionOptionUnpacker { _, _ -> }.build()

            // The front camera is not defined, we should get the IllegalArgumentException when it
            // tries to get the camera.
            assertThrows<IllegalArgumentException> {
                provider.bindToLifecycle(
                    lifecycleOwner0,
                    CameraSelector.DEFAULT_FRONT_CAMERA,
                    useCase
                )
            }
            assertThat(provider.isConcurrentCameraModeOn).isFalse()
        }
    }

    @Test
    fun lifecycleCameraIsNotActive_withZeroUseCases_bindBeforeLifecycleStarted() {
        ProcessCameraProvider.configureInstance(FakeAppConfig.create())
        runBlocking(MainScope().coroutineContext) {
            provider = ProcessCameraProvider.getInstance(context).await()
            val camera: LifecycleCamera =
                provider.bindToLifecycle(lifecycleOwner0, CameraSelector.DEFAULT_BACK_CAMERA) as
                    LifecycleCamera
            lifecycleOwner0.startAndResume()
            assertThat(camera.isActive).isFalse()
            assertThat(provider.isConcurrentCameraModeOn).isFalse()
        }
    }

    @Test
    fun lifecycleCameraIsNotActive_withZeroUseCases_bindAfterLifecycleStarted() {
        ProcessCameraProvider.configureInstance(FakeAppConfig.create())
        runBlocking(MainScope().coroutineContext) {
            provider = ProcessCameraProvider.getInstance(context).await()
            lifecycleOwner0.startAndResume()
            val camera: LifecycleCamera =
                provider.bindToLifecycle(lifecycleOwner0, CameraSelector.DEFAULT_BACK_CAMERA) as
                    LifecycleCamera
            assertThat(camera.isActive).isFalse()
            assertThat(provider.isConcurrentCameraModeOn).isFalse()
        }
    }

    @Test
    fun lifecycleCameraIsActive_withUseCases_bindBeforeLifecycleStarted() {
        ProcessCameraProvider.configureInstance(FakeAppConfig.create())
        runBlocking(MainScope().coroutineContext) {
            provider = ProcessCameraProvider.getInstance(context).await()
            val useCase = Preview.Builder().setSessionOptionUnpacker { _, _ -> }.build()
            val camera: LifecycleCamera =
                provider.bindToLifecycle(
                    lifecycleOwner0, CameraSelector.DEFAULT_BACK_CAMERA,
                    useCase
                ) as LifecycleCamera
            lifecycleOwner0.startAndResume()
            assertThat(camera.isActive).isTrue()
            assertThat(provider.isConcurrentCameraModeOn).isFalse()
        }
    }

    @Test
    fun lifecycleCameraIsActive_withUseCases_bindAfterLifecycleStarted() {
        ProcessCameraProvider.configureInstance(FakeAppConfig.create())
        runBlocking(MainScope().coroutineContext) {
            provider = ProcessCameraProvider.getInstance(context).await()
            val useCase = Preview.Builder().setSessionOptionUnpacker { _, _ -> }.build()
            lifecycleOwner0.startAndResume()
            val camera: LifecycleCamera =
                provider.bindToLifecycle(
                    lifecycleOwner0, CameraSelector.DEFAULT_BACK_CAMERA,
                    useCase
                ) as LifecycleCamera
            assertThat(camera.isActive).isTrue()
            assertThat(provider.isConcurrentCameraModeOn).isFalse()
        }
    }

    @Test
    fun lifecycleCameraIsNotActive_unbindUseCase() {
        ProcessCameraProvider.configureInstance(FakeAppConfig.create())
        runBlocking(MainScope().coroutineContext) {
            provider = ProcessCameraProvider.getInstance(context).await()
            val useCase = Preview.Builder().setSessionOptionUnpacker { _, _ -> }.build()
            lifecycleOwner0.startAndResume()
            val camera: LifecycleCamera =
                provider.bindToLifecycle(
                    lifecycleOwner0, CameraSelector.DEFAULT_BACK_CAMERA,
                    useCase
                ) as LifecycleCamera
            assertThat(camera.isActive).isTrue()
            provider.unbind(useCase)
            assertThat(camera.isActive).isFalse()
            assertThat(provider.isConcurrentCameraModeOn).isFalse()
        }
    }

    @Test
    fun lifecycleCameraIsNotActive_unbindAll() {
        ProcessCameraProvider.configureInstance(FakeAppConfig.create())
        runBlocking(MainScope().coroutineContext) {
            provider = ProcessCameraProvider.getInstance(context).await()
            val useCase = Preview.Builder().setSessionOptionUnpacker { _, _ -> }.build()
            lifecycleOwner0.startAndResume()
            val camera: LifecycleCamera =
                provider.bindToLifecycle(
                    lifecycleOwner0, CameraSelector.DEFAULT_BACK_CAMERA,
                    useCase
                ) as LifecycleCamera
            assertThat(camera.isActive).isTrue()
            provider.unbindAll()
            assertThat(camera.isActive).isFalse()
            assertThat(provider.isConcurrentCameraModeOn).isFalse()
        }
    }

    @Test
    fun getAvailableCameraInfos_usesAllCameras() {
        ProcessCameraProvider.configureInstance(FakeAppConfig.create())
        runBlocking {
            provider = ProcessCameraProvider.getInstance(context).await()
            assertThat(provider.availableCameraInfos.size).isEqualTo(2)
        }
    }

    @Test
    fun getAvailableCameraInfos_usesFilteredCameras() {
        ProcessCameraProvider.configureInstance(
            FakeAppConfig.create(CameraSelector.DEFAULT_BACK_CAMERA)
        )
        runBlocking {
            provider = ProcessCameraProvider.getInstance(context).await()

            val cameraInfos = provider.availableCameraInfos
            assertThat(cameraInfos.size).isEqualTo(1)

            val cameraInfo = cameraInfos.first() as FakeCameraInfoInternal
            assertThat(cameraInfo.lensFacing).isEqualTo(CameraSelector.LENS_FACING_BACK)
        }
    }

    @Test
    fun getAvailableConcurrentCameraInfos() {
        ProcessCameraProvider.configureInstance(createConcurrentCameraAppConfig())
        runBlocking {
            provider = ProcessCameraProvider.getInstance(context).await()
            assertThat(provider.availableConcurrentCameraInfos.size).isEqualTo(2)
            assertThat(provider.availableConcurrentCameraInfos[0].size).isEqualTo(2)
            assertThat(provider.availableConcurrentCameraInfos[1].size).isEqualTo(2)
        }
    }

    @Test
    fun cannotConfigureTwice() {
        ProcessCameraProvider.configureInstance(FakeAppConfig.create())
        assertThrows<IllegalStateException> {
            ProcessCameraProvider.configureInstance(FakeAppConfig.create())
        }
    }

    @Test
    fun shutdown_clearsPreviousConfiguration() {
        ProcessCameraProvider.configureInstance(FakeAppConfig.create())

        runBlocking {
            provider = ProcessCameraProvider.getInstance(context).await()
            // Clear the configuration so we can reinit
            provider.shutdown().await()
        }

        // Should not throw exception
        ProcessCameraProvider.configureInstance(FakeAppConfig.create())
    }

    @Test
    fun bindConcurrentCamera_isBound() {
        ProcessCameraProvider.configureInstance(createConcurrentCameraAppConfig())

        runBlocking(MainScope().coroutineContext) {
            provider = ProcessCameraProvider.getInstance(context).await()
            val useCase0 = Preview.Builder().setSessionOptionUnpacker { _, _ -> }.build()
            val useCase1 = Preview.Builder().setSessionOptionUnpacker { _, _ -> }.build()

            val singleCameraConfig0 = SingleCameraConfig.Builder()
                .setCameraSelector(CameraSelector.DEFAULT_BACK_CAMERA)
                .setUseCaseGroup(UseCaseGroup.Builder()
                    .addUseCase(useCase0)
                    .build())
                .setLifecycleOwner(lifecycleOwner0)
                .build()
            val singleCameraConfig1 = SingleCameraConfig.Builder()
                .setCameraSelector(CameraSelector.DEFAULT_FRONT_CAMERA)
                .setUseCaseGroup(UseCaseGroup.Builder()
                    .addUseCase(useCase1)
                    .build())
                .setLifecycleOwner(lifecycleOwner1)
                .build()

            val concurrentCameraConfig = ConcurrentCameraConfig.Builder()
                .setCameraConfigs(listOf(singleCameraConfig0, singleCameraConfig1))
                .build()

            val concurrentCamera = provider.bindToLifecycle(concurrentCameraConfig)

            assertThat(concurrentCamera).isNotNull()
            assertThat(concurrentCamera.cameras.size).isEqualTo(2)
            assertThat(provider.isBound(useCase0)).isTrue()
            assertThat(provider.isBound(useCase1)).isTrue()
            assertThat(provider.isConcurrentCameraModeOn).isTrue()
        }
    }

    @Test
    fun bindConcurrentCameraTwice_isBound() {
        ProcessCameraProvider.configureInstance(createConcurrentCameraAppConfig())

        runBlocking(MainScope().coroutineContext) {
            provider = ProcessCameraProvider.getInstance(context).await()
            val useCase0 = Preview.Builder().setSessionOptionUnpacker { _, _ -> }.build()
            val useCase1 = Preview.Builder().setSessionOptionUnpacker { _, _ -> }.build()
            val useCase2 = Preview.Builder().setSessionOptionUnpacker { _, _ -> }.build()

            val singleCameraConfig0 = SingleCameraConfig.Builder()
                .setCameraSelector(CameraSelector.DEFAULT_BACK_CAMERA)
                .setUseCaseGroup(UseCaseGroup.Builder()
                    .addUseCase(useCase0)
                    .build())
                .setLifecycleOwner(lifecycleOwner0)
                .build()
            val singleCameraConfig1 = SingleCameraConfig.Builder()
                .setCameraSelector(CameraSelector.DEFAULT_FRONT_CAMERA)
                .setUseCaseGroup(UseCaseGroup.Builder()
                    .addUseCase(useCase1)
                    .build())
                .setLifecycleOwner(lifecycleOwner1)
                .build()

            val singleCameraConfig2 = SingleCameraConfig.Builder()
                .setCameraSelector(CameraSelector.DEFAULT_FRONT_CAMERA)
                .setUseCaseGroup(UseCaseGroup.Builder()
                    .addUseCase(useCase2)
                    .build())
                .setLifecycleOwner(lifecycleOwner1)
                .build()

            val concurrentCameraConfig0 = ConcurrentCameraConfig.Builder()
                .setCameraConfigs(listOf(singleCameraConfig0, singleCameraConfig1))
                .build()

            val concurrentCamera0 = provider.bindToLifecycle(concurrentCameraConfig0)

            assertThat(concurrentCamera0).isNotNull()
            assertThat(concurrentCamera0.cameras.size).isEqualTo(2)
            assertThat(provider.isBound(useCase0)).isTrue()
            assertThat(provider.isBound(useCase1)).isTrue()
            assertThat(provider.isConcurrentCameraModeOn).isTrue()

            val concurrentCameraConfig1 = ConcurrentCameraConfig.Builder()
                .setCameraConfigs(listOf(singleCameraConfig0, singleCameraConfig2))
                .build()

            val concurrentCamera1 = provider.bindToLifecycle(concurrentCameraConfig1)

            assertThat(concurrentCamera1).isNotNull()
            assertThat(concurrentCamera1.cameras.size).isEqualTo(2)
            assertThat(provider.isBound(useCase0)).isTrue()
            assertThat(provider.isBound(useCase2)).isTrue()
            assertThat(provider.isConcurrentCameraModeOn).isTrue()
        }
    }

    @Test
    fun bindConcurrentCamera_lessThanTwoSingleCameraConfigs() {
        ProcessCameraProvider.configureInstance(createConcurrentCameraAppConfig())

        runBlocking(MainScope().coroutineContext) {
            provider = ProcessCameraProvider.getInstance(context).await()
            val useCase0 = Preview.Builder().setSessionOptionUnpacker { _, _ -> }.build()

            val singleCameraConfig0 = SingleCameraConfig.Builder()
                .setCameraSelector(CameraSelector.DEFAULT_BACK_CAMERA)
                .setUseCaseGroup(UseCaseGroup.Builder()
                    .addUseCase(useCase0)
                    .build())
                .setLifecycleOwner(lifecycleOwner0)
                .build()

            val concurrentCameraConfig = ConcurrentCameraConfig.Builder()
                .setCameraConfigs(listOf(singleCameraConfig0))
                .build()

            assertThrows<IllegalArgumentException> {
                provider.bindToLifecycle(concurrentCameraConfig)
            }
            assertThat(provider.isConcurrentCameraModeOn).isFalse()
        }
    }

    @Test
    fun bindConcurrentCamera_moreThanTwoSingleCameraConfigs() {
        ProcessCameraProvider.configureInstance(createConcurrentCameraAppConfig())

        runBlocking(MainScope().coroutineContext) {
            provider = ProcessCameraProvider.getInstance(context).await()
            val useCase0 = Preview.Builder().setSessionOptionUnpacker { _, _ -> }.build()
            val useCase1 = Preview.Builder().setSessionOptionUnpacker { _, _ -> }.build()

            val singleCameraConfig0 = SingleCameraConfig.Builder()
                .setCameraSelector(CameraSelector.DEFAULT_BACK_CAMERA)
                .setUseCaseGroup(UseCaseGroup.Builder()
                    .addUseCase(useCase0)
                    .build())
                .setLifecycleOwner(lifecycleOwner0)
                .build()

            val singleCameraConfig1 = SingleCameraConfig.Builder()
                .setCameraSelector(CameraSelector.DEFAULT_FRONT_CAMERA)
                .setUseCaseGroup(UseCaseGroup.Builder()
                    .addUseCase(useCase1)
                    .build())
                .setLifecycleOwner(lifecycleOwner1)
                .build()

            val singleCameraConfig2 = SingleCameraConfig.Builder()
                .setCameraSelector(CameraSelector.DEFAULT_FRONT_CAMERA)
                .setUseCaseGroup(UseCaseGroup.Builder()
                    .addUseCase(useCase0)
                    .build())
                .setLifecycleOwner(lifecycleOwner1)
                .build()

            val concurrentCameraConfig = ConcurrentCameraConfig.Builder()
                .setCameraConfigs(listOf(singleCameraConfig0,
                    singleCameraConfig1,
                    singleCameraConfig2))
                .build()

            assertThrows<UnsupportedOperationException> {
                provider.bindToLifecycle(concurrentCameraConfig)
            }
            assertThat(provider.isConcurrentCameraModeOn).isFalse()
        }
    }

    private fun createConcurrentCameraAppConfig(): CameraXConfig {
        val cameraCoordinator = FakeCameraCoordinator()
        val combination0 = mapOf(
            "0" to CameraSelector.Builder().requireLensFacing(LENS_FACING_BACK).build(),
            "1" to CameraSelector.Builder().requireLensFacing(LENS_FACING_FRONT).build())
        val combination1 = mapOf(
            "0" to CameraSelector.Builder().requireLensFacing(LENS_FACING_BACK).build(),
            "2" to CameraSelector.Builder().requireLensFacing(LENS_FACING_FRONT).build())

        cameraCoordinator.addConcurrentCameraIdsAndCameraSelectors(combination0)
        cameraCoordinator.addConcurrentCameraIdsAndCameraSelectors(combination1)
        val cameraFactoryProvider =
            CameraFactory.Provider { _, _, _ ->
                val cameraFactory = FakeCameraFactory()
                cameraFactory.insertCamera(
                    CameraSelector.LENS_FACING_BACK,
                    "0"
                ) {
                    FakeCamera(
                        "0", null,
                        FakeCameraInfoInternal(
                            "0", 0,
                            CameraSelector.LENS_FACING_BACK
                        )
                    )
                }
                cameraFactory.insertCamera(
                    CameraSelector.LENS_FACING_FRONT,
                    "1"
                ) {
                    FakeCamera(
                        "1", null,
                        FakeCameraInfoInternal(
                            "1", 0,
                            CameraSelector.LENS_FACING_FRONT
                        )
                    )
                }
                cameraFactory.insertCamera(
                    CameraSelector.LENS_FACING_FRONT,
                    "2"
                ) {
                    FakeCamera(
                        "2", null,
                        FakeCameraInfoInternal(
                            "2", 0,
                            CameraSelector.LENS_FACING_FRONT
                        )
                    )
                }
                cameraFactory.cameraCoordinator = cameraCoordinator
                cameraFactory
            }
        val appConfigBuilder = CameraXConfig.Builder()
            .setCameraFactoryProvider(cameraFactoryProvider)
            .setDeviceSurfaceManagerProvider { _, _, _ -> FakeCameraDeviceSurfaceManager() }
            .setUseCaseConfigFactoryProvider { FakeUseCaseConfigFactory() }

        return appConfigBuilder.build()
    }
}

private class TestAppContextWrapper(base: Context, val app: Application? = null) :
    ContextWrapper(base) {

    override fun getApplicationContext(): Context {
        return app ?: this
    }

    override fun createAttributionContext(attributionTag: String?): Context {
        return this
    }
}

@RequiresApi(21)
private class TestApplication(val pm: PackageManager) : Application(), CameraXConfig.Provider {
    private val used = atomic(false)
    val providerUsed: Boolean
        get() = used.value

    override fun getCameraXConfig(): CameraXConfig {
        used.value = true
        return FakeAppConfig.create()
    }

    override fun getPackageManager(): PackageManager {
        return pm
    }

    override fun createAttributionContext(attributionTag: String?): Context {
        return this
    }
}
