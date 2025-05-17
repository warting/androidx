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

package androidx.compose.ui.input.pointer

import android.view.MotionEvent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.ViewConfiguration
import androidx.compose.ui.platform.ViewRootForTest
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.TouchInjectionScope
import androidx.compose.ui.test.tryPerformAccessibilityChecks
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.core.view.InputDeviceCompat
import androidx.test.platform.app.InstrumentationRegistry
import kotlin.math.roundToInt

// We don't have StylusInjectionScope at the moment. This is a simplified implementation for
// the basic use cases in tests. It only supports single stylus pointer, and the pointerId
// is totally ignored.
internal class HandwritingTestStylusInjectScope(semanticsNode: SemanticsNode) :
    TouchInjectionScope, Density by semanticsNode.layoutInfo.density {
    private val root = semanticsNode.root as ViewRootForTest
    private val downTime: Long = System.currentTimeMillis()

    private var lastPosition: Offset = Offset.Unspecified
    private var currentTime: Long = System.currentTimeMillis()
    private val boundsInRoot = semanticsNode.boundsInRoot

    override val visibleSize: IntSize =
        IntSize(boundsInRoot.width.roundToInt(), boundsInRoot.height.roundToInt())

    override val viewConfiguration: ViewConfiguration = semanticsNode.layoutInfo.viewConfiguration

    private fun localToRoot(position: Offset): Offset {
        return position + boundsInRoot.topLeft
    }

    override fun advanceEventTime(durationMillis: Long) {
        require(durationMillis >= 0) {
            "duration of a delay can only be positive, not $durationMillis"
        }
        currentTime += durationMillis
    }

    override fun currentPosition(pointerId: Int): Offset {
        return lastPosition
    }

    override fun down(pointerId: Int, position: Offset) {
        lastPosition = localToRoot(position)
        sendTouchEvent(MotionEvent.ACTION_DOWN)
    }

    override fun updatePointerTo(pointerId: Int, position: Offset) {
        lastPosition = localToRoot(position)
    }

    override fun move(delayMillis: Long) {
        advanceEventTime(delayMillis)
        sendTouchEvent(MotionEvent.ACTION_MOVE)
    }

    @ExperimentalTestApi
    override fun moveWithHistoryMultiPointer(
        relativeHistoricalTimes: List<Long>,
        historicalCoordinates: List<List<Offset>>,
        delayMillis: Long,
    ) {
        // Not needed for this test because Android only supports one stylus pointer.
    }

    override fun up(pointerId: Int) {
        sendTouchEvent(MotionEvent.ACTION_UP)
    }

    fun hoverEnter(position: Offset = lastPosition, delayMillis: Long = eventPeriodMillis) {
        advanceEventTime(delayMillis)
        lastPosition = localToRoot(position)
        sendTouchEvent(MotionEvent.ACTION_HOVER_ENTER)
    }

    fun hoverMoveTo(position: Offset, delayMillis: Long = eventPeriodMillis) {
        advanceEventTime(delayMillis)
        lastPosition = localToRoot(position)
        sendTouchEvent(MotionEvent.ACTION_HOVER_MOVE)
    }

    fun hoverExit(position: Offset = lastPosition, delayMillis: Long = eventPeriodMillis) {
        advanceEventTime(delayMillis)
        lastPosition = localToRoot(position)
        sendTouchEvent(MotionEvent.ACTION_HOVER_EXIT)
    }

    override fun cancel(delayMillis: Long) {
        sendTouchEvent(MotionEvent.ACTION_CANCEL)
    }

    private fun sendTouchEvent(action: Int) {
        val motionEvent =
            MotionEvent.obtain(
                /* downTime = */ downTime,
                /* eventTime = */ currentTime,
                /* action = */ action,
                /* pointerCount = */ 1,
                /* pointerProperties = */ arrayOf(
                    MotionEvent.PointerProperties().apply {
                        id = 0
                        toolType = MotionEvent.TOOL_TYPE_STYLUS
                    }
                ),
                /* pointerCoords = */ arrayOf(
                    MotionEvent.PointerCoords().apply {
                        val startOffset = lastPosition

                        // Allows for non-valid numbers/Offsets to be passed along to Compose to
                        // test if it handles them properly (versus breaking here and we not knowing
                        // if Compose properly handles these values).
                        x =
                            if (startOffset.isValid()) {
                                startOffset.x
                            } else {
                                Float.NaN
                            }

                        y =
                            if (startOffset.isValid()) {
                                startOffset.y
                            } else {
                                Float.NaN
                            }
                    }
                ),
                /* metaState = */ 0,
                /* buttonState = */ 0,
                /* xPrecision = */ 1f,
                /* yPrecision = */ 1f,
                /* deviceId = */ 0,
                /* edgeFlags = */ 0,
                /* source = */ InputDeviceCompat.SOURCE_STYLUS,
                /* flags = */ 0,
            )

        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            root.view.dispatchTouchEvent(motionEvent)
        }
    }
}

internal fun SemanticsNodeInteraction.performStylusInput(
    block: HandwritingTestStylusInjectScope.() -> Unit
): SemanticsNodeInteraction {
    tryPerformAccessibilityChecks()
    val node = fetchSemanticsNode("Failed to inject stylus input.")
    val stylusInjectionScope = HandwritingTestStylusInjectScope(node)
    block.invoke(stylusInjectionScope)
    return this
}
