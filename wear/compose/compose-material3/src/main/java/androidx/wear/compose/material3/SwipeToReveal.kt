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

package androidx.wear.compose.material3

import android.annotation.SuppressLint
import androidx.annotation.FloatRange
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.GestureInclusion
import androidx.wear.compose.material3.ButtonDefaults.buttonColors
import androidx.wear.compose.material3.RevealDirection.Companion.Bidirectional
import androidx.wear.compose.material3.RevealDirection.Companion.RightToLeft
import androidx.wear.compose.material3.RevealValue.Companion.Covered
import androidx.wear.compose.material3.RevealValue.Companion.LeftRevealed
import androidx.wear.compose.material3.RevealValue.Companion.LeftRevealing
import androidx.wear.compose.material3.RevealValue.Companion.RightRevealed
import androidx.wear.compose.material3.RevealValue.Companion.RightRevealing
import androidx.wear.compose.material3.SwipeToRevealDefaults.DoubleActionAnchorWidth
import androidx.wear.compose.material3.SwipeToRevealDefaults.LeftEdgeZoneFraction
import androidx.wear.compose.material3.SwipeToRevealDefaults.SingleActionAnchorWidth
import androidx.wear.compose.material3.SwipeToRevealDefaults.bidirectionalGestureInclusion
import androidx.wear.compose.material3.SwipeToRevealDefaults.gestureInclusion
import androidx.wear.compose.material3.tokens.SwipeToRevealTokens
import androidx.wear.compose.materialcore.CustomTouchSlopProvider
import androidx.wear.compose.materialcore.SwipeableV2State
import androidx.wear.compose.materialcore.screenWidthDp
import androidx.wear.compose.materialcore.swipeAnchors
import androidx.wear.compose.materialcore.swipeableV2
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

/**
 * [SwipeToReveal] Material composable. This adds the option to configure up to two additional
 * actions on a Composable: a mandatory [primaryAction] and an optional [secondaryAction]. These
 * actions are initially hidden (unless [RevealState] is created with an initial value other than
 * [RevealValue.Covered]) and revealed only when the [content] is swiped. These additional actions
 * can be triggered by clicking on them after they are revealed. A full swipe of the [content] will
 * trigger the [onFullSwipe] callback, which is expected to match the [primaryAction]'s onClick
 * callback. The actions revealed on swipe are the same on both sides if [revealDirection] is set to
 * [RevealDirection.Bidirectional].
 *
 * For actions like "Delete", consider adding [undoPrimaryAction], which is displayed when the
 * [primaryAction] is performed via click or swipe. Adding undo composables allow users to undo the
 * action that they just performed.
 *
 * [SwipeToReveal] composable adds the [CustomAccessibilityAction]s using the labels from primary
 * and secondary actions.
 *
 * Example of [SwipeToReveal] with primary and secondary actions
 *
 * @sample androidx.wear.compose.material3.samples.SwipeToRevealSample
 *
 * Example of [SwipeToReveal] with a Card composable, it reveals a taller button.
 *
 * @sample androidx.wear.compose.material3.samples.SwipeToRevealSingleActionCardSample
 *
 * Example of [SwipeToReveal] that doesn't reveal the actions, instead it only executes them when
 * fully swiped or bounces back to its initial state.
 *
 * @sample androidx.wear.compose.material3.samples.SwipeToRevealNonAnchoredSample
 *
 * Example of [SwipeToReveal] with a [TransformingLazyColumn]
 *
 * @sample androidx.wear.compose.material3.samples.SwipeToRevealWithTransformingLazyColumnSample
 * @param primaryAction The primary action of this component.
 *   [SwipeToRevealScope.PrimaryActionButton] should be used to create a button for this slot.
 * @param onFullSwipe A callback which will be triggered when a full swipe is performed. It is
 *   expected that the same callback is given to [SwipeToRevealScope.PrimaryActionButton]s onClick
 *   action.
 * @param modifier [Modifier] to be applied on the composable.
 * @param secondaryAction Optional secondary action of this component.
 *   [SwipeToRevealScope.SecondaryActionButton] should be used to create a button for this slot.
 * @param undoPrimaryAction Optional undo action for the primary action of this component.
 *   [SwipeToRevealScope.UndoActionButton] should be used to create a button for this slot.
 * @param undoSecondaryAction Optional undo action for the secondary action of this component -
 *   ignored if the secondary action has not been specified. [SwipeToRevealScope.UndoActionButton]
 *   should be used to create a button for this slot.
 * @param revealState [RevealState] of the [SwipeToReveal].
 * @param revealDirection The direction from which [SwipeToReveal] can reveal the actions. It is
 *   strongly recommended to respect the default value of [RightToLeft] to avoid conflicting with
 *   the system-side swipe-to-dismiss gesture.
 * @param actionButtonHeight Desired height of the revealed action buttons. In case the content is a
 *   Button composable, it's suggested to use [SwipeToRevealDefaults.SmallActionButtonHeight], and
 *   for a Card composable, it's suggested to use [SwipeToRevealDefaults.LargeActionButtonHeight].
 * @param hasPartiallyRevealedState Determines whether the intermediate states [RightRevealing] and
 *   [LeftRevealing] are used. These indicate a settled state, where the primary action is partially
 *   revealed. By default, partially revealed state is allowed for single actions - set to false to
 *   make actions complete when swiped instead. This flag has no effect if a secondary action is
 *   provided (when there are two actions, the component always allows the partially revealed
 *   states).
 * @param gestureInclusion Provides fine-grained control so that touch gestures can be excluded when
 *   they start in a certain region. An instance of [GestureInclusion] can be passed in here which
 *   will determine via [GestureInclusion.ignoreGestureStart] whether the gesture should proceed or
 *   not. By default, [gestureInclusion] allows gestures everywhere for when [revealState] contains
 *   anchors for both directions (see [bidirectionalGestureInclusion]). If it doesn't, then it
 *   allows gestures everywhere, except a zone on the left edge, which is used for swipe-to-dismiss
 *   (see [gestureInclusion]).
 * @param content The content that will be initially displayed over the other actions provided.
 * @see [androidx.wear.compose.foundation.SwipeToReveal]
 */
