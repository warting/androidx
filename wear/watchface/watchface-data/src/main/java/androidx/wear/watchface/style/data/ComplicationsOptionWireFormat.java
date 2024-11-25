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

package androidx.wear.watchface.style.data;

import android.graphics.drawable.Icon;

import androidx.annotation.RestrictTo;
import androidx.versionedparcelable.ParcelField;
import androidx.versionedparcelable.VersionedParcelize;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Wire format for {@link
 * androidx.wear.watchface.style.ComplicationsUserStyleSetting.ComplicationsOption}.
 *
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@VersionedParcelize
public class ComplicationsOptionWireFormat extends OptionWireFormat {
    @ParcelField(2)
    public @NonNull CharSequence mDisplayName;

    @ParcelField(3)
    public @Nullable Icon mIcon;

    // WARNING: This class is held in a list and can't change due to flaws in VersionedParcelable.

    /**
     * Great care should be taken to ensure backwards compatibility of the versioned parcelable if
     * {@link ComplicationOverlayWireFormat} is ever extended.
     */
    @ParcelField(100)
    public ComplicationOverlayWireFormat @NonNull [] mComplicationOverlays =
            new ComplicationOverlayWireFormat[0];

    @ParcelField(value = 101, defaultValue = "null")
    public @Nullable List<PerComplicationTypeMargins> mComplicationOverlaysMargins;

    @ParcelField(value = 102, defaultValue = "null")
    public @Nullable List<Integer> mComplicationNameResourceIds;

    @ParcelField(value = 103, defaultValue = "null")
    public @Nullable List<Integer> mComplicationScreenReaderNameResourceIds;

    ComplicationsOptionWireFormat() {}

    public ComplicationsOptionWireFormat(
            byte @NonNull [] id,
            @NonNull CharSequence displayName,
            @Nullable Icon icon,
            ComplicationOverlayWireFormat @NonNull [] complicationOverlays,
            @Nullable List<PerComplicationTypeMargins> complicationOverlaysMargins,
            @Nullable List<Integer> complicationNameResourceIds,
            @Nullable List<Integer> complicationScreenReaderNameResourceIds) {
        super(id);
        mDisplayName = displayName;
        mIcon = icon;
        mComplicationOverlays = complicationOverlays;
        mComplicationOverlaysMargins = complicationOverlaysMargins;
        mComplicationNameResourceIds = complicationNameResourceIds;
        mComplicationScreenReaderNameResourceIds = complicationScreenReaderNameResourceIds;
    }

    /**
     * @deprecated Use a constructor with perComplicationTypeMargins instead.
     */
    @Deprecated
    public ComplicationsOptionWireFormat(
            byte @NonNull [] id,
            @NonNull CharSequence displayName,
            @Nullable Icon icon,
            ComplicationOverlayWireFormat @NonNull [] complicationOverlays) {
        super(id);
        mDisplayName = displayName;
        mIcon = icon;
        mComplicationOverlays = complicationOverlays;
    }
}
