/*
 * Copyright (C) 2025 The Android Open Source Project
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

package androidx.ink.strokes

import androidx.ink.brush.InputToolType
import androidx.ink.geometry.AffineTransform
import androidx.ink.geometry.ImmutableBox
import androidx.ink.geometry.ImmutableVec
import androidx.ink.geometry.Intersection.intersects
import com.google.common.truth.Truth.assertThat
import kotlin.collections.listOf
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class MeshCreationTest {

    private fun createStrokeInputBatch(points: List<ImmutableVec>): StrokeInputBatch {
        val strokeInputBatch = MutableStrokeInputBatch()
        var count = 0
        for (point in points) {
            strokeInputBatch.add(
                type = InputToolType.STYLUS,
                x = point.x,
                y = point.y,
                elapsedTimeMillis = 5L * count++,
            )
        }
        return strokeInputBatch
    }

    @Test
    fun createClosedShapeFromStrokeInputBatch_square_intersectsCorrectPoints() {
        val strokeInputBatch =
            createStrokeInputBatch(
                listOf(
                    ImmutableVec(1f, 1f),
                    ImmutableVec(9f, 1f),
                    ImmutableVec(9f, 9f),
                    ImmutableVec(1f, 9f),
                    ImmutableVec(1f, 1f),
                )
            )

        val mesh = strokeInputBatch.createClosedShape()

        assertThat(mesh.intersects(ImmutableVec(2f, 2f), AffineTransform.IDENTITY)).isTrue()
        assertThat(mesh.intersects(ImmutableVec(4f, 2f), AffineTransform.IDENTITY)).isTrue()
        assertThat(mesh.intersects(ImmutableVec(4f, 4f), AffineTransform.IDENTITY)).isTrue()
        assertThat(mesh.intersects(ImmutableVec(2f, 4f), AffineTransform.IDENTITY)).isTrue()

        assertThat(mesh.intersects(ImmutableVec(0f, 0f), AffineTransform.IDENTITY)).isFalse()
        assertThat(mesh.intersects(ImmutableVec(10f, 0f), AffineTransform.IDENTITY)).isFalse()
        assertThat(mesh.intersects(ImmutableVec(10f, 10f), AffineTransform.IDENTITY)).isFalse()
        assertThat(mesh.intersects(ImmutableVec(0f, 10f), AffineTransform.IDENTITY)).isFalse()
    }

    @Test
    fun createClosedShapeFromStrokeInputBatch_triangle_intersectsCorrectPoints() {
        val strokeInputBatch =
            createStrokeInputBatch(
                listOf(
                    ImmutableVec(-1f, -1f),
                    ImmutableVec(-90f, -90f),
                    ImmutableVec(-90f, -1f),
                    ImmutableVec(-1f, -1f),
                )
            )

        val mesh = strokeInputBatch.createClosedShape()

        assertThat(mesh.intersects(ImmutableVec(-3f, -1.5f), AffineTransform.IDENTITY)).isTrue()
        assertThat(mesh.intersects(ImmutableVec(-85f, -50f), AffineTransform.IDENTITY)).isTrue()
        assertThat(mesh.intersects(ImmutableVec(-89f, -1.1f), AffineTransform.IDENTITY)).isTrue()
        assertThat(mesh.intersects(ImmutableVec(-9f, -8f), AffineTransform.IDENTITY)).isTrue()

        assertThat(mesh.intersects(ImmutableVec(0f, 0f), AffineTransform.IDENTITY)).isFalse()
        assertThat(mesh.intersects(ImmutableVec(5f, -2f), AffineTransform.IDENTITY)).isFalse()
        assertThat(mesh.intersects(ImmutableVec(-5f, 2f), AffineTransform.IDENTITY)).isFalse()
        assertThat(mesh.intersects(ImmutableVec(-91f, -10f), AffineTransform.IDENTITY)).isFalse()
    }

    @Test
    fun createClosedShapeFromStrokeInputBatch_onePoint_createsPointLikeMesh() {
        val strokeInputBatch = createStrokeInputBatch(listOf(ImmutableVec(-90f, -90f)))

        val mesh = strokeInputBatch.createClosedShape()

        assertThat(mesh.intersects(ImmutableVec(-90f, -90f), AffineTransform.IDENTITY)).isTrue()
        assertThat(mesh.computeBoundingBox())
            .isEqualTo(
                ImmutableBox.fromTwoPoints(ImmutableVec(-90f, -90f), ImmutableVec(-90f, -90f))
            )
    }

    @Test
    fun createClosedShapeFromStrokeInputBatch_manyIdentiticalPoints_createsPointLikeMesh() {
        val strokeInputBatch =
            createStrokeInputBatch(
                listOf(
                    ImmutableVec(35f, 85f),
                    ImmutableVec(35f, 85f),
                    ImmutableVec(35f, 85f),
                    ImmutableVec(35f, 85f),
                    ImmutableVec(35f, 85f),
                    ImmutableVec(35f, 85f),
                    ImmutableVec(35f, 85f),
                )
            )

        val mesh = strokeInputBatch.createClosedShape()

        assertThat(mesh.intersects(ImmutableVec(35f, 85f), AffineTransform.IDENTITY)).isTrue()
        assertThat(mesh.computeBoundingBox())
            .isEqualTo(ImmutableBox.fromTwoPoints(ImmutableVec(35f, 85f), ImmutableVec(35f, 85f)))
    }

    @Test
    fun createClosedShapeFromStrokeInputBatch_twoPoints_createsSegmentLikeMesh() {
        val strokeInputBatch =
            createStrokeInputBatch(listOf(ImmutableVec(-1f, -1f), ImmutableVec(-1f, 99f)))

        val mesh = strokeInputBatch.createClosedShape()

        assertThat(mesh.intersects(ImmutableVec(-1f, -1f), AffineTransform.IDENTITY)).isTrue()
        assertThat(mesh.intersects(ImmutableVec(-1f, 99f), AffineTransform.IDENTITY)).isTrue()
        assertThat(mesh.intersects(ImmutableVec(-1f, 50f), AffineTransform.IDENTITY)).isTrue()
        assertThat(mesh.computeBoundingBox())
            .isEqualTo(ImmutableBox.fromTwoPoints(ImmutableVec(-1f, -1f), ImmutableVec(-1f, 99f)))
    }

    @Test
    fun createClosedShapeFromStrokeInputBatch_twoRepeatedPoints_createsSegmentLikeMesh() {
        val strokeInputBatch =
            createStrokeInputBatch(
                listOf(
                    ImmutableVec(80f, 1f),
                    ImmutableVec(80f, 1f),
                    ImmutableVec(80f, 1f),
                    ImmutableVec(80f, 1f),
                    ImmutableVec(80f, 1f),
                    ImmutableVec(1f, 1f),
                    ImmutableVec(1f, 1f),
                    ImmutableVec(1f, 1f),
                    ImmutableVec(1f, 1f),
                    ImmutableVec(1f, 1f),
                    ImmutableVec(1f, 1f),
                    ImmutableVec(1f, 1f),
                    ImmutableVec(1f, 1f),
                )
            )

        val mesh = strokeInputBatch.createClosedShape()

        assertThat(mesh.intersects(ImmutableVec(80f, 1f), AffineTransform.IDENTITY)).isTrue()
        assertThat(mesh.intersects(ImmutableVec(40f, 1f), AffineTransform.IDENTITY)).isTrue()
        assertThat(mesh.intersects(ImmutableVec(1f, 1f), AffineTransform.IDENTITY)).isTrue()
        assertThat(mesh.computeBoundingBox())
            .isEqualTo(ImmutableBox.fromTwoPoints(ImmutableVec(80f, 1f), ImmutableVec(1f, 1f)))
    }

    @Test
    fun createClosedShapeFromStrokeInputBatch_colinearPointsWithPositiveSlope_createsSegmentLikeMesh() {
        // ^
        // |   x
        // |
        // | x
        // |x
        // 0------->
        val strokeInputBatchPositiveSlope =
            createStrokeInputBatch(
                listOf(ImmutableVec(1f, 1f), ImmutableVec(2f, 2f), ImmutableVec(4f, 4f))
            )
        val mesh = strokeInputBatchPositiveSlope.createClosedShape()
        // co-linear points within the stroke
        assertThat(mesh.intersects(ImmutableVec(3f, 3f), AffineTransform.IDENTITY)).isTrue()
        assertThat(mesh.intersects(ImmutableVec(1.5f, 1.5f), AffineTransform.IDENTITY)).isTrue()

        // co-linear points beyond the end of the stroke
        assertThat(mesh.intersects(ImmutableVec(4.5f, 4.5f), AffineTransform.IDENTITY)).isFalse()
        assertThat(mesh.intersects(ImmutableVec(0.8f, 0.8f), AffineTransform.IDENTITY)).isFalse()
        // point above the stroke
        assertThat(mesh.intersects(ImmutableVec(2f, 2.2f), AffineTransform.IDENTITY)).isFalse()
        // point below the stroke
        assertThat(mesh.intersects(ImmutableVec(2f, 1.8f), AffineTransform.IDENTITY)).isFalse()
    }

    @Test
    fun createClosedShapeFromStrokeInputBatch_colinearPointsWithNegativeSlope_createsSegmentLikeMesh() {
        // ^
        // |x
        // |
        // |  x
        // |   x
        // 0------->
        val strokeInputBatchNegativeSlope =
            createStrokeInputBatch(
                listOf(ImmutableVec(4f, 1f), ImmutableVec(2f, 3f), ImmutableVec(1f, 4f))
            )
        val mesh = strokeInputBatchNegativeSlope.createClosedShape()
        // co-linear points within the stroke
        assertThat(mesh.intersects(ImmutableVec(2f, 3f), AffineTransform.IDENTITY)).isTrue()
        assertThat(mesh.intersects(ImmutableVec(1.5f, 3.5f), AffineTransform.IDENTITY)).isTrue()
        // co-linear points beyond the end of the stroke
        assertThat(mesh.intersects(ImmutableVec(0.5f, 4.5f), AffineTransform.IDENTITY)).isFalse()
        assertThat(mesh.intersects(ImmutableVec(4.5f, 0.5f), AffineTransform.IDENTITY)).isFalse()
        // point above the stroke
        assertThat(mesh.intersects(ImmutableVec(2f, 3.2f), AffineTransform.IDENTITY)).isFalse()
        // point below the stroke
        assertThat(mesh.intersects(ImmutableVec(2f, 2.8f), AffineTransform.IDENTITY)).isFalse()
    }

    @Test
    fun createClosedShapeFromStrokeInputBatch_colinearPointsWithInfiniteSlope_createsSegmentLikeMesh() {
        // |
        // |x
        // |
        // |x
        // |x
        // 0------->
        val strokeInputBatchInfiniteSlope =
            createStrokeInputBatch(
                listOf(ImmutableVec(1f, 1f), ImmutableVec(1f, 2f), ImmutableVec(1f, 4f))
            )
        val mesh = strokeInputBatchInfiniteSlope.createClosedShape()
        // co-linear points within the stroke
        assertThat(mesh.intersects(ImmutableVec(1f, 3f), AffineTransform.IDENTITY)).isTrue()
        assertThat(mesh.intersects(ImmutableVec(1f, 1.5f), AffineTransform.IDENTITY)).isTrue()
        // co-linear points beyond the end of the stroke
        assertThat(mesh.intersects(ImmutableVec(1f, 0.9f), AffineTransform.IDENTITY)).isFalse()
        assertThat(mesh.intersects(ImmutableVec(1f, 4.2f), AffineTransform.IDENTITY)).isFalse()
        // point left of the stroke
        assertThat(mesh.intersects(ImmutableVec(0.8f, 1.8f), AffineTransform.IDENTITY)).isFalse()
        // point right of the stroke
        assertThat(mesh.intersects(ImmutableVec(1.2f, 3f), AffineTransform.IDENTITY)).isFalse()
    }

    @Test
    fun createClosedShapeFromStrokeInputBatch_colinearPointsWithZeroSlope_createsSegmentLikeMesh() {
        // ^
        // |
        // |
        // |
        // |xxx
        // 0------->
        val strokeInputBatchInfiniteSlope =
            createStrokeInputBatch(
                listOf(ImmutableVec(1f, 1f), ImmutableVec(2f, 1f), ImmutableVec(3f, 1f))
            )
        val mesh = strokeInputBatchInfiniteSlope.createClosedShape()
        // co-linear points within the stroke
        assertThat(mesh.intersects(ImmutableVec(2.5f, 1f), AffineTransform.IDENTITY)).isTrue()
        assertThat(mesh.intersects(ImmutableVec(3f, 1f), AffineTransform.IDENTITY)).isTrue()
        // co-linear points beyond the end of the stroke
        assertThat(mesh.intersects(ImmutableVec(4f, 1f), AffineTransform.IDENTITY)).isFalse()
        assertThat(mesh.intersects(ImmutableVec(0.5f, 1f), AffineTransform.IDENTITY)).isFalse()
        // point above the stroke
        assertThat(mesh.intersects(ImmutableVec(2f, 1.2f), AffineTransform.IDENTITY)).isFalse()
        // point below the stroke
        assertThat(mesh.intersects(ImmutableVec(2f, 0.8f), AffineTransform.IDENTITY)).isFalse()
    }
}
