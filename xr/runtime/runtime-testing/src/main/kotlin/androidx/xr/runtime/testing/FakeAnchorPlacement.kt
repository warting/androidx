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

import androidx.xr.scenecore.internal.AnchorPlacement
import androidx.xr.scenecore.internal.PlaneSemantic
import androidx.xr.scenecore.internal.PlaneType

/**
 * A test implementation of the [AnchorPlacement] interface, used to define and inspect anchor
 * placement rules in tests.
 *
 * This class specifies the conditions under which an entity can be anchored to a real-world plane.
 * An entity is eligible for anchoring if it is released near a plane that matches the criteria
 * defined by both the [planeTypeFilter] and [planeSemanticFilter]. A plane is considered a match if
 * its type is present in the [planeTypeFilter] AND its semantic label is present in the
 * [planeSemanticFilter].
 *
 * By default, both filters are initialized to allow anchoring on any plane. If either filter set is
 * empty, no planes will match, and anchoring will not occur.
 *
 * For plane-based anchoring to function, the [androidx.xr.runtime.Session] must be configured with
 * [androidx.xr.runtime.Config.PlaneTrackingMode.HORIZONTAL_AND_VERTICAL] to enable plane detection.
 *
 * When an entity is successfully anchored, its pose is adjusted so that its local Z-axis aligns
 * with the plane's normal vector (i.e., it sits flat against the surface).
 */
public class FakeAnchorPlacement
internal constructor(
    internal val planeTypeFilter: Set<@JvmSuppressWildcards PlaneType> = setOf(PlaneType.ANY),
    internal val planeSemanticFilter: Set<@JvmSuppressWildcards PlaneSemantic> =
        setOf(PlaneSemantic.ANY),
) : AnchorPlacement
