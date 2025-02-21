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

/**
 * XR Runtime spatial capabilities.
 *
 * @param capabilities the set of capabilities enabled for the platform.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public class SpatialCapabilities(public val capabilities: Int) {

    /** Spatial Capabilities for SceneCore Platform. */
    @Retention(AnnotationRetention.SOURCE)
    public annotation class SpatialCapability {
        public companion object {
            public const val SPATIAL_CAPABILITY_UI: Int = 1.shl(0)
            public const val SPATIAL_CAPABILITY_3D_CONTENT: Int = 1.shl(1)
            public const val SPATIAL_CAPABILITY_PASSTHROUGH_CONTROL: Int = 1.shl(2)
            public const val SPATIAL_CAPABILITY_APP_ENVIRONMENT: Int = 1.shl(3)
            public const val SPATIAL_CAPABILITY_SPATIAL_AUDIO: Int = 1.shl(4)
            public const val SPATIAL_CAPABILITY_EMBED_ACTIVITY: Int = 1.shl(5)
        }
    }

    public fun hasCapability(@SpatialCapability capability: Int): Boolean =
        (capabilities and capability) != 0

    public companion object {
        /** The activity can spatialize itself by e.g. adding a spatial panel. */
        public const val SPATIAL_CAPABILITY_UI: Int = 1.shl(0)

        /** The activity can create 3D contents. */
        public const val SPATIAL_CAPABILITY_3D_CONTENT: Int = 1.shl(1)

        /** The activity can enable or disable passthrough. */
        public const val SPATIAL_CAPABILITY_PASSTHROUGH_CONTROL: Int = 1.shl(2)

        /** The activity can set its own environment. */
        public const val SPATIAL_CAPABILITY_APP_ENVIRONMENT: Int = 1.shl(3)

        /** The activity can use spatial audio. */
        public const val SPATIAL_CAPABILITY_SPATIAL_AUDIO: Int = 1.shl(4)

        /** The activity can spatially embed another activity. */
        public const val SPATIAL_CAPABILITY_EMBED_ACTIVITY: Int = 1.shl(5)
    }
}
