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

package androidx.appcompat.res

import com.android.tools.lint.client.api.TYPE_INT
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity

// Flags usage of Resources.getColorStateList and suggests converting it to either
// ContextCompat.getColorStateList or AppCompatResources.getColorStateList based
// on the API level
@Suppress("UnstableApiUsage")
class ColorStateListLoadingDetector : BaseMethodDeprecationDetector(
    NOT_USING_COMPAT_LOADING,
    // Suggest using ContextCompat.getColorStateList at API > 23
    DeprecationCondition(
        MethodLocation("android.content.res.Resources", "getColorStateList", TYPE_INT),
        "Use `ContextCompat.getColorStateList()`",
        ApiAbove(23)
    ),
    // Suggest using AppCompatResources.getColorStateList at API <= 23
    DeprecationCondition(
        MethodLocation("android.content.res.Resources", "getColorStateList", TYPE_INT),
        "Use `AppCompatResources.getColorStateList()`",
        ApiAtOrBelow(23)
    )
) {
    companion object {
        internal val NOT_USING_COMPAT_LOADING: Issue = Issue.create(
            "UseCompatLoadingForColorStateLists",
            "Should not call `Resources.getColorStateList` directly",
            "Use Compat loading of color state lists",
            Category.CORRECTNESS,
            1,
            Severity.WARNING,
            Implementation(ColorStateListLoadingDetector::class.java, Scope.JAVA_FILE_SCOPE)
        )
    }
}