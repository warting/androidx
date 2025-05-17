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

package androidx.room.solver.shortcut.result

import androidx.room.compiler.codegen.CodeLanguage
import androidx.room.compiler.codegen.XCodeBlock
import androidx.room.compiler.codegen.XPropertySpec
import androidx.room.compiler.codegen.XTypeName
import androidx.room.compiler.processing.XType
import androidx.room.compiler.processing.isInt
import androidx.room.compiler.processing.isKotlinUnit
import androidx.room.compiler.processing.isVoid
import androidx.room.compiler.processing.isVoidObject
import androidx.room.ext.KotlinTypeNames
import androidx.room.ext.isNotKotlinUnit
import androidx.room.ext.isNotVoid
import androidx.room.ext.isNotVoidObject
import androidx.room.solver.CodeGenScope
import androidx.room.vo.ShortcutQueryParameter

/** Class that knows how to generate a delete or update function body. */
class DeleteOrUpdateFunctionAdapter private constructor(val returnType: XType) {
    companion object {
        fun create(returnType: XType): DeleteOrUpdateFunctionAdapter? {
            if (isDeleteOrUpdateValid(returnType)) {
                return DeleteOrUpdateFunctionAdapter(returnType)
            }
            return null
        }

        private fun isDeleteOrUpdateValid(returnType: XType): Boolean {
            return returnType.isVoid() ||
                returnType.isInt() ||
                returnType.isVoidObject() ||
                returnType.isKotlinUnit()
        }
    }

    fun generateFunctionBody(
        scope: CodeGenScope,
        parameters: List<ShortcutQueryParameter>,
        adapters: Map<String, Pair<XPropertySpec, Any>>,
        connectionVar: String,
    ) {
        scope.builder.apply {
            val hasReturnValue =
                returnType.isNotVoid() &&
                    returnType.isNotVoidObject() &&
                    returnType.isNotKotlinUnit()
            val resultVar =
                if (hasReturnValue) {
                    scope.getTmpVar("_result")
                } else {
                    null
                }
            if (resultVar != null) {
                addLocalVariable(
                    name = resultVar,
                    typeName = XTypeName.PRIMITIVE_INT,
                    isMutable = true,
                    assignExpr = XCodeBlock.of("0"),
                )
            }
            parameters.forEach { param ->
                val adapter = adapters.getValue(param.name).first
                addStatement(
                    "%L%L.%L(%L, %L)",
                    if (resultVar == null) "" else "$resultVar += ",
                    adapter.name,
                    param.handleFunctionName,
                    connectionVar,
                    param.name,
                )
            }
            when (scope.language) {
                CodeLanguage.KOTLIN ->
                    if (resultVar != null) {
                        addStatement("%L", resultVar)
                    } else if (returnType.isVoidObject()) {
                        addStatement("null")
                    }
                CodeLanguage.JAVA ->
                    if (resultVar != null) {
                        addStatement("return %L", resultVar)
                    } else if (returnType.isVoidObject() || returnType.isVoid()) {
                        addStatement("return null")
                    } else {
                        addStatement("return %T.INSTANCE", KotlinTypeNames.UNIT)
                    }
            }
        }
    }
}
