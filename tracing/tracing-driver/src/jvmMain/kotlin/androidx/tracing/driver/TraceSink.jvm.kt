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

package androidx.tracing.driver

import com.squareup.wire.ProtoWriter
import java.io.Closeable
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.intrinsics.COROUTINE_SUSPENDED
import kotlin.coroutines.intrinsics.createCoroutineUnintercepted
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import okio.BufferedSink
import okio.appendingSink
import okio.buffer
import perfetto.protos.MutableTracePacket

/** The trace sink that writes to a new file per trace session. */
public class JvmTraceSink(
    @Suppress("UNUSED") private val baseDir: File,
    private val coroutineContext: CoroutineContext = Dispatchers.IO
) : TraceSink(), Closeable {
    internal val traceFile = baseDir.perfettoTraceFile()
    private val bufferedSink: BufferedSink = traceFile.appendingSink().buffer()
    private val protoWriter: ProtoWriter = ProtoWriter(bufferedSink)

    // There are 2 distinct mechanisms for thread safety here, and they are not necessarily in sync.
    // The Queue by itself is thread-safe, but after we drain the queue we mark drainRequested
    // to false (not an atomic operation). So a writer can come along and add a pooled array of
    // trace packets. That is still okay given, those packets will get picked during the next
    // drain request; or on flush() prior to the close() of the Sink.
    // No packets are lost or dropped; and therefore we are still okay with this small
    // compromise with thread safety.
    private val queue = Queue<PooledTracePacketArray>()
    private val drainRequested = AtomicBoolean(false)

    @Volatile private var resumeDrain: Continuation<Unit>? = null

    init {
        resumeDrain =
            suspend {
                    coroutineContext[Job]?.invokeOnCompletion { makeDrainRequest() }
                    while (true) {
                        drainQueue() // Sets drainRequested to false on completion
                        suspendCoroutine<Unit> { continuation ->
                            resumeDrain = continuation
                            COROUTINE_SUSPENDED // Suspend
                        }
                    }
                }
                .createCoroutineUnintercepted(Continuation(context = coroutineContext) {})

        // Kick things off and suspend
        makeDrainRequest()
    }

    override fun enqueue(pooledPacketArray: PooledTracePacketArray) {
        queue.addLast(pooledPacketArray)
        makeDrainRequest()
    }

    override fun flush() {
        makeDrainRequest()
        while (drainRequested.get()) {
            // Await completion of the drain.
        }
        bufferedSink.flush()
    }

    private fun makeDrainRequest() {
        // Only make a request if one is not already ongoing
        if (drainRequested.compareAndSet(false, true)) {
            resumeDrain?.resume(Unit)
        }
    }

    private fun drainQueue() {
        while (queue.isNotEmpty()) {
            val pooledPacketArray = queue.removeFirstOrNull()
            if (pooledPacketArray != null) {
                pooledPacketArray.forEach {
                    MutableTracePacket.ADAPTER.encodeWithTag(protoWriter, 1, it)
                }
                pooledPacketArray.recycle()
            }
        }
        drainRequested.set(false)
        // Mark resumeDrain as consumed because the Coroutines Machinery might still consider
        // the Continuation as resumed after drainQueue() completes. This was the Atomics
        // drainRequested, and the Continuation resumeDrain are in sync.
        resumeDrain = null
    }

    override fun close() {
        flush()
        bufferedSink.close()
    }
}

private fun File.perfettoTraceFile(): File {
    val formatter = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss", Locale.getDefault())
    formatter.timeZone = TimeZone.getTimeZone("UTC")
    val traceFile = File(this, "perfetto-${formatter.format(Date())}.perfetto-trace")
    return traceFile
}
