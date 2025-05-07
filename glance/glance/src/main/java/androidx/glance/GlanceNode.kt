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

@file:GlanceComposable

package androidx.glance

import androidx.annotation.RestrictTo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposeNode
import androidx.compose.runtime.DisallowComposableCalls
import androidx.compose.runtime.Updater

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Composable
public inline fun <T : Emittable> GlanceNode(
    noinline factory: () -> T,
    update: @DisallowComposableCalls Updater<T>.() -> Unit
) {
    ComposeNode<T, Applier>(factory, update)
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Composable
public inline fun <T : Emittable> GlanceNode(
    noinline factory: () -> T,
    update: @DisallowComposableCalls Updater<T>.() -> Unit,
    content: @Composable @GlanceComposable () -> Unit,
) {
    ComposeNode<T, Applier>(factory, update, content)
}
