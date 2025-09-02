/*
 * Copyright (C) 2024 The Android Open Source Project
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
@file:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)

package androidx.compose.remote.frontend.action

import androidx.annotation.RestrictTo
import androidx.compose.remote.creation.actions.Action
import androidx.compose.runtime.Composable

/**
 * A RemoteCompose frontend model of Actions that can be converted to either RemoteCompose
 * operations or a ComposeUI lambda.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public interface Action {
    public fun toRemoteAction(): Action

    public @Composable fun toComposeUiAction(): () -> Unit
}
