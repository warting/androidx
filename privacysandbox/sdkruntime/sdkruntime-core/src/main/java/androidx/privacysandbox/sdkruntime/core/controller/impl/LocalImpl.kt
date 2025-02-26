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

package androidx.privacysandbox.sdkruntime.core.controller.impl

import android.content.Context
import android.os.Bundle
import android.os.IBinder
import androidx.privacysandbox.sdkruntime.core.AppOwnedSdkSandboxInterfaceCompat
import androidx.privacysandbox.sdkruntime.core.SandboxedSdkCompat
import androidx.privacysandbox.sdkruntime.core.SdkSandboxClientImportanceListenerCompat
import androidx.privacysandbox.sdkruntime.core.activity.SdkSandboxActivityHandlerCompat
import androidx.privacysandbox.sdkruntime.core.controller.LoadSdkCallback
import androidx.privacysandbox.sdkruntime.core.controller.SdkSandboxControllerCompat
import androidx.privacysandbox.sdkruntime.core.internal.ClientFeature
import java.util.concurrent.Executor

/**
 * Wrapper for client provided implementation of [SdkSandboxControllerCompat]. Checks client version
 * to determine if method supported.
 */
internal class LocalImpl(
    private val implFromClient: SdkSandboxControllerCompat.SandboxControllerImpl,
    private val sdkContext: Context,
    private val clientVersion: Int
) : SdkSandboxControllerCompat.SandboxControllerImpl {

    override fun loadSdk(
        sdkName: String,
        params: Bundle,
        executor: Executor,
        callback: LoadSdkCallback
    ) {
        implFromClient.loadSdk(sdkName, params, executor, callback)
    }

    override fun getSandboxedSdks(): List<SandboxedSdkCompat> {
        return implFromClient.getSandboxedSdks()
    }

    override fun getAppOwnedSdkSandboxInterfaces(): List<AppOwnedSdkSandboxInterfaceCompat> {
        return implFromClient.getAppOwnedSdkSandboxInterfaces()
    }

    override fun registerSdkSandboxActivityHandler(
        handlerCompat: SdkSandboxActivityHandlerCompat
    ): IBinder {
        return implFromClient.registerSdkSandboxActivityHandler(handlerCompat)
    }

    override fun unregisterSdkSandboxActivityHandler(
        handlerCompat: SdkSandboxActivityHandlerCompat
    ) {
        implFromClient.unregisterSdkSandboxActivityHandler(handlerCompat)
    }

    override fun getClientPackageName(): String {
        if (ClientFeature.GET_CLIENT_PACKAGE_NAME.isAvailable(clientVersion)) {
            return implFromClient.getClientPackageName()
        } else {
            /**
             * When loaded locally sdkContext is wrapped Application context. All previously
             * released client library versions returns client app package name.
             *
             * After supporting [ClientFeature.GET_CLIENT_PACKAGE_NAME] it will work correctly for
             * future versions, even if getPackageName() behaviour will be changed for sdk context
             * wrapper.
             */
            return sdkContext.getPackageName()
        }
    }

    override fun registerSdkSandboxClientImportanceListener(
        executor: Executor,
        listenerCompat: SdkSandboxClientImportanceListenerCompat
    ) {
        if (ClientFeature.CLIENT_IMPORTANCE_LISTENER.isAvailable(clientVersion)) {
            implFromClient.registerSdkSandboxClientImportanceListener(executor, listenerCompat)
        }
    }

    override fun unregisterSdkSandboxClientImportanceListener(
        listenerCompat: SdkSandboxClientImportanceListenerCompat
    ) {
        if (ClientFeature.CLIENT_IMPORTANCE_LISTENER.isAvailable(clientVersion)) {
            implFromClient.unregisterSdkSandboxClientImportanceListener(listenerCompat)
        }
    }
}
