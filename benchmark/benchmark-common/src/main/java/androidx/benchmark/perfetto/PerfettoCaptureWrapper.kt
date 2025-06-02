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

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo
import androidx.benchmark.InMemoryTracing
import androidx.benchmark.Outputs
import androidx.benchmark.Outputs.dateToFileName
import androidx.benchmark.PropOverride
import androidx.benchmark.Shell
import androidx.benchmark.ShellFile
import androidx.benchmark.UserFile
import androidx.benchmark.UserInfo
import androidx.benchmark.perfetto.PerfettoHelper.Companion.LOG_TAG
import androidx.benchmark.perfetto.PerfettoHelper.Companion.isAbiSupported
import androidx.tracing.perfetto.handshake.protocol.ResponseResultCodes.RESULT_CODE_ALREADY_ENABLED
import androidx.tracing.perfetto.handshake.protocol.ResponseResultCodes.RESULT_CODE_SUCCESS
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/** Wrapper for [PerfettoCapture] which does nothing below API 23. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class PerfettoCaptureWrapper {
    private var capture: PerfettoCapture? = null
    private val TRACE_ENABLE_PROP = "persist.traced.enable"

    init {
        if (Build.VERSION.SDK_INT >= 23) {
            capture = PerfettoCapture()
        }
    }

    companion object {
        val inUseLock = Object()

        /**
         * Prevents re-entrance of perfetto trace capture, as it doesn't handle this correctly
         *
         * (Single file output location, process cleanup, etc.)
         */
        var inUse = false
    }

    @RequiresApi(23)
    private fun start(
        config: PerfettoConfig,
        perfettoSdkConfig: PerfettoCapture.PerfettoSdkConfig?,
    ): Boolean {
        capture?.apply {
            Log.d(LOG_TAG, "Recording perfetto trace")
            if (perfettoSdkConfig != null && Build.VERSION.SDK_INT >= 30) {
                val (resultCode, message) = enableAndroidxTracingPerfetto(perfettoSdkConfig)
                Log.d(LOG_TAG, "Enable full tracing result=$message")

                if (resultCode !in arrayOf(RESULT_CODE_SUCCESS, RESULT_CODE_ALREADY_ENABLED)) {
                    throw RuntimeException(
                        "Issue while enabling Perfetto SDK tracing in" +
                            " ${perfettoSdkConfig.targetPackage}: $message"
                    )
                }
            }
            start(config)
        }

        return true
    }

    @RequiresApi(23)
    private fun stop(traceLabel: String, inMemoryTracingLabel: String?): String {
        return Outputs.writeFile(fileName = "${traceLabel}_${dateToFileName()}.perfetto-trace") {

            // The output of this method expects the final to be written in a user writeable folder.
            // If the default user is selected, perfetto can stop and write the file directly there.
            // Otherwise, we first need to write it in a shell storage and the use the VirtualFile
            // to cross between shell and user storage.

            if (UserInfo.isAdditionalUser) {
                ShellFile.inTempDir(it.name).apply {
                    capture!!.stop(absolutePath, inMemoryTracingLabel)
                    copyTo(UserFile(it.absolutePath))
                    delete()
                }
            } else {
                capture!!.stop(it.absolutePath, inMemoryTracingLabel)
                if (Outputs.forceFilesForShellAccessible) {
                    // This shell written file must be made readable to be later accessed by this
                    // process (e.g. for appending UiState). Unlike in other places, shell
                    // must increase access, since it's giving the app access
                    Shell.chmod(path = it.absolutePath, args = "777")
                }
            }
        }
    }

    @OptIn(ExperimentalContracts::class)
    fun record(
        fileLabel: String,
        config: PerfettoConfig,
        perfettoSdkConfig: PerfettoCapture.PerfettoSdkConfig?,
        traceCallback: ((String) -> Unit)? = null,
        enableTracing: Boolean = true,
        inMemoryTracingLabel: String? = null,
        block: () -> Unit,
    ): String? {
        contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }
        // skip if Perfetto not supported, or if caller opts out
        if (Build.VERSION.SDK_INT < 23 || !isAbiSupported() || !enableTracing) {
            block()
            return null
        }

        synchronized(inUseLock) {
            if (inUse) {
                throw IllegalStateException(
                    "Reentrant Perfetto Tracing is not supported." +
                        " This means you cannot use more than one of" +
                        " BenchmarkRule/MacrobenchmarkRule/PerfettoTraceRule/PerfettoTrace.record" +
                        " together."
                )
            }
            inUse = true
        }
        // Prior to Android 11 (R), a shell property must be set to enable perfetto tracing, see
        // https://perfetto.dev/docs/quickstart/android-tracing#starting-the-tracing-services
        val propOverride =
            if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
                PropOverride(TRACE_ENABLE_PROP, "1")
            } else null

        val path: String
        try {
            propOverride?.forceValue()
            start(config, perfettoSdkConfig)

            // To avoid b/174007010, userspace tracing is cleared and saved *during* trace, so
            // that events won't lie outside the bounds of the trace content.
            InMemoryTracing.clearEvents()
            try {
                block()
            } finally {
                // finally here to ensure trace is fully recorded if block throws
                path = stop(fileLabel, inMemoryTracingLabel)
                traceCallback?.invoke(path)
            }
        } finally {
            propOverride?.resetIfOverridden()
            synchronized(inUseLock) { inUse = false }
        }
        return path
    }
}
