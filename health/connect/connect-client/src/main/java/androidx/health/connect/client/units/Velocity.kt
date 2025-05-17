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
 * Represents a unit of speed. Supported units:
 * - metersPerSecond - see [Velocity.metersPerSecond], [Double.metersPerSecond]
 * - kilometersPerHour - see [Velocity.kilometersPerHour], [Double.kilometersPerHour]
 * - milesPerHour - see [Velocity.milesPerHour], [Double.milesPerHour]
 */
class Velocity private constructor(private val value: Double, private val type: Type) :
    Comparable<Velocity> {

    /** Returns the velocity in meters per second. */
    @get:JvmName("getMetersPerSecond")
    val inMetersPerSecond: Double
        get() = value * type.metersPerSecondPerUnit

    /** Returns the velocity in kilometers per hour. */
    @get:JvmName("getKilometersPerHour")
    val inKilometersPerHour: Double
        get() = get(type = Type.KILOMETERS_PER_HOUR)

    /** Returns the velocity in miles per hour. */
    @get:JvmName("getMilesPerHour")
    val inMilesPerHour: Double
        get() = get(type = Type.MILES_PER_HOUR)

    private fun get(type: Type): Double =
        if (this.type == type) value else inMetersPerSecond / type.metersPerSecondPerUnit

    /** Returns zero [Velocity] of the same [Type]. */
    internal fun zero(): Velocity = ZEROS.getValue(type)

    override fun compareTo(other: Velocity): Int =
        if (type == other.type) {
            value.compareTo(other.value)
        } else {
            inMetersPerSecond.compareTo(other.inMetersPerSecond)
        }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Velocity) return false

        if (type == other.type) {
            return value == other.value
        }

        return inMetersPerSecond == other.inMetersPerSecond
    }

    override fun hashCode(): Int = inMetersPerSecond.hashCode()

    override fun toString(): String = "$value ${type.title}"

    companion object {
        private val ZEROS = Type.values().associateWith { Velocity(value = 0.0, type = it) }

        /** Creates [Velocity] with the specified value in meters per second. */
        @JvmStatic
        fun metersPerSecond(value: Double): Velocity = Velocity(value, Type.METERS_PER_SECOND)

        /** Creates [Velocity] with the specified value in kilometers per hour. */
        @JvmStatic
        fun kilometersPerHour(value: Double): Velocity = Velocity(value, Type.KILOMETERS_PER_HOUR)

        /** Creates [Velocity] with the specified value in miles per hour. */
        @JvmStatic fun milesPerHour(value: Double): Velocity = Velocity(value, Type.MILES_PER_HOUR)
    }

    private enum class Type {
        METERS_PER_SECOND {
            override val metersPerSecondPerUnit: Double = 1.0
            override val title: String = "meters/sec"
        },
        KILOMETERS_PER_HOUR {
            override val metersPerSecondPerUnit: Double = 1.0 / 3.6
            override val title: String = "km/h"
        },
        MILES_PER_HOUR {
            override val metersPerSecondPerUnit: Double = 0.447040357632
            override val title: String = "miles/h"
        };

        abstract val metersPerSecondPerUnit: Double
        abstract val title: String
    }
}

/** Creates [Velocity] with the specified value in meters per second. */
@get:JvmSynthetic
val Double.metersPerSecond: Velocity
    get() = Velocity.metersPerSecond(value = this)

/** Creates [Velocity] with the specified value in meters per second. */
@get:JvmSynthetic
val Long.metersPerSecond: Velocity
    get() = toDouble().metersPerSecond

/** Creates [Velocity] with the specified value in meters per second. */
@get:JvmSynthetic
val Float.metersPerSecond: Velocity
    get() = toDouble().metersPerSecond

/** Creates [Velocity] with the specified value in meters per second. */
@get:JvmSynthetic
val Int.metersPerSecond: Velocity
    get() = toDouble().metersPerSecond

/** Creates [Velocity] with the specified value in kilometers per hour. */
@get:JvmSynthetic
val Double.kilometersPerHour: Velocity
    get() = Velocity.kilometersPerHour(value = this)

/** Creates [Velocity] with the specified value in kilometers per hour. */
@get:JvmSynthetic
val Long.kilometersPerHour: Velocity
    get() = toDouble().kilometersPerHour

/** Creates [Velocity] with the specified value in kilometers per hour. */
@get:JvmSynthetic
val Float.kilometersPerHour: Velocity
    get() = toDouble().kilometersPerHour

/** Creates [Velocity] with the specified value in kilometers per hour. */
@get:JvmSynthetic
val Int.kilometersPerHour: Velocity
    get() = toDouble().kilometersPerHour

/** Creates [Velocity] with the specified value in miles per hour. */
@get:JvmSynthetic
val Double.milesPerHour: Velocity
    get() = Velocity.milesPerHour(value = this)

/** Creates [Velocity] with the specified value in miles per hour. */
@get:JvmSynthetic
val Long.milesPerHour: Velocity
    get() = toDouble().milesPerHour

/** Creates [Velocity] with the specified value in miles per hour. */
@get:JvmSynthetic
val Float.milesPerHour: Velocity
    get() = toDouble().milesPerHour

/** Creates [Velocity] with the specified value in miles per hour. */
@get:JvmSynthetic
val Int.milesPerHour: Velocity
    get() = toDouble().milesPerHour
