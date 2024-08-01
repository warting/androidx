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

package androidx.room.compiler.processing.util.compiler.steps

import androidx.room.compiler.processing.util.DiagnosticLocation
import androidx.room.compiler.processing.util.DiagnosticMessage
import androidx.room.compiler.processing.util.Resource
import androidx.room.compiler.processing.util.Source
import androidx.room.compiler.processing.util.compiler.SourceSet
import java.io.File
import javax.tools.Diagnostic

/**
 * Kotlin compilation is run in multiple steps:
 * * process KSP
 * * process KAPT
 * * compile kotlin sources
 * * compile java sources
 *
 * Each step implements the [KotlinCompilationStep] interfaces and provides the arguments for the
 * following step.
 */
internal interface KotlinCompilationStep {
    /** A name to identify the step. */
    val name: String

    fun execute(
        /** Temporary folder that can be used by the step */
        workingDir: File,
        /** Compilation parameters for the step. */
        arguments: CompilationStepArguments
    ): CompilationStepResult
}

/** Diagnostic message that was captured from the compiler, before it is processed. */
internal data class RawDiagnosticMessage(
    val kind: Diagnostic.Kind,
    val message: String,
    val location: Location?
) {
    data class Location(
        val path: String,
        val line: Int,
    )
}

/** Parameters for each compilation step */
internal data class CompilationStepArguments(
    /**
     * List of source sets. Each source set has a root folder that can be used to pass to the
     * compiler.
     */
    val sourceSets: List<SourceSet>,
    /** Any additional classpath provided to the compilation */
    val additionalClasspaths: List<File>,
    /** If `true`, the classpath of the test application should be provided to the compiler */
    val inheritClasspaths: Boolean,
    /**
     * Arguments to pass to the java compiler. This is also important for KAPT where part of the
     * compilation is run by javac.
     */
    val javacArguments: List<String>,

    /** Arguments to pass to the kotlin compiler. */
    val kotlincArguments: List<String>,
)

/** Result of a compilation step. */
internal data class CompilationStepResult(
    /** Whether it succeeded or not. */
    val success: Boolean,
    /** List of source sets generated by this step */
    val generatedSourceRoots: List<SourceSet>,
    /** List of diagnotic messages created by this step */
    val diagnostics: List<DiagnosticMessage>,
    /**
     * Arguments for the next compilation step. Current step might've modified its own parameters
     * (e.g. add generated sources etc) for this one.
     */
    val nextCompilerArguments: CompilationStepArguments,
    /** If the step compiled sources, this field includes the list of Files for each classpath. */
    val outputClasspath: List<File>,
    /** List of resource files generated by this step */
    val generatedResources: List<Resource>
) {
    val generatedSources: List<Source> by lazy { generatedSourceRoots.flatMap { it.sources } }

    companion object {
        /**
         * Creates a [CompilationStepResult] that does not create any outputs but instead simply
         * passes the arguments to the next step.
         */
        fun skip(arguments: CompilationStepArguments) =
            CompilationStepResult(
                success = true,
                generatedSourceRoots = emptyList(),
                diagnostics = emptyList(),
                nextCompilerArguments = arguments,
                outputClasspath = emptyList(),
                generatedResources = emptyList()
            )
    }
}

/** Associates [RawDiagnosticMessage]s with sources and creates [DiagnosticMessage]s. */
internal fun resolveDiagnostics(
    diagnostics: List<RawDiagnosticMessage>,
    sourceSets: List<SourceSet>,
): List<DiagnosticMessage> {
    return diagnostics.map { rawDiagnostic ->
        // match it to source
        val location = rawDiagnostic.location
        if (location == null) {
            DiagnosticMessage(
                kind = rawDiagnostic.kind,
                msg = rawDiagnostic.message,
                location = null,
            )
        } else {
            // find matching source file
            val source = sourceSets.firstNotNullOfOrNull { it.findSourceFile(location.path) }

            // source might be null for KAPT if it failed to match the diagnostic to a real
            // source file (e.g. error is reported on the stub)
            check(source != null || location.path.contains("kapt")) {
                "Cannot find source file for the diagnostic :/ $rawDiagnostic"
            }
            DiagnosticMessage(
                kind = rawDiagnostic.kind,
                msg = rawDiagnostic.message,
                location =
                    DiagnosticLocation(
                        source = source,
                        line = location.line,
                    ),
            )
        }
    }
}
