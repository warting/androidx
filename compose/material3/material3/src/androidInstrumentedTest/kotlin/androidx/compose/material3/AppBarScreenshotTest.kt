/*
 * Copyright 2021 The Android Open Source Project
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

import android.os.Build
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.BottomAppBarDefaults.bottomAppBarFabColor
import androidx.compose.material3.TopAppBarDefaults.enterAlwaysScrollBehavior
import androidx.compose.material3.tokens.AppBarSmallTokens
import androidx.compose.testutils.assertAgainstGolden
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.test.screenshot.AndroidXScreenshotTestRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
class AppBarScreenshotTest {

    @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @get:Rule val screenshotRule = AndroidXScreenshotTestRule(GOLDEN_MATERIAL3)

    @Test
    fun smallAppBar_lightTheme() {
        composeTestRule.setMaterialContent(lightColorScheme()) {
            Box(Modifier.testTag(TopAppBarTestTag)) {
                TopAppBar(
                    navigationIcon = {
                        IconButton(onClick = { /* doSomething() */ }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                            )
                        }
                    },
                    title = { Text("Title") },
                    actions = {
                        IconButton(onClick = { /* doSomething() */ }) {
                            Icon(imageVector = Icons.Filled.Favorite, contentDescription = "Like")
                        }
                    },
                )
            }
        }

        assertAppBarAgainstGolden(goldenIdentifier = "smallAppBar_lightTheme")
    }

    @Test
    fun smallAppBar_withSubtitle_lightTheme() {
        composeTestRule.setMaterialContent(lightColorScheme()) {
            Box(Modifier.testTag(TopAppBarTestTag)) {
                TopAppBar(
                    navigationIcon = {
                        IconButton(onClick = { /* doSomething() */ }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                            )
                        }
                    },
                    title = { Text("Title") },
                    subtitle = { Text("Subtitle") },
                    actions = {
                        IconButton(onClick = { /* doSomething() */ }) {
                            Icon(imageVector = Icons.Filled.Favorite, contentDescription = "Like")
                        }
                    },
                )
            }
        }

        assertAppBarAgainstGolden(goldenIdentifier = "smallAppBar_withSubtitle_lightTheme")
    }

    @Test
    fun smallAppBar_withoutSubtitle_lightTheme() {
        composeTestRule.setMaterialContent(lightColorScheme()) {
            Box(Modifier.testTag(TopAppBarTestTag)) {
                TopAppBar(
                    navigationIcon = {
                        IconButton(onClick = { /* doSomething() */ }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                            )
                        }
                    },
                    title = { Text("Title") },
                    subtitle = {},
                    actions = {
                        IconButton(onClick = { /* doSomething() */ }) {
                            Icon(imageVector = Icons.Filled.Favorite, contentDescription = "Like")
                        }
                    },
                )
            }
        }

        assertAppBarAgainstGolden(goldenIdentifier = "smallAppBar_withoutSubtitle_lightTheme")
    }

    @Test
    fun smallAppBar_lightTheme_clipsWhenCollapsedWithInsets() {
        composeTestRule.setMaterialContent(lightColorScheme()) {
            val behavior = enterAlwaysScrollBehavior(rememberTopAppBarState())
            Box(Modifier.testTag(TopAppBarTestTag)) {
                TopAppBar(
                    navigationIcon = {
                        IconButton(onClick = { /* doSomething() */ }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                            )
                        }
                    },
                    scrollBehavior = behavior,
                    windowInsets = WindowInsets(top = 30.dp),
                    title = { Text("Title") },
                    actions = {
                        IconButton(onClick = { /* doSomething() */ }) {
                            Icon(imageVector = Icons.Filled.Favorite, contentDescription = "Like")
                        }
                    },
                )
            }
        }

        composeTestRule.onNodeWithTag(TopAppBarTestTag).performTouchInput {
            // start from the bottom so we can drag enough
            down(bottomCenter - Offset(1f, 1f))
            moveBy(Offset(0f, -((AppBarSmallTokens.ContainerHeight - 10.dp).toPx())))
        }

        assertAppBarAgainstGolden(
            goldenIdentifier = "smallAppBar_lightTheme_clipsWhenCollapsedWithInsets"
        )
    }

    @Test
    fun smallAppBar_darkTheme() {
        composeTestRule.setMaterialContent(darkColorScheme()) {
            Box(Modifier.testTag(TopAppBarTestTag)) {
                TopAppBar(
                    navigationIcon = {
                        IconButton(onClick = { /* doSomething() */ }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                            )
                        }
                    },
                    title = { Text("Title") },
                    actions = {
                        IconButton(onClick = { /* doSomething() */ }) {
                            Icon(imageVector = Icons.Filled.Favorite, contentDescription = "Like")
                        }
                    },
                )
            }
        }

        assertAppBarAgainstGolden(goldenIdentifier = "smallAppBar_darkTheme")
    }

    @Test
    fun centerAlignedAppBar_lightTheme() {
        composeTestRule.setMaterialContent(lightColorScheme()) {
            Box(Modifier.testTag(TopAppBarTestTag)) {
                CenterAlignedTopAppBar(
                    navigationIcon = {
                        IconButton(onClick = { /* doSomething() */ }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                            )
                        }
                    },
                    title = { Text("Title") },
                    actions = {
                        IconButton(onClick = { /* doSomething() */ }) {
                            Icon(imageVector = Icons.Filled.Favorite, contentDescription = "Like")
                        }
                    },
                )
            }
        }

        assertAppBarAgainstGolden(goldenIdentifier = "centerAlignedAppBar_lightTheme")
    }

    @Test
    fun centerAlignedAppBar_withSubtitle_lightTheme() {
        composeTestRule.setMaterialContent(lightColorScheme()) {
            Box(Modifier.testTag(TopAppBarTestTag)) {
                TopAppBar(
                    navigationIcon = {
                        IconButton(onClick = { /* doSomething() */ }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                            )
                        }
                    },
                    title = { Text("Title") },
                    subtitle = { Text("Subtitle") },
                    titleHorizontalAlignment = Alignment.CenterHorizontally,
                    actions = {
                        IconButton(onClick = { /* doSomething() */ }) {
                            Icon(imageVector = Icons.Filled.Favorite, contentDescription = "Like")
                        }
                    },
                )
            }
        }

        assertAppBarAgainstGolden(goldenIdentifier = "centerAlignedAppBar_withSubtitle_lightTheme")
    }

    @Test
    fun centerAlignedAppBar_withoutSubtitle_lightTheme() {
        composeTestRule.setMaterialContent(lightColorScheme()) {
            Box(Modifier.testTag(TopAppBarTestTag)) {
                TopAppBar(
                    navigationIcon = {
                        IconButton(onClick = { /* doSomething() */ }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                            )
                        }
                    },
                    title = { Text("Title") },
                    subtitle = {},
                    titleHorizontalAlignment = Alignment.CenterHorizontally,
                    actions = {
                        IconButton(onClick = { /* doSomething() */ }) {
                            Icon(imageVector = Icons.Filled.Favorite, contentDescription = "Like")
                        }
                    },
                )
            }
        }

        assertAppBarAgainstGolden(
            goldenIdentifier = "centerAlignedAppBar_withoutSubtitle_lightTheme"
        )
    }

    @Test
    fun centerAlignedAppBar_darkTheme() {
        composeTestRule.setMaterialContent(darkColorScheme()) {
            Box(Modifier.testTag(TopAppBarTestTag)) {
                CenterAlignedTopAppBar(
                    navigationIcon = {
                        IconButton(onClick = { /* doSomething() */ }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                            )
                        }
                    },
                    title = { Text("Title") },
                    actions = {
                        IconButton(onClick = { /* doSomething() */ }) {
                            Icon(imageVector = Icons.Filled.Favorite, contentDescription = "Like")
                        }
                    },
                )
            }
        }

        assertAppBarAgainstGolden(goldenIdentifier = "centerAlignedAppBar_darkTheme")
    }

    @Test
    fun mediumAppBar_lightTheme() {
        composeTestRule.setMaterialContent(lightColorScheme()) {
            Box(Modifier.testTag(TopAppBarTestTag)) {
                MediumTopAppBar(
                    navigationIcon = {
                        IconButton(onClick = { /* doSomething() */ }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                            )
                        }
                    },
                    title = { Text("Title") },
                    actions = {
                        IconButton(onClick = { /* doSomething() */ }) {
                            Icon(imageVector = Icons.Filled.Favorite, contentDescription = "Like")
                        }
                    },
                )
            }
        }

        assertAppBarAgainstGolden(goldenIdentifier = "mediumAppBar_lightTheme")
    }

    @Test
    fun mediumFlexibleAppBar_centerAligned_withSubtitle_lightTheme() {
        composeTestRule.setMaterialContent(lightColorScheme()) {
            Box(Modifier.testTag(TopAppBarTestTag)) {
                MediumFlexibleTopAppBar(
                    navigationIcon = {
                        IconButton(onClick = { /* doSomething() */ }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                            )
                        }
                    },
                    title = { Text("Title") },
                    subtitle = { Text("Subtitle") },
                    titleHorizontalAlignment = Alignment.CenterHorizontally,
                    actions = {
                        IconButton(onClick = { /* doSomething() */ }) {
                            Icon(imageVector = Icons.Filled.Favorite, contentDescription = "Like")
                        }
                    },
                )
            }
        }

        assertAppBarAgainstGolden(
            goldenIdentifier = "mediumFlexibleAppBar_centerAligned_withSubtitle_lightTheme"
        )
    }

    @Test
    fun mediumFlexibleAppBar_centerAligned_withoutSubtitle_lightTheme() {
        composeTestRule.setMaterialContent(lightColorScheme()) {
            Box(Modifier.testTag(TopAppBarTestTag)) {
                MediumFlexibleTopAppBar(
                    navigationIcon = {
                        IconButton(onClick = { /* doSomething() */ }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                            )
                        }
                    },
                    title = { Text("Title") },
                    titleHorizontalAlignment = Alignment.CenterHorizontally,
                    actions = {
                        IconButton(onClick = { /* doSomething() */ }) {
                            Icon(imageVector = Icons.Filled.Favorite, contentDescription = "Like")
                        }
                    },
                )
            }
        }

        assertAppBarAgainstGolden(
            goldenIdentifier = "mediumFlexibleAppBar_centerAligned_withoutSubtitle_lightTheme"
        )
    }

    @Test
    fun mediumFlexibleAppBar_startAligned_withSubtitle_darkTheme() {
        composeTestRule.setMaterialContent(darkColorScheme()) {
            Box(Modifier.testTag(TopAppBarTestTag)) {
                MediumFlexibleTopAppBar(
                    navigationIcon = {
                        IconButton(onClick = { /* doSomething() */ }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                            )
                        }
                    },
                    title = { Text("Title") },
                    subtitle = { Text("Subtitle") },
                    actions = {
                        IconButton(onClick = { /* doSomething() */ }) {
                            Icon(imageVector = Icons.Filled.Favorite, contentDescription = "Like")
                        }
                    },
                )
            }
        }

        assertAppBarAgainstGolden(
            goldenIdentifier = "mediumFlexibleAppBar_startAligned_withSubtitle_darkTheme"
        )
    }

    @Test
    fun mediumAppBar_darkTheme() {
        composeTestRule.setMaterialContent(darkColorScheme()) {
            Box(Modifier.testTag(TopAppBarTestTag)) {
                MediumTopAppBar(
                    navigationIcon = {
                        IconButton(onClick = { /* doSomething() */ }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                            )
                        }
                    },
                    title = { Text("Title") },
                    actions = {
                        IconButton(onClick = { /* doSomething() */ }) {
                            Icon(imageVector = Icons.Filled.Favorite, contentDescription = "Like")
                        }
                    },
                )
            }
        }

        assertAppBarAgainstGolden(goldenIdentifier = "mediumAppBar_darkTheme")
    }

    @Test
    fun largeAppBar_lightTheme() {
        composeTestRule.setMaterialContent(lightColorScheme()) {
            Box(Modifier.testTag(TopAppBarTestTag)) {
                LargeTopAppBar(
                    navigationIcon = {
                        IconButton(onClick = { /* doSomething() */ }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                            )
                        }
                    },
                    title = { Text("Title") },
                    actions = {
                        IconButton(onClick = { /* doSomething() */ }) {
                            Icon(imageVector = Icons.Filled.Favorite, contentDescription = "Like")
                        }
                    },
                )
            }
        }

        assertAppBarAgainstGolden(goldenIdentifier = "largeAppBar_lightTheme")
    }

    @Test
    fun largeFlexibleAppBar_centerAligned_withSubtitle_lightTheme() {
        composeTestRule.setMaterialContent(lightColorScheme()) {
            Box(Modifier.testTag(TopAppBarTestTag)) {
                LargeFlexibleTopAppBar(
                    navigationIcon = {
                        IconButton(onClick = { /* doSomething() */ }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                            )
                        }
                    },
                    title = { Text("Title") },
                    subtitle = { Text("Subtitle") },
                    titleHorizontalAlignment = Alignment.CenterHorizontally,
                    actions = {
                        IconButton(onClick = { /* doSomething() */ }) {
                            Icon(imageVector = Icons.Filled.Favorite, contentDescription = "Like")
                        }
                    },
                )
            }
        }

        assertAppBarAgainstGolden(
            goldenIdentifier = "largeFlexibleAppBar_centerAligned_withSubtitle_lightTheme"
        )
    }

    @Test
    fun largeFlexibleAppBar_centerAligned_withoutSubtitle_lightTheme() {
        composeTestRule.setMaterialContent(lightColorScheme()) {
            Box(Modifier.testTag(TopAppBarTestTag)) {
                LargeFlexibleTopAppBar(
                    navigationIcon = {
                        IconButton(onClick = { /* doSomething() */ }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                            )
                        }
                    },
                    title = { Text("Title") },
                    titleHorizontalAlignment = Alignment.CenterHorizontally,
                    actions = {
                        IconButton(onClick = { /* doSomething() */ }) {
                            Icon(imageVector = Icons.Filled.Favorite, contentDescription = "Like")
                        }
                    },
                )
            }
        }

        assertAppBarAgainstGolden(
            goldenIdentifier = "largeFlexibleAppBar_centerAligned_withoutSubtitle_lightTheme"
        )
    }

    @Test
    fun largeFlexibleAppBar_startAligned_withSubtitle_darkTheme() {
        composeTestRule.setMaterialContent(darkColorScheme()) {
            Box(Modifier.testTag(TopAppBarTestTag)) {
                LargeFlexibleTopAppBar(
                    navigationIcon = {
                        IconButton(onClick = { /* doSomething() */ }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                            )
                        }
                    },
                    title = { Text("Title") },
                    subtitle = { Text("Subtitle") },
                    actions = {
                        IconButton(onClick = { /* doSomething() */ }) {
                            Icon(imageVector = Icons.Filled.Favorite, contentDescription = "Like")
                        }
                    },
                )
            }
        }

        assertAppBarAgainstGolden(
            goldenIdentifier = "largeFlexibleAppBar_startAligned_withSubtitle_darkTheme"
        )
    }

    @Test
    fun largeAppBar_darkTheme() {
        composeTestRule.setMaterialContent(darkColorScheme()) {
            Box(Modifier.testTag(TopAppBarTestTag)) {
                LargeTopAppBar(
                    navigationIcon = {
                        IconButton(onClick = { /* doSomething() */ }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                            )
                        }
                    },
                    title = { Text("Title") },
                    actions = {
                        IconButton(onClick = { /* doSomething() */ }) {
                            Icon(imageVector = Icons.Filled.Favorite, contentDescription = "Like")
                        }
                    },
                )
            }
        }

        assertAppBarAgainstGolden(goldenIdentifier = "largeAppBar_darkTheme")
    }

    @Test
    fun bottomAppBarWithFAB_lightTheme() {
        composeTestRule.setMaterialContent(lightColorScheme()) {
            Box(Modifier.testTag(BottomAppBarTestTag)) {
                BottomAppBar(
                    actions = {
                        IconButton(onClick = { /* doSomething() */ }) {
                            Icon(imageVector = Icons.Filled.Menu, contentDescription = "Menu")
                        }
                    },
                    floatingActionButton = {
                        FloatingActionButton(
                            onClick = { /* do something */ },
                            containerColor = bottomAppBarFabColor,
                            elevation = FloatingActionButtonDefaults.bottomAppBarFabElevation(),
                        ) {
                            Icon(Icons.Filled.Add, "Localized description")
                        }
                    },
                )
            }
        }

        assertAppBarAgainstGolden(
            goldenIdentifier = "bottomAppBarWithFAB_lightTheme",
            testTag = BottomAppBarTestTag,
        )
    }

    @Test
    fun bottomAppBarWithFAB_darkTheme() {
        composeTestRule.setMaterialContent(darkColorScheme()) {
            Box(Modifier.testTag(BottomAppBarTestTag)) {
                BottomAppBar(
                    actions = {
                        IconButton(onClick = { /* doSomething() */ }) {
                            Icon(imageVector = Icons.Filled.Menu, contentDescription = "Menu")
                        }
                    },
                    floatingActionButton = {
                        FloatingActionButton(
                            onClick = { /* do something */ },
                            containerColor = bottomAppBarFabColor,
                            elevation = FloatingActionButtonDefaults.bottomAppBarFabElevation(),
                        ) {
                            Icon(Icons.Filled.Add, "Localized description")
                        }
                    },
                )
            }
        }

        assertAppBarAgainstGolden(
            goldenIdentifier = "bottomAppBarWithFAB_darkTheme",
            testTag = BottomAppBarTestTag,
        )
    }

    @Test
    fun bottomAppBarSpacedAround_lightTheme() {
        composeTestRule.setMaterialContent(lightColorScheme()) {
            Box(Modifier.testTag(BottomAppBarTestTag)) {
                FlexibleBottomAppBar(
                    horizontalArrangement = Arrangement.SpaceAround,
                    contentPadding = PaddingValues(horizontal = 0.dp),
                    content = {
                        IconButton(onClick = { /* doSomething() */ }) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Localized description",
                            )
                        }
                        IconButton(onClick = { /* doSomething() */ }) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "Localized description",
                            )
                        }
                        FilledIconButton(
                            modifier = Modifier.width(56.dp),
                            onClick = { /* doSomething() */ },
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = "Localized description")
                        }
                        IconButton(onClick = { /* doSomething() */ }) {
                            Icon(Icons.Filled.Check, contentDescription = "Localized description")
                        }
                        IconButton(onClick = { /* doSomething() */ }) {
                            Icon(Icons.Filled.Edit, contentDescription = "Localized description")
                        }
                    },
                )
            }
        }

        assertAppBarAgainstGolden(
            goldenIdentifier = "bottomAppBarSpacedAround_lightTheme",
            testTag = BottomAppBarTestTag,
        )
    }

    @Test
    fun bottomAppBarSpacedBetween_lightTheme() {
        composeTestRule.setMaterialContent(lightColorScheme()) {
            Box(Modifier.testTag(BottomAppBarTestTag)) {
                FlexibleBottomAppBar(
                    // The default BottomAppBarDefaults.FlexibleHorizontalArrangement is an
                    // Arrangement.SpacedBetween.
                    content = {
                        IconButton(onClick = { /* doSomething() */ }) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Localized description",
                            )
                        }
                        IconButton(onClick = { /* doSomething() */ }) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "Localized description",
                            )
                        }
                        FilledIconButton(
                            modifier = Modifier.width(56.dp),
                            onClick = { /* doSomething() */ },
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = "Localized description")
                        }
                        IconButton(onClick = { /* doSomething() */ }) {
                            Icon(Icons.Filled.Check, contentDescription = "Localized description")
                        }
                        IconButton(onClick = { /* doSomething() */ }) {
                            Icon(Icons.Filled.Edit, contentDescription = "Localized description")
                        }
                    }
                )
            }
        }

        assertAppBarAgainstGolden(
            goldenIdentifier = "bottomAppBarSpacedBetween_lightTheme",
            testTag = BottomAppBarTestTag,
        )
    }

    @Test
    fun bottomAppBarSpacedEvenly_lightTheme() {
        composeTestRule.setMaterialContent(lightColorScheme()) {
            Box(Modifier.testTag(BottomAppBarTestTag)) {
                FlexibleBottomAppBar(
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    contentPadding = PaddingValues(horizontal = 0.dp),
                    content = {
                        IconButton(onClick = { /* doSomething() */ }) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Localized description",
                            )
                        }
                        IconButton(onClick = { /* doSomething() */ }) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "Localized description",
                            )
                        }
                        FilledIconButton(
                            modifier = Modifier.width(56.dp),
                            onClick = { /* doSomething() */ },
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = "Localized description")
                        }
                        IconButton(onClick = { /* doSomething() */ }) {
                            Icon(Icons.Filled.Check, contentDescription = "Localized description")
                        }
                        IconButton(onClick = { /* doSomething() */ }) {
                            Icon(Icons.Filled.Edit, contentDescription = "Localized description")
                        }
                    },
                )
            }
        }

        assertAppBarAgainstGolden(
            goldenIdentifier = "bottomAppBarSpacedEvenly_lightTheme",
            testTag = BottomAppBarTestTag,
        )
    }

    @Test
    fun bottomAppBarFixed_lightTheme() {
        composeTestRule.setMaterialContent(lightColorScheme()) {
            Box(Modifier.testTag(BottomAppBarTestTag)) {
                FlexibleBottomAppBar(
                    horizontalArrangement = BottomAppBarDefaults.FlexibleFixedHorizontalArrangement,
                    content = {
                        IconButton(onClick = { /* doSomething() */ }) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Localized description",
                            )
                        }
                        IconButton(onClick = { /* doSomething() */ }) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "Localized description",
                            )
                        }
                        FilledIconButton(
                            modifier = Modifier.width(56.dp),
                            onClick = { /* doSomething() */ },
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = "Localized description")
                        }
                        IconButton(onClick = { /* doSomething() */ }) {
                            Icon(Icons.Filled.Check, contentDescription = "Localized description")
                        }
                        IconButton(onClick = { /* doSomething() */ }) {
                            Icon(Icons.Filled.Edit, contentDescription = "Localized description")
                        }
                    },
                )
            }
        }

        assertAppBarAgainstGolden(
            goldenIdentifier = "bottomAppBarFixed_lightTheme",
            testTag = BottomAppBarTestTag,
        )
    }

    @Test
    fun bottomAppBarFixed_darkTheme() {
        composeTestRule.setMaterialContent(darkColorScheme()) {
            Box(Modifier.testTag(BottomAppBarTestTag)) {
                FlexibleBottomAppBar(
                    horizontalArrangement = BottomAppBarDefaults.FlexibleFixedHorizontalArrangement,
                    content = {
                        IconButton(onClick = { /* doSomething() */ }) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Localized description",
                            )
                        }
                        IconButton(onClick = { /* doSomething() */ }) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "Localized description",
                            )
                        }
                        FilledIconButton(
                            modifier = Modifier.width(56.dp),
                            onClick = { /* doSomething() */ },
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = "Localized description")
                        }
                        IconButton(onClick = { /* doSomething() */ }) {
                            Icon(Icons.Filled.Check, contentDescription = "Localized description")
                        }
                        IconButton(onClick = { /* doSomething() */ }) {
                            Icon(Icons.Filled.Edit, contentDescription = "Localized description")
                        }
                    },
                )
            }
        }

        assertAppBarAgainstGolden(
            goldenIdentifier = "bottomAppBarFixed_darkTheme",
            testTag = BottomAppBarTestTag,
        )
    }

    private fun assertAppBarAgainstGolden(
        goldenIdentifier: String,
        testTag: String = TopAppBarTestTag,
    ) {
        composeTestRule
            .onNodeWithTag(testTag)
            .captureToImage()
            .assertAgainstGolden(screenshotRule, goldenIdentifier)
    }

    private val TopAppBarTestTag = "topAppBar"
    private val BottomAppBarTestTag = "bottomAppBar"
}
