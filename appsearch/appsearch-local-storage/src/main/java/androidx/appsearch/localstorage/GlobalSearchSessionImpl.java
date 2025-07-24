/*
 * Copyright 2020 The Android Open Source Project
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
// @exportToFramework:skipFile()
package androidx.appsearch.localstorage;

import static androidx.appsearch.app.AppSearchResult.throwableToFailedResult;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.ParcelFileDescriptor;

import androidx.appsearch.app.AppSearchBatchResult;
import androidx.appsearch.app.AppSearchBlobHandle;
import androidx.appsearch.app.AppSearchResult;
import androidx.appsearch.app.ExperimentalAppSearchApi;
import androidx.appsearch.app.Features;
import androidx.appsearch.app.GenericDocument;
import androidx.appsearch.app.GetByDocumentIdRequest;
import androidx.appsearch.app.GetSchemaResponse;
import androidx.appsearch.app.GlobalSearchSession;
import androidx.appsearch.app.OpenBlobForReadResponse;
import androidx.appsearch.app.ReportSystemUsageRequest;
import androidx.appsearch.app.SearchResults;
import androidx.appsearch.app.SearchSpec;
import androidx.appsearch.exceptions.AppSearchException;
import androidx.appsearch.localstorage.util.FutureUtil;
import androidx.appsearch.localstorage.visibilitystore.CallerAccess;
import androidx.appsearch.observer.ObserverCallback;
import androidx.appsearch.observer.ObserverSpec;
import androidx.core.util.Preconditions;

import com.google.common.util.concurrent.ListenableFuture;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Set;
import java.util.concurrent.Executor;

/**
 * An implementation of {@link GlobalSearchSession} which stores data locally
 * in the app's storage space using a bundled version of the search native library.
 *
 * <p>Queries are executed multi-threaded, but a single thread is used for mutate requests (put,
 * delete, etc..).
 */
class GlobalSearchSessionImpl implements GlobalSearchSession {
    private final AppSearchImpl mAppSearchImpl;
    private final Executor mExecutor;
    private final Features mFeatures;
    private final Context mContext;
    private final @Nullable AppSearchLogger mLogger;

    private final CallerAccess mSelfCallerAccess;

    private boolean mIsClosed = false;

    GlobalSearchSessionImpl(
            @NonNull AppSearchImpl appSearchImpl,
            @NonNull Executor executor,
            @NonNull Features features,
            @NonNull Context context,
            @Nullable AppSearchLogger logger) {
        mAppSearchImpl = Preconditions.checkNotNull(appSearchImpl);
        mExecutor = Preconditions.checkNotNull(executor);
        mFeatures = Preconditions.checkNotNull(features);
        mContext = Preconditions.checkNotNull(context);
        mLogger = logger;

        mSelfCallerAccess = new CallerAccess(/*callingPackageName=*/mContext.getPackageName());
    }

    @Override
    public @NonNull ListenableFuture<AppSearchBatchResult<String, GenericDocument>>
            getByDocumentIdAsync(
                    @NonNull String packageName,
                    @NonNull String databaseName,
                    @NonNull GetByDocumentIdRequest request) {
        Preconditions.checkNotNull(packageName);
        Preconditions.checkNotNull(databaseName);
        Preconditions.checkNotNull(request);
        Preconditions.checkState(!mIsClosed, "GlobalSearchSession has already been closed");
        return FutureUtil.execute(mExecutor, () -> {
            CallerAccess access = new CallerAccess(mContext.getPackageName());
            return mAppSearchImpl.batchGetDocuments(packageName, databaseName, request, access,
                    /*callStatsBuilder=*/null);
        });
    }

