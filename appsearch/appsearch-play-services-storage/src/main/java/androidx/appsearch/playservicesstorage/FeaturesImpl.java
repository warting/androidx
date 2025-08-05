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

package androidx.appsearch.playservicesstorage;

import androidx.annotation.OptIn;
import androidx.appsearch.app.ExperimentalAppSearchApi;
import androidx.appsearch.app.Features;

import org.jspecify.annotations.NonNull;

/**
 * An implementation of {@link Features}. Feature availability is dependent on Android API
 * level and GMSCore AppSearch module.
 */
final class FeaturesImpl implements Features {
    @Override
    @OptIn(markerClass = ExperimentalAppSearchApi.class)
    public boolean isFeatureSupported(@NonNull String feature) {
        switch (feature) {
            case Features.ADD_PERMISSIONS_AND_GET_VISIBILITY:
                // fall through
            case Features.GLOBAL_SEARCH_SESSION_GET_SCHEMA:
                // fall through
            case Features.GLOBAL_SEARCH_SESSION_GET_BY_ID:
                // fall through
            case Features.JOIN_SPEC_AND_QUALIFIED_ID:
                // fall through
            case Features.NUMERIC_SEARCH:
                // fall through
            case Features.VERBATIM_SEARCH:
                // fall through
            case Features.LIST_FILTER_QUERY_LANGUAGE:
                // fall through
            case Features.LIST_FILTER_HAS_PROPERTY_FUNCTION:
                // fall through
            case Features.SEARCH_SPEC_GROUPING_TYPE_PER_SCHEMA:
                // fall through
            case Features.SEARCH_RESULT_MATCH_INFO_SUBMATCH:
                // fall through
            case Features.SEARCH_SPEC_PROPERTY_WEIGHTS:
                // fall through
            case Features.TOKENIZER_TYPE_RFC822:
                // fall through
            case Features.SEARCH_SPEC_ADVANCED_RANKING_EXPRESSION:
                // fall through
            case Features.SEARCH_SUGGESTION:
                // fall through
            case Features.SET_SCHEMA_CIRCULAR_REFERENCES:
                // fall through
            case Features.SCHEMA_ADD_PARENT_TYPE:
                // fall through
            case Features.SCHEMA_ADD_INDEXABLE_NESTED_PROPERTIES:
                // fall through
            case Features.SEARCH_SPEC_ADD_FILTER_PROPERTIES:
                // fall through
            case Features.SEARCH_SPEC_SET_SEARCH_SOURCE_LOG_TAG:
                // fall through
            case Features.SET_SCHEMA_REQUEST_SET_PUBLICLY_VISIBLE:
                // fall through
            case Features.SET_SCHEMA_REQUEST_ADD_SCHEMA_TYPE_VISIBLE_TO_CONFIG:
                // fall through
            case Features.SEARCH_SPEC_RANKING_FUNCTION_MAX_MIN_OR_DEFAULT:
                // fall through
            case Features.SEARCH_SPEC_RANKING_FUNCTION_FILTER_BY_RANGE:
                return true; // AppSearch features present in GMSCore AppSearch.

            // RegisterObserver and UnregisterObserver are not yet supported by GMSCore AppSearch.
            // TODO(b/208654892) : Update to reflect support once this feature is supported.
            case Features.GLOBAL_SEARCH_SESSION_REGISTER_OBSERVER_CALLBACK:
                // fall through
            case Features.SCHEMA_EMBEDDING_PROPERTY_CONFIG:
                // fall through
            case Features.SCHEMA_EMBEDDING_QUANTIZATION:
                // fall through
            case Features.SCHEMA_SET_DESCRIPTION:
                // fall through
            case Features.SEARCH_SPEC_ADD_INFORMATIONAL_RANKING_EXPRESSIONS:
                // fall through
            case Features.SEARCH_SPEC_SEARCH_STRING_PARAMETERS:
                // fall through
            case Features.SEARCH_SPEC_ADD_FILTER_DOCUMENT_IDS:
                // fall through
            case Features.LIST_FILTER_MATCH_SCORE_EXPRESSION_FUNCTION:
                // fall through
            case Features.SCHEMA_SCORABLE_PROPERTY_CONFIG:
                // fall through
            case Features.SEARCH_RESULT_PARENT_TYPES:
                // fall through
            case Features.SCHEMA_STRING_PROPERTY_CONFIG_DELETE_PROPAGATION_TYPE_PROPAGATE_FROM:
                // fall through
            case Features.INDEXER_MOBILE_APPLICATIONS:
                // TODO(b/275592563) : Update once this features is supported.
                // fall through
            case Features.SEARCH_EMBEDDING_MATCH_INFO:
                // TODO(b/395128139) : Update once this features is supported.
                // fall through

            default:
                return false; // AppSearch features absent in GMSCore AppSearch.
        }
    }
    @Override
    public int getMaxIndexedProperties() {
        return 64;
    }
}
