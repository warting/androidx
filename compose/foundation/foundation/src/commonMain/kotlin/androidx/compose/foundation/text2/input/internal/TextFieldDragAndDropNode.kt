/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.compose.foundation.text2.input.internal

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.content.MediaType
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropModifierNode
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.ClipMetadata

@OptIn(ExperimentalFoundationApi::class)
internal expect fun textFieldDragAndDropNode(
    hintMediaTypes: () -> Set<MediaType>,
    onDrop: (clipEntry: ClipEntry, clipMetadata: ClipMetadata) -> Boolean,
    dragAndDropRequestPermission: (DragAndDropEvent) -> Unit,
    onStarted: ((event: DragAndDropEvent) -> Unit)? = null,
    onEntered: ((event: DragAndDropEvent) -> Unit)? = null,
    onMoved: ((position: Offset) -> Unit)? = null,
    onChanged: ((event: DragAndDropEvent) -> Unit)? = null,
    onExited: ((event: DragAndDropEvent) -> Unit)? = null,
    onEnded: ((event: DragAndDropEvent) -> Unit)? = null,
): DragAndDropModifierNode
