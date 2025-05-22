/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.camera.testing.impl;

import static android.hardware.camera2.CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_BACKWARD_COMPATIBLE;

import static org.junit.Assume.assumeTrue;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.OutputConfiguration;
import android.media.CamcorderProfile;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.view.Surface;

import androidx.annotation.GuardedBy;
import androidx.annotation.IntDef;
import androidx.annotation.OptIn;
import androidx.annotation.RequiresApi;
import androidx.annotation.RequiresPermission;
import androidx.annotation.VisibleForTesting;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.CameraX;
import androidx.camera.core.CameraXConfig;
import androidx.camera.core.CompositionSettings;
import androidx.camera.core.ExperimentalLensFacing;
import androidx.camera.core.ExperimentalRetryPolicy;
import androidx.camera.core.Logger;
import androidx.camera.core.RetryPolicy;
import androidx.camera.core.UseCase;
import androidx.camera.core.concurrent.CameraCoordinator;
import androidx.camera.core.impl.AdapterCameraInfo;
import androidx.camera.core.impl.CameraConfig;
import androidx.camera.core.impl.CameraConfigs;
import androidx.camera.core.impl.CameraInternal;
import androidx.camera.core.impl.utils.futures.Futures;
import androidx.camera.core.internal.CameraUseCaseAdapter;
import androidx.camera.core.internal.StreamSpecsCalculator;
import androidx.camera.core.internal.StreamSpecsCalculatorImpl;
import androidx.camera.testing.impl.fakes.FakeCameraCoordinator;
import androidx.concurrent.futures.CallbackToFutureAdapter;
import androidx.concurrent.futures.CallbackToFutureAdapter.Completer;
import androidx.core.util.Preconditions;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.GrantPermissionRule;

import com.google.common.util.concurrent.ListenableFuture;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.junit.AssumptionViolatedException;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

/** Utility functions for obtaining instances of camera2 classes. */
public final class CameraUtil {
    private CameraUtil() {
    }

    private static final String LOG_TAG = "CameraUtil";

    /** Amount of time to wait before timing out when trying to open a {@link CameraDevice}. */
    private static final int CAMERA_OPEN_TIMEOUT_SECONDS = 2;

    /** The device debug property key for the tests to enable the camera pretest. */
    private static final String PRETEST_CAMERA_TAG = "PreTestCamera";

    /**
     * Gets a new instance of a {@link CameraDevice}.
     *
     * <p>This method attempts to open up a new camera. Since the camera api is asynchronous it
     * needs to wait for camera open
     *
     * <p>After the camera is no longer needed {@link #releaseCameraDevice(CameraDeviceHolder)}
     * should be called to clean up resources.
     *
     * @throws CameraAccessException if the device is unable to access the camera
     * @throws InterruptedException  if a {@link CameraDevice} can not be retrieved within a set
     *                               time
     */
    @RequiresPermission(Manifest.permission.CAMERA)
    public static @NonNull CameraDeviceHolder getCameraDevice(
            CameraDevice.@Nullable StateCallback stateCallback)
            throws CameraAccessException, InterruptedException, TimeoutException,
            ExecutionException {
        // Use the first camera available.
        List<String> cameraIds = getBackwardCompatibleCameraIdListOrThrow();
        if (cameraIds.isEmpty()) {
            throw new CameraAccessException(
                    CameraAccessException.CAMERA_ERROR, "Device contains no cameras.");
        }
        String cameraName = cameraIds.get(0);

        return new CameraDeviceHolder(getCameraManager(), cameraName, stateCallback);
    }

    /**
     * Gets a new instance of a {@link CameraDevice} by given camera id.
     *
     * <p>This method attempts to open up a new camera. Since the camera api is asynchronous it
     * needs to wait for camera open
     *
     * <p>After the camera is no longer needed {@link #releaseCameraDevice(CameraDeviceHolder)}
     * should be called to clean up resources.
     *
     * @throws InterruptedException  if a {@link CameraDevice} can not be retrieved within a set
     *                               time
     */
    @RequiresPermission(Manifest.permission.CAMERA)
    public static @NonNull CameraDeviceHolder getCameraDevice(
            @NonNull String cameraId,
            CameraDevice.@Nullable StateCallback stateCallback)
            throws InterruptedException, TimeoutException,
            ExecutionException {
        return new CameraDeviceHolder(getCameraManager(), cameraId, stateCallback);
    }

