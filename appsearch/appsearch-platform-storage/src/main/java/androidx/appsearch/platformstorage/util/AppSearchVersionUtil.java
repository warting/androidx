/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.appsearch.platformstorage.util;

import android.content.Context;
import android.content.pm.ModuleInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.annotation.ChecksSdkIntAtLeast;
import androidx.annotation.DoNotInline;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.core.util.Preconditions;

import org.jspecify.annotations.NonNull;

/**
 * Utilities for retrieving platform AppSearch's module version code.
 *
 * @exportToFramework:hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class AppSearchVersionUtil {
    public static class TExtensionVersions {
        public static final int U_BASE = 7;
        public static final int M2023_11 = 10;
        public static final int V_BASE = 13;
    }
    public static final long APPSEARCH_U_BASE_VERSION_CODE = 340800000;
    public static final long APPSEARCH_M2023_11_VERSION_CODE = 341113000;
    public static final long APPSEARCH_V_BASE_VERSION_CODE = 350700000;
    public static final long APPSEARCH_M2024_11_VERSION_CODE = 351112060;

    private static final String APPSEARCH_MODULE_NAME = "com.android.appsearch";

    // This will be set to -1 to indicate the AppSearch version code hasn't been checked, then to
    // 0 if it is not found, or the version code if it is found.
    private static volatile long sAppSearchVersionCode = -1;

    private AppSearchVersionUtil() {
    }

    /**
     * Returns AppSearch's version code from the context.
     */
    @RequiresApi(Build.VERSION_CODES.Q)
    public static long getAppSearchVersionCode(@NonNull Context context) {
        Preconditions.checkNotNull(context);
        if (sAppSearchVersionCode != -1) {
            return sAppSearchVersionCode;
        }
        synchronized (AppSearchVersionUtil.class) {
            // Check again in case it was assigned while waiting
            if (sAppSearchVersionCode == -1) {
                long appsearchVersionCode = 0;
                try {
                    PackageManager packageManager = context.getPackageManager();
                    String appSearchPackageName =
                            ApiHelperForQ.getAppSearchPackageName(packageManager);
                    if (appSearchPackageName != null) {
                        PackageInfo pInfo = packageManager
                                .getPackageInfo(appSearchPackageName, PackageManager.MATCH_APEX);
                        appsearchVersionCode = ApiHelperForQ.getPackageInfoLongVersionCode(pInfo);
                    }
                } catch (PackageManager.NameNotFoundException e) {
                    // Module not installed
                }
                sAppSearchVersionCode = appsearchVersionCode;
            }
        }
        return sAppSearchVersionCode;
    }

    // TODO(b/326656531): Remove this function once BuildCompat#isAtLeastB is available.
    @ChecksSdkIntAtLeast(api = 36, codename = "Baklava")
    public static boolean isAtLeastB() {
        return Build.VERSION.SDK_INT >= 36
                || (Build.VERSION.SDK_INT >= 35 && Build.VERSION.CODENAME.equals("Baklava"));
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private static class ApiHelperForQ {
        @DoNotInline
        static long getPackageInfoLongVersionCode(PackageInfo pInfo) {
            return pInfo.getLongVersionCode();
        }

        @DoNotInline
        static String getAppSearchPackageName(PackageManager packageManager)
                throws PackageManager.NameNotFoundException {
            ModuleInfo appSearchModule =
                    packageManager.getModuleInfo(APPSEARCH_MODULE_NAME, 1);
            return appSearchModule.getPackageName();
        }
    }
}
