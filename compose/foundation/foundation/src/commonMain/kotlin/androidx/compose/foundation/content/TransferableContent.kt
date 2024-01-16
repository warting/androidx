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
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.ClipMetadata

/**
 * Represents content that can be transferred between applications or processes.
 *
 * Note; Consult platform-specific guidelines for best practices in content transfer operations.
 *
 * @param clipEntry The main content data, typically representing a text, image, file, or other
 * transferable item.
 * @param source The source from which the content originated like Keyboard, DragAndDrop, or
 * Clipboard.
 * @param clipMetadata Metadata associated with the content, providing additional information or
 * context.
 * @param platformTransferableContent Optional platform-specific representation of the content, or
 * additional platform-specific information, that can be used to access platform level APIs.
 */
@ExperimentalFoundationApi
class TransferableContent internal constructor(
    val clipEntry: ClipEntry,
    val clipMetadata: ClipMetadata,
    val source: Source,
    val platformTransferableContent: PlatformTransferableContent? = null
) {

    /**
     * Defines the type of operation that a [TransferableContent] originates from.
     */
    @ExperimentalFoundationApi
    @JvmInline
    value class Source internal constructor(private val value: Int) {

        companion object {

            val Keyboard = Source(0)

            val DragAndDrop = Source(1)

            val Clipboard = Source(2)
        }

        override fun toString(): String = when (this) {
            Keyboard -> "Source.Keyboard"
            DragAndDrop -> "Source.DragAndDrop"
            Clipboard -> "Source.Clipboard"
            else -> "Invalid ($value)"
        }
    }
}

/**
 * All the platform-specific information regarding a [TransferableContent] that cannot be
 * abstracted away in a platform agnostic way.
 */
@ExperimentalFoundationApi
expect class PlatformTransferableContent

/**
 * Returns whether this [TransferableContent] can provide an item with the [mediaType].
 */
@ExperimentalFoundationApi
expect fun TransferableContent.hasMediaType(mediaType: MediaType): Boolean

/**
 * Reads the text part of this [TransferableContent]. The returned result may not include the full
 * text representation of content e.g., if there is a URL pointing at another source. This function
 * only reads the explicit text that was transferred directly inside the [TransferableContent].
 */
@OptIn(ExperimentalFoundationApi::class)
internal expect fun TransferableContent.readPlainText(): String?
