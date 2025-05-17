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

package androidx.testutils

import androidx.benchmark.Outputs
import androidx.benchmark.macro.ExperimentalMetricApi
import androidx.benchmark.macro.Metric
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.assertEqualMeasurements
import androidx.benchmark.macro.runSingleSessionServer
import androidx.benchmark.perfetto.PerfettoHelper
import androidx.benchmark.traceprocessor.TraceProcessor
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import org.junit.Assume.assumeTrue
import org.junit.Test

class SdkSandboxCrossProcessLatencyMetricTest {
    @OptIn(ExperimentalMetricApi::class)
    @Test
    fun fixedTrace35() {
        assumeTrue(PerfettoHelper.isAbiSupported())
        val traceFile = createTempFileFromAsset("api35_cross_process_latency", ".perfetto-trace")
        val captureInfo =
            Metric.CaptureInfo(
                targetPackageName = "androidx.privacysandbox.ui.integration.testapp",
                testPackageName = "androidx.privacysandbox.ui.integration.testapp",
                startupMode = StartupMode.COLD,
                apiLevel = 35,
            )

        val e2eLatencyMetric =
            SdkSandboxCrossProcessLatencyMetric("checkClientOpenSession", "onUiDisplayed", "e2e")
        val openSessionMediateeLatencyMetric =
            SdkSandboxCrossProcessLatencyMetric(
                "checkClientOpenSession",
                "checkClientOpenSession",
                "openSessionMediatee",
                occurrenceOfEndTrace = SdkSandboxCrossProcessLatencyMetric.TraceOccurrence.LAST,
            )
        val uiDisplayLatencyMetric =
            SdkSandboxCrossProcessLatencyMetric(
                "checkClientOpenSession",
                "onUiDisplayed",
                "uiDisplay",
                occurrenceOfBeginTrace = SdkSandboxCrossProcessLatencyMetric.TraceOccurrence.LAST,
            )
        val crossProcessUiDisplayLatencyMetric =
            SdkSandboxCrossProcessLatencyMetric(
                "sdkOpenSession",
                "onUiDisplayed",
                "crossProcessUiDisplay",
            )

        val result1 =
            TraceProcessor.runSingleSessionServer(traceFile.absolutePath) {
                e2eLatencyMetric.getMeasurements(captureInfo = captureInfo, traceSession = this)
            }

        val result2 =
            TraceProcessor.runSingleSessionServer(traceFile.absolutePath) {
                openSessionMediateeLatencyMetric.getMeasurements(
                    captureInfo = captureInfo,
                    traceSession = this,
                )
            }

        val result3 =
            TraceProcessor.runSingleSessionServer(traceFile.absolutePath) {
                uiDisplayLatencyMetric.getMeasurements(
                    captureInfo = captureInfo,
                    traceSession = this,
                )
            }

        val result4 =
            TraceProcessor.runSingleSessionServer(traceFile.absolutePath) {
                crossProcessUiDisplayLatencyMetric.getMeasurements(
                    captureInfo = captureInfo,
                    traceSession = this,
                )
            }

        assertEqualMeasurements(
            expected = listOf(Metric.Measurement("e2eLatencyMillis", 270.9)),
            observed = result1,
            threshold = 0.1,
        )
        assertEqualMeasurements(
            expected = listOf(Metric.Measurement("openSessionMediateeLatencyMillis", 72.0)),
            observed = result2,
            threshold = 0.1,
        )
        assertEqualMeasurements(
            expected = listOf(Metric.Measurement("uiDisplayLatencyMillis", 199.0)),
            observed = result3,
            threshold = 0.1,
        )
        assertEqualMeasurements(
            expected = listOf(Metric.Measurement("crossProcessUiDisplayLatencyMillis", 251.6)),
            observed = result4,
            threshold = 0.1,
        )
    }

    fun createTempFileFromAsset(prefix: String, suffix: String): File {
        val file = File.createTempFile(prefix, suffix, Outputs.dirUsableByAppAndShell)
        InstrumentationRegistry.getInstrumentation()
            .context
            .assets
            .open(prefix + suffix)
            .copyTo(file.outputStream())
        return file
    }
}
