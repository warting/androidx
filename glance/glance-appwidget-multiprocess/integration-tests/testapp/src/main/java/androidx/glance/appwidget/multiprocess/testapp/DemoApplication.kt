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

package androidx.glance.appwidget.multiprocess.testapp

import android.app.Application
import android.os.StrictMode
import androidx.work.Configuration

class DemoApplication : Application(), Configuration.Provider {
    override fun onCreate() {
        StrictMode.enableDefaults()
        super.onCreate()
    }

    override val workManagerConfiguration: Configuration = Configuration.Builder().build()
}
