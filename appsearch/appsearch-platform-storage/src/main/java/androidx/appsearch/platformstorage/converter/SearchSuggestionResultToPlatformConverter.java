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

package androidx.appsearch.platformstorage.converter;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.appsearch.app.SearchSuggestionResult;
import androidx.core.util.Preconditions;

import java.util.ArrayList;
import java.util.List;

/**
 * Translates between Platform and Jetpack versions of {@link SearchSuggestionResult}.
 *
 * @exportToFramework:hide
 */
// TODO(b/227356108) replace literal '34' with Build.VERSION_CODES.U once the SDK_INT is finalized.
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@RequiresApi(34)
public class SearchSuggestionResultToPlatformConverter {
    private SearchSuggestionResultToPlatformConverter() {}

    /** Translates from Platform to Jetpack versions of {@linkSearchSuggestionResult}   */
    @NonNull
    public static List<SearchSuggestionResult> toJetpackSearchSuggestionResults(
            @NonNull List<android.app.appsearch.SearchSuggestionResult>
                    platformSearchSuggestionResults) {
        Preconditions.checkNotNull(platformSearchSuggestionResults);
        List<SearchSuggestionResult> jetpackSearchSuggestionResults =
                new ArrayList<>(platformSearchSuggestionResults.size());
        for (int i = 0; i < platformSearchSuggestionResults.size(); i++) {
            jetpackSearchSuggestionResults.add(new SearchSuggestionResult.Builder()
                    .setSuggestedResult(platformSearchSuggestionResults.get(i).getSuggestedResult())
                    .build());
        }
        return jetpackSearchSuggestionResults;
    }
}
