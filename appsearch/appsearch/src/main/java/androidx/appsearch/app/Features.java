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
package androidx.appsearch.app;

import androidx.annotation.RestrictTo;

import org.jspecify.annotations.NonNull;

import java.util.Set;

/**
 * A class that encapsulates all features that are only supported in certain cases (e.g. only on
 * certain implementations or only at a certain Android API Level).
 *
 * <p>Features do not depend on any runtime state, and features will never be removed. Once
 * {@link #isFeatureSupported} returns {@code true} for a certain feature, it is safe to assume that
 * the feature will be available forever on that AppSearch storage implementation, at that
 * Android API level, on that device.
 */

// @exportToFramework:copyToPath(../../../cts/tests/appsearch/testutils/src/android/app/appsearch/testutil/external/Features.java)
// Note: When adding new fields, The @RequiresFeature is needed in setters but could be skipped in
// getters if call the getter won't send unsupported requests to the AppSearch-framework-impl.
public interface Features {

    /**
     * Feature for {@link #isFeatureSupported(String)}. This feature covers
     * {@link SearchResult.MatchInfo#getSubmatchRange} and
     * {@link SearchResult.MatchInfo#getSubmatch}.
     */
    String SEARCH_RESULT_MATCH_INFO_SUBMATCH = "SEARCH_RESULT_MATCH_INFO_SUBMATCH";

    /**
     * Feature for {@link #isFeatureSupported(String)}. This feature covers
     * {@link GlobalSearchSession#registerObserverCallback} and
     * {@link GlobalSearchSession#unregisterObserverCallback}.
     */
    String GLOBAL_SEARCH_SESSION_REGISTER_OBSERVER_CALLBACK =
            "GLOBAL_SEARCH_SESSION_REGISTER_OBSERVER_CALLBACK";

    /**
     * Feature for {@link #isFeatureSupported(String)}. This feature covers
     * {@link GlobalSearchSession#getSchemaAsync}.
     */
    String GLOBAL_SEARCH_SESSION_GET_SCHEMA = "GLOBAL_SEARCH_SESSION_GET_SCHEMA";

    /**
     * Feature for {@link #isFeatureSupported(String)}. This feature covers
     * {@link GlobalSearchSession#getByDocumentIdAsync}.
     */
    String GLOBAL_SEARCH_SESSION_GET_BY_ID = "GLOBAL_SEARCH_SESSION_GET_BY_ID";

    /**
     * Feature for {@link #isFeatureSupported(String)}. This feature covers
     * {@link SetSchemaRequest.Builder#addRequiredPermissionsForSchemaTypeVisibility(String, Set)},
     * {@link SetSchemaRequest.Builder#clearRequiredPermissionsForSchemaTypeVisibility(String)},
     * {@link GetSchemaResponse#getSchemaTypesNotDisplayedBySystem()},
     * {@link GetSchemaResponse#getSchemaTypesVisibleToPackages()},
     * {@link GetSchemaResponse#getRequiredPermissionsForSchemaTypeVisibility()},
     * {@link SetSchemaRequest.Builder#addRequiredPermissionsForSchemaTypeVisibility} and
     * {@link SetSchemaRequest.Builder#clearRequiredPermissionsForSchemaTypeVisibility}
     */
    String ADD_PERMISSIONS_AND_GET_VISIBILITY = "ADD_PERMISSIONS_AND_GET_VISIBILITY";

    /**
     * Feature for {@link #isFeatureSupported(String)}. This feature covers
     * {@link AppSearchSchema.StringPropertyConfig#TOKENIZER_TYPE_RFC822}.
     */
    String TOKENIZER_TYPE_RFC822 = "TOKENIZER_TYPE_RFC822";

    /**
     * Feature for {@link #isFeatureSupported(String)}. This feature covers
     * {@link AppSearchSchema.LongPropertyConfig#INDEXING_TYPE_RANGE} and all other numeric search
     * features.
     *
     * <p>For details on the numeric search expressions in the query language, see
     * {@link AppSearchSession#search}.
     */
    // Note: The preferred name of this feature should have been LIST_FILTER_NUMERIC_SEARCH.
    String NUMERIC_SEARCH = FeatureConstants.NUMERIC_SEARCH;

