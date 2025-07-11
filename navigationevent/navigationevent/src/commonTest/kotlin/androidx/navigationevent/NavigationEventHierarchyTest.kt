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

package androidx.navigationevent

import androidx.kruth.assertThat
import androidx.navigationevent.testing.TestNavigationEvent
import androidx.navigationevent.testing.TestNavigationEventCallback
import kotlin.test.Test

class NavigationEventHierarchyTest {

    @Test
    fun init_whenChildIsCreatedWithParent_thenCallbacksAreSharedAndDispatched() {
        // Given a parent dispatcher and a callback for it
        val parentDispatcher = NavigationEventDispatcher()
        val parentCallback = TestNavigationEventCallback()
        parentDispatcher.addCallback(parentCallback)

        // When a child dispatcher is created with the parent and a callback is added to the child
        val childDispatcher = NavigationEventDispatcher(parentDispatcher)
        val childCallback = TestNavigationEventCallback()
        childDispatcher.addCallback(childCallback)

        // Then, dispatching an event from the parent should also trigger the child's callback,
        // indicating the shared processing.
        val event = TestNavigationEvent()
        parentDispatcher.dispatchOnStarted(event)

        assertThat(parentCallback.startedInvocations)
            .isEqualTo(0) // Assuming LIFO, parent callback is skipped
        assertThat(childCallback.startedInvocations).isEqualTo(1)
    }

    @Test
    fun init_whenChildIsCreatedWithNoParent_thenCallbacksAreIndependent() {
        // Given a parent dispatcher and a child dispatcher created without a parent
        val parentDispatcher = NavigationEventDispatcher()
        val childDispatcher = NavigationEventDispatcher()

        // And a callback for each
        val parentCallback = TestNavigationEventCallback()
        val childCallback = TestNavigationEventCallback()
        parentDispatcher.addCallback(parentCallback)
        childDispatcher.addCallback(childCallback)

        // When an event is dispatched through the parent
        val event = TestNavigationEvent()
        parentDispatcher.dispatchOnStarted(event)

        // Then only the parent's callback should be invoked, showing independent processing.
        assertThat(parentCallback.startedInvocations).isEqualTo(1)
        assertThat(childCallback.startedInvocations).isEqualTo(0)

        // When an event is dispatched through the child
        childDispatcher.dispatchOnStarted(event)

        // Then only the child's callback should be invoked, showing independent processing.
        assertThat(parentCallback.startedInvocations).isEqualTo(1)
        assertThat(childCallback.startedInvocations).isEqualTo(1)
    }

    @Test
    fun addCallback_whenCalledOnChild_thenCallbackIsDispatchedViaParent() {
        // Given a parent and child dispatcher
        val parentDispatcher = NavigationEventDispatcher()
        val childDispatcher = NavigationEventDispatcher(parentDispatcher)
        val callback = TestNavigationEventCallback()

        // When a new callback is added to the child
        childDispatcher.addCallback(callback)

        // Then dispatching an event from the parent should trigger the child's callback
        val event = TestNavigationEvent()
        parentDispatcher.dispatchOnStarted(event)
        assertThat(callback.startedInvocations).isEqualTo(1)
    }

    @Test
    fun addCallback_whenAddedToParentThenChild_thenCallbacksAreOrderedLIFO() {
        // Given a parent and child dispatcher
        val parentDispatcher = NavigationEventDispatcher()
        val childDispatcher = NavigationEventDispatcher(parentDispatcher)
        val parentCallback = TestNavigationEventCallback()
        val childCallback = TestNavigationEventCallback()

        // When a callback is added to the parent, then to the child
        parentDispatcher.addCallback(parentCallback)
        childDispatcher.addCallback(childCallback)

        // Then when an event is dispatched, the last-added callback (child's) should be invoked
        // first.
        val event = TestNavigationEvent()
        parentDispatcher.dispatchOnStarted(event)

        assertThat(parentCallback.startedInvocations).isEqualTo(0)
        assertThat(childCallback.startedInvocations).isEqualTo(1)
    }

