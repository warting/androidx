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

@file:JvmMultifileClass
@file:JvmName("RoomDatabaseKt")

package androidx.room

import androidx.annotation.RestrictTo
import androidx.room.migration.AutoMigrationSpec
import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.SQLiteDriver
import androidx.sqlite.SQLiteStatement
import kotlin.reflect.KClass

/**
 * Base class for all Room databases. All classes that are annotated with [Database] must
 * extend this class.
 *
 * RoomDatabase provides direct access to the underlying database implementation but you should
 * prefer using [Dao] classes.
 *
 * @see Database
 */
actual abstract class RoomDatabase {

    private lateinit var connectionManager: RoomJvmConnectionManager

    private val typeConverters: MutableMap<KClass<*>, Any> = mutableMapOf()

    /**
     * The invalidation tracker for this database.
     *
     * You can use the invalidation tracker to get notified when certain tables in the database
     * are modified.
     *
     * @return The invalidation tracker for the database.
     */
    actual val invalidationTracker: InvalidationTracker = createInvalidationTracker()

    /**
     * Called by Room when it is initialized.
     *
     * @param configuration The database configuration.
     * @throws IllegalArgumentException if initialization fails.
     */
    internal fun init(configuration: DatabaseConfiguration) {
        connectionManager = createConnectionManager(configuration) as RoomJvmConnectionManager
        validateAutoMigrations(configuration)
        validateTypeConverters(configuration)
    }

    /**
     * Creates a connection manager to manage database connection. Note that this method
     * is called when the [RoomDatabase] is initialized.
     *
     * @param configuration The database configuration
     * @return A new connection manager
     */
    internal actual fun createConnectionManager(
        configuration: DatabaseConfiguration
    ): RoomConnectionManager = RoomJvmConnectionManager(
        configuration = configuration,
        sqliteDriver = checkNotNull(configuration.sqliteDriver),
        openDelegate = createOpenDelegate() as RoomOpenDelegate
    )

    /**
     * Creates a delegate to configure and initialize the database when it is being opened.
     * An implementation of this function is generated by the Room processor. Note that this method
     * is called when the [RoomDatabase] is initialized.
     *
     * @return A new delegate to be used while opening the database
     * @throws NotImplementedError by default
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    protected actual open fun createOpenDelegate(): RoomOpenDelegateMarker {
        throw NotImplementedError()
    }

    /**
     * Creates the invalidation tracker
     *
     * An implementation of this function is generated by the Room processor. Note that this method
     * is called when the [RoomDatabase] is initialized.
     *
     * @return A new invalidation tracker.
     */
    protected actual abstract fun createInvalidationTracker(): InvalidationTracker

    /**
     * Returns a Set of required [AutoMigrationSpec] classes.
     *
     * An implementation of this function is generated by the Room processor. Note that this method
     * is called when the [RoomDatabase] is initialized.
     *
     * @return Creates a set that will include the classes of all required auto migration specs for
     * this database.
     * @throws NotImplementedError by default
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    actual open fun getRequiredAutoMigrationSpecClasses(): Set<KClass<out AutoMigrationSpec>> {
        throw NotImplementedError()
    }

    /**
     * Returns a list of automatic [Migration]s that have been generated.
     *
     * An implementation of this function is generated by the Room processor. Note that this method
     * is called when the [RoomDatabase] is initialized.
     *
     * @param autoMigrationSpecs the provided specs needed by certain migrations.
     * @return A list of migration instances each of which is a generated 'auto migration'.
     * @throws NotImplementedError by default
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    actual open fun createAutoMigrations(
        autoMigrationSpecs: Map<KClass<out AutoMigrationSpec>, AutoMigrationSpec>
    ): List<Migration> {
        throw NotImplementedError()
    }

    /**
     * Gets the instance of the given type converter class.
     *
     * This method should only be called by the generated DAO implementations.
     *
     * @param klass The Type Converter class.
     * @param T The type of the expected Type Converter subclass.
     * @return An instance of T.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @Suppress("UNCHECKED_CAST")
    actual fun <T : Any> getTypeConverter(klass: KClass<T>): T {
        return typeConverters[klass] as T
    }

    /**
     * Adds a provided type converter to be used in the database DAOs.
     *
     * @param kclass the class of the type converter
     * @param converter an instance of the converter
     */
    internal actual fun addTypeConverter(kclass: KClass<*>, converter: Any) {
        typeConverters[kclass] = converter
    }

    /**
     * Returns a Map of String -> List&lt;KClass&gt; where each entry has the `key` as the DAO name
     * and `value` as the list of type converter classes that are necessary for the database to
     * function.
     *
     * An implementation of this function is generated by the Room processor. Note that this method
     * is called when the [RoomDatabase] is initialized.
     *
     * @return A map that will include all required type converters for this database.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    protected actual open fun getRequiredTypeConverterClasses(): Map<KClass<*>, List<KClass<*>>> {
        throw NotImplementedError()
    }

    /**
     * Property delegate of [getRequiredTypeConverterClasses] for common ext functionality.
     */
    internal actual val requiredTypeConverterClasses: Map<KClass<*>, List<KClass<*>>>
        get() = getRequiredTypeConverterClasses()

    /**
     * Initialize invalidation tracker. Note that this method is called when the [RoomDatabase] is
     * initialized and opens a database connection.
     *
     * @param connection The database connection.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    protected actual fun internalInitInvalidationTracker(connection: SQLiteConnection) {
        invalidationTracker.internalInit(connection)
    }

    /**
     * Closes the database.
     *
     * Once a [RoomDatabase] is closed it should no longer be used.
     */
    actual fun close() {
        connectionManager.close()
    }

    /**
     * Performs a database operation.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    actual suspend fun <R> perform(
        isReadOnly: Boolean,
        sql: String,
        block: (SQLiteStatement) -> R
    ): R {
        return connectionManager.useConnection(isReadOnly) { connection ->
            connection.usePrepared(sql, block)
        }
    }

    /**
     * Performs a database operation in a transaction.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    actual suspend fun <R> performTransaction(
        isReadOnly: Boolean,
        block: suspend (TransactionScope<R>) -> R
    ): R {
        return connectionManager.useConnection(isReadOnly) { transactor ->
            val type = if (isReadOnly) {
                Transactor.SQLiteTransactionType.DEFERRED
            } else {
                Transactor.SQLiteTransactionType.IMMEDIATE
            }
            // TODO: Notify Invalidation Tracker before and after transaction block.
            transactor.withTransaction(type, block)
        }
    }

    /**
     * Journal modes for SQLite database.
     *
     * @see Builder.setJournalMode
     */
    actual enum class JournalMode {
        /**
         * Truncate journal mode.
         */
        TRUNCATE,

        /**
         * Write-Ahead Logging mode.
         */
        WRITE_AHEAD_LOGGING;
    }

    /**
     * Builder for [RoomDatabase].
     *
     * @param T The type of the abstract database class.
     * @param klass The abstract database class.
     * @param name The name of the database or NULL for an in-memory database.
     * @param factory The lambda calling `initializeImpl()` on the abstract database class which
     * returns the generated database implementation.
     */
    actual class Builder<T : RoomDatabase>
    @PublishedApi internal constructor(
        private val klass: KClass<T>,
        private val name: String?,
        private val factory: (() -> T)
    ) {

        private var driver: SQLiteDriver? = null

        /**
         * Sets the [SQLiteDriver] implementation to be used by Room to open database connections.
         * For example, an instance of [androidx.sqlite.driver.NativeSQLiteDriver] or
         * [androidx.sqlite.driver.bundled.BundledSQLiteDriver].
         *
         * @param driver The driver
         * @return This builder instance.
         */
        actual fun setDriver(driver: SQLiteDriver): Builder<T> = apply {
            this.driver = driver
        }

        /**
         * Creates the database and initializes it.
         *
         * @return A new database instance.
         * @throws IllegalArgumentException if the builder was misconfigured.
         */
        actual fun build(): T {
            requireNotNull(driver) {
                "Cannot create a RoomDatabase without providing a SQLiteDriver via setDriver()."
            }
            val configuration = DatabaseConfiguration(
                name = name,
                migrationContainer = MigrationContainer(),
                journalMode = JournalMode.WRITE_AHEAD_LOGGING,
                requireMigration = false,
                allowDestructiveMigrationOnDowngrade = false,
                migrationNotRequiredFrom = null,
                typeConverters = emptyList(),
                autoMigrationSpecs = emptyList(),
                sqliteDriver = driver
            )
            val db = factory.invoke()
            db.init(configuration)
            return db
        }
    }

    /**
     * A container to hold migrations. It also allows querying its contents to find migrations
     * between two versions.
     */
    actual class MigrationContainer {
        private val migrations = mutableMapOf<Int, MutableMap<Int, Migration>>()

        /**
         * Returns the map of available migrations where the key is the start version of the
         * migration, and the value is a map of (end version -> Migration).
         *
         * @return Map of migrations keyed by the start version
         */
        actual fun getMigrations(): Map<Int, Map<Int, Migration>> {
            return migrations
        }

        /**
         * Add a [Migration] to the container. If the container already has a migration with the
         * same start-end versions then it will be overwritten.
         *
         * @param migration the migration to add.
         */
        internal actual fun addMigration(migration: Migration) {
            val start = migration.startVersion
            val end = migration.endVersion
            val targetMap = migrations.getOrPut(start) { mutableMapOf() }
            targetMap[end] = migration
        }

        /**
         * Returns a pair corresponding to an entry in the map of available migrations whose key
         * is [migrationStart] and its sorted keys in ascending order.
         */
        internal actual fun getSortedNodes(
            migrationStart: Int
        ): Pair<Map<Int, Migration>, Iterable<Int>>? {
            val targetNodes = migrations[migrationStart] ?: return null
            return targetNodes to targetNodes.keys.sorted()
        }

        /**
         * Returns a pair corresponding to an entry in the map of available migrations whose key
         * is [migrationStart] and its sorted keys in descending order.
         */
        internal actual fun getSortedDescendingNodes(
            migrationStart: Int
        ): Pair<Map<Int, Migration>, Iterable<Int>>? {
            val targetNodes = migrations[migrationStart] ?: return null
            return targetNodes to targetNodes.keys.sortedDescending()
        }
    }
}
