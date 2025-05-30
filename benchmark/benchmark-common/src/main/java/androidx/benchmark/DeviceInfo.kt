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

package androidx.benchmark

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.BatteryManager
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
object DeviceInfo {
    val isEmulator =
        Build.FINGERPRINT.startsWith("generic") ||
            Build.FINGERPRINT.startsWith("unknown") ||
            Build.FINGERPRINT.contains("emulator") ||
            Build.MODEL.contains("google_sdk") ||
            Build.MODEL.startsWith("sdk_") ||
            Build.MODEL.contains("sdk_gphone64") ||
            Build.MODEL.contains("Emulator") ||
            Build.MODEL.contains("Android SDK built for") ||
            Build.MANUFACTURER.contains("Genymotion") ||
            Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic") ||
            "google_sdk" == Build.PRODUCT

    val typeLabel = if (isEmulator) "emulator" else "device"

    val isEngBuild = Build.FINGERPRINT.contains(":eng/")
    private val isUserdebugBuild = Build.FINGERPRINT.contains(":userdebug/")

    val profileableEnforced = !isEngBuild && !isUserdebugBuild

    val isRooted =
        Build.FINGERPRINT.contains(":userdebug/") ||
            arrayOf(
                    "/system/app/Superuser.apk",
                    "/sbin/su",
                    "/system/bin/su",
                    "/system/xbin/su",
                    "/data/local/xbin/su",
                    "/data/local/bin/su",
                    "/system/sd/xbin/su",
                    "/system/bin/failsafe/su",
                    "/data/local/su",
                    "/su/bin/su",
                )
                .any { File(it).exists() }

    /**
     * Null if BP capture is supported on this device, error string if it's not.
     *
     * Can be passed to assumeTrue()/require()
     *
     * Lazy to allow late init, after shell connection is set up
     */
    val supportsBaselineProfileCaptureError: String? by lazy {
        if (
            Build.VERSION.SDK_INT >= 33 || (Build.VERSION.SDK_INT >= 28 && Shell.isSessionRooted())
        ) {
            null // profile capture works, no error
        } else {
            "Baseline Profile collection requires API 33+, or a rooted" +
                " device running API 28 or higher and rooted adb session (via `adb root`)."
        }
    }

    /**
     * Battery percentage required to avoid low battery warning.
     *
     * This number is supposed to be a conservative cutoff for when low-battery-triggered power
     * savings modes (such as disabling cores) may be enabled. It's possible that
     * [BatteryManager.EXTRA_BATTERY_LOW] is a better source of truth for this, but we want to be
     * conservative in case the device loses power slowly while benchmarks run.
     */
    const val MINIMUM_BATTERY_PERCENT = 25

    val initialBatteryPercent: Int

    /** String summarizing device hardware and software, for bug reporting purposes. */
    val deviceSummaryString: String

    /**
     * General errors about device configuration, applicable to all types of benchmark.
     *
     * These errors indicate no performance tests should be performed on this device, in it's
     * current conditions.
     */
    val errors: List<ConfigurationError>

    /**
     * Tracks whether the virtual kernel files have been properly configured on this OS build.
     *
     * If not, only recourse is to try a different device.
     */
    val misconfiguredForTracing =
        !File("/sys/kernel/tracing/trace_marker").exists() &&
            !File("/sys/kernel/debug/tracing/trace_marker").exists()

    private fun getMainlinePackageInfo(packageName: String): PackageInfo? {
        return try {
            InstrumentationRegistry.getInstrumentation()
                .context
                .packageManager
                .getPackageInfo(packageName, PackageManager.MATCH_APEX)
        } catch (_: PackageManager.NameNotFoundException) {
            null
        }
    }

