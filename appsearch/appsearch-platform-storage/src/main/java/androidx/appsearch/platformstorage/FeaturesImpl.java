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
package androidx.appsearch.platformstorage;

import android.content.Context;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.appsearch.app.Features;
import androidx.appsearch.platformstorage.util.AppSearchVersionUtil;
import androidx.core.util.Preconditions;

/**
 * An implementation of {@link Features}. Feature availability is dependent on Android API
 * level.
 */
final class FeaturesImpl implements Features {
    // Context is used to check mainline module version, as support varies by module version.
    private final Context mContext;

    FeaturesImpl(@NonNull Context context) {
        mContext = Preconditions.checkNotNull(context);
    }

    @Override
    public boolean isFeatureSupported(@NonNull String feature) {
        switch (feature) {
            // Android T Features
            case Features.ADD_PERMISSIONS_AND_GET_VISIBILITY:
                // fall through
            case Features.GLOBAL_SEARCH_SESSION_GET_SCHEMA:
                // fall through
            case Features.GLOBAL_SEARCH_SESSION_GET_BY_ID:
                // fall through
            case Features.GLOBAL_SEARCH_SESSION_REGISTER_OBSERVER_CALLBACK:
                // fall through
            case Features.SEARCH_RESULT_MATCH_INFO_SUBMATCH:
                return Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU;

            // Android U Features
            case Features.JOIN_SPEC_AND_QUALIFIED_ID:
                // fall through
            case Features.LIST_FILTER_QUERY_LANGUAGE:
                // fall through
            case Features.NUMERIC_SEARCH:
                // fall through
            case Features.SEARCH_SPEC_ADVANCED_RANKING_EXPRESSION:
                // fall through
            case Features.SEARCH_SPEC_PROPERTY_WEIGHTS:
                // fall through
            case Features.SEARCH_SUGGESTION:
                // fall through
            case Features.TOKENIZER_TYPE_RFC822:
                // fall through
            case Features.VERBATIM_SEARCH:
                // fall through
            case Features.SET_SCHEMA_CIRCULAR_REFERENCES:
                return Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE;

            // Beyond Android U features
            case Features.SEARCH_SPEC_GROUPING_TYPE_PER_SCHEMA:
                // TODO(b/258715421) : Update to reflect support in Android U+ once this feature has
                // an extservices sdk that includes it.
                // fall through
            case Features.SCHEMA_ADD_PARENT_TYPE:
                // TODO(b/269295094) : Update when feature is ready in service-appsearch.
                // fall through
            case Features.SCHEMA_ADD_INDEXABLE_NESTED_PROPERTIES:
                // TODO(b/289150947) : Update when feature is ready in service-appsearch.
                // fall through
            case Features.SEARCH_SPEC_ADD_FILTER_PROPERTIES:
                // TODO(b/296088047) : Update when feature is ready in service-appsearch.
                // fall through
            case Features.LIST_FILTER_HAS_PROPERTY_FUNCTION:
                // TODO(b/309826655) : Update when feature is ready in service-appsearch.
                // fall through
            case Features.SCHEMA_EMBEDDING_PROPERTY_CONFIG:
                // TODO(b/326656531) : Update when feature is ready in service-appsearch.
                // fall through
            case Features.SEARCH_SPEC_SET_SEARCH_SOURCE_LOG_TAG:
                // TODO(b/309826655) : Update when feature is ready in service-appsearch.
                // fall through
            case Features.SET_SCHEMA_REQUEST_SET_PUBLICLY_VISIBLE:
                // TODO(b/296088047) : Update when feature is ready in service-appsearch.
                // fall through
            case Features.SET_SCHEMA_REQUEST_ADD_SCHEMA_TYPE_VISIBLE_TO_CONFIG:
                // TODO(b/296088047) : Update when feature is ready in service-appsearch.
                // fall through
            case Features.ENTERPRISE_GLOBAL_SEARCH_SESSION:
                // TODO(b/237388235) : Update when feature is ready in service-appsearch.
                return false;
            default:
                return false;
        }
    }

    @Override
    public int getMaxIndexedProperties() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            return 64;
        } else if (Build.VERSION.SDK_INT == Build.VERSION_CODES.TIRAMISU) {
            // Sixty-four properties were enabled in mainline module of the U base version
            return AppSearchVersionUtil.getAppSearchVersionCode(mContext)
                    >= AppSearchVersionUtil.APPSEARCH_U_BASE_VERSION_CODE ? 64 : 16;
        } else {
            return 16;
        }
    }
}
