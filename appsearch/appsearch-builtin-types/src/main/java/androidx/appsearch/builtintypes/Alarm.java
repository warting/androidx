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

package androidx.appsearch.builtintypes;

import androidx.annotation.IntDef;
import androidx.annotation.IntRange;
import androidx.annotation.OptIn;
import androidx.annotation.RestrictTo;
import androidx.appsearch.annotation.Document;
import androidx.appsearch.app.ExperimentalAppSearchApi;
import androidx.appsearch.utils.DateTimeFormatValidator;
import androidx.core.util.Preconditions;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Calendar;
import java.util.List;

/**
 * AppSearch document representing an {@link Alarm} entity.
 */
@Document(name = "builtin:Alarm")
public class Alarm extends Thing {
    /** The device that this {@link Alarm} belongs to. */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @IntDef({
            ORIGINATING_DEVICE_UNKNOWN,
            ORIGINATING_DEVICE_SMART_PHONE,
            ORIGINATING_DEVICE_SMART_WATCH})
    @Retention(RetentionPolicy.SOURCE)
    public @interface OriginatingDevice {}
    /** The {@link Alarm} belongs to an unknown device. */
    public static final int ORIGINATING_DEVICE_UNKNOWN = 0;
    /** The {@link Alarm} belongs to a smart phone device. */
    public static final int ORIGINATING_DEVICE_SMART_PHONE = 1;
    /** The {@link Alarm} belongs to a smart watch device. */
    public static final int ORIGINATING_DEVICE_SMART_WATCH = 2;

    @Document.BooleanProperty
    private final boolean mEnabled;

    @Document.LongProperty
    private final int[] mDaysOfWeek;

    @Document.LongProperty
    private final int mHour;

    @Document.LongProperty
    private final int mMinute;

    @Document.StringProperty
    private final String mBlackoutPeriodStartDate;

    @Document.StringProperty
    private final String mBlackoutPeriodEndDate;

    @Document.StringProperty
    private final String mRingtone;

    @Document.BooleanProperty
    private final boolean mShouldVibrate;

    @Document.DocumentProperty
    private final AlarmInstance mPreviousInstance;

    @Document.DocumentProperty
    private final AlarmInstance mNextInstance;

    // This property was originally released as computingDevice, and the old name is maintained for
    // compatibility with existing documents. Since the field is not indexed, the impact is limited
    // to use of this field as a property path for projections and inheritance.
    // If this limitation causes problems, we should add the mComputingDevice field back, mark it
    // deprecated, leave it in the API surface, and provide a migrator for convenience of upgrading.
    @Document.LongProperty(name = "computingDevice")
    private final int mOriginatingDevice;

    @OptIn(markerClass = ExperimentalAppSearchApi.class)
    Alarm(@NonNull String namespace, @NonNull String id, int documentScore,
            long creationTimestampMillis, long documentTtlMillis, @Nullable String name,
            @Nullable List<String> alternateNames, @Nullable String description,
            @Nullable String image, @Nullable String url,
            @NonNull List<PotentialAction> potentialActions,
            boolean enabled, int @Nullable [] daysOfWeek, int hour, int minute,
            @Nullable String blackoutPeriodStartDate, @Nullable String blackoutPeriodEndDate,
            @Nullable String ringtone, boolean shouldVibrate,
            @Nullable AlarmInstance previousInstance, @Nullable AlarmInstance nextInstance,
            int originatingDevice) {
        super(namespace, id, documentScore, creationTimestampMillis, documentTtlMillis, name,
                alternateNames, description, image, url, potentialActions);
        mEnabled = enabled;
        mDaysOfWeek = daysOfWeek;
        mHour = hour;
        mMinute = minute;
        mBlackoutPeriodStartDate = blackoutPeriodStartDate;
        mBlackoutPeriodEndDate = blackoutPeriodEndDate;
        mRingtone = ringtone;
        mShouldVibrate = shouldVibrate;
        mPreviousInstance = previousInstance;
        mNextInstance = nextInstance;
        mOriginatingDevice = originatingDevice;
    }

