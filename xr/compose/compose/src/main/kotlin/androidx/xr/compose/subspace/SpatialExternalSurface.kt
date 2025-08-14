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

package androidx.xr.compose.subspace

import android.view.Surface
import androidx.annotation.RestrictTo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.ExperimentalComposeApi
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.util.fastForEachIndexed
import androidx.xr.compose.platform.LocalSession
import androidx.xr.compose.subspace.layout.CoreSphereSurfaceEntity
import androidx.xr.compose.subspace.layout.CoreSurfaceEntity
import androidx.xr.compose.subspace.layout.ParentLayoutParamsAdjustable
import androidx.xr.compose.subspace.layout.SpatialAlignment
import androidx.xr.compose.subspace.layout.SpatialFeatheringEffect
import androidx.xr.compose.subspace.layout.SubspaceLayout
import androidx.xr.compose.subspace.layout.SubspaceMeasurable
import androidx.xr.compose.subspace.layout.SubspaceMeasurePolicy
import androidx.xr.compose.subspace.layout.SubspaceMeasureResult
import androidx.xr.compose.subspace.layout.SubspaceMeasureScope
import androidx.xr.compose.subspace.layout.SubspaceModifier
import androidx.xr.compose.subspace.layout.SubspacePlaceable
import androidx.xr.compose.subspace.layout.ZeroFeatheringEffect
import androidx.xr.compose.subspace.layout.fillMaxSize
import androidx.xr.compose.subspace.layout.offset
import androidx.xr.compose.unit.IntVolumeSize
import androidx.xr.compose.unit.Meter
import androidx.xr.compose.unit.VolumeConstraints
import androidx.xr.compose.unit.toMeter
import androidx.xr.runtime.Config.HeadTrackingMode
import androidx.xr.runtime.SessionConfigureSuccess
import androidx.xr.runtime.math.Pose
import androidx.xr.scenecore.SpatialEnvironment
import androidx.xr.scenecore.SurfaceEntity
import androidx.xr.scenecore.scene
import kotlin.math.max

/** Contains default values used by SpatialExternalSurface. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public object SpatialExternalSurfaceDefaults {

    /** Default radius for spheres. */
    public val sphereRadius: Dp = Meter(15f).toDp()
}

/**
 * [SpatialExternalSurfaceScope] is a scoped environment that provides the [Surface] associated with
 * a [SpatialExternalSurface]
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public interface SpatialExternalSurfaceScope {
    /**
     * Invoked only one time when the Surface is created. This will execute before any layout or
     * modifiers are computed.
     */
    public fun onSurfaceCreated(onSurfaceCreated: (Surface) -> Unit)

    /** Invoked when the Composable and its associated [Surface] are destroyed. */
    public fun onSurfaceDestroyed(onSurfaceDestroyed: (Surface) -> Unit)
}

private class SpatialExternalSurfaceScopeInstance(private val entity: CoreSurfaceEntity) :
    SpatialExternalSurfaceScope {

    private var executedInit = false
    private var pendingOnCreate: ((Surface) -> Unit)? = null

    override fun onSurfaceCreated(onSurfaceCreated: (Surface) -> Unit) {
        pendingOnCreate = onSurfaceCreated
    }

    internal fun executeOnCreate() {
        if (!executedInit) {
            executedInit = true
            pendingOnCreate?.let { it(entity.surfaceEntity.getSurface()) }
        }
    }

    override fun onSurfaceDestroyed(onSurfaceDestroyed: (Surface) -> Unit) {
        entity.setOnSurfaceDestroyed(onSurfaceDestroyed)
    }
}

private class SpatialExternalSphereSurfaceScopeInstance(
    private val entity: CoreSphereSurfaceEntity
) : SpatialExternalSurfaceScope {

    private var executedInit = false
    private var pendingOnCreate: ((Surface) -> Unit)? = null

    override fun onSurfaceCreated(onSurfaceCreated: (Surface) -> Unit) {
        pendingOnCreate = onSurfaceCreated
    }

    internal fun executeOnCreate() {
        if (!executedInit) {
            executedInit = true
            pendingOnCreate?.let { it(entity.surfaceEntity.getSurface()) }
        }
    }

    override fun onSurfaceDestroyed(onSurfaceDestroyed: (Surface) -> Unit) {
        entity.setOnSurfaceDestroyed(onSurfaceDestroyed)
    }
}

