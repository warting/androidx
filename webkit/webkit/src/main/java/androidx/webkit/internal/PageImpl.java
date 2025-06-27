/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.webkit.internal;

import androidx.webkit.Page;
import androidx.webkit.WebNavigationClient;

import org.chromium.support_lib_boundary.WebViewPageBoundaryInterface;
import org.jspecify.annotations.NonNull;

@WebNavigationClient.ExperimentalNavigationCallback
public class PageImpl implements Page {
    private final WebViewPageBoundaryInterface mPageBoundaryInterface;

    public PageImpl(@NonNull WebViewPageBoundaryInterface impl) {
        mPageBoundaryInterface = impl;
    }

}
