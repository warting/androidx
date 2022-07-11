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
package androidx.car.app;

import static java.lang.annotation.RetentionPolicy.SOURCE;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.DoNotInline;
import androidx.annotation.IntDef;
import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.car.app.annotations.CarProtocol;
import androidx.car.app.annotations.ExperimentalCarApi;
import androidx.car.app.annotations.RequiresCarApi;
import androidx.car.app.model.Template;
import androidx.car.app.navigation.model.NavigationTemplate;
import androidx.car.app.serialization.Bundleable;
import androidx.car.app.serialization.BundlerException;
import androidx.car.app.versioning.CarAppApiLevel;
import androidx.car.app.versioning.CarAppApiLevels;

import com.google.common.collect.ImmutableSet;

import java.lang.annotation.Retention;
import java.util.Objects;
import java.util.Set;

/** Information about a {@link Session}, such as the physical display and the session ID. */
@RequiresCarApi(5)
@CarProtocol
public class SessionInfo {
    private static final char DIVIDER = '/';

    /** The key for a {@link Bundleable} extra containing the {@link SessionInfo} for a bind. */
    public static final String EXTRA_SESSION_INFO = "androidx.car.app.extra.SESSION_INFO";

    /** The primary infotainment display usually in the center column of the vehicle. */
    public static final int DISPLAY_TYPE_MAIN = 0;

    /** The cluster display, usually located behind the steering wheel. */
    public static final int DISPLAY_TYPE_CLUSTER = 1;

    private static final ImmutableSet<Class<? extends Template>> CLUSTER_SUPPORTED_TEMPLATES_API_5 =
            ImmutableSet.of(NavigationTemplate.class);
    private static final ImmutableSet<Class<? extends Template>>
            CLUSTER_SUPPORTED_TEMPLATES_LESS_THAN_API_5 = ImmutableSet.of();

    /**
     * @hide
     */
    @IntDef({DISPLAY_TYPE_MAIN, DISPLAY_TYPE_CLUSTER})
    @Retention(SOURCE)
    public @interface DisplayType {
    }

    /**
     * A default {@link SessionInfo} for the main display, used when the host is on a version
     * that doesn't support this new class.
     */
    @NonNull
    public static final SessionInfo DEFAULT_SESSION_INFO = new SessionInfo(
            DISPLAY_TYPE_MAIN, "main");

    /** A string identifier unique per physical display. */
    @Keep
    @NonNull
    private final String mSessionId;

    /** The type of display the {@link Session} is rendering on. */
    @Keep
    @DisplayType
    private final int mDisplayType;

    /**
     * Returns a session-stable ID, unique to the display that the {@link Session} is rendering on.
     */
    @NonNull
    public String getSessionId() {
        return mSessionId;
    }

    /** Returns the type of display that the {@link Session} is rendering on. */
    @DisplayType
    public int getDisplayType() {
        return mDisplayType;
    }

    /**
     * Creates a new {@link SessionInfo} with the provided {@code displayType} and {@code
     * sessionId}.
     */
    public SessionInfo(@DisplayType int displayType, @NonNull String sessionId) {
        mDisplayType = displayType;
        mSessionId = sessionId;
    }

    /**
     * Creates a new {@link SessionInfo} for a given bind {@code intent}
     */
    @SuppressWarnings("deprecation")
    public SessionInfo(@NonNull Intent intent) {
        Bundle extras = intent.getExtras();
        if (extras == null) {
            throw new IllegalArgumentException(
                    "Expected the SessionInfo to be encoded in the bind intent extras, but the "
                            + "extras were null.");
        }

        Bundleable sessionInfoBundleable = extras.getParcelable(EXTRA_SESSION_INFO);
        if (sessionInfoBundleable == null) {
            throw new IllegalArgumentException(
                    "Expected the SessionInfo to be encoded in the bind intent extras, but they "
                            + "couldn't be found in the extras.");
        }

        try {
            SessionInfo info = (SessionInfo) sessionInfoBundleable.get();
            this.mSessionId = info.mSessionId;
            this.mDisplayType = info.mDisplayType;
        } catch (BundlerException e) {
            throw new IllegalArgumentException(
                    "Expected the SessionInfo to be encoded in the bind intent extras, but they "
                            + "were encoded improperly", e);
        }
    }

    /**
     * Adds the {@link SessionInfo} and associated identifier on the passed {@code intent} to
     * pass to this service on bind.
     */
    public static void setBindData(@NonNull Intent intent, @NonNull SessionInfo sessionInfo) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Api29.setIdentifier(intent, sessionInfo.toString());
        } else {
            intent.setData(new Uri.Builder().path(sessionInfo.toString()).build());
        }
        try {
            intent.putExtra(EXTRA_SESSION_INFO, Bundleable.create(sessionInfo));
        } catch (BundlerException e) {
            throw new RuntimeException(e);
        }
    }

    // Required for Bundler
    private SessionInfo() {
        mSessionId = "main";
        mDisplayType = DISPLAY_TYPE_MAIN;
    }

    /**
     * Returns the set of templates that are allowed for this {@link Session}, or {@code null} if
     * there are no restrictions (ie. all templates are allowed).
     */
    @Nullable
    @SuppressWarnings("NullableCollection") // Set does not contain nulls
    @ExperimentalCarApi
    public Set<Class<? extends Template>> getSupportedTemplates(
            @CarAppApiLevel int carAppApiLevel) {
        if (mDisplayType == DISPLAY_TYPE_CLUSTER) {
            if (carAppApiLevel >= CarAppApiLevels.LEVEL_5) {
                return CLUSTER_SUPPORTED_TEMPLATES_API_5;
            }

            return CLUSTER_SUPPORTED_TEMPLATES_LESS_THAN_API_5;
        }

        return null;
    }

    @NonNull
    @Override
    public String toString() {
        return String.valueOf(mDisplayType) + DIVIDER + mSessionId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(mSessionId, mDisplayType);
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof SessionInfo)) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        SessionInfo object = (SessionInfo) obj;
        return this.getSessionId().equals(object.getSessionId())
                && this.getDisplayType() == object.getDisplayType();
    }

    /** Android Q method calls wrapped in a {@link RequiresApi} class to appease the compiler. */
    @RequiresApi(Build.VERSION_CODES.Q)
    private static class Api29 {
        // Not instantiable
        private Api29() {
        }

        /** Wrapper for {@link Intent#getIdentifier()}. */
        @DoNotInline
        @Nullable
        static String getIdentifier(@NonNull Intent intent) {
            return intent.getIdentifier();
        }

        /** Wrapper for {@link Intent#setIdentifier(String)}. */
        @DoNotInline
        static void setIdentifier(@NonNull Intent intent, @NonNull String identifier) {
            intent.setIdentifier(identifier);
        }
    }
}
