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

package androidx.xr.scenecore

import android.util.Log
import androidx.annotation.IntDef
import androidx.annotation.RestrictTo
import androidx.concurrent.futures.ResolvableFuture
import androidx.xr.runtime.internal.ActivityPose as RtActivityPose
import androidx.xr.runtime.internal.CameraViewActivityPose as RtCameraViewActivityPose
import androidx.xr.runtime.internal.HeadActivityPose as RtHeadActivityPose
import androidx.xr.runtime.internal.JxrPlatformAdapter
import androidx.xr.runtime.internal.PerceptionSpaceActivityPose as RtPerceptionSpaceActivityPose
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Vector3
import com.google.common.util.concurrent.ListenableFuture

/**
 * Interface for a ActivityPose.
 *
 * A ActivityPose contains a pose in activity space and it's pose can be transformed into a pose
 * relative to another ActivityPose.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public interface ActivityPose {

    /**
     * Returns the activity space pose for this ActivityPose.
     *
     * @return Current [Pose] relative to the activity space root.
     */
    public fun getActivitySpacePose(): Pose

    /**
     * Returns a pose relative to this ActivityPose transformed into a pose relative to the
     * destination.
     *
     * @param pose A pose in this ActivityPose's local coordinate space.
     * @param destination The ActivityPose which the returned pose will be relative to.
     * @return The pose relative to the destination ActivityPose.
     */
    public fun transformPoseTo(pose: Pose, destination: ActivityPose): Pose

    /** A filter for which Scenes to hit test with ActivityPose.hitTest */
    public object HitTestFilter {
        /** Register hit tests for the scene which this Activity pose belongs to. */
        public const val SELF_SCENE: Int = 1 shl 0
        /**
         * Register hit tests only for other scenes. An Application will only have access to other
         * scenes if it has the android.permission.ACCESS_OVERLAY_SPACE permission.
         */
        public const val OTHER_SCENES: Int = 1 shl 1
    }

    @Retention(AnnotationRetention.SOURCE)
    @Suppress("PublicTypedef")
    @IntDef(flag = true, value = [HitTestFilter.SELF_SCENE, HitTestFilter.OTHER_SCENES])
    public annotation class HitTestFilterValue

    /**
     * Creates a hit test from the specified origin in the specified direction into the scene.
     *
     * @param origin The translation of the origin of the hit test relative to this ActivityPose.
     * @param direction The direction for the hit test ray from the origin.
     * @return a ListenableFuture<HitResult>. The HitResult describes if it hit something and where
     *   relative to this [ActivityPose]. Listeners will be called on the main thread if
     *   Runnable::run is supplied.
     */
    public fun hitTestAsync(origin: Vector3, direction: Vector3): ListenableFuture<HitTestResult>

    /**
     * Creates a hit test from the specified origin in the specified direction into the scene.
     *
     * @param origin The translation of the origin of the hit test relative to this ActivityPose.
     * @param direction The direction for the hit test ray from the origin
     * @param hitTestFilter Filter for which scenes to hit test. Hitting other scenes is only
     *   allowed for apps with the `android.permission.ACCESS_OVERLAY_SPACE` permission.
     * @return a ListenableFuture<HitResult>. The HitResult describes if it hit something and where
     *   relative to this [ActivityPose]. Listeners will be called on the main thread if
     *   Runnable::run is supplied.
     */
    public fun hitTestAsync(
        origin: Vector3,
        direction: Vector3,
        @HitTestFilterValue hitTestFilter: Int,
    ): ListenableFuture<HitTestResult>
}

