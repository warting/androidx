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
import com.android.tools.lint.checks.infrastructure.TestMode
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)

/** Test for [UnrememberedStateDetector]. */
class UnrememberedStateDetectorTest : LintDetectorTest() {
    override fun getDetector(): Detector = UnrememberedStateDetector()

    override fun getIssues(): MutableList<Issue> =
        mutableListOf(UnrememberedStateDetector.UnrememberedState)

    @Test
    fun notRemembered() {
        lint()
            .files(
                kotlin(
                    """
                package androidx.compose.runtime.foo

                import androidx.compose.runtime.*

                @Composable
                fun Test() {
                    val foo = mutableStateOf(0)
                    val bar = mutableStateListOf<Int>()
                    val baz = mutableStateMapOf<Int, Float>()
                    val derived = derivedStateOf { foo.value }
                }

                val lambda = @Composable {
                    val foo = mutableStateOf(0)
                    val bar = mutableStateListOf<Int>()
                    val baz = mutableStateMapOf<Int, Float>()
                    val derived = derivedStateOf { foo.value }
                }

                val lambda2: @Composable () -> Unit = {
                    val foo = mutableStateOf(0)
                    val bar = mutableStateListOf<Int>()
                    val baz = mutableStateMapOf<Int, Float>()
                    val derived = derivedStateOf { foo.value }
                }

                @Composable
                fun LambdaParameter(content: @Composable () -> Unit) {}

                @Composable
                fun Test2() {
                    LambdaParameter(content = {
                        val foo = mutableStateOf(0)
                        val bar = mutableStateListOf<Int>()
                        val baz = mutableStateMapOf<Int, Float>()
                        val derived = derivedStateOf { foo.value }
                    })
                    LambdaParameter {
                        val foo = mutableStateOf(0)
                        val bar = mutableStateListOf<Int>()
                        val baz = mutableStateMapOf<Int, Float>()
                        val derived = derivedStateOf { foo.value }
                    }
                }

                fun test3() {
                    val localLambda1 = @Composable {
                        val foo = mutableStateOf(0)
                        val bar = mutableStateListOf<Int>()
                        val baz = mutableStateMapOf<Int, Float>()
                        val derived = derivedStateOf { foo.value }
                    }

                    val localLambda2: @Composable () -> Unit = {
                        val foo = mutableStateOf(0)
                        val bar = mutableStateListOf<Int>()
                        val baz = mutableStateMapOf<Int, Float>()
                        val derived = derivedStateOf { foo.value }
                    }
                }

                @Composable
                fun Test4() {
                    val localObject = object {
                        val foo = mutableStateOf(0)
                        val bar = mutableStateListOf<Int>()
                        val baz = mutableStateMapOf<Int, Float>()
                        val derived = derivedStateOf { foo.value }
                    }
                }
            """
                ),
                Stubs.Composable,
                Stubs.SnapshotState,
                Stubs.StateFactoryMarker,
                Stubs.Remember,
            )
            .skipTestModes(TestMode.TYPE_ALIAS)
            .run()
            .expect(
                """
src/androidx/compose/runtime/foo/{.kt:8: Error: Creating a state object during composition without using remember [UnrememberedMutableState]
                    val foo = mutableStateOf(0)
                              ~~~~~~~~~~~~~~
src/androidx/compose/runtime/foo/{.kt:9: Error: Creating a state object during composition without using remember [UnrememberedMutableState]
                    val bar = mutableStateListOf<Int>()
                              ~~~~~~~~~~~~~~~~~~
src/androidx/compose/runtime/foo/{.kt:10: Error: Creating a state object during composition without using remember [UnrememberedMutableState]
                    val baz = mutableStateMapOf<Int, Float>()
                              ~~~~~~~~~~~~~~~~~
src/androidx/compose/runtime/foo/{.kt:11: Error: Creating a state object during composition without using remember [UnrememberedMutableState]
                    val derived = derivedStateOf { foo.value }
                                  ~~~~~~~~~~~~~~
src/androidx/compose/runtime/foo/{.kt:15: Error: Creating a state object during composition without using remember [UnrememberedMutableState]
                    val foo = mutableStateOf(0)
                              ~~~~~~~~~~~~~~
src/androidx/compose/runtime/foo/{.kt:16: Error: Creating a state object during composition without using remember [UnrememberedMutableState]
                    val bar = mutableStateListOf<Int>()
                              ~~~~~~~~~~~~~~~~~~
src/androidx/compose/runtime/foo/{.kt:17: Error: Creating a state object during composition without using remember [UnrememberedMutableState]
                    val baz = mutableStateMapOf<Int, Float>()
                              ~~~~~~~~~~~~~~~~~
src/androidx/compose/runtime/foo/{.kt:18: Error: Creating a state object during composition without using remember [UnrememberedMutableState]
                    val derived = derivedStateOf { foo.value }
                                  ~~~~~~~~~~~~~~
src/androidx/compose/runtime/foo/{.kt:22: Error: Creating a state object during composition without using remember [UnrememberedMutableState]
                    val foo = mutableStateOf(0)
                              ~~~~~~~~~~~~~~
src/androidx/compose/runtime/foo/{.kt:23: Error: Creating a state object during composition without using remember [UnrememberedMutableState]
                    val bar = mutableStateListOf<Int>()
                              ~~~~~~~~~~~~~~~~~~
src/androidx/compose/runtime/foo/{.kt:24: Error: Creating a state object during composition without using remember [UnrememberedMutableState]
                    val baz = mutableStateMapOf<Int, Float>()
                              ~~~~~~~~~~~~~~~~~
src/androidx/compose/runtime/foo/{.kt:25: Error: Creating a state object during composition without using remember [UnrememberedMutableState]
                    val derived = derivedStateOf { foo.value }
                                  ~~~~~~~~~~~~~~
src/androidx/compose/runtime/foo/{.kt:34: Error: Creating a state object during composition without using remember [UnrememberedMutableState]
                        val foo = mutableStateOf(0)
                                  ~~~~~~~~~~~~~~
src/androidx/compose/runtime/foo/{.kt:35: Error: Creating a state object during composition without using remember [UnrememberedMutableState]
                        val bar = mutableStateListOf<Int>()
                                  ~~~~~~~~~~~~~~~~~~
src/androidx/compose/runtime/foo/{.kt:36: Error: Creating a state object during composition without using remember [UnrememberedMutableState]
                        val baz = mutableStateMapOf<Int, Float>()
                                  ~~~~~~~~~~~~~~~~~
src/androidx/compose/runtime/foo/{.kt:37: Error: Creating a state object during composition without using remember [UnrememberedMutableState]
                        val derived = derivedStateOf { foo.value }
                                      ~~~~~~~~~~~~~~
src/androidx/compose/runtime/foo/{.kt:40: Error: Creating a state object during composition without using remember [UnrememberedMutableState]
                        val foo = mutableStateOf(0)
                                  ~~~~~~~~~~~~~~
src/androidx/compose/runtime/foo/{.kt:41: Error: Creating a state object during composition without using remember [UnrememberedMutableState]
                        val bar = mutableStateListOf<Int>()
                                  ~~~~~~~~~~~~~~~~~~
src/androidx/compose/runtime/foo/{.kt:42: Error: Creating a state object during composition without using remember [UnrememberedMutableState]
                        val baz = mutableStateMapOf<Int, Float>()
                                  ~~~~~~~~~~~~~~~~~
src/androidx/compose/runtime/foo/{.kt:43: Error: Creating a state object during composition without using remember [UnrememberedMutableState]
                        val derived = derivedStateOf { foo.value }
                                      ~~~~~~~~~~~~~~
src/androidx/compose/runtime/foo/{.kt:49: Error: Creating a state object during composition without using remember [UnrememberedMutableState]
                        val foo = mutableStateOf(0)
                                  ~~~~~~~~~~~~~~
src/androidx/compose/runtime/foo/{.kt:50: Error: Creating a state object during composition without using remember [UnrememberedMutableState]
                        val bar = mutableStateListOf<Int>()
                                  ~~~~~~~~~~~~~~~~~~
src/androidx/compose/runtime/foo/{.kt:51: Error: Creating a state object during composition without using remember [UnrememberedMutableState]
                        val baz = mutableStateMapOf<Int, Float>()
                                  ~~~~~~~~~~~~~~~~~
src/androidx/compose/runtime/foo/{.kt:52: Error: Creating a state object during composition without using remember [UnrememberedMutableState]
                        val derived = derivedStateOf { foo.value }
                                      ~~~~~~~~~~~~~~
src/androidx/compose/runtime/foo/{.kt:56: Error: Creating a state object during composition without using remember [UnrememberedMutableState]
                        val foo = mutableStateOf(0)
                                  ~~~~~~~~~~~~~~
src/androidx/compose/runtime/foo/{.kt:57: Error: Creating a state object during composition without using remember [UnrememberedMutableState]
                        val bar = mutableStateListOf<Int>()
                                  ~~~~~~~~~~~~~~~~~~
src/androidx/compose/runtime/foo/{.kt:58: Error: Creating a state object during composition without using remember [UnrememberedMutableState]
                        val baz = mutableStateMapOf<Int, Float>()
                                  ~~~~~~~~~~~~~~~~~
src/androidx/compose/runtime/foo/{.kt:59: Error: Creating a state object during composition without using remember [UnrememberedMutableState]
                        val derived = derivedStateOf { foo.value }
                                      ~~~~~~~~~~~~~~
src/androidx/compose/runtime/foo/{.kt:66: Error: Creating a state object during composition without using remember [UnrememberedMutableState]
                        val foo = mutableStateOf(0)
                                  ~~~~~~~~~~~~~~
src/androidx/compose/runtime/foo/{.kt:67: Error: Creating a state object during composition without using remember [UnrememberedMutableState]
                        val bar = mutableStateListOf<Int>()
                                  ~~~~~~~~~~~~~~~~~~
src/androidx/compose/runtime/foo/{.kt:68: Error: Creating a state object during composition without using remember [UnrememberedMutableState]
                        val baz = mutableStateMapOf<Int, Float>()
                                  ~~~~~~~~~~~~~~~~~
src/androidx/compose/runtime/foo/{.kt:69: Error: Creating a state object during composition without using remember [UnrememberedMutableState]
                        val derived = derivedStateOf { foo.value }
                                      ~~~~~~~~~~~~~~
32 errors, 0 warnings
            """
            )
    }

