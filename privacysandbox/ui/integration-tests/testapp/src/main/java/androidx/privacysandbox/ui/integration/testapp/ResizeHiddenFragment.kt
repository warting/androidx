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

package androidx.privacysandbox.ui.integration.testapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.privacysandbox.ui.client.view.SandboxedSdkView

class ResizeHiddenFragment : BaseHiddenFragment() {
    private lateinit var resizableBannerView: SandboxedSdkView
    private lateinit var inflatedView: View

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        inflatedView = inflater.inflate(R.layout.fragment_resize_hidden, container, false)
        resizableBannerView = inflatedView.findViewById(R.id.hidden_resizable_ad_view)
        return inflatedView
    }
}
