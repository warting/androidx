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

package androidx.wear.compose.material3

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.keyframes
import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.lerp
import androidx.wear.compose.material3.tokens.ColorSchemeKeyTokens
import androidx.wear.compose.material3.tokens.MotionTokens
import androidx.wear.compose.materialcore.toRadians
import kotlin.math.absoluteValue
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.round
import kotlin.math.sin

/** Contains defaults for Progress Indicators. */
public object ProgressIndicatorDefaults {
    /** Creates a [ProgressIndicatorColors] with the default colors. */
    @Composable
    public fun colors(): ProgressIndicatorColors =
        MaterialTheme.colorScheme.defaultProgressIndicatorColors

    /**
     * Creates a [ProgressIndicatorColors] with modified colors.
     *
     * @param indicatorColor The indicator color.
     * @param trackColor The track color.
     * @param overflowTrackColor The overflow track color.
     * @param disabledIndicatorColor The disabled indicator color.
     * @param disabledTrackColor The disabled track color.
     * @param disabledOverflowTrackColor The disabled overflow track color.
     */
    @Composable
    public fun colors(
        indicatorColor: Color = Color.Unspecified,
        trackColor: Color = Color.Unspecified,
        overflowTrackColor: Color = Color.Unspecified,
        disabledIndicatorColor: Color = Color.Unspecified,
        disabledTrackColor: Color = Color.Unspecified,
        disabledOverflowTrackColor: Color = Color.Unspecified,
    ): ProgressIndicatorColors =
        MaterialTheme.colorScheme.defaultProgressIndicatorColors.copy(
            indicatorColor = indicatorColor,
            trackColor = trackColor,
            overflowTrackColor = overflowTrackColor,
            disabledIndicatorColor = disabledIndicatorColor,
            disabledTrackColor = disabledTrackColor,
            disabledOverflowTrackColor = disabledOverflowTrackColor,
        )

    /**
     * Creates a [ProgressIndicatorColors] with modified brushes.
     *
     * @param indicatorBrush [Brush] used to draw indicator.
     * @param trackBrush [Brush] used to draw track.
     * @param overflowTrackBrush [Brush] used to draw track for progress overflow.
     * @param disabledIndicatorBrush [Brush] used to draw the indicator if the progress is disabled.
     * @param disabledTrackBrush [Brush] used to draw the track if the progress is disabled.
     * @param disabledOverflowTrackBrush [Brush] used to draw the overflow track if the progress is
     *   disabled.
     */
    @Composable
    public fun colors(
        indicatorBrush: Brush? = null,
        trackBrush: Brush? = null,
        overflowTrackBrush: Brush? = null,
        disabledIndicatorBrush: Brush? = null,
        disabledTrackBrush: Brush? = null,
        disabledOverflowTrackBrush: Brush? = null,
    ): ProgressIndicatorColors =
        MaterialTheme.colorScheme.defaultProgressIndicatorColors.copy(
            indicatorBrush = indicatorBrush,
            trackBrush = trackBrush,
            overflowTrackBrush = overflowTrackBrush,
            disabledIndicatorBrush = disabledIndicatorBrush,
            disabledTrackBrush = disabledTrackBrush,
            disabledOverflowTrackBrush = disabledOverflowTrackBrush
        )

    // TODO(b/364538891): add color and alpha tokens for ProgressIndicator
    private const val OverflowTrackColorAlpha = 0.6f

