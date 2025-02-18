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

package androidx.health.connect.client.impl

import android.content.Context
import android.os.Build
import android.os.ext.SdkExtensions
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.HealthConnectFeatures
import androidx.health.connect.client.changes.DeletionChange
import androidx.health.connect.client.changes.UpsertionChange
import androidx.health.connect.client.impl.platform.aggregate.AGGREGATE_METRICS_ADDED_IN_SDK_EXT_10
import androidx.health.connect.client.impl.platform.records.SDK_TO_PLATFORM_RECORD_CLASS
import androidx.health.connect.client.impl.platform.records.SDK_TO_PLATFORM_RECORD_CLASS_EXT_13
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.readRecord
import androidx.health.connect.client.records.BloodPressureRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.NutritionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.records.WheelchairPushesRecord
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.request.AggregateGroupByDurationRequest
import androidx.health.connect.client.request.AggregateGroupByPeriodRequest
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ChangesTokenRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.health.connect.client.units.Energy
import androidx.health.connect.client.units.Mass
import androidx.health.connect.client.units.grams
import androidx.health.connect.client.units.kilocalories
import androidx.health.connect.client.units.millimetersOfMercury
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.test.rule.GrantPermissionRule
import com.google.common.truth.Truth.assertThat
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Period
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertThrows
import org.junit.Assume.assumeFalse
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@MediumTest
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.UPSIDE_DOWN_CAKE, codeName = "UpsideDownCake")
class HealthConnectClientUpsideDownImplTest {

    private companion object {
        private const val TOLERANCE = 1.0e-9

        private val START_TIME =
            LocalDate.now().minusDays(5).atStartOfDay().toInstant(ZoneOffset.UTC)
        private val ZONE_OFFSET = ZoneOffset.UTC
        private val ZONE_ID = ZoneId.of(ZONE_OFFSET.id)

        fun getAllRecordPermissions(): Array<String> {
            val permissions: HashSet<String> = HashSet()

            for (recordType in SDK_TO_PLATFORM_RECORD_CLASS.keys) {
                permissions.add(HealthPermission.getReadPermission(recordType))
                permissions.add(HealthPermission.getWritePermission(recordType))
            }

            if (SdkExtensions.getExtensionVersion(Build.VERSION_CODES.UPSIDE_DOWN_CAKE) >= 13) {
                for (recordType in SDK_TO_PLATFORM_RECORD_CLASS_EXT_13.keys) {
                    permissions.add(HealthPermission.getReadPermission(recordType))
                    permissions.add(HealthPermission.getWritePermission(recordType))
                }
            }

            return permissions.toTypedArray()
        }
    }

    private val context: Context = ApplicationProvider.getApplicationContext()

    @get:Rule
    val grantPermissionRule: GrantPermissionRule =
        GrantPermissionRule.grant(*getAllRecordPermissions())

    private lateinit var healthConnectClient: HealthConnectClient

    @Before
    fun setUp() {
        healthConnectClient = HealthConnectClientUpsideDownImpl(context)
    }

    @After
    fun tearDown() = runTest {
        for (recordType in SDK_TO_PLATFORM_RECORD_CLASS.keys) {
            healthConnectClient.deleteRecords(recordType, TimeRangeFilter.none())
        }

        if (SdkExtensions.getExtensionVersion(Build.VERSION_CODES.UPSIDE_DOWN_CAKE) >= 13) {
            for (recordType in SDK_TO_PLATFORM_RECORD_CLASS_EXT_13.keys) {
                healthConnectClient.deleteRecords(recordType, TimeRangeFilter.none())
            }
        }
    }

    @Test
    fun getFeatureStatus_featuresAddedInExt13_areAvailableInExt13() {
        assumeTrue(SdkExtensions.getExtensionVersion(Build.VERSION_CODES.UPSIDE_DOWN_CAKE) >= 13)

        for (feature in
            setOf(
                HealthConnectFeatures.FEATURE_READ_HEALTH_DATA_IN_BACKGROUND,
                HealthConnectFeatures.FEATURE_SKIN_TEMPERATURE,
                HealthConnectFeatures.FEATURE_PLANNED_EXERCISE
            )) {
            assertThat(healthConnectClient.features.getFeatureStatus(feature))
                .isEqualTo(HealthConnectFeatures.FEATURE_STATUS_AVAILABLE)
        }
    }