@Composable
public fun SwipeToReveal(
    primaryAction: @Composable SwipeToRevealScope.() -> Unit,
    onFullSwipe: () -> Unit,
    modifier: Modifier = Modifier,
    secondaryAction: (@Composable SwipeToRevealScope.() -> Unit)? = null,
    undoPrimaryAction: (@Composable SwipeToRevealScope.() -> Unit)? = null,
    undoSecondaryAction: (@Composable SwipeToRevealScope.() -> Unit)? = null,
    revealState: RevealState = rememberRevealState(),
    revealDirection: RevealDirection = RevealDirection.RightToLeft,
    actionButtonHeight: Dp = SwipeToRevealDefaults.SmallActionButtonHeight,
    hasPartiallyRevealedState: Boolean = true,
    gestureInclusion: GestureInclusion =
        if (revealDirection == Bidirectional) {
            bidirectionalGestureInclusion
        } else {
            gestureInclusion(revealState)
        },
    content: @Composable () -> Unit,
) {
    val hapticFeedback = LocalHapticFeedback.current

    @SuppressLint("PrimitiveInCollection")
    val anchors: Set<RevealValue> =
        if (revealDirection == Bidirectional) {
            BidirectionalAnchors
        } else {
            UnidirectionAnchors
        }

    val swipeToRevealScope =
        remember(
            revealState,
            secondaryAction,
            undoPrimaryAction,
            undoSecondaryAction,
            actionButtonHeight
        ) {
            SwipeToRevealScope(
                revealState = revealState,
                hasSecondaryAction = secondaryAction != null,
                hasPrimaryUndo = undoPrimaryAction != null,
                hasSecondaryUndo = undoSecondaryAction != null,
                actionButtonHeight = actionButtonHeight,
            )
        }

    SwipeToRevealImpl(
        primaryAction = { primaryAction(swipeToRevealScope) },
        onFullSwipe = onFullSwipe,
        anchors = anchors,
        modifier = modifier.fillMaxWidth(),
        state = revealState,
        revealDirection = revealDirection,
        secondaryAction = secondaryAction?.let { { secondaryAction(swipeToRevealScope) } },
        undoAction =
            when (revealState.lastActionType) {
                RevealActionType.SecondaryAction ->
                    undoSecondaryAction?.let { { undoSecondaryAction(swipeToRevealScope) } }
                else -> undoPrimaryAction?.let { { undoPrimaryAction(swipeToRevealScope) } }
            },
        hasPartiallyRevealedState = hasPartiallyRevealedState,
        gestureInclusion = gestureInclusion,
        content = content,
    )

    LaunchedEffect(revealState.targetValue) {
        if ((revealState.targetValue == LeftRevealed || revealState.targetValue == RightRevealed)) {
            hapticFeedback.performHapticFeedback(HapticFeedbackType.GestureThresholdActivate)
        }
    }
}

/**
 * Scope for the actions of a [SwipeToReveal] composable. Used to define the primary, secondary,
 * undo primary and undo secondary actions.
 */
