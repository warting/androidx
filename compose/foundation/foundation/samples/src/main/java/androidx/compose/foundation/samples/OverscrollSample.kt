/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.compose.foundation.samples

import androidx.annotation.Sampled
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.OverscrollEffect
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.gestures.rememberScrollableState
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.overscroll
import androidx.compose.foundation.rememberOverscrollEffect
import androidx.compose.foundation.withoutVisualEffect
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sign
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Sampled
@Composable
fun OverscrollSample() {
    // our custom offset overscroll that offset the element it is applied to when we hit the bound
    // on the scrollable container.
    class OffsetOverscrollEffect(val scope: CoroutineScope) : OverscrollEffect {
        private val overscrollOffset = Animatable(0f)

        override fun applyToScroll(
            delta: Offset,
            source: NestedScrollSource,
            performScroll: (Offset) -> Offset,
        ): Offset {
            // in pre scroll we relax the overscroll if needed
            // relaxation: when we are in progress of the overscroll and user scrolls in the
            // different direction = substract the overscroll first
            val sameDirection = sign(delta.y) == sign(overscrollOffset.value)
            val consumedByPreScroll =
                if (abs(overscrollOffset.value) > 0.5 && !sameDirection) {
                    val prevOverscrollValue = overscrollOffset.value
                    val newOverscrollValue = overscrollOffset.value + delta.y
                    if (sign(prevOverscrollValue) != sign(newOverscrollValue)) {
                        // sign changed, coerce to start scrolling and exit
                        scope.launch { overscrollOffset.snapTo(0f) }
                        Offset(x = 0f, y = delta.y + prevOverscrollValue)
                    } else {
                        scope.launch { overscrollOffset.snapTo(overscrollOffset.value + delta.y) }
                        delta.copy(x = 0f)
                    }
                } else {
                    Offset.Zero
                }
            val leftForScroll = delta - consumedByPreScroll
            val consumedByScroll = performScroll(leftForScroll)
            val overscrollDelta = leftForScroll - consumedByScroll
            // if it is a drag, not a fling, add the delta left to our over scroll value
            if (abs(overscrollDelta.y) > 0.5 && source == NestedScrollSource.UserInput) {
                scope.launch {
                    // multiply by 0.1 for the sake of parallax effect
                    overscrollOffset.snapTo(overscrollOffset.value + overscrollDelta.y * 0.1f)
                }
            }
            return consumedByPreScroll + consumedByScroll
        }

        override suspend fun applyToFling(
            velocity: Velocity,
            performFling: suspend (Velocity) -> Velocity,
        ) {
            val consumed = performFling(velocity)
            // when the fling happens - we just gradually animate our overscroll to 0
            val remaining = velocity - consumed
            overscrollOffset.animateTo(
                targetValue = 0f,
                initialVelocity = remaining.y,
                animationSpec = spring(),
            )
        }

        override val isInProgress: Boolean
            get() = overscrollOffset.value != 0f

        // Create a LayoutModifierNode that offsets by overscrollOffset.value
        override val node: DelegatableNode =
            object : Modifier.Node(), LayoutModifierNode {
                override fun MeasureScope.measure(
                    measurable: Measurable,
                    constraints: Constraints,
                ): MeasureResult {
                    val placeable = measurable.measure(constraints)
                    return layout(placeable.width, placeable.height) {
                        val offsetValue = IntOffset(x = 0, y = overscrollOffset.value.roundToInt())
                        placeable.placeRelativeWithLayer(offsetValue.x, offsetValue.y)
                    }
                }
            }
    }

    val offset = remember { mutableStateOf(0f) }
    val scope = rememberCoroutineScope()
    // Create the overscroll controller
    val overscroll = remember(scope) { OffsetOverscrollEffect(scope) }
    // let's build a scrollable that scroll until -512 to 512
    val scrollStateRange = (-512f).rangeTo(512f)
    Box(
        Modifier.size(150.dp)
            .scrollable(
                orientation = Orientation.Vertical,
                state =
                    rememberScrollableState { delta ->
                        // use the scroll data and indicate how much this element consumed.
                        val oldValue = offset.value
                        // coerce to our range
                        offset.value = (offset.value + delta).coerceIn(scrollStateRange)

                        offset.value - oldValue // indicate that we consumed what's needed
                    },
                // pass the overscroll to the scrollable so the data is updated
                overscrollEffect = overscroll,
            )
            .background(Color.LightGray),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            offset.value.roundToInt().toString(),
            style = TextStyle(fontSize = 32.sp),
            modifier =
                Modifier
                    // show the overscroll only on the text, not the containers (just for fun)
                    .overscroll(overscroll),
        )
    }
}