    @Test
    fun getFeatureStatus_belowUExt13_noneIsAvailable() {
        assumeTrue(SdkExtensions.getExtensionVersion(Build.VERSION_CODES.UPSIDE_DOWN_CAKE) < 13)

        val features =
            listOf(
                HealthConnectFeatures.FEATURE_READ_HEALTH_DATA_IN_BACKGROUND,
                HealthConnectFeatures.FEATURE_READ_HEALTH_DATA_HISTORY,
                HealthConnectFeatures.FEATURE_SKIN_TEMPERATURE,
                HealthConnectFeatures.FEATURE_PLANNED_EXERCISE
            )

        for (feature in features) {
            assertThat(healthConnectClient.features.getFeatureStatus(feature))
                .isEqualTo(HealthConnectFeatures.FEATURE_STATUS_UNAVAILABLE)
        }
    }

    @Test
    fun insertRecords() = runTest {
        val response =
            healthConnectClient.insertRecords(
                listOf(
                    StepsRecord(
                        count = 10,
                        startTime = START_TIME,
                        startZoneOffset = null,
                        endTime = START_TIME + 1.minutes,
                        endZoneOffset = null,
                        metadata = Metadata.manualEntry(),
                    )
                )
            )
        assertThat(response.recordIdsList).hasSize(1)
    }

    @Test
    fun deleteRecords_byId() = runTest {
        val recordIds =
            healthConnectClient
                .insertRecords(
                    listOf(
                        StepsRecord(
                            count = 10,
                            startTime = START_TIME,
                            startZoneOffset = null,
                            endTime = START_TIME + 1.minutes,
                            endZoneOffset = null,
                            metadata = Metadata.manualEntry(),
                        ),
                        StepsRecord(
                            count = 15,
                            startTime = START_TIME + 2.minutes,
                            startZoneOffset = null,
                            endTime = START_TIME + 3.minutes,
                            endZoneOffset = null,
                            metadata = Metadata.manualEntry(),
                        ),
                        StepsRecord(
                            count = 20,
                            startTime = START_TIME + 4.minutes,
                            startZoneOffset = null,
                            endTime = START_TIME + 5.minutes,
                            endZoneOffset = null,
                            metadata = Metadata.manualEntry(clientRecordId = "clientId")
                        ),
                    )
                )
                .recordIdsList

        val initialRecords =
            healthConnectClient
                .readRecords(ReadRecordsRequest(StepsRecord::class, TimeRangeFilter.none()))
                .records

        healthConnectClient.deleteRecords(
            StepsRecord::class,
            listOf(recordIds[1]),
            listOf("clientId")
        )

        assertThat(
                healthConnectClient
                    .readRecords(ReadRecordsRequest(StepsRecord::class, TimeRangeFilter.none()))
                    .records
            )
            .containsExactly(initialRecords[0])
    }

    @Test
    fun deleteRecords_byTimeRange() = runTest {
        healthConnectClient
            .insertRecords(
                listOf(
                    StepsRecord(
                        count = 100,
                        startTime = START_TIME,
                        startZoneOffset = ZoneOffset.UTC,
                        endTime = START_TIME + 1.minutes,
                        endZoneOffset = ZoneOffset.UTC,
                        metadata = Metadata.manualEntry(),
                    ),
                    StepsRecord(
                        count = 150,
                        startTime = START_TIME + 2.minutes,
                        startZoneOffset = ZoneOffset.UTC,
                        endTime = START_TIME + 3.minutes,
                        endZoneOffset = ZoneOffset.UTC,
                        metadata = Metadata.manualEntry(),
                    ),
                )
            )
            .recordIdsList

        val initialRecords =
            healthConnectClient
                .readRecords(ReadRecordsRequest(StepsRecord::class, TimeRangeFilter.none()))
                .records

        healthConnectClient.deleteRecords(
            StepsRecord::class,
            TimeRangeFilter.before(START_TIME + 1.minutes + 30.seconds)
        )

        assertThat(
                healthConnectClient
                    .readRecords(ReadRecordsRequest(StepsRecord::class, TimeRangeFilter.none()))
                    .records
            )
            .containsExactly(initialRecords[1])
    }

