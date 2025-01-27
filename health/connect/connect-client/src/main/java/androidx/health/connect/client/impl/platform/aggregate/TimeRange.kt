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

package androidx.health.connect.client.impl.platform.aggregate

import java.time.Instant
import java.time.LocalDateTime
import java.time.temporal.Temporal

internal sealed interface TimeRange<T : Temporal> {
    val startTime: T
    val endTime: T
}

internal data class InstantTimeRange(
    override val startTime: Instant,
    override val endTime: Instant
) : TimeRange<Instant>

internal data class LocalTimeRange(
    override val startTime: LocalDateTime,
    override val endTime: LocalDateTime
) : TimeRange<LocalDateTime>
