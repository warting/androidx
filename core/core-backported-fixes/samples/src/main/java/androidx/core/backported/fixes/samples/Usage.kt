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

package androidx.core.backported.fixes.samples

import androidx.annotation.Sampled
import androidx.core.backported.fixes.BackportedFixManager
import androidx.core.backported.fixes.KnownIssues

@Sampled
fun ki350037023() {
    val bf = BackportedFixManager()
    if (bf.isFixed(KnownIssues.KI_350037023)) {
        println("Hello world")
    } else {
        // Since the known issue is not fixed
        println("Goodbye")
    }
}

@Sampled
fun ki350037348() {
    val bf = BackportedFixManager()
    if (bf.isFixed(KnownIssues.KI_350037348)) {
        println("Hello world")
    } else {
        // Since the known issue is not fixed
        println("Goodbye")
    }
}

@Sampled
fun ki372917199() {
    val bf = BackportedFixManager()
    if (bf.isFixed(KnownIssues.KI_372917199)) {
        println("Hello world")
    } else {
        // Since the known issue is not fixed
        println("Goodbye")
    }
}

@Sampled
fun ki398591036() {
    val bf = BackportedFixManager()
    val format =
        if (bf.isFixed(KnownIssues.KI_398591036)) {
            "JPEG-R"
        } else {
            "JPEG"
        }
}
