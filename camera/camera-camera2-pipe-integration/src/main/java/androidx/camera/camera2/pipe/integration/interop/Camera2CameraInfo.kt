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

package androidx.camera.camera2.pipe.integration.interop

import android.hardware.camera2.CameraCharacteristics
import android.util.Pair
import androidx.annotation.RestrictTo
import androidx.camera.camera2.pipe.integration.adapter.CameraInfoAdapter.Companion.unwrapAs
import androidx.camera.camera2.pipe.integration.compat.workaround.getSafely
import androidx.camera.camera2.pipe.integration.impl.CameraProperties
import androidx.camera.core.CameraInfo
import androidx.camera.core.impl.AdapterCameraInfo

/** An interface for retrieving Camera2-related camera information. */
@ExperimentalCamera2Interop
public class Camera2CameraInfo
private constructor(
    private val cameraProperties: CameraProperties,
    private val extensionsSpecificChars: List<Pair<CameraCharacteristics.Key<*>, Any>>? = null,
) {
    @JvmSynthetic @JvmField public val cameraId: String = cameraProperties.cameraId.value

    /**
     * Gets a camera characteristic value.
     *
     * The characteristic value is the same as the value in the [CameraCharacteristics] that would
     * be obtained from [android.hardware.camera2.CameraManager.getCameraCharacteristics].
     *
     * @param <T> The type of the characteristic value.
     * @param key The [CameraCharacteristics.Key] of the characteristic.
     * @return the value of the characteristic. </T>
     */
    public fun <T> getCameraCharacteristic(key: CameraCharacteristics.Key<T>): T? {
        extensionsSpecificChars?.forEach {
            if (it.first == key) {
                @Suppress("UNCHECKED_CAST")
                return it.second as T
            }
        }
        return cameraProperties.metadata.getSafely(key)
    }

    /**
     * Gets the string camera ID.
     *
     * The camera ID is the same as the camera ID that would be obtained from
     * [android.hardware.camera2.CameraManager.getCameraIdList]. The ID that is retrieved is not
     * static and can change depending on the current internal configuration of the
     * [androidx.camera.core.Camera] from which the CameraInfo was retrieved.
     *
     * The Camera is a logical camera which can be backed by multiple
     * [android.hardware.camera2.CameraDevice]. However, only one CameraDevice is active at one
     * time. When the CameraDevice changes then the camera id will change.
     *
     * @return the camera ID.
     * @throws IllegalStateException if the camera info does not contain the camera 2 camera ID
     *   (e.g., if CameraX was not initialized with a [androidx.camera.camera2.Camera2Config]).
     */
    public fun getCameraId(): String = cameraId

    public companion object {

        /**
         * Gets the [Camera2CameraInfo] from a [CameraInfo].
         *
         * If the [CameraInfo] is retrieved by an Extensions-enabled
         * [androidx.camera.core.CameraSelector], calling [getCameraCharacteristic] will return any
         * available Extensions-specific characteristics if exists.
         *
         * @param cameraInfo The [CameraInfo] to get from.
         * @return The camera information with Camera2 implementation.
         * @throws IllegalArgumentException if the camera info does not contain the camera2
         *   information (e.g., if CameraX was not initialized with a
         *   [androidx.camera.camera2.Camera2Config]).
         */
        @JvmStatic
        public fun from(cameraInfo: CameraInfo): Camera2CameraInfo {
            var camera2CameraInfo = cameraInfo.unwrapAs(Camera2CameraInfo::class)
            requireNotNull(camera2CameraInfo) {
                "Could not unwrap $cameraInfo as Camera2CameraInfo!"
            }

            if (cameraInfo is AdapterCameraInfo) {
                if (cameraInfo.sessionProcessor != null) {
                    camera2CameraInfo =
                        Camera2CameraInfo(
                            camera2CameraInfo.cameraProperties,
                            cameraInfo.sessionProcessor?.availableCharacteristicsKeyValues,
                        )
                }
            }
            return camera2CameraInfo
        }

        /** This is the workaround to prevent constructor from being added to public API. */
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        @JvmStatic
        public fun create(cameraProperties: CameraProperties): Camera2CameraInfo =
            Camera2CameraInfo(cameraProperties)
    }
}
