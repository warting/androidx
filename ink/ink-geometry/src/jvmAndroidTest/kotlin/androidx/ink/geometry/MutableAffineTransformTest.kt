/*
 * Copyright (C) 2024 The Android Open Source Project
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

package androidx.ink.geometry

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class MutableAffineTransformTest {

    @Test
    fun defaultConstructor_shouldBeEqualToIdentity() {
        val identity = MutableAffineTransform()

        assertThat(identity).isEqualTo(AffineTransform.IDENTITY)
    }

    @Test
    fun equals_whenSameInstance_returnsTrueAndSameHashCode() {
        val affineTransform = MutableAffineTransform(A, B, C, D, E, F)

        // Ensure test coverage of the same-instance case, but call .equals directly for lint.
        assertThat(affineTransform.equals(affineTransform)).isTrue()
        assertThat(affineTransform.hashCode()).isEqualTo(affineTransform.hashCode())
    }

    @Test
    fun equals_whenSameValues_returnsTrueAndSameHashCode() {
        val affineTransform = MutableAffineTransform(A, B, C, D, E, F)
        val otherTransform = MutableAffineTransform(A, B, C, D, E, F)

        assertThat(affineTransform).isEqualTo(otherTransform)
        assertThat(affineTransform.hashCode()).isEqualTo(otherTransform.hashCode())
    }

    @Test
    fun equals_whenSameInterfacePropertiesAndDifferentType_returnsTrue() {
        val immutable = ImmutableAffineTransform(A, B, C, D, E, F)
        val mutable = MutableAffineTransform(A, B, C, D, E, F)

        assertThat(mutable).isEqualTo(immutable)
    }

    @Test
    fun equals_whenDifferentA_returnsFalse() {
        val affineTransform = MutableAffineTransform(1f, B, C, D, E, F)
        val otherTransform = MutableAffineTransform(10f, B, C, D, E, F)

        assertThat(affineTransform).isNotEqualTo(otherTransform)
    }

    @Test
    fun equals_whenDifferentB_returnsFalse() {
        val affineTransform = MutableAffineTransform(A, 2f, C, D, E, F)
        val otherTransform = MutableAffineTransform(A, 20f, C, D, E, F)

        assertThat(affineTransform).isNotEqualTo(otherTransform)
    }

    @Test
    fun equals_whenDifferentC_returnsFalse() {
        val affineTransform = MutableAffineTransform(A, B, 3f, D, E, F)
        val otherTransform = MutableAffineTransform(A, B, 30f, D, E, F)

        assertThat(affineTransform).isNotEqualTo(otherTransform)
    }

    @Test
    fun equals_whenDifferentD_returnsFalse() {
        val affineTransform = MutableAffineTransform(A, B, C, 4f, E, F)
        val otherTransform = MutableAffineTransform(A, B, C, 40f, E, F)

        assertThat(affineTransform).isNotEqualTo(otherTransform)
    }

    @Test
    fun equals_whenDifferentE_returnsFalse() {
        val affineTransform = MutableAffineTransform(A, B, C, D, 5f, F)
        val otherTransform = MutableAffineTransform(A, B, C, D, 50f, F)

        assertThat(affineTransform).isNotEqualTo(otherTransform)
    }

    @Test
    fun equals_whenDifferentF_returnsFalse() {
        val affineTransform = MutableAffineTransform(A, B, C, D, E, 6f)
        val otherTransform = MutableAffineTransform(A, B, C, D, E, 60f)

        assertThat(affineTransform).isNotEqualTo(otherTransform)
    }

    @Test
    fun setValuesAndGetValues_shouldRoundTrip() {
        val affineTransform = MutableAffineTransform()
        val values = floatArrayOf(1F, 2F, 3F, 4F, 5F, 6F)

        affineTransform.setValues(values)
        val outValues = FloatArray(6)
        affineTransform.getValues(outValues)

        assertThat(outValues).usingExactEquality().containsExactly(values)
    }

    @Test
    fun constructWithValuesAndGetValues_shouldRoundTrip() {
        val affineTransform = MutableAffineTransform(1F, 2F, 3F, 4F, 5F, 6F)

        val outValues = FloatArray(6)
        affineTransform.getValues(outValues)

        assertThat(outValues)
            .usingExactEquality()
            .containsExactly(floatArrayOf(1F, 2F, 3F, 4F, 5F, 6F))
    }

    @Test
    fun setValues_shouldMatchConstructedWithFactoryFunctions() {
        assertThat(MutableAffineTransform().apply { setValues(7F, 0F, 0F, 0F, 7F, 0F) })
            .isEqualTo(ImmutableAffineTransform.scale(7F))

        assertThat(MutableAffineTransform().apply { setValues(3F, 0F, 0F, 0F, 5F, 0F) })
            .isEqualTo(ImmutableAffineTransform.scale(3F, 5F))

        assertThat(MutableAffineTransform().apply { setValues(4F, 0F, 0F, 0F, 1F, 0F) })
            .isEqualTo(ImmutableAffineTransform.scaleX(4F))

        assertThat(MutableAffineTransform().apply { setValues(1F, 0F, 0F, 0F, 2F, 0F) })
            .isEqualTo(ImmutableAffineTransform.scaleY(2F))

        assertThat(MutableAffineTransform().apply { setValues(1F, 0F, 8F, 0F, 1F, 9F) })
            .isEqualTo(ImmutableAffineTransform.translate(ImmutableVec(8F, 9F)))
    }

    @Test
    fun setValuesArray_shouldMatchConstructedWithFactoryFunctions() {
        assertThat(
                MutableAffineTransform().apply { setValues(floatArrayOf(7F, 0F, 0F, 0F, 7F, 0F)) }
            )
            .isEqualTo(ImmutableAffineTransform.scale(7F))

        assertThat(
                MutableAffineTransform().apply { setValues(floatArrayOf(3F, 0F, 0F, 0F, 5F, 0F)) }
            )
            .isEqualTo(ImmutableAffineTransform.scale(3F, 5F))

        assertThat(
                MutableAffineTransform().apply { setValues(floatArrayOf(4F, 0F, 0F, 0F, 1F, 0F)) }
            )
            .isEqualTo(ImmutableAffineTransform.scaleX(4F))

        assertThat(
                MutableAffineTransform().apply { setValues(floatArrayOf(1F, 0F, 0F, 0F, 2F, 0F)) }
            )
            .isEqualTo(ImmutableAffineTransform.scaleY(2F))

        assertThat(
                MutableAffineTransform().apply { setValues(floatArrayOf(1F, 0F, 8F, 0F, 1F, 9F)) }
            )
            .isEqualTo(ImmutableAffineTransform.translate(ImmutableVec(8F, 9F)))
    }

    @Test
    fun toImmutable_returnsEquivalentImmutableAffineTransform() {
        val affineTransform = MutableAffineTransform(A, B, C, D, E, F)

        val output = affineTransform.toImmutable()

        assertThat(output).isEqualTo(ImmutableAffineTransform(A, B, C, D, E, F))
        assertThat(output).isInstanceOf(ImmutableAffineTransform::class.java)
    }

    @Test
    fun toImmutable_returnsSingletonForIdentity() {
        assertThat(MutableAffineTransform().toImmutable())
            .isSameInstanceAs(AffineTransform.IDENTITY)
    }

    @Test
    fun populateFrom_correctlyCopiesFromAffineTransform() {
        val source = ImmutableAffineTransform(A, B, C, D, E, F)
        val dest = MutableAffineTransform()
        dest.populateFrom(source)

        assertThat(dest.equals(source)).isTrue()
    }

    @Test
    fun populateFromIdentity_correctlySetsAffineTransformToIdentity() {
        val transform = MutableAffineTransform(A, B, C, D, E, F)

        transform.populateFromIdentity()

        assertThat(transform.equals(AffineTransform.IDENTITY)).isTrue()
    }

    @Test
    fun populateFromTranslate_correctlyPopulatesAffineTransform() {
        val transform = MutableAffineTransform(A, B, C, D, E, F)

        transform.populateFromTranslate(ImmutableVec(x = 0.1f, y = 0.2f))

        assertThat(transform.equals(MutableAffineTransform(1f, 0f, 0.1f, 0f, 1f, 0.2f))).isTrue()
    }

    @Test
    fun populateFromScale_singleArgument_correctlyPopulatesAffineTransform() {
        val transform = MutableAffineTransform(A, B, C, D, E, F)

        transform.populateFromScale(0.2f)

        assertThat(transform.equals(MutableAffineTransform(0.2f, 0f, 0f, 0f, 0.2f, 0f))).isTrue()
    }

    @Test
    fun populateFromScale_separateArguments_correctlyPopulatesAffineTransform() {
        val transform = MutableAffineTransform(A, B, C, D, E, F)

        transform.populateFromScale(0.2f, 0.3f)

        assertThat(transform.equals(MutableAffineTransform(0.2f, 0f, 0f, 0f, 0.3f, 0f))).isTrue()
    }

    @Test
    fun populateFromScaleX_correctlyPopulatesAffineTransform() {
        val transform = MutableAffineTransform(A, B, C, D, E, F)

        transform.populateFromScaleX(0.1f)

        assertThat(transform.equals(MutableAffineTransform(0.1f, 0f, 0f, 0f, 1f, 0f))).isTrue()
    }

    @Test
    fun populateFromScaleY_correctlyPopulatesAffineTransform() {
        val transform = MutableAffineTransform(A, B, C, D, E, F)

        transform.populateFromScaleY(0.7f)

        assertThat(transform.equals(MutableAffineTransform(1f, 0f, 0f, 0f, 0.7f, 0f))).isTrue()
    }

    @Test
    fun shearX_returnsCorrectTransform() {
        val transform = MutableAffineTransform(A, B, C, D, E, F)
        transform.populateFromShearX(0.0f)
        assertThat(transform.equals(AffineTransform.IDENTITY)).isTrue()
        transform.populateFromShearX(2.2f)
        assertThat(transform.equals(MutableAffineTransform(1.0f, 2.2f, 0.0f, 0.0f, 1.0f, 0.0f)))
            .isTrue()
    }

    @Test
    fun shearY_returnsCorrectTransform() {
        val transform = MutableAffineTransform(A, B, C, D, E, F)
        transform.populateFromShearY(0.0f)
        assertThat(transform.equals(AffineTransform.IDENTITY)).isTrue()
        transform.populateFromShearY(2.2f)
        assertThat(transform.equals(MutableAffineTransform(1.0f, 0.0f, 0.0f, 2.2f, 1.0f, 0.0f)))
            .isTrue()
    }

    @Test
    fun rotate_returnsCorrectTransform() {
        val transform = MutableAffineTransform(A, B, C, D, E, F)
        transform.populateFromRotation(Angle.ZERO)
        assertThat(transform.equals(AffineTransform.IDENTITY)).isTrue()
        transform.populateFromRotation(Angle.HALF_TURN_RADIANS)
        assertThat(
                transform.isAlmostEqual(
                    MutableAffineTransform(-1f, 0f, 0f, 0f, -1f, 0.0f),
                    tolerance = 0.00001f,
                )
            )
            .isTrue()
        transform.populateFromRotation(Angle.QUARTER_TURN_RADIANS)
        assertThat(
                transform.isAlmostEqual(
                    MutableAffineTransform(0f, -1f, 0f, 1f, 0f, 0.0f),
                    tolerance = 0.00001f,
                )
            )
            .isTrue()
    }

    companion object {
        private const val A = 1f

        private const val B = 2f

        private const val C = 3f

        private const val D = 4f

        private const val E = 5f

        private const val F = 6f
    }
}
