/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.camera.camera2.pipe.integration.impl

import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraExtensionSession
import android.hardware.camera2.CaptureFailure
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.CaptureResult
import android.hardware.camera2.TotalCaptureResult
import android.os.Build
import android.view.Surface
import androidx.camera.camera2.pipe.CameraTimestamp
import androidx.camera.camera2.pipe.FrameInfo
import androidx.camera.camera2.pipe.FrameMetadata
import androidx.camera.camera2.pipe.FrameNumber
import androidx.camera.camera2.pipe.Request
import androidx.camera.camera2.pipe.RequestFailure
import androidx.camera.camera2.pipe.RequestMetadata
import androidx.camera.camera2.pipe.SensorTimestamp
import androidx.camera.camera2.pipe.StreamId
import androidx.camera.camera2.pipe.integration.adapter.CameraUseCaseAdapter
import androidx.camera.camera2.pipe.integration.adapter.CaptureResultAdapter
import androidx.camera.camera2.pipe.integration.compat.Api24Compat
import androidx.camera.camera2.pipe.integration.compat.Api34Compat
import androidx.camera.camera2.pipe.integration.config.CameraScope
import androidx.camera.core.impl.CameraCaptureCallback
import androidx.camera.core.impl.CameraCaptureFailure
import androidx.camera.core.impl.CaptureConfig
import androidx.camera.core.impl.TagBundle
import java.util.concurrent.Executor
import javax.inject.Inject

/** A map of [CameraCaptureCallback] that are invoked on each [Request]. */
@CameraScope
public class CameraCallbackMap @Inject constructor() : Request.Listener {
    private val callbackMap = mutableMapOf<CameraCaptureCallback, Executor>()
    private val rejectOperationCameraCaptureSession: CameraCaptureSession by lazy {
        RejectOperationCameraCaptureSession()
    }

    @Volatile private var callbacks: Map<CameraCaptureCallback, Executor> = mapOf()

    public fun addCaptureCallback(callback: CameraCaptureCallback, executor: Executor) {
        check(!callbacks.contains(callback)) { "$callback was already registered!" }

        synchronized(callbackMap) {
            callbackMap[callback] = executor
            callbacks = callbackMap.toMap()
        }
    }

    public fun removeCaptureCallback(callback: CameraCaptureCallback) {
        synchronized(callbackMap) {
            callbackMap.remove(callback)
            callbacks = callbackMap.toMap()
        }
    }

    override fun onBufferLost(
        requestMetadata: RequestMetadata,
        frameNumber: FrameNumber,
        stream: StreamId
    ) {
        for ((callback, executor) in callbacks) {
            if (
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.N &&
                    callback is CameraUseCaseAdapter.CaptureCallbackContainer
            ) {
                val session: CameraCaptureSession? =
                    requestMetadata.unwrapAs(CameraCaptureSession::class)
                val request: CaptureRequest? = requestMetadata.unwrapAs(CaptureRequest::class)
                val surface: Surface? = requestMetadata.streams[stream]
                if (session != null && request != null && surface != null) {
                    executor.execute {
                        Api24Compat.onCaptureBufferLost(
                            callback.captureCallback,
                            session,
                            request,
                            surface,
                            frameNumber.value
                        )
                    }
                }
            }
        }
    }

    override fun onComplete(
        requestMetadata: RequestMetadata,
        frameNumber: FrameNumber,
        result: FrameInfo
    ) {
        for ((callback, executor) in callbacks) {
            if (callback is CameraUseCaseAdapter.CaptureCallbackContainer) {
                val session: CameraCaptureSession? = getCameraCaptureSession(requestMetadata)
                val request: CaptureRequest? = requestMetadata.unwrapAs(CaptureRequest::class)
                val totalCaptureResult: TotalCaptureResult? =
                    result.unwrapAs(TotalCaptureResult::class)
                if (session != null && request != null && totalCaptureResult != null) {
                    executor.execute {
                        callback.captureCallback.onCaptureCompleted(
                            session,
                            request,
                            totalCaptureResult
                        )
                    }
                }
            } else {
                val captureResult = CaptureResultAdapter(requestMetadata, frameNumber, result)
                executor.execute {
                    callback.onCaptureCompleted(requestMetadata.getCaptureConfigId(), captureResult)
                }
            }
        }
    }

