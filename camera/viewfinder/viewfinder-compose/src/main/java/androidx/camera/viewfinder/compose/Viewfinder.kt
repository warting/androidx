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

package androidx.camera.viewfinder.compose

import android.graphics.RectF
import android.util.Size
import android.util.SizeF
import android.view.Surface
import androidx.camera.viewfinder.core.ImplementationMode
import androidx.camera.viewfinder.core.TransformationInfo
import androidx.camera.viewfinder.core.TransformationInfo.Companion.DEFAULT
import androidx.camera.viewfinder.core.ViewfinderSurfaceRequest
import androidx.camera.viewfinder.core.ViewfinderSurfaceSessionScope
import androidx.camera.viewfinder.core.impl.OffsetF
import androidx.camera.viewfinder.core.impl.RefCounted
import androidx.camera.viewfinder.core.impl.ScaleFactorF
import androidx.camera.viewfinder.core.impl.Transformations
import androidx.camera.viewfinder.core.impl.ViewfinderSurfaceSessionImpl
import androidx.compose.foundation.AndroidEmbeddedExternalSurface
import androidx.compose.foundation.AndroidExternalSurface
import androidx.compose.foundation.AndroidExternalSurfaceScope
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.setFrom
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.util.fastRoundToInt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope

/**
 * Displays a media stream with the given transformations for crop and rotation while maintaining
 * proper scaling.
 *
 * A [Surface] for the given [ViewfinderSurfaceRequest] can be retrieved from the
 * [ViewfinderSurfaceSessionScope] of the callback registered via
 * [ViewfinderInitScope.onSurfaceSession] in [onInit].
 *
 * This has two underlying implementations either using an [AndroidEmbeddedExternalSurface] for
 * [ImplementationMode.EMBEDDED] or an [AndroidExternalSurface] for [ImplementationMode.EXTERNAL].
 * These can be set by the [ImplementationMode] argument in the [surfaceRequest] constructor. If the
 * implementation mode is `null`, then [ImplementationMode.EXTERNAL] will be used.
 *
 * The [onInit] lambda, and the callback registered with [ViewfinderInitScope.onSurfaceSession], are
 * always called from the main thread. [onInit] will be called every time a new [surfaceRequest] is
 * provided.
 *
 * @param surfaceRequest Details about the surface being requested
 * @param transformationInfo Specifies the required transformations for the media being displayed.
 * @param modifier Modifier to be applied to the [Viewfinder]
 * @param coordinateTransformer Coordinate transformer that can be used to convert Compose space
 *   coordinates such as touch coordinates to surface space coordinates. When the Viewfinder is
 *   displaying content from the camera, this transformer can be used to translate touch events into
 *   camera sensor coordinates for focus and metering actions.
 * @param alignment Optional alignment parameter used to place the [Surface] in the given bounds of
 *   the [Viewfinder]. Defaults to [Alignment.Center].
 * @param contentScale Optional scale parameter used to determine the aspect ratio scaling to be
 *   used to fit the [Surface] in the bounds of the [Viewfinder]. Defaults to [ContentScale.Crop].
 * @param onInit Lambda invoked on first composition and any time a new [surfaceRequest] is
 *   provided. This lambda can be used to declare a [ViewfinderInitScope.onSurfaceSession] callback
 *   that will be called each time a new [Surface] is provided by the viewfinder.
 *
 * TODO(b/322420487): Add a sample with `@sample`
 */
