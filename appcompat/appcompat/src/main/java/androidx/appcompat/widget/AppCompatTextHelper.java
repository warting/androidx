/*
 * Copyright (C) 2015 The Android Open Source Project
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

package androidx.appcompat.widget;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP_PREFIX;
import static androidx.appcompat.widget.ViewUtils.SDK_LEVEL_SUPPORTS_AUTOSIZE;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.fonts.FontStyle;
import android.graphics.fonts.FontVariationAxis;
import android.os.Build;
import android.os.LocaleList;
import android.text.TextUtils;
import android.text.method.PasswordTransformationMethod;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.widget.TextView;

import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.annotation.UiThread;
import androidx.annotation.VisibleForTesting;
import androidx.appcompat.R;
import androidx.collection.LruCache;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.util.Consumer;
import androidx.core.util.TypedValueCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.inputmethod.EditorInfoCompat;
import androidx.core.widget.TextViewCompat;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.lang.ref.WeakReference;
import java.util.Locale;
import java.util.Objects;

class AppCompatTextHelper {

    private static final int TEXT_FONT_WEIGHT_UNSPECIFIED = -1;

    // Enum for the "typeface" XML parameter.
    private static final int SANS = 1;
    private static final int SERIF = 2;
    private static final int MONOSPACE = 3;

    private final @NonNull TextView mView;

    private TintInfo mDrawableLeftTint;
    private TintInfo mDrawableTopTint;
    private TintInfo mDrawableRightTint;
    private TintInfo mDrawableBottomTint;
    private TintInfo mDrawableStartTint;
    private TintInfo mDrawableEndTint;
    private TintInfo mDrawableTint; // Tint used for all compound drawables

    private final @NonNull AppCompatTextViewAutoSizeHelper mAutoSizeTextHelper;

    private int mStyle = Typeface.NORMAL;
    private int mFontWeight = TEXT_FONT_WEIGHT_UNSPECIFIED;
    private Typeface mFontTypeface;
    private @Nullable String mFontVariationSettings = null;
    private boolean mAsyncFontPending;

    AppCompatTextHelper(@NonNull TextView view) {
        mView = view;
        mAutoSizeTextHelper = new AppCompatTextViewAutoSizeHelper(mView);
    }

    @SuppressLint("NewApi")
    void loadFromAttributes(@Nullable AttributeSet attrs, int defStyleAttr) {
        final Context context = mView.getContext();
        final AppCompatDrawableManager drawableManager = AppCompatDrawableManager.get();

        // First read the TextAppearance style id
        TintTypedArray a = TintTypedArray.obtainStyledAttributes(context, attrs,
                R.styleable.AppCompatTextHelper, defStyleAttr, 0);
        ViewCompat.saveAttributeDataForStyleable(mView, mView.getContext(),
                R.styleable.AppCompatTextHelper, attrs, a.getWrappedTypeArray(),
                defStyleAttr, 0);

        final int ap = a.getResourceId(R.styleable.AppCompatTextHelper_android_textAppearance, -1);
        // Now read the compound drawable and grab any tints
        if (a.hasValue(R.styleable.AppCompatTextHelper_android_drawableLeft)) {
            mDrawableLeftTint = createTintInfo(context, drawableManager,
                    a.getResourceId(R.styleable.AppCompatTextHelper_android_drawableLeft, 0));
        }
        if (a.hasValue(R.styleable.AppCompatTextHelper_android_drawableTop)) {
            mDrawableTopTint = createTintInfo(context, drawableManager,
                    a.getResourceId(R.styleable.AppCompatTextHelper_android_drawableTop, 0));
        }
        if (a.hasValue(R.styleable.AppCompatTextHelper_android_drawableRight)) {
            mDrawableRightTint = createTintInfo(context, drawableManager,
                    a.getResourceId(R.styleable.AppCompatTextHelper_android_drawableRight, 0));
        }
        if (a.hasValue(R.styleable.AppCompatTextHelper_android_drawableBottom)) {
            mDrawableBottomTint = createTintInfo(context, drawableManager,
                    a.getResourceId(R.styleable.AppCompatTextHelper_android_drawableBottom, 0));
        }
        if (a.hasValue(R.styleable.AppCompatTextHelper_android_drawableStart)) {
            mDrawableStartTint = createTintInfo(context, drawableManager,
                    a.getResourceId(R.styleable.AppCompatTextHelper_android_drawableStart, 0));
        }
        if (a.hasValue(R.styleable.AppCompatTextHelper_android_drawableEnd)) {
            mDrawableEndTint = createTintInfo(context, drawableManager,
                    a.getResourceId(R.styleable.AppCompatTextHelper_android_drawableEnd, 0));
        }

        a.recycle();

        // PasswordTransformationMethod wipes out all other TransformationMethod instances
        // in TextView's constructor, so we should only set a new transformation method
        // if we don't have a PasswordTransformationMethod currently...
        final boolean hasPwdTm =
                mView.getTransformationMethod() instanceof PasswordTransformationMethod;
        boolean allCaps = false;
        boolean allCapsSet = false;
        ColorStateList textColor = null;
        ColorStateList textColorHint = null;
        ColorStateList textColorLink = null;
        String localeListString = null;

        // First check TextAppearance's textAllCaps value
        if (ap != -1) {
            a = TintTypedArray.obtainStyledAttributes(context, ap, R.styleable.TextAppearance);
            if (!hasPwdTm && a.hasValue(R.styleable.TextAppearance_textAllCaps)) {
                allCapsSet = true;
                allCaps = a.getBoolean(R.styleable.TextAppearance_textAllCaps, false);
            }

            updateTypefaceAndStyle(context, a);
            if (Build.VERSION.SDK_INT < 23) {
                // If we're running on < API 23, the text color may contain theme references
                // so let's re-set using our own inflater
                if (a.hasValue(R.styleable.TextAppearance_android_textColor)) {
                    textColor = a.getColorStateList(R.styleable.TextAppearance_android_textColor);
                }
                if (a.hasValue(R.styleable.TextAppearance_android_textColorHint)) {
                    textColorHint = a.getColorStateList(
                            R.styleable.TextAppearance_android_textColorHint);
                }
                if (a.hasValue(R.styleable.TextAppearance_android_textColorLink)) {
                    textColorLink = a.getColorStateList(
                            R.styleable.TextAppearance_android_textColorLink);
                }
            }
            if (a.hasValue(R.styleable.TextAppearance_textLocale)) {
                localeListString = a.getString(R.styleable.TextAppearance_textLocale);
            }
            a.recycle();
        }

        // Now read the style's values
        a = TintTypedArray.obtainStyledAttributes(context, attrs, R.styleable.TextAppearance,
                defStyleAttr, 0);
        if (!hasPwdTm && a.hasValue(R.styleable.TextAppearance_textAllCaps)) {
            allCapsSet = true;
            allCaps = a.getBoolean(R.styleable.TextAppearance_textAllCaps, false);
        }
        if (Build.VERSION.SDK_INT < 23) {
            // If we're running on < API 23, the text color may contain theme references
            // so let's re-set using our own inflater
            if (a.hasValue(R.styleable.TextAppearance_android_textColor)) {
                textColor = a.getColorStateList(R.styleable.TextAppearance_android_textColor);
            }
            if (a.hasValue(R.styleable.TextAppearance_android_textColorHint)) {
                textColorHint = a.getColorStateList(
                        R.styleable.TextAppearance_android_textColorHint);
            }
            if (a.hasValue(R.styleable.TextAppearance_android_textColorLink)) {
                textColorLink = a.getColorStateList(
                        R.styleable.TextAppearance_android_textColorLink);
            }
        }
        if (a.hasValue(R.styleable.TextAppearance_textLocale)) {
            localeListString = a.getString(R.styleable.TextAppearance_textLocale);
        }

        // In P, when the text size attribute is 0, this would not be set. Fix this here.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P
                && a.hasValue(R.styleable.TextAppearance_android_textSize)) {
            if (a.getDimensionPixelSize(R.styleable.TextAppearance_android_textSize, -1) == 0) {
                mView.setTextSize(TypedValue.COMPLEX_UNIT_PX, 0.0f);
            }
        }

        updateTypefaceAndStyle(context, a);
        a.recycle();

        if (textColor != null) {
            mView.setTextColor(textColor);
        }
        if (textColorHint != null) {
            mView.setHintTextColor(textColorHint);
        }
        if (textColorLink != null) {
            mView.setLinkTextColor(textColorLink);
        }
        if (!hasPwdTm && allCapsSet) {
            setAllCaps(allCaps);
        }

        applyFontAndVariationSettings(/* forceNullSet */ false);

        if (localeListString != null) {
            if (Build.VERSION.SDK_INT >= 24) {
                Api24Impl.setTextLocales(mView, Api24Impl.forLanguageTags(localeListString));
            } else {
                @SuppressWarnings("StringSplitter")
                final String firstLanTag = localeListString.split(",")[0];
                mView.setTextLocale(Api21Impl.forLanguageTag(firstLanTag));
            }
        }

        mAutoSizeTextHelper.loadFromAttributes(attrs, defStyleAttr);

        if (SDK_LEVEL_SUPPORTS_AUTOSIZE) {
            // Delegate auto-size functionality to the framework implementation.
            if (mAutoSizeTextHelper.getAutoSizeTextType()
                    != TextViewCompat.AUTO_SIZE_TEXT_TYPE_NONE) {
                final int[] autoSizeTextSizesInPx =
                        mAutoSizeTextHelper.getAutoSizeTextAvailableSizes();
                if (autoSizeTextSizesInPx.length > 0) {
                    if (Api26Impl.getAutoSizeStepGranularity(mView)
                            != AppCompatTextViewAutoSizeHelper
                            .UNSET_AUTO_SIZE_UNIFORM_CONFIGURATION_VALUE) {
                        // Configured with granularity, preserve details.
                        Api26Impl.setAutoSizeTextTypeUniformWithConfiguration(mView,
                                mAutoSizeTextHelper.getAutoSizeMinTextSize(),
                                mAutoSizeTextHelper.getAutoSizeMaxTextSize(),
                                mAutoSizeTextHelper.getAutoSizeStepGranularity(),
                                TypedValue.COMPLEX_UNIT_PX);
                    } else {
                        Api26Impl.setAutoSizeTextTypeUniformWithPresetSizes(mView,
                                autoSizeTextSizesInPx, TypedValue.COMPLEX_UNIT_PX);
                    }
                }
            }
        }

        // Read line and baseline heights attributes.
        a = TintTypedArray.obtainStyledAttributes(context, attrs, R.styleable.AppCompatTextView);

        // Load compat compound drawables, allowing vector backport
        Drawable drawableLeft = null, drawableTop = null, drawableRight = null,
                drawableBottom = null, drawableStart = null, drawableEnd = null;
        final int drawableLeftId = a.getResourceId(
                R.styleable.AppCompatTextView_drawableLeftCompat, -1);
        if (drawableLeftId != -1) {
            drawableLeft = drawableManager.getDrawable(context, drawableLeftId);
        }
        final int drawableTopId = a.getResourceId(
                R.styleable.AppCompatTextView_drawableTopCompat, -1);
        if (drawableTopId != -1) {
            drawableTop = drawableManager.getDrawable(context, drawableTopId);
        }
        final int drawableRightId = a.getResourceId(
                R.styleable.AppCompatTextView_drawableRightCompat, -1);
        if (drawableRightId != -1) {
            drawableRight = drawableManager.getDrawable(context, drawableRightId);
        }
        final int drawableBottomId = a.getResourceId(
                R.styleable.AppCompatTextView_drawableBottomCompat, -1);
        if (drawableBottomId != -1) {
            drawableBottom = drawableManager.getDrawable(context, drawableBottomId);
        }
        final int drawableStartId = a.getResourceId(
                R.styleable.AppCompatTextView_drawableStartCompat, -1);
        if (drawableStartId != -1) {
            drawableStart = drawableManager.getDrawable(context, drawableStartId);
        }
        final int drawableEndId = a.getResourceId(
                R.styleable.AppCompatTextView_drawableEndCompat, -1);
        if (drawableEndId != -1) {
            drawableEnd = drawableManager.getDrawable(context, drawableEndId);
        }
        setCompoundDrawables(drawableLeft, drawableTop, drawableRight, drawableBottom,
                drawableStart, drawableEnd);

        if (a.hasValue(R.styleable.AppCompatTextView_drawableTint)) {
            final ColorStateList tintList = a.getColorStateList(
                    R.styleable.AppCompatTextView_drawableTint);
            TextViewCompat.setCompoundDrawableTintList(mView, tintList);
        }
        if (a.hasValue(R.styleable.AppCompatTextView_drawableTintMode)) {
            final PorterDuff.Mode tintMode = DrawableUtils.parseTintMode(
                    a.getInt(R.styleable.AppCompatTextView_drawableTintMode, -1), null);
            TextViewCompat.setCompoundDrawableTintMode(mView, tintMode);
        }

        final int firstBaselineToTopHeight = a.getDimensionPixelSize(
                R.styleable.AppCompatTextView_firstBaselineToTopHeight, -1);
        final int lastBaselineToBottomHeight = a.getDimensionPixelSize(
                R.styleable.AppCompatTextView_lastBaselineToBottomHeight, -1);
        float lineHeight = -1;
        int lineHeightUnit = -1;
        if (a.hasValue(R.styleable.AppCompatTextView_lineHeight)) {
            TypedValue peekValue = a.peekValue(R.styleable.AppCompatTextView_lineHeight);
            if (peekValue != null && peekValue.type == TypedValue.TYPE_DIMENSION) {
                lineHeightUnit = TypedValueCompat.getUnitFromComplexDimension(peekValue.data);
                lineHeight = TypedValue.complexToFloat(peekValue.data);
            } else {
                lineHeight = a.getDimensionPixelSize(
                        R.styleable.AppCompatTextView_lineHeight,
                        -1
                );
            }
        }

        a.recycle();
        if (firstBaselineToTopHeight != -1) {
            TextViewCompat.setFirstBaselineToTopHeight(mView, firstBaselineToTopHeight);
        }
        if (lastBaselineToBottomHeight != -1) {
            TextViewCompat.setLastBaselineToBottomHeight(mView, lastBaselineToBottomHeight);
        }
        if (lineHeight != -1) {
            if (lineHeightUnit == -1) {
                TextViewCompat.setLineHeight(mView, (int) lineHeight);
            } else {
                TextViewCompat.setLineHeight(mView, lineHeightUnit, lineHeight);
            }
        }
    }

    /**
     * Apply mFontTypeface previously loaded from XML, and apply mFontVariationSettings to it.
     *
     * This should only be called from xml initialization, or setTextAppearance.
     *
     * @param forceNullSet Explicit null values should clobber existing Typefaces
     */
    private void applyFontAndVariationSettings(boolean forceNullSet) {
        if (mFontTypeface != null) {
            if (mFontWeight == TEXT_FONT_WEIGHT_UNSPECIFIED) {
                mView.setTypeface(mFontTypeface, mStyle);
            } else {
                mView.setTypeface(mFontTypeface);
            }
        } else if (forceNullSet) {
            mView.setTypeface(null);
        }

        if (mFontVariationSettings != null && Build.VERSION.SDK_INT >= 26) {
            Api26Impl.setFontVariationSettings(mView, mFontVariationSettings);
        }
    }

    /**
     * Load mFontTypeface from an xml, may be called multiple times with e.g. style, textAppearance,
     * and attribute items.
     *
     * Note: Setting multiple fonts at different levels currently triggers multiple font-loads.
     *
     * @param context to check if restrictions avoid loading downloadable fonts
     * @param a attributes to read from, e.g. a textappearance
     */
    private boolean updateTypefaceAndStyle(Context context, TintTypedArray a) {
        mStyle = a.getInt(R.styleable.TextAppearance_android_textStyle, mStyle);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            mFontWeight = a.getInt(R.styleable.TextAppearance_android_textFontWeight,
                    TEXT_FONT_WEIGHT_UNSPECIFIED);
            if (mFontWeight != TEXT_FONT_WEIGHT_UNSPECIFIED) {
                mStyle = Typeface.NORMAL | (mStyle & Typeface.ITALIC);
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                && a.hasValue(R.styleable.TextAppearance_fontVariationSettings)) {
            mFontVariationSettings = a.getString(R.styleable.TextAppearance_fontVariationSettings);
        }

        if (a.hasValue(R.styleable.TextAppearance_android_fontFamily)
                || a.hasValue(R.styleable.TextAppearance_fontFamily)) {
            mFontTypeface = null;
            int fontFamilyId = a.hasValue(R.styleable.TextAppearance_fontFamily)
                    ? R.styleable.TextAppearance_fontFamily
                    : R.styleable.TextAppearance_android_fontFamily;
            final int fontWeight = mFontWeight;
            final int style = mStyle;
            if (!context.isRestricted()) {
                ResourcesCompat.FontCallback replyCallback = makeFontCallback(fontWeight,
                        style);
                try {
                    // Note the callback will be triggered on the UI thread.
                    final Typeface typeface = a.getFont(fontFamilyId, mStyle, replyCallback);
                    // assume Typeface does have fontVariationSettings in this path
                    if (typeface != null) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P
                                && mFontWeight != TEXT_FONT_WEIGHT_UNSPECIFIED) {
                            mFontTypeface = Api28Impl.create(
                                    Typeface.create(typeface, Typeface.NORMAL), mFontWeight,
                                    (mStyle & Typeface.ITALIC) != 0);
                        } else {
                            mFontTypeface = typeface;
                        }
                    }
                    // If this call gave us an immediate result, ignore any pending callbacks.
                    mAsyncFontPending = mFontTypeface == null;
                } catch (UnsupportedOperationException | Resources.NotFoundException e) {
                    // Expected if it is not a font resource.
                }
            }
            if (mFontTypeface == null) {
                // Try with String. This is done by TextView JB+, but fails in ICS
                String fontFamilyName = a.getString(fontFamilyId);
                if (fontFamilyName != null) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P
                            && mFontWeight != TEXT_FONT_WEIGHT_UNSPECIFIED) {
                        mFontTypeface = Api28Impl.create(
                                Typeface.create(fontFamilyName, Typeface.NORMAL), mFontWeight,
                                (mStyle & Typeface.ITALIC) != 0);
                    } else {
                        mFontTypeface = Typeface.create(fontFamilyName, mStyle);
                    }
                }
            }
            return true;
        }

        if (a.hasValue(R.styleable.TextAppearance_android_typeface)) {
            // Ignore previous pending fonts
            mAsyncFontPending = false;
            int typefaceIndex = a.getInt(R.styleable.TextAppearance_android_typeface, SANS);
            switch (typefaceIndex) {
                case SANS:
                    mFontTypeface = Typeface.SANS_SERIF;
                    break;

                case SERIF:
                    mFontTypeface = Typeface.SERIF;
                    break;

                case MONOSPACE:
                    mFontTypeface = Typeface.MONOSPACE;
                    break;
            }
            return true;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P
                && mFontWeight != TEXT_FONT_WEIGHT_UNSPECIFIED && mFontTypeface != null) {
            mFontTypeface = Api28Impl.create(mFontTypeface, mFontWeight,
                    (mStyle & Typeface.ITALIC) != 0);
            return true;
        }

        return false;
    }

    private ResourcesCompat.@NonNull FontCallback makeFontCallback(int fontWeight, int style) {
        final WeakReference<TextView> textViewWeak = new WeakReference<>(mView);
        return new ResourcesCompat.FontCallback() {
            @Override
            public void onFontRetrieved(@NonNull Typeface typeface) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    if (fontWeight != TEXT_FONT_WEIGHT_UNSPECIFIED) {
                        typeface = Api28Impl.create(typeface, fontWeight,
                                (style & Typeface.ITALIC) != 0);
                    }
                }
                onAsyncTypefaceReceived(textViewWeak, typeface);
            }

            @Override
            public void onFontRetrievalFailed(int reason) {
                // Do nothing.
            }
        };
    }

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    void onAsyncTypefaceReceived(WeakReference<TextView> textViewWeak, final Typeface typeface) {
        if (mAsyncFontPending) {
            // we assume that typeface has the correct variation settings from androidx.core
            mFontTypeface = typeface;
            // TODO(b/266112457) unset mFontVariationSettings here so we don't double apply it
            final TextView textView = textViewWeak.get();
            if (textView != null) {
                if (textView.isAttachedToWindow()) {
                    final int style = mStyle;
                    textView.post(new Runnable() {
                        @Override
                        public void run() {
                            applyNewTypefacePreservingVariationSettings(textView, typeface, style);
                        }
                    });
                } else {
                    applyNewTypefacePreservingVariationSettings(textView, typeface, mStyle);
                }
            }
        }
    }

    private static void applyNewTypefacePreservingVariationSettings(TextView textView,
            Typeface typeface, int style) {
        String fontVariationSettings = null;
        if (Build.VERSION.SDK_INT >= 26) {
            fontVariationSettings = Api26Impl.getFontVariationSettings(textView);
            if (!TextUtils.isEmpty(fontVariationSettings)) {
                Api26Impl.setFontVariationSettings(textView, null);
            }
        }

        textView.setTypeface(typeface, style);
        if (Build.VERSION.SDK_INT >= 26 && !TextUtils.isEmpty(fontVariationSettings)) {
            Api26Impl.setFontVariationSettings(textView, fontVariationSettings);
        }
    }

    void onSetTextAppearance(Context context, int resId) {
        final TintTypedArray a = TintTypedArray.obtainStyledAttributes(context,
                resId, R.styleable.TextAppearance);
        if (a.hasValue(R.styleable.TextAppearance_textAllCaps)) {
            // This breaks away slightly from the logic in TextView.setTextAppearance that serves
            // as an "overlay" on the current state of the TextView. Since android:textAllCaps
            // may have been set to true in this text appearance, we need to make sure that
            // app:textAllCaps has the chance to override it
            setAllCaps(a.getBoolean(R.styleable.TextAppearance_textAllCaps, false));
        }
        if (Build.VERSION.SDK_INT < 23) {
            // If we're running on < API 23, the text colors may contain theme references
            // so let's re-set using our own inflater
            if (a.hasValue(R.styleable.TextAppearance_android_textColor)) {
                final ColorStateList textColor =
                        a.getColorStateList(R.styleable.TextAppearance_android_textColor);
                if (textColor != null) {
                    mView.setTextColor(textColor);
                }
            }
            if (a.hasValue(R.styleable.TextAppearance_android_textColorLink)) {
                final ColorStateList textColorLink =
                        a.getColorStateList(R.styleable.TextAppearance_android_textColorLink);
                if (textColorLink != null) {
                    mView.setLinkTextColor(textColorLink);
                }
            }
            if (a.hasValue(R.styleable.TextAppearance_android_textColorHint)) {
                final ColorStateList textColorHint =
                        a.getColorStateList(R.styleable.TextAppearance_android_textColorHint);
                if (textColorHint != null) {
                    mView.setHintTextColor(textColorHint);
                }
            }
        }
        // For SDK <= P, when the text size attribute is 0, this would not be set. Fix this here.
        if (a.hasValue(R.styleable.TextAppearance_android_textSize)) {
            if (a.getDimensionPixelSize(R.styleable.TextAppearance_android_textSize, -1) == 0) {
                mView.setTextSize(TypedValue.COMPLEX_UNIT_PX, 0.0f);
            }
        }

        boolean containsTypeface = updateTypefaceAndStyle(context, a);
        a.recycle();
        applyFontAndVariationSettings(containsTypeface);
    }

    void setAllCaps(boolean allCaps) {
        mView.setAllCaps(allCaps);
    }

    void onSetCompoundDrawables() {
        applyCompoundDrawablesTints();
    }

    void applyCompoundDrawablesTints() {
        if (mDrawableLeftTint != null || mDrawableTopTint != null ||
                mDrawableRightTint != null || mDrawableBottomTint != null) {
            final Drawable[] compoundDrawables = mView.getCompoundDrawables();
            applyCompoundDrawableTint(compoundDrawables[0], mDrawableLeftTint);
            applyCompoundDrawableTint(compoundDrawables[1], mDrawableTopTint);
            applyCompoundDrawableTint(compoundDrawables[2], mDrawableRightTint);
            applyCompoundDrawableTint(compoundDrawables[3], mDrawableBottomTint);
        }
        if (mDrawableStartTint != null || mDrawableEndTint != null) {
            final Drawable[] compoundDrawables = mView.getCompoundDrawablesRelative();
            applyCompoundDrawableTint(compoundDrawables[0], mDrawableStartTint);
            applyCompoundDrawableTint(compoundDrawables[2], mDrawableEndTint);
        }
    }

    private void applyCompoundDrawableTint(Drawable drawable, TintInfo info) {
        if (drawable != null && info != null) {
            AppCompatDrawableManager.tintDrawable(drawable, info, mView.getDrawableState());
        }
    }

    private static TintInfo createTintInfo(Context context,
            AppCompatDrawableManager drawableManager, int drawableId) {
        final ColorStateList tintList = drawableManager.getTintList(context, drawableId);
        if (tintList != null) {
            final TintInfo tintInfo = new TintInfo();
            tintInfo.mHasTintList = true;
            tintInfo.mTintList = tintList;
            return tintInfo;
        }
        return null;
    }

    @RestrictTo(LIBRARY_GROUP_PREFIX)
    void onLayout(boolean changed, int left, int top, int right, int bottom) {
        if (!SDK_LEVEL_SUPPORTS_AUTOSIZE) {
            autoSizeText();
        }
    }

    @RestrictTo(LIBRARY_GROUP_PREFIX)
    void setTextSize(int unit, float size) {
        if (!SDK_LEVEL_SUPPORTS_AUTOSIZE) {
            if (!isAutoSizeEnabled()) {
                setTextSizeInternal(unit, size);
            }
        }
    }

    @RestrictTo(LIBRARY_GROUP_PREFIX)
    void autoSizeText() {
        mAutoSizeTextHelper.autoSizeText();
    }

    @RestrictTo(LIBRARY_GROUP_PREFIX)
    boolean isAutoSizeEnabled() {
        return mAutoSizeTextHelper.isAutoSizeEnabled();
    }

    private void setTextSizeInternal(int unit, float size) {
        mAutoSizeTextHelper.setTextSizeInternal(unit, size);
    }

    void setAutoSizeTextTypeWithDefaults(@TextViewCompat.AutoSizeTextType int autoSizeTextType) {
        mAutoSizeTextHelper.setAutoSizeTextTypeWithDefaults(autoSizeTextType);
    }

    void setAutoSizeTextTypeUniformWithConfiguration(
            int autoSizeMinTextSize,
            int autoSizeMaxTextSize,
            int autoSizeStepGranularity,
            int unit) throws IllegalArgumentException {
        mAutoSizeTextHelper.setAutoSizeTextTypeUniformWithConfiguration(
                autoSizeMinTextSize, autoSizeMaxTextSize, autoSizeStepGranularity, unit);
    }

    void setAutoSizeTextTypeUniformWithPresetSizes(int @NonNull [] presetSizes, int unit)
            throws IllegalArgumentException {
        mAutoSizeTextHelper.setAutoSizeTextTypeUniformWithPresetSizes(presetSizes, unit);
    }

    @TextViewCompat.AutoSizeTextType
    int getAutoSizeTextType() {
        return mAutoSizeTextHelper.getAutoSizeTextType();
    }

    int getAutoSizeStepGranularity() {
        return mAutoSizeTextHelper.getAutoSizeStepGranularity();
    }

    int getAutoSizeMinTextSize() {
        return mAutoSizeTextHelper.getAutoSizeMinTextSize();
    }

    int getAutoSizeMaxTextSize() {
        return mAutoSizeTextHelper.getAutoSizeMaxTextSize();
    }

    int[] getAutoSizeTextAvailableSizes() {
        return mAutoSizeTextHelper.getAutoSizeTextAvailableSizes();
    }

    @Nullable ColorStateList getCompoundDrawableTintList() {
        return mDrawableTint != null ? mDrawableTint.mTintList : null;
    }

    void setCompoundDrawableTintList(@Nullable ColorStateList tintList) {
        if (mDrawableTint == null) {
            mDrawableTint = new TintInfo();
        }
        mDrawableTint.mTintList = tintList;
        mDrawableTint.mHasTintList = tintList != null;
        setCompoundTints();
    }

    PorterDuff.@Nullable Mode getCompoundDrawableTintMode() {
        return mDrawableTint != null ? mDrawableTint.mTintMode : null;
    }

    void setCompoundDrawableTintMode(PorterDuff.@Nullable Mode tintMode) {
        if (mDrawableTint == null) {
            mDrawableTint = new TintInfo();
        }
        mDrawableTint.mTintMode = tintMode;
        mDrawableTint.mHasTintMode = tintMode != null;
        setCompoundTints();
    }

    private void setCompoundTints() {
        mDrawableLeftTint = mDrawableTint;
        mDrawableTopTint = mDrawableTint;
        mDrawableRightTint = mDrawableTint;
        mDrawableBottomTint = mDrawableTint;
        mDrawableStartTint = mDrawableTint;
        mDrawableEndTint = mDrawableTint;
    }

    private void setCompoundDrawables(Drawable drawableLeft, Drawable drawableTop,
            Drawable drawableRight, Drawable drawableBottom, Drawable drawableStart,
            Drawable drawableEnd) {
        // Mirror TextView logic: if start/end drawables supplied, ignore left/right
        if (drawableStart != null || drawableEnd != null) {
            final Drawable[] existingRel = mView.getCompoundDrawablesRelative();
            Drawable start = drawableStart != null ? drawableStart : existingRel[0];
            Drawable top = drawableTop != null ? drawableTop : existingRel[1];
            Drawable end = drawableEnd != null ? drawableEnd : existingRel[2];
            mView.setCompoundDrawablesRelativeWithIntrinsicBounds(start, top, end,
                    drawableBottom != null ? drawableBottom : existingRel[3]);
        } else if (drawableLeft != null || drawableTop != null
                || drawableRight != null || drawableBottom != null) {
            // If have non-compat relative drawables, then ignore leftCompat/rightCompat
            final Drawable[] existingRel = mView.getCompoundDrawablesRelative();
            if (existingRel[0] != null || existingRel[2] != null) {
                Drawable top = drawableTop != null ? drawableTop : existingRel[1];
                Drawable bottom = drawableBottom != null ? drawableBottom : existingRel[3];
                mView.setCompoundDrawablesRelativeWithIntrinsicBounds(existingRel[0], top,
                        existingRel[2], bottom);
                return;
            }
            // No relative drawables, so just set any compat drawables
            final Drawable[] existingAbs = mView.getCompoundDrawables();
            mView.setCompoundDrawablesWithIntrinsicBounds(
                    drawableLeft != null ? drawableLeft : existingAbs[0],
                    drawableTop != null ? drawableTop : existingAbs[1],
                    drawableRight != null ? drawableRight : existingAbs[2],
                    drawableBottom != null ? drawableBottom : existingAbs[3]
            );
        }
    }

    /**
     * For SDK < R(API 30), populates the {@link EditorInfo}'s initial surrounding text from the
     * given {@link TextView} if it created an {@link InputConnection}.
     *
     * <p>
     * Use {@link EditorInfoCompat#setInitialSurroundingText(EditorInfo, CharSequence)} to provide
     * initial input text when {@link TextView#onCreateInputConnection(EditorInfo). This method
     * would only be used when running on < R since {@link TextView} already does this on R.
     *
     * @param textView the {@code TextView} to extract the initial surrounding text from
     * @param editorInfo the {@link EditorInfo} on which to set the surrounding text
     */
    void populateSurroundingTextIfNeeded(
            @NonNull TextView textView,
            @Nullable InputConnection inputConnection,
            @NonNull EditorInfo editorInfo) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R && inputConnection != null) {
            EditorInfoCompat.setInitialSurroundingText(editorInfo, textView.getText());
        }
    }

    @RequiresApi(26)
    static class Api26Impl {

        /**
         * Cache for variation instances created based on an existing Typeface
         */
        private static class VarCacheKey {
            private final Typeface mTypeface;
            private final String mFontVariationSettings;
            private final int mWeightAdjustment;

            VarCacheKey(Typeface typeface, String fontVariationSettings, int weightAdjustment) {
                this.mTypeface = typeface;
                this.mFontVariationSettings = fontVariationSettings;
                this.mWeightAdjustment = weightAdjustment;
            }

            @Override
            public boolean equals(Object o) {
                if (!(o instanceof VarCacheKey)) return false;
                VarCacheKey cacheKey = (VarCacheKey) o;
                return mWeightAdjustment == cacheKey.mWeightAdjustment && Objects.equals(mTypeface,
                        cacheKey.mTypeface) && Objects.equals(mFontVariationSettings,
                        cacheKey.mFontVariationSettings);
            }

            @Override
            public int hashCode() {
                return Objects.hash(mTypeface, mFontVariationSettings, mWeightAdjustment);
            }
        }
        private static final LruCache<VarCacheKey, Typeface> sVariationsCache = new LruCache<>(30);

        /**
         * Used to create variation instances; initialized lazily
         */
        private static @Nullable Paint sPaint;

        private Api26Impl() {
            // This class is not instantiable.
        }

        static String getFontVariationSettings(TextView textView) {
            return textView.getFontVariationSettings();
        }

        static boolean setFontVariationSettings(TextView textView, String fontVariationSettings) {
            if (Objects.equals(textView.getFontVariationSettings(), fontVariationSettings)) {
                // textView will early-exit if we don't clear fontVariationSettings
                textView.setFontVariationSettings("");
            }
            return textView.setFontVariationSettings(fontVariationSettings);
        }

        private static float clampWeight(float value) {
            if (value < FontStyle.FONT_WEIGHT_MIN) {
                return FontStyle.FONT_WEIGHT_MIN;
            }
            return Math.min(value, FontStyle.FONT_WEIGHT_MAX);
        }

        @VisibleForTesting
        static String adjustFontVariationSettings(String fontVariationSettings,
                int fontWeightAdjustment) {
            if (fontWeightAdjustment == 0) {
                return fontVariationSettings;
            }

            FontVariationAxis[] axes;
            if (TextUtils.isEmpty(fontVariationSettings)) {
                // fromFontVariationSettings returns null if the given text is empty. This should
                // not be treated as error.
                axes = new FontVariationAxis[0];
            } else {
                axes = FontVariationAxis.fromFontVariationSettings(fontVariationSettings);
                if (axes == null) {
                    // invalid format of the font variation settings. Let caller to handle this.
                    return fontVariationSettings;
                }
            }

            boolean wghtAdjusted = false;
            for (int i = 0; i < axes.length; ++i) {
                FontVariationAxis axis = axes[i];
                if ("wght".equals(axis.getTag())) {
                    axes[i] = new FontVariationAxis("wght",
                            clampWeight(axis.getStyleValue() + fontWeightAdjustment));
                    wghtAdjusted = true;
                }
            }
            if (!wghtAdjusted) {
                FontVariationAxis[] newAxes = new FontVariationAxis[axes.length + 1];
                System.arraycopy(axes, 0, newAxes, 0, axes.length);
                newAxes[axes.length] = new FontVariationAxis("wght",
                        clampWeight(400 + fontWeightAdjustment));
                axes = newAxes;
            }
            return FontVariationAxis.toFontVariationSettings(axes);
        }

        /**
         * Create a new Typeface based on {@code baseTypeFace} with the specified variation
         * settings.  Uses a cache to avoid memory scaling with the number of AppCompatTextViews.
         *
         * @param baseTypeface the original typeface, preferably without variations applied
         *                     (used both to create the new instance, and as a cache key).
         *                     Note: this method will correctly handle instances with variations
         *                     applied, as we have no way of detecting that.  However, cache hit
         *                     rates may be decreased.
         * @param fontVariationSettings the new font variation settings.
         *                              This is used as a cache key without sorting, to avoid
         *                              additional per-TextView allocations to parse and sort the
         *                              variation settings.  App developers should strive to provide
         *                              the settings in the same order every time within their app,
         *                              in order to get the best cache performance.
         * @return the new instance, or {@code null} if
         *         {@link Paint#setFontVariationSettings(String)} would return null for this
         *         Typeface and font variation settings string.
         */
        @UiThread
        static @Nullable Typeface createVariationInstance(@Nullable Typeface baseTypeface,
                @Nullable String fontVariationSettings, int fontAdjustment) {
            VarCacheKey cacheKey = new VarCacheKey(baseTypeface, fontVariationSettings,
                    fontAdjustment);

            Typeface result = sVariationsCache.get(cacheKey);
            if (result != null) {
                return result;
            }
            Paint paint = sPaint != null ? sPaint : (sPaint = new Paint());

            fontVariationSettings = adjustFontVariationSettings(fontVariationSettings,
                    fontAdjustment);

            // Work around b/353609778
            if (Objects.equals(paint.getFontVariationSettings(), fontVariationSettings)) {
                paint.setFontVariationSettings(null);
            }

            // Use Paint to create a new Typeface based on an existing one
            paint.setTypeface(baseTypeface);
            boolean effective = paint.setFontVariationSettings(fontVariationSettings);
            if (effective) {
                result = paint.getTypeface();
                sVariationsCache.put(cacheKey, result);
                return result;
            } else {
                return null;
            }
        }

        static int getAutoSizeStepGranularity(TextView textView) {
            return textView.getAutoSizeStepGranularity();
        }

        static void setAutoSizeTextTypeUniformWithConfiguration(TextView textView,
                int autoSizeMinTextSize, int autoSizeMaxTextSize, int autoSizeStepGranularity,
                int unit) {
            textView.setAutoSizeTextTypeUniformWithConfiguration(autoSizeMinTextSize,
                    autoSizeMaxTextSize, autoSizeStepGranularity, unit);
        }

        static void setAutoSizeTextTypeUniformWithPresetSizes(TextView textView, int[] presetSizes,
                int unit) {
            textView.setAutoSizeTextTypeUniformWithPresetSizes(presetSizes, unit);
        }
    }

    @RequiresApi(24)
    static class Api24Impl {
        private Api24Impl() {
            // This class is not instantiable.
        }

        static void setTextLocales(TextView textView, LocaleList locales) {
            textView.setTextLocales(locales);
        }

        static LocaleList forLanguageTags(String list) {
            return LocaleList.forLanguageTags(list);
        }
    }

    static class Api21Impl {
        private Api21Impl() {
            // This class is not instantiable.
        }

        static Locale forLanguageTag(String languageTag) {
            return Locale.forLanguageTag(languageTag);
        }

    }

    @RequiresApi(28)
    static class Api28Impl {
        private Api28Impl() {
            // This class is not instantiable.
        }

        static Typeface create(Typeface family, int weight, boolean italic) {
            return Typeface.create(family, weight, italic);
        }

    }

    @RequiresApi(26)
    static class FontVariationSettingsManager {
        private static final String TAG = "FontVarSettings";

        private final TextView mTextView;
        private final Consumer<Typeface> mTypefaceSetter;

        /**
         * Equivalent to Typeface.mOriginalTypeface.
         * Used to correctly emulate the behavior of getTypeface(), because we need to call
         * TextView#setTypeface directly in order to implement caching of variation instances of
         * typefaces.
         */
        private Typeface mOriginalTypeface;

        /**
         * The last Typeface we are aware of being set on TextView.
         * Used to detect if it has been changed out from under us via directly calling
         * {@link android.graphics.Paint#setTypeface(Typeface)} or
         * {@link android.graphics.Paint#setFontVariationSettings(String)}
         * (which is not supported, so this is a best-effort workaround).
         *
         * @see #setTypefaceInternal(Typeface)
         */
        private Typeface mLastKnownTypefaceSetOnPaint;

        /**
         * The currently applied font variation settings.
         * Used to make getFontVariationSettings somewhat more accurate with Typeface instance
         * caching, as we don't call super.setFontVariationSettings.
         */
        private String mFontVariationSettings;

        FontVariationSettingsManager(TextView textView, Consumer<Typeface> typefaceSetter) {
            mTextView = textView;
            mTypefaceSetter = typefaceSetter;
        }

        public boolean setFontVariationSettings(@Nullable String fontVariationSettings) {
            Typeface baseTypeface = mOriginalTypeface;
            // Try to work around apps mutating the result of getPaint()
            // See setTypefaceInternal doc comment for details.
            Paint paint = mTextView.getPaint();
            if (mLastKnownTypefaceSetOnPaint != paint.getTypeface()) {
                Log.w(TAG, "getPaint().getTypeface() changed unexpectedly."
                        + " App code should not modify the result of getPaint().");
                // Best effort: use that new Typeface instead.
                baseTypeface = paint.getTypeface();
            }
            int fontAdjustment = getFontWeightAdjustment();
            if (fontAdjustment == Configuration.FONT_WEIGHT_ADJUSTMENT_UNDEFINED) {
                fontAdjustment = 0;
            }
            Typeface variationTypefaceInstance =
                    AppCompatTextHelper.Api26Impl.createVariationInstance(
                            baseTypeface, fontVariationSettings, fontAdjustment);
            if (variationTypefaceInstance != null) {
                setTypefaceInternal(variationTypefaceInstance);
                mFontVariationSettings = fontVariationSettings;
                return true;
            } else {
                return false;
            }
        }

        public String getFontVariationSettings() {
            return mFontVariationSettings;
        }

        private int mFontWeightAdjustmentForTesting = -1;

        @VisibleForTesting
        int getFontWeightAdjustment() {
            if (mFontWeightAdjustmentForTesting != -1) {
                return mFontWeightAdjustmentForTesting;
            }
            if (Build.VERSION.SDK_INT >= 31) {
                Resources res = mTextView.getContext().getResources();
                int adjustment = res.getConfiguration().fontWeightAdjustment;
                if (adjustment == Configuration.FONT_WEIGHT_ADJUSTMENT_UNDEFINED) {
                    return 0;
                } else {
                    return adjustment;
                }
            } else {
                return 0;
            }
        }

        @VisibleForTesting
        void setFontWeightAdjustmentForTesting(int fontWeightAdjustmentForTesting) {
            mFontWeightAdjustmentForTesting = fontWeightAdjustmentForTesting;
        }

        // Never call super.setTypeface directly, always use this or setTypefaceInternal
        // See docs on setTypefaceInternal for the differences
        public void setTypeface(@Nullable Typeface tf) {
            mOriginalTypeface = tf;
            setTypefaceInternal(tf);
        }

        public Typeface getTypeface() {
            return mOriginalTypeface;
        }

        /**
         * Call this when setting the typeface in any way that the user didn't directly ask for
         * (that is, any case where TextView itself does not call through to setTypeface or
         * otherwise set its mOriginalTypeface).  Otherwise, use {@link #setTypeface(Typeface)}
         * (or something that calls it).
         * <p>
         * Calls the superclass setTypeface, but does not set mOriginalTypeface.
         * Also tracks what we set it to, in order to detect when it's been changed out from under
         * us via modifying the Paint object directly.
         * This isn't officially supported ({@link TextView#getPaint()} specifically says not to
         * modify it), but at least one app is known to have done this, so we're providing
         * best-effort support.
         */
        private void setTypefaceInternal(@Nullable Typeface tf) {
            mLastKnownTypefaceSetOnPaint = tf;
            mTypefaceSetter.accept(tf);
        }
    }
}
