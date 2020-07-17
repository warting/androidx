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

package androidx.ui.graphics

import androidx.compose.InternalComposeApi
import androidx.ui.geometry.Offset
import androidx.ui.geometry.Rect
import androidx.ui.graphics.vectormath.Matrix4
import androidx.ui.graphics.vectormath.isIdentity
import androidx.ui.unit.IntOffset
import androidx.ui.unit.IntSize
import androidx.ui.util.fastForEach
import org.jetbrains.skija.ClipMode as SkijaClipMode
import org.jetbrains.skija.IRect as SkijaIRect
import org.jetbrains.skija.Matrix33 as SkijaMatrix33
import org.jetbrains.skija.RRect as SkijaRRect
import org.jetbrains.skija.Rect as SkijaRect

@Suppress("UNUSED_PARAMETER")
fun DesktopCanvas(image: ImageAsset): Canvas {
    println("Canvas(image: ImageAsset) isn't implemented yet")
    return DesktopCanvas(android.graphics.Canvas())
}

@OptIn(InternalComposeApi::class)
internal class DesktopInternalCanvasHolder : InternalCanvasHolder {
    private val _canvas = DesktopCanvas(android.graphics.Canvas())
    override val canvas: Canvas get() = _canvas

    override var internalCanvas: NativeCanvas
        get() = _canvas.nativeCanvas
        set(value) {
            _canvas.nativeCanvas = value
        }
}

class DesktopCanvas(override var nativeCanvas: NativeCanvas) : Canvas {
    private val skija get() = nativeCanvas.skijaCanvas
    private val Paint.skija get() = asFrameworkPaint().skijaPaint

    override fun save() {
        skija.save()
    }

    override fun restore() {
        skija.restore()
    }

    override fun saveLayer(bounds: Rect, paint: Paint) {
        skija.saveLayer(
            bounds.left,
            bounds.top,
            bounds.right,
            bounds.bottom,
            paint.skija
        )
    }

    override fun translate(dx: Float, dy: Float) {
        skija.translate(dx, dy)
    }

    override fun scale(sx: Float, sy: Float) {
        skija.scale(sx, sy)
    }

    override fun rotate(degrees: Float) {
        skija.rotate(degrees)
    }

    override fun skew(sx: Float, sy: Float) {
        skija.skew(sx, sy)
    }

    /**
     * @throws IllegalStateException if an arbitrary transform is provided
     */
    override fun concat(matrix4: Matrix4) {
        if (!matrix4.isIdentity()) {
            if (matrix4[2, 0] != 0f ||
                matrix4[2, 1] != 0f ||
                matrix4[2, 0] != 0f ||
                matrix4[2, 1] != 0f ||
                matrix4[2, 2] != 1f ||
                matrix4[2, 3] != 0f ||
                matrix4[3, 2] != 0f) {
                throw IllegalStateException("Desktop does not support arbitrary transforms")
            }
            skija.concat(
                SkijaMatrix33(
                    matrix4[0, 0],
                    matrix4[1, 0],
                    matrix4[3, 0],
                    matrix4[0, 1],
                    matrix4[1, 1],
                    matrix4[3, 1],
                    matrix4[0, 3],
                    matrix4[1, 3],
                    matrix4[3, 3]
                )
            )
        }
    }

    override fun clipRect(left: Float, top: Float, right: Float, bottom: Float, clipOp: ClipOp) {
        skija.clipRect(SkijaRect.makeLTRB(left, top, right, bottom), clipOp.toSkija())
    }

    override fun clipPath(path: Path, clipOp: ClipOp) {
        skija.clipPath(path.asDesktopPath(), clipOp.toSkija())
    }

    override fun drawLine(p1: Offset, p2: Offset, paint: Paint) {
        skija.drawLine(p1.x, p1.y, p2.x, p2.y, paint.skija)
    }

    override fun drawRect(left: Float, top: Float, right: Float, bottom: Float, paint: Paint) {
        skija.drawRect(SkijaRect.makeLTRB(left, top, right, bottom), paint.skija)
    }

    override fun drawRoundRect(
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        radiusX: Float,
        radiusY: Float,
        paint: Paint
    ) {
        skija.drawRRect(
            SkijaRRect.makeLTRB(
                left,
                top,
                right,
                bottom,
                radiusX,
                radiusY
            ),
            paint.skija
        )
    }

    override fun drawOval(left: Float, top: Float, right: Float, bottom: Float, paint: Paint) {
        skija.drawOval(SkijaRect.makeLTRB(left, top, right, bottom), paint.skija)
    }

    override fun drawCircle(center: Offset, radius: Float, paint: Paint) {
        skija.drawCircle(center.x, center.y, radius, paint.skija)
    }

    override fun drawArc(
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        startAngle: Float,
        sweepAngle: Float,
        useCenter: Boolean,
        paint: Paint
    ) {
        skija.drawArc(
            left,
            top,
            right,
            bottom,
            startAngle,
            sweepAngle,
            useCenter,
            paint.skija
        )
    }

