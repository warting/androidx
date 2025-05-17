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
import java.time.Instant
import java.time.ZoneOffset

/**
 * Captures user's heart rate variability (HRV) as measured by the root mean square of successive
 * differences (RMSSD) between normal heartbeats.
 */
public class HeartRateVariabilityRmssdRecord(
    override val time: Instant,
    override val zoneOffset: ZoneOffset?,
    /** Heart rate variability in milliseconds. Required field. Valid Range: 1-200. */
    public val heartRateVariabilityMillis: Double,
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
            heartRateVariabilityMillis.requireInRange(
                min = MIN_HRV_RMSSD,
                max = MAX_HRV_RMSSD,
                name = "heartRateVariabilityMillis",
            )
        }
    }

    internal companion object {
        internal const val MIN_HRV_RMSSD = 1.0
        internal const val MAX_HRV_RMSSD = 200.0
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is HeartRateVariabilityRmssdRecord) return false

        if (heartRateVariabilityMillis != other.heartRateVariabilityMillis) return false
        if (time != other.time) return false
        if (zoneOffset != other.zoneOffset) return false
        if (metadata != other.metadata) return false

        return true
    }

    override fun hashCode(): Int {
        var result = 0
        result = 31 * result + heartRateVariabilityMillis.hashCode()
        result = 31 * result + time.hashCode()
        result = 31 * result + (zoneOffset?.hashCode() ?: 0)
        result = 31 * result + metadata.hashCode()
        return result
    }

    override fun toString(): String {
        return "HeartRateVariabilityRmssdRecord(time=$time, zoneOffset=$zoneOffset, heartRateVariabilityMillis=$heartRateVariabilityMillis, metadata=$metadata)"
    }
}
