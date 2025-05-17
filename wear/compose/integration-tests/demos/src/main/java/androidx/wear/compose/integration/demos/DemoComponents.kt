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

package androidx.wear.compose.integration.demos

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.LocalContentAlpha
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text

/** A simple [Icon] with default size */
@Composable
fun DemoIcon(
    resourceId: Int,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    contentDescription: String? = null,
) {
    Icon(
        painter = painterResource(id = resourceId),
        contentDescription = contentDescription,
        modifier = modifier.size(size).wrapContentSize(align = Alignment.Center),
    )
}

@Composable
fun DemoImage(resourceId: Int, modifier: Modifier = Modifier, size: Dp = 24.dp) {
    Image(
        painter = painterResource(id = resourceId),
        contentDescription = null,
        modifier = modifier.size(size),
        contentScale = ContentScale.Crop,
        alpha = LocalContentAlpha.current,
    )
}

@Composable
fun TextIcon(text: String, size: Dp = 24.dp, style: TextStyle = MaterialTheme.typography.button) {
    Button(
        modifier = Modifier.padding(0.dp).requiredSize(32.dp),
        onClick = {},
        colors =
            ButtonDefaults.buttonColors(
                disabledBackgroundColor =
                    MaterialTheme.colors.primary.copy(alpha = LocalContentAlpha.current),
                disabledContentColor =
                    MaterialTheme.colors.onPrimary.copy(alpha = LocalContentAlpha.current),
            ),
        enabled = false,
    ) {
        Box(
            modifier =
                Modifier.padding(all = 0.dp)
                    .requiredSize(size)
                    .wrapContentSize(align = Alignment.Center)
        ) {
            Text(
                text = text,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colors.onPrimary.copy(alpha = LocalContentAlpha.current),
                style = style,
            )
        }
    }
}

public val DemoListTag = "DemoListTag"

public val AlternatePrimaryColor1 = Color(0x7F, 0xCF, 0xFF)
public val AlternatePrimaryColor2 = Color(0xD0, 0xBC, 0xFF)
public val AlternatePrimaryColor3 = Color(0x6D, 0xD5, 0x8C)
