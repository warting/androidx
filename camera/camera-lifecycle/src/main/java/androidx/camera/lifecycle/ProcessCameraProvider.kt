/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.camera.lifecycle

import android.app.Application
import android.content.Context
import androidx.annotation.MainThread
import androidx.annotation.OptIn
import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import androidx.camera.core.Camera
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraInfoUnavailableException
import androidx.camera.core.CameraProvider
import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraXConfig
import androidx.camera.core.ConcurrentCamera
import androidx.camera.core.ExperimentalSessionConfig
import androidx.camera.core.InitializationException
import androidx.camera.core.SessionConfig
import androidx.camera.core.UseCase
import androidx.camera.core.UseCaseGroup
import androidx.camera.core.impl.utils.executor.CameraXExecutors
import androidx.camera.core.impl.utils.futures.Futures
import androidx.camera.lifecycle.ProcessCameraProvider.Companion.configureInstance
import androidx.camera.lifecycle.ProcessCameraProvider.Companion.getInstance
import androidx.core.util.Preconditions
import androidx.lifecycle.LifecycleOwner
import androidx.tracing.trace
import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.TimeUnit
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * A singleton which can be used to bind the lifecycle of cameras to any [LifecycleOwner] within an
 * application's process.
 *
 * Only a single process camera provider can exist within a process, and it can be retrieved with
 * [getInstance].
 *
 * Heavyweight resources, such as open and running camera devices, will be scoped to the lifecycle
 * provided to [bindToLifecycle]. Other lightweight resources, such as static camera
 * characteristics, may be retrieved and cached upon first retrieval of this provider with
 * [getInstance], and will persist for the lifetime of the process.
 *
 * This is the standard provider for applications to use.
 */
