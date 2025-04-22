/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.appsearch.localstorage.stats;

import static com.google.common.truth.Truth.assertThat;

import androidx.appsearch.app.AppSearchResult;
import androidx.appsearch.stats.SchemaMigrationStats;

import org.junit.Test;

public class AppSearchStatsTest {
    static final String TEST_PACKAGE_NAME = "com.google.test";
    static final String TEST_DATA_BASE = "testDataBase";
    static final int TEST_STATUS_CODE = AppSearchResult.RESULT_INTERNAL_ERROR;
    static final int TEST_TOTAL_LATENCY_MILLIS = 20;

    @Test
    public void testAppSearchStats_CallStats() {
        final int estimatedBinderLatencyMillis = 1;
        final int numOperationsSucceeded = 2;
        final int numOperationsFailed = 3;
        final @CallStats.CallType int callType =
                CallStats.CALL_TYPE_PUT_DOCUMENTS;

        final CallStats cStats = new CallStats.Builder()
                .setPackageName(TEST_PACKAGE_NAME)
                .setDatabase(TEST_DATA_BASE)
                .setStatusCode(TEST_STATUS_CODE)
                .setTotalLatencyMillis(TEST_TOTAL_LATENCY_MILLIS)
                .setCallType(callType)
                .setEstimatedBinderLatencyMillis(estimatedBinderLatencyMillis)
                .setNumOperationsSucceeded(numOperationsSucceeded)
                .setNumOperationsFailed(numOperationsFailed)
                .setLaunchVMEnabled(true)
                .build();

        assertThat(cStats.getPackageName()).isEqualTo(TEST_PACKAGE_NAME);
        assertThat(cStats.getDatabase()).isEqualTo(TEST_DATA_BASE);
        assertThat(cStats.getStatusCode()).isEqualTo(TEST_STATUS_CODE);
        assertThat(cStats.getTotalLatencyMillis()).isEqualTo(
                TEST_TOTAL_LATENCY_MILLIS);
        assertThat(cStats.getEstimatedBinderLatencyMillis())
                .isEqualTo(estimatedBinderLatencyMillis);
        assertThat(cStats.getCallType()).isEqualTo(callType);
        assertThat(cStats.getNumOperationsSucceeded()).isEqualTo(numOperationsSucceeded);
        assertThat(cStats.getNumOperationsFailed()).isEqualTo(numOperationsFailed);
        assertThat(cStats.getEnabledFeatures()).isEqualTo(1);
    }

    @Test
    public void testAppSearchStats_setLaunchVMEnabled_false() {
        final CallStats cStats = new CallStats.Builder()
                .setPackageName(TEST_PACKAGE_NAME)
                .setDatabase(TEST_DATA_BASE)
                .setStatusCode(TEST_STATUS_CODE)
                .setTotalLatencyMillis(TEST_TOTAL_LATENCY_MILLIS)
                .setLaunchVMEnabled(false)
                .build();

        assertThat(cStats.getPackageName()).isEqualTo(TEST_PACKAGE_NAME);
        assertThat(cStats.getDatabase()).isEqualTo(TEST_DATA_BASE);
        assertThat(cStats.getStatusCode()).isEqualTo(TEST_STATUS_CODE);
        assertThat(cStats.getTotalLatencyMillis()).isEqualTo(
                TEST_TOTAL_LATENCY_MILLIS);
        assertThat(cStats.getEnabledFeatures()).isEqualTo(0);
    }

    @Test
    public void testAppSearchCallStats_nullValues() {
        final @CallStats.CallType int callType =
                CallStats.CALL_TYPE_PUT_DOCUMENTS;

        final CallStats.Builder cStatsBuilder = new CallStats.Builder()
                .setCallType(callType);

        final CallStats cStats = cStatsBuilder.build();

        assertThat(cStats.getPackageName()).isNull();
        assertThat(cStats.getDatabase()).isNull();
        assertThat(cStats.getCallType()).isEqualTo(callType);
    }

