/*
 * Copyright 2023 The Android Open Source Project
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

@file:OptIn(ExperimentalWearFoundationApi::class)
@file:Suppress("DEPRECATION")

package androidx.wear.compose.foundation

import androidx.annotation.FloatRange
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollDispatcher
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.findRootCoordinates
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionOnScreen
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.core.util.Predicate
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Different values that determine the state of the [SwipeToReveal] composable, reflected in
 * [RevealState.currentValue]. [RevealValue.Covered] is considered the default state where none of
 * the actions are revealed yet.
 *
 * [SwipeToReveal] direction is not localised, with the default being [RevealDirection.RightToLeft],
 * and [RevealValue.RightRevealing] and [RevealValue.RightRevealed] correspond to the actions
 * getting revealed from the right side of the screen. In case swipe direction is set to
 * [RevealDirection.Both], actions can also get revealed from the left side of the screen, and in
 * that case [RevealValue.LeftRevealing] and [RevealValue.LeftRevealed] are used.
 *
 * @see [RevealDirection]
 */
@Deprecated(
    message =
        "The SwipeToReveal component from the latest material library should be used instead. This will be removed in a future release of this library.",
    replaceWith = ReplaceWith("RevealValue", "androidx.wear.compose.material3.RevealValue"),
)
@ExperimentalWearFoundationApi
@JvmInline
public value class RevealValue private constructor(public val value: Int) {
    public companion object {
        /**
         * The value which represents the state in which the whole revealable content is fully
         * revealed, and they are displayed on the left side of the screen. This also represents the
         * state in which one of the actions has been triggered/performed.
         *
         * This is only used when the swipe direction is set to [RevealDirection.Both], and the user
         * swipes from the left side of the screen.
         */
        public val LeftRevealed: RevealValue = RevealValue(-2)

        /**
         * The value which represents the state in which all the actions are revealed and the top
         * content is not being swiped. In this state, none of the actions have been triggered or
         * performed yet, and they are displayed on the left side of the screen.
         *
         * This is only used when the swipe direction is set to [RevealDirection.Both], and the user
         * swipes from the left side of the screen.
         */
        public val LeftRevealing: RevealValue = RevealValue(-1)

        /**
         * The default first value which generally represents the state where the revealable actions
         * have not been revealed yet. In this state, none of the actions have been triggered or
         * performed yet.
         */
        public val Covered: RevealValue = RevealValue(0)

        /**
         * The value which represents the state in which all the actions are revealed and the top
         * content is not being swiped. In this state, none of the actions have been triggered or
         * performed yet, and they are displayed on the right side of the screen.
         */
        public val RightRevealing: RevealValue = RevealValue(1)

        /**
         * The value which represents the state in which the whole revealable content is fully
         * revealed, and the actions are revealed on the right side of the screen. This also
         * represents the state in which one of the actions has been triggered/performed.
         */
        public val RightRevealed: RevealValue = RevealValue(2)
    }
}

/**
 * Different values [SwipeToReveal] composable can reveal the actions from.
 *
 * [RevealDirection] is not localised, with the default being [RevealDirection.RightToLeft] to
 * prevent conflict with the system-wide swipe to dismiss gesture in an activity, so it's strongly
 * advised to respect the default value to avoid conflicting gestures.
 */
@Deprecated(
    message =
        "The SwipeToReveal component from the latest material library should be used instead. This will be removed in a future release of this library.",
    replaceWith = ReplaceWith("RevealDirection", "androidx.wear.compose.material3.RevealDirection"),
)
@ExperimentalWearFoundationApi
@JvmInline
public value class RevealDirection private constructor(public val value: Int) {
    public companion object {
        /**
         * The default value which allows the user to swipe right to left to reveal or execute the
         * actions. It's strongly advised to respect the default behavior to avoid conflict with the
         * swipe-to-dismiss gesture.
         */
        public val RightToLeft: RevealDirection = RevealDirection(0)

        /**
         * The value which allows the user to swipe in either direction to reveal or execute the
         * actions. This should not be used if the component is used in an activity as the gesture
         * might conflict with the swipe-to-dismiss gesture and could be confusing for the users.
         * This is only supported for rare cases where the current screen does not support swipe to
         * dismiss.
         */
        public val Both: RevealDirection = RevealDirection(1)
    }
}

/**
 * Different values which can trigger the state change from one [RevealValue] to another. These are
 * not set by themselves and need to be set appropriately with [RevealState.snapTo] and
 * [RevealState.animateTo].
 */
