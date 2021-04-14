/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.compose.ui.platform

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.graphics.Matrix
import android.graphics.Rect
import android.os.Build
import android.os.Looper
import android.util.Log
import android.util.SparseArray
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewStructure
import android.view.ViewTreeObserver
import android.view.animation.AnimationUtils
import android.view.autofill.AutofillValue
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import androidx.annotation.DoNotInline
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.AndroidAutofill
import androidx.compose.ui.autofill.Autofill
import androidx.compose.ui.autofill.AutofillTree
import androidx.compose.ui.autofill.performAutofill
import androidx.compose.ui.autofill.populateViewStructure
import androidx.compose.ui.autofill.registerCallback
import androidx.compose.ui.autofill.unregisterCallback
import androidx.compose.ui.focus.FOCUS_TAG
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusDirectionInternal
import androidx.compose.ui.focus.FocusDirectionInternal.Down
import androidx.compose.ui.focus.FocusDirectionInternal.In
import androidx.compose.ui.focus.FocusDirectionInternal.Left
import androidx.compose.ui.focus.FocusDirectionInternal.Next
import androidx.compose.ui.focus.FocusDirectionInternal.Out
import androidx.compose.ui.focus.FocusDirectionInternal.Previous
import androidx.compose.ui.focus.FocusDirectionInternal.Right
import androidx.compose.ui.focus.FocusDirectionInternal.Up
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.FocusManagerImpl
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.CanvasHolder
import androidx.compose.ui.hapticfeedback.AndroidHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.input.key.Key.Companion.DirectionCenter
import androidx.compose.ui.input.key.Key.Companion.DirectionDown
import androidx.compose.ui.input.key.Key.Companion.DirectionLeft
import androidx.compose.ui.input.key.Key.Companion.DirectionRight
import androidx.compose.ui.input.key.Key.Companion.DirectionUp
import androidx.compose.ui.input.key.Key.Companion.Tab
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType.KeyDown
import androidx.compose.ui.input.key.KeyInputModifier
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.MotionEventAdapter
import androidx.compose.ui.input.pointer.PointerInputEventProcessor
import androidx.compose.ui.input.pointer.PositionCalculator
import androidx.compose.ui.input.pointer.ProcessResult
import androidx.compose.ui.layout.RootMeasurePolicy
import androidx.compose.ui.node.InternalCoreApi
import androidx.compose.ui.node.LayoutNode
import androidx.compose.ui.node.LayoutNode.UsageByParent
import androidx.compose.ui.node.MeasureAndLayoutDelegate
import androidx.compose.ui.node.OwnedLayer
import androidx.compose.ui.node.Owner
import androidx.compose.ui.node.OwnerSnapshotObserver
import androidx.compose.ui.node.RootForTest
import androidx.compose.ui.semantics.SemanticsModifierCore
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsOwner
import androidx.compose.ui.semantics.outerSemantics
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.input.PlatformTextInputService
import androidx.compose.ui.text.input.TextInputService
import androidx.compose.ui.text.input.TextInputServiceAndroid
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.util.trace
import androidx.compose.ui.viewinterop.AndroidViewHolder
import androidx.core.view.AccessibilityDelegateCompat
import androidx.core.view.ViewCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import androidx.core.view.accessibility.AccessibilityNodeProviderCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.ViewTreeSavedStateRegistryOwner
import java.lang.reflect.Method
import android.view.KeyEvent as AndroidKeyEvent

