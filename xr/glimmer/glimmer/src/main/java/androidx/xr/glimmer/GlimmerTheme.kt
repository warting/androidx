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

package androidx.xr.glimmer

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocal
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.node.DelegatableNode

/**
 * Glimmer contains different theme subsystems to allow visual customization across an application.
 *
 * Components use properties provided here when retrieving default values.
 *
 * Any values that are not set will inherit the current value from the theme, falling back to the
 * defaults if there is no parent GlimmerTheme. This allows using a GlimmerTheme at the top of your
 * application, and then separate GlimmerTheme(s) for different screens / parts of your UI,
 * overriding only the parts of the theme definition that need to change.
 *
 * @param colors [Colors] used by components within this hierarchy
 * @param typography [Typography] used by components within this hierarchy
 * @param content The content that can retrieve values from this theme
 */
@Composable
public fun GlimmerTheme(
    colors: Colors = GlimmerTheme.colors,
    typography: Typography = GlimmerTheme.typography,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(
        _localGlimmerTheme provides GlimmerTheme(colors, typography),
        // TODO: b/413429405
        LocalIndication provides NoIndication,
        LocalTextStyle provides typography.bodySmall,
        content = content
    )
}

/**
 * Glimmer contains different theme subsystems to allow visual customization across an application.
 *
 * Components use properties provided here when retrieving default values.
 *
 * @property colors [Colors] used by Glimmer components
 * @property typography [Typography] used by Glimmer components
 */
@Immutable
public class GlimmerTheme(
    public val colors: Colors = Colors(),
    public val typography: Typography = Typography()
) {
    public companion object {
        /** Retrieves the current [Colors] at the call site's position in the hierarchy. */
        public val colors: Colors
            @Composable @ReadOnlyComposable get() = LocalGlimmerTheme.current.colors

        /** Retrieves the current [Typography] at the call site's position in the hierarchy. */
        public val typography: Typography
            @Composable @ReadOnlyComposable get() = LocalGlimmerTheme.current.typography

        /**
         * [CompositionLocal] providing [GlimmerTheme] throughout the hierarchy. You can use
         * properties in the companion object to access specific subsystems, for example [colors].
         * To provide a new value for this, use [GlimmerTheme]. This API is exposed to allow
         * retrieving values from inside CompositionLocalConsumerModifierNode implementations - in
         * most cases you should use [colors] and other properties directly.
         */
        public val LocalGlimmerTheme: CompositionLocal<GlimmerTheme>
            get() = _localGlimmerTheme
    }

    internal var defaultSurfaceBorderCached: BorderStroke? = null
}

private object NoIndication : IndicationNodeFactory {
    override fun create(interactionSource: InteractionSource): DelegatableNode {
        return object : Modifier.Node() {}
    }

    override fun equals(other: Any?): Boolean = other === this

    override fun hashCode(): Int = -5
}

/** Use [GlimmerTheme.LocalGlimmerTheme] to access this publicly. */
@Suppress("CompositionLocalNaming")
private val _localGlimmerTheme: ProvidableCompositionLocal<GlimmerTheme> =
    staticCompositionLocalOf {
        GlimmerTheme()
    }
