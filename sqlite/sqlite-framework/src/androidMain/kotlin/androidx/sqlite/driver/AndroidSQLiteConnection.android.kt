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
package androidx.sqlite.driver

import android.database.sqlite.SQLiteDatabase
import androidx.annotation.RestrictTo
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.SQLiteStatement
import androidx.sqlite.db.framework.FrameworkSQLiteDatabase
import androidx.sqlite.driver.ResultCode.SQLITE_MISUSE
import androidx.sqlite.throwSQLiteException

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public class AndroidSQLiteConnection(public val db: SQLiteDatabase) : SQLiteConnection {

    override fun inTransaction(): Boolean = db.inTransaction()

    override fun prepare(sql: String): SQLiteStatement {
        if (db.isOpen) {
            return SupportSQLiteStatement.create(FrameworkSQLiteDatabase(db), sql)
        } else {
            throwSQLiteException(SQLITE_MISUSE, "connection is closed")
        }
    }

    override fun close() {
        db.close()
    }
}
