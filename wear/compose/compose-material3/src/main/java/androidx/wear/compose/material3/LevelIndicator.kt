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

import androidx.annotation.FloatRange
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.tokens.ColorSchemeKeyTokens
import kotlin.math.sin

/**
 * Creates a [LevelIndicator] for screens that that control a setting such as volume with either
 * rotating side button, rotating bezel.
 *
 * Example of [LevelIndicator]:
 *
 * @sample androidx.wear.compose.material3.samples.LevelIndicatorSample
 * @param value Value of the indicator as a fraction in the range [0,1]. Values outside of the range
 *   [0,1] will be coerced.
 * @param modifier Modifier to be applied to the component
 * @param enabled Controls the enabled state of [LevelIndicator] - when false, disabled colors will
 *   be used.
 * @param colors [LevelIndicatorColors] that will be used to resolve the indicator and track colors
 *   for this [LevelIndicator] in different states
 * @param strokeWidth The stroke width for the indicator and track strokes
 * @param sweepAngle The angle covered by the curved LevelIndicator, in degrees
 * @param reverseDirection Reverses direction of PositionIndicator if true
 */
@Composable
public fun LevelIndicator(
    value: () -> Float,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: LevelIndicatorColors = LevelIndicatorDefaults.colors(),
    strokeWidth: Dp = LevelIndicatorDefaults.StrokeWidth,
    @FloatRange(from = 0.0, to = 360.0) sweepAngle: Float = LevelIndicatorDefaults.SweepAngle,
    reverseDirection: Boolean = false,
) {
    val updatedValue by rememberUpdatedState(value)
    val screenWidthDp = LocalConfiguration.current.screenWidthDp
    val paddingHorizontal = LevelIndicatorDefaults.edgePadding
    val radius = screenWidthDp / 2 - paddingHorizontal.value - strokeWidth.value / 2
    // Calculate indicator height based on a triangle of the top half of the sweep angle
    val indicatorHeight = 2f * sin((0.5f * sweepAngle).toRadians()) * radius

    IndicatorImpl(
        state = FractionPositionStateAdapter { updatedValue().coerceIn(0f, 1f) },
        indicatorHeight = indicatorHeight.dp,
        indicatorWidth = strokeWidth,
        paddingHorizontal = paddingHorizontal,
        background = colors.trackColor(enabled),
        color = colors.indicatorColor(enabled),
        modifier = modifier,
        reverseDirection = reverseDirection,
        rsbSide = false,
    )
}

/**
 * Creates a [StepperLevelIndicator] for screens that that control a setting, such as volume, with a
 * [Stepper].
 *
 * Example of [LevelIndicator] with a [Stepper]:
 *
 * @sample androidx.wear.compose.material3.samples.StepperSample
 * @param value Value of the indicator in the [valueRange].
 * @param modifier Modifier to be applied to the component
 * @param valueRange range of values that [value] can take
 * @param enabled Controls the enabled state of [LevelIndicator] - when false, disabled colors will
 *   be used.
 * @param colors [LevelIndicatorColors] that will be used to resolve the indicator and track colors
 *   for this [LevelIndicator] in different states
 * @param strokeWidth The stroke width for the indicator and track strokes
 * @param sweepAngle The angle covered by the curved LevelIndicator, in degrees
 * @param reverseDirection Reverses direction of PositionIndicator if true
 */
@Composable
public fun StepperLevelIndicator(
    value: () -> Float,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    enabled: Boolean = true,
    colors: LevelIndicatorColors = LevelIndicatorDefaults.colors(),
    strokeWidth: Dp = LevelIndicatorDefaults.StrokeWidth,
    @FloatRange(from = 0.0, to = 360.0) sweepAngle: Float = LevelIndicatorDefaults.SweepAngle,
    reverseDirection: Boolean = false,
): Unit =
    LevelIndicator(
        value = { (value() - valueRange.start) / (valueRange.endInclusive - valueRange.start) },
        modifier = modifier,
        enabled = enabled,
        colors = colors,
        strokeWidth = strokeWidth,
        sweepAngle = sweepAngle,
        reverseDirection = reverseDirection,
    )

