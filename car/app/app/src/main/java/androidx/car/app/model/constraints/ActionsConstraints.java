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

package androidx.car.app.model.constraints;


import static androidx.annotation.RestrictTo.Scope;
import static androidx.car.app.model.Action.FLAG_PRIMARY;

import static java.util.Objects.requireNonNull;

import android.annotation.SuppressLint;

import androidx.annotation.RestrictTo;
import androidx.annotation.VisibleForTesting;
import androidx.car.app.annotations.RequiresCarApi;
import androidx.car.app.model.Action;
import androidx.car.app.model.Action.ActionType;
import androidx.car.app.model.CarColor;
import androidx.car.app.model.CarText;

import org.jspecify.annotations.NonNull;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Encapsulates the constraints to apply when rendering a list of {@link Action}s on a template.
 */
@RestrictTo(Scope.LIBRARY)
public final class ActionsConstraints {

    /**
     * Constraints for template headers, where only one of a custom non-standard action with an
     * icon, the special-purpose back or app-icon standard action is allowed.
     */
    public static final @NonNull ActionsConstraints ACTIONS_CONSTRAINTS_HEADER =
            new ActionsConstraints.Builder()
                    .setMaxActions(1)
                    .setRequireActionIcons(true)
                    .setOnClickListenerAllowed(false)
                    .build();
    /**
     * Constraints for template headers, where any custom action with an icon is allowed in
     * addition to the special-purpose back and app-icon standard actions .
     */
    public static final @NonNull ActionsConstraints ACTIONS_CONSTRAINTS_MULTI_HEADER =
            new ActionsConstraints.Builder()
                    .setMaxActions(2)
                    .setRequireActionIcons(true)
                    .setOnClickListenerAllowed(true)
                    .build();
    /** Conservative constraints for most template types. */
    private static final @NonNull ActionsConstraints ACTIONS_CONSTRAINTS_CONSERVATIVE =
            new ActionsConstraints.Builder()
                    .setTitleTextConstraints(CarTextConstraints.CONSERVATIVE)
                    .setMaxActions(2)
                    .build();
    /**
     * Constraints for actions within the template body.
     */
    public static final @NonNull ActionsConstraints ACTIONS_CONSTRAINTS_BODY =
            new ActionsConstraints.Builder(ACTIONS_CONSTRAINTS_CONSERVATIVE)
                    .setTitleTextConstraints(CarTextConstraints.COLOR_ONLY)
                    .setMaxCustomTitles(2)
                    .setOnClickListenerAllowed(true)
                    .build();
    /**
     * Constraints for actions within the template body. The one of the action in this body can be
     * primary action.
     */
    public static final @NonNull ActionsConstraints ACTIONS_CONSTRAINTS_BODY_WITH_PRIMARY_ACTION =
            new ActionsConstraints.Builder(ACTIONS_CONSTRAINTS_CONSERVATIVE)
                    .setTitleTextConstraints(CarTextConstraints.COLOR_ONLY)
                    .setMaxCustomTitles(2)
                    .setMaxPrimaryActions(1)
                    .setOnClickListenerAllowed(true)
                    .build();
    /**
     * Default constraints that should be applied to most templates (2 actions, 1 can have
     * title)'s {@link androidx.car.app.model.ActionStrip}.
     */
    public static final @NonNull ActionsConstraints ACTIONS_CONSTRAINTS_SIMPLE =
            new ActionsConstraints.Builder(ACTIONS_CONSTRAINTS_CONSERVATIVE)
                    .setMaxCustomTitles(1)
                    .setTitleTextConstraints(CarTextConstraints.TEXT_ONLY)
                    .setOnClickListenerAllowed(true)
                    .setRestrictBackgroundColorToPrimaryAction(true)
                    .build();

    /** Constraints for map based templates. */
    public static final @NonNull ActionsConstraints ACTIONS_CONSTRAINTS_NAVIGATION =
            new ActionsConstraints.Builder(ACTIONS_CONSTRAINTS_CONSERVATIVE)
                    .setMaxActions(4)
                    .setMaxCustomTitles(4)
                    .setMaxPrimaryActions(1)
                    .setTitleTextConstraints(CarTextConstraints.TEXT_AND_ICON)
                    .setOnClickListenerAllowed(true)
                    .setRestrictBackgroundColorToPrimaryAction(true)
                    .build();

