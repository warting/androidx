/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.compose.ui.text.android.style

import android.graphics.Paint.FontMetricsInt
import androidx.compose.ui.text.android.InternalPlatformTextApi
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.LineHeightStyle.Mode.Companion.Fixed
import androidx.compose.ui.text.style.LineHeightStyle.Mode.Companion.Minimum
import androidx.compose.ui.text.style.LineHeightStyle.Mode.Companion.Tight
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import kotlin.math.abs
import kotlin.math.ceil
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

private const val SingleLineStartIndex = 0
private const val SingleLineEndIndex = 1
private const val MultiLineStartIndex = 0
private const val MultiLineEndIndex = 3

@OptIn(InternalPlatformTextApi::class)
@RunWith(Parameterized::class)
@SmallTest
class LineHeightStyleSpanTest(val modeInt: Int) {

    // Parameterized doesn't work with value classes directly.
    val mode: LineHeightStyle.Mode
        get() =
            when (modeInt) {
                0 -> Fixed
                1 -> Minimum
                else -> Tight
            }

    companion object {
        @Parameterized.Parameters @JvmStatic fun parameters() = listOf(0, 1, 2)
    }

    @Test
    fun negative_line_height_does_not_change_the_values() {
        val fontMetrics = FontMetricsInt(ascent = 1, descent = 1)

        val newFontMetrics =
            runSingleLine(
                topRatio = 0.5f,
                trimFirstLineTop = false,
                trimLastLineBottom = false,
                fontMetrics = fontMetrics,
                mode = mode,
                tallerLineHeight = true,
            )

        assertThat(newFontMetrics.ascent).isEqualTo(fontMetrics.ascent)
        assertThat(newFontMetrics.descent).isEqualTo(fontMetrics.descent)
    }

    /* single line, top percentage 0 */

    @Test
    fun singleLine_topRatio_0_trimFirstLineTop_false_trimLastLineBottom_false() {
        val fontMetrics = createFontMetrics()

        val newFontMetrics =
            runSingleLine(
                topRatio = 0f,
                trimFirstLineTop = false,
                trimLastLineBottom = false,
                fontMetrics = fontMetrics,
                mode = mode,
                tallerLineHeight = true,
            )

        assertThat(newFontMetrics.ascent).isEqualTo(fontMetrics.ascent)
        assertThat(newFontMetrics.descent).isEqualTo(fontMetrics.descent + fontMetrics.lineHeight())
    }

    @Test
    fun singleLine_topRatio_0_trimFirstLineTop_false_trimLastLineBottom_true() {
        val fontMetrics = createFontMetrics()

        val newFontMetrics =
            runSingleLine(
                topRatio = 0f,
                trimFirstLineTop = false,
                trimLastLineBottom = true,
                fontMetrics = fontMetrics,
                mode = mode,
                tallerLineHeight = true,
            )

        assertThat(newFontMetrics.ascent).isEqualTo(fontMetrics.ascent)
        assertThat(newFontMetrics.descent).isEqualTo(fontMetrics.descent)
    }

    @Test
    fun singleLine_topRatio_0_trimFirstLineTop_true_trimLastLineBottom_false_taller() {
        val fontMetrics = createFontMetrics()

        val newFontMetrics =
            runSingleLine(
                topRatio = 0f,
                trimFirstLineTop = true,
                trimLastLineBottom = false,
                fontMetrics = fontMetrics,
                mode = mode,
                tallerLineHeight = true,
            )

        assertThat(newFontMetrics.ascent).isEqualTo(fontMetrics.ascent)
        assertThat(newFontMetrics.descent).isEqualTo(fontMetrics.descent + fontMetrics.lineHeight())
    }

    @Test
    fun singleLine_topRatio_0_trimFirstLineTop_true_trimLastLineBottom_false_shorter() {
        val fontMetrics = createFontMetrics()

        val newFontMetrics =
            runSingleLine(
                topRatio = 0f,
                trimFirstLineTop = true,
                trimLastLineBottom = false,
                fontMetrics = fontMetrics,
                mode = mode,
                tallerLineHeight = true,
            )

        assertThat(newFontMetrics.ascent).isEqualTo(fontMetrics.ascent)
        assertThat(newFontMetrics.descent).isEqualTo(fontMetrics.descent + fontMetrics.lineHeight())
    }

    @Test
    fun singleLine_topRatio_0f_trimFirstLineTop_true_trimLastLineBottom_true_taller() {
        val fontMetrics = createFontMetrics()

        val newFontMetrics =
            runSingleLine(
                topRatio = 0f,
                trimFirstLineTop = true,
                trimLastLineBottom = true,
                fontMetrics = fontMetrics,
                mode = mode,
                tallerLineHeight = true,
            )

        assertThat(newFontMetrics.ascent).isEqualTo(fontMetrics.ascent)
        assertThat(newFontMetrics.descent).isEqualTo(fontMetrics.descent)
    }

    @Test
    fun singleLine_topRatio_0f_trimFirstLineTop_true_trimLastLineBottom_true_shorter() {
        val fontMetrics = createFontMetrics()

        val newFontMetrics =
            runSingleLine(
                topRatio = 0f,
                trimFirstLineTop = true,
                trimLastLineBottom = true,
                fontMetrics = fontMetrics,
                mode = mode,
                tallerLineHeight = false,
            )

        assertThat(newFontMetrics.ascent)
            .isEqualTo(
                if (mode == Tight) fontMetrics.ascent + fontMetrics.halfLineHeight()
                else fontMetrics.ascent
            )
        assertThat(newFontMetrics.descent).isEqualTo(fontMetrics.descent)
    }

    /* single line, bottom aligned */

    @Test
    fun singleLine_topRatio_1f_trimFirstLineTop_false_trimLastLineBottom_false_taller() {
        val fontMetrics = createFontMetrics()

        val newFontMetrics =
            runSingleLine(
                topRatio = 1f,
                trimFirstLineTop = false,
                trimLastLineBottom = false,
                fontMetrics = fontMetrics,
                mode = mode,
                tallerLineHeight = true,
            )

        // The VerticalPaddings for not trimming will be added by TextLayout
        assertThat(newFontMetrics.ascent).isEqualTo(fontMetrics.ascent - fontMetrics.lineHeight())
        assertThat(newFontMetrics.descent).isEqualTo(fontMetrics.descent)
    }

