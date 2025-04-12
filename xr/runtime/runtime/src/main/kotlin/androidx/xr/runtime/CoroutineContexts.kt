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

package androidx.xr.runtime

import java.util.concurrent.Executors
import java.util.concurrent.ThreadFactory
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.asCoroutineDispatcher

/** Provides the [CoroutineContext] objects for all coroutines in Jetpack XR Runtime. */
internal object CoroutineContexts {

    /** A [CoroutineContext] for lightweight tasks that are small and non-blocking. */
    val Lightweight: CoroutineContext =
        Executors.newSingleThreadExecutor(
                object : ThreadFactory {
                    override fun newThread(r: Runnable): Thread {
                        return Thread(r, "JXRRuntimeSession")
                    }
                }
            )
            .asCoroutineDispatcher()
}
