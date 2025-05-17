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

package androidx.xr.glimmer

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.modifier.ModifierLocalModifierNode
import androidx.compose.ui.modifier.modifierLocalMapOf
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.SemanticsModifierNode
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * A surface is a fundamental building block in Glimmer. A surface represents a distinct visual area
 * or 'physical' boundary for components such as buttons and cards. A surface is responsible for:
 * 1) Clipping: a surface clips its children to the shape specified by [shape]
 * 2) Border: a surface draws an inner [border] to emphasize the boundary of the component
 * 3) Background: a surface has a background color of [color].
 * 4) Content color: a surface provides a [contentColor] through a ModifierLocal for text and icons
 *    inside the surface. By default this is calculated from the provided background color. See
 *    [ModifierLocalContentColor].
 *
 * @sample androidx.xr.glimmer.samples.SurfaceSample
 * @param shape the [Shape] used to clip this surface, and also used to draw the background and
 *   border
 * @param color the background [Color] for this surface
 * @param contentColor the [Color] for content inside this surface
 * @param border an optional inner border for this surface
 */
@Composable
public fun Modifier.surface(
    shape: Shape = SurfaceDefaults.Shape,
    color: Color = GlimmerTheme.colors.surface,
    contentColor: Color = calculateContentColor(color),
    border: BorderStroke? = SurfaceDefaults.border(),
): Modifier =
    this.clip(shape)
        .then(if (border != null) Modifier.border(border, shape) else Modifier)
        .background(color = color, shape = shape)
        .then(SurfaceNodeElement(contentColor))

/** Default values used for [surface]. */
public object SurfaceDefaults {
    /** The default [Shape] used for a [surface] */
    public val Shape: Shape = RoundedCornerShape(40.dp)

    /**
     * Create the default [BorderStroke] used for a [surface]. Use the other overload in order to
     * change the width or color.
     */
    @Composable
    public fun border(): BorderStroke {
        return GlimmerTheme.LocalGlimmerTheme.current.defaultSurfaceBorder
    }

    /**
     * Create the default [BorderStroke] used for a [surface], with optional overrides for [width]
     * and [color].
     *
     * @param width width of the border in [Dp]. Use [Dp.Hairline] for one-pixel border.
     * @param color color to paint the border with
     */
    @Composable
    public fun border(
        width: Dp = DefaultSurfaceBorderWidth,
        color: Color = GlimmerTheme.colors.outline,
    ): BorderStroke {
        return BorderStroke(width, color)
    }

    /** Returns the default (cached) border for a [surface]. */
    internal val GlimmerTheme.defaultSurfaceBorder: BorderStroke
        get() {
            return defaultSurfaceBorderCached
                ?: BorderStroke(DefaultSurfaceBorderWidth, colors.outline).also {
                    defaultSurfaceBorderCached = it
                }
        }
}

private class SurfaceNodeElement(private val contentColor: Color) :
    ModifierNodeElement<SurfaceNode>() {
    override fun create(): SurfaceNode = SurfaceNode(contentColor)

    override fun update(node: SurfaceNode) = node.update(contentColor)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SurfaceNodeElement) return false

        if (contentColor != other.contentColor) return false

        return true
    }

    override fun hashCode(): Int {
        return contentColor.hashCode()
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "surface"
        properties["contentColor"] = contentColor
    }
}

private class SurfaceNode(private var contentColor: Color) :
    SemanticsModifierNode, ModifierLocalModifierNode, Modifier.Node() {
    override val providedValues = modifierLocalMapOf(ModifierLocalContentColor to contentColor)

    override val shouldAutoInvalidate = false

    fun update(contentColor: Color) {
        this.contentColor = contentColor
        provide(ModifierLocalContentColor, contentColor)
    }

    override fun SemanticsPropertyReceiver.applySemantics() {
        isTraversalGroup = true
    }
}

/** Default border width for a [surface]. */
private val DefaultSurfaceBorderWidth = 3.dp
