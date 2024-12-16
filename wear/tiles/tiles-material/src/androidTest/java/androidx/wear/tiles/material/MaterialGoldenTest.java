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

package androidx.wear.tiles.material;

import static androidx.wear.tiles.material.RunnerUtils.SCREEN_HEIGHT;
import static androidx.wear.tiles.material.RunnerUtils.SCREEN_WIDTH;
import static androidx.wear.tiles.material.RunnerUtils.runSingleScreenshotTest;
import static androidx.wear.tiles.material.RunnerUtils.waitForNotificationToDisappears;
import static androidx.wear.tiles.material.ScreenshotKt.SCREENSHOT_GOLDEN_PATH;
import static androidx.wear.tiles.material.TestCasesGenerator.generateTestCases;

import android.content.Context;
import android.util.DisplayMetrics;

import androidx.annotation.Dimension;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.screenshot.AndroidXScreenshotTestRule;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

@RunWith(Parameterized.class)
@LargeTest
@SuppressWarnings("deprecation")
public class MaterialGoldenTest {
    private final androidx.wear.tiles.LayoutElementBuilders.LayoutElement mLayoutElement;
    private final String mExpected;

    @Rule
    public AndroidXScreenshotTestRule mScreenshotRule =
            new AndroidXScreenshotTestRule(SCREENSHOT_GOLDEN_PATH);

    public MaterialGoldenTest(
            String expected,
            androidx.wear.tiles.LayoutElementBuilders.LayoutElement layoutElement) {
        mLayoutElement = layoutElement;
        mExpected = expected;
    }

    @Dimension(unit = Dimension.DP)
    static int pxToDp(int px, float scale) {
        return (int) ((px - 0.5f) / scale);
    }

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> data() {
        Context context = InstrumentationRegistry.getInstrumentation().getContext();
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        float scale = displayMetrics.density;

        InstrumentationRegistry.getInstrumentation()
                .getContext()
                .getResources()
                .getDisplayMetrics()
                .setTo(displayMetrics);
        InstrumentationRegistry.getInstrumentation()
                .getTargetContext()
                .getResources()
                .getDisplayMetrics()
                .setTo(displayMetrics);

        androidx.wear.tiles.DeviceParametersBuilders.DeviceParameters deviceParameters =
                new androidx.wear.tiles.DeviceParametersBuilders.DeviceParameters.Builder()
                        .setScreenWidthDp(pxToDp(SCREEN_WIDTH, scale))
                        .setScreenHeightDp(pxToDp(SCREEN_HEIGHT, scale))
                        .setScreenDensity(displayMetrics.density)
                        // Not important for components.
                        .setScreenShape(
                                androidx.wear.tiles.DeviceParametersBuilders.SCREEN_SHAPE_RECT)
                        .build();

        Map<String, androidx.wear.tiles.LayoutElementBuilders.LayoutElement> testCases =
                generateTestCases(context, deviceParameters, "");

        waitForNotificationToDisappears();

        return testCases.entrySet().stream()
                .map(test -> new Object[] {test.getKey(), test.getValue()})
                .collect(Collectors.toList());
    }

    @Test
    public void test() {
        runSingleScreenshotTest(mScreenshotRule, mLayoutElement, mExpected);
    }
}