    @Test
    fun singleLine_topRatio_1f_trimFirstLineTop_false_trimLastLineBottom_false_shorter() {
        val fontMetrics = createFontMetrics()

        val newFontMetrics =
            runSingleLine(
                topRatio = 1f,
                trimFirstLineTop = false,
                trimLastLineBottom = false,
                fontMetrics = fontMetrics,
                mode = mode,
                tallerLineHeight = true,
            )

        assertThat(newFontMetrics.ascent).isEqualTo(fontMetrics.ascent - fontMetrics.lineHeight())
        assertThat(newFontMetrics.descent).isEqualTo(fontMetrics.descent)
    }

    @Test
    fun singleLine_topRatio_1f_trimFirstLineTop_false_trimLastLineBottom_true_taller() {
        val fontMetrics = createFontMetrics()

        val newFontMetrics =
            runSingleLine(
                topRatio = 1f,
                trimFirstLineTop = false,
                trimLastLineBottom = true,
                fontMetrics = fontMetrics,
                mode = mode,
                tallerLineHeight = true,
            )

        assertThat(newFontMetrics.ascent).isEqualTo(fontMetrics.ascent - fontMetrics.lineHeight())
        assertThat(newFontMetrics.descent).isEqualTo(fontMetrics.descent)
    }

    @Test
    fun singleLine_topRatio_1f_trimFirstLineTop_false_trimLastLineBottom_true_shorter() {
        val fontMetrics = createFontMetrics()

        val newFontMetrics =
            runSingleLine(
                topRatio = 1f,
                trimFirstLineTop = false,
                trimLastLineBottom = true,
                fontMetrics = fontMetrics,
                mode = mode,
                tallerLineHeight = false,
            )

        assertThat(newFontMetrics.ascent).isEqualTo(fontMetrics.ascent)
        assertThat(newFontMetrics.descent)
            .isEqualTo(
                if (mode == Tight) fontMetrics.descent - fontMetrics.halfLineHeight()
                else fontMetrics.descent
            )
    }

    @Test
    fun singleLine_topRatio_1f_trimFirstLineTop_true_trimLastLineBottom_false_taller() {
        val fontMetrics = createFontMetrics()

        val newFontMetrics =
            runSingleLine(
                topRatio = 1f,
                trimFirstLineTop = true,
                trimLastLineBottom = false,
                fontMetrics = fontMetrics,
                mode = mode,
                tallerLineHeight = true,
            )

        assertThat(newFontMetrics.ascent).isEqualTo(fontMetrics.ascent)
        assertThat(newFontMetrics.descent).isEqualTo(fontMetrics.descent)
    }

    @Test
    fun singleLine_topRatio_1f_trimFirstLineTop_true_trimLastLineBottom_false_shorter() {
        val fontMetrics = createFontMetrics()

        val newFontMetrics =
            runSingleLine(
                topRatio = 1f,
                trimFirstLineTop = true,
                trimLastLineBottom = false,
                fontMetrics = fontMetrics,
                mode = mode,
                tallerLineHeight = false,
            )

        assertThat(newFontMetrics.ascent).isEqualTo(fontMetrics.ascent)
        assertThat(newFontMetrics.descent)
            .isEqualTo(
                if (mode == Minimum || mode == Tight) fontMetrics.descent
                else fontMetrics.descent - fontMetrics.halfLineHeight()
            )
    }

    @Test
    fun singleLine_topRatio_1f_trimFirstLineTop_true_trimLastLineBottom_true_taller() {
        val fontMetrics = createFontMetrics()

        val newFontMetrics =
            runSingleLine(
                topRatio = 1f,
                trimFirstLineTop = true,
                trimLastLineBottom = true,
                fontMetrics = fontMetrics,
                mode = mode,
                tallerLineHeight = true,
            )

        assertThat(newFontMetrics.ascent).isEqualTo(fontMetrics.ascent)
        assertThat(newFontMetrics.descent).isEqualTo(fontMetrics.descent)
    }

    @Test
    fun singleLine_topRatio_1f_trimFirstLineTop_true_trimLastLineBottom_true_shorter() {
        val fontMetrics = createFontMetrics()

        val newFontMetrics =
            runSingleLine(
                topRatio = 1f,
                trimFirstLineTop = true,
                trimLastLineBottom = true,
                fontMetrics = fontMetrics,
                mode = mode,
                tallerLineHeight = false,
            )

        assertThat(newFontMetrics.ascent).isEqualTo(fontMetrics.ascent)
        assertThat(newFontMetrics.descent)
            .isEqualTo(
                if (mode == Tight) fontMetrics.descent - fontMetrics.halfLineHeight()
                else fontMetrics.descent
            )
    }

    /* single line, top percentage 50 */

    @Test
    fun singleLine_topRatio_05f_trimFirstLineTop_false_trimLastLineBottom_false_taller() {
        val fontMetrics = createFontMetrics()

        val newFontMetrics =
            runSingleLine(
                topRatio = 0.5f,
                trimFirstLineTop = false,
                trimLastLineBottom = false,
                fontMetrics = fontMetrics,
                mode = mode,
                tallerLineHeight = true,
            )

        assertThat(newFontMetrics.ascent)
            .isEqualTo(fontMetrics.ascent - fontMetrics.halfLineHeight())
        assertThat(newFontMetrics.descent)
            .isEqualTo(fontMetrics.descent + fontMetrics.halfLineHeight())
    }

    @Test
    fun singleLine_topRatio_05f_trimFirstLineTop_false_trimLastLineBottom_false_shorter() {
        val fontMetrics = createFontMetrics()

        val newFontMetrics =
            runSingleLine(
                topRatio = 0.5f,
                trimFirstLineTop = false,
                trimLastLineBottom = false,
                fontMetrics = fontMetrics,
                mode = mode,
                tallerLineHeight = false,
            )

        assertThat(newFontMetrics.ascent)
            .isEqualTo(
                if (mode == Fixed) fontMetrics.ascent + fontMetrics.halfLineHeight() / 2
                else fontMetrics.ascent
            )
        assertThat(newFontMetrics.descent)
            .isEqualTo(
                if (mode == Fixed) fontMetrics.descent - fontMetrics.halfLineHeight() / 2
                else fontMetrics.descent
            )
    }

    @Test
    fun singleLine_topRatio_50_trimFirstLineTop_false_trimLastLineBottom_true() {
        val fontMetrics = createFontMetrics()

        val newFontMetrics =
            runSingleLine(
                topRatio = 0.5f,
                trimFirstLineTop = false,
                trimLastLineBottom = true,
                fontMetrics = fontMetrics,
                mode = mode,
                tallerLineHeight = true,
            )

        val halfLeading = fontMetrics.lineHeight() / 2
        assertThat(newFontMetrics.ascent).isEqualTo(fontMetrics.ascent - halfLeading)
        assertThat(newFontMetrics.descent).isEqualTo(fontMetrics.descent)
    }

