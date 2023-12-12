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

package androidx.sqliteMultiplatform

import androidx.kruth.assertThat
import kotlin.test.Test
import kotlin.test.assertFailsWith

abstract class BaseConformanceTest {

    abstract fun getDriver(): SQLiteDriver

    @Test
    fun openAndCloseConnection() {
        val driver = getDriver()
        val connection = driver.open()
        try {
            val version = connection.prepare("PRAGMA user_version").use { statement ->
                statement.step()
                statement.getLong(0)
            }
            assertThat(version).isEqualTo(0)
        } finally {
            connection.close()
        }
    }

    @Test
    fun bindAndReadColumns() = testWithConnection { connection ->
        connection.execSQL(
            "CREATE TABLE Test(integerCol INTEGER, realCol REAL, textCol TEXT, blobCol BLOB)"
        )
        connection.prepare(
            "INSERT INTO Test (integerCol, realCol, textCol, blobCol) VALUES (?, ?, ?, ?)"
        ).use {
            it.bindLong(1, 3)
            it.bindDouble(2, 7.87)
            it.bindText(3, "PR")
            it.bindBlob(4, byteArrayOf(0x0F, 0x12, 0x1B))
            assertThat(it.step()).isFalse() // SQLITE_DONE
        }
        connection.prepare("SELECT * FROM Test").use {
            assertThat(it.step()).isTrue() // SQLITE_ROW
            assertThat(it.getColumnCount()).isEqualTo(4)
            assertThat(it.getColumnName(0)).isEqualTo("integerCol")
            assertThat(it.getColumnName(1)).isEqualTo("realCol")
            assertThat(it.getColumnName(2)).isEqualTo("textCol")
            assertThat(it.getColumnName(3)).isEqualTo("blobCol")
            assertThat(it.getLong(0)).isEqualTo(3)
            assertThat(it.getDouble(1)).isEqualTo(7.87)
            assertThat(it.getText(2)).isEqualTo("PR")
            assertThat(it.getBlob(3)).isEqualTo(byteArrayOf(0x0F, 0x12, 0x1B))
            assertThat(it.step()).isFalse() // SQLITE_DONE
        }
    }

    @Test
    fun bindAndReadTextUtf8() = testWithConnection { connection ->
        val konnichiwa = "こんにちわ"
        val world = "κόσμε"
        connection.execSQL("CREATE TABLE Test (textCol TEXT)")
        connection.prepare("INSERT INTO Test (textCol) VALUES (?)").use {
            it.bindText(1, konnichiwa)
            assertThat(it.step()).isFalse() // SQLITE_DONE
            it.reset()
            it.bindText(1, "Hello $world")
            assertThat(it.step()).isFalse() // SQLITE_DONE
        }
        connection.prepare("SELECT * FROM Test").use {
            assertThat(it.step()).isTrue() // SQLITE_ROW
            assertThat(it.getText(0)).isEqualTo(konnichiwa)
            assertThat(it.step()).isTrue() // SQLITE_ROW
            assertThat(it.getText(0)).isEqualTo("Hello $world")
        }
    }

    @Test
    fun bindAndReadNull() = testWithConnection { connection ->
        connection.execSQL("CREATE TABLE Test (col)")
        connection.prepare("INSERT INTO Test (col) VALUES (?)").use {
            it.bindNull(1)
            assertThat(it.step()).isFalse() // SQLITE_DONE
        }
        connection.prepare("SELECT * FROM Test").use {
            assertThat(it.step()).isTrue() // SQLITE_ROW
            assertThat(it.isNull(0)).isTrue()
        }
    }

    @Test
    open fun bindInvalidParam() = testWithConnection { connection ->
        connection.execSQL("CREATE TABLE Test (col)")
        connection.prepare("SELECT 1 FROM Test").use {
            var message: String?
            val expectedMessage = "Error code: 25, message: column index out of range"
            message = assertFailsWith<SQLiteException> { it.bindNull(1) }.message
            assertThat(message).isEqualTo(expectedMessage)
            message = assertFailsWith<SQLiteException> { it.bindBlob(1, byteArrayOf()) }.message
            assertThat(message).isEqualTo(expectedMessage)
            message = assertFailsWith<SQLiteException> { it.bindDouble(1, 0.0) }.message
            assertThat(message).isEqualTo(expectedMessage)
            message = assertFailsWith<SQLiteException> { it.bindLong(1, 0) }.message
            assertThat(message).isEqualTo(expectedMessage)
            message = assertFailsWith<SQLiteException> { it.bindText(1, "") }.message
            assertThat(message).isEqualTo(expectedMessage)
        }
    }

    @Test
    open fun readInvalidColumn() = testWithConnection { connection ->
        connection.execSQL("CREATE TABLE Test (col)")
        connection.execSQL("INSERT INTO Test (col) VALUES ('')")
        connection.prepare("SELECT * FROM Test").use {
            assertThat(it.step()).isTrue() // SQLITE_ROW
            var message: String?
            val expectedMessage = "Error code: 25, message: column index out of range"
            message = assertFailsWith<SQLiteException> { it.isNull(3) }.message
            assertThat(message).isEqualTo(expectedMessage)
            message = assertFailsWith<SQLiteException> { it.getBlob(3) }.message
            assertThat(message).isEqualTo(expectedMessage)
            message = assertFailsWith<SQLiteException> { it.getDouble(3) }.message
            assertThat(message).isEqualTo(expectedMessage)
            message = assertFailsWith<SQLiteException> { it.getLong(3) }.message
            assertThat(message).isEqualTo(expectedMessage)
            message = assertFailsWith<SQLiteException> { it.getText(3) }.message
            assertThat(message).isEqualTo(expectedMessage)
            message = assertFailsWith<SQLiteException> { it.getColumnName(3) }.message
            assertThat(message).isEqualTo(expectedMessage)
        }
    }

    @Test
    fun readColumnWithoutStep() = testWithConnection { connection ->
        connection.execSQL("CREATE TABLE Test (col)")
        connection.execSQL("INSERT INTO Test (col) VALUES ('')")
        connection.prepare("SELECT * FROM Test").use {
            val message = assertFailsWith<SQLiteException> { it.getText(1) }.message
            assertThat(message).isEqualTo("Error code: 21, message: no row")
        }
    }

    @Test
    open fun readColumnNameWithoutStep() = testWithConnection { connection ->
        connection.execSQL("CREATE TABLE Test (col)")
        connection.prepare("SELECT col FROM Test").use {
            assertThat(it.getColumnCount()).isEqualTo(1)
            assertThat(it.getColumnName(0)).isEqualTo("col")
        }
    }

    @Test
    open fun prepareInvalidReadStatement() = testWithConnection {
        assertThat(
            assertFailsWith<SQLiteException> {
                it.prepare("SELECT * FROM Foo").use { it.step() }
            }.message
        ).contains("no such table: Foo")
    }

    @Test
    open fun prepareInvalidWriteStatement() = testWithConnection {
        assertThat(
            assertFailsWith<SQLiteException> {
                it.execSQL("INSERT INTO Foo (id) VALUES (1)")
            }.message
        ).contains("no such table: Foo")
    }

    private inline fun testWithConnection(block: (SQLiteConnection) -> Unit) {
        val driver = getDriver()
        val connection = driver.open()
        try {
            block.invoke(connection)
        } finally {
            connection.close()
        }
    }
}
