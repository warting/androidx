/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.build.lint.replacewith

import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class ReplaceWithDetectorImportsTest {

    @Test
    fun methodWithImportsJava() {
        val input = arrayOf(javaSample("replacewith.MethodWithImportsJava"))

        val expected =
            """
src/replacewith/MethodWithImportsJava.java:38: Hint: Replacement available [ReplaceWith]
        oldMethodSingleImport(null);
        ~~~~~~~~~~~~~~~~~~~~~~~~~~~
src/replacewith/MethodWithImportsJava.java:42: Hint: Replacement available [ReplaceWith]
        oldMethodMultiImport(null);
        ~~~~~~~~~~~~~~~~~~~~~~~~~~
0 errors, 0 warnings, 2 hints
        """
                .trimIndent()

        val expectedFixDiffs =
            """
Fix for src/replacewith/MethodWithImportsJava.java line 38: Replace with `newMethod(null)`:
@@ -20 +20
+ import androidx.annotation.Deprecated;
@@ -38 +39
-         oldMethodSingleImport(null);
+         newMethod(null);
Fix for src/replacewith/MethodWithImportsJava.java line 42: Replace with `newMethod(null)`:
@@ -20 +20
+ import androidx.annotation.Deprecated;
+ import androidx.annotation.NonNull;
@@ -42 +44
-         oldMethodMultiImport(null);
+         newMethod(null);
        """
                .trimIndent()

        check(*input).expect(expected).expectFixDiffs(expectedFixDiffs)
    }

    @Test
    fun methodWithImportsKotlin() {
        val input =
            arrayOf(
                ktSample("replacewith.MethodWithImportsKotlin"),
                javaSample("replacewith.ReplaceWithUsageJava"),
            )

        val expected =
            """
src/replacewith/MethodWithImportsKotlin.kt:25: Hint: Replacement available [ReplaceWith]
        ReplaceWithUsageJava.toStringWithImport("hello")
        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
src/replacewith/MethodWithImportsKotlin.kt:30: Hint: Replacement available [ReplaceWith]
        ReplaceWithUsageJava.toStringWithImports("world")
        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
0 errors, 0 warnings, 2 hints
        """
                .trimIndent()

        val expectedFixDiffs =
            """
Fix for src/replacewith/MethodWithImportsKotlin.kt line 25: Replace with `"hello".toString()`:
@@ -19 +19
+ import androidx.annotation.Deprecated
@@ -25 +26
-         ReplaceWithUsageJava.toStringWithImport("hello")
+         "hello".toString()
Fix for src/replacewith/MethodWithImportsKotlin.kt line 30: Replace with `"world".toString()`:
@@ -19 +19
+ import androidx.annotation.Deprecated
+ import androidx.annotation.NonNull
@@ -30 +32
-         ReplaceWithUsageJava.toStringWithImports("world")
+         "world".toString()
        """
                .trimIndent()

        check(*input).expect(expected).expectFixDiffs(expectedFixDiffs)
    }

    @Test
    fun methodWithNoImportsJava() {
        val input = arrayOf(javaSample("replacewith.MethodWithNoImportsJava"))

        val expected =
            """
src/replacewith/MethodWithNoImportsJava.java:37: Hint: Replacement available [ReplaceWith]
        oldMethodSingleImport(null);
        ~~~~~~~~~~~~~~~~~~~~~~~~~~~
src/replacewith/MethodWithNoImportsJava.java:41: Hint: Replacement available [ReplaceWith]
        oldMethodMultiImport(null);
        ~~~~~~~~~~~~~~~~~~~~~~~~~~
0 errors, 0 warnings, 2 hints
        """
                .trimIndent()

        val expectedFixDiffs =
            """
Fix for src/replacewith/MethodWithNoImportsJava.java line 37: Replace with `newMethod(null)`:
@@ -19 +19
+ import androidx.annotation.Deprecated;
+
@@ -37 +39
-         oldMethodSingleImport(null);
+         newMethod(null);
Fix for src/replacewith/MethodWithNoImportsJava.java line 41: Replace with `newMethod(null)`:
@@ -19 +19
+ import androidx.annotation.Deprecated;
+ import androidx.annotation.NonNull;
+
@@ -41 +44
-         oldMethodMultiImport(null);
+         newMethod(null);
        """
                .trimIndent()

        check(*input).expect(expected).expectFixDiffs(expectedFixDiffs)
    }

    @Test
    fun methodWithNoImportsKotlin() {
        val input =
            arrayOf(
                ktSample("replacewith.MethodWithNoImportsKotlin"),
                javaSample("replacewith.ReplaceWithUsageJava"),
            )

        val expected =
            """
src/replacewith/MethodWithNoImportsKotlin.kt:22: Hint: Replacement available [ReplaceWith]
        ReplaceWithUsageJava.toStringWithImport("hello")
        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
src/replacewith/MethodWithNoImportsKotlin.kt:26: Hint: Replacement available [ReplaceWith]
        ReplaceWithUsageJava.toStringWithImports("world")
        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
0 errors, 0 warnings, 2 hints
        """
                .trimIndent()

        val expectedFixDiffs =
            """
Fix for src/replacewith/MethodWithNoImportsKotlin.kt line 22: Replace with `"hello".toString()`:
@@ -18 +18
+ import androidx.annotation.Deprecated
+
@@ -22 +24
-         ReplaceWithUsageJava.toStringWithImport("hello")
+         "hello".toString()
Fix for src/replacewith/MethodWithNoImportsKotlin.kt line 26: Replace with `"world".toString()`:
@@ -18 +18
+ import androidx.annotation.Deprecated
+ import androidx.annotation.NonNull
+
@@ -26 +29
-         ReplaceWithUsageJava.toStringWithImports("world")
+         "world".toString()
        """
                .trimIndent()

        check(*input).expect(expected).expectFixDiffs(expectedFixDiffs)
    }

    @Test
    fun methodWithNoImportsOrPackage() {
        val input = arrayOf(javaSample("replacewith.MethodWithNoImportsOrPackage"))

        val expected =
            """
src/MethodWithNoImportsOrPackage.java:35: Hint: Replacement available [ReplaceWith]
        oldMethodSingleImport(null);
        ~~~~~~~~~~~~~~~~~~~~~~~~~~~
src/MethodWithNoImportsOrPackage.java:39: Hint: Replacement available [ReplaceWith]
        oldMethodMultiImport(null);
        ~~~~~~~~~~~~~~~~~~~~~~~~~~
0 errors, 0 warnings, 2 hints
        """
                .trimIndent()

        val expectedFixDiffs =
            """
Fix for src/MethodWithNoImportsOrPackage.java line 35: Replace with `newMethod(null)`:
@@ -1 +1
+ import androidx.annotation.Deprecated;
@@ -35 +36
-         oldMethodSingleImport(null);
+         newMethod(null);
Fix for src/MethodWithNoImportsOrPackage.java line 39: Replace with `newMethod(null)`:
@@ -1 +1
+ import androidx.annotation.Deprecated;
+ import androidx.annotation.NonNull;
@@ -39 +41
-         oldMethodMultiImport(null);
+         newMethod(null);
        """
                .trimIndent()

        check(*input).expect(expected).expectFixDiffs(expectedFixDiffs)
    }
}
