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

package androidx.compose.foundation.layout

import androidx.compose.ui.unit.IntRect
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class InsetsHelperTest {
    @Test
    fun intRectToAndroidXInsetsConverts() {
        assertEquals(
            androidx.core.graphics.Insets.of(5, 6, 7, 8),
            IntRect(5, 6, 7, 8).toAndroidXInsets(),
        )
    }

    @Test
    fun androidXInsetsToOntRectConverts() {
        assertEquals(
            IntRect(5, 6, 7, 8),
            androidx.core.graphics.Insets.of(5, 6, 7, 8).toComposeIntRect(),
        )
    }
}
