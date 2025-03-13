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

package androidx.pdf.viewer.fragment

import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.widget.LinearLayout.GONE
import android.widget.LinearLayout.VISIBLE
import android.widget.ProgressBar
import android.widget.TextView
import androidx.annotation.RequiresExtension
import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import androidx.core.os.OperationCanceledException
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsAnimationCompat.Callback.DISPATCH_MODE_CONTINUE_ON_SUBTREE
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.pdf.util.AnnotationUtils
import androidx.pdf.view.PdfView
import androidx.pdf.view.Selection
import androidx.pdf.view.ToolBoxView
import androidx.pdf.view.search.PdfSearchView
import androidx.pdf.viewer.PdfPasswordDialog
import androidx.pdf.viewer.PdfPasswordDialog.KEY_CANCELABLE
import androidx.pdf.viewer.fragment.insets.TranslateInsetsAnimationCallback
import androidx.pdf.viewer.fragment.model.PdfFragmentUiState.DocumentError
import androidx.pdf.viewer.fragment.model.PdfFragmentUiState.DocumentLoaded
import androidx.pdf.viewer.fragment.model.PdfFragmentUiState.Loading
import androidx.pdf.viewer.fragment.model.PdfFragmentUiState.PasswordRequested
import androidx.pdf.viewer.fragment.model.SearchViewUiState
import androidx.pdf.viewer.fragment.search.PdfSearchViewManager
import androidx.pdf.viewer.fragment.toolbox.ToolboxGestureEventProcessor
import androidx.pdf.viewer.fragment.toolbox.ToolboxGestureEventProcessor.MotionEventType.ScrollTo
import androidx.pdf.viewer.fragment.toolbox.ToolboxGestureEventProcessor.MotionEventType.SingleTap
import androidx.pdf.viewer.fragment.toolbox.ToolboxGestureEventProcessor.ToolboxGestureDelegate
import androidx.pdf.viewer.fragment.util.getCenter
import androidx.pdf.viewer.fragment.view.PdfViewManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * A Fragment that renders a PDF document.
 *
 * <p>A [PdfViewerFragment] that can display paginated PDFs. The viewer includes a FAB for
 * annotation support and a search menu. Each page is rendered in its own View. Upon creation, this
 * fragment displays a loading spinner.
 *
 * <p>Rendering is done in 2 passes:
 * <ol>
 * <li>Layout: Request the page data, get the dimensions and set them as measure for the image view.
 * <li>Render: Create bitmap(s) at adequate dimensions and attach them to the page view.
 * </ol>
 *
 * <p>The layout pass is progressive: starts with a few first pages of the document, then reach
 * further as the user scrolls down (and ultimately spans the whole document). The rendering pass is
 * tightly limited to the currently visible pages. Pages that are scrolled past (become not visible)
 * have their bitmaps released to free up memory.
 *
 * <p>Note that every activity/fragment that uses this class has to be themed with Theme.AppCompat
 * or a theme that extends that theme.
 *
 * @see documentUri
 */
@RequiresExtension(extension = Build.VERSION_CODES.S, version = 13)
public open class PdfViewerFragment constructor() : Fragment() {

    /**
     * Protected constructor for instantiating a [PdfViewerFragment] with the specified styling
     * options.
     *
     * @param pdfStylingOptions The styling options to be applied to the PDF viewer.
     */
    protected constructor(pdfStylingOptions: PdfStylingOptions) : this() {
        val args =
            Bundle().also { it.putInt(KEY_PDF_VIEW_STYLE, pdfStylingOptions.containerStyleResId) }
        arguments = args
    }

    private var isAnnotationIntentResolvable = false

    /**
     * The URI of the PDF document to display defaulting to `null`.
     *
     * When this property is set, the fragment begins loading the PDF document. A visual indicator
     * is displayed while the document is being loaded. Once the loading is fully completed, the
     * [onLoadDocumentSuccess] callback is invoked. If an error occurs during the loading phase, the
     * [onLoadDocumentError] callback is invoked with the exception.
     *
     * <p>Note: This property is recommended to be set when the fragment is in the started state.
     */
    public var documentUri: Uri?
        get() = documentViewModel.documentUriFromState
        set(value) {
            documentViewModel.loadDocument(uri = value, password = null)
        }

