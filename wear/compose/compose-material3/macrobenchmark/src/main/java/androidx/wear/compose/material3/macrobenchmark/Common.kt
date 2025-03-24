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

package androidx.wear.compose.material3.macrobenchmark

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice

internal fun pressHome() {
    val instrumentation = InstrumentationRegistry.getInstrumentation()
    val device = UiDevice.getInstance(instrumentation)
    device.pressHome()
}

internal fun disableChargingExperience() {
    val instrumentation = InstrumentationRegistry.getInstrumentation()
    val device = UiDevice.getInstance(instrumentation)
    device.executeShellCommand(
        "am broadcast -a " +
            "com.google.android.clockwork.sysui.charging.ENABLE_CHARGING_EXPERIENCE " +
            "--ez value \"false\" com.google.android.wearable.sysui"
    )
}

internal fun enableChargingExperience() {
    val instrumentation = InstrumentationRegistry.getInstrumentation()
    val device = UiDevice.getInstance(instrumentation)
    device.executeShellCommand(
        "am broadcast -a " +
            "com.google.android.clockwork.sysui.charging.ENABLE_CHARGING_EXPERIENCE " +
            "--ez value \"true\" com.google.android.wearable.sysui"
    )
}

internal const val PACKAGE_NAME = "androidx.wear.compose.material3.macrobenchmark.target"