    /** Returns whether or not the {@link Alarm} is active. */
    public boolean isEnabled() {
        return mEnabled;
    }

    /**
     * Returns the scheduled days for repeating.
     *
     * <p>Days of the week can be {@link java.util.Calendar#MONDAY},
     * {@link java.util.Calendar#TUESDAY}, {@link java.util.Calendar#WEDNESDAY},
     * {@link java.util.Calendar#THURSDAY}, {@link java.util.Calendar#FRIDAY},
     * {@link java.util.Calendar#SATURDAY}, or {@link java.util.Calendar#SUNDAY}.
     *
     * <p>If null, or if the list is empty, then the {@link Alarm} does not repeat.
     */
    public int @Nullable [] getDaysOfWeek() {
        return mDaysOfWeek;
    }

    /**
     * Returns the hour the {@link Alarm} will fire.
     *
     * <p>Hours are specified by integers from 0 to 23.
     */
    @IntRange(from = 0, to = 23)
    public int getHour() {
        return mHour;
    }

    /**
     * Returns the minute the {@link Alarm} will fire.
     *
     * <p>Minutes are specified by integers from 0 to 59.
     */
    @IntRange(from = 0, to = 59)
    public int getMinute() {
        return mMinute;
    }

    /**
     * Returns the start date of the blackout period in ISO 8601 format.
     * E.g.: 2022-01-14
     *
     * <p>A blackout period means the {@link Alarm} will not fire during this period.
     *
     * <p>If not set, then it indicates that the blackout period has no start time.
     *
     * <p>If neither blackoutPeriodStartDate and blackoutPeriodEndDate are set, then
     * the blackout period is not defined.
     */
    public @Nullable String getBlackoutPeriodStartDate() {
        return mBlackoutPeriodStartDate;
    }

    /**
     * Returns the end time for the blackout period in ISO 8601 format.
     * E.g.: 2022-01-14
     *
     * <p>A blackout period means the {@link Alarm} will not fire during this period.
     *
     * <p>If not set, then it indicates that the blackout period has no end time.
     *
     * <p>If neither blackoutPeriodStartDate and blackoutPeriodEndDate are set, then
     * the blackout period is not defined.
     */
    public @Nullable String getBlackoutPeriodEndDate() {
        return mBlackoutPeriodEndDate;
    }

    /**
     * Returns the ringtone as a content URI to be played, or
     * {@link android.provider.AlarmClock#VALUE_RINGTONE_SILENT} if no ringtone will be played.
     */
    public @Nullable String getRingtone() {
        return mRingtone;
    }

    /** Returns whether or not to activate the device vibrator when the {@link Alarm} fires. */
    public boolean shouldVibrate() {
        return mShouldVibrate;
    }

    /**
     * Returns the previous {@link AlarmInstance}.
     *
     * <p>The previous {@link AlarmInstance} is most recent past instance that was fired. If
     * there are no past instances, then null will be returned.
     *
     * <p>See {@link AlarmInstance}.
     */
    public @Nullable AlarmInstance getPreviousInstance() {
        return mPreviousInstance;
    }

    /**
     * Returns the next {@link AlarmInstance}.
     *
     * <p>The next {@link AlarmInstance} is the immediate future instance that is scheduled to fire.
     * If there are no future instances, then null will be returned.
     *
     * <p>See {@link AlarmInstance}.
     */
    public @Nullable AlarmInstance getNextInstance() {
        return mNextInstance;
    }

    /** Returns the {@link OriginatingDevice} this alarm belongs to. */
    @OriginatingDevice
    public int getOriginatingDevice() {
        return mOriginatingDevice;
    }

