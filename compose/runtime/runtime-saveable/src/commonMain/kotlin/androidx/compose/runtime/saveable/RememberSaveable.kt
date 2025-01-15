/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.compose.runtime.saveable

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.RememberObserver
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.SnapshotMutationPolicy
import androidx.compose.runtime.currentCompositeKeyHashCode
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.neverEqualPolicy
import androidx.compose.runtime.referentialEqualityPolicy
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotMutableState
import androidx.compose.runtime.structuralEqualityPolicy
import androidx.compose.runtime.toString

/**
 * Remember the value produced by [init].
 *
 * It behaves similarly to [remember], but the stored value will survive the activity or process
 * recreation using the saved instance state mechanism (for example it happens when the screen is
 * rotated in the Android application).
 *
 * @sample androidx.compose.runtime.saveable.samples.RememberSaveable
 *
 * If you use it with types which can be stored inside the Bundle then it will be saved and restored
 * automatically using [autoSaver], otherwise you will need to provide a custom [Saver]
 * implementation via the [saver] param.
 *
 * @sample androidx.compose.runtime.saveable.samples.RememberSaveableCustomSaver
 *
 * You can use it with a value stored inside [androidx.compose.runtime.mutableStateOf].
 *
 * @sample androidx.compose.runtime.saveable.samples.RememberSaveableWithMutableState
 *
 * If the value inside the MutableState can be stored inside the Bundle it would be saved and
 * restored automatically, otherwise you will need to provide a custom [Saver] implementation via an
 * overload with which has `stateSaver` param.
 *
 * @sample androidx.compose.runtime.saveable.samples.RememberSaveableWithMutableStateAndCustomSaver
 * @param inputs A set of inputs such that, when any of them have changed, will cause the state to
 *   reset and [init] to be rerun. Note that state restoration DOES NOT validate against inputs
 *   provided before value was saved.
 * @param saver The [Saver] object which defines how the state is saved and restored.
 * @param key An optional key to be used as a key for the saved value. If not provided we use the
 *   automatically generated by the Compose runtime which is unique for the every exact code
 *   location in the composition tree
 * @param init A factory function to create the initial value of this state
 */
@Composable
fun <T : Any> rememberSaveable(
    vararg inputs: Any?,
    saver: Saver<T, out Any> = autoSaver(),
    key: String? = null,
    init: () -> T
): T {
    val compositeKey = currentCompositeKeyHashCode
    // key is the one provided by the user or the one generated by the compose runtime
    val finalKey =
        if (!key.isNullOrEmpty()) {
            key
        } else {
            compositeKey.toString(MaxSupportedRadix)
        }
    @Suppress("UNCHECKED_CAST") (saver as Saver<T, Any>)

    val registry = LocalSaveableStateRegistry.current

    val holder = remember {
        // value is restored using the registry or created via [init] lambda
        val restored = registry?.consumeRestored(finalKey)?.let { saver.restore(it) }
        val finalValue = restored ?: init()
        SaveableHolder(saver, registry, finalKey, finalValue, inputs)
    }

    val value = holder.getValueIfInputsDidntChange(inputs) ?: init()
    SideEffect { holder.update(saver, registry, finalKey, value, inputs) }

    return value
}

/**
 * Remember the value produced by [init].
 *
 * It behaves similarly to [remember], but the stored value will survive the activity or process
 * recreation using the saved instance state mechanism (for example it happens when the screen is
 * rotated in the Android application).
 *
 * Use this overload if you remember a mutable state with a type which can't be stored in the Bundle
 * so you have to provide a custom saver object.
 *
 * @sample androidx.compose.runtime.saveable.samples.RememberSaveableWithMutableStateAndCustomSaver
 * @param inputs A set of inputs such that, when any of them have changed, will cause the state to
 *   reset and [init] to be rerun. Note that state restoration DOES NOT validate against inputs
 *   provided before value was saved.
 * @param stateSaver The [Saver] object which defines how the value inside the MutableState is saved
 *   and restored.
 * @param key An optional key to be used as a key for the saved value. If not provided we use the
 *   automatically generated by the Compose runtime which is unique for the every exact code
 *   location in the composition tree
 * @param init A factory function to create the initial value of this state
 */
@Composable
fun <T> rememberSaveable(
    vararg inputs: Any?,
    stateSaver: Saver<T, out Any>,
    key: String? = null,
    init: () -> MutableState<T>
): MutableState<T> =
    rememberSaveable(*inputs, saver = mutableStateSaver(stateSaver), key = key, init = init)

