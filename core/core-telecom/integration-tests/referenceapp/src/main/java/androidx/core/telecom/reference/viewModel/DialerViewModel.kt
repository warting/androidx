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

package androidx.core.telecom.reference.viewModel

import android.content.Context
import android.net.Uri
import androidx.core.telecom.CallAttributesCompat
import androidx.core.telecom.reference.CallRepository
import androidx.core.telecom.reference.model.DialerUiState
import androidx.core.telecom.reference.view.loadPhoneNumberPrefix
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * ViewModel for the Dialer screen.
 *
 * This ViewModel manages the state of the dialer UI, including the display name, phone number, call
 * type (audio/video), and call capabilities (e.g., hold). It interacts with the [CallRepository] to
 * initiate outgoing calls based on the current UI state.
 *
 * @param context The application context, used for accessing resources like phone number prefixes.
 * @param callRepository The repository responsible for handling call-related operations.
 */
class DialerViewModel(
    private val context: Context,
    private val callRepository: CallRepository = CallRepository()
) : ViewModel() {
    // Internal mutable state flow to hold the Dialer UI state.
    private val _uiState = MutableStateFlow(DialerUiState())
    // Publicly exposed immutable state flow for observing UI state changes.
    val uiState: StateFlow<DialerUiState> = _uiState.asStateFlow()

    /**
     * Updates the display name in the UI state.
     *
     * @param name The new display name to set.
     */
    fun updateDisplayName(name: String) {
        _uiState.update { it.copy(displayName = name) }
    }

    /**
     * Updates the phone number in the UI state.
     *
     * @param number The new phone number to set (should be the raw number without prefix).
     */
    fun updatePhoneNumber(number: String) {
        _uiState.update { it.copy(phoneNumber = number) }
    }

    /**
     * Updates whether the call should be a video call in the UI state.
     *
     * @param isVideo `true` if the call should be a video call, `false` otherwise.
     */
    fun updateIsVideoCall(isVideo: Boolean) {
        _uiState.update { it.copy(isVideoCall = isVideo) }
    }

    /**
     * Updates whether the call should support the hold capability in the UI state.
     *
     * @param canHold `true` if the call should support being put on hold, `false` otherwise.
     */
    fun updateCanHold(canHold: Boolean) {
        _uiState.update { it.copy(canHold = canHold) }
    }

    /**
     * Initiates an outgoing call using the current UI state.
     *
     * Constructs [CallAttributesCompat] based on the current display name, phone number (with
     * prefix), call type, and capabilities, then requests the [callRepository] to add the outgoing
     * call.
     */
    fun initiateOutgoingCall() {
        callRepository.addOutgoingCall(
            CallAttributesCompat(
                _uiState.value.displayName,
                // Assumes loadPhoneNumberPrefix function exists elsewhere to get the appropriate
                // prefix.
                Uri.parse(loadPhoneNumberPrefix(context) + _uiState.value.phoneNumber),
                CallAttributesCompat.DIRECTION_OUTGOING,
                callType = getCallType(),
                callCapabilities = getCallCapabilities()
            )
        )
    }

    /**
     * Determines the appropriate call type based on the current UI state.
     *
     * @return [CallAttributesCompat.CALL_TYPE_VIDEO_CALL] if `isVideoCall` is true, otherwise
     *   [CallAttributesCompat.CALL_TYPE_AUDIO_CALL].
     */
    fun getCallType(): Int {
        return if (_uiState.value.isVideoCall) {
            CallAttributesCompat.CALL_TYPE_VIDEO_CALL
        } else {
            CallAttributesCompat.CALL_TYPE_AUDIO_CALL
        }
    }

    /**
     * Determines the call capabilities based on the current UI state.
     *
     * @return [CallAttributesCompat.SUPPORTS_SET_INACTIVE] if `canHold` is true, otherwise 0 (no
     *   specific capabilities).
     */
    fun getCallCapabilities(): Int {
        return if (_uiState.value.canHold) {
            CallAttributesCompat.SUPPORTS_SET_INACTIVE // Capability for supporting hold
        } else {
            0 // No specific capabilities indicated
        }
    }
}
