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

package androidx.compose.runtime.tooling

import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Row
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Composition
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.ExperimentalComposeRuntimeApi
import androidx.compose.runtime.InternalComposeApi
import androidx.compose.runtime.Recomposer
import androidx.compose.runtime.currentComposer
import androidx.compose.runtime.currentCompositionContext
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@OptIn(ExperimentalComposeRuntimeApi::class)
@RunWith(AndroidJUnit4::class)
class CompositionRegistrationObserverTest {

    @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private lateinit var rootRecomposer: Recomposer

    @Test
    fun testRecomposerNotifiesForInitialCompositions() = runTest {
        val expectedCompositions = mutableSetOf<Composition>()
        setContent {
            Row {
                repeat(4) { subcomposition ->
                    SubcomposeLayout { constraints ->
                        val content =
                            subcompose(null) {
                                    Text("Subcomposition $subcomposition")

                                    val composition = currentComposer.composition
                                    DisposableEffect(composition) {
                                        expectedCompositions += composition
                                        onDispose { expectedCompositions -= composition }
                                    }
                                }
                                .first()
                                .measure(constraints)
                        layout(content.width, content.height) { content.place(0, 0) }
                    }
                }
            }

            val composition = currentComposer.composition
            DisposableEffect(composition) {
                expectedCompositions += composition
                onDispose { expectedCompositions -= composition }
            }
        }

        val receivedCompositions = mutableSetOf<Composition>()
        var disposed = false
        val handle =
            rootRecomposer.observe(
                object : CompositionRegistrationObserver {
                    override fun onCompositionRegistered(
                        recomposer: Recomposer,
                        composition: Composition
                    ) {
                        assertFalse(disposed, "Callback invoked after being disposed")
                        assertSame(rootRecomposer, recomposer, "Unexpected Recomposer")
                        assertTrue(
                            receivedCompositions.add(composition),
                            "Attempted to register a duplicate composition"
                        )
                    }

                    override fun onCompositionUnregistered(
                        recomposer: Recomposer,
                        composition: Composition
                    ) {
                        assertFalse(disposed, "Callback invoked after being disposed")
                        assertSame(rootRecomposer, recomposer, "Unexpected Recomposer")
                        assertTrue(
                            receivedCompositions.remove(composition),
                            "Attempted to unregister an unknown composition"
                        )
                    }
                }
            )

        composeTestRule.awaitIdle()
        assertEquals(expectedCompositions, receivedCompositions, "Got unexpected compositions")
        assertEquals(5, expectedCompositions.size, "Got an unexpected number of compositions")

        disposed = true
        handle.dispose()
    }

    @Test
    fun testRecomposerNotifiesForAddedSubcompositions() = runTest {
        val expectedCompositions = mutableSetOf<Composition>()
        var subcompositionCount by mutableIntStateOf(0)
        setContent {
            Row {
                repeat(subcompositionCount) { subcomposition ->
                    SubcomposeLayout { constraints ->
                        val content =
                            subcompose(null) {
                                    Text("Subcomposition $subcomposition")

                                    val composition = currentComposer.composition
                                    DisposableEffect(composition) {
                                        expectedCompositions += composition
                                        onDispose { expectedCompositions -= composition }
                                    }
                                }
                                .first()
                                .measure(constraints)
                        layout(content.width, content.height) { content.place(0, 0) }
                    }
                }
            }

            val composition = currentComposer.composition
            DisposableEffect(composition) {
                expectedCompositions += composition
                onDispose { expectedCompositions -= composition }
            }
        }

        val receivedCompositions = mutableSetOf<Composition>()
        var disposed = false
        val handle =
            rootRecomposer.observe(
                object : CompositionRegistrationObserver {
                    override fun onCompositionRegistered(
                        recomposer: Recomposer,
                        composition: Composition
                    ) {
                        assertFalse(disposed, "Callback invoked after being disposed")
                        assertSame(rootRecomposer, recomposer, "Unexpected Recomposer")
                        assertTrue(
                            receivedCompositions.add(composition),
                            "Attempted to register a duplicate composition"
                        )
                    }

                    override fun onCompositionUnregistered(
                        recomposer: Recomposer,
                        composition: Composition
                    ) {
                        assertFalse(disposed, "Callback invoked after being disposed")
                        assertSame(rootRecomposer, recomposer, "Unexpected Recomposer")
                        assertTrue(
                            receivedCompositions.remove(composition),
                            "Attempted to unregister an unknown composition"
                        )
                    }
                }
            )

        composeTestRule.awaitIdle()
        assertEquals(expectedCompositions, receivedCompositions, "Got unexpected compositions")
        assertEquals(1, expectedCompositions.size, "Got an unexpected number of compositions")

        subcompositionCount = 12
        composeTestRule.awaitIdle()

        assertEquals(expectedCompositions, receivedCompositions, "Got unexpected compositions")
        assertEquals(13, expectedCompositions.size, "Got an unexpected number of compositions")

        disposed = true
        handle.dispose()
    }

