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
package androidx.work.impl.constraints.trackers;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Build;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;
import androidx.work.impl.constraints.ConstraintListener;
import androidx.work.impl.utils.taskexecutor.InstantWorkTaskExecutor;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class BatteryChargingTrackerTest {

    private BatteryChargingTracker mTracker;
    private ConstraintListener<Boolean> mListener;
    private Context mMockContext;

    @SuppressWarnings("unchecked")
    @Before
    public void setUp() {
        mMockContext = mock(Context.class);
        when(mMockContext.getApplicationContext()).thenReturn(mMockContext);
        mTracker = new BatteryChargingTracker(mMockContext, new InstantWorkTaskExecutor());
        mListener = mock(ConstraintListener.class);
    }

    private void mockContextReturns(Intent expectedIntent) {
        when(mMockContext.registerReceiver((BroadcastReceiver) isNull(),
                any(IntentFilter.class))).thenReturn(expectedIntent);
    }

    private Intent createBatteryChangedIntent(boolean charging) {
        Intent intent = new Intent(Intent.ACTION_BATTERY_CHANGED);
        if (Build.VERSION.SDK_INT >= 23) {
            int status = charging ? BatteryManager.BATTERY_STATUS_CHARGING
                    : BatteryManager.BATTERY_STATUS_DISCHARGING;
            intent.putExtra(BatteryManager.EXTRA_STATUS, status);
        } else {
            int plugged = charging ? 1 : 0;
            intent.putExtra(BatteryManager.EXTRA_PLUGGED, plugged);
        }
        return intent;
    }

    private Intent createChargingIntent(boolean charging) {
        return new Intent(
                charging ? Intent.ACTION_POWER_CONNECTED : Intent.ACTION_POWER_DISCONNECTED);
    }

    private Intent createChargingIntent_afterApi23(boolean charging) {
        return new Intent(
                charging ? BatteryManager.ACTION_CHARGING : BatteryManager.ACTION_DISCHARGING);
    }

    @Test
    @SmallTest
    public void testReadSystemState_nullIntent() {
        mockContextReturns(null);
        assertThat(mTracker.readSystemState(), is(false));
    }

    @Test
    @SmallTest
    public void testReadSystemState_chargingIntent() {
        mockContextReturns(createBatteryChangedIntent(true));
        assertThat(mTracker.readSystemState(), is(true));
    }

    @Test
    @SmallTest
    public void testReadSystemState_dischargingIntent() {
        mockContextReturns(createBatteryChangedIntent(false));
        assertThat(mTracker.readSystemState(), is(false));
    }

    @Test
    @SmallTest
    public void testGetIntentFilter_afterApi23() {
        IntentFilter intentFilter = mTracker.getIntentFilter();
        assertThat(intentFilter.hasAction(BatteryManager.ACTION_CHARGING), is(true));
        assertThat(intentFilter.hasAction(BatteryManager.ACTION_DISCHARGING), is(true));
        assertThat(intentFilter.countActions(), is(2));
    }

    @Test
    @SmallTest
    public void testOnBroadcastReceive_invalidIntentAction_doesNotNotifyListeners() {
        mockContextReturns(createBatteryChangedIntent(true));
        mTracker.addListener(mListener);
        verify(mListener).onConstraintChanged(true);

        mTracker.onBroadcastReceive(new Intent("INVALID"));
        verifyNoMoreInteractions(mListener);
    }

    @Test
    @SmallTest
    public void testOnBroadcastReceive_notifiesListeners_afterApi23() {
        mockContextReturns(null);
        mTracker.addListener(mListener);
        verify(mListener).onConstraintChanged(false);

        mTracker.onBroadcastReceive(createChargingIntent_afterApi23(true));
        verify(mListener).onConstraintChanged(true);
        mTracker.onBroadcastReceive(createChargingIntent_afterApi23(false));
        // onConstraintChanged was called once more, in total, twice
        verify(mListener, times(2)).onConstraintChanged(false);
    }
}
