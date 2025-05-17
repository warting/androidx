/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.camera.camera2.pipe.graph

import android.hardware.camera2.CaptureRequest
import android.os.Build
import androidx.camera.camera2.pipe.CameraGraphId
import androidx.camera.camera2.pipe.Request
import androidx.camera.camera2.pipe.Result3A
import androidx.camera.camera2.pipe.StreamId
import androidx.camera.camera2.pipe.testing.FakeCameraMetadata
import androidx.camera.camera2.pipe.testing.FakeCaptureSequenceProcessor
import androidx.camera.camera2.pipe.testing.FakeCaptureSequenceProcessor.Companion.defaultParameters
import androidx.camera.camera2.pipe.testing.FakeCaptureSequenceProcessor.Companion.graphParameters
import androidx.camera.camera2.pipe.testing.FakeCaptureSequenceProcessor.Companion.isAbort
import androidx.camera.camera2.pipe.testing.FakeCaptureSequenceProcessor.Companion.isCapture
import androidx.camera.camera2.pipe.testing.FakeCaptureSequenceProcessor.Companion.isClose
import androidx.camera.camera2.pipe.testing.FakeCaptureSequenceProcessor.Companion.isRejected
import androidx.camera.camera2.pipe.testing.FakeCaptureSequenceProcessor.Companion.isRepeating
import androidx.camera.camera2.pipe.testing.FakeCaptureSequenceProcessor.Companion.isStopRepeating
import androidx.camera.camera2.pipe.testing.FakeCaptureSequenceProcessor.Companion.requests
import androidx.camera.camera2.pipe.testing.FakeCaptureSequenceProcessor.Companion.requiredParameters
import androidx.camera.camera2.pipe.testing.FakeMetadata.Companion.TEST_KEY
import androidx.camera.camera2.pipe.testing.FakeSurfaces
import androidx.testutils.assertThrows
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(JUnit4::class)
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
class GraphLoopTest {
    private val testScope = TestScope()
    private val testDispatcher = StandardTestDispatcher(testScope.testScheduler)
    private val shutdownScope = CoroutineScope(testDispatcher)

    private val listener3A = Listener3A()
    private val defaultParameters = emptyMap<Any, Any?>()
    private val requiredParameters = emptyMap<Any, Any?>()
    private val mockListener: Request.Listener = mock<Request.Listener>()

    private val fakeCameraMetadata = FakeCameraMetadata()
    private val fakeCameraId = fakeCameraMetadata.camera
    private val stream1 = StreamId(1)
    private val stream2 = StreamId(2)
    private val fakeSurfaces = FakeSurfaces()
    private val surfaceMap =
        mapOf(
            stream1 to fakeSurfaces.createFakeSurface(),
            stream2 to fakeSurfaces.createFakeSurface(),
        )

    private val csp1 =
        FakeCaptureSequenceProcessor(fakeCameraId).also { it.surfaceMap = surfaceMap }
    private val csp2 =
        FakeCaptureSequenceProcessor(fakeCameraId).also { it.surfaceMap = surfaceMap }

    private val grp1 = GraphRequestProcessor.from(csp1)
    private val grp2 = GraphRequestProcessor.from(csp2)

    private val request1 = Request(streams = listOf(stream1))
    private val request2 = Request(streams = listOf(stream2))
    private val request3 = Request(streams = listOf(stream1, stream2))
    private val cameraGraphId = CameraGraphId.nextId()

    private val graphLoop =
        GraphLoop(
            cameraGraphId = cameraGraphId,
            defaultParameters = defaultParameters,
            requiredParameters = requiredParameters,
            graphListeners = listOf(mockListener),
            listeners = listOf(listener3A),
            shutdownScope = shutdownScope,
            dispatcher = testDispatcher,
        )

    @After
    fun teardown() {
        fakeSurfaces.close()
        shutdownScope.cancel()
    }

    @Test
    fun graphLoopSubmitsRequests() =
        testScope.runTest {
            graphLoop.submit(listOf(request1))
            graphLoop.submit(listOf(request2))
            graphLoop.requestProcessor = grp1
            assertThat(csp1.events).isEmpty()

            advanceUntilIdle()

            assertThat(csp1.events.size).isEqualTo(2)
            assertThat(csp1.events[0].isCapture).isTrue()
            assertThat(csp1.events[0].requests).containsExactly(request1)
            assertThat(csp1.events[1].isCapture).isTrue()
            assertThat(csp1.events[1].requests).containsExactly(request2)
        }

    @Test
    fun abortRemovesPendingRequests() =
        testScope.runTest {
            graphLoop.requestProcessor = grp1
            graphLoop.submit(listOf(request1))
            graphLoop.abort()
            assertThat(csp1.events).isEmpty()

            advanceUntilIdle()
            assertThat(csp1.events).isEmpty()
        }

    @Test
    fun abortRemovesPendingRequestsAndInvokesAbort() =
        testScope.runTest {
            graphLoop.requestProcessor = grp1
            advanceUntilIdle()

            graphLoop.submit(listOf(request1))
            graphLoop.abort()
            advanceUntilIdle()

            assertThat(csp1.events.size).isEqualTo(1)
            assertThat(csp1.events[0].isAbort).isTrue()
        }

    @Test
    fun abortRemovesStartRepeating() =
        testScope.runTest {
            graphLoop.requestProcessor = grp1
            advanceUntilIdle()

            graphLoop.repeatingRequest = request1
            graphLoop.repeatingRequest = null // StopRepeating
            graphLoop.abort()
            assertThat(csp1.events).isEmpty()

            advanceUntilIdle()

            assertThat(csp1.events.size).isEqualTo(1)
            assertThat(csp1.events[0].isAbort).isTrue()
            verify(mockListener, never()).onAborted(request1)
        }

