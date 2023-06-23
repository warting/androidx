/* Copyright (C) 2014 The Android Open Source Project
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
///////////////////////////////////////////////////////////////////////////////
// THIS FILE IS IMMUTABLE. DO NOT EDIT IN ANY CASE.                          //
///////////////////////////////////////////////////////////////////////////////

// This file is a snapshot of an AIDL file. Do not edit it manually. There are
// two cases:
// 1). this is a frozen version file - do not edit this in any case.
// 2). this is a 'current' file. If you make a backwards compatible change to
//     the interface (from the latest frozen version), the build system will
//     prompt you to update this file with `m <name>-update-api`.
//
// You must not make a backward incompatible change to any AIDL file built
// with the aidl_interface module type with versions property set. The module
// type is used to build AIDL files in a way that they can be used across
// independently updatable components of the system. If a device is shipped
// with such a backward incompatible change, it has a high risk of breaking
// later when a module using the interface is updated, e.g., Mainline modules.

package android.support.v4.media.session;
@JavaPassthrough(annotation="@androidx.annotation.RestrictTo(androidx.annotation.RestrictTo.Scope.LIBRARY)")
interface IMediaControllerCallback {
  oneway void onEvent(String event, in android.os.Bundle extras);
  oneway void onSessionDestroyed();
  oneway void onPlaybackStateChanged(in android.support.v4.media.session.PlaybackStateCompat state);
  oneway void onMetadataChanged(in android.support.v4.media.MediaMetadataCompat metadata);
  oneway void onQueueChanged(in List<android.support.v4.media.session.MediaSessionCompat.QueueItem> queue);
  oneway void onQueueTitleChanged(CharSequence title);
  oneway void onExtrasChanged(in android.os.Bundle extras);
  oneway void onVolumeInfoChanged(in android.support.v4.media.session.ParcelableVolumeInfo info);
  oneway void onRepeatModeChanged(int repeatMode);
  oneway void onShuffleModeChangedRemoved(boolean enabled);
  oneway void onCaptioningEnabledChanged(boolean enabled);
  oneway void onShuffleModeChanged(int shuffleMode);
  oneway void onSessionReady();
}
