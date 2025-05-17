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

package androidx.wear.compose.material3.samples.icons

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.samples.R

@Composable
fun FavoriteIcon(size: Dp) {
    Icon(
        painter = painterResource(R.drawable.ic_favorite_rounded),
        contentDescription = "Favorite icon",
        modifier = Modifier.size(size),
    )
}

@Composable
fun AvatarIcon() {
    Icon(
        painter = painterResource(R.drawable.ic_account_circle),
        contentDescription = "Account",
        modifier = Modifier.size(ButtonDefaults.LargeIconSize),
    )
}

@Composable
fun CheckIcon(modifier: Modifier = Modifier.size(ButtonDefaults.IconSize)) {
    Icon(
        painter = painterResource(R.drawable.ic_check_rounded),
        contentDescription = "Check",
        modifier = modifier,
    )
}

@Composable
fun WifiOnIcon(modifier: Modifier = Modifier) {
    Icon(
        painter = painterResource(R.drawable.ic_wifi_on),
        contentDescription = "Wifi on",
        modifier = modifier.size(ButtonDefaults.IconSize),
    )
}

@Composable
fun WifiOffIcon(modifier: Modifier = Modifier) {
    Icon(
        painter = painterResource(R.drawable.ic_wifi_off),
        contentDescription = "Wifi off",
        modifier = modifier.size(ButtonDefaults.IconSize),
    )
}

@Composable
fun HeadphoneIcon(size: Dp) {
    Icon(
        painter = painterResource(R.drawable.ic_headphones),
        contentDescription = "Headphone icon",
        modifier = Modifier.size(size),
    )
}

@Composable
fun VolumeUpIcon(size: Dp) {
    Icon(
        painter = painterResource(R.drawable.ic_volume_up),
        contentDescription = "Volume up icon",
        modifier = Modifier.size(size),
    )
}

@Composable
fun VolumeDownIcon(size: Dp) {
    Icon(
        painter = painterResource(R.drawable.ic_volume_down),
        contentDescription = "Volume down icon",
        modifier = Modifier.size(size),
    )
}
