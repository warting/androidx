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

package androidx.camera.camera2.pipe.testing

import androidx.camera.camera2.pipe.GraphState
import androidx.camera.camera2.pipe.GraphState.GraphStateError
import androidx.camera.camera2.pipe.GraphState.GraphStateStarted
import androidx.camera.camera2.pipe.GraphState.GraphStateStarting
import androidx.camera.camera2.pipe.GraphState.GraphStateStopped
import androidx.camera.camera2.pipe.GraphState.GraphStateStopping
import androidx.camera.camera2.pipe.Request
import androidx.camera.camera2.pipe.graph.GraphListener
import androidx.camera.camera2.pipe.graph.GraphProcessor
import androidx.camera.camera2.pipe.graph.GraphRequestProcessor
import androidx.camera.camera2.pipe.graph.GraphState3A
import androidx.camera.camera2.pipe.graph.Listener3A
import androidx.camera.camera2.pipe.putAllMetadata
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.runBlocking

/** Fake implementation of a [GraphProcessor] for tests. */
internal class FakeGraphProcessor(
    val graphState3A: GraphState3A = GraphState3A(),
    val graphListener3A: Listener3A = Listener3A(),
    val defaultParameters: Map<*, Any?> = emptyMap<Any, Any?>(),
    val defaultListeners: List<Request.Listener> = emptyList()
) : GraphProcessor, GraphListener {
    var active = true
        private set

    var closed = false
        private set

    private var _repeatingRequest: Request? = null
    override var repeatingRequest: Request?
        get() = _repeatingRequest
        set(value) {
            _repeatingRequest = value
            if (value == null) {
                graphListener3A.onStopRepeating()
            }
        }

    val requestQueue: List<List<Request>>
        get() = _requestQueue

    private val _requestQueue = mutableListOf<List<Request>>()
    private var processor: GraphRequestProcessor? = null

    private val _graphState = MutableStateFlow<GraphState>(GraphStateStopped)

    override val graphState: StateFlow<GraphState>
        get() = _graphState

    override fun submit(request: Request): Boolean = submit(listOf(request))

    override fun submit(requests: List<Request>): Boolean {
        if (closed) return false
        _requestQueue.add(requests)
        return true
    }

    override fun submit(parameters: Map<*, Any?>): Boolean {
        check(repeatingRequest != null)
        if (closed) return false

        val currProcessor = processor
        val currRepeatingRequest = repeatingRequest
        val requiredParameters = mutableMapOf<Any, Any?>()
        requiredParameters.putAllMetadata(parameters)
        graphState3A.writeTo(requiredParameters)

        if (currProcessor != null && currRepeatingRequest != null) {
            currProcessor.submit(
                isRepeating = false,
                requests = listOf(currRepeatingRequest),
                defaultParameters = defaultParameters,
                graphParameters = mapOf<Any, Any?>(),
                requiredParameters = requiredParameters,
                listeners = defaultListeners
            )
        }
        return true
    }

    override fun abort() {
        val requests = _requestQueue.toList()
        _requestQueue.clear()

        for (burst in requests) {
            for (request in burst) {
                for (listener in defaultListeners) {
                    listener.onAborted(request)
                }
            }
        }

        for (burst in requests) {
            for (request in burst) {
                for (listener in request.listeners) {
                    listener.onAborted(request)
                }
            }
        }
    }

    override fun close() {
        if (closed) {
            return
        }
        closed = true
        active = false
        _requestQueue.clear()
        graphListener3A.onGraphShutdown()
    }

    override fun onGraphStarting() {
        _graphState.value = GraphStateStarting
    }

    override fun onGraphStarted(requestProcessor: GraphRequestProcessor) {
        _graphState.value = GraphStateStarted
        val old = processor
        processor = requestProcessor
        runBlocking { old?.shutdown() }
    }

    override fun onGraphStopping() {
        _graphState.value = GraphStateStopping
        graphListener3A.onGraphStopped()
    }

    override fun onGraphStopped(requestProcessor: GraphRequestProcessor?) {
        _graphState.value = GraphStateStopped
        if (requestProcessor == null) return
        val old = processor
        if (requestProcessor === old) {
            processor = null
            runBlocking { old.shutdown() }
        }
    }

    override fun onGraphModified(requestProcessor: GraphRequestProcessor) {
        invalidate()
    }

    override fun onGraphError(graphStateError: GraphStateError) {
        _graphState.update { graphState ->
            if (graphState is GraphStateStopping || graphState is GraphStateStopped) {
                GraphStateStopped
            } else {
                graphStateError
            }
        }
    }

    override fun invalidate() {
        if (closed) {
            return
        }

        val currProcessor = processor
        val currRepeatingRequest = repeatingRequest
        val requiredParameters = graphState3A.readState()

        if (currProcessor == null || currRepeatingRequest == null) {
            return
        }

        currProcessor.submit(
            isRepeating = true,
            requests = listOf(currRepeatingRequest),
            defaultParameters = defaultParameters,
            graphParameters = mapOf<Any, Any?>(),
            requiredParameters = requiredParameters,
            listeners = defaultListeners
        )
    }
}