@Deprecated(
    message =
        "The SwipeToReveal component from the latest material library should be used instead. This will be removed in a future release of this library."
)
@ExperimentalWearFoundationApi
@JvmInline
public value class RevealActionType private constructor(public val value: Int) {
    public companion object {
        /**
         * Represents the primary action composable of [SwipeToReveal]. This corresponds to the
         * mandatory `primaryAction` parameter of [SwipeToReveal].
         */
        public val PrimaryAction: RevealActionType = RevealActionType(0)

        /**
         * Represents the secondary action composable of [SwipeToReveal]. This corresponds to the
         * optional `secondaryAction` composable of [SwipeToReveal].
         */
        public val SecondaryAction: RevealActionType = RevealActionType(1)

        /**
         * Represents the undo action composable of [SwipeToReveal]. This corresponds to the
         * `undoAction` composable of [SwipeToReveal] which is shown once an action is performed.
         */
        public val UndoAction: RevealActionType = RevealActionType(2)

        /** Default value when none of the above are applicable. */
        public val None: RevealActionType = RevealActionType(-1)
    }
}

/**
 * Creates the required anchors to which the top content can be swiped, to reveal the actions. Each
 * value should be in the range [0..1], where 0 represents right most end and 1 represents the full
 * width of the top content starting from right and ending on left.
 *
 * @param coveredAnchor Anchor for the [RevealValue.Covered] value
 * @param revealingAnchor Anchor for the [RevealValue.LeftRevealing] or [RevealValue.RightRevealing]
 *   value
 * @param revealedAnchor Anchor for the [RevealValue.LeftRevealed] or [RevealValue.RightRevealed]
 *   value
 * @param revealDirection The direction in which the content can be swiped. It's strongly advised to
 *   keep the default [RevealDirection.RightToLeft] in order to preserve compatibility with the
 *   system wide swipe to dismiss gesture.
 */
@Deprecated(
    "The SwipeToReveal component from the latest material library should be used instead. This will be removed in a future release of this library."
)
@ExperimentalWearFoundationApi
@SuppressWarnings("PrimitiveInCollection")
public fun createRevealAnchors(
    coveredAnchor: Float = 0f,
    revealingAnchor: Float = SwipeToRevealDefaults.RevealingRatio,
    revealedAnchor: Float = 1f,
    revealDirection: RevealDirection = RevealDirection.RightToLeft,
): Map<RevealValue, Float> {
    if (revealDirection == RevealDirection.Both) {
        return mapOf(
            RevealValue.LeftRevealed to -revealedAnchor,
            RevealValue.LeftRevealing to -revealingAnchor,
            RevealValue.Covered to coveredAnchor,
            RevealValue.RightRevealing to revealingAnchor,
            RevealValue.RightRevealed to revealedAnchor,
        )
    }
    return mapOf(
        RevealValue.Covered to coveredAnchor,
        RevealValue.RightRevealing to revealingAnchor,
        RevealValue.RightRevealed to revealedAnchor,
    )
}

/**
 * A class to keep track of the state of the composable. It can be used to customise the behavior
 * and state of the composable.
 *
 * @constructor Create a [RevealState].
 */
