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

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.graphics.ExperimentalAnimationGraphicsApi
import androidx.compose.animation.graphics.res.animatedVectorResource
import androidx.compose.animation.graphics.res.rememberAnimatedVectorPainter
import androidx.compose.animation.graphics.vector.AnimatedImageVector
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalAccessibilityManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.wear.compose.foundation.CurvedDirection
import androidx.wear.compose.foundation.CurvedLayout
import androidx.wear.compose.foundation.CurvedModifier
import androidx.wear.compose.foundation.CurvedScope
import androidx.wear.compose.foundation.CurvedTextStyle
import androidx.wear.compose.foundation.padding
import androidx.wear.compose.material3.MotionScheme.Companion.expressive
import androidx.wear.compose.material3.MotionScheme.Companion.standard
import androidx.wear.compose.material3.tokens.ColorSchemeKeyTokens
import androidx.wear.compose.material3.tokens.MotionTokens
import androidx.wear.compose.material3.tokens.MotionTokens.DurationLong2
import androidx.wear.compose.material3.tokens.MotionTokens.DurationShort2
import androidx.wear.compose.material3.tokens.MotionTokens.DurationShort3
import androidx.wear.compose.material3.tokens.ShapeTokens
import androidx.wear.compose.materialcore.screenHeightDp
import androidx.wear.compose.materialcore.screenHeightPx
import androidx.wear.compose.materialcore.screenWidthDp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Shows a [Confirmation] dialog with an icon and optional very short curved text. The length of the
 * curved text should be very short and should not exceed 1-2 words. If a longer text required, then
 * another [Confirmation] overload with a column content should be used instead.
 *
 * The confirmation will be showing a message to the user for [durationMillis]. After a specified
 * timeout, the [onDismissRequest] callback will be invoked, where it's up to the caller to handle
 * the dismissal. To hide the confirmation, [show] parameter should be set to false.
 *
 * Example of a [Confirmation] with an icon and a curved text content:
 *
 * @sample androidx.wear.compose.material3.samples.ConfirmationSample
 * @param show A boolean indicating whether the confirmation should be displayed.
 * @param onDismissRequest A lambda function to be called when the dialog is dismissed - either by
 *   swiping right or when the [durationMillis] has passed.
 * @param curvedText A slot for displaying curved text content which will be shown along the bottom
 *   edge of the dialog.
 * @param modifier Modifier to be applied to the confirmation content.
 * @param colors A [ConfirmationColors] object for customizing the colors used in this
 *   [Confirmation].
 * @param properties An optional [DialogProperties] object for configuring the dialog's behavior.
 * @param durationMillis The duration in milliseconds for which the dialog is displayed. Defaults to
 *   [ConfirmationDefaults.ConfirmationDurationMillis].
 * @param content A slot for displaying an icon inside the confirmation dialog. It's recommended to
 *   set its size to [ConfirmationDefaults.IconSize]
 */
@Composable
fun Confirmation(
    show: Boolean,
    onDismissRequest: () -> Unit,
    curvedText: (CurvedScope.() -> Unit)?,
    modifier: Modifier = Modifier,
    colors: ConfirmationColors = ConfirmationDefaults.confirmationColors(),
    properties: DialogProperties = DialogProperties(),
    durationMillis: Long = ConfirmationDefaults.ConfirmationDurationMillis,
    content: @Composable BoxScope.() -> Unit
) {
    ConfirmationImpl(
        show = show,
        performHapticFeedback = null,
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        curvedText = curvedText,
        colors = colors,
        properties = properties,
        durationMillis = durationMillis,
    ) {
        IconContainer(
            iconColor = colors.iconColor,
            iconBackground = confirmationIconContainer(true, colors.iconContainerColor),
            content = content
        )
    }
}

@Composable
private fun BoxScope.IconContainer(
    modifier: Modifier = Modifier,
    iconColor: Color,
    iconBackground: @Composable BoxScope.() -> Unit,
    content: @Composable BoxScope.() -> Unit
) {
    Box(modifier.align(Alignment.Center), contentAlignment = Alignment.Center) {
        iconBackground()
        CompositionLocalProvider(LocalContentColor provides iconColor) { content() }
    }
}

