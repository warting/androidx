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

package androidx.benchmark.macro.perfetto.server

import android.util.Log
import androidx.benchmark.Shell
import androidx.benchmark.ShellScript
import androidx.benchmark.macro.perfetto.PerfettoTraceProcessor
import androidx.benchmark.userspaceTrace
import java.io.InputStream
import java.io.OutputStream
import java.net.ConnectException
import java.net.HttpURLConnection
import java.net.URL
import perfetto.protos.AppendTraceDataResult
import perfetto.protos.ComputeMetricArgs
import perfetto.protos.ComputeMetricResult
import perfetto.protos.QueryArgs
import perfetto.protos.QueryResult
import perfetto.protos.StatusResult

/**
 * Wrapper around perfetto trace_shell_processor that communicates via http. The implementation
 * is based on the python one of the official repo:
 * https://github.com/google/perfetto/blob/master/python/perfetto/trace_processor/http.py
 */
internal class PerfettoHttpServer(private val port: Int) {

    companion object {
        private const val HTTP_ADDRESS = "http://localhost"
        private const val METHOD_GET = "GET"
        private const val METHOD_POST = "POST"
        private const val PATH_QUERY = "/query"
        private const val PATH_COMPUTE_METRIC = "/compute_metric"
        private const val PATH_PARSE = "/parse"
        private const val PATH_NOTIFY_EOF = "/notify_eof"
        private const val PATH_STATUS = "/status"
        private const val PATH_RESTORE_INITIAL_TABLES = "/restore_initial_tables"

        private const val TAG = "PerfettoHttpServer"
        private const val SERVER_START_TIMEOUT_MS = 5000
        private const val READ_TIMEOUT_SECONDS = 300000

        private var shellScript: ShellScript? = null

        /**
         * Returns a cached instance of the shell script to run the perfetto trace shell processor
         * as http server. Note that the generated script doesn't specify the port and this must
         * be passed as parameter when running the script.
         */
        fun getOrCreateShellScript(): ShellScript = shellScript ?: synchronized(this) {
            var instance = shellScript
            if (instance != null) {
                return@synchronized instance
            }
            val script =
                """echo pid:$$ ; exec ${PerfettoTraceProcessor.shellPath} -D --http-port "$@" """
            instance = Shell.createShellScript(script)
            shellScript = instance
            instance
        }

        /**
         * Clean up the shell script
         */
        fun cleanUpShellScript() = synchronized(this) {
            shellScript?.cleanUp()
            shellScript = null
        }
    }

    private var processId: Int? = null

    /**
     * Blocking method that runs the perfetto trace_shell_processor in server mode.
     *
     * @throws IllegalStateException if the server is not running by the end of the timeout.
     */
    fun startServer() = userspaceTrace("PerfettoHttpServer#startServer port $port") {
        if (processId != null) {
            Log.w(TAG, "Tried to start a trace shell processor that is already running.")
            return@userspaceTrace
        }

        val shellScript = getOrCreateShellScript().start(port.toString())

        processId = shellScript
            .stdOutLineSequence()
            .first { it.startsWith("pid:") }
            .split("pid:")[1]
            .toInt()

        // Wait for the trace_processor_shell server to start.
        var elapsed = 0
        while (!isRunning()) {
            Thread.sleep(5)
            elapsed += 5
            if (elapsed >= SERVER_START_TIMEOUT_MS) {
                throw IllegalStateException(
                    """
                        Perfetto trace_processor_shell did not start correctly.
                        Process stderr:
                        ${shellScript.getOutputAndClose().stderr}
                    """.trimIndent()
                )
            }
        }
        Log.i(TAG, "Perfetto trace processor shell server started (pid=$processId).")
    }

    /**
     * Stops the server killing the associated process
     */
    fun stopServer() = userspaceTrace("PerfettoHttpServer#stopServer port $port") {
        if (processId == null) {
            Log.w(TAG, "Tried to stop trace shell processor http server without starting it.")
            return@userspaceTrace
        }
        Shell.executeCommand("kill -TERM $processId")
        Log.i(TAG, "Perfetto trace processor shell server stopped (pid=$processId).")
    }

    /**
     * Returns true whether the server is running, false otherwise.
     */
    fun isRunning(): Boolean = userspaceTrace("PerfettoHttpServer#isRunning port $port") {
        return@userspaceTrace try {
            val statusResult = status()
            return@userspaceTrace statusResult.api_version != null && statusResult.api_version > 0
        } catch (e: ConnectException) {
            false
        }
    }

    /**
     * Executes the given [sqlQuery] on a previously parsed trace and returns the result as a
     * query result iterator.
     */
    fun query(sqlQuery: String): QueryResultIterator =
        QueryResultIterator(
            httpRequest(
                method = METHOD_POST,
                url = PATH_QUERY,
                encodeBlock = { QueryArgs.ADAPTER.encode(it, QueryArgs(sqlQuery)) },
                decodeBlock = { QueryResult.ADAPTER.decode(it) }
            )
        )

    /**
     * Computes the given metrics on a previously parsed trace.
     */
    fun computeMetric(metrics: List<String>): ComputeMetricResult =
        httpRequest(
            method = METHOD_POST,
            url = PATH_COMPUTE_METRIC,
            encodeBlock = { ComputeMetricArgs.ADAPTER.encode(it, ComputeMetricArgs(metrics)) },
            decodeBlock = { ComputeMetricResult.ADAPTER.decode(it) }
        )

    /**
     * Parses the trace file in chunks. Note that [notifyEof] should be called at the end to let
     * the processor know that no more chunks will be sent.
     */
    fun parse(bytes: ByteArray): AppendTraceDataResult =
        httpRequest(
            method = METHOD_POST,
            url = PATH_PARSE,
            encodeBlock = { it.write(bytes) },
            decodeBlock = { AppendTraceDataResult.ADAPTER.decode(it) }
        )

    /**
     * Notifies that the entire trace has been uploaded and no more chunks will be sent.
     */
    fun notifyEof() =
        httpRequest(
            method = METHOD_GET,
            url = PATH_NOTIFY_EOF,
            encodeBlock = null,
            decodeBlock = { }
        )

    /**
     * Clears the loaded trace and restore the state of the initial tables
     */
    fun restoreInitialTables() =
        httpRequest(
            method = METHOD_GET,
            url = PATH_RESTORE_INITIAL_TABLES,
            encodeBlock = null,
            decodeBlock = { }
        )

    /**
     * Checks the status of the trace_shell_processor http server.
     */
    private fun status(): StatusResult =
        httpRequest(
            method = METHOD_GET,
            url = PATH_STATUS,
            encodeBlock = null,
            decodeBlock = { StatusResult.ADAPTER.decode(it) }
        )

    private fun <T> httpRequest(
        method: String,
        url: String,
        contentType: String = "application/octet-stream",
        encodeBlock: ((OutputStream) -> Unit)?,
        decodeBlock: ((InputStream) -> T)
    ): T {
        with(URL("$HTTP_ADDRESS:$port$url").openConnection() as HttpURLConnection) {
            requestMethod = method
            readTimeout = READ_TIMEOUT_SECONDS
            setRequestProperty("Content-Type", contentType)
            if (encodeBlock != null) {
                doOutput = true
                encodeBlock(outputStream)
                outputStream.close()
            }
            val value = decodeBlock(inputStream)
            if (responseCode != 200) {
                throw IllegalStateException(responseMessage)
            }
            return value
        }
    }
}
