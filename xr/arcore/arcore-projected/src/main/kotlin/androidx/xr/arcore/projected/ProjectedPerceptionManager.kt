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

package androidx.xr.arcore.projected

import androidx.annotation.RestrictTo
import androidx.xr.arcore.internal.Anchor
import androidx.xr.arcore.internal.ArDevice
import androidx.xr.arcore.internal.DepthMap
import androidx.xr.arcore.internal.Earth
import androidx.xr.arcore.internal.Face
import androidx.xr.arcore.internal.Hand
import androidx.xr.arcore.internal.HitResult
import androidx.xr.arcore.internal.PerceptionManager
import androidx.xr.arcore.internal.RenderViewpoint
import androidx.xr.arcore.internal.Trackable
import androidx.xr.runtime.VpsAvailabilityErrorInternal
import androidx.xr.runtime.VpsAvailabilityResult
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Quaternion
import androidx.xr.runtime.math.Ray
import androidx.xr.runtime.math.Vector3
import java.util.UUID
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * Implementation of the perception capabilities of a runtime using Projected.
 *
 * @property timeSource The time source to use for the perception manager.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public class ProjectedPerceptionManager
internal constructor(private val timeSource: ProjectedTimeSource) : PerceptionManager {
    private val xrResources = XrResources()
    internal var service: IProjectedPerceptionService? = null

    /**
     * Creates an anchor in the scene.
     *
     * This method calls the [Session.createAnchor] method.
     *
     * @param pose The pose of the anchor.
     * @return The created anchor.
     */
    override fun createAnchor(pose: Pose): Anchor {
        throw NotImplementedError("Create anchor is currently not supported by Projected.")
    }

    /**
     * Performs a hit test against the scene.
     *
     * This method calls the [Frame.hitTest] method.
     *
     * @param ray The ray to perform the hit test against.
     * @return The list of hit results.
     */
    override fun hitTest(ray: Ray): List<HitResult> {
        throw NotImplementedError("Hit test is currently not supported by Projected.")
    }

    /**
     * Returns the UUIDs of all persisted anchors.
     *
     * This method throws [NotImplementedError] because Projected does not support anchor
     * persistence.
     */
    override fun getPersistedAnchorUuids(): List<UUID> {
        throw NotImplementedError("Anchor persistence is currently not supported by Projected.")
    }

    /**
     * Loads an anchor from the given UUID.
     *
     * This method throws [NotImplementedError] because Projected does not support anchor
     * persistence.
     */
    override fun loadAnchor(uuid: UUID): Anchor {
        throw NotImplementedError("Anchor persistence is currently not supported by Projected.")
    }

    /**
     * Loads an anchor from the given native pointer.
     *
     * This method throws [NotImplementedError] because Projected does not support native pointers.
     */
    override fun loadAnchorFromNativePointer(nativePointer: Long): Anchor {
        throw NotImplementedError("Native pointers are not supported by Projected.")
    }

    /**
     * Unpersists an anchor with the given UUID.
     *
     * This method throws [NotImplementedError] because Projected does not support anchor
     * persistence.
     */
    override fun unpersistAnchor(uuid: UUID) {
        throw NotImplementedError("Anchor persistence is currently not supported by Projected.")
    }

    /** Gets the VPS availability at the given location. */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    override public suspend fun checkVpsAvailability(
        latitude: Double,
        longitude: Double,
    ): VpsAvailabilityResult = suspendCancellableCoroutine { continuation ->
        val callback =
            object : IVpsAvailabilityCallback.Stub() {
                override fun onVpsAvailabilityChanged(vpsState: Int) {
                    // TODO b/438071712 - map the vpsState to VpsResult
                    val vpsResult = VpsAvailabilityErrorInternal()
                    continuation.resume(vpsResult)
                }
            }
        try {
            this.service?.checkVpsAvailability(latitude, longitude, callback)
        } catch (e: Exception) {
            continuation.resumeWithException(e)
        }
    }

    override val trackables: Collection<Trackable> = emptyList()

    /**
     * Returns the left hand.
     *
     * Projected does not support hand tracking, so this property is always null.
     */
    override val leftHand: Hand? = null

    /**
     * Returns the right hand.
     *
     * Projected does not support hand tracking, so this property is always null.
     */
    override val rightHand: Hand? = null

    /** Returns the [Earth] instance. */
    // @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    override val earth: Earth = ProjectedEarth(xrResources)

    /** Returns the [ArDevice] instance. */
    override val arDevice: ArDevice
        get() = xrResources.arDevice

    /** Returns the left [RenderViewpoint] object. */
    override val leftRenderViewpoint: ProjectedRuntimeRenderViewpoint? =
        ProjectedRuntimeRenderViewpoint(Pose(Vector3(1f, 0f, 0f), Quaternion.Identity))

    /** Returns the right [RenderViewpoint] object. */
    override val rightRenderViewpoint: ProjectedRuntimeRenderViewpoint? =
        ProjectedRuntimeRenderViewpoint(Pose(Vector3(0f, 1f, 0f), Quaternion.Identity))

    /** Returns the mono [RenderViewpoint] object. */
    override val monoRenderViewpoint: ProjectedRuntimeRenderViewpoint? =
        ProjectedRuntimeRenderViewpoint(Pose(Vector3(0f, 0f, 1f), Quaternion.Identity))

    /** Left [androidx.xr.arcore.internal.DepthMap]'s current frame information */
    override val leftDepthMap: DepthMap? = null

    /** Right [androidx.xr.arcore.internal.DepthMap]'s current frame information */
    override val rightDepthMap: DepthMap? = null

    /** Mono [androidx.xr.arcore.internal.DepthMap]'s current frame information */
    override val monoDepthMap: DepthMap? = null

    /**
     * Returns the face
     *
     * Projected does not support face tracking, so this property is always null.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX) override val userFace: Face? = null

    /**
     * Updates the perception manager.
     *
     * Sets the display geometry of the underlying [Session] if the display has changed. Grabs the
     * latest [Frame] from the underlying [Session], and if new, updates the internal state of the
     * perception manager.
     */
    internal fun update() {
        throw NotImplementedError("update is currently not supported by Projected.")
    }

    /**
     * Clears any internal state of the perception manager.
     *
     * Currently, this method only clears the [xrResources] instance.
     */
    internal fun clear() {
        throw NotImplementedError("clear is currently not supported by Projected.")
    }

    public fun setDisplayRotation(rotation: Int, width: Int, height: Int) {
        throw NotImplementedError("setDisplayRotation is currently not supported by Projected.")
    }
}
