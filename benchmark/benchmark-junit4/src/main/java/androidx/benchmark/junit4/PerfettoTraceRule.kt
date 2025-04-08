/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.benchmark.junit4

import android.os.Build
import androidx.benchmark.InstrumentationResults
import androidx.benchmark.Profiler
import androidx.benchmark.perfetto.ExperimentalPerfettoCaptureApi
import androidx.benchmark.perfetto.PerfettoCapture
import androidx.benchmark.perfetto.PerfettoCapture.PerfettoSdkConfig.InitialProcessState
import androidx.benchmark.perfetto.PerfettoCaptureWrapper
import androidx.benchmark.perfetto.PerfettoConfig
import androidx.benchmark.traceprocessor.PerfettoTrace
import androidx.benchmark.traceprocessor.record
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

/**
 * Add this rule to record a Perfetto trace for each test on Android Lollipop (API 21)+ devices.
 *
 * ```
 * @RunWith(AndroidJUnit4::class)
 * class PerfettoOverheadBenchmark {
 *     // traces all tests in file
 *     @get:Rule
 *     val perfettoRule = PerfettoTraceRule()
 *
 *     @Test
 *     fun test() {}
 * }
 * ```
 *
 * Captured traces can be observed through any of:
 * * Android Studio trace linking under `Benchmark` in test output tab
 * * The optional `traceCallback` parameter
 * * Android Gradle defining and pulling the file via additionalTestOutputDir.
 *
 * When invoked via Gradle, files will be copied to host path like the following:
 * ```
 * out/build/outputs/connected_android_test_additional_output/debugAndroidTest/connected/<deviceName>/androidx.mypackage.TestClass_testMethod.perfetto-trace
 * ```
 *
 * You can additionally check logcat for messages tagged "PerfettoCapture:" for the path of each
 * perfetto trace.
 *
 * ```
 * > adb pull /storage/emulated/0/Android/data/mypackage.test/files/PerfettoCaptureTest.trace
 * ```
 *
 * Reentrant Perfetto trace capture is not supported, so this API may not be combined with
 * `BenchmarkRule`, `MacrobenchmarkRule`, or `PerfettoTrace.record`.
 */
@ExperimentalPerfettoCaptureApi
class PerfettoTraceRule(
    /** Config used to record Perfetto trace. */
    val config: PerfettoConfig,

    /**
     * Pass true to enable userspace tracing (androidx.tracing.tracing-perfetto APIs)
     *
     * Defaults to false.
     */
    val enableUserspaceTracing: Boolean = false,

    /** Callback for each captured trace. */
    val traceCallback: ((PerfettoTrace) -> Unit)? = null
) : TestRule {
    constructor(
        /**
         * Pass false to disable android.os.Trace API tracing in this process
         *
         * Defaults to true.
         */
        enableAppTagTracing: Boolean = true,

        /**
         * Pass true to enable userspace tracing (androidx.tracing.tracing-perfetto APIs)
         *
         * Defaults to false.
         */
        enableUserspaceTracing: Boolean = false,

        /** Callback for each captured trace. */
        traceCallback: ((PerfettoTrace) -> Unit)? = null
    ) : this(
        config =
            PerfettoConfig.Benchmark(
                appTagPackages = if (enableAppTagTracing) listOf(thisPackage) else emptyList(),
                useStackSamplingConfig = false
            ),
        enableUserspaceTracing = enableUserspaceTracing,
        traceCallback = traceCallback
    )

    override fun apply(
        @Suppress("InvalidNullabilityOverride") // JUnit missing annotations
        base: Statement,
        @Suppress("InvalidNullabilityOverride") // JUnit missing annotations
        description: Description
    ): Statement =
        object : Statement() {
            override fun evaluate() {
                val label = "${description.className}_${description.methodName}"
                PerfettoCaptureWrapper()
                    .record(
                        fileLabel = label,
                        config = config,
                        perfettoSdkConfig =
                            if (enableUserspaceTracing && Build.VERSION.SDK_INT >= 23) {
                                PerfettoCapture.PerfettoSdkConfig(
                                    thisPackage,
                                    InitialProcessState.Alive
                                )
                            } else null,
                        traceCallback = { path ->
                            val trace = PerfettoTrace(path)
                            InstrumentationResults.instrumentationReport {
                                reportSummaryToIde(
                                    testName = label,
                                    profilerResults =
                                        listOf(Profiler.ResultFile.ofPerfettoTrace("Trace", path))
                                )
                            }
                            traceCallback?.invoke(trace)
                        },
                        enableTracing = Build.VERSION.SDK_INT >= 23,
                        // Temporary, see b/409397427
                        // after that is resolved, switch back to PerfettoTrace.record()
                        inMemoryTracingLabel = "InMemoryTracing",
                        block = { base.evaluate() }
                    )
            }
        }

    private companion object {
        private val thisPackage = InstrumentationRegistry.getInstrumentation().context.packageName
    }
}
