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

package androidx.compose.foundation.samples

import androidx.annotation.Sampled
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.animateBy
import androidx.compose.foundation.gestures.animateZoomBy
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.max
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

@Sampled
@Composable
fun TransformableSample() {
    Box(Modifier.size(200.dp).clipToBounds().background(Color.LightGray)) {
        // set up all transformation states
        var scale by remember { mutableStateOf(1f) }
        var rotation by remember { mutableStateOf(0f) }
        var offset by remember { mutableStateOf(Offset.Zero) }
        val coroutineScope = rememberCoroutineScope()
        // let's create a modifier state to specify how to update our UI state defined above
        val state = rememberTransformableState { zoomChange, offsetChange, rotationChange ->
            // note: scale goes by factor, not an absolute difference, so we need to multiply it
            // for this example, we don't allow downscaling, so cap it to 1f
            scale = max(scale * zoomChange, 1f)
            rotation += rotationChange
            offset += offsetChange
        }
        Box(
            Modifier
                // apply pan offset state as a layout transformation before other modifiers
                .offset { IntOffset(offset.x.roundToInt(), offset.y.roundToInt()) }
                // add transformable to listen to multitouch transformation events after offset
                .transformable(state = state)
                // optional for example: add double click to zoom
                .pointerInput(Unit) {
                    detectTapGestures(
                        onDoubleTap = { coroutineScope.launch { state.animateZoomBy(4f) } }
                    )
                }
                .fillMaxSize()
                .border(1.dp, Color.Green),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "\uD83C\uDF55",
                fontSize = 32.sp,
                // apply other transformations like rotation and zoom on the pizza slice emoji
                modifier =
                    Modifier.graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        rotationZ = rotation
                    },
            )
        }
    }
}

@Sampled
@Composable
fun TransformableSampleInsideScroll() {
    Row(Modifier.size(width = 120.dp, height = 100.dp).horizontalScroll(rememberScrollState())) {
        // first child of the scrollable row is a transformable
        Box(Modifier.size(100.dp).clipToBounds().background(Color.LightGray)) {
            // set up all transformation states
            var scale by remember { mutableStateOf(1f) }
            var rotation by remember { mutableStateOf(0f) }
            var offset by remember { mutableStateOf(Offset.Zero) }
            val coroutineScope = rememberCoroutineScope()
            // let's create a modifier state to specify how to update our UI state defined above
            val state = rememberTransformableState { zoomChange, offsetChange, rotationChange ->
                // note: scale goes by factor, not an absolute difference, so we need to multiply it
                // for this example, we don't allow downscaling, so cap it to 1f
                scale = max(scale * zoomChange, 1f)
                rotation += rotationChange
                offset += offsetChange
            }
            Box(
                Modifier
                    // apply pan offset state as a layout transformation before other modifiers
                    .offset { IntOffset(offset.x.roundToInt(), offset.y.roundToInt()) }
                    // add transformable to listen to multitouch transformation events after offset
                    // To make sure our transformable work well within pager or scrolling lists,
                    // disallow panning if we are not zoomed in.
                    .transformable(state = state, canPan = { scale != 1f })
                    // optional for example: add double click to zoom
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onDoubleTap = { coroutineScope.launch { state.animateZoomBy(4f) } }
                        )
                    }
                    .fillMaxSize()
                    .border(1.dp, Color.Green),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "\uD83C\uDF55",
                    fontSize = 32.sp,
                    // apply other transformations like rotation and zoom on the pizza slice emoji
                    modifier =
                        Modifier.graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            rotationZ = rotation
                        },
                )
            }
        }
        // other children are just colored boxes
        Box(Modifier.size(100.dp).background(Color.Red).border(2.dp, Color.Black))
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Sampled
@Composable
fun TransformableAnimateBySample() {
    Box(Modifier.size(200.dp).clipToBounds().background(Color.LightGray)) {
        // set up all transformation states
        var scale by remember { mutableStateOf(1f) }
        var rotation by remember { mutableStateOf(0f) }
        var offset by remember { mutableStateOf(Offset.Zero) }
        val coroutineScope = rememberCoroutineScope()
        // let's create a modifier state to specify how to update our UI state defined above
        val state = rememberTransformableState { zoomChange, offsetChange, rotationChange ->
            // note: scale goes by factor, not an absolute difference, so we need to multiply it
            // for this example, we don't allow downscaling, so cap it to 1f
            scale = max(scale * zoomChange, 1f)
            rotation += rotationChange
            offset += offsetChange
        }
        Box(
            Modifier
                // apply pan offset state as a layout transformation before other modifiers
                .offset { IntOffset(offset.x.roundToInt(), offset.y.roundToInt()) }
                // add transformable to listen to multitouch transformation events after offset
                .transformable(state = state)
                // detect tap gestures:
                // 1) single tap to simultaneously animate zoom, pan, and rotation
                // 2) double tap to animate back to the initial position
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = {
                            coroutineScope.launch {
                                state.animateBy(
                                    zoomFactor = 1.5f,
                                    panOffset = Offset(20f, 20f),
                                    rotationDegrees = 90f,
                                    zoomAnimationSpec = spring(),
                                    panAnimationSpec = tween(durationMillis = 1000),
                                    rotationAnimationSpec = spring(),
                                )
                            }
                        },
                        onDoubleTap = {
                            coroutineScope.launch { state.animateBy(1 / scale, -offset, -rotation) }
                        },
                    )
                }
                .fillMaxSize()
                .border(1.dp, Color.Green),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "\uD83C\uDF55",
                fontSize = 32.sp,
                // apply other transformations like rotation and zoom on the pizza slice emoji
                modifier =
                    Modifier.graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        rotationZ = rotation
                    },
            )
        }
    }
}
