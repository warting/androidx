/*
 * Copyright 2022 The Android Open Source Project
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

import androidx.health.services.client.data.ComparisonType.Companion.GREATER_THAN_OR_EQUAL
import androidx.health.services.client.data.DataType.Companion.CALORIES_TOTAL
import androidx.health.services.client.data.DataType.Companion.HEART_RATE_BPM
import androidx.health.services.client.data.ExerciseGoal.Companion.createOneTimeGoal
import androidx.health.services.client.data.ExerciseType.Companion.GOLF
import androidx.health.services.client.data.ExerciseType.Companion.WALKING
import androidx.health.services.client.data.ExerciseUpdate.ActiveDurationCheckpoint
import com.google.common.truth.Truth.assertThat
import java.time.Duration
import java.time.Instant
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class ExerciseUpdateTest {

    fun Int.instant() = Instant.ofEpochMilli(toLong())

    fun Int.duration() = Duration.ofSeconds(toLong())

    @Test
    public fun protoRoundTrip() {
        val goal =
            createOneTimeGoal(DataTypeCondition(CALORIES_TOTAL, 125.0, GREATER_THAN_OR_EQUAL))
        val debouncedGoal =
            DebouncedGoal.createSampleDebouncedGoal(
                DebouncedDataTypeCondition.createDebouncedDataTypeCondition(
                    HEART_RATE_BPM,
                    120.0,
                    GREATER_THAN_OR_EQUAL,
                    /* initialDelay= */ 60,
                    /* durationAtThreshold= */ 5,
                )
            )
        val proto =
            ExerciseUpdate(
                    latestMetrics =
                        DataPointContainer(
                            listOf(DataPoints.calories(130.0, 15.duration(), 35.duration()))
                        ),
                    latestAchievedGoals = setOf(goal),
                    latestMilestoneMarkerSummaries =
                        setOf(
                            MilestoneMarkerSummary(
                                15.instant(),
                                40.instant(),
                                20.duration(),
                                goal,
                                DataPointContainer(
                                    listOf(DataPoints.calories(130.0, 15.duration(), 35.duration()))
                                ),
                            )
                        ),
                    exerciseStateInfo =
                        ExerciseStateInfo(ExerciseState.ACTIVE, ExerciseEndReason.UNKNOWN),
                    exerciseConfig =
                        ExerciseConfig(
                            GOLF,
                            setOf(CALORIES_TOTAL),
                            isAutoPauseAndResumeEnabled = false,
                            isGpsEnabled = false,
                            exerciseGoals = listOf(goal),
                            exerciseTypeConfig =
                                GolfExerciseTypeConfig(
                                    GolfExerciseTypeConfig.GolfShotTrackingPlaceInfo
                                        .GOLF_SHOT_TRACKING_PLACE_INFO_FAIRWAY
                                ),
                        ),
                    activeDurationCheckpoint =
                        ActiveDurationCheckpoint(42.instant(), 30.duration()),
                    updateDurationFromBoot = 42.duration(),
                    startTime = 10.instant(),
                    activeDurationLegacy = 60.duration(),
                    latestAchievedDebouncedGoals = setOf(debouncedGoal),
                )
                .proto

        val update = ExerciseUpdate(proto)

        val caloriesDataPoint = update.latestMetrics.getData(DataType.CALORIES).first()
        val markerSummary = update.latestMilestoneMarkerSummaries.first()
        assertThat(update.startTime).isEqualTo(10.instant())
        assertThat(update.activeDurationLegacy).isEqualTo(60.duration())
        assertThat(update.getUpdateDurationFromBoot()).isEqualTo(42.duration())
        assertThat(caloriesDataPoint.value).isEqualTo(130.0)
        assertThat(caloriesDataPoint.startDurationFromBoot).isEqualTo(15.duration())
        assertThat(caloriesDataPoint.endDurationFromBoot).isEqualTo(35.duration())
        assertThat(update.latestAchievedGoals.first().dataTypeCondition.dataType)
            .isEqualTo(CALORIES_TOTAL)
        assertThat(update.latestAchievedDebouncedGoals.first().debouncedDataTypeCondition.dataType)
            .isEqualTo(HEART_RATE_BPM)
        assertThat(markerSummary.achievedGoal.dataTypeCondition.dataType).isEqualTo(CALORIES_TOTAL)
        assertThat(update.exerciseConfig!!.exerciseType).isEqualTo(GOLF)
        assertThat(update.exerciseConfig!!.exerciseTypeConfig!!)
            .isInstanceOf(GolfExerciseTypeConfig::class.java)
        assertThat(
                (update.exerciseConfig!!.exerciseTypeConfig!! as GolfExerciseTypeConfig)
                    .golfShotTrackingPlaceInfo
            )
            .isEqualTo(
                GolfExerciseTypeConfig.GolfShotTrackingPlaceInfo
                    .GOLF_SHOT_TRACKING_PLACE_INFO_FAIRWAY
            )
        assertThat(update.activeDurationCheckpoint!!.activeDuration).isEqualTo(30.duration())
        assertThat(update.exerciseStateInfo.state).isEqualTo(ExerciseState.ACTIVE)
    }

    @Test
    fun exerciseTypeConfigNull_protoRoundTrip() {
        val goal =
            createOneTimeGoal(DataTypeCondition(CALORIES_TOTAL, 125.0, GREATER_THAN_OR_EQUAL))
        val proto =
            ExerciseUpdate(
                    latestMetrics =
                        DataPointContainer(
                            listOf(DataPoints.calories(130.0, 15.duration(), 35.duration()))
                        ),
                    latestAchievedGoals = setOf(goal),
                    latestMilestoneMarkerSummaries =
                        setOf(
                            MilestoneMarkerSummary(
                                15.instant(),
                                40.instant(),
                                20.duration(),
                                goal,
                                DataPointContainer(
                                    listOf(DataPoints.calories(130.0, 15.duration(), 35.duration()))
                                ),
                            )
                        ),
                    exerciseStateInfo =
                        ExerciseStateInfo(ExerciseState.ACTIVE, ExerciseEndReason.UNKNOWN),
                    exerciseConfig =
                        ExerciseConfig(
                            WALKING,
                            setOf(CALORIES_TOTAL),
                            isAutoPauseAndResumeEnabled = false,
                            isGpsEnabled = false,
                            exerciseGoals = listOf(goal),
                        ),
                    activeDurationCheckpoint =
                        ActiveDurationCheckpoint(42.instant(), 30.duration()),
                    updateDurationFromBoot = 42.duration(),
                    startTime = 10.instant(),
                    activeDurationLegacy = 60.duration(),
                )
                .proto

        val update = ExerciseUpdate(proto)

        val caloriesDataPoint = update.latestMetrics.getData(DataType.CALORIES).first()
        val markerSummary = update.latestMilestoneMarkerSummaries.first()
        assertThat(update.startTime).isEqualTo(10.instant())
        assertThat(update.activeDurationLegacy).isEqualTo(60.duration())
        assertThat(update.getUpdateDurationFromBoot()).isEqualTo(42.duration())
        assertThat(caloriesDataPoint.value).isEqualTo(130.0)
        assertThat(caloriesDataPoint.startDurationFromBoot).isEqualTo(15.duration())
        assertThat(caloriesDataPoint.endDurationFromBoot).isEqualTo(35.duration())
        assertThat(update.latestAchievedGoals.first().dataTypeCondition.dataType)
            .isEqualTo(CALORIES_TOTAL)
        assertThat(markerSummary.achievedGoal.dataTypeCondition.dataType).isEqualTo(CALORIES_TOTAL)
        assertThat(update.exerciseConfig!!.exerciseType).isEqualTo(WALKING)
        assertThat(update.exerciseConfig!!.exerciseTypeConfig).isNull()
        assertThat(update.activeDurationCheckpoint!!.activeDuration).isEqualTo(30.duration())
        assertThat(update.exerciseStateInfo.state).isEqualTo(ExerciseState.ACTIVE)
    }
}
