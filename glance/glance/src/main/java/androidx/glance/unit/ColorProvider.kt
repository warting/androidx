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

package androidx.glance.unit

import android.content.Context
import android.os.Build
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo
import androidx.compose.ui.graphics.Color

/** Provider of colors for a glance composable's attributes. */
public interface ColorProvider {
    /** Returns the color the provider would use in the given [context]. */
    public fun getColor(context: Context): Color
}

/** Returns a [ColorProvider] that always resolves to the [Color]. */
public fun ColorProvider(color: Color): ColorProvider {
    return FixedColorProvider(color)
}

/**
 * Returns a [ColorProvider] that resolves to the color resource. This should not be used outside of
 * the Glance Libraries due to inconsistencies with regards to what process (app vs launcher) colors
 * are resolved in
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun ColorProvider(@ColorRes resId: Int): ColorProvider {
    return ResourceColorProvider(resId)
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public data class FixedColorProvider(val color: Color) : ColorProvider {
    override fun getColor(context: Context): Color = color
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public data class ResourceColorProvider(@ColorRes public val resId: Int) : ColorProvider {
    override fun getColor(context: Context): Color {
        val androidColor =
            if (Build.VERSION.SDK_INT >= 23) {
                ColorProviderApi23Impl.getColor(context, resId)
            } else {
                @Suppress("DEPRECATION") // Resources.getColor must be used on < 23.
                context.resources.getColor(resId)
            }
        return Color(androidColor)
    }
}

@RequiresApi(23)
private object ColorProviderApi23Impl {
    @ColorInt
    fun getColor(context: Context, @ColorRes resId: Int): Int {
        return context.getColor(resId)
    }
}
