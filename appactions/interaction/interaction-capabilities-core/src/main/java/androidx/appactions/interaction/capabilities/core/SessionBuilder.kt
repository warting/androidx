/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.appactions.interaction.capabilities.core

/**
 * Interface to be implemented for creating SessionT instances.
 */
fun interface SessionBuilder<SessionT> {
    /**
     * Implement this method to create session for handling assistant requests.\
     *
     * @param hostProperties only applicable while used with AppInteractionService. Contains the
     *   dimensions of the UI area. Null when used without AppInteractionService.
     *
     * @return A new SessionT instance for handling a task.
     */
    fun createSession(
        hostProperties: HostProperties?,
    ): SessionT
}
