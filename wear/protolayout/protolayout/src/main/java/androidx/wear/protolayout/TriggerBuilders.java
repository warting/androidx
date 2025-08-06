/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.wear.protolayout;

import static androidx.wear.protolayout.expression.Preconditions.checkNotNull;

import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import androidx.wear.protolayout.expression.DynamicBuilders;
import androidx.wear.protolayout.expression.DynamicBuilders.DynamicBool;
import androidx.wear.protolayout.expression.Fingerprint;
import androidx.wear.protolayout.expression.ProtoLayoutExperimental;
import androidx.wear.protolayout.expression.RequiresSchemaVersion;
import androidx.wear.protolayout.proto.TriggerProto;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;
import java.util.Objects;

/** Builders for triggers that can be used to start an animation. */
public final class TriggerBuilders {
    private TriggerBuilders() {}

    /** Creates a {@link Trigger} that fires immediately when the layout is loaded / reloaded. */
    @RequiresSchemaVersion(major = 1, minor = 200)
    public static @NonNull Trigger createOnLoadTrigger() {
        return new OnLoadTrigger.Builder().build();
    }

    /**
     * Creates a {@link Trigger} that fires *every time* the condition switches from false to true.
     * If the condition is true initially, that will fire the trigger on load.
     */
    @RequiresSchemaVersion(major = 1, minor = 200)
    public static @NonNull Trigger createOnConditionMetTrigger(@NonNull DynamicBool dynamicBool) {
        return new OnConditionMetTrigger.Builder().setCondition(dynamicBool).build();
    }

    /**
     * Creates a {@link Trigger} that fires *every time* the layout becomes visible.
     *
     * <p>As opposed to {@link #createOnLoadTrigger()}, this will wait until layout is fully visible
     * before firing a trigger.
     */
    @RequiresSchemaVersion(major = 1, minor = 200)
    public static @NonNull Trigger createOnVisibleTrigger() {
        return new OnVisibleTrigger.Builder().build();
    }

    /**
     * Creates a {@link Trigger} that fires the first time that layout becomes visible.
     *
     * <p>As opposed to {@link #createOnVisibleTrigger()}, this will only be fired the first time
     * that the layout becomes visible.
     */
    @RequiresSchemaVersion(major = 1, minor = 200)
    @ProtoLayoutExperimental
    public static @NonNull Trigger createOnVisibleOnceTrigger() {
        return new OnVisibleOnceTrigger.Builder().build();
    }

    /** Triggers when the layout visibility state turns from invisible to fully visible. */
    @RequiresSchemaVersion(major = 1, minor = 200)
    public static final class OnVisibleTrigger implements Trigger {
        private final TriggerProto.OnVisibleTrigger mImpl;
        private final @Nullable Fingerprint mFingerprint;

        OnVisibleTrigger(TriggerProto.OnVisibleTrigger impl, @Nullable Fingerprint fingerprint) {
            this.mImpl = impl;
            this.mFingerprint = fingerprint;
        }

        @Override
        public int hashCode() {
            return 1;
        }

        @Override
        public boolean equals(@Nullable Object obj) {
            if (this == obj) {
                return true;
            }
            // Visible trigger doesn't have modifications
            return obj instanceof OnVisibleTrigger;
        }

        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        public @Nullable Fingerprint getFingerprint() {
            return mFingerprint;
        }

        /** Creates a new wrapper instance from the proto. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        public static @NonNull OnVisibleTrigger fromProto(
                TriggerProto.@NonNull OnVisibleTrigger proto, @Nullable Fingerprint fingerprint) {
            return new OnVisibleTrigger(proto, fingerprint);
        }

        static @NonNull OnVisibleTrigger fromProto(TriggerProto.@NonNull OnVisibleTrigger proto) {
            return fromProto(proto, null);
        }

        /** Returns the internal proto instance. */
        TriggerProto.@NonNull OnVisibleTrigger toProto() {
            return mImpl;
        }

        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        public TriggerProto.@NonNull Trigger toTriggerProto() {
            return TriggerProto.Trigger.newBuilder().setOnVisibleTrigger(mImpl).build();
        }

        @Override
        public @NonNull String toString() {
            return "OnVisibleTrigger";
        }