    @Test
    public void testAppSearchStats_PutDocumentStats() {
        final int generateDocumentProtoLatencyMillis = 1;
        final int rewriteDocumentTypesLatencyMillis = 2;
        final int nativeLatencyMillis = 3;
        final int nativeDocumentStoreLatencyMillis = 4;
        final int nativeIndexLatencyMillis = 5;
        final int nativeIndexMergeLatencyMillis = 6;
        final int nativeDocumentSize = 7;
        final int nativeNumTokensIndexed = 8;
        final boolean nativeExceededMaxNumTokens = true;
        final int nativeTermIndexLatencyMillis = 9;
        final int nativeIntegerIndexLatencyMillis = 10;
        final int nativeQualifiedIdJoinIndexLatencyMillis = 11;
        final int nativeLiteIndexSortLatencyMillis = 12;
        final int enabledFeatures = 1;
        final PutDocumentStats.Builder pStatsBuilder =
                new PutDocumentStats.Builder(TEST_PACKAGE_NAME, TEST_DATA_BASE)
                        .setStatusCode(TEST_STATUS_CODE)
                        .setTotalLatencyMillis(TEST_TOTAL_LATENCY_MILLIS)
                        .setGenerateDocumentProtoLatencyMillis(generateDocumentProtoLatencyMillis)
                        .setRewriteDocumentTypesLatencyMillis(rewriteDocumentTypesLatencyMillis)
                        .setNativeLatencyMillis(nativeLatencyMillis)
                        .setNativeDocumentStoreLatencyMillis(nativeDocumentStoreLatencyMillis)
                        .setNativeIndexLatencyMillis(nativeIndexLatencyMillis)
                        .setNativeIndexMergeLatencyMillis(nativeIndexMergeLatencyMillis)
                        .setNativeDocumentSizeBytes(nativeDocumentSize)
                        .setNativeNumTokensIndexed(nativeNumTokensIndexed)
                        .setNativeTermIndexLatencyMillis(nativeTermIndexLatencyMillis)
                        .setNativeIntegerIndexLatencyMillis(nativeIntegerIndexLatencyMillis)
                        .setNativeQualifiedIdJoinIndexLatencyMillis(
                                nativeQualifiedIdJoinIndexLatencyMillis)
                        .setNativeLiteIndexSortLatencyMillis(nativeLiteIndexSortLatencyMillis)
                        .setLaunchVMEnabled(true);

        final PutDocumentStats pStats = pStatsBuilder.build();

        assertThat(pStats.getPackageName()).isEqualTo(TEST_PACKAGE_NAME);
        assertThat(pStats.getDatabase()).isEqualTo(TEST_DATA_BASE);
        assertThat(pStats.getStatusCode()).isEqualTo(TEST_STATUS_CODE);
        assertThat(pStats.getTotalLatencyMillis()).isEqualTo(
                TEST_TOTAL_LATENCY_MILLIS);
        assertThat(pStats.getGenerateDocumentProtoLatencyMillis()).isEqualTo(
                generateDocumentProtoLatencyMillis);
        assertThat(pStats.getRewriteDocumentTypesLatencyMillis()).isEqualTo(
                rewriteDocumentTypesLatencyMillis);
        assertThat(pStats.getNativeLatencyMillis()).isEqualTo(nativeLatencyMillis);
        assertThat(pStats.getNativeDocumentStoreLatencyMillis()).isEqualTo(
                nativeDocumentStoreLatencyMillis);
        assertThat(pStats.getNativeIndexLatencyMillis()).isEqualTo(nativeIndexLatencyMillis);
        assertThat(pStats.getNativeIndexMergeLatencyMillis()).isEqualTo(
                nativeIndexMergeLatencyMillis);
        assertThat(pStats.getNativeDocumentSizeBytes()).isEqualTo(nativeDocumentSize);
        assertThat(pStats.getNativeNumTokensIndexed()).isEqualTo(nativeNumTokensIndexed);
        assertThat(pStats.getNativeTermIndexLatencyMillis()).isEqualTo(
                nativeTermIndexLatencyMillis);
        assertThat(pStats.getNativeIntegerIndexLatencyMillis()).isEqualTo(
                nativeIntegerIndexLatencyMillis);
        assertThat(pStats.getNativeQualifiedIdJoinIndexLatencyMillis()).isEqualTo(
                nativeQualifiedIdJoinIndexLatencyMillis);
        assertThat(pStats.getNativeLiteIndexSortLatencyMillis()).isEqualTo(
                nativeLiteIndexSortLatencyMillis);
        assertThat(pStats.getEnabledFeatures()).isEqualTo(
                enabledFeatures);
    }

