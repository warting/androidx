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

import android.net.Uri
import android.telecom.Call
import androidx.core.telecom.util.ExperimentalAppActions

/**
 * Provides a scope where extensions can be first initialized and next managed for a [Call] once
 * [onConnected] is called.
 *
 * The following extension is supported on a call:
 * - [addParticipantExtension] - Show the user more information about the [Participant]s in the
 *   call.
 *
 * ```
 * class InCallServiceImpl : InCallServiceCompat() {
 * ...
 *   override fun onCallAdded(call: Call) {
 *     lifecycleScope.launch {
 *       connectExtensions(context, call) {
 *         // Initialize extensions
 *         onConnected { call ->
 *           // change call states & listen for extension updates/send extension actions
 *         }
 *       }
 *       // Once the call is destroyed, control flow will resume again
 *     }
 *   }
 *  ...
 * }
 * ```
 */
@ExperimentalAppActions
public interface CallExtensionScope {

    /**
     * Called when the [Call] extensions have been successfully set up and are ready to be used.
     *
     * @param block Called when the [Call] and initialized extensions are ready to be used.
     */
    public fun onConnected(block: suspend (Call) -> Unit)

    /**
     * Add support for this remote surface to display information related to the [Participant]s in
     * this call.
     *
     * ```
     * connectExtensions(call) {
     *     val participantExtension = addParticipantExtension(
     *         // consume participant changed events
     *     )
     *     onConnected {
     *         // extensions have been negotiated and actions are ready to be used
     *     }
     * }
     * ```
     *
     * @param onActiveParticipantChanged Called with the active [Participant] in the call has
     *   changed. If this method is called with a `null` [Participant], there is no active
     *   [Participant]. The active [Participant] in the call is the [Participant] that should take
     *   focus and be either more prominent on the screen or otherwise featured as active in UI. For
     *   example, this could be the [Participant] that is actively talking or presenting.
     * @param onParticipantsUpdated Called when the [Participant]s in the [Call] have changed and
     *   the UI should be updated.
     * @return The interface that is used to set up additional actions for this extension.
     */
    public fun addParticipantExtension(
        onActiveParticipantChanged: suspend (Participant?) -> Unit,
        onParticipantsUpdated: suspend (Set<Participant>) -> Unit,
    ): ParticipantExtensionRemote

    /**
     * Add support for this remote surface to display meeting summary information for this call.
     *
     * This function establishes a connection with a remote service that provides meeting summary
     * information, such as the current speaker and the number of participants. The extension will
     * provide updates via the provided callbacks:
     *
     * @param onCurrentSpeakerChanged A suspend function that is called whenever the current speaker
     *   in the meeting changes. The function receives a [CharSequence] representing the new
     *   speaker's identifier (e.g., name or ID) or null if there is no current speaker.
     * @param onParticipantCountChanged A suspend function that is called whenever the number of
     *   participants in the meeting changes. It receives the new participant count as an [Int],
     *   which is always 0 or greater.
     * @return A [MeetingSummaryRemote] object with an `isSupported` property of this object will
     *   indicate whether the meeting summary extension is supported by the calling application.
     *
     * Example Usage:
     * ```kotlin
     * connectExtensions(call) {
     *     val meetingSummaryRemote =  addMeetingSummaryExtension(
     *          onCurrentSpeakerChanged = { speaker ->
     *              // Update UI with the new speaker
     *              Log.d(TAG, "Current speaker: $speaker")
     *         },
     *         onParticipantCountChanged = { count ->
     *             // Update UI with the new participant count
     *             Log.d(TAG, "Participant count: $count")
     *         }
     *     )
     *    onConnected {
     *       if (meetingSummaryRemote.isSupported) {
     *          // The extension is ready to use
     *       } else {
     *          // Handle the case where the extension is not supported.
     *       }
     *    }
     * }
     *  ```
     */
    public fun addMeetingSummaryExtension(
        onCurrentSpeakerChanged: suspend (CharSequence?) -> Unit,
        onParticipantCountChanged: suspend (Int) -> Unit,
    ): MeetingSummaryRemote

    /**
     * Add support for this remote surface to display information related to the local call silence
     * state for this call.
     *
     * ```
     * connectExtensions(call) {
     *     val localCallSilenceExtension = addLocalCallSilenceExtension(
     *         // consume local call silence state changes
     *     )
     *     onConnected {
     *         // At this point, support for the local call silence extension will be known
     *     }
     * }
     * ```
     *
     * @param onIsLocallySilencedUpdated Called when the local call silence state has changed and
     *   the UI should be updated.
     * @return The interface that is used to interact with the local call silence extension methods.
     */
    public fun addLocalCallSilenceExtension(
        onIsLocallySilencedUpdated: suspend (Boolean) -> Unit
    ): LocalCallSilenceExtensionRemote

    /**
     * Add support for call icon updates and provides a callback to receive those updates. This
     * remote surface should implement a [android.database.ContentObserver] to observe changes to
     * the icon's content URI. This is necessary to ensure the displayed icon reflects any updates
     * made by the application if the URI remains the same.
     *
     * ```
     * connectExtensions(call) {
     *     val callIconExtension = addCallIconSupport(
     *         // consume call icon state changes
     *     )
     *     onConnected {
     *         // At this point, support for call icon extension will be known
     *     }
     * }
     * ```
     *
     * @param onCallIconChanged A suspend function that will be invoked with the [Uri] of the new
     *   call icon whenever it changes. This callback will only be called if the calling application
     *   supports the call icon extension (i.e., `isSupported` returns `true`).
     * @return A [CallIconExtensionRemote] instance that allows the remote to check if the calling
     *   application supports the call icon extension. The remote *must* use this instance to check
     *   support before expecting icon updates.
     */
    public fun addCallIconSupport(onCallIconChanged: suspend (Uri) -> Unit): CallIconExtensionRemote
}
