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

package androidx.bluetooth

import android.bluetooth.BluetoothDevice
import androidx.annotation.IntDef
import androidx.annotation.RestrictTo

/**
 * Represents a Bluetooth device address type.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
@Retention(AnnotationRetention.SOURCE)
@IntDef(
    AddressType.ADDRESS_TYPE_PUBLIC,
    AddressType.ADDRESS_TYPE_RANDOM_STATIC,
    AddressType.ADDRESS_TYPE_RANDOM_RESOLVABLE,
    AddressType.ADDRESS_TYPE_RANDOM_NON_RESOLVABLE,
    AddressType.ADDRESS_TYPE_UNKNOWN
)
annotation class AddressType {
    companion object {
        /** Address type is public and registered with the IEEE. */
        const val ADDRESS_TYPE_PUBLIC: Int = BluetoothDevice.ADDRESS_TYPE_PUBLIC

        /** Address type is random static. */
        const val ADDRESS_TYPE_RANDOM_STATIC: Int = 1

        /** Address type is random resolvable. */
        const val ADDRESS_TYPE_RANDOM_RESOLVABLE: Int = 2

        /** Address type is random non resolvable. */
        const val ADDRESS_TYPE_RANDOM_NON_RESOLVABLE: Int = 3

        /** Address type is unknown. */
        const val ADDRESS_TYPE_UNKNOWN: Int = BluetoothDevice.ADDRESS_TYPE_UNKNOWN
    }
}