    @Test
    fun singleLine_topRatio_50_trimFirstLineTop_true_trimLastLineBottom_false() {
        val fontMetrics = createFontMetrics()

        val newFontMetrics =
            runSingleLine(
                topRatio = 0.5f,
                trimFirstLineTop = true,
                trimLastLineBottom = false,
                fontMetrics = fontMetrics,
                mode = mode,
                tallerLineHeight = true,
            )

        val halfLeading = fontMetrics.lineHeight() / 2
        assertThat(newFontMetrics.ascent).isEqualTo(fontMetrics.ascent)
        assertThat(newFontMetrics.descent).isEqualTo(fontMetrics.descent + halfLeading)
    }

    @Test
    fun singleLine_topRatio_50_trimFirstLineTop_true_trimLastLineBottom_true() {
        val fontMetrics = createFontMetrics()

        val newFontMetrics =
            runSingleLine(
                topRatio = 0.5f,
                trimFirstLineTop = true,
                trimLastLineBottom = true,
                fontMetrics = fontMetrics,
                mode = mode,
                tallerLineHeight = true,
            )

        assertThat(newFontMetrics.ascent).isEqualTo(fontMetrics.ascent)
        assertThat(newFontMetrics.descent).isEqualTo(fontMetrics.descent)
    }

    /* single line, proportional (topRatio -1) */

    @Test
    fun singleLine_topRatio_proportional_trimFirstLineTop_false_trimLastLineBottom_false() {
        val fontMetrics = createFontMetrics()

        val newFontMetrics =
            runSingleLine(
                topRatio = -1f,
                trimFirstLineTop = false,
                trimLastLineBottom = false,
                fontMetrics = fontMetrics,
                mode = mode,
                tallerLineHeight = true,
            )

        val descentDiff = proportionalDescentDiff(fontMetrics)
        val ascentDiff = fontMetrics.lineHeight() - descentDiff
        assertThat(newFontMetrics.ascent).isEqualTo(fontMetrics.ascent - ascentDiff)
        assertThat(newFontMetrics.descent).isEqualTo(fontMetrics.descent + descentDiff)
    }

    @Test
    fun singleLine_topRatio_proportional_trimFirstLineTop_false_trimLastLineBottom_true() {
        val fontMetrics = createFontMetrics()

        val newFontMetrics =
            runSingleLine(
                topRatio = -1f,
                trimFirstLineTop = false,
                trimLastLineBottom = true,
                fontMetrics = fontMetrics,
                mode = mode,
                tallerLineHeight = true,
            )

        val descentDiff = proportionalDescentDiff(fontMetrics)
        val ascentDiff = fontMetrics.lineHeight() - descentDiff
        assertThat(newFontMetrics.ascent).isEqualTo(fontMetrics.ascent - ascentDiff)
        assertThat(newFontMetrics.descent).isEqualTo(fontMetrics.descent)
    }

    @Test
    fun singleLine_topRatio_proportional_trimFirstLineTop_true_trimLastLineBottom_false() {
        val fontMetrics = createFontMetrics()

        val newFontMetrics =
            runSingleLine(
                topRatio = -1f,
                trimFirstLineTop = true,
                trimLastLineBottom = false,
                fontMetrics = fontMetrics,
                mode = mode,
                tallerLineHeight = true,
            )

        val descentDiff = proportionalDescentDiff(fontMetrics)
        assertThat(newFontMetrics.ascent).isEqualTo(fontMetrics.ascent)
        assertThat(newFontMetrics.descent).isEqualTo(fontMetrics.descent + descentDiff)
    }

    @Test
    fun singleLine_topRatio_proportional_trimFirstLineTop_true_trimLastLineBottom_true() {
        val fontMetrics = createFontMetrics()

        val newFontMetrics =
            runSingleLine(
                topRatio = -1f,
                trimFirstLineTop = true,
                trimLastLineBottom = true,
                fontMetrics = fontMetrics,
                mode = mode,
                tallerLineHeight = true,
            )

        assertThat(newFontMetrics.ascent).isEqualTo(fontMetrics.ascent)
        assertThat(newFontMetrics.descent).isEqualTo(fontMetrics.descent)
    }

    /* multi line, top percentage = 0 */

    @Test
    fun multiLine_topRatio_0_trimFirstLineTop_false_trimLastLineBottom_false() {
        val fontMetrics = createFontMetrics()

        val span =
            createMultiLineSpan(
                topRatio = 0f,
                trimFirstLineTop = false,
                trimLastLineBottom = false,
                fontMetrics = fontMetrics,
                mode = mode,
                tallerLineHeight = true,
            )

        var newFontMetrics = span.runFirstLine(fontMetrics)
        assertThat(newFontMetrics.ascent).isEqualTo(fontMetrics.ascent)
        assertThat(newFontMetrics.descent).isEqualTo(fontMetrics.descent + fontMetrics.lineHeight())

        newFontMetrics = span.runSecondLine(fontMetrics)
        assertThat(newFontMetrics.ascent).isEqualTo(fontMetrics.ascent)
        assertThat(newFontMetrics.descent).isEqualTo(fontMetrics.descent + fontMetrics.lineHeight())

        newFontMetrics = span.runLastLine(fontMetrics)
        assertThat(newFontMetrics.ascent).isEqualTo(fontMetrics.ascent)
        assertThat(newFontMetrics.descent).isEqualTo(fontMetrics.descent + fontMetrics.lineHeight())
    }

    @Test
    fun multiLine_topRatio_0f_trimFirstLineTop_false_trimLastLineBottom_true_taller() {
        val fontMetrics = createFontMetrics()

        val span =
            createMultiLineSpan(
                topRatio = 0f,
                trimFirstLineTop = false,
                trimLastLineBottom = true,
                fontMetrics = fontMetrics,
                mode = mode,
                tallerLineHeight = true,
            )

        var newFontMetrics = span.runFirstLine(fontMetrics)
        assertThat(newFontMetrics.ascent).isEqualTo(fontMetrics.ascent)
        assertThat(newFontMetrics.descent).isEqualTo(fontMetrics.descent + fontMetrics.lineHeight())

        newFontMetrics = span.runSecondLine(fontMetrics)
        assertThat(newFontMetrics.ascent).isEqualTo(fontMetrics.ascent)
        assertThat(newFontMetrics.descent).isEqualTo(fontMetrics.descent + fontMetrics.lineHeight())

        newFontMetrics = span.runLastLine(fontMetrics)
        assertThat(newFontMetrics.ascent).isEqualTo(fontMetrics.ascent)
        assertThat(newFontMetrics.descent).isEqualTo(fontMetrics.descent)
    }