    private fun RequestMetadata.getCaptureConfigId(): Int {
        val tagBundle = this[CAMERAX_TAG_BUNDLE]
        return tagBundle?.getTag(CaptureConfig.CAPTURE_CONFIG_ID_TAG_KEY) as? Int
            ?: CaptureConfig.DEFAULT_ID
    }

    override fun onFailed(
        requestMetadata: RequestMetadata,
        frameNumber: FrameNumber,
        requestFailure: RequestFailure
    ) {
        for ((callback, executor) in callbacks) {
            if (callback is CameraUseCaseAdapter.CaptureCallbackContainer) {
                val session: CameraCaptureSession? = getCameraCaptureSession(requestMetadata)
                val request: CaptureRequest? = requestMetadata.unwrapAs(CaptureRequest::class)
                val captureFailure = requestFailure.unwrapAs(CaptureFailure::class)
                if (session != null && request != null && captureFailure != null) {
                    executor.execute {
                        callback.captureCallback.onCaptureFailed(session, request, captureFailure)
                    }
                }
            } else {
                val failure = CameraCaptureFailure(CameraCaptureFailure.Reason.ERROR)
                executor.execute {
                    callback.onCaptureFailed(requestMetadata.getCaptureConfigId(), failure)
                }
            }
        }
    }

    override fun onAborted(request: Request) {
        for ((callback, executor) in callbacks) {
            // TODO: get the correct requestId
            val tagBundle = request.extras[CAMERAX_TAG_BUNDLE] as? TagBundle
            val captureConfigId =
                tagBundle?.getTag(CaptureConfig.CAPTURE_CONFIG_ID_TAG_KEY) as? Int
                    ?: CaptureConfig.DEFAULT_ID
            executor.execute { callback.onCaptureCancelled(captureConfigId) }
        }
    }

    override fun onPartialCaptureResult(
        requestMetadata: RequestMetadata,
        frameNumber: FrameNumber,
        captureResult: FrameMetadata
    ) {
        for ((callback, executor) in callbacks) {
            if (callback is CameraUseCaseAdapter.CaptureCallbackContainer) {
                val session: CameraCaptureSession? =
                    requestMetadata.unwrapAs(CameraCaptureSession::class)
                val request: CaptureRequest? = requestMetadata.unwrapAs(CaptureRequest::class)
                val partialResult: CaptureResult? = captureResult.unwrapAs(CaptureResult::class)
                if (session != null && request != null && partialResult != null) {
                    executor.execute {
                        callback.captureCallback.onCaptureProgressed(
                            session,
                            request,
                            partialResult
                        )
                    }
                }
            }
        }
    }

    override fun onRequestSequenceAborted(requestMetadata: RequestMetadata) {
        for ((callback, executor) in callbacks) {
            if (callback is CameraUseCaseAdapter.CaptureCallbackContainer) {
                val session: CameraCaptureSession? =
                    requestMetadata.unwrapAs(CameraCaptureSession::class)
                val request: CaptureRequest? = requestMetadata.unwrapAs(CaptureRequest::class)
                if (session != null && request != null) {
                    executor.execute {
                        callback.captureCallback.onCaptureSequenceAborted(
                            session,
                            -1 /*sequenceId*/
                        )
                    }
                }
            } else {
                executor.execute {
                    callback.onCaptureCancelled(requestMetadata.getCaptureConfigId())
                }
            }
        }
    }

