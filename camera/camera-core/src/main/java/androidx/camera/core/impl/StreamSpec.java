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

package androidx.camera.core.impl;

import android.hardware.camera2.CameraMetadata;
import android.util.Range;
import android.util.Size;

import androidx.camera.core.DynamicRange;

import com.google.auto.value.AutoValue;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * A stream specification defining how a camera frame stream should be configured.
 *
 * <p>The values communicated by this class specify what the camera is expecting to produce as a
 * frame stream, and can be useful for configuring the frame consumer.
 */
@AutoValue
public abstract class StreamSpec {

    /** A frame rate range with no specified upper or lower bound. */
    public static final Range<Integer> FRAME_RATE_RANGE_UNSPECIFIED = new Range<>(0, 0);

    /**
     * Returns the resolution for the stream associated with this stream specification.
     * @return the resolution for the stream.
     */
    public abstract @NonNull Size getResolution();

    /**
     * Returns the {@link DynamicRange} for the stream associated with this stream specification.
     * @return the dynamic range for the stream.
     */
    public abstract @NonNull DynamicRange getDynamicRange();

    /**
     * Returns the expected frame rate range for the stream associated with this stream
     * specification.
     * @return the expected frame rate range for the stream.
     */
    public abstract @NonNull Range<Integer> getExpectedFrameRateRange();

    /**
     * Returns the implementation options associated with this stream
     * specification.
     * @return the implementation options for the stream.
     */
    public abstract @Nullable Config getImplementationOptions();

    /**
     * Returns the flag if zero-shutter lag needs to be disabled by user case combinations.
     */
    public abstract boolean getZslDisabled();

    /** Returns a build for a stream configuration that takes a required resolution. */
    public static @NonNull Builder builder(@NonNull Size resolution) {
        return new AutoValue_StreamSpec.Builder()
                .setResolution(resolution)
                .setExpectedFrameRateRange(FRAME_RATE_RANGE_UNSPECIFIED)
                .setDynamicRange(DynamicRange.SDR)
                .setZslDisabled(false);
    }

    /** Returns a builder pre-populated with the current specification. */
    public abstract @NonNull Builder toBuilder();

    /** A builder for a stream specification */
    @AutoValue.Builder
    public abstract static class Builder {
        // Restrict construction to same package
        Builder() {
        }

        /** Sets the resolution, overriding the existing resolution set in this builder. */
        public abstract @NonNull Builder setResolution(@NonNull Size resolution);

        /**
         * Sets the dynamic range.
         *
         * <p>If not set, the default dynamic range is {@link DynamicRange#SDR}.
         */
        public abstract @NonNull Builder setDynamicRange(@NonNull DynamicRange dynamicRange);

        /**
         * Sets the expected frame rate range.
         *
         * <p>If not set, the default expected frame rate range is
         * {@link #FRAME_RATE_RANGE_UNSPECIFIED}.
         */
        public abstract @NonNull Builder setExpectedFrameRateRange(@NonNull Range<Integer> range);

        /**
         * Sets the implementation options.
         *
         * <p>If not set, the default expected frame rate range is
         * {@link CameraMetadata#SCALER_AVAILABLE_STREAM_USE_CASES_DEFAULT}.
         */
        public abstract @NonNull Builder setImplementationOptions(@NonNull Config config);

        /**
         * Sets the flag if zero-shutter lag needs to be disabled by user case combinations.
         */
        public abstract @NonNull Builder setZslDisabled(boolean disabled);

        /** Builds the stream specification */
        public abstract @NonNull StreamSpec build();
    }

}
