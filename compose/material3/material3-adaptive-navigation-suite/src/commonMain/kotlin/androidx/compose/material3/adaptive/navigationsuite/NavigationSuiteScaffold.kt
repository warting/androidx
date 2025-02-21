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

@file:Suppress("DEPRECATION") // Suppress for imports of WindowWidthSizeClass

package androidx.compose.material3.adaptive.navigationsuite

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.only
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.DrawerDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemColors
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemColors
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailDefaults
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemColors
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.PermanentDrawerSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveComponentOverrideApi
import androidx.compose.material3.adaptive.WindowAdaptiveInfo
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.State
import androidx.compose.runtime.collection.MutableVector
import androidx.compose.runtime.collection.mutableVectorOf
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.util.fastFirst
import androidx.window.core.layout.WindowHeightSizeClass
import androidx.window.core.layout.WindowWidthSizeClass

/** Possible values of [NavigationSuiteScaffoldState]. */
enum class NavigationSuiteScaffoldValue {
    /** The state of the navigation component of the scaffold when it's visible. */
    Visible,

    /** The state of the navigation component of the scaffold when it's hidden. */
    Hidden
}

/**
 * A state object that can be hoisted to observe the navigation suite scaffold state. It allows for
 * setting its navigation component to be hidden or displayed.
 *
 * @see rememberNavigationSuiteScaffoldState to construct the default implementation.
 */
interface NavigationSuiteScaffoldState {
    /** Whether the state is currently animating. */
    val isAnimating: Boolean

    /** Whether the navigation component is going to be shown or hidden. */
    val targetValue: NavigationSuiteScaffoldValue

    /** Whether the navigation component is currently shown or hidden. */
    val currentValue: NavigationSuiteScaffoldValue

    /** Hide the navigation component with animation and suspend until it fully expands. */
    suspend fun hide()

    /** Show the navigation component with animation and suspend until it fully expands. */
    suspend fun show()

    /**
     * Hide the navigation component with animation if it's shown, or collapse it otherwise, and
     * suspend until it fully expands.
     */
    suspend fun toggle()

    /**
     * Set the state without any animation and suspend until it's set.
     *
     * @param targetValue the value to set to
     */
    suspend fun snapTo(targetValue: NavigationSuiteScaffoldValue)
}

/** Create and [remember] a [NavigationSuiteScaffoldState] */
@Composable
fun rememberNavigationSuiteScaffoldState(
    initialValue: NavigationSuiteScaffoldValue = NavigationSuiteScaffoldValue.Visible
): NavigationSuiteScaffoldState {
    return rememberSaveable(saver = NavigationSuiteScaffoldStateImpl.Saver()) {
        NavigationSuiteScaffoldStateImpl(initialValue = initialValue)
    }
}

/**
 * The Navigation Suite Scaffold wraps the provided content and places the adequate provided
 * navigation component on the screen according to the current [NavigationSuiteType].
 *
 * The navigation component can be animated to be hidden or shown via a
 * [NavigationSuiteScaffoldState].
 *
 * Example default usage:
 *
 * @sample androidx.compose.material3.adaptive.navigationsuite.samples.NavigationSuiteScaffoldSample
 *
 * Example custom configuration usage:
 *
 * @sample androidx.compose.material3.adaptive.navigationsuite.samples.NavigationSuiteScaffoldCustomConfigSample
 * @param navigationSuiteItems the navigation items to be displayed
 * @param modifier the [Modifier] to be applied to the navigation suite scaffold
 * @param layoutType the current [NavigationSuiteType]. Defaults to
 *   [NavigationSuiteScaffoldDefaults.calculateFromAdaptiveInfo]
 * @param navigationSuiteColors [NavigationSuiteColors] that will be used to determine the container
 *   (background) color of the navigation component and the preferred color for content inside the
 *   navigation component
 * @param containerColor the color used for the background of the navigation suite scaffold,
 *   including the passed [content] composable. Use [Color.Transparent] to have no color
 * @param contentColor the preferred color to be used for typography and iconography within the
 *   passed in [content] lambda inside the navigation suite scaffold.
 * @param state the [NavigationSuiteScaffoldState] of this navigation suite scaffold
 * @param content the content of your screen
 */
