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

package androidx.camera.video

import android.media.CamcorderProfile.QUALITY_2160P
import android.media.CamcorderProfile.QUALITY_720P
import android.media.CamcorderProfile.QUALITY_HIGH
import android.media.CamcorderProfile.QUALITY_HIGH_SPEED_2160P
import android.media.CamcorderProfile.QUALITY_HIGH_SPEED_720P
import android.media.CamcorderProfile.QUALITY_HIGH_SPEED_HIGH
import android.media.CamcorderProfile.QUALITY_HIGH_SPEED_LOW
import android.media.CamcorderProfile.QUALITY_LOW
import androidx.camera.core.impl.EncoderProfilesProvider
import androidx.camera.testing.impl.EncoderProfilesUtil.PROFILES_2160P
import androidx.camera.testing.impl.EncoderProfilesUtil.PROFILES_720P
import androidx.camera.testing.impl.EncoderProfilesUtil.PROFILES_HIGH_SPEED_2160P
import androidx.camera.testing.impl.EncoderProfilesUtil.PROFILES_HIGH_SPEED_720P
import androidx.camera.testing.impl.EncoderProfilesUtil.RESOLUTION_1080P
import androidx.camera.testing.impl.EncoderProfilesUtil.RESOLUTION_2160P
import androidx.camera.testing.impl.EncoderProfilesUtil.RESOLUTION_480P
import androidx.camera.testing.impl.EncoderProfilesUtil.RESOLUTION_4KDCI
import androidx.camera.testing.impl.EncoderProfilesUtil.RESOLUTION_720P
import androidx.camera.testing.impl.EncoderProfilesUtil.RESOLUTION_QVGA
import androidx.camera.testing.impl.fakes.FakeEncoderProfilesProvider
import androidx.camera.video.Quality.FHD
import androidx.camera.video.Quality.HD
import androidx.camera.video.Quality.HIGHEST
import androidx.camera.video.Quality.LOWEST
import androidx.camera.video.Quality.QUALITY_SOURCE_HIGH_SPEED
import androidx.camera.video.Quality.QUALITY_SOURCE_REGULAR
import androidx.camera.video.Quality.SD
import androidx.camera.video.Quality.UHD
import androidx.camera.video.internal.VideoValidatedEncoderProfilesProxy
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.internal.DoNotInstrument