    @Test
    fun multiLine_topRatio_0f_trimFirstLineTop_false_trimLastLineBottom_true_shorter() {
        val fontMetrics = createFontMetrics()

        val span =
            createMultiLineSpan(
                topRatio = 0f,
                trimFirstLineTop = false,
                trimLastLineBottom = true,
                fontMetrics = fontMetrics,
                mode = mode,
                tallerLineHeight = false,
            )

        var newFontMetrics = span.runFirstLine(fontMetrics)
        assertThat(newFontMetrics.ascent)
            .isEqualTo(
                if (mode == Fixed) fontMetrics.ascent + fontMetrics.halfLineHeight()
                else fontMetrics.ascent
            )
        assertThat(newFontMetrics.descent).isEqualTo(fontMetrics.descent)

        newFontMetrics = span.runSecondLine(fontMetrics)
        assertThat(newFontMetrics.ascent)
            .isEqualTo(
                if (mode == Fixed || mode == Tight)
                    fontMetrics.ascent + fontMetrics.halfLineHeight()
                else fontMetrics.ascent
            )
        assertThat(newFontMetrics.descent).isEqualTo(fontMetrics.descent)

        newFontMetrics = span.runLastLine(fontMetrics)
        assertThat(newFontMetrics.ascent)
            .isEqualTo(
                if (mode == Fixed || mode == Tight)
                    fontMetrics.ascent + fontMetrics.halfLineHeight()
                else fontMetrics.ascent
            )
        assertThat(newFontMetrics.descent).isEqualTo(fontMetrics.descent)
    }

    @Test
    fun multiLine_topRatio_0_trimFirstLineTop_true_trimLastLineBottom_false() {
        val fontMetrics = createFontMetrics()

        val span =
            createMultiLineSpan(
                topRatio = 0f,
                trimFirstLineTop = true,
                trimLastLineBottom = false,
                fontMetrics = fontMetrics,
                mode = mode,
                tallerLineHeight = true,
            )

        var newFontMetrics = span.runFirstLine(fontMetrics)
        assertThat(newFontMetrics.ascent).isEqualTo(fontMetrics.ascent)
        assertThat(newFontMetrics.descent).isEqualTo(fontMetrics.descent + fontMetrics.lineHeight())

        newFontMetrics = span.runSecondLine(fontMetrics)
        assertThat(newFontMetrics.ascent).isEqualTo(fontMetrics.ascent)
        assertThat(newFontMetrics.descent).isEqualTo(fontMetrics.descent + fontMetrics.lineHeight())

        newFontMetrics = span.runLastLine(fontMetrics)
        assertThat(newFontMetrics.ascent).isEqualTo(fontMetrics.ascent)
        assertThat(newFontMetrics.descent).isEqualTo(fontMetrics.descent + fontMetrics.lineHeight())
    }

    @Test
    fun multiLine_topRatio_0_trimFirstLineTop_true_trimLastLineBottom_true() {
        val fontMetrics = createFontMetrics()

        val span =
            createMultiLineSpan(
                topRatio = 0f,
                trimFirstLineTop = true,
                trimLastLineBottom = true,
                fontMetrics = fontMetrics,
                mode = mode,
                tallerLineHeight = true,
            )

        var newFontMetrics = span.runFirstLine(fontMetrics)
        assertThat(newFontMetrics.ascent).isEqualTo(fontMetrics.ascent)
        assertThat(newFontMetrics.descent).isEqualTo(fontMetrics.descent + fontMetrics.lineHeight())

        newFontMetrics = span.runSecondLine(fontMetrics)
        assertThat(newFontMetrics.ascent).isEqualTo(fontMetrics.ascent)
        assertThat(newFontMetrics.descent).isEqualTo(fontMetrics.descent + fontMetrics.lineHeight())

        newFontMetrics = span.runLastLine(fontMetrics)
        assertThat(newFontMetrics.ascent).isEqualTo(fontMetrics.ascent)
        assertThat(newFontMetrics.descent).isEqualTo(fontMetrics.descent)
    }

    /* multi line, bottom aligned */

    @Test
    fun multiLine_topRatio_1f_trimFirstLineTop_false_trimLastLineBottom_false_taller() {
        val fontMetrics = createFontMetrics()

        val span =
            createMultiLineSpan(
                topRatio = 1f,
                trimFirstLineTop = false,
                trimLastLineBottom = false,
                fontMetrics = fontMetrics,
                mode = mode,
                tallerLineHeight = true,
            )

        var newFontMetrics = span.runFirstLine(fontMetrics)
        assertThat(newFontMetrics.ascent).isEqualTo(fontMetrics.ascent - fontMetrics.lineHeight())
        assertThat(newFontMetrics.descent).isEqualTo(fontMetrics.descent)

        newFontMetrics = span.runSecondLine(fontMetrics)
        assertThat(newFontMetrics.ascent).isEqualTo(fontMetrics.ascent - fontMetrics.lineHeight())
        assertThat(newFontMetrics.descent).isEqualTo(fontMetrics.descent)

        newFontMetrics = span.runLastLine(fontMetrics)
        assertThat(newFontMetrics.ascent).isEqualTo(fontMetrics.ascent - fontMetrics.lineHeight())
        assertThat(newFontMetrics.descent).isEqualTo(fontMetrics.descent)
    }

    @Test
    fun multiLine_topRatio_1f_trimFirstLineTop_false_trimLastLineBottom_false_shorter() {
        val fontMetrics = createFontMetrics()

        val span =
            createMultiLineSpan(
                topRatio = 1f,
                trimFirstLineTop = false,
                trimLastLineBottom = false,
                fontMetrics = fontMetrics,
                mode = mode,
                tallerLineHeight = false,
            )

        var newFontMetrics = span.runFirstLine(fontMetrics)
        assertThat(newFontMetrics.ascent).isEqualTo(fontMetrics.ascent)
        assertThat(newFontMetrics.descent)
            .isEqualTo(
                if (mode == Fixed || mode == Tight)
                    fontMetrics.descent - fontMetrics.halfLineHeight()
                else fontMetrics.descent
            )

        newFontMetrics = span.runSecondLine(fontMetrics)
        assertThat(newFontMetrics.ascent).isEqualTo(fontMetrics.ascent)
        assertThat(newFontMetrics.descent)
            .isEqualTo(
                if (mode == Fixed || mode == Tight)
                    fontMetrics.descent - fontMetrics.halfLineHeight()
                else fontMetrics.descent
            )

        newFontMetrics = span.runLastLine(fontMetrics)
        assertThat(newFontMetrics.ascent).isEqualTo(fontMetrics.ascent)
        assertThat(newFontMetrics.descent)
            .isEqualTo(
                if (mode == Fixed) fontMetrics.descent - fontMetrics.halfLineHeight()
                else fontMetrics.descent
            )
    }

