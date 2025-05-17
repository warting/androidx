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

package androidx.wear.compose.materialcore

import android.provider.Settings
import android.text.format.DateFormat
import androidx.annotation.RestrictTo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Composable
public fun isLayoutDirectionRtl(): Boolean {
    val layoutDirection: LayoutDirection = LocalLayoutDirection.current
    return layoutDirection == LayoutDirection.Rtl
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Composable
public fun isRoundDevice(): Boolean {
    val configuration = LocalConfiguration.current
    return configuration.isScreenRound
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Composable
public fun isLeftyModeEnabled(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        Settings.System.getInt(
            context.contentResolver,
            Settings.System.USER_ROTATION,
            android.view.Surface.ROTATION_0,
        ) == android.view.Surface.ROTATION_180
    }
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Composable
public fun is24HourFormat(): Boolean = DateFormat.is24HourFormat(LocalContext.current)

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun currentTimeMillis(): Long = System.currentTimeMillis()

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Composable
public fun screenHeightDp(): Int = LocalConfiguration.current.screenHeightDp

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Composable
public fun screenHeightPx(): Int =
    with(LocalDensity.current) { LocalConfiguration.current.screenHeightDp.dp.roundToPx() }

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Composable
public fun screenWidthDp(): Int = LocalConfiguration.current.screenWidthDp

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Composable
public fun isSmallScreen(): Boolean =
    LocalConfiguration.current.screenWidthDp < LARGE_SCREEN_WIDTH_DP

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Composable
public fun isLargeScreen(): Boolean =
    LocalConfiguration.current.screenWidthDp >= LARGE_SCREEN_WIDTH_DP

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) public const val LARGE_SCREEN_WIDTH_DP: Int = 225
