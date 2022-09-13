/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.camera.core.imagecapture;

import static androidx.camera.core.CaptureBundles.singleDefaultCaptureBundle;
import static androidx.camera.core.impl.utils.Threads.checkMainThread;
import static androidx.camera.core.impl.utils.TransformUtils.hasCropping;

import static java.util.Objects.requireNonNull;

import android.graphics.ImageFormat;
import android.media.ImageReader;
import android.os.Build;
import android.util.Size;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.annotation.VisibleForTesting;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.impl.CaptureBundle;
import androidx.camera.core.impl.CaptureConfig;
import androidx.camera.core.impl.CaptureStage;
import androidx.camera.core.impl.ImageCaptureConfig;
import androidx.camera.core.impl.SessionConfig;
import androidx.camera.core.impl.utils.executor.CameraXExecutors;
import androidx.camera.core.internal.compat.workaround.ExifRotationAvailability;
import androidx.core.util.Pair;

import java.util.ArrayList;
import java.util.List;

/**
 * The class that builds and maintains the {@link ImageCapture} pipeline.
 *
 * <p>This class is responsible for building the entire pipeline, from creating camera request to
 * post-processing the output.
 */
@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class ImagePipeline {

    static final ExifRotationAvailability EXIF_ROTATION_AVAILABILITY =
            new ExifRotationAvailability();
    // Use case configs.
    @NonNull
    private final ImageCaptureConfig mUseCaseConfig;
    @NonNull
    private final CaptureConfig mCaptureConfig;

    // Post-processing pipeline.
    @NonNull
    private final CaptureNode mCaptureNode;
    @NonNull
    private final SingleBundlingNode mBundlingNode;
    @NonNull
    private ProcessingNode mProcessingNode;
    @NonNull
    private final CaptureNode.In mPipelineIn;

    // ===== public methods =====

    @MainThread
    public ImagePipeline(
            @NonNull ImageCaptureConfig useCaseConfig,
            @NonNull Size cameraSurfaceSize) {
        checkMainThread();
        mUseCaseConfig = useCaseConfig;
        mCaptureConfig = CaptureConfig.Builder.createFrom(useCaseConfig).build();

        // Create nodes
        mCaptureNode = new CaptureNode();
        mBundlingNode = new SingleBundlingNode();
        mProcessingNode = new ProcessingNode(
                requireNonNull(mUseCaseConfig.getIoExecutor(CameraXExecutors.ioExecutor())));

        // Connect nodes
        mPipelineIn = CaptureNode.In.of(cameraSurfaceSize, mUseCaseConfig.getInputFormat());
        CaptureNode.Out captureOut = mCaptureNode.transform(mPipelineIn);
        ProcessingNode.In processingIn = mBundlingNode.transform(captureOut);
        mProcessingNode.transform(processingIn);
    }

    /**
     * Creates a {@link SessionConfig.Builder} for configuring camera.
     */
    @NonNull
    public SessionConfig.Builder createSessionConfigBuilder() {
        SessionConfig.Builder builder = SessionConfig.Builder.createFrom(mUseCaseConfig);
        builder.addNonRepeatingSurface(mPipelineIn.getSurface());
        return builder;
    }

    /**
     * Closes the pipeline and release all resources.
     *
     * <p>Releases all the buffers and resources allocated by the pipeline. e.g. closing
     * {@link ImageReader}s.
     */
    @MainThread
    public void close() {
        checkMainThread();
        mCaptureNode.release();
        mBundlingNode.release();
        mProcessingNode.release();
    }

    // ===== protected methods =====

    /**
     * Creates two requests from a {@link TakePictureRequest}: a request for camera and a request
     * for post-processing.
     *
     * <p>{@link ImagePipeline} creates two requests from {@link TakePictureRequest}: 1) a
     * request sent for post-processing pipeline and 2) a request for camera. The camera request
     * is returned to the caller, and the post-processing request is handled by this class.
     */
    @MainThread
    @NonNull
    Pair<CameraRequest, ProcessingRequest> createRequests(
            @NonNull TakePictureRequest takePictureRequest,
            @NonNull TakePictureCallback takePictureCallback) {
        checkMainThread();
        CaptureBundle captureBundle = createCaptureBundle();
        return new Pair<>(
                createCameraRequest(
                        captureBundle,
                        takePictureRequest,
                        takePictureCallback),
                createProcessingRequest(
                        captureBundle,
                        takePictureRequest,
                        takePictureCallback));
    }

    @MainThread
    void postProcess(@NonNull ProcessingRequest request) {
        checkMainThread();
        mPipelineIn.getRequestEdge().accept(request);
    }

    // ===== private methods =====

    @NonNull
    private CaptureBundle createCaptureBundle() {
        return requireNonNull(mUseCaseConfig.getCaptureBundle(singleDefaultCaptureBundle()));
    }

    @NonNull
    private ProcessingRequest createProcessingRequest(
            @NonNull CaptureBundle captureBundle,
            @NonNull TakePictureRequest takePictureRequest,
            @NonNull TakePictureCallback takePictureCallback) {
        return new ProcessingRequest(
                captureBundle,
                takePictureRequest.getOutputFileOptions(),
                takePictureRequest.getCropRect(),
                takePictureRequest.getRotationDegrees(),
                takePictureRequest.getJpegQuality(),
                takePictureRequest.getSensorToBufferTransform(),
                takePictureCallback);
    }

    private CameraRequest createCameraRequest(
            @NonNull CaptureBundle captureBundle,
            @NonNull TakePictureRequest takePictureRequest,
            @NonNull TakePictureCallback takePictureCallback) {
        List<CaptureConfig> captureConfigs = new ArrayList<>();
        String tagBundleKey = String.valueOf(captureBundle.hashCode());
        for (final CaptureStage captureStage : requireNonNull(captureBundle.getCaptureStages())) {
            final CaptureConfig.Builder builder = new CaptureConfig.Builder();
            builder.setTemplateType(mCaptureConfig.getTemplateType());

            // Add the default implementation options of ImageCapture
            builder.addImplementationOptions(mCaptureConfig.getImplementationOptions());
            builder.addAllCameraCaptureCallbacks(
                    takePictureRequest.getSessionConfigCameraCaptureCallbacks());
            builder.addSurface(mPipelineIn.getSurface());

            // Only sets the JPEG rotation and quality for JPEG format. Some devices do not
            // handle these configs for non-JPEG images. See b/204375890.
            if (mPipelineIn.getFormat() == ImageFormat.JPEG) {
                if (EXIF_ROTATION_AVAILABILITY.isRotationOptionSupported()) {
                    builder.addImplementationOption(CaptureConfig.OPTION_ROTATION,
                            takePictureRequest.getRotationDegrees());
                }
                builder.addImplementationOption(CaptureConfig.OPTION_JPEG_QUALITY,
                        getCameraRequestJpegQuality(takePictureRequest));
            }

            // Add the implementation options required by the CaptureStage
            builder.addImplementationOptions(
                    captureStage.getCaptureConfig().getImplementationOptions());

            // Use CaptureBundle object as the key for TagBundle
            builder.addTag(tagBundleKey, captureStage.getId());
            builder.addCameraCaptureCallback(mPipelineIn.getCameraCaptureCallback());
            captureConfigs.add(builder.build());
        }

        return new CameraRequest(captureConfigs, takePictureCallback);
    }

    /**
     * Returns the JPEG quality for camera request.
     *
     * <p>If there is JPEG encoding in post-processing, use max quality for the camera request to
     * minimize quality loss.
     *
     * <p>However this results in poor performance during cropping than setting 95 (b/206348741).
     */
    int getCameraRequestJpegQuality(@NonNull TakePictureRequest request) {
        boolean isOnDisk = request.getOnDiskCallback() != null;
        boolean hasCropping = hasCropping(request.getCropRect(), mPipelineIn.getSize());
        boolean isMaxQuality =
                request.getCaptureMode() == ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY;
        if (isOnDisk && hasCropping && isMaxQuality) {
            // For saving to disk, the image is decoded to Bitmap, cropped and encoded to JPEG
            // again. In that case, use 100 to avoid compression quality loss. The trade-off of
            // using a high quality is poorer performance. So we only do that if the capture mode
            // is CAPTURE_MODE_MAXIMIZE_QUALITY.
            return 100;
        }
        return request.getJpegQuality();
    }

    @NonNull
    @VisibleForTesting
    CaptureNode getCaptureNode() {
        return mCaptureNode;
    }

}
