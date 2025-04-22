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

package androidx.camera.camera2.pipe.framegraph

import android.hardware.camera2.CaptureRequest
import androidx.annotation.GuardedBy
import androidx.camera.camera2.pipe.CameraGraph
import androidx.camera.camera2.pipe.FrameGraph.FrameBuffer
import androidx.camera.camera2.pipe.Metadata
import androidx.camera.camera2.pipe.Request
import androidx.camera.camera2.pipe.StreamId
import androidx.camera.camera2.pipe.config.CameraGraphScope
import androidx.camera.camera2.pipe.config.ForCameraGraph
import androidx.camera.camera2.pipe.core.Log
import androidx.camera.camera2.pipe.filterToCaptureRequestParameters
import androidx.camera.camera2.pipe.filterToMetadataParameters
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope

internal class FrameBufferImpl(
    override val streams: Set<StreamId>,
    override val parameters: Map<Any, Any?>,
    private val frameBuffers: FrameBuffers,
) : FrameBuffer {
    override fun close() {
        frameBuffers.detach(this)
    }
}

@CameraGraphScope
internal class FrameBuffers
@Inject
internal constructor(
    private val cameraGraph: CameraGraph,
    @ForCameraGraph private val graphScope: CoroutineScope
) {
    private val lock = Any()
    @GuardedBy("lock") private val buffers = mutableListOf<FrameBuffer>()
    @GuardedBy("lock") private var streams = mutableSetOf<StreamId>()
    @GuardedBy("lock") private var parameters = mutableMapOf<Any, Any>()

    fun attach(frameBuffer: FrameBuffer) {
        val modified =
            synchronized(lock) {
                buffers.add(frameBuffer)
                updateStreamsAndParameters()
            }
        if (modified) {
            invalidate()
        }
    }

    fun detach(frameBuffer: FrameBuffer) {
        val modified =
            synchronized(lock) {
                buffers.remove(frameBuffer)
                updateStreamsAndParameters()
            }
        if (modified) {
            invalidate()
        }
    }

    @GuardedBy("lock")
    private fun updateStreamsAndParameters(): Boolean {
        val newStreams = mutableSetOf<StreamId>()
        val newParameters = mutableMapOf<Any, Any>()
        var modified: Boolean
        synchronized(lock) {
            for (buffer in buffers) {
                newStreams.addAll(buffer.streams)

                for (parameter in buffer.parameters) {
                    val key = parameter.key
                    val value = parameter.value
                    check(key is CaptureRequest.Key<*> || key is Metadata.Key<*>) {
                        "Invalid type for ${parameter.key}"
                    }
                    if (newParameters.containsKey(key) && newParameters[key] != value) {
                        throw IllegalStateException(
                            "Conflicting parameter values, $key and ${parameters[key]} have different values."
                        )
                    } else if (value == null) {
                        continue
                    } else {
                        newParameters.put(key, value)
                    }
                }
            }
            modified = newStreams != streams || newParameters != parameters
            streams = newStreams
            parameters = newParameters
        }
        return modified
    }

    private fun invalidate() {
        if (buffers.isEmpty()) {
            Log.warn { "No available buffer, invoke stop repeating." }
            cameraGraph.useSessionIn(graphScope) { session -> session.stopRepeating() }
        } else {
            cameraGraph.useSessionIn(graphScope) { session ->
                session.startRepeating(
                    Request(
                        streams = streams.toList(),
                        parameters = parameters.filterToCaptureRequestParameters(),
                        extras = parameters.filterToMetadataParameters()
                    )
                )
            }
        }
    }
}
