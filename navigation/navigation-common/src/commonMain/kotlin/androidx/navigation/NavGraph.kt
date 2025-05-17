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

@file:JvmName("NavGraphKt")
@file:JvmMultifileClass

package androidx.navigation

import androidx.annotation.RestrictTo
import androidx.collection.SparseArrayCompat
import kotlin.jvm.JvmMultifileClass
import kotlin.jvm.JvmName
import kotlin.jvm.JvmStatic
import kotlin.jvm.JvmSynthetic
import kotlin.reflect.KClass
import kotlinx.serialization.KSerializer

/**
 * NavGraph is a collection of [NavDestination] nodes fetchable by ID.
 *
 * A NavGraph serves as a 'virtual' destination: while the NavGraph itself will not appear on the
 * back stack, navigating to the NavGraph will cause the [starting destination][getStartDestination]
 * to be added to the back stack.
 *
 * Construct a new NavGraph. This NavGraph is not valid until you
 * [add a destination][addDestination] and [set the starting destination][setStartDestination].
 *
 * @param navGraphNavigator The [NavGraphNavigator] which this destination will be associated with.
 *   Generally retrieved via a [NavController]'s[NavigatorProvider.getNavigator] method.
 */
public expect open class NavGraph(navGraphNavigator: Navigator<out NavGraph>) :
    NavDestination, Iterable<NavDestination> {

    public val nodes: SparseArrayCompat<NavDestination>
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) get

    /**
     * Matches route with all children and parents recursively.
     *
     * Does not revisit graphs (whether it's a child or parent) if it has already been visited.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public fun matchRouteComprehensive(
        route: String,
        searchChildren: Boolean,
        searchParent: Boolean,
        lastVisited: NavDestination,
    ): DeepLinkMatch?

    /**
     * Matches deeplink with all children and parents recursively.
     *
     * Does not revisit graphs (whether it's a child or parent) if it has already been visited.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public fun matchDeepLinkComprehensive(
        navDeepLinkRequest: NavDeepLinkRequest,
        searchChildren: Boolean,
        searchParent: Boolean,
        lastVisited: NavDestination,
    ): DeepLinkMatch?

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public override fun matchDeepLink(navDeepLinkRequest: NavDeepLinkRequest): DeepLinkMatch?

    /**
     * Adds a destination to this NavGraph. The destination must have an [NavDestination.id] id}
     * set.
     *
     * The destination must not have a [parent][NavDestination.parent] set. If the destination is
     * already part of a [navigation graph][NavGraph], call [remove] before calling this method.
     *
     * @param node destination to add
     * @throws IllegalArgumentException if destination does not have an id, the destination has the
     *   same id as the graph, or the destination already has a parent.
     */
    public fun addDestination(node: NavDestination)

    /**
     * Adds multiple destinations to this NavGraph. Each destination must have an
     * [NavDestination.id] id} set.
     *
     * Each destination must not have a [parent][NavDestination.parent] set. If any destination is
     * already part of a [navigation graph][NavGraph], call [remove] before calling this method.
     *
     * @param nodes destinations to add
     */
    public fun addDestinations(nodes: Collection<NavDestination?>)

    /**
     * Adds multiple destinations to this NavGraph. Each destination must have an
     * [NavDestination.id] id} set.
     *
     * Each destination must not have a [parent][NavDestination.parent] set. If any destination is
     * already part of a [navigation graph][NavGraph], call [remove] before calling this method.
     *
     * @param nodes destinations to add
     */
    public fun addDestinations(vararg nodes: NavDestination)

    /**
     * Searches all children and parents recursively.
     *
     * Does not revisit graphs (whether it's a child or parent) if it has already been visited.
     *
     * @param resId the [NavDestination.id]
     * @param lastVisited the previously visited node
     * @param searchChildren searches the graph's children for the node when true
     * @param matchingDest an optional NavDestination that the node should match with. This is
     *   because [resId] is only unique to a local graph. Nodes in sibling graphs can have the same
     *   id.
     */
    public fun findNodeComprehensive(
        resId: Int,
        lastVisited: NavDestination?,
        searchChildren: Boolean,
        matchingDest: NavDestination? = null,
    ): NavDestination?

    /**
     * Finds a destination in the collection by route. This will recursively check the
     * [parent][parent] of this navigation graph if node is not found in this navigation graph.
     *
     * @param route Route to locate
     * @return the node with route
     */
    public fun findNode(route: String?): NavDestination?

    /**
     * Finds a destination in the collection by route from [KClass]. This will recursively check the
     * [parent][parent] of this navigation graph if node is not found in this navigation graph.
     *
     * @param T Route from a [KClass] to locate
     * @return the node with route - the node must have been created with a route from [KClass]
     */
    public inline fun <reified T> findNode(): NavDestination?

    /**
     * Finds a destination in the collection by route from [KClass]. This will recursively check the
     * [parent][parent] of this navigation graph if node is not found in this navigation graph.
     *
     * @param route Route from a [KClass] to locate
     * @return the node with route - the node must have been created with a route from [KClass]
     */
    public fun findNode(route: KClass<*>): NavDestination?

    /**
     * Finds a destination in the collection by route from Object. This will recursively check the
     * [parent][parent] of this navigation graph if node is not found in this navigation graph.
     *
     * @param route Route to locate
     * @return the node with route - the node must have been created with a route from [KClass]
     */
    public fun <T> findNode(route: T?): NavDestination?

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public fun findNode(route: String, searchParents: Boolean): NavDestination?

    /** @throws NoSuchElementException if there no more elements */
    public final override fun iterator(): MutableIterator<NavDestination>

    /**
     * Add all destinations from another collection to this one. As each destination has at most one
     * parent, the destinations will be removed from the given NavGraph.
     *
     * @param other collection of destinations to add. All destinations will be removed from this
     *   graph after being added to this graph.
     */
    public fun addAll(other: NavGraph)

    /**
     * Remove a given destination from this NavGraph
     *
     * @param node the destination to remove.
     */
    public fun remove(node: NavDestination)

    /** Clear all destinations from this navigation graph. */
    public fun clear()

    override val displayName: String

    /**
     * The starting destination id for this NavGraph. When navigating to the NavGraph, the
     * destination represented by this id is the one the user will initially see.
     */
    public var startDestinationId: Int
        private set

    /**
     * Sets the starting destination for this NavGraph.
     *
     * This will override any previously set [startDestinationId]
     *
     * @param startDestRoute The route of the destination to be shown when navigating to this
     *   NavGraph.
     */
    public fun setStartDestination(startDestRoute: String)

    /**
     * Sets the starting destination for this NavGraph.
     *
     * This will override any previously set [startDestinationId]
     *
     * @param T The route of the destination as a [KClass] to be shown when navigating to this
     *   NavGraph.
     */
    public inline fun <reified T : Any> setStartDestination()

    /**
     * Sets the starting destination for this NavGraph.
     *
     * This will override any previously set [startDestinationId]
     *
     * @param startDestRoute The [KClass] of route [T] to be shown when navigating to this NavGraph.
     */
    @JvmSynthetic public fun <T : Any> setStartDestination(startDestRoute: KClass<T>)

    /**
     * Sets the starting destination for this NavGraph.
     *
     * This will override any previously set [startDestinationId]
     *
     * @param startDestRoute The route of the destination as an object to be shown when navigating
     *   to this NavGraph.
     */
    public fun <T : Any> setStartDestination(startDestRoute: T)

    // unfortunately needs to be public so reified setStartDestination can access this
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public fun <T> setStartDestination(
        serializer: KSerializer<T>,
        parseRoute: (NavDestination) -> String,
    )

    /**
     * The route for the starting destination for this NavGraph. When navigating to the NavGraph,
     * the destination represented by this route is the one the user will initially see.
     */
    public var startDestinationRoute: String?
        private set

    public val startDestDisplayName: String

    public companion object {
        /**
         * Finds the actual start destination of the graph, handling cases where the graph's
         * starting destination is itself a NavGraph.
         *
         * @return the actual startDestination of the given graph.
         */
        @JvmStatic public fun NavGraph.findStartDestination(): NavDestination

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public fun NavGraph.childHierarchy(): Sequence<NavDestination>
    }
}