    @Test
    fun abortBeforeRequestProcessorDoesNotInvokeAbortOnRequestProcessor() =
        testScope.runTest {
            graphLoop.submit(listOf(request1))
            graphLoop.abort()
            graphLoop.requestProcessor = grp1
            advanceUntilIdle()

            assertThat(csp1.events).isEmpty()

            graphLoop.submit(listOf(request2))
            advanceUntilIdle()

            assertThat(csp1.events.size).isEqualTo(1)
            assertThat(csp1.events[0].requests).containsExactly(request2)
        }

    @Test
    fun repeatingRequestsCanBeSkipped() =
        testScope.runTest {
            graphLoop.repeatingRequest = request1
            graphLoop.repeatingRequest = request2
            graphLoop.requestProcessor = grp1
            assertThat(csp1.events).isEmpty()
            advanceUntilIdle()
            assertThat(csp1.events.size).isEqualTo(1)

            graphLoop.repeatingRequest = request3
            advanceUntilIdle()

            assertThat(csp1.events.size).isEqualTo(2)
            assertThat(csp1.events[0].isRepeating).isTrue()
            assertThat(csp1.events[0].requests).containsExactly(request2)

            assertThat(csp1.events[1].isRepeating).isTrue()
            assertThat(csp1.events[1].requests).containsExactly(request3)
        }

    @Test
    fun nullRequestProcessorHaltsProcessing() =
        testScope.runTest {
            graphLoop.requestProcessor = grp1
            graphLoop.repeatingRequest = request1
            advanceUntilIdle()

            graphLoop.requestProcessor = null
            advanceUntilIdle()

            assertThat(csp1.events.size).isEqualTo(2)
            assertThat(csp1.events[0].isRepeating).isTrue()
            assertThat(csp1.events[0].requests).containsExactly(request1)
            assertThat(csp1.events[1].isClose).isTrue()
        }

    @Test
    fun nullRepeatingRequestInvokesStopRepeating() =
        testScope.runTest {
            graphLoop.requestProcessor = grp1
            graphLoop.repeatingRequest = request1
            advanceUntilIdle()

            // Set to null and toggle to ensure only one stopRepeating event is issued.
            graphLoop.repeatingRequest = null
            graphLoop.repeatingRequest = request2
            graphLoop.repeatingRequest = null
            advanceUntilIdle()

            assertThat(csp1.events.size).isEqualTo(2)
            assertThat(csp1.events[0].isRepeating).isTrue()
            assertThat(csp1.events[0].requests).containsExactly(request1)
            assertThat(csp1.events[1].isStopRepeating).isTrue()
        }

    @Test
    fun repeatingAfterStopRepeatingDoesNotSkipStopRepeating() =
        testScope.runTest {
            graphLoop.requestProcessor = grp1
            graphLoop.repeatingRequest = request1
            advanceUntilIdle()

            // Set to null and toggle to ensure only one stopRepeating event is issued.
            graphLoop.repeatingRequest = null
            graphLoop.repeatingRequest = request2
            graphLoop.repeatingRequest = null
            graphLoop.repeatingRequest = request2
            advanceUntilIdle()

            assertThat(csp1.events.size).isEqualTo(3)
            assertThat(csp1.events[0].isRepeating).isTrue()
            assertThat(csp1.events[0].requests).containsExactly(request1)

            assertThat(csp1.events[1].isStopRepeating).isTrue()

            assertThat(csp1.events[2].isRepeating).isTrue()
            assertThat(csp1.events[2].requests).containsExactly(request2)
        }

    @Test
    fun changingRequestProcessorsReIssuesRepeatingRequest() =
        testScope.runTest {
            graphLoop.requestProcessor = grp1
            graphLoop.repeatingRequest = request1
            advanceUntilIdle()

            graphLoop.requestProcessor = grp2
            advanceUntilIdle()

            assertThat(csp2.events.size).isEqualTo(1)
            assertThat(csp2.events[0].isRepeating).isTrue()
            assertThat(csp2.events[0].requests).containsExactly(request1)
        }

    @Test
    fun changingRequestProcessorsReIssuesCaptureRequests() =
        testScope.runTest {
            graphLoop.requestProcessor = grp1
            csp1.shutdown() // reject incoming requests
            graphLoop.submit(listOf(request1))
            graphLoop.submit(listOf(request2))
            advanceUntilIdle()

            graphLoop.requestProcessor = grp2
            advanceUntilIdle()

            assertThat(csp2.events.size).isEqualTo(2)
            assertThat(csp2.events[0].isCapture).isTrue()
            assertThat(csp2.events[0].requests).containsExactly(request1)
            assertThat(csp2.events[1].isCapture).isTrue()
            assertThat(csp2.events[1].requests).containsExactly(request2)
        }

    @Test
    fun capturesThatFailCanBeRetried() =
        testScope.runTest {
            graphLoop.requestProcessor = grp1
            csp1.shutdown() // reject incoming requests
            graphLoop.repeatingRequest = request1
            advanceUntilIdle()

            graphLoop.requestProcessor = grp2
            advanceUntilIdle()

            assertThat(csp2.events.size).isEqualTo(1)
            assertThat(csp2.events[0].isRepeating).isTrue()
            assertThat(csp2.events[0].requests).containsExactly(request1)
        }

    @Test
    fun closingGraphLoopAbortsPendingRequests() =
        testScope.runTest {
            graphLoop.submit(listOf(request1))
            graphLoop.submit(listOf(request2))
            graphLoop.close()

            // Ensure close does not synchronously cause shutdown to fire.
            verify(mockListener, never()).onAborted(request1)
            verify(mockListener, never()).onAborted(request2)

            advanceUntilIdle()

            // Ensure listeners have been invoked.
            verify(mockListener).onAborted(request1)
            verify(mockListener).onAborted(request2)
        }

