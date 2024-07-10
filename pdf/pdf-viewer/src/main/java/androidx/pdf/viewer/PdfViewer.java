/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.pdf.viewer;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver.OnScrollChangedListener;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.UiThread;
import androidx.annotation.VisibleForTesting;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.pdf.R;
import androidx.pdf.data.DisplayData;
import androidx.pdf.data.ErrorType;
import androidx.pdf.data.FutureValue;
import androidx.pdf.data.FutureValues.SettableFutureValue;
import androidx.pdf.data.Openable;
import androidx.pdf.data.PdfStatus;
import androidx.pdf.data.Range;
import androidx.pdf.fetcher.Fetcher;
import androidx.pdf.find.FindInFileListener;
import androidx.pdf.find.FindInFileView;
import androidx.pdf.find.MatchCount;
import androidx.pdf.models.Dimensions;
import androidx.pdf.models.GotoLink;
import androidx.pdf.models.LinkRects;
import androidx.pdf.models.MatchRects;
import androidx.pdf.models.PageSelection;
import androidx.pdf.util.AnnotationUtils;
import androidx.pdf.util.CycleRange;
import androidx.pdf.util.ObservableValue;
import androidx.pdf.util.ObservableValue.ValueObserver;
import androidx.pdf.util.Preconditions;
import androidx.pdf.util.Screen;
import androidx.pdf.util.ThreadUtils;
import androidx.pdf.util.TileBoard;
import androidx.pdf.util.TileBoard.TileInfo;
import androidx.pdf.util.Toaster;
import androidx.pdf.util.Uris;
import androidx.pdf.viewer.PageViewFactory.PageView;
import androidx.pdf.viewer.loader.PdfLoader;
import androidx.pdf.viewer.loader.PdfLoaderCallbacks;
import androidx.pdf.widget.FastScrollContentModel;
import androidx.pdf.widget.FastScrollView;
import androidx.pdf.widget.ZoomView;
import androidx.pdf.widget.ZoomView.ContentResizedMode;
import androidx.pdf.widget.ZoomView.FitMode;
import androidx.pdf.widget.ZoomView.InitialZoomMode;
import androidx.pdf.widget.ZoomView.RotateMode;
import androidx.pdf.widget.ZoomView.ZoomScroll;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.errorprone.annotations.CanIgnoreReturnValue;

import java.util.List;

