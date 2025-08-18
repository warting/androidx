/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.xr.glimmer

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Title Chip is a component used to provide context for associated content, such as a [Card]. They
 * are designed for brief information, typically a short title, name, or status. Title chips are not
 * focusable, and not interactable.
 *
 * @sample androidx.xr.glimmer.samples.TitleChipSample
 *
 * Title chips may have a leading icon to provide more context:
 *
 * @sample androidx.xr.glimmer.samples.TitleChipWithLeadingIconSample
 *
 * To use a title chip with another component, place the title chip
 * [TitleChipDefaults.AssociatedContentSpacing] above the other component. For example, to use a
 * title chip with a card:
 *
 * @sample androidx.xr.glimmer.samples.TitleChipWithCardSample
 * @param modifier the [Modifier] to be applied to this title chip
 * @param leadingIcon optional leading icon to be placed before the [content]. This is typically an
 *   [Icon].
 * @param shape the [Shape] used to clip this title chip, and also used to draw the background and
 *   border
 * @param color background color of this title chip
 * @param contentColor content color used by components inside [content]
 * @param border the border to draw around this title chip
 * @param contentPadding the spacing values to apply internally between the container and the
 *   content
 * @param content the main content, typically [Text], to display inside this title chip
 */
@Composable
public fun TitleChip(
    modifier: Modifier = Modifier,
    leadingIcon: @Composable (() -> Unit)? = null,
    shape: Shape = GlimmerTheme.shapes.large,
    color: Color = GlimmerTheme.colors.surface,
    contentColor: Color = calculateContentColor(color),
    border: BorderStroke? = SurfaceDefaults.border(),
    contentPadding: PaddingValues = TitleChipDefaults.contentPadding(hasIcon = leadingIcon != null),
    content: @Composable RowScope.() -> Unit,
) {
    val colors = GlimmerTheme.colors
    val iconSize = GlimmerTheme.iconSizes.medium

    CompositionLocalProvider(LocalTextStyle provides GlimmerTheme.typography.titleSmall) {
        Row(
            modifier
                .surface(
                    focusable = false,
                    shape = shape,
                    color = color,
                    contentColor = contentColor,
                    depth = null,
                    border = border,
                )
                .defaultMinSize(minHeight = MinimumHeight)
                .widthIn(max = MaximumWidth)
                .padding(contentPadding),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (leadingIcon != null) {
                Box(Modifier.padding(end = IconSpacing).contentColorProvider(colors.primary)) {
                    CompositionLocalProvider(LocalIconSize provides iconSize, content = leadingIcon)
                }
            }
            content()
        }
    }
}

/** Default values used for [TitleChip]. */
public object TitleChipDefaults {
    /**
     * Default content padding used for a [TitleChip]
     *
     * @param hasIcon whether the [TitleChip] has an icon specified
     */
    public fun contentPadding(hasIcon: Boolean): PaddingValues =
        if (hasIcon) ContentPaddingWithIcon else ContentPadding

    /**
     * Default spacing between the bottom of a [TitleChip] and content associated with this title
     * chip, such as a [Card]. For example this can be used with a
     * [androidx.compose.foundation.layout.Spacer], or with [padding].
     *
     * @sample androidx.xr.glimmer.samples.TitleChipWithCardSample
     */
    public val AssociatedContentSpacing: Dp = 12.dp
}

/** Default content padding for a [TitleChip] */
private val ContentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)

/** Default content padding for a [TitleChip] with an icon specified */
private val ContentPaddingWithIcon =
    PaddingValues(start = 8.dp, end = 16.dp, top = 8.dp, bottom = 8.dp)

/** Default minimum height for a [TitleChip] */
private val MinimumHeight = 56.dp

/** Default maximum width for a [TitleChip] */
private val MaximumWidth = 352.dp

/** Spacing between icons and the text in a [TitleChip] */
private val IconSpacing = 8.dp
