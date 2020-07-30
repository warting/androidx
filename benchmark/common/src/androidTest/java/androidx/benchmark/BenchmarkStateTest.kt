/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.benchmark

import android.Manifest
import android.util.Log
import androidx.benchmark.BenchmarkState.Companion.ExperimentalExternalReport
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.test.rule.GrantPermissionRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.util.concurrent.TimeUnit
import kotlin.test.assertFailsWith

@LargeTest
@RunWith(JUnit4::class)
class BenchmarkStateTest {
    private fun us2ns(ms: Long): Long = TimeUnit.MICROSECONDS.toNanos(ms)

    @get:Rule
    val writePermissionRule =
        GrantPermissionRule.grant(Manifest.permission.WRITE_EXTERNAL_STORAGE)!!

    /**
     * Run the block, and then spin-loop until durationUs has elapsed.
     *
     * Note: block must take less time than durationUs
     */
    private inline fun runAndSpin(durationUs: Long, crossinline block: () -> Unit = {}) {
        val start = System.nanoTime()
        block()
        val end = start + us2ns(durationUs)

        @Suppress("ControlFlowWithEmptyBody") // intentionally spinning
        while (System.nanoTime() < end) {}
    }

    @Test
    fun validateMetrics() {
        val state = BenchmarkState()
        while (state.keepRunning()) {
            runAndSpin(durationUs = 300) {
                // note, important here to not do too much work - this test may run on an
                // extremely slow device, or wacky emulator.
                allocate(40)
            }

            state.pauseTiming()
            runAndSpin(durationUs = 700) {
                allocate(80)
            }
            state.resumeTiming()
        }
        // The point of these asserts are to verify that pause/resume work, and that metrics that
        // come out are reasonable, not perfect - this isn't always run in stable perf environments
        val medianTime = state.getReport().getStats("timeNs").median
        assertTrue(
            "median time (ns) $medianTime should be roughly 300us",
            medianTime in us2ns(280)..us2ns(900)
        )
        val medianAlloc = state.getReport().getStats("allocationCount").median
        assertTrue(
            "median allocs $medianAlloc should be approximately 40",
            medianAlloc in 40..50
        )
    }

    @Test
    fun keepRunningMissingResume() {
        val state = BenchmarkState()

        assertEquals(true, state.keepRunning())
        state.pauseTiming()
        assertFailsWith<IllegalStateException> { state.keepRunning() }
    }

    @Test
    fun pauseCalledTwice() {
        val state = BenchmarkState()

        assertEquals(true, state.keepRunning())
        state.pauseTiming()
        assertFailsWith<IllegalStateException> { state.pauseTiming() }
    }

    @SdkSuppress(minSdkVersion = 24)
    @Test
    fun priorityJitThread() {
        assertEquals(
            "JIT priority should not yet be modified",
            ThreadPriority.JIT_INITIAL_PRIORITY,
            ThreadPriority.getJit()
        )

        // verify priority is only bumped during loop (NOTE: lower number means higher priority)
        val state = BenchmarkState()
        while (state.keepRunning()) {
            val currentJitPriority = ThreadPriority.getJit()
            assertTrue(
                "JIT priority should be bumped," +
                        " is $currentJitPriority vs ${ThreadPriority.JIT_INITIAL_PRIORITY}",
                currentJitPriority < ThreadPriority.JIT_INITIAL_PRIORITY
            )
        }
        assertEquals(ThreadPriority.JIT_INITIAL_PRIORITY, ThreadPriority.getJit())
    }

    @Test
    fun priorityBenchThread() {
        val initialPriority = ThreadPriority.get()
        assertNotEquals(
            "Priority should not be max",
            ThreadPriority.HIGH_PRIORITY,
            ThreadPriority.get()
        )

        // verify priority is only bumped during loop (NOTE: lower number means higher priority)
        val state = BenchmarkState()
        while (state.keepRunning()) {
            val currentPriority = ThreadPriority.get()
            assertTrue(
                "Priority should be bumped, is $currentPriority",
                currentPriority < initialPriority
            )
        }
        assertEquals(initialPriority, ThreadPriority.get())
    }

