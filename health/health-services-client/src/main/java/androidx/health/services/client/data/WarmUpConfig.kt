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

/**
 * Defines configuration for an exercise tracked using Health Services.
 *
 * @property exerciseType The active [ExerciseType] user is performing for this exercise
 * @property dataTypes [DeltaDataType]s which should be tracked during this exercise
 * @constructor Creates a new WarmUpConfig for an exercise tracked using Health Services
 */
@Suppress("ParcelCreator")
public class WarmUpConfig(
    public val exerciseType: ExerciseType,
    public val dataTypes: Set<DeltaDataType<*, *>>,
) {

    internal constructor(
        proto: DataProto.WarmUpConfig
    ) : this(
        ExerciseType.fromProto(proto.exerciseType),
        proto.dataTypesList.map { DataType.deltaFromProto(it) }.toSet(),
    )

    init {
        require(dataTypes.isNotEmpty()) { "Must specify the desired data types." }
    }

    internal val proto: DataProto.WarmUpConfig =
        DataProto.WarmUpConfig.newBuilder()
            .setExerciseType(exerciseType.toProto())
            .addAllDataTypes(dataTypes.map { it.proto })
            .build()

    override fun toString(): String =
        "WarmUpConfig(exerciseType=$exerciseType, dataTypes=$dataTypes)"
}