    @Test
    fun rememberedInsideComposableBody() {
        lint()
            .files(
                kotlin(
                    """
                package androidx.compose.runtime.foo

                import androidx.compose.runtime.*

                @Composable
                fun Test() {
                    val foo = remember { mutableStateOf(0) }
                    val bar = remember { mutableStateListOf<Int>() }
                    val baz = remember {
                        val test = "test"
                        mutableStateMapOf<Int, Float>()
                    }
                    val derived = remember { derivedStateOf { foo.value } }
                }

                val lambda = @Composable {
                    val foo = remember { mutableStateOf(0) }
                    val bar = remember { mutableStateListOf<Int>() }
                    val baz = remember {
                        val test = "test"
                        mutableStateMapOf<Int, Float>()
                    }
                    val derived = remember { derivedStateOf { foo.value } }
                }

                val lambda2: @Composable () -> Unit = {
                    val foo = remember { mutableStateOf(0) }
                    val bar = remember { mutableStateListOf<Int>() }
                    val baz = remember {
                        val test = "test"
                        mutableStateMapOf<Int, Float>()
                    }
                    val derived = remember { derivedStateOf { foo.value } }
                }

                @Composable
                fun LambdaParameter(content: @Composable () -> Unit) {}

                @Composable
                fun Test2() {
                    LambdaParameter(content = {
                        val foo = remember { mutableStateOf(0) }
                        val bar = remember { mutableStateListOf<Int>() }
                        val baz = remember {
                            val test = "test"
                            mutableStateMapOf<Int, Float>()
                        }
                        val derived = remember { derivedStateOf { foo.value } }
                    })
                    LambdaParameter {
                        val foo = remember { mutableStateOf(0) }
                        val bar = remember { mutableStateListOf<Int>() }
                        val baz = remember {
                            val test = "test"
                            mutableStateMapOf<Int, Float>()
                        }
                        val derived = remember { derivedStateOf { foo.value } }
                    }
                }

                fun test3() {
                    val localLambda1 = @Composable {
                        val foo = remember { mutableStateOf(0) }
                        val bar = remember { mutableStateListOf<Int>() }
                        val baz = remember {
                            val test = "test"
                            mutableStateMapOf<Int, Float>()
                        }
                        val derived = remember { derivedStateOf { foo.value } }
                    }

                    val localLambda2: @Composable () -> Unit = {
                        val foo = remember { mutableStateOf(0) }
                        val bar = remember { mutableStateListOf<Int>() }
                        val baz = remember {
                            val test = "test"
                            mutableStateMapOf<Int, Float>()
                        }
                        val derived = remember { derivedStateOf { foo.value } }
                    }
                }
            """
                ),
                Stubs.Composable,
                Stubs.SnapshotState,
                Stubs.StateFactoryMarker,
                Stubs.Remember,
            )
            .run()
            .expectClean()
    }