    private val ColorScheme.defaultProgressIndicatorColors: ProgressIndicatorColors
        get() {
            return defaultProgressIndicatorColorsCached
                ?: ProgressIndicatorColors(
                        indicatorBrush = SolidColor(fromToken(ColorSchemeKeyTokens.Primary)),
                        trackBrush = SolidColor(fromToken(ColorSchemeKeyTokens.SurfaceContainer)),
                        overflowTrackBrush =
                            SolidColor(
                                fromToken(ColorSchemeKeyTokens.Primary)
                                    .copy(alpha = OverflowTrackColorAlpha)
                            ),
                        disabledIndicatorBrush =
                            SolidColor(
                                fromToken(ColorSchemeKeyTokens.OnSurface)
                                    .toDisabledColor(disabledAlpha = DisabledContentAlpha)
                            ),
                        disabledTrackBrush =
                            SolidColor(
                                fromToken(ColorSchemeKeyTokens.OnSurface)
                                    .toDisabledColor(disabledAlpha = DisabledContainerAlpha)
                            ),
                        disabledOverflowTrackBrush =
                            SolidColor(
                                fromToken(ColorSchemeKeyTokens.Primary)
                                    .copy(alpha = OverflowTrackColorAlpha)
                                    .toDisabledColor(disabledAlpha = DisabledContainerAlpha)
                            )
                    )
                    .also { defaultProgressIndicatorColorsCached = it }
        }
}

/**
 * Represents the indicator and track colors used in progress indicator.
 *
 * @param indicatorBrush [Brush] used to draw the indicator of progress indicator.
 * @param trackBrush [Brush] used to draw the track of progress indicator.
 * @param overflowTrackBrush [Brush] used to draw the track for progress overflow (>100%).
 * @param disabledIndicatorBrush [Brush] used to draw the indicator if the component is disabled.
 * @param disabledTrackBrush [Brush] used to draw the track if the component is disabled.
 * @param disabledOverflowTrackBrush [Brush] used to draw the overflow track if the component is
 *   disabled.
 */
