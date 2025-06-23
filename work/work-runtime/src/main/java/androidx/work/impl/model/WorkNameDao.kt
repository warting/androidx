/*
 * Copyright 2018 The Android Open Source Project
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
package androidx.work.impl.model

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

/** The Data Access Object for [WorkName]s. */
@Dao
public interface WorkNameDao {
    /**
     * Inserts a [WorkName] into the table.
     *
     * @param workName The [WorkName] to insert
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE) public fun insert(workName: WorkName)

    /**
     * Retrieves all [WorkSpec] ids in the given named graph.
     *
     * @param name The matching name
     * @return All [WorkSpec] ids in the given named graph
     */
    @Query("SELECT work_spec_id FROM workname WHERE name=:name")
    public fun getWorkSpecIdsWithName(name: String): List<String>

    /**
     * @param workSpecId The [WorkSpec] id
     * @return All the names associated to the [WorkSpec] id
     */
    @Query("SELECT name FROM workname WHERE work_spec_id=:workSpecId")
    public fun getNamesForWorkSpecId(workSpecId: String): List<String>
}
