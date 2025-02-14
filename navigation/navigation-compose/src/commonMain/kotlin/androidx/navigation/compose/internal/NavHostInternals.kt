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

package androidx.navigation.compose.internal

import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModelStoreOwner
import kotlinx.coroutines.flow.Flow

internal expect object LocalViewModelStoreOwner {
    @get:Composable val current: ViewModelStoreOwner?
}

internal expect class BackEventCompat {
    val touchX: Float
    val touchY: Float
    val progress: Float
    val swipeEdge: Int
}

@Composable
internal expect fun PredictiveBackHandler(
    enabled: Boolean = true,
    onBack: suspend (progress: Flow<BackEventCompat>) -> Unit
)
