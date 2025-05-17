/*
 * Copyright 2018 The Android Open Source Project
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

@file:JvmName("NavGraphBuilderKt")
@file:JvmMultifileClass

package androidx.navigation

import kotlin.jvm.JvmMultifileClass
import kotlin.jvm.JvmName
import kotlin.jvm.JvmSuppressWildcards
import kotlin.reflect.KClass
import kotlin.reflect.KType

/**
 * Construct a new [NavGraph]
 *
 * @param startDestination the starting destination's route for this NavGraph
 * @param route the destination's unique route
 * @param builder the builder used to construct the graph
 * @return the newly constructed NavGraph
 */
public inline fun NavigatorProvider.navigation(
    startDestination: String,
    route: String? = null,
    builder: NavGraphBuilder.() -> Unit,
): NavGraph = NavGraphBuilder(this, startDestination, route).apply(builder).build()

/**
 * Construct a new [NavGraph]
 *
 * @param startDestination the starting destination's route from a [KClass] for this NavGraph. The
 *   respective NavDestination must be added with route from a [KClass] in order to match.
 * @param route the graph's unique route as a [KClass]
 * @param typeMap A mapping of KType to custom NavType<*> in the [route]. May be empty if [route]
 *   does not use custom NavTypes.
 * @param builder the builder used to construct the graph
 * @return the newly constructed NavGraph
 */
public inline fun NavigatorProvider.navigation(
    startDestination: KClass<*>,
    route: KClass<*>? = null,
    typeMap: Map<KType, @JvmSuppressWildcards NavType<*>> = emptyMap(),
    builder: NavGraphBuilder.() -> Unit,
): NavGraph = NavGraphBuilder(this, startDestination, route, typeMap).apply(builder).build()

/**
 * Construct a new [NavGraph]
 *
 * @param startDestination the starting destination's route from an Object for this NavGraph. The
 *   respective NavDestination must be added with route from a [KClass] in order to match.
 * @param route the graph's unique route as a [KClass]
 * @param typeMap A mapping of KType to custom NavType<*> in the [route]. May be empty if [route]
 *   does not use custom NavTypes.
 * @param builder the builder used to construct the graph
 * @return the newly constructed NavGraph
 */
public inline fun NavigatorProvider.navigation(
    startDestination: Any,
    route: KClass<*>? = null,
    typeMap: Map<KType, @JvmSuppressWildcards NavType<*>> = emptyMap(),
    builder: NavGraphBuilder.() -> Unit,
): NavGraph = NavGraphBuilder(this, startDestination, route, typeMap).apply(builder).build()

/**
 * Construct a nested [NavGraph]
 *
 * @param startDestination the starting destination's route for this NavGraph
 * @param route the destination's unique route
 * @param builder the builder used to construct the graph
 * @return the newly constructed nested NavGraph
 */
public inline fun NavGraphBuilder.navigation(
    startDestination: String,
    route: String,
    builder: NavGraphBuilder.() -> Unit,
): Unit = destination(NavGraphBuilder(provider, startDestination, route).apply(builder))

/**
 * Construct a nested [NavGraph]
 *
 * @param T the graph's unique route from a KClass<T>
 * @param startDestination the starting destination's route from a [KClass] for this NavGraph. The
 *   respective NavDestination must be added with route from a [KClass] in order to match.
 * @param typeMap A mapping of KType to custom NavType<*> in the [T]. May be empty if [T] does not
 *   use custom NavTypes.
 * @param builder the builder used to construct the graph
 * @return the newly constructed nested NavGraph
 */
public inline fun <reified T : Any> NavGraphBuilder.navigation(
    startDestination: KClass<*>,
    typeMap: Map<KType, @JvmSuppressWildcards NavType<*>> = emptyMap(),
    noinline builder: NavGraphBuilder.() -> Unit,
): Unit = navigation(T::class, startDestination, typeMap, builder)

/**
 * Construct a nested [NavGraph]
 *
 * @param route the graph's unique route from KClass<T>
 * @param startDestination the starting destination's route from a [KClass] for this NavGraph. The
 *   respective NavDestination must be added with route from a [KClass] in order to match.
 * @param typeMap A mapping of KType to custom NavType<*> in the [route]. May be empty if [route]
 *   does not use custom NavTypes.
 * @param builder the builder used to construct the graph
 * @return the newly constructed nested NavGraph
 */
