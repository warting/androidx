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

package androidx.ui.framework.demos

import androidx.compose.Composable
import androidx.ui.core.Modifier
import androidx.ui.graphics.Color
import androidx.ui.graphics.HorizontalGradient
import androidx.ui.graphics.RadialGradient
import androidx.ui.graphics.ScaleFit
import androidx.ui.graphics.SolidColor
import androidx.ui.graphics.TileMode
import androidx.ui.graphics.VerticalGradient
import androidx.ui.graphics.vector.Group
import androidx.ui.graphics.vector.Path
import androidx.ui.graphics.vector.PathBuilder
import androidx.ui.graphics.vector.PathData
import androidx.ui.graphics.vector.VectorScope
import androidx.ui.graphics.vector.drawVector
import androidx.ui.layout.Center
import androidx.ui.layout.Column
import androidx.ui.layout.Container
import androidx.ui.res.loadVectorResource
import androidx.ui.unit.Dp
import androidx.ui.unit.Px
import androidx.ui.unit.dp

@Composable
fun VectorGraphicsDemo() {
    Column {
        val vectorAsset = loadVectorResource(R.drawable.ic_crane)
        vectorAsset.resource.resource?.let {
            Center {
                Container(width = 200.dp, height = 100.dp, modifier = drawVector(it)) {
                }
            }
        }

        Center {
            val width = 120.dp
            val height = 120.dp
            Container(
                width = width,
                height = height,
                modifier = vectorShape(width, height)) {
            }
        }
    }
}

@Composable
private fun vectorShape(width: Dp, height: Dp): Modifier = drawVector(
        name = "vectorShape",
        defaultWidth = width,
        defaultHeight = height,
        scaleFit = ScaleFit.FillMaxDimension
    ) { viewportWidth, viewportHeight ->
    Group(
        scaleX = 0.75f,
        scaleY = 0.75f,
        rotation = 45.0f,
        pivotX = (viewportWidth / 2),
        pivotY = (viewportHeight / 2)
    ) {
        BackgroundPath(viewportWidth, viewportHeight)
        StripePath(viewportWidth, viewportHeight)
        Group(
            translationX = 50.0f,
            translationY = 50.0f,
            pivotX = (viewportWidth / 2),
            pivotY = (viewportHeight / 2),
            rotation = 25.0f
        ) {
            val pathData = PathData {
                moveTo(viewportWidth / 2 - 100, viewportHeight / 2 - 100)
                horizontalLineToRelative(200.0f)
                verticalLineToRelative(200.0f)
                horizontalLineToRelative(-200.0f)
                close()
            }
            Path(
                fill = HorizontalGradient(
                    listOf(
                        Color.Red,
                        Color.Blue
                    ),
                    startX = Px.Zero,
                    endX = Px(viewportWidth / 2 + 100)
                ),
                pathData = pathData
            )
        }
        Triangle()
        TriangleWithOffsets()
    }
}

@Composable
private fun VectorScope.BackgroundPath(vectorWidth: Float, vectorHeight: Float) {
    val background = PathData {
        horizontalLineTo(vectorWidth)
        verticalLineTo(vectorHeight)
        horizontalLineTo(0.0f)
        close()
    }

    Path(
        fill = VerticalGradient(
            0.0f to Color.Cyan,
            0.3f to Color.Green,
            1.0f to Color.Magenta,
            startY = Px.Zero,
            endY = Px(vectorHeight)
        ),
        pathData = background
    )
}

@Composable
private fun VectorScope.Triangle() {
    val length = 150.0f
    Path(
        fill = RadialGradient(
            listOf(
                Color(0xFF000080),
                Color(0xFF808000),
                Color(0xFF008080)
            ),
            centerX = length / 2.0f,
            centerY = length / 2.0f,
            radius = length / 2.0f,
            tileMode = TileMode.Repeated
        ),
        pathData = PathData {
            verticalLineTo(length)
            horizontalLineTo(length)
            close()
        }
    )
}

@Composable
private fun VectorScope.TriangleWithOffsets() {

    val side1 = 150.0f
    val side2 = 150.0f
    Path(
        fill = RadialGradient(
            0.0f to Color(0xFF800000),
            0.3f to Color.Cyan,
            0.8f to Color.Yellow,
            centerX = side1 / 2.0f,
            centerY = side2 / 2.0f,
            radius = side1 / 2.0f
        ),
        pathData = PathData {
            horizontalLineToRelative(side1)
            verticalLineToRelative(side2)
            close()
        }
    )
}

@Composable
private fun VectorScope.StripePath(vectorWidth: Float, vectorHeight: Float) {
    val stripeDelegate = PathData {
        stripe(vectorWidth, vectorHeight, 10)
    }

    Path(stroke = SolidColor(Color.Blue), pathData = stripeDelegate)
}

private fun PathBuilder.stripe(vectorWidth: Float, vectorHeight: Float, numLines: Int) {
    val stepSize = vectorWidth / numLines
    var currentStep = stepSize
    for (i in 1..numLines) {
        moveTo(currentStep, 0.0f)
        verticalLineTo(vectorHeight)
        currentStep += stepSize
    }
}
