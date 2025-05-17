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

package androidx.room.solver.shortcut.binder

import androidx.room.compiler.codegen.XCodeBlock
import androidx.room.compiler.codegen.XPropertySpec
import androidx.room.compiler.codegen.XTypeSpec
import androidx.room.compiler.codegen.box
import androidx.room.compiler.processing.XType
import androidx.room.ext.InvokeWithLambdaParameter
import androidx.room.ext.LambdaSpec
import androidx.room.ext.RoomMemberNames.DB_UTIL_PERFORM_SUSPENDING
import androidx.room.ext.SQLiteDriverTypeNames
import androidx.room.solver.CodeGenScope
import androidx.room.solver.shortcut.result.DeleteOrUpdateFunctionAdapter
import androidx.room.vo.ShortcutQueryParameter

/** Binder for suspending delete and update methods. */
class CoroutineDeleteOrUpdateFunctionBinder(
    val typeArg: XType,
    adapter: DeleteOrUpdateFunctionAdapter?,
    private val continuationParamName: String,
) : DeleteOrUpdateFunctionBinder(adapter) {

    override fun convertAndReturn(
        parameters: List<ShortcutQueryParameter>,
        adapters: Map<String, Pair<XPropertySpec, XTypeSpec>>,
        dbProperty: XPropertySpec,
        scope: CodeGenScope,
    ) {
        if (adapter == null) {
            return
        }
        val connectionVar = scope.getTmpVar("_connection")
        val performBlock =
            InvokeWithLambdaParameter(
                scope = scope,
                functionName = DB_UTIL_PERFORM_SUSPENDING,
                argFormat = listOf("%N", "%L", "%L"),
                args = listOf(dbProperty, /* isReadOnly= */ false, /* inTransaction= */ true),
                continuationParamName = continuationParamName,
                lambdaSpec =
                    object :
                        LambdaSpec(
                            parameterTypeName = SQLiteDriverTypeNames.CONNECTION,
                            parameterName = connectionVar,
                            returnTypeName = adapter.returnType.asTypeName().box(),
                            javaLambdaSyntaxAvailable = scope.javaLambdaSyntaxAvailable,
                        ) {
                        override fun XCodeBlock.Builder.body(scope: CodeGenScope) {
                            adapter.generateFunctionBody(
                                scope = scope,
                                parameters = parameters,
                                adapters = adapters,
                                connectionVar = connectionVar,
                            )
                        }
                    },
            )
        scope.builder.add("return %L", performBlock)
    }
}
