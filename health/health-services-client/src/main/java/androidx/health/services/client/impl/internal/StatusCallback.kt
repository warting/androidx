/*
 * Copyright (C) 2021 The Android Open Source Project
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

package androidx.health.services.client.impl.internal

import android.os.RemoteException
import androidx.annotation.CallSuper
import androidx.annotation.RestrictTo
import com.google.common.util.concurrent.SettableFuture
import java.lang.SecurityException
/**
 * A generic callback for ipc invocations.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
internal open class StatusCallback(private val resultFuture: SettableFuture<Void?>) :
    IStatusCallback.Stub() {

    @Throws(RemoteException::class)
    @CallSuper
    override fun onSuccess() {
        resultFuture.set(null)
    }

    @Throws(RuntimeException::class, SecurityException::class)
    @CallSuper
    override fun onFailure(msg: String) {
        if (msg.startsWith("Missing permissions"))
            resultFuture.setException(SecurityException(msg))
        else
            resultFuture.setException(RuntimeException(msg))
    }
}
