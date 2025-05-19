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

package androidx.wear.compose.foundation.lazy.layout

import androidx.collection.ObjectIntMap
import androidx.collection.mutableScatterMapOf
import androidx.collection.mutableScatterSetOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.GraphicsContext
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastForEach
import kotlinx.coroutines.CoroutineScope

/**
 * Handles the item animations when it is set via "animateItem" modifiers.
 *
 * This class is responsible for:
 * - animating item appearance for the new items.
 * - detecting when item position changed, figuring our start/end offsets and starting the
 *   animations for placement animations.
 * - animating item disappearance for the removed items.
 */
internal class LazyLayoutItemAnimator<T : LazyLayoutMeasuredItem> {
    // state containing relevant info for active items.
    private val keyToItemInfoMap = mutableScatterMapOf<Any, ItemInfo>()

    // snapshot of the key to index map used for the last measuring.
    private var keyIndexMap: LazyLayoutKeyIndexMap? = null

    // keeps the index of the first visible item index.
    private var firstVisibleIndex = 0

    // stored to not allocate it every pass.
    private val movingAwayKeys = mutableScatterSetOf<Any>()
    private val movingInFromStartBound = mutableListOf<T>()
    private val movingInFromEndBound = mutableListOf<T>()
    private val disappearingItems = mutableListOf<LazyLayoutItemAnimation>()
    private var displayingNode: DrawModifierNode? = null

