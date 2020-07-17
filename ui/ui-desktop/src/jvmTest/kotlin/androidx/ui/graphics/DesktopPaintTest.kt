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

import androidx.ui.desktop.TestResources.testImageAsset
import androidx.ui.geometry.Offset
import androidx.ui.unit.IntOffset
import androidx.ui.unit.IntSize
import org.junit.Assert.assertEquals
import org.junit.Test

class DesktopPaintTest : DesktopGraphicsTest() {
    private val canvas: Canvas = initCanvas(widthPx = 16, heightPx = 16)

    @Test
    fun initialParameters() {
        val paint = Paint()

        assertEquals(Color.Black, paint.color)
        assertEquals(1f, paint.alpha)
        assertEquals(PaintingStyle.fill, paint.style)
        assertEquals(0f, paint.strokeWidth)
        assertEquals(StrokeCap.butt, paint.strokeCap)
        assertEquals(0f, paint.strokeMiterLimit)
        assertEquals(StrokeJoin.round, paint.strokeJoin)
        assertEquals(true, paint.isAntiAlias)
        assertEquals(FilterQuality.none, paint.filterQuality)
        assertEquals(BlendMode.srcOver, paint.blendMode)
        assertEquals(null, paint.colorFilter)
        assertEquals(null, paint.shader)
    }

    @Test
    fun blendModePlus() {
        canvas.drawRect(left = 0f, top = 0f, right = 16f, bottom = 16f, paint = redPaint)
        canvas.drawRect(left = 4f, top = 4f, right = 12f, bottom = 12f, paint = Paint().apply {
            color = Color.Blue
            blendMode = BlendMode.plus
        })

        screenshotRule.snap(surface)
    }

    @Test
    fun blendModeMultiply() {
        canvas.drawRect(left = 0f, top = 0f, right = 16f, bottom = 16f, paint = redPaint)
        canvas.drawRect(left = 4f, top = 4f, right = 12f, bottom = 12f, paint = Paint().apply {
            color = Color.Gray
            blendMode = BlendMode.multiply
        })

        screenshotRule.snap(surface)
    }

    @Test
    fun colorFilter() {
        canvas.drawRect(left = 0f, top = 0f, right = 16f, bottom = 16f, paint = redPaint)

        canvas.drawImage(
            image = testImageAsset("androidx/ui/desktop/test.png"),
            topLeftOffset = Offset(2f, 4f),
            paint = Paint().apply {
                colorFilter = ColorFilter(Color.Blue, BlendMode.plus)
            }
        )

        screenshotRule.snap(surface)
    }

    @Test
    fun filterQuality() {
        canvas.drawImageRect(
            image = testImageAsset("androidx/ui/desktop/test.png"),
            srcOffset = IntOffset(0, 2),
            srcSize = IntSize(2, 4),
            dstOffset = IntOffset(0, 4),
            dstSize = IntSize(4, 12),
            paint = redPaint
        )
        canvas.drawImageRect(
            image = testImageAsset("androidx/ui/desktop/test.png"),
            srcOffset = IntOffset(0, 2),
            srcSize = IntSize(2, 4),
            dstOffset = IntOffset(4, 4),
            dstSize = IntSize(4, 12),
            paint = redPaint.apply {
                filterQuality = FilterQuality.low
            }
        )
        canvas.drawImageRect(
            image = testImageAsset("androidx/ui/desktop/test.png"),
            srcOffset = IntOffset(0, 2),
            srcSize = IntSize(2, 4),
            dstOffset = IntOffset(8, 4),
            dstSize = IntSize(4, 12),
            paint = redPaint.apply {
                filterQuality = FilterQuality.high
            }
        )

        screenshotRule.snap(surface)
    }

    @Test
    fun linearGradientShader() {
        canvas.drawRect(left = 0f, top = 0f, right = 16f, bottom = 16f, paint = redPaint)

        canvas.drawRect(left = 2f, top = 2f, right = 14f, bottom = 14f, paint = Paint().apply {
            shader = LinearGradientShader(
                from = Offset(0f, 0f),
                to = Offset(6f, 6f),
                colors = listOf(Color.Blue, Color.Green),
                tileMode = TileMode.Mirror
            )
        })

        screenshotRule.snap(surface)
    }

    @Test
    fun linearGradientShaderWithStops() {
        canvas.drawRect(left = 0f, top = 0f, right = 16f, bottom = 16f, paint = redPaint)

        canvas.drawRect(left = 1f, top = 2f, right = 14f, bottom = 15f, paint = Paint().apply {
            shader = LinearGradientShader(
                from = Offset(0f, 0f),
                to = Offset(12f, 0f),
                colorStops = listOf(0f, 0.25f, 1f),
                colors = listOf(Color.Blue, Color.Green, Color.Yellow),
                tileMode = TileMode.Mirror
            )
        })

        screenshotRule.snap(surface)
    }

    @Test
    fun radialGradientShader() {
        canvas.drawRect(left = 0f, top = 0f, right = 16f, bottom = 16f, paint = redPaint)

        canvas.drawRect(left = 2f, top = 2f, right = 14f, bottom = 14f, paint = Paint().apply {
            shader = RadialGradientShader(
                center = Offset(4f, 8f),
                radius = 8f,
                colors = listOf(Color.Blue, Color.Green),
                tileMode = TileMode.Clamp
            )
        })

        screenshotRule.snap(surface)
    }
}
