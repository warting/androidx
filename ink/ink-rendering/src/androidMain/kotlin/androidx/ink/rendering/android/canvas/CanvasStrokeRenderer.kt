/*
 * Copyright (C) 2024 The Android Open Source Project
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

package androidx.ink.rendering.android.canvas

import android.graphics.Canvas
import android.graphics.Matrix
import androidx.annotation.Px
import androidx.annotation.RestrictTo
import androidx.ink.brush.ExperimentalInkCustomBrushApi
import androidx.ink.brush.TextureBitmapStore
import androidx.ink.geometry.AffineTransform
import androidx.ink.nativeloader.NativeLoader
import androidx.ink.rendering.android.canvas.internal.CanvasPathRenderer
import androidx.ink.rendering.android.canvas.internal.CanvasStrokeUnifiedRenderer
import androidx.ink.strokes.InProgressStroke
import androidx.ink.strokes.Stroke

/**
 * Renders strokes to a [Canvas].
 *
 * Instead of calling the [draw] methods here directly, it may be simpler to pass an instance of
 * [CanvasStrokeRenderer] to [androidx.ink.rendering.android.view.ViewStrokeRenderer] and use it to
 * calculate transform matrix values.
 *
 * An example of how to use [CanvasStrokeRenderer.draw] directly:
 * ```
 * class MyView {
 *   // Update these according to app business logic, and call `MyView.invalidate()`
 *   val worldToViewTransform = Matrix() // Call e.g. `setScale(2F)` to zoom in 2x
 *   val strokesWithTransforms = mutableMapOf<Stroke, Matrix>()
 *
 *   private val strokeToViewTransform = Matrix() // reusable scratch object
 *   private val renderer = CanvasStrokeRenderer.create()
 *
 *   fun onDraw(canvas: Canvas) {
 *     for ((stroke, strokeToWorldTransform) in strokesWithTransforms) {
 *       // Combine worldToViewTransform (drawing surface being panned/zoomed/rotated) with
 *       // strokeToWorldTransform (stroke itself being moved/scaled/rotated within the drawing
 *       // surface) to get the overall transform of this stroke.
 *       strokeToViewTransform.set(strokeToWorldTransform)
 *       strokeToViewTransform.postConcat(worldToViewTransform)
 *
 *       canvas.withMatrix(strokeToViewTransform) {
 *         // If coordinates of MyView are scaled/rotated from screen coordinates, then those
 *         // scale/rotation values should be multiplied into the strokeToScreenTransform
 *         // argument to renderer.draw.
 *         renderer.draw(canvas, stroke, strokeToViewTransform)
 *       }
 *     }
 *   }
 * }
 * ```
 *
 * In almost all cases, a developer should use an implementation of this interface obtained from
 * [CanvasStrokeRenderer.create].
 *
 * However, some developers may find it helpful to use their own implementation of this interface,
 * possibly to draw other effects to the [Canvas], typically delegating to a renderer from
 * [CanvasStrokeRenderer.create] for part of the custom rendering behavior to have the additional
 * effects add to or modify the standard stroke rendering behavior. Custom [CanvasStrokeRenderer]
 * implementations are generally less efficient than effects that can be achieved with a custom
 * [androidx.ink.brush.BrushFamily]. If a custom implementation draws to different screen locations
 * than the standard implementation, for example surrounding a stroke with additional content, then
 * that additional content will not be taken into account in geometry operations like
 * [androidx.ink.geometry.Intersection] or [androidx.ink.geometry.PartitionedMesh.computeCoverage].
 *
 * If custom rendering is needed during live authoring of in-progress strokes and that custom
 * rendering involves drawing content outside the stroke boundaries, then be sure to override
 * [strokeModifiedRegionOutsetPx].
 */
public interface CanvasStrokeRenderer {

    /**
     * Render a single [stroke] on the provided [canvas]. If [stroke] has animated textures, then
     * this will use a default animation progress value of zero.
     *
     * To avoid needing to calculate and maintain [strokeToScreenTransform], consider using
     * [androidx.ink.rendering.android.view.ViewStrokeRenderer] instead.
     *
     * The [strokeToScreenTransform] should represent the complete transformation from stroke
     * coordinates to the screen, modulo translation. This transform will not be applied to the
     * [canvas] in any way, as it may be made up of several individual transforms applied to the
     * [canvas] during an app’s drawing logic. If this transform is inaccurate, strokes may appear
     * blurry or aliased.
     */
    // TODO: b/353561141 - Reference ComposeStrokeRenderer above once implemented.
    public fun draw(
        canvas: Canvas,
        stroke: Stroke,
        strokeToScreenTransform: AffineTransform
    ): Unit = draw(canvas, stroke, strokeToScreenTransform, 0f)

