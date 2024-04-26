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

package androidx.benchmark.macro

object Packages {
    /**
     * Separate target app.
     *
     * Use this app/package if it's necessary to kill/compile target process.
     */
    const val TARGET = "androidx.benchmark.integration.macrobenchmark.target"

    /**
     * This test app - this process.
     *
     * Preferably use this app/package if not killing/compiling target.
     */
    const val TEST = "androidx.benchmark.macro.test"

    /**
     * Package not present on device.
     *
     * Used to validate behavior when package can't be found.
     */
    const val MISSING = "not.real.fake.package"
}
