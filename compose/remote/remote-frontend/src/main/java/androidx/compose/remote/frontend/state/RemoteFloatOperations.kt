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

package androidx.compose.remote.frontend.state

import androidx.annotation.RestrictTo
import androidx.annotation.RestrictTo.Scope
import androidx.compose.remote.core.RemoteComposeBuffer
import androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.atan2
import kotlin.math.cbrt
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.round
import kotlin.math.sign
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

private const val FP_TO_RAD = 57.29578f // 180/PI
private const val FP_TO_DEG = 0.017453292f // 180/PI

fun toFloat(a: Number): Float {
    return when (a) {
        is RemoteFloat -> a.id
        else -> a.toFloat()
    }
}

fun max(a: RemoteFloat, b: Float) =
    binaryOp(a, b, AnimatedFloatExpression.MAX) { a, b -> max(a, b) }

fun max(a: Float, b: RemoteFloat) =
    binaryOp(a, b, AnimatedFloatExpression.MAX) { a, b -> max(a, b) }

fun max(a: RemoteFloat, b: RemoteFloat) =
    binaryOp(a, b, AnimatedFloatExpression.MAX) { a, b -> max(a, b) }

fun min(a: RemoteFloat, b: Float) =
    binaryOp(a, b, AnimatedFloatExpression.MIN) { a, b -> min(a, b) }

fun min(a: Float, b: RemoteFloat) =
    binaryOp(a, b, AnimatedFloatExpression.MIN) { a, b -> min(a, b) }

fun min(a: RemoteFloat, b: RemoteFloat) =
    binaryOp(a, b, AnimatedFloatExpression.MIN) { a, b -> min(a, b) }

fun pow(a: RemoteFloat, b: Float) = binaryOp(a, b, AnimatedFloatExpression.POW) { a, b -> a.pow(b) }

fun pow(a: Float, b: RemoteFloat) = binaryOp(a, b, AnimatedFloatExpression.POW) { a, b -> a.pow(b) }

fun pow(a: RemoteFloat, b: RemoteFloat) =
    binaryOp(a, b, AnimatedFloatExpression.POW) { a, b -> a.pow(b) }

fun sqrt(a: RemoteFloat) = a.unaryOp(AnimatedFloatExpression.SQRT) { a -> sqrt(a) }

fun abs(a: RemoteFloat) = a.unaryOp(AnimatedFloatExpression.ABS) { a -> abs(a) }

fun sign(a: RemoteFloat) = a.unaryOp(AnimatedFloatExpression.SIGN) { a -> sign(a) }

fun copySign(a: RemoteFloat, b: Float) =
    binaryOp(a, b, AnimatedFloatExpression.COPY_SIGN) { a, b -> Math.copySign(a, b) }

fun copySign(a: Float, b: RemoteFloat) =
    binaryOp(a, b, AnimatedFloatExpression.COPY_SIGN) { a, b -> Math.copySign(a, b) }

fun copySign(a: RemoteFloat, b: RemoteFloat) =
    binaryOp(a, b, AnimatedFloatExpression.COPY_SIGN) { a, b -> Math.copySign(a, b) }

fun exp(a: RemoteFloat) = a.unaryOp(AnimatedFloatExpression.EXP) { a -> exp(a) }

fun ceil(a: RemoteFloat) = a.unaryOp(AnimatedFloatExpression.CEIL) { a -> ceil(a) }

fun floor(a: RemoteFloat) = a.unaryOp(AnimatedFloatExpression.FLOOR) { a -> floor(a) }

fun log(a: RemoteFloat) = a.unaryOp(AnimatedFloatExpression.LOG) { a -> log10(a) }

fun ln(a: RemoteFloat) = a.unaryOp(AnimatedFloatExpression.LN) { a -> ln(a) }

fun round(a: RemoteFloat) = a.unaryOp(AnimatedFloatExpression.ROUND) { a -> round(a) }

fun sin(a: RemoteFloat) = a.unaryOp(AnimatedFloatExpression.SIN) { a -> sin(a) }

fun cos(a: RemoteFloat) = a.unaryOp(AnimatedFloatExpression.COS) { a -> cos(a) }

fun tan(a: RemoteFloat) = a.unaryOp(AnimatedFloatExpression.TAN) { a -> tan(a) }

fun asin(a: RemoteFloat) = a.unaryOp(AnimatedFloatExpression.ASIN) { a -> asin(a) }

fun acos(a: RemoteFloat) = a.unaryOp(AnimatedFloatExpression.ACOS) { a -> acos(a) }

fun atan(a: RemoteFloat) = a.unaryOp(AnimatedFloatExpression.ATAN) { a -> atan(a) }

fun atan2(a: RemoteFloat, b: Float) =
    binaryOp(a, b, AnimatedFloatExpression.ATAN2) { a, b -> atan2(a, b) }

fun atan2(a: Float, b: RemoteFloat) =
    binaryOp(a, b, AnimatedFloatExpression.ATAN2) { a, b -> atan2(a, b) }

