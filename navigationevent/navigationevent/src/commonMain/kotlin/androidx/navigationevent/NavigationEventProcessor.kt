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

import androidx.annotation.MainThread
import androidx.navigationevent.NavigationEventInfo.NotProvided
import androidx.navigationevent.NavigationEventPriority.Companion.Default
import androidx.navigationevent.NavigationEventPriority.Companion.Overlay
import androidx.navigationevent.NavigationEventState.Idle
import androidx.navigationevent.NavigationEventState.InProgress
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Manages the lifecycle and dispatching of [NavigationEventCallback] instances across all
 * NavigationEventDispatcher instances. This class ensures consistent ordering, state management,
 * and prioritized dispatch for navigation events.
 */
internal class NavigationEventProcessor {

    /** The private, mutable source of truth for the navigation state. */
    internal val _state = MutableStateFlow<NavigationEventState<*>>(Idle(NotProvided))

    /**
     * The [StateFlow] from the highest-priority, enabled navigation callback.
     *
     * This represents the navigation state of the currently active component.
     */
    val state: StateFlow<NavigationEventState<*>> = _state.asStateFlow()

    /**
     * Stores high-priority callbacks that should be evaluated before default callbacks.
     *
     * `ArrayDeque` is used for efficient `addFirst()` and `remove()` operations, which is ideal for
     * maintaining a Last-In, First-Out (LIFO) dispatch order. This means the most recently added
     * overlay callback is the first to be checked.
     *
     * @see [defaultCallbacks]
     * @see [inProgressCallback]
     */
    private val overlayCallbacks = ArrayDeque<NavigationEventCallback<*>>()

    /**
     * Stores standard-priority callbacks.
     *
     * Like `overlayCallbacks`, this uses `ArrayDeque` to efficiently manage a LIFO queue, ensuring
     * the most recently added default callback is checked first within its priority level.
     *
     * @see [overlayCallbacks]
     * @see [inProgressCallback]
     */
    private val defaultCallbacks = ArrayDeque<NavigationEventCallback<*>>()

    /**
     * The callback for a navigation event that is currently in progress.
     *
     * This callback has the highest dispatch priority, ensuring that terminal events (like
     * [dispatchOnCompleted] or [dispatchOnCancelled]) are delivered only to the participant of the
     * active navigation. This is cleared after the event is terminated.
     *
     * Notably, if this callback is removed while an event is in progress, it is implicitly treated
     * as a terminal event and receives a cancellation call before being removed.
     *
     * @see [overlayCallbacks]
     * @see [defaultCallbacks]
     */
    private var inProgressCallback: NavigationEventCallback<*>? = null

    /**
     * A central registry of all active [NavigationEventInput] instances associated with this
     * processor.
     *
     * This set is managed by the [NavigationEventDispatcher] and allows the processor to
     * communicate global state changes—such as whether any callbacks are enabled—to all relevant
     * input sources.
     *
     * It is not intended for direct public use and is exposed internally for the dispatcher.
     */
    val inputs = mutableSetOf<NavigationEventInput>()

    /**
     * Represents whether there is at least one enabled callback registered across all dispatchers.
     *
     * This property serves as a global flag that input handlers can observe to enable or disable
     * system back gestures. For example, on Android, this would control `OnBackInvokedDispatcher.`
     * `OnBackPressedDispatcher.setEnabled()`.
     *
     * It is updated automatically when callbacks are added, removed, or their enabled state
     * changes. When its value changes, it notifies all registered [NavigationEventInput] instances.
     */
    private var hasEnabledCallbacks: Boolean = false
        set(value) {
            // Only proceed if the enabled state is actually changing to avoid redundant work.
            if (field == value) return

            field = value
            for (input in inputs) {
                input.doOnHasEnabledCallbacksChanged(hasEnabledCallbacks = value)
            }
        }

    /**
     * Recomputes and updates [hasEnabledCallbacks] based on the enabled status of all registered
     * callbacks. This should be called whenever a callback’s enabled state or its registration
     * status (added or removed) changes.
     */
    fun updateEnabledCallbacks() {
        // `any` and `||` are efficient as they short-circuit on the first `true` result.
        hasEnabledCallbacks =
            overlayCallbacks.any { it.isBackEnabled } || defaultCallbacks.any { it.isBackEnabled }

        // Whenever the set of enabled callbacks changes, we must immediately
        // synchronize the global navigation state. This picks the new highest-priority
        // active callback and updates the state to reflect its info, preventing stale data.
        val enabledCallback = resolveEnabledCallback()
        if (enabledCallback != null) {
            updateEnabledCallbackState(enabledCallback)
        }
    }

    /**
     * Called by a NavigationEventCallback when its info changes via `setInfo`.
     *
     * This method centralizes the state update logic. It checks if the callback that changed is the
     * authoritative one (either the `inProgressCallback` or the highest-priority idle callback)
     * before updating the shared `_state`. This prevents lower-priority callbacks from incorrectly
     * overwriting the state.
     */
    internal fun updateEnabledCallbackState(callback: NavigationEventCallback<*>) {
        val currentCallback = inProgressCallback ?: resolveEnabledCallback()
        if (currentCallback != callback) return

        _state.update { state ->
            when (state) {
                is Idle -> Idle(callback.currentInfo ?: NotProvided)
                is InProgress ->
                    InProgress(
                        currentInfo = callback.currentInfo ?: NotProvided,
                        previousInfo = callback.previousInfo,
                        latestEvent = state.latestEvent,
                    )
            }
        }
    }