    private fun iterationCheck(checkingForThermalThrottling: Boolean) {
        val state = BenchmarkState()
        // disable thermal throttle checks, since it can cause loops to be thrown out
        // note that this bypasses allocation count
        state.simplifiedTimingOnlyMode = checkingForThermalThrottling
        var total = 0
        while (state.keepRunning()) {
            total++
        }

        val report = state.getReport()
        val expectedRepeatCount = BenchmarkState.REPEAT_COUNT_TIME +
                if (!checkingForThermalThrottling) BenchmarkState.REPEAT_COUNT_ALLOCATION else 0
        val expectedCount = report.warmupIterations + report.repeatIterations * expectedRepeatCount
        assertEquals(expectedCount, total)

        // verify we're not in warmup mode
        assertTrue(report.warmupIterations > 0)
        assertTrue(report.repeatIterations > 1)
        // verify we're not running in a special mode that affects repeat count (dry run, profiling)
        assertEquals(50, BenchmarkState.REPEAT_COUNT_TIME)
    }

    @Test
    fun iterationCheck_simple() {
        iterationCheck(checkingForThermalThrottling = true)
    }

    @Test
    fun iterationCheck_withAllocations() {
        if (CpuInfo.locked ||
            IsolationActivity.sustainedPerformanceModeInUse ||
            Errors.isEmulator
        ) {
            // In any of these conditions, it's known that throttling won't happen, so it's safe
            // to check for allocation count, by setting checkingForThermalThrottling = false
            iterationCheck(checkingForThermalThrottling = false)
        } else {
            Log.d(BenchmarkState.TAG, "Warning - bypassing iterationCheck_withAllocations")
        }
    }

    @Test
    fun bundle() {
        val bundle = BenchmarkState().apply {
            while (keepRunning()) {
                // nothing, we're ignoring numbers
            }
        }.getFullStatusReport(key = "foo", includeStats = true)

        assertTrue(
            (bundle.get("android.studio.display.benchmark") as String).contains("foo")
        )

        // check attribute presence and naming
        val prefix = Errors.PREFIX

        // legacy - before metric name was included
        assertNotNull(bundle.get("${prefix}min"))
        assertNotNull(bundle.get("${prefix}median"))
        assertNotNull(bundle.get("${prefix}standardDeviation"))

        // including metric name
        assertNotNull(bundle.get("${prefix}time_nanos_min"))
        assertNotNull(bundle.get("${prefix}time_nanos_median"))
        assertNotNull(bundle.get("${prefix}time_nanos_stddev"))

        assertNotNull(bundle.get("${prefix}allocation_count_min"))
        assertNotNull(bundle.get("${prefix}allocation_count_median"))
        assertNotNull(bundle.get("${prefix}allocation_count_stddev"))
    }

    @Test
    fun notStarted() {
        val initialPriority = ThreadPriority.get()
        try {
            BenchmarkState().getReport().getStats("timeNs").median
            fail("expected exception")
        } catch (e: IllegalStateException) {
            assertEquals(initialPriority, ThreadPriority.get())
            assertTrue(e.message!!.contains("wasn't started"))
            assertTrue(e.message!!.contains("benchmarkRule.measureRepeated {}"))
        }
    }

    @Test
    fun notFinished() {
        val initialPriority = ThreadPriority.get()
        try {
            BenchmarkState().run {
                keepRunning()
                getReport().getStats("timeNs").median
            }
            fail("expected exception")
        } catch (e: IllegalStateException) {
            assertEquals(initialPriority, ThreadPriority.get())
            assertTrue(e.message!!.contains("hasn't finished"))
            assertTrue(e.message!!.contains("benchmarkRule.measureRepeated {}"))
        }
    }

    @Suppress("DEPRECATION")
    @UseExperimental(ExperimentalExternalReport::class)
    @Test
    fun reportResult() {
        BenchmarkState.reportData(
            className = "className",
            testName = "testName",
            totalRunTimeNs = 900000000,
            dataNs = listOf(100L, 200L, 300L),
            warmupIterations = 1,
            thermalThrottleSleepSeconds = 0,
            repeatIterations = 1
        )
        val expectedReport = BenchmarkState.Report(
            className = "className",
            testName = "testName",
            totalRunTimeNs = 900000000,
            data = listOf(listOf(100L, 200L, 300L)),
            stats = listOf(Stats(longArrayOf(100, 200, 300), "timeNs")),
            repeatIterations = 1,
            thermalThrottleSleepSeconds = 0,
            warmupIterations = 1
        )
        assertEquals(expectedReport, ResultWriter.reports.last())
    }
}
