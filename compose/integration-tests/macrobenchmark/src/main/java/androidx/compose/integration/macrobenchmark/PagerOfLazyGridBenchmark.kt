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

package androidx.compose.integration.macrobenchmark

import android.content.Intent
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.ExperimentalMetricApi
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.TraceSectionMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.compose.integration.macrobenchmark.FormFillingBenchmark.Companion.COMPOSE_APPLY_CHANGES
import androidx.compose.integration.macrobenchmark.FormFillingBenchmark.Companion.CONTENT_CAPTURE_CHANGE_CHECKER
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import androidx.testutils.createCompilationParams
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@LargeTest
@RunWith(Parameterized::class)
class PagerOfLazyGridBenchmark(private val compilationMode: CompilationMode) {
    @get:Rule val benchmarkRule = MacrobenchmarkRule()

    private lateinit var device: UiDevice

    @Before
    fun setUp() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        device = UiDevice.getInstance(instrumentation)
    }

    @Test
    fun scroll() {
        benchmarkRule.measureRepeated(
            packageName = PackageName,
            metrics =
                @OptIn(ExperimentalMetricApi::class)
                listOf(
                    FrameTimingMetric(),
                    TraceSectionMetric(
                        sectionName = CONTENT_CAPTURE_CHANGE_CHECKER,
                        mode = TraceSectionMetric.Mode.Sum
                    ),
                    TraceSectionMetric(
                        sectionName = COMPOSE_APPLY_CHANGES,
                        mode = TraceSectionMetric.Mode.Sum
                    )
                ),
            compilationMode = compilationMode,
            startupMode = StartupMode.WARM,
            iterations = 10,
            setupBlock = {
                val intent = Intent()
                intent.action = Action
                startActivityAndWait(intent)
            }
        ) {
            val nextButton = device.findObject(By.text(NextDescription))
            repeat(3) {
                nextButton.click()
                device.wait(Until.findObject(By.desc(ComposeIdle)), 3000)
            }
        }
    }

    companion object {
        private const val PackageName = "androidx.compose.integration.macrobenchmark.target"
        private const val Action =
            "androidx.compose.integration.macrobenchmark.target.PAGER_LAZYGRID_ACTIVITY"
        private const val ComposeIdle = "COMPOSE-IDLE"
        private const val NextDescription = "Next"

        @Parameterized.Parameters(name = "compilation={0}")
        @JvmStatic
        fun parameters() = createCompilationParams()
    }
}
