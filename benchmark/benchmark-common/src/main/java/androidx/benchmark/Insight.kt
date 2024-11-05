/*
 * Copyright (C) 2024 The Android Open Source Project
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

package androidx.benchmark

import androidx.annotation.RestrictTo

/**
 * Represents an insight into performance issues detected during a benchmark.
 *
 * Provides details about the specific criterion that was violated, along with information about
 * where and how the violation was observed.
 *
 * @param criterion A description of the performance issue, including the expected behavior and any
 *   relevant thresholds.
 * @param observedV2 Specific details about when and how the violation occurred, such as the
 *   iterations where it was observed and any associated values. Uses [LinkFormat.V2].
 * @param observedV3 Specific details about when and how the violation occurred, such as the
 *   iterations where it was observed and any associated values. Uses [LinkFormat.V3] to link more
 *   precisely into traces.
 *
 * TODO(364598145): generalize
 * TODO: Defer string construction until InstrumentationResults output step
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class Insight(
    val criterion: String,
    val observedV2: String,
    val observedV3: String,
)