/** Mode for SpatialExternalSurface display. */
@JvmInline
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public value class StereoMode private constructor(public val value: Int) {
    public companion object {
        /** Each eye will see the entire surface (no separation). */
        public val Mono: StereoMode = StereoMode(SurfaceEntity.StereoMode.STEREO_MODE_MONO)
        /** The [top, bottom] halves of the surface will map to [left, right] eyes. */
        public val TopBottom: StereoMode =
            StereoMode(SurfaceEntity.StereoMode.STEREO_MODE_TOP_BOTTOM)
        /** The [left, right] halves of the surface will map to [left, right] eyes. */
        public val SideBySide: StereoMode =
            StereoMode(SurfaceEntity.StereoMode.STEREO_MODE_SIDE_BY_SIDE)
        /**
         * For displaying mv-hevc video format, [base, secondary] view layers will map to
         * [left, right] eyes.
         */
        public val MultiviewLeftPrimary: StereoMode =
            StereoMode(SurfaceEntity.StereoMode.STEREO_MODE_MULTIVIEW_LEFT_PRIMARY)
        /**
         * For displaying mv-hevc video format, [base, secondary] view layers will map to
         * [right, left] eyes.
         */
        public val MultiviewRightPrimary: StereoMode =
            StereoMode(SurfaceEntity.StereoMode.STEREO_MODE_MULTIVIEW_RIGHT_PRIMARY)
    }
}

/** Protection levels for the Surface content. */
@JvmInline
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public value class SurfaceProtection private constructor(public val value: Int) {
    public companion object {
        /** No security is applied. */
        public val None: SurfaceProtection =
            SurfaceProtection(SurfaceEntity.SurfaceProtection.SURFACE_PROTECTION_NONE)
        /**
         * Sets the underlying Surface to set the
         * [android.hardware.HardwareBuffer.USAGE_PROTECTED_CONTENT] flag. This is mainly used to
         * protect DRM video content.
         */
        public val Protected: SurfaceProtection =
            SurfaceProtection(SurfaceEntity.SurfaceProtection.SURFACE_PROTECTION_PROTECTED)
    }
}

/**
 * A Composable that creates and owns an Android Surface into which the application can render
 * stereo image content. This can be thought of as the spatial equivalent of AndroidExternalSurface.
 * This Surface is texture mapped to the canvas, and if a stereoscopic StereoMode is specified, then
 * the User will see left and right eye content mapped to the appropriate display. Width and height
 * will default to 400 pixels if it is not specified using size modifiers.
 *
 * Note that this Surface does not capture input events. It is also not currently possible to
 * synchronize StereoMode changes with application rendering or video decoding. This composable
 * currently cannot render in front of other panels, so [dragPolicy] usage is not recommended if
 * there are other panels in the layout, aside from the content block of this Composable.
 *
 * Playing certain content will require the proper [SurfaceProtection]. This is mainly used to
 * protect DRM video content.
 *
 * @param modifier SubspaceModifiers to apply to the SpatialSurfacePanel.
 * @param stereoMode The [StereoMode] which describes how parts of the surface are displayed to the
 *   user's eyes. This will affect how the content is interpreted and displayed on the surface.
 * @param featheringEffect A [SpatialFeatheringEffect] to apply to to canvas of the surface exposed
 *   from [SpatialExternalSurfaceScope.onSurfaceCreated].
 * @param surfaceProtection Sets the Surface's protection from CPU access.
 * @param dragPolicy An optional [DragPolicy] that defines the motion behavior of the
 *   [SpatialPanel]. This can be either a [MovePolicy] for free movement or an [AnchorPolicy] for
 *   anchoring to real-world surfaces. If a policy is provided, draggable UI controls will be shown,
 *   allowing the user to manipulate the panel in 3D space. If null, no motion behavior is applied.
 * @param resizePolicy An optional [ResizePolicy] configuration object that resizing behavior of
 *   this [SpatialPanel]. The draggable UI controls will be shown that allow the user to resize the
 *   element in 3D space. If null, there is no resize behavior applied to the element.
 * @param content Content block where the surface can be accessed using
 *   [SpatialExternalSurfaceScope.onSurfaceCreated]. Composable content will be rendered over the
 *   Surface canvas. If using [StereoMode.SideBySide] or [StereoMode.TopBottom], it is recommended
 *   to offset Composable content far enough to avoid depth perception issues.
 */
