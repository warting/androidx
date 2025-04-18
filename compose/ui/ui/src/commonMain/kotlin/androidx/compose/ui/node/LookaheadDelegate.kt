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

package androidx.compose.ui.node

import androidx.collection.MutableScatterMap
import androidx.collection.MutableScatterSet
import androidx.collection.mutableObjectIntMapOf
import androidx.collection.mutableScatterSetOf
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.internal.checkPrecondition
import androidx.compose.ui.internal.throwIllegalStateExceptionForNullCheck
import androidx.compose.ui.layout.AlignmentLine
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.LookaheadLayoutCoordinates
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.PlacementScope
import androidx.compose.ui.layout.Ruler
import androidx.compose.ui.layout.RulerScope
import androidx.compose.ui.layout.VerticalAlignmentLine
import androidx.compose.ui.layout.VerticalRuler
import androidx.compose.ui.layout.positionOnScreen
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.round

/**
 * This is the base class for NodeCoordinator and LookaheadDelegate. The common functionalities
 * between the two are extracted here.
 */
internal abstract class LookaheadCapablePlaceable :
    Placeable(), MeasureScopeWithLayoutNode, MotionReferencePlacementDelegate {
    abstract val position: IntOffset
    abstract val child: LookaheadCapablePlaceable?
    abstract val parent: LookaheadCapablePlaceable?
    abstract val hasMeasureResult: Boolean
    abstract override val layoutNode: LayoutNode
    abstract val coordinates: LayoutCoordinates
    private var _rulerScope: ResettableRulerScope? = null
    private var rulersLambda: (RulerScope.() -> Unit)? = null

    /**
     * A cached value for PlaceableResult, used when calculating Rulers. This is used to avoid
     * reallocating a PlaceableResult every time rulers are calculated.
     */
    private var cachedRulerPlaceableResult: PlaceableResult? = null

    /**
     * Indicates whether the [Placeable] was placed under a motion frame of reference.
     *
     * This means, that its offset may be excluded from calculation with
     * `includeMotionFrameOfReference = false` in [LookaheadLayoutCoordinates.localPositionOf].
     */
    override var isPlacedUnderMotionFrameOfReference: Boolean = false

    override fun updatePlacedUnderMotionFrameOfReference(newMFR: Boolean) {
        val parentNode = parent?.layoutNode
        if (parentNode == layoutNode) {
            isPlacedUnderMotionFrameOfReference = newMFR
        } else {
            // This node is the beginning of the chain (i.e. outerCoordinator), check if this
            // placement call comes from the parent
            if (
                parentNode?.layoutState == LayoutNode.LayoutState.LayingOut ||
                    parentNode?.layoutState == LayoutNode.LayoutState.LookaheadLayingOut
            ) {
                isPlacedUnderMotionFrameOfReference = newMFR
            }
            // If the node is simply being replaced without parent, we need to maintain the flag
            // from last time when `placeChildren` lambda was run. Therefore no op.
        }
    }

    private val rulerScope: ResettableRulerScope
        get() {
            return _rulerScope ?: ResettableRulerScope().also { _rulerScope = it }
        }

    final override fun get(alignmentLine: AlignmentLine): Int {
        if (!hasMeasureResult) return AlignmentLine.Unspecified
        val measuredPosition = calculateAlignmentLine(alignmentLine)
        if (measuredPosition == AlignmentLine.Unspecified) return AlignmentLine.Unspecified
        return measuredPosition +
            if (alignmentLine is VerticalAlignmentLine) {
                apparentToRealOffset.x
            } else {
                apparentToRealOffset.y
            }
    }

    abstract fun calculateAlignmentLine(alignmentLine: AlignmentLine): Int

    /**
     * True when the coordinator is running its own placing block to obtain the position in parent,
     * but is not interested in the position of children.
     */
    internal var isShallowPlacing: Boolean = false
    internal abstract val measureResult: MeasureResult

    internal abstract fun replace()

    abstract val alignmentLinesOwner: AlignmentLinesOwner

    /**
     * Used to indicate that this placement pass is for the purposes of calculating an alignment
     * line. If it is, then [LayoutNodeLayoutDelegate.coordinatesAccessedDuringPlacement] will be
     * changed when [Placeable.PlacementScope.coordinates] is accessed to indicate that the
     * placement is not finalized and must be run again.
     */
    internal var isPlacingForAlignment = false

    /** [PlacementScope] used to place children. */
    val placementScope = PlacementScope(this)

    protected fun NodeCoordinator.invalidateAlignmentLinesFromPositionChange() {
        if (wrapped?.layoutNode != layoutNode) {
            alignmentLinesOwner.alignmentLines.onAlignmentsChanged()
        } else {
            alignmentLinesOwner.parentAlignmentLinesOwner?.alignmentLines?.onAlignmentsChanged()
        }
    }

    override val isLookingAhead: Boolean
        get() = false

    private var rulerValues: RulerTrackingMap? = null

    private var rulerReaders:
        MutableScatterMap<Ruler, MutableScatterSet<WeakReference<LayoutNode>>>? =
        null

    fun findRulerValue(ruler: Ruler, defaultValue: Float): Float {
        if (isPlacingForAlignment) {
            return defaultValue
        }
        var p: LookaheadCapablePlaceable = this
        while (true) {
            val rulerValue = p.rulerValues?.getOrDefault(ruler, Float.NaN) ?: Float.NaN
            if (!rulerValue.isNaN()) {
                p.addRulerReader(layoutNode, ruler)
                return ruler.calculateCoordinate(rulerValue, p.coordinates, coordinates)
            }
            val parent = p.parent
            if (parent == null) {
                p.addRulerReader(layoutNode, ruler)
                return defaultValue
            }
            p = parent
        }
    }

    private fun addRulerReader(layoutNode: LayoutNode, ruler: Ruler) {
        rulerReaders?.forEachValue { set -> set.removeIf { it.get()?.isAttached != true } }
        rulerReaders?.removeIf { _, value -> value.isEmpty() }
        val readerMap =
            rulerReaders
                ?: MutableScatterMap<Ruler, MutableScatterSet<WeakReference<LayoutNode>>>().also {
                    rulerReaders = it
                }
        val readers = readerMap.getOrPut(ruler) { MutableScatterSet() }
        readers += WeakReference(layoutNode)
    }

    private fun findAncestorRulerDefiner(ruler: Ruler): LookaheadCapablePlaceable {
        var p: LookaheadCapablePlaceable = this
        while (true) {
            if (p.rulerValues?.contains(ruler) == true) {
                return p
            }
            val parent = p.parent ?: return p
            p = parent
        }
    }

    private fun LayoutNode.isLayoutNodeAncestor(ancestor: LayoutNode): Boolean {
        if (this === ancestor) {
            return true
        }
        return parent?.isLayoutNodeAncestor(ancestor) ?: false
    }

    internal fun invalidateChildrenOfDefiningRuler(ruler: Ruler) {
        val definer = findAncestorRulerDefiner(ruler)
        val readers = definer.rulerReaders?.remove(ruler)
        if (readers != null) {
            notifyRulerValueChange(readers)
        }
    }

    @Suppress("PrimitiveInCollection")
    override fun layout(
        width: Int,
        height: Int,
        alignmentLines: Map<AlignmentLine, Int>,
        rulers: (RulerScope.() -> Unit)?,
        placementBlock: PlacementScope.() -> Unit
    ): MeasureResult {
        checkMeasuredSize(width, height)
        return object : MeasureResult {
            override val width: Int
                get() = width

            override val height: Int
                get() = height

            override val alignmentLines: Map<AlignmentLine, Int>
                get() = alignmentLines

            override val rulers: (RulerScope.() -> Unit)?
                get() = rulers

            override fun placeChildren() {
                placementScope.placementBlock()
            }
        }
    }

    internal fun captureRulersIfNeeded(result: MeasureResult?) {
        val rulerReaders = rulerReaders
        if (result != null) {
            if (isPlacingForAlignment) {
                return
            }
            val rulerLambda = result.rulers
            if (rulerLambda == null) {
                // Notify anything that read a value it must have a relayout
                if (rulerReaders != null) {
                    rulerReaders.forEachValue { notifyRulerValueChange(it) }
                    rulerReaders.clear()
                }
            } else {
                // NOTE: consider using a mutable PlaceableResult to be reused for this purpose
                var recaptureRulers = (this.rulersLambda !== rulerLambda)
                var positionOnScreen = IntOffset.Max
                var size = IntSize.Zero
                if (!recaptureRulers && rulerScope.coordinatesAccessed) {
                    val coordinates = this.coordinates
                    positionOnScreen = coordinates.positionOnScreen().round()
                    size = coordinates.size
                    recaptureRulers =
                        positionOnScreen != rulerScope.positionOnScreen || size != rulerScope.size
                }
                if (recaptureRulers) {
                    val placeableResult =
                        cachedRulerPlaceableResult?.also { it.result = result }
                            ?: PlaceableResult(result, this).also {
                                cachedRulerPlaceableResult = it
                            }
                    captureRulers(placeableResult, positionOnScreen, size)
                    this.rulersLambda = result.rulers
                }
            }
        } else {
            rulerReaders?.forEachValue { notifyRulerValueChange(it) }
            rulerReaders?.clear()
            rulerValues?.clear()
        }
    }

    private fun captureRulers(
        placeableResult: PlaceableResult,
        positionOnScreen: IntOffset = IntOffset.Max,
        size: IntSize = IntSize.Zero
    ) {
        val rulerReaders = rulerReaders
        val newValues = rulerValues ?: RulerTrackingMap().also { rulerValues = it }
        // capture the new values
        layoutNode.owner?.snapshotObserver?.observeReads(placeableResult, onCommitAffectingRuler) {
            rulerScope.coordinatesAccessed = false
            rulerScope.positionOnScreen = positionOnScreen
            rulerScope.size = size
            placeableResult.result.rulers?.invoke(rulerScope)
        }
        // Notify changes
        newValues.notifyChanged(isLookingAhead, parent, rulerReaders)
    }

    private fun captureRulersIfNeeded(placeableResult: PlaceableResult) {
        if (isPlacingForAlignment) {
            return
        }
        val rulerLambda = placeableResult.result.rulers
        val rulerReaders = rulerReaders
        if (rulerLambda == null) {
            // Notify anything that read a value it must have a relayout
            if (rulerReaders != null) {
                rulerReaders.forEachValue { notifyRulerValueChange(it) }
                rulerReaders.clear()
            }
        } else {
            captureRulers(placeableResult)
            this.rulersLambda = rulerLambda
        }
    }

    private fun notifyRulerValueChange(layoutNodes: MutableScatterSet<WeakReference<LayoutNode>>) {
        layoutNodes.forEach { layoutNodeRef ->
            layoutNodeRef.get()?.let { layoutNode ->
                if (isLookingAhead) {
                    layoutNode.requestLookaheadRelayout(false)
                } else {
                    layoutNode.requestRelayout(false)
                }
            }
        }
    }

    fun provideRulerValue(ruler: Ruler, value: Float) {
        val rulerValues = rulerValues ?: RulerTrackingMap().also { rulerValues = it }
        rulerValues[ruler] = value
    }

    fun provideRelativeRulerValue(ruler: Ruler, value: Float) {
        val rulerValues = rulerValues ?: RulerTrackingMap().also { rulerValues = it }
        rulerValues[ruler] =
            if (layoutDirection == LayoutDirection.Ltr) {
                value
            } else {
                width - value
            }
    }

    private inner class ResettableRulerScope : RulerScope {
        var coordinatesAccessed = false
        var positionOnScreen = IntOffset.Max
        var size = IntSize.Zero

        override val coordinates: LayoutCoordinates
            get() {
                coordinatesAccessed = true
                val coords = this@LookaheadCapablePlaceable.coordinates
                if (positionOnScreen == IntOffset.Max) {
                    positionOnScreen = coords.positionOnScreen().round()
                    size = coords.size
                }
                this@LookaheadCapablePlaceable.layoutNode.layoutDelegate.onCoordinatesUsed()
                return coords
            }

        override fun Ruler.provides(value: Float) {
            this@LookaheadCapablePlaceable.provideRulerValue(this, value)
        }

        override fun VerticalRuler.providesRelative(value: Float) {
            this@LookaheadCapablePlaceable.provideRelativeRulerValue(this, value)
        }

        override val density: Float
            get() = this@LookaheadCapablePlaceable.density

        override val fontScale: Float
            get() = this@LookaheadCapablePlaceable.fontScale
    }

    companion object {
        private val onCommitAffectingRuler: (PlaceableResult) -> Unit = { result ->
            if (result.isValidOwnerScope) {
                result.placeable.captureRulersIfNeeded(result)
            }
        }
    }
}