    @Test
    public void testAppSearchStats_InitializeStats() {
        int enabledFeatures = 1;
        int prepareSchemaAndNamespacesLatencyMillis = 1;
        int prepareVisibilityFileLatencyMillis = 2;
        int nativeLatencyMillis = 3;
        int nativeDocumentStoreRecoveryCause = InitializeStats.RECOVERY_CAUSE_DEPENDENCIES_CHANGED;
        int nativeIndexRestorationCause = InitializeStats.RECOVERY_CAUSE_FEATURE_FLAG_CHANGED;
        int nativeSchemaStoreRecoveryCause = InitializeStats.RECOVERY_CAUSE_IO_ERROR;
        int nativeDocumentStoreRecoveryLatencyMillis = 4;
        int nativeIndexRestorationLatencyMillis = 5;
        int nativeSchemaStoreRecoveryLatencyMillis = 6;
        int nativeDocumentStoreDataStatus = 7;
        int nativeNumDocuments = 8;
        int nativeNumSchemaTypes = 9;
        int numPreviousInitFailures = 10;
        int integerIndexRestorationCause = InitializeStats.RECOVERY_CAUSE_DATA_LOSS;
        int qualifiedIdJoinIndexRestorationCause =
                InitializeStats.RECOVERY_CAUSE_INCONSISTENT_WITH_GROUND_TRUTH;
        int embeddingIndexRestorationCause = InitializeStats.RECOVERY_CAUSE_DATA_LOSS;
        int initializeIcuDataStatusCode = 11;
        int numFailedReindexedDocuments = 12;

        final InitializeStats.Builder iStatsBuilder = new InitializeStats.Builder()
                .setStatusCode(TEST_STATUS_CODE)
                .setTotalLatencyMillis(TEST_TOTAL_LATENCY_MILLIS)
                .setHasDeSync(/* hasDeSyncs= */ true)
                .setPrepareSchemaAndNamespacesLatencyMillis(prepareSchemaAndNamespacesLatencyMillis)
                .setPrepareVisibilityStoreLatencyMillis(prepareVisibilityFileLatencyMillis)
                .setNativeLatencyMillis(nativeLatencyMillis)
                .setNativeDocumentStoreRecoveryCause(nativeDocumentStoreRecoveryCause)
                .setNativeIndexRestorationCause(nativeIndexRestorationCause)
                .setNativeSchemaStoreRecoveryCause(nativeSchemaStoreRecoveryCause)
                .setNativeDocumentStoreRecoveryLatencyMillis(
                        nativeDocumentStoreRecoveryLatencyMillis)
                .setNativeIndexRestorationLatencyMillis(nativeIndexRestorationLatencyMillis)
                .setNativeSchemaStoreRecoveryLatencyMillis(nativeSchemaStoreRecoveryLatencyMillis)
                .setNativeDocumentStoreDataStatus(nativeDocumentStoreDataStatus)
                .setNativeDocumentCount(nativeNumDocuments)
                .setNativeSchemaTypeCount(nativeNumSchemaTypes)
                .setHasReset(true)
                .setResetStatusCode(AppSearchResult.RESULT_INVALID_SCHEMA)
                .setLaunchVMEnabled(true)
                .setNativeNumPreviousInitFailures(numPreviousInitFailures)
                .setNativeIntegerIndexRestorationCause(integerIndexRestorationCause)
                .setNativeQualifiedIdJoinIndexRestorationCause(qualifiedIdJoinIndexRestorationCause)
                .setNativeEmbeddingIndexRestorationCause(embeddingIndexRestorationCause)
                .setNativeInitializeIcuDataStatusCode(initializeIcuDataStatusCode)
                .setNativeNumFailedReindexedDocuments(numFailedReindexedDocuments);
        final InitializeStats iStats = iStatsBuilder.build();

        assertThat(iStats.getStatusCode()).isEqualTo(TEST_STATUS_CODE);
        assertThat(iStats.getTotalLatencyMillis()).isEqualTo(
                TEST_TOTAL_LATENCY_MILLIS);
        assertThat(iStats.hasDeSync()).isTrue();
        assertThat(iStats.getPrepareSchemaAndNamespacesLatencyMillis()).isEqualTo(
                prepareSchemaAndNamespacesLatencyMillis);
        assertThat(iStats.getPrepareVisibilityStoreLatencyMillis()).isEqualTo(
                prepareVisibilityFileLatencyMillis);
        assertThat(iStats.getNativeLatencyMillis()).isEqualTo(nativeLatencyMillis);
        assertThat(iStats.getNativeDocumentStoreRecoveryCause()).isEqualTo(
                nativeDocumentStoreRecoveryCause);
        assertThat(iStats.getNativeIndexRestorationCause()).isEqualTo(nativeIndexRestorationCause);
        assertThat(iStats.getNativeSchemaStoreRecoveryCause()).isEqualTo(
                nativeSchemaStoreRecoveryCause);
        assertThat(iStats.getNativeDocumentStoreRecoveryLatencyMillis()).isEqualTo(
                nativeDocumentStoreRecoveryLatencyMillis);
        assertThat(iStats.getNativeIndexRestorationLatencyMillis()).isEqualTo(
                nativeIndexRestorationLatencyMillis);
        assertThat(iStats.getNativeSchemaStoreRecoveryLatencyMillis()).isEqualTo(
                nativeSchemaStoreRecoveryLatencyMillis);
        assertThat(iStats.getNativeDocumentStoreDataStatus()).isEqualTo(
                nativeDocumentStoreDataStatus);
        assertThat(iStats.getNativeDocumentCount()).isEqualTo(nativeNumDocuments);
        assertThat(iStats.getNativeSchemaTypeCount()).isEqualTo(nativeNumSchemaTypes);
        assertThat(iStats.hasReset()).isTrue();
        assertThat(iStats.getResetStatusCode()).isEqualTo(AppSearchResult.RESULT_INVALID_SCHEMA);
        assertThat(iStats.getEnabledFeatures()).isEqualTo(enabledFeatures);
        assertThat(iStats.getNativeNumPreviousInitFailures()).isEqualTo(numPreviousInitFailures);
        assertThat(iStats.getNativeIntegerIndexRestorationCause())
                .isEqualTo(integerIndexRestorationCause);
        assertThat(iStats.getNativeQualifiedIdJoinIndexRestorationCause())
                .isEqualTo(qualifiedIdJoinIndexRestorationCause);
        assertThat(iStats.getNativeEmbeddingIndexRestorationCause())
                .isEqualTo(embeddingIndexRestorationCause);
        assertThat(iStats.getNativeInitializeIcuDataStatusCode())
                .isEqualTo(initializeIcuDataStatusCode);
        assertThat(iStats.getNativeNumFailedReindexedDocuments())
                .isEqualTo(numFailedReindexedDocuments);
    }

