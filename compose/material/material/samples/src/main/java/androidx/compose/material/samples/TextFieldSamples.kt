/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.compose.material.samples

import androidx.annotation.Sampled
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextObfuscationMode
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.SecureTextField
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.TextFieldDefaults.indicatorLine
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.materialIcon
import androidx.compose.material.icons.materialPath
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp

@Sampled
@Composable
fun SimpleTextFieldSample() {
    TextField(
        state = rememberTextFieldState(),
        label = { Text("Label") },
        lineLimits = TextFieldLineLimits.SingleLine,
    )
}

@Sampled
@Composable
fun SimpleOutlinedTextFieldSample() {
    OutlinedTextField(
        state = rememberTextFieldState(),
        label = { Text("Label") },
        lineLimits = TextFieldLineLimits.SingleLine,
    )
}

@Sampled
@Composable
fun TextFieldWithIcons() {
    val state = rememberTextFieldState()
    TextField(
        state = state,
        lineLimits = TextFieldLineLimits.SingleLine,
        placeholder = { Text("placeholder") },
        leadingIcon = { Icon(Icons.Filled.Favorite, contentDescription = null) },
        trailingIcon = {
            IconButton(onClick = { state.clearText() }) {
                Icon(Icons.Filled.Clear, contentDescription = "Clear text")
            }
        },
    )
}

@Sampled
@Composable
fun TextFieldWithPlaceholder() {
    TextField(
        state = rememberTextFieldState(),
        lineLimits = TextFieldLineLimits.SingleLine,
        label = { Text("Email") },
        placeholder = { Text("example@gmail.com") },
    )
}

@Sampled
@Composable
fun TextFieldWithErrorState() {
    val state = rememberTextFieldState()
    var isError by rememberSaveable { mutableStateOf(false) }

    fun validate(text: CharSequence) {
        val atIndex = text.indexOf('@')
        isError = atIndex < 0 || text.indexOf('.', startIndex = atIndex) < 0
    }

    LaunchedEffect(Unit) {
        snapshotFlow { state.text }
            .collect {
                // Do something whenever text field value changes
                isError = false
            }
    }
    TextField(
        state = state,
        lineLimits = TextFieldLineLimits.SingleLine,
        label = { Text(if (isError) "Email*" else "Email") },
        isError = isError,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
        onKeyboardAction = { validate(state.text) },
        modifier =
            Modifier.semantics {
                // Provide localized description of the error
                if (isError) error("Email format is invalid.")
            },
    )
}

@Sampled
@Composable
fun TextFieldWithHelperMessage() {
    Column {
        TextField(
            state = rememberTextFieldState(),
            label = { Text("Label") },
            lineLimits = TextFieldLineLimits.SingleLine,
        )
        Text(
            text = "Helper message",
            color = MaterialTheme.colors.onSurface.copy(alpha = ContentAlpha.medium),
            style = MaterialTheme.typography.caption,
            modifier = Modifier.padding(start = 16.dp),
        )
    }
}

@Sampled
@Composable
fun PasswordTextField() {
    var passwordHidden by rememberSaveable { mutableStateOf(true) }
    SecureTextField(
        state = rememberTextFieldState(),
        label = { Text("Enter password") },
        textObfuscationMode =
            if (passwordHidden) TextObfuscationMode.RevealLastTyped
            else TextObfuscationMode.Visible,
        trailingIcon = {
            IconButton(onClick = { passwordHidden = !passwordHidden }) {
                val visibilityIcon =
                    if (passwordHidden) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                // Provide localized description for accessibility services
                val description = if (passwordHidden) "Show password" else "Hide password"
                Icon(imageVector = visibilityIcon, contentDescription = description)
            }
        },
    )
}

/**
 * We copy the implementation of Visibility and VisibilityOff icons to showcase them in the password
 * text field sample but to avoid adding material-icons-extended library as a dependency to the
 * samples not to increase the build time
 */
