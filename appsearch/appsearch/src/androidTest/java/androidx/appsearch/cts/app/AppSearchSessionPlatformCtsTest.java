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
package androidx.appsearch.cts.app;

import static androidx.appsearch.testutil.AppSearchTestUtils.checkIsBatchResultSuccess;
import static androidx.appsearch.testutil.AppSearchTestUtils.doGet;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assume.assumeTrue;

import android.content.Context;
import android.content.pm.ModuleInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.appsearch.app.AppSearchBatchResult;
import androidx.appsearch.app.AppSearchSchema;
import androidx.appsearch.app.AppSearchSession;
import androidx.appsearch.app.Features;
import androidx.appsearch.app.GenericDocument;
import androidx.appsearch.app.GetByDocumentIdRequest;
import androidx.appsearch.app.PutDocumentsRequest;
import androidx.appsearch.app.SetSchemaRequest;
import androidx.appsearch.platformstorage.PlatformStorage;
import androidx.appsearch.testutil.AppSearchEmail;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.filters.SdkSuppress;

import com.google.common.util.concurrent.ListenableFuture;

import org.junit.Test;

import java.util.List;
import java.util.concurrent.ExecutorService;

@SdkSuppress(minSdkVersion = Build.VERSION_CODES.S)
public class AppSearchSessionPlatformCtsTest extends AppSearchSessionCtsTestBase {
    static final String APPSEARCH_MAINLINE_MODULE_NAME = "com.google.android.appsearch";

    @Override
    protected ListenableFuture<AppSearchSession> createSearchSessionAsync(@NonNull String dbName) {
        Context context = ApplicationProvider.getApplicationContext();
        return PlatformStorage.createSearchSessionAsync(
                new PlatformStorage.SearchContext.Builder(context, dbName).build());
    }

    @Override
    protected ListenableFuture<AppSearchSession> createSearchSessionAsync(
            @NonNull String dbName, @NonNull ExecutorService executor) {
        Context context = ApplicationProvider.getApplicationContext();
        return PlatformStorage.createSearchSessionAsync(
                new PlatformStorage.SearchContext.Builder(context, dbName)
                        .setWorkerExecutor(executor).build());
    }