    @Test
    public void testAppSearchStats_SearchStats() {
        int rewriteSearchSpecLatencyMillis = 1;
        int rewriteSearchResultLatencyMillis = 2;
        int javaLockAcquisitionLatencyMillis = 3;
        int aclCheckLatencyMillis = 4;
        int visibilityScope = SearchStats.VISIBILITY_SCOPE_LOCAL;
        int nativeLatencyMillis = 6;
        int nativeNumTerms = 7;
        int nativeQueryLength = 8;
        int nativeNumNamespacesFiltered = 9;
        int nativeNumSchemaTypesFiltered = 10;
        int nativeRequestedPageSize = 11;
        int nativeNumResultsReturnedCurrentPage = 12;
        boolean nativeIsFirstPage = true;
        int nativeParseQueryLatencyMillis = 13;
        int nativeRankingStrategy = 14;
        int nativeNumDocumentsScored = 15;
        int nativeScoringLatencyMillis = 16;
        int nativeRankingLatencyMillis = 17;
        int nativeNumResultsSnippeted = 18;
        int nativeDocumentRetrievingLatencyMillis = 19;
        int nativeLockAcquisitionLatencyMillis = 20;
        int javaToNativeJniLatencyMillis = 21;
        int nativeToJavaJniLatencyMillis = 22;
        String searchSourceLogTag = "tag";
        int enabledFeatures = 1;
        final SearchStats.Builder sStatsBuilder = new SearchStats.Builder(visibilityScope,
                TEST_PACKAGE_NAME)
                .setDatabase(TEST_DATA_BASE)
                .setStatusCode(TEST_STATUS_CODE)
                .setTotalLatencyMillis(TEST_TOTAL_LATENCY_MILLIS)
                .setRewriteSearchSpecLatencyMillis(rewriteSearchSpecLatencyMillis)
                .setRewriteSearchResultLatencyMillis(rewriteSearchResultLatencyMillis)
                .setJavaLockAcquisitionLatencyMillis(javaLockAcquisitionLatencyMillis)
                .setAclCheckLatencyMillis(aclCheckLatencyMillis)
                .setNativeLatencyMillis(nativeLatencyMillis)
                .setTermCount(nativeNumTerms)
                .setQueryLength(nativeQueryLength)
                .setFilteredNamespaceCount(nativeNumNamespacesFiltered)
                .setFilteredSchemaTypeCount(nativeNumSchemaTypesFiltered)
                .setRequestedPageSize(nativeRequestedPageSize)
                .setCurrentPageReturnedResultCount(nativeNumResultsReturnedCurrentPage)
                .setIsFirstPage(nativeIsFirstPage)
                .setParseQueryLatencyMillis(nativeParseQueryLatencyMillis)
                .setRankingStrategy(nativeRankingStrategy)
                .setScoredDocumentCount(nativeNumDocumentsScored)
                .setScoringLatencyMillis(nativeScoringLatencyMillis)
                .setRankingLatencyMillis(nativeRankingLatencyMillis)
                .setResultWithSnippetsCount(nativeNumResultsSnippeted)
                .setDocumentRetrievingLatencyMillis(nativeDocumentRetrievingLatencyMillis)
                .setNativeLockAcquisitionLatencyMillis(nativeLockAcquisitionLatencyMillis)
                .setJavaToNativeJniLatencyMillis(javaToNativeJniLatencyMillis)
                .setNativeToJavaJniLatencyMillis(nativeToJavaJniLatencyMillis)
                .setSearchSourceLogTag(searchSourceLogTag)
                .setLaunchVMEnabled(true);
        final SearchStats sStats = sStatsBuilder.build();

        assertThat(sStats.getPackageName()).isEqualTo(TEST_PACKAGE_NAME);
        assertThat(sStats.getDatabase()).isEqualTo(TEST_DATA_BASE);
        assertThat(sStats.getStatusCode()).isEqualTo(TEST_STATUS_CODE);
        assertThat(sStats.getTotalLatencyMillis()).isEqualTo(
                TEST_TOTAL_LATENCY_MILLIS);
        assertThat(sStats.getRewriteSearchSpecLatencyMillis()).isEqualTo(
                rewriteSearchSpecLatencyMillis);
        assertThat(sStats.getRewriteSearchResultLatencyMillis()).isEqualTo(
                rewriteSearchResultLatencyMillis);
        assertThat(sStats.getJavaLockAcquisitionLatencyMillis()).isEqualTo(
                javaLockAcquisitionLatencyMillis);
        assertThat(sStats.getAclCheckLatencyMillis()).isEqualTo(
                aclCheckLatencyMillis);
        assertThat(sStats.getVisibilityScope()).isEqualTo(visibilityScope);
        assertThat(sStats.getNativeLatencyMillis()).isEqualTo(nativeLatencyMillis);
        assertThat(sStats.getTermCount()).isEqualTo(nativeNumTerms);
        assertThat(sStats.getQueryLength()).isEqualTo(nativeQueryLength);
        assertThat(sStats.getFilteredNamespaceCount()).isEqualTo(nativeNumNamespacesFiltered);
        assertThat(sStats.getFilteredSchemaTypeCount()).isEqualTo(
                nativeNumSchemaTypesFiltered);
        assertThat(sStats.getRequestedPageSize()).isEqualTo(nativeRequestedPageSize);
        assertThat(sStats.getCurrentPageReturnedResultCount()).isEqualTo(
                nativeNumResultsReturnedCurrentPage);
        assertThat(sStats.isFirstPage()).isTrue();
        assertThat(sStats.getParseQueryLatencyMillis()).isEqualTo(
                nativeParseQueryLatencyMillis);
        assertThat(sStats.getRankingStrategy()).isEqualTo(nativeRankingStrategy);
        assertThat(sStats.getScoredDocumentCount()).isEqualTo(nativeNumDocumentsScored);
        assertThat(sStats.getScoringLatencyMillis()).isEqualTo(nativeScoringLatencyMillis);
        assertThat(sStats.getRankingLatencyMillis()).isEqualTo(nativeRankingLatencyMillis);
        assertThat(sStats.getResultWithSnippetsCount()).isEqualTo(nativeNumResultsSnippeted);
        assertThat(sStats.getDocumentRetrievingLatencyMillis()).isEqualTo(
                nativeDocumentRetrievingLatencyMillis);
        assertThat(sStats.getNativeLockAcquisitionLatencyMillis()).isEqualTo(
                nativeLockAcquisitionLatencyMillis);
        assertThat(sStats.getJavaToNativeJniLatencyMillis()).isEqualTo(
                javaToNativeJniLatencyMillis);
        assertThat(sStats.getNativeToJavaJniLatencyMillis()).isEqualTo(
                nativeToJavaJniLatencyMillis);
        assertThat(sStats.getSearchSourceLogTag()).isEqualTo(searchSourceLogTag);
        assertThat(sStats.getEnabledFeatures()).isEqualTo(enabledFeatures);
    }

