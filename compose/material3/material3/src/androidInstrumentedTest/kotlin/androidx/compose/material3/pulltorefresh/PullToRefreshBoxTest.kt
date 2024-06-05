/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.compose.material3.pulltorefresh

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalMaterial3Api::class)
class PullToRefreshBoxTest {

    @get:Rule val rule = createComposeRule()

    @Test
    fun box_startRefreshing_updatesFraction() {
        val state = PullToRefreshState()
        rule.setContent { PullToRefreshBox(isRefreshing = true, state = state, onRefresh = {}) {} }

        assertThat(state.distanceFraction).isEqualTo(1f)
    }

    @Test
    fun box_startNotRefreshing_updatesFraction() {
        val state = PullToRefreshState()
        rule.setContent { PullToRefreshBox(isRefreshing = false, state = state, onRefresh = {}) {} }

        assertThat(state.distanceFraction).isEqualTo(0f)
    }
}
