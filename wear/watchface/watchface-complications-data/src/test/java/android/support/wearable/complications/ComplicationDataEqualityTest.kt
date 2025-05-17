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

package android.support.wearable.complications

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.support.wearable.complications.ComplicationData.Companion.TYPE_NO_DATA
import android.support.wearable.complications.ComplicationData.Companion.TYPE_SHORT_TEXT
import android.support.wearable.complications.ComplicationText.plainText
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import androidx.wear.protolayout.expression.DynamicBuilders.DynamicFloat
import androidx.wear.protolayout.expression.DynamicBuilders.DynamicString
import androidx.wear.watchface.complications.data.ComplicationDisplayPolicies
import androidx.wear.watchface.complications.data.ComplicationPersistencePolicies
import androidx.wear.watchface.complications.data.SharedRobolectricTestRunner
import com.google.common.truth.Expect
import kotlin.random.Random
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLog

@RunWith(SharedRobolectricTestRunner::class)
class ComplicationDataEqualityTest {
    @get:Rule val expect = Expect.create()

    @Before
    fun setup() {
        ShadowLog.setLoggable("ComplicationData", Log.DEBUG)
    }

    /**
     * Testing equality by generating a scenario per field.
     *
     * Each value of the enum contains two setters that describes how to build a [ComplicationData]
     * in two variations that should not equal each other, like this:
     * ```
     * TEST_NAME(
     *   { setField(<variation one>) },
     *   { setField(<variation two>) },
     * )
     * ```
     *
     * It is then verified that two instances built with `<variation one>` are equal, but different
     * from an instance built with `<variation two>`.
     */
    private enum class EqualsScenario(
        val setterOne: ComplicationData.Builder.() -> ComplicationData,
        val setterTwo: ComplicationData.Builder.() -> ComplicationData,
    ) {
        PERSISTENCE_POLICY(
            { setPersistencePolicy(ComplicationPersistencePolicies.DO_NOT_PERSIST).build() },
            { setPersistencePolicy(ComplicationPersistencePolicies.CACHING_ALLOWED).build() },
        ),
        DISPLAY_POLICY(
            {
                setDisplayPolicy(ComplicationDisplayPolicies.DO_NOT_SHOW_WHEN_DEVICE_LOCKED).build()
            },
            { setDisplayPolicy(ComplicationDisplayPolicies.ALWAYS_DISPLAY).build() },
        ),
        START_DATE_TIME_MILLIS(
            { setStartDateTimeMillis(1).build() },
            { setStartDateTimeMillis(2).build() },
        ),
        END_DATE_TIME_MILLIS(
            { setEndDateTimeMillis(1).build() },
            { setEndDateTimeMillis(2).build() },
        ),
        RANGED_VALUE({ setRangedValue(1f).build() }, { setRangedValue(2f).build() }),
        RANGED_VALUE_EXPRESSION(
            { setRangedDynamicValue(DynamicFloat.constant(1.2f)).build() },
            { setRangedDynamicValue(DynamicFloat.constant(3.4f)).build() },
        ),
        RANGED_VALUE_TYPE({ setRangedValueType(1).build() }, { setRangedValueType(2).build() }),
        RANGED_MIN_VALUE({ setRangedMinValue(1f).build() }, { setRangedMinValue(2f).build() }),
        RANGED_MAX_VALUE({ setRangedMaxValue(1f).build() }, { setRangedMaxValue(2f).build() }),
        TARGET_VALUE({ setTargetValue(1f).build() }, { setTargetValue(2f).build() }),
        LONG_TITLE(
            { setLongTitle(plainText("1")).build() },
            { setLongTitle(plainText("2")).build() },
        ),
        LONG_TEXT({ setLongText(plainText("1")).build() }, { setLongText(plainText("2")).build() }),
        SHORT_TITLE(
            { setShortTitle(plainText("1")).build() },
            { setShortTitle(plainText("2")).build() },
        ),
        SHORT_TEXT(
            { setShortText(plainText("1")).build() },
            { setShortText(plainText("2")).build() },
        ),
        ICON(
            { setIcon(Icon.createWithContentUri("1")).build() },
            { setIcon(Icon.createWithContentUri("2")).build() },
        ),
        BURN_IN_PROTECTION_ICON(
            { setBurnInProtectionIcon(Icon.createWithContentUri("1")).build() },
            { setBurnInProtectionIcon(Icon.createWithContentUri("2")).build() },
        ),
        SMALL_IMAGE(
            { setSmallImage(Icon.createWithContentUri("1")).build() },
            { setSmallImage(Icon.createWithContentUri("2")).build() },
        ),
        BURN_IN_PROTECTION_SMALL_IMAGE(
            { setBurnInProtectionSmallImage(Icon.createWithContentUri("1")).build() },
            { setBurnInProtectionSmallImage(Icon.createWithContentUri("2")).build() },
        ),
        SMALL_IMAGE_STYLE(
            { setSmallImageStyle(ComplicationData.IMAGE_STYLE_ICON).build() },
            { setSmallImageStyle(ComplicationData.IMAGE_STYLE_PHOTO).build() },
        ),
        LARGE_IMAGE(
            { setLargeImage(Icon.createWithContentUri("1")).build() },
            { setLargeImage(Icon.createWithContentUri("2")).build() },
        ),
        LIST_STYLE_HINT({ setListStyleHint(1).build() }, { setListStyleHint(2).build() }),
        TAP_ACTION(
            {
                setTapAction(
                        PendingIntent.getBroadcast(
                            ApplicationProvider.getApplicationContext(),
                            0,
                            Intent("1"),
                            0,
                        )
                    )
                    .build()
            },
            {
                setTapAction(
                        PendingIntent.getBroadcast(
                            ApplicationProvider.getApplicationContext(),
                            0,
                            Intent("2"),
                            0,
                        )
                    )
                    .build()
            },
        ),
        CONTENT_DESCRIPTION(
            { setContentDescription(plainText("1")).build() },
            { setContentDescription(plainText("2")).build() },
        ),
        TAP_ACTION_LOST_DUE_TO_SERIALIZATION(
            { setTapActionLostDueToSerialization(true).build() },
            { setTapActionLostDueToSerialization(false).build() },
        ),
        PLACEHOLDER(
            { setPlaceholder(staticData("1")).build() },
            { setPlaceholder(staticData("2")).build() },
        ),
        DATA_SOURCE(
            { setDataSource(ComponentName.createRelative("", "1")).build() },
            { setDataSource(ComponentName.createRelative("", "2")).build() },
        ),
        AMBIENT_LAYOUT(
            { setAmbientLayout(byteArrayOf(1, 2)).build() },
            { setAmbientLayout(byteArrayOf(3, 4)).build() },
        ),
        INTERACTIVE_LAYOUT(
            { setInteractiveLayout(byteArrayOf(1, 2)).build() },
            { setInteractiveLayout(byteArrayOf(3, 4)).build() },
        ),
        LAYOUT_RESOURCES(
            { setLayoutResources(byteArrayOf(1, 2)).build() },
            { setLayoutResources(byteArrayOf(3, 4)).build() },
        ),
        COLOR_RAMP(
            { setColorRamp(intArrayOf(1, 2)).build() },
            { setColorRamp(intArrayOf(3, 4)).build() },
        ),
        COLOR_RAMP_IS_SMOOTH_SHADED(
            { setColorRampInterpolated(true).build() },
            { setColorRampInterpolated(false).build() },
        ),
        LIST_ENTRY_COLLECTION(
            { setListEntryCollection(listOf(staticData("1"))).build() },
            { setListEntryCollection(listOf(staticData("2"))).build() },
        ),
        ELEMENT_WEIGHTS(
            { setElementWeights(floatArrayOf(1f, 2f)).build() },
            { setElementWeights(floatArrayOf(3f, 4f)).build() },
        ),
        ELEMENT_COLORS(
            { setElementColors(intArrayOf(1, 2)).build() },
            { setElementColors(intArrayOf(3, 4)).build() },
        ),
        ELEMENT_BACKGROUND_COLOR(
            { setElementBackgroundColor(1).build() },
            { setElementBackgroundColor(2).build() },
        ),
        TIMELINE_START_TIME(
            { build().apply { timelineStartEpochSecond = 100 } },
            { build().apply { timelineStartEpochSecond = 200 } },
        ),
        TIMELINE_END_TIME(
            { build().apply { timelineEndEpochSecond = 100 } },
            { build().apply { timelineEndEpochSecond = 200 } },
        ),
        TIMELINE_ENTRIES(
            { build().apply { setTimelineEntryCollection(listOf(staticData("1"))) } },
            { build().apply { setTimelineEntryCollection(listOf(staticData("2"))) } },
        );

        val base = ComplicationData.Builder(TYPE_NO_DATA).build()

        /** Builds a [ComplicationData] with the first variation. */
        fun buildOne() = setterOne(ComplicationData.Builder(base))

        /** Builds a [ComplicationData] with the second variation. */
        fun buildTwo() = setterTwo(ComplicationData.Builder(base))
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.P]) // Icon comparison only works since P.
    public fun testEquals() {
        for (scenario in EqualsScenario.values()) {
            expect
                .withMessage("${scenario.name} equals same")
                .that(scenario.buildOne())
                .isEqualTo(scenario.buildOne())
            expect
                .withMessage("${scenario.name} does not equal another")
                .that(scenario.buildOne())
                .isNotEqualTo(scenario.buildTwo())
            expect
                .withMessage("${scenario.name} does not equal unset")
                .that(scenario.buildOne())
                .isNotEqualTo(scenario.base)
        }
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.P]) // Icon hashing only works since P.
    public fun testHashCode() {
        for (scenario in EqualsScenario.values()) {
            expect
                .withMessage("${scenario.name} equals same")
                .that(scenario.buildOne().hashCode())
                .isEqualTo(scenario.buildOne().hashCode())
            expect
                .withMessage("${scenario.name} does not equal another")
                .that(scenario.buildOne().hashCode())
                .isNotEqualTo(scenario.buildTwo().hashCode())
            expect
                .withMessage("${scenario.name} does not equal unset")
                .that(scenario.buildOne().hashCode())
                .isNotEqualTo(scenario.base.hashCode())
        }
    }

    /**
     * Testing equality by generating a scenario per field.
     *
     * Each value of the enum contains two setters that describes how to build a [ComplicationData]
     * in two variations that should not equal each other, like this:
     * ```
     * TEST_NAME(
     *   { setField(<variation one>) },
     *   { setField(<variation two>) },
     * )
     * ```
     *
     * It is then verified that two instances built with `<variation one>` are equal, but different
     * from an instance built with `<variation two>`.
     */
    private enum class EqualsUnevaluatedScenario(
        val setterOne: ComplicationData.Builder.() -> Unit,
        val setterTwo: ComplicationData.Builder.() -> Unit,
    ) {
        RANGED_VALUE_EXPRESSION(
            {
                setRangedValue(Random.nextFloat()) // Ignored when there's an expression.
                    .setRangedDynamicValue(DynamicFloat.constant(1.2f))
            },
            {
                setRangedValue(Random.nextFloat()) // Ignored when there's an expression.
                    .setRangedDynamicValue(DynamicFloat.constant(3.4f))
            },
        ),
        RANGED_VALUE_NO_EXPRESSION({ setRangedValue(1f) }, { setRangedValue(2f) }),
        SHORT_TITLE_EXPRESSION(
            { setShortTitle(expressionText("1")) },
            { setShortTitle(expressionText("2")) },
        ),
        SHORT_TITLE_NO_EXPRESSION(
            { setShortTitle(plainText("1")) },
            { setShortTitle(plainText("2")) },
        ),
        SHORT_TEXT_EXPRESSION(
            { setShortText(expressionText("1")) },
            { setShortText(expressionText("2")) },
        ),
        SHORT_TEXT_NO_EXPRESSION(
            { setShortText(plainText("1")) },
            { setShortText(plainText("2")) },
        ),
        LONG_TITLE_EXPRESSION(
            { setLongTitle(expressionText("1")) },
            { setLongTitle(expressionText("2")) },
        ),
        LONG_TITLE_NO_EXPRESSION(
            { setLongTitle(plainText("1")) },
            { setLongTitle(plainText("2")) },
        ),
        LONG_TEXT_EXPRESSION(
            { setLongText(expressionText("1")) },
            { setLongText(expressionText("2")) },
        ),
        LONG_TEXT_NO_EXPRESSION({ setLongText(plainText("1")) }, { setLongText(plainText("2")) }),
        CONTENT_DESCRIPTION_EXPRESSION(
            { setContentDescription(expressionText("1")) },
            { setContentDescription(expressionText("2")) },
        ),
        CONTENT_DESCRIPTION_NO_EXPRESSION(
            { setContentDescription(plainText("1")) },
            { setContentDescription(plainText("2")) },
        ),
        PLACEHOLDER_EXPRESSION(
            { setPlaceholder(expressionData("1")) },
            { setPlaceholder(expressionData("2")) },
        ),
        PLACEHOLDER_NO_EXPRESSION(
            { setPlaceholder(staticData("1")) },
            { setPlaceholder(staticData("2")) },
        ),
        LIST_ENTRY_COLLECTION_EXPRESSION(
            { setListEntryCollection(listOf(expressionData("1"))) },
            { setListEntryCollection(listOf(expressionData("2"))) },
        ),
        LIST_ENTRY_COLLECTION_EXPRESSION_DIFFERENT_SIZE(
            { setListEntryCollection(listOf(expressionData("1"), expressionData("1"))) },
            { setListEntryCollection(listOf(expressionData("1"))) },
        ),
        LIST_ENTRY_COLLECTION_NO_EXPRESSION(
            { setListEntryCollection(listOf(staticData("1"))) },
            { setListEntryCollection(listOf(staticData("2"))) },
        ),
        LIST_ENTRY_COLLECTION_NO_EXPRESSION_DIFFERENT_SIZE(
            { setListEntryCollection(listOf(staticData("1"), staticData("1"))) },
            { setListEntryCollection(listOf(staticData("1"))) },
        );

        val base = ComplicationData.Builder(TYPE_NO_DATA).build()

        /** Builds a [ComplicationData] with the first variation. */
        fun buildOne() = ComplicationData.Builder(base).apply { setterOne(this) }.build()

        /** Builds a [ComplicationData] with the second variation. */
        fun buildTwo() = ComplicationData.Builder(base).apply { setterTwo(this) }.build()
    }

    @Test
    public fun equalsUnevaluated() {
        for (scenario in EqualsUnevaluatedScenario.values()) {
            expect
                .withMessage("${scenario.name} unevaluated equals same")
                .that(scenario.buildOne().equalsUnevaluated(scenario.buildOne()))
                .isTrue()
            expect
                .withMessage("${scenario.name} does not unevaluated equal another")
                .that(scenario.buildOne().equalsUnevaluated(scenario.buildTwo()))
                .isFalse()
            expect
                .withMessage("${scenario.name} does not unevaluated equal unset")
                .that(scenario.buildOne().equalsUnevaluated(scenario.base))
                .isFalse()
        }
    }

    private companion object {
        fun staticData(value: String) =
            ComplicationData.Builder(TYPE_SHORT_TEXT).setShortText(plainText(value)).build()

        fun expressionData(value: String) =
            ComplicationData.Builder(TYPE_SHORT_TEXT).setShortText(expressionText(value)).build()

        fun expressionText(value: String) =
            ComplicationText(
                Random.nextInt().toString(), // Ignored when there's an expression.
                DynamicString.constant(value),
            )
    }
}
