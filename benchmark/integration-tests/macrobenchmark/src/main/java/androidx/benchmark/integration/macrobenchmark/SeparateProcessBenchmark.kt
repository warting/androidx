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

package androidx.benchmark.integration.macrobenchmark

import android.content.Intent
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Validates metrics coming from an app's sub-process. */
@LargeTest
@RunWith(AndroidJUnit4::class)
class SeparateProcessBenchmark {
    @get:Rule val benchmarkRule = MacrobenchmarkRule()

    @Test
    fun startup() =
        benchmarkRule.measureRepeated(
            compilationMode = CompilationMode.DEFAULT,
            startupMode = StartupMode.COLD,
            packageName = "androidx.benchmark.integration.macrobenchmark.target",
            metrics = listOf(StartupTimingMetric(), FrameTimingMetric()),
            iterations = 2,
        ) {
            startActivityAndWait(Intent("$packageName.SEPARATE_PROCESS_ACTIVITY"))
        }
}