private class PlaceableResult(var result: MeasureResult, val placeable: LookaheadCapablePlaceable) :
    OwnerScope {
    override val isValidOwnerScope: Boolean
        get() = placeable.coordinates.isAttached
}

// This is about 16 million pixels. That should be big enough. We'll treat anything bigger as an
// error.
private const val MaxLayoutDimension = (1 shl 24) - 1
private const val MaxLayoutMask: Int = 0xFF00_0000.toInt()

@Suppress("NOTHING_TO_INLINE")
internal inline fun checkMeasuredSize(width: Int, height: Int) {
    checkPrecondition(width and MaxLayoutMask == 0 && height and MaxLayoutMask == 0) {
        "Size($width x $height) is out of range. Each dimension must be between 0 and " +
            "$MaxLayoutDimension."
    }
}

internal abstract class LookaheadDelegate(
    val coordinator: NodeCoordinator,
) : Measurable, LookaheadCapablePlaceable() {
    override val child: LookaheadCapablePlaceable?
        get() = coordinator.wrapped?.lookaheadDelegate

    override val hasMeasureResult: Boolean
        get() = _measureResult != null

    override var position = IntOffset.Zero
    private var oldAlignmentLines: MutableMap<AlignmentLine, Int>? = null
    override val measureResult: MeasureResult
        get() =
            _measureResult
                ?: throwIllegalStateExceptionForNullCheck(
                    "LookaheadDelegate has not been measured yet when measureResult is requested."
                )

    override val isLookingAhead: Boolean
        get() = true

    override val layoutDirection: LayoutDirection
        get() = coordinator.layoutDirection

    override val density: Float
        get() = coordinator.density

    override val fontScale: Float
        get() = coordinator.fontScale

    override val parent: LookaheadCapablePlaceable?
        get() = coordinator.wrappedBy?.lookaheadDelegate

    override val layoutNode: LayoutNode
        get() = coordinator.layoutNode

    override val coordinates: LayoutCoordinates
        get() = lookaheadLayoutCoordinates

    internal val size: IntSize
        get() = IntSize(width, height)

    internal val constraints: Constraints
        get() = measurementConstraints

    val lookaheadLayoutCoordinates = LookaheadLayoutCoordinates(this)
    override val alignmentLinesOwner: AlignmentLinesOwner
        get() = coordinator.layoutNode.layoutDelegate.lookaheadAlignmentLinesOwner!!

    private var _measureResult: MeasureResult? = null
        set(result) {
            result?.let { measuredSize = IntSize(it.width, it.height) }
                ?: run { measuredSize = IntSize.Zero }
            if (field != result && result != null) {
                // We do not simply compare against old.alignmentLines in case this is a
                // MutableStateMap and the same instance might be passed.
                if (
                    (!oldAlignmentLines.isNullOrEmpty() || result.alignmentLines.isNotEmpty()) &&
                        result.alignmentLines != oldAlignmentLines
                ) {
                    alignmentLinesOwner.alignmentLines.onAlignmentsChanged()

                    @Suppress("PrimitiveInCollection")
                    val oldLines =
                        oldAlignmentLines
                            ?: (mutableMapOf<AlignmentLine, Int>().also { oldAlignmentLines = it })
                    oldLines.clear()
                    oldLines.putAll(result.alignmentLines)
                }
            }
            field = result
        }

    protected val cachedAlignmentLinesMap = mutableObjectIntMapOf<AlignmentLine>()

    internal fun getCachedAlignmentLine(alignmentLine: AlignmentLine): Int =
        cachedAlignmentLinesMap.getOrDefault(alignmentLine, AlignmentLine.Unspecified)

    override fun replace() {
        placeAt(position, 0f, null)
    }

    final override fun placeAt(
        position: IntOffset,
        zIndex: Float,
        layerBlock: (GraphicsLayerScope.() -> Unit)?
    ) {
        placeSelf(position)
        if (isShallowPlacing) return
        placeChildren()
    }

    private fun placeSelf(position: IntOffset) {
        if (this.position != position) {
            this.position = position
            layoutNode.layoutDelegate.lookaheadPassDelegate
                ?.notifyChildrenUsingLookaheadCoordinatesWhilePlacing()
            coordinator.invalidateAlignmentLinesFromPositionChange()
        }
        if (!isPlacingForAlignment) {
            captureRulersIfNeeded(measureResult)
        }
    }

    internal fun placeSelfApparentToRealOffset(position: IntOffset) {
        placeSelf(position + apparentToRealOffset)
    }

    protected open fun placeChildren() {
        measureResult.placeChildren()
    }

    inline fun performingMeasure(constraints: Constraints, block: () -> MeasureResult): Placeable {
        measurementConstraints = constraints
        _measureResult = block()
        return this
    }

    override val parentData: Any?
        get() = coordinator.parentData

    override fun minIntrinsicWidth(height: Int): Int {
        return coordinator.wrapped!!.lookaheadDelegate!!.minIntrinsicWidth(height)
    }

    override fun maxIntrinsicWidth(height: Int): Int {
        return coordinator.wrapped!!.lookaheadDelegate!!.maxIntrinsicWidth(height)
    }

    override fun minIntrinsicHeight(width: Int): Int {
        return coordinator.wrapped!!.lookaheadDelegate!!.minIntrinsicHeight(width)
    }

    override fun maxIntrinsicHeight(width: Int): Int {
        return coordinator.wrapped!!.lookaheadDelegate!!.maxIntrinsicHeight(width)
    }

    internal fun positionIn(
        ancestor: LookaheadDelegate,
        excludingAgnosticOffset: Boolean,
    ): IntOffset {
        var aggregatedOffset = IntOffset.Zero
        var lookaheadDelegate = this
        while (lookaheadDelegate != ancestor) {
            if (
                !lookaheadDelegate.isPlacedUnderMotionFrameOfReference || !excludingAgnosticOffset
            ) {
                aggregatedOffset += lookaheadDelegate.position
            }
            lookaheadDelegate = lookaheadDelegate.coordinator.wrappedBy!!.lookaheadDelegate!!
        }
        return aggregatedOffset
    }
}

