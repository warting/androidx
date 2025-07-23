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

package androidx.privacysandbox.sdkruntime.testsdk

import android.content.Context
import android.os.Binder
import android.os.Bundle
import android.os.Looper
import androidx.privacysandbox.sdkruntime.core.LoadSdkCompatException
import androidx.privacysandbox.sdkruntime.core.SandboxedSdkCompat
import androidx.privacysandbox.sdkruntime.core.SandboxedSdkProviderCompat
import androidx.privacysandbox.sdkruntime.provider.controller.SdkSandboxControllerCompat
import java.lang.IllegalStateException
import kotlinx.coroutines.runBlocking

@Suppress("unused") // Reflection usage from tests in privacysandbox:sdkruntime:sdkruntime-client
class CompatProvider : SandboxedSdkProviderCompat() {
    @JvmField var onLoadSdkBinder: Binder? = null

    @JvmField var lastOnLoadSdkParams: Bundle? = null

    @JvmField var isBeforeUnloadSdkCalled = false

    @Throws(LoadSdkCompatException::class)
    override fun onLoadSdk(params: Bundle): SandboxedSdkCompat {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            throw IllegalStateException("onLoadSdk() must be called from main thread")
        }
        val dependencySdkToLoad = params.getString("dependencySdkToLoad")
        if (dependencySdkToLoad != null) {
            val controller = SdkSandboxControllerCompat.from(context!!)
            runBlocking { controller.loadSdk(dependencySdkToLoad, Bundle()) }
        }
        val result = SdkImpl(context!!)
        onLoadSdkBinder = result

        lastOnLoadSdkParams = params
        if (params.getBoolean("needFail", false)) {
            throw LoadSdkCompatException(RuntimeException("Expected to fail"), params)
        }
        if (params.getBoolean("needFailWithRuntimeException", false)) {
            throw RuntimeException("Expected to fail")
        }
        return SandboxedSdkCompat(result)
    }

    override fun beforeUnloadSdk() {
        isBeforeUnloadSdkCalled = true
    }

    internal class SdkImpl(
        @Suppress("MemberVisibilityCanBePrivate") // Reflection usage from LocalSdkTestUtils
        val context: Context
    ) : Binder()
}