    @Test
    fun mixedUpdatesPrioritizeRepeatingRequests() =
        testScope.runTest {
            graphLoop.submit(listOf(request1))
            graphLoop.repeatingRequest = request2
            graphLoop.requestProcessor = grp1
            advanceUntilIdle()

            assertThat(csp1.events.size).isEqualTo(2)
            assertThat(csp1.events[0].isCapture).isTrue()
            assertThat(csp1.events[0].requests).containsExactly(request1)
            assertThat(csp1.events[1].isRepeating).isTrue()
            assertThat(csp1.events[1].requests).containsExactly(request2)
        }

    @Test
    fun submitParametersUsesLatestRepeatingRequest() =
        testScope.runTest {
            graphLoop.requestProcessor = grp1
            graphLoop.repeatingRequest = request1
            graphLoop.repeatingRequest = request2
            graphLoop.trigger(mapOf<Any, Any?>(TEST_KEY to 42))
            advanceUntilIdle()

            assertThat(csp1.events.size).isEqualTo(2)
            assertThat(csp1.events[0].isRepeating).isTrue()
            assertThat(csp1.events[0].requests).containsExactly(request2)
            assertThat(csp1.events[1].isCapture).isTrue() // Capture, based on request 2, with keys
            assertThat(csp1.events[1].requests).containsExactly(request2)
            assertThat(csp1.events[1].requiredParameters).containsEntry(TEST_KEY, 42)
        }

    @Test
    fun abortCaptureIsProcessedBeforeGraphRequestProcessorEvents() =
        testScope.runTest {
            graphLoop.requestProcessor = grp1
            graphLoop.submit(listOf(request1))
            graphLoop.abort()
            graphLoop.requestProcessor = grp2 // Change the graphRequestProcessor
            advanceUntilIdle()

            assertThat(csp1.events.size).isEqualTo(1)
            assertThat(csp1.events[0].isClose).isTrue()

            // Abort happens before grp1 & grp2
            assertThat(csp2.events).isEmpty()
        }

    @Test
    fun abortCaptureIsProcessedOnActiveRequestProcessor() =
        testScope.runTest {
            graphLoop.requestProcessor = grp1
            advanceUntilIdle()

            graphLoop.submit(listOf(request1))
            graphLoop.abort()
            graphLoop.requestProcessor = grp2 // Change the graphRequestProcessor
            advanceUntilIdle()

            assertThat(csp1.events.size).isEqualTo(2)
            assertThat(csp1.events[0].isAbort).isTrue()
            assertThat(csp1.events[1].isClose).isTrue()

            // Abort happens before grp1 & grp2
            assertThat(csp2.events).isEmpty()
        }

    @Test
    fun stopCaptureIsOnlyInvokedOnActiveGraphRequestProcessor() =
        testScope.runTest {
            graphLoop.repeatingRequest = request1
            graphLoop.requestProcessor = grp1
            advanceUntilIdle()

            graphLoop.repeatingRequest = request2
            graphLoop.repeatingRequest = null
            graphLoop.requestProcessor = grp2 // Change the graphRequestProcessor
            advanceUntilIdle()

            assertThat(csp1.events.size).isEqualTo(3)
            assertThat(csp1.events[0].isRepeating).isTrue()
            assertThat(csp1.events[1].isStopRepeating).isTrue() // StopRepeating is fired.
            assertThat(csp1.events[2].isClose).isTrue()

            assertThat(csp2.events).isEmpty()
        }

    @Test
    fun abortAndStopDoNotPropagateToNewRequestProcessor() =
        testScope.runTest {
            graphLoop.repeatingRequest = request1
            graphLoop.submit(listOf(request2))
            graphLoop.repeatingRequest = null
            graphLoop.abort()
            graphLoop.submit(listOf(request3))
            graphLoop.requestProcessor = grp1
            advanceUntilIdle()

            assertThat(csp1.events.size).isEqualTo(1)
            assertThat(csp1.events[0].isCapture).isTrue()
            assertThat(csp1.events[0].requests).containsExactly(request3)
        }

    @Test
    fun stopCaptureOnlyRemovesPriorStopCapturesFromSameGraphRequestProcessor() =
        testScope.runTest {
            graphLoop.requestProcessor = grp1
            graphLoop.repeatingRequest = request1
            graphLoop.repeatingRequest = null // issue stopCapture #1 with grp1
            graphLoop.requestProcessor = grp2
            graphLoop.repeatingRequest = request2
            graphLoop.repeatingRequest = null // issue stopCapture #2 with grp2 (skip r1, r2)

            advanceUntilIdle()

            assertThat(csp1.events.size).isEqualTo(1)
            assertThat(csp1.events[0].isClose).isTrue()

            assertThat(csp2.events).isEmpty()
        }

    @Test
    fun submitParametersBeforeRequestProcessorUsesLatestRepeatingRequest() =
        testScope.runTest {
            graphLoop.repeatingRequest = request1
            graphLoop.repeatingRequest = request2
            graphLoop.trigger(mapOf<Any, Any?>(TEST_KEY to 42))
            graphLoop.repeatingRequest = request3
            graphLoop.requestProcessor = grp1
            advanceUntilIdle()

            assertThat(csp1.events.size).isEqualTo(3)
            assertThat(csp1.events[0].isRepeating).isTrue()
            assertThat(csp1.events[0].requests).containsExactly(request2)
            assertThat(csp1.events[0].requiredParameters).isEmpty()

            assertThat(csp1.events[1].isCapture).isTrue()
            assertThat(csp1.events[1].requests).containsExactly(request2)
            assertThat(csp1.events[1].requiredParameters).containsEntry(TEST_KEY, 42)

            assertThat(csp1.events[2].isRepeating).isTrue()
            assertThat(csp1.events[2].requests).containsExactly(request3)
            assertThat(csp1.events[2].requiredParameters).isEmpty()
        }