    @Test
    fun updateRecords() = runTest {
        val id =
            healthConnectClient
                .insertRecords(
                    listOf(
                        StepsRecord(
                            count = 10,
                            startTime = START_TIME,
                            startZoneOffset = null,
                            endTime = START_TIME + 30.seconds,
                            endZoneOffset = null,
                            metadata = Metadata.manualEntry(),
                        )
                    )
                )
                .recordIdsList[0]

        healthConnectClient.updateRecords(
            listOf(
                StepsRecord(
                    count = 5,
                    startTime = START_TIME,
                    startZoneOffset = null,
                    endTime = START_TIME + 30.seconds,
                    endZoneOffset = null,
                    metadata = Metadata.manualEntryWithId(id = id)
                )
            )
        )

        val updatedRecord = healthConnectClient.readRecord(StepsRecord::class, id).record

        assertThat(updatedRecord.count).isEqualTo(5L)
    }

    @Test
    fun readRecord_withId() = runTest {
        val insertResponse =
            healthConnectClient.insertRecords(
                listOf(
                    StepsRecord(
                        count = 10,
                        startTime = START_TIME,
                        startZoneOffset = ZoneOffset.UTC,
                        endTime = START_TIME + 1.minutes,
                        endZoneOffset = ZoneOffset.UTC,
                        metadata = Metadata.manualEntry(),
                    )
                )
            )

        val readResponse =
            healthConnectClient.readRecord(StepsRecord::class, insertResponse.recordIdsList[0])

        with(readResponse.record) {
            assertThat(count).isEqualTo(10)
            assertThat(startTime).isEqualTo(START_TIME.truncatedTo(ChronoUnit.MILLIS))
            assertThat(startZoneOffset).isEqualTo(ZoneOffset.UTC)
            assertThat(endTime).isEqualTo((START_TIME + 1.minutes).truncatedTo(ChronoUnit.MILLIS))
            assertThat(endZoneOffset).isEqualTo(ZoneOffset.UTC)
        }
    }

    @Test
    fun readRecords_withFilters() = runTest {
        healthConnectClient.insertRecords(
            listOf(
                StepsRecord(
                    count = 10,
                    startTime = START_TIME,
                    startZoneOffset = ZoneOffset.UTC,
                    endTime = START_TIME + 1.minutes,
                    endZoneOffset = ZoneOffset.UTC,
                    metadata = Metadata.manualEntry(),
                ),
                StepsRecord(
                    count = 5,
                    startTime = START_TIME + 2.minutes,
                    startZoneOffset = ZoneOffset.UTC,
                    endTime = START_TIME + 3.minutes,
                    endZoneOffset = ZoneOffset.UTC,
                    metadata = Metadata.manualEntry(),
                ),
            )
        )

        val readResponse =
            healthConnectClient.readRecords(
                ReadRecordsRequest(
                    StepsRecord::class,
                    TimeRangeFilter.after(START_TIME + 1.minutes + 30.seconds)
                )
            )

        assertThat(readResponse.records[0].count).isEqualTo(5)
    }

    @Test
    fun aggregateRecords() = runTest {
        healthConnectClient.insertRecords(
            listOf(
                HeartRateRecord(
                    startTime = START_TIME,
                    startZoneOffset = ZoneOffset.UTC,
                    endTime = START_TIME + 30.seconds,
                    endZoneOffset = ZoneOffset.UTC,
                    metadata = Metadata.manualEntry(),
                    samples =
                        listOf(
                            HeartRateRecord.Sample(START_TIME, 57L),
                            HeartRateRecord.Sample(START_TIME + 15.seconds, 120L)
                        )
                ),
                HeartRateRecord(
                    startTime = START_TIME + 1.minutes,
                    startZoneOffset = ZoneOffset.UTC,
                    endTime = START_TIME + 1.minutes + 30.seconds,
                    endZoneOffset = ZoneOffset.UTC,
                    metadata = Metadata.manualEntry(),
                    samples =
                        listOf(
                            HeartRateRecord.Sample(START_TIME + 1.minutes, 47L),
                            HeartRateRecord.Sample(START_TIME + 1.minutes + 15.seconds, 48L)
                        )
                ),
                NutritionRecord(
                    startTime = START_TIME,
                    startZoneOffset = ZoneOffset.UTC,
                    endTime = START_TIME + 1.minutes,
                    endZoneOffset = ZoneOffset.UTC,
                    metadata = Metadata.manualEntry(),
                    energy = Energy.kilocalories(200.0)
                ),
                WeightRecord(
                    time = START_TIME,
                    zoneOffset = ZoneOffset.UTC,
                    metadata = Metadata.manualEntry(),
                    weight = Mass.kilograms(100.0)
                ),
            )
        )

        val aggregateResponse =
            healthConnectClient.aggregate(
                AggregateRequest(
                    setOf(
                        HeartRateRecord.BPM_MIN,
                        HeartRateRecord.BPM_MAX,
                        NutritionRecord.ENERGY_TOTAL,
                        NutritionRecord.CAFFEINE_TOTAL,
                        WeightRecord.WEIGHT_MAX,
                        WheelchairPushesRecord.COUNT_TOTAL,
                    ),
                    TimeRangeFilter.none()
                )
            )

        with(aggregateResponse) {
            assertThat(this[HeartRateRecord.BPM_MIN]).isEqualTo(47L)
            assertThat(this[HeartRateRecord.BPM_MAX]).isEqualTo(120L)
            assertThat(this[NutritionRecord.ENERGY_TOTAL]).isEqualTo(Energy.kilocalories(200.0))
            assertThat(this[NutritionRecord.CAFFEINE_TOTAL]!!.inGrams).isWithin(TOLERANCE).of(0.0)
            assertThat(this[WeightRecord.WEIGHT_MAX]).isEqualTo(Mass.kilograms(100.0))

            assertThat(contains(WheelchairPushesRecord.COUNT_TOTAL)).isFalse()
        }
    }