@RunWith(ParameterizedRobolectricTestRunner::class)
@DoNotInstrument
class CapabilitiesByQualityTest(private val qualitySource: Int) {

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "isHighSpeed={0}")
        fun data() = listOf(arrayOf(QUALITY_SOURCE_REGULAR), arrayOf(QUALITY_SOURCE_HIGH_SPEED))
    }

    private val profile2160p =
        if (qualitySource == QUALITY_SOURCE_HIGH_SPEED) PROFILES_HIGH_SPEED_2160P
        else PROFILES_2160P
    private val profile720p =
        if (qualitySource == QUALITY_SOURCE_HIGH_SPEED) PROFILES_HIGH_SPEED_720P else PROFILES_720P

    private val encoderProfilesProvider =
        FakeEncoderProfilesProvider.Builder()
            .apply {
                if (qualitySource == QUALITY_SOURCE_HIGH_SPEED) {
                    add(QUALITY_HIGH_SPEED_HIGH, profile2160p)
                    add(QUALITY_HIGH_SPEED_2160P, profile2160p) // UHD
                    add(QUALITY_HIGH_SPEED_720P, profile720p) // HD
                    add(QUALITY_HIGH_SPEED_LOW, profile720p)
                } else {
                    add(QUALITY_HIGH, profile2160p)
                    add(QUALITY_2160P, profile2160p) // UHD
                    add(QUALITY_720P, profile720p) // HD
                    add(QUALITY_LOW, profile720p)
                }
            }
            .build()

    private val capabilitiesByQuality =
        CapabilitiesByQuality(encoderProfilesProvider, qualitySource)

    @Test
    fun canGetSupportedQualities() {
        assertThat(capabilitiesByQuality.supportedQualities).containsExactly(UHD, HD)
    }

    @Test
    fun isQualitySupported_returnExpectedResults() {
        assertThat(capabilitiesByQuality.isQualitySupported(HIGHEST)).isTrue()
        assertThat(capabilitiesByQuality.isQualitySupported(LOWEST)).isTrue()
        assertThat(capabilitiesByQuality.isQualitySupported(UHD)).isTrue()
        assertThat(capabilitiesByQuality.isQualitySupported(FHD)).isFalse()
        assertThat(capabilitiesByQuality.isQualitySupported(HD)).isTrue()
        assertThat(capabilitiesByQuality.isQualitySupported(SD)).isFalse()
    }

    @Test
    fun canGetProfiles() {
        assertThat(capabilitiesByQuality.getProfiles(HIGHEST)).isNotNull()
        assertThat(capabilitiesByQuality.getProfiles(LOWEST)).isNotNull()
        assertThat(capabilitiesByQuality.getProfiles(UHD)).isNotNull()
        assertThat(capabilitiesByQuality.getProfiles(FHD)).isNull()
        assertThat(capabilitiesByQuality.getProfiles(HD)).isNotNull()
        assertThat(capabilitiesByQuality.getProfiles(SD)).isNull()
    }

    @Test
    fun canFindNearestHigherSupportedEncoderProfiles() {
        val videoValidProfile2160P = VideoValidatedEncoderProfilesProxy.from(profile2160p)
        val videoValidProfile720P = VideoValidatedEncoderProfilesProxy.from(profile720p)

        assertThat(
                capabilitiesByQuality.findNearestHigherSupportedEncoderProfilesFor(RESOLUTION_4KDCI)
            )
            .isEqualTo(videoValidProfile2160P)
        assertThat(
                capabilitiesByQuality.findNearestHigherSupportedEncoderProfilesFor(RESOLUTION_2160P)
            )
            .isEqualTo(videoValidProfile2160P)
        assertThat(
                capabilitiesByQuality.findNearestHigherSupportedEncoderProfilesFor(RESOLUTION_1080P)
            )
            .isEqualTo(videoValidProfile2160P)
        assertThat(
                capabilitiesByQuality.findNearestHigherSupportedEncoderProfilesFor(RESOLUTION_720P)
            )
            .isEqualTo(videoValidProfile720P)
        assertThat(
                capabilitiesByQuality.findNearestHigherSupportedEncoderProfilesFor(RESOLUTION_480P)
            )
            .isEqualTo(videoValidProfile720P)
        assertThat(
                capabilitiesByQuality.findNearestHigherSupportedEncoderProfilesFor(RESOLUTION_QVGA)
            )
            .isEqualTo(videoValidProfile720P)
    }

    @Test
    fun canFindNearestHigherSupportedQuality() {
        assertThat(capabilitiesByQuality.findNearestHigherSupportedQualityFor(RESOLUTION_4KDCI))
            .isEqualTo(UHD)
        assertThat(capabilitiesByQuality.findNearestHigherSupportedQualityFor(RESOLUTION_2160P))
            .isEqualTo(UHD)
        assertThat(capabilitiesByQuality.findNearestHigherSupportedQualityFor(RESOLUTION_1080P))
            .isEqualTo(UHD)
        assertThat(capabilitiesByQuality.findNearestHigherSupportedQualityFor(RESOLUTION_720P))
            .isEqualTo(HD)
        assertThat(capabilitiesByQuality.findNearestHigherSupportedQualityFor(RESOLUTION_480P))
            .isEqualTo(HD)
        assertThat(capabilitiesByQuality.findNearestHigherSupportedQualityFor(RESOLUTION_QVGA))
            .isEqualTo(HD)
    }

    @Test
    fun containsSupportedQuality() {
        assertThat(
                CapabilitiesByQuality.containsSupportedQuality(
                    encoderProfilesProvider,
                    qualitySource,
                )
            )
            .isTrue()

        assertThat(
                CapabilitiesByQuality.containsSupportedQuality(
                    EncoderProfilesProvider.EMPTY,
                    qualitySource,
                )
            )
            .isFalse()
    }
}
