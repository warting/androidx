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

package androidx.privacysandbox.ui.provider.test

import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import androidx.annotation.RequiresApi
import androidx.privacysandbox.ui.provider.ProviderViewWrapper
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
@RunWith(AndroidJUnit4::class)
@LargeTest
class ProviderViewWrapperTest {
    companion object {
        const val TIMEOUT_MILLIS: Long = 2000
        const val VSYNC_INTERVAL_MS: Long = 100
        const val WIDTH = 500
        const val HEIGHT = 500
    }

    @get:Rule val activityRule = ActivityScenarioRule(MainActivity::class.java)

    private lateinit var mainHandler: Handler
    private lateinit var providerViewWrapper: ProviderViewWrapper
    private lateinit var providerView: View
    private var dispatchedEventsSinceLastFrame = 0

    @Before
    fun setup() {
        activityRule.scenario.onActivity { activity ->
            mainHandler = Handler(Looper.getMainLooper())
            providerViewWrapper = ProviderViewWrapper(activity)
            providerView = View(activity)
            providerView.layoutParams = FrameLayout.LayoutParams(WIDTH, HEIGHT)
            providerViewWrapper.addView(providerView)
            activity.setContentView(providerViewWrapper)
            setUpOnTouchListener()
        }
    }

    @Test
    fun processingOfMotionEventsWillNotScheduledIfProviderViewDetachedTest() {
        // Closing the activity, detaching the View.
        activityRule.scenario.close()

        val frameTimes = simulateFrameTimes(/* frames */ 3)

        val events = createGestureEvents(/* moveEventNumbers */ 1)
        val eventTargetTimesRelativeToFrames =
            listOf<Long>(
                VSYNC_INTERVAL_MS / 2, // (vsync_interval / 2) after frame1
                0, // at frame2 time
                -(VSYNC_INTERVAL_MS / 2) // (vsync_interval / 2) before frame3
            )
        simulateSchedulingEventDispatchingMessages(
            events,
            frameTimes,
            eventTargetTimesRelativeToFrames
        )

        val expectedEventsPerFrame = listOf<Long>(0, 0, 0)
        assertNumberOfDispatchedEventsOnSimulateFrameTimes(frameTimes, expectedEventsPerFrame)
    }

    @Test
    fun removePendingMotionEventDispatchMessagesIfViewIsDetachedTest() {
        val frameTimes = simulateFrameTimes(/* frames */ 3)

        val events = createGestureEvents(/* moveEventNumbers */ 1)
        val eventTargetTimesRelativeToFrames =
            listOf<Long>(
                VSYNC_INTERVAL_MS / 2, // (vsync_interval / 2) after frame1
                0, // at frame2 time
                -(VSYNC_INTERVAL_MS / 2) // (vsync_interval / 2) before frame3
            )
        simulateSchedulingEventDispatchingMessages(
            events,
            frameTimes,
            eventTargetTimesRelativeToFrames
        )

        activityRule.scenario.close()

        val expectedEventsPerFrame = listOf<Long>(0, 0, 0)
        assertNumberOfDispatchedEventsOnSimulateFrameTimes(frameTimes, expectedEventsPerFrame)
    }

    @Test
    fun processMotionEventsTargetingNextFrameTest() {
        val frameTimes = simulateFrameTimes(/* frames */ 5)

        // To simulate real scenarios, setting DOWN, UP and last MOVE as unbuffered events, and
        // send them randomly between frames.
        val events = createGestureEvents(/* moveEventNumbers */ 3)
        val eventTargetTimesRelativeToFrames =
            listOf<Long>(
                VSYNC_INTERVAL_MS / 2, // (vsync_interval / 2) after frame1
                0, // at frame2 time
                0, // at frame3 time
                -(VSYNC_INTERVAL_MS / 2), // (vsync_interval / 2) before frame4
                -(VSYNC_INTERVAL_MS / 2) // (vsync_interval / 2) before frame5
            )

        simulateSchedulingEventDispatchingMessages(
            events,
            frameTimes,
            eventTargetTimesRelativeToFrames
        )

        val expectedEventsPerFrame = listOf<Long>(0, 1, 1, 2, 1)
        assertNumberOfDispatchedEventsOnSimulateFrameTimes(frameTimes, expectedEventsPerFrame)
    }

    private fun setUpOnTouchListener() {
        providerView.setOnTouchListener { _, event ->
            dispatchedEventsSinceLastFrame++
            true
        }
    }

    private fun simulateFrameTimes(frames: Int): List<Long> {
        val firstFrameTime = SystemClock.uptimeMillis() + VSYNC_INTERVAL_MS
        return List(frames) { index -> firstFrameTime + (index * VSYNC_INTERVAL_MS) }
    }

    private fun createGestureEvents(moveEventNumbers: Int): List<MotionEvent> {
        val events: MutableList<MotionEvent> = mutableListOf()
        events.add(createMotionEvent(MotionEvent.ACTION_DOWN))
        repeat(moveEventNumbers) { events.add(createMotionEvent(MotionEvent.ACTION_MOVE)) }
        events.add(createMotionEvent(MotionEvent.ACTION_UP))
        return events
    }

    private fun createMotionEvent(motionEventAction: Int): MotionEvent {
        return MotionEvent.obtain(
            SystemClock.uptimeMillis(),
            SystemClock.uptimeMillis(),
            motionEventAction,
            providerView.width / 2f,
            providerView.width / 2f,
            /* metaState = */ 0
        )
    }

    private fun simulateSchedulingEventDispatchingMessages(
        events: List<MotionEvent>,
        frameTimes: List<Long>,
        eventTargetTimesRelativeToFrames: List<Long>
    ) {
        val eventTargetTime =
            eventTargetTimesRelativeToFrames.withIndex().map { (index, value) ->
                frameTimes[index] + value
            }

        for (i in 0 until events.size) {
            providerViewWrapper.scheduleMotionEventProcessing(events[i], eventTargetTime[i])
        }
    }

    private fun assertNumberOfDispatchedEventsOnSimulateFrameTimes(
        frameTimes: List<Long>,
        expectedDispatchedEventsPerFrame: List<Long>
    ) {
        val allFramesPassedLatch = CountDownLatch(frameTimes.size)
        // Simulating doFrame at the frame times.
        for (i in 0 until frameTimes.size) {
            mainHandler.postAtTime(
                {
                    assertThat(dispatchedEventsSinceLastFrame)
                        .isEqualTo(expectedDispatchedEventsPerFrame[i])
                    dispatchedEventsSinceLastFrame = 0
                    allFramesPassedLatch.countDown()
                },
                frameTimes[i]
            )
        }
        assertTrue(
            "Timeout before passing all frames",
            allFramesPassedLatch.await(TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)
        )
    }
}