    // TODO(b/361297592): Remove once the aggregation bug is fixed
    @Test
    fun aggregateRecords_unsupportedMetrics_throwsUOE() = runTest {
        for (metric in AGGREGATE_METRICS_ADDED_IN_SDK_EXT_10) {
            assertThrows(UnsupportedOperationException::class.java) {
                runBlocking {
                    healthConnectClient.aggregate(
                        AggregateRequest(setOf(metric), TimeRangeFilter.none())
                    )
                }
            }

            assertThrows(UnsupportedOperationException::class.java) {
                runBlocking {
                    healthConnectClient.aggregateGroupByDuration(
                        AggregateGroupByDurationRequest(
                            setOf(metric),
                            TimeRangeFilter.none(),
                            Duration.ofDays(1)
                        )
                    )
                }
            }

            assertThrows(UnsupportedOperationException::class.java) {
                runBlocking {
                    healthConnectClient.aggregateGroupByPeriod(
                        AggregateGroupByPeriodRequest(
                            setOf(metric),
                            TimeRangeFilter.none(),
                            Period.ofDays(1)
                        )
                    )
                }
            }
        }
    }

    @Ignore("b/326414908")
    @Test
    fun aggregateRecords_belowSdkExt10() = runTest {
        assumeFalse(SdkExtensions.getExtensionVersion(Build.VERSION_CODES.UPSIDE_DOWN_CAKE) >= 10)

        healthConnectClient.insertRecords(
            listOf(
                NutritionRecord(
                    startTime = START_TIME,
                    startZoneOffset = ZoneOffset.UTC,
                    endTime = START_TIME + 1.minutes,
                    endZoneOffset = ZoneOffset.UTC,
                    metadata = Metadata.manualEntry(),
                    transFat = 0.5.grams
                ),
                BloodPressureRecord(
                    time = START_TIME,
                    zoneOffset = ZoneOffset.UTC,
                    metadata = Metadata.manualEntry(),
                    systolic = 120.millimetersOfMercury,
                    diastolic = 80.millimetersOfMercury
                )
            )
        )

        val aggregateResponse =
            healthConnectClient.aggregate(
                AggregateRequest(
                    setOf(
                        BloodPressureRecord.DIASTOLIC_AVG,
                        BloodPressureRecord.DIASTOLIC_MAX,
                        BloodPressureRecord.DIASTOLIC_MIN,
                        BloodPressureRecord.SYSTOLIC_AVG,
                        BloodPressureRecord.SYSTOLIC_MAX,
                        BloodPressureRecord.SYSTOLIC_MIN,
                        NutritionRecord.TRANS_FAT_TOTAL
                    ),
                    TimeRangeFilter.none()
                )
            )

        assertEquals(
            aggregateResponse[NutritionRecord.TRANS_FAT_TOTAL] to 0.5.grams,
            aggregateResponse[BloodPressureRecord.SYSTOLIC_AVG] to 120.millimetersOfMercury,
            aggregateResponse[BloodPressureRecord.SYSTOLIC_MAX] to 120.millimetersOfMercury,
            aggregateResponse[BloodPressureRecord.SYSTOLIC_MIN] to 120.millimetersOfMercury,
            aggregateResponse[BloodPressureRecord.DIASTOLIC_AVG] to 80.millimetersOfMercury,
            aggregateResponse[BloodPressureRecord.DIASTOLIC_MAX] to 80.millimetersOfMercury,
            aggregateResponse[BloodPressureRecord.DIASTOLIC_MIN] to 80.millimetersOfMercury,
        )
    }