public class ProgressIndicatorColors(
    public val indicatorBrush: Brush,
    public val trackBrush: Brush,
    public val overflowTrackBrush: Brush,
    public val disabledIndicatorBrush: Brush,
    public val disabledTrackBrush: Brush,
    public val disabledOverflowTrackBrush: Brush,
) {
    /**
     * Returns a copy of this ProgressIndicatorColors optionally overriding some of the values.
     *
     * @param indicatorColor The indicator color.
     * @param trackColor The track color.
     * @param overflowTrackColor The overflow track color.
     * @param disabledIndicatorColor The disabled indicator color.
     * @param disabledTrackColor The disabled track color.
     * @param disabledOverflowTrackColor The disabled overflow track color.
     */
    public fun copy(
        indicatorColor: Color = Color.Unspecified,
        trackColor: Color = Color.Unspecified,
        overflowTrackColor: Color = Color.Unspecified,
        disabledIndicatorColor: Color = Color.Unspecified,
        disabledTrackColor: Color = Color.Unspecified,
        disabledOverflowTrackColor: Color = Color.Unspecified,
    ): ProgressIndicatorColors =
        ProgressIndicatorColors(
            indicatorBrush =
                if (indicatorColor.isSpecified) SolidColor(indicatorColor) else indicatorBrush,
            trackBrush = if (trackColor.isSpecified) SolidColor(trackColor) else trackBrush,
            overflowTrackBrush =
                if (overflowTrackColor.isSpecified) SolidColor(overflowTrackColor)
                else overflowTrackBrush,
            disabledIndicatorBrush =
                if (disabledIndicatorColor.isSpecified) SolidColor(disabledIndicatorColor)
                else disabledIndicatorBrush,
            disabledTrackBrush =
                if (disabledTrackColor.isSpecified) SolidColor(disabledTrackColor)
                else disabledTrackBrush,
            disabledOverflowTrackBrush =
                if (disabledOverflowTrackColor.isSpecified) SolidColor(disabledOverflowTrackColor)
                else disabledOverflowTrackBrush,
        )

    /**
     * Returns a copy of this ProgressIndicatorColors optionally overriding some of the values.
     *
     * @param indicatorBrush [Brush] used to draw the indicator of progress indicator.
     * @param trackBrush [Brush] used to draw the track of progress indicator.
     * @param overflowTrackBrush [Brush] used to draw the track for progress overflow.
     * @param disabledIndicatorBrush [Brush] used to draw the indicator if the component is
     *   disabled.
     * @param disabledTrackBrush [Brush] used to draw the track if the component is disabled.
     * @param disabledOverflowTrackBrush [Brush] used to draw the overflow track if the component is
     *   disabled.
     */
    public fun copy(
        indicatorBrush: Brush? = null,
        trackBrush: Brush? = null,
        overflowTrackBrush: Brush? = null,
        disabledIndicatorBrush: Brush? = null,
        disabledTrackBrush: Brush? = null,
        disabledOverflowTrackBrush: Brush? = null,
    ): ProgressIndicatorColors =
        ProgressIndicatorColors(
            indicatorBrush = indicatorBrush ?: this.indicatorBrush,
            trackBrush = trackBrush ?: this.trackBrush,
            overflowTrackBrush = overflowTrackBrush ?: this.overflowTrackBrush,
            disabledIndicatorBrush = disabledIndicatorBrush ?: this.disabledIndicatorBrush,
            disabledTrackBrush = disabledTrackBrush ?: this.disabledTrackBrush,
            disabledOverflowTrackBrush =
                disabledOverflowTrackBrush ?: this.disabledOverflowTrackBrush,
        )

    /**
     * Represents the indicator color, depending on [enabled].
     *
     * @param enabled whether the component is enabled.
     */
    internal fun indicatorBrush(enabled: Boolean): Brush {
        return if (enabled) indicatorBrush else disabledIndicatorBrush
    }

    /**
     * Represents the track color, depending on [enabled].
     *
     * @param enabled whether the component is enabled.
     */
    internal fun trackBrush(enabled: Boolean): Brush {
        return if (enabled) trackBrush else disabledTrackBrush
    }

    /**
     * Represents the animated overflow track color. The result will be a combination of
     * [indicatorBrush] and [overflowTrackBrush] depending on the [fraction].
     *
     * @param enabled whether the component is enabled.
     * @param fraction the fraction of indicator to overflow color, should be between 0 and 1,
     *   inclusive.
     */
    internal fun overflowTrackBrush(enabled: Boolean, fraction: Float = 1f): Brush =
        if (enabled) {
            if (overflowTrackBrush is SolidColor && indicatorBrush is SolidColor && fraction < 1f) {
                SolidColor(lerp(indicatorBrush.value, overflowTrackBrush.value, fraction))
            } else {
                overflowTrackBrush
            }
        } else {
            disabledOverflowTrackBrush
        }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || other !is ProgressIndicatorColors) return false

        if (indicatorBrush != other.indicatorBrush) return false
        if (trackBrush != other.trackBrush) return false
        if (overflowTrackBrush != other.overflowTrackBrush) return false
        if (disabledIndicatorBrush != other.disabledIndicatorBrush) return false
        if (disabledTrackBrush != other.disabledTrackBrush) return false
        if (disabledOverflowTrackBrush != other.disabledOverflowTrackBrush) return false

        return true
    }

    override fun hashCode(): Int {
        var result = indicatorBrush.hashCode()
        result = 31 * result + trackBrush.hashCode()
        result = 31 * result + overflowTrackBrush.hashCode()
        result = 31 * result + disabledIndicatorBrush.hashCode()
        result = 31 * result + disabledTrackBrush.hashCode()
        result = 31 * result + disabledOverflowTrackBrush.hashCode()
        return result
    }
}

internal fun brushWithAlpha(brush: Brush, alpha: Float): Brush {
    return if (brush is SolidColor && alpha < 1f) {
        SolidColor(brush.value.copy(alpha = brush.value.alpha * alpha))
    } else {
        brush
    }
}

/**
 * Draws an arc for indicator segment leaving half of the `gapSweep` before each visual end.
 *
 * If indicator gets too small, the circle that proportionally scales down is drawn instead.
 */