    /**
     * Feature for {@link #isFeatureSupported(String)}. This feature covers
     * {@link AppSearchSchema.StringPropertyConfig#TOKENIZER_TYPE_VERBATIM} and all other
     * verbatim search features within the query language that allows clients to search using the
     * verbatim string operator.
     *
     * <p>For details on the verbatim string operator, see {@link AppSearchSession#search}.
     */
    // Note: The preferred name of this feature should have been LIST_FILTER_VERBATIM_SEARCH.
    String VERBATIM_SEARCH = FeatureConstants.VERBATIM_SEARCH;

    /**
     * Feature for {@link #isFeatureSupported(String)}. This feature covers the expansion of the
     * query language to conform to the definition of the list filters language
     * (https://aip.dev/160).
     *
     * <p>For more details, see {@link AppSearchSession#search}.
     */
    String LIST_FILTER_QUERY_LANGUAGE = FeatureConstants.LIST_FILTER_QUERY_LANGUAGE;

    /**
     * Feature for {@link #isFeatureSupported(String)}. This feature covers the use of the
     * "hasProperty" function in query expressions.
     *
     * <p>For details on the "hasProperty" function in the query language, see
     * {@link AppSearchSession#search}.
     */
    String LIST_FILTER_HAS_PROPERTY_FUNCTION = FeatureConstants.LIST_FILTER_HAS_PROPERTY_FUNCTION;

    /**
     * Feature for {@link #isFeatureSupported(String)}. This feature covers the use of the
     * "matchScoreExpression" function in query expressions.
     *
     * <p>For details on the "matchScoreExpression" function in the query language, see
     * {@link AppSearchSession#search}.
     */
    @ExperimentalAppSearchApi
    String LIST_FILTER_MATCH_SCORE_EXPRESSION_FUNCTION =
            FeatureConstants.LIST_FILTER_MATCH_SCORE_EXPRESSION_FUNCTION;

    /**
     * Feature for {@link #isFeatureSupported(String)}. This feature covers whether or not the
     * AppSearch backend can store the descriptions returned by
     * {@link AppSearchSchema#getDescription} and
     * {@link AppSearchSchema.PropertyConfig#getDescription}.
     */
    @ExperimentalAppSearchApi
    String SCHEMA_SET_DESCRIPTION = "SCHEMA_SET_DESCRIPTION";

    /**
     * Feature for {@link #isFeatureSupported(String)}. This feature covers
     * {@link AppSearchSchema.EmbeddingPropertyConfig}.
     *
     * <p>For details on the embedding search expressions, see {@link AppSearchSession#search} for
     * the query language and {@link SearchSpec.Builder#setRankingStrategy(String)} for the ranking
     * language.
     */
    String SCHEMA_EMBEDDING_PROPERTY_CONFIG = "SCHEMA_EMBEDDING_PROPERTY_CONFIG";

    /**
     * Feature for {@link #isFeatureSupported(String)}. This feature covers
     * {@link AppSearchSchema.EmbeddingPropertyConfig.Builder#setQuantizationType(int)}.
     */
    @ExperimentalAppSearchApi
    String SCHEMA_EMBEDDING_QUANTIZATION = "SCHEMA_EMBEDDING_QUANTIZATION";

    /**
     * Feature for {@link #isFeatureSupported(String)}. This feature covers
     * {@link SearchSpec#GROUPING_TYPE_PER_SCHEMA}
     */
    String SEARCH_SPEC_GROUPING_TYPE_PER_SCHEMA = "SEARCH_SPEC_GROUPING_TYPE_PER_SCHEMA";