    @RequiresApi(31)
    private fun queryArtMainlineVersion(): Long {
        val artMainlinePackage =
            getMainlinePackageInfo("com.google.android.art")
                ?: getMainlinePackageInfo("com.android.art")
                ?: getMainlinePackageInfo("com.google.android.go.art")
                ?: getMainlinePackageInfo("com.android.go.art")
        if (artMainlinePackage == null) {
            Log.d(
                BenchmarkState.TAG,
                "No ART mainline module found on API ${Build.VERSION.SDK_INT}",
            )
            return if (Build.VERSION.SDK_INT >= 34) {
                // defer error to avoid crashing during init
                ART_MAINLINE_VERSION_UNDETECTED_ERROR
            } else {
                // accept missing module if we can't be sure it would have one installed (e.g. go)
                ART_MAINLINE_VERSION_UNDETECTED
            }
        }
        return artMainlinePackage.longVersionCode
    }

    val isLowRamDevice: Boolean

    init {
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        initialBatteryPercent =
            context.registerReceiver(null, filter)?.run {
                val level =
                    if (getBooleanExtra(BatteryManager.EXTRA_PRESENT, true)) {
                        getIntExtra(BatteryManager.EXTRA_LEVEL, 100)
                    } else {
                        // If the device has no battery consider it full for this check.
                        100
                    }
                val scale = getIntExtra(BatteryManager.EXTRA_SCALE, 100)
                level * 100 / scale
            } ?: 100

        isLowRamDevice =
            (context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager).isLowRamDevice

        deviceSummaryString =
            "DeviceInfo(Brand=${Build.BRAND}" +
                ", Model=${Build.MODEL}" +
                ", SDK=${Build.VERSION.SDK_INT}" +
                ", BuildFp=${Build.FINGERPRINT})"

        errors =
            listOfNotNull(
                conditionalError(
                    hasError = isEngBuild,
                    id = "ENG-BUILD",
                    summary = "Running on Eng Build",
                    message =
                        """
                    Benchmark is running on device flashed with a '-eng' build. Eng builds
                    of the platform drastically reduce performance to enable testing
                    changes quickly. For this reason they should not be used for
                    benchmarking. Use a '-user' or '-userdebug' system image.
                """
                            .trimIndent(),
                ),
                conditionalError(
                    hasError = isEmulator,
                    id = "EMULATOR",
                    summary = "Running on Emulator",
                    message =
                        """
                    Benchmark is running on an emulator, which is not representative of
                    real user devices. Use a physical device to benchmark. Emulator
                    benchmark improvements might not carry over to a real user's
                    experience (or even regress real device performance).
                """
                            .trimIndent(),
                ),
                conditionalError(
                    hasError = initialBatteryPercent < MINIMUM_BATTERY_PERCENT,
                    id = "LOW-BATTERY",
                    summary = "Device has low battery ($initialBatteryPercent)",
                    message =
                        """
                    When battery is low, devices will often reduce performance (e.g. disabling big
                    cores) to save remaining battery. This occurs even when they are plugged in.
                    Wait for your battery to charge to at least $MINIMUM_BATTERY_PERCENT%.
                    Currently at $initialBatteryPercent%.
                """
                            .trimIndent(),
                ),
            )
    }

    /**
     * Starting with the first Android U release, ART mainline drops optimizations after method
     * tracing occurs, so we disable tracing on those mainline versions.
     *
     * Fix cherry picked into 341513000, so we exclude that value
     *
     * See b/303660864
     */
    private val ART_MAINLINE_VERSIONS_AFFECTING_METHOD_TRACING = 340000000L.until(341513000)

    /**
     * Starting with an API 35 change cherry-picked to mainline, ART traces class init.
     *
     * Fix cherry picked into 341511000
     *
     * See b/292294133
     */
    const val ART_MAINLINE_MIN_VERSION_CLASS_LOAD_TRACING = 341511000L

