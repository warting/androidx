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

package androidx.xr.runtime.internal

import androidx.annotation.RestrictTo
import com.google.common.util.concurrent.ListenableFuture

/**
 * RenderingRuntime encapsulates all the platform-specific rendering-related operations. Its
 * responsibilities include toggle the render loop, loading assets, and creating renderable scene
 * entities.
 *
 * It is designed to work in tandem with a [SceneRuntime], which manages the scene graph. An
 * instance of `RenderingRuntime` is always created for a specific [SceneRuntime], and both are
 * expected to operate within the same context and lifecycle.
 *
 * This API is not intended to be used by applications.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public interface RenderingRuntime {
    /**
     * Creates a water material by querying it from the system's built-in materials. The future
     * returned by this method will fire listeners on the UI thread if Runnable::run is supplied.
     *
     * @param isAlphaMapVersion True if the water material should be the alpha map version.
     * @return A ListenableFuture containing a WaterMaterial backed by an imp::WaterMaterial. The
     *   WaterMaterial can be destroyed by passing it to destroyNativeObject.
     */
    @Suppress("AsyncSuffixFuture")
    public fun createWaterMaterial(isAlphaMapVersion: Boolean): ListenableFuture<MaterialResource>

    /**
     * Destroys the given water material resource.
     *
     * @param material The name of the WaterMaterial to destroy.
     */
    public fun destroyWaterMaterial(material: MaterialResource)

    /** Starts the renderer. */
    public fun startRenderer()

    /** Stops the renderer. */
    public fun stopRenderer()

    /** Disposes of the resources used by this runtime. */
    public fun dispose()
}
