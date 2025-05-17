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

package androidx.wear.protolayout.material3

import android.graphics.Color
import androidx.annotation.Dimension
import androidx.annotation.Dimension.Companion.DP
import androidx.annotation.FloatRange
import androidx.wear.protolayout.DimensionBuilders.ContainerDimension
import androidx.wear.protolayout.DimensionBuilders.expand
import androidx.wear.protolayout.LayoutElementBuilders.Box
import androidx.wear.protolayout.LayoutElementBuilders.Column
import androidx.wear.protolayout.LayoutElementBuilders.HORIZONTAL_ALIGN_END
import androidx.wear.protolayout.LayoutElementBuilders.HORIZONTAL_ALIGN_START
import androidx.wear.protolayout.LayoutElementBuilders.HorizontalAlignment
import androidx.wear.protolayout.LayoutElementBuilders.LayoutElement
import androidx.wear.protolayout.LayoutElementBuilders.Row
import androidx.wear.protolayout.ModifiersBuilders.Padding
import androidx.wear.protolayout.material3.ButtonDefaults.DEFAULT_CONTENT_PADDING
import androidx.wear.protolayout.material3.CompactButtonStyle.COMPACT_BUTTON_ICON_LABEL_SPACE_DP
import androidx.wear.protolayout.material3.Typography.TypographyToken
import androidx.wear.protolayout.modifiers.padding
import androidx.wear.protolayout.types.LayoutColor
import androidx.wear.protolayout.types.argb
import androidx.wear.protolayout.types.dp

/**
 * Represents the container and content colors used in buttons, such as [textEdgeButton] or
 * [iconEdgeButton].
 *
 * @param containerColor The container color to be used for a button's background.
 * @param iconColor The icon tint color to be used for a button.
 * @param labelColor The label color to be used for a button.
 * @param secondaryLabelColor The secondary label color to be used for a button.
 */
public class ButtonColors(
    /** The container color to be used for a button's background. */
    public val containerColor: LayoutColor = Color.BLACK.argb,
    /** The icon tint color to be used for a button. */
    public val iconColor: LayoutColor = Color.BLACK.argb,
    /** The label color to be used for a button. */
    public val labelColor: LayoutColor = Color.BLACK.argb,
    /** The secondary label color to be used for a button. */
    public val secondaryLabelColor: LayoutColor = Color.BLACK.argb,
) {
    /**
     * Returns a copy of this [androidx.wear.protolayout.material3.ButtonColors], optionally
     * overriding some of the values.
     *
     * @param containerColor The container color to be used for a button's background.
     * @param iconColor The icon tint color to be used for a button.
     * @param labelColor The label color to be used for a button.
     * @param secondaryLabelColor The secondary label color to be used for a button.
     */
    public fun copy(
        containerColor: LayoutColor = this.containerColor,
        iconColor: LayoutColor = this.iconColor,
        labelColor: LayoutColor = this.labelColor,
        secondaryLabelColor: LayoutColor = this.secondaryLabelColor,
    ): ButtonColors =
        ButtonColors(
            containerColor = containerColor,
            iconColor = iconColor,
            labelColor = labelColor,
            secondaryLabelColor = secondaryLabelColor,
        )
}

public object ButtonDefaults {
    /**
     * Returns [LayoutElement] describing the inner content for the pill shape button.
     *
     * This is a [Row] containing the following:
     * * icon
     * * spacing if icon is present
     * * labels that are in [Column]
     */
    internal fun buildContentForPillShapeButton(
        label: LayoutElement,
        secondaryLabel: LayoutElement?,
        icon: LayoutElement?,
        @HorizontalAlignment horizontalAlignment: Int,
        style: ButtonStyle,
    ): LayoutElement {
        val labels: Column.Builder = Column.Builder().setHorizontalAlignment(horizontalAlignment)

        val row: Row.Builder = Row.Builder()

        ContainerWithSpacersBuilder<LayoutElement>(labels::addContent, label)
            .addElement(secondaryLabel)

        ContainerWithSpacersBuilder<LayoutElement>(row::addContent, icon)
            .addElement(labels.build(), verticalSpacer(style.iconToLabelsSpaceDp))

        return row.build()
    }

