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

package androidx.window.embedding

import androidx.window.extensions.embedding.SplitInfo as OEMSplitInfo
import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.window.core.ConsumerAdapter
import androidx.window.core.ExtensionsUtil
import androidx.window.embedding.EmbeddingInterfaceCompat.EmbeddingCallbackInterface
import androidx.window.extensions.WindowExtensions
import androidx.window.extensions.WindowExtensions.VENDOR_API_LEVEL_2
import androidx.window.extensions.WindowExtensionsProvider
import androidx.window.extensions.core.util.function.Consumer
import androidx.window.extensions.embedding.ActivityEmbeddingComponent
import java.lang.reflect.Proxy

/**
 * Adapter implementation for different historical versions of activity embedding OEM interface in
 * [ActivityEmbeddingComponent]. Only supports the single current version in this implementation.
 */
internal class EmbeddingCompat constructor(
    private val embeddingExtension: ActivityEmbeddingComponent,
    private val adapter: EmbeddingAdapter,
    private val consumerAdapter: ConsumerAdapter,
    private val applicationContext: Context
) : EmbeddingInterfaceCompat {

    override fun setRules(rules: Set<EmbeddingRule>) {
        var hasSplitRule = false
        for (rule in rules) {
            if (rule is SplitRule) {
                hasSplitRule = true
                break
            }
        }
        if (hasSplitRule && !SplitController.getInstance(applicationContext).isSplitSupported()) {
            Log.e(
                TAG, "Cannot set SplitRule because ActivityEmbedding Split is not supported" +
                    " or PROPERTY_ACTIVITY_EMBEDDING_SPLITS_ENABLED is not set."
            )
            return
        }

        val r = adapter.translate(applicationContext, rules)
        embeddingExtension.setEmbeddingRules(r)
    }

    override fun setEmbeddingCallback(embeddingCallback: EmbeddingCallbackInterface) {
        if (ExtensionsUtil.safeVendorApiLevel < VENDOR_API_LEVEL_2) {
            consumerAdapter.addConsumer(
                embeddingExtension,
                List::class,
                "setSplitInfoCallback"
            ) { values ->
                val splitInfoList = values.filterIsInstance<OEMSplitInfo>()
                embeddingCallback.onSplitInfoChanged(adapter.translate(splitInfoList))
            }
        } else {
            val callback = Consumer<List<OEMSplitInfo>> { splitInfoList ->
                embeddingCallback.onSplitInfoChanged(adapter.translate(splitInfoList))
            }
            embeddingExtension.setSplitInfoCallback(callback)
        }
    }

    override fun isActivityEmbedded(activity: Activity): Boolean {
        return embeddingExtension.isActivityEmbedded(activity)
    }

    override fun setSplitAttributesCalculator(calculator: SplitAttributesCalculator) {
        if (!isSplitAttributesCalculatorSupported()) {
            throw UnsupportedOperationException("#setSplitAttributesCalculator is not supported " +
                "on the device.")
        }
        return embeddingExtension.setSplitAttributesCalculator(
            adapter.translateSplitAttributesCalculator(calculator)
        )
    }

    override fun clearSplitAttributesCalculator() {
        if (!isSplitAttributesCalculatorSupported()) {
            throw UnsupportedOperationException("#clearSplitAttributesCalculator is not " +
                "supported on the device.")
        }
        return embeddingExtension.clearSplitAttributesCalculator()
    }

    override fun isSplitAttributesCalculatorSupported(): Boolean =
        ExtensionsUtil.safeVendorApiLevel >= WindowExtensions.VENDOR_API_LEVEL_2

    companion object {
        const val DEBUG = true
        private const val TAG = "EmbeddingCompat"

        fun isEmbeddingAvailable(): Boolean {
            return try {
                WindowExtensionsProvider.getWindowExtensions().activityEmbeddingComponent != null
            } catch (e: NoClassDefFoundError) {
                if (DEBUG) {
                    Log.d(TAG, "Embedding extension version not found")
                }
                false
            } catch (e: UnsupportedOperationException) {
                if (DEBUG) {
                    Log.d(TAG, "Stub Extension")
                }
                false
            }
        }

        fun embeddingComponent(): ActivityEmbeddingComponent {
            return if (isEmbeddingAvailable()) {
                WindowExtensionsProvider.getWindowExtensions().activityEmbeddingComponent
                    ?: Proxy.newProxyInstance(
                        EmbeddingCompat::class.java.classLoader,
                        arrayOf(ActivityEmbeddingComponent::class.java)
                    ) { _, _, _ -> } as ActivityEmbeddingComponent
            } else {
                Proxy.newProxyInstance(
                    EmbeddingCompat::class.java.classLoader,
                    arrayOf(ActivityEmbeddingComponent::class.java)
                ) { _, _, _ -> } as ActivityEmbeddingComponent
            }
        }
    }
}