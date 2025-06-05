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

package androidx.compose.foundation

import androidx.collection.mutableLongObjectMapOf
import androidx.compose.foundation.ComposeFoundationFlags.isDetectTapGesturesImmediateCoroutineDispatchEnabled
import androidx.compose.foundation.gestures.PressGestureScope
import androidx.compose.foundation.gestures.ScrollableContainerNode
import androidx.compose.foundation.gestures.detectTapAndPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.isChangedToDown
import androidx.compose.foundation.interaction.HoverInteraction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.internal.requirePrecondition
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.focus.Focusability
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType.Companion.KeyDown
import androidx.compose.ui.input.key.KeyEventType.Companion.KeyUp
import androidx.compose.ui.input.key.KeyInputModifierNode
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.SuspendingPointerInputModifierNode
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.isOutOfBounds
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.ObserverModifierNode
import androidx.compose.ui.node.PointerInputModifierNode
import androidx.compose.ui.node.SemanticsModifierNode
import androidx.compose.ui.node.TraversableNode
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.node.invalidateSemantics
import androidx.compose.ui.node.observeReads
import androidx.compose.ui.node.requireDensity
import androidx.compose.ui.node.traverseAncestors
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.debugInspectorInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.center
import androidx.compose.ui.unit.toOffset
import androidx.compose.ui.util.fastAll
import androidx.compose.ui.util.fastAny
import kotlin.math.max
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Configure component to receive clicks via input or accessibility "click" event.
 *
 * Add this modifier to the element to make it clickable within its bounds and show a default
 * indication when it's pressed.
 *
 * This version has no [MutableInteractionSource] or [Indication] parameters, the default indication
 * from [LocalIndication] will be used. To specify [MutableInteractionSource] or [Indication], use
 * the other overload.
 *
 * If you are only creating this clickable modifier inside composition, consider using the other
 * overload and explicitly passing `LocalIndication.current` for improved performance. For more
 * information see the documentation on the other overload.
 *
 * If you need to support double click or long click alongside the single click, consider using
 * [combinedClickable].
 *
 * ***Note*** Any removal operations on Android Views from `clickable` should wrap `onClick` in a
 * `post { }` block to guarantee the event dispatch completes before executing the removal. (You do
 * not need to do this when removing a composable because Compose guarantees it completes via the
 * snapshot state system.)
 *
 * @sample androidx.compose.foundation.samples.ClickableSample
 * @param enabled Controls the enabled state. When `false`, [onClick], and this modifier will appear
 *   disabled for accessibility services
 * @param onClickLabel semantic / accessibility label for the [onClick] action
 * @param role the type of user interface element. Accessibility services might use this to describe
 *   the element or do customizations
 * @param onClick will be called when user clicks on the element
 */
@Deprecated(
    message =
        "Replaced with new overload that only supports IndicationNodeFactory instances inside LocalIndication, and does not use composed",
    level = DeprecationLevel.HIDDEN,
)
fun Modifier.clickable(
    enabled: Boolean = true,
    onClickLabel: String? = null,
    role: Role? = null,
    onClick: () -> Unit,
) =
    composed(
        inspectorInfo =
            debugInspectorInfo {
                name = "clickable"
                properties["enabled"] = enabled
                properties["onClickLabel"] = onClickLabel
                properties["role"] = role
                properties["onClick"] = onClick
            }
    ) {
        val localIndication = LocalIndication.current
        val interactionSource =
            if (localIndication is IndicationNodeFactory) {
                // We can fast path here as it will be created inside clickable lazily
                null
            } else {
                // We need an interaction source to pass between the indication modifier and
                // clickable, so
                // by creating here we avoid another composed down the line
                remember { MutableInteractionSource() }
            }
        Modifier.clickable(
            enabled = enabled,
            onClickLabel = onClickLabel,
            onClick = onClick,
            role = role,
            indication = localIndication,
            interactionSource = interactionSource,
        )
    }

/**
 * Configure component to receive clicks via input or accessibility "click" event.
 *
 * Add this modifier to the element to make it clickable within its bounds and show a default
 * indication when it's pressed.
 *
 * This overload will use the [Indication] from [LocalIndication]. Use the other overload to
 * explicitly provide an [Indication] instance. Note that this overload only supports
 * [IndicationNodeFactory] instances provided through [LocalIndication] - it is strongly recommended
 * to migrate to [IndicationNodeFactory], but you can use the other overload if you still need to
 * support [Indication] instances that are not [IndicationNodeFactory].
 *
 * If [interactionSource] is `null`, an internal [MutableInteractionSource] will be lazily created
 * only when needed. This reduces the performance cost of clickable during composition, as creating
 * the [indication] can be delayed until there is an incoming
 * [androidx.compose.foundation.interaction.Interaction]. If you are only passing a remembered
 * [MutableInteractionSource] and you are never using it outside of clickable, it is recommended to
 * instead provide `null` to enable lazy creation. If you need the [Indication] to be created
 * eagerly, provide a remembered [MutableInteractionSource].
 *
 * If you need to support double click or long click alongside the single click, consider using
 * [combinedClickable].
 *
 * ***Note*** Any removal operations on Android Views from `clickable` should wrap `onClick` in a
 * `post { }` block to guarantee the event dispatch completes before executing the removal. (You do
 * not need to do this when removing a composable because Compose guarantees it completes via the
 * snapshot state system.)
 *
 * @sample androidx.compose.foundation.samples.ClickableSample
 * @param enabled Controls the enabled state. When `false`, [onClick], and this modifier will appear
 *   disabled for accessibility services
 * @param onClickLabel semantic / accessibility label for the [onClick] action
 * @param role the type of user interface element. Accessibility services might use this to describe
 *   the element or do customizations
 * @param interactionSource [MutableInteractionSource] that will be used to dispatch
 *   [PressInteraction.Press] when this clickable is pressed. If `null`, an internal
 *   [MutableInteractionSource] will be created if needed.
 * @param onClick will be called when user clicks on the element
 */
fun Modifier.clickable(
    enabled: Boolean = true,
    onClickLabel: String? = null,
    role: Role? = null,
    interactionSource: MutableInteractionSource? = null,
    onClick: () -> Unit,
): Modifier {
    @OptIn(ExperimentalFoundationApi::class)
    return if (ComposeFoundationFlags.isNonComposedClickableEnabled) {
        this.then(
            ClickableElement(
                interactionSource = interactionSource,
                indicationNodeFactory = null,
                useLocalIndication = true,
                enabled = enabled,
                onClickLabel = onClickLabel,
                role = role,
                onClick = onClick,
            )
        )
    } else {
        composed(
            inspectorInfo =
                debugInspectorInfo {
                    name = "clickable"
                    properties["enabled"] = enabled
                    properties["onClickLabel"] = onClickLabel
                    properties["role"] = role
                    properties["interactionSource"] = interactionSource
                    properties["onClick"] = onClick
                }
        ) {
            val localIndication = LocalIndication.current
            val intSource =
                interactionSource
                    ?: if (localIndication is IndicationNodeFactory) {
                        // We can fast path here as it will be created inside clickable lazily
                        null
                    } else {
                        // We need an interaction source to pass between the indication modifier and
                        // clickable, so
                        // by creating here we avoid another composed down the line
                        remember { MutableInteractionSource() }
                    }
            Modifier.clickable(
                enabled = enabled,
                onClickLabel = onClickLabel,
                onClick = onClick,
                role = role,
                indication = localIndication,
                interactionSource = intSource,
            )
        }
    }
}

