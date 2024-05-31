/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.compose.ui.text

import android.graphics.Bitmap
import android.graphics.Canvas
import kotlin.math.ceil

internal fun AndroidParagraph.bitmap(): Bitmap {
    val bitmap =
        Bitmap.createBitmap(
            ceil(this.width).toInt(),
            ceil(this.height).toInt(),
            Bitmap.Config.ARGB_8888
        )
    this.paint(androidx.compose.ui.graphics.Canvas(Canvas(bitmap)))
    return bitmap
}
