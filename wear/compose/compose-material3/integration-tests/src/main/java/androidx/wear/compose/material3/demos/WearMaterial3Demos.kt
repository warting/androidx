/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.wear.compose.material3.demos

import android.content.Context
import android.os.Build
import android.widget.Toast
import androidx.wear.compose.integration.demos.common.Centralize
import androidx.wear.compose.integration.demos.common.ComposableDemo
import androidx.wear.compose.integration.demos.common.Material3DemoCategory
import androidx.wear.compose.material3.samples.AnimatedTextSample
import androidx.wear.compose.material3.samples.AnimatedTextSampleButtonResponse
import androidx.wear.compose.material3.samples.AnimatedTextSampleSharedFontRegistry
import androidx.wear.compose.material3.samples.ButtonGroupSample
import androidx.wear.compose.material3.samples.ButtonGroupThreeButtonsSample
import androidx.wear.compose.material3.samples.ButtonWithImageSample
import androidx.wear.compose.material3.samples.EdgeButtonListSample
import androidx.wear.compose.material3.samples.EdgeButtonSample
import androidx.wear.compose.material3.samples.EdgeSwipeForSwipeToDismiss
import androidx.wear.compose.material3.samples.FadingExpandingLabelButtonSample
import androidx.wear.compose.material3.samples.ImageCardSample
import androidx.wear.compose.material3.samples.LevelIndicatorSample
import androidx.wear.compose.material3.samples.ListHeaderSample
import androidx.wear.compose.material3.samples.SimpleSwipeToDismissBox
import androidx.wear.compose.material3.samples.StatefulSwipeToDismissBox
import androidx.wear.compose.material3.samples.SwipeToRevealNoPartiallyRevealedStateSample
import androidx.wear.compose.material3.samples.SwipeToRevealSample
import androidx.wear.compose.material3.samples.SwipeToRevealSingleActionCardSample
import androidx.wear.compose.material3.samples.SwipeToRevealWithTransformingLazyColumnResetOnScrollSample
import androidx.wear.compose.material3.samples.TitleCardWithImageWithTimeAndTitleSample
import androidx.wear.compose.material3.samples.TransformingLazyColumnAnimationSample
import androidx.wear.compose.material3.samples.TransformingLazyColumnExpandableCardSample
import androidx.wear.compose.material3.samples.TransformingLazyColumnReducedMotionSample