    /**
     * Constraints for map action buttons.
     *
     * <p>Only buttons with icons are allowed.
     */
    public static final @NonNull ActionsConstraints ACTIONS_CONSTRAINTS_MAP =
            new ActionsConstraints.Builder(ACTIONS_CONSTRAINTS_CONSERVATIVE)
                    .setMaxActions(4)
                    .setMaxPrimaryActions(1)
                    .setOnClickListenerAllowed(true)
                    .setRestrictBackgroundColorToPrimaryAction(true)
                    .build();

    /**
     * Constraints for additional row actions. Only allows custom actions.
     * Note: From Car API 8 onwards, Rows are allowed to have 2 max actions to be set.
     */
    @SuppressWarnings("UnsafeOptInUsageError")
    public static final @NonNull ActionsConstraints ACTIONS_CONSTRAINTS_ROW =
            new ActionsConstraints.Builder()
                    .setMaxActions(2)
                    .setMaxCustomTitles(2)
                    .setMaxPrimaryActions(1)
                    .addAllowedActionType(Action.TYPE_CUSTOM)
                    .addAllowedActionType(Action.TYPE_MEDIA_PLAYBACK)
                    .setOnClickListenerAllowed(true)
                    .build();

    /**
     * Constraints for additional ConversationItem actions. Only allows custom actions.
     */
    public static final @NonNull ActionsConstraints ACTIONS_CONSTRAINTS_CONVERSATION_ITEM =
            new ActionsConstraints.Builder()
                    .setMaxActions(1)
                    .setMaxCustomTitles(1)
                    .addAllowedActionType(Action.TYPE_CUSTOM)
                    .setRequireActionIcons(true)
                    .setOnClickListenerAllowed(true)
                    .build();

    /**
     * Constraints for floating action buttons which enforces the following on the list of actions:
     *
     * <ul>
     *     <li>Maximum of {@code 2}
     *     <li>Must be of type {@link Action#TYPE_CUSTOM} or {@link Action#TYPE_COMPOSE_MESSAGE} or
     *     {@link Action#TYPE_MEDIA_PLAYBACK}
     *     <li>Must have an icon
     *     <li>Must have a background color, though the host may choose to ignore this color
     *     depending on where it appears (eg. usually only the first action is rendered with a
     *     custom color and the rest are shown with a neutral color)
     *     <li>Can have a click listener
     * </ul>
     */
    @SuppressLint("UnsafeOptInUsageError")
    public static final @NonNull ActionsConstraints ACTIONS_CONSTRAINTS_FAB =
            new ActionsConstraints.Builder()
                    .setMaxActions(2)
                    .addAllowedActionType(Action.TYPE_CUSTOM)
                    .addAllowedActionType(Action.TYPE_COMPOSE_MESSAGE)
                    .addAllowedActionType(Action.TYPE_MEDIA_PLAYBACK)
                    .setRequireActionIcons(true)
                    .setRequireActionBackgroundColor(true)
                    .setOnClickListenerAllowed(true)
                    .build();

    /** Constraints for TabTemplate. */
    @RequiresCarApi(6)
    public static final @NonNull ActionsConstraints ACTIONS_CONSTRAINTS_TABS =
            new ActionsConstraints.Builder(ACTIONS_CONSTRAINTS_HEADER)
                    .addRequiredActionType(Action.TYPE_APP_ICON)
                    .build();

    private final int mMaxActions;
    private final int mMaxPrimaryActions;
    private final int mMaxCustomTitles;
    private final boolean mRequireActionIcons;
    private final boolean mRequireActionBackgroundColor;
    private final boolean mOnClickListenerAllowed;
    private final boolean mRestrictBackgroundColorToPrimaryAction;
    private final CarTextConstraints mTitleTextConstraints;
    private final Set<Integer> mRequiredActionTypes;
    private final Set<Integer> mDisallowedActionTypes;
    private final Set<Integer> mAllowedActionTypes;