@Deprecated(
    message =
        "The SwipeToReveal component from the latest material library should be used instead. This will be removed in a future release of this library.",
    replaceWith = ReplaceWith("RevealState", "androidx.wear.compose.material3.RevealState"),
)
@ExperimentalWearFoundationApi
public class RevealState
internal constructor(
    initialValue: RevealValue,
    animationSpec: AnimationSpec<Float>,
    confirmValueChange: (RevealValue) -> Boolean,
    positionalThreshold: (totalDistance: Float) -> Float,
    internal val anchors: Map<RevealValue, Float>,
    internal val coroutineScope: CoroutineScope,
    internal val nestedScrollDispatcher: NestedScrollDispatcher,
) {
    /** [SwipeableV2State] internal instance for the state. */
    internal val swipeableState =
        SwipeableV2State(
            initialValue = initialValue,
            animationSpec = animationSpec,
            confirmValueChange = { revealValue ->
                confirmValueChangeAndReset(confirmValueChange, revealValue)
            },
            positionalThreshold = { totalDistance -> positionalThreshold(totalDistance) },
            nestedScrollDispatcher = nestedScrollDispatcher,
        )

    public var lastActionType: RevealActionType by mutableStateOf(RevealActionType.None)

    /**
     * The current [RevealValue] based on the status of the component.
     *
     * @see Modifier.swipeableV2
     */
    public val currentValue: RevealValue
        get() = swipeableState.currentValue

    /**
     * The target [RevealValue] based on the status of the component. This will be equal to the
     * [currentValue] if there is no animation running or swiping has stopped. Otherwise, this
     * returns the next [RevealValue] based on the animation/swipe direction.
     *
     * @see Modifier.swipeableV2
     */
    public val targetValue: RevealValue
        get() = swipeableState.targetValue

    /**
     * Returns whether the animation is running or not.
     *
     * @see Modifier.swipeableV2
     */
    public val isAnimationRunning: Boolean
        get() = swipeableState.isAnimationRunning

    /**
     * The current amount by which the revealable content has been revealed by.
     *
     * @see Modifier.swipeableV2
     */
    public val offset: Float
        get() = swipeableState.offset ?: 0f

    /**
     * Defines the anchors for revealable content. These anchors are used to determine the width at
     * which the revealable content can be revealed to and stopped without requiring any input from
     * the user.
     *
     * @see Modifier.swipeableV2
     */
    public val swipeAnchors: Map<RevealValue, Float>
        get() = anchors

    /**
     * The threshold, in pixels, where the revealed actions are fully visible but the existing
     * content would be left in place if the reveal action was stopped. This threshold is used to
     * create the anchor for [RevealValue.RightRevealing]. If there is no such anchor defined for
     * [RevealValue.RightRevealing], it returns 0.0f.
     */
    /* @FloatRange(from = 0.0) */
    public val revealThreshold: Float
        get() = width.floatValue * (swipeAnchors[RevealValue.RightRevealing] ?: 0.0f)

    /**
     * The total width of the component in pixels. Initialise to zero, updated when the width
     * changes.
     */
    public val width: MutableFloatState = mutableFloatStateOf(0.0f)

    /**
     * Snaps to the [targetValue] without any animation.
     *
     * @param targetValue The target [RevealValue] where the [currentValue] will be changed to.
     * @see Modifier.swipeableV2
     */
    public suspend fun snapTo(targetValue: RevealValue) {
        // Cover the previously open component if revealing a different one
        if (targetValue != RevealValue.Covered) {
            resetLastState(this)
        }
        swipeableState.snapTo(targetValue)
    }

    /**
     * Animates to the [targetValue] with the animation spec provided.
     *
     * @param targetValue The target [RevealValue] where the [currentValue] will animate to.
     * @throws IllegalStateException if the target [RevealValue] is not valid for current
     *   [RevealState] instance.
     */
    public suspend fun animateTo(targetValue: RevealValue) {
        checkNotNull(anchors[targetValue]) {
            "The RevealValue you're targeting isn't supported by current RevealState instance. " +
                "Ensure the RevealState was created with an anchor map that contains " +
                "the target RevealValue."
        }
        // Cover the previously open component if revealing a different one
        if (targetValue != RevealValue.Covered) {
            resetLastState(this)
        }
        try {
            swipeableState.animateTo(targetValue)
        } finally {
            if (targetValue == RevealValue.Covered) {
                lastActionType = RevealActionType.None
            }
        }
    }

    /**
     * Require the current offset.
     *
     * @throws IllegalStateException If the offset has not been initialized yet
     */
    internal fun requireOffset(): Float = swipeableState.requireOffset()

    private fun confirmValueChangeAndReset(
        confirmValueChange: Predicate<RevealValue>,
        revealValue: RevealValue,
    ): Boolean {
        val canChangeValue = confirmValueChange.test(revealValue)
        val currentState = this
        // Update the state if the reveal value is changing to a different value than Covered.
        if (canChangeValue && revealValue != RevealValue.Covered) {
            coroutineScope.launch { resetLastState(currentState) }
        }
        return canChangeValue
    }

    /**
     * Resets last state if a different SwipeToReveal is being moved to new anchor and the last
     * state is in [RevealValue.RightRevealing] mode which represents no action has been performed
     * yet. In [RevealValue.RightRevealed], the action has been performed and it will not be reset.
     */
    private suspend fun resetLastState(currentState: RevealState) {
        val oldState = SingleSwipeCoordinator.lastUpdatedState.getAndSet(currentState)
        if (currentState != oldState && oldState?.currentValue == RevealValue.RightRevealing) {
            oldState.animateTo(RevealValue.Covered)
        }
    }

    /** A singleton instance to keep track of the [RevealState] which was modified the last time. */
    private object SingleSwipeCoordinator {
        var lastUpdatedState: AtomicReference<RevealState?> = AtomicReference(null)
    }
}