    @Test
    fun noErrors() {
        lint()
            .files(
                kotlin(
                    """
                package androidx.compose.runtime.foo

                import androidx.compose.runtime.*

                fun test() {
                    val foo = mutableStateOf(0)
                    val bar = mutableStateListOf<Int>()
                    val baz = mutableStateMapOf<Int, Float>()
                    val derived = derivedStateOf { foo.value }
                }

                val lambda = {
                    val foo = mutableStateOf(0)
                    val bar = mutableStateListOf<Int>()
                    val baz = mutableStateMapOf<Int, Float>()
                    val derived = derivedStateOf { foo.value }
                }

                val lambda2: () -> Unit = {
                    val foo = mutableStateOf(0)
                    val bar = mutableStateListOf<Int>()
                    val baz = mutableStateMapOf<Int, Float>()
                    val derived = derivedStateOf { foo.value }
                }

                fun LambdaParameter(content: () -> Unit) {}

                fun test2() {
                    LambdaParameter(content = {
                        val foo = mutableStateOf(0)
                        val bar = mutableStateListOf<Int>()
                        val baz = mutableStateMapOf<Int, Float>()
                        val derived = derivedStateOf { foo.value }
                    })
                    LambdaParameter {
                        val foo = mutableStateOf(0)
                        val bar = mutableStateListOf<Int>()
                        val baz = mutableStateMapOf<Int, Float>()
                        val derived = derivedStateOf { foo.value }
                    }
                }

                fun test3() {
                    val localLambda1 = {
                        val foo = mutableStateOf(0)
                        val bar = mutableStateListOf<Int>()
                        val baz = mutableStateMapOf<Int, Float>()
                        val derived = derivedStateOf { foo.value }
                    }

                    val localLambda2: () -> Unit = {
                        val foo = mutableStateOf(0)
                        val bar = mutableStateListOf<Int>()
                        val baz = mutableStateMapOf<Int, Float>()
                        val derived = derivedStateOf { foo.value }
                    }
                }

                fun test3() {
                    class Foo {
                        val foo = mutableStateOf(0)
                        val bar = mutableStateListOf<Int>()
                        val baz = mutableStateMapOf<Int, Float>()
                        val derived = derivedStateOf { foo.value }
                    }

                    val localObject = object {
                        val foo = mutableStateOf(0)
                        val bar = mutableStateListOf<Int>()
                        val baz = mutableStateMapOf<Int, Float>()
                        val derived = derivedStateOf { foo.value }
                    }
                }

                @Composable
                fun Test4() {
                    class Foo {
                        val foo = mutableStateOf(0)
                        val bar = mutableStateListOf<Int>()
                        val baz = mutableStateMapOf<Int, Float>()
                        val derived = derivedStateOf { foo.value }
                    }
                }
            """
                ),
                Stubs.Composable,
                Stubs.SnapshotState,
                Stubs.Remember,
            )
            .run()
            .expectClean()
    }

