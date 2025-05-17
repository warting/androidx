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

package androidx.camera.camera2.pipe.integration.impl

import androidx.annotation.VisibleForTesting
import androidx.camera.camera2.pipe.CameraTimestamp
import androidx.camera.camera2.pipe.FrameInfo
import androidx.camera.camera2.pipe.FrameMetadata
import androidx.camera.camera2.pipe.FrameNumber
import androidx.camera.camera2.pipe.Request
import androidx.camera.camera2.pipe.RequestFailure
import androidx.camera.camera2.pipe.RequestMetadata
import androidx.camera.camera2.pipe.StreamId
import androidx.camera.camera2.pipe.integration.config.CameraScope
import androidx.camera.core.impl.TagBundle
import java.util.concurrent.Executor
import javax.inject.Inject

/**
 * A ComboRequestListener which contains a set of [Request.Listener]s. The primary purpose of this
 * class is to receive the capture result from the currently configured [UseCaseCamera] and
 * propagate to the registered [Request.Listener]s.
 */
@CameraScope
public class ComboRequestListener @Inject constructor() : Request.Listener {
    private val requestListeners = mutableMapOf<Request.Listener, Executor>()

    @Volatile
    public var listeners: Map<Request.Listener, Executor> = mapOf()
        @VisibleForTesting get
        private set

    public fun addListener(listener: Request.Listener, executor: Executor) {
        check(!listeners.contains(listener)) { "$listener was already registered!" }
        synchronized(requestListeners) {
            requestListeners[listener] = executor
            listeners = requestListeners.toMap()
        }
    }

    public fun removeListener(listener: Request.Listener) {
        synchronized(requestListeners) {
            requestListeners.remove(listener)
            listeners = requestListeners.toMap()
        }
    }

    override fun onAborted(request: Request) {
        listeners.forEach { (listener, executor) ->
            executor.execute { listener.onAborted(request) }
        }
    }

    override fun onBufferLost(
        requestMetadata: RequestMetadata,
        frameNumber: FrameNumber,
        stream: StreamId,
    ) {
        listeners.forEach { (listener, executor) ->
            executor.execute { listener.onBufferLost(requestMetadata, frameNumber, stream) }
        }
    }

    override fun onComplete(
        requestMetadata: RequestMetadata,
        frameNumber: FrameNumber,
        result: FrameInfo,
    ) {
        listeners.forEach { (listener, executor) ->
            executor.execute { listener.onComplete(requestMetadata, frameNumber, result) }
        }
    }

    override fun onFailed(
        requestMetadata: RequestMetadata,
        frameNumber: FrameNumber,
        requestFailure: RequestFailure,
    ) {
        listeners.forEach { (listener, executor) ->
            executor.execute { listener.onFailed(requestMetadata, frameNumber, requestFailure) }
        }
    }

    override fun onPartialCaptureResult(
        requestMetadata: RequestMetadata,
        frameNumber: FrameNumber,
        captureResult: FrameMetadata,
    ) {
        listeners.forEach { (listener, executor) ->
            executor.execute {
                listener.onPartialCaptureResult(requestMetadata, frameNumber, captureResult)
            }
        }
    }

    override fun onRequestSequenceAborted(requestMetadata: RequestMetadata) {
        listeners.forEach { (listener, executor) ->
            executor.execute { listener.onRequestSequenceAborted(requestMetadata) }
        }
    }

    override fun onRequestSequenceCompleted(
        requestMetadata: RequestMetadata,
        frameNumber: FrameNumber,
    ) {
        listeners.forEach { (listener, executor) ->
            executor.execute { listener.onRequestSequenceCompleted(requestMetadata, frameNumber) }
        }
    }

    override fun onRequestSequenceCreated(requestMetadata: RequestMetadata) {
        listeners.forEach { (listener, executor) ->
            executor.execute { listener.onRequestSequenceCreated(requestMetadata) }
        }
    }

    override fun onRequestSequenceSubmitted(requestMetadata: RequestMetadata) {
        listeners.forEach { (listener, executor) ->
            executor.execute { listener.onRequestSequenceSubmitted(requestMetadata) }
        }
    }

    override fun onStarted(
        requestMetadata: RequestMetadata,
        frameNumber: FrameNumber,
        timestamp: CameraTimestamp,
    ) {
        listeners.forEach { (listener, executor) ->
            executor.execute { listener.onStarted(requestMetadata, frameNumber, timestamp) }
        }
    }

    override fun onTotalCaptureResult(
        requestMetadata: RequestMetadata,
        frameNumber: FrameNumber,
        totalCaptureResult: FrameInfo,
    ) {
        listeners.forEach { (listener, executor) ->
            executor.execute {
                listener.onTotalCaptureResult(requestMetadata, frameNumber, totalCaptureResult)
            }
        }
    }
}

public fun RequestMetadata.containsTag(tagKey: String, tagValue: Any): Boolean =
    getOrDefault(CAMERAX_TAG_BUNDLE, TagBundle.emptyBundle()).getTag(tagKey).let {
        return it == tagValue
    }
