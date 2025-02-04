/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.compose.runtime.tooling

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Composer
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.ReusableContent
import androidx.compose.runtime.ReusableContentHost
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mock.CompositionTestScope
import androidx.compose.runtime.mock.compositionTest
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.fastForEach
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ErrorTraceTests {
    @BeforeTest
    fun setUp() {
        Composer.setDiagnosticStackTraceEnabled(true)
    }

    @AfterTest
    fun tearDown() {
        Composer.setDiagnosticStackTraceEnabled(false)
    }

    @Test
    fun setContent() = exceptionTest {
        assertTrace(listOf("<lambda>(ErrorTraceTests.kt:<unknown line>)")) {
            compose { throwTestException() }
        }
    }

    @Test
    fun recompose() = exceptionTest {
        var state by mutableStateOf(false)
        compose {
            if (state) {
                throwTestException()
            }
        }

        assertTrace(
            listOf(
                "<lambda>(ErrorTraceTests.kt:<unknown line>)",
            )
        ) {
            state = true
            advance()
        }
    }

    @Test
    fun setContentLinear() = exceptionTest {
        assertTrace(
            listOf(
                "<lambda>(ErrorTraceTests.kt:<unknown line>)",
                "<lambda>(ErrorTraceComposables.kt:77)",
                "ReusableComposeNode(Composables.kt:<line number>)",
                "Linear(ErrorTraceComposables.kt:73)",
                "<lambda>(ErrorTraceTests.kt:<line number>)"
            ),
        ) {
            compose { Linear { throwTestException() } }
        }
    }

    @Test
    fun recomposeLinear() = exceptionTest {
        var state by mutableStateOf(false)
        compose {
            Linear {
                if (state) {
                    throwTestException()
                }
            }
        }

        assertTrace(
            listOf(
                "<lambda>(ErrorTraceTests.kt:<unknown line>)",
                "<lambda>(ErrorTraceComposables.kt:77)",
                "ReusableComposeNode(Composables.kt:<line number>)",
                "Linear(ErrorTraceComposables.kt:73)",
                "<lambda>(ErrorTraceTests.kt:<line number>)"
            ),
        ) {
            state = true
            advance()
        }
    }

    @Test
    fun setContentInlineLinear() = exceptionTest {
        assertTrace(
            listOf(
                "<lambda>(ErrorTraceTests.kt:<unknown line>)",
                "<lambda>(ErrorTraceComposables.kt:87)",
                "ReusableComposeNode(Composables.kt:<line number>)",
                "InlineLinear(ErrorTraceComposables.kt:83)",
                "<lambda>(ErrorTraceTests.kt:<line number>)"
            ),
        ) {
            compose { InlineLinear { throwTestException() } }
        }
    }

    @Test
    fun recomposeInlineLinear() = exceptionTest {
        var state by mutableStateOf(false)

        compose {
            InlineLinear {
                if (state) {
                    throwTestException()
                }
            }
        }

        assertTrace(
            listOf(
                "<lambda>(ErrorTraceTests.kt:<unknown line>)",
                "<lambda>(ErrorTraceComposables.kt:87)",
                "ReusableComposeNode(Composables.kt:<line number>)",
                "InlineLinear(ErrorTraceComposables.kt:83)",
                "<lambda>(ErrorTraceTests.kt:<line number>)"
            )
        ) {
            state = true
            advance()
        }
    }

    @Test
    fun setContentAfterTextInLoopInlineWrapper() = exceptionTest {
        assertTrace(
            listOf(
                "<lambda>(ErrorTraceTests.kt:<unknown line>)",
                "InlineWrapper(ErrorTraceComposables.kt:57)",
                "<lambda>(ErrorTraceTests.kt:<line number>)"
            ),
        ) {
            compose {
                InlineWrapper {
                    repeat(5) { it ->
                        Text("test")
                        if (it > 3) {
                            throwTestException()
                        }
                    }
                }
            }
        }
    }

    @Test
    fun recomposeAfterTextInLoopInlineWrapper() = exceptionTest {
        var state by mutableStateOf(false)

        compose {
            InlineWrapper {
                repeat(5) { it ->
                    Text("test")
                    if (it > 3 && state) {
                        throwTestException()
                    }
                }
            }
        }

        assertTrace(
            listOf(
                "<lambda>(ErrorTraceTests.kt:<unknown line>)",
                "InlineWrapper(ErrorTraceComposables.kt:57)",
                "<lambda>(ErrorTraceTests.kt:<line number>)"
            )
        ) {
            state = true
            advance()
        }
    }

    @Test
    fun setContentAfterTextInLoop() = exceptionTest {
        assertTrace(
            listOf(
                "<lambda>(ErrorTraceTests.kt:<unknown line>)",
                "Repeated(ErrorTraceComposables.kt:94)",
                "<lambda>(ErrorTraceTests.kt:<line number>)"
            ),
        ) {
            compose {
                Repeated(List(10) { it }) {
                    Text("test")
                    throwTestException()
                }
            }
        }
    }

    @Test
    fun recomposeAfterTextInLoop() = exceptionTest {
        var state by mutableStateOf(false)

        compose {
            Repeated(List(10) { it }) {
                Text("test")
                if (state) {
                    throwTestException()
                }
            }
        }

        assertTrace(
            listOf(
                "<lambda>(ErrorTraceTests.kt:<unknown line>)",
                "Repeated(ErrorTraceComposables.kt:94)",
                "<lambda>(ErrorTraceTests.kt:<line number>)"
            )
        ) {
            state = true
            advance()
        }
    }

    @Test
    fun setContentSubcomposition() = exceptionTest {
        assertTrace(
            listOf(
                "<lambda>(ErrorTraceTests.kt:<unknown line>)",
                "<lambda>(ErrorTraceComposables.kt:66)",
                "Subcompose(ErrorTraceComposables.kt:62)",
                "<lambda>(ErrorTraceTests.kt:<line number>)"
            )
        ) {
            compose { Subcompose { throwTestException() } }
        }
    }

    @Test
    fun recomposeSubcomposition() = exceptionTest {
        var state by mutableStateOf(false)

        compose {
            Subcompose {
                if (state) {
                    throwTestException()
                }
            }
        }

        assertTrace(
            listOf(
                "<lambda>(ErrorTraceTests.kt:<unknown line>)",
                "<lambda>(ErrorTraceComposables.kt:66)",
                "Subcompose(ErrorTraceComposables.kt:62)",
                "<lambda>(ErrorTraceTests.kt:<line number>)"
            )
        ) {
            state = true
            advance()
        }
    }

    @Test
    fun setContentDefaults() = exceptionTest {
        assertTrace(
            listOf(
                "<lambda>(ErrorTraceTests.kt:<unknown line>)",
                "ComposableWithDefaults(ErrorTraceComposables.kt:109)",
                "<lambda>(ErrorTraceTests.kt:<line number>)"
            )
        ) {
            compose { ComposableWithDefaults { throwTestException() } }
        }
    }

    @Test
    fun recomposeDefaults() = exceptionTest {
        var state by mutableStateOf(false)

        compose {
            ComposableWithDefaults {
                if (state) {
                    throwTestException()
                }
            }
        }

        assertTrace(
            listOf(
                "<lambda>(ErrorTraceTests.kt:<unknown line>)",
                "ComposableWithDefaults(ErrorTraceComposables.kt:109)",
                "<lambda>(ErrorTraceTests.kt:<line number>)"
            )
        ) {
            state = true
            advance()
        }
    }

    @Test
    fun setContentRemember() = exceptionTest {
        assertTrace(
            listOf(
                "remember(ErrorTraceTests.kt:<unknown line>)",
                "<lambda>(ErrorTraceTests.kt:<line number>)"
            )
        ) {
            compose { remember { throwTestException() } }
        }
    }

    @Test
    fun setContentRememberObserver() = exceptionTest {
        assertTrace(
            listOf(
                "remember(Effects.kt:<unknown line>)",
                "DisposableEffect(Effects.kt:<line number>)",
                "<lambda>(ErrorTraceTests.kt:<line number>)"
            )
        ) {
            compose { DisposableEffect(Unit) { throwTestException() } }
        }
    }

    @Test
    fun recomposeRememberObserver() = exceptionTest {
        var state by mutableStateOf(false)
        compose {
            DisposableEffect(state) {
                if (state) {
                    throwTestException()
                }
                onDispose {}
            }
        }

        assertTrace(
            listOf(
                "remember(Effects.kt:<unknown line>)",
                "DisposableEffect(Effects.kt:<line number>)",
                "<lambda>(ErrorTraceTests.kt:<line number>)"
            )
        ) {
            state = true
            advance()
        }
    }

    @Test
    fun nodeReuse() = exceptionTest {
        var state by mutableStateOf(false)
        compose { ReusableContent(state) { NodeWithCallbacks(onReuse = { throwTestException() }) } }

        assertTrace(
            listOf(
                // missing ReusableComposeNode because writer is not set directly to the node group
                "NodeWithCallbacks(ErrorTraceComposables.kt:<unknown line>)",
                "<lambda>(ErrorTraceTests.kt:<line number>)",
                "ReusableContent(Composables.kt:<line number>)",
                "<lambda>(ErrorTraceTests.kt:<line number>)"
            )
        ) {
            state = true
            advance()
        }
    }

    @Test
    fun nodeDeactivate() = exceptionTest {
        var active by mutableStateOf(true)
        compose {
            ReusableContentHost(active) {
                NodeWithCallbacks(onDeactivate = { throwTestException() })
            }
        }

        assertTrace(
            listOf(
                "ReusableComposeNode(Composables.kt:<unknown line>)",
                "NodeWithCallbacks(ErrorTraceComposables.kt:121)",
                "<lambda>(ErrorTraceTests.kt:<line number>)",
                "ReusableContentHost(Composables.kt:<line number>)",
                "<lambda>(ErrorTraceTests.kt:<line number>)"
            )
        ) {
            active = false
            advance()
        }
    }

    @Test
    fun setContentNodeAttach() = exceptionTest {
        assertTrace(
            listOf(
                "ReusableComposeNode(Composables.kt:<unknown line>)",
                "NodeWithCallbacks(ErrorTraceComposables.kt:121)",
                "<lambda>(ErrorTraceTests.kt:<line number>)",
                "InlineWrapper(ErrorTraceComposables.kt:57)",
                "<lambda>(ErrorTraceTests.kt:<line number>)"
            )
        ) {
            compose { InlineWrapper { NodeWithCallbacks(onAttach = { throwTestException() }) } }
        }
    }

    @Test
    fun recomposeNodeAttach() = exceptionTest {
        var state by mutableStateOf(false)
        compose {
            Wrapper {
                if (state) {
                    NodeWithCallbacks(onAttach = { throwTestException() })
                }
            }
        }

        assertTrace(
            listOf(
                "ReusableComposeNode(Composables.kt:<unknown line>)",
                "NodeWithCallbacks(ErrorTraceComposables.kt:121)",
                "<lambda>(ErrorTraceTests.kt:<line number>)",
                "Wrapper(ErrorTraceComposables.kt:149)",
                "<lambda>(ErrorTraceTests.kt:<line number>)"
            )
        ) {
            state = true
            advance()
        }
    }

    @Test
    fun recomposeNodeAttachInlineWrapper() = exceptionTest {
        var state by mutableStateOf(false)
        compose {
            InlineWrapper {
                if (state) {
                    NodeWithCallbacks(onAttach = { throwTestException() })
                }
            }
        }

        assertTrace(
            listOf(
                "ReusableComposeNode(Composables.kt:<unknown line>)",
                "NodeWithCallbacks(ErrorTraceComposables.kt:121)",
                // (b/380272059): groupless source information is missing here after recomposition
                //                "<lambda>(ErrorTraceTests.kt:<line number>)",
                //                "InlineWrapper(ErrorTraceComposables.kt:148)",
                "<lambda>(ErrorTraceTests.kt:<line number>)"
            )
        ) {
            state = true
            advance()
        }
    }

    @Test
    fun emptySourceInformation() = exceptionTest {
        val list = listOf(1, 2, 3)
        var content: (@Composable () -> Unit)? = null
        // some gymnastics to ensure that Kotlin generates a null check
        if (3 in list) {
            content = { throwTestException() }
        }

        assertTrace(
            listOf(
                "<lambda>(ErrorTraceTests.kt:<unknown line>)",
                "<lambda>(ErrorTraceTests.kt:<line number>)",
                "InlineWrapper(ErrorTraceComposables.kt:57)",
                "<lambda>(ErrorTraceTests.kt:<line number>)"
            )
        ) {
            compose { InlineWrapper { list.fastForEach { key(it) { content?.invoke() } } } }
        }
    }

    @Test
    fun setContentNodeUpdate() = exceptionTest {
        assertTrace(
            listOf(
                "ReusableComposeNode(Composables.kt:<unknown line>)",
                "NodeWithCallbacks(ErrorTraceComposables.kt:121)",
                "<lambda>(ErrorTraceTests.kt:<line number>)",
                "Wrapper(ErrorTraceComposables.kt:149)",
                "<lambda>(ErrorTraceTests.kt:<line number>)"
            )
        ) {
            compose { Wrapper { NodeWithCallbacks(onUpdate = { throwTestException() }) } }
        }
    }

    @Test
    fun recomposeUpdate() = exceptionTest {
        var state by mutableStateOf(false)
        compose {
            Wrapper {
                if (state) {
                    NodeWithCallbacks(
                        onUpdate =
                            if (state) {
                                { throwTestException() }
                            } else {
                                {}
                            }
                    )
                }
            }
        }

        assertTrace(
            listOf(
                "ReusableComposeNode(Composables.kt:<unknown line>)",
                "NodeWithCallbacks(ErrorTraceComposables.kt:121)",
                "<lambda>(ErrorTraceTests.kt:<line number>)",
                "Wrapper(ErrorTraceComposables.kt:149)",
                "<lambda>(ErrorTraceTests.kt:<line number>)"
            )
        ) {
            state = true
            advance()
        }
    }
}

