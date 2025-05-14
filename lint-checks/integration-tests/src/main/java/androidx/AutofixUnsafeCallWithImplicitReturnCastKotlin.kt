/*
 * Copyright 2025 The Android Open Source Project
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
package androidx

import android.app.Notification
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.Icon
import androidx.annotation.RequiresApi

/**
 * Tests to ensure the generated lint fix does not leave in implicit casts from a new return type.
 */
@Suppress("unused")
abstract class AutofixUnsafeCallWithImplicitReturnCastKotlin {
    /** This method creates an AdaptiveIconDrawable and implicitly casts it to Drawable. */
    @RequiresApi(26)
    fun createAdaptiveIconDrawableReturnDrawable(): Drawable {
        return AdaptiveIconDrawable(null, null)
    }

    /** This method also creates an AdaptiveIconDrawable but does not cast it to Drawable. */
    @RequiresApi(26)
    fun createAndReturnAdaptiveIconDrawable(): AdaptiveIconDrawable {
        return AdaptiveIconDrawable(null, null)
    }

    /** This calls a method returning an Icon and implicitly casts it to Object. */
    @RequiresApi(26)
    fun methodReturnsIconAsObject(): Any {
        return Icon.createWithAdaptiveBitmap(null)
    }

    /** This calls a method returning an Icon and returns it as an Icon. */
    @RequiresApi(26)
    fun methodReturnsIconAsIcon(): Icon {
        return Icon.createWithAdaptiveBitmap(null)
    }

    /** This uses the constructed value as Notification.Style in a method call. */
    @RequiresApi(24)
    fun methodUsesStyleAsParam() {
        useStyle(Notification.DecoratedCustomViewStyle())
    }

    companion object {
        /** This is here so there's a method to use the DecoratedCustomViewStyle as a Style. */
        fun useStyle(style: Notification.Style?): Boolean {
            return false
        }
    }
}
