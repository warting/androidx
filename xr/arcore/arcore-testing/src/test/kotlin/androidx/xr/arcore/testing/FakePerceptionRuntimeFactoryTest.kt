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

package androidx.xr.arcore.testing

import androidx.activity.ComponentActivity
import androidx.kruth.assertThat
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.xr.arcore.internal.PerceptionRuntime
import androidx.xr.runtime.Session
import androidx.xr.runtime.SessionCreateSuccess
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FakePerceptionRuntimeFactoryTest {

    @get:Rule val activityRule = ActivityScenarioRule(ComponentActivity::class.java)

    @Test
    fun createRuntime_createsFakeRuntime() {
        activityRule.scenario.onActivity {
            assertThat(
                    (Session.Companion.create(it) as SessionCreateSuccess)
                        .session
                        .runtimes
                        .filterIsInstance<PerceptionRuntime>()
                        .first()
                )
                .isInstanceOf<FakePerceptionRuntime>()
        }
    }
}
