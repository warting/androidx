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

package androidx.xr.glimmer.list

import androidx.compose.animation.core.AnimationState
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.animateTo
import androidx.compose.animation.core.copy
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.foundation.lazy.layout.LazyLayoutScrollScope
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastFirstOrNull
import androidx.xr.glimmer.requirePrecondition
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.abs

private val TargetDistance = 2500.dp
private val BoundDistance = 1500.dp
private val MinimumDistance = 50.dp
internal const val NumberOfItemsToTeleport = 100

internal class GlimmerListScrollScope(private val state: ListState, scrollScope: ScrollScope) :
    LazyLayoutScrollScope, ScrollScope by scrollScope {

    override val firstVisibleItemIndex: Int
        get() = state.firstVisibleItemIndex

    override val firstVisibleItemScrollOffset: Int
        get() = state.firstVisibleItemScrollOffset

    override val lastVisibleItemIndex: Int
        get() = state.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0

    override val itemCount: Int
        get() = state.layoutInfo.totalItemsCount

    override fun snapToItem(index: Int, offset: Int) {
        state.snapToItemIndexInternal(index, offset)
    }

    override fun calculateDistanceTo(targetIndex: Int, targetOffset: Int): Int {
        val layoutInfo = state.layoutInfo
        if (layoutInfo.visibleItemsInfo.isEmpty()) return 0
        return if (targetIndex !in firstVisibleItemIndex..lastVisibleItemIndex) {
            val averageSize = layoutInfo.visibleItemsAverageSize()
            val indexesDiff = targetIndex - firstVisibleItemIndex
            (averageSize * indexesDiff) - firstVisibleItemScrollOffset
        } else {
            val visibleItem =
                layoutInfo.visibleItemsInfo.fastFirstOrNull { it.index == targetIndex }
            visibleItem?.offset ?: 0
        } + targetOffset
    }
}

/**
 * Default animateScrollToItem logic to be used by any [LazyLayoutScrollScope].
 *
 * @param index Target index to animate to.
 * @param scrollOffset Target offset to animate to.
 * @param numOfItemsForTeleport In case teleporting is needed, the number of items to jump
 *   ahead/back to avoid composing intermediate items.
 * @param density A [Density] instance.
 */
