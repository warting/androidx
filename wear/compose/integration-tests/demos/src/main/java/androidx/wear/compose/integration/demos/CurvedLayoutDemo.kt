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

package androidx.wear.compose.integration.demos

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.paint
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.AnchorType
import androidx.wear.compose.foundation.CurvedAlignment
import androidx.wear.compose.foundation.CurvedDirection
import androidx.wear.compose.foundation.CurvedLayout
import androidx.wear.compose.foundation.CurvedModifier
import androidx.wear.compose.foundation.CurvedScope
import androidx.wear.compose.foundation.CurvedTextStyle
import androidx.wear.compose.foundation.angularSize
import androidx.wear.compose.foundation.angularSizeDp
import androidx.wear.compose.foundation.background
import androidx.wear.compose.foundation.basicCurvedText
import androidx.wear.compose.foundation.curvedBox
import androidx.wear.compose.foundation.curvedColumn
import androidx.wear.compose.foundation.curvedComposable
import androidx.wear.compose.foundation.curvedRow
import androidx.wear.compose.foundation.padding
import androidx.wear.compose.foundation.sizeIn
import androidx.wear.compose.foundation.weight
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.ToggleButton
import androidx.wear.compose.material.curvedText

@Composable
fun CurvedWorldDemo() {
    CurvedLayout(modifier = Modifier.fillMaxSize()) {
        curvedComposable { Box(modifier = Modifier.size(20.dp).background(Color.Red)) }
        curvedComposable {
            Column(
                modifier = Modifier.background(Color.Gray).padding(3.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "A",
                    color = Color.Black,
                    fontSize = 16.sp,
                    modifier = Modifier.background(Color.Blue),
                )
                Row {
                    Text(
                        text = "B",
                        color = Color.Black,
                        fontSize = 16.sp,
                        modifier = Modifier.background(Color.Green).padding(2.dp),
                    )
                    Text(
                        text = "C",
                        color = Color.Black,
                        fontSize = 16.sp,
                        modifier = Modifier.background(Color.Red),
                    )
                }
            }
        }
        curvedComposable { Box(modifier = Modifier.size(20.dp).background(Color.Red)) }
    }
    CurvedLayout(
        anchor = 90F,
        anchorType = AnchorType.Start,
        angularDirection = CurvedDirection.Angular.Reversed,
    ) {
        curvedComposable {
            Text(
                text = "Start",
                color = Color.Black,
                fontSize = 30.sp,
                modifier = Modifier.background(Color.White).padding(horizontal = 10.dp),
            )
        }
    }
    CurvedLayout(
        anchor = 90F,
        anchorType = AnchorType.End,
        angularDirection = CurvedDirection.Angular.Reversed,
    ) {
        curvedComposable {
            Text(
                text = "End",
                color = Color.Black,
                fontSize = 30.sp,
                modifier = Modifier.background(Color.White).padding(horizontal = 10.dp),
            )
        }
    }
    CurvedLayout(
        modifier = Modifier.padding(50.dp),
        anchor = 90f,
        anchorType = AnchorType.Center,
        angularDirection = CurvedDirection.Angular.Reversed,
    ) {
        listOf("A", "B", "C").forEach {
            curvedComposable {
                Text(
                    text = "$it",
                    color = Color.Black,
                    fontSize = 30.sp,
                    modifier = Modifier.background(Color.White).padding(horizontal = 10.dp),
                )
            }
        }
    }
}

private fun CurvedScope.SeparatorBlock() {
    curvedComposable(radialAlignment = CurvedAlignment.Radial.Outer) {
        Box(modifier = Modifier.size(10.dp, 40.dp).background(Color.Gray))
    }
}

private fun CurvedScope.RgbBlocks() {
    curvedComposable(radialAlignment = CurvedAlignment.Radial.Outer) {
        Box(modifier = Modifier.size(20.dp).background(Color.Red))
    }
    curvedComposable(radialAlignment = CurvedAlignment.Radial.Center) {
        Box(modifier = Modifier.size(20.dp).background(Color.Green))
    }
    curvedComposable(radialAlignment = CurvedAlignment.Radial.Inner) {
        Box(modifier = Modifier.size(20.dp).background(Color.Blue))
    }
}

