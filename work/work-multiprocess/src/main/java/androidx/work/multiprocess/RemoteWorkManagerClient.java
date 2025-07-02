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

package androidx.work.multiprocess;

import static android.content.Context.BIND_AUTO_CREATE;

import static androidx.work.multiprocess.RemoteClientUtilsKt.map;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;

import androidx.annotation.RestrictTo;
import androidx.annotation.VisibleForTesting;
import androidx.arch.core.util.Function;
import androidx.work.Data;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.ExistingWorkPolicy;
import androidx.work.ForegroundInfo;
import androidx.work.Logger;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.RunnableScheduler;
import androidx.work.WorkContinuation;
import androidx.work.WorkInfo;
import androidx.work.WorkQuery;
import androidx.work.WorkRequest;
import androidx.work.impl.WorkContinuationImpl;
import androidx.work.impl.WorkManagerImpl;
import androidx.work.impl.utils.futures.SettableFuture;
import androidx.work.multiprocess.parcelable.ParcelConverters;
import androidx.work.multiprocess.parcelable.ParcelableForegroundRequestInfo;
import androidx.work.multiprocess.parcelable.ParcelableUpdateRequest;
import androidx.work.multiprocess.parcelable.ParcelableWorkContinuationImpl;
import androidx.work.multiprocess.parcelable.ParcelableWorkInfos;
import androidx.work.multiprocess.parcelable.ParcelableWorkQuery;
import androidx.work.multiprocess.parcelable.ParcelableWorkRequest;
import androidx.work.multiprocess.parcelable.ParcelableWorkRequests;

import com.google.common.util.concurrent.ListenableFuture;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;

