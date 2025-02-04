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

package androidx.privacysandbox.ui.provider

import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Message
import android.view.MotionEvent
import android.widget.FrameLayout
import androidx.annotation.RequiresApi

/**
 * A container [FrameLayout] that wraps the provider content view.
 *
 * It dispatches [MotionEvent] objects passed from the host against the provider view. [MotionEvent]
 * objects passed from the host (accompanied with target frame time) are scheduled to be dispatched
 * on the UiThread with a delay from the target frame time, so any UI impact caused by processing
 * these events will target the following frame.
 */
@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
internal class ProviderViewWrapper(context: Context) : FrameLayout(context) {
    private companion object {
        // That delay will be used to ensure processing the transferred events after their target
        // frame time and before the following frame time.
        const val TRANSFERRED_EVENT_DISPATCH_DELAY_MS = 1
    }

    private var eventDispatchHandler: Handler? = null

    override fun onAttachedToWindow() {
        eventDispatchHandler = Handler(handler.looper)
        super.onAttachedToWindow()
    }

    override fun onDetachedFromWindow() {
        /** Remove [eventDispatchHandler] posted messages. */
        eventDispatchHandler?.removeCallbacksAndMessages(/* token */ null)
        eventDispatchHandler = null
        super.onDetachedFromWindow()
    }

    fun scheduleMotionEventProcessing(motionEvent: MotionEvent, eventTargetFrameTime: Long) {
        if (eventDispatchHandler == null) {
            return
        }
        val dispatchMessage: Message = Message.obtain(handler, { dispatchTouchEvent(motionEvent) })
        dispatchMessage.isAsynchronous = true

        eventDispatchHandler?.sendMessageAtTime(
            dispatchMessage,
            eventTargetFrameTime + TRANSFERRED_EVENT_DISPATCH_DELAY_MS
        )
    }
}
