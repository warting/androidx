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

package androidx.room.solver.shortcut.binderprovider

import androidx.room.compiler.processing.XRawType
import androidx.room.compiler.processing.XType
import androidx.room.ext.KotlinTypeNames
import androidx.room.processor.Context
import androidx.room.solver.RxType
import androidx.room.solver.shortcut.binder.DeleteOrUpdateFunctionBinder
import androidx.room.solver.shortcut.binder.LambdaDeleteOrUpdateFunctionBinder

/** Provider for Rx Callable binders. */
open class RxCallableDeleteOrUpdateFunctionBinderProvider
internal constructor(val context: Context, private val rxType: RxType) :
    DeleteOrUpdateFunctionBinderProvider {

    /**
     * [Single] and [Maybe] are generics but [Completable] is not so each implementation of this
     * class needs to define how to extract the type argument.
     */
    open fun extractTypeArg(declared: XType): XType = declared.typeArguments.first()

    override fun matches(declared: XType): Boolean =
        declared.typeArguments.size == 1 && matchesRxType(declared)

    private fun matchesRxType(declared: XType): Boolean {
        return declared.rawType.asTypeName() == rxType.className
    }

    override fun provide(declared: XType): DeleteOrUpdateFunctionBinder {
        val typeArg = extractTypeArg(declared)
        val adapter = context.typeAdapterStore.findDeleteOrUpdateAdapter(typeArg)
        return LambdaDeleteOrUpdateFunctionBinder(
            typeArg = typeArg,
            functionName = rxType.factoryMethodName,
            adapter = adapter,
        )
    }

    companion object {
        fun getAll(context: Context) =
            listOf(
                RxSingleOrMaybeDeleteOrUpdateFunctionBinderProvider(context, RxType.RX2_SINGLE),
                RxSingleOrMaybeDeleteOrUpdateFunctionBinderProvider(context, RxType.RX2_MAYBE),
                RxCompletableDeleteOrUpdateFunctionBinderProvider(context, RxType.RX2_COMPLETABLE),
                RxSingleOrMaybeDeleteOrUpdateFunctionBinderProvider(context, RxType.RX3_SINGLE),
                RxSingleOrMaybeDeleteOrUpdateFunctionBinderProvider(context, RxType.RX3_MAYBE),
                RxCompletableDeleteOrUpdateFunctionBinderProvider(context, RxType.RX3_COMPLETABLE),
            )
    }
}

private class RxCompletableDeleteOrUpdateFunctionBinderProvider(context: Context, rxType: RxType) :
    RxCallableDeleteOrUpdateFunctionBinderProvider(context, rxType) {

    private val completableType: XRawType? by lazy {
        context.processingEnv.findType(rxType.className.canonicalName)?.rawType
    }

    /**
     * Since Completable has no type argument, the supported return type is Unit (non-nullable)
     * since the 'createCompletable" factory method take a Kotlin lambda.
     */
    override fun extractTypeArg(declared: XType): XType =
        context.processingEnv.requireType(KotlinTypeNames.UNIT)

    override fun matches(declared: XType): Boolean = isCompletable(declared)

    private fun isCompletable(declared: XType): Boolean {
        val completableType = this.completableType ?: return false
        return declared.rawType.isAssignableFrom(completableType)
    }
}

private class RxSingleOrMaybeDeleteOrUpdateFunctionBinderProvider(
    context: Context,
    rxType: RxType,
) : RxCallableDeleteOrUpdateFunctionBinderProvider(context, rxType) {

    /** Since Maybe can have null values, the lambda returned must allow for null values. */
    override fun extractTypeArg(declared: XType): XType =
        declared.typeArguments.first().makeNullable()
}
