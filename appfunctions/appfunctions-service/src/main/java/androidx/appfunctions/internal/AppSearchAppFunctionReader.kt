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
import androidx.appfunctions.AppFunctionFunctionNotFoundException
import androidx.appfunctions.AppFunctionSearchSpec
import androidx.appfunctions.metadata.AppFunctionComponentsMetadata
import androidx.appfunctions.metadata.AppFunctionComponentsMetadataDocument
import androidx.appfunctions.metadata.AppFunctionMetadata
import androidx.appfunctions.metadata.AppFunctionMetadataDocument
import androidx.appfunctions.metadata.AppFunctionParameterMetadata
import androidx.appfunctions.metadata.AppFunctionParameterMetadataDocument
import androidx.appfunctions.metadata.AppFunctionResponseMetadata
import androidx.appfunctions.metadata.AppFunctionRuntimeMetadata
import androidx.appfunctions.metadata.AppFunctionSchemaMetadata
import androidx.appsearch.app.GenericDocument
import androidx.appsearch.app.GetByDocumentIdRequest
import androidx.appsearch.app.GlobalSearchSession
import androidx.appsearch.app.JoinSpec
import androidx.appsearch.app.SearchResult
import androidx.appsearch.app.SearchSpec
import androidx.appsearch.observer.DocumentChangeInfo
import androidx.appsearch.observer.ObserverCallback
import androidx.appsearch.observer.ObserverSpec
import androidx.appsearch.observer.SchemaChangeInfo
import com.android.extensions.appfunctions.AppFunctionManager
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.launch

/**
 * A class responsible for reading and searching for app functions based on a search specification.
 *
 * It searches for AppFunction documents using the [GlobalSearchSession] and converts them into
 * [AppFunctionMetadata] objects.
 *
 * @param context The context of the application, used for session creation.
 * @param schemaAppFunctionInventory If provided, use to looks up the statically generated
 *   [AppFunctionMetadata] based on the [AppFunctionSchemaMetadata] when unable to retrieve the
 *   details from AppSearch.
 */
