/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.compose.ui.platform

import android.content.ComponentCallbacks2
import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.compositionLocalWithComputedDefaultOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.LocalSaveableStateRegistry
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.res.ImageVectorCache
import androidx.compose.ui.res.ResourceIdCache
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.compose.LocalSavedStateRegistryOwner

/**
 * The Android [Configuration]. The [Configuration] is useful for determining how to organize the
 * UI.
 */
val LocalConfiguration =
    compositionLocalOf<Configuration> { noLocalProvidedFor("LocalConfiguration") }

/** Provides a [Context] that can be used by Android applications. */
val LocalContext = staticCompositionLocalOf<Context> { noLocalProvidedFor("LocalContext") }

/**
 * The Android [Resources]. This will be updated when [LocalConfiguration] changes, to ensure that
 * calls to APIs such as [Resources.getString] return updated values.
 */
val LocalResources =
    compositionLocalWithComputedDefaultOf<Resources> {
        // Read LocalConfiguration here to invalidate callers of LocalResources when the
        // configuration changes. This is preferable to explicitly providing the resources object
        // because the resources object can still have the same instance, even though the
        // configuration changed, which would mean that callers would not get invalidated. To
        // resolve that we would need to use neverEqualPolicy to force an invalidation even though
        // the Resources didn't change, but then that would cause invalidations every time the
        // providing Composable is recomposed, regardless of whether a configuration change happened
        // or not.
        LocalConfiguration.currentValue
        LocalContext.currentValue.resources
    }

internal val LocalImageVectorCache =
    staticCompositionLocalOf<ImageVectorCache> { noLocalProvidedFor("LocalImageVectorCache") }

internal val LocalResourceIdCache =
    staticCompositionLocalOf<ResourceIdCache> { noLocalProvidedFor("LocalResourceIdCache") }

@Deprecated(
    "Moved to lifecycle-runtime-compose library in androidx.lifecycle.compose package.",
    ReplaceWith("androidx.lifecycle.compose.LocalLifecycleOwner"),
)
actual val LocalLifecycleOwner
    get() = LocalLifecycleOwner

/** The CompositionLocal containing the current [SavedStateRegistryOwner]. */
@Deprecated(
    "Moved to savedstate-compose library in androidx.savedstate.compose package.",
    ReplaceWith("androidx.savedstate.compose.LocalSavedStateRegistryOwner"),
)
val LocalSavedStateRegistryOwner
    get() = LocalSavedStateRegistryOwner

/** The CompositionLocal containing the current Compose [View]. */
val LocalView = staticCompositionLocalOf<View> { noLocalProvidedFor("LocalView") }

@Composable
@OptIn(ExperimentalComposeUiApi::class)
internal fun ProvideAndroidCompositionLocals(
    owner: AndroidComposeView,
    content: @Composable () -> Unit,
) {
    val view = owner
    val context = view.context
    // Make a deep copy to compare to later, since the same configuration object will be mutated
    // as part of configuration changes
    var configuration by remember { mutableStateOf(Configuration(context.resources.configuration)) }

    owner.configurationChangeObserver = { configuration = Configuration(it) }

    val uriHandler = remember { AndroidUriHandler(context) }
    val viewTreeOwners =
        owner.viewTreeOwners
            ?: throw IllegalStateException(
                "Called when the ViewTreeOwnersAvailability is not yet in Available state"
            )

    val saveableStateRegistry = remember {
        DisposableSaveableStateRegistry(view, viewTreeOwners.savedStateRegistryOwner)
    }
    DisposableEffect(Unit) { onDispose { saveableStateRegistry.dispose() } }

    val hapticFeedback = remember {
        if (HapticDefaults.isPremiumVibratorEnabled(context)) {
            DefaultHapticFeedback(owner.view)
        } else {
            NoHapticFeedback()
        }
    }

    val imageVectorCache = obtainImageVectorCache(context, configuration)
    val resourceIdCache = obtainResourceIdCache(context)
    val scrollCaptureInProgress =
        LocalScrollCaptureInProgress.current or owner.scrollCaptureInProgress
    CompositionLocalProvider(
        LocalConfiguration provides configuration,
        LocalContext provides context,
        LocalLifecycleOwner provides viewTreeOwners.lifecycleOwner,
        LocalSavedStateRegistryOwner provides viewTreeOwners.savedStateRegistryOwner,
        LocalSaveableStateRegistry provides saveableStateRegistry,
        LocalView provides owner.view,
        LocalImageVectorCache provides imageVectorCache,
        LocalResourceIdCache provides resourceIdCache,
        LocalProvidableScrollCaptureInProgress provides scrollCaptureInProgress,
        LocalHapticFeedback provides hapticFeedback,
    ) {
        ProvideCommonCompositionLocals(owner = owner, uriHandler = uriHandler, content = content)
    }
}

@Stable
@Composable
private fun obtainResourceIdCache(context: Context): ResourceIdCache {
    val resourceIdCache = remember { ResourceIdCache() }
    val callbacks = remember {
        object : ComponentCallbacks2 {
            override fun onConfigurationChanged(newConfig: Configuration) {
                resourceIdCache.clear()
            }

            @Deprecated("This callback is superseded by onTrimMemory")
            override fun onLowMemory() {
                resourceIdCache.clear()
            }

            override fun onTrimMemory(level: Int) {
                resourceIdCache.clear()
            }
        }
    }
    DisposableEffect(resourceIdCache) {
        context.applicationContext.registerComponentCallbacks(callbacks)
        onDispose { context.applicationContext.unregisterComponentCallbacks(callbacks) }
    }
    return resourceIdCache
}

@Stable
@Composable
private fun obtainImageVectorCache(
    context: Context,
    configuration: Configuration?,
): ImageVectorCache {
    val imageVectorCache = remember { ImageVectorCache() }
    val currentConfiguration: Configuration = remember {
        Configuration().apply { configuration?.let { this.setTo(it) } }
    }
    val callbacks = remember {
        object : ComponentCallbacks2 {
            override fun onConfigurationChanged(configuration: Configuration) {
                val changedFlags = currentConfiguration.updateFrom(configuration)
                imageVectorCache.prune(changedFlags)
                currentConfiguration.setTo(configuration)
            }

            @Deprecated("This callback is superseded by onTrimMemory")
            override fun onLowMemory() {
                imageVectorCache.clear()
            }

            override fun onTrimMemory(level: Int) {
                imageVectorCache.clear()
            }
        }
    }
    DisposableEffect(imageVectorCache) {
        context.applicationContext.registerComponentCallbacks(callbacks)
        onDispose { context.applicationContext.unregisterComponentCallbacks(callbacks) }
    }
    return imageVectorCache
}

private fun noLocalProvidedFor(name: String): Nothing {
    error("CompositionLocal $name not present")
}
