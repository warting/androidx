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

package androidx.xr.runtime.testing

import android.app.Activity
import android.content.Context
import android.view.View
import androidx.annotation.RestrictTo
import androidx.xr.runtime.math.Pose
import androidx.xr.scenecore.internal.ActivityPanelEntity
import androidx.xr.scenecore.internal.ActivitySpace
import androidx.xr.scenecore.internal.CameraViewActivityPose
import androidx.xr.scenecore.internal.Dimensions
import androidx.xr.scenecore.internal.Entity
import androidx.xr.scenecore.internal.GltfEntity
import androidx.xr.scenecore.internal.GltfFeature
import androidx.xr.scenecore.internal.PanelEntity
import androidx.xr.scenecore.internal.PixelDimensions
import androidx.xr.scenecore.internal.RenderingEntityFactory
import androidx.xr.scenecore.internal.SceneRuntime
import androidx.xr.scenecore.internal.SpatialCapabilities
import java.util.concurrent.Executor
import java.util.function.Consumer

/** Test-only implementation of [SceneRuntime] */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public class FakeSceneRuntime() : SceneRuntime, RenderingEntityFactory {
    override val spatialCapabilities: SpatialCapabilities = SpatialCapabilities(0)

    override val activitySpace: ActivitySpace = FakeActivitySpace()

    override fun getCameraViewActivityPose(
        @CameraViewActivityPose.CameraType cameraType: Int
    ): CameraViewActivityPose? = FakeCameraViewActivityPose()

    override fun createPanelEntity(
        context: Context,
        pose: Pose,
        view: View,
        dimensions: Dimensions,
        name: String,
        parent: Entity,
    ): PanelEntity = FakePanelEntity()

    override fun createPanelEntity(
        context: Context,
        pose: Pose,
        view: View,
        pixelDimensions: PixelDimensions,
        name: String,
        parent: Entity,
    ): PanelEntity = FakePanelEntity()

    override fun createActivityPanelEntity(
        pose: Pose,
        windowBoundsPx: PixelDimensions,
        name: String,
        hostActivity: Activity,
        parent: Entity,
    ): ActivityPanelEntity = FakeActivityPanelEntity()

    override fun createGltfEntity(
        feature: GltfFeature,
        pose: Pose,
        parentEntity: Entity,
    ): GltfEntity = FakeGltfEntity()

    override fun createGroupEntity(pose: Pose, name: String, parent: Entity): Entity = FakeEntity()

    override fun addSpatialCapabilitiesChangedListener(
        callbackExecutor: Executor,
        listener: Consumer<SpatialCapabilities>,
    ) {}

    override fun removeSpatialCapabilitiesChangedListener(
        listener: Consumer<SpatialCapabilities>
    ) {}

    override fun dispose() {}
}
