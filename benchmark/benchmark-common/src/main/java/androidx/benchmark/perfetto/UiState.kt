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

package androidx.benchmark.perfetto

import androidx.annotation.RestrictTo
import java.io.File
import perfetto.protos.Trace
import perfetto.protos.TracePacket
import perfetto.protos.UiState

/** Convenience for UiState construction with specified package */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun UiState(timelineStart: Long? = null, timelineEnd: Long? = null, highlightPackage: String?) =
    UiState(
        timeline_start_ts = timelineStart,
        timeline_end_ts = timelineEnd,
        highlight_process =
            highlightPackage?.run { UiState.HighlightProcess(cmdline = highlightPackage) }
    )

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun File.appendUiState(state: UiState) {
    val traceToAppend = Trace(packet = listOf(TracePacket(ui_state = state)))
    appendBytes(traceToAppend.encode())
}
