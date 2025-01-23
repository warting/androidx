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

package androidx.navigation3

import androidx.compose.runtime.Composable
import kotlin.collections.plus

/**
 * Function that provides all of the [NavEntry]s wrapped with the given [NavLocalProvider]s. It is
 * responsible for executing the functions provided by each [NavLocalProvider] appropriately.
 *
 * Note: the order in which the [NavLocalProvider]s are added to the list determines their scope,
 * i.e. a [NavLocalProvider] added earlier in a list has its data available to those added later.
 *
 * @param backStack the list of keys that represent the backstack
 * @param localProviders the [NavLocalProvider]s that are providing data to the content
 * @param entryProvider a function that returns the [NavEntry] for a given key
 * @param content the content to be displayed
 */
@Composable
public fun <T : Any> NavBackStackProvider(
    backStack: List<T>,
    localProviders: List<NavLocalProvider>,
    entryProvider: (key: T) -> NavEntry<T>,
    content: @Composable (List<NavEntry<T>>) -> Unit
) {
    val finalProviders = (listOf(SaveableStateNavLocalProvider()) + localProviders).distinct()

    // Generates a list of entries that are wrapped with the given providers
    val entries =
        backStack.map {
            val entry = entryProvider.invoke(it)
            finalProviders.distinct().foldRight(entry) { provider: NavLocalProvider, wrappedEntry ->
                object : NavEntryWrapper<T>(wrappedEntry) {
                    override val content: @Composable ((T) -> Unit)
                        get() = { provider.ProvideToEntry(wrappedEntry) }
                }
            }
        }

    // Provides the entire backstack to the previously wrapped entries
    finalProviders
        .distinct()
        .foldRight<NavLocalProvider, @Composable () -> Unit>({ content(entries) }) {
            provider: NavLocalProvider,
            wrappedContent ->
            { provider.ProvideToBackStack(backStack = backStack, wrappedContent) }
        }
        .invoke()
}
