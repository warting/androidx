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

import android.graphics.RuntimeShader
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.FocusInteraction
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.intellij.lang.annotations.Language

/**
 * A surface is a fundamental building block in Glimmer. A surface represents a distinct visual area
 * or 'physical' boundary for components such as buttons and cards. A surface is responsible for:
 * 1) Clipping: a surface clips its children to the shape specified by [shape]
 * 2) Border: a surface draws an inner [border] to emphasize the boundary of the component.
 * 3) Background: a surface has a background color of [color].
 * 4) Content color: a surface provides a [contentColor] for text and icons inside the surface. By
 *    default this is calculated from the provided background color.
 * 5) Interaction states: when focused, a surface displays draws a wider border with a focused
 *    highlight on top. When pressed, a surface draws a pressed overlay. This happens for
 *    interactions emitted from [interactionSource], whether this surface is [focusable] or not.
 *
 * This surface is focusable by default - set [focusable] to false for un-interactive / decorative
 * surfaces. For handling clicks, use the other [surface] overload with an `onClick` parameter.
 *
 * Simple usage:
 *
 * @sample androidx.xr.glimmer.samples.SurfaceSample
 *
 * For custom gesture handling, add the gesture modifier after this [surface], and provide a shared
 * [MutableInteractionSource] to enable this surface to handle focus / press states. You should also
 * pass `false` for [focusable] if that modifier already includes a focus target by default. For
 * example, to create a toggleable surface:
 *
 * @sample androidx.xr.glimmer.samples.ToggleableSurfaceSample
 * @param focusable whether this surface is focusable, true by default. Most surfaces should be
 *   focusable to allow navigation between surfaces in a screen. Unfocusable surfaces may be used
 *   for decorative only elements, such as surfaces used in a compound component with a separate
 *   focusable area.
 * @param shape the [Shape] used to clip this surface, and also used to draw the background and
 *   border
 * @param color the background [Color] for this surface
 * @param contentColor the [Color] for content inside this surface
 * @param border an optional inner border for this surface
 * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
 *   emitting [Interaction]s for this surface. Note that if `null` is provided, interactions will
 *   still happen internally.
 */
@Composable
public fun Modifier.surface(
    focusable: Boolean = true,
    shape: Shape = GlimmerTheme.shapes.medium,
    color: Color = GlimmerTheme.colors.surface,
    contentColor: Color = calculateContentColor(color),
    border: BorderStroke? = SurfaceDefaults.border(),
    interactionSource: MutableInteractionSource? = null,
): Modifier {
    val interactionSource = interactionSource ?: remember { MutableInteractionSource() }
    return this.clip(shape)
        .contentColorProvider(contentColor)
        .then(SurfaceNodeElement(shape, border, interactionSource))
        .background(color = color, shape = shape)
        .focusable(enabled = focusable, interactionSource = interactionSource)
}

/**
 * A surface is a fundamental building block in Glimmer. A surface represents a distinct visual area
 * or 'physical' boundary for components such as buttons and cards. A surface is responsible for:
 * 1) Clipping: a surface clips its children to the shape specified by [shape]
 * 2) Border: a surface draws an inner [border] to emphasize the boundary of the component. When
 *    focused, a surface draws a wider border with a focused highlight on top to indicate the focus
 *    state.
 * 3) Background: a surface has a background color of [color].
 * 4) Content color: a surface provides a [contentColor] for text and icons inside the surface. By
 *    default this is calculated from the provided background color.
 * 5) Interaction states: when focused, a surface displays draws a wider border with a focused
 *    highlight on top. When pressed, a surface draws a pressed overlay. This happens for
 *    interactions emitted from [interactionSource], whether this surface is [enabled] or not.
 *
 * This surface is focusable and handles clicks. For non-clickable surfaces, use the other overload
 * of [surface] instead. For surfaces with custom gesture handling, refer to the sample and guidance
 * on the other overload of [surface].
 *
 * @sample androidx.xr.glimmer.samples.ClickableSurfaceSample
 * @param enabled whether this surface is enabled, true by default. When false, this surface will
 *   not respond to user input, and will not be focusable.
 * @param shape the [Shape] used to clip this surface, and also used to draw the background and
 *   border
 * @param color the background [Color] for this surface
 * @param contentColor the [Color] for content inside this surface
 * @param border an optional inner border for this surface
 * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
 *   emitting [Interaction]s for this surface. Note that if `null` is provided, interactions will
 *   still happen internally.
 * @param onClick callback invoked when this surface is clicked
 */
