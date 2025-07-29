/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.appsearch.localstorage.visibilitystore;

import static androidx.appsearch.localstorage.visibilitystore.VisibilityStoreMigrationHelperFromV0.DEPRECATED_ACCESSIBLE_SCHEMA_PROPERTY;
import static androidx.appsearch.localstorage.visibilitystore.VisibilityStoreMigrationHelperFromV0.DEPRECATED_NOT_DISPLAYED_BY_SYSTEM_PROPERTY;
import static androidx.appsearch.localstorage.visibilitystore.VisibilityStoreMigrationHelperFromV0.DEPRECATED_PACKAGE_NAME_PROPERTY;
import static androidx.appsearch.localstorage.visibilitystore.VisibilityStoreMigrationHelperFromV0.DEPRECATED_PACKAGE_SCHEMA_TYPE;
import static androidx.appsearch.localstorage.visibilitystore.VisibilityStoreMigrationHelperFromV0.DEPRECATED_SHA_256_CERT_PROPERTY;
import static androidx.appsearch.localstorage.visibilitystore.VisibilityStoreMigrationHelperFromV0.DEPRECATED_VISIBILITY_SCHEMA_TYPE;
import static androidx.appsearch.localstorage.visibilitystore.VisibilityStoreMigrationHelperFromV0.DEPRECATED_VISIBLE_TO_PACKAGES_PROPERTY;

import static com.google.common.truth.Truth.assertThat;

import androidx.appsearch.app.AppSearchSchema;
import androidx.appsearch.app.GenericDocument;
import androidx.appsearch.app.InternalSetSchemaResponse;
import androidx.appsearch.app.InternalVisibilityConfig;
import androidx.appsearch.app.PackageIdentifier;
import androidx.appsearch.localstorage.AppSearchConfig;
import androidx.appsearch.localstorage.AppSearchConfigImpl;
import androidx.appsearch.localstorage.AppSearchImpl;
import androidx.appsearch.localstorage.LocalStorageIcingOptionsConfig;
import androidx.appsearch.localstorage.OptimizeStrategy;
import androidx.appsearch.localstorage.UnlimitedLimitConfig;
import androidx.appsearch.localstorage.util.PrefixUtil;

import com.google.common.collect.ImmutableList;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.util.Collections;

public class VisibilityStoreMigrationHelperFromV0Test {

    /**
     * Always trigger optimize in this class. OptimizeStrategy will be tested in its own test class.
     */
    private static final OptimizeStrategy ALWAYS_OPTIMIZE = optimizeInfo -> true;

    @Rule
    public TemporaryFolder mTemporaryFolder = new TemporaryFolder();
    private File mFile;
    private AppSearchConfig mConfig = new AppSearchConfigImpl(new UnlimitedLimitConfig(),
            new LocalStorageIcingOptionsConfig());

    @Before
    public void setUp() throws Exception {
        // Give ourselves global query permissions
        mFile = mTemporaryFolder.newFolder();
    }