    @Test
    fun multiLine_topRatio_100_trimFirstLineTop_false_trimLastLineBottom_true() {
        val fontMetrics = createFontMetrics()

        val span =
            createMultiLineSpan(
                topRatio = 1f,
                trimFirstLineTop = false,
                trimLastLineBottom = true,
                fontMetrics = fontMetrics,
                mode = mode,
                tallerLineHeight = true,
            )

        var newFontMetrics = span.runFirstLine(fontMetrics)
        assertThat(newFontMetrics.ascent).isEqualTo(fontMetrics.ascent - fontMetrics.lineHeight())
        assertThat(newFontMetrics.descent).isEqualTo(fontMetrics.descent)

        newFontMetrics = span.runSecondLine(fontMetrics)
        assertThat(newFontMetrics.ascent).isEqualTo(fontMetrics.ascent - fontMetrics.lineHeight())
        assertThat(newFontMetrics.descent).isEqualTo(fontMetrics.descent)

        newFontMetrics = span.runLastLine(fontMetrics)
        assertThat(newFontMetrics.ascent).isEqualTo(fontMetrics.ascent - fontMetrics.lineHeight())
        assertThat(newFontMetrics.descent).isEqualTo(fontMetrics.descent)
    }

    @Test
    fun multiLine_topRatio_100_trimFirstLineTop_true_trimLastLineBottom_false() {
        val fontMetrics = createFontMetrics()

        val span =
            createMultiLineSpan(
                topRatio = 1f,
                trimFirstLineTop = true,
                trimLastLineBottom = false,
                fontMetrics = fontMetrics,
                mode = mode,
                tallerLineHeight = true,
            )

        var newFontMetrics = span.runFirstLine(fontMetrics)
        assertThat(newFontMetrics.ascent).isEqualTo(fontMetrics.ascent)
        assertThat(newFontMetrics.descent).isEqualTo(fontMetrics.descent)

        newFontMetrics = span.runSecondLine(fontMetrics)
        assertThat(newFontMetrics.ascent).isEqualTo(fontMetrics.ascent - fontMetrics.lineHeight())
        assertThat(newFontMetrics.descent).isEqualTo(fontMetrics.descent)

        newFontMetrics = span.runLastLine(fontMetrics)
        assertThat(newFontMetrics.ascent).isEqualTo(fontMetrics.ascent - fontMetrics.lineHeight())
        assertThat(newFontMetrics.descent).isEqualTo(fontMetrics.descent)
    }

    @Test
    fun multiLine_topRatio_1f_trimFirstLineTop_true_trimLastLineBottom_true_taller() {
        val fontMetrics = createFontMetrics()

        val span =
            createMultiLineSpan(
                topRatio = 1f,
                trimFirstLineTop = true,
                trimLastLineBottom = true,
                fontMetrics = fontMetrics,
                mode = mode,
                tallerLineHeight = true,
            )

        var newFontMetrics = span.runFirstLine(fontMetrics)
        assertThat(newFontMetrics.ascent).isEqualTo(fontMetrics.ascent)
        assertThat(newFontMetrics.descent).isEqualTo(fontMetrics.descent)

        newFontMetrics = span.runSecondLine(fontMetrics)
        assertThat(newFontMetrics.ascent).isEqualTo(fontMetrics.ascent - fontMetrics.lineHeight())
        assertThat(newFontMetrics.descent).isEqualTo(fontMetrics.descent)

        newFontMetrics = span.runLastLine(fontMetrics)
        assertThat(newFontMetrics.ascent).isEqualTo(fontMetrics.ascent - fontMetrics.lineHeight())
        assertThat(newFontMetrics.descent).isEqualTo(fontMetrics.descent)
    }

    @Test
    fun multiLine_topRatio_1f_trimFirstLineTop_true_trimLastLineBottom_true_shorter() {
        val fontMetrics = createFontMetrics()

        val span =
            createMultiLineSpan(
                topRatio = 1f,
                trimFirstLineTop = true,
                trimLastLineBottom = true,
                fontMetrics = fontMetrics,
                mode = mode,
                tallerLineHeight = false,
            )

        var newFontMetrics = span.runFirstLine(fontMetrics)
        assertThat(newFontMetrics.ascent).isEqualTo(fontMetrics.ascent)
        assertThat(newFontMetrics.descent)
            .isEqualTo(
                if (mode == Fixed || mode == Tight)
                    fontMetrics.descent - fontMetrics.halfLineHeight()
                else fontMetrics.descent
            )

        newFontMetrics = span.runSecondLine(fontMetrics)
        assertThat(newFontMetrics.ascent).isEqualTo(fontMetrics.ascent)
        assertThat(newFontMetrics.descent)
            .isEqualTo(
                if (mode == Fixed || mode == Tight)
                    fontMetrics.descent - fontMetrics.halfLineHeight()
                else fontMetrics.descent
            )

        newFontMetrics = span.runLastLine(fontMetrics)
        assertThat(newFontMetrics.ascent).isEqualTo(fontMetrics.ascent)
        assertThat(newFontMetrics.descent)
            .isEqualTo(
                if (mode == Tight) fontMetrics.descent - fontMetrics.halfLineHeight()
                else fontMetrics.descent
            )
    }

    /* multi line, top percentage = 50 */

    @Test
    fun multiLine_topRatio_50_trimFirstLineTop_false_trimLastLineBottom_false() {
        val fontMetrics = createFontMetrics()

        val span =
            createMultiLineSpan(
                topRatio = 0.5f,
                trimFirstLineTop = false,
                trimLastLineBottom = false,
                fontMetrics = fontMetrics,
                mode = mode,
                tallerLineHeight = true,
            )

        val halfLeading = fontMetrics.lineHeight() / 2
        var newFontMetrics = span.runFirstLine(fontMetrics)
        assertThat(newFontMetrics.ascent).isEqualTo(fontMetrics.ascent - halfLeading)
        assertThat(newFontMetrics.descent).isEqualTo(fontMetrics.descent + halfLeading)

        newFontMetrics = span.runSecondLine(fontMetrics)
        assertThat(newFontMetrics.ascent).isEqualTo(fontMetrics.ascent - halfLeading)
        assertThat(newFontMetrics.descent).isEqualTo(fontMetrics.descent + halfLeading)

        newFontMetrics = span.runLastLine(fontMetrics)
        assertThat(newFontMetrics.ascent).isEqualTo(fontMetrics.ascent - halfLeading)
        assertThat(newFontMetrics.descent).isEqualTo(fontMetrics.descent + halfLeading)
    }

