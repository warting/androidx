/*
 * Copyright 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.navigationevent

import androidx.kruth.assertThat
import androidx.kruth.assertThrows
import kotlin.test.Test

class NavigationEventDispatcherTest {
    @Test
    fun removeCallback_whenNavigationIsInProgress_thenOnCancelledIsSent() {
        val dispatcher = NavigationEventDispatcher()

        // We need to capture the state when onEventCancelled is called to verify the order.
        var startedInvocationsAtCancelTime = 0
        val callback =
            TestNavigationEventCallback(
                onEventCancelled = {
                    // Capture the count of 'started' invocations when 'cancelled' is called.
                    startedInvocationsAtCancelTime = this.startedInvocations
                }
            )
        dispatcher.addCallback(callback)

        dispatcher.dispatchOnStarted(TestNavigationEvent())
        // Sanity check that navigation has started.
        assertThat(callback.startedInvocations).isEqualTo(1)

        callback.remove()

        // Assert that onEventCancelled was called once, and it happened after onEventStarted.
        assertThat(callback.cancelledInvocations).isEqualTo(1)
        assertThat(startedInvocationsAtCancelTime).isEqualTo(1)
    }

    @Test
    fun dispatch_whenCallbackDisablesItself_thenOnCancelledIsNotSent() {
        val dispatcher = NavigationEventDispatcher()
        val callback = TestNavigationEventCallback(onEventStarted = { isEnabled = false })
        dispatcher.addCallback(callback)

        dispatcher.dispatchOnStarted(TestNavigationEvent())
        dispatcher.dispatchOnCompleted()

        // The callback was disabled, but cancellation should not be triggered.
        // The 'completed' event should still be received because the navigation was in progress.
        assertThat(callback.startedInvocations).isEqualTo(1)
        assertThat(callback.cancelledInvocations).isEqualTo(0)
        assertThat(callback.completedInvocations).isEqualTo(1)
    }

    @Test
    fun setEnabled_whenNavigationIsInProgress_thenOnCancelledIsNotSent() {
        val dispatcher = NavigationEventDispatcher()
        val callback = TestNavigationEventCallback()
        dispatcher.addCallback(callback)

        dispatcher.dispatchOnStarted(TestNavigationEvent())
        assertThat(callback.startedInvocations).isEqualTo(1)

        callback.isEnabled = false

        // Assert that disabling the callback does not trigger a cancellation.
        assertThat(callback.cancelledInvocations).isEqualTo(0)
    }

    @Test
    fun dispatch_whenCallbackRemovesItselfOnStarted_thenOnCancelledIsSent() {
        val dispatcher = NavigationEventDispatcher()
        var cancelledInvocationsAtStartTime = 0
        val callback =
            TestNavigationEventCallback(
                onEventStarted = {
                    // Capture the 'cancelled' count before removing to ensure it was 0.
                    cancelledInvocationsAtStartTime = this.cancelledInvocations
                    remove()
                }
            )
        dispatcher.addCallback(callback)

        dispatcher.dispatchOnStarted(TestNavigationEvent())

        // Assert that 'onEventStarted' was called.
        assertThat(callback.startedInvocations).isEqualTo(1)
        // Assert that 'onEventCancelled' was called from within 'onEventStarted'.
        assertThat(callback.cancelledInvocations).isEqualTo(1)
        // Assert that 'onEventCancelled' had not been called before 'remove()'.
        assertThat(cancelledInvocationsAtStartTime).isEqualTo(0)
    }

    @Test
    fun dispatch_whenAnotherNavigationIsInProgress_thenPreviousIsCancelled() {
        val dispatcher = NavigationEventDispatcher()

        val callback1 = TestNavigationEventCallback()
        dispatcher.addCallback(callback1)

        // Start the first navigation.
        dispatcher.dispatchOnStarted(TestNavigationEvent())
        assertThat(callback1.startedInvocations).isEqualTo(1)

        val callback2 = TestNavigationEventCallback()
        dispatcher.addCallback(callback2)

        // Start the second navigation, which should cancel the first.
        dispatcher.dispatchOnStarted(TestNavigationEvent())

        // Assert callback1 was cancelled and callback2 was started.
        assertThat(callback1.cancelledInvocations).isEqualTo(1)
        assertThat(callback2.startedInvocations).isEqualTo(1)

        // Complete the second navigation.
        dispatcher.dispatchOnCompleted()
        assertThat(callback2.completedInvocations).isEqualTo(1)

        // Ensure callback1 was not affected by the completion of the second navigation.
        assertThat(callback1.completedInvocations).isEqualTo(0)
    }

    @Test
    fun addCallback_whenNavigationIsInProgress_thenNewCallbackIsIgnoredForCurrentNavigation() {
        val dispatcher = NavigationEventDispatcher()

        val callback1 = TestNavigationEventCallback()
        dispatcher.addCallback(callback1)
        dispatcher.dispatchOnStarted(TestNavigationEvent())
        assertThat(callback1.startedInvocations).isEqualTo(1)

        // Add a second callback while the first navigation is in progress.
        val callback2 = TestNavigationEventCallback()
        dispatcher.addCallback(callback2)

        // Complete the first navigation.
        dispatcher.dispatchOnCompleted()

        // Assert that only the first callback was affected.
        assertThat(callback1.completedInvocations).isEqualTo(1)
        assertThat(callback2.startedInvocations).isEqualTo(0)
        assertThat(callback2.completedInvocations).isEqualTo(0)

        // Start and complete a second navigation.
        dispatcher.dispatchOnStarted(TestNavigationEvent())
        dispatcher.dispatchOnCompleted()

        // Assert that the second navigation was handled by the new top callback (callback2).
        assertThat(callback1.startedInvocations).isEqualTo(1) // Unchanged
        assertThat(callback1.completedInvocations).isEqualTo(1) // Unchanged
        assertThat(callback2.startedInvocations).isEqualTo(1)
        assertThat(callback2.completedInvocations).isEqualTo(1)
    }

    @Test
    fun dispatch_whenNoEnabledCallbacksExist_thenFallbackIsInvoked() {
        var fallbackCalled = false
        val dispatcher =
            NavigationEventDispatcher(fallbackOnBackPressed = { fallbackCalled = true })
        val callback = TestNavigationEventCallback()
        dispatcher.addCallback(callback)

        dispatcher.dispatchOnCompleted()
        assertThat(callback.completedInvocations).isEqualTo(1)
        assertThat(fallbackCalled).isFalse()

        // After disabling the only callback, the fallback should be called.
        callback.isEnabled = false
        dispatcher.dispatchOnCompleted()
        assertThat(callback.completedInvocations).isEqualTo(1) // Unchanged
        assertThat(fallbackCalled).isTrue()
    }

    @Test
    fun dispatch_whenAllEnabledCallbacksArePassThrough_thenFallbackIsNotInvoked() {
        var fallbackCalled = false
        val dispatcher =
            NavigationEventDispatcher(fallbackOnBackPressed = { fallbackCalled = true })
        val callback1 = TestNavigationEventCallback(isPassThrough = true)
        val callback2 = TestNavigationEventCallback(isPassThrough = true)
        dispatcher.addCallback(callback1)
        dispatcher.addCallback(callback2)

        dispatcher.dispatchOnCompleted()

        assertThat(callback1.completedInvocations).isEqualTo(1)
        assertThat(callback2.completedInvocations).isEqualTo(1)
        assertThat(fallbackCalled).isFalse()
    }

    @Test
    fun dispatch_whenOverlayCallbackExists_thenOverlaySupersedesDefault() {
        val dispatcher = NavigationEventDispatcher()
        val overlayCallback = TestNavigationEventCallback()
        val normalCallback = TestNavigationEventCallback()

        dispatcher.addCallback(overlayCallback, NavigationEventPriority.Overlay)
        dispatcher.addCallback(normalCallback, NavigationEventPriority.Default)

        dispatcher.dispatchOnCompleted()

        // The overlay callback should handle the event, and the normal one should not.
        assertThat(overlayCallback.completedInvocations).isEqualTo(1)
        assertThat(normalCallback.completedInvocations).isEqualTo(0)
    }

    @Test
    fun dispatch_whenPassThroughOverlayCallbackExists_thenBothCallbacksAreInvoked() {
        val dispatcher = NavigationEventDispatcher()
        val overlayCallback = TestNavigationEventCallback(isPassThrough = true)
        val normalCallback = TestNavigationEventCallback()

        dispatcher.addCallback(normalCallback, NavigationEventPriority.Default)
        dispatcher.addCallback(overlayCallback, NavigationEventPriority.Overlay)

        dispatcher.dispatchOnCompleted()

        // Both callbacks should be invoked because the overlay callback is pass-through.
        assertThat(overlayCallback.completedInvocations).isEqualTo(1)
        assertThat(normalCallback.completedInvocations).isEqualTo(1)
    }

    @Test
    fun addCallback_whenCallbackIsRegisteredWithAnotherDispatcher_thenThrowsException() {
        val callback = TestNavigationEventCallback()
        val dispatcher1 = NavigationEventDispatcher()
        dispatcher1.addCallback(callback)

        val dispatcher2 = NavigationEventDispatcher()
        assertThrows<IllegalArgumentException> { dispatcher2.addCallback(callback) }
            .hasMessageThat()
            .contains("is already registered with a dispatcher")
    }

    @Test
    fun addCallback_whenCallbackIsAlreadyRegistered_thenThrowsException() {
        val callback = TestNavigationEventCallback()
        val dispatcher = NavigationEventDispatcher()
        dispatcher.addCallback(callback)

        assertThrows<IllegalArgumentException> { dispatcher.addCallback(callback) }
            .hasMessageThat()
            .contains("is already registered with a dispatcher")
    }

    @Test
    fun dispatch_whenCallbackIsRemovedDuringDispatch_thenDoesNotThrowException() {
        val dispatcher = NavigationEventDispatcher()

        val callback1 = TestNavigationEventCallback(isPassThrough = true)
        val callback2 = TestNavigationEventCallback(isPassThrough = true)
        val callback3 =
            TestNavigationEventCallback(
                isPassThrough = true,
                // The important part of this test is that removing a callback during dispatch
                // does not cause a crash.
                onEventProgressed = { remove() },
            )
        dispatcher.addCallback(callback1)
        dispatcher.addCallback(callback2)
        dispatcher.addCallback(callback3)

        val event = TestNavigationEvent()
        dispatcher.dispatchOnStarted(event)
        // This should not throw a ConcurrentModificationException.
        dispatcher.dispatchOnProgressed(event)

        // All 3 callbacks should have started.
        assertThat(callback1.startedInvocations).isEqualTo(1)
        assertThat(callback2.startedInvocations).isEqualTo(1)
        assertThat(callback3.startedInvocations).isEqualTo(1)

        // All 3 should have also received the progress event, even though one removed itself.
        assertThat(callback1.progressedInvocations).isEqualTo(1)
        assertThat(callback2.progressedInvocations).isEqualTo(1)
        assertThat(callback3.progressedInvocations).isEqualTo(1)
    }
}
