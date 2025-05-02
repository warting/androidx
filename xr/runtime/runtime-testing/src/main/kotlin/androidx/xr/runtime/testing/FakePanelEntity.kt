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

package androidx.xr.runtime.testing

import androidx.annotation.RestrictTo
import androidx.xr.runtime.internal.Dimensions
import androidx.xr.runtime.internal.PanelEntity
import androidx.xr.runtime.internal.PixelDimensions
import androidx.xr.runtime.math.Vector3

// TODO: b/405218432 - Implement this correctly instead of stubbing it out.
/** Test-only implementation of [PanelEntity] */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public open class FakePanelEntity : PanelEntity, FakeEntity() {
    override var sizeInPixels: PixelDimensions = PixelDimensions(0, 0)

    override var cornerRadius: Float = 0.0f

    override val pixelDensity: Vector3 = Vector3()

    override var size: Dimensions = Dimensions(0.0f, 0.0f, 0.0f)
}