@Composable
public fun Modifier.surface(
    enabled: Boolean = true,
    shape: Shape = GlimmerTheme.shapes.medium,
    color: Color = GlimmerTheme.colors.surface,
    contentColor: Color = calculateContentColor(color),
    border: BorderStroke? = SurfaceDefaults.border(),
    interactionSource: MutableInteractionSource? = null,
    onClick: () -> Unit,
): Modifier {
    val interactionSource = interactionSource ?: remember { MutableInteractionSource() }
    return this.clip(shape)
        .contentColorProvider(contentColor)
        .then(SurfaceNodeElement(shape, border, interactionSource))
        .background(color = color, shape = shape)
        // TODO: b/423573184 align on disabled behavior / state
        .clickable(enabled = enabled, interactionSource = interactionSource, onClick = onClick)
}

/** Default values used for [surface]. */
public object SurfaceDefaults {
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

/**
 * Surface node responsible for drawing the border, focused border and highlight, and pressed
 * overlay.
 */
private class SurfaceNodeElement(
    private val shape: Shape,
    private val border: BorderStroke?,
    private val interactionSource: InteractionSource?,
) : ModifierNodeElement<SurfaceNode>() {
    override fun create(): SurfaceNode = SurfaceNode(shape, border, interactionSource)

    override fun update(node: SurfaceNode) = node.update(shape, border, interactionSource)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SurfaceNodeElement) return false

        if (shape != other.shape) return false
        if (border != other.border) return false
        if (interactionSource != other.interactionSource) return false

        return true
    }

    override fun hashCode(): Int {
        var result = shape.hashCode()
        result = 31 * result + (border?.hashCode() ?: 0)
        result = 31 * result + (interactionSource?.hashCode() ?: 0)
        return result
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "surface"
        properties["shape"] = shape
        properties["border"] = border
        properties["interactionSource"] = interactionSource
    }
}

