/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.room.compiler.codegen.impl

import androidx.room.compiler.codegen.XAnnotationSpec
import androidx.room.compiler.codegen.XCodeBlock
import androidx.room.compiler.codegen.XPropertySpec
import androidx.room.compiler.codegen.XSpec
import androidx.room.compiler.codegen.java.JavaPropertySpec
import androidx.room.compiler.codegen.kotlin.KotlinPropertySpec

internal class XPropertySpecImpl(
    val java: JavaPropertySpec,
    val kotlin: KotlinPropertySpec,
) : XSpec(), XPropertySpec {

    override val name: String by lazy {
        check(java.name == kotlin.name)
        java.name
    }

    internal class Builder(
        val java: JavaPropertySpec.Builder,
        val kotlin: KotlinPropertySpec.Builder,
    ) : XSpec.Builder(), XPropertySpec.Builder {
        private val delegates: List<XPropertySpec.Builder> = listOf(java, kotlin)

        override fun addAnnotation(annotation: XAnnotationSpec) = apply {
            delegates.forEach { it.addAnnotation(annotation) }
        }

        override fun initializer(initExpr: XCodeBlock) = apply {
            delegates.forEach { it.initializer(initExpr) }
        }

        override fun build() = XPropertySpecImpl(java.build(), kotlin.build())
    }
}
