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

package androidx.compose.foundation.text

import androidx.compose.foundation.implementedInJetBrainsFork
import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily

@Suppress("ComposableNaming")
@Composable
@NonRestartableComposable
internal actual fun BackgroundTextMeasurement(
    text: String,
    style: TextStyle,
    fontFamilyResolver: FontFamily.Resolver
): Unit = implementedInJetBrainsFork()

@Suppress("ComposableNaming")
@Composable
@NonRestartableComposable
internal actual fun BackgroundTextMeasurement(
    text: AnnotatedString,
    style: TextStyle,
    fontFamilyResolver: FontFamily.Resolver,
    placeholders: List<AnnotatedString.Range<Placeholder>>?
): Unit = implementedInJetBrainsFork()
