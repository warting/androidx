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

package androidx.core.telecom.extensions

import android.util.Log
import androidx.core.telecom.internal.ParticipantActionCallbackRepository
import androidx.core.telecom.internal.ParticipantStateListenerRemote
import androidx.core.telecom.util.ExperimentalAppActions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/**
 * Tracks the current raised hand state of all of the Participants of this call and notifies the
 * listener if a remote requests to change the user's raised hand state.
 *
 * @param participants The StateFlow containing the current set of Participants in the call
 * @param onHandRaisedChanged The action to perform when the remote InCallService requests to change
 *   this user's raised hand state.
 */
@ExperimentalAppActions
internal class RaiseHandState(
    val participants: StateFlow<Set<Participant>>,
    private val onHandRaisedChanged: suspend (Boolean) -> Unit
) {
    companion object {
        const val LOG_TAG = Extensions.LOG_TAG + "(RHAR)"
    }

    private val raisedHandsState: MutableStateFlow<Set<Participant>> = MutableStateFlow(emptySet())

    /**
     * Notify the remote InCallService of an update to the participants that have their hands raised
     *
     * @param raisedHands The new set of Participants that have their hands raised.
     */
    suspend fun updateRaisedHands(raisedHands: Set<Participant>) {
        raisedHandsState.emit(raisedHands)
    }

    /**
     * Connect this Action to a new remote that supports listening to this action's state updates.
     *
     * @param scope The CoroutineScope to use to update the remote
     * @param repository The event repository used to listen to state updates from the remote.
     * @param remote The interface used to communicate with the remote.
     */
    internal fun connect(
        scope: CoroutineScope,
        repository: ParticipantActionCallbackRepository,
        remote: ParticipantStateListenerRemote
    ) {
        Log.i(LOG_TAG, "initialize: sync state")
        repository.raiseHandStateCallback = ::raiseHandStateChanged
        // Send current state
        remote.updateRaisedHandsAction(raisedHandsState.value.map { it.id }.toIntArray())
        // Set up updates to the remote when the state changes
        participants
            .combine(raisedHandsState) { p, rhs -> p.intersect(rhs) }
            .distinctUntilChanged()
            .onEach {
                Log.i(LOG_TAG, "to remote: updateRaisedHands=$it")
                remote.updateRaisedHandsAction(it.map { p -> p.id }.toIntArray())
            }
            .launchIn(scope)
    }

    /**
     * Registered to be called when the remote InCallService has requested to change the raised hand
     * state of the user.
     *
     * @param state The new raised hand state, true if hand is raised, false if it is not.
     */
    private suspend fun raiseHandStateChanged(state: Boolean) {
        Log.d(LOG_TAG, "raisedHandStateChanged: updated state: $state")
        onHandRaisedChanged(state)
    }
}
