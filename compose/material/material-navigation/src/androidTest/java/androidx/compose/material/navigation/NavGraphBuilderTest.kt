/*
 * Copyright 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.compose.material.navigation

import android.net.Uri
import androidx.compose.material.ModalBottomSheetState
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.core.net.toUri
import androidx.navigation.NavDeepLinkRequest
import androidx.navigation.compose.NavHost
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import androidx.navigation.plusAssign
import androidx.navigation.testing.TestNavHostController
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth.assertThat
import kotlinx.serialization.Serializable
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
internal class NavGraphBuilderTest {
    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun testCurrentBackStackEntryNavigate() {
        lateinit var navController: TestNavHostController
        val key = "key"
        val arg = "myarg"
        composeTestRule.setContent {
            navController = TestNavHostController(LocalContext.current)
            navController.navigatorProvider += createBottomSheetNavigator()

            NavHost(navController, startDestination = firstRoute) {
                bottomSheet(firstRoute) {}
                bottomSheet("$secondRoute/{$key}") {}
            }
        }

        composeTestRule.runOnUiThread {
            navController.navigate("$secondRoute/$arg")
            assertThat(navController.currentBackStackEntry!!.arguments!!.getString(key))
                .isEqualTo(arg)
        }
    }

    @Test
    fun testCurrentBackStackEntryNavigateKClass() {
        lateinit var navController: TestNavHostController
        composeTestRule.setContent {
            navController = TestNavHostController(LocalContext.current)
            navController.navigatorProvider += createBottomSheetNavigator()

            NavHost(navController, startDestination = firstRoute) {
                bottomSheet(firstRoute) {}
                bottomSheet<TestClassArg> {}
            }
        }

        composeTestRule.runOnUiThread {
            navController.navigate(TestClassArg(15))
            assertThat(navController.currentBackStackEntry!!.arguments!!.getInt("arg"))
                .isEqualTo(15)
        }
    }

    @Test
    fun testDefaultArguments() {
        lateinit var navController: TestNavHostController
        val key = "key"
        val defaultArg = "default"
        composeTestRule.setContent {
            navController = TestNavHostController(LocalContext.current)
            navController.navigatorProvider += createBottomSheetNavigator()

            NavHost(navController, startDestination = firstRoute) {
                bottomSheet(firstRoute) {}
                bottomSheet(
                    secondRoute,
                    arguments = listOf(navArgument(key) { defaultValue = defaultArg })
                ) {}
            }
        }

        composeTestRule.runOnUiThread {
            navController.navigate(secondRoute)
            assertThat(navController.currentBackStackEntry!!.arguments!!.getString(key))
                .isEqualTo(defaultArg)
        }
    }

    @Test
    fun testDefaultArgumentsKClass() {
        lateinit var navController: TestNavHostController
        composeTestRule.setContent {
            navController = TestNavHostController(LocalContext.current)
            navController.navigatorProvider += createBottomSheetNavigator()

            NavHost(navController, startDestination = firstRoute) {
                bottomSheet(firstRoute) {}
                bottomSheet<TestClassArg> {}
            }
        }

        composeTestRule.runOnUiThread {
            navController.navigate(TestClassArg())
            assertThat(navController.currentBackStackEntry!!.arguments!!.getInt("arg")).isEqualTo(1)
        }
    }

    @Test
    fun testDeepLink() {
        lateinit var navController: TestNavHostController
        val uriString = "https://www.example.com"
        val deeplink = NavDeepLinkRequest.Builder.fromUri(Uri.parse(uriString)).build()
        composeTestRule.setContent {
            navController = TestNavHostController(LocalContext.current)
            navController.navigatorProvider += createBottomSheetNavigator()

            NavHost(navController, startDestination = firstRoute) {
                bottomSheet(firstRoute) {}
                bottomSheet(
                    secondRoute,
                    deepLinks = listOf(navDeepLink { uriPattern = uriString })
                ) {}
            }
        }

        composeTestRule.runOnUiThread {
            navController.navigate(uriString.toUri())
            assertThat(navController.currentBackStackEntry!!.destination.hasDeepLink(deeplink))
                .isTrue()
        }
    }

    @Test
    fun testDeepLinkKClass() {
        lateinit var navController: TestNavHostController
        val uriString = "https://www.example.com"
        val deeplink = NavDeepLinkRequest.Builder.fromUri(Uri.parse(uriString)).build()
        composeTestRule.setContent {
            navController = TestNavHostController(LocalContext.current)
            navController.navigatorProvider += createBottomSheetNavigator()

            NavHost(navController, startDestination = firstRoute) {
                bottomSheet(firstRoute) {}
                bottomSheet<TestClass>(
                    deepLinks = listOf(navDeepLink<TestClass>(basePath = "www.example.com"))
                ) {}
            }
        }

        composeTestRule.runOnUiThread {
            navController.navigate(TestClass())
            assertThat(navController.currentBackStackEntry!!.destination.hasDeepLink(deeplink))
                .isTrue()
        }
    }

    private fun createBottomSheetNavigator() =
        BottomSheetNavigator(
            sheetState =
                ModalBottomSheetState(ModalBottomSheetValue.Hidden, composeTestRule.density)
        )
}

private const val firstRoute = "first"
private const val secondRoute = "second"

@Serializable private class TestClass

@Serializable private class TestClassArg(val arg: Int = 1)
