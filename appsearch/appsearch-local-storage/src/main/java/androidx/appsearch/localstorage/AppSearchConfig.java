/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.appsearch.localstorage;

import androidx.annotation.RestrictTo;
import androidx.appsearch.app.GenericDocument;

import com.google.android.icing.proto.PersistType;

import org.jspecify.annotations.NonNull;

/**
 * An interface that wraps AppSearch configurations required to create {@link AppSearchImpl}.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public interface AppSearchConfig extends IcingOptionsConfig, LimitConfig {

    /**
     * Whether to store {@link GenericDocument}'s parent types as a synthetic property. If not,
     * the list of parent types will be wrapped as a meta field in {@link GenericDocument}, in a
     * similar way as namespace, id, creationTimestamp, etc.
     */
    boolean shouldStoreParentInfoAsSyntheticProperty();

    /**
     * Whether to include the list of parent types when returning a {@link GenericDocument} or a
     * {@link androidx.appsearch.app.SearchResult} when
     * {@link androidx.appsearch.flags.Flags#FLAG_ENABLE_SEARCH_RESULT_PARENT_TYPES} in on.
     */
    boolean shouldRetrieveParentInfo();

    /**
     * Returns the {@code PersistType.Code} that should be used to persist common mutations such as
     * PUTs or DELETEs.
     */
    PersistType. @NonNull Code getLightweightPersistType();
}
