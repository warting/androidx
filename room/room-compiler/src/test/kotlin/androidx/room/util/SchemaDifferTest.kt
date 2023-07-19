/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.room.util

import androidx.room.migration.bundle.DatabaseBundle
import androidx.room.migration.bundle.EntityBundle
import androidx.room.migration.bundle.FieldBundle
import androidx.room.migration.bundle.ForeignKeyBundle
import androidx.room.migration.bundle.IndexBundle
import androidx.room.migration.bundle.PrimaryKeyBundle
import androidx.room.migration.bundle.SchemaBundle
import androidx.room.migration.bundle.TABLE_NAME_PLACEHOLDER
import androidx.room.processor.ProcessorErrors
import androidx.room.vo.AutoMigration
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.fail
import org.junit.Test

class SchemaDifferTest {

    @Test
    fun testPrimaryKeyChanged() {
        val diffResult = SchemaDiffer(
            fromSchemaBundle = from.database,
            toSchemaBundle = toChangeInPrimaryKey.database,
            className = "MyAutoMigration",
            renameColumnEntries = listOf(),
            deleteColumnEntries = listOf(),
            renameTableEntries = listOf(),
            deleteTableEntries = listOf()
        ).diffSchemas()

        assertThat(diffResult.complexChangedTables.keys).contains("Song")
    }

    @Test
    fun testForeignKeyFieldChanged() {
        val diffResult = SchemaDiffer(
            fromSchemaBundle = from.database,
            toSchemaBundle = toForeignKeyAdded.database,
            className = "MyAutoMigration",
            renameColumnEntries = listOf(),
            deleteColumnEntries = listOf(),
            renameTableEntries = listOf(),
            deleteTableEntries = listOf()
        ).diffSchemas()

        assertThat(diffResult.complexChangedTables["Song"] != null)
    }

    @Test
    fun testComplexChangeInvolvingIndex() {
        val diffResult = SchemaDiffer(
            fromSchemaBundle = from.database,
            toSchemaBundle = toIndexAdded.database,
            className = "MyAutoMigration",
            renameColumnEntries = listOf(),
            deleteColumnEntries = listOf(),
            renameTableEntries = listOf(),
            deleteTableEntries = listOf()
        ).diffSchemas()

        assertThat(diffResult.complexChangedTables["Song"] != null)
    }

    @Test
    fun testColumnAddedWithColumnInfoDefaultValue() {
        val schemaDiffResult = SchemaDiffer(
            fromSchemaBundle = from.database,
            toSchemaBundle = toColumnAddedWithColumnInfoDefaultValue.database,
            className = "MyAutoMigration",
            renameColumnEntries = listOf(),
            deleteColumnEntries = listOf(),
            renameTableEntries = listOf(),
            deleteTableEntries = listOf()
        ).diffSchemas()
        assertThat(schemaDiffResult.addedColumns.single().fieldBundle.columnName)
            .isEqualTo("artistId")
    }

    @Test
    fun testColumnsAddedInOrder() {
        val schemaDiffResult = SchemaDiffer(
            fromSchemaBundle = from.database,
            toSchemaBundle = toColumnsAddedInOrder.database,
            className = "MyAutoMigration",
            renameColumnEntries = listOf(),
            deleteColumnEntries = listOf(),
            renameTableEntries = listOf(),
            deleteTableEntries = listOf()
        ).diffSchemas()
        assertThat(schemaDiffResult.addedColumns).hasSize(2)
        assertThat(schemaDiffResult.addedColumns[0].fieldBundle.columnName)
            .isEqualTo("recordLabelId")
        assertThat(schemaDiffResult.addedColumns[1].fieldBundle.columnName)
            .isEqualTo("artistId")
    }

    @Test
    fun testColumnAddedWithNoDefaultValue() {
        try {
            SchemaDiffer(
                fromSchemaBundle = from.database,
                toSchemaBundle = toColumnAddedWithNoDefaultValue.database,
                className = "MyAutoMigration",
                renameColumnEntries = listOf(),
                deleteColumnEntries = listOf(),
                renameTableEntries = listOf(),
                deleteTableEntries = listOf()
            ).diffSchemas()
            fail("DiffException should have been thrown.")
        } catch (ex: DiffException) {
            assertThat(ex.errorMessage).isEqualTo(
                ProcessorErrors.newNotNullColumnMustHaveDefaultValue("artistId")
            )
        }
    }

    @Test
    fun testTableAddedWithColumnInfoDefaultValue() {
        val schemaDiffResult = SchemaDiffer(
            fromSchemaBundle = from.database,
            toSchemaBundle = toTableAddedWithColumnInfoDefaultValue.database,
            className = "MyAutoMigration",
            renameColumnEntries = listOf(),
            deleteColumnEntries = listOf(),
            renameTableEntries = listOf(),
            deleteTableEntries = listOf()
        ).diffSchemas()
        assertThat(schemaDiffResult.addedTables.toList()[0].entityBundle.tableName)
            .isEqualTo("Album")
    }

