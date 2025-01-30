/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.appfunctions.compiler.processors

import androidx.appfunctions.compiler.core.AnnotatedAppFunctionSerializable
import androidx.appfunctions.compiler.core.IntrospectionHelper.AppFunctionSerializableAnnotation
import androidx.appfunctions.compiler.core.ProcessingException
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration

/**
 * Generates a factory class with methods to convert classes annotated with
 * [androidx.appfunctions.AppFunctionSerializable] to [androidx.appfunctions.AppFunctionData], and
 * vice-versa.
 */
class AppFunctionSerializableProcessor(
    private val codeGenerator: CodeGenerator,
) : SymbolProcessor {
    override fun process(resolver: Resolver): List<KSAnnotated> {
        val entityClasses = resolveAppFunctionSerializables(resolver)
        for (entity in entityClasses) {
            // TODO: implement createEntityFactoryClass()
        }
        return emptyList()
    }

    fun resolveAppFunctionSerializables(
        resolver: Resolver
    ): List<AnnotatedAppFunctionSerializable> {
        val annotatedAppFunctionSerializables =
            resolver.getSymbolsWithAnnotation(
                AppFunctionSerializableAnnotation.CLASS_NAME.canonicalName
            )
        return annotatedAppFunctionSerializables
            .map {
                if (it !is KSClassDeclaration) {
                    throw ProcessingException(
                        "Only classes can be annotated with @AppFunctionSerializable",
                        it
                    )
                }
                AnnotatedAppFunctionSerializable(it).validate()
            }
            .toList()
    }
}