    @Test
    fun addCallback_whenMultipleDispatchersAndCallbacksAdded_thenLastAddedCallbackIsInvokedFirst() {
        // Given a parent NavigationEventDispatcher and two child dispatchers.
        val parentDispatcher = NavigationEventDispatcher()
        val child1Dispatcher = NavigationEventDispatcher(parentDispatcher)
        val child2Dispatcher = NavigationEventDispatcher(parentDispatcher)

        // And three TestNavigationCallbacks: one for the parent and one for each child.
        val parentCallback = TestNavigationEventCallback()
        val childCallback1 = TestNavigationEventCallback()
        val childCallback2 = TestNavigationEventCallback()

        // When callbacks are added to the parent, then child2, then child1.
        parentDispatcher.addCallback(parentCallback)
        child2Dispatcher.addCallback(childCallback2)
        child1Dispatcher.addCallback(childCallback1)

        // Then, when an event is dispatched through the parent, only the most recently added active
        // callback (callbackC1 from child1) receives the event. This demonstrates that callbacks
        // are processed in a LIFO manner across the dispatcher hierarchy and that subsequent
        // callbacks are not invoked if an earlier one does not pass through.
        val event = TestNavigationEvent()
        parentDispatcher.dispatchOnStarted(event)

        assertThat(parentCallback.startedInvocations).isEqualTo(0)
        assertThat(childCallback2.startedInvocations).isEqualTo(0)
        assertThat(childCallback1.startedInvocations).isEqualTo(1)
    }

    @Test
    fun dispose_whenCalledOnChild_thenParentCallbackStillReceivesEvents() {
        // Given a parent and child, both with callbacks
        val parentDispatcher = NavigationEventDispatcher()
        val childDispatcher = NavigationEventDispatcher(parentDispatcher)
        val parentCallback = TestNavigationEventCallback()
        val childCallback = TestNavigationEventCallback()
        parentDispatcher.addCallback(parentCallback)
        childDispatcher.addCallback(childCallback)

        // When child is disposed
        childDispatcher.dispose()

        // Then dispatching an event from the parent should only trigger the parent's callback
        val event = TestNavigationEvent()
        parentDispatcher.dispatchOnStarted(event)
        assertThat(parentCallback.startedInvocations).isEqualTo(1)
        assertThat(childCallback.startedInvocations).isEqualTo(0)
    }

    @Test
    fun dispose_whenCalledOnParent_thenNoCallbacksReceiveEvents() {
        // Given a parent and child, both with callbacks
        val parentDispatcher = NavigationEventDispatcher()
        val childDispatcher = NavigationEventDispatcher(parentDispatcher)
        val parentCallback = TestNavigationEventCallback()
        val childCallback = TestNavigationEventCallback()
        parentDispatcher.addCallback(parentCallback)
        childDispatcher.addCallback(childCallback)

        // When parent is disposed
        parentDispatcher.dispose()

        // Then dispatching an event from either should result in no callbacks being invoked
        val event = TestNavigationEvent()
        parentDispatcher.dispatchOnStarted(event)
        childDispatcher.dispatchOnStarted(
            event
        ) // Attempting to dispatch from child, though its processor is now "gone"

        assertThat(parentCallback.startedInvocations).isEqualTo(0)
        assertThat(childCallback.startedInvocations).isEqualTo(0)
    }

