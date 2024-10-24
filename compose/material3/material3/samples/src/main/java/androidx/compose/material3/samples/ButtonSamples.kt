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

package androidx.compose.material3.samples

import androidx.annotation.Sampled
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview

@Preview
@Sampled
@Composable
fun ButtonSample() {
    Button(onClick = { /* Do something! */ }) { Text("Button") }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Preview
@Sampled
@Composable
fun SquareButtonSample() {
    Button(onClick = { /* Do something! */ }, shape = ButtonDefaults.squareShape) { Text("Button") }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Preview
@Sampled
@Composable
fun SmallButtonSample() {
    Button(
        onClick = { /* Do something! */ },
        contentPadding = ButtonDefaults.SmallButtonContentPadding
    ) {
        Text("Button")
    }
}

@Preview
@Sampled
@Composable
fun ElevatedButtonSample() {
    ElevatedButton(onClick = { /* Do something! */ }) { Text("Elevated Button") }
}

@Preview
@Sampled
@Composable
fun FilledTonalButtonSample() {
    FilledTonalButton(onClick = { /* Do something! */ }) { Text("Filled Tonal Button") }
}

@Preview
@Sampled
@Composable
fun OutlinedButtonSample() {
    OutlinedButton(onClick = { /* Do something! */ }) { Text("Outlined Button") }
}

@Preview
@Sampled
@Composable
fun TextButtonSample() {
    TextButton(onClick = { /* Do something! */ }) { Text("Text Button") }
}

@Preview
@Sampled
@Composable
fun ButtonWithIconSample() {
    Button(
        onClick = { /* Do something! */ },
        contentPadding = ButtonDefaults.ButtonWithIconContentPadding
    ) {
        Icon(
            Icons.Filled.Favorite,
            contentDescription = "Localized description",
            modifier = Modifier.size(ButtonDefaults.IconSize)
        )
        Spacer(Modifier.size(ButtonDefaults.IconSpacing))
        Text("Like")
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Preview
@Sampled
@Composable
fun XSmallButtonWithIconSample() {
    Button(
        onClick = { /* Do something! */ },
        modifier = Modifier.heightIn(ButtonDefaults.XSmallContainerHeight),
        contentPadding = ButtonDefaults.XSmallContentPadding
    ) {
        Icon(
            Icons.Filled.Edit,
            contentDescription = "Localized description",
            modifier = Modifier.size(ButtonDefaults.XSmallIconSize)
        )
        Spacer(Modifier.size(ButtonDefaults.XSmallIconSpacing))
        Text("Label")
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Preview
@Sampled
@Composable
fun MediumButtonWithIconSample() {
    Button(
        onClick = { /* Do something! */ },
        modifier = Modifier.heightIn(ButtonDefaults.MediumContainerHeight),
        contentPadding = ButtonDefaults.MediumContentPadding
    ) {
        Icon(
            Icons.Filled.Edit,
            contentDescription = "Localized description",
            modifier = Modifier.size(ButtonDefaults.MediumIconSize)
        )
        Spacer(Modifier.size(ButtonDefaults.MediumIconSpacing))
        Text(text = "Label", style = MaterialTheme.typography.titleMedium)
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Preview
@Sampled
@Composable
fun LargeButtonWithIconSample() {
    Button(
        onClick = { /* Do something! */ },
        modifier = Modifier.heightIn(ButtonDefaults.LargeContainerHeight),
        contentPadding = ButtonDefaults.LargeContentPadding
    ) {
        Icon(
            Icons.Filled.Edit,
            contentDescription = "Localized description",
            modifier = Modifier.size(ButtonDefaults.LargeIconSize)
        )
        Spacer(Modifier.size(ButtonDefaults.LargeIconSpacing))
        Text(text = "Label", style = MaterialTheme.typography.headlineSmall)
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Preview
@Sampled
@Composable
fun XLargeButtonWithIconSample() {
    Button(
        onClick = { /* Do something! */ },
        modifier = Modifier.heightIn(ButtonDefaults.XLargeContainerHeight),
        contentPadding = ButtonDefaults.XLargeContentPadding
    ) {
        Icon(
            Icons.Filled.Edit,
            contentDescription = "Localized description",
            modifier = Modifier.size(ButtonDefaults.XLargeIconSize)
        )
        Spacer(Modifier.size(ButtonDefaults.XLargeIconSpacing))
        Text(text = "Label", style = MaterialTheme.typography.headlineLarge)
    }
}
