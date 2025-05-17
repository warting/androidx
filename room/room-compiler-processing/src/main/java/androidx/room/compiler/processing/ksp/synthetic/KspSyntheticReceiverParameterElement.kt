/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.room.compiler.processing.ksp.synthetic

import androidx.room.compiler.processing.XAnnotated
import androidx.room.compiler.processing.XEquality
import androidx.room.compiler.processing.XExecutableParameterElement
import androidx.room.compiler.processing.XMemberContainer
import androidx.room.compiler.processing.XType
import androidx.room.compiler.processing.ksp.KSTypeVarianceResolverScope
import androidx.room.compiler.processing.ksp.KspAnnotated
import androidx.room.compiler.processing.ksp.KspMethodElement
import androidx.room.compiler.processing.ksp.KspProcessingEnv
import androidx.room.compiler.processing.ksp.KspType
import com.google.devtools.ksp.symbol.KSTypeReference

internal class KspSyntheticReceiverParameterElement(
    val env: KspProcessingEnv,
    override val enclosingElement: KspMethodElement,
    val receiverType: KSTypeReference,
) :
    XExecutableParameterElement,
    XEquality,
    XAnnotated by KspAnnotated.create(
        env = env,
        delegate = receiverType, // for @receiver use-site annotations
        filter = KspAnnotated.UseSiteFilter.NO_USE_SITE_OR_RECEIVER,
    ) {

    override fun isContinuationParam() = false

    override fun isReceiverParam() = true

    override fun isKotlinPropertyParam() = false

    override fun isVarArgs() = false

    override val name: String by lazy {
        // KAPT uses `$this$<functionName>`
        "$" + "this" + "$" + enclosingElement.name
    }

    override val jvmName = name

    override val equalityItems: Array<out Any?> by lazy { arrayOf(enclosingElement, receiverType) }

    override val hasDefaultValue: Boolean
        get() = false

    override val type: KspType by lazy { createAsMemberOf(closestMemberContainer.type) }

    override val fallbackLocationText: String
        get() = "receiver parameter of ${enclosingElement.fallbackLocationText}"

    // Not applicable
    override val docComment: String?
        get() = null

    override val closestMemberContainer: XMemberContainer by lazy {
        enclosingElement.closestMemberContainer
    }

    override fun asMemberOf(other: XType): KspType {
        return if (closestMemberContainer.type?.isSameType(other) != false) {
            type
        } else {
            createAsMemberOf(other)
        }
    }

    private fun createAsMemberOf(container: XType?): KspType {
        check(container is KspType?)
        val asMemberReceiverType =
            receiverType.resolve().let {
                if (container?.ksType == null || it.isError) {
                    return@let it
                }
                val asMember =
                    enclosingElement.declaration.asMemberOf((container as KspType).ksType)
                checkNotNull(asMember.extensionReceiverType)
            }
        return env.wrap(originatingReference = receiverType, ksType = asMemberReceiverType)
            .copyWithScope(
                KSTypeVarianceResolverScope.MethodParameter(
                    kspExecutableElement = enclosingElement,
                    parameterIndex = 0, // Receiver param is the 1st one
                    annotated = enclosingElement.declaration,
                    container = container?.ksType?.declaration,
                    asMemberOf = container,
                )
            )
    }

    override fun kindName(): String {
        return "synthetic receiver parameter"
    }

    override fun validate(): Boolean {
        return true
    }

    override fun equals(other: Any?): Boolean {
        return XEquality.equals(this, other)
    }

    override fun toString(): String {
        return name
    }

    override fun hashCode(): Int {
        return XEquality.hashCode(equalityItems)
    }
}
