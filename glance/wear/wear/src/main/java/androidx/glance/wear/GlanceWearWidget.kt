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

package androidx.glance.wear

import android.content.ComponentName
import android.content.Context
import androidx.annotation.MainThread
import androidx.glance.wear.data.WidgetUpdateClient
import androidx.glance.wear.data.WidgetUpdateClientImpl

/**
 * Object that handles providing the contents of a Wear Widget.
 *
 * The widget UI is defined by the content in [WearWidgetContent], provided in the implementation of
 * [provideWidgetContent].
 *
 * An implementation of this class can be associated with a [GlanceWearWidgetService] for receiving
 * content requests and events from the Host.
 */
public abstract class GlanceWearWidget
internal constructor(private val updateClient: WidgetUpdateClient) {

    public constructor() : this(WidgetUpdateClientImpl())

    /**
     * Override this method to provide the contents for this Widget.
     *
     * This method is called from the main thread.
     *
     * @param context the context from which this method is called
     * @param request provides parameters for the contents being requested
     */
    @MainThread
    public abstract suspend fun provideWidgetContent(
        context: Context,
        request: WearWidgetRequest,
    ): WearWidgetContent

    /**
     * Notify the host that it should fetch a new layout for this widget.
     *
     * <p>If sdk version is API 36 or lower, or the widget [instanceId] is invalid (i.e. doesn't
     * exist or is not owned by your package); [instanceId] will be ignored and an update will be
     * triggered for all widgets associated with the [provider] service component.
     *
     * @param context the context from which this method is called.
     * @param provider the component name of the widget provider service to request an update from.
     * @param instanceId the id of the active widget to request an update from.
     *
     * TODO: Add push update flow for newer SDK version.
     * TODO: Provide a default mechanism for storing instanceId
     */
    public fun triggerUpdate(context: Context, provider: ComponentName, instanceId: Int) {
        updateClient.requestUpdate(context, provider, instanceId)
    }
}
