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
package androidx.navigation

import androidx.annotation.MainThread
import androidx.annotation.RestrictTo
import androidx.lifecycle.HasDefaultViewModelProviderFactory
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.navigation.internal.NavContext
import androidx.navigation.serialization.decodeArguments
import androidx.savedstate.SavedState
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.savedState
import kotlin.reflect.KClass
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.serializer

/**
 * Representation of an entry in the back stack of a [androidx.navigation.NavController]. The
 * [Lifecycle], [ViewModelStore], and [SavedStateRegistry] provided via this object are valid for
 * the lifetime of this destination on the back stack: when this destination is popped off the back
 * stack, the lifecycle will be destroyed, state will no longer be saved, and ViewModels will be
 * cleared.
 */
public expect class NavBackStackEntry :
    LifecycleOwner,
    ViewModelStoreOwner,
    HasDefaultViewModelProviderFactory,
    SavedStateRegistryOwner {

    internal val context: NavContext?
    internal val immutableArgs: SavedState?
    internal var hostLifecycleState: Lifecycle.State
    internal val viewModelStoreProvider: NavViewModelStoreProvider?
    internal val savedState: SavedState?

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public constructor(entry: NavBackStackEntry, arguments: SavedState? = entry.arguments)

    public companion object {
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public fun create(
            context: NavContext?,
            destination: NavDestination,
            arguments: SavedState? = null,
            hostLifecycleState: Lifecycle.State = Lifecycle.State.CREATED,
            viewModelStoreProvider: NavViewModelStoreProvider? = null,
            id: String = randomUUID(),
            savedState: SavedState? = null,
        ): NavBackStackEntry

        internal fun randomUUID(): String
    }

    /**
     * The destination associated with this entry
     *
     * @return The destination that is currently visible to users
     */
    @set:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) public var destination: NavDestination

    /**
     * The unique ID that serves as the identity of this entry
     *
     * @return the unique ID of this entry
     */
    public val id: String

    /**
     * The arguments used for this entry. Note that the arguments of a NavBackStackEntry are
     * immutable and defined when you `navigate()` to the destination - changes you make to this
     * SavedState will not be reflected in future calls to this property.
     *
     * @return The arguments used when this entry was created
     */
    public val arguments: SavedState?

    /** The [SavedStateHandle] for this entry. */
    @get:MainThread public val savedStateHandle: SavedStateHandle

    /**
     * {@inheritDoc}
     *
     * If the [androidx.navigation.NavHost] has not called
     * [androidx.navigation.NavHostController.setLifecycleOwner], the Lifecycle will be capped at
     * [Lifecycle.State.CREATED].
     */
    override val lifecycle: Lifecycle

    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @set:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public var maxLifecycle: Lifecycle.State

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public fun handleLifecycleEvent(event: Lifecycle.Event)

    /** Update the state to be the lower of the two constraints: */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) public fun updateState()

    /**
     * {@inheritDoc}
     *
     * @throws IllegalStateException if called before the [lifecycle] has moved to
     *   [Lifecycle.State.CREATED] or before the [androidx.navigation.NavHost] has called
     *   [androidx.navigation.NavHostController.setViewModelStore].
     */
    public override val viewModelStore: ViewModelStore

    override val defaultViewModelProviderFactory: ViewModelProvider.Factory

    override val defaultViewModelCreationExtras: CreationExtras

    override val savedStateRegistry: SavedStateRegistry

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) public fun saveState(outBundle: SavedState)
}

/**
 * Returns route as an object of type [T]
 *
 * Extrapolates arguments from [NavBackStackEntry.arguments] and recreates object [T]
 *
 * @param [T] the entry's [NavDestination.route] as a [KClass]
 * @return A new instance of this entry's [NavDestination.route] as an object of type [T]
 */
public inline fun <reified T> NavBackStackEntry.toRoute(): T = toRoute(T::class)

/**
 * Returns route as an object of type [T]
 *
 * Extrapolates arguments from [NavBackStackEntry.arguments] and recreates object [T]
 *
 * @param [route] the entry's [NavDestination.route] as a [KClass]
 * @return A new instance of this entry's [NavDestination.route] as an object of type [T]
 */
@OptIn(InternalSerializationApi::class)
@Suppress("UNCHECKED_CAST")
public fun <T> NavBackStackEntry.toRoute(route: KClass<*>): T {
    val savedState = arguments ?: savedState()
    val typeMap = destination.arguments.mapValues { it.value.type }
    return route.serializer().decodeArguments(savedState, typeMap) as T
}
