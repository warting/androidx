/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.room.compiler.processing.ksp

import androidx.room.compiler.processing.tryUnbox
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSType
import com.squareup.kotlinpoet.javapoet.JTypeName
import com.squareup.kotlinpoet.javapoet.KTypeName

/**
 * This tries to mimic primitive types in Kotlin.
 *
 * Primitiveness of a type cannot always be driven from itself (e.g. its nullability). For instance,
 * a kotlin.Int might be non-null but still be non primitive if it is derived from a generic type
 * argument or is part of type parameters.
 */
internal class KspPrimitiveType(
    env: KspProcessingEnv,
    ksType: KSType,
    originalKSAnnotations: Sequence<KSAnnotation> = ksType.annotations,
    typeAlias: KSType? = null,
) : KspType(env, ksType, originalKSAnnotations, null, typeAlias) {
    override fun resolveJTypeName(): JTypeName {
        return ksType.asJTypeName(env.resolver).tryUnbox()
    }

    override fun resolveKTypeName(): KTypeName {
        return ksType.asKTypeName(env.resolver)
    }

    override fun boxed(): KspType {
        return env.wrap(ksType = ksType, allowPrimitives = false)
    }

    override fun copy(
        env: KspProcessingEnv,
        ksType: KSType,
        originalKSAnnotations: Sequence<KSAnnotation>,
        scope: KSTypeVarianceResolverScope?,
        typeAlias: KSType?,
    ) = KspPrimitiveType(env, ksType, originalKSAnnotations, typeAlias)
}
