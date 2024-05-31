/*
 * Copyright 2023 The Android Open Source Project
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

@file:OptIn(ExperimentalMaterialApi::class)

package androidx.compose.foundation.demos.text2

import androidx.compose.foundation.border
import androidx.compose.foundation.demos.text.TagLine
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.LocalContentColor
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun DecorationBoxDemos() {
    Column(Modifier.imePadding().verticalScroll(rememberScrollState()).padding(16.dp)) {
        TagLine(tag = "Simple Decoration w/ Label")
        SimpleDecorationWithLabel()

        TagLine(tag = "OutlinedTextField")
        OutlinedBasicTextField()

        TagLine(tag = "No inner text field")
        NoInnerTextField()
    }
}

@Composable
fun SimpleDecorationWithLabel() {
    val state = remember { TextFieldState() }
    BasicTextField(
        state = state,
        modifier = Modifier,
        textStyle = LocalTextStyle.current,
        decorator = {
            Column(Modifier.padding(4.dp)) {
                Text("Label", style = MaterialTheme.typography.caption)
                Spacer(modifier = Modifier.height(4.dp))
                it()
            }
        }
    )
}

@Composable
fun OutlinedBasicTextField() {
    val state = remember { TextFieldState() }
    val cursorColor by TextFieldDefaults.outlinedTextFieldColors().cursorColor(isError = false)
    BasicTextField(
        state = state,
        modifier = Modifier.fillMaxWidth(),
        textStyle = LocalTextStyle.current.copy(color = LocalContentColor.current),
        cursorBrush = SolidColor(cursorColor),
        lineLimits = TextFieldLineLimits.MultiLine(1, 3),
        decorator = {
            TextFieldDefaults.OutlinedTextFieldDecorationBox(
                value = state.text.toString(),
                visualTransformation = VisualTransformation.None,
                innerTextField = it,
                placeholder = null,
                label = null,
                leadingIcon = null,
                trailingIcon = null,
                singleLine = true,
                enabled = true,
                isError = false,
                interactionSource = remember { MutableInteractionSource() },
                colors = TextFieldDefaults.outlinedTextFieldColors()
            )
        }
    )
}

@Composable
fun NoInnerTextField(modifier: Modifier = Modifier) {
    val state = rememberTextFieldState()
    BasicTextField(
        state = state,
        modifier = modifier,
        textStyle = TextStyle(fontSize = 24.sp),
        decorator = {
            Box(
                Modifier.fillMaxWidth()
                    .border(1.dp, Color.Blue, RoundedCornerShape(8.dp))
                    .padding(8.dp)
            ) {
                Text("${state.text}", Modifier.align(Alignment.Center))
            }
        }
    )
}
