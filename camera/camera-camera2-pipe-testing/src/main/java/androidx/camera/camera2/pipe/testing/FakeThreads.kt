/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.camera.camera2.pipe.testing

import android.os.Handler
import android.os.HandlerThread
import androidx.camera.camera2.pipe.core.Threads
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope

public object FakeThreads {
    public fun fromDispatcher(
        dispatcher: CoroutineDispatcher,
        blockingDispatcher: CoroutineDispatcher? = null,
    ): Threads {
        val scope = CoroutineScope(dispatcher + CoroutineName("CXCP-TestScope"))
        return create(scope, dispatcher, blockingDispatcher)
    }

    public fun fromTestScope(
        scope: TestScope,
        blockingDispatcher: CoroutineDispatcher? = null,
    ): Threads {
        val dispatcher = StandardTestDispatcher(scope.testScheduler, "CXCP-TestScope")
        return create(scope, dispatcher, blockingDispatcher)
    }

    private fun create(
        scope: CoroutineScope,
        dispatcher: CoroutineDispatcher,
        blockingDispatcher: CoroutineDispatcher?,
    ): Threads {
        val cameraPipeDispatchScope =
            CoroutineScope(SupervisorJob() + CoroutineName("CXCP-Dispatch"))
        val executor = dispatcher.asExecutor()

        @Suppress("deprecation")
        val fakeHandler = {
            val handlerThread = HandlerThread("FakeHandlerThread").apply { start() }
            Handler(handlerThread.looper)
        }

        return Threads(
            cameraPipeScope = scope,
            cameraPipeDispatchScope = cameraPipeDispatchScope,
            blockingExecutor = blockingDispatcher?.asExecutor() ?: executor,
            blockingDispatcher = blockingDispatcher ?: dispatcher,
            backgroundExecutor = executor,
            backgroundDispatcher = dispatcher,
            lightweightExecutor = executor,
            lightweightDispatcher = dispatcher,
            camera2Handler = fakeHandler,
            camera2Executor = { executor }
        )
    }
}
