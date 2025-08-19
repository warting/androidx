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
import androidx.xr.runtime.NodeHolder

/**
 * Defines the internal rendering implementation for an entity.
 *
 * This feature provides the rendering logic and manages the underlying extension node and its
 * associated resources. An instance of a `RenderingFeature` is injected into an entity that
 * requires rendering API support.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public interface RenderingFeature {
    /** Holds the underlying extension Node for the corresponding entity's creation. */
    public val nodeHolder: NodeHolder<*>

    /**
     * Disposes the resources used by the feature. This is called by the corresponding entity's
     * dispose method.
     */
    public fun dispose()
}