@OptIn(ExperimentalMaterial3AdaptiveComponentOverrideApi::class)
@Composable
fun NavigationSuiteScaffold(
    navigationSuiteItems: NavigationSuiteScope.() -> Unit,
    modifier: Modifier = Modifier,
    layoutType: NavigationSuiteType =
        NavigationSuiteScaffoldDefaults.calculateFromAdaptiveInfo(WindowAdaptiveInfoDefault),
    navigationSuiteColors: NavigationSuiteColors = NavigationSuiteDefaults.colors(),
    containerColor: Color = NavigationSuiteScaffoldDefaults.containerColor,
    contentColor: Color = NavigationSuiteScaffoldDefaults.contentColor,
    state: NavigationSuiteScaffoldState = rememberNavigationSuiteScaffoldState(),
    content: @Composable () -> Unit = {},
) {
    with(LocalNavigationSuiteScaffoldOverride.current) {
        NavigationSuiteScaffoldOverrideScope(
                navigationSuiteItems = navigationSuiteItems,
                modifier = modifier,
                layoutType = layoutType,
                navigationSuiteColors = navigationSuiteColors,
                containerColor = containerColor,
                contentColor = contentColor,
                state = state,
                content = content
            )
            .NavigationSuiteScaffold()
    }
}

/**
 * This override provides the default behavior of the [NavigationSuiteScaffold] component.
 *
 * [NavigationSuiteScaffoldOverride] used when no override is specified.
 */
@ExperimentalMaterial3AdaptiveComponentOverrideApi
object DefaultNavigationSuiteScaffoldOverride : NavigationSuiteScaffoldOverride {
    @Composable
    override fun NavigationSuiteScaffoldOverrideScope.NavigationSuiteScaffold() {
        Surface(modifier = modifier, color = containerColor, contentColor = contentColor) {
            NavigationSuiteScaffoldLayout(
                navigationSuite = {
                    NavigationSuite(
                        layoutType = layoutType,
                        colors = navigationSuiteColors,
                        content = navigationSuiteItems
                    )
                },
                state = state,
                layoutType = layoutType,
                content = {
                    Box(
                        Modifier.consumeWindowInsets(
                            if (
                                state.currentValue == NavigationSuiteScaffoldValue.Hidden &&
                                    !state.isAnimating
                            ) {
                                NoWindowInsets
                            } else {
                                when (layoutType) {
                                    NavigationSuiteType.NavigationBar ->
                                        NavigationBarDefaults.windowInsets.only(
                                            WindowInsetsSides.Bottom
                                        )
                                    NavigationSuiteType.NavigationRail ->
                                        NavigationRailDefaults.windowInsets.only(
                                            WindowInsetsSides.Start
                                        )
                                    NavigationSuiteType.NavigationDrawer ->
                                        DrawerDefaults.windowInsets.only(WindowInsetsSides.Start)
                                    else -> NoWindowInsets
                                }
                            }
                        )
                    ) {
                        content()
                    }
                }
            )
        }
    }
}

/**
 * The Navigation Suite Scaffold wraps the provided content and places the adequate provided
 * navigation component on the screen according to the current [NavigationSuiteType].
 *
 * Example default usage:
 *
 * @sample androidx.compose.material3.adaptive.navigationsuite.samples.NavigationSuiteScaffoldSample
 *
 * Example custom configuration usage:
 *
 * @sample androidx.compose.material3.adaptive.navigationsuite.samples.NavigationSuiteScaffoldCustomConfigSample
 * @param navigationSuiteItems the navigation items to be displayed
 * @param modifier the [Modifier] to be applied to the navigation suite scaffold
 * @param layoutType the current [NavigationSuiteType]. Defaults to
 *   [NavigationSuiteScaffoldDefaults.calculateFromAdaptiveInfo]
 * @param navigationSuiteColors [NavigationSuiteColors] that will be used to determine the container
 *   (background) color of the navigation component and the preferred color for content inside the
 *   navigation component
 * @param containerColor the color used for the background of the navigation suite scaffold,
 *   including the passed [content] composable. Use [Color.Transparent] to have no color
 * @param contentColor the preferred color to be used for typography and iconography within the
 *   passed in [content] lambda inside the navigation suite scaffold.
 * @param content the content of your screen
 */
@Deprecated(
    message = "Deprecated in favor of NavigationSuiteScaffold with state parameter",
    level = DeprecationLevel.HIDDEN
)
@Composable
fun NavigationSuiteScaffold(
    navigationSuiteItems: NavigationSuiteScope.() -> Unit,
    modifier: Modifier = Modifier,
    layoutType: NavigationSuiteType =
        NavigationSuiteScaffoldDefaults.calculateFromAdaptiveInfo(WindowAdaptiveInfoDefault),
    navigationSuiteColors: NavigationSuiteColors = NavigationSuiteDefaults.colors(),
    containerColor: Color = NavigationSuiteScaffoldDefaults.containerColor,
    contentColor: Color = NavigationSuiteScaffoldDefaults.contentColor,
    content: @Composable () -> Unit = {},
) =
    NavigationSuiteScaffold(
        navigationSuiteItems = navigationSuiteItems,
        modifier = modifier,
        state = rememberNavigationSuiteScaffoldState(),
        layoutType = layoutType,
        navigationSuiteColors = navigationSuiteColors,
        containerColor = containerColor,
        contentColor = contentColor,
        content = content
    )

