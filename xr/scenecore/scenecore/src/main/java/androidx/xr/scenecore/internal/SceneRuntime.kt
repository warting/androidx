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

package androidx.xr.scenecore.internal

import android.app.Activity
import androidx.annotation.RestrictTo
import androidx.xr.runtime.internal.JxrRuntime
import androidx.xr.runtime.math.Pose
import java.util.concurrent.Executor
import java.util.function.Consumer

/**
 * Defines the contract for a platform-agnostic runtime that manages the scene graph and spatial
 * logic backend.
 *
 * This interface is responsible for the logical structure of the XR experience, managing the
 * hierarchy of entities, their transformations, and their behaviors. It provides the core
 * functionalities for spatial computing, such as world tracking, plane detection, and anchoring
 * objects to the physical environment. It also handles user interaction components and spatial
 * audio.
 *
 * This API is intended for internal use only and is not a public API.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public interface SceneRuntime : JxrRuntime {
    /** Return the Spatial Capabilities set that are currently supported by the platform. */
    public val spatialCapabilities: SpatialCapabilities

    /** Returns the Activity Space entity at the root of the scene. */
    public val activitySpace: ActivitySpace

    /**
     * Returns the CameraViewActivityPose for the specified camera type or null if it is not
     * ready/available.
     *
     * @param cameraType The type of camera to retrieve the pose for.
     */
    public fun getCameraViewActivityPose(
        @CameraViewActivityPose.CameraType cameraType: Int
    ): CameraViewActivityPose?

    /**
     * Factory function to create ActivityPanel to launch/move activity into.
     *
     * @param pose Initial pose of the panel.
     * @param windowBoundsPx Boundary for the window
     * @param name Name of the panel.
     * @param hostActivity Activity to host the panel.
     * @param parent Parent entity.
     */
    public fun createActivityPanelEntity(
        pose: Pose,
        windowBoundsPx: PixelDimensions,
        name: String,
        hostActivity: Activity,
        parent: Entity,
    ): ActivityPanelEntity

    /**
     * A factory function to create a group entity. This entity is used as a connection point for
     * attaching children entities and managing them (i.e. setPose()) as a group.
     *
     * @param pose Initial pose of the entity.
     * @param name Name of the entity.
     * @param parent Parent entity.
     */
    public fun createGroupEntity(pose: Pose, name: String, parent: Entity): Entity

    /**
     * Adds the given {@link Consumer} as a listener to be invoked when this Session's current
     * SpatialCapabilities change. {@link Consumer#accept(SpatialCapabilities)} will be invoked on
     * the given Executor.
     *
     * @param callbackExecutor Executor on which the listener will be invoked.
     * @param listener Listener to be invoked when the Session's SpatialCapabilities change.
     */
    @Suppress("ExecutorRegistration")
    public fun addSpatialCapabilitiesChangedListener(
        callbackExecutor: Executor,
        listener: Consumer<SpatialCapabilities>,
    )

    /**
     * Releases the given {@link Consumer} from receiving updates when the Session's {@link
     * SpatialCapabilities} change.
     *
     * @param listener Listener to be removed from the list of listeners.
     */
    public fun removeSpatialCapabilitiesChangedListener(listener: Consumer<SpatialCapabilities>)

    /** Disposes of the resources used by this runtime */
    public fun dispose()
}