    @Test
    fun multiLine_topRatio_50_trimFirstLineTop_false_trimLastLineBottom_true() {
        val fontMetrics = createFontMetrics()

        val span =
            createMultiLineSpan(
                topRatio = 0.5f,
                trimFirstLineTop = false,
                trimLastLineBottom = true,
                fontMetrics = fontMetrics,
                mode = mode,
                tallerLineHeight = true,
            )

        val halfLeading = fontMetrics.lineHeight() / 2
        var newFontMetrics = span.runFirstLine(fontMetrics)
        assertThat(newFontMetrics.ascent).isEqualTo(fontMetrics.ascent - halfLeading)
        assertThat(newFontMetrics.descent).isEqualTo(fontMetrics.descent + halfLeading)

        newFontMetrics = span.runSecondLine(fontMetrics)
        assertThat(newFontMetrics.ascent).isEqualTo(fontMetrics.ascent - halfLeading)
        assertThat(newFontMetrics.descent).isEqualTo(fontMetrics.descent + halfLeading)

        newFontMetrics = span.runLastLine(fontMetrics)
        assertThat(newFontMetrics.ascent).isEqualTo(fontMetrics.ascent - halfLeading)
        assertThat(newFontMetrics.descent).isEqualTo(fontMetrics.descent)
    }

    @Test
    fun multiLine_topRatio_50_trimFirstLineTop_true_trimLastLineBottom_false() {
        val fontMetrics = createFontMetrics()

        val span =
            createMultiLineSpan(
                topRatio = 0.5f,
                trimFirstLineTop = true,
                trimLastLineBottom = false,
                fontMetrics = fontMetrics,
                mode = mode,
                tallerLineHeight = true,
            )

        val halfLeading = fontMetrics.lineHeight() / 2
        var newFontMetrics = span.runFirstLine(fontMetrics)
        assertThat(newFontMetrics.ascent).isEqualTo(fontMetrics.ascent)
        assertThat(newFontMetrics.descent).isEqualTo(fontMetrics.descent + halfLeading)

        newFontMetrics = span.runSecondLine(fontMetrics)
        assertThat(newFontMetrics.ascent).isEqualTo(fontMetrics.ascent - halfLeading)
        assertThat(newFontMetrics.descent).isEqualTo(fontMetrics.descent + halfLeading)

        newFontMetrics = span.runLastLine(fontMetrics)
        assertThat(newFontMetrics.ascent).isEqualTo(fontMetrics.ascent - halfLeading)
        assertThat(newFontMetrics.descent).isEqualTo(fontMetrics.descent + halfLeading)
    }

    @Test
    fun multiLine_topRatio_50_trimFirstLineTop_true_trimLastLineBottom_true() {
        val fontMetrics = createFontMetrics()

        val span =
            createMultiLineSpan(
                topRatio = 0.5f,
                trimFirstLineTop = true,
                trimLastLineBottom = true,
                fontMetrics = fontMetrics,
                mode = mode,
                tallerLineHeight = true,
            )

        val halfLeading = fontMetrics.lineHeight() / 2
        var newFontMetrics = span.runFirstLine(fontMetrics)
        assertThat(newFontMetrics.ascent).isEqualTo(fontMetrics.ascent)
        assertThat(newFontMetrics.descent).isEqualTo(fontMetrics.descent + halfLeading)

        newFontMetrics = span.runSecondLine(fontMetrics)
        assertThat(newFontMetrics.ascent).isEqualTo(fontMetrics.ascent - halfLeading)
        assertThat(newFontMetrics.descent).isEqualTo(fontMetrics.descent + halfLeading)

        newFontMetrics = span.runLastLine(fontMetrics)
        assertThat(newFontMetrics.ascent).isEqualTo(fontMetrics.ascent - halfLeading)
        assertThat(newFontMetrics.descent).isEqualTo(fontMetrics.descent)
    }

    /* multi line, proportional (topRatio -1) */

    @Test
    fun multiLine_topRatio_proportional_trimFirstLineTop_false_trimLastLineBottom_false() {
        val fontMetrics = createFontMetrics()

        val span =
            createMultiLineSpan(
                topRatio = -1f,
                trimFirstLineTop = false,
                trimLastLineBottom = false,
                fontMetrics = fontMetrics,
                mode = mode,
                tallerLineHeight = true,
            )

        val descentDiff = proportionalDescentDiff(fontMetrics)
        val ascentDiff = fontMetrics.lineHeight() - descentDiff

        var newFontMetrics = span.runFirstLine(fontMetrics)
        assertThat(newFontMetrics.ascent).isEqualTo(fontMetrics.ascent - ascentDiff)
        assertThat(newFontMetrics.descent).isEqualTo(fontMetrics.descent + descentDiff)

        newFontMetrics = span.runSecondLine(fontMetrics)
        assertThat(newFontMetrics.ascent).isEqualTo(fontMetrics.ascent - ascentDiff)
        assertThat(newFontMetrics.descent).isEqualTo(fontMetrics.descent + descentDiff)

        newFontMetrics = span.runLastLine(fontMetrics)
        assertThat(newFontMetrics.ascent).isEqualTo(fontMetrics.ascent - ascentDiff)
        assertThat(newFontMetrics.descent).isEqualTo(fontMetrics.descent + descentDiff)
    }

    @Test
    fun multiLine_topRatio_proportional_trimFirstLineTop_false_trimLastLineBottom_true() {
        val fontMetrics = createFontMetrics()

        val span =
            createMultiLineSpan(
                topRatio = -1f,
                trimFirstLineTop = false,
                trimLastLineBottom = true,
                fontMetrics = fontMetrics,
                mode = mode,
                tallerLineHeight = true,
            )

        val descentDiff = proportionalDescentDiff(fontMetrics)
        val ascentDiff = fontMetrics.lineHeight() - descentDiff

        var newFontMetrics = span.runFirstLine(fontMetrics)
        assertThat(newFontMetrics.ascent).isEqualTo(fontMetrics.ascent - ascentDiff)
        assertThat(newFontMetrics.descent).isEqualTo(fontMetrics.descent + descentDiff)

        newFontMetrics = span.runSecondLine(fontMetrics)
        assertThat(newFontMetrics.ascent).isEqualTo(fontMetrics.ascent - ascentDiff)
        assertThat(newFontMetrics.descent).isEqualTo(fontMetrics.descent + descentDiff)

        newFontMetrics = span.runLastLine(fontMetrics)
        assertThat(newFontMetrics.ascent).isEqualTo(fontMetrics.ascent - ascentDiff)
        assertThat(newFontMetrics.descent).isEqualTo(fontMetrics.descent)
    }