/**
 * Returns the destination with `route`.
 *
 * @throws IllegalArgumentException if no destination is found with that route.
 */
@Suppress("NOTHING_TO_INLINE")
public inline operator fun NavGraph.get(route: String): NavDestination =
    findNode(route)
        ?: throw IllegalArgumentException("No destination for $route was found in $this")

/**
 * Returns the destination with `route` from [KClass].
 *
 * @throws IllegalArgumentException if no destination is found with that route.
 */
@Suppress("NOTHING_TO_INLINE")
public inline operator fun <reified T : Any> NavGraph.get(route: KClass<T>): NavDestination =
    findNode<T>() ?: throw IllegalArgumentException("No destination for $route was found in $this")

/**
 * Returns the destination with `route` from an Object.
 *
 * @throws IllegalArgumentException if no destination is found with that route.
 */
@Suppress("NOTHING_TO_INLINE")
public inline operator fun <T : Any> NavGraph.get(route: T): NavDestination =
    findNode(route)
        ?: throw IllegalArgumentException("No destination for $route was found in $this")

/** Returns `true` if a destination with `route` is found in this navigation graph. */
public operator fun NavGraph.contains(route: String): Boolean = findNode(route) != null

/** Returns `true` if a destination with `route` is found in this navigation graph. */
@Suppress("UNUSED_PARAMETER")
public inline operator fun <reified T : Any> NavGraph.contains(route: KClass<T>): Boolean =
    findNode<T>() != null

/** Returns `true` if a destination with `route` is found in this navigation graph. */
public operator fun <T : Any> NavGraph.contains(route: T): Boolean = findNode(route) != null

/**
 * Adds a destination to this NavGraph. The destination must have an [id][NavDestination.id] set.
 *
 * The destination must not have a [parent][NavDestination.parent] set. If the destination is
 * already part of a [NavGraph], call [NavGraph.remove] before calling this method.</p>
 *
 * @param node destination to add
 */
@Suppress("NOTHING_TO_INLINE")
public inline operator fun NavGraph.plusAssign(node: NavDestination) {
    addDestination(node)
}

/**
 * Add all destinations from another collection to this one. As each destination has at most one
 * parent, the destinations will be removed from the given NavGraph.
 *
 * @param other collection of destinations to add. All destinations will be removed from the
 *   parameter graph after being added to this graph.
 */
@Suppress("NOTHING_TO_INLINE")
public inline operator fun NavGraph.plusAssign(other: NavGraph) {
    addAll(other)
}

/** Removes `node` from this navigation graph. */
@Suppress("NOTHING_TO_INLINE")
public inline operator fun NavGraph.minusAssign(node: NavDestination) {
    remove(node)
}
