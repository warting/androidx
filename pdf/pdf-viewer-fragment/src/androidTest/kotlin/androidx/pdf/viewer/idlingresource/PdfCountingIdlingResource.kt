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

package androidx.pdf.viewer.idlingresource

import androidx.test.espresso.idling.CountingIdlingResource
import java.util.concurrent.atomic.AtomicInteger

/**
 * A wrapper around Espresso's [CountingIdlingResource] that will help to define idling resource for
 * any background work.
 */
internal class PdfCountingIdlingResource(private val resourceName: String) {

    val countingIdlingResource: CountingIdlingResource = CountingIdlingResource(resourceName)
    val decrementCounter: AtomicInteger = AtomicInteger()

    fun increment() {
        countingIdlingResource.increment()
        if (decrementCounter.get() > 0) {
            decrementCounter.andDecrement
            decrement()
        }
    }

    fun decrement() {
        // Check if idling resource is not already idle
        if (!countingIdlingResource.isIdleNow) {
            countingIdlingResource.decrement()
        } else {
            decrementCounter.andIncrement
        }
    }
}
