/*
 * Copyright (C) 2022 The Android Open Source Project
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

package androidx.constraintlayout.compose

import androidx.collection.IntSet
import androidx.collection.emptyIntSet
import androidx.collection.intSetOf
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.isDebugInspectorInfoEnabled
import androidx.compose.ui.test.assertPositionInRootIsEqualTo
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Tests for the Grid Helper (Row / Column) */
@MediumTest
@RunWith(AndroidJUnit4::class)
class RowColumnDslTest {
    @get:Rule val rule = createComposeRule()

    @Before
    fun setup() {
        isDebugInspectorInfoEnabled = true
    }

    @After
    fun tearDown() {
        isDebugInspectorInfoEnabled = false
    }

    @Test
    fun testColumn() {
        val rootSize = 200.dp
        val boxesCount = 4
        rule.setContent {
            ColumnComposableTest(
                modifier = Modifier.size(rootSize),
                weights = floatArrayOf(),
                skipIndices = emptyIntSet(),
                boxesCount = boxesCount,
            )
        }
        var expectedX = 0.dp
        var expectedY = 0.dp

        // 10.dp is the size of a singular box
        val hGapSize = (rootSize - 10.dp) / 2f
        val vGapSize = (rootSize - (10.dp * 4f)) / (boxesCount * 2f)
        rule.waitForIdle()
        expectedX += hGapSize
        expectedY += vGapSize
        rule.onNodeWithTag("box0").assertPositionInRootIsEqualTo(expectedX, expectedY)
        expectedY += vGapSize + vGapSize + 10.dp
        rule.onNodeWithTag("box1").assertPositionInRootIsEqualTo(expectedX, expectedY)
        expectedY += vGapSize + vGapSize + 10.dp
        rule.onNodeWithTag("box2").assertPositionInRootIsEqualTo(expectedX, expectedY)
        expectedY += vGapSize + vGapSize + 10.dp
        rule.onNodeWithTag("box3").assertPositionInRootIsEqualTo(expectedX, expectedY)
    }

    @Test
    fun testColumnSkips() {
        val rootSize = 200.dp
        val boxesCount = 4
        rule.setContent {
            ColumnComposableTest(
                modifier = Modifier.size(rootSize),
                weights = floatArrayOf(),
                // Represents `Skip(position = 1, size = 2)`
                skipIndices = intSetOf(1, 2),
                boxesCount = boxesCount,
            )
        }
        var expectedX = 0.dp
        var expectedY = 0.dp

        // 10.dp is the size of a singular box
        val hGapSize = (rootSize - 10.dp) / 2f
        val vGapSize = (rootSize - (10.dp * 6f)) / ((boxesCount + 2) * 2f)
        rule.waitForIdle()
        expectedX += hGapSize
        expectedY += vGapSize
        rule.onNodeWithTag("box0").assertPositionInRootIsEqualTo(expectedX, expectedY)
        expectedY += vGapSize + vGapSize + 10.dp
        expectedY += vGapSize + vGapSize + 10.dp
        expectedY += vGapSize + vGapSize + 10.dp
        rule.onNodeWithTag("box1").assertPositionInRootIsEqualTo(expectedX, expectedY)
        expectedY += vGapSize + vGapSize + 10.dp
        rule.onNodeWithTag("box2").assertPositionInRootIsEqualTo(expectedX, expectedY)
        expectedY += vGapSize + vGapSize + 10.dp
        rule.onNodeWithTag("box3").assertPositionInRootIsEqualTo(expectedX, expectedY)
    }