    @Test
    fun testColumnsAddedWithSameName() {
        val schemaDiffResult = SchemaDiffer(
            fromSchemaBundle = from.database,
            toSchemaBundle = toColumnsAddedWithSameName.database,
            className = "MyAutoMigration",
            renameColumnEntries = listOf(),
            deleteColumnEntries = listOf(),
            renameTableEntries = listOf(),
            deleteTableEntries = listOf()
        ).diffSchemas()
        assertThat(schemaDiffResult.addedColumns).hasSize(2)
        assertThat(
            schemaDiffResult.addedColumns.any {
                it.tableName == "Song" && it.fieldBundle.columnName == "newColumn"
            }
        ).isTrue()
        assertThat(
            schemaDiffResult.addedColumns.any {
                it.tableName == "Artist" && it.fieldBundle.columnName == "newColumn"
            }
        ).isTrue()
    }

    @Test
    fun testColumnRenamed() {
        try {
            SchemaDiffer(
                fromSchemaBundle = from.database,
                toSchemaBundle = toColumnRenamed.database,
                className = "MyAutoMigration",
                renameColumnEntries = listOf(),
                deleteColumnEntries = listOf(),
                renameTableEntries = listOf(),
                deleteTableEntries = listOf()
            ).diffSchemas()
            fail("DiffException should have been thrown.")
        } catch (ex: DiffException) {
            assertThat(ex.errorMessage).isEqualTo(
                ProcessorErrors.deletedOrRenamedColumnFound("MyAutoMigration", "length", "Song")
            )
        }
    }

    @Test
    fun testColumnRemoved() {
        try {
            SchemaDiffer(
                fromSchemaBundle = from.database,
                toSchemaBundle = toColumnRemoved.database,
                className = "MyAutoMigration",
                renameColumnEntries = listOf(),
                deleteColumnEntries = listOf(),
                renameTableEntries = listOf(),
                deleteTableEntries = listOf()
            ).diffSchemas()
            fail("DiffException should have been thrown.")
        } catch (ex: DiffException) {
            assertThat(ex.errorMessage).isEqualTo(
                ProcessorErrors.deletedOrRenamedColumnFound("MyAutoMigration", "length", "Song")
            )
        }
    }

    @Test
    fun testTableRenamedWithoutAnnotation() {
        try {
            SchemaDiffer(
                fromSchemaBundle = from.database,
                toSchemaBundle = toTableRenamed.database,
                className = "MyAutoMigration",
                renameColumnEntries = listOf(),
                deleteColumnEntries = listOf(),
                renameTableEntries = listOf(),
                deleteTableEntries = listOf()
            ).diffSchemas()
            fail("DiffException should have been thrown.")
        } catch (ex: DiffException) {
            assertThat(ex.errorMessage).isEqualTo(
                ProcessorErrors.deletedOrRenamedTableFound("MyAutoMigration", "Artist")
            )
        }
    }

    @Test
    fun testTableRemovedWithoutAnnotation() {
        try {
            SchemaDiffer(
                fromSchemaBundle = from.database,
                toSchemaBundle = toTableDeleted.database,
                className = "MyAutoMigration",
                renameColumnEntries = listOf(),
                deleteColumnEntries = listOf(),
                renameTableEntries = listOf(),
                deleteTableEntries = listOf()
            ).diffSchemas()
            fail("DiffException should have been thrown.")
        } catch (ex: DiffException) {
            assertThat(ex.errorMessage).isEqualTo(
                ProcessorErrors.deletedOrRenamedTableFound("MyAutoMigration", "Artist")
            )
        }
    }

