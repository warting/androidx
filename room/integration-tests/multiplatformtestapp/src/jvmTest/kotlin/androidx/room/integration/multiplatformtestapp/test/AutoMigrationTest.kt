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

package androidx.room.integration.multiplatformtestapp.test

import androidx.room.Room
import androidx.sqlite.SQLiteDriver
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlin.io.path.createTempFile

class AutoMigrationTest : BaseAutoMigrationTest() {
    private val tempFile = createTempFile(
        "test.db"
    ).also { it.toFile().deleteOnExit() }
    override val driver: SQLiteDriver = BundledSQLiteDriver(tempFile.toString())

    override fun getRoomDatabase(): AutoMigrationDatabase {
        return Room.databaseBuilder<AutoMigrationDatabase>(tempFile.toString())
            .setDriver(driver).build()
    }
}
