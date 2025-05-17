/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.glance.wear.tiles

import android.content.Context
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.glance.layout.size
import androidx.glance.text.Text
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertIs
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class, ExperimentalGlanceWearTilesApi::class)
@RunWith(RobolectricTestRunner::class)
class GlanceWearTilesTest {
    private lateinit var fakeCoroutineScope: TestScope

    @Before
    fun setUp() {
        fakeCoroutineScope = TestScope(UnconfinedTestDispatcher())
    }

    @Test
    @Suppress("deprecation") // For backwards compatibility.
    fun createEmptyUi() =
        fakeCoroutineScope.runTest {
            val compositionResult =
                compose(
                    context = ApplicationProvider.getApplicationContext<Context>(),
                    size = DpSize(100.dp, 50.dp),
                ) {}
            assertIs<androidx.wear.tiles.LayoutElementBuilders.Box>(compositionResult.layout)
            assertThat(
                    (compositionResult.layout as androidx.wear.tiles.LayoutElementBuilders.Box)
                        .contents
                )
                .isEmpty()
        }

    @Test
    @Suppress("deprecation") // For backwards compatibility.
    fun createSimpleWearTiles() =
        fakeCoroutineScope.runTest {
            val compositionResult =
                compose(
                    context = ApplicationProvider.getApplicationContext<Context>(),
                    size = DpSize(100.dp, 50.dp),
                ) {
                    Text("text content")
                }

            val box =
                assertIs<androidx.wear.tiles.LayoutElementBuilders.Box>(compositionResult.layout)
            assertThat(box.contents).hasSize(1)
            val text = assertIs<androidx.wear.tiles.LayoutElementBuilders.Text>(box.contents[0])
            assertThat(text.text!!.value).isEqualTo("text content")
        }
}
