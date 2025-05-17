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

package androidx.room.solver.query.result

import androidx.room.compiler.codegen.CodeLanguage
import androidx.room.compiler.codegen.XCodeBlock
import androidx.room.compiler.codegen.XCodeBlock.Builder.Companion.applyTo
import androidx.room.compiler.codegen.XPropertySpec
import androidx.room.compiler.codegen.XTypeName
import androidx.room.compiler.processing.XType
import androidx.room.ext.ArrayLiteral
import androidx.room.ext.CommonTypeNames
import androidx.room.ext.InvokeWithLambdaParameter
import androidx.room.ext.LambdaSpec
import androidx.room.ext.SQLiteDriverTypeNames
import androidx.room.solver.CodeGenScope
import androidx.room.solver.RxType

/** Binds the result as an RxJava2 Flowable, Publisher and Observable. */
internal class RxQueryResultBinder(
    private val rxType: RxType,
    val typeArg: XType,
    val queryTableNames: Set<String>,
    adapter: QueryResultAdapter?,
) : BaseObservableQueryResultBinder(adapter) {

    override fun convertAndReturn(
        sqlQueryVar: String,
        dbProperty: XPropertySpec,
        bindStatement: (CodeGenScope.(String) -> Unit)?,
        returnTypeName: XTypeName,
        inTransaction: Boolean,
        scope: CodeGenScope,
    ) {
        val connectionVar = scope.getTmpVar("_connection")
        val performBlock =
            InvokeWithLambdaParameter(
                scope = scope,
                functionName = rxType.factoryMethodName,
                argFormat = listOf("%N", "%L", "%L"),
                args =
                    listOf(
                        dbProperty,
                        inTransaction,
                        ArrayLiteral(CommonTypeNames.STRING, *queryTableNames.toTypedArray()),
                    ),
                lambdaSpec =
                    object :
                        LambdaSpec(
                            parameterTypeName = SQLiteDriverTypeNames.CONNECTION,
                            parameterName = connectionVar,
                            returnTypeName = typeArg.asTypeName(),
                            javaLambdaSyntaxAvailable = scope.javaLambdaSyntaxAvailable,
                        ) {
                        override fun XCodeBlock.Builder.body(scope: CodeGenScope) {
                            val statementVar = scope.getTmpVar("_stmt")
                            addLocalVal(
                                statementVar,
                                SQLiteDriverTypeNames.STATEMENT,
                                "%L.prepare(%L)",
                                connectionVar,
                                sqlQueryVar,
                            )
                            beginControlFlow("try")
                            bindStatement?.invoke(scope, statementVar)
                            val outVar = scope.getTmpVar("_result")
                            adapter?.convert(outVar, statementVar, scope)
                            applyTo { language ->
                                when (language) {
                                    CodeLanguage.JAVA -> addStatement("return %L", outVar)
                                    CodeLanguage.KOTLIN -> addStatement("%L", outVar)
                                }
                            }
                            nextControlFlow("finally")
                            addStatement("%L.close()", statementVar)
                            endControlFlow()
                        }
                    },
            )
        scope.builder.add("return %L", performBlock)
    }
}
