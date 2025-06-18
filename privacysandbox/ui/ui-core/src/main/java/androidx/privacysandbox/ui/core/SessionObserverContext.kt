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

import android.view.View

/**
 * A class containing information that will be constant through the lifetime of a [SessionObserver].
 *
 * When [SessionObserver.onSessionClosed] is called for the associated session observers, the
 * resources of the [SessionObserverContext] will be freed.
 */
public class SessionObserverContext(
    /**
     * Returns the view that is presenting content for the associated [SandboxedUiAdapter.Session].
     *
     * This value will be non-null if the [SandboxedUiAdapter.Session] and the [SessionObserver] are
     * created from the same process. Otherwise, it will be null.
     */
    public val view: View?,
    /** Returns the signal options which are supported by the container hosting the UI session. */
    public val supportedSignalOptions: Set<String>,
) {
    override fun toString(): String =
        "SessionObserverContext(view=$view, supportedSignalOptions=$supportedSignalOptions)"

    override fun equals(other: Any?): Boolean {
        return other is SessionObserverContext &&
            view == other.view &&
            supportedSignalOptions == other.supportedSignalOptions
    }

    override fun hashCode(): Int {
        var result = view.hashCode()
        return result + 31 * supportedSignalOptions.hashCode()
    }
}
