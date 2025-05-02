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

package androidx.navigation3.runtime

import androidx.compose.runtime.Composable

/**
 * Interface that offers the ability to provide information to some Composable content that is
 * integrated with a [NavDisplay](reference/androidx/navigation/NavDisplay).
 *
 * Information can be provided to the entire back stack via [NavEntryDecorator.DecorateBackStack] or
 * to a single entry via [NavEntryDecorator.DecorateEntry].
 */
public interface NavEntryDecorator {

    /**
     * Allows a [NavEntryDecorator] to provide to the entire backstack.
     *
     * This function is called by the [DecoratedNavEntryProvider] and should not be called directly.
     */
    @Composable
    public fun DecorateBackStack(backStack: List<Any>, content: @Composable () -> Unit): Unit =
        content.invoke()

    /**
     * Allows a [NavEntryDecorator] to provide information to a single entry.
     *
     * This function is called by the [NavDisplay](reference/androidx/navigation/NavDisplay) and
     * should not be called directly.
     */
    @Composable public fun <T : Any> DecorateEntry(entry: NavEntry<T>)
}