    /** Builder for {@link Alarm}. */
    @Document.BuilderProducer
    public static final class Builder extends BuilderImpl<Builder> {
        /**
         * Constructor for {@link Alarm.Builder}.
         *
         * @param namespace Namespace for the Document. See
         * {@link Document.Namespace}.
         * @param id Unique identifier for the Document. See {@link Document.Id}.
         */
        public Builder(@NonNull String namespace, @NonNull String id) {
            super(namespace, id);
        }

        /**
         * Constructor with all the existing values.
         */
        public Builder(@NonNull Alarm alarm) {
            super(alarm);
        }
    }

    @SuppressWarnings("unchecked")
    static class BuilderImpl<T extends BuilderImpl<T>> extends Thing.BuilderImpl<T> {
        protected boolean mEnabled;
        protected int[] mDaysOfWeek;
        protected int mHour;
        protected int mMinute;
        protected String mBlackoutPeriodStartDate;
        protected String mBlackoutPeriodEndDate;
        protected String mRingtone;
        protected boolean mShouldVibrate;
        protected AlarmInstance mPreviousInstance;
        protected AlarmInstance mNextInstance;
        protected int mOriginatingDevice;

        BuilderImpl(@NonNull String namespace, @NonNull String id) {
            super(namespace, id);
        }

        BuilderImpl(@NonNull Alarm alarm) {
            super(new Thing.Builder(alarm).build());
            mEnabled = alarm.isEnabled();
            mDaysOfWeek = alarm.getDaysOfWeek();
            mHour = alarm.getHour();
            mMinute = alarm.getMinute();
            mBlackoutPeriodStartDate = alarm.getBlackoutPeriodStartDate();
            mBlackoutPeriodEndDate = alarm.getBlackoutPeriodEndDate();
            mRingtone = alarm.getRingtone();
            mShouldVibrate = alarm.shouldVibrate();
            mPreviousInstance = alarm.getPreviousInstance();
            mNextInstance = alarm.getNextInstance();
            mOriginatingDevice = alarm.getOriginatingDevice();
        }

        /** Sets whether or not the {@link Alarm} is active. */
        public @NonNull T setEnabled(boolean enabled) {
            mEnabled = enabled;
            return (T) this;
        }

        /**
         * Sets the scheduled days for repeating.
         *
         * <p>Days of the week can be {@link java.util.Calendar#MONDAY},
         * {@link java.util.Calendar#TUESDAY}, {@link java.util.Calendar#WEDNESDAY},
         * {@link java.util.Calendar#THURSDAY}, {@link java.util.Calendar#FRIDAY},
         * {@link java.util.Calendar#SATURDAY}, or {@link java.util.Calendar#SUNDAY}.
         *
         * <p>If not set, or if the list is empty, then the {@link Alarm} does not repeat.
         */
        public @NonNull T setDaysOfWeek(
                @IntRange(from = Calendar.SUNDAY, to = Calendar.SATURDAY)
                int @Nullable ... daysOfWeek) {
            if (daysOfWeek != null) {
                for (int day : daysOfWeek) {
                    Preconditions.checkArgumentInRange(day, Calendar.SUNDAY, Calendar.SATURDAY,
                            "daysOfWeek");
                }
            }
            mDaysOfWeek = daysOfWeek;
            return (T) this;
        }

        /**
         * Sets the hour the {@link Alarm} will fire.
         *
         * <p>Hours are specified by integers from 0 to 23.
         */
        public @NonNull T setHour(@IntRange(from = 0, to = 23) int hour) {
            mHour = Preconditions.checkArgumentInRange(hour, 0, 23, "hour");
            return (T) this;
        }

        /**
         * Sets the minute the {@link Alarm} will fire.
         *
         * <p>Minutes are specified by integers from 0 to 59.
         */
        public @NonNull T setMinute(@IntRange(from = 0, to = 59) int minute) {
            mMinute = Preconditions.checkArgumentInRange(minute, 0, 59, "minute");
            return (T) this;
        }

