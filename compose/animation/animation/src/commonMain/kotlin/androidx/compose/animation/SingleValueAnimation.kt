/*
 * Copyright 2020 The Android Open Source Project
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

@file:Suppress("DEPRECATION")

package androidx.compose.animation

import androidx.compose.animation.core.AnimationEndReason
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.AnimationState
import androidx.compose.animation.core.AnimationVector
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.TwoWayConverter
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.animateAsState
import androidx.compose.animation.core.animateTo
import androidx.compose.animation.core.isFinished
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.onCommit
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.AmbientAnimationClock
import androidx.compose.ui.unit.Bounds
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.Position

private val defaultAnimation = spring<Float>()

/**
 * Fire-and-forget animation [Composable] for [Float]. Once such an animation is created, it will be
 * positionally memoized, like other @[Composable]s. To trigger the animation, or alter the
 * course of the animation, simply supply a different [target] to the [Composable].
 *
 * Note, [animateTo] is for simple animations that cannot be canceled. For cancellable animations
 * see [animatedFloat].
 *
 * @sample androidx.compose.animation.samples.VisibilityTransitionSample
 *
 * @param target Target value of the animation
 * @param animSpec The animation that will be used to change the value through time. [SpringSpec]
 *                 will be used by default.
 * @param visibilityThreshold An optional threshold for deciding when the animation value is
 *                            considered close enough to the target.
 * @param endListener An optional end listener to get notified when the animation is finished.
 */
@Deprecated(
    "animate has been replaced with animateAsState",
    ReplaceWith(
        "animateAsState(target, animSpec, visibilityThreshold, endListener).value",
        "androidx.compose.animation.core.animateAsState"
    )
)
@Composable
fun animate(
    target: Float,
    animSpec: AnimationSpec<Float> = defaultAnimation,
    visibilityThreshold: Float = 0.01f,
    endListener: ((Float) -> Unit)? = null
): Float {
    val resolvedAnimSpec =
        if (animSpec === defaultAnimation) {
            remember(visibilityThreshold) { SpringSpec(visibilityThreshold = visibilityThreshold) }
        } else {
            animSpec
        }
    var animationState: AnimationState<Float, AnimationVector1D> by remember {
        mutableStateOf(AnimationState(target))
    }

    val currentEndListener by rememberUpdatedState(endListener)
    LaunchedEffect(target, animSpec) {
        animationState.animateTo(
            target,
            resolvedAnimSpec,
            // If the previous animation was interrupted (i.e. not finished), make it sequential.
            !animationState.isFinished
        )
        currentEndListener?.invoke(animationState.value)
    }
    return animationState.value
}

/**
 * Fire-and-forget animation [Composable] for [Color]. Once such an animation is created, it will be
 * positionally memoized, like other @[Composable]s. To trigger the animation, or alter the
 * course of the animation, simply supply a different [target] to the [Composable].
 *
 * Note, [animateTo] is for simple animations that cannot be canceled. For cancellable animations
 * see [animatedColor].
 *
 * @sample androidx.compose.animation.samples.ColorTransitionSample
 *
 * @param target Target value of the animation
 * @param animSpec The animation that will be used to change the value through time. Physics
 *                    animation will be used by default.
 * @param endListener An optional end listener to get notified when the animation is finished.
 */
@Deprecated(
    "animate has been replaced with animateAsState",
    ReplaceWith(
        "animateAsState(target, animSpec, endListener).value",
        "androidx.compose.animation.animateAsState"
    )
)
@Composable
fun animate(
    target: Color,
    animSpec: AnimationSpec<Color> = remember { spring() },
    endListener: ((Color) -> Unit)? = null
): Color {
    val converter = remember(target.colorSpace) { (Color.VectorConverter)(target.colorSpace) }
    return animate(target, converter, animSpec, endListener = endListener)
}

