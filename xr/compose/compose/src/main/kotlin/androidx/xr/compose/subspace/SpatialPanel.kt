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

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Rect
import android.view.View
import android.view.View.MeasureSpec
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.RestrictTo
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.runtime.Applier
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposeNode
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.currentComposer
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.UiComposable
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastMaxOfOrNull
import androidx.core.graphics.drawable.toDrawable
import androidx.xr.compose.platform.LocalDialogManager
import androidx.xr.compose.platform.LocalOpaqueEntity
import androidx.xr.compose.platform.LocalSession
import androidx.xr.compose.platform.coreMainPanelEntity
import androidx.xr.compose.platform.getActivity
import androidx.xr.compose.subspace.layout.MeasurePolicy
import androidx.xr.compose.subspace.layout.SpatialRoundedCornerShape
import androidx.xr.compose.subspace.layout.SpatialShape
import androidx.xr.compose.subspace.layout.SubspaceLayout
import androidx.xr.compose.subspace.layout.SubspaceModifier
import androidx.xr.compose.subspace.node.ComposeSubspaceNode
import androidx.xr.compose.subspace.node.ComposeSubspaceNode.Companion.SetCompositionLocalMap
import androidx.xr.compose.subspace.node.ComposeSubspaceNode.Companion.SetCoreEntity
import androidx.xr.compose.subspace.node.ComposeSubspaceNode.Companion.SetMeasurePolicy
import androidx.xr.compose.subspace.node.ComposeSubspaceNode.Companion.SetModifier
import androidx.xr.compose.unit.Meter.Companion.millimeters
import androidx.xr.compose.unit.toMeter
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.ActivityPanelEntity
import androidx.xr.scenecore.Dimensions
import androidx.xr.scenecore.PanelEntity

private const val DEFAULT_SIZE_PX = 400

// Max allowed size for makeMeasureSpec is (1 << MeasureSpec.MODE_SHIFT) - 1.
private const val MAX_MEASURE_SPEC_SIZE = (1 shl 30) - 1

/** Set the scrim alpha to 32% opacity across all spatial panels. */
private const val DEFAULT_SCRIM_ALPHA = 0x52000000

/** Contains default values used by spatial panels. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public object SpatialPanelDefaults {

    /** Default shape for a Spatial Panel. */
    public val shape: SpatialShape = SpatialRoundedCornerShape(CornerSize(32.dp))

    /** Default minimum dimensions for a Spatial Panel in Meters. */
    public val minimumPanelDimension: Dimensions = Dimensions(0.1f, 0.1f, 0.1f)
}

/**
 * Creates a [SpatialPanel] representing a 2D plane in 3D space in which an application can fill
 * content.
 *
 * @param view Content view to be displayed within the SpatialPanel.
 * @param modifier SubspaceModifiers to apply to the SpatialPanel.
 * @param shape The shape of this Spatial Panel.
 */
@Composable
@SubspaceComposable
@Deprecated("Use SpatialPanel(factory, modifier, update, shape) instead.")
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public fun SpatialPanel(
    view: View,
    modifier: SubspaceModifier = SubspaceModifier,
    shape: SpatialShape = SpatialPanelDefaults.shape,
) {
    val frameLayout = remember {
        FrameLayout(view.context).also {
            if (view.parent != it) {
                val parent = view.parent as? ViewGroup
                parent?.removeView(view)
                it.addView(view)
            }
        }
    }
    val scrim = remember { View(view.context) }
    val dialogManager = LocalDialogManager.current
    val corePanelEntity =
        rememberCorePanelEntity(shape = shape) {
            PanelEntity.create(
                session = this,
                view = frameLayout,
                dimensions = SpatialPanelDefaults.minimumPanelDimension,
                name = entityName("SpatialPanel"),
                pose = Pose.Identity,
            )
        }

    LaunchedEffect(dialogManager.isSpatialDialogActive.value) {
        if (dialogManager.isSpatialDialogActive.value) {
            scrim.setBackgroundColor(DEFAULT_SCRIM_ALPHA)

            if (scrim.parent == null) {
                val scrimLayoutParams =
                    FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT,
                    )
                frameLayout.addView(scrim, scrimLayoutParams)
            }

            scrim.setOnClickListener { dialogManager.isSpatialDialogActive.value = false }
        } else {
            frameLayout.removeView(scrim)
        }
    }

    SubspaceLayout(modifier = modifier, coreEntity = corePanelEntity) { _, constraints ->
        view.measure(
            MeasureSpec.makeMeasureSpec(
                constraints.maxWidth.coerceAtMost(MAX_MEASURE_SPEC_SIZE),
                MeasureSpec.AT_MOST,
            ),
            MeasureSpec.makeMeasureSpec(
                constraints.maxHeight.coerceAtMost(MAX_MEASURE_SPEC_SIZE),
                MeasureSpec.AT_MOST,
            ),
        )
        val width = view.measuredWidth.coerceIn(constraints.minWidth, constraints.maxWidth)
        val height = view.measuredHeight.coerceIn(constraints.minHeight, constraints.maxHeight)
        val depth = constraints.minDepth.coerceAtLeast(0)
        layout(width, height, depth) {}
    }
}

