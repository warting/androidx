/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.wear.watchface.style.data;

import android.graphics.drawable.Icon;
import android.os.Bundle;

import androidx.annotation.RestrictTo;
import androidx.versionedparcelable.VersionedParcelize;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Wire format for {@link androidx.wear.watchface.style.CustomValueStyleSetting2}.
 *
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@VersionedParcelize
public class CustomValueUserStyleSetting2WireFormat extends UserStyleSettingWireFormat {

    CustomValueUserStyleSetting2WireFormat() {}

    /**
     * @deprecated use a constructor with List<Bundle> perOptionOnWatchFaceEditorBundles.
     */
    @Deprecated
    public CustomValueUserStyleSetting2WireFormat(
            @NonNull String id,
            @NonNull CharSequence displayName,
            @NonNull CharSequence description,
            @Nullable Icon icon,
            @NonNull List<OptionWireFormat> options,
            @NonNull List<Integer> affectsLayers) {
        super(id, displayName, description, icon, options, 0, affectsLayers);
    }

    public CustomValueUserStyleSetting2WireFormat(
            @NonNull String id,
            @NonNull CharSequence displayName,
            @NonNull CharSequence description,
            @Nullable Icon icon,
            @NonNull List<OptionWireFormat> options,
            @NonNull List<Integer> affectsLayers,
            @Nullable Bundle onWatchFaceEditorBundle,
            @Nullable List<Bundle> perOptionOnWatchFaceEditorBundles) {
        super(
                id,
                displayName,
                description,
                icon,
                options,
                0,
                affectsLayers,
                onWatchFaceEditorBundle,
                perOptionOnWatchFaceEditorBundles);
    }
}