/**
 * Fire-and-forget animation [Composable] for [Dp]. Once such an animation is created, it will be
 * positionally memoized, like other @[Composable]s. To trigger the animation, or alter the
 * course of the animation, simply supply a different [target] to the [Composable].
 *
 * Note, [animateTo] is for simple animations that cannot be canceled. For cancellable animations
 * see [animatedValue].
 *
 * @sample androidx.compose.animation.samples.DpAnimationSample
 *
 * @param target Target value of the animation
 * @param animSpec The animation that will be used to change the value through time. Physics
 *                    animation will be used by default.
 * @param endListener An optional end listener to get notified when the animation is finished.
 */
@Deprecated(
    "animate has been replaced with animateAsState",
    ReplaceWith(
        "animateAsState(target, animSpec, endListener).value",
        "androidx.compose.animation.core.animateAsState"
    )
)
@Composable
fun animate(
    target: Dp,
    animSpec: AnimationSpec<Dp> = remember {
        spring(visibilityThreshold = Dp.VisibilityThreshold)
    },
    endListener: ((Dp) -> Unit)? = null
): Dp {
    return animate(target, Dp.VectorConverter, animSpec, endListener = endListener)
}

/**
 * Fire-and-forget animation [Composable] for [Position]. Once such an animation is created, it will
 * be positionally memoized, like other @[Composable]s. To trigger the animation, or alter the
 * course of the animation, simply supply a different [target] to the [Composable].
 *
 * Note, [animateTo] is for simple animations that cannot be canceled. For cancellable animations
 * see [animatedValue].
 *
 *     val position : Position = animate(
 *         if (selected) Position(0.dp, 0.dp) else Position(20.dp, 20.dp))
 *
 * @param target Target value of the animation
 * @param animSpec The animation that will be used to change the value through time. Physics
 *                    animation will be used by default.
 * @param endListener An optional end listener to get notified when the animation is finished.
 */
@Deprecated(
    "animate has been replaced with animateAsState",
    ReplaceWith(
        "animateAsState(target, animSpec, endListener).value",
        "androidx.compose.animation.core.animateAsState"
    )
)
@Composable
fun animate(
    target: Position,
    animSpec: AnimationSpec<Position> = remember {
        spring(visibilityThreshold = Position.VisibilityThreshold)
    },
    endListener: ((Position) -> Unit)? = null
): Position {
    return animate(
        target, Position.VectorConverter, animSpec, endListener = endListener
    )
}

/**
 * Fire-and-forget animation [Composable] for [Size]. Once such an animation is created, it will be
 * positionally memoized, like other @[Composable]s. To trigger the animation, or alter the
 * course of the animation, simply supply a different [target] to the [Composable].
 *
 * Note, [animateTo] is for simple animations that cannot be canceled. For cancellable animations
 * see [animatedValue].
 *
 *     val size : Size = animate(
 *         if (selected) Size(20f, 20f) else Size(10f, 10f))
 *
 * @param target Target value of the animation
 * @param animSpec The animation that will be used to change the value through time. Physics
 *                    animation will be used by default.
 * @param endListener An optional end listener to get notified when the animation is finished.
 */
@Deprecated(
    "animate has been replaced with animateAsState",
    ReplaceWith(
        "animateAsState(target, animSpec, endListener).value",
        "androidx.compose.animation.core.animateAsState"
    )
)
@Composable
fun animate(
    target: Size,
    animSpec: AnimationSpec<Size> = remember {
        spring(visibilityThreshold = Size.VisibilityThreshold)
    },
    endListener: ((Size) -> Unit)? = null
): Size {
    return animate(target, Size.VectorConverter, animSpec, endListener = endListener)
}

