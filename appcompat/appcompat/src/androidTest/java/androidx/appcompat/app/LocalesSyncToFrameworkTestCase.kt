/*
 * Copyright 2022 The Android Open Source Project
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

@file:Suppress("deprecation")

package androidx.appcompat.app

import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.LocaleList
import androidx.appcompat.testutils.LocalesActivityTestRule
import androidx.appcompat.testutils.LocalesUtils.CUSTOM_LOCALE_LIST
import androidx.appcompat.testutils.LocalesUtils.assertConfigurationLocalesEquals
import androidx.core.app.AppLocalesStorageHelper
import androidx.core.os.LocaleListCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import junit.framework.Assert.assertEquals
import junit.framework.Assert.assertNull
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Test case to verify app-locales sync to framework on Version upgrade from Pre T to T. */
@LargeTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 33, maxSdkVersion = 33)
class LocalesSyncToFrameworkTestCase {
    @get:Rule val rule = LocalesActivityTestRule(LocalesUpdateActivity::class.java)
    private var systemLocales = LocaleListCompat.getEmptyLocaleList()
    private var expectedLocales = LocaleListCompat.getEmptyLocaleList()
    private lateinit var appLocalesComponent: ComponentName
    private val instrumentation = InstrumentationRegistry.getInstrumentation()

    @Before
    fun setUp() {
        // setting the app to follow system.
        AppCompatDelegate.Api33Impl.localeManagerSetApplicationLocales(
            AppCompatDelegate.getLocaleManagerForApplication(),
            LocaleList.getEmptyLocaleList(),
        )

        // Since no locales are applied as of now, current configuration will have system
        // locales.
        systemLocales =
            LocalesUpdateActivity.getConfigLocales(rule.activity.resources.configuration)
        expectedLocales =
            LocalesUpdateActivity.overlayCustomAndSystemLocales(CUSTOM_LOCALE_LIST, systemLocales)

        appLocalesComponent =
            ComponentName(
                instrumentation.context,
                AppCompatDelegate.APP_LOCALES_META_DATA_HOLDER_SERVICE_NAME,
            )
    }

    @Test
    fun testAutoSync_preTToPostT_syncsSuccessfully() {
        if (Build.VERSION.SDK_INT == 33 && Build.VERSION.CODENAME != "REL") {
            return // b/262909049: Do not run this test on pre-release Android U.
        }

        val firstActivity = rule.activity

        // activity is following the system and the requested locales are null.
        assertConfigurationLocalesEquals(systemLocales, firstActivity)
        assertNull(AppCompatDelegate.getRequestedAppLocales())

        val context = instrumentation.context

        // persist some app locales in storage, mimicking locales set using the backward
        // compatibility API
        AppCompatDelegate.setIsAutoStoreLocalesOptedIn(true)
        AppLocalesStorageHelper.persistLocales(context, CUSTOM_LOCALE_LIST.toLanguageTags())

        // explicitly disable appLocalesComponent that acts as a marker to represent that the
        // locales has been synced so that when a new activity is created the locales are
        // synced from storage
        context.packageManager.setComponentEnabledSetting(
            appLocalesComponent,
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            /* flags= */ PackageManager.DONT_KILL_APP,
        )

        // resetting static storage represents a fresh app start up.
        AppCompatDelegate.resetStaticRequestedAndStoredLocales()

        // Start a new Activity, so that the original Activity goes into the background
        val intent =
            Intent(firstActivity, AppCompatActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        val secondActivity = instrumentation.startActivitySync(intent) as AppCompatActivity

        // wait for locales to get synced, stop execution of the current thread for the
        // timeout period
        Thread.sleep(/* timeout= */ 1000)

        // check that the locales were set using the framework API and they have been synced
        // successfully
        assertEquals(
            CUSTOM_LOCALE_LIST.toLanguageTags(),
            AppCompatDelegate.Api33Impl.localeManagerGetApplicationLocales(
                    AppCompatDelegate.getLocaleManagerForApplication()
                )
                .toLanguageTags(),
        )
        // check that the activity has the app specific locales
        assertConfigurationLocalesEquals(expectedLocales, secondActivity)
        // check that the override was not done by AndroidX, but by the framework
        assertNull(AppCompatDelegate.getRequestedAppLocales())
        // check that the synced marker was set to true
        assertEquals(
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
            context.packageManager.getComponentEnabledSetting(appLocalesComponent),
        )

        AppCompatDelegate.setIsAutoStoreLocalesOptedIn(false)
    }

    @After
    fun teardown() {
        val context = instrumentation.context

        AppCompatDelegate.setIsAutoStoreLocalesOptedIn(true)
        // setting empty locales deletes the persisted locales record.
        AppLocalesStorageHelper.persistLocales(context, /* empty locales */ "")
        AppCompatDelegate.setIsAutoStoreLocalesOptedIn(false)

        // clearing locales from framework.
        // setting the app to follow system.
        AppCompatDelegate.Api33Impl.localeManagerSetApplicationLocales(
            AppCompatDelegate.getLocaleManagerForApplication(),
            LocaleList.getEmptyLocaleList(),
        )

        // disabling component enabled setting for app_locales sync marker.
        context.packageManager.setComponentEnabledSetting(
            appLocalesComponent,
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            /* flags= */ PackageManager.DONT_KILL_APP,
        )
    }
}