/**
 * Create and [remember] a [RevealState].
 *
 * @param initialValue The initial value of the [RevealValue].
 * @param animationSpec The animation which will be applied on the top content.
 * @param confirmValueChange Optional callback invoked to confirm or veto a pending state change.
 * @param positionalThreshold The positional threshold to be used when calculating the target state
 *   while the reveal is in progress and when settling after the revealing ends. This is the
 *   distance from the start of a transition. It will be, depending on the direction of the
 *   interaction, added or subtracted from/to the origin offset. It should always be a positive
 *   value.
 * @param anchors A map of [RevealValue] to the fraction where the content can be revealed to reach
 *   that value. Each anchor should be between [0..1] which will be adjusted based on total width.
 */
@Deprecated(
    message =
        "The SwipeToReveal component from the latest material library should be used instead. This will be removed in a future release of this library.",
    replaceWith =
        ReplaceWith("rememberRevealState", "androidx.wear.compose.material3.rememberRevealState"),
)
@ExperimentalWearFoundationApi
@Composable
public fun rememberRevealState(
    initialValue: RevealValue = RevealValue.Covered,
    animationSpec: AnimationSpec<Float> = SwipeToRevealDefaults.AnimationSpec,
    confirmValueChange: (RevealValue) -> Boolean = { true },
    positionalThreshold: (totalDistance: Float) -> Float =
        SwipeToRevealDefaults.PositionalThreshold,
    anchors: Map<RevealValue, Float> = createRevealAnchors(),
): RevealState {
    val coroutineScope = rememberCoroutineScope()
    val nestedScrollDispatcher = remember { NestedScrollDispatcher() }
    return remember(initialValue, animationSpec) {
        RevealState(
            initialValue = initialValue,
            animationSpec = animationSpec,
            confirmValueChange = confirmValueChange,
            positionalThreshold = positionalThreshold,
            anchors = anchors,
            coroutineScope = coroutineScope,
            nestedScrollDispatcher = nestedScrollDispatcher,
        )
    }
}

/**
 * A composable that can be used to add extra actions to a composable (up to two) which will be
 * revealed when the original composable is swiped to the left. This composable requires a primary
 * swipe/click action, a secondary optional click action can also be provided.
 *
 * When the composable reaches the state where all the actions are revealed and the swipe continues
 * beyond the positional threshold defined in [RevealState], the primary action is automatically
 * triggered.
 *
 * An optional undo action can also be added. This undo action will be visible to users once the
 * [RevealValue] becomes [RevealValue.RightRevealed].
 *
 * It is strongly recommended to have icons represent the actions and maybe a text and icon for the
 * undo action.
 *
 * Example of SwipeToReveal with primary action and undo action
 *
 * @sample androidx.wear.compose.foundation.samples.SwipeToRevealSample
 *
 * Example of SwipeToReveal using [RevealState] to delay the appearance of primary action text
 *
 * @sample androidx.wear.compose.foundation.samples.SwipeToRevealWithDelayedText
 *
 * Example of SwipeToReveal used with Expandables
 *
 * @sample androidx.wear.compose.foundation.samples.SwipeToRevealWithExpandables
 * @param primaryAction The primary action that will be triggered in the event of a completed swipe.
 *   We also strongly recommend to trigger the action when it is clicked.
 * @param modifier Optional [Modifier] for this component.
 * @param onFullSwipe An optional lambda which will be triggered when a full swipe from either of
 *   the anchors is performed.
 * @param state The [RevealState] of this component. It can be used to customise the anchors and
 *   threshold config of the swipeable modifier which is applied.
 * @param secondaryAction An optional action that can be added to the component. We strongly
 *   recommend triggering the action when it is clicked.
 * @param undoAction The optional undo action that will be applied to the component once the the
 *   [RevealState.currentValue] becomes [RevealValue.RightRevealed].
 * @param gestureInclusion Provides fine-grained control so that touch gestures can be excluded when
 *   they start in a certain region. An instance of [GestureInclusion] can be passed in here which
 *   will determine via [GestureInclusion.ignoreGestureStart] whether the gesture should proceed or
 *   not. By default, [gestureInclusion] allows gestures everywhere except a zone on the left edge,
 *   which is used for swipe-to-dismiss (see [SwipeToRevealDefaults.gestureInclusion]).
 * @param content The content that will be initially displayed over the other actions provided.
 *   Custom accessibility actions should always be added to the content using [Modifier.semantics] -
 *   examples are shown in the code samples.
 */