        /**
         * Sets the start date for the blackout period in ISO 8601 format.
         * E.g.: 2022-01-14
         *
         * <p>A blackout period means the {@link Alarm} will not fire during this period.
         *
         * <p>If not set, then it indicates that the blackout period has no start time.
         *
         * <p>If neither blackoutPeriodStartDate and blackoutPeriodEndDate are set, then
         * the blackout period is not defined.
         */
        public @NonNull T setBlackoutPeriodStartDate(
                @Nullable String blackoutPeriodStartDate) {
            if (blackoutPeriodStartDate != null) {
                Preconditions.checkArgument(
                        DateTimeFormatValidator.validateISO8601Date(blackoutPeriodStartDate),
                        "blackoutPeriodStartDate must be in the format: yyyy-MM-dd");
            }
            mBlackoutPeriodStartDate = blackoutPeriodStartDate;
            return (T) this;
        }

        /**
         * Sets the end time for the blackout period in ISO 8601 format.
         * E.g.: 2022-01-14
         *
         * <p>A blackout period means the {@link Alarm} will not fire during this period.
         *
         * <p>If not set, then it indicates that the blackout period has no end time.
         *
         * <p>If neither blackoutPeriodStartDate and blackoutPeriodEndDate are set, then
         * the blackout period is not defined.
         */
        public @NonNull T setBlackoutPeriodEndDate(@Nullable String blackoutPeriodEndDate) {
            if (blackoutPeriodEndDate != null) {
                Preconditions.checkArgument(
                        DateTimeFormatValidator.validateISO8601Date(blackoutPeriodEndDate),
                        "blackoutPeriodEndDate must be in the format: yyyy-MM-dd");
            }
            mBlackoutPeriodEndDate = blackoutPeriodEndDate;
            return (T) this;
        }

        /**
         * Sets the content URI for the ringtone to be played, or
         * {@link android.provider.AlarmClock#VALUE_RINGTONE_SILENT} if no ringtone will be played.
         */
        public @NonNull T setRingtone(@Nullable String ringtone) {
            mRingtone = ringtone;
            return (T) this;
        }

        /** Sets whether or not to activate the device vibrator when the {@link Alarm} fires. */
        public @NonNull T setShouldVibrate(boolean shouldVibrate) {
            mShouldVibrate = shouldVibrate;
            return (T) this;
        }

        /**
         * Sets the previous {@link AlarmInstance}.
         *
         * <p>The previous {@link AlarmInstance} is most recent past instance that was fired. If
         * not set, then there are no past instances.
         *
         * <p>See {@link AlarmInstance}.
         */
        public @NonNull T setPreviousInstance(@Nullable AlarmInstance previousInstance) {
            mPreviousInstance = previousInstance;
            return (T) this;
        }

        /**
         * Sets the next {@link AlarmInstance}.
         *
         * <p>The next {@link AlarmInstance} is the immediate future instance that is scheduled
         * to fire. If not set, then there are no future instances.
         *
         * <p>See {@link AlarmInstance}.
         */
        public @NonNull T setNextInstance(@Nullable AlarmInstance nextInstance) {
            mNextInstance = nextInstance;
            return (T) this;
        }

        /** Sets the {@link OriginatingDevice} this alarm belongs to. */
        public @NonNull T setOriginatingDevice(@OriginatingDevice int originatingDevice) {
            mOriginatingDevice = originatingDevice;
            return (T) this;
        }

        /** Builds the {@link Alarm}. */
        @Override
        public @NonNull Alarm build() {
            return new Alarm(mNamespace, mId, mDocumentScore, mCreationTimestampMillis,
                    mDocumentTtlMillis, mName, mAlternateNames, mDescription, mImage, mUrl,
                    mPotentialActions,
                    mEnabled, mDaysOfWeek, mHour, mMinute, mBlackoutPeriodStartDate,
                    mBlackoutPeriodEndDate, mRingtone, mShouldVibrate, mPreviousInstance,
                    mNextInstance, mOriginatingDevice);
        }
    }
}