    @Test
    fun testColumnSpans() {
        val rootSize = 200.dp
        val boxesCount = 4
        rule.setContent {
            ColumnComposableTest(
                modifier = Modifier.size(rootSize),
                weights = floatArrayOf(2f, 1f, 1f, 1f),
                skipIndices = emptyIntSet(),
                boxesCount = boxesCount,
            )
        }

        // 10.dp is the size of a singular box
        val hGapSize = (rootSize - 10.dp) / 2f
        val vGapSize = (rootSize - (10.dp * 5f)) / ((boxesCount + 1) * 2f)
        val rowSize = 10.dp + vGapSize * 2
        var expectedX = 0.dp
        var expectedY = rowSize - 5.dp

        expectedX += hGapSize
        rule.onNodeWithTag("box0").assertPositionInRootIsEqualTo(expectedX, expectedY)
        expectedY += expectedY + vGapSize + 10.dp
        rule.onNodeWithTag("box1").assertPositionInRootIsEqualTo(expectedX, expectedY)
        expectedY += vGapSize + vGapSize + 10.dp
        rule.onNodeWithTag("box2").assertPositionInRootIsEqualTo(expectedX, expectedY)
        expectedY += vGapSize + vGapSize + 10.dp
        rule.onNodeWithTag("box3").assertPositionInRootIsEqualTo(expectedX, expectedY)
    }

    @Test
    fun testRow() {
        val rootSize = 200.dp
        val boxesCount = 4
        rule.setContent {
            RowComposableTest(
                modifier = Modifier.size(rootSize),
                weights = floatArrayOf(),
                skipIndices = emptyIntSet(),
                boxesCount = boxesCount,
            )
        }
        var expectedX = 0.dp
        var expectedY = 0.dp

        // 10.dp is the size of a singular box
        val hGapSize = (rootSize - (10.dp * 4f)) / (boxesCount * 2f)
        val vGapSize = (rootSize - 10.dp) / 2f
        rule.waitForIdle()
        expectedX += hGapSize
        expectedY += vGapSize
        rule.onNodeWithTag("box0").assertPositionInRootIsEqualTo(expectedX, expectedY)
        expectedX += hGapSize + hGapSize + 10.dp
        rule.onNodeWithTag("box1").assertPositionInRootIsEqualTo(expectedX, expectedY)
        expectedX += hGapSize + hGapSize + 10.dp
        rule.onNodeWithTag("box2").assertPositionInRootIsEqualTo(expectedX, expectedY)
        expectedX += hGapSize + hGapSize + 10.dp
        rule.onNodeWithTag("box3").assertPositionInRootIsEqualTo(expectedX, expectedY)
    }

    @Test
    fun testRowSkips() {
        val rootSize = 200.dp
        val boxesCount = 4
        rule.setContent {
            RowComposableTest(
                modifier = Modifier.size(rootSize),
                weights = floatArrayOf(),
                // Represents `Skip(position = 1, size = 2)`
                skipIndices = intSetOf(1, 2),
                boxesCount = boxesCount,
            )
        }
        var expectedX = 0.dp
        var expectedY = 0.dp

        // 10.dp is the size of a singular box
        val hGapSize = (rootSize - (10.dp * 6f)) / ((boxesCount + 2) * 2f)
        val vGapSize = (rootSize - 10.dp) / 2f
        rule.waitForIdle()
        expectedX += hGapSize
        expectedY += vGapSize
        rule.onNodeWithTag("box0").assertPositionInRootIsEqualTo(expectedX, expectedY)
        expectedX += hGapSize + hGapSize + 10.dp
        expectedX += hGapSize + hGapSize + 10.dp
        expectedX += hGapSize + hGapSize + 10.dp
        rule.onNodeWithTag("box1").assertPositionInRootIsEqualTo(expectedX, expectedY)
        expectedX += hGapSize + hGapSize + 10.dp
        rule.onNodeWithTag("box2").assertPositionInRootIsEqualTo(expectedX, expectedY)
        expectedX += hGapSize + hGapSize + 10.dp
        rule.onNodeWithTag("box3").assertPositionInRootIsEqualTo(expectedX, expectedY)
    }

