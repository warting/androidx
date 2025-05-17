/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.wear.compose.foundation

import androidx.compose.foundation.layout.LayoutScopeMarker
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.util.fastForEach

/**
 * Layout scope used for curved containers. This is the base of a DSL that specifies what components
 * can be added to a curved layout.
 */
@LayoutScopeMarker
public class CurvedScope
internal constructor(internal val curvedLayoutDirection: CurvedLayoutDirection) {
    internal val nodes = mutableListOf<CurvedChild>()

    internal fun add(node: CurvedChild, modifier: CurvedModifier) {
        nodes.add(modifier.wrap(node))
    }
}

/** Base class for sub-layouts */
internal abstract class ContainerChild(
    curvedLayoutDirection: CurvedLayoutDirection,
    internal val reverseLayout: Boolean,
    contentBuilder: CurvedScope.() -> Unit,
) : CurvedChild() {
    private val curvedContainerScope = CurvedScope(curvedLayoutDirection).apply(contentBuilder)
    internal val children
        get() = curvedContainerScope.nodes

    internal val childrenInLayoutOrder
        get() =
            children.indices.map { ix ->
                children[if (reverseLayout) children.size - 1 - ix else ix]
            }

    @Composable
    override fun SubComposition(semanticProperties: CurvedSemanticProperties) {
        require(!semanticProperties.hasInfo()) {
            "Cannot add semantic properties to a curved container"
        }
        children.fastForEach { it.SubComposition(CurvedSemanticProperties()) }
    }

    override fun CurvedMeasureScope.initializeMeasure(measurables: Iterator<Measurable>) {
        children.fastForEach { node ->
            with(
                CurvedMeasureScope(
                    subDensity = this,
                    curvedContainerScope.curvedLayoutDirection,
                    radius,
                )
            ) {
                with(node) { initializeMeasure(measurables) }
            }
        }
    }

    override fun DrawScope.draw() = children.fastForEach { with(it) { draw() } }

    override fun (Placeable.PlacementScope).placeIfNeeded() {
        children.fastForEach { with(it) { placeIfNeeded() } }
    }
}
