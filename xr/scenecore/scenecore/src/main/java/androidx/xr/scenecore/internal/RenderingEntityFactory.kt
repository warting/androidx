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

import androidx.annotation.RestrictTo
import androidx.xr.runtime.math.Pose

/**
 * An interface for creating entities with rendering features.
 *
 * This interface is implemented by a [SceneRuntime] instance to provide rendering-specific entity
 * creation methods for use by a [RenderingRuntime] instance. By separating these methods into a
 * distinct interface, we avoid exposing internal rendering operations on the public [SceneRuntime]
 * API.
 *
 * The intended usage is for a [RenderingRuntime] to cast its [SceneRuntime] instance to
 * `RenderingEntityFactory` to access these factory methods.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public interface RenderingEntityFactory {
    /**
     * A factory function to create a SceneCore GltfEntity. The parent may be the activity space or
     * GltfEntity in the scene.
     */
    public fun createGltfEntity(feature: GltfFeature, pose: Pose, parentEntity: Entity): GltfEntity
}
