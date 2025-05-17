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

package androidx.wear.watchface.client

import androidx.annotation.RestrictTo
import androidx.wear.watchface.data.DeviceConfig as WireDeviceConfig

/**
 * Describes the hardware configuration of the device the watch face is running on.
 *
 * @param hasLowBitAmbient Whether or not the watch hardware supports low bit ambient support.
 * @param hasBurnInProtection Whether or not the watch hardware supports burn in protection.
 * @param analogPreviewReferenceTimeMillis UTC reference time for screenshots of analog watch faces
 *   in milliseconds since the epoch.
 * @param digitalPreviewReferenceTimeMillis UTC reference time for screenshots of digital watch
 *   faces in milliseconds since the epoch.
 * @deprecated use Watch Face Format instead
 */
@Deprecated(
    message =
        "AndroidX watchface libraries are deprecated, use Watch Face Format instead. For more info see: https://developer.android.com/training/wearables/wff"
)
public class DeviceConfig(
    @get:JvmName("hasLowBitAmbient") public val hasLowBitAmbient: Boolean,
    @get:JvmName("hasBurnInProtection") public val hasBurnInProtection: Boolean,
    public val analogPreviewReferenceTimeMillis: Long,
    public val digitalPreviewReferenceTimeMillis: Long,
) {
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public fun asWireDeviceConfig(): WireDeviceConfig =
        WireDeviceConfig(
            hasLowBitAmbient,
            hasBurnInProtection,
            analogPreviewReferenceTimeMillis,
            digitalPreviewReferenceTimeMillis,
        )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as DeviceConfig

        if (hasLowBitAmbient != other.hasLowBitAmbient) {
            return false
        }
        if (hasBurnInProtection != other.hasBurnInProtection) {
            return false
        }
        if (analogPreviewReferenceTimeMillis != other.analogPreviewReferenceTimeMillis) {
            return false
        }
        if (digitalPreviewReferenceTimeMillis != other.digitalPreviewReferenceTimeMillis) {
            return false
        }
        return true
    }

    override fun hashCode(): Int {
        var result = hasLowBitAmbient.hashCode()
        result = 31 * result + hasBurnInProtection.hashCode()
        result = 31 * result + analogPreviewReferenceTimeMillis.hashCode()
        result = 31 * result + digitalPreviewReferenceTimeMillis.hashCode()
        return result
    }

    override fun toString(): String {
        return "DeviceConfig(hasLowBitAmbient=$hasLowBitAmbient, " +
            "hasBurnInProtection=$hasBurnInProtection, " +
            "analogPreviewReferenceTimeMillis=$analogPreviewReferenceTimeMillis, " +
            "digitalPreviewReferenceTimeMillis=$digitalPreviewReferenceTimeMillis)"
    }
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun WireDeviceConfig.asApiDeviceConfig(): DeviceConfig =
    DeviceConfig(
        hasLowBitAmbient,
        hasBurnInProtection,
        analogPreviewReferenceTimeMillis,
        digitalPreviewReferenceTimeMillis,
    )