private val Icons.Filled.Visibility: ImageVector
    get() {
        if (_visibility != null) {
            return _visibility!!
        }
        _visibility =
            materialIcon(name = "Filled.Visibility") {
                materialPath {
                    moveTo(12.0f, 4.5f)
                    curveTo(7.0f, 4.5f, 2.73f, 7.61f, 1.0f, 12.0f)
                    curveToRelative(1.73f, 4.39f, 6.0f, 7.5f, 11.0f, 7.5f)
                    reflectiveCurveToRelative(9.27f, -3.11f, 11.0f, -7.5f)
                    curveToRelative(-1.73f, -4.39f, -6.0f, -7.5f, -11.0f, -7.5f)
                    close()
                    moveTo(12.0f, 17.0f)
                    curveToRelative(-2.76f, 0.0f, -5.0f, -2.24f, -5.0f, -5.0f)
                    reflectiveCurveToRelative(2.24f, -5.0f, 5.0f, -5.0f)
                    reflectiveCurveToRelative(5.0f, 2.24f, 5.0f, 5.0f)
                    reflectiveCurveToRelative(-2.24f, 5.0f, -5.0f, 5.0f)
                    close()
                    moveTo(12.0f, 9.0f)
                    curveToRelative(-1.66f, 0.0f, -3.0f, 1.34f, -3.0f, 3.0f)
                    reflectiveCurveToRelative(1.34f, 3.0f, 3.0f, 3.0f)
                    reflectiveCurveToRelative(3.0f, -1.34f, 3.0f, -3.0f)
                    reflectiveCurveToRelative(-1.34f, -3.0f, -3.0f, -3.0f)
                    close()
                }
            }
        return _visibility!!
    }
private var _visibility: ImageVector? = null

private val Icons.Filled.VisibilityOff: ImageVector
    get() {
        if (_visibilityOff != null) {
            return _visibilityOff!!
        }
        _visibilityOff =
            materialIcon(name = "Filled.VisibilityOff") {
                materialPath {
                    moveTo(12.0f, 7.0f)
                    curveToRelative(2.76f, 0.0f, 5.0f, 2.24f, 5.0f, 5.0f)
                    curveToRelative(0.0f, 0.65f, -0.13f, 1.26f, -0.36f, 1.83f)
                    lineToRelative(2.92f, 2.92f)
                    curveToRelative(1.51f, -1.26f, 2.7f, -2.89f, 3.43f, -4.75f)
                    curveToRelative(-1.73f, -4.39f, -6.0f, -7.5f, -11.0f, -7.5f)
                    curveToRelative(-1.4f, 0.0f, -2.74f, 0.25f, -3.98f, 0.7f)
                    lineToRelative(2.16f, 2.16f)
                    curveTo(10.74f, 7.13f, 11.35f, 7.0f, 12.0f, 7.0f)
                    close()
                    moveTo(2.0f, 4.27f)
                    lineToRelative(2.28f, 2.28f)
                    lineToRelative(0.46f, 0.46f)
                    curveTo(3.08f, 8.3f, 1.78f, 10.02f, 1.0f, 12.0f)
                    curveToRelative(1.73f, 4.39f, 6.0f, 7.5f, 11.0f, 7.5f)
                    curveToRelative(1.55f, 0.0f, 3.03f, -0.3f, 4.38f, -0.84f)
                    lineToRelative(0.42f, 0.42f)
                    lineTo(19.73f, 22.0f)
                    lineTo(21.0f, 20.73f)
                    lineTo(3.27f, 3.0f)
                    lineTo(2.0f, 4.27f)
                    close()
                    moveTo(7.53f, 9.8f)
                    lineToRelative(1.55f, 1.55f)
                    curveToRelative(-0.05f, 0.21f, -0.08f, 0.43f, -0.08f, 0.65f)
                    curveToRelative(0.0f, 1.66f, 1.34f, 3.0f, 3.0f, 3.0f)
                    curveToRelative(0.22f, 0.0f, 0.44f, -0.03f, 0.65f, -0.08f)
                    lineToRelative(1.55f, 1.55f)
                    curveToRelative(-0.67f, 0.33f, -1.41f, 0.53f, -2.2f, 0.53f)
                    curveToRelative(-2.76f, 0.0f, -5.0f, -2.24f, -5.0f, -5.0f)
                    curveToRelative(0.0f, -0.79f, 0.2f, -1.53f, 0.53f, -2.2f)
                    close()
                    moveTo(11.84f, 9.02f)
                    lineToRelative(3.15f, 3.15f)
                    lineToRelative(0.02f, -0.16f)
                    curveToRelative(0.0f, -1.66f, -1.34f, -3.0f, -3.0f, -3.0f)
                    lineToRelative(-0.17f, 0.01f)
                    close()
                }
            }
        return _visibilityOff!!
    }