fun atan2(a: RemoteFloat, b: RemoteFloat) =
    binaryOp(a, b, AnimatedFloatExpression.ATAN2) { a, b -> atan2(a, b) }

fun cbrt(a: RemoteFloat) = a.unaryOp(AnimatedFloatExpression.CBRT) { a -> cbrt(a) }

fun toDeg(a: RemoteFloat) = a.unaryOp(AnimatedFloatExpression.DEG) { a -> a * FP_TO_RAD }

fun toRad(a: RemoteFloat) = a.unaryOp(AnimatedFloatExpression.RAD) { a -> a * FP_TO_DEG }

/**
 * Computes [from] + ([to] - [from]) * [tween].
 *
 * @param from The [RemoteFloat] we're interpolating from, i.e. when [tween] is 0, lerp evaluates to
 *   [from]
 * @param to The [RemoteFloat] we're interpolating towards, i.e. when [tween] is 1, lerp evaluates
 *   to [to]
 * @param tween The ratio between [from] and [to] that controls the result.
 */
fun lerp(from: RemoteFloat, to: RemoteFloat, tween: RemoteFloat): RemoteFloat {
    return RemoteFloatExpression(
        from.hasConstantValue && to.hasConstantValue && tween.hasConstantValue,
        { creationState ->
            floatArrayOf(
                *from.arrayProvider(creationState),
                *to.arrayProvider(creationState),
                *tween.arrayProvider(creationState),
                AnimatedFloatExpression.LERP,
            )
        },
    )
}

private fun isConst(a: Number) =
    when (a) {
        is RemoteFloat -> a.hasConstantValue
        else -> true
    }

/**
 * parameters can be float or RemoteFloat. Coded this way to not require 8 versions returns a*b+c
 */
fun mad(a: Number, b: Number, c: Number): RemoteFloat {
    return RemoteFloatExpression(
        isConst(a) && isConst(b) && isConst(c),
        { creationState ->
            floatArrayOf(*(toArray(a)), *(toArray(b)), *(toArray(c)), AnimatedFloatExpression.MAD)
        },
    )
}

fun clamp(min: RemoteFloat, max: RemoteFloat, value: RemoteFloat): RemoteFloat {
    return RemoteFloatExpression(
        min.hasConstantValue && max.hasConstantValue && value.hasConstantValue,
        { creationState ->
            floatArrayOf(
                *min.arrayProvider(creationState),
                *max.arrayProvider(creationState),
                *value.arrayProvider(creationState),
                AnimatedFloatExpression.CLAMP,
            )
        },
    )
}

fun clamp(min: Float, max: Float, value: RemoteFloat): RemoteFloat {
    return RemoteFloatExpression(
        value.hasConstantValue,
        { creationState ->
            floatArrayOf(
                min,
                max,
                *value.arrayProvider(creationState),
                AnimatedFloatExpression.CLAMP,
            )
        },
    )
}

/**
 * Returns a [RemoteFloat] which applies an animation based on the value of [rf].
 *
 * @param rf The [RemoteFloat] which the animation is keyed from
 * @param duration The duration of the animation in seconds
 * @param type The type of animation
 * @param spec The parameters of the animation if any
 * @param initialValue The initial value if it animates to a start
 * @param wrap If not [Float.NaN], then all animations will be computed modulo this value. For
 *   example, if the animation is for an angle, wrap=360 means that an angle of 355 would animate
 *   to 5.
 * @return A [RemoteFloat] based on [rf] but with an animation applied to it
 */
fun animateRemoteFloat(
    rf: RemoteFloat,
    duration: Float = 1f,
    @AnimationType type: Int = CUBIC_STANDARD,
    spec: FloatArray? = null,
    initialValue: Float = Float.NaN,
    wrap: Float = Float.NaN,
): RemoteFloat {
    val anim = RemoteComposeBuffer.packAnimation(duration, type, spec, initialValue, wrap)
    return AnimatedRemoteFloat(rf, anim)
}

/**
 * Returns a [RemoteFloat] which applies an animation based on the result of [content].
 *
 * @param duration The duration of the animation in seconds
 * @param type The type of animation
 * @param spec The parameters of the animation if any
 * @param initialValue The initial value if it animates to a start
 * @param wrap If not [Float.NaN], then all animations will be computed modulo this value. For
 *   example, if the animation is for an angle, wrap=360 means that an angle of 355 would animate
 *   to 5.
 * @param content Callback that provides a [RemoteFloat] upon which the animation is based
 * @return A [RemoteFloat] based on the result of [content] but with an animation applied to it
 */
fun animateRemoteFloat(
    duration: Float = 1f,
    @AnimationType type: Int = CUBIC_STANDARD,
    spec: FloatArray? = null,
    initialValue: Float = Float.NaN,
    wrap: Float = Float.NaN,
    content: () -> RemoteFloat,
): RemoteFloat {
    return animateRemoteFloat(content(), duration, type, spec, initialValue, wrap)
}