    ActionsConstraints(Builder builder) {
        mMaxActions = builder.mMaxActions;
        mMaxPrimaryActions = builder.mMaxPrimaryActions;
        mMaxCustomTitles = builder.mMaxCustomTitles;
        mTitleTextConstraints = builder.mTitleTextConstraints;
        mRequireActionIcons = builder.mRequireActionIcons;
        mRequireActionBackgroundColor = builder.mRequireActionBackgroundColor;
        mOnClickListenerAllowed = builder.mOnClickListenerAllowed;
        mRestrictBackgroundColorToPrimaryAction = builder.mRestrictBackgroundColorToPrimaryAction;
        mRequiredActionTypes = new HashSet<>(builder.mRequiredActionTypes);
        mAllowedActionTypes = new HashSet<>(builder.mAllowedActionTypes);

        Set<Integer> disallowedActionTypes = new HashSet<>(builder.mDisallowedActionTypes);
        disallowedActionTypes.retainAll(mRequiredActionTypes);
        if (!disallowedActionTypes.isEmpty()) {
            throw new IllegalArgumentException(
                    "Disallowed action types cannot also be in the required set");
        }

        if (!builder.mDisallowedActionTypes.isEmpty() && !mAllowedActionTypes.isEmpty()) {
            throw new IllegalArgumentException(
                    "Both disallowed and allowed action type set cannot be defined.");
        }

        mDisallowedActionTypes = new HashSet<>(builder.mDisallowedActionTypes);

        if (mRequiredActionTypes.size() > mMaxActions) {
            throw new IllegalArgumentException(
                    "Required action types exceeded max allowed actions");
        }
    }

    /** Returns the max number of actions allowed. */
    public int getMaxActions() {
        return mMaxActions;
    }

    /** Returns the max number of primary actions allowed. */
    public int getMaxPrimaryActions() {
        return mMaxPrimaryActions;
    }

    /** Returns the max number of actions with custom titles allowed. */
    public int getMaxCustomTitles() {
        return mMaxCustomTitles;
    }

    /** Returns the {@link CarTextConstraints} fpr the title. */
    public @NonNull CarTextConstraints getTitleTextConstraints() {
        return mTitleTextConstraints;
    }

    /** Adds the set of required action types. */
    public @NonNull Set<Integer> getRequiredActionTypes() {
        return mRequiredActionTypes;
    }

    /** Adds the set of disallowed action types. */
    public @NonNull Set<Integer> getDisallowedActionTypes() {
        return mDisallowedActionTypes;
    }

    /** Adds the set of allowed action types. */
    public @NonNull Set<Integer> getAllowedActionTypes() {
        return mAllowedActionTypes;
    }

    /**
     * If {@code true}, all non-standard actions must have an
     * {@link androidx.car.app.model.CarIcon}.
     */
    public boolean areActionIconsRequired() {
        return mRequireActionIcons;
    }

    /**
     * If {@code true}, all non-standard actions must have a background
     * {@link androidx.car.app.model.CarColor}.
     */
    public boolean isActionBackgroundColorRequired() {
        return mRequireActionBackgroundColor;
    }

    /** If {@code true}, actions can enable an {@link androidx.car.app.model.OnClickDelegate}. */
    public boolean isOnClickListenerAllowed() {
        return mOnClickListenerAllowed;
    }

    /** If {@code true}, background color can only be set for {@code FLAG_PRIMARY} actions. */
    public boolean restrictBackgroundColorToPrimaryAction() {
        return mRestrictBackgroundColorToPrimaryAction;
    }

