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

package androidx.xr.runtime.internal

import androidx.annotation.RestrictTo
import androidx.xr.runtime.VpsAvailabilityResult
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Ray
import java.util.UUID
import kotlin.collections.Collection
import kotlin.collections.List

/**
 * Describes the perception functionality that is required from a [Runtime] implementation. It is
 * expected that these functions are only valid while the [Runtime] is in a resumed state.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public interface PerceptionManager {
    /** Defines a tracked location in the physical world. */
    public fun createAnchor(pose: Pose): Anchor

    /** Performs a ray cast in the direction of the given [ray] in the latest camera view. */
    public fun hitTest(ray: Ray): List<HitResult>

    /** Retrieves all the [UUID] instances from [Anchor] objects that have been persisted. */
    public fun getPersistedAnchorUuids(): List<UUID>

    /** Loads an [Anchor] from local storage. */
    public fun loadAnchor(uuid: UUID): Anchor

    /** Loads an [Anchor] from a native pointer. */
    // TODO(b/373711152) : Remove this method once the Jetpack XR Runtime API migration is done.
    public fun loadAnchorFromNativePointer(nativePointer: Long): Anchor

    /** Deletes a persisted [Anchor] from local storage. */
    public fun unpersistAnchor(uuid: UUID)

    /** Checks the VPS availability at the given location. */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    public suspend fun checkVpsAvailability(
        latitude: Double,
        longitude: Double,
    ): VpsAvailabilityResult

    /** Returns the list of all known trackables. */
    public val trackables: Collection<Trackable>

    /** Hand tracking information for the left [Hand]. Only available on supported platforms. */
    public val leftHand: Hand?

    /** Hand tracking information for the right [Hand]. Only available on supported platforms. */
    public val rightHand: Hand?

    /** AR device tracking information. */
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX) public val arDevice: ArDevice

    /** Left View Camera information. */
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    public val leftRenderViewpoint: RenderViewpoint?

    /** Right View Camera information. */
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    public val rightRenderViewpoint: RenderViewpoint?

    /** Mono View Camera information. */
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    public val monoRenderViewpoint: RenderViewpoint?

    /** [Earth] tracking information. */
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX) public val earth: Earth

    /** Collection of [DepthMap]s for the current frame */
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX) public val depthMaps: List<DepthMap>

    /** Face tracking information for the face. Only available on supported platforms. */
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX) public val userFace: Face?
}