    /**
     * Returns `true` if there is at least one [NavigationEventCallback.isBackEnabled] callback
     * registered globally within this processor.
     *
     * @return `true` if any callback is enabled, `false` otherwise.
     */
    fun hasEnabledCallbacks(): Boolean = hasEnabledCallbacks

    /**
     * Checks if there are any registered callbacks, either overlay or normal.
     *
     * @return `true` if there is at least one overlay callback or one normal callback registered,
     *   `false` otherwise.
     */
    fun hasCallbacks(): Boolean = overlayCallbacks.isNotEmpty() || defaultCallbacks.isNotEmpty()

    /**
     * Adds a new [NavigationEventCallback] to receive navigation events, associating it with its
     * [NavigationEventDispatcher].
     *
     * Callbacks are placed into priority-specific queues ([Overlay] or [Default]) and within those
     * queues, they are ordered in Last-In, First-Out (LIFO) manner. This ensures that the most
     * recently added callback of a given priority is considered first.
     *
     * All callbacks are invoked on the main thread. To stop receiving events, a callback must be
     * removed via [NavigationEventCallback.remove].
     *
     * @param dispatcher The [NavigationEventDispatcher] instance registering this callback. This
     *   link is stored on the callback itself to enable self-removal and state tracking.
     * @param callback The callback instance to be added.
     * @param priority The priority of the callback, determining its invocation order relative to
     *   others. See [NavigationEventPriority].
     * @throws IllegalArgumentException if the given callback is already registered with a different
     *   dispatcher.
     */
    @Suppress("PairedRegistration") // Callback is removed via `NavigationEventCallback.remove()`
    @MainThread
    fun addCallback(
        dispatcher: NavigationEventDispatcher,
        callback: NavigationEventCallback<*>,
        priority: NavigationEventPriority = Default,
    ) {
        // Enforce that a callback is not already registered with another dispatcher.
        require(callback.dispatcher == null) {
            "Callback '$callback' is already registered with a dispatcher"
        }

        // Add to the front of the appropriate queue to achieve LIFO ordering.
        when (priority) {
            Overlay -> overlayCallbacks.addFirst(callback)
            Default -> defaultCallbacks.addFirst(callback)
        }

        // Store the dispatcher reference on the callback for self-management and internal tracking.
        callback.dispatcher = dispatcher
        updateEnabledCallbacks()
    }

    /**
     * Removes a [NavigationEventCallback] from the processor's registry.
     *
     * If the callback is currently part of an active event (i.e., it is the `inProgressCallback`),
     * it will be notified of cancellation before being removed. This method is idempotent and can
     * be called safely even if the callback is not currently registered.
     *
     * @param callback The [NavigationEventCallback] to remove.
     */
    @MainThread
    fun removeCallback(callback: NavigationEventCallback<*>) {
        // If the callback is the one currently being processed, it needs to be notified of
        // cancellation and then cleared from the in-progress state.
        if (callback == inProgressCallback) {
            callback.doOnBackCancelled()
            inProgressCallback = null
        }

        // The `remove()` operation on ArrayDeque is efficient and simply returns `false` if the
        // element is not found. There's no need for a preceding `contains()` check.
        overlayCallbacks.remove(callback)
        defaultCallbacks.remove(callback)

        // Clear the dispatcher reference to mark the callback as unregistered and available for
        // re-registration.
        callback.dispatcher = null
        updateEnabledCallbacks()
    }

    /**
     * Dispatches an [NavigationEventCallback.onBackStarted] event with the given event to the
     * highest-priority enabled callback.
     *
     * If an event is currently in progress, it will be cancelled first to ensure a clean state for
     * the new event. Only the single, highest-priority enabled callback is notified and becomes the
     * `inProgressCallback`.
     *
     * @param input The [NavigationEventInput] that sourced this event.
     * @param direction The direction of the navigation event being started.
     * @param event [NavigationEvent] to dispatch to the callback.
     */
    @MainThread
    fun dispatchOnStarted(
        input: NavigationEventInput,
        direction: NavigationEventDirection,
        event: NavigationEvent,
    ) {
        // TODO(mgalhardo): Update sharedProcessor to use input to distinguish events.
        // TODO(mgalhardo): Update the sharedProcessor to handle NavigationEventDirection.

        if (inProgressCallback != null) {
            // It's important to ensure that any ongoing operations from previous events are
            // properly cancelled before starting new ones to maintain a consistent state.
            dispatchOnCancelled(input, direction)
        }

        // Find the highest-priority enabled callback to handle this event.
        val callback = resolveEnabledCallback()
        if (callback != null) {
            // Set this callback as the one in progress *before* execution. This ensures
            // `onCancelled` can be correctly handled if the callback removes itself during
            // `onEventStarted`.
            inProgressCallback = callback
            callback.doOnBackStarted(event)
            _state.update {
                InProgress(callback.currentInfo ?: NotProvided, callback.previousInfo, event)
            }
        }
    }

