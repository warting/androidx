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

package androidx.wear.protolayout.material;

import static androidx.wear.protolayout.material.RunnerUtils.SCREEN_HEIGHT;
import static androidx.wear.protolayout.material.RunnerUtils.SCREEN_WIDTH;
import static androidx.wear.protolayout.material.RunnerUtils.convertToTestParameters;
import static androidx.wear.protolayout.material.RunnerUtils.getFontScale;
import static androidx.wear.protolayout.material.RunnerUtils.runSingleScreenshotTest;
import static androidx.wear.protolayout.material.RunnerUtils.setFontScale;
import static androidx.wear.protolayout.material.RunnerUtils.waitForNotificationToDisappears;
import static androidx.wear.protolayout.material.ScreenshotKt.SCREENSHOT_GOLDEN_PATH;
import static androidx.wear.protolayout.material.TestCasesGenerator.XXXL_SCALE_SUFFIX;
import static androidx.wear.protolayout.material.TestCasesGenerator.generateTestCases;
import static androidx.wear.protolayout.material.TestCasesGenerator.generateTextTestCasesLtrOnly;
import static androidx.wear.protolayout.material.TestCasesGenerator.generateTextTestCasesRtlOnly;

import android.content.Context;
import android.os.Build;
import android.util.DisplayMetrics;

import androidx.annotation.Dimension;
import androidx.test.filters.LargeTest;
import androidx.test.filters.SdkSuppress;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.screenshot.AndroidXScreenshotTestRule;
import androidx.wear.protolayout.DeviceParametersBuilders;
import androidx.wear.protolayout.DeviceParametersBuilders.DeviceParameters;
import androidx.wear.protolayout.material.RunnerUtils.TestCase;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@RunWith(Parameterized.class)
@LargeTest
@SdkSuppress(
        minSdkVersion = 35,
        maxSdkVersion = 35
)
public class MaterialGoldenXLTest {
    private static final float FONT_SCALE_XXXL = 1.24f;

    private static float originalFontScale;

    private final TestCase mTestCase;
    private final String mExpected;

    @Rule
    public AndroidXScreenshotTestRule mScreenshotRule =
            new AndroidXScreenshotTestRule(SCREENSHOT_GOLDEN_PATH);

    public MaterialGoldenXLTest(String expected, TestCase testCase) {
        mTestCase = testCase;
        mExpected = expected;
    }

    @Dimension(unit = Dimension.DP)
    static int pxToDp(int px, float scale) {
        return (int) ((px - 0.5f) / scale);
    }

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> data() throws Exception {
        // These "parameters" methods are called before any parameterized test (from any class)
        // executes. We set and later reset the font here to have the correct context during test
        // generation. We later set and reset the font for the actual test in BeforeClass/AfterClass
        // methods.
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        originalFontScale =
                getFontScale(InstrumentationRegistry.getInstrumentation().getTargetContext());
        setFontScale(context, FONT_SCALE_XXXL);

        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();

        float scale = displayMetrics.density;
        DeviceParameters deviceParameters =
                new DeviceParameters.Builder()
                        .setScreenWidthDp(pxToDp(SCREEN_WIDTH, scale))
                        .setScreenHeightDp(pxToDp(SCREEN_HEIGHT, scale))
                        .setScreenDensity(displayMetrics.density)
                        .setFontScale(context.getResources().getConfiguration().fontScale)
                        // Not important for components.
                        .setScreenShape(DeviceParametersBuilders.SCREEN_SHAPE_RECT)
                        .build();

        List<Object[]> testCaseList = new ArrayList<>();
        testCaseList.addAll(
                convertToTestParameters(
                        generateTestCases(context, deviceParameters, XXXL_SCALE_SUFFIX),
                        /* isForRtl= */ true,
                        /* isForLtr= */ true));
        testCaseList.addAll(
                convertToTestParameters(
                        generateTextTestCasesRtlOnly(context, deviceParameters, XXXL_SCALE_SUFFIX),
                        /* isForRtl= */ true,
                        /* isForLtr= */ false));
        testCaseList.addAll(
                convertToTestParameters(
                        generateTextTestCasesLtrOnly(context, deviceParameters, XXXL_SCALE_SUFFIX),
                        /* isForRtl= */ false,
                        /* isForLtr= */ true));

        // Restore state before this method, so other test have correct context.
        setFontScale(context, originalFontScale);
        waitForNotificationToDisappears();

        return testCaseList;
    }

    @Before
    public void setUp() {
        setFontScale(
                InstrumentationRegistry.getInstrumentation().getTargetContext(), FONT_SCALE_XXXL);
    }

    @After
    public void tearDown() {
        setFontScale(
                InstrumentationRegistry.getInstrumentation().getTargetContext(), originalFontScale);
    }

    @Test
    @Ignore("b/376758169")
    public void test() {
        runSingleScreenshotTest(mScreenshotRule, mTestCase, mExpected);
    }
}
