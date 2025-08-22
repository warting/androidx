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

package androidx.xr.compose.spatial

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onChild
import androidx.compose.ui.test.onChildren
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.xr.compose.platform.LocalSession
import androidx.xr.compose.platform.LocalSpatialCapabilities
import androidx.xr.compose.platform.SpatialCapabilities
import androidx.xr.compose.subspace.SpatialMainPanel
import androidx.xr.compose.subspace.SpatialPanel
import androidx.xr.compose.subspace.layout.SubspaceModifier
import androidx.xr.compose.subspace.layout.height
import androidx.xr.compose.subspace.layout.size
import androidx.xr.compose.subspace.layout.testTag
import androidx.xr.compose.subspace.layout.width
import androidx.xr.compose.testing.SubspaceTestingActivity
import androidx.xr.compose.testing.TestJxrPlatformAdapter
import androidx.xr.compose.testing.createFakeRuntime
import androidx.xr.compose.testing.createFakeSession
import androidx.xr.compose.testing.session
import androidx.xr.compose.testing.setContentWithCompatibilityForXr
import androidx.xr.compose.testing.toDp
import androidx.xr.scenecore.PanelEntity
import androidx.xr.scenecore.internal.PanelEntity as RtPanelEntity
import androidx.xr.scenecore.scene
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify

@RunWith(AndroidJUnit4::class)
class OrbiterTest {

    @get:Rule val composeTestRule = createAndroidComposeRule<SubspaceTestingActivity>()

    private val parentTestTag = "parent"

    @Test
    fun orbiter_contentIsElevated() {
        composeTestRule.setContentWithCompatibilityForXr {
            Box(Modifier.testTag(parentTestTag)) {
                Orbiter(ContentEdge.Top) { Text("Main Content") }
            }
        }

        composeTestRule.onNodeWithText("Main Content").assertExists()
        composeTestRule.onNodeWithTag(parentTestTag).onChild().assertDoesNotExist()
    }

    @Test
    fun orbiter_nonXr_contentIsInline() {
        composeTestRule.setContent {
            Box(Modifier.testTag(parentTestTag)) {
                Orbiter(ContentEdge.Top) { Text("Main Content") }
            }
        }

        composeTestRule.onNodeWithTag(parentTestTag).onChild().assertTextContains("Main Content")
    }

    @Test
    fun orbiter_homeSpaceMode_contentIsInline() {
        composeTestRule.session = createFakeSession(composeTestRule.activity)
        composeTestRule.session?.scene?.requestHomeSpaceMode()

        composeTestRule.setContentWithCompatibilityForXr {
            Box(Modifier.testTag(parentTestTag)) {
                Orbiter(ContentEdge.Top) { Text("Main Content") }
            }
        }

        composeTestRule.onNodeWithTag(parentTestTag).onChild().assertTextContains("Main Content")
    }

    @Test
    fun orbiter_nonSpatial_doesNotRenderContent() {
        composeTestRule.session = createFakeSession(composeTestRule.activity)
        composeTestRule.session?.scene?.requestHomeSpaceMode()

        composeTestRule.setContentWithCompatibilityForXr {
            Box {
                Orbiter(ContentEdge.Top, shouldRenderInNonSpatial = false) { Text("Main Content") }
            }
        }

        composeTestRule.onNodeWithText("Main Content").assertDoesNotExist()
    }

    @Test
    fun orbiter_multipleInstances_rendersInSpatial() {
        composeTestRule.setContentWithCompatibilityForXr {
            Box(Modifier.testTag(parentTestTag)) {
                Orbiter(position = ContentEdge.Top) { Text("Top") }
                Orbiter(position = ContentEdge.Start) { Text("Start") }
                Orbiter(position = ContentEdge.End) { Text("End") }
                Orbiter(position = ContentEdge.Bottom) { Text("Bottom") }
            }
        }

        composeTestRule.onNodeWithTag(parentTestTag).onChild().assertDoesNotExist()
    }

