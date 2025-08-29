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

package androidx.navigation3.runtime

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.snapshots.StateObject

/**
 * A mutable back stack of [NavKey] elements that integrates with Compose state.
 *
 * This class wraps a [SnapshotStateList] so that updates to the stack automatically trigger
 * recomposition in any observing Composables. It also implements [StateObject], which allows it to
 * participate in Compose's snapshot system directly.
 *
 * Typically, you won’t construct a [NavBackStack] manually. Instead, prefer using
 * [rememberNavBackStack], which provides a stack that is automatically saved and restored across
 * process death and configuration changes.
 *
 * ### Example
 *
 * ```kotlin
 * val backStack = NavBackStack(Home("start"))
 * backStack += Details("item42") // pushes onto stack
 * backStack.removeLast()         // pops stack
 * ```
 *
 * @constructor Creates a new back stack backed by the provided [SnapshotStateList].
 * @see rememberNavBackStack for lifecycle-aware persistence.
 */
public class NavBackStack public constructor(base: SnapshotStateList<NavKey>) :
    MutableList<NavKey> by base, StateObject by base {

    public constructor() : this(base = mutableStateListOf())

    public constructor(vararg elements: NavKey) : this(base = mutableStateListOf(*elements))
}