    /**
     * Validates the input list of {@link Action}s against this {@link ActionsConstraints} instance.
     *
     * @throws IllegalArgumentException if the actions has more actions than allowed, if it has
     *                                  more actions with custom titles than allowed, if the
     *                                  actions do not contain all required types, if the
     *                                  actions contain any disallowed types, or if the actions
     *                                  contain any types not in the allowed set.
     */
    public void validateOrThrow(@NonNull List<Action> actions) {
        int maxAllowedActions = mMaxActions;
        int maxAllowedPrimaryActions = mMaxPrimaryActions;
        int maxAllowedCustomTitles = mMaxCustomTitles;

        Set<Integer> requiredTypes =
                mRequiredActionTypes.isEmpty()
                        ? Collections.emptySet()
                        : new HashSet<>(mRequiredActionTypes);

        for (Action action : actions) {
            if (!mDisallowedActionTypes.isEmpty()
                    && mDisallowedActionTypes.contains(action.getType())) {
                throw new IllegalArgumentException(
                        Action.typeToString(action.getType()) + " is disallowed");
            }

            if (!mAllowedActionTypes.isEmpty()
                    && !mAllowedActionTypes.contains(action.getType())) {
                throw new IllegalArgumentException(
                        Action.typeToString(action.getType()) + " is not allowed");
            }

            requiredTypes.remove(action.getType());

            CarText title = action.getTitle();
            if (title != null && !title.isEmpty()) {
                if (--maxAllowedCustomTitles < 0) {
                    throw new IllegalArgumentException(
                            "Action list exceeded max number of "
                                    + mMaxCustomTitles
                                    + " actions with custom titles");
                }

                mTitleTextConstraints.validateOrThrow(title);
            }

            if (--maxAllowedActions < 0) {
                throw new IllegalArgumentException(
                        "Action list exceeded max number of " + mMaxActions + " actions");
            }

            if ((action.getFlags() & FLAG_PRIMARY) != 0) {
                if (--maxAllowedPrimaryActions < 0) {
                    throw new IllegalArgumentException(
                            "Action list exceeded max number of "
                                    + mMaxPrimaryActions
                                    + " primary actions");
                }
            }

            if (mRequireActionIcons && action.getIcon() == null && !action.isStandard()) {
                throw new IllegalArgumentException("Non-standard actions without an icon are "
                        + "disallowed");
            }

            if (mRequireActionBackgroundColor
                    && (action.getBackgroundColor() == null
                    || CarColor.DEFAULT.equals(action.getBackgroundColor()))
                    && !action.isStandard()) {
                throw new IllegalArgumentException("Non-standard actions without a background "
                        + "color are disallowed");
            }

            // Skip this check if the action requires a custom background color. Else ensure that
            // the background color is being applied to a primary action.
            if (!mRequireActionBackgroundColor
                    && !CarColor.DEFAULT.equals(action.getBackgroundColor())
                    && mRestrictBackgroundColorToPrimaryAction
                    && ((action.getFlags() & FLAG_PRIMARY) == 0)) {
                throw new IllegalArgumentException("Background color can only be set for primary "
                        + "actions");
            }

            if (!mOnClickListenerAllowed
                    && action.getOnClickDelegate() != null
                    && !action.isStandard()) {
                throw new IllegalArgumentException("Setting a click listener for a custom action "
                        + "is disallowed");
            }
        }

        if (!requiredTypes.isEmpty()) {
            StringBuilder missingTypeError = new StringBuilder();
            for (@ActionType int type : requiredTypes) {
                missingTypeError.append(Action.typeToString(type)).append(",");
            }
            throw new IllegalArgumentException(
                    "Missing required action types: " + missingTypeError);
        }
    }

    /**
     * A builder of {@link ActionsConstraints}.
     */
    @VisibleForTesting
    public static final class Builder {
        final Set<Integer> mRequiredActionTypes = new HashSet<>();
        final Set<Integer> mDisallowedActionTypes = new HashSet<>();
        final Set<Integer> mAllowedActionTypes = new HashSet<>();
        int mMaxActions = Integer.MAX_VALUE;
        int mMaxPrimaryActions = 0;
        int mMaxCustomTitles;
        boolean mRequireActionIcons;
        boolean mRequireActionBackgroundColor;
        boolean mOnClickListenerAllowed;
        boolean mRestrictBackgroundColorToPrimaryAction = false;
        CarTextConstraints mTitleTextConstraints = CarTextConstraints.UNCONSTRAINED;

        /** Returns an empty {@link Builder} instance. */
        public Builder() {
        }

