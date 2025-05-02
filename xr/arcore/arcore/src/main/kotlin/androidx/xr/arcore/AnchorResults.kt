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

package androidx.xr.arcore

import androidx.annotation.RestrictTo

/** Result of a [Anchor.create] call. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX) public sealed class AnchorCreateResult

/**
 * Result of a successful [Anchor.create] call.
 *
 * @property anchor the [Anchor] that was created.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public class AnchorCreateSuccess(public val anchor: Anchor) : AnchorCreateResult()

/**
 * Result of an unsuccessful [Anchor.create] call. The resources allocated for anchors has been
 * exhausted.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public class AnchorCreateResourcesExhausted() : AnchorCreateResult()

/** Result of an unsuccessful [Anchor.create] call. Required tracking is not available. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public class AnchorCreateNotTracking() : AnchorCreateResult()

/** Result of an unsuccessful [Anchor.load] call. The anchor was loaded from an invalid UUID. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public class AnchorLoadInvalidUuid() : AnchorCreateResult()