/**
 * Shows a [Confirmation] dialog with an icon and optional short text. The length of the text should
 * not exceed 3 lines. If the text is very short and fits into 1-2 words, consider using another
 * [Confirmation] overload with curvedContent instead.
 *
 * The confirmation will show a message to the user for [durationMillis]. After a specified timeout,
 * the [onDismissRequest] callback will be invoked, where it's up to the caller to handle the
 * dismissal. To hide the confirmation, [show] parameter should be set to false.
 *
 * Example of a [Confirmation] with an icon and a text which fits into 3 lines:
 *
 * @sample androidx.wear.compose.material3.samples.LongTextConfirmationSample
 * @param show A boolean indicating whether the confirmation should be displayed.
 * @param onDismissRequest A lambda function to be called when the dialog is dismissed - either by
 *   swiping right or when the [durationMillis] has passed.
 * @param text A slot for displaying text below the icon. It should not exceed 3 lines.
 * @param modifier Modifier to be applied to the confirmation content.
 * @param colors A [ConfirmationColors] object for customizing the colors used in this
 *   [Confirmation].
 * @param properties An optional [DialogProperties] object for configuring the dialog's behavior.
 * @param durationMillis The duration in milliseconds for which the dialog is displayed. Defaults to
 *   [ConfirmationDefaults.ConfirmationDurationMillis].
 * @param content A slot for displaying an icon inside the confirmation dialog, which can be
 *   animated. It's recommended to set its size to [ConfirmationDefaults.SmallIconSize]
 */
@Composable
fun Confirmation(
    show: Boolean,
    onDismissRequest: () -> Unit,
    text: @Composable (ColumnScope.() -> Unit)?,
    modifier: Modifier = Modifier,
    colors: ConfirmationColors = ConfirmationDefaults.confirmationColors(),
    properties: DialogProperties = DialogProperties(),
    durationMillis: Long = ConfirmationDefaults.ConfirmationDurationMillis,
    content: @Composable BoxScope.() -> Unit
) {

    val a11yDurationMillis =
        LocalAccessibilityManager.current?.calculateRecommendedTimeoutMillis(
            originalTimeoutMillis = durationMillis,
            containsIcons = true,
            containsText = text != null,
            containsControls = false,
        ) ?: durationMillis

    LaunchedEffect(show, a11yDurationMillis) {
        if (show) {
            delay(a11yDurationMillis)
            onDismissRequest()
        }
    }

    Dialog(
        show = show,
        modifier = modifier,
        onDismissRequest = onDismissRequest,
        properties = properties,
    ) {
        val textTransitionStart = screenHeightPx() * ConfirmationTextTransitionFraction

        val translationYAnimatable = remember { Animatable(textTransitionStart) }
        val alphaAnimatable = remember { Animatable(0f) }

        LaunchedEffect(Unit) {
            delay(DurationShort3.toLong())
            launch { translationYAnimatable.animateTo(0f, ConfirmationTranslationSpec) }
            alphaAnimatable.animateTo(1f, AlphaAnimationSpec)
        }

        Box(Modifier.fillMaxSize()) {
            val horizontalPadding = screenWidthDp().dp * HorizontalLinearContentPaddingFraction
            Column(
                modifier = Modifier.align(Alignment.Center).padding(horizontal = horizontalPadding),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    contentAlignment = Alignment.Center
                ) {
                    confirmationIconContainer(false, colors.iconContainerColor)()
                    CompositionLocalProvider(LocalContentColor provides colors.iconColor) {
                        content()
                    }
                }
                CompositionLocalProvider(
                    LocalContentColor provides colors.textColor,
                    LocalTextStyle provides MaterialTheme.typography.titleMedium,
                    LocalTextConfiguration provides
                        TextConfiguration(
                            textAlign = TextAlign.Center,
                            maxLines = LinearContentMaxLines,
                            overflow = TextOverflow.Ellipsis
                        ),
                ) {
                    if (text != null) {
                        Spacer(Modifier.height(LinearContentSpacing))
                        Column(
                            modifier =
                                Modifier.fillMaxWidth().graphicsLayer {
                                    translationY = translationYAnimatable.value
                                    alpha = alphaAnimatable.value
                                },
                            horizontalAlignment = Alignment.CenterHorizontally,
                            content = text
                        )
                        Spacer(Modifier.height(LinearContentSpacing))
                    }
                }
            }
        }
    }
}

