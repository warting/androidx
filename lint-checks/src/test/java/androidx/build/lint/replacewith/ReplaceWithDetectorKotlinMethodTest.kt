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
class ReplaceWithDetectorKotlinMethodTest {

    @Test
    fun staticMethodExplicitClass() {
        val input =
            arrayOf(
                ktSample("replacewith.ReplaceWithUsageKotlin"),
                javaSample("replacewith.StaticKotlinMethodExplicitClassJava"),
            )

        val expected =
            """
src/replacewith/StaticKotlinMethodExplicitClassJava.java:25: Hint: Replacement available [ReplaceWith]
        ReplaceWithUsageKotlin.toString(this);
        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
0 errors, 0 warnings, 1 hint
        """
                .trimIndent()

        val expectedFixDiffs =
            """
Fix for src/replacewith/StaticKotlinMethodExplicitClassJava.java line 25: Replace with `this.toString()`:
@@ -25 +25
-         ReplaceWithUsageKotlin.toString(this);
+         this.toString();
        """
                .trimIndent()

        check(*input).expect(expected).expectFixDiffs(expectedFixDiffs)
    }

    @Test
    fun staticMethodExplicitClass_withKotlinSource_hasNoWarnings() {
        val input =
            arrayOf(
                ktSample("replacewith.ReplaceWithUsageKotlin"),
                ktSample("replacewith.StaticKotlinMethodExplicitClassKotlin"),
            )

        val expected =
            """
No warnings.
        """
                .trimIndent()

        check(*input).expect(expected)
    }
}
