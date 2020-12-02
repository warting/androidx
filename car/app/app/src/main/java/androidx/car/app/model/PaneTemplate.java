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

package androidx.car.app.model;

import static androidx.annotation.RestrictTo.Scope.LIBRARY;
import static androidx.car.app.model.constraints.ActionsConstraints.ACTIONS_CONSTRAINTS_HEADER;
import static androidx.car.app.model.constraints.ActionsConstraints.ACTIONS_CONSTRAINTS_SIMPLE;
import static androidx.car.app.model.constraints.RowListConstraints.ROW_LIST_CONSTRAINTS_PANE;

import static java.util.Objects.requireNonNull;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;

import java.util.Collections;
import java.util.Objects;

/**
 * A template that displays a {@link Pane}.
 *
 * <h4>Template Restrictions</h4>
 *
 * In regards to template refreshes, as described in
 * {@link androidx.car.app.Screen#onGetTemplate()}, this template is considered a refresh of a
 * previous one if:
 *
 * <ul>
 *   <li>The template title has not changed, and
 *   <li>The previous template is in a loading state (see {@link Pane.Builder#setLoading}, or the
 *       number of rows and the string contents (title, texts, not counting spans) of each row
 *       between the previous and new {@link Pane}s have not changed.
 * </ul>
 */
public final class PaneTemplate implements Template {
    @Keep
    @Nullable
    private final CarText mTitle;
    @Keep
    @Nullable
    private final Pane mPane;
    @Keep
    @Nullable
    private final Action mHeaderAction;
    @Keep
    @Nullable
    private final ActionStrip mActionStrip;

    /**
     * Constructs a new builder of {@link PaneTemplate}.
     *
     * @throws NullPointerException if {@code pane} is {@code null}
     */
    @NonNull
    public static Builder builder(@NonNull Pane pane) {
        return new Builder(requireNonNull(pane));
    }

    @Nullable
    public CarText getTitle() {
        return mTitle;
    }

    @Nullable
    public Action getHeaderAction() {
        return mHeaderAction;
    }

    @NonNull
    public Pane getPane() {
        return requireNonNull(mPane);
    }

    @Nullable
    public ActionStrip getActionStrip() {
        return mActionStrip;
    }

    @NonNull
    @Override
    public String toString() {
        return "PaneTemplate";
    }

    @Override
    public int hashCode() {
        return Objects.hash(mTitle, mPane, mHeaderAction, mActionStrip);
    }

    @Override
    public boolean equals(@Nullable Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof PaneTemplate)) {
            return false;
        }
        PaneTemplate otherTemplate = (PaneTemplate) other;

        return Objects.equals(mTitle, otherTemplate.mTitle)
                && Objects.equals(mPane, otherTemplate.mPane)
                && Objects.equals(mHeaderAction, otherTemplate.mHeaderAction)
                && Objects.equals(mActionStrip, otherTemplate.mActionStrip);
    }

    private PaneTemplate(Builder builder) {
        mTitle = builder.mTitle;
        mPane = builder.mPane;
        mHeaderAction = builder.mHeaderAction;
        mActionStrip = builder.mActionStrip;
    }

    /** Constructs an empty instance, used by serialization code. */
    private PaneTemplate() {
        mTitle = null;
        mPane = null;
        mHeaderAction = null;
        mActionStrip = null;
    }

    /** A builder of {@link PaneTemplate}. */
    public static final class Builder {
        @Nullable
        private CarText mTitle;
        private Pane mPane;
        @Nullable
        private Action mHeaderAction;
        @Nullable
        private ActionStrip mActionStrip;

        private Builder(Pane pane) {
            this.mPane = pane;
        }

        /**
         * Sets the {@link CharSequence} to show as the template's title, or {@code null} to not
         * show a
         * title.
         */
        @NonNull
        public Builder setTitle(@Nullable CharSequence title) {
            this.mTitle = title == null ? null : CarText.create(title);
            return this;
        }

        /**
         * Sets the {@link Action} that will be displayed in the header of the template, or
         * {@code null}
         * to not display an action.
         *
         * <h4>Requirements</h4>
         *
         * This template only supports either either one of {@link Action#APP_ICON} and {@link
         * Action#BACK} as a header {@link Action}.
         *
         * @throws IllegalArgumentException if {@code headerAction} does not meet the template's
         *                                  requirements.
         */
        @NonNull
        public Builder setHeaderAction(@Nullable Action headerAction) {
            ACTIONS_CONSTRAINTS_HEADER.validateOrThrow(
                    headerAction == null ? Collections.emptyList()
                            : Collections.singletonList(headerAction));
            this.mHeaderAction = headerAction;
            return this;
        }

        /**
         * Sets the {@link Pane} to display in the template.
         *
         * @throws NullPointerException if {@code pane} is {@code null}.
         */
        @NonNull
        public Builder setPane(@NonNull Pane pane) {
            this.mPane = requireNonNull(pane);
            return this;
        }

        /**
         * Sets the {@link ActionStrip} for this template.
         *
         * <h4>Requirements</h4>
         *
         * This template allows up to 2 {@link Action}s in its {@link ActionStrip}. Of the 2 allowed
         * {@link Action}s, one of them can contain a title as set via
         * {@link Action.Builder#setTitle}.
         * Otherwise, only {@link Action}s with icons are allowed.
         *
         * @throws IllegalArgumentException if {@code actionStrip} does not meet the requirements.
         */
        @NonNull
        public Builder setActionStrip(@Nullable ActionStrip actionStrip) {
            ACTIONS_CONSTRAINTS_SIMPLE.validateOrThrow(
                    actionStrip == null ? Collections.emptyList() : actionStrip.getActions());
            this.mActionStrip = actionStrip;
            return this;
        }

        /**
         * Constructs the template defined by this builder.
         *
         * <h4>Requirements</h4>
         *
         * This template allows up to 2 {@link Row}s and 2 {@link Action}s in the {@link Pane}.
         * The host
         * will ignore any rows over that limit. Each {@link Row}s can add up to 2 lines of texts
         * via
         * {@link Row.Builder#addText} and cannot contain either a {@link Toggle} or a {@link
         * OnClickListener}.
         *
         * <p>Either a header {@link Action} or title must be set on the template.
         *
         * @throws IllegalArgumentException if the {@link Pane} does not meet the requirements.
         * @throws IllegalStateException    if the template does not have either a title or header
         *                                  {@link
         *                                  Action} set.
         */
        @NonNull
        public PaneTemplate build() {
            ROW_LIST_CONSTRAINTS_PANE.validateOrThrow(mPane);

            if (CarText.isNullOrEmpty(mTitle) && mHeaderAction == null) {
                throw new IllegalStateException("Either the title or header action must be set");
            }

            return new PaneTemplate(this);
        }

        /** @hide */
        @RestrictTo(LIBRARY)
        @NonNull
        public PaneTemplate buildForTesting() {
            return new PaneTemplate(this);
        }
    }
}