    @Test
    fun testRenameTwoColumnsOnComplexChangedTable() {
        val fromSchemaBundle = SchemaBundle(
            1,
            DatabaseBundle(
                1,
                "",
                mutableListOf(
                    EntityBundle(
                        "Song",
                        "CREATE TABLE IF NOT EXISTS `$TABLE_NAME_PLACEHOLDER` (`id` " +
                            "INTEGER NOT NULL, " +
                            "`title` TEXT NOT NULL, `length` INTEGER NOT NULL, PRIMARY KEY(`id`))",
                        listOf(
                            FieldBundle(
                                "id",
                                "id",
                                "INTEGER",
                                true,
                                "1"
                            ),
                            FieldBundle(
                                "title",
                                "title",
                                "TEXT",
                                true,
                                ""
                            ),
                            FieldBundle(
                                "length",
                                "length",
                                "INTEGER",
                                true,
                                "1"
                            )
                        ),
                        PrimaryKeyBundle(
                            false,
                            mutableListOf("id")
                        ),
                        mutableListOf(),
                        mutableListOf()
                    )
                ),
                mutableListOf(),
                mutableListOf()
            )
        )
        val toSchemaBundle = SchemaBundle(
            2,
            DatabaseBundle(
                2,
                "",
                mutableListOf(
                    EntityBundle(
                        "SongTable",
                        "CREATE TABLE IF NOT EXISTS `$TABLE_NAME_PLACEHOLDER` (`id` " +
                            "INTEGER NOT NULL, " +
                            "`songTitle` TEXT NOT NULL, `songLength` " +
                            "INTEGER NOT NULL, PRIMARY KEY(`id`))",
                        listOf(
                            FieldBundle(
                                "id",
                                "id",
                                "INTEGER",
                                true,
                                "1"
                            ),
                            FieldBundle(
                                "songTitle",
                                "songTitle",
                                "TEXT",
                                true,
                                ""
                            ),
                            FieldBundle(
                                "songLength",
                                "songLength",
                                "INTEGER",
                                true,
                                "1"
                            )
                        ),
                        PrimaryKeyBundle(
                            false,
                            mutableListOf("id")
                        ),
                        mutableListOf(),
                        mutableListOf()
                    )
                ),
                mutableListOf(),
                mutableListOf()
            )
        )
        val schemaDiffResult = SchemaDiffer(
            fromSchemaBundle = fromSchemaBundle.database,
            toSchemaBundle = toSchemaBundle.database,
            className = "MyAutoMigration",
            renameColumnEntries = listOf(
                AutoMigration.RenamedColumn("Song", "title", "songTitle"),
                AutoMigration.RenamedColumn("Song", "length", "songLength")
            ),
            deleteColumnEntries = listOf(),
            renameTableEntries = listOf(
                AutoMigration.RenamedTable("Song", "SongTable")
            ),
            deleteTableEntries = listOf()
        ).diffSchemas()
        assertThat(schemaDiffResult.complexChangedTables.size).isEqualTo(1)
        schemaDiffResult.complexChangedTables.values.single().let { complexChange ->
            assertThat(complexChange.tableName).isEqualTo("Song")
            assertThat(complexChange.tableNameWithNewPrefix).isEqualTo("_new_SongTable")
            assertThat(complexChange.renamedColumnsMap).containsExactlyEntriesIn(
                mapOf("songTitle" to "title", "songLength" to "length")
            )
        }
    }

    private val from = SchemaBundle(
        1,
        DatabaseBundle(
            1,
            "",
            mutableListOf(
                EntityBundle(
                    "Song",
                    "CREATE TABLE IF NOT EXISTS `Song` (`id` INTEGER NOT NULL, " +
                        "`title` TEXT NOT NULL, `length` INTEGER NOT NULL, PRIMARY KEY(`id`))",
                    listOf(
                        FieldBundle(
                            "id",
                            "id",
                            "INTEGER",
                            true,
                            "1"
                        ),
                        FieldBundle(
                            "title",
                            "title",
                            "TEXT",
                            true,
                            ""
                        ),
                        FieldBundle(
                            "length",
                            "length",
                            "INTEGER",
                            true,
                            "1"
                        )
                    ),
                    PrimaryKeyBundle(
                        false,
                        mutableListOf("id")
                    ),
                    mutableListOf(),
                    mutableListOf()
                ),
                EntityBundle(
                    "Artist",
                    "CREATE TABLE IF NOT EXISTS `Artist` (`id` INTEGER NOT NULL, " +
                        "`title` TEXT NOT NULL, `length` INTEGER NOT NULL, PRIMARY KEY(`id`))",
                    listOf(
                        FieldBundle(
                            "id",
                            "id",
                            "INTEGER",
                            true,
                            "1"
                        ),
                        FieldBundle(
                            "title",
                            "title",
                            "TEXT",
                            true,
                            ""
                        ),
                        FieldBundle(
                            "length",
                            "length",
                            "INTEGER",
                            true,
                            "1"
                        )
                    ),
                    PrimaryKeyBundle(
                        false,
                        mutableListOf("id")
                    ),
                    mutableListOf(),
                    mutableListOf()
                )
            ),
            mutableListOf(),
            mutableListOf()
        )
    )

    //region Valid "to" Schemas

    private val toTableRenamed = SchemaBundle(
        2,
        DatabaseBundle(
            2,
            "",
            mutableListOf(
                EntityBundle(
                    "Song",
                    "CREATE TABLE IF NOT EXISTS `Song` (`id` INTEGER NOT NULL, " +
                        "`title` TEXT NOT NULL, `length` INTEGER NOT NULL, PRIMARY KEY(`id`))",
                    listOf(
                        FieldBundle(
                            "id",
                            "id",
                            "INTEGER",
                            true,
                            "1"
                        ),
                        FieldBundle(
                            "title",
                            "title",
                            "TEXT",
                            true,
                            ""
                        ),
                        FieldBundle(
                            "length",
                            "length",
                            "INTEGER",
                            true,
                            "1"
                        )
                    ),
                    PrimaryKeyBundle(
                        false,
                        mutableListOf("id")
                    ),
                    mutableListOf(),
                    mutableListOf()
                ),
                EntityBundle(
                    "Album",
                    "CREATE TABLE IF NOT EXISTS `Song` (`id` INTEGER NOT NULL, " +
                        "`title` TEXT NOT NULL, `length` INTEGER NOT NULL, PRIMARY KEY(`id`))",
                    listOf(
                        FieldBundle(
                            "id",
                            "id",
                            "INTEGER",
                            true,
                            "1"
                        ),
                        FieldBundle(
                            "title",
                            "title",
                            "TEXT",
                            true,
                            ""
                        ),
                        FieldBundle(
                            "length",
                            "length",
                            "INTEGER",
                            true,
                            "1"
                        )
                    ),
                    PrimaryKeyBundle(
                        false,
                        mutableListOf("id")
                    ),
                    mutableListOf(),
                    mutableListOf()
                )
            ),
            mutableListOf(),
            mutableListOf()
        )
    )

