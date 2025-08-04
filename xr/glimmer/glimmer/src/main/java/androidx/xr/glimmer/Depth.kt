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

import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.ShadowScope
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.graphics.shadow.lerp

/**
 * Depth establishes a sense of hierarchy by using shadows to occlude content underneath. Depth
 * consists of two shadow layers, [layer1] and [layer2]. [layer2] is drawn on top of [layer1]:
 *
 *     _________________
 *    |    _________    |
 *    |   | content |   |
 *    |   |_________|   |
 *    |   ___________   |
 *    |  |  layer 2  |  |
 *    |  |___________|  |
 *    |  _____________  |
 *    | |   layer 1   | |
 *    | |_____________| |
 *    |_________________|
 *
 * [GlimmerTheme.depthLevels] provides theme defined levels of depth that should be used to add
 * depth to surfaces.
 *
 * Higher level components apply depth automatically when needed, and depth can also be configured
 * through [surface]. To manually render depth shadows for advanced use-cases, see the [depth]
 * [Modifier].
 *
 * @param layer1 the 'base' [Shadow] layer, drawn first
 * @param layer2 the second [Shadow] layer, drawn on top of [layer1]
 */
@Immutable
public class Depth(internal val layer1: Shadow, internal val layer2: Shadow) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Depth) return false

        if (layer1 != other.layer1) return false
        if (layer2 != other.layer2) return false

        return true
    }

    override fun hashCode(): Int {
        var result = layer1.hashCode()
        result = 31 * result + layer2.hashCode()
        return result
    }
}

/**
 * Renders shadows for the provided [depth].
 *
 * @param depth Depth to render shadows for. If `null`, no shadows will be rendered.
 * @param shape [Shape] of the shadows
 */
public fun Modifier.depth(depth: Depth?, shape: Shape): Modifier {
    if (depth == null) return this
    return this
        // dropShadow draws the shadow, and then the content on top. So in order to get layer2 to
        // render on top of layer1, we draw layer1 first - this means that layer1's dropShadow will
        // draw its shadow, and then its content on top (since the 'content' includes children
        // modifiers, this includes layer2's shadow).
        .dropShadow(shape, depth.layer1)
        .dropShadow(shape, depth.layer2)
}

/**
 * Renders depth shadows by lerping between the provided [from] and [to] depths using [progress].
 * This allows for efficient animation - to render a static depth, see the other overload.
 *
 * @param from Depth to render shadows for when [progress] is 0.
 * @param to Depth to render shadows for when [progress] is 1.
 * @param shape [Shape] of the shadows
 * @param progress progress of the animation between [from] and [to], from 0 to 1. Values may go
 *   outside these bounds for overshoot / undershoot.
 */
// TODO: can be simplified with style API in the future
internal fun Modifier.depth(
    from: Depth?,
    to: Depth?,
    shape: Shape,
    progress: () -> Float,
): Modifier {
    // dropShadow draws the shadow, and then the content on top. So in order to get layer2 to
    // render on top of layer1, we draw layer1 first - this means that layer1's dropShadow will
    // draw its shadow, and then its content on top (since the 'content' includes children
    // modifiers, this includes layer2's shadow).
    return this.dropShadow(shape) {
            val shadow = lerp(from?.layer1, to?.layer1, progress())
            resetProperties()
            if (shadow != null) {
                updateFrom(shadow)
            }
        }
        .dropShadow(shape) {
            val shadow = lerp(from?.layer2, to?.layer2, progress())
            resetProperties()
            if (shadow != null) {
                updateFrom(shadow)
            }
        }
}

// TODO: b/434990398 - remove the following and use public APIs if / when added

private fun ShadowScope.updateFrom(shadow: Shadow) {
    this.radius = shadow.radius.toPx()
    this.spread = shadow.spread.toPx()
    this.offset = Offset(shadow.offset.x.toPx(), shadow.offset.y.toPx())
    this.color = shadow.color
    this.brush = shadow.brush
    this.alpha = shadow.alpha
    this.blendMode = shadow.blendMode
}

private fun ShadowScope.resetProperties() {
    this.radius = 0f
    this.spread = 0f
    this.offset = Offset.Zero
    this.color = Color.Black
    this.brush = null
    this.alpha = 1f
    this.blendMode = BlendMode.SrcOver
}
