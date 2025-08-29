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
import androidx.appsearch.stats.BaseStats;
import androidx.appsearch.stats.SchemaMigrationStats;

import com.google.android.icing.proto.PersistType;

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
        final int javaLockAcquisitionLatencyMillis = 4;
        final @CallStats.CallType int lastBlockingOperation = BaseStats.CALL_TYPE_REMOVE_BLOB;
        final int lastBlockingOperationLatencyMillis = 6;
        final int getVmLatency1 = 7;
        final int getVmLatency2 = 8;
        final int unblockedAppSearchLatencyMillis = 9;

        final @CallStats.CallType int callType =
                BaseStats.CALL_TYPE_PUT_DOCUMENTS;

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
                .setJavaLockAcquisitionLatencyMillis(javaLockAcquisitionLatencyMillis)
                .setLastBlockingOperation(lastBlockingOperation)
                .setLastBlockingOperationLatencyMillis(lastBlockingOperationLatencyMillis)
                .addGetVmLatencyMillis(getVmLatency1)
                .addGetVmLatencyMillis(getVmLatency2)
                .setUnblockedAppSearchLatencyMillis(unblockedAppSearchLatencyMillis)
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
        assertThat(cStats.getJavaLockAcquisitionLatencyMillis())
                .isEqualTo(javaLockAcquisitionLatencyMillis);
        assertThat(cStats.getLastBlockingOperation()).isEqualTo(lastBlockingOperation);
        assertThat(cStats.getLastBlockingOperationLatencyMillis())
                .isEqualTo(lastBlockingOperationLatencyMillis);
        assertThat(cStats.getGetVmLatencyMillis())
                .isEqualTo(getVmLatency1 + getVmLatency2);
        assertThat(cStats.getUnblockedAppSearchLatencyMillis())
                .isEqualTo(unblockedAppSearchLatencyMillis);
        assertThat(cStats.getNumIcingCalls())
                .isEqualTo(2);
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
                BaseStats.CALL_TYPE_PUT_DOCUMENTS;

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
        int metadataTermIndexLatencyMillis = 13;
        int embeddingIndexLatencyMillis = 14;
        final int javaLockAcquisitionLatencyMillis = 15;
        final int lastBlockingOperation = 16;
        final int lastBlockingOperationLatencyMillis = 17;
        final int getVmLatencyMillis = 18;
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
                        .setLaunchVMEnabled(true)
                        .setMetadataTermIndexLatencyMillis(metadataTermIndexLatencyMillis)
                        .setEmbeddingIndexLatencyMillis(embeddingIndexLatencyMillis)
                        .setJavaLockAcquisitionLatencyMillis(javaLockAcquisitionLatencyMillis)
                        .setLastBlockingOperation(lastBlockingOperation)
                        .setLastBlockingOperationLatencyMillis(lastBlockingOperationLatencyMillis)
                        .addGetVmLatencyMillis(getVmLatencyMillis);

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
        assertThat(pStats.getMetadataTermIndexLatencyMillis()).isEqualTo(
                metadataTermIndexLatencyMillis);
        assertThat(pStats.getEmbeddingIndexLatencyMillis()).isEqualTo(
                embeddingIndexLatencyMillis);
        assertThat(pStats.getJavaLockAcquisitionLatencyMillis())
                .isEqualTo(javaLockAcquisitionLatencyMillis);
        assertThat(pStats.getLastBlockingOperation()).isEqualTo(lastBlockingOperation);
        assertThat(pStats.getLastBlockingOperationLatencyMillis())
                .isEqualTo(lastBlockingOperationLatencyMillis);
        assertThat(pStats.getGetVmLatencyMillis()).isEqualTo(getVmLatencyMillis);
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
        final int javaLockAcquisitionLatencyMillis = 13;
        final int lastBlockingOperation = 14;
        final int lastBlockingOperationLatencyMillis = 15;
        int getVmLatencyMillis = 16;

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
                .setNativeNumFailedReindexedDocuments(numFailedReindexedDocuments)
                .setJavaLockAcquisitionLatencyMillis(javaLockAcquisitionLatencyMillis)
                .setLastBlockingOperation(lastBlockingOperation)
                .setLastBlockingOperationLatencyMillis(lastBlockingOperationLatencyMillis)
                .addGetVmLatencyMillis(getVmLatencyMillis);
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
        assertThat(iStats.getJavaLockAcquisitionLatencyMillis())
                .isEqualTo(javaLockAcquisitionLatencyMillis);
        assertThat(iStats.getLastBlockingOperation()).isEqualTo(lastBlockingOperation);
        assertThat(iStats.getLastBlockingOperationLatencyMillis())
                .isEqualTo(lastBlockingOperationLatencyMillis);
        assertThat(iStats.getGetVmLatencyMillis()).isEqualTo(getVmLatencyMillis);
    }

    @Test
    public void testAppSearchStats_SearchStats() {
        int nativeQueryLength = 1;
        int nativeNumTerms = 2;
        int nativeNumNamespacesFiltered = 3;
        int nativeNumSchemaTypesFiltered = 4;
        int nativeRankingStrategy = 5;
        int nativeNumDocumentsScored = 6;
        int nativeParseQueryLatencyMillis = 7;
        int nativeScoringLatencyMillis = 8;
        boolean isNumericQuery = true;
        int numFetchedHitsLiteIndex = 9;
        int numFetchedHitsMainIndex = 10;
        int numFetchedHitsIntegerIndex = 11;
        int queryProcessorLexerExtractTokenLatencyMillis = 12;
        int queryProcessorParserConsumeQueryLatencyMillis = 13;
        int queryProcessorQueryVisitorLatencyMillis = 14;

        final SearchStats.Builder sStatsBuilder = new SearchStats.Builder()
                .setNativeQueryLength(nativeQueryLength)
                .setNativeTermCount(nativeNumTerms)
                .setNativeFilteredNamespaceCount(nativeNumNamespacesFiltered)
                .setNativeFilteredSchemaTypeCount(nativeNumSchemaTypesFiltered)
                .setNativeParseQueryLatencyMillis(nativeParseQueryLatencyMillis)
                .setNativeRankingStrategy(nativeRankingStrategy)
                .setNativeQueryProcessorParserConsumeQueryLatencyMillis(
                        nativeParseQueryLatencyMillis)
                .setNativeScoredDocumentCount(nativeNumDocumentsScored)
                .setNativeScoringLatencyMillis(nativeScoringLatencyMillis)
                .setNativeIsNumericQuery(isNumericQuery)
                .setNativeNumFetchedHitsLiteIndex(numFetchedHitsLiteIndex)
                .setNativeNumFetchedHitsMainIndex(numFetchedHitsMainIndex)
                .setNativeNumFetchedHitsIntegerIndex(numFetchedHitsIntegerIndex)
                .setNativeQueryProcessorLexerExtractTokenLatencyMillis(
                        queryProcessorLexerExtractTokenLatencyMillis)
                .setNativeQueryProcessorParserConsumeQueryLatencyMillis(
                        queryProcessorParserConsumeQueryLatencyMillis)
                .setNativeQueryProcessorQueryVisitorLatencyMillis(
                        queryProcessorQueryVisitorLatencyMillis);
        final SearchStats sStats = sStatsBuilder.build();

        assertThat(sStats.getNativeQueryLength()).isEqualTo(nativeQueryLength);
        assertThat(sStats.getNativeTermCount()).isEqualTo(nativeNumTerms);
        assertThat(sStats.getNativeFilteredNamespaceCount()).isEqualTo(nativeNumNamespacesFiltered);
        assertThat(sStats.getNativeFilteredSchemaTypeCount()).isEqualTo(
                nativeNumSchemaTypesFiltered);
        assertThat(sStats.getNativeRankingStrategy()).isEqualTo(nativeRankingStrategy);
        assertThat(sStats.getNativeParseQueryLatencyMillis()).isEqualTo(
                nativeParseQueryLatencyMillis);
        assertThat(sStats.getNativeScoredDocumentCount()).isEqualTo(nativeNumDocumentsScored);
        assertThat(sStats.getNativeScoringLatencyMillis()).isEqualTo(nativeScoringLatencyMillis);
        assertThat(sStats.isNativeNumericQuery()).isEqualTo(isNumericQuery);
        assertThat(sStats.getNativeNumFetchedHitsLiteIndex()).isEqualTo(numFetchedHitsLiteIndex);
        assertThat(sStats.getNativeNumFetchedHitsMainIndex()).isEqualTo(numFetchedHitsMainIndex);
        assertThat(sStats.getNativeNumFetchedHitsIntegerIndex())
                .isEqualTo(numFetchedHitsIntegerIndex);
        assertThat(sStats.getNativeQueryProcessorLexerExtractTokenLatencyMillis())
                .isEqualTo(queryProcessorLexerExtractTokenLatencyMillis);
        assertThat(sStats.getNativeQueryProcessorParserConsumeQueryLatencyMillis())
                .isEqualTo(queryProcessorParserConsumeQueryLatencyMillis);
        assertThat(sStats.getNativeQueryProcessorQueryVisitorLatencyMillis())
                .isEqualTo(queryProcessorQueryVisitorLatencyMillis);
        String expectedString = "SearchStats {\n"
                + "query_length=1, num_terms=2, num_namespaces_filtered=3, "
                + "num_schema_types_filtered=4,\n"
                + "ranking_strategy=5, num_docs_scored=6, parse_query_latency=7, "
                + "scoring_latency=8, is_numeric_query=true,\n"
                + "num_fetched_hits_lite_index=9, num_fetched_hits_main_index=10, "
                + "num_fetched_hits_integer_index=11,\n"
                + "query_processor_lexer_extract_token_latency=12, "
                + "query_processor_parser_consume_query_latency=13,\n"
                + "query_processor_query_visitor_latency=14}";
        assertThat(sStats.toString()).isEqualTo(expectedString);
    }

    @Test
    public void testAppSearchStats_QueryStats() {
        int nativeQueryLength = 101;
        int nativeNumTerms = 102;
        int nativeNumNamespacesFiltered = 103;
        int nativeNumSchemaTypesFiltered = 104;
        int nativeRankingStrategy = 105;
        int nativeNumDocumentsScored = 106;
        int nativeParseQueryLatencyMillis = 107;
        int nativeScoringLatencyMillis = 108;
        boolean isNumericQuery = true;
        int numFetchedHitsLiteIndex = 109;
        int numFetchedHitsMainIndex = 110;
        int numFetchedHitsIntegerIndex = 111;
        int queryProcessorLexerExtractTokenLatencyMillis = 112;
        int queryProcessorParserConsumeQueryLatencyMillis = 113;
        int queryProcessorQueryVisitorLatencyMillis = 114;

        SearchStats searchStats = new SearchStats.Builder()
                .setNativeQueryLength(nativeQueryLength)
                .setNativeTermCount(nativeNumTerms)
                .setNativeFilteredNamespaceCount(nativeNumNamespacesFiltered)
                .setNativeFilteredSchemaTypeCount(nativeNumSchemaTypesFiltered)
                .setNativeParseQueryLatencyMillis(nativeParseQueryLatencyMillis)
                .setNativeRankingStrategy(nativeRankingStrategy)
                .setNativeQueryProcessorParserConsumeQueryLatencyMillis(
                        nativeParseQueryLatencyMillis)
                .setNativeScoredDocumentCount(nativeNumDocumentsScored)
                .setNativeScoringLatencyMillis(nativeScoringLatencyMillis)
                .setNativeIsNumericQuery(isNumericQuery)
                .setNativeNumFetchedHitsLiteIndex(numFetchedHitsLiteIndex)
                .setNativeNumFetchedHitsMainIndex(numFetchedHitsMainIndex)
                .setNativeNumFetchedHitsIntegerIndex(numFetchedHitsIntegerIndex)
                .setNativeQueryProcessorLexerExtractTokenLatencyMillis(
                        queryProcessorLexerExtractTokenLatencyMillis)
                .setNativeQueryProcessorParserConsumeQueryLatencyMillis(
                        queryProcessorParserConsumeQueryLatencyMillis)
                .setNativeQueryProcessorQueryVisitorLatencyMillis(
                        queryProcessorQueryVisitorLatencyMillis).build();

        int enabledFeatures = 1;
        int rewriteSearchSpecLatencyMillis = 202;
        int rewriteSearchResultLatencyMillis = 203;
        int javaLockAcquisitionLatencyMillis = 204;
        int aclCheckLatencyMillis = 205;
        int visibilityScope = QueryStats.VISIBILITY_SCOPE_LOCAL;
        String searchSourceLogTag = "tag";
        boolean nativeIsFirstPage = true;
        int nativeRequestedPageSize = 206;
        int nativeNumResultsReturnedCurrentPage = 207;
        int nativeLatencyMillis = 208;
        int nativeRankingLatencyMillis = 209;
        int nativeDocumentRetrievingLatencyMillis = 210;
        int nativeNumResultsSnippeted = 211;
        int nativeLockAcquisitionLatencyMillis = 212;
        int javaToNativeJniLatencyMillis = 213;
        int nativeToJavaJniLatencyMillis = 214;
        long liteIndexHitBufferByteSize = 215;
        long liteIndexHitBufferUnsortedByteSize = 216;
        int pageTypeToken = QueryStats.PAGE_TOKEN_TYPE_EMPTY;
        int numResultStatesEvicted = 217;
        int additionalPageCount = 218;
        int numResultsReturnedAdditionalPages = 219;
        int additionalPagesRetrievalLatency = 220;
        int firstNativeCallLatencyMillis = 221;
        int lastBlockingOperation = 222;
        int lastBlockingOperationLatencyMillis = 223;
        int getVmLatencyMillis = 224;

        final QueryStats.Builder qStatsBuilder = new QueryStats.Builder(visibilityScope,
                TEST_PACKAGE_NAME)
                .setDatabase(TEST_DATA_BASE)
                .setStatusCode(TEST_STATUS_CODE)
                .setLaunchVMEnabled(true)
                .setTotalLatencyMillis(TEST_TOTAL_LATENCY_MILLIS)
                .setRewriteSearchSpecLatencyMillis(rewriteSearchSpecLatencyMillis)
                .setRewriteSearchResultLatencyMillis(rewriteSearchResultLatencyMillis)
                .setJavaLockAcquisitionLatencyMillis(javaLockAcquisitionLatencyMillis)
                .setAclCheckLatencyMillis(aclCheckLatencyMillis)
                .setSearchSourceLogTag(searchSourceLogTag)
                .setIsFirstPage(nativeIsFirstPage)
                .setRequestedPageSize(nativeRequestedPageSize)
                .setCurrentPageReturnedResultCount(nativeNumResultsReturnedCurrentPage)
                .setNativeLatencyMillis(nativeLatencyMillis)
                .setRankingLatencyMillis(nativeRankingLatencyMillis)
                .setDocumentRetrievingLatencyMillis(nativeDocumentRetrievingLatencyMillis)
                .setResultWithSnippetsCount(nativeNumResultsSnippeted)
                .setNativeLockAcquisitionLatencyMillis(nativeLockAcquisitionLatencyMillis)
                .setJavaToNativeJniLatencyMillis(javaToNativeJniLatencyMillis)
                .setNativeToJavaJniLatencyMillis(nativeToJavaJniLatencyMillis)
                .setChildSearchStats(searchStats)
                .setParentSearchStats(searchStats)
                .setLiteIndexHitBufferByteSize(liteIndexHitBufferByteSize)
                .setLiteIndexHitBufferUnsortedByteSize(liteIndexHitBufferUnsortedByteSize)
                .setPageTokenType(pageTypeToken)
                .setNumResultStatsEvicted(numResultStatesEvicted)
                .setAdditionalPageCount(additionalPageCount)
                .setAdditionalPagesReturnedResultCount(numResultsReturnedAdditionalPages)
                .setAdditionalPageRetrievalLatencyMillis(additionalPagesRetrievalLatency)
                .setFirstNativeCallLatency(firstNativeCallLatencyMillis)
                .setLastBlockingOperation(lastBlockingOperation)
                .setLastBlockingOperationLatencyMillis(lastBlockingOperationLatencyMillis)
                .addGetVmLatencyMillis(getVmLatencyMillis);
        final QueryStats qStats = qStatsBuilder.build();

        assertThat(qStats.getEnabledFeatures()).isEqualTo(enabledFeatures);
        assertThat(qStats.getPackageName()).isEqualTo(TEST_PACKAGE_NAME);
        assertThat(qStats.getDatabase()).isEqualTo(TEST_DATA_BASE);
        assertThat(qStats.getStatusCode()).isEqualTo(TEST_STATUS_CODE);
        assertThat(qStats.getTotalLatencyMillis()).isEqualTo(
                TEST_TOTAL_LATENCY_MILLIS);
        assertThat(qStats.getRewriteSearchSpecLatencyMillis()).isEqualTo(
                rewriteSearchSpecLatencyMillis);
        assertThat(qStats.getRewriteSearchResultLatencyMillis()).isEqualTo(
                rewriteSearchResultLatencyMillis);
        assertThat(qStats.getJavaLockAcquisitionLatencyMillis()).isEqualTo(
                javaLockAcquisitionLatencyMillis);
        assertThat(qStats.getAclCheckLatencyMillis()).isEqualTo(
                aclCheckLatencyMillis);
        assertThat(qStats.getVisibilityScope()).isEqualTo(visibilityScope);
        assertThat(qStats.getSearchSourceLogTag()).isEqualTo(searchSourceLogTag);
        assertThat(qStats.isFirstPage()).isTrue();
        assertThat(qStats.getRequestedPageSize()).isEqualTo(nativeRequestedPageSize);
        assertThat(qStats.getCurrentPageReturnedResultCount()).isEqualTo(
                nativeNumResultsReturnedCurrentPage);
        assertThat(qStats.getNativeLatencyMillis()).isEqualTo(nativeLatencyMillis);
        assertThat(qStats.getFirstNativeCallLatencyMillis()).isEqualTo(
                firstNativeCallLatencyMillis);
        assertThat(qStats.getRankingLatencyMillis()).isEqualTo(nativeRankingLatencyMillis);
        assertThat(qStats.getResultWithSnippetsCount()).isEqualTo(nativeNumResultsSnippeted);
        assertThat(qStats.getDocumentRetrievingLatencyMillis()).isEqualTo(
                nativeDocumentRetrievingLatencyMillis);
        assertThat(qStats.getNativeLockAcquisitionLatencyMillis()).isEqualTo(
                nativeLockAcquisitionLatencyMillis);
        assertThat(qStats.getJavaToNativeJniLatencyMillis()).isEqualTo(
                javaToNativeJniLatencyMillis);
        assertThat(qStats.getNativeToJavaJniLatencyMillis()).isEqualTo(
                nativeToJavaJniLatencyMillis);
        assertThat(qStats.getParentSearchStats()).isEqualTo(searchStats);
        assertThat(qStats.getChildSearchStats()).isEqualTo(searchStats);
        assertThat(qStats.getLiteIndexHitBufferByteSize()).isEqualTo(liteIndexHitBufferByteSize);
        assertThat(qStats.getLiteIndexHitBufferUnsortedByteSize())
                .isEqualTo(liteIndexHitBufferUnsortedByteSize);
        assertThat(qStats.getPageTokenType()).isEqualTo(pageTypeToken);
        assertThat(qStats.getNumResultStatesEvicted()).isEqualTo(numResultStatesEvicted);
        assertThat(qStats.getAdditionalPageCount()).isEqualTo(additionalPageCount);
        assertThat(qStats.getAdditionalPagesReturnedResultCount()).isEqualTo(
                numResultsReturnedAdditionalPages);
        assertThat(qStats.getAdditionalPageRetrievalLatencyMillis()).isEqualTo(
                additionalPagesRetrievalLatency);
        assertThat(qStats.getJavaLockAcquisitionLatencyMillis())
                .isEqualTo(javaLockAcquisitionLatencyMillis);
        assertThat(qStats.getLastBlockingOperation()).isEqualTo(lastBlockingOperation);
        assertThat(qStats.getLastBlockingOperationLatencyMillis())
                .isEqualTo(lastBlockingOperationLatencyMillis);
        String expectedString = "QueryStats {\n"
                + "package=com.google.test, database=testDataBase, status=2, total_latency=20, "
                + "rewrite_search_spec_latency=202,\n"
                + "rewrite_search_result_latency=203, java_lock_acquisition_latency=204, "
                + "acl_check_latency=205, visibility_score=1,\n"
                + "search_source_log_tag=tag, is_first_page=true, requested_page_size=206, "
                + "num_results_returned_current_page=207,\n"
                + "native_latency=208, ranking_latency=209, document_retrieving_latency=210, "
                + "num_results_with_snippets=211,\n"
                + "native_lock_acquisition_latency=212, java_to_native_jni_latency=213, "
                + "native_to_java_jni_latency=214,\n"
                + "join_latency_ms=0, num_joined_results_current_page=0, join_type=0, "
                + "lite_index_hit_buffer_byte_size=215,\n"
                + "lite_index_hit_buffer_unsorted_byte_size=216\n"
                + "page_token_type=3, num_result_states_evicted=217\n"
                + "parent_search_stats=SearchStats {\n"
                + "query_length=101, num_terms=102, num_namespaces_filtered=103, "
                + "num_schema_types_filtered=104,\n"
                + "ranking_strategy=105, num_docs_scored=106, parse_query_latency=107, "
                + "scoring_latency=108, is_numeric_query=true,\n"
                + "num_fetched_hits_lite_index=109, num_fetched_hits_main_index=110, "
                + "num_fetched_hits_integer_index=111,\n"
                + "query_processor_lexer_extract_token_latency=112, "
                + "query_processor_parser_consume_query_latency=113,\n"
                + "query_processor_query_visitor_latency=114},\n"
                + " child_search_stats=SearchStats {\n"
                + "query_length=101, num_terms=102, num_namespaces_filtered=103, "
                + "num_schema_types_filtered=104,\n"
                + "ranking_strategy=105, num_docs_scored=106, parse_query_latency=107, "
                + "scoring_latency=108, is_numeric_query=true,\n"
                + "num_fetched_hits_lite_index=109, num_fetched_hits_main_index=110, "
                + "num_fetched_hits_integer_index=111,\n"
                + "query_processor_lexer_extract_token_latency=112, "
                + "query_processor_parser_consume_query_latency=113,\n"
                + "query_processor_query_visitor_latency=114}}";
        assertThat(qStats.toString()).isEqualTo(expectedString);
        assertThat(qStats.getGetVmLatencyMillis()).isEqualTo(getVmLatencyMillis);
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
        int lastBlockingOperation = 19;
        int lastBlockingOperationLatencyMillis = 20;
        int getVmLatencyMillis = 21;
        int enabledFeatures = 1;
        int joinIndexIncompatibleTypeChangeCount = 22;
        int scorablePropertyIncompatibleTypeChangeCount = 23;
        int deletedDocumentCount = 24;
        boolean isTermIndexRestored = true;
        boolean isIntegerIndexRestored = true;
        boolean isEmbeddingIndexRestored = true;
        boolean isQualifiedIdJoinIndexRestored = true;
        int nativeSchemaStoreSetSchemaLatencyMillis = 25;
        int nativeDocumentStoreUpdateSchemaLatencyMillis = 26;
        int nativeDocumentStoreOptimizedUpdateSchemaLatencyMillis = 27;
        int nativeIndexRestorationLatencyMillis = 28;
        int nativeScorablePropertyCacheRegenerationLatencyMillis = 29;
        SetSchemaStats sStats = new SetSchemaStats.Builder(TEST_PACKAGE_NAME, TEST_DATA_BASE)
                .setStatusCode(TEST_STATUS_CODE)
                .setTotalLatencyMillis(TEST_TOTAL_LATENCY_MILLIS)
                .setNewTypeCount(newTypeCount)
                .setDeletedTypeCount(deletedTypeCount)
                .setCompatibleTypeChangeCount(compatibleTypeChangeCount)
                .setIndexIncompatibleTypeChangeCount(indexIncompatibleTypeChangeCount)
                .setJoinIndexIncompatibleTypeChangeCount(joinIndexIncompatibleTypeChangeCount)
                .setScorablePropertyIncompatibleTypeChangeCount(
                        scorablePropertyIncompatibleTypeChangeCount)
                .setBackwardsIncompatibleTypeChangeCount(backwardsIncompatibleTypeChangeCount)
                .setDeletedDocumentCount(deletedDocumentCount)
                .setIsTermIndexRestored(isTermIndexRestored)
                .setIsIntegerIndexRestored(isIntegerIndexRestored)
                .setIsEmbeddingIndexRestored(isEmbeddingIndexRestored)
                .setIsQualifiedIdJoinIndexRestored(isQualifiedIdJoinIndexRestored)
                .setVerifyIncomingCallLatencyMillis(verifyIncomingCallLatencyMillis)
                .setExecutorAcquisitionLatencyMillis(executorAcquisitionLatencyMillis)
                .setRebuildFromBundleLatencyMillis(rebuildFromBundleLatencyMillis)
                .setJavaLockAcquisitionLatencyMillis(javaLockAcquisitionLatencyMillis)
                .setRewriteSchemaLatencyMillis(rewriteSchemaLatencyMillis)
                .setTotalNativeLatencyMillis(totalNativeLatencyMillis)
                .setNativeSchemaStoreSetSchemaLatencyMillis(nativeSchemaStoreSetSchemaLatencyMillis)
                .setNativeDocumentStoreUpdateSchemaLatencyMillis(
                        nativeDocumentStoreUpdateSchemaLatencyMillis)
                .setNativeDocumentStoreOptimizedUpdateSchemaLatencyMillis(
                        nativeDocumentStoreOptimizedUpdateSchemaLatencyMillis)
                .setNativeIndexRestorationLatencyMillis(nativeIndexRestorationLatencyMillis)
                .setNativeScorablePropertyCacheRegenerationLatencyMillis(
                        nativeScorablePropertyCacheRegenerationLatencyMillis)
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
                .setJavaLockAcquisitionLatencyMillis(javaLockAcquisitionLatencyMillis)
                .setLastBlockingOperation(lastBlockingOperation)
                .setLastBlockingOperationLatencyMillis(lastBlockingOperationLatencyMillis)
                .addGetVmLatencyMillis(getVmLatencyMillis)
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
        assertThat(sStats.getJoinIndexIncompatibleTypeChangeCount()).isEqualTo(
                joinIndexIncompatibleTypeChangeCount);
        assertThat(sStats.getScorablePropertyIncompatibleTypeChangeCount()).isEqualTo(
                scorablePropertyIncompatibleTypeChangeCount);
        assertThat(sStats.getBackwardsIncompatibleTypeChangeCount()).isEqualTo(
                backwardsIncompatibleTypeChangeCount);
        assertThat(sStats.getDeletedDocumentCount()).isEqualTo(deletedDocumentCount);
        assertThat(sStats.isTermIndexRestored()).isEqualTo(isTermIndexRestored);
        assertThat(sStats.isIntegerIndexRestored()).isEqualTo(isIntegerIndexRestored);
        assertThat(sStats.isEmbeddingIndexRestored()).isEqualTo(isEmbeddingIndexRestored);
        assertThat(sStats.isQualifiedIdJoinIndexRestored()).isEqualTo(
                isQualifiedIdJoinIndexRestored);
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
        assertThat(sStats.getNativeSchemaStoreSetSchemaLatencyMillis()).isEqualTo(
                nativeSchemaStoreSetSchemaLatencyMillis);
        assertThat(sStats.getNativeDocumentStoreUpdateSchemaLatencyMillis()).isEqualTo(
                nativeDocumentStoreUpdateSchemaLatencyMillis);
        assertThat(sStats.getNativeDocumentStoreOptimizedUpdateSchemaLatencyMillis()).isEqualTo(
                nativeDocumentStoreOptimizedUpdateSchemaLatencyMillis);
        assertThat(sStats.getNativeIndexRestorationLatencyMillis()).isEqualTo(
                nativeIndexRestorationLatencyMillis);
        assertThat(sStats.getNativeScorablePropertyCacheRegenerationLatencyMillis()).isEqualTo(
                nativeScorablePropertyCacheRegenerationLatencyMillis);
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
        assertThat(sStats.getJavaLockAcquisitionLatencyMillis())
                .isEqualTo(javaLockAcquisitionLatencyMillis);
        assertThat(sStats.getLastBlockingOperation()).isEqualTo(lastBlockingOperation);
        assertThat(sStats.getLastBlockingOperationLatencyMillis())
                .isEqualTo(lastBlockingOperationLatencyMillis);
        assertThat(sStats.getGetVmLatencyMillis()).isEqualTo(getVmLatencyMillis);
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
        int queryLength = 4;
        int numTerms = 5;
        int numNamespacesFiltered = 6;
        int numSchemaTypesFiltered = 7;
        int parseQueryLatencyMillis = 8;
        int documentRemovalLatencyMillis = 9;
        int javaLockAcquisitionLatencyMillis = 10;
        int lastBlockingOperation = 11;
        int lastBlockingOperationLatencyMillis = 12;
        int getVmLatencyMillis = 13;

        final RemoveStats rStats = new RemoveStats.Builder(TEST_PACKAGE_NAME,
                TEST_DATA_BASE)
                .setStatusCode(TEST_STATUS_CODE)
                .setTotalLatencyMillis(TEST_TOTAL_LATENCY_MILLIS)
                .setNativeLatencyMillis(nativeLatencyMillis)
                .setDeleteType(deleteType)
                .setDeletedDocumentCount(documentDeletedCount)
                .setLaunchVMEnabled(true)
                .setQueryLength(queryLength)
                .setNumTerms(numTerms)
                .setNumNamespacesFiltered(numNamespacesFiltered)
                .setNumSchemaTypesFiltered(numSchemaTypesFiltered)
                .setParseQueryLatencyMillis(parseQueryLatencyMillis)
                .setDocumentRemovalLatencyMillis(documentRemovalLatencyMillis)
                .setJavaLockAcquisitionLatencyMillis(javaLockAcquisitionLatencyMillis)
                .setLastBlockingOperation(lastBlockingOperation)
                .setLastBlockingOperationLatencyMillis(lastBlockingOperationLatencyMillis)
                .addGetVmLatencyMillis(getVmLatencyMillis)
                .build();


        assertThat(rStats.getPackageName()).isEqualTo(TEST_PACKAGE_NAME);
        assertThat(rStats.getDatabase()).isEqualTo(TEST_DATA_BASE);
        assertThat(rStats.getStatusCode()).isEqualTo(TEST_STATUS_CODE);
        assertThat(rStats.getTotalLatencyMillis()).isEqualTo(TEST_TOTAL_LATENCY_MILLIS);
        assertThat(rStats.getNativeLatencyMillis()).isEqualTo(nativeLatencyMillis);
        assertThat(rStats.getDeleteType()).isEqualTo(deleteType);
        assertThat(rStats.getDeletedDocumentCount()).isEqualTo(documentDeletedCount);
        assertThat(rStats.getEnabledFeatures()).isEqualTo(enabledFeatures);
        assertThat(rStats.getQueryLength()).isEqualTo(queryLength);
        assertThat(rStats.getNumTerms()).isEqualTo(numTerms);
        assertThat(rStats.getNumNamespacesFiltered()).isEqualTo(numNamespacesFiltered);
        assertThat(rStats.getNumSchemaTypesFiltered()).isEqualTo(numSchemaTypesFiltered);
        assertThat(rStats.getParseQueryLatencyMillis()).isEqualTo(parseQueryLatencyMillis);
        assertThat(rStats.getDocumentRemovalLatencyMillis())
                .isEqualTo(documentRemovalLatencyMillis);
        assertThat(rStats.getJavaLockAcquisitionLatencyMillis())
                .isEqualTo(javaLockAcquisitionLatencyMillis);
        assertThat(rStats.getLastBlockingOperation()).isEqualTo(lastBlockingOperation);
        assertThat(rStats.getLastBlockingOperationLatencyMillis())
                .isEqualTo(lastBlockingOperationLatencyMillis);
        assertThat(rStats.getGetVmLatencyMillis()).isEqualTo(getVmLatencyMillis);
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
        int indexRestorationMode = 1;
        int numOriginalNamespaces = 7;
        int numDeletedNamespaces = 8;
        int javaLockAcquisitionLatencyMillis = 9;
        int lastBlockingOperation = 10;
        int lastBlockingOperationLatencyMillis = 11;
        int getVmLatencyMillis = 12;

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
                .setIndexRestorationMode(indexRestorationMode)
                .setNumOriginalNamespaces(numOriginalNamespaces)
                .setNumDeletedNamespaces(numDeletedNamespaces)
                .setJavaLockAcquisitionLatencyMillis(javaLockAcquisitionLatencyMillis)
                .setLastBlockingOperation(lastBlockingOperation)
                .setLastBlockingOperationLatencyMillis(lastBlockingOperationLatencyMillis)
                .addGetVmLatencyMillis(getVmLatencyMillis)
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
        assertThat(oStats.getIndexRestorationMode()).isEqualTo(indexRestorationMode);
        assertThat(oStats.getNumOriginalNamespaces()).isEqualTo(numOriginalNamespaces);
        assertThat(oStats.getNumDeletedNamespaces()).isEqualTo(numDeletedNamespaces);
        assertThat(oStats.getJavaLockAcquisitionLatencyMillis())
                .isEqualTo(javaLockAcquisitionLatencyMillis);
        assertThat(oStats.getLastBlockingOperation()).isEqualTo(lastBlockingOperation);
        assertThat(oStats.getLastBlockingOperationLatencyMillis())
                .isEqualTo(lastBlockingOperationLatencyMillis);
        assertThat(oStats.getGetVmLatencyMillis()).isEqualTo(getVmLatencyMillis);
    }

    @Test
    public void testAppSearchStats_JavaLockLatencyCanBeSetOnce() {
        final OptimizeStats oStats = new OptimizeStats.Builder()
                .setJavaLockAcquisitionLatencyMillis(-10)
                .setJavaLockAcquisitionLatencyMillis(10)
                .setJavaLockAcquisitionLatencyMillis(20)
                .build();
        // Can only be set once for non-negative latency.
        assertThat(oStats.getJavaLockAcquisitionLatencyMillis()).isEqualTo(10);
    }

    @Test
    public void testAppSearchStats_PersistToDiskStats() {
        int triggerCallType = 1;
        PersistType.Code persistType = PersistType.Code.FULL;
        int nativeLatencyMillis = 3;
        int blobStorePersistLatencyMillis = 4;
        int documentStoreTotalPersistLatencyMillis = 5;
        int documentStoreComponentsPersistLatencyMillis = 6;
        int documentStoreChecksumUpdateLatencyMillis = 7;
        int documentLogChecksumUpdateLatencyMillis = 8;
        int documentLogDataSyncLatencyMillis = 9;
        int schemaStorePersistLatencyMillis = 10;
        int indexPersistLatencyMillis = 11;
        int integerIndexPersistLatencyMillis = 12;
        int qualifiedIdJoinIndexPersistLatencyMillis = 13;
        int embeddingIndexPersistLatencyMillis = 14;
        int javaLockAcquisitionLatencyMillis = 101;
        int lastBlockingOperation = 102;
        int lastBlockingOperationLatencyMillis = 103;
        int getVmLatencyMillis = 104;
        int enabledFeatures = 1;

        final PersistToDiskStats pStats = new PersistToDiskStats.Builder(
                TEST_PACKAGE_NAME, triggerCallType)
                .setStatusCode(TEST_STATUS_CODE)
                .setTotalLatencyMillis(TEST_TOTAL_LATENCY_MILLIS)
                .setPersistType(persistType)
                .setNativeLatencyMillis(nativeLatencyMillis)
                .setNativeBlobStorePersistLatencyMillis(blobStorePersistLatencyMillis)
                .setNativeDocumentStoreTotalPersistLatencyMillis(
                        documentStoreTotalPersistLatencyMillis)
                .setNativeDocumentStoreComponentsPersistLatencyMillis(
                        documentStoreComponentsPersistLatencyMillis)
                .setNativeDocumentStoreChecksumUpdateLatencyMillis(
                        documentStoreChecksumUpdateLatencyMillis)
                .setNativeDocumentLogChecksumUpdateLatencyMillis(
                        documentLogChecksumUpdateLatencyMillis)
                .setNativeDocumentLogDataSyncLatencyMillis(documentLogDataSyncLatencyMillis)
                .setNativeSchemaStorePersistLatencyMillis(schemaStorePersistLatencyMillis)
                .setNativeIndexPersistLatencyMillis(indexPersistLatencyMillis)
                .setNativeIntegerIndexPersistLatencyMillis(integerIndexPersistLatencyMillis)
                .setNativeQualifiedIdJoinIndexPersistLatencyMillis(
                        qualifiedIdJoinIndexPersistLatencyMillis)
                .setNativeEmbeddingIndexPersistLatencyMillis(embeddingIndexPersistLatencyMillis)
                .setJavaLockAcquisitionLatencyMillis(javaLockAcquisitionLatencyMillis)
                .setLastBlockingOperation(lastBlockingOperation)
                .setLastBlockingOperationLatencyMillis(lastBlockingOperationLatencyMillis)
                .addGetVmLatencyMillis(getVmLatencyMillis)
                .setLaunchVMEnabled(true)
                .build();

        assertThat(pStats.getPackageName()).isEqualTo(TEST_PACKAGE_NAME);
        assertThat(pStats.getTriggerCallType()).isEqualTo(triggerCallType);
        assertThat(pStats.getStatusCode()).isEqualTo(TEST_STATUS_CODE);
        assertThat(pStats.getTotalLatencyMillis()).isEqualTo(TEST_TOTAL_LATENCY_MILLIS);
        assertThat(pStats.getNativeLatencyMillis()).isEqualTo(nativeLatencyMillis);
        assertThat(pStats.getBlobStorePersistLatencyMillis())
                .isEqualTo(blobStorePersistLatencyMillis);
        assertThat(pStats.getDocumentStoreTotalPersistLatencyMillis())
                .isEqualTo(documentStoreTotalPersistLatencyMillis);
        assertThat(pStats.getDocumentStoreComponentsPersistLatencyMillis())
                .isEqualTo(documentStoreComponentsPersistLatencyMillis);
        assertThat(pStats.getDocumentStoreChecksumUpdateLatencyMillis())
                .isEqualTo(documentStoreChecksumUpdateLatencyMillis);
        assertThat(pStats.getDocumentLogChecksumUpdateLatencyMillis())
                .isEqualTo(documentLogChecksumUpdateLatencyMillis);
        assertThat(pStats.getDocumentLogDataSyncLatencyMillis())
                .isEqualTo(documentLogDataSyncLatencyMillis);
        assertThat(pStats.getSchemaStorePersistLatencyMillis())
                .isEqualTo(schemaStorePersistLatencyMillis);
        assertThat(pStats.getIndexPersistLatencyMillis()).isEqualTo(indexPersistLatencyMillis);
        assertThat(pStats.getIntegerIndexPersistLatencyMillis())
                .isEqualTo(integerIndexPersistLatencyMillis);
        assertThat(pStats.getQualifiedIdJoinIndexPersistLatencyMillis())
                .isEqualTo(qualifiedIdJoinIndexPersistLatencyMillis);
        assertThat(pStats.getEmbeddingIndexPersistLatencyMillis())
                .isEqualTo(embeddingIndexPersistLatencyMillis);

        assertThat(pStats.getJavaLockAcquisitionLatencyMillis())
                .isEqualTo(javaLockAcquisitionLatencyMillis);
        assertThat(pStats.getLastBlockingOperation()).isEqualTo(lastBlockingOperation);
        assertThat(pStats.getLastBlockingOperationLatencyMillis())
                .isEqualTo(lastBlockingOperationLatencyMillis);
        assertThat(pStats.getGetVmLatencyMillis()).isEqualTo(getVmLatencyMillis);
    }
}