    private val toTableDeleted = SchemaBundle(
        2,
        DatabaseBundle(
            2,
            "",
            mutableListOf(
                EntityBundle(
                    "Song",
                    "CREATE TABLE IF NOT EXISTS `Song` (`id` INTEGER NOT NULL, " +
                        "`title` TEXT NOT NULL, `length` INTEGER NOT NULL, PRIMARY KEY(`id`))",
                    listOf(
                        FieldBundle(
                            "id",
                            "id",
                            "INTEGER",
                            true,
                            "1"
                        ),
                        FieldBundle(
                            "title",
                            "title",
                            "TEXT",
                            true,
                            ""
                        ),
                        FieldBundle(
                            "length",
                            "length",
                            "INTEGER",
                            true,
                            "1"
                        )
                    ),
                    PrimaryKeyBundle(
                        false,
                        mutableListOf("id")
                    ),
                    mutableListOf(),
                    mutableListOf()
                )
            ),
            mutableListOf(),
            mutableListOf()
        )
    )

    private val toColumnAddedWithColumnInfoDefaultValue = SchemaBundle(
        2,
        DatabaseBundle(
            2,
            "",
            listOf(
                EntityBundle(
                    "Song",
                    "CREATE TABLE IF NOT EXISTS `Song` (`id` INTEGER NOT NULL, " +
                        "`title` TEXT NOT NULL, `length` INTEGER NOT NULL, `artistId` " +
                        "INTEGER NOT NULL DEFAULT 0, PRIMARY KEY(`id`))",
                    listOf(
                        FieldBundle(
                            "id",
                            "id",
                            "INTEGER",
                            true,
                            "1"
                        ),
                        FieldBundle(
                            "title",
                            "title",
                            "TEXT",
                            true,
                            ""
                        ),
                        FieldBundle(
                            "length",
                            "length",
                            "INTEGER",
                            true,
                            "1"
                        ),
                        FieldBundle(
                            "artistId",
                            "artistId",
                            "INTEGER",
                            true,
                            "0"
                        )
                    ),
                    PrimaryKeyBundle(
                        false,
                        mutableListOf("id")
                    ),
                    emptyList(),
                    emptyList()
                ),
                EntityBundle(
                    "Artist",
                    "CREATE TABLE IF NOT EXISTS `Artist` (`id` INTEGER NOT NULL, " +
                        "`title` TEXT NOT NULL, `length` INTEGER NOT NULL, PRIMARY KEY(`id`))",
                    listOf(
                        FieldBundle(
                            "id",
                            "id",
                            "INTEGER",
                            true,
                            "1"
                        ),
                        FieldBundle(
                            "title",
                            "title",
                            "TEXT",
                            true,
                            ""
                        ),
                        FieldBundle(
                            "length",
                            "length",
                            "INTEGER",
                            true,
                            "1"
                        )
                    ),
                    PrimaryKeyBundle(
                        false,
                        mutableListOf("id")
                    ),
                    mutableListOf(),
                    mutableListOf()
                )
            ),
            mutableListOf(),
            mutableListOf()
        )
    )

    /**
     * Adding multiple columns, preserving the order in which they have been added.
     */
    private val toColumnsAddedInOrder = SchemaBundle(
        2,
        DatabaseBundle(
            2,
            "",
            listOf(
                EntityBundle(
                    "Song",
                    "CREATE TABLE IF NOT EXISTS `Song` (`id` INTEGER NOT NULL, " +
                        "`title` TEXT NOT NULL, `length` INTEGER NOT NULL, `artistId` " +
                        "INTEGER NOT NULL, PRIMARY KEY(`id`))",
                    listOf(
                        FieldBundle(
                            "id",
                            "id",
                            "INTEGER",
                            true,
                            "1"
                        ),
                        FieldBundle(
                            "title",
                            "title",
                            "TEXT",
                            true,
                            ""
                        ),
                        FieldBundle(
                            "length",
                            "length",
                            "INTEGER",
                            true,
                            "1"
                        ),
                        FieldBundle(
                            "recordLabelId",
                            "recordLabelId",
                            "INTEGER",
                            true,
                            "0"
                        ),
                        FieldBundle(
                            "artistId",
                            "artistId",
                            "INTEGER",
                            true,
                            "0"
                        )
                    ),
                    PrimaryKeyBundle(
                        false,
                        mutableListOf("id")
                    ),
                    emptyList(),
                    emptyList()
                ),
                EntityBundle(
                    "Artist",
                    "CREATE TABLE IF NOT EXISTS `Artist` (`id` INTEGER NOT NULL, " +
                        "`title` TEXT NOT NULL, `length` INTEGER NOT NULL, PRIMARY KEY(`id`))",
                    listOf(
                        FieldBundle(
                            "id",
                            "id",
                            "INTEGER",
                            true,
                            "1"
                        ),
                        FieldBundle(
                            "title",
                            "title",
                            "TEXT",
                            true,
                            ""
                        ),
                        FieldBundle(
                            "length",
                            "length",
                            "INTEGER",
                            true,
                            "1"
                        ),
                    ),
                    PrimaryKeyBundle(
                        false,
                        mutableListOf("id")
                    ),
                    mutableListOf(),
                    mutableListOf()
                )
            ),
            mutableListOf(),
            mutableListOf()
        )
    )

