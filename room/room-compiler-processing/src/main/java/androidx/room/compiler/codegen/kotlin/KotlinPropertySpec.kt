/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.room.compiler.codegen.kotlin

import androidx.room.compiler.codegen.KPropertySpec
import androidx.room.compiler.codegen.KPropertySpecBuilder
import androidx.room.compiler.codegen.XAnnotationSpec
import androidx.room.compiler.codegen.XCodeBlock
import androidx.room.compiler.codegen.XPropertySpec
import androidx.room.compiler.codegen.XSpec
import androidx.room.compiler.codegen.XTypeName
import androidx.room.compiler.codegen.impl.XAnnotationSpecImpl
import androidx.room.compiler.codegen.impl.XCodeBlockImpl

internal class KotlinPropertySpec(
    override val name: String,
    override val type: XTypeName,
    override val actual: KPropertySpec
) : KotlinSpec<KPropertySpec>(), XPropertySpec {
    override fun toBuilder() = Builder(name, type, actual.toBuilder())

    internal class Builder(
        private val name: String,
        private val type: XTypeName,
        internal val actual: KPropertySpecBuilder
    ) : XSpec.Builder(), XPropertySpec.Builder {

        override fun addAnnotation(annotation: XAnnotationSpec) = apply {
            require(annotation is XAnnotationSpecImpl)
            actual.addAnnotation(annotation.kotlin.actual)
        }

        override fun initializer(initExpr: XCodeBlock) = apply {
            require(initExpr is XCodeBlockImpl)
            actual.initializer(initExpr.kotlin.actual)
        }

        override fun build() = KotlinPropertySpec(name, type, actual.build())
    }
}