public class SwipeToRevealScope
internal constructor(
    internal val revealState: RevealState,
    internal val hasSecondaryAction: Boolean,
    internal val hasPrimaryUndo: Boolean,
    internal val hasSecondaryUndo: Boolean,
    internal val actionButtonHeight: Dp
) {
    /**
     * Provides a button for the primary action of a [SwipeToReveal].
     *
     * When first revealed the primary action displays an icon and then, if fully swiped, it
     * additionally shows text.
     *
     * @param onClick Callback to be executed when the action is performed via a button click.
     * @param icon Icon composable to be displayed for this action.
     * @param text Text composable to be displayed when the user fully swipes to execute the primary
     *   action.
     * @param modifier [Modifier] to be applied on the composable.
     * @param containerColor Container color for this action. This can be [Color.Unspecified], and
     *   in case it is, a default color will be used.
     * @param contentColor Content color for this action. This can be [Color.Unspecified], and in
     *   case it is, a default color will be used.
     */
    @Composable
    public fun PrimaryActionButton(
        onClick: () -> Unit,
        icon: @Composable () -> Unit,
        text: @Composable () -> Unit,
        modifier: Modifier = Modifier,
        containerColor: Color = Color.Unspecified,
        contentColor: Color = Color.Unspecified
    ) {
        ActionButton(
            revealState = revealState,
            action = SwipeToRevealAction(onClick, icon, text, containerColor, contentColor),
            revealActionType = RevealActionType.PrimaryAction,
            buttonHeight = actionButtonHeight,
            iconStartFadeInFraction = startFadeInFraction(hasSecondaryAction),
            iconEndFadeInFraction = endFadeInFraction(hasSecondaryAction),
            modifier = modifier,
            hasUndo = hasPrimaryUndo,
        )
    }

    /**
     * Provides a button for the optional secondary action of a [SwipeToReveal].
     *
     * Secondary action only displays an icon, because, unlike the primary action, it is never
     * extended to full width so does not have room to display text.
     *
     * @param onClick Callback to be executed when the action is performed via a button click.
     * @param icon Icon composable to be displayed for this action.
     * @param modifier [Modifier] to be applied on the composable.
     * @param containerColor Container color for this action.This can be [Color.Unspecified], and in
     *   case it is, a default color will be used.
     * @param contentColor Content color for this action. This can be [Color.Unspecified], and in
     *   case it is, a default color will be used.
     */
    @Composable
    public fun SecondaryActionButton(
        onClick: () -> Unit,
        icon: @Composable () -> Unit,
        modifier: Modifier = Modifier,
        containerColor: Color = Color.Unspecified,
        contentColor: Color = Color.Unspecified
    ) {
        ActionButton(
            revealState,
            SwipeToRevealAction(onClick, icon, null, containerColor, contentColor),
            RevealActionType.SecondaryAction,
            actionButtonHeight,
            iconStartFadeInFraction = startFadeInFraction(hasSecondaryAction),
            iconEndFadeInFraction = endFadeInFraction(hasSecondaryAction),
            modifier = modifier,
            hasSecondaryUndo
        )
    }

    /**
     * Provides a button for the undo action of a [SwipeToReveal]. Displayed after the user performs
     * either a primary or a secondary action.
     *
     * @param onClick Callback to be executed when the action is performed via a button click.
     * @param text Text composable to indicate what the undo action is, to be displayed when the
     *   user executes the primary action. This should include appropriated semantics for
     *   accessibility.
     * @param modifier [Modifier] to be applied on the composable.
     * @param icon Optional Icon composable to be displayed for this action.
     * @param containerColor Container color for this action. This can be [Color.Unspecified], and
     *   in case it is, a default color will be used.
     * @param contentColor Content color for this action. This can be [Color.Unspecified], and in
     *   case it is, a default color will be used.
     */
    @Composable
    public fun UndoActionButton(
        onClick: () -> Unit,
        text: @Composable () -> Unit,
        modifier: Modifier = Modifier,
        icon: @Composable (() -> Unit)? = null,
        containerColor: Color = Color.Unspecified,
        contentColor: Color = Color.Unspecified
    ) {
        ActionButton(
            revealState,
            SwipeToRevealAction(onClick, icon, text, containerColor, contentColor),
            RevealActionType.UndoAction,
            actionButtonHeight,
            iconStartFadeInFraction = startFadeInFraction(hasSecondaryAction),
            iconEndFadeInFraction = endFadeInFraction(hasSecondaryAction),
            modifier = modifier,
        )
    }
}

/** Defaults for Material 3 [SwipeToReveal]. */
public object SwipeToRevealDefaults {

    /** Standard height for a small revealed action, such as when the swiped item is a Button. */
    public val SmallActionButtonHeight: Dp = 52.dp

    /** Standard height for a large revealed action, such as when the swiped item is a Card. */
    public val LargeActionButtonHeight: Dp = 84.dp

    /**
     * The default value used to configure the size of the left edge zone in a [SwipeToReveal]. The
     * left edge zone in this case refers to the leftmost edge of the screen, in this region it is
     * common to disable scrolling in order for swipe-to-dismiss handlers to take over.
     */
    public val LeftEdgeZoneFraction: Float = 0.15f

    /**
     * The default behaviour for when [SwipeToReveal] should handle gestures. In this implementation
     * of [GestureInclusion], swipe events that originate in the left edge of the screen (as
     * determined by [LeftEdgeZoneFraction]) will be ignored, if the [RevealState] is [Covered].
     * This allows swipe-to-dismiss handlers (if present) to handle the gesture in this region.
     *
     * @param state [RevealState] of the [SwipeToReveal].
     * @param edgeZoneFraction The fraction of the screen width from the left edge where gestures
     *   should be ignored. Defaults to [LeftEdgeZoneFraction].
     */
    public fun gestureInclusion(
        state: RevealState,
        @FloatRange(from = 0.0, to = 1.0) edgeZoneFraction: Float = LeftEdgeZoneFraction
    ): GestureInclusion = DefaultGestureInclusion(state, edgeZoneFraction)