/**
 * Layout for a [NavigationSuiteScaffold]'s content. This function wraps the [content] and places
 * the [navigationSuite] component according to the given [layoutType].
 *
 * The usage of this function is recommended when you need some customization that is not viable via
 * the use of [NavigationSuiteScaffold]. Example usage:
 *
 * @sample androidx.compose.material3.adaptive.navigationsuite.samples.NavigationSuiteScaffoldCustomNavigationRail
 * @param navigationSuite the navigation component to be displayed, typically [NavigationSuite]
 * @param layoutType the current [NavigationSuiteType]. Defaults to
 *   [NavigationSuiteScaffoldDefaults.calculateFromAdaptiveInfo]
 * @param state the [NavigationSuiteScaffoldState] of this navigation suite scaffold layout
 * @param content the content of your screen
 */
@Composable
fun NavigationSuiteScaffoldLayout(
    navigationSuite: @Composable () -> Unit,
    layoutType: NavigationSuiteType =
        NavigationSuiteScaffoldDefaults.calculateFromAdaptiveInfo(WindowAdaptiveInfoDefault),
    state: NavigationSuiteScaffoldState = rememberNavigationSuiteScaffoldState(),
    content: @Composable () -> Unit = {}
) {
    val animationProgress by
        animateFloatAsState(
            targetValue = if (state.currentValue == NavigationSuiteScaffoldValue.Hidden) 0f else 1f,
            animationSpec = AnimationSpec
        )

    Layout({
        // Wrap the navigation suite and content composables each in a Box to not propagate the
        // parent's (Surface) min constraints to its children (see b/312664933).
        Box(Modifier.layoutId(NavigationSuiteLayoutIdTag)) { navigationSuite() }
        Box(Modifier.layoutId(ContentLayoutIdTag)) { content() }
    }) { measurables, constraints ->
        val looseConstraints = constraints.copy(minWidth = 0, minHeight = 0)
        // Find the navigation suite composable through it's layoutId tag
        val navigationPlaceable =
            measurables
                .fastFirst { it.layoutId == NavigationSuiteLayoutIdTag }
                .measure(looseConstraints)
        val isNavigationBar = layoutType == NavigationSuiteType.NavigationBar
        val layoutHeight = constraints.maxHeight
        val layoutWidth = constraints.maxWidth
        // Find the content composable through it's layoutId tag
        val contentPlaceable =
            measurables
                .fastFirst { it.layoutId == ContentLayoutIdTag }
                .measure(
                    if (isNavigationBar) {
                        constraints.copy(
                            minHeight =
                                layoutHeight -
                                    (navigationPlaceable.height * animationProgress).toInt(),
                            maxHeight =
                                layoutHeight -
                                    (navigationPlaceable.height * animationProgress).toInt()
                        )
                    } else {
                        constraints.copy(
                            minWidth =
                                layoutWidth -
                                    (navigationPlaceable.width * animationProgress).toInt(),
                            maxWidth =
                                layoutWidth -
                                    (navigationPlaceable.width * animationProgress).toInt()
                        )
                    }
                )

        layout(layoutWidth, layoutHeight) {
            if (isNavigationBar) {
                // Place content above the navigation component.
                contentPlaceable.placeRelative(0, 0)
                // Place the navigation component at the bottom of the screen.
                navigationPlaceable.placeRelative(
                    0,
                    layoutHeight - (navigationPlaceable.height * animationProgress).toInt()
                )
            } else {
                // Place the navigation component at the start of the screen.
                navigationPlaceable.placeRelative(
                    (0 - (navigationPlaceable.width * (1f - animationProgress))).toInt(),
                    0
                )
                // Place content to the side of the navigation component.
                contentPlaceable.placeRelative(
                    (navigationPlaceable.width * animationProgress).toInt(),
                    0
                )
            }
        }
    }
}