    /**
     * Starting with an API 34 change cherry-picked to mainline, when `verify`-compiled, ART will
     * save loaded classes to disk to prevent subsequent cold starts from reinitializing after the
     * first startup.
     *
     * This can only happen once, and may not occur if the app doesn't have enough time to save the
     * classes. Additionally, the list of classes is not updated in subsequent starts - it is
     * possible for an ineffective runtime image to be generated, e.g. from a trivial broadcast
     * receiver wakeup (again, only if the app has enough time to save the image). Experiments on an
     * API 35 emulator show that runtime images are generally saved roughly 4 seconds after an app
     * starts up.
     *
     * To disable this behavior, we re-compile with verify after each `kill` to clear profiles when
     * desired.
     *
     * See b/368404173
     *
     * @see androidx.benchmark.macro.MacrobenchmarkScope.KillFlushMode.ClearArtRuntimeImage
     * @see ART_MAINLINE_MIN_VERSION_VERIFY_CLEARS_RUNTIME_IMAGE
     */
    private const val ART_MAINLINE_MIN_VERSION_RUNTIME_IMAGE = 340800000L

    /**
     * Starting with an API 35 backported with mainline, an additional `verify` will clear runtime
     * images.
     *
     * Without this functionality, --reset (root & pre API 34) or reinstall is needed to reset.
     */
    private const val ART_MAINLINE_MIN_VERSION_VERIFY_CLEARS_RUNTIME_IMAGE = 350800000L

    /**
     * ART mainline 990090000 means the module is built from source in the system image and isn't
     * updatable, and thus expectations should be conservative - assume that any potential bug on
     * the current SDK version may be present on this device.
     *
     * Ideally, we'd have a minimum release build ID, but these may not be consistently and easily
     * sortable.
     */
    private const val ART_MAINLINE_INTERNAL_BUILD_MIN = 990000000

    /**
     * Used when mainline version failed to detect, but this is accepted due to low API level (<34)
     * where presence isn't guaranteed (e.g. go devices)
     */
    const val ART_MAINLINE_VERSION_UNDETECTED = -1L

    /**
     * Used when mainline version failed to detect, and should throw an error when running a
     * microbenchmark
     */
    const val ART_MAINLINE_VERSION_UNDETECTED_ERROR = -100L

    val artMainlineVersion =
        when {
            Build.VERSION.SDK_INT >= 31 -> queryArtMainlineVersion()
            Build.VERSION.SDK_INT == 30 -> 1
            else -> ART_MAINLINE_VERSION_UNDETECTED
        }

    fun willMethodTracingAffectMeasurements(sdkInt: Int, artVersion: Long): Boolean =
        sdkInt in 26..30 || // b/313868903
            artVersion in ART_MAINLINE_VERSIONS_AFFECTING_METHOD_TRACING || // b/303660864
            (sdkInt == 34 && artVersion >= ART_MAINLINE_INTERNAL_BUILD_MIN) // b/303686344#comment31

    val methodTracingAffectsMeasurements =
        willMethodTracingAffectMeasurements(Build.VERSION.SDK_INT, artMainlineVersion)

    fun isClassLoadTracingAvailable(sdkInt: Int, artVersion: Long?): Boolean =
        sdkInt >= 35 ||
            (sdkInt >= 31 &&
                (artVersion == null || artVersion >= ART_MAINLINE_MIN_VERSION_CLASS_LOAD_TRACING))

    val supportsClassLoadTracing =
        isClassLoadTracingAvailable(Build.VERSION.SDK_INT, artMainlineVersion)

    val supportsRuntimeImages =
        Build.VERSION.SDK_INT >= 34 || artMainlineVersion >= ART_MAINLINE_MIN_VERSION_RUNTIME_IMAGE

    val verifyClearsRuntimeImage =
        Build.VERSION.SDK_INT >= 35 ||
            (Build.VERSION.SDK_INT == 34 &&
                artMainlineVersion >= ART_MAINLINE_MIN_VERSION_VERIFY_CLEARS_RUNTIME_IMAGE)

    val supportsCpuEventCounters =
        Build.VERSION.SDK_INT < CpuEventCounter.MIN_API_ROOT_REQUIRED || isRooted
}
