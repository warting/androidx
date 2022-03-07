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
package androidx.health.data.client.aggregate

import androidx.annotation.RestrictTo

/**
 * A aggregate metric identifier with value of type [Long].
 *
 * See [AggregateDataRow.getMetric].
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
class LongAggregateMetric
internal constructor(
    override val dataTypeName: String,
    override val aggregationSuffix: String,
    override val fieldName: String? = null
) : AggregateMetric
