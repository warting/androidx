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

package androidx.compose.foundation.samples

import android.os.Build
import androidx.annotation.Sampled
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.magnifier
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput

@Sampled
@Composable
fun MagnifierSample() {
    // When the magnifier center position is Unspecified, it is hidden.
    // Hide the magnifier until a drag starts.
    var magnifierCenter by remember { mutableStateOf(Offset.Unspecified) }

    if (Build.VERSION.SDK_INT < 28) {
        Text("Magnifier is not supported on this platform.")
    } else {
        Box(
            Modifier.magnifier(sourceCenter = { magnifierCenter }, zoom = 2f)
                .pointerInput(Unit) {
                    detectDragGestures(
                        // Show the magnifier at the original pointer position.
                        onDragStart = { magnifierCenter = it },
                        // Make the magnifier follow the finger while dragging.
                        onDrag = { _, delta -> magnifierCenter += delta },
                        // Hide the magnifier when the finger lifts.
                        onDragEnd = { magnifierCenter = Offset.Unspecified },
                        onDragCancel = { magnifierCenter = Offset.Unspecified },
                    )
                }
                .drawBehind {
                    // Some concentric circles to zoom in on.
                    for (diameter in 2 until size.maxDimension.toInt() step 10) {
                        drawCircle(color = Color.Black, radius = diameter / 2f, style = Stroke())
                    }
                }
        )
    }
}
