/*
 * Copyright (C) 2022 The Android Open Source Project
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
package androidx.health.connect.client.impl.converters.aggregate

import androidx.health.connect.client.aggregate.AggregationResult
import androidx.health.connect.client.aggregate.AggregationResultGroupedByDuration
import androidx.health.connect.client.aggregate.AggregationResultGroupedByPeriod
import androidx.health.connect.client.records.metadata.DataOrigin
import androidx.health.platform.client.proto.DataProto
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AggregationResultConverterTest {
    @Test
    fun retrieveAggregateDataRow() {
        val proto =
            DataProto.AggregateDataRow.newBuilder()
                .addDataOrigins(DataProto.DataOrigin.newBuilder().setApplicationId("testApp"))
                .putDoubleValues("doubleKey", 123.4)
                .putLongValues("longKey", 567)
                .build()

        proto
            .retrieveAggregateDataRow()
            .assertEquals(
                AggregationResult(
                    longValues = mapOf(Pair("longKey", 567L)),
                    doubleValues = mapOf(Pair("doubleKey", 123.4)),
                    dataOrigins = setOf(DataOrigin("testApp")),
                )
            )
    }

    // ZoneOffset.ofTotalSeconds() has been banned but safe here for serialization.
    @SuppressWarnings("GoodTime")
    @Test
    fun toAggregateDataRowGroupByDuration() {
        val proto =
            DataProto.AggregateDataRow.newBuilder()
                .addDataOrigins(DataProto.DataOrigin.newBuilder().setApplicationId("testApp"))
                .putDoubleValues("doubleKey", 123.4)
                .putLongValues("longKey", 567)
                .setStartTimeEpochMs(1111)
                .setEndTimeEpochMs(9999)
                .setZoneOffsetSeconds(123)
                .build()

        proto
            .toAggregateDataRowGroupByDuration()
            .assertEquals(
                AggregationResultGroupedByDuration(
                    result =
                        AggregationResult(
                            longValues = mapOf(Pair("longKey", 567L)),
                            doubleValues = mapOf(Pair("doubleKey", 123.4)),
                            dataOrigins = setOf(DataOrigin("testApp")),
                        ),
                    startTime = Instant.ofEpochMilli(1111),
                    endTime = Instant.ofEpochMilli(9999),
                    zoneOffset = ZoneOffset.ofTotalSeconds(123),
                )
            )
    }

    @Test
    fun toAggregateDataRowGroupByDuration_startOrEndTimeNotSet_throws() {
        val proto =
            DataProto.AggregateDataRow.newBuilder()
                .addDataOrigins(DataProto.DataOrigin.newBuilder().setApplicationId("testApp"))
                .putDoubleValues("doubleKey", 123.4)
                .putLongValues("longKey", 567)
                .setStartTimeEpochMs(1111)
                .setEndTimeEpochMs(9999)
                .setZoneOffsetSeconds(123)
                .build()

        var thrown =
            assertThrows(IllegalArgumentException::class.java) {
                proto
                    .toBuilder()
                    .clearStartTimeEpochMs()
                    .build()
                    .toAggregateDataRowGroupByDuration()
            }
        assertThat(thrown.message).isEqualTo("start time must be set")
        thrown =
            assertThrows(IllegalArgumentException::class.java) {
                proto.toBuilder().clearEndTimeEpochMs().build().toAggregateDataRowGroupByDuration()
            }
        assertThat(thrown.message).isEqualTo("end time must be set")
    }

    @Test
    fun toAggregateDataRowGroupByPeriod() {
        val proto =
            DataProto.AggregateDataRow.newBuilder()
                .addDataOrigins(DataProto.DataOrigin.newBuilder().setApplicationId("testApp"))
                .putDoubleValues("doubleKey", 123.4)
                .putLongValues("longKey", 567)
                .setStartLocalDateTime("2022-02-11T20:22:02")
                .setEndLocalDateTime("2022-02-22T20:22:02")
                .build()

        proto
            .toAggregateDataRowGroupByPeriod()
            .assertEquals(
                AggregationResultGroupedByPeriod(
                    result =
                        AggregationResult(
                            longValues = mapOf(Pair("longKey", 567L)),
                            doubleValues = mapOf(Pair("doubleKey", 123.4)),
                            dataOrigins = setOf(DataOrigin("testApp")),
                        ),
                    startTime = LocalDateTime.parse("2022-02-11T20:22:02"),
                    endTime = LocalDateTime.parse("2022-02-22T20:22:02"),
                )
            )
    }

    @Test
    fun toAggregateDataRowGroupByPeriod_startOrEndTimeNotSet_throws() {
        val proto =
            DataProto.AggregateDataRow.newBuilder()
                .addDataOrigins(DataProto.DataOrigin.newBuilder().setApplicationId("testApp"))
                .putDoubleValues("doubleKey", 123.4)
                .putLongValues("longKey", 567)
                .setStartLocalDateTime("2022-02-11T20:22:02")
                .setEndLocalDateTime("2022-02-12T20:22:02")
                .build()

        var thrown =
            assertThrows(IllegalArgumentException::class.java) {
                proto
                    .toBuilder()
                    .clearStartLocalDateTime()
                    .build()
                    .toAggregateDataRowGroupByPeriod()
            }
        assertThat(thrown.message).isEqualTo("start time must be set")
        thrown =
            assertThrows(IllegalArgumentException::class.java) {
                proto.toBuilder().clearEndLocalDateTime().build().toAggregateDataRowGroupByPeriod()
            }
        assertThat(thrown.message).isEqualTo("end time must be set")
    }

    private fun AggregationResult.assertEquals(expected: AggregationResult) {
        assertThat(longValues).isEqualTo(expected.longValues)
        assertThat(doubleValues).isEqualTo(expected.doubleValues)
        assertThat(dataOrigins).isEqualTo(expected.dataOrigins)
    }

    // ZoneOffset.ofTotalSeconds() has been banned but safe here for serialization.
    @SuppressWarnings("GoodTime")
    private fun AggregationResultGroupedByDuration.assertEquals(
        expected: AggregationResultGroupedByDuration
    ) {
        result.assertEquals(expected.result)
        assertThat(startTime).isEqualTo(expected.startTime)
        assertThat(endTime).isEqualTo(expected.endTime)
        assertThat(zoneOffset).isEqualTo(expected.zoneOffset)
    }

    private fun AggregationResultGroupedByPeriod.assertEquals(
        expected: AggregationResultGroupedByPeriod
    ) {
        result.assertEquals(expected.result)
        assertThat(startTime).isEqualTo(expected.startTime)
        assertThat(endTime).isEqualTo(expected.endTime)
    }
}
