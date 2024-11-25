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

package android.support.wearable.watchface.accessibility;

import android.app.PendingIntent;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.wearable.complications.TimeDependentText;
import android.support.wearable.watchface.Constants;

import androidx.annotation.RestrictTo;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

/** Holds labels for screen regions which should respond to accessibility events. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@SuppressWarnings("BanParcelableUsage")
public final class ContentDescriptionLabel implements Parcelable {

    @SuppressWarnings("unused")
    public static final Parcelable.Creator<ContentDescriptionLabel> CREATOR =
            new Parcelable.Creator<ContentDescriptionLabel>() {
                @Override
                public @NonNull ContentDescriptionLabel createFromParcel(@NonNull Parcel in) {
                    return new ContentDescriptionLabel(in);
                }

                @Override
                public ContentDescriptionLabel @NonNull [] newArray(int size) {
                    return new ContentDescriptionLabel[size];
                }
            };

    private final TimeDependentText mText;
    private final Rect mBounds;
    private PendingIntent mTapAction;

    /**
     * Creates a new ContentDescriptionLabel without a tap action.
     *
     * @param bounds absolute coordinates of where this label should appear on the screen
     * @param text time-dependent text describing this label
     */
    public ContentDescriptionLabel(@NonNull Rect bounds, @NonNull TimeDependentText text) {
        this.mBounds = bounds;
        this.mText = text;
    }

    /**
     * Creates a new ContentDescriptionLabel with optional tap action.
     *
     * @param bounds absolute coordinates of where this label should appear on the screen
     * @param text time-dependent text describing this label
     * @param tapAction the tap action
     */
    public ContentDescriptionLabel(
            @NonNull Rect bounds,
            @NonNull TimeDependentText text,
            @Nullable PendingIntent tapAction) {
        this(bounds, text);
        this.mTapAction = tapAction;
    }

    @SuppressWarnings("deprecation")
    protected ContentDescriptionLabel(@NonNull Parcel in) {
        Bundle bundle = in.readBundle(getClass().getClassLoader());
        mText = bundle.getParcelable(Constants.KEY_TEXT);
        mBounds = bundle.getParcelable(Constants.KEY_BOUNDS);
        mTapAction = bundle.getParcelable(Constants.KEY_TAP_ACTION);
    }

    /** Returns the absolute coordinates of where this label should appear on the screen. */
    public @NonNull Rect getBounds() {
        return mBounds;
    }

    /** Returns the {@link TimeDependentText} describing this label. */
    public @NonNull TimeDependentText getText() {
        return mText;
    }

    /** Returns the optional {@link PendingIntent} to launch when this label is tapped. */
    public @Nullable PendingIntent getTapAction() {
        return mTapAction;
    }

    /**
     * Sets an optional {@link PendingIntent} which is launched when the label is tapped by the
     * user.
     *
     * <p>Normally, you do not need to use this, since when a Talkback user taps on a
     * ContentDescriptionLabel, it will pass the tap through to your WatchFaceService's {@link
     * androidx.wear.watchface.WatchFace#onTapCommand} method, where you can treat it as a normal
     * tap.
     *
     * <p>This is used internally when a watchface does not provide ContentDescriptionLabels, and
     * thus the system has to generate automatic labels that may not line up with the visual
     * location of the complication on the watchface. Since a tap would not be passed through to the
     * correct location, the system will need to handle the taps itself.
     *
     * @param tapAction launched when the user taps this label
     */
    public void setTapAction(@Nullable PendingIntent tapAction) {
        this.mTapAction = tapAction;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        Bundle bundle = new Bundle();
        bundle.putParcelable(Constants.KEY_TEXT, mText);
        bundle.putParcelable(Constants.KEY_BOUNDS, mBounds);
        bundle.putParcelable(Constants.KEY_TAP_ACTION, mTapAction);
        dest.writeBundle(bundle);
    }

    @Override
    public @NonNull String toString() {
        return "ContentDescriptionLabel{text="
                + mText
                + ", bounds="
                + mBounds
                + ", tapAction="
                + mTapAction
                + '}';
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ContentDescriptionLabel that = (ContentDescriptionLabel) o;
        return Objects.equals(mText, that.mText)
                && Objects.equals(mBounds, that.mBounds)
                && Objects.equals(mTapAction, that.mTapAction);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mText, mBounds, mTapAction);
    }
}
