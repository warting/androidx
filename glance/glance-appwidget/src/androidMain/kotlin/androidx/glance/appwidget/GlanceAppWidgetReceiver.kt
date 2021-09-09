/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.glance.appwidget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.os.Bundle
import androidx.annotation.CallSuper
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll

/**
 * [AppWidgetProvider] using the given [GlanceAppWidget] to generate the remote views when needed.
 *
 * This should typically used as:
 *
 *     class MyGlanceAppWidgetProvider : GlanceAppWidgetProvider() {
 *       override val glanceAppWidget: GlanceAppWidget()
 *         get() = MyGlanceAppWidget()
 *     }
 *
 * Note: If you override any of the [AppWidgetProvider] methods, ensure you call their super-class
 * implementation.
 */
abstract class GlanceAppWidgetReceiver : AppWidgetProvider() {
    /**
     * Instance of the [GlanceAppWidget] to use to generate the App Widget and send it to the
     * [AppWidgetManager]
     */
    abstract val glanceAppWidget: GlanceAppWidget

    @CallSuper
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        goAsync {
            appWidgetIds.map { async { glanceAppWidget.update(context, appWidgetManager, it) } }
                .awaitAll()
        }
    }

    @CallSuper
    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle
    ) {
        goAsync {
            glanceAppWidget.resize(context, appWidgetManager, appWidgetId, newOptions)
        }
    }
}