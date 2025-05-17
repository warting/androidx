/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.tv.integration.playground

import android.content.Context
import android.view.accessibility.AccessibilityManager
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.CollectionItemInfo
import androidx.compose.ui.semantics.collectionItemInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Carousel
import androidx.tv.material3.CarouselDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.rememberCarouselState

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun FeaturedCarouselContent() {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        items(3) { SampleLazyRow() }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                Column(
                    modifier = Modifier.focusRestorer().focusGroup(),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                ) {
                    repeat(3) {
                        key(it) {
                            Box(
                                modifier =
                                    Modifier.background(Color.Magenta.copy(alpha = 0.3f))
                                        .width(50.dp)
                                        .height(50.dp)
                                        .drawBorderOnFocus()
                                        .focusable()
                            )
                        }
                    }
                }

                FeaturedCarousel(Modifier.weight(1f))

                Column(
                    modifier = Modifier.focusRestorer().focusGroup(),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                ) {
                    repeat(3) {
                        key(it) {
                            Box(
                                modifier =
                                    Modifier.background(Color.Magenta.copy(alpha = 0.3f))
                                        .width(50.dp)
                                        .height(50.dp)
                                        .drawBorderOnFocus()
                                        .focusable()
                            )
                        }
                    }
                }
            }
        }
        items(2) { SampleLazyRow() }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
internal fun FeaturedCarousel(modifier: Modifier = Modifier) {
    val backgrounds =
        listOf(
            Color.Red.copy(alpha = 0.3f),
            Color.Yellow.copy(alpha = 0.3f),
            Color.Green.copy(alpha = 0.3f),
            Color.Blue.copy(alpha = 0.3f),
            Color.LightGray.copy(alpha = 0.3f),
            Color.Magenta.copy(alpha = 0.3f),
            Color.DarkGray.copy(alpha = 0.3f),
            Color.LightGray.copy(alpha = 0.3f),
        )

    val carouselState = rememberCarouselState()
    var carouselFocused by remember { mutableStateOf(false) }
    Carousel(
        itemCount = backgrounds.size,
        carouselState = carouselState,
        modifier =
            modifier.height(300.dp).fillMaxWidth().onFocusChanged {
                carouselFocused = it.isFocused
            },
        carouselIndicator = {
            CarouselDefaults.IndicatorRow(
                itemCount = backgrounds.size,
                activeItemIndex = carouselState.activeItemIndex,
                modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
            )
        },
        contentTransformStartToEnd = fadeIn(tween(1000)).togetherWith(fadeOut(tween(1000))),
        contentTransformEndToStart = fadeIn(tween(1000)).togetherWith(fadeOut(tween(1000))),
    ) { itemIndex ->
        Box(
            modifier =
                Modifier.background(backgrounds[itemIndex])
                    .fillMaxSize()
                    .carouselItemSemantics(itemIndex, contentDescription = "Featured Content")
        ) {
            Column(
                modifier =
                    Modifier.padding(start = 50.dp, top = 100.dp)
                        .animateEnterExit(
                            enter = slideInVertically(animationSpec = tween(1000)),
                            exit = slideOutHorizontally(animationSpec = tween(1000)),
                        )
            ) {
                Text(text = "This is sample text content.", color = Color.Yellow)
                Text(text = "Sample description of slide ${itemIndex + 1}.", color = Color.Yellow)
                val playButtonModifier =
                    if (carouselFocused) {
                        Modifier.requestFocusOnFirstGainingVisibility()
                    } else {
                        Modifier
                    }

                Row {
                    OverlayButton(modifier = playButtonModifier, text = "Play")
                    OverlayButton(text = "Add to Watchlist")
                }
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Suppress("ComposableModifierFactory")
@Composable
internal fun Modifier.carouselItemSemantics(itemIndex: Int, contentDescription: String?): Modifier {
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current
    val accessibilityManager = remember {
        context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
    }

    return this.semantics(mergeDescendants = true) {
            contentDescription?.let { this.contentDescription = it }
            collectionItemInfo =
                CollectionItemInfo(
                    rowIndex = 0,
                    rowSpan = 1,
                    columnIndex = itemIndex,
                    columnSpan = 1,
                )
        }
        .then(
            if (accessibilityManager.isEnabled) {
                Modifier.clickable { focusManager.moveFocus(FocusDirection.Enter) }
            } else Modifier
        )
}

@Composable
private fun OverlayButton(modifier: Modifier = Modifier, text: String = "Play") {
    var isFocused by remember { mutableStateOf(false) }

    Button(
        onClick = {},
        modifier =
            modifier
                .onFocusChanged { isFocused = it.isFocused }
                .padding(20.dp)
                .border(
                    width = 2.dp,
                    color = if (isFocused) Color.Red else Color.Transparent,
                    shape = RoundedCornerShape(50),
                )
                .padding(vertical = 2.dp, horizontal = 5.dp),
    ) {
        Text(text = text)
    }
}

@Composable
fun Modifier.onFirstGainingVisibility(onGainingVisibility: () -> Unit): Modifier {
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(isVisible) { if (isVisible) onGainingVisibility() }

    return onPlaced { isVisible = true }
}

@Composable
fun Modifier.requestFocusOnFirstGainingVisibility(): Modifier {
    val focusRequester = remember { FocusRequester() }
    return focusRequester(focusRequester).onFirstGainingVisibility { focusRequester.requestFocus() }
}
