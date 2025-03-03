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

import androidx.annotation.VisibleForTesting
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.util.fastForEach
import androidx.wear.compose.foundation.ScrollInfoProvider
import kotlin.collections.find
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

internal class ScaffoldState(
    internal val appScaffoldPresent: Boolean,
    appTimeText: (@Composable (() -> Unit))? = null,
) {
    val fullScreenContent = FullScreenContent()
    val screenContent = ScreenContent(appTimeText)

    /**
     * Represents the scale factor applied to the parent screen. This should be used when scaling is
     * needed for transitions or other animations affecting the parent.
     */
    var parentScale = mutableFloatStateOf(1f)
}

/**
 * Manages an ordered list of full-screen composable content items. Used for displaying the full
 * screen content - such as various dialogs.
 */
internal class FullScreenContent() {

    fun addOrUpdateFullScreen(key: Any, content: @Composable () -> Unit) {
        contentItems.apply {
            find { it.key === key }?.let { it.content = content }
                ?: add(FullScreenContentItem(key, content))
        }
    }

    fun removeFullScreen(key: Any) {
        contentItems.removeIf { it.key === key }
    }

    @Composable fun OverlayContent() = contentItems.forEach { it.content() }

    @VisibleForTesting internal val contentItems = mutableStateListOf<FullScreenContentItem>()

    internal data class FullScreenContentItem(val key: Any, var content: @Composable () -> Unit)
}

/**
 * Manages the content and state of a screen, including the visibility and behavior of a time text
 * element and handling screen stages (New, Scrolling, Idle).
 *
 * This class is designed to be used internally within a screen management system. It allows adding
 * and removing screen content, displaying a time text element, and managing the screen's stage
 * based on scrolling activity.
 */
internal class ScreenContent(private val appTimeText: @Composable (() -> Unit)?) {

    val timeText: @Composable (() -> Unit)
        get() = {
            val (screenContent, timeText) = currentContent()
            Box(
                modifier =
                    screenContent?.scrollInfoProvider?.let {
                        Modifier.fillMaxSize().scrollAway(it) { screenStage.value }
                    } ?: Modifier
            ) {
                timeText()
            }
        }

    fun removeScreen(key: Any) {
        contentItems.removeIf { it.key === key }
    }

    fun addScreen(
        key: Any,
        timeText: @Composable (() -> Unit)?,
        scrollInfoProvider: ScrollInfoProvider? = null
    ) {
        contentItems.add(ScreenContent(key, scrollInfoProvider, timeText))
    }

    internal val screenStage: MutableState<ScreenStage> = mutableStateOf(ScreenStage.New)

    @Composable
    internal fun UpdateIdlingDetectorIfNeeded() {
        val scrollInfoProvider = currentContent().first?.scrollInfoProvider
        LaunchedEffect(scrollInfoProvider) { screenStage.value = ScreenStage.New }
        if (scrollInfoProvider?.isScrollInProgress == true) {
            screenStage.value = ScreenStage.Scrolling
        } else {
            LaunchedEffect(Unit) {
                // Entering the idle state will show the Time text (if it's hidden) AND hide the
                // scroll indicator.
                delay(IDLE_DELAY)
                screenStage.value = ScreenStage.Idle
            }
        }
    }

    private fun currentContent(): Pair<ScreenContent?, @Composable (() -> Unit)> {
        var resultTimeText: @Composable (() -> Unit)? = null
        var resultContent: ScreenContent? = null
        contentItems.fastForEach {
            if (it.timeText != null) {
                resultTimeText = it.timeText
            }
            if (it.scrollInfoProvider != null) {
                resultContent = it
            }
        }
        return resultContent to (resultTimeText ?: appTimeText ?: {})
    }

    private val contentItems = mutableStateListOf<ScreenContent>()

    private data class ScreenContent(
        val key: Any,
        val scrollInfoProvider: ScrollInfoProvider? = null,
        val timeText: (@Composable () -> Unit)? = null,
    )
}

@Composable
internal fun AnimatedIndicator(
    isVisible: () -> Boolean,
    animationSpec: AnimationSpec<Float>? = spring(stiffness = Spring.StiffnessMediumLow),
    content: @Composable (BoxScope.() -> Unit)? = null
) {
    // Skip if no indicator provided
    content?.let { pageIndicator ->
        if (animationSpec == null) {
            // if no animationSpec is provided then indicator will always be visible
            Box(modifier = Modifier.fillMaxSize(), content = pageIndicator)
        } else {
            // if animationSpec is provided this will be used to fade out indicator
            val alphaValue = remember { mutableFloatStateOf(0f) }
            LaunchedEffect(isVisible) {
                launch {
                    snapshotFlow { if (isVisible()) 1f else 0f }
                        .distinctUntilChanged()
                        .collectLatest { targetValue ->
                            animate(
                                alphaValue.floatValue,
                                targetValue,
                                animationSpec = animationSpec
                            ) { value, _ ->
                                alphaValue.floatValue = value
                            }
                        }
                }
            }
            Box(
                modifier = Modifier.fillMaxSize().graphicsLayer { alpha = alphaValue.floatValue },
                content = pageIndicator
            )
        }
    }
}

internal val LocalScaffoldState = compositionLocalOf { ScaffoldState(appScaffoldPresent = false) }

private const val IDLE_DELAY = 2000L