@Composable
@SubspaceComposable
@ExperimentalComposeApi
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public fun SpatialExternalSurface(
    stereoMode: StereoMode,
    modifier: SubspaceModifier = SubspaceModifier,
    featheringEffect: SpatialFeatheringEffect = ZeroFeatheringEffect,
    surfaceProtection: SurfaceProtection = SurfaceProtection.None,
    dragPolicy: DragPolicy? = null,
    resizePolicy: ResizePolicy? = null,
    content: @Composable @SubspaceComposable SpatialExternalSurfaceScope.() -> Unit,
) {
    val finalModifier = buildSpatialPanelModifier(modifier, dragPolicy, resizePolicy)
    val session = LocalSession.current
    val density = LocalDensity.current

    // When surface protection changes, the surface entity has to be recreated because protection is
    // a non mutable setting.
    val coreSurfaceEntity =
        remember(surfaceProtection) {
            CoreSurfaceEntity(
                SurfaceEntity.create(
                    session = checkNotNull(session) { "Session is required" },
                    stereoMode = stereoMode.value,
                    surfaceProtection = surfaceProtection.value,
                ),
                localDensity = density,
            )
        }
    val instance =
        remember(coreSurfaceEntity) { SpatialExternalSurfaceScopeInstance(coreSurfaceEntity) }

    // Stereo mode can update during a recomposition.
    coreSurfaceEntity.stereoMode = stereoMode.value
    coreSurfaceEntity.setFeatheringEffect(featheringEffect)

    LaunchedEffect(instance) { instance.executeOnCreate() }

    key(coreSurfaceEntity) {
        SubspaceLayout(
            modifier = finalModifier,
            coreEntity = coreSurfaceEntity,
            content = { instance.content() },
            measurePolicy = SpatialBoxMeasurePolicy(SpatialAlignment.Center, false),
        )
    }
}

/**
 * A Composable that creates and owns an Android Surface into which the application can render
 * stereo image content inside a 180 degree hemisphere dome. This Surface is texture mapped to the
 * canvas, and if a stereoscopic StereoMode is specified, then the User will see left and right eye
 * content mapped to the appropriate display. This is an environment-like Composable that will
 * appear centered around the user's head position. If head tracking isn't already configured, an
 * attempt will be made to configure it. While this Composable is active, a temporary preferred
 * environment will be set, if one isn't already set, to put the user inside a boundary. In cases
 * where the user has not consented to the boundary or if passthrough is ever fully enabled, a
 * transparent feathered surface will display instead.
 *
 * Note that this Surface does not capture input events. It is also not currently possible to
 * synchronize StereoMode changes with application rendering or video decoding.
 *
 * Playing certain content will require the proper [SurfaceProtection]. This is mainly used to
 * protect DRM video content.
 *
 * @param modifier SubspaceModifiers to apply to the hemisphere. A sphere's measured size is
 *   automatically inferred from [radius] and does not need to be set through a modifier.
 * @param stereoMode The [StereoMode] which describes how parts of the surface are displayed to the
 *   user's eyes. This will affect how the content is interpreted and displayed on the surface.
 * @param radius The radius of the dome displaying the video.
 * @param featheringEffect A [SpatialFeatheringEffect] to apply to to canvas of the surface exposed
 *   from [SpatialExternalSurfaceScope.onSurfaceCreated]. For hemisphere domes, vertical feathering
 *   applies to the top and bottom poles of the dome, while horizontal feathering applies to the
 *   left and right sides.
 * @param surfaceProtection Sets the Surface's protection from CPU access.
 * @param content Content block where the surface can be accessed using
 *   [SpatialExternalSurfaceScope.onSurfaceCreated]. Composable content will be rendered in front of
 *   the user, slightly below the current gaze level. This default location is scaled with radius.
 */
@Composable
@SubspaceComposable
@ExperimentalComposeApi
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public fun SpatialExternalSurface180Hemisphere(
    stereoMode: StereoMode,
    modifier: SubspaceModifier = SubspaceModifier,
    radius: Dp = SpatialExternalSurfaceDefaults.sphereRadius,
    featheringEffect: SpatialFeatheringEffect = ZeroFeatheringEffect,
    surfaceProtection: SurfaceProtection = SurfaceProtection.None,
    content: @Composable @SubspaceComposable SpatialExternalSurfaceScope.() -> Unit,
) {
    SpatialExternalSurfaceSphere(
        stereoMode = stereoMode,
        isHemisphere = true,
        modifier = modifier,
        radius = radius,
        featheringEffect = featheringEffect,
        surfaceProtection = surfaceProtection,
        content = content,
    )
}