@Deprecated(
    message =
        "The SwipeToReveal component from the latest material library should be used instead. This will be removed in a future release of this library.",
    replaceWith = ReplaceWith("SwipeToReveal", "androidx.wear.compose.material3.SwipeToReveal"),
)
@ExperimentalWearFoundationApi
@Composable
public fun SwipeToReveal(
    primaryAction: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    onFullSwipe: () -> Unit = {},
    state: RevealState = rememberRevealState(),
    secondaryAction: (@Composable () -> Unit)? = null,
    undoAction: (@Composable () -> Unit)? = null,
    gestureInclusion: GestureInclusion = SwipeToRevealDefaults.gestureInclusion(state = state),
    content: @Composable () -> Unit,
) {
    // A no-op NestedScrollConnection which does not consume scroll/fling events
    val noOpNestedScrollConnection = remember { object : NestedScrollConnection {} }

    var globalPosition by remember { mutableStateOf<LayoutCoordinates?>(null) }

    var allowSwipe by remember { mutableStateOf(true) }

    CustomTouchSlopProvider(
        newTouchSlop = LocalViewConfiguration.current.touchSlop * CustomTouchSlopMultiplier
    ) {
        Box(
            modifier =
                modifier
                    .onGloballyPositioned { layoutCoordinates ->
                        globalPosition = layoutCoordinates
                    }
                    .pointerInput(globalPosition) {
                        awaitEachGesture {
                            allowSwipe = true
                            val firstDown = awaitFirstDown(false, PointerEventPass.Initial)
                            globalPosition?.let {
                                allowSwipe =
                                    !gestureInclusion.ignoreGestureStart(firstDown.position, it)
                            }
                        }
                    }
                    .swipeableV2(
                        state = state.swipeableState,
                        orientation = Orientation.Horizontal,
                        enabled =
                            allowSwipe &&
                                state.currentValue != RevealValue.LeftRevealed &&
                                state.currentValue != RevealValue.RightRevealed,
                    )
                    .swipeAnchors(
                        state = state.swipeableState,
                        possibleValues = state.swipeAnchors.keys,
                    ) { value, layoutSize ->
                        val swipeableWidth = layoutSize.width.toFloat()
                        // Update the total width which will be used to calculate the anchors
                        state.width.floatValue = swipeableWidth
                        // Multiply the anchor with -1f to get the actual swipeable anchor
                        -state.swipeAnchors[value]!! * swipeableWidth
                    }
                    // NestedScrollDispatcher sends the scroll/fling events from the node to its
                    // parent
                    // and onwards including the modifier chain. Apply it in the end to let nested
                    // scroll
                    // connection applied before this modifier consume the scroll/fling events.
                    .nestedScroll(noOpNestedScrollConnection, state.nestedScrollDispatcher)
        ) {
            val swipeCompleted =
                state.currentValue == RevealValue.RightRevealed ||
                    state.currentValue == RevealValue.LeftRevealed
            val lastActionIsSecondary = state.lastActionType == RevealActionType.SecondaryAction
            val isWithinRevealOffset by remember {
                derivedStateOf { abs(state.offset) <= state.revealThreshold }
            }
            val canSwipeRight =
                (state.swipeAnchors.minOfOrNull { (_, offset) -> offset } ?: 0f) < 0f

            // Determines whether the secondary action will be visible based on the current
            // reveal offset
            val showSecondaryAction = isWithinRevealOffset || lastActionIsSecondary

            // Determines whether both primary and secondary action should be hidden, usually the
            // case
            // when secondary action is clicked
            val hideActions = !isWithinRevealOffset && lastActionIsSecondary

            val swipingRight by remember { derivedStateOf { state.offset > 0 } }

            // Don't draw actions on the left side if the user cannot swipe right, and they are
            // currently swiping right
            val shouldDrawActions by remember {
                derivedStateOf { abs(state.offset) > 0 && (canSwipeRight || !swipingRight) }
            }

            // Draw the buttons only when offset is greater than zero.
            if (shouldDrawActions) {
                Box(
                    modifier = Modifier.matchParentSize(),
                    contentAlignment =
                        if (swipingRight) AbsoluteAlignment.CenterLeft
                        else AbsoluteAlignment.CenterRight,
                ) {
                    AnimatedContent(
                        targetState = swipeCompleted && undoAction != null,
                        transitionSpec = {
                            if (targetState) { // Fade in the Undo composable and fade out actions
                                fadeInUndo()
                            } else { // Fade in the actions and fade out the undo composable
                                fadeOutUndo()
                            }
                        },
                        label = "AnimatedContentS2R",
                    ) { displayUndo ->
                        if (displayUndo && undoAction != null) {
                            val undoActionAlpha =
                                animateFloatAsState(
                                    targetValue = if (swipeCompleted) 1f else 0f,
                                    animationSpec =
                                        tween(
                                            durationMillis = RAPID_ANIMATION,
                                            delayMillis = FLASH_ANIMATION,
                                            easing = STANDARD_IN_OUT,
                                        ),
                                    label = "UndoActionAlpha",
                                )
                            Row(
                                modifier =
                                    Modifier.graphicsLayer { alpha = undoActionAlpha.value }
                                        .fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                            ) {
                                ActionSlot(content = undoAction)
                            }
                        } else {
                            // Animate weight for secondary action slot.
                            val secondaryActionWeight =
                                animateFloatAsState(
                                    targetValue = if (showSecondaryAction) 1f else 0f,
                                    animationSpec = tween(durationMillis = QUICK_ANIMATION),
                                    label = "SecondaryActionAnimationSpec",
                                )
                            val secondaryActionAlpha =
                                animateFloatAsState(
                                    targetValue =
                                        if (!showSecondaryAction || hideActions) 0f else 1f,
                                    animationSpec =
                                        tween(
                                            durationMillis = QUICK_ANIMATION,
                                            easing = LinearEasing,
                                        ),
                                    label = "SecondaryActionAlpha",
                                )
                            val primaryActionAlpha =
                                animateFloatAsState(
                                    targetValue = if (hideActions) 0f else 1f,
                                    animationSpec =
                                        tween(durationMillis = 100, easing = LinearEasing),
                                    label = "PrimaryActionAlpha",
                                )
                            val revealedContentAlpha =
                                animateFloatAsState(
                                    targetValue = if (swipeCompleted) 0f else 1f,
                                    animationSpec =
                                        tween(
                                            durationMillis = FLASH_ANIMATION,
                                            easing = LinearEasing,
                                        ),
                                    label = "RevealedContentAlpha",
                                )
                            var revealedContentHeight by remember { mutableIntStateOf(0) }
                            Row(
                                modifier =
                                    Modifier.graphicsLayer { alpha = revealedContentAlpha.value }
                                        .onSizeChanged { revealedContentHeight = it.height }
                                        .layout { measurable, constraints ->
                                            val placeable =
                                                measurable.measure(
                                                    constraints.copy(
                                                        maxWidth =
                                                            if (hideActions) {
                                                                    state.revealThreshold
                                                                } else {
                                                                    abs(state.offset)
                                                                }
                                                                .roundToInt()
                                                    )
                                                )
                                            layout(placeable.width, placeable.height) {
                                                placeable.placeRelative(
                                                    0,
                                                    calculateVerticalOffsetBasedOnScreenPosition(
                                                        revealedContentHeight,
                                                        globalPosition,
                                                    ),
                                                )
                                            }
                                        },
                                horizontalArrangement = Arrangement.Absolute.Right,
                            ) {
                                if (!swipingRight) {
                                    // weight cannot be 0 so remove the composable when weight
                                    // becomes 0
                                    if (
                                        secondaryAction != null && secondaryActionWeight.value > 0
                                    ) {
                                        Spacer(Modifier.size(SwipeToRevealDefaults.Padding))
                                        ActionSlot(
                                            weight = secondaryActionWeight.value,
                                            opacity = secondaryActionAlpha,
                                            content = secondaryAction,
                                        )
                                    }
                                    Spacer(Modifier.size(SwipeToRevealDefaults.Padding))
                                    ActionSlot(
                                        content = primaryAction,
                                        opacity = primaryActionAlpha,
                                    )
                                } else {
                                    ActionSlot(
                                        content = primaryAction,
                                        opacity = primaryActionAlpha,
                                    )
                                    Spacer(Modifier.size(SwipeToRevealDefaults.Padding))
                                    // weight cannot be 0 so remove the composable when weight
                                    // becomes 0
                                    if (
                                        secondaryAction != null && secondaryActionWeight.value > 0
                                    ) {
                                        ActionSlot(
                                            weight = secondaryActionWeight.value,
                                            opacity = secondaryActionAlpha,
                                            content = secondaryAction,
                                        )
                                        Spacer(Modifier.size(SwipeToRevealDefaults.Padding))
                                    }
                                }
                            }
                        }
                    }
                }
            }
            Row(
                modifier =
                    Modifier.absoluteOffset {
                        val xOffset = state.requireOffset().roundToInt()
                        IntOffset(
                            x = if (canSwipeRight) xOffset else xOffset.coerceAtMost(0),
                            y = 0,
                        )
                    }
            ) {
                content()
            }
            LaunchedEffect(state.currentValue) {
                if (
                    (state.currentValue == RevealValue.LeftRevealed ||
                        state.currentValue == RevealValue.RightRevealed) &&
                        state.lastActionType == RevealActionType.None
                ) {
                    onFullSwipe()
                }
            }
        }
    }
}

