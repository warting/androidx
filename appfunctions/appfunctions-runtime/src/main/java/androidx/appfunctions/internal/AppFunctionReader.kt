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

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.appfunctions.AppFunctionSearchSpec
import androidx.appfunctions.metadata.AppFunctionComponentsMetadata
import androidx.appfunctions.metadata.AppFunctionMetadata
import androidx.appfunctions.metadata.AppFunctionMetadataDocument
import androidx.appfunctions.metadata.AppFunctionPrimitiveTypeMetadata
import androidx.appfunctions.metadata.AppFunctionResponseMetadata
import androidx.appfunctions.metadata.AppFunctionRuntimeMetadata
import androidx.appfunctions.metadata.AppFunctionSchemaMetadata
import androidx.appsearch.app.GenericDocument
import androidx.appsearch.app.GlobalSearchSession
import androidx.appsearch.app.JoinSpec
import androidx.appsearch.app.SearchResult
import androidx.appsearch.app.SearchSpec
import com.android.extensions.appfunctions.AppFunctionManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * A class responsible for reading and searching for app functions based on a search specification.
 *
 * It searches for AppFunction documents using the [GlobalSearchSession] and converts them into
 * [AppFunctionMetadata] objects.
 *
 * @param context The context of the application, used for session creation.
 */
internal class AppFunctionReader(private val context: Context) {

    /**
     * Searches for app functions based on the provided search specification.
     *
     * @param searchFunctionSpec The search specification, which includes filters for searching
     *   matching documents.
     * @return A flow emitting a list of app function metadata matching the search criteria.
     * @see AppFunctionSearchSpec
     */
    @RequiresApi(Build.VERSION_CODES.S)
    fun searchAppFunctions(
        searchFunctionSpec: AppFunctionSearchSpec
    ): Flow<List<AppFunctionMetadata>> {
        // TODO: Use observer API to emit new values when underlying documents are changed.
        return flow {
            if (searchFunctionSpec.packageNames?.isEmpty() == true) {
                emit(emptyList())
                return@flow
            }

            createSearchSession(context = context).use { session ->
                emit(performSearch(session, searchFunctionSpec))
            }
        }
    }

    private suspend fun performSearch(
        session: GlobalSearchSession,
        searchFunctionSpec: AppFunctionSearchSpec,
    ): List<AppFunctionMetadata> {
        val joinSpec =
            JoinSpec.Builder(AppFunctionRuntimeMetadata.STATIC_METADATA_JOIN_PROPERTY)
                .setNestedSearch("", RUNTIME_SEARCH_SPEC)
                .build()

        val staticSearchSpec =
            SearchSpec.Builder()
                .addFilterNamespaces(APP_FUNCTIONS_NAMESPACE)
                .addFilterDocumentClasses(AppFunctionMetadataDocument::class.java)
                .addFilterPackageNames(SYSTEM_PACKAGE_NAME)
                .setJoinSpec(joinSpec)
                .setVerbatimSearchEnabled(true)
                .setNumericSearchEnabled(true)
                .build()
        return session
            .search(searchFunctionSpec.toStaticMetadataAppSearchQuery(), staticSearchSpec)
            .readAll(::convertSearchResultToAppFunctionMetadata)
            .filterNotNull()
    }

    private fun convertSearchResultToAppFunctionMetadata(
        searchResult: SearchResult
    ): AppFunctionMetadata? {

        // This is different from document id which for uniqueness is computed as packageName + "/"
        // + functionId.
        val functionId = checkNotNull(searchResult.genericDocument.getPropertyString("functionId"))
        val packageName =
            checkNotNull(searchResult.genericDocument.getPropertyString("packageName"))

        // TODO: Handle failures and log instead of throwing.
        val staticMetadataDocument =
            searchResult.genericDocument.toDocumentClass(AppFunctionMetadataDocument::class.java)
        val runtimeMetadataDocument =
            searchResult.joinedResults
                .single()
                .genericDocument
                .toDocumentClass(AppFunctionRuntimeMetadata::class.java)

        return AppFunctionMetadata(
            id = functionId,
            packageName = packageName,
            isEnabled = computeEffectivelyEnabled(staticMetadataDocument, runtimeMetadataDocument),
            schema = buildSchemaMetadataFromGdForLegacyIndexer(searchResult.genericDocument),
            // TODO: Populate them separately for legacy indexer.
            parameters =
                // Since this is a list type it can be null for cases where an app function has no
                // parameters.
                if (staticMetadataDocument.response != null) {
                    staticMetadataDocument.parameters?.map { it.toAppFunctionParameterMetadata() }
                        ?: emptyList()
                } else {
                    // TODO - Populate for legacy indexer
                    emptyList()
                },
            response =
                staticMetadataDocument.response?.toAppFunctionResponseMetadata()
                    ?: AppFunctionResponseMetadata(
                        valueType =
                            AppFunctionPrimitiveTypeMetadata(
                                type = AppFunctionPrimitiveTypeMetadata.TYPE_UNIT,
                                isNullable = false
                            )
                    ),
            components =
                staticMetadataDocument.components?.toAppFunctionComponentsMetadata()
                    ?: AppFunctionComponentsMetadata(),
        )
    }

    private fun computeEffectivelyEnabled(
        staticMetadata: AppFunctionMetadataDocument,
        runtimeMetadata: AppFunctionRuntimeMetadata,
    ): Boolean =
        when (runtimeMetadata.enabled.toInt()) {
            AppFunctionManager.APP_FUNCTION_STATE_ENABLED -> true
            AppFunctionManager.APP_FUNCTION_STATE_DISABLED -> false
            AppFunctionManager.APP_FUNCTION_STATE_DEFAULT -> staticMetadata.isEnabledByDefault
            else ->
                throw IllegalStateException(
                    "Unknown AppFunction state: ${runtimeMetadata.enabled}."
                )
        }

    private fun buildSchemaMetadataFromGdForLegacyIndexer(
        document: GenericDocument
    ): AppFunctionSchemaMetadata? {
        val schemaName = document.getPropertyString("schemaName")
        val schemaCategory = document.getPropertyString("schemaCategory")
        val schemaVersion = document.getPropertyLong("schemaVersion")

        if (schemaName == null || schemaCategory == null || schemaVersion == 0L) {
            if (schemaName != null || schemaCategory != null || schemaVersion != 0L) {
                Log.e(
                    AppFunctionReader::class.simpleName,
                    "Unexpected state: schemaName=$schemaName, schemaCategory=$schemaCategory, schemaVersion=$schemaVersion"
                )
            }
            return null
        }

        return AppFunctionSchemaMetadata(
            name = schemaName,
            category = schemaCategory,
            version = schemaVersion
        )
    }

    private companion object {
        const val SYSTEM_PACKAGE_NAME = "android"
        const val APP_FUNCTIONS_NAMESPACE = "app_functions"
        const val APP_FUNCTIONS_RUNTIME_NAMESPACE = "app_functions_runtime"

        val RUNTIME_SEARCH_SPEC =
            SearchSpec.Builder()
                .addFilterNamespaces(APP_FUNCTIONS_RUNTIME_NAMESPACE)
                .addFilterDocumentClasses(AppFunctionRuntimeMetadata::class.java)
                .addFilterPackageNames(SYSTEM_PACKAGE_NAME)
                .setVerbatimSearchEnabled(true)
                .build()
    }
}