/**
 * Fire-and-forget animation [Composable] for [Bounds]. Once such an animation is created, it will be
 * positionally memoized, like other @[Composable]s. To trigger the animation, or alter the
 * course of the animation, simply supply a different [target] to the [Composable].
 *
 * Note, [animateTo] is for simple animations that cannot be canceled. For cancellable animations
 * see [animatedValue].
 *
 *    val bounds : Bounds = animate(
 *        if (collapsed) Bounds(0.dp, 0.dp, 10.dp, 20.dp) else Bounds(0.dp, 0.dp, 100.dp, 200.dp))
 *
 * @param target Target value of the animation
 * @param animSpec The animation that will be used to change the value through time. Physics
 *                    animation will be used by default.
 * @param endListener An optional end listener to get notified when the animation is finished.
 */
@Deprecated(
    "animate has been replaced with animateAsState",
    ReplaceWith(
        "animateAsState(target, animSpec, endListener).value",
        "androidx.compose.animation.core.animateAsState"
    )
)
@Composable
fun animate(
    target: Bounds,
    animSpec: AnimationSpec<Bounds> = remember {
        spring(visibilityThreshold = Bounds.VisibilityThreshold)
    },
    endListener: ((Bounds) -> Unit)? = null
): Bounds {
    return animate(
        target,
        Bounds.VectorConverter,
        animSpec,
        endListener = endListener
    )
}

/**
 * Fire-and-forget animation [Composable] for [Offset]. Once such an animation is created, it
 * will be positionally memoized, like other @[Composable]s. To trigger the animation, or alter the
 * course of the animation, simply supply a different [target] to the [Composable].
 *
 * Note, [animateTo] is for simple animations that cannot be canceled. For cancellable animations
 * see [animatedValue].
 *
 * @sample androidx.compose.animation.samples.AnimateOffsetSample
 *
 * @param target Target value of the animation
 * @param animSpec The animation that will be used to change the value through time. Physics
 *                    animation will be used by default.
 * @param endListener An optional end listener to get notified when the animation is finished.
 */
@Deprecated(
    "animate has been replaced with animateAsState",
    ReplaceWith(
        "animateAsState(target, animSpec, endListener).value",
        "androidx.compose.animation.core.animateAsState"
    )
)
@Composable
fun animate(
    target: Offset,
    animSpec: AnimationSpec<Offset> = remember {
        spring(visibilityThreshold = Offset.VisibilityThreshold)
    },
    endListener: ((Offset) -> Unit)? = null
): Offset {
    return animate(
        target, Offset.VectorConverter, animSpec, endListener = endListener
    )
}

/**
 * Fire-and-forget animation [Composable] for [Rect]. Once such an animation is created, it will
 * be positionally memoized, like other @[Composable]s. To trigger the animation, or alter the
 * course of the animation, simply supply a different [target] to the [Composable].
 *
 * Note, [animateTo] is for simple animations that cannot be canceled. For cancellable animations
 * see [animatedValue].
 *
 *    val bounds : Rect = animate(
 *        if (enabled) Rect(0f, 0f, 100f, 100f) else Rect(8f, 8f, 80f, 80f))
 *
 * @param target Target value of the animation
 * @param animSpec The animation that will be used to change the value through time. Physics
 *                    animation will be used by default.
 * @param endListener An optional end listener to get notified when the animation is finished.
 */
@Deprecated(
    "animate has been replaced with animateAsState",
    ReplaceWith(
        "animateAsState(target, animSpec, endListener).value",
        "androidx.compose.animation.core.animateAsState"
    )
)
@Composable
fun animate(
    target: Rect,
    animSpec: AnimationSpec<Rect> = remember {
        spring(visibilityThreshold = Rect.VisibilityThreshold)
    },
    endListener: ((Rect) -> Unit)? = null
): Rect {
    return animate(
        target, Rect.VectorConverter, animSpec, endListener = endListener
    )
}

/**
 * Fire-and-forget animation [Composable] for [Int]. Once such an animation is created, it
 * will be positionally memoized, like other @[Composable]s. To trigger the animation, or alter the
 * course of the animation, simply supply a different [target] to the [Composable].
 *
 * Note, [animateTo] is for simple animations that cannot be canceled. For cancellable animations
 * see [animatedValue].
 *
 * @param target Target value of the animation
 * @param animSpec The animation that will be used to change the value through time. Physics
 *                    animation will be used by default.
 * @param endListener An optional end listener to get notified when the animation is finished.
 */
