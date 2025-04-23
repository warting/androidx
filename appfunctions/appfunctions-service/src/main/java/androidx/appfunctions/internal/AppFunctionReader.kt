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

import androidx.appfunctions.AppFunctionSearchSpec
import androidx.appfunctions.metadata.AppFunctionMetadata
import androidx.appfunctions.metadata.AppFunctionSchemaMetadata
import kotlinx.coroutines.flow.Flow

/** Searches AppFunctions. */
internal interface AppFunctionReader {

    /**
     * Searches for app functions based on the provided search specification.
     *
     * @param searchFunctionSpec The search specification, which includes filters for searching
     *   matching documents.
     * @return A flow emitting a list of app function metadata matching the search criteria.
     * @see androidx.appfunctions.AppFunctionSearchSpec
     */
    fun searchAppFunctions(
        searchFunctionSpec: AppFunctionSearchSpec
    ): Flow<List<AppFunctionMetadata>>

    /**
     * Returns the [AppFunctionSchemaMetadata] of the given app function. Returns null if the
     * function is not implementing a predefined schema.
     *
     * @throws androidx.appfunctions.AppFunctionFunctionNotFoundException if the function does not
     *   exist.
     */
    suspend fun getAppFunctionSchemaMetadata(
        functionId: String,
        packageName: String
    ): AppFunctionSchemaMetadata?
}