/**
 * A Composable that creates and owns an Android Surface into which the application can render
 * stereo image content inside a 360 degree sphere dome. This Surface is then texture mapped to the
 * canvas, and if a stereoscopic StereoMode is specified, then the User will see left and right eye
 * content mapped to the appropriate display. This is an environment-like Composable that will
 * appear centered around the user's head position. If head tracking isn't already configured, an
 * attempt will be made to configure it. While this Composable is active, a temporary preferred
 * environment will be set, if one isn't already set, to put the user inside a boundary. In cases
 * where the user has not consented to the boundary or if passthrough is ever fully enabled, a
 * transparent feathered surface will display instead.
 *
 * Note that this Surface does not capture input events. It is also not currently possible to
 * synchronize StereoMode changes with application rendering or video decoding.
 *
 * Playing certain content will require the proper [SurfaceProtection]. This is mainly used to
 * protect DRM video content.
 *
 * @param modifier SubspaceModifiers to apply to the sphere. A sphere's measured size is
 *   automatically inferred from [radius] and does not need to be set through a modifier.
 * @param stereoMode The [StereoMode] which describes how parts of the surface are displayed to the
 *   user's eyes. This will affect how the content is interpreted and displayed on the surface.
 * @param radius The radius of the dome displaying the video.
 * @param featheringEffect A [SpatialFeatheringEffect] to apply to to canvas of the surface exposed
 *   from [SpatialExternalSurfaceScope.onSurfaceCreated]. For sphere domes, vertical feathering
 *   applies to the top and bottom poles of the dome, while horizontal feathering applies to the
 *   left and right sides where the video is stitched together.
 * @param surfaceProtection Sets the Surface's protection from CPU access.
 * @param content Content block where the surface can be accessed using
 *   [SpatialExternalSurfaceScope.onSurfaceCreated]. Composable content will be rendered in front of
 *   the user, slightly below the current gaze level. This default location is scaled with radius.
 */
@Composable
@SubspaceComposable
@ExperimentalComposeApi
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public fun SpatialExternalSurface360Sphere(
    stereoMode: StereoMode,
    modifier: SubspaceModifier = SubspaceModifier,
    radius: Dp = SpatialExternalSurfaceDefaults.sphereRadius,
    featheringEffect: SpatialFeatheringEffect = ZeroFeatheringEffect,
    surfaceProtection: SurfaceProtection = SurfaceProtection.None,
    content: @Composable @SubspaceComposable SpatialExternalSurfaceScope.() -> Unit,
) {
    SpatialExternalSurfaceSphere(
        stereoMode = stereoMode,
        isHemisphere = false,
        modifier = modifier,
        radius = radius,
        featheringEffect = featheringEffect,
        surfaceProtection = surfaceProtection,
        content = content,
    )
}