    @Test
    fun abortWillSkipSubmitParameters() =
        testScope.runTest {
            graphLoop.repeatingRequest = request1
            graphLoop.repeatingRequest = request2
            graphLoop.trigger(mapOf<Any, Any?>(TEST_KEY to 42))
            graphLoop.repeatingRequest = request3
            graphLoop.requestProcessor = grp1
            graphLoop.abort()
            advanceUntilIdle()

            assertThat(csp1.events).isEmpty()
        }

    @Test
    fun requestsCanBeSubmittedWithParameters() =
        testScope.runTest {
            graphLoop.requestProcessor = grp1
            graphLoop.repeatingRequest = request1
            graphLoop.trigger(mapOf<Any, Any?>(TEST_KEY to 42))
            graphLoop.submit(listOf(request2))
            advanceUntilIdle()

            assertThat(csp1.events.size).isEqualTo(3)
            assertThat(csp1.events[0].isRepeating).isTrue()
            assertThat(csp1.events[0].requests).containsExactly(request1)
            assertThat(csp1.events[0].requiredParameters).isEmpty()

            assertThat(csp1.events[1].isCapture).isTrue()
            assertThat(csp1.events[1].requests).containsExactly(request1)
            assertThat(csp1.events[1].requiredParameters).containsEntry(TEST_KEY, 42)

            assertThat(csp1.events[2].isCapture).isTrue()
            assertThat(csp1.events[2].requests).containsExactly(request2)
            assertThat(csp1.events[2].requiredParameters).isEmpty()
        }

    @Test
    fun defaultParametersAreAppliedToAllRequests() =
        testScope.runTest {
            val gl =
                GraphLoop(
                    cameraGraphId = cameraGraphId,
                    defaultParameters = mapOf<Any, Any?>(TEST_KEY to 10),
                    requiredParameters = requiredParameters,
                    graphListeners = listOf(mockListener),
                    listeners = listOf(listener3A),
                    shutdownScope = shutdownScope,
                    dispatcher = testDispatcher,
                )

            gl.requestProcessor = grp1
            gl.repeatingRequest = request1
            gl.trigger(mapOf<Any, Any?>(TEST_KEY to 42))
            gl.submit(listOf(request2))
            advanceUntilIdle()

            assertThat(csp1.events.size).isEqualTo(3)
            assertThat(csp1.events[0].isRepeating).isTrue()
            assertThat(csp1.events[0].requests).containsExactly(request1)
            assertThat(csp1.events[0].defaultParameters).containsEntry(TEST_KEY, 10)
            assertThat(csp1.events[0].requiredParameters).isEmpty()

            assertThat(csp1.events[1].isCapture).isTrue()
            assertThat(csp1.events[1].requests).containsExactly(request1)
            assertThat(csp1.events[1].defaultParameters).containsEntry(TEST_KEY, 10)
            assertThat(csp1.events[1].requiredParameters).containsEntry(TEST_KEY, 42)

            assertThat(csp1.events[2].isCapture).isTrue()
            assertThat(csp1.events[2].requests).containsExactly(request2)
            assertThat(csp1.events[2].defaultParameters).containsEntry(TEST_KEY, 10)
            assertThat(csp1.events[2].requiredParameters).isEmpty()
        }

    @Test
    fun requiredParametersOverrideSubmittedParametersAndCameraGraphParameters() =
        testScope.runTest {
            val gl =
                GraphLoop(
                    cameraGraphId = cameraGraphId,
                    defaultParameters = emptyMap<Any, Any?>(),
                    requiredParameters = mapOf<Any, Any?>(TEST_KEY to 10),
                    graphListeners = listOf(mockListener),
                    listeners = listOf(listener3A),
                    shutdownScope = shutdownScope,
                    dispatcher = testDispatcher,
                )

            gl.requestProcessor = grp1
            gl.repeatingRequest = request1
            gl.graphParameters = mapOf(TEST_KEY to 1)
            gl.trigger(mapOf<Any, Any?>(TEST_KEY to 42))
            gl.submit(listOf(request2))
            advanceUntilIdle()

            assertThat(csp1.events.size).isEqualTo(3)
            assertThat(csp1.events[0].isRepeating).isTrue()
            assertThat(csp1.events[0].requests).containsExactly(request1)
            assertThat(csp1.events[0].defaultParameters).isEmpty()
            assertThat(csp1.events[0].graphParameters).containsEntry(TEST_KEY, 1)
            assertThat(csp1.events[0].requiredParameters).containsEntry(TEST_KEY, 10)

            assertThat(csp1.events[1].isCapture).isTrue()
            assertThat(csp1.events[1].requests).containsExactly(request1)
            assertThat(csp1.events[1].defaultParameters).isEmpty()
            assertThat(csp1.events[1].graphParameters).containsEntry(TEST_KEY, 1)
            assertThat(csp1.events[1].requiredParameters).containsEntry(TEST_KEY, 10)

            assertThat(csp1.events[2].isCapture).isTrue()
            assertThat(csp1.events[2].requests).containsExactly(request2)
            assertThat(csp1.events[2].defaultParameters).isEmpty()
            assertThat(csp1.events[2].graphParameters).containsEntry(TEST_KEY, 1)
            assertThat(csp1.events[2].requiredParameters).containsEntry(TEST_KEY, 10)
        }

