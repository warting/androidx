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

package androidx.glance.wear.tiles

import androidx.compose.runtime.Composable
import androidx.glance.Emittable
import androidx.glance.GlanceModifier
import androidx.glance.GlanceNode

/**
 * Add [LayoutElementBuilders.LayoutElement] into the glance composition.
 *
 * @param layoutElement the layout element to add to the composition
 */
@Composable
@Suppress("deprecation") // For backwards compatibility.
public fun AndroidLayoutElement(
    layoutElement: androidx.wear.tiles.LayoutElementBuilders.LayoutElement
) {
    GlanceNode(
        factory = ::EmittableAndroidLayoutElement,
        update = { this.set(layoutElement) { this.layoutElement = it } },
    )
}

@Suppress("deprecation") // For backwards compatibility.
internal class EmittableAndroidLayoutElement : Emittable {
    override var modifier: GlanceModifier = GlanceModifier
    lateinit var layoutElement: androidx.wear.tiles.LayoutElementBuilders.LayoutElement

    override fun copy(): Emittable =
        EmittableAndroidLayoutElement().also {
            it.modifier = modifier
            it.layoutElement = layoutElement
        }

    override fun toString(): String = "EmittableAndroidLayoutElement()"
}