    @Test
    fun aggregateRecordsGroupByDuration() = runTest {
        healthConnectClient.insertRecords(
            listOf(
                NutritionRecord(
                    energy = 100.kilocalories,
                    startTime = START_TIME,
                    startZoneOffset = ZoneOffset.UTC,
                    endTime = START_TIME + 10.seconds,
                    endZoneOffset = ZoneOffset.UTC,
                    metadata = Metadata.manualEntry(),
                ),
                NutritionRecord(
                    energy = 200.kilocalories,
                    startTime = START_TIME + 15.seconds,
                    startZoneOffset = ZoneOffset.UTC,
                    endTime = START_TIME + 25.seconds,
                    endZoneOffset = ZoneOffset.UTC,
                    metadata = Metadata.manualEntry(),
                ),
                NutritionRecord(
                    energy = 500.kilocalories,
                    startTime = START_TIME + 40.seconds,
                    startZoneOffset = ZoneOffset.UTC,
                    endTime = START_TIME + 1.minutes,
                    endZoneOffset = ZoneOffset.UTC,
                    metadata = Metadata.manualEntry(),
                )
            )
        )

        val aggregateResponse =
            healthConnectClient.aggregateGroupByDuration(
                AggregateGroupByDurationRequest(
                    setOf(NutritionRecord.ENERGY_TOTAL),
                    TimeRangeFilter.between(START_TIME, START_TIME + 1.minutes),
                    Duration.ofSeconds(30),
                    setOf()
                )
            )

        with(aggregateResponse) {
            assertThat(this).hasSize(2)
            assertThat(this[0].result[NutritionRecord.ENERGY_TOTAL]).isEqualTo(300.kilocalories)
            assertThat(this[1].result[NutritionRecord.ENERGY_TOTAL]).isEqualTo(500.kilocalories)
        }
    }

    @Test
    fun aggregateRecordsGroupByPeriod() = runTest {
        healthConnectClient.insertRecords(
            listOf(
                NutritionRecord(
                    energy = 100.kilocalories,
                    startTime = START_TIME,
                    startZoneOffset = ZONE_OFFSET,
                    endTime = START_TIME + 5.minutes,
                    endZoneOffset = ZONE_OFFSET,
                    metadata = Metadata.manualEntry(),
                ),
                NutritionRecord(
                    energy = 200.kilocalories,
                    startTime = START_TIME + 10.minutes,
                    startZoneOffset = ZONE_OFFSET,
                    endTime = START_TIME + 30.minutes,
                    endZoneOffset = ZONE_OFFSET,
                    metadata = Metadata.manualEntry(),
                ),
                NutritionRecord(
                    energy = 50.kilocalories,
                    startTime = START_TIME + 1.days,
                    startZoneOffset = ZONE_OFFSET,
                    endTime = START_TIME + 1.days + 10.minutes,
                    endZoneOffset = ZONE_OFFSET,
                    metadata = Metadata.manualEntry(),
                )
            )
        )

        val aggregateResponse =
            healthConnectClient.aggregateGroupByPeriod(
                AggregateGroupByPeriodRequest(
                    setOf(NutritionRecord.ENERGY_TOTAL),
                    TimeRangeFilter.between(
                        LocalDateTime.ofInstant(START_TIME, ZONE_ID),
                        LocalDateTime.ofInstant(START_TIME + 2.days, ZONE_ID),
                    ),
                    timeRangeSlicer = Period.ofDays(1)
                )
            )

        with(aggregateResponse) {
            assertThat(this).hasSize(2)
            assertThat(this[0].result[NutritionRecord.ENERGY_TOTAL]).isEqualTo(300.kilocalories)
            assertThat(this[1].result[NutritionRecord.ENERGY_TOTAL]).isEqualTo(50.kilocalories)
        }
    }