    @Test
    fun testRowSpans() {
        val rootSize = 200.dp
        val boxesCount = 4
        rule.setContent {
            RowComposableTest(
                modifier = Modifier.size(rootSize),
                weights = floatArrayOf(2f, 1f, 1f, 1f),
                skipIndices = emptyIntSet(),
                boxesCount = boxesCount,
            )
        }

        // 10.dp is the size of a singular box
        val hGapSize = (rootSize - (10.dp * 5f)) / ((boxesCount + 1) * 2f)
        val vGapSize = (rootSize - 10.dp) / 2f
        val columnSize = 10.dp + hGapSize * 2
        var expectedX = columnSize - 5.dp
        var expectedY = 0.dp

        expectedY += vGapSize

        rule.onNodeWithTag("box0").assertPositionInRootIsEqualTo(expectedX, expectedY)
        expectedX += expectedX + hGapSize + 10.dp
        rule.onNodeWithTag("box1").assertPositionInRootIsEqualTo(expectedX, expectedY)
        expectedX += hGapSize + hGapSize + 10.dp
        rule.onNodeWithTag("box2").assertPositionInRootIsEqualTo(expectedX, expectedY)
        expectedX += hGapSize + hGapSize + 10.dp
        rule.onNodeWithTag("box3").assertPositionInRootIsEqualTo(expectedX, expectedY)
    }

    @Test
    fun testInconsistentWeightsOnColumnTrows() {
        val count = 3
        val error =
            assertFailsWith<IllegalArgumentException> {
                rule.setContent {
                    ColumnComposableTest(
                        modifier = Modifier.size(100.dp),
                        boxesCount = count,
                        // Insufficient weights should throw
                        weights = FloatArray(count - 1) { it.toFloat() },
                        skipIndices = emptyIntSet(),
                    )
                }
            }
        assertEquals("Number of weights (2) should match number of elements (3).", error.message)
    }

    @Test
    fun testInconsistentWeightsOnRowTrows() {
        val count = 3
        val error =
            assertFailsWith<IllegalArgumentException> {
                rule.setContent {
                    RowComposableTest(
                        modifier = Modifier.size(100.dp),
                        boxesCount = count,
                        // Excessive weights should throw
                        weights = FloatArray(count + 1) { it.toFloat() },
                        skipIndices = emptyIntSet(),
                    )
                }
            }
        assertEquals("Number of weights (4) should match number of elements (3).", error.message)
    }

    @Composable
    private fun ColumnComposableTest(
        modifier: Modifier = Modifier,
        weights: FloatArray,
        skipIndices: IntSet,
        boxesCount: Int,
    ) {
        ConstraintLayout(
            ConstraintSet {
                // We consider the skips as fake boxes, so we need them as references
                val totalBoxes = boxesCount + skipIndices.size
                // Track "skipped" boxes to properly assign all incremental `id` to the boxes
                var skipCount = 0

                val ids =
                    Array(totalBoxes) { index ->
                        if (skipIndices.contains(index)) {
                            skipCount++
                            createRefFor("placeholder")
                        } else {
                            createRefFor("box${index - skipCount}")
                        }
                    }

                val g1 = createColumn(elements = ids, weights = weights)

                constrain(g1) {
                    width = Dimension.matchParent
                    height = Dimension.matchParent
                }
            },
            modifier = modifier,
        ) {
            repeat(boxesCount) {
                Box(Modifier.layoutTestId("box$it").background(Color.Red).size(10.dp))
            }
        }
    }

    @Composable
    private fun RowComposableTest(
        modifier: Modifier = Modifier,
        weights: FloatArray,
        skipIndices: IntSet,
        boxesCount: Int,
    ) {
        ConstraintLayout(
            ConstraintSet {
                // We consider the skips as fake boxes, so we need them as references
                val totalBoxes = boxesCount + skipIndices.size
                // Track "skipped" boxes to properly assign all incremental `id` to the boxes
                var skipCount = 0

                val ids =
                    Array(totalBoxes) { index ->
                        if (skipIndices.contains(index)) {
                            skipCount++
                            createRefFor("placeholder")
                        } else {
                            createRefFor("box${index - skipCount}")
                        }
                    }

                val g1 = createRow(elements = ids, weights = weights)
                constrain(g1) {
                    width = Dimension.matchParent
                    height = Dimension.matchParent
                }
            },
            modifier = modifier,
        ) {
            repeat(boxesCount) {
                Box(Modifier.layoutTestId("box$it").background(Color.Red).size(10.dp))
            }
        }
    }
}
