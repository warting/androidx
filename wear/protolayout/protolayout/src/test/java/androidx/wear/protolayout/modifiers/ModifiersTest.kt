/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.wear.protolayout.modifiers

import android.app.PendingIntent
import android.content.Intent
import android.graphics.Color
import androidx.core.os.BundleCompat
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.wear.protolayout.ActionBuilders.LaunchAction
import androidx.wear.protolayout.ActionBuilders.LoadAction
import androidx.wear.protolayout.ColorBuilders.LinearGradient
import androidx.wear.protolayout.ModifiersBuilders.DefaultContentTransitions.fadeInSlideIn
import androidx.wear.protolayout.ModifiersBuilders.DefaultContentTransitions.fadeOutSlideOut
import androidx.wear.protolayout.ModifiersBuilders.FadeInTransition
import androidx.wear.protolayout.ModifiersBuilders.FadeOutTransition
import androidx.wear.protolayout.ModifiersBuilders.SEMANTICS_ROLE_BUTTON
import androidx.wear.protolayout.ModifiersBuilders.SEMANTICS_ROLE_NONE
import androidx.wear.protolayout.ModifiersBuilders.SLIDE_DIRECTION_BOTTOM_TO_TOP
import androidx.wear.protolayout.ProtoLayoutScope
import androidx.wear.protolayout.expression.DynamicBuilders.DynamicBool
import androidx.wear.protolayout.expression.DynamicBuilders.DynamicString
import androidx.wear.protolayout.expression.dynamicDataMapOf
import androidx.wear.protolayout.expression.intAppDataKey
import androidx.wear.protolayout.expression.mapTo
import androidx.wear.protolayout.expression.stringAppDataKey
import androidx.wear.protolayout.types.LayoutColor
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ModifiersTest {

    @Test
    fun contentDescription_toModifier() {
        val modifiers =
            LayoutModifier.contentDescription(
                    STATIC_CONTENT_DESCRIPTION,
                    DYNAMIC_CONTENT_DESCRIPTION,
                )
                .toProtoLayoutModifiers()

        assertThat(modifiers.semantics?.contentDescription?.value)
            .isEqualTo(STATIC_CONTENT_DESCRIPTION)
        assertThat(modifiers.semantics?.contentDescription?.dynamicValue?.toDynamicStringProto())
            .isEqualTo(DYNAMIC_CONTENT_DESCRIPTION.toDynamicStringProto())
        assertThat(modifiers.semantics?.role).isEqualTo(SEMANTICS_ROLE_NONE)
    }

    @Test
    fun semanticsRole_toModifier() {
        val modifiers = LayoutModifier.semanticsRole(SEMANTICS_ROLE_BUTTON).toProtoLayoutModifiers()

        assertThat(modifiers.semantics?.role).isEqualTo(SEMANTICS_ROLE_BUTTON)
        assertThat(modifiers.semantics?.contentDescription).isNull()
    }

    @Test
    fun contentDescription_semanticRole_toModifier() {
        val modifiers =
            LayoutModifier.contentDescription(
                    STATIC_CONTENT_DESCRIPTION,
                    DYNAMIC_CONTENT_DESCRIPTION,
                )
                .semanticsRole(SEMANTICS_ROLE_BUTTON)
                .toProtoLayoutModifiers()

        assertThat(modifiers.semantics?.contentDescription?.value)
            .isEqualTo(STATIC_CONTENT_DESCRIPTION)
        assertThat(modifiers.semantics?.contentDescription?.dynamicValue?.toDynamicStringProto())
            .isEqualTo(DYNAMIC_CONTENT_DESCRIPTION.toDynamicStringProto())
        assertThat(modifiers.semantics?.role).isEqualTo(SEMANTICS_ROLE_BUTTON)
    }

    @Test
    fun semanticsHeading_toModifier() {
        val modifiers = LayoutModifier.semanticsHeading(true).toProtoLayoutModifiers()

        assertThat(modifiers.semantics?.isHeading).isTrue()
    }

    @Test
    fun clearSemantics_fromModifier() {
        val modifiers =
            LayoutModifier.contentDescription(
                    STATIC_CONTENT_DESCRIPTION,
                    DYNAMIC_CONTENT_DESCRIPTION,
                )
                .semanticsRole(SEMANTICS_ROLE_BUTTON)
                .semanticsHeading(true)
                .clearSemantics()
                .toProtoLayoutModifiers()

        assertThat(modifiers.semantics).isNull()
    }

    @Test
    fun background_clip_toModifier() {
        val modifiers =
            LayoutModifier.background(COLOR)
                .clip(CORNER_RADIUS)
                .clip(CORNER_RADIUS_X, CORNER_RADIUS_Y)
                .clipTopRight(0f, 0f)
                .toProtoLayoutModifiers()

        assertThat(modifiers.background?.color?.argb).isEqualTo(COLOR.prop.argb)
        assertThat(modifiers.background?.corner?.radius?.value).isEqualTo(CORNER_RADIUS)
        assertThat(modifiers.background?.corner?.topLeftRadius?.x?.value).isEqualTo(CORNER_RADIUS_X)
        assertThat(modifiers.background?.corner?.topLeftRadius?.y?.value).isEqualTo(CORNER_RADIUS_Y)
        assertThat(modifiers.background?.corner?.topRightRadius?.x?.value).isEqualTo(0f)
        assertThat(modifiers.background?.corner?.topRightRadius?.y?.value).isEqualTo(0f)
        assertThat(modifiers.background?.corner?.bottomLeftRadius?.x?.value)
            .isEqualTo(CORNER_RADIUS_X)
        assertThat(modifiers.background?.corner?.bottomLeftRadius?.y?.value)
            .isEqualTo(CORNER_RADIUS_Y)
        assertThat(modifiers.background?.corner?.bottomRightRadius?.x?.value)
            .isEqualTo(CORNER_RADIUS_X)
        assertThat(modifiers.background?.corner?.bottomRightRadius?.y?.value)
            .isEqualTo(CORNER_RADIUS_Y)
    }

    @Test
    fun backgroundBrush_toModifier() {
        val brush = LinearGradient.Builder(COLOR.prop, COLOR1.prop).build()
        val modifiers = LayoutModifier.background(brush).toProtoLayoutModifiers()

        assertThat(modifiers.background?.brush).isInstanceOf(LinearGradient::class.java)
        assertThat((modifiers.background?.brush as LinearGradient).colorStops.map { it.color.argb })
            .containsExactlyElementsIn(listOf(COLOR.staticArgb, COLOR1.staticArgb))
    }

    @Test
    fun perCornerClip_clip_overwritesAllCorners() {
        val modifiers =
            LayoutModifier.clipTopLeft(0f, 1f)
                .clipTopRight(2f, 3f)
                .clipBottomLeft(4f, 5f)
                .clipBottomRight(6f, 7f)
                .clip(CORNER_RADIUS_X, CORNER_RADIUS_Y)
                .toProtoLayoutModifiers()

        assertThat(modifiers.background?.color).isNull()
        assertThat(modifiers.background?.corner?.radius?.value).isEqualTo(null)
        assertThat(modifiers.background?.corner?.topLeftRadius?.x?.value).isEqualTo(CORNER_RADIUS_X)
        assertThat(modifiers.background?.corner?.topLeftRadius?.y?.value).isEqualTo(CORNER_RADIUS_Y)
        assertThat(modifiers.background?.corner?.topRightRadius?.x?.value)
            .isEqualTo(CORNER_RADIUS_X)
        assertThat(modifiers.background?.corner?.topRightRadius?.y?.value)
            .isEqualTo(CORNER_RADIUS_Y)
        assertThat(modifiers.background?.corner?.bottomLeftRadius?.x?.value)
            .isEqualTo(CORNER_RADIUS_X)
        assertThat(modifiers.background?.corner?.bottomLeftRadius?.y?.value)
            .isEqualTo(CORNER_RADIUS_Y)
        assertThat(modifiers.background?.corner?.bottomRightRadius?.x?.value)
            .isEqualTo(CORNER_RADIUS_X)
        assertThat(modifiers.background?.corner?.bottomRightRadius?.y?.value)
            .isEqualTo(CORNER_RADIUS_Y)
    }

    @Test
    fun clickable_toModifier() {
        val id = "ID"
        val minTouchWidth = 51f
        val minTouchHeight = 52f
        val statePair1 = intAppDataKey("Int") mapTo 42
        val statePair2 = stringAppDataKey("String") mapTo "42"

        val modifiers =
            LayoutModifier.clickable(loadAction(dynamicDataMapOf(statePair1, statePair2)), id)
                .minimumTouchTargetSize(minTouchWidth, minTouchHeight)
                .toProtoLayoutModifiers()

        assertThat(modifiers.clickable?.id).isEqualTo(id)
        assertThat(modifiers.clickable?.minimumClickableWidth?.value).isEqualTo(minTouchWidth)
        assertThat(modifiers.clickable?.minimumClickableHeight?.value).isEqualTo(minTouchHeight)
        assertThat(modifiers.clickable?.onClick).isInstanceOf(LoadAction::class.java)
        val action = modifiers.clickable?.onClick as LoadAction
        assertThat(action.requestState?.keyToValueMapping)
            .containsExactlyEntriesIn(mapOf(statePair1.asPair(), statePair2.asPair()))
    }

    @Test
    fun clickable_fromProto_toModifier() {
        val id = "ID"
        val minTouchWidth = 51f
        val minTouchHeight = 52f
        val statePair1 = intAppDataKey("Int") mapTo 42
        val statePair2 = stringAppDataKey("String") mapTo "42"

        val modifiers =
            LayoutModifier.clickable(
                    clickable(
                        loadAction(dynamicDataMapOf(statePair1, statePair2)),
                        id = id,
                        minClickableWidth = minTouchWidth,
                        minClickableHeight = minTouchHeight,
                    )
                )
                .toProtoLayoutModifiers()

        assertThat(modifiers.clickable?.id).isEqualTo(id)
        assertThat(modifiers.clickable?.minimumClickableWidth?.value).isEqualTo(minTouchWidth)
        assertThat(modifiers.clickable?.minimumClickableHeight?.value).isEqualTo(minTouchHeight)
        assertThat(modifiers.clickable?.onClick).isInstanceOf(LoadAction::class.java)
        val action = modifiers.clickable?.onClick as LoadAction
        assertThat(action.requestState?.keyToValueMapping)
            .containsExactlyEntriesIn(mapOf(statePair1.asPair(), statePair2.asPair()))
    }

    @Test
    fun pendingIntent_clickable_toModifier() {
        val scope = ProtoLayoutScope()
        val id = "ID"
        val minTouchWidth = 51f
        val minTouchHeight = 52f
        val pendingIntent =
            PendingIntent.getActivity(
                /* context = */ ApplicationProvider.getApplicationContext(),
                /*requestCode = */ 0,
                /* intent = */ Intent(),
                /* flags = */ PendingIntent.FLAG_IMMUTABLE,
            )

        val modifiers =
            LayoutModifier.clickable(scope.clickable(pendingIntent = pendingIntent, id = id))
                .minimumTouchTargetSize(minTouchWidth, minTouchHeight)
                .toProtoLayoutModifiers()
        val collectedPendingIntents = scope.collectPendingIntents()

        assertThat(modifiers.clickable?.id).isEqualTo(id)
        assertThat(modifiers.clickable?.minimumClickableWidth?.value).isEqualTo(minTouchWidth)
        assertThat(modifiers.clickable?.minimumClickableHeight?.value).isEqualTo(minTouchHeight)
        // PendingIntentAction has package private access, so it is not accessible for the test here
        assertThat(modifiers.clickable?.onClick).isNotNull()
        assertThat(modifiers.clickable?.onClick).isNotInstanceOf(LoadAction::class.java)
        assertThat(modifiers.clickable?.onClick).isNotInstanceOf(LaunchAction::class.java)

        assertThat(collectedPendingIntents.containsKey(id)).isTrue()
        assertThat(
                BundleCompat.getParcelable<PendingIntent?>(
                    collectedPendingIntents,
                    id,
                    PendingIntent::class.java,
                )
            )
            .isEqualTo(pendingIntent)
    }

    @Test
    fun padding_toModifier() {
        val modifiers =
            LayoutModifier.padding(PADDING_ALL)
                .padding(bottom = BOTTOM_PADDING, rtlAware = false)
                .toProtoLayoutModifiers()

        assertThat(modifiers.padding?.start?.value).isEqualTo(PADDING_ALL)
        assertThat(modifiers.padding?.top?.value).isEqualTo(PADDING_ALL)
        assertThat(modifiers.padding?.end?.value).isEqualTo(PADDING_ALL)
        assertThat(modifiers.padding?.bottom?.value).isEqualTo(BOTTOM_PADDING)
        assertThat(modifiers.padding?.rtlAware?.value).isFalse()
    }

    @Test
    fun perSidePadding_padding_overwritesAllSides() {
        val modifiers =
            LayoutModifier.padding(
                    start = START_PADDING,
                    top = TOP_PADDING,
                    end = END_PADDING,
                    bottom = BOTTOM_PADDING,
                    rtlAware = true,
                )
                .padding(PADDING_ALL)
                .toProtoLayoutModifiers()

        assertThat(modifiers.padding?.start?.value).isEqualTo(PADDING_ALL)
        assertThat(modifiers.padding?.top?.value).isEqualTo(PADDING_ALL)
        assertThat(modifiers.padding?.end?.value).isEqualTo(PADDING_ALL)
        assertThat(modifiers.padding?.bottom?.value).isEqualTo(PADDING_ALL)
        assertThat(modifiers.padding?.rtlAware?.value).isFalse()
    }

    @Test
    fun metadata_toModifier() {
        val modifiers = LayoutModifier.tag(METADATA).toProtoLayoutModifiers()

        assertThat(modifiers.metadata?.tagData).isEqualTo(METADATA_BYTE_ARRAY)
    }

    @Test
    fun border_toModifier() {
        val modifier =
            LayoutModifier.border(width = WIDTH_DP, color = COLOR).toProtoLayoutModifiers()

        assertThat(modifier.border?.width?.value).isEqualTo(WIDTH_DP)
        assertThat(modifier.border?.color?.argb).isEqualTo(COLOR.prop.argb)
    }

    @Test
    fun visibility_toModifier() {
        val modifier =
            LayoutModifier.visibility(staticVisibility = false, dynamicVisibility = DYNAMIC_BOOL)
                .toProtoLayoutModifiers()

        assertThat(modifier.isVisible.value).isEqualTo(false)
        assertThat(modifier.isVisible.dynamicValue?.toDynamicBoolProto())
            .isEqualTo(DYNAMIC_BOOL.toDynamicBoolProto())
    }

    @Test
    fun enterTransition_toModifier() {
        val modifier =
            LayoutModifier.enterTransition(fadeInSlideIn(SLIDE_DIRECTION_BOTTOM_TO_TOP))
                .enterTransition(fadeIn = FadeInTransition.Builder().setInitialAlpha(ALPHA).build())
                .toProtoLayoutModifiers()

        assertThat(modifier.contentUpdateAnimation?.enterTransition?.fadeIn?.initialAlpha)
            .isEqualTo(ALPHA)
        assertThat(modifier.contentUpdateAnimation?.enterTransition?.slideIn?.direction)
            .isEqualTo(SLIDE_DIRECTION_BOTTOM_TO_TOP)
    }

    @Test
    fun exitTransition_toModifier() {
        val modifier =
            LayoutModifier.exitTransition(fadeOutSlideOut(SLIDE_DIRECTION_BOTTOM_TO_TOP))
                .exitTransition(fadeOut = FadeOutTransition.Builder().setTargetAlpha(ALPHA).build())
                .toProtoLayoutModifiers()

        assertThat(modifier.contentUpdateAnimation?.exitTransition?.fadeOut?.targetAlpha)
            .isEqualTo(ALPHA)
        assertThat(modifier.contentUpdateAnimation?.exitTransition?.slideOut?.direction)
            .isEqualTo(SLIDE_DIRECTION_BOTTOM_TO_TOP)
    }

    companion object {
        const val STATIC_CONTENT_DESCRIPTION = "content desc"
        val DYNAMIC_CONTENT_DESCRIPTION = DynamicString.constant("dynamic content")
        val COLOR = LayoutColor(Color.RED)
        val COLOR1 = LayoutColor(Color.GREEN)
        const val CORNER_RADIUS_X = 1.2f
        const val CORNER_RADIUS_Y = 3.4f
        const val CORNER_RADIUS = 5.6f
        const val START_PADDING = 1f
        const val TOP_PADDING = 2f
        const val END_PADDING = 3f
        const val BOTTOM_PADDING = 4f
        const val PADDING_ALL = 5f
        const val METADATA = "metadata"
        val METADATA_BYTE_ARRAY = METADATA.toByteArray()
        const val WIDTH_DP = 5f
        val DYNAMIC_BOOL = DynamicBool.constant(true)
        const val ALPHA = 0.7f
    }
}
