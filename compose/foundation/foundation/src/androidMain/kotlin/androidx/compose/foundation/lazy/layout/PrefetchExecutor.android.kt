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

package androidx.compose.foundation.lazy.layout

import android.view.Choreographer
import android.view.Display
import android.view.View
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.RememberObserver
import androidx.compose.runtime.collection.mutableVectorOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView
import java.util.concurrent.TimeUnit
import kotlin.math.max

@ExperimentalFoundationApi
@Composable
internal actual fun rememberDefaultPrefetchExecutor(): PrefetchExecutor {
    val view = LocalView.current
    return remember(view) {
        AndroidPrefetchExecutor(view)
    }
}

/**
 * Android specific prefetch implementation. The only platform specific things are:
 * 1) Calculating the deadline
 * 2) Posting the delayed runnable
 * This could be refactored in the future in order to keep the most logic platform agnostic to
 * enable the prefetching on desktop.
 *
 * The differences with the implementation in RecyclerView:
 *
 * 1) Prefetch is per-list-index, and performed on whole item.
 *    With RecyclerView, nested scrolling RecyclerViews would prefetch incrementally, e.g. items
 *    like the following in a scrolling vertical list could be broken up within a frame:
 *    [Row1 [a], [b], [c]]
 *    [Row2 [d], [e]]
 *    [Row3 [f], [g], [h]]
 *    You could have frames that break up this work arbitrarily:
 *    Frame 1 - prefetch [a]
 *    Frame 2 - prefetch [b], [c]
 *    Frame 3 - prefetch [d]
 *    Frame 4 - prefetch [e], [f]
 *    Something similar is not possible with LazyColumn yet.
 *
 * 2) Prefetching time estimation only captured during the prefetch.
 *    We currently don't track the time of the regular subcompose call happened during the regular
 *    measure pass, only the ones which are done during the prefetching. The downside is we build
 *    our prefetch information only after scrolling has started and items are showing up. Your very
 *    first scroll won't know if it's safe to prefetch. Why:
 *    a) SubcomposeLayout is not exposing an API to understand if subcompose() call is going to
 *    do the real work. The work could be skipped if the same lambda was passed as for the
 *    previous invocation or if there were no recompositions scheduled. We could workaround it
 *    by keeping the extra state in LazyListState about what items we already composed and to
 *    only measure the first composition for the given slot, or consider exposing extra
 *    information in SubcomposeLayoutState API.
 *    b) It allows us to nicely decouple the logic, now the prefetching logic is build on
 *    top of the regular LazyColumn measuring functionallity and the main logic knows nothing
 *    about prefetch
 *    c) Maybe the better approach would be to wait till the low-level runtime infra is ready to
 *    do subcompositions on the different threads which illuminates the need to calculate the
 *    deadlines completely.
 *    Tracking bug: b/187393381.
 *
 * 3) Prefetch is not aware of item type.
 *    RecyclerView separates timing metadata about different item types. For example, in play
 *    store style UI, this allows RecyclerView to separately estimate the cost of a header,
 *    separator, and item row. In this implementation, all of these would be averaged together in
 *    the same estimation.
 *    There is no view type concept in LazyColumn at all. What can we possible do:
 *    a) Think of different item/items calls in the builder dsl as different view types
 *    automatically. It is close enough but still not entirely correct if the user have something
 *    like a list of elements which are objects of some sealed classes and they consider
 *    different classes as completely different types
 *    b) Maybe if we would be able to precompose on the different thread this issue is also not
 *    so critical given that we don't need to calculate the deadline.
 *    Tracking bug: 187393922
 */
