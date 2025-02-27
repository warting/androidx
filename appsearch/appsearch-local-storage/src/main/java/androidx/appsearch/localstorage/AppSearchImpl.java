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

package androidx.appsearch.localstorage;

import static androidx.appsearch.app.AppSearchResult.RESULT_SECURITY_ERROR;
import static androidx.appsearch.app.InternalSetSchemaResponse.newFailedSetSchemaResponse;
import static androidx.appsearch.app.InternalSetSchemaResponse.newSuccessfulSetSchemaResponse;
import static androidx.appsearch.localstorage.util.PrefixUtil.addPrefixToDocument;
import static androidx.appsearch.localstorage.util.PrefixUtil.createPrefix;
import static androidx.appsearch.localstorage.util.PrefixUtil.getDatabaseName;
import static androidx.appsearch.localstorage.util.PrefixUtil.getPackageName;
import static androidx.appsearch.localstorage.util.PrefixUtil.getPrefix;
import static androidx.appsearch.localstorage.util.PrefixUtil.removePrefixesFromDocument;

import android.os.ParcelFileDescriptor;
import android.os.SystemClock;
import android.util.Log;

import androidx.annotation.GuardedBy;
import androidx.annotation.OptIn;
import androidx.annotation.RestrictTo;
import androidx.annotation.VisibleForTesting;
import androidx.annotation.WorkerThread;
import androidx.appsearch.app.AppSearchBlobHandle;
import androidx.appsearch.app.AppSearchResult;
import androidx.appsearch.app.AppSearchSchema;
import androidx.appsearch.app.ExperimentalAppSearchApi;
import androidx.appsearch.app.GenericDocument;
import androidx.appsearch.app.GetByDocumentIdRequest;
import androidx.appsearch.app.GetSchemaResponse;
import androidx.appsearch.app.InternalSetSchemaResponse;
import androidx.appsearch.app.InternalVisibilityConfig;
import androidx.appsearch.app.JoinSpec;
import androidx.appsearch.app.PackageIdentifier;
import androidx.appsearch.app.SchemaVisibilityConfig;
import androidx.appsearch.app.SearchResultPage;
import androidx.appsearch.app.SearchSpec;
import androidx.appsearch.app.SearchSuggestionResult;
import androidx.appsearch.app.SearchSuggestionSpec;
import androidx.appsearch.app.SetSchemaResponse;
import androidx.appsearch.app.StorageInfo;
import androidx.appsearch.exceptions.AppSearchException;
import androidx.appsearch.flags.Flags;
import androidx.appsearch.localstorage.converter.BlobHandleToProtoConverter;
import androidx.appsearch.localstorage.converter.GenericDocumentToProtoConverter;
import androidx.appsearch.localstorage.converter.ResultCodeToProtoConverter;
import androidx.appsearch.localstorage.converter.SchemaToProtoConverter;
import androidx.appsearch.localstorage.converter.SearchResultToProtoConverter;
import androidx.appsearch.localstorage.converter.SearchSpecToProtoConverter;
import androidx.appsearch.localstorage.converter.SearchSuggestionSpecToProtoConverter;
import androidx.appsearch.localstorage.converter.SetSchemaResponseToProtoConverter;
import androidx.appsearch.localstorage.converter.TypePropertyPathToProtoConverter;
import androidx.appsearch.localstorage.stats.InitializeStats;
import androidx.appsearch.localstorage.stats.OptimizeStats;
import androidx.appsearch.localstorage.stats.PutDocumentStats;
import androidx.appsearch.localstorage.stats.RemoveStats;
import androidx.appsearch.localstorage.stats.SearchStats;
import androidx.appsearch.localstorage.stats.SetSchemaStats;
import androidx.appsearch.localstorage.util.PrefixUtil;
import androidx.appsearch.localstorage.visibilitystore.CallerAccess;
import androidx.appsearch.localstorage.visibilitystore.VisibilityChecker;
import androidx.appsearch.localstorage.visibilitystore.VisibilityStore;
import androidx.appsearch.localstorage.visibilitystore.VisibilityUtil;
import androidx.appsearch.observer.ObserverCallback;
import androidx.appsearch.observer.ObserverSpec;
import androidx.appsearch.util.LogUtil;
import androidx.collection.ArrayMap;
import androidx.collection.ArraySet;
import androidx.core.util.ObjectsCompat;
import androidx.core.util.Preconditions;

