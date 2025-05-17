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

package androidx.annotation.experimental.lint

import com.android.tools.lint.checks.infrastructure.TestFile
import com.android.tools.lint.checks.infrastructure.TestLintResult
import com.android.tools.lint.checks.infrastructure.TestLintTask
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class AnnotationRetentionDetectorTest {

    private fun check(vararg testFiles: TestFile): TestLintResult {
        return TestLintTask.lint()
            .files(
                RequiresOptInDetectorTest.ANDROIDX_REQUIRES_OPT_IN_KT,
                RequiresOptInDetectorTest.ANDROIDX_OPT_IN_KT,
                *testFiles,
            )
            .issues(*AnnotationRetentionDetector.ISSUES.toTypedArray())
            .run()
    }

    @Test
    fun experimentalAnnotationsJava() {
        val input =
            arrayOf(
                javaSample("sample.optin.ExperimentalJavaAnnotation"),
                javaSample("sample.optin.ExperimentalJavaAnnotation2"),
                javaSample("sample.optin.ExperimentalJavaAnnotationWrongRetention"),
            )

        val expected =
            """
src/sample/optin/ExperimentalJavaAnnotationWrongRetention.java:28: Error: Experimental annotation has RUNTIME retention, should use default (CLASS) [ExperimentalAnnotationRetention]
public @interface ExperimentalJavaAnnotationWrongRetention {}
                  ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
1 errors, 0 warnings
        """
                .trimIndent()

        check(*input).expect(expected)
    }

    @Test
    fun experimentalAnnotationsKotlin() {
        val input =
            arrayOf(
                ktSample("sample.optin.ExperimentalKotlinAnnotation"),
                ktSample("sample.optin.ExperimentalKotlinAnnotation2"),
                ktSample("sample.optin.ExperimentalKotlinAnnotationWrongRetention"),
            )

        val expected =
            """
src/sample/optin/ExperimentalKotlinAnnotationWrongRetention.kt:21: Error: Experimental annotation has default (RUNTIME) retention, should use BINARY [ExperimentalAnnotationRetention]
annotation class ExperimentalKotlinAnnotationWrongRetention
                 ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
1 errors, 0 warnings
        """
                .trimIndent()

        check(*input).expect(expected)
    }

    /**
     * Test for lint check that discourages the use of Java-style opt-in on Kotlin-sourced
     * annotations.
     */
    @Test
    fun wrongRequiresOptInAnnotation() {
        val input = arrayOf(ktSample("sample.kotlin.ExperimentalKotlinAnnotationWrongAnnotation"))

        val expected =
            """
src/sample/kotlin/ExperimentalKotlinAnnotationWrongAnnotation.kt:22: Error: Experimental annotation should use kotlin.RequiresOptIn [WrongRequiresOptIn]
annotation class ExperimentalKotlinAnnotationWrongAnnotation
                 ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
1 errors, 0 warnings
        """
                .trimIndent()

        check(*input).expect(expected)
    }
}