    @Test
    public void testAppSearchStats_SetSchemaStats() {
        int newTypeCount = 1;
        int deletedTypeCount = 2;
        int compatibleTypeChangeCount = 3;
        int indexIncompatibleTypeChangeCount = 4;
        int backwardsIncompatibleTypeChangeCount = 5;
        int verifyIncomingCallLatencyMillis = 6;
        int executorAcquisitionLatencyMillis = 7;
        int rebuildFromBundleLatencyMillis = 8;
        int javaLockAcquisitionLatencyMillis = 9;
        int totalNativeLatencyMillis = 10;
        int rewriteSchemaLatencyMillis = 11;
        int visibilitySettingLatencyMillis = 12;
        int convertToResponseLatencyMillis = 13;
        int dispatchChangeNotificationsLatencyMillis = 14;
        int optimizeLatencyMillis = 15;
        boolean isPackageObserved = true;
        int getOldSchemaLatencyMillis = 16;
        int getObserverLatencyMillis = 17;
        int sendNotificationLatencyMillis = 18;
        int enabledFeatures = 1;
        SetSchemaStats sStats = new SetSchemaStats.Builder(TEST_PACKAGE_NAME, TEST_DATA_BASE)
                .setStatusCode(TEST_STATUS_CODE)
                .setTotalLatencyMillis(TEST_TOTAL_LATENCY_MILLIS)
                .setNewTypeCount(newTypeCount)
                .setDeletedTypeCount(deletedTypeCount)
                .setCompatibleTypeChangeCount(compatibleTypeChangeCount)
                .setIndexIncompatibleTypeChangeCount(indexIncompatibleTypeChangeCount)
                .setBackwardsIncompatibleTypeChangeCount(backwardsIncompatibleTypeChangeCount)
                .setVerifyIncomingCallLatencyMillis(verifyIncomingCallLatencyMillis)
                .setExecutorAcquisitionLatencyMillis(executorAcquisitionLatencyMillis)
                .setRebuildFromBundleLatencyMillis(rebuildFromBundleLatencyMillis)
                .setJavaLockAcquisitionLatencyMillis(javaLockAcquisitionLatencyMillis)
                .setRewriteSchemaLatencyMillis(rewriteSchemaLatencyMillis)
                .setTotalNativeLatencyMillis(totalNativeLatencyMillis)
                .setVisibilitySettingLatencyMillis(visibilitySettingLatencyMillis)
                .setConvertToResponseLatencyMillis(convertToResponseLatencyMillis)
                .setDispatchChangeNotificationsLatencyMillis(
                        dispatchChangeNotificationsLatencyMillis)
                .setOptimizeLatencyMillis(optimizeLatencyMillis)
                .setIsPackageObserved(isPackageObserved)
                .setGetOldSchemaLatencyMillis(getOldSchemaLatencyMillis)
                .setGetObserverLatencyMillis(getObserverLatencyMillis)
                .setPreparingChangeNotificationLatencyMillis(sendNotificationLatencyMillis)
                .setSchemaMigrationCallType(SchemaMigrationStats.SECOND_CALL_APPLY_NEW_SCHEMA)
                .setLaunchVMEnabled(true)
                .build();

        assertThat(sStats.getPackageName()).isEqualTo(TEST_PACKAGE_NAME);
        assertThat(sStats.getDatabase()).isEqualTo(TEST_DATA_BASE);
        assertThat(sStats.getStatusCode()).isEqualTo(TEST_STATUS_CODE);
        assertThat(sStats.getTotalLatencyMillis()).isEqualTo(
                TEST_TOTAL_LATENCY_MILLIS);
        assertThat(sStats.getNewTypeCount()).isEqualTo(newTypeCount);
        assertThat(sStats.getDeletedTypeCount()).isEqualTo(deletedTypeCount);
        assertThat(sStats.getCompatibleTypeChangeCount()).isEqualTo(compatibleTypeChangeCount);
        assertThat(sStats.getIndexIncompatibleTypeChangeCount()).isEqualTo(
                indexIncompatibleTypeChangeCount);
        assertThat(sStats.getBackwardsIncompatibleTypeChangeCount()).isEqualTo(
                backwardsIncompatibleTypeChangeCount);
        assertThat(sStats.getVerifyIncomingCallLatencyMillis()).isEqualTo(
                verifyIncomingCallLatencyMillis);
        assertThat(sStats.getExecutorAcquisitionLatencyMillis()).isEqualTo(
                executorAcquisitionLatencyMillis);
        assertThat(sStats.getRebuildFromBundleLatencyMillis()).isEqualTo(
                rebuildFromBundleLatencyMillis);
        assertThat(sStats.getJavaLockAcquisitionLatencyMillis()).isEqualTo(
                javaLockAcquisitionLatencyMillis);
        assertThat(sStats.getRewriteSchemaLatencyMillis()).isEqualTo(rewriteSchemaLatencyMillis);
        assertThat(sStats.getTotalNativeLatencyMillis()).isEqualTo(totalNativeLatencyMillis);
        assertThat(sStats.getVisibilitySettingLatencyMillis()).isEqualTo(
                visibilitySettingLatencyMillis);
        assertThat(sStats.getConvertToResponseLatencyMillis()).isEqualTo(
                convertToResponseLatencyMillis);
        assertThat(sStats.getDispatchChangeNotificationsLatencyMillis()).isEqualTo(
                dispatchChangeNotificationsLatencyMillis);
        assertThat(sStats.getOptimizeLatencyMillis()).isEqualTo(optimizeLatencyMillis);
        assertThat(sStats.isPackageObserved()).isEqualTo(isPackageObserved);
        assertThat(sStats.getGetOldSchemaLatencyMillis()).isEqualTo(getOldSchemaLatencyMillis);
        assertThat(sStats.getGetObserverLatencyMillis()).isEqualTo(getObserverLatencyMillis);
        assertThat(sStats.getPreparingChangeNotificationLatencyMillis())
                .isEqualTo(sendNotificationLatencyMillis);
        assertThat(sStats.getSchemaMigrationCallType())
                .isEqualTo(SchemaMigrationStats.SECOND_CALL_APPLY_NEW_SCHEMA);
        assertThat(sStats.getEnabledFeatures())
                .isEqualTo(enabledFeatures);
    }