    @Test
    fun arbitraryStateFactoryAnnotated() {
        lint()
            .files(
                kotlin(
                    """
                package androidx.compose.runtime.foo

                import androidx.compose.runtime.*
                import androidx.compose.runtime.snapshots.StateFactoryMarker

                @StateFactoryMarker
                fun makeMyState(): State<Nothing> = TODO()

                @Composable
                fun Test() {
                    val foo = makeMyState()
                    val bar = remember { makeMyState() }
                }

                val lambda = @Composable {
                    val foo = makeMyState()
                    val bar = remember { makeMyState() }
                }

                val lambda2: @Composable () -> Unit = {
                    val foo = makeMyState()
                    val bar = remember { makeMyState() }
                }

                @Composable
                fun LambdaParameter(content: @Composable () -> Unit) {}

                @Composable
                fun Test2() {
                    LambdaParameter(content = {
                        val foo = makeMyState()
                        val bar = remember { makeMyState() }
                    })
                    LambdaParameter {
                        val foo = makeMyState()
                        val bar = remember { makeMyState() }
                    }
                }

                fun test3() {
                    val localLambda1 = @Composable {
                        val foo = makeMyState()
                        val bar = remember { makeMyState() }
                    }

                    val localLambda2: @Composable () -> Unit = {
                        val foo = makeMyState()
                        val bar = remember { makeMyState() }
                    }
                }

                @Composable
                fun Test4() {
                    val localObject = object {
                        val foo = makeMyState()
                        val bar = remember { makeMyState() }
                    }
                }
            """
                ),
                Stubs.Composable,
                Stubs.SnapshotState,
                Stubs.StateFactoryMarker,
                Stubs.Remember,
            )
            .skipTestModes(TestMode.TYPE_ALIAS)
            .run()
            .expect(
                """
src/androidx/compose/runtime/foo/{.kt:12: Error: Creating a state object during composition without using remember [UnrememberedMutableState]
                    val foo = makeMyState()
                              ~~~~~~~~~~~
src/androidx/compose/runtime/foo/{.kt:17: Error: Creating a state object during composition without using remember [UnrememberedMutableState]
                    val foo = makeMyState()
                              ~~~~~~~~~~~
src/androidx/compose/runtime/foo/{.kt:22: Error: Creating a state object during composition without using remember [UnrememberedMutableState]
                    val foo = makeMyState()
                              ~~~~~~~~~~~
src/androidx/compose/runtime/foo/{.kt:32: Error: Creating a state object during composition without using remember [UnrememberedMutableState]
                        val foo = makeMyState()
                                  ~~~~~~~~~~~
src/androidx/compose/runtime/foo/{.kt:36: Error: Creating a state object during composition without using remember [UnrememberedMutableState]
                        val foo = makeMyState()
                                  ~~~~~~~~~~~
src/androidx/compose/runtime/foo/{.kt:43: Error: Creating a state object during composition without using remember [UnrememberedMutableState]
                        val foo = makeMyState()
                                  ~~~~~~~~~~~
src/androidx/compose/runtime/foo/{.kt:48: Error: Creating a state object during composition without using remember [UnrememberedMutableState]
                        val foo = makeMyState()
                                  ~~~~~~~~~~~
src/androidx/compose/runtime/foo/{.kt:56: Error: Creating a state object during composition without using remember [UnrememberedMutableState]
                        val foo = makeMyState()
                                  ~~~~~~~~~~~
8 errors, 0 warnings
            """
            )
    }