    /**
     * Adding multiple columns, preserving the order in which they have been added.
     */
    private val toColumnsAddedWithSameName = SchemaBundle(
        2,
        DatabaseBundle(
            2,
            "",
            listOf(
                EntityBundle(
                    "Song",
                    "CREATE TABLE IF NOT EXISTS `Song` (`id` INTEGER NOT NULL, " +
                        "`title` TEXT NOT NULL, `length` INTEGER NOT NULL, `artistId` " +
                        "INTEGER NOT NULL, PRIMARY KEY(`id`))",
                    listOf(
                        FieldBundle(
                            "id",
                            "id",
                            "INTEGER",
                            true,
                            "1"
                        ),
                        FieldBundle(
                            "title",
                            "title",
                            "TEXT",
                            true,
                            ""
                        ),
                        FieldBundle(
                            "length",
                            "length",
                            "INTEGER",
                            true,
                            "1"
                        ),
                        FieldBundle(
                            "newColumn",
                            "newColumn",
                            "INTEGER",
                            true,
                            "0"
                        ),
                    ),
                    PrimaryKeyBundle(
                        false,
                        mutableListOf("id")
                    ),
                    emptyList(),
                    emptyList()
                ),
                EntityBundle(
                    "Artist",
                    "CREATE TABLE IF NOT EXISTS `Artist` (`id` INTEGER NOT NULL, " +
                        "`title` TEXT NOT NULL, `length` INTEGER NOT NULL, PRIMARY KEY(`id`))",
                    listOf(
                        FieldBundle(
                            "id",
                            "id",
                            "INTEGER",
                            true,
                            "1"
                        ),
                        FieldBundle(
                            "title",
                            "title",
                            "TEXT",
                            true,
                            ""
                        ),
                        FieldBundle(
                            "length",
                            "length",
                            "INTEGER",
                            true,
                            "1"
                        ),
                        FieldBundle(
                            "newColumn",
                            "newColumn",
                            "INTEGER",
                            true,
                            "0"
                        ),
                    ),
                    PrimaryKeyBundle(
                        false,
                        mutableListOf("id")
                    ),
                    mutableListOf(),
                    mutableListOf()
                )
            ),
            mutableListOf(),
            mutableListOf()
        )
    )

    /**
     * Renaming the length column to duration.
     */
    private val toColumnRenamed = SchemaBundle(
        2,
        DatabaseBundle(
            2,
            "",
            mutableListOf(
                EntityBundle(
                    "Song",
                    "CREATE TABLE IF NOT EXISTS `Song` (`id` INTEGER NOT " +
                        "NULL, `title` TEXT NOT NULL, `duration` INTEGER NOT NULL DEFAULT 0, " +
                        "PRIMARY KEY(`id`))",
                    listOf(
                        FieldBundle(
                            "id",
                            "id",
                            "INTEGER",
                            true,
                            "1"
                        ),
                        FieldBundle(
                            "title",
                            "title",
                            "TEXT",
                            true,
                            ""
                        ),
                        FieldBundle(
                            "duration",
                            "duration",
                            "INTEGER",
                            true,
                            "0"
                        )
                    ),
                    PrimaryKeyBundle(
                        false,
                        mutableListOf("id")
                    ),
                    mutableListOf(),
                    mutableListOf()
                ),
                EntityBundle(
                    "Artist",
                    "CREATE TABLE IF NOT EXISTS `Artist` (`id` INTEGER NOT NULL, " +
                        "`title` TEXT NOT NULL, `length` INTEGER NOT NULL, PRIMARY KEY(`id`))",
                    listOf(
                        FieldBundle(
                            "id",
                            "id",
                            "INTEGER",
                            true,
                            "1"
                        ),
                        FieldBundle(
                            "title",
                            "title",
                            "TEXT",
                            true,
                            ""
                        ),
                        FieldBundle(
                            "length",
                            "length",
                            "INTEGER",
                            true,
                            "1"
                        )
                    ),
                    PrimaryKeyBundle(
                        false,
                        mutableListOf("id")
                    ),
                    mutableListOf(),
                    mutableListOf()
                )
            ),
            mutableListOf(),
            mutableListOf()
        )
    )

