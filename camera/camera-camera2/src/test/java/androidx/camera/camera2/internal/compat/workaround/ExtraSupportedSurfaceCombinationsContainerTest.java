/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.camera.camera2.internal.compat.workaround;


import static com.google.common.truth.Truth.assertThat;

import android.os.Build;

import androidx.camera.core.impl.SurfaceCombination;
import androidx.camera.core.impl.SurfaceConfig;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.ParameterizedRobolectricTestRunner;
import org.robolectric.annotation.internal.DoNotInstrument;
import org.robolectric.util.ReflectionHelpers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Unit test for {@link ExtraSupportedSurfaceCombinationsContainer}
 */
@RunWith(ParameterizedRobolectricTestRunner.class)
@DoNotInstrument
public class ExtraSupportedSurfaceCombinationsContainerTest {

    @ParameterizedRobolectricTestRunner.Parameters
    public static Collection<Object[]> data() {
        final List<Object[]> data = new ArrayList<>();
        // Tests for Samsung S7 case
        data.add(new Object[]{new Config(null, "heroqltevzw", null, "0")});
        data.add(new Object[]{new Config(null, "heroqltevzw", null, "1",
                createFullLevelYPYSupportedCombinations())});
        data.add(new Object[]{new Config(null, "heroqltetmo", null, "0")});
        data.add(new Object[]{new Config(null, "heroqltetmo", null, "1",
                createFullLevelYPYSupportedCombinations())});

        // Tests for FULL Pixel devices
        data.add(new Object[]{new Config("Google", null, "Pixel 6", "0",
                createLevel3PrivPrivYuvSubsetConfiguration())});
        data.add(new Object[]{new Config("Google", null, "Pixel 6", "1",
                createLevel3PrivPrivYuvSubsetConfiguration())});
        data.add(new Object[]{new Config("Google", null, "Pixel 6 Pro", "0",
                createLevel3PrivPrivYuvSubsetConfiguration())});
        data.add(new Object[]{new Config("Google", null, "Pixel 6 Pro", "1",
                createLevel3PrivPrivYuvSubsetConfiguration())});
        data.add(new Object[]{new Config("Google", null, "Pixel 7", "0",
                createLevel3PrivPrivYuvSubsetConfiguration())});
        data.add(new Object[]{new Config("Google", null, "Pixel 7", "1",
                createLevel3PrivPrivYuvSubsetConfiguration())});
        data.add(new Object[]{new Config("Google", null, "Pixel 7 Pro", "0",
                createLevel3PrivPrivYuvSubsetConfiguration())});
        data.add(new Object[]{new Config("Google", null, "Pixel 7 Pro", "1",
                createLevel3PrivPrivYuvSubsetConfiguration())});
        data.add(new Object[]{new Config("Google", null, "Pixel 8", "0",
                createLevel3PrivPrivYuvSubsetConfiguration())});
        data.add(new Object[]{new Config("Google", null, "Pixel 8", "1",
                createLevel3PrivPrivYuvSubsetConfiguration())});
        data.add(new Object[]{new Config("Google", null, "Pixel 8 Pro", "0",
                createLevel3PrivPrivYuvSubsetConfiguration())});
        data.add(new Object[]{new Config("Google", null, "Pixel 8 Pro", "1",
                createLevel3PrivPrivYuvSubsetConfiguration())});
        data.add(new Object[]{new Config("Google", null, "Pixel 9", "0",
                createLevel3PrivPrivYuvSubsetConfiguration())});
        data.add(new Object[]{new Config("Google", null, "Pixel 9", "1",
                createLevel3PrivPrivYuvSubsetConfiguration())});
        data.add(new Object[]{new Config("Google", null, "Pixel 9 Pro", "0",
                createLevel3PrivPrivYuvSubsetConfiguration())});
        data.add(new Object[]{new Config("Google", null, "Pixel 9 Pro", "1",
                createLevel3PrivPrivYuvSubsetConfiguration())});
        data.add(new Object[]{new Config("Google", null, "Pixel 9 Pro XL", "0",
                createLevel3PrivPrivYuvSubsetConfiguration())});
        data.add(new Object[]{new Config("Google", null, "Pixel 9 Pro XL", "1",
                createLevel3PrivPrivYuvSubsetConfiguration())});
        data.add(new Object[]{new Config("Google", null, "Pixel 9 Pro Fold", "0",
                createLevel3PrivPrivYuvSubsetConfiguration())});
        data.add(new Object[]{new Config("Google", null, "Pixel 9 Pro Fold", "1",
                createLevel3PrivPrivYuvSubsetConfiguration())});

        // Tests for FULL Samsung devices
        data.add(new Object[]{new Config("Samsung", null, "SCG25", "0",
                createLevel3PrivPrivYuvSubsetConfiguration())});
        data.add(new Object[]{new Config("Samsung", null, "SCG25", "1",
                createLevel3PrivPrivYuvSubsetConfiguration())});
        data.add(new Object[]{new Config("Samsung", null, "SM-S9210", "0",
                createLevel3PrivPrivYuvSubsetConfiguration())});
        data.add(new Object[]{new Config("Samsung", null, "SM-S9213", "1",
                createLevel3PrivPrivYuvSubsetConfiguration())});
        data.add(new Object[]{new Config("Samsung", null, "SM-S926B", "0",
                createLevel3PrivPrivYuvSubsetConfiguration())});
        data.add(new Object[]{new Config("Samsung", null, "SM-S926U", "1",
                createLevel3PrivPrivYuvSubsetConfiguration())});
        data.add(new Object[]{new Config("Samsung", null, "SM-S928U1", "0",
                createLevel3PrivPrivYuvSubsetConfiguration())});
        data.add(new Object[]{new Config("Samsung", null, "SM-S928U", "1",
                createLevel3PrivPrivYuvSubsetConfiguration())});
        data.add(new Object[]{new Config("Samsung", null, "SC-51E", "0",
                createLevel3PrivPrivYuvSubsetConfiguration())});
        data.add(new Object[]{new Config("Samsung", null, "SC-51E", "1",
                createLevel3PrivPrivYuvSubsetConfiguration())});
        data.add(new Object[]{new Config("Samsung", null, "SC-52E", "0",
                createLevel3PrivPrivYuvSubsetConfiguration())});
        data.add(new Object[]{new Config("Samsung", null, "SC-52E", "1",
                createLevel3PrivPrivYuvSubsetConfiguration())});
        data.add(new Object[]{new Config("Samsung", null, "SCG26", "0",
                createLevel3PrivPrivYuvSubsetConfiguration())});
        data.add(new Object[]{new Config("Samsung", null, "SCG26", "1",
                createLevel3PrivPrivYuvSubsetConfiguration())});
        data.add(new Object[]{new Config("Samsung", null, "SM-S931U", "0",
                createLevel3PrivPrivYuvSubsetConfiguration())});
        data.add(new Object[]{new Config("Samsung", null, "SM-S931B", "1",
                createLevel3PrivPrivYuvSubsetConfiguration())});
        data.add(new Object[]{new Config("Samsung", null, "SM-S936U", "0",
                createLevel3PrivPrivYuvSubsetConfiguration())});
        data.add(new Object[]{new Config("Samsung", null, "SM-S936L", "1",
                createLevel3PrivPrivYuvSubsetConfiguration())});
        data.add(new Object[]{new Config("Samsung", null, "SM-S937", "0",
                createLevel3PrivPrivYuvSubsetConfiguration())});
        data.add(new Object[]{new Config("Samsung", null, "SM-S937U", "1",
                createLevel3PrivPrivYuvSubsetConfiguration())});
        data.add(new Object[]{new Config("Samsung", null, "SM-S938N", "0",
                createLevel3PrivPrivYuvSubsetConfiguration())});
        data.add(new Object[]{new Config("Samsung", null, "SM-S938R", "1",
                createLevel3PrivPrivYuvSubsetConfiguration())});
        data.add(new Object[]{new Config("Samsung", null, "SCG31", "0",
                createLevel3PrivPrivYuvSubsetConfiguration())});
        data.add(new Object[]{new Config("Samsung", null, "SCG31", "1",
                createLevel3PrivPrivYuvSubsetConfiguration())});
        data.add(new Object[]{new Config("Samsung", null, "SCG32", "0",
                createLevel3PrivPrivYuvSubsetConfiguration())});
        data.add(new Object[]{new Config("Samsung", null, "SCG32", "1",
                createLevel3PrivPrivYuvSubsetConfiguration())});
        data.add(new Object[]{new Config("Samsung", null, "SC-51F", "0",
                createLevel3PrivPrivYuvSubsetConfiguration())});
        data.add(new Object[]{new Config("Samsung", null, "SC-51F", "1",
                createLevel3PrivPrivYuvSubsetConfiguration())});
        data.add(new Object[]{new Config("Samsung", null, "SC-52F", "0",
                createLevel3PrivPrivYuvSubsetConfiguration())});
        data.add(new Object[]{new Config("Samsung", null, "SC-52F", "1",
                createLevel3PrivPrivYuvSubsetConfiguration())});

        // Other cases
        data.add(new Object[]{new Config(null, null, null, "0")});
        return data;
    }

