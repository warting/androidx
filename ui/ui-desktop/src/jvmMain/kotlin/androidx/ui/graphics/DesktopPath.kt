/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.ui.graphics

import androidx.ui.geometry.Offset
import androidx.ui.geometry.RRect
import androidx.ui.geometry.Rect
import androidx.ui.graphics.vectormath.degrees
import org.jetbrains.skija.Matrix33
import org.jetbrains.skija.PathDirection
import org.jetbrains.skija.PathFillMode

/**
 * @Throws UnsupportedOperationException if this Path is not backed by an org.jetbrains.skija.Path
 */
@Suppress("NOTHING_TO_INLINE")
inline fun Path.asDesktopPath(): org.jetbrains.skija.Path =
    if (this is DesktopPath) {
        internalPath
    } else {
        throw UnsupportedOperationException("Unable to obtain org.jetbrains.skija.Path")
    }

class DesktopPath(
    val internalPath: org.jetbrains.skija.Path = org.jetbrains.skija.Path()
) : Path {
    private val radii = FloatArray(8)

    override var fillType: PathFillType
        get() {
            if (internalPath.fillMode == PathFillMode.EVEN_ODD) {
                return PathFillType.evenOdd
            } else {
                return PathFillType.nonZero
            }
        }

        set(value) {
            internalPath.fillMode =
                if (value == PathFillType.evenOdd) {
                    PathFillMode.EVEN_ODD
                } else {
                    PathFillMode.WINDING
                }
        }

    override fun moveTo(x: Float, y: Float) {
        internalPath.moveTo(x, y)
    }

    override fun relativeMoveTo(dx: Float, dy: Float) {
        internalPath.rMoveTo(dx, dy)
    }

    override fun lineTo(x: Float, y: Float) {
        internalPath.lineTo(x, y)
    }

    override fun relativeLineTo(dx: Float, dy: Float) {
        internalPath.rLineTo(dx, dy)
    }

    override fun quadraticBezierTo(x1: Float, y1: Float, x2: Float, y2: Float) {
        internalPath.quadTo(x1, y1, x2, y2)
    }

    override fun relativeQuadraticBezierTo(dx1: Float, dy1: Float, dx2: Float, dy2: Float) {
        internalPath.rQuadTo(dx1, dy1, dx2, dy2)
    }

    override fun cubicTo(x1: Float, y1: Float, x2: Float, y2: Float, x3: Float, y3: Float) {
        internalPath.cubicTo(
            x1, y1,
            x2, y2,
            x3, y3
        )
    }

    override fun relativeCubicTo(
        dx1: Float,
        dy1: Float,
        dx2: Float,
        dy2: Float,
        dx3: Float,
        dy3: Float
    ) {
        internalPath.rCubicTo(
            dx1, dy1,
            dx2, dy2,
            dx3, dy3
        )
    }

    override fun arcTo(
        rect: Rect,
        startAngleDegrees: Float,
        sweepAngleDegrees: Float,
        forceMoveTo: Boolean
    ) {
        internalPath.arcTo(
            rect.toSkija(),
            startAngleDegrees,
            sweepAngleDegrees,
            forceMoveTo
        )
    }

    override fun addRect(rect: Rect) {
        internalPath.addRect(rect.toSkija(), PathDirection.COUNTER_CLOCKWISE)
    }

    override fun addOval(oval: Rect) {
        internalPath.addOval(oval.toSkija(), PathDirection.COUNTER_CLOCKWISE)
    }

    override fun addArcRad(oval: Rect, startAngleRadians: Float, sweepAngleRadians: Float) {
        addArc(oval, degrees(startAngleRadians), degrees(sweepAngleRadians))
    }

    override fun addArc(oval: Rect, startAngleDegrees: Float, sweepAngleDegrees: Float) {
        internalPath.addArc(oval.toSkija(), startAngleDegrees, sweepAngleDegrees)
    }

    override fun addRRect(rrect: RRect) {
        radii[0] = rrect.topLeftRadiusX
        radii[1] = rrect.topLeftRadiusY

        radii[2] = rrect.topRightRadiusX
        radii[3] = rrect.topRightRadiusY

        radii[4] = rrect.bottomRightRadiusX
        radii[5] = rrect.bottomRightRadiusY

        radii[6] = rrect.bottomLeftRadiusX
        radii[7] = rrect.bottomLeftRadiusY

        internalPath.addRRect(
            org.jetbrains.skija.RRect.makeComplexLTRB(
                rrect.left, rrect.top, rrect.right, rrect.bottom, radii
            ),
            PathDirection.COUNTER_CLOCKWISE
        )
    }

    override fun addPath(path: Path, offset: Offset) {
        internalPath.addPath(path.asDesktopPath(), offset.x, offset.y)
    }

    override fun close() {
        internalPath.closePath()
    }

    override fun reset() {
        internalPath.reset()
    }

    override fun shift(offset: Offset) {
        internalPath.transform(Matrix33.makeTranslate(offset.x, offset.y))
    }

    override fun getBounds(): Rect {
        val bounds = internalPath.bounds
        return Rect(
            bounds.left,
            bounds.top,
            bounds.right,
            bounds.bottom
        )
    }

    override fun op(
        path1: Path,
        path2: Path,
        operation: PathOperation
    ): Boolean {
        println("Path.op not implemented yet")
        return false
    }

    override val isConvex: Boolean get() = internalPath.isConvex

    override val isEmpty: Boolean get() = internalPath.isEmpty
}