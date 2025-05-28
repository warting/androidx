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

package androidx.compose.ui.graphics.shadow

import androidx.compose.ui.graphics.Shape

/**
 * Class responsible for managing shadow related dependencies. This includes creation and caching of
 * various [DropShadowPainter] and [InnerShadowPainter] instances based on the provided [DropShadow]
 * and [InnerShadow] parameters
 */
interface ShadowContext {

    /**
     * Return an [InnerShadowPainter] instance for the provided [shape] and [innerShadow]. This may
     * return a previously allocated [InnerShadowPainter] instance for the same parameters, for
     * efficiency. For example, a design system that leverages shadows may have the same properties
     * across multiple UI elements. In this case, the same dependencies for an [InnerShadowPainter]
     * can be reused.
     */
    fun createInnerShadowPainter(shape: Shape, innerShadow: InnerShadow): InnerShadowPainter =
        InnerShadowPainter(shape, innerShadow)

    /**
     * Return a [DropShadowPainter] instance for the provided [shape] and [dropShadow]. This may
     * return a previously allocated [DropShadowPainter] instance for the same parameters, for
     * efficiency. For example, a design system that leverages shadows may have the same properties
     * across multiple UI elements. In this case, the same dependencies for a [DropShadowPainter]
     * can be reused.
     */
    fun createDropShadowPainter(shape: Shape, dropShadow: DropShadow): DropShadowPainter =
        DropShadowPainter(shape, dropShadow)

    /**
     * Clear all previously cached [InnerShadowPainter] and [DropShadowPainter] instances alongside
     * all other shadow dependencies.
     */
    fun clearCache() {}
}
