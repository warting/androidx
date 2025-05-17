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

package androidx.compose.foundation.text.contextmenu.modifier

import android.content.res.Resources
import androidx.compose.foundation.text.contextmenu.builder.TextContextMenuBuilderScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.LocalResources

internal fun Modifier.addTextContextMenuComponentsWithResources(
    builder: TextContextMenuBuilderScope.(Resources) -> Unit
): Modifier = this then AddTextContextMenuDataComponentsWithResourcesElement(builder)

private class AddTextContextMenuDataComponentsWithResourcesElement(
    private val builder: TextContextMenuBuilderScope.(Resources) -> Unit
) : ModifierNodeElement<AddTextContextMenuDataComponentsWithResourcesNode>() {
    override fun create(): AddTextContextMenuDataComponentsWithResourcesNode =
        AddTextContextMenuDataComponentsWithResourcesNode(builder)

    override fun update(node: AddTextContextMenuDataComponentsWithResourcesNode) {
        node.builder = builder
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "addTextContextMenuDataComponentsWithResources"
        properties["builder"] = builder
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AddTextContextMenuDataComponentsWithResourcesElement) return false

        if (builder !== other.builder) return false

        return true
    }

    override fun hashCode(): Int = builder.hashCode()
}

private class AddTextContextMenuDataComponentsWithResourcesNode(
    var builder: TextContextMenuBuilderScope.(Resources) -> Unit
) : DelegatingNode(), CompositionLocalConsumerModifierNode {
    init {
        delegate(AddTextContextMenuDataComponentsNode { builder(currentValueOf(LocalResources)) })
    }
}
