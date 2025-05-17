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

package androidx.window.layout.adapter.extensions

import android.content.Context
import androidx.core.util.Consumer
import androidx.window.layout.SupportedPosture
import androidx.window.layout.WindowLayoutInfo
import androidx.window.layout.adapter.WindowBackend
import java.util.concurrent.Executor

internal open class ExtensionWindowBackendApi0 : WindowBackend {

    override fun registerLayoutChangeCallback(
        context: Context,
        executor: Executor,
        callback: Consumer<WindowLayoutInfo>,
    ) {
        executor.execute { callback.accept(WindowLayoutInfo(emptyList())) }
    }

    override fun unregisterLayoutChangeCallback(callback: Consumer<WindowLayoutInfo>) {
        // empty implementation since there are no consumers
    }

    override val supportedPostures: List<SupportedPosture>
        get() = throw UnsupportedOperationException("Extensions version must be at least 6")

    override fun getCurrentWindowLayoutInfo(context: Context): WindowLayoutInfo =
        throw UnsupportedOperationException("Extensions version must be at least 9")
}