    @Test
    fun requestsSubmittedToClosedRequestProcessorAreEnqueuedToTheNextOne() =
        testScope.runTest {
            graphLoop.requestProcessor = grp1
            grp1.shutdown()
            graphLoop.repeatingRequest = request1
            graphLoop.trigger(mapOf<Any, Any?>(TEST_KEY to 42))
            graphLoop.submit(listOf(request2))
            advanceUntilIdle()

            graphLoop.requestProcessor = grp2
            advanceUntilIdle()

            assertThat(csp2.events.size).isEqualTo(3)
            assertThat(csp2.events[0].isRepeating).isTrue()
            assertThat(csp2.events[0].requests).containsExactly(request1)
            assertThat(csp2.events[0].requiredParameters).isEmpty()

            assertThat(csp2.events[1].isCapture).isTrue()
            assertThat(csp2.events[1].requests).containsExactly(request1)
            assertThat(csp2.events[1].requiredParameters).containsEntry(TEST_KEY, 42)

            assertThat(csp2.events[2].isCapture).isTrue()
            assertThat(csp2.events[2].requests).containsExactly(request2)
            assertThat(csp2.events[2].requiredParameters).isEmpty()
        }

    @Test
    fun closingGraphLoopClosesRequestProcessor() =
        testScope.runTest {
            graphLoop.requestProcessor = grp1
            graphLoop.close()
            advanceUntilIdle()

            assertThat(csp1.events.size).isEqualTo(1)
            assertThat(csp1.events[0].isClose).isTrue()
        }

    @Test
    fun swappingRequestProcessorClosesPreviousRequestProcessor() =
        testScope.runTest {
            graphLoop.requestProcessor = grp1
            graphLoop.requestProcessor = grp2
            advanceUntilIdle()

            assertThat(csp1.events.size).isEqualTo(1)
            assertThat(csp1.events[0].isClose).isTrue()

            assertThat(csp2.events).isEmpty()
        }

    @Test
    fun submitParametersUseInitialRequest() =
        testScope.runTest {
            graphLoop.requestProcessor = grp1
            graphLoop.repeatingRequest = request1
            graphLoop.trigger(mapOf<Any, Any?>(TEST_KEY to 42))
            graphLoop.repeatingRequest = request2
            advanceUntilIdle()

            assertThat(csp1.events.size).isEqualTo(3)
            assertThat(csp1.events[0].isRepeating).isTrue()
            assertThat(csp1.events[0].requests).containsExactly(request1)
            assertThat(csp1.events[0].requiredParameters).isEmpty()

            assertThat(csp1.events[1].isCapture).isTrue()
            assertThat(csp1.events[1].requests).containsExactly(request1) // uses original request
            assertThat(csp1.events[1].requiredParameters).containsEntry(TEST_KEY, 42)

            assertThat(csp1.events[2].isRepeating).isTrue()
            assertThat(csp1.events[2].requests).containsExactly(request2)
            assertThat(csp1.events[2].requiredParameters).isEmpty()
        }

    @Test
    fun submitParametersDoesNotWorkIfRepeatingRequestIsStopped() =
        testScope.runTest {
            graphLoop.requestProcessor = grp1
            graphLoop.repeatingRequest = request1
            graphLoop.trigger(mapOf<Any, Any?>(TEST_KEY to 42))
            graphLoop.repeatingRequest = null
            advanceUntilIdle()

            assertThat(csp1.events).isEmpty()
        }

    @Test
    fun exceptionsAreThrown() {
        assertThrows(RuntimeException::class.java) {
                testScope.runTest {
                    graphLoop.requestProcessor = grp1
                    csp1.throwOnBuild = true
                    graphLoop.repeatingRequest = request1

                    advanceUntilIdle()
                }
            }
            .hasMessageThat()
            .contains("Test Exception")
    }

    @Test
    fun stopRepeatingCancelsTriggers() =
        testScope.runTest {
            val listener = Result3AStateListenerImpl({ _ -> true }, 10, 1_000_000_000)
            listener3A.addListener(listener)
            assertThat(listener.result.isCompleted).isFalse()

            graphLoop.repeatingRequest = null

            assertThat(listener.result.isCompleted).isTrue()
            assertThat(listener.result.await().status).isEqualTo(Result3A.Status.SUBMIT_CANCELLED)
        }

    @Test
    fun clearingRequestProcessorCancelsTriggers() =
        testScope.runTest {
            // Setup the graph loop so that the repeating request and trigger are enqueued before
            // the graphRequestProcessor is configured. Assert that the listener is not invoked
            // until after the requestProcessor is stopped.
            graphLoop.repeatingRequest = request1
            val listener = Result3AStateListenerImpl({ _ -> true }, 10, 1_000_000_000)
            listener3A.addListener(listener)
            graphLoop.trigger(mapOf<Any, Any?>(TEST_KEY to 42))
            graphLoop.requestProcessor = grp1
            advanceUntilIdle()

            assertThat(listener.result.isCompleted).isFalse()

            graphLoop.requestProcessor = null
            advanceUntilIdle()

            assertThat(listener.result.isCompleted).isTrue()
            assertThat(listener.result.await().status).isEqualTo(Result3A.Status.SUBMIT_CANCELLED)
        }

    @Test
    fun shutdownRequestProcessorCancelsTriggers() =
        testScope.runTest {
            // Arrange
            val listener = Result3AStateListenerImpl({ _ -> true }, 10, 1_000_000_000)
            listener3A.addListener(listener)

            // Act
            graphLoop.requestProcessor = null

            // Assert
            assertThat(listener.result.isCompleted).isTrue()
            assertThat(listener.result.await().status).isEqualTo(Result3A.Status.SUBMIT_CANCELLED)
        }

