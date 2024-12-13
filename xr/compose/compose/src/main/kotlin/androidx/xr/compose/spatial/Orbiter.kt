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

package androidx.xr.compose.spatial

import android.view.View
import androidx.annotation.RestrictTo
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.ZeroCornerSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.xr.compose.platform.LocalDialogManager
import androidx.xr.compose.platform.LocalPanelEntity
import androidx.xr.compose.platform.LocalSession
import androidx.xr.compose.platform.LocalSpatialCapabilities
import androidx.xr.compose.spatial.EdgeOffset.Companion.outer
import androidx.xr.compose.subspace.layout.SpatialRoundedCornerShape
import androidx.xr.compose.subspace.layout.SpatialShape
import androidx.xr.scenecore.PixelDimensions

/** Contains default values used by Orbiters. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public object OrbiterDefaults {

    /** Default shape for an Orbiter. */
    public val shape: SpatialShape = SpatialRoundedCornerShape(ZeroCornerSize)

    /** Default settings for an Orbiter */
    public val orbiterSettings: OrbiterSettings = OrbiterSettings()
}

/**
 * Settings for an Orbiter.
 *
 * @property shouldRenderInNonSpatial controls whether the orbiter content should be rendered in the
 *   normal flow in non-spatial environments. If `true`, the content is rendered normally;
 *   otherwise, it's removed from the flow.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public class OrbiterSettings(
    @get:JvmName("shouldRenderInNonSpatial") public val shouldRenderInNonSpatial: Boolean = true
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is OrbiterSettings) return false

        if (shouldRenderInNonSpatial != other.shouldRenderInNonSpatial) return false

        return true
    }

    override fun hashCode(): Int {
        return shouldRenderInNonSpatial.hashCode()
    }

    override fun toString(): String {
        return "OrbiterSettings(shouldRenderInNonSpatial=$shouldRenderInNonSpatial)"
    }

    public fun copy(
        shouldRenderInNonSpatial: Boolean = this.shouldRenderInNonSpatial
    ): OrbiterSettings = OrbiterSettings(shouldRenderInNonSpatial = shouldRenderInNonSpatial)
}

/**
 * A composable that creates an orbiter along the top or bottom edges of a view.
 *
 * @param position The edge of the orbiter. Use [OrbiterEdge.Top] or [OrbiterEdge.Bottom].
 * @param offset The offset of the orbiter based on the outer edge of the orbiter.
 * @param alignment The alignment of the orbiter. Use [Alignment.CenterHorizontally] or
 *   [Alignment.Start] or [Alignment.End].
 * @param settings The settings for the orbiter.
 * @param shape The shape of this Orbiter when it is rendered in 3D space.
 * @param content The content of the orbiter.
 *
 * Example:
 * ```
 * Orbiter(position = OrbiterEdge.Top, offset = 10.dp) {
 *   Text("This is a top edge Orbiter")
 * }
 * ```
 */
@Composable
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public fun Orbiter(
    position: OrbiterEdge.Horizontal,
    offset: Dp = 0.dp,
    alignment: Alignment.Horizontal = Alignment.CenterHorizontally,
    settings: OrbiterSettings = OrbiterDefaults.orbiterSettings,
    shape: SpatialShape = OrbiterDefaults.shape,
    content: @Composable () -> Unit,
) {
    Orbiter(
        OrbiterData(
            position = position,
            horizontalAlignment = alignment,
            offset = outer(offset),
            settings = settings,
            shape = shape,
            content = content,
        )
    )
}

/**
 * A composable that creates an orbiter along the top or bottom edges of a view.
 *
 * @param position The edge of the orbiter. Use [OrbiterEdge.Top] or [OrbiterEdge.Bottom].
 * @param offset The offset of the orbiter based on the inner or outer edge of the orbiter. Use
 *   [EdgeOffset.outer] to create an [EdgeOffset] aligned to the outer edge of the orbiter or
 *   [innerEdge] or [EdgeOffset.overlap] to create an [EdgeOffset] aligned to the inner edge of the
 *   orbiter.
 * @param alignment The alignment of the orbiter. Use [Alignment.CenterHorizontally] or
 *   [Alignment.Start] or [Alignment.End].
 * @param settings The settings for the orbiter.
 * @param shape The shape of this Orbiter when it is rendered in 3D space.
 * @param content The content of the orbiter.
 *
 * Example:
 * ```
 * Orbiter(position = OrbiterEdge.Top, offset = outer(10.dp)) {
 *   Text("This is a top edge Orbiter")
 * }
 * ```
 */