    @Test
    fun orbiter_afterSwitchToFullSpaceMode_isSpatial() {
        composeTestRule.session = createFakeSession(composeTestRule.activity)
        composeTestRule.session?.scene?.requestHomeSpaceMode()

        composeTestRule.setContentWithCompatibilityForXr {
            Box(Modifier.testTag(parentTestTag)) {
                Orbiter(position = ContentEdge.Bottom) { Text("Bottom") }
            }
            checkNotNull(LocalSession.current).scene.requestFullSpaceMode()
        }

        composeTestRule.onNodeWithTag(parentTestTag).onChild().assertDoesNotExist()
    }

    @Test
    fun orbiter_setting_contentIsNotInline() {
        composeTestRule.setContent {
            Box(Modifier.testTag(parentTestTag)) {
                Orbiter(ContentEdge.Top, shouldRenderInNonSpatial = false) { Text("Main Content") }
            }
        }

        composeTestRule.onNodeWithTag(parentTestTag).onChild().assertDoesNotExist()
    }

    @Test
    fun orbiter_settingChange_contentIsInline() {
        var shouldRenderInNonSpatial by mutableStateOf(false)
        composeTestRule.setContent {
            Box(Modifier.testTag(parentTestTag)) {
                Orbiter(ContentEdge.Top, shouldRenderInNonSpatial = shouldRenderInNonSpatial) {
                    Text("Main Content")
                }
            }
        }

        shouldRenderInNonSpatial = true

        composeTestRule.onNodeWithTag(parentTestTag).onChild().assertTextContains("Main Content")
    }

    @Test
    fun orbiter_orbiterRendered() {
        composeTestRule.session = createFakeSession(composeTestRule.activity)
        composeTestRule.session?.scene?.requestHomeSpaceMode()

        composeTestRule.setContentWithCompatibilityForXr {
            Box {
                Text("Main Content")
                Orbiter(ContentEdge.Start) { Text("Orbiter Content") }
            }
        }

        composeTestRule.onNodeWithText("Main Content").assertExists()
        composeTestRule.onNodeWithText("Orbiter Content").assertExists()
    }

    @Test
    fun orbiter_orbiterCanBeRemoved() {
        var showOrbiter by mutableStateOf(true)

        composeTestRule.session = createFakeSession(composeTestRule.activity)
        composeTestRule.session?.scene?.requestHomeSpaceMode()

        composeTestRule.setContentWithCompatibilityForXr {
            Box(modifier = Modifier.size(100.dp)) {
                Text("Main Content")
                if (showOrbiter) {
                    Orbiter(position = ContentEdge.Top) { Text("Top Orbiter Content") }
                }
            }
        }

        composeTestRule.onNodeWithText("Top Orbiter Content").assertExists()
        showOrbiter = false
        composeTestRule.onNodeWithText("Top Orbiter Content").assertDoesNotExist()
    }

    @Test
    fun orbiter_orbiterRenderedInlineInHomeSpaceMode() {
        composeTestRule.setContentWithCompatibilityForXr {
            Box(Modifier.testTag(parentTestTag)) {
                Box(modifier = Modifier.size(100.dp)) { Text("Main Content") }
                Orbiter(
                    position = ContentEdge.Top,
                    offset = 0.dp,
                    offsetType = OrbiterOffsetType.InnerEdge,
                ) {
                    Text("Top Orbiter Content")
                }
                Orbiter(position = ContentEdge.Start) { Text("Start Orbiter Content") }
                Orbiter(position = ContentEdge.Bottom) { Text("Bottom Orbiter Content") }
                Orbiter(position = ContentEdge.End) { Text("End Orbiter Content") }
            }
        }

        composeTestRule.onNodeWithTag(parentTestTag).onChild().assertTextContains("Main Content")
        composeTestRule.runOnIdle {
            checkNotNull(composeTestRule.session).scene.requestHomeSpaceMode()
        }

        // All orbiters become children of the Parent node
        composeTestRule.onNodeWithTag(parentTestTag).onChildren().assertCountEquals(5)
        composeTestRule.runOnIdle {
            checkNotNull(composeTestRule.session).scene.requestFullSpaceMode()
        }

        // Orbiters exist outside of the compose hierarchy
        composeTestRule.onNodeWithTag(parentTestTag).onChildren().assertCountEquals(1)
    }