    @Test
    fun swappingRequestProcessorsDoesNotCancelTriggers() {
        testScope.runTest {
            // Arrange

            // Setup the graph loop so that the repeating request and trigger are enqueued before
            // the graphRequestProcessor is configured. Assert that the listener is not invoked
            // until after the requestProcessor is stopped.
            graphLoop.requestProcessor = grp1
            val listener = Result3AStateListenerImpl({ _ -> true }, 10, 1_000_000_000)
            listener3A.addListener(listener)
            graphLoop.requestProcessor = grp2 // Does not cancel trigger
            advanceUntilIdle()
            assertThat(listener.result.isCompleted).isFalse()

            // Act
            graphLoop.requestProcessor = null // Cancel triggers

            // Assert
            assertThat(listener.result.isCompleted).isTrue()
            assertThat(listener.result.await().status).isEqualTo(Result3A.Status.SUBMIT_CANCELLED)
        }
    }

    @Test
    fun pausingCaptureProcessingPreventsCaptureRequests() =
        testScope.runTest {
            // Arrange
            graphLoop.requestProcessor = grp1
            graphLoop.captureProcessingEnabled = false // Disable captureProcessing

            // Act
            graphLoop.submit(listOf(request1))
            graphLoop.submit(listOf(request2))
            advanceUntilIdle()

            // Assert: Events are not processed
            assertThat(csp1.events.size).isEqualTo(0)
        }

    @Test
    fun resumingCaptureProcessingResumesCaptureRequests() =
        testScope.runTest {
            // Arrange
            graphLoop.requestProcessor = grp1
            graphLoop.captureProcessingEnabled = false // Disable captureProcessing

            // Act
            graphLoop.submit(listOf(request1))
            graphLoop.submit(listOf(request2))
            advanceUntilIdle()
            graphLoop.captureProcessingEnabled = true // Enable processing
            advanceUntilIdle()

            // Assert: Events are not processed
            assertThat(csp1.events.size).isEqualTo(2)

            assertThat(csp1.events[0].isCapture).isTrue()
            assertThat(csp1.events[0].requests).containsExactly(request1)

            assertThat(csp1.events[1].isCapture).isTrue()
            assertThat(csp1.events[1].requests).containsExactly(request2)
        }

    @Test
    fun disablingCaptureProcessingAllowsRepeatingRequests() =
        testScope.runTest {
            // Arrange
            graphLoop.requestProcessor = grp1

            // Act
            graphLoop.captureProcessingEnabled = false // Disable captureProcessing
            graphLoop.repeatingRequest = request1
            advanceUntilIdle()

            // Assert
            assertThat(csp1.events.size).isEqualTo(1)
            assertThat(csp1.events[0].isRepeating).isTrue()
            assertThat(csp1.events[0].requests).containsExactly(request1)
        }

    @Test
    fun settingNullForRequestProcessorAfterCloseDoesNotCrash() =
        testScope.runTest {
            // Arrange
            graphLoop.requestProcessor = grp1
            graphLoop.close()

            // Act
            graphLoop.requestProcessor = null
            advanceUntilIdle()

            // Assert: does not crash, and only Close is invoked.
            assertThat(csp1.events.size).isEqualTo(1)
            assertThat(csp1.events[0].isClose).isTrue()
        }

    @Test
    fun settingRequestProcessorAfterCloseCausesRequestProcessorToBeShutdown() =
        testScope.runTest {
            // Arrange
            graphLoop.close()

            // Act
            graphLoop.requestProcessor = grp1
            advanceUntilIdle()

            // Assert: Does not crash, and request processor is closed.
            assertThat(csp1.events.size).isEqualTo(1)
            assertThat(csp1.events[0].isClose).isTrue()
        }

    @Test
    fun settingRequestProcessorAfterShutdownCausesRequestProcessorToBeShutdown() =
        testScope.runTest {
            // Arrange

            graphLoop.requestProcessor = grp1
            graphLoop.close()
            advanceUntilIdle() // Shutdown fully completes

            // Act
            graphLoop.requestProcessor = grp2
            advanceUntilIdle()

            // Assert: Does not crash, and request processor is closed.
            assertThat(csp1.events.size).isEqualTo(1)
            assertThat(csp1.events[0].isClose).isTrue()

            assertThat(csp2.events.size).isEqualTo(1)
            assertThat(csp2.events[0].isClose).isTrue()
        }

    @Test
    fun settingRepeatingRequestWhenRequestsAreRejectedDoesNotAttemptMultipleRepeatingRequests() =
        testScope.runTest {
            // Arrange
            csp1.rejectSubmit = true
            graphLoop.requestProcessor = grp1
            graphLoop.repeatingRequest = request1
            advanceUntilIdle()

            assertThat(csp1.events.size).isEqualTo(1)
            assertThat(csp1.events[0].isRejected).isTrue()
            assertThat(csp1.events[0].requests).containsExactly(request1)

            graphLoop.repeatingRequest = request2
            advanceUntilIdle()

            assertThat(csp1.events.size).isEqualTo(3)
            assertThat(csp1.events[1].isRejected).isTrue()
            assertThat(csp1.events[1].requests).containsExactly(request2) // First try most recent
            assertThat(csp1.events[2].isRejected).isTrue()
            assertThat(csp1.events[2].requests).containsExactly(request1) // THen try previous

            csp1.rejectSubmit = false
            graphLoop.invalidate()
            advanceUntilIdle()

            assertThat(csp1.events.size).isEqualTo(4)
            assertThat(csp1.events[3].isRepeating).isTrue()
            assertThat(csp1.events[3].requests).containsExactly(request2)
        }

