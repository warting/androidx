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

package androidx.compose.runtime.lint

import androidx.compose.lint.test.Stubs
import com.android.tools.lint.checks.infrastructure.LintDetectorTest
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)

/** Test for [CompositionLocalNamingDetector]. */
class CompositionLocalNamingDetectorTest : LintDetectorTest() {
    override fun getDetector(): Detector = CompositionLocalNamingDetector()

    override fun getIssues(): MutableList<Issue> =
        mutableListOf(CompositionLocalNamingDetector.CompositionLocalNaming)

    @Test
    fun noLocalPrefix() {
        lint()
            .files(
                kotlin(
                    """
                package androidx.compose.runtime.foo

                import androidx.compose.runtime.*

                val FooCompositionLocal = compositionLocalOf { 5 }

                object Test {
                    val BarCompositionLocal: CompositionLocal<String?> = staticCompositionLocalOf {
                        null
                    }
                }

                class Test2 {
                    companion object {
                        val BazCompositionLocal: ProvidableCompositionLocal<Int> =
                            compositionLocalOf()
                    }
                }
            """
                ),
                Stubs.CompositionLocal,
            )
            .run()
            .expect(
                """
src/androidx/compose/runtime/foo/Test.kt:6: Warning: CompositionLocal properties should be prefixed with Local [CompositionLocalNaming]
                val FooCompositionLocal = compositionLocalOf { 5 }
                    ~~~~~~~~~~~~~~~~~~~
src/androidx/compose/runtime/foo/Test.kt:9: Warning: CompositionLocal properties should be prefixed with Local [CompositionLocalNaming]
                    val BarCompositionLocal: CompositionLocal<String?> = staticCompositionLocalOf {
                        ~~~~~~~~~~~~~~~~~~~
src/androidx/compose/runtime/foo/Test.kt:16: Warning: CompositionLocal properties should be prefixed with Local [CompositionLocalNaming]
                        val BazCompositionLocal: ProvidableCompositionLocal<Int> =
                            ~~~~~~~~~~~~~~~~~~~
0 errors, 3 warnings
            """
            )
    }

    @Test
    fun prefixedWithLocal() {
        lint()
            .files(
                kotlin(
                    """
                package androidx.compose.runtime.foo

                import androidx.compose.runtime.*

                val LocalFoo = compositionLocalOf { 5 }

                object Test {
                    val LocalBar: CompositionLocal<String?> = staticCompositionLocalOf { null }
                }

                class Test2 {
                    companion object {
                        val LocalBaz: ProvidableCompositionLocal<Int> = compositionLocalOf()
                    }
                }
            """
                ),
                Stubs.CompositionLocal,
            )
            .run()
            .expectClean()
    }
}
