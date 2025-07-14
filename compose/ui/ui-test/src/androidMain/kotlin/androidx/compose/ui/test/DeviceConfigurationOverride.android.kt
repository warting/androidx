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

package androidx.compose.ui.test

import android.content.res.Configuration
import android.util.DisplayMetrics
import android.view.ContextThemeWrapper
import android.view.View
import android.view.WindowInsets
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.createFontFamilyResolver
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.util.fastJoinToString
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.os.ConfigurationCompat
import androidx.core.os.LocaleListCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.children
import kotlin.math.floor

actual fun DeviceConfigurationOverride.Companion.ForcedSize(
    size: DpSize
): DeviceConfigurationOverride = DeviceConfigurationOverride { contentUnderTest ->
    // First override the density. Doing this first allows using the resulting density in the
    // overridden configuration.
    DensityForcedSize(size) {
        // Second, override the configuration, with the current configuration modified by the
        // resulting density
        OverriddenConfiguration(
            configuration =
                Configuration().apply {
                    // Initialize from the current configuration
                    updateFrom(LocalConfiguration.current)

                    // Override densityDpi
                    densityDpi =
                        floor(LocalDensity.current.density * DisplayMetrics.DENSITY_DEFAULT).toInt()
                },
            content = contentUnderTest,
        )
    }
}

actual fun DeviceConfigurationOverride.Companion.FontScale(
    fontScale: Float
): DeviceConfigurationOverride = DeviceConfigurationOverride { contentUnderTest ->
    OverriddenConfiguration(
        configuration =
            Configuration().apply {
                // Initialize from the current configuration
                updateFrom(LocalConfiguration.current)

                // Override font scale
                this.fontScale = fontScale
            },
        content = contentUnderTest,
    )
}

actual fun DeviceConfigurationOverride.Companion.LayoutDirection(
    layoutDirection: LayoutDirection
): DeviceConfigurationOverride = DeviceConfigurationOverride { contentUnderTest ->
    OverriddenConfiguration(
        configuration =
            Configuration().apply {
                // Initialize from the current configuration
                updateFrom(LocalConfiguration.current)

                // Override screen layout for layout direction
                screenLayout =
                    screenLayout and
                        Configuration.SCREENLAYOUT_LAYOUTDIR_MASK.inv() or
                        when (layoutDirection) {
                            LayoutDirection.Ltr -> Configuration.SCREENLAYOUT_LAYOUTDIR_LTR
                            LayoutDirection.Rtl -> Configuration.SCREENLAYOUT_LAYOUTDIR_RTL
                        }
            },
        content = contentUnderTest,
    )
}

/**
 * A [DeviceConfigurationOverride] that overrides the locales for the contained content.
 *
 * This will change resource resolution for the content under test, and also override the layout
 * direction as specified by the locales.
 *
 * @param locales the [LocaleList] to use for the content under test.
 * @return a [DeviceConfigurationOverride] that specifies the locales for the content under test.
 * @sample androidx.compose.ui.test.samples.DeviceConfigurationOverrideLocalesSample
 */
fun DeviceConfigurationOverride.Companion.Locales(
    locales: LocaleList
): DeviceConfigurationOverride = DeviceConfigurationOverride { contentUnderTest ->
    OverriddenConfiguration(
        configuration =
            Configuration().apply {
                // Initialize from the current configuration
                updateFrom(LocalConfiguration.current)

                // Update the locale list
                ConfigurationCompat.setLocales(
                    this,
                    LocaleListCompat.forLanguageTags(
                        locales.localeList.fastJoinToString(",", transform = Locale::toLanguageTag)
                    ),
                )
            },
        content = contentUnderTest,
    )
}

/**
 * A [DeviceConfigurationOverride] that overrides the dark mode or light mode theme for the
 * contained content. Inside the content under test, `isSystemInDarkTheme()` will return
 * [isDarkMode].
 *
 * @param isDarkMode if `true`, render content under test in dark mode.
 * @return a [DeviceConfigurationOverride] that specifies the dark mode for the content under test.
 * @sample androidx.compose.ui.test.samples.DeviceConfigurationOverrideDarkModeSample
 */
fun DeviceConfigurationOverride.Companion.DarkMode(
    isDarkMode: Boolean
): DeviceConfigurationOverride = DeviceConfigurationOverride { contentUnderTest ->
    OverriddenConfiguration(
        configuration =
            Configuration().apply {
                // Initialize from the current configuration
                updateFrom(LocalConfiguration.current)

                // Override dark mode
                uiMode =
                    uiMode and
                        Configuration.UI_MODE_NIGHT_MASK.inv() or
                        if (isDarkMode) {
                            Configuration.UI_MODE_NIGHT_YES
                        } else {
                            Configuration.UI_MODE_NIGHT_NO
                        }
            },
        content = contentUnderTest,
    )
}

