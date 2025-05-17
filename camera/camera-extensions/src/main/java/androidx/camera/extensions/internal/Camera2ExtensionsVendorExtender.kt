/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.camera.extensions.internal

import android.content.Context
import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraExtensionCharacteristics
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.CaptureResult
import android.os.Build
import android.util.Log
import android.util.Pair
import android.util.Range
import android.util.Size
import androidx.annotation.GuardedBy
import androidx.annotation.RequiresApi
import androidx.camera.core.CameraInfo
import androidx.camera.core.impl.CameraInfoInternal
import androidx.camera.core.impl.SessionProcessor
import androidx.camera.core.impl.utils.CompareSizesByArea
import androidx.camera.core.internal.utils.SizeUtil
import androidx.camera.extensions.internal.Camera2ExtensionsUtil.convertCameraXModeToCamera2Mode
import androidx.camera.extensions.internal.sessionprocessor.Camera2ExtensionsSessionProcessor
import androidx.core.util.Preconditions

private const val TAG = "Camera2ExtExtender"

@RequiresApi(31)
public class Camera2ExtensionsVendorExtender(
    private val mode: Int,
    private val cameraManager: CameraManager,
) : VendorExtender {

    private val camera2ExtensionMode: Int = convertCameraXModeToCamera2Mode(mode)
    private val lock = Any()
    @GuardedBy("lock")
    private val cachedExtensionsCharacteristicsMap:
        MutableMap<String, CameraExtensionCharacteristics> =
        mutableMapOf()
    private lateinit var cameraId: String
    private lateinit var cameraExtensionCharacteristics: CameraExtensionCharacteristics
    private var isExtensionStrengthSupported: Boolean = false
    private var isCurrentExtensionModeSupported: Boolean = false

    override fun isExtensionAvailable(
        cameraId: String,
        characteristicsMap: Map<String, CameraCharacteristics>,
    ): Boolean {
        val extensionCharacteristics: CameraExtensionCharacteristics? =
            getCamera2ExtensionsCharacteristics(cameraId)

        if (extensionCharacteristics == null) {
            return false
        }

        return extensionCharacteristics.getSupportedExtensions().contains(camera2ExtensionMode)
    }

    private fun getCamera2ExtensionsCharacteristics(
        cameraId: String
    ): CameraExtensionCharacteristics? {
        synchronized(lock) {
            if (cachedExtensionsCharacteristicsMap.contains(cameraId)) {
                return cachedExtensionsCharacteristicsMap[cameraId]
            }

            try {
                cameraManager.getCameraExtensionCharacteristics(cameraId).let {
                    cachedExtensionsCharacteristicsMap[cameraId] = it
                    return it
                }
            } catch (e: CameraAccessException) {
                Log.e(
                    TAG,
                    "Failed to retrieve CameraExtensionCharacteristics for camera id $cameraId.",
                )
            }

            return null
        }
    }

    override fun init(cameraInfo: CameraInfo) {
        cameraId = (cameraInfo as CameraInfoInternal).getCameraId()
        cameraExtensionCharacteristics =
            Preconditions.checkNotNull(getCamera2ExtensionsCharacteristics(cameraId))

        isExtensionStrengthSupported =
            if (
                isCamera2ExtensionAvailable() &&
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE
            ) {
                cameraExtensionCharacteristics
                    .getAvailableCaptureRequestKeys(camera2ExtensionMode)
                    .contains(CaptureRequest.EXTENSION_STRENGTH)
            } else {
                false
            }

        isCurrentExtensionModeSupported =
            if (
                isCamera2ExtensionAvailable() &&
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE
            ) {
                cameraExtensionCharacteristics
                    .getAvailableCaptureResultKeys(camera2ExtensionMode)
                    .contains(CaptureResult.EXTENSION_CURRENT_TYPE)
            } else {
                false
            }
    }

    override fun getEstimatedCaptureLatencyRange(size: Size?): Range<Long>? {
        checkInitialized()

        return if (isCamera2ExtensionAvailable()) {
            cameraExtensionCharacteristics.getEstimatedCaptureLatencyRangeMillis(
                camera2ExtensionMode,
                size ?: getCamera2ExtensionsMaximumSupportedSize(),
                ImageFormat.JPEG,
            )
        } else {
            null
        }
    }

    override fun getSupportedPreviewOutputResolutions(): List<Pair<Int, Array<Size>>> {
        checkInitialized()
        return if (isCamera2ExtensionAvailable()) {
            getExtensionSupportedSizes(intArrayOf(ImageFormat.PRIVATE, ImageFormat.YUV_420_888))
        } else {
            emptyList()
        }
    }

    override fun getSupportedCaptureOutputResolutions(): List<Pair<Int, Array<Size>>> {
        checkInitialized()
        return if (isCamera2ExtensionAvailable()) {
            getExtensionSupportedSizes(
                intArrayOf(ImageFormat.JPEG, ImageFormat.YUV_420_888, ImageFormat.JPEG_R)
            )
        } else {
            emptyList()
        }
    }

    private fun getExtensionSupportedSizes(formats: IntArray): List<Pair<Int, Array<Size>>> {
        val camera2SupportedOutputSizesList = mutableListOf<Pair<Int, Array<Size>>>()

        for (format in formats) {
            if (format == ImageFormat.PRIVATE) {
                cameraExtensionCharacteristics
                    .getExtensionSupportedSizes(camera2ExtensionMode, SurfaceTexture::class.java)
                    .toTypedArray<Size>()
                    .let {
                        if (it.isNotEmpty()) {
                            camera2SupportedOutputSizesList.add(
                                Pair.create(ImageFormat.PRIVATE, it)
                            )
                        }
                    }
            } else {
                try {
                    cameraExtensionCharacteristics
                        .getExtensionSupportedSizes(camera2ExtensionMode, format)
                        .toTypedArray<Size>()
                        .let {
                            if (it.isNotEmpty()) {
                                camera2SupportedOutputSizesList.add(Pair.create(format, it))
                            }
                        }
                } catch (_: IllegalArgumentException) {
                    Log.e(TAG, "Failed to retrieve supported output sizes of format $format")
                }
            }
        }

        return camera2SupportedOutputSizesList
    }

    override fun getSupportedYuvAnalysisResolutions(): Array<Size> {
        checkInitialized()
        return super.getSupportedYuvAnalysisResolutions()
    }

    override fun getSupportedPostviewResolutions(captureSize: Size): Map<Int, List<Size>> {
        checkInitialized()

        if (!isCamera2ExtensionAvailable()) {
            return emptyMap()
        }

        val camera2SupportedPostviewResolutions = mutableMapOf<Int, List<Size>>()

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            return camera2SupportedPostviewResolutions
        }

        for (format in intArrayOf(ImageFormat.JPEG, ImageFormat.YUV_420_888, ImageFormat.JPEG_R)) {
            try {
                cameraExtensionCharacteristics
                    .getPostviewSupportedSizes(camera2ExtensionMode, captureSize, format)
                    .let {
                        if (it.isNotEmpty()) {
                            camera2SupportedPostviewResolutions.put(format, it)
                        }
                    }
            } catch (_: IllegalArgumentException) {
                Log.e(TAG, "Failed to retrieve postview supported output sizes of format $format")
            }
        }

        return camera2SupportedPostviewResolutions
    }

    override fun isPostviewAvailable(): Boolean {
        checkInitialized()
        return if (
            isCamera2ExtensionAvailable() &&
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE
        ) {
            cameraExtensionCharacteristics.isPostviewAvailable(camera2ExtensionMode)
        } else {
            false
        }
    }

    override fun isCaptureProcessProgressAvailable(): Boolean {
        checkInitialized()
        return if (
            isCamera2ExtensionAvailable() &&
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE
        ) {
            cameraExtensionCharacteristics.isCaptureProcessProgressAvailable(camera2ExtensionMode)
        } else {
            false
        }
    }

    override fun isExtensionStrengthAvailable(): Boolean {
        checkInitialized()
        return isExtensionStrengthSupported
    }

    override fun isCurrentExtensionModeAvailable(): Boolean {
        checkInitialized()
        return isCurrentExtensionModeSupported
    }

    override fun createSessionProcessor(context: Context): SessionProcessor? {
        checkInitialized()
        return Camera2ExtensionsSessionProcessor(getAvailableCaptureRequestKeys(), mode, this)
    }

    private fun getAvailableCaptureRequestKeys(): List<CaptureRequest.Key<*>> {
        val availableCaptureRequestKeys = mutableListOf<CaptureRequest.Key<*>>()

        if (!isCamera2ExtensionAvailable()) {
            return emptyList()
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            cameraExtensionCharacteristics
                .getAvailableCaptureRequestKeys(camera2ExtensionMode)
                .forEach { availableCaptureRequestKeys.add(it) }
        }

        return availableCaptureRequestKeys
    }

    @Suppress("UNCHECKED_CAST")
    override fun getSupportedCaptureResultKeys(): List<CaptureResult.Key<*>> {
        checkInitialized()
        return if (
            isCamera2ExtensionAvailable() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
        )
            cameraExtensionCharacteristics
                .getAvailableCaptureResultKeys(camera2ExtensionMode)
                .toList()
        else emptyList()
    }

    override fun willReceiveOnCaptureCompleted(): Boolean {
        return super.willReceiveOnCaptureCompleted()
    }

    override fun getAvailableCharacteristicsKeyValues():
        List<Pair<CameraCharacteristics.Key<*>, Any>> {
        checkInitialized()
        return mutableListOf<Pair<CameraCharacteristics.Key<*>, Any>>().apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                cameraExtensionCharacteristics.getKeys(camera2ExtensionMode).forEach { key ->
                    cameraExtensionCharacteristics.get(camera2ExtensionMode, key)?.let { value ->
                        add(Pair.create<CameraCharacteristics.Key<*>, Any>(key, value))
                    }
                }
            }
        }
    }

    private fun getCamera2ExtensionsMaximumSupportedSize(): Size {
        val supportedSizes =
            cameraExtensionCharacteristics.getExtensionSupportedSizes(
                camera2ExtensionMode,
                ImageFormat.JPEG,
            )
        return if (supportedSizes.isEmpty()) {
            SizeUtil.RESOLUTION_ZERO
        } else {
            supportedSizes.maxWith(CompareSizesByArea(true))
        }
    }

    private fun checkInitialized() =
        Preconditions.checkState(
            ::cameraId.isInitialized,
            "VendorExtender#init() must be called first",
        )

    private fun isCamera2ExtensionAvailable(): Boolean =
        cameraExtensionCharacteristics.supportedExtensions.contains(camera2ExtensionMode)
}