/**
 * Configure component to receive clicks via input or accessibility "click" event.
 *
 * Add this modifier to the element to make it clickable within its bounds and show an indication as
 * specified in [indication] parameter.
 *
 * If [interactionSource] is `null`, and [indication] is an [IndicationNodeFactory], an internal
 * [MutableInteractionSource] will be lazily created along with the [indication] only when needed.
 * This reduces the performance cost of clickable during composition, as creating the [indication]
 * can be delayed until there is an incoming [androidx.compose.foundation.interaction.Interaction].
 * If you are only passing a remembered [MutableInteractionSource] and you are never using it
 * outside of clickable, it is recommended to instead provide `null` to enable lazy creation. If you
 * need [indication] to be created eagerly, provide a remembered [MutableInteractionSource].
 *
 * If [indication] is _not_ an [IndicationNodeFactory], and instead implements the deprecated
 * [Indication.rememberUpdatedInstance] method, you should explicitly pass a remembered
 * [MutableInteractionSource] as a parameter for [interactionSource] instead of `null`, as this
 * cannot be lazily created inside clickable.
 *
 * If you need to support double click or long click alongside the single click, consider using
 * [combinedClickable].
 *
 * ***Note*** Any removal operations on Android Views from `clickable` should wrap `onClick` in a
 * `post { }` block to guarantee the event dispatch completes before executing the removal. (You do
 * not need to do this when removing a composable because Compose guarantees it completes via the
 * snapshot state system.)
 *
 * @sample androidx.compose.foundation.samples.ClickableSample
 * @param interactionSource [MutableInteractionSource] that will be used to dispatch
 *   [PressInteraction.Press] when this clickable is pressed. If `null`, an internal
 *   [MutableInteractionSource] will be created if needed.
 * @param indication indication to be shown when modified element is pressed. By default, indication
 *   from [LocalIndication] will be used. Pass `null` to show no indication, or current value from
 *   [LocalIndication] to show theme default
 * @param enabled Controls the enabled state. When `false`, [onClick], and this modifier will appear
 *   disabled for accessibility services
 * @param onClickLabel semantic / accessibility label for the [onClick] action
 * @param role the type of user interface element. Accessibility services might use this to describe
 *   the element or do customizations
 * @param onClick will be called when user clicks on the element
 */
fun Modifier.clickable(
    interactionSource: MutableInteractionSource?,
    indication: Indication?,
    enabled: Boolean = true,
    onClickLabel: String? = null,
    role: Role? = null,
    onClick: () -> Unit,
) =
    clickableWithIndicationIfNeeded(
        interactionSource = interactionSource,
        indication = indication,
    ) { intSource, indicationNodeFactory ->
        ClickableElement(
            interactionSource = intSource,
            indicationNodeFactory = indicationNodeFactory,
            useLocalIndication = false,
            enabled = enabled,
            onClickLabel = onClickLabel,
            role = role,
            onClick = onClick,
        )
    }

/**
 * Configure component to receive clicks, double clicks and long clicks via input or accessibility
 * "click" event.
 *
 * Add this modifier to the element to make it clickable within its bounds.
 *
 * If you need only click handling, and no double or long clicks, consider using [clickable]
 *
 * This version has no [MutableInteractionSource] or [Indication] parameters, the default indication
 * from [LocalIndication] will be used. To specify [MutableInteractionSource] or [Indication], use
 * the other overload.
 *
 * If you are only creating this combinedClickable modifier inside composition, consider using the
 * other overload and explicitly passing `LocalIndication.current` for improved performance. For
 * more information see the documentation on the other overload.
 *
 * Note, if the modifier instance gets re-used between a key down and key up events, the ongoing
 * input will be aborted.
 *
 * ***Note*** Any removal operations on Android Views from `clickable` should wrap `onClick` in a
 * `post { }` block to guarantee the event dispatch completes before executing the removal. (You do
 * not need to do this when removing a composable because Compose guarantees it completes via the
 * snapshot state system.)
 *
 * @sample androidx.compose.foundation.samples.ClickableSample
 * @param enabled Controls the enabled state. When `false`, [onClick], [onLongClick] or
 *   [onDoubleClick] won't be invoked
 * @param onClickLabel semantic / accessibility label for the [onClick] action
 * @param role the type of user interface element. Accessibility services might use this to describe
 *   the element or do customizations
 * @param onLongClickLabel semantic / accessibility label for the [onLongClick] action
 * @param onLongClick will be called when user long presses on the element
 * @param onDoubleClick will be called when user double clicks on the element
 * @param hapticFeedbackEnabled whether to use the default [HapticFeedback] behavior
 * @param onClick will be called when user clicks on the element
 */
@Deprecated(
    message =
        "Replaced with new overload that only supports IndicationNodeFactory instances inside LocalIndication, and does not use composed",
    level = DeprecationLevel.HIDDEN,
)
fun Modifier.combinedClickable(
    enabled: Boolean = true,
    onClickLabel: String? = null,
    role: Role? = null,
    onLongClickLabel: String? = null,
    onLongClick: (() -> Unit)? = null,
    onDoubleClick: (() -> Unit)? = null,
    hapticFeedbackEnabled: Boolean = true,
    onClick: () -> Unit,
) =
    composed(
        inspectorInfo =
            debugInspectorInfo {
                name = "combinedClickable"
                properties["enabled"] = enabled
                properties["onClickLabel"] = onClickLabel
                properties["role"] = role
                properties["onClick"] = onClick
                properties["onDoubleClick"] = onDoubleClick
                properties["onLongClick"] = onLongClick
                properties["onLongClickLabel"] = onLongClickLabel
                properties["hapticFeedbackEnabled"] = hapticFeedbackEnabled
            }
    ) {
        val localIndication = LocalIndication.current
        val interactionSource =
            if (localIndication is IndicationNodeFactory) {
                // We can fast path here as it will be created inside clickable lazily
                null
            } else {
                // We need an interaction source to pass between the indication modifier and
                // clickable, so
                // by creating here we avoid another composed down the line
                remember { MutableInteractionSource() }
            }
        Modifier.combinedClickable(
            enabled = enabled,
            onClickLabel = onClickLabel,
            onLongClickLabel = onLongClickLabel,
            onLongClick = onLongClick,
            onDoubleClick = onDoubleClick,
            onClick = onClick,
            role = role,
            indication = localIndication,
            interactionSource = interactionSource,
            hapticFeedbackEnabled = hapticFeedbackEnabled,
        )
    }

/**
 * Configure component to receive clicks, double clicks and long clicks via input or accessibility
 * "click" event.
 *
 * Add this modifier to the element to make it clickable within its bounds.
 *
 * If you need only click handling, and no double or long clicks, consider using [clickable]
 *
 * This overload will use the [Indication] from [LocalIndication]. Use the other overload to
 * explicitly provide an [Indication] instance. Note that this overload only supports
 * [IndicationNodeFactory] instances provided through [LocalIndication] - it is strongly recommended
 * to migrate to [IndicationNodeFactory], but you can use the other overload if you still need to
 * support [Indication] instances that are not [IndicationNodeFactory].
 *
 * If [interactionSource] is `null`, an internal [MutableInteractionSource] will be lazily created
 * only when needed. This reduces the performance cost of combinedClickable during composition, as
 * creating the [indication] can be delayed until there is an incoming
 * [androidx.compose.foundation.interaction.Interaction]. If you are only passing a remembered
 * [MutableInteractionSource] and you are never using it outside of combinedClickable, it is
 * recommended to instead provide `null` to enable lazy creation. If you need the [Indication] to be
 * created eagerly, provide a remembered [MutableInteractionSource].
 *
 * Note, if the modifier instance gets re-used between a key down and key up events, the ongoing
 * input will be aborted.
 *
 * ***Note*** Any removal operations on Android Views from `clickable` should wrap `onClick` in a
 * `post { }` block to guarantee the event dispatch completes before executing the removal. (You do
 * not need to do this when removing a composable because Compose guarantees it completes via the
 * snapshot state system.)
 *
 * @sample androidx.compose.foundation.samples.ClickableSample
 * @param enabled Controls the enabled state. When `false`, [onClick], [onLongClick] or
 *   [onDoubleClick] won't be invoked
 * @param onClickLabel semantic / accessibility label for the [onClick] action
 * @param role the type of user interface element. Accessibility services might use this to describe
 *   the element or do customizations
 * @param onLongClickLabel semantic / accessibility label for the [onLongClick] action
 * @param onLongClick will be called when user long presses on the element
 * @param onDoubleClick will be called when user double clicks on the element
 * @param hapticFeedbackEnabled whether to use the default [HapticFeedback] behavior
 * @param interactionSource [MutableInteractionSource] that will be used to dispatch
 *   [PressInteraction.Press] when this clickable is pressed. If `null`, an internal
 *   [MutableInteractionSource] will be created if needed.
 * @param onClick will be called when user clicks on the element
 */