    @Test
    @SuppressWarnings("deprecation") // AppSearchImpl.putDocument
    public void testVisibilityMigration_from0() throws Exception {
        // Values for a "foo" client
        String packageNameFoo = "packageFoo";
        byte[] sha256CertFoo = new byte[32];

        // Values for a "bar" client
        String packageNameBar = "packageBar";
        byte[] sha256CertBar = new byte[32];

        // Create AppSearchImpl with visibility document version 0;
        AppSearchImpl appSearchImplInV0 = buildAppSearchImplInV0();
        // Build deprecated visibility documents in version 0
        // "schema1" and "schema2" are platform hidden.
        // "schema1" is accessible to packageFoo and "schema2" is accessible to packageBar.
        String prefix = PrefixUtil.createPrefix("package", "database");
        GenericDocument deprecatedVisibilityToPackageFoo = new GenericDocument.Builder<>(
                VisibilityToDocumentConverter.VISIBILITY_DOCUMENT_NAMESPACE, "",
                DEPRECATED_PACKAGE_SCHEMA_TYPE)
                .setPropertyString(DEPRECATED_ACCESSIBLE_SCHEMA_PROPERTY, prefix + "Schema1")
                .setPropertyString(DEPRECATED_PACKAGE_NAME_PROPERTY, packageNameFoo)
                .setPropertyBytes(DEPRECATED_SHA_256_CERT_PROPERTY, sha256CertFoo)
                .build();
        GenericDocument deprecatedVisibilityToPackageBar = new GenericDocument.Builder<>(
                VisibilityToDocumentConverter.VISIBILITY_DOCUMENT_NAMESPACE, "",
                DEPRECATED_PACKAGE_SCHEMA_TYPE)
                .setPropertyString(DEPRECATED_ACCESSIBLE_SCHEMA_PROPERTY, prefix + "Schema2")
                .setPropertyString(DEPRECATED_PACKAGE_NAME_PROPERTY, packageNameBar)
                .setPropertyBytes(DEPRECATED_SHA_256_CERT_PROPERTY, sha256CertBar)
                .build();
        GenericDocument deprecatedVisibilityDocument = new GenericDocument.Builder<>(
                VisibilityToDocumentConverter.VISIBILITY_DOCUMENT_NAMESPACE,
                VisibilityStoreMigrationHelperFromV0.getDeprecatedVisibilityDocumentId(
                        "package", "database"),
                DEPRECATED_VISIBILITY_SCHEMA_TYPE)
                .setPropertyString(DEPRECATED_NOT_DISPLAYED_BY_SYSTEM_PROPERTY,
                        prefix + "Schema1", prefix + "Schema2")
                .setPropertyDocument(DEPRECATED_VISIBLE_TO_PACKAGES_PROPERTY,
                        deprecatedVisibilityToPackageFoo, deprecatedVisibilityToPackageBar)
                .build();

        // Set some client schemas into AppSearchImpl with empty VisibilityDocument since we need to
        // directly put old version of VisibilityDocument.
        InternalSetSchemaResponse internalSetSchemaResponse = appSearchImplInV0.setSchema(
                "package",
                "database",
                ImmutableList.of(
                        new AppSearchSchema.Builder("schema1").build(),
                        new AppSearchSchema.Builder("schema2").build()),
                /*prefixedVisibilityBundles=*/ Collections.emptyList(),
                /*forceOverride=*/ false,
                /*schemaVersion=*/ 0,
                /*setSchemaStatsBuilder=*/ null,
                /*callStatsBuilder=*/ null);
        assertThat(internalSetSchemaResponse.isSuccess()).isTrue();

        // Put deprecated visibility documents in version 0 to AppSearchImpl
        appSearchImplInV0.putDocument(
                VisibilityStore.VISIBILITY_PACKAGE_NAME,
                VisibilityStore.DOCUMENT_VISIBILITY_DATABASE_NAME,
                deprecatedVisibilityDocument,
                /*sendChangeNotifications=*/ false,
                /*logger=*/null,
                /*callStatsBuilder=*/ null);

        // Persist to disk and re-open the AppSearchImpl
        appSearchImplInV0.close();
        AppSearchImpl appSearchImpl = AppSearchImpl.create(mFile,
                mConfig,
                /*initStatsBuilder=*/ null,
                /*callStatsBuilder=*/ null,
                /*visibilityChecker=*/ null,
                /*revocableFileDescriptorStore=*/ null,
                /*icingSearchEngine=*/ null,
                ALWAYS_OPTIMIZE);

        GenericDocument actualDocument1 =
                appSearchImpl.getDocument(
                        VisibilityStore.VISIBILITY_PACKAGE_NAME,
                        VisibilityStore.DOCUMENT_VISIBILITY_DATABASE_NAME,
                        VisibilityToDocumentConverter.VISIBILITY_DOCUMENT_NAMESPACE,
                        /*id=*/ prefix + "Schema1",
                        /*typePropertyPaths=*/ Collections.emptyMap(),
                        /*callStatsBuilder=*/ null);
        GenericDocument actualDocument2 =
                appSearchImpl.getDocument(
                        VisibilityStore.VISIBILITY_PACKAGE_NAME,
                        VisibilityStore.DOCUMENT_VISIBILITY_DATABASE_NAME,
                        VisibilityToDocumentConverter.VISIBILITY_DOCUMENT_NAMESPACE,
                        /*id=*/ prefix + "Schema2",
                        /*typePropertyPaths=*/ Collections.emptyMap(),
                        /*callStatsBuilder=*/ null);

        GenericDocument expectedDocument1 = VisibilityToDocumentConverter.createVisibilityDocument(
                new InternalVisibilityConfig.Builder(prefix + "Schema1")
                        .setNotDisplayedBySystem(true)
                        .addVisibleToPackage(new PackageIdentifier(packageNameFoo, sha256CertFoo))
                        .build());
        GenericDocument expectedDocument2 = VisibilityToDocumentConverter.createVisibilityDocument(
                new InternalVisibilityConfig.Builder(prefix + "Schema2")
                        .setNotDisplayedBySystem(true)
                        .addVisibleToPackage(new PackageIdentifier(packageNameBar, sha256CertBar))
                        .build());

        // Ignore the creation timestamp
        actualDocument1 = new GenericDocument.Builder<>(actualDocument1)
                .setCreationTimestampMillis(0).build();
        actualDocument2 = new GenericDocument.Builder<>(actualDocument2)
                .setCreationTimestampMillis(0).build();

        assertThat(actualDocument1).isEqualTo(expectedDocument1);
        assertThat(actualDocument2).isEqualTo(expectedDocument2);
        appSearchImpl.close();
    }