    /**
     * A behaviour for [SwipeToReveal] to handle all gestures, intended for rare cases where
     * bidirectional anchors are used and no swipe events are ignored
     */
    public val bidirectionalGestureInclusion: GestureInclusion
        get() = BidirectionalGestureInclusion

    /** Width that's required to display both actions in a [SwipeToReveal] composable. */
    internal val DoubleActionAnchorWidth: Dp = 130.dp

    /** Width that's required to display a single action in a [SwipeToReveal] composable. */
    internal val SingleActionAnchorWidth: Dp = 64.dp

    internal val IconSize = 26.dp

    /** Default animation spec used when moving between states. */
    internal val AnimationSpec: AnimationSpec<Float> =
        tween(durationMillis = RAPID_ANIMATION, easing = FastOutSlowInEasing)

    /** Default padding space between action slots. */
    internal val Padding = 4.dp

    /**
     * Default position threshold that needs to be swiped in order to transition to the next state.
     * For example, a threshold of 0.5 with a revealing ratio of 0.7 means that the user needs to
     * swipe at least 35% (0.5 * 0.7) of the component width to go from [Covered] to
     * [RightRevealing] and at least 85% (0.7 + 0.5 * (1 - 0.7)) of the component width to go from
     * [RightRevealing] to [RightRevealed].
     */
    internal val PositionalThreshold: (totalDistance: Float) -> Float = { totalDistance: Float ->
        totalDistance * 0.5f
    }
}

@Composable
internal fun ActionButton(
    revealState: RevealState,
    action: SwipeToRevealAction,
    revealActionType: RevealActionType,
    buttonHeight: Dp,
    iconStartFadeInFraction: Float,
    iconEndFadeInFraction: Float,
    modifier: Modifier = Modifier,
    hasUndo: Boolean = false,
) {
    val containerColor =
        action.containerColor.takeOrElse {
            when (revealActionType) {
                RevealActionType.PrimaryAction ->
                    MaterialTheme.colorScheme.fromToken(
                        SwipeToRevealTokens.PrimaryActionContainerColor
                    )
                RevealActionType.SecondaryAction ->
                    MaterialTheme.colorScheme.fromToken(
                        SwipeToRevealTokens.SecondaryActionContainerColor
                    )
                RevealActionType.UndoAction ->
                    MaterialTheme.colorScheme.fromToken(
                        SwipeToRevealTokens.UndoActionContainerColor
                    )
                else -> Color.Unspecified
            }
        }
    val contentColor =
        action.contentColor.takeOrElse {
            when (revealActionType) {
                RevealActionType.PrimaryAction ->
                    MaterialTheme.colorScheme.fromToken(
                        SwipeToRevealTokens.PrimaryActionContentColor
                    )
                RevealActionType.SecondaryAction ->
                    MaterialTheme.colorScheme.fromToken(
                        SwipeToRevealTokens.SecondaryActionContentColor
                    )
                RevealActionType.UndoAction ->
                    MaterialTheme.colorScheme.fromToken(SwipeToRevealTokens.UndoActionContentColor)
                else -> Color.Unspecified
            }
        }
    val fullScreenPaddingDp = (screenWidthDp() * FULL_SCREEN_PADDING_FRACTION).dp
    val startPadding =
        when (revealActionType) {
            RevealActionType.UndoAction -> fullScreenPaddingDp
            else -> 0.dp
        }
    val endPadding =
        when (revealActionType) {
            RevealActionType.UndoAction -> fullScreenPaddingDp
            else -> 0.dp
        }
    val screenWidthPx = with(LocalDensity.current) { screenWidthDp().dp.toPx() }
    val fadeInStart = screenWidthPx * BUTTON_VISIBLE_THRESHOLD_AS_SCREEN_WIDTH_PERCENTAGE
    val fadeInEnd = screenWidthPx * BUTTON_FADE_IN_END_THRESHOLD_AS_SCREEN_WIDTH_PERCENTAGE
    val coroutineScope = rememberCoroutineScope()
    Button(
        modifier =
            modifier
                .height(buttonHeight)
                .padding(startPadding, 0.dp, endPadding, 0.dp)
                .fillMaxWidth()
                .graphicsLayer {
                    val offset = abs(revealState.offset)
                    val shouldDisplayButton =
                        offset > screenWidthPx * BUTTON_VISIBLE_THRESHOLD_AS_SCREEN_WIDTH_PERCENTAGE
                    alpha =
                        if (shouldDisplayButton) {
                            val coercedOffset =
                                offset.coerceIn(
                                    minimumValue = fadeInStart,
                                    maximumValue = fadeInEnd
                                )
                            (coercedOffset - fadeInStart) / (fadeInEnd - fadeInStart)
                        } else {
                            0f
                        }
                },
        onClick = {
            coroutineScope.launch {
                try {
                    if (revealActionType == RevealActionType.UndoAction) {
                        revealState.animateTo(Covered)
                    } else {
                        if (hasUndo || revealActionType == RevealActionType.PrimaryAction) {
                            revealState.lastActionType = revealActionType
                            revealState.animateTo(
                                if (revealState.offset > 0) {
                                    LeftRevealed
                                } else {
                                    RightRevealed
                                }
                            )
                        }
                    }
                } finally {
                    // Execute onClick even if the animation gets interrupted
                    action.onClick()
                }
            }
        },
        colors = buttonColors(containerColor = containerColor, contentColor = contentColor),
        contentPadding = PaddingValues(ACTION_BUTTON_CONTENT_PADDING),
        shape = CircleShape
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().fillMaxHeight(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val primaryActionTextRevealed =
                remember(revealState) {
                    derivedStateOf {
                        abs(revealState.offset) > revealState.revealThreshold &&
                            (revealState.targetValue == RightRevealed ||
                                revealState.targetValue == LeftRevealed)
                    }
                }
            action.icon?.let {
                ActionIconWrapper(revealState, iconStartFadeInFraction, iconEndFadeInFraction, it)
            }
            when (revealActionType) {
                RevealActionType.PrimaryAction -> {
                    AnimatedVisibility(
                        visible = primaryActionTextRevealed.value,
                        enter =
                            fadeIn(spring(stiffness = Spring.StiffnessLow)) +
                                expandHorizontally(spring(stiffness = Spring.StiffnessMedium)),
                        exit =
                            fadeOut(spring(stiffness = Spring.StiffnessHigh)) +
                                shrinkHorizontally(spring(stiffness = Spring.StiffnessMedium)),
                    ) {
                        ActionText(action, contentColor)
                    }
                }
                RevealActionType.UndoAction -> ActionText(action, contentColor)
            }
        }
    }
}