fun Modifier.combinedClickable(
    enabled: Boolean = true,
    onClickLabel: String? = null,
    role: Role? = null,
    onLongClickLabel: String? = null,
    onLongClick: (() -> Unit)? = null,
    onDoubleClick: (() -> Unit)? = null,
    hapticFeedbackEnabled: Boolean = true,
    interactionSource: MutableInteractionSource? = null,
    onClick: () -> Unit,
): Modifier {
    @OptIn(ExperimentalFoundationApi::class)
    return if (ComposeFoundationFlags.isNonComposedClickableEnabled) {
        this.then(
            CombinedClickableElement(
                enabled = enabled,
                onClickLabel = onClickLabel,
                onLongClickLabel = onLongClickLabel,
                onLongClick = onLongClick,
                onDoubleClick = onDoubleClick,
                onClick = onClick,
                role = role,
                interactionSource = interactionSource,
                indicationNodeFactory = null,
                useLocalIndication = true,
                hapticFeedbackEnabled = hapticFeedbackEnabled,
            )
        )
    } else
        composed(
            inspectorInfo =
                debugInspectorInfo {
                    name = "combinedClickable"
                    properties["enabled"] = enabled
                    properties["onClickLabel"] = onClickLabel
                    properties["role"] = role
                    properties["onClick"] = onClick
                    properties["onDoubleClick"] = onDoubleClick
                    properties["onLongClick"] = onLongClick
                    properties["onLongClickLabel"] = onLongClickLabel
                    properties["hapticFeedbackEnabled"] = hapticFeedbackEnabled
                }
        ) {
            val localIndication = LocalIndication.current
            val intSource =
                interactionSource
                    ?: if (localIndication is IndicationNodeFactory) {
                        // We can fast path here as it will be created inside clickable lazily
                        null
                    } else {
                        // We need an interaction source to pass between the indication modifier and
                        // clickable, so
                        // by creating here we avoid another composed down the line
                        remember { MutableInteractionSource() }
                    }
            Modifier.combinedClickable(
                enabled = enabled,
                onClickLabel = onClickLabel,
                onLongClickLabel = onLongClickLabel,
                onLongClick = onLongClick,
                onDoubleClick = onDoubleClick,
                onClick = onClick,
                role = role,
                indication = localIndication,
                interactionSource = intSource,
                hapticFeedbackEnabled = hapticFeedbackEnabled,
            )
        }
}

@Deprecated(message = "Maintained for binary compatibility", level = DeprecationLevel.HIDDEN)
fun Modifier.combinedClickable(
    enabled: Boolean = true,
    onClickLabel: String? = null,
    role: Role? = null,
    onLongClickLabel: String? = null,
    onLongClick: (() -> Unit)? = null,
    onDoubleClick: (() -> Unit)? = null,
    onClick: () -> Unit,
) =
    composed(
        inspectorInfo =
            debugInspectorInfo {
                name = "combinedClickable"
                properties["enabled"] = enabled
                properties["onClickLabel"] = onClickLabel
                properties["role"] = role
                properties["onClick"] = onClick
                properties["onDoubleClick"] = onDoubleClick
                properties["onLongClick"] = onLongClick
                properties["onLongClickLabel"] = onLongClickLabel
            }
    ) {
        val localIndication = LocalIndication.current
        val interactionSource =
            if (localIndication is IndicationNodeFactory) {
                // We can fast path here as it will be created inside clickable lazily
                null
            } else {
                // We need an interaction source to pass between the indication modifier and
                // clickable, so
                // by creating here we avoid another composed down the line
                remember { MutableInteractionSource() }
            }
        Modifier.combinedClickable(
            enabled = enabled,
            onClickLabel = onClickLabel,
            onLongClickLabel = onLongClickLabel,
            onLongClick = onLongClick,
            onDoubleClick = onDoubleClick,
            onClick = onClick,
            role = role,
            indication = localIndication,
            interactionSource = interactionSource,
            hapticFeedbackEnabled = true,
        )
    }

/**
 * Configure component to receive clicks, double clicks and long clicks via input or accessibility
 * "click" event.
 *
 * Add this modifier to the element to make it clickable within its bounds.
 *
 * If you need only click handling, and no double or long clicks, consider using [clickable].
 *
 * Add this modifier to the element to make it clickable within its bounds.
 *
 * If [interactionSource] is `null`, and [indication] is an [IndicationNodeFactory], an internal
 * [MutableInteractionSource] will be lazily created along with the [indication] only when needed.
 * This reduces the performance cost of clickable during composition, as creating the [indication]
 * can be delayed until there is an incoming [androidx.compose.foundation.interaction.Interaction].
 * If you are only passing a remembered [MutableInteractionSource] and you are never using it
 * outside of clickable, it is recommended to instead provide `null` to enable lazy creation. If you
 * need [indication] to be created eagerly, provide a remembered [MutableInteractionSource].
 *
 * If [indication] is _not_ an [IndicationNodeFactory], and instead implements the deprecated
 * [Indication.rememberUpdatedInstance] method, you should explicitly pass a remembered
 * [MutableInteractionSource] as a parameter for [interactionSource] instead of `null`, as this
 * cannot be lazily created inside clickable.
 *
 * Note, if the modifier instance gets re-used between a key down and key up events, the ongoing
 * input will be aborted.
 *
 * ***Note*** Any removal operations on Android Views from `clickable` should wrap `onClick` in a
 * `post { }` block to guarantee the event dispatch completes before executing the removal. (You do
 * not need to do this when removing a composable because Compose guarantees it completes via the
 * snapshot state system.)
 *
 * @sample androidx.compose.foundation.samples.ClickableSample
 * @param interactionSource [MutableInteractionSource] that will be used to emit
 *   [PressInteraction.Press] when this clickable is pressed. If `null`, an internal
 *   [MutableInteractionSource] will be created if needed.
 * @param indication indication to be shown when modified element is pressed. By default, indication
 *   from [LocalIndication] will be used. Pass `null` to show no indication, or current value from
 *   [LocalIndication] to show theme default
 * @param enabled Controls the enabled state. When `false`, [onClick], [onLongClick] or
 *   [onDoubleClick] won't be invoked
 * @param onClickLabel semantic / accessibility label for the [onClick] action
 * @param role the type of user interface element. Accessibility services might use this to describe
 *   the element or do customizations
 * @param onLongClickLabel semantic / accessibility label for the [onLongClick] action
 * @param onLongClick will be called when user long presses on the element
 * @param onDoubleClick will be called when user double clicks on the element
 * @param hapticFeedbackEnabled whether to use the default [HapticFeedback] behavior
 * @param onClick will be called when user clicks on the element
 */
