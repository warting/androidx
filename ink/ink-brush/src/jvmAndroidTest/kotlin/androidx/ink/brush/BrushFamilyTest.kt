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

package androidx.ink.brush

import androidx.ink.nativeloader.UsedByNative
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@OptIn(ExperimentalInkCustomBrushApi::class)
@RunWith(JUnit4::class)
class BrushFamilyTest {
    @Test
    fun constructor_withValidArguments_returnsABrushFamily() {
        assertThat(BrushFamily(customTip, customPaint, customBrushFamilyId)).isNotNull()
    }

    @Test
    fun constructor_withDefaultArguments_returnsABrushFamily() {
        assertThat(BrushFamily(BrushTip(), BrushPaint(), clientBrushFamilyId = "")).isNotNull()
    }

    @Test
    fun hashCode_withIdenticalValues_matches() {
        assertThat(newCustomBrushFamily().hashCode()).isEqualTo(newCustomBrushFamily().hashCode())
    }

    @Test
    fun inputModelHashCode_isSameForIdenticalModels() {
        assertThat(BrushFamily.SPRING_MODEL.hashCode())
            .isEqualTo(BrushFamily.SPRING_MODEL.hashCode())
    }

    @Test
    fun equals_comparesValues() {
        val brushFamily =
            BrushFamily(customTip, customPaint, customBrushFamilyId, BrushFamily.SPRING_MODEL)
        val differentCoat = BrushCoat(BrushTip(), BrushPaint())
        val differentId = "different"

        // same values are equal.
        assertThat(brushFamily)
            .isEqualTo(
                BrushFamily(customTip, customPaint, customBrushFamilyId, BrushFamily.SPRING_MODEL)
            )

        // different values are not equal.
        assertThat(brushFamily).isNotEqualTo(null)
        assertThat(brushFamily).isNotEqualTo(Any())
        assertThat(brushFamily).isNotEqualTo(brushFamily.copy(coat = differentCoat))
        assertThat(brushFamily).isNotEqualTo(brushFamily.copy(clientBrushFamilyId = differentId))
    }

    @Test
    fun inputModelEquals_comparesModels() {
        assertThat(BrushFamily.SPRING_MODEL).isEqualTo(BrushFamily.SPRING_MODEL)
    }

    @Test
    fun toString_returnsExpectedValues() {
        assertThat(BrushFamily(inputModel = BrushFamily.SPRING_MODEL).toString())
            .isEqualTo(
                "BrushFamily(coats=[BrushCoat(tip=BrushTip(scale=(1.0, 1.0), " +
                    "cornerRounding=1.0, slant=0.0, pinch=0.0, rotation=0.0, opacityMultiplier=1.0, " +
                    "particleGapDistanceScale=0.0, particleGapDurationMillis=0, " +
                    "behaviors=[]), paint=BrushPaint(textureLayers=[]))], clientBrushFamilyId=, " +
                    "inputModel=SpringModel)"
            )
    }

    @Test
    fun inputModelToString_returnsExpectedValues() {

        assertThat(BrushFamily.SPRING_MODEL.toString()).isEqualTo("SpringModel")
    }

    @Test
    fun copy_whenSameContents_returnsSameInstance() {
        val customFamily = BrushFamily(customTip, customPaint, customBrushFamilyId)

        // A pure copy returns `this`.
        val copy = customFamily.copy()
        assertThat(copy).isSameInstanceAs(customFamily)
    }

    @Test
    fun copy_withArguments_createsCopyWithChanges() {
        val brushFamily = BrushFamily(customTip, customPaint, customBrushFamilyId)
        val differentCoats = listOf(BrushCoat(BrushTip(), BrushPaint()))
        val differentId = "different"

        assertThat(brushFamily.copy(coats = differentCoats))
            .isEqualTo(BrushFamily(differentCoats, customBrushFamilyId))
        assertThat(brushFamily.copy(clientBrushFamilyId = differentId))
            .isEqualTo(BrushFamily(customTip, customPaint, differentId))
    }

