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

package androidx.compose.ui.test

import androidx.compose.ui.test.internal.JvmDefaultWithCompatibility

/**
 * Provides the main entry point into testing.
 *
 * Typically implemented by a test rule.
 */
@JvmDefaultWithCompatibility
interface SemanticsNodeInteractionsProvider {
    /**
     * Finds a semantics node that matches the given condition.
     *
     * Any subsequent operation on its result will expect exactly one element found (unless
     * [SemanticsNodeInteraction.assertDoesNotExist] is used) and will throw an [AssertionError] if
     * none or more than one element is found.
     *
     * For usage patterns and semantics concepts see [SemanticsNodeInteraction]
     *
     * @param matcher Matcher used for filtering
     * @param useUnmergedTree If `true`, searches the unmerged semantics tree instead of the merged
     *   semantics tree. This allows you to search for individual nodes that would otherwise be part
     *   of a larger semantic unit, for example a text and an image forming a Button together.
     * @see onAllNodes to work with multiple elements
     */
    fun onNode(
        matcher: SemanticsMatcher,
        useUnmergedTree: Boolean = false,
    ): SemanticsNodeInteraction

    /**
     * Finds all semantics nodes that match the given condition.
     *
     * If you are working with elements that are not supposed to occur multiple times use [onNode]
     * instead.
     *
     * For usage patterns and semantics concepts see [SemanticsNodeInteraction]
     *
     * @param matcher Matcher used for filtering.
     * @param useUnmergedTree If `true`, searches the unmerged semantics tree instead of the merged
     *   semantics tree. This allows you to search for individual nodes that would otherwise be part
     *   of a larger semantic unit, for example a text and an image forming a Button together.
     * @see onNode
     */
    fun onAllNodes(
        matcher: SemanticsMatcher,
        useUnmergedTree: Boolean = false,
    ): SemanticsNodeInteractionCollection
}
