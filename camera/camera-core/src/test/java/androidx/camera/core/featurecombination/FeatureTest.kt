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

package androidx.camera.core.featurecombination

import androidx.camera.core.DynamicRange
import androidx.camera.core.ImageCapture
import androidx.camera.core.featurecombination.Feature.Companion.FEATURE_TYPE_DYNAMIC_RANGE
import androidx.camera.core.featurecombination.Feature.Companion.FEATURE_TYPE_FPS_RANGE
import androidx.camera.core.featurecombination.Feature.Companion.FEATURE_TYPE_IMAGE_FORMAT
import androidx.camera.core.featurecombination.Feature.Companion.FEATURE_TYPE_VIDEO_STABILIZATION
import androidx.camera.core.featurecombination.Feature.Companion.FPS_60
import androidx.camera.core.featurecombination.Feature.Companion.HDR_HLG10
import androidx.camera.core.featurecombination.Feature.Companion.IMAGE_ULTRA_HDR
import androidx.camera.core.featurecombination.Feature.Companion.PREVIEW_STABILIZATION
import androidx.camera.core.featurecombination.impl.feature.DynamicRangeFeature
import androidx.camera.core.featurecombination.impl.feature.FpsRangeFeature
import androidx.camera.core.featurecombination.impl.feature.ImageFormatFeature
import androidx.camera.core.featurecombination.impl.feature.VideoStabilizationFeature
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
@SdkSuppress(minSdkVersion = 21)
class FeatureTest {
    @Test
    fun objectHlg10_dynamicRangeIsHlg10Bit() {
        assertThat((HDR_HLG10 as DynamicRangeFeature).dynamicRange)
            .isEqualTo(DynamicRange.HLG_10_BIT)
    }

    @Test
    fun objectHlg10_featureTypeIsDynamicRange() {
        assertThat(HDR_HLG10.getFeatureType()).isEqualTo(FEATURE_TYPE_DYNAMIC_RANGE)
    }

    @Test
    fun objectFps60_minAndMaxFpsAre60() {
        val fps60Feature = (FPS_60 as FpsRangeFeature)
        assertThat(fps60Feature.minFps).isEqualTo(60)
        assertThat(fps60Feature.maxFps).isEqualTo(60)
    }

    @Test
    fun objectFps60_featureTypeIsFpsRange() {
        assertThat(FPS_60.getFeatureType()).isEqualTo(FEATURE_TYPE_FPS_RANGE)
    }

    @Test
    fun objectImageUltraHdr_outputFormatIsJpegUltraHdr() {
        assertThat((IMAGE_ULTRA_HDR as ImageFormatFeature).imageCaptureOutputFormat)
            .isEqualTo(ImageCapture.OUTPUT_FORMAT_JPEG_ULTRA_HDR)
    }

    @Test
    fun objectImageUltraHdr_featureTypeIsImageFormat() {
        assertThat(IMAGE_ULTRA_HDR.getFeatureType()).isEqualTo(FEATURE_TYPE_IMAGE_FORMAT)
    }

    @Test
    fun objectPreviewStabilization_stabilizationModeIsPreview() {
        assertThat((PREVIEW_STABILIZATION as VideoStabilizationFeature).mode)
            .isEqualTo(VideoStabilizationFeature.StabilizationMode.PREVIEW)
    }

    @Test
    fun objectPreviewStabilization_featureTypeIsVideoStabilization() {
        assertThat(PREVIEW_STABILIZATION.getFeatureType())
            .isEqualTo(FEATURE_TYPE_VIDEO_STABILIZATION)
    }
}
