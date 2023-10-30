/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.window.extensions.embedding;

import android.app.Activity;
import android.graphics.Rect;
import android.os.IBinder;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.window.extensions.RequiresVendorApiLevel;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Description of a group of activities stacked on top of each other and shown as a single
 * container, all within the same task.
 */
public class ActivityStack {

    @NonNull
    private final List<Activity> mActivities;

    private final boolean mIsEmpty;

    @NonNull
    private final IBinder mToken;

    @NonNull
    private final Rect mRelativeBounds;

    @Nullable
    private final String mTag;

    /**
     * The {@code ActivityStack} constructor
     *
     * @param activities {@link Activity Activities} in this application's process that
     *                   belongs to this {@code ActivityStack}
     * @param isEmpty Indicates whether there's any {@link Activity} running in this
     *                {@code ActivityStack}
     * @param token The token to identify this {@code ActivityStack}
     * @param relativeBounds The bounds relative to its parent container
     * @param tag A unique identifier of {@link ActivityStack}. Only specifies for the overlay
     *            standalone {@link ActivityStack} currently.
     */
    ActivityStack(@NonNull List<Activity> activities, boolean isEmpty, @NonNull IBinder token,
            @NonNull Rect relativeBounds, @Nullable String tag) {
        Objects.requireNonNull(activities);
        Objects.requireNonNull(token);
        Objects.requireNonNull(relativeBounds);

        mActivities = new ArrayList<>(activities);
        mIsEmpty = isEmpty;
        mToken = token;
        mRelativeBounds = relativeBounds;
        mTag = tag;
    }

    /**
     * Returns {@link Activity Activities} in this application's process that belongs to this
     * ActivityStack.
     * <p>
     * Note that Activities that are running in other processes are not reported in the returned
     * Activity list. They can be in any position in terms of ordering relative to the activities
     * in the list.
     * </p>
     */
    @NonNull
    public List<Activity> getActivities() {
        return new ArrayList<>(mActivities);
    }

    /**
     * Returns {@code true} if there's no {@link Activity} running in this ActivityStack.
     * <p>
     * Note that {@link #getActivities()} only report Activity in the process used to create this
     * ActivityStack. That said, if this ActivityStack only contains activities from another
     * process, {@link #getActivities()} will return empty list, while this method will return
     * {@code false}.
     * </p>
     */
    public boolean isEmpty() {
        return mIsEmpty;
    }

    /**
     * Returns a token uniquely identifying the container.
     */
    @RequiresVendorApiLevel(level = 5)
    @NonNull
    public IBinder getToken() {
        return mToken;
    }

    /** Returns the bounds relative to its parent container. */
    @RequiresVendorApiLevel(level = 5)
    @NonNull
    public Rect getRelativeBounds() {
        return mRelativeBounds;
    }

    /**
     * Returns the associated tag if specified. Otherwise, returns {@code null}.
     */
    @RequiresVendorApiLevel(level = 5)
    @Nullable
    public String getTag() {
        return mTag;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ActivityStack)) return false;
        ActivityStack that = (ActivityStack) o;
        return mActivities.equals(that.mActivities)
                && mIsEmpty == that.mIsEmpty
                && mToken.equals(that.mToken)
                && mRelativeBounds.equals(that.mRelativeBounds)
                && Objects.equals(mTag, that.mTag);
    }

    @Override
    public int hashCode() {
        int result = (mIsEmpty ? 1 : 0);
        result = result * 31 + mActivities.hashCode();
        result = result * 31 + mToken.hashCode();
        result = result * 31 + mRelativeBounds.hashCode();
        result = result * 31 + Objects.hashCode(mTag);

        return result;
    }

    @NonNull
    @Override
    public String toString() {
        return "ActivityStack{" + "mActivities=" + mActivities
                + ", mIsEmpty=" + mIsEmpty
                + ", mToken=" + mToken
                + ", mRelativeBounds=" + mRelativeBounds
                + ", mTag=" + mTag
                + '}';
    }
}
