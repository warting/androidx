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

package androidx.compose.foundation.content

import androidx.compose.foundation.ExperimentalFoundationApi

/**
 * Definition of common MediaTypes on the Android platform.
 *
 * @param representation MimeType string that conforms to RFC 2045.
 */
@ExperimentalFoundationApi
actual class MediaType actual constructor(actual val representation: String) {

    actual companion object {
        actual val Text: MediaType = MediaType("text/*")

        actual val PlainText: MediaType = MediaType("text/plain")

        actual val HtmlText: MediaType = MediaType("text/html")

        actual val Image: MediaType = MediaType("image/*")

        actual val All: MediaType = MediaType("*/*")
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is MediaType) return false

        return representation == other.representation
    }

    override fun hashCode(): Int {
        return representation.hashCode()
    }

    override fun toString(): String {
        return "MediaType(representation='$representation')"
    }
}