    /**
     * The affinity of a length column is changed from Integer to Text. No columns are
     * added/removed.
     */
    val toColumnAffinityChanged = SchemaBundle(
        2,
        DatabaseBundle(
            2,
            "",
            mutableListOf(
                EntityBundle(
                    "Song",
                    "CREATE TABLE IF NOT EXISTS `Song` (`id` INTEGER NOT NULL, " +
                        "`title` TEXT NOT NULL, `length` TEXT NOT NULL DEFAULT length, " +
                        "PRIMARY KEY(`id`))",
                    listOf(
                        FieldBundle(
                            "id",
                            "id",
                            "INTEGER",
                            true,
                            "1"
                        ),
                        FieldBundle(
                            "title",
                            "title",
                            "TEXT",
                            true,
                            ""
                        ),
                        FieldBundle(
                            "length",
                            "length",
                            "TEXT",
                            true,
                            "length"
                        )
                    ),
                    PrimaryKeyBundle(
                        false,
                        mutableListOf("id")
                    ),
                    mutableListOf(),
                    mutableListOf()
                ),
                EntityBundle(
                    "Artist",
                    "CREATE TABLE IF NOT EXISTS `Artist` (`id` INTEGER NOT NULL, " +
                        "`title` TEXT NOT NULL, `length` INTEGER NOT NULL, PRIMARY KEY(`id`))",
                    listOf(
                        FieldBundle(
                            "id",
                            "id",
                            "INTEGER",
                            true,
                            "1"
                        ),
                        FieldBundle(
                            "title",
                            "title",
                            "TEXT",
                            true,
                            ""
                        ),
                        FieldBundle(
                            "length",
                            "length",
                            "INTEGER",
                            true,
                            "1"
                        )
                    ),
                    PrimaryKeyBundle(
                        false,
                        mutableListOf("id")
                    ),
                    mutableListOf(),
                    mutableListOf()
                )
            ),
            mutableListOf(),
            mutableListOf()
        )
    )

    private val toTableAddedWithColumnInfoDefaultValue = SchemaBundle(
        1,
        DatabaseBundle(
            1,
            "",
            mutableListOf(
                EntityBundle(
                    "Song",
                    "CREATE TABLE IF NOT EXISTS `Song` (`id` INTEGER NOT NULL, " +
                        "`title` TEXT NOT NULL, `length` INTEGER NOT NULL, PRIMARY KEY(`id`))",
                    listOf(
                        FieldBundle(
                            "id",
                            "id",
                            "INTEGER",
                            true,
                            "1"
                        ),
                        FieldBundle(
                            "title",
                            "title",
                            "TEXT",
                            true,
                            ""
                        ),
                        FieldBundle(
                            "length",
                            "length",
                            "INTEGER",
                            true,
                            "1"
                        )
                    ),
                    PrimaryKeyBundle(
                        false,
                        mutableListOf("id")
                    ),
                    mutableListOf(),
                    mutableListOf()
                ),
                EntityBundle(
                    "Artist",
                    "CREATE TABLE IF NOT EXISTS `Artist` (`id` INTEGER NOT NULL, " +
                        "`title` TEXT NOT NULL, `length` INTEGER NOT NULL, PRIMARY KEY(`id`))",
                    listOf(
                        FieldBundle(
                            "id",
                            "id",
                            "INTEGER",
                            true,
                            "1"
                        ),
                        FieldBundle(
                            "title",
                            "title",
                            "TEXT",
                            true,
                            ""
                        ),
                        FieldBundle(
                            "length",
                            "length",
                            "INTEGER",
                            true,
                            "1"
                        )
                    ),
                    PrimaryKeyBundle(
                        false,
                        mutableListOf("id")
                    ),
                    mutableListOf(),
                    mutableListOf()
                ),
                EntityBundle(
                    "Album",
                    "CREATE TABLE IF NOT EXISTS `Album` (`albumId` INTEGER NOT NULL, `name` TEXT " +
                        "NOT NULL, PRIMARY KEY(`albumId`))",
                    listOf(
                        FieldBundle(
                            "albumId",
                            "albumId",
                            "INTEGER",
                            true,
                            "1"
                        )
                    ),
                    PrimaryKeyBundle(true, listOf("albumId")),
                    listOf(),
                    listOf()
                )
            ),
            mutableListOf(),
            mutableListOf()
        )
    )