/**
 * A [DeviceConfigurationOverride] that overrides the font weight adjustment for the contained
 * content.
 *
 * @param fontWeightAdjustment the font weight adjustment to use to render the content under test.
 * @return a [DeviceConfigurationOverride] that specifies the font weight adjustment for the content
 *   under test.
 * @sample androidx.compose.ui.test.samples.DeviceConfigurationOverrideFontWeightAdjustmentSample
 */
@RequiresApi(31)
fun DeviceConfigurationOverride.Companion.FontWeightAdjustment(
    fontWeightAdjustment: Int
): DeviceConfigurationOverride = DeviceConfigurationOverride { contentUnderTest ->
    OverriddenConfiguration(
        configuration =
            Configuration().apply {
                // Initialize from the current configuration
                updateFrom(LocalConfiguration.current)

                // Override fontWeightAdjustment
                this.fontWeightAdjustment = fontWeightAdjustment
            },
        content = contentUnderTest,
    )
}

/**
 * A [DeviceConfigurationOverride] that overrides whether the screen is round for the contained
 * content.
 *
 * @param isScreenRound if `true`, render content under test in a round screen.
 * @return a [DeviceConfigurationOverride] that specifies whether the screen is round for the
 *   content under test.
 * @sample androidx.compose.ui.test.samples.DeviceConfigurationOverrideRoundScreenSample
 */
@RequiresApi(23)
fun DeviceConfigurationOverride.Companion.RoundScreen(
    isScreenRound: Boolean
): DeviceConfigurationOverride = DeviceConfigurationOverride { contentUnderTest ->
    OverriddenConfiguration(
        configuration =
            Configuration().apply {
                // Initialize from the current configuration
                updateFrom(LocalConfiguration.current)

                // Override isRound in screenLayout
                screenLayout =
                    when (isScreenRound) {
                        true ->
                            (screenLayout and Configuration.SCREENLAYOUT_ROUND_MASK.inv()) or
                                Configuration.SCREENLAYOUT_ROUND_YES
                        false ->
                            (screenLayout and Configuration.SCREENLAYOUT_ROUND_MASK.inv()) or
                                Configuration.SCREENLAYOUT_ROUND_NO
                    }
            },
        content = contentUnderTest,
    )
}

/** Values corresponding to keyboard type constants in [Configuration]. */
@JvmInline
value class KeyboardType private constructor(internal val configValue: Int) {
    companion object {
        /**
         * No hardware keyboard for [Configuration.keyboard]
         *
         * @see Configuration.KEYBOARD_NOKEYS
         */
        val NoKeys = KeyboardType(Configuration.KEYBOARD_NOKEYS)

        /**
         * A keyboard type of `qwerty` for [Configuration.keyboard]
         *
         * @see Configuration.KEYBOARD_QWERTY
         */
        val Qwerty = KeyboardType(Configuration.KEYBOARD_QWERTY)

        /**
         * A keyboard type of `12key` for [Configuration.keyboard]
         *
         * @see Configuration.KEYBOARD_12KEY
         */
        val TwelveKey = KeyboardType(Configuration.KEYBOARD_12KEY)
    }
}

/**
 * A [DeviceConfigurationOverride] that overrides the current keyboard type.
 *
 * @param keyboardType the keyboard type to render content under test in.
 * @param isHidden if `true`, render the content under test with a hidden keyboard.
 * @param isHardKeyboardHidden if `true`, render the content under test with a hidden hard keyboard.
 * @return a [DeviceConfigurationOverride] that specifies the keyboard status for the content under
 *   test.
 * @sample androidx.compose.ui.test.samples.DeviceConfigurationOverrideKeyboard
 * @see [Configuration.keyboard]
 * @see [Configuration.keyboardHidden]
 * @see [Configuration.hardKeyboardHidden]
 */
fun DeviceConfigurationOverride.Companion.Keyboard(
    keyboardType: KeyboardType,
    isHardKeyboardHidden: Boolean = false,
    isHidden: Boolean = false,
): DeviceConfigurationOverride = DeviceConfigurationOverride { contentUnderTest ->
    OverriddenConfiguration(
        configuration =
            Configuration().apply {
                // Initialize from the current configuration
                updateFrom(LocalConfiguration.current)

                keyboard = keyboardType.configValue
                hardKeyboardHidden =
                    if (isHardKeyboardHidden) {
                        Configuration.HARDKEYBOARDHIDDEN_YES
                    } else {
                        Configuration.HARDKEYBOARDHIDDEN_NO
                    }
                keyboardHidden =
                    if (isHidden) {
                        Configuration.KEYBOARDHIDDEN_YES
                    } else {
                        Configuration.KEYBOARDHIDDEN_NO
                    }
            },
        content = contentUnderTest,
    )
}

