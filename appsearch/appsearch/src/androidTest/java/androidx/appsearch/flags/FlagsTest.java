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

package androidx.appsearch.flags;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;

public class FlagsTest {
    @Test
    public void testFlagValue_enableSafeParcelable() {
        assertThat(Flags.FLAG_ENABLE_SAFE_PARCELABLE).isEqualTo(
                "com.android.appsearch.flags.enable_safe_parcelable");
    }

    @Test
    public void testFlagValue_enableListFilterHasPropertyFunction() {
        assertThat(Flags.FLAG_ENABLE_LIST_FILTER_HAS_PROPERTY_FUNCTION).isEqualTo(
                "com.android.appsearch.flags.enable_list_filter_has_property_function");
    }

    @Test
    public void testFlagValue_enableGroupingTypePerSchema() {
        assertThat(Flags.FLAG_ENABLE_GROUPING_TYPE_PER_SCHEMA).isEqualTo(
                "com.android.appsearch.flags.enable_grouping_type_per_schema");
    }

    @Test
    public void testFlagValue_enableGenericDocumentCopyConstructor() {
        assertThat(Flags.FLAG_ENABLE_GENERIC_DOCUMENT_COPY_CONSTRUCTOR).isEqualTo("com.android"
                + ".appsearch.flags.enable_generic_document_copy_constructor");
    }

    public void testFlagValue_enableSearchSpecFilterProperties() {
        assertThat(Flags.FLAG_ENABLE_SEARCH_SPEC_FILTER_PROPERTIES).isEqualTo(
                "com.android.appsearch.flags.enable_search_spec_filter_properties");
    }
}
