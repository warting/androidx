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

package androidx.xr.compose.material3

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.xr.compose.spatial.EdgeOffset
import androidx.xr.compose.spatial.Orbiter
import androidx.xr.compose.spatial.OrbiterEdge
import androidx.xr.compose.spatial.OrbiterSettings
import androidx.xr.compose.subspace.layout.SpatialShape

/**
 * XR-specific properties for components that use horizontally-aligned [Orbiter]s.
 *
 * These properties should be provided via a `CompositionLocal` for the given component.
 *
 * The component should also define a publicly-visible default instance of
 * [HorizontalOrbiterProperties] and use it if the `CompositionLocal` is not set.
 */
@ExperimentalMaterial3XrApi
@Immutable
public class HorizontalOrbiterProperties(
    public val offset: EdgeOffset,
    public val position: OrbiterEdge.Horizontal,
    public val alignment: Alignment.Horizontal,
    public val settings: OrbiterSettings,
    public val shape: SpatialShape,
) {
    /**
     * Returns a new [HorizontalOrbiterProperties] with one or more properties changed.
     *
     * If `null` is provided for any value, the existing value will be used.
     */
    public fun copy(
        offset: EdgeOffset? = null,
        position: OrbiterEdge.Horizontal? = null,
        alignment: Alignment.Horizontal? = null,
        settings: OrbiterSettings? = null,
        shape: SpatialShape? = null,
    ): HorizontalOrbiterProperties =
        HorizontalOrbiterProperties(
            offset = offset ?: this.offset,
            position = position ?: this.position,
            alignment = alignment ?: this.alignment,
            settings = settings ?: this.settings,
            shape = shape ?: this.shape,
        )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is HorizontalOrbiterProperties) return false

        if (offset != other.offset) return false
        if (position != other.position) return false
        if (alignment != other.alignment) return false
        if (settings != other.settings) return false
        if (shape != other.shape) return false

        return true
    }

    override fun hashCode(): Int {
        var result = offset.hashCode()
        result = 31 * result + position.hashCode()
        result = 31 * result + alignment.hashCode()
        result = 31 * result + settings.hashCode()
        result = 31 * result + shape.hashCode()
        return result
    }

    override fun toString(): String {
        return "HorizontalOrbiterProperties(" +
            "offset=$offset, " +
            "position=$position, " +
            "alignment=$alignment, " +
            "settings=$settings, " +
            "shape=$shape" +
            ")"
    }
}

/**
 * XR-specific properties for components that use vertically-aligned [Orbiter]s.
 *
 * These properties should be provided via a `CompositionLocal` for the given component.
 *
 * The component should also define a publicly-visible default instance of
 * [VerticalOrbiterProperties] and use it if the `CompositionLocal` is not set.
 */
@ExperimentalMaterial3XrApi
@Immutable
public class VerticalOrbiterProperties(
    public val offset: EdgeOffset,
    public val position: OrbiterEdge.Vertical,
    public val alignment: Alignment.Vertical,
    public val settings: OrbiterSettings,
    public val shape: SpatialShape,
) {
    /**
     * Returns a new [VerticalOrbiterProperties] with one or more properties changed.
     *
     * If `null` is provided for any value, the existing value will be used.
     */
    public fun copy(
        offset: EdgeOffset? = null,
        position: OrbiterEdge.Vertical? = null,
        alignment: Alignment.Vertical? = null,
        settings: OrbiterSettings? = null,
        shape: SpatialShape? = null,
    ): VerticalOrbiterProperties =
        VerticalOrbiterProperties(
            offset = offset ?: this.offset,
            position = position ?: this.position,
            alignment = alignment ?: this.alignment,
            settings = settings ?: this.settings,
            shape = shape ?: this.shape,
        )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is VerticalOrbiterProperties) return false

        if (offset != other.offset) return false
        if (position != other.position) return false
        if (alignment != other.alignment) return false
        if (settings != other.settings) return false
        if (shape != other.shape) return false

        return true
    }

    override fun hashCode(): Int {
        var result = offset.hashCode()
        result = 31 * result + position.hashCode()
        result = 31 * result + alignment.hashCode()
        result = 31 * result + settings.hashCode()
        result = 31 * result + shape.hashCode()
        return result
    }

    override fun toString(): String {
        return "VerticalOrbiterProperties(" +
            "offset=$offset, " +
            "position=$position, " +
            "alignment=$alignment, " +
            "settings=$settings, " +
            "shape=$shape" +
            ")"
    }
}

@OptIn(ExperimentalMaterial3XrApi::class)
@Composable
internal fun VerticalOrbiter(
    properties: VerticalOrbiterProperties,
    content: @Composable () -> Unit,
) {
    Orbiter(
        position = properties.position,
        offset = properties.offset,
        alignment = properties.alignment,
        settings = properties.settings,
        shape = properties.shape,
        content = content,
    )
}

@OptIn(ExperimentalMaterial3XrApi::class)
@Composable
internal fun HorizontalOrbiter(
    properties: HorizontalOrbiterProperties,
    content: @Composable () -> Unit,
) {
    Orbiter(
        position = properties.position,
        offset = properties.offset,
        alignment = properties.alignment,
        settings = properties.settings,
        shape = properties.shape,
        content = content,
    )
}