    @Test
    fun orbiter_inSetContent_noSubspace_usesMainWindowSize() {
        composeTestRule.setContentWithCompatibilityForXr {
            Orbiter(ContentEdge.Top) {
                // The content of the Orbiter. We'll use its size, which is constrained
                // by the parent's panel size, to verify the change.
                Box(modifier = Modifier.fillMaxSize().testTag("orbiterContentBox")) {
                    Text("Some Orbiter content")
                }
            }
        }

        val session = composeTestRule.session
        assertNotNull(session)

        composeTestRule
            .onNodeWithTag("orbiterContentBox")
            .assertWidthIsEqualTo(session.activity.window.decorView.width.toDp())
            .assertHeightIsEqualTo(session.activity.window.decorView.height.toDp())
    }

    @Test
    fun orbiter_inSubspace_spatialPanelParent_usesSpatialPanelSize() {
        val testMainPanelEntity = mock<RtPanelEntity>()
        val fakeRuntime = createFakeRuntime(composeTestRule.activity)
        val testJxrPlatformAdapter =
            TestJxrPlatformAdapter.create(fakeRuntime).apply {
                mainPanelEntity = testMainPanelEntity
            }
        composeTestRule.session =
            createFakeSession(composeTestRule.activity, testJxrPlatformAdapter)

        composeTestRule.setContentWithCompatibilityForXr {
            ApplicationSubspace {
                SpatialPanel(SubspaceModifier.width(200.dp).height(200.dp).testTag("panel")) {
                    Orbiter(ContentEdge.Top) {
                        // The content of the Orbiter. We'll use its size, which is constrained
                        // by the parent's panel size, to verify the change.
                        Box(modifier = Modifier.fillMaxSize().testTag("orbiterContentBox")) {
                            Text("Some Orbiter content")
                        }
                    }
                }
            }
        }

        composeTestRule
            .onNodeWithTag("orbiterContentBox")
            .assertWidthIsEqualTo(200.toDp())
            .assertHeightIsEqualTo(200.toDp())
        // Check `getMainWindowSize` is never called.
        verify(testMainPanelEntity, never()).sizeInPixels
    }

    @Test
    fun orbiter_inSubspace_spatialPanelParent_resizesToParentResize() {
        var panelWidthDp by mutableStateOf(200.dp)
        var panelHeightDp by mutableStateOf(200.dp)

        composeTestRule.setContentWithCompatibilityForXr {
            ApplicationSubspace {
                SpatialPanel(
                    modifier =
                        SubspaceModifier.width(panelWidthDp)
                            .height(panelHeightDp)
                            .testTag("spatialPanelParent")
                ) {
                    Orbiter(ContentEdge.Start) {
                        // The content of the Orbiter. We'll use its size, which is constrained
                        // by the parent's panel size, to verify the change.
                        Box(modifier = Modifier.fillMaxSize().testTag("orbiterContentBox"))
                    }
                }
            }
        }

        composeTestRule
            .onNodeWithTag("orbiterContentBox")
            .assertWidthIsEqualTo(panelWidthDp)
            .assertHeightIsEqualTo(panelHeightDp)

        panelWidthDp = 300.dp
        panelHeightDp = 300.dp

        composeTestRule
            .onNodeWithTag("orbiterContentBox")
            .assertWidthIsEqualTo(panelWidthDp)
            .assertHeightIsEqualTo(panelHeightDp)
    }

    @Test
    fun orbiter_inSubspace_mainPanelParent_usesMainPanelSize() {
        val testMainPanelEntity = mock<RtPanelEntity>()
        val fakeRuntime = createFakeRuntime(composeTestRule.activity)
        val testJxrPlatformAdapter =
            TestJxrPlatformAdapter.create(fakeRuntime).apply {
                mainPanelEntity = testMainPanelEntity
            }
        composeTestRule.session =
            createFakeSession(composeTestRule.activity, testJxrPlatformAdapter)

        composeTestRule.setContentWithCompatibilityForXr {
            ApplicationSubspace {
                SpatialMainPanel(SubspaceModifier.width(200.dp).height(200.dp).testTag("panel"))
                Orbiter(ContentEdge.Top) {
                    // The content of the Orbiter. We'll use its size, which is constrained
                    // by the parent's panel size, to verify the change.
                    Box(modifier = Modifier.fillMaxSize().testTag("orbiterContentBox")) {}
                }
            }
        }

        composeTestRule
            .onNodeWithTag("orbiterContentBox")
            .assertWidthIsEqualTo(200.toDp())
            .assertHeightIsEqualTo(200.toDp())
        // Check `getMainWindowSize` is never called.
        verify(testMainPanelEntity, never()).sizeInPixels
    }