internal suspend fun LazyLayoutScrollScope.animateScrollToItem(
    index: Int,
    scrollOffset: Int,
    numOfItemsForTeleport: Int,
    density: Density,
) {
    requirePrecondition(index >= 0f) { "Index should be non-negative" }

    try {
        val targetDistancePx = with(density) { TargetDistance.toPx() }
        val boundDistancePx = with(density) { BoundDistance.toPx() }
        val minDistancePx = with(density) { MinimumDistance.toPx() }
        var loop = true
        var anim = AnimationState(0f)

        if (isItemVisible(index)) {
            val targetItemInitialOffset = calculateDistanceTo(index)
            // It's already visible, just animate directly
            throw ItemFoundInScroll(targetItemInitialOffset, anim)
        }
        val forward = index > firstVisibleItemIndex

        fun isOvershot(): Boolean {
            // Did we scroll past the item?
            @Suppress("RedundantIf") // It's way easier to understand the logic this way
            return if (forward) {
                if (firstVisibleItemIndex > index) {
                    true
                } else if (
                    firstVisibleItemIndex == index && firstVisibleItemScrollOffset > scrollOffset
                ) {
                    true
                } else {
                    false
                }
            } else { // backward
                if (firstVisibleItemIndex < index) {
                    true
                } else if (
                    firstVisibleItemIndex == index && firstVisibleItemScrollOffset < scrollOffset
                ) {
                    true
                } else {
                    false
                }
            }
        }

        var loops = 1
        while (loop && itemCount > 0) {
            val expectedDistance = calculateDistanceTo(index) + scrollOffset
            val target =
                if (abs(expectedDistance) < targetDistancePx) {
                    val absTargetPx = maxOf(abs(expectedDistance.toFloat()), minDistancePx)
                    if (forward) absTargetPx else -absTargetPx
                } else {
                    if (forward) targetDistancePx else -targetDistancePx
                }

            debugLog {
                "Scrolling to index=$index offset=$scrollOffset from " +
                    "index=$firstVisibleItemIndex offset=$firstVisibleItemScrollOffset with " +
                    "calculated target=$target"
            }

            anim = anim.copy(value = 0f)
            var prevValue = 0f
            anim.animateTo(target, sequentialAnimation = (anim.velocity != 0f)) {
                // If we haven't found the item yet, check if it's visible.
                debugLog { "firstVisibleItemIndex=$firstVisibleItemIndex" }
                if (!isItemVisible(index)) {
                    // Springs can overshoot their target, clamp to the desired range
                    val coercedValue =
                        if (target > 0) {
                            value.coerceAtMost(target)
                        } else {
                            value.coerceAtLeast(target)
                        }
                    val delta = coercedValue - prevValue
                    debugLog {
                        "Scrolling by $delta (target: $target, coercedValue: $coercedValue)"
                    }

                    val consumed = scrollBy(delta)
                    if (isItemVisible(index)) {
                        debugLog { "Found the item after performing scrollBy()" }
                    } else if (!isOvershot()) {
                        if (delta != consumed) {
                            debugLog { "Hit end without finding the item" }
                            cancelAnimation()
                            loop = false
                            return@animateTo
                        }
                        prevValue += delta
                        if (forward) {
                            if (value > boundDistancePx) {
                                debugLog { "Struck bound going forward" }
                                cancelAnimation()
                            }
                        } else {
                            if (value < -boundDistancePx) {
                                debugLog { "Struck bound going backward" }
                                cancelAnimation()
                            }
                        }

                        if (forward) {
                            if (
                                loops >= 2 && index - lastVisibleItemIndex > numOfItemsForTeleport
                            ) {
                                // Teleport
                                debugLog { "Teleport forward" }
                                snapToItem(index = index - numOfItemsForTeleport, offset = 0)
                            }
                        } else {
                            if (
                                loops >= 2 && firstVisibleItemIndex - index > numOfItemsForTeleport
                            ) {
                                // Teleport
                                debugLog { "Teleport backward" }
                                snapToItem(index = index + numOfItemsForTeleport, offset = 0)
                            }
                        }
                    }
                }

                // We don't throw ItemFoundInScroll when we snap, because once we've snapped to
                // the final position, there's no need to animate to it.
                if (isOvershot()) {
                    debugLog {
                        "Overshot, " +
                            "item $firstVisibleItemIndex at  $firstVisibleItemScrollOffset," +
                            " target is $scrollOffset"
                    }
                    snapToItem(index = index, offset = scrollOffset)
                    loop = false
                    cancelAnimation()
                    return@animateTo
                } else if (isItemVisible(index)) {
                    val targetItemOffset = calculateDistanceTo(index)
                    debugLog { "Found item" }
                    throw ItemFoundInScroll(targetItemOffset, anim)
                }
            }

            loops++
        }
    } catch (itemFound: ItemFoundInScroll) {
        // We found it, animate to it
        // Bring to the requested position - will be automatically stopped if not possible
        val anim = itemFound.previousAnimation.copy(value = 0f)
        val target = (itemFound.itemOffset + scrollOffset).toFloat()
        var prevValue = 0f
        debugLog { "Seeking by $target at velocity ${itemFound.previousAnimation.velocity}" }
        anim.animateTo(target, sequentialAnimation = (anim.velocity != 0f)) {
            // Springs can overshoot their target, clamp to the desired range
            val coercedValue =
                when {
                    target > 0 -> {
                        value.coerceAtMost(target)
                    }
                    target < 0 -> {
                        value.coerceAtLeast(target)
                    }
                    else -> {
                        debugLog { "WARNING: somehow ended up seeking 0px, this shouldn't happen" }
                        0f
                    }
                }
            val delta = coercedValue - prevValue
            debugLog { "Seeking by $delta (coercedValue = $coercedValue)" }
            val consumed = scrollBy(delta)
            if (
                delta != consumed /* hit the end, stop */ ||
                    coercedValue != value /* would have overshot, stop */
            ) {
                cancelAnimation()
            }
            prevValue += delta
        }
        // Once we're finished the animation, snap to the exact position to account for
        // rounding error (otherwise we tend to end up with the previous item scrolled the
        // tiniest bit onscreen)
        // TODO: b/427148034 - Prevent temporarily scrolling *past* the item
        snapToItem(index = index, offset = scrollOffset)
    }
}

internal fun LazyLayoutScrollScope.isItemVisible(index: Int): Boolean {
    return index in firstVisibleItemIndex..lastVisibleItemIndex
}

private const val DEBUG = false

private inline fun debugLog(generateMsg: () -> String) {
    if (DEBUG) {
        println("LazyScrolling: ${generateMsg()}")
    }
}

private class ItemFoundInScroll(
    val itemOffset: Int,
    val previousAnimation: AnimationState<Float, AnimationVector1D>,
) : CancellationException()
