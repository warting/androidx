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
 * Represents a unit of mass. Supported units:
 * - grams - see [Mass.grams], [Double.grams]
 * - kilograms - see [Mass.kilograms], [Double.kilograms]
 * - milligrams - see [Mass.milligrams], [Double.milligrams]
 * - micrograms - see [Mass.micrograms], [Double.micrograms]
 * - ounces - see [Mass.ounces], [Double.ounces]
 * - pounds - see [Mass.pounds], [Double.pounds]
 */
class Mass private constructor(private val value: Double, private val type: Type) :
    Comparable<Mass> {

    /** Returns the mass in grams. */
    @get:JvmName("getGrams")
    val inGrams: Double
        get() = value * type.gramsPerUnit

    /** Returns the mass in kilograms. */
    @get:JvmName("getKilograms")
    val inKilograms: Double
        get() = get(type = Type.KILOGRAMS)

    /** Returns the mass in milligrams. */
    @get:JvmName("getMilligrams")
    val inMilligrams: Double
        get() = get(type = Type.MILLIGRAMS)

    /** Returns the mass in micrograms. */
    @get:JvmName("getMicrograms")
    val inMicrograms: Double
        get() = get(type = Type.MICROGRAMS)

    /** Returns the mass in ounces. */
    @get:JvmName("getOunces")
    val inOunces: Double
        get() = get(type = Type.OUNCES)

    /** Returns the mass in pounds. */
    @get:JvmName("getPounds")
    val inPounds: Double
        get() = get(type = Type.POUNDS)

    private fun get(type: Type): Double =
        if (this.type == type) value else inGrams / type.gramsPerUnit

    /** Returns zero [Mass] of the same [Type]. */
    internal fun zero(): Mass = ZEROS.getValue(type)

    override fun compareTo(other: Mass): Int =
        if (type == other.type) {
            value.compareTo(other.value)
        } else {
            inGrams.compareTo(other.inGrams)
        }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Mass) return false

        if (type == other.type) {
            return value == other.value
        }

        return inGrams == other.inGrams
    }

    override fun hashCode(): Int = inGrams.hashCode()

    override fun toString(): String = "$value ${type.name.lowercase()}"

    companion object {
        private val ZEROS = Type.values().associateWith { Mass(value = 0.0, type = it) }

        /** Creates [Mass] with the specified value in grams. */
        @JvmStatic fun grams(value: Double): Mass = Mass(value, Type.GRAMS)

        /** Creates [Mass] with the specified value in kilograms. */
        @JvmStatic fun kilograms(value: Double): Mass = Mass(value, Type.KILOGRAMS)

        /** Creates [Mass] with the specified value in milligrams. */
        @JvmStatic fun milligrams(value: Double): Mass = Mass(value, Type.MILLIGRAMS)

        /** Creates [Mass] with the specified value in micrograms. */
        @JvmStatic fun micrograms(value: Double): Mass = Mass(value, Type.MICROGRAMS)

        /** Creates [Mass] with the specified value in ounces. */
        @JvmStatic fun ounces(value: Double): Mass = Mass(value, Type.OUNCES)

        /** Creates [Mass] with the specified value in pounds. */
        @JvmStatic fun pounds(value: Double): Mass = Mass(value, Type.POUNDS)
    }

    private enum class Type {
        GRAMS {
            override val gramsPerUnit: Double = 1.0
        },
        KILOGRAMS {
            override val gramsPerUnit: Double = 1000.0
        },
        MILLIGRAMS {
            override val gramsPerUnit: Double = 0.001
        },
        MICROGRAMS {
            override val gramsPerUnit: Double = 0.000001
        },
        OUNCES {
            override val gramsPerUnit: Double = 28.34952
        },
        POUNDS {
            override val gramsPerUnit: Double = 453.59237
        };

        abstract val gramsPerUnit: Double
    }
}

/** Creates [Mass] with the specified value in grams. */
@get:JvmSynthetic
val Double.grams: Mass
    get() = Mass.grams(value = this)

/** Creates [Mass] with the specified value in grams. */
@get:JvmSynthetic
val Float.grams: Mass
    get() = toDouble().grams

/** Creates [Mass] with the specified value in grams. */
@get:JvmSynthetic
val Long.grams: Mass
    get() = toDouble().grams

/** Creates [Mass] with the specified value in grams. */
@get:JvmSynthetic
val Int.grams: Mass
    get() = toDouble().grams

/** Creates [Mass] with the specified value in kilograms. */
@get:JvmSynthetic
val Double.kilograms: Mass
    get() = Mass.kilograms(value = this)

/** Creates [Mass] with the specified value in kilograms. */
@get:JvmSynthetic
val Float.kilograms: Mass
    get() = toDouble().kilograms

/** Creates [Mass] with the specified value in kilograms. */
@get:JvmSynthetic
val Long.kilograms: Mass
    get() = toDouble().kilograms

/** Creates [Mass] with the specified value in kilograms. */
@get:JvmSynthetic
val Int.kilograms: Mass
    get() = toDouble().kilograms

/** Creates [Mass] with the specified value in milligrams. */
@get:JvmSynthetic
val Double.milligrams: Mass
    get() = Mass.milligrams(value = this)

/** Creates [Mass] with the specified value in milligrams. */
@get:JvmSynthetic
val Float.milligrams: Mass
    get() = toDouble().milligrams

/** Creates [Mass] with the specified value in milligrams. */
@get:JvmSynthetic
val Long.milligrams: Mass
    get() = toDouble().milligrams

/** Creates [Mass] with the specified value in milligrams. */
@get:JvmSynthetic
val Int.milligrams: Mass
    get() = toDouble().milligrams

/** Creates [Mass] with the specified value in micrograms. */
@get:JvmSynthetic
val Double.micrograms: Mass
    get() = Mass.micrograms(value = this)

/** Creates [Mass] with the specified value in micrograms. */
@get:JvmSynthetic
val Float.micrograms: Mass
    get() = toDouble().micrograms

/** Creates [Mass] with the specified value in micrograms. */
@get:JvmSynthetic
val Long.micrograms: Mass
    get() = toDouble().micrograms

/** Creates [Mass] with the specified value in micrograms. */
@get:JvmSynthetic
val Int.micrograms: Mass
    get() = toDouble().micrograms

/** Creates [Mass] with the specified value in ounces. */
@get:JvmSynthetic
val Double.ounces: Mass
    get() = Mass.ounces(value = this)

/** Creates [Mass] with the specified value in ounces. */
@get:JvmSynthetic
val Float.ounces: Mass
    get() = toDouble().ounces

/** Creates [Mass] with the specified value in ounces. */
@get:JvmSynthetic
val Long.ounces: Mass
    get() = toDouble().ounces

/** Creates [Mass] with the specified value in ounces. */
@get:JvmSynthetic
val Int.ounces: Mass
    get() = toDouble().ounces

/** Creates [Mass] with the specified value in pounds. */
@get:JvmSynthetic
val Double.pounds: Mass
    get() = Mass.pounds(value = this)

/** Creates [Mass] with the specified value in pounds. */
@get:JvmSynthetic
val Float.pounds: Mass
    get() = toDouble().pounds

/** Creates [Mass] with the specified value in pounds. */
@get:JvmSynthetic
val Long.pounds: Mass
    get() = toDouble().pounds

/** Creates [Mass] with the specified value in pounds. */
@get:JvmSynthetic
val Int.pounds: Mass
    get() = toDouble().pounds