/** Values corresponding to navigation type constants in [Configuration]. */
@JvmInline
value class NavigationType private constructor(internal val configValue: Int) {
    companion object {
        /**
         * A navigation type of `dpad` for [Configuration.navigation]
         *
         * @see Configuration.NAVIGATION_DPAD
         */
        val Dpad = NavigationType(Configuration.NAVIGATION_DPAD)

        /**
         * A navigation type of `wheel` for [Configuration.navigation]
         *
         * @see Configuration.NAVIGATION_WHEEL
         */
        val Wheel = NavigationType(Configuration.NAVIGATION_WHEEL)

        /**
         * No navigation type for [Configuration.navigation]
         *
         * @see Configuration.NAVIGATION_NONAV
         */
        val NoNav = NavigationType(Configuration.NAVIGATION_NONAV)

        /**
         * A navigation type of `trackball` for [Configuration.navigation]
         *
         * @see Configuration.NAVIGATION_TRACKBALL
         */
        val Trackball = NavigationType(Configuration.NAVIGATION_TRACKBALL)
    }
}

/**
 * A [DeviceConfigurationOverride] that overrides the current navigation type and whether it is
 * hidden.
 *
 * @param navigationType the navigation type to render the content under test in.
 * @param isHidden if `true`, render the content under test with hidden navigation.
 * @return a [DeviceConfigurationOverride] that specifies the navigation type for the content under
 *   test.
 * @sample androidx.compose.ui.test.samples.DeviceConfigurationOverrideNavigation
 * @see [Configuration.navigation]
 * @see [Configuration.navigationHidden]
 */
fun DeviceConfigurationOverride.Companion.Navigation(
    navigationType: NavigationType,
    isHidden: Boolean = false,
): DeviceConfigurationOverride = DeviceConfigurationOverride { contentUnderTest ->
    OverriddenConfiguration(
        configuration =
            Configuration().apply {
                // Initialize from the current configuration
                updateFrom(LocalConfiguration.current)

                navigation = navigationType.configValue
                navigationHidden =
                    if (isHidden) {
                        Configuration.NAVIGATIONHIDDEN_YES
                    } else {
                        Configuration.NAVIGATIONHIDDEN_NO
                    }
            },
        content = contentUnderTest,
    )
}

/**
 * A [DeviceConfigurationOverride] that overrides the current touchscreen type.
 *
 * @sample androidx.compose.ui.test.samples.DeviceConfigurationOverrideTouchscreen
 */
fun DeviceConfigurationOverride.Companion.Touchscreen(
    isTouchScreen: Boolean
): DeviceConfigurationOverride = DeviceConfigurationOverride { contentUnderTest ->
    OverriddenConfiguration(
        configuration =
            Configuration().apply {
                // Initialize from the current configuration
                updateFrom(LocalConfiguration.current)

                touchscreen =
                    if (isTouchScreen) {
                        Configuration.TOUCHSCREEN_FINGER
                    } else {
                        Configuration.TOUCHSCREEN_NOTOUCH
                    }
            },
        content = contentUnderTest,
    )
}

/** Values corresponding to UI mode type constants in [Configuration]. */
@JvmInline
value class UiModeType private constructor(internal val configValue: Int) {
    companion object {
        /**
         * A uiMode type of `appliance` for the [Configuration.UI_MODE_TYPE_MASK] portion of
         * [Configuration.uiMode].
         *
         * @see Configuration.UI_MODE_TYPE_APPLIANCE
         */
        val Appliance = UiModeType(Configuration.UI_MODE_TYPE_APPLIANCE)

        /**
         * A uiMode type of `car` for the [Configuration.UI_MODE_TYPE_MASK] portion of
         * [Configuration.uiMode].
         *
         * @see Configuration.UI_MODE_TYPE_CAR
         */
        val Car = UiModeType(Configuration.UI_MODE_TYPE_CAR)

        /**
         * A uiMode type of `desk` for the [Configuration.UI_MODE_TYPE_MASK] portion of
         * [Configuration.uiMode].
         *
         * @see Configuration.UI_MODE_TYPE_DESK
         */
        val Desk = UiModeType(Configuration.UI_MODE_TYPE_DESK)

        /**
         * No uiMode type for the [Configuration.UI_MODE_TYPE_MASK] portion of
         * [Configuration.uiMode].
         *
         * @see Configuration.UI_MODE_TYPE_NORMAL
         */
        val Normal = UiModeType(Configuration.UI_MODE_TYPE_NORMAL)

        /**
         * A uiMode type of `television` for the [Configuration.UI_MODE_TYPE_MASK] portion of
         * [Configuration.uiMode].
         *
         * @see Configuration.UI_MODE_TYPE_TELEVISION
         */
        val Television = UiModeType(Configuration.UI_MODE_TYPE_TELEVISION)

        /**
         * A uiMode type of `vrheadset` for the [Configuration.UI_MODE_TYPE_MASK] portion of
         * [Configuration.uiMode].
         *
         * @see Configuration.UI_MODE_TYPE_VR_HEADSET
         */
        val VrHeadset
            @RequiresApi(26) get() = UiModeType(Configuration.UI_MODE_TYPE_VR_HEADSET)

        /**
         * A uiMode type of `watch` for the [Configuration.UI_MODE_TYPE_MASK] portion of
         * [Configuration.uiMode].
         *
         * @see Configuration.UI_MODE_TYPE_WATCH
         */
        val Watch = UiModeType(Configuration.UI_MODE_TYPE_WATCH)
    }
}

