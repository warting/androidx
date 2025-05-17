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

package androidx.wear.compose.material3.demos

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.CurvedAlignment
import androidx.wear.compose.foundation.CurvedDirection
import androidx.wear.compose.foundation.CurvedLayout
import androidx.wear.compose.foundation.CurvedModifier
import androidx.wear.compose.foundation.angularSizeDp
import androidx.wear.compose.foundation.background
import androidx.wear.compose.foundation.curvedBox
import androidx.wear.compose.foundation.curvedRow
import androidx.wear.compose.integration.demos.common.ComposableDemo
import androidx.wear.compose.material3.CurvedTextDefaults
import androidx.wear.compose.material3.curvedText
import androidx.wear.compose.material3.samples.CurvedTextBottom
import androidx.wear.compose.material3.samples.CurvedTextTop

val CurvedTextDemos =
    listOf(
        ComposableDemo("Top placement") { CurvedTextTop() },
        ComposableDemo("Bottom placement") { CurvedTextBottom() },
        ComposableDemo("Larger Font") { LargerFont() },
        ComposableDemo("Kerning Test") { KerningDemo() },
        ComposableDemo("Small Arc") { SmallArcDemo() },
        ComposableDemo("Large Arc") { LargeArcDemo() },
    )

@Composable
fun LargerFont() {
    val backgroundColor = CurvedTextDefaults.backgroundColor()
    CurvedLayout(modifier = Modifier.background(Color.DarkGray)) {
        curvedRow(
            CurvedModifier.background(backgroundColor, StrokeCap.Round),
            radialAlignment = CurvedAlignment.Radial.Center,
        ) {
            curvedText("Larger", fontSize = 24.sp)
            curvedBox(CurvedModifier.angularSizeDp(5.dp)) {}
            curvedText("Normal")
        }
    }
}

@Composable
fun KerningDemo() {
    val backgroundColor = CurvedTextDefaults.backgroundColor()
    Box(Modifier.background(Color.DarkGray)) {
        CurvedLayout {
            curvedText("MMMMMMMM", CurvedModifier.background(backgroundColor, StrokeCap.Round))
        }
        CurvedLayout(anchor = 90f, angularDirection = CurvedDirection.Angular.Reversed) {
            curvedText("MMMMMMMM", CurvedModifier.background(backgroundColor, StrokeCap.Round))
        }
    }
}

@Composable
fun SmallArcDemo() {
    val backgroundColor = CurvedTextDefaults.backgroundColor()
    CurvedLayout(Modifier.background(Color.DarkGray)) {
        // Default sweep is 70 degrees
        curvedText(
            "Long text that will be cut for sure.",
            CurvedModifier.background(backgroundColor, StrokeCap.Round),
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun LargeArcDemo() {
    val backgroundColor = CurvedTextDefaults.backgroundColor()
    CurvedLayout(Modifier.background(Color.DarkGray)) {
        // Static content can use 120 degrees
        curvedText(
            "Long text that will be cut for sure.",
            CurvedModifier.background(backgroundColor, StrokeCap.Round),
            maxSweepAngle = CurvedTextDefaults.StaticContentMaxSweepAngle,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