    @Test
    fun dispose_whenCalledOnGrandparent_thenCascadesToAllDescendantsAndNoCallbacksReceiveEvents() {
        // Given a three-level hierarchy, each with a callback
        val grandparentDispatcher = NavigationEventDispatcher()
        val parentDispatcher = NavigationEventDispatcher(grandparentDispatcher)
        val childDispatcher = NavigationEventDispatcher(parentDispatcher)
        val grandparentCallback = TestNavigationEventCallback()
        val parentCallback = TestNavigationEventCallback()
        val childCallback = TestNavigationEventCallback()

        grandparentDispatcher.addCallback(grandparentCallback)
        parentDispatcher.addCallback(parentCallback)
        childDispatcher.addCallback(childCallback)

        // When the grandparent is disposed
        grandparentDispatcher.dispose()

        // Then dispatching an event from any level should result in no callbacks being invoked
        val event = TestNavigationEvent()
        grandparentDispatcher.dispatchOnStarted(event)
        parentDispatcher.dispatchOnStarted(event)
        childDispatcher.dispatchOnStarted(event)

        assertThat(grandparentCallback.startedInvocations).isEqualTo(0)
        assertThat(parentCallback.startedInvocations).isEqualTo(0)
        assertThat(childCallback.startedInvocations).isEqualTo(0)
    }

    @Test
    fun isEnabled_whenSetToTrue_thenEventsAreDispatched() {
        // Given a dispatcher
        val dispatcher = NavigationEventDispatcher()
        val callback = TestNavigationEventCallback()
        dispatcher.addCallback(callback)

        // When the dispatcher is enabled
        dispatcher.isEnabled = true

        // Then dispatching an event should trigger the callback
        val event = TestNavigationEvent()
        dispatcher.dispatchOnStarted(event)
        assertThat(callback.startedInvocations).isEqualTo(1)
    }

    @Test
    fun isEnabled_whenSetToFalse_thenNoCallbacksAreDispatched() {
        // Given a dispatcher with an enabled callback
        val dispatcher = NavigationEventDispatcher()
        val callback = TestNavigationEventCallback(isEnabled = true)
        dispatcher.addCallback(callback)

        // When the dispatcher is disabled
        dispatcher.isEnabled = false

        // Then dispatching an event should not trigger the callback
        val event = TestNavigationEvent()
        dispatcher.dispatchOnStarted(event)
        assertThat(callback.startedInvocations).isEqualTo(0)
    }

    @Test
    fun isEnabled_whenParentIsDisabled_thenChildDoesNotDispatchEvents() {
        // Given a parent and child dispatcher, both with callbacks
        val parentDispatcher = NavigationEventDispatcher()
        val childDispatcher = NavigationEventDispatcher(parentDispatcher)
        val parentCallback = TestNavigationEventCallback()
        val childCallback = TestNavigationEventCallback()
        parentDispatcher.addCallback(parentCallback)
        childDispatcher.addCallback(childCallback)

        // When the parent is disabled
        parentDispatcher.isEnabled = false

        // Then dispatching an event from the child should not invoke any callbacks,
        // because the parent's disabled state propagates.
        val event = TestNavigationEvent()
        childDispatcher.dispatchOnStarted(event)

        assertThat(parentCallback.startedInvocations).isEqualTo(0)
        assertThat(childCallback.startedInvocations).isEqualTo(0)
    }

    @Test
    fun isEnabled_whenChildIsLocallyDisabled_thenChildDoesNotDispatchEvents() {
        // Given a parent (enabled) and child, both with callbacks
        val parentDispatcher = NavigationEventDispatcher()
        val childDispatcher = NavigationEventDispatcher(parentDispatcher)
        val parentCallback = TestNavigationEventCallback()
        val childCallback = TestNavigationEventCallback()
        parentDispatcher.addCallback(parentCallback)
        childDispatcher.addCallback(childCallback)

        // When child is locally disabled
        childDispatcher.isEnabled = false

        // Then dispatching an event from the child should not trigger its callback.
        // The parent's callback should still be invokable via the parent directly.
        val event = TestNavigationEvent()
        childDispatcher.dispatchOnStarted(event)

        assertThat(childCallback.startedInvocations).isEqualTo(0)
        assertThat(parentCallback.startedInvocations)
            .isEqualTo(0) // Parent's callback should still fire via parent
    }

