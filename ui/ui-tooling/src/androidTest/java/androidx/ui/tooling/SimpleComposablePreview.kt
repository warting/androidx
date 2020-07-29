/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.ui.tooling

import androidx.compose.runtime.Composable
import androidx.lifecycle.Lifecycle
import androidx.compose.foundation.Text
import androidx.compose.ui.graphics.Color
import androidx.compose.material.Surface
import androidx.compose.runtime.savedinstancestate.UiSavedStateRegistryAmbient
import androidx.compose.ui.platform.LifecycleOwnerAmbient
import androidx.ui.tooling.preview.Preview

@Preview
@Composable
fun SimpleComposablePreview() {
    Surface(color = Color.Red) {
        Text("Hello world")
    }
}

@Preview
@Composable
private fun PrivateSimpleComposablePreview() {
    Surface(color = Color.Red) {
        Text("Private Hello world")
    }
}

data class Data(val name: String = "123")

@Preview
@Composable
fun DefaultParametersPreview1(a: Data = Data()) {
    if (a.name != "123") throw IllegalArgumentException("Unexpected default value")
    Text("Default parameter  ${a.name}")
}

@Preview
@Composable
fun DefaultParametersPreview2(a: Int = 3, b: Data = Data()) {
    if (a != 3) throw IllegalArgumentException("Unexpected default value")
    if (b.name != "123") throw IllegalArgumentException("Unexpected default value")
    Text("Default parameter  $a ${b.name}")
}

@Preview
@Composable
fun DefaultParametersPreview3(a: () -> Int = { 4 }, b: Int = 3, c: Data = Data()) {
    if (a() != 4) throw IllegalArgumentException("Unexpected default value")
    if (b != 3) throw IllegalArgumentException("Unexpected default value")
    if (c.name != "123") throw IllegalArgumentException("Unexpected default value")
    Text("Default parameter  ${a()} $b ${c.name}")
}

@Preview
@Composable
private fun LifecyclePreview() {
    val lifecycleState = LifecycleOwnerAmbient.current.lifecycle.currentState
    if (lifecycleState != Lifecycle.State.RESUMED) throw IllegalArgumentException(
        "Lifecycle state is not resumed. $lifecycleState"
    )
    Text("Lifecycle is $lifecycleState")
}

@Preview
@Composable
private fun UiSavedStateRegistryPreview() {
    if (UiSavedStateRegistryAmbient.current == null) throw IllegalArgumentException(
        "UiSavedStateRegistry is not provided"
    )
    Text("UiSavedStateRegistry preview")
}

class TestGroup {
    @Preview
    @Composable
    fun InClassPreview() {
        Surface(color = Color.Red) {
            Text("In class")
        }
    }
}