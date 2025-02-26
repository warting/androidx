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

package androidx.privacysandbox.ui.core

import android.os.Bundle
import androidx.annotation.RestrictTo

/**
 * Interface implemented by client side SandboxedUiAdapters. This interface provides the underlying
 * Bundle of the SandboxedUiAdapter.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
interface ClientAdapterWrapper {
    /** Provides the underlying bundle with which the client side SandboxedUiAdapter was created. */
    fun getSourceBundle(): Bundle
}