fun Modifier.combinedClickable(
    interactionSource: MutableInteractionSource?,
    indication: Indication?,
    enabled: Boolean = true,
    onClickLabel: String? = null,
    role: Role? = null,
    onLongClickLabel: String? = null,
    onLongClick: (() -> Unit)? = null,
    onDoubleClick: (() -> Unit)? = null,
    hapticFeedbackEnabled: Boolean = true,
    onClick: () -> Unit,
) =
    clickableWithIndicationIfNeeded(
        interactionSource = interactionSource,
        indication = indication,
    ) { intSource, indicationNodeFactory ->
        CombinedClickableElement(
            interactionSource = intSource,
            indicationNodeFactory = indicationNodeFactory,
            useLocalIndication = false,
            enabled = enabled,
            onClickLabel = onClickLabel,
            role = role,
            onClick = onClick,
            onLongClickLabel = onLongClickLabel,
            onLongClick = onLongClick,
            onDoubleClick = onDoubleClick,
            hapticFeedbackEnabled = hapticFeedbackEnabled,
        )
    }

@Deprecated(message = "Maintained for binary compatibility", level = DeprecationLevel.HIDDEN)
fun Modifier.combinedClickable(
    interactionSource: MutableInteractionSource?,
    indication: Indication?,
    enabled: Boolean = true,
    onClickLabel: String? = null,
    role: Role? = null,
    onLongClickLabel: String? = null,
    onLongClick: (() -> Unit)? = null,
    onDoubleClick: (() -> Unit)? = null,
    onClick: () -> Unit,
) =
    clickableWithIndicationIfNeeded(
        interactionSource = interactionSource,
        indication = indication,
    ) { intSource, indicationNodeFactory ->
        CombinedClickableElement(
            interactionSource = intSource,
            indicationNodeFactory = indicationNodeFactory,
            useLocalIndication = false,
            enabled = enabled,
            onClickLabel = onClickLabel,
            role = role,
            onClick = onClick,
            onLongClickLabel = onLongClickLabel,
            onLongClick = onLongClick,
            onDoubleClick = onDoubleClick,
            hapticFeedbackEnabled = true,
        )
    }

/**
 * Utility Modifier factory that handles edge cases for [interactionSource], and [indication].
 * [createClickable] is the lambda that creates the actual clickable element, which will be chained
 * with [Modifier.indication] if needed.
 */
internal inline fun Modifier.clickableWithIndicationIfNeeded(
    interactionSource: MutableInteractionSource?,
    indication: Indication?,
    crossinline createClickable: (MutableInteractionSource?, IndicationNodeFactory?) -> Modifier,
): Modifier {
    return this.then(
        when {
            // Fast path - indication is managed internally
            indication is IndicationNodeFactory -> createClickable(interactionSource, indication)
            // Fast path - no need for indication
            indication == null -> createClickable(interactionSource, null)
            // Non-null Indication (not IndicationNodeFactory) with a non-null InteractionSource
            interactionSource != null ->
                Modifier.indication(interactionSource, indication)
                    .then(createClickable(interactionSource, null))
            // Non-null Indication (not IndicationNodeFactory) with a null InteractionSource, so we
            // need
            // to use composed to create an InteractionSource that can be shared. This should be a
            // rare
            // code path and can only be hit from new callers.
            else ->
                Modifier.composed {
                    val newInteractionSource = remember { MutableInteractionSource() }
                    Modifier.indication(newInteractionSource, indication)
                        .then(createClickable(newInteractionSource, null))
                }
        }
    )
}

/**
 * How long to wait before appearing 'pressed' (emitting [PressInteraction.Press]) - if a touch down
 * will quickly become a drag / scroll, this timeout means that we don't show a press effect.
 */
internal expect val TapIndicationDelay: Long

/**
 * Returns whether the root Compose layout node is hosted in a scrollable container outside of
 * Compose. On Android this will be whether the root View is in a scrollable ViewGroup, as even if
 * nothing in the Compose part of the hierarchy is scrollable, if the View itself is in a scrollable
 * container, we still want to delay presses in case presses in Compose convert to a scroll outside
 * of Compose.
 *
 * Combine this with [hasScrollableContainer], which returns whether a [Modifier] is within a
 * scrollable Compose layout, to calculate whether this modifier is within some form of scrollable
 * container, and hence should delay presses.
 */
internal expect fun DelegatableNode.isComposeRootInScrollableContainer(): Boolean

/**
 * Whether the specified [KeyEvent] should trigger a press for a clickable component, i.e. whether
 * it is associated with a press of an enter key or dpad centre.
 */
private val KeyEvent.isPress: Boolean
    get() = type == KeyDown && isEnter

/**
 * Whether the specified [KeyEvent] should trigger a click for a clickable component, i.e. whether
 * it is associated with a release of an enter key or dpad centre.
 */
private val KeyEvent.isClick: Boolean
    get() = type == KeyUp && isEnter

private val KeyEvent.isEnter: Boolean
    get() =
        when (key) {
            Key.DirectionCenter,
            Key.Enter,
            Key.NumPadEnter,
            Key.Spacebar -> true
            else -> false
        }

private class ClickableElement(
    private val interactionSource: MutableInteractionSource?,
    private val indicationNodeFactory: IndicationNodeFactory?,
    private val useLocalIndication: Boolean,
    private val enabled: Boolean,
    private val onClickLabel: String?,
    private val role: Role?,
    private val onClick: () -> Unit,
) : ModifierNodeElement<ClickableNode>() {
    override fun create() =
        ClickableNode(
            interactionSource = interactionSource,
            indicationNodeFactory = indicationNodeFactory,
            useLocalIndication = useLocalIndication,
            enabled = enabled,
            onClickLabel = onClickLabel,
            role = role,
            onClick = onClick,
        )

    override fun update(node: ClickableNode) {
        node.update(
            interactionSource = interactionSource,
            indicationNodeFactory = indicationNodeFactory,
            useLocalIndication = useLocalIndication,
            enabled = enabled,
            onClickLabel = onClickLabel,
            role = role,
            onClick = onClick,
        )
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "clickable"
        properties["enabled"] = enabled
        properties["onClick"] = onClick
        properties["onClickLabel"] = onClickLabel
        properties["role"] = role
        properties["interactionSource"] = interactionSource
        properties["indicationNodeFactory"] = indicationNodeFactory
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other === null) return false
        if (this::class != other::class) return false

        other as ClickableElement

        if (interactionSource != other.interactionSource) return false
        if (indicationNodeFactory != other.indicationNodeFactory) return false
        if (useLocalIndication != other.useLocalIndication) return false
        if (enabled != other.enabled) return false
        if (onClickLabel != other.onClickLabel) return false
        if (role != other.role) return false
        if (onClick !== other.onClick) return false

        return true
    }

    override fun hashCode(): Int {
        var result = (interactionSource?.hashCode() ?: 0)
        result = 31 * result + (indicationNodeFactory?.hashCode() ?: 0)
        result = 31 * result + useLocalIndication.hashCode()
        result = 31 * result + enabled.hashCode()
        result = 31 * result + (onClickLabel?.hashCode() ?: 0)
        result = 31 * result + (role?.hashCode() ?: 0)
        result = 31 * result + onClick.hashCode()
        return result
    }
}

