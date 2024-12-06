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
// @exportToFramework:skipFile()
package androidx.appsearch.localstorage;

import androidx.annotation.RestrictTo;
import androidx.annotation.VisibleForTesting;

import com.google.android.icing.proto.GetOptimizeInfoResultProto;

import org.jspecify.annotations.NonNull;

/**
 * An implementation of {@link androidx.appsearch.localstorage.OptimizeStrategy} will
 * determine when to trigger {@link androidx.appsearch.localstorage.AppSearchImpl#optimize()} in
 * Jetpack environment.
 * @exportToFramework:hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class JetpackOptimizeStrategy implements OptimizeStrategy {

    @VisibleForTesting
    static final int DOC_COUNT_OPTIMIZE_THRESHOLD = 1000;
    @VisibleForTesting
    static final int BYTES_OPTIMIZE_THRESHOLD = 1 * 1024 * 1024; // 1MB

    @Override
    public boolean shouldOptimize(@NonNull GetOptimizeInfoResultProto optimizeInfo) {
        return optimizeInfo.getOptimizableDocs() >= DOC_COUNT_OPTIMIZE_THRESHOLD
                || optimizeInfo.getEstimatedOptimizableBytes() >= BYTES_OPTIMIZE_THRESHOLD;
    }
}
