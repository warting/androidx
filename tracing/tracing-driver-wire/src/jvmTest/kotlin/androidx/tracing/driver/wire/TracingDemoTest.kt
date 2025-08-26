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

package androidx.tracing.driver.wire

import androidx.tracing.driver.ThreadTrack
import androidx.tracing.driver.TraceDriver
import java.io.File
import kotlin.random.Random
import kotlin.test.Test
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

class TracingDemoTest {
    internal val random = Random(42)

    internal val forkSize = 1
    internal val multiplier = 1000
    internal val inputSize = forkSize * multiplier

    // Tracks the number of batches completed
    internal var count = 0L
    internal val driver =
        TraceDriver(sink = TraceSink(sequenceId = 1, directory = File("/tmp")), isEnabled = true)

    // The Process Track
    internal val track = driver.ProcessTrack(id = 1, name = "TracingTest")

    private fun threadTrack(): ThreadTrack {
        val thread = Thread.currentThread()
        @Suppress("DEPRECATION")
        return track.getOrCreateThreadTrack(id = thread.id.toInt(), name = thread.name)
    }

    @Test
    internal fun testSimpleTracingTest() = runBlocking {
        driver.context.use {
            withContext(context = Dispatchers.Default) {
                threadTrack().traceCoroutine(
                    "trace",
                    metadataBlock = {
                        addMetadataEntry("context", "basic trace with 1 suspension point")
                    },
                ) {
                    coroutineScope { delay(10L) }
                }
            }
        }
    }

    @Test
    internal fun testTracingEndToEnd(): Unit = runBlocking {
        driver.context.use {
            withContext(context = Dispatchers.Default) {
                threadTrack().traceCoroutine(
                    "begin",
                    metadataBlock = { addMetadataEntry("context", "end to end tracing test") },
                ) {
                    coroutineScope {
                        delay(20L)
                        threadTrack().traceCoroutine("histograms-end-to-end") {
                            computeHistograms()
                        }
                        threadTrack().traceCoroutine("end") { delay(20L) }
                        delay(40L)
                    }
                }
            }
        }
    }

    internal suspend fun computeHistograms(): Map<Int, Int> {
        val input = List(inputSize) { random.nextInt(0, 100_000) }
        val batches = input.chunked(multiplier)
        return coroutineScope {
            val jobs = mutableListOf<Deferred<Map<Int, Int>>>()
            batches.forEachIndexed { index, batch ->
                jobs += async {
                    threadTrack().traceCoroutine("histograms-batch-$index") {
                        computeHistogram(batch)
                    }
                }
            }
            val histograms = jobs.awaitAll()
            val output =
                threadTrack().traceCoroutine("merge-histograms") {
                    mergeHistograms(input, histograms)
                }
            output
        }
    }

    internal suspend fun computeHistogram(list: List<Int>): Map<Int, Int> {
        val counter = track.getOrCreateCounterTrack("Batches Completed")
        val frequency = mutableMapOf<Int, Int>()
        for (element in list) {
            val count = frequency[element] ?: 0
            frequency[element] = count + 1
        }
        delay(random.nextLong(10L, 20L)) // Waterfall
        count += 1
        counter.setCounter(count)
        return frequency
    }

    internal suspend fun mergeHistograms(
        input: List<Int>,
        histograms: List<Map<Int, Int>>,
    ): Map<Int, Int> {
        val frequency = mutableMapOf<Int, Int>()
        for (element in input) {
            var count = 0
            for (histogram in histograms) {
                count += histogram[element] ?: 0
            }
            frequency[element] = count
        }
        delay(random.nextLong(10L, 20L)) // Waterfall
        return frequency
    }
}