private class CombinedClickableElement(
    private val interactionSource: MutableInteractionSource?,
    private val indicationNodeFactory: IndicationNodeFactory?,
    private val useLocalIndication: Boolean,
    private val enabled: Boolean,
    private val onClickLabel: String?,
    private val role: Role?,
    private val onClick: () -> Unit,
    private val onLongClickLabel: String?,
    private val onLongClick: (() -> Unit)?,
    private val onDoubleClick: (() -> Unit)?,
    private val hapticFeedbackEnabled: Boolean,
) : ModifierNodeElement<CombinedClickableNode>() {
    override fun create() =
        CombinedClickableNode(
            onClick = onClick,
            onLongClickLabel = onLongClickLabel,
            onLongClick = onLongClick,
            onDoubleClick = onDoubleClick,
            hapticFeedbackEnabled = hapticFeedbackEnabled,
            interactionSource = interactionSource,
            indicationNodeFactory = indicationNodeFactory,
            useLocalIndication = useLocalIndication,
            enabled = enabled,
            onClickLabel = onClickLabel,
            role = role,
        )

    override fun update(node: CombinedClickableNode) {
        node.hapticFeedbackEnabled = hapticFeedbackEnabled
        node.update(
            onClick,
            onLongClickLabel,
            onLongClick,
            onDoubleClick,
            interactionSource,
            indicationNodeFactory,
            useLocalIndication,
            enabled,
            onClickLabel,
            role,
        )
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "combinedClickable"
        properties["indicationNodeFactory"] = indicationNodeFactory
        properties["interactionSource"] = interactionSource
        properties["enabled"] = enabled
        properties["onClickLabel"] = onClickLabel
        properties["role"] = role
        properties["onClick"] = onClick
        properties["onDoubleClick"] = onDoubleClick
        properties["onLongClick"] = onLongClick
        properties["onLongClickLabel"] = onLongClickLabel
        properties["hapticFeedbackEnabled"] = hapticFeedbackEnabled
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other === null) return false
        if (this::class != other::class) return false

        other as CombinedClickableElement

        if (interactionSource != other.interactionSource) return false
        if (indicationNodeFactory != other.indicationNodeFactory) return false
        if (useLocalIndication != other.useLocalIndication) return false
        if (enabled != other.enabled) return false
        if (onClickLabel != other.onClickLabel) return false
        if (role != other.role) return false
        if (onClick !== other.onClick) return false
        if (onLongClickLabel != other.onLongClickLabel) return false
        if (onLongClick !== other.onLongClick) return false
        if (onDoubleClick !== other.onDoubleClick) return false
        if (hapticFeedbackEnabled != other.hapticFeedbackEnabled) return false

        return true
    }

    override fun hashCode(): Int {
        var result = (interactionSource?.hashCode() ?: 0)
        result = 31 * result + (indicationNodeFactory?.hashCode() ?: 0)
        result = 31 * result + useLocalIndication.hashCode()
        result = 31 * result + enabled.hashCode()
        result = 31 * result + (onClickLabel?.hashCode() ?: 0)
        result = 31 * result + (role?.hashCode() ?: 0)
        result = 31 * result + onClick.hashCode()
        result = 31 * result + (onLongClickLabel?.hashCode() ?: 0)
        result = 31 * result + (onLongClick?.hashCode() ?: 0)
        result = 31 * result + (onDoubleClick?.hashCode() ?: 0)
        result = 31 * result + hapticFeedbackEnabled.hashCode()
        return result
    }
}

internal open class ClickableNode(
    interactionSource: MutableInteractionSource?,
    indicationNodeFactory: IndicationNodeFactory?,
    useLocalIndication: Boolean,
    enabled: Boolean,
    onClickLabel: String?,
    role: Role?,
    onClick: () -> Unit,
) :
    AbstractClickableNode(
        interactionSource = interactionSource,
        indicationNodeFactory = indicationNodeFactory,
        useLocalIndication = useLocalIndication,
        enabled = enabled,
        onClickLabel = onClickLabel,
        role = role,
        onClick = onClick,
    ) {

    @OptIn(ExperimentalFoundationApi::class)
    private val isSuspendingPointerInputEnabled =
        // old behavior prior this flag was heavily relying on coroutines dispatching
        !isDetectTapGesturesImmediateCoroutineDispatchEnabled ||
            !ComposeFoundationFlags.isNonSuspendingPointerInputInClickableEnabled

    override fun createPointerInputNodeIfNeeded(): SuspendingPointerInputModifierNode? =
        if (isSuspendingPointerInputEnabled) {
            SuspendingPointerInputModifierNode {
                detectTapAndPress(
                    onPress = { offset ->
                        if (enabled) {
                            handlePressInteraction(offset)
                        }
                    },
                    onTap = { if (enabled) onClick() },
                )
            }
        } else {
            null
        }

    private fun getExtendedTouchPadding(size: IntSize): Size {
        // copied from SuspendingPointerInputModifierNodeImpl.extendedTouchPadding:
        // TODO expose this as a new public api available outside of suspending apis b/422396609
        val minimumTouchTargetSizeDp = currentValueOf(LocalViewConfiguration).minimumTouchTargetSize
        val minimumTouchTargetSize = with(requireDensity()) { minimumTouchTargetSizeDp.toSize() }
        val size = size
        val horizontal = max(0f, minimumTouchTargetSize.width - size.width) / 2f
        val vertical = max(0f, minimumTouchTargetSize.height - size.height) / 2f
        return Size(horizontal, vertical)
    }

    private var downEvent: PointerInputChange? = null

    @OptIn(ExperimentalFoundationApi::class)
    override fun onPointerEvent(
        pointerEvent: PointerEvent,
        pass: PointerEventPass,
        bounds: IntSize,
    ) {
        super.onPointerEvent(pointerEvent, pass, bounds)
        if (isSuspendingPointerInputEnabled) {
            return
        }
        if (pass == PointerEventPass.Main) {
            val downEvent = this.downEvent
            if (downEvent == null) {
                if (pointerEvent.isChangedToDown(requireUnconsumed = true)) {
                    val change = pointerEvent.changes[0]
                    change.consume()
                    this.downEvent = change
                    if (enabled) {
                        handlePressInteractionStart(change.position)
                    }
                }
            } else if (pointerEvent.changes.fastAll { it.changedToUp() }) {
                // All pointers are up
                val up = pointerEvent.changes[0]
                up.consume()
                if (enabled) {
                    handlePressInteractionRelease(downEvent.position)
                    onClick()
                }
                this.downEvent = null
            } else {
                val touchPadding = getExtendedTouchPadding(bounds)
                if (
                    pointerEvent.changes.fastAny {
                        it.isConsumed || it.isOutOfBounds(bounds, touchPadding)
                    }
                ) {
                    // Canceled
                    this.downEvent = null
                    handlePressInteractionCancel()
                }
            }
        } else if (pass == PointerEventPass.Final && downEvent != null) {
            // Check for cancel by position consumption. We can look on the Final pass of the
            // existing pointer event because it comes after the pass we checked above.
            if (pointerEvent.changes.fastAny { it.isConsumed && it != downEvent }) {
                // Canceled
                downEvent = null
                handlePressInteractionCancel()
            }
        }
    }

    override fun onCancelPointerInput() {
        super.onCancelPointerInput()
        if (downEvent != null) {
            downEvent = null
            handlePressInteractionCancel()
        }
    }

    fun update(
        interactionSource: MutableInteractionSource?,
        indicationNodeFactory: IndicationNodeFactory?,
        useLocalIndication: Boolean,
        enabled: Boolean,
        onClickLabel: String?,
        role: Role?,
        onClick: () -> Unit,
    ) {
        // enabled and onClick are captured inside callbacks, not as an input to detectTapGestures,
        // so no need need to reset pointer input handling when they change
        updateCommon(
            interactionSource = interactionSource,
            indicationNodeFactory = indicationNodeFactory,
            useLocalIndication = useLocalIndication,
            enabled = enabled,
            onClickLabel = onClickLabel,
            role = role,
            onClick = onClick,
        )
    }

    final override fun onClickKeyDownEvent(event: KeyEvent) = false

    final override fun onClickKeyUpEvent(event: KeyEvent): Boolean {
        onClick()
        return true
    }
}