    @Test
    fun isEnabled_whenChildIsLocallyDisabled_thenChildCallbacksDoesNotReceiveEvents() {
        // Given a parent (enabled) and child, both with callbacks
        val parentDispatcher = NavigationEventDispatcher()
        val childDispatcher = NavigationEventDispatcher(parentDispatcher)
        val parentCallback = TestNavigationEventCallback()
        val childCallback = TestNavigationEventCallback()
        parentDispatcher.addCallback(parentCallback)
        childDispatcher.addCallback(childCallback)

        // When child is locally disabled
        childDispatcher.isEnabled = false

        // Then dispatching an event from the child should not trigger its callback.
        // The parent's callback should still be invokable via the parent directly.
        val event = TestNavigationEvent()
        parentDispatcher.dispatchOnStarted(event) // Confirm parent is still active

        assertThat(childCallback.startedInvocations).isEqualTo(0)
        assertThat(parentCallback.startedInvocations)
            .isEqualTo(1) // Parent's callback should still fire via parent
    }

    @Test
    fun isEnabled_whenDisabledParentIsReEnabled_thenChildDispatchesEventsAgain() {
        // Given a disabled parent and a locally-enabled child, both with callbacks
        val parentDispatcher = NavigationEventDispatcher()
        val childDispatcher = NavigationEventDispatcher(parentDispatcher)
        val parentCallback = TestNavigationEventCallback()
        val childCallback = TestNavigationEventCallback()
        parentDispatcher.addCallback(parentCallback)
        childDispatcher.addCallback(childCallback)

        parentDispatcher.isEnabled = false // Initial state: parent (and thus child) disabled
        // Verify pre-condition (no dispatch before re-enabling)
        val initialEvent = TestNavigationEvent()
        childDispatcher.dispatchOnStarted(initialEvent)
        assertThat(childCallback.startedInvocations).isEqualTo(0)

        // When the parent is re-enabled
        parentDispatcher.isEnabled = true

        // Then the child should now dispatch events
        val reEnabledEvent = TestNavigationEvent()
        childDispatcher.dispatchOnStarted(reEnabledEvent)
        assertThat(childCallback.startedInvocations).isEqualTo(1)
        assertThat(parentCallback.startedInvocations)
            .isEqualTo(0) // Parent's callback is still LIFO behind child
    }

    @Test
    fun isEnabled_whenDisabledParentIsReEnabled_thenChildCallbacksReceiveEventsAgain() {
        // Given a disabled parent and a locally-enabled child, both with callbacks
        val parentDispatcher = NavigationEventDispatcher()
        val childDispatcher = NavigationEventDispatcher(parentDispatcher)
        val parentCallback = TestNavigationEventCallback()
        val childCallback = TestNavigationEventCallback()
        parentDispatcher.addCallback(parentCallback)
        childDispatcher.addCallback(childCallback)

        parentDispatcher.isEnabled = false // Initial state: parent (and thus child) disabled
        // Verify pre-condition (no dispatch before re-enabling)
        val initialEvent = TestNavigationEvent()
        parentDispatcher.dispatchOnStarted(initialEvent)
        assertThat(parentCallback.startedInvocations).isEqualTo(0)
        assertThat(childCallback.startedInvocations).isEqualTo(0)

        // When the parent is re-enabled
        parentDispatcher.isEnabled = true

        // Then the child should now dispatch events
        val reEnabledEvent = TestNavigationEvent()
        parentDispatcher.dispatchOnStarted(reEnabledEvent)
        assertThat(parentCallback.startedInvocations)
            .isEqualTo(0) // Parent's callback is still LIFO behind child
        assertThat(childCallback.startedInvocations).isEqualTo(1)
    }

