/*
 * Copyright (C) 2016 The Android Open Source Project
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

package androidx.room.solver.query.parameter

import androidx.room.compiler.codegen.XCodeBlock
import androidx.room.compiler.codegen.XTypeName
import androidx.room.compiler.processing.XNullability
import androidx.room.ext.ArraySizeExprCode
import androidx.room.solver.CodeGenScope
import androidx.room.solver.types.StatementValueBinder

/** Binds ARRAY(T) (e.g. int[]) into String[] args of a query. */
class ArrayQueryParameterAdapter(
    private val bindAdapter: StatementValueBinder,
    private val nullability: XNullability,
) : QueryParameterAdapter(true) {
    override fun bindToStmt(
        inputVarName: String,
        stmtVarName: String,
        startIndexVarName: String,
        scope: CodeGenScope,
    ) {
        scope.builder.apply {
            fun XCodeBlock.Builder.addForEachBindCode() {
                val itrVar = scope.getTmpVar("_item")
                beginForEachControlFlow(
                        itemVarName = itrVar,
                        typeName = bindAdapter.typeMirror().asTypeName(),
                        iteratorVarName = inputVarName,
                    )
                    .apply {
                        bindAdapter.bindToStmt(stmtVarName, startIndexVarName, itrVar, scope)
                        addStatement("%L++", startIndexVarName)
                    }
                endControlFlow()
            }
            if (nullability == XNullability.NONNULL) {
                addForEachBindCode()
            } else {
                beginControlFlow("if (%L == null)", inputVarName)
                    .addStatement("%L.bindNull(%L)", stmtVarName, startIndexVarName)
                nextControlFlow("else")
                addForEachBindCode()
                endControlFlow()
            }
        }
    }

    override fun getArgCount(inputVarName: String, outputVarName: String, scope: CodeGenScope) {
        val sizeExpr = ArraySizeExprCode(inputVarName)
        val countAssignment =
            if (nullability == XNullability.NONNULL) {
                sizeExpr
            } else {
                XCodeBlock.ofTernaryIf(
                    condition = XCodeBlock.of("%L == null", inputVarName),
                    leftExpr = XCodeBlock.of("1"),
                    rightExpr = XCodeBlock.of("%L", sizeExpr),
                )
            }
        scope.builder.addLocalVariable(
            name = outputVarName,
            typeName = XTypeName.PRIMITIVE_INT,
            assignExpr = countAssignment,
        )
    }
}
