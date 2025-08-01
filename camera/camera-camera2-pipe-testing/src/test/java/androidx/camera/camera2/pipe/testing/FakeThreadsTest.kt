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

package androidx.camera.camera2.pipe.testing

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricCameraPipeTestRunner::class)
class FakeThreadsTest {
    private val testScope = TestScope()
    private val fakeThreads = FakeThreads.fromTestScope(testScope)

    @Test
    fun fakeThreadsUseDelaySkipping() =
        testScope.runTest {
            launch(fakeThreads.backgroundDispatcher) { delay(1000000) }.join()
            launch(fakeThreads.lightweightDispatcher) { delay(1000000) }.join()
            fakeThreads.cameraPipeScope.launch { delay(1000000) }.join()

            var backgroundTaskExecuted = false
            var lightweightTaskExecuted = false
            fakeThreads.backgroundExecutor.execute { backgroundTaskExecuted = true }
            fakeThreads.lightweightExecutor.execute { lightweightTaskExecuted = true }
            advanceUntilIdle()

            assertThat(backgroundTaskExecuted).isTrue()
            assertThat(lightweightTaskExecuted).isTrue()
        }

    @Test
    fun exceptionsInDispatcherPropagateToTestScopeFailure() {

        // Exceptions in CameraPipeScope is propagated out of the test.
        assertThrows(RuntimeException::class.java) {
            val scope = TestScope()
            val localFakeThreads = FakeThreads.fromTestScope(scope)
            scope.runTest {
                localFakeThreads.cameraPipeScope.launch {
                    throw RuntimeException("cameraPipeScope")
                }
            }
        }

        // Exceptions in Dispatchers are propagated out of the test.
        assertThrows(RuntimeException::class.java) {
            val scope = TestScope()
            val localFakeThreads = FakeThreads.fromTestScope(scope)
            scope.runTest {
                launch(localFakeThreads.backgroundDispatcher) {
                    throw RuntimeException("backgroundDispatcher")
                }
            }
        }

        // Exceptions in Executors are propagated out of the test.
        assertThrows(RuntimeException::class.java) {
            val scope = TestScope()
            val localFakeThreads = FakeThreads.fromTestScope(scope)
            scope.runTest {
                localFakeThreads.backgroundExecutor.execute {
                    throw RuntimeException("backgroundExecutor")
                }
            }
        }
    }
}