    /**
     * Dispatches an [NavigationEventCallback.onBackProgressed] event with the given event.
     *
     * If a callback is currently in progress (from a [dispatchOnStarted] call), only that callback
     * will be notified. Otherwise, the highest-priority enabled callback will receive the progress
     * event. This is not a terminal event, so `inProgressCallback` is not cleared.
     *
     * @param input The [NavigationEventInput] that sourced this event.
     * @param direction The direction of the navigation event being started.
     * @param event [NavigationEvent] to dispatch to the callback.
     */
    @MainThread
    fun dispatchOnProgressed(
        input: NavigationEventInput,
        direction: NavigationEventDirection,
        event: NavigationEvent,
    ) {
        // TODO(mgalhardo): Update sharedProcessor to use input to distinguish events.
        // TODO(mgalhardo): Update the sharedProcessor to handle NavigationEventDirection.

        // If there is a callback in progress, only that one is notified.
        // Otherwise, the highest-priority enabled callback is notified.
        val callback = inProgressCallback ?: resolveEnabledCallback()
        // Progressed is not a terminal event, so `inProgressCallback` is not cleared.

        if (callback != null) {
            callback.doOnBackProgressed(event)
            _state.update {
                InProgress(callback.currentInfo ?: NotProvided, callback.previousInfo, event)
            }
        }
    }

    /**
     * Dispatches an [NavigationEventCallback.onBackCompleted] event.
     *
     * If a callback is currently in progress, only it will be notified. Otherwise, the
     * highest-priority enabled callback will be notified. This is a terminal event, clearing the
     * `inProgressCallback`. If no callback handles the event, the `fallbackOnBackPressed` action is
     * invoked.
     *
     * @param input The [NavigationEventInput] that sourced this event.
     * @param direction The direction of the navigation event being started.
     * @param fallbackOnBackPressed The action to invoke if no callback handles the completion.
     */
    @MainThread
    fun dispatchOnCompleted(
        input: NavigationEventInput,
        direction: NavigationEventDirection,
        fallbackOnBackPressed: (() -> Unit)?,
    ) {
        // TODO(mgalhardo): Update sharedProcessor to use input to distinguish events.
        // TODO(mgalhardo): Update the sharedProcessor to handle NavigationEventDirection.

        // If there is a callback in progress, only that one is notified.
        // Otherwise, the highest-priority enabled callback is notified.
        val callback = inProgressCallback ?: resolveEnabledCallback()
        inProgressCallback = null // Clear in-progress, as 'completed' is a terminal event.

        // If no callback is notified, use the fallback.
        if (callback == null) {
            fallbackOnBackPressed?.invoke()
        } else {
            callback.doOnBackCompleted()
            _state.update { Idle(callback.currentInfo ?: NotProvided) }
        }
    }

    /**
     * Dispatches an [NavigationEventCallback.onBackCancelled] event.
     *
     * If a callback is currently in progress, only it will be notified. Otherwise, the
     * highest-priority enabled callback will be notified. This is a terminal event, clearing the
     * `inProgressCallback`.
     *
     * @param input The [NavigationEventInput] that sourced this event.
     * @param direction The direction of the navigation event being started.
     */
    @MainThread
    fun dispatchOnCancelled(input: NavigationEventInput, direction: NavigationEventDirection) {
        // TODO(mgalhardo): Update sharedProcessor to use input to distinguish events.
        // TODO(mgalhardo): Update the sharedProcessor to handle NavigationEventDirection.

        // If there is a callback in progress, only that one is notified.
        // Otherwise, the highest-priority enabled callback is notified.
        val callback = inProgressCallback ?: resolveEnabledCallback()
        inProgressCallback = null // Clear in-progress, as 'cancelled' is a terminal event.

        if (callback != null) {
            callback.doOnBackCancelled()
            _state.update { Idle(callback.currentInfo ?: NotProvided) }
        }
    }

    /**
     * Resolves which callback should handle a navigation event based on priority and enabled state.
     *
     * This function is the core of the priority dispatch system. It ensures that only one callback
     * is selected to receive an event by strictly enforcing dispatch order. The resolution process
     * is:
     * 1. It first scans **overlay** callbacks, from most-to-least recently added.
     * 2. If no enabled overlay callback is found, it then scans **default** callbacks in the same
     *    LIFO order.
     *
     * The very first callback that is found to be `isEnabled` is returned immediately.
     *
     * @return The single highest-priority [NavigationEventCallback] that is currently enabled, or
     *   `null` if no enabled callbacks exist.
     */
    fun resolveEnabledCallback(): NavigationEventCallback<*>? {
        // `firstOrNull` is efficient and respects the LIFO order of the ArrayDeque.
        return overlayCallbacks.firstOrNull { it.isBackEnabled }
            ?: defaultCallbacks.firstOrNull { it.isBackEnabled }
    }
}