    @Test
    fun isEnabled_whenGrandparentIsDisabled_thenGrandchildDoesNotDispatchEvents() {
        // Given a three-level hierarchy, each with a callback
        val grandparentDispatcher = NavigationEventDispatcher()
        val parentDispatcher = NavigationEventDispatcher(grandparentDispatcher)
        val childDispatcher = NavigationEventDispatcher(parentDispatcher)
        val grandparentCallback = TestNavigationEventCallback()
        val parentCallback = TestNavigationEventCallback()
        val childCallback = TestNavigationEventCallback()

        grandparentDispatcher.addCallback(grandparentCallback)
        parentDispatcher.addCallback(parentCallback)
        childDispatcher.addCallback(childCallback)

        // When the grandparent is disabled
        grandparentDispatcher.isEnabled = false

        // Then dispatching an event from the grandchild should result in no callbacks being
        // invoked, as the disabled state cascades down.
        val event = TestNavigationEvent()
        childDispatcher.dispatchOnStarted(event)

        assertThat(grandparentCallback.startedInvocations).isEqualTo(0)
        assertThat(parentCallback.startedInvocations).isEqualTo(0)
        assertThat(childCallback.startedInvocations).isEqualTo(0)
    }

    @Test
    fun isEnabled_whenGrandparentIsDisabled_thenGrandchildCallbackDoesNotReceiveEvents() {
        // Given a three-level hierarchy, each with a callback
        val grandparentDispatcher = NavigationEventDispatcher()
        val parentDispatcher = NavigationEventDispatcher(grandparentDispatcher)
        val childDispatcher = NavigationEventDispatcher(parentDispatcher)
        val grandparentCallback = TestNavigationEventCallback()
        val parentCallback = TestNavigationEventCallback()
        val childCallback = TestNavigationEventCallback()

        grandparentDispatcher.addCallback(grandparentCallback)
        parentDispatcher.addCallback(parentCallback)
        childDispatcher.addCallback(childCallback)

        // When the grandparent is disabled
        grandparentDispatcher.isEnabled = false

        // Then dispatching an event from the grandparent should result in no callbacks being
        // invoked, as the disabled state cascades down.
        val event = TestNavigationEvent()
        grandparentDispatcher.dispatchOnStarted(event)

        assertThat(grandparentCallback.startedInvocations).isEqualTo(0)
        assertThat(parentCallback.startedInvocations).isEqualTo(0)
        assertThat(childCallback.startedInvocations).isEqualTo(0)
    }

    @Test
    fun callbackIsEnabled_whenItsDispatcherIsDisabled_thenCallbackDoesNotReceiveEvents() {
        // Given a dispatcher and an enabled TestNavigationCallback
        val dispatcher = NavigationEventDispatcher()
        val callback = TestNavigationEventCallback()
        dispatcher.addCallback(callback)
        // Ensure callback is initially enabled
        val preDisableEvent = TestNavigationEvent()
        dispatcher.dispatchOnStarted(preDisableEvent)
        assertThat(callback.startedInvocations).isEqualTo(1)

        // When the dispatcher associated with the callback is disabled
        dispatcher.isEnabled = false

        // Then dispatching an event (even if the callback's local isEnabled is true)
        // should not trigger the callback because its dispatcher is disabled.
        val event = TestNavigationEvent()
        dispatcher.dispatchOnStarted(event)

        assertThat(callback.startedInvocations).isEqualTo(1)
    }

    @Test
    fun callbackIsEnabled_whenItsDispatcherIsReEnabled_thenCallbackReceivesEventsAgain() {
        // Given a dispatcher and an enabled TestNavigationCallback, and the dispatcher is disabled
        val dispatcher = NavigationEventDispatcher()
        val callback = TestNavigationEventCallback()
        dispatcher.addCallback(callback)
        dispatcher.isEnabled = false // Disable dispatcher

        // Pre-condition: Callback does not receive events when dispatcher is disabled
        val preEnableEvent = TestNavigationEvent()
        dispatcher.dispatchOnStarted(preEnableEvent)
        assertThat(callback.startedInvocations).isEqualTo(0)

        // When the dispatcher associated with the callback is re-enabled
        dispatcher.isEnabled = true

        // Then dispatching an event should now trigger the callback
        val reEnabledEvent = TestNavigationEvent()
        dispatcher.dispatchOnStarted(reEnabledEvent)

        assertThat(callback.startedInvocations).isEqualTo(1)
    }
}
