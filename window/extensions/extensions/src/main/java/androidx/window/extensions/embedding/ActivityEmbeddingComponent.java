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

package androidx.window.extensions.embedding;

import android.app.Activity;

import androidx.annotation.NonNull;
import androidx.window.extensions.WindowExtensions;
import androidx.window.extensions.core.util.function.Consumer;

import java.util.List;
import java.util.Set;

/**
 * Extension component definition that is used by the WindowManager library to trigger custom
 * OEM-provided methods for organizing activities that isn't covered by platform APIs.
 *
 * <p>This interface should be implemented by OEM and deployed to the target devices.
 * @see androidx.window.extensions.WindowExtensions
 */
public interface ActivityEmbeddingComponent {

    /**
     * Updates the rules of embedding activities that are started in the client process.
     */
    void setEmbeddingRules(@NonNull Set<EmbeddingRule> splitRules);

    /**
     * @deprecated Use {@link #setSplitInfoCallback(Consumer)} starting with
     * {@link WindowExtensions#VENDOR_API_LEVEL_2}. Only used if
     * {@link #setSplitInfoCallback(Consumer)} can't be called on
     * {@link WindowExtensions#VENDOR_API_LEVEL_1}.
     */
    @Deprecated
    @SuppressWarnings("ExecutorRegistration") // Jetpack will post it on the app-provided executor.
    void setSplitInfoCallback(@NonNull java.util.function.Consumer<List<SplitInfo>> consumer);

    /**
     * Sets the callback that notifies WM Jetpack about changes in split states from the Extensions
     * Sidecar implementation. The listener should be registered for the lifetime of the process.
     * There are no threading guarantees where the events are dispatched from. All messages are
     * re-posted to the executors provided by developers.
     *
     * @param consumer the callback to notify {@link SplitInfo} list changes
     * @since {@link WindowExtensions#VENDOR_API_LEVEL_2}
     */
    @SuppressWarnings("ExecutorRegistration") // Jetpack will post it on the app-provided executor.
    default void setSplitInfoCallback(@NonNull Consumer<List<SplitInfo>> consumer) {
        throw new UnsupportedOperationException("This method must not be called unless there is a"
                + " corresponding override implementation on the device.");
    }

    /**
     * Clears the callback that was set in
     * {@link ActivityEmbeddingComponent#setSplitInfoCallback(Consumer)}.
     * Added in {@link WindowExtensions#getVendorApiLevel()} 2, calling an earlier version will
     * throw {@link java.lang.NoSuchMethodError}.
     * @since {@link androidx.window.extensions.WindowExtensions#VENDOR_API_LEVEL_2}
     */
    void clearSplitInfoCallback();

    /**
     * Checks if an activity's' presentation is customized by its or any other process and only
     * occupies a portion of Task bounds.
     */
    boolean isActivityEmbedded(@NonNull Activity activity);

    /**
     * Sets a {@link SplitAttributesCalculator}.
     *
     * @param calculator the calculator to set. It will replace the previously set
     * {@link SplitAttributesCalculator} if it exists.
     * @since {@link androidx.window.extensions.WindowExtensions#VENDOR_API_LEVEL_2}
     */
    void setSplitAttributesCalculator(@NonNull SplitAttributesCalculator calculator);

    /**
     * Clears the previously set {@link SplitAttributesCalculator}.
     *
     * @see #setSplitAttributesCalculator(SplitAttributesCalculator)
     * @since {@link androidx.window.extensions.WindowExtensions#VENDOR_API_LEVEL_2}
     */
    void clearSplitAttributesCalculator();
}
