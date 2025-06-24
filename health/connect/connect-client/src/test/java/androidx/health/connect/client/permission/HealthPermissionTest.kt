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
package androidx.health.connect.client.permission

import androidx.health.connect.client.RECORD_CLASSES
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.records.StepsRecord
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HealthPermissionTest {

    @Test
    fun createReadPermission() {
        val permission = HealthPermission.getReadPermission(StepsRecord::class)
        assertThat(permission).isEqualTo(HealthPermission.READ_STEPS)
    }

    @Test
    fun createReadPermission_reified() {
        val permission = HealthPermission.getReadPermission<StepsRecord>()
        assertThat(permission).isEqualTo(HealthPermission.READ_STEPS)
    }

    @Test
    fun createReadPermission_everyRecord() {
        RECORD_CLASSES.forEach {
            val permission = HealthPermission.getReadPermission(it)
            assertThat(permission).isNotNull()
        }
    }

    @Test
    fun createReadPermission_invalidRecord_throwsIAE() {
        assertThrows(IllegalArgumentException::class.java) {
            HealthPermission.getReadPermission(Record::class)
        }
    }

    @Test
    fun createReadPermission_reified_invalidRecord_throwsIAE() {
        assertThrows(IllegalArgumentException::class.java) {
            HealthPermission.getReadPermission<Record>()
        }
    }

    @Test
    fun createWritePermission() {
        val permission = HealthPermission.getWritePermission(StepsRecord::class)
        assertThat(permission).isEqualTo(HealthPermission.WRITE_STEPS)
    }

    @Test
    fun createWritePermission_reified() {
        val permission = HealthPermission.getWritePermission<StepsRecord>()
        assertThat(permission).isEqualTo(HealthPermission.WRITE_STEPS)
    }

    @Test
    fun createWritePermission_everyRecord() {
        RECORD_CLASSES.forEach {
            val permission = HealthPermission.getWritePermission(it)
            assertThat(permission).isNotNull()
        }
    }

    @Test
    fun createWritePermission_invalidRecord_throwsIAE() {
        assertThrows(IllegalArgumentException::class.java) {
            HealthPermission.getWritePermission(Record::class)
        }
    }

    @Test
    fun createWritePermission_reified_invalidRecord_throwsIAE() {
        assertThrows(IllegalArgumentException::class.java) {
            HealthPermission.getWritePermission<Record>()
        }
    }

    @Test
    fun readExerciseRoutes_validPermissionStringContainedInAllPermissions() {
        assertThat(HealthPermission.ALL_PERMISSIONS)
            .contains("android.permission.health.READ_EXERCISE_ROUTES")
    }

    @Test
    fun writeExerciseRoute_validPermissionStringContainedInAllPermissions() {
        assertThat(HealthPermission.ALL_PERMISSIONS)
            .contains("android.permission.health.WRITE_EXERCISE_ROUTE")
    }
}