    /**
     * Returns physical camera ids of the specified camera id.
     */
    public static @NonNull List<String> getPhysicalCameraIds(@NonNull String cameraId) {
        try {
            if (Build.VERSION.SDK_INT >= 28) {
                return Collections.unmodifiableList(new ArrayList<>(Api28Impl.getPhysicalCameraId(
                        getCameraManager().getCameraCharacteristics(cameraId))));
            } else {
                return Collections.emptyList();
            }

        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    @RequiresApi(28)
    private static class Api28Impl {
        static Set<String> getPhysicalCameraId(CameraCharacteristics cameraCharacteristics) {
            return cameraCharacteristics.getPhysicalCameraIds();
        }
    }

    /**
     * A container class used to hold a {@link CameraDevice}.
     *
     * <p>This class should contain a valid {@link CameraDevice} that can be retrieved with
     * {@link #get()}, unless the device has been closed.
     *
     * <p>The camera device should always be closed with
     * {@link CameraUtil#releaseCameraDevice(CameraDeviceHolder)} once finished with the device.
     */
    public static class CameraDeviceHolder {

        final Object mLock = new Object();

        @GuardedBy("mLock")
        CameraDevice mCameraDevice;
        final HandlerThread mHandlerThread;
        final Handler mHandler;
        private ListenableFuture<Void> mCloseFuture;
        CameraCaptureSessionHolder mCameraCaptureSessionHolder;

        @RequiresPermission(Manifest.permission.CAMERA)
        CameraDeviceHolder(@NonNull CameraManager cameraManager, @NonNull String cameraId,
                CameraDevice.@Nullable StateCallback stateCallback)
                throws InterruptedException, ExecutionException, TimeoutException {
            mHandlerThread = new HandlerThread(String.format("CameraThread-%s", cameraId));
            mHandlerThread.start();
            mHandler = new Handler(mHandlerThread.getLooper());

            ListenableFuture<Void> cameraOpenFuture = openCamera(cameraManager, cameraId,
                    stateCallback);

            // Wait for the open future to complete before continuing.
            cameraOpenFuture.get(CAMERA_OPEN_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        }

        @RequiresPermission(Manifest.permission.CAMERA)
        // Should only be called once during initialization.
        private ListenableFuture<Void> openCamera(@NonNull CameraManager cameraManager,
                @NonNull String cameraId,
                CameraDevice.@Nullable StateCallback extraStateCallback) {
            return CallbackToFutureAdapter.getFuture(openCompleter -> {
                mCloseFuture = CallbackToFutureAdapter.getFuture(closeCompleter -> {
                    cameraManager.openCamera(cameraId,
                            new DeviceStateCallbackImpl(openCompleter, closeCompleter,
                                    extraStateCallback), mHandler);
                    return "Close[cameraId=" + cameraId + "]";
                });
                return "Open[cameraId=" + cameraId + "]";
            });
        }

        final class DeviceStateCallbackImpl extends CameraDevice.StateCallback {

            private final CallbackToFutureAdapter.Completer<Void> mOpenCompleter;
            private final CallbackToFutureAdapter.Completer<Void> mCloseCompleter;
            private final CameraDevice.@Nullable StateCallback mExtraStateCallback;

            DeviceStateCallbackImpl(
                    CallbackToFutureAdapter.@NonNull Completer<Void> openCompleter,
                    CallbackToFutureAdapter.@NonNull Completer<Void> closeCompleter,
                    CameraDevice.@Nullable StateCallback extraStateCallback) {
                mOpenCompleter = openCompleter;
                mCloseCompleter = closeCompleter;
                mExtraStateCallback = extraStateCallback;
            }

            @Override
            public void onOpened(@NonNull CameraDevice cameraDevice) {
                synchronized (mLock) {
                    Preconditions.checkState(mCameraDevice == null, "CameraDevice "
                            + "should not have been opened yet.");
                    mCameraDevice = cameraDevice;
                }
                if (mExtraStateCallback != null) {
                    mExtraStateCallback.onOpened(cameraDevice);
                }
                mOpenCompleter.set(null);
            }

            @Override
            public void onClosed(@NonNull CameraDevice cameraDevice) {
                if (mExtraStateCallback != null) {
                    mExtraStateCallback.onClosed(cameraDevice);
                }
                mCloseCompleter.set(null);
                mHandlerThread.quitSafely();
            }

            @Override
            public void onDisconnected(@NonNull CameraDevice cameraDevice) {
                synchronized (mLock) {
                    mCameraDevice = null;
                    mCameraCaptureSessionHolder = null;
                }
                if (mExtraStateCallback != null) {
                    mExtraStateCallback.onDisconnected(cameraDevice);
                }
                cameraDevice.close();
            }

            @Override
            public void onError(@NonNull CameraDevice cameraDevice, int i) {
                boolean notifyOpenFailed = false;
                synchronized (mLock) {
                    if (mCameraDevice == null) {
                        notifyOpenFailed = true;
                    } else {
                        mCameraDevice = null;
                        mCameraCaptureSessionHolder = null;
                    }
                }
                if (mExtraStateCallback != null) {
                    mExtraStateCallback.onError(cameraDevice, i);
                }

                if (notifyOpenFailed) {
                    mOpenCompleter.setException(new RuntimeException("Failed to "
                            + "open camera device due to error code: " + i));
                }
                cameraDevice.close();

            }
        }

        /**
         * Blocks until the camera device has been closed.
         */
        void close() throws ExecutionException, InterruptedException, TimeoutException {
            CameraDevice cameraDevice;
            synchronized (mLock) {
                cameraDevice = mCameraDevice;
                mCameraDevice = null;
                mCameraCaptureSessionHolder = null;
            }

            if (cameraDevice != null) {
                cameraDevice.close();
            }

            mCloseFuture.get(10L, TimeUnit.SECONDS);
        }

        /**
         * Returns a ListenableFuture representing the closed state.
         */
        public @NonNull ListenableFuture<Void> getClosedFuture() {
            return Futures.nonCancellationPropagating(mCloseFuture);
        }

        /**
         * Returns the camera device if it opened successfully and has not been closed.
         */
        public @Nullable CameraDevice get() {
            synchronized (mLock) {
                return mCameraDevice;
            }
        }

        /**
         * Create a {@link CameraCaptureSession} by the hold CameraDevice
         *
         * @param surfaces the surfaces used to create CameraCaptureSession
         * @return the CameraCaptureSession holder
         */
        public @NonNull CameraCaptureSessionHolder createCaptureSession(
                @NonNull List<Surface> surfaces)
                throws ExecutionException, InterruptedException, TimeoutException {
            synchronized (mLock) {
                Preconditions.checkState(mCameraDevice != null, "Camera is closed.");
            }
            if (mCameraCaptureSessionHolder != null) {
                mCameraCaptureSessionHolder.close();
                mCameraCaptureSessionHolder = null;
            }
            mCameraCaptureSessionHolder = CameraCaptureSessionHolder.create(this, surfaces, null);
            return mCameraCaptureSessionHolder;
        }

        /**
         * Create a {@link CameraCaptureSession} by the hold CameraDevice
         *
         * @param outputConfigurations the outputConfigurations used to create CameraCaptureSession
         * @return the CameraCaptureSession holder
         */
        @RequiresApi(24)
        public @NonNull CameraCaptureSessionHolder createCaptureSessionByOutputConfigurations(
                @NonNull List<OutputConfiguration> outputConfigurations)
                throws ExecutionException, InterruptedException, TimeoutException {
            synchronized (mLock) {
                Preconditions.checkState(mCameraDevice != null, "Camera is closed.");
            }
            if (mCameraCaptureSessionHolder != null) {
                mCameraCaptureSessionHolder.close();
                mCameraCaptureSessionHolder = null;
            }
            mCameraCaptureSessionHolder = CameraCaptureSessionHolder.createByOutputConfigurations(
                    this, outputConfigurations, null);
            return mCameraCaptureSessionHolder;
        }
    }

    /**
     * A container class used to hold a {@link CameraCaptureSession}.
     *
     * <p>This class contains a valid {@link CameraCaptureSession} that can be retrieved with
     * {@link #get()}, unless the session has been closed.
     *
     * <p>The instance can be obtained via {@link CameraDeviceHolder#createCaptureSession}
     * and will be closed by creating another CameraCaptureSessionHolder. The latest instance will
     * be closed when the associated CameraDeviceHolder is released by
     * {@link CameraUtil#releaseCameraDevice(CameraDeviceHolder)}.
     */
    public static class CameraCaptureSessionHolder {

        private final CameraDeviceHolder mCameraDeviceHolder;
        private CameraCaptureSession mCameraCaptureSession;
        private ListenableFuture<Void> mCloseFuture;

        static @NonNull CameraCaptureSessionHolder create(
                @NonNull CameraDeviceHolder cameraDeviceHolder, @NonNull List<Surface> surfaces,
                CameraCaptureSession.@Nullable StateCallback stateCallback
        ) throws ExecutionException, InterruptedException, TimeoutException {
            return new CameraCaptureSessionHolder(cameraDeviceHolder, surfaces, stateCallback);
        }

        @RequiresApi(24)
        static @NonNull CameraCaptureSessionHolder createByOutputConfigurations(
                @NonNull CameraDeviceHolder cameraDeviceHolder,
                @NonNull List<OutputConfiguration> outputConfigurations,
                CameraCaptureSession.@Nullable StateCallback stateCallback
        ) throws ExecutionException, InterruptedException, TimeoutException {
            return new CameraCaptureSessionHolder(cameraDeviceHolder, outputConfigurations,
                    stateCallback);
        }

        private CameraCaptureSessionHolder(@NonNull CameraDeviceHolder cameraDeviceHolder,
                @NonNull Object paramToCreateSession,
                CameraCaptureSession.@Nullable StateCallback stateCallback
        ) throws ExecutionException, InterruptedException, TimeoutException {
            mCameraDeviceHolder = cameraDeviceHolder;
            CameraDevice cameraDevice = Preconditions.checkNotNull(cameraDeviceHolder.get());
            ListenableFuture<CameraCaptureSession> openFuture = openCaptureSession(cameraDevice,
                    paramToCreateSession, stateCallback, cameraDeviceHolder.mHandler);

            mCameraCaptureSession = openFuture.get(5, TimeUnit.SECONDS);
        }

        @SuppressWarnings({"deprecation", "newApi", "unchecked"})
        private @NonNull ListenableFuture<CameraCaptureSession> openCaptureSession(
                @NonNull CameraDevice cameraDevice,
                @NonNull Object paramToCreateSession,
                CameraCaptureSession.@Nullable StateCallback stateCallback,
                @NonNull Handler handler) {
            return CallbackToFutureAdapter.getFuture(
                    openCompleter -> {
                        mCloseFuture = CallbackToFutureAdapter.getFuture(
                                closeCompleter -> {
                                    if (isOutputConfigurationList(paramToCreateSession)) {
                                        cameraDevice.createCaptureSessionByOutputConfigurations(
                                                (List<OutputConfiguration>) paramToCreateSession,
                                                new SessionStateCallbackImpl(
                                                        openCompleter, closeCompleter,
                                                        stateCallback),
                                                handler);
                                    } else {
                                        cameraDevice.createCaptureSession(
                                                (List<Surface>) paramToCreateSession,
                                                new SessionStateCallbackImpl(
                                                        openCompleter, closeCompleter,
                                                        stateCallback),
                                                handler);
                                    }
                                    return "Close CameraCaptureSession";
                                });
                        return "Open CameraCaptureSession";
                    });
        }

        private static boolean isOutputConfigurationList(@NonNull Object param) {
            List<?> list;
            return Build.VERSION.SDK_INT >= 24 && param instanceof List
                    && !(list = (List<?>) param).isEmpty()
                    && OutputConfiguration.class.isInstance(list.get(0));
        }

        void close() throws ExecutionException, InterruptedException, TimeoutException {
            if (mCameraCaptureSession != null) {
                mCameraCaptureSession.close();
                mCameraCaptureSession = null;
            }
            mCloseFuture.get(10L, TimeUnit.SECONDS);
        }

        /**
         * A simplified method to start a repeating capture request.
         *
         * <p>For advance usage, use {@link #get} to obtain the CameraCaptureSession and then issue
         * repeating request.
         *
         * @param template one of the {@link CameraDevice} template.
         * @param surfaces the surfaces add to the repeating request
         * @param captureParams the pairs of {@link CaptureRequest.Key} and value
         * @param captureCallback the capture callback
         * @throws CameraAccessException if fail to issue the request
         */
        @SuppressWarnings("unchecked") // Cast to CaptureRequest.Key<Object>
        public void startRepeating(int template, @NonNull List<Surface> surfaces,
                @Nullable Map<CaptureRequest.Key<?>, Object> captureParams,
                CameraCaptureSession.@Nullable CaptureCallback captureCallback)
                throws CameraAccessException {
            checkSessionOrThrow();
            CameraDevice cameraDevice = mCameraDeviceHolder.get();
            Preconditions.checkState(cameraDevice != null, "CameraDevice is closed.");
            CaptureRequest.Builder builder = cameraDevice.createCaptureRequest(template);
            for (Surface surface : surfaces) {
                builder.addTarget(surface);
            }
            if (captureParams != null) {
                for (Map.Entry<CaptureRequest.Key<?>, Object> entry : captureParams.entrySet()) {
                    builder.set((CaptureRequest.Key<Object>) entry.getKey(), entry.getValue());
                }
            }
            mCameraCaptureSession.setRepeatingRequest(builder.build(), captureCallback,
                    mCameraDeviceHolder.mHandler);
        }

        /**
         * Returns the camera capture session if it opened successfully and has not been closed.
         *
         * @throws IllegalStateException if the camera capture session is closed
         */
        public @NonNull CameraCaptureSession get() {
            checkSessionOrThrow();
            return mCameraCaptureSession;
        }

        private void checkSessionOrThrow() {
            Preconditions.checkState(mCameraCaptureSession != null,
                    "CameraCaptureSession is closed");
        }

        private static class SessionStateCallbackImpl extends
                CameraCaptureSession.StateCallback {
            private final Completer<CameraCaptureSession> mOpenCompleter;
            private final CallbackToFutureAdapter.Completer<Void> mCloseCompleter;
            private final CameraCaptureSession.@Nullable StateCallback mExtraStateCallback;

            SessionStateCallbackImpl(
                    @NonNull Completer<CameraCaptureSession> openCompleter,
                    @NonNull Completer<Void> closeCompleter,
                    CameraCaptureSession.@Nullable StateCallback extraStateCallback) {
                mOpenCompleter = openCompleter;
                mCloseCompleter = closeCompleter;
                mExtraStateCallback = extraStateCallback;
            }

            @Override
            public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                if (mExtraStateCallback != null) {
                    mExtraStateCallback.onConfigured(cameraCaptureSession);
                }
                mOpenCompleter.set(cameraCaptureSession);
            }

            @Override
            public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                if (mExtraStateCallback != null) {
                    mExtraStateCallback.onConfigureFailed(cameraCaptureSession);
                }
                mOpenCompleter.setException(new RuntimeException("Failed to "
                        + "open CameraCaptureSession"));
                mCloseCompleter.set(null);
            }

            @Override
            public void onClosed(@NonNull CameraCaptureSession session) {
                if (mExtraStateCallback != null) {
                    mExtraStateCallback.onClosed(session);
                }
                mCloseCompleter.set(null);
            }
        }
    }

    /**
     * Cleans up resources that need to be kept around while the camera device is active.
     *
     * @param cameraDeviceHolder camera that was obtained via
     *                           {@link #getCameraDevice(CameraDevice.StateCallback)}
     */
    public static void releaseCameraDevice(@NonNull CameraDeviceHolder cameraDeviceHolder)
            throws ExecutionException, InterruptedException, TimeoutException {
        cameraDeviceHolder.close();
    }

    public static @NonNull CameraManager getCameraManager() {
        return (CameraManager)
                ApplicationProvider.getApplicationContext()
                        .getSystemService(Context.CAMERA_SERVICE);
    }


    /**
     * Creates the CameraUseCaseAdapter that would be created with the given CameraSelector.
     *
     * <p>This requires that {@link CameraXUtil#initialize(Context, CameraXConfig)} has been called
     * to properly initialize the cameras. {@link CameraXUtil#shutdown()} also needs to be
     * properly called by the caller class to release the created {@link CameraX} instance.
     *
     * <p>A new CameraUseCaseAdapter instance will be created every time this method is called.
     * UseCases previously attached to CameraUseCasesAdapters returned by this method or
     * {@link #createCameraAndAttachUseCase(Context, CameraSelector, UseCase...)}
     * will not be attached to the new CameraUseCaseAdapter returned by this method.
     *
     * @param context        The context used to initialize CameraX
     * @param cameraCoordinator The camera coordinator for concurrent cameras.
     * @param cameraSelector The selector to select cameras with.
     */
    @SuppressLint("NullAnnotationGroup")
    @OptIn(markerClass = ExperimentalRetryPolicy.class)
    @VisibleForTesting
    public static @NonNull CameraUseCaseAdapter createCameraUseCaseAdapter(
            @NonNull Context context,
            @NonNull CameraCoordinator cameraCoordinator,
            @NonNull CameraSelector cameraSelector,
            @NonNull CameraConfig cameraConfig) {
        try {
            CameraX cameraX = CameraXUtil.getOrCreateInstance(context, null).get(
                    RetryPolicy.getDefaultRetryTimeoutInMillis() + 2000, TimeUnit.MILLISECONDS);
            CameraInternal camera =
                    cameraSelector.select(cameraX.getCameraRepository().getCameras());
            StreamSpecsCalculator streamSpecsCalculator = new StreamSpecsCalculatorImpl(
                    cameraX.getDefaultConfigFactory(), cameraX.getCameraDeviceSurfaceManager());
            return new CameraUseCaseAdapter(camera,
                    null,
                    new AdapterCameraInfo(camera.getCameraInfoInternal(), cameraConfig),
                    null,
                    CompositionSettings.DEFAULT,
                    CompositionSettings.DEFAULT,
                    cameraCoordinator,
                    streamSpecsCalculator,
                    cameraX.getDefaultConfigFactory());
        } catch (ExecutionException | InterruptedException | TimeoutException e) {
            throw new RuntimeException("Unable to retrieve CameraX instance");
        }
    }

    /**
     * Creates the CameraUseCaseAdapter that would be created with the given CameraSelector.
     *
     * <p>This requires that {@link CameraXUtil#initialize(Context, CameraXConfig)} has been called
     * to properly initialize the cameras. {@link CameraXUtil#shutdown()} also needs to be
     * properly called by the caller class to release the created {@link CameraX} instance.
     *
     * <p>A new CameraUseCaseAdapter instance will be created every time this method is called.
     * UseCases previously attached to CameraUseCasesAdapters returned by this method or
     * {@link #createCameraAndAttachUseCase(Context, CameraSelector, UseCase...)}
     * will not be attached to the new CameraUseCaseAdapter returned by this method.
     *
     * @param context        The context used to initialize CameraX
     * @param cameraSelector The selector to select cameras with.
     */
    @VisibleForTesting
    public static @NonNull CameraUseCaseAdapter createCameraUseCaseAdapter(
            @NonNull Context context,
            @NonNull CameraSelector cameraSelector) {
        return createCameraUseCaseAdapter(context, new FakeCameraCoordinator(),
                cameraSelector, CameraConfigs.defaultConfig());
    }

    /**
     * Creates the CameraUseCaseAdapter that would be created with the given CameraSelector and
     * CameraConfig
     */
    @VisibleForTesting
    public static @NonNull CameraUseCaseAdapter createCameraUseCaseAdapter(
            @NonNull Context context,
            @NonNull CameraSelector cameraSelector,
            @NonNull CameraConfig cameraConfig) {
        return createCameraUseCaseAdapter(context, new FakeCameraCoordinator(),
                cameraSelector, cameraConfig);
    }

    /**
     * Creates the CameraUseCaseAdapter that would be created with the given CameraSelector and
     * attaches the UseCases.
     *
     * <p>This requires that {@link CameraXUtil#initialize(Context, CameraXConfig)} has been called
     * to properly initialize the cameras. {@link CameraXUtil#shutdown()} also needs to be
     * properly called by the caller class to release the created {@link CameraX} instance.
     *
     * <p>A new CameraUseCaseAdapter instance will be created every time this method is called.
     * UseCases previously attached to CameraUseCasesAdapters returned by this method or
     * {@link #createCameraUseCaseAdapter(Context, CameraSelector)} will not be
     * attached to the new CameraUseCaseAdapter returned by this method.
     *
     * @param context        The context used to initialize CameraX
     * @param cameraSelector The selector to select cameras with.
     * @param useCases       The UseCases to attach to the CameraUseCaseAdapter.
     */
    @VisibleForTesting
    public static @NonNull CameraUseCaseAdapter createCameraAndAttachUseCase(
            @NonNull Context context,
            @NonNull CameraSelector cameraSelector,
            UseCase @NonNull ... useCases) {
        CameraUseCaseAdapter cameraUseCaseAdapter = createCameraUseCaseAdapter(context,
                cameraSelector);

        // TODO(b/160249108) move off of main thread once UseCases can be attached on any
        //  thread
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            try {
                cameraUseCaseAdapter.addUseCases(Arrays.asList(useCases));
            } catch (CameraUseCaseAdapter.CameraException e) {
                throw new IllegalArgumentException("Unable to attach use cases to camera.", e);
            }
        });

