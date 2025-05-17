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
 * Represents a unit of blood glucose level (glycaemia). Supported units:
 * - mmol/L - see [BloodGlucose.millimolesPerLiter]
 * - mg/dL - see [BloodGlucose.milligramsPerDeciliter]
 */
class BloodGlucose private constructor(private val value: Double, private val type: Type) :
    Comparable<BloodGlucose> {

    /** Returns the blood glucose level in mmol/L. */
    @get:JvmName("getMillimolesPerLiter")
    val inMillimolesPerLiter: Double
        get() = value * type.millimolesPerLiterPerUnit

    /** Returns the blood glucose level concentration in mg/dL. */
    @get:JvmName("getMilligramsPerDeciliter")
    val inMilligramsPerDeciliter: Double
        get() = get(type = Type.MILLIGRAMS_PER_DECILITER)

    private fun get(type: Type): Double =
        if (this.type == type) value else inMillimolesPerLiter / type.millimolesPerLiterPerUnit

    /** Returns zero [BloodGlucose] of the same [Type]. */
    internal fun zero(): BloodGlucose = ZEROS.getValue(type)

    override fun compareTo(other: BloodGlucose): Int =
        if (type == other.type) {
            value.compareTo(other.value)
        } else {
            inMillimolesPerLiter.compareTo(other.inMillimolesPerLiter)
        }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is BloodGlucose) return false

        if (type == other.type) {
            return value == other.value
        }

        return inMillimolesPerLiter == other.inMillimolesPerLiter
    }

    override fun hashCode(): Int = inMillimolesPerLiter.hashCode()

    override fun toString(): String = "$value ${type.title}"

    companion object {
        private val ZEROS = Type.values().associateWith { BloodGlucose(value = 0.0, type = it) }

        /** Creates [BloodGlucose] with the specified value in mmol/L. */
        @JvmStatic
        fun millimolesPerLiter(value: Double): BloodGlucose =
            BloodGlucose(value, Type.MILLIMOLES_PER_LITER)

        /** Creates [BloodGlucose] with the specified value in mg/dL. */
        @JvmStatic
        fun milligramsPerDeciliter(value: Double): BloodGlucose =
            BloodGlucose(value, Type.MILLIGRAMS_PER_DECILITER)
    }

    private enum class Type {
        MILLIMOLES_PER_LITER {
            override val millimolesPerLiterPerUnit: Double = 1.0
            override val title: String
                get() = "mmol/L"
        },
        MILLIGRAMS_PER_DECILITER {
            override val millimolesPerLiterPerUnit: Double = 1 / 18.0
            override val title: String = "mg/dL"
        };

        abstract val millimolesPerLiterPerUnit: Double
        abstract val title: String
    }
}