    @Test
    fun aggregateRecordsGroupByPeriod_monthly() = runTest {
        healthConnectClient.insertRecords(
            listOf(
                NutritionRecord(
                    energy = 100.kilocalories,
                    startTime = START_TIME - 40.days,
                    startZoneOffset = ZONE_OFFSET,
                    endTime = START_TIME - 40.days + 5.minutes,
                    endZoneOffset = ZONE_OFFSET,
                    metadata = Metadata.manualEntry(),
                ),
                NutritionRecord(
                    energy = 200.kilocalories,
                    startTime = START_TIME - 40.days + 10.minutes,
                    startZoneOffset = ZONE_OFFSET,
                    endTime = START_TIME - 40.days + 30.minutes,
                    endZoneOffset = ZONE_OFFSET,
                    metadata = Metadata.manualEntry(),
                ),
                NutritionRecord(
                    energy = 50.kilocalories,
                    startTime = START_TIME,
                    startZoneOffset = ZONE_OFFSET,
                    endTime = START_TIME + 10.minutes,
                    endZoneOffset = ZONE_OFFSET,
                    metadata = Metadata.manualEntry(),
                )
            )
        )

        val queryStartTime = LocalDateTime.ofInstant(START_TIME - 40.days, ZONE_ID)
        val queryEndTime = LocalDateTime.ofInstant(START_TIME + 2.days, ZONE_ID)

        val aggregateResponse =
            healthConnectClient.aggregateGroupByPeriod(
                AggregateGroupByPeriodRequest(
                    setOf(NutritionRecord.ENERGY_TOTAL),
                    TimeRangeFilter.between(
                        queryStartTime,
                        queryEndTime,
                    ),
                    timeRangeSlicer = Period.ofMonths(1)
                )
            )

        with(aggregateResponse) {
            assertThat(this).hasSize(2)

            assertThat(this[0].startTime).isEqualTo(queryStartTime)
            assertThat(this[0].endTime).isEqualTo(queryStartTime.plus(Period.ofMonths(1)))
            assertThat(this[0].result[NutritionRecord.ENERGY_TOTAL]).isEqualTo(300.kilocalories)

            assertThat(this[1].startTime).isEqualTo(queryStartTime.plus(Period.ofMonths(1)))
            assertThat(this[1].endTime).isEqualTo(queryEndTime)
            assertThat(this[1].result[NutritionRecord.ENERGY_TOTAL]).isEqualTo(50.kilocalories)
        }
    }

    @Test
    fun aggregateRecordsGroupByPeriod_monthly_noData() = runTest {
        val queryStartTime = LocalDateTime.ofInstant(START_TIME - 40.days, ZONE_ID)
        val queryEndTime = LocalDateTime.ofInstant(START_TIME + 2.days, ZONE_ID)

        val aggregateResponse =
            healthConnectClient.aggregateGroupByPeriod(
                AggregateGroupByPeriodRequest(
                    setOf(NutritionRecord.ENERGY_TOTAL),
                    TimeRangeFilter.between(
                        queryStartTime,
                        queryEndTime,
                    ),
                    timeRangeSlicer = Period.ofMonths(1)
                )
            )

        with(aggregateResponse) {
            assertThat(this).hasSize(2)

            assertThat(this[0].startTime).isEqualTo(queryStartTime)
            assertThat(this[0].endTime).isEqualTo(queryStartTime.plus(Period.ofMonths(1)))
            assertThat(this[0].result[NutritionRecord.ENERGY_TOTAL]).isNull()

            assertThat(this[1].startTime).isEqualTo(queryStartTime.plus(Period.ofMonths(1)))
            assertThat(this[1].endTime).isEqualTo(queryEndTime)
            assertThat(this[1].result[NutritionRecord.ENERGY_TOTAL]).isNull()
        }
    }

    @Test
    fun getChangesToken() = runTest {
        val token =
            healthConnectClient.getChangesToken(
                ChangesTokenRequest(setOf(StepsRecord::class), setOf())
            )
        assertThat(token).isNotEmpty()
    }

