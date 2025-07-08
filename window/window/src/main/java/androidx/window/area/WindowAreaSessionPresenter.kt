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
import androidx.window.core.ExperimentalWindowApi

/**
 * A container that allows getting access to and showing content on a window area. The container is
 * provided from [WindowAreaPresentationSessionCallback] when a requested session becomes active.
 * The presentation can be automatically dismissed by the system when the user leaves the primary
 * application window, or can be closed by calling [WindowAreaSessionPresenter.close].
 *
 * @see WindowAreaController.presentContentOnWindowArea
 */
@ExperimentalWindowApi
public interface WindowAreaSessionPresenter : WindowAreaSession {
    /** Returns the [Context] associated with the window area. */
    public val context: Context

    /**
     * Returns the [Window] associated with the active presentation window area or null if there is
     * no [Window] currently active. This could occur if the presenter has already been dismissed,
     * and there is no expectation that the [Window] would become non-null at a later point. This
     * API can be used to directly access parts of the [Window] API that are not available through
     * the [Context] provided.
     */
    public val window: Window?

    /**
     * Sets a [View] to show on a window area. After setting the view the system can turn on the
     * corresponding display and start showing content.
     */
    public fun setContentView(view: View)
}