    @Test
    fun customStateConstructor() {
        lint()
            .files(
                kotlin(
                    """
                package androidx.compose.runtime.foo

                import androidx.compose.runtime.*
                import androidx.compose.runtime.snapshots.StateFactoryMarker

                @Composable
                fun Test() {
                    val foo = CustomState<Nothing>()
                    val bar = remember { makeMyState() }
                }

                val lambda = @Composable {
                    val foo = CustomState<Nothing>()
                    val bar = remember { makeMyState() }
                }

                val lambda2: @Composable () -> Unit = {
                    val foo = CustomState<Nothing>()
                    val bar = remember { makeMyState() }
                }

                @Composable
                fun LambdaParameter(content: @Composable () -> Unit) {}

                @Composable
                fun Test2() {
                    LambdaParameter(content = {
                        val foo = CustomState<Nothing>()
                        val bar = remember { makeMyState() }
                    })
                    LambdaParameter {
                        val foo = CustomState<Nothing>()
                        val bar = remember { makeMyState() }
                    }
                }

                fun test3() {
                    val localLambda1 = @Composable {
                        val foo = CustomState<Nothing>()
                        val bar = remember { makeMyState() }
                    }

                    val localLambda2: @Composable () -> Unit = {
                        val foo = CustomState<Nothing>()
                        val bar = remember { makeMyState() }
                    }
                }

                @Composable
                fun Test4() {
                    val localObject = object {
                        val foo = CustomState<Nothing>()
                        val bar = remember { makeMyState() }
                    }
                }

                class CustomState<T> : MutableState<T> {
                    override var value: T
                        get() = throw UnsupportedOperationException()
                        set(_) { throw UnsupportedOperationException() }
                    override fun component1(): T = value
                    override fun component2(): (T) -> Unit = { value = it }
                }
            """
                ),
                Stubs.Composable,
                Stubs.SnapshotState,
                Stubs.StateFactoryMarker,
                Stubs.Remember,
            )
            .skipTestModes(TestMode.TYPE_ALIAS)
            .run()
            .expect(
                """
src/androidx/compose/runtime/foo/{.kt:9: Error: Creating a state object during composition without using remember [UnrememberedMutableState]
                    val foo = CustomState<Nothing>()
                              ~~~~~~~~~~~
src/androidx/compose/runtime/foo/{.kt:14: Error: Creating a state object during composition without using remember [UnrememberedMutableState]
                    val foo = CustomState<Nothing>()
                              ~~~~~~~~~~~
src/androidx/compose/runtime/foo/{.kt:19: Error: Creating a state object during composition without using remember [UnrememberedMutableState]
                    val foo = CustomState<Nothing>()
                              ~~~~~~~~~~~
src/androidx/compose/runtime/foo/{.kt:29: Error: Creating a state object during composition without using remember [UnrememberedMutableState]
                        val foo = CustomState<Nothing>()
                                  ~~~~~~~~~~~
src/androidx/compose/runtime/foo/{.kt:33: Error: Creating a state object during composition without using remember [UnrememberedMutableState]
                        val foo = CustomState<Nothing>()
                                  ~~~~~~~~~~~
src/androidx/compose/runtime/foo/{.kt:40: Error: Creating a state object during composition without using remember [UnrememberedMutableState]
                        val foo = CustomState<Nothing>()
                                  ~~~~~~~~~~~
src/androidx/compose/runtime/foo/{.kt:45: Error: Creating a state object during composition without using remember [UnrememberedMutableState]
                        val foo = CustomState<Nothing>()
                                  ~~~~~~~~~~~
src/androidx/compose/runtime/foo/{.kt:53: Error: Creating a state object during composition without using remember [UnrememberedMutableState]
                        val foo = CustomState<Nothing>()
                                  ~~~~~~~~~~~
8 errors, 0 warnings
            """
            )
    }