/**
 * Creates a [SpatialPanel] representing a 2D plane in 3D space where an Android View will be
 * hosted.
 *
 * The presented View is obtained from [factory]. The [factory] block will be called exactly once to
 * obtain the [View] being composed into this panel, and it is also guaranteed to be executed on the
 * main thread. Therefore, in addition to creating the [View], the [factory] block can also be used
 * to perform one-off initializations and [View] constant properties' setting. The factory inside of
 * the constructor is used to avoid the need to pass the context to the factory. There is one [View]
 * for every [SpatialPanel] instance and it is reused across recompositions. This [View] is shown
 * effectively in isolation and does not interact directly with the other composable's that surround
 * it. The [update] block can run multiple times (on the UI thread as well) due to recomposition,
 * and it is the right place to set the new properties. Note that the block will also run once right
 * after the [factory] block completes. [SpatialPanel] will clip the view content to fit the panel.
 *
 * @param T The type of the Android View to be created.
 * @param factory A lambda that creates an instance of the Android View [T].
 * @param modifier SubspaceModifiers to apply to the SpatialPanel.
 * @param update A lambda that allows updating the created Android View [T].
 * @param shape The shape of this Spatial Panel.
 */
@Composable
@SubspaceComposable
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public fun <T : View> SpatialPanel(
    factory: (Context) -> T,
    modifier: SubspaceModifier = SubspaceModifier,
    update: (T) -> Unit = {},
    shape: SpatialShape = SpatialPanelDefaults.shape,
) {
    val dialogManager = LocalDialogManager.current
    val context = LocalContext.current

    @Suppress("UnnecessaryLambdaCreation")
    AndroidViewPanel(
        factory = { factory(context) },
        modifier = modifier,
        update = { view ->
            if (dialogManager.isSpatialDialogActive.value) {
                view.foreground = DEFAULT_SCRIM_ALPHA.toDrawable()
                view.setOnClickListener { dialogManager.isSpatialDialogActive.value = false }
            } else {
                view.foreground = Color.TRANSPARENT.toDrawable()
                view.setOnClickListener(null)
            }
            update(view)
        },
        shape = shape,
    )
}

/**
 * Private [SpatialPanel] implementation that reports its created PanelEntity. ComposeNode is used
 * directly for better timing when it comes to Update invocations.
 *
 * @param factory A lambda that creates an instance of the Android View [T].
 * @param modifier SubspaceModifiers.
 * @param update A lambda that allows updating the created Android View [T].
 * @param shape The shape of this Spatial Panel.
 */
@Composable
@SubspaceComposable
private fun <T : View> AndroidViewPanel(
    factory: (Context) -> T,
    modifier: SubspaceModifier = SubspaceModifier,
    update: (T) -> Unit = {},
    shape: SpatialShape,
) {
    val context = LocalContext.current
    val view = remember { factory(context) }

    val corePanelEntity =
        rememberCorePanelEntity(shape = shape) {
            PanelEntity.create(
                session = this,
                view = view,
                dimensions = SpatialPanelDefaults.minimumPanelDimension,
                name = "ViewPanel",
                pose = Pose.Identity,
            )
        }

    val measurePolicy = MeasurePolicy { _, constraints ->
        view.measure(
            MeasureSpec.makeMeasureSpec(
                constraints.maxWidth.coerceAtMost(MAX_MEASURE_SPEC_SIZE),
                MeasureSpec.AT_MOST,
            ),
            MeasureSpec.makeMeasureSpec(
                constraints.maxHeight.coerceAtMost(MAX_MEASURE_SPEC_SIZE),
                MeasureSpec.AT_MOST,
            ),
        )
        val width = view.measuredWidth.coerceIn(constraints.minWidth, constraints.maxWidth)
        val height = view.measuredHeight.coerceIn(constraints.minHeight, constraints.maxHeight)
        val depth = constraints.minDepth.coerceAtLeast(0)
        layout(width, height, depth) {}
    }

    val compositionLocalMap = currentComposer.currentCompositionLocalMap
    ComposeNode<ComposeSubspaceNode, Applier<Any>>(
        factory = ComposeSubspaceNode.Constructor,
        update = {
            set(compositionLocalMap, SetCompositionLocalMap)
            set(measurePolicy, SetMeasurePolicy)
            set(corePanelEntity, SetCoreEntity)
            // TODO(b/390674036) Remove call-order dependency between SetCoreEntity and SetModifier
            // Execute SetModifier after SetCoreEntity, it depends on CoreEntity.
            set(modifier, SetModifier)
            update(view)
        },
    )
}