    @Test
    fun updateGraphParametersInvokesSubmitRequestWithGraphParameters() =
        testScope.runTest {
            graphLoop.requestProcessor = grp1
            graphLoop.repeatingRequest = request1
            advanceUntilIdle()
            graphLoop.graphParameters = mapOf(TEST_KEY to 1)
            advanceUntilIdle()

            assertThat(csp1.events.size).isEqualTo(2)
            assertThat(csp1.events[0].isRepeating).isTrue()
            assertThat(csp1.events[0].requests).containsExactly(request1)

            assertThat(csp1.events[1].isRepeating).isTrue()
            assertThat(csp1.events[1].requests).containsExactly(request1)
            assertThat(csp1.events[1].graphParameters).containsEntry(TEST_KEY, 1)
        }

    @Test
    fun updateGraphParametersNoAdvanceUntilIdleInBetweenInvokesSubmitRequestWithGraphParameters() =
        testScope.runTest {
            graphLoop.requestProcessor = grp1

            graphLoop.repeatingRequest = request1
            graphLoop.graphParameters = mapOf(TEST_KEY to 1)
            advanceUntilIdle()

            assertThat(csp1.events.size).isEqualTo(1)
            assertThat(csp1.events[0].isRepeating).isTrue()
            assertThat(csp1.events[0].requests).containsExactly(request1)
            assertThat(csp1.events[0].graphParameters).containsEntry(TEST_KEY, 1)
        }

    @Test
    fun updateGraphParametersMultipleTimesPrioritizesUpdates() =
        testScope.runTest {
            graphLoop.requestProcessor = grp1

            graphLoop.repeatingRequest = request1
            graphLoop.graphParameters = mapOf(TEST_KEY to 1)
            graphLoop.graphParameters = mapOf(TEST_KEY to 2)
            graphLoop.graphParameters = mapOf(TEST_KEY to 3)
            graphLoop.repeatingRequest = request2
            graphLoop.graphParameters = mapOf(TEST_KEY to 4)
            graphLoop.repeatingRequest = request3
            advanceUntilIdle()

            assertThat(csp1.events.size).isEqualTo(1)
            assertThat(csp1.events[0].isRepeating).isTrue()
            assertThat(csp1.events[0].requests).containsExactly(request3)
            assertThat(csp1.events[0].graphParameters).containsEntry(TEST_KEY, 4)
        }

    @Test
    fun updateGraphParametersAfterRepeatingWithMultipleUpdatesPrioritizesLatest() =
        testScope.runTest {
            graphLoop.requestProcessor = grp1
            graphLoop.repeatingRequest = request1
            advanceUntilIdle()

            graphLoop.graphParameters = mapOf(TEST_KEY to 1)
            graphLoop.graphParameters = mapOf(TEST_KEY to 2)
            graphLoop.graphParameters = mapOf(TEST_KEY to 3)
            graphLoop.repeatingRequest = request2
            graphLoop.graphParameters = mapOf(TEST_KEY to 4)
            graphLoop.repeatingRequest = request3
            advanceUntilIdle()

            assertThat(csp1.events.size).isEqualTo(3)
            assertThat(csp1.events[0].isRepeating).isTrue()
            assertThat(csp1.events[0].requests).containsExactly(request1)
            assertThat(csp1.events[0].graphParameters).isEmpty()

            assertThat(csp1.events[1].isRepeating).isTrue()
            assertThat(csp1.events[1].requests).containsExactly(request1)
            assertThat(csp1.events[1].graphParameters).containsEntry(TEST_KEY, 4)

            assertThat(csp1.events[2].isRepeating).isTrue()
            assertThat(csp1.events[2].requests).containsExactly(request3)
            assertThat(csp1.events[2].graphParameters).containsEntry(TEST_KEY, 4)
        }

    @Test
    fun updateGraphParametersBeforeRepeatingRequestReadySubmitRequestWhenReady() =
        testScope.runTest {
            graphLoop.requestProcessor = grp1

            graphLoop.graphParameters = mapOf(TEST_KEY to 1)
            advanceUntilIdle()
            graphLoop.repeatingRequest = request1
            advanceUntilIdle()

            assertThat(csp1.events.size).isEqualTo(1)
            assertThat(csp1.events[0].isRepeating).isTrue()
            assertThat(csp1.events[0].requests).containsExactly(request1)
            assertThat(csp1.events[0].graphParameters).containsEntry(TEST_KEY, 1)
        }

    @Test
    fun updateGraphParametersBeforeRepeatingRequestReadyNoAdvanceUntilIdleInBetweenSubmitRequestWhenReady() =
        testScope.runTest {
            graphLoop.requestProcessor = grp1

            graphLoop.graphParameters = mapOf(TEST_KEY to 1)
            graphLoop.repeatingRequest = request1
            advanceUntilIdle()

            assertThat(csp1.events.size).isEqualTo(1)
            assertThat(csp1.events[0].isRepeating).isTrue()
            assertThat(csp1.events[0].requests).containsExactly(request1)
            assertThat(csp1.events[0].graphParameters).containsEntry(TEST_KEY, 1)
        }

    @Test
    fun updateGraphParametersFollowingSubmitContainsGraphParameters() =
        testScope.runTest {
            graphLoop.requestProcessor = grp1
            graphLoop.graphParameters = mapOf(TEST_KEY to 1)
            graphLoop.repeatingRequest = request1
            graphLoop.submit(listOf(request2))
            graphLoop.trigger(mapOf<Any, Any?>(CaptureRequest.CONTROL_AF_MODE to 1))
            advanceUntilIdle()

            assertThat(csp1.events.size).isEqualTo(3)
            assertThat(csp1.events[0].isRepeating).isTrue()
            assertThat(csp1.events[0].requests).containsExactly(request1)
            assertThat(csp1.events[0].graphParameters).containsEntry(TEST_KEY, 1)
            assertThat(csp1.events[0].requiredParameters).isEmpty()

            assertThat(csp1.events[1].isCapture).isTrue()
            assertThat(csp1.events[1].requests).containsExactly(request2)
            assertThat(csp1.events[1].graphParameters).containsEntry(TEST_KEY, 1)
            assertThat(csp1.events[1].requiredParameters).isEmpty()

            assertThat(csp1.events[2].isRepeating).isFalse()
            assertThat(csp1.events[2].requests).containsExactly(request1)
            assertThat(csp1.events[2].graphParameters).containsEntry(TEST_KEY, 1)
            assertThat(csp1.events[2].requiredParameters)
                .containsEntry(CaptureRequest.CONTROL_AF_MODE, 1)
        }

