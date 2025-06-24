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

package androidx.appfunctions.service

/**
 * Marks a function within a class as callable by other applications.
 *
 * The `@AppFunction` annotation signals that the annotated function can be invoked by external
 * applications with proper permission (e.g., an agent). For instance, a note-taking app could
 * expose a function allowing an agent to create notes based on user commands.
 *
 * The enclosing class of the function would be instantiated whenever an AppFunction within the
 * class is called. If the enclosing class requires constructor parameters or custom instantiation,
 * add a custom factory in [AppFunctionConfiguration.Builder.addEnclosingClassFactory]. This allows
 * you to inject dependencies or handle more complex object creation scenarios.
 *
 * When a function is annotated with `@AppFunction`, the compiler will automatically:
 * * Generate an XML file within the APK. This file describes the signatures of all
 *   `@AppFunction`-annotated functions within the application.
 * * Provide the necessary infrastructure to expose these functions via
 *   [android.app.appfunctions.AppFunctionService].
 *
 * Applications with the appropriate permissions can then use
 * [android.app.appfunctions.AppFunctionManager] to invoke these exposed functions.
 *
 * The compiler also generates an ID class associated with the class containing `@AppFunction`
 * annotations. This ID class provides constants for retrieving the unique identifier of each
 * annotated function. Consider this example:
 * ```kotlin
 * package com.example.mynotes
 *
 * class NoteFunctions: CreateNote, UpdateNote {
 *   @AppFunction
 *   override suspend fun createNote(...): Note { ... }
 *
 *   @AppFunction
 *   override suspend fun updateNote(...): Note? { ... }
 * }
 * ```
 *
 * The compiler will generate a `NoteFunctionsIds` class within the same `com.example.mynotes`
 * package. This generated class will contain constants like `CREATE_NOTE_ID` and `UPDATE_NOTE_ID`,
 * which correspond to the `createNote` and `updateNote` functions, respectively.
 *
 * **IMPORTANT:** By default, functions annotated with `@AppFunction` are executed on the main
 * thread. For operations that may take a significant amount of time, it is crucial to use a
 * coroutine dispatcher that runs on a background thread.
 *
 * In exceptional cases, implementations should throw an appropriate
 * [androidx.appfunctions.AppFunctionException]. This allows the agent to better understand the
 * cause of the failure. For example, if an input argument is invalid, throw an
 * [androidx.appfunctions.AppFunctionInvalidArgumentException] with a detailed message explaining
 * why it is invalid.
 *
 * @see AppFunctionConfiguration.Builder.addEnclosingClassFactory
 * @see androidx.appfunctions.AppFunctionException
 */
// Use BINARY here so that the annotation is kept around at the aggregation stage.
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.FUNCTION)
public annotation class AppFunction(
    /**
     * Indicates whether this function is enabled. The default value is `true`.
     *
     * If set to `false`, this function will be unavailable for invocation by other applications
     * unless explicitly enabled at runtime. When disabled, any attempt to call this function by
     * another application will be rejected.
     */
    public val isEnabled: Boolean = true,

    /**
     * Whether to use the function's KDoc as a function's description to the agent. The default
     * value is `false`.
     *
     * If set to `true`, the KDoc will be set as the function's
     * [androidx.appfunctions.metadata.AppFunctionMetadata.description]. The caller will use this
     * description to interpret when and how to use the function, including allowed parameters,
     * return type and thrown exceptions.
     */
    public val isDescribedByKdoc: Boolean = false,
)
