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

import androidx.health.services.client.data.ExerciseTrackedStatus.Companion.toProto
import androidx.health.services.client.proto.DataProto

/** High-level info about the exercise. */
@Suppress("ParcelCreator")
public class ExerciseInfo(
    /** Returns the [ExerciseTrackedStatus]. */
    @ExerciseTrackedStatus public val exerciseTrackedStatus: Int,

    /**
     * Returns the [ExerciseType] of the active exercise, or [ExerciseType.UNKNOWN] if there is no
     * active exercise.
     */
    public val exerciseType: ExerciseType,
) {

    internal constructor(
        proto: DataProto.ExerciseInfo
    ) : this(
        ExerciseTrackedStatus.fromProto(proto.exerciseTrackedStatus),
        ExerciseType.fromProto(proto.exerciseType),
    )

    internal val proto: DataProto.ExerciseInfo =
        DataProto.ExerciseInfo.newBuilder()
            .setExerciseTrackedStatus(exerciseTrackedStatus.toProto())
            .setExerciseType(exerciseType.toProto())
            .build()
}
