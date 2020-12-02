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

import static java.util.Objects.requireNonNull;

import android.annotation.SuppressLint;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Represents a list of rows used for displaying informational content and a set of {@link Action}s
 * that users can perform based on such content.
 */
public final class Pane {
    @Keep
    @Nullable
    private final ActionList mActionList;
    @Keep
    private final List<Object> mRows;
    @Keep
    private final boolean mIsLoading;

    /** Constructs a new builder of {@link Pane}. */
    @NonNull
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Returns the list of {@link Action}s displayed alongside the {@link Row}s in this pane.
     */
    @Nullable
    public ActionList getActionList() {
        return mActionList;
    }

    /**
     * Returns the list of {@link Row} objects that make up the {@link Pane}.
     */
    @NonNull
    public List<Object> getRows() {
        return mRows;
    }

    /**
     * Returns the {@code true} if the {@link Pane} is loading.*
     */
    public boolean isLoading() {
        return mIsLoading;
    }

    @Override
    @NonNull
    public String toString() {
        return "[ rows: "
                + (mRows != null ? mRows.toString() : null)
                + ", action list: "
                + mActionList
                + "]";
    }

    @Override
    public int hashCode() {
        return Objects.hash(mRows, mActionList, mIsLoading);
    }

    @Override
    public boolean equals(@Nullable Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Pane)) {
            return false;
        }
        Pane otherPane = (Pane) other;

        return mIsLoading == otherPane.mIsLoading
                && Objects.equals(mActionList, otherPane.mActionList)
                && Objects.equals(mRows, otherPane.mRows);
    }

    private Pane(Builder builder) {
        mRows = new ArrayList<>(builder.mRows);
        mActionList = builder.mActionList;
        mIsLoading = builder.mIsLoading;
    }

    /** Constructs an empty instance, used by serialization code. */
    private Pane() {
        mRows = Collections.emptyList();
        mActionList = null;
        mIsLoading = false;
    }

    /** A builder of {@link Pane}. */
    public static final class Builder {
        private final List<Object> mRows = new ArrayList<>();
        @Nullable
        private ActionList mActionList;
        private boolean mIsLoading;

        /**
         * Sets whether the {@link Pane} is in a loading state.
         *
         * <p>If set to {@code true}, the UI will display a loading indicator where the list content
         * would be otherwise. The caller is expected to call {@link
         * androidx.car.app.Screen#invalidate()} and send the new template content
         * to the host once the data is ready. If set to {@code false}, the UI shows the actual row
         * contents.
         *
         * @see #build
         */
        @NonNull
        public Builder setLoading(boolean isLoading) {
            this.mIsLoading = isLoading;
            return this;
        }

        /**
         * Adds a row to display in the list.
         *
         * @throws NullPointerException if {@code row} is {@code null}.
         */
        @NonNull
        public Builder addRow(@NonNull Row row) {
            mRows.add(requireNonNull(row));
            return this;
        }

        /** Clears any rows that may have been added with {@link #addRow(Row)} up to this point. */
        @NonNull
        public Builder clearRows() {
            mRows.clear();
            return this;
        }

        /**
         * Sets multiple {@link Action}s to display alongside the rows in the pane.
         *
         * <p>By default, no actions are displayed.
         *
         * @throws NullPointerException if {@code actions} is {@code null}.
         */
        @NonNull
        // TODO(shiufai): consider rename to match getter's name (e.g. setActionList or getActions).
        @SuppressLint("MissingGetterMatchingBuilder")
        public Builder setActions(@NonNull List<Action> actions) {
            mActionList = ActionList.create(requireNonNull(actions));
            return this;
        }

        /**
         * Constructs the row list defined by this builder.
         *
         * @throws IllegalStateException if the pane is in loading state and also contains rows, or
         *                               vice-versa.
         */
        @NonNull
        public Pane build() {
            int size = size();
            if (size > 0 == mIsLoading) {
                throw new IllegalStateException(
                        "The pane is set to loading but is not empty, or vice versa");
            }

            return new Pane(this);
        }

        private int size() {
            return mRows.size();
        }
    }
}
