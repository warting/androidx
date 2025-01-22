/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.appfunctions

/** The configuration object used to customize AppFunction setup. */
public class AppFunctionConfiguration
internal constructor(
    /**
     * A map of [AppFunctionFactory] used to construct the enclosing classes of AppFunctions.
     *
     * The keys in this map are the enclosing classes of the AppFunctions to be constructed, and the
     * values are the corresponding [AppFunctionFactory] instance. If not provided in the map, the
     * default no-argument constructors will be used to construct the classes.
     */
    public val factories: Map<Class<*>, AppFunctionFactory<*>>
) {
    /**
     * A class to provide customized [AppFunctionConfiguration] object.
     *
     * To provide the configuration, implements the [AppFunctionConfiguration.Provider] interface on
     * your [android.app.Application] class.
     */
    public interface Provider {
        /** The [AppFunctionConfiguration] used to customize AppFunction setup. */
        public val appFunctionConfiguration: AppFunctionConfiguration
    }

    /** A builder for [AppFunctionConfiguration]. */
    public class Builder {

        private val factories = mutableMapOf<Class<*>, AppFunctionFactory<*>>()

        /**
         * Adds a [factory] instance for creating an [enclosingClass].
         *
         * If there is already a factory instance set for [enclosingClass], it will be overridden.
         */
        public fun <T : Any> addFactory(
            enclosingClass: Class<T>,
            factory: AppFunctionFactory<T>
        ): Builder {
            factories[enclosingClass] = factory
            return this
        }

        /** Builds the [AppFunctionConfiguration]. */
        public fun build(): AppFunctionConfiguration {
            return AppFunctionConfiguration(factories.toMap())
        }
    }
}
