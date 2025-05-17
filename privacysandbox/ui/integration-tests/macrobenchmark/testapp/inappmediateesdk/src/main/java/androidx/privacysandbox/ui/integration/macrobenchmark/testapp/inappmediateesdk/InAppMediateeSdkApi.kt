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

package androidx.privacysandbox.ui.integration.macrobenchmark.testapp.inappmediateesdk

import android.content.Context
import android.os.Bundle
import androidx.privacysandbox.ui.integration.macrobenchmark.testapp.sdkproviderutils.MediateeSdkApiImpl
import androidx.privacysandbox.ui.integration.macrobenchmark.testapp.sdkproviderutils.SdkApiConstants.Companion.AdFormat
import androidx.privacysandbox.ui.integration.macrobenchmark.testapp.sdkproviderutils.SdkApiConstants.Companion.AdType

class InAppMediateeSdkApi(private val context: Context) {
    fun loadAd(
        @AdFormat adFormat: Int,
        @AdType adType: Int,
        withSlowDraw: Boolean,
        drawViewability: Boolean,
    ): Bundle {
        return MediateeSdkApiImpl.loadAdUtil(
            adFormat,
            adType,
            withSlowDraw,
            drawViewability,
            context,
        )
    }
}