/** An internal object containing some defaults used across the Swipe to reveal component. */
@Deprecated(
    message =
        "The SwipeToReveal component from the latest material library should be used instead. This will be removed in a future release of this library.",
    replaceWith =
        ReplaceWith(
            "SwipeToRevealDefaults",
            "androidx.wear.compose.material3.SwipeToRevealDefaults",
        ),
)
@ExperimentalWearFoundationApi
public object SwipeToRevealDefaults {
    /** Default animation spec used when moving between states. */
    internal val AnimationSpec: AnimationSpec<Float> =
        tween(durationMillis = RAPID_ANIMATION, easing = FastOutSlowInEasing)

    /** Default padding space between action slots. */
    internal val Padding = 4.dp

    /**
     * Default ratio of the content displayed when in [RevealValue.RightRevealing] state, i.e. all
     * the actions are revealed and the top content is not being swiped. For example, a value of 0.7
     * means that 70% of the width is used to place the actions.
     */
    public val RevealingRatio: Float = 0.7f

    /**
     * Default position threshold that needs to be swiped in order to transition to the next state.
     * Used in conjunction with [RevealingRatio]; for example, a threshold of 0.5 with a revealing
     * ratio of 0.7 means that the user needs to swipe at least 35% (0.5 * 0.7) of the component
     * width to go from [RevealValue.Covered] to [RevealValue.RightRevealing] and at least 85%
     * (0.7 + 0.5 * (1 - 0.7)) of the component width to go from [RevealValue.RightRevealing] to
     * [RevealValue.RightRevealed].
     */
    public val PositionalThreshold: (totalDistance: Float) -> Float = { totalDistance: Float ->
        totalDistance * 0.5f
    }

