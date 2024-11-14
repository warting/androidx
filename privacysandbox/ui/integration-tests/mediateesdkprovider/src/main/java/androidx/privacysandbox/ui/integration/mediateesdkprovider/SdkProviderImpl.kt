/*
 * Copyright (C) 2024 The Android Open Source Project
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

package androidx.privacysandbox.ui.integration.mediateesdkprovider

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.privacysandbox.sdkruntime.core.SandboxedSdkCompat
import androidx.privacysandbox.sdkruntime.core.SandboxedSdkProviderCompat
import androidx.privacysandbox.ui.integration.sdkproviderutils.MediateeSdkApiImpl

class SdkProviderImpl : SandboxedSdkProviderCompat() {
    override fun onLoadSdk(params: Bundle): SandboxedSdkCompat {
        return SandboxedSdkCompat(MediateeSdkApiImpl(context!!))
    }

    override fun getView(windowContext: Context, params: Bundle, width: Int, height: Int): View {
        throw IllegalStateException("This getView method will not be used.")
    }
}
