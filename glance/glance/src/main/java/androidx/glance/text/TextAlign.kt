/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.glance.text

/** Defines the alignment of the text in its view. */
@JvmInline
public value class TextAlign internal constructor(private val value: Int) {
    override fun toString(): String {
        return when (this) {
            Left -> "Left"
            Right -> "Right"
            Center -> "Center"
            Start -> "Start"
            End -> "End"
            else -> "Invalid"
        }
    }

    public companion object {
        /** Align the text on the left edge of the container. */
        public val Left: TextAlign = TextAlign(1)

        /** Align the text on the right edge of the container. */
        public val Right: TextAlign = TextAlign(2)

        /** Align the text in the center of the container. */
        public val Center: TextAlign = TextAlign(3)

        /**
         * Align the text on the leading edge of the container.
         *
         * For Left to Right text, this is the left edge.
         *
         * For Right to Left text, like Arabic, this is the right edge.
         */
        public val Start: TextAlign = TextAlign(4)

        /**
         * Align the text on the trailing edge of the container.
         *
         * For Left to Right text, this is the right edge.
         *
         * For Right to Left text, like Arabic, this is the left edge.
         */
        public val End: TextAlign = TextAlign(5)

        /** Return a list containing all possible values of TextAlign. */
        public fun values(): List<TextAlign> = listOf(Left, Right, Center, Start, End)
    }
}