private class SaveableHolder<T>(
    private var saver: Saver<T, Any>,
    private var registry: SaveableStateRegistry?,
    private var key: String,
    private var value: T,
    private var inputs: Array<out Any?>
) : SaverScope, RememberObserver {
    private var entry: SaveableStateRegistry.Entry? = null
    /** Value provider called by the registry. */
    private val valueProvider = {
        with(saver) { save(requireNotNull(value) { "Value should be initialized" }) }
    }

    fun update(
        saver: Saver<T, Any>,
        registry: SaveableStateRegistry?,
        key: String,
        value: T,
        inputs: Array<out Any?>
    ) {
        var entryIsOutdated = false
        if (this.registry !== registry) {
            this.registry = registry
            entryIsOutdated = true
        }
        if (this.key != key) {
            this.key = key
            entryIsOutdated = true
        }
        this.saver = saver
        this.value = value
        this.inputs = inputs
        if (entry != null && entryIsOutdated) {
            entry?.unregister()
            entry = null
            register()
        }
    }

    private fun register() {
        val registry = registry
        require(entry == null) { "entry($entry) is not null" }
        if (registry != null) {
            registry.requireCanBeSaved(valueProvider())
            entry = registry.registerProvider(key, valueProvider)
        }
    }

    override fun canBeSaved(value: Any): Boolean {
        val registry = registry
        return registry == null || registry.canBeSaved(value)
    }

    override fun onRemembered() {
        register()
    }

    override fun onForgotten() {
        entry?.unregister()
    }

    override fun onAbandoned() {
        entry?.unregister()
    }

    fun getValueIfInputsDidntChange(inputs: Array<out Any?>): T? {
        return if (inputs.contentEquals(this.inputs)) {
            value
        } else {
            null
        }
    }
}

@Suppress("UNCHECKED_CAST")
private fun <T> mutableStateSaver(inner: Saver<T, out Any>) =
    with(inner as Saver<T, Any>) {
        Saver<MutableState<T>, MutableState<Any?>>(
            save = { state ->
                require(state is SnapshotMutableState<T>) {
                    "If you use a custom MutableState implementation you have to write a custom " +
                        "Saver and pass it as a saver param to rememberSaveable()"
                }
                val saved = save(state.value)
                if (saved != null) {
                    mutableStateOf(saved, state.policy as SnapshotMutationPolicy<Any?>)
                } else {
                    // if the inner saver returned null we need to return null as well so the
                    // user's init lambda will be used instead of restoring mutableStateOf(null)
                    null
                }
            },
            restore =
                @Suppress("UNCHECKED_CAST", "ExceptionMessage") {
                    require(it is SnapshotMutableState<Any?>)
                    mutableStateOf(
                        if (it.value != null) restore(it.value!!) else null,
                        it.policy as SnapshotMutationPolicy<T?>
                    )
                        as MutableState<T>
                }
        )
    }

private fun SaveableStateRegistry.requireCanBeSaved(value: Any?) {
    if (value != null && !canBeSaved(value)) {
        throw IllegalArgumentException(
            if (value is SnapshotMutableState<*>) {
                if (
                    value.policy !== neverEqualPolicy<Any?>() &&
                        value.policy !== structuralEqualityPolicy<Any?>() &&
                        value.policy !== referentialEqualityPolicy<Any?>()
                ) {
                    "If you use a custom SnapshotMutationPolicy for your MutableState you have to" +
                        " write a custom Saver"
                } else {
                    "MutableState containing ${value.value} cannot be saved using the current " +
                        "SaveableStateRegistry. The default implementation only supports types " +
                        "which can be stored inside the Bundle. Please consider implementing a " +
                        "custom Saver for this class and pass it as a stateSaver parameter to " +
                        "rememberSaveable()."
                }
            } else {
                generateCannotBeSavedErrorMessage(value)
            }
        )
    }
}

internal fun generateCannotBeSavedErrorMessage(value: Any): String =
    "$value cannot be saved using the current SaveableStateRegistry. The default " +
        "implementation only supports types which can be stored inside the Bundle" +
        ". Please consider implementing a custom Saver for this class and pass it" +
        " to rememberSaveable()."

/** The maximum radix available for conversion to and from strings. */
private val MaxSupportedRadix = 36
