/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.xr.runtime.internal

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ConfigTest {

    @Test
    fun equals_sameInstance_returnsTrue() {
        val config =
            Config(
                planeTracking = Config.PlaneTrackingMode.HorizontalAndVertical,
                handTracking = Config.HandTrackingMode.Enabled,
            )

        assertThat(config).isEqualTo(config)
    }

    @Test
    fun equals_sameConfig_returnsTrue() {
        val config1 =
            Config(
                planeTracking = Config.PlaneTrackingMode.HorizontalAndVertical,
                handTracking = Config.HandTrackingMode.Enabled,
            )
        val config2 =
            Config(
                planeTracking = Config.PlaneTrackingMode.HorizontalAndVertical,
                handTracking = Config.HandTrackingMode.Enabled,
            )

        assertThat(config1).isEqualTo(config2)
    }

    @Test
    fun equals_differentPlaneTracking_returnsFalse() {
        val config1 = Config(planeTracking = Config.PlaneTrackingMode.HorizontalAndVertical)
        val config2 = Config(planeTracking = Config.PlaneTrackingMode.Disabled)

        assertThat(config1).isNotEqualTo(config2)
    }

    @Test
    fun equals_differentHandTracking_returnsFalse() {
        val config1 = Config(handTracking = Config.HandTrackingMode.Enabled)
        val config2 = Config(handTracking = Config.HandTrackingMode.Disabled)

        assertThat(config1).isNotEqualTo(config2)
    }

    @Test
    fun equals_differentDepthEstimation_returnsFalse() {
        val config1 = Config(depthEstimation = Config.DepthEstimationMode.Enabled)
        val config2 = Config(depthEstimation = Config.DepthEstimationMode.Disabled)

        assertThat(config1).isNotEqualTo(config2)
    }

    @Test
    fun equals_differentAnchorPersistence_returnsFalse() {
        val config1 = Config(anchorPersistence = Config.AnchorPersistenceMode.Enabled)

        val config2 = Config(anchorPersistence = Config.AnchorPersistenceMode.Disabled)

        assertThat(config1).isNotEqualTo(config2)
    }

    @Test
    fun hashCode_sameConfig_returnsSameHashCode() {
        val config1 =
            Config(
                planeTracking = Config.PlaneTrackingMode.HorizontalAndVertical,
                handTracking = Config.HandTrackingMode.Enabled,
            )
        val config2 =
            Config(
                planeTracking = Config.PlaneTrackingMode.HorizontalAndVertical,
                handTracking = Config.HandTrackingMode.Enabled,
            )

        assertThat(config1.hashCode()).isEqualTo(config2.hashCode())
    }

    @Test
    fun hashCode_differentConfig_returnsDifferentHashCode() {
        val config1 =
            Config(
                planeTracking = Config.PlaneTrackingMode.HorizontalAndVertical,
                handTracking = Config.HandTrackingMode.Enabled,
            )
        val config2 =
            Config(
                planeTracking = Config.PlaneTrackingMode.Disabled,
                handTracking = Config.HandTrackingMode.Enabled,
            )

        assertThat(config1.hashCode()).isNotEqualTo(config2.hashCode())
    }

    @Test
    fun copy_createsNewInstanceWithSameValues() {
        val config =
            Config(
                planeTracking = Config.PlaneTrackingMode.HorizontalAndVertical,
                handTracking = Config.HandTrackingMode.Enabled,
            )

        val copy = config.copy()

        assertThat(copy).isEqualTo(config)
        assertThat(copy).isNotSameInstanceAs(config)
    }

    @Test
    fun copy_withDifferentValues_createsNewInstanceWithSameValues() {
        val config =
            Config(
                planeTracking = Config.PlaneTrackingMode.Disabled,
                handTracking = Config.HandTrackingMode.Disabled,
            )
        val copy =
            config.copy(
                planeTracking = Config.PlaneTrackingMode.HorizontalAndVertical,
                handTracking = Config.HandTrackingMode.Enabled,
            )

        assertThat(copy).isNotEqualTo(config)
        assertThat(copy).isNotSameInstanceAs(config)
    }

    @Test
    fun copy_withDefaultValues_createsNewInstanceWithSameValues() {
        val config = Config()
        val copy = config.copy()

        assertThat(copy).isEqualTo(config)
        assertThat(copy).isNotSameInstanceAs(config)
    }
}
