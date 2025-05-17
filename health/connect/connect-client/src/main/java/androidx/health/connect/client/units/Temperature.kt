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
 * Represents a unit of temperature. Supported units:
 * - Celsius - see [Temperature.celsius], [Double.celsius]
 * - Fahrenheit - see [Temperature.fahrenheit], [Double.fahrenheit]
 */
class Temperature private constructor(private val value: Double, private val type: Type) :
    Comparable<Temperature> {

    /** Returns the temperature in Celsius degrees. */
    @get:JvmName("getCelsius")
    val inCelsius: Double
        get() =
            when (type) {
                Type.CELSIUS -> value
                Type.FAHRENHEIT -> (value - 32.0) / 1.8
            }

    /** Returns the temperature in Fahrenheit degrees. */
    @get:JvmName("getFahrenheit")
    val inFahrenheit: Double
        get() =
            when (type) {
                Type.CELSIUS -> value * 1.8 + 32.0
                Type.FAHRENHEIT -> value
            }

    override fun compareTo(other: Temperature): Int =
        if (type == other.type) {
            value.compareTo(other.value)
        } else {
            inCelsius.compareTo(other.inCelsius)
        }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Temperature) return false

        if (type == other.type) {
            return value == other.value
        }

        return inCelsius == other.inCelsius
    }

    override fun hashCode(): Int = inCelsius.hashCode()

    override fun toString(): String = "$value ${type.title}"

    companion object {
        /** Creates [Temperature] with the specified value in Celsius degrees. */
        @JvmStatic fun celsius(value: Double): Temperature = Temperature(value, Type.CELSIUS)

        /** Creates [Temperature] with the specified value in Fahrenheit degrees. */
        @JvmStatic fun fahrenheit(value: Double): Temperature = Temperature(value, Type.FAHRENHEIT)
    }

    private enum class Type {
        CELSIUS {
            override val title: String = "Celsius"
        },
        FAHRENHEIT {
            override val title: String = "Fahrenheit"
        };

        abstract val title: String
    }
}

/** Creates [Temperature] with the specified value in Celsius degrees. */
@get:JvmSynthetic
val Double.celsius: Temperature
    get() = Temperature.celsius(value = this)

/** Creates [Temperature] with the specified value in Celsius degrees. */
@get:JvmSynthetic
val Long.celsius: Temperature
    get() = toDouble().celsius

/** Creates [Temperature] with the specified value in Celsius degrees. */
@get:JvmSynthetic
val Float.celsius: Temperature
    get() = toDouble().celsius

/** Creates [Temperature] with the specified value in Celsius degrees. */
@get:JvmSynthetic
val Int.celsius: Temperature
    get() = toDouble().celsius

/** Creates [Temperature] with the specified value in Fahrenheit degrees. */
@get:JvmSynthetic
val Double.fahrenheit: Temperature
    get() = Temperature.fahrenheit(value = this)

/** Creates [Temperature] with the specified value in Fahrenheit degrees. */
@get:JvmSynthetic
val Long.fahrenheit: Temperature
    get() = toDouble().fahrenheit

/** Creates [Temperature] with the specified value in Fahrenheit degrees. */
@get:JvmSynthetic
val Float.fahrenheit: Temperature
    get() = toDouble().fahrenheit

/** Creates [Temperature] with the specified value in Fahrenheit degrees. */
@get:JvmSynthetic
val Int.fahrenheit: Temperature
    get() = toDouble().fahrenheit
