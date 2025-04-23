/*
 * Copyright (C) 2010 The Android Open Source Project
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

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.RestrictTo;
import androidx.appcompat.R;
import androidx.appcompat.view.ActionMode;
import androidx.appcompat.view.menu.MenuBuilder;
import androidx.core.view.ViewCompat;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 */
@RestrictTo(LIBRARY_GROUP_PREFIX)
public class ActionBarContextView extends AbsActionBarView {
    private CharSequence mTitle;
    private CharSequence mSubtitle;

    private View mClose;
    private View mCloseButton;
    private View mCustomView;
    private LinearLayout mTitleLayout;
    private TextView mTitleView;
    private TextView mSubtitleView;
    private int mTitleStyleRes;
    private int mSubtitleStyleRes;
    private boolean mTitleOptional;
    private int mCloseItemLayout;
    private int mInsetsPaddingLeft;
    private int mInsetsPaddingTop;
    private int mInsetsPaddingRight;
    private int mInsetsPaddingBottom;
    private int mNonInsetsPaddingLeft;
    private int mNonInsetsPaddingTop;
    private int mNonInsetsPaddingRight;
    private int mNonInsetsPaddingBottom;

    public ActionBarContextView(@NonNull Context context) {
        this(context, null);
    }

    public ActionBarContextView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, R.attr.actionModeStyle);
    }

    public ActionBarContextView(
            @NonNull Context context, @Nullable AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        final TintTypedArray a = TintTypedArray.obtainStyledAttributes(context, attrs,
                R.styleable.ActionMode, defStyle, 0);
        setBackground(a.getDrawable(R.styleable.ActionMode_background));
        mTitleStyleRes = a.getResourceId(
                R.styleable.ActionMode_titleTextStyle, 0);
        mSubtitleStyleRes = a.getResourceId(
                R.styleable.ActionMode_subtitleTextStyle, 0);

        mContentHeight = a.getLayoutDimension(
                R.styleable.ActionMode_height, 0);

        mCloseItemLayout = a.getResourceId(
                R.styleable.ActionMode_closeItemLayout,
                R.layout.abc_action_mode_close_item_material);

        a.recycle();

        mNonInsetsPaddingLeft = getPaddingLeft();
        mNonInsetsPaddingTop = getPaddingTop();
        mNonInsetsPaddingRight = getPaddingRight();
        mNonInsetsPaddingBottom = getPaddingBottom();
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (mActionMenuPresenter != null) {
            mActionMenuPresenter.hideOverflowMenu();
            mActionMenuPresenter.hideSubMenus();
        }
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        // Action bar can change size on configuration changes.
        // Reread the desired height from the theme-specified style.
        final TypedArray a = getContext().obtainStyledAttributes(null, R.styleable.ActionMode,
                R.attr.actionModeStyle, 0);
        setContentHeight(a.getLayoutDimension(R.styleable.ActionMode_height, 0));
        a.recycle();
    }

    @Override
    public void setContentHeight(int height) {
        mContentHeight = height;
    }

    public void setCustomView(View view) {
        if (mCustomView != null) {
            removeView(mCustomView);
        }
        mCustomView = view;
        if (view != null && mTitleLayout != null) {
            removeView(mTitleLayout);
            mTitleLayout = null;
        }
        if (view != null) {
            addView(view);
        }
        requestLayout();
    }

    public void setTitle(CharSequence title) {
        mTitle = title;
        initTitle();
        ViewCompat.setAccessibilityPaneTitle(this, title);
    }

    public void setSubtitle(CharSequence subtitle) {
        mSubtitle = subtitle;
        initTitle();
    }

    public CharSequence getTitle() {
        return mTitle;
    }

    public CharSequence getSubtitle() {
        return mSubtitle;
    }

    private void initTitle() {
        if (mTitleLayout == null) {
            LayoutInflater inflater = LayoutInflater.from(getContext());
            inflater.inflate(R.layout.abc_action_bar_title_item, this);
            mTitleLayout = (LinearLayout) getChildAt(getChildCount() - 1);
            mTitleView = (TextView) mTitleLayout.findViewById(R.id.action_bar_title);
            mSubtitleView = (TextView) mTitleLayout.findViewById(R.id.action_bar_subtitle);
            if (mTitleStyleRes != 0) {
                mTitleView.setTextAppearance(getContext(), mTitleStyleRes);
            }
            if (mSubtitleStyleRes != 0) {
                mSubtitleView.setTextAppearance(getContext(), mSubtitleStyleRes);
            }
        }

        mTitleView.setText(mTitle);
        mSubtitleView.setText(mSubtitle);

        final boolean hasTitle = !TextUtils.isEmpty(mTitle);
        final boolean hasSubtitle = !TextUtils.isEmpty(mSubtitle);
        mSubtitleView.setVisibility(hasSubtitle ? VISIBLE : GONE);
        mTitleLayout.setVisibility(hasTitle || hasSubtitle ? VISIBLE : GONE);
        if (mTitleLayout.getParent() == null) {
            addView(mTitleLayout);
        }
    }

    public void initForMode(final ActionMode mode) {
        if (mClose == null) {
            LayoutInflater inflater = LayoutInflater.from(getContext());
            mClose = inflater.inflate(mCloseItemLayout, this, false);
            addView(mClose);
        } else if (mClose.getParent() == null) {
            addView(mClose);
        }

        mCloseButton = mClose.findViewById(R.id.action_mode_close_button);
        mCloseButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mode.finish();
            }
        });

        final MenuBuilder menu = (MenuBuilder) mode.getMenu();
        if (mActionMenuPresenter != null) {
            mActionMenuPresenter.dismissPopupMenus();
        }
        mActionMenuPresenter = new ActionMenuPresenter(getContext());
        mActionMenuPresenter.setReserveOverflow(true);

        final LayoutParams layoutParams = new LayoutParams(LayoutParams.WRAP_CONTENT,
                LayoutParams.MATCH_PARENT);
        menu.addMenuPresenter(mActionMenuPresenter, mPopupContext);
        mMenuView = (ActionMenuView) mActionMenuPresenter.getMenuView(this);
        mMenuView.setBackground(null);
        addView(mMenuView, layoutParams);
    }

    public void closeMode() {
        if (mClose == null) {
            killMode();
            return;
        }
    }

    public void killMode() {
        removeAllViews();
        mCustomView = null;
        mMenuView = null;
        mActionMenuPresenter = null;
        if (mCloseButton != null) {
            mCloseButton.setOnClickListener(null);
        }
    }

    @Override
    public boolean showOverflowMenu() {
        if (mActionMenuPresenter != null) {
            return mActionMenuPresenter.showOverflowMenu();
        }
        return false;
    }

    @Override
    public boolean hideOverflowMenu() {
        if (mActionMenuPresenter != null) {
            return mActionMenuPresenter.hideOverflowMenu();
        }
        return false;
    }

    @Override
    public boolean isOverflowMenuShowing() {
        if (mActionMenuPresenter != null) {
            return mActionMenuPresenter.isOverflowMenuShowing();
        }
        return false;
    }

    @Override
    protected ViewGroup.LayoutParams generateDefaultLayoutParams() {
        // Used by custom views if they don't supply layout params. Everything else
        // added to an ActionBarContextView should have them already.
        return new MarginLayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
    }

    @Override
    public ViewGroup.LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new MarginLayoutParams(getContext(), attrs);
    }

    @Override
    public void setPadding(int left, int top, int right, int bottom) {
        if (mNonInsetsPaddingLeft != left || mNonInsetsPaddingTop != top
                || mNonInsetsPaddingRight != right || mNonInsetsPaddingBottom != bottom) {
            mNonInsetsPaddingLeft = left;
            mNonInsetsPaddingTop = top;
            mNonInsetsPaddingRight = right;
            mNonInsetsPaddingBottom = bottom;
            updatePadding();
        }
    }

    /**
     * Sets the padding for insets to prevent the content from extending into the insets area.
     * The total visible padding will be the addition of the padding set with this method and the
     * one set by {@link #setPadding(int, int, int, int)}.
     *
     * @param left the left padding in pixels.
     * @param top the top padding in pixels.
     * @param right the right padding in pixels.
     * @param bottom the bottom padding in pixels.
     */
    public void setPaddingForInsets(int left, int top, int right, int bottom) {
        if (mInsetsPaddingLeft != left || mInsetsPaddingTop != top
                || mInsetsPaddingRight != right || mInsetsPaddingBottom != bottom) {
            mInsetsPaddingLeft = left;
            mInsetsPaddingTop = top;
            mInsetsPaddingRight = right;
            mInsetsPaddingBottom = bottom;
            updatePadding();
        }
    }

    private void updatePadding() {
        super.setPadding(
                mInsetsPaddingLeft + mNonInsetsPaddingLeft,
                mInsetsPaddingTop + mNonInsetsPaddingTop,
                mInsetsPaddingRight + mNonInsetsPaddingRight,
                mInsetsPaddingBottom + mNonInsetsPaddingBottom);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        final int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        if (widthMode != MeasureSpec.EXACTLY) {
            throw new IllegalStateException(getClass().getSimpleName() + " can only be used " +
                    "with android:layout_width=\"match_parent\" (or fill_parent)");
        }

        final int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        if (heightMode == MeasureSpec.UNSPECIFIED) {
            throw new IllegalStateException(getClass().getSimpleName() + " can only be used " +
                    "with android:layout_height=\"wrap_content\"");
        }

        final int contentWidth = MeasureSpec.getSize(widthMeasureSpec);
        int availableWidth = contentWidth - getPaddingLeft() - getPaddingRight();

        final int verticalPadding = getPaddingTop() + getPaddingBottom();
        final int maxHeight = mContentHeight > 0
                ? mContentHeight + mInsetsPaddingTop + mInsetsPaddingBottom
                : MeasureSpec.getSize(heightMeasureSpec);
        final int height = maxHeight - verticalPadding;
        final int childSpecHeight = MeasureSpec.makeMeasureSpec(height, MeasureSpec.AT_MOST);

        if (mClose != null) {
            availableWidth = measureChildView(mClose, availableWidth, childSpecHeight, 0);
            MarginLayoutParams lp = (MarginLayoutParams) mClose.getLayoutParams();
            availableWidth -= lp.leftMargin + lp.rightMargin;
        }

        if (mMenuView != null && mMenuView.getParent() == this) {
            availableWidth = measureChildView(mMenuView, availableWidth,
                    childSpecHeight, 0);
        }

        if (mTitleLayout != null && mCustomView == null) {
            if (mTitleOptional) {
                final int titleWidthSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
                mTitleLayout.measure(titleWidthSpec, childSpecHeight);
                final int titleWidth = mTitleLayout.getMeasuredWidth();
                final boolean titleFits = titleWidth <= availableWidth;
                if (titleFits) {
                    availableWidth -= titleWidth;
                }
                mTitleLayout.setVisibility(titleFits ? VISIBLE : GONE);
            } else {
                availableWidth = measureChildView(mTitleLayout, availableWidth, childSpecHeight, 0);
            }
        }

        if (mCustomView != null) {
            ViewGroup.LayoutParams lp = mCustomView.getLayoutParams();
            final int customWidthMode = lp.width != LayoutParams.WRAP_CONTENT ?
                    MeasureSpec.EXACTLY : MeasureSpec.AT_MOST;
            final int customWidth = lp.width >= 0 ?
                    Math.min(lp.width, availableWidth) : availableWidth;
            final int customHeightMode = lp.height != LayoutParams.WRAP_CONTENT ?
                    MeasureSpec.EXACTLY : MeasureSpec.AT_MOST;
            final int customHeight = lp.height >= 0 ?
                    Math.min(lp.height, height) : height;
            mCustomView.measure(MeasureSpec.makeMeasureSpec(customWidth, customWidthMode),
                    MeasureSpec.makeMeasureSpec(customHeight, customHeightMode));
        }

        if (mContentHeight <= 0) {
            int measuredHeight = 0;
            final int count = getChildCount();
            for (int i = 0; i < count; i++) {
                View v = getChildAt(i);
                int paddedViewHeight = v.getMeasuredHeight() + verticalPadding;
                if (paddedViewHeight > measuredHeight) {
                    measuredHeight = paddedViewHeight;
                }
            }
            setMeasuredDimension(contentWidth, measuredHeight);
        } else {
            setMeasuredDimension(contentWidth, maxHeight);
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        final boolean isLayoutRtl = ViewUtils.isLayoutRtl(this);
        int x = isLayoutRtl ? r - l - getPaddingRight() : getPaddingLeft();
        final int y = getPaddingTop();
        final int contentHeight = b - t - getPaddingTop() - getPaddingBottom();

        if (mClose != null && mClose.getVisibility() != GONE) {
            MarginLayoutParams lp = (MarginLayoutParams) mClose.getLayoutParams();
            final int startMargin = (isLayoutRtl ? lp.rightMargin : lp.leftMargin);
            final int endMargin = (isLayoutRtl ? lp.leftMargin : lp.rightMargin);
            x = next(x, startMargin, isLayoutRtl);
            x += positionChild(mClose, x, y, contentHeight, isLayoutRtl);
            x = next(x, endMargin, isLayoutRtl);
        }

        if (mTitleLayout != null && mCustomView == null && mTitleLayout.getVisibility() != GONE) {
            x += positionChild(mTitleLayout, x, y, contentHeight, isLayoutRtl);
        }

        if (mCustomView != null) {
            x += positionChild(mCustomView, x, y, contentHeight, isLayoutRtl);
        }

        x = isLayoutRtl ? getPaddingLeft() : r - l - getPaddingRight();

        if (mMenuView != null) {
            x += positionChild(mMenuView, x, y, contentHeight, !isLayoutRtl);
        }
    }

    @Override
    public boolean shouldDelayChildPressedState() {
        return false;
    }

    public void setTitleOptional(boolean titleOptional) {
        if (titleOptional != mTitleOptional) {
            requestLayout();
        }
        mTitleOptional = titleOptional;
    }

    public boolean isTitleOptional() {
        return mTitleOptional;
    }
}
