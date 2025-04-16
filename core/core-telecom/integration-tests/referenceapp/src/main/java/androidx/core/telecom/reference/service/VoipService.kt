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

package androidx.core.telecom.reference.service

import androidx.core.telecom.CallAttributesCompat
import androidx.core.telecom.CallEndpointCompat
import androidx.core.telecom.reference.model.CallData
import kotlinx.coroutines.flow.StateFlow

interface VoipService {
    val callDataUpdates: StateFlow<List<CallData>>

    fun addCall(callAttributes: CallAttributesCompat, notificationId: Int)

    fun setCallActive(callId: String)

    fun setCallInactive(callId: String)

    fun endCall(callId: String)

    fun switchCallEndpoint(callId: String, endpoint: CallEndpointCompat)

    fun toggleGlobalMute(isMuted: Boolean)
}