/**
 * Shows a [Confirmation] dialog with a success icon and optional short curved text. This
 * confirmation indicates a successful operation or action.
 *
 * The confirmation will show a message to the user for [durationMillis]. After a specified timeout,
 * the [onDismissRequest] callback will be invoked, where it's up to the caller to handle the
 * dismissal. To hide the confirmation, [show] parameter should be set to false.
 *
 * Example of a [SuccessConfirmation] usage:
 *
 * @sample androidx.wear.compose.material3.samples.SuccessConfirmationSample
 * @param show A boolean indicating whether the confirmation should be displayed.
 * @param onDismissRequest A lambda function to be called when the dialog is dismissed - either by
 *   swiping right or when the [durationMillis] has passed.
 * @param modifier Modifier to be applied to the confirmation content.
 * @param curvedText A slot for displaying curved text content which will be shown along the bottom
 *   edge of the dialog. Defaults to a localized success message.
 * @param colors A [ConfirmationColors] object for customizing the colors used in this
 *   [SuccessConfirmation].
 * @param properties An optional [DialogProperties] object for configuring the dialog's behavior.
 * @param durationMillis The duration in milliseconds for which the dialog is displayed. Defaults to
 *   [ConfirmationDefaults.ConfirmationDurationMillis].
 * @param content A slot for displaying an icon inside the confirmation dialog, which can be
 *   animated. Defaults to an animated [ConfirmationDefaults.SuccessIcon].
 */
@Composable
fun SuccessConfirmation(
    show: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    curvedText: (CurvedScope.() -> Unit)? = ConfirmationDefaults.successText(),
    colors: ConfirmationColors = ConfirmationDefaults.successColors(),
    properties: DialogProperties = DialogProperties(),
    durationMillis: Long = ConfirmationDefaults.ConfirmationDurationMillis,
    content: @Composable BoxScope.() -> Unit = ConfirmationDefaults.SuccessIcon,
) {
    val hapticFeedback = LocalHapticFeedback.current
    ConfirmationImpl(
        show = show,
        performHapticFeedback = {
            hapticFeedback.performHapticFeedback(HapticFeedbackType.Confirm)
        },
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        curvedText = curvedText,
        colors = colors,
        properties = properties,
        durationMillis = durationMillis
    ) {
        IconContainer(
            modifier = Modifier.fillMaxSize(),
            iconColor = colors.iconColor,
            iconBackground = successIconContainer(colors.iconContainerColor),
            content = content
        )
    }
}

/**
 * Shows a [Confirmation] dialog with a failure icon and an optional short curved text. This
 * confirmation indicates an unsuccessful operation or action.
 *
 * The confirmation will show a message to the user for [durationMillis]. After a specified timeout,
 * the [onDismissRequest] callback will be invoked, where it's up to the caller to handle the
 * dismissal. To hide the confirmation, [show] parameter should be set to false.
 *
 * Example of a [FailureConfirmation] usage:
 *
 * @sample androidx.wear.compose.material3.samples.FailureConfirmationSample
 * @param show A boolean indicating whether the confirmation should be displayed.
 * @param onDismissRequest A lambda function to be called when the dialog is dismissed - either by
 *   swiping right or when the [durationMillis] has passed.
 * @param modifier Modifier to be applied to the confirmation content.
 * @param curvedText A slot for displaying curved text content which will be shown along the bottom
 *   edge of the dialog. Defaults to a localized failure message.
 * @param colors A [ConfirmationColors] object for customizing the colors used in this
 *   [FailureConfirmation].
 * @param properties An optional [DialogProperties] object for configuring the dialog's behavior.
 * @param durationMillis The duration in milliseconds for which the dialog is displayed. Defaults to
 *   [ConfirmationDefaults.ConfirmationDurationMillis].
 * @param content A slot for displaying an icon inside the confirmation dialog, which can be
 *   animated. Defaults to [ConfirmationDefaults.FailureIcon].
 */
