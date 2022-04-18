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

package androidx.room.solver.query.result

import androidx.room.ext.L
import androidx.room.ext.RoomTypeNames
import androidx.room.ext.S
import androidx.room.ext.T
import androidx.room.ext.capitalize
import androidx.room.ext.stripNonJava
import androidx.room.parser.ParsedQuery
import androidx.room.processor.ProcessorErrors
import androidx.room.solver.CodeGenScope
import androidx.room.verifier.QueryResultInfo
import androidx.room.vo.ColumnIndexVar
import com.squareup.javapoet.TypeName
import java.util.Locale

/**
 * Creates the index variables to retrieve columns from a cursor for a [PojoRowAdapter].
 */
class PojoIndexAdapter(
    private val mapping: PojoRowAdapter.PojoMapping,
    private val info: QueryResultInfo?,
    private val query: ParsedQuery?,
) : IndexAdapter {

    private lateinit var columnIndexVars: List<ColumnIndexVar>

    override fun onCursorReady(cursorVarName: String, scope: CodeGenScope) {
        columnIndexVars = mapping.matchedFields.map {
            val indexVar = scope.getTmpVar(
                "_cursorIndexOf${it.name.stripNonJava().capitalize(Locale.US)}"
            )
            if (info != null && query != null && query.hasTopStarProjection == false) {
                // When result info is available and query does not have a top-level star
                // projection we can generate column to field index since the column result order
                // is deterministic.
                val infoIndex = info.columns.indexOfFirst { columnInfo ->
                    columnInfo.name == it.columnName
                }
                check(infoIndex != -1) {
                    "Result column index not found for field '$it' with column name " +
                        "'${it.columnName}'. Query: ${query.original}. Please file a bug at " +
                        ProcessorErrors.ISSUE_TRACKER_LINK
                }
                scope.builder().addStatement(
                    "final $T $L = $L",
                    TypeName.INT, indexVar, infoIndex
                )
            } else {
                val indexMethod = if (info == null) {
                    "getColumnIndex"
                } else {
                    "getColumnIndexOrThrow"
                }
                scope.builder().addStatement(
                    "final $T $L = $T.$L($L, $S)",
                    TypeName.INT, indexVar, RoomTypeNames.CURSOR_UTIL, indexMethod, cursorVarName,
                    it.columnName
                )
            }
            ColumnIndexVar(it.columnName, indexVar)
        }
    }

    override fun getIndexVars() = columnIndexVars
}