public fun <T : Any> NavGraphBuilder.navigation(
    route: KClass<T>,
    startDestination: KClass<*>,
    typeMap: Map<KType, @JvmSuppressWildcards NavType<*>> = emptyMap(),
    builder: NavGraphBuilder.() -> Unit,
): Unit = destination(NavGraphBuilder(provider, startDestination, route, typeMap).apply(builder))

/**
 * Construct a nested [NavGraph]
 *
 * @param T the graph's unique route from a KClass<T>
 * @param startDestination the starting destination's route from an Object for this NavGraph. The
 *   respective NavDestination must be added with route from a [KClass] in order to match.
 * @param typeMap A mapping of KType to custom NavType<*> in the [T]. May be empty if [T] does not
 *   use custom NavTypes.
 * @param builder the builder used to construct the graph
 * @return the newly constructed nested NavGraph
 */
public inline fun <reified T : Any> NavGraphBuilder.navigation(
    startDestination: Any,
    typeMap: Map<KType, @JvmSuppressWildcards NavType<*>> = emptyMap(),
    noinline builder: NavGraphBuilder.() -> Unit,
): Unit = navigation(T::class, startDestination, typeMap, builder)

/**
 * Construct a nested [NavGraph]
 *
 * @param route the graph's unique route from a KClass<T>
 * @param startDestination the starting destination's route from an Object for this NavGraph. The
 *   respective NavDestination must be added with route from a [KClass] in order to match.
 * @param typeMap A mapping of KType to custom NavType<*> in the [route]. May be empty if [route]
 *   does not use custom NavTypes.
 * @param builder the builder used to construct the graph
 * @return the newly constructed nested NavGraph
 */
public fun <T : Any> NavGraphBuilder.navigation(
    route: KClass<T>,
    startDestination: Any,
    typeMap: Map<KType, @JvmSuppressWildcards NavType<*>> = emptyMap(),
    builder: NavGraphBuilder.() -> Unit,
): Unit = destination(NavGraphBuilder(provider, startDestination, route, typeMap).apply(builder))

/** DSL for constructing a new [NavGraph] */
@NavDestinationDsl
public expect open class NavGraphBuilder : NavDestinationBuilder<NavGraph> {
    /** The [NavGraphBuilder]'s [NavigatorProvider]. */
    public val provider: NavigatorProvider

    /**
     * DSL for constructing a new [NavGraph]
     *
     * @param provider navigator used to create the destination
     * @param startDestination the starting destination's route for this NavGraph
     * @param route the graph's unique route
     * @return the newly created NavGraph
     */
    public constructor(provider: NavigatorProvider, startDestination: String, route: String?)

    /**
     * DSL for constructing a new [NavGraph]
     *
     * @param provider navigator used to create the destination
     * @param startDestination the starting destination's route as a [KClass] for this NavGraph. The
     *   respective NavDestination must be added with route from a [KClass] in order to match.
     * @param route the graph's unique route as a [KClass]
     * @param typeMap A mapping of KType to custom NavType<*> in the [route]. May be empty if
     *   [route] does not use custom NavTypes.
     * @return the newly created NavGraph
     */
    public constructor(
        provider: NavigatorProvider,
        startDestination: KClass<*>,
        route: KClass<*>?,
        typeMap: Map<KType, @JvmSuppressWildcards NavType<*>>,
    )

    /**
     * DSL for constructing a new [NavGraph]
     *
     * @param provider navigator used to create the destination
     * @param startDestination the starting destination's route as an Object for this NavGraph. The
     *   respective NavDestination must be added with route from a [KClass] in order to match.
     * @param route the graph's unique route as a [KClass]
     * @param typeMap A mapping of KType to custom NavType<*> in the [route]. May be empty if
     *   [route] does not use custom NavTypes.
     * @return the newly created NavGraph
     */
    public constructor(
        provider: NavigatorProvider,
        startDestination: Any,
        route: KClass<*>?,
        typeMap: Map<KType, @JvmSuppressWildcards NavType<*>>,
    )

    /** Build and add a new destination to the [NavGraphBuilder] */
    public fun <D : NavDestination> destination(navDestination: NavDestinationBuilder<D>)

    /** Adds this destination to the [NavGraphBuilder] */
    public operator fun NavDestination.unaryPlus()

    /** Add the destination to the [NavGraphBuilder] */
    public fun addDestination(destination: NavDestination)

    override fun build(): NavGraph
}