@Composable
fun FailureConfirmation(
    show: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    curvedText: (CurvedScope.() -> Unit)? = ConfirmationDefaults.failureText(),
    colors: ConfirmationColors = ConfirmationDefaults.failureColors(),
    properties: DialogProperties = DialogProperties(),
    durationMillis: Long = ConfirmationDefaults.ConfirmationDurationMillis,
    content: @Composable BoxScope.() -> Unit = ConfirmationDefaults.FailureIcon,
) {
    val hapticFeedback = LocalHapticFeedback.current
    ConfirmationImpl(
        show = show,
        performHapticFeedback = { hapticFeedback.performHapticFeedback(HapticFeedbackType.Reject) },
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        curvedText = curvedText,
        colors = colors,
        properties = properties,
        durationMillis = durationMillis,
    ) {
        val translationXAnimatable = remember { Animatable(FailureContentTransition[0]) }
        LaunchedEffect(Unit) {
            delay(DurationShort3.toLong())
            translationXAnimatable.animateTo(
                FailureContentTransition[1],
                FailureContentAnimationSpecs[0]
            )
            translationXAnimatable.animateTo(
                FailureContentTransition[2],
                FailureContentAnimationSpecs[1]
            )
        }
        IconContainer(
            modifier = Modifier.graphicsLayer { translationX = translationXAnimatable.value },
            iconColor = colors.iconColor,
            iconBackground = failureIconContainer(colors.iconContainerColor),
            content = content
        )
    }
}

/** Contains default values used by [Confirmation] composable. */
object ConfirmationDefaults {

    /**
     * Returns a lambda to display a curved success message. The success message is retrieved from
     * the application's string resources.
     */
    @Composable
    fun successText(): CurvedScope.() -> Unit =
        curvedText(
            LocalContext.current.resources.getString(R.string.wear_m3c_confirmation_success_message)
        )

    /**
     * Returns a lambda to display a curved failure message. The failure message is retrieved from
     * the application's string resources.
     */
    @Composable
    fun failureText(): CurvedScope.() -> Unit =
        curvedText(
            LocalContext.current.resources.getString(R.string.wear_m3c_confirmation_failure_message)
        )

