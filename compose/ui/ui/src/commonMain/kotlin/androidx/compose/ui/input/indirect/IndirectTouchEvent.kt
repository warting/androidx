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

package androidx.compose.ui.input.indirect

import androidx.compose.ui.ExperimentalIndirectTouchTypeApi
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEvent

/**
 * Represents a touch event that did not result from contact with a touchscreen.
 *
 * This event differs from a [PointerEvent] as it does not necessitate an existence of a pointer. If
 * an event were to have an associated pointer, they will be routed to through [PointerEvent].
 */
@ExperimentalIndirectTouchTypeApi
sealed interface IndirectTouchEvent {

    /** The position relative to the input device. */
    val position: Offset

    /** The time at which this event occurred. */
    val uptimeMillis: Long

    /** The reason the [IndirectTouchEvent] was sent. */
    val type: IndirectTouchEventType

    /** Main coordinate axis to use for movement. */
    val primaryAxis: IndirectTouchEventPrimaryAxis
}

// Work around for Kotlin cross module sealed interfaces.
@OptIn(ExperimentalIndirectTouchTypeApi::class)
internal interface PlatformIndirectTouchEvent : IndirectTouchEvent

/** Indicates the reason that the [IndirectTouchEvent] was sent. */
@kotlin.jvm.JvmInline
@ExperimentalIndirectTouchTypeApi
value class IndirectTouchEventType private constructor(internal val value: Int) {
    @ExperimentalIndirectTouchTypeApi
    companion object {

        /** An unknown reason for the event. */
        val Unknown = IndirectTouchEventType(0)

        /** A pressed gesture as started. */
        val Press = IndirectTouchEventType(1)

        /** A pressed gesture has finished. */
        val Release = IndirectTouchEventType(2)

        /** A change has happened during a press gesture. */
        val Move = IndirectTouchEventType(3)
    }

    override fun toString(): String =
        when (this) {
            Press -> "Press"
            Release -> "Release"
            Move -> "Move"
            else -> "Unknown"
        }
}

/** Indicates the primary axis to use for movement of an [IndirectTouchEvent]. */
@kotlin.jvm.JvmInline
@ExperimentalIndirectTouchTypeApi
value class IndirectTouchEventPrimaryAxis private constructor(internal val value: Int) {
    @ExperimentalIndirectTouchTypeApi
    companion object {

        /** No coordinate axes specified for movement. */
        val None = IndirectTouchEventPrimaryAxis(0)

        /** X coordinate axis specified as the primary movement axis. */
        val X = IndirectTouchEventPrimaryAxis(1)

        /** Y coordinate axis specified as the primary movement axis. */
        val Y = IndirectTouchEventPrimaryAxis(2)
    }
}