/**
 * Layout for a [NavigationSuiteScaffold]'s content. This function wraps the [content] and places
 * the [navigationSuite] component according to the given [layoutType].
 *
 * The usage of this function is recommended when you need some customization that is not viable via
 * the use of [NavigationSuiteScaffold]. Example usage:
 *
 * @sample androidx.compose.material3.adaptive.navigationsuite.samples.NavigationSuiteScaffoldCustomNavigationRail
 * @param navigationSuite the navigation component to be displayed, typically [NavigationSuite]
 * @param layoutType the current [NavigationSuiteType]. Defaults to
 *   [NavigationSuiteScaffoldDefaults.calculateFromAdaptiveInfo]
 * @param content the content of your screen
 */
@Deprecated(
    message = "Deprecated in favor of NavigationSuiteScaffoldLayout with state parameter",
    level = DeprecationLevel.HIDDEN
)
@Composable
fun NavigationSuiteScaffoldLayout(
    navigationSuite: @Composable () -> Unit,
    layoutType: NavigationSuiteType =
        NavigationSuiteScaffoldDefaults.calculateFromAdaptiveInfo(WindowAdaptiveInfoDefault),
    content: @Composable () -> Unit = {}
) =
    NavigationSuiteScaffoldLayout(
        navigationSuite = navigationSuite,
        state = rememberNavigationSuiteScaffoldState(),
        layoutType = layoutType,
        content = content
    )

/**
 * The default Material navigation component according to the current [NavigationSuiteType] to be
 * used with the [NavigationSuiteScaffold].
 *
 * For specifics about each navigation component, see [NavigationBar], [NavigationRail], and
 * [PermanentDrawerSheet].
 *
 * @param modifier the [Modifier] to be applied to the navigation component
 * @param layoutType the current [NavigationSuiteType] of the [NavigationSuiteScaffold]. Defaults to
 *   [NavigationSuiteScaffoldDefaults.calculateFromAdaptiveInfo]
 * @param colors [NavigationSuiteColors] that will be used to determine the container (background)
 *   color of the navigation component and the preferred color for content inside the navigation
 *   component
 * @param content the content inside the current navigation component, typically
 *   [NavigationSuiteScope.item]s
 */
@Composable
fun NavigationSuite(
    modifier: Modifier = Modifier,
    layoutType: NavigationSuiteType =
        NavigationSuiteScaffoldDefaults.calculateFromAdaptiveInfo(WindowAdaptiveInfoDefault),
    colors: NavigationSuiteColors = NavigationSuiteDefaults.colors(),
    content: NavigationSuiteScope.() -> Unit
) {
    val scope by rememberStateOfItems(content)
    // Define defaultItemColors here since we can't set NavigationSuiteDefaults.itemColors() as a
    // default for the colors param of the NavigationSuiteScope.item non-composable function.
    val defaultItemColors = NavigationSuiteDefaults.itemColors()

    when (layoutType) {
        NavigationSuiteType.NavigationBar -> {
            NavigationBar(
                modifier = modifier,
                containerColor = colors.navigationBarContainerColor,
                contentColor = colors.navigationBarContentColor
            ) {
                scope.itemList.forEach {
                    NavigationBarItem(
                        modifier = it.modifier,
                        selected = it.selected,
                        onClick = it.onClick,
                        icon = { NavigationItemIcon(icon = it.icon, badge = it.badge) },
                        enabled = it.enabled,
                        label = it.label,
                        alwaysShowLabel = it.alwaysShowLabel,
                        colors =
                            it.colors?.navigationBarItemColors
                                ?: defaultItemColors.navigationBarItemColors,
                        interactionSource = it.interactionSource
                    )
                }
            }
        }
        NavigationSuiteType.NavigationRail -> {
            NavigationRail(
                modifier = modifier,
                containerColor = colors.navigationRailContainerColor,
                contentColor = colors.navigationRailContentColor
            ) {
                scope.itemList.forEach {
                    NavigationRailItem(
                        modifier = it.modifier,
                        selected = it.selected,
                        onClick = it.onClick,
                        icon = { NavigationItemIcon(icon = it.icon, badge = it.badge) },
                        enabled = it.enabled,
                        label = it.label,
                        alwaysShowLabel = it.alwaysShowLabel,
                        colors =
                            it.colors?.navigationRailItemColors
                                ?: defaultItemColors.navigationRailItemColors,
                        interactionSource = it.interactionSource
                    )
                }
            }
        }
        NavigationSuiteType.NavigationDrawer -> {
            PermanentDrawerSheet(
                modifier = modifier,
                drawerContainerColor = colors.navigationDrawerContainerColor,
                drawerContentColor = colors.navigationDrawerContentColor
            ) {
                scope.itemList.forEach {
                    NavigationDrawerItem(
                        modifier = it.modifier,
                        selected = it.selected,
                        onClick = it.onClick,
                        icon = it.icon,
                        badge = it.badge,
                        label = { it.label?.invoke() ?: Text("") },
                        colors =
                            it.colors?.navigationDrawerItemColors
                                ?: defaultItemColors.navigationDrawerItemColors,
                        interactionSource = it.interactionSource
                    )
                }
            }
        }
        NavigationSuiteType.None -> {
            /* Do nothing. */
        }
    }
}

