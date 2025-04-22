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

package androidx.appsearch.localstorage;

import androidx.annotation.RestrictTo;
import androidx.appsearch.localstorage.stats.InitializeStats;
import androidx.appsearch.localstorage.stats.OptimizeStats;
import androidx.appsearch.localstorage.stats.PutDocumentStats;
import androidx.appsearch.localstorage.stats.RemoveStats;
import androidx.appsearch.localstorage.stats.SearchStats;
import androidx.appsearch.localstorage.stats.SetSchemaStats;
import androidx.core.util.Preconditions;

import com.google.android.icing.proto.DeleteByQueryStatsProto;
import com.google.android.icing.proto.DeleteStatsProto;
import com.google.android.icing.proto.InitializeStatsProto;
import com.google.android.icing.proto.OptimizeStatsProto;
import com.google.android.icing.proto.PutDocumentStatsProto;
import com.google.android.icing.proto.QueryStatsProto;
import com.google.android.icing.proto.SetSchemaResultProto;

import org.jspecify.annotations.NonNull;

/**
 * Class contains helper functions for logging.
 *
 * <p>E.g. we need to have helper functions to copy numbers from IcingLib to stats classes.
 *
 * @exportToFramework:hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public final class AppSearchLoggerHelper {
    private AppSearchLoggerHelper() {
    }

    /**
     * Copies native PutDocument stats to builder.
     *
     * @param fromNativeStats stats copied from
     * @param toStatsBuilder  stats copied to
     */
    static void copyNativeStats(@NonNull PutDocumentStatsProto fromNativeStats,
            PutDocumentStats.@NonNull Builder toStatsBuilder) {
        Preconditions.checkNotNull(fromNativeStats);
        Preconditions.checkNotNull(toStatsBuilder);
        toStatsBuilder
                .setNativeLatencyMillis(fromNativeStats.getLatencyMs())
                .setNativeDocumentStoreLatencyMillis(
                        fromNativeStats.getDocumentStoreLatencyMs())
                .setNativeIndexLatencyMillis(fromNativeStats.getIndexLatencyMs())
                .setNativeIndexMergeLatencyMillis(fromNativeStats.getIndexMergeLatencyMs())
                .setNativeDocumentSizeBytes(fromNativeStats.getDocumentSize())
                .setNativeNumTokensIndexed(
                        fromNativeStats.getTokenizationStats().getNumTokensIndexed())
                .setNativeTermIndexLatencyMillis(fromNativeStats.getTermIndexLatencyMs())
                .setNativeIntegerIndexLatencyMillis(fromNativeStats.getIntegerIndexLatencyMs())
                .setNativeQualifiedIdJoinIndexLatencyMillis(
                        fromNativeStats.getQualifiedIdJoinIndexLatencyMs())
                .setNativeLiteIndexSortLatencyMillis(
                        fromNativeStats.getLiteIndexSortLatencyMs());
    }

    /**
     * Copies native Initialize stats to builder.
     *
     * @param fromNativeStats stats copied from
     * @param toStatsBuilder  stats copied to
     */
    static void copyNativeStats(@NonNull InitializeStatsProto fromNativeStats,
            InitializeStats.@NonNull Builder toStatsBuilder) {
        Preconditions.checkNotNull(fromNativeStats);
        Preconditions.checkNotNull(toStatsBuilder);
        toStatsBuilder
                .setNativeLatencyMillis(fromNativeStats.getLatencyMs())
                .setNativeDocumentStoreRecoveryCause(
                        fromNativeStats.getDocumentStoreRecoveryCause().getNumber())
                .setNativeIndexRestorationCause(
                        fromNativeStats.getIndexRestorationCause().getNumber())
                .setNativeSchemaStoreRecoveryCause(
                        fromNativeStats.getSchemaStoreRecoveryCause().getNumber())
                .setNativeDocumentStoreRecoveryLatencyMillis(
                        fromNativeStats.getDocumentStoreRecoveryLatencyMs())
                .setNativeIndexRestorationLatencyMillis(
                        fromNativeStats.getIndexRestorationLatencyMs())
                .setNativeSchemaStoreRecoveryLatencyMillis(
                        fromNativeStats.getSchemaStoreRecoveryLatencyMs())
                .setNativeDocumentStoreDataStatus(
                        fromNativeStats.getDocumentStoreDataStatus().getNumber())
                .setNativeDocumentCount(fromNativeStats.getNumDocuments())
                .setNativeSchemaTypeCount(fromNativeStats.getNumSchemaTypes())
                .setNativeNumPreviousInitFailures(fromNativeStats.getNumPreviousInitFailures())
                .setNativeIntegerIndexRestorationCause(
                        fromNativeStats.getIntegerIndexRestorationCause().getNumber())
                .setNativeQualifiedIdJoinIndexRestorationCause(
                        fromNativeStats.getQualifiedIdJoinIndexRestorationCause().getNumber())
                .setNativeEmbeddingIndexRestorationCause(
                        fromNativeStats.getEmbeddingIndexRestorationCause().getNumber())
                .setNativeInitializeIcuDataStatusCode(
                        fromNativeStats.getInitializeIcuDataStatus().getCode().getNumber())
                .setNativeNumFailedReindexedDocuments(
                        fromNativeStats.getNumFailedReindexedDocuments());
    }

    /**
     * Copies native Query stats to builder.
     *
     * @param fromNativeStats Stats copied from.
     * @param toStatsBuilder Stats copied to.
     */
    static void copyNativeStats(@NonNull QueryStatsProto fromNativeStats,
            SearchStats.@NonNull Builder toStatsBuilder) {
        Preconditions.checkNotNull(fromNativeStats);
        Preconditions.checkNotNull(toStatsBuilder);
        toStatsBuilder
                .setNativeLatencyMillis(fromNativeStats.getLatencyMs())
                .setTermCount(fromNativeStats.getNumTerms())
                .setQueryLength(fromNativeStats.getQueryLength())
                .setFilteredNamespaceCount(fromNativeStats.getNumNamespacesFiltered())
                .setFilteredSchemaTypeCount(fromNativeStats.getNumSchemaTypesFiltered())
                .setRequestedPageSize(fromNativeStats.getRequestedPageSize())
                .setCurrentPageReturnedResultCount(
                        fromNativeStats.getNumResultsReturnedCurrentPage())
                .setIsFirstPage(fromNativeStats.getIsFirstPage())
                .setParseQueryLatencyMillis(fromNativeStats.getParseQueryLatencyMs())
                .setRankingStrategy(fromNativeStats.getRankingStrategy().getNumber())
                .setScoredDocumentCount(fromNativeStats.getNumDocumentsScored())
                .setScoringLatencyMillis(fromNativeStats.getScoringLatencyMs())
                .setRankingLatencyMillis(fromNativeStats.getRankingLatencyMs())
                .setResultWithSnippetsCount(fromNativeStats.getNumResultsWithSnippets())
                .setDocumentRetrievingLatencyMillis(
                        fromNativeStats.getDocumentRetrievalLatencyMs())
                .setNativeLockAcquisitionLatencyMillis(
                        fromNativeStats.getLockAcquisitionLatencyMs())
                .setJavaToNativeJniLatencyMillis(
                        fromNativeStats.getJavaToNativeJniLatencyMs())
                .setNativeToJavaJniLatencyMillis(
                        fromNativeStats.getNativeToJavaJniLatencyMs())
                .setNativeNumJoinedResultsCurrentPage(
                        fromNativeStats.getNumJoinedResultsReturnedCurrentPage())
                .setNativeJoinLatencyMillis(fromNativeStats.getJoinLatencyMs());
    }

    /**
     * Copies native Delete stats to builder.
     *
     * @param fromNativeStats Stats copied from.
     * @param toStatsBuilder Stats copied to.
     */
    static void copyNativeStats(@NonNull DeleteStatsProto fromNativeStats,
            RemoveStats.@NonNull Builder toStatsBuilder) {
        Preconditions.checkNotNull(fromNativeStats);
        Preconditions.checkNotNull(toStatsBuilder);
        toStatsBuilder
                .setNativeLatencyMillis(fromNativeStats.getLatencyMs())
                .setDeleteType(fromNativeStats.getDeleteType().getNumber())
                .setDeletedDocumentCount(fromNativeStats.getNumDocumentsDeleted());
    }

    /**
     * Copies native DeleteByQuery stats to builder.
     *
     * @param fromNativeStats Stats copied from.
     * @param toStatsBuilder Stats copied to.
     */
    static void copyNativeStats(@NonNull DeleteByQueryStatsProto fromNativeStats,
            RemoveStats.@NonNull Builder toStatsBuilder) {
        Preconditions.checkNotNull(fromNativeStats);
        Preconditions.checkNotNull(toStatsBuilder);

        toStatsBuilder
                .setNativeLatencyMillis(fromNativeStats.getLatencyMs())
                .setDeleteType(RemoveStats.QUERY)
                .setDeletedDocumentCount(fromNativeStats.getNumDocumentsDeleted());
    }

    /**
     * Copies native {@link OptimizeStatsProto} to builder.
     *
     * @param fromNativeStats Stats copied from.
     * @param toStatsBuilder Stats copied to.
     */
    static void copyNativeStats(@NonNull OptimizeStatsProto fromNativeStats,
            OptimizeStats.@NonNull Builder toStatsBuilder) {
        Preconditions.checkNotNull(fromNativeStats);
        Preconditions.checkNotNull(toStatsBuilder);
        toStatsBuilder
                .setNativeLatencyMillis(fromNativeStats.getLatencyMs())
                .setDocumentStoreOptimizeLatencyMillis(
                        fromNativeStats.getDocumentStoreOptimizeLatencyMs())
                .setIndexRestorationLatencyMillis(fromNativeStats.getIndexRestorationLatencyMs())
                .setOriginalDocumentCount(fromNativeStats.getNumOriginalDocuments())
                .setDeletedDocumentCount(fromNativeStats.getNumDeletedDocuments())
                .setExpiredDocumentCount(fromNativeStats.getNumExpiredDocuments())
                .setStorageSizeBeforeBytes(fromNativeStats.getStorageSizeBefore())
                .setStorageSizeAfterBytes(fromNativeStats.getStorageSizeAfter())
                .setTimeSinceLastOptimizeMillis(fromNativeStats.getTimeSinceLastOptimizeMs());
    }

    /*
     * Copy SetSchema result stats to builder.
     *
     * @param fromProto Stats copied from.
     * @param toStatsBuilder Stats copied to.
     */
    static void copyNativeStats(@NonNull SetSchemaResultProto fromProto,
            SetSchemaStats.@NonNull Builder toStatsBuilder) {
        Preconditions.checkNotNull(fromProto);
        Preconditions.checkNotNull(toStatsBuilder);
        toStatsBuilder
                .setNewTypeCount(fromProto.getNewSchemaTypesCount())
                .setDeletedTypeCount(fromProto.getDeletedSchemaTypesCount())
                .setCompatibleTypeChangeCount(fromProto.getFullyCompatibleChangedSchemaTypesCount())
                .setIndexIncompatibleTypeChangeCount(
                        fromProto.getIndexIncompatibleChangedSchemaTypesCount())
                .setBackwardsIncompatibleTypeChangeCount(
                        fromProto.getIncompatibleSchemaTypesCount());
    }
}
