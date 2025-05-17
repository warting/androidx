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
import androidx.core.telecom.util.ExperimentalAppActions
import kotlinx.coroutines.flow.StateFlow

/**
 * Tracks requests to kick participants from a remote InCallService and invokes the supplied action
 * when a request comes in.
 *
 * @param participants A StateFlow containing a list of unique Participants in the call, which is
 *   used to validate the participant to kick is valid.
 * @param onKickParticipant The action to perform when a request comes in from the remote
 *   InCallService to kick a participant.
 */
@OptIn(ExperimentalAppActions::class)
internal class KickParticipantState(
    val participants: StateFlow<List<Participant>>,
    private val onKickParticipant: suspend (Participant) -> Unit,
) {
    companion object {
        const val LOG_TAG = Extensions.LOG_TAG + "(KPAR)"
    }

    /**
     * Connects this action to the remote in order to listen to kick participant updates.
     *
     * @param repository The repository of callbacks this method will use to register for kick
     *   participant callbacks.
     */
    internal fun connect(repository: ParticipantActionCallbackRepository) {
        Log.i(LOG_TAG, "initialize: register callback from remote")
        repository.kickParticipantCallback = ::kickParticipant
    }

    /**
     * Registers to be called when the remote InCallService has requested to kick a Participant.
     *
     * @param participantId The id of the participant to kick
     */
    private suspend fun kickParticipant(participantId: String) {
        val participant =
            participants.value.firstOrNull { participant -> participant.id == participantId }
        if (participant == null) {
            Log.w(LOG_TAG, "kickParticipant: $participantId can not be found")
            return
        }
        Log.d(LOG_TAG, "kickParticipant: kicking $participant")
        onKickParticipant(participant)
    }
}