        /**
         * Returns a new builder that contains the same data as the given {@link ActionsConstraints}
         * instance.
         *
         * @throws NullPointerException if {@code latLng} is {@code null}
         */
        public Builder(@NonNull ActionsConstraints constraints) {
            requireNonNull(constraints);
            mMaxActions = constraints.getMaxActions();
            mMaxPrimaryActions = constraints.getMaxPrimaryActions();
            mMaxCustomTitles = constraints.getMaxCustomTitles();
            mTitleTextConstraints = constraints.getTitleTextConstraints();
            mRequiredActionTypes.addAll(constraints.getRequiredActionTypes());
            mDisallowedActionTypes.addAll(constraints.getDisallowedActionTypes());
            mAllowedActionTypes.addAll(constraints.getAllowedActionTypes());
            mRequireActionIcons = constraints.areActionIconsRequired();
            mRequireActionBackgroundColor = constraints.isActionBackgroundColorRequired();
            mOnClickListenerAllowed = constraints.isOnClickListenerAllowed();
            mRestrictBackgroundColorToPrimaryAction =
                    constraints.restrictBackgroundColorToPrimaryAction();
        }

        /** Sets the maximum number of actions allowed. */
        public @NonNull Builder setMaxActions(int maxActions) {
            mMaxActions = maxActions;
            return this;
        }

        /**
         * Set {@code true} if all non-standard actions must have an
         * {@link androidx.car.app.model.CarIcon}.
         */
        public @NonNull Builder setRequireActionIcons(boolean requireActionIcons) {
            mRequireActionIcons = requireActionIcons;
            return this;
        }

        /**
         * Set {@code true} if all non-standard actions must have a background
         * {@link androidx.car.app.model.CarColor}.
         */
        public @NonNull Builder setRequireActionBackgroundColor(
                boolean requireActionBackgroundColor) {
            mRequireActionBackgroundColor = requireActionBackgroundColor;
            return this;
        }

        /**
         * Set {@code true} if all actions can have an
         * {@link androidx.car.app.model.OnClickDelegate}.
         */
        public @NonNull Builder setOnClickListenerAllowed(boolean onClickListenerAllowed) {
            mOnClickListenerAllowed = onClickListenerAllowed;
            return this;
        }

        /**
         * Set {@code true} if background color can only be set for {@code FLAG_PRIMARY} actions.
         */
        public @NonNull Builder setRestrictBackgroundColorToPrimaryAction(
                boolean restrictBackgroundColorToPrimaryAction) {
            this.mRestrictBackgroundColorToPrimaryAction = restrictBackgroundColorToPrimaryAction;
            return this;
        }

        /** Sets the maximum number of primary actions allowed. */
        public @NonNull Builder setMaxPrimaryActions(int maxPrimaryActions) {
            mMaxPrimaryActions = maxPrimaryActions;
            return this;
        }

        /** Sets the maximum number of actions with custom titles allowed. */
        public @NonNull Builder setMaxCustomTitles(int maxCustomTitles) {
            mMaxCustomTitles = maxCustomTitles;
            return this;
        }

        /** Sets the {@link CarTextConstraints} for the title. */
        public @NonNull Builder setTitleTextConstraints(
                @NonNull CarTextConstraints carTextConstraints) {
            mTitleTextConstraints = carTextConstraints;
            return this;
        }

        /** Adds an action type to the set of required types. */
        public @NonNull Builder addRequiredActionType(@ActionType int actionType) {
            mRequiredActionTypes.add(actionType);
            return this;
        }

        /** Adds an action type to the set of disallowed types. */
        public @NonNull Builder addDisallowedActionType(@ActionType int actionType) {
            mDisallowedActionTypes.add(actionType);
            return this;
        }

        /** Adds an action type to the set of allowed types. */
        public @NonNull Builder addAllowedActionType(@ActionType int actionType) {
            mAllowedActionTypes.add(actionType);
            return this;
        }

        /**
         * Returns an {@link ActionsConstraints} instance defined by this builder.
         */
        public @NonNull ActionsConstraints build() {
            return new ActionsConstraints(this);
        }
    }
}
