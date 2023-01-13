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

import android.app.Activity
import androidx.core.util.Consumer
import java.util.concurrent.Executor

// TODO(b/191164045): Move to window-testing or adapt for testing otherwise.
internal interface EmbeddingBackend {
    fun setRules(rules: Set<EmbeddingRule>)

    fun getRules(): Set<EmbeddingRule>

    fun addRule(rule: EmbeddingRule)

    fun removeRule(rule: EmbeddingRule)

    fun addSplitListenerForActivity(
        activity: Activity,
        executor: Executor,
        callback: Consumer<List<SplitInfo>>
    )

    fun removeSplitListenerForActivity(
        consumer: Consumer<List<SplitInfo>>
    )

    fun isSplitSupported(): Boolean

    fun isActivityEmbedded(activity: Activity): Boolean

    fun setSplitAttributesCalculator(
        calculator: (SplitAttributesCalculatorParams) -> SplitAttributes
    )

    fun clearSplitAttributesCalculator()

    fun isSplitAttributesCalculatorSupported(): Boolean

    fun getActivityStack(activity: Activity): ActivityStack?
}