    /**
     * A default composable used in [SuccessConfirmation] that displays a success icon with an
     * animation.
     */
    @OptIn(ExperimentalAnimationGraphicsApi::class)
    val SuccessIcon: @Composable BoxScope.() -> Unit = {
        val animation =
            AnimatedImageVector.animatedVectorResource(R.drawable.wear_m3c_check_animation)
        var atEnd by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) {
            delay(IconDelay)
            atEnd = true
        }
        Icon(
            painter = rememberAnimatedVectorPainter(animation, atEnd),
            contentDescription = null,
            modifier = Modifier.size(IconSize)
        )
    }

    /**
     * A default composable used in [FailureConfirmation] that displays a failure icon with an
     * animation.
     */
    @OptIn(ExperimentalAnimationGraphicsApi::class)
    val FailureIcon: @Composable BoxScope.() -> Unit = {
        val animation =
            AnimatedImageVector.animatedVectorResource(R.drawable.wear_m3c_failure_animation)
        var atEnd by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) {
            delay(IconDelay)
            atEnd = true
        }
        Icon(
            painter = rememberAnimatedVectorPainter(animation, atEnd),
            contentDescription = null,
            modifier = Modifier.size(IconSize)
        )
    }

    /**
     * A default composable that displays text along a curved path, used in [Confirmation].
     *
     * @param text The text to display.
     * @param style The style to apply to the text. Defaults to
     *   CurvedTextStyle(MaterialTheme.typography.titleLarge).
     */
    @Composable
    fun curvedText(
        text: String,
        style: CurvedTextStyle = CurvedTextStyle(MaterialTheme.typography.titleLarge)
    ): CurvedScope.() -> Unit = {
        curvedText(
            text = text,
            style = style,
            maxSweepAngle = CurvedTextDefaults.StaticContentMaxSweepAngle,
            modifier = CurvedModifier.padding(PaddingDefaults.edgePadding),
            angularDirection = CurvedDirection.Angular.Reversed
        )
    }

    /**
     * Creates a [ConfirmationColors] that represents the default colors used in a [Confirmation].
     */
    @Composable fun confirmationColors() = MaterialTheme.colorScheme.defaultConfirmationColors

    /**
     * Creates a [ConfirmationColors] with modified colors used in [Confirmation].
     *
     * @param iconColor The icon color.
     * @param iconContainerColor The icon container color.
     * @param textColor The text color.
     */
    @Composable
    fun confirmationColors(
        iconColor: Color = Color.Unspecified,
        iconContainerColor: Color = Color.Unspecified,
        textColor: Color = Color.Unspecified,
    ) =
        MaterialTheme.colorScheme.defaultConfirmationColors.copy(
            iconColor = iconColor,
            iconContainerColor = iconContainerColor,
            textColor = textColor,
        )

    /**
     * Creates a [ConfirmationColors] that represents the default colors used in a
     * [SuccessConfirmation].
     */
    @Composable fun successColors() = MaterialTheme.colorScheme.defaultSuccessConfirmationColors

    /**
     * Creates a [ConfirmationColors] with modified colors used in [SuccessConfirmation].
     *
     * @param iconColor The icon color.
     * @param iconContainerColor The icon container color.
     * @param textColor The text color.
     */
    @Composable
    fun successColors(
        iconColor: Color = Color.Unspecified,
        iconContainerColor: Color = Color.Unspecified,
        textColor: Color = Color.Unspecified,
    ) =
        MaterialTheme.colorScheme.defaultSuccessConfirmationColors.copy(
            iconColor = iconColor,
            iconContainerColor = iconContainerColor,
            textColor = textColor,
        )

    /**
     * Creates a [ConfirmationColors] that represents the default colors used in a
     * [FailureConfirmation].
     */
    @Composable fun failureColors() = MaterialTheme.colorScheme.defaultFailureConfirmationColors

    /**
     * Creates a [ConfirmationColors] with modified colors used in [FailureConfirmation].
     *
     * @param iconColor The icon color.
     * @param iconContainerColor The icon container color.
     * @param textColor The text color.
     */
    @Composable
    fun failureColors(
        iconColor: Color = Color.Unspecified,
        iconContainerColor: Color = Color.Unspecified,
        textColor: Color = Color.Unspecified,
    ) =
        MaterialTheme.colorScheme.defaultFailureConfirmationColors.copy(
            iconColor = iconColor,
            iconContainerColor = iconContainerColor,
            textColor = textColor,
        )

    /** Default timeout for the [Confirmation] dialog, in milliseconds. */
    const val ConfirmationDurationMillis = 4000L

    /** Default icon size for the [Confirmation] with curved content */
    val IconSize = 52.dp

    /** Default icon size for the [Confirmation] with linear content */
    val SmallIconSize = 36.dp

    private val ColorScheme.defaultConfirmationColors: ConfirmationColors
        get() {
            return defaultConfirmationColorsCached
                ?: ConfirmationColors(
                        iconColor = fromToken(ColorSchemeKeyTokens.Primary),
                        iconContainerColor = fromToken(ColorSchemeKeyTokens.OnPrimary),
                        textColor = fromToken(ColorSchemeKeyTokens.OnBackground)
                    )
                    .also { defaultConfirmationColorsCached = it }
        }

    private val ColorScheme.defaultSuccessConfirmationColors: ConfirmationColors
        get() {
            return defaultSuccessConfirmationColorsCached
                ?: ConfirmationColors(
                        iconColor = fromToken(ColorSchemeKeyTokens.Primary),
                        iconContainerColor = fromToken(ColorSchemeKeyTokens.OnPrimary),
                        textColor = fromToken(ColorSchemeKeyTokens.OnBackground)
                    )
                    .also { defaultSuccessConfirmationColorsCached = it }
        }

    private val ColorScheme.defaultFailureConfirmationColors: ConfirmationColors
        get() {
            return defaultFailureConfirmationColorsCached
                ?: ConfirmationColors(
                        iconColor = fromToken(ColorSchemeKeyTokens.ErrorContainer),
                        iconContainerColor =
                            fromToken(ColorSchemeKeyTokens.OnErrorContainer).copy(.8f),
                        textColor = fromToken(ColorSchemeKeyTokens.OnBackground)
                    )
                    .also { defaultFailureConfirmationColorsCached = it }
        }

    private val IconDelay = DurationShort2.toLong()
}