/**
 * Creates a [StepperLevelIndicator] for screens that that control a setting, such as volume, with a
 * [Stepper].
 *
 * Example of [LevelIndicator] with a [Stepper] working on an [IntProgression]:
 *
 * @sample androidx.wear.compose.material3.samples.StepperWithIntegerSample
 * @param value Current value of the Stepper. If outside of [valueProgression] provided, value will
 *   be coerced to this range.
 * @param modifier Modifier to be applied to the component
 * @param valueProgression Progression of values that [StepperLevelIndicator] value can take.
 *   Consists of rangeStart, rangeEnd and step. Range will be equally divided by step size.
 * @param enabled Controls the enabled state of [LevelIndicator] - when false, disabled colors will
 *   be used.
 * @param colors [LevelIndicatorColors] that will be used to resolve the indicator and track colors
 *   for this [LevelIndicator] in different states
 * @param strokeWidth The stroke width for the indicator and track strokes
 * @param sweepAngle The angle covered by the curved LevelIndicator, in degrees
 * @param reverseDirection Reverses direction of PositionIndicator if true
 */
@Composable
public fun StepperLevelIndicator(
    value: () -> Int,
    valueProgression: IntProgression,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: LevelIndicatorColors = LevelIndicatorDefaults.colors(),
    strokeWidth: Dp = LevelIndicatorDefaults.StrokeWidth,
    @FloatRange(from = 0.0, to = 360.0) sweepAngle: Float = LevelIndicatorDefaults.SweepAngle,
    reverseDirection: Boolean = false,
): Unit =
    LevelIndicator(
        value = {
            (value() - valueProgression.first) /
                (valueProgression.last - valueProgression.first).toFloat()
        },
        modifier = modifier,
        enabled = enabled,
        colors = colors,
        strokeWidth = strokeWidth,
        sweepAngle = sweepAngle,
        reverseDirection = reverseDirection,
    )

/** Contains the default values used for [LevelIndicator]. */
public object LevelIndicatorDefaults {
    /**
     * Creates a [LevelIndicatorColors] that represents the default colors used in a
     * [LevelIndicator].
     */
    @Composable
    public fun colors(): LevelIndicatorColors =
        MaterialTheme.colorScheme.defaultLevelIndicatorColors

    /**
     * Creates a [LevelIndicatorColors] with modified colors used in [LevelIndicator].
     *
     * @param indicatorColor The indicator color.
     * @param trackColor The track color.
     * @param disabledIndicatorColor The disabled indicator color.
     * @param disabledTrackColor The disabled track color.
     */
    @Composable
    public fun colors(
        indicatorColor: Color = Color.Unspecified,
        trackColor: Color = Color.Unspecified,
        disabledIndicatorColor: Color = Color.Unspecified,
        disabledTrackColor: Color = Color.Unspecified,
    ): LevelIndicatorColors =
        MaterialTheme.colorScheme.defaultLevelIndicatorColors.copy(
            indicatorColor = indicatorColor,
            trackColor = trackColor,
            disabledIndicatorColor = disabledIndicatorColor,
            disabledTrackColor = disabledTrackColor,
        )

    /**
     * The sweep angle for the curved [LevelIndicator], measured up to the centers of the stroke
     * caps. The default value of 72 degrees equates to 20% of the circumference, i.e. 360/5.
     */
    public val SweepAngle: Float = 72f

    /** The default stroke width for the indicator and track strokes */
    public val StrokeWidth: Dp = 6.dp

    internal val edgePadding = PaddingDefaults.edgePadding

