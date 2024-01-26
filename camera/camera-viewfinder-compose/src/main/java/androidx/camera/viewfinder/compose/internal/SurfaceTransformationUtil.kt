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

package androidx.camera.viewfinder.compose.internal

import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.RectF
import android.util.Size
import androidx.camera.viewfinder.surface.TransformationInfo

/**
 * A util class with methods that transform the input viewFinder surface so that its preview fits
 * the given aspect ratio of its parent view.
 *
 * The goal is to transform it in a way so that the entire area of
 * TransformationInfo's cropRect is 1) visible to end users, and 2)
 * displayed as large as possible.
 *
 * The inputs for the calculation are 1) the dimension of the Surface, 2) the crop rect, 3) the
 * dimension of the Viewfinder and 4) rotation degrees
 */
object SurfaceTransformationUtil {

    fun getTextureViewCorrectionMatrix(
        transformationInfo: TransformationInfo,
        resolution: Size
    ): Matrix {
        val surfaceRect =
            RectF(0f, 0f, resolution.width.toFloat(), resolution.height.toFloat())
        val rotationDegrees: Int = transformationInfo.sourceRotation
        return TransformUtil.getRectToRect(surfaceRect, surfaceRect, rotationDegrees)
    }

    private fun getRotatedViewportSize(
        transformationInfo: TransformationInfo
    ): Size {
        return if (TransformUtil.is90or270(transformationInfo.sourceRotation)) {
            Size(
                transformationInfo.cropRectTop - transformationInfo.cropRectBottom,
                transformationInfo.cropRectRight - transformationInfo.cropRectLeft)
        } else {
            Size(
                transformationInfo.cropRectRight - transformationInfo.cropRectLeft,
                transformationInfo.cropRectTop - transformationInfo.cropRectBottom)
        }
    }

    private fun isViewportAspectRatioMatchViewFinder(
        transformationInfo: TransformationInfo,
        viewFinderSize: Size
    ): Boolean {
        // Using viewport rect to check if the viewport is based on the view finder.
        val rotatedViewportSize: Size = getRotatedViewportSize(transformationInfo)
        return TransformUtil.isAspectRatioMatchingWithRoundingError(
            viewFinderSize,
            true,
            rotatedViewportSize,
            false
        )
    }

    private fun setMatrixRectToRect(matrix: Matrix, source: RectF, destination: RectF) {
        val matrixScaleType = Matrix.ScaleToFit.CENTER
        // android.graphics.Matrix doesn't support fill scale types. The workaround is
        // mapping inversely from destination to source, then invert the matrix.
        matrix.setRectToRect(destination, source, matrixScaleType)
        matrix.invert(matrix)
    }

    private fun getViewFinderViewportRectForMismatchedAspectRatios(
        transformationInfo: TransformationInfo,
        viewFinderSize: Size
    ): RectF {
        val viewFinderRect =
            RectF(
                0f,
                0f,
                viewFinderSize.width.toFloat(),
                viewFinderSize.height.toFloat()
            )
        val rotatedViewportSize = getRotatedViewportSize(transformationInfo)
        val rotatedViewportRect =
            RectF(
                0f,
                0f,
                rotatedViewportSize.width.toFloat(),
                rotatedViewportSize.height.toFloat()
            )
        val matrix = Matrix()
        setMatrixRectToRect(
            matrix,
            rotatedViewportRect,
            viewFinderRect
        )
        matrix.mapRect(rotatedViewportRect)
        return rotatedViewportRect
    }

    private fun getSurfaceToViewFinderMatrix(
        viewFinderSize: Size,
        transformationInfo: TransformationInfo,
    ): Matrix {
        // Get the target of the mapping, the coordinates of the crop rect in view finder.
        val viewFinderCropRect: RectF =
            if (isViewportAspectRatioMatchViewFinder(transformationInfo, viewFinderSize)) {
                // If crop rect has the same aspect ratio as view finder, scale the crop rect to
                // fill the entire view finder. This happens if the scale type is FILL_* AND a
                // view-finder-based viewport is used.
                RectF(
                    0f,
                    0f,
                    viewFinderSize.width.toFloat(),
                    viewFinderSize.height.toFloat()
                )
            } else {
                // If the aspect ratios don't match, it could be 1) scale type is FIT_*, 2) the
                // Viewport is not based on the view finder or 3) both.
                getViewFinderViewportRectForMismatchedAspectRatios(
                    transformationInfo,
                    viewFinderSize
                )
            }

        val rect = Rect(transformationInfo.cropRectLeft,
                        transformationInfo.cropRectTop,
                        transformationInfo.cropRectRight,
                        transformationInfo.cropRectBottom)
        val matrix =
            TransformUtil.getRectToRect(
                RectF(),
                viewFinderCropRect,
                transformationInfo.sourceRotation
            )

        if (transformationInfo.shouldMirror) {
            if (TransformUtil.is90or270(transformationInfo.sourceRotation)) {
                // If the rotation is 90/270, the Surface should be flipped vertically.
                //   +---+     90 +---+  270 +---+
                //   | ^ | -->    | < |      | > |
                //   +---+        +---+      +---+
                matrix.preScale(
                    1f,
                    -1f,
                    rect.centerX().toFloat(),
                    rect.centerY().toFloat()
                )
            } else {
                // If the rotation is 0/180, the Surface should be flipped horizontally.
                //   +---+      0 +---+  180 +---+
                //   | ^ | -->    | ^ |      | v |
                //   +---+        +---+      +---+
                matrix.preScale(
                    -1f,
                    1f,
                    rect.centerX().toFloat(),
                    rect.centerY().toFloat()
                )
            }
        }
        return matrix
    }

    fun getTransformedSurfaceRect(
        resolution: Size,
        transformationInfo: TransformationInfo,
        viewFinderSize: Size,
    ): RectF {
        val surfaceToViewFinder: Matrix =
            getSurfaceToViewFinderMatrix(
                viewFinderSize,
                transformationInfo,
            )
        val rect = RectF(0f, 0f, resolution.width.toFloat(), resolution.height.toFloat())
        surfaceToViewFinder.mapRect(rect)
        return rect
    }
}