@OptIn(ExperimentalCameraProviderConfiguration::class)
public class ProcessCameraProvider
private constructor(private val lifecycleCameraProvider: LifecycleCameraProviderImpl) :
    CameraProvider {

    public fun isBound(useCase: UseCase): Boolean {
        return lifecycleCameraProvider.isBound(useCase)
    }

    @ExperimentalSessionConfig
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public fun isBound(sessionConfig: SessionConfig): Boolean {
        return lifecycleCameraProvider.isBound(sessionConfig)
    }

    @MainThread
    public fun unbind(vararg useCases: UseCase?) {
        return lifecycleCameraProvider.unbind(*useCases)
    }

    @ExperimentalSessionConfig
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public fun unbind(sessionConfig: SessionConfig) {
        return lifecycleCameraProvider.unbind(sessionConfig)
    }

    @MainThread
    public fun unbindAll() {
        return lifecycleCameraProvider.unbindAll()
    }

    @MainThread
    public fun bindToLifecycle(
        lifecycleOwner: LifecycleOwner,
        cameraSelector: CameraSelector,
        vararg useCases: UseCase?
    ): Camera {
        return lifecycleCameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, *useCases)
    }

    @MainThread
    public fun bindToLifecycle(
        lifecycleOwner: LifecycleOwner,
        cameraSelector: CameraSelector,
        useCaseGroup: UseCaseGroup
    ): Camera {
        return lifecycleCameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, useCaseGroup)
    }

    @ExperimentalSessionConfig
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public fun bindToLifecycle(
        lifecycleOwner: LifecycleOwner,
        cameraSelector: CameraSelector,
        sessionConfig: SessionConfig
    ): Camera {
        return lifecycleCameraProvider.bindToLifecycle(
            lifecycleOwner,
            cameraSelector,
            sessionConfig
        )
    }

    @MainThread
    public fun bindToLifecycle(
        singleCameraConfigs: List<ConcurrentCamera.SingleCameraConfig?>
    ): ConcurrentCamera {
        return lifecycleCameraProvider.bindToLifecycle(singleCameraConfigs)
    }

    override val availableCameraInfos: List<CameraInfo>
        get() = lifecycleCameraProvider.availableCameraInfos

    override val availableConcurrentCameraInfos: List<List<CameraInfo>>
        get() = lifecycleCameraProvider.availableConcurrentCameraInfos

    override val isConcurrentCameraModeOn: Boolean
        @MainThread get() = lifecycleCameraProvider.isConcurrentCameraModeOn

    override val configImplType: Int
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) get() = lifecycleCameraProvider.configImplType

    @Throws(CameraInfoUnavailableException::class)
    override fun hasCamera(cameraSelector: CameraSelector): Boolean {
        return lifecycleCameraProvider.hasCamera(cameraSelector)
    }

    override fun getCameraInfo(cameraSelector: CameraSelector): CameraInfo {
        return lifecycleCameraProvider.getCameraInfo(cameraSelector)
    }

    /**
     * Allows shutting down this ProcessCameraProvider instance so a new instance can be retrieved
     * by [getInstance].
     *
     * Once shutdownAsync is invoked, a new instance can be retrieved with [getInstance].
     *
     * This method should be used for testing purposes only. Along with [configureInstance], this
     * allows the process camera provider to be used in test suites which may need to initialize
     * CameraX in different ways in between tests.
     *
     * @return A [ListenableFuture] representing the shutdown status. Cancellation of this future is
     *   a no-op.
     */
    @VisibleForTesting
    public fun shutdownAsync(): ListenableFuture<Void> {
        return lifecycleCameraProvider.shutdownAsync()
    }

    private fun initAsync(context: Context): ListenableFuture<Void> {
        return lifecycleCameraProvider.initAsync(context, null)
    }

    private fun configure(cameraXConfig: CameraXConfig) {
        return lifecycleCameraProvider.configure(cameraXConfig)
    }

    public companion object {
        private val sAppInstance = ProcessCameraProvider(LifecycleCameraProviderImpl())

        /**
         * Retrieves the ProcessCameraProvider associated with the current process.
         *
         * The instance returned here can be used to bind use cases to any [LifecycleOwner] with
         * [bindToLifecycle].
         *
         * The instance's configuration may be customized by subclassing the application's
         * [Application] class and implementing [CameraXConfig.Provider]. For example, the sample
         * implements [CameraXConfig.Provider.getCameraXConfig] and initializes this process camera
         * provider with a [Camera2 implementation][androidx.camera.camera2.Camera2Config] from
         * [androidx.camera.camera2], and with a custom executor.
         *
         * @sample androidx.camera.lifecycle.samples.getCameraXConfigSample
         *
         * If it isn't possible to subclass the [Application] class, such as in library code, then
         * the singleton can be configured via [configureInstance] before the first invocation of
         * `getInstance(context)`, the sample implements a customized camera provider that
         * configures the instance before getting it.
         *
         * @sample androidx.camera.lifecycle.samples.configureAndGetInstanceSample
         *
         * If no [CameraXConfig.Provider] is implemented by [Application], or if the singleton has
         * not been configured via [configureInstance] a default configuration will be used.
         *
         * @param context The application context.
         * @return A future which will contain the ProcessCameraProvider. Cancellation of this
         *   future is a no-op. This future may fail with an [InitializationException] and
         *   associated cause that can be retrieved by [Throwable.cause]. The cause will be a
         *   [androidx.camera.core.CameraUnavailableException] if it fails to access any camera
         *   during initialization.
         * @throws IllegalStateException if CameraX fails to initialize via a default provider or a
         *   [CameraXConfig.Provider].
         * @see configureInstance
         */
        @Suppress("AsyncSuffixFuture")
        @JvmStatic
        public fun getInstance(context: Context): ListenableFuture<ProcessCameraProvider> {
            Preconditions.checkNotNull(context)
            return Futures.transform(
                sAppInstance.initAsync(context),
                { sAppInstance },
                CameraXExecutors.directExecutor()
            )
        }

        /**
         * Perform one-time configuration of the ProcessCameraProvider singleton with the given
         * [CameraXConfig].
         *
         * This method allows configuration of the camera provider via [CameraXConfig]. All
         * initialization tasks, such as communicating with the camera service, will be executed on
         * the [java.util.concurrent.Executor] set by [CameraXConfig.Builder.setCameraExecutor], or
         * by an internally defined executor if none is provided.
         *
         * This method is not required for every application. If the method is not called and
         * [CameraXConfig.Provider] is not implemented in [Application], default configuration will
         * be used.
         *
         * Once this method is called, the instance configured by the given [CameraXConfig] can be
         * retrieved with [getInstance]. [CameraXConfig.Provider] implemented in [Application] will
         * be ignored.
         *
         * Configuration can only occur once. Once the ProcessCameraProvider has been configured
         * with `configureInstance()` or [getInstance], this method will throw an
         * [IllegalStateException]. Because configuration can only occur once, **usage of this
         * method from library code is not recommended** as the application owner should ultimately
         * be in control of singleton configuration.
         *
         * @param cameraXConfig The configuration options for the singleton process camera provider
         *   instance.
         * @throws IllegalStateException If the camera provider has already been configured by a
         *   previous call to `configureInstance()` or [getInstance].
         */
        @JvmStatic
        @ExperimentalCameraProviderConfiguration
        public fun configureInstance(cameraXConfig: CameraXConfig): Unit =
            trace("CX:configureInstance") { sAppInstance.configure(cameraXConfig) }

        @JvmStatic
        @VisibleForTesting
        @ExperimentalCameraProviderConfiguration
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public fun clearConfiguration(timeout: Duration = 10.seconds) {
            sAppInstance.shutdownAsync().get(timeout.inWholeNanoseconds, TimeUnit.NANOSECONDS)
        }

        /**
         * Shuts down the ProcessCameraProvider asynchronously.
         *
         * This will release all resources held by the provider. The returned [ListenableFuture]
         * will complete once the shutdown is complete.
         *
         * @return A [ListenableFuture] which will complete when the provider has been shut down.
         */
        @JvmStatic
        @VisibleForTesting
        @ExperimentalCameraProviderConfiguration
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public fun shutdown(): ListenableFuture<Void> {
            return sAppInstance.shutdownAsync()
        }
    }
}