        return cameraUseCaseAdapter;
    }

    /**
     * Check if there is any camera in the device.
     *
     * <p>If there is no camera in the device, most tests will failed.
     *
     * @return false if no camera
     */
    @SuppressWarnings("deprecation")
    public static boolean deviceHasCamera() {
        // TODO Think about external camera case,
        //  especially no built in camera but there might be some external camera

        // It also could be checked by PackageManager's hasSystemFeature() with following:
        //     FEATURE_CAMERA, FEATURE_CAMERA_FRONT, FEATURE_CAMERA_ANY.
        // But its needed to consider one case that platform build with camera feature but there is
        // no built in camera or external camera.

        int numberOfCamera = 0;
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
            try {
                numberOfCamera = getBackwardCompatibleCameraIdListOrThrow().size();
            } catch (IllegalStateException e) {
                Logger.e(LOG_TAG, "Unable to check camera availability.", e);
            } catch (IllegalArgumentException e) {
                Logger.e(LOG_TAG,
                        "Unable to access camera device. This could be due to a Camera HAL issue "
                                + "or an incorrect device ID.",
                        e);
            }
        } else {
            numberOfCamera = android.hardware.Camera.getNumberOfCameras();
        }

        return numberOfCamera > 0;
    }

    /**
     * Check if the specified lensFacing is supported by the device.
     *
     * @param lensFacing The desired camera lensFacing.
     * @return True if the device supports the lensFacing.
     * @throws IllegalStateException if the CAMERA permission is not currently granted.
     */
    public static boolean hasCameraWithLensFacing(@CameraSelector.LensFacing int lensFacing) {
        return getCameraCharacteristics(lensFacing) != null;
    }

    /**
     * Retrieves a list of {@link CameraSelector} instances corresponding to available physical
     * cameras on the device (back, front, and external).
     *
     * <p>This method checks for the presence of default back-facing, default front-facing,
     * and any external-facing cameras. For each available lens facing direction, a corresponding
     * {@link CameraSelector} is added to the returned list.
     *
     * <p>Note: Accessing {@link CameraSelector#LENS_FACING_EXTERNAL} requires opting in to
     * {@link ExperimentalLensFacing}. This method itself is annotated with
     * {@code @OptIn(markerClass = ExperimentalLensFacing.class)}.
     *
     * @return A {@link List} of {@link CameraSelector}s for available cameras. The list will
     * be empty if no back, front, or external cameras are detected. It will contain
     * {@link CameraSelector#DEFAULT_BACK_CAMERA} if a back camera is present,
     * {@link CameraSelector#DEFAULT_FRONT_CAMERA} if a front camera is present,
     * and a custom {@code CameraSelector} requiring {@link CameraSelector#LENS_FACING_EXTERNAL}
     * if an external camera is present.
     */
    @OptIn(markerClass = ExperimentalLensFacing.class)
    public static @NonNull List<CameraSelector> getAvailableCameraSelectors() {
        ArrayList<CameraSelector> list = new ArrayList<>();
        if (hasCameraWithLensFacing(CameraSelector.LENS_FACING_BACK)) {
            list.add(CameraSelector.DEFAULT_BACK_CAMERA);
        }

        if (hasCameraWithLensFacing(CameraSelector.LENS_FACING_FRONT)) {
            list.add(CameraSelector.DEFAULT_FRONT_CAMERA);
        }

        if (hasCameraWithLensFacing(CameraSelector.LENS_FACING_EXTERNAL)) {
            list.add(new CameraSelector.Builder().requireLensFacing(
                    CameraSelector.LENS_FACING_EXTERNAL).build());
        }

        return list;
    }

    /**
     * Assumes and returns the first available {@link CameraSelector}.
     *
     * <p>This method retrieves a list of available camera selectors using
     * {@link #getAvailableCameraSelectors()}. If the list is empty, indicating no
     * available cameras, it throws an {@link AssumptionViolatedException}.
     * Otherwise, it returns the first {@link CameraSelector} from the list.
     *
     * @return A {@link NonNull} {@link CameraSelector} representing the first available camera.
     * @throws AssumptionViolatedException if no cameras are available to be selected.
     */
    public static @NonNull CameraSelector assumeFirstAvailableCameraSelector() {
        List<CameraSelector> cameraSelectors = getAvailableCameraSelectors();

        if (cameraSelectors.isEmpty()) {
            throw new AssumptionViolatedException("No available camera to test.");
        }

        return cameraSelectors.get(0);
    }

    /**
     * Check if the camera sensor in the native orientation({@link Surface.ROTATION_0}) is portrait
     * or not.
     *
     * @param lensFacing The desired camera lensFacing.
     * @return True if camera sensor is portrait in the native orientation.
     * @throws IllegalStateException if the CAMERA permission is not currently granted.
     */
    public static boolean isCameraSensorPortraitInNativeOrientation(
            @CameraSelector.LensFacing int lensFacing) {
        CameraCharacteristics cameraCharacteristics = getCameraCharacteristics(lensFacing);
        if (cameraCharacteristics == null) {
            return false;
        }
        int sensorOrientation =
                    cameraCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
        return (sensorOrientation == 90 || sensorOrientation == 270);
    }

    /**
     * Check if the aspect ratio needs to be corrected.
     *
     * @param lensFacing The desired camera lensFacing.
     * @return True if the aspect ratio has been corrected.
     * @throws IllegalStateException if the CAMERA permission is not currently granted.
     */
    public static boolean requiresCorrectedAspectRatio(@CameraSelector.LensFacing int lensFacing) {
        Integer hardwareLevelValue;
        CameraCharacteristics cameraCharacteristics = getCameraCharacteristics(lensFacing);
        if (cameraCharacteristics == null) {
            return false;
        }
        hardwareLevelValue = cameraCharacteristics.get(
                CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL);
        // There is a bug because of a flipped scaling factor in the intermediate texture
        // transform matrix, and it was fixed in L MR1. If the device is LEGACY + Android 5.0,
        // then auto resolution will return the same aspect ratio as maximum JPEG resolution.
        return (Build.VERSION.SDK_INT == 21 && hardwareLevelValue != null && hardwareLevelValue
                == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY);
    }

    /**
     * Gets the camera id of the first camera with the given lensFacing.
     *
     * @param lensFacing The desired camera lensFacing.
     * @return Camera id of the first camera with the given lensFacing, null if there's no camera
     * has the lensFacing.
     * @throws IllegalStateException if the CAMERA permission is not currently granted.
     */
    public static @Nullable String getCameraIdWithLensFacing(
            @CameraSelector.LensFacing int lensFacing) {
        @SupportedLensFacingInt
        int lensFacingInteger = getLensFacingIntFromEnum(lensFacing);
        for (String cameraId : getBackwardCompatibleCameraIdListOrThrow()) {
            CameraCharacteristics characteristics = getCameraCharacteristicsOrThrow(cameraId);
            Integer cameraLensFacing = characteristics.get(CameraCharacteristics.LENS_FACING);
            if (cameraLensFacing != null && cameraLensFacing == lensFacingInteger) {
                return cameraId;
            }
        }
        return null;
    }

    /**
     * Checks if the device has a flash unit with the specified lensFacing.
     *
     * @param lensFacing The desired camera lensFacing.
     * @return True if the device has flash unit with the specified LensFacing.
     * @throws IllegalStateException if the CAMERA permission is not currently granted.
     */
    public static boolean hasFlashUnitWithLensFacing(@CameraSelector.LensFacing int lensFacing) {
        @SupportedLensFacingInt
        int lensFacingInteger = getLensFacingIntFromEnum(lensFacing);
        for (String cameraId : getBackwardCompatibleCameraIdListOrThrow()) {
            CameraCharacteristics characteristics = getCameraCharacteristicsOrThrow(cameraId);
            Integer cameraLensFacing = characteristics.get(CameraCharacteristics.LENS_FACING);
            if (cameraLensFacing == null || cameraLensFacing != lensFacingInteger) {
                continue;
            }
            Boolean hasFlashUnit = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
            if (hasFlashUnit != null && hasFlashUnit) {
                return true;
            }
        }
        return false;
    }

    /**
     * Gets the {@link CameraCharacteristics} by specified lens facing if possible.
     *
     * @return the camera characteristics for the given lens facing or {@code null} if it can't
     * be retrieved.
     */
    public static @Nullable CameraCharacteristics getCameraCharacteristics(
            @CameraSelector.LensFacing int lensFacing) {
        @SupportedLensFacingInt
        int lensFacingInteger = getLensFacingIntFromEnum(lensFacing);
        for (String cameraId : getBackwardCompatibleCameraIdListOrThrow()) {
            CameraCharacteristics characteristics = getCameraCharacteristicsOrThrow(cameraId);
            Integer cameraLensFacing = characteristics.get(CameraCharacteristics.LENS_FACING);
            if (cameraLensFacing != null && cameraLensFacing == lensFacingInteger) {
                return characteristics;
            }
        }
        return null;
    }

    /**
     * Gets the {@link CameraCharacteristics} by specified camera id.
     *
     * @return the camera characteristics for the given camera id or {@code null} if it can't
     * be retrieved.
     */
    public static @Nullable CameraCharacteristics getCameraCharacteristics(
            @NonNull String cameraId) {
        try {
            return getCameraCharacteristicsOrThrow(cameraId);
        } catch (RuntimeException e) {
            Logger.e(LOG_TAG, "Unable to get CameraCharacteristics.", e);
            return null;
        }
    }

    /**
     * The current lens facing directions supported by CameraX, as defined the
     * {@link CameraMetadata}.
     */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({CameraMetadata.LENS_FACING_FRONT, CameraMetadata.LENS_FACING_BACK,
            CameraMetadata.LENS_FACING_EXTERNAL})
    @interface SupportedLensFacingInt {
    }


    /**
     * Converts a lens facing direction from a {@link CameraMetadata} integer to a lensFacing.
     *
     * @param lensFacingInteger The lens facing integer, as defined in {@link CameraMetadata}.
     * @return The lens facing enum.
     */
    @OptIn(markerClass = ExperimentalLensFacing.class)
    @CameraSelector.LensFacing
    public static int getLensFacingEnumFromInt(
            @SupportedLensFacingInt int lensFacingInteger) {
        switch (lensFacingInteger) {
            case CameraMetadata.LENS_FACING_BACK:
                return CameraSelector.LENS_FACING_BACK;
            case CameraMetadata.LENS_FACING_FRONT:
                return CameraSelector.LENS_FACING_FRONT;
            case CameraMetadata.LENS_FACING_EXTERNAL:
                return CameraSelector.LENS_FACING_EXTERNAL;
            default:
                throw new IllegalArgumentException(
                        "Unsupported lens facing integer: " + lensFacingInteger);
        }
    }

    /**
     * Gets if the sensor orientation of the given lens facing.
     *
     * @param lensFacing The desired camera lensFacing.
     * @return The sensor orientation degrees, or null if it's undefined.
     * @throws IllegalStateException if the CAMERA permission is not currently granted.
     */
    public static @Nullable Integer getSensorOrientation(
            @CameraSelector.LensFacing int lensFacing) {
        @SupportedLensFacingInt
        int lensFacingInteger = getLensFacingIntFromEnum(lensFacing);
        for (String cameraId : getBackwardCompatibleCameraIdListOrThrow()) {
            CameraCharacteristics characteristics = getCameraCharacteristicsOrThrow(cameraId);
            Integer cameraLensFacing = characteristics.get(CameraCharacteristics.LENS_FACING);
            if (cameraLensFacing == null || cameraLensFacing != lensFacingInteger) {
                continue;
            }
            return characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
        }
        return null;
    }

    /**
     * Gets the camera id list or throw exception if the CAMERA permission is not currently granted.
     *
     * @return the camera id list
     * @throws IllegalStateException if the CAMERA permission is not currently granted.
     */
    public static @NonNull List<String> getBackwardCompatibleCameraIdListOrThrow() {
        try {
            List<String> backwardCompatibleCameraIdList = new ArrayList<>();

            for (String cameraId : getCameraManager().getCameraIdList()) {
                int[] capabilities = getCameraCharacteristicsOrThrow(cameraId).get(
                        CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES);

                if (capabilities == null) {
                    continue;
                }

                for (int capability : capabilities) {
                    if (capability == REQUEST_AVAILABLE_CAPABILITIES_BACKWARD_COMPATIBLE) {
                        backwardCompatibleCameraIdList.add(cameraId);
                        break;
                    }
                }
            }

            return backwardCompatibleCameraIdList;
        } catch (CameraAccessException e) {
            throw new IllegalStateException("Unable to retrieve list of cameras on device.", e);
        }
    }

    /**
     * Converts a lens facing direction from a lensFacing to a {@link CameraMetadata} integer.
     *
     * @param lensFacing The lens facing enum, as defined in {@link CameraSelector}.
     * @return The lens facing integer.
     */
    @OptIn(markerClass = ExperimentalLensFacing.class)
    @SupportedLensFacingInt
    private static int getLensFacingIntFromEnum(@CameraSelector.LensFacing int lensFacing) {
        switch (lensFacing) {
            case CameraSelector.LENS_FACING_BACK:
                return CameraMetadata.LENS_FACING_BACK;
            case CameraSelector.LENS_FACING_FRONT:
                return CameraMetadata.LENS_FACING_FRONT;
            case CameraSelector.LENS_FACING_EXTERNAL:
                return CameraMetadata.LENS_FACING_EXTERNAL;
            default:
                throw new IllegalArgumentException("Unsupported lens facing enum: " + lensFacing);
        }
    }

    /**
     * Gets the {@link CameraCharacteristics} by specified camera id or throw exception if the
     * CAMERA permission is not currently granted.
     *
     * @return the camera id list
     * @throws IllegalStateException if the CAMERA permission is not currently granted.
     */
    private static @NonNull CameraCharacteristics getCameraCharacteristicsOrThrow(
            @NonNull String cameraId) {
        try {
            return getCameraManager().getCameraCharacteristics(cameraId);
        } catch (CameraAccessException e) {
            throw new IllegalStateException(
                    "Unable to retrieve info for camera with id " + cameraId + ".", e);
        }
    }

    /**
     * Check if the resource sufficient to recording a video.
     */
    public static @NonNull TestRule checkVideoRecordingResource() {
        return RuleChain.outerRule((base, description) -> new Statement() {
            @RequiresApi(api = Build.VERSION_CODES.M)
            @Override
            public void evaluate() throws Throwable {
                // The default resolution in VideoCapture is 1080P.
                assumeTrue(checkVideoRecordingResource(CamcorderProfile.QUALITY_1080P));
                base.evaluate();
            }
        });
    }

    /**
     * Check resource for video recording.
     *
     * <p> Tries to configure an video encoder to ensure current resource is sufficient to
     * recording a video.
     */
    @SuppressWarnings("deprecation")
    public static boolean checkVideoRecordingResource(int quality) {
        String videoMimeType = "video/avc";
        // Assume the device resource is sufficient.
        boolean checkResult = true;

        if (CamcorderProfile.hasProfile(quality)) {
            CamcorderProfile profile = CamcorderProfile.get(quality);
            MediaFormat format =
                    MediaFormat.createVideoFormat(
                            videoMimeType, profile.videoFrameWidth, profile.videoFrameHeight);
            format.setInteger(MediaFormat.KEY_COLOR_FORMAT,
                    MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
            format.setInteger(MediaFormat.KEY_BIT_RATE, profile.videoBitRate);
            format.setInteger(MediaFormat.KEY_FRAME_RATE, profile.videoFrameRate);
            format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1);

            MediaCodec codec = null;

            try {
                codec = MediaCodec.createEncoderByType(videoMimeType);
                codec.configure(
                        format, /*surface*/
                        null, /*crypto*/
                        null,
                        MediaCodec.CONFIGURE_FLAG_ENCODE);
            } catch (MediaCodec.CodecException e) {
                Logger.i(LOG_TAG,
                        "Video encoder pre-test configured fail CodecException: " + e.getMessage());
                // Skip tests if a video encoder cannot be configured successfully.
                checkResult = false;
            } catch (IOException | IllegalArgumentException | IllegalStateException e) {
                Logger.i(LOG_TAG, "Video encoder pre-test configured fail: " + e.getMessage());
                checkResult = false;
            } finally {
                Logger.i(LOG_TAG, "codec.release()");
                if (codec != null) {
                    codec.release();
                }
            }
        }

        return checkResult;
    }

    /**
     * Grant the camera permission and test the camera.
     *
     * <p>It will
     * (1) Grant the camera permission.
     * (2) Check if there is at least one camera on the device alive. If not, it will ignore
     * the test.
     * (3) Ensure the rear and front cameras (when device has them) can be opened successfully
     * before the test. If not, it will ignore the test.
     * (4) Ensure the default rear and front cameras (if device has them) are available again
     * after test.
     */
    public static @NonNull TestRule grantCameraPermissionAndPreTestAndPostTest() {
        return grantCameraPermissionAndPreTestAndPostTest(new PreTestCamera(),
                new PreTestCameraIdList(), new PostTestCameraAvailability(
                        Arrays.asList(CameraSelector.LENS_FACING_BACK,
                                CameraSelector.LENS_FACING_FRONT)));
    }

    /**
     * Grant the camera permission and test the camera.
     *
     * <p>It will
     * (1) Grant the camera permission.
     * (2) Check if there is at least one camera on the device alive. If not, it will ignore
     * the test.
     * (3) Ensure the rear and front cameras (when device has them) can be opened successfully
     * before the test. If not, it will ignore the test.
     * (4) Ensure default cameras for provided {@code lensFacings} (if device has them) are
     * available again after test.
     */
    public static @NonNull TestRule grantCameraPermissionAndPreTestAndPostTest(
            @NonNull List<Integer> lensFacings
    ) {
        return grantCameraPermissionAndPreTestAndPostTest(new PreTestCamera(),
                new PreTestCameraIdList(), new PostTestCameraAvailability(lensFacings));
    }

    /**
     * Grant the camera permission and test the camera.
     *
     * <p>This method is mainly required to be used when running the test with
     * Camera2Config/CameraPipeConfig. Please create a PreTestCameraIdList with the CameraXConfig
     * that is used in the test.
     * If the test uses fake CameraXConfig or doesn't initialize CameraX, i.e. doesn't uses
     * {@link androidx.camera.lifecycle.ProcessCameraProvider} or {@link CameraXUtil#initialize} to
     * initialize CameraX for testing, you can use
     * {@link CameraUtil#grantCameraPermissionAndPreTestAndPostTest()} instead.
     *
     * <p>It will
     * (1) Grant the camera permission.
     * (2) Check if there is at least one camera on the device alive. If not, it will ignore
     * the test. This is based on the {@link PreTestCameraIdList} parameter, which is intended to be
     * based on a {@link CameraXConfig} as mentioned earlier.
     * (3) Ensure the rear and front cameras (when device has them) can be opened successfully
     * before the test. If not, it will ignore the test.
     * (4) Ensure the default rear and front cameras (if device has them) are available again
     * after test.
     */
    public static @NonNull TestRule grantCameraPermissionAndPreTestAndPostTest(
            @Nullable PreTestCameraIdList cameraIdListTestRule) {

        return grantCameraPermissionAndPreTestAndPostTest(new PreTestCamera(), cameraIdListTestRule,
                new PostTestCameraAvailability(Arrays.asList(CameraSelector.LENS_FACING_BACK,
                        CameraSelector.LENS_FACING_FRONT)));
    }

    /**
     * Grant the camera permission and test the camera.
     *
     * <p>This method is mainly required to be used when running the test with
     * Camera2Config/CameraPipeConfig. Please create a PreTestCameraIdList with the CameraXConfig
     * that is used in the test.
     * If the test uses fake CameraXConfig or doesn't initialize CameraX, i.e. doesn't uses
     * {@link androidx.camera.lifecycle.ProcessCameraProvider} or {@link CameraXUtil#initialize} to
     * initialize CameraX for testing, you can use
     * {@link CameraUtil#grantCameraPermissionAndPreTestAndPostTest()} instead.
     *
     * <p>It will
     * (1) Grant the camera permission.
     * (2) Check if there is at least one camera on the device alive. If not, it will ignore
     * the test. This is based on the {@link PreTestCameraIdList} parameter, which is intended to be
     * based on a {@link CameraXConfig} as mentioned earlier.
     * (3) Ensure the rear and front cameras (when device has them) can be opened successfully
     * before the test. If not, it will ignore the test.
     * (4) Ensure default cameras for provided {@code lensFacings} (if device has them) are
     * available again after test.
     */
    public static @NonNull TestRule grantCameraPermissionAndPreTestAndPostTest(
            @Nullable PreTestCamera cameraTestRule,
            @Nullable PreTestCameraIdList cameraIdListTestRule) {
        return grantCameraPermissionAndPreTestAndPostTest(cameraTestRule, cameraIdListTestRule,
                new PostTestCameraAvailability(Arrays.asList(CameraSelector.LENS_FACING_BACK,
                        CameraSelector.LENS_FACING_FRONT)));
    }

    /**
     * Grant the camera permission and test the camera.
     *
     * <p>This method is mainly required to be used when running the test with
     * Camera2Config/CameraPipeConfig. Please create a PreTestCameraIdList with the CameraXConfig
     * that is used in the test.
     * If the test uses fake CameraXConfig or doesn't initialize CameraX, i.e. doesn't uses
     * {@link androidx.camera.lifecycle.ProcessCameraProvider} or {@link CameraXUtil#initialize} to
     * initialize CameraX for testing, you can use
     * {@link CameraUtil#grantCameraPermissionAndPreTestAndPostTest()} instead.
     *
     * <p>It will
     * (1) Grant the camera permission.
     * (2) Check if there is at least one camera on the device alive. If not, it will ignore
     * the test. This is based on the {@link PreTestCameraIdList} parameter, which is intended to be
     * based on a {@link CameraXConfig} as mentioned earlier.
     * (3) Ensure the rear and front cameras (when device has them) can be opened successfully
     * before the test. If not, it will ignore the test.
     * (4) Ensure default cameras for provided {@code lensFacings} (if device has them) are
     * available again after test.
     */
    public static @NonNull TestRule grantCameraPermissionAndPreTestAndPostTest(
            @Nullable PreTestCameraIdList cameraIdListTestRule,
            @NonNull List<Integer> lensFacings) {
        return grantCameraPermissionAndPreTestAndPostTest(new PreTestCamera(), cameraIdListTestRule,
                new PostTestCameraAvailability(lensFacings));
    }

    /**
     * Grant the camera permission and test the camera.
     *
     * @param cameraTestRule                to check if camera can be opened.
     * @param cameraIdListTestRule          to check if camera characteristic reports correct
     *                                      information that includes the supported camera devices
     *                                      that shows in the system.
     * @param postTestCameraAvailability    to check if camera is available again after test end.
     */
    public static @NonNull TestRule grantCameraPermissionAndPreTestAndPostTest(
            @Nullable PreTestCamera cameraTestRule,
            @Nullable PreTestCameraIdList cameraIdListTestRule,
            @Nullable PostTestCameraAvailability postTestCameraAvailability) {
        RuleChain rule = RuleChain.outerRule(GrantPermissionRule.grant(Manifest.permission.CAMERA));
        rule = rule.around(new IgnoreProblematicDeviceRule());
        if (cameraIdListTestRule != null) {
            rule = rule.around(cameraIdListTestRule);
        }
        if (cameraTestRule != null) {
            rule = rule.around(cameraTestRule);
        }
        if (postTestCameraAvailability != null) {
            rule = rule.around(postTestCameraAvailability);
        }
        rule = rule.around((base, description) -> new Statement() {
            @Override
            public void evaluate() throws Throwable {
                assumeTrue(deviceHasCamera());
                base.evaluate();
            }
        });

        return rule;
    }

    /**
     * Test the camera device
     *
     * <p>Try to open the camera with the front and back lensFacing. It throws an exception when
     * the test is running in the CameraX lab, or ignore the test otherwise.
     */
    public static class PreTestCamera implements TestRule {
        final boolean mThrowOnError = Log.isLoggable(PRETEST_CAMERA_TAG, Log.DEBUG);
        final AtomicReference<Boolean> mCanOpenCamera = new AtomicReference<>();

        @Override
        public @NonNull Statement apply(@NonNull Statement base, @NonNull Description description) {
            return new Statement() {
                @Override
                public void evaluate() throws Throwable {
                    if (mCanOpenCamera.get() == null) {
                        boolean backStatus = true;
                        if (hasCameraWithLensFacing(CameraSelector.LENS_FACING_BACK)) {
                            RetryCameraOpener opener =
                                    new RetryCameraOpener(CameraSelector.LENS_FACING_BACK);
                            backStatus = opener.openWithRetry(5, 5000);
                            opener.shutdown();
                        }

                        boolean frontStatus = true;
                        if (hasCameraWithLensFacing(CameraSelector.LENS_FACING_FRONT)) {
                            RetryCameraOpener opener =
                                    new RetryCameraOpener(CameraSelector.LENS_FACING_FRONT);
                            frontStatus = opener.openWithRetry(5, 5000);
                            opener.shutdown();
                        }
                        Logger.d(LOG_TAG,
                                "PreTest Open camera result " + backStatus + " " + frontStatus);
                        mCanOpenCamera.set(backStatus && frontStatus);
                    }

                    if (Boolean.TRUE.equals(mCanOpenCamera.get())) {
                        base.evaluate();
                    } else {
                        if (mThrowOnError) {
                            throw new RuntimeException(
                                    "CameraX_cannot_test_with_failed_camera, model:" + Build.MODEL);
                        }

                        // Ignore the test, throw the AssumptionViolatedException.
                        throw new AssumptionViolatedException("Ignore the test since the camera "
                                + "failed, on test " + description.getDisplayName());
                    }
                }
            };
        }
    }

    /**
     * Try to open the camera, and close it immediately.
     *
     * @param cameraId the id of the camera to test
     * @return true if the camera can be opened successfully
     */
    @SuppressLint("MissingPermission")
    public static boolean tryOpenCamera(@NonNull String cameraId) {
        CameraDeviceHolder deviceHolder = null;
        boolean ret = true;
        try {
            deviceHolder = new CameraDeviceHolder(getCameraManager(), cameraId, null);
            if (deviceHolder.get() == null) {
                ret = false;
            }
            if (Build.HARDWARE.equalsIgnoreCase("universal7420")
                    || Build.HARDWARE.equalsIgnoreCase("samsungexynos7420")) {
                // Please see b/305835396
                TimeUnit.SECONDS.sleep(1);
            }
        } catch (Exception e) {
            ret = false;
        } finally {
            if (deviceHolder != null) {
                try {
                    releaseCameraDevice(deviceHolder);
                } catch (Exception e) {
                    Logger.e(LOG_TAG, "Cannot close cameraDevice.", e);
                }
            }
        }

        return ret;
    }

    /**
     * A {@link CameraManager.AvailabilityCallback} implementation to observe the availability of a
     * specific camera device.
     */
    static class CameraAvailability extends CameraManager.AvailabilityCallback {
        private final Object mLock = new Object();
        private final String mCameraId;

        @GuardedBy("mLock")
        private boolean mCameraAvailable = false;
        @GuardedBy("mLock")
        private CallbackToFutureAdapter.Completer<Void> mCompleter;

        /**
         * Creates a new instance of {@link CameraAvailability}.
         *
         * @param cameraId The ID of the camera to observe.
         */
        CameraAvailability(@NonNull String cameraId) {
            mCameraId = cameraId;
        }

        /**
         * Returns a{@link ListenableFuture} that represents the availability of the camera.
         *
         * <p>If the camera is already available, the future will return immediately. Otherwise, the
         * future will complete when the camera becomes available.
         *
         * @return A {@link ListenableFuture} that represents the availability of the camera.
         */
        ListenableFuture<Void> observeAvailable() {
            synchronized (mLock) {
                if (mCameraAvailable) {
                    return Futures.immediateFuture(null);
                }
                return CallbackToFutureAdapter.getFuture(
                        completer -> {
                            synchronized (mLock) {
                                if (mCompleter != null) {
                                    mCompleter.setCancelled();
                                }
                                mCompleter = completer;
                            }
                            return "observeCameraAvailable_" + mCameraId;
                        });
            }
        }

        @Override
        public void onCameraAvailable(@NonNull String cameraId) {
            Logger.d(LOG_TAG, "Camera id " + cameraId + " onCameraAvailable callback");
            if (!mCameraId.equals(cameraId)) {
                // Ignore availability for other cameras
                return;
            }

            synchronized (mLock) {
                Logger.d(LOG_TAG, "Camera id " + mCameraId + " onCameraAvailable");
                mCameraAvailable = true;
                if (mCompleter != null) {
                    mCompleter.set(null);
                }
            }
        }

        @Override
        public void onCameraUnavailable(@NonNull String cameraId) {
            if (!mCameraId.equals(cameraId)) {
                // Ignore availability for other cameras
                return;
            }
            synchronized (mLock) {
                Logger.d(LOG_TAG, "Camera id " + mCameraId + " onCameraUnavailable");
                mCameraAvailable = false;
            }
        }
    }

    /**
     * Helper to verify the camera can be opened or not.
     *
     * <p>Call {@link #openWithRetry(int, long)} to start the test on the camera.
     *
     * <p>Call {@link #shutdown()} after finish the test.
     */
    public static class RetryCameraOpener {
        private static final int RETRY_DELAY_MS = 1000;
        private CameraAvailability mCameraAvailability;
        private HandlerThread mHandlerThread;
        private @Nullable String mCameraId;

        /**
         * @param lensFacing The camera lens facing to be tested.
         */
        public RetryCameraOpener(@CameraSelector.LensFacing int lensFacing) {
            mCameraId = getCameraIdWithLensFacing(lensFacing);
            Logger.d(LOG_TAG,
                    "PreTest init Camera lensFacing: " + lensFacing + " id: " + mCameraId);
            if (mCameraId == null) {
                return;
            }
            mCameraAvailability = new CameraAvailability(mCameraId);
            mHandlerThread = new HandlerThread(String.format("CameraThread-%s", mCameraId));
            mHandlerThread.start();

            getCameraManager().registerAvailabilityCallback(mCameraAvailability,
                    new Handler(mHandlerThread.getLooper()));
        }

        /**
         * Test to open the camera
         *
         * @param retryCount        the retry count when it cannot open camera.
         * @param waitCameraTimeout the time to wait if camera unavailable. In milliseconds.
         * @return true if camera can be opened, otherwise false.
         */
        @SuppressLint("BanThreadSleep")
        public boolean openWithRetry(int retryCount, long waitCameraTimeout) {
            if (mCameraId == null) {
                return false;
            }

            // Try to open the camera at the first time and we can grab the camera from the lower
            // priority user.
            for (int i = 0; i < retryCount; i++) {
                if (tryOpenCamera(mCameraId)) {
                    return true;
                }
                Logger.d(LOG_TAG,
                        "Cannot open camera with camera id: " + mCameraId + " retry:" + i);
                if (!waitForCameraAvailable(waitCameraTimeout)) {
                    return false;
                }

                try {
                    Thread.sleep(RETRY_DELAY_MS);
                } catch (InterruptedException e) {
                    // do nothing
                }
            }
            return false;
        }

        private boolean waitForCameraAvailable(long waitCameraTimeout) {
            try {
                mCameraAvailability.observeAvailable().get(waitCameraTimeout,
                        TimeUnit.MILLISECONDS);
                return true;
            } catch (Exception e) {
                Logger.e(LOG_TAG, "Wait for camera available timeout camera id:" + mCameraId);
                return false;
            }
        }

        /**
         * Close the opener and release resource.
         */
        public void shutdown() {
            if (mCameraId == null) {
                return;
            }
            mCameraId = null;
            getCameraManager().unregisterAvailabilityCallback(mCameraAvailability);
            mHandlerThread.quitSafely();
        }
    }

    /**
     * Check the camera lensFacing info is reported correctly
     *
     * <p>For b/167201193
     *
     * <P>Verify the lensFacing info is available in the CameraCharacteristic, or initialize
     * CameraX with the CameraXConfig if it is provided. Throws an exception to interrupt the
     * test if it detects incorrect info, or throws AssumptionViolatedException when it is not in
     * the CameraX lab.
     */
    public static class PreTestCameraIdList implements TestRule {
        final boolean mThrowOnError = Log.isLoggable("CameraXDumpIdList", Log.DEBUG);
        final AtomicReference<Boolean> mCameraIdListCorrect = new AtomicReference<>();

        final @Nullable CameraXConfig mCameraXConfig;

        public PreTestCameraIdList() {
            mCameraXConfig = null;
        }

        /**
         * Try to use the {@link CameraXConfig} to initialize CameraX when it fails to detect the
         * valid lens facing info from camera characteristics.
         */
        public PreTestCameraIdList(@NonNull CameraXConfig config) {
            mCameraXConfig = config;
        }

        @Override
        public @NonNull Statement apply(@NonNull Statement base, @NonNull Description description) {
            return new Statement() {
                @Override
                public void evaluate() throws Throwable {
                    String logPrefix = String.format("[%s]", System.currentTimeMillis());
                    if (mCameraIdListCorrect.get() == null) {
                        if (isCameraLensFacingInfoAvailable(logPrefix)) {
                            mCameraIdListCorrect.set(true);
                        } else {
                            mCameraIdListCorrect.set(false);
                        }

                        // Always try to initialize CameraX if the CameraXConfig has been set.
                        if (mCameraXConfig != null) {
                            if (checkLensFacingByCameraXConfig(logPrefix,
                                    ApplicationProvider.getApplicationContext(), mCameraXConfig)) {
                                mCameraIdListCorrect.set(true);
                            } else {
                                mCameraIdListCorrect.set(false);
                            }
                        }
                    }

                    if (Boolean.TRUE.equals(mCameraIdListCorrect.get())) {
                        base.evaluate();
                    } else {
                        if (mThrowOnError) {
                            throw new IllegalArgumentException(
                                    "CameraIdList_incorrect:" + Build.MODEL);
                        }

                        // Ignore the test, throw the AssumptionViolatedException.
                        throw new AssumptionViolatedException("Ignore the test since the camera "
                                + "id list failed, on test " + description.getDisplayName());
                    }
                }
            };
        }
    }

    /**
     * Waits for the camera to be available after test.
     *
     * <p>Try to open the camera with the front and back lensFacing. It throws an exception when
     * the test is running in the CameraX lab, or ignore the test otherwise.
     */
    public static class PostTestCameraAvailability implements TestRule {
        private int mTimeoutMillis = 5000;
        private final List<@CameraSelector.LensFacing Integer> mLensFacings;

        public PostTestCameraAvailability(
                @NonNull List<@CameraSelector.LensFacing Integer> lensFacings) {
            mLensFacings = lensFacings;
        }

        public PostTestCameraAvailability(
                @NonNull List<@CameraSelector.LensFacing Integer> lensFacings,
                int timeoutPerCameraMillis) {
            mLensFacings = lensFacings;
            mTimeoutMillis = timeoutPerCameraMillis;
        }

        @Override
        public @NonNull Statement apply(@NonNull Statement base, @NonNull Description description) {
            return new Statement() {
                @Override
                public void evaluate() throws Throwable {
                    base.evaluate();

                    for (int lensFacing : mLensFacings) {
                        String cameraId = getCameraIdWithLensFacing(lensFacing);
                        Logger.d(LOG_TAG, "PostTestCameraAvailability: lensFacing = " + lensFacing
                                + ", cameraId = " + cameraId);
                        if (cameraId == null) {
                            return;
                        }

                        CameraAvailability cameraAvailability = new CameraAvailability(cameraId);

                        HandlerThread handlerThread = new HandlerThread(
                                String.format("CameraThread-%s", cameraId));
                        handlerThread.start();

                        getCameraManager().registerAvailabilityCallback(cameraAvailability,
                                new Handler(handlerThread.getLooper()));

                        try {
                            Logger.d(LOG_TAG,
                                    "PostTestCameraAvailability: Waiting for camera["
                                            + cameraId + "] to be available!");
                            cameraAvailability.observeAvailable().get(mTimeoutMillis,
                                    TimeUnit.MILLISECONDS);
                            Logger.d(LOG_TAG,
                                    "PostTestCameraAvailability: camera[" + cameraId
                                            + "] is now available!");
                        } catch (Exception e) {
                            Logger.d(LOG_TAG,
                                    "PostTestCameraAvailability: observeAvailable failed for "
                                            + "lensFacing = " + lensFacing + ", cameraId = "
                                            + cameraId, e);
                        } finally {
                            getCameraManager().unregisterAvailabilityCallback(cameraAvailability);
                            handlerThread.quitSafely();
                        }
                    }
                }
            };
        }
    }

    static boolean checkLensFacingByCameraXConfig(
            @NonNull String logPrefix,
            @NonNull Context context,
            @NonNull CameraXConfig config) {
        try {
            // Shutdown exist instances, if there is any
            CameraXUtil.shutdown().get(10, TimeUnit.SECONDS);
            logInit(logPrefix, "Start init CameraX");

            CameraXUtil.initialize(context, config).get(10, TimeUnit.SECONDS);
            CameraX camerax = CameraXUtil.getOrCreateInstance(context, null).get(5,
                    TimeUnit.SECONDS);
            LinkedHashSet<CameraInternal> cameras = camerax.getCameraRepository().getCameras();

            PackageManager pm = context.getPackageManager();
            boolean backFeature = pm.hasSystemFeature(PackageManager.FEATURE_CAMERA);
            boolean frontFeature = pm.hasSystemFeature(PackageManager.FEATURE_CAMERA_FRONT);
            if (backFeature) {
                CameraSelector.DEFAULT_BACK_CAMERA.select(cameras);
            }
            if (frontFeature) {
                CameraSelector.DEFAULT_FRONT_CAMERA.select(cameras);
            }
            logInit(logPrefix, "Successfully init CameraX");
            return true;
        } catch (Exception e) {
            logInit(logPrefix, "CameraX init fail", e);
        } finally {
            try {
                CameraXUtil.shutdown().get(10, TimeUnit.SECONDS);
            } catch (Exception e) {
                // Ignore all exceptions in the shutdown process.
            }
        }
        return false;
    }

    /**
     * Check the camera lensFacing info for b/167201193 debug.
     *
     * @return true if the front and main camera info exists in the camera characteristic.
     */
    @SuppressWarnings("ObjectToString")
    static boolean isCameraLensFacingInfoAvailable(@NonNull String logPrefix) {
        boolean error = false;
        Context context = ApplicationProvider.getApplicationContext();
        CameraManager manager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);

        String[] cameraIds = new String[0];
        try {
            cameraIds = manager.getCameraIdList();
            Logger.d(LOG_TAG, "ids: " + Arrays.toString(cameraIds));
        } catch (CameraAccessException e) {
            error = true;
            Logger.e(LOG_TAG, "Cannot find default camera id");
        }

        if (cameraIds != null && cameraIds.length > 0) {
            boolean hasFront = false;
            boolean hasBack = false;
            for (String id : cameraIds) {
                Logger.d(LOG_TAG, "++ Camera id: " + id);
                try {
                    CameraCharacteristics c = manager.getCameraCharacteristics(id);
                    if (c != null) {
                        Integer lensFacing = c.get(CameraCharacteristics.LENS_FACING);
                        Logger.d(LOG_TAG, id + " lensFacing: " + lensFacing);
                        if (lensFacing == CameraCharacteristics.LENS_FACING_BACK) {
                            hasBack = true;
                        }
                        if (lensFacing == CameraCharacteristics.LENS_FACING_FRONT) {
                            hasFront = true;
                        }
                    }
                } catch (Throwable t) {
                    Logger.d(LOG_TAG, id + ", failed to get CameraCharacteristics", t);
                }
                Logger.d(LOG_TAG, "-- Camera id: " + id);
            }

            PackageManager pm = context.getPackageManager();
            boolean backFeature = pm.hasSystemFeature(PackageManager.FEATURE_CAMERA);
            boolean frontFeature = pm.hasSystemFeature(PackageManager.FEATURE_CAMERA_FRONT);

            // Pass when no such feature or it gets the camera from the camera characteristic.
            boolean backPass = !backFeature || hasBack;
            boolean frontPass = !frontFeature || hasFront;

            if (!backPass || !frontPass) {
                error = true;
                logInit(logPrefix,
                        "Missing front or back camera, has front camera: " + hasFront + ", has "
                                + "back camera: " + hasBack + " has main camera feature:"
                                + backFeature + " has front camera feature:" + frontFeature
                                + " ids: " + Arrays.toString(cameraIds));
            }
        } else {
            error = true;
            logInit(logPrefix, "cameraIds.length is zero");
        }

        return !error;
    }

    private static void logInit(@NonNull String prefix, @NonNull String message) {
        logInit(prefix, message, null);
    }

    private static void logInit(
            @NonNull String prefix, @NonNull String message, @Nullable Throwable t) {
        if (t != null) {
            Logger.i(LOG_TAG, prefix + message + " Time:" + System.currentTimeMillis(), t);
        } else {
            Logger.i(LOG_TAG, prefix + message + " Time:" + System.currentTimeMillis());
        }
    }
}
