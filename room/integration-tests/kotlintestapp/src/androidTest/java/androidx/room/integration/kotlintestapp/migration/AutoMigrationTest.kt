/*
 * Copyright (C) 2017 The Android Open Source Project
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
package androidx.room.integration.kotlintestapp.migration

import androidx.kruth.assertThat
import androidx.room.testing.MigrationTestHelper
import androidx.room.util.TableInfo.Companion.read
import androidx.sqlite.SQLiteException
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Test custom database migrations. */
@RunWith(AndroidJUnit4::class)
@LargeTest
class AutoMigrationTest {
    @JvmField
    @Rule
    var helper: MigrationTestHelper =
        MigrationTestHelper(
            InstrumentationRegistry.getInstrumentation(),
            AutoMigrationDb::class.java,
        )

    // Run this to create the very 1st version of the db.
    fun createFirstVersion() {
        val db = helper.createDatabase(TEST_DB, 1)
        db.execSQL("INSERT INTO Entity9 (id, name) VALUES (1, 'row1')")
        db.execSQL("INSERT INTO Entity9 (id, name) VALUES (2, 'row2')")
        db.execSQL("INSERT INTO Entity27 (id27) VALUES (3)")
        db.execSQL("INSERT INTO Entity27 (id27) VALUES (5)")
        db.close()
    }

    @Test
    @Suppress("DEPRECATION") // Due to TableInfo.read()
    fun goFromV1ToV2() {
        createFirstVersion()
        val db = helper.runMigrationsAndValidate(TEST_DB, 2, true)
        val info = read(db, AutoMigrationDb.Entity1.TABLE_NAME)
        assertThat(info.columns.size).isEqualTo(3)
        db.close()
    }

    @Test
    @Suppress("DEPRECATION") // Due to TableInfo.read()
    fun goFromV1ToV3() {
        createFirstVersion()
        val db = helper.runMigrationsAndValidate(TEST_DB, 3, true)
        val info = read(db, AutoMigrationDb.Entity1.TABLE_NAME)
        assertThat(info.columns.size).isEqualTo(3)
        db.close()
    }

    @Test
    @Suppress("DEPRECATION") // Due to TableInfo.read()
    fun goFromV1ToV4() {
        createFirstVersion()
        val db = helper.runMigrationsAndValidate(TEST_DB, 4, true)
        val info = read(db, AutoMigrationDb.Entity1.TABLE_NAME)
        assertThat(info.columns.size).isEqualTo(3)
        db.close()
    }

    @Test
    fun goFromV1ToV5() {
        createFirstVersion()
        try {
            helper.runMigrationsAndValidate(TEST_DB, 5, true)
        } catch (e: SQLiteException) {
            assertThat(e.message).contains("""Foreign key violation(s) detected in 'Entity9'""")
        }
    }

    @Test
    @Suppress("DEPRECATION") // Due to TableInfo.read()
    fun testAutoMigrationWithNewEmbeddedField() {
        val embeddedHelper =
            MigrationTestHelper(
                InstrumentationRegistry.getInstrumentation(),
                EmbeddedAutoMigrationDb::class.java,
            )
        val db = embeddedHelper.createDatabase("embedded-auto-migration-test", 1)
        db.execSQL("INSERT INTO Entity1 (id, name) VALUES (1, 'row1')")
        db.close()

        val migratedDb =
            embeddedHelper.runMigrationsAndValidate("embedded-auto-migration-test", 2, true)
        val info = read(migratedDb, EmbeddedAutoMigrationDb.EmbeddedEntity1.TABLE_NAME)
        assertThat(info.columns.size).isEqualTo(3)
        migratedDb.close()
    }

    companion object {
        private const val TEST_DB = "auto-migration-test"
    }
}