@RequiresApi(Build.VERSION_CODES.S)
internal class AppSearchAppFunctionReader(
    private val context: Context,
    private val schemaAppFunctionInventory: SchemaAppFunctionInventory?,
) : AppFunctionReader {

    @OptIn(FlowPreview::class)
    override fun searchAppFunctions(
        searchFunctionSpec: AppFunctionSearchSpec
    ): Flow<List<AppFunctionMetadata>> {
        if (searchFunctionSpec.packageNames?.isEmpty() == true) {
            return flow { emit(emptyList()) }
        }

        return callbackFlow {
            val session = createSearchSession(context)

            // Perform initial search immediately
            send(performSearch(session, searchFunctionSpec))

            val appSearchChannelObserver = AppSearchChannelObserver()
            // Register the observer callback
            session.registerObserverCallback(
                SYSTEM_PACKAGE_NAME,
                buildObserverSpec(searchFunctionSpec.packageNames ?: emptySet()),
                Dispatchers.Worker.asExecutor(),
                appSearchChannelObserver
            )

            // Coroutine to react to updates from the observer
            val observerJob = launch {
                appSearchChannelObserver.observe().debounce(OBSERVER_DEBOUNCE_MILLIS).collect {
                    // TODO(b/403264749): Check if we can skip the running a full search again by
                    // caching the results.
                    send(performSearch(session, searchFunctionSpec))
                }
            }

            // Clean up when collection stops
            awaitClose {
                observerJob.cancel()
                appSearchChannelObserver.close()
                session.unregisterObserverCallback(SYSTEM_PACKAGE_NAME, appSearchChannelObserver)
                session.close()
            }
        }
    }

    private class AppSearchChannelObserver : ObserverCallback {
        private val updateChannel = Channel<Unit>(Channel.RENDEZVOUS)

        override fun onSchemaChanged(changeInfo: SchemaChangeInfo) {
            updateChannel.trySend(Unit)
        }

        override fun onDocumentChanged(changeInfo: DocumentChangeInfo) {
            updateChannel.trySend(Unit)
        }

        fun observe(): Flow<Unit> = updateChannel.receiveAsFlow()

        fun close() {
            updateChannel.close()
        }
    }

    private fun buildObserverSpec(packageNames: Set<String>) =
        ObserverSpec.Builder()
            .addFilterSchemas(
                packageNames.flatMap {
                    listOf("AppFunctionStaticMetadata-$it", "AppFunctionRuntimeMetadata-$it")
                }
            )
            .build()

    private suspend fun performSearch(
        session: GlobalSearchSession,
        searchFunctionSpec: AppFunctionSearchSpec,
    ): List<AppFunctionMetadata> {
        val joinSpec =
            JoinSpec.Builder(AppFunctionRuntimeMetadata.STATIC_METADATA_JOIN_PROPERTY)
                .setNestedSearch("", RUNTIME_SEARCH_SPEC)
                .build()

        val staticMetadataSearchSpecWithJoin =
            SearchSpec.Builder()
                .addFilterNamespaces(APP_FUNCTIONS_NAMESPACE)
                .addFilterDocumentClasses(AppFunctionMetadataDocument::class.java)
                .addFilterPackageNames(SYSTEM_PACKAGE_NAME)
                .setJoinSpec(joinSpec)
                .setVerbatimSearchEnabled(true)
                .setNumericSearchEnabled(true)
                .setListFilterQueryLanguageEnabled(true)
                .build()

        val topLevelComponentsSearchSpec =
            SearchSpec.Builder()
                .addFilterNamespaces(APP_FUNCTIONS_NAMESPACE)
                .addFilterSchemas(
                    (searchFunctionSpec.packageNames ?: emptySet()).flatMap {
                        listOf("AppFunctionComponentMetadataDocument-$it")
                    }
                )
                .addFilterPackageNames(SYSTEM_PACKAGE_NAME)
                .setVerbatimSearchEnabled(true)
                .setNumericSearchEnabled(true)
                .setListFilterQueryLanguageEnabled(true)
                .build()

        val sharedTopLevelComponentsByPackage: MutableMap<String, AppFunctionComponentsMetadata> =
            mutableMapOf()
        session.search("", topLevelComponentsSearchSpec).readAll { searchResult ->
            extractAppFunctionComponentsMetadataFromSearchResult(
                searchResult,
                sharedTopLevelComponentsByPackage
            )
        }

        return session
            .search(
                searchFunctionSpec.toStaticMetadataAppSearchQuery(),
                staticMetadataSearchSpecWithJoin
            )
            .readAll { searchResult ->
                convertSearchResultToAppFunctionMetadata(
                    searchResult,
                    sharedTopLevelComponentsByPackage
                )
            }
            .filterNotNull()
    }

    private fun extractAppFunctionComponentsMetadataFromSearchResult(
        searchResult: SearchResult,
        sharedTopLevelComponentsByPackage: MutableMap<String, AppFunctionComponentsMetadata>
    ) {
        val packageName =
            checkNotNull(searchResult.genericDocument.getPropertyString("packageName"))
        val componentMetadataSearchResult =
            searchResult.genericDocument
                .toDocumentClass(AppFunctionComponentsMetadataDocument::class.java)
                .toAppFunctionComponentsMetadata()
        // There is only a single component metadata per package, so we can safely overwrite the
        // existing value.
        if (componentMetadataSearchResult.dataTypes.isNotEmpty()) {
            sharedTopLevelComponentsByPackage[packageName] = componentMetadataSearchResult
        }
    }

    /**
     * Converts the [SearchResult] to an [AppFunctionMetadata].
     *
     * When the [searchResult] is schema-less function and the device does not have dynamic indexer
     * enabled, it is impossible to resolve the function signature information (e.g. parameters,
     * response). In such case, the function would return null.
     *
     * @return [AppFunctionMetadata] or null if unable to resolve the function signature.
     */
    private fun convertSearchResultToAppFunctionMetadata(
        searchResult: SearchResult,
        sharedTopLevelComponentsByPackage: Map<String, AppFunctionComponentsMetadata>,
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

        val schemaMetadata = buildSchemaMetadataFromGdForLegacyIndexer(searchResult.genericDocument)
        val parameterMetadata =
            getAppFunctionParameterMetadata(staticMetadataDocument, schemaMetadata) ?: return null
        val responseMetadata =
            getAppFunctionResponseMetadata(staticMetadataDocument, schemaMetadata) ?: return null
        val componentMetadata =
            getAppFunctionComponentsMetadata(
                packageName,
                staticMetadataDocument,
                schemaMetadata,
                sharedTopLevelComponentsByPackage
            ) ?: return null

        return AppFunctionMetadata(
            id = functionId,
            packageName = packageName,
            isEnabled = computeEffectivelyEnabled(staticMetadataDocument, runtimeMetadataDocument),
            schema = schemaMetadata,
            parameters = parameterMetadata,
            response = responseMetadata,
            components = componentMetadata,
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

    /**
     * Returns the [AppFunctionSchemaMetadata] of the given app function. Returns null if the
     * function is not implementing a predefined schema.
     *
     * @throws AppFunctionFunctionNotFoundException if the function does not exist.
     */
    override suspend fun getAppFunctionSchemaMetadata(
        functionId: String,
        packageName: String
    ): AppFunctionSchemaMetadata? {
        val documentId = getAppFunctionId(packageName, functionId)
        val result =
            createSearchSession(context = context)
                .use { session ->
                    session.getByDocumentIdAsync(
                        SYSTEM_PACKAGE_NAME,
                        APP_FUNCTIONS_STATIC_DATABASE_NAME,
                        GetByDocumentIdRequest.Builder(APP_FUNCTIONS_NAMESPACE)
                            .addIds(documentId)
                            .build()
                    )
                }
                .await()
        val genericDocument =
            result.successes[documentId]
                ?: throw AppFunctionFunctionNotFoundException(
                    "Function with ID = $documentId is not available"
                )
        return buildSchemaMetadataFromGdForLegacyIndexer(genericDocument)
    }

    private fun getAppFunctionId(packageName: String, functionId: String) =
        "$packageName/$functionId"

    private fun getAppFunctionParameterMetadata(
        appFunctionMetadataDocument: AppFunctionMetadataDocument,
        schemaMetadata: AppFunctionSchemaMetadata?,
    ): List<AppFunctionParameterMetadata>? {
        if (isAppFunctionMetadataDocumentFromDynamicIndexer(appFunctionMetadataDocument)) {
            // When the function does not have parameters, the document would be null instead of an
            // empty list.
            return appFunctionMetadataDocument.parameters?.map(
                AppFunctionParameterMetadataDocument::toAppFunctionParameterMetadata
            ) ?: emptyList()
        }

        return if (schemaMetadata == null) {
            null
        } else {
            schemaAppFunctionInventory
                ?.schemaFunctionsMap
                ?.get(prepareStaticMappingKey(schemaMetadata))
                ?.parameters
        }
    }

    private fun getAppFunctionResponseMetadata(
        appFunctionMetadataDocument: AppFunctionMetadataDocument,
        schemaMetadata: AppFunctionSchemaMetadata?,
    ): AppFunctionResponseMetadata? {
        if (isAppFunctionMetadataDocumentFromDynamicIndexer(appFunctionMetadataDocument)) {
            return checkNotNull(appFunctionMetadataDocument.response)
                .toAppFunctionResponseMetadata()
        }

        return if (schemaMetadata == null) {
            null
        } else {
            schemaAppFunctionInventory
                ?.schemaFunctionsMap
                ?.get(prepareStaticMappingKey(schemaMetadata))
                ?.response
        }
    }

    private fun getAppFunctionComponentsMetadata(
        packageName: String,
        appFunctionMetadataDocument: AppFunctionMetadataDocument,
        schemaMetadata: AppFunctionSchemaMetadata?,
        sharedTopLevelComponentsByPackage: Map<String, AppFunctionComponentsMetadata>,
    ): AppFunctionComponentsMetadata? {
        if (isAppFunctionMetadataDocumentFromDynamicIndexer(appFunctionMetadataDocument)) {
            return sharedTopLevelComponentsByPackage[packageName] ?: AppFunctionComponentsMetadata()
        }

        return if (schemaMetadata == null) {
            null
        } else {
            schemaAppFunctionInventory
                ?.schemaFunctionsMap
                ?.get(prepareStaticMappingKey(schemaMetadata))
                ?.components
        }
    }

    private fun isAppFunctionMetadataDocumentFromDynamicIndexer(
        document: AppFunctionMetadataDocument
    ): Boolean {
        return document.response != null
    }

    /**
     * Prepare the key for looking up [AppFunctionMetadata] from statically generated inventory.
     *
     * This is to ensure that when looking up [AppFunctionMetadata] for app that used legacy SDK, it
     * can still resolve the compatible version defined in Jetpack.
     */
    private fun prepareStaticMappingKey(
        schemaMetadata: AppFunctionSchemaMetadata
    ): AppFunctionSchemaMetadata {
        return AppFunctionSchemaMetadata(
            category = schemaMetadata.category,
            name = schemaMetadata.name,
            version = 2,
        )
    }

    private companion object {
        const val SYSTEM_PACKAGE_NAME = "android"
        const val APP_FUNCTIONS_NAMESPACE = "app_functions"
        const val APP_FUNCTIONS_RUNTIME_NAMESPACE = "app_functions_runtime"
        const val APP_FUNCTIONS_STATIC_DATABASE_NAME = "apps-db"

        val OBSERVER_DEBOUNCE_MILLIS = 1.seconds

        val RUNTIME_SEARCH_SPEC =
            SearchSpec.Builder()
                .addFilterNamespaces(APP_FUNCTIONS_RUNTIME_NAMESPACE)
                .addFilterDocumentClasses(AppFunctionRuntimeMetadata::class.java)
                .addFilterPackageNames(SYSTEM_PACKAGE_NAME)
                .setVerbatimSearchEnabled(true)
                .build()
    }
}