/**
 * The BaseActivityPose is an implementation of ActivityPose interface that wraps a platformAdapter
 * ActivityPose.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public abstract class BaseActivityPose<out RtActivityPoseType : RtActivityPose>(
    internal val rtActivityPose: RtActivityPoseType
) : ActivityPose {
    private companion object {
        private const val TAG = "BaseRtActivityPose"
    }

    override fun getActivitySpacePose(): Pose {
        return rtActivityPose.activitySpacePose
    }

    override fun transformPoseTo(pose: Pose, destination: ActivityPose): Pose {
        if (destination !is BaseActivityPose<RtActivityPose>) {
            Log.e(TAG, "Destination must be a subclass of BaseActivityPose!")
            return Pose.Identity
        }
        return rtActivityPose.transformPoseTo(pose, destination.rtActivityPose)
    }

    override fun hitTestAsync(
        origin: Vector3,
        direction: Vector3,
        @ActivityPose.HitTestFilterValue hitTestFilter: Int,
    ): ListenableFuture<HitTestResult> {
        val hitTestRtFuture =
            this.rtActivityPose.hitTest(origin, direction, hitTestFilter.toRtHitTestFilter())
        val resultFuture = ResolvableFuture.create<HitTestResult>()
        hitTestRtFuture.addListener(
            {
                try {
                    val hitTestRt = hitTestRtFuture.get()
                    resultFuture.set(hitTestRt.toHitTestResult())
                } catch (e: Exception) {
                    if (e is InterruptedException) {
                        Thread.currentThread().interrupt()
                    }
                    resultFuture.setException(e)
                }
            },
            Runnable::run,
        )
        return resultFuture
    }

    override fun hitTestAsync(
        origin: Vector3,
        direction: Vector3
    ): ListenableFuture<HitTestResult> {
        return hitTestAsync(
            origin,
            direction,
            ActivityPose.HitTestFilter.SELF_SCENE.toRtHitTestFilter(),
        )
    }
}

/** A ActivityPose which tracks a camera's position and view into physical space. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public class CameraView
private constructor(private val rtCameraViewActivityPose: RtCameraViewActivityPose) :
    BaseActivityPose<RtCameraViewActivityPose>(rtCameraViewActivityPose) {

    internal companion object {
        internal fun createLeft(platformAdapter: JxrPlatformAdapter): CameraView? {
            val cameraViewActivityPose =
                platformAdapter.getCameraViewActivityPose(
                    RtCameraViewActivityPose.CameraType.CAMERA_TYPE_LEFT_EYE
                )
            return cameraViewActivityPose?.let { CameraView(it) }
        }

        internal fun createRight(platformAdapter: JxrPlatformAdapter): CameraView? {
            val cameraViewActivityPose =
                platformAdapter.getCameraViewActivityPose(
                    RtCameraViewActivityPose.CameraType.CAMERA_TYPE_RIGHT_EYE
                )
            return cameraViewActivityPose?.let { CameraView(it) }
        }
    }

    /** Describes the type of camera that this CameraView represents. */
    public enum class CameraType {
        /** This CameraView represents an unknown camera view. */
        UNKNOWN,

        /** This CameraView represents the user's left eye. */
        LEFT_EYE,

        /** This CameraView represents the user's right eye. */
        RIGHT_EYE,
    }

    public val cameraType: CameraType = CameraType.UNKNOWN

    /** Gets the FOV for the camera */
    public val fov: Fov
        get() {
            val rtFov = rtCameraViewActivityPose.fov
            return Fov(rtFov.angleLeft, rtFov.angleRight, rtFov.angleUp, rtFov.angleDown)
        }
}

/**
 * Head is a ActivityPose used to track the position of the user's head. If there is a left and
 * right camera it is calculated as the position between the two.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public class Head private constructor(rtActivityPose: RtHeadActivityPose) :
    BaseActivityPose<RtHeadActivityPose>(rtActivityPose) {

    internal companion object {

        /** Factory function for creating [Head] instance. */
        internal fun create(platformAdapter: JxrPlatformAdapter): Head? {
            return platformAdapter.headActivityPose?.let { Head(it) }
        }
    }
}

/**
 * PerceptionSpace is ActivityPose used to track the origin of the space used by ARCore for XR APIs.
 */
// TODO: b/360870690 - Remove suppression annotation when API council review is complete.
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public class PerceptionSpace private constructor(rtActivityPose: RtPerceptionSpaceActivityPose) :
    BaseActivityPose<RtPerceptionSpaceActivityPose>(rtActivityPose) {

    internal companion object {

        /** Factory function for creating [PerceptionSpace] instance. */
        internal fun create(platformAdapter: JxrPlatformAdapter): PerceptionSpace =
            PerceptionSpace(platformAdapter.perceptionSpaceActivityPose)
    }
}
