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

package androidx.room.solver.query.result

import androidx.room.compiler.codegen.XCodeBlock
import androidx.room.compiler.processing.XType
import androidx.room.ext.CommonTypeNames
import androidx.room.solver.CodeGenScope

/**
 * Wraps a row adapter when there is only 1 item in the result, and the result's outer type is
 * {@link java.util.Optional}.
 *
 * <p>n.b. this will only be useful if the project uses Java 8.
 */
class OptionalQueryResultAdapter(
    private val typeArg: XType,
    private val resultAdapter: SingleItemQueryResultAdapter,
) : QueryResultAdapter(resultAdapter.rowAdapters) {
    override fun convert(outVarName: String, stmtVarName: String, scope: CodeGenScope) {
        scope.builder.apply {
            val valueVarName = scope.getTmpVar("_value")
            resultAdapter.convert(valueVarName, stmtVarName, scope)
            addLocalVariable(
                name = outVarName,
                typeName = CommonTypeNames.OPTIONAL.parametrizedBy(typeArg.asTypeName()),
                assignExpr =
                    XCodeBlock.of(
                        format = "%T.ofNullable(%L)",
                        CommonTypeNames.OPTIONAL,
                        valueVarName,
                    ),
            )
        }
    }
}
