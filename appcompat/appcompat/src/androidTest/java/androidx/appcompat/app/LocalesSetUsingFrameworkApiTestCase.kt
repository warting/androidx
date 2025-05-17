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

import android.os.LocaleList
import androidx.appcompat.testutils.LocalesActivityTestRule
import androidx.appcompat.testutils.LocalesUtils
import androidx.appcompat.testutils.LocalesUtils.CUSTOM_LOCALE_LIST
import androidx.appcompat.testutils.LocalesUtils.assertConfigurationLocalesEquals
import androidx.core.os.LocaleListCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import junit.framework.Assert.assertEquals
import junit.framework.Assert.assertNull
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 33)
class LocalesSetUsingFrameworkApiTestCase {
    @get:Rule val rule = LocalesActivityTestRule(LocalesUpdateActivity::class.java)
    private var systemLocales = LocaleListCompat.getEmptyLocaleList()
    private var expectedLocales = LocaleListCompat.getEmptyLocaleList()

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
            LocalesUpdateActivity.overlayCustomAndSystemLocales(
                LocalesUtils.CUSTOM_LOCALE_LIST,
                systemLocales,
            )
    }

    /**
     * Verifies that for API version >=T the AppCompatDelegate.setApplicationLocales() call is
     * redirected to the framework API and the locales are applied successfully.
     */
    @Test
    fun testSetApplicationLocales_postT_frameworkApiCalled() {
        val firstActivity = rule.activity
        assertConfigurationLocalesEquals(systemLocales, firstActivity)

        assertEquals(
            LocaleListCompat.getEmptyLocaleList(),
            AppCompatDelegate.getApplicationLocales(),
        )
        assertNull(AppCompatDelegate.getRequestedAppLocales())

        // Now change the locales for the activity
        val recreatedFirst =
            LocalesUtils.setLocalesAndWaitForRecreate(firstActivity, CUSTOM_LOCALE_LIST)

        assertEquals(CUSTOM_LOCALE_LIST, AppCompatDelegate.getApplicationLocales())
        // check that the locales were set using the framework API
        assertEquals(
            CUSTOM_LOCALE_LIST.toLanguageTags(),
            AppCompatDelegate.Api33Impl.localeManagerGetApplicationLocales(
                    AppCompatDelegate.getLocaleManagerForApplication()
                )
                .toLanguageTags(),
        )
        // check locales are applied successfully
        assertConfigurationLocalesEquals(expectedLocales, recreatedFirst)
        // check that the override was not done by AndroidX, but by the framework
        assertNull(AppCompatDelegate.getRequestedAppLocales())
    }

    @After
    fun teardown() {
        // clearing locales from framework. setting the app to follow system.
        AppCompatDelegate.Api33Impl.localeManagerSetApplicationLocales(
            AppCompatDelegate.getLocaleManagerForApplication(),
            LocaleList.getEmptyLocaleList(),
        )
    }
}
