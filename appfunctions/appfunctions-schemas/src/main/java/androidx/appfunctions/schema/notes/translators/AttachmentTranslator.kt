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

package androidx.appfunctions.schema.notes.translators

import androidx.annotation.RequiresApi
import androidx.appfunctions.AppFunctionData
import androidx.appfunctions.schema.notes.translators.UriTranslator.upgradeUri

@RequiresApi(33)
internal object AttachmentTranslator {
    fun AttachmentImpl.downgradeAttachment(): AppFunctionData {
        return AppFunctionData.Builder(qualifiedName = "")
            .setAppFunctionData("uri", UriTranslator.downgradeUri(uri))
            .setString("displayName", displayName)
            .apply { mimeType?.let { setString("mimeType", it) } }
            .build()
    }

    fun AppFunctionData.upgradeAttachment() =
        AttachmentImpl(
            uri = upgradeUri(checkNotNull(getAppFunctionData("uri"))),
            displayName = checkNotNull(getString("displayName")),
            mimeType = getString("mimeType")
        )
}
