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
@file:Suppress("BanConcurrentHashMap")

package androidx.pdf.service

import android.graphics.pdf.LoadParams
import android.graphics.pdf.PdfRenderer
import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresExtension
import androidx.annotation.RestrictTo
import java.util.concurrent.ConcurrentHashMap

@RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
@RequiresExtension(extension = Build.VERSION_CODES.S, version = 13)
@RestrictTo(RestrictTo.Scope.LIBRARY)
internal class PdfRendererWrapperPostV(
    parcelFileDescriptor: ParcelFileDescriptor,
    password: String,
) : PdfRendererWrapper {

    private val pdfRenderer: PdfRenderer

    private val cachedPageMap = ConcurrentHashMap<Int, PdfPageWrapper>()

    init {
        val params = LoadParams.Builder().setPassword(password).build()
        pdfRenderer = PdfRenderer(parcelFileDescriptor, params)
    }

    /** Caller should use [releasePage] to close the page resource reliably after usage. */
    override fun openPage(pageNum: Int, useCache: Boolean): PdfPageWrapper {
        return if (useCache) {
            openPageWithCache(pageNum)
        } else {
            PdfPageWrapperPostV(pdfRenderer.openPage(pageNum))
        }
    }

    private fun openPageWithCache(pageNum: Int): PdfPageWrapper {
        // Check cache first
        var page = cachedPageMap[pageNum]
        if (page != null) {
            return page
        }

        // Not in cache, create a new page using executeOnRenderer
        page = PdfPageWrapperPostV(pdfRenderer.openPage(pageNum))

        // Add to cache
        cachedPageMap[pageNum] = page
        return page
    }

    /** Closes the page. Also removes and clears the cached instance, if held. */
    override fun releasePage(page: PdfPageWrapper?, pageNum: Int) {
        page?.close()
        val removedPage = cachedPageMap.remove(pageNum)
        removedPage?.close()
    }

    override val documentLinearizationType: Int
        get() = pdfRenderer.documentLinearizationType

    override val pageCount: Int
        get() = pdfRenderer.pageCount

    override val documentFormType: Int
        get() = pdfRenderer.pdfFormType

    override fun close() {
        pdfRenderer.close()
    }
}