@Composable
fun CurvedRowAlignmentDemo() {
    CurvedLayout(modifier = Modifier.fillMaxSize()) {
        SeparatorBlock()
        RgbBlocks()
        SeparatorBlock()
        (0..10).forEach {
            curvedComposable(radialAlignment = CurvedAlignment.Radial.Custom(it / 10.0f)) {
                Box(modifier = Modifier.size(10.dp).background(Color.White))
            }
        }
        SeparatorBlock()
    }
    CurvedLayout(anchor = 90f, angularDirection = CurvedDirection.Angular.Reversed) {
        SeparatorBlock()
        RgbBlocks()
        SeparatorBlock()
    }
}

@Composable
fun BasicCurvedTextDemo() {
    CurvedLayout(modifier = Modifier.fillMaxSize().background(Color.White)) {
        SeparatorBlock()
        basicCurvedText(
            "Curved Text",
            CurvedTextStyle(fontSize = 18.sp),
            // TODO: Re-add when we implement alignment modifiers.
            // modifier = Modifier.radialAlignment(RadialAlignment.Outer)
        )
        SeparatorBlock()
        basicCurvedText(
            "And More",
            CurvedTextStyle(fontSize = 24.sp),
            angularDirection = CurvedDirection.Angular.Reversed,
            modifier = CurvedModifier.padding(angular = 5.dp),
            // TODO: Re-add when we implement alignment modifiers.
            // modifier = Modifier.radialAlignment(RadialAlignment.Inner)
        )
        SeparatorBlock()
    }
}

@Composable
fun CurvedEllipsis() {
    CurvedLayout {
        curvedRow(modifier = CurvedModifier.sizeIn(maxSweepDegrees = 90f)) {
            curvedText(
                "This text too long to actually fit in the provided space",
                modifier = CurvedModifier.weight(1f),
                overflow = TextOverflow.Ellipsis,
            )
            curvedText("10:00")
        }
    }
}

