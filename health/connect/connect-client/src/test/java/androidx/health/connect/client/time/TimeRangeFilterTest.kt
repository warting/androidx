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
package androidx.health.connect.client.time

import androidx.test.ext.junit.runners.AndroidJUnit4
import java.time.Instant
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TimeRangeFilterTest {

    @Test
    fun checksStartTimeBeforeEndTime() {
        assertFailsWith<IllegalArgumentException> {
            TimeRangeFilter.between(
                endTime = Instant.ofEpochMilli(1234L),
                startTime = Instant.ofEpochMilli(5679L),
            )
        }
        assertFailsWith<IllegalArgumentException> {
            TimeRangeFilter.between(
                startTime = Instant.ofEpochMilli(1234L),
                endTime = Instant.ofEpochMilli(1234L),
            )
        }
        TimeRangeFilter.between(
            startTime = Instant.ofEpochMilli(1234L),
            endTime = Instant.ofEpochMilli(5679L),
        )
    }

    @Test
    fun checksLocalStartTimeBeforeEndTime() {
        assertFailsWith<IllegalArgumentException> {
            TimeRangeFilter.between(
                startTime = LocalDateTime.parse("2021-02-01T02:00:00"),
                endTime = LocalDateTime.parse("2021-02-01T01:00:00"),
            )
        }
        assertFailsWith<IllegalArgumentException> {
            TimeRangeFilter.between(
                startTime = LocalDateTime.parse("2021-02-01T02:00:00"),
                endTime = LocalDateTime.parse("2021-02-01T02:00:00"),
            )
        }
        TimeRangeFilter.between(
            startTime = LocalDateTime.parse("2021-02-01T01:00:00"),
            endTime = LocalDateTime.parse("2021-02-01T02:00:00"),
        )
    }

    @Test
    fun equals() {
        assertEquals(
            TimeRangeFilter.between(
                startTime = Instant.ofEpochMilli(1234L),
                endTime = Instant.ofEpochMilli(5679L),
            ),
            TimeRangeFilter.between(
                startTime = Instant.ofEpochMilli(1234L),
                endTime = Instant.ofEpochMilli(5679L),
            ),
        )
        assertEquals(
            TimeRangeFilter.between(
                startTime = LocalDateTime.parse("2021-02-01T01:00:00"),
                endTime = LocalDateTime.parse("2021-02-01T02:00:00"),
            ),
            TimeRangeFilter.between(
                startTime = LocalDateTime.parse("2021-02-01T01:00:00"),
                endTime = LocalDateTime.parse("2021-02-01T02:00:00"),
            ),
        )

        assertNotEquals(
            TimeRangeFilter.between(
                startTime = Instant.ofEpochMilli(1234L),
                endTime = Instant.ofEpochMilli(5678L),
            ),
            TimeRangeFilter.between(
                startTime = Instant.ofEpochMilli(1234L),
                endTime = Instant.ofEpochMilli(5679L),
            ),
        )
        assertNotEquals(
            TimeRangeFilter.between(
                startTime = LocalDateTime.parse("2021-02-01T01:00:00"),
                endTime = LocalDateTime.parse("2021-02-01T02:00:00"),
            ),
            TimeRangeFilter.between(
                startTime = LocalDateTime.parse("2021-02-01T01:30:00"),
                endTime = LocalDateTime.parse("2021-02-01T02:00:00"),
            ),
        )
    }
}
