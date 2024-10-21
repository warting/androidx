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

package androidx.compose.foundation.text.input.internal

import androidx.compose.ui.text.TextRange
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
internal class FinishComposingTextCommandTest : ImeEditCommandTest() {

    @Test
    fun test_set() {
        initialize("ABCDE", TextRange.Zero)

        imeScope.setComposingRegion(1, 4)
        imeScope.finishComposingText()

        assertThat(state.text.toString()).isEqualTo("ABCDE")
        assertThat(state.selection.start).isEqualTo(0)
        assertThat(state.selection.end).isEqualTo(0)
        assertThat(state.composition).isNull()
    }

    @Test
    fun test_preserve_selection() {
        initialize("ABCDE", TextRange(1, 4))

        imeScope.setComposingRegion(2, 5)
        imeScope.finishComposingText()

        assertThat(state.text.toString()).isEqualTo("ABCDE")
        assertThat(state.selection.start).isEqualTo(1)
        assertThat(state.selection.end).isEqualTo(4)
        assertThat(state.composition).isNull()
    }
}