val WearMaterial3Demos =
    Material3DemoCategory(
        "Material 3",
        listOf(
                ComposableDemo("LevelIndicator") { Centralize { LevelIndicatorSample() } },
                ComposableDemo("Haptics") { Centralize { HapticsDemos() } },
                ComposableDemo("Performance") { Centralize { PerformanceDemos() } },
                Material3DemoCategory(
                    "Button",
                    listOf(
                        ComposableDemo("Base Button") { BaseButtonDemo() },
                        ComposableDemo("Filled Button") { ButtonDemo() },
                        ComposableDemo("Filled Tonal Button") { FilledTonalButtonDemo() },
                        ComposableDemo("Filled Variant Button") { FilledVariantButtonDemo() },
                        ComposableDemo("Outlined Button") { OutlinedButtonDemo() },
                        ComposableDemo("Child Button") { ChildButtonDemo() },
                        ComposableDemo("Multiline Button") { MultilineButtonDemo() },
                        ComposableDemo("App Button") { AppButtonDemo() },
                        ComposableDemo("Avatar Button") { AvatarButtonDemo() },
                        ComposableDemo("Image Button") { ButtonBackgroundImageDemo() },
                        ComposableDemo("Image Button Sample") {
                            Centralize { ButtonWithImageSample() }
                        },
                        ComposableDemo("Image Button builder") { ImageButtonBuilder() },
                        ComposableDemo("Button Stack") { ButtonStackDemo() },
                        ComposableDemo("Button Merge") { ButtonMergeDemo() },
                        ComposableDemo("Button Update Animation") { ButtonUpdateAnimationDemo() },
                        ComposableDemo("Fading Expanding Label") {
                            FadingExpandingLabelButtonSample()
                        },
                    ),
                ),
                ComposableDemo("Color Scheme") { ColorSchemeDemos() },
                ComposableDemo("Dynamic Color Scheme") { DynamicColorSchemeDemos() },
                Material3DemoCategory("Curved Text", CurvedTextDemos),
                Material3DemoCategory("Alert Dialog", AlertDialogDemos),
                Material3DemoCategory("Confirmation Dialog", ComfirmationDialogDemos),
                Material3DemoCategory("Open on phone Dialog", OpenOnPhoneDialogDemos),
                Material3DemoCategory("Scaffold", ScaffoldDemos),
                Material3DemoCategory("ScrollAway", ScrollAwayDemos),
                Material3DemoCategory(title = "Typography", TypographyDemos),
                ComposableDemo("Compact Button") { CompactButtonDemo() },
                ComposableDemo("Icon Button") { IconButtonDemo() },
                ComposableDemo("Text Button") { TextButtonDemo() },
                Material3DemoCategory(
                    "Edge Button",
                    listOf(
                        ComposableDemo("Simple Edge Button") { EdgeButtonSample() },
                        ComposableDemo("Sizes and Colors") { EdgeButtonMultiDemo() },
                        ComposableDemo("Configurable") { EdgeButtonConfigurableDemo() },
                        ComposableDemo("Simple Edge Button below SLC") { EdgeButtonListSample() },
                        ComposableDemo("Edge Button Below LC") { EdgeButtonBelowLazyColumnDemo() },
                        ComposableDemo("Edge Button Below SLC") {
                            EdgeButtonBelowScalingLazyColumnDemo()
                        },
                        ComposableDemo("Edge Button Below TLC") {
                            EdgeButtonBelowTransformingLazyColumnDemo()
                        },
                    ),
                ),
                Material3DemoCategory(
                    "Button Group",
                    listOf(
                        ComposableDemo("Two buttons") { Centralize { ButtonGroupSample() } },
                        ComposableDemo("ABC") { Centralize { ButtonGroupThreeButtonsSample() } },
                        ComposableDemo("Text And Icon") { ButtonGroupDemo() },
                        ComposableDemo("ToggleButtons") { ButtonGroupToggleButtonsDemo() },
                    ),
                ),
                Material3DemoCategory(
                    "List Header",
                    listOf(
                        ComposableDemo("List headers") { ListHeaderSample() },
                        ComposableDemo("Long list headers") { ListHeaderDemo() },
                    ),
                ),
                Material3DemoCategory("Time Text", TimeTextDemos),
                Material3DemoCategory(
                    "Card",
                    listOf(
                        ComposableDemo("Card") { CardDemo() },
                        ComposableDemo("Outlined Card") { OutlinedCardDemo() },
                        ComposableDemo("App Card") { AppCardDemo() },
                        ComposableDemo("Title Card") { TitleCardDemo() },
                        ComposableDemo("Base Image Card") { Centralize { ImageCardSample() } },
                        ComposableDemo("Image Card") {
                            Centralize { TitleCardWithImageWithTimeAndTitleSample() }
                        },
                        ComposableDemo("Image Card Builder") { ImageCardBuilder() },
                    ),
                ),
                ComposableDemo("Text Toggle Button") { TextToggleButtonDemo() },
                ComposableDemo("Icon Toggle Button") { IconToggleButtonDemo() },
                ComposableDemo("Checkbox Button") { CheckboxButtonDemo() },
                ComposableDemo("Split Checkbox Button") { SplitCheckboxButtonDemo() },
                ComposableDemo("Radio Button") { RadioButtonDemo() },
                ComposableDemo("Split Radio Button") { SplitRadioButtonDemo() },
                ComposableDemo("Switch Button") { SwitchButtonDemo() },
                ComposableDemo("Split Switch Button") { SplitSwitchButtonDemo() },
                Material3DemoCategory("Stepper", StepperDemos),
                Material3DemoCategory("Slider", SliderDemos),
                Material3DemoCategory("Picker", PickerDemos),
                // Requires API level 26 or higher due to java.time dependency.
                *(if (Build.VERSION.SDK_INT >= 26)
                    arrayOf(
                        Material3DemoCategory("TimePicker", TimePickerDemos),
                        Material3DemoCategory("DatePicker", DatePickerDemos),
                    )
                else emptyArray<Material3DemoCategory>()),
                Material3DemoCategory("Progress Indicator", ProgressIndicatorDemos),
                Material3DemoCategory("Scroll Indicator", ScrollIndicatorDemos),
                Material3DemoCategory("Placeholder", PlaceholderDemos),
                Material3DemoCategory(
                    title = "Swipe To Dismiss",
                    listOf(
                        ComposableDemo("Simple") { SimpleSwipeToDismissBox(it.navigateBack) },
                        ComposableDemo("Stateful") { StatefulSwipeToDismissBox() },
                        ComposableDemo("Edge swipe") { EdgeSwipeForSwipeToDismiss(it.navigateBack) },
                    ),
                ),
                Material3DemoCategory(title = "Page Indicator", PageIndicatorDemos),
                Material3DemoCategory(
                    title = "Swipe to Reveal",
                    listOf(
                        ComposableDemo("Single Action with partial reveal") {
                            SwipeToRevealSingleButtonWithPartialReveal()
                        },
                        ComposableDemo("Bi-directional / No partial reveal") {
                            SwipeToRevealBothDirectionsNoPartialReveal()
                        },
                        ComposableDemo("Bi-directional Two Actions") {
                            SwipeToRevealBothDirections()
                        },
                        ComposableDemo("Two Actions") {
                            ScalingLazyDemo { item { SwipeToRevealSample() } }
                        },
                        ComposableDemo("Two Undo Actions") { SwipeToRevealTwoActionsWithUndo() },
                        ComposableDemo("Single action with Card") {
                            ScalingLazyDemo { item { SwipeToRevealSingleActionCardSample() } }
                        },
                        ComposableDemo("In ScalingLazyColumn") {
                            SwipeToRevealInScalingLazyColumn()
                        },
                        ComposableDemo("In TransformingLazyColumn") {
                            SwipeToRevealWithTransformingLazyColumnNoResetOnScrollDemo()
                        },
                        ComposableDemo("In TLC, with icon action") {
                            SwipeToRevealWithTransformingLazyColumnIconActionNoResetOnScrollDemo()
                        },
                        ComposableDemo("In TLC with expansions and deletions") {
                            SwipeToRevealWithTransformingLazyColumnExpansionAndDeletionDemo()
                        },
                        ComposableDemo("In TLC, with reset on scroll") {
                            SwipeToRevealWithTransformingLazyColumnResetOnScrollSample()
                        },
                        ComposableDemo("No Partial Reveal") {
                            ScalingLazyDemo {
                                item { SwipeToRevealNoPartiallyRevealedStateSample() }
                            }
                        },
                        ComposableDemo("Long labels") { SwipeToRevealWithLongLabels() },
                        ComposableDemo("Custom Icons") { SwipeToRevealWithCustomIcons() },
                        ComposableDemo("With edgeSwipeToDismiss") { params ->
                            SwipeToRevealWithEdgeSwipeToDismiss(params.swipeToDismissBoxState)
                        },
                    ),
                ),
                Material3DemoCategory(
                    "Animated Text",
                    if (Build.VERSION.SDK_INT > 31) {
                        listOf(
                            ComposableDemo("Simple animation") {
                                Centralize { AnimatedTextSample() }
                            },
                            ComposableDemo("Animation with button click") {
                                Centralize { AnimatedTextSampleButtonResponse() }
                            },
                            ComposableDemo("Shared Font Registry") {
                                Centralize { AnimatedTextSampleSharedFontRegistry() }
                            },
                        )
                    } else {
                        emptyList()
                    },
                ),
                ComposableDemo("Settings Demo") { SettingsDemo() },
                Material3DemoCategory(
                    title = "TransformingLazyColumn",
                    listOf(
                        ComposableDemo("Notifications") {
                            TransformingLazyColumnNotificationsDemo()
                        },
                        ComposableDemo("Morphing Notifications") {
                            TransformingLazyColumnMorphingNotificationsDemo()
                        },
                        ComposableDemo("Expandable Cards") {
                            TransformingLazyColumnExpandableCardSample()
                        },
                        ComposableDemo("TLC Buttons and Cards") { SurfaceTransformationDemo() },
                        ComposableDemo("Animation Demo") {
                            TransformingLazyColumnAnimationSample()
                        },
                        ComposableDemo("Reduced Motion") {
                            TransformingLazyColumnReducedMotionSample()
                        },
                    ),
                ),
                ComposableDemo("Text") { TextWeightDemo() },
            )
            .sortedBy { it.title },
    )

internal fun showOnClickToast(context: Context) {
    Toast.makeText(context, "Clicked", Toast.LENGTH_SHORT).show()
}

internal fun showOnLongClickToast(context: Context) {
    Toast.makeText(context, "Long clicked", Toast.LENGTH_SHORT).show()
}
