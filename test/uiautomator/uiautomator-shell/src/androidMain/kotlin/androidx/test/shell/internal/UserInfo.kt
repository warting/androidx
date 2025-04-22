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

package androidx.test.shell.internal

import android.os.Build
import android.os.Parcel
import android.os.Process

/** Allows accessing the current user id and some quick checks around android multiuser. */
internal object UserInfo {
    private val currentUserId: Int
        get() {
            // UserHandle does not have public api but it's parcelable so we can read through that.
            val parcel = Parcel.obtain()
            Process.myUserHandle().writeToParcel(parcel, 0)
            parcel.setDataPosition(0)
            return parcel.readInt()
        }

    val isNonPrimaryUser: Boolean
        get() = currentUserId > 0 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    val isUnsupportedNonPrimaryUser: Boolean
        get() = currentUserId > 0 && Build.VERSION.SDK_INT == Build.VERSION_CODES.R
}
