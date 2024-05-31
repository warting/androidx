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

package androidx.compose.material3

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.paddingFromBaseline
import androidx.compose.foundation.layout.requiredHeightIn
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material3.tokens.PlainTooltipTokens
import androidx.compose.material3.tokens.RichTooltipTokens
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize

/**
 * Plain tooltip that provides a descriptive message.
 *
 * Usually used with [TooltipBox].
 *
 * @param modifier the [Modifier] to be applied to the tooltip.
 * @param caretSize [DpSize] for the caret of the tooltip, if a default caret is desired with a
 *   specific dimension. Please see [TooltipDefaults.caretSize] to see the default dimensions. Pass
 *   in Dp.Unspecified for this parameter if no caret is desired.
 * @param shape the [Shape] that should be applied to the tooltip container.
 * @param contentColor [Color] that will be applied to the tooltip's content.
 * @param containerColor [Color] that will be applied to the tooltip's container.
 * @param tonalElevation the tonal elevation of the tooltip.
 * @param shadowElevation the shadow elevation of the tooltip.
 * @param content the composable that will be used to populate the tooltip's content.
 */
@Composable
@ExperimentalMaterial3Api
actual fun TooltipScope.PlainTooltip(
    modifier: Modifier,
    caretSize: DpSize,
    shape: Shape,
    contentColor: Color,
    containerColor: Color,
    tonalElevation: Dp,
    shadowElevation: Dp,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier,
        shape = shape,
        color = containerColor,
        tonalElevation = tonalElevation,
        shadowElevation = shadowElevation
    ) {
        Box(
            modifier =
                Modifier.sizeIn(
                        minWidth = TooltipMinWidth,
                        maxWidth = PlainTooltipMaxWidth,
                        minHeight = TooltipMinHeight
                    )
                    .padding(PlainTooltipContentPadding)
        ) {
            val textStyle = PlainTooltipTokens.SupportingTextFont.value
            CompositionLocalProvider(
                LocalContentColor provides contentColor,
                LocalTextStyle provides textStyle,
                content = content
            )
        }
    }
}

/**
 * Rich text tooltip that allows the user to pass in a title, text, and action. Tooltips are used to
 * provide a descriptive message.
 *
 * Usually used with [TooltipBox]
 *
 * @param modifier the [Modifier] to be applied to the tooltip.
 * @param title An optional title for the tooltip.
 * @param action An optional action for the tooltip.
 * @param caretSize [DpSize] for the caret of the tooltip, if a default caret is desired with a
 *   specific dimension. Please see [TooltipDefaults.caretSize] to see the default dimensions. Pass
 *   in Dp.Unspecified for this parameter if no caret is desired.
 * @param shape the [Shape] that should be applied to the tooltip container.
 * @param colors [RichTooltipColors] that will be applied to the tooltip's container and content.
 * @param tonalElevation the tonal elevation of the tooltip.
 * @param shadowElevation the shadow elevation of the tooltip.
 * @param text the composable that will be used to populate the rich tooltip's text.
 */
@Composable
@ExperimentalMaterial3Api
actual fun TooltipScope.RichTooltip(
    modifier: Modifier,
    title: (@Composable () -> Unit)?,
    action: (@Composable () -> Unit)?,
    caretSize: DpSize,
    shape: Shape,
    colors: RichTooltipColors,
    tonalElevation: Dp,
    shadowElevation: Dp,
    text: @Composable () -> Unit
) {
    Surface(
        modifier =
            modifier.sizeIn(
                minWidth = TooltipMinWidth,
                maxWidth = RichTooltipMaxWidth,
                minHeight = TooltipMinHeight
            ),
        shape = shape,
        color = colors.containerColor,
        tonalElevation = tonalElevation,
        shadowElevation = shadowElevation
    ) {
        val actionLabelTextStyle = RichTooltipTokens.ActionLabelTextFont.value
        val subheadTextStyle = RichTooltipTokens.SubheadFont.value
        val supportingTextStyle = RichTooltipTokens.SupportingTextFont.value

        Column(modifier = Modifier.padding(horizontal = RichTooltipHorizontalPadding)) {
            title?.let {
                Box(modifier = Modifier.paddingFromBaseline(top = HeightToSubheadFirstLine)) {
                    CompositionLocalProvider(
                        LocalContentColor provides colors.titleContentColor,
                        LocalTextStyle provides subheadTextStyle,
                        content = it
                    )
                }
            }
            Box(modifier = Modifier.textVerticalPadding(title != null, action != null)) {
                CompositionLocalProvider(
                    LocalContentColor provides colors.contentColor,
                    LocalTextStyle provides supportingTextStyle,
                    content = text
                )
            }
            action?.let {
                Box(
                    modifier =
                        Modifier.requiredHeightIn(min = ActionLabelMinHeight)
                            .padding(bottom = ActionLabelBottomPadding)
                ) {
                    CompositionLocalProvider(
                        LocalContentColor provides colors.actionContentColor,
                        LocalTextStyle provides actionLabelTextStyle,
                        content = it
                    )
                }
            }
        }
    }
}
