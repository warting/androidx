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

package androidx.navigationevent.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.navigationevent.NavigationEvent
import androidx.navigationevent.NavigationEventCallback
import androidx.navigationevent.NavigationEventInfo.NotProvided
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.BUFFERED
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.launch

/**
 * Handles predictive back navigation gestures.
 *
 * This effect registers a callback to receive updates on the progress of system back gestures as a
 * [Flow] of [NavigationEvent].
 *
 * The [onEvent] lambda should be structured to handle the start, progress, completion, and
 * cancellation of the gesture:
 * ```kotlin
 * NavigationEventHandler { progress: Flow<NavigationEvent> ->
 *   // This block is executed when the back gesture begins.
 *     try {
 *       progress.collect { backEvent ->
 *         // Handle gesture progress updates here.
 *       }
 *       // This block is executed if the gesture completes successfully.
 *     } catch (e: CancellationException) {
 *       // This block is executed if the gesture is cancelled.
 *       throw e
 *     } finally {
 *       // This block is executed either the gesture is completed or cancelled.
 *   }
 * }
 * ```
 *
 * ## Precedence
 * When multiple [NavigationEventHandler] are present in the composition, the one that is composed
 * * **last** among all enabled handlers will be invoked.
 *
 * ## Usage
 * It is important to call this composable **unconditionally**. Use the `enabled` parameter to
 * control whether the handler is active. This is preferable to conditionally calling
 * [NavigationEventHandler] (e.g., inside an `if` block), as conditional calls can change the order
 * of composition, leading to unpredictable behavior where different handlers are invoked after
 * recomposition.
 *
 * @param enabled A lambda that returns `true` if this handler should be enabled. This lambda is
 *   observed for changes. To ensure the handler's status is updated correctly, the lambda should
 *   read from a snapshot-aware state object (e.g., a [State] created with `remember {
 *   mutableStateOf(true) }`). Defaults to always `true`.
 * @param onEvent The lambda that receives the flow of back gesture events.
 */
@Composable
public fun NavigationEventHandler(
    enabled: () -> Boolean = { true },
    onEvent: suspend (progress: Flow<NavigationEvent>) -> Unit,
) {
    // ensure we don't re-register callbacks when onBack changes
    val currentOnBack by rememberUpdatedState(onEvent)
    val navEventScope = rememberCoroutineScope()

    val navEventCallBack = remember {
        NavigationEventHandlerCallback(enabled, navEventScope, currentOnBack)
    }

    // we want to use the same callback, but ensure we adjust the variable on recomposition
    SideEffect {
        navEventCallBack.currentOnBack = currentOnBack
        navEventCallBack.onBackScope = navEventScope
    }

    LaunchedEffect(enabled) {
        // LaunchedEffect is not snapshot-aware by itself, so we use `snapshotFlow` to observe
        // changes to `enabled()`. `snapshotFlow` converts snapshot state reads into a cold Flow
        // that emits whenever the underlying snapshot-aware state changes.
        //
        // Note: `snapshotFlow` only works correctly when the lambda reads values from
        // snapshot-aware state objects (e.g., `State`, `MutableState`, or Compose state APIs).
        //
        // We collect this Flow to update the callback whenever `enabled` changes.
        //
        // Because we collect this Flow inside a coroutine, the timing of emissions is also bound to
        // the CoroutineDispatcher used by the composition. This means snapshot state changes are
        // only observed and handled when the coroutine dispatcher schedules the collection, so
        // updates might not be strictly synchronous with the state change.
        snapshotFlow(enabled).collect { isEnabled -> navEventCallBack.setIsEnabled(isEnabled) }
    }

    val navEventDispatcher =
        checkNotNull(LocalNavigationEventDispatcherOwner.current) {
                "No NavigationEventDispatcher was provided via LocalNavigationEventDispatcherOwner"
            }
            .navigationEventDispatcher

    DisposableEffect(navEventDispatcher) {
        navEventDispatcher.addCallback(navEventCallBack)

        onDispose { navEventCallBack.remove() }
    }
}

private class OnBackInstance(
    scope: CoroutineScope,
    var isPredictiveBack: Boolean,
    onBack: suspend (progress: Flow<NavigationEvent>) -> Unit,
    callback: NavigationEventCallback<*>,
) {
    val channel =
        Channel<NavigationEvent>(capacity = BUFFERED, onBufferOverflow = BufferOverflow.SUSPEND)
    val job =
        scope.launch {
            if (callback.isEnabled) {
                var completed = false
                onBack(channel.consumeAsFlow().onCompletion { completed = true })
                check(completed) { "You must collect the progress flow" }
            }
        }

    fun send(backEvent: NavigationEvent) = channel.trySend(backEvent)

    // idempotent if invoked more than once
    fun close() = channel.close()

    fun cancel() {
        channel.cancel(CancellationException("navEvent cancelled"))
        job.cancel()
    }
}

// TODO(mgalhardo): consider allowing `NavigationEventHandler` to set a `T` for State.
private class NavigationEventHandlerCallback(
    enabled: () -> Boolean,
    var onBackScope: CoroutineScope,
    var currentOnBack: suspend (progress: Flow<NavigationEvent>) -> Unit,
) : NavigationEventCallback<NotProvided>(enabled()) {
    private var onBackInstance: OnBackInstance? = null
    private var isActive = false

    fun setIsEnabled(enabled: Boolean) {
        // We are disabling a callback that was enabled.
        if (!enabled && !isActive && isEnabled) {
            onBackInstance?.cancel()
        }
        isEnabled = enabled
    }

    override fun onEventStarted(event: NavigationEvent) {
        // in case the previous onBackInstance was started by a normal back gesture
        // we want to make sure it's still cancelled before we start a predictive
        // back gesture
        onBackInstance?.cancel()
        if (isEnabled) {
            onBackInstance = OnBackInstance(onBackScope, true, currentOnBack, this)
        }
        isActive = true
    }

    override fun onEventProgressed(event: NavigationEvent) {
        onBackInstance?.send(event)
    }

    override fun onEventCompleted() {
        // handleOnBackPressed could be called by regular back to restart
        // a new back instance. If this is the case (where current back instance
        // was NOT started by handleOnBackStarted) then we need to reset the previous
        // regular back.
        onBackInstance?.apply {
            if (!isPredictiveBack) {
                cancel()
                onBackInstance = null
            }
        }
        if (onBackInstance == null) {
            onBackInstance = OnBackInstance(onBackScope, false, currentOnBack, this)
        }

        // finally, we close the channel to ensure no more events can be sent
        // but let the job complete normally
        onBackInstance?.close()
        onBackInstance?.isPredictiveBack = false
        isActive = false
    }

    override fun onEventCancelled() {
        // cancel will purge the channel of any sent events that are yet to be received
        onBackInstance?.cancel()
        onBackInstance?.isPredictiveBack = false
        isActive = false
    }
}
