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

import android.view.Surface
import androidx.camera.camera2.pipe.CameraContext
import androidx.camera.camera2.pipe.CameraController
import androidx.camera.camera2.pipe.CameraGraph
import androidx.camera.camera2.pipe.CameraGraphId
import androidx.camera.camera2.pipe.CameraId
import androidx.camera.camera2.pipe.GraphState.GraphStateError
import androidx.camera.camera2.pipe.StreamGraph
import androidx.camera.camera2.pipe.StreamId
import androidx.camera.camera2.pipe.graph.GraphListener
import androidx.camera.camera2.pipe.graph.GraphRequestProcessor

/**
 * The CameraControllerSimulator is a [CameraController] implementation designed to simulate actions
 * and reactions to an underlying camera.
 *
 * Most interactions with a [CameraController] are in the form of "action -> reaction" where the
 * reaction must be simulated, and is useful for testing nuanced behavior with how a real camera
 * controller may behave in different circumstances or edge cases. As an example, invoking [start]
 * must be paired with [simulateCameraStarted] in most situations for a [CameraGraph] to be able to
 * actively submit requests. This mirrors the underlying behavior of an actual Camera, which may
 * take time to configure and become ready.
 */
public class CameraControllerSimulator(
    cameraContext: CameraContext,
    private val graphId: CameraGraphId,
    private val graphConfig: CameraGraph.Config,
    private val graphListener: GraphListener,
) : CameraController {
    override val cameraId: CameraId
        get() = graphConfig.camera

    override val cameraGraphId: CameraGraphId
        get() = graphId

    override var isForeground: Boolean = true

    private val lock = Any()
    private var currentSurfaceMap: Map<StreamId, Surface> = emptyMap()
    private var currentGraphRequestProcessor: GraphRequestProcessor? = null

    private var _closed = false
    public var closed: Boolean
        get() = _closed
        private set(value) {
            _closed = value
        }

    private var _started = false
    public var started: Boolean
        get() = _started
        private set(value) {
            _started = value
        }

    public var currentCaptureSequenceProcessor: FakeCaptureSequenceProcessor? = null
        private set

    public var outputLatencySet: StreamGraph.OutputLatency? = null
        private set

    public var streamGraph: StreamGraph? = null

    public val simulatedCaptureLatency: Long = 5L
    public val simulatedProcessingLatency: Long = 10L

    init {
        check(cameraContext.cameraBackends.allIds.isNotEmpty()) {
            "Backends provided by cameraContext.cameraBackends cannot be empty"
        }
        val cameraBackendId = graphConfig.cameraBackendId
        if (cameraBackendId != null) {
            check(cameraContext.cameraBackends.allIds.contains(cameraBackendId)) {
                "Backends provided by cameraContext do not contain $cameraBackendId which was " +
                    "requested by $graphConfig"
            }
        }
    }

    public fun simulateCameraStarted() {
        synchronized(lock) {
            check(!closed) {
                "Attempted to invoke simulateStarted after the CameraController was closed."
            }

            val captureSequenceProcessor =
                FakeCaptureSequenceProcessor(graphConfig.camera, graphConfig.defaultTemplate)
            val graphRequestProcessor = GraphRequestProcessor.from(captureSequenceProcessor)
            captureSequenceProcessor.surfaceMap = currentSurfaceMap
            currentCaptureSequenceProcessor = captureSequenceProcessor
            currentGraphRequestProcessor = graphRequestProcessor

            graphListener.onGraphStarted(graphRequestProcessor)
        }
    }

    public fun simulateCameraStopped() {
        synchronized(lock) {
            check(!closed) {
                "Attempted to invoke simulateCameraStopped after the CameraController was closed."
            }
            val captureSequenceProcessor = currentCaptureSequenceProcessor
            val graphRequestProcessor = currentGraphRequestProcessor

            currentCaptureSequenceProcessor = null
            currentGraphRequestProcessor = null

            if (captureSequenceProcessor != null && graphRequestProcessor != null) {
                graphListener.onGraphStopped(graphRequestProcessor)
            }
        }
    }

    public fun simulateCameraModified() {
        synchronized(lock) {
            val captureSequenceProcessor = currentCaptureSequenceProcessor
            val graphRequestProcessor = currentGraphRequestProcessor

            currentCaptureSequenceProcessor = null
            currentGraphRequestProcessor = null

            if (captureSequenceProcessor != null && graphRequestProcessor != null) {
                graphListener.onGraphStopped(graphRequestProcessor)
            }
        }
    }

    public fun simulateCameraError(graphStateError: GraphStateError) {
        synchronized(lock) {
            check(!closed) {
                "Attempted to invoke simulateCameraError after the CameraController was closed."
            }
            graphListener.onGraphError(graphStateError)
        }
    }

    public fun simulateOutputLatency() {
        outputLatencySet =
            StreamGraph.OutputLatency(simulatedCaptureLatency, simulatedProcessingLatency)
    }

    override fun start() {
        synchronized(lock) {
            check(!closed) { "Attempted to invoke start after close." }
            started = true
        }
    }

    override fun stop() {
        synchronized(lock) {
            check(!closed) { "Attempted to invoke stop after close." }
            started = false
        }
    }

    override fun close() {
        synchronized(lock) {
            closed = true
            started = false
        }
    }

    override fun updateSurfaceMap(surfaceMap: Map<StreamId, Surface>) {
        streamGraph?.streamIds?.containsAll(surfaceMap.keys).let { check(it == true) }

        synchronized(lock) {
            currentSurfaceMap = surfaceMap

            val captureSequenceProcessor = currentCaptureSequenceProcessor
            val graphRequestProcessor = currentGraphRequestProcessor
            if (captureSequenceProcessor != null && graphRequestProcessor != null) {
                captureSequenceProcessor.surfaceMap = surfaceMap
                graphListener.onGraphModified(graphRequestProcessor)
            }
        }
    }

    override fun getOutputLatency(streamId: StreamId?): StreamGraph.OutputLatency? {
        return outputLatencySet
    }
}