    /**
     * Should be called after the measuring so we can detect position changes and start animations.
     *
     * Note that this method can compose new item and add it into the [positionedItems] list.
     */
    fun onMeasured(
        positionedItems: List<T>,
        firstPassOffsetsPerItemKey: ObjectIntMap<Any>,
        keyIndexMap: LazyLayoutKeyIndexMap,
        isLookingAhead: Boolean,
        hasLookaheadOccurred: Boolean,
        layoutMinOffset: Int,
        layoutMaxOffset: Int,
        coroutineScope: CoroutineScope,
        graphicsContext: GraphicsContext,
    ) {
        val previousKeyToIndexMap = this.keyIndexMap
        this.keyIndexMap = keyIndexMap

        val hasAnimations = positionedItems.fastAny { it.hasAnimations() }
        if (!hasAnimations && keyToItemInfoMap.isEmpty()) {
            // no animations specified - no work needed - clear animation info
            releaseAnimations()
            return
        }

        val previousFirstVisibleIndex = firstVisibleIndex
        firstVisibleIndex = positionedItems.firstOrNull()?.index ?: 0

        // Only setup animations when we have access to target value in the current pass, which
        // means lookahead pass, or regular pass when not in a lookahead scope.
        val shouldSetupAnimation = isLookingAhead || !hasLookaheadOccurred

        // first add all items we had in the previous run
        keyToItemInfoMap.forEachKey { movingAwayKeys.add(it) }

        // iterate through the items which are visible (without animated offsets)
        positionedItems.fastForEach { item ->
            // remove items we have in the current one as they are still visible.
            movingAwayKeys.remove(item.key)
            if (item.hasAnimations()) {
                val itemInfo = keyToItemInfoMap[item.key]

                // We check how much the item moved between before and after applying
                // the scroll, since we don't want to animate that movement.
                val scrollOffset =
                    item.mainAxisOffset -
                        firstPassOffsetsPerItemKey.getOrDefault(item.key, item.mainAxisOffset)

                val previousIndex = previousKeyToIndexMap?.getIndex(item.key) ?: -1
                val shouldAnimateAppearance = previousIndex == -1 && previousKeyToIndexMap != null

                // there is no state associated with this item yet
                if (itemInfo == null) {
                    val newItemInfo = ItemInfo()
                    newItemInfo.updateAnimation(
                        item,
                        coroutineScope,
                        graphicsContext,
                        layoutMinOffset,
                        layoutMaxOffset,
                    )
                    keyToItemInfoMap[item.key] = newItemInfo

                    if (item.index != previousIndex && previousIndex != -1) {
                        if (previousIndex < previousFirstVisibleIndex) {
                            // the larger index will be in the start of the list
                            movingInFromStartBound.add(item)
                        } else {
                            movingInFromEndBound.add(item)
                        }
                    } else {
                        initializeAnimation(item, item.mainAxisOffset, newItemInfo)
                        applyScrollWithoutAnimation(newItemInfo, scrollOffset)
                        if (shouldAnimateAppearance) {
                            newItemInfo.animation?.animateAppearance()
                        }
                    }
                } else {
                    if (shouldSetupAnimation) {
                        itemInfo.updateAnimation(
                            item,
                            coroutineScope,
                            graphicsContext,
                            layoutMinOffset,
                            layoutMaxOffset,
                        )
                        applyScrollWithoutAnimation(itemInfo, scrollOffset)
                        if (shouldAnimateAppearance) {

                            itemInfo.animation?.let {
                                if (it.isDisappearanceAnimationInProgress) {
                                    disappearingItems.remove(it)
                                    displayingNode?.invalidateDraw()
                                }
                                it.animateAppearance()
                            }
                        }
                        startPlacementAnimationsIfNeeded(item)
                    }
                }
            } else {
                removeInfoForKey(item.key)
            }
        }

        if (shouldSetupAnimation && previousKeyToIndexMap != null) {
            if (movingInFromStartBound.isNotEmpty()) {
                var accumulatedOffset = 0
                movingInFromStartBound.sortByDescending { previousKeyToIndexMap.getIndex(it.key) }
                val startOffset =
                    positionedItems
                        .firstOrNull {
                            // Find the item right below the first moving in item in previous order
                            // as the anchor item.
                            previousKeyToIndexMap.getIndex(it.key) ==
                                previousKeyToIndexMap.getIndex(movingInFromStartBound[0].key) + 1
                        }
                        ?.let { getAnimation(it.key)?.finalOffset?.y }
                        // If the anchor item is removed, fallback to the layoutMinOffset.
                        ?: layoutMinOffset
                movingInFromStartBound.fastForEach { item ->
                    accumulatedOffset += item.mainAxisSizeWithSpacings
                    val mainAxisOffset = startOffset - accumulatedOffset
                    initializeAnimation(item, mainAxisOffset)
                    startPlacementAnimationsIfNeeded(item)
                }
            }
            if (movingInFromEndBound.isNotEmpty()) {
                var accumulatedOffset = 0
                movingInFromEndBound.sortBy { previousKeyToIndexMap.getIndex(it.key) }
                val startOffset =
                    positionedItems
                        .firstOrNull {
                            // Find the item right above the first moving in item in previous order
                            // as the anchor item.
                            previousKeyToIndexMap.getIndex(it.key) ==
                                previousKeyToIndexMap.getIndex(movingInFromEndBound[0].key) - 1
                        }
                        ?.let {
                            getAnimation(it.key)?.finalOffset?.run {
                                it.mainAxisSizeWithSpacings + y
                            }
                        }
                        // If the anchor item is removed, fallback to the layoutMaxOffset.
                        ?: layoutMaxOffset

                movingInFromEndBound.fastForEach { item ->
                    accumulatedOffset += item.mainAxisSizeWithSpacings
                    val mainAxisOffset =
                        startOffset + accumulatedOffset - item.mainAxisSizeWithSpacings
                    initializeAnimation(item, mainAxisOffset)
                    startPlacementAnimationsIfNeeded(item)
                }
            }
        }

        movingAwayKeys.forEach { key ->
            // found an item which was in our map previously but is not a part of the
            // positionedItems now
            val info = keyToItemInfoMap[key] ?: return@forEach

            // TODO: b/380044722 - handle cases in which the item is actually moving away (vs just
            //  being removed)
            if (keyIndexMap.getIndex(key) != -1) return@forEach

            var isProgress = false
            info.animation?.let {
                if (it.isDisappearanceAnimationInProgress) {
                    isProgress = true
                } else if (it.isDisappearanceAnimationFinished) {
                    it.release()
                    disappearingItems.remove(it)
                    displayingNode?.invalidateDraw()
                } else {
                    if (it.layer != null) {
                        it.animateDisappearance()
                    }
                    if (it.isDisappearanceAnimationInProgress) {
                        disappearingItems.add(it)
                        displayingNode?.invalidateDraw()
                        isProgress = true
                    } else {
                        it.release()
                    }
                }
            }

            if (!isProgress) {
                removeInfoForKey(key)
            }
        }

        movingInFromStartBound.clear()
        movingInFromEndBound.clear()
        movingAwayKeys.clear()
    }

    private fun applyScrollWithoutAnimation(
        itemInfo: LazyLayoutItemAnimator<T>.ItemInfo,
        scrollYOffset: Int,
    ) =
        itemInfo.animation?.let {
            if (it.rawOffset != LazyLayoutItemAnimation.NotInitialized) {
                it.rawOffset += IntOffset(0, scrollYOffset)
            }
        }

    private fun removeInfoForKey(key: Any) {
        keyToItemInfoMap.remove(key)?.animation?.release()
    }

    /**
     * Should be called when the animations are not needed for the next positions change, for
     * example when we snap to a new position.
     */
    fun reset() {
        releaseAnimations()
        keyIndexMap = null
        firstVisibleIndex = -1
    }

    fun releaseAnimations() {
        if (keyToItemInfoMap.isNotEmpty()) {
            keyToItemInfoMap.forEachValue { it.animation?.release() }
            keyToItemInfoMap.clear()
        }
    }