@Composable
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public fun Orbiter(
    position: OrbiterEdge.Horizontal,
    offset: EdgeOffset,
    alignment: Alignment.Horizontal = Alignment.CenterHorizontally,
    settings: OrbiterSettings = OrbiterDefaults.orbiterSettings,
    shape: SpatialShape = OrbiterDefaults.shape,
    content: @Composable () -> Unit,
) {
    Orbiter(
        OrbiterData(
            position = position,
            horizontalAlignment = alignment,
            offset = offset,
            settings = settings,
            shape = shape,
            content = content,
        )
    )
}

/**
 * A composable that creates an orbiter along the start or end edges of a view.
 *
 * @param position The edge of the orbiter. Use [OrbiterEdge.Start] or [OrbiterEdge.End].
 * @param offset The offset of the orbiter based on the outer edge of the orbiter.
 * @param alignment The alignment of the orbiter. Use [Alignment.CenterVertically] or
 *   [Alignment.Top] or [Alignment.Bottom].
 * @param settings The settings for the orbiter.
 * @param shape The shape of this Orbiter when it is rendered in 3D space.
 * @param content The content of the orbiter.
 *
 * Example:
 * ```
 * Orbiter(position = OrbiterEdge.Start, offset = 10.dp) {
 *   Text("This is a start edge Orbiter")
 * }
 * ```
 */
@Composable
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public fun Orbiter(
    position: OrbiterEdge.Vertical,
    offset: Dp = 0.dp,
    alignment: Alignment.Vertical = Alignment.CenterVertically,
    settings: OrbiterSettings = OrbiterDefaults.orbiterSettings,
    shape: SpatialShape = OrbiterDefaults.shape,
    content: @Composable () -> Unit,
) {
    Orbiter(
        OrbiterData(
            position = position,
            verticalAlignment = alignment,
            offset = outer(offset),
            settings = settings,
            shape = shape,
            content = content,
        )
    )
}

/**
 * A composable that creates an orbiter along the start or end edges of a view.
 *
 * @param position The edge of the orbiter. Use [OrbiterEdge.Start] or [OrbiterEdge.End].
 * @param offset The offset of the orbiter based on the inner or outer edge of the orbiter. Use
 *   [EdgeOffset.outer] to create an [EdgeOffset] aligned to the outer edge of the orbiter or
 *   [innerEdge] or [EdgeOffset.overlap] to create an [EdgeOffset] aligned to the inner edge of the
 *   orbiter.
 * @param alignment The alignment of the orbiter. Use [Alignment.CenterVertically] or
 *   [Alignment.Top] or [Alignment.Bottom].
 * @param settings The settings for the orbiter.
 * @param shape The shape of this Orbiter when it is rendered in 3D space.
 * @param content The content of the orbiter.
 *
 * Example:
 * ```
 * Orbiter(position = OrbiterEdge.Start, offset = outer(10.dp)) {
 *   Text("This is a start edge Orbiter")
 * }
 * ```
 */
@Composable
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public fun Orbiter(
    position: OrbiterEdge.Vertical,
    offset: EdgeOffset,
    alignment: Alignment.Vertical = Alignment.CenterVertically,
    settings: OrbiterSettings = OrbiterDefaults.orbiterSettings,
    shape: SpatialShape = OrbiterDefaults.shape,
    content: @Composable () -> Unit,
) {
    Orbiter(
        OrbiterData(
            position = position,
            verticalAlignment = alignment,
            offset = offset,
            settings = settings,
            shape = shape,
            content = content,
        )
    )
}

@Composable
private fun Orbiter(data: OrbiterData) {
    if (LocalSpatialCapabilities.current.isSpatialUiEnabled) {
        PositionedOrbiter(data)
    } else if (data.settings.shouldRenderInNonSpatial) {
        data.content()
    }
}

