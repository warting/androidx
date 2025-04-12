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

@file:Suppress("DEPRECATION")

package androidx.compose.ui.node

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.input.indirect.IndirectTouchEvent
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.node.RootForTest.UncaughtExceptionHandler
import androidx.compose.ui.semantics.SemanticsOwner
import androidx.compose.ui.text.input.TextInputService
import androidx.compose.ui.unit.Density

/**
 * The marker interface to be implemented by the root backing the composition. To be used in tests.
 */
interface RootForTest {
    /** Current device density. */
    val density: Density

    /** Semantics owner for this root. Manages all the semantics nodes. */
    val semanticsOwner: SemanticsOwner

    /** The service handling text input. */
    @Deprecated("Use PlatformTextInputModifierNode instead.") val textInputService: TextInputService

    /**
     * Send this [KeyEvent] to the focused component in this [Owner].
     *
     * @return true if the event was consumed. False otherwise.
     */
    fun sendKeyEvent(keyEvent: KeyEvent): Boolean

    /**
     * Send this [IndirectTouchEvent] to the focused component in this [Owner].
     *
     * @return true if the event was consumed. False otherwise.
     */
    @ExperimentalComposeUiApi
    fun sendIndirectTouchEvent(indirectTouchEvent: IndirectTouchEvent): Boolean = false

    /**
     * Force accessibility to be enabled for testing.
     *
     * @param enable force enable accessibility if true.
     */
    fun forceAccessibilityForTesting(enable: Boolean) {}

    /**
     * Set the time interval between sending accessibility events in milliseconds.
     *
     * This is the delay before dispatching a recurring accessibility event in milliseconds. It
     * delays the loop that sends events to the accessibility and content capture framework in
     * batches. A recurring event will be sent at most once during the [intervalMillis] timeframe.
     * The default time delay is 100 milliseconds.
     */
    fun setAccessibilityEventBatchIntervalMillis(intervalMillis: Long) {}

    /**
     * Requests another layout (measure + placement) pass be performed for any nodes that need it.
     * This doesn't force anything to be remeasured that wouldn't be if `requestLayout` were called.
     * However, unlike `requestLayout`, it doesn't merely _schedule_ another layout pass to be
     * performed, it actually performs it synchronously.
     *
     * This method is used in UI tests to perform layout in between frames when pumping frames as
     * fast as possible (i.e. without waiting for the choreographer to schedule them) in order to
     * get to idle, e.g. during a `waitForIdle` call.
     */
    fun measureAndLayoutForTest() {}

    /**
     * Sets the [UncaughtExceptionHandler] callback to dispatch layout, measure, and draw exceptions
     * from this Composition to. If this method is called multiple times, the previous callback is
     * discarded.
     *
     * The default test runners that ship with the Compose UI test and associated JUnit variation
     * use this API to reroute exceptions back to the test under execution. Custom test runners may
     * need to associate their own exception handler to do the same. If the Compose UI Test runner's
     * exception handler is overwritten, it may lead to unhandled exceptions crashing the
     * instrumented process and terminating the entire test suite early.
     */
    fun setUncaughtExceptionHandler(handler: UncaughtExceptionHandler?) {
        // Not implemented.
    }

    /**
     * An optional error handler that can be set to catch exceptions thrown during the layout, draw,
     * and teardown phases of the associated composition. If an exception is thrown to this
     * callback, the composition is already in an unrecoverable state and must be abandoned. Tests
     * may choose to catch exceptions to forward or process them differently. By default, no
     * exception handler is present and exceptions are thrown on the composer's thread, which may
     * cause the process to crash.
     *
     * This interface should generally not be used in production, and is intended for error routing
     * or introspection rather than true error recovery.
     */
    interface UncaughtExceptionHandler {
        /**
         * Invoked for testing infrastructure to be able to redirect an exception [t] that occurred
         * during the layout, measure, or draw phase of the underlying view. When this function is
         * invoked, the associated composition is in an unrecoverable state. The original exception
         * may be re-thrown to propagate it.
         *
         * If this callback swallows an exception to prevent a crash, you should expect other
         * follow-up exceptions to be directed here as more operations are executed on the
         * degenerate composition. The first exception passed to this callback is likely to be the
         * only useful exception.
         *
         * @param t The exception thrown by the composition hierarchy during the layout, measure, or
         *   draw phase of the associated view.
         */
        fun onUncaughtException(t: Throwable)
    }
}