    /**
     * Returns [LayoutElement] describing the inner content for the avatar shape button.
     *
     * This is a [Row] containing the following:
     * * avatar
     * * spacing if icon is present
     * * labels that are in [Column]
     *
     * Additionally, horizontal padding and spacing for avatar and labels is weight based.
     *
     * [horizontalAlignment] defines side that avatar is.
     */
    internal fun MaterialScope.buildContentForAvatarButton(
        avatar: LayoutElement,
        label: LayoutElement,
        secondaryLabel: LayoutElement?,
        @HorizontalAlignment horizontalAlignment: Int,
        style: AvatarButtonStyle,
        height: ContainerDimension,
    ): LayoutElement {
        val verticalElementBuilder: Column.Builder =
            Column.Builder().setWidth(expand()).setHorizontalAlignment(HORIZONTAL_ALIGN_START)
        val horizontalElementBuilder: Row.Builder =
            Row.Builder().setWidth(expand()).setHeight(height)

        ContainerWithSpacersBuilder<LayoutElement>(
                { element: LayoutElement? -> verticalElementBuilder.addContent(element!!) },
                label,
            )
            .addElement(secondaryLabel)

        // Side padding - start
        horizontalElementBuilder.addContent(
            verticalSpacer(
                (deviceConfiguration.screenWidthDp *
                        (if (horizontalAlignment == HORIZONTAL_ALIGN_START) {
                            style.avatarPaddingPercentage
                        } else {
                            style.labelsPaddingPercentage
                        }) / 100f)
                    .dp
            )
        )

        // Wrap avatar in expandable box with weights
        val avatarSize = (deviceConfiguration.screenWidthDp * style.avatarSizePercentage / 100f).dp
        val wrapAvatar =
            Box.Builder().setWidth(avatarSize).setHeight(avatarSize).addContent(avatar).build()

        if (horizontalAlignment == HORIZONTAL_ALIGN_START) {
            horizontalElementBuilder.addContent(wrapAvatar)
            horizontalElementBuilder.addContent(verticalSpacer(style.avatarToLabelsSpaceDp))
        }

        // Labels
        horizontalElementBuilder.addContent(
            Box.Builder()
                .setHorizontalAlignment(HORIZONTAL_ALIGN_START)
                .setWidth(expand())
                .addContent(verticalElementBuilder.build())
                .build()
        )

        if (horizontalAlignment == HORIZONTAL_ALIGN_END) {
            horizontalElementBuilder.addContent(verticalSpacer(style.avatarToLabelsSpaceDp))
            horizontalElementBuilder.addContent(wrapAvatar)
        }

        // Side padding - end
        horizontalElementBuilder.addContent(
            verticalSpacer(
                (deviceConfiguration.screenWidthDp *
                        (if (horizontalAlignment == HORIZONTAL_ALIGN_START) {
                            style.labelsPaddingPercentage
                        } else {
                            style.avatarPaddingPercentage
                        }) / 100f)
                    .dp
            )
        )

        return horizontalElementBuilder.build()
    }

    /**
     * Returns [LayoutElement] describing the inner content for the compact button.
     *
     * This is a [Row] wrapped inside of the Box for alignment, containing the following:
     * * icon
     * * spacing if icon is present
     * * label
     */
    internal fun buildContentForCompactButton(
        label: LayoutElement?,
        icon: LayoutElement?,
        @HorizontalAlignment horizontalAlignment: Int,
    ): LayoutElement {
        val row: Row.Builder = Row.Builder()

        // Icon can be placed on start or on end position, so we need to figure our which is first
        // and which is second element.
        val firstElement = if (horizontalAlignment == HORIZONTAL_ALIGN_START) icon else label
        val secondElement = if (horizontalAlignment == HORIZONTAL_ALIGN_START) label else icon

        ContainerWithSpacersBuilder<LayoutElement>(row::addContent, firstElement)
            .addElement(secondElement, verticalSpacer(COMPACT_BUTTON_ICON_LABEL_SPACE_DP))

        return Box.Builder()
            .addContent(row.build())
            .setHorizontalAlignment(horizontalAlignment)
            .build()
    }

