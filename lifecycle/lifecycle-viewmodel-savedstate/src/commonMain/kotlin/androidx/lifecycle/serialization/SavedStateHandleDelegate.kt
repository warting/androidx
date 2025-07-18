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

package androidx.lifecycle.serialization

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.internal.canonicalName
import androidx.savedstate.SavedState
import androidx.savedstate.SavedStateRegistry.SavedStateProvider
import androidx.savedstate.read
import androidx.savedstate.savedState
import androidx.savedstate.serialization.SavedStateConfiguration
import androidx.savedstate.serialization.decodeFromSavedState
import androidx.savedstate.serialization.encodeToSavedState
import kotlin.jvm.JvmName
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty
import kotlinx.serialization.KSerializer
import kotlinx.serialization.serializer

/**
 * Returns a property delegate that uses [SavedStateHandle] to save and restore a value of type [T]
 * with the default serializer.
 *
 * @sample androidx.lifecycle.delegateExplicitKey
 * @param key An optional [String] key to use for storing the value in the [SavedStateHandle]. A
 *   default key will be generated if it's omitted or when 'null' is passed.
 * @param configuration The [SavedStateConfiguration] to use. Defaults to
 *   [SavedStateConfiguration.DEFAULT].
 * @param init The function to provide the initial value of the property.
 * @return A property delegate that manages the saving and restoring of the value.
 */
@Deprecated(
    message = "Use the new 'saved' overload that supports both nullable and non-nullable types.",
    level = DeprecationLevel.HIDDEN,
)
public inline fun <reified T : Any> SavedStateHandle.saved(
    key: String? = null,
    configuration: SavedStateConfiguration = SavedStateConfiguration.DEFAULT,
    noinline init: () -> T,
): ReadWriteProperty<Any?, T> {
    return saved(configuration.serializersModule.serializer(), key, configuration, init)
}

/**
 * Returns a property delegate that uses [SavedStateHandle] to save and restore a value of type [T]
 * with the default serializer.
 *
 * @sample androidx.lifecycle.delegateExplicitKey
 * @param key An optional [String] key to use for storing the value in the [SavedStateHandle]. A
 *   default key will be generated if it's omitted or when 'null' is passed.
 * @param configuration The [SavedStateConfiguration] to use. Defaults to
 *   [SavedStateConfiguration.DEFAULT].
 * @param init The function to provide the initial value of the property.
 * @return A property delegate that manages the saving and restoring of the value.
 */
@JvmName("savedNullable")
public inline fun <reified T> SavedStateHandle.saved(
    key: String? = null,
    configuration: SavedStateConfiguration = SavedStateConfiguration.DEFAULT,
    noinline init: () -> T,
): ReadWriteProperty<Any?, T> {
    return saved(configuration.serializersModule.serializer(), key, configuration, init)
}

/**
 * Returns a property delegate that uses [SavedStateHandle] to save and restore a value of type [T].
 *
 * @sample androidx.lifecycle.delegateExplicitKeyAndSerializer
 * @param serializer The [KSerializer] to use for serializing and deserializing the value.
 * @param key An optional [String] key to use for storing the value in the [SavedStateHandle]. A
 *   default key will be generated if it's omitted or when 'null' is passed.
 * @param configuration The [SavedStateConfiguration] to use. Defaults to
 *   [SavedStateConfiguration.DEFAULT].
 * @param init The function to provide the initial value of the property.
 * @return A property delegate that manages the saving and restoring of the value.
 */
@Deprecated(
    message = "Use the new 'saved' overload that supports both nullable and non-nullable types.",
    level = DeprecationLevel.HIDDEN,
)
public fun <T : Any> SavedStateHandle.saved(
    serializer: KSerializer<T>,
    key: String? = null,
    configuration: SavedStateConfiguration = SavedStateConfiguration.DEFAULT,
    init: () -> T,
): ReadWriteProperty<Any?, T> {
    return SavedStateHandleDelegate(this, serializer, key, configuration, init)
}

/**
 * Returns a property delegate that uses [SavedStateHandle] to save and restore a value of type [T].
 *
 * @sample androidx.lifecycle.delegateExplicitKeyAndSerializer
 * @param serializer The [KSerializer] to use for serializing and deserializing the value.
 * @param key An optional [String] key to use for storing the value in the [SavedStateHandle]. A
 *   default key will be generated if it's omitted or when 'null' is passed.
 * @param configuration The [SavedStateConfiguration] to use. Defaults to
 *   [SavedStateConfiguration.DEFAULT].
 * @param init The function to provide the initial value of the property.
 * @return A property delegate that manages the saving and restoring of the value.
 */