    /**
     * Render a single [stroke] on the provided [canvas], using the specified
     * [textureAnimationProgress] value (typically 0 to 1) for the stroke's animated textures, if
     * any. Renderer implementations that don't support animated textures may ignore the
     * [textureAnimationProgress] argument.
     *
     * To avoid needing to calculate and maintain [strokeToScreenTransform], consider using
     * [androidx.ink.rendering.android.view.ViewStrokeRenderer] instead.
     *
     * The [strokeToScreenTransform] should represent the complete transformation from stroke
     * coordinates to the screen, modulo translation. This transform will not be applied to the
     * [canvas] in any way, as it may be made up of several individual transforms applied to the
     * [canvas] during an app’s drawing logic. If this transform is inaccurate, strokes may appear
     * blurry or aliased.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // PublicApiNotReadyForJetpackReview
    public fun draw(
        canvas: Canvas,
        stroke: Stroke,
        strokeToScreenTransform: AffineTransform,
        textureAnimationProgress: Float,
    )

    /**
     * Render a single [stroke] on the provided [canvas]. If [stroke] has animated textures, then
     * this will use a default animation progress value of zero.
     *
     * To avoid needing to calculate and maintain [strokeToScreenTransform], consider using
     * [androidx.ink.rendering.android.view.ViewStrokeRenderer] instead.
     *
     * The [strokeToScreenTransform] must be affine. It should represent the complete transformation
     * from stroke coordinates to the canvas, modulo translation. This transform will not be applied
     * to the [canvas] in any way, as it may be made up of several individual transforms applied to
     * the [canvas] during an app’s drawing logic. If this transform is inaccurate, strokes may
     * appear blurry or aliased.
     */
    // TODO: b/353561141 - Reference ComposeStrokeRenderer above once implemented.
    public fun draw(canvas: Canvas, stroke: Stroke, strokeToScreenTransform: Matrix): Unit =
        draw(canvas, stroke, strokeToScreenTransform, 0f)

    /**
     * Render a single [stroke] on the provided [canvas], using the specified
     * [textureAnimationProgress] value (typically 0 to 1) for the stroke's animated textures, if
     * any. Renderer implementations that don't support animated textures may ignore the
     * [textureAnimationProgress] argument.
     *
     * To avoid needing to calculate and maintain [strokeToScreenTransform], consider using
     * [androidx.ink.rendering.android.view.ViewStrokeRenderer] instead.
     *
     * The [strokeToScreenTransform] must be affine. It should represent the complete transformation
     * from stroke coordinates to the canvas, modulo translation. This transform will not be applied
     * to the [canvas] in any way, as it may be made up of several individual transforms applied to
     * the [canvas] during an app’s drawing logic. If this transform is inaccurate, strokes may
     * appear blurry or aliased.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // PublicApiNotReadyForJetpackReview
    public fun draw(
        canvas: Canvas,
        stroke: Stroke,
        strokeToScreenTransform: Matrix,
        textureAnimationProgress: Float,
    )

    /**
     * Render a single [inProgressStroke] on the provided [canvas]. If [inProgressStroke] has
     * animated textures, then this will use a default animation progress value of zero.
     *
     * The [strokeToScreenTransform] should represent the complete transformation from stroke
     * coordinates to the canvas, modulo translation. This transform will not be applied to the
     * [canvas] in any way, as it may be made up of several individual transforms applied to the
     * [canvas] during an app’s drawing logic. If this transform is inaccurate, strokes may appear
     * blurry or aliased.
     */
    public fun draw(
        canvas: Canvas,
        inProgressStroke: InProgressStroke,
        strokeToScreenTransform: AffineTransform,
    ): Unit = draw(canvas, inProgressStroke, strokeToScreenTransform, 0f)