    @Test
    public void testAppSearchStats_SchemaMigrationStats() {
        int executorAcquisitionLatencyMillis = 1;
        int getSchemaLatency = 2;
        int queryAndTransformLatency = 3;
        int firstSetSchemaLatency = 4;
        boolean isFirstSetSchemaSuccess = true;
        int secondSetSchemaLatency = 5;
        int saveDocumentLatency = 6;
        int migratedDocumentCount = 7;
        int savedDocumentCount = 8;
        int migrationFailureCount = 9;
        SchemaMigrationStats sStats = new SchemaMigrationStats.Builder(
                TEST_PACKAGE_NAME, TEST_DATA_BASE)
                .setStatusCode(TEST_STATUS_CODE)
                .setTotalLatencyMillis(TEST_TOTAL_LATENCY_MILLIS)
                .setExecutorAcquisitionLatencyMillis(executorAcquisitionLatencyMillis)
                .setGetSchemaLatencyMillis(getSchemaLatency)
                .setQueryAndTransformLatencyMillis(queryAndTransformLatency)
                .setFirstSetSchemaLatencyMillis(firstSetSchemaLatency)
                .setIsFirstSetSchemaSuccess(isFirstSetSchemaSuccess)
                .setSecondSetSchemaLatencyMillis(secondSetSchemaLatency)
                .setSaveDocumentLatencyMillis(saveDocumentLatency)
                .setTotalNeedMigratedDocumentCount(migratedDocumentCount)
                .setTotalSuccessMigratedDocumentCount(savedDocumentCount)
                .setMigrationFailureCount(migrationFailureCount)
                .build();

        assertThat(sStats.getPackageName()).isEqualTo(TEST_PACKAGE_NAME);
        assertThat(sStats.getDatabase()).isEqualTo(TEST_DATA_BASE);
        assertThat(sStats.getStatusCode()).isEqualTo(TEST_STATUS_CODE);
        assertThat(sStats.getTotalLatencyMillis()).isEqualTo(
                TEST_TOTAL_LATENCY_MILLIS);
        assertThat(sStats.getExecutorAcquisitionLatencyMillis())
                .isEqualTo(executorAcquisitionLatencyMillis);
        assertThat(sStats.getGetSchemaLatencyMillis()).isEqualTo(getSchemaLatency);
        assertThat(sStats.getQueryAndTransformLatencyMillis()).isEqualTo(queryAndTransformLatency);
        assertThat(sStats.getFirstSetSchemaLatencyMillis()).isEqualTo(firstSetSchemaLatency);
        assertThat(sStats.isFirstSetSchemaSuccess()).isEqualTo(isFirstSetSchemaSuccess);
        assertThat(sStats.getSecondSetSchemaLatencyMillis()).isEqualTo(secondSetSchemaLatency);
        assertThat(sStats.getSaveDocumentLatencyMillis()).isEqualTo(saveDocumentLatency);
        assertThat(sStats.getTotalNeedMigratedDocumentCount()).isEqualTo(migratedDocumentCount);
        assertThat(sStats.getTotalSuccessMigratedDocumentCount()).isEqualTo(savedDocumentCount);
        assertThat(sStats.getMigrationFailureCount()).isEqualTo(migrationFailureCount);
    }