internal fun DrawScope.drawIndicatorSegment(
    startAngle: Float,
    sweep: Float,
    gapSweep: Float,
    brush: Brush,
    strokeWidth: Float,
    strokePadding: Float = 0f,
) {
    if (sweep <= gapSweep) {
        // Draw a small indicator.
        val angle = (startAngle + sweep / 2f).toRadians()
        val radius = size.minDimension / 2 - strokeWidth / 2
        val circleRadius = ((strokeWidth + strokePadding) / 2) * sweep / gapSweep
        val alpha = (circleRadius / strokeWidth * 2f).coerceAtMost(1f)
        drawCircle(
            brushWithAlpha(brush, alpha),
            circleRadius,
            center =
                Offset(
                    radius * cos(angle) + size.minDimension / 2,
                    radius * sin(angle) + size.minDimension / 2
                )
        )
    } else {
        // To draw this circle we need a rect with edges that line up with the midpoint of the
        // stroke.
        // To do this we need to remove half the stroke width from the total diameter for both
        // sides.
        val diameter = min(size.width, size.height)
        val diameterOffset = strokeWidth / 2
        val arcDimen = diameter - 2 * diameterOffset
        val stroke = Stroke(width = strokeWidth + strokePadding, cap = StrokeCap.Round)

        drawArc(
            brush = brush,
            startAngle = startAngle + gapSweep / 2,
            sweepAngle = sweep - gapSweep,
            useCenter = false,
            topLeft =
                Offset(
                    diameterOffset + (size.width - diameter) / 2,
                    diameterOffset + (size.height - diameter) / 2
                ),
            size = Size(arcDimen, arcDimen),
            style = stroke
        )
    }
}

/**
 * Wrap a [Float] progress value to [0.0..1.0] range.
 *
 * If overflow is enabled, truncate overflow values larger than 1.0 to only the fractional part.
 * Integer values larger than 1.0 always return 1.0 (full progress) and negative values are coerced
 * to 0.0. For example: 1.2 will be return 0.2, and 2.0 will return 1.0. If overflow is disabled,
 * simply coerce all values to [0.0..1.0] range. For example, 1.2 and 2.0 will both return 1.0.
 *
 * @param progress The progress value to be wrapped to [0.0..1.0] range.
 * @param allowProgressOverflow If overflow is allowed.
 */
internal fun wrapProgress(progress: Float, allowProgressOverflow: Boolean): Float {
    if (!allowProgressOverflow) return progress.coerceIn(0f, 1f)
    if (progress <= 0.0f) return 0.0f
    if (progress <= 1.0f) return progress

    val fraction = progress % 1.0f
    // Round to 5 decimals to avoid floating point errors.
    val roundedFraction = round(fraction * 100000f) / 100000f
    return if (roundedFraction == 0.0f) 1.0f else roundedFraction
}

internal fun DrawScope.drawCircularIndicator(
    startAngle: Float,
    sweep: Float,
    brush: Brush,
    stroke: Stroke
) {
    // To draw this circle we need a rect with edges that line up with the midpoint of the stroke.
    // To do this we need to remove half the stroke width from the total diameter for both sides.
    val diameter = min(size.width, size.height)
    val diameterOffset = stroke.width / 2
    val arcDimen = diameter - 2 * diameterOffset
    drawArc(
        brush = brush,
        startAngle = startAngle,
        sweepAngle = sweep,
        useCenter = false,
        topLeft =
            Offset(
                diameterOffset + (size.width - diameter) / 2,
                diameterOffset + (size.height - diameter) / 2
            ),
        size = Size(arcDimen, arcDimen),
        style = stroke
    )
}

internal fun DrawScope.drawIndicatorArc(
    startAngle: Float,
    sweep: Float,
    brush: Brush,
    stroke: Stroke,
    gapSweep: Float
) {
    if (sweep.absoluteValue < gapSweep) {
        // Draw a small circle indicator.
        val angle = (startAngle + sweep / 2f).toRadians()
        val radius = size.width / 2 - stroke.width / 2
        val circleRadius = (stroke.width / 2) * sweep.absoluteValue / gapSweep
        val alpha = (circleRadius / stroke.width * 2f).coerceAtMost(1f)
        drawCircle(
            brush = brushWithAlpha(brush, alpha),
            radius = circleRadius,
            center =
                Offset(radius * cos(angle) + size.width / 2, radius * sin(angle) + size.width / 2)
        )
    } else {
        drawCircularIndicator(
            startAngle = if (sweep > 0) startAngle + gapSweep / 2 else startAngle - gapSweep / 2,
            sweep = if (sweep > 0) sweep - gapSweep else sweep + gapSweep,
            brush = brush,
            stroke = stroke
        )
    }
}

