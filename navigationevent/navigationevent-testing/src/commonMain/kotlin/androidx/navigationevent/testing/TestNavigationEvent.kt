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

package androidx.navigationevent.testing

import androidx.annotation.FloatRange
import androidx.navigationevent.NavigationEvent
import androidx.navigationevent.NavigationEvent.Companion.EDGE_NONE
import androidx.navigationevent.NavigationEvent.SwipeEdge

/**
 * Creates a [NavigationEvent] for testing purposes.
 *
 * This function provides a convenient way to construct a [NavigationEvent] with default values,
 * useful for unit tests where you need to simulate a navigation gesture.
 *
 * @param touchX The absolute X coordinate of the touch event, defaulting to 0.0F.
 * @param touchY The absolute Y coordinate of the touch event, defaulting to 0.0F.
 * @param progress The progress of the navigation gesture, defaulting to 0.0F. This ranges from 0.0F
 *   to 1.0F, representing the completion of the gesture.
 * @param swipeEdge The edge from which the swipe originated, defaulting to [EDGE_NONE].
 * @param frameTimeMillis The timestamp of the event in milliseconds, defaulting to 0.
 * @return A new [NavigationEvent] instance configured with the provided parameters.
 */
@Suppress("FunctionName")
public fun TestNavigationEvent(
    @FloatRange(from = 0.0) touchX: Float = 0.0F,
    @FloatRange(from = 0.0) touchY: Float = 0.0F,
    @FloatRange(from = 0.0, to = 1.0) progress: Float = 0.0F,
    swipeEdge: @SwipeEdge Int = EDGE_NONE,
    frameTimeMillis: Long = 0,
): NavigationEvent {
    return NavigationEvent(touchX, touchY, progress, swipeEdge, frameTimeMillis)
}
