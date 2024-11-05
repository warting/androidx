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

package androidx.compose.runtime.tooling

import androidx.compose.runtime.Composition
import androidx.compose.runtime.CompositionImplServiceKey
import androidx.compose.runtime.ExperimentalComposeRuntimeApi
import androidx.compose.runtime.RecomposeScope
import androidx.compose.runtime.RecomposeScopeImpl
import androidx.compose.runtime.Recomposer
import androidx.compose.runtime.getCompositionService

/**
 * Observe when new compositions are added to a recomposer. This, combined with,
 * [CompositionObserver], allows observing when any composition is being performed.
 *
 * This observer is registered with a [Recomposer] by calling [Recomposer.observe].
 */
@ExperimentalComposeRuntimeApi
@Suppress("CallbackName")
interface CompositionRegistrationObserver {

    /**
     * Called whenever a [Composition] is registered with a [Recomposer] for which this is an
     * observer. A Composition is registered with its Recomposer when it begins its initial
     * composition, before any content is added. When a [CompositionRegistrationObserver] is
     * registered, this method will be called for all the [Recomposer]'s currently known
     * composition.
     *
     * This method is called on the same thread that the [Composition] being registered is being
     * composed on. During the initial dispatch, it is invoked on the same thread that the callback
     * is being registered on. Implementations of this method should be thread safe as they might be
     * called on an arbitrary thread.
     *
     * @param recomposer The [Recomposer] the [composition] was registered with. This is always the
     *   instance of the [Recomposer] that `observe` was called.
     * @param composition The [Composition] instance that is being registered with the recomposer.
     */
    fun onCompositionRegistered(recomposer: Recomposer, composition: Composition)

    /**
     * Called whenever a [Composition] is unregistered with a [Recomposer] for which this is an
     * observer. A Composition is unregistered from its Recomposer when the composition is
     * [disposed][Composition.dispose]. This method is called on the same thread that the
     * [Composition] being unregistered was composed on. Implementations of this method should be
     * thread safe as they might be called on an arbitrary thread.
     *
     * @param recomposer The [Recomposer] the [composition] was registered with. This is always the
     *   instance of the [Recomposer] that `observe` was called.
     * @param composition The [Composition] instance that is being unregistered with the recomposer.
     */
    fun onCompositionUnregistered(recomposer: Recomposer, composition: Composition)
}

/** Observe when the composition begins and ends. */
@ExperimentalComposeRuntimeApi
@Suppress("CallbackName")
interface CompositionObserver {
    /**
     * Called when the composition begins on the [composition]. The [invalidationMap] a map of
     * invalid recompose scopes that are scheduled to be recomposed. The [CompositionObserver] will
     * be called for the [composition].
     *
     * The scopes in the [invalidationMap] are not guaranteed to be composed. Some cases where they
     * are not composed are 1) the scope is no longer part of the composition (e.g the parent scope
     * no longer executed the code branch the scope was a part of) 2) the scope is part of movable
     * content that was moved out of the composition.
     *
     * In the case of movable content, the scope will be recomposed as part of a different
     * composition when it is moved to that composition or it might be discarded if no other
     * composition claims it.
     *
     * @param composition the composition that is beginning to be recomposed
     * @param invalidationMap the recompose scopes that will be recomposed by this composition. This
     *   list is empty for the initial composition.
     */
    fun onBeginComposition(composition: Composition, invalidationMap: Map<RecomposeScope, Set<Any>>)

    /** Called after composition has been completed for [composition]. */
    fun onEndComposition(composition: Composition)
}

/** Observer when a recompose scope is being recomposed or when the scope is disposed. */
@ExperimentalComposeRuntimeApi
@Suppress("CallbackName")
interface RecomposeScopeObserver {
    /** Called just before the recompose scope's recompose lambda is invoked. */
    fun onBeginScopeComposition(scope: RecomposeScope)

    /** Called just after the recompose scopes' recompose lambda returns. */
    fun onEndScopeComposition(scope: RecomposeScope)

    /** Called when the recompose scope is disposed. */
    fun onScopeDisposed(scope: RecomposeScope)
}

/**
 * The handle returned by [Composition.observe] and [RecomposeScope.observe]. Calling [dispose] will
 * prevent further composition observation events from being sent to the registered observer.
 */
@ExperimentalComposeRuntimeApi
interface CompositionObserverHandle {
    /** Unregister the observer. */
    fun dispose()
}

/**
 * Register an observer to be notified when a composition is added to or removed from the given
 * [Recomposer]. When this method is called, the observer will be notified of all currently
 * registered compositions per the documentation in
 * [CompositionRegistrationObserver.onCompositionRegistered].
 *
 * @param observer the observer that will be informed of new compositions registered with this
 *   [Recomposer].
 * @return a handle that allows the observer to be disposed and detached from the [Recomposer].
 */
@ExperimentalComposeRuntimeApi
fun Recomposer.observe(observer: CompositionRegistrationObserver): CompositionObserverHandle {
    return addCompositionRegistrationObserver(observer)
}

/**
 * Observe the composition. Calling this twice on the same composition will implicitly dispose the
 * previous observer. the [CompositionObserver] will be called for this composition and all
 * sub-composition, transitively, for which this composition is a context. If, however, [observe] is
 * called on a sub-composition, it will override the parent composition and notification for it and
 * all sub-composition of it, will go to its observer instead of the one registered for the parent.
 *
 * @param observer the observer that will be informed of composition events for this composition and
 *   all sub-compositions for which this composition is the composition context. Observing a
 *   composition will prevent the parent composition's observer from receiving composition events
 *   about this composition.
 * @return a handle that allows the observer to be disposed and detached from the composition.
 *   Disposing an observer for a composition with a parent observer will begin sending the events to
 *   the parent composition's observer. A `null` indicates the composition does not support being
 *   observed.
 */
@ExperimentalComposeRuntimeApi
fun Composition.observe(observer: CompositionObserver): CompositionObserverHandle? =
    getCompositionService(CompositionImplServiceKey)?.observe(observer)

/**
 * Observer when this scope recomposes.
 *
 * @param observer the observer that will be informed of recompose events for this scope.
 * @return a handle that allows the observer to be disposed and detached from the recompose scope.
 */
@ExperimentalComposeRuntimeApi
fun RecomposeScope.observe(observer: RecomposeScopeObserver): CompositionObserverHandle =
    (this as RecomposeScopeImpl).observe(observer)