    @Test
    public void testAppSearchStats_RemoveStats() {
        int nativeLatencyMillis = 1;
        @RemoveStats.DeleteType int deleteType = 2;
        int documentDeletedCount = 3;
        int enabledFeatures = 1;

        final RemoveStats rStats = new RemoveStats.Builder(TEST_PACKAGE_NAME,
                TEST_DATA_BASE)
                .setStatusCode(TEST_STATUS_CODE)
                .setTotalLatencyMillis(TEST_TOTAL_LATENCY_MILLIS)
                .setNativeLatencyMillis(nativeLatencyMillis)
                .setDeleteType(deleteType)
                .setDeletedDocumentCount(documentDeletedCount)
                .setLaunchVMEnabled(true)
                .build();


        assertThat(rStats.getPackageName()).isEqualTo(TEST_PACKAGE_NAME);
        assertThat(rStats.getDatabase()).isEqualTo(TEST_DATA_BASE);
        assertThat(rStats.getStatusCode()).isEqualTo(TEST_STATUS_CODE);
        assertThat(rStats.getTotalLatencyMillis()).isEqualTo(TEST_TOTAL_LATENCY_MILLIS);
        assertThat(rStats.getNativeLatencyMillis()).isEqualTo(nativeLatencyMillis);
        assertThat(rStats.getDeleteType()).isEqualTo(deleteType);
        assertThat(rStats.getDeletedDocumentCount()).isEqualTo(documentDeletedCount);
        assertThat(rStats.getEnabledFeatures()).isEqualTo(enabledFeatures);
    }

