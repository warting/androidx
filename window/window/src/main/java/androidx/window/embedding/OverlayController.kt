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

package androidx.window.embedding

import android.content.Context
import android.os.Bundle
import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import androidx.window.RequiresWindowSdkExtension
import androidx.window.WindowSdkExtensions

// TODO(b/295804279): Un-hide after APIs are ready
/**
 * The controller to manage overlay [ActivityStack], which is launched by
 * the activityOptions that [setOverlayCreateParams].
 *
 * See linked sample below for how to launch an [android.app.Activity] into an overlay
 * [ActivityStack].
 *
 * Supported operations are:
 * - [setOverlayAttributesCalculator] to update overlay presentation with device or window state and
 *   [OverlayCreateParams.tag].
 *
 * @sample androidx.window.samples.embedding.launchOverlayActivityStackSample
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class OverlayController @VisibleForTesting internal constructor(
    private val backend: EmbeddingBackend
) {

    @RequiresWindowSdkExtension(5)
    internal fun setOverlayCreateParams(
        options: Bundle,
        overlayCreateParams: OverlayCreateParams,
    ): Bundle = backend.setOverlayCreateParams(options, overlayCreateParams)

    /**
     * Sets an overlay calculator function to update overlay presentation with device or window
     * state and [OverlayCreateParams.tag].
     *
     * Overlay calculator function is triggered with following scenarios:
     * - An overlay [ActivityStack] is launched.
     * - The parent task configuration changes. i.e. orientation change, enter/exit multi-window
     *   mode or resize apps in multi-window mode.
     * - Device folding state changes.
     * - Device is attached to an external display and the app is forwarded to that display.
     *
     * If there's no [calculator] set, the overlay presentation will be calculated with
     * the previous set [OverlayAttributes], either from [OverlayCreateParams] to initialize
     * the overlay container, or from the runtime API to update the overlay container's
     * [OverlayAttributes].
     *
     * See the sample linked below for how to use [OverlayAttributes] calculator
     *
     * @param calculator The overlay calculator function to compute [OverlayAttributes] by
     *   [OverlayAttributesCalculatorParams]. It will replace the previously set if it exists.
     * @throws UnsupportedOperationException if [WindowSdkExtensions.extensionVersion]
     *   is less than 5.
     * @sample androidx.window.samples.embedding.overlayAttributesCalculatorSample
     */
    @RequiresWindowSdkExtension(5)
    fun setOverlayAttributesCalculator(
        calculator: (OverlayAttributesCalculatorParams) -> OverlayAttributes
    ) {
        backend.setOverlayAttributesCalculator(calculator)
    }

    /**
     * Clears the overlay calculator function previously set by [setOverlayAttributesCalculator].
     *
     * @throws UnsupportedOperationException if [WindowSdkExtensions.extensionVersion]
     *                                       is less than 5.
     */
    @RequiresWindowSdkExtension(5)
    fun clearOverlayAttributesCalculator() {
        backend.clearOverlayAttributesCalculator()
    }

    companion object {
        /**
         * Obtains an instance of [OverlayController].
         *
         * @param context the [Context] to initialize the controller with
         */
        @JvmStatic
        fun getInstance(context: Context): OverlayController {
            val backend = EmbeddingBackend.getInstance(context)
            return OverlayController(backend)
        }
    }
}