@Composable
internal fun PositionedOrbiter(data: OrbiterData) {
    val view = LocalView.current
    val session = checkNotNull(LocalSession.current) { "session must be initialized" }
    val panelEntity = LocalPanelEntity.current ?: session.mainPanelEntity
    var panelSize by remember { mutableStateOf(panelEntity.getPixelDimensions()) }
    var contentSize: IntSize? by remember { mutableStateOf(null) }
    val dialogManager = LocalDialogManager.current

    DisposableEffect(view) {
        val listener =
            View.OnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
                panelSize = panelEntity.getPixelDimensions()
            }
        view.addOnLayoutChangeListener(listener)
        onDispose { view.removeOnLayoutChangeListener(listener) }
    }

    ElevatedPanel(
        spatialElevationLevel = SpatialElevationLevel.Level1,
        contentSize = contentSize ?: IntSize.Zero,
        contentOffset = contentSize?.let { data.calculateOffset(panelSize, it) },
        shape = data.shape,
    ) {
        Box(
            modifier =
                Modifier.constrainTo(Constraints(0, panelSize.width, 0, panelSize.height))
                    .onSizeChanged { contentSize = it }
        ) {
            data.content()
        }
        Box(
            modifier =
                Modifier.fillMaxSize()
                    .then(
                        if (dialogManager.isSpatialDialogActive.value) {
                            Modifier.background(Color.Black.copy(alpha = 0.2f)).pointerInput(Unit) {
                                detectTapGestures {
                                    dialogManager.isSpatialDialogActive.value = false
                                }
                            }
                        } else {
                            Modifier
                        }
                    )
        ) {}
    }
}

/** An enum that represents the edges of a view where an orbiter can be placed. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public sealed interface OrbiterEdge {
    @JvmInline
    public value class Horizontal private constructor(private val value: Int) : OrbiterEdge {
        public companion object {
            public val Top: Horizontal = Horizontal(0)
            public val Bottom: Horizontal = Horizontal(1)
        }
    }

    /** Represents vertical edges (start or end). */
    @JvmInline
    public value class Vertical private constructor(private val value: Int) : OrbiterEdge {
        public companion object {
            public val Start: Vertical = Vertical(0)
            public val End: Vertical = Vertical(1)
        }
    }

    public companion object {
        /** The top edge. */
        public val Top: Horizontal = Horizontal.Top
        /** The bottom edge. */
        public val Bottom: Horizontal = Horizontal.Bottom
        /** The start edge. */
        public val Start: Vertical = Vertical.Start
        /** The end edge. */
        public val End: Vertical = Vertical.End
    }
}

/** Represents the type of offset used for positioning an orbiter. */
@JvmInline
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public value class OrbiterOffsetType private constructor(private val value: Int) {
    public companion object {
        /** Indicates that the offset is relative to the outer edge of the orbiter. */
        public val OuterEdge: OrbiterOffsetType = OrbiterOffsetType(0)
        /** Indicates that the offset is relative to the inner edge of the orbiter. */
        public val InnerEdge: OrbiterOffsetType = OrbiterOffsetType(1)
    }
}

