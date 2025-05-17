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

package androidx.wear.protolayout.testing

import androidx.wear.protolayout.LayoutElementBuilders.Layout
import androidx.wear.protolayout.LayoutElementBuilders.LayoutElement
import androidx.wear.protolayout.LayoutElementBuilders.layoutElementFromProto
import androidx.wear.protolayout.expression.DynamicDataPair
import androidx.wear.protolayout.expression.dynamicDataMapOf

/** Provides the main entry point into testing by exposing methods to find a layout element. */
public class LayoutElementAssertionsProvider(layoutRoot: LayoutElement) {
    private val root: LayoutElement =
        layoutElementFromProto(layoutRoot.toLayoutElementProto(), null)
    private var context: TestContext = TestContext()

    public constructor(layout: Layout) : this(layout.root!!)

    /**
     * Injects the app state and/or platform data for evaluating the dynamic data applied on the
     * layout element under testing.
     */
    public fun withDynamicData(
        vararg dynamicDataPairs: DynamicDataPair<*>
    ): LayoutElementAssertionsProvider {
        context.addDynamicData(dynamicDataMapOf(*dynamicDataPairs))

        return this
    }

    /** Finds an element that matches the given condition. */
    public fun onElement(matcher: LayoutElementMatcher): LayoutElementAssertion {
        val elementDescription = "element matching '${matcher.description}'"
        return LayoutElementAssertion(
            elementDescription,
            searchElement(root, matcher, context),
            context,
        )
    }

    /**
     * Finds the top level element of the element tree added to this
     * [LayoutElementAssertionsProvider].
     */
    public fun onRoot(): LayoutElementAssertion = LayoutElementAssertion("root", root, context)

    // TODO - b/374944199: add onAllElement which returns a LayoutElementAssertionCollection

    private fun searchElement(
        root: LayoutElement?,
        matcher: LayoutElementMatcher,
        context: TestContext,
    ): LayoutElement? {
        if (root == null) return null
        if (matcher.matches(root, context)) return root
        return root.children.firstNotNullOfOrNull { searchElement(it, matcher, context) }
    }
}