    @Test
    fun capturesAfterStopRepeatingHappenBeforeRepeatingRequests() =
        testScope.runTest {
            graphLoop.requestProcessor = grp1
            graphLoop.repeatingRequest = request1
            advanceUntilIdle()

            graphLoop.repeatingRequest = null // StopRepeating
            graphLoop.submit(request2) // First, submit
            graphLoop.repeatingRequest = request3 // Then, start repeating
            advanceUntilIdle()

            assertThat(csp1.events.size).isEqualTo(4)
            assertThat(csp1.events[0].isRepeating).isTrue()
            assertThat(csp1.events[0].requests).containsExactly(request1)

            assertThat(csp1.events[1].isStopRepeating).isTrue()

            assertThat(csp1.events[2].isCapture).isTrue()
            assertThat(csp1.events[2].requests)
                .containsExactly(request2) // Make sure capture is first

            assertThat(csp1.events[3].isRepeating).isTrue()
            assertThat(csp1.events[3].requests).containsExactly(request3)
        }

    @Test
    fun multipleRejectedCapturesWillAttemptCapture() =
        testScope.runTest {
            csp1.rejectSubmit = true
            graphLoop.requestProcessor = grp1
            graphLoop.repeatingRequest = request1
            graphLoop.repeatingRequest = request2
            graphLoop.submit(request3)
            advanceUntilIdle()

            assertThat(csp1.events.size).isEqualTo(3)
            // If there is no (current) repeating request, first attempt repeating requests.
            // Start by attempting R2 (most recent)
            assertThat(csp1.events[0].isRepeating).isTrue()
            assertThat(csp1.events[0].isRejected).isTrue()
            assertThat(csp1.events[0].requests).containsExactly(request2)

            // Fallback to R2 (next most recent repeating request)
            assertThat(csp1.events[1].isRepeating).isTrue()
            assertThat(csp1.events[1].isRejected).isTrue()
            assertThat(csp1.events[1].requests).containsExactly(request1)

            // Finally, fallback to attempting Capture (C1)
            assertThat(csp1.events[2].isCapture).isTrue()
            assertThat(csp1.events[2].isRejected).isTrue()
            assertThat(csp1.events[2].requests).containsExactly(request3)
        }

    @Test
    fun multipleRejectedCapturesWillAttemptRepeatingWithExistingRepeatingRequest() =
        testScope.runTest {
            graphLoop.requestProcessor = grp1
            graphLoop.repeatingRequest = request1
            advanceUntilIdle()

            csp1.rejectSubmit = true

            graphLoop.repeatingRequest = request2
            graphLoop.submit(request3)
            advanceUntilIdle()

            assertThat(csp1.events.size).isEqualTo(3)
            // R1 is set as the repeating request
            assertThat(csp1.events[0].isRepeating).isTrue()
            assertThat(csp1.events[0].requests).containsExactly(request1)

            // Requests are rejected. GraphLoop should start with the first Capture (since there is
            // already a successful repeating request)
            assertThat(csp1.events[1].isCapture).isTrue()
            assertThat(csp1.events[1].isRejected).isTrue()
            assertThat(csp1.events[1].requests).containsExactly(request3)

            // Since capture was rejected, it should then fall back to attempting R2.
            assertThat(csp1.events[2].isRepeating).isTrue()
            assertThat(csp1.events[2].requests).containsExactly(request2)
        }

    @Test
    fun triggerCommandsMergeRequiredParameters() =
        testScope.runTest {
            graphLoop.requestProcessor = grp1
            graphLoop.repeatingRequest = request1
            advanceUntilIdle()

            graphLoop.graph3AParameters = mapOf(TEST_KEY to 1)
            graphLoop.trigger(mapOf(TEST_KEY to 2))
            graphLoop.repeatingRequest = request2
            advanceUntilIdle()

            assertThat(csp1.events.size).isEqualTo(4)
            // R1 is issued as the repeating request
            assertThat(csp1.events[0].isRepeating).isTrue()
            assertThat(csp1.events[0].requests).containsExactly(request1)
            assertThat(csp1.events[0].requiredParameters).isEmpty()

            // The update to graph3A parameters causes the repeating request to be re-issued with
            // the
            // new parameters.
            assertThat(csp1.events[1].isRepeating).isTrue()
            assertThat(csp1.events[1].requests).containsExactly(request1)
            assertThat(csp1.events[1].graphParameters).isEmpty()
            assertThat(csp1.events[1].requiredParameters).containsEntry(TEST_KEY, 1)

            // Trigger causes a one-time event. Trigger parameters are prioritized over existing
            // graph3AParameters.
            assertThat(csp1.events[2].isCapture).isTrue()
            assertThat(csp1.events[2].requests).containsExactly(request1)
            assertThat(csp1.events[2].graphParameters).isEmpty()
            assertThat(csp1.events[2].requiredParameters).containsEntry(TEST_KEY, 2)

            // R2 is issued _after_ trigger and using the original required parameters.
            assertThat(csp1.events[3].isRepeating).isTrue()
            assertThat(csp1.events[3].requests).containsExactly(request2)
            assertThat(csp1.events[3].graphParameters).isEmpty()
            assertThat(csp1.events[3].requiredParameters).containsEntry(TEST_KEY, 1)
        }
}
