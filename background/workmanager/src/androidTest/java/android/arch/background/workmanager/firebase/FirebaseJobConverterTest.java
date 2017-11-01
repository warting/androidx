/*
 * Copyright (C) 2017 The Android Open Source Project
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

package android.arch.background.workmanager.firebase;

import static android.arch.background.workmanager.WorkSpecs.getWorkSpec;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.core.Is.is;

import android.arch.background.workmanager.Work;
import android.arch.background.workmanager.model.Constraints;
import android.arch.background.workmanager.model.WorkSpec;
import android.arch.background.workmanager.worker.TestWorker;
import android.net.Uri;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import com.firebase.jobdispatcher.Constraint;
import com.firebase.jobdispatcher.FirebaseJobDispatcher;
import com.firebase.jobdispatcher.GooglePlayDriver;
import com.firebase.jobdispatcher.Job;
import com.firebase.jobdispatcher.JobTrigger;
import com.firebase.jobdispatcher.Lifetime;
import com.firebase.jobdispatcher.ObservedUri;
import com.firebase.jobdispatcher.RetryStrategy;
import com.firebase.jobdispatcher.Trigger;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;
import java.util.concurrent.TimeUnit;

@RunWith(AndroidJUnit4.class)
public class FirebaseJobConverterTest {
    private FirebaseJobConverter mConverter;

    @Before
    public void setUp() {
        FirebaseJobDispatcher dispatcher = new FirebaseJobDispatcher(
                new GooglePlayDriver(InstrumentationRegistry.getTargetContext()));
        mConverter = new FirebaseJobConverter(dispatcher);
    }

    @Test
    @SmallTest
    public void testConvert_basicWorkSpec() {
        final String expectedWorkSpecId = "026e3422-9cd1-11e7-abc4-cec278b6b50a";
        WorkSpec workSpec = new WorkSpec(expectedWorkSpecId);
        Job job = mConverter.convert(workSpec);
        assertThat(job.getTag(), is(expectedWorkSpecId));
        assertThat(job.getLifetime(), is(Lifetime.FOREVER));
        assertThat(job.getService(), is(FirebaseJobService.class.getName()));

        JobTrigger.ImmediateTrigger trigger = (JobTrigger.ImmediateTrigger) job.getTrigger();
        assertThat(trigger, is(Trigger.NOW));
    }

    @Test
    @SmallTest
    public void testConvert_backoffPolicy() {
        long givenBackoffDelayDuration = 50000L;
        WorkSpec workSpec = new WorkSpec("id");
        workSpec.setBackoffDelayDuration(givenBackoffDelayDuration);
        workSpec.setBackoffPolicy(Work.BACKOFF_POLICY_LINEAR);
        Job job = mConverter.convert(workSpec);

        int expectedBackoffDelayDuration = (int) TimeUnit.SECONDS
                .convert(givenBackoffDelayDuration, TimeUnit.MILLISECONDS);
        assertThat(job.getRetryStrategy().getInitialBackoff(), is(expectedBackoffDelayDuration));
        assertThat(job.getRetryStrategy().getPolicy(), is(RetryStrategy.RETRY_POLICY_LINEAR));
    }

    @Test
    @SmallTest
    public void testConvert_initialDelay() {
        long givenInitialDelayDuration = 50000L;
        WorkSpec workSpec = new WorkSpec("id");
        workSpec.setInitialDelay(givenInitialDelayDuration);
        Job job = mConverter.convert(workSpec);

        // Initial delay is handled via an AlarmManager broadcast
        assertThat(job.getTrigger(), is(instanceOf(JobTrigger.ImmediateTrigger.class)));
    }

    @Test
    @SmallTest
    public void testConvert_requireContentUriTrigger() {
        final Uri expectedUri = Uri.parse("TEST_URI");
        final ObservedUri expectedObservedUri =
                new ObservedUri(expectedUri, ObservedUri.Flags.FLAG_NOTIFY_FOR_DESCENDANTS);
        WorkSpec workSpec = getWorkSpec(TestWorker.class, new Constraints.Builder()
                .addContentUriTrigger(expectedUri, true).build());
        Job job = mConverter.convert(workSpec);

        JobTrigger.ContentUriTrigger trigger = (JobTrigger.ContentUriTrigger) job.getTrigger();
        List<ObservedUri> observedUriList = trigger.getUris();
        assertThat(observedUriList, contains(expectedObservedUri));
    }

    @Test
    @SmallTest
    public void testConvert_requiresCharging() {
        WorkSpec workSpec = getWorkSpec(TestWorker.class, new Constraints.Builder()
                .setRequiresCharging(true).build());
        Job job = mConverter.convert(workSpec);
        assertHasIntInArray(job.getConstraints(), Constraint.DEVICE_CHARGING);
    }

    @Test
    @SmallTest
    public void testConvert_requiresDeviceIdle() {
        WorkSpec workSpec = getWorkSpec(TestWorker.class, new Constraints.Builder()
                .setRequiresDeviceIdle(true).build());
        Job job = mConverter.convert(workSpec);
        assertHasIntInArray(job.getConstraints(), Constraint.DEVICE_IDLE);
    }

    @Test
    @SmallTest
    public void testConvert_requiresNetworkAny() {
        WorkSpec workSpec = getWorkSpec(TestWorker.class, new Constraints.Builder()
                .setRequiredNetworkType(Constraints.NETWORK_TYPE_ANY).build());
        Job job = mConverter.convert(workSpec);
        assertHasIntInArray(job.getConstraints(), Constraint.ON_ANY_NETWORK);
    }

    @Test
    @SmallTest
    public void testConvert_requiresNetworkMetered_unsupported() {
        WorkSpec workSpec = getWorkSpec(TestWorker.class, new Constraints.Builder()
                .setRequiredNetworkType(Constraints.NETWORK_TYPE_METERED).build());
        Job job = mConverter.convert(workSpec);
        assertHasIntInArray(job.getConstraints(), Constraint.ON_ANY_NETWORK);
    }

    @Test
    @SmallTest
    public void testConvert_requiresNetworkNotRoaming_unsupported() {
        WorkSpec workSpec = getWorkSpec(TestWorker.class, new Constraints.Builder()
                .setRequiredNetworkType(Constraints.NETWORK_TYPE_NOT_ROAMING).build());
        Job job = mConverter.convert(workSpec);
        assertHasIntInArray(job.getConstraints(), Constraint.ON_ANY_NETWORK);
    }

    @Test
    @SmallTest
    public void testConvert_requiresNetworkUnmetered() {
        WorkSpec workSpec = getWorkSpec(TestWorker.class, new Constraints.Builder()
                .setRequiredNetworkType(Constraints.NETWORK_TYPE_UNMETERED).build());
        Job job = mConverter.convert(workSpec);
        assertHasIntInArray(job.getConstraints(), Constraint.ON_UNMETERED_NETWORK);
    }

    private void assertHasIntInArray(int[] array, int expectedItem) {
        boolean found = false;
        for (int item : array) {
            if (item == expectedItem) {
                found = true;
                break;
            }
        }
        assertThat(found, is(true));
    }
}
