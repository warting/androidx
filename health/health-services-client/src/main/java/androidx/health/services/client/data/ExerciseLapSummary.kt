/*
 * Copyright (C) 2021 The Android Open Source Project
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

import androidx.health.services.client.proto.DataProto
import androidx.health.services.client.proto.DataProto.ExerciseLapSummary.LapMetricsEntry
import java.time.Duration
import java.time.Instant

/** Describes a completed exercise lap. */
@Suppress("ParcelCreator")
class ExerciseLapSummary(
    /** Returns the lap count of this summary. Lap count starts at 1 for the first lap. */
    val lapCount: Int,

    /** Returns the time at which the lap has started. */
    val startTime: Instant,

    /** Returns the time at which the lap has ended. */
    val endTime: Instant,

    /**
     * Returns the total elapsed time for which the exercise has been active during this lap, i.e.
     * started but not paused.
     */
    val activeDuration: Duration,

    /**
     * Returns the [DataPoint]s for each metric keyed by [DataType] tracked between [startTime] and
     * [endTime] i.e. during the duration of this lap. This will only contain [AggregateDataType]s
     * calculated over the duration of the lap.
     */
    val lapMetrics: DataPointContainer,
) {

    internal constructor(
        proto: DataProto.ExerciseLapSummary
    ) : this(
        proto.lapCount,
        Instant.ofEpochMilli(proto.startTimeEpochMs),
        Instant.ofEpochMilli(proto.endTimeEpochMs),
        Duration.ofMillis(proto.activeDurationMs),
        DataPointContainer(proto.lapMetricsList.map { DataPoint.fromProto(it.aggregateDataPoint) }),
    )

    internal val proto: DataProto.ExerciseLapSummary =
        DataProto.ExerciseLapSummary.newBuilder()
            .setLapCount(lapCount)
            .setStartTimeEpochMs(startTime.toEpochMilli())
            .setEndTimeEpochMs(endTime.toEpochMilli())
            .setActiveDurationMs(activeDuration.toMillis())
            .addAllLapMetrics(
                lapMetrics.statisticalDataPoints
                    .map {
                        LapMetricsEntry.newBuilder()
                            .setDataType(it.dataType.proto)
                            .setAggregateDataPoint(it.proto)
                            .build()
                    }
                    .sortedBy { it.dataType.name } // Required to ensure equals() works
            )
            .addAllLapMetrics(
                lapMetrics.cumulativeDataPoints
                    .map {
                        LapMetricsEntry.newBuilder()
                            .setDataType(it.dataType.proto)
                            .setAggregateDataPoint(it.proto)
                            .build()
                    }
                    .sortedBy { it.dataType.name } // Required to ensure equals() works
            )
            .build()

    override fun toString(): String =
        "ExerciseLapSummary(" +
            "lapCount=$lapCount, " +
            "startTime=$startTime, " +
            "endTime=$endTime, " +
            "activeDuration=$activeDuration, " +
            "lapMetrics=$lapMetrics)"
}