    private fun initializeAnimation(
        item: T,
        mainAxisOffset: Int,
        itemInfo: ItemInfo = keyToItemInfoMap[item.key]!!,
    ) {
        val firstPlaceableOffset = item.getOffset()

        val targetFirstPlaceableOffset = firstPlaceableOffset.copy(y = mainAxisOffset)

        // initialize offsets
        itemInfo.animation?.let { animation ->
            val diffToFirstPlaceableOffset = item.getOffset() - firstPlaceableOffset
            animation.rawOffset = targetFirstPlaceableOffset + diffToFirstPlaceableOffset
        }
    }

    private fun startPlacementAnimationsIfNeeded(item: T, isMovingAway: Boolean = false) {
        val itemInfo = keyToItemInfoMap[item.key]!!
        itemInfo.animation?.let { animation ->
            val newTarget = item.getOffset()
            val currentTarget = animation.rawOffset
            if (
                currentTarget != LazyLayoutItemAnimation.NotInitialized &&
                    currentTarget != newTarget
            ) {
                animation.animatePlacementDelta(newTarget - currentTarget, isMovingAway)
            }
            animation.rawOffset = newTarget
        }
    }

    fun getAnimation(key: Any): LazyLayoutItemAnimation? = keyToItemInfoMap[key]?.animation

    val modifier: Modifier = DisplayingDisappearingItemsElement(this)

    private inner class ItemInfo {
        var animation: LazyLayoutItemAnimation? = null
            private set

        var constraints: Constraints? = null
        var crossAxisOffset: Int = 0

        private val isRunningPlacement
            get() = animation?.isRunningMovingAwayAnimation == true

        var layoutMinOffset = 0
            private set

        var layoutMaxOffset = 0
            private set

        fun updateAnimation(
            positionedItem: T,
            coroutineScope: CoroutineScope,
            graphicsContext: GraphicsContext,
            layoutMinOffset: Int,
            layoutMaxOffset: Int,
            crossAxisOffset: Int = positionedItem.crossAxisOffset,
        ) {
            if (!isRunningPlacement) {
                this.layoutMinOffset = layoutMinOffset
                this.layoutMaxOffset = layoutMaxOffset
            }
            constraints = positionedItem.constraints
            this.crossAxisOffset = crossAxisOffset
            val specs = positionedItem.parentData.specs
            if (specs == null) {
                animation?.release()
                animation = null
            } else {
                val animation =
                    animation
                        ?: LazyLayoutItemAnimation(
                                coroutineScope = coroutineScope,
                                graphicsContext = graphicsContext,
                                // until b/329417380 is fixed we have to trigger any
                                // invalidation in
                                // order for the layer properties change to be applied:
                                onLayerPropertyChanged = { displayingNode?.invalidateDraw() },
                                containerHeight = layoutMaxOffset - layoutMinOffset,
                                transformedHeight = positionedItem.transformedHeight,
                                measuredHeight = positionedItem.measuredHeight,
                                measurementDirection = positionedItem.measurementDirection,
                            )
                            .also { animation = it }
                animation.fadeInSpec = specs.fadeInSpec
                animation.placementSpec = specs.placementSpec
                animation.fadeOutSpec = specs.fadeOutSpec
            }
        }
    }

    private data class DisplayingDisappearingItemsElement(
        private val animator: LazyLayoutItemAnimator<*>
    ) : ModifierNodeElement<DisplayingDisappearingItemsNode>() {
        override fun create() = DisplayingDisappearingItemsNode(animator)

        override fun update(node: DisplayingDisappearingItemsNode) {
            node.setAnimator(animator)
        }

        override fun InspectorInfo.inspectableProperties() {
            name = "DisplayingDisappearingItemsElement"
        }
    }

    private data class DisplayingDisappearingItemsNode(
        private var animator: LazyLayoutItemAnimator<*>
    ) : Modifier.Node(), DrawModifierNode {
        override fun ContentDrawScope.draw() {
            animator.disappearingItems.fastForEach {
                val layer = it.layer ?: return@fastForEach
                val x = it.finalOffset.x.toFloat()
                val y = it.finalOffset.y.toFloat()
                translate(x - layer.topLeft.x.toFloat(), y - layer.topLeft.y.toFloat()) {
                    drawLayer(layer)
                }
            }
            drawContent()
        }

        override fun onAttach() {
            animator.displayingNode = this
        }

        override fun onDetach() {
            animator.reset()
        }

        fun setAnimator(animator: LazyLayoutItemAnimator<*>) {
            if (this.animator != animator) {
                if (node.isAttached) {
                    this.animator.reset()
                    animator.displayingNode = this
                    this.animator = animator
                }
            }
        }
    }
}
