/*
 * Copyright (C) 2017 The Android Open Source Project
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

@file:JvmName("NavigatorProviderKt")
@file:JvmMultifileClass

package androidx.navigation

import androidx.annotation.CallSuper
import androidx.annotation.RestrictTo
import kotlin.jvm.JvmMultifileClass
import kotlin.jvm.JvmName
import kotlin.reflect.KClass

/**
 * A NavigationProvider stores a set of [Navigator]s that are valid ways to navigate to a
 * destination.
 */
public expect open class NavigatorProvider() {
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public val navigators: Map<String, Navigator<out NavDestination>>

    /**
     * Retrieves a registered [Navigator] by name.
     *
     * @param name name of the navigator to return
     * @return the registered navigator with the given name
     * @throws IllegalStateException if the Navigator has not been added
     * @see NavigatorProvider.addNavigator
     */
    @Suppress("UNCHECKED_CAST") public open fun <T : Navigator<*>> getNavigator(name: String): T

    /**
     * Register a navigator using the name provided by the
     * [Navigator.Name annotation][Navigator.Name]. [destinations][NavDestination] may refer to any
     * registered navigator by name for inflation. If a navigator by this name is already
     * registered, this new navigator will replace it.
     *
     * @param navigator navigator to add
     * @return the previously added Navigator for the name provided by the
     *   [Navigator.Name annotation][Navigator.Name], if any
     */
    public fun addNavigator(
        navigator: Navigator<out NavDestination>
    ): Navigator<out NavDestination>?

    /**
     * Register a navigator by name. [destinations][NavDestination] may refer to any registered
     * navigator by name for inflation. If a navigator by this name is already registered, this new
     * navigator will replace it.
     *
     * @param name name for this navigator
     * @param navigator navigator to add
     * @return the previously added Navigator for the given name, if any
     */
    @CallSuper
    public open fun addNavigator(
        name: String,
        navigator: Navigator<out NavDestination>,
    ): Navigator<out NavDestination>?
}

/**
 * Retrieves a registered [Navigator] by name.
 *
 * @throws IllegalStateException if the Navigator has not been added
 */
@Suppress("NOTHING_TO_INLINE")
public inline operator fun <T : Navigator<out NavDestination>> NavigatorProvider.get(
    name: String
): T = getNavigator(name)

/**
 * Retrieves a registered [Navigator] using the name provided by the
 * [Navigator.Name annotation][Navigator.Name].
 *
 * @throws IllegalStateException if the Navigator has not been added
 */
@Suppress("NOTHING_TO_INLINE")
public expect inline operator fun <T : Navigator<out NavDestination>> NavigatorProvider.get(
    clazz: KClass<T>
): T

/**
 * Register a [Navigator] by name. If a navigator by this name is already registered, this new
 * navigator will replace it.
 *
 * @return the previously added [Navigator] for the given name, if any
 */
@Suppress("NOTHING_TO_INLINE")
public inline operator fun NavigatorProvider.set(
    name: String,
    navigator: Navigator<out NavDestination>,
): Navigator<out NavDestination>? = addNavigator(name, navigator)

/**
 * Register a navigator using the name provided by the [Navigator.Name annotation][Navigator.Name].
 */
@Suppress("NOTHING_TO_INLINE")
public inline operator fun NavigatorProvider.plusAssign(navigator: Navigator<out NavDestination>) {
    addNavigator(navigator)
}
