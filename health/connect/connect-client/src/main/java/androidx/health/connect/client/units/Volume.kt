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
 * Represents a unit of volume. Supported units:
 * - liters - see [Volume.liters], [Double.liters]
 * - milliliters - see [Volume.milliliters], [Double.milliliters]
 * - US fluid ounces - see [Volume.fluidOuncesUs], [Double.fluidOuncesUs]
 */
class Volume private constructor(private val value: Double, private val type: Type) :
    Comparable<Volume> {

    /** Returns the volume in liters. */
    @get:JvmName("getLiters")
    val inLiters: Double
        get() = value * type.litersPerUnit

    /** Returns the volume in milliliters. */
    @get:JvmName("getMilliliters")
    val inMilliliters: Double
        get() = get(type = Type.MILLILITERS)

    /** Returns the volume in US fluid ounces. */
    @get:JvmName("getFluidOuncesUs")
    val inFluidOuncesUs: Double
        get() = get(type = Type.FLUID_OUNCES_US)

    private fun get(type: Type): Double =
        if (this.type == type) value else inLiters / type.litersPerUnit

    /** Returns zero [Volume] of the same [Type]. */
    internal fun zero(): Volume = ZEROS.getValue(type)

    override fun compareTo(other: Volume): Int =
        if (type == other.type) {
            value.compareTo(other.value)
        } else {
            inLiters.compareTo(other.inLiters)
        }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Volume) return false

        if (type == other.type) {
            return value == other.value
        }

        return inLiters == other.inLiters
    }

    override fun hashCode(): Int = inLiters.hashCode()

    override fun toString(): String = "$value ${type.title}"

    companion object {
        private val ZEROS = Type.values().associateWith { Volume(value = 0.0, type = it) }

        /** Creates [Volume] with the specified value in liters. */
        @JvmStatic fun liters(value: Double): Volume = Volume(value, Type.LITERS)

        /** Creates [Volume] with the specified value in milliliters. */
        @JvmStatic fun milliliters(value: Double): Volume = Volume(value, Type.MILLILITERS)

        /** Creates [Volume] with the specified value in US fluid ounces. */
        @JvmStatic fun fluidOuncesUs(value: Double): Volume = Volume(value, Type.FLUID_OUNCES_US)
    }

    private enum class Type {
        LITERS {
            override val litersPerUnit: Double = 1.0
            override val title: String = "L"
        },
        MILLILITERS {
            override val litersPerUnit: Double = 0.001
            override val title: String = "mL"
        },
        FLUID_OUNCES_US {
            override val litersPerUnit: Double = 0.02957353
            override val title: String = "fl. oz (US)"
        };

        abstract val litersPerUnit: Double
        abstract val title: String
    }
}

/** Creates [Volume] with the specified value in liters. */
@get:JvmSynthetic
val Double.liters: Volume
    get() = Volume.liters(value = this)

/** Creates [Volume] with the specified value in liters. */
@get:JvmSynthetic
val Long.liters: Volume
    get() = toDouble().liters

/** Creates [Volume] with the specified value in liters. */
@get:JvmSynthetic
val Float.liters: Volume
    get() = toDouble().liters

/** Creates [Volume] with the specified value in liters. */
@get:JvmSynthetic
val Int.liters: Volume
    get() = toDouble().liters

/** Creates [Volume] with the specified value in milliliters. */
@get:JvmSynthetic
val Double.milliliters: Volume
    get() = Volume.milliliters(value = this)

/** Creates [Volume] with the specified value in milliliters. */
@get:JvmSynthetic
val Long.milliliters: Volume
    get() = toDouble().milliliters

/** Creates [Volume] with the specified value in milliliters. */
@get:JvmSynthetic
val Float.milliliters: Volume
    get() = toDouble().milliliters

/** Creates [Volume] with the specified value in milliliters. */
@get:JvmSynthetic
val Int.milliliters: Volume
    get() = toDouble().milliliters

/** Creates [Volume] with the specified value in US fluid ounces. */
@get:JvmSynthetic
val Double.fluidOuncesUs: Volume
    get() = Volume.fluidOuncesUs(value = this)

/** Creates [Volume] with the specified value in US fluid ounces. */
@get:JvmSynthetic
val Long.fluidOuncesUs: Volume
    get() = toDouble().fluidOuncesUs

/** Creates [Volume] with the specified value in US fluid ounces. */
@get:JvmSynthetic
val Float.fluidOuncesUs: Volume
    get() = toDouble().fluidOuncesUs

/** Creates [Volume] with the specified value in US fluid ounces. */
@get:JvmSynthetic
val Int.fluidOuncesUs: Volume
    get() = toDouble().fluidOuncesUs
