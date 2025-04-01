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

package androidx.camera.camera2.pipe.testing

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.params.SessionConfiguration.SESSION_HIGH_SPEED
import android.hardware.camera2.params.SessionConfiguration.SESSION_REGULAR
import androidx.camera.camera2.pipe.CameraGraph
import androidx.camera.camera2.pipe.CameraGraph.OperatingMode
import androidx.camera.camera2.pipe.CameraId
import androidx.camera.camera2.pipe.CameraPipe
import androidx.camera.camera2.pipe.CameraStream
import androidx.camera.camera2.pipe.RequestTemplate
import androidx.camera.camera2.pipe.integration.adapter.CameraStateAdapter
import androidx.camera.camera2.pipe.integration.adapter.SessionConfigAdapter
import androidx.camera.camera2.pipe.integration.adapter.ZslControlNoOpImpl
import androidx.camera.camera2.pipe.integration.compat.StreamConfigurationMapCompat
import androidx.camera.camera2.pipe.integration.compat.quirk.CameraQuirks
import androidx.camera.camera2.pipe.integration.compat.workaround.NoOpInactiveSurfaceCloser
import androidx.camera.camera2.pipe.integration.compat.workaround.NoOpTemplateParamsOverride
import androidx.camera.camera2.pipe.integration.compat.workaround.OutputSizesCorrector
import androidx.camera.camera2.pipe.integration.config.CameraConfig
import androidx.camera.camera2.pipe.integration.config.UseCaseCameraConfig
import androidx.camera.camera2.pipe.integration.config.UseCaseGraphConfig
import androidx.camera.camera2.pipe.integration.impl.CameraCallbackMap
import androidx.camera.camera2.pipe.integration.impl.CameraInteropStateCallbackRepository
import androidx.camera.camera2.pipe.integration.impl.CapturePipeline
import androidx.camera.camera2.pipe.integration.impl.ComboRequestListener
import androidx.camera.camera2.pipe.integration.impl.UseCaseCamera
import androidx.camera.camera2.pipe.integration.impl.UseCaseCameraRequestControl
import androidx.camera.camera2.pipe.integration.impl.UseCaseCameraRequestControlImpl
import androidx.camera.camera2.pipe.integration.impl.UseCaseCameraState
import androidx.camera.camera2.pipe.integration.impl.UseCaseManager.Companion.createCameraGraphConfig
import androidx.camera.camera2.pipe.integration.impl.UseCaseSurfaceManager
import androidx.camera.camera2.pipe.integration.impl.UseCaseThreads
import androidx.camera.camera2.pipe.integration.impl.toMap
import androidx.camera.core.ImageCapture
import androidx.camera.core.UseCase
import androidx.camera.core.imagecapture.CameraCapturePipeline
import androidx.camera.core.impl.CaptureConfig
import androidx.camera.core.impl.Config
import androidx.camera.core.impl.DeferrableSurface
import androidx.camera.testing.impl.FakeCameraCapturePipeline
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/** Open a [CameraGraph] for the desired [cameraId] and [useCases] */
class TestUseCaseCamera(
    private val context: Context,
    private val cameraId: String,
    private val threads: UseCaseThreads,
    private val useCases: List<UseCase>,
    private val cameraConfig: CameraConfig = CameraConfig(CameraId(cameraId)),
    val cameraPipe: CameraPipe = CameraPipe(CameraPipe.Config(context)),
    val useCaseSurfaceManager: UseCaseSurfaceManager =
        UseCaseSurfaceManager(
            threads,
            cameraPipe,
            NoOpInactiveSurfaceCloser,
            SessionConfigAdapter(useCases = useCases),
        ),
) : UseCaseCamera {
    val cameraMetadata =
        cameraPipe.cameras().awaitCameraMetadata(CameraId.fromCamera2Id(cameraId))!!
    val streamConfigurationMap =
        cameraMetadata[CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP]
    val cameraQuirks =
        CameraQuirks(
            cameraMetadata,
            StreamConfigurationMapCompat(
                streamConfigurationMap,
                OutputSizesCorrector(cameraMetadata, streamConfigurationMap)
            )
        )
    val sessionConfigAdapter = SessionConfigAdapter(useCases)
    val useCaseCameraGraphConfig: UseCaseGraphConfig

    init {
        val streamConfigMap = mutableMapOf<CameraStream.Config, DeferrableSurface>()
        val callbackMap = CameraCallbackMap()
        val requestListener = ComboRequestListener()
        val cameraGraphConfig =
            createCameraGraphConfig(
                sessionConfigAdapter.getValidSessionConfigOrNull()?.let { sessionConfig ->
                    when (sessionConfig.sessionType) {
                        SESSION_REGULAR -> OperatingMode.NORMAL
                        SESSION_HIGH_SPEED -> OperatingMode.HIGH_SPEED
                        else -> OperatingMode.custom(sessionConfig.sessionType)
                    }
                } ?: OperatingMode.NORMAL,
                sessionConfigAdapter,
                streamConfigMap,
                callbackMap,
                requestListener,
                cameraConfig,
                cameraQuirks,
                ZslControlNoOpImpl(),
                NoOpTemplateParamsOverride,
                cameraMetadata,
            )
        val cameraGraph = cameraPipe.create(cameraGraphConfig)

        useCaseCameraGraphConfig =
            UseCaseCameraConfig(
                    useCases,
                    sessionConfigAdapter,
                    CameraStateAdapter(),
                    cameraGraph,
                    streamConfigMap,
                    sessionProcessorManager = null
                )
                .provideUseCaseGraphConfig(
                    useCaseSurfaceManager = useCaseSurfaceManager,
                    cameraInteropStateCallbackRepository = CameraInteropStateCallbackRepository()
                )
    }

    override val requestControl: UseCaseCameraRequestControl =
        UseCaseCameraRequestControlImpl(
                capturePipeline =
                    object : CapturePipeline {
                        override var template: Int = CameraDevice.TEMPLATE_PREVIEW

                        override suspend fun submitStillCaptures(
                            configs: List<CaptureConfig>,
                            requestTemplate: RequestTemplate,
                            sessionConfigOptions: Config,
                            @ImageCapture.CaptureMode captureMode: Int,
                            @ImageCapture.FlashType flashType: Int,
                            @ImageCapture.FlashMode flashMode: Int
                        ): List<Deferred<Void?>> {
                            throw NotImplementedError("Not implemented")
                        }

                        override suspend fun getCameraCapturePipeline(
                            captureMode: Int,
                            flashMode: Int,
                            flashType: Int
                        ): CameraCapturePipeline = FakeCameraCapturePipeline()
                    },
                state =
                    UseCaseCameraState(
                        useCaseCameraGraphConfig,
                        threads,
                        sessionProcessorManager = null,
                        templateParamsOverride = NoOpTemplateParamsOverride,
                    ),
                useCaseGraphConfig = useCaseCameraGraphConfig,
                useCaseSurfaceManager = useCaseSurfaceManager,
                threads = threads,
            )
            .apply {
                SessionConfigAdapter(useCases).getValidSessionConfigOrNull()?.let { sessionConfig ->
                    setConfigAsync(
                        type = UseCaseCameraRequestControl.Type.SESSION_CONFIG,
                        config = sessionConfig.implementationOptions,
                        tags = sessionConfig.repeatingCaptureConfig.tagBundle.toMap(),
                        listeners =
                            setOf(
                                CameraCallbackMap.createFor(
                                    sessionConfig.repeatingCameraCaptureCallbacks,
                                    threads.backgroundExecutor
                                )
                            ),
                        template =
                            RequestTemplate(sessionConfig.repeatingCaptureConfig.templateType),
                        streams =
                            useCaseCameraGraphConfig.getStreamIdsFromSurfaces(
                                sessionConfig.repeatingCaptureConfig.surfaces
                            ),
                        sessionConfig = sessionConfig,
                    )
                }
            }

    override suspend fun getCameraCapturePipeline(
        captureMode: Int,
        flashMode: Int,
        flashType: Int
    ): CameraCapturePipeline = FakeCameraCapturePipeline()

    override fun close(): Job {
        return threads.scope.launch {
            useCaseCameraGraphConfig.graph.close()
            useCaseSurfaceManager.stopAsync().await()
        }
    }
}
