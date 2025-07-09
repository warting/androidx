/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.xr.runtime.math

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class FloatSize3dTest {
    @Test
    fun equals_withIdenticalValues_returnsTrue() {
        val size1 = FloatSize3d(width = 1.0f, height = 2.0f, depth = 3.0f)
        var size2 = FloatSize3d(width = 1.0f, height = 2.0f, depth = 3.0f)

        assertThat(size1.width).isEqualTo(size2.width)
        assertThat(size1.height).isEqualTo(size2.height)
        assertThat(size1.depth).isEqualTo(size2.depth)
        assertThat(size1).isEqualTo(size2)
    }

    @Test
    fun equals_withDifferentValues_returnsFalse() {
        val size1 = FloatSize3d(width = 1.0f, height = 1.0f, depth = 1.0f)
        val size2 = FloatSize3d(width = 2.0f, height = 2.0f, depth = 2.0f)

        assertThat(size1.width).isNotEqualTo(size2.width)
        assertThat(size1.height).isNotEqualTo(size2.height)
        assertThat(size1.depth).isNotEqualTo(size2.depth)
        assertThat(size1).isNotEqualTo(size2)
    }

    @Test
    fun hashCode_isConsistentForEqualValues() {
        val size1 = FloatSize3d(width = 1.0f, height = 1.0f, depth = 1.0f)
        val size2 = FloatSize3d(width = 1.0f, height = 1.0f, depth = 1.0f)

        assertThat(size1.hashCode()).isEqualTo(size2.hashCode())
    }

    @Test
    fun hashCode_isDifferentForUnequalObjects() {
        val size1 = FloatSize3d(width = 1.0f, height = 1.0f, depth = 1.0f)
        val size2 = FloatSize3d(width = 2.0f, height = 2.0f, depth = 2.0f)

        assertThat(size1.hashCode()).isNotEqualTo(size2.hashCode())
    }

    @Test
    fun toString_returnsCorrectFormat() {
        val size = FloatSize3d(width = 1.0f, height = 2.0f, depth = 3.0f)

        val repr = "$size"

        assertThat(repr).isEqualTo(size.toString())
    }

    @Test
    fun to2d_returnsCorrectValues() {
        val size1 = FloatSize3d(width = 1.0f, height = 2.0f, depth = 3.0f)

        val size2 = size1.to2d()

        assertThat(size1.width).isEqualTo(size2.width)
        assertThat(size1.height).isEqualTo(size2.height)
    }
}