@Composable
private fun ActionText(action: SwipeToRevealAction, contentColor: Color) {
    require(action.text != null) { "A text composable should be provided to ActionText." }
    Row(modifier = Modifier.padding(start = action.icon?.let { ICON_AND_TEXT_PADDING } ?: 0.dp)) {
        CompositionLocalProvider(
            LocalContentColor provides contentColor,
            LocalTextStyle provides LocalTextStyle.current,
            LocalTextConfiguration provides
                TextConfiguration(
                    textAlign = LocalTextConfiguration.current.textAlign,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1,
                )
        ) {
            action.text.invoke()
        }
    }
}

@Composable
private fun ActionIconWrapper(
    revealState: RevealState,
    iconStartFadeInFraction: Float,
    iconEndFadeInFraction: Float,
    content: @Composable () -> Unit
) {
    val screenWidthPx = with(LocalDensity.current) { screenWidthDp().dp.toPx() }
    val fadeInStart = screenWidthPx * iconStartFadeInFraction
    val fadeInEnd = screenWidthPx * iconEndFadeInFraction
    Box(
        modifier =
            Modifier.size(SwipeToRevealDefaults.IconSize, Dp.Unspecified).graphicsLayer {
                val offset = abs(revealState.offset)
                val shouldDisplayIcon = offset > fadeInStart
                alpha =
                    if (shouldDisplayIcon) {
                        val coercedOffset =
                            offset.coerceIn(minimumValue = fadeInStart, maximumValue = fadeInEnd)
                        (coercedOffset - fadeInStart) / (fadeInEnd - fadeInStart)
                    } else {
                        0f
                    }
            }
    ) {
        content()
    }
}

/** Data class to define an action to be displayed in a [SwipeToReveal] composable. */
internal data class SwipeToRevealAction(
    /** Callback to be executed when the action is performed via a full swipe, or a button click. */
    val onClick: () -> Unit,

    /**
     * Icon composable to be displayed for this action. This accepts a scale parameter that should
     * be used to increase icon icon when an action is fully revealed.
     */
    val icon: @Composable (() -> Unit)?,

    /**
     * Text composable to be displayed when the user fully swipes to execute the primary action, or
     * when the undo action is shown.
     */
    val text: @Composable (() -> Unit)?,

    /**
     * Color of the container, used for the background of the action button. This can be
     * [Color.Unspecified], and in case it is, needs to be replaced with a default.
     */
    val containerColor: Color,

    /**
     * Color of the content, used for the icon and text. This can be [Color.Unspecified], and in
     * case it is, needs to be replaced with a default.
     */
    val contentColor: Color,
)

/**
 * Different values that determine the state of the [SwipeToReveal] composable, reflected in
 * [RevealState.currentValue]. [Covered] is considered the default state where none of the actions
 * are revealed yet.
 *
 * [SwipeToReveal] direction is not localised, with the default being [RevealDirection.RightToLeft],
 * and [RightRevealing] and [RightRevealed] correspond to the actions getting revealed from the
 * right side of the screen. In case swipe direction is set to [RevealDirection.Bidirectional],
 * actions can also get revealed from the left side of the screen, and in that case [LeftRevealing]
 * and [LeftRevealed] are used.
 *
 * @see [RevealDirection]
 */