    @Test
    public void testAppSearchStats_OptimizeStats() {
        int nativeLatencyMillis = 1;
        int nativeDocumentStoreOptimizeLatencyMillis = 2;
        int nativeIndexRestorationLatencyMillis = 3;
        int nativeNumOriginalDocuments = 4;
        int nativeNumDeletedDocuments = 5;
        int nativeNumExpiredDocuments = 6;
        int enabledFeatures = 1;
        long nativeStorageSizeBeforeBytes = Integer.MAX_VALUE + 1;
        long nativeStorageSizeAfterBytes = Integer.MAX_VALUE + 2;
        long nativeTimeSinceLastOptimizeMillis = Integer.MAX_VALUE + 3;

        final OptimizeStats oStats = new OptimizeStats.Builder()
                .setStatusCode(TEST_STATUS_CODE)
                .setTotalLatencyMillis(TEST_TOTAL_LATENCY_MILLIS)
                .setNativeLatencyMillis(nativeLatencyMillis)
                .setDocumentStoreOptimizeLatencyMillis(nativeDocumentStoreOptimizeLatencyMillis)
                .setIndexRestorationLatencyMillis(nativeIndexRestorationLatencyMillis)
                .setOriginalDocumentCount(nativeNumOriginalDocuments)
                .setDeletedDocumentCount(nativeNumDeletedDocuments)
                .setExpiredDocumentCount(nativeNumExpiredDocuments)
                .setStorageSizeBeforeBytes(nativeStorageSizeBeforeBytes)
                .setStorageSizeAfterBytes(nativeStorageSizeAfterBytes)
                .setTimeSinceLastOptimizeMillis(nativeTimeSinceLastOptimizeMillis)
                .setLaunchVMEnabled(true)
                .build();

        assertThat(oStats.getStatusCode()).isEqualTo(TEST_STATUS_CODE);
        assertThat(oStats.getTotalLatencyMillis()).isEqualTo(TEST_TOTAL_LATENCY_MILLIS);
        assertThat(oStats.getNativeLatencyMillis()).isEqualTo(nativeLatencyMillis);
        assertThat(oStats.getNativeLatencyMillis()).isEqualTo(nativeLatencyMillis);
        assertThat(oStats.getDocumentStoreOptimizeLatencyMillis()).isEqualTo(
                nativeDocumentStoreOptimizeLatencyMillis);
        assertThat(oStats.getIndexRestorationLatencyMillis()).isEqualTo(
                nativeIndexRestorationLatencyMillis);
        assertThat(oStats.getOriginalDocumentCount()).isEqualTo(nativeNumOriginalDocuments);
        assertThat(oStats.getDeletedDocumentCount()).isEqualTo(nativeNumDeletedDocuments);
        assertThat(oStats.getExpiredDocumentCount()).isEqualTo(nativeNumExpiredDocuments);
        assertThat(oStats.getStorageSizeBeforeBytes()).isEqualTo(nativeStorageSizeBeforeBytes);
        assertThat(oStats.getStorageSizeAfterBytes()).isEqualTo(nativeStorageSizeAfterBytes);
        assertThat(oStats.getTimeSinceLastOptimizeMillis()).isEqualTo(
                nativeTimeSinceLastOptimizeMillis);
        assertThat(oStats.getEnabledFeatures()).isEqualTo(enabledFeatures);
    }
}
