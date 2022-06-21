/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.camera.integration.uiwidgets.compose.ui.navigation

import androidx.camera.integration.uiwidgets.compose.ui.Greeting
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable

@Composable
fun ComposeCameraNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = ComposeCameraScreen.ImageCapture.name,
        modifier = modifier
    ) {
        composable(ComposeCameraScreen.ImageCapture.name) {
            Greeting()
        }

        composable(ComposeCameraScreen.VideoCapture.name) {
            Greeting()
        }

        composable(ComposeCameraScreen.Gallery.name) {
            Greeting()
        }
    }
}