    /**
     * [ButtonColors] for the high-emphasis button representing the primary, most important or most
     * common action on a screen.
     *
     * These colors are using [ColorScheme.primary] for background color and [ColorScheme.onPrimary]
     * for content color from the given [MaterialScope]'s [ColorScheme].
     */
    public fun MaterialScope.filledButtonColors(): ButtonColors =
        ButtonColors(
            containerColor = theme.colorScheme.primary,
            iconColor = theme.colorScheme.onPrimary,
            labelColor = theme.colorScheme.onPrimary,
            secondaryLabelColor = theme.colorScheme.onPrimary.withOpacity(0.8f),
        )

    /**
     * [ButtonColors] for the medium-emphasis button.
     *
     * These colors are using [ColorScheme.surfaceContainer] for background color,
     * [ColorScheme.onSurface] for content color and [ColorScheme.primary] for icon from the given
     * [MaterialScope]'s [ColorScheme].
     */
    public fun MaterialScope.filledTonalButtonColors(): ButtonColors =
        ButtonColors(
            containerColor = theme.colorScheme.surfaceContainer,
            iconColor = theme.colorScheme.primary,
            labelColor = theme.colorScheme.onSurface,
            secondaryLabelColor = theme.colorScheme.onSurfaceVariant,
        )

    /**
     * Alternative [ButtonColors] for the high-emphasis button.
     *
     * These colors are using [ColorScheme.primaryContainer] for background color and
     * [ColorScheme.onPrimaryContainer] for content color from the given [MaterialScope]'s
     * [ColorScheme].
     */
    public fun MaterialScope.filledVariantButtonColors(): ButtonColors =
        ButtonColors(
            containerColor = theme.colorScheme.primaryContainer,
            iconColor = theme.colorScheme.onPrimaryContainer,
            labelColor = theme.colorScheme.onPrimaryContainer,
            secondaryLabelColor = theme.colorScheme.onPrimaryContainer.withOpacity(0.9f),
        )

    internal const val METADATA_TAG_BUTTON: String = "BTN"
    internal val DEFAULT_CONTENT_PADDING = padding(8f)
    @Dimension(DP) internal const val IMAGE_BUTTON_DEFAULT_SIZE_DP = 52
}

/** Provides style values for the compact button component. */
internal object CompactButtonStyle {
    @Dimension(DP) internal const val COMPACT_BUTTON_HEIGHT_DP: Float = 32f
    @Dimension(DP) internal const val COMPACT_BUTTON_ICON_SIZE_SMALL_DP: Float = 20f
    @Dimension(DP) internal const val COMPACT_BUTTON_ICON_SIZE_LARGE_DP: Float = 24f
    @Dimension(DP) internal const val COMPACT_BUTTON_DEFAULT_CONTENT_PADDING_DP: Int = 12
    @Dimension(DP) internal const val COMPACT_BUTTON_ICON_LABEL_SPACE_DP = 6
    @TypographyToken internal const val COMPACT_BUTTON_LABEL_TYPOGRAPHY = Typography.LABEL_SMALL
}

/** Provides style values for the icon button component. */
public class IconButtonStyle
internal constructor(
    @Dimension(unit = DP) internal val iconSize: Float,
    internal val innerPadding: Padding = DEFAULT_CONTENT_PADDING,
) {
    public companion object {
        /**
         * Default style variation for the [iconButton] where all opinionated inner content is
         * displayed in a medium size.
         */
        public fun defaultIconButtonStyle(): IconButtonStyle = IconButtonStyle(26f)

        /**
         * Default style variation for the [iconButton] where all opinionated inner content is
         * displayed in a large size.
         */
        public fun largeIconButtonStyle(): IconButtonStyle = IconButtonStyle(32f)
    }
}

/** Provides style values for the text button component. */
public class TextButtonStyle
internal constructor(
    @TypographyToken internal val labelTypography: Int,
    internal val innerPadding: Padding = DEFAULT_CONTENT_PADDING,
) {
    public companion object {
        /**
         * Default style variation for the [textButton] where all opinionated inner content is
         * displayed in a small size.
         */
        public fun smallTextButtonStyle(): TextButtonStyle =
            TextButtonStyle(Typography.LABEL_MEDIUM)

        /**
         * Default style variation for the [textButton] where all opinionated inner content is
         * displayed in a medium size.
         */
        public fun defaultTextButtonStyle(): TextButtonStyle =
            TextButtonStyle(Typography.LABEL_LARGE)

        /**
         * Default style variation for the [textButton] where all opinionated inner content is
         * displayed in a large size.
         */
        public fun largeTextButtonStyle(): TextButtonStyle =
            TextButtonStyle(Typography.DISPLAY_SMALL)

        /**
         * Default style variation for the [textButton] where all opinionated inner content is
         * displayed in an extra large size.
         */
        public fun extraLargeTextButtonStyle(): TextButtonStyle =
            TextButtonStyle(Typography.DISPLAY_MEDIUM)
    }
}

