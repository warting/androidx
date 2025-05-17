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

package androidx.navigation.serialization

import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavType
import androidx.savedstate.SavedState
import androidx.savedstate.read
import androidx.savedstate.savedState
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.AbstractDecoder
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.modules.EmptySerializersModule
import kotlinx.serialization.modules.SerializersModule

/**
 * Decoder to deserialize a SavedState of argument back into an object instance of type [T]
 *
 * This decoder iterates through every class field (argument) in [T], retrieves the value for that
 * argument from the SavedState (or fallback to default value), then use the retrieved values to
 * re-create the object instance.
 */
@OptIn(ExperimentalSerializationApi::class)
internal class RouteDecoder : AbstractDecoder {

    private val store: ArgStore
    private var elementIndex: Int = -1
    private var elementName: String = ""

    // SavedState as argument source
    constructor(bundle: SavedState, typeMap: Map<String, NavType<*>>) {
        this.store = SavedStateArgStore(bundle, typeMap)
    }

    // SavedStateHandle as argument source
    constructor(handle: SavedStateHandle, typeMap: Map<String, NavType<*>>) {
        this.store = SavedStateHandleArgStore(handle, typeMap)
    }

    @Suppress("DEPRECATION") // deprecated in 1.6.3
    override val serializersModule: SerializersModule = EmptySerializersModule

    /**
     * Decodes the index of the next element to be decoded. Index represents a position of the
     * current element in the [descriptor] that can be found with [descriptor].getElementIndex.
     *
     * The returned index will trigger deserializer to call [decodeValue] on the argument at that
     * index.
     *
     * The decoder continually calls this method to process the next available argument until this
     * method returns [CompositeDecoder.DECODE_DONE], which indicates that there are no more
     * arguments to decode.
     *
     * This method should sequentially return the element index for every element that has its value
     * available within the [ArgStore].
     */
    override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
        var currentIndex = elementIndex
        while (true) {
            // proceed to next element
            currentIndex++
            // if we have reached the end, let decoder know there are not more arguments to decode
            if (currentIndex >= descriptor.elementsCount) return CompositeDecoder.DECODE_DONE
            val currentName = descriptor.getElementName(currentIndex)
            // Check if bundle has argument value. If so, we tell decoder to process
            // currentIndex. Otherwise, we skip this index and proceed to next index.
            if (store.contains(currentName)) {
                elementIndex = currentIndex
                elementName = currentName
                return elementIndex
            }
        }
    }

    /**
     * Returns argument value from the [ArgStore] for the argument at the index returned from
     * [decodeElementIndex]
     */
    override fun decodeValue(): Any = internalDecodeValue()

    override fun decodeNull(): Nothing? = null

    // we want to know if it is not null, so its !isNull
    override fun decodeNotNullMark(): Boolean = store.get(elementName) != null

    /** To handle value class */
    override fun decodeInline(
        descriptor: SerialDescriptor
    ): kotlinx.serialization.encoding.Decoder {
        if (descriptor.isValueClass()) {
            elementName = descriptor.getElementName(0)
            elementIndex = 0
        }
        return super.decodeInline(descriptor)
    }

    /**
     * Entry point to decoding the route
     *
     * The original entry point was [decodeSerializableValue], however we needed to override it to
     * handle nested serializable values without recursing into the nested serializable
     * (non-primitives). So this is our new entry point which calls super.decodeSerializableValue to
     * deserialize only the route.
     */
    internal fun <T> decodeRouteWithArgs(deserializer: DeserializationStrategy<T>): T =
        super.decodeSerializableValue(deserializer)

    /**
     * Decodes the arguments within the route.
     *
     * Handles both primitives and non-primitives in three scenarios:
     * 1. nullable primitives with non-null value
     * 2. nullable non-primitive with non-null value
     * 3. non-nullable non-primitive values
     */
    @Suppress("UNCHECKED_CAST")
    override fun <T> decodeSerializableValue(deserializer: DeserializationStrategy<T>): T =
        internalDecodeValue() as T

    /**
     * [internalDecodeValue] should only be called for arguments with values stored within [store].
     * Otherwise, we should let the deserializer fall back to default value. This is done by
     * skipping (not returning) the indices whose argument is not present in the bundle. In doing
     * so, the deserializer considers the skipped element un-processed and will use the default
     * value (if present) instead. For index skipping, see [decodeElementIndex]
     */
    private fun internalDecodeValue(): Any {
        val arg = store.get(elementName)
        checkNotNull(arg) { "Unexpected null value for non-nullable argument $elementName" }
        return arg
    }
}

// key-value map of argument values where the key is argument name
private abstract class ArgStore {
    // Retrieves argument value from store
    abstract fun get(key: String): Any?

    // Checks if store contains argument for key
    abstract fun contains(key: String): Boolean
}

private class SavedStateHandleArgStore(
    private val handle: SavedStateHandle,
    private val typeMap: Map<String, NavType<*>>,
) : ArgStore() {
    override fun get(key: String): Any? {
        val arg: Any? = handle[key]
        val savedState = savedState(mapOf(key to arg))
        return checkNotNull(typeMap[key]) { "Failed to find type for $key when decoding $handle" }[
            savedState, key]
    }

    override fun contains(key: String) = handle.contains(key)
}

private class SavedStateArgStore(
    private val savedState: SavedState,
    private val typeMap: Map<String, NavType<*>>,
) : ArgStore() {
    override fun get(key: String): Any? {
        val navType = typeMap[key]
        return navType?.get(savedState, key)
    }

    override fun contains(key: String): Boolean {
        return savedState.read { contains(key) }
    }
}