private class SurfaceNode(
    private var shape: Shape,
    private var border: BorderStroke?,
    private var interactionSource: InteractionSource?,
) : DrawModifierNode, Modifier.Node() {

    override val shouldAutoInvalidate = false

    // Cache borders and highlight for unfocused and focused states. This means we
    // can avoid recreating these for a given surface, if the border and shape never
    // change. Changing between unfocused and focus states only requires a draw
    // invalidation, as the borders are already cached.

    // Unfocused border
    private val unfocusedBorderLogic: BorderLogic = BorderLogic()
    private val unfocusedBorderWidth: () -> Dp = { border?.width ?: 0.dp }
    // Focused border - this consists of two layers. A 'base' layer (which is the
    // unfocused border with a different size) and the highlight we draw on top of
    // this base layer. We need to increase the size of the underlying border to
    // make sure that the highlight area fully matches the underlying border, to
    // avoid inconsistent areas of coverage due to the transparency of the
    // highlight.
    private var focusedBorderLogic: BorderLogic? = null
    private var focusedHighlightBorderLogic: BorderLogic? = null
    private var focusedBorderWidth: (() -> Dp)? = null

    // Highlight shader / brush
    var shader: RuntimeShader? = null
    var shaderBrush: Brush? = null

    private var interactionCollectionJob: Job? = null

    // Enter / exit animation progress for the width and fade effect applied to the highlight
    private var _focusedHighlightProgress: Animatable<Float, AnimationVector1D>? = null
    private val focusedHighlightProgress
        get() = _focusedHighlightProgress?.value ?: 0f

    // Rotation progress applied to the highlight
    private var _focusedHighlightRotationProgress: Animatable<Float, AnimationVector1D>? = null
    private val focusedHighlightRotationProgress
        get() = _focusedHighlightRotationProgress?.value ?: 0f

    private var pressedOverlayAlpha: Animatable<Float, AnimationVector1D>? = null
    // Job that runs for a minimum duration to make sure quick presses are still visible
    private var minimumPressDuration: Job? = null
    private var pressReleaseAnimation: Job? = null

    fun update(shape: Shape, border: BorderStroke?, interactionSource: InteractionSource?) {
        if (this.shape != shape) {
            this.shape = shape
            invalidateDraw()
        }
        if (this.border != border) {
            this.border = border
            invalidateDraw()
        }
        if (this.interactionSource != interactionSource) {
            this.interactionSource = interactionSource
            observeInteractions()
        }
    }

    override fun onAttach() {
        observeInteractions()
    }

    var isFocused = false
        set(value) {
            if (field != value) {
                field = value
                if (value) {
                    startFocusAnimation()
                } else {
                    stopFocusAnimation()
                }
                // No need to invalidate the border cache - we build it ahead of time to account for
                // focus changes. Just invalidate draw so we can switch to drawing the correct
                // border.
                invalidateDraw()
            }
        }

    var isPressed = false
        set(value) {
            if (field != value) {
                field = value
                if (value) {
                    pressedOverlayAlpha = pressedOverlayAlpha ?: Animatable(0f)
                    pressReleaseAnimation?.cancel()
                    minimumPressDuration?.cancel()

                    minimumPressDuration =
                        coroutineScope.launch(start = CoroutineStart.UNDISPATCHED) {
                            delay(PressedOverlayMinimumDurationMillis)
                        }
                    coroutineScope.launch(start = CoroutineStart.UNDISPATCHED) {
                        pressedOverlayAlpha?.animateTo(
                            PressedOverlayAlpha,
                            PressedOverlayAnimationSpec,
                        )
                    }
                } else {
                    pressedOverlayAlpha?.let { progress ->
                        pressReleaseAnimation =
                            coroutineScope.launch(start = CoroutineStart.UNDISPATCHED) {
                                minimumPressDuration?.join()
                                progress.animateTo(0f, PressedOverlayAnimationSpec)
                            }
                    }
                }
                invalidateDraw()
            }
        }

    private fun observeInteractions() {
        interactionCollectionJob?.cancel()
        interactionCollectionJob = null
        isFocused = false
        isPressed = false
        interactionSource?.let { source ->
            interactionCollectionJob =
                coroutineScope.launch(start = CoroutineStart.UNDISPATCHED) {
                    var focusCount = 0
                    var pressCount = 0
                    source.interactions.collect { interaction ->
                        when (interaction) {
                            is FocusInteraction.Focus -> focusCount++
                            is FocusInteraction.Unfocus -> focusCount--
                            is PressInteraction.Press -> pressCount++
                            is PressInteraction.Release -> pressCount--
                            is PressInteraction.Cancel -> pressCount--
                        }
                        isFocused = focusCount > 0
                        isPressed = pressCount > 0
                    }
                }
        }
    }

    private fun startFocusAnimation() {
        _focusedHighlightProgress = _focusedHighlightProgress ?: Animatable(0f)
        _focusedHighlightRotationProgress = _focusedHighlightRotationProgress ?: Animatable(0f)
        coroutineScope.launch(start = CoroutineStart.UNDISPATCHED) {
            _focusedHighlightProgress?.snapTo(0f)
            _focusedHighlightProgress?.animateTo(
                targetValue = 1f,
                animationSpec = FocusedHighlightEnterAnimationSpec,
            )
        }
        coroutineScope.launch(start = CoroutineStart.UNDISPATCHED) {
            _focusedHighlightRotationProgress?.snapTo(0f)
            _focusedHighlightRotationProgress?.animateTo(
                targetValue = 1f,
                animationSpec = FocusedHighlightRotationAnimationSpec,
            )
        }
    }

    private fun stopFocusAnimation() {
        coroutineScope.launch(start = CoroutineStart.UNDISPATCHED) {
            _focusedHighlightProgress?.animateTo(
                targetValue = 0f,
                animationSpec = FocusedHighlightExitAnimationSpec,
            )
            if (isActive) {
                _focusedHighlightRotationProgress?.snapTo(0f)
            }
        }
    }

    override fun ContentDrawScope.draw() {
        val outline = shape.createOutline(size, layoutDirection, this)
        drawContent()
        val pressedOverlayColor = PressedOverlayColor.copy(alpha = pressedOverlayAlpha?.value ?: 0f)
        drawRect(color = pressedOverlayColor)
        if (border != null) {
            val progress = focusedHighlightProgress
            if (progress > 0f) {
                shader = shader ?: RuntimeShader(FocusedHighlightShader)
                shaderBrush = shaderBrush ?: ShaderBrush(shader!!)
                val rotationRadians = focusedHighlightRotationProgress * Math.TAU
                shader!!.setFloatUniform("iResolution", size.width, size.height)
                shader!!.setFloatUniform("iRotation", rotationRadians.toFloat())
                shader!!.setFloatUniform("iAlphaProgress", progress)
                focusedBorderLogic = focusedBorderLogic ?: BorderLogic()
                focusedHighlightBorderLogic = focusedHighlightBorderLogic ?: BorderLogic()
                focusedBorderWidth =
                    focusedBorderWidth
                        ?: {
                            val b = border
                            if (b != null) {
                                lerp(
                                    b.width,
                                    FocusedSurfaceBorderWidth,
                                    // Capture class property instead of function-local progress to
                                    // make sure this will read the animation state when the lambda
                                    // is invoked and not capture a stale variable
                                    focusedHighlightProgress,
                                )
                            } else {
                                0.dp
                            }
                        }
                focusedBorderLogic!!.drawBorder(this, focusedBorderWidth!!, border!!.brush, outline)

                focusedHighlightBorderLogic!!.drawBorder(
                    this,
                    focusedBorderWidth!!,
                    shaderBrush!!,
                    outline,
                )
            } else {
                unfocusedBorderLogic.drawBorder(this, unfocusedBorderWidth, border!!.brush, outline)
            }
        }
    }

    override fun onDetach() {
        _focusedHighlightProgress = null
        _focusedHighlightRotationProgress = null
        pressedOverlayAlpha = null
    }
}