    @Test
    fun testRecomposerNotifiesForRemovedSubcompositions() = runTest {
        val expectedCompositions = mutableSetOf<Composition>()
        var subcompositionCount by mutableIntStateOf(12)
        setContent {
            Row {
                repeat(subcompositionCount) { subcomposition ->
                    SubcomposeLayout { constraints ->
                        val content =
                            subcompose(null) {
                                    Text("Subcomposition $subcomposition")

                                    val composition = currentComposer.composition
                                    DisposableEffect(composition) {
                                        expectedCompositions += composition
                                        onDispose { expectedCompositions -= composition }
                                    }
                                }
                                .first()
                                .measure(constraints)
                        layout(content.width, content.height) { content.place(0, 0) }
                    }
                }
            }

            val composition = currentComposer.composition
            DisposableEffect(composition) {
                expectedCompositions += composition
                onDispose { expectedCompositions -= composition }
            }
        }

        val receivedCompositions = mutableSetOf<Composition>()
        var disposed = false
        val handle =
            rootRecomposer.observe(
                object : CompositionRegistrationObserver {
                    override fun onCompositionRegistered(
                        recomposer: Recomposer,
                        composition: Composition
                    ) {
                        assertFalse(disposed, "Callback invoked after being disposed")
                        assertSame(rootRecomposer, recomposer, "Unexpected Recomposer")
                        assertTrue(
                            receivedCompositions.add(composition),
                            "Attempted to register a duplicate composition"
                        )
                    }

                    override fun onCompositionUnregistered(
                        recomposer: Recomposer,
                        composition: Composition
                    ) {
                        assertFalse(disposed, "Callback invoked after being disposed")
                        assertSame(rootRecomposer, recomposer, "Unexpected Recomposer")
                        assertTrue(
                            receivedCompositions.remove(composition),
                            "Attempted to unregister an unknown composition"
                        )
                    }
                }
            )

        composeTestRule.awaitIdle()
        assertEquals(expectedCompositions, receivedCompositions, "Got unexpected compositions")
        assertEquals(13, expectedCompositions.size, "Got an unexpected number of compositions")

        subcompositionCount = 3
        composeTestRule.awaitIdle()

        assertEquals(expectedCompositions, receivedCompositions, "Got unexpected compositions")
        assertEquals(4, expectedCompositions.size, "Got an unexpected number of compositions")

        disposed = true
        handle.dispose()
    }

