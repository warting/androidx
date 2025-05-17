/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.health.connect.client.units

/**
 * Represents a unit of pressure. Supported units:
 * - millimeters of Mercury (mmHg) - see [Pressure.millimetersOfMercury],
 *   [Double.millimetersOfMercury].
 */
class Pressure private constructor(private val value: Double) : Comparable<Pressure> {

    /** Returns the pressure in millimeters of Mercury (mmHg). */
    @get:JvmName("getMillimetersOfMercury")
    val inMillimetersOfMercury: Double
        get() = value

    /** Returns zero [Pressure] of the same type (currently there is only one type - mmHg). */
    internal fun zero(): Pressure = ZERO

    override fun compareTo(other: Pressure): Int = value.compareTo(other.value)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Pressure) return false

        return value == other.value
    }

    override fun hashCode(): Int = value.hashCode()

    override fun toString(): String = "$value mmHg"

    companion object {
        private val ZERO = Pressure(value = 0.0)

        /** Creates [Pressure] with the specified value in millimeters of Mercury (mmHg). */
        @JvmStatic fun millimetersOfMercury(value: Double): Pressure = Pressure(value)
    }
}

/** Creates [Pressure] with the specified value in millimeters of Mercury (mmHg). */
@get:JvmSynthetic
val Double.millimetersOfMercury: Pressure
    get() = Pressure.millimetersOfMercury(value = this)

/** Creates [Pressure] with the specified value in millimeters of Mercury (mmHg). */
@get:JvmSynthetic
val Long.millimetersOfMercury: Pressure
    get() = toDouble().millimetersOfMercury

/** Creates [Pressure] with the specified value in millimeters of Mercury (mmHg). */
@get:JvmSynthetic
val Float.millimetersOfMercury: Pressure
    get() = toDouble().millimetersOfMercury

/** Creates [Pressure] with the specified value in millimeters of Mercury (mmHg). */
@get:JvmSynthetic
val Int.millimetersOfMercury: Pressure
    get() = toDouble().millimetersOfMercury