    /**
     * Render a single [inProgressStroke] on the provided [canvas], using the specified
     * [textureAnimationProgress] value (typically 0 to 1) for the stroke's animated textures, if
     * any. Renderer implementations that don't support animated textures may ignore the
     * [textureAnimationProgress] argument.
     *
     * The [strokeToScreenTransform] should represent the complete transformation from stroke
     * coordinates to the canvas, modulo translation. This transform will not be applied to the
     * [canvas] in any way, as it may be made up of several individual transforms applied to the
     * [canvas] during an app’s drawing logic. If this transform is inaccurate, strokes may appear
     * blurry or aliased.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // PublicApiNotReadyForJetpackReview
    public fun draw(
        canvas: Canvas,
        inProgressStroke: InProgressStroke,
        strokeToScreenTransform: AffineTransform,
        textureAnimationProgress: Float,
    )

    /**
     * Render a single [inProgressStroke] on the provided [canvas]. If [inProgressStroke] has
     * animated textures, then this will use a default animation progress value of zero.
     *
     * The [strokeToScreenTransform] must be affine. It should represent the complete transformation
     * from stroke coordinates to the canvas, modulo translation. This transform will not be applied
     * to the [canvas] in any way, as it may be made up of several individual transforms applied to
     * the [canvas] during an app’s drawing logic. If this transform is inaccurate, strokes may
     * appear blurry or aliased.
     */
    public fun draw(
        canvas: Canvas,
        inProgressStroke: InProgressStroke,
        strokeToScreenTransform: Matrix,
    ): Unit = draw(canvas, inProgressStroke, strokeToScreenTransform, 0f)

    /**
     * Render a single [inProgressStroke] on the provided [canvas], using the specified
     * [textureAnimationProgress] value (typically 0 to 1) for the stroke's animated textures, if
     * any. Renderer implementations that don't support animated textures may ignore the
     * [textureAnimationProgress] argument.
     *
     * The [strokeToScreenTransform] must be affine. It should represent the complete transformation
     * from stroke coordinates to the canvas, modulo translation. This transform will not be applied
     * to the [canvas] in any way, as it may be made up of several individual transforms applied to
     * the [canvas] during an app’s drawing logic. If this transform is inaccurate, strokes may
     * appear blurry or aliased.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // PublicApiNotReadyForJetpackReview
    public fun draw(
        canvas: Canvas,
        inProgressStroke: InProgressStroke,
        strokeToScreenTransform: Matrix,
        textureAnimationProgress: Float,
    )

    /**
     * The distance beyond a stroke geometry's bounds that rendering might affect. This is currently
     * only applicable to in-progress stroke rendering, where the smallest possible region of the
     * screen is redrawn to optimize performance. But with a custom [CanvasStrokeRenderer], certain
     * effects like drop shadows or blurs may render beyond the stroke's geometry, and setting a
     * higher value here can ensure that artifacts are not left on screen after an in-progress
     * stroke has moved on from a particular region of the screen. This value should be set to the
     * lowest value that avoids the artifacts, as larger values will be less performant, and effects
     * that rely on larger values will be less compatible with stroke geometry operations.
     */
    @Px public fun strokeModifiedRegionOutsetPx(): Int = 3

    public companion object {

        init {
            NativeLoader.load()
        }

        /** Create a [CanvasStrokeRenderer] that is appropriate to the device's API version. */
        @JvmStatic
        public fun create(): CanvasStrokeRenderer {
            @OptIn(ExperimentalInkCustomBrushApi::class)
            return create(TextureBitmapStore { null }, forcePathRendering = false)
        }

        /**
         * Create a [CanvasStrokeRenderer] that is appropriate to the device's API version.
         *
         * @param textureStore The [TextureBitmapStore] that will be called to retrieve image data
         *   for drawing textured strokes.
         */
        @ExperimentalInkCustomBrushApi
        @JvmStatic
        public fun create(textureStore: TextureBitmapStore): CanvasStrokeRenderer {
            @OptIn(ExperimentalInkCustomBrushApi::class)
            return create(textureStore, forcePathRendering = false)
        }

        /**
         * Create a [CanvasStrokeRenderer] that is appropriate to the device's API version.
         *
         * @param forcePathRendering Overrides the drawing strategy selected based on API version to
         *   always draw strokes using [Canvas.drawPath] instead of [Canvas.drawMesh].
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // NonPublicApi
        @JvmStatic
        public fun create(forcePathRendering: Boolean): CanvasStrokeRenderer {
            @OptIn(ExperimentalInkCustomBrushApi::class)
            return create(TextureBitmapStore { null }, forcePathRendering)
        }

        /**
         * Create a [CanvasStrokeRenderer] that is appropriate to the device's API version.
         *
         * @param textureStore The [TextureBitmapStore] that will be called to retrieve image data
         *   for drawing textured strokes.
         * @param forcePathRendering Overrides the drawing strategy selected based on API version to
         *   always draw strokes using [Canvas.drawPath] instead of [Canvas.drawMesh].
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // NonPublicApi
        @ExperimentalInkCustomBrushApi
        @JvmStatic
        public fun create(
            textureStore: TextureBitmapStore,
            forcePathRendering: Boolean,
        ): CanvasStrokeRenderer {
            if (!forcePathRendering) return CanvasStrokeUnifiedRenderer(textureStore)
            return CanvasPathRenderer(textureStore)
        }
    }
}