@JvmInline
public value class RevealValue private constructor(public val value: Int) {
    public companion object {
        /**
         * The value which represents the state in which the whole revealable content is fully
         * revealed, and they are displayed on the left side of the screen. This also represents the
         * state in which one of the actions has been triggered/performed.
         *
         * This is only used when the swipe direction is set to [RevealDirection.Bidirectional], and
         * the user swipes from the left side of the screen.
         */
        public val LeftRevealed: RevealValue = RevealValue(-2)

        /**
         * The value which represents the state in which all the actions are revealed and the top
         * content is not being swiped. In this state, none of the actions have been triggered or
         * performed yet, and they are displayed on the left side of the screen.
         *
         * This is only used when the swipe direction is set to [RevealDirection.Bidirectional], and
         * the user swipes from the left side of the screen.
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
        public val Bidirectional: RevealDirection = RevealDirection(1)
    }
}

/**
 * A class to keep track of the state of the composable. It can be used to customise the behavior
 * and state of the composable.
 *
 * @param initialValue The initial value of this state.
 * @constructor Create a [RevealState].
 */
public class RevealState(
    initialValue: RevealValue,
) {
    /**
     * The current [RevealValue] based on the status of the component.
     *
     * @see swipeableV2
     */
    public val currentValue: RevealValue
        get() = swipeableState.currentValue

    /**
     * The target [RevealValue] based on the status of the component. This will be equal to the
     * [currentValue] if there is no animation running or swiping has stopped. Otherwise, this
     * returns the next [RevealValue] based on the animation/swipe direction.
     *
     * @see swipeableV2
     */
    public val targetValue: RevealValue
        get() = swipeableState.targetValue

    /**
     * Returns whether the animation is running or not.
     *
     * @see swipeableV2
     */
    public val isAnimationRunning: Boolean
        get() = swipeableState.isAnimationRunning

    /**
     * The current amount by which the revealable content has been revealed by.
     *
     * @see swipeableV2
     */
    public val offset: Float
        get() = swipeableState.offset ?: 0f

    /**
     * Snaps to the [targetValue] without any animation.
     *
     * @param targetValue The target [RevealValue] where the [currentValue] will be changed to.
     * @see swipeableV2
     */
    public suspend fun snapTo(targetValue: RevealValue) {
        // Cover the previously open component if revealing a different one
        if (targetValue != Covered) {
            resetLastState(this)
        }
        swipeableState.snapTo(targetValue)
    }

    /**
     * Animates to the [targetValue] with the animation spec provided.
     *
     * @param targetValue The target [RevealValue] where the [currentValue] will animate to.
     */
    public suspend fun animateTo(targetValue: RevealValue) {
        // Cover the previously open component if revealing a different one
        if (targetValue != Covered) {
            resetLastState(this)
        }
        try {
            swipeableState.animateTo(targetValue)
        } finally {
            if (targetValue == Covered) {
                lastActionType = RevealActionType.None
            }
        }
    }

    internal val nestedScrollDispatcher: NestedScrollDispatcher = NestedScrollDispatcher()

    /** [androidx.wear.compose.materialcore.SwipeableV2State] internal instance for the state. */
    internal val swipeableState =
        SwipeableV2State(
            initialValue = initialValue,
            animationSpec = SwipeToRevealDefaults.AnimationSpec,
            confirmValueChange = { revealValue -> confirmValueChangeAndReset(revealValue) },
            positionalThreshold = { totalDistance ->
                SwipeToRevealDefaults.PositionalThreshold(totalDistance)
            },
            nestedScrollDispatcher = nestedScrollDispatcher,
        )

    internal var lastActionType: RevealActionType by mutableStateOf(RevealActionType.None)

    /**
     * The threshold, in pixels, where the revealed actions are fully visible but the existing
     * content would be left in place if the reveal action was stopped. This threshold is defined by
     * the [RightRevealing] anchor. If there is no such anchor defined for [RightRevealing], it
     * returns 0.0f.
     */
    /* @FloatRange(from = 0.0) */
    internal var revealThreshold: Float by mutableFloatStateOf(0.0f)

    /**
     * The total width of the component in pixels. Initialise to zero, updated when the width
     * changes.
     */
    internal var width: Float by mutableFloatStateOf(0.0f)

    /**
     * Require the current offset.
     *
     * @throws IllegalStateException If the offset has not been initialized yet
     */
    internal fun requireOffset(): Float = swipeableState.requireOffset()

    private suspend fun confirmValueChangeAndReset(revealValue: RevealValue): Boolean {
        val currentState = this
        // Update the state if the reveal value is changing to a different value than Covered.
        if (revealValue != Covered) {
            resetLastState(currentState)
        }
        return true
    }

    /**
     * Resets last state if a different SwipeToReveal is being moved to new anchor and the last
     * state is in [RightRevealing] mode which represents no action has been performed yet. In
     * [RightRevealed], the action has been performed and it will not be reset.
     */
    private suspend fun resetLastState(currentState: RevealState) {
        val oldState = SingleSwipeCoordinator.lastUpdatedState.getAndSet(currentState)
        if (currentState != oldState && oldState?.currentValue == RightRevealing) {
            oldState.animateTo(Covered)
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
 */
@Composable
public fun rememberRevealState(
    initialValue: RevealValue = Covered,
): RevealState =
    remember(initialValue) {
        RevealState(
            initialValue = initialValue,
        )
    }

@Composable
internal fun SwipeToRevealImpl(
    primaryAction: @Composable () -> Unit,
    onFullSwipe: () -> Unit,
    @SuppressLint("PrimitiveInCollection") anchors: Set<RevealValue>,
    modifier: Modifier = Modifier,
    state: RevealState = rememberRevealState(),
    revealDirection: RevealDirection = RightToLeft,
    secondaryAction: (@Composable () -> Unit)? = null,
    undoAction: (@Composable () -> Unit)? = null,
    hasPartiallyRevealedState: Boolean = true,
    gestureInclusion: GestureInclusion = gestureInclusion(state = state),
    content: @Composable () -> Unit
) {
    // A no-op NestedScrollConnection which does not consume scroll/fling events
    val noOpNestedScrollConnection = remember { object : NestedScrollConnection {} }

    var globalPosition by remember { mutableStateOf<LayoutCoordinates?>(null) }

    var allowSwipe by remember { mutableStateOf(true) }

    val screenWidthPx =
        with(LocalDensity.current) { LocalConfiguration.current.screenWidthDp.dp.toPx() }
    val anchorWidthPx =
        with(LocalDensity.current) {
            if (secondaryAction == null) {
                SingleActionAnchorWidth.toPx()
            } else {
                DoubleActionAnchorWidth.toPx()
            }
        }

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
                                state.currentValue != LeftRevealed &&
                                state.currentValue != RightRevealed,
                    )
                    .swipeAnchors(state = state.swipeableState, possibleValues = anchors) {
                        value,
                        layoutSize ->
                        val swipeableWidthPx = layoutSize.width.toFloat()
                        // Update the total width which will be used to calculate the anchors
                        state.width = swipeableWidthPx
                        // Multiply the anchor with -1f to get the actual swipeable anchor
                        when (value) {
                            Covered -> 0f
                            LeftRevealing,
                            RightRevealing -> {
                                if (secondaryAction == null && !hasPartiallyRevealedState) {
                                    0f
                                } else {
                                    val anchorSideMultiplier =
                                        if (value == RightRevealing) -1 else 1

                                    val result =
                                        (anchorWidthPx / screenWidthPx) *
                                            swipeableWidthPx *
                                            anchorSideMultiplier

                                    if (value == RightRevealing) {
                                        state.revealThreshold = abs(result)
                                    }

                                    result
                                }
                            }
                            LeftRevealed,
                            RightRevealed -> {
                                val anchorSideMultiplier = if (value == RightRevealed) -1 else 1
                                swipeableWidthPx * anchorSideMultiplier
                            }
                            else -> null
                        }
                    }
                    // NestedScrollDispatcher sends the scroll/fling events from the node to its
                    // parent
                    // and onwards including the modifier chain. Apply it in the end to let nested
                    // scroll
                    // connection applied before this modifier consume the scroll/fling events.
                    .nestedScroll(noOpNestedScrollConnection, state.nestedScrollDispatcher)
        ) {
            val swipeCompleted =
                state.currentValue == RightRevealed || state.currentValue == LeftRevealed
            val lastActionIsSecondary = state.lastActionType == RevealActionType.SecondaryAction
            val isWithinRevealOffset by remember {
                derivedStateOf { abs(state.offset) <= state.revealThreshold }
            }
            val canSwipeRight = revealDirection == Bidirectional

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
                        else AbsoluteAlignment.CenterRight
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
                        label = "AnimatedContentS2R"
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
                                    label = "UndoActionAlpha"
                                )
                            Row(
                                modifier =
                                    Modifier.graphicsLayer { alpha = undoActionAlpha.value }
                                        .fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                ActionSlot(content = undoAction)
                            }
                        } else {
                            // Animate weight for secondary action slot.
                            val secondaryActionWeight =
                                animateFloatAsState(
                                    targetValue = if (showSecondaryAction) 1f else 0f,
                                    animationSpec = tween(durationMillis = QUICK_ANIMATION),
                                    label = "SecondaryActionAnimationSpec"
                                )
                            val secondaryActionAlpha =
                                animateFloatAsState(
                                    targetValue =
                                        if (!showSecondaryAction || hideActions) 0f else 1f,
                                    animationSpec =
                                        tween(
                                            durationMillis = QUICK_ANIMATION,
                                            easing = LinearEasing
                                        ),
                                    label = "SecondaryActionAlpha"
                                )
                            val primaryActionAlpha =
                                animateFloatAsState(
                                    targetValue = if (hideActions) 0f else 1f,
                                    animationSpec =
                                        tween(durationMillis = 100, easing = LinearEasing),
                                    label = "PrimaryActionAlpha"
                                )
                            val revealedContentAlpha =
                                animateFloatAsState(
                                    targetValue = if (swipeCompleted) 0f else 1f,
                                    animationSpec =
                                        tween(
                                            durationMillis = FLASH_ANIMATION,
                                            easing = LinearEasing
                                        ),
                                    label = "RevealedContentAlpha"
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
                                                        globalPosition
                                                    )
                                                )
                                            }
                                        },
                                horizontalArrangement = Arrangement.Absolute.Right
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
                                        opacity = primaryActionAlpha
                                    )
                                } else {
                                    ActionSlot(
                                        content = primaryAction,
                                        opacity = primaryActionAlpha
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
                            y = 0
                        )
                    }
            ) {
                content()
            }
            LaunchedEffect(state.currentValue, state.lastActionType) {
                if (
                    (state.currentValue == LeftRevealed || state.currentValue == RightRevealed) &&
                        state.lastActionType == RevealActionType.None
                ) {
                    // Full swipe triggers the main action, but does not set the click type.
                    // Explicitly set the click type as main action when full swipe occurs.
                    state.lastActionType = RevealActionType.PrimaryAction
                    onFullSwipe()
                }
            }
        }
    }
}