@Deprecated(
    "animate has been replaced with animateAsState",
    ReplaceWith(
        "animateAsState(target, animSpec, endListener).value",
        "androidx.compose.animation.core.animateAsState"
    )
)
@Composable
fun animate(
    target: Int,
    animSpec: AnimationSpec<Int> = remember { spring(visibilityThreshold = 1) },
    endListener: ((Int) -> Unit)? = null
): Int {
    return animate(
        target, Int.VectorConverter, animSpec, endListener = endListener
    )
}

/**
 * Fire-and-forget animation [Composable] for [IntOffset]. Once such an animation is created, it
 * will be positionally memoized, like other @[Composable]s. To trigger the animation, or alter the
 * course of the animation, simply supply a different [target] to the [Composable].
 *
 * Note, [animateTo] is for simple animations that cannot be canceled. For cancellable animations
 * see [animatedValue].
 *
 * @sample androidx.compose.animation.samples.AnimateOffsetSample
 *
 * @param target Target value of the animation
 * @param animSpec The animation that will be used to change the value through time. Physics
 *                    animation will be used by default.
 * @param endListener An optional end listener to get notified when the animation is finished.
 */
@Deprecated(
    "animate has been replaced with animateAsState",
    ReplaceWith(
        "animateAsState(target, animSpec, endListener).value",
        "androidx.compose.animation.core.animateAsState"
    )
)
@Composable
fun animate(
    target: IntOffset,
    animSpec: AnimationSpec<IntOffset> = remember {
        spring(visibilityThreshold = IntOffset.VisibilityThreshold)
    },
    endListener: ((IntOffset) -> Unit)? = null
): IntOffset {
    return animate(
        target, IntOffset.VectorConverter, animSpec, endListener = endListener
    )
}

/**
 * Fire-and-forget animation [Composable] for [IntSize]. Once such an animation is created, it
 * will be positionally memoized, like other @[Composable]s. To trigger the animation, or alter the
 * course of the animation, simply supply a different [target] to the [Composable].
 *
 * Note, [animateTo] is for simple animations that cannot be canceled. For cancellable animations
 * see [animatedValue].
 *
 * @param target Target value of the animation
 * @param animSpec The animation that will be used to change the value through time. Physics
 *                    animation will be used by default.
 * @param endListener An optional end listener to get notified when the animation is finished.
 */
@Deprecated(
    "animate has been replaced with animateAsState",
    ReplaceWith(
        "animateAsState(target, animSpec, endListener).value",
        "androidx.compose.animation.core.animateAsState"
    )
)
@Composable
fun animate(
    target: IntSize,
    animSpec: AnimationSpec<IntSize> = remember {
        spring(visibilityThreshold = IntSize.VisibilityThreshold)
    },
    endListener: ((IntSize) -> Unit)? = null
): IntSize {
    return animate(
        target, IntSize.VectorConverter, animSpec, endListener = endListener
    )
}

/**
 * Fire-and-forget animation [Composable] for [AnimationVector]. Once such an animation is created,
 * it will be positionally memoized, like other @[Composable]s. To trigger the animation, or alter
 * the course of the animation, simply supply a different [target] to the [Composable].
 *
 * Note, [animateTo] is for simple animations that cannot be canceled. For cancellable animations
 * see [animatedValue].
 *
 * @param target Target value of the animation
 * @param animSpec The animation that will be used to change the value through time. Physics
 *                    animation will be used by default.
 * @param visibilityThreshold An optional threshold to define when the animation value can be
 *                            considered close enough to the target to end the animation.
 * @param endListener An optional end listener to get notified when the animation is finished.
 */
