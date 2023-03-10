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

package androidx.privacysandbox.sdkruntime.client.loader.impl

import android.os.IBinder
import androidx.privacysandbox.sdkruntime.core.SandboxedSdkCompat
import androidx.privacysandbox.sdkruntime.core.SandboxedSdkInfo
import androidx.privacysandbox.sdkruntime.core.controller.SdkSandboxControllerCompat
import java.lang.reflect.Constructor
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy

/**
 * Injects local implementation of [SdkSandboxControllerCompat.SandboxControllerImpl]
 * to [SdkSandboxControllerCompat] loaded by SDK Classloader.
 * Using [Proxy] to allow interaction between classes loaded by different classloaders.
 */
internal object SandboxControllerInjector {

    /**
     * Injects local implementation to SDK instance of [SdkSandboxControllerCompat].
     * 1) Retrieve [SdkSandboxControllerCompat] loaded by [sdkClassLoader]
     * 2) Create proxy that implements class from (1) and delegate to [controller]
     * 3) Call (via reflection) [SdkSandboxControllerCompat.injectLocalImpl] with proxy from (2)
     */
    fun inject(
        sdkClassLoader: ClassLoader,
        controller: SdkSandboxControllerCompat.SandboxControllerImpl
    ) {
        val controllerClass = Class.forName(
            SdkSandboxControllerCompat::class.java.name,
            false,
            sdkClassLoader
        )

        val controllerImplClass = Class.forName(
            SdkSandboxControllerCompat.SandboxControllerImpl::class.java.name,
            false,
            sdkClassLoader
        )

        val injectMethod = controllerClass.getMethod("injectLocalImpl", controllerImplClass)

        val sdkCompatBuilder = CompatSdkBuilder.createFor(sdkClassLoader)

        val proxy = Proxy.newProxyInstance(
            sdkClassLoader,
            arrayOf(controllerImplClass),
            Handler(
                controller,
                sdkCompatBuilder
            )
        )

        injectMethod.invoke(null, proxy)
    }

    private class Handler(
        private val controller: SdkSandboxControllerCompat.SandboxControllerImpl,
        private val compatSdkBuilder: CompatSdkBuilder
    ) : InvocationHandler {
        override fun invoke(proxy: Any, method: Method, args: Array<out Any?>?): Any {
            return when (method.name) {
                "getSandboxedSdks" -> getSandboxedSdks()
                else -> {
                    throw UnsupportedOperationException(
                        "Unexpected method call object:$proxy, method: $method, args: $args"
                    )
                }
            }
        }

        private fun getSandboxedSdks(): List<Any> {
            return controller
                .getSandboxedSdks()
                .map { compatSdkBuilder.createFrom(it) }
        }
    }

    private class CompatSdkBuilder(
        private val sandboxedSdkInfoConstructor: Constructor<out Any>,
        private val sandboxedSdkCompatConstructor: Constructor<out Any>,
    ) {
        /**
         * Creates instance of [SandboxedSdkCompat] class loaded by SDK Classloader.
         *
         * @param source instance of SandboxedSdkCompat loaded by app classloader.
         * @return instance of SandboxedSdkCompat loaded by SDK classloader.
         */
        fun createFrom(source: SandboxedSdkCompat): Any {
            val sdkInfo = createSdkInfoFrom(source.getSdkInfo())
            return sandboxedSdkCompatConstructor.newInstance(source.getInterface(), sdkInfo)
        }

        fun createSdkInfoFrom(source: SandboxedSdkInfo?): Any? {
            if (source == null) {
                return null
            }
            return sandboxedSdkInfoConstructor.newInstance(source.name, source.version)
        }

        companion object {
            fun createFor(classLoader: ClassLoader): CompatSdkBuilder {
                val sandboxedSdkCompatClass = Class.forName(
                    SandboxedSdkCompat::class.java.name,
                    /* initialize = */ false,
                    classLoader
                )
                val sandboxedSdkInfoClass = Class.forName(
                    SandboxedSdkInfo::class.java.name,
                    /* initialize = */ false,
                    classLoader
                )
                val sandboxedSdkCompatConstructor =
                    sandboxedSdkCompatClass.getConstructor(
                        /* parameter1 */ IBinder::class.java,
                        /* parameter2 */ sandboxedSdkInfoClass
                    )
                val sandboxedSdkInfoConstructor =
                    sandboxedSdkInfoClass.getConstructor(
                        /* parameter1 */ String::class.java,
                        /* parameter2 */ Long::class.java
                    )
                return CompatSdkBuilder(
                    sandboxedSdkInfoConstructor = sandboxedSdkInfoConstructor,
                    sandboxedSdkCompatConstructor = sandboxedSdkCompatConstructor
                )
            }
        }
    }
}