@Sampled
@Composable
fun OverscrollWithDraggable_Before() {
    var dragPosition by remember { mutableStateOf(0f) }
    val minPosition = -1000f
    val maxPosition = 1000f

    val draggableState = rememberDraggableState { delta ->
        val newPosition = (dragPosition + delta).coerceIn(minPosition, maxPosition)
        dragPosition = newPosition
    }

    Box(
        Modifier.size(100.dp).draggable(draggableState, orientation = Orientation.Horizontal),
        contentAlignment = Alignment.Center,
    ) {
        Text("Drag position $dragPosition")
    }
}

@Sampled
@Composable
fun OverscrollWithDraggable_After() {
    var dragPosition by remember { mutableStateOf(0f) }
    val minPosition = -1000f
    val maxPosition = 1000f

    val overscrollEffect = rememberOverscrollEffect()

    val draggableState = rememberDraggableState { delta ->
        // Horizontal, so convert the delta to a horizontal offset
        val deltaAsOffset = Offset(delta, 0f)

        val performDrag: (Offset) -> Offset = { remainingOffset ->
            val remainingDelta = remainingOffset.x
            val newPosition = (dragPosition + remainingDelta).coerceIn(minPosition, maxPosition)
            // Calculate how much delta we have consumed
            val consumed = newPosition - dragPosition
            dragPosition = newPosition
            // Return how much offset we consumed, so that we can show overscroll for what is left
            Offset(consumed, 0f)
        }

        if (overscrollEffect != null) {
            // Wrap the original logic inside applyToScroll
            overscrollEffect.applyToScroll(deltaAsOffset, NestedScrollSource.UserInput, performDrag)
        } else {
            performDrag(deltaAsOffset)
        }
    }

    Box(
        Modifier
            // Draw overscroll on the box
            .overscroll(overscrollEffect)
            .size(100.dp)
            .draggable(
                draggableState,
                orientation = Orientation.Horizontal,
                onDragStopped = {
                    overscrollEffect?.applyToFling(Velocity(it, 0f)) { velocity ->
                        if (dragPosition == minPosition || dragPosition == maxPosition) {
                            // If we are at the min / max bound, give overscroll all of the velocity
                            Velocity.Zero
                        } else {
                            // If we aren't at the min / max bound, consume all of the velocity so
                            // overscroll won't show. Normally in this case something like
                            // Modifier.scrollable would use the velocity to update the scroll state
                            // with a fling animation, but just do nothing to keep this simpler.
                            velocity
                        }
                    }
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text("Drag position $dragPosition")
    }
}

@Sampled
@Composable
fun OverscrollRenderedOnTopOfLazyListDecorations() {
    val items = remember { (1..100).toList() }
    val state = rememberLazyListState()
    val overscroll = rememberOverscrollEffect()
    // Create a wrapped version of the above overscroll effect that does not apply a visual effect.
    // This will be used inside LazyColumn to provide events to overscroll, without letting
    // LazyColumn render the overscroll effect internally.
    val overscrollWithoutVisualEffect = overscroll?.withoutVisualEffect()
    LazyColumn(
        content = { items(items) { Text("Item $it") } },
        state = state,
        modifier =
            Modifier.size(300.dp)
                .clip(RectangleShape)
                // Manually render the overscroll on top of the lazy list _and_ the 'decorations' we
                // are
                // manually drawing, to make sure they will also be included in the overscroll
                // effect.
                .overscroll(overscroll)
                .drawBehind {
                    state.layoutInfo.visibleItemsInfo.drop(1).forEach { info ->
                        val verticalOffset = info.offset.toFloat()
                        drawLine(
                            color = Color.Red,
                            start = Offset(0f, verticalOffset),
                            end = Offset(size.width, verticalOffset),
                        )
                    }
                },
        // Pass the overscroll effect that does not apply a visual effect inside the LazyList to
        // receive overscroll events
        overscrollEffect = overscrollWithoutVisualEffect,
    )
}
