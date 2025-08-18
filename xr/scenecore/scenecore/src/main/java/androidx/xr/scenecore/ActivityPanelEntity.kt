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

import android.app.Activity
import android.content.Intent
import androidx.xr.runtime.Session
import androidx.xr.runtime.internal.ActivityPanelEntity as RtActivityPanelEntity
import androidx.xr.runtime.internal.JxrPlatformAdapter
import androidx.xr.runtime.internal.LifecycleManager
import androidx.xr.runtime.math.IntSize2d
import androidx.xr.runtime.math.Pose

/**
 * ActivityPanelEntity creates a spatial panel for embedding an [Activity] in Android XR. Users can
 * either use an [Intent] to launch an Activity in the given panel or provide an instance of
 * Activity to move into this panel. In order to launch and embed an activity,
 * [SpatialCapabilities.SPATIAL_CAPABILITY_EMBED_ACTIVITY] capability is required. Calling
 * [Entity.dispose] on this Entity will destroy the underlying Activity.
 */
public class ActivityPanelEntity
private constructor(
    private val lifecycleManager: LifecycleManager,
    private val rtActivityPanelEntity: RtActivityPanelEntity,
    entityManager: EntityManager,
) : PanelEntity(lifecycleManager, rtActivityPanelEntity, entityManager) {

    /**
     * Starts an [Activity] in the given panel. Subsequent calls to this method will replace the
     * already existing Activity in the panel with the new one. The panel will not be visible until
     * an Activity is successfully launched. This will fail if the [Scene] does not have the
     * [SpatialCapabilities.SPATIAL_CAPABILITY_EMBED_ACTIVITY] capability. This method will not
     * provide any information about when the Activity successfully launches.
     *
     * @param intent Intent to launch the activity.
     */
    public fun startActivity(intent: Intent) {
        rtActivityPanelEntity.launchActivity(intent, null)
    }

    /**
     * Moves the given [Activity] into this panel. This will fail if the application does not have
     * the [SpatialCapabilities.SPATIAL_CAPABILITY_EMBED_ACTIVITY] capability.
     *
     * @param activity Activity to move into this panel.
     */
    public fun moveActivity(activity: Activity) {
        rtActivityPanelEntity.moveActivity(activity)
    }

    public companion object {
        internal fun create(
            lifecycleManager: LifecycleManager,
            adapter: JxrPlatformAdapter,
            entityManager: EntityManager,
            pixelDimensions: IntSize2d,
            name: String,
            hostActivity: Activity,
            pose: Pose = Pose.Identity,
        ): ActivityPanelEntity =
            ActivityPanelEntity(
                lifecycleManager,
                adapter.createActivityPanelEntity(
                    pose,
                    pixelDimensions.toRtPixelDimensions(),
                    name,
                    hostActivity,
                    adapter.activitySpaceRootImpl,
                ),
                entityManager,
            )

        /**
         * Public factory function for a spatial ActivityPanelEntity.
         *
         * @param session XR [Session] to create the ActivityPanelEntity.
         * @param pixelDimensions Bounds for the panel surface in pixels.
         * @param name Name of the panel.
         * @param pose [Pose] of this entity relative to its parent, the default value is
         *   [Pose.Identity].
         * @return an ActivityPanelEntity instance.
         */
        @JvmOverloads
        @JvmStatic
        public fun create(
            session: Session,
            pixelDimensions: IntSize2d,
            name: String,
            pose: Pose = Pose.Identity,
        ): ActivityPanelEntity =
            ActivityPanelEntity.create(
                session.runtime.lifecycleManager,
                session.platformAdapter,
                session.scene.entityManager,
                pixelDimensions,
                name,
                session.activity,
                pose,
            )
    }
}