/**
 * Creates a [SpatialPanel] representing a 2D plane in 3D space in which an application can fill
 * content.
 *
 * @param modifier SubspaceModifiers to apply to the SpatialPanel.
 * @param shape The shape of this Spatial Panel.
 * @param content The composable content to render within the SpatialPanel.
 */
@Composable
@SubspaceComposable
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public fun SpatialPanel(
    modifier: SubspaceModifier = SubspaceModifier,
    shape: SpatialShape = SpatialPanelDefaults.shape,
    content: @Composable @UiComposable () -> Unit,
) {
    val view = rememberComposeView()
    val dialogManager = LocalDialogManager.current
    val corePanelEntity =
        rememberCorePanelEntity(shape = shape) {
            PanelEntity.create(
                session = this,
                view = view,
                dimensions = SpatialPanelDefaults.minimumPanelDimension,
                name = entityName("SpatialPanel"),
                pose = Pose.Identity,
            )
        }
    var intrinsicWidth by remember { mutableIntStateOf(DEFAULT_SIZE_PX) }
    var intrinsicHeight by remember { mutableIntStateOf(DEFAULT_SIZE_PX) }

    SubspaceLayout(modifier = modifier, coreEntity = corePanelEntity) { _, volumeConstraints ->
        view.setContent {
            CompositionLocalProvider(LocalOpaqueEntity provides corePanelEntity) {
                Layout(content = content, modifier = Modifier) { measurables, constraints ->
                    intrinsicWidth =
                        measurables.fastMaxOfOrNull {
                            try {
                                it.maxIntrinsicWidth(volumeConstraints.maxHeight)
                            } catch (e: IllegalStateException) {
                                0
                            }
                        } ?: DEFAULT_SIZE_PX
                    intrinsicHeight =
                        measurables.fastMaxOfOrNull {
                            try {
                                it.maxIntrinsicHeight(volumeConstraints.maxWidth)
                            } catch (e: IllegalStateException) {
                                0
                            }
                        } ?: DEFAULT_SIZE_PX
                    val placeables = measurables.map { it.measure(constraints) }
                    layout(
                        placeables.fastMaxOfOrNull { it.measuredWidth } ?: DEFAULT_SIZE_PX,
                        placeables.fastMaxOfOrNull { it.measuredHeight } ?: DEFAULT_SIZE_PX,
                    ) {
                        placeables.fastForEach { placeable -> placeable.place(0, 0) }
                    }
                }
            }
            if (dialogManager.isSpatialDialogActive.value) {
                view.foreground = DEFAULT_SCRIM_ALPHA.toDrawable()
                Box(
                    modifier =
                        Modifier.fillMaxSize().pointerInput(Unit) {
                            detectTapGestures { dialogManager.isSpatialDialogActive.value = false }
                        }
                ) {}
            } else {
                view.foreground = Color.TRANSPARENT.toDrawable()
            }
        }
        val width = intrinsicWidth.coerceIn(volumeConstraints.minWidth, volumeConstraints.maxWidth)
        val height =
            intrinsicHeight.coerceIn(volumeConstraints.minHeight, volumeConstraints.maxHeight)
        val depth = volumeConstraints.minDepth.coerceAtLeast(0)
        layout(width, height, depth) {}
    }
}

