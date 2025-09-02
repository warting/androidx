/*
 * Copyright (C) 2024 The Android Open Source Project
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
@file:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)

package androidx.compose.remote.frontend.modifier

import androidx.annotation.RestrictTo
import androidx.compose.ui.graphics.TileMode

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class BlurEffect(
    public val radiusX: Float,
    public val radiusY: Float = radiusX,
    public val edgeTreatment: TileMode = TileMode.Clamp,
) : RenderEffect() {
    override fun toComposeRenderEffect(): androidx.compose.ui.graphics.RenderEffect {
        return androidx.compose.ui.graphics.BlurEffect(radiusX, radiusY, edgeTreatment)
    }
}
