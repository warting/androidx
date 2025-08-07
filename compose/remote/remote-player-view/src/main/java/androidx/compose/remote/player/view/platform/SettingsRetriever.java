/*
 * Copyright (C) 2025 The Android Open Source Project
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
package androidx.compose.remote.player.view.player.platform;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;

import android.content.Context;
import android.provider.Settings;

import androidx.annotation.RestrictTo;

/** Class to retrieve values from {@link Settings}. */
@RestrictTo(LIBRARY_GROUP)
public class SettingsRetriever {
    /** Determines whether the Remove Animations accessibility setting is enabled. */
    public static Boolean animationsEnabled(Context context) {
        return !(Settings.Global.getFloat(
                        context.getContentResolver(), Settings.Global.ANIMATOR_DURATION_SCALE, 1.0f)
                == 0f);
    }
}