    /**
     * The default value used to configure the size of the left edge zone in a [SwipeToReveal]. The
     * left edge zone in this case refers to the leftmost edge of the screen, in this region it is
     * common to disable scrolling in order for swipe-to-dismiss handlers to take over.
     */
    public val LeftEdgeZoneFraction: Float = 0.15f

    /**
     * The default behaviour for when [SwipeToReveal] should handle gestures. In this implementation
     * of [GestureInclusion], swipe events that originate in the left edge of the screen (as
     * determined by [LeftEdgeZoneFraction]) will be ignored, if the [RevealState] is
     * [RevealValue.Covered]. This allows swipe-to-dismiss handlers (if present) to handle the
     * gesture in this region.
     *
     * @param state [RevealState] of the [SwipeToReveal].
     * @param edgeZoneFraction The fraction of the screen width from the left edge where gestures
     *   should be ignored. Defaults to [LeftEdgeZoneFraction].
     */
    public fun gestureInclusion(
        state: RevealState,
        @FloatRange(from = 0.0, to = 1.0) edgeZoneFraction: Float = LeftEdgeZoneFraction,
    ): GestureInclusion = DefaultGestureInclusion(state, edgeZoneFraction)

    /**
     * A behaviour for [SwipeToReveal] to handle all gestures, intended for rare cases where
     * bidirectional anchors are used and no swipe events are ignored
     */
    public val bidirectionalGestureInclusion: GestureInclusion
        get() = BidirectionalGestureInclusion
}

