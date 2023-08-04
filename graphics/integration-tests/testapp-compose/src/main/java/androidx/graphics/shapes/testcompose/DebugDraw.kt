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

package androidx.graphics.shapes.testcompose

import android.graphics.Path
import android.graphics.PointF
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.graphics.shapes.Cubic
import androidx.graphics.shapes.CubicShape
import androidx.graphics.shapes.Morph

internal fun DrawScope.debugDraw(morph: Morph, progress: Float) =
    debugDraw(morph.asCubics(progress), morph.asPath(progress))

internal fun DrawScope.debugDraw(cubicShape: CubicShape) =
    debugDraw(cubicShape.cubics, cubicShape.toPath())

internal fun DrawScope.debugDraw(cubics: List<Cubic>, path: Path) {
    drawPath(path.asComposePath(), Color.Green, style = Stroke(2f))

    for (bezier in cubics) {
        // Draw red circles for start and end.
        drawCircle(bezier.anchor0X, bezier.anchor0Y, 6f, Color.Red, strokeWidth = 2f)
        drawCircle(bezier.anchor1X, bezier.anchor1Y, 8f, Color.Magenta, strokeWidth = 2f)
        // Draw a circle for the first control point, and a line from start to it.
        // The curve will start in this direction

        drawLine(bezier.anchor0X, bezier.anchor0Y, bezier.control0X, bezier.control0Y, Color.Yellow,
            strokeWidth = 0f)
        drawCircle(bezier.control0X, bezier.control0Y, 4f, Color.Yellow, strokeWidth = 2f)
        // Draw a circle for the second control point, and a line from it to the end.
        // The curve will end in this direction
        drawLine(bezier.control1X, bezier.control1Y, bezier.anchor1X, bezier.anchor1Y, Color.Yellow,
            strokeWidth = 0f)
        drawCircle(bezier.control1X, bezier.control1Y, 4f, Color.Yellow, strokeWidth = 2f)
    }
}

/**
 * Utility extension functions to bridge OffsetF as points to Compose's Offsets.
 */
private fun PointF.asOffset() = Offset(x, y)

private fun DrawScope.drawCircle(
    center: PointF,
    radius: Float,
    color: Color,
    strokeWidth: Float = 2f
) {
    drawCircle(color, radius, center.asOffset(), style = Stroke(strokeWidth))
}

private fun DrawScope.drawCircle(
    centerX: Float,
    centerY: Float,
    radius: Float,
    color: Color,
    strokeWidth: Float = 2f
) {
    drawCircle(color, radius, Offset(centerX, centerY), style = Stroke(strokeWidth))
}

private fun DrawScope.drawLine(start: PointF, end: PointF, color: Color, strokeWidth: Float = 2f) {
    drawLine(color, start.asOffset(), end.asOffset(), strokeWidth = strokeWidth)
}

private fun DrawScope.drawLine(
    startX: Float,
    startY: Float,
    endX: Float,
    endY: Float,
    color: Color,
    strokeWidth: Float = 2f
) {
    drawLine(color, Offset(startX, startY), Offset(endX, endY), strokeWidth = strokeWidth)
}