/** The scope associated with the [NavigationSuiteScope]. */
sealed interface NavigationSuiteScope {

    /**
     * This function sets the parameters of the default Material navigation item to be used with the
     * Navigation Suite Scaffold. The item is called in [NavigationSuite], according to the current
     * [NavigationSuiteType].
     *
     * For specifics about each item component, see [NavigationBarItem], [NavigationRailItem], and
     * [NavigationDrawerItem].
     *
     * @param selected whether this item is selected
     * @param onClick called when this item is clicked
     * @param icon icon for this item, typically an [Icon]
     * @param modifier the [Modifier] to be applied to this item
     * @param enabled controls the enabled state of this item. When `false`, this component will not
     *   respond to user input, and it will appear visually disabled and disabled to accessibility
     *   services. Note: as of now, for [NavigationDrawerItem], this is always `true`.
     * @param label the text label for this item
     * @param alwaysShowLabel whether to always show the label for this item. If `false`, the label
     *   will only be shown when this item is selected. Note: for [NavigationDrawerItem] this is
     *   always `true`
     * @param badge optional badge to show on this item
     * @param colors [NavigationSuiteItemColors] that will be used to resolve the colors used for
     *   this item in different states. If null, [NavigationSuiteDefaults.itemColors] will be used.
     * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
     *   emitting [Interaction]s for this item. You can use this to change the item's appearance or
     *   preview the item in different states. Note that if `null` is provided, interactions will
     *   still happen internally.
     */
    fun item(
        selected: Boolean,
        onClick: () -> Unit,
        icon: @Composable () -> Unit,
        modifier: Modifier = Modifier,
        enabled: Boolean = true,
        label: @Composable (() -> Unit)? = null,
        alwaysShowLabel: Boolean = true,
        badge: (@Composable () -> Unit)? = null,
        colors: NavigationSuiteItemColors? = null,
        interactionSource: MutableInteractionSource? = null
    )
}

/**
 * Class that describes the different navigation suite types of the [NavigationSuiteScaffold].
 *
 * The [NavigationSuiteType] informs the [NavigationSuite] of what navigation component to expect.
 */
@JvmInline
value class NavigationSuiteType private constructor(private val description: String) {
    override fun toString(): String {
        return description
    }

    companion object {
        /**
         * A navigation suite type that instructs the [NavigationSuite] to expect a [NavigationBar]
         * that will be displayed at the bottom of the screen.
         *
         * @see NavigationBar
         */
        val NavigationBar = NavigationSuiteType(description = "NavigationBar")

        /**
         * A navigation suite type that instructs the [NavigationSuite] to expect a [NavigationRail]
         * that will be displayed at the start of the screen.
         *
         * @see NavigationRail
         */
        val NavigationRail = NavigationSuiteType(description = "NavigationRail")

        /**
         * A navigation suite type that instructs the [NavigationSuite] to expect a
         * [PermanentDrawerSheet] that will be displayed at the start of the screen.
         *
         * @see PermanentDrawerSheet
         */
        val NavigationDrawer = NavigationSuiteType(description = "NavigationDrawer")

        /**
         * A navigation suite type that instructs the [NavigationSuite] to not display any
         * navigation components on the screen.
         */
        val None = NavigationSuiteType(description = "None")
    }
}

/** Contains the default values used by the [NavigationSuiteScaffold]. */
object NavigationSuiteScaffoldDefaults {
    /**
     * Returns the expected [NavigationSuiteType] according to the provided [WindowAdaptiveInfo].
     * Usually used with the [NavigationSuiteScaffold] and related APIs.
     *
     * @param adaptiveInfo the provided [WindowAdaptiveInfo]
     * @see NavigationSuiteScaffold
     */
    @Suppress("DEPRECATION") // WindowWidthSizeClass deprecated
    fun calculateFromAdaptiveInfo(adaptiveInfo: WindowAdaptiveInfo): NavigationSuiteType {
        return with(adaptiveInfo) {
            if (
                windowPosture.isTabletop ||
                    windowSizeClass.windowHeightSizeClass == WindowHeightSizeClass.COMPACT
            ) {
                NavigationSuiteType.NavigationBar
            } else if (
                windowSizeClass.windowWidthSizeClass == WindowWidthSizeClass.EXPANDED ||
                    windowSizeClass.windowWidthSizeClass == WindowWidthSizeClass.MEDIUM
            ) {
                NavigationSuiteType.NavigationRail
            } else {
                NavigationSuiteType.NavigationBar
            }
        }
    }

