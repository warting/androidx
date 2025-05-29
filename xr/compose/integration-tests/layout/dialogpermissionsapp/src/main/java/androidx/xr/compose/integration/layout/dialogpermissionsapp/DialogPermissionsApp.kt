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

package androidx.xr.compose.integration.layout.dialogpermissionsapp

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.xr.compose.spatial.Subspace
import androidx.xr.compose.subspace.MainPanel
import androidx.xr.compose.subspace.SubspaceComposable
import androidx.xr.compose.subspace.layout.SubspaceModifier
import androidx.xr.compose.subspace.layout.movable
import androidx.xr.compose.subspace.layout.resizable
import androidx.xr.compose.subspace.layout.size
import androidx.xr.runtime.manifest.SCENE_UNDERSTANDING_COARSE

/**
 * Launches a dialog in the Main Panel for the scene understanding permission. If it is not given,
 * the app will close and alert of missing required permissions.
 */
class DialogPermissionsApp : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val permissionsLauncher =
            super.registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
                permissions ->
                if (permissions.values.contains(false)) {
                    Toast.makeText(this, "Missing required permissions", Toast.LENGTH_LONG).show()
                    finish()
                }
            }
        permissionsLauncher.launch(arrayOf(SCENE_UNDERSTANDING_COARSE))
        setContent { Subspace { SpatialContent() } }
    }

    @Composable
    @SubspaceComposable
    private fun SpatialContent() {
        MainPanel(modifier = SubspaceModifier.size(600.dp).movable().resizable())
    }
}
