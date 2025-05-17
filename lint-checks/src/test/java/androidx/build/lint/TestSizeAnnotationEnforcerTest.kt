/*
 * Copyright 2020 The Android Open Source Project
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

import com.android.tools.lint.checks.infrastructure.LintDetectorTest
import com.android.tools.lint.checks.infrastructure.TestMode
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class TestSizeAnnotationEnforcerTest : LintDetectorTest() {
    override fun getDetector(): Detector = TestSizeAnnotationEnforcer()

    override fun getIssues(): List<Issue> =
        listOf(TestSizeAnnotationEnforcer.UNEXPECTED_TEST_SIZE_ANNOTATION)

    @Test
    fun allowJUnit4ForHostSideTests() {
        lint()
            .files(
                kotlin(
                        """
                package androidx.foo

                import org.junit.runner.RunWith
                import org.junit.runners.JUnit4

                @RunWith(JUnit4::class)
                class Test {
                    @Test
                    fun aTest() {}
                }
            """
                    )
                    .within("src/test"),
                *StubClasses,
            )
            .skipTestModes(TestMode.JVM_OVERLOADS)
            .run()
            .expectClean()
    }

    @Test
    fun noTestSizeAnnotationsForHostSideTests() {
        lint()
            .files(
                kotlin(
                        """
                package androidx.foo

                import androidx.test.filters.MediumTest
                import org.junit.runner.RunWith
                import org.junit.runners.JUnit4

                @MediumTest
                @RunWith(JUnit4::class)
                class Test {
                    @MediumTest
                    fun notATest() {}

                    @MediumTest
                    @Test
                    fun aTest() {}

                    @Test
                    fun anotherTest() {}
                }
            """
                    )
                    .within("src/test"),
                *StubClasses,
            )
            .run()
            .expect(
                """
src/test/androidx/foo/Test.kt:8: Error: Unexpected test size annotation [UnexpectedTestSizeAnnotation]
                @MediumTest
                ~~~~~~~~~~~
1 errors, 0 warnings
"""
            )
    }

    private val StubClasses =
        arrayOf(
            Stubs.RunWith,
            Stubs.JUnit4Runner,
            Stubs.ParameterizedRunner,
            Stubs.AndroidJUnit4Runner,
            Stubs.TestSizeAnnotations,
            Stubs.TestAnnotation,
            Stubs.TestParameterInjector,
        )
}
