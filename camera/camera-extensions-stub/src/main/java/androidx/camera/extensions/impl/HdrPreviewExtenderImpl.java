/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.camera.extensions.impl;

import android.content.Context;
import android.hardware.camera2.CameraCharacteristics;
import android.util.Pair;
import android.util.Size;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Stub implementation for HDR preview use case.
 *
 * <p>This class should be implemented by OEM and deployed to the target devices.
 *
 * @since 1.0
 */
public final class HdrPreviewExtenderImpl implements PreviewExtenderImpl {
    public HdrPreviewExtenderImpl() {
    }

    @Override
    public boolean isExtensionAvailable(@NonNull String cameraId,
            @Nullable CameraCharacteristics cameraCharacteristics) {
        throw new RuntimeException("Stub, replace with implementation.");
    }

    @Override
    public void init(@NonNull String cameraId,
            @NonNull CameraCharacteristics cameraCharacteristics) {
        throw new RuntimeException("Stub, replace with implementation.");
    }

    @Override
    public @NonNull CaptureStageImpl getCaptureStage() {
        throw new RuntimeException("Stub, replace with implementation.");
    }

    @Override
    public @NonNull ProcessorType getProcessorType() {
        throw new RuntimeException("Stub, replace with implementation.");
    }

    @Override
    public @Nullable ProcessorImpl getProcessor() {
        throw new RuntimeException("Stub, replace with implementation.");
    }

    @Override
    public void onInit(@NonNull String cameraId,
            @NonNull CameraCharacteristics cameraCharacteristics,
            @NonNull Context context) {
        throw new RuntimeException("Stub, replace with implementation.");
    }

    @Override
    public void onDeInit() {
        throw new RuntimeException("Stub, replace with implementation.");
    }

    @Override
    public @Nullable CaptureStageImpl onPresetSession() {
        throw new RuntimeException("Stub, replace with implementation.");
    }

    @Override
    public @Nullable CaptureStageImpl onEnableSession() {
        throw new RuntimeException("Stub, replace with implementation.");
    }

    @Override
    public @Nullable CaptureStageImpl onDisableSession() {
        throw new RuntimeException("Stub, replace with implementation.");
    }

    @Override
    public @Nullable List<Pair<Integer, Size[]>> getSupportedResolutions() {
        throw new RuntimeException("Stub, replace with implementation.");
    }

    @Override
    public int onSessionType() {
        throw new RuntimeException("Stub, replace with implementation.");
    }
}
