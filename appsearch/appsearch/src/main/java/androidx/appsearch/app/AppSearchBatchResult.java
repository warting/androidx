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
// @exportToFramework:skipFile()
package androidx.appsearch.app;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.collection.ArrayMap;
import androidx.core.util.Preconditions;

import java.util.Map;

/**
 * Provides results for AppSearch batch operations which encompass multiple documents.
 *
 * <p>Individual results of a batch operation are separated into two maps: one for successes and
 * one for failures. For successes, {@link #getSuccesses()} will return a map of keys to
 * instances of the value type. For failures, {@link #getFailures()} will return a map of keys to
 * {@link AppSearchResult} objects.
 *
 * <p>Alternatively, {@link #getAll()} returns a map of keys to {@link AppSearchResult} objects for
 * both successes and failures.
 *
 * @see AppSearchSession#put
 * @see AppSearchSession#getByUri
 * @see AppSearchSession#remove
 */
public final class AppSearchBatchResult<KeyType, ValueType> {
    @NonNull private final Map<KeyType, ValueType> mSuccesses;
    @NonNull private final Map<KeyType, AppSearchResult<ValueType>> mFailures;
    @NonNull private final Map<KeyType, AppSearchResult<ValueType>> mAll;

    AppSearchBatchResult(
            @NonNull Map<KeyType, ValueType> successes,
            @NonNull Map<KeyType, AppSearchResult<ValueType>> failures,
            @NonNull Map<KeyType, AppSearchResult<ValueType>> all) {
        mSuccesses = successes;
        mFailures = failures;
        mAll = all;
    }

    /** Returns {@code true} if this {@link AppSearchBatchResult} has no failures. */
    public boolean isSuccess() {
        return mFailures.isEmpty();
    }

    /**
     * Returns a {@link Map} of keys mapped to instances of the value type for all successful
     * individual results.
     *
     * <p>Example: {@link AppSearchSession#getByUri} returns an {@link AppSearchBatchResult}. Each
     * key (a URI of {@code String} type) will map to a {@link GenericDocument} object.
     *
     * <p>The values of the {@link Map} will not be {@code null}.
     */
    @NonNull
    public Map<KeyType, ValueType> getSuccesses() {
        return mSuccesses;
    }

    /**
     * Returns a {@link Map} of keys mapped to instances of {@link AppSearchResult} for all
     * failed individual results.
     *
     * <p>The values of the {@link Map} will not be {@code null}.
     */
    @NonNull
    public Map<KeyType, AppSearchResult<ValueType>> getFailures() {
        return mFailures;
    }

    /**
     * Returns a {@link Map} of keys mapped to instances of {@link AppSearchResult} for all
     * individual results.
     *
     * <p>The values of the {@link Map} will not be {@code null}.
     */
    @NonNull
    public Map<KeyType, AppSearchResult<ValueType>> getAll() {
        return mAll;
    }

    /**
     * Asserts that this {@link AppSearchBatchResult} has no failures.
     * @hide
     */
    public void checkSuccess() {
        if (!isSuccess()) {
            throw new IllegalStateException("AppSearchBatchResult has failures: " + this);
        }
    }

    @Override
    @NonNull
    public String toString() {
        return "{\n  successes: " + mSuccesses + "\n  failures: " + mFailures + "\n}";
    }

    /**
     * Builder for {@link AppSearchBatchResult} objects.
     *
     * <p>Once {@link #build} is called, the instance can no longer be used.
     *
     * @hide
     */
    public static final class Builder<KeyType, ValueType> {
        private final Map<KeyType, ValueType> mSuccesses = new ArrayMap<>();
        private final Map<KeyType, AppSearchResult<ValueType>> mFailures = new ArrayMap<>();
        private final Map<KeyType, AppSearchResult<ValueType>> mAll = new ArrayMap<>();
        private boolean mBuilt = false;

        /**
         * Associates the {@code key} with the provided successful return value.
         *
         * <p>Any previous mapping for a key, whether success or failure, is deleted.
         *
         * @throws IllegalStateException if the builder has already been used.
         */
        @NonNull
        public Builder<KeyType, ValueType> setSuccess(
                @NonNull KeyType key, @Nullable ValueType result) {
            Preconditions.checkState(!mBuilt, "Builder has already been used");
            Preconditions.checkNotNull(key);
            return setResult(key, AppSearchResult.newSuccessfulResult(result));
        }

        /**
         * Associates the {@code key} with the provided failure code and error message.
         *
         * <p>Any previous mapping for a key, whether success or failure, is deleted.
         *
         * @throws IllegalStateException if the builder has already been used.
         */
        @NonNull
        public Builder<KeyType, ValueType> setFailure(
                @NonNull KeyType key,
                @AppSearchResult.ResultCode int resultCode,
                @Nullable String errorMessage) {
            Preconditions.checkState(!mBuilt, "Builder has already been used");
            Preconditions.checkNotNull(key);
            return setResult(key, AppSearchResult.newFailedResult(resultCode, errorMessage));
        }

        /**
         * Associates the {@code key} with the provided {@code result}.
         *
         * <p>Any previous mapping for a key, whether success or failure, is deleted.
         *
         * @throws IllegalStateException if the builder has already been used.
         */
        @NonNull
        public Builder<KeyType, ValueType> setResult(
                @NonNull KeyType key, @NonNull AppSearchResult<ValueType> result) {
            Preconditions.checkState(!mBuilt, "Builder has already been used");
            Preconditions.checkNotNull(key);
            Preconditions.checkNotNull(result);
            if (result.isSuccess()) {
                mSuccesses.put(key, result.getResultValue());
                mFailures.remove(key);
            } else {
                mFailures.put(key, result);
                mSuccesses.remove(key);
            }
            mAll.put(key, result);
            return this;
        }

        /**
         * Builds an {@link AppSearchBatchResult} object from the contents of this {@link Builder}.
         *
         * @throws IllegalStateException if the builder has already been used.
         */
        @NonNull
        public AppSearchBatchResult<KeyType, ValueType> build() {
            Preconditions.checkState(!mBuilt, "Builder has already been used");
            mBuilt = true;
            return new AppSearchBatchResult<>(mSuccesses, mFailures, mAll);
        }
    }
}
