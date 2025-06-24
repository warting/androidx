/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.xr.runtime.rxjava3

import android.app.Activity
import androidx.activity.ComponentActivity
import androidx.lifecycle.Lifecycle
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.xr.runtime.Session
import androidx.xr.runtime.SessionCreateSuccess
import com.google.common.truth.Truth.assertThat
import kotlin.time.Duration.Companion.hours
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf

@RunWith(AndroidJUnit4::class)
class FlowsTest {
    private lateinit var activity: Activity
    private lateinit var testDispatcher: TestDispatcher
    private lateinit var testScope: TestScope

    @get:Rule
    val activityScenarioRule =
        ActivityScenarioRule<ComponentActivity>(ComponentActivity::class.java)

    @Before
    fun setUp() {
        activityScenarioRule.scenario.onActivity { this.activity = it }
        shadowOf(activity)
            .grantPermissions(
                "android.permission.SCENE_UNDERSTANDING_COARSE",
                "android.permission.HAND_TRACKING",
            )

        testDispatcher = StandardTestDispatcher()
        testScope = TestScope(testDispatcher)
    }

    @Test
    @OptIn(ExperimentalCoroutinesApi::class)
    fun toObservable_terminatesObservableWhenSessionIsDestroyed() =
        runTest(testDispatcher) {
            val session = (Session.create(activity, testDispatcher) as SessionCreateSuccess).session
            var isTerminated = false

            val observable =
                toObservable(
                        session,
                        flow {
                            emit(1)
                            delay(1.hours)
                            emit(2)
                        },
                    )
                    .doOnTerminate() { isTerminated = true }
            observable.subscribe()
            activityScenarioRule.scenario.moveToState(Lifecycle.State.DESTROYED)
            testScope.advanceUntilIdle()

            assertThat(isTerminated).isTrue()
        }
}
