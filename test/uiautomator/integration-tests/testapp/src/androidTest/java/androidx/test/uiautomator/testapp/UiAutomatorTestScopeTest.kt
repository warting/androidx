/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.test.uiautomator.testapp

import android.widget.Button
import androidx.kruth.assertThat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.ViewNotFoundException
import androidx.test.uiautomator.onView
import androidx.test.uiautomator.onViewOrNull
import androidx.test.uiautomator.scrollUntilView
import androidx.test.uiautomator.uiAutomator
import androidx.test.uiautomator.waitForStable
import androidx.test.uiautomator.watcher.ScopedUiWatcher
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UiAutomatorTestScopeTest {

    companion object {
        private const val APP_PACKAGE_NAME = "androidx.test.uiautomator.testapp"
    }

    @Before
    fun setup() = uiAutomator {
        uiDevice.wakeUp()
        uiDevice.pressMenu()
        uiDevice.pressHome()
        uiDevice.setOrientationNatural()
    }

    @Test
    @LargeTest
    fun mainActivity() = uiAutomator {
        startActivity(MainActivity::class.java)

        onView { view.id == "button" }.click()
        onView { view.textAsString == "Accessible button" }.click()

        with(onView { view.id == "nested_elements" }) {
            onView { view.textAsString == "First Level" }
            onView { view.textAsString == "Second Level" }
            onView { view.textAsString == "Third Level" }
        }
    }

    @Test
    @LargeTest
    fun waitForStable() = uiAutomator {
        startActivity(MainActivity::class.java)
        waitForAppToBeVisible()
        activeWindow().waitForStable(stableTimeoutMs = 2000, stableIntervalMs = 1000)
    }

    @SdkSuppress(minSdkVersion = 26)
    @Test
    @LargeTest
    fun bySelectorTestActivity() = uiAutomator {
        startActivity(BySelectorTestActivity::class.java)

        with(onView { view.id == "selected" }) {
            assertThat(isSelected)
            click()
            assertThat(!isSelected)
        }

        onView {
            view.packageName == APP_PACKAGE_NAME &&
                view.id == "clazz" &&
                view.className == "android.widget.Button"
        }
    }

    @SdkSuppress(minSdkVersion = 26)
    @Test
    @LargeTest
    fun hintTestActivity() = uiAutomator {
        startActivity(HintTestActivity::class.java)
        onView { view.hintText == "sample_hint" }
    }

    @Test(expected = ViewNotFoundException::class)
    @LargeTest
    fun bySelectorTestActivityFail() = uiAutomator {
        startActivity(BySelectorTestActivity::class.java)

        onView { view.id == "clazz" && view.className == "android.widget.TextView" }
    }

    @Test
    @LargeTest
    fun composeTest() = uiAutomator {
        startActivity(ComposeTestActivity::class.java)

        onView { view.id == "top-text" }
        val button =
            onView { view.isScrollable }
                .scrollUntilView(Direction.DOWN) { view.className == Button::class.java.name }
        val textView = onView { view.textAsString == "Initial" }
        button.click()
        assertThat(textView.text).isEqualTo("Updated")
    }

    @Test
    @LargeTest
    fun dialogActivityTest() = uiAutomator {

        // Test when pressing yes
        startActivity(DialogActivity::class.java)
        val registrationYes = watchFor(MyDialog()) { clickYes() }
        onView { view.textAsString == "Show Dialog" }.click()
        onView { view.textAsString == "Dialog Result: Pressed Yes" }
        registrationYes.unregister()

        // Test when pressing no
        startActivity(DialogActivity::class.java)
        val registrationNo = watchFor(MyDialog()) { clickNo() }
        onView { view.textAsString == "Show Dialog" }.click()
        onView { view.textAsString == "Dialog Result: Pressed No" }
        registrationNo.unregister()
    }
}

// Define a dialog
class MyDialog : ScopedUiWatcher<MyDialog.Scope> {

    private val uiDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

    override fun isVisible(): Boolean =
        uiDevice.onViewOrNull(0) { view.textAsString == "Confirmation" } != null

    override fun scope() = Scope()

    inner class Scope {
        fun clickYes() = uiDevice.onView { view.textAsString == "Yes" }.click()

        fun clickNo() = uiDevice.onView { view.textAsString == "No" }.click()
    }
}