    /**
     * Feature for {@link #isFeatureSupported(String)}. This feature covers
     * {@link SearchSpec.Builder#setPropertyWeights}.
     */
    String SEARCH_SPEC_PROPERTY_WEIGHTS = "SEARCH_SPEC_PROPERTY_WEIGHTS";

    /**
     * Feature for {@link #isFeatureSupported(String)}. This feature covers
     * {@link SearchSpec.Builder#addFilterProperties} and
     * {@link SearchSuggestionSpec.Builder#addFilterProperties}.
     */
    String SEARCH_SPEC_ADD_FILTER_PROPERTIES = "SEARCH_SPEC_ADD_FILTER_PROPERTIES";

    /**
     * Feature for {@link #isFeatureSupported(String)}. This feature covers
     * {@link SearchSpec.Builder#setRankingStrategy(String)}.
     */
    String SEARCH_SPEC_ADVANCED_RANKING_EXPRESSION = "SEARCH_SPEC_ADVANCED_RANKING_EXPRESSION";

    /**
     * Feature for {@link #isFeatureSupported(String)}. This feature covers the support of the
     * {@link SearchSpec.Builder#addSearchStringParameters} and
     * {@link SearchSuggestionSpec.Builder#addSearchStringParameters} apis.
     */
    String SEARCH_SPEC_SEARCH_STRING_PARAMETERS = "SEARCH_SPEC_SEARCH_STRING_PARAMETERS";

    /**
     * Feature for {@link #isFeatureSupported(String)}. This feature covers
     * {@link SearchSpec.Builder#addFilterDocumentIds}.
     */
    @ExperimentalAppSearchApi
    String SEARCH_SPEC_ADD_FILTER_DOCUMENT_IDS = "SEARCH_SPEC_ADD_FILTER_DOCUMENT_IDS";

    /**
     * Feature for {@link #isFeatureSupported(String)}. This feature covers
     * {@link AppSearchSchema.StringPropertyConfig#JOINABLE_VALUE_TYPE_QUALIFIED_ID},
     * {@link SearchSpec.Builder#setJoinSpec}, and all other join features.
     */
    String JOIN_SPEC_AND_QUALIFIED_ID = "JOIN_SPEC_AND_QUALIFIED_ID";

    /**
     * Feature for {@link #isFeatureSupported(String)}. This feature covers
     * {@link AppSearchSession#searchSuggestionAsync}.
     */
    String SEARCH_SUGGESTION = "SEARCH_SUGGESTION";

    /**
     * Feature for {@link #isFeatureSupported(String)}. This feature covers setting schemas with
     * circular references for {@link AppSearchSession#setSchemaAsync}.
     */
    String SET_SCHEMA_CIRCULAR_REFERENCES = "SET_SCHEMA_CIRCULAR_REFERENCES";

    /**
     * Feature for {@link #isFeatureSupported(String)}. This feature covers
     * {@link AppSearchSchema.Builder#addParentType}.
     */
    String SCHEMA_ADD_PARENT_TYPE = "SCHEMA_ADD_PARENT_TYPE";

    /**
     * Feature for {@link #isFeatureSupported(String)}. This feature covers
     * {@link
     * AppSearchSchema.DocumentPropertyConfig.Builder#addIndexableNestedProperties(String...)}.
     */
    String SCHEMA_ADD_INDEXABLE_NESTED_PROPERTIES = "SCHEMA_ADD_INDEXABLE_NESTED_PROPERTIES";

    /**
     * Feature for {@link #isFeatureSupported(String)}. This feature covers
     * {@link AppSearchSchema.DocumentPropertyConfig#getIndexableNestedProperties()}.
     */
    String SCHEMA_GET_INDEXABLE_NESTED_PROPERTIES = "SCHEMA_GET_INDEXABLE_NESTED_PROPERTIES";