internal fun Float.isFullInt(): Boolean = (round(this) == this)

internal fun Float.equalsWithTolerance(number: Float, tolerance: Float = 0.1f) =
    (this - number).absoluteValue < tolerance

/**
 * Animation spec for over 100% Progress animations.
 *
 * Consists of 3 phases:
 * 1) Intro: A time-based animation that accelerates the progression from 0 degrees per second to
 *    the peak speed over a fixed duration using easing.
 * 2) Peak Speed: A rotation at a constant speed, defined in degrees per second, with a variable
 *    duration.
 * 3) Outro: A time-based animation that decelerates the progression from the peak speed back to 0
 *    degrees per second over a fixed duration using easing.
 */
internal fun createOverflowProgressAnimationSpec(
    newProgress: Float,
    oldProgress: Float
): AnimationSpec<Float> {
    val progressDiff = newProgress - oldProgress
    val peakSpeed = OverflowProgressMiddlePhaseSpeed

    // Calculate intro and outro progress distance from the area under the CubicBezier curve.
    val introProgressDistance =
        IntroCubicBezierCurveAreaFactor * peakSpeed * (OverflowProgressIntroPhaseDuration / 1000f)
    val outroProgressDistance =
        OutroCubicBezierCurveAreaFactor * peakSpeed * (OverflowProgressOutroPhaseDuration / 1000f)
    val introProgress = if (progressDiff > 0) introProgressDistance else -introProgressDistance
    val outroProgress = if (progressDiff > 0) outroProgressDistance else -outroProgressDistance
    val midProgress = progressDiff - introProgress - outroProgress
    // Calculate the duration of the middle phase by dividing distance(progress) with speed.
    val midDuration = (midProgress.absoluteValue / peakSpeed * 1000).toInt()

    return keyframes {
        durationMillis =
            OverflowProgressIntroPhaseDuration + OverflowProgressOutroPhaseDuration + midDuration
        // Intro phase
        oldProgress at 0 using OverflowProgressIntroPhaseEasing
        // Middle phase
        oldProgress + introProgress at OverflowProgressIntroPhaseDuration using LinearEasing
        // Outro phase
        oldProgress + introProgress + midProgress at
            OverflowProgressIntroPhaseDuration + midDuration using
            OverflowProgressOutroPhaseEasing
    }
}

// The determinate circular indicator progress animation constants for progress over 100%
internal val OverflowProgressIntroPhaseDuration = MotionTokens.DurationMedium1 // 250ms
internal val OverflowProgressIntroPhaseEasing = MotionTokens.EasingStandardAccelerate
internal val OverflowProgressOutroPhaseDuration = MotionTokens.DurationMedium4 // 400ms
internal val OverflowProgressOutroPhaseEasing = MotionTokens.EasingStandardDecelerate
internal val OverflowProgressMiddlePhaseSpeed = 2f // Full progress circle rotations per second

/**
 * The area under the Bezier curve for [MotionTokens.EasingStandardAccelerate], see
 * https://github.com/Pomax/BezierInfo-2/issues/238 for how this is calculated.
 */
internal const val IntroCubicBezierCurveAreaFactor = 0.41f
/** The area under the Bezier curve for [MotionTokens.EasingStandardDecelerate]. */
internal const val OutroCubicBezierCurveAreaFactor = 0.2f

/** Progress animation spec for determinate [CircularProgressIndicator] */
internal val determinateCircularProgressAnimationSpec: AnimationSpec<Float>
    @Composable get() = MaterialTheme.motionScheme.slowEffectsSpec()

/** Progress overflow color animation spec for determinate [CircularProgressIndicator] */
internal val progressOverflowColorAnimationSpec: AnimationSpec<Float>
    @Composable get() = MaterialTheme.motionScheme.fastEffectsSpec()