/**
 * The implementation of the {@link RemoteWorkManager} which sets up the
 * {@link android.content.ServiceConnection} and dispatches the request.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class RemoteWorkManagerClient extends RemoteWorkManager {

    // Synthetic access
    static final String TAG = Logger.tagWithPrefix("RemoteWorkManagerClient");

    /**
     * A mapper that essentially drops the byte[].
     */
    public static final Function<byte[], Void> sVoidMapper = input -> null;

    // Synthetic access
    Session mSession;

    final Context mContext;
    final WorkManagerImpl mWorkManager;
    final Executor mExecutor;
    final Object mLock;

    private volatile long mSessionIndex;
    private final long mSessionTimeout;
    private final RunnableScheduler mRunnableScheduler;
    private final SessionTracker mSessionTracker;

    public RemoteWorkManagerClient(@NonNull Context context, @NonNull WorkManagerImpl workManager) {
        mContext = context.getApplicationContext();
        mWorkManager = workManager;
        mExecutor = mWorkManager.getWorkTaskExecutor().getSerialTaskExecutor();
        mLock = new Object();
        mSession = null;
        mSessionTracker = new SessionTracker(this);
        mSessionTimeout = workManager.getConfiguration().getRemoteSessionTimeoutMillis();
        mRunnableScheduler = mWorkManager.getConfiguration().getRunnableScheduler();
    }

    @Override
    public @NonNull ListenableFuture<Void> enqueue(@NonNull WorkRequest request) {
        return enqueue(Collections.singletonList(request));
    }

    @Override
    public @NonNull ListenableFuture<Void> enqueue(final @NonNull List<WorkRequest> requests) {
        ListenableFuture<byte[]> result = execute(new RemoteDispatcher<IWorkManagerImpl>() {
            @Override
            public void execute(
                    @NonNull IWorkManagerImpl iWorkManagerImpl,
                    @NonNull IWorkManagerImplCallback callback) throws RemoteException {
                byte[] request = ParcelConverters.marshall(new ParcelableWorkRequests(requests));
                iWorkManagerImpl.enqueueWorkRequests(request, callback);
            }
        });
        return map(result, sVoidMapper, mExecutor);
    }

    @Override
    public @NonNull ListenableFuture<Void> enqueueUniqueWork(
            @NonNull String uniqueWorkName,
            @NonNull ExistingWorkPolicy existingWorkPolicy,
            @NonNull List<OneTimeWorkRequest> work) {
        return beginUniqueWork(uniqueWorkName, existingWorkPolicy, work).enqueue();
    }

    @Override
    public @NonNull ListenableFuture<Void> enqueueUniquePeriodicWork(
            @NonNull String uniqueWorkName,
            @NonNull ExistingPeriodicWorkPolicy existingPeriodicWorkPolicy,
            @NonNull PeriodicWorkRequest periodicWork) {
        if (existingPeriodicWorkPolicy == ExistingPeriodicWorkPolicy.UPDATE) {
            ListenableFuture<byte[]> result = execute((iWorkManagerImpl, callback) -> {
                byte[] request = ParcelConverters.marshall(new ParcelableWorkRequest(periodicWork));
                iWorkManagerImpl.updateUniquePeriodicWorkRequest(uniqueWorkName, request, callback);
            });
            return map(result, sVoidMapper, mExecutor);
        }
        WorkContinuation continuation = mWorkManager.createWorkContinuationForUniquePeriodicWork(
                uniqueWorkName,
                existingPeriodicWorkPolicy,
                periodicWork
        );
        return enqueue(continuation);
    }

    @Override
    public @NonNull RemoteWorkContinuation beginWith(@NonNull List<OneTimeWorkRequest> work) {
        return new RemoteWorkContinuationImpl(this, mWorkManager.beginWith(work));
    }

    @Override
    public @NonNull RemoteWorkContinuation beginUniqueWork(
            @NonNull String uniqueWorkName,
            @NonNull ExistingWorkPolicy existingWorkPolicy,
            @NonNull List<OneTimeWorkRequest> work) {
        return new RemoteWorkContinuationImpl(this,
                mWorkManager.beginUniqueWork(uniqueWorkName, existingWorkPolicy, work));
    }

    @Override
    public @NonNull ListenableFuture<Void> enqueue(final @NonNull WorkContinuation continuation) {
        ListenableFuture<byte[]> result = execute(new RemoteDispatcher<IWorkManagerImpl>() {
            @Override
            public void execute(@NonNull IWorkManagerImpl iWorkManagerImpl,
                    @NonNull IWorkManagerImplCallback callback) throws Throwable {
                WorkContinuationImpl workContinuation = (WorkContinuationImpl) continuation;
                byte[] request = ParcelConverters.marshall(
                        new ParcelableWorkContinuationImpl(workContinuation));
                iWorkManagerImpl.enqueueContinuation(request, callback);
            }
        });
        return map(result, sVoidMapper, mExecutor);
    }

    @Override
    public @NonNull ListenableFuture<Void> cancelWorkById(final @NonNull UUID id) {
        ListenableFuture<byte[]> result = execute(new RemoteDispatcher<IWorkManagerImpl>() {
            @Override
            public void execute(@NonNull IWorkManagerImpl iWorkManagerImpl,
                    @NonNull IWorkManagerImplCallback callback) throws Throwable {
                iWorkManagerImpl.cancelWorkById(id.toString(), callback);
            }
        });
        return map(result, sVoidMapper, mExecutor);
    }

    @Override
    public @NonNull ListenableFuture<Void> cancelAllWorkByTag(final @NonNull String tag) {
        ListenableFuture<byte[]> result = execute(new RemoteDispatcher<IWorkManagerImpl>() {
            @Override
            public void execute(@NonNull IWorkManagerImpl iWorkManagerImpl,
                    @NonNull IWorkManagerImplCallback callback) throws Throwable {
                iWorkManagerImpl.cancelAllWorkByTag(tag, callback);
            }
        });
        return map(result, sVoidMapper, mExecutor);
    }

    @Override
    public @NonNull ListenableFuture<Void> cancelUniqueWork(final @NonNull String uniqueWorkName) {
        ListenableFuture<byte[]> result = execute(new RemoteDispatcher<IWorkManagerImpl>() {
            @Override
            public void execute(@NonNull IWorkManagerImpl iWorkManagerImpl,
                    @NonNull IWorkManagerImplCallback callback) throws Throwable {
                iWorkManagerImpl.cancelUniqueWork(uniqueWorkName, callback);
            }
        });
        return map(result, sVoidMapper, mExecutor);
    }

    @Override
    public @NonNull ListenableFuture<Void> cancelAllWork() {
        ListenableFuture<byte[]> result = execute(new RemoteDispatcher<IWorkManagerImpl>() {
            @Override
            public void execute(@NonNull IWorkManagerImpl iWorkManagerImpl,
                    @NonNull IWorkManagerImplCallback callback) throws Throwable {
                iWorkManagerImpl.cancelAllWork(callback);
            }
        });
        return map(result, sVoidMapper, mExecutor);
    }

    @Override
    public @NonNull ListenableFuture<List<WorkInfo>> getWorkInfos(
            final @NonNull WorkQuery workQuery) {
        ListenableFuture<byte[]> result = execute(new RemoteDispatcher<IWorkManagerImpl>() {
            @Override
            public void execute(
                    @NonNull IWorkManagerImpl iWorkManagerImpl,
                    @NonNull IWorkManagerImplCallback callback) throws Throwable {
                byte[] request = ParcelConverters.marshall(new ParcelableWorkQuery(workQuery));
                iWorkManagerImpl.queryWorkInfo(request, callback);
            }
        });
        return map(result, new Function<byte[], List<WorkInfo>>() {
            @Override
            public List<WorkInfo> apply(byte[] input) {
                ParcelableWorkInfos infos =
                        ParcelConverters.unmarshall(input, ParcelableWorkInfos.CREATOR);
                return infos.getWorkInfos();
            }
        }, mExecutor);
    }

    @Override
    public @NonNull ListenableFuture<Void> setProgress(final @NonNull UUID id,
            final @NonNull Data data) {
        ListenableFuture<byte[]> result = execute(new RemoteDispatcher<IWorkManagerImpl>() {
            @Override
            public void execute(
                    @NonNull IWorkManagerImpl iWorkManagerImpl,
                    @NonNull IWorkManagerImplCallback callback) throws Throwable {
                byte[] request = ParcelConverters.marshall(new ParcelableUpdateRequest(id, data));
                iWorkManagerImpl.setProgress(request, callback);
            }
        });
        return map(result, sVoidMapper, mExecutor);
    }

    @Override
    public @NonNull ListenableFuture<Void> setForegroundAsync(
            @NonNull String id,
            @NonNull ForegroundInfo foregroundInfo) {
        ListenableFuture<byte[]> result = execute(new RemoteDispatcher<IWorkManagerImpl>() {
            @Override
            public void execute(
                    @NonNull IWorkManagerImpl iWorkManagerImpl,
                    @NonNull IWorkManagerImplCallback callback) throws Throwable {
                byte[] request = ParcelConverters.marshall(
                        new ParcelableForegroundRequestInfo(id, foregroundInfo));
                iWorkManagerImpl.setForegroundAsync(request, callback);
            }
        });
        return map(result, sVoidMapper, mExecutor);
    }

    /**
     * Executes a {@link RemoteDispatcher} after having negotiated a service connection.
     *
     * @param dispatcher The {@link RemoteDispatcher} instance.
     * @return The {@link ListenableFuture} instance.
     */
    public @NonNull ListenableFuture<byte[]> execute(
            final @NonNull RemoteDispatcher<IWorkManagerImpl> dispatcher) {
        return execute(getSession(), dispatcher);
    }

    /**
     * Gets a handle to an instance of {@link IWorkManagerImpl} by binding to the
     * {@link RemoteWorkManagerService} if necessary.
     */
    public @NonNull ListenableFuture<IWorkManagerImpl> getSession() {
        return getSession(newIntent(mContext));
    }

    /**
     * @return The application {@link Context}.
     */
    public @NonNull Context getContext() {
        return mContext;
    }

    /**
     * @return The session timeout in milliseconds.
     */
    public long getSessionTimeout() {
        return mSessionTimeout;
    }

    /**
     * @return The current {@link Session} in use by {@link RemoteWorkManagerClient}.
     */
    public @Nullable Session getCurrentSession() {
        return mSession;
    }

    /**
     * @return the {@link SessionTracker} instance.
     */
    public @NonNull SessionTracker getSessionTracker() {
        return mSessionTracker;
    }

    /**
     * @return The {@link Object} session lock.
     */
    public @NonNull Object getSessionLock() {
        return mLock;
    }

    /**
     * @return The background {@link Executor} used by {@link RemoteWorkManagerClient}.
     */
    public @NonNull Executor getExecutor() {
        return mExecutor;
    }

    /**
     * @return The session index.
     */
    public long getSessionIndex() {
        return mSessionIndex;
    }

    @VisibleForTesting
    @NonNull ListenableFuture<byte[]> execute(
            final @NonNull ListenableFuture<IWorkManagerImpl> session,
            final @NonNull RemoteDispatcher<IWorkManagerImpl> dispatcher) {
        session.addListener(() -> {
            try {
                session.get();
            } catch (ExecutionException | InterruptedException exception) {
                cleanUp();
            }
        }, mExecutor);
        ListenableFuture<byte[]> future = RemoteExecuteKt.execute(mExecutor, session,
                dispatcher);
        future.addListener(() -> {
            SessionTracker tracker = getSessionTracker();
            // Start tracking for session timeout.
            // These callbacks are removed when the session timeout has expired or when getSession()
            // is called.
            mRunnableScheduler.scheduleWithDelay(getSessionTimeout(), tracker);
        }, mExecutor);
        return future;
    }

    @VisibleForTesting
    @NonNull ListenableFuture<IWorkManagerImpl> getSession(@NonNull Intent intent) {
        synchronized (mLock) {
            mSessionIndex += 1;
            if (mSession == null) {
                Logger.get().debug(TAG, "Creating a new session");
                mSession = new Session(this);
                try {
                    boolean bound = mContext.bindService(intent, mSession, BIND_AUTO_CREATE);
                    if (!bound) {
                        unableToBind(mSession, new RuntimeException("Unable to bind to service"));
                    }
                } catch (Throwable throwable) {
                    unableToBind(mSession, throwable);
                }
            }
            // Reset session tracker.
            mRunnableScheduler.cancel(mSessionTracker);
            return mSession.mFuture;
        }
    }

    /**
     * Cleans up a session. This could happen when we are unable to bind to the service or
     * we get disconnected.
     */
    public void cleanUp() {
        synchronized (mLock) {
            Logger.get().debug(TAG, "Cleaning up.");
            mSession = null;
        }
    }

    private void unableToBind(@NonNull Session session, @NonNull Throwable throwable) {
        Logger.get().error(TAG, "Unable to bind to service", throwable);
        session.mFuture.setException(throwable);
    }

    /**
     * @return the intent that is used to bind to the instance of {@link IWorkManagerImpl}.
     */
    private static Intent newIntent(@NonNull Context context) {
        return new Intent(context, RemoteWorkManagerService.class);
    }

    /**
     * The implementation of {@link ServiceConnection} that handles changes in the connection.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public static class Session implements ServiceConnection {
        private static final String TAG = Logger.tagWithPrefix("RemoteWMgr.Connection");

        final SettableFuture<IWorkManagerImpl> mFuture;
        final RemoteWorkManagerClient mClient;

        public Session(@NonNull RemoteWorkManagerClient client) {
            mClient = client;
            mFuture = SettableFuture.create();
        }

        @Override
        public void onServiceConnected(
                @NonNull ComponentName componentName,
                @NonNull IBinder iBinder) {
            Logger.get().debug(TAG, "Service connected");
            IWorkManagerImpl iWorkManagerImpl = IWorkManagerImpl.Stub.asInterface(iBinder);
            mFuture.set(iWorkManagerImpl);
        }

        @Override
        public void onServiceDisconnected(@NonNull ComponentName componentName) {
            Logger.get().debug(TAG, "Service disconnected");
            mFuture.setException(new RuntimeException("Service disconnected"));
            mClient.cleanUp();
        }

        @Override
        public void onBindingDied(@NonNull ComponentName name) {
            onBindingDied();
        }

        /**
         * Clean-up client when a binding dies.
         */
        public void onBindingDied() {
            Logger.get().debug(TAG, "Binding died");
            mFuture.setException(new RuntimeException("Binding died"));
            mClient.cleanUp();
        }

        @Override
        public void onNullBinding(@NonNull ComponentName name) {
            Logger.get().error(TAG, "Unable to bind to service");
            mFuture.setException(
                    new RuntimeException("Cannot bind to service " + name));
        }
    }

    /**
     * A {@link Runnable} that enforces a TTL for a {@link RemoteWorkManagerClient} session.
     */
    public static class SessionTracker implements Runnable {
        private static final String TAG = Logger.tagWithPrefix("SessionHandler");
        private final RemoteWorkManagerClient mClient;

        public SessionTracker(@NonNull RemoteWorkManagerClient client) {
            mClient = client;
        }

        @Override
        public void run() {
            final long preLockIndex = mClient.getSessionIndex();
            synchronized (mClient.getSessionLock()) {
                final long sessionIndex = mClient.getSessionIndex();
                final Session currentSession = mClient.getCurrentSession();
                // We check for a session index here. This is because if the index changes
                // while we acquire a lock, that would mean that a new session request came through.
                if (currentSession != null) {
                    if (preLockIndex == sessionIndex) {
                        Logger.get().debug(TAG, "Unbinding service");
                        mClient.getContext().unbindService(currentSession);
                        // Cleanup as well.
                        currentSession.onBindingDied();
                    } else {
                        Logger.get().debug(TAG, "Ignoring request to unbind.");
                    }
                }
            }
        }
    }
}
