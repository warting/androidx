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

package androidx.camera.camera2.internal

import android.content.Context
import android.graphics.Point
import android.hardware.display.DisplayManager
import android.os.Build
import android.util.Size
import android.view.Display
import android.view.WindowManager
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.spy
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.internal.DoNotInstrument
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowDisplay
import org.robolectric.shadows.ShadowDisplayManager
import org.robolectric.shadows.ShadowDisplayManager.removeDisplay
import org.robolectric.util.ReflectionHelpers

@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Suppress("DEPRECATION") // getRealSize
class DisplayInfoManagerTest {

    private fun addDisplay(width: Int, height: Int, state: Int = Display.STATE_ON) {
        val displayStr = String.format("w%ddp-h%ddp", width, height)
        val displayId = ShadowDisplayManager.addDisplay(displayStr)
        if (state != Display.STATE_ON) {
            val displayManager =
                (ApplicationProvider.getApplicationContext() as Context).getSystemService(
                    Context.DISPLAY_SERVICE
                ) as DisplayManager
            (Shadow.extract(displayManager.getDisplay(displayId)) as ShadowDisplay).setState(state)
        }
    }

    @After
    fun tearDown() {
        DisplayInfoManager.releaseInstance()
    }

    @Test
    fun canReturnMaxSizeDisplay_oneDisplay() {
        // Arrange
        val displayManager =
            (ApplicationProvider.getApplicationContext() as Context).getSystemService(
                Context.DISPLAY_SERVICE
            ) as DisplayManager
        val currentDisplaySize = Point()
        displayManager.displays.get(0).getRealSize(currentDisplaySize)

        // Act
        val displayInfoManager =
            DisplayInfoManager.getInstance(ApplicationProvider.getApplicationContext())
        val size = Point()
        displayInfoManager.getMaxSizeDisplay(false).getRealSize(size)

        // Assert
        assertThat(size).isEqualTo(currentDisplaySize)
    }

    @Test
    fun canReturnMaxSizeDisplay_multipleDisplay() {
        // Arrange
        addDisplay(2000, 3000)
        addDisplay(480, 640)

        // Act
        val displayInfoManager =
            DisplayInfoManager.getInstance(ApplicationProvider.getApplicationContext())
        val size = Point()
        displayInfoManager.getMaxSizeDisplay(false).getRealSize(size)

        // Assert
        assertThat(size).isEqualTo(Point(2000, 3000))
    }

    @Test
    fun canReturnMaxSizeDisplay_offState() {
        // Arrange
        addDisplay(2000, 3000, Display.STATE_OFF)
        addDisplay(480, 640)
        addDisplay(200, 300, Display.STATE_OFF)

        // Act
        val displayInfoManager =
            DisplayInfoManager.getInstance(ApplicationProvider.getApplicationContext())
        val size = Point()
        displayInfoManager.getMaxSizeDisplay(true).getRealSize(size)

        // Assert
        assertThat(size).isEqualTo(Point(480, 640))
    }

    @Test(expected = IllegalArgumentException::class)
    fun canReturnMaxSizeDisplay_noDisplay() {
        // Arrange
        removeDisplay(0)

        // Act
        val displayInfoManager =
            DisplayInfoManager.getInstance(ApplicationProvider.getApplicationContext())
        val size = Point()
        displayInfoManager.getMaxSizeDisplay(false).getRealSize(size)
    }

    @Test
    fun canReturnMaxSizeDisplay_allOffState() {
        // Arrange
        addDisplay(480, 640, Display.STATE_OFF)
        addDisplay(200, 300, Display.STATE_OFF)
        addDisplay(2000, 3000, Display.STATE_OFF)
        removeDisplay(0)

        // Act
        val displayInfoManager =
            DisplayInfoManager.getInstance(ApplicationProvider.getApplicationContext())
        val size = Point()
        displayInfoManager.getMaxSizeDisplay(true).getRealSize(size)

        // Assert
        assertThat(size).isEqualTo(Point(2000, 3000))
    }

    @Test
    fun canReturnPreviewSize_displaySmallerThan1080P() {
        // Arrange
        addDisplay(480, 640)

        // Act & Assert
        val displayInfoManager =
            DisplayInfoManager.getInstance(ApplicationProvider.getApplicationContext())
        assertThat(displayInfoManager.previewSize).isEqualTo(Size(640, 480))
    }

    @Test
    fun canReturnPreviewSize_displayLargerThan1080P() {
        // Arrange
        addDisplay(2000, 3000)

        // Act & Assert
        val displayInfoManager =
            DisplayInfoManager.getInstance(ApplicationProvider.getApplicationContext())
        assertThat(displayInfoManager.previewSize).isEqualTo(Size(1920, 1080))
    }

    @Test
    fun canReturnDifferentPreviewSize_refreshIsCalled() {
        // Arrange
        val displayInfoManager =
            spy(DisplayInfoManager.getInstance(ApplicationProvider.getApplicationContext()))

        // Act
        displayInfoManager.previewSize
        displayInfoManager.refresh()
        displayInfoManager.previewSize

        // Assert
        verify(displayInfoManager, times(2)).getMaxSizeDisplay(false)
    }

    @Test
    fun canReturnFallbackPreviewSize640x480_displaySmallerThan320x240() {
        // Arrange
        DisplayInfoManager.releaseInstance()
        val windowManager =
            ApplicationProvider.getApplicationContext<Context>()
                .getSystemService(Context.WINDOW_SERVICE) as WindowManager
        Shadows.shadowOf(windowManager.defaultDisplay).setRealWidth(16)
        Shadows.shadowOf(windowManager.defaultDisplay).setRealHeight(16)

        // Act & Assert
        val displayInfoManager =
            DisplayInfoManager.getInstance(ApplicationProvider.getApplicationContext())
        assertThat(displayInfoManager.previewSize).isEqualTo(Size(640, 480))
    }

    @Test
    fun canReturnCorrectPreviewSize_fromDisplaySizeCorrector() {
        // Arrange
        DisplayInfoManager.releaseInstance()
        val windowManager =
            ApplicationProvider.getApplicationContext<Context>()
                .getSystemService(Context.WINDOW_SERVICE) as WindowManager
        Shadows.shadowOf(windowManager.defaultDisplay).setRealWidth(16)
        Shadows.shadowOf(windowManager.defaultDisplay).setRealHeight(16)

        ReflectionHelpers.setStaticField(Build::class.java, "MODEL", "SM-A127F")

        // Act & Assert
        val displayInfoManager =
            DisplayInfoManager.getInstance(ApplicationProvider.getApplicationContext())
        assertThat(displayInfoManager.previewSize).isEqualTo(Size(1600, 720))
    }
}