    @Override
    @ExperimentalAppSearchApi
    public @NonNull ListenableFuture<OpenBlobForReadResponse> openBlobForReadAsync(
            @NonNull Set<AppSearchBlobHandle> handles) {
        Preconditions.checkNotNull(handles);
        Preconditions.checkState(!mIsClosed, "GlobalSearchSession has already been closed");
        return FutureUtil.execute(mExecutor, () -> {
            AppSearchBatchResult.Builder<AppSearchBlobHandle, ParcelFileDescriptor> resultBuilder =
                    new AppSearchBatchResult.Builder<>();
            CallerAccess access = new CallerAccess(mContext.getPackageName());
            for (AppSearchBlobHandle handle : handles) {
                try {
                    // Global reader could read blobs that are written by other apps. We skip the
                    // verification that the handle's package name and database name must match
                    // to the caller.
                    ParcelFileDescriptor pfd = mAppSearchImpl.globalOpenReadBlob(handle, access,
                            /*callStatsBuilder=*/null);
                    resultBuilder.setSuccess(handle, pfd);
                } catch (Throwable t) {
                    resultBuilder.setResult(handle, throwableToFailedResult(t));
                }
            }
            return new OpenBlobForReadResponse(resultBuilder.build());
        });
    }

    @Override
    public @NonNull SearchResults search(
            @NonNull String queryExpression, @NonNull SearchSpec searchSpec) {
        Preconditions.checkNotNull(queryExpression);
        Preconditions.checkNotNull(searchSpec);
        Preconditions.checkState(!mIsClosed, "GlobalSearchSession has already been closed");
        return new SearchResultsImpl(
                mAppSearchImpl,
                mExecutor,
                mContext.getPackageName(),
                /*databaseName=*/ null,
                queryExpression,
                searchSpec,
                mLogger);
    }

    /**
     * Reporting system usage is not supported in the local backend, so this method does nothing
     * and always completes the return value with an
     * {@link androidx.appsearch.exceptions.AppSearchException} having a result code of
     * {@link AppSearchResult#RESULT_SECURITY_ERROR}.
     */
    @Override
    public @NonNull ListenableFuture<Void> reportSystemUsageAsync(
            @NonNull ReportSystemUsageRequest request) {
        Preconditions.checkNotNull(request);
        Preconditions.checkState(!mIsClosed, "GlobalSearchSession has already been closed");
        return FutureUtil.execute(mExecutor, () -> {
            throw new AppSearchException(
                    AppSearchResult.RESULT_SECURITY_ERROR,
                    mContext.getPackageName() + " does not have access to report system usage");
        });
    }

    @SuppressLint("KotlinPropertyAccess")
    @Override
    public @NonNull ListenableFuture<GetSchemaResponse> getSchemaAsync(
            @NonNull String packageName, @NonNull String databaseName) {
        Preconditions.checkNotNull(packageName);
        Preconditions.checkNotNull(databaseName);
        Preconditions.checkState(!mIsClosed, "GlobalSearchSession has already been closed");
        return FutureUtil.execute(mExecutor,
                () -> mAppSearchImpl.getSchema(packageName, databaseName, mSelfCallerAccess,
                        /*callStatsBuilder=*/null));
    }

    @Override
    public @NonNull Features getFeatures() {
        return mFeatures;
    }

    @Override
    public void registerObserverCallback(
            @NonNull String targetPackageName,
            @NonNull ObserverSpec spec,
            @NonNull Executor executor,
            @NonNull ObserverCallback observer) {
        Preconditions.checkNotNull(targetPackageName);
        Preconditions.checkNotNull(spec);
        Preconditions.checkNotNull(executor);
        Preconditions.checkNotNull(observer);
        // LocalStorage does not support observing data from other packages.
        if (!targetPackageName.equals(mContext.getPackageName())) {
            throw new UnsupportedOperationException(
                    "Local storage implementation does not support receiving change notifications "
                            + "from other packages.");
        }
        mAppSearchImpl.registerObserverCallback(
                /*listeningPackageAccess=*/mSelfCallerAccess,
                /*targetPackageName=*/targetPackageName,
                spec,
                executor,
                observer);
    }

    @Override
    public void unregisterObserverCallback(
            @NonNull String targetPackageName, @NonNull ObserverCallback observer) {
        Preconditions.checkNotNull(targetPackageName);
        Preconditions.checkNotNull(observer);
        // LocalStorage does not support observing data from other packages.
        if (!targetPackageName.equals(mContext.getPackageName())) {
            throw new UnsupportedOperationException(
                    "Local storage implementation does not support receiving change notifications "
                            + "from other packages.");
        }
        mAppSearchImpl.unregisterObserverCallback(targetPackageName, observer);
    }

    @Override
    public void close() {
        mIsClosed = true;
    }
}
