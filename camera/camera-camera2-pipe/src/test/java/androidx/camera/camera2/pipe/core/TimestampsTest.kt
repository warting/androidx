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

package androidx.camera.camera2.pipe.core

import androidx.camera.camera2.pipe.testing.RobolectricCameraPipeTestRunner
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(RobolectricCameraPipeTestRunner::class)
class TimestampsTest {

    @Test
    fun testDurationComparisons() {
        val duration1 = DurationNs(100L)
        val duration2 = DurationNs(200L)
        val duration3 = DurationNs(100L)

        assertThat(duration1 < duration2).isTrue()
        assertThat(duration1 > duration2).isFalse()
        assertThat(duration1 == duration3).isTrue()
    }

    @Test
    fun testDurationNsFromMs() {
        val duration = DurationNs.fromMs(500L)
        assertThat(duration.value).isEqualTo(500_000_000L)
    }
}
