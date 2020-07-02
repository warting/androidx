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

package androidx.ui.test

import androidx.ui.core.AlignmentLine
import androidx.ui.core.AndroidOwner
import androidx.ui.core.ExperimentalLayoutNodeApi
import androidx.ui.core.semantics.SemanticsNode
import androidx.ui.unit.Bounds
import androidx.ui.unit.Density
import androidx.ui.unit.Dp
import androidx.ui.unit.PxBounds
import androidx.ui.unit.height
import androidx.ui.unit.toSize
import androidx.ui.unit.width
import kotlin.math.absoluteValue

private const val floatTolerance = 0.5f

/**
 * Asserts that the layout of this node has width equal to [expectedWidth].
 *
 * @throws AssertionError if comparison fails.
 */
fun SemanticsNodeInteraction.assertWidthIsEqualTo(expectedWidth: Dp): SemanticsNodeInteraction {
    return withBoundsInRoot {
        it.width.toDp().assertIsEqualTo(expectedWidth, "width")
    }
}

/**
 * Asserts that the layout of this node has height equal to [expectedHeight].
 *
 * @throws AssertionError if comparison fails.
 */
fun SemanticsNodeInteraction.assertHeightIsEqualTo(expectedHeight: Dp): SemanticsNodeInteraction {
    return withBoundsInRoot {
        it.height.toDp().assertIsEqualTo(expectedHeight, "height")
    }
}
/**
 * Asserts that the layout of this node has width that is greater ot equal to [expectedMinWidth].
 *
 * @throws AssertionError if comparison fails.
 */
fun SemanticsNodeInteraction.assertWidthIsAtLeast(expectedMinWidth: Dp): SemanticsNodeInteraction {
    return withBoundsInRoot {
        isAtLeastOrThrow("width", it.width, expectedMinWidth)
    }
}

/**
 * Asserts that the layout of this node has height that is greater ot equal to [expectedMinHeight].
 *
 * @throws AssertionError if comparison fails.
 */
fun SemanticsNodeInteraction.assertHeightIsAtLeast(
    expectedMinHeight: Dp
): SemanticsNodeInteraction {
    return withBoundsInRoot {
        isAtLeastOrThrow("height", it.height, expectedMinHeight)
    }
}

/**
* Returns the bounds of the layout of this node. The bounds are relative to the root composable.
*/
fun SemanticsNodeInteraction.getBoundsInRoot(): Bounds {
    lateinit var bounds: Bounds
    withBoundsInRoot {
        bounds = Bounds(
            left = it.left.toDp(),
            top = it.top.toDp(),
            right = it.right.toDp(),
            bottom = it.bottom.toDp()
        )
    }
    return bounds
}

/**
 * Asserts that the layout of this node has position in the root composable that is equal to the
 * given position.
 *
 * @param expectedLeft The left (x) position to assert.
 * @param expectedTop The top (y) position to assert.
 *
 * @throws AssertionError if comparison fails.
 */
fun SemanticsNodeInteraction.assertPositionInRootIsEqualTo(
    expectedLeft: Dp,
    expectedTop: Dp
): SemanticsNodeInteraction {
    return withBoundsInRoot {
        it.left.toDp().assertIsEqualTo(expectedLeft, "left")
        it.top.toDp().assertIsEqualTo(expectedTop, "top")
    }
}

/**
 * Asserts that the layout of this node has the top position in the root composable that is equal to
 * the given position.
 *
 * @param expectedTop The top (y) position to assert.
 *
 * @throws AssertionError if comparison fails.
 */
fun SemanticsNodeInteraction.assertTopPositionInRootIsEqualTo(
    expectedTop: Dp
): SemanticsNodeInteraction {
    return withBoundsInRoot {
        it.top.toDp().assertIsEqualTo(expectedTop, "top")
    }
}

/**
 * Asserts that the layout of this node has the left position in the root composable that is
 * equal to the given position.
 *
 * @param expectedTop The left (x) position to assert.
 *
 * @throws AssertionError if comparison fails.
 */