    @Test
    fun testRecomposerNotifiesForAddedComposeView() = runTest {
        val expectedCompositions = mutableSetOf<Composition>()
        var nestedInteropViewCount by mutableIntStateOf(0)
        setContent {
            Row {
                repeat(nestedInteropViewCount) { view ->
                    AndroidView(
                        factory = { context ->
                            FrameLayout(context).apply {
                                addView(
                                    ComposeView(context).apply {
                                        setContent {
                                            Text("Compose in AndroidView $view")

                                            val composition = currentComposer.composition
                                            DisposableEffect(composition) {
                                                expectedCompositions += composition
                                                onDispose { expectedCompositions -= composition }
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    )
                }
            }

            val composition = currentComposer.composition
            DisposableEffect(composition) {
                expectedCompositions += composition
                onDispose { expectedCompositions -= composition }
            }
        }

        val receivedCompositions = mutableSetOf<Composition>()
        var disposed = false
        val handle =
            rootRecomposer.observe(
                object : CompositionRegistrationObserver {
                    override fun onCompositionRegistered(
                        recomposer: Recomposer,
                        composition: Composition
                    ) {
                        assertFalse(disposed, "Callback invoked after being disposed")
                        assertSame(rootRecomposer, recomposer, "Unexpected Recomposer")
                        assertTrue(
                            receivedCompositions.add(composition),
                            "Attempted to register a duplicate composition"
                        )
                    }

                    override fun onCompositionUnregistered(
                        recomposer: Recomposer,
                        composition: Composition
                    ) {
                        assertFalse(disposed, "Callback invoked after being disposed")
                        assertSame(rootRecomposer, recomposer, "Unexpected Recomposer")
                        assertTrue(
                            receivedCompositions.remove(composition),
                            "Attempted to unregister an unknown composition"
                        )
                    }
                }
            )

        composeTestRule.awaitIdle()
        assertEquals(expectedCompositions, receivedCompositions, "Got unexpected compositions")
        assertEquals(1, expectedCompositions.size, "Got an unexpected number of compositions")

        nestedInteropViewCount = 6
        composeTestRule.awaitIdle()

        assertEquals(expectedCompositions, receivedCompositions, "Got unexpected compositions")
        assertEquals(7, expectedCompositions.size, "Got an unexpected number of compositions")

        disposed = true
        handle.dispose()
    }

    @Test
    fun testRecomposerNotifiesForRemovedComposeView() = runTest {
        val expectedCompositions = mutableSetOf<Composition>()
        var nestedInteropViewCount by mutableIntStateOf(8)
        setContent {
            Row {
                repeat(nestedInteropViewCount) { view ->
                    AndroidView(
                        factory = { context ->
                            FrameLayout(context).apply {
                                addView(
                                    ComposeView(context).apply {
                                        setContent {
                                            Text("Compose in AndroidView $view")

                                            val composition = currentComposer.composition
                                            DisposableEffect(composition) {
                                                expectedCompositions += composition
                                                onDispose { expectedCompositions -= composition }
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    )
                }
            }

            val composition = currentComposer.composition
            DisposableEffect(composition) {
                expectedCompositions += composition
                onDispose { expectedCompositions -= composition }
            }
        }

        val receivedCompositions = mutableSetOf<Composition>()
        var disposed = false
        val handle =
            rootRecomposer.observe(
                object : CompositionRegistrationObserver {
                    override fun onCompositionRegistered(
                        recomposer: Recomposer,
                        composition: Composition
                    ) {
                        assertFalse(disposed, "Callback invoked after being disposed")
                        assertSame(rootRecomposer, recomposer, "Unexpected Recomposer")
                        assertTrue(
                            receivedCompositions.add(composition),
                            "Attempted to register a duplicate composition"
                        )
                    }

                    override fun onCompositionUnregistered(
                        recomposer: Recomposer,
                        composition: Composition
                    ) {
                        assertFalse(disposed, "Callback invoked after being disposed")
                        assertSame(rootRecomposer, recomposer, "Unexpected Recomposer")
                        assertTrue(
                            receivedCompositions.remove(composition),
                            "Attempted to unregister an unknown composition"
                        )
                    }
                }
            )

        composeTestRule.awaitIdle()
        assertEquals(expectedCompositions, receivedCompositions, "Got unexpected compositions")
        assertEquals(9, expectedCompositions.size, "Got an unexpected number of compositions")

        nestedInteropViewCount = 2
        composeTestRule.awaitIdle()

        assertEquals(expectedCompositions, receivedCompositions, "Got unexpected compositions")
        assertEquals(3, expectedCompositions.size, "Got an unexpected number of compositions")

        disposed = true
        handle.dispose()
    }

    @Test
    fun testRecomposerNotifiesForAddedDialog() = runTest {
        val expectedCompositions = mutableSetOf<Composition>()
        var showDialog by mutableStateOf(false)
        setContent {
            if (showDialog) {
                Dialog(onDismissRequest = { showDialog = false }) {
                    Text("Dialog")
                    val composition = currentComposer.composition
                    DisposableEffect(composition) {
                        expectedCompositions += composition
                        onDispose { expectedCompositions -= composition }
                    }
                }
            }

            val composition = currentComposer.composition
            DisposableEffect(composition) {
                expectedCompositions += composition
                onDispose { expectedCompositions -= composition }
            }
        }

        val receivedCompositions = mutableSetOf<Composition>()
        var disposed = false
        val handle =
            rootRecomposer.observe(
                object : CompositionRegistrationObserver {
                    override fun onCompositionRegistered(
                        recomposer: Recomposer,
                        composition: Composition
                    ) {
                        assertFalse(disposed, "Callback invoked after being disposed")
                        assertSame(rootRecomposer, recomposer, "Unexpected Recomposer")
                        assertTrue(
                            receivedCompositions.add(composition),
                            "Attempted to register a duplicate composition"
                        )
                    }

                    override fun onCompositionUnregistered(
                        recomposer: Recomposer,
                        composition: Composition
                    ) {
                        assertFalse(disposed, "Callback invoked after being disposed")
                        assertSame(rootRecomposer, recomposer, "Unexpected Recomposer")
                        assertTrue(
                            receivedCompositions.remove(composition),
                            "Attempted to unregister an unknown composition"
                        )
                    }
                }
            )

        composeTestRule.awaitIdle()
        assertEquals(expectedCompositions, receivedCompositions, "Got unexpected compositions")
        assertEquals(1, expectedCompositions.size, "Got an unexpected number of compositions")

        showDialog = true
        composeTestRule.awaitIdle()

        assertEquals(expectedCompositions, receivedCompositions, "Got unexpected compositions")
        assertEquals(2, expectedCompositions.size, "Got an unexpected number of compositions")

        disposed = true
        handle.dispose()
    }

    @Test
    fun testRecomposerNotifiesForRemovedDialog() = runTest {
        val expectedCompositions = mutableSetOf<Composition>()
        var showDialog by mutableStateOf(true)
        setContent {
            if (showDialog) {
                Dialog(onDismissRequest = { showDialog = false }) {
                    Text("Dialog")
                    val composition = currentComposer.composition
                    DisposableEffect(composition) {
                        expectedCompositions += composition
                        onDispose { expectedCompositions -= composition }
                    }
                }
            }

            val composition = currentComposer.composition
            DisposableEffect(composition) {
                expectedCompositions += composition
                onDispose { expectedCompositions -= composition }
            }
        }

        val receivedCompositions = mutableSetOf<Composition>()
        var disposed = false
        val handle =
            rootRecomposer.observe(
                object : CompositionRegistrationObserver {
                    override fun onCompositionRegistered(
                        recomposer: Recomposer,
                        composition: Composition
                    ) {
                        assertFalse(disposed, "Callback invoked after being disposed")
                        assertSame(rootRecomposer, recomposer, "Unexpected Recomposer")
                        assertTrue(
                            receivedCompositions.add(composition),
                            "Attempted to register a duplicate composition"
                        )
                    }

                    override fun onCompositionUnregistered(
                        recomposer: Recomposer,
                        composition: Composition
                    ) {
                        assertFalse(disposed, "Callback invoked after being disposed")
                        assertSame(rootRecomposer, recomposer, "Unexpected Recomposer")
                        assertTrue(
                            receivedCompositions.remove(composition),
                            "Attempted to unregister an unknown composition"
                        )
                    }
                }
            )

        composeTestRule.awaitIdle()
        assertEquals(expectedCompositions, receivedCompositions, "Got unexpected compositions")
        assertEquals(2, expectedCompositions.size, "Got an unexpected number of compositions")

        showDialog = false
        composeTestRule.awaitIdle()

        assertEquals(expectedCompositions, receivedCompositions, "Got unexpected compositions")
        assertEquals(1, expectedCompositions.size, "Got an unexpected number of compositions")

        disposed = true
        handle.dispose()
    }

    private fun setContent(content: @Composable () -> Unit) {
        composeTestRule.setContent {
            content()
            @OptIn(InternalComposeApi::class)
            rootRecomposer = currentCompositionContext as Recomposer
        }
    }
}
