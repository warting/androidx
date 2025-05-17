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

package androidx.room.solver.prepared.binderprovider

import androidx.room.compiler.processing.XRawType
import androidx.room.compiler.processing.XType
import androidx.room.ext.KotlinTypeNames
import androidx.room.parser.ParsedQuery
import androidx.room.processor.Context
import androidx.room.solver.RxType
import androidx.room.solver.prepared.binder.LambdaPreparedQueryResultBinder
import androidx.room.solver.prepared.binder.PreparedQueryResultBinder

open class RxPreparedQueryResultBinderProvider
internal constructor(val context: Context, private val rxType: RxType) :
    PreparedQueryResultBinderProvider {

    private val hasRxJavaArtifact by lazy {
        context.processingEnv.findTypeElement(rxType.version.rxMarkerClassName.canonicalName) !=
            null
    }

    override fun matches(declared: XType): Boolean =
        declared.typeArguments.size == 1 && matchesRxType(declared)

    private fun matchesRxType(declared: XType): Boolean {
        return declared.rawType.asTypeName() == rxType.className
    }

    override fun provide(declared: XType, query: ParsedQuery): PreparedQueryResultBinder {
        if (!hasRxJavaArtifact) {
            context.logger.e(rxType.version.missingArtifactMessage)
        }
        val typeArg = extractTypeArg(declared)
        return LambdaPreparedQueryResultBinder(
            returnType = typeArg,
            functionName = rxType.factoryMethodName,
            adapter = context.typeAdapterStore.findPreparedQueryResultAdapter(typeArg, query),
        )
    }

    open fun extractTypeArg(declared: XType): XType = declared.typeArguments.first()

    companion object {
        fun getAll(context: Context) =
            listOf(
                RxSingleOrMaybePreparedQueryResultBinderProvider(context, RxType.RX2_SINGLE),
                RxSingleOrMaybePreparedQueryResultBinderProvider(context, RxType.RX2_MAYBE),
                RxCompletablePreparedQueryResultBinderProvider(context, RxType.RX2_COMPLETABLE),
                RxSingleOrMaybePreparedQueryResultBinderProvider(context, RxType.RX3_SINGLE),
                RxSingleOrMaybePreparedQueryResultBinderProvider(context, RxType.RX3_MAYBE),
                RxCompletablePreparedQueryResultBinderProvider(context, RxType.RX3_COMPLETABLE),
            )
    }
}

private class RxCompletablePreparedQueryResultBinderProvider(context: Context, rxType: RxType) :
    RxPreparedQueryResultBinderProvider(context, rxType) {

    private val completableType: XRawType? by lazy {
        context.processingEnv.findType(rxType.className.canonicalName)?.rawType
    }

    override fun matches(declared: XType): Boolean {
        if (completableType == null) {
            return false
        }
        return declared.rawType.isAssignableFrom(completableType!!)
    }

    /**
     * Since Completable has no type argument, the supported return type is Unit (non-nullable)
     * since the 'createCompletable" factory function take a Kotlin lambda.
     */
    override fun extractTypeArg(declared: XType): XType =
        context.processingEnv.requireType(KotlinTypeNames.UNIT)
}

private class RxSingleOrMaybePreparedQueryResultBinderProvider(context: Context, rxType: RxType) :
    RxPreparedQueryResultBinderProvider(context, rxType) {

    /** Since Maybe can have null values, the lambda returned must allow for null values. */
    override fun extractTypeArg(declared: XType): XType =
        declared.typeArguments.first().makeNullable()
}