    @Test
    fun getChanges() = runTest {
        var token =
            healthConnectClient.getChangesToken(
                ChangesTokenRequest(setOf(StepsRecord::class), setOf())
            )

        val insertedRecordId =
            healthConnectClient
                .insertRecords(
                    listOf(
                        StepsRecord(
                            count = 10,
                            startTime = START_TIME,
                            startZoneOffset = ZoneOffset.UTC,
                            endTime = START_TIME + 5.minutes,
                            endZoneOffset = ZoneOffset.UTC,
                            metadata = Metadata.manualEntry(),
                        )
                    )
                )
                .recordIdsList[0]

        val record = healthConnectClient.readRecord(StepsRecord::class, insertedRecordId).record

        assertThat(healthConnectClient.getChanges(token).changes)
            .containsExactly(UpsertionChange(record))

        token =
            healthConnectClient.getChangesToken(
                ChangesTokenRequest(setOf(StepsRecord::class), setOf())
            )

        healthConnectClient.deleteRecords(StepsRecord::class, listOf(insertedRecordId), emptyList())

        assertThat(healthConnectClient.getChanges(token).changes)
            .containsExactly(DeletionChange(insertedRecordId))
    }

    @Test
    fun nutritionRecord_roundTrip_valuesEqual() = runTest {
        val recordId =
            healthConnectClient
                .insertRecords(
                    listOf(
                        NutritionRecord(
                            startTime = START_TIME,
                            startZoneOffset = ZONE_OFFSET,
                            endTime = START_TIME + 10.minutes,
                            endZoneOffset = ZONE_OFFSET,
                            metadata = Metadata.manualEntry(),
                            calcium = Mass.grams(15.0),
                            monounsaturatedFat = Mass.grams(50.0),
                            energy = Energy.calories(300.0)
                        )
                    )
                )
                .recordIdsList[0]

        val nutritionRecord = healthConnectClient.readRecord<NutritionRecord>(recordId).record

        with(nutritionRecord) {
            assertThat(calcium).isEqualTo(Mass.grams(15.0))
            assertThat(monounsaturatedFat).isEqualTo(Mass.grams(50.0))
            assertThat(energy).isEqualTo(Energy.calories(300.0))
        }
    }

    @Test
    fun nutritionRecord_roundTrip_zeroValues() = runTest {
        val recordId =
            healthConnectClient
                .insertRecords(
                    listOf(
                        NutritionRecord(
                            startTime = START_TIME,
                            startZoneOffset = ZONE_OFFSET,
                            endTime = START_TIME + 10.minutes,
                            endZoneOffset = ZONE_OFFSET,
                            metadata = Metadata.manualEntry(),
                            calcium = Mass.grams(0.0),
                            monounsaturatedFat = Mass.grams(0.0),
                            energy = Energy.calories(0.0)
                        )
                    )
                )
                .recordIdsList[0]

        val nutritionRecord = healthConnectClient.readRecord<NutritionRecord>(recordId).record

        with(nutritionRecord) {
            assertThat(calcium).isEqualTo(Mass.grams(0.0))
            assertThat(monounsaturatedFat).isEqualTo(Mass.grams(0.0))
            assertThat(energy).isEqualTo(Energy.calories(0.0))
        }
    }

    @Test
    fun nutritionRecord_roundTrip_nullValues() = runTest {
        val recordId =
            healthConnectClient
                .insertRecords(
                    listOf(
                        NutritionRecord(
                            startTime = START_TIME,
                            startZoneOffset = ZONE_OFFSET,
                            endTime = START_TIME + 10.minutes,
                            endZoneOffset = ZONE_OFFSET,
                            metadata = Metadata.manualEntry(),
                        )
                    )
                )
                .recordIdsList[0]

        val nutritionRecord = healthConnectClient.readRecord<NutritionRecord>(recordId).record

        with(nutritionRecord) {
            assertThat(calcium).isNull()
            assertThat(monounsaturatedFat).isNull()
            assertThat(energy).isNull()
        }
    }

    @Test
    fun getGrantedPermissions() = runTest {
        assertThat(healthConnectClient.permissionController.getGrantedPermissions())
            .containsExactlyElementsIn(getAllRecordPermissions())
    }

    private fun <A, E> assertEquals(vararg assertions: Pair<A, E>) {
        assertions.forEach { (actual, expected) -> assertThat(actual).isEqualTo(expected) }
    }

    private val Int.seconds: Duration
        get() = Duration.ofSeconds(this.toLong())

    private val Int.minutes: Duration
        get() = Duration.ofMinutes(this.toLong())

    private val Int.days: Duration
        get() = Duration.ofDays(this.toLong())
}