/**
 * A class to track which ruler values have changed, which have been added, and which have been
 * removed. This has been somewhat optimized by using arrays as a backing store.
 */
private class RulerTrackingMap {
    private var size = 0
    private var rulers = arrayOfNulls<Ruler>(32)
    private var values = FloatArray(32)
    private var accessFlags = ByteArray(32)
    private var layoutNodes = mutableScatterSetOf<WeakReference<LayoutNode>>()
    private val newRulers = mutableScatterSetOf<Ruler>()

    fun getOrDefault(ruler: Ruler, defaultValue: Float): Float {
        val index = rulers.indexOf(ruler)
        return if (index < 0) {
            defaultValue
        } else {
            values[index]
        }
    }

    operator fun set(ruler: Ruler, value: Float) {
        val index = rulers.indexOf(ruler)
        if (index < 0) {
            val newIndex = size
            if (newIndex == rulers.size) {
                val newSize = newIndex * 2
                rulers = rulers.copyOf(newSize)
                values = values.copyOf(newSize)
                accessFlags = accessFlags.copyOf(newSize)
            }
            rulers[newIndex] = ruler
            accessFlags[newIndex] = AccessNewValue
            values[newIndex] = value
            size++
        } else {
            val oldValue = values[index]
            if (oldValue != value) {
                values[index] = value
                accessFlags[index] = AccessChanged
            } else {
                accessFlags[index] = AccessNoChange
            }
        }
    }

