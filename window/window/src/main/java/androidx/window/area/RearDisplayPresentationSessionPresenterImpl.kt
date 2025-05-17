/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.window.area

import android.content.Context
import android.view.View
import android.view.Window
import androidx.window.area.utils.PresentationWindowCompatUtils
import androidx.window.core.ExperimentalWindowApi
import androidx.window.extensions.area.ExtensionWindowAreaPresentation
import androidx.window.extensions.area.WindowAreaComponent

@ExperimentalWindowApi
internal class RearDisplayPresentationSessionPresenterImpl(
    private val windowAreaComponent: WindowAreaComponent,
    private val presentation: ExtensionWindowAreaPresentation,
    vendorApiLevel: Int,
) : WindowAreaSessionPresenter {

    override val context: Context = presentation.presentationContext

    override val window: Window? =
        if (vendorApiLevel >= 4) presentation.window
        else PresentationWindowCompatUtils.getWindowBeforeVendorApiLevel4(presentation)

    override fun setContentView(view: View) {
        presentation.setPresentationView(view)
    }

    override fun close() {
        windowAreaComponent.endRearDisplayPresentationSession()
    }
}