    @Test
    fun orbiter_inSubspace_mainPanelParent_resizesToParentResize() {
        var panelWidthDp by mutableStateOf(200.dp)
        var panelHeightDp by mutableStateOf(200.dp)

        composeTestRule.setContentWithCompatibilityForXr {
            ApplicationSubspace {
                SpatialMainPanel(
                    modifier =
                        SubspaceModifier.width(panelWidthDp)
                            .height(panelHeightDp)
                            .testTag("mainPanelParent")
                )
                Orbiter(ContentEdge.Top) {
                    // The content of the Orbiter. We'll use its size, which is constrained
                    // by the parent's panel size, to verify the change.
                    Box(modifier = Modifier.fillMaxSize().testTag("orbiterContentBox"))
                }
            }
        }

        composeTestRule
            .onNodeWithTag("orbiterContentBox")
            .assertWidthIsEqualTo(panelWidthDp)
            .assertHeightIsEqualTo(panelHeightDp)

        panelWidthDp = 300.dp
        panelHeightDp = 300.dp

        composeTestRule
            .onNodeWithTag("orbiterContentBox")
            .assertWidthIsEqualTo(panelWidthDp)
            .assertHeightIsEqualTo(panelHeightDp)
    }

    @Test
    fun orbiter_contentLargerThanParent_isConstrainedBySpatialPanel() {
        composeTestRule.setContentWithCompatibilityForXr {
            ApplicationSubspace {
                // Parent panel with a fixed size
                SpatialPanel(SubspaceModifier.size(200.dp)) {
                    Orbiter(ContentEdge.Top) {
                        // Orbiter content that is larger than the parent panel
                        Box(modifier = Modifier.size(300.dp).testTag("orbiterContentBox"))
                    }
                }
            }
        }

        // The orbiter's content should be constrained by the parent's size (200.dp)
        composeTestRule
            .onNodeWithTag("orbiterContentBox")
            .assertWidthIsEqualTo(200.dp)
            .assertHeightIsEqualTo(200.dp)
    }

    @Test
    fun orbiter_contentLargerThanParent_isConstrainedByMainPanel() {
        composeTestRule.setContentWithCompatibilityForXr {
            ApplicationSubspace {
                // Main panel with a fixed size
                SpatialMainPanel(SubspaceModifier.size(200.dp))
                Orbiter(ContentEdge.Top) {
                    // Orbiter content that is larger than the main panel
                    Box(modifier = Modifier.size(300.dp).testTag("orbiterContentBox"))
                }
            }
        }

        // The orbiter's content should be constrained by the main panel's size (200.dp)
        composeTestRule
            .onNodeWithTag("orbiterContentBox")
            .assertWidthIsEqualTo(200.dp)
            .assertHeightIsEqualTo(200.dp)
    }

    @Test
    fun orbiter_unparented_ContentLargerThanParent_isConstrainedByMainWindow() {
        var windowWidthDp by mutableStateOf(0.dp)
        var windowHeightDp by mutableStateOf(0.dp)

        composeTestRule.setContentWithCompatibilityForXr {
            val window = composeTestRule.activity.window
            windowWidthDp = window.decorView.width.toDp()
            windowHeightDp = window.decorView.height.toDp()

            Orbiter(ContentEdge.Top) {
                // Orbiter content that is larger than the main window
                Box(
                    modifier =
                        Modifier.size(windowWidthDp + 100.dp, windowHeightDp + 100.dp)
                            .testTag("orbiterContentBox")
                )
            }
        }

        // The orbiter's content should be constrained by the main window's size
        composeTestRule
            .onNodeWithTag("orbiterContentBox")
            .assertWidthIsEqualTo(windowWidthDp)
            .assertHeightIsEqualTo(windowHeightDp)
    }

