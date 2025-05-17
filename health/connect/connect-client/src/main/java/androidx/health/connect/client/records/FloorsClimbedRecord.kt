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
package androidx.health.connect.client.records

import android.os.Build
import androidx.health.connect.client.aggregate.AggregateMetric
import androidx.health.connect.client.impl.platform.records.toPlatformRecord
import androidx.health.connect.client.records.metadata.Metadata
import java.time.Instant
import java.time.ZoneOffset

/** Captures the number of floors climbed by the user since the last reading. */
public class FloorsClimbedRecord(
    override val startTime: Instant,
    override val startZoneOffset: ZoneOffset?,
    override val endTime: Instant,
    override val endZoneOffset: ZoneOffset?,
    /** Number of floors. Required field. Valid range: 0-1000000. */
    public val floors: Double,
    override val metadata: Metadata,
) : IntervalRecord {
    /*
     * Android U devices and later use the platform's validation instead of Jetpack validation.
     * See b/400965398 for more context.
     */
    init {
        require(startTime.isBefore(endTime)) { "startTime must be before endTime." }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            this.toPlatformRecord()
        } else {
            requireNonNegative(value = floors, name = "floors")
            floors.requireNotMore(other = 1000_000.0, name = "floors")
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is FloorsClimbedRecord) return false

        if (floors != other.floors) return false
        if (startTime != other.startTime) return false
        if (startZoneOffset != other.startZoneOffset) return false
        if (endTime != other.endTime) return false
        if (endZoneOffset != other.endZoneOffset) return false
        if (metadata != other.metadata) return false

        return true
    }

    override fun hashCode(): Int {
        var result = 0
        result = 31 * result + floors.hashCode()
        result = 31 * result + (startZoneOffset?.hashCode() ?: 0)
        result = 31 * result + endTime.hashCode()
        result = 31 * result + (endZoneOffset?.hashCode() ?: 0)
        result = 31 * result + metadata.hashCode()
        return result
    }

    override fun toString(): String {
        return "FloorsClimbedRecord(startTime=$startTime, startZoneOffset=$startZoneOffset, endTime=$endTime, endZoneOffset=$endZoneOffset, floors=$floors, metadata=$metadata)"
    }

    companion object {
        /**
         * Metric identifier to retrieve the total floors climbed from
         * [androidx.health.connect.client.aggregate.AggregationResult].
         */
        @JvmField
        val FLOORS_CLIMBED_TOTAL: AggregateMetric<Double> =
            AggregateMetric.doubleMetric(
                "FloorsClimbed",
                AggregateMetric.AggregationType.TOTAL,
                "floors",
            )
    }
}
