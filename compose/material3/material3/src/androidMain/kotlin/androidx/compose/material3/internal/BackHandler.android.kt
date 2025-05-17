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

package androidx.compose.material3.internal

import androidx.compose.runtime.Composable
import kotlinx.coroutines.flow.Flow

internal actual typealias BackEventCompat = androidx.activity.BackEventCompat

@Composable
internal actual fun BackHandler(enabled: Boolean, onBack: () -> Unit) {
    androidx.activity.compose.BackHandler(enabled, onBack)
}

@Composable
internal actual fun PredictiveBackHandler(
    enabled: Boolean,
    onBack: suspend (progress: Flow<BackEventCompat>) -> Unit,
) {
    androidx.activity.compose.PredictiveBackHandler(enabled, onBack)
}