/**
 * Different values which can trigger the state change from one [RevealValue] to another. These are
 * not set by themselves and need to be set appropriately with [RevealState.snapTo] and
 * [RevealState.animateTo].
 */
@JvmInline
internal value class RevealActionType private constructor(public val value: Int) {
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

@Stable
private class DefaultGestureInclusion(
    private val revealState: RevealState,
    private val edgeZoneFraction: Float
) : GestureInclusion {
    override fun ignoreGestureStart(offset: Offset, layoutCoordinates: LayoutCoordinates): Boolean {
        val screenOffset = layoutCoordinates.localToScreen(offset)
        val screenWidth = layoutCoordinates.findRootCoordinates().size.width
        return revealState.currentValue == Covered &&
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
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier.weight(weight).graphicsLayer { alpha = opacity.value },
        contentAlignment = Alignment.Center
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
                            easing = STANDARD_IN_OUT
                        )
                ),
        // animation spec for the fading out content and actions (fadeOut)
        initialContentExit =
            fadeOut(animationSpec = tween(durationMillis = FLASH_ANIMATION, easing = LinearEasing))
    )

private fun fadeOutUndo(): ContentTransform =
    ContentTransform(
        // No animation, fade-in in 0 milliseconds since enter transition is mandatory
        targetContentEnter =
            fadeIn(animationSpec = tween(durationMillis = 0, delayMillis = SHORT_ANIMATION)),

        // animation spec for the fading out undo action (fadeOut + scaleOut)
        initialContentExit =
            fadeOut(animationSpec = tween(durationMillis = SHORT_ANIMATION, easing = LinearEasing))
    )