    override fun onRequestSequenceCompleted(
        requestMetadata: RequestMetadata,
        frameNumber: FrameNumber
    ) {
        for ((callback, executor) in callbacks) {
            if (callback is CameraUseCaseAdapter.CaptureCallbackContainer) {
                val session: CameraCaptureSession? = getCameraCaptureSession(requestMetadata)
                val request: CaptureRequest? = requestMetadata.unwrapAs(CaptureRequest::class)
                if (session != null && request != null) {
                    executor.execute {
                        callback.captureCallback.onCaptureSequenceCompleted(
                            session,
                            -1 /*sequenceId*/,
                            frameNumber.value
                        )
                    }
                }
            }
        }
    }

    override fun onStarted(
        requestMetadata: RequestMetadata,
        frameNumber: FrameNumber,
        timestamp: CameraTimestamp
    ) {
        for ((callback, executor) in callbacks) {
            if (callback is CameraUseCaseAdapter.CaptureCallbackContainer) {
                val session: CameraCaptureSession? = getCameraCaptureSession(requestMetadata)
                val request: CaptureRequest? = requestMetadata.unwrapAs(CaptureRequest::class)
                if (session != null && request != null) {
                    executor.execute {
                        callback.captureCallback.onCaptureStarted(
                            session,
                            request,
                            timestamp.value,
                            frameNumber.value
                        )
                    }
                }
            } else {
                executor.execute { callback.onCaptureStarted(requestMetadata.getCaptureConfigId()) }
            }
        }
    }

    override fun onCaptureProgress(requestMetadata: RequestMetadata, progress: Int) {
        for ((callback, executor) in callbacks) {
            if (callback is CameraUseCaseAdapter.CaptureCallbackContainer) {
                val session: CameraCaptureSession? =
                    requestMetadata.unwrapAs(CameraCaptureSession::class)
                val request: CaptureRequest? = requestMetadata.unwrapAs(CaptureRequest::class)
                val partialResult: CaptureResult? = requestMetadata.unwrapAs(CaptureResult::class)
                if (session != null && request != null && partialResult != null) {
                    executor.execute {
                        callback.captureCallback.onCaptureProgressed(
                            session,
                            request,
                            partialResult
                        )
                    }
                }
            } else {
                executor.execute {
                    callback.onCaptureProcessProgressed(
                        requestMetadata.getCaptureConfigId(),
                        progress
                    )
                }
            }
        }
    }

    override fun onReadoutStarted(
        requestMetadata: RequestMetadata,
        frameNumber: FrameNumber,
        timestamp: SensorTimestamp
    ) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            return
        }
        for ((callback, executor) in callbacks) {
            if (callback is CameraUseCaseAdapter.CaptureCallbackContainer) {
                val session: CameraCaptureSession? =
                    requestMetadata.unwrapAs(CameraCaptureSession::class)
                val request: CaptureRequest? = requestMetadata.unwrapAs(CaptureRequest::class)
                if (session != null && request != null) {
                    executor.execute {
                        Api34Compat.onReadoutStarted(
                            callback.captureCallback,
                            session,
                            request,
                            timestamp.value,
                            frameNumber.value
                        )
                    }
                }
            }
        }
    }

    private fun getCameraCaptureSession(requestMetadata: RequestMetadata): CameraCaptureSession? =
        requestMetadata.unwrapAs(CameraCaptureSession::class)
            // Also try the CameraExtensionSession for callback when API level is 31 or above
            ?: if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                requestMetadata.unwrapAs(CameraExtensionSession::class)?.let {
                    rejectOperationCameraCaptureSession
                }
            } else {
                null
            }

    public companion object {
        public fun createFor(
            callbacks: Collection<CameraCaptureCallback>,
            executor: Executor
        ): CameraCallbackMap {
            return CameraCallbackMap().apply {
                callbacks.forEach { callback -> addCaptureCallback(callback, executor) }
            }
        }
    }
}
