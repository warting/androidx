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

import androidx.benchmark.macro.ExperimentalMetricApi
import androidx.benchmark.macro.TraceMetric
import androidx.benchmark.traceprocessor.TraceProcessor

/**
 * Calculates latency of sections where begin and end trace points are in different processes. This
 * is currently not supported by [androidx.benchmark.macro.TraceSectionMetric] but is important for
 * privacysandbox.
 *
 * @param beginPointName Name of begin tracepoint.
 * @param endPointName Name of end tracepoint.
 * @param eventName Name of the final metric that is spit out after the test run. The metric name
 *   will be {$eventName}LatencyMillis.
 * @param occurrenceOfBeginTrace Occurrence of the begin tracepoint among other traces with the same
 *   name. Default value is TraceOccurrence.FIRST.
 * @param occurrenceOfEndTrace Occurrence of the end tracepoint among other traces with the same
 *   name. Default value is TraceOccurrence.FIRST.
 */
@OptIn(ExperimentalMetricApi::class)
public class SdkSandboxCrossProcessLatencyMetric(
    private val beginPointName: String,
    private val endPointName: String,
    private val eventName: String,
    private val occurrenceOfBeginTrace: TraceOccurrence = TraceOccurrence.FIRST,
    private val occurrenceOfEndTrace: TraceOccurrence = TraceOccurrence.FIRST,
) : TraceMetric() {
    /**
     * There may be multiple occurrences of the tracepoint in the traces collected. For example, if
     * we add a tracepoint at [SandboxedSdkView] checkClientOpenSession method for mediatee with
     * mediator overlay case, this will be logged twice - first time for mediator
     * [SandboxedUiAdapter], and then another time for the mediatee [SandboxedUiAdapter]. In such
     * cases we need to specify which occurrence we care about.
     *
     * For the tracepoints finalised currently, we have at best 2 occurrences of the same
     * tracepoint. More values can be added if needed.
     */
    enum class TraceOccurrence {
        FIRST,
        LAST,
    }

    @OptIn(ExperimentalMetricApi::class)
    override fun getMeasurements(
        captureInfo: CaptureInfo,
        traceSession: TraceProcessor.Session,
    ): List<Measurement> {
        val query =
            """
             INCLUDE perfetto module slices.with_context;
             SELECT
                 event,
                 ts2-ts1 AS latency_in_nanos
             FROM
             (
               (SELECT
                   ts AS ts1,
                   "event" AS event,
                   LOWER(TRIM(process_name)) as process_name_1
                   FROM process_slice WHERE name = '${beginPointName}'
               """
                .trimIndent() +
                getQueryByTraceOccurrence(occurrenceOfBeginTrace) +
                """
               LEFT JOIN
               (SELECT
                   ts AS ts2,
                   "event" AS event,
                   LOWER(TRIM(process_name)) as process_name_2
                   FROM process_slice WHERE name = '${endPointName}'
               """
                    .trimIndent() +
                getQueryByTraceOccurrence(occurrenceOfEndTrace) +
                " USING (event)) " +
                checkIfEventsInSameOrSandboxProcess()
        val rowSequence = traceSession.query(query)
        // First row (or null) is returned.
        val latencyResultNanos = rowSequence.firstOrNull()?.long("latency_in_nanos")
        return if (latencyResultNanos != null) {
            listOf(Measurement(eventName + "LatencyMillis", latencyResultNanos / 1_000_000.0))
        } else {
            // We can spit out a value anyway so that we’ll be alerted in case the trace has been
            // removed.  Consistent 0 value for metric (ie persistent drop in metric) will lead
            // to alerts unless trace is added back.
            listOf(Measurement(eventName + "LatencyMillis", 0.0))
        }
    }

    private fun getQueryByTraceOccurrence(traceOccurrence: TraceOccurrence): String {
        return if (traceOccurrence == TraceOccurrence.FIRST) {
            // Returned row will be the one which was logged first.
            " ORDER BY ts LIMIT 1) "
        } else {
            // Returned row will be the one which was logged last.
            " ORDER BY ts DESC LIMIT 1) "
        }
    }

    private fun checkIfEventsInSameOrSandboxProcess(): String {
        return """
            WHERE (process_name_1 = process_name_2
            OR process_name_1 = CONCAT(process_name_2, "_sdk_sandbox")
            OR process_name_2 = CONCAT(process_name_1, "_sdk_sandbox"))
        """
            .trimIndent()
    }
}