/**
 * Creates a [SpatialPanel] backed by the main Window content.
 *
 * This panel requires the following specific configuration in the Android Manifest for proper
 * sizing/resizing behavior:
 * ```
 * <activity
 * android:configChanges="orientation|screenSize|screenLayout|smallestScreenSize>
 * <!--suppress AndroidElementNotAllowed -->
 * <layout android:defaultWidth="50dp" android:defaultHeight="50dp" android:minHeight="50dp"
 * android:minWidth="50dp"/>
 * </activity>
 * ```
 *
 * @param modifier SubspaceModifier to apply to the MainPanel.
 * @param shape The shape of this Spatial Panel.
 */
@Composable
@SubspaceComposable
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public fun MainPanel(
    modifier: SubspaceModifier = SubspaceModifier,
    shape: SpatialShape = SpatialPanelDefaults.shape,
) {
    val session = checkNotNull(LocalSession.current) { "session must be initialized" }
    val mainPanel = session.coreMainPanelEntity
    LaunchedEffect(shape) { mainPanel.shape = shape }

    val view = LocalContext.current.getActivity().window?.decorView ?: LocalView.current

    // When the MainPanel enters the compose hierarchy, we can't directly set the mainPanel.hidden
    // to false here because the hidden state is a subcomponent of the size calculation, see
    // [SubspaceLayoutNode.MeasureLayout.placeAt] and [CoreEntity.size].
    // This means hidden will be set after layout completes, on the first frame when the MainPanel
    // enters the Compose hierarchy.
    DisposableEffect(Unit) { onDispose { mainPanel.hidden = true } }

    SubspaceLayout(modifier = modifier, coreEntity = mainPanel) { _, constraints ->
        val width = view.measuredWidth.coerceIn(constraints.minWidth, constraints.maxWidth)
        val height = view.measuredHeight.coerceIn(constraints.minHeight, constraints.maxHeight)
        val depth = constraints.minDepth.coerceAtLeast(0)
        layout(width, height, depth) {}
    }
}

/**
 * Creates a [SpatialPanel] and launches an Activity within it.
 *
 * @param intent The intent of an Activity to launch within this panel.
 * @param modifier SubspaceModifiers to apply to the SpatialPanel.
 * @param shape The shape of this Spatial Panel.
 */
@Composable
@SubspaceComposable
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public fun SpatialPanel(
    intent: Intent,
    modifier: SubspaceModifier = SubspaceModifier,
    shape: SpatialShape = SpatialPanelDefaults.shape,
) {
    val session = checkNotNull(LocalSession.current) { "session must be initialized" }
    val dialogManager = LocalDialogManager.current
    val density = LocalDensity.current

    val rect = Rect(0, 0, DEFAULT_SIZE_PX, DEFAULT_SIZE_PX)
    val activityPanelEntity = rememberCorePanelEntity {
        ActivityPanelEntity.create(session, rect, entityName("ActivityPanel-${intent.action}"))
            .also { it.launchActivity(intent) }
    }

    val scrimWidth = with(density) { activityPanelEntity.size.width.toDp().toMeter() }
    val scrimHeight = with(density) { activityPanelEntity.size.height.toDp().toMeter() }

    SpatialBox {
        SubspaceLayout(modifier = modifier, coreEntity = activityPanelEntity) { _, constraints ->
            val width = DEFAULT_SIZE_PX.coerceIn(constraints.minWidth, constraints.maxWidth)
            val height = DEFAULT_SIZE_PX.coerceIn(constraints.minHeight, constraints.maxHeight)
            val depth = constraints.minDepth.coerceAtLeast(0)
            layout(width, height, depth) {}
        }

        if (dialogManager.isSpatialDialogActive.value) {
            val localContext = LocalContext.current
            val scrimView = remember {
                View(localContext).apply {
                    foreground = DEFAULT_SCRIM_ALPHA.toDrawable()
                    setOnClickListener { dialogManager.isSpatialDialogActive.value = false }
                }
            }

            val scrimPanelEntity =
                rememberCorePanelEntity(shape = shape) {
                    PanelEntity.create(
                            session = session,
                            view = scrimView,
                            dimensions = Dimensions(scrimWidth.toM(), scrimHeight.toM()),
                            name = entityName("ScrimPanel"),
                            pose = Pose.Identity,
                        )
                        .apply {
                            setParent(activityPanelEntity.entity)
                            setPose(Pose(translation = Vector3(0f, 0f, 3.millimeters.toM())))
                        }
                }

            LaunchedEffect(activityPanelEntity.size) {
                scrimPanelEntity.size = activityPanelEntity.size
            }
        }
    }
}