    operator fun contains(ruler: Ruler): Boolean = rulers.contains(ruler)

    /**
     * Notifies for any changed ruler values, modifying [rulerReaders] to remove any readers that
     * were notified.
     */
    fun notifyChanged(
        isLookingAhead: Boolean,
        parent: LookaheadCapablePlaceable?,
        rulerReaders: MutableScatterMap<Ruler, MutableScatterSet<WeakReference<LayoutNode>>>?
    ) {
        for (i in 0 until size) {
            val access = accessFlags[i]
            if (access == AccessNewValue) {
                newRulers += rulers[i]!!
            } else if (access != AccessNoChange && rulerReaders != null) {
                val readers = rulerReaders.remove(rulers[i]!!)
                if (readers != null) {
                    layoutNodes += readers
                }
            }
        }
        var removed = 0
        for (sourceIndex in 0 until size) {
            if (accessFlags[sourceIndex] == AccessNotSet) {
                // remove this value
                removed++
            } else if (removed > 0) {
                val destIndex = sourceIndex - removed
                rulers[destIndex] = rulers[sourceIndex]
            }
            accessFlags[sourceIndex] = AccessNotSet
        }
        for (i in size - removed until size) {
            rulers[i] = null
        }
        size -= removed

        parent?.let { newRulers.forEach { ruler -> it.invalidateChildrenOfDefiningRuler(ruler) } }
        newRulers.clear()

        layoutNodes.forEach { layoutNodeRef ->
            layoutNodeRef.get()?.let { layoutNode ->
                if (isLookingAhead) {
                    layoutNode.requestLookaheadRelayout(false)
                } else {
                    layoutNode.requestRelayout(false)
                }
            }
        }

        layoutNodes.clear()
    }

    fun clear() {
        for (i in 0 until size) {
            rulers[i] = null
            values[i] = Float.NaN
            accessFlags[i] = 0
        }
        size = 0
    }
}

/**
 * Constant used in [RulerTrackingMap.accessFlags] for a ruler whose value was set, but the value
 * itself hasn't changed.
 */
private const val AccessNoChange = 0.toByte()
/**
 * Constant used in [RulerTrackingMap.accessFlags] for a ruler whose value was set, and the value
 * itself has changed.
 */
private const val AccessChanged = 1.toByte()
/** Constant used in [RulerTrackingMap.accessFlags] for a ruler whose value has not been set. */
private const val AccessNotSet = 2.toByte()
/**
 * Constant used in [RulerTrackingMap.accessFlags] for a ruler whose value was newly set by the
 * ruler lambda.
 */
private const val AccessNewValue = 3.toByte()
