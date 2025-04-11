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
package androidx.wear.remote.interactions

import android.net.Uri
import android.os.OutcomeReceiver
import androidx.annotation.ChecksSdkIntAtLeast
import com.google.wear.services.remoteinteractions.RemoteInteractionsManager
import java.util.concurrent.Executor
import java.util.function.Consumer

/** Forwards remote interactions to [RemoteInteractionsManager]. */
internal interface IRemoteInteractionsManager {

    /** Whether the availability status API is supported. */
    val isAvailabilityStatusApiSupported: Boolean

    /** Whether the startRemoteActivity API is supported. */
    @get:ChecksSdkIntAtLeast(api = 36) val isStartRemoteActivityApiSupported: Boolean

    /**
     * Forwards a call to [RemoteInteractionsManager.registerRemoteActivityHelperStatusListener].
     */
    fun registerRemoteActivityHelperStatusListener(executor: Executor, listener: Consumer<Int>)

    /**
     * Forwards a call to [RemoteInteractionsManager.unregisterRemoteActivityHelperStatusListener].
     */
    fun unregisterRemoteActivityHelperStatusListener(listener: Consumer<Int>)

    /**
     * Forwards a call to [RemoteInteractionsManager.startRemoteActivity].
     *
     * @throws IllegalStateException if the API is not supported.
     */
    fun startRemoteActivity(
        dataUri: Uri,
        additionalCategories: List<String>,
        executor: Executor,
        outcomeReceiver: OutcomeReceiver<Void, Throwable>
    )
}
