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

package androidx.privacysandbox.sdkruntime.core.controller.impl

import androidx.annotation.RestrictTo
import androidx.privacysandbox.sdkruntime.core.LoadSdkCompatException
import androidx.privacysandbox.sdkruntime.core.SandboxedSdkCompat
import androidx.privacysandbox.sdkruntime.core.controller.LoadSdkCallback
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** Implementation of [LoadSdkCallback] that passes result to [Continuation]. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class ContinuationLoadSdkCallback(
    private val continuation: Continuation<SandboxedSdkCompat>
) : LoadSdkCallback, AtomicBoolean(false) {
    override fun onResult(result: SandboxedSdkCompat) {
        // Do not attempt to resume more than once, even if the caller is buggy.
        if (compareAndSet(false, true)) {
            continuation.resume(result)
        }
    }

    override fun onError(error: LoadSdkCompatException) {
        // Do not attempt to resume more than once, even if the caller is buggy.
        if (compareAndSet(false, true)) {
            continuation.resumeWithException(error)
        }
    }

    override fun toString(): String = "ContinuationLoadSdkCallback(outcomeReceived = ${get()})"
}