/**
 * A [DeviceConfigurationOverride] that overrides the current navigation type.
 *
 * @param uiModeType the uiMode type to render the content under test in.
 * @return a [DeviceConfigurationOverride] that specifies the uiMode type for the content under
 *   test.
 * @sample androidx.compose.ui.test.samples.DeviceConfigurationOverrideUiMode
 */
fun DeviceConfigurationOverride.Companion.UiMode(
    uiModeType: UiModeType
): DeviceConfigurationOverride = DeviceConfigurationOverride { contentUnderTest ->
    OverriddenConfiguration(
        configuration =
            Configuration().apply {
                // Initialize from the current configuration
                updateFrom(LocalConfiguration.current)

                uiMode =
                    (uiMode and Configuration.UI_MODE_TYPE_MASK.inv()) or uiModeType.configValue
            },
        content = contentUnderTest,
    )
}

/**
 * A [DeviceConfigurationOverride] that overrides the window insets for the contained content.
 *
 * @param windowInsets the [WindowInsetsCompat] to render the content under test in.
 * @return a [DeviceConfigurationOverride] that specifies the window insets for the content under
 *   test.
 * @sample androidx.compose.ui.test.samples.DeviceConfigurationOverrideWindowInsetsSample
 */
fun DeviceConfigurationOverride.Companion.WindowInsets(
    windowInsets: WindowInsetsCompat
): DeviceConfigurationOverride = DeviceConfigurationOverride { contentUnderTest ->
    val currentContentUnderTest by rememberUpdatedState(contentUnderTest)
    val currentWindowInsets by rememberUpdatedState(windowInsets)
    AndroidView(
        factory = { context ->
            object : AbstractComposeView(context) {
                @Composable
                override fun Content() {
                    currentContentUnderTest()
                }

                override fun dispatchApplyWindowInsets(insets: WindowInsets): WindowInsets {
                    children.forEach {
                        it.dispatchApplyWindowInsets(
                            WindowInsets(currentWindowInsets.toWindowInsets())
                        )
                    }
                    return WindowInsetsCompat.CONSUMED.toWindowInsets()!!
                }

                /**
                 * Deprecated, but intercept the `requestApplyInsets` call via the deprecated
                 * method.
                 */
                @Deprecated("Deprecated in Java")
                override fun requestFitSystemWindows() {
                    dispatchApplyWindowInsets(WindowInsets(currentWindowInsets.toWindowInsets()!!))
                }
            }
        },
        update = { with(currentWindowInsets) { it.requestApplyInsets() } },
    )
}

/**
 * Overrides the compositions locals related to the given [configuration].
 *
 * There currently isn't a single source of truth for these values, so we update them all according
 * to the given [configuration].
 */
@Composable
private fun OverriddenConfiguration(configuration: Configuration, content: @Composable () -> Unit) {
    // We don't override the theme, but we do want to override the configuration and this seems
    // convenient to do so
    val newContext =
        ContextThemeWrapper(LocalContext.current, 0).apply {
            applyOverrideConfiguration(configuration)
        }

    CompositionLocalProvider(
        LocalContext provides newContext,
        LocalConfiguration provides configuration,
        LocalLayoutDirection provides
            if (configuration.layoutDirection == View.LAYOUT_DIRECTION_LTR) {
                LayoutDirection.Ltr
            } else {
                LayoutDirection.Rtl
            },
        LocalDensity provides
            Density(
                configuration.densityDpi.toFloat() / DisplayMetrics.DENSITY_DEFAULT,
                configuration.fontScale,
            ),
        LocalFontFamilyResolver provides createFontFamilyResolver(newContext),
        content = content,
    )
}