    /** Build AppSearchImpl with deprecated visibility schemas version 0.     */
    private AppSearchImpl buildAppSearchImplInV0() throws Exception {
        AppSearchSchema visibilityDocumentSchemaV0 = new AppSearchSchema.Builder(
                DEPRECATED_VISIBILITY_SCHEMA_TYPE)
                .addProperty(new AppSearchSchema.StringPropertyConfig.Builder(
                        DEPRECATED_NOT_DISPLAYED_BY_SYSTEM_PROPERTY)
                        .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_REPEATED)
                        .build())
                .addProperty(new AppSearchSchema.DocumentPropertyConfig.Builder(
                        DEPRECATED_VISIBLE_TO_PACKAGES_PROPERTY,
                        DEPRECATED_PACKAGE_SCHEMA_TYPE)
                        .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_REPEATED)
                        .build())
                .build();
        AppSearchSchema visibilityToPackagesSchemaV0 = new AppSearchSchema.Builder(
                DEPRECATED_PACKAGE_SCHEMA_TYPE)
                .addProperty(new AppSearchSchema.StringPropertyConfig.Builder(
                        DEPRECATED_PACKAGE_NAME_PROPERTY)
                        .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
                        .build())
                .addProperty(new AppSearchSchema.BytesPropertyConfig.Builder(
                        DEPRECATED_SHA_256_CERT_PROPERTY)
                        .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
                        .build())
                .addProperty(new AppSearchSchema.StringPropertyConfig.Builder(
                        DEPRECATED_ACCESSIBLE_SCHEMA_PROPERTY)
                        .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
                        .build())
                .build();
        // Set deprecated visibility schema version 0 into AppSearchImpl.
        AppSearchImpl appSearchImpl = AppSearchImpl.create(mFile,
                mConfig,
                /*initStatsBuilder=*/ null,
                /*callStatsBuilder=*/ null,
                /*visibilityChecker=*/ null,
                /*revocableFileDescriptorStore=*/ null,
                /*icingSearchEngine=*/ null,
                ALWAYS_OPTIMIZE);
        InternalSetSchemaResponse internalSetSchemaResponse = appSearchImpl.setSchema(
                VisibilityStore.VISIBILITY_PACKAGE_NAME,
                VisibilityStore.DOCUMENT_VISIBILITY_DATABASE_NAME,
                ImmutableList.of(visibilityDocumentSchemaV0, visibilityToPackagesSchemaV0),
                /*prefixedVisibilityBundles=*/ Collections.emptyList(),
                /*forceOverride=*/ true, // force push the old version into disk
                /*version=*/ 0,
                /*setSchemaStatsBuilder=*/ null,
                /*callStatsBuilder=*/ null);
        assertThat(internalSetSchemaResponse.isSuccess()).isTrue();
        return appSearchImpl;
    }
}
