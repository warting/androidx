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
import androidx.health.connect.client.impl.platform.records.toPlatformRecord
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.units.Percentage
import java.time.Instant
import java.time.ZoneOffset

/**
 * Captures the amount of oxygen circulating in the blood, measured as a percentage of
 * oxygen-saturated hemoglobin. Each record represents a single blood oxygen saturation reading at
 * the time of measurement.
 */
public class OxygenSaturationRecord(
    override val time: Instant,
    override val zoneOffset: ZoneOffset?,
    /** Percentage. Required field. Valid range: 0-100. */
    public val percentage: Percentage,
    override val metadata: Metadata,
) : InstantaneousRecord {

    /*
     * Android U devices and later use the platform's validation instead of Jetpack validation.
     * See b/400965398 for more context.
     */
    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            this.toPlatformRecord()
        } else {
            requireNonNegative(value = percentage.value, name = "percentage")
            percentage.value.requireNotMore(other = 100.0, name = "percentage")
        }
    }

    /*
     * Generated by the IDE: Code -> Generate -> "equals() and hashCode()".
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is OxygenSaturationRecord) return false

        if (percentage != other.percentage) return false
        if (time != other.time) return false
        if (zoneOffset != other.zoneOffset) return false
        if (metadata != other.metadata) return false

        return true
    }

    /*
     * Generated by the IDE: Code -> Generate -> "equals() and hashCode()".
     */
    override fun hashCode(): Int {
        var result = percentage.hashCode()
        result = 31 * result + time.hashCode()
        result = 31 * result + (zoneOffset?.hashCode() ?: 0)
        result = 31 * result + metadata.hashCode()
        return result
    }

    override fun toString(): String {
        return "OxygenSaturationRecord(time=$time, zoneOffset=$zoneOffset, percentage=$percentage, metadata=$metadata)"
    }
}
