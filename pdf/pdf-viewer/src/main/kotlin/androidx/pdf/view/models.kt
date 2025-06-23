/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.pdf.view

import android.os.Parcel
import android.text.TextUtils
import androidx.annotation.ColorInt
import androidx.pdf.PdfPoint
import androidx.pdf.PdfRect
import kotlin.Int

/**
 * Writes a [androidx.pdf.PdfRect] to [dest].
 *
 * Not part of the public API because public APIs cannot be [android.os.Parcelable]
 */
internal fun PdfRect.writeToParcel(dest: Parcel) {
    dest.writeInt(pageNum)
    dest.writeFloat(left)
    dest.writeFloat(top)
    dest.writeFloat(right)
    dest.writeFloat(bottom)
}

/**
 * Reads a [PdfRect] from [source].
 *
 * Not part of the public API because public APIs cannot be [android.os.Parcelable]
 */
internal fun pdfRectFromParcel(source: Parcel): PdfRect {
    val pageNum = source.readInt()
    val left = source.readFloat()
    val top = source.readFloat()
    val right = source.readFloat()
    val bottom = source.readFloat()
    return PdfRect(pageNum, left, top, right, bottom)
}

/**
 * Writes a [androidx.pdf.PdfPoint] to [dest].
 *
 * Not part of the public API because public APIs cannot be [android.os.Parcelable]
 */
internal fun PdfPoint.writeToParcel(dest: Parcel) {
    dest.writeInt(pageNum)
    dest.writeFloat(x)
    dest.writeFloat(y)
}

/**
 * Reads a [PdfPoint] from [source].
 *
 * Not part of the public API because public APIs cannot be [android.os.Parcelable]
 */
internal fun pdfPointFromParcel(source: Parcel): PdfPoint {
    val pageNum = source.readInt()
    val x = source.readFloat()
    val y = source.readFloat()
    return PdfPoint(pageNum, x, y)
}

/** Represents an [area] that should be highlighted with [color]. */
public class Highlight(public val area: PdfRect, @ColorInt public val color: Int) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || other !is Highlight) return false

        if (other.area != this.area) return false
        if (other.color != this.color) return false

        return true
    }

    override fun hashCode(): Int {
        var result = area.hashCode()
        result = 31 * result + color.hashCode()
        return result
    }

    override fun toString(): String {
        return "Highlight: area $area color $color"
    }

    /** Deeply copies this [androidx.pdf.view.Highlight] */
    internal fun copy(): Highlight {
        return Highlight(PdfRect(area.pageNum, area.left, area.top, area.right, area.bottom), color)
    }
}

/** Represents PDF content that has been selected */
public interface Selection {
    /**
     * The [PdfRect] bounds of this selection. May contain multiple [PdfRect] if this selection
     * spans multiple discrete areas within the PDF. Consider for example any selection spanning
     * multiple pages, or a text selection spanning multiple lines on the same page.
     */
    public val bounds: List<PdfRect>
}

/** Represents text content that has been selected */
public class TextSelection(public val text: CharSequence, override val bounds: List<PdfRect>) :
    Selection {

    /** Returns [text] as a [String] */
    public fun textAsString(): String = text.toString()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || other !is TextSelection) return false

        if (other.text != this.text) return false
        if (other.bounds != this.bounds) return false

        return true
    }

    override fun hashCode(): Int {
        var result = text.hashCode()
        result = 31 * result + bounds.hashCode()
        return result
    }

    override fun toString(): String {
        return "TextSelection: text $text bounds $bounds"
    }
}

/**
 * Writes a [TextSelection] to [dest].
 *
 * Not part of the public API because public APIs cannot be [android.os.Parcelable]
 */
internal fun TextSelection.writeToParcel(dest: Parcel, flags: Int) {
    TextUtils.writeToParcel(text, dest, flags)
    dest.writeInt(bounds.size)
    for (bound in bounds) {
        bound.writeToParcel(dest)
    }
}

/**
 * Reads a [TextSelection] from [source].
 *
 * Not part of the public API because public APIs cannot be [android.os.Parcelable]
 */
internal fun textSelectionFromParcel(source: Parcel): TextSelection {
    val text = requireNotNull(TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(source))
    val boundsSize = source.readInt()
    val bounds = mutableListOf<PdfRect>()
    for (i in 0 until boundsSize) {
        bounds.add(pdfRectFromParcel(source))
    }
    return TextSelection(text, bounds.toList())
}
