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

package androidx.camera.view.internal.compat.quirk;

import androidx.camera.core.impl.Quirk;
import androidx.camera.core.impl.QuirkSettings;

import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Loads all device specific quirks required for the current device
 */
public class DeviceQuirksLoader {

    private DeviceQuirksLoader() {
    }

    /**
     * Goes through all defined device-specific quirks, and returns those that should be loaded
     * on the current device.
     */
    static @NonNull List<Quirk> loadQuirks(@NonNull QuirkSettings quirkSettings) {
        final List<Quirk> quirks = new ArrayList<>();

        if (quirkSettings.shouldEnableQuirk(SurfaceViewStretchedQuirk.class,
                SurfaceViewStretchedQuirk.load())) {
            quirks.add(new SurfaceViewStretchedQuirk());
        }

        if (quirkSettings.shouldEnableQuirk(SurfaceViewNotCroppedByParentQuirk.class,
                SurfaceViewNotCroppedByParentQuirk.load())) {
            quirks.add(new SurfaceViewNotCroppedByParentQuirk());
        }

        return quirks;
    }
}
