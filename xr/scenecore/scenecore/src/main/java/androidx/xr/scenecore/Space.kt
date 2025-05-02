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

import androidx.annotation.IntDef
import androidx.annotation.RestrictTo

@IntDef(Space.PARENT, Space.ACTIVITY, Space.REAL_WORLD)
@Retention(AnnotationRetention.SOURCE)
internal annotation class SpaceValue

/** Coordinate spaces in which to apply the transformation values. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public object Space {
    /** The local coordinate space of an [Entity], relative to its parent. */
    public const val PARENT: Int = 0
    /** The global coordinate space, at the root of the scene graph for the activity. */
    public const val ACTIVITY: Int = 1
    /** The global coordinate space, unscaled, at the root of the scene graph of the activity. */
    public const val REAL_WORLD: Int = 2
}