    /**
     * Feature for {@link #isFeatureSupported(String)}. This feature covers
     * {@link AppSearchSchema.LongPropertyConfig.Builder#setScoringEnabled(boolean)},
     * {@link AppSearchSchema.DoublePropertyConfig.Builder#setScoringEnabled(boolean)},
     * {@link AppSearchSchema.BooleanPropertyConfig.Builder#setScoringEnabled(boolean)}.
     * {@link SearchSpec.Builder#setScorablePropertyRankingEnabled(boolean)}.
     */
    @ExperimentalAppSearchApi
    String SCHEMA_SCORABLE_PROPERTY_CONFIG = FeatureConstants.SCHEMA_SCORABLE_PROPERTY_CONFIG;

    /**
     * Feature for {@link #isFeatureSupported(String)}. This feature covers
     * {@link SearchSpec.Builder#setSearchSourceLogTag(String)}.
     */
    String SEARCH_SPEC_SET_SEARCH_SOURCE_LOG_TAG = "SEARCH_SPEC_SET_SEARCH_SOURCE_LOG_TAG";

    /**
     * Feature for {@link #isFeatureSupported(String)}. This feature covers
     * {@link SetSchemaRequest.Builder#setPubliclyVisibleSchema(String, PackageIdentifier)}.
     */
    String SET_SCHEMA_REQUEST_SET_PUBLICLY_VISIBLE = "SET_SCHEMA_REQUEST_SET_PUBLICLY_VISIBLE";

    /**
     * Feature for {@link #isFeatureSupported(String)}. This feature covers
     * {@link SetSchemaRequest.Builder#addSchemaTypeVisibleToConfig}.
     */
    String SET_SCHEMA_REQUEST_ADD_SCHEMA_TYPE_VISIBLE_TO_CONFIG =
            "SET_SCHEMA_REQUEST_ADD_SCHEMA_TYPE_VISIBLE_TO_CONFIG";

    /**
     * Feature for {@link #isFeatureSupported(String)}. This feature covers
     * {@link EnterpriseGlobalSearchSession}
     */
    String ENTERPRISE_GLOBAL_SEARCH_SESSION = "ENTERPRISE_GLOBAL_SEARCH_SESSION";

    /**
     * Feature for {@link #isFeatureSupported(String)}. This feature covers
     * {@link SearchSpec.Builder#addInformationalRankingExpressions}.
     */
    String SEARCH_SPEC_ADD_INFORMATIONAL_RANKING_EXPRESSIONS =
            "SEARCH_SPEC_ADD_INFORMATIONAL_RANKING_EXPRESSIONS";

    /**
     * Feature for {@link #isFeatureSupported(String)}. This feature covers
     * {@link AppSearchBlobHandle}.
     */
    // TODO(b/273591938) improve the java doc when we support set blob property in GenericDocument
    // TODO(b/273591938) unhide the API once it read for API review.
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @ExperimentalAppSearchApi
    String BLOB_STORAGE = "BLOB_STORAGE";

    /**
     * Feature for {@link #isFeatureSupported(String)}. This feature indicates whether or not the
     * MobileApplication corpus is being indexed.
     *
     * <p>This corpus contains documents representing applications installed on the device, and each
     * document includes an icon uri, a package id, a display name, and nicknames of the app. This
     * corpus may be queried by applications interested in querying for and launching applications,
     * such as a search app. {@link androidx.appsearch.builtintypes.MobileApplication} can be used
     * to parse documents returned from this corpus.
     *
     * <p>This corpus can be queried with a {@link GlobalSearchSession}, by filtering on the
     * "android" package and the "apps" namespace, for example:
     * <code>
     * globalSession.query("",
     * new SearchSpec.Builder().addFilterPackageNames("android").addFilterNamespace("apps").build())
     * </code>
     *
     * @see androidx.appsearch.builtintypes.MobileApplication
     */
    @ExperimentalAppSearchApi
    String INDEXER_MOBILE_APPLICATIONS = "INDEXER_MOBILE_APPLICATIONS";