@Deprecated(
    "animate has been replaced with animateAsState",
    ReplaceWith(
        "animateAsState(target, animSpec, visibilityThreshold, endListener).value",
        "androidx.compose.animation.core.animateAsState"
    )
)
@Composable
fun <T : AnimationVector> animate(
    target: T,
    animSpec: AnimationSpec<T> = remember {
        spring(visibilityThreshold = visibilityThreshold)
    },
    visibilityThreshold: T? = null,
    endListener: ((T) -> Unit)? = null
): T {
    return animate(
        target,
        remember { TwoWayConverter<T, T>({ it }, { it }) },
        animSpec,
        endListener = endListener
    )
}

/**
 * Fire-and-forget animation [Composable] for any value. Once such an animation is created, it
 * will be positionally memoized, like other @[Composable]s. To trigger the animation, or alter
 * the course of the animation, simply supply a different [target] to the [Composable].
 *
 * Note, [animateTo] is for simple animations that cannot be canceled. For cancellable animations
 * see [animatedValue].
 *
 * @sample androidx.compose.animation.samples.ArbitraryValueTypeTransitionSample
 *
 *     data class MySize(val width: Dp, val height: Dp)
 *
 * @param target Target value of the animation
 * @param animSpec The animation that will be used to change the value through time. Physics
 *                    animation will be used by default.
 * @param visibilityThreshold An optional threshold to define when the animation value can be
 *                            considered close enough to the target to end the animation.
 * @param endListener An optional end listener to get notified when the animation is finished.
 */
@Deprecated(
    "animate has been replaced with animateAsState",
    ReplaceWith(
        "animateAsState(target, converter, animSpec, visibilityThreshold, endListener).value",
        "androidx.compose.animation.core.animateAsState"
    )
)
@Composable
fun <T, V : AnimationVector> animate(
    target: T,
    converter: TwoWayConverter<T, V>,
    animSpec: AnimationSpec<T> = remember {
        spring(visibilityThreshold = visibilityThreshold)
    },
    visibilityThreshold: T? = null,
    endListener: ((T) -> Unit)? = null
): T {
    val clock = AmbientAnimationClock.current.asDisposableClock()
    val anim = remember(clock, converter) {
        AnimatedValueModel(target, converter, clock, visibilityThreshold)
    }
    // TODO: Support changing animation while keeping the same target
    onCommit(target) {
        if (endListener != null) {
            anim.animateTo(target, animSpec) { reason, value ->
                if (reason == AnimationEndReason.TargetReached) {
                    endListener.invoke(value)
                }
            }
        } else {
            anim.animateTo(target, animSpec)
        }
    }
    return anim.value
}

/**
 * Fire-and-forget animation function for [Color]. This Composable function is overloaded for
 * different parameter types such as [Dp], [Float], [Int], [Size], [Offset], [DpOffset],
 * etc. When the provided [targetValue] is changed, the animation will run automatically. If there
 * is already an animation in-flight whe [targetValue] changes, the on-going animation will adjust
 * course to animate towards the new target value.
 *
 * [animateAsState] returns a [State] object. The value of the state object will continuously be
 * updated by the animation until the animation finishes.
 *
 * Note, [animateAsState] cannot be canceled/stopped without removing this composable function
 * from the tree. See [animatedColor][androidx.compose.animation.animatedColor] for cancelable
 * animations.
 *
 * @sample androidx.compose.animation.samples.ColorAnimationSample
 *
 * @param targetValue Target value of the animation
 * @param animationSpec The animation that will be used to change the value through time. Physics
 *                    animation will be used by default.
 * @param finishedListener An optional end listener to get notified when the animation is finished.
 */
@Composable
fun animateAsState(
    targetValue: Color,
    animationSpec: AnimationSpec<Color> = remember { spring() },
    finishedListener: ((Color) -> Unit)? = null
): State<Color> {
    val converter = remember(targetValue.colorSpace) {
        (Color.VectorConverter)(targetValue.colorSpace)
    }
    return animateAsState(
        targetValue, converter, animationSpec, finishedListener = finishedListener
    )
}