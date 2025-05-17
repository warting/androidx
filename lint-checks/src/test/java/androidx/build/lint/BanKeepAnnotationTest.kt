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

@file:Suppress("UnstableApiUsage")

package androidx.build.lint

import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class BanKeepAnnotationTest :
    AbstractLintDetectorTest(
        useDetector = BanKeepAnnotation(),
        useIssues = listOf(BanKeepAnnotation.ISSUE),
        stubs = arrayOf(Stubs.Keep),
    ) {

    @Test
    fun `Detection of Keep annotation in Java sources`() {
        val input = arrayOf(javaSample("androidx.KeepAnnotationUsageJava"))

        val expected =
            """
src/androidx/KeepAnnotationUsageJava.java:21: Error: Uses @Keep annotation [BanKeepAnnotation]
@Keep
~~~~~
1 errors, 0 warnings
        """
                .trimIndent()

        check(*input).expect(expected)
    }

    @Test
    fun `Detection of Keep annotation in Kotlin sources`() {
        val input = arrayOf(ktSample("androidx.KeepAnnotationUsageKotlin"))

        val expected =
            """
src/androidx/KeepAnnotationUsageKotlin.kt:21: Error: Uses @Keep annotation [BanKeepAnnotation]
@Keep class KeepAnnotationUsageKotlin
~~~~~
1 errors, 0 warnings
        """
                .trimIndent()

        check(*input).expect(expected)
    }
}