    /**
     * Feature for {@link #isFeatureSupported(String)}. This feature covers whether to wrap the
     * parent types of a document in the corresponding
     * {@link androidx.appsearch.app.SearchResult}, instead of in
     * {@link androidx.appsearch.app.GenericDocument}.
     */
    @ExperimentalAppSearchApi
    String SEARCH_RESULT_PARENT_TYPES = "SEARCH_RESULT_PARENT_TYPES";

    /**
     * Feature for {@link #isFeatureSupported(String)}. This feature covers
     * {@link AppSearchSchema.StringPropertyConfig#DELETE_PROPAGATION_TYPE_PROPAGATE_FROM} and
     * {@link AppSearchSchema.StringPropertyConfig.Builder#setDeletePropagationType}.
     */
    // TODO(b/384947619) unhide the API once it is ready.
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @ExperimentalAppSearchApi
    String SCHEMA_STRING_PROPERTY_CONFIG_DELETE_PROPAGATION_TYPE_PROPAGATE_FROM =
            "SCHEMA_STRING_PROPERTY_CONFIG_DELETE_PROPAGATION_TYPE_PROPAGATE_FROM";

    /**
     * Feature for {@link #isFeatureSupported(String)}. This feature covers whether to use isolated
     * storage for user data.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    String ISOLATED_STORAGE = "ISOLATED_STORAGE";

    /**
     * Feature for {@link #isFeatureSupported(String)}. This feature covers the use of the
     * "minOrDefault" and "maxOrDefault" functions in ranking expressions.
     *
     * <p>For details on the functions in the ranking language, see
     * {@link SearchSpec.Builder#setRankingStrategy(String)}.
     */
    @ExperimentalAppSearchApi
    String SEARCH_SPEC_RANKING_FUNCTION_MAX_MIN_OR_DEFAULT =
            "SEARCH_SPEC_RANKING_FUNCTION_MAX_MIN_OR_DEFAULT";

    /**
     * Feature for {@link #isFeatureSupported(String)}. This feature covers the use of the
     * "filterByRange" function in ranking expressions.
     *
     * <p>For details on the function in the ranking language, see
     * {@link SearchSpec.Builder#setRankingStrategy(String)}.
     */
    @ExperimentalAppSearchApi
    String SEARCH_SPEC_RANKING_FUNCTION_FILTER_BY_RANGE =
            "SEARCH_SPEC_RANKING_FUNCTION_FILTER_BY_RANGE";

    /**
     * Feature for {@link #isFeatureSupported(String)}. This feature covers
     * {@link SearchSpec.Builder#setRetrieveEmbeddingMatchInfos(boolean)}.
     */
    @ExperimentalAppSearchApi
    String SEARCH_EMBEDDING_MATCH_INFO = "SEARCH_EMBEDDING_MATCH_INFO";

    /**
     * Feature for {@link #isFeatureSupported(String)}. This feature check if
     * READ_GLOBAL_APP_SEARCH_DATA permission is supported in this environment.
     * This permission allows documents to be visible on any system UI surface.
     * This feature covers {@link SetSchemaRequest.Builder#setSchemaTypeDisplayedBySystem}.
     */
    @ExperimentalAppSearchApi
    String SET_SCHEMA_REQUEST_SCHEMA_TYPE_DISPLAYED_BY_SYSTEM =
            "SET_SCHEMA_REQUEST_SCHEMA_TYPE_DISPLAYED_BY_SYSTEM";

    /**
     * Returns whether a feature is supported at run-time. Feature support depends on the
     * feature in question, the AppSearch backend being used and the Android version of the
     * device.
     *
     * <p class="note"><b>Note:</b> If this method returns {@code false}, it is not safe to invoke
     * the methods requiring the desired feature.
     *
     * @param feature the feature to be checked
     * @return whether the capability is supported given the Android API level and AppSearch
     * backend.
     */
    boolean isFeatureSupported(@NonNull String feature);

    /**
     * Returns the maximum amount of properties that can be indexed in a Document
     * given the Android API level and AppSearch backend.
     *
     * <p>A property is defined as all values that are present at a particular path.
     */
    int getMaxIndexedProperties();
}