/**
 * Represents the colors used in [Confirmation], [SuccessConfirmation] and [FailureConfirmation].
 *
 * @param iconColor Color used to tint the icon.
 * @param iconContainerColor The color of the container behind the icon.
 * @param textColor Color used to tint the text.
 */
class ConfirmationColors(
    val iconColor: Color,
    val iconContainerColor: Color,
    val textColor: Color,
) {
    /**
     * Returns a copy of this ConfirmationColors, optionally overriding some of the values.
     *
     * @param iconColor Color used to tint the icon.
     * @param iconContainerColor The color of the container behind the icon.
     * @param textColor Color used to tint the text.
     */
    fun copy(
        iconColor: Color = this.iconColor,
        iconContainerColor: Color = this.iconContainerColor,
        textColor: Color = this.textColor
    ) =
        ConfirmationColors(
            iconColor = iconColor.takeOrElse { this.iconColor },
            iconContainerColor = iconContainerColor.takeOrElse { this.iconContainerColor },
            textColor = textColor.takeOrElse { this.textColor },
        )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || other !is ConfirmationColors) return false

        if (iconColor != other.iconColor) return false
        if (iconContainerColor != other.iconContainerColor) return false
        if (textColor != other.textColor) return false

        return true
    }

    override fun hashCode(): Int {
        var result = iconColor.hashCode()
        result = 31 * result + iconContainerColor.hashCode()
        result = 31 * result + textColor.hashCode()
        return result
    }
}

@Composable
internal fun ConfirmationImpl(
    show: Boolean,
    performHapticFeedback: (() -> Unit)?,
    onDismissRequest: () -> Unit,
    modifier: Modifier,
    curvedText: (CurvedScope.() -> Unit)?,
    colors: ConfirmationColors,
    properties: DialogProperties,
    durationMillis: Long,
    content: @Composable BoxScope.() -> Unit,
) {
    val a11yDurationMillis =
        LocalAccessibilityManager.current?.calculateRecommendedTimeoutMillis(
            originalTimeoutMillis = durationMillis,
            containsIcons = true,
            containsText = curvedText != null,
            containsControls = false,
        ) ?: durationMillis

    val alphaAnimatable = remember(show) { Animatable(0f) }

    LaunchedEffect(show, a11yDurationMillis) {
        if (show) {
            performHapticFeedback?.invoke()
            launch {
                delay(DurationShort3.toLong())
                alphaAnimatable.animateTo(1f, AlphaAnimationSpec)
            }
            delay(a11yDurationMillis)
            onDismissRequest()
        }
    }

    Dialog(
        show = show,
        modifier = modifier,
        onDismissRequest = onDismissRequest,
        properties = properties,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            content()
            CompositionLocalProvider(LocalContentColor provides colors.textColor) {
                curvedText?.let {
                    CurvedLayout(
                        modifier = Modifier.graphicsLayer { alpha = alphaAnimatable.value },
                        anchor = 90f,
                        contentBuilder = curvedText
                    )
                }
            }
        }
    }
}