    @Test
    fun customStateAnonymousClass() {
        lint()
            .files(
                kotlin(
                    """
                package androidx.compose.runtime.foo

                import androidx.compose.runtime.*
                import androidx.compose.runtime.snapshots.StateFactoryMarker

                @Composable
                fun Test() {
                    val foo = object : MutableState<Nothing> {
                        override var value: Nothing
                            get() = throw UnsupportedOperationException()
                            set(_) { throw UnsupportedOperationException() }
                        override fun component1() = value
                        override fun component2() = { _: Nothing -> }
                    }
                    val bar = remember {
                        object : MutableState<Nothing> {
                            override var value: Nothing
                                get() = throw UnsupportedOperationException()
                                set(_) { throw UnsupportedOperationException() }
                            override fun component1() = value
                            override fun component2() = { _: Nothing -> }
                        }
                    }
                }

                val lambda = @Composable {
                    val foo = object : MutableState<Nothing> {
                        override var value: Nothing
                            get() = throw UnsupportedOperationException()
                            set(_) { throw UnsupportedOperationException() }
                        override fun component1() = value
                        override fun component2() = { _: Nothing -> }
                    }
                    val bar = remember {
                        object : MutableState<Nothing> {
                            override var value: Nothing
                                get() = throw UnsupportedOperationException()
                                set(_) { throw UnsupportedOperationException() }
                            override fun component1() = value
                            override fun component2() = { _: Nothing -> }
                        }
                    }
                }

                val lambda2: @Composable () -> Unit = {
                    val foo = object : MutableState<Nothing> {
                        override var value: Nothing
                            get() = throw UnsupportedOperationException()
                            set(_) { throw UnsupportedOperationException() }
                        override fun component1() = value
                        override fun component2() = { _: Nothing -> }
                    }
                    val bar = remember {
                        object : MutableState<Nothing> {
                            override var value: Nothing
                                get() = throw UnsupportedOperationException()
                                set(_) { throw UnsupportedOperationException() }
                            override fun component1() = value
                            override fun component2() = { _: Nothing -> }
                        }
                    }
                }

                @Composable
                fun LambdaParameter(content: @Composable () -> Unit) {}

                @Composable
                fun Test2() {
                    LambdaParameter(content = {
                        val foo = object : MutableState<Nothing> {
                            override var value: Nothing
                                get() = throw UnsupportedOperationException()
                                set(_) { throw UnsupportedOperationException() }
                            override fun component1() = value
                            override fun component2() = { _: Nothing -> }
                        }
                        val bar = remember {
                            object : MutableState<Nothing> {
                                override var value: Nothing
                                    get() = throw UnsupportedOperationException()
                                    set(_) { throw UnsupportedOperationException() }
                                override fun component1() = value
                                override fun component2() = { _: Nothing -> }
                            }
                        }
                    })
                    LambdaParameter {
                        val foo = object : MutableState<Nothing> {
                            override var value: Nothing
                                get() = throw UnsupportedOperationException()
                                set(_) { throw UnsupportedOperationException() }
                            override fun component1() = value
                            override fun component2() = { _: Nothing -> }
                        }
                        val bar = remember {
                            object : MutableState<Nothing> {
                                override var value: Nothing
                                    get() = throw UnsupportedOperationException()
                                    set(_) { throw UnsupportedOperationException() }
                                override fun component1() = value
                                override fun component2() = { _: Nothing -> }
                            }
                        }
                    }
                }

                fun test3() {
                    val localLambda1 = @Composable {
                        val foo = object : MutableState<Nothing> {
                            override var value: Nothing
                                get() = throw UnsupportedOperationException()
                                set(_) { throw UnsupportedOperationException() }
                            override fun component1() = value
                            override fun component2() = { _: Nothing -> }
                        }
                        val bar = remember {
                            object : MutableState<Nothing> {
                                override var value: Nothing
                                    get() = throw UnsupportedOperationException()
                                    set(_) { throw UnsupportedOperationException() }
                                override fun component1() = value
                                override fun component2() = { _: Nothing -> }
                            }
                        }
                    }

                    val localLambda2: @Composable () -> Unit = {
                        val foo = object : MutableState<Nothing> {
                            override var value: Nothing
                                get() = throw UnsupportedOperationException()
                                set(_) { throw UnsupportedOperationException() }
                            override fun component1() = value
                            override fun component2() = { _: Nothing -> }
                        }
                        val bar = remember {
                            object : MutableState<Nothing> {
                                override var value: Nothing
                                    get() = throw UnsupportedOperationException()
                                    set(_) { throw UnsupportedOperationException() }
                                override fun component1() = value
                                override fun component2() = { _: Nothing -> }
                            }
                        }
                    }
                }

                @Composable
                fun Test4() {
                    val localObject = object {
                        val foo = object : MutableState<Nothing> {
                            override var value: Nothing
                                get() = throw UnsupportedOperationException()
                                set(_) { throw UnsupportedOperationException() }
                            override fun component1() = value
                            override fun component2() = { _: Nothing -> }
                        }
                        val bar = remember {
                            object : MutableState<Nothing> {
                                override var value: Nothing
                                    get() = throw UnsupportedOperationException()
                                    set(_) { throw UnsupportedOperationException() }
                                override fun component1() = value
                                override fun component2() = { _: Nothing -> }
                            }
                        }
                    }
                }
            """
                ),
                Stubs.Composable,
                Stubs.SnapshotState,
                Stubs.StateFactoryMarker,
                Stubs.Remember,
            )
            .skipTestModes(TestMode.TYPE_ALIAS)
            .run()
            .expect(
                """
src/androidx/compose/runtime/foo/{.kt:9: Error: Creating a state object during composition without using remember [UnrememberedMutableState]
                    val foo = object : MutableState<Nothing> {
                              ^
src/androidx/compose/runtime/foo/{.kt:28: Error: Creating a state object during composition without using remember [UnrememberedMutableState]
                    val foo = object : MutableState<Nothing> {
                              ^
src/androidx/compose/runtime/foo/{.kt:47: Error: Creating a state object during composition without using remember [UnrememberedMutableState]
                    val foo = object : MutableState<Nothing> {
                              ^
src/androidx/compose/runtime/foo/{.kt:71: Error: Creating a state object during composition without using remember [UnrememberedMutableState]
                        val foo = object : MutableState<Nothing> {
                                  ^
src/androidx/compose/runtime/foo/{.kt:89: Error: Creating a state object during composition without using remember [UnrememberedMutableState]
                        val foo = object : MutableState<Nothing> {
                                  ^
src/androidx/compose/runtime/foo/{.kt:110: Error: Creating a state object during composition without using remember [UnrememberedMutableState]
                        val foo = object : MutableState<Nothing> {
                                  ^
src/androidx/compose/runtime/foo/{.kt:129: Error: Creating a state object during composition without using remember [UnrememberedMutableState]
                        val foo = object : MutableState<Nothing> {
                                  ^
src/androidx/compose/runtime/foo/{.kt:151: Error: Creating a state object during composition without using remember [UnrememberedMutableState]
                        val foo = object : MutableState<Nothing> {
                                  ^
8 errors, 0 warnings
            """
            )
    }
}
