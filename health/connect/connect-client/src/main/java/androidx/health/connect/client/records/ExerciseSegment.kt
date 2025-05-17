/*
 * Copyright 2023 The Android Open Source Project
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

import androidx.annotation.IntDef
import androidx.annotation.RestrictTo
import androidx.health.connect.client.records.ExerciseSessionRecord.ExerciseTypes
import java.time.Instant

/**
 * Represents particular exercise within an exercise session.
 *
 * <p>Each segment contains start and end time of the exercise, exercise type and optional number of
 * repetitions.
 *
 * @see ExerciseSessionRecord
 */
public class ExerciseSegment(
    public val startTime: Instant,
    public val endTime: Instant,
    /** Type of segment (e.g. biking, plank). */
    @property:ExerciseSegmentTypes public val segmentType: Int,
    /** Number of repetitions in the segment. Must be non-negative. */
    public val repetitions: Int = 0,
) {
    init {
        require(startTime.isBefore(endTime)) { "startTime must be before endTime." }
        require(repetitions >= 0) { "repetitions can not be negative." }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ExerciseSegment) return false

        if (startTime != other.startTime) return false
        if (endTime != other.endTime) return false
        if (segmentType != other.segmentType) return false
        if (repetitions != other.repetitions) return false

        return true
    }

    override fun hashCode(): Int {
        var result = 0
        result = 31 * result + startTime.hashCode()
        result = 31 * result + endTime.hashCode()
        result = 31 * result + segmentType.hashCode()
        result = 31 * result + repetitions.hashCode()
        return result
    }

    override fun toString(): String {
        return "ExerciseSegment(startTime=$startTime, endTime=$endTime, segmentType=$segmentType, repetitions=$repetitions)"
    }

    companion object {
        /**
         * Is a segment type compatible with a session type.
         *
         * <p>For example, a swimming session can contain [EXERCISE_SEGMENT_TYPE_SWIMMING_FREESTYLE]
         * segments, but can't contain [EXERCISE_SEGMENT_TYPE_YOGA] segments.
         *
         * @param segmentType the segment type to be contained within [sessionType].
         * @param sessionType the session type that should contain [segmentType].
         * @return True, if [sessionType] can contain the provided segment, otherwise false.
         */
        @JvmStatic
        fun isSegmentTypeCompatibleWithSessionType(
            @ExerciseSegmentTypes segmentType: Int,
            @ExerciseTypes sessionType: Int,
        ): Boolean {
            if (UNIVERSAL_SESSION_TYPES.contains(sessionType)) {
                return true
            }
            if (UNIVERSAL_SEGMENTS.contains(segmentType)) {
                return true
            }
            return SESSION_TO_SEGMENTS_MAPPING[sessionType]?.contains(segmentType) ?: false
        }

        /** Next Id: 68. */

        /** Use this type if the type of the exercise segment is not known. */
        const val EXERCISE_SEGMENT_TYPE_UNKNOWN = 0

        /** Use this type for arm curls. */
        const val EXERCISE_SEGMENT_TYPE_ARM_CURL = 1

        /** Use this type for back extensions. */
        const val EXERCISE_SEGMENT_TYPE_BACK_EXTENSION = 2

        /** Use this type for ball slams. */
        const val EXERCISE_SEGMENT_TYPE_BALL_SLAM = 3

        /** Use this type for barbel shoulder press. */
        const val EXERCISE_SEGMENT_TYPE_BARBELL_SHOULDER_PRESS = 4

        /** Use this type for bench presses. */
        const val EXERCISE_SEGMENT_TYPE_BENCH_PRESS = 5

        /** Use this type for bench sit up. */
        const val EXERCISE_SEGMENT_TYPE_BENCH_SIT_UP = 6

        /** Use this type for biking. */
        const val EXERCISE_SEGMENT_TYPE_BIKING = 7

        /** Use this type for stationary biking. */
        const val EXERCISE_SEGMENT_TYPE_BIKING_STATIONARY = 8

        /** Use this type for burpees. */
        const val EXERCISE_SEGMENT_TYPE_BURPEE = 9

        /** Use this type for crunches. */
        const val EXERCISE_SEGMENT_TYPE_CRUNCH = 10

        /** Use this type for deadlifts. */
        const val EXERCISE_SEGMENT_TYPE_DEADLIFT = 11

        /** Use this type for double arms triceps extensions. */
        const val EXERCISE_SEGMENT_TYPE_DOUBLE_ARM_TRICEPS_EXTENSION = 12

        /** Use this type for left arm dumbbell curl. */
        const val EXERCISE_SEGMENT_TYPE_DUMBBELL_CURL_LEFT_ARM = 13

        const val EXERCISE_SEGMENT_TYPE_DUMBBELL_CURL_RIGHT_ARM = 14

        /** Use this type for right arm dumbbell curl. */
        const val EXERCISE_SEGMENT_TYPE_DUMBBELL_FRONT_RAISE = 15

        /** Use this type for dumbbell lateral raises. */
        const val EXERCISE_SEGMENT_TYPE_DUMBBELL_LATERAL_RAISE = 16

        /** Use this type for dumbbells rows. */
        const val EXERCISE_SEGMENT_TYPE_DUMBBELL_ROW = 17

        /** Use this type for left arm triceps extensions. */
        const val EXERCISE_SEGMENT_TYPE_DUMBBELL_TRICEPS_EXTENSION_LEFT_ARM = 18

        /** Use this type for right arm triceps extensions. */
        const val EXERCISE_SEGMENT_TYPE_DUMBBELL_TRICEPS_EXTENSION_RIGHT_ARM = 19

        /** Use this type for two arms triceps extensions. */
        const val EXERCISE_SEGMENT_TYPE_DUMBBELL_TRICEPS_EXTENSION_TWO_ARM = 20

        /** Use this type for elliptical workout. */
        const val EXERCISE_SEGMENT_TYPE_ELLIPTICAL = 21

        /** Use this type for forward twists. */
        const val EXERCISE_SEGMENT_TYPE_FORWARD_TWIST = 22

        /** Use this type for front raises. */
        const val EXERCISE_SEGMENT_TYPE_FRONT_RAISE = 23

        /** Use this type for high intensity training. */
        const val EXERCISE_SEGMENT_TYPE_HIGH_INTENSITY_INTERVAL_TRAINING = 24

        /** Use this type for hip thrusts. */
        const val EXERCISE_SEGMENT_TYPE_HIP_THRUST = 25

        /** Use this type for hula-hoops. */
        const val EXERCISE_SEGMENT_TYPE_HULA_HOOP = 26

        /** Use this type for jumping jacks. */
        const val EXERCISE_SEGMENT_TYPE_JUMPING_JACK = 27

        /** Use this type for jump rope. */
        const val EXERCISE_SEGMENT_TYPE_JUMP_ROPE = 28

        /** Use this type for kettlebell swings. */
        const val EXERCISE_SEGMENT_TYPE_KETTLEBELL_SWING = 29

        /** Use this type for lateral raises. */
        const val EXERCISE_SEGMENT_TYPE_LATERAL_RAISE = 30

        /** Use this type for lat pull-downs. */
        const val EXERCISE_SEGMENT_TYPE_LAT_PULL_DOWN = 31

        /** Use this type for leg curls. */
        const val EXERCISE_SEGMENT_TYPE_LEG_CURL = 32

        /** Use this type for leg extensions. */
        const val EXERCISE_SEGMENT_TYPE_LEG_EXTENSION = 33

        /** Use this type for leg presses. */
        const val EXERCISE_SEGMENT_TYPE_LEG_PRESS = 34

        /** Use this type for leg raises. */
        const val EXERCISE_SEGMENT_TYPE_LEG_RAISE = 35

        /** Use this type for lunges. */
        const val EXERCISE_SEGMENT_TYPE_LUNGE = 36

        /** Use this type for mountain climber. */
        const val EXERCISE_SEGMENT_TYPE_MOUNTAIN_CLIMBER = 37

        /** Use this type for other workout. */
        const val EXERCISE_SEGMENT_TYPE_OTHER_WORKOUT = 38

        /** Use this type for the pause. */
        const val EXERCISE_SEGMENT_TYPE_PAUSE = 39

        /** Use this type for pilates. */
        const val EXERCISE_SEGMENT_TYPE_PILATES = 40

        /** Use this type for plank. */
        const val EXERCISE_SEGMENT_TYPE_PLANK = 41

        /** Use this type for pull-ups. */
        const val EXERCISE_SEGMENT_TYPE_PULL_UP = 42

        /** Use this type for punches. */
        const val EXERCISE_SEGMENT_TYPE_PUNCH = 43

        /** Use this type for the rest. */
        const val EXERCISE_SEGMENT_TYPE_REST = 44

        /** Use this type for rowing machine workout. */
        const val EXERCISE_SEGMENT_TYPE_ROWING_MACHINE = 45

        /** Use this type for running. */
        const val EXERCISE_SEGMENT_TYPE_RUNNING = 46

        /** Use this type for treadmill running. */
        const val EXERCISE_SEGMENT_TYPE_RUNNING_TREADMILL = 47

        const val EXERCISE_SEGMENT_TYPE_SHOULDER_PRESS = 48

        /** Use this type for shoulder press. */
        const val EXERCISE_SEGMENT_TYPE_SINGLE_ARM_TRICEPS_EXTENSION = 49

        /** Use this type for sit-ups. */
        const val EXERCISE_SEGMENT_TYPE_SIT_UP = 50

        /** Use this type for squats. */
        const val EXERCISE_SEGMENT_TYPE_SQUAT = 51

        /** Use this type for stair climbing. */
        const val EXERCISE_SEGMENT_TYPE_STAIR_CLIMBING = 52

        /** Use this type for stair climbing machine. */
        const val EXERCISE_SEGMENT_TYPE_STAIR_CLIMBING_MACHINE = 53

        /** Use this type for stretching. */
        const val EXERCISE_SEGMENT_TYPE_STRETCHING = 54

        /** Use this type for backstroke swimming. */
        const val EXERCISE_SEGMENT_TYPE_SWIMMING_BACKSTROKE = 55

        /** Use this type for breaststroke swimming. */
        const val EXERCISE_SEGMENT_TYPE_SWIMMING_BREASTSTROKE = 56

        /** Use this type for butterfly swimming. */
        const val EXERCISE_SEGMENT_TYPE_SWIMMING_BUTTERFLY = 57

        const val EXERCISE_SEGMENT_TYPE_SWIMMING_FREESTYLE = 58

        /** Use this type for mixed swimming. */
        const val EXERCISE_SEGMENT_TYPE_SWIMMING_MIXED = 59

        /** Use this type for swimming in open water. */
        const val EXERCISE_SEGMENT_TYPE_SWIMMING_OPEN_WATER = 60

        /** Use this type if other swimming styles are not suitable. */
        const val EXERCISE_SEGMENT_TYPE_SWIMMING_OTHER = 61

        /** Use this type for swimming in the pool. */
        const val EXERCISE_SEGMENT_TYPE_SWIMMING_POOL = 62

        /** Use this type for upper twists. */
        const val EXERCISE_SEGMENT_TYPE_UPPER_TWIST = 63

        /** Use this type for walking. */
        const val EXERCISE_SEGMENT_TYPE_WALKING = 64

        /** Use this type for weightlifting. */
        const val EXERCISE_SEGMENT_TYPE_WEIGHTLIFTING = 65

        /** Use this type for wheelchair. */
        const val EXERCISE_SEGMENT_TYPE_WHEELCHAIR = 66

        /** Use this type for yoga. */
        const val EXERCISE_SEGMENT_TYPE_YOGA = 67

        internal val UNIVERSAL_SESSION_TYPES =
            setOf(
                ExerciseSessionRecord.EXERCISE_TYPE_BOOT_CAMP,
                ExerciseSessionRecord.EXERCISE_TYPE_HIGH_INTENSITY_INTERVAL_TRAINING,
                ExerciseSessionRecord.EXERCISE_TYPE_OTHER_WORKOUT,
            )

        internal val UNIVERSAL_SEGMENTS =
            setOf(
                EXERCISE_SEGMENT_TYPE_OTHER_WORKOUT,
                EXERCISE_SEGMENT_TYPE_PAUSE,
                EXERCISE_SEGMENT_TYPE_REST,
                EXERCISE_SEGMENT_TYPE_STRETCHING,
                EXERCISE_SEGMENT_TYPE_UNKNOWN,
            )

        internal val EXERCISE_SEGMENTS =
            setOf(
                EXERCISE_SEGMENT_TYPE_ARM_CURL,
                EXERCISE_SEGMENT_TYPE_BACK_EXTENSION,
                EXERCISE_SEGMENT_TYPE_BALL_SLAM,
                EXERCISE_SEGMENT_TYPE_BARBELL_SHOULDER_PRESS,
                EXERCISE_SEGMENT_TYPE_BENCH_PRESS,
                EXERCISE_SEGMENT_TYPE_BENCH_SIT_UP,
                EXERCISE_SEGMENT_TYPE_BURPEE,
                EXERCISE_SEGMENT_TYPE_CRUNCH,
                EXERCISE_SEGMENT_TYPE_DEADLIFT,
                EXERCISE_SEGMENT_TYPE_DOUBLE_ARM_TRICEPS_EXTENSION,
                EXERCISE_SEGMENT_TYPE_DUMBBELL_CURL_LEFT_ARM,
                EXERCISE_SEGMENT_TYPE_DUMBBELL_CURL_RIGHT_ARM,
                EXERCISE_SEGMENT_TYPE_DUMBBELL_FRONT_RAISE,
                EXERCISE_SEGMENT_TYPE_DUMBBELL_LATERAL_RAISE,
                EXERCISE_SEGMENT_TYPE_DUMBBELL_ROW,
                EXERCISE_SEGMENT_TYPE_DUMBBELL_TRICEPS_EXTENSION_LEFT_ARM,
                EXERCISE_SEGMENT_TYPE_DUMBBELL_TRICEPS_EXTENSION_RIGHT_ARM,
                EXERCISE_SEGMENT_TYPE_DUMBBELL_TRICEPS_EXTENSION_TWO_ARM,
                EXERCISE_SEGMENT_TYPE_FORWARD_TWIST,
                EXERCISE_SEGMENT_TYPE_FRONT_RAISE,
                EXERCISE_SEGMENT_TYPE_HIP_THRUST,
                EXERCISE_SEGMENT_TYPE_HULA_HOOP,
                EXERCISE_SEGMENT_TYPE_JUMP_ROPE,
                EXERCISE_SEGMENT_TYPE_JUMPING_JACK,
                EXERCISE_SEGMENT_TYPE_KETTLEBELL_SWING,
                EXERCISE_SEGMENT_TYPE_LATERAL_RAISE,
                EXERCISE_SEGMENT_TYPE_LAT_PULL_DOWN,
                EXERCISE_SEGMENT_TYPE_LEG_CURL,
                EXERCISE_SEGMENT_TYPE_LEG_EXTENSION,
                EXERCISE_SEGMENT_TYPE_LEG_PRESS,
                EXERCISE_SEGMENT_TYPE_LEG_RAISE,
                EXERCISE_SEGMENT_TYPE_LUNGE,
                EXERCISE_SEGMENT_TYPE_MOUNTAIN_CLIMBER,
                EXERCISE_SEGMENT_TYPE_PLANK,
                EXERCISE_SEGMENT_TYPE_PULL_UP,
                EXERCISE_SEGMENT_TYPE_PUNCH,
                EXERCISE_SEGMENT_TYPE_SHOULDER_PRESS,
                EXERCISE_SEGMENT_TYPE_SINGLE_ARM_TRICEPS_EXTENSION,
                EXERCISE_SEGMENT_TYPE_SIT_UP,
                EXERCISE_SEGMENT_TYPE_SQUAT,
                EXERCISE_SEGMENT_TYPE_UPPER_TWIST,
                EXERCISE_SEGMENT_TYPE_WEIGHTLIFTING,
            )
        internal val SWIMMING_SEGMENTS =
            setOf(
                EXERCISE_SEGMENT_TYPE_SWIMMING_BACKSTROKE,
                EXERCISE_SEGMENT_TYPE_SWIMMING_BREASTSTROKE,
                EXERCISE_SEGMENT_TYPE_SWIMMING_FREESTYLE,
                EXERCISE_SEGMENT_TYPE_SWIMMING_BUTTERFLY,
                EXERCISE_SEGMENT_TYPE_SWIMMING_MIXED,
                EXERCISE_SEGMENT_TYPE_SWIMMING_OTHER,
            )

        private val SESSION_TO_SEGMENTS_MAPPING =
            mapOf(
                ExerciseSessionRecord.EXERCISE_TYPE_BIKING to setOf(EXERCISE_SEGMENT_TYPE_BIKING),
                ExerciseSessionRecord.EXERCISE_TYPE_BIKING_STATIONARY to
                    setOf(EXERCISE_SEGMENT_TYPE_BIKING_STATIONARY),
                ExerciseSessionRecord.EXERCISE_TYPE_CALISTHENICS to EXERCISE_SEGMENTS,
                ExerciseSessionRecord.EXERCISE_TYPE_ELLIPTICAL to
                    setOf(EXERCISE_SEGMENT_TYPE_ELLIPTICAL),
                ExerciseSessionRecord.EXERCISE_TYPE_EXERCISE_CLASS to
                    setOf(
                        EXERCISE_SEGMENT_TYPE_YOGA,
                        EXERCISE_SEGMENT_TYPE_BIKING_STATIONARY,
                        EXERCISE_SEGMENT_TYPE_PILATES,
                        EXERCISE_SEGMENT_TYPE_HIGH_INTENSITY_INTERVAL_TRAINING,
                    ),
                ExerciseSessionRecord.EXERCISE_TYPE_GYMNASTICS to EXERCISE_SEGMENTS,
                ExerciseSessionRecord.EXERCISE_TYPE_HIKING to
                    setOf(EXERCISE_SEGMENT_TYPE_WALKING, EXERCISE_SEGMENT_TYPE_WHEELCHAIR),
                ExerciseSessionRecord.EXERCISE_TYPE_PILATES to setOf(EXERCISE_SEGMENT_TYPE_PILATES),
                ExerciseSessionRecord.EXERCISE_TYPE_ROWING_MACHINE to
                    setOf(EXERCISE_SEGMENT_TYPE_ROWING_MACHINE),
                ExerciseSessionRecord.EXERCISE_TYPE_RUNNING to
                    setOf(EXERCISE_SEGMENT_TYPE_RUNNING, EXERCISE_SEGMENT_TYPE_WALKING),
                ExerciseSessionRecord.EXERCISE_TYPE_RUNNING_TREADMILL to
                    setOf(EXERCISE_SEGMENT_TYPE_RUNNING_TREADMILL),
                ExerciseSessionRecord.EXERCISE_TYPE_STRENGTH_TRAINING to EXERCISE_SEGMENTS,
                ExerciseSessionRecord.EXERCISE_TYPE_STAIR_CLIMBING to
                    setOf(EXERCISE_SEGMENT_TYPE_STAIR_CLIMBING),
                ExerciseSessionRecord.EXERCISE_TYPE_STAIR_CLIMBING_MACHINE to
                    setOf(EXERCISE_SEGMENT_TYPE_STAIR_CLIMBING_MACHINE),
                ExerciseSessionRecord.EXERCISE_TYPE_SWIMMING_OPEN_WATER to
                    buildSet {
                        add(EXERCISE_SEGMENT_TYPE_SWIMMING_OPEN_WATER)
                        addAll(SWIMMING_SEGMENTS)
                    },
                ExerciseSessionRecord.EXERCISE_TYPE_SWIMMING_POOL to
                    buildSet {
                        add(EXERCISE_SEGMENT_TYPE_SWIMMING_POOL)
                        addAll(SWIMMING_SEGMENTS)
                    },
                ExerciseSessionRecord.EXERCISE_TYPE_WALKING to setOf(EXERCISE_SEGMENT_TYPE_WALKING),
                ExerciseSessionRecord.EXERCISE_TYPE_WHEELCHAIR to
                    setOf(EXERCISE_SEGMENT_TYPE_WHEELCHAIR),
                ExerciseSessionRecord.EXERCISE_TYPE_WEIGHTLIFTING to EXERCISE_SEGMENTS,
                ExerciseSessionRecord.EXERCISE_TYPE_YOGA to setOf(EXERCISE_SEGMENT_TYPE_YOGA),
            )

        /** List of supported segment types on Health Platform. */
        @Retention(AnnotationRetention.SOURCE)
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        @IntDef(
            value =
                [
                    EXERCISE_SEGMENT_TYPE_UNKNOWN,
                    EXERCISE_SEGMENT_TYPE_BARBELL_SHOULDER_PRESS,
                    EXERCISE_SEGMENT_TYPE_BENCH_SIT_UP,
                    EXERCISE_SEGMENT_TYPE_BIKING,
                    EXERCISE_SEGMENT_TYPE_BIKING_STATIONARY,
                    EXERCISE_SEGMENT_TYPE_DUMBBELL_CURL_LEFT_ARM,
                    EXERCISE_SEGMENT_TYPE_DUMBBELL_CURL_RIGHT_ARM,
                    EXERCISE_SEGMENT_TYPE_DUMBBELL_FRONT_RAISE,
                    EXERCISE_SEGMENT_TYPE_DUMBBELL_LATERAL_RAISE,
                    EXERCISE_SEGMENT_TYPE_DUMBBELL_TRICEPS_EXTENSION_LEFT_ARM,
                    EXERCISE_SEGMENT_TYPE_DUMBBELL_TRICEPS_EXTENSION_RIGHT_ARM,
                    EXERCISE_SEGMENT_TYPE_DUMBBELL_TRICEPS_EXTENSION_TWO_ARM,
                    EXERCISE_SEGMENT_TYPE_FORWARD_TWIST,
                    EXERCISE_SEGMENT_TYPE_ELLIPTICAL,
                    EXERCISE_SEGMENT_TYPE_HIGH_INTENSITY_INTERVAL_TRAINING,
                    EXERCISE_SEGMENT_TYPE_PILATES,
                    EXERCISE_SEGMENT_TYPE_ROWING_MACHINE,
                    EXERCISE_SEGMENT_TYPE_RUNNING,
                    EXERCISE_SEGMENT_TYPE_RUNNING_TREADMILL,
                    EXERCISE_SEGMENT_TYPE_STAIR_CLIMBING,
                    EXERCISE_SEGMENT_TYPE_STAIR_CLIMBING_MACHINE,
                    EXERCISE_SEGMENT_TYPE_STRETCHING,
                    EXERCISE_SEGMENT_TYPE_SWIMMING_OPEN_WATER,
                    EXERCISE_SEGMENT_TYPE_SWIMMING_POOL,
                    EXERCISE_SEGMENT_TYPE_UPPER_TWIST,
                    EXERCISE_SEGMENT_TYPE_WALKING,
                    EXERCISE_SEGMENT_TYPE_WEIGHTLIFTING,
                    EXERCISE_SEGMENT_TYPE_WHEELCHAIR,
                    EXERCISE_SEGMENT_TYPE_OTHER_WORKOUT,
                    EXERCISE_SEGMENT_TYPE_YOGA,
                    EXERCISE_SEGMENT_TYPE_ARM_CURL,
                    EXERCISE_SEGMENT_TYPE_BACK_EXTENSION,
                    EXERCISE_SEGMENT_TYPE_BALL_SLAM,
                    EXERCISE_SEGMENT_TYPE_BENCH_PRESS,
                    EXERCISE_SEGMENT_TYPE_BURPEE,
                    EXERCISE_SEGMENT_TYPE_CRUNCH,
                    EXERCISE_SEGMENT_TYPE_DEADLIFT,
                    EXERCISE_SEGMENT_TYPE_DOUBLE_ARM_TRICEPS_EXTENSION,
                    EXERCISE_SEGMENT_TYPE_DUMBBELL_ROW,
                    EXERCISE_SEGMENT_TYPE_FRONT_RAISE,
                    EXERCISE_SEGMENT_TYPE_HIP_THRUST,
                    EXERCISE_SEGMENT_TYPE_HULA_HOOP,
                    EXERCISE_SEGMENT_TYPE_JUMPING_JACK,
                    EXERCISE_SEGMENT_TYPE_JUMP_ROPE,
                    EXERCISE_SEGMENT_TYPE_KETTLEBELL_SWING,
                    EXERCISE_SEGMENT_TYPE_LATERAL_RAISE,
                    EXERCISE_SEGMENT_TYPE_LAT_PULL_DOWN,
                    EXERCISE_SEGMENT_TYPE_LEG_CURL,
                    EXERCISE_SEGMENT_TYPE_LEG_EXTENSION,
                    EXERCISE_SEGMENT_TYPE_LEG_PRESS,
                    EXERCISE_SEGMENT_TYPE_LEG_RAISE,
                    EXERCISE_SEGMENT_TYPE_LUNGE,
                    EXERCISE_SEGMENT_TYPE_MOUNTAIN_CLIMBER,
                    EXERCISE_SEGMENT_TYPE_PLANK,
                    EXERCISE_SEGMENT_TYPE_PULL_UP,
                    EXERCISE_SEGMENT_TYPE_PUNCH,
                    EXERCISE_SEGMENT_TYPE_SHOULDER_PRESS,
                    EXERCISE_SEGMENT_TYPE_SINGLE_ARM_TRICEPS_EXTENSION,
                    EXERCISE_SEGMENT_TYPE_SIT_UP,
                    EXERCISE_SEGMENT_TYPE_SQUAT,
                    EXERCISE_SEGMENT_TYPE_SWIMMING_FREESTYLE,
                    EXERCISE_SEGMENT_TYPE_SWIMMING_BACKSTROKE,
                    EXERCISE_SEGMENT_TYPE_SWIMMING_BREASTSTROKE,
                    EXERCISE_SEGMENT_TYPE_SWIMMING_BUTTERFLY,
                    EXERCISE_SEGMENT_TYPE_SWIMMING_MIXED,
                    EXERCISE_SEGMENT_TYPE_SWIMMING_OTHER,
                    EXERCISE_SEGMENT_TYPE_REST,
                    EXERCISE_SEGMENT_TYPE_PAUSE,
                ]
        )
        annotation class ExerciseSegmentTypes
    }
}
