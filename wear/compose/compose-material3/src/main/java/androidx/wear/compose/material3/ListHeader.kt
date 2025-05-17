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

package androidx.wear.compose.material3

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.material3.tokens.ListHeaderTokens
import androidx.wear.compose.material3.tokens.ListSubHeaderTokens

/**
 * A slot based composable for creating a list header item. [ListHeader]s are typically expected to
 * be a few words of text on a single line. The contents will be start and end padded.
 *
 * ListHeader scales itself appropriately when used within the scope of a [TransformingLazyColumn].
 *
 * Example of a [ListHeader]:
 *
 * @sample androidx.wear.compose.material3.samples.ListHeaderSample
 * @param modifier The modifier for the [ListHeader].
 * @param backgroundColor The background color to apply - typically Color.Transparent
 * @param contentColor The color to apply to content.
 * @param contentPadding The spacing values to apply internally between the background and the
 *   content.
 * @param transformation Transformation to be used when header appears inside the container that
 *   needs to dynamically change its content separately from the background.
 * @param content Slot for [ListHeader] content, expected to be a single line of text.
 */
@Composable
public fun ListHeader(
    modifier: Modifier = Modifier,
    backgroundColor: Color = Color.Transparent,
    contentColor: Color = ListHeaderDefaults.contentColor,
    contentPadding: PaddingValues = ListHeaderDefaults.ContentPadding,
    transformation: SurfaceTransformation? = null,
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.Center,
        modifier =
            modifier
                .defaultMinSize(minHeight = ListHeaderTokens.Height)
                .wrapContentSize()
                .surface(transformation = transformation, painter = ColorPainter(backgroundColor))
                .padding(contentPadding)
                .semantics(mergeDescendants = true) { heading() },
    ) {
        CompositionLocalProvider(
            LocalContentColor provides contentColor,
            LocalTextStyle provides ListHeaderTokens.ContentTypography.value,
            LocalTextConfiguration provides
                TextConfiguration(
                    textAlign = TextAlign.Center,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 3,
                ),
        ) {
            content()
        }
    }
}

/**
 * A two slot based composable for creating a list sub-header item. [ListSubHeader]s offer slots for
 * an icon and for a text label. The contents will be start and end padded.
 *
 * ListSubHeader scales itself appropriately when used within the scope of a
 * [TransformingLazyColumn].
 *
 * Example with use of [ListSubHeader]:
 *
 * @sample androidx.wear.compose.material3.samples.ListHeaderSample
 * @param modifier The modifier for the [ListSubHeader].
 * @param backgroundColor The background color to apply - typically Color.Transparent
 * @param contentColor The color to apply to content.
 * @param contentPadding The spacing values to apply internally between the background and the
 *   content.
 * @param transformation Transformer to be used when header appears inside the container that needs
 *   to dynamically change its content separately from the background.
 * @param icon A slot for providing icon to the [ListSubHeader].
 * @param label A slot for providing label to the [ListSubHeader].
 */
@Composable
public fun ListSubHeader(
    modifier: Modifier = Modifier,
    backgroundColor: Color = Color.Transparent,
    contentColor: Color = ListHeaderDefaults.subHeaderContentColor,
    contentPadding: PaddingValues = ListHeaderDefaults.SubHeaderContentPadding,
    transformation: SurfaceTransformation? = null,
    icon: (@Composable BoxScope.() -> Unit)? = null,
    label: @Composable RowScope.() -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start,
        modifier =
            modifier
                .defaultMinSize(minHeight = ListSubHeaderTokens.Height)
                .fillMaxWidth()
                .wrapContentSize(align = Alignment.CenterStart)
                .surface(painter = ColorPainter(backgroundColor), transformation = transformation)
                .padding(contentPadding)
                .semantics(mergeDescendants = true) { heading() },
    ) {
        CompositionLocalProvider(
            LocalContentColor provides contentColor,
            LocalTextStyle provides ListSubHeaderTokens.ContentTypography.value,
            LocalTextConfiguration provides
                TextConfiguration(
                    textAlign = TextAlign.Start,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 3,
                ),
        ) {
            if (icon != null) {
                Box(
                    modifier = Modifier.wrapContentSize(align = Alignment.CenterStart),
                    content = icon,
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
            label()
        }
    }
}

public object ListHeaderDefaults {
    private val TopPadding = 16.dp
    private val SubHeaderBottomPadding = 8.dp
    private val HeaderBottomPadding = 12.dp
    private val HorizontalPadding = 14.dp

    /** The default content padding for ListHeader */
    public val ContentPadding: PaddingValues =
        PaddingValues(HorizontalPadding, TopPadding, HorizontalPadding, HeaderBottomPadding)

    /** The default content padding for ListSubHeader */
    public val SubHeaderContentPadding: PaddingValues =
        PaddingValues(HorizontalPadding, TopPadding, HorizontalPadding, SubHeaderBottomPadding)

    /** The default color for ListHeader */
    public val contentColor: Color
        @Composable get() = ListHeaderTokens.ContentColor.value

    /** The default color for ListSubHeader */
    public val subHeaderContentColor: Color
        @Composable get() = ListSubHeaderTokens.ContentColor.value
}
