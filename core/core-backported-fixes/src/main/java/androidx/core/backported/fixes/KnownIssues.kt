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

package androidx.core.backported.fixes

import android.os.Build

/**
 * List of all known issues reportable by [BackportedFixManager].
 *
 * These are critical issues with fixes that are backported to existing android releases and are
 * reasonable for app developers to guard a code block with []BackportedFixManager.isFixed].
 *
 * Each known issue includes sample usage.
 *
 * The `id` and `alias` of a known issue comes from the list of approved backported fixes in the
 * Android Compatibility Test source directory
 * [cts/backported_fixes/approved](https://cs.android.com/android/platform/superproject/+/android-latest-release:cts/backported_fixes/approved/).
 */
public sealed class KnownIssues {
    // TODO: b/381266031 - include samples for each
    public companion object {

        // sort the known issues by alias

        /**
         * **TEST ONLY** known issue that is always fixed on a device.
         *
         * @sample androidx.core.backported.fixes.samples.ki350037023
         */
        @JvmField public val KI_350037023: KnownIssue = KnownIssue(350037023L, 1)

        /**
         * **TEST ONLY** known issue that only applies to robolectric devices
         *
         * @sample androidx.core.backported.fixes.samples.ki372917199
         */
        @JvmField
        public val KI_372917199: KnownIssue =
            KnownIssue(372917199L, 2) { (Build.BRAND.equals("robolectric")) }

        /**
         * **TEST ONLY** known issue that is never fixed on a device.
         *
         * @sample androidx.core.backported.fixes.samples.ki350037348
         */
        @JvmField public val KI_350037348: KnownIssue = KnownIssue(350037348L, 3)

        /**
         * Abnormal color tone when capturing `JPEG-R` images on some Pixel devices.
         *
         * Fix by using `JPEG` outputs until this KI is resolved.
         *
         * @sample androidx.core.backported.fixes.samples.ki398591036
         *
         * Full details are at https://issuetracker.google.com/issues/398591036
         */
        @JvmField
        public val KI_398591036: KnownIssue =
            KnownIssue(398591036L, 5) {
                // This known issue only applies to Pixel devices.
                (Build.BRAND.equals("google"))
            }
    }
}