    private val toForeignKeyAdded = SchemaBundle(
        2,
        DatabaseBundle(
            2,
            "",
            listOf(
                EntityBundle(
                    "Song",
                    "CREATE TABLE IF NOT EXISTS `Song` (`id` INTEGER NOT NULL, " +
                        "`title` TEXT NOT NULL, `length` INTEGER NOT NULL, `artistId` " +
                        "INTEGER NOT NULL DEFAULT 0, PRIMARY KEY(`id`), FOREIGN KEY(`title`) " +
                        "REFERENCES `Song`(`artistId`) ON UPDATE NO ACTION ON DELETE NO " +
                        "ACTION DEFERRABLE INITIALLY DEFERRED))",
                    listOf(
                        FieldBundle(
                            "id",
                            "id",
                            "INTEGER",
                            true,
                            "1"
                        ),
                        FieldBundle(
                            "title",
                            "title",
                            "TEXT",
                            true,
                            ""
                        ),
                        FieldBundle(
                            "length",
                            "length",
                            "INTEGER",
                            true,
                            "1"
                        ),
                        FieldBundle(
                            "artistId",
                            "artistId",
                            "INTEGER",
                            true,
                            "0"
                        )
                    ),
                    PrimaryKeyBundle(
                        false,
                        mutableListOf("id")
                    ),
                    emptyList(),
                    listOf(
                        ForeignKeyBundle(
                            "Song",
                            "onDelete",
                            "onUpdate",
                            listOf("title"),
                            listOf("artistId")
                        )
                    )
                ),
                EntityBundle(
                    "Artist",
                    "CREATE TABLE IF NOT EXISTS `Artist` (`id` INTEGER NOT NULL, " +
                        "`title` TEXT NOT NULL, `length` INTEGER NOT NULL, PRIMARY KEY(`id`))",
                    listOf(
                        FieldBundle(
                            "id",
                            "id",
                            "INTEGER",
                            true,
                            "1"
                        ),
                        FieldBundle(
                            "title",
                            "title",
                            "TEXT",
                            true,
                            ""
                        ),
                        FieldBundle(
                            "length",
                            "length",
                            "INTEGER",
                            true,
                            "1"
                        )
                    ),
                    PrimaryKeyBundle(
                        false,
                        mutableListOf("id")
                    ),
                    mutableListOf(),
                    mutableListOf()
                )
            ),
            mutableListOf(),
            mutableListOf()
        )
    )

    val toIndexAdded = SchemaBundle(
        2,
        DatabaseBundle(
            2,
            "",
            mutableListOf(
                EntityBundle(
                    "Song",
                    "CREATE TABLE IF NOT EXISTS `Song` (`id` INTEGER NOT NULL, " +
                        "`title` TEXT NOT NULL, `length` INTEGER NOT NULL, PRIMARY KEY(`id`))",
                    listOf(
                        FieldBundle(
                            "id",
                            "id",
                            "INTEGER",
                            true,
                            "1"
                        ),
                        FieldBundle(
                            "title",
                            "title",
                            "TEXT",
                            true,
                            ""
                        ),
                        FieldBundle(
                            "length",
                            "length",
                            "INTEGER",
                            true,
                            "1"
                        )
                    ),
                    PrimaryKeyBundle(
                        false,
                        mutableListOf("id")
                    ),
                    listOf(
                        IndexBundle(
                            "index1",
                            true,
                            emptyList<String>(),
                            emptyList<String>(),
                            "CREATE UNIQUE INDEX IF NOT EXISTS `index1` ON `Song`" +
                                "(`title`)"
                        )
                    ),
                    mutableListOf()
                ),
                EntityBundle(
                    "Artist",
                    "CREATE TABLE IF NOT EXISTS `Artist` (`id` INTEGER NOT NULL, " +
                        "`title` TEXT NOT NULL, `length` INTEGER NOT NULL, PRIMARY KEY(`id`))",
                    listOf(
                        FieldBundle(
                            "id",
                            "id",
                            "INTEGER",
                            true,
                            "1"
                        ),
                        FieldBundle(
                            "title",
                            "title",
                            "TEXT",
                            true,
                            ""
                        ),
                        FieldBundle(
                            "length",
                            "length",
                            "INTEGER",
                            true,
                            "1"
                        )
                    ),
                    PrimaryKeyBundle(
                        false,
                        mutableListOf("id")
                    ),
                    mutableListOf(),
                    mutableListOf()
                )
            ),
            mutableListOf(),
            mutableListOf()
        )
    )

