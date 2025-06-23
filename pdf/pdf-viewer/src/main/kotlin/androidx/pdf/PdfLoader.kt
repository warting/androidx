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

package androidx.pdf

import android.net.Uri
import java.io.IOException

/**
 * Provides an abstraction for asynchronously opening PDF documents from a Uri. Implementations of
 * this interface are responsible for handling the retrieval, decryption (if necessary), and
 * creation of a [PdfDocument] representation.
 */
public interface PdfLoader {

    /**
     * Asynchronously opens a PDF document from the specified [Uri].
     *
     * @param uri The URI of the PDF document to open.
     * @param password (Optional) The password to unlock the document if it is encrypted.
     * @return The opened [PdfDocument].
     * @throws PdfPasswordException If the provided password is incorrect.
     * @throws IOException If an error occurs while opening the document.
     */
    @Throws(IOException::class)
    public suspend fun openDocument(uri: Uri, password: String? = null): PdfDocument
}
