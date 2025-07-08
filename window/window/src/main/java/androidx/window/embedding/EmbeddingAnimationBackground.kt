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

package androidx.window.embedding

import android.graphics.Color
import androidx.annotation.ColorInt
import androidx.annotation.IntRange

/**
 * Background to be used for window transition animations for embedding activities if the animation
 * requires a background.
 *
 * @see EmbeddingAnimationParams.animationBackground
 */
public abstract class EmbeddingAnimationBackground private constructor() {

    /**
     * An {@link EmbeddingAnimationBackground} to specify of using a developer-defined color as the
     * animation background. Only opaque background is supported.
     *
     * @see EmbeddingAnimationBackground.createColorBackground
     */
    public class ColorBackground
    internal constructor(
        /** [ColorInt] to represent the color to use as the background color. */
        @IntRange(from = Color.BLACK.toLong(), to = Color.WHITE.toLong())
        @ColorInt
        public val color: Int
    ) : EmbeddingAnimationBackground() {

        init {
            require(Color.alpha(color) == 255) { "Background color must be opaque" }
        }

        override fun toString(): String = "ColorBackground{color:${Integer.toHexString(color)}}"

        override fun equals(other: Any?): Boolean {
            if (other === this) return true
            if (other !is ColorBackground) return false
            return color == other.color
        }

        override fun hashCode(): Int = color.hashCode()
    }

    /** @see EmbeddingAnimationBackground.DEFAULT */
    private class DefaultBackground : EmbeddingAnimationBackground() {

        override fun toString() = "DefaultBackground"

        // Override #hashCode to return consistent value every time.
        @Suppress("EqualsAndHashCode") override fun hashCode() = toString().hashCode()
    }

    /** Methods that create various [EmbeddingAnimationBackground]. */
    public companion object {

        /**
         * Creates a [ColorBackground] to represent the given [color].
         *
         * Only opaque color is supported.
         *
         * @param color [ColorInt] of an opaque color.
         * @return the [ColorBackground] representing the [color].
         * @throws IllegalArgumentException if the [color] is not opaque.
         * @see [DEFAULT] for the default value, which means to use the current theme window
         *   background color.
         */
        @JvmStatic
        public fun createColorBackground(
            @IntRange(from = Color.BLACK.toLong(), to = Color.WHITE.toLong()) @ColorInt color: Int
        ): ColorBackground = ColorBackground(color)

        /**
         * The special [EmbeddingAnimationBackground] to represent the default value, which means to
         * use the current theme window background color.
         */
        @JvmField public val DEFAULT: EmbeddingAnimationBackground = DefaultBackground()

        /** Returns an [EmbeddingAnimationBackground] with the given [color] */
        internal fun buildFromValue(@ColorInt color: Int): EmbeddingAnimationBackground {
            return if (Color.alpha(color) != 255) {
                // Treat any non-opaque color as the default.
                DEFAULT
            } else {
                createColorBackground(color)
            }
        }
    }
}