@Stable
private class DefaultGestureInclusion(
    private val revealState: RevealState,
    private val edgeZoneFraction: Float,
) : GestureInclusion {
    override fun ignoreGestureStart(offset: Offset, layoutCoordinates: LayoutCoordinates): Boolean {
        val screenOffset = layoutCoordinates.localToScreen(offset)
        val screenWidth = layoutCoordinates.findRootCoordinates().size.width
        return revealState.currentValue == RevealValue.Covered &&
            screenOffset.x <= screenWidth * edgeZoneFraction
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as DefaultGestureInclusion

        if (edgeZoneFraction != other.edgeZoneFraction) return false
        if (revealState != other.revealState) return false

        return true
    }

    override fun hashCode(): Int {
        var result = edgeZoneFraction.hashCode()
        result = 31 * result + revealState.hashCode()
        return result
    }
}

@Stable
private object BidirectionalGestureInclusion : GestureInclusion {
    override fun ignoreGestureStart(offset: Offset, layoutCoordinates: LayoutCoordinates): Boolean =
        false

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        return true
    }

    override fun hashCode(): Int {
        return javaClass.hashCode()
    }
}

@Composable
private fun RowScope.ActionSlot(
    modifier: Modifier = Modifier,
    weight: Float = 1f,
    opacity: State<Float> = mutableFloatStateOf(1f),
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier.weight(weight).graphicsLayer { alpha = opacity.value },
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

private fun fadeInUndo(): ContentTransform =
    ContentTransform(
        // animation spec for the fading in undo action (fadeIn + scaleIn)
        targetContentEnter =
            fadeIn(
                animationSpec =
                    tween(
                        durationMillis = RAPID_ANIMATION,
                        delayMillis = FLASH_ANIMATION,
                        easing = LinearEasing,
                    )
            ) +
                scaleIn(
                    initialScale = 1.2f,
                    animationSpec =
                        tween(
                            durationMillis = RAPID_ANIMATION,
                            delayMillis = FLASH_ANIMATION,
                            easing = STANDARD_IN_OUT,
                        ),
                ),
        // animation spec for the fading out content and actions (fadeOut)
        initialContentExit =
            fadeOut(animationSpec = tween(durationMillis = FLASH_ANIMATION, easing = LinearEasing)),
    )

private fun fadeOutUndo(): ContentTransform =
    ContentTransform(
        // No animation, fade-in in 0 milliseconds since enter transition is mandatory
        targetContentEnter =
            fadeIn(animationSpec = tween(durationMillis = 0, delayMillis = SHORT_ANIMATION)),

        // animation spec for the fading out undo action (fadeOut + scaleOut)
        initialContentExit =
            fadeOut(animationSpec = tween(durationMillis = SHORT_ANIMATION, easing = LinearEasing)),
    )

private fun calculateVerticalOffsetBasedOnScreenPosition(
    childHeight: Int,
    globalPosition: LayoutCoordinates?,
): Int {
    if (globalPosition == null || !globalPosition.positionOnScreen().isSpecified) {
        return 0
    }
    val positionOnScreen = globalPosition.positionOnScreen()
    val boundsInWindow = globalPosition.boundsInWindow()
    val parentTop = positionOnScreen.y.toInt()
    val parentHeight = globalPosition.size.height
    val parentBottom = parentTop + parentHeight
    if (parentTop >= boundsInWindow.top && parentBottom <= boundsInWindow.bottom) {
        // Don't offset if the item is fully on screen
        return 0
    }

    // Avoid going outside parent bounds
    val minCenter = parentTop + childHeight / 2
    val maxCenter = parentTop + parentHeight - childHeight / 2
    val desiredCenter = boundsInWindow.center.y.toInt().coerceIn(minCenter, maxCenter)
    val actualCenter = parentTop + parentHeight / 2
    return desiredCenter - actualCenter
}

internal const val CustomTouchSlopMultiplier = 1.20f

/** Short animation in milliseconds. */
private const val SHORT_ANIMATION = 50

/** Flash animation length in milliseconds. */
private const val FLASH_ANIMATION = 100

/** Rapid animation length in milliseconds. */
private const val RAPID_ANIMATION = 200

/** Quick animation length in milliseconds. */
private const val QUICK_ANIMATION = 250

/** Standard easing for Swipe To Reveal. */
private val STANDARD_IN_OUT = CubicBezierEasing(0.20f, 0.0f, 0.0f, 1.00f)