@Composable
fun CurvedLayoutDirection() {
    var layoutDirection by remember { mutableStateOf(false) }
    val actualLayoutDirection = if (layoutDirection) LayoutDirection.Rtl else LayoutDirection.Ltr
    CompositionLocalProvider(LocalLayoutDirection provides actualLayoutDirection) {
        Box {
            Row(modifier = Modifier.align(Alignment.Center)) {
                Text("LayoutDirection: ")
                ToggleButton(
                    checked = layoutDirection,
                    onCheckedChange = { layoutDirection = !layoutDirection },
                ) {
                    Text(if (layoutDirection) "Rtl" else "Ltr")
                }
            }
            repeat(2) { topDown ->
                CurvedLayout(
                    anchor = listOf(270f, 90f)[topDown],
                    angularDirection =
                        listOf(CurvedDirection.Angular.Normal, CurvedDirection.Angular.Reversed)[
                            topDown],
                ) {
                    curvedRow(CurvedModifier.background(Color.White)) {
                        basicCurvedText(
                            "Before",
                            CurvedTextStyle(fontSize = 24.sp),
                            modifier = CurvedModifier.padding(angular = 5.dp),
                        )
                        curvedColumn { repeat(3) { basicCurvedText("#$it") } }
                        curvedRow {
                            curvedComposable {
                                Text(
                                    "after",
                                    modifier = Modifier.padding(4.dp),
                                    color = Color.Black,
                                )
                            }
                            basicCurvedText(
                                "end",
                                modifier = CurvedModifier.padding(angular = 4.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CurvedBoxDemo() {
    CurvedLayout(modifier = Modifier.fillMaxSize(), anchor = 90f) {
        curvedBox(
            modifier = CurvedModifier.background(Color.Red),
            radialAlignment = CurvedAlignment.Radial.Inner,
            angularAlignment = CurvedAlignment.Angular.End,
        ) {
            curvedComposable {
                Box(modifier = Modifier.width(60.dp).height(40.dp).background(Color.Green))
            }
            curvedComposable { WhiteCircle() }
        }
    }
    CurvedLayout(modifier = Modifier.fillMaxSize(), anchor = 180f) {
        curvedBox(modifier = CurvedModifier.background(Color.Red)) {
            curvedComposable { Box(modifier = Modifier.size(60.dp).background(Color.Green)) }
            curvedComposable { WhiteCircle() }
        }
    }
    CurvedLayout(modifier = Modifier.fillMaxSize()) {
        curvedBox(
            modifier = CurvedModifier.background(Color.Red),
            radialAlignment = CurvedAlignment.Radial.Outer,
            angularAlignment = CurvedAlignment.Angular.Start,
        ) {
            curvedComposable {
                Box(modifier = Modifier.width(40.dp).height(60.dp).background(Color.Green))
            }
            curvedComposable { WhiteCircle() }
        }
    }
}

@Composable
private fun SampleIcon(
    @DrawableRes id: Int,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    background: Color = Color.Black,
) {
    Box(
        modifier
            .size(40.dp)
            .border(2.dp, Color.White, CircleShape)
            .clip(CircleShape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .background(background, CircleShape)
            .padding(3.dp)
            .paint(painterResource(id), contentScale = ContentScale.Fit)
    )
}

@Composable
fun CurvedIconsDemo() {
    Box(Modifier.fillMaxSize().padding(10.dp), contentAlignment = Alignment.Center) {
        CurvedLayout(
            modifier = Modifier.fillMaxSize(),
            anchor = 90f,
            angularDirection = CurvedDirection.Angular.CounterClockwise,
        ) {
            curvedComposable(rotationLocked = true) { Text("Foo", color = Color.White) }
            listOf(R.drawable.ic_skip_previous, R.drawable.ic_play, R.drawable.ic_skip_next)
                .forEach {
                    curvedComposable(
                        modifier = CurvedModifier.angularSize(40f),
                        rotationLocked = true,
                    ) {
                        SampleIcon(it)
                    }
                }
            curvedComposable(rotationLocked = true) { Text("Bar", color = Color.White) }
        }
    }
}

@Composable
private fun WhiteCircle() {
    Box(modifier = Modifier.size(30.dp).clip(CircleShape).background(Color.White))
}

@Composable
fun CurvedSpacingEmDemo() {
    val style = CurvedTextStyle(MaterialTheme.typography.body1)
    repeat(2) {
        CurvedLayout(
            anchor = if (it == 0) 270f else 90f,
            angularDirection =
                if (it == 0) CurvedDirection.Angular.Clockwise
                else CurvedDirection.Angular.CounterClockwise,
            modifier = Modifier.size(300.dp),
        ) {
            listOf(-0.1f, 0f, 0.05f, 0.1f, 0.15f).forEachIndexed { ix, spacing ->
                if (ix > 0) {
                    curvedBox(modifier = CurvedModifier.angularSizeDp(10.dp)) {}
                }
                basicCurvedText(
                    "| $spacing em |",
                    style =
                        style.copy(
                            letterSpacing = spacing.em,
                            letterSpacingCounterClockwise = spacing.em,
                        ),
                    modifier =
                        CurvedModifier.background(if (ix % 2 == 0) Color.DarkGray else Color.Gray),
                )
            }
        }
    }
}

@Composable
fun CurvedSpacingSpDemo() {
    val style = CurvedTextStyle(MaterialTheme.typography.body1)
    repeat(2) {
        CurvedLayout(
            anchor = if (it == 0) 270f else 90f,
            angularDirection =
                if (it == 0) CurvedDirection.Angular.Clockwise
                else CurvedDirection.Angular.CounterClockwise,
            modifier = Modifier.size(300.dp),
        ) {
            listOf(-1f, 0f, 1f, 2f).forEachIndexed { ix, spacing ->
                if (ix > 0) {
                    curvedBox(modifier = CurvedModifier.angularSizeDp(10.dp)) {}
                }
                basicCurvedText(
                    "| $spacing sp |",
                    style =
                        style.copy(
                            letterSpacing = spacing.sp,
                            letterSpacingCounterClockwise = spacing.sp,
                        ),
                    modifier =
                        CurvedModifier.background(if (ix % 2 == 0) Color.DarkGray else Color.Gray),
                )
            }
        }
    }
}
