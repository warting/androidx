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

package androidx.room

import androidx.annotation.RestrictTo
import androidx.room.RoomDatabase.JournalMode.TRUNCATE
import androidx.room.RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING
import androidx.room.concurrent.ExclusiveLock
import androidx.room.util.findMigrationPath
import androidx.room.util.isMigrationRequired
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.SQLiteDriver
import androidx.sqlite.execSQL

/** Expect implementation declaration of Room's connection manager. */
internal expect class RoomConnectionManager

/**
 * Base class for Room's database connection manager, responsible for opening and managing such
 * connections, including performing migrations if necessary and validating schema.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public abstract class BaseRoomConnectionManager {

    protected abstract val configuration: DatabaseConfiguration
    protected abstract val openDelegate: RoomOpenDelegate
    protected abstract val callbacks: List<RoomDatabase.Callback>

    // Flag indicating that the database was configured, i.e. at least one connection has been
    // opened, configured and schema validated.
    private var isConfigured = false
    // Flag set during initialization to prevent recursive initialization.
    private var isInitializing = false

    public abstract suspend fun <R> useConnection(
        isReadOnly: Boolean,
        block: suspend (Transactor) -> R
    ): R

    // Lets impl class resolve driver file name if necessary.
    internal open fun resolveFileName(fileName: String): String = fileName

    /* A driver wrapper that configures opened connections per the manager. */
    protected inner class DriverWrapper(private val actual: SQLiteDriver) : SQLiteDriver {
        override fun open(fileName: String): SQLiteConnection {
            return openLocked(resolveFileName(fileName))
        }

        private fun openLocked(filename: String) =
            ExclusiveLock(
                    filename = filename,
                    useFileLock = !isConfigured && !isInitializing && filename != ":memory:"
                )
                .withLock(
                    onLocked = {
                        check(!isInitializing) {
                            "Recursive database initialization detected. Did you try to use the " +
                                "database instance during initialization? Maybe in one of the " +
                                "callbacks?"
                        }
                        val connection = actual.open(filename)
                        if (!isConfigured) {
                            // Perform initial connection configuration
                            try {
                                isInitializing = true
                                configureDatabase(connection)
                            } finally {
                                isInitializing = false
                            }
                        } else {
                            // Perform other non-initial connection configuration
                            configurationConnection(connection)
                        }
                        return@withLock connection
                    },
                    onLockError = { error ->
                        throw IllegalStateException(
                            "Unable to open database '$filename'. Was a proper path / " +
                                "name used in Room's database builder?",
                            error
                        )
                    }
                )
    }

    /**
     * Performs initial database connection configuration and opening procedure, such as running
     * migrations if necessary, validating schema and invoking configured callbacks if any.
     */
    // TODO(b/316944352): Retry mechanism
    private fun configureDatabase(connection: SQLiteConnection) {
        configureJournalMode(connection)
        configureSynchronousFlag(connection)
        configureBusyTimeout(connection)
        val version =
            connection.prepare("PRAGMA user_version").use { statement ->
                statement.step()
                statement.getLong(0).toInt()
            }
        if (version != openDelegate.version) {
            connection.execSQL("BEGIN EXCLUSIVE TRANSACTION")
            runCatching {
                    if (version == 0) {
                        onCreate(connection)
                    } else {
                        onMigrate(connection, version, openDelegate.version)
                    }
                    connection.execSQL("PRAGMA user_version = ${openDelegate.version}")
                }
                .onSuccess { connection.execSQL("END TRANSACTION") }
                .onFailure {
                    connection.execSQL("ROLLBACK TRANSACTION")
                    throw it
                }
        }
        onOpen(connection)
    }

    /**
     * Performs non-initial database connection configuration, specifically executing any
     * per-connection PRAGMA.
     */
    private fun configurationConnection(connection: SQLiteConnection) {
        configureSynchronousFlag(connection)
        configureBusyTimeout(connection)
        openDelegate.onOpen(connection)
    }

    private fun configureJournalMode(connection: SQLiteConnection) {
        val wal = configuration.journalMode == WRITE_AHEAD_LOGGING
        if (wal) {
            connection.execSQL("PRAGMA journal_mode = WAL")
        } else {
            connection.execSQL("PRAGMA journal_mode = TRUNCATE")
        }
    }

    private fun configureSynchronousFlag(connection: SQLiteConnection) {
        // Use NORMAL in WAL mode and FULL for non-WAL as recommended in
        // https://www.sqlite.org/pragma.html#pragma_synchronous
        val wal = configuration.journalMode == WRITE_AHEAD_LOGGING
        if (wal) {
            connection.execSQL("PRAGMA synchronous = NORMAL")
        } else {
            connection.execSQL("PRAGMA synchronous = FULL")
        }
    }

    private fun configureBusyTimeout(connection: SQLiteConnection) {
        // Set a busy timeout if no timeout is set to avoid SQLITE_BUSY during slow I/O or during
        // an auto-checkpoint.
        val currentBusyTimeout =
            connection.prepare("PRAGMA busy_timeout").use {
                it.step()
                it.getLong(0)
            }
        if (currentBusyTimeout < BUSY_TIMEOUT_MS) {
            connection.execSQL("PRAGMA busy_timeout = $BUSY_TIMEOUT_MS")
        }
    }

    protected fun onCreate(connection: SQLiteConnection) {
        val isEmptyDatabase = hasEmptySchema(connection)
        openDelegate.createAllTables(connection)
        if (!isEmptyDatabase) {
            // A 0 version pre-populated database goes through the create path, Room only allows
            // for versions greater than 0, so if we find the database not to be empty, then it is
            // a pre-populated, we must validate it to see if its suitable for usage.
            val result = openDelegate.onValidateSchema(connection)
            if (!result.isValid) {
                error("Pre-packaged database has an invalid schema: ${result.expectedFoundMsg}")
            }
        }
        updateIdentity(connection)
        openDelegate.onCreate(connection)
        invokeCreateCallback(connection)
    }

    private fun hasEmptySchema(connection: SQLiteConnection): Boolean =
        connection
            .prepare("SELECT count(*) FROM sqlite_master WHERE name != 'android_metadata'")
            .use { it.step() && it.getLong(0) == 0L }

    private fun updateIdentity(connection: SQLiteConnection) {
        createMasterTableIfNotExists(connection)
        connection.execSQL(RoomMasterTable.createInsertQuery(openDelegate.identityHash))
    }

    private fun createMasterTableIfNotExists(connection: SQLiteConnection) {
        connection.execSQL(RoomMasterTable.CREATE_QUERY)
    }

    protected fun onMigrate(connection: SQLiteConnection, oldVersion: Int, newVersion: Int) {
        var migrated = false
        val migrations = configuration.migrationContainer.findMigrationPath(oldVersion, newVersion)
        if (migrations != null) {
            openDelegate.onPreMigrate(connection)
            migrations.forEach { it.migrate(connection) }
            val result = openDelegate.onValidateSchema(connection)
            if (!result.isValid) {
                error("Migration didn't properly handle: ${result.expectedFoundMsg}")
            }
            openDelegate.onPostMigrate(connection)
            updateIdentity(connection)
            migrated = true
        }
        if (!migrated) {
            if (configuration.isMigrationRequired(oldVersion, newVersion)) {
                error(
                    "A migration from $oldVersion to $newVersion was required but not found. " +
                        "Please provide the necessary Migration path via " +
                        "RoomDatabase.Builder.addMigration(...) or allow for " +
                        "destructive migrations via one of the " +
                        "RoomDatabase.Builder.fallbackToDestructiveMigration* functions."
                )
            }
            dropAllTables(connection)
            invokeDestructiveMigrationCallback(connection)
            openDelegate.createAllTables(connection)
        }
    }

    private fun dropAllTables(connection: SQLiteConnection) {
        if (configuration.allowDestructiveMigrationForAllTables) {
            // Drops all tables and views (excluding special ones)
            connection
                .prepare(
                    "SELECT name, type FROM sqlite_master WHERE type = 'table' OR type = 'view'"
                )
                .use { statement ->
                    buildList {
                        while (statement.step()) {
                            val name = statement.getText(0)
                            if (name.startsWith("sqlite_") || name == "android_metadata") {
                                continue
                            }
                            val isView = statement.getText(1) == "view"
                            add(name to isView)
                        }
                    }
                }
                .forEach { (name, isView) ->
                    if (isView) {
                        connection.execSQL("DROP VIEW IF EXISTS $name")
                    } else {
                        connection.execSQL("DROP TABLE IF EXISTS $name")
                    }
                }
        } else {
            // Drops known tables (Room entity tables)
            openDelegate.dropAllTables(connection)
        }
    }

    protected fun onOpen(connection: SQLiteConnection) {
        checkIdentity(connection)
        openDelegate.onOpen(connection)
        invokeOpenCallback(connection)
        isConfigured = true
    }

    private fun checkIdentity(connection: SQLiteConnection) {
        if (hasRoomMasterTable(connection)) {
            val identityHash: String? =
                connection.prepare(RoomMasterTable.READ_QUERY).use {
                    if (it.step()) {
                        it.getText(0)
                    } else {
                        null
                    }
                }
            if (
                openDelegate.identityHash != identityHash &&
                    openDelegate.legacyIdentityHash != identityHash
            ) {
                error(
                    "Room cannot verify the data integrity. Looks like" +
                        " you've changed schema but forgot to update the version number. You can" +
                        " simply fix this by increasing the version number. Expected identity" +
                        " hash: ${openDelegate.identityHash}, found: $identityHash"
                )
            }
        } else {
            connection.execSQL("BEGIN EXCLUSIVE TRANSACTION")
            runCatching {
                    // No room_master_table, this might an a pre-populated DB, we must validate to
                    // see
                    // if it's suitable for usage.
                    val result = openDelegate.onValidateSchema(connection)
                    if (!result.isValid) {
                        error(
                            "Pre-packaged database has an invalid schema: ${result.expectedFoundMsg}"
                        )
                    }
                    openDelegate.onPostMigrate(connection)
                    updateIdentity(connection)
                }
                .onSuccess { connection.execSQL("END TRANSACTION") }
                .onFailure {
                    connection.execSQL("ROLLBACK TRANSACTION")
                    throw it
                }
        }
    }

    private fun hasRoomMasterTable(connection: SQLiteConnection): Boolean =
        connection
            .prepare(
                "SELECT 1 FROM sqlite_master " +
                    "WHERE type = 'table' AND name = '${RoomMasterTable.TABLE_NAME}'"
            )
            .use { it.step() && it.getLong(0) != 0L }

    @Suppress("REDUNDANT_ELSE_IN_WHEN") // Redundant in common but not in Android
    protected fun RoomDatabase.JournalMode.getMaxNumberOfReaders() =
        when (this) {
            TRUNCATE -> 1
            WRITE_AHEAD_LOGGING -> 4
            else -> error("Can't get max number of reader for journal mode '$this'")
        }

    @Suppress("REDUNDANT_ELSE_IN_WHEN") // Redundant in common but not in Android
    protected fun RoomDatabase.JournalMode.getMaxNumberOfWriters() =
        when (this) {
            TRUNCATE -> 1
            WRITE_AHEAD_LOGGING -> 1
            else -> error("Can't get max number of writers for journal mode '$this'")
        }

    private fun invokeCreateCallback(connection: SQLiteConnection) {
        callbacks.forEach { it.onCreate(connection) }
    }

    private fun invokeDestructiveMigrationCallback(connection: SQLiteConnection) {
        callbacks.forEach { it.onDestructiveMigration(connection) }
    }

    private fun invokeOpenCallback(connection: SQLiteConnection) {
        callbacks.forEach { it.onOpen(connection) }
    }

    public companion object {
        /*
         * Busy timeout amount. This wait time is relevant to same-process connections, if a
         * database is used across multiple processes, it is recommended that the developer sets a
         * higher timeout.
         */
        public const val BUSY_TIMEOUT_MS: Int = 3000
    }
}
