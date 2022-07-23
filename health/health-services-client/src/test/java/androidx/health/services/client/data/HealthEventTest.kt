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

package androidx.health.services.client.data

import androidx.health.services.client.data.DataType.Companion.DISTANCE
import androidx.health.services.client.data.DataType.Companion.HEART_RATE_BPM
import androidx.health.services.client.data.HealthEvent.Type.Companion.FALL_DETECTED
import com.google.common.truth.Truth.assertThat
import java.time.Duration
import java.time.Instant
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class HealthEventTest {

    fun Int.instant() = Instant.ofEpochMilli(toLong())
    fun Int.duration() = Duration.ofSeconds(toLong())

    @Test
    fun protoRoundTripWithDataPoint() {
        val proto = HealthEvent(
            FALL_DETECTED,
            30.instant(),
            DataPointContainer(listOf(
                DataPoints.heartRate(42.0, 20.duration()),
                DataPoints.heartRate(43.0, 10.duration()),
                DataPoints.distance(180.0, 20.duration(), 40.duration()),
            ))
        ).proto

        val event = HealthEvent(proto)

        assertThat(event.type).isEqualTo(FALL_DETECTED)
        assertThat(event.eventTime).isEqualTo(30.instant())
        assertThat(event.metrics.getData(HEART_RATE_BPM)[0].value).isEqualTo(42.0)
        assertThat(event.metrics.getData(HEART_RATE_BPM)[1].value).isEqualTo(43.0)
        assertThat(event.metrics.getData(DISTANCE)[0].value).isEqualTo(180.0)
    }

    @Test
    fun protoRoundTripEmptyDataPointContainer() {
        val proto = HealthEvent(FALL_DETECTED, 30.instant(), DataPointContainer(listOf())).proto

        val event = HealthEvent(proto)

        assertThat(event.type).isEqualTo(FALL_DETECTED)
        assertThat(event.eventTime).isEqualTo(30.instant())
        assertThat(event.metrics.dataPoints).isEmpty()
    }
}