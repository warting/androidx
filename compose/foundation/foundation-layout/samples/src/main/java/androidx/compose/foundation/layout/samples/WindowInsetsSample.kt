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
package androidx.compose.foundation.layout.samples

import androidx.annotation.Sampled
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fitInside
import androidx.compose.foundation.layout.fitOutside
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.WindowInsetsRulers.Companion.NavigationBars
import androidx.compose.ui.layout.WindowInsetsRulers.Companion.SafeContent
import androidx.compose.ui.layout.WindowInsetsRulers.Companion.StatusBars

@Sampled
@Composable
fun FitInsideOutsideExample() {
    Box(Modifier.fillMaxSize()) {
        // Drawn behind the status bar
        Box(Modifier.fillMaxSize().fitOutside(StatusBars.current).background(Color.Blue))
        // Drawn behind the navigation bar
        Box(Modifier.fillMaxSize().fitOutside(NavigationBars.current).background(Color.Red))
        // Body of the app
        Box(Modifier.fillMaxSize().fitInside(SafeContent.current).background(Color.Yellow))
    }
}