fun SemanticsNodeInteraction.assertLeftPositionInRootIsEqualTo(
    expectedTop: Dp
): SemanticsNodeInteraction {
    return withBoundsInRoot {
        it.left.toDp().assertIsEqualTo(expectedTop, "left")
    }
}

/**
 * Returns the position of an [alignment line][AlignmentLine], or [Dp.Unspecified] if the line is
 * not provided.
 */
fun SemanticsNodeInteraction.getAlignmentLinePosition(line: AlignmentLine): Dp {
    return withDensity {
        val pos = it.getAlignmentLinePosition(line)
        if (pos == AlignmentLine.Unspecified) {
            Dp.Unspecified
        } else {
            pos.toDp()
        }
    }
}

/**
 * Asserts that this value is equal to the given [expected] value.
 *
 * Performs the comparison with the given [tolerance] or the default one if none is provided. It is
 * recommended to use tolerance when comparing positions and size coming from the framework as there
 * can be rounding operation performed by individual layouts so the values can be slightly off from
 * the expected ones.
 *
 * @param expected The expected value to which this one should be equal to.
 * @param subject Used in the error message to identify which item this assertion failed on.
 * @param tolerance The tolerance within which the values should be treated as equal.
 *
 * @throws AssertionError if comparison fails.
 */
fun Dp.assertIsEqualTo(expected: Dp, subject: String = "", tolerance: Dp = Dp(.5f)) {
    val diff = (this - expected).value.absoluteValue
    if (diff > tolerance.value) {
        // Comparison failed, report the error in DPs
        throw AssertionError(
            "Actual $subject is $this, expected $expected (tolerance: $tolerance)")
    }
}

/**
 * Asserts that this value is not equal to the given [unexpected] value.
 *
 * Performs the comparison with the given [tolerance] or the default one if none is provided. It is
 * recommended to use tolerance when comparing positions and size coming from the framework as there
 * can be rounding operation performed by individual layouts so the values can be slightly off from
 * the expected ones.
 *
 * @param unexpected The value to which this one should not be equal to.
 * @param subject Used in the error message to identify which item this assertion failed on.
 * @param tolerance The tolerance that is expected to be greater than the difference between the
 * given values to treat them as non-equal.
 *
 * @throws AssertionError if comparison fails.
 */
fun Dp.assertIsNotEqualTo(unexpected: Dp, subject: String = "", tolerance: Dp = Dp(.5f)) {
    val diff = (this - unexpected).value.absoluteValue
    if (diff <= tolerance.value) {
        // Comparison failed, report the error in DPs
        throw AssertionError(
            "Actual $subject is $this, not expected to be equal to $unexpected within a " +
                    "tolerance of $tolerance")
    }
}

private val SemanticsNode.unclippedBoundsInRoot: PxBounds
    get() {
        return PxBounds(positionInRoot, size.toSize())
    }

private fun <R> SemanticsNodeInteraction.withDensity(
    operation: Density.(SemanticsNode) -> R
): R {
    val node = fetchSemanticsNode("Failed to retrieve density for the node.")
    @OptIn(ExperimentalLayoutNodeApi::class)
    val density = (node.componentNode.owner as AndroidOwner).density
    return operation.invoke(density, node)
}

private fun SemanticsNodeInteraction.withBoundsInRoot(
    assertion: Density.(PxBounds) -> Unit
): SemanticsNodeInteraction {
    val node = fetchSemanticsNode("Failed to retrieve bounds of the node.")
    @OptIn(ExperimentalLayoutNodeApi::class)
    val density = (node.componentNode.owner as AndroidOwner).density

    assertion.invoke(density, node.unclippedBoundsInRoot)
    return this
}

private fun Density.isAtLeastOrThrow(
    subject: String,
    actualPx: Float,
    expected: Dp
) {
    if (actualPx + floatTolerance < expected.toPx()) {
        // Comparison failed, report the error in DPs
        throw AssertionError(
            "Actual $subject is ${actualPx.toDp()}, expected at least $expected")
    }
}