private fun confirmationIconContainer(
    curvedContent: Boolean,
    color: Color
): @Composable BoxScope.() -> Unit = {
    val width =
        if (curvedContent) {
            (screenWidthDp() * ConfirmationSizeFraction).dp
        } else ConfirmationLinearIconContainerSize

    val startShape = ShapeTokens.CornerFull
    val targetShape =
        (if (curvedContent) MaterialTheme.shapes.extraLarge else MaterialTheme.shapes.large)
            as RoundedCornerShape

    val rotateAnimatable = remember { Animatable(ConfirmationIconInitialAngle) }
    val shapeAnimatable = remember { Animatable(0f) }
    val shape =
        remember(shapeAnimatable) {
            AnimatedRoundedCornerShape(startShape, targetShape) { shapeAnimatable.value }
        }

    LaunchedEffect(Unit) {
        delay(DurationShort3.toLong())
        launch { shapeAnimatable.animateTo(1f, ContainerAnimationSpec) }
        rotateAnimatable.animateTo(0f, StandardDecelerateSpec)
    }

    Box(
        Modifier.size(width)
            .graphicsLayer {
                this.shape = shape
                rotationZ = rotateAnimatable.value
                clip = true
            }
            .background(color)
            .align(Alignment.Center)
    )
}

private fun successIconContainer(color: Color): @Composable BoxScope.() -> Unit = {
    val width = screenWidthDp() * SuccessWidthFraction

    val targetHeight = screenHeightDp() * SuccessHeightFraction.toFloat()
    val heightAnimatable = remember { Animatable(width) }

    LaunchedEffect(Unit) {
        delay(DurationShort3.toLong())
        heightAnimatable.animateTo(targetHeight, ContainerAnimationSpec)
    }
    Box(
        Modifier.size(width.dp, heightAnimatable.value.dp)
            .graphicsLayer {
                rotationZ = 45f
                shape = CircleShape
                clip = true
            }
            .background(color)
    )
}

private fun failureIconContainer(color: Color): @Composable BoxScope.() -> Unit = {
    val size = screenWidthDp() * FailureSizeFraction

    val startShape = ShapeTokens.CornerFull
    val targetShape = MaterialTheme.shapes.extraLarge as RoundedCornerShape
    val shapeAnimatable = remember { Animatable(0f) }
    val shape =
        remember(shapeAnimatable) {
            AnimatedRoundedCornerShape(startShape, targetShape) { shapeAnimatable.value }
        }

    LaunchedEffect(Unit) {
        delay(DurationShort3.toLong())
        shapeAnimatable.animateTo(1f, ContainerAnimationSpec)
    }

    Box(
        Modifier.size(size.dp)
            .graphicsLayer {
                this.shape = shape
                clip = true
            }
            .background(color)
    )
}

internal val ConfirmationLinearIconContainerSize = 80.dp
internal val LinearContentSpacing = 8.dp

private const val SuccessWidthPaddingFraction = 0.2315f
private const val SuccessHeightPaddingFraction = 0.176
private const val SuccessWidthFraction = 1 - SuccessWidthPaddingFraction * 2
private const val SuccessHeightFraction = 1 - SuccessHeightPaddingFraction * 2

private const val FailureSizePaddingFraction = 0.213f
private const val FailureSizeFraction = 1 - FailureSizePaddingFraction * 2

private const val ConfirmationSizePaddingFraction = 0.213f
private const val ConfirmationSizeFraction = 1 - ConfirmationSizePaddingFraction * 2

private const val LinearContentMaxLines = 3
private const val HorizontalLinearContentPaddingFraction = 0.12f

private const val ConfirmationTextTransitionFraction = 0.015f
private const val ConfirmationIconInitialAngle = -45f

private val FailureContentTransition = arrayOf(-15f, -20f, 0f)
private val FailureContentAnimationSpecs =
    arrayOf(
        spring(
            dampingRatio = ExpressiveDefaultDamping,
            stiffness = ExpressiveDefaultStiffness,
            visibilityThreshold = 0f
        ),
        spring(
            dampingRatio = 0.5f,
            stiffness = ExpressiveDefaultStiffness,
        )
    )
private val AlphaAnimationSpec: AnimationSpec<Float> = standard().fastEffectsSpec()
private val ConfirmationTranslationSpec: AnimationSpec<Float> = standard().slowSpatialSpec()
private val ContainerAnimationSpec: AnimationSpec<Float> = expressive().defaultSpatialSpec()
private val StandardDecelerateSpec: AnimationSpec<Float> =
    tween(DurationLong2, easing = MotionTokens.EasingStandardDecelerate)