    private final @NonNull Config mConfig;

    public ExtraSupportedSurfaceCombinationsContainerTest(final @NonNull Config config) {
        mConfig = config;

    }

    @Test
    public void checkExtraSupportedSurfaceCombinations() {
        // Set up brand properties
        if (mConfig.mBrand != null) {
            ReflectionHelpers.setStaticField(Build.class, "BRAND", mConfig.mBrand);
        }

        // Set up device properties
        if (mConfig.mDevice != null) {
            ReflectionHelpers.setStaticField(Build.class, "DEVICE", mConfig.mDevice);
        }

        // Set up model properties
        if (mConfig.mModel != null) {
            ReflectionHelpers.setStaticField(Build.class, "MODEL", mConfig.mModel);
        }

        // Initializes ExtraSupportedSurfaceCombinationsContainer instance with camera id
        final ExtraSupportedSurfaceCombinationsContainer
                extraSupportedSurfaceCombinationsContainer =
                new ExtraSupportedSurfaceCombinationsContainer();

        // Gets the extra supported surface combinations on the device
        List<SurfaceCombination> extraSurfaceCombinations =
                extraSupportedSurfaceCombinationsContainer.get(mConfig.mCameraId);

        for (SurfaceCombination expectedSupportedSurfaceCombination :
                mConfig.mExpectedSupportedSurfaceCombinations) {
            boolean isSupported = false;

            // Checks the combination is supported by the list retrieved from the
            // ExtraSupportedSurfaceCombinationsContainer.
            for (SurfaceCombination extraSurfaceCombination : extraSurfaceCombinations) {
                if (extraSurfaceCombination.getOrderedSupportedSurfaceConfigList(
                        expectedSupportedSurfaceCombination.getSurfaceConfigList()) != null) {
                    isSupported = true;
                    break;
                }
            }

            assertThat(isSupported).isTrue();
        }
    }

