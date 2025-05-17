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

package androidx.compose.ui.layout

import android.annotation.SuppressLint
import android.os.Build
import android.view.View
import android.view.ViewTreeObserver
import android.widget.FrameLayout
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.ReusableContent
import androidx.compose.runtime.ReusableContentHost
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.testutils.expectAssertionError
import androidx.compose.ui.Modifier
import androidx.compose.ui.background
import androidx.compose.ui.composed
import androidx.compose.ui.draw.assertColor
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.isExactly
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.RootMeasurePolicy.measure
import androidx.compose.ui.layout.SubcomposeLayoutState.PrecomposedSlotHandle
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.invalidateMeasurement
import androidx.compose.ui.node.invalidatePlacement
import androidx.compose.ui.platform.AndroidOwnerExtraAssertionsRule
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.semanticsId
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.TestActivity
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertPositionInRootIsEqualTo
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onChildren
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class SubcomposeLayoutTest {

    @get:Rule val rule = createAndroidComposeRule<TestActivity>()

    @get:Rule val excessiveAssertions = AndroidOwnerExtraAssertionsRule()

    @After
    fun teardown() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val activity = rule.activity
        while (!activity.isDestroyed) {
            instrumentation.runOnMainSync {
                if (!activity.isDestroyed) {
                    activity.finish()
                }
            }
        }
    }

    @Test
    fun useSizeOfTheFirstItemInSecondSubcomposition() {
        val firstTag = "first"
        val secondTag = "second"

        rule.setContent {
            SubcomposeLayout { constraints ->
                val first =
                    subcompose(0) { Spacer(Modifier.requiredSize(50.dp).testTag(firstTag)) }
                        .first()
                        .measure(constraints)

                // it is an input for the second subcomposition
                val halfFirstSize = (first.width / 2).toDp()

                val second =
                    subcompose(1) {
                            Spacer(Modifier.requiredSize(halfFirstSize).testTag(secondTag))
                        }
                        .first()
                        .measure(constraints)

                layout(first.width, first.height) {
                    first.place(0, 0)
                    second.place(first.width - second.width, first.height - second.height)
                }
            }
        }

        rule
            .onNodeWithTag(firstTag)
            .assertPositionInRootIsEqualTo(0.dp, 0.dp)
            .assertWidthIsEqualTo(50.dp)
            .assertHeightIsEqualTo(50.dp)

        rule
            .onNodeWithTag(secondTag)
            .assertPositionInRootIsEqualTo(25.dp, 25.dp)
            .assertWidthIsEqualTo(25.dp)
            .assertHeightIsEqualTo(25.dp)
    }

    @Test
    fun subcomposeMultipleLayoutsInOneSlot() {
        val firstTag = "first"
        val secondTag = "second"
        val layoutTag = "layout"

        rule.setContent {
            SubcomposeLayout(Modifier.testTag(layoutTag)) { constraints ->
                val placeables =
                    subcompose(Unit) {
                            Spacer(Modifier.requiredSize(50.dp).testTag(firstTag))
                            Spacer(Modifier.requiredSize(30.dp).testTag(secondTag))
                        }
                        .map { it.measure(constraints) }

                val maxWidth = placeables.maxByOrNull { it.width }!!.width
                val height = placeables.sumOf { it.height }

                layout(maxWidth, height) {
                    placeables.fold(0) { top, placeable ->
                        placeable.place(0, top)
                        top + placeable.height
                    }
                }
            }
        }

        rule
            .onNodeWithTag(firstTag)
            .assertPositionInRootIsEqualTo(0.dp, 0.dp)
            .assertWidthIsEqualTo(50.dp)
            .assertHeightIsEqualTo(50.dp)

        rule
            .onNodeWithTag(secondTag)
            .assertPositionInRootIsEqualTo(0.dp, 50.dp)
            .assertWidthIsEqualTo(30.dp)
            .assertHeightIsEqualTo(30.dp)

        rule.onNodeWithTag(layoutTag).assertWidthIsEqualTo(50.dp).assertHeightIsEqualTo(80.dp)
    }

    @Test
    fun recompositionDeepInsideTheSlotDoesntRecomposeUnaffectedLayerOrRemeasure() {
        val model = mutableStateOf(0)
        var measuresCount = 0
        var recompositionsCount1 = 0
        var recompositionsCount2 = 0

        rule.setContent {
            SubcomposeLayout { constraints ->
                measuresCount++
                val placeable =
                    subcompose(Unit) {
                            recompositionsCount1++
                            NonInlineBox(Modifier.requiredSize(20.dp)) {
                                model.value // model read
                                recompositionsCount2++
                            }
                        }
                        .first()
                        .measure(constraints)

                layout(placeable.width, placeable.height) { placeable.place(0, 0) }
            }
        }

        rule.runOnIdle { model.value++ }

        rule.runOnIdle {
            assertEquals(1, measuresCount)
            assertEquals(1, recompositionsCount1)
            assertEquals(2, recompositionsCount2)
        }
    }

    @Composable
    private fun NonInlineBox(modifier: Modifier, content: @Composable () -> Unit) {
        Box(modifier = modifier) { content() }
    }

    @Test
    fun recompositionOfTheFirstSlotDoestAffectTheSecond() {
        val model = mutableStateOf(0)
        var recompositionsCount1 = 0
        var recompositionsCount2 = 0

        rule.setContent {
            SubcomposeLayout {
                subcompose(1) {
                    recompositionsCount1++
                    model.value // model read
                }
                subcompose(2) { recompositionsCount2++ }

                layout(100, 100) {}
            }
        }

        rule.runOnIdle { model.value++ }

        rule.runOnIdle {
            assertEquals(2, recompositionsCount1)
            assertEquals(1, recompositionsCount2)
        }
    }

    @Test
    fun addLayoutOnlyAfterRecomposition() {
        val addChild = mutableStateOf(false)
        val childTag = "child"
        val layoutTag = "layout"

        rule.setContent {
            SubcomposeLayout(Modifier.testTag(layoutTag)) { constraints ->
                val placeables =
                    subcompose(Unit) {
                            if (addChild.value) {
                                Spacer(Modifier.requiredSize(20.dp).testTag(childTag))
                            }
                        }
                        .map { it.measure(constraints) }

                val size = placeables.firstOrNull()?.width ?: 0
                layout(size, size) { placeables.forEach { it.place(0, 0) } }
            }
        }

        rule.onNodeWithTag(layoutTag).assertWidthIsEqualTo(0.dp).assertHeightIsEqualTo(0.dp)

        rule.onNodeWithTag(childTag).assertDoesNotExist()

        rule.runOnIdle { addChild.value = true }

        rule.onNodeWithTag(layoutTag).assertWidthIsEqualTo(20.dp).assertHeightIsEqualTo(20.dp)

        rule.onNodeWithTag(childTag).assertWidthIsEqualTo(20.dp).assertHeightIsEqualTo(20.dp)
    }

    @Test
    fun providingNewLambdaCausingRecomposition() {
        val content =
            mutableStateOf<@Composable () -> Unit>({ Spacer(Modifier.requiredSize(10.dp)) })

        rule.setContent { MySubcomposeLayout(content.value) }

        val updatedTag = "updated"

        rule.runOnIdle {
            content.value = { Spacer(Modifier.requiredSize(10.dp).testTag(updatedTag)) }
        }

        rule.onNodeWithTag(updatedTag).assertIsDisplayed()
    }

    @Composable
    private fun MySubcomposeLayout(content: @Composable () -> Unit) {
        SubcomposeLayout { constraints ->
            val placeables = subcompose(Unit, content).map { it.measure(constraints) }
            val maxWidth = placeables.maxByOrNull { it.width }!!.width
            val height = placeables.sumOf { it.height }
            layout(maxWidth, height) { placeables.forEach { it.place(0, 0) } }
        }
    }

    @Test
    fun notSubcomposedSlotIsDisposed() {
        val addSlot = mutableStateOf(true)
        var composed = false
        var disposed = false

        rule.setContent {
            SubcomposeLayout {
                if (addSlot.value) {
                    subcompose(Unit) {
                        DisposableEffect(Unit) {
                            composed = true
                            onDispose {}
                        }
                        DisposableEffect(Unit) { onDispose { disposed = true } }
                    }
                }
                layout(10, 10) {}
            }
        }

        rule.runOnIdle {
            assertThat(composed).isTrue()
            assertThat(disposed).isFalse()

            addSlot.value = false
        }

        rule.runOnIdle { assertThat(disposed).isTrue() }
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun slotsAreDrawnInTheOrderTheyComposed() {
        val layoutTag = "layout"

        rule.setContent {
            SubcomposeLayout(Modifier.testTag(layoutTag)) { constraints ->
                val first =
                    subcompose(Color.Red) {
                            Spacer(Modifier.requiredSize(10.dp).background(Color.Red))
                        }
                        .first()
                        .measure(constraints)
                val second =
                    subcompose(Color.Green) {
                            Spacer(Modifier.requiredSize(10.dp).background(Color.Green))
                        }
                        .first()
                        .measure(constraints)
                layout(first.width, first.height) {
                    first.place(0, 0)
                    second.place(0, 0)
                }
            }
        }

        rule.waitForIdle()

        rule.onNodeWithTag(layoutTag).captureToImage().assertCenterPixelColor(Color.Green)
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun slotsCouldBeReordered() {
        val layoutTag = "layout"
        val firstSlotIsRed = mutableStateOf(true)

        rule.setContent {
            SubcomposeLayout(Modifier.testTag(layoutTag)) { constraints ->
                val firstColor = if (firstSlotIsRed.value) Color.Red else Color.Green
                val secondColor = if (firstSlotIsRed.value) Color.Green else Color.Red
                val first =
                    subcompose(firstColor) {
                            Spacer(Modifier.requiredSize(10.dp).background(firstColor))
                        }
                        .first()
                        .measure(constraints)
                val second =
                    subcompose(secondColor) {
                            Spacer(Modifier.requiredSize(10.dp).background(secondColor))
                        }
                        .first()
                        .measure(constraints)
                layout(first.width, first.height) {
                    first.place(0, 0)
                    second.place(0, 0)
                }
            }
        }

        rule.onNodeWithTag(layoutTag).captureToImage().assertCenterPixelColor(Color.Green)

        rule.runOnIdle { firstSlotIsRed.value = false }

        rule.onNodeWithTag(layoutTag).captureToImage().assertCenterPixelColor(Color.Red)
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun drawingOrderCouldBeChangedUsingZIndex() {
        val layoutTag = "layout"

        rule.setContent {
            SubcomposeLayout(Modifier.testTag(layoutTag)) { constraints ->
                val first =
                    subcompose(Color.Red) {
                            Spacer(Modifier.requiredSize(10.dp).background(Color.Red).zIndex(1f))
                        }
                        .first()
                        .measure(constraints)
                val second =
                    subcompose(Color.Green) {
                            Spacer(Modifier.requiredSize(10.dp).background(Color.Green))
                        }
                        .first()
                        .measure(constraints)
                layout(first.width, first.height) {
                    first.place(0, 0)
                    second.place(0, 0)
                }
            }
        }

        rule.onNodeWithTag(layoutTag).captureToImage().assertCenterPixelColor(Color.Red)
    }

    @Test
    fun slotsAreDisposedWhenLayoutIsDisposed() {
        val addLayout = mutableStateOf(true)
        var firstDisposed = false
        var secondDisposed = false

        rule.setContent {
            if (addLayout.value) {
                SubcomposeLayout {
                    subcompose(0) { DisposableEffect(Unit) { onDispose { firstDisposed = true } } }
                    subcompose(1) { DisposableEffect(Unit) { onDispose { secondDisposed = true } } }
                    layout(10, 10) {}
                }
            }
        }

        rule.runOnIdle {
            assertThat(firstDisposed).isFalse()
            assertThat(secondDisposed).isFalse()

            addLayout.value = false
        }

        rule.runOnIdle {
            assertThat(firstDisposed).isTrue()
            assertThat(secondDisposed).isTrue()
        }
    }

    @Test
    fun propagatesDensity() {
        rule.setContent {
            val size = 50.dp
            val density = Density(3f)
            val sizeIpx = with(density) { size.roundToPx() }
            CompositionLocalProvider(LocalDensity provides density) {
                SubcomposeLayout(
                    Modifier.requiredSize(size).onGloballyPositioned {
                        assertThat(it.size).isEqualTo(IntSize(sizeIpx, sizeIpx))
                    }
                ) { constraints ->
                    layout(constraints.maxWidth, constraints.maxHeight) {}
                }
            }
        }
        rule.waitForIdle()
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun drawingOrderIsControlledByPlaceCalls() {
        val layoutTag = "layout"

        rule.setContent {
            SubcomposeLayout(Modifier.testTag(layoutTag)) { constraints ->
                val first =
                    subcompose(Color.Red) {
                            Spacer(Modifier.requiredSize(10.dp).background(Color.Red))
                        }
                        .first()
                        .measure(constraints)
                val second =
                    subcompose(Color.Green) {
                            Spacer(Modifier.requiredSize(10.dp).background(Color.Green))
                        }
                        .first()
                        .measure(constraints)

                layout(first.width, first.height) {
                    second.place(0, 0)
                    first.place(0, 0)
                }
            }
        }

        rule.waitForIdle()

        rule.onNodeWithTag(layoutTag).captureToImage().assertCenterPixelColor(Color.Red)
    }

    @Test
    @LargeTest
    fun viewWithSubcomposeLayoutCanBeDetached() {
        // verifies that the View with composed SubcomposeLayout can be detached at any point of
        // time without runtime crashes and once the view will be attached again the change will
        // be applied

        val scenario = rule.activityRule.scenario

        lateinit var container1: FrameLayout
        lateinit var container2: ComposeView
        val state = mutableStateOf(10.dp)
        var stateUsedLatch = CountDownLatch(1)

        scenario.onActivity {
            container1 = FrameLayout(it)
            container2 = ComposeView(it)
            it.setContentView(container1)
            container1.addView(container2)
            container2.setContent {
                SubcomposeLayout { constraints ->
                    val first =
                        subcompose(Unit) {
                                stateUsedLatch.countDown()
                                Box(Modifier.requiredSize(state.value))
                            }
                            .first()
                            .measure(constraints)
                    layout(first.width, first.height) { first.place(0, 0) }
                }
            }
        }

        assertTrue("state was used in setup", stateUsedLatch.await(1, TimeUnit.SECONDS))

        stateUsedLatch = CountDownLatch(1)
        scenario.onActivity {
            state.value = 15.dp
            container1.removeView(container2)
        }

        // The subcomposition is allowed to be active while the View is detached,
        // but it isn't required
        rule.waitForIdle()

        scenario.onActivity { container1.addView(container2) }

        assertTrue(
            "state was used after reattaching view",
            stateUsedLatch.await(1, TimeUnit.SECONDS),
        )
    }

    @Test
    fun deactivatingOnDetachedView() {
        val scenario = rule.activityRule.scenario

        lateinit var container1: FrameLayout
        lateinit var container2: ComposeView
        lateinit var remeasurement: Remeasurement
        var emitChild = true
        var composed = false

        scenario.onActivity {
            container1 = FrameLayout(it)
            container2 = ComposeView(it)
            container2.setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
            )
            it.setContentView(container1)
            container1.addView(container2)
            val state = SubcomposeLayoutState(SubcomposeSlotReusePolicy(1))
            container2.setContent {
                SubcomposeLayout(
                    state,
                    object : RemeasurementModifier {
                        override fun onRemeasurementAvailable(param: Remeasurement) {
                            remeasurement = param
                        }
                    },
                ) { constraints ->
                    if (emitChild) {
                        subcompose(Unit) {
                            DisposableEffect(Unit) {
                                composed = true
                                onDispose { composed = false }
                            }
                        }
                    }
                    layout(10, 10) {}
                }
            }
        }

        rule.runOnIdle { container1.removeView(container2) }

        rule.runOnIdle {
            assertThat(composed).isTrue()

            emitChild = false
            remeasurement.forceRemeasure()
        }

        rule.runOnIdle { assertThat(composed).isFalse() }
    }

    @Test
    fun precompose() {
        val addSlot = mutableStateOf(false)
        var composingCounter = 0
        var composedDuringMeasure = false
        val state = SubcomposeLayoutState()
        val content: @Composable () -> Unit = { composingCounter++ }

        rule.setContent {
            SubcomposeLayout(state) {
                if (addSlot.value) {
                    composedDuringMeasure = true
                    subcompose(Unit, content)
                }
                layout(10, 10) {}
            }
        }

        rule.runOnIdle {
            assertThat(composingCounter).isEqualTo(0)
            state.precompose(Unit, content)
        }

        rule.runOnIdle {
            assertThat(composingCounter).isEqualTo(1)

            assertThat(composedDuringMeasure).isFalse()
            addSlot.value = true
        }

        rule.runOnIdle {
            assertThat(composedDuringMeasure).isTrue()
            assertThat(composingCounter).isEqualTo(1)
        }
    }

    @Test
    fun disposePrecomposedItem() {
        var composed = false
        var disposed = false
        val state = SubcomposeLayoutState()

        rule.setContent { SubcomposeLayout(state) { layout(10, 10) {} } }

        val slot =
            rule.runOnIdle {
                state.precompose(Unit) {
                    DisposableEffect(Unit) {
                        composed = true
                        onDispose { disposed = true }
                    }
                }
            }

        rule.runOnIdle {
            assertThat(composed).isTrue()
            assertThat(disposed).isFalse()

            slot.dispose()
        }

        rule.runOnIdle { assertThat(disposed).isTrue() }
    }

    @Test
    fun composeItemRegularlyAfterDisposingPrecomposedItem() {
        val addSlot = mutableStateOf(false)
        var composingCounter = 0
        var enterCounter = 0
        var exitCounter = 0
        val state = SubcomposeLayoutState()
        val content: @Composable () -> Unit =
            @Composable {
                composingCounter++
                DisposableEffect(Unit) {
                    enterCounter++
                    onDispose { exitCounter++ }
                }
            }

        rule.setContent {
            SubcomposeLayout(state) {
                if (addSlot.value) {
                    subcompose(Unit, content)
                }
                layout(10, 10) {}
            }
        }

        val slot = rule.runOnIdle { state.precompose(Unit, content) }

        rule.runOnIdle { slot.dispose() }

        rule.runOnIdle {
            assertThat(composingCounter).isEqualTo(1)
            assertThat(enterCounter).isEqualTo(1)
            assertThat(exitCounter).isEqualTo(1)

            addSlot.value = true
        }

        rule.runOnIdle {
            assertThat(composingCounter).isEqualTo(2)
            assertThat(enterCounter).isEqualTo(2)
            assertThat(exitCounter).isEqualTo(1)
        }
    }

    @Test
    fun precomposeTwoItems() {
        val addSlots = mutableStateOf(false)
        var composing1Counter = 0
        var composing2Counter = 0
        val state = SubcomposeLayoutState()
        val content1: @Composable () -> Unit = { composing1Counter++ }
        val content2: @Composable () -> Unit = { composing2Counter++ }

        rule.setContent {
            SubcomposeLayout(state) {
                subcompose(0) {}
                if (addSlots.value) {
                    subcompose(1, content1)
                    subcompose(2, content2)
                }
                subcompose(3) {}
                layout(10, 10) {}
            }
        }

        rule.runOnIdle {
            assertThat(composing1Counter).isEqualTo(0)
            assertThat(composing2Counter).isEqualTo(0)
            state.precompose(1, content1)
            state.precompose(2, content2)
        }

        rule.runOnIdle {
            assertThat(composing1Counter).isEqualTo(1)
            assertThat(composing2Counter).isEqualTo(1)
            addSlots.value = true
        }

        rule.runOnIdle {
            assertThat(composing1Counter).isEqualTo(1)
            assertThat(composing2Counter).isEqualTo(1)
        }
    }

    @Test
    fun precomposedItemDisposedWhenSubcomposeLayoutIsDisposed() {
        val emitLayout = mutableStateOf(true)
        var enterCounter = 0
        var exitCounter = 0
        val state = SubcomposeLayoutState()
        val content: @Composable () -> Unit =
            @Composable {
                DisposableEffect(Unit) {
                    enterCounter++
                    onDispose { exitCounter++ }
                }
            }

        rule.setContent {
            if (emitLayout.value) {
                SubcomposeLayout(state) { layout(10, 10) {} }
            }
        }

        rule.runOnIdle { state.precompose(Unit, content) }

        rule.runOnIdle {
            assertThat(enterCounter).isEqualTo(1)
            assertThat(exitCounter).isEqualTo(0)
            emitLayout.value = false
        }

        rule.runOnIdle { assertThat(exitCounter).isEqualTo(1) }
    }

    @Test
    fun precomposeIsNotTriggeringParentRemeasure() {
        val state = SubcomposeLayoutState()

        var measureCount = 0
        var layoutCount = 0

        rule.setContent {
            SubcomposeLayout(state) {
                measureCount++
                layout(10, 10) { layoutCount++ }
            }
        }

        rule.runOnIdle {
            assertThat(measureCount).isEqualTo(1)
            assertThat(layoutCount).isEqualTo(1)
            state.precompose(Unit) { Box(Modifier.fillMaxSize()) }
        }

        rule.runOnIdle {
            assertThat(measureCount).isEqualTo(1)
            assertThat(layoutCount).isEqualTo(1)
        }
    }

    @Test
    fun precomposedItemDisposalIsNotTriggeringParentRemeasure() {
        val state = SubcomposeLayoutState()

        var measureCount = 0
        var layoutCount = 0

        rule.setContent {
            SubcomposeLayout(state) {
                measureCount++
                layout(10, 10) { layoutCount++ }
            }
        }

        rule.runOnIdle {
            assertThat(measureCount).isEqualTo(1)
            assertThat(layoutCount).isEqualTo(1)
            val handle = state.precompose(Unit) { Box(Modifier.fillMaxSize()) }
            handle.dispose()
        }

        rule.runOnIdle {
            assertThat(measureCount).isEqualTo(1)
            assertThat(layoutCount).isEqualTo(1)
        }
    }

    @Test
    fun slotsKeptForReuse() {
        val items = mutableStateOf(listOf(0, 1, 2, 3, 4))
        val state = SubcomposeLayoutState(SubcomposeSlotReusePolicy(2))

        composeItems(state, items)
        val id0 = rule.onNodeWithTag("0").semanticsId()
        val id1 = rule.onNodeWithTag("1").semanticsId()

        rule.runOnIdle { items.value = listOf(2, 3) }

        assertNodes(active = listOf(2, 3), disposed = listOf(4))
        rule.onRoot().fetchSemanticsNode().assertLayoutDeactivatedById(id0)
        rule.onRoot().fetchSemanticsNode().assertLayoutDeactivatedById(id1)
    }

    @Test
    fun newSlotIsUsingReusedSlot() {
        val items = mutableStateOf(listOf(0, 1, 2, 3, 4))
        val state = SubcomposeLayoutState(SubcomposeSlotReusePolicy(2))

        composeItems(state, items)
        val id0 = rule.onNodeWithTag("0").semanticsId()

        rule.runOnIdle {
            items.value = listOf(2, 3)
            // 0 and 1 are now in reusable buffer
        }

        rule.runOnIdle {
            items.value = listOf(2, 3, 5)
            // the last reusable slot (1) will be used for composing 5
        }

        assertNodes(active = listOf(2, 3, 5), disposed = listOf(1, 4))
        rule.onRoot().fetchSemanticsNode().assertLayoutDeactivatedById(id0)
    }

    @Test
    fun theSameSlotIsUsedWhileItIsInReusableList() {
        val items = mutableStateOf(listOf(0, 1, 2, 3, 4))
        val state = SubcomposeLayoutState(SubcomposeSlotReusePolicy(2))

        composeItems(state, items)
        val id0 = rule.onNodeWithTag("0").semanticsId()

        rule.runOnIdle {
            items.value = listOf(2, 3)
            // 0 and 1 are now in reusable buffer
        }

        rule.runOnIdle {
            items.value = listOf(2, 3, 1)
            // slot 1 should be taken back from reusable
        }

        assertNodes(active = listOf(2, 3, 1))
        rule.onRoot().fetchSemanticsNode().assertLayoutDeactivatedById(id0)
    }

    @Test
    fun prefetchIsUsingReusableNodes() {
        val items = mutableStateOf(listOf(0, 1, 2, 3, 4))
        val state = SubcomposeLayoutState(SubcomposeSlotReusePolicy(2))

        composeItems(state, items)
        val id0 = rule.onNodeWithTag("0").semanticsId()

        rule.runOnIdle {
            items.value = listOf(2, 3)
            // 0 and 1 are now in reusable buffer
        }

        rule.runOnIdle {
            state.precompose(5) { ItemContent(5) }
            // prefetch should take slot 1 from reuse
        }

        assertNodes(active = listOf(2, 3) + /*prefetch*/ listOf(5))
        rule.onRoot().fetchSemanticsNode().assertLayoutDeactivatedById(id0)
    }

    @Test
    fun prefetchSlotWhichIsInReusableList() {
        val items = mutableStateOf(listOf(0, 1, 2, 3, 4))
        val state = SubcomposeLayoutState(SubcomposeSlotReusePolicy(3))

        composeItems(state, items)
        val id0 = rule.onNodeWithTag("0").semanticsId()
        val id1 = rule.onNodeWithTag("1").semanticsId()

        rule.runOnIdle {
            items.value = listOf(2)
            // 0, 1, 3 are now in reusable buffer
        }

        rule.runOnIdle {
            state.precompose(3) { ItemContent(3) }
            // prefetch should take slot 3 from reuse
        }

        assertNodes(active = listOf(2) + /*prefetch*/ listOf(3), disposed = listOf(4))
        rule.onRoot().fetchSemanticsNode().assertLayoutDeactivatedById(id0)
        rule.onRoot().fetchSemanticsNode().assertLayoutDeactivatedById(id1)
    }

    @Test
    fun nothingIsReusedWhenMaxSlotsAre0() {
        val items = mutableStateOf(listOf(0, 1, 2, 3, 4))
        val state = SubcomposeLayoutState(SubcomposeSlotReusePolicy(0))

        composeItems(state, items)

        rule.runOnIdle { items.value = listOf(2, 4) }

        assertNodes(active = listOf(2, 4), disposed = listOf(0, 1, 3))
    }

    @Test
    fun reuse1Node() {
        val items = mutableStateOf(listOf(0, 1, 2, 3))
        val state = SubcomposeLayoutState(SubcomposeSlotReusePolicy(1))

        composeItems(state, items)
        val id2 = rule.onNodeWithTag("2").semanticsId()

        rule.runOnIdle { items.value = listOf(0, 1) }

        assertNodes(active = listOf(0, 1), disposed = listOf(3))
        rule.onRoot().fetchSemanticsNode().assertLayoutDeactivatedById(id2)
    }

    @SuppressLint("RememberReturnType")
    @Test
    fun reusedCompositionResetsRememberedObject() {
        val slotState = mutableStateOf(0)
        var lastRememberedSlot: Any? = null
        var lastRememberedComposedModifierSlot: Any? = null

        rule.setContent {
            SubcomposeLayout(remember { SubcomposeLayoutState(SubcomposeSlotReusePolicy(1)) }) {
                val slot = slotState.value
                subcompose(slot) {
                    ReusableContent(slot) {
                        remember { lastRememberedSlot = slot }
                        Box(
                            Modifier.composed {
                                remember { lastRememberedComposedModifierSlot = slot }
                                Modifier
                            }
                        )
                    }
                }
                layout(10, 10) {}
            }
        }

        rule.runOnIdle {
            assertThat(lastRememberedSlot).isEqualTo(0)
            assertThat(lastRememberedComposedModifierSlot).isEqualTo(0)
            slotState.value = 1
        }

        rule.runOnIdle {
            assertThat(lastRememberedSlot).isEqualTo(1)
            assertThat(lastRememberedComposedModifierSlot).isEqualTo(1)
            slotState.value = 2
        }

        rule.runOnIdle {
            assertThat(lastRememberedSlot).isEqualTo(2)
            assertThat(lastRememberedComposedModifierSlot).isEqualTo(2)
        }
    }

    @Test
    fun subcomposeLayoutInsideLayoutUsingAlignmentsIsNotCrashing() {
        // fix for regression from b/189965769
        val emit = mutableStateOf(false)
        rule.setContent {
            LayoutUsingAlignments {
                Box {
                    if (emit.value) {
                        SubcomposeLayout {
                            subcompose(Unit) {}
                            layout(10, 10) {}
                        }
                    }
                }
            }
        }

        rule.runOnIdle { emit.value = true }

        // awaits that the change is applied and no crash happened
        rule.runOnIdle {}
    }

    @Test
    fun compositionLocalChangeInMainCompositionRecomposesSubcomposition() {
        var flag by mutableStateOf(true)
        val compositionLocal = compositionLocalOf<Boolean> { error("") }
        var subcomposionValue: Boolean? = null
        val subcomposeLambda =
            @Composable {
                // makes sure the recomposition happens only once after the change
                assertThat(compositionLocal.current).isNotEqualTo(subcomposionValue)
                subcomposionValue = compositionLocal.current
            }

        rule.setContent {
            CompositionLocalProvider(compositionLocal provides flag) {
                val mainMovableValue = flag
                SubcomposeLayout(
                    Modifier.drawBehind {
                        // makes sure we never draw inconsistent states
                        assertThat(subcomposionValue).isEqualTo(mainMovableValue)
                    }
                ) {
                    subcompose(Unit, subcomposeLambda)
                    layout(100, 100) {}
                }
            }
        }

        rule.runOnIdle {
            assertThat(subcomposionValue).isTrue()
            flag = false
        }

        rule.runOnIdle { assertThat(subcomposionValue).isFalse() }
    }

    @Test
    fun compositionLocalChangeInMainCompositionRecomposesSubcomposition_noRemeasure() {
        var flag by mutableStateOf(true)
        val compositionLocal = compositionLocalOf<Boolean> { error("") }
        var subcomposionValue: Boolean? = null
        val subcomposeLambda =
            @Composable {
                // makes sure the recomposition happens only once after the change
                assertThat(compositionLocal.current).isNotEqualTo(subcomposionValue)
                subcomposionValue = compositionLocal.current
            }
        val measurePolicy: SubcomposeMeasureScope.(Constraints) -> MeasureResult = {
            subcompose(Unit, subcomposeLambda)
            layout(100, 100) {}
        }

        rule.setContent {
            CompositionLocalProvider(compositionLocal provides flag) {
                SubcomposeLayout(measurePolicy = measurePolicy)
            }
        }

        rule.runOnIdle {
            assertThat(subcomposionValue).isTrue()
            flag = false
        }

        rule.runOnIdle { assertThat(subcomposionValue).isFalse() }
    }

    @Test
    fun staticCompositionLocalChangeInMainCompositionRecomposesSubcomposition() {
        var flag by mutableStateOf(true)
        val compositionLocal = staticCompositionLocalOf<Boolean> { error("") }
        var subcomposionValue: Boolean? = null
        val subcomposeLambda =
            @Composable {
                // makes sure the recomposition happens only once after the change
                assertThat(compositionLocal.current).isNotEqualTo(subcomposionValue)
                subcomposionValue = compositionLocal.current
            }
        val measureBlock: SubcomposeMeasureScope.(Constraints) -> MeasureResult = {
            subcompose(Unit, subcomposeLambda)
            layout(100, 100) {}
        }

        rule.setContent {
            CompositionLocalProvider(compositionLocal provides flag) {
                val mainCompositionValue = flag
                SubcomposeLayout(
                    Modifier.drawBehind {
                        // makes sure we never draw inconsistent states
                        assertThat(subcomposionValue).isEqualTo(mainCompositionValue)
                    },
                    measureBlock,
                )
            }
        }

        rule.runOnIdle {
            assertThat(subcomposionValue).isTrue()
            flag = false
        }

        rule.runOnIdle { assertThat(subcomposionValue).isFalse() }
    }

    @Test
    fun staticCompositionLocalChangeInMainCompositionRecomposesSubcomposition_noRemeasure() {
        var flag by mutableStateOf(true)
        val compositionLocal = staticCompositionLocalOf<Boolean> { error("") }
        var subcomposionValue: Boolean? = null
        val subcomposeLambda =
            @Composable {
                // makes sure the recomposition happens only once after the change
                assertThat(compositionLocal.current).isNotEqualTo(subcomposionValue)
                subcomposionValue = compositionLocal.current
            }
        val measurePolicy: SubcomposeMeasureScope.(Constraints) -> MeasureResult = {
            subcompose(Unit, subcomposeLambda)
            layout(100, 100) {}
        }

        rule.setContent {
            CompositionLocalProvider(compositionLocal provides flag) {
                SubcomposeLayout(measurePolicy = measurePolicy)
            }
        }

        rule.runOnIdle {
            assertThat(subcomposionValue).isTrue()
            flag = false
        }

        rule.runOnIdle { assertThat(subcomposionValue).isFalse() }
    }

    @Test
    fun staticCompositionLocalChangeInMainComposition_withNonStaticLocal_invalidatesComposition() {
        var isDark by mutableStateOf(false)

        val staticLocal = staticCompositionLocalOf<Boolean> { error("Not defined") }
        val local = compositionLocalOf<Boolean> { error("Not defined") }
        val innerLocal = staticCompositionLocalOf<Unit> { error("\not defined") }

        val content =
            @Composable {
                CompositionLocalProvider(innerLocal provides Unit) {
                    val value1 = staticLocal.current
                    val value2 = local.current
                    Box(
                        Modifier.testTag(if (value1) "dark" else "light")
                            .requiredSize(if (value2) 50.dp else 100.dp)
                    )
                }
            }

        rule.setContent {
            CompositionLocalProvider(staticLocal provides isDark) {
                CompositionLocalProvider(local provides staticLocal.current) {
                    SubcomposeLayout { constraints ->
                        val measurables = subcompose(Unit, content)
                        val placeables = measurables.map { it.measure(constraints) }
                        layout(100, 100) { placeables.forEach { it.place(IntOffset.Zero) } }
                    }
                }
            }
        }

        rule.onNodeWithTag("light").assertWidthIsEqualTo(100.dp)

        isDark = true

        rule.waitForIdle()

        rule.onNodeWithTag("dark").assertWidthIsEqualTo(50.dp)
    }

    @Test
    fun derivedStateChangeInMainCompositionRecomposesSubcomposition() {
        var flag by mutableStateOf(true)
        var subcomposionValue: Boolean? = null

        rule.setContent {
            val updatedState = rememberUpdatedState(flag)
            val derivedState = remember { derivedStateOf { updatedState.value } }
            val subcomposeLambda =
                remember<@Composable () -> Unit> {
                    {
                        // makes sure the recomposition happens only once after the change
                        assertThat(derivedState.value).isNotEqualTo(subcomposionValue)
                        subcomposionValue = derivedState.value
                    }
                }

            SubcomposeLayout(
                Modifier.drawBehind {
                    // makes sure we never draw inconsistent states
                    assertThat(subcomposionValue).isEqualTo(updatedState.value)
                }
            ) {
                subcompose(Unit, subcomposeLambda)
                layout(100, 100) {}
            }
        }

        rule.runOnIdle {
            assertThat(subcomposionValue).isTrue()
            flag = false
        }

        rule.runOnIdle { assertThat(subcomposionValue).isFalse() }
    }

    @Test
    fun updatingStateWorks() {
        val tagState = mutableStateOf("box1")

        rule.setContent {
            val tag = tagState.value
            val state = remember(tag) { SubcomposeLayoutState() }

            SubcomposeLayout(state = state) {
                val placeable =
                    subcompose(Unit) { Box(Modifier.size(10.dp).testTag(tag)) }
                        .first()
                        .measure(Constraints())
                layout(placeable.width, placeable.height) { placeable.place(0, 0) }
            }
        }

        rule.onNodeWithTag("box1").assertIsDisplayed()

        rule.runOnIdle { tagState.value = "box2" }

        rule.onNodeWithTag("box2").assertIsDisplayed()
    }

    @Test
    fun nodesKeptAsReusableAreReusedWhenTheStateObjectChanges() {
        val slotState = mutableStateOf(0)
        var remeasuresCount = 0
        val measureModifier =
            Modifier.layout { _, _ ->
                remeasuresCount++
                layout(10, 10) {}
            }
        val layoutState = mutableStateOf(SubcomposeLayoutState(SubcomposeSlotReusePolicy(1)))

        rule.setContent {
            val slot = slotState.value
            SubcomposeLayout(layoutState.value) {
                val placeable =
                    subcompose(slot) { ReusableContent(slot) { Box(measureModifier) } }
                        .first()
                        .measure(Constraints())
                layout(placeable.width, placeable.height) { placeable.place(0, 0) }
            }
        }

        rule.runOnIdle {
            slotState.value = 1
            // slot 0 is kept for reuse
        }

        rule.runOnIdle {
            assertThat(remeasuresCount).isEqualTo(2)
            remeasuresCount = 0
            slotState.value = 2 // slot 0 should be reused
            layoutState.value = SubcomposeLayoutState(SubcomposeSlotReusePolicy(1))
        }

        rule.runOnIdle {
            // there is no remeasure as the node was reused and the modifier didn't change
            assertThat(remeasuresCount).isEqualTo(0)
        }
    }

    @Test
    fun previouslyActiveNodesAreReusedWhenTheStateObjectChanges() {
        val slotState = mutableStateOf(0)
        var remeasuresCount = 0
        val measureModifier =
            Modifier.layout { _, _ ->
                remeasuresCount++
                layout(10, 10) {}
            }
        val layoutState = mutableStateOf(SubcomposeLayoutState(SubcomposeSlotReusePolicy(1)))

        rule.setContent {
            val slot = slotState.value
            SubcomposeLayout(layoutState.value) { _ ->
                val placeable =
                    subcompose(slot) { ReusableContent(slot) { Box(measureModifier) } }
                        .first()
                        .measure(Constraints())
                layout(placeable.width, placeable.height) { placeable.place(0, 0) }
            }
        }

        rule.runOnIdle {
            assertThat(remeasuresCount).isEqualTo(1)
            remeasuresCount = 0
            slotState.value = 1
            layoutState.value = SubcomposeLayoutState(SubcomposeSlotReusePolicy(1))
        }

        rule.runOnIdle {
            // there is no remeasure as the node was reused and the modifier didn't change
            assertThat(remeasuresCount).isEqualTo(0)
        }
    }

    @Test
    fun reusableNodeIsKeptAsReusableAfterStateUpdate() {
        val layoutState = mutableStateOf(SubcomposeLayoutState(SubcomposeSlotReusePolicy(1)))
        val needChild = mutableStateOf(true)

        rule.setContent {
            SubcomposeLayout(state = layoutState.value) { constraints ->
                val node =
                    if (needChild.value) {
                        subcompose(Unit) { Box(Modifier.testTag("child")) }
                            .first()
                            .measure(constraints)
                    } else {
                        null
                    }
                layout(10, 10) { node?.place(0, 0) }
            }
        }

        rule.onNodeWithTag("child").assertExists()
        val idChild = rule.onNodeWithTag("child").semanticsId()

        needChild.value = false

        rule.onRoot().fetchSemanticsNode().assertLayoutDeactivatedById(idChild)

        layoutState.value = SubcomposeLayoutState(SubcomposeSlotReusePolicy(1))

        rule.onRoot().fetchSemanticsNode().assertLayoutDeactivatedById(idChild)
    }

    @Test
    fun passingSmallerMaxSlotsToRetainForReuse() {
        val layoutState = mutableStateOf(SubcomposeLayoutState(SubcomposeSlotReusePolicy(1)))
        val needChild = mutableStateOf(true)

        rule.setContent {
            SubcomposeLayout(state = layoutState.value) { constraints ->
                val node =
                    if (needChild.value) {
                        subcompose(Unit) { Box(Modifier.testTag("child")) }
                            .first()
                            .measure(constraints)
                    } else {
                        null
                    }
                layout(10, 10) { node?.place(0, 0) }
            }
        }

        val idChild = rule.onNodeWithTag("child").semanticsId()

        rule.runOnIdle { needChild.value = false }

        rule.onRoot().fetchSemanticsNode().assertLayoutDeactivatedById(idChild)

        layoutState.value = SubcomposeLayoutState(SubcomposeSlotReusePolicy(0))

        rule.onNodeWithTag("child").assertIsDetached()
    }

    @Test
    fun compositionKeptForReuseIsDisposed() {
        val needChild = mutableStateOf(true)
        var disposed = false

        rule.setContent {
            SubcomposeLayout(state = SubcomposeLayoutState(SubcomposeSlotReusePolicy(1))) {
                if (needChild.value) {
                    subcompose(Unit) { DisposableEffect(Unit) { onDispose { disposed = true } } }
                }
                layout(10, 10) {}
            }
        }

        rule.runOnIdle { needChild.value = false }

        rule.runOnIdle {
            // the composition in the reusable pool is disposed
            assertThat(disposed).isTrue()
        }
    }

    @Test
    fun composedModifierOnReusableNodeIsDisposedButAttached() {
        val layoutState = SubcomposeLayoutState(SubcomposeSlotReusePolicy(1))
        val needChild = mutableStateOf(true)
        var composed = false

        rule.setContent {
            SubcomposeLayout(state = layoutState) { constraints ->
                val node =
                    if (needChild.value) {
                        subcompose(Unit) {
                                Box(
                                    Modifier.composed {
                                        DisposableEffect(Unit) {
                                            composed = true
                                            onDispose { composed = false }
                                        }
                                        testTag("child")
                                    }
                                )
                            }
                            .first()
                            .measure(constraints)
                    } else {
                        null
                    }
                layout(10, 10) { node?.place(0, 0) }
            }
        }

        rule.onNodeWithTag("child").assertExists()
        val idChild = rule.onNodeWithTag("child").semanticsId()

        assertThat(composed).isTrue()
        needChild.value = false

        rule.onRoot().fetchSemanticsNode().assertLayoutDeactivatedById(idChild)
        assertThat(composed).isFalse()
        needChild.value = true

        rule.onNodeWithTag("child").assertExists()
        assertThat(composed).isTrue()
    }

    @Test
    fun customPolicy_retainingExactItem() {
        val items = mutableStateOf(listOf(0, 1, 2, 3, 4))
        val policy =
            object : SubcomposeSlotReusePolicy {
                override fun getSlotsToRetain(slotIds: SubcomposeSlotReusePolicy.SlotIdsSet) {
                    assertThat(slotIds).containsExactly(1, 2, 4).inOrder()
                    slotIds.remove(1)
                    slotIds.remove(4)
                }

                override fun areCompatible(slotId: Any?, reusableSlotId: Any?): Boolean {
                    assertThat(reusableSlotId).isEqualTo(2)
                    return true
                }
            }
        val state = SubcomposeLayoutState(policy)

        composeItems(state, items)
        val id2 = rule.onNodeWithTag("2").semanticsId()

        rule.runOnIdle { items.value = listOf(0, 3) }

        assertNodes(active = listOf(0, 3), disposed = listOf(1, 4))
        rule.onRoot().fetchSemanticsNode().assertLayoutDeactivatedById(id2)

        rule.runOnIdle { items.value = listOf(0, 3, 5) }

        assertNodes(active = listOf(0, 3, 5), disposed = listOf(1, 2, 4))
    }

    @Test
    fun customPolicy_lastUsedItemsAreFirstInSet() {
        val items = mutableStateOf(listOf(0, 1, 2, 3, 4))
        var expectedReusable = arrayOf<Int>()
        val policy =
            object : SubcomposeSlotReusePolicy {
                override fun getSlotsToRetain(slotIds: SubcomposeSlotReusePolicy.SlotIdsSet) {
                    assertThat(slotIds).containsExactly(*expectedReusable).inOrder()
                }

                override fun areCompatible(slotId: Any?, reusableSlotId: Any?) = true
            }
        val state = SubcomposeLayoutState(policy)

        composeItems(state, items)

        rule.runOnIdle {
            expectedReusable = arrayOf(1, 2, 3, 4)
            items.value = listOf(0)
        }

        rule.runOnIdle {
            expectedReusable = arrayOf(1, 2, 3)
            items.value = listOf(0, 4)
        }

        rule.runOnIdle {
            expectedReusable = arrayOf(4, 1, 2, 3)
            items.value = listOf(0)
        }
    }

    @Test
    fun customPolicy_disposedPrefetchedItemIsFirstInSet() {
        val items = mutableStateOf(listOf(0, 1, 2, 3, 4))
        var expectedReusable = arrayOf<Int>()
        var callbackCalled = false
        var expectedSlotId: Any? = null
        var expectedreusableSlotId: Any? = null
        val policy =
            object : SubcomposeSlotReusePolicy {
                override fun getSlotsToRetain(slotIds: SubcomposeSlotReusePolicy.SlotIdsSet) {
                    callbackCalled = true
                    assertThat(slotIds).containsExactly(*expectedReusable).inOrder()
                }

                override fun areCompatible(slotId: Any?, reusableSlotId: Any?): Boolean {
                    assertThat(slotId).isEqualTo(expectedSlotId)
                    assertThat(reusableSlotId).isEqualTo(expectedreusableSlotId)
                    return true
                }
            }
        val state = SubcomposeLayoutState(policy)

        composeItems(state, items)

        rule.runOnIdle {
            expectedReusable = arrayOf(1, 2, 3, 4)
            items.value = listOf(0)
        }

        rule.runOnIdle {
            assertThat(callbackCalled).isTrue()
            callbackCalled = false

            expectedSlotId = 5
            expectedreusableSlotId = 4
            val handle = state.precompose(5, {}) // it should reuse slot 4
            expectedReusable = arrayOf(5, 1, 2, 3)
            handle.dispose()
            assertThat(callbackCalled).isTrue()
        }
    }

    @Test
    fun customPolicy_retainingOddNumbers() {
        fun isOdd(number: Any?): Boolean {
            return (number as Int) % 2 == 1
        }

        val items = mutableStateOf(listOf(0, 1, 2, 3, 4, 5, 6))
        val policy =
            object : SubcomposeSlotReusePolicy {
                override fun getSlotsToRetain(slotIds: SubcomposeSlotReusePolicy.SlotIdsSet) {
                    slotIds.removeAll { !isOdd(it) }
                }

                override fun areCompatible(slotId: Any?, reusableSlotId: Any?): Boolean {
                    return isOdd(slotId) && isOdd(reusableSlotId)
                }
            }
        val state = SubcomposeLayoutState(policy)

        composeItems(state, items)
        val id1 = rule.onNodeWithTag("1").semanticsId()
        val id3 = rule.onNodeWithTag("3").semanticsId()
        val id5 = rule.onNodeWithTag("5").semanticsId()

        rule.runOnIdle { items.value = listOf() }

        assertNodes(disposed = listOf(0, 2, 4, 6))
        rule.onRoot().fetchSemanticsNode().assertLayoutDeactivatedById(id1)
        rule.onRoot().fetchSemanticsNode().assertLayoutDeactivatedById(id3)
        rule.onRoot().fetchSemanticsNode().assertLayoutDeactivatedById(id5)

        rule.runOnIdle {
            items.value = listOf(8, 9, 10)
            // new slots composed for 8 and 10
            // 5 is reused for 9
        }

        assertNodes(active = listOf(8, 9, 10), disposed = listOf(5))
        rule.onRoot().fetchSemanticsNode().assertLayoutDeactivatedById(id1)
        rule.onRoot().fetchSemanticsNode().assertLayoutDeactivatedById(id3)
    }

    @Test
    fun customPolicy_reusingSecondSlotFromTheEnd() {
        fun isOdd(number: Any?): Boolean {
            return (number as Int) % 2 == 1
        }

        val items = mutableStateOf(listOf(0, 1, 2, 3))
        val policy =
            object : SubcomposeSlotReusePolicy {
                override fun getSlotsToRetain(slotIds: SubcomposeSlotReusePolicy.SlotIdsSet) {}

                override fun areCompatible(slotId: Any?, reusableSlotId: Any?): Boolean {
                    return isOdd(slotId) == isOdd(reusableSlotId)
                }
            }
        val state = SubcomposeLayoutState(policy)

        composeItems(state, items)
        val id0 = rule.onNodeWithTag("0").semanticsId()
        val id1 = rule.onNodeWithTag("1").semanticsId()
        val id2 = rule.onNodeWithTag("2").semanticsId()
        val id3 = rule.onNodeWithTag("3").semanticsId()

        rule.runOnIdle { items.value = listOf() }

        rule.onRoot().fetchSemanticsNode().assertLayoutDeactivatedById(id0)
        rule.onRoot().fetchSemanticsNode().assertLayoutDeactivatedById(id1)
        rule.onRoot().fetchSemanticsNode().assertLayoutDeactivatedById(id2)
        rule.onRoot().fetchSemanticsNode().assertLayoutDeactivatedById(id3)

        rule.runOnIdle {
            items.value = listOf(10) // slot 2 should be reused
        }

        assertNodes(active = listOf(10), disposed = listOf(2))
        rule.onRoot().fetchSemanticsNode().assertLayoutDeactivatedById(id0)
        rule.onRoot().fetchSemanticsNode().assertLayoutDeactivatedById(id1)
        rule.onRoot().fetchSemanticsNode().assertLayoutDeactivatedById(id3)
    }

    @Test
    fun premeasuringAllowsToSkipMeasureOnceTheSlotIsComposed() {
        val state = SubcomposeLayoutState()
        var remeasuresCount = 0
        var relayoutCount = 0
        var subcomposeLayoutRemeasures = 0
        val modifier =
            Modifier.layout { measurable, constraints ->
                    val placeable = measurable.measure(constraints)
                    remeasuresCount++
                    layout(placeable.width, placeable.height) {
                        relayoutCount++
                        placeable.place(0, 0)
                    }
                }
                .fillMaxSize()
        val content = @Composable { Box(modifier) }
        val constraints = Constraints(maxWidth = 100, minWidth = 100)
        var needContent by mutableStateOf(false)

        rule.setContent {
            SubcomposeLayout(state) {
                subcomposeLayoutRemeasures++
                val placeable =
                    if (needContent) {
                        subcompose(Unit, content).first().measure(constraints)
                    } else {
                        null
                    }
                layout(10, 10) { placeable?.place(0, 0) }
            }
        }

        rule.runOnIdle {
            assertThat(remeasuresCount).isEqualTo(0)
            subcomposeLayoutRemeasures = 0
            val handle = state.precompose(Unit, content)

            assertThat(remeasuresCount).isEqualTo(0)
            assertThat(handle.placeablesCount).isEqualTo(1)
            handle.premeasure(0, constraints)

            assertThat(remeasuresCount).isEqualTo(1)
            assertThat(relayoutCount).isEqualTo(0)
            assertThat(subcomposeLayoutRemeasures).isEqualTo(0)
            remeasuresCount = 0

            needContent = true
        }

        rule.runOnIdle {
            assertThat(remeasuresCount).isEqualTo(0)
            assertThat(relayoutCount).isEqualTo(1)
        }
    }

    @Test
    fun premeasuringTwoPlaceables() {
        val state = SubcomposeLayoutState()
        var remeasuresCount = 0
        val modifier =
            Modifier.layout { measurable, constraints ->
                    val placeable = measurable.measure(constraints)
                    remeasuresCount++
                    layout(placeable.width, placeable.height) { placeable.place(0, 0) }
                }
                .fillMaxSize()
        val content =
            @Composable {
                Box(modifier)
                Box(modifier)
            }
        val constraints0 = Constraints(maxWidth = 100, minWidth = 100)
        val constraints1 = Constraints(maxWidth = 200, minWidth = 200)
        var needContent by mutableStateOf(false)

        rule.setContent {
            SubcomposeLayout(state) {
                val placeables =
                    if (needContent) {
                        val measurables = subcompose(Unit, content)
                        assertThat(measurables.size).isEqualTo(2)
                        measurables.mapIndexed { index, measurable ->
                            measurable.measure(if (index == 0) constraints0 else constraints1)
                        }
                    } else {
                        emptyList()
                    }
                layout(10, 10) { placeables.forEach { it.place(0, 0) } }
            }
        }

        rule.runOnIdle {
            assertThat(remeasuresCount).isEqualTo(0)
            val handle = state.precompose(Unit, content)

            assertThat(remeasuresCount).isEqualTo(0)
            assertThat(handle.placeablesCount).isEqualTo(2)
            handle.premeasure(0, constraints0)

            assertThat(remeasuresCount).isEqualTo(1)
            handle.premeasure(1, constraints1)
            assertThat(remeasuresCount).isEqualTo(2)
            remeasuresCount = 0

            needContent = true
        }

        rule.runOnIdle { assertThat(remeasuresCount).isEqualTo(0) }
    }

    @Test
    fun premeasuringTwoPlaceables_allowsQueryingSizeAfter() {
        val state = SubcomposeLayoutState()
        var remeasuresCount = 0
        val modifier =
            Modifier.layout { measurable, constraints ->
                    val placeable = measurable.measure(constraints)
                    remeasuresCount++
                    layout(placeable.width, placeable.height) { placeable.place(0, 0) }
                }
                .fillMaxSize()
        val content =
            @Composable {
                Box(modifier)
                Box(modifier)
            }
        val constraints0 = Constraints(maxWidth = 100, minWidth = 100)
        val constraints1 = Constraints(maxWidth = 200, minWidth = 200)
        var needContent by mutableStateOf(false)

        rule.setContent {
            SubcomposeLayout(state) {
                val placeables =
                    if (needContent) {
                        val measurables = subcompose(Unit, content)
                        assertThat(measurables.size).isEqualTo(2)
                        measurables.mapIndexed { index, measurable ->
                            measurable.measure(if (index == 0) constraints0 else constraints1)
                        }
                    } else {
                        emptyList()
                    }
                layout(10, 10) { placeables.forEach { it.place(0, 0) } }
            }
        }

        rule.runOnIdle {
            assertThat(remeasuresCount).isEqualTo(0)
            val handle = state.precompose(Unit, content)

            assertThat(remeasuresCount).isEqualTo(0)
            assertThat(handle.placeablesCount).isEqualTo(2)

            assertThat(handle.getSize(0)).isEqualTo(IntSize.Zero)
            handle.premeasure(0, constraints0)
            assertThat(handle.getSize(0)).isEqualTo(IntSize(100, 0))

            assertThat(remeasuresCount).isEqualTo(1)
            assertThat(handle.getSize(1)).isEqualTo(IntSize.Zero)
            handle.premeasure(1, constraints1)
            assertThat(handle.getSize(1)).isEqualTo(IntSize(200, 0))
            assertThat(remeasuresCount).isEqualTo(2)
        }
    }

    @Test
    fun premeasuringIncorrectIndexesCrashes() {
        val state = SubcomposeLayoutState()
        val content =
            @Composable {
                Box(Modifier.size(10.dp))
                Box(Modifier.size(10.dp))
            }

        rule.setContent { SubcomposeLayout(state) { layout(10, 10) {} } }

        rule.runOnIdle {
            val handle = state.precompose(Unit, content)

            assertThrows(IndexOutOfBoundsException::class.java) {
                handle.premeasure(-1, Constraints())
            }
            assertThrows(IndexOutOfBoundsException::class.java) {
                handle.premeasure(2, Constraints())
            }
        }
    }

    @Test
    fun getMeasuredSizes_IncorrectIndexesCrashes() {
        val state = SubcomposeLayoutState()
        val content =
            @Composable {
                Box(Modifier.size(10.dp))
                Box(Modifier.size(10.dp))
            }

        rule.setContent { SubcomposeLayout(state) { layout(10, 10) {} } }

        rule.runOnIdle {
            val handle = state.precompose(Unit, content)
            handle.premeasure(0, Constraints())
            handle.premeasure(1, Constraints())

            assertThrows(IndexOutOfBoundsException::class.java) { handle.getSize(-1) }
            assertThrows(IndexOutOfBoundsException::class.java) { handle.getSize(2) }
        }
    }

    @Test
    fun ifSlotWasUsedDuringMeasurePassHandleHasZeroPlaceables() {
        val state = SubcomposeLayoutState()
        val content = @Composable { Box(Modifier.size(10.dp)) }
        var needContent by mutableStateOf(false)

        rule.setContent {
            SubcomposeLayout(state) {
                val placeable =
                    if (needContent) {
                        subcompose(Unit, content).first().measure(Constraints())
                    } else {
                        null
                    }
                layout(10, 10) { placeable?.place(0, 0) }
            }
        }

        lateinit var handle: SubcomposeLayoutState.PrecomposedSlotHandle

        rule.runOnIdle {
            handle = state.precompose(Unit, content)
            handle.premeasure(0, Constraints())
            needContent = true
        }

        rule.runOnIdle {
            assertThat(handle.placeablesCount).isEqualTo(0)
            // we also make sure that calling dispose on such handle is safe
            handle.dispose()
        }
    }

    @Test
    fun stateIsRestoredWhenGoBackToScreen1WithSubcomposition() {
        val restorationTester = StateRestorationTester(rule)

        var increment = 0
        var screen by mutableStateOf(Screens.Screen1)
        var restorableNumberOnScreen1 = -1
        restorationTester.setContent {
            val holder = rememberSaveableStateHolder()
            holder.SaveableStateProvider(screen) {
                if (screen == Screens.Screen1) {
                    SubcomposeLayout {
                        subcompose(Unit) {
                            restorableNumberOnScreen1 = rememberSaveable { increment++ }
                        }
                        layout(10, 10) {}
                    }
                }
            }
        }

        rule.runOnIdle {
            assertThat(restorableNumberOnScreen1).isEqualTo(0)
            screen = Screens.Screen2
        }

        // wait for the screen switch to apply
        rule.runOnIdle {
            restorableNumberOnScreen1 = -1
            // switch back to screen1
            screen = Screens.Screen1
        }

        rule.runOnIdle { assertThat(restorableNumberOnScreen1).isEqualTo(0) }
    }

    @Test
    fun disposeSecondPrecomposedItem() {
        // it is a regression from b/218668336. the assertion was incorrectly checking
        // for the ranges so disposing the second active precomposed node was crashing.
        val state = SubcomposeLayoutState(SubcomposeSlotReusePolicy(0))

        composeItems(state, mutableStateOf(emptyList()))

        rule.runOnIdle {
            state.precompose(0) { ItemContent(0) }
            val handle = state.precompose(1) { ItemContent(1) }
            handle.dispose()
        }

        assertNodes(active = /*prefetch*/ listOf(0), disposed = listOf(1))
    }

    @Test
    fun reusingWithNestedSubcomposeLayoutInside() {
        val slotState = mutableStateOf(0)

        rule.setContent {
            SubcomposeLayout(remember { SubcomposeLayoutState(SubcomposeSlotReusePolicy(1)) }) {
                constraints ->
                val slot = slotState.value
                val child =
                    subcompose(slot) {
                            ReusableContent(slot) {
                                Box {
                                    SubcomposeLayout(Modifier.testTag("$slot")) { constraints ->
                                        val placeable =
                                            subcompose(0) { Box(modifier = Modifier.size(10.dp)) }
                                                .first()
                                                .measure(constraints)
                                        layout(placeable.width, placeable.height) {
                                            placeable.place(0, 0)
                                        }
                                    }
                                }
                            }
                        }
                        .first()
                        .measure(constraints)
                layout(child.width, child.height) { child.place(0, 0) }
            }
        }

        rule.runOnIdle { slotState.value = 1 }

        rule.runOnIdle { slotState.value = 2 }

        rule.onNodeWithTag("2").assertIsDisplayed()
        rule.onNodeWithTag("1").assertIsNotDisplayed()
        rule.onNodeWithTag("0").assertDoesNotExist()
    }

    @Test
    fun disposingPrecomposedItemInTheNestedSubcomposeLayout() {
        var needSlot by mutableStateOf(true)
        val state = SubcomposeLayoutState(SubcomposeSlotReusePolicy(1))

        rule.setContent {
            SubcomposeLayout(remember { SubcomposeLayoutState(SubcomposeSlotReusePolicy(1)) }) {
                constraints ->
                val child =
                    if (needSlot) {
                        subcompose(0) {
                                Box {
                                    SubcomposeLayout(state = state, Modifier.testTag("0")) {
                                        constraints ->
                                        if (needSlot) {
                                            val placeable =
                                                subcompose(0) {
                                                        Box(modifier = Modifier.size(10.dp))
                                                    }
                                                    .first()
                                                    .measure(constraints)
                                            layout(placeable.width, placeable.height) {
                                                placeable.place(0, 0)
                                            }
                                        } else {
                                            layout(100, 100) {}
                                        }
                                    }
                                }
                            }
                            .first()
                            .measure(constraints)
                    } else {
                        null
                    }
                layout(100, 100) { child?.place(0, 0) }
            }
        }

        var precomposedSlotActive = false

        val handle =
            rule.runOnIdle {
                state.precompose(1) {
                    Box(modifier = Modifier.size(10.dp).testTag("1"))

                    DisposableEffect(Unit) {
                        precomposedSlotActive = true
                        onDispose { precomposedSlotActive = false }
                    }
                }
            }

        rule.runOnIdle {
            assertThat(precomposedSlotActive).isTrue()
            needSlot = false
        }

        rule.runOnIdle { handle.dispose() }

        assertThat(precomposedSlotActive).isFalse()

        // Both slots inside subcompose are reused, as parent was detached with these nodes active
        rule.onNodeWithTag("1").assertIsNotDisplayed()
        rule.onNodeWithTag("0").assertIsNotDisplayed()
    }

    @Test
    fun getAlignmentsOnSubcomposeLayoutContent() {
        var baseline: Int? = null
        rule.setContent {
            Layout({
                SubcomposeLayout { constraints ->
                    val placeable =
                        subcompose("0") {
                                Layout(content = {}) { _, _ ->
                                    layout(10, 10, mapOf(FirstBaseline to 100)) {}
                                }
                            }
                            .first()
                            .measure(constraints)
                    layout(placeable.width, placeable.height) { placeable.place(0, 0) }
                }
            }) { measurables, constraints ->
                val titlePlaceable = measurables.first().measure(constraints.copy(minHeight = 0))
                baseline = titlePlaceable[FirstBaseline]
                layout(titlePlaceable.width, titlePlaceable.height) { titlePlaceable.place(0, 0) }
            }
        }

        rule.runOnIdle { assertThat(baseline).isEqualTo(100) }
    }

    @Test
    fun noNotMeasuredWrappersInTheHierarchy() {
        lateinit var coordinates: LayoutCoordinates
        var size: IntSize? = null
        rule.setContent {
            Box {
                SubcomposeLayout { constraints ->
                    val placeable =
                        subcompose("0") {
                                Box(
                                    Modifier.fillMaxSize()
                                        .onGloballyPositioned { coordinates = it }
                                        .onSizeChanged { size = it }
                                )
                            }
                            .first()
                            .measure(constraints)
                    layout(placeable.width, placeable.height) { placeable.place(0, 0) }
                }
            }
        }

        rule.runOnIdle {
            var current: LayoutCoordinates? = coordinates
            while (current != null) {
                assertThat(current.isAttached)
                assertThat(current.size).isEqualTo(size)
                current = current.parentCoordinates
            }
        }
    }

    @Test
    fun subcomposeLayoutInsideMovableContent_compositionIsNotDisposed() {
        var disposeCount = 0

        val content = movableContentOf {
            SubcomposeLayout(
                state = remember { SubcomposeLayoutState(SubcomposeSlotReusePolicy(1)) }
            ) { constraints ->
                val placeable =
                    subcompose(0) {
                            Box(Modifier.testTag("0"))

                            DisposableEffect(Unit) { onDispose { disposeCount++ } }
                        }
                        .single()
                        .measure(constraints)

                layout(placeable.width, placeable.height) { placeable.place(IntOffset.Zero) }
            }
        }

        var wrappedWithColumn by mutableStateOf(false)

        rule.setContent {
            if (wrappedWithColumn) {
                Column { content() }
            } else {
                content()
            }
        }

        rule.onNodeWithTag("0").assertExists()

        rule.runOnIdle { wrappedWithColumn = true }

        rule.onNodeWithTag("0").assertExists()
        assertThat(disposeCount).isEqualTo(0)
    }

    @Test
    fun subcomposeLayout_movedToDifferentGroup() {
        var wrapped by mutableStateOf(false)
        rule.setContent {
            val content = remember {
                movableContentOf {
                    BoxWithConstraints { Spacer(modifier = Modifier.testTag(wrapped.toString())) }
                }
            }

            if (wrapped) {
                Box { content() }
            } else {
                content()
            }
        }

        rule.runOnIdle { wrapped = !wrapped }

        rule.waitForIdle()
    }

    @Test
    @Ignore("b/188320755")
    fun forceMeasureOfInactiveElementFromLaunchedEffect() {
        var isActive by mutableStateOf(true)
        var forceMeasureFromLaunchedEffect by mutableStateOf(false)

        var remeasurer: Remeasurement? = null
        rule.setContent {
            SubcomposeLayout(
                modifier =
                    Modifier.then(
                        object : RemeasurementModifier {
                            override fun onRemeasurementAvailable(remeasurement: Remeasurement) {
                                remeasurer = remeasurement
                            }
                        }
                    ),
                state = remember { SubcomposeLayoutState(SubcomposeSlotReusePolicy(1)) },
            ) { constraints ->
                val placeable =
                    if (isActive) {
                        val measureables = subcompose(null) { Box(Modifier) }
                        measureables.map { it.measure(constraints) }
                    } else {
                        forceMeasureFromLaunchedEffect = true
                        emptyList()
                    }
                layout(0, 0) { placeable.forEach { it.place(0, 0) } }
            }

            if (forceMeasureFromLaunchedEffect) {
                LaunchedEffect(Unit) {
                    isActive = true
                    remeasurer?.forceRemeasure()
                }
            }
        }

        rule.runOnIdle { isActive = false }

        rule.waitUntil { isActive }
    }

    @Test
    fun composingTheSameKeyTwiceIsNotAllowed() {
        var error: Exception? = null
        rule.setContent {
            SubcomposeLayout { _ ->
                subcompose(0) {}
                try {
                    subcompose(0) {}
                } catch (e: Exception) {
                    error = e
                }
                layout(100, 100) {}
            }
        }
        assertThat(error).isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun notSubcomposedAnymoreNodesAreNotRecomposed() {
        var flag by mutableStateOf(true)
        var updatedInMeasureFlag by mutableStateOf(true)
        val state = SubcomposeLayoutState(SubcomposeSlotReusePolicy(1))
        rule.setContent {
            SubcomposeLayout(state) {
                updatedInMeasureFlag = flag
                Snapshot.sendApplyNotifications()
                if (flag) {
                    subcompose(0) {
                        if (updatedInMeasureFlag) {
                            Box(Modifier.testTag("tag"))
                        }
                    }
                }
                layout(100, 100) {}
            }
        }

        val idTag = rule.onNodeWithTag("tag").semanticsId()

        rule.runOnIdle { flag = false }

        // the node will exist when after `flag` was switched to false it will first cause
        // remeasure, and because during the remeasure we will not subcompose the child
        // the node will be deactivated before its block recomposes causing the Box to be
        // removed from the hierarchy.
        rule.onRoot().fetchSemanticsNode().assertLayoutDeactivatedById(idTag)
    }

    // Regression test of b/271156218
    @Test
    fun deactivatingDeeplyNestedAndroidViewDoesNotCauseRemeasure() {
        var showContent by mutableStateOf(true)
        val state = SubcomposeLayoutState(SubcomposeSlotReusePolicy(1))
        rule.setContent {
            SubcomposeLayout(state = state, modifier = Modifier.fillMaxSize()) { constraints ->
                val content =
                    if (showContent) {
                        subcompose(0) {
                            Box {
                                AndroidView(::View, Modifier.fillMaxSize().testTag("AndroidView"))
                            }
                        }
                    } else emptyList()

                val placeables = measure(content, constraints)
                layout(100, 100) { placeables.placeChildren() }
            }
        }

        rule.onNodeWithTag("AndroidView").assertExists()

        rule.runOnIdle { showContent = false }
        rule.onNodeWithTag("AndroidView").assertIsNotDisplayed()

        rule.runOnIdle { showContent = true }
        rule.onNodeWithTag("AndroidView").assertExists()
    }

    @Test
    fun deactivatingDeeplyNestedLayoutDoesNotCauseRemeasure() {
        var showContent by mutableStateOf(true)
        val state = SubcomposeLayoutState(SubcomposeSlotReusePolicy(1))
        rule.setContent {
            SubcomposeLayout(state = state, modifier = Modifier.fillMaxSize()) { constraints ->
                val content =
                    if (showContent) {
                        subcompose(0) {
                            Box {
                                var disposed by remember { mutableStateOf(false) }
                                DisposableEffect(Unit) { onDispose { disposed = true } }
                                Box(
                                    Modifier.layout { measurable, constraints ->
                                        assertThat(disposed).isFalse()
                                        val placeable = measurable.measure(constraints)
                                        layout(placeable.width, placeable.height) {
                                            placeable.place(0, 0)
                                        }
                                    }
                                )
                            }
                        }
                    } else emptyList()

                val placeables = measure(content, constraints)
                layout(100, 100) { placeables.placeChildren() }
            }
        }

        rule.runOnIdle { showContent = false }
        rule.runOnIdle { showContent = true }
        rule.waitForIdle()
    }

    @Test
    fun reusingNestedSubcompose_nestedChildrenAreResetAndReused() {
        val slotState = mutableStateOf(0)

        val activeChildren = mutableSetOf<Int>()
        var remeasureCount = 0
        val measureCountModifier =
            Modifier.layout { measurable, constraints ->
                remeasureCount++
                val placeable = measurable.measure(constraints)
                layout(placeable.width, placeable.height) { placeable.place(0, 0) }
            }

        rule.setContent {
            SubcomposeLayout(remember { SubcomposeLayoutState(SubcomposeSlotReusePolicy(1)) }) {
                constraints ->
                val slot = slotState.value
                val child =
                    measure(slot, constraints) {
                        Box {
                            SubcomposeLayout { constraints ->
                                val placeable =
                                    measure(Unit, constraints) {
                                        Box(
                                            modifier =
                                                Modifier.size(10.dp).then(measureCountModifier)
                                        )

                                        DisposableEffect(Unit) {
                                            activeChildren += slot
                                            onDispose { activeChildren -= slot }
                                        }
                                    }
                                layout(placeable.width, placeable.height) { placeable.place(0, 0) }
                            }
                        }
                    }
                layout(child.width, child.height) { child.place(0, 0) }
            }
        }

        rule.runOnIdle {
            assertThat(activeChildren).containsExactly(0)

            slotState.value = 1
        }

        rule.runOnIdle {
            assertThat(activeChildren).containsExactly(1)
            assertThat(remeasureCount).isEqualTo(2)

            remeasureCount = 0

            slotState.value = 2
        }

        rule.runOnIdle {
            assertThat(activeChildren).containsExactly(2)
            assertThat(remeasureCount).isEqualTo(0)
        }
    }

    @Test
    fun reusingNestedSubcompose_nestedContentIsResetWhenReusedOnNextFrame() {
        var contentActive by mutableStateOf(true)
        var slotId by mutableStateOf(0)
        val activeChildren = mutableSetOf<Int>()
        var remeasureCount = 0
        val measureCountModifier =
            Modifier.layout { measurable, constraints ->
                remeasureCount++
                val placeable = measurable.measure(constraints)
                layout(placeable.width, placeable.height) { placeable.place(0, 0) }
            }

        rule.setContent {
            SubcomposeLayout(remember { SubcomposeLayoutState(SubcomposeSlotReusePolicy(1)) }) {
                constraints ->
                if (contentActive) {
                    val child =
                        measure(slotId, constraints) {
                            Box {
                                SubcomposeLayout { constraints ->
                                    val placeable =
                                        measure(Unit, constraints) {
                                            Box(
                                                modifier =
                                                    Modifier.size(10.dp).then(measureCountModifier)
                                            )

                                            DisposableEffect(Unit) {
                                                val capturedSlotId = slotId
                                                activeChildren += slotId
                                                onDispose { activeChildren -= capturedSlotId }
                                            }
                                        }
                                    layout(placeable.width, placeable.height) {
                                        placeable.place(0, 0)
                                    }
                                }
                            }

                            DisposableEffect(Unit) {
                                onDispose {
                                    // schedule remeasure / compose when child is reset
                                    contentActive = true
                                    slotId++
                                }
                            }
                        }
                    layout(child.width, child.height) { child.place(0, 0) }
                } else {
                    layout(0, 0) {}
                }
            }
        }

        rule.runOnIdle {
            assertThat(activeChildren).containsExactly(0)

            contentActive = false
        }

        rule.runOnIdle { assertThat(activeChildren).containsExactly(1) }
    }

    @Test
    fun slotIsProperlyDeactivatedAfterUpdatingReusePolicy() {
        var state by mutableStateOf(SubcomposeLayoutState(SubcomposeSlotReusePolicy(1)))
        var shouldCompose by mutableStateOf(true)
        var disposed = false
        rule.setContent {
            SubcomposeLayout(state) { constraints ->
                val placeables =
                    if (shouldCompose) {
                        subcompose(Unit) {
                                DisposableEffect(Unit) { onDispose { disposed = true } }
                            }
                            .map { it.measure(constraints) }
                    } else {
                        emptyList()
                    }
                layout(100, 100) { placeables.forEach { it.place(0, 0) } }
            }
        }

        rule.runOnIdle {
            state = SubcomposeLayoutState(SubcomposeSlotReusePolicy(1))
            shouldCompose = false
        }

        rule.runOnIdle { assertThat(disposed).isTrue() }
    }

    @Test
    fun subcomposeCoordinates() {
        var coords: LayoutCoordinates? = null
        rule.setContent {
            SubcomposeLayout { constraints ->
                layout(constraints.maxWidth, constraints.maxHeight) { coords = coordinates }
            }
        }
        rule.runOnIdle { assertThat(coords).isNotNull() }
    }

    @Test
    fun updatingModifierOnDetachedSubcomposition() {
        var active by mutableStateOf(true)

        var modifier by mutableStateOf<Modifier>(Modifier)

        rule.setContent {
            ReusableContentHost(active) {
                SubcomposeLayout { constraints ->
                    val placeable =
                        subcompose(Unit) {
                                Layout(modifier = modifier) { _, _ -> layout(10, 10) {} }
                            }
                            .first()
                            .measure(constraints)
                    layout(placeable.width, placeable.height) { placeable.place(0, 0) }
                }
            }
        }

        rule.runOnIdle {
            active = false
            modifier = Modifier.drawBehind {}
        }

        rule.runOnIdle { active = true }

        rule.runOnIdle {
            // makes sure there will be no runtime crash
        }
    }

    @Test
    fun precomposeOnDetachedStateIsNoOp() {
        var needSubcomposeLayout by mutableStateOf(true)
        val state = SubcomposeLayoutState(SubcomposeSlotReusePolicy(1))
        rule.setContent {
            if (needSubcomposeLayout) {
                SubcomposeLayout(state) { _ -> layout(10, 10) {} }
            }
        }

        rule.runOnIdle { needSubcomposeLayout = false }

        rule.runOnIdle {
            val handle = state.precompose(Unit) { Box(Modifier) }
            assertThat(handle.placeablesCount).isEqualTo(0)
        }
    }

    @Test
    fun deactivatedNodesAreNotPartOfChildrenSemantics() {
        val layoutState = mutableStateOf(SubcomposeLayoutState(SubcomposeSlotReusePolicy(1)))
        val needChild = mutableStateOf(true)

        rule.setContent {
            SubcomposeLayout(modifier = Modifier.testTag("layout"), state = layoutState.value) {
                constraints ->
                val node =
                    if (needChild.value) {
                        subcompose(Unit) { Box(Modifier.testTag("child")) }
                            .first()
                            .measure(constraints)
                    } else {
                        null
                    }
                layout(10, 10) { node?.place(0, 0) }
            }
        }

        rule.onNodeWithTag("layout").onChildren().assertCountEquals(1)

        needChild.value = false

        rule.onNodeWithTag("layout").onChildren().assertCountEquals(0)
    }

    @Test
    fun measureWidthTooLarge() {
        var exception: IllegalStateException? = null
        rule.setContent {
            SubcomposeLayout {
                try {
                    layout(1 shl 24, 100) {}
                } catch (e: IllegalStateException) {
                    exception = e
                    layout(0, 0) {}
                }
            }
        }
        rule.waitForIdle()
        assertThat(exception).isNotNull()
    }

    @Test
    fun measureHeightTooLarge() {
        var exception: IllegalStateException? = null
        rule.setContent {
            SubcomposeLayout {
                try {
                    layout(100, 1 shl 24) {}
                } catch (e: IllegalStateException) {
                    exception = e
                    layout(0, 0) {}
                }
            }
        }
        rule.waitForIdle()
        assertThat(exception).isNotNull()
    }

    @Test
    fun nestedDisposeIsCalledInOrder() {
        val disposeOrder = mutableListOf<String>()
        var active by mutableStateOf(true)
        rule.setContent {
            if (active) {
                BoxWithConstraints {
                    BoxWithConstraints {
                        DisposableEffect(Unit) { onDispose { disposeOrder += "inner 1" } }
                    }

                    DisposableEffect(Unit) { onDispose { disposeOrder += "outer" } }

                    BoxWithConstraints {
                        DisposableEffect(Unit) { onDispose { disposeOrder += "inner 2" } }
                    }
                }
            }
        }

        rule.runOnIdle { active = false }

        rule.runOnIdle { assertThat(disposeOrder).isExactly("inner 2", "outer", "inner 1") }
    }

    @SdkSuppress(
        excludedSdks =
            [
                // API 28 is using ViewLayer which invalidates when layer is created
                Build.VERSION_CODES.P,
                // waitForIdle doesn't wait for draw on API 26 (b/372068529)
                Build.VERSION_CODES.O,
            ]
    )
    @Test
    fun precomposeAndPremeasureAreNotCausingViewInvalidations() {
        val state = SubcomposeLayoutState()

        var drawingCount = 0

        rule.setContent {
            val view = LocalView.current
            DisposableEffect(view) {
                val listener = ViewTreeObserver.OnDrawListener { drawingCount++ }
                view.viewTreeObserver.addOnDrawListener(listener)
                onDispose { view.viewTreeObserver.removeOnDrawListener(listener) }
            }
            SubcomposeLayout(state) { layout(10, 10) {} }
        }

        rule.runOnIdle {
            drawingCount = 0

            val handle = state.precompose(Unit) { Box(Modifier.graphicsLayer().size(10.dp)) }
            handle.premeasure(0, Constraints())
        }

        rule.runOnIdle { assertThat(drawingCount).isEqualTo(0) }
    }

    @Test
    fun placeChildrenWithoutFirstPlacingThemInLookahead() {
        var lookaheadPos: Offset? = null
        var approachPos: Offset? = null
        rule.setContent {
            LookaheadScope {
                CompositionLocalProvider(LocalDensity provides Density(1f)) {
                    SubcomposeLayout(Modifier.size(200.dp, 600.dp)) {
                        val m1 = subcompose(1) { Box(Modifier.size(200.dp)) }
                        val m2 =
                            subcompose(2) {
                                // This box's placement is skipped in lookahead
                                Box(
                                    Modifier.passThroughLayout { placementScope ->
                                            with(placementScope) {
                                                val pos = coordinates?.positionInParent()
                                                if (isLookingAhead) {
                                                    lookaheadPos = pos ?: lookaheadPos
                                                } else {
                                                    approachPos = pos ?: lookaheadPos
                                                }
                                            }
                                        }
                                        .size(200.dp)
                                )
                            }
                        val p1 = m1[0].measure(it)
                        val p2 = m2[0].measure(it)
                        layout(200, 400) {
                            if (isLookingAhead) {
                                p1.place(0, 0)
                            } else {
                                p1.place(0, 0)
                                p2.place(0, 200)
                            }
                        }
                    }
                }
            }
        }
        rule.runOnIdle {
            assertEquals(Offset(0f, 200f), lookaheadPos)
            assertEquals(Offset(0f, 200f), approachPos)
        }
    }

    @Test
    fun placeChildrenWithMFRWithoutFirstPlacingThemInLookahead() {
        // Check that place with motion frame of reference is picked up by lookahead
        var lookaheadPos: Offset? = null
        var approachPos: Offset? = null
        var lookaheadPosExcludeMFR: Offset? = null
        var approachPosExcludeMFR: Offset? = null
        rule.setContent {
            LookaheadScope {
                CompositionLocalProvider(LocalDensity provides Density(1f)) {
                    SubcomposeLayout(Modifier.size(200.dp, 600.dp)) {
                        val m1 = subcompose(1) { Box(Modifier.size(200.dp)) }
                        val m2 =
                            subcompose(2) {
                                // This box's placement is skipped in lookahead
                                Box(
                                    Modifier.passThroughLayout { placementScope ->
                                            with(placementScope) {
                                                val pos = coordinates?.positionInParent()
                                                val posExcludeMFR =
                                                    coordinates
                                                        ?.parentCoordinates
                                                        ?.localPositionOf(
                                                            coordinates!!,
                                                            includeMotionFrameOfReference = false,
                                                        )
                                                if (isLookingAhead) {
                                                    lookaheadPos = pos ?: lookaheadPos
                                                    lookaheadPosExcludeMFR = posExcludeMFR
                                                } else {
                                                    approachPos = pos ?: lookaheadPos
                                                    approachPosExcludeMFR = posExcludeMFR
                                                }
                                            }
                                        }
                                        .size(200.dp)
                                )
                            }
                        val p1 = m1[0].measure(it)
                        val p2 = m2[0].measure(it)
                        layout(200, 400) {
                            if (isLookingAhead) {
                                p1.place(0, 0)
                            } else {
                                p1.place(0, 0)
                                withMotionFrameOfReferencePlacement { p2.place(0, 200) }
                            }
                        }
                    }
                }
            }
        }
        rule.runOnIdle {
            assertEquals(Offset(0f, 200f), lookaheadPos)
            assertEquals(Offset(0f, 200f), approachPos)
            assertEquals(Offset.Zero, lookaheadPosExcludeMFR)
            assertEquals(Offset.Zero, approachPosExcludeMFR)
        }
    }

    @Test
    fun addContentToItemDetachedFromLookaheadPlacement() {
        // Add content to the child of subcomposeLayout that has intentionally skipped lookahead
        // placement, and make sure that the new content gets accounted for in both
        // lookahead and approach.
        var lookaheadSize: IntSize? = null
        var approachSize: IntSize? = null
        var itemCount by mutableStateOf(1)
        var lookaheadPos: Array<Offset?> = arrayOfNulls(6)
        var approachPos: Array<Offset?> = arrayOfNulls(6)
        rule.setContent {
            SubcomposeLayoutWithItemDetachedFromLookaheadPlacement {
                // The content that is detached from LookaheadPlacement
                Column(
                    Modifier.wrapContentSize().layout { m, c ->
                        m.measure(c).run {
                            if (isLookingAhead) {
                                lookaheadSize = IntSize(width, height)
                            } else {
                                approachSize = IntSize(width, height)
                            }
                            layout(width, height) { place(0, 0) }
                        }
                    }
                ) {
                    repeat(itemCount) { id ->
                        Box(
                            Modifier.passThroughLayout {
                                    with(it) {
                                        if (isLookingAhead) {
                                            lookaheadPos[id] = coordinates?.positionInParent()
                                        } else {
                                            approachPos[id] = coordinates?.positionInParent()
                                        }
                                    }
                                }
                                .size(100.dp, 100.dp)
                        )
                    }
                }
            }
        }

        repeat(5) {
            rule.waitForIdle()
            assertEquals(IntSize(100, 100 * itemCount), lookaheadSize)
            assertEquals(IntSize(100, 100 * itemCount), approachSize)
            repeat(itemCount) {
                assertEquals(Offset(0f, it * 100f), lookaheadPos[it])
                assertEquals(Offset(0f, it * 100f), approachPos[it])
            }
            itemCount = it + 1
        }
    }

    /* Add content to the child of subcomposeLayout that is nested in another
     * SubcomposeLayout that has intentionally skipped lookahead
     * placement, and make sure that the new content gets accounted for in both
     * lookahead and approach.
     */
    @Test
    fun addContentToNestedItemDetachedFromLookaheadPlacement() {
        var lookaheadSize: IntSize? = null
        var approachSize: IntSize? = null
        var itemCount by mutableStateOf(1)
        var lookaheadPos: Array<Offset?> = arrayOfNulls(6)
        var approachPos: Array<Offset?> = arrayOfNulls(6)
        rule.setContent {
            SubcomposeLayoutWithItemDetachedFromLookaheadPlacement {
                SubcomposeLayoutWithItemDetachedFromLookaheadPlacement {
                    // The content that is detached from LookaheadPlacement
                    Column(
                        Modifier.wrapContentSize().layout { m, c ->
                            m.measure(c).run {
                                if (isLookingAhead) {
                                    lookaheadSize = IntSize(width, height)
                                } else {
                                    approachSize = IntSize(width, height)
                                }
                                layout(width, height) { place(0, 0) }
                            }
                        }
                    ) {
                        repeat(itemCount) { id ->
                            Box(
                                Modifier.passThroughLayout {
                                        with(it) {
                                            if (isLookingAhead) {
                                                lookaheadPos[id] = coordinates?.positionInParent()
                                            } else {
                                                approachPos[id] = coordinates?.positionInParent()
                                            }
                                        }
                                    }
                                    .size(100.dp, 100.dp)
                            )
                        }
                    }
                }
            }
        }

        repeat(5) {
            rule.waitForIdle()
            assertEquals(IntSize(100, 100 * itemCount), lookaheadSize)
            assertEquals(IntSize(100, 100 * itemCount), approachSize)
            repeat(itemCount) {
                assertEquals(Offset(0f, it * 100f), lookaheadPos[it])
                assertEquals(Offset(0f, it * 100f), approachPos[it])
            }
            itemCount = it + 1
        }
    }

    @Test
    fun changePositionOfItemDetachedFromLookaheadPlacement() {
        var lookaheadPos: Offset? = null
        var approachPos: Offset? = null
        var itemPos: IntOffset by mutableStateOf(IntOffset(0, 200))
        rule.setContent {
            LookaheadScope {
                CompositionLocalProvider(LocalDensity provides Density(1f)) {
                    SubcomposeLayout(Modifier.size(200.dp, 600.dp)) {
                        val m1 = subcompose(1) { Box(Modifier.size(200.dp)) }
                        val m2 =
                            subcompose(2) {
                                // This box's placement is skipped in lookahead
                                Box(
                                    Modifier.passThroughLayout { placementScope ->
                                            with(placementScope) {
                                                val pos = coordinates?.positionInParent()
                                                if (isLookingAhead) {
                                                    lookaheadPos = pos ?: lookaheadPos
                                                } else {
                                                    approachPos = pos ?: lookaheadPos
                                                }
                                            }
                                        }
                                        .size(200.dp)
                                )
                            }
                        val p1 = m1[0].measure(it)
                        val p2 = m2[0].measure(it)
                        layout(200, 400) {
                            if (isLookingAhead) {
                                p1.place(0, 0)
                            } else {
                                p1.place(0, 0)
                                p2.place(itemPos.x, itemPos.y)
                            }
                        }
                    }
                }
            }
        }

        rule.waitForIdle()
        assertEquals(itemPos, lookaheadPos?.round())
        assertEquals(itemPos, approachPos?.round())

        itemPos = IntOffset(70, 50)
        rule.waitForIdle()
        assertEquals(itemPos, lookaheadPos?.round())
        assertEquals(itemPos, approachPos?.round())
    }

    @Test
    fun changeLookaheadPositionOfContentDetachedFromParentLookaheadPlacement() {
        var lookaheadOffset by mutableStateOf(IntOffset(50, 20))
        var lookaheadOffsetFromParent: Offset? = null
        var approachOffsetFromParent: Offset? = null
        rule.setContent {
            SubcomposeLayoutWithItemDetachedFromLookaheadPlacement {
                Box(
                    Modifier.size(100.dp)
                        .layout { m, c ->
                            m.measure(c).run {
                                layout(width, height) {
                                    if (isLookingAhead) {
                                        place(lookaheadOffset.x, lookaheadOffset.y)
                                    } else {
                                        place(100, 0)
                                    }
                                }
                            }
                        }
                        .passThroughLayout {
                            with(it) {
                                val offset =
                                    coordinates
                                        ?.parentCoordinates
                                        ?.parentCoordinates
                                        ?.localPositionOf(coordinates!!)
                                if (isLookingAhead) {
                                    lookaheadOffsetFromParent = offset
                                } else {
                                    approachOffsetFromParent = offset
                                }
                            }
                        }
                )
            }
        }
        rule.waitForIdle()
        assertEquals(lookaheadOffset, lookaheadOffsetFromParent?.round())
        assertEquals(IntOffset(100, 0), approachOffsetFromParent?.round())

        lookaheadOffset = IntOffset(23, 45)
        rule.waitForIdle()
        assertEquals(lookaheadOffset, lookaheadOffsetFromParent?.round())
        assertEquals(IntOffset(100, 0), approachOffsetFromParent?.round())

        lookaheadOffset = IntOffset(65, 432)
        rule.waitForIdle()
        assertEquals(lookaheadOffset, lookaheadOffsetFromParent?.round())
        assertEquals(IntOffset(100, 0), approachOffsetFromParent?.round())
    }

    @Composable
    private fun SubcomposeLayoutWithItemDetachedFromLookaheadPlacement(
        detachedFromParentLookaheadPlacmeentItemContent: @Composable () -> Unit
    ) {
        LookaheadScope {
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                SubcomposeLayout(Modifier.size(200.dp, 600.dp)) {
                    val m1 = subcompose(1) { Box(Modifier.size(200.dp)) }
                    val m2 =
                        subcompose(2) {
                            // This box's placement is skipped in lookahead
                            detachedFromParentLookaheadPlacmeentItemContent()
                        }
                    val p1 = m1[0].measure(it)
                    val p2 = m2[0].measure(it)
                    layout(200, 400) {
                        if (isLookingAhead) {
                            p1.place(0, 0)
                        } else {
                            p1.place(0, 0)
                            p2.place(0, 200)
                        }
                    }
                }
            }
        }
    }

    @Test
    fun subcomposePlacementFromPlacedToNotPlaced() {
        alternateLookaheadPlacement(booleanArrayOf(true, false, true))
    }

    @Test
    fun subcomposePlacementFromNotPlacedToPlaced() {
        alternateLookaheadPlacement(booleanArrayOf(false, true, false))
    }

    @Test
    fun precomposeOverReusedNodeWithUpdatedModifierIsNotCausingEarlyRemeasureForIt() {
        var addSlot by mutableStateOf(true)
        val state = SubcomposeLayoutState(SubcomposeSlotReusePolicy(1))
        var measured = 0
        var placed = 0
        var modifier: Modifier by mutableStateOf(Modifier)
        val content: @Composable () -> Unit = { Box { Box(modifier) } }
        var lastConstraints = Constraints()

        rule.setContent {
            SubcomposeLayout(state) { constraints ->
                lastConstraints = constraints
                val items =
                    if (addSlot) {
                        subcompose(Unit, content).map { it.measure(constraints) }
                    } else {
                        emptyList()
                    }
                layout(10, 10) { items.forEach { it.place(0, 0) } }
            }
        }

        rule.runOnIdle { addSlot = false }

        val handle =
            rule.runOnIdle {
                modifier =
                    Modifier.layout { measurable, _ ->
                        val placeable = measurable.measure(Constraints.fixed(10, 10))
                        measured++
                        layout(placeable.width, placeable.height) {
                            placeable.place(0, 0)
                            placed++
                        }
                    }
                state.precompose(Unit, content)
            }

        rule.runOnIdle {
            assertThat(measured).isEqualTo(0)
            assertThat(placed).isEqualTo(0)
            handle.premeasure(0, lastConstraints)
        }

        rule.runOnIdle {
            assertThat(measured).isEqualTo(1)
            assertThat(placed).isEqualTo(0)
            addSlot = true
        }

        rule.runOnIdle {
            assertThat(measured).isEqualTo(1)
            assertThat(placed).isEqualTo(1)
        }
    }

    @Test
    fun precomposeOverReusedNodeWithUpdatedModifierIsNotCausingEarlyRemeasureForIt2() {
        var addSlot by mutableStateOf(true)
        val state = SubcomposeLayoutState(SubcomposeSlotReusePolicy(1))
        var measured = 0
        var placed = 0
        val onMeasured: () -> Unit = { measured++ }
        val onPlaced: () -> Unit = { placed++ }
        var modifier: Modifier by
            mutableStateOf(RemeasureAndRelayoutOnChangeModifierElement(onMeasured, onPlaced, 0))
        val content: @Composable () -> Unit = { Box { Box(modifier) } }
        var lastConstraints = Constraints()

        rule.setContent {
            SubcomposeLayout(state) { constraints ->
                lastConstraints = constraints
                val items =
                    if (addSlot) {
                        subcompose(Unit, content).map { it.measure(constraints) }
                    } else {
                        emptyList()
                    }
                layout(10, 10) { items.forEach { it.place(0, 0) } }
            }
        }

        rule.runOnIdle { addSlot = false }

        val handle =
            rule.runOnIdle {
                measured = 0
                placed = 0
                modifier = RemeasureAndRelayoutOnChangeModifierElement(onMeasured, onPlaced, 1)
                state.precompose(Unit, content)
            }

        rule.runOnIdle {
            assertThat(measured).isEqualTo(0)
            assertThat(placed).isEqualTo(0)
            handle.premeasure(0, lastConstraints)
        }

        rule.runOnIdle {
            assertThat(measured).isEqualTo(1)
            assertThat(placed).isEqualTo(0)
            addSlot = true
        }

        rule.runOnIdle {
            assertThat(measured).isEqualTo(1)
            assertThat(placed).isEqualTo(1)
            addSlot = true
        }
    }

    private class RemeasureAndRelayoutOnChangeModifierElement(
        val onMeasured: () -> Unit,
        val onPlaced: () -> Unit,
        val identity: Int,
    ) : ModifierNodeElement<RemeasureAndRelayoutOnChangeModifier>() {
        override fun create(): RemeasureAndRelayoutOnChangeModifier =
            RemeasureAndRelayoutOnChangeModifier(onMeasured, onPlaced)

        override fun update(node: RemeasureAndRelayoutOnChangeModifier) {
            node.onMeasured = onMeasured
            node.onPlaced = onPlaced
            node.invalidateMeasurement()
            node.invalidatePlacement()
        }

        override fun hashCode(): Int = identity

        override fun equals(other: Any?) =
            other is RemeasureAndRelayoutOnChangeModifierElement && other.identity == identity
    }

    private class RemeasureAndRelayoutOnChangeModifier(
        var onMeasured: () -> Unit,
        var onPlaced: () -> Unit,
    ) : Modifier.Node(), LayoutModifierNode {
        override fun MeasureScope.measure(
            measurable: Measurable,
            constraints: Constraints,
        ): MeasureResult {
            val placeable = measurable.measure(constraints)
            onMeasured()
            return layout(placeable.width, placeable.height) {
                onPlaced()
                placeable.place(0, 0)
            }
        }

        override val shouldAutoInvalidate: Boolean
            get() = false
    }

    @Test
    // regression test for b/382042245
    fun remeasureRequestDuringSubcompositionIsNotSkippedForNotPlacedChild() {
        var size by mutableStateOf(100)

        rule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                SubcomposeLayout(Modifier.testTag("node")) { constraints ->
                    // for a child to not read state directly, instead we have a new lambda
                    val childSize = size
                    val measurable =
                        subcompose(Unit) {
                            Box {
                                Box(
                                    Modifier.layout { measurable, _ ->
                                        val placeable =
                                            measurable.measure(
                                                Constraints.fixed(childSize, childSize)
                                            )
                                        layout(placeable.width, placeable.height) {
                                            placeable.place(0, 0)
                                        }
                                    }
                                )
                            }
                        }
                    val fab = measurable.first().measure(constraints)
                    layout(fab.width, fab.height) {}
                }
            }
        }

        rule.runOnIdle { size = 150 }

        rule.onNodeWithTag("node").assertWidthIsEqualTo(150.dp)
    }

    @Test
    fun precomposePaused_composeAndApply() {
        val addSlot = mutableStateOf(false)
        var composingCounter = 0
        var applyCounter = 0
        val state = SubcomposeLayoutState()
        val content: @Composable () -> Unit = {
            composingCounter++
            SideEffect { applyCounter++ }
        }

        rule.setContent {
            SubcomposeLayout(state) {
                if (addSlot.value) {
                    subcompose(Unit, content)
                }
                layout(10, 10) {}
            }
        }

        val precomposition =
            rule.runOnIdle {
                assertThat(composingCounter).isEqualTo(0)
                state.createPausedPrecomposition(Unit, content)
            }

        rule.runOnIdle {
            assertThat(composingCounter).isEqualTo(0)
            precomposition.resumeUntilCompleted()

            assertThat(composingCounter).isEqualTo(1)
            assertThat(applyCounter).isEqualTo(0)

            precomposition.apply()
            assertThat(composingCounter).isEqualTo(1)
            assertThat(applyCounter).isEqualTo(1)
        }

        rule.runOnIdle { addSlot.value = true }

        rule.runOnIdle {
            assertThat(composingCounter).isEqualTo(1)
            assertThat(applyCounter).isEqualTo(1)
        }
    }

    @Test
    fun precomposePaused_composeOnly_applyDuringRegularPhase() {
        val addSlot = mutableStateOf(false)
        var composingCounter = 0
        var applyCounter = 0
        val state = SubcomposeLayoutState()
        val content: @Composable () -> Unit = {
            composingCounter++
            SideEffect { applyCounter++ }
        }

        rule.setContent {
            SubcomposeLayout(state) {
                if (addSlot.value) {
                    subcompose(Unit, content)
                }
                layout(10, 10) {}
            }
        }

        val precomposition =
            rule.runOnIdle {
                assertThat(composingCounter).isEqualTo(0)
                state.createPausedPrecomposition(Unit, content)
            }

        rule.runOnIdle {
            assertThat(composingCounter).isEqualTo(0)
            precomposition.resumeUntilCompleted()
        }

        rule.runOnIdle {
            assertThat(composingCounter).isEqualTo(1)
            assertThat(applyCounter).isEqualTo(0)
            addSlot.value = true
        }

        rule.runOnIdle {
            assertThat(composingCounter).isEqualTo(1)
            assertThat(applyCounter).isEqualTo(1)
        }
    }

    @Test
    fun precomposePaused_pauseStraightAway_doTheRestDuringRegularPhase() {
        val addSlot = mutableStateOf(false)
        var composingCounter = 0
        var applyCounter = 0
        val state = SubcomposeLayoutState()
        val content: @Composable () -> Unit = {
            composingCounter++
            SideEffect { applyCounter++ }
        }

        rule.setContent {
            SubcomposeLayout(state) {
                if (addSlot.value) {
                    subcompose(Unit, content)
                }
                layout(10, 10) {}
            }
        }

        val precomposition =
            rule.runOnIdle {
                assertThat(composingCounter).isEqualTo(0)
                state.createPausedPrecomposition(Unit, content)
            }

        rule.runOnIdle {
            assertThat(composingCounter).isEqualTo(0)
            precomposition.resume { true }
        }

        rule.runOnIdle { addSlot.value = true }

        rule.runOnIdle {
            assertThat(composingCounter).isEqualTo(1)
            assertThat(applyCounter).isEqualTo(1)
        }
    }

    @Test
    fun disposePrecomposedPausedItem() {
        val addSlot = mutableStateOf(false)
        var composingCounter = 0
        var applyCounter = 0
        val state = SubcomposeLayoutState()
        val content: @Composable () -> Unit = {
            composingCounter++
            SideEffect { applyCounter++ }
        }

        rule.setContent {
            SubcomposeLayout(state) {
                if (addSlot.value) {
                    subcompose(Unit, content)
                }
                layout(10, 10) {}
            }
        }

        rule.runOnIdle {
            assertThat(composingCounter).isEqualTo(0)
            val precomposition = state.createPausedPrecomposition(Unit, content)
            precomposition.resumeUntilCompleted()
            assertThat(composingCounter).isEqualTo(1)
            precomposition.cancel()
        }

        rule.runOnIdle { addSlot.value = true }

        rule.runOnIdle {
            // as we canceled precomposition, we compose it again during measure
            assertThat(composingCounter).isEqualTo(2)
            assertThat(applyCounter).isEqualTo(1)
        }
    }

    @Test
    fun precomposePaused_isComplete() {
        val state = SubcomposeLayoutState()

        rule.setContent { SubcomposeLayout(state) { layout(10, 10) {} } }

        rule.runOnIdle {
            val precomposition =
                state.createPausedPrecomposition(Unit) { Box(Modifier.size(100.dp)) }
            assertThat(precomposition.isComplete).isFalse()
            while (!precomposition.isComplete) {
                val result = precomposition.resume { true }
                assertThat(result).isEqualTo(precomposition.isComplete)
            }
        }
    }

    @Test(expected = IllegalStateException::class)
    fun precomposePaused_applyOnNotCompletedPrecompositionThrows() {
        val state = SubcomposeLayoutState()

        rule.setContent { SubcomposeLayout(state) { layout(10, 10) {} } }

        rule.runOnIdle {
            val precomposition =
                state.createPausedPrecomposition(Unit) { Box(Modifier.size(100.dp)) }
            assertThat(precomposition.isComplete).isFalse()
            precomposition.apply()
        }
    }

    @Test
    fun premeasuringAfterPrecomposePaused() {
        val state = SubcomposeLayoutState()
        var remeasuresCount = 0
        val modifier =
            Modifier.layout { measurable, constraints ->
                    val placeable = measurable.measure(constraints)
                    remeasuresCount++
                    layout(placeable.width, placeable.height) { placeable.place(0, 0) }
                }
                .fillMaxSize()
        val content = @Composable { Box(modifier) }
        val constraints = Constraints(maxWidth = 100, minWidth = 100)

        rule.setContent { SubcomposeLayout(state) { layout(10, 10) {} } }

        rule.runOnIdle {
            assertThat(remeasuresCount).isEqualTo(0)
            val precomposition = state.createPausedPrecomposition(Unit, content)
            precomposition.resumeUntilCompleted()
            val handle = precomposition.apply()

            assertThat(remeasuresCount).isEqualTo(0)
            assertThat(handle.placeablesCount).isEqualTo(1)
            handle.premeasure(0, constraints)

            assertThat(remeasuresCount).isEqualTo(1)
        }
    }

    @Test
    fun schedulingRecompositionOnDeactivatingChildIsNotCausingRecomposition() {
        val state = SubcomposeLayoutState(SubcomposeSlotReusePolicy(1))
        var counter by mutableStateOf(0)
        var addSlot by mutableStateOf(true)
        var counterInSubcomposition = 0
        rule.setContent {
            SubcomposeLayout(state) {
                if (addSlot) {
                    subcompose(Unit) { counterInSubcomposition = counter }
                } else {
                    counter = 1
                    Snapshot.sendApplyNotifications()
                }
                layout(10, 10) {}
            }
        }

        rule.runOnIdle { addSlot = false }

        rule.runOnIdle { assertThat(counterInSubcomposition).isEqualTo(0) }
    }

    @Test
    fun precomposeOnTopOfCancelledPrecomposition() {
        val state = SubcomposeLayoutState(SubcomposeSlotReusePolicy(1))

        rule.setContent { SubcomposeLayout(state) { layout(10, 10) {} } }
        var content1Composed = false
        var content2Composed = false

        rule.runOnIdle {
            val precomposition =
                state.createPausedPrecomposition(Unit) {
                    Box(Modifier.size(100.dp))
                    DisposableEffect(Unit) {
                        content1Composed = true
                        onDispose { content1Composed = false }
                    }
                }

            precomposition.resumeUntilCompleted()
            precomposition.cancel()

            val precomposition2 =
                state.createPausedPrecomposition(Unit) {
                    Box(Modifier.padding(5.dp))
                    DisposableEffect(Unit) {
                        content2Composed = true
                        onDispose { content2Composed = false }
                    }
                }

            precomposition2.resumeUntilCompleted()
            precomposition2.apply()

            assertThat(content1Composed).isFalse()
            assertThat(content2Composed).isTrue()
        }
    }

    @Test
    fun precomposingDifferentContentOnTop() {
        val state = SubcomposeLayoutState()

        rule.setContent { SubcomposeLayout(state) { layout(10, 10) {} } }
        var content1Composed = false
        var content2Composed = false

        rule.runOnIdle {
            state.precompose(Unit) {
                Box(Modifier.size(100.dp))
                DisposableEffect(Unit) {
                    content1Composed = true
                    onDispose { content1Composed = false }
                }
            }

            assertThat(content1Composed).isTrue()

            val precomposition2 =
                state.precompose(Unit) {
                    Box(Modifier.padding(5.dp))
                    DisposableEffect(Unit) {
                        content2Composed = true
                        onDispose { content2Composed = false }
                    }
                }

            assertThat(content1Composed).isFalse()
            assertThat(content2Composed).isTrue()

            precomposition2.dispose()

            assertThat(content1Composed).isFalse()
            assertThat(content2Composed).isFalse()
        }
    }

    @Test
    fun precomposingDifferentContentOnTop_paused() {
        val state = SubcomposeLayoutState()

        rule.setContent { SubcomposeLayout(state) { layout(10, 10) {} } }
        var content1Composed = false
        var content2Composed = false

        rule.runOnIdle {
            val precomposition =
                state.createPausedPrecomposition(Unit) {
                    Box(Modifier.size(100.dp))
                    DisposableEffect(Unit) {
                        content1Composed = true
                        onDispose { content1Composed = false }
                    }
                }

            precomposition.resumeUntilCompleted()
            precomposition.apply()

            assertThat(content1Composed).isTrue()

            val precomposition2 =
                state.createPausedPrecomposition(Unit) {
                    Box(Modifier.padding(5.dp))
                    DisposableEffect(Unit) {
                        content2Composed = true
                        onDispose { content2Composed = false }
                    }
                }

            precomposition2.resumeUntilCompleted()
            val handle = precomposition2.apply()

            assertThat(content1Composed).isFalse()
            assertThat(content2Composed).isTrue()

            handle.dispose()

            assertThat(content1Composed).isFalse()
            assertThat(content2Composed).isFalse()
        }
    }

    @Test
    fun precomposingNotPausedOnTopOfPaused() {
        val state = SubcomposeLayoutState()

        rule.setContent { SubcomposeLayout(state) { layout(10, 10) {} } }
        var content1Composed = false
        var content2Composed = false

        rule.runOnIdle {
            val precomposition =
                state.createPausedPrecomposition(Unit) {
                    Box(Modifier.size(100.dp))
                    DisposableEffect(Unit) {
                        content1Composed = true
                        onDispose { content1Composed = false }
                    }
                }

            precomposition.resumeUntilCompleted()
            precomposition.apply()

            assertThat(content1Composed).isTrue()

            val handle =
                state.precompose(Unit) {
                    Box(Modifier.padding(5.dp))
                    DisposableEffect(Unit) {
                        content2Composed = true
                        onDispose { content2Composed = false }
                    }
                }

            assertThat(content1Composed).isFalse()
            assertThat(content2Composed).isTrue()

            handle.dispose()

            assertThat(content1Composed).isFalse()
            assertThat(content2Composed).isFalse()
        }
    }

    @Test
    fun precomposingNotPausedOnTopOfNotAppliedPaused() {
        val state = SubcomposeLayoutState()

        rule.setContent { SubcomposeLayout(state) { layout(10, 10) {} } }
        var contentComposed = false
        val content =
            @Composable {
                Box(Modifier.size(100.dp))
                DisposableEffect(Unit) {
                    contentComposed = true
                    onDispose { contentComposed = false }
                }
            }

        rule.runOnIdle {
            val precomposition = state.createPausedPrecomposition(Unit, content)

            precomposition.resumeUntilCompleted() // but not applying

            assertThat(contentComposed).isFalse()

            val handle = state.precompose(Unit, content)

            assertThat(contentComposed).isTrue()

            // should do nothing as we already composed another content over
            precomposition.cancel()

            assertThat(contentComposed).isTrue()

            handle.dispose()

            assertThat(contentComposed).isFalse()
        }
    }

    @Test
    fun precomposingNotPausedOnTopOfNotAppliedPaused_differentContent() {
        val state = SubcomposeLayoutState()

        rule.setContent { SubcomposeLayout(state) { layout(10, 10) {} } }
        var content1Composed = false
        var content2Composed = false

        rule.runOnIdle {
            val precomposition =
                state.createPausedPrecomposition(Unit) {
                    Box(Modifier.size(100.dp))
                    DisposableEffect(Unit) {
                        content1Composed = true
                        onDispose { content1Composed = false }
                    }
                }

            precomposition.resumeUntilCompleted() // but not applying

            val handle =
                state.precompose(Unit) {
                    Box(Modifier.padding(5.dp))
                    DisposableEffect(Unit) {
                        content2Composed = true
                        onDispose { content2Composed = false }
                    }
                }

            assertThat(content1Composed).isFalse()
            assertThat(content2Composed).isTrue()

            // should do nothing as we already composed another content over
            precomposition.cancel()

            assertThat(content2Composed).isTrue()

            handle.dispose()

            assertThat(content2Composed).isFalse()
        }
    }

    @Test
    fun precomposingPausedOnTopOfNotAppliedPaused() {
        val state = SubcomposeLayoutState()

        rule.setContent { SubcomposeLayout(state) { layout(10, 10) {} } }
        var contentComposed = false
        val content =
            @Composable {
                Box(Modifier.size(100.dp))
                DisposableEffect(Unit) {
                    contentComposed = true
                    onDispose { contentComposed = false }
                }
            }

        rule.runOnIdle {
            val precomposition = state.createPausedPrecomposition(Unit, content)

            precomposition.resumeUntilCompleted() // but not applying

            assertThat(contentComposed).isFalse()

            val precomposition2 = state.createPausedPrecomposition(Unit, content)

            assertThat(contentComposed).isFalse()

            precomposition2.resumeUntilCompleted()
            val handle = precomposition2.apply()

            assertThat(contentComposed).isTrue()

            // both should do nothing as we already applied
            precomposition.cancel()
            precomposition2.cancel()

            assertThat(contentComposed).isTrue()

            handle.dispose()

            assertThat(contentComposed).isFalse()
        }
    }

    @Test
    fun precomposingPausedOnTopOfNotAppliedPaused_differentContent() {
        val state = SubcomposeLayoutState()

        rule.setContent { SubcomposeLayout(state) { layout(10, 10) {} } }
        var content1Composed = false
        var content2Composed = false

        rule.runOnIdle {
            val precomposition =
                state.createPausedPrecomposition(Unit) {
                    Box(Modifier.size(100.dp))
                    DisposableEffect(Unit) {
                        content1Composed = true
                        onDispose { content1Composed = false }
                    }
                }

            precomposition.resumeUntilCompleted() // but not applying

            val precomposition2 =
                state.createPausedPrecomposition(Unit) {
                    Box(Modifier.padding(5.dp))
                    DisposableEffect(Unit) {
                        content2Composed = true
                        onDispose { content2Composed = false }
                    }
                }

            assertThat(content1Composed).isFalse()
            assertThat(content2Composed).isFalse()

            precomposition2.resumeUntilCompleted()
            precomposition2.apply()

            assertThat(content1Composed).isFalse()
            assertThat(content2Composed).isTrue()
        }
    }

    private fun alternateLookaheadPlacement(shouldPlaceItem: BooleanArray) {
        var lookaheadPos: Offset? = null
        var approachPos: Offset? = null
        var placeItem2InLookahead: Boolean by mutableStateOf(shouldPlaceItem[0])
        rule.setContent {
            LookaheadScope {
                CompositionLocalProvider(LocalDensity provides Density(1f)) {
                    SubcomposeLayout(Modifier.size(200.dp, 600.dp)) {
                        val m1 = subcompose(1) { Box(Modifier.size(200.dp)) }
                        val m2 =
                            subcompose(2) {
                                // This box's placement is skipped in lookahead
                                Box(
                                    Modifier.passThroughLayout { placementScope ->
                                            with(placementScope) {
                                                val pos = coordinates?.positionInParent()
                                                if (isLookingAhead) {
                                                    lookaheadPos = pos ?: lookaheadPos
                                                } else {
                                                    approachPos = pos ?: lookaheadPos
                                                }
                                            }
                                        }
                                        .size(200.dp)
                                )
                            }
                        val p1 = m1[0].measure(it)
                        val p2 = m2[0].measure(it)
                        layout(200, 400) {
                            if (isLookingAhead) {
                                p1.place(0, 0)
                                if (placeItem2InLookahead) {
                                    p2.place(0, 300)
                                }
                            } else {
                                p1.place(0, 0)
                                p2.place(0, 200)
                            }
                        }
                    }
                }
            }
        }
        repeat(shouldPlaceItem.size) { id ->
            rule.runOnIdle {
                assertEquals(shouldPlaceItem[id], placeItem2InLookahead)
                if (placeItem2InLookahead) {
                    assertEquals(Offset(0f, 300f), lookaheadPos)
                } else {
                    assertEquals(Offset(0f, 200f), lookaheadPos)
                }
                assertEquals(Offset(0f, 200f), approachPos)
            }
            if (shouldPlaceItem.size > id + 1) {
                placeItem2InLookahead = shouldPlaceItem[id + 1]
            }
        }
    }

    private fun Modifier.passThroughLayout(
        beforePlace: MeasureScope.(Placeable.PlacementScope) -> Unit
    ) =
        this.layout { m, c ->
            m.measure(c).run {
                layout(width, height) {
                    beforePlace(this)
                    place(0, 0)
                }
            }
        }

    private fun SubcomposeMeasureScope.measure(
        slotId: Any,
        constraints: Constraints,
        content: @Composable () -> Unit,
    ): Placeable = subcompose(slotId, content).first().measure(constraints)

    private fun composeItems(state: SubcomposeLayoutState, items: MutableState<List<Int>>) {
        rule.setContent {
            SubcomposeLayout(state) { constraints ->
                items.value.forEach {
                    subcompose(it) { ItemContent(it) }.forEach { it.measure(constraints) }
                }
                layout(10, 10) {}
            }
        }
    }

    @Composable
    private fun ItemContent(index: Int) {
        Box(Modifier.fillMaxSize().testTag("$index"))
    }

    private fun assertNodes(active: List<Int> = emptyList(), disposed: List<Int> = emptyList()) {
        active.forEach { rule.onNodeWithTag("$it").assertExists() }
        disposed.forEach { rule.onNodeWithTag("$it").assertDoesNotExist() }
    }

    private fun SemanticsNodeInteraction.assertIsDetached() {
        assertDoesNotExist()
        // we want to verify the node is not deactivated, but such API does not exist yet
        expectAssertionError { assertIsDeactivated() }
    }

    private fun SemanticsNode.assertLayoutDeactivatedById(id: Int) {
        children.fastForEach {
            if (it.id == id) {
                assert(it.layoutInfo.isDeactivated)
            }
        }
    }
}

fun ImageBitmap.assertCenterPixelColor(expectedColor: Color) {
    asAndroidBitmap().assertColor(expectedColor, width / 2, height / 2)
}

@Composable
private fun LayoutUsingAlignments(content: @Composable () -> Unit) {
    Layout(content) { measurables, constraints ->
        val placeable = measurables.first().measure(constraints)
        placeable[FirstBaseline]
        layout(placeable.width, placeable.height) { placeable.place(0, 0) }
    }
}

private enum class Screens {
    Screen1,
    Screen2,
}

private fun SubcomposeLayoutState.PausedPrecomposition.resumeUntilCompleted() {
    while (!isComplete) {
        resume { false }
    }
}
