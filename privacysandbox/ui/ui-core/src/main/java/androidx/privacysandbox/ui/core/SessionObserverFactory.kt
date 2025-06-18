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

package androidx.privacysandbox.ui.core

/**
 * A factory that creates [SessionObserver] instances that can be attached to a
 * [SandboxedUiAdapter.Session]. Many [SessionObserver]s may be created for the same
 * [SandboxedUiAdapter.Session].
 */
interface SessionObserverFactory {
    /**
     * The set of signals that should be collected for each [SandboxedUiAdapter.Session]. This set
     * of signals is defined by [SandboxedUiAdapterSignalOptions].
     *
     * The set of signals that are supported by the client will be sent in the
     * [SessionObserverContext] object in [SessionObserver.onSessionOpened].
     */
    val signalOptions: Set<String>
        get() = setOf()

    /**
     * Called if a new [SandboxedUiAdapter.Session] has been opened by the [SandboxedUiAdapter] that
     * this factory is registered to. This will not be called for sessions that are already open.
     */
    fun create(): SessionObserver
}
