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

package androidx.appactions.interaction.capabilities.testing.internal

import androidx.appactions.interaction.capabilities.core.ActionExecutorAsync
import androidx.appactions.interaction.capabilities.core.ExecutionResult
import androidx.appactions.interaction.capabilities.core.impl.concurrent.Futures

object TestingUtils {
    const val CB_TIMEOUT = 1000L

    fun <ArgumentT, OutputT> createFakeActionExecutor(): ActionExecutorAsync<ArgumentT, OutputT> {
        return ActionExecutorAsync { _: ArgumentT ->
            Futures.immediateFuture(
                ExecutionResult.getDefaultInstance(),
            )
        }
    }
}