@ExperimentalFoundationApi
internal class AndroidPrefetchExecutor(
    private val view: View
) : PrefetchExecutor, RememberObserver, Runnable, Choreographer.FrameCallback {

    /**
     * The list of currently not processed prefetch requests. The requests will be processed one by
     * during subsequent [run]s.
     */
    private val prefetchRequests = mutableVectorOf<PrefetchRequest>()
    private var prefetchScheduled = false
    private val choreographer = Choreographer.getInstance()

    /** Is true when LazyList was composed and not yet disposed. */
    private var isActive = false

    init {
        calculateFrameIntervalIfNeeded(view)
    }

    /**
     * Callback to be executed when the prefetching is needed.
     * [prefetchRequests] will be used as an input.
     */
    override fun run() {
        if (prefetchRequests.isEmpty() || !prefetchScheduled || !isActive ||
            view.windowVisibility != View.VISIBLE
        ) {
            // incorrect input. ignore
            prefetchScheduled = false
            return
        }
        val latestFrameVsyncNs = TimeUnit.MILLISECONDS.toNanos(view.drawingTime)
        val nextFrameNs = latestFrameVsyncNs + frameIntervalNs
        val oneOverTimeTaskAllowed = System.nanoTime() > nextFrameNs
        val scope = PrefetchRequestScopeImpl(nextFrameNs, oneOverTimeTaskAllowed)
        var scheduleForNextFrame = false
        while (prefetchRequests.isNotEmpty() && !scheduleForNextFrame) {
            val request = prefetchRequests[0]
            val hasMoreWorkToDo = with(request) { scope.execute() }
            if (hasMoreWorkToDo) {
                scheduleForNextFrame = true
            } else {
                prefetchRequests.removeAt(0)
            }
        }

        if (scheduleForNextFrame) {
            // there is not enough time left in this frame. we schedule a next frame callback
            // in which we are going to post the message in the handler again.
            choreographer.postFrameCallback(this)
        } else {
            prefetchScheduled = false
        }
    }

    /**
     * Choreographer frame callback. It will be called when during the previous frame we didn't
     * have enough time left. We will post a new message in the handler in order to try to
     * prefetch again after this frame.
     */
    override fun doFrame(frameTimeNanos: Long) {
        if (isActive) {
            view.post(this)
        }
    }

    override fun requestPrefetch(prefetchRequest: PrefetchRequest) {
        prefetchRequests.add(prefetchRequest)
        if (!prefetchScheduled) {
            prefetchScheduled = true
            // schedule the prefetching
            view.post(this)
        }
    }

    override fun onRemembered() {
        isActive = true
    }

    override fun onForgotten() {
        isActive = false
        view.removeCallbacks(this)
        choreographer.removeFrameCallback(this)
    }

    override fun onAbandoned() {}

    class PrefetchRequestScopeImpl(
        private val nextFrameTimeNs: Long,
        isOneOverTimeTaskAllowed: Boolean
    ) : PrefetchRequestScope {

        private var canDoOverTimeTask = isOneOverTimeTaskAllowed

        override val availableTimeNanos: Long
            get() {
                // This logic is meant to be temporary until we replace the isOneOverTimeTaskAllowed
                // logic with something more general. For now, we assume that a PrefetchRequest
                // impl will check availableTimeNanos once per task and we give it a large amount
                // of time the first time it checks if we allow an overtime task.
                return if (canDoOverTimeTask) {
                    canDoOverTimeTask = false
                    Long.MAX_VALUE
                } else {
                    max(0, nextFrameTimeNs - System.nanoTime())
                }
            }
    }

    companion object {

        /**
         * The static cache in order to not gather the display refresh rate to often (expensive operation).
         */
        private var frameIntervalNs: Long = 0

        private fun calculateFrameIntervalIfNeeded(view: View) {
            // we only do this query once, statically, because it's very expensive (> 1ms)
            if (frameIntervalNs == 0L) {
                val display: Display? = view.display
                var refreshRate = 60f
                if (!view.isInEditMode && display != null) {
                    val displayRefreshRate = display.refreshRate
                    if (displayRefreshRate >= 30f) {
                        // break 60 fps assumption if data from display appears valid
                        refreshRate = displayRefreshRate
                    }
                }
                frameIntervalNs = (1000000000 / refreshRate).toLong()
            }
        }
    }
}