import com.google.android.icing.IcingSearchEngine;
import com.google.android.icing.IcingSearchEngineInterface;
import com.google.android.icing.proto.BlobProto;
import com.google.android.icing.proto.DebugInfoProto;
import com.google.android.icing.proto.DebugInfoResultProto;
import com.google.android.icing.proto.DebugInfoVerbosity;
import com.google.android.icing.proto.DeleteByQueryResultProto;
import com.google.android.icing.proto.DeleteResultProto;
import com.google.android.icing.proto.DocumentProto;
import com.google.android.icing.proto.DocumentStorageInfoProto;
import com.google.android.icing.proto.GetAllNamespacesResultProto;
import com.google.android.icing.proto.GetOptimizeInfoResultProto;
import com.google.android.icing.proto.GetResultProto;
import com.google.android.icing.proto.GetResultSpecProto;
import com.google.android.icing.proto.GetSchemaResultProto;
import com.google.android.icing.proto.IcingSearchEngineOptions;
import com.google.android.icing.proto.InitializeResultProto;
import com.google.android.icing.proto.LogSeverity;
import com.google.android.icing.proto.NamespaceBlobStorageInfoProto;
import com.google.android.icing.proto.NamespaceStorageInfoProto;
import com.google.android.icing.proto.OptimizeResultProto;
import com.google.android.icing.proto.PersistToDiskResultProto;
import com.google.android.icing.proto.PersistType;
import com.google.android.icing.proto.PropertyConfigProto;
import com.google.android.icing.proto.PropertyProto;
import com.google.android.icing.proto.PutResultProto;
import com.google.android.icing.proto.ReportUsageResultProto;
import com.google.android.icing.proto.ResetResultProto;
import com.google.android.icing.proto.ResultSpecProto;
import com.google.android.icing.proto.SchemaProto;
import com.google.android.icing.proto.SchemaTypeConfigProto;
import com.google.android.icing.proto.ScoringSpecProto;
import com.google.android.icing.proto.SearchResultProto;
import com.google.android.icing.proto.SearchSpecProto;
import com.google.android.icing.proto.SetSchemaResultProto;
import com.google.android.icing.proto.StatusProto;
import com.google.android.icing.proto.StorageInfoProto;
import com.google.android.icing.proto.StorageInfoResultProto;
import com.google.android.icing.proto.SuggestionResponse;
import com.google.android.icing.proto.TypePropertyMask;
import com.google.android.icing.proto.UsageReport;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Manages interaction with the native IcingSearchEngine and other components to implement AppSearch
 * functionality.
 *
 * <p>Never create two instances using the same folder.
 *
 * <p>A single instance of {@link AppSearchImpl} can support all packages and databases.
 * This is done by combining the package and database name into a unique prefix and
 * prefixing the schemas and documents stored under that owner. Schemas and documents are
 * physically saved together in {@link IcingSearchEngine}, but logically isolated:
 * <ul>
 *      <li>Rewrite SchemaType in SchemaProto by adding the package-database prefix and save into
 *          SchemaTypes set in {@link #setSchema}.
 *      <li>Rewrite namespace and SchemaType in DocumentProto by adding package-database prefix and
 *          save to namespaces set in {@link #putDocument}.
 *      <li>Remove package-database prefix when retrieving documents in {@link #getDocument} and
 *          {@link #query}.
 *      <li>Rewrite filters in {@link SearchSpecProto} to have all namespaces and schema types of
 *          the queried database when user using empty filters in {@link #query}.
 * </ul>
 *
 * <p>Methods in this class belong to two groups, the query group and the mutate group.
 * <ul>
 *     <li>All methods are going to modify global parameters and data in Icing are executed under
 *         WRITE lock to keep thread safety.
 *     <li>All methods are going to access global parameters or query data from Icing are executed
 *         under READ lock to improve query performance.
 * </ul>
 *
 * <p>This class is thread safe.
 *
 * @exportToFramework:hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@WorkerThread
public final class AppSearchImpl implements Closeable {
    private static final String TAG = "AppSearchImpl";

    /** A value 0 means that there're no more pages in the search results. */
    private static final long EMPTY_PAGE_TOKEN = 0;
    @VisibleForTesting
    static final int CHECK_OPTIMIZE_INTERVAL = 100;

    /** A GetResultSpec that uses projection to skip all properties. */
    private static final GetResultSpecProto GET_RESULT_SPEC_NO_PROPERTIES =
            GetResultSpecProto.newBuilder().addTypePropertyMasks(
                    TypePropertyMask.newBuilder().setSchemaType(
                            GetByDocumentIdRequest.PROJECTION_SCHEMA_TYPE_WILDCARD)).build();

    private final ReadWriteLock mReadWriteLock = new ReentrantReadWriteLock();
    private final OptimizeStrategy mOptimizeStrategy;
    private final AppSearchConfig mConfig;
    private final File mBlobFilesDir;

    @GuardedBy("mReadWriteLock")
    @VisibleForTesting
    final IcingSearchEngineInterface mIcingSearchEngineLocked;

    @GuardedBy("mReadWriteLock")
    private final SchemaCache mSchemaCacheLocked = new SchemaCache();

    @GuardedBy("mReadWriteLock")
    private final NamespaceCache mNamespaceCacheLocked = new NamespaceCache();

    // Marked as volatile because a new instance may be assigned during resetLocked.
    @GuardedBy("mReadWriteLock")
    private volatile DocumentLimiter mDocumentLimiterLocked;

    // Maps packages to the set of valid nextPageTokens that the package can manipulate. A token
    // is unique and constant per query (i.e. the same token '123' is used to iterate through
    // pages of search results). The tokens themselves are generated and tracked by
    // IcingSearchEngine. IcingSearchEngine considers a token valid and won't be reused
    // until we call invalidateNextPageToken on the token.
    //
    // Note that we synchronize on itself because the nextPageToken cache is checked at
    // query-time, and queries are done in parallel with a read lock. Ideally, this would be
    // guarded by the normal mReadWriteLock.writeLock, but ReentrantReadWriteLocks can't upgrade
    // read to write locks. This lock should be acquired at the smallest scope possible.
    // mReadWriteLock is a higher-level lock, so calls shouldn't be made out
    // to any functions that grab the lock.
    @GuardedBy("mNextPageTokensLocked")
    private final Map<String, Set<Long>> mNextPageTokensLocked = new ArrayMap<>();

    private final ObserverManager mObserverManager = new ObserverManager();

    /**
     * VisibilityStore will be used in {@link #setSchema} and {@link #getSchema} to store and query
     * visibility information. But to create a {@link VisibilityStore}, it will call
     * {@link #setSchema} and {@link #getSchema} to get the visibility schema. Make it nullable to
     * avoid call it before we actually create it.
     */
    @VisibleForTesting
    @GuardedBy("mReadWriteLock")
    final @Nullable VisibilityStore mDocumentVisibilityStoreLocked;

    @VisibleForTesting
    @GuardedBy("mReadWriteLock")
    final @Nullable VisibilityStore mBlobVisibilityStoreLocked;

    @GuardedBy("mReadWriteLock")
    private final @Nullable VisibilityChecker mVisibilityCheckerLocked;

    /**
     * The counter to check when to call {@link #checkForOptimize}. The
     * interval is
     * {@link #CHECK_OPTIMIZE_INTERVAL}.
     */
    @GuardedBy("mReadWriteLock")
    private int mOptimizeIntervalCountLocked = 0;

    @ExperimentalAppSearchApi
    private final @Nullable RevocableFileDescriptorStore mRevocableFileDescriptorStore;

    /** Whether this instance has been closed, and therefore unusable. */
    @GuardedBy("mReadWriteLock")
    private boolean mClosedLocked = false;

    /**
     * Creates and initializes an instance of {@link AppSearchImpl} which writes data to the given
     * folder.
     *
     * <p>Clients can pass a {@link AppSearchLogger} here through their AppSearchSession, but it
     * can't be saved inside {@link AppSearchImpl}, because the impl will be shared by all the
     * sessions for the same package in JetPack.
     *
     * <p>Instead, logger instance needs to be passed to each individual method, like create, query
     * and putDocument.
     *
     * @param initStatsBuilder  collects stats for initialization if provided.
     * @param visibilityChecker The {@link VisibilityChecker} that check whether the caller has
     *                          access to aa specific schema. Pass null will lost that ability and
     *                          global querier could only get their own data.
     * @param icingSearchEngine the underlying icing instance to use. If not provided, a new {@link
     *     IcingSearchEngine} instance will be created and used.
     */
    @OptIn(markerClass = ExperimentalAppSearchApi.class)
    public static @NonNull AppSearchImpl create(
            @NonNull File icingDir,
            @NonNull AppSearchConfig config,
            InitializeStats.@Nullable Builder initStatsBuilder,
            @Nullable VisibilityChecker visibilityChecker,
            @Nullable RevocableFileDescriptorStore revocableFileDescriptorStore,
            @Nullable IcingSearchEngineInterface icingSearchEngine,
            @NonNull OptimizeStrategy optimizeStrategy)
            throws AppSearchException {
        return new AppSearchImpl(icingDir, config, initStatsBuilder, visibilityChecker,
                revocableFileDescriptorStore, icingSearchEngine, optimizeStrategy);
    }

    /**
     * @param initStatsBuilder collects stats for initialization if provided.
     */
    @OptIn(markerClass = ExperimentalAppSearchApi.class)
    private AppSearchImpl(
            @NonNull File icingDir,
            @NonNull AppSearchConfig config,
            InitializeStats.@Nullable Builder initStatsBuilder,
            @Nullable VisibilityChecker visibilityChecker,
            @Nullable RevocableFileDescriptorStore revocableFileDescriptorStore,
            @Nullable IcingSearchEngineInterface icingSearchEngine,
            @NonNull OptimizeStrategy optimizeStrategy)
            throws AppSearchException {
        Preconditions.checkNotNull(icingDir);
        // This directory stores blob files. It is the same directory that Icing used to manage
        // blob files when Flags.enableAppSearchManageBlobFiles() was false. After the rollout of
        // this flag, AppSearch will continue to manage blob files in this same directory within
        // Icing's directory. The location remains unchanged to ensure that the flag does not
        // introduce any behavioral changes.
        mBlobFilesDir = new File(icingDir, "blob_dir/blob_files");
        mConfig = Preconditions.checkNotNull(config);
        mOptimizeStrategy = Preconditions.checkNotNull(optimizeStrategy);
        mVisibilityCheckerLocked = visibilityChecker;
        mRevocableFileDescriptorStore = revocableFileDescriptorStore;

        mReadWriteLock.writeLock().lock();
        try {
            // We synchronize here because we don't want to call IcingSearchEngine.initialize() more
            // than once. It's unnecessary and can be a costly operation.
            if (icingSearchEngine == null) {
                IcingSearchEngineOptions options = mConfig.toIcingSearchEngineOptions(
                        icingDir.getAbsolutePath());
                LogUtil.piiTrace(TAG, "Constructing IcingSearchEngine, request", options);
                mIcingSearchEngineLocked = new IcingSearchEngine(options);
                LogUtil.piiTrace(
                        TAG,
                        "Constructing IcingSearchEngine, response",
                        ObjectsCompat.hashCode(mIcingSearchEngineLocked));
            } else {
                mIcingSearchEngineLocked = icingSearchEngine;
            }

            // The core initialization procedure. If any part of this fails, we bail into
            // resetLocked(), deleting all data (but hopefully allowing AppSearchImpl to come up).
            try {
                LogUtil.piiTrace(TAG, "icingSearchEngine.initialize, request");
                InitializeResultProto initializeResultProto = mIcingSearchEngineLocked.initialize();
                LogUtil.piiTrace(
                        TAG,
                        "icingSearchEngine.initialize, response",
                        initializeResultProto.getStatus(),
                        initializeResultProto);

                if (initStatsBuilder != null) {
                    initStatsBuilder
                            .setStatusCode(
                                    statusProtoToResultCode(initializeResultProto.getStatus()))
                            // TODO(b/173532925) how to get DeSyncs value
                            .setHasDeSync(false);
                    AppSearchLoggerHelper.copyNativeStats(
                            initializeResultProto.getInitializeStats(), initStatsBuilder);
                }
                checkSuccess(initializeResultProto.getStatus());

                if (Flags.enableAppSearchManageBlobFiles() && !mBlobFilesDir.exists()
                        && !mBlobFilesDir.mkdirs()) {
                    throw new AppSearchException(AppSearchResult.RESULT_IO_ERROR,
                            "Cannot create the blob file directory: "
                                    + mBlobFilesDir.getAbsolutePath());
                }

                // Read all protos we need to construct AppSearchImpl's cache maps
                long prepareSchemaAndNamespacesLatencyStartMillis = SystemClock.elapsedRealtime();
                SchemaProto schemaProto = getSchemaProtoLocked();

                LogUtil.piiTrace(TAG, "init:getAllNamespaces, request");
                GetAllNamespacesResultProto getAllNamespacesResultProto =
                        mIcingSearchEngineLocked.getAllNamespaces();
                LogUtil.piiTrace(
                        TAG,
                        "init:getAllNamespaces, response",
                        getAllNamespacesResultProto.getNamespacesCount(),
                        getAllNamespacesResultProto);

                StorageInfoProto storageInfoProto = getRawStorageInfoProto();

                // Log the time it took to read the data that goes into the cache maps
                if (initStatsBuilder != null) {
                    // In case there is some error for getAllNamespaces, we can still
                    // set the latency for preparation.
                    // If there is no error, the value will be overridden by the actual one later.
                    initStatsBuilder.setStatusCode(
                            statusProtoToResultCode(getAllNamespacesResultProto.getStatus()))
                            .setPrepareSchemaAndNamespacesLatencyMillis(
                                    (int) (SystemClock.elapsedRealtime()
                                            - prepareSchemaAndNamespacesLatencyStartMillis));
                }
                checkSuccess(getAllNamespacesResultProto.getStatus());

                // Populate schema map
                List<SchemaTypeConfigProto> schemaProtoTypesList = schemaProto.getTypesList();
                for (int i = 0; i < schemaProtoTypesList.size(); i++) {
                    SchemaTypeConfigProto schema = schemaProtoTypesList.get(i);
                    String prefixedSchemaType = schema.getSchemaType();
                    mSchemaCacheLocked.addToSchemaMap(getPrefix(prefixedSchemaType), schema);
                }

                // Populate schema parent-to-children map
                mSchemaCacheLocked.rebuildCache();

                // Populate namespace map
                List<String> prefixedNamespaceList =
                        getAllNamespacesResultProto.getNamespacesList();
                for (int i = 0; i < prefixedNamespaceList.size(); i++) {
                    String prefixedNamespace = prefixedNamespaceList.get(i);
                    mNamespaceCacheLocked.addToDocumentNamespaceMap(
                            getPrefix(prefixedNamespace), prefixedNamespace);
                }

                // Populate blob namespace map
                if (mRevocableFileDescriptorStore != null) {
                    List<NamespaceBlobStorageInfoProto> namespaceBlobStorageInfoProto =
                            storageInfoProto.getNamespaceBlobStorageInfoList();
                    for (int i = 0; i < namespaceBlobStorageInfoProto.size(); i++) {
                        String prefixedNamespace = namespaceBlobStorageInfoProto.get(
                                i).getNamespace();
                        mNamespaceCacheLocked.addToBlobNamespaceMap(
                                getPrefix(prefixedNamespace), prefixedNamespace);
                    }
                }

                // Populate document count map
                mDocumentLimiterLocked =
                        new DocumentLimiter(
                                mConfig.getDocumentCountLimitStartThreshold(),
                                mConfig.getPerPackageDocumentCountLimit(),
                                storageInfoProto.getDocumentStorageInfo()
                                        .getNamespaceStorageInfoList());

                // logging prepare_schema_and_namespaces latency
                if (initStatsBuilder != null) {
                    initStatsBuilder.setPrepareSchemaAndNamespacesLatencyMillis(
                            (int) (SystemClock.elapsedRealtime()
                                    - prepareSchemaAndNamespacesLatencyStartMillis));
                }

                LogUtil.piiTrace(TAG, "Init completed successfully");

            } catch (AppSearchException e) {
                // Some error. Reset and see if it fixes it.
                Log.e(TAG, "Error initializing, resetting IcingSearchEngine.", e);
                if (initStatsBuilder != null) {
                    initStatsBuilder.setStatusCode(e.getResultCode());
                }
                resetLocked(initStatsBuilder);
            }

            // AppSearchImpl core parameters are initialized and we should be able to build
            // VisibilityStores based on that. We shouldn't wipe out everything if we only failed to
            // build VisibilityStores.
            long prepareVisibilityStoreLatencyStartMillis = SystemClock.elapsedRealtime();
            mDocumentVisibilityStoreLocked = VisibilityStore.createDocumentVisibilityStore(this);
            if (mRevocableFileDescriptorStore != null) {
                mBlobVisibilityStoreLocked = VisibilityStore.createBlobVisibilityStore(this);
            } else {
                mBlobVisibilityStoreLocked = null;
            }
            long prepareVisibilityStoreLatencyEndMillis = SystemClock.elapsedRealtime();
            if (initStatsBuilder != null) {
                initStatsBuilder.setPrepareVisibilityStoreLatencyMillis((int)
                        (prepareVisibilityStoreLatencyEndMillis
                                - prepareVisibilityStoreLatencyStartMillis));
            }
        } finally {
            mReadWriteLock.writeLock().unlock();
        }
    }

    @GuardedBy("mReadWriteLock")
    private void throwIfClosedLocked() {
        if (mClosedLocked) {
            throw new IllegalStateException("Trying to use a closed AppSearchImpl instance.");
        }
    }

    /**
     * Persists data to disk and closes the instance.
     *
     * <p>This instance is no longer usable after it's been closed. Call {@link #create} to
     * create a new, usable instance.
     */
    @Override
    @OptIn(markerClass = ExperimentalAppSearchApi.class)
    public void close() {
        mReadWriteLock.writeLock().lock();
        try {
            if (mClosedLocked) {
                return;
            }
            persistToDisk(PersistType.Code.FULL);
            LogUtil.piiTrace(TAG, "icingSearchEngine.close, request");
            mIcingSearchEngineLocked.close();
            LogUtil.piiTrace(TAG, "icingSearchEngine.close, response");
            if (mRevocableFileDescriptorStore != null) {
                mRevocableFileDescriptorStore.revokeAll();
            }
            mClosedLocked = true;
        } catch (AppSearchException | IOException e) {
            Log.w(TAG, "Error when closing AppSearchImpl.", e);
        } finally {
            mReadWriteLock.writeLock().unlock();
        }
    }

    /**
     * Returns the instance of AppSearchConfig used by this instance of AppSearchImpl.
     */
    public @NonNull AppSearchConfig getConfig() {
        return mConfig;
    }

    /**
     * Updates the AppSearch schema for this app.
     *
     * <p>This method belongs to mutate group.
     *
     * @param packageName                 The package name that owns the schemas.
     * @param databaseName                The name of the database where this schema lives.
     * @param schemas                     Schemas to set for this app.
     * @param visibilityConfigs           {@link InternalVisibilityConfig}s that contain all
     *                                    visibility setting information for those schemas
     *                                    has user custom settings. Other schemas in the list
     *                                    that don't has a {@link InternalVisibilityConfig}
     *                                    will be treated as having the default visibility,
     *                                    which is accessible by the system and no other packages.
     * @param forceOverride               Whether to force-apply the schema even if it is
     *                                    incompatible. Documents
     *                                    which do not comply with the new schema will be deleted.
     * @param version                     The overall version number of the request.
     * @param setSchemaStatsBuilder       Builder for {@link SetSchemaStats} to hold stats for
     *                                    setSchema
     * @return A success {@link InternalSetSchemaResponse} with a {@link SetSchemaResponse}. Or a
     * failed {@link InternalSetSchemaResponse} if this call contains incompatible change. The
     * {@link SetSchemaResponse} in the failed {@link InternalSetSchemaResponse} contains which type
     * is incompatible. You need to check the status by
     * {@link InternalSetSchemaResponse#isSuccess()}.
     *
     * @throws AppSearchException On IcingSearchEngine error. If the status code is
     *                            FAILED_PRECONDITION for the incompatible change, the
     *                            exception will be converted to the SetSchemaResponse.
     */
    public @NonNull InternalSetSchemaResponse setSchema(
            @NonNull String packageName,
            @NonNull String databaseName,
            @NonNull List<AppSearchSchema> schemas,
            @NonNull List<InternalVisibilityConfig> visibilityConfigs,
            boolean forceOverride,
            int version,
            SetSchemaStats.@Nullable Builder setSchemaStatsBuilder) throws AppSearchException {
        long javaLockAcquisitionLatencyStartMillis = SystemClock.elapsedRealtime();
        mReadWriteLock.writeLock().lock();
        try {
            throwIfClosedLocked();
            if (setSchemaStatsBuilder != null) {
                setSchemaStatsBuilder.setJavaLockAcquisitionLatencyMillis(
                        (int) (SystemClock.elapsedRealtime()
                                - javaLockAcquisitionLatencyStartMillis));
            }
            if (mObserverManager.isPackageObserved(packageName)) {
                return doSetSchemaWithChangeNotificationLocked(
                        packageName,
                        databaseName,
                        schemas,
                        visibilityConfigs,
                        forceOverride,
                        version,
                        setSchemaStatsBuilder);
            } else {
                return doSetSchemaNoChangeNotificationLocked(
                        packageName,
                        databaseName,
                        schemas,
                        visibilityConfigs,
                        forceOverride,
                        version,
                        setSchemaStatsBuilder);
            }
        } finally {
            mReadWriteLock.writeLock().unlock();
        }
    }

    /**
     * Updates the AppSearch schema for this app, dispatching change notifications.
     *
     * @see #setSchema
     * @see #doSetSchemaNoChangeNotificationLocked
     */
    @GuardedBy("mReadWriteLock")
    private @NonNull InternalSetSchemaResponse doSetSchemaWithChangeNotificationLocked(
            @NonNull String packageName,
            @NonNull String databaseName,
            @NonNull List<AppSearchSchema> schemas,
            @NonNull List<InternalVisibilityConfig> visibilityConfigs,
            boolean forceOverride,
            int version,
            SetSchemaStats.@Nullable Builder setSchemaStatsBuilder) throws AppSearchException {
        // First, capture the old state of the system. This includes the old schema as well as
        // whether each registered observer can access each type. Once VisibilityStore is updated
        // by the setSchema call, the information of which observers could see which types will be
        // lost.
        long getOldSchemaStartTimeMillis = SystemClock.elapsedRealtime();
        GetSchemaResponse oldSchema = getSchema(
                packageName,
                databaseName,
                // A CallerAccess object for internal use that has local access to this database.
                new CallerAccess(/*callingPackageName=*/packageName));
        long getOldSchemaEndTimeMillis = SystemClock.elapsedRealtime();
        if (setSchemaStatsBuilder != null) {
            setSchemaStatsBuilder.setIsPackageObserved(true)
                    .setGetOldSchemaLatencyMillis(
                            (int) (getOldSchemaEndTimeMillis - getOldSchemaStartTimeMillis));
        }

        long getOldSchemaObserverStartTimeMillis = SystemClock.elapsedRealtime();
        // Cache some lookup tables to help us work with the old schema
        Set<AppSearchSchema> oldSchemaTypes = oldSchema.getSchemas();
        Map<String, AppSearchSchema> oldSchemaNameToType = new ArrayMap<>(oldSchemaTypes.size());
        // Maps unprefixed schema name to the set of listening packages that had visibility into
        // that type under the old schema.
        Map<String, Set<String>> oldSchemaNameToVisibleListeningPackage =
                new ArrayMap<>(oldSchemaTypes.size());
        for (AppSearchSchema oldSchemaType : oldSchemaTypes) {
            String oldSchemaName = oldSchemaType.getSchemaType();
            oldSchemaNameToType.put(oldSchemaName, oldSchemaType);
            oldSchemaNameToVisibleListeningPackage.put(
                    oldSchemaName,
                    mObserverManager.getObserversForSchemaType(
                            packageName,
                            databaseName,
                            oldSchemaName,
                            mDocumentVisibilityStoreLocked,
                            mVisibilityCheckerLocked));
        }
        int getOldSchemaObserverLatencyMillis =
                (int) (SystemClock.elapsedRealtime() - getOldSchemaObserverStartTimeMillis);

        // Apply the new schema
        InternalSetSchemaResponse internalSetSchemaResponse = doSetSchemaNoChangeNotificationLocked(
                packageName,
                databaseName,
                schemas,
                visibilityConfigs,
                forceOverride,
                version,
                setSchemaStatsBuilder);

        // This check is needed wherever setSchema is called to detect soft errors which do not
        // throw an exception but also prevent the schema from actually being applied.
        if (!internalSetSchemaResponse.isSuccess()) {
            return internalSetSchemaResponse;
        }

        long getNewSchemaObserverStartTimeMillis = SystemClock.elapsedRealtime();
        // Cache some lookup tables to help us work with the new schema
        Map<String, AppSearchSchema> newSchemaNameToType = new ArrayMap<>(schemas.size());
        // Maps unprefixed schema name to the set of listening packages that have visibility into
        // that type under the new schema.
        Map<String, Set<String>> newSchemaNameToVisibleListeningPackage =
                new ArrayMap<>(schemas.size());
        for (AppSearchSchema newSchemaType : schemas) {
            String newSchemaName = newSchemaType.getSchemaType();
            newSchemaNameToType.put(newSchemaName, newSchemaType);
            newSchemaNameToVisibleListeningPackage.put(
                    newSchemaName,
                    mObserverManager.getObserversForSchemaType(
                            packageName,
                            databaseName,
                            newSchemaName,
                            mDocumentVisibilityStoreLocked,
                            mVisibilityCheckerLocked));
        }
        long getNewSchemaObserverEndTimeMillis = SystemClock.elapsedRealtime();
        if (setSchemaStatsBuilder != null) {
            setSchemaStatsBuilder.setGetObserverLatencyMillis(getOldSchemaObserverLatencyMillis
                    + (int) (getNewSchemaObserverEndTimeMillis
                    - getNewSchemaObserverStartTimeMillis));
        }

        long preparingChangeNotificationStartTimeMillis = SystemClock.elapsedRealtime();
        // Create a unified set of all schema names mentioned in either the old or new schema.
        Set<String> allSchemaNames = new ArraySet<>(oldSchemaNameToType.keySet());
        allSchemaNames.addAll(newSchemaNameToType.keySet());

        // Perform the diff between the old and new schema.
        for (String schemaName : allSchemaNames) {
            final AppSearchSchema contentBefore = oldSchemaNameToType.get(schemaName);
            final AppSearchSchema contentAfter = newSchemaNameToType.get(schemaName);

            final boolean existBefore = (contentBefore != null);
            final boolean existAfter = (contentAfter != null);

            // This should never happen
            if (!existBefore && !existAfter) {
                continue;
            }

            boolean contentsChanged = true;
            if (contentBefore != null
                    && contentBefore.equals(contentAfter)) {
                contentsChanged = false;
            }

            Set<String> oldVisibleListeners =
                    oldSchemaNameToVisibleListeningPackage.get(schemaName);
            Set<String> newVisibleListeners =
                    newSchemaNameToVisibleListeningPackage.get(schemaName);
            Set<String> allListeningPackages = new ArraySet<>(oldVisibleListeners);
            if (newVisibleListeners != null) {
                allListeningPackages.addAll(newVisibleListeners);
            }

            // Now that we've computed the relationship between the old and new schema, we go
            // observer by observer and consider the observer's own personal view of the schema.
            for (String listeningPackageName : allListeningPackages) {
                // Figure out the visibility
                final boolean visibleBefore = (
                        existBefore
                                && oldVisibleListeners != null
                                && oldVisibleListeners.contains(listeningPackageName));
                final boolean visibleAfter = (
                        existAfter
                                && newVisibleListeners != null
                                && newVisibleListeners.contains(listeningPackageName));

                // Now go through the truth table of all the relevant flags.
                // visibleBefore and visibleAfter take into account existBefore and existAfter, so
                // we can stop worrying about existBefore and existAfter.
                boolean sendNotification = false;
                if (visibleBefore && visibleAfter && contentsChanged) {
                    sendNotification = true;  // Type configuration was modified
                } else if (!visibleBefore && visibleAfter) {
                    sendNotification = true;  // Newly granted visibility or type was created
                } else if (visibleBefore && !visibleAfter) {
                    sendNotification = true;  // Revoked visibility or type was deleted
                } else {
                    // No visibility before and no visibility after. Nothing to dispatch.
                }

                if (sendNotification) {
                    mObserverManager.onSchemaChange(
                            /*listeningPackageName=*/listeningPackageName,
                            /*targetPackageName=*/packageName,
                            /*databaseName=*/databaseName,
                            /*schemaName=*/schemaName);
                }
            }
        }
        if (setSchemaStatsBuilder != null) {
            setSchemaStatsBuilder.setPreparingChangeNotificationLatencyMillis(
                    (int) (SystemClock.elapsedRealtime()
                            - preparingChangeNotificationStartTimeMillis));
        }

        return internalSetSchemaResponse;
    }

    /**
     * Updates the AppSearch schema for this app, without dispatching change notifications.
     *
     * <p>This method can be used only when no one is observing {@code packageName}.
     *
     * @see #setSchema
     * @see #doSetSchemaWithChangeNotificationLocked
     */
    @GuardedBy("mReadWriteLock")
    private @NonNull InternalSetSchemaResponse doSetSchemaNoChangeNotificationLocked(
            @NonNull String packageName,
            @NonNull String databaseName,
            @NonNull List<AppSearchSchema> schemas,
            @NonNull List<InternalVisibilityConfig> visibilityConfigs,
            boolean forceOverride,
            int version,
            SetSchemaStats.@Nullable Builder setSchemaStatsBuilder) throws AppSearchException {
        long setRewriteSchemaLatencyStartTimeMillis = SystemClock.elapsedRealtime();
        SchemaProto.Builder existingSchemaBuilder = getSchemaProtoLocked().toBuilder();

        SchemaProto.Builder newSchemaBuilder = SchemaProto.newBuilder();
        for (int i = 0; i < schemas.size(); i++) {
            AppSearchSchema schema = schemas.get(i);
            SchemaTypeConfigProto schemaTypeProto =
                    SchemaToProtoConverter.toSchemaTypeConfigProto(schema, version);
            newSchemaBuilder.addTypes(schemaTypeProto);
        }

        String prefix = createPrefix(packageName, databaseName);
        // Combine the existing schema (which may have types from other prefixes) with this
        // prefix's new schema. Modifies the existingSchemaBuilder.
        RewrittenSchemaResults rewrittenSchemaResults = rewriteSchema(prefix,
                existingSchemaBuilder,
                newSchemaBuilder.build());

        long rewriteSchemaEndTimeMillis = SystemClock.elapsedRealtime();
        if (setSchemaStatsBuilder != null) {
            setSchemaStatsBuilder.setRewriteSchemaLatencyMillis(
                    (int) (rewriteSchemaEndTimeMillis - setRewriteSchemaLatencyStartTimeMillis));
        }

        // Apply schema
        long nativeLatencyStartTimeMillis = SystemClock.elapsedRealtime();
        SchemaProto finalSchema = existingSchemaBuilder.build();
        LogUtil.piiTrace(TAG, "setSchema, request", finalSchema.getTypesCount(), finalSchema);
        SetSchemaResultProto setSchemaResultProto =
                mIcingSearchEngineLocked.setSchema(finalSchema, forceOverride);
        LogUtil.piiTrace(
                TAG, "setSchema, response", setSchemaResultProto.getStatus(), setSchemaResultProto);
        long nativeLatencyEndTimeMillis = SystemClock.elapsedRealtime();
        if (setSchemaStatsBuilder != null) {
            setSchemaStatsBuilder
                    .setTotalNativeLatencyMillis(
                            (int) (nativeLatencyEndTimeMillis - nativeLatencyStartTimeMillis))
                    .setStatusCode(statusProtoToResultCode(
                            setSchemaResultProto.getStatus()));
            AppSearchLoggerHelper.copyNativeStats(setSchemaResultProto,
                    setSchemaStatsBuilder);
        }

        boolean isFailedPrecondition = setSchemaResultProto.getStatus().getCode()
                == StatusProto.Code.FAILED_PRECONDITION;
        // Determine whether it succeeded.
        try {
            checkSuccess(setSchemaResultProto.getStatus());
        } catch (AppSearchException e) {
            // Swallow the exception for the incompatible change case. We will generate a failed
            // InternalSetSchemaResponse for this case.
            int deletedTypes = setSchemaResultProto.getDeletedSchemaTypesCount();
            int incompatibleTypes = setSchemaResultProto.getIncompatibleSchemaTypesCount();
            boolean isIncompatible = deletedTypes > 0 || incompatibleTypes > 0;
            if (isFailedPrecondition && !forceOverride  && isIncompatible) {
                SetSchemaResponse setSchemaResponse = SetSchemaResponseToProtoConverter
                        .toSetSchemaResponse(setSchemaResultProto, prefix);
                String errorMessage = "Schema is incompatible."
                        + "\n  Deleted types: " + setSchemaResponse.getDeletedTypes()
                        + "\n  Incompatible types: " + setSchemaResponse.getIncompatibleTypes();
                return newFailedSetSchemaResponse(setSchemaResponse, errorMessage);
            } else {
                throw e;
            }
        }

        long saveVisibilitySettingStartTimeMillis = SystemClock.elapsedRealtime();
        // Update derived data structures.
        for (SchemaTypeConfigProto schemaTypeConfigProto :
                rewrittenSchemaResults.mRewrittenPrefixedTypes.values()) {
            mSchemaCacheLocked.addToSchemaMap(prefix, schemaTypeConfigProto);
        }

        for (String schemaType : rewrittenSchemaResults.mDeletedPrefixedTypes) {
            mSchemaCacheLocked.removeFromSchemaMap(prefix, schemaType);
        }

        mSchemaCacheLocked.rebuildCacheForPrefix(prefix);

        // Since the constructor of VisibilityStore will set schema. Avoid call visibility
        // store before we have already created it.
        if (mDocumentVisibilityStoreLocked != null) {
            // Add prefix to all visibility documents.
            // Find out which Visibility document is deleted or changed to all-default settings.
            // We need to remove them from Visibility Store.
            Set<String> deprecatedVisibilityDocuments =
                    new ArraySet<>(rewrittenSchemaResults.mRewrittenPrefixedTypes.keySet());
            List<InternalVisibilityConfig> prefixedVisibilityConfigs = rewriteVisibilityConfigs(
                    prefix, visibilityConfigs, deprecatedVisibilityDocuments);
            // Now deprecatedVisibilityDocuments contains those existing schemas that has
            // all-default visibility settings, add deleted schemas. That's all we need to
            // remove.
            deprecatedVisibilityDocuments.addAll(rewrittenSchemaResults.mDeletedPrefixedTypes);
            mDocumentVisibilityStoreLocked.removeVisibility(deprecatedVisibilityDocuments);
            mDocumentVisibilityStoreLocked.setVisibility(prefixedVisibilityConfigs);
        }
        long saveVisibilitySettingEndTimeMillis = SystemClock.elapsedRealtime();
        if (setSchemaStatsBuilder != null) {
            setSchemaStatsBuilder.setVisibilitySettingLatencyMillis(
                    (int) (saveVisibilitySettingEndTimeMillis
                            - saveVisibilitySettingStartTimeMillis));
        }

        long convertToResponseStartTimeMillis = SystemClock.elapsedRealtime();
        InternalSetSchemaResponse setSchemaResponse = newSuccessfulSetSchemaResponse(
                SetSchemaResponseToProtoConverter
                        .toSetSchemaResponse(setSchemaResultProto, prefix));
        long convertToResponseEndTimeMillis = SystemClock.elapsedRealtime();
        if (setSchemaStatsBuilder != null) {
            setSchemaStatsBuilder.setConvertToResponseLatencyMillis(
                    (int) (convertToResponseEndTimeMillis
                            - convertToResponseStartTimeMillis));
        }
        return setSchemaResponse;
    }

    /**
     * Retrieves the AppSearch schema for this package name, database.
     *
     * <p>This method belongs to query group.
     *
     * @param packageName  Package that owns the requested {@link AppSearchSchema} instances.
     * @param databaseName Database that owns the requested {@link AppSearchSchema} instances.
     * @param callerAccess Visibility access info of the calling app
     * @throws AppSearchException on IcingSearchEngine error.
     */
    public @NonNull GetSchemaResponse getSchema(
            @NonNull String packageName,
            @NonNull String databaseName,
            @NonNull CallerAccess callerAccess)
            throws AppSearchException {
        mReadWriteLock.readLock().lock();
        try {
            throwIfClosedLocked();

            SchemaProto fullSchema = getSchemaProtoLocked();
            String prefix = createPrefix(packageName, databaseName);
            GetSchemaResponse.Builder responseBuilder = new GetSchemaResponse.Builder();
            for (int i = 0; i < fullSchema.getTypesCount(); i++) {
                // Check that this type belongs to the requested app and that the caller has
                // access to it.
                SchemaTypeConfigProto typeConfig = fullSchema.getTypes(i);
                String prefixedSchemaType = typeConfig.getSchemaType();
                String typePrefix = getPrefix(prefixedSchemaType);
                if (!prefix.equals(typePrefix)) {
                    // This schema type doesn't belong to the database we're querying for.
                    continue;
                }
                if (!VisibilityUtil.isSchemaSearchableByCaller(
                        callerAccess,
                        packageName,
                        prefixedSchemaType,
                        mDocumentVisibilityStoreLocked,
                        mVisibilityCheckerLocked)) {
                    // Caller doesn't have access to this type.
                    continue;
                }

                // Rewrite SchemaProto.types.schema_type
                SchemaTypeConfigProto.Builder typeConfigBuilder = typeConfig.toBuilder();
                PrefixUtil.removePrefixesFromSchemaType(typeConfigBuilder);
                AppSearchSchema schema = SchemaToProtoConverter.toAppSearchSchema(
                        typeConfigBuilder);

                responseBuilder.setVersion(typeConfig.getVersion());
                responseBuilder.addSchema(schema);

                // Populate visibility info. Since the constructor of VisibilityStore will get
                // schema. Avoid call visibility store before we have already created it.
                if (mDocumentVisibilityStoreLocked != null) {
                    String typeName = typeConfig.getSchemaType().substring(typePrefix.length());
                    InternalVisibilityConfig visibilityConfig =
                            mDocumentVisibilityStoreLocked.getVisibility(prefixedSchemaType);
                    if (visibilityConfig != null) {
                        if (visibilityConfig.isNotDisplayedBySystem()) {
                            responseBuilder.addSchemaTypeNotDisplayedBySystem(typeName);
                        }
                        List<PackageIdentifier> packageIdentifiers =
                                visibilityConfig.getVisibilityConfig().getAllowedPackages();
                        if (!packageIdentifiers.isEmpty()) {
                            responseBuilder.setSchemaTypeVisibleToPackages(typeName,
                                    new ArraySet<>(packageIdentifiers));
                        }
                        Set<Set<Integer>> visibleToPermissions =
                                visibilityConfig.getVisibilityConfig().getRequiredPermissions();
                        if (!visibleToPermissions.isEmpty()) {
                            Set<Set<Integer>> visibleToPermissionsSet =
                                    new ArraySet<>(visibleToPermissions.size());
                            for (Set<Integer> permissionList : visibleToPermissions) {
                                visibleToPermissionsSet.add(new ArraySet<>(permissionList));
                            }

                            responseBuilder.setRequiredPermissionsForSchemaTypeVisibility(typeName,
                                    visibleToPermissionsSet);
                        }

                        // Check for Visibility properties from the overlay
                        PackageIdentifier publiclyVisibleFromPackage =
                                visibilityConfig.getVisibilityConfig()
                                        .getPubliclyVisibleTargetPackage();
                        if (publiclyVisibleFromPackage != null) {
                            responseBuilder.setPubliclyVisibleSchema(
                                    typeName, publiclyVisibleFromPackage);
                        }
                        Set<SchemaVisibilityConfig> visibleToConfigs =
                                visibilityConfig.getVisibleToConfigs();
                        if (!visibleToConfigs.isEmpty()) {
                            responseBuilder.setSchemaTypeVisibleToConfigs(
                                    typeName, visibleToConfigs);
                        }
                    }
                }
            }
            return responseBuilder.build();

        } finally {
            mReadWriteLock.readLock().unlock();
        }
    }

    /**
     * Retrieves the list of namespaces with at least one document for this package name, database.
     *
     * <p>This method belongs to query group.
     *
     * @param packageName  Package name that owns this schema
     * @param databaseName The name of the database where this schema lives.
     * @throws AppSearchException on IcingSearchEngine error.
     */
    public @NonNull List<String> getNamespaces(
            @NonNull String packageName, @NonNull String databaseName) throws AppSearchException {
        mReadWriteLock.readLock().lock();
        try {
            throwIfClosedLocked();
            LogUtil.piiTrace(TAG, "getAllNamespaces, request");
            // We can't just use mNamespaceMap here because we have no way to prune namespaces from
            // mNamespaceMap when they have no more documents (e.g. after setting schema to empty or
            // using deleteByQuery).
            GetAllNamespacesResultProto getAllNamespacesResultProto =
                    mIcingSearchEngineLocked.getAllNamespaces();
            LogUtil.piiTrace(
                    TAG,
                    "getAllNamespaces, response",
                    getAllNamespacesResultProto.getNamespacesCount(),
                    getAllNamespacesResultProto);
            checkSuccess(getAllNamespacesResultProto.getStatus());
            String prefix = createPrefix(packageName, databaseName);
            List<String> results = new ArrayList<>();
            for (int i = 0; i < getAllNamespacesResultProto.getNamespacesCount(); i++) {
                String prefixedNamespace = getAllNamespacesResultProto.getNamespaces(i);
                if (prefixedNamespace.startsWith(prefix)) {
                    results.add(prefixedNamespace.substring(prefix.length()));
                }
            }
            return results;
        } finally {
            mReadWriteLock.readLock().unlock();
        }
    }

    /**
     * Adds a document to the AppSearch index.
     *
     * <p>This method belongs to mutate group.
     *
     * @param packageName             The package name that owns this document.
     * @param databaseName            The databaseName this document resides in.
     * @param document                The document to index.
     * @param sendChangeNotifications Whether to dispatch
     *                                {@link androidx.appsearch.observer.DocumentChangeInfo}
     *                                messages to observers for this change.
     * @throws AppSearchException on IcingSearchEngine error.
     */
    public void putDocument(
            @NonNull String packageName,
            @NonNull String databaseName,
            @NonNull GenericDocument document,
            boolean sendChangeNotifications,
            @Nullable AppSearchLogger logger)
            throws AppSearchException {
        PutDocumentStats.Builder pStatsBuilder = null;
        if (logger != null) {
            pStatsBuilder = new PutDocumentStats.Builder(packageName, databaseName);
        }
        long totalStartTimeMillis = SystemClock.elapsedRealtime();

        mReadWriteLock.writeLock().lock();
        try {
            throwIfClosedLocked();

            // Generate Document Proto
            long generateDocumentProtoStartTimeMillis = SystemClock.elapsedRealtime();
            DocumentProto.Builder documentBuilder = GenericDocumentToProtoConverter.toDocumentProto(
                    document).toBuilder();
            long generateDocumentProtoEndTimeMillis = SystemClock.elapsedRealtime();

            // Rewrite Document Type
            long rewriteDocumentTypeStartTimeMillis = SystemClock.elapsedRealtime();
            String prefix = createPrefix(packageName, databaseName);
            addPrefixToDocument(documentBuilder, prefix);
            long rewriteDocumentTypeEndTimeMillis = SystemClock.elapsedRealtime();
            DocumentProto finalDocument = documentBuilder.build();

            // Check limits
            enforceLimitConfigLocked(
                    packageName, finalDocument.getUri(), finalDocument.getSerializedSize());

            // Insert document
            LogUtil.piiTrace(TAG, "putDocument, request", finalDocument.getUri(), finalDocument);
            PutResultProto putResultProto = mIcingSearchEngineLocked.put(finalDocument);
            LogUtil.piiTrace(
                    TAG, "putDocument, response", putResultProto.getStatus(), putResultProto);

            // Logging stats
            if (pStatsBuilder != null) {
                pStatsBuilder
                        .setStatusCode(statusProtoToResultCode(putResultProto.getStatus()))
                        .setGenerateDocumentProtoLatencyMillis(
                                (int) (generateDocumentProtoEndTimeMillis
                                        - generateDocumentProtoStartTimeMillis))
                        .setRewriteDocumentTypesLatencyMillis(
                                (int) (rewriteDocumentTypeEndTimeMillis
                                        - rewriteDocumentTypeStartTimeMillis));
                AppSearchLoggerHelper.copyNativeStats(putResultProto.getPutDocumentStats(),
                        pStatsBuilder);
            }

            checkSuccess(putResultProto.getStatus());

            // Only update caches if the document is successfully put to Icing.

            mNamespaceCacheLocked.addToDocumentNamespaceMap(prefix, finalDocument.getNamespace());
            if (!Flags.enableDocumentLimiterReplaceTracking()
                    || !putResultProto.getWasReplacement()) {
                // If the document was a replacement, then there is no need to report it because the
                // number of documents has not changed. We only need to report "true" additions to
                // the DocumentLimiter.
                // Although a replacement document will consume a document id, the limit is only
                // intended to apply to "living" documents. It is the responsibility of AppSearch's
                // optimization task to reclaim space when needed.
                mDocumentLimiterLocked.reportDocumentAdded(
                        packageName,
                        () -> getRawStorageInfoProto().getDocumentStorageInfo()
                                .getNamespaceStorageInfoList());
            }

            // Prepare notifications
            if (sendChangeNotifications) {
                mObserverManager.onDocumentChange(
                        packageName,
                        databaseName,
                        document.getNamespace(),
                        document.getSchemaType(),
                        document.getId(),
                        mDocumentVisibilityStoreLocked,
                        mVisibilityCheckerLocked);
            }
        } finally {
            mReadWriteLock.writeLock().unlock();

            if (pStatsBuilder != null && logger != null) {
                long totalEndTimeMillis = SystemClock.elapsedRealtime();
                pStatsBuilder.setTotalLatencyMillis(
                        (int) (totalEndTimeMillis - totalStartTimeMillis));
                logger.logStats(pStatsBuilder.build());
            }
        }
    }

    /**
     * Gets the {@link ParcelFileDescriptor} for write purpose of the given
     * {@link AppSearchBlobHandle}.
     *
     * <p> Only one opened {@link ParcelFileDescriptor} is allowed for each
     * {@link AppSearchBlobHandle}. The same {@link ParcelFileDescriptor} will be returned if it is
     * not closed by caller.
     *
     * @param packageName    The package name that owns this blob.
     * @param databaseName   The databaseName this blob resides in.
     * @param handle         The {@link AppSearchBlobHandle} represent the blob.
     */
    @ExperimentalAppSearchApi
    public @NonNull ParcelFileDescriptor openWriteBlob(
            @NonNull String packageName,
            @NonNull String databaseName,
            @NonNull AppSearchBlobHandle handle)
            throws AppSearchException, IOException {
        if (mRevocableFileDescriptorStore == null) {
            throw new UnsupportedOperationException(
                    "BLOB_STORAGE is not available on this AppSearch implementation.");
        }
        mReadWriteLock.writeLock().lock();
        try {
            throwIfClosedLocked();
            verifyCallingBlobHandle(packageName, databaseName, handle);
            ParcelFileDescriptor pfd = mRevocableFileDescriptorStore
                    .getOpenedRevocableFileDescriptorForWrite(packageName, handle);
            if (pfd != null) {
                // There is already an opened pfd for write with same blob handle, just return the
                // already opened one.
                return pfd;
            }
            mRevocableFileDescriptorStore.checkBlobStoreLimit(packageName);
            PropertyProto.BlobHandleProto blobHandleProto =
                    BlobHandleToProtoConverter.toBlobHandleProto(handle);
            BlobProto result = mIcingSearchEngineLocked.openWriteBlob(blobHandleProto);
            pfd = retrieveFileDescriptorLocked(result,
                    ParcelFileDescriptor.MODE_CREATE | ParcelFileDescriptor.MODE_READ_WRITE);
            mNamespaceCacheLocked.addToBlobNamespaceMap(createPrefix(packageName, databaseName),
                    blobHandleProto.getNamespace());

            return mRevocableFileDescriptorStore.wrapToRevocableFileDescriptor(
                    packageName, handle, pfd, ParcelFileDescriptor.MODE_READ_WRITE);
        } finally {
            mReadWriteLock.writeLock().unlock();
        }
    }

    /**
     * Remove and delete the blob file of given {@link AppSearchBlobHandle} from AppSearch
     * storage.
     *
     * <p> This method will delete pending blob or committed blobs. Remove blobs that have reference
     * documents linked to it will make those reference document has nothing to read.
     *
     * @param packageName    The package name that owns this blob.
     * @param databaseName   The databaseName this blob resides in.
     * @param handle         The {@link AppSearchBlobHandle} represent the blob.
     */
    @ExperimentalAppSearchApi
    public void removeBlob(
            @NonNull String packageName,
            @NonNull String databaseName,
            @NonNull AppSearchBlobHandle handle)
            throws AppSearchException, IOException {
        if (mRevocableFileDescriptorStore == null) {
            throw new UnsupportedOperationException(
                    "BLOB_STORAGE is not available on this AppSearch implementation.");
        }
        mReadWriteLock.writeLock().lock();
        try {
            throwIfClosedLocked();
            verifyCallingBlobHandle(packageName, databaseName, handle);

            BlobProto result = mIcingSearchEngineLocked.removeBlob(
                    BlobHandleToProtoConverter.toBlobHandleProto(handle));

            checkSuccess(result.getStatus());
            if (Flags.enableAppSearchManageBlobFiles()) {
                File blobFileToRemove = new File(mBlobFilesDir, result.getFileName());
                if (!blobFileToRemove.delete()) {
                    throw new AppSearchException(AppSearchResult.RESULT_IO_ERROR,
                            "Cannot delete the blob file: " + blobFileToRemove.getName());
                }
            }
            mRevocableFileDescriptorStore.revokeFdForWrite(packageName, handle);
        } finally {
            mReadWriteLock.writeLock().unlock();
        }
    }

    /**
     * Verifies the integrity of a blob file by comparing its SHA-256 digest with the expected
     * digest.
     *
     * <p>This method is used when AppSearch manages blob files directly. It opens the blob file
     * associated with the given {@link AppSearchBlobHandle}, calculates its SHA-256 digest, and
     * compares it with the digest provided in the handle. If the file does not exist or the
     * calculated digest does not match the expected digest, the blob is considered invalid and is
     * removed.
     *
     * @param handle The {@link AppSearchBlobHandle} representing the blob to verify.
     * @throws AppSearchException if the blob file does not exist, the calculated digest does not
     *                            match the expected digest, or if there is an error removing the
     *                            invalid blob.
     * @throws IOException        if there is an error opening or reading the blob file.
     */
    @GuardedBy("mReadWriteLock")
    @OptIn(markerClass = ExperimentalAppSearchApi.class)
    private void verifyBlobIntegrityLocked(@NonNull AppSearchBlobHandle handle)
            throws AppSearchException, IOException {
        // Since the blob has not yet been committed, we open the blob for *write* again to
        // get the file name.
        BlobProto result = mIcingSearchEngineLocked.openWriteBlob(
                BlobHandleToProtoConverter.toBlobHandleProto(handle));
        checkSuccess(result.getStatus());
        File blobFile = new File(mBlobFilesDir, result.getFileName());
        boolean fileExists = blobFile.exists();
        boolean digestMatches = false;

        if (fileExists) {
            // Read the file to check the digest.
            byte[] digest;
            ParcelFileDescriptor pfd = ParcelFileDescriptor.open(blobFile,
                    ParcelFileDescriptor.MODE_READ_ONLY);
            try (InputStream inputStream =
                         new ParcelFileDescriptor.AutoCloseInputStream(pfd);
                 DigestInputStream digestInputStream =
                         new DigestInputStream(inputStream, MessageDigest.getInstance("SHA-256"))) {
                byte[] buffer = new byte[8192];
                while (digestInputStream.read(buffer) != -1) ;
                digest = digestInputStream.getMessageDigest().digest();
            } catch (NoSuchAlgorithmException e) {
                throw new AppSearchException(AppSearchResult.RESULT_INTERNAL_ERROR,
                        "Failed to get MessageDigest for SHA-256.", e);
            }
            digestMatches = Arrays.equals(digest, handle.getSha256Digest());
        }

        // If the file does not exist or the digest is wrong, delete the blob and throw
        // an exception.
        if (!fileExists || !digestMatches) {
            BlobProto removeResult = mIcingSearchEngineLocked.removeBlob(
                    BlobHandleToProtoConverter.toBlobHandleProto(handle));
            checkSuccess(removeResult.getStatus());

            if (!fileExists) {
                throw new AppSearchException(AppSearchResult.RESULT_NOT_FOUND,
                        "Cannot find the blob for handle: " + handle);
            } else {
                File blobFileToRemove = new File(mBlobFilesDir, removeResult.getFileName());
                if (!blobFileToRemove.delete()) {
                    throw new AppSearchException(AppSearchResult.RESULT_IO_ERROR,
                            "Cannot delete the blob file: " + blobFileToRemove.getName());
                }
                throw new AppSearchException(AppSearchResult.RESULT_INVALID_ARGUMENT,
                        "The blob content doesn't match to the digest.");
            }
        }
    }

    /**
     * Commits and seals the blob represented by the given {@link AppSearchBlobHandle}.
     *
     * <p>After this call, the blob is readable via {@link #openReadBlob}. And any rewrite is not
     * allowed.
     *
     * @param packageName    The package name that owns this blob.
     * @param databaseName   The databaseName this blob resides in.
     * @param handle         The {@link AppSearchBlobHandle} represent the blob.
     */
    @ExperimentalAppSearchApi
    public void commitBlob(
            @NonNull String packageName,
            @NonNull String databaseName,
            @NonNull AppSearchBlobHandle handle)
            throws AppSearchException, IOException {
        if (mRevocableFileDescriptorStore == null) {
            throw new UnsupportedOperationException(
                    "BLOB_STORAGE is not available on this AppSearch implementation.");
        }
        mReadWriteLock.writeLock().lock();
        try {
            throwIfClosedLocked();
            verifyCallingBlobHandle(packageName, databaseName, handle);

            // If AppSearch manages blob files, it is responsible for verifying the digest of the
            // blob file.
            if (Flags.enableAppSearchManageBlobFiles()) {
                verifyBlobIntegrityLocked(handle);
            }

            BlobProto result = mIcingSearchEngineLocked.commitBlob(
                    BlobHandleToProtoConverter.toBlobHandleProto(handle));

            checkSuccess(result.getStatus());
            // The blob is committed and sealed, revoke the sent pfd for writing.
            mRevocableFileDescriptorStore.revokeFdForWrite(packageName, handle);
        } finally {
            mReadWriteLock.writeLock().unlock();
        }
    }

    /**
     * Gets the {@link ParcelFileDescriptor} for read only purpose of the given
     * {@link AppSearchBlobHandle}.
     *
     * <p>The target must be committed via {@link #commitBlob};
     *
     * @param packageName    The package name that owns this blob.
     * @param databaseName   The databaseName this blob resides in.
     * @param handle         The {@link AppSearchBlobHandle} represent the blob.
     */
    @ExperimentalAppSearchApi
    public @NonNull ParcelFileDescriptor openReadBlob(
            @NonNull String packageName,
            @NonNull String databaseName,
            @NonNull AppSearchBlobHandle handle)
            throws AppSearchException, IOException {
        if (mRevocableFileDescriptorStore == null) {
            throw new UnsupportedOperationException(
                    "BLOB_STORAGE is not available on this AppSearch implementation.");
        }

        mReadWriteLock.readLock().lock();
        try {
            throwIfClosedLocked();
            verifyCallingBlobHandle(packageName, databaseName, handle);
            mRevocableFileDescriptorStore.checkBlobStoreLimit(packageName);
            BlobProto result = mIcingSearchEngineLocked.openReadBlob(
                    BlobHandleToProtoConverter.toBlobHandleProto(handle));
            ParcelFileDescriptor pfd = retrieveFileDescriptorLocked(result,
                    ParcelFileDescriptor.MODE_READ_ONLY);

            // We do NOT need to look up the revocable file descriptor for read, skip passing the
            // blob handle key.
            return mRevocableFileDescriptorStore.wrapToRevocableFileDescriptor(
                    packageName, /*blobHandle=*/null, pfd, ParcelFileDescriptor.MODE_READ_ONLY);
        } finally {
            mReadWriteLock.readLock().unlock();
        }
    }

    /**
     * Gets the {@link ParcelFileDescriptor} for read only purpose of the given
     * {@link AppSearchBlobHandle}.
     *
     * <p>The target must be committed via {@link #commitBlob};
     *
     * @param handle         The {@link AppSearchBlobHandle} represent the blob.
     */
    @ExperimentalAppSearchApi
    public @NonNull ParcelFileDescriptor globalOpenReadBlob(@NonNull AppSearchBlobHandle handle,
            @NonNull CallerAccess access)
            throws AppSearchException, IOException {
        if (mRevocableFileDescriptorStore == null) {
            throw new UnsupportedOperationException(
                    "BLOB_STORAGE is not available on this AppSearch implementation.");
        }

        mReadWriteLock.readLock().lock();
        try {
            throwIfClosedLocked();
            mRevocableFileDescriptorStore.checkBlobStoreLimit(access.getCallingPackageName());
            String prefixedNamespace =
                    createPrefix(handle.getPackageName(), handle.getDatabaseName())
                            + handle.getNamespace();
            PropertyProto.BlobHandleProto blobHandleProto =
                    BlobHandleToProtoConverter.toBlobHandleProto(handle);
            // We are using namespace to check blob's visibility.
            if (!VisibilityUtil.isSchemaSearchableByCaller(
                    access,
                    handle.getPackageName(),
                    prefixedNamespace,
                    mBlobVisibilityStoreLocked,
                    mVisibilityCheckerLocked)) {
                // Caller doesn't have access to this namespace.
                throw new AppSearchException(AppSearchResult.RESULT_NOT_FOUND,
                        "Cannot find the blob for handle: "
                                + blobHandleProto.getDigest().toStringUtf8());
            }

            BlobProto result = mIcingSearchEngineLocked.openReadBlob(blobHandleProto);
            ParcelFileDescriptor pfd = retrieveFileDescriptorLocked(result,
                    ParcelFileDescriptor.MODE_READ_ONLY);

            // We do NOT need to look up the revocable file descriptor for read, skip passing the
            // blob handle key.
            return mRevocableFileDescriptorStore.wrapToRevocableFileDescriptor(
                    access.getCallingPackageName(),
                    /*blobHandle=*/null,
                    pfd,
                    ParcelFileDescriptor.MODE_READ_ONLY);
        } finally {
            mReadWriteLock.readLock().unlock();
        }
    }

    /**
     * Updates the visibility configuration for a specified namespace within a blob storage.
     *
     * <p>This method configures the visibility blob namespaces in the given specific database.
     *
     * <p>After applying the new visibility configurations, the method identifies and removes any
     * existing visibility settings that do not included in the new visibility configurations from
     * the visibility store.
     *
     * @param packageName       The package name that owns these blobs.
     * @param databaseName      The databaseName these blobs resides in.
     * @param visibilityConfigs a list of {@link InternalVisibilityConfig} objects representing the
     *                          visibility configurations to be set for the specified namespace.
     * @throws AppSearchException if an error occurs while updating the visibility configurations.
     *                            This could happen if the database is closed or in an invalid
     *                            state.
     */
    @ExperimentalAppSearchApi
    public void setBlobNamespaceVisibility(
            @NonNull String packageName,
            @NonNull String databaseName,
            @NonNull List<InternalVisibilityConfig> visibilityConfigs) throws AppSearchException {
        mReadWriteLock.writeLock().lock();
        try {
            throwIfClosedLocked();
            if (mBlobVisibilityStoreLocked != null) {
                String prefix = PrefixUtil.createPrefix(packageName, databaseName);
                Set<String> removedVisibilityConfigs =
                        mNamespaceCacheLocked.getPrefixedBlobNamespaces(prefix);
                if (removedVisibilityConfigs == null) {
                    removedVisibilityConfigs = new ArraySet<>();
                } else {
                    // wrap it to allow rewriteVisibilityConfigs modify it.
                    removedVisibilityConfigs = new ArraySet<>(removedVisibilityConfigs);
                }
                List<InternalVisibilityConfig> prefixedVisibilityConfigs = rewriteVisibilityConfigs(
                        prefix, visibilityConfigs, removedVisibilityConfigs);
                for (int i = 0; i < prefixedVisibilityConfigs.size(); i++) {
                    // We are using schema type to represent blob's namespace in
                    // InternalVisibilityConfig.
                    mNamespaceCacheLocked.addToBlobNamespaceMap(prefix,
                            prefixedVisibilityConfigs.get(i).getSchemaType());
                }
                // Now removedVisibilityConfigs contains those existing schemas that has
                // all-default visibility settings, add deleted schemas. That's all we need to
                // remove.
                mBlobVisibilityStoreLocked.setVisibility(prefixedVisibilityConfigs);
                mBlobVisibilityStoreLocked.removeVisibility(removedVisibilityConfigs);
            } else {
                throw new UnsupportedOperationException(
                        "BLOB_STORAGE is not available on this AppSearch implementation.");
            }
        } finally {
            mReadWriteLock.writeLock().unlock();
        }
    }

    /**
     * Retrieves the {@link ParcelFileDescriptor} from a {@link BlobProto}.
     *
     * <p>This method handles retrieving the actual file descriptor from the provided
     * {@link BlobProto}, taking into account whether AppSearch manages blob files directly.
     * If AppSearch manages blob files ({@code Flags.enableAppSearchManageBlobFiles()} is true),
     * it opens the file using the file name from the {@link BlobProto}. Otherwise, it retrieves
     * the file descriptor directly from the {@link BlobProto}.
     *
     * @return The {@link ParcelFileDescriptor} for the blob.
     * @throws AppSearchException if the {@link BlobProto}'s status indicates an error.
     * @throws IOException        if there is an error opening the file, such as the file not
     *                            being found.
     */
    @GuardedBy("mReadWriteLock")
    private ParcelFileDescriptor retrieveFileDescriptorLocked(
            BlobProto blobProto, int mode) throws AppSearchException, IOException {
        checkSuccess(blobProto.getStatus());
        if (Flags.enableAppSearchManageBlobFiles()) {
            File blobFile = new File(mBlobFilesDir, blobProto.getFileName());
            return ParcelFileDescriptor.open(blobFile, mode);
        } else {
            return ParcelFileDescriptor.fromFd(blobProto.getFileDescriptor());
        }
    }

    /**
     * Checks that a new document can be added to the given packageName with the given serialized
     * size without violating our {@link LimitConfig}.
     *
     * @throws AppSearchException with a code of {@link AppSearchResult#RESULT_OUT_OF_SPACE} if the
     *                            limits are violated by the new document.
     */
    @GuardedBy("mReadWriteLock")
    private void enforceLimitConfigLocked(String packageName, String newDocUri, int newDocSize)
            throws AppSearchException {
        // Limits check: size of document
        if (newDocSize > mConfig.getMaxDocumentSizeBytes()) {
            throw new AppSearchException(
                    AppSearchResult.RESULT_OUT_OF_SPACE,
                    "Document \"" + newDocUri + "\" for package \"" + packageName
                            + "\" serialized to " + newDocSize + " bytes, which exceeds "
                            + "limit of " + mConfig.getMaxDocumentSizeBytes() + " bytes");
        }

        mDocumentLimiterLocked.enforceDocumentCountLimit(
                packageName,
                () -> getRawStorageInfoProto().getDocumentStorageInfo()
                        .getNamespaceStorageInfoList());
    }

    /**
     * Retrieves a document from the AppSearch index by namespace and document ID from any
     * application the caller is allowed to view
     *
     * <p>This method will handle both Icing engine errors as well as permission errors by
     * throwing an obfuscated RESULT_NOT_FOUND exception. This is done so the caller doesn't
     * receive information on whether or not a file they are not allowed to access exists or not.
     * This is different from the behavior of {@link #getDocument}.
     *
     * @param packageName       The package that owns this document.
     * @param databaseName      The databaseName this document resides in.
     * @param namespace         The namespace this document resides in.
     * @param id                The ID of the document to get.
     * @param typePropertyPaths A map of schema type to a list of property paths to return in the
     *                          result.
     * @param callerAccess      Visibility access info of the calling app
     * @return The Document contents
     * @throws AppSearchException on IcingSearchEngine error or invalid permissions
     */
    public @NonNull GenericDocument globalGetDocument(
            @NonNull String packageName,
            @NonNull String databaseName,
            @NonNull String namespace,
            @NonNull String id,
            @NonNull Map<String, List<String>> typePropertyPaths,
            @NonNull CallerAccess callerAccess) throws AppSearchException {
        mReadWriteLock.readLock().lock();
        try {
            throwIfClosedLocked();
            // We retrieve the document before checking for access, as we do not know which
            // schema the document is under. Schema is required for checking access
            DocumentProto documentProto;
            try {
                documentProto = getDocumentProtoByIdLocked(packageName, databaseName,
                        namespace, id, typePropertyPaths);

                if (!VisibilityUtil.isSchemaSearchableByCaller(
                        callerAccess,
                        packageName,
                        documentProto.getSchema(),
                        mDocumentVisibilityStoreLocked,
                        mVisibilityCheckerLocked)) {
                    throw new AppSearchException(AppSearchResult.RESULT_NOT_FOUND);
                }
            } catch (AppSearchException e) {
                // Not passing cause in AppSearchException as that violates privacy guarantees as
                // user could differentiate between document not existing and not having access.
                throw new AppSearchException(AppSearchResult.RESULT_NOT_FOUND,
                        "Document (" + namespace + ", " + id + ") not found.");
            }

            DocumentProto.Builder documentBuilder = documentProto.toBuilder();
            removePrefixesFromDocument(documentBuilder);
            String prefix = createPrefix(packageName, databaseName);
            return GenericDocumentToProtoConverter.toGenericDocument(documentBuilder.build(),
                    prefix, mSchemaCacheLocked, mConfig);
        } finally {
            mReadWriteLock.readLock().unlock();
        }
    }

    /**
     * Retrieves a document from the AppSearch index by namespace and document ID.
     *
     * <p>This method belongs to query group.
     *
     * @param packageName       The package that owns this document.
     * @param databaseName      The databaseName this document resides in.
     * @param namespace         The namespace this document resides in.
     * @param id                The ID of the document to get.
     * @param typePropertyPaths A map of schema type to a list of property paths to return in the
     *                          result.
     * @return The Document contents
     * @throws AppSearchException on IcingSearchEngine error.
     */
    public @NonNull GenericDocument getDocument(
            @NonNull String packageName,
            @NonNull String databaseName,
            @NonNull String namespace,
            @NonNull String id,
            @NonNull Map<String, List<String>> typePropertyPaths) throws AppSearchException {
        mReadWriteLock.readLock().lock();
        try {
            throwIfClosedLocked();
            DocumentProto documentProto = getDocumentProtoByIdLocked(packageName, databaseName,
                    namespace, id, typePropertyPaths);
            DocumentProto.Builder documentBuilder = documentProto.toBuilder();
            removePrefixesFromDocument(documentBuilder);

            String prefix = createPrefix(packageName, databaseName);
            // The schema type map cannot be null at this point. It could only be null if no
            // schema had ever been set for that prefix. Given we have retrieved a document from
            // the index, we know a schema had to have been set.
            return GenericDocumentToProtoConverter.toGenericDocument(documentBuilder.build(),
                    prefix, mSchemaCacheLocked, mConfig);
        } finally {
            mReadWriteLock.readLock().unlock();
        }
    }

    /**
     * Returns a DocumentProto from Icing.
     *
     * @param packageName       The package that owns this document.
     * @param databaseName      The databaseName this document resides in.
     * @param namespace         The namespace this document resides in.
     * @param id                The ID of the document to get.
     * @param typePropertyPaths A map of schema type to a list of property paths to return in the
     *                          result.
     * @return the DocumentProto object
     * @throws AppSearchException on IcingSearchEngine error
     */
    @GuardedBy("mReadWriteLock")
    // We only log getResultProto.toString() in fullPii trace for debugging.
    @SuppressWarnings("LiteProtoToString")
    private @NonNull DocumentProto getDocumentProtoByIdLocked(
            @NonNull String packageName,
            @NonNull String databaseName,
            @NonNull String namespace,
            @NonNull String id,
            @NonNull Map<String, List<String>> typePropertyPaths)
            throws AppSearchException {
        String prefix = createPrefix(packageName, databaseName);
        List<TypePropertyMask.Builder> nonPrefixedPropertyMaskBuilders =
                TypePropertyPathToProtoConverter
                        .toTypePropertyMaskBuilderList(typePropertyPaths);
        List<TypePropertyMask> prefixedPropertyMasks =
                new ArrayList<>(nonPrefixedPropertyMaskBuilders.size());
        for (int i = 0; i < nonPrefixedPropertyMaskBuilders.size(); ++i) {
            String nonPrefixedType = nonPrefixedPropertyMaskBuilders.get(i).getSchemaType();
            String prefixedType = nonPrefixedType.equals(
                    GetByDocumentIdRequest.PROJECTION_SCHEMA_TYPE_WILDCARD)
                    ? nonPrefixedType : prefix + nonPrefixedType;
            prefixedPropertyMasks.add(
                    nonPrefixedPropertyMaskBuilders.get(i).setSchemaType(prefixedType).build());
        }
        GetResultSpecProto getResultSpec =
                GetResultSpecProto.newBuilder().addAllTypePropertyMasks(prefixedPropertyMasks)
                        .build();

        String finalNamespace = createPrefix(packageName, databaseName) + namespace;
        if (LogUtil.isPiiTraceEnabled()) {
            LogUtil.piiTrace(
                    TAG, "getDocument, request", finalNamespace + ", " + id + "," + getResultSpec);
        }
        GetResultProto getResultProto =
                mIcingSearchEngineLocked.get(finalNamespace, id, getResultSpec);
        LogUtil.piiTrace(TAG, "getDocument, response", getResultProto.getStatus(), getResultProto);
        checkSuccess(getResultProto.getStatus());

        return getResultProto.getDocument();
    }

    /**
     * Executes a query against the AppSearch index and returns results.
     *
     * <p>This method belongs to query group.
     *
     * @param packageName     The package name that is performing the query.
     * @param databaseName    The databaseName this query for.
     * @param queryExpression Query String to search.
     * @param searchSpec      Spec for setting filters, raw query etc.
     * @param logger          logger to collect query stats
     * @return The results of performing this search. It may contain an empty list of results if
     * no documents matched the query.
     * @throws AppSearchException on IcingSearchEngine error.
     */
    public @NonNull SearchResultPage query(
            @NonNull String packageName,
            @NonNull String databaseName,
            @NonNull String queryExpression,
            @NonNull SearchSpec searchSpec,
            @Nullable AppSearchLogger logger) throws AppSearchException {
        long totalLatencyStartMillis = SystemClock.elapsedRealtime();
        SearchStats.Builder sStatsBuilder = null;
        if (logger != null) {
            sStatsBuilder =
                    new SearchStats.Builder(SearchStats.VISIBILITY_SCOPE_LOCAL, packageName)
                            .setDatabase(databaseName)
                            .setSearchSourceLogTag(searchSpec.getSearchSourceLogTag());
        }

        long javaLockAcquisitionLatencyStartMillis = SystemClock.elapsedRealtime();
        mReadWriteLock.readLock().lock();
        try {
            if (sStatsBuilder != null) {
                sStatsBuilder.setJavaLockAcquisitionLatencyMillis(
                        (int) (SystemClock.elapsedRealtime()
                                - javaLockAcquisitionLatencyStartMillis));
            }
            throwIfClosedLocked();

            List<String> filterPackageNames = searchSpec.getFilterPackageNames();
            if (!filterPackageNames.isEmpty() && !filterPackageNames.contains(packageName)) {
                // Client wanted to query over some packages that weren't its own. This isn't
                // allowed through local query so we can return early with no results.
                if (sStatsBuilder != null && logger != null) {
                    sStatsBuilder.setStatusCode(AppSearchResult.RESULT_SECURITY_ERROR);
                }
                return new SearchResultPage();
            }

            String prefix = createPrefix(packageName, databaseName);
            SearchSpecToProtoConverter searchSpecToProtoConverter =
                    new SearchSpecToProtoConverter(queryExpression, searchSpec,
                            Collections.singleton(prefix), mNamespaceCacheLocked,
                            mSchemaCacheLocked, mConfig);
            if (searchSpecToProtoConverter.hasNothingToSearch()) {
                // there is nothing to search over given their search filters, so we can return an
                // empty SearchResult and skip sending request to Icing.
                return new SearchResultPage();
            }

            SearchResultPage searchResultPage =
                    doQueryLocked(
                            searchSpecToProtoConverter,
                            sStatsBuilder);
            addNextPageToken(packageName, searchResultPage.getNextPageToken());
            return searchResultPage;
        } finally {
            mReadWriteLock.readLock().unlock();
            if (sStatsBuilder != null && logger != null) {
                sStatsBuilder.setTotalLatencyMillis(
                        (int) (SystemClock.elapsedRealtime() - totalLatencyStartMillis));
                logger.logStats(sStatsBuilder.build());
            }
        }
    }

    /**
     * Executes a global query, i.e. over all permitted prefixes, against the AppSearch index and
     * returns results.
     *
     * <p>This method belongs to query group.
     *
     * @param queryExpression Query String to search.
     * @param searchSpec      Spec for setting filters, raw query etc.
     * @param callerAccess    Visibility access info of the calling app
     * @param logger          logger to collect globalQuery stats
     * @return The results of performing this search. It may contain an empty list of results if
     * no documents matched the query.
     * @throws AppSearchException on IcingSearchEngine error.
     */
    public @NonNull SearchResultPage globalQuery(
            @NonNull String queryExpression,
            @NonNull SearchSpec searchSpec,
            @NonNull CallerAccess callerAccess,
            @Nullable AppSearchLogger logger) throws AppSearchException {
        long totalLatencyStartMillis = SystemClock.elapsedRealtime();
        SearchStats.Builder sStatsBuilder = null;
        if (logger != null) {
            sStatsBuilder =
                    new SearchStats.Builder(
                            SearchStats.VISIBILITY_SCOPE_GLOBAL,
                            callerAccess.getCallingPackageName())
                            .setSearchSourceLogTag(searchSpec.getSearchSourceLogTag());
        }

        long javaLockAcquisitionLatencyStartMillis = SystemClock.elapsedRealtime();
        mReadWriteLock.readLock().lock();
        try {
            if (sStatsBuilder != null) {
                sStatsBuilder.setJavaLockAcquisitionLatencyMillis(
                        (int) (SystemClock.elapsedRealtime()
                                - javaLockAcquisitionLatencyStartMillis));
            }
            throwIfClosedLocked();

            long aclLatencyStartMillis = SystemClock.elapsedRealtime();

            // The two scenarios where we want to limit package filters are if the outer
            // SearchSpec has package filters and there is no JoinSpec, or if both outer and
            // nested SearchSpecs have package filters. If outer SearchSpec has no package
            // filters or the nested SearchSpec has no package filters, then we pass the key set of
            // documentNamespace map of mNamespaceCachedLocked to the SearchSpecToProtoConverter,
            // signifying that there is a SearchSpec that wants to query every visible package.
            Set<String> packageFilters = new ArraySet<>();
            if (!searchSpec.getFilterPackageNames().isEmpty()) {
                JoinSpec joinSpec = searchSpec.getJoinSpec();
                if (joinSpec == null) {
                    packageFilters.addAll(searchSpec.getFilterPackageNames());
                } else if (!joinSpec.getNestedSearchSpec()
                        .getFilterPackageNames().isEmpty()) {
                    packageFilters.addAll(searchSpec.getFilterPackageNames());
                    packageFilters.addAll(joinSpec.getNestedSearchSpec().getFilterPackageNames());
                }
            }

            // Convert package filters to prefix filters
            Set<String> prefixFilters = new ArraySet<>();
            if (packageFilters.isEmpty()) {
                // Client didn't restrict their search over packages. Try to query over all
                // packages/prefixes
                prefixFilters = mNamespaceCacheLocked.getAllDocumentPrefixes();
            } else {
                // Client did restrict their search over packages. Only include the prefixes that
                // belong to the specified packages.
                for (String prefix : mNamespaceCacheLocked.getAllDocumentPrefixes()) {
                    String packageName = getPackageName(prefix);
                    if (packageFilters.contains(packageName)) {
                        prefixFilters.add(prefix);
                    }
                }
            }
            SearchSpecToProtoConverter searchSpecToProtoConverter =
                    new SearchSpecToProtoConverter(queryExpression, searchSpec, prefixFilters,
                            mNamespaceCacheLocked, mSchemaCacheLocked, mConfig);
            // Remove those inaccessible schemas.
            searchSpecToProtoConverter.removeInaccessibleSchemaFilter(
                    callerAccess, mDocumentVisibilityStoreLocked, mVisibilityCheckerLocked);
            if (searchSpecToProtoConverter.hasNothingToSearch()) {
                // there is nothing to search over given their search filters, so we can return an
                // empty SearchResult and skip sending request to Icing.
                return new SearchResultPage();
            }
            if (sStatsBuilder != null) {
                sStatsBuilder.setAclCheckLatencyMillis(
                        (int) (SystemClock.elapsedRealtime() - aclLatencyStartMillis));
            }
            SearchResultPage searchResultPage =
                    doQueryLocked(
                            searchSpecToProtoConverter,
                            sStatsBuilder);
            addNextPageToken(
                    callerAccess.getCallingPackageName(), searchResultPage.getNextPageToken());
            return searchResultPage;
        } finally {
            mReadWriteLock.readLock().unlock();

            if (sStatsBuilder != null && logger != null) {
                sStatsBuilder.setTotalLatencyMillis(
                        (int) (SystemClock.elapsedRealtime() - totalLatencyStartMillis));
                logger.logStats(sStatsBuilder.build());
            }
        }
    }

    @GuardedBy("mReadWriteLock")
    private SearchResultPage doQueryLocked(
            @NonNull SearchSpecToProtoConverter searchSpecToProtoConverter,
            SearchStats.@Nullable Builder sStatsBuilder)
            throws AppSearchException {
        // Rewrite the given SearchSpec into SearchSpecProto, ResultSpecProto and ScoringSpecProto.
        // All processes are counted in rewriteSearchSpecLatencyMillis
        long rewriteSearchSpecLatencyStartMillis = SystemClock.elapsedRealtime();
        SearchSpecProto finalSearchSpec = searchSpecToProtoConverter.toSearchSpecProto();
        ResultSpecProto finalResultSpec = searchSpecToProtoConverter.toResultSpecProto(
                mNamespaceCacheLocked, mSchemaCacheLocked);
        ScoringSpecProto scoringSpec = searchSpecToProtoConverter.toScoringSpecProto();
        if (sStatsBuilder != null) {
            sStatsBuilder.setRewriteSearchSpecLatencyMillis((int)
                    (SystemClock.elapsedRealtime() - rewriteSearchSpecLatencyStartMillis));
        }

        // Send request to Icing.
        SearchResultProto searchResultProto = searchInIcingLocked(
                finalSearchSpec, finalResultSpec, scoringSpec, sStatsBuilder);

        long rewriteSearchResultLatencyStartMillis = SystemClock.elapsedRealtime();
        // Rewrite search result before we return.
        SearchResultPage searchResultPage = SearchResultToProtoConverter
                .toSearchResultPage(searchResultProto, mSchemaCacheLocked, mConfig);
        if (sStatsBuilder != null) {
            sStatsBuilder.setRewriteSearchResultLatencyMillis(
                    (int) (SystemClock.elapsedRealtime()
                            - rewriteSearchResultLatencyStartMillis));
        }
        return searchResultPage;
    }

    @GuardedBy("mReadWriteLock")
    // We only log searchSpec, scoringSpec and resultSpec in fullPii trace for debugging.
    @SuppressWarnings("LiteProtoToString")
    private SearchResultProto searchInIcingLocked(
            @NonNull SearchSpecProto searchSpec,
            @NonNull ResultSpecProto resultSpec,
            @NonNull ScoringSpecProto scoringSpec,
            SearchStats.@Nullable Builder sStatsBuilder) throws AppSearchException {
        if (LogUtil.isPiiTraceEnabled()) {
            LogUtil.piiTrace(
                    TAG,
                    "search, request",
                    searchSpec.getQuery(),
                    searchSpec + ", " + scoringSpec + ", " + resultSpec);
        }
        SearchResultProto searchResultProto = mIcingSearchEngineLocked.search(
                searchSpec, scoringSpec, resultSpec);
        LogUtil.piiTrace(
                TAG, "search, response", searchResultProto.getResultsCount(), searchResultProto);
        if (sStatsBuilder != null) {
            sStatsBuilder.setStatusCode(statusProtoToResultCode(searchResultProto.getStatus()));
            if (searchSpec.hasJoinSpec()) {
                sStatsBuilder.setJoinType(AppSearchSchema.StringPropertyConfig
                        .JOINABLE_VALUE_TYPE_QUALIFIED_ID);
            }
            AppSearchLoggerHelper.copyNativeStats(searchResultProto.getQueryStats(), sStatsBuilder);
        }
        checkSuccess(searchResultProto.getStatus());
        return searchResultProto;
    }

    /**
     * Generates suggestions based on the given search prefix.
     *
     * <p>This method belongs to query group.
     *
     * @param packageName               The package name that is performing the query.
     * @param databaseName              The databaseName this query for.
     * @param suggestionQueryExpression The non-empty query expression used to be completed.
     * @param searchSuggestionSpec      Spec for setting filters.
     * @return a List of {@link SearchSuggestionResult}. The returned {@link SearchSuggestionResult}
     *      are order by the number of {@link androidx.appsearch.app.SearchResult} you could get
     *      by using that suggestion in {@link #query}.
     * @throws AppSearchException if the suggestionQueryExpression is empty.
     */
    public @NonNull List<SearchSuggestionResult> searchSuggestion(
            @NonNull String packageName,
            @NonNull String databaseName,
            @NonNull String suggestionQueryExpression,
            @NonNull SearchSuggestionSpec searchSuggestionSpec) throws AppSearchException {
        mReadWriteLock.readLock().lock();
        try {
            throwIfClosedLocked();
            if (suggestionQueryExpression.isEmpty()) {
                throw new AppSearchException(
                        AppSearchResult.RESULT_INVALID_ARGUMENT,
                        "suggestionQueryExpression cannot be empty.");
            }
            if (searchSuggestionSpec.getMaximumResultCount()
                    > mConfig.getMaxSuggestionCount()) {
                throw new AppSearchException(
                        AppSearchResult.RESULT_INVALID_ARGUMENT,
                        "Trying to get " + searchSuggestionSpec.getMaximumResultCount()
                                + " suggestion results, which exceeds limit of "
                                + mConfig.getMaxSuggestionCount());
            }

            String prefix = createPrefix(packageName, databaseName);
            SearchSuggestionSpecToProtoConverter searchSuggestionSpecToProtoConverter =
                    new SearchSuggestionSpecToProtoConverter(suggestionQueryExpression,
                            searchSuggestionSpec,
                            Collections.singleton(prefix),
                            mNamespaceCacheLocked,
                            mSchemaCacheLocked);

            if (searchSuggestionSpecToProtoConverter.hasNothingToSearch()) {
                // there is nothing to search over given their search filters, so we can return an
                // empty SearchResult and skip sending request to Icing.
                return new ArrayList<>();
            }

            SuggestionResponse response = mIcingSearchEngineLocked.searchSuggestions(
                    searchSuggestionSpecToProtoConverter.toSearchSuggestionSpecProto());

            checkSuccess(response.getStatus());
            List<SearchSuggestionResult> suggestions =
                    new ArrayList<>(response.getSuggestionsCount());
            for (int i = 0; i < response.getSuggestionsCount(); i++) {
                suggestions.add(new SearchSuggestionResult.Builder()
                        .setSuggestedResult(response.getSuggestions(i).getQuery())
                        .build());
            }
            return suggestions;
        } finally {
            mReadWriteLock.readLock().unlock();
        }
    }

    /**
     * Returns a mapping of package names to all the databases owned by that package.
     *
     * <p>This method is inefficient to call repeatedly.
     */
    public @NonNull Map<String, Set<String>> getPackageToDatabases() {
        mReadWriteLock.readLock().lock();
        try {
            Map<String, Set<String>> packageToDatabases = new ArrayMap<>();
            for (String prefix : mSchemaCacheLocked.getAllPrefixes()) {
                String packageName = getPackageName(prefix);

                Set<String> databases = packageToDatabases.get(packageName);
                if (databases == null) {
                    databases = new ArraySet<>();
                    packageToDatabases.put(packageName, databases);
                }

                String databaseName = getDatabaseName(prefix);
                databases.add(databaseName);
            }

            return packageToDatabases;
        } finally {
            mReadWriteLock.readLock().unlock();
        }
    }

    /**
     * Fetches the next page of results of a previously executed query. Results can be empty if
     * next-page token is invalid or all pages have been returned.
     *
     * <p>This method belongs to query group.
     *
     * @param packageName   Package name of the caller.
     * @param nextPageToken The token of pre-loaded results of previously executed query.
     * @return The next page of results of previously executed query.
     * @throws AppSearchException on IcingSearchEngine error or if can't advance on nextPageToken.
     */
    public @NonNull SearchResultPage getNextPage(@NonNull String packageName, long nextPageToken,
            SearchStats.@Nullable Builder sStatsBuilder)
            throws AppSearchException {
        long totalLatencyStartMillis = SystemClock.elapsedRealtime();

        long javaLockAcquisitionLatencyStartMillis = SystemClock.elapsedRealtime();
        mReadWriteLock.readLock().lock();
        try {
            if (sStatsBuilder != null) {
                sStatsBuilder.setJavaLockAcquisitionLatencyMillis(
                        (int) (SystemClock.elapsedRealtime()
                                - javaLockAcquisitionLatencyStartMillis));
            }
            throwIfClosedLocked();

            LogUtil.piiTrace(TAG, "getNextPage, request", nextPageToken);
            checkNextPageToken(packageName, nextPageToken);
            SearchResultProto searchResultProto = mIcingSearchEngineLocked.getNextPage(
                    nextPageToken);

            if (sStatsBuilder != null) {
                sStatsBuilder.setStatusCode(statusProtoToResultCode(searchResultProto.getStatus()));
                // Join query stats are handled by SearchResultsImpl, which has access to the
                // original SearchSpec.
                AppSearchLoggerHelper.copyNativeStats(searchResultProto.getQueryStats(),
                        sStatsBuilder);
            }

            LogUtil.piiTrace(
                    TAG,
                    "getNextPage, response",
                    searchResultProto.getResultsCount(),
                    searchResultProto);
            checkSuccess(searchResultProto.getStatus());
            if (nextPageToken != EMPTY_PAGE_TOKEN
                    && searchResultProto.getNextPageToken() == EMPTY_PAGE_TOKEN) {
                // At this point, we're guaranteed that this nextPageToken exists for this package,
                // otherwise checkNextPageToken would've thrown an exception.
                // Since the new token is 0, this is the last page. We should remove the old token
                // from our cache since it no longer refers to this query.
                synchronized (mNextPageTokensLocked) {
                    Set<Long> nextPageTokensForPackage =
                            Preconditions.checkNotNull(mNextPageTokensLocked.get(packageName));
                    nextPageTokensForPackage.remove(nextPageToken);
                }
            }
            long rewriteSearchResultLatencyStartMillis = SystemClock.elapsedRealtime();
            // Rewrite search result before we return.
            SearchResultPage searchResultPage = SearchResultToProtoConverter
                    .toSearchResultPage(searchResultProto, mSchemaCacheLocked, mConfig);
            if (sStatsBuilder != null) {
                sStatsBuilder.setRewriteSearchResultLatencyMillis(
                        (int) (SystemClock.elapsedRealtime()
                                - rewriteSearchResultLatencyStartMillis));
            }
            return searchResultPage;
        } finally {
            mReadWriteLock.readLock().unlock();
            if (sStatsBuilder != null) {
                sStatsBuilder.setTotalLatencyMillis(
                        (int) (SystemClock.elapsedRealtime() - totalLatencyStartMillis));
            }
        }
    }

    /**
     * Invalidates the next-page token so that no more results of the related query can be returned.
     *
     * <p>This method belongs to query group.
     *
     * @param packageName   Package name of the caller.
     * @param nextPageToken The token of pre-loaded results of previously executed query to be
     *                      Invalidated.
     * @throws AppSearchException if nextPageToken is unusable.
     */
    public void invalidateNextPageToken(@NonNull String packageName, long nextPageToken)
            throws AppSearchException {
        if (nextPageToken == EMPTY_PAGE_TOKEN) {
            // (b/208305352) Directly return here since we are no longer caching EMPTY_PAGE_TOKEN
            // in the cached token set. So no need to remove it anymore.
            return;
        }

        mReadWriteLock.readLock().lock();
        try {
            throwIfClosedLocked();

            LogUtil.piiTrace(TAG, "invalidateNextPageToken, request", nextPageToken);
            checkNextPageToken(packageName, nextPageToken);
            mIcingSearchEngineLocked.invalidateNextPageToken(nextPageToken);

            synchronized (mNextPageTokensLocked) {
                Set<Long> tokens = mNextPageTokensLocked.get(packageName);
                if (tokens != null) {
                    tokens.remove(nextPageToken);
                } else {
                    Log.e(TAG, "Failed to invalidate token " + nextPageToken + ": tokens are not "
                            + "cached.");
                }
            }
        } finally {
            mReadWriteLock.readLock().unlock();
        }
    }

    /** Reports a usage of the given document at the given timestamp. */
    public void reportUsage(
            @NonNull String packageName,
            @NonNull String databaseName,
            @NonNull String namespace,
            @NonNull String documentId,
            long usageTimestampMillis,
            boolean systemUsage) throws AppSearchException {
        mReadWriteLock.writeLock().lock();
        try {
            throwIfClosedLocked();

            String prefixedNamespace = createPrefix(packageName, databaseName) + namespace;
            UsageReport.UsageType usageType = systemUsage
                    ? UsageReport.UsageType.USAGE_TYPE2 : UsageReport.UsageType.USAGE_TYPE1;
            UsageReport report = UsageReport.newBuilder()
                    .setDocumentNamespace(prefixedNamespace)
                    .setDocumentUri(documentId)
                    .setUsageTimestampMs(usageTimestampMillis)
                    .setUsageType(usageType)
                    .build();

            LogUtil.piiTrace(TAG, "reportUsage, request", report.getDocumentUri(), report);
            ReportUsageResultProto result = mIcingSearchEngineLocked.reportUsage(report);
            LogUtil.piiTrace(TAG, "reportUsage, response", result.getStatus(), result);
            checkSuccess(result.getStatus());
        } finally {
            mReadWriteLock.writeLock().unlock();
        }
    }

    /**
     * Removes the given document by id.
     *
     * <p>This method belongs to mutate group.
     *
     * @param packageName        The package name that owns the document.
     * @param databaseName       The databaseName the document is in.
     * @param namespace          Namespace of the document to remove.
     * @param documentId         ID of the document to remove.
     * @param removeStatsBuilder builder for {@link RemoveStats} to hold stats for remove
     * @throws AppSearchException on IcingSearchEngine error.
     */
    public void remove(
            @NonNull String packageName,
            @NonNull String databaseName,
            @NonNull String namespace,
            @NonNull String documentId,
            RemoveStats.@Nullable Builder removeStatsBuilder) throws AppSearchException {
        long totalLatencyStartTimeMillis = SystemClock.elapsedRealtime();
        mReadWriteLock.writeLock().lock();
        try {
            throwIfClosedLocked();

            String prefixedNamespace = createPrefix(packageName, databaseName) + namespace;
            String schemaType = null;
            if (mObserverManager.isPackageObserved(packageName)) {
                // Someone might be observing the type this document is under, but we have no way to
                // know its type without retrieving it. Do so now.
                // TODO(b/193494000): If Icing Lib can return information about the deleted
                //  document's type we can remove this code.
                if (LogUtil.isPiiTraceEnabled()) {
                    LogUtil.piiTrace(
                            TAG, "removeById, getRequest", prefixedNamespace + ", " + documentId);
                }
                GetResultProto getResult = mIcingSearchEngineLocked.get(
                        prefixedNamespace, documentId, GET_RESULT_SPEC_NO_PROPERTIES);
                LogUtil.piiTrace(TAG, "removeById, getResponse", getResult.getStatus(), getResult);
                checkSuccess(getResult.getStatus());
                schemaType = PrefixUtil.removePrefix(getResult.getDocument().getSchema());
            }

            if (LogUtil.isPiiTraceEnabled()) {
                LogUtil.piiTrace(TAG, "removeById, request", prefixedNamespace + ", " + documentId);
            }
            DeleteResultProto deleteResultProto =
                    mIcingSearchEngineLocked.delete(prefixedNamespace, documentId);
            LogUtil.piiTrace(
                    TAG, "removeById, response", deleteResultProto.getStatus(), deleteResultProto);

            if (removeStatsBuilder != null) {
                removeStatsBuilder.setStatusCode(statusProtoToResultCode(
                        deleteResultProto.getStatus()));
                AppSearchLoggerHelper.copyNativeStats(deleteResultProto.getDeleteStats(),
                        removeStatsBuilder);
            }
            checkSuccess(deleteResultProto.getStatus());

            // Update derived maps
            mDocumentLimiterLocked.reportDocumentsRemoved(packageName, /*numDocumentsDeleted=*/1);

            // Prepare notifications
            if (schemaType != null) {
                mObserverManager.onDocumentChange(
                        packageName,
                        databaseName,
                        namespace,
                        schemaType,
                        documentId,
                        mDocumentVisibilityStoreLocked,
                        mVisibilityCheckerLocked);
            }
        } finally {
            mReadWriteLock.writeLock().unlock();
            if (removeStatsBuilder != null) {
                removeStatsBuilder.setTotalLatencyMillis(
                        (int) (SystemClock.elapsedRealtime() - totalLatencyStartTimeMillis));
            }
        }
    }

    /**
     * Removes documents by given query.
     *
     * <p>This method belongs to mutate group.
     *
     * <p> {@link SearchSpec} objects containing a {@link JoinSpec} are not allowed here.
     *
     * @param packageName        The package name that owns the documents.
     * @param databaseName       The databaseName the document is in.
     * @param queryExpression    Query String to search.
     * @param searchSpec         Defines what and how to remove
     * @param removeStatsBuilder builder for {@link RemoveStats} to hold stats for remove
     * @throws AppSearchException on IcingSearchEngine error.
     * @throws IllegalArgumentException if the {@link SearchSpec} contains a {@link JoinSpec}.
     */
    public void removeByQuery(@NonNull String packageName, @NonNull String databaseName,
            @NonNull String queryExpression,
            @NonNull SearchSpec searchSpec,
            RemoveStats.@Nullable Builder removeStatsBuilder)
            throws AppSearchException {
        if (searchSpec.getJoinSpec() != null) {
            throw new IllegalArgumentException("JoinSpec not allowed in removeByQuery, but "
                    + "JoinSpec was provided");
        }

        long totalLatencyStartTimeMillis = SystemClock.elapsedRealtime();
        mReadWriteLock.writeLock().lock();
        try {
            throwIfClosedLocked();

            List<String> filterPackageNames = searchSpec.getFilterPackageNames();
            if (!filterPackageNames.isEmpty() && !filterPackageNames.contains(packageName)) {
                // We're only removing documents within the parameter `packageName`. If we're not
                // restricting our remove-query to this package name, then there's nothing for us to
                // remove.
                return;
            }

            String prefix = createPrefix(packageName, databaseName);
            if (!mNamespaceCacheLocked.getAllDocumentPrefixes().contains(prefix)) {
                // The target database is empty so we can return early and skip sending request to
                // Icing.
                return;
            }

            SearchSpecToProtoConverter searchSpecToProtoConverter =
                    new SearchSpecToProtoConverter(queryExpression, searchSpec,
                            Collections.singleton(prefix), mNamespaceCacheLocked,
                            mSchemaCacheLocked, mConfig);
            if (searchSpecToProtoConverter.hasNothingToSearch()) {
                // there is nothing to search over given their search filters, so we can return
                // early and skip sending request to Icing.
                return;
            }

            SearchSpecProto finalSearchSpec = searchSpecToProtoConverter.toSearchSpecProto();

            Set<String> prefixedObservedSchemas = null;
            if (mObserverManager.isPackageObserved(packageName)) {
                prefixedObservedSchemas = new ArraySet<>();
                List<String> prefixedTargetSchemaTypes =
                        finalSearchSpec.getSchemaTypeFiltersList();
                for (int i = 0; i < prefixedTargetSchemaTypes.size(); i++) {
                    String prefixedType = prefixedTargetSchemaTypes.get(i);
                    String shortTypeName = PrefixUtil.removePrefix(prefixedType);
                    if (mObserverManager.isSchemaTypeObserved(packageName, shortTypeName)) {
                        prefixedObservedSchemas.add(prefixedType);
                    }
                }
            }

            doRemoveByQueryLocked(
                    packageName, finalSearchSpec, prefixedObservedSchemas, removeStatsBuilder);

        } finally {
            mReadWriteLock.writeLock().unlock();
            if (removeStatsBuilder != null) {
                removeStatsBuilder.setTotalLatencyMillis(
                        (int) (SystemClock.elapsedRealtime() - totalLatencyStartTimeMillis));
            }
        }
    }

    /**
     * Executes removeByQuery.
     *
     * <p>Change notifications will be created if prefixedObservedSchemas is not null.
     *
     * @param packageName             The package name that owns the documents.
     * @param finalSearchSpec         The final search spec that has been written through
     *                                {@link SearchSpecToProtoConverter}.
     * @param prefixedObservedSchemas The set of prefixed schemas that have valid registered
     *                                observers. Only changes to schemas in this set will be queued.
     */
    @GuardedBy("mReadWriteLock")
    private void doRemoveByQueryLocked(
            @NonNull String packageName,
            @NonNull SearchSpecProto finalSearchSpec,
            @Nullable Set<String> prefixedObservedSchemas,
            RemoveStats.@Nullable Builder removeStatsBuilder) throws AppSearchException {
        LogUtil.piiTrace(TAG, "removeByQuery, request", finalSearchSpec);
        boolean returnDeletedDocumentInfo =
                prefixedObservedSchemas != null && !prefixedObservedSchemas.isEmpty();
        DeleteByQueryResultProto deleteResultProto =
                mIcingSearchEngineLocked.deleteByQuery(finalSearchSpec,
                        returnDeletedDocumentInfo);
        LogUtil.piiTrace(
                TAG, "removeByQuery, response", deleteResultProto.getStatus(), deleteResultProto);

        if (removeStatsBuilder != null) {
            removeStatsBuilder.setStatusCode(statusProtoToResultCode(
                    deleteResultProto.getStatus()));
            // TODO(b/187206766) also log query stats here once IcingLib returns it
            AppSearchLoggerHelper.copyNativeStats(deleteResultProto.getDeleteByQueryStats(),
                    removeStatsBuilder);
        }

        // It seems that the caller wants to get success if the data matching the query is
        // not in the DB because it was not there or was successfully deleted.
        checkCodeOneOf(deleteResultProto.getStatus(),
                StatusProto.Code.OK, StatusProto.Code.NOT_FOUND);

        // Update derived maps
        int numDocumentsDeleted =
                deleteResultProto.getDeleteByQueryStats().getNumDocumentsDeleted();
        mDocumentLimiterLocked.reportDocumentsRemoved(packageName, numDocumentsDeleted);

        if (prefixedObservedSchemas != null && !prefixedObservedSchemas.isEmpty()) {
            dispatchChangeNotificationsAfterRemoveByQueryLocked(packageName,
                    deleteResultProto, prefixedObservedSchemas);
        }
    }

    @GuardedBy("mReadWriteLock")
    private void dispatchChangeNotificationsAfterRemoveByQueryLocked(
            @NonNull String packageName,
            @NonNull DeleteByQueryResultProto deleteResultProto,
            @NonNull Set<String> prefixedObservedSchemas
    ) throws AppSearchException {
        for (int i = 0; i < deleteResultProto.getDeletedDocumentsCount(); ++i) {
            DeleteByQueryResultProto.DocumentGroupInfo group =
                    deleteResultProto.getDeletedDocuments(i);
            if (!prefixedObservedSchemas.contains(group.getSchema())) {
                continue;
            }
            String databaseName = PrefixUtil.getDatabaseName(group.getNamespace());
            String namespace = PrefixUtil.removePrefix(group.getNamespace());
            String schemaType = PrefixUtil.removePrefix(group.getSchema());
            for (int j = 0; j < group.getUrisCount(); ++j) {
                String uri = group.getUris(j);
                mObserverManager.onDocumentChange(
                        packageName,
                        databaseName,
                        namespace,
                        schemaType,
                        uri,
                        mDocumentVisibilityStoreLocked,
                        mVisibilityCheckerLocked);
            }
        }
    }

    /** Estimates the storage usage info for a specific package. */
    @ExperimentalAppSearchApi
    public @NonNull StorageInfo getStorageInfoForPackage(@NonNull String packageName)
            throws AppSearchException {
        mReadWriteLock.readLock().lock();
        try {
            throwIfClosedLocked();

            StorageInfo.Builder storageInfoBuilder = new StorageInfo.Builder();
            if (Flags.enableBlobStore()) {
                StorageInfoProto storageInfoProto = getRawStorageInfoProto();
                // read blob storage info and set to storageInfoBuilder
                getBlobStorageInfoForPrefix(storageInfoProto, packageName, storageInfoBuilder);
                // read document storage info and set to storageInfoBuilder
                Set<String> wantedPrefixedDocumentNamespaces = mNamespaceCacheLocked
                        .getAllPrefixedDocumentNamespaceForPackage(packageName);
                if (!wantedPrefixedDocumentNamespaces.isEmpty()) {
                    getDocumentStorageInfoForNamespaces(storageInfoProto,
                            wantedPrefixedDocumentNamespaces, storageInfoBuilder);
                }
            } else {
                // blob flag off, only read document storage info and set to storageInfoBuilder if
                // the database exists.
                Set<String> wantedPrefixedDocumentNamespaces = mNamespaceCacheLocked
                        .getAllPrefixedDocumentNamespaceForPackage(packageName);
                if (!wantedPrefixedDocumentNamespaces.isEmpty()) {
                    getDocumentStorageInfoForNamespaces(getRawStorageInfoProto(),
                            wantedPrefixedDocumentNamespaces, storageInfoBuilder);
                }
            }
            return storageInfoBuilder.build();
        } finally {
            mReadWriteLock.readLock().unlock();
        }
    }

    /** Estimates the storage usage info for a specific database in a package. */
    @ExperimentalAppSearchApi
    public @NonNull StorageInfo getStorageInfoForDatabase(@NonNull String packageName,
            @NonNull String databaseName)
            throws AppSearchException {
        mReadWriteLock.readLock().lock();
        try {
            throwIfClosedLocked();

            StorageInfo.Builder storageInfoBuilder = new StorageInfo.Builder();
            String prefix = createPrefix(packageName, databaseName);
            if (Flags.enableBlobStore()) {
                // read blob storage info and set to storageInfoBuilder
                StorageInfoProto storageInfoProto = getRawStorageInfoProto();
                getBlobStorageInfoForPrefix(storageInfoProto, prefix, storageInfoBuilder);
                // read document storage info and set to storageInfoBuilder
                Set<String> wantedPrefixedDocumentNamespaces =
                        mNamespaceCacheLocked.getPrefixedDocumentNamespaces(prefix);
                if (wantedPrefixedDocumentNamespaces == null
                        || wantedPrefixedDocumentNamespaces.isEmpty()) {
                    return storageInfoBuilder.build();
                }
                getDocumentStorageInfoForNamespaces(storageInfoProto,
                        wantedPrefixedDocumentNamespaces, storageInfoBuilder);
            } else {
                Map<String, Set<String>> packageToDatabases = getPackageToDatabases();
                Set<String> databases = packageToDatabases.get(packageName);
                if (databases == null) {
                    // Package doesn't exist, no storage info to report
                    return storageInfoBuilder.build();
                }
                if (!databases.contains(databaseName)) {
                    // Database doesn't exist, no storage info to report
                    return storageInfoBuilder.build();
                }

                Set<String> wantedPrefixedDocumentNamespaces =
                        mNamespaceCacheLocked.getPrefixedDocumentNamespaces(prefix);
                if (wantedPrefixedDocumentNamespaces == null
                        || wantedPrefixedDocumentNamespaces.isEmpty()) {
                    return storageInfoBuilder.build();
                }
                getDocumentStorageInfoForNamespaces(getRawStorageInfoProto(),
                        wantedPrefixedDocumentNamespaces, storageInfoBuilder);
            }
            return storageInfoBuilder.build();
        } finally {
            mReadWriteLock.readLock().unlock();
        }
    }

    /**
     * Returns the native storage info capsuled in {@link StorageInfoResultProto} directly from
     * IcingSearchEngine.
     */
    public @NonNull StorageInfoProto getRawStorageInfoProto() throws AppSearchException {
        mReadWriteLock.readLock().lock();
        try {
            throwIfClosedLocked();
            LogUtil.piiTrace(TAG, "getStorageInfo, request");
            StorageInfoResultProto storageInfoResult = mIcingSearchEngineLocked.getStorageInfo();
            LogUtil.piiTrace(
                    TAG,
                    "getStorageInfo, response", storageInfoResult.getStatus(), storageInfoResult);
            checkSuccess(storageInfoResult.getStatus());
            return storageInfoResult.getStorageInfo();
        } finally {
            mReadWriteLock.readLock().unlock();
        }
    }

    /**
     * Extracts and returns {@link StorageInfo} from {@link StorageInfoProto} based on
     * prefixed namespaces.
     *
     * @param storageInfoProto   The source {@link StorageInfoProto} containing storage information
     *                           to be analyzed.
     * @param prefixedNamespaces A set of prefixed namespaces that the storage information will be
     *                           filtered against. Only namespaces in this set will be included
     *                           in the analysis.
     * @param storageInfoBuilder The {@link StorageInfo.Builder} used to and build the resulting
     *                           {@link StorageInfo}. This builder will be modified with calculated
     *                           values.
     */
    private static void getDocumentStorageInfoForNamespaces(
            @NonNull StorageInfoProto storageInfoProto,
            @NonNull Set<String> prefixedNamespaces,
            StorageInfo.@NonNull Builder storageInfoBuilder) {
        if (!storageInfoProto.hasDocumentStorageInfo()) {
            return;
        }

        long totalStorageSize = storageInfoProto.getTotalStorageSize();
        DocumentStorageInfoProto documentStorageInfo =
                storageInfoProto.getDocumentStorageInfo();
        int totalDocuments =
                documentStorageInfo.getNumAliveDocuments()
                        + documentStorageInfo.getNumExpiredDocuments();

        if (totalStorageSize == 0 || totalDocuments == 0) {
            // Maybe we can exit early and also avoid a divide by 0 error.
            return;
        }

        // Accumulate stats across the package's namespaces.
        int aliveDocuments = 0;
        int expiredDocuments = 0;
        int aliveNamespaces = 0;
        List<NamespaceStorageInfoProto> namespaceStorageInfos =
                documentStorageInfo.getNamespaceStorageInfoList();
        for (int i = 0; i < namespaceStorageInfos.size(); i++) {
            NamespaceStorageInfoProto namespaceStorageInfo = namespaceStorageInfos.get(i);
            // The namespace from icing lib is already the prefixed format
            if (prefixedNamespaces.contains(namespaceStorageInfo.getNamespace())) {
                if (namespaceStorageInfo.getNumAliveDocuments() > 0) {
                    aliveNamespaces++;
                    aliveDocuments += namespaceStorageInfo.getNumAliveDocuments();
                }
                expiredDocuments += namespaceStorageInfo.getNumExpiredDocuments();
            }
        }
        int namespaceDocuments = aliveDocuments + expiredDocuments;

        // Since we don't have the exact size of all the documents, we do an estimation. Note
        // that while the total storage takes into account schema, index, etc. in addition to
        // documents, we'll only calculate the percentage based on number of documents a
        // client has.
        storageInfoBuilder
                .setSizeBytes((long) (namespaceDocuments * 1.0 / totalDocuments * totalStorageSize))
                .setAliveDocumentsCount(aliveDocuments)
                .setAliveNamespacesCount(aliveNamespaces);
    }

    /**
     * Extracts and returns blob storage information from {@link StorageInfoProto} based on
     * a namespace prefix.
     *
     * @param storageInfoProto   The source {@link StorageInfoProto} containing blob storage
     *                           information to be analyzed.
     * @param prefix             The prefix to match namespaces against. Only blob storage for
     *                           namespaces starting with this prefix will be included.
     * @param storageInfoBuilder The {@link StorageInfo.Builder} used to and build the resulting
     *                           {@link StorageInfo}. This builder will be modified with calculated
     *                           values.
     */
    @ExperimentalAppSearchApi
    private void getBlobStorageInfoForPrefix(
            @NonNull StorageInfoProto storageInfoProto,
            @NonNull String prefix,
            StorageInfo.@NonNull Builder storageInfoBuilder) {
        if (storageInfoProto.getNamespaceBlobStorageInfoCount() == 0) {
            return;
        }
        List<NamespaceBlobStorageInfoProto> blobStorageInfoProtos =
                storageInfoProto.getNamespaceBlobStorageInfoList();
        long blobSizeBytes = 0;
        int blobCount = 0;
        for (int i = 0; i < blobStorageInfoProtos.size(); i++) {
            NamespaceBlobStorageInfoProto blobStorageInfoProto = blobStorageInfoProtos.get(i);
            if (blobStorageInfoProto.getNamespace().startsWith(prefix)) {
                if (Flags.enableAppSearchManageBlobFiles()) {
                    List<String> blobFileNames = blobStorageInfoProto.getBlobFileNamesList();
                    for (int j = 0; j < blobFileNames.size(); j++) {
                        File blobFile = new File(mBlobFilesDir, blobFileNames.get(j));
                        blobSizeBytes += blobFile.length();
                    }
                    blobCount += blobFileNames.size();
                } else {
                    blobSizeBytes += blobStorageInfoProto.getBlobSize();
                    blobCount += blobStorageInfoProto.getNumBlobs();
                }
            }
        }
        storageInfoBuilder.setBlobsCount(blobCount)
                .setBlobsSizeBytes(blobSizeBytes);
    }

    /**
     * Returns the native debug info capsuled in {@link DebugInfoResultProto} directly from
     * IcingSearchEngine.
     *
     * @param verbosity The verbosity of the debug info. {@link DebugInfoVerbosity.Code#BASIC}
     *                  will return the simplest debug information.
     *                  {@link DebugInfoVerbosity.Code#DETAILED} will return more detailed
     *                  debug information as indicated in the comments in debug.proto
     */
    public @NonNull DebugInfoProto getRawDebugInfoProto(DebugInfoVerbosity.@NonNull Code verbosity)
            throws AppSearchException {
        mReadWriteLock.readLock().lock();
        try {
            throwIfClosedLocked();
            LogUtil.piiTrace(TAG, "getDebugInfo, request");
            DebugInfoResultProto debugInfoResult = mIcingSearchEngineLocked.getDebugInfo(
                    verbosity);
            LogUtil.piiTrace(TAG, "getDebugInfo, response", debugInfoResult.getStatus(),
                    debugInfoResult);
            checkSuccess(debugInfoResult.getStatus());
            return debugInfoResult.getDebugInfo();
        } finally {
            mReadWriteLock.readLock().unlock();
        }
    }

    /**
     * Persists all update/delete requests to the disk.
     *
     * <p>If the app crashes after a call to PersistToDisk with {@link PersistType.Code#FULL}, Icing
     * would be able to fully recover all data written up to this point without a costly recovery
     * process.
     *
     * <p>If the app crashes after a call to PersistToDisk with {@link PersistType.Code#LITE}, Icing
     * would trigger a costly recovery process in next initialization. After that, Icing would still
     * be able to recover all written data - excepting Usage data. Usage data is only guaranteed
     * to be safe after a call to PersistToDisk with {@link PersistType.Code#FULL}
     *
     * <p>If the app crashes after an update/delete request has been made, but before any call to
     * PersistToDisk, then all data in Icing will be lost.
     *
     * @param persistType the amount of data to persist. {@link PersistType.Code#LITE} will only
     *                    persist the minimal amount of data to ensure all data can be recovered.
     *                    {@link PersistType.Code#FULL} will persist all data necessary to
     *                    prevent data loss without needing data recovery.
     * @throws AppSearchException on any error that AppSearch persist data to disk.
     */
    public void persistToDisk(PersistType.@NonNull Code persistType) throws AppSearchException {
        mReadWriteLock.writeLock().lock();
        try {
            throwIfClosedLocked();

            LogUtil.piiTrace(TAG, "persistToDisk, request", persistType);
            PersistToDiskResultProto persistToDiskResultProto =
                    mIcingSearchEngineLocked.persistToDisk(persistType);
            LogUtil.piiTrace(
                    TAG,
                    "persistToDisk, response",
                    persistToDiskResultProto.getStatus(),
                    persistToDiskResultProto);
            checkSuccess(persistToDiskResultProto.getStatus());
        } finally {
            mReadWriteLock.writeLock().unlock();
        }
    }

    /**
     * Remove all {@link AppSearchSchema}s and {@link GenericDocument}s under the given package.
     *
     * @param packageName The name of package to be removed.
     * @throws AppSearchException if we cannot remove the data.
     */
    @OptIn(markerClass = ExperimentalAppSearchApi.class)
    public void clearPackageData(@NonNull String packageName) throws AppSearchException,
            IOException {
        mReadWriteLock.writeLock().lock();
        try {
            throwIfClosedLocked();
            if (LogUtil.DEBUG) {
                Log.d(TAG, "Clear data for package: " + packageName);
            }
            // TODO(b/193494000): We are calling getPackageToDatabases here and in several other
            //  places within AppSearchImpl. This method is not efficient and does a lot of string
            //  manipulation. We should find a way to cache the package to database map so it can
            //  just be obtained from a local variable instead of being parsed out of the prefixed
            //  map.
            Set<String> existingPackages = getPackageToDatabases().keySet();
            if (existingPackages.contains(packageName)) {
                existingPackages.remove(packageName);
                prunePackageData(existingPackages);
            }
            if (mRevocableFileDescriptorStore != null) {
                mRevocableFileDescriptorStore.revokeForPackage(packageName);
            }
        } finally {
            mReadWriteLock.writeLock().unlock();
        }
    }

    /**
     * Remove all {@link AppSearchSchema}s and {@link GenericDocument}s that doesn't belong to any
     * of the given installed packages
     *
     * @param installedPackages The name of all installed package.
     * @throws AppSearchException if we cannot remove the data.
     */
    public void prunePackageData(@NonNull Set<String> installedPackages) throws AppSearchException {
        mReadWriteLock.writeLock().lock();
        try {
            throwIfClosedLocked();
            Map<String, Set<String>> packageToDatabases = getPackageToDatabases();
            if (installedPackages.containsAll(packageToDatabases.keySet())) {
                // No package got removed. We are good.
                return;
            }

            // Prune schema proto
            SchemaProto existingSchema = getSchemaProtoLocked();
            SchemaProto.Builder newSchemaBuilder = SchemaProto.newBuilder();
            for (int i = 0; i < existingSchema.getTypesCount(); i++) {
                String packageName = getPackageName(existingSchema.getTypes(i).getSchemaType());
                if (installedPackages.contains(packageName)) {
                    newSchemaBuilder.addTypes(existingSchema.getTypes(i));
                }
            }

            SchemaProto finalSchema = newSchemaBuilder.build();

            // Apply schema, set force override to true to remove all schemas and documents that
            // doesn't belong to any of these installed packages.
            LogUtil.piiTrace(
                    TAG,
                    "clearPackageData.setSchema, request",
                    finalSchema.getTypesCount(),
                    finalSchema);
            SetSchemaResultProto setSchemaResultProto = mIcingSearchEngineLocked.setSchema(
                    finalSchema, /*ignoreErrorsAndDeleteDocuments=*/ true);
            LogUtil.piiTrace(
                    TAG,
                    "clearPackageData.setSchema, response",
                    setSchemaResultProto.getStatus(),
                    setSchemaResultProto);

            // Determine whether it succeeded.
            checkSuccess(setSchemaResultProto.getStatus());

            // Prune cached maps
            for (Map.Entry<String, Set<String>> entry : packageToDatabases.entrySet()) {
                String packageName = entry.getKey();
                Set<String> databaseNames = entry.getValue();
                if (!installedPackages.contains(packageName) && databaseNames != null) {
                    mDocumentLimiterLocked.reportPackageRemoved(packageName);
                    synchronized (mNextPageTokensLocked) {
                        mNextPageTokensLocked.remove(packageName);
                    }
                    for (String databaseName : databaseNames) {
                        String removedPrefix = createPrefix(packageName, databaseName);
                        Set<String> removedSchemas = mSchemaCacheLocked.removePrefix(removedPrefix);
                        if (mDocumentVisibilityStoreLocked != null) {
                            mDocumentVisibilityStoreLocked.removeVisibility(removedSchemas);
                        }

                        mNamespaceCacheLocked.removeDocumentNamespaces(removedPrefix);
                    }
                }
            }
        } finally {
            mReadWriteLock.writeLock().unlock();
        }
    }

    /**
     * Clears documents and schema across all packages and databaseNames.
     *
     * <p>This method belongs to mutate group.
     *
     * @throws AppSearchException on IcingSearchEngine error.
     */
    @GuardedBy("mReadWriteLock")
    private void resetLocked(InitializeStats.@Nullable Builder initStatsBuilder)
            throws AppSearchException {
        LogUtil.piiTrace(TAG, "icingSearchEngine.reset, request");
        ResetResultProto resetResultProto = mIcingSearchEngineLocked.reset();
        LogUtil.piiTrace(
                TAG,
                "icingSearchEngine.reset, response",
                resetResultProto.getStatus(),
                resetResultProto);
        mOptimizeIntervalCountLocked = 0;
        mSchemaCacheLocked.clear();
        mNamespaceCacheLocked.clear();

        // We just reset the index. So there is no need to retrieve the actual storage info. We know
        // that there are no actual namespaces.
        List<NamespaceStorageInfoProto> emptyNamespaceInfos = Collections.emptyList();
        mDocumentLimiterLocked =
                new DocumentLimiter(
                        mConfig.getDocumentCountLimitStartThreshold(),
                        mConfig.getPerPackageDocumentCountLimit(), emptyNamespaceInfos);
        synchronized (mNextPageTokensLocked) {
            mNextPageTokensLocked.clear();
        }
        if (initStatsBuilder != null) {
            initStatsBuilder
                    .setHasReset(true)
                    .setResetStatusCode(statusProtoToResultCode(resetResultProto.getStatus()));
        }

        checkSuccess(resetResultProto.getStatus());
    }

    /** Wrapper around schema changes */
    @VisibleForTesting
    static class RewrittenSchemaResults {
        // Any prefixed types that used to exist in the schema, but are deleted in the new one.
        final Set<String> mDeletedPrefixedTypes = new ArraySet<>();

        // Map of prefixed schema types to SchemaTypeConfigProtos that were part of the new schema.
        final Map<String, SchemaTypeConfigProto> mRewrittenPrefixedTypes = new ArrayMap<>();
    }

    /**
     * Rewrites all types mentioned in the given {@code newSchema} to prepend {@code prefix}.
     * Rewritten types will be added to the {@code existingSchema}.
     *
     * @param prefix         The full prefix to prepend to the schema.
     * @param existingSchema A schema that may contain existing types from across all prefixes.
     *                       Will be mutated to contain the properly rewritten schema
     *                       types from {@code newSchema}.
     * @param newSchema      Schema with types to add to the {@code existingSchema}.
     * @return a RewrittenSchemaResults that contains all prefixed schema type names in the given
     * prefix as well as a set of schema types that were deleted.
     */
    @VisibleForTesting
    static RewrittenSchemaResults rewriteSchema(@NonNull String prefix,
            SchemaProto.@NonNull Builder existingSchema,
            @NonNull SchemaProto newSchema) throws AppSearchException {
        HashMap<String, SchemaTypeConfigProto> newTypesToProto = new HashMap<>();
        // Rewrite the schema type to include the typePrefix.
        for (int typeIdx = 0; typeIdx < newSchema.getTypesCount(); typeIdx++) {
            SchemaTypeConfigProto.Builder typeConfigBuilder =
                    newSchema.getTypes(typeIdx).toBuilder();

            // Rewrite SchemaProto.types.schema_type
            String newSchemaType = prefix + typeConfigBuilder.getSchemaType();
            typeConfigBuilder.setSchemaType(newSchemaType);

            // Rewrite SchemaProto.types.properties.schema_type
            for (int propertyIdx = 0;
                    propertyIdx < typeConfigBuilder.getPropertiesCount();
                    propertyIdx++) {
                PropertyConfigProto.Builder propertyConfigBuilder =
                        typeConfigBuilder.getProperties(propertyIdx).toBuilder();
                if (!propertyConfigBuilder.getSchemaType().isEmpty()) {
                    String newPropertySchemaType =
                            prefix + propertyConfigBuilder.getSchemaType();
                    propertyConfigBuilder.setSchemaType(newPropertySchemaType);
                    typeConfigBuilder.setProperties(propertyIdx, propertyConfigBuilder);
                }
            }

            // Rewrite SchemaProto.types.parent_types
            for (int parentTypeIdx = 0; parentTypeIdx < typeConfigBuilder.getParentTypesCount();
                    parentTypeIdx++) {
                String newParentType = prefix + typeConfigBuilder.getParentTypes(parentTypeIdx);
                typeConfigBuilder.setParentTypes(parentTypeIdx, newParentType);
            }

            newTypesToProto.put(newSchemaType, typeConfigBuilder.build());
        }

        // newTypesToProto is modified below, so we need a copy first
        RewrittenSchemaResults rewrittenSchemaResults = new RewrittenSchemaResults();
        rewrittenSchemaResults.mRewrittenPrefixedTypes.putAll(newTypesToProto);

        // Combine the existing schema (which may have types from other prefixes) with this
        // prefix's new schema. Modifies the existingSchemaBuilder.
        // Check if we need to replace any old schema types with the new ones.
        for (int i = 0; i < existingSchema.getTypesCount(); i++) {
            String schemaType = existingSchema.getTypes(i).getSchemaType();
            SchemaTypeConfigProto newProto = newTypesToProto.remove(schemaType);
            if (newProto != null) {
                // Replacement
                existingSchema.setTypes(i, newProto);
            } else if (prefix.equals(getPrefix(schemaType))) {
                // All types existing before but not in newSchema should be removed.
                existingSchema.removeTypes(i);
                --i;
                rewrittenSchemaResults.mDeletedPrefixedTypes.add(schemaType);
            }
        }
        // We've been removing existing types from newTypesToProto, so everything that remains is
        // new.
        existingSchema.addAllTypes(newTypesToProto.values());

        return rewrittenSchemaResults;
    }

    /**
     * Rewrite the {@link InternalVisibilityConfig} to add given prefix in the schemaType of the
     * given List of {@link InternalVisibilityConfig}
     *
     * @param prefix                      The full prefix to prepend to the visibilityConfigs.
     * @param visibilityConfigs           The visibility configs that need to add prefix
     * @param removedVisibilityConfigs    The removed configs that is not included in the given
     *                                    visibilityConfigs.
     * @return The List of {@link InternalVisibilityConfig} that contains prefixed in its schema
     * types.
     */
    private List<InternalVisibilityConfig> rewriteVisibilityConfigs(@NonNull String prefix,
            @NonNull List<InternalVisibilityConfig> visibilityConfigs,
            @NonNull Set<String> removedVisibilityConfigs) {
        List<InternalVisibilityConfig> prefixedVisibilityConfigs =
                new ArrayList<>(visibilityConfigs.size());
        for (int i = 0; i < visibilityConfigs.size(); i++) {
            InternalVisibilityConfig visibilityConfig = visibilityConfigs.get(i);
            // The VisibilityConfig is controlled by the client and it's untrusted but we
            // make it safe by appending a prefix.
            // We must control the package-database prefix. Therefore even if the client
            // fake the id, they can only mess their own app. That's totally allowed and
            // they can do this via the public API too.
            // TODO(b/275592563): Move prefixing into VisibilityConfig
            //  .createVisibilityDocument and createVisibilityOverlay
            String namespace = visibilityConfig.getSchemaType();
            String prefixedNamespace = prefix + namespace;
            prefixedVisibilityConfigs.add(
                    new InternalVisibilityConfig.Builder(visibilityConfig)
                            .setSchemaType(prefixedNamespace)
                            .build());
            // This schema has visibility settings. We should keep it from the removal list.
            removedVisibilityConfigs.remove(prefixedNamespace);
        }
        return prefixedVisibilityConfigs;
    }

    @VisibleForTesting
    @GuardedBy("mReadWriteLock")
    SchemaProto getSchemaProtoLocked() throws AppSearchException {
        LogUtil.piiTrace(TAG, "getSchema, request");
        GetSchemaResultProto schemaProto = mIcingSearchEngineLocked.getSchema();
        LogUtil.piiTrace(TAG, "getSchema, response", schemaProto.getStatus(), schemaProto);
        // TODO(b/161935693) check GetSchemaResultProto is success or not. Call reset() if it's not.
        // TODO(b/161935693) only allow GetSchemaResultProto NOT_FOUND on first run
        checkCodeOneOf(schemaProto.getStatus(), StatusProto.Code.OK, StatusProto.Code.NOT_FOUND);
        return schemaProto.getSchema();
    }

    private void addNextPageToken(String packageName, long nextPageToken) {
        if (nextPageToken == EMPTY_PAGE_TOKEN) {
            // There is no more pages. No need to add it.
            return;
        }
        synchronized (mNextPageTokensLocked) {
            Set<Long> tokens = mNextPageTokensLocked.get(packageName);
            if (tokens == null) {
                tokens = new ArraySet<>();
                mNextPageTokensLocked.put(packageName, tokens);
            }
            tokens.add(nextPageToken);
        }
    }

    private void checkNextPageToken(String packageName, long nextPageToken)
            throws AppSearchException {
        if (nextPageToken == EMPTY_PAGE_TOKEN) {
            // Swallow the check for empty page token, token = 0 means there is no more page and it
            // won't return anything from Icing.
            return;
        }
        synchronized (mNextPageTokensLocked) {
            Set<Long> nextPageTokens = mNextPageTokensLocked.get(packageName);
            if (nextPageTokens == null || !nextPageTokens.contains(nextPageToken)) {
                throw new AppSearchException(RESULT_SECURITY_ERROR,
                        "Package \"" + packageName + "\" cannot use nextPageToken: "
                                + nextPageToken);
            }
        }
    }

    /**
     * Adds an {@link ObserverCallback} to monitor changes within the databases owned by
     * {@code targetPackageName} if they match the given
     * {@link androidx.appsearch.observer.ObserverSpec}.
     *
     * <p>If the data owned by {@code targetPackageName} is not visible to you, the registration
     * call will succeed but no notifications will be dispatched. Notifications could start flowing
     * later if {@code targetPackageName} changes its schema visibility settings.
     *
     * <p>If no package matching {@code targetPackageName} exists on the system, the registration
     * call will succeed but no notifications will be dispatched. Notifications could start flowing
     * later if {@code targetPackageName} is installed and starts indexing data.
     *
     * <p>Note that this method does not take the standard read/write lock that guards I/O, so it
     * will not queue behind I/O. Therefore it is safe to call from any thread including UI or
     * binder threads.
     *
     * @param listeningPackageAccess Visibility information about the app that wants to receive
     *                               notifications.
     * @param targetPackageName      The package that owns the data the observer wants to be
     *                               notified for.
     * @param spec                   Describes the kind of data changes the observer should trigger
     *                               for.
     * @param executor               The executor on which to trigger the observer callback to
     *                               deliver notifications.
     * @param observer               The callback to trigger on notifications.
     */
    public void registerObserverCallback(
            @NonNull CallerAccess listeningPackageAccess,
            @NonNull String targetPackageName,
            @NonNull ObserverSpec spec,
            @NonNull Executor executor,
            @NonNull ObserverCallback observer) {
        // This method doesn't consult mSchemaMap or mNamespaceMap, and it will register
        // observers for types that don't exist. This is intentional because we notify for types
        // being created or removed. If we only registered observer for existing types, it would
        // be impossible to ever dispatch a notification of a type being added.
        mObserverManager.registerObserverCallback(
                listeningPackageAccess, targetPackageName, spec, executor, observer);
    }

    /**
     * Removes an {@link ObserverCallback} from watching the databases owned by
     * {@code targetPackageName}.
     *
     * <p>All observers which compare equal to the given observer via
     * {@link ObserverCallback#equals} are removed. This may be 0, 1, or many observers.
     *
     * <p>Note that this method does not take the standard read/write lock that guards I/O, so it
     * will not queue behind I/O. Therefore it is safe to call from any thread including UI or
     * binder threads.
     */
    public void unregisterObserverCallback(
            @NonNull String targetPackageName, @NonNull ObserverCallback observer) {
        mObserverManager.unregisterObserverCallback(targetPackageName, observer);
    }

    /**
     * Dispatches the pending change notifications one at a time.
     *
     * <p>The notifications are dispatched on the respective executors that were provided at the
     * time of observer registration. This method does not take the standard read/write lock that
     * guards I/O, so it is safe to call from any thread including UI or binder threads.
     *
     * <p>Exceptions thrown from notification dispatch are logged but otherwise suppressed.
     */
    public void dispatchAndClearChangeNotifications() {
        mObserverManager.dispatchAndClearPendingNotifications();
    }

    /**
     * Checks the given status code and throws an {@link AppSearchException} if code is an error.
     *
     * @throws AppSearchException on error codes.
     */
    private static void checkSuccess(StatusProto statusProto) throws AppSearchException {
        checkCodeOneOf(statusProto, StatusProto.Code.OK);
    }

    /**
     * Checks the given status code is one of the provided codes, and throws an
     * {@link AppSearchException} if it is not.
     */
    private static void checkCodeOneOf(StatusProto statusProto, StatusProto.Code... codes)
            throws AppSearchException {
        for (int i = 0; i < codes.length; i++) {
            if (codes[i] == statusProto.getCode()) {
                // Everything's good
                return;
            }
        }

        if (statusProto.getCode() == StatusProto.Code.WARNING_DATA_LOSS) {
            // TODO: May want to propagate WARNING_DATA_LOSS up to AppSearchSession so they can
            //  choose to log the error or potentially pass it on to clients.
            Log.w(TAG, "Encountered WARNING_DATA_LOSS: " + statusProto.getMessage());
            return;
        }

        throw new AppSearchException(
                ResultCodeToProtoConverter.toResultCode(statusProto.getCode()),
                statusProto.getMessage());
    }

    /**
     * Checks whether {@link IcingSearchEngine#optimize()} should be called to release resources.
     *
     * <p>This method should be only called after a mutation to local storage backend which
     * deletes a mass of data and could release lots resources after
     * {@link IcingSearchEngine#optimize()}.
     *
     * <p>This method will trigger {@link IcingSearchEngine#getOptimizeInfo()} to check
     * resources that could be released for every {@link #CHECK_OPTIMIZE_INTERVAL} mutations.
     *
     * <p>{@link IcingSearchEngine#optimize()} should be called only if
     * {@link GetOptimizeInfoResultProto} shows there is enough resources could be released.
     *
     * @param mutationSize The number of how many mutations have been executed for current request.
     *                     An inside counter will accumulates it. Once the counter reaches
     *                     {@link #CHECK_OPTIMIZE_INTERVAL},
     *                     {@link IcingSearchEngine#getOptimizeInfo()} will be triggered and the
     *                     counter will be reset.
     */
    public void checkForOptimize(int mutationSize, OptimizeStats.@Nullable Builder builder)
            throws AppSearchException {
        mReadWriteLock.writeLock().lock();
        try {
            mOptimizeIntervalCountLocked += mutationSize;
            if (mOptimizeIntervalCountLocked >= CHECK_OPTIMIZE_INTERVAL) {
                checkForOptimize(builder);
            }
        } finally {
            mReadWriteLock.writeLock().unlock();
        }
    }

    /**
     * Checks whether {@link IcingSearchEngine#optimize()} should be called to release resources.
     *
     * <p>This method will directly trigger {@link IcingSearchEngine#getOptimizeInfo()} to check
     * resources that could be released.
     *
     * <p>{@link IcingSearchEngine#optimize()} should be called only if
     * {@link OptimizeStrategy#shouldOptimize(GetOptimizeInfoResultProto)} return true.
     */
    public void checkForOptimize(OptimizeStats.@Nullable Builder builder)
            throws AppSearchException {
        mReadWriteLock.writeLock().lock();
        try {
            GetOptimizeInfoResultProto optimizeInfo = getOptimizeInfoResultLocked();
            checkSuccess(optimizeInfo.getStatus());
            mOptimizeIntervalCountLocked = 0;
            if (mOptimizeStrategy.shouldOptimize(optimizeInfo)) {
                optimize(builder);
            }
        } finally {
            mReadWriteLock.writeLock().unlock();
        }
        // TODO(b/147699081): Return OptimizeResultProto & log lost data detail once we add
        //  a field to indicate lost_schema and lost_documents in OptimizeResultProto.
        //  go/icing-library-apis.
    }

    /** Triggers {@link IcingSearchEngine#optimize()} directly. */
    public void optimize(OptimizeStats.@Nullable Builder builder) throws AppSearchException {
        mReadWriteLock.writeLock().lock();
        try {
            LogUtil.piiTrace(TAG, "optimize, request");
            OptimizeResultProto optimizeResultProto = mIcingSearchEngineLocked.optimize();
            LogUtil.piiTrace(
                    TAG,
                    "optimize, response", optimizeResultProto.getStatus(), optimizeResultProto);
            if (builder != null) {
                builder.setStatusCode(statusProtoToResultCode(optimizeResultProto.getStatus()));
                AppSearchLoggerHelper.copyNativeStats(optimizeResultProto.getOptimizeStats(),
                        builder);
            }
            checkSuccess(optimizeResultProto.getStatus());

            // If AppSearch manages blob files, remove the optimized blob files.
            if (Flags.enableAppSearchManageBlobFiles()) {
                List<String> blobFileNamesToRemove =
                        optimizeResultProto.getBlobFileNamesToRemoveList();
                for (int i = 0; i < blobFileNamesToRemove.size(); i++) {
                    File blobFileToRemove = new File(mBlobFilesDir, blobFileNamesToRemove.get(i));
                    if (!blobFileToRemove.delete()) {
                        Log.e(TAG, "Cannot delete the optimized blob file: "
                                + blobFileToRemove.getName());
                    }
                }
            }
        } finally {
            mReadWriteLock.writeLock().unlock();
        }
    }

    /**
     * Sync the current Android logging level to Icing for the entire process. No lock required.
     */
    public static void syncLoggingLevelToIcing() {
        String icingTag = IcingSearchEngine.getLoggingTag();
        if (icingTag == null) {
            Log.e(TAG, "Received null logging tag from Icing");
            return;
        }
        if (LogUtil.DEBUG) {
            if (Log.isLoggable(icingTag, Log.VERBOSE)) {
                boolean unused = IcingSearchEngine.setLoggingLevel(LogSeverity.Code.VERBOSE,
                        /*verbosity=*/ (short) 1);
                return;
            } else if (Log.isLoggable(icingTag, Log.DEBUG)) {
                boolean unused = IcingSearchEngine.setLoggingLevel(LogSeverity.Code.DBG);
                return;
            }
        }
        if (LogUtil.INFO) {
            if (Log.isLoggable(icingTag, Log.INFO)) {
                boolean unused = IcingSearchEngine.setLoggingLevel(LogSeverity.Code.INFO);
                return;
            }
        }
        if (Log.isLoggable(icingTag, Log.WARN)) {
            boolean unused = IcingSearchEngine.setLoggingLevel(LogSeverity.Code.WARNING);
        } else if (Log.isLoggable(icingTag, Log.ERROR)) {
            boolean unused = IcingSearchEngine.setLoggingLevel(LogSeverity.Code.ERROR);
        } else {
            boolean unused = IcingSearchEngine.setLoggingLevel(LogSeverity.Code.FATAL);
        }
    }

    @GuardedBy("mReadWriteLock")
    @VisibleForTesting
    GetOptimizeInfoResultProto getOptimizeInfoResultLocked() {
        LogUtil.piiTrace(TAG, "getOptimizeInfo, request");
        GetOptimizeInfoResultProto result = mIcingSearchEngineLocked.getOptimizeInfo();
        LogUtil.piiTrace(TAG, "getOptimizeInfo, response", result.getStatus(), result);
        return result;
    }

    /**
     * Returns all prefixed schema types saved in AppSearch.
     *
     * <p>This method is inefficient to call repeatedly.
     */
    public @NonNull List<String> getAllPrefixedSchemaTypes() {
        mReadWriteLock.readLock().lock();
        try {
            return mSchemaCacheLocked.getAllPrefixedSchemaTypes();
        } finally {
            mReadWriteLock.readLock().unlock();
        }
    }

    /**
     * Returns all prefixed blob namespaces saved in AppSearch.
     *
     * <p>This method is inefficient to call repeatedly.
     */
    public @NonNull List<String> getAllPrefixedBlobNamespaces() {
        mReadWriteLock.readLock().lock();
        try {
            return mNamespaceCacheLocked.getAllPrefixedBlobNamespaces();
        } finally {
            mReadWriteLock.readLock().unlock();
        }
    }

    /**
     * Converts an erroneous status code from the Icing status enums to the AppSearchResult enums.
     *
     * <p>Callers should ensure that the status code is not OK or WARNING_DATA_LOSS.
     *
     * @param statusProto StatusProto with error code to translate into an
     *                    {@link AppSearchResult} code.
     * @return {@link AppSearchResult} error code
     */
    @AppSearchResult.ResultCode private static int statusProtoToResultCode(
            @NonNull StatusProto statusProto) {
        return ResultCodeToProtoConverter.toResultCode(statusProto.getCode());
    }

    @ExperimentalAppSearchApi
    private static void verifyCallingBlobHandle(@NonNull String callingPackageName,
            @NonNull String callingDatabaseName, @NonNull AppSearchBlobHandle blobHandle)
            throws AppSearchException {
        if (!blobHandle.getPackageName().equals(callingPackageName)) {
            throw new AppSearchException(AppSearchResult.RESULT_INVALID_ARGUMENT,
                    "Blob package doesn't match calling package, calling package: "
                            + callingPackageName + ", blob package: "
                            + blobHandle.getPackageName());
        }
        if (!blobHandle.getDatabaseName().equals(callingDatabaseName)) {
            throw new AppSearchException(AppSearchResult.RESULT_INVALID_ARGUMENT,
                    "Blob database doesn't match calling database, calling database: "
                            + callingDatabaseName + ", blob database: "
                            + blobHandle.getDatabaseName());
        }
    }

}
