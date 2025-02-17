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

package androidx.appfunctions

import androidx.annotation.RestrictTo
import androidx.annotation.StringDef

/**
 * Marks a class as a registry for AppFunction components, enabling compile-time aggregation.
 *
 * This annotation is used by the AppFunction compiler plugin to discover and aggregate AppFunction
 * components declared across different modules. The compiler plugin searches for classes annotated
 * with [AppFunctionComponentRegistry] within the designated `appfunctions_aggregated_deps` package.
 *
 * The registry classes are usually generated by AppFunction compiler plugin automatically while
 * processing each submodules. For example, for an AppFunction implementation like
 *
 * ```
 * package com.notes
 *
 * class NotesFunction {
 *   @AppFunction
 *   suspend fun createNote(params: Params): Note { ... }
 * }
 * ```
 *
 * An inventory registry would be generated:
 * ```
 * package appfunctions_aggregated_deps
 *
 * @AppFunctionComponentRegistry(
 *   componentCategory = AppFunctionComponentCategory.INVENTORY,
 *   componentNames = [
 *     "com.notes.${'$'}NotesFunction_AppFunctionInventory",
 *   ]
 * )
 * ```
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.CLASS)
public annotation class AppFunctionComponentRegistry(
    @AppFunctionComponentCategory public val componentCategory: String,
    public val componentNames: Array<String>,
)

/**
 * Defines the categories of AppFunction components that can be registered in a
 * [AppFunctionComponentRegistry].
 *
 * These categories guide the AppFunction compiler plugin in how to process and aggregate the
 * registered components.
 */
@StringDef(
    AppFunctionComponentCategory.INVENTORY,
    AppFunctionComponentCategory.INVOKER,
    AppFunctionComponentCategory.FUNCTION,
)
@Retention(AnnotationRetention.SOURCE)
internal annotation class AppFunctionComponentCategory {
    companion object {
        /**
         * The components in inventory category are used to generate the implementation of an
         * [androidx.appfunctions.internal.AggregatedAppFunctionInventory].
         */
        const val INVENTORY: String = "INVENTORY"
        /**
         * The components in invoker category are used to generate the implementation of an
         * [androidx.appfunctions.internal.AggregatedAppFunctionInvoker].
         */
        const val INVOKER: String = "INVOKER"
        /**
         * The components in function category are used to generate the asset XML file for platform
         * indexer to index available functions from the app.
         */
        const val FUNCTION: String = "FUNCTION"
    }
}