    @Test
    fun builder_createsExpectedBrushFamily() {
        val family =
            BrushFamily.Builder()
                .setCoat(customTip, customPaint)
                .setClientBrushFamilyId(customBrushFamilyId)
                .build()
        assertThat(family).isEqualTo(BrushFamily(customTip, customPaint, customBrushFamilyId))
    }

    /**
     * Creates an expected C++ BrushFamily with defaults and returns true if every property of the
     * Kotlin BrushFamily's JNI-created C++ counterpart is equivalent to the expected C++
     * BrushFamily.
     */
    @UsedByNative private external fun matchesDefaultFamily(brushFamilyNativePointer: Long): Boolean

    /**
     * Creates an expected C++ BrushFamily with custom values and returns true if every property of
     * the Kotlin BrushFamily's JNI-created C++ counterpart is equivalent to the expected C++
     * BrushFamily.
     */
    @UsedByNative
    private external fun matchesMultiBehaviorTipFamily(brushFamilyNativePointer: Long): Boolean

    private val customBrushFamilyId = "inkpen"

    /** Brush behavior with every field different from default values. */
    private val customBehavior =
        BrushBehavior(
            source = BrushBehavior.Source.TILT_IN_RADIANS,
            target = BrushBehavior.Target.HEIGHT_MULTIPLIER,
            sourceValueRangeStart = 0.2f,
            sourceValueRangeEnd = .8f,
            targetModifierRangeStart = 1.1f,
            targetModifierRangeEnd = 1.7f,
            sourceOutOfRangeBehavior = BrushBehavior.OutOfRange.MIRROR,
            responseCurve = EasingFunction.Predefined.EASE_IN_OUT,
            responseTimeMillis = 1L,
            enabledToolTypes = setOf(InputToolType.STYLUS),
            isFallbackFor = BrushBehavior.OptionalInputProperty.TILT_X_AND_Y,
        )

    /** Brush tip with every field different from default values and non-empty behaviors. */
    private val customTip =
        BrushTip(
            scaleX = 0.1f,
            scaleY = 0.2f,
            cornerRounding = 0.3f,
            slant = 0.4f,
            pinch = 0.5f,
            rotation = 0.6f,
            opacityMultiplier = 0.7f,
            particleGapDistanceScale = 0.8f,
            particleGapDurationMillis = 9L,
            listOf(customBehavior),
        )

    /**
     * Brush Paint with every field different from default values, including non-empty texture
     * layers.
     */
    private val customPaint =
        BrushPaint(
            listOf(
                BrushPaint.TextureLayer(
                    clientTextureId = "test-one",
                    sizeX = 123.45F,
                    sizeY = 678.90F,
                    offsetX = 0.123f,
                    offsetY = 0.678f,
                    rotation = 0.1f,
                    opacity = 0.123f,
                    animationFrames = 2,
                    animationRows = 3,
                    animationColumns = 4,
                    animationDurationMillis = 5000,
                    BrushPaint.TextureSizeUnit.STROKE_COORDINATES,
                    BrushPaint.TextureOrigin.STROKE_SPACE_ORIGIN,
                    BrushPaint.TextureMapping.TILING,
                ),
                BrushPaint.TextureLayer(
                    clientTextureId = "test-two",
                    sizeX = 256F,
                    sizeY = 256F,
                    offsetX = 0.456f,
                    offsetY = 0.567f,
                    rotation = 0.2f,
                    opacity = 0.987f,
                    animationFrames = 2,
                    animationRows = 3,
                    animationColumns = 4,
                    animationDurationMillis = 5000,
                    BrushPaint.TextureSizeUnit.STROKE_COORDINATES,
                    BrushPaint.TextureOrigin.STROKE_SPACE_ORIGIN,
                    BrushPaint.TextureMapping.TILING,
                ),
            )
        )

    /** Brush Family with every field different from default values. */
    private fun newCustomBrushFamily(): BrushFamily =
        BrushFamily(customTip, customPaint, customBrushFamilyId)
}