private var _visibilityOff: ImageVector? = null

@Sampled
@Composable
fun TextFieldWithInitialValueAndSelection() {
    val state = rememberTextFieldState("Initial text", TextRange(0, 12))
    TextField(state = state, label = { Text("Label") }, lineLimits = TextFieldLineLimits.SingleLine)
}

@Sampled
@Composable
fun OutlinedTextFieldWithInitialValueAndSelection() {
    val state = rememberTextFieldState("Initial text", TextRange(0, 12))
    OutlinedTextField(
        state = state,
        label = { Text("Label") },
        lineLimits = TextFieldLineLimits.SingleLine,
    )
}

@Sampled
@Composable
fun TextFieldWithHideKeyboardOnImeAction() {
    val keyboardController = LocalSoftwareKeyboardController.current
    TextField(
        state = rememberTextFieldState(),
        label = { Text("Label") },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        onKeyboardAction = { keyboardController?.hide() },
    )
}

@Composable
fun TextArea() {
    val state =
        rememberTextFieldState(
            "This is a very long input that extends beyond the height of the text area."
        )
    TextField(state = state, modifier = Modifier.height(100.dp), label = { Text("Label") })
}

@Sampled
@Composable
fun CustomTextFieldBasedOnDecorationBox() {
    val (value, onValueChange) = remember { mutableStateOf("") }
    val interactionSource = remember { MutableInteractionSource() }
    // parameters below will be passed to BasicTextField for correct behavior of the text field,
    // and to the decoration box for proper styling and sizing
    val enabled = true
    val singleLine = true
    val passwordTransformation = PasswordVisualTransformation()

    val colors = TextFieldDefaults.textFieldColors()
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier =
            Modifier.background(
                    color = colors.backgroundColor(enabled).value,
                    shape = TextFieldDefaults.TextFieldShape,
                )
                .indicatorLine(
                    enabled = enabled,
                    isError = false,
                    interactionSource = interactionSource,
                    colors = colors,
                ),
        visualTransformation = passwordTransformation,
        // internal implementation of the BasicTextField will dispatch focus events
        interactionSource = interactionSource,
        enabled = enabled,
        singleLine = singleLine,
    ) {
        TextFieldDefaults.TextFieldDecorationBox(
            value = value,
            visualTransformation = passwordTransformation,
            innerTextField = it,
            singleLine = singleLine,
            enabled = enabled,
            // same interaction source as the one passed to BasicTextField to read focus state
            // for text field styling
            interactionSource = interactionSource,
            // keep vertical paddings but change the horizontal
            contentPadding =
                TextFieldDefaults.textFieldWithoutLabelPadding(start = 8.dp, end = 8.dp),
        )
    }
}

@Sampled
@Composable
fun CustomOutlinedTextFieldBasedOnDecorationBox() {
    val (value, onValueChange) = remember { mutableStateOf("") }
    val interactionSource = remember { MutableInteractionSource() }
    // parameters below will be passed to BasicTextField for correct behavior of the text field,
    // and to the decoration box for proper styling and sizing
    val enabled = true
    val singleLine = true

    val colors =
        TextFieldDefaults.outlinedTextFieldColors(
            unfocusedBorderColor = Color.LightGray,
            focusedBorderColor = Color.DarkGray,
        )
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier,
        // internal implementation of the BasicTextField will dispatch focus events
        interactionSource = interactionSource,
        enabled = enabled,
        singleLine = singleLine,
    ) {
        TextFieldDefaults.OutlinedTextFieldDecorationBox(
            value = value,
            visualTransformation = VisualTransformation.None,
            innerTextField = it,
            singleLine = singleLine,
            enabled = enabled,
            // same interaction source as the one passed to BasicTextField to read focus state
            // for text field styling
            interactionSource = interactionSource,
            // keep vertical paddings but change the horizontal
            contentPadding =
                TextFieldDefaults.textFieldWithoutLabelPadding(start = 8.dp, end = 8.dp),
            // update border thickness and shape
            border = {
                TextFieldDefaults.BorderBox(
                    enabled = enabled,
                    isError = false,
                    colors = colors,
                    interactionSource = interactionSource,
                    shape = RectangleShape,
                    unfocusedBorderThickness = 2.dp,
                    focusedBorderThickness = 4.dp,
                )
            },
            // update border colors
            colors = colors,
        )
    }
}
