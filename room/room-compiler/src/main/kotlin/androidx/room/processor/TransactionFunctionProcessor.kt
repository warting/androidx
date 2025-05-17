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

package androidx.room.processor

import androidx.room.compiler.codegen.CodeLanguage
import androidx.room.compiler.processing.XMethodElement
import androidx.room.compiler.processing.XType
import androidx.room.compiler.processing.XTypeElement
import androidx.room.ext.DEFERRED_TYPES
import androidx.room.vo.TransactionFunction

class TransactionFunctionProcessor(
    baseContext: Context,
    val containingElement: XTypeElement,
    val containingType: XType,
    val executableElement: XMethodElement,
) {

    val context = baseContext.fork(executableElement)

    fun process(): TransactionFunction {
        val delegate =
            FunctionProcessorDelegate.createFor(context, containingType, executableElement)
        val hasKotlinDefaultImpl = executableElement.hasKotlinDefaultImpl()
        context.checker.check(
            executableElement.isOverrideableIgnoringContainer() &&
                (!executableElement.isAbstract() || hasKotlinDefaultImpl),
            executableElement,
            ProcessorErrors.TRANSACTION_FUNCTION_MODIFIERS,
        )

        val returnType = delegate.extractReturnType()
        val rawReturnType = returnType.rawType

        val deferredReturnTypeName =
            DEFERRED_TYPES.firstOrNull { className ->
                context.processingEnv
                    .findType(className.canonicalName)
                    ?.rawType
                    ?.isAssignableFrom(rawReturnType) ?: false
            }
        if (deferredReturnTypeName != null) {
            context.logger.e(
                ProcessorErrors.transactionFunctionAsync(
                    deferredReturnTypeName.toString(context.codeLanguage)
                ),
                executableElement,
            )
        }

        val isSuspendFunction = delegate.executableElement.isSuspendFunction()
        if (
            !isSuspendFunction && deferredReturnTypeName == null && !context.isAndroidOnlyTarget()
        ) {
            // A blocking transaction wrapper function is not allowed if the target platforms
            // include non-Android targets.
            context.logger.e(
                executableElement,
                ProcessorErrors.INVALID_BLOCKING_DAO_FUNCTION_NON_ANDROID,
            )
        }

        val callType =
            when {
                containingElement.isInterface() && executableElement.isJavaDefault() ->
                    TransactionFunction.CallType.DEFAULT_JAVA8
                containingElement.isInterface() && hasKotlinDefaultImpl ->
                    TransactionFunction.CallType.DEFAULT_KOTLIN
                else -> TransactionFunction.CallType.CONCRETE
            }

        val parameters = delegate.extractParams()
        val processedParamNames =
            parameters.map { param ->
                // Apply spread operator when delegating to a vararg parameter in Kotlin.
                if (context.codeLanguage == CodeLanguage.KOTLIN && param.isVarArgs()) {
                    "*${param.name}"
                } else {
                    param.name
                }
            }

        return TransactionFunction(
            element = executableElement,
            returnType = returnType,
            parameterNames = processedParamNames,
            callType = callType,
            functionBinder = delegate.findTransactionFunctionBinder(callType),
        )
    }
}
