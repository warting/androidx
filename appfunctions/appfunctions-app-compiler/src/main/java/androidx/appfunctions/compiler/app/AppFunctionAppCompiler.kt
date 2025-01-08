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

package androidx.appfunctions.compiler.app

import androidx.appfunctions.compiler.app.processors.AppFunctionIdProcessor
import androidx.appfunctions.compiler.core.ProcessingException
import androidx.appfunctions.compiler.core.logException
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.google.devtools.ksp.symbol.KSAnnotated
import com.squareup.kotlinpoet.AnnotationSpec
import javax.annotation.processing.Generated

/** The compiler to process AppFunction implementations. */
class AppFunctionAppCompiler(
    private val processors: List<SymbolProcessor>,
    private val logger: KSPLogger,
) : SymbolProcessor {

    override fun process(resolver: Resolver): List<KSAnnotated> {
        return try {
            buildList {
                for (processor in processors) {
                    addAll(processor.process(resolver))
                }
            }
        } catch (e: ProcessingException) {
            logger.logException(e)
            emptyList()
        }
    }

    class Provider : SymbolProcessorProvider {

        override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
            val idProcessor = AppFunctionIdProcessor(environment.codeGenerator)
            return AppFunctionAppCompiler(
                listOf(
                    idProcessor,
                ),
                environment.logger,
            )
        }
    }

    companion object {
        internal val GENERATED_ANNOTATION =
            AnnotationSpec.builder(Generated::class)
                .addMember("%S", AppFunctionAppCompiler::class.java.canonicalName)
                .build()
    }
}
