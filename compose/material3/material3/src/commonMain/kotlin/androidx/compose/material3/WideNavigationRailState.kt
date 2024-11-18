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

package androidx.compose.material3

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animate
import androidx.compose.material3.internal.AnchoredDraggableState
import androidx.compose.material3.internal.snapTo
import androidx.compose.material3.tokens.MotionSchemeKeyTokens
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp

/**
 * A state object that can be hoisted to observe the wide navigation rail state. It allows for
 * setting to the rail to be collapsed or expanded.
 *
 * @see rememberWideNavigationRailState to construct the default implementation.
 */
@ExperimentalMaterial3ExpressiveApi
interface WideNavigationRailState {
    /** Whether the state is currently animating */
    val isAnimating: Boolean

    /** Whether the rail is going to be expanded or not. */
    @get:Suppress("GetterSetterNames") val targetValue: Boolean

    /** Whether the rail is currently expanded or not. */
    @get:Suppress("GetterSetterNames") val currentValue: Boolean

    /** Expand the rail with animation and suspend until it fully expands. */
    suspend fun expand()

    /** Collapse the rail with animation and suspend until it fully collapses. */
    suspend fun collapse()

    /**
     * Collapse the rail with animation if it's expanded, or expand it if it's collapsed, and
     * suspend until it's set to its new state.
     */
    suspend fun toggle()

    /**
     * Set the state without any animation and suspend until it's set.
     *
     * @param targetValue the expanded boolean to set to
     */
    suspend fun snapTo(targetValue: Boolean)
}

/** Create and [remember] a [WideNavigationRailState]. */
@ExperimentalMaterial3ExpressiveApi
@Composable
fun rememberWideNavigationRailState(initialValue: Boolean = false): WideNavigationRailState {
    // TODO: Load the motionScheme tokens from the component tokens file.
    val animationSpec = MotionSchemeKeyTokens.DefaultSpatial.value<Float>()
    return rememberSaveable(saver = WideNavigationRailStateImpl.Saver(animationSpec)) {
        WideNavigationRailStateImpl(
            initialValue = initialValue,
            animationSpec = animationSpec,
        )
    }
}

@ExperimentalMaterial3ExpressiveApi
internal class WideNavigationRailStateImpl(
    var initialValue: Boolean,
    private val animationSpec: AnimationSpec<Float>,
) : WideNavigationRailState {

    private val internalValue = if (initialValue) Expanded else Collapsed
    private val internalState = Animatable(internalValue, Float.VectorConverter)
    private val _currentVal = derivedStateOf { internalState.value == Expanded }

    override val isAnimating: Boolean
        get() = internalState.isRunning

    override val targetValue: Boolean
        get() = internalState.targetValue == Expanded

    override val currentValue: Boolean
        get() = _currentVal.value

    override suspend fun expand() {
        internalState.animateTo(targetValue = Expanded, animationSpec = animationSpec)
    }

    override suspend fun collapse() {
        internalState.animateTo(targetValue = Collapsed, animationSpec = animationSpec)
    }

    override suspend fun toggle() {
        internalState.animateTo(
            targetValue = if (targetValue) Collapsed else Expanded,
            animationSpec = animationSpec
        )
    }

    override suspend fun snapTo(targetValue: Boolean) {
        val target = if (targetValue) Expanded else Collapsed
        internalState.snapTo(target)
    }

    companion object {
        private const val Collapsed = 0f
        private const val Expanded = 1f

        /** The default [Saver] implementation for [WideNavigationRailState]. */
        fun Saver(
            animationSpec: AnimationSpec<Float>,
        ) =
            Saver<WideNavigationRailState, Boolean>(
                save = { it.targetValue },
                restore = { WideNavigationRailStateImpl(it, animationSpec) }
            )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
internal class ModalWideNavigationRailState(
    state: WideNavigationRailState,
    density: Density,
    val animationSpec: AnimationSpec<Float>,
) : WideNavigationRailState by state {
    internal val anchoredDraggableState: AnchoredDraggableState<Boolean> =
        AnchoredDraggableState(
            initialValue = state.targetValue,
            positionalThreshold = { distance -> distance * 0.5f },
            velocityThreshold = { with(density) { 400.dp.toPx() } },
            animationSpec = { animationSpec },
        )

    /**
     * The current value of the state.
     *
     * If no swipe or animation is in progress, this corresponds to the value the dismissible modal
     * wide navigation rail is currently in. If a swipe or an animation is in progress, this
     * corresponds to the value the rail was in before the swipe or animation started.
     */
    override val currentValue: Boolean
        get() = anchoredDraggableState.currentValue

    /**
     * The target value of the dismissible modal wide navigation rail state.
     *
     * If a swipe is in progress, this is the value that the modal rail will animate to if the swipe
     * finishes. If an animation is running, this is the target value of that animation. Finally, if
     * no swipe or animation is in progress, this is the same as the [currentValue].
     */
    override val targetValue: Boolean
        get() = anchoredDraggableState.targetValue

    override val isAnimating: Boolean
        get() = anchoredDraggableState.isAnimationRunning

    override suspend fun expand() = animateTo(true)

    override suspend fun collapse() = animateTo(false)

    override suspend fun toggle() {
        animateTo(!this.targetValue)
    }

    override suspend fun snapTo(targetValue: Boolean) {
        anchoredDraggableState.snapTo(targetValue)
    }

    /**
     * Find the closest anchor taking into account the velocity and settle at it with an animation.
     */
    internal suspend fun settle(velocity: Float) {
        anchoredDraggableState.settle(velocity)
    }

    /**
     * The current position (in pixels) of the rail, or Float.NaN before the offset is initialized.
     *
     * @see [AnchoredDraggableState.offset] for more information.
     */
    val currentOffset: Float
        get() = anchoredDraggableState.offset

    private suspend fun animateTo(
        targetValue: Boolean,
        animationSpec: AnimationSpec<Float> = this.animationSpec,
        velocity: Float = anchoredDraggableState.lastVelocity
    ) {
        anchoredDraggableState.anchoredDrag(targetValue = targetValue) { anchors, latestTarget ->
            val targetOffset = anchors.positionOf(latestTarget)
            if (!targetOffset.isNaN()) {
                var prev = if (currentOffset.isNaN()) 0f else currentOffset
                animate(prev, targetOffset, velocity, animationSpec) { value, velocity ->
                    // Our onDrag coerces the value within the bounds, but an animation may
                    // overshoot, for example a spring animation or an overshooting interpolator.
                    // We respect the user's intention and allow the overshoot, but still use
                    // DraggableState's drag for its mutex.
                    dragTo(value, velocity)
                    prev = value
                }
            }
        }
    }
}

@Stable
internal class RailPredictiveBackState {
    var swipeEdgeMatchesRail by mutableStateOf(true)

    fun update(
        isSwipeEdgeLeft: Boolean,
        isRtl: Boolean,
    ) {
        swipeEdgeMatchesRail = (isSwipeEdgeLeft && !isRtl) || (!isSwipeEdgeLeft && isRtl)
    }
}
