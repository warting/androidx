/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.camera.camera2.internal.compat;

import static android.hardware.camera2.CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.hardware.camera2.CameraCharacteristics;

import androidx.test.filters.SdkSuppress;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.internal.DoNotInstrument;
import org.robolectric.shadow.api.Shadow;
import org.robolectric.shadows.ShadowCameraCharacteristics;

@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
public class CameraCharacteristicsCompatTest {
    private CameraCharacteristics mCharacteristics;
    private static final int SENSOR_ORIENTATION_VAL = 270;
    private static final String CAMERA_ID_0 = "0";

    @Before
    public void setUp() {
        mCharacteristics = ShadowCameraCharacteristics.newCameraCharacteristics();
        ShadowCameraCharacteristics shadowCharacteristics0 = Shadow.extract(mCharacteristics);
        shadowCharacteristics0.set(CameraCharacteristics.CONTROL_MAX_REGIONS_AE, 1);
        shadowCharacteristics0.set(CameraCharacteristics.CONTROL_MAX_REGIONS_AF, 2);
        shadowCharacteristics0.set(CameraCharacteristics.CONTROL_MAX_REGIONS_AWB, 3);

        shadowCharacteristics0.set(CameraCharacteristics.CONTROL_AE_AVAILABLE_MODES, null);
        shadowCharacteristics0.set(
                CameraCharacteristics.SENSOR_ORIENTATION, SENSOR_ORIENTATION_VAL);
    }

    @Test
    public void canGetCorrectValues() {
        CameraCharacteristicsCompat characteristicsCompat =
                CameraCharacteristicsCompat.toCameraCharacteristicsCompat(mCharacteristics,
                        CAMERA_ID_0);

        assertThat(characteristicsCompat.get(CameraCharacteristics.CONTROL_MAX_REGIONS_AE))
                .isEqualTo(mCharacteristics.get(CameraCharacteristics.CONTROL_MAX_REGIONS_AE));

        assertThat(characteristicsCompat.get(CameraCharacteristics.CONTROL_MAX_REGIONS_AF))
                .isEqualTo(mCharacteristics.get(CameraCharacteristics.CONTROL_MAX_REGIONS_AF));
    }

    @Test
    public void canGetCachedValues() {
        CameraCharacteristicsCompat characteristicsCompat =
                CameraCharacteristicsCompat.toCameraCharacteristicsCompat(mCharacteristics,
                        CAMERA_ID_0);


        assertThat(characteristicsCompat.get(CameraCharacteristics.CONTROL_MAX_REGIONS_AE))
                .isEqualTo(mCharacteristics.get(CameraCharacteristics.CONTROL_MAX_REGIONS_AE));

        assertThat(characteristicsCompat.get(CameraCharacteristics.CONTROL_MAX_REGIONS_AE))
                .isEqualTo(mCharacteristics.get(CameraCharacteristics.CONTROL_MAX_REGIONS_AE));
    }

    @Test
    public void canGetNullValue() {
        CameraCharacteristicsCompat characteristicsCompat =
                CameraCharacteristicsCompat.toCameraCharacteristicsCompat(mCharacteristics,
                        CAMERA_ID_0);

        // CONTROL_AE_AVAILABLE_MODES is set to null in setUp
        assertThat(characteristicsCompat.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_MODES))
                .isNull();
        // INFO_SUPPORTED_HARDWARE_LEVEL is not set.
        assertThat(characteristicsCompat.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL))
                .isNull();
    }

    @Config(minSdk = 28)
    @SdkSuppress(minSdkVersion = 28)
    @Test
    public void getPhysicalCameraIds_invokeCameraCharacteristics_api28() {
        CameraCharacteristics cameraCharacteristics = mock(CameraCharacteristics.class);
        CameraCharacteristicsCompat characteristicsCompat =
                CameraCharacteristicsCompat.toCameraCharacteristicsCompat(cameraCharacteristics,
                        CAMERA_ID_0);

        characteristicsCompat.getPhysicalCameraIds();
        verify(cameraCharacteristics).getPhysicalCameraIds();
    }

    @Config(maxSdk = 27)
    @Test
    public void getPhysicalCameraIds_returnEmptyList_below28() {
        CameraCharacteristicsCompat characteristicsCompat =
                CameraCharacteristicsCompat.toCameraCharacteristicsCompat(mCharacteristics,
                        CAMERA_ID_0);
        assertThat(characteristicsCompat.getPhysicalCameraIds()).isEmpty();
    }

    @Test
    public void getSensorOrientation_shouldNotCache() {
        CameraCharacteristics cameraCharacteristics = spy(mCharacteristics);
        CameraCharacteristicsCompat characteristicsCompat =
                CameraCharacteristicsCompat.toCameraCharacteristicsCompat(cameraCharacteristics,
                        CAMERA_ID_0);
        assertThat(characteristicsCompat.get(CameraCharacteristics.SENSOR_ORIENTATION))
                .isEqualTo(SENSOR_ORIENTATION_VAL);

        // Call get() twice, cameraCharacteristics.get() should be called twice as well.
        assertThat(characteristicsCompat.get(CameraCharacteristics.SENSOR_ORIENTATION))
                .isEqualTo(SENSOR_ORIENTATION_VAL);
        verify(cameraCharacteristics, times(2)).get(CameraCharacteristics.SENSOR_ORIENTATION);
    }

    @Test(expected = IllegalArgumentException.class)
    public void getStreamConfigurationMapCompat_assertionError() {
        CameraCharacteristics cameraCharacteristics = spy(mCharacteristics);
        when(cameraCharacteristics.get(SCALER_STREAM_CONFIGURATION_MAP)).thenThrow(
                new AssertionError(
                        "At least one stream configuration for IMPLEMENTATION_DEFINED must exist"));
        CameraCharacteristicsCompat characteristicsCompat =
                CameraCharacteristicsCompat.toCameraCharacteristicsCompat(cameraCharacteristics,
                        CAMERA_ID_0);

        characteristicsCompat.getStreamConfigurationMapCompat();
    }

    @Test(expected = IllegalArgumentException.class)
    public void getStreamConfigurationMapCompat_nullPointerException() {
        CameraCharacteristics cameraCharacteristics = spy(mCharacteristics);
        when(cameraCharacteristics.get(SCALER_STREAM_CONFIGURATION_MAP)).thenThrow(
                new NullPointerException(
                        "At least one of color/depth/heic configurations must not be null"));
        CameraCharacteristicsCompat characteristicsCompat =
                CameraCharacteristicsCompat.toCameraCharacteristicsCompat(cameraCharacteristics,
                        CAMERA_ID_0);

        characteristicsCompat.getStreamConfigurationMapCompat();
    }
}