@Composable
fun Viewfinder(
    surfaceRequest: ViewfinderSurfaceRequest,
    modifier: Modifier = Modifier,
    transformationInfo: TransformationInfo = DEFAULT,
    coordinateTransformer: MutableCoordinateTransformer? = null,
    alignment: Alignment = Alignment.Center,
    contentScale: ContentScale = ContentScale.Crop,
    onInit: ViewfinderInitScope.() -> Unit
) {
    Box(modifier = modifier.clipToBounds().fillMaxSize()) {
        key(surfaceRequest) {
            TransformedSurface(
                surfaceWidth = surfaceRequest.width,
                surfaceHeight = surfaceRequest.height,
                transformationInfo = transformationInfo,
                implementationMode =
                    surfaceRequest.implementationMode ?: ImplementationMode.EXTERNAL,
                coordinateTransformer = coordinateTransformer,
                alignment = alignment,
                contentScale = contentScale
            ) {
                val viewfinderInitScope =
                    ViewfinderInitScopeImpl(viewfinderSurfaceRequest = surfaceRequest)

                // Register callback from onInit()
                onInit.invoke(viewfinderInitScope)

                onSurface { newSurface, _, _ ->
                    val refCountedSurface = RefCounted<Surface> { it.release() }
                    refCountedSurface.initialize(newSurface)

                    // TODO(b/390508238): Stop underlying View from releasing the Surface
                    // automatically. It should wait for the RefCount to get to 0.
                    newSurface.onDestroyed { refCountedSurface.release() }

                    // TODO(b/322420176): Properly handle onSurfaceChanged()

                    // Dispatch surface to registered onSurfaceSession callback
                    viewfinderInitScope.dispatchOnSurfaceSession(refCountedSurface)
                }
            }
        }
    }
}

@Composable
private fun TransformedSurface(
    surfaceWidth: Int,
    surfaceHeight: Int,
    transformationInfo: TransformationInfo,
    implementationMode: ImplementationMode,
    coordinateTransformer: MutableCoordinateTransformer?,
    alignment: Alignment,
    contentScale: ContentScale,
    onInit: AndroidExternalSurfaceScope.() -> Unit
) {
    val layoutDirection = LocalConfiguration.current.layoutDirection
    val surfaceModifier =
        Modifier.layout { measurable, constraints ->
            val placeable = measurable.measure(Constraints.fixed(surfaceWidth, surfaceHeight))

            // When the child placeable is larger than the parent's constraints, rather
            // than the child overflowing through the right or bottom of the parent, it overflows
            // evenly on all sides, as if it's placed exactly in the center of the parent.
            // To compensate for this, we must offset the child by the amount it overflows
            // so it is consistently placed in the top left corner of the parent before
            // we apply scaling and translation in the graphics layer.
            val widthOffset = 0.coerceAtLeast((placeable.width - constraints.maxWidth) / 2)
            val heightOffset = 0.coerceAtLeast((placeable.height - constraints.maxHeight) / 2)
            layout(placeable.width, placeable.height) {
                placeable.placeWithLayer(widthOffset, heightOffset) {
                    val surfaceToViewFinderMatrix =
                        Transformations.getSurfaceToViewfinderMatrix(
                            viewfinderSize = Size(constraints.maxWidth, constraints.maxHeight),
                            surfaceResolution = Size(surfaceWidth, surfaceHeight),
                            transformationInfo = transformationInfo,
                            layoutDirection = layoutDirection,
                            contentScale = contentScale.toInternalContentScale(),
                            alignment = alignment.toInternalAlignment()
                        )

                    coordinateTransformer?.transformMatrix =
                        Matrix().apply {
                            setFrom(surfaceToViewFinderMatrix)
                            invert()
                        }

                    val surfaceRectInViewfinder =
                        RectF(0f, 0f, surfaceWidth.toFloat(), surfaceHeight.toFloat())
                            .also(surfaceToViewFinderMatrix::mapRect)

                    transformOrigin = TransformOrigin(0f, 0f)
                    scaleX = surfaceRectInViewfinder.width() / surfaceWidth
                    scaleY = surfaceRectInViewfinder.height() / surfaceHeight

                    translationX = surfaceRectInViewfinder.left
                    translationY = surfaceRectInViewfinder.top
                }
            }
        }

    when (implementationMode) {
        ImplementationMode.EXTERNAL -> {
            AndroidExternalSurface(modifier = surfaceModifier, onInit = onInit)
        }
        ImplementationMode.EMBEDDED -> {
            val displayRotationDegrees =
                key(LocalConfiguration.current) {
                    Transformations.surfaceRotationToRotationDegrees(
                        LocalView.current.display.rotation
                    )
                }

            // For TextureView, correct the orientation to match the display rotation.
            val correctionMatrix = remember { Matrix() }

            transformationInfo.let {
                correctionMatrix.setFrom(
                    Transformations.getTextureViewCorrectionMatrix(
                        displayRotationDegrees = displayRotationDegrees,
                        width = surfaceWidth,
                        height = surfaceHeight
                    )
                )
            }

            AndroidEmbeddedExternalSurface(
                modifier = surfaceModifier,
                transform = correctionMatrix,
                onInit = onInit
            )
        }
    }
}