/** Provides style values for the pill shape button component. */
public class ButtonStyle
internal constructor(
    @TypographyToken internal val labelTypography: Int,
    @TypographyToken internal val secondaryLabelTypography: Int,
    @Dimension(DP) internal val iconSize: Float,
    internal val innerPadding: Padding,
    @Dimension(DP) internal val iconToLabelsSpaceDp: Int,
) {
    public companion object {
        /**
         * Default style variation for the [button] where all opinionated inner content is displayed
         * in a small size.
         */
        public fun smallButtonStyle(): ButtonStyle =
            ButtonStyle(
                labelTypography = Typography.LABEL_MEDIUM,
                secondaryLabelTypography = Typography.BODY_SMALL,
                iconSize = 24f,
                innerPadding = padding(horizontal = 14f, vertical = 10f),
                iconToLabelsSpaceDp = 6,
            )

        /**
         * Default style variation for the [button] where all opinionated inner content is displayed
         * in a medium size.
         */
        public fun defaultButtonStyle(): ButtonStyle =
            ButtonStyle(
                labelTypography = Typography.TITLE_MEDIUM,
                secondaryLabelTypography = Typography.LABEL_SMALL,
                iconSize = 26f,
                innerPadding = padding(horizontal = 14f, vertical = 6f),
                iconToLabelsSpaceDp = 8,
            )

        /**
         * Default style variation for the [button] where all opinionated inner content is displayed
         * in a large size.
         */
        public fun largeButtonStyle(): ButtonStyle =
            ButtonStyle(
                labelTypography = Typography.LABEL_LARGE,
                secondaryLabelTypography = Typography.LABEL_SMALL,
                iconSize = 32f,
                innerPadding = padding(horizontal = 14f, vertical = 8f),
                iconToLabelsSpaceDp = 10,
            )
    }
}

/** Provides style values for the avatar button component. */
public class AvatarButtonStyle
internal constructor(
    @TypographyToken internal val labelTypography: Int,
    @TypographyToken internal val secondaryLabelTypography: Int,
    // These are percentages of the total screen size because avatar slot should stay the same if we
    // have multiple buttons on the screen.
    @FloatRange(from = 0.0, to = 100.0) internal val avatarSizePercentage: Float,
    @FloatRange(from = 0.0, to = 100.0) internal val avatarPaddingPercentage: Float,
    @FloatRange(from = 0.0, to = 100.0) internal val labelsPaddingPercentage: Float,
    internal val innerVerticalPadding: Padding,
    @Dimension(DP) internal val avatarToLabelsSpaceDp: Int,
) {
    public companion object {
        /**
         * Default style variation for the [avatarButton] where all opinionated inner content is
         * displayed in a medium size.
         */
        public fun defaultAvatarButtonStyle(): AvatarButtonStyle =
            AvatarButtonStyle(
                labelTypography = Typography.LABEL_MEDIUM,
                secondaryLabelTypography = Typography.BODY_SMALL,
                avatarSizePercentage = 19.6f,
                avatarPaddingPercentage = 4.16f,
                labelsPaddingPercentage = 7.1f,
                innerVerticalPadding = padding(vertical = 8f, horizontal = Float.NaN),
                avatarToLabelsSpaceDp = 6,
            )

        /**
         * Default style variation for the [avatarButton] where all opinionated inner content is
         * displayed in a large size.
         */
        public fun largeAvatarButtonStyle(): AvatarButtonStyle =
            AvatarButtonStyle(
                labelTypography = Typography.TITLE_MEDIUM,
                secondaryLabelTypography = Typography.LABEL_SMALL,
                avatarSizePercentage = 23.15f,
                avatarPaddingPercentage = 2.1f,
                labelsPaddingPercentage = 7.1f,
                innerVerticalPadding = padding(vertical = 6f, horizontal = Float.NaN),
                avatarToLabelsSpaceDp = 6,
            )
    }
}