    /** Default container color for a navigation suite scaffold. */
    val containerColor: Color
        @Composable get() = MaterialTheme.colorScheme.background

    /** Default content color for a navigation suite scaffold. */
    val contentColor: Color
        @Composable get() = MaterialTheme.colorScheme.onBackground
}

/** Contains the default values used by the [NavigationSuite]. */
object NavigationSuiteDefaults {
    /**
     * Creates a [NavigationSuiteColors] with the provided colors for the container color, according
     * to the Material specification.
     *
     * Use [Color.Transparent] for the navigation*ContainerColor to have no color. The
     * navigation*ContentColor will default to either the matching content color for
     * navigation*ContainerColor, or to the current [LocalContentColor] if navigation*ContainerColor
     * is not a color from the theme.
     *
     * @param navigationBarContainerColor the default container color for the [NavigationBar]
     * @param navigationBarContentColor the default content color for the [NavigationBar]
     * @param navigationRailContainerColor the default container color for the [NavigationRail]
     * @param navigationRailContentColor the default content color for the [NavigationRail]
     * @param navigationDrawerContainerColor the default container color for the
     *   [PermanentDrawerSheet]
     * @param navigationDrawerContentColor the default content color for the [PermanentDrawerSheet]
     */
    @Composable
    fun colors(
        navigationBarContainerColor: Color = NavigationBarDefaults.containerColor,
        navigationBarContentColor: Color = contentColorFor(navigationBarContainerColor),
        navigationRailContainerColor: Color = NavigationRailDefaults.ContainerColor,
        navigationRailContentColor: Color = contentColorFor(navigationRailContainerColor),
        navigationDrawerContainerColor: Color =
            @Suppress("DEPRECATION") DrawerDefaults.containerColor,
        navigationDrawerContentColor: Color = contentColorFor(navigationDrawerContainerColor),
    ): NavigationSuiteColors =
        NavigationSuiteColors(
            navigationBarContainerColor = navigationBarContainerColor,
            navigationBarContentColor = navigationBarContentColor,
            navigationRailContainerColor = navigationRailContainerColor,
            navigationRailContentColor = navigationRailContentColor,
            navigationDrawerContainerColor = navigationDrawerContainerColor,
            navigationDrawerContentColor = navigationDrawerContentColor
        )

    /**
     * Creates a [NavigationSuiteItemColors] with the provided colors for a
     * [NavigationSuiteScope.item].
     *
     * For specifics about each navigation item colors see [NavigationBarItemColors],
     * [NavigationRailItemColors], and [NavigationDrawerItemColors].
     *
     * @param navigationBarItemColors the [NavigationBarItemColors] associated with the
     *   [NavigationBarItem] of the [NavigationSuiteScope.item]
     * @param navigationRailItemColors the [NavigationRailItemColors] associated with the
     *   [NavigationRailItem] of the [NavigationSuiteScope.item]
     * @param navigationDrawerItemColors the [NavigationDrawerItemColors] associated with the
     *   [NavigationDrawerItem] of the [NavigationSuiteScope.item]
     */
    @Composable
    fun itemColors(
        navigationBarItemColors: NavigationBarItemColors = NavigationBarItemDefaults.colors(),
        navigationRailItemColors: NavigationRailItemColors = NavigationRailItemDefaults.colors(),
        navigationDrawerItemColors: NavigationDrawerItemColors =
            NavigationDrawerItemDefaults.colors()
    ): NavigationSuiteItemColors =
        NavigationSuiteItemColors(
            navigationBarItemColors = navigationBarItemColors,
            navigationRailItemColors = navigationRailItemColors,
            navigationDrawerItemColors = navigationDrawerItemColors
        )
}

/**
 * Represents the colors of a [NavigationSuite].
 *
 * For specifics about each navigation component colors see [NavigationBarDefaults],
 * [NavigationRailDefaults], and [DrawerDefaults].
 *
 * @param navigationBarContainerColor the container color for the [NavigationBar] of the
 *   [NavigationSuite]
 * @param navigationBarContentColor the content color for the [NavigationBar] of the
 *   [NavigationSuite]
 * @param navigationRailContainerColor the container color for the [NavigationRail] of the
 *   [NavigationSuite]
 * @param navigationRailContentColor the content color for the [NavigationRail] of the
 *   [NavigationSuite]
 * @param navigationDrawerContainerColor the container color for the [PermanentDrawerSheet] of the
 *   [NavigationSuite]
 * @param navigationDrawerContentColor the content color for the [PermanentDrawerSheet] of the
 *   [NavigationSuite]
 */
