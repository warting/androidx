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

package androidx.xr.runtime.openxr

import android.app.Activity
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.xr.runtime.Config
import androidx.xr.runtime.internal.PermissionNotGrantedException
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// TODO - b/382119583: Remove the @SdkSuppress annotation once "androidx.xr.runtime.openxr.test"
// supports a
// lower SDK version.
@SdkSuppress(minSdkVersion = 29)
@LargeTest
@RunWith(AndroidJUnit4::class)
class OpenXrManagerTest {

    companion object {
        init {
            System.loadLibrary("androidx.xr.runtime.openxr.test")
        }
    }

    @get:Rule val activityRule = ActivityScenarioRule(Activity::class.java)

    private lateinit var underTest: OpenXrManager
    private lateinit var perceptionManager: OpenXrPerceptionManager
    private lateinit var timeSource: OpenXrTimeSource

    @Before
    fun setUp() {
        timeSource = OpenXrTimeSource()
        perceptionManager = OpenXrPerceptionManager(timeSource)
    }

    @Test
    fun create_initializesNativeOpenXrManager() = initOpenXrManagerAndRunTest {
        check(underTest.nativePointer == 0L)

        underTest.create()

        assertThat(underTest.nativePointer).isGreaterThan(0L)
    }

    @Test
    fun create_afterStop_initializesNativeOpenXrManager() = initOpenXrManagerAndRunTest {
        underTest.create()
        underTest.stop()
        check(underTest.nativePointer == 0L)

        underTest.create()

        assertThat(underTest.nativePointer).isGreaterThan(0L)
    }

    @Test
    fun configure_handTrackingEnabled_addsHandToUpdatables() = initOpenXrManagerAndRunTest {
        underTest.create()
        check(underTest.config.handTracking == Config.HandTrackingMode.DISABLED)
        check(perceptionManager.xrResources.updatables.isEmpty())

        underTest.configure(Config(handTracking = Config.HandTrackingMode.BOTH))

        assertThat(perceptionManager.xrResources.updatables)
            .containsExactly(
                perceptionManager.xrResources.leftHand,
                perceptionManager.xrResources.rightHand,
            )
    }

    @Test
    fun configure_handTrackingDisabled_removesHandsFromUpdatables() = initOpenXrManagerAndRunTest {
        underTest.create()
        underTest.configure(Config(handTracking = Config.HandTrackingMode.BOTH))
        check(
            perceptionManager.xrResources.updatables.containsAll(
                listOf(
                    perceptionManager.xrResources.leftHand,
                    perceptionManager.xrResources.rightHand,
                )
            )
        )

        underTest.configure(Config(handTracking = Config.HandTrackingMode.DISABLED))

        assertThat(perceptionManager.xrResources.updatables)
            .doesNotContain(perceptionManager.xrResources.leftHand)
        assertThat(perceptionManager.xrResources.updatables)
            .doesNotContain(perceptionManager.xrResources.rightHand)
    }

    @Test
    fun configure_deviceTrackingEnabled_addsDeviceToUpdatables() = initOpenXrManagerAndRunTest {
        underTest.create()
        check(underTest.config.deviceTracking == Config.DeviceTrackingMode.DISABLED)
        check(perceptionManager.xrResources.updatables.isEmpty())

        underTest.configure(Config(deviceTracking = Config.DeviceTrackingMode.LAST_KNOWN))

        assertThat(perceptionManager.xrResources.updatables)
            .containsExactly(perceptionManager.xrResources.arDevice)
    }

    @Test
    fun configure_deviceTrackingDisabled_removesDeviceToUpdatables() = initOpenXrManagerAndRunTest {
        underTest.create()
        underTest.configure(Config(deviceTracking = Config.DeviceTrackingMode.LAST_KNOWN))
        check(
            perceptionManager.xrResources.updatables.contains(
                perceptionManager.xrResources.arDevice
            )
        )

        underTest.configure(Config(deviceTracking = Config.DeviceTrackingMode.DISABLED))

        assertThat(perceptionManager.xrResources.updatables)
            .doesNotContain(perceptionManager.xrResources.arDevice)
    }

    // TODO(b/392660855): Add a test for all APIs gated by a feature that needs to be configured.
    @Test
    fun configure_withSufficientPermissions_doesNotThrowException() = initOpenXrManagerAndRunTest {
        underTest.create()

        underTest.configure(
            Config(
                Config.PlaneTrackingMode.HORIZONTAL_AND_VERTICAL,
                Config.HandTrackingMode.BOTH,
                Config.HeadTrackingMode.DISABLED,
                Config.DepthEstimationMode.DISABLED,
                Config.AnchorPersistenceMode.LOCAL,
            )
        )
    }

