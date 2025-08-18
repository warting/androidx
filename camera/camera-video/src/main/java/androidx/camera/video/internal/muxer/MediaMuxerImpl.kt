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

package androidx.camera.video.internal.muxer

import android.media.MediaCodec
import android.media.MediaFormat
import android.media.MediaMuxer
import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.camera.video.internal.compat.Api26Impl
import androidx.camera.video.internal.muxer.Muxer.Companion.MUXER_FORMAT_3GPP
import androidx.camera.video.internal.muxer.Muxer.Companion.MUXER_FORMAT_MPEG_4
import androidx.camera.video.internal.muxer.Muxer.Companion.MUXER_FORMAT_WEBM
import androidx.camera.video.internal.workaround.CorrectNegativeLatLongForMediaMuxer
import java.nio.ByteBuffer

/** An implementation of the [Muxer] interface using the Android platform's [MediaMuxer]. */
public class MediaMuxerImpl : Muxer {

    private enum class State {
        IDLE,
        CONFIGURED,
        STARTED,
        STOPPED,
        RELEASED,
    }

    private var mediaMuxer: MediaMuxer? = null
    private var state: State = State.IDLE

    override fun setOutput(path: String, @Muxer.Format format: Int) {
        check(state == State.IDLE) { "Muxer is not idle. Current state: $state" }

        mediaMuxer = MediaMuxer(path, format.toMediaMuxerFormat())
        state = State.CONFIGURED
    }

    override fun setOutput(parcelFileDescriptor: ParcelFileDescriptor, @Muxer.Format format: Int) {
        check(state == State.IDLE) { "Muxer is not idle. Current state: $state" }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            throw IllegalStateException("MediaMuxer doesn't accept FileDescriptor before API 26")
        }
        mediaMuxer =
            Api26Impl.createMediaMuxer(
                parcelFileDescriptor.fileDescriptor,
                format.toMediaMuxerFormat(),
            )
        // FileDescriptor is safe to close as soon as the MediaMuxer constructor returns.
        parcelFileDescriptor.close()
        state = State.CONFIGURED
    }

    override fun setOrientationDegrees(degrees: Int) {
        check(state == State.CONFIGURED) { "Muxer is not configured. Current state: $state" }
        mediaMuxer!!.setOrientationHint(degrees)
    }

    override fun setLocation(latitude: Double, longitude: Double) {
        check(state == State.CONFIGURED) { "Muxer is not configured. Current state: $state" }
        val geoLocation = CorrectNegativeLatLongForMediaMuxer.adjustGeoLocation(latitude, longitude)
        mediaMuxer!!.setLocation(geoLocation.first.toFloat(), geoLocation.second.toFloat())
    }

    override fun addTrack(format: MediaFormat): Int {
        check(state == State.CONFIGURED) { "Muxer is not configured. Current state: $state" }
        return wrapMuxerException { mediaMuxer!!.addTrack(format) }
    }

    override fun start() {
        if (state == State.STARTED) {
            return
        }
        check(state == State.CONFIGURED) { "Muxer is not configured. Current state: $state" }
        wrapMuxerException { mediaMuxer!!.start() }
        state = State.STARTED
    }

    override fun stop() {
        if (state == State.STOPPED) {
            return
        }
        check(state == State.STARTED) { "Muxer is not started. Current state: $state" }
        try {
            wrapMuxerException { mediaMuxer!!.stop() }
        } finally {
            state = State.STOPPED
        }
    }

    override fun writeSampleData(
        trackIndex: Int,
        byteBuffer: ByteBuffer,
        bufferInfo: MediaCodec.BufferInfo,
    ) {
        check(state == State.STARTED) { "Muxer is not started. Current state: $state" }
        wrapMuxerException { mediaMuxer!!.writeSampleData(trackIndex, byteBuffer, bufferInfo) }
    }

    override fun release() {
        if (state == State.RELEASED) {
            return
        }
        // MediaMuxer.release() will internally call MediaMuxer.stop().
        // Ignore any exception thrown.
        runCatching { mediaMuxer?.release() }
        mediaMuxer = null
        state = State.RELEASED
    }

    override fun isInterruptionResilient(): Boolean {
        return false
    }

    private fun @receiver:Muxer.Format Int.toMediaMuxerFormat(): Int {
        return when (this) {
            MUXER_FORMAT_MPEG_4 -> MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4
            MUXER_FORMAT_WEBM -> MediaMuxer.OutputFormat.MUXER_OUTPUT_WEBM
            MUXER_FORMAT_3GPP -> {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                    // MediaMuxer does not support 3GPP on pre-Android O(API 26) devices.
                    MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4
                } else {
                    MediaMuxer.OutputFormat.MUXER_OUTPUT_3GPP
                }
            }
            else -> throw IllegalArgumentException("Unsupported format: $this")
        }
    }

    @Throws(MuxerException::class)
    private fun <T> wrapMuxerException(block: () -> T): T {
        try {
            return block()
        } catch (e: Exception) {
            throw MuxerException("MediaMuxer operation failed", e)
        }
    }
}