    val toChangeInPrimaryKey = SchemaBundle(
        2,
        DatabaseBundle(
            2,
            "",
            mutableListOf(
                EntityBundle(
                    "Song",
                    "CREATE TABLE IF NOT EXISTS `Song` (`id` INTEGER NOT NULL, " +
                        "`title` TEXT NOT NULL, `length` INTEGER NOT NULL, PRIMARY KEY(`title`))",
                    listOf(
                        FieldBundle(
                            "id",
                            "id",
                            "INTEGER",
                            true,
                            "1"
                        ),
                        FieldBundle(
                            "title",
                            "title",
                            "TEXT",
                            true,
                            ""
                        ),
                        FieldBundle(
                            "length",
                            "length",
                            "INTEGER",
                            true,
                            "1"
                        )
                    ),
                    PrimaryKeyBundle(
                        false,
                        mutableListOf("title")
                    ),
                    mutableListOf(),
                    mutableListOf()
                ),
                EntityBundle(
                    "Artist",
                    "CREATE TABLE IF NOT EXISTS `Artist` (`id` INTEGER NOT NULL, " +
                        "`title` TEXT NOT NULL, `length` INTEGER NOT NULL, PRIMARY KEY(`id`))",
                    listOf(
                        FieldBundle(
                            "id",
                            "id",
                            "INTEGER",
                            true,
                            "1"
                        ),
                        FieldBundle(
                            "title",
                            "title",
                            "TEXT",
                            true,
                            ""
                        ),
                        FieldBundle(
                            "length",
                            "length",
                            "INTEGER",
                            true,
                            "1"
                        )
                    ),
                    PrimaryKeyBundle(
                        false,
                        mutableListOf("id")
                    ),
                    mutableListOf(),
                    mutableListOf()
                )
            ),
            mutableListOf(),
            mutableListOf()
        )
    )

    //endregion

    //region Invalid "to" Schemas (These are expected to throw an error.)

    /**
     * The length column is removed from the first version. No other changes made.
     */
    private val toColumnRemoved = SchemaBundle(
        2,
        DatabaseBundle(
            2,
            "",
            listOf(
                EntityBundle(
                    "Song",
                    "CREATE TABLE IF NOT EXISTS `Song` (`id` INTEGER NOT NULL, " +
                        "`title` TEXT NOT NULL, PRIMARY KEY(`id`))",
                    listOf(
                        FieldBundle(
                            "id",
                            "id",
                            "INTEGER",
                            true,
                            "1"
                        ),
                        FieldBundle(
                            "title",
                            "title",
                            "TEXT",
                            true,
                            ""
                        ),
                    ),
                    PrimaryKeyBundle(
                        false,
                        mutableListOf("id")
                    ),
                    emptyList(),
                    emptyList()
                ),
                EntityBundle(
                    "Artist",
                    "CREATE TABLE IF NOT EXISTS `Artist` (`id` INTEGER NOT NULL, " +
                        "`title` TEXT NOT NULL, `length` INTEGER NOT NULL, PRIMARY KEY(`id`))",
                    listOf(
                        FieldBundle(
                            "id",
                            "id",
                            "INTEGER",
                            true,
                            "1"
                        ),
                        FieldBundle(
                            "title",
                            "title",
                            "TEXT",
                            true,
                            ""
                        ),
                        FieldBundle(
                            "length",
                            "length",
                            "INTEGER",
                            true,
                            "1"
                        )
                    ),
                    PrimaryKeyBundle(
                        false,
                        mutableListOf("id")
                    ),
                    mutableListOf(),
                    mutableListOf()
                )
            ),
            mutableListOf(),
            mutableListOf()
        )
    )

    /**
     * If the user declared the default value in the SQL statement and not used a @ColumnInfo,
     * Room will put null for that default value in the exported schema. In this case we
     * can't migrate.
     */
    private val toColumnAddedWithNoDefaultValue = SchemaBundle(
        2,
        DatabaseBundle(
            2,
            "",
            listOf(
                EntityBundle(
                    "Song",
                    "CREATE TABLE IF NOT EXISTS `Song` (`id` INTEGER NOT NULL, " +
                        "`title` TEXT NOT NULL, `length` INTEGER NOT NULL, `artistId` " +
                        "INTEGER NOT NULL, PRIMARY KEY(`id`))",
                    listOf(
                        FieldBundle(
                            "id",
                            "id",
                            "INTEGER",
                            true,
                            "1"
                        ),
                        FieldBundle(
                            "title",
                            "title",
                            "TEXT",
                            true,
                            ""
                        ),
                        FieldBundle(
                            "length",
                            "length",
                            "INTEGER",
                            true,
                            "1"
                        ),
                        FieldBundle(
                            "artistId",
                            "artistId",
                            "INTEGER",
                            true,
                            null
                        )
                    ),
                    PrimaryKeyBundle(
                        false,
                        mutableListOf("id")
                    ),
                    emptyList(),
                    emptyList()
                ),
                EntityBundle(
                    "Artist",
                    "CREATE TABLE IF NOT EXISTS `Artist` (`id` INTEGER NOT NULL, " +
                        "`title` TEXT NOT NULL, `length` INTEGER NOT NULL, PRIMARY KEY(`id`))",
                    listOf(
                        FieldBundle(
                            "id",
                            "id",
                            "INTEGER",
                            true,
                            "1"
                        ),
                        FieldBundle(
                            "title",
                            "title",
                            "TEXT",
                            true,
                            ""
                        ),
                        FieldBundle(
                            "length",
                            "length",
                            "INTEGER",
                            true,
                            "1"
                        )
                    ),
                    PrimaryKeyBundle(
                        false,
                        mutableListOf("id")
                    ),
                    mutableListOf(),
                    mutableListOf()
                )
            ),
            mutableListOf(),
            mutableListOf()
        )
    )

    //endregion
}
