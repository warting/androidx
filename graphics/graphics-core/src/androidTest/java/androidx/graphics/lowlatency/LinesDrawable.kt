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

package androidx.graphics.lowlatency

import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable

class LinesDrawable : Drawable() {

    private var mLines: FloatArray? = null
    private val mPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeWidth = 5f }

    var strokeWidth: Float
        get() = mPaint.strokeWidth
        set(value) {
            mPaint.strokeWidth = value
        }

    override fun draw(canvas: Canvas) {
        mLines?.let { lines ->
            for (i in lines.indices step 4) {
                canvas.drawLine(lines[i], lines[i + 1], lines[i + 2], lines[i + 3], mPaint)
            }
        }
    }

    fun setLines(lines: FloatArray) {
        mLines = lines
        invalidateSelf()
    }

    fun setColor(color: Int) {
        mPaint.color = color
        invalidateSelf()
    }

    override fun setAlpha(alpha: Int) {
        mPaint.alpha = alpha
        invalidateSelf()
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        mPaint.colorFilter = colorFilter
        invalidateSelf()
    }

    @Suppress("OVERRIDE_DEPRECATION") // b/407504449
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
}