class NavigationSuiteColors
internal constructor(
    val navigationBarContainerColor: Color,
    val navigationBarContentColor: Color,
    val navigationRailContainerColor: Color,
    val navigationRailContentColor: Color,
    val navigationDrawerContainerColor: Color,
    val navigationDrawerContentColor: Color
)

/**
 * Represents the colors of a [NavigationSuiteScope.item].
 *
 * For specifics about each navigation item colors see [NavigationBarItemColors],
 * [NavigationRailItemColors], and [NavigationDrawerItemColors].
 *
 * @param navigationBarItemColors the [NavigationBarItemColors] associated with the
 *   [NavigationBarItem] of the [NavigationSuiteScope.item]
 * @param navigationRailItemColors the [NavigationRailItemColors] associated with the
 *   [NavigationRailItem] of the [NavigationSuiteScope.item]
 * @param navigationDrawerItemColors the [NavigationDrawerItemColors] associated with the
 *   [NavigationDrawerItem] of the [NavigationSuiteScope.item]
 */
class NavigationSuiteItemColors(
    val navigationBarItemColors: NavigationBarItemColors,
    val navigationRailItemColors: NavigationRailItemColors,
    val navigationDrawerItemColors: NavigationDrawerItemColors,
)

internal val WindowAdaptiveInfoDefault
    @Composable get() = currentWindowAdaptiveInfo()

internal val NavigationSuiteScaffoldValue.isVisible
    get() = this == NavigationSuiteScaffoldValue.Visible

internal class NavigationSuiteScaffoldStateImpl(var initialValue: NavigationSuiteScaffoldValue) :
    NavigationSuiteScaffoldState {
    private val internalValue: Float = if (initialValue.isVisible) Visible else Hidden
    private val internalState = Animatable(internalValue, Float.VectorConverter)
    private val _currentVal = derivedStateOf {
        if (internalState.value == Visible) {
            NavigationSuiteScaffoldValue.Visible
        } else {
            NavigationSuiteScaffoldValue.Hidden
        }
    }

    override val isAnimating: Boolean
        get() = internalState.isRunning

    override val targetValue: NavigationSuiteScaffoldValue
        get() =
            if (internalState.targetValue == Visible) {
                NavigationSuiteScaffoldValue.Visible
            } else {
                NavigationSuiteScaffoldValue.Hidden
            }

    override val currentValue: NavigationSuiteScaffoldValue
        get() = _currentVal.value

    override suspend fun hide() {
        internalState.animateTo(targetValue = Hidden, animationSpec = AnimationSpec)
    }

    override suspend fun show() {
        internalState.animateTo(targetValue = Visible, animationSpec = AnimationSpec)
    }

    override suspend fun toggle() {
        internalState.animateTo(
            targetValue = if (targetValue.isVisible) Hidden else Visible,
            animationSpec = AnimationSpec
        )
    }

    override suspend fun snapTo(targetValue: NavigationSuiteScaffoldValue) {
        val target = if (targetValue.isVisible) Visible else Hidden
        internalState.snapTo(target)
    }

    companion object {
        private const val Hidden = 0f
        private const val Visible = 1f

        /** The default [Saver] implementation for [NavigationSuiteScaffoldState]. */
        fun Saver() =
            Saver<NavigationSuiteScaffoldState, NavigationSuiteScaffoldValue>(
                save = { it.targetValue },
                restore = { NavigationSuiteScaffoldStateImpl(it) }
            )
    }
}

private interface NavigationSuiteItemProvider {
    val itemsCount: Int
    val itemList: MutableVector<NavigationSuiteItem>
}

private class NavigationSuiteItem(
    val selected: Boolean,
    val onClick: () -> Unit,
    val icon: @Composable () -> Unit,
    val modifier: Modifier,
    val enabled: Boolean,
    val label: @Composable (() -> Unit)?,
    val alwaysShowLabel: Boolean,
    val badge: (@Composable () -> Unit)?,
    val colors: NavigationSuiteItemColors?,
    val interactionSource: MutableInteractionSource?
)

private class NavigationSuiteScopeImpl : NavigationSuiteScope, NavigationSuiteItemProvider {

