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
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.AnimationVector
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.AnimationVector2D
import androidx.compose.animation.core.AnimationVector3D
import androidx.compose.animation.core.AnimationVector4D
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.TwoWayConverter
import androidx.compose.animation.core.VectorizedFiniteAnimationSpec
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import kotlinx.coroutines.delay

/**
 * Returns a new [FiniteAnimationSpec] that is a slower or faster version of this one.
 *
 * @param factor How much to speed or slow the animation. 0f -> runs forever, zero speed (not
 *   allowed) 0.5f -> half speed 1f -> current speed 2f -> double speed
 */
internal fun <T> FiniteAnimationSpec<T>.speedFactor(
    @FloatRange(from = 0.0, fromInclusive = false) factor: Float
): FiniteAnimationSpec<T> {
    require(factor > 0f) { "factor has to be positive. Was: $factor" }
    return when (this) {
        is SpringSpec -> SpringSpec(dampingRatio, stiffness * factor * factor, visibilityThreshold)
        else -> WrappedAnimationSpec(this, factor)
    }
}

/**
 * Returns a new [FiniteAnimationSpec] that is a faster version of this one.
 *
 * @param speedupPct How much to speed up the animation, as a percentage of the current speed. 0f
 *   being no change, 100f being double, speed and so on.
 */
internal fun <T> FiniteAnimationSpec<T>.faster(
    @FloatRange(from = 0.0) speedupPct: Float
): FiniteAnimationSpec<T> {
    require(speedupPct >= 0f) { "speedupPct has to be positive. Was: $speedupPct" }
    return speedFactor(1 + speedupPct / 100)
}

/**
 * Returns a new [FiniteAnimationSpec] that is a slower version of this one.
 *
 * @param slowdownPct How much to slow down the animation, as a percentage of the current speed. 0f
 *   being no change, 50f being half the speed.
 */
internal fun <T> FiniteAnimationSpec<T>.slower(
    @FloatRange(from = 0.0, to = 100.0, toInclusive = false) slowdownPct: Float
): FiniteAnimationSpec<T> {
    require(slowdownPct >= 0f && slowdownPct < 100f) {
        "slowdownPct has to be between 0 and 100. Was: $slowdownPct"
    }
    return speedFactor(1 - slowdownPct / 100)
}

/**
 * Returns the animated color based on enabled state.
 *
 * @param enabled Boolean flag checking if the component is enabled.
 * @param enabledColor Color when [enabled] = true.
 * @param disabledColor Color when [enabled] = false.
 * @param animationSpec AnimationSpec for the color transition animations.
 */
@Composable
internal fun animateEnabledStateColor(
    enabled: Boolean,
    enabledColor: Color,
    disabledColor: Color,
    animationSpec: AnimationSpec<Color>,
): State<Color> =
    animateColorAsState(
        targetValue = if (enabled) enabledColor else disabledColor,
        animationSpec = animationSpec,
    )

/**
 * Returns a modified [FiniteAnimationSpec] with a delay of [startDelayMillis].
 *
 * @param startDelayMillis how long to delay before starting the animation, in ms.
 */
internal fun <T> FiniteAnimationSpec<T>.delayMillis(
    startDelayMillis: Long
): FiniteAnimationSpec<T> {
    require(startDelayMillis >= 0) { "startDelayMillis has to be positive. Was: $startDelayMillis" }
    return WrappedAnimationSpec(this, 1f, TimeUnit.MILLISECONDS.toNanos(startDelayMillis))
}

private class WrappedAnimationSpec<T>(
    val wrapped: FiniteAnimationSpec<T>,
    val speedupFactor: Float,
    val startDelayNanos: Long = 0,
) : FiniteAnimationSpec<T> {
    override fun <V : AnimationVector> vectorize(
        converter: TwoWayConverter<T, V>
    ): VectorizedFiniteAnimationSpec<V> =
        WrappedVectorizedAnimationSpec(wrapped.vectorize(converter), speedupFactor, startDelayNanos)
}