/**
 * A {@link Viewer} that can display paginated PDFs. Each page is rendered in its own View.
 * Rendering is done in 2 passes:
 *
 * <ol>
 *   <li>Layout: Request the dimensions of the page and set them as measure for the image view,
 *   <li>Render: Create bitmap(s) at adequate dimensions and attach them to the page view.
 * </ol>
 *
 * <p>The layout pass is progressive: starts with a few first pages of the document, then reach
 * further as the user scrolls down (and ultimately spans the whole document). The rendering pass is
 * tightly limited to the currently visible pages. Pages that are scrolled past (become not visible)
 * have their bitmaps released to free up memory.
 *
 * <p>This is a {@link #SELF_MANAGED_CONTENTS} Viewer: its contents and internal models are kept
 * when the view is destroyed, and re-used when the view is re-created.
 *
 * <p>Major lifecycle events include:
 *
 * <ol>
 *   <li>{@link #onContentsAvailable} / {@link #onDestroy} : Content model is created.
 *   <li>{@link #onCreateView} / {@link #destroyView} : All views are created, the pdf service is
 *       connected.
 * </ol>
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
@SuppressWarnings({"UnusedMethod", "UnusedVariable"})
public class PdfViewer extends LoadingViewer implements FastScrollContentModel {

    private static final String TAG = "PdfViewer";

    /** {@link View#setElevation(float)} value for PDF Pages (API 21+). */
    private static final int PAGE_ELEVATION_DP = 2;

    /** Key for saving page layout reach in bundles. */
    private static final String KEY_LAYOUT_REACH = "plr";

    private static final String KEY_SPACE_LEFT = "leftSpace";
    private static final String KEY_SPACE_TOP = "topSpace";
    private static final String KEY_SPACE_BOTTOM = "bottomSpace";
    private static final String KEY_SPACE_RIGHT = "rightSpace";
    private static final String KEY_QUIT_ON_ERROR = "quitOnError";
    private static final String KEY_EXIT_ON_CANCEL = "exitOnCancel";

    private static Screen sScreen;

    /** Single access to the PDF document: loads contents asynchronously (bitmaps, text,...) */
    private PdfLoader mPdfLoader;

    /** The file being displayed by this viewer. */
    private DisplayData mFileData;

    /** Callbacks of PDF loading asynchronous tasks. */
    @VisibleForTesting
    public final PdfLoaderCallbacks mPdfLoaderCallbacks;

    /** Observer of the page position that controls loading of relevant PDF assets. */
    private ValueObserver<ZoomScroll> mZoomScrollObserver;

    /** Observer to be set when the view is created. */
    @Nullable
    private ValueObserver<ZoomScroll> mPendingScrollPositionObserver;

    private Object mScrollPositionObserverKey;

    private ZoomView mZoomView;

    private PaginatedView mPaginatedView;
    private PaginationModel mPaginationModel;

    private PageIndicator mPageIndicator;

    private SearchModel mSearchModel;
    private PdfSelectionModel mSelectionModel;
    private PdfSelectionHandles mSelectionHandles;

    private ValueObserver<String> mSearchQueryObserver;
    private ValueObserver<Integer> mFastscrollerPositionObserver;
    private ValueObserver<SelectedMatch> mSelectedMatchObserver;
    private ValueObserver<PageSelection> mSelectionObserver;
    private Object mFastscrollerPositionObserverKey;
    private FastScrollView mFastScrollView;
    private ProgressBar mLoadingSpinner;

    private boolean mDocumentLoaded = false;
    private boolean mIsAnnotationIntentResolvable = false;

    /**
     * After the document content is saved over the original in InkActivity, we set this bit to true
     * so we know to callwhen the new document content is loaded.
     */
    private boolean mShouldRedrawOnDocumentLoaded = false;

    @Nullable
    private SettableFutureValue<Boolean> mPrintableVersionCallback;

    // Non-null when a save-as operation is in progress. Cleared when operation is complete and
    // value has been set with success/failure result.
    @Nullable
    private SettableFutureValue<Boolean> mSaveAsCallback;

    private Snackbar mSnackbar;

    private LayoutHandler mLayoutHandler;

    private Uri mLocalUri;
    private FrameLayout mPdfViewer;

    private FindInFileView mFindInFileView;

    private FloatingActionButton mAnnotationButton;

    private PageViewFactory mPageViewFactory;

    private SingleTapHandler mSingleTapHandler;

    public PdfViewer() {
        super(SELF_MANAGED_CONTENTS);
    }

    @Override
    public void configureShareScroll(boolean left, boolean right, boolean top, boolean bottom) {
        mZoomView.setShareScroll(left, right, top, bottom);
    }

    /**
     * If set, this Viewer will call {@link Activity#finish()} if it can't load the PDF. By default,
     * the value is false.
     */
    @NonNull
    @CanIgnoreReturnValue
    public PdfViewer setQuitOnError(boolean quit) {
        getArguments().putBoolean(KEY_QUIT_ON_ERROR, quit);
        return this;
    }

    /**
     * If set, this viewer will finish the attached activity when the user presses cancel on the
     * prompt for the document password.
     */
    @NonNull
    @CanIgnoreReturnValue
    public PdfViewer setExitOnPasswordCancel(boolean shouldExitOnPasswordCancel) {
        getArguments().putBoolean(KEY_EXIT_ON_CANCEL, shouldExitOnPasswordCancel);
        return this;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mFetcher = Fetcher.build(getContext(), 1);
        sScreen = new Screen(this.requireActivity().getApplicationContext());
    }

    @NonNull
    @SuppressLint("InflateParams")
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedState) {
        super.onCreateView(inflater, container, savedState);

        mPdfViewer = (FrameLayout) inflater.inflate(R.layout.pdf_viewer_container, container,
                false);
        mFindInFileView = mPdfViewer.findViewById(R.id.search);
        mFastScrollView = mPdfViewer.findViewById(R.id.fast_scroll_view);
        mPaginatedView = mFastScrollView.findViewById(R.id.pdf_view);
        mPaginationModel = mPaginatedView.getPaginationModel();

        mZoomView = mFastScrollView.findViewById(R.id.zoom_view);
        mZoomView.setStraightenVerticalScroll(true);

        mZoomView
                .setFitMode(FitMode.FIT_TO_WIDTH)
                .setInitialZoomMode(InitialZoomMode.ZOOM_TO_FIT)
                .setRotateMode(RotateMode.KEEP_SAME_VIEWPORT_WIDTH)
                .setContentResizedModeX(ContentResizedMode.KEEP_SAME_RELATIVE);

        // Setting an id so that the View can restore itself. The Id has to be unique and
        // predictable. An alternative that doesn't require id is to rely on this Fragment's
        // onSaveInstanceState().
        mZoomView.setId(getId() * 100);

        mPageIndicator = new PageIndicator(getActivity(), mFastScrollView);
        applyReservedSpace();
        mZoomView.adjustZoomViewMargins();
        mFastscrollerPositionObserver =
                new FastScrollPositionValueObserver(mFastScrollView, mPageIndicator);
        mFastscrollerPositionObserver.onChange(null, mFastScrollView.getScrollerPositionY().get());
        mFastscrollerPositionObserverKey =
                mFastScrollView.getScrollerPositionY().addObserver(mFastscrollerPositionObserver);

        // The view system requires the document loaded in order to be properly initialized, so
        // we delay anything view-related until ViewState.VIEW_READY.
        mZoomView.setVisibility(View.GONE);

        mFastScrollView.setScrollable(this);
        mFastScrollView.setId(getId() * 10);

        mLoadingSpinner = mFastScrollView.findViewById(R.id.progress_indicator);

        setUpEditFab();

        return mPdfViewer;
    }

    @Nullable
    public static Screen getScreen() {
        return sScreen;
    }

    @VisibleForTesting
    public static void setScreenForTest(@NonNull Context context) {
        sScreen = new Screen(context);
    }

    private void applyReservedSpace() {
        if (getArguments().containsKey(KEY_SPACE_TOP)) {
            mZoomView.saveZoomViewBasePadding();
            int left = getArguments().getInt(KEY_SPACE_LEFT, 0);
            int top = getArguments().getInt(KEY_SPACE_TOP, 0);
            int right = getArguments().getInt(KEY_SPACE_RIGHT, 0);
            int bottom = getArguments().getInt(KEY_SPACE_BOTTOM, 0);

            mPageIndicator.getView().setTranslationX(-right);

            mZoomView.setPaddingWithBase(left, top, right, bottom);

            // Adjust the scroll bar to also include the same padding.
            mFastScrollView.setScrollbarMarginTop(mZoomView.getPaddingTop());
            mFastScrollView.setScrollbarMarginRight(right);
            mFastScrollView.setScrollbarMarginBottom(mZoomView.getPaddingBottom());
        }
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (mPendingScrollPositionObserver != null) {
            mScrollPositionObserverKey = mZoomView.zoomScroll().addObserver(
                    mPendingScrollPositionObserver);
            mPendingScrollPositionObserver = null;
        }
    }

    @Override
    protected void onContentsAvailable(@NonNull DisplayData contents, @Nullable Bundle savedState) {
        mFileData = contents;
        createContentModel(
                PdfLoader.create(
                        getActivity().getApplicationContext(),
                        contents,
                        TileBoard.DEFAULT_RECYCLER,
                        mPdfLoaderCallbacks,
                        false));
        mLayoutHandler = new LayoutHandler(mPdfLoader);
        mZoomView.setPdfSelectionModel(mSelectionModel);
        mPaginatedView.setSelectionModel(mSelectionModel);
        mPaginatedView.setSearchModel(mSearchModel);
        mPaginatedView.setPdfLoader(mPdfLoader);

        mSearchQueryObserver =
                new SearchQueryObserver(mPaginatedView);
        mSearchModel.query().addObserver(mSearchQueryObserver);

        mZoomScrollObserver =
                new ZoomScrollValueObserver(mZoomView, mPaginatedView,
                        mLayoutHandler, mAnnotationButton, mFindInFileView, mPageIndicator,
                        mFastScrollView, mIsAnnotationIntentResolvable, mViewState);
        mZoomView.zoomScroll().addObserver(mZoomScrollObserver);

        if (savedState != null) {
            int layoutReach = savedState.getInt(KEY_LAYOUT_REACH);
            mLayoutHandler.setInitialPageLayoutReachWithMax(layoutReach);
        }
    }

    @Override
    protected void onEnter() {
        super.onEnter();
        // This is necessary for password protected PDF documents. If the user failed to produce the
        // correct password, we want to prompt for the correct password every time the film strip
        // comes back to this viewer.
        if (!mDocumentLoaded && mPdfLoader != null) {
            mPdfLoader.reconnect();
        }

        mSingleTapHandler = new SingleTapHandler(getContext(), mAnnotationButton,
                mFindInFileView, mZoomView, mSelectionModel, mPaginationModel, mLayoutHandler);
        mPageViewFactory = new PageViewFactory(requireContext(), mPdfLoader,
                mPaginatedView, mZoomView, mSingleTapHandler);
        mPaginatedView.setPageViewFactory(mPageViewFactory);
        mSelectionObserver =
                new PageSelectionValueObserver(mPaginatedView, mPaginationModel, mPageViewFactory,
                        requireContext());
        mSelectionModel.selection().addObserver(mSelectionObserver);

        mSelectedMatchObserver =
                new SelectedMatchValueObserver(mPaginatedView, mPaginationModel, mPageViewFactory,
                        mZoomView, mLayoutHandler, requireContext());
        mSearchModel.selectedMatch().addObserver(mSelectedMatchObserver);
        if (mPaginatedView != null && mPaginatedView.getChildCount() > 0) {
            loadPageAssets(mZoomView.zoomScroll().get());
        }
    }

    @Override
    public void onExit() {
        super.onExit();
        if (!mDocumentLoaded && mPdfLoader != null) {
            // e.g. a password-protected pdf that wasn't loaded.
            mPdfLoader.disconnect();
        }
    }

    private void createContentModel(PdfLoader pdfLoader) {
        this.mPdfLoader = pdfLoader;

        mSearchModel = new SearchModel(pdfLoader);

        mSelectionModel = new PdfSelectionModel(pdfLoader);

        mSelectionHandles = new PdfSelectionHandles(mSelectionModel, mZoomView, mPaginatedView);

    }

    private void destroyContentModel() {

        mPageIndicator = null;

        mSelectionHandles.destroy();
        mSelectionHandles = null;

        mSelectionModel.selection().removeObserver(mSelectionObserver);
        mSelectionModel = null;

        mSearchModel.selectedMatch().removeObserver(mSelectedMatchObserver);
        mSearchModel.query().removeObserver(mSearchQueryObserver);
        mSearchModel = null;

        mPdfLoader.disconnect();
        mPdfLoader = null;
        mDocumentLoaded = false;
    }

    /**
     *
     */
    public void setPassword(@NonNull String password) {
        if (mPdfLoader != null) {
            mPdfLoader.applyPassword(password);
        }
    }

    @Override
    public void destroyView() {
        if (mZoomView != null) {
            mZoomView.zoomScroll().removeObserver(mZoomScrollObserver);
            if (mScrollPositionObserverKey != null) {
                mZoomView.zoomScroll().removeObserver(mScrollPositionObserverKey);
            }
            mZoomView = null;
        }

        if (mPaginatedView != null) {
            mPaginatedView.removeAllViews();
            mPaginationModel.removeObserver(mPaginatedView);
            mPaginatedView = null;
        }

        if (mPdfLoader != null) {
            mPdfLoader.cancelAll();
            mPdfLoader.disconnect();
            mDocumentLoaded = false;
        }
        mZoomView.setZoomViewBasePadding(new Rect());
        mZoomView.setZoomViewBasePaddingSaved(false);
        super.destroyView();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (mSnackbar != null) {
            mSnackbar.dismiss();
        }
        if (mFastscrollerPositionObserverKey != null && mFastScrollView != null) {
            mFastScrollView.getScrollerPositionY().removeObserver(mFastscrollerPositionObserverKey);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mPdfLoader != null) {
            destroyContentModel();
        }
        mPrintableVersionCallback = null;
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putInt(KEY_LAYOUT_REACH, mLayoutHandler.getPageLayoutReach());
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mZoomView.adjustZoomViewMargins();
    }

    /**
     * Load the PDF document.
     *
     * @param fileUri URI of the document.
     */
    public void loadFile(@NonNull Uri fileUri) {
        Preconditions.checkNotNull(fileUri);
        try {
            validateFileUri(fileUri);
        } catch (SecurityException e) {
            // TODO Toaster.LONG.popToast(this, R.string.problem_with_file);
            finishActivity();
        }

        showSpinner();
        fetchFile(fileUri);
        mLocalUri = fileUri;
        mIsAnnotationIntentResolvable = AnnotationUtils.resolveAnnotationIntent(requireContext(),
                mLocalUri);
        mSingleTapHandler.setAnnotationIntentResolvable(mIsAnnotationIntentResolvable);
    }

    private void validateFileUri(Uri fileUri) {
        if (!Uris.isContentUri(fileUri) && !Uris.isFileUri(fileUri)) {
            throw new IllegalArgumentException("Only content and file uri is supported");
        }

        // TODO confirm this exception
        if (Uris.isFileUriInSamePackageDataDir(fileUri)) {
            throw new SecurityException(
                    "Disallow opening file:// URIs in the parent package's data directory for "
                            + "security reasons");
        }
    }

    private void fetchFile(@NonNull final Uri fileUri) {
        Preconditions.checkNotNull(fileUri);
        final String fileName = getFileName(fileUri);
        final FutureValue<Openable> openable;
        openable = mFetcher.loadLocal(fileUri);

        // Only make this visible when we know a file needs to be fetched.
        // TODO loadingScreen.setVisibility(View.VISIBLE);

        openable.get(
                new FutureValue.Callback<Openable>() {
                    @Override
                    public void available(Openable openable) {
                        viewerAvailable(fileUri, fileName, openable);
                    }

                    @Override
                    public void failed(@NonNull Throwable thrown) {
                        finishActivity();
                    }

                    @Override
                    public void progress(float progress) {
                    }
                });
    }

    private void finishActivity() {
        if (getActivity() != null) {
            getActivity().finish();
        }
    }

    @Nullable
    private ContentResolver getResolver() {
        if (getActivity() != null) {
            return getActivity().getContentResolver();
        }
        return null;
    }

    private String getFileName(@NonNull Uri fileUri) {
        ContentResolver resolver = getResolver();
        return resolver != null ? Uris.extractName(fileUri, resolver) : Uris.extractFileName(
                fileUri);
    }

    private void viewerAvailable(Uri fileUri, String fileName, Openable openable) {
        DisplayData contents = new DisplayData(fileUri, fileName, openable);

        // TODO loadingScreen.setVisibility(View.GONE);

        startViewer(contents);
    }

    private void startViewer(@NonNull DisplayData contents) {
        Preconditions.checkNotNull(contents);

        setQuitOnError(true);
        setExitOnPasswordCancel(false);
        feed(contents);
        postEnter();
    }

    private boolean isPageCreated(int pageNum) {
        return pageNum < mPaginationModel.getSize() && mPaginatedView.getViewAt(pageNum) != null;
    }

    private PageView getPage(int pageNum) {
        return mPaginatedView.getViewAt(pageNum);
    }

    private void lookAtSelection(SelectedMatch selection) {
        if (selection == null || selection.isEmpty()) {
            return;
        }
        if (selection.getPage() >= mPaginationModel.getSize()) {
            mLayoutHandler.layoutPages(selection.getPage() + 1);
            return;
        }
        Rect rect = selection.getPageMatches().getFirstRect(selection.getSelected());
        int x = mPaginationModel.getLookAtX(selection.getPage(), rect.centerX());
        int y = mPaginationModel.getLookAtY(selection.getPage(), rect.centerY());
        mZoomView.centerAt(x, y);

        PageMosaicView pageView = (PageMosaicView) mPageViewFactory.getOrCreatePageView(
                selection.getPage(),
                sScreen.pxFromDp(PAGE_ELEVATION_DP),
                mPaginationModel.getPageSize(selection.getPage()));
        pageView.setOverlay(selection.getOverlay());
    }

    private void loadPageAssets(ZoomScroll position) {
        // Change the resolution of the bitmaps only when a gesture is not in progress.
        if (position.stable || mZoomView.getStableZoom() == 0) {
            mZoomView.setStableZoom(position.zoom);
        }

        mPaginationModel.setViewArea(mZoomView.getVisibleAreaInContentCoords());
        mPaginatedView.refreshPageRangeInVisibleArea(position, mZoomView.getHeight());
        mPaginatedView.handleGonePages(/* clearViews= */ false);
        mPaginatedView.loadInvisibleNearPageRange(mZoomView.getStableZoom());

        // The step (4) below requires page Views to be created and laid out. So we create them here
        // and set this flag if that operation needs to wait for a layout pass.
        boolean requiresLayoutPass = mPaginatedView.createPageViewsForVisiblePageRange();

        // 4. Refresh tiles and/or full pages.
        if (position.stable) {
            // Perform a full refresh on all visible pages
            mPaginatedView.refreshVisiblePages(requiresLayoutPass, viewState().get(),
                    mZoomView.getStableZoom());
            mPaginatedView.handleGonePages(/* clearViews= */ true);
        } else if (mZoomView.getStableZoom() == position.zoom) {
            // Just load a few more tiles in case of tile-scroll
            mPaginatedView.refreshVisibleTiles(requiresLayoutPass, viewState().get());
        }

        mLayoutHandler.maybeLayoutPages(
                mPaginatedView.getPageRangeHandler().getVisiblePages().getLast());
    }

    /** Show the loading spinner. */
    @UiThread
    public void showSpinner() {
        if (mLoadingSpinner != null) {
            mLoadingSpinner.post(() -> mLoadingSpinner.setVisibility(View.VISIBLE));
        }
    }

    /** Hide the loading spinner. */
    @UiThread
    public void hideSpinner() {
        if (mLoadingSpinner != null) {
            mLoadingSpinner.post(() -> mLoadingSpinner.setVisibility(View.GONE));
        }
    }

    private FindInFileListener makeFindInFileListener() {
        return new FindInFileListener() {
            @Override
            public boolean onQueryTextChange(@Nullable String query) {
                if (mSearchModel != null) {
                    mSearchModel.setQuery(query,
                            mPaginatedView.getPageRangeHandler().getVisiblePage());
                    return true;
                }
                return false;
            }

            @Override
            public boolean onFindNextMatch(String query, boolean backwards) {
                if (mSearchModel != null) {
                    CycleRange.Direction direction;
                    if (backwards) {
                        direction = CycleRange.Direction.BACKWARDS;
                        // TODO: Track "find previous" action event.
                    } else {
                        direction = CycleRange.Direction.FORWARDS;
                        // TODO: Track "find next" action event.
                    }
                    mSearchModel.selectNextMatch(direction,
                            mPaginatedView.getPageRangeHandler().getVisiblePage());
                    return true;
                }
                return false;
            }

            @Nullable
            @Override
            public ObservableValue<MatchCount> matchCount() {
                return mSearchModel != null ? mSearchModel.matchCount() : null;
            }
        };
    }

    // TODO: Revisit this method for its usage. Currently redundant

    { // Init pdfLoaderCallbacks
        mPdfLoaderCallbacks =
                new PdfLoaderCallbacks() {
                    static final String PASSWORD_DIALOG_TAG = "password-dialog";

                    @Nullable
                    private PdfPasswordDialog currentPasswordDialog(@Nullable FragmentManager fm) {
                        if (fm != null) {
                            Fragment passwordDialog = fm.findFragmentByTag(PASSWORD_DIALOG_TAG);
                            if (passwordDialog instanceof PdfPasswordDialog) {
                                return (PdfPasswordDialog) passwordDialog;
                            }
                        }
                        return null;
                    }

                    // Callbacks should exit early if viewState == NO_VIEW (typically a Destroy
                    // is in progress).
                    @Override
                    public void requestPassword(boolean incorrect) {
                        mIsPasswordProtected = true;

                        if (!isShowing()) {
                            // This would happen if the service decides to start while we're in
                            // the background.
                            // The dialog code below would then crash. We can't just bypass it
                            // because then we'd
                            // have
                            // a started service with no loaded PDF and no means to load it. The
                            // best way is to
                            // just
                            // kill the service which will restart on the next onStart.
                            if (mPdfLoader != null) {
                                mPdfLoader.disconnect();
                            }
                            return;
                        }

                        if (viewState().get() != ViewState.NO_VIEW) {
                            FragmentManager fm = requireActivity().getSupportFragmentManager();

                            PdfPasswordDialog passwordDialog = currentPasswordDialog(fm);
                            if (passwordDialog == null) {
                                passwordDialog = new PdfPasswordDialog();
                                passwordDialog.setTargetFragment(PdfViewer.this, 0);
                                passwordDialog.setFinishOnCancel(
                                        getArguments().getBoolean(KEY_EXIT_ON_CANCEL));
                                passwordDialog.show(fm, PASSWORD_DIALOG_TAG);
                            }

                            if (incorrect) {
                                passwordDialog.retry();
                            }
                        }
                    }

                    @Override
                    public void documentLoaded(int numPages) {
                        if (numPages <= 0) {
                            documentNotLoaded(PdfStatus.PDF_ERROR);
                            return;
                        }

                        mDocumentLoaded = true;
                        hideSpinner();

                        // Assume we see at least the first page
                        mPaginatedView.getPageRangeHandler().setMaxPage(1);
                        if (viewState().get() != ViewState.NO_VIEW) {
                            mPaginationModel.initialize(numPages);
                            mPaginatedView.setModel(mPaginationModel);
                            mPaginationModel.addObserver(mPaginatedView);

                            dismissPasswordDialog();
                            mLayoutHandler.maybeLayoutPages(1);
                            mPageIndicator.setNumPages(numPages);
                            mSearchModel.setNumPages(numPages);
                        }

                        if (mShouldRedrawOnDocumentLoaded) {
                            mShouldRedrawOnDocumentLoaded = false;
                        }

                        if (mIsAnnotationIntentResolvable) {
                            mAnnotationButton.setVisibility(VISIBLE);
                        }
                    }

                    @Override
                    public void documentNotLoaded(@NonNull PdfStatus status) {
                        if (viewState().get() != ViewState.NO_VIEW) {
                            dismissPasswordDialog();
                            if (getArguments().getBoolean(KEY_QUIT_ON_ERROR)) {
                                getActivity().finish();
                            }
                            switch (status) {
                                case NONE:
                                case FILE_ERROR:
                                    handleError();
                                    break;
                                case PDF_ERROR:
                                    Toaster.LONG.popToast(
                                            getActivity(), R.string.error_file_format_pdf,
                                            mFileData.getName());
                                    break;
                                case LOADED:
                                case REQUIRES_PASSWORD:
                                    Preconditions.checkArgument(
                                            false,
                                            "Document not loaded but status " + status.getNumber());
                                    break;
                                case PAGE_BROKEN:
                                case NEED_MORE_DATA:
                                    // no op.
                            }
                            // TODO: Tracker render error.
                        }
                    }

                    @Override
                    public void pageBroken(int page) {
                        if (viewState().get() != ViewState.NO_VIEW) {
                            ((PageMosaicView) mPageViewFactory.getOrCreatePageView(
                                    page,
                                    sScreen.pxFromDp(PAGE_ELEVATION_DP),
                                    mPaginationModel.getPageSize(page)))
                                    .setFailure(getString(R.string.error_on_page, page + 1));
                            Toaster.LONG.popToast(getActivity(), R.string.error_on_page, page + 1);
                            // TODO: Track render error.
                        }
                    }

                    private void dismissPasswordDialog() {
                        DialogFragment passwordDialog = currentPasswordDialog(
                                requireActivity().getSupportFragmentManager());
                        if (passwordDialog != null
                                && PdfViewer.this.equals(passwordDialog.getTargetFragment())) {
                            passwordDialog.dismiss();
                        }
                    }

                    @Override
                    public void setPageDimensions(int pageNum, @NonNull Dimensions dimensions) {
                        if (viewState().get() != ViewState.NO_VIEW) {
                            mPaginationModel.addPage(pageNum, dimensions);
                            mLayoutHandler.setPageLayoutReach(mPaginationModel.getSize());

                            if (mSearchModel.query().get() != null
                                    && mSearchModel.selectedMatch().get() != null
                                    && mSearchModel.selectedMatch().get().getPage() == pageNum) {
                                // lookAtSelection is posted to run once layout has finished:
                                ThreadUtils.postOnUiThread(
                                        () -> {
                                            if (viewState().get() != ViewState.NO_VIEW) {
                                                lookAtSelection(mSearchModel.selectedMatch().get());
                                            }
                                        });
                            }

                            mLayoutHandler.processCallbacksInQueue(viewState().get(), pageNum);

                            // The new page might actually be visible on the screen, so we need
                            // to fetch assets:
                            ZoomScroll position = mZoomView.zoomScroll().get();
                            Range newRange =
                                    mPaginatedView.getPageRangeHandler().computeVisibleRange(
                                            position.scrollY, position.zoom, mZoomView.getHeight(),
                                            true);
                            if (newRange.isEmpty()) {
                                // During fast-scroll, we mostly don't need to fetch assets, but
                                // make sure we keep pushing layout bounds far enough, and update
                                // page numbers as we "scroll" down.
                                if (mPageIndicator.setRangeAndZoom(newRange,
                                        mZoomView.getStableZoom(), false)) {
                                    showFastScrollView();
                                }
                                mLayoutHandler.maybeLayoutPages(newRange.getLast());
                            } else if (newRange.contains(pageNum)) {
                                // The new page is visible, fetch its assets.
                                loadPageAssets(mZoomView.zoomScroll().get());
                            }
                        }
                    }

                    @Override
                    public void setTileBitmap(int pageNum, @NonNull TileInfo tileInfo,
                            @NonNull Bitmap bitmap) {
                        if (viewState().get() != ViewState.NO_VIEW && isPageCreated(pageNum)) {
                            getPage(pageNum).getPageView().setTileBitmap(tileInfo, bitmap);
                        }
                    }

                    @Override
                    public void setPageBitmap(int pageNum, @NonNull Bitmap bitmap) {
                        // We announce that the viewer is ready as soon as a bitmap is loaded
                        // (not before).
                        if (mViewState.get() == ViewState.VIEW_CREATED) {
                            mZoomView.setVisibility(View.VISIBLE);
                            mViewState.set(ViewState.VIEW_READY);
                        }
                        if (viewState().get() != ViewState.NO_VIEW && isPageCreated(pageNum)) {
                            getPage(pageNum).getPageView().setPageBitmap(bitmap);
                        }
                    }

                    @Override
                    public void setPageText(int pageNum, @NonNull String text) {
                        if (viewState().get() != ViewState.NO_VIEW && isPageCreated(pageNum)) {
                            getPage(pageNum).getPageView().setPageText(text);
                        }
                    }

                    @Override
                    public void setSearchResults(@NonNull String query, int pageNum,
                            @NonNull MatchRects matches) {
                        if (viewState().get() != ViewState.NO_VIEW && query.equals(
                                mSearchModel.query().get())) {
                            mSearchModel.updateMatches(query, pageNum, matches);
                            if (isPageCreated(pageNum)) {
                                getPage(pageNum)
                                        .getPageView()
                                        .setOverlay(
                                                mSearchModel.getOverlay(query, pageNum, matches));
                            }
                        }
                    }

                    @Override
                    public void setSelection(int pageNum, @Nullable PageSelection selection) {
                        if (viewState().get() == ViewState.NO_VIEW) {
                            return;
                        }
                        if (selection != null) {
                            // Clear searchModel - we hide the search and show the selection
                            // instead.
                            mSearchModel.setQuery(null, -1);
                        }
                        mSelectionModel.setSelection(selection);
                    }

                    @Override
                    public void setPageUrlLinks(int pageNum, @NonNull LinkRects links) {
                        if (viewState().get() != ViewState.NO_VIEW && links != null
                                && isPageCreated(pageNum)) {
                            getPage(pageNum).setPageUrlLinks(links);
                        }
                    }

                    @Override
                    public void setPageGotoLinks(int pageNum, @NonNull List<GotoLink> links) {
                        if (viewState().get() != ViewState.NO_VIEW && isPageCreated(pageNum)) {
                            getPage(pageNum).setPageGotoLinks(links);
                        }
                    }

                    @Override
                    public void documentCloned(boolean result) {
                        if (mPrintableVersionCallback != null) {
                            mPrintableVersionCallback.set(result);
                        }
                        mPrintableVersionCallback = null;
                    }

                    @Override
                    public void documentSavedAs(boolean result) {
                        if (mSaveAsCallback != null) {
                            mSaveAsCallback.set(result);
                        }
                        mSaveAsCallback = null;
                    }

                    /**
                     * Receives areas of a page that have been invalidated by an editing action
                     * and asks the
                     * appropriate page view to redraw them.
                     */
                    @Override
                    public void setInvalidRects(int pageNum, @NonNull List<Rect> invalidRects) {
                        if (viewState().get() != ViewState.NO_VIEW && isPageCreated(pageNum)) {
                            if (invalidRects == null || invalidRects.isEmpty()) {
                                return;
                            }
                            mPaginatedView.getViewAt(pageNum).getPageView().requestRedrawAreas(
                                    invalidRects);
                        }
                    }
                };
    }

    @Override
    public float estimateFullContentHeight() {
        return mPaginationModel.getEstimatedFullHeight();
    }

    @Override
    public float visibleHeight() {
        return mZoomView.getViewportHeight() / mZoomView.getZoom();
    }

    @Override
    public void fastScrollTo(float position, boolean stable) {
        mZoomView.scrollTo(mZoomView.getScrollX(), (int) (position * mZoomView.getZoom()), stable);
    }

    @Override
    public void setFastScrollListener(final @NonNull FastScrollListener listener) {
        mZoomView
                .getViewTreeObserver()
                .addOnScrollChangedListener(
                        new OnScrollChangedListener() {
                            @Override
                            public void onScrollChanged() {
                                if (mZoomView != null) {
                                    listener.updateFastScrollbar(
                                            mZoomView.getScrollY() / mZoomView.getZoom());
                                }
                            }
                        });
    }

    protected void handleError() {
        mViewState.set(ViewState.ERROR);
    }


    /** Create callback to retry password input when user cancels password prompt. */
    public void setPasswordCancelError() {
        Runnable retryCallback = () -> mPdfLoaderCallbacks.requestPassword(false);
        displayViewerError(ErrorType.FILE_PASSWORD_PROTECTED, this, retryCallback);
    }

    private void displayViewerError(ErrorType errorType, Viewer viewer, Runnable actionCallback) {
        switch (errorType) {
            case FILE_PASSWORD_PROTECTED:
                showSnackBar(R.string.password_not_entered, R.string.retry_button_text,
                        actionCallback);
                return;
            default:
                break;
        }

    }

    private void showSnackBar(int text, int actionText, Runnable actionCallback) {
        mSnackbar = Snackbar.make(mPdfViewer, text, Snackbar.LENGTH_INDEFINITE);
        View.OnClickListener mResolveClickListener =
                v -> {
                    actionCallback.run();
                };
        mSnackbar.setAction(actionText, mResolveClickListener);
        mSnackbar.show();
    }

    private void showFastScrollView() {
        if (mFastScrollView != null) {
            mFastScrollView.setVisible();
        }
    }

    /**
     * Set up the find in file menu.
     */
    public void setFindInFileView(boolean visibility) {
        if (visibility) {
            mFindInFileView.setVisibility(VISIBLE);
            setupFindInFileBtn();
        } else {
            mFindInFileView.setVisibility(GONE);
        }
    }

    private void setupFindInFileBtn() {
        mFindInFileView.setFindInFileListener(this.makeFindInFileListener());
        mFindInFileView.queryBoxRequestFocus();

        TextView queryBox = mFindInFileView.findViewById(R.id.find_query_box);
        ImageView close_button = mFindInFileView.findViewById(R.id.close_btn);
        close_button.setOnClickListener(view -> {
            View parentLayout = (View) close_button.getParent();
            queryBox.clearFocus();
            queryBox.setText("");
            parentLayout.setVisibility(GONE);
            if (mIsAnnotationIntentResolvable) {
                mAnnotationButton.setVisibility(VISIBLE);
            }
        });
    }

    private void setUpEditFab() {
        mAnnotationButton = mPdfViewer.findViewById(R.id.edit_fab);
        mAnnotationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                performEdit();
            }
        });

    }

    private void performEdit() {
        Intent intent = AnnotationUtils.getAnnotationIntent(mLocalUri);
        intent.setData(mLocalUri);
        startActivity(intent);
    }

}