    @Test
    // TODO - b/346615429: Control the values returned by the OpenXR stub instead of relying on the
    // stub's current implementation.
    fun configure_insufficientPermissions_throwsPermissionNotGrantedException() =
        initOpenXrManagerAndRunTest {
            underTest.create()

            // The OpenXR stub returns `XR_ERROR_PERMISSION_INSUFFICIENT` when calling
            // `xrEnumerateDepthResolutionsANDROID` which is triggered by attempting to enable the
            // DepthEstimation feature.
            assertThrows(PermissionNotGrantedException::class.java) {
                underTest.configure(
                    Config(
                        Config.PlaneTrackingMode.DISABLED,
                        Config.HandTrackingMode.DISABLED,
                        Config.HeadTrackingMode.DISABLED,
                        Config.DepthEstimationMode.SMOOTH_AND_RAW,
                        Config.AnchorPersistenceMode.DISABLED,
                    )
                )
            }
        }

    @Test
    fun configure_withoutCreate_throwsIllegalStateException() = initOpenXrManagerAndRunTest {
        // The OpenXR stub returns `XR_ERROR_HANDLE_INVALID` if the `xrSession` has not been
        // initialized
        // by `OpenXrManager.create()`.
        assertThrows(IllegalStateException::class.java) {
            underTest.configure(
                Config(
                    Config.PlaneTrackingMode.DISABLED,
                    Config.HandTrackingMode.DISABLED,
                    Config.HeadTrackingMode.DISABLED,
                    Config.DepthEstimationMode.SMOOTH_AND_RAW,
                    Config.AnchorPersistenceMode.DISABLED,
                )
            )
        }
    }

    // TODO: b/344962771 - Add a more meaningful test once we can use the update() method.
    @Test
    fun resume_doesNotThrowIllegalStateException() = initOpenXrManagerAndRunTest {
        underTest.create()

        underTest.resume()
    }

    @Test
    fun resume_afterStopAndCreate_doesNotThrowIllegalStateException() =
        initOpenXrManagerAndRunTest {
            underTest.create()
            underTest.stop()
            check(underTest.nativePointer == 0L)
            underTest.create()

            underTest.resume()
        }

    @Test
    fun update_planeTrackingDisabled_doesNotUpdateTrackables() = initOpenXrManagerAndRunTest {
        runTest {
            underTest.create()
            underTest.resume()
            check(perceptionManager.trackables.isEmpty())
            check(underTest.config.planeTracking == Config.PlaneTrackingMode.DISABLED)

            underTest.update()

            assertThat(perceptionManager.trackables).isEmpty()
        }
    }

    @Test
    fun update_planeTrackingEnabled_addsPlaneToUpdatables() = initOpenXrManagerAndRunTest {
        runTest {
            underTest.create()
            underTest.resume()
            check(perceptionManager.xrResources.updatables.isEmpty())
            underTest.configure(
                Config(planeTracking = Config.PlaneTrackingMode.HORIZONTAL_AND_VERTICAL)
            )

            underTest.update()

            assertThat(perceptionManager.trackables).isNotEmpty()
        }
    }

    @Test
    // TODO - b/346615429: Control the values returned by the OpenXR stub instead of relying on the
    // stub's current implementation.
    fun update_returnsTimeMarkFromTimeSource() = initOpenXrManagerAndRunTest {
        runTest {
            underTest.create()
            underTest.resume()

            // The OpenXR stub returns a different value for each call to [OpenXrTimeSource::read]
            // in
            // increments of 1000ns when `xrConvertTimespecTimeToTimeKHR` is executed. The first
            // call
            // returns 1000ns and is the value associated with [timeMark]. The second call returns
            // 2000ns
            // and is the value associated with [AbstractLongTimeSource::zero], which is calculated
            // automatically with the first call to [OpenXrTimeSource::markNow].
            // Note that this is just an idiosyncrasy of the test stub and not how OpenXR works in
            // practice,
            // where the second call would return an almost identical value to the first call's
            // value.
            val timeMark = underTest.update()

            // The third call happens with the call to [elapsedNow] and returns 3000ns. Thus, the
            // elapsed
            // time is 3000ns (i.e. "now") -  1000ns (i.e. "the start time") = 2000ns.
            assertThat(timeMark.elapsedNow().inWholeNanoseconds).isEqualTo(2000L)
        }
    }

    // TODO: b/344962771 - Add a more meaningful test once we can use the update() method.
    @Test
    fun pause_doesNotThrowIllegalStateException() = initOpenXrManagerAndRunTest {
        underTest.create()
        underTest.resume()

        underTest.pause()
    }

    @Test
    fun pause_afterStop_throwsIllegalStateException() = initOpenXrManagerAndRunTest {
        underTest.create()
        underTest.stop()

        assertThrows(IllegalStateException::class.java) { underTest.pause() }
    }

    @Test
    fun stop_destroysNativeOpenXrManager() = initOpenXrManagerAndRunTest {
        underTest.create()
        check(underTest.nativePointer != 0L)

        underTest.stop()

        assertThat(underTest.nativePointer).isEqualTo(0L)
    }

    private fun initOpenXrManagerAndRunTest(testBody: () -> Unit) {
        activityRule.scenario.onActivity {
            underTest = OpenXrManager(it, perceptionManager, timeSource)

            testBody()

            // Stop the OpenXR manager here in lieu of an @After method to ensure that the
            // calls to the OpenXR manager are coming from the same thread.
            underTest.stop()
        }
    }
}
