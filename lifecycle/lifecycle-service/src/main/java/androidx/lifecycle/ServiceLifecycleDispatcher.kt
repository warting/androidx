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
package androidx.lifecycle

import android.app.Service
import android.os.Handler
import android.os.Looper

/**
 * Helper class to dispatch lifecycle events for a Service. Use it only if it is impossible to use
 * [LifecycleService].
 *
 * @param provider [LifecycleOwner] for a service, usually it is a service itself
 */
public open class ServiceLifecycleDispatcher(provider: LifecycleOwner) {

    private val registry: LifecycleRegistry = LifecycleRegistry(provider)
    private val handler: Handler = Handler(Looper.getMainLooper())
    private var lastDispatchRunnable: DispatchRunnable? = null

    private fun postDispatchRunnable(event: Lifecycle.Event) {
        lastDispatchRunnable?.run()
        DispatchRunnable(registry, event).also {
            lastDispatchRunnable = it
            handler.postAtFrontOfQueue(it)
        }
    }

    /** Must be a first call in [Service.onCreate] method, even before super.onCreate call. */
    public open fun onServicePreSuperOnCreate() {
        postDispatchRunnable(Lifecycle.Event.ON_CREATE)
    }

    /** Must be a first call in [Service.onBind] method, even before super.onBind call. */
    public open fun onServicePreSuperOnBind() {
        postDispatchRunnable(Lifecycle.Event.ON_START)
    }

    /**
     * Must be a first call in [Service.onStart] or [Service.onStartCommand] methods, even before a
     * corresponding super call.
     */
    public open fun onServicePreSuperOnStart() {
        postDispatchRunnable(Lifecycle.Event.ON_START)
    }

    /** Must be a first call in [Service.onDestroy] method, even before super.OnDestroy call. */
    public open fun onServicePreSuperOnDestroy() {
        postDispatchRunnable(Lifecycle.Event.ON_STOP)
        postDispatchRunnable(Lifecycle.Event.ON_DESTROY)
    }

    /** [Lifecycle] for the given [LifecycleOwner] */
    public open val lifecycle: Lifecycle
        get() = registry

    internal class DispatchRunnable(
        private val registry: LifecycleRegistry,
        val event: Lifecycle.Event,
    ) : Runnable {
        private var wasExecuted = false

        override fun run() {
            if (!wasExecuted) {
                registry.handleLifecycleEvent(event)
                wasExecuted = true
            }
        }
    }
}