        /** Builder for {@link OnVisibleTrigger}. */
        @SuppressWarnings("HiddenSuperclass")
        public static final class Builder implements Trigger.Builder {
            private final TriggerProto.OnVisibleTrigger.Builder mImpl =
                    TriggerProto.OnVisibleTrigger.newBuilder();
            private final Fingerprint mFingerprint = new Fingerprint(1416366796);

            /** Creates an instance of {@link Builder}. */
            @RequiresSchemaVersion(major = 1, minor = 200)
            public Builder() {}

            /** Builds an instance from accumulated values. */
            @Override
            public @NonNull OnVisibleTrigger build() {
                return new OnVisibleTrigger(mImpl.build(), mFingerprint);
            }
        }
    }

    /**
     * Triggers only once when the layout visibility state turns from invisible to fully visible for
     * the first time.
     */
    @RequiresSchemaVersion(major = 1, minor = 200)
    @ProtoLayoutExperimental
    public static final class OnVisibleOnceTrigger implements Trigger {
        private final TriggerProto.OnVisibleOnceTrigger mImpl;
        private final @Nullable Fingerprint mFingerprint;

        OnVisibleOnceTrigger(
                TriggerProto.OnVisibleOnceTrigger impl, @Nullable Fingerprint fingerprint) {
            this.mImpl = impl;
            this.mFingerprint = fingerprint;
        }

        @Override
        public int hashCode() {
            return 2;
        }

        @Override
        public boolean equals(@Nullable Object obj) {
            if (this == obj) {
                return true;
            }
            // VisibleOnce trigger doesn't have modifications
            return obj instanceof OnVisibleOnceTrigger;
        }

        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        public @Nullable Fingerprint getFingerprint() {
            return mFingerprint;
        }

        /** Creates a new wrapper instance from the proto. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        public static @NonNull OnVisibleOnceTrigger fromProto(
                TriggerProto.@NonNull OnVisibleOnceTrigger proto,
                @Nullable Fingerprint fingerprint) {
            return new OnVisibleOnceTrigger(proto, fingerprint);
        }

        static @NonNull OnVisibleOnceTrigger fromProto(
                TriggerProto.@NonNull OnVisibleOnceTrigger proto) {
            return fromProto(proto, null);
        }

        /** Returns the internal proto instance. */
        TriggerProto.@NonNull OnVisibleOnceTrigger toProto() {
            return mImpl;
        }

        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        public TriggerProto.@NonNull Trigger toTriggerProto() {
            return TriggerProto.Trigger.newBuilder().setOnVisibleOnceTrigger(mImpl).build();
        }

        @Override
        public @NonNull String toString() {
            return "OnVisibleOnceTrigger";
        }

        /** Builder for {@link OnVisibleOnceTrigger}. */
        @SuppressWarnings("HiddenSuperclass")
        public static final class Builder implements Trigger.Builder {
            private final TriggerProto.OnVisibleOnceTrigger.Builder mImpl =
                    TriggerProto.OnVisibleOnceTrigger.newBuilder();
            private final Fingerprint mFingerprint = new Fingerprint(-1661457257);

            /** Creates an instance of {@link Builder}. */
            @RequiresSchemaVersion(major = 1, minor = 200)
            public Builder() {}

            /** Builds an instance from accumulated values. */
            @Override
            public @NonNull OnVisibleOnceTrigger build() {
                return new OnVisibleOnceTrigger(mImpl.build(), mFingerprint);
            }
        }
    }

    /** Triggers immediately when the layout is loaded / reloaded. */
    @RequiresSchemaVersion(major = 1, minor = 200)
    static final class OnLoadTrigger implements Trigger {
        private final TriggerProto.OnLoadTrigger mImpl;
        private final @Nullable Fingerprint mFingerprint;

        OnLoadTrigger(TriggerProto.OnLoadTrigger impl, @Nullable Fingerprint fingerprint) {
            this.mImpl = impl;
            this.mFingerprint = fingerprint;
        }

        @Override
        public int hashCode() {
            return 3;
        }

