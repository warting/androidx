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

package androidx.compose.integration.demos

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.Window
import androidx.activity.OnBackPressedCallback
import androidx.activity.OnBackPressedDispatcher
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.integration.demos.common.ActivityDemo
import androidx.compose.integration.demos.common.Demo
import androidx.compose.integration.demos.common.DemoCategory
import androidx.compose.integration.demos.settings.DecorFitsSystemWindowsEffect
import androidx.compose.integration.demos.settings.DecorFitsSystemWindowsSetting
import androidx.compose.integration.demos.settings.DynamicThemeSetting
import androidx.compose.integration.demos.settings.LayoutDirectionSetting
import androidx.compose.integration.demos.settings.SoftInputModeEffect
import androidx.compose.integration.demos.settings.SoftInputModeSetting
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.fragment.app.FragmentActivity

/**
 * Main [Activity] containing all Compose related demos.
 *
 * You can pass a specific demo's name as string extra "demoname" to launch this demo only. Read
 * this module's readme to learn more!
 */
@Suppress("DEPRECATION")
class DemoActivity : FragmentActivity() {
    lateinit var hostView: View
    lateinit var focusManager: FocusManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val rootDemo =
            when (val demoName = intent.getStringExtra(DEMO_NAME)) {
                null -> AllDemosCategory
                else -> Navigator.searchAllDemos(demoName)
            }

        ComposeView(this)
            .also { setContentView(it) }
            .setContent {
                hostView = LocalView.current
                focusManager = LocalFocusManager.current
                val activityStarter =
                    fun(demo: ActivityDemo<*>) {
                        startActivity(Intent(this, demo.activityClass.java))
                    }
                val navigator =
                    rememberSaveable(
                        saver = Navigator.Saver(rootDemo, onBackPressedDispatcher, activityStarter)
                    ) {
                        Navigator(rootDemo, onBackPressedDispatcher, activityStarter)
                    }

                SoftInputModeEffect(SoftInputModeSetting.asState().value, window)
                DecorFitsSystemWindowsEffect(
                    DecorFitsSystemWindowsSetting.asState().value,
                    hostView,
                    window,
                )

                CompositionLocalProvider(
                    LocalLayoutDirection provides LayoutDirectionSetting.asState().value
                ) {
                    DemoTheme(DynamicThemeSetting.asState().value, this.hostView, window) {
                        val filteringMode =
                            rememberSaveable(saver = FilterMode.Saver(onBackPressedDispatcher)) {
                                FilterMode(onBackPressedDispatcher)
                            }
                        val onStartFiltering = { filteringMode.isFiltering = true }
                        val onEndFiltering = { filteringMode.isFiltering = false }
                        DemoApp(
                            currentDemo = navigator.currentDemo,
                            backStackTitle = navigator.backStackTitle,
                            isFiltering = filteringMode.isFiltering,
                            onStartFiltering = onStartFiltering,
                            onEndFiltering = onEndFiltering,
                            onNavigateToDemo = { demo ->
                                if (filteringMode.isFiltering) {
                                    onEndFiltering()
                                    navigator.popAll()
                                }
                                navigator.navigateTo(demo)
                            },
                            canNavigateUp = !navigator.isRoot,
                            onNavigateUp = { onBackPressed() },
                            launchSettings = {
                                startActivity(Intent(this, DemoSettingsActivity::class.java))
                            },
                        )
                    }
                }
            }
    }

    companion object {
        const val DEMO_NAME = "demoname"
    }
}

@Composable
private fun DemoTheme(
    isDynamicThemeOn: Boolean,
    view: View,
    window: Window,
    content: @Composable () -> Unit,
) {
    val isDarkMode = isSystemInDarkTheme()

    @Suppress("NewApi")
    val colorScheme =
        if (isDynamicThemeOn) {
            val context = LocalContext.current
            if (isDarkMode) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        } else {
            if (isDarkMode) darkColorScheme() else lightColorScheme()
        }

    SideEffect {
        WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !isDarkMode
        WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !isDarkMode
        @Suppress("deprecation")
        window.statusBarColor = Color.Transparent.toArgb()
        @Suppress("deprecation")
        window.navigationBarColor = Color.Transparent.toArgb()
    }
    MaterialTheme(colorScheme = colorScheme, content = content)
}

