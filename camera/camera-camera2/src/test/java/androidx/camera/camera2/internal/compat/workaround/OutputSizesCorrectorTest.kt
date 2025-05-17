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

package androidx.camera.camera2.internal.compat.workaround

import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraCharacteristics
import android.os.Build
import android.util.Size
import androidx.camera.core.impl.ImageFormatConstants
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowCameraCharacteristics
import org.robolectric.shadows.StreamConfigurationMapBuilder
import org.robolectric.util.ReflectionHelpers

private const val CAMERA_ID_0 = "0"

private const val MOTOROLA_BRAND_NAME = "motorola"
private const val MOTOROLA_E5_PLAY_MODEL_NAME = "moto e5 play"
private const val SAMSUNG_BRAND_NAME = "SAMSUNG"
private const val SAMSUNG_J7_DEVICE_NAME = "J7XELTE"

private val outputSizes =
    arrayOf(
        // Samsung J7 API 27 above excluded sizes
        Size(4128, 3096),
        Size(4128, 2322),
        Size(3088, 3088),
        Size(3264, 2448),
        Size(3264, 1836),
        Size(2048, 1536),
        Size(2048, 1152),
        Size(1920, 1080),

        // Add some other sizes
        Size(1280, 960),
        Size(1280, 720),
        Size(640, 480),
        Size(320, 240),
    )

@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
class OutputSizesCorrectorTest {
    @Test
    fun canAddExtraSupportedSizesForMotoE5PlayByFormat() {
        val outputSizesCorrector =
            createOutputSizesCorrector(
                MOTOROLA_BRAND_NAME,
                null,
                MOTOROLA_E5_PLAY_MODEL_NAME,
                CAMERA_ID_0,
                CameraCharacteristics.LENS_FACING_BACK,
                CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED,
            )

        val resultList =
            outputSizesCorrector
                .applyQuirks(
                    arrayOf(Size(4128, 3096), Size(4128, 2322), Size(3088, 3088)),
                    ImageFormatConstants.INTERNAL_DEFINED_IMAGE_FORMAT_PRIVATE,
                )
                .toList()

        assertThat(resultList)
            .containsExactlyElementsIn(
                listOf(
                    Size(4128, 3096),
                    Size(4128, 2322),
                    Size(3088, 3088),

                    // Added extra supported sizes for Motorola E5 Play device
                    Size(1440, 1080),
                    Size(960, 720),
                )
            )
            .inOrder()
    }

    @Test
    fun canAddExtraSupportedSizesForMotoE5PlayByClass() {
        val outputSizesCorrector =
            createOutputSizesCorrector(
                MOTOROLA_BRAND_NAME,
                null,
                MOTOROLA_E5_PLAY_MODEL_NAME,
                CAMERA_ID_0,
                CameraCharacteristics.LENS_FACING_BACK,
                CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED,
            )

        val resultList =
            outputSizesCorrector
                .applyQuirks(
                    arrayOf(Size(4128, 3096), Size(4128, 2322), Size(3088, 3088)),
                    SurfaceTexture::class.java,
                )
                .toList()

        assertThat(resultList)
            .containsExactlyElementsIn(
                listOf(
                    Size(4128, 3096),
                    Size(4128, 2322),
                    Size(3088, 3088),

                    // Added extra supported sizes for Motorola E5 Play device
                    Size(1440, 1080),
                    Size(960, 720),
                )
            )
            .inOrder()
    }

    @Test
    @Config(minSdk = 27)
    fun canExcludeSamsungJ7Api27AboveProblematicSizesByFormat() {
        val outputSizesCorrector =
            createOutputSizesCorrector(
                SAMSUNG_BRAND_NAME,
                SAMSUNG_J7_DEVICE_NAME,
                null,
                CAMERA_ID_0,
                CameraCharacteristics.LENS_FACING_BACK,
                CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY,
            )

        val resultList =
            mutableListOf<Size>().apply {
                outputSizesCorrector.applyQuirks(outputSizes, ImageFormat.YUV_420_888).forEach {
                    size ->
                    add(size)
                }
            }

        assertThat(resultList)
            .containsExactlyElementsIn(
                listOf(
                    Size(4128, 3096),
                    Size(4128, 2322),
                    Size(3088, 3088),
                    Size(3264, 2448),
                    Size(3264, 1836),

                    // Size(2048, 1536), Size(2048, 1152), Size(1920, 1080) are excluded for YUV
                    // format

                    Size(1280, 960),
                    Size(1280, 720),
                    Size(640, 480),
                    Size(320, 240),
                )
            )
            .inOrder()
    }

    @Test
    @Config(minSdk = 27)
    fun canExcludeSamsungJ7Api27AboveProblematicSizesByClass() {
        val outputSizesCorrector =
            createOutputSizesCorrector(
                SAMSUNG_BRAND_NAME,
                SAMSUNG_J7_DEVICE_NAME,
                null,
                CAMERA_ID_0,
                CameraCharacteristics.LENS_FACING_BACK,
                CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY,
            )

        val resultList =
            outputSizesCorrector.applyQuirks(outputSizes, SurfaceTexture::class.java).toList()

        assertThat(resultList)
            .containsExactlyElementsIn(
                listOf(
                    // Size(4128, 3096), Size(4128, 2322), Size(3088, 3088), Size(3264, 2448),
                    // Size(3264, 1836), Size(2048, 1536), Size(2048, 1152), Size(1920, 1080)
                    // are excluded for SurfaceTexture class

                    Size(1280, 960),
                    Size(1280, 720),
                    Size(640, 480),
                    Size(320, 240),
                )
            )
            .inOrder()
    }

    private fun createOutputSizesCorrector(
        brand: String,
        device: String?,
        model: String?,
        cameraId: String,
        lensFacing: Int,
        hardwareLevel: Int,
    ): OutputSizesCorrector {
        ReflectionHelpers.setStaticField(Build::class.java, "BRAND", brand)
        device?.let { ReflectionHelpers.setStaticField(Build::class.java, "DEVICE", it) }
        model?.let { ReflectionHelpers.setStaticField(Build::class.java, "MODEL", it) }

        val characteristics = ShadowCameraCharacteristics.newCameraCharacteristics()

        Shadow.extract<ShadowCameraCharacteristics>(characteristics).apply {
            set(CameraCharacteristics.LENS_FACING, lensFacing)
            set(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL, hardwareLevel)
            set(
                CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP,
                StreamConfigurationMapBuilder.newBuilder()
                    .apply { outputSizes.forEach { outputSize -> addOutputSize(outputSize) } }
                    .build(),
            )
        }

        return OutputSizesCorrector(cameraId)
    }
}