/** Default border width for a [surface]. */
private val DefaultSurfaceBorderWidth = 2.dp

/** Focused border width for a [surface]. */
private val FocusedSurfaceBorderWidth = 5.dp

private val FocusedHighlightEnterAnimationSpec: AnimationSpec<Float> =
    tween(650, easing = FastOutSlowInEasing)

private val FocusedHighlightExitAnimationSpec: AnimationSpec<Float> =
    tween(300, easing = FastOutSlowInEasing)

private val FocusedHighlightRotationAnimationSpec: AnimationSpec<Float> =
    tween(durationMillis = 7000, easing = LinearOutSlowInEasing)

private val PressedOverlayColor = Color.White

private const val PressedOverlayAlpha = 0.16f

private val PressedOverlayAnimationSpec: AnimationSpec<Float> =
    spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessVeryLow)

private const val PressedOverlayMinimumDurationMillis = 300L

@Language(value = "AGSL")
private const val FocusedHighlightShader =
    """
/**
 * Rotating linear gradient shader, where rotation is controlled by iRotation uniform.
 * This is essentially the same as:
 * LinearGradientShader(
 *     colors = FocusedHighlightColors,
 *     colorStops = FocusedHighlightColorStops,
 *     from = Offset.Zero,
 *     to = Offset(size.width, size.height),
 * )
 * But allowing for efficient rotation, instead of needing to create a new shader / brush every
 * frame with new coordinates.
 */
// Width / height
uniform float2 iResolution;
// Rotation in radians. 0 radians means a horizontal gradient.
// Positive values will have the effect of rotating the gradient clockwise.
uniform float iRotation;
// Alpha animation progress from 0 to 1. This will be applied to the color stops so that each
// color stop will fade in.
uniform float iAlphaProgress;

half4 main(float2 fragCoord) {
    // Horizontal gradient
    half4 colors[4];
    colors[0] = half4(1.0, 1.0, 1.0, 0.8 * iAlphaProgress); // White with 80% alpha
    colors[1] = half4(1.0, 1.0, 1.0, 0.0); // Transparent
    colors[2] = half4(1.0, 1.0, 1.0, 0.0); // Transparent
    colors[3] = half4(1.0, 1.0, 1.0, 0.2 * iAlphaProgress); // White with 20% alpha

    // Stops for the horizontal gradient
    float stops[4];
    stops[0] = 0.0;
    stops[1] = 0.3;
    stops[2] = 0.66;
    stops[3] = 1.0;

    // Normalize
    half2 uv = fragCoord.xy / iResolution.xy;

    // Offset around a rotational center
    half2 rotationCenter = half2(0.5, 0.5);
    uv -= rotationCenter;

    // Rotate
    // We rotate in the opposite direction as we are rotating the coordinate we sample the gradient
    // from. To create the effect of a gradient 'moving' clockwise, we need to move the
    // coordinate in the opposite direction (counter-clockwise).
    float2x2 matrix = float2x2(cos(-iRotation),-sin(-iRotation),sin(-iRotation),cos(-iRotation));
    uv *= matrix;

    // Translate back into [0,1] space
    uv += rotationCenter;

    // Blend through stops using the x coordinate, since we have a horizontal gradient
    half4 color = mix(colors[0], colors[1], smoothstep(stops[0], stops[1], uv.x));
    color = mix(color, colors[2], smoothstep(stops[1], stops[2], uv.x));
    color = mix(color, colors[3], smoothstep(stops[2], stops[3], uv.x));

    return color;
}
"""