private fun Alignment.toInternalAlignment(): androidx.camera.viewfinder.core.impl.Alignment =
    object : androidx.camera.viewfinder.core.impl.Alignment {
        override fun align(size: SizeF, space: SizeF, layoutDirection: Int): OffsetF {
            val composeSize =
                androidx.compose.ui.unit.IntSize(
                    size.width.fastRoundToInt(),
                    size.height.fastRoundToInt()
                )
            val composeSpace =
                androidx.compose.ui.unit.IntSize(
                    space.width.fastRoundToInt(),
                    space.height.fastRoundToInt()
                )
            val composeLayoutDirection =
                when (layoutDirection) {
                    android.util.LayoutDirection.LTR -> androidx.compose.ui.unit.LayoutDirection.Ltr
                    android.util.LayoutDirection.RTL -> androidx.compose.ui.unit.LayoutDirection.Rtl
                    else ->
                        throw IllegalArgumentException("Invalid layout direction: $layoutDirection")
                }
            val offset = align(composeSize, composeSpace, composeLayoutDirection)
            return OffsetF(offset.x.toFloat(), offset.y.toFloat())
        }
    }

private fun ContentScale.toInternalContentScale():
    androidx.camera.viewfinder.core.impl.ContentScale =
    object : androidx.camera.viewfinder.core.impl.ContentScale {
        override fun computeScaleFactor(srcSize: SizeF, dstSize: SizeF): ScaleFactorF {
            val composeSrcSize = androidx.compose.ui.geometry.Size(srcSize.width, srcSize.height)
            val composeDstSize = androidx.compose.ui.geometry.Size(dstSize.width, dstSize.height)
            val scale = computeScaleFactor(composeSrcSize, composeDstSize)
            return ScaleFactorF(scale.scaleX, scale.scaleY)
        }
    }

/**
 * A scoped environment provided when a [Viewfinder] is first initialized.
 *
 * The environment can be used to register a lambda to invoke when a new
 * [ViewfinderSurfaceSessionScope] is available.
 */
interface ViewfinderInitScope {
    /**
     * Registers a callback to be invoked when a new [ViewfinderSurfaceSessionScope] is created.
     *
     * The provided callback will be invoked each time a new `ViewfinderSurfaceSessionScope` is
     * available. If a new [ViewfinderSurfaceSessionScope] is available, the previous one will be
     * cancelled before [block] is invoked with the new one.
     *
     * The provided callback will always be invoked from the main thread.
     */
    fun onSurfaceSession(block: suspend ViewfinderSurfaceSessionScope.() -> Unit)
}

private class ViewfinderInitScopeImpl(val viewfinderSurfaceRequest: ViewfinderSurfaceRequest) :
    ViewfinderInitScope {
    private var onSurfaceSession: (suspend ViewfinderSurfaceSessionScope.() -> Unit)? = null

    override fun onSurfaceSession(block: suspend ViewfinderSurfaceSessionScope.() -> Unit) {
        this.onSurfaceSession = block
    }

    suspend fun dispatchOnSurfaceSession(refCountedSurface: RefCounted<Surface>) {
        onSurfaceSession?.let { block ->
            refCountedSurface.acquire()?.let { surface ->
                ViewfinderSurfaceSessionImpl(surface, viewfinderSurfaceRequest) {
                        refCountedSurface.release()
                    }
                    .use { surfaceSession ->
                        coroutineScope {
                            val receiver =
                                object :
                                    ViewfinderSurfaceSessionScope,
                                    CoroutineScope by this@coroutineScope {
                                    override val surface = surfaceSession.surface
                                    override val request = surfaceSession.request
                                }
                            block.invoke(receiver)
                        }
                    }
            }
        }
    }
}