/**
 * Represents the offset of an orbiter from the main panel.
 *
 * @property amount the magnitude of the offset in pixels.
 * @property type the type of offset ([OrbiterOffsetType.OuterEdge] or
 *   [OrbiterOffsetType.InnerEdge]).
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public class EdgeOffset
internal constructor(public val amount: Float, public val type: OrbiterOffsetType) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is EdgeOffset) return false

        if (amount != other.amount) return false
        if (type != other.type) return false

        return true
    }

    override fun hashCode(): Int {
        var result = amount.hashCode()
        result = 31 * result + type.hashCode()
        return result
    }

    override fun toString(): String {
        return "EdgeOffset(amount=$amount, type=$type)"
    }

    public fun copy(amount: Float = this.amount, type: OrbiterOffsetType = this.type): EdgeOffset =
        EdgeOffset(amount = amount, type = type)

    public companion object {
        /**
         * Creates an [EdgeOffset] representing an offset from the outer edge of an orbiter.
         *
         * An offset that represents the offset of an orbiter from the main panel relative to the
         * outer edge of the orbiter. In outer edge alignment, the outer edge of the orbiter will be
         * [offset] distance away from the edge of the main panel.
         *
         * @param offset the offset value in [Dp].
         * @return an [EdgeOffset] with the specified offset and type [OrbiterOffsetType.OuterEdge].
         */
        @Composable
        public fun outer(offset: Dp): EdgeOffset =
            with(LocalDensity.current) { EdgeOffset(offset.toPx(), OrbiterOffsetType.OuterEdge) }

        /**
         * Creates an [EdgeOffset] representing an offset from the inner edge of an orbiter.
         *
         * An offset that represents the offset of an orbiter from the main panel relative to the
         * inner edge of the orbiter. In inner edge alignment, the inner edge of the orbiter will be
         * [offset] distance away from the edge of the main panel.
         *
         * @param offset the offset value in [Dp].
         * @return an [EdgeOffset] with the specified offset and type [OrbiterOffsetType.InnerEdge].
         */
        @Composable
        public fun inner(offset: Dp): EdgeOffset =
            with(LocalDensity.current) { EdgeOffset(offset.toPx(), OrbiterOffsetType.InnerEdge) }

        /**
         * Creates an [EdgeOffset] representing an overlap of an orbiter into the main panel
         * relative to the inner edge of the orbiter.
         *
         * In overlap alignment, the inner edge of the orbiter will be [offset] distance inset into
         * the edge of the main panel.
         *
         * @param offset the amount of overlap, specified in [Dp].
         * @return an [EdgeOffset] with the [offset]'s pixel value and
         *   [OrbiterOffsetType.InnerEdge].
         */
        @Composable
        public fun overlap(offset: Dp): EdgeOffset =
            with(LocalDensity.current) { EdgeOffset(-offset.toPx(), OrbiterOffsetType.InnerEdge) }
    }
}

internal data class OrbiterData(
    public val position: OrbiterEdge,
    public val verticalAlignment: Alignment.Vertical = Alignment.CenterVertically,
    public val horizontalAlignment: Alignment.Horizontal = Alignment.CenterHorizontally,
    public val offset: EdgeOffset,
    public val settings: OrbiterSettings = OrbiterDefaults.orbiterSettings,
    public val content: @Composable () -> Unit,
    public val shape: SpatialShape,
)

/**
 * Calculates the offset that should be applied to the orbiter given its settings, the panel size,
 * and the size of the orbiter content.
 */
private fun OrbiterData.calculateOffset(viewSize: PixelDimensions, contentSize: IntSize): Offset {
    if (position is OrbiterEdge.Vertical) {
        val y = verticalAlignment.align(contentSize.height, viewSize.height)

        val xOffset =
            when (offset.type) {
                OrbiterOffsetType.OuterEdge -> -offset.amount
                OrbiterOffsetType.InnerEdge -> -contentSize.width - offset.amount
                else -> error("Unexpected OrbiterOffsetType: ${offset.type}")
            }

        val x =
            when (position) {
                OrbiterEdge.Start -> xOffset
                OrbiterEdge.End -> viewSize.width - contentSize.width - xOffset
                else -> error("Unexpected OrbiterEdge: $position")
            }
        return Offset(x, y.toFloat())
    } else {
        // It should be fine to use LTR layout direction here since we can use placeRelative to
        // adjust
        val x = horizontalAlignment.align(contentSize.width, viewSize.width, LayoutDirection.Ltr)

        val yOffset =
            when (offset.type) {
                OrbiterOffsetType.OuterEdge -> -offset.amount
                OrbiterOffsetType.InnerEdge -> -contentSize.height - offset.amount
                else -> error("Unexpected OrbiterOffsetType: ${offset.type}")
            }

        val y =
            when (position) {
                OrbiterEdge.Top -> yOffset
                OrbiterEdge.Bottom -> viewSize.height - contentSize.height - yOffset
                else -> error("Unexpected OrbiterEdge: $position")
            }
        return Offset(x.toFloat(), y)
    }
}