    @Test
    fun multiLine_topRatio_proportional_trimFirstLineTop_true_trimLastLineBottom_false() {
        val fontMetrics = createFontMetrics()

        val span =
            createMultiLineSpan(
                topRatio = -1f,
                trimFirstLineTop = true,
                trimLastLineBottom = false,
                fontMetrics = fontMetrics,
                mode = mode,
                tallerLineHeight = true,
            )

        val descentDiff = proportionalDescentDiff(fontMetrics)
        val ascentDiff = fontMetrics.lineHeight() - descentDiff

        var newFontMetrics = span.runFirstLine(fontMetrics)
        assertThat(newFontMetrics.ascent).isEqualTo(fontMetrics.ascent)
        assertThat(newFontMetrics.descent).isEqualTo(fontMetrics.descent + descentDiff)

        newFontMetrics = span.runSecondLine(fontMetrics)
        assertThat(newFontMetrics.ascent).isEqualTo(fontMetrics.ascent - ascentDiff)
        assertThat(newFontMetrics.descent).isEqualTo(fontMetrics.descent + descentDiff)

        newFontMetrics = span.runLastLine(fontMetrics)
        assertThat(newFontMetrics.ascent).isEqualTo(fontMetrics.ascent - ascentDiff)
        assertThat(newFontMetrics.descent).isEqualTo(fontMetrics.descent + descentDiff)
    }

    @Test
    fun multiLine_topRatio_proportional_trimFirstLineTop_true_trimLastLineBottom_true() {
        val fontMetrics = createFontMetrics()

        val span =
            createMultiLineSpan(
                topRatio = -1f,
                trimFirstLineTop = true,
                trimLastLineBottom = true,
                fontMetrics = fontMetrics,
                mode = mode,
                tallerLineHeight = true,
            )

        val descentDiff = proportionalDescentDiff(fontMetrics)
        val ascentDiff = fontMetrics.lineHeight() - descentDiff

        var newFontMetrics = span.runFirstLine(fontMetrics)
        assertThat(newFontMetrics.ascent).isEqualTo(fontMetrics.ascent)
        assertThat(newFontMetrics.descent).isEqualTo(fontMetrics.descent + descentDiff)

        newFontMetrics = span.runSecondLine(fontMetrics)
        assertThat(newFontMetrics.ascent).isEqualTo(fontMetrics.ascent - ascentDiff)
        assertThat(newFontMetrics.descent).isEqualTo(fontMetrics.descent + descentDiff)

        newFontMetrics = span.runLastLine(fontMetrics)
        assertThat(newFontMetrics.ascent).isEqualTo(fontMetrics.ascent - ascentDiff)
        assertThat(newFontMetrics.descent).isEqualTo(fontMetrics.descent)
    }

    /* first ascent & last descent diff */

    @Test
    fun singleLine_with_firstLineTop_and_lastLineBottom_topRatio_05f_taller() {
        val fontMetrics = createFontMetrics()

        val span =
            createSingleLineSpan(
                topRatio = 0.5f,
                trimFirstLineTop = false,
                trimLastLineBottom = false,
                newLineHeight = fontMetrics.doubleLineHeight(),
                mode = mode,
            )

        span.runFirstLine(fontMetrics)

        val halfLeading = fontMetrics.lineHeight() / 2
        assertThat(span.firstAscentDiff).isEqualTo(halfLeading)
        assertThat(span.lastDescentDiff).isEqualTo(halfLeading)
    }

    @Test
    fun singleLine_with_firstLineTop_and_lastLineBottom_topRatio_05f_shorter() {
        val fontMetrics = createFontMetrics()

        val span =
            createSingleLineSpan(
                topRatio = 0.5f,
                trimFirstLineTop = false,
                trimLastLineBottom = false,
                newLineHeight = fontMetrics.halfLineHeight(),
                mode = mode,
            )

        span.runFirstLine(fontMetrics)

        if (mode == Fixed) {
            val halfLeading = fontMetrics.lineHeight() / -4
            assertThat(span.firstAscentDiff).isEqualTo(halfLeading)
            assertThat(span.lastDescentDiff).isEqualTo(halfLeading)
        } else if (mode == Minimum) {
            assertThat(span.firstAscentDiff).isEqualTo(0)
            assertThat(span.lastDescentDiff).isEqualTo(0)
        }
    }

    @Test
    fun multiLine_with_firstLineTop_and_lastLineBottom_topRatio_05f_taller() {
        val fontMetrics = createFontMetrics()

        val span =
            createMultiLineSpan(
                topRatio = 0.5f,
                trimFirstLineTop = false,
                trimLastLineBottom = false,
                fontMetrics = fontMetrics,
                mode = mode,
                tallerLineHeight = true,
            )

        span.runFirstLine(fontMetrics)
        span.runSecondLine(fontMetrics)
        span.runLastLine(fontMetrics)

        val halfLeading = fontMetrics.lineHeight() / 2
        assertThat(span.firstAscentDiff).isEqualTo(halfLeading)
        assertThat(span.lastDescentDiff).isEqualTo(halfLeading)
    }

    @Test
    fun multiLine_with_firstLineTop_and_lastLineBottom_topRatio_05f_shorter() {
        val fontMetrics = createFontMetrics()

        val span =
            createMultiLineSpan(
                topRatio = 0.5f,
                trimFirstLineTop = false,
                trimLastLineBottom = false,
                fontMetrics = fontMetrics,
                mode = mode,
                tallerLineHeight = false,
            )

        span.runFirstLine(fontMetrics)
        span.runSecondLine(fontMetrics)
        span.runLastLine(fontMetrics)

        if (mode == Fixed) {
            val halfLeading = fontMetrics.lineHeight() / -4
            assertThat(span.firstAscentDiff).isEqualTo(halfLeading)
            assertThat(span.lastDescentDiff).isEqualTo(halfLeading)
        } else if (mode == Minimum) {
            assertThat(span.firstAscentDiff).isEqualTo(0)
            assertThat(span.lastDescentDiff).isEqualTo(0)
        }
    }

