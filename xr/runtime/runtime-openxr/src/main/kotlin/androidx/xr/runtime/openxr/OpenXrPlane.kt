/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.xr.runtime.openxr

import androidx.annotation.RestrictTo
import androidx.xr.arcore.internal.Anchor
import androidx.xr.arcore.internal.AnchorResourcesExhaustedException
import androidx.xr.arcore.internal.Plane
import androidx.xr.runtime.TrackingState
import androidx.xr.runtime.math.FloatSize2d
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Vector2

/** Wraps the native [XrTrackableANDROID] with the [androidx.xr.arcore.internal.Plane] interface. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public class OpenXrPlane
internal constructor(
    internal val planeId: Long,
    override val type: Plane.Type,
    internal val timeSource: OpenXrTimeSource,
    private val xrResources: XrResources,
) : Plane, Updatable {
    override var label: Plane.Label = Plane.Label.UNKNOWN
        private set

    override var centerPose: Pose = Pose()
        private set

    override var vertices: List<Vector2> = emptyList()
        private set

    override var extents: FloatSize2d = FloatSize2d()
        private set

    override var subsumedBy: Plane? = null
        private set

    override var trackingState: TrackingState = TrackingState.PAUSED
        private set

    override fun createAnchor(pose: Pose): Anchor {
        val xrTime = timeSource.getXrTime(timeSource.markNow())
        val anchorNativePointer = nativeCreateAnchorForPlane(planeId, pose, xrTime)
        checkNativeAnchorIsValid(anchorNativePointer)
        val anchor: Anchor = OpenXrAnchor(anchorNativePointer, xrResources)
        xrResources.addUpdatable(anchor as Updatable)
        return anchor
    }

    override fun update(xrTime: Long) {
        val planeState = nativeGetPlaneState(planeId, xrTime)
        if (planeState == null) {
            trackingState = TrackingState.PAUSED
            return
        }

        label = planeState.label
        trackingState = planeState.trackingState
        centerPose = planeState.centerPose
        extents = planeState.extents
        vertices = planeState.vertices.toList()

        if (planeState.subsumedByPlaneId != 0L) {
            subsumedBy = xrResources.trackablesMap[planeState.subsumedByPlaneId] as OpenXrPlane?
        }
    }

    private fun checkNativeAnchorIsValid(nativeAnchor: Long) {
        when (nativeAnchor) {
            -2L -> throw IllegalStateException("Failed to create anchor.") // kErrorRuntimeFailure
            -10L -> throw AnchorResourcesExhaustedException() // kErrorLimitReached
        }
    }

    private external fun nativeGetPlaneState(planeId: Long, timestampNs: Long): PlaneState?

    private external fun nativeCreateAnchorForPlane(
        planeId: Long,
        pose: Pose,
        timeStampNs: Long,
    ): Long
}

/**
 * Create a [androidx.xr.arcore.internal.Plane.Type] from an integer value corresponding to an
 * [XrPlaneTypeANDROID].
 */
internal fun Plane.Type.Companion.fromOpenXrType(type: Int): Plane.Type =
    when (type) {
        0 ->
            Plane.Type
                .HORIZONTAL_DOWNWARD_FACING // XR_PLANE_TYPE_HORIZONTAL_DOWNWARD_FACING_ANDROID
        1 -> Plane.Type.HORIZONTAL_UPWARD_FACING // XR_PLANE_TYPE_HORIZONTAL_UPWARD_FACING_ANDROID
        2 -> Plane.Type.VERTICAL // XR_PLANE_TYPE_VERTICAL_ANDROID
        else -> {
            throw IllegalArgumentException("Invalid plane type.")
        }
    }

/**
 * Create a [androidx.xr.arcore.internal.Plane.Label] from an integer value corresponding to an
 * [XrPlaneLabelANDROID].
 */
internal fun Plane.Label.Companion.fromOpenXrLabel(label: Int): Plane.Label =
    when (label) {
        0 -> Plane.Label.UNKNOWN // XR_PLANE_LABEL_UNKNOWN_ANDROID
        1 -> Plane.Label.WALL // XR_PLANE_LABEL_WALL_ANDROID
        2 -> Plane.Label.FLOOR // XR_PLANE_LABEL_FLOOR_ANDROID
        3 -> Plane.Label.CEILING // XR_PLANE_LABEL_CEILING_ANDROID
        4 -> Plane.Label.TABLE // XR_PLANE_LABEL_TABLE_ANDROID
        else -> {
            throw IllegalArgumentException("Invalid plane label.")
        }
    }
