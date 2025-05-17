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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.curvedComposable
import androidx.wear.compose.integration.demos.common.ComposableDemo
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.OutlinedButton
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.TimeText
import androidx.wear.compose.material3.TimeTextDefaults
import androidx.wear.compose.material3.curvedText
import androidx.wear.compose.material3.samples.TimeTextClockOnly
import androidx.wear.compose.material3.samples.TimeTextWithStatus
import androidx.wear.compose.material3.samples.TimeTextWithStatusEllipsized
import androidx.wear.compose.material3.timeTextCurvedText
import androidx.wear.compose.material3.timeTextSeparator

val TimeTextDemos =
    listOf(
        ComposableDemo("Clock only") { TimeTextClockOnly() },
        ComposableDemo("Clock with Status") { TimeTextWithStatus() },
        ComposableDemo("Clock with Ellipsized Status") { TimeTextWithStatusEllipsized() },
        ComposableDemo("Clock with long Status") { TimeTextWithLongStatus() },
        ComposableDemo("Clock with Icon") { TimeTextWithIcon() },
        ComposableDemo("Clock with custom colors") { TimeTextWithCustomColors() },
        ComposableDemo("Clock with custom font size") { TimeTextCustomSize() },
        ComposableDemo("Clock on list") { TimeTextOnScreen() },
        ComposableDemo("Clock on white background") { TimeTextOnScreenWhiteBackground() },
        ComposableDemo("Background override") { TimeTextBackgroundOverride() },
    )

@Composable
fun TimeTextWithLongStatus() {
    val style = TimeTextDefaults.timeTextStyle()
    TimeText { time ->
        curvedText("Some long leading text")
        timeTextSeparator(style)
        timeTextCurvedText(time)
    }
}

@Composable
fun TimeTextWithCustomColors() {
    val customStyle = TimeTextDefaults.timeTextStyle(color = Color.Red)

    TimeText { time ->
        curvedText("ETA", style = customStyle)
        curvedComposable { Spacer(modifier = Modifier.size(4.dp)) }
        curvedText("12:48")
        timeTextSeparator(customStyle)
        curvedText(time)
    }
}

@Composable
fun TimeTextCustomSize() {
    val style = TimeTextDefaults.timeTextStyle()
    val customStyle = TimeTextDefaults.timeTextStyle(color = Color.Green, fontSize = 24.sp)

    TimeText { time ->
        curvedText("ETA", style = customStyle)
        timeTextSeparator(style)
        timeTextCurvedText(time)
    }
}

@Composable
fun TimeTextWithIcon() {
    val style = TimeTextDefaults.timeTextStyle()

    TimeText { time ->
        timeTextCurvedText(time)
        timeTextSeparator(style)
        curvedComposable {
            Icon(
                imageVector = Icons.Filled.Favorite,
                contentDescription = "Favorite",
                modifier = Modifier.size(13.dp),
            )
        }
    }
}

@Composable
fun TimeTextOnScreen() {
    val colors =
        listOf(
            ButtonDefaults.buttonColors(),
            ButtonDefaults.filledTonalButtonColors(),
            ButtonDefaults.filledVariantButtonColors(),
            ButtonDefaults.childButtonColors(),
        )

    AppScaffold() {
        ScalingLazyDemo() {
            item { ListHeader { Text("Buttons") } }
            item {
                OutlinedButton(onClick = {}) {
                    Text("Outlined Button", Modifier.align(Alignment.CenterVertically))
                }
            }
            item {
                Button(onClick = {}) {
                    Text("Filled Button", Modifier.align(Alignment.CenterVertically))
                }
            }
            items(colors.size) {
                Button(onClick = {}, colors = colors[it]) {
                    Text(
                        "Item with long content so test overlap.",
                        Modifier.align(Alignment.CenterVertically),
                    )
                }
            }
            items(10) { Text("Some extra items ($it) to scroll", Modifier.padding(5.dp)) }
        }
    }
}

@Composable
fun TimeTextOnScreenWhiteBackground() {
    Box(Modifier.fillMaxSize().background(Color.White)) {
        TimeText { time -> timeTextCurvedText(time) }
    }
}

@Composable
fun TimeTextBackgroundOverride() {
    Box(Modifier.fillMaxSize().background(Color.DarkGray)) {
        TimeText(backgroundColor = Color.Transparent) { time -> timeTextCurvedText(time) }
    }
}