private class WrappedVectorizedAnimationSpec<V : AnimationVector>(
    val wrapped: VectorizedFiniteAnimationSpec<V>,
    val speedupFactor: Float,
    val startDelayNanos: Long = 0,
) : VectorizedFiniteAnimationSpec<V> {

    override fun getValueFromNanos(
        playTimeNanos: Long,
        initialValue: V,
        targetValue: V,
        initialVelocity: V,
    ): V =
        if (playTimeNanos < startDelayNanos) {
            initialValue
        } else {
            wrapped.getValueFromNanos(
                ((playTimeNanos - startDelayNanos) * speedupFactor).toLong(),
                initialValue,
                targetValue,
                initialVelocity,
            )
        }

    override fun getVelocityFromNanos(
        playTimeNanos: Long,
        initialValue: V,
        targetValue: V,
        initialVelocity: V,
    ): V =
        if (playTimeNanos < startDelayNanos) {
            initialVelocity
        } else {
            wrapped.getVelocityFromNanos(
                ((playTimeNanos - startDelayNanos) * speedupFactor).toLong(),
                initialValue,
                targetValue,
                initialVelocity,
            ) * speedupFactor
        }

    @Suppress("MethodNameUnits")
    override fun getDurationNanos(initialValue: V, targetValue: V, initialVelocity: V): Long =
        (wrapped.getDurationNanos(initialValue, targetValue, initialVelocity) / speedupFactor)
            .toLong() + startDelayNanos
}

@Suppress("UNCHECKED_CAST")
internal operator fun <T : AnimationVector> T.times(k: Float): T {
    val t = this as AnimationVector
    return when (t) {
        is AnimationVector1D -> AnimationVector1D(t.value * k)
        is AnimationVector2D -> AnimationVector2D(t.v1 * k, t.v2 * k)
        is AnimationVector3D -> AnimationVector3D(t.v1 * k, t.v2 * k, t.v3 * k)
        is AnimationVector4D -> AnimationVector4D(t.v1 * k, t.v2 * k, t.v3 * k, t.v4 * k)
    }
        as T
}

internal suspend fun waitUntil(condition: () -> Boolean) {
    val initialTimeMillis = withFrameMillis { it }
    while (!condition()) {
        val timeMillis = withFrameMillis { it }
        if (timeMillis - initialTimeMillis > MAX_WAIT_TIME_MILLIS) return
    }
    return
}

/** Delay to be used for animations. Will enable delay only when ReducedMotion is disabled. */
internal suspend fun animatedDelay(duration: Long, reduceMotionEnabled: Boolean) {
    if (!reduceMotionEnabled) {
        delay(duration)
    }
}

/**
 * Animated [Text] based component that on text change fades out the old value and then fades in the
 * new value in one smooth animation.
 */
@Composable
internal fun FadeLabel(
    text: String,
    animationSpec: FiniteAnimationSpec<Float>,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    style: TextStyle = LocalTextStyle.current,
    maxLines: Int = LocalTextConfiguration.current.maxLines,
    textAlign: TextAlign? = LocalTextConfiguration.current.textAlign,
) {
    var currentText by remember { mutableStateOf(text) }
    var targetText by remember { mutableStateOf(text) }
    val animatedAlpha = remember { Animatable(1f) }

    LaunchedEffect(text) {
        // Don't animate the first time the text is set
        if (currentText.isEmpty() || currentText == text) {
            currentText = text
            return@LaunchedEffect
        }

        // If an animation is already running when a new animation is started, finish the animation
        // quickly and then start the new animation
        if (animatedAlpha.value != animatedAlpha.targetValue) {
            animatedAlpha.animateTo(-1f, animationSpec.faster(200f)) {
                if (animatedAlpha.value < 0f) {
                    currentText = targetText
                }
            }
            animatedAlpha.snapTo(1f)
        }

        targetText = text
        animatedAlpha.animateTo(-1f, animationSpec) {
            if (animatedAlpha.value < 0f) {
                currentText = targetText
            }
        }
        animatedAlpha.snapTo(1f)
    }

    Text(
        text = currentText,
        modifier = modifier.graphicsLayer { alpha = abs(animatedAlpha.value) },
        color = color,
        style = style,
        maxLines = maxLines,
        textAlign = textAlign,
    )
}

private const val MAX_WAIT_TIME_MILLIS = 1_000L