        @Override
        public boolean equals(@Nullable Object obj) {
            if (this == obj) {
                return true;
            }
            // Visible trigger doesn't have modifications
            return obj instanceof OnLoadTrigger;
        }

        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        public @Nullable Fingerprint getFingerprint() {
            return mFingerprint;
        }

        /** Creates a new wrapper instance from the proto. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        public static @NonNull OnLoadTrigger fromProto(
                TriggerProto.@NonNull OnLoadTrigger proto, @Nullable Fingerprint fingerprint) {
            return new OnLoadTrigger(proto, fingerprint);
        }

        static @NonNull OnLoadTrigger fromProto(TriggerProto.@NonNull OnLoadTrigger proto) {
            return fromProto(proto, null);
        }

        /** Returns the internal proto instance. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        TriggerProto.@NonNull OnLoadTrigger toProto() {
            return mImpl;
        }

        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        public TriggerProto.@NonNull Trigger toTriggerProto() {
            return TriggerProto.Trigger.newBuilder().setOnLoadTrigger(mImpl).build();
        }

        @Override
        public @NonNull String toString() {
            return "OnLoadTrigger";
        }

        /** Builder for {@link OnLoadTrigger}. */
        public static final class Builder implements Trigger.Builder {
            private final TriggerProto.OnLoadTrigger.Builder mImpl =
                    TriggerProto.OnLoadTrigger.newBuilder();
            private final Fingerprint mFingerprint = new Fingerprint(-1262805599);

            /** Creates an instance of {@link Builder}. */
            @RequiresSchemaVersion(major = 1, minor = 200)
            public Builder() {}

            /** Builds an instance from accumulated values. */
            @Override
            public @NonNull OnLoadTrigger build() {
                return new OnLoadTrigger(mImpl.build(), mFingerprint);
            }
        }
    }

    /**
     * Triggers *every time* the condition switches from false to true. If the condition is true
     * initially, that will fire the trigger on load.
     */
    @RequiresSchemaVersion(major = 1, minor = 200)
    static final class OnConditionMetTrigger implements Trigger {
        private final TriggerProto.OnConditionMetTrigger mImpl;
        private final @Nullable Fingerprint mFingerprint;

        OnConditionMetTrigger(
                TriggerProto.OnConditionMetTrigger impl, @Nullable Fingerprint fingerprint) {
            this.mImpl = impl;
            this.mFingerprint = fingerprint;
        }

        /** Gets dynamic boolean used as trigger. */
        public @Nullable DynamicBool getCondition() {
            if (mImpl.hasCondition()) {
                return DynamicBuilders.dynamicBoolFromProto(mImpl.getCondition());
            } else {
                return null;
            }
        }

        @SuppressWarnings("NullAway") // getCondition()
        @Override
        public int hashCode() {
            DynamicBool condition = getCondition();
            return condition == null ? 4 : Arrays.hashCode(getCondition().toDynamicBoolByteArray());
        }

