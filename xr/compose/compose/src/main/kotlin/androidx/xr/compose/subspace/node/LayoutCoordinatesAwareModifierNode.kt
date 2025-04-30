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

package androidx.xr.compose.subspace.node

import androidx.annotation.RestrictTo
import androidx.xr.compose.subspace.layout.SubspaceLayoutCoordinates

/**
 * A [SubspaceModifier.Node] whose [onLayoutCoordinates] callback is invoked when the layout
 * coordinates of the layout node may have changed.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public interface LayoutCoordinatesAwareModifierNode {
    /**
     * Called with the final [SubspaceLayoutCoordinates] of the layout node after placement. The
     * coordinates value may or may not have changed since the last callback.
     */
    public fun onLayoutCoordinates(coordinates: SubspaceLayoutCoordinates)
}
