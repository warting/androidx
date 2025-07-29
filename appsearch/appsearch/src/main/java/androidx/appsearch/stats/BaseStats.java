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

package androidx.appsearch.stats;

import android.annotation.SuppressLint;

import androidx.annotation.IntDef;
import androidx.annotation.RestrictTo;
import androidx.appsearch.annotation.CanIgnoreReturnValue;
import androidx.core.util.Preconditions;

import org.jspecify.annotations.NonNull;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Encapsulates base statistics information for AppSearch results.
 *
 * This class provides a convenient way to store and retrieve key statistics,
 * such as the result code and a bitmask of enabled features.
 * @exportToFramework:hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class BaseStats {

    /** Call types. */
    @IntDef(value = {
            CALL_TYPE_UNKNOWN,
            CALL_TYPE_INITIALIZE,
            CALL_TYPE_SET_SCHEMA,
            CALL_TYPE_PUT_DOCUMENTS,
            CALL_TYPE_GET_DOCUMENTS,
            CALL_TYPE_REMOVE_DOCUMENTS_BY_ID,
            CALL_TYPE_PUT_DOCUMENT,
            CALL_TYPE_GET_DOCUMENT,
            CALL_TYPE_REMOVE_DOCUMENT_BY_ID,
            CALL_TYPE_SEARCH,
            CALL_TYPE_OPTIMIZE,
            CALL_TYPE_FLUSH,
            CALL_TYPE_GLOBAL_SEARCH,
            CALL_TYPE_REMOVE_DOCUMENTS_BY_SEARCH,
            CALL_TYPE_REMOVE_DOCUMENT_BY_SEARCH,
            CALL_TYPE_GLOBAL_GET_DOCUMENT_BY_ID,
            CALL_TYPE_SCHEMA_MIGRATION,
            CALL_TYPE_GLOBAL_GET_SCHEMA,
            CALL_TYPE_GET_SCHEMA,
            CALL_TYPE_GET_NAMESPACES,
            CALL_TYPE_GET_NEXT_PAGE,
            CALL_TYPE_INVALIDATE_NEXT_PAGE_TOKEN,
            CALL_TYPE_WRITE_SEARCH_RESULTS_TO_FILE,
            CALL_TYPE_PUT_DOCUMENTS_FROM_FILE,
            CALL_TYPE_SEARCH_SUGGESTION,
            CALL_TYPE_REPORT_SYSTEM_USAGE,
            CALL_TYPE_REPORT_USAGE,
            CALL_TYPE_GET_STORAGE_INFO,
            CALL_TYPE_REGISTER_OBSERVER_CALLBACK,
            CALL_TYPE_UNREGISTER_OBSERVER_CALLBACK,
            CALL_TYPE_GLOBAL_GET_NEXT_PAGE,
            CALL_TYPE_EXECUTE_APP_FUNCTION,
            CALL_TYPE_OPEN_WRITE_BLOB,
            CALL_TYPE_COMMIT_BLOB,
            CALL_TYPE_OPEN_READ_BLOB,
            CALL_TYPE_GLOBAL_OPEN_READ_BLOB,
            CALL_TYPE_REMOVE_BLOB,
            CALL_TYPE_SET_BLOB_VISIBILITY,
            CALL_TYPE_PRUNE_PACKAGE_DATA,
            CALL_TYPE_CLOSE
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface CallType {
    }

    public static final int CALL_TYPE_UNKNOWN = 0;
    public static final int CALL_TYPE_INITIALIZE = 1;
    public static final int CALL_TYPE_SET_SCHEMA = 2;
    public static final int CALL_TYPE_PUT_DOCUMENTS = 3;
    public static final int CALL_TYPE_GET_DOCUMENTS = 4;
    public static final int CALL_TYPE_REMOVE_DOCUMENTS_BY_ID = 5;
    public static final int CALL_TYPE_PUT_DOCUMENT = 6;
    public static final int CALL_TYPE_GET_DOCUMENT = 7;
    public static final int CALL_TYPE_REMOVE_DOCUMENT_BY_ID = 8;
    public static final int CALL_TYPE_SEARCH = 9;
    public static final int CALL_TYPE_OPTIMIZE = 10;
    public static final int CALL_TYPE_FLUSH = 11;
    public static final int CALL_TYPE_GLOBAL_SEARCH = 12;
    public static final int CALL_TYPE_REMOVE_DOCUMENTS_BY_SEARCH = 13;
    public static final int CALL_TYPE_REMOVE_DOCUMENT_BY_SEARCH = 14;
    public static final int CALL_TYPE_GLOBAL_GET_DOCUMENT_BY_ID = 15;
    public static final int CALL_TYPE_SCHEMA_MIGRATION = 16;
    public static final int CALL_TYPE_GLOBAL_GET_SCHEMA = 17;
    public static final int CALL_TYPE_GET_SCHEMA = 18;
    public static final int CALL_TYPE_GET_NAMESPACES = 19;
    public static final int CALL_TYPE_GET_NEXT_PAGE = 20;
    public static final int CALL_TYPE_INVALIDATE_NEXT_PAGE_TOKEN = 21;
    public static final int CALL_TYPE_WRITE_SEARCH_RESULTS_TO_FILE = 22;
    public static final int CALL_TYPE_PUT_DOCUMENTS_FROM_FILE = 23;
    public static final int CALL_TYPE_SEARCH_SUGGESTION = 24;
    public static final int CALL_TYPE_REPORT_SYSTEM_USAGE = 25;
    public static final int CALL_TYPE_REPORT_USAGE = 26;
    public static final int CALL_TYPE_GET_STORAGE_INFO = 27;
    public static final int CALL_TYPE_REGISTER_OBSERVER_CALLBACK = 28;
    public static final int CALL_TYPE_UNREGISTER_OBSERVER_CALLBACK = 29;
    public static final int CALL_TYPE_GLOBAL_GET_NEXT_PAGE = 30;
    public static final int CALL_TYPE_EXECUTE_APP_FUNCTION = 31;
    public static final int CALL_TYPE_OPEN_WRITE_BLOB = 32;
    public static final int CALL_TYPE_COMMIT_BLOB = 33;
    public static final int CALL_TYPE_OPEN_READ_BLOB = 34;
    public static final int CALL_TYPE_GLOBAL_OPEN_READ_BLOB = 35;
    public static final int CALL_TYPE_REMOVE_BLOB = 36;
    public static final int CALL_TYPE_SET_BLOB_VISIBILITY = 37;
    // Most call types are for AppSearchManager APIs. This call type is for internal calls, such
    // as from indexers.
    public static final int INTERNAL_CALL_TYPE_APP_OPEN_EVENT_INDEXER = 38;
    public static final int CALL_TYPE_PRUNE_PACKAGE_DATA = 39;
    public static final int CALL_TYPE_CLOSE = 40;

    // These strings are for the subset of call types that correspond to an AppSearchManager API
    public static final String CALL_TYPE_STRING_INITIALIZE = "initialize";
    public static final String CALL_TYPE_STRING_SET_SCHEMA = "localSetSchema";
    public static final String CALL_TYPE_STRING_PUT_DOCUMENTS = "localPutDocuments";
    public static final String CALL_TYPE_STRING_GET_DOCUMENTS = "localGetDocuments";
    public static final String CALL_TYPE_STRING_REMOVE_DOCUMENTS_BY_ID = "localRemoveByDocumentId";
    public static final String CALL_TYPE_STRING_SEARCH = "localSearch";
    public static final String CALL_TYPE_STRING_FLUSH = "flush";
    public static final String CALL_TYPE_STRING_GLOBAL_SEARCH = "globalSearch";
    public static final String CALL_TYPE_STRING_REMOVE_DOCUMENTS_BY_SEARCH = "localRemoveBySearch";
    public static final String CALL_TYPE_STRING_GLOBAL_GET_DOCUMENT_BY_ID = "globalGetDocuments";
    public static final String CALL_TYPE_STRING_GLOBAL_GET_SCHEMA = "globalGetSchema";
    public static final String CALL_TYPE_STRING_GET_SCHEMA = "localGetSchema";
    public static final String CALL_TYPE_STRING_GET_NAMESPACES = "localGetNamespaces";
    public static final String CALL_TYPE_STRING_GET_NEXT_PAGE = "localGetNextPage";
    public static final String CALL_TYPE_STRING_INVALIDATE_NEXT_PAGE_TOKEN =
            "invalidateNextPageToken";
    public static final String CALL_TYPE_STRING_WRITE_SEARCH_RESULTS_TO_FILE =
                    "localWriteSearchResultsToFile";
    public static final String CALL_TYPE_STRING_PUT_DOCUMENTS_FROM_FILE =
                            "localPutDocumentsFromFile";
    public static final String CALL_TYPE_STRING_SEARCH_SUGGESTION = "localSearchSuggestion";
    public static final String CALL_TYPE_STRING_REPORT_SYSTEM_USAGE = "globalReportUsage";
    public static final String CALL_TYPE_STRING_REPORT_USAGE = "localReportUsage";
    public static final String CALL_TYPE_STRING_GET_STORAGE_INFO = "localGetStorageInfo";
    public static final String CALL_TYPE_STRING_REGISTER_OBSERVER_CALLBACK =
                                    "globalRegisterObserverCallback";
    public static final String CALL_TYPE_STRING_UNREGISTER_OBSERVER_CALLBACK =
                                            "globalUnregisterObserverCallback";
    public static final String CALL_TYPE_STRING_GLOBAL_GET_NEXT_PAGE = "globalGetNextPage";
    public static final String CALL_TYPE_STRING_EXECUTE_APP_FUNCTION = "executeAppFunction";
    public static final String CALL_TYPE_STRING_OPEN_WRITE_BLOB = "openWriteBlob";
    public static final String CALL_TYPE_STRING_COMMIT_BLOB = "commitBlob";
    public static final String CALL_TYPE_STRING_OPEN_READ_BLOB = "openReadBlob";
    public static final String CALL_TYPE_STRING_GLOBAL_OPEN_READ_BLOB = "globalOpenReadBlob";
    public static final String CALL_TYPE_STRING_REMOVE_BLOB = "removeBlob";
    public static final String CALL_TYPE_STRING_SET_BLOB_VISIBILITY = "setBlobVisibility";
    public static final String CALL_TYPE_STRING_PRUNE_PACKAGE_DATA = "prunePackageData";
    public static final String CALL_TYPE_STRING_CLOSE = "close";

    private static final int LAUNCH_VM = 0;
    private final long mEnabledFeatures;
    /** Time passed while waiting to acquire the lock during Java function calls. */
    protected final int mJavaLockAcquisitionLatencyMillis;
    @CallType
    private final int mLastWriteOperation;
    private final int mLastWriteOperationLatencyMillis;
    // The latency of get the VM instance.
    int mGetVmLatencyMillis;

    protected BaseStats(@NonNull Builder<?> builder) {
        Preconditions.checkNotNull(builder);
        mEnabledFeatures = builder.mEnabledFeatures;
        mJavaLockAcquisitionLatencyMillis = builder.mJavaLockAcquisitionLatencyMillis;
        mLastWriteOperation = builder.mLastWriteOperation;
        mLastWriteOperationLatencyMillis = builder.mLastWriteOperationLatencyMillis;
        mGetVmLatencyMillis = builder.mGetVmLatencyMillis;
    }

    /** Returns the bitmask representing the enabled features. */
    public long getEnabledFeatures() {
        return mEnabledFeatures;
    }

    /**  Returns the last write operation call type. */
    @CallType
    public int getLastWriteOperation() {
        return mLastWriteOperation;
    }

    /**  Returns latency for last write operation which hold the write lock in milliseconds. */
    public int getLastWriteOperationLatencyMillis() {
        return mLastWriteOperationLatencyMillis;
    }

    /** Returns time passed while waiting to acquire the lock during Java function calls */
    public int getJavaLockAcquisitionLatencyMillis() {
        return mJavaLockAcquisitionLatencyMillis;
    }

    /** Returns time passed while get the vm instance. */
    public int getGetVmLatencyMillis() {
        return mGetVmLatencyMillis;
    }

    /**
     * Builder for {@link BaseStats}.
     *
     * @param <BuilderType> Type of subclass who extends this.
     */
    // This builder is specifically designed to be extended by stats classes.
    @SuppressLint("StaticFinalBuilder")
    @SuppressWarnings("rawtypes")
    public static class Builder<BuilderType extends Builder> {
        private final BuilderType mBuilderTypeInstance;
        long mEnabledFeatures;
        /**
         * The latency in ms to get the lock.
         *
         * <p> For multi-level call like checkForOptimize -> optimize. We only need to set the java
         * lock latency in the first level call. Since the write lock won't be released, the latency
         * in the second and deeper level will include the actually work in top level and won't be
         * accurate.
         */
        int mJavaLockAcquisitionLatencyMillis = -1;

        // The call type of the last mutation call that hold the write lock.
        @CallType
        int mLastWriteOperation;
        // The latency of the last mutation call holds the write lock in AppSearch.
        int mLastWriteOperationLatencyMillis;
        // The latency of get the VM instance.
        int mGetVmLatencyMillis = 0;

        /** Creates a new {@link BaseStats.Builder}. */
        @SuppressWarnings("unchecked")
        public Builder() {
            mBuilderTypeInstance = (BuilderType) this;
        }

        /** Sets bitmask for all enabled features . */
        @CanIgnoreReturnValue
        public @NonNull BuilderType setLaunchVMEnabled(boolean enabled) {
            modifyEnabledFeature(LAUNCH_VM, enabled);
            return mBuilderTypeInstance;
        }

        /**
         * Sets latency for waiting to acquire the lock during Java function calls in milliseconds.
         */
        @CanIgnoreReturnValue
        public @NonNull BuilderType setJavaLockAcquisitionLatencyMillis(
                int javaLockAcquisitionLatencyMillis) {
            if (mJavaLockAcquisitionLatencyMillis < 0) {
                mJavaLockAcquisitionLatencyMillis = javaLockAcquisitionLatencyMillis;
            }
            return mBuilderTypeInstance;
        }

        /**  Sets the last write operation call type. */
        @CanIgnoreReturnValue
        public @NonNull BuilderType setLastWriteOperation(@CallType int lastWriteOperation) {
            mLastWriteOperation = lastWriteOperation;
            return mBuilderTypeInstance;
        }

        /**  Sets latency for last write operation which hold the write lock in milliseconds. */
        @CanIgnoreReturnValue
        public @NonNull BuilderType setLastWriteOperationLatencyMillis(
                int lastWriteOperationLatencyMillis) {
            mLastWriteOperationLatencyMillis = lastWriteOperationLatencyMillis;
            return mBuilderTypeInstance;
        }

        /**  Adds latency for last write operation which hold the write lock in milliseconds. */
        @CanIgnoreReturnValue
        public @NonNull BuilderType addGetVmLatencyMillis(int getVmLatencyMillis) {
            mGetVmLatencyMillis += getVmLatencyMillis;
            return mBuilderTypeInstance;
        }


        /** Builds the {@link BaseStats} instance. */
        public @NonNull BaseStats build() {
            return new BaseStats(this);
        }

        private void modifyEnabledFeature(int feature, boolean enabled) {
            if (enabled) {
                mEnabledFeatures |= (1L << feature);
            } else {
                mEnabledFeatures &= ~(1L << feature);
            }
        }
    }
}