    private val ColorScheme.defaultLevelIndicatorColors: LevelIndicatorColors
        get() {
            return defaultLevelIndicatorColorsCached
                ?: LevelIndicatorColors(
                        indicatorColor = fromToken(ColorSchemeKeyTokens.SecondaryDim),
                        trackColor = fromToken(ColorSchemeKeyTokens.SurfaceContainer),
                        disabledIndicatorColor =
                            fromToken(ColorSchemeKeyTokens.OnSurface)
                                .toDisabledColor(disabledAlpha = DisabledContentAlpha),
                        disabledTrackColor =
                            fromToken(ColorSchemeKeyTokens.OnSurface)
                                .toDisabledColor(disabledAlpha = DisabledContainerAlpha),
                    )
                    .also { defaultLevelIndicatorColorsCached = it }
        }
}

/**
 * Represents the indicator and track colors used in [LevelIndicator].
 *
 * @param indicatorColor Color used to draw the indicator of [LevelIndicator].
 * @param trackColor Color used to draw the track of [LevelIndicator].
 * @param disabledIndicatorColor Color used to draw the indicator of [LevelIndicator] when it is not
 *   enabled.
 * @param disabledTrackColor Color used to draw the track of [LevelIndicator] when it is not
 *   enabled.
 */
public class LevelIndicatorColors(
    public val indicatorColor: Color,
    public val trackColor: Color,
    public val disabledIndicatorColor: Color,
    public val disabledTrackColor: Color,
) {
    /**
     * Returns a copy of this LevelIndicatorColors optionally overriding some of the values.
     *
     * @param indicatorColor Color used to draw the indicator of [LevelIndicator].
     * @param trackColor Color used to draw the track of [LevelIndicator].
     * @param disabledIndicatorColor Color used to draw the indicator of [LevelIndicator] when it is
     *   not enabled.
     * @param disabledTrackColor Color used to draw the track of [LevelIndicator] when it is not
     *   enabled.
     */
    public fun copy(
        indicatorColor: Color = this.indicatorColor,
        trackColor: Color = this.trackColor,
        disabledIndicatorColor: Color = this.disabledIndicatorColor,
        disabledTrackColor: Color = this.disabledTrackColor,
    ): LevelIndicatorColors =
        LevelIndicatorColors(
            indicatorColor = indicatorColor.takeOrElse { this.indicatorColor },
            trackColor = trackColor.takeOrElse { this.trackColor },
            disabledIndicatorColor =
                disabledIndicatorColor.takeOrElse { this.disabledIndicatorColor },
            disabledTrackColor = disabledTrackColor.takeOrElse { this.disabledTrackColor },
        )

    /**
     * Represents the indicator color, depending on [enabled].
     *
     * @param enabled whether the component is enabled.
     */
    internal fun indicatorColor(enabled: Boolean): Color {
        return if (enabled) indicatorColor else disabledIndicatorColor
    }

    /**
     * Represents the track color, depending on [enabled].
     *
     * @param enabled whether the component is enabled.
     */
    internal fun trackColor(enabled: Boolean): Color {
        return if (enabled) trackColor else disabledTrackColor
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || other !is LevelIndicatorColors) return false

        if (indicatorColor != other.indicatorColor) return false
        if (trackColor != other.trackColor) return false
        if (disabledIndicatorColor != other.disabledIndicatorColor) return false
        if (disabledTrackColor != other.disabledTrackColor) return false

        return true
    }

    override fun hashCode(): Int {
        var result = indicatorColor.hashCode()
        result = 31 * result + trackColor.hashCode()
        result = 31 * result + disabledIndicatorColor.hashCode()
        result = 31 * result + disabledTrackColor.hashCode()
        return result
    }
}

/**
 * An implementation of [IndicatorState] to display the amount and position of a component
 * implementing the [LevelIndicator].
 *
 * @param valueFraction the value fraction to adapt to a ScrollIndicatorState
 * @VisibleForTesting
 */
internal class FractionPositionStateAdapter(private val valueFraction: () -> Float) :
    IndicatorState {

    override val positionFraction = 1f // LevelIndicator always starts at the bottom

    override val sizeFraction: Float
        get() = valueFraction()

    override fun equals(other: Any?): Boolean {
        // Compare lambdas with referential equality
        return (other as? FractionPositionStateAdapter)?.valueFraction === valueFraction
    }

    override fun hashCode(): Int = valueFraction.hashCode()
}