private class CombinedClickableNode(
    onClick: () -> Unit,
    private var onLongClickLabel: String?,
    private var onLongClick: (() -> Unit)?,
    private var onDoubleClick: (() -> Unit)?,
    var hapticFeedbackEnabled: Boolean,
    interactionSource: MutableInteractionSource?,
    indicationNodeFactory: IndicationNodeFactory?,
    useLocalIndication: Boolean,
    enabled: Boolean,
    onClickLabel: String?,
    role: Role?,
) :
    CompositionLocalConsumerModifierNode,
    AbstractClickableNode(
        interactionSource = interactionSource,
        indicationNodeFactory = indicationNodeFactory,
        useLocalIndication = useLocalIndication,
        enabled = enabled,
        onClickLabel = onClickLabel,
        role = role,
        onClick = onClick,
    ) {
    class DoubleKeyClickState(val job: Job) {
        var doubleTapMinTimeMillisElapsed: Boolean = false
    }

    private val longKeyPressJobs = mutableLongObjectMapOf<Job>()
    private val doubleKeyClickStates = mutableLongObjectMapOf<DoubleKeyClickState>()

    override fun createPointerInputNodeIfNeeded() = SuspendingPointerInputModifierNode {
        detectTapGestures(
            onDoubleTap =
                if (enabled && onDoubleClick != null) {
                    { onDoubleClick?.invoke() }
                } else null,
            onLongPress =
                if (enabled && onLongClick != null) {
                    {
                        onLongClick?.invoke()
                        if (hapticFeedbackEnabled) {
                            currentValueOf(LocalHapticFeedback)
                                .performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                    }
                } else null,
            onPress = { offset ->
                if (enabled) {
                    handlePressInteraction(offset)
                }
            },
            onTap = {
                if (enabled) {
                    onClick()
                }
            },
        )
    }

    fun update(
        onClick: () -> Unit,
        onLongClickLabel: String?,
        onLongClick: (() -> Unit)?,
        onDoubleClick: (() -> Unit)?,
        interactionSource: MutableInteractionSource?,
        indicationNodeFactory: IndicationNodeFactory?,
        useLocalIndication: Boolean,
        enabled: Boolean,
        onClickLabel: String?,
        role: Role?,
    ) {
        var resetPointerInputHandling = false

        // onClick is captured inside a callback, not as an input to detectTapGestures,
        // so no need need to reset pointer input handling

        if (this.onLongClickLabel != onLongClickLabel) {
            this.onLongClickLabel = onLongClickLabel
            invalidateSemantics()
        }

        // We capture onLongClick and onDoubleClick inside the callback, so if the lambda changes
        // value we don't want to reset input handling - only reset if they go from not-defined to
        // defined, and vice-versa, as that is what is captured in the parameter to
        // detectTapGestures.
        if ((this.onLongClick == null) != (onLongClick == null)) {
            // Adding or removing longClick should cancel any existing press interactions
            disposeInteractions()
            // Adding or removing longClick should add / remove the corresponding property
            invalidateSemantics()
            resetPointerInputHandling = true
        }

        this.onLongClick = onLongClick

        if ((this.onDoubleClick == null) != (onDoubleClick == null)) {
            resetPointerInputHandling = true
        }
        this.onDoubleClick = onDoubleClick

        // enabled is captured as a parameter to detectTapGestures, so we need to restart detecting
        // gestures if it changes.
        if (this.enabled != enabled) {
            resetPointerInputHandling = true
            // Updating is handled inside updateCommon
        }

        updateCommon(
            interactionSource = interactionSource,
            indicationNodeFactory = indicationNodeFactory,
            useLocalIndication = useLocalIndication,
            enabled = enabled,
            onClickLabel = onClickLabel,
            role = role,
            onClick = onClick,
        )

        if (resetPointerInputHandling) resetPointerInputHandler()
    }

    override fun SemanticsPropertyReceiver.applyAdditionalSemantics() {
        if (onLongClick != null) {
            onLongClick(
                action = {
                    onLongClick?.invoke()
                    true
                },
                label = onLongClickLabel,
            )
        }
    }

    override fun onClickKeyDownEvent(event: KeyEvent): Boolean {
        val keyCode = event.key.keyCode
        var handledByLongClick = false
        if (onLongClick != null) {
            if (longKeyPressJobs[keyCode] == null) {
                longKeyPressJobs[keyCode] =
                    coroutineScope.launch {
                        delay(currentValueOf(LocalViewConfiguration).longPressTimeoutMillis)
                        onLongClick?.invoke()
                    }
                handledByLongClick = true
            }
        }
        val doubleClickState = doubleKeyClickStates[keyCode]
        // This is the second down event, so it might be a double click
        if (doubleClickState != null) {
            // Within the allowed timeout, so check if this is above the minimum time needed for
            // a double click
            if (doubleClickState.job.isActive) {
                doubleClickState.job.cancel()
                // If the second down was before the minimum double tap time, don't track this as
                // a double click. Instead, we need to invoke onClick for the previous click, since
                // that is now counted as a standalone click instead of the first of a double click.
                if (!doubleClickState.doubleTapMinTimeMillisElapsed) {
                    onClick()
                    doubleKeyClickStates.remove(keyCode)
                }
            } else {
                // We already invoked onClick because we passed the timeout, so stop tracking this
                // as a double click
                doubleKeyClickStates.remove(keyCode)
            }
        }
        return handledByLongClick
    }

    override fun onClickKeyUpEvent(event: KeyEvent): Boolean {
        val keyCode = event.key.keyCode
        var longClickInvoked = false
        if (longKeyPressJobs[keyCode] != null) {
            longKeyPressJobs[keyCode]?.let {
                if (it.isActive) {
                    it.cancel()
                } else {
                    // If we already passed the timeout, we invoked long click already, and so
                    // we shouldn't invoke onClick in this case
                    longClickInvoked = true
                }
            }
            longKeyPressJobs.remove(keyCode)
        }
        if (onDoubleClick != null) {
            when {
                // First click
                doubleKeyClickStates[keyCode] == null -> {
                    // We only track the second click if the first click was not a long click
                    if (!longClickInvoked) {
                        doubleKeyClickStates[keyCode] =
                            DoubleKeyClickState(
                                coroutineScope.launch {
                                    val configuration = currentValueOf(LocalViewConfiguration)
                                    val minTime = configuration.doubleTapMinTimeMillis
                                    val timeout = configuration.doubleTapTimeoutMillis
                                    delay(minTime)
                                    doubleKeyClickStates[keyCode]?.doubleTapMinTimeMillisElapsed =
                                        true
                                    // Delay the remainder until we are at timeout
                                    delay(timeout - minTime)
                                    // If there was no second key press after the timeout, invoke
                                    // onClick as normal
                                    onClick()
                                }
                            )
                    }
                }
                // Second click
                else -> {
                    // Invoke onDoubleClick if the second click was not a long click
                    if (!longClickInvoked) {
                        onDoubleClick?.invoke()
                    }
                    doubleKeyClickStates.remove(keyCode)
                }
            }
        } else {
            if (!longClickInvoked) {
                onClick()
            }
        }
        return true
    }

    override fun onCancelKeyInput() {
        resetKeyPressState()
    }

    override fun onReset() {
        super.onReset()
        resetKeyPressState()
    }

    private fun resetKeyPressState() {
        longKeyPressJobs.apply {
            forEachValue { it.cancel() }
            clear()
        }
        doubleKeyClickStates.apply {
            forEachValue { it.job.cancel() }
            clear()
        }
    }
}

internal abstract class AbstractClickableNode(
    private var interactionSource: MutableInteractionSource?,
    private var indicationNodeFactory: IndicationNodeFactory?,
    private var useLocalIndication: Boolean,
    enabled: Boolean,
    private var onClickLabel: String?,
    private var role: Role?,
    onClick: () -> Unit,
) :
    DelegatingNode(),
    PointerInputModifierNode,
    KeyInputModifierNode,
    SemanticsModifierNode,
    TraversableNode,
    CompositionLocalConsumerModifierNode,
    ObserverModifierNode {
    protected var enabled = enabled
        private set

    protected var onClick = onClick
        private set

    final override val shouldAutoInvalidate: Boolean = false

    private val focusableNode: FocusableNode =
        FocusableNode(
            interactionSource,
            focusability = Focusability.SystemDefined,
            onFocusChange = ::onFocusChange,
        )

    private var localIndicationNodeFactory: IndicationNodeFactory? = null

    private var pointerInputNode: SuspendingPointerInputModifierNode? = null
    private var indicationNode: DelegatableNode? = null

    private var pressInteraction: PressInteraction.Press? = null
    private var hoverInteraction: HoverInteraction.Enter? = null
    private val currentKeyPressInteractions = mutableLongObjectMapOf<PressInteraction.Press>()
    private var centerOffset: Offset = Offset.Zero

    // Track separately from interactionSource, as we will create our own internal
    // InteractionSource if needed
    private var userProvidedInteractionSource: MutableInteractionSource? = interactionSource

    private var lazilyCreateIndication = shouldLazilyCreateIndication()

    private fun shouldLazilyCreateIndication() = userProvidedInteractionSource == null

    /**
     * Handles subclass-specific click related pointer input logic. Hover is already handled
     * elsewhere, so this should only handle clicks.
     */
    abstract fun createPointerInputNodeIfNeeded(): SuspendingPointerInputModifierNode?

    open fun SemanticsPropertyReceiver.applyAdditionalSemantics() {}

    protected fun updateCommon(
        interactionSource: MutableInteractionSource?,
        indicationNodeFactory: IndicationNodeFactory?,
        useLocalIndication: Boolean,
        enabled: Boolean,
        onClickLabel: String?,
        role: Role?,
        onClick: () -> Unit,
    ) {
        var isIndicationNodeDirty = false
        // Compare against userProvidedInteractionSource, as we will create a new InteractionSource
        // lazily if the userProvidedInteractionSource is null, and assign it to interactionSource
        if (userProvidedInteractionSource != interactionSource) {
            disposeInteractions()
            userProvidedInteractionSource = interactionSource
            this.interactionSource = interactionSource
            isIndicationNodeDirty = true
        }
        if (this.indicationNodeFactory != indicationNodeFactory) {
            this.indicationNodeFactory = indicationNodeFactory
            isIndicationNodeDirty = true
        }
        if (this.useLocalIndication != useLocalIndication) {
            this.useLocalIndication = useLocalIndication
            if (useLocalIndication) {
                // Need to update localIndicationNodeFactory, and start observing changes
                onObservedReadsChanged()
            }
            isIndicationNodeDirty = true
        }
        if (this.enabled != enabled) {
            if (enabled) {
                delegate(focusableNode)
            } else {
                // TODO: Should we remove indicationNode? Previously we always emitted indication
                undelegate(focusableNode)
                disposeInteractions()
            }
            invalidateSemantics()
            this.enabled = enabled
        }
        if (this.onClickLabel != onClickLabel) {
            this.onClickLabel = onClickLabel
            invalidateSemantics()
        }
        if (this.role != role) {
            this.role = role
            invalidateSemantics()
        }
        this.onClick = onClick
        if (lazilyCreateIndication != shouldLazilyCreateIndication()) {
            lazilyCreateIndication = shouldLazilyCreateIndication()
            // If we are no longer lazily creating the node, and we haven't created the node yet,
            // create it
            if (!lazilyCreateIndication && indicationNode == null) isIndicationNodeDirty = true
        }
        // Create / recreate indication node
        if (isIndicationNodeDirty) {
            recreateIndicationIfNeeded()
        }
        focusableNode.update(this.interactionSource)
    }

    final override fun onAttach() {
        onObservedReadsChanged()
        if (!lazilyCreateIndication) {
            initializeIndicationAndInteractionSourceIfNeeded()
        }
        if (enabled) {
            delegate(focusableNode)
        }
    }

    override fun onObservedReadsChanged() {
        if (useLocalIndication) {
            observeReads {
                val indication = currentValueOf(LocalIndication)
                requirePrecondition(indication is IndicationNodeFactory) {
                    unsupportedIndicationExceptionMessage(indication)
                }
                val previousFactory = localIndicationNodeFactory
                localIndicationNodeFactory = indication
                // If we are changing from a non-null factory to a different factory, recreate
                // indication if needed
                if (previousFactory != null && localIndicationNodeFactory != previousFactory) {
                    recreateIndicationIfNeeded()
                }
            }
        }
    }

    final override fun onDetach() {
        disposeInteractions()
        // If we lazily created an interaction source, reset it in case we are reused / moved. Note
        // that we need to do it here instead of onReset() - since onReset won't be called in the
        // movableContent case but we still want to dispose for that case
        if (userProvidedInteractionSource == null) {
            interactionSource = null
        }
        // Remove indication in case we are reused / moved - we will create a new node when needed
        indicationNode?.let { undelegate(it) }
        indicationNode = null
    }

    protected fun disposeInteractions() {
        interactionSource?.let { interactionSource ->
            pressInteraction?.let { oldValue ->
                val interaction = PressInteraction.Cancel(oldValue)
                interactionSource.tryEmit(interaction)
            }
            hoverInteraction?.let { oldValue ->
                val interaction = HoverInteraction.Exit(oldValue)
                interactionSource.tryEmit(interaction)
            }
            currentKeyPressInteractions.forEachValue {
                interactionSource.tryEmit(PressInteraction.Cancel(it))
            }
        }
        pressInteraction = null
        hoverInteraction = null
        currentKeyPressInteractions.clear()
    }

    private fun onFocusChange(isFocused: Boolean) {
        if (isFocused) {
            initializeIndicationAndInteractionSourceIfNeeded()
        } else {
            // If we are no longer focused while we are tracking existing key presses, we need to
            // clear them and cancel the presses.
            if (interactionSource != null) {
                currentKeyPressInteractions.forEachValue {
                    coroutineScope.launch { interactionSource?.emit(PressInteraction.Cancel(it)) }
                }
            }
            currentKeyPressInteractions.clear()
            onCancelKeyInput()
        }
    }

    private fun recreateIndicationIfNeeded() {
        // If we already created a node lazily, or we are not lazily creating the node, create
        if (indicationNode != null || !lazilyCreateIndication) {
            indicationNode?.let { undelegate(it) }
            indicationNode = null
            initializeIndicationAndInteractionSourceIfNeeded()
        }
    }

    private fun initializeIndicationAndInteractionSourceIfNeeded() {
        // We have already created the node, no need to do any work
        if (indicationNode != null) return
        val indicationFactory =
            if (useLocalIndication) localIndicationNodeFactory else indicationNodeFactory
        indicationFactory?.let { factory ->
            if (interactionSource == null) {
                interactionSource = MutableInteractionSource()
            }
            focusableNode.update(interactionSource)
            val node = factory.create(interactionSource!!)
            delegate(node)
            indicationNode = node
        }
    }

    override fun onPointerEvent(
        pointerEvent: PointerEvent,
        pass: PointerEventPass,
        bounds: IntSize,
    ) {
        centerOffset = bounds.center.toOffset()
        initializeIndicationAndInteractionSourceIfNeeded()
        if (enabled) {
            if (pass == PointerEventPass.Main) {
                when (pointerEvent.type) {
                    PointerEventType.Enter -> coroutineScope.launch { emitHoverEnter() }
                    PointerEventType.Exit -> coroutineScope.launch { emitHoverExit() }
                }
            }
        }
        if (pointerInputNode == null) {
            val node = createPointerInputNodeIfNeeded()
            if (node != null) {
                pointerInputNode = delegate(node)
            }
        }
        pointerInputNode?.onPointerEvent(pointerEvent, pass, bounds)
    }

    override fun onCancelPointerInput() {
        // Press cancellation is handled as part of detecting presses
        interactionSource?.let { interactionSource ->
            hoverInteraction?.let { oldValue ->
                val interaction = HoverInteraction.Exit(oldValue)
                interactionSource.tryEmit(interaction)
            }
        }
        hoverInteraction = null
        pointerInputNode?.onCancelPointerInput()
    }

    final override fun onKeyEvent(event: KeyEvent): Boolean {
        // Key events usually require focus, but if a focused child does not handle the KeyEvent,
        // the event can bubble up without this clickable ever being focused, and hence without
        // this being initialized through the focus path
        initializeIndicationAndInteractionSourceIfNeeded()
        val keyCode = event.key.keyCode
        return when {
            enabled && event.isPress -> {
                // If the key already exists in the map, keyEvent is a repeat event.
                // We ignore it as we only want to emit an interaction for the initial key press.
                var wasInteractionHandled = false
                if (!currentKeyPressInteractions.containsKey(keyCode)) {
                    val press = PressInteraction.Press(centerOffset)
                    currentKeyPressInteractions[keyCode] = press
                    // Even if the interactionSource is null, we still want to intercept the presses
                    // so we always track them above, and return true
                    if (interactionSource != null) {
                        coroutineScope.launch { interactionSource?.emit(press) }
                    }
                    wasInteractionHandled = true
                }
                onClickKeyDownEvent(event) || wasInteractionHandled
            }
            enabled && event.isClick -> {
                val press = currentKeyPressInteractions.remove(keyCode)
                if (press != null) {
                    if (interactionSource != null) {
                        coroutineScope.launch {
                            interactionSource?.emit(PressInteraction.Release(press))
                        }
                    }
                    // Don't invoke onClick if we were not pressed - this could happen if we became
                    // focused after the down event, or if the node was reused after the down event.
                    onClickKeyUpEvent(event)
                }
                // Only consume if we were previously pressed for this key event
                press != null
            }
            else -> false
        }
    }

    protected abstract fun onClickKeyDownEvent(event: KeyEvent): Boolean

    protected abstract fun onClickKeyUpEvent(event: KeyEvent): Boolean

    /**
     * Called when focus is lost, to allow cleaning up and resetting the state for ongoing key
     * presses
     */
    protected open fun onCancelKeyInput() {}

    final override fun onPreKeyEvent(event: KeyEvent) = false

    final override val shouldMergeDescendantSemantics: Boolean
        get() = true

    final override fun SemanticsPropertyReceiver.applySemantics() {
        if (this@AbstractClickableNode.role != null) {
            role = this@AbstractClickableNode.role!!
        }
        onClick(
            action = {
                onClick()
                true
            },
            label = onClickLabel,
        )
        if (enabled) {
            with(focusableNode) { applySemantics() }
        } else {
            disabled()
        }
        applyAdditionalSemantics()
    }

    protected fun resetPointerInputHandler() = pointerInputNode?.resetPointerInputHandler()

    private var delayJob: Job? = null

    protected fun handlePressInteractionStart(offset: Offset) {
        interactionSource?.let { interactionSource ->
            val press = PressInteraction.Press(offset)
            if (delayPressInteraction()) {
                delayJob =
                    coroutineScope.launch {
                        delay(TapIndicationDelay)
                        interactionSource.emit(press)
                        pressInteraction = press
                    }
            } else {
                pressInteraction = press
                coroutineScope.launch { interactionSource.emit(press) }
            }
        }
    }

    protected fun handlePressInteractionRelease(offset: Offset) {
        interactionSource?.let { interactionSource ->
            if (delayJob?.isActive == true) {
                coroutineScope.launch {
                    delayJob?.cancelAndJoin()
                    // The press released successfully, before the timeout duration - emit the press
                    // interaction instantly.
                    val press = PressInteraction.Press(offset)
                    val release = PressInteraction.Release(press)
                    interactionSource.emit(press)
                    interactionSource.emit(release)
                }
            } else {
                pressInteraction?.let { pressInteraction ->
                    coroutineScope.launch {
                        val endInteraction = PressInteraction.Release(pressInteraction)
                        interactionSource.emit(endInteraction)
                    }
                }
            }
            pressInteraction = null
        }
    }

    protected fun handlePressInteractionCancel() {
        interactionSource?.let { interactionSource ->
            if (delayJob?.isActive == true) {
                // We didn't finish sending the press, and we are cancelled, so we don't emit
                // any interaction.
                delayJob?.cancel()
            } else {
                pressInteraction?.let { pressInteraction ->
                    coroutineScope.launch {
                        val endInteraction = PressInteraction.Cancel(pressInteraction)
                        interactionSource.emit(endInteraction)
                    }
                }
            }
            pressInteraction = null
        }
    }

    protected suspend fun PressGestureScope.handlePressInteraction(offset: Offset) {
        interactionSource?.let { interactionSource ->
            coroutineScope {
                val delayJob = launch {
                    if (delayPressInteraction()) {
                        delay(TapIndicationDelay)
                    }
                    val press = PressInteraction.Press(offset)
                    interactionSource.emit(press)
                    pressInteraction = press
                }
                val success = tryAwaitRelease()
                if (delayJob.isActive) {
                    delayJob.cancelAndJoin()
                    // The press released successfully, before the timeout duration - emit the press
                    // interaction instantly. No else branch - if the press was cancelled before the
                    // timeout, we don't want to emit a press interaction.
                    if (success) {
                        val press = PressInteraction.Press(offset)
                        val release = PressInteraction.Release(press)
                        interactionSource.emit(press)
                        interactionSource.emit(release)
                    }
                } else {
                    pressInteraction?.let { pressInteraction ->
                        val endInteraction =
                            if (success) {
                                PressInteraction.Release(pressInteraction)
                            } else {
                                PressInteraction.Cancel(pressInteraction)
                            }
                        interactionSource.emit(endInteraction)
                    }
                }
                pressInteraction = null
            }
        }
    }

    private fun delayPressInteraction(): Boolean =
        hasScrollableContainer() || isComposeRootInScrollableContainer()

    private fun emitHoverEnter() {
        if (hoverInteraction == null) {
            val interaction = HoverInteraction.Enter()
            interactionSource?.let { interactionSource ->
                coroutineScope.launch { interactionSource.emit(interaction) }
            }
            hoverInteraction = interaction
        }
    }

    private fun emitHoverExit() {
        hoverInteraction?.let { oldValue ->
            val interaction = HoverInteraction.Exit(oldValue)
            interactionSource?.let { interactionSource ->
                coroutineScope.launch { interactionSource.emit(interaction) }
            }
            hoverInteraction = null
        }
    }

    override val traverseKey: Any = TraverseKey

    companion object TraverseKey
}

internal fun TraversableNode.hasScrollableContainer(): Boolean {
    var hasScrollable = false
    traverseAncestors(ScrollableContainerNode.TraverseKey) { node ->
        hasScrollable = hasScrollable || (node as ScrollableContainerNode).enabled
        !hasScrollable
    }
    return hasScrollable
}

private fun unsupportedIndicationExceptionMessage(indication: Indication): String {
    return "clickable only supports IndicationNodeFactory instances provided to LocalIndication, " +
        "but Indication was provided instead. Either migrate the Indication implementation to " +
        "implement IndicationNodeFactory, or use the other clickable overload that takes an " +
        "Indication parameter, and explicitly pass LocalIndication.current there. You can also " +
        "use ComposeFoundationFlags.isNonComposedClickableEnabled to temporarily opt-out; note " +
        "that this flag will be removed in a future release and is only intended to be a " +
        "temporary migration aid. The Indication instance provided here was: $indication"
}
