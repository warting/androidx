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
class BanRestrictToTestsScopeTest :
    AbstractLintDetectorTest(
        useDetector = BanRestrictToTestsScope(),
        useIssues = listOf(BanRestrictToTestsScope.ISSUE),
        stubs = arrayOf(Stubs.RestrictTo),
    ) {

    @Test
    fun `Detection of @RestrictTo(TESTS) usage in Java sources`() {
        val input = arrayOf(javaSample("androidx.RestrictToTestsAnnotationUsageJava"))

        val expected =
            """
src/androidx/RestrictToTestsAnnotationUsageJava.java:26: Error: Replace @RestrictTo(TESTS) with @VisibleForTesting [UsesRestrictToTestsScope]
    @RestrictTo(androidx.annotation.RestrictTo.Scope.TESTS)
    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
src/androidx/RestrictToTestsAnnotationUsageJava.java:29: Error: Replace @RestrictTo(TESTS) with @VisibleForTesting [UsesRestrictToTestsScope]
    @RestrictTo(RestrictTo.Scope.TESTS)
    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
src/androidx/RestrictToTestsAnnotationUsageJava.java:32: Error: Replace @RestrictTo(TESTS) with @VisibleForTesting [UsesRestrictToTestsScope]
    @RestrictTo(Scope.TESTS)
    ~~~~~~~~~~~~~~~~~~~~~~~~
src/androidx/RestrictToTestsAnnotationUsageJava.java:35: Error: Replace @RestrictTo(TESTS) with @VisibleForTesting [UsesRestrictToTestsScope]
    @RestrictTo(TESTS)
    ~~~~~~~~~~~~~~~~~~
src/androidx/RestrictToTestsAnnotationUsageJava.java:38: Error: Replace @RestrictTo(TESTS) with @VisibleForTesting [UsesRestrictToTestsScope]
    @RestrictTo({Scope.TESTS, Scope.LIBRARY})
    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
5 errors, 0 warnings
        """
                .trimIndent()

        val fixDiffs =
            """
Fix for src/androidx/RestrictToTestsAnnotationUsageJava.java line 26: Replace with `@VisibleForTesting`:
@@ -26 +26
-     @RestrictTo(androidx.annotation.RestrictTo.Scope.TESTS)
+     @androidx.annotation.VisibleForTesting
Fix for src/androidx/RestrictToTestsAnnotationUsageJava.java line 29: Replace with `@VisibleForTesting`:
@@ -29 +29
-     @RestrictTo(RestrictTo.Scope.TESTS)
+     @androidx.annotation.VisibleForTesting
Fix for src/androidx/RestrictToTestsAnnotationUsageJava.java line 32: Replace with `@VisibleForTesting`:
@@ -32 +32
-     @RestrictTo(Scope.TESTS)
+     @androidx.annotation.VisibleForTesting
Fix for src/androidx/RestrictToTestsAnnotationUsageJava.java line 35: Replace with `@VisibleForTesting`:
@@ -35 +35
-     @RestrictTo(TESTS)
+     @androidx.annotation.VisibleForTesting
        """
                .trimIndent()

        check(*input).expect(expected).expectFixDiffs(fixDiffs)
    }

    @Test
    fun `Detection of @RestrictTo(TESTS) usage in Kotlin sources`() {
        val input = arrayOf(ktSample("androidx.RestrictToTestsAnnotationUsageKotlin"))

        val expected =
            """
src/androidx/RestrictToTestsAnnotationUsageKotlin.kt:24: Error: Replace @RestrictTo(TESTS) with @VisibleForTesting [UsesRestrictToTestsScope]
    @RestrictTo(RestrictTo.Scope.TESTS) fun testMethod() {}
    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
src/androidx/RestrictToTestsAnnotationUsageKotlin.kt:26: Error: Replace @RestrictTo(TESTS) with @VisibleForTesting [UsesRestrictToTestsScope]
    @RestrictTo(RestrictTo.Scope.TESTS, RestrictTo.Scope.LIBRARY) fun testMethodVarArg() {}
    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
src/androidx/RestrictToTestsAnnotationUsageKotlin.kt:28: Error: Replace @RestrictTo(TESTS) with @VisibleForTesting [UsesRestrictToTestsScope]
    @get:RestrictTo(RestrictTo.Scope.TESTS) val testPropertyGet = "test"
    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
3 errors, 0 warnings
        """
                .trimIndent()

        val fixDiffs =
            """
Fix for src/androidx/RestrictToTestsAnnotationUsageKotlin.kt line 24: Replace with `@VisibleForTesting`:
@@ -22 +22
+ import androidx.annotation.VisibleForTesting
@@ -24 +25
-     @RestrictTo(RestrictTo.Scope.TESTS) fun testMethod() {}
+     @VisibleForTesting fun testMethod() {}
Fix for src/androidx/RestrictToTestsAnnotationUsageKotlin.kt line 28: Replace with `@get:VisibleForTesting`:
@@ -22 +22
+ import androidx.annotation.VisibleForTesting
@@ -28 +29
-     @get:RestrictTo(RestrictTo.Scope.TESTS) val testPropertyGet = "test"
+     @get:VisibleForTesting val testPropertyGet = "test"
        """
                .trimIndent()

        check(*input).expect(expected).expectFixDiffs(fixDiffs)
    }
}
