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

package androidx.compose.ui.input.rotary

/**
 * This event represents a rotary input event.
 */
expect class RotaryScrollEvent {
    /**
     * The amount to scroll (in pixels) in response to a [RotaryScrollEvent] in a container that
     * can scroll vertically.
     */
    val verticalScrollPixels: Float

    /**
     * The amount to scroll (in pixels) in response to a [RotaryScrollEvent] in a container that
     * can scroll horizontally.
     */
    val horizontalScrollPixels: Float

    /**
     * The time in milliseconds at which this even occurred. The start (`0`) time is
     * platform-dependent.
     */
    val uptimeMillis: Long
}