private class Navigator
private constructor(
    private val backDispatcher: OnBackPressedDispatcher,
    private val launchActivityDemo: (ActivityDemo<*>) -> Unit,
    private val rootDemo: Demo,
    initialDemo: Demo,
    private val backStack: MutableList<Demo>,
) {
    constructor(
        rootDemo: Demo,
        backDispatcher: OnBackPressedDispatcher,
        launchActivityDemo: (ActivityDemo<*>) -> Unit,
    ) : this(backDispatcher, launchActivityDemo, rootDemo, rootDemo, mutableListOf<Demo>())

    private val onBackPressed =
        object : OnBackPressedCallback(false) {
                override fun handleOnBackPressed() {
                    popBackStack()
                }
            }
            .apply {
                isEnabled = !isRoot
                backDispatcher.addCallback(this)
            }

    private var _currentDemo by mutableStateOf(initialDemo)
    var currentDemo: Demo
        get() = _currentDemo
        private set(value) {
            _currentDemo = value
            onBackPressed.isEnabled = !isRoot
        }

    val isRoot: Boolean
        get() = backStack.isEmpty()

    val backStackTitle: String
        get() = (backStack.drop(1) + currentDemo).joinToString(separator = " > ") { it.title }

    fun navigateTo(demo: Demo) {
        if (demo is ActivityDemo<*>) {
            launchActivityDemo(demo)
        } else {
            backStack.add(currentDemo)
            currentDemo = demo
        }
    }

    fun popAll() {
        if (!isRoot) {
            backStack.clear()
            currentDemo = rootDemo
        }
    }

    private fun popBackStack() {
        currentDemo = backStack.removeAt(backStack.lastIndex)
    }

    companion object {
        fun Saver(
            rootDemo: Demo,
            backDispatcher: OnBackPressedDispatcher,
            launchActivityDemo: (ActivityDemo<*>) -> Unit,
        ): Saver<Navigator, *> =
            listSaver<Navigator, String>(
                save = { navigator ->
                    (navigator.backStack + navigator.currentDemo).map { it.title }
                },
                restore = { restored ->
                    require(restored.isNotEmpty()) { "no restored items" }
                    val backStack =
                        restored.mapTo(mutableListOf()) {
                            requireNotNull(findDemo(rootDemo, it)) { "no root demo" }
                        }
                    val initial = backStack.removeAt(backStack.lastIndex)
                    Navigator(backDispatcher, launchActivityDemo, rootDemo, initial, backStack)
                },
            )

        fun findDemo(demo: Demo, title: String): Demo? {
            if (demo.title == title) return demo
            if (demo is DemoCategory) {
                demo.demos.forEach { child ->
                    findDemo(child, title)?.let {
                        return it
                    }
                }
            }
            return null
        }

        fun searchAllDemos(demoName: String): Demo {
            val demos = mutableListOf<Demo>()
            demos.addDemos(AllDemosCategory, demoName, exact = true)
            if (demos.size == 1) return demos.single()

            require(demos.isEmpty()) {
                "${demos.size} demos have the demo name \"$demoName\", " +
                    "can't disambiguate between them."
            }

            demos.addDemos(AllDemosCategory, demoName, exact = false)
            if (demos.size == 1) return demos.single()

            val errorMessage =
                if (demos.isEmpty()) {
                    "No demo called \"$demoName\" could be found. " +
                        "Note substring matches are allowed."
                } else {
                    "Found multiple demos matching the substring \"$demoName\", " +
                        "please use a more specific substring. " +
                        "Matching demo names: ${demos.joinToString { "\"${it.title}\"" }}"
                }
            throw IllegalArgumentException(errorMessage)
        }

        private fun MutableList<Demo>.addDemos(demo: Demo, title: String, exact: Boolean = false) {
            if ((exact && demo.title == title) || (!exact && demo.title.contains(title))) {
                add(demo)
            }
            if (demo is DemoCategory) {
                demo.demos.forEach { addDemos(it, title, exact) }
            }
        }
    }
}

private class FilterMode(backDispatcher: OnBackPressedDispatcher, initialValue: Boolean = false) {

    private var _isFiltering by mutableStateOf(initialValue)

    private val onBackPressed =
        object : OnBackPressedCallback(false) {
                override fun handleOnBackPressed() {
                    isFiltering = false
                }
            }
            .apply {
                isEnabled = initialValue
                backDispatcher.addCallback(this)
            }

    var isFiltering
        get() = _isFiltering
        set(value) {
            _isFiltering = value
            onBackPressed.isEnabled = value
        }

    companion object {
        fun Saver(backDispatcher: OnBackPressedDispatcher) =
            Saver<FilterMode, Boolean>(
                save = { it.isFiltering },
                restore = { FilterMode(backDispatcher, it) },
            )
    }
}