private class ExceptionTestScope(
    private val externalExceptions: List<Throwable>,
    private val scope: CompositionTestScope
) : CompositionTestScope by scope {
    fun assertTrace(expected: List<String>, block: () -> Unit) {
        var exception: TestComposeException? = null
        try {
            block()
        } catch (e: TestComposeException) {
            exception = e
        } finally {
            try {
                scope.composition?.dispose()
            } catch (e: Throwable) {
                // swallow
            } finally {
                scope.composition = null
            }
        }
        exception =
            exception
                ?: externalExceptions.firstOrNull { it is TestComposeException }
                    as? TestComposeException
                ?: error("Composition exception was not caught or not thrown")

        val composeTrace =
            exception.suppressedExceptions.firstOrNull { it is DiagnosticComposeException }
        if (composeTrace == null) {
            throw exception
        }
        val message = composeTrace.message.orEmpty()
        val frameString =
            message
                .substringAfter("Composition stack when thrown:\n")
                .lines()
                .filter { it.isNotEmpty() }
                .map {
                    val trace = it.removePrefix("\tat ")
                    // Only keep the lines in the test file
                    if (trace.contains(TestFile)) {
                        trace
                    } else {
                        val line = trace.substringAfter(':').substringBefore(')')
                        if (line == "<unknown line>" || DebugKeepLineNumbers) {
                            trace
                        } else {
                            trace.replace(line, "<line number>")
                        }
                    }
                }
                .joinToString(",\n") { "\"$it\"" }

        val expectedString = expected.joinToString(",\n") { "\"$it\"" }
        assertEquals(expectedString, frameString)
    }
}

private fun throwTestException(): Nothing = throw TestComposeException()

private class TestComposeException : Exception("Test exception")

private const val TestFile = "ErrorTraceComposables.kt"
private const val DebugKeepLineNumbers = false

private fun exceptionTest(block: ExceptionTestScope.() -> Unit) {
    val recomposeExceptions = mutableListOf<Exception>()
    compositionTest(
        recomposeInvoker = {
            try {
                it()
            } catch (e: TestComposeException) {
                recomposeExceptions += e
            }
        }
    ) {
        val scope = ExceptionTestScope(recomposeExceptions, this)
        with(scope) { block() }
    }
}
