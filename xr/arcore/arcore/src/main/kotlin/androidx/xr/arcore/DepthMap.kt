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

package androidx.xr.arcore

import androidx.annotation.RestrictTo
import androidx.xr.arcore.internal.DepthMap as RuntimeDepthMap
import androidx.xr.runtime.Session
import java.nio.ByteBuffer
import java.nio.FloatBuffer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Contains the depth map information corresponding to a specific [RenderViewpoint] */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public class DepthMap internal constructor(internal val runtimeDepthMap: RuntimeDepthMap) :
    Updatable {
    public companion object {
        /**
         * Returns the DepthMap associated with the left display.
         *
         * @param session the currently active [Session].
         * @note Supported only on devices that use stereo displays for rendering.
         */
        @JvmStatic
        public fun left(session: Session): DepthMap? {
            val perceptionStateExtender = DepthMap.Companion.getPerceptionStateExtender(session)
            return perceptionStateExtender.xrResourcesManager.leftDepthMap
        }

        /**
         * Returns the DepthMap associated with the right display.
         *
         * @param session the currently active [Session].
         * @note Supported only on devices that use stereo displays for rendering.
         */
        @JvmStatic
        public fun right(session: Session): DepthMap? {
            val perceptionStateExtender = DepthMap.Companion.getPerceptionStateExtender(session)
            return perceptionStateExtender.xrResourcesManager.rightDepthMap
        }

        /**
         * Returns the DepthMap associated with the single device display.
         *
         * @param session the currently active [Session].
         * @note When the device uses a single display, this will return the depth map for that
         *   display. When the device uses stereo displays, this will return the depth map for the
         *   center of the two displays.
         */
        @JvmStatic
        public fun mono(session: Session): DepthMap? {
            val perceptionStateExtender = DepthMap.Companion.getPerceptionStateExtender(session)
            return perceptionStateExtender.xrResourcesManager.monoDepthMap
        }

        // TODO(b/421240554): Combine getPerceptionStateExtender in different classes.
        private fun getPerceptionStateExtender(session: Session): PerceptionStateExtender {
            val perceptionStateExtender: PerceptionStateExtender? =
                session.stateExtenders.filterIsInstance<PerceptionStateExtender>().first()
            check(perceptionStateExtender != null) { "PerceptionStateExtender is not available." }
            return perceptionStateExtender
        }
    }

    /**
     * Contains the current state of depth tracking
     *
     * @property width The width of the depth map.
     * @property height The height of the depth map.
     * @property rawDepthMap Buffer of size [width x height] representing raw depth in meters from
     *   the image plane. The row and pixel stride of the buffer are both zero.
     * @property rawConfidenceMap Confidence for each pixel in [rawDepthMap], with 0 representing
     *   the lowest confidence and 255 representing the highest confidence.
     * @property smoothDepthMap Buffer of size [width x height] representing smooth depth in meters
     *   from the image plane. The row and pixel stride of the buffer are both zero.
     * @property smoothConfidenceMap Confidence for each pixel in [smoothDepthMap], with 0
     *   representing the lowest confidence and 255 representing the highest confidence.
     */
    public class State(
        public val width: Int,
        public val height: Int,
        public val rawDepthMap: FloatBuffer?,
        public val rawConfidenceMap: ByteBuffer?,
        public val smoothDepthMap: FloatBuffer?,
        public val smoothConfidenceMap: ByteBuffer?,
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is androidx.xr.arcore.DepthMap.State) return false
            return width == other.width &&
                height == other.height &&
                rawDepthMap == other.rawDepthMap &&
                rawConfidenceMap == other.rawConfidenceMap &&
                smoothDepthMap == other.smoothDepthMap &&
                smoothConfidenceMap == other.smoothConfidenceMap
        }

        override fun hashCode(): Int {
            var result = width.hashCode()
            result = 31 * result + height.hashCode()
            result = 31 * result + (rawDepthMap?.hashCode() ?: 0)
            result = 31 * result + (rawConfidenceMap?.hashCode() ?: 0)
            result = 31 * result + (smoothDepthMap?.hashCode() ?: 0)
            result = 31 * result + (smoothConfidenceMap?.hashCode() ?: 0)
            return result
        }
    }

    private val _state =
        MutableStateFlow<State>(
            State(
                width = 0,
                height = 0,
                rawDepthMap = null,
                rawConfidenceMap = null,
                smoothDepthMap = null,
                smoothConfidenceMap = null,
            )
        )

    /** The current [State] of the depth map. */
    public val state: StateFlow<DepthMap.State> = _state.asStateFlow()

    override suspend fun update() {
        _state.emit(
            State(
                width = runtimeDepthMap.width,
                height = runtimeDepthMap.height,
                rawDepthMap = runtimeDepthMap.rawDepthMap,
                rawConfidenceMap = runtimeDepthMap.rawConfidenceMap,
                smoothDepthMap = runtimeDepthMap.smoothDepthMap,
                smoothConfidenceMap = runtimeDepthMap.smoothConfidenceMap,
            )
        )
    }
}