@JvmName("savedNullable")
public fun <T> SavedStateHandle.saved(
    serializer: KSerializer<T>,
    key: String? = null,
    configuration: SavedStateConfiguration = SavedStateConfiguration.DEFAULT,
    init: () -> T,
): ReadWriteProperty<Any?, T> {
    return SavedStateHandleDelegate(this, serializer, key, configuration, init)
}

/**
 * A property delegate that saves and restores a value in a [SavedStateHandle].
 *
 * @param T The type of the property.
 * @param savedStateHandle The [SavedStateHandle] to save and restore state.
 * @param serializer The [KSerializer] used for serialization and deserialization.
 * @param key Optional key to store the value. If null, a key is generated from the property's name.
 * @param configuration Configuration for serialization.
 * @param init A function that provides the initial value if no saved state exists.
 */
private class SavedStateHandleDelegate<T>(
    private val savedStateHandle: SavedStateHandle,
    private val serializer: KSerializer<T>,
    private val key: String?,
    private val configuration: SavedStateConfiguration,
    private val init: () -> T,
) : ReadWriteProperty<Any?, T>, SavedStateProvider {

    /**
     * A sentinel object to represent that the cached value is not yet initialized. This
     * distinguishes uninitialized from cached `null` values.
     */
    private object UNINITIALIZED

    // TODO(mgalhardo): encode/decode operations with SavedState requires a non-nullable type.
    //  To support nullable values, we rely on unchecked casts and brittle generic workarounds.
    //  Once the codec supports nullables directly, this logic can be simplified.
    private var cachedValue: Any? = UNINITIALIZED

    override fun getValue(thisRef: Any?, property: KProperty<*>): T {
        // On first read, register this as a SavedStateProvider and load the initial value.
        if (cachedValue == UNINITIALIZED) {
            val qualifiedKey = getQualifiedKey(thisRef, property)
            savedStateHandle.setSavedStateProvider(qualifiedKey, provider = this)
            cachedValue = loadInitialValue(qualifiedKey)
        }
        @Suppress("UNCHECKED_CAST")
        return cachedValue as T
    }

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        // On first write, register this as a SavedStateProvider if not already done.
        // We skip loading the initial value here to avoid overwriting the new value immediately.
        if (cachedValue == UNINITIALIZED) {
            val qualifiedKey = getQualifiedKey(thisRef, property)
            savedStateHandle.setSavedStateProvider(qualifiedKey, provider = this)
        }
        cachedValue = value
    }

    /**
     * Saves the current value.
     *
     * Returns an empty state if the value was never accessed. If the value is `null`, it saves a
     * special marker to distinguish a saved `null` from a state that was never saved. Otherwise, it
     * serializes the current value.
     */
    override fun saveState(): SavedState {
        // Don't save anything if the value was never even accessed.
        if (cachedValue == UNINITIALIZED) return savedState()

        // Using `putNull` distinguishes a saved `null` from a state that was never saved.
        @Suppress("UNCHECKED_CAST")
        val typedValue = cachedValue as? T ?: return savedState { putNull(key = "") }

        return encodeToSavedState(serializer, typedValue, configuration)
    }

    /**
     * Loads the value from saved state or provides an initial value.
     *
     * If no state is found for the [qualifiedKey], this falls back to the [init] lambda. It also
     * correctly restores a `null` value by checking for the special marker written by [saveState].
     */
    private fun loadInitialValue(qualifiedKey: String): T? {
        val restored = savedStateHandle.get<SavedState>(qualifiedKey) ?: return init()

        // Check for the special marker used in `saveState()` for a `null` value.
        if (restored.read { isNull(key = "") && size() == 1 }) return null

        @Suppress("UNCHECKED_CAST")
        return decodeFromSavedState(
            deserializer = serializer as KSerializer<T & Any>,
            savedState = restored,
            configuration = configuration,
        )
    }

    /** Generates a unique key for the property to avoid collisions in the [SavedStateHandle]. */
    private fun getQualifiedKey(thisRef: Any?, property: KProperty<*>): String {
        if (key != null) return key

        val classNamePrefix = if (thisRef != null) thisRef::class.canonicalName + "." else ""
        return classNamePrefix + property.name
    }
}