    private static SurfaceCombination[] createFullLevelYPYSupportedCombinations() {
        // (YUV, ANALYSIS) + (PRIV, PREVIEW) + (YUV, MAXIMUM)
        SurfaceCombination surfaceCombination = new SurfaceCombination();
        surfaceCombination.addSurfaceConfig(SurfaceConfig.create(SurfaceConfig.ConfigType.YUV,
                SurfaceConfig.ConfigSize.VGA));
        surfaceCombination.addSurfaceConfig(SurfaceConfig.create(SurfaceConfig.ConfigType.PRIV,
                SurfaceConfig.ConfigSize.PREVIEW));
        surfaceCombination.addSurfaceConfig(SurfaceConfig.create(SurfaceConfig.ConfigType.YUV,
                SurfaceConfig.ConfigSize.MAXIMUM));
        return new SurfaceCombination[]{surfaceCombination};
    }

    private static SurfaceCombination[] createLevel3PrivPrivYuvSubsetConfiguration() {
        // (PRIV, PREVIEW) + (PRIV, ANALYSIS) + (YUV, MAXIMUM)
        SurfaceCombination surfaceCombination = new SurfaceCombination();
        surfaceCombination.addSurfaceConfig(SurfaceConfig.create(SurfaceConfig.ConfigType.PRIV,
                SurfaceConfig.ConfigSize.PREVIEW));
        surfaceCombination.addSurfaceConfig(SurfaceConfig.create(SurfaceConfig.ConfigType.PRIV,
                SurfaceConfig.ConfigSize.VGA));
        surfaceCombination.addSurfaceConfig(SurfaceConfig.create(SurfaceConfig.ConfigType.YUV,
                SurfaceConfig.ConfigSize.MAXIMUM));

        return new SurfaceCombination[]{surfaceCombination};
    }

    static class Config {
        final @Nullable String mBrand;
        final @Nullable String mDevice;
        final @Nullable String mModel;
        final @NonNull String mCameraId;
        final SurfaceCombination @NonNull [] mExpectedSupportedSurfaceCombinations;

        Config(@Nullable String brand, @Nullable String device, @Nullable String model,
                @NonNull String cameraId,
                SurfaceCombination @NonNull ... expectedSupportedSurfaceCombinations) {
            mBrand = brand;
            mDevice = device;
            mModel = model;
            mCameraId = cameraId;
            mExpectedSupportedSurfaceCombinations = expectedSupportedSurfaceCombinations;
        }
    }
}
