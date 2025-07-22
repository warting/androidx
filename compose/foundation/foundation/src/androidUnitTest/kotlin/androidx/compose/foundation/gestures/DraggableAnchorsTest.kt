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

package androidx.compose.foundation.gestures

import androidx.compose.foundation.gestures.TestValue.A
import androidx.compose.foundation.gestures.TestValue.B
import androidx.compose.foundation.gestures.TestValue.C
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class DraggableAnchorsTest {

    @Test
    fun draggableAnchors_builder_containsAll() {
        val anchors = DraggableAnchors {
            A at 0f
            B at 100f
            C at 200f
        }
        assertThat(anchors.positionOf(A)).isEqualTo(0f)
        assertThat(anchors.positionOf(B)).isEqualTo(100f)
        assertThat(anchors.positionOf(C)).isEqualTo(200f)
    }

    @Test
    fun draggableAnchors_get_nonexistentAnchor_returnsNaN() {
        val anchors = DraggableAnchors<TestValue> {}
        assertThat(anchors.positionOf(A)).isNaN()
    }

    @Test
    fun draggableAnchors_findsClosestAnchor() {
        val anchors = DraggableAnchors {
            A at 0f
            B at 100f
            C at 200f
        }
        assertThat(anchors.closestAnchor(25f)).isEqualTo(A)
        assertThat(anchors.closestAnchor(75f)).isEqualTo(B)
        assertThat(anchors.closestAnchor(175f)).isEqualTo(C)
    }

    @Test
    fun draggableAnchors_findsClosestAnchor_directional() {
        val anchors = DraggableAnchors {
            A at 0f
            B at 100f
        }
        assertThat(anchors.closestAnchor(1f, searchUpwards = true)).isEqualTo(B)
        assertThat(anchors.closestAnchor(99f, searchUpwards = false)).isEqualTo(A)
    }

    @Test
    fun draggableAnchors_closestAnchor_returnsNullWhenEmpty() {
        val anchors = DraggableAnchors<Float> {}
        assertWithMessage(
                "closestAnchor(<any>, searchUpwards=true) should be null when anchors" +
                    " are empty"
            )
            .that(anchors.closestAnchor(1f, searchUpwards = true))
            .isNull()
        assertWithMessage(
                "closestAnchor(<any>, searchUpwards=false) should be null when anchors" +
                    " are empty"
            )
            .that(anchors.closestAnchor(1f, searchUpwards = false))
            .isNull()
        assertWithMessage("closestAnchor(<any>) should be null when anchors are empty")
            .that(anchors.closestAnchor(1f))
            .isNull()
    }

    @Test
    fun draggableAnchors_minPosition() {
        val anchors = DraggableAnchors {
            A at -100f
            B at 100f
        }
        assertThat(anchors.minPosition()).isEqualTo(-100f)
    }

    @Test
    fun draggableAnchors_maxPosition() {
        val anchors = DraggableAnchors {
            A at -100f
            B at 100f
        }
        assertThat(anchors.maxPosition()).isEqualTo(100f)
    }

    @Test
    fun draggableAnchors_hasPositionFor() {
        val anchors = DraggableAnchors { A at 100f }
        assertThat(anchors.positionOf(A)).isEqualTo(100f)
        assertThat(anchors.hasPositionFor(A)).isTrue()
    }

    @Test
    fun draggableAnchors_equality_equalAnchors() {
        val anchors1 = DraggableAnchors { A at 100f }
        val anchors2 = DraggableAnchors { A at 100f }
        assertThat(anchors1).isEqualTo(anchors2)
    }

    @Test
    fun draggableAnchors_equality_inequalAnchors() {
        val anchors1 = DraggableAnchors { A at 100f }
        val anchors2 = DraggableAnchors { B at 100f }
        assertThat(anchors1).isNotEqualTo(anchors2)
    }

    @Test
    fun draggableAnchors_equality_differentObject() {
        val anchors = DraggableAnchors { A at 100f }
        assertThat(anchors).isNotEqualTo("Test")
    }
}

private enum class TestValue {
    A,
    B,
    C,
}