@SuppressLint("ViewConstructor", "VisibleForTests")
@OptIn(ExperimentalComposeUiApi::class)
@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
internal class AndroidComposeView(context: Context) :
    ViewGroup(context), Owner, ViewRootForTest, PositionCalculator {

    /**
     * Signal that AndroidComposeView's superclass constructors have finished running.
     * If this is false, it's because the runtime's default uninitialized value is currently
     * visible and AndroidComposeView's constructor hasn't started running yet. In this state
     * other expected invariants do not hold, e.g. property delegates may not be initialized.
     * View/ViewGroup have a history of calling non-final methods in their constructors that
     * can lead to this case, e.g. [onRtlPropertiesChanged].
     */
    private var superclassInitComplete = true

    override val view: View get() = this

    override var density = Density(context)
        private set

    private val semanticsModifier = SemanticsModifierCore(
        id = SemanticsModifierCore.generateSemanticsId(),
        mergeDescendants = false,
        clearAndSetSemantics = false,
        properties = {}
    )

    private val _focusManager: FocusManagerImpl = FocusManagerImpl()
    override val focusManager: FocusManager
        get() = _focusManager

    private val _windowInfo: WindowInfoImpl = WindowInfoImpl()
    override val windowInfo: WindowInfo
        get() = _windowInfo

    // TODO(b/177931787) : Consider creating a KeyInputManager like we have for FocusManager so
    //  that this common logic can be used by all owners.
    private val keyInputModifier: KeyInputModifier = KeyInputModifier(
        onKeyEvent = {
            val focusDirection = getFocusDirection(it)
            if (focusDirection == null || it.type != KeyDown) return@KeyInputModifier false

            val focusMoveSuccess = with(focusManager) {
                when (focusDirection) {
                    Up -> moveFocus(FocusDirection.Up)
                    Down -> moveFocus(FocusDirection.Down)
                    Left -> moveFocus(FocusDirection.Left)
                    Right -> moveFocus(FocusDirection.Right)
                    In -> moveFocusIn()
                    Out -> moveFocusOut()
                    Next -> moveFocus(FocusDirection.Next)
                    Previous -> moveFocus(FocusDirection.Previous)
                }
            }

            // Consume the key event if we moved focus.
            focusMoveSuccess
        },
        onPreviewKeyEvent = null
    )

    private val canvasHolder = CanvasHolder()

    override val root = LayoutNode().also {
        it.measurePolicy = RootMeasurePolicy
        it.modifier = Modifier
            .then(semanticsModifier)
            .then(_focusManager.modifier)
            .then(keyInputModifier)
    }

    override val rootForTest: RootForTest = this

    override val semanticsOwner: SemanticsOwner = SemanticsOwner(root)
    private val accessibilityDelegate = AndroidComposeViewAccessibilityDelegateCompat(this)

    // Used by components that want to provide autofill semantic information.
    // TODO: Replace with SemanticsTree: Temporary hack until we have a semantics tree implemented.
    // TODO: Replace with SemanticsTree.
    //  This is a temporary hack until we have a semantics tree implemented.
    override val autofillTree = AutofillTree()

    // OwnedLayers that are dirty and should be redrawn.
    internal val dirtyLayers = mutableListOf<OwnedLayer>()

    private val motionEventAdapter = MotionEventAdapter()
    private val pointerInputEventProcessor = PointerInputEventProcessor(root)

    // TODO(mount): reinstate when coroutines are supported by IR compiler
    // private val ownerScope = CoroutineScope(Dispatchers.Main.immediate + Job())

    /**
     * Used for updating LocalConfiguration when configuration changes - consume LocalConfiguration
     * instead of changing this observer if you are writing a component that adapts to
     * configuration changes.
     */
    var configurationChangeObserver: (Configuration) -> Unit = {}

    private val _autofill = if (autofillSupported()) AndroidAutofill(this, autofillTree) else null

    // Used as a CompositionLocal for performing autofill.
    override val autofill: Autofill? get() = _autofill

    private var observationClearRequested = false

    /**
     * Provide clipboard manager to the user. Use the Android version of clipboard manager.
     */
    override val clipboardManager = AndroidClipboardManager(context)

    /**
     * Provide accessibility manager to the user. Use the Android version of accessibility manager.
     */
    override val accessibilityManager = AndroidAccessibilityManager(context)

    override val snapshotObserver = OwnerSnapshotObserver { command ->
        if (handler?.looper === Looper.myLooper()) {
            command()
        } else {
            handler?.post(command)
        }
    }

    @OptIn(InternalCoreApi::class)
    override var showLayoutBounds = false

    private var _androidViewsHandler: AndroidViewsHandler? = null
    internal val androidViewsHandler: AndroidViewsHandler
        get() {
            if (_androidViewsHandler == null) {
                _androidViewsHandler = AndroidViewsHandler(context)
                addView(_androidViewsHandler)
            }
            return _androidViewsHandler!!
        }
    private var viewLayersContainer: DrawChildContainer? = null

    // The constraints being used by the last onMeasure. It is set to null in onLayout. It allows
    // us to detect the case when the View was measured twice with different constraints within
    // the same measure pass.
    private var onMeasureConstraints: Constraints? = null

    // Will be set to true when we were measured twice with different constraints during the last
    // measure pass.
    private var wasMeasuredWithMultipleConstraints = false

    private val measureAndLayoutDelegate = MeasureAndLayoutDelegate(root)

    override val measureIteration: Long get() = measureAndLayoutDelegate.measureIteration
    override val viewConfiguration: ViewConfiguration =
        AndroidViewConfiguration(android.view.ViewConfiguration.get(context))

    override val hasPendingMeasureOrLayout
        get() = measureAndLayoutDelegate.hasPendingMeasureOrLayout

    private var globalPosition: IntOffset = IntOffset.Zero

    private val tmpPositionArray = intArrayOf(0, 0)
    private val tmpOffsetArray = floatArrayOf(0f, 0f)
    private val viewToWindowMatrix = Matrix()
    private val windowToViewMatrix = Matrix()
    private var lastMatrixRecalculationAnimationTime = -1L

    /**
     * On some devices, the `getLocationOnScreen()` returns `(0, 0)` even when the Window
     * is offset in special circumstances. This contains the screen coordinates of the containing
     * Window the last time the [viewToWindowMatrix] and [windowToViewMatrix] were recalculated.
     */
    private var windowPosition = Offset.Infinite

    // Used to track whether or not there was an exception while creating an MRenderNode
    // so that we don't have to continue using try/catch after fails once.
    private var isRenderNodeCompatible = true

    /**
     * Current [ViewTreeOwners]. Use [setOnViewTreeOwnersAvailable] if you want to
     * execute your code when the object will be created.
     */
    var viewTreeOwners: ViewTreeOwners? = null
        private set

    private var onViewTreeOwnersAvailable: ((ViewTreeOwners) -> Unit)? = null

    // executed when the layout pass has been finished. as a result of it our view could be moved
    // inside the window (we are interested not only in the event when our parent positioned us
    // on a different position, but also in the position of each of the grandparents as all these
    // positions add up to final global position)
    private val globalLayoutListener = ViewTreeObserver.OnGlobalLayoutListener {
        updatePositionCacheAndDispatch()
    }

    // executed when a scrolling container like ScrollView of RecyclerView performed the scroll,
    // this could affect our global position
    private val scrollChangedListener = ViewTreeObserver.OnScrollChangedListener {
        updatePositionCacheAndDispatch()
    }

    private val textInputServiceAndroid = TextInputServiceAndroid(this)

    @OptIn(InternalComposeUiApi::class)
    override val textInputService = textInputServiceFactory(textInputServiceAndroid)

    override val fontLoader: Font.ResourceLoader = AndroidFontResourceLoader(context)

    // Backed by mutableStateOf so that the ambient provider recomposes when it changes
    override var layoutDirection by mutableStateOf(
        context.resources.configuration.localeLayoutDirection
    )
        private set

    /**
     * Provide haptic feedback to the user. Use the Android version of haptic feedback.
     */
    override val hapticFeedBack: HapticFeedback =
        AndroidHapticFeedback(this)

    /**
     * Provide textToolbar to the user, for text-related operation. Use the Android version of
     * floating toolbar(post-M) and primary toolbar(pre-M).
     */
    override val textToolbar: TextToolbar = AndroidTextToolbar(this)

    init {
        setWillNotDraw(false)
        isFocusable = true
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            AndroidComposeViewVerificationHelperMethods.focusable(
                this,
                focusable = View.FOCUSABLE,
                defaultFocusHighlightEnabled = false
            )
        }
        isFocusableInTouchMode = true
        clipChildren = false
        ViewCompat.setAccessibilityDelegate(this, accessibilityDelegate)
        ViewRootForTest.onViewCreatedCallback?.invoke(this)
        root.attach(this)
    }

    override fun onFocusChanged(gainFocus: Boolean, direction: Int, previouslyFocusedRect: Rect?) {
        super.onFocusChanged(gainFocus, direction, previouslyFocusedRect)
        Log.d(FOCUS_TAG, "Owner FocusChanged($gainFocus)")
        with(_focusManager) {
            if (gainFocus) takeFocus() else releaseFocus()
        }
    }

    override fun onWindowFocusChanged(hasWindowFocus: Boolean) {
        _windowInfo.isWindowFocused = hasWindowFocus
        super.onWindowFocusChanged(hasWindowFocus)
    }

    override fun sendKeyEvent(keyEvent: KeyEvent): Boolean {
        return keyInputModifier.processKeyInput(keyEvent)
    }

    override fun dispatchKeyEvent(event: AndroidKeyEvent) =
        if (isFocused) {
            // Focus lies within the Compose hierarchy, so we dispatch the key event to the
            // appropriate place.
            sendKeyEvent(KeyEvent(event))
        } else {
            // This Owner has a focused child view, which is a view interop use case,
            // so we use the default ViewGroup behavior which will route tke key event to the
            // focused view.
            super.dispatchKeyEvent(event)
        }

    override fun onAttach(node: LayoutNode) {
    }

    override fun onDetach(node: LayoutNode) {
        measureAndLayoutDelegate.onNodeDetached(node)
        requestClearInvalidObservations()
    }

    fun requestClearInvalidObservations() {
        observationClearRequested = true
    }

    internal fun clearInvalidObservations() {
        if (observationClearRequested) {
            snapshotObserver.clearInvalidObservations()
            observationClearRequested = false
        }
        val childAndroidViews = _androidViewsHandler
        if (childAndroidViews != null) {
            clearChildInvalidObservations(childAndroidViews)
        }
    }

    private fun clearChildInvalidObservations(viewGroup: ViewGroup) {
        for (i in 0 until viewGroup.childCount) {
            val child = viewGroup.getChildAt(i)
            if (child is AndroidComposeView) {
                child.clearInvalidObservations()
            } else if (child is ViewGroup) {
                clearChildInvalidObservations(child)
            }
        }
    }

    /**
     * Called to inform the owner that a new Android [View] was [attached][Owner.onAttach]
     * to the hierarchy.
     */
    fun addAndroidView(view: AndroidViewHolder, layoutNode: LayoutNode) {
        androidViewsHandler.holderToLayoutNode[view] = layoutNode
        androidViewsHandler.addView(view)
        androidViewsHandler.layoutNodeToHolder[layoutNode] = view
        // Fetching AccessibilityNodeInfo from a View which is not set to
        // IMPORTANT_FOR_ACCESSIBILITY_YES will return null.
        ViewCompat.setImportantForAccessibility(
            view,
            ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_YES
        )
        val thisView = this
        ViewCompat.setAccessibilityDelegate(
            view,
            object : AccessibilityDelegateCompat() {
                override fun onInitializeAccessibilityNodeInfo(
                    host: View?,
                    info: AccessibilityNodeInfoCompat?
                ) {
                    super.onInitializeAccessibilityNodeInfo(host, info)
                    var parentId = SemanticsNode(layoutNode.outerSemantics!!, true).parent!!.id
                    if (parentId == semanticsOwner.rootSemanticsNode.id) {
                        parentId = AccessibilityNodeProviderCompat.HOST_VIEW_ID
                    }
                    info!!.setParent(thisView, parentId)
                }
            }
        )
    }

    /**
     * Called to inform the owner that an Android [View] was [detached][Owner.onDetach]
     * from the hierarchy.
     */
    fun removeAndroidView(view: AndroidViewHolder) {
        androidViewsHandler.removeView(view)
        androidViewsHandler.holderToLayoutNode.remove(view)
        androidViewsHandler.layoutNodeToHolder.remove(
            androidViewsHandler.holderToLayoutNode[view]
        )
        ViewCompat.setImportantForAccessibility(
            view,
            ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_AUTO
        )
    }

    /**
     * Called to ask the owner to draw a child Android [View] to [canvas].
     */
    fun drawAndroidView(view: AndroidViewHolder, canvas: android.graphics.Canvas) {
        androidViewsHandler.drawView(view, canvas)
    }

    private fun scheduleMeasureAndLayout(nodeToRemeasure: LayoutNode? = null) {
        if (!isLayoutRequested && isAttachedToWindow) {
            if (wasMeasuredWithMultipleConstraints && nodeToRemeasure != null) {
                // if nodeToRemeasure can potentially resize the root and the view was measured
                // twice with different constraints last time it means the constraints we have could
                // be not the final constraints and in fact our parent ViewGroup can remeasure us
                // with larger constraints if we call requestLayout()
                var node = nodeToRemeasure
                while (node != null && node.measuredByParent == UsageByParent.InMeasureBlock) {
                    node = node.parent
                }
                if (node === root) {
                    requestLayout()
                    return
                }
            }
            if (width == 0 || height == 0) {
                // if the view has no size calling invalidate() will be skipped
                requestLayout()
            } else {
                invalidate()
            }
        }
    }

    override fun measureAndLayout() {
        val rootNodeResized = measureAndLayoutDelegate.measureAndLayout()
        if (rootNodeResized) {
            requestLayout()
        }
        measureAndLayoutDelegate.dispatchOnPositionedCallbacks()
    }

    override fun onRequestMeasure(layoutNode: LayoutNode) {
        if (measureAndLayoutDelegate.requestRemeasure(layoutNode)) {
            scheduleMeasureAndLayout(layoutNode)
        }
    }

    override fun onRequestRelayout(layoutNode: LayoutNode) {
        if (measureAndLayoutDelegate.requestRelayout(layoutNode)) {
            scheduleMeasureAndLayout()
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        trace("AndroidOwner:onMeasure") {
            if (!isAttachedToWindow) {
                invalidateLayoutNodeMeasurement(root)
            }
            val (minWidth, maxWidth) = convertMeasureSpec(widthMeasureSpec)
            val (minHeight, maxHeight) = convertMeasureSpec(heightMeasureSpec)

            val constraints = Constraints(minWidth, maxWidth, minHeight, maxHeight)
            if (onMeasureConstraints == null) {
                // first onMeasure after last onLayout
                onMeasureConstraints = constraints
                wasMeasuredWithMultipleConstraints = false
            } else if (onMeasureConstraints != constraints) {
                // we were remeasured twice with different constraints after last onLayout
                wasMeasuredWithMultipleConstraints = true
            }
            measureAndLayoutDelegate.updateRootConstraints(constraints)
            measureAndLayoutDelegate.measureAndLayout()
            setMeasuredDimension(root.width, root.height)
        }
    }

    private fun convertMeasureSpec(measureSpec: Int): Pair<Int, Int> {
        val mode = MeasureSpec.getMode(measureSpec)
        val size = MeasureSpec.getSize(measureSpec)
        return when (mode) {
            MeasureSpec.EXACTLY -> size to size
            MeasureSpec.UNSPECIFIED -> 0 to Constraints.Infinity
            MeasureSpec.AT_MOST -> 0 to size
            else -> throw IllegalStateException()
        }
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        onMeasureConstraints = null
        // we postpone onPositioned callbacks until onLayout as LayoutCoordinates
        // are currently wrong if you try to get the global(activity) coordinates -
        // View is not yet laid out.
        updatePositionCacheAndDispatch()
        if (_androidViewsHandler != null) {
            // Even if we laid out during onMeasure, we want to set the bounds of the
            // AndroidViewsHandler for accessibility and for Views making assumptions based on
            // the size of their ancestors. Usually the Views in the hierarchy will not
            // be relaid out, as they have not requested layout in the meantime.
            // However, there is also chance for the AndroidViewsHandler to be isLayoutRequested
            // at this point, in case the Views hierarchy receives forceLayout().
            // In this case, calling layout here will relayout to clear the isLayoutRequested
            // info on the Views, as otherwise further layout requests will be discarded.
            androidViewsHandler.layout(0, 0, r - l, b - t)
        }
    }

    private fun updatePositionCacheAndDispatch() {
        var positionChanged = false
        getLocationOnScreen(tmpPositionArray)
        if (globalPosition.x != tmpPositionArray[0] || globalPosition.y != tmpPositionArray[1]) {
            globalPosition = IntOffset(tmpPositionArray[0], tmpPositionArray[1])
            positionChanged = true
        }
        measureAndLayoutDelegate.dispatchOnPositionedCallbacks(forceDispatch = positionChanged)
    }

    override fun onDraw(canvas: android.graphics.Canvas) {
    }

    override fun createLayer(
        drawBlock: (Canvas) -> Unit,
        invalidateParentLayer: () -> Unit
    ): OwnedLayer {
        // RenderNode is supported on Q+ for certain, but may also be supported on M-O.
        // We can't be confident that RenderNode is supported, so we try and fail over to
        // the ViewLayer implementation. We'll try even on on P devices, but it will fail
        // until ART allows things on the unsupported list on P.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && isRenderNodeCompatible) {
            try {
                return RenderNodeLayer(
                    this,
                    drawBlock,
                    invalidateParentLayer
                )
            } catch (_: Throwable) {
                isRenderNodeCompatible = false
            }
        }
        if (viewLayersContainer == null) {
            if (!ViewLayer.hasRetrievedMethod) {
                // Test to see if updateDisplayList() can be called. If this fails then
                // ViewLayer.shouldUseDispatchDraw will be true.
                ViewLayer.updateDisplayList(View(context))
            }
            viewLayersContainer = if (ViewLayer.shouldUseDispatchDraw) {
                DrawChildContainer(context)
            } else {
                ViewLayerContainer(context)
            }
            addView(viewLayersContainer)
        }
        return ViewLayer(this, viewLayersContainer!!, drawBlock, invalidateParentLayer)
    }

    override fun onSemanticsChange() {
        accessibilityDelegate.onSemanticsChange()
    }

    override fun onLayoutChange(layoutNode: LayoutNode) {
        accessibilityDelegate.onLayoutChange(layoutNode)
    }

    override fun getFocusDirection(keyEvent: KeyEvent): FocusDirectionInternal? {
        return when (keyEvent.key) {
            Tab -> if (keyEvent.isShiftPressed) Previous else Next
            DirectionRight -> Right
            DirectionLeft -> Left
            DirectionUp -> Up
            DirectionDown -> Down
            DirectionCenter -> In
            // TODO(b/183746743): Enable Back after fixing issue with DemoTests (b/185211677).
            // If we use the back button to clear focus, then the demo tests need two Back
            // events when an item is focused. Either remove initial focus from the affected
            // Demos or call clearFocus() before sending the Back key event.
            // Back -> Out
            else -> null
        }
    }

    override fun dispatchDraw(canvas: android.graphics.Canvas) {
        if (!isAttachedToWindow) {
            invalidateLayers(root)
        }
        measureAndLayout()

        // we don't have to observe here because the root has a layer modifier
        // that will observe all children. The AndroidComposeView has only the
        // root, so it doesn't have to invalidate itself based on model changes.
        canvasHolder.drawInto(canvas) { root.draw(this) }

        if (dirtyLayers.isNotEmpty()) {
            for (i in 0 until dirtyLayers.size) {
                val layer = dirtyLayers[i]
                layer.updateDisplayList()
            }
            dirtyLayers.clear()
        }

        if (ViewLayer.shouldUseDispatchDraw) {
            // We must update the display list of all children using dispatchDraw()
            // instead of updateDisplayList(). But since we don't want to actually draw
            // the contents, we will clip out everything from the canvas.
            val saveCount = canvas.save()
            canvas.clipRect(0f, 0f, 0f, 0f)

            super.dispatchDraw(canvas)
            canvas.restoreToCount(saveCount)
        }
    }

    /**
     * The callback to be executed when [viewTreeOwners] is created and not-null anymore.
     * Note that this callback will be fired inline when it is already available
     */
    fun setOnViewTreeOwnersAvailable(callback: (ViewTreeOwners) -> Unit) {
        val viewTreeOwners = viewTreeOwners
        if (viewTreeOwners != null) {
            callback(viewTreeOwners)
        } else {
            onViewTreeOwnersAvailable = callback
        }
    }

    suspend fun boundsUpdatesEventLoop() {
        accessibilityDelegate.boundsUpdatesEventLoop()
    }

    /**
     * Android has an issue where calling showSoftwareKeyboard after calling
     * hideSoftwareKeyboard, it results in keyboard flickering and sometimes the keyboard ends up
     * being hidden even though the most recent call was to showKeyboard.
     *
     * This function starts a suspended function that listens for show/hide commands and only
     * runs the latest command.
     */
    suspend fun keyboardVisibilityEventLoop() {
        textInputServiceAndroid.keyboardVisibilityEventLoop()
    }

    /**
     * Walks the entire LayoutNode sub-hierarchy and marks all nodes as needing measurement.
     */
    private fun invalidateLayoutNodeMeasurement(node: LayoutNode) {
        measureAndLayoutDelegate.requestRemeasure(node)
        node._children.forEach { invalidateLayoutNodeMeasurement(it) }
    }

    /**
     * Walks the entire LayoutNode sub-hierarchy and marks all layers as needing to be redrawn.
     */
    private fun invalidateLayers(node: LayoutNode) {
        node.invalidateLayers()
        node._children.forEach { invalidateLayers(it) }
    }

    override fun invalidateDescendants() {
        invalidateLayers(root)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        invalidateLayoutNodeMeasurement(root)
        invalidateLayers(root)
        showLayoutBounds = getIsShowingLayoutBounds()
        snapshotObserver.startObserving()
        ifDebug { if (autofillSupported()) _autofill?.registerCallback() }

        if (viewTreeOwners == null) {
            val lifecycleOwner = ViewTreeLifecycleOwner.get(this) ?: throw IllegalStateException(
                "Composed into the View which doesn't propagate ViewTreeLifecycleOwner!"
            )
            val savedStateRegistryOwner =
                ViewTreeSavedStateRegistryOwner.get(this) ?: throw IllegalStateException(
                    "Composed into the View which doesn't propagate" +
                        "ViewTreeSavedStateRegistryOwner!"
                )
            val viewTreeOwners = ViewTreeOwners(
                lifecycleOwner = lifecycleOwner,
                savedStateRegistryOwner = savedStateRegistryOwner
            )
            this.viewTreeOwners = viewTreeOwners
            onViewTreeOwnersAvailable?.invoke(viewTreeOwners)
            onViewTreeOwnersAvailable = null
        }
        viewTreeObserver.addOnGlobalLayoutListener(globalLayoutListener)
        viewTreeObserver.addOnScrollChangedListener(scrollChangedListener)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        snapshotObserver.stopObserving()
        ifDebug { if (autofillSupported()) _autofill?.unregisterCallback() }
        viewTreeObserver.removeOnGlobalLayoutListener(globalLayoutListener)
        viewTreeObserver.removeOnScrollChangedListener(scrollChangedListener)
    }

    override fun onProvideAutofillVirtualStructure(structure: ViewStructure?, flags: Int) {
        if (autofillSupported() && structure != null) _autofill?.populateViewStructure(structure)
    }

    override fun autofill(values: SparseArray<AutofillValue>) {
        if (autofillSupported()) _autofill?.performAutofill(values)
    }

    // TODO(shepshapard): Test this method.
    override fun dispatchTouchEvent(motionEvent: MotionEvent): Boolean {
        measureAndLayout()
        val processResult = trace("AndroidOwner:onTouch") {
            val pointerInputEvent = motionEventAdapter.convertToPointerInputEvent(motionEvent, this)
            if (pointerInputEvent != null) {
                pointerInputEventProcessor.process(pointerInputEvent, this)
            } else {
                pointerInputEventProcessor.processCancel()
                ProcessResult(
                    dispatchedToAPointerInputModifier = false,
                    anyMovementConsumed = false
                )
            }
        }

        if (processResult.anyMovementConsumed) {
            parent.requestDisallowInterceptTouchEvent(true)
        }

        return processResult.dispatchedToAPointerInputModifier
    }

    override fun localToScreen(localPosition: Offset): Offset {
        recalculateWindowPosition()
        val points = tmpOffsetArray
        points[0] = localPosition.x
        points[1] = localPosition.y
        viewToWindowMatrix.mapPoints(points)
        return Offset(
            points[0] + windowPosition.x,
            points[1] + windowPosition.y
        )
    }

    override fun screenToLocal(positionOnScreen: Offset): Offset {
        recalculateWindowPosition()
        val points = tmpOffsetArray
        points[0] = positionOnScreen.x - windowPosition.x
        points[1] = positionOnScreen.y - windowPosition.y
        windowToViewMatrix.mapPoints(points)
        return Offset(points[0], points[1])
    }

    private fun recalculateWindowPosition() {
        val animationTime = AnimationUtils.currentAnimationTimeMillis()
        if (animationTime != lastMatrixRecalculationAnimationTime) {
            lastMatrixRecalculationAnimationTime = animationTime
            recalculateWindowViewTransforms()
            var viewParent = parent
            var view: View = this
            while (viewParent is ViewGroup) {
                view = viewParent
                viewParent = view.parent
            }
            view.getLocationOnScreen(tmpPositionArray)
            val screenX = tmpPositionArray[0].toFloat()
            val screenY = tmpPositionArray[1].toFloat()
            view.getLocationInWindow(tmpPositionArray)
            val windowX = tmpPositionArray[0].toFloat()
            val windowY = tmpPositionArray[1].toFloat()
            windowPosition = Offset(screenX - windowX, screenY - windowY)
        }
    }

    private fun recalculateWindowViewTransforms() {
        viewToWindowMatrix.reset()
        transformMatrixToWindow(this, viewToWindowMatrix)
        viewToWindowMatrix.invert(windowToViewMatrix)
    }

    override fun onCheckIsTextEditor(): Boolean = textInputServiceAndroid.isEditorFocused()

    override fun onCreateInputConnection(outAttrs: EditorInfo): InputConnection? =
        textInputServiceAndroid.createInputConnection(outAttrs)

    override fun calculateLocalPosition(positionInWindow: Offset): Offset {
        recalculateWindowPosition()
        val points = tmpOffsetArray
        points[0] = positionInWindow.x
        points[1] = positionInWindow.y
        windowToViewMatrix.mapPoints(points)
        return Offset(points[0], points[1])
    }

    override fun calculatePositionInWindow(localPosition: Offset): Offset {
        recalculateWindowPosition()
        val points = tmpOffsetArray
        points[0] = localPosition.x
        points[1] = localPosition.y
        viewToWindowMatrix.mapPoints(points)
        return Offset(points[0], points[1])
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        density = Density(context)
        configurationChangeObserver(newConfig)
    }

    override fun onRtlPropertiesChanged(layoutDirection: Int) {
        // This method can be called while View's constructor is running
        // by way of resolving padding in response to initScrollbars.
        // If we get such a call, don't try to write to a property delegate
        // that hasn't been initialized yet.
        if (superclassInitComplete) {
            this.layoutDirection = layoutDirectionFromInt(layoutDirection)
        }
    }

    private fun autofillSupported() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O

    public override fun dispatchHoverEvent(event: MotionEvent): Boolean {
        return accessibilityDelegate.dispatchHoverEvent(event)
    }

    private fun findViewByAccessibilityIdRootedAtCurrentView(
        accessibilityId: Int,
        currentView: View
    ): View? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            val getAccessibilityViewIdMethod = View::class.java
                .getDeclaredMethod("getAccessibilityViewId")
            getAccessibilityViewIdMethod.isAccessible = true
            if (getAccessibilityViewIdMethod.invoke(currentView) == accessibilityId) {
                return currentView
            }
            if (currentView is ViewGroup) {
                for (i in 0 until currentView.childCount) {
                    val foundView = findViewByAccessibilityIdRootedAtCurrentView(
                        accessibilityId,
                        currentView.getChildAt(i)
                    )
                    if (foundView != null) {
                        return foundView
                    }
                }
            }
        }
        return null
    }

    /**
     * This overrides an @hide method in ViewGroup. Because of the @hide, the override keyword
     * cannot be used, but the override works anyway because the ViewGroup method is not final.
     * In Android P and earlier, the call path is
     * AccessibilityInteractionController#findViewByAccessibilityId ->
     * View#findViewByAccessibilityId -> ViewGroup#findViewByAccessibilityIdTraversal. In Android
     * Q and later, AccessibilityInteractionController#findViewByAccessibilityId uses
     * AccessibilityNodeIdManager and findViewByAccessibilityIdTraversal is only used by autofill.
     */
    fun findViewByAccessibilityIdTraversal(accessibilityId: Int): View? {
        try {
            // AccessibilityInteractionController#findViewByAccessibilityId doesn't call this
            // method in Android Q and later. Ideally, we should only define this method in
            // Android P and earlier, but since we don't have a way to do so, we can simply
            // invoke the hidden parent method after Android P. If in new android, the hidden method
            // ViewGroup#findViewByAccessibilityIdTraversal signature is changed or removed, we can
            // simply return null here because there will be no call to this method.
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val findViewByAccessibilityIdTraversalMethod = View::class.java
                    .getDeclaredMethod("findViewByAccessibilityIdTraversal", Int::class.java)
                findViewByAccessibilityIdTraversalMethod.isAccessible = true
                findViewByAccessibilityIdTraversalMethod.invoke(this, accessibilityId) as? View
            } else {
                findViewByAccessibilityIdRootedAtCurrentView(accessibilityId, this)
            }
        } catch (e: NoSuchMethodException) {
            return null
        }
    }

    override val isLifecycleInResumedState: Boolean
        get() = viewTreeOwners?.lifecycleOwner
            ?.lifecycle?.currentState == Lifecycle.State.RESUMED

    private fun transformMatrixToWindow(view: View, matrix: Matrix) {
        val parentView = view.parent
        if (parentView is View) {
            transformMatrixToWindow(parentView, matrix)
            matrix.preTranslate(-view.scrollX.toFloat(), -view.scrollY.toFloat())
            matrix.preTranslate(view.left.toFloat(), view.top.toFloat())
        } else {
            view.getLocationInWindow(tmpPositionArray)
            matrix.preTranslate(-view.scrollX.toFloat(), -view.scrollY.toFloat())
            matrix.preTranslate(tmpPositionArray[0].toFloat(), tmpPositionArray[1].toFloat())
        }

        val viewMatrix = view.matrix
        if (!viewMatrix.isIdentity) {
            matrix.preConcat(viewMatrix)
        }
    }

    companion object {
        private var systemPropertiesClass: Class<*>? = null
        private var getBooleanMethod: Method? = null

        // TODO(mount): replace with ViewCompat.isShowingLayoutBounds() when it becomes available.
        @SuppressLint("PrivateApi", "BanUncheckedReflection")
        private fun getIsShowingLayoutBounds(): Boolean = try {
            if (systemPropertiesClass == null) {
                systemPropertiesClass = Class.forName("android.os.SystemProperties")
                getBooleanMethod = systemPropertiesClass?.getDeclaredMethod(
                    "getBoolean",
                    String::class.java,
                    Boolean::class.java
                )
            }
            getBooleanMethod?.invoke(null, "debug.layout", false) as? Boolean ?: false
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Combines objects populated via ViewTree*Owner
     */
    class ViewTreeOwners(
        /**
         * The [LifecycleOwner] associated with this owner.
         */
        val lifecycleOwner: LifecycleOwner,
        /**
         * The [SavedStateRegistryOwner] associated with this owner.
         */
        val savedStateRegistryOwner: SavedStateRegistryOwner
    )
}

/**
 * Return the layout direction set by the [Locale][java.util.Locale].
 *
 * A convenience getter that translates [Configuration.getLayoutDirection] result into
 * [LayoutDirection] instance.
 */
internal val Configuration.localeLayoutDirection: LayoutDirection
    // We don't use the attached View's layout direction here since that layout direction may not
    // be resolved since the composables may be composed without attaching to the RootViewImpl.
    // In Jetpack Compose, use the locale layout direction (i.e. layoutDirection came from
    // configuration) as a default layout direction.
    get() = layoutDirectionFromInt(layoutDirection)

private fun layoutDirectionFromInt(layoutDirection: Int): LayoutDirection = when (layoutDirection) {
    android.util.LayoutDirection.LTR -> LayoutDirection.Ltr
    android.util.LayoutDirection.RTL -> LayoutDirection.Rtl
    else -> LayoutDirection.Ltr
}

/** @suppress */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@InternalComposeUiApi // used by testing infra
var textInputServiceFactory: (PlatformTextInputService) -> TextInputService =
    { TextInputService(it) }

/**
 * This class is here to ensure that the classes that use this API will get verified and can be
 * AOT compiled. It is expected that this class will soft-fail verification, but the classes
 * which use this method will pass.
 */
@RequiresApi(Build.VERSION_CODES.O)
internal object AndroidComposeViewVerificationHelperMethods {
    @RequiresApi(Build.VERSION_CODES.O)
    @DoNotInline
    fun focusable(view: View, focusable: Int, defaultFocusHighlightEnabled: Boolean) {
        view.focusable = focusable
        // not to add the default focus highlight to the whole compose view
        view.defaultFocusHighlightEnabled = defaultFocusHighlightEnabled
    }
}