@Composable
@SubspaceComposable
private fun SpatialExternalSurfaceSphere(
    stereoMode: StereoMode,
    isHemisphere: Boolean,
    modifier: SubspaceModifier = SubspaceModifier,
    radius: Dp = SpatialExternalSurfaceDefaults.sphereRadius,
    featheringEffect: SpatialFeatheringEffect = ZeroFeatheringEffect,
    surfaceProtection: SurfaceProtection = SurfaceProtection.None,
    content: @Composable @SubspaceComposable SpatialExternalSurfaceScope.() -> Unit,
) {
    val session = checkNotNull(LocalSession.current) { "session must be initialized" }
    val density = LocalDensity.current

    val meterRadius = radius.toMeter().value

    val coreSurfaceEntity =
        remember(surfaceProtection) {
            val headPose =
                if (
                    session.config.headTracking == HeadTrackingMode.LAST_KNOWN ||
                        session.configure(
                            config = session.config.copy(headTracking = HeadTrackingMode.LAST_KNOWN)
                        ) is SessionConfigureSuccess
                ) {
                    session.scene.spatialUser.head?.activitySpacePose
                } else {
                    null
                }
            CoreSphereSurfaceEntity(
                SurfaceEntity.create(
                    session = checkNotNull(session) { "Session is required" },
                    stereoMode = stereoMode.value,
                    surfaceProtection = surfaceProtection.value,
                    shape =
                        if (isHemisphere) {
                            SurfaceEntity.Shape.Hemisphere(meterRadius)
                        } else {
                            SurfaceEntity.Shape.Sphere(meterRadius)
                        },
                ),
                headPose,
                density,
            )
        }

    val instance =
        remember(coreSurfaceEntity) { SpatialExternalSphereSurfaceScopeInstance(coreSurfaceEntity) }

    coreSurfaceEntity.stereoMode = stereoMode.value
    coreSurfaceEntity.radius = meterRadius
    coreSurfaceEntity.setFeatheringEffect(featheringEffect)

    LaunchedEffect(instance) { instance.executeOnCreate() }

    // Sets a black environment if a custom one isn't set. With a custom background at 0
    // passthrough set, the system will minimize the application if the user leaves the boundary for
    // an extended time.
    DisposableEffect(Unit) {
        val session = checkNotNull(session) { "Session is required" }
        var temporaryEnvironmentSet = false
        val previousPassthrough = session.scene.spatialEnvironment.preferredPassthroughOpacity

        if (session.scene.spatialEnvironment.preferredSpatialEnvironment == null) {
            session.scene.spatialEnvironment.preferredSpatialEnvironment =
                SpatialEnvironment.SpatialEnvironmentPreference(skybox = null, geometry = null)
            temporaryEnvironmentSet = true
        }

        // Since we lack better boundary APIs at the moment, we will correlate full
        // passthrough being on as an indication that boundary is unavailable, as we set
        // passthrough to 0 if boundary is available.
        val passthroughListener = { passthrough: Float ->
            coreSurfaceEntity.isBoundaryAvailable = passthrough != 1.0f
        }
        session.scene.spatialEnvironment.addOnPassthroughOpacityChangedListener(passthroughListener)
        session.scene.spatialEnvironment.preferredPassthroughOpacity = 0.0f

        onDispose {
            session.scene.spatialEnvironment.removeOnPassthroughOpacityChangedListener(
                passthroughListener
            )
            if (temporaryEnvironmentSet) {
                session.scene.spatialEnvironment.preferredSpatialEnvironment = null
            }
            session.scene.spatialEnvironment.preferredPassthroughOpacity = previousPassthrough
        }
    }

    key(coreSurfaceEntity) {
        val density = LocalDensity.current
        SubspaceLayout(
            modifier = modifier,
            coreEntity = coreSurfaceEntity,
            content = {
                SpatialBox(
                    modifier =
                        SubspaceModifier.fillMaxSize()
                            .offset(
                                z = radius * SPHERE_CONTENT_Z_OFFSET_PERCENT,
                                y = radius * SPHERE_CONTENT_Y_OFFSET_PERCENT,
                            )
                ) {
                    instance.content()
                }
            },
            measurePolicy = SphereMeasurePolicy(with(density) { radius.roundToPx() }),
        )
    }
}

/** Uses [radius] to measure a cube out of the hemisphere or front half of a sphere. */
internal class SphereMeasurePolicy(private val radius: Int) : SubspaceMeasurePolicy {
    override fun SubspaceMeasureScope.measure(
        measurables: List<SubspaceMeasurable>,
        constraints: VolumeConstraints,
    ): SubspaceMeasureResult {
        if (measurables.isEmpty()) {
            return layout(radius * 2, radius * 2, radius) {}
        }

        val contentConstraints =
            VolumeConstraints(maxWidth = radius * 2, maxHeight = radius * 2, maxDepth = radius)

        val placeables = arrayOfNulls<SubspacePlaceable>(measurables.size)
        var boxWidth = radius * 2
        var boxHeight = radius * 2
        var boxDepth = radius
        measurables.fastForEachIndexed { index, measurable ->
            val placeable = measurable.measure(contentConstraints)
            placeables[index] = placeable
            boxWidth = max(boxWidth, placeable.measuredWidth)
            boxHeight = max(boxHeight, placeable.measuredHeight)
            boxDepth = max(boxDepth, placeable.measuredDepth)
        }

        return layout(boxWidth, boxHeight, boxDepth) {
            val space = IntVolumeSize(boxWidth, boxHeight, boxDepth)
            placeables.forEachIndexed { index, placeable ->
                placeable as SubspacePlaceable
                val measurable = measurables[index]
                val childSpatialAlignment =
                    SphereParentData(SpatialAlignment.Center)
                        .also { measurable.adjustParams(it) }
                        .alignment
                placeable.place(Pose(childSpatialAlignment.position(placeable.size(), space)))
            }
        }
    }

    private fun SubspacePlaceable.size() =
        IntVolumeSize(measuredWidth, measuredHeight, measuredDepth)
}

private data class SphereParentData(var alignment: SpatialAlignment) : ParentLayoutParamsAdjustable

private const val SPHERE_CONTENT_Z_OFFSET_PERCENT = -0.11f
private const val SPHERE_CONTENT_Y_OFFSET_PERCENT = SPHERE_CONTENT_Z_OFFSET_PERCENT / 3