    private fun proportionalDescentDiff(fontMetrics: FontMetricsInt): Int {
        val ascent = abs(fontMetrics.ascent.toFloat())
        val ascentRatio = ascent / fontMetrics.lineHeight()
        return ceil(fontMetrics.lineHeight() * (1f - ascentRatio)).toInt()
    }

    /**
     * Creates a single line span, and runs for the first line. Returns a new font metrics for the
     * updated font metrics.
     */
    private fun runSingleLine(
        topRatio: Float,
        trimFirstLineTop: Boolean,
        trimLastLineBottom: Boolean,
        fontMetrics: FontMetricsInt,
        mode: LineHeightStyle.Mode,
        tallerLineHeight: Boolean,
    ): FontMetricsInt {
        val span =
            createSingleLineSpan(
                topRatio = topRatio,
                trimFirstLineTop = trimFirstLineTop,
                trimLastLineBottom = trimLastLineBottom,
                newLineHeight =
                    if (tallerLineHeight) fontMetrics.doubleLineHeight()
                    else fontMetrics.halfLineHeight(),
                mode = mode,
            )

        return span.runFirstLine(fontMetrics.copy())
    }

    /** Creates a LineHeightSpan that covers [SingleLineStartIndex, SingleLineEndIndex]. */
    private fun createSingleLineSpan(
        topRatio: Float,
        trimFirstLineTop: Boolean,
        trimLastLineBottom: Boolean,
        newLineHeight: Int,
        mode: LineHeightStyle.Mode,
    ): LineHeightStyleSpan =
        LineHeightStyleSpan(
            lineHeight = newLineHeight.toFloat(),
            startIndex = SingleLineStartIndex,
            endIndex = SingleLineEndIndex,
            trimFirstLineTop = trimFirstLineTop,
            trimLastLineBottom = trimLastLineBottom,
            topRatio = topRatio,
            mode = mode,
        )

    /** Creates a LineHeightSpan that covers [MultiLineStartIndex, MultiLineEndIndex]. */
    private fun createMultiLineSpan(
        topRatio: Float,
        trimFirstLineTop: Boolean,
        trimLastLineBottom: Boolean,
        fontMetrics: FontMetricsInt,
        mode: LineHeightStyle.Mode,
        tallerLineHeight: Boolean,
    ): LineHeightStyleSpan =
        createMultiLineSpan(
            topRatio = topRatio,
            trimFirstLineTop = trimFirstLineTop,
            trimLastLineBottom = trimLastLineBottom,
            newLineHeight =
                if (tallerLineHeight) fontMetrics.doubleLineHeight()
                else fontMetrics.halfLineHeight(),
            mode = mode,
        )

    /** Creates a LineHeightSpan that covers [MultiLineStartIndex, MultiLineEndIndex]. */
    private fun createMultiLineSpan(
        topRatio: Float,
        trimFirstLineTop: Boolean,
        trimLastLineBottom: Boolean,
        newLineHeight: Int,
        mode: LineHeightStyle.Mode,
    ): LineHeightStyleSpan =
        LineHeightStyleSpan(
            lineHeight = newLineHeight.toFloat(),
            startIndex = MultiLineStartIndex,
            endIndex = MultiLineEndIndex,
            trimFirstLineTop = trimFirstLineTop,
            trimLastLineBottom = trimLastLineBottom,
            topRatio = topRatio,
            mode = mode,
        )

    /** Creates a FontMetricsInt with line height of 20, where ascent is -10, descent is 10 */
    private fun createFontMetrics(): FontMetricsInt = FontMetricsInt(ascent = -10, descent = 10)
}

/** Creates a copy of FontMetricsInt. */
private fun FontMetricsInt.copy(): FontMetricsInt =
    FontMetricsInt(
        top = this.top,
        ascent = this.ascent,
        descent = this.descent,
        bottom = this.bottom,
        leading = this.leading,
    )

/** Returns 2 * fontMetrics.lineHeight. */
private fun FontMetricsInt.doubleLineHeight(): Int = this.lineHeight() * 2

/** Returns fontMetrics.lineHeight / 2. */
private fun FontMetricsInt.halfLineHeight(): Int = this.lineHeight() / 2

/** Creates a FontMetricsInt. */
private fun FontMetricsInt(
    ascent: Int,
    descent: Int,
    bottom: Int = descent,
    top: Int = ascent,
    leading: Int = 0,
): FontMetricsInt =
    FontMetricsInt().apply {
        this.top = top
        this.ascent = ascent
        this.descent = descent
        this.bottom = bottom
        this.leading = leading
    }

/**
 * Runs the chooseHeight for the first line on the span and returns a new FontMetrics with the
 * updated values.
 */
@OptIn(InternalPlatformTextApi::class)
private fun LineHeightStyleSpan.runFirstLine(fontMetrics: FontMetricsInt): FontMetricsInt {
    return this.runMultiLine(0, fontMetrics)
}

/**
 * Runs the chooseHeight for the second line on the span and returns a new FontMetrics with the
 * updated values.
 */
@OptIn(InternalPlatformTextApi::class)
private fun LineHeightStyleSpan.runSecondLine(fontMetrics: FontMetricsInt): FontMetricsInt {
    return this.runMultiLine(1, fontMetrics)
}

/**
 * Runs the chooseHeight for the last line on the span and returns a new FontMetrics with the
 * updated values.
 */
@OptIn(InternalPlatformTextApi::class)
private fun LineHeightStyleSpan.runLastLine(fontMetrics: FontMetricsInt): FontMetricsInt {
    return this.runMultiLine(2, fontMetrics)
}

/**
 * Utility function to run chooseHeight on a given line and return a new FontMetrics with the
 * updated values.
 */
@OptIn(InternalPlatformTextApi::class)
private fun LineHeightStyleSpan.runMultiLine(
    line: Int,
    fontMetrics: FontMetricsInt,
): FontMetricsInt {
    val newFontMetrics = fontMetrics.copy()

    this.chooseHeight(
        start = MultiLineStartIndex + line,
        end = MultiLineStartIndex + line + 1,
        fontMetricsInt = newFontMetrics,
    )

    return newFontMetrics
}

/**
 * Shortcut function for chooseHeight since some of the parameters of chooseHeight is not being
 * used.
 */
@OptIn(InternalPlatformTextApi::class)
private fun LineHeightStyleSpan.chooseHeight(start: Int, end: Int, fontMetricsInt: FontMetricsInt) {
    this.chooseHeight(
        text = "",
        start = start,
        end = end,
        spanStartVertical = 0,
        lineHeight = 0,
        fontMetricsInt = fontMetricsInt,
    )
}