    @Test
    public void testFeaturesSupported() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        AppSearchSession db2 = PlatformStorage.createSearchSessionAsync(
                new PlatformStorage.SearchContext.Builder(context, DB_NAME_2).build()).get();
        assertThat(db2.getFeatures().isFeatureSupported(
                Features.SEARCH_RESULT_MATCH_INFO_SUBMATCH))
                .isEqualTo(Build.VERSION.SDK_INT >= 33);
        assertThat(db2.getFeatures().isFeatureSupported(
                Features.GLOBAL_SEARCH_SESSION_REGISTER_OBSERVER_CALLBACK))
                .isEqualTo(Build.VERSION.SDK_INT >= 33);
        assertThat(db2.getFeatures().isFeatureSupported(
                Features.GLOBAL_SEARCH_SESSION_GET_SCHEMA))
                .isEqualTo(Build.VERSION.SDK_INT >= 33);
        assertThat(db2.getFeatures().isFeatureSupported(
                Features.GLOBAL_SEARCH_SESSION_GET_BY_ID))
                .isEqualTo(Build.VERSION.SDK_INT >= 33);
        assertThat(db2.getFeatures().isFeatureSupported(
                Features.ADD_PERMISSIONS_AND_GET_VISIBILITY))
                .isEqualTo(Build.VERSION.SDK_INT >= 33);
    }

    @Test
    public void testPutDocuments_emptyBytesAndDocuments() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        AppSearchSession db = PlatformStorage.createSearchSessionAsync(
                new PlatformStorage.SearchContext.Builder(context, DB_NAME_1).build()).get();
        // Schema registration
        AppSearchSchema schema = new AppSearchSchema.Builder("testSchema")
                .addProperty(new AppSearchSchema.BytesPropertyConfig.Builder("bytes")
                        .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_REPEATED)
                        .build())
                .addProperty(new AppSearchSchema.DocumentPropertyConfig.Builder(
                        "document", AppSearchEmail.SCHEMA_TYPE)
                        .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_REPEATED)
                        .setShouldIndexNestedProperties(true)
                        .build())
                .build();
        db.setSchemaAsync(new SetSchemaRequest.Builder()
                .addSchemas(schema, AppSearchEmail.SCHEMA).build()).get();

        // Index a document
        GenericDocument document = new GenericDocument.Builder<>("namespace", "id1", "testSchema")
                .setPropertyBytes("bytes")
                .setPropertyDocument("document")
                .build();

        AppSearchBatchResult<String, Void> result = checkIsBatchResultSuccess(db.putAsync(
                new PutDocumentsRequest.Builder().addGenericDocuments(document).build()));
        assertThat(result.getSuccesses()).containsExactly("id1", null);
        assertThat(result.getFailures()).isEmpty();

        GetByDocumentIdRequest request = new GetByDocumentIdRequest.Builder("namespace")
                .addIds("id1")
                .build();
        List<GenericDocument> outDocuments = doGet(db, request);
        assertThat(outDocuments).hasSize(1);
        GenericDocument outDocument = outDocuments.get(0);
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.S
                || Build.VERSION.SDK_INT == Build.VERSION_CODES.S_V2) {
            // We fixed b/204677124 in Android T, so in S and S_V2, getByteArray and
            // getDocumentArray will return null if we set empty properties.
            assertThat(outDocument.getPropertyBytesArray("bytes")).isNull();
            assertThat(outDocument.getPropertyDocumentArray("document")).isNull();
        } else {
            assertThat(outDocument.getPropertyBytesArray("bytes")).isEmpty();
            assertThat(outDocument.getPropertyDocumentArray("document")).isEmpty();
        }
    }

    @Override
    @Test
    public void testSetSchema_addIndexedNestedDocumentProperty() throws Exception {
        long appsearchVersionCode = 0;
        PackageManager pm = ApplicationProvider.getApplicationContext().getPackageManager();
        List<ModuleInfo> modules = pm.getInstalledModules(0);
        for (int i = 0; i < modules.size(); ++i) {
            String packageName = modules.get(i).getPackageName();
            if (packageName.equals(APPSEARCH_MAINLINE_MODULE_NAME)) {
                PackageInfo pInfo = pm.getPackageInfo(packageName, PackageManager.MATCH_APEX);
                appsearchVersionCode = pInfo.getLongVersionCode();
            }
        }
        // This is a test for b/291019114. The bug was only fixed in mainline module
        // 'aml_ase_340913000', so this test will fail on any versions below that.
        assumeTrue(appsearchVersionCode >= 340913000);
        super.testSetSchema_addIndexedNestedDocumentProperty();
    }

    @Override
    @Test
    public void testPutLargeDocumentBatch() throws Exception {
        // b/185441119 was fixed in Android T, this test will fail on S_V2 devices and below.
        assumeTrue(Build.VERSION.SDK_INT >= 33);
        super.testPutLargeDocumentBatch();
    }

    @Override
    @Test
    public void testSetSchemaWithIncompatibleNestedSchema() throws Exception {
        // TODO(b/230879098) This bug was fixed in Android T, but will currently fail on S_V2
        // devices and below. However, we could implement a workaround in platform-storage.
        // Implement that workaround and enable on S and S_V2.
        assumeTrue(Build.VERSION.SDK_INT >= 33);
        super.testSetSchemaWithIncompatibleNestedSchema();
    }

    @Override
    @Test
    public void testEmojiSnippet() throws Exception {
        // b/229770338 was fixed in Android T, this test will fail on S_V2 devices and below.
        assumeTrue(Build.VERSION.SDK_INT >= 33);
        super.testEmojiSnippet();
    }

    // TODO(b/256022027) Remove this overridden test once the change to setMaxJoinedResultCount
    // is synced over into framework.
    @Override
    @Test
    public void testSimpleJoin() throws Exception { }

    // TODO(b/256022027) Remove this overridden test once the change to rename
    //  `this.childrenScores()` to `this.childrenRankingSignals()` is synced to udc-dev.
    @Override
    @Test
    public void testQuery_invalidAdvancedRankingWithChildrenRankingSignals() throws Exception { }

    // TODO(b/256022027) Remove this overridden test once the change to rename
    //  `this.childrenScores()` to `this.childrenRankingSignals()` is synced to udc-dev.
    @Override
    @Test
    public void testQuery_advancedRankingWithJoin() throws Exception { }
}
