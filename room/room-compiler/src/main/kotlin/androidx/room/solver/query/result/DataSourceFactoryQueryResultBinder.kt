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

import androidx.room.compiler.codegen.VisibilityModifier
import androidx.room.compiler.codegen.XFunSpec
import androidx.room.compiler.codegen.XPropertySpec
import androidx.room.compiler.codegen.XTypeName
import androidx.room.compiler.codegen.XTypeSpec
import androidx.room.ext.PagingTypeNames
import androidx.room.solver.CodeGenScope

class DataSourceFactoryQueryResultBinder(
    val positionalDataSourceQueryResultBinder: PositionalDataSourceQueryResultBinder
) : QueryResultBinder(positionalDataSourceQueryResultBinder.listAdapter) {

    val typeName: XTypeName = positionalDataSourceQueryResultBinder.itemTypeName

    override val usesCompatQueryWriter = true

    override fun convertAndReturn(
        sqlQueryVar: String,
        dbProperty: XPropertySpec,
        bindStatement: (CodeGenScope.(String) -> Unit)?,
        returnTypeName: XTypeName,
        inTransaction: Boolean,
        scope: CodeGenScope,
    ) {
        scope.builder.apply {
            val pagedListProvider =
                XTypeSpec.anonymousClassBuilder()
                    .apply {
                        superclass(
                            PagingTypeNames.DATA_SOURCE_FACTORY.parametrizedBy(
                                XTypeName.BOXED_INT,
                                typeName,
                            )
                        )
                        addCreateMethod(
                            roomSQLiteQueryVar = sqlQueryVar,
                            dbProperty = dbProperty,
                            bindStatement = bindStatement,
                            returnTypeName = returnTypeName,
                            inTransaction = inTransaction,
                            scope = scope,
                        )
                    }
                    .build()
            addStatement("return %L", pagedListProvider)
        }
    }

    private fun XTypeSpec.Builder.addCreateMethod(
        roomSQLiteQueryVar: String,
        dbProperty: XPropertySpec,
        inTransaction: Boolean,
        scope: CodeGenScope,
        bindStatement: (CodeGenScope.(String) -> Unit)?,
        returnTypeName: XTypeName,
    ) {
        addFunction(
            XFunSpec.builder(
                    name = "create",
                    visibility = VisibilityModifier.PUBLIC,
                    isOverride = true,
                )
                .apply {
                    returns(positionalDataSourceQueryResultBinder.typeName)
                    val countedBinderScope = scope.fork()
                    positionalDataSourceQueryResultBinder.convertAndReturn(
                        sqlQueryVar = roomSQLiteQueryVar,
                        dbProperty = dbProperty,
                        bindStatement = bindStatement,
                        returnTypeName = returnTypeName,
                        inTransaction = inTransaction,
                        scope = countedBinderScope,
                    )
                    addCode(countedBinderScope.generate())
                }
                .build()
        )
    }
}