    override fun item(
        selected: Boolean,
        onClick: () -> Unit,
        icon: @Composable () -> Unit,
        modifier: Modifier,
        enabled: Boolean,
        label: @Composable (() -> Unit)?,
        alwaysShowLabel: Boolean,
        badge: (@Composable () -> Unit)?,
        colors: NavigationSuiteItemColors?,
        interactionSource: MutableInteractionSource?
    ) {
        itemList.add(
            NavigationSuiteItem(
                selected = selected,
                onClick = onClick,
                icon = icon,
                modifier = modifier,
                enabled = enabled,
                label = label,
                alwaysShowLabel = alwaysShowLabel,
                badge = badge,
                colors = colors,
                interactionSource = interactionSource
            )
        )
    }

    override val itemList: MutableVector<NavigationSuiteItem> = mutableVectorOf()

    override val itemsCount: Int
        get() = itemList.size
}

@Composable
private fun rememberStateOfItems(
    content: NavigationSuiteScope.() -> Unit
): State<NavigationSuiteItemProvider> {
    val latestContent = rememberUpdatedState(content)
    return remember { derivedStateOf { NavigationSuiteScopeImpl().apply(latestContent.value) } }
}

@Composable
private fun NavigationItemIcon(
    icon: @Composable () -> Unit,
    badge: (@Composable () -> Unit)? = null,
) {
    if (badge != null) {
        BadgedBox(badge = { badge.invoke() }) { icon() }
    } else {
        icon()
    }
}

private const val SpringDefaultSpatialDamping = 0.9f
private const val SpringDefaultSpatialStiffness = 700.0f
private const val NavigationSuiteLayoutIdTag = "navigationSuite"
private const val ContentLayoutIdTag = "content"

private val NoWindowInsets = WindowInsets(0, 0, 0, 0)
private val AnimationSpec: SpringSpec<Float> =
    spring(dampingRatio = SpringDefaultSpatialDamping, stiffness = SpringDefaultSpatialStiffness)

/**
 * Interface that allows libraries to override the behavior of the [NavigationSuiteScaffold]
 * component.
 *
 * To override this component, implement the member function of this interface, then provide the
 * implementation to [LocalNavigationSuiteScaffoldOverride] in the Compose hierarchy.
 */
@ExperimentalMaterial3AdaptiveComponentOverrideApi
interface NavigationSuiteScaffoldOverride {
    /** Behavior function that is called by the [NavigationSuiteScaffold] component. */
    @Composable fun NavigationSuiteScaffoldOverrideScope.NavigationSuiteScaffold()
}

/**
 * Parameters available to [NavigationSuiteScaffold].
 *
 * @param navigationSuiteItems the navigation items to be displayed
 * @param modifier the [Modifier] to be applied to the navigation suite scaffold
 * @param layoutType the current [NavigationSuiteType]. Defaults to
 *   [NavigationSuiteScaffoldDefaults.calculateFromAdaptiveInfo]
 * @param navigationSuiteColors [NavigationSuiteColors] that will be used to determine the container
 *   (background) color of the navigation component and the preferred color for content inside the
 *   navigation component
 * @param containerColor the color used for the background of the navigation suite scaffold,
 *   including the passed [content] composable. Use [Color.Transparent] to have no color
 * @param contentColor the preferred color to be used for typography and iconography within the
 *   passed in [content] lambda inside the navigation suite scaffold.
 * @param state the [NavigationSuiteScaffoldState] of this navigation suite scaffold
 * @param content the content of your screen
 */
@ExperimentalMaterial3AdaptiveComponentOverrideApi
class NavigationSuiteScaffoldOverrideScope
internal constructor(
    val navigationSuiteItems: NavigationSuiteScope.() -> Unit,
    val modifier: Modifier = Modifier,
    val layoutType: NavigationSuiteType,
    val navigationSuiteColors: NavigationSuiteColors,
    val containerColor: Color,
    val contentColor: Color,
    val state: NavigationSuiteScaffoldState,
    val content: @Composable () -> Unit = {},
)

/** CompositionLocal containing the currently-selected [NavigationSuiteScaffoldOverride]. */
@Suppress("OPT_IN_MARKER_ON_WRONG_TARGET")
@get:ExperimentalMaterial3AdaptiveComponentOverrideApi
@ExperimentalMaterial3AdaptiveComponentOverrideApi
val LocalNavigationSuiteScaffoldOverride:
    ProvidableCompositionLocal<NavigationSuiteScaffoldOverride> =
    compositionLocalOf {
        DefaultNavigationSuiteScaffoldOverride
    }
