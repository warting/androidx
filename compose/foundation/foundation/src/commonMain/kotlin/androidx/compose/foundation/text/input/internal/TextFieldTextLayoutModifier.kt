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

package androidx.compose.foundation.text.input.internal

import androidx.compose.foundation.relocation.BringIntoViewRequesterNode
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.ceilToIntPx
import androidx.compose.ui.layout.AlignmentLine
import androidx.compose.ui.layout.FirstBaseline
import androidx.compose.ui.layout.LastBaseline
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.node.GlobalPositionAwareModifierNode
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastRoundToInt

/**
 * This ModifierNodeElement is only responsible for laying out text and reporting its global
 * position coordinates. Text layout is kept in a separate node than the rest of the Core modifiers
 * because it will also go through scroll and minSize constraints. We need to know exact size and
 * coordinates of [TextLayoutResult] to make it relatively easier to calculate the offset between
 * exact touch coordinates and where they map on the [TextLayoutResult].
 */
internal class TextFieldTextLayoutModifier(
    private val textLayoutState: TextLayoutState,
    private val textFieldState: TransformedTextFieldState,
    private val textStyle: TextStyle,
    private val singleLine: Boolean,
    private val onTextLayout: (Density.(getResult: () -> TextLayoutResult?) -> Unit)?,
    private val keyboardOptions: KeyboardOptions,
) : ModifierNodeElement<TextFieldTextLayoutModifierNode>() {
    override fun create(): TextFieldTextLayoutModifierNode =
        TextFieldTextLayoutModifierNode(
            textLayoutState = textLayoutState,
            textFieldState = textFieldState,
            textStyle = textStyle,
            singleLine = singleLine,
            onTextLayout = onTextLayout,
            keyboardOptions = keyboardOptions,
        )

    override fun update(node: TextFieldTextLayoutModifierNode) {
        node.updateNode(
            textLayoutState = textLayoutState,
            textFieldState = textFieldState,
            textStyle = textStyle,
            singleLine = singleLine,
            onTextLayout = onTextLayout,
            keyboardOptions = keyboardOptions,
        )
    }

    override fun InspectorInfo.inspectableProperties() {
        // no inspector info
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TextFieldTextLayoutModifier) return false

        if (singleLine != other.singleLine) return false
        if (textLayoutState != other.textLayoutState) return false
        if (textFieldState != other.textFieldState) return false
        if (textStyle != other.textStyle) return false
        if (onTextLayout !== other.onTextLayout) return false
        if (keyboardOptions != other.keyboardOptions) return false

        return true
    }

    override fun hashCode(): Int {
        var result = singleLine.hashCode()
        result = 31 * result + textLayoutState.hashCode()
        result = 31 * result + textFieldState.hashCode()
        result = 31 * result + textStyle.hashCode()
        result = 31 * result + (onTextLayout?.hashCode() ?: 0)
        result = 31 * result + keyboardOptions.hashCode()
        return result
    }
}

internal class TextFieldTextLayoutModifierNode(
    private var textLayoutState: TextLayoutState,
    textFieldState: TransformedTextFieldState,
    textStyle: TextStyle,
    private var singleLine: Boolean,
    onTextLayout: (Density.(getResult: () -> TextLayoutResult?) -> Unit)?,
    keyboardOptions: KeyboardOptions,
) :
    DelegatingNode(),
    LayoutModifierNode,
    GlobalPositionAwareModifierNode,
    CompositionLocalConsumerModifierNode {

    private val bringIntoViewRequesterNode =
        delegate(BringIntoViewRequesterNode(textLayoutState.bringIntoViewRequester))

    init {
        textLayoutState.onTextLayout = onTextLayout
        textLayoutState.updateNonMeasureInputs(
            textFieldState = textFieldState,
            textStyle = textStyle,
            singleLine = singleLine,
            softWrap = !singleLine,
            keyboardOptions = keyboardOptions,
        )
    }

    @Suppress("PrimitiveInCollection")
    private var baselineCache: MutableMap<AlignmentLine, Int>? = null

    /** Updates all the related properties and invalidates internal state based on the changes. */
    fun updateNode(
        textLayoutState: TextLayoutState,
        textFieldState: TransformedTextFieldState,
        textStyle: TextStyle,
        singleLine: Boolean,
        onTextLayout: (Density.(getResult: () -> TextLayoutResult?) -> Unit)?,
        keyboardOptions: KeyboardOptions,
    ) {
        val previousTextLayoutState = this.textLayoutState

        this.textLayoutState = textLayoutState
        this.textLayoutState.onTextLayout = onTextLayout
        this.singleLine = singleLine
        this.textLayoutState.updateNonMeasureInputs(
            textFieldState = textFieldState,
            textStyle = textStyle,
            singleLine = singleLine,
            softWrap = !singleLine,
            keyboardOptions = keyboardOptions,
        )

        if (previousTextLayoutState != textLayoutState) {
            bringIntoViewRequesterNode.updateRequester(textLayoutState.bringIntoViewRequester)
        }
    }

    override fun onGloballyPositioned(coordinates: LayoutCoordinates) {
        this.textLayoutState.textLayoutNodeCoordinates = coordinates
    }

    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints,
    ): MeasureResult {
        val result =
            textLayoutState.layoutWithNewMeasureInputs(
                density = this,
                layoutDirection = layoutDirection,
                fontFamilyResolver = currentValueOf(LocalFontFamilyResolver),
                constraints = constraints,
            )

        val placeable =
            measurable.measure(
                Constraints.fitPrioritizingWidth(
                    minWidth = result.size.width,
                    maxWidth = result.size.width,
                    minHeight = result.size.height,
                    maxHeight = result.size.height,
                )
            )

        // calculate the min height for single line text to prevent text cuts.
        // for single line text maxLines puts in max height constraint based on
        // constant characters therefore if the user enters a character that is
        // longer (i.e. emoji or a tall script) the text is cut
        textLayoutState.minHeightForSingleLineField =
            if (singleLine) {
                result.getLineBottom(0).ceilToIntPx().toDp()
            } else {
                0.dp
            }

        @Suppress("PrimitiveInCollection") val cache = baselineCache ?: LinkedHashMap(2)
        cache[FirstBaseline] = result.firstBaseline.fastRoundToInt()
        cache[LastBaseline] = result.lastBaseline.fastRoundToInt()
        baselineCache = cache

        return layout(
            width = result.size.width,
            height = result.size.height,
            alignmentLines = baselineCache!!,
        ) {
            placeable.place(0, 0)
        }
    }
}
