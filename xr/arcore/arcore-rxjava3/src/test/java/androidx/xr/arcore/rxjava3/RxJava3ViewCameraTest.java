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

package androidx.xr.arcore.rxjava3;

import static androidx.xr.arcore.rxjava3.RxJava3ViewCamera.getStateAsFlowable;

import static com.google.common.truth.Truth.assertThat;

import static kotlinx.coroutines.test.TestCoroutineDispatchersKt.StandardTestDispatcher;

import androidx.activity.ComponentActivity;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.xr.arcore.ViewCamera;
import androidx.xr.runtime.Session;
import androidx.xr.runtime.SessionCreateSuccess;
import androidx.xr.runtime.math.Pose;

import io.reactivex.rxjava3.subscribers.TestSubscriber;

import kotlinx.coroutines.test.TestDispatcher;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class RxJava3ViewCameraTest {
    private Session mSession;
    private TestDispatcher mTestDispatcher;

    @Test
    public void viewCamera_stateAsFlowable_returnsViewCameraState() {
        createTestSessionAndRunTest(() -> {
            ViewCamera underTest = ViewCamera.getAll(mSession).get(0);
            TestSubscriber<ViewCamera.State> testSubscriber = new TestSubscriber<>();

            getStateAsFlowable(underTest).subscribe(testSubscriber);

            assertThat(testSubscriber.values().get(0).getPose()).isEqualTo(Pose.Identity);
        });
    }

    private void createTestSessionAndRunTest(Runnable testBody) {
        try (ActivityScenario<ComponentActivity> scenario = ActivityScenario.launch(
                ComponentActivity.class)) {
            scenario.onActivity(activity -> {
                mTestDispatcher = StandardTestDispatcher(/* scheduler= */ null, /* name= */ null);
                mSession = ((SessionCreateSuccess) Session.create(activity,
                        mTestDispatcher)).getSession();

                try {
                    testBody.run();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        } catch (Exception e) {
            throw new RuntimeException("Error during ActivityScenario setup or teardown", e);
        }
    }
}
