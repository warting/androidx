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
package androidx.health.connect.client.request

import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.time.Instant
import kotlin.test.assertFailsWith
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ReadRecordsRequestTest {

    private val closedTimeRange =
        TimeRangeFilter.between(Instant.ofEpochMilli(1234L), Instant.ofEpochMilli(1235L))

    @Test
    fun negativePageSize_throws() {
        assertFailsWith<IllegalArgumentException> {
            ReadRecordsRequest(
                recordType = StepsRecord::class,
                timeRangeFilter = TimeRangeFilter.after(Instant.EPOCH),
                pageSize = -1,
            )
        }
    }

    @Test
    fun zeroPageSize_throws() {
        assertFailsWith<IllegalArgumentException> {
            ReadRecordsRequest(
                recordType = StepsRecord::class,
                timeRangeFilter = TimeRangeFilter.after(Instant.EPOCH),
                pageSize = 0,
            )
        }
    }

    @Test
    fun openEndedTimeRange_success() {
        ReadRecordsRequest(
            recordType = StepsRecord::class,
            timeRangeFilter = TimeRangeFilter.after(Instant.EPOCH),
        )
    }

    @Test
    fun closedTimeRange_success() {
        ReadRecordsRequest(recordType = StepsRecord::class, timeRangeFilter = closedTimeRange)
    }

    @Test
    fun pageTokenWithPageSize_success() {
        ReadRecordsRequest(
            recordType = StepsRecord::class,
            timeRangeFilter = closedTimeRange,
            pageSize = 10,
            pageToken = "token",
        )
    }
}
