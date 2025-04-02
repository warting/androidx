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

package androidx.work.multiprocess.parcelable;

import static androidx.work.impl.model.WorkTypeConverters.intToState;
import static androidx.work.impl.model.WorkTypeConverters.stateToInt;
import static androidx.work.multiprocess.parcelable.ParcelUtils.readBooleanValue;
import static androidx.work.multiprocess.parcelable.ParcelUtils.writeBooleanValue;

import android.annotation.SuppressLint;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.RestrictTo;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.WorkInfo;

import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * {@link androidx.work.WorkInfo} but parcelable.
 *
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@SuppressLint("BanParcelableUsage")
public class ParcelableWorkInfo implements Parcelable {

    private static final String[] sEMPTY = new String[0];

    private final WorkInfo mWorkInfo;

    public ParcelableWorkInfo(@NonNull WorkInfo workInfo) {
        mWorkInfo = workInfo;
    }

    protected ParcelableWorkInfo(@NonNull Parcel parcel) {
        // id
        UUID id = UUID.fromString(parcel.readString());
        // state
        WorkInfo.State state = intToState(parcel.readInt());
        // outputData
        ParcelableData parcelableOutputData = new ParcelableData(parcel);
        Data output = parcelableOutputData.getData();
        // tags
        String[] tagsArray = parcel.createStringArray();
        Set<String> tags = new HashSet<>(Arrays.asList(tagsArray));
        // progress
        ParcelableData parcelableProgressData = new ParcelableData(parcel);
        Data progress = parcelableProgressData.getData();
        // runAttemptCount
        int runAttemptCount = parcel.readInt();
        // generation
        int generation = parcel.readInt();
        // constraints
        ParcelableConstraints parcelableConstraints = new ParcelableConstraints(parcel);
        Constraints constraints = parcelableConstraints.getConstraints();
        // initialDelay
        long initialDelay = parcel.readLong();
        // periodicityInfo
        boolean hasPeriodicityInfo = readBooleanValue(parcel);
        WorkInfo.PeriodicityInfo periodicityInfo = null;
        if (hasPeriodicityInfo) {
            long repeatInterval = parcel.readLong();
            long flexInterval = parcel.readLong();
            periodicityInfo = new WorkInfo.PeriodicityInfo(repeatInterval, flexInterval);
        }
        // nextScheduleTimeMillis
        long nextScheduleTimeMillis = parcel.readLong();
        // stopReason
        int stopReason = WorkInfo.STOP_REASON_NOT_STOPPED;
        if (Build.VERSION.SDK_INT >= 31) {
            stopReason = parcel.readInt();
        }
        mWorkInfo = new WorkInfo(id, state, tags, output, progress, runAttemptCount, generation,
                constraints, initialDelay, periodicityInfo, nextScheduleTimeMillis, stopReason);
    }

    public @NonNull WorkInfo getWorkInfo() {
        return mWorkInfo;
    }

    public static final Creator<ParcelableWorkInfo> CREATOR = new Creator<ParcelableWorkInfo>() {
        @Override
        public ParcelableWorkInfo createFromParcel(Parcel in) {
            return new ParcelableWorkInfo(in);
        }

        @Override
        public ParcelableWorkInfo[] newArray(int size) {
            return new ParcelableWorkInfo[size];
        }
    };

    @Override
    public int describeContents() {
        // No file descriptors being returned.
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel parcel, int flags) {
        // id
        parcel.writeString(mWorkInfo.getId().toString());
        // state
        parcel.writeInt(stateToInt(mWorkInfo.getState()));
        // outputData
        ParcelableData parcelableOutputData = new ParcelableData(mWorkInfo.getOutputData());
        parcelableOutputData.writeToParcel(parcel, flags);
        // tags
        // Note: converting to a String[] because that is faster.
        List<String> tags = new ArrayList<>(mWorkInfo.getTags());
        parcel.writeStringArray(tags.toArray(sEMPTY));
        // progress
        ParcelableData parcelableProgress = new ParcelableData(mWorkInfo.getProgress());
        parcelableProgress.writeToParcel(parcel, flags);
        // runAttemptCount
        parcel.writeInt(mWorkInfo.getRunAttemptCount());
        // generation
        parcel.writeInt(mWorkInfo.getGeneration());
        // constraints
        ParcelableConstraints parcelableConstraints = new ParcelableConstraints(
                mWorkInfo.getConstraints());
        parcelableConstraints.writeToParcel(parcel, flags);
        // initialDelay
        parcel.writeLong(mWorkInfo.getInitialDelayMillis());
        // periodicityInfo
        WorkInfo.PeriodicityInfo periodicityInfo = mWorkInfo.getPeriodicityInfo();
        boolean hasPeriodicityInfo = periodicityInfo != null;
        writeBooleanValue(parcel, hasPeriodicityInfo);
        if (hasPeriodicityInfo) {
            parcel.writeLong(periodicityInfo.getRepeatIntervalMillis());
            parcel.writeLong(periodicityInfo.getFlexIntervalMillis());
        }
        // nextScheduleTimeMillis
        parcel.writeLong(mWorkInfo.getNextScheduleTimeMillis());
        // stopReason
        if (Build.VERSION.SDK_INT >= 31) {
            parcel.writeInt(mWorkInfo.getStopReason());
        }
    }
}