    @Test
    fun orbiter_inSubspace_noMainPanel_isSizeZero() {
        composeTestRule.setContentWithCompatibilityForXr {
            ApplicationSubspace {
                Orbiter(ContentEdge.Top) {
                    // The content of the Orbiter. We'll use its size, which is constrained
                    // by the parent's panel size, to verify the change.
                    Box(modifier = Modifier.fillMaxSize().testTag("orbiterContentBox")) {}
                }
            }
        }

        composeTestRule
            .onNodeWithTag("orbiterContentBox")
            .assertWidthIsEqualTo(0.toDp())
            .assertHeightIsEqualTo(0.toDp())
    }

    @Test
    fun orbiter_inSubspace_noSpatialCapabilities_doesNotThrow() {
        composeTestRule.setContentWithCompatibilityForXr {
            ApplicationSubspace {
                CompositionLocalProvider(
                    LocalSpatialCapabilities provides SpatialCapabilities.NoCapabilities
                ) {
                    Orbiter(position = ContentEdge.Top) {
                        Box(modifier = Modifier.fillMaxSize().testTag("orbiterContentBox")) {}
                    }
                }
            }
        }

        composeTestRule.onNodeWithTag("orbiterContentBox").assertExists()
    }

    @Test
    fun unparentedOrbiter_adaptsToMainWindowResize_viaListener() {
        var triggerResize by mutableStateOf(false)

        var initialWidth = 0
        var initialHeight = 0
        var targetResizeWidth = 0
        var targetResizeHeight = 0

        composeTestRule.setContentWithCompatibilityForXr {
            val session = checkNotNull(LocalSession.current)

            initialWidth = session.activity.window.decorView.width
            initialHeight = session.activity.window.decorView.height
            targetResizeWidth = initialWidth + 100
            targetResizeHeight = initialHeight + 100

            // This LaunchedEffect will simulate the window resize when triggerResize becomes
            // true.
            LaunchedEffect(triggerResize) {
                if (triggerResize) {
                    session.activity.window.decorView.layout(
                        0,
                        0,
                        targetResizeWidth,
                        targetResizeHeight,
                    )
                }
            }

            Orbiter(ContentEdge.Top) {
                Box(modifier = Modifier.fillMaxSize().testTag("orbiterContentBox")) {
                    Text("Some Orbiter content")
                }
            }
        }

        composeTestRule
            .onNodeWithTag("orbiterContentBox")
            .assertWidthIsEqualTo(initialWidth.toDp())
            .assertHeightIsEqualTo(initialHeight.toDp())

        triggerResize = true

        composeTestRule
            .onNodeWithTag("orbiterContentBox")
            .assertWidthIsEqualTo(targetResizeWidth.toDp())
            .assertHeightIsEqualTo(targetResizeHeight.toDp())
    }

    @Test
    fun orbiter_inPanel_isParentedToTheContainingPanel() {
        composeTestRule.setContentWithCompatibilityForXr {
            Subspace {
                SpatialPanel(SubspaceModifier.size(100.dp)) {
                    Orbiter(ContentEdge.Top) {
                        Box(modifier = Modifier.size(10.dp).testTag("orbiterContentBox")) {
                            Text("Some Orbiter content")
                        }
                    }
                }
            }
        }

        composeTestRule.onNodeWithTag("orbiterContentBox").assertWidthIsEqualTo(10.dp)

        val session = checkNotNull(composeTestRule.session)
        val entity =
            session.scene.getEntitiesOfType(PanelEntity::class.java).first {
                it.sizeInPixels.width == 10
            }
        val parentPanel =
            session.scene.getEntitiesOfType(PanelEntity::class.java).first {
                it.sizeInPixels.width == 100
            }
        assertThat(entity.parent).isNotEqualTo(session.scene.mainPanelEntity)
        assertThat(entity.parent).isEqualTo(parentPanel)
    }

    @Test
    fun orbiter_notInPanel_isParentedToTheMainPanel() {
        composeTestRule.setContentWithCompatibilityForXr {
            Orbiter(ContentEdge.Top) {
                Box(modifier = Modifier.size(10.dp).testTag("orbiterContentBox")) {
                    Text("Some Orbiter content")
                }
            }
        }

        composeTestRule.onNodeWithTag("orbiterContentBox").assertExists()

        val session = checkNotNull(composeTestRule.session)
        val entity =
            session.scene.getEntitiesOfType(PanelEntity::class.java).first {
                it.sizeInPixels.width == 10
            }
        assertThat(entity.parent).isEqualTo(session.scene.mainPanelEntity)
    }
}
