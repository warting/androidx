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

package androidx.savedstate

import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.viewtree.setViewTreeDisjointParent
import androidx.lifecycle.Lifecycle
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class ViewTreeSavedStateRegistryOwnerTest {
    /** Tests that a direct set/get on a single view survives a round trip */
    @Test
    fun setGetSameView() {
        val v = View(InstrumentationRegistry.getInstrumentation().context)

        assertWithMessage("initial SavedStateRegistryOwner expects null")
            .that(v.findViewTreeSavedStateRegistryOwner())
            .isNull()

        val fakeOwner: SavedStateRegistryOwner = FakeSavedStateRegistryOwner()
        v.setViewTreeSavedStateRegistryOwner(fakeOwner)

        assertWithMessage("get the SavedStateRegistryOwner set directly")
            .that(v.findViewTreeSavedStateRegistryOwner())
            .isEqualTo(fakeOwner)
    }

    /**
     * Tests that the owner set on a root of a subhierarchy is seen by both direct children and
     * other descendants
     */
    @Test
    fun getAncestorOwner() {
        val context = InstrumentationRegistry.getInstrumentation().context
        val root: ViewGroup = FrameLayout(context)
        val parent: ViewGroup = FrameLayout(context)
        val child = View(context)
        root.addView(parent)
        parent.addView(child)

        assertWithMessage("initial SavedStateRegistryOwner expects null")
            .that(child.findViewTreeSavedStateRegistryOwner())
            .isNull()

        val fakeOwner: SavedStateRegistryOwner = FakeSavedStateRegistryOwner()
        root.setViewTreeSavedStateRegistryOwner(fakeOwner)

        assertWithMessage("root sees owner")
            .that(root.findViewTreeSavedStateRegistryOwner())
            .isEqualTo(fakeOwner)
        assertWithMessage("direct child sees owner")
            .that(parent.findViewTreeSavedStateRegistryOwner())
            .isEqualTo(fakeOwner)
        assertWithMessage("grandchild sees owner")
            .that(child.findViewTreeSavedStateRegistryOwner())
            .isEqualTo(fakeOwner)
    }

    /**
     * Tests that a new owner set between a root and a descendant is seen by the descendant instead
     * of the root value
     */
    @Test
    fun shadowedOwner() {
        val context = InstrumentationRegistry.getInstrumentation().context
        val root: ViewGroup = FrameLayout(context)
        val parent: ViewGroup = FrameLayout(context)
        val child = View(context)
        root.addView(parent)
        parent.addView(child)

        assertWithMessage("initial SavedStateRegistryOwner expects null")
            .that(child.findViewTreeSavedStateRegistryOwner())
            .isNull()

        val rootFakeOwner: SavedStateRegistryOwner = FakeSavedStateRegistryOwner()
        root.setViewTreeSavedStateRegistryOwner(rootFakeOwner)

        val parentFakeOwner: SavedStateRegistryOwner = FakeSavedStateRegistryOwner()
        parent.setViewTreeSavedStateRegistryOwner(parentFakeOwner)

        assertWithMessage("root sees owner")
            .that(root.findViewTreeSavedStateRegistryOwner())
            .isEqualTo(rootFakeOwner)
        assertWithMessage("direct child sees owner")
            .that(parent.findViewTreeSavedStateRegistryOwner())
            .isEqualTo(parentFakeOwner)
        assertWithMessage("grandchild sees owner")
            .that(child.findViewTreeSavedStateRegistryOwner())
            .isEqualTo(parentFakeOwner)
    }

    @Test
    fun disjointParentOwner() {
        val context = getInstrumentation().context
        val root = FrameLayout(context)
        val disjointParent = FrameLayout(context)
        val parent = FrameLayout(context)
        val child = View(context)

        root.addView(disjointParent)
        parent.addView(child)
        parent.setViewTreeDisjointParent(disjointParent)

        val rootFakeOwner = FakeSavedStateRegistryOwner()
        root.setViewTreeSavedStateRegistryOwner(rootFakeOwner)

        assertEquals(
            "disjoint parent sees owner",
            rootFakeOwner,
            parent.findViewTreeSavedStateRegistryOwner(),
        )
        assertEquals(
            "disjoint child sees owner",
            rootFakeOwner,
            child.findViewTreeSavedStateRegistryOwner(),
        )
    }

    @Test
    fun shadowedDisjointParentOwner() {
        val context = getInstrumentation().context
        val root = FrameLayout(context)
        val disjointParent = FrameLayout(context)
        val parent = FrameLayout(context)
        val child = View(context)

        root.addView(disjointParent)
        parent.addView(child)
        parent.setViewTreeDisjointParent(disjointParent)

        val rootFakeOwner = FakeSavedStateRegistryOwner()
        val parentFakeOwner = FakeSavedStateRegistryOwner()
        root.setViewTreeSavedStateRegistryOwner(rootFakeOwner)
        parent.setViewTreeSavedStateRegistryOwner(parentFakeOwner)

        assertEquals(
            "child sees owner",
            parentFakeOwner,
            child.findViewTreeSavedStateRegistryOwner(),
        )
    }

    internal class FakeSavedStateRegistryOwner : SavedStateRegistryOwner {
        override val lifecycle: Lifecycle
            get() = throw UnsupportedOperationException("not a real SavedStateRegistryOwner")

        override val savedStateRegistry: SavedStateRegistry
            get() = throw UnsupportedOperationException("not a real SavedStateRegistryOwner")
    }
}
