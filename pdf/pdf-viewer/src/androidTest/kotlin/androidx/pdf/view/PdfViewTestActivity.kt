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

package androidx.pdf.view

import android.app.Activity
import android.content.Context
import android.os.Bundle

/** Bare bones test helper [Activity] for [PdfView] integration tests */
class PdfViewTestActivity : Activity() {

    override fun attachBaseContext(newBase: Context?) {
        onAttachCallback?.let {
            val wrappedContext = it.invoke(newBase!!)
            return super.attachBaseContext(wrappedContext)
        }
        super.attachBaseContext(newBase)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        onCreateCallback(this)
        // disable enter animation.
        @Suppress("Deprecation") overridePendingTransition(0, 0)
    }

    override fun finish() {
        super.finish()
        // disable exit animation.
        @Suppress("Deprecation") overridePendingTransition(0, 0)
    }

    companion object {
        var onCreateCallback: ((PdfViewTestActivity) -> Unit) = {}

        // TODO(b/419791000): Modify logic to avoid static callbacks
        var onAttachCallback: ((Context) -> Context)? = null
    }
}
