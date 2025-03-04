/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.pdf

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.pdf.idlingresource.PdfIdlingResource
import androidx.pdf.testapp.R
import androidx.pdf.view.PdfView
import androidx.pdf.view.PdfView.OnScrollStateChangedListener
import androidx.pdf.viewer.fragment.PdfViewerFragmentV2
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.util.UUID

/**
 * A subclass fragment from [PdfViewerFragmentV2] to include [androidx.test.espresso.IdlingResource]
 * while loading pdf document.
 *
 * TODO(b/386721657) Remove this when PdfViewerFragment is replaced with PdfViewerFragmentV2.
 */
internal class TestPdfViewerFragmentV2 : PdfViewerFragmentV2() {

    val pdfLoadingIdlingResource = PdfIdlingResource(PDF_LOAD_RESOURCE_NAME)
    val pdfScrollIdlingResource = PdfIdlingResource(PDF_SCROLL_RESOURCE_NAME)
    val pdfSearchFocusIdlingResource = PdfIdlingResource(PDF_SEARCH_FOCUS_RESOURCE_NAME)

    private var hostView: FrameLayout? = null
    private var search: FloatingActionButton? = null

    var documentLoaded = false
    var documentError: Throwable? = null

    fun getPdfViewInstance(): PdfView = pdfView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = super.onCreateView(inflater, container, savedInstanceState) as ConstraintLayout

        // Inflate the custom layout for this fragment
        hostView = inflater.inflate(R.layout.fragment_host, container, false) as FrameLayout
        hostView?.let { hostView -> handleInsets(hostView) }

        // Add the default PDF viewer to the custom layout
        hostView?.addView(view)
        return hostView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        search = hostView?.findViewById(R.id.host_Search)

        // Show/hide the search button based on initial toolbox visibility
        if (isToolboxVisible) search?.show() else search?.hide()

        // Set up search button click listener
        search?.setOnClickListener { isTextSearchActive = true }

        pdfView.scrollStateChangedListener =
            object : OnScrollStateChangedListener {
                override fun onScrollStateChanged(x: Int, y: Int, isStable: Boolean) {
                    if (isStable) {
                        pdfScrollIdlingResource.decrement()
                    }
                }
            }
        pdfSearchView.searchQueryBox.onFocusChangeListener =
            object : View.OnFocusChangeListener {
                override fun onFocusChange(v: View?, hasFocus: Boolean) {
                    if (!hasFocus) {
                        pdfSearchFocusIdlingResource.decrement()
                    }
                }
            }
    }

    override fun onRequestImmersiveMode(enterImmersive: Boolean) {
        super.onRequestImmersiveMode(enterImmersive)
        if (!enterImmersive) search?.show() else search?.hide()
    }

    override fun onLoadDocumentSuccess() {
        documentLoaded = true
        pdfLoadingIdlingResource.decrement()
    }

    override fun onLoadDocumentError(error: Throwable) {
        documentError = error
        pdfLoadingIdlingResource.decrement()
    }

    companion object {
        // Resource name must be unique to avoid conflicts while running multiple test scenarios
        private val PDF_LOAD_RESOURCE_NAME = "PdfLoad-${UUID.randomUUID()}"
        private val PDF_SCROLL_RESOURCE_NAME = "PdfScroll-${UUID.randomUUID()}"
        private val PDF_SEARCH_FOCUS_RESOURCE_NAME = "PdfSearchFocus-${UUID.randomUUID()}"

        fun handleInsets(hostView: View) {
            ViewCompat.setOnApplyWindowInsetsListener(hostView) { view, insets ->
                // Get the insets for the system bars (status bar, navigation bar)
                val systemBarsInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars())

                // Adjust the padding of the container view to accommodate system windows
                view.setPadding(
                    view.paddingLeft,
                    systemBarsInsets.top,
                    view.paddingRight,
                    systemBarsInsets.bottom
                )
                insets
            }
        }
    }
}