    override fun drawPath(path: Path, paint: Paint) {
        skija.drawPath(path.asDesktopPath(), paint.skija)
    }

    override fun drawImage(image: ImageAsset, topLeftOffset: Offset, paint: Paint) {
        skija.drawImageRect(
            image.asAndroidBitmap().skiaImage,
            SkijaIRect.makeXYWH(
                0,
                0,
                image.width,
                image.height
            ),
            SkijaRect.makeXYWH(
                topLeftOffset.x,
                topLeftOffset.y,
                image.width.toFloat(),
                image.height.toFloat()
            ),
            paint.skija
        )
    }

    override fun drawImageRect(
        image: ImageAsset,
        srcOffset: IntOffset,
        srcSize: IntSize,
        dstOffset: IntOffset,
        dstSize: IntSize,
        paint: Paint
    ) {
        skija.drawImageRect(
            image.asAndroidBitmap().skiaImage,
            SkijaIRect.makeXYWH(
                srcOffset.x,
                srcOffset.y,
                srcSize.width,
                srcSize.height
            ),
            SkijaRect.makeXYWH(
                dstOffset.x.toFloat(),
                dstOffset.y.toFloat(),
                dstSize.width.toFloat(),
                dstSize.height.toFloat()
            ),
            paint.skija
        )
    }

    override fun drawPoints(pointMode: PointMode, points: List<Offset>, paint: Paint) {
        when (pointMode) {
            // Draw a line between each pair of points, each point has at most one line
            // If the number of points is odd, then the last point is ignored.
            PointMode.lines -> drawLines(points, paint, 2)

            // Connect each adjacent point with a line
            PointMode.polygon -> drawLines(points, paint, 1)

            // Draw a point at each provided coordinate
            PointMode.points -> drawPoints(points, paint)
        }
    }

    override fun enableZ() = Unit

    override fun disableZ() = Unit

    private fun drawPoints(points: List<Offset>, paint: Paint) {
        points.fastForEach { point ->
            skija.drawPoint(
                point.x,
                point.y,
                paint.skija
            )
        }
    }

    /**
     * Draw lines connecting points based on the corresponding step.
     *
     * ex. 3 points with a step of 1 would draw 2 lines between the first and second points
     * and another between the second and third
     *
     * ex. 4 points with a step of 2 would draw 2 lines between the first and second and another
     * between the third and fourth. If there is an odd number of points, the last point is
     * ignored
     *
     * @see drawRawLines
     */
    private fun drawLines(points: List<Offset>, paint: Paint, stepBy: Int) {
        if (points.size >= 2) {
            for (i in 0 until points.size - 1 step stepBy) {
                val p1 = points[i]
                val p2 = points[i + 1]
                skija.drawLine(
                    p1.x,
                    p1.y,
                    p2.x,
                    p2.y,
                    paint.skija
                )
            }
        }
    }

    /**
     * @throws IllegalArgumentException if a non even number of points is provided
     */
    override fun drawRawPoints(pointMode: PointMode, points: FloatArray, paint: Paint) {
        if (points.size % 2 != 0) {
            throw IllegalArgumentException("points must have an even number of values")
        }
        when (pointMode) {
            PointMode.lines -> drawRawLines(points, paint, 2)
            PointMode.polygon -> drawRawLines(points, paint, 1)
            PointMode.points -> drawRawPoints(points, paint, 2)
        }
    }

    private fun drawRawPoints(points: FloatArray, paint: Paint, stepBy: Int) {
        if (points.size % 2 == 0) {
            for (i in 0 until points.size - 1 step stepBy) {
                val x = points[i]
                val y = points[i + 1]
                skija.drawPoint(x, y, paint.skija)
            }
        }
    }

    /**
     * Draw lines connecting points based on the corresponding step. The points are interpreted
     * as x, y coordinate pairs in alternating index positions
     *
     * ex. 3 points with a step of 1 would draw 2 lines between the first and second points
     * and another between the second and third
     *
     * ex. 4 points with a step of 2 would draw 2 lines between the first and second and another
     * between the third and fourth. If there is an odd number of points, the last point is
     * ignored
     *
     * @see drawLines
     */
    private fun drawRawLines(points: FloatArray, paint: Paint, stepBy: Int) {
        // Float array is treated as alternative set of x and y coordinates
        // x1, y1, x2, y2, x3, y3, ... etc.
        if (points.size >= 4 && points.size % 2 == 0) {
            for (i in 0 until points.size - 3 step stepBy * 2) {
                val x1 = points[i]
                val y1 = points[i + 1]
                val x2 = points[i + 2]
                val y2 = points[i + 3]
                skija.drawLine(
                    x1,
                    y1,
                    x2,
                    y2,
                    paint.skija
                )
            }
        }
    }

    override fun drawVertices(vertices: Vertices, blendMode: BlendMode, paint: Paint) {
        println("Canvas.drawVertices not implemented yet")
    }

    private fun ClipOp.toSkija() = when (this) {
        ClipOp.difference -> SkijaClipMode.DIFFERENCE
        ClipOp.intersect -> SkijaClipMode.INTERSECT
    }
}