    /**
     * Controls whether text search mode is active. Defaults to false.
     *
     * When text search mode is activated, the search menu becomes visible, and search functionality
     * is enabled. Deactivating text search mode hides the search menu, clears search results, and
     * removes any search-related highlights.
     *
     * <p>Note: This property can only be set after the document has successfully loaded
     * i.e.[onLoadDocumentSuccess] is triggered. Any attempts to change it beforehand will have no
     * effect.
     */
    public var isTextSearchActive: Boolean
        get() = documentViewModel.isTextSearchActiveFromState
        set(value) {
            documentViewModel.updateSearchState(value)
            // entering the immersive mode when search is active and exiting when search is closes
            documentViewModel.setImmersiveModeDesired(enterImmersive = value)
        }

    /**
     * Indicates whether the toolbox should be visible.
     *
     * The host app can control this property to show/hide the toolbox based on its state and the
     * `onRequestImmersiveMode` callback. The setter updates the UI elements within the fragment
     * accordingly.
     */
    public var isToolboxVisible: Boolean
        // We can't use toolbox.visibility because toolboxView is the layout here, and
        // its visibility doesn't change.
        get() = if (::toolboxView.isInitialized) toolboxView.toolboxVisibility == VISIBLE else false
        set(value) {
            if (value && isAnnotationIntentResolvable) toolboxView.show() else toolboxView.hide()
        }

    /**
     * Called when the PDF view wants to enter or exit immersive mode based on user's interaction
     * with the content. Apps would typically hide their top bar or other navigational interface
     * when in immersive mode. The default implementation keeps toolbox visibility in sync with the
     * enterImmersive mode. It is recommended that apps keep this behaviour by calling
     * super.onRequestImmersiveMode while overriding this method.
     *
     * @param enterImmersive true to enter immersive mode, false to exit.
     */
    public open fun onRequestImmersiveMode(enterImmersive: Boolean) {
        // Update toolbox visibility
        isToolboxVisible = !enterImmersive
    }

    /**
     * Invoked when the document has been fully loaded, processed, and the initial pages are
     * displayed within the viewing area. This callback signifies that the document is ready for
     * user interaction.
     *
     * <p>Note that this callback is dispatched only when the fragment is fully created and not yet
     * destroyed, i.e., after [onCreate] has fully run and before [onDestroy] runs, and only on the
     * main thread.
     */
    public open fun onLoadDocumentSuccess() {}

    /**
     * Invoked when a problem arises during the loading process of the PDF document. This callback
     * provides details about the encountered error, allowing for appropriate error handling and
     * user notification.
     *
     * <p>Note that this callback is dispatched only when the fragment is fully created and not yet
     * destroyed, i.e., after [onCreate] has fully run and before [onDestroy] runs, and only on the
     * main thread.
     *
     * @param error [Throwable] that occurred during document loading.
     */
    @Suppress("UNUSED_PARAMETER") public open fun onLoadDocumentError(error: Throwable) {}

    private val documentViewModel: PdfDocumentViewModel by viewModels {
        PdfDocumentViewModel.Factory
    }

    @VisibleForTesting
    protected val pdfView: PdfView
        @RestrictTo(RestrictTo.Scope.LIBRARY) get() = _pdfView

    @VisibleForTesting
    protected val pdfSearchView: PdfSearchView
        @RestrictTo(RestrictTo.Scope.LIBRARY) get() = _pdfSearchView

    private lateinit var _pdfView: PdfView
    private lateinit var _pdfSearchView: PdfSearchView
    private lateinit var toolboxView: ToolBoxView
    private lateinit var errorView: TextView
    private lateinit var loadingView: ProgressBar
    private lateinit var pdfViewManager: PdfViewManager
    private lateinit var pdfSearchViewManager: PdfSearchViewManager

    private var searchStateCollector: Job? = null
    private var highlightStateCollector: Job? = null
    private var toolboxStateCollector: Job? = null

