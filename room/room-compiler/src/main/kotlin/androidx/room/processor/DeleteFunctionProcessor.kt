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
package androidx.room.processor

import androidx.room.Delete
import androidx.room.compiler.processing.XMethodElement
import androidx.room.compiler.processing.XType
import androidx.room.vo.DeleteFunction

class DeleteFunctionProcessor(
    baseContext: Context,
    val containing: XType,
    val executableElement: XMethodElement
) {
    val context = baseContext.fork(executableElement)

    fun process(): DeleteFunction {
        val delegate = ShortcutFunctionProcessor(context, containing, executableElement)
        val annotation =
            delegate.extractAnnotation(Delete::class, ProcessorErrors.MISSING_DELETE_ANNOTATION)

        val returnType = delegate.extractReturnType()

        val functionBinder = delegate.findDeleteOrUpdateFunctionBinder(returnType)

        context.checker.check(
            functionBinder.adapter != null,
            executableElement,
            ProcessorErrors.CANNOT_FIND_DELETE_RESULT_ADAPTER
        )

        val (entities, params) =
            delegate.extractParams(
                targetEntityType = annotation?.get("entity")?.asType(),
                missingParamError = ProcessorErrors.DELETE_MISSING_PARAMS,
                onValidatePartialEntity = { _, _ -> }
            )

        return DeleteFunction(
            element = delegate.executableElement,
            entities = entities,
            parameters = params,
            functionBinder = functionBinder
        )
    }
}