private fun calculateVerticalOffsetBasedOnScreenPosition(
    childHeight: Int,
    globalPosition: LayoutCoordinates?
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

private fun startFadeInFraction(hasSecondaryAction: Boolean) =
    if (hasSecondaryAction) {
        DOUBLE_ICON_VISIBLE_THRESHOLD_AS_SCREEN_WIDTH_PERCENTAGE
    } else {
        SINGLE_ICON_VISIBLE_THRESHOLD_AS_SCREEN_WIDTH_PERCENTAGE
    }

private fun endFadeInFraction(hasSecondaryAction: Boolean) =
    if (hasSecondaryAction) {
        DOUBLE_ICON_FADE_IN_END_THRESHOLD_AS_SCREEN_WIDTH_PERCENTAGE
    } else {
        SINGLE_ICON_FADE_IN_END_THRESHOLD_AS_SCREEN_WIDTH_PERCENTAGE
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

private val ICON_AND_TEXT_PADDING = 4.dp

private val ACTION_BUTTON_CONTENT_PADDING = 4.dp

// Swipe required to start displaying the action buttons.
private const val BUTTON_VISIBLE_THRESHOLD_AS_SCREEN_WIDTH_PERCENTAGE = 0.06f

// End threshold for the fade in progression of the action buttons.
private const val BUTTON_FADE_IN_END_THRESHOLD_AS_SCREEN_WIDTH_PERCENTAGE = 0.12f

// Swipe required to start displaying the icon for a single action button.
private const val SINGLE_ICON_VISIBLE_THRESHOLD_AS_SCREEN_WIDTH_PERCENTAGE = 0.15f

// Swipe required to start displaying the icon for two action buttons.
private const val DOUBLE_ICON_VISIBLE_THRESHOLD_AS_SCREEN_WIDTH_PERCENTAGE = 0.30f

// End threshold for the fade in progression of the icon for a single action button.
private const val SINGLE_ICON_FADE_IN_END_THRESHOLD_AS_SCREEN_WIDTH_PERCENTAGE = 0.21f

// End threshold for the fade in progression of the icon for two action buttons.
private const val DOUBLE_ICON_FADE_IN_END_THRESHOLD_AS_SCREEN_WIDTH_PERCENTAGE = 0.36f

private val FULL_SCREEN_PADDING_FRACTION = 0.0625f

@SuppressLint("PrimitiveInCollection")
private val BidirectionalAnchors: Set<RevealValue> =
    setOf(LeftRevealed, LeftRevealing, Covered, RightRevealing, RightRevealed)
@SuppressLint("PrimitiveInCollection")
private val UnidirectionAnchors: Set<RevealValue> = setOf(Covered, RightRevealing, RightRevealed)