    // Provides visible pages in viewport both end inclusive.
    private val PdfView.visiblePages: IntRange
        get() = IntRange(firstVisiblePage, firstVisiblePage + visiblePagesCount - 1)

    private val searchQueryTextWatcher =
        object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // No-Op.
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                documentViewModel.searchDocument(
                    query = s.toString(),
                    visiblePageRange = _pdfView.visiblePages
                )
            }

            override fun afterTextChanged(s: Editable?) {
                // No-Op.
            }
        }

    private var toolboxGestureEventProcessor: ToolboxGestureEventProcessor =
        ToolboxGestureEventProcessor(
            toolboxGestureDelegate =
                object : ToolboxGestureDelegate {
                    override fun onSingleTap() {
                        documentViewModel.toggleImmersiveModeState()
                        _pdfSearchView.clearFocus()
                    }

                    override fun onScroll(position: Int) {
                        documentViewModel.setImmersiveModeDesired(enterImmersive = (position > 0))
                    }
                }
        )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        return inflater.inflate(R.layout.pdf_viewer_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        with(view) {
            _pdfView = findViewById(R.id.pdfView)
            errorView = findViewById(R.id.errorTextView)
            loadingView = findViewById(R.id.pdfLoadingProgressBar)
            _pdfSearchView = findViewById(R.id.pdfSearchView)
            toolboxView = findViewById(R.id.toolBoxView)
        }
        val gestureDetector =
            GestureDetector(
                activity,
                object : GestureDetector.SimpleOnGestureListener() {
                    override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                        toolboxGestureEventProcessor.processEvent(SingleTap)
                        // we should not consume this event as the events are required in PdfView
                        return false
                    }
                }
            )

        _pdfView.setOnTouchListener { _, event ->
            // we should not consume this event as the events are required in PdfView
            gestureDetector.onTouchEvent(event)
        }
        _pdfView.setOnScrollChangeListener { _, _, scrollY, _, _ ->
            toolboxGestureEventProcessor.processEvent(ScrollTo(scrollY))
        }

        pdfViewManager =
            PdfViewManager(
                pdfView = _pdfView,
                selectedHighlightColor =
                    requireContext().getColor(R.color.selected_highlight_color),
                highlightColor = requireContext().getColor(R.color.highlight_color)
            )
        pdfSearchViewManager = PdfSearchViewManager(_pdfSearchView)

        setupPdfViewListeners()

        onPdfSearchViewCreated(_pdfSearchView)

        collectFlowOnLifecycleScope { collectFragmentUiScreenState() }
        toolboxView.hide()
        toolboxView.setOnCurrentPageRequested { _pdfView.visiblePages.getCenter() }
    }

    override fun onResume() {
        // This ensures that the focus request occurs before the view becomes visible,
        // providing a smoother search animation without noticeable jerking.
        if (
            (documentViewModel.searchViewUiState.value !is SearchViewUiState.Closed) &&
                !_pdfSearchView.searchQueryBox.hasFocus()
        )
            _pdfSearchView.searchQueryBox.requestFocus()

        super.onResume()
        pdfView.pdfDocument?.uri?.let { uri -> setAnnotationIntentResolvability(uri) }
    }

    /**
     * Called from Fragment.onViewCreated(). This gives subclasses a chance to customize component.
     */
    private fun onPdfSearchViewCreated(searchView: PdfSearchView) {
        setupSearchViewListeners(searchView)
        val windowManager = activity?.getSystemService(Context.WINDOW_SERVICE) as WindowManager

        activity?.let {
            // Attach the callback to the decorView to reliably receive insets animation events,
            // such as those triggered by soft keyboard input.
            ViewCompat.setWindowInsetsAnimationCallback(
                searchView,
                TranslateInsetsAnimationCallback(
                    view = searchView,
                    windowManager = windowManager,
                    pdfContainer = view,
                    // As the decorView is a top-level view, insets must not be consumed here.
                    // They must be propagated to child views for adjustments at their level.
                    dispatchMode = DISPATCH_MODE_CONTINUE_ON_SUBTREE
                )
            )
        }
    }

    private fun setupPdfViewListeners() {
        /**
         * Closes any active search session if the user selects anything in the PdfView. This
         * improves the user experience by allowing the focus to shift to the intended content.
         */
        _pdfView.addOnSelectionChangedListener(
            object : PdfView.OnSelectionChangedListener {
                override fun onSelectionChanged(
                    previousSelection: Selection?,
                    newSelection: Selection?
                ) {
                    newSelection?.let { isTextSearchActive = false }
                }
            }
        )
    }

    private fun setupSearchViewListeners(searchView: PdfSearchView) {
        with(searchView) {
            searchQueryBox.addTextChangedListener(searchQueryTextWatcher)

            searchQueryBox.setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    performSearch()
                }
                true // IME action consumed
            }
            findPrevButton.setOnClickListener {
                searchQueryBox.clearFocus()
                documentViewModel.findPreviousMatch()
            }
            findNextButton.setOnClickListener {
                searchQueryBox.clearFocus()
                documentViewModel.findNextMatch()
            }
            closeButton.setOnClickListener { isTextSearchActive = false }
        }
    }

    private fun PdfSearchView.performSearch() {
        searchQueryBox.clearFocus()

        searchDocument(searchQueryBox.text.toString())
    }

    private fun searchDocument(query: String) {
        documentViewModel.searchDocument(query = query, visiblePageRange = _pdfView.visiblePages)
    }

    private fun collectViewStates() {
        searchStateCollector = collectFlowOnLifecycleScope {
            documentViewModel.searchViewUiState.collect { uiState ->
                pdfSearchViewManager.setState(uiState)

                /** Clear selection when we start a search session. Also hide the fast scroller. */
                if (uiState !is SearchViewUiState.Closed) {
                    _pdfView.apply {
                        clearSelection()
                        forcedFastScrollVisibility = false
                    }
                } else {
                    // Let PdfView internally control fast scroller visibility.
                    _pdfView.forcedFastScrollVisibility = null
                }
            }
        }

        highlightStateCollector = collectFlowOnLifecycleScope {
            documentViewModel.highlightsFlow.collect { highlightData ->
                pdfViewManager.apply {
                    setHighlights(highlightData)
                    scrollToCurrentSearchResult(highlightData)
                }
            }
        }

        toolboxStateCollector = collectFlowOnLifecycleScope {
            documentViewModel.immersiveModeFlow.collect { immersiveModeState ->
                onRequestImmersiveMode(immersiveModeState)
            }
        }
    }

    private fun cancelViewStateCollection() {
        searchStateCollector?.cancel()
        searchStateCollector = null
        highlightStateCollector?.cancel()
        highlightStateCollector = null
        toolboxStateCollector?.cancel()
        toolboxStateCollector = null
    }

    private fun getPasswordDialog(): PdfPasswordDialog {
        return (childFragmentManager.findFragmentByTag(PASSWORD_DIALOG_TAG) as? PdfPasswordDialog)
            ?: PdfPasswordDialog().apply {
                arguments = Bundle().apply { putBoolean(KEY_CANCELABLE, false) }
            }
    }

    private fun dismissPasswordDialog() {
        val passwordDialog =
            childFragmentManager.findFragmentByTag(PASSWORD_DIALOG_TAG) as? PdfPasswordDialog
        passwordDialog?.dismiss()
    }

    private fun requestPassword(isPasswordIncorrectRetry: Boolean) {

        val passwordDialog = getPasswordDialog()
        if (!passwordDialog.isAdded) {
            passwordDialog.show(childFragmentManager, PASSWORD_DIALOG_TAG)
        }
        if (isPasswordIncorrectRetry) {
            passwordDialog.showIncorrectMessage()
        }

        passwordDialog.setListener(
            object : PdfPasswordDialog.PasswordDialogEventsListener {
                override fun onPasswordSubmit(password: String) {
                    documentViewModel.loadDocument(uri = documentUri, password = password)
                }

                override fun onDialogCancelled() {
                    documentViewModel.passwordDialogCancelled()
                }

                override fun onDialogShown() {}
            }
        )
    }

    /**
     * Collects the UI state of the fragment and updates the views accordingly.
     *
     * This is a suspend function that continuously observes the fragment's UI state and updates the
     * corresponding views to reflect the latest state. This ensures that the UI remains
     * synchronized with any changes in the underlying data or user interactions.
     */
    private suspend fun collectFragmentUiScreenState() {
        documentViewModel.fragmentUiScreenState.collect { uiState ->
            when (uiState) {
                is Loading -> handleLoading()
                is PasswordRequested -> handlePasswordRequested(uiState)
                is DocumentLoaded -> handleDocumentLoaded(uiState)
                is DocumentError -> handleDocumentError(uiState)
            }
        }
    }

    private fun handleLoading() {
        setViewVisibility(
            pdfView = GONE,
            loadingView = VISIBLE,
            errorView = GONE,
        )
        // Cancel view state collection upon new document load.
        // These state should only be relevant if document is loaded successfully.
        cancelViewStateCollection()
    }

    private fun handlePasswordRequested(uiState: PasswordRequested) {
        requestPassword(uiState.passwordFailed)
        setViewVisibility(pdfView = GONE, loadingView = GONE, errorView = GONE)
        // Utilize retry param to show incorrect password on PasswordDialog
    }

    private fun handleDocumentLoaded(uiState: DocumentLoaded) {
        dismissPasswordDialog()
        onLoadDocumentSuccess()
        _pdfView.pdfDocument = uiState.pdfDocument
        toolboxView.setPdfDocument(uiState.pdfDocument)
        setAnnotationIntentResolvability(uiState.pdfDocument.uri)
        setViewVisibility(
            pdfView = VISIBLE,
            loadingView = GONE,
            errorView = GONE,
        )
        // Start collection of view states like search, toolbox, etc. once document is loaded.
        collectViewStates()
    }

    private fun setAnnotationIntentResolvability(uri: Uri) {
        isAnnotationIntentResolvable =
            AnnotationUtils.resolveAnnotationIntent(requireContext(), uri)
        if (!isAnnotationIntentResolvable) {
            toolboxView.hide()
        }
    }

    private fun handleDocumentError(uiState: DocumentError) {
        dismissPasswordDialog()
        if (uiState.exception is OperationCanceledException) {
            onLoadDocumentError(uiState.exception)
        } else {
            onLoadDocumentError(
                RuntimeException(
                    context?.resources?.getString(androidx.pdf.R.string.pdf_error)
                        ?: uiState.exception.message,
                    uiState.exception
                )
            )
        }

        setViewVisibility(
            pdfView = GONE,
            loadingView = GONE,
            errorView = VISIBLE,
        )
    }

    private fun setViewVisibility(
        pdfView: Int,
        loadingView: Int,
        errorView: Int,
    ) {
        this._pdfView.visibility = pdfView
        this.loadingView.visibility = loadingView
        this.errorView.visibility = errorView
    }

    private fun collectFlowOnLifecycleScope(block: suspend () -> Unit): Job {
        return viewLifecycleOwner.lifecycleScope.launch {
            /**
             * [repeatOnLifecycle] launches the block in a new coroutine every time the lifecycle is
             * in the STARTED state (or above) and cancels it when it's STOPPED.
             */
            repeatOnLifecycle(Lifecycle.State.STARTED) { block() }
        }
    }

    public companion object {
        private const val PASSWORD_DIALOG_TAG = "password-dialog"
        private const val KEY_PDF_VIEW_STYLE = "keyPdfViewStyle"

        /**
         * Creates a new instance of [PdfViewerFragment] with the specified styling options.
         *
         * @param pdfStylingOptions The styling options to be applied.
         * @return A new instance of [PdfViewerFragment] with the provided styling options.
         */
        @JvmStatic
        public fun newInstance(pdfStylingOptions: PdfStylingOptions): PdfViewerFragment {
            val fragment = PdfViewerFragment()
            val args =
                Bundle().also {
                    it.putInt(KEY_PDF_VIEW_STYLE, pdfStylingOptions.containerStyleResId)
                }

            fragment.arguments = args
            return fragment
        }
    }
}