        @Override
        public boolean equals(@Nullable Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof OnConditionMetTrigger)) {
                return false;
            }
            OnConditionMetTrigger that = (OnConditionMetTrigger) obj;
            DynamicBool condition = getCondition();
            DynamicBool thatCondition = that.getCondition();
            return (condition == thatCondition)
                    || (condition != null
                            && thatCondition != null
                            && Arrays.equals(
                                    condition.toDynamicBoolByteArray(),
                                    thatCondition.toDynamicBoolByteArray()));
        }

        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        public @Nullable Fingerprint getFingerprint() {
            return mFingerprint;
        }

        /** Creates a new wrapper instance from the proto. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        public static @NonNull OnConditionMetTrigger fromProto(
                TriggerProto.@NonNull OnConditionMetTrigger proto,
                @Nullable Fingerprint fingerprint) {
            return new OnConditionMetTrigger(proto, fingerprint);
        }

        static @NonNull OnConditionMetTrigger fromProto(
                TriggerProto.@NonNull OnConditionMetTrigger proto) {
            return fromProto(proto, null);
        }

        /** Returns the internal proto instance. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        TriggerProto.@NonNull OnConditionMetTrigger toProto() {
            return mImpl;
        }

        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        public TriggerProto.@NonNull Trigger toTriggerProto() {
            return TriggerProto.Trigger.newBuilder().setOnConditionMetTrigger(mImpl).build();
        }

        @Override
        public @NonNull String toString() {
            return "OnConditionMetTrigger{" + "condition=" + getCondition() + "}";
        }

        /** Builder for {@link OnConditionMetTrigger}. */
        public static final class Builder implements Trigger.Builder {
            private final TriggerProto.OnConditionMetTrigger.Builder mImpl =
                    TriggerProto.OnConditionMetTrigger.newBuilder();
            private final Fingerprint mFingerprint = new Fingerprint(756642641);

            /** Creates an instance of {@link Builder}. */
            @RequiresSchemaVersion(major = 1, minor = 200)
            public Builder() {}

            /** Sets dynamic boolean used as trigger. */
            @RequiresSchemaVersion(major = 1, minor = 200)
            public @NonNull Builder setCondition(@NonNull DynamicBool condition) {
                mImpl.setCondition(condition.toDynamicBoolProto());
                mFingerprint.recordPropertyUpdate(
                        1, checkNotNull(condition.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /** Builds an instance from accumulated values. */
            @Override
            public @NonNull OnConditionMetTrigger build() {
                return new OnConditionMetTrigger(mImpl.build(), mFingerprint);
            }
        }
    }

    /**
     * Interface defining the triggers that can be fired. These triggers can be used to allow acting
     * on events. For example some animations can be set to start based on a trigger.
     */
    @RequiresSchemaVersion(major = 1, minor = 200)
    public interface Trigger {
        /**
         * Returns hash code for the given {@link Trigger}, taking into account hash of the subclass
         * and the inner position of this proto message.
         */
        @RestrictTo(Scope.LIBRARY)
        static int hash(@Nullable Trigger trigger) {
            // We need "Trigger" string so that if the object is implementing some other interface
            // that has a oneof and subclass is on the exact same inner case, we want these two to
            // be different.
            return trigger != null
                    ? Objects.hash(
                            "Trigger", trigger.toTriggerProto().getInnerCase().getNumber(), trigger)
                    : 0;
        }

        /**
         * Checks whether the given {@link Trigger} is equal to the object taking into account inner
         * position.
         */
        @SuppressWarnings("NullAway") // that.toTriggerProto()
        @RestrictTo(Scope.LIBRARY)
        static boolean equal(@Nullable Trigger trigger, @Nullable Trigger that) {
            if (trigger == that) {
                return true;
            }
            return that.toTriggerProto().getInnerCase().getNumber()
                            == trigger.toTriggerProto().getInnerCase().getNumber()
                    && Objects.equals(that, trigger);
        }

        /** Get the protocol buffer representation of this object. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        TriggerProto.@NonNull Trigger toTriggerProto();

        /** Get the fingerprint for this object or null if unknown. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @Nullable Fingerprint getFingerprint();

        /** Builder to create {@link Trigger} objects. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        interface Builder {

            /** Builds an instance with values accumulated in this Builder. */
            @NonNull Trigger build();
        }
    }

    /** Creates a new wrapper instance from the proto. */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @ProtoLayoutExperimental
    public static @NonNull Trigger triggerFromProto(
            TriggerProto.@NonNull Trigger proto, @Nullable Fingerprint fingerprint) {
        if (proto.hasOnVisibleTrigger()) {
            return OnVisibleTrigger.fromProto(proto.getOnVisibleTrigger(), fingerprint);
        }
        if (proto.hasOnVisibleOnceTrigger()) {
            return OnVisibleOnceTrigger.fromProto(proto.getOnVisibleOnceTrigger(), fingerprint);
        }
        if (proto.hasOnLoadTrigger()) {
            return OnLoadTrigger.fromProto(proto.getOnLoadTrigger(), fingerprint);
        }
        if (proto.hasOnConditionMetTrigger()) {
            return OnConditionMetTrigger.fromProto(proto.getOnConditionMetTrigger(), fingerprint);
        }
        throw new IllegalStateException("Proto was not a recognised instance of Trigger");
    }

    @ProtoLayoutExperimental
    static @NonNull Trigger triggerFromProto(TriggerProto.@NonNull Trigger proto) {
        return triggerFromProto(proto, null);
    }
}
