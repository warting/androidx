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

package androidx.xr.runtime.testing

import androidx.annotation.RestrictTo
import androidx.xr.runtime.SessionConnector
import androidx.xr.runtime.internal.JxrPlatformAdapter
import androidx.xr.runtime.internal.LifecycleManager

@Suppress("NotCloseable")
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public class FakeSessionConnector : SessionConnector {
    /** Whether the [SessionConnector] has been initialized or not. */
    public var isInitialized: Boolean = false

    override fun initialize(
        lifecycleManager: LifecycleManager,
        platformAdapter: JxrPlatformAdapter
    ) {
        isInitialized = true
    }

    override fun close() {
        isInitialized = false
    }
}
