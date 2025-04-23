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

package androidx.appfunctions.internal

import androidx.annotation.RestrictTo
import androidx.appfunctions.AppFunctionContext
import androidx.appfunctions.AppFunctionFunctionNotFoundException

/**
 * An [AppFunctionInvoker] that will delegate [unsafeInvoke] to the implementation that supports the
 * given function call request.
 *
 * AppFunction compiler will automatically generate the implementation of this class.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public abstract class AggregatedAppFunctionInvoker : AppFunctionInvoker {

    /** The list of [AppFunctionInvoker] instances that contribute to this aggregate. */
    public abstract val invokers: List<AppFunctionInvoker>

    final override val supportedFunctionIds: Set<String> by lazy {
        // Empty collection can't be reduced
        if (invokers.isEmpty()) return@lazy emptySet<String>()
        invokers.map(AppFunctionInvoker::supportedFunctionIds).reduce { acc, ids -> acc + ids }
    }

    final override suspend fun unsafeInvoke(
        appFunctionContext: AppFunctionContext,
        functionIdentifier: String,
        parameters: Map<String, Any?>
    ): Any? {
        for (invoker in invokers) {
            if (invoker.supportedFunctionIds.contains(functionIdentifier)) {
                return invoker.unsafeInvoke(appFunctionContext, functionIdentifier, parameters)
            }
        }
        throw AppFunctionFunctionNotFoundException("Unable to find $functionIdentifier")
    }
}
