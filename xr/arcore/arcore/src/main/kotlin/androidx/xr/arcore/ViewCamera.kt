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
import androidx.xr.runtime.FieldOfView
import androidx.xr.runtime.Session
import androidx.xr.runtime.internal.ViewCamera as RuntimeViewCamera
import androidx.xr.runtime.math.Pose
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Contains view cameras information. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public class ViewCamera internal constructor(internal val runtimeViewCamera: RuntimeViewCamera) :
    Updatable {

    public companion object {
        /**
         * Returns all view cameras.
         *
         * @param session the currently active [Session].
         */
        @JvmStatic
        public fun getAll(session: Session): List<ViewCamera> {
            val perceptionStateExtender = getPerceptionStateExtender(session)
            return perceptionStateExtender.xrResourcesManager.viewCameras
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
     * Data class that contains the current state of the view camera.
     *
     * @property pose The current pose of the view camera.
     * @property fieldOfView The current field of view of the view camera.
     */
    public class State(public val pose: Pose, public val fieldOfView: FieldOfView) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is State) return false
            return pose == other.pose && fieldOfView == other.fieldOfView
        }

        override fun hashCode(): Int {
            var result = pose.hashCode()
            result = 31 * result + fieldOfView.hashCode()
            return result
        }
    }

    private val _state = MutableStateFlow<State>(State(Pose(), FieldOfView(0f, 0f, 0f, 0f)))
    /** The current [State] of the view camera. */
    public val state: StateFlow<State> = _state.asStateFlow()

    override suspend fun update() {
        _state.emit(State(runtimeViewCamera.pose, runtimeViewCamera.fieldOfView))
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ViewCamera) return false
        return runtimeViewCamera == other.runtimeViewCamera
    }

    override fun hashCode(): Int = runtimeViewCamera.hashCode()
}
