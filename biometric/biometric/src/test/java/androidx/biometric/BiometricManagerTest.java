/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.biometric;

import static androidx.biometric.BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE;
import static androidx.biometric.BiometricManager.BIOMETRIC_ERROR_IDENTITY_CHECK_NOT_ACTIVE;
import static androidx.biometric.BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED;
import static androidx.biometric.BiometricManager.BIOMETRIC_ERROR_NOT_ENABLED_FOR_APPS;
import static androidx.biometric.BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE;
import static androidx.biometric.BiometricManager.BIOMETRIC_ERROR_UNSUPPORTED;
import static androidx.biometric.BiometricManager.BIOMETRIC_STATUS_UNKNOWN;
import static androidx.biometric.BiometricManager.BIOMETRIC_SUCCESS;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.res.Resources;
import android.os.Build;

import androidx.biometric.BiometricManager.AuthenticatorTypes;
import androidx.biometric.BiometricManager.Authenticators;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.filters.SdkSuppress;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.internal.DoNotInstrument;

@SuppressWarnings("deprecation")
@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
public class BiometricManagerTest {
    @Rule
    public final MockitoRule mocks = MockitoJUnit.rule();
    @Mock private androidx.core.hardware.fingerprint.FingerprintManagerCompat mFingerprintManager;

    private Context mContext;

    @Before
    public void setUp() {
        mContext = ApplicationProvider.getApplicationContext();
    }

    @Test
    @Config(minSdk = Build.VERSION_CODES.Q)
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.Q)
    public void testCanAuthenticate_ReturnsSuccess_WhenManagerReturnsSuccess_OnApi29AndAbove() {
        final int authenticators = Authenticators.BIOMETRIC_WEAK;
        final android.hardware.biometrics.BiometricManager frameworkBiometricManager =
                mock(android.hardware.biometrics.BiometricManager.class);
        when(frameworkBiometricManager.canAuthenticate()).thenReturn(BIOMETRIC_SUCCESS);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            when(frameworkBiometricManager.canAuthenticate(authenticators))
                    .thenReturn(BIOMETRIC_SUCCESS);
        }
        when(mFingerprintManager.isHardwareDetected()).thenReturn(false);
        when(mFingerprintManager.hasEnrolledFingerprints()).thenReturn(false);

        final BiometricManager biometricManager = new BiometricManager(
                new TestInjector.Builder(mContext)
                        .setBiometricManager(frameworkBiometricManager)
                        .setFingerprintManager(mFingerprintManager)
                        .setDeviceSecurable(true)
                        .setDeviceSecuredWithCredential(true)
                        .setFingerprintHardwarePresent(true)
                        .build());

        assertThat(biometricManager.canAuthenticate(authenticators)).isEqualTo(BIOMETRIC_SUCCESS);
    }

    @Test
    @Config(minSdk = Build.VERSION_CODES.Q)
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.Q)
    public void testCanAuthenticate_ReturnsError_WhenManagerReturnsNoneEnrolled_OnApi29AndAbove() {
        final int authenticators = Authenticators.BIOMETRIC_WEAK;
        final android.hardware.biometrics.BiometricManager frameworkBiometricManager =
                mock(android.hardware.biometrics.BiometricManager.class);
        when(frameworkBiometricManager.canAuthenticate()).thenReturn(BIOMETRIC_ERROR_NONE_ENROLLED);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            when(frameworkBiometricManager.canAuthenticate(authenticators))
                    .thenReturn(BIOMETRIC_ERROR_NONE_ENROLLED);
        }
        when(mFingerprintManager.isHardwareDetected()).thenReturn(true);
        when(mFingerprintManager.hasEnrolledFingerprints()).thenReturn(true);

        final BiometricManager biometricManager = new BiometricManager(
                new TestInjector.Builder(mContext)
                        .setBiometricManager(frameworkBiometricManager)
                        .setFingerprintManager(mFingerprintManager)
                        .setDeviceSecurable(true)
                        .setDeviceSecuredWithCredential(true)
                        .setFingerprintHardwarePresent(true)
                        .build());

        assertThat(biometricManager.canAuthenticate(authenticators))
                .isEqualTo(BIOMETRIC_ERROR_NONE_ENROLLED);
    }

    @Test
    @Config(minSdk = Build.VERSION_CODES.Q)
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.Q)
    public void testCanAuthenticate_ReturnsError_WhenManagerReturnsNoHardware_OnApi29AndAbove() {
        final int authenticators = Authenticators.BIOMETRIC_WEAK;
        final android.hardware.biometrics.BiometricManager frameworkBiometricManager =
                mock(android.hardware.biometrics.BiometricManager.class);
        when(frameworkBiometricManager.canAuthenticate()).thenReturn(BIOMETRIC_ERROR_NO_HARDWARE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            when(frameworkBiometricManager.canAuthenticate(authenticators))
                    .thenReturn(BIOMETRIC_ERROR_NO_HARDWARE);
        }
        when(mFingerprintManager.isHardwareDetected()).thenReturn(true);
        when(mFingerprintManager.hasEnrolledFingerprints()).thenReturn(true);

        final BiometricManager biometricManager = new BiometricManager(
                new TestInjector.Builder(mContext)
                        .setBiometricManager(frameworkBiometricManager)
                        .setFingerprintManager(mFingerprintManager)
                        .setDeviceSecurable(true)
                        .setDeviceSecuredWithCredential(true)
                        .setFingerprintHardwarePresent(true)
                        .build());

        assertThat(biometricManager.canAuthenticate(authenticators))
                .isEqualTo(BIOMETRIC_ERROR_NO_HARDWARE);
    }

    @Test
    @Config(sdk = Build.VERSION_CODES.Q)
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.Q)
    public void testCanAuthenticate_ReturnsError_WhenCombinationNotSupported_OnApi29() {
        final android.hardware.biometrics.BiometricManager frameworkBiometricManager =
                mock(android.hardware.biometrics.BiometricManager.class);
        when(frameworkBiometricManager.canAuthenticate()).thenReturn(BIOMETRIC_SUCCESS);
        when(mFingerprintManager.isHardwareDetected()).thenReturn(true);
        when(mFingerprintManager.hasEnrolledFingerprints()).thenReturn(true);

        final BiometricManager biometricManager = new BiometricManager(
                new TestInjector.Builder(mContext)
                        .setBiometricManager(frameworkBiometricManager)
                        .setFingerprintManager(mFingerprintManager)
                        .setDeviceSecurable(true)
                        .setDeviceSecuredWithCredential(true)
                        .setFingerprintHardwarePresent(true)
                        .build());

        final int authenticators = Authenticators.DEVICE_CREDENTIAL;
        assertThat(biometricManager.canAuthenticate(authenticators))
                .isEqualTo(BIOMETRIC_ERROR_UNSUPPORTED);
    }

    @Test
    @Config(maxSdk = Build.VERSION_CODES.P)
    public void testCanAuthenticate_ReturnsError_WhenCombinationNotSupported_OnApi28AndBelow() {
        when(mFingerprintManager.isHardwareDetected()).thenReturn(true);
        when(mFingerprintManager.hasEnrolledFingerprints()).thenReturn(true);

        final BiometricManager biometricManager = new BiometricManager(
                new TestInjector.Builder(mContext)
                        .setFingerprintManager(mFingerprintManager)
                        .setDeviceSecurable(true)
                        .setDeviceSecuredWithCredential(true)
                        .setFingerprintHardwarePresent(true)
                        .build());

        final int authenticators = Authenticators.DEVICE_CREDENTIAL;
        assertThat(biometricManager.canAuthenticate(authenticators))
                .isEqualTo(BIOMETRIC_ERROR_UNSUPPORTED);
    }

    @Test
    @Config(sdk = Build.VERSION_CODES.Q)
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.Q)
    public void testCanAuthenticate_ReturnsError_WhenNoAuthenticatorsAllowed_OnApi29() {
        final android.hardware.biometrics.BiometricManager frameworkBiometricManager =
                mock(android.hardware.biometrics.BiometricManager.class);
        when(frameworkBiometricManager.canAuthenticate()).thenReturn(BIOMETRIC_SUCCESS);
        when(mFingerprintManager.isHardwareDetected()).thenReturn(true);
        when(mFingerprintManager.hasEnrolledFingerprints()).thenReturn(true);

        final BiometricManager biometricManager = new BiometricManager(
                new TestInjector.Builder(mContext)
                        .setBiometricManager(frameworkBiometricManager)
                        .setFingerprintManager(mFingerprintManager)
                        .setDeviceSecurable(true)
                        .setDeviceSecuredWithCredential(true)
                        .setFingerprintHardwarePresent(true)
                        .build());

        assertThat(biometricManager.canAuthenticate(0)).isEqualTo(BIOMETRIC_ERROR_NO_HARDWARE);
    }

    @Test
    @Config(maxSdk = Build.VERSION_CODES.P)
    public void testCanAuthenticate_ReturnsError_WhenNoAuthenticatorsAllowed_OnApi28AndBelow() {
        when(mFingerprintManager.isHardwareDetected()).thenReturn(true);
        when(mFingerprintManager.hasEnrolledFingerprints()).thenReturn(true);

        final BiometricManager biometricManager = new BiometricManager(
                new TestInjector.Builder(mContext)
                        .setFingerprintManager(mFingerprintManager)
                        .setDeviceSecurable(true)
                        .setDeviceSecuredWithCredential(true)
                        .setFingerprintHardwarePresent(true)
                        .build());

        assertThat(biometricManager.canAuthenticate(0)).isEqualTo(BIOMETRIC_ERROR_NO_HARDWARE);
    }

    @Test
    @Config(sdk = Build.VERSION_CODES.Q)
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.Q)
    public void testCanAuthenticate_ReturnsError_WhenDeviceNotSecurable_OnApi29() {
        final android.hardware.biometrics.BiometricManager frameworkBiometricManager =
                mock(android.hardware.biometrics.BiometricManager.class);
        when(frameworkBiometricManager.canAuthenticate()).thenReturn(BIOMETRIC_SUCCESS);
        when(mFingerprintManager.isHardwareDetected()).thenReturn(false);
        when(mFingerprintManager.hasEnrolledFingerprints()).thenReturn(false);

        final BiometricManager biometricManager = new BiometricManager(
                new TestInjector.Builder(mContext)
                        .setBiometricManager(frameworkBiometricManager)
                        .setFingerprintManager(mFingerprintManager)
                        .setFingerprintHardwarePresent(true)
                        .build());

        final int authenticators = Authenticators.BIOMETRIC_WEAK;
        assertThat(biometricManager.canAuthenticate(authenticators))
                .isEqualTo(BIOMETRIC_ERROR_NO_HARDWARE);
    }

    @Test
    @Config(maxSdk = Build.VERSION_CODES.P)
    public void testCanAuthenticate_ReturnsError_WhenDeviceNotSecurable_OnApi28AndBelow() {
        when(mFingerprintManager.isHardwareDetected()).thenReturn(true);
        when(mFingerprintManager.hasEnrolledFingerprints()).thenReturn(true);

        final BiometricManager biometricManager = new BiometricManager(
                new TestInjector.Builder(mContext)
                        .setFingerprintManager(mFingerprintManager)
                        .setFingerprintHardwarePresent(true)
                        .build());

        final int authenticators = Authenticators.BIOMETRIC_WEAK;
        assertThat(biometricManager.canAuthenticate(authenticators))
                .isEqualTo(BIOMETRIC_ERROR_NO_HARDWARE);
    }

    @Test
    @Config(sdk = Build.VERSION_CODES.Q)
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.Q)
    public void testCanAuthenticate_ReturnsError_WhenUnsecured_BiometricOnly_OnApi29() {
        final android.hardware.biometrics.BiometricManager frameworkBiometricManager =
                mock(android.hardware.biometrics.BiometricManager.class);
        when(frameworkBiometricManager.canAuthenticate()).thenReturn(BIOMETRIC_ERROR_NO_HARDWARE);
        when(mFingerprintManager.isHardwareDetected()).thenReturn(false);
        when(mFingerprintManager.hasEnrolledFingerprints()).thenReturn(false);

        final BiometricManager biometricManager = new BiometricManager(
                new TestInjector.Builder(mContext)
                        .setBiometricManager(frameworkBiometricManager)
                        .setDeviceSecurable(true)
                        .setFingerprintHardwarePresent(false)
                        .build());

        final int authenticators = Authenticators.BIOMETRIC_WEAK;
        assertThat(biometricManager.canAuthenticate(authenticators))
                .isEqualTo(BIOMETRIC_ERROR_NO_HARDWARE);
    }

    @Test
    @Config(maxSdk = Build.VERSION_CODES.P)
    public void testCanAuthenticate_ReturnsError_WhenUnsecured_BiometricOnly_OnApi28AndBelow() {
        when(mFingerprintManager.isHardwareDetected()).thenReturn(false);
        when(mFingerprintManager.hasEnrolledFingerprints()).thenReturn(false);

        final BiometricManager biometricManager = new BiometricManager(
                new TestInjector.Builder(mContext)
                        .setFingerprintManager(mFingerprintManager)
                        .setDeviceSecurable(true)
                        .setFingerprintHardwarePresent(false)
                        .build());

        final int authenticators = Authenticators.BIOMETRIC_WEAK;
        assertThat(biometricManager.canAuthenticate(authenticators))
                .isEqualTo(BIOMETRIC_ERROR_NO_HARDWARE);
    }

    @Test
    @Config(sdk = Build.VERSION_CODES.Q)
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.Q)
    public void testCanAuthenticate_ReturnsError_WhenUnsecured_CredentialAllowed_OnApi29() {
        final android.hardware.biometrics.BiometricManager frameworkBiometricManager =
                mock(android.hardware.biometrics.BiometricManager.class);
        when(frameworkBiometricManager.canAuthenticate()).thenReturn(BIOMETRIC_ERROR_NO_HARDWARE);
        when(mFingerprintManager.isHardwareDetected()).thenReturn(false);
        when(mFingerprintManager.hasEnrolledFingerprints()).thenReturn(false);

        final BiometricManager biometricManager = new BiometricManager(
                new TestInjector.Builder(mContext)
                        .setBiometricManager(frameworkBiometricManager)
                        .setDeviceSecurable(true)
                        .setFingerprintHardwarePresent(false)
                        .build());

        final int authenticators = Authenticators.BIOMETRIC_WEAK | Authenticators.DEVICE_CREDENTIAL;
        assertThat(biometricManager.canAuthenticate(authenticators))
                .isEqualTo(BIOMETRIC_ERROR_NONE_ENROLLED);
    }

    @Test
    @Config(maxSdk = Build.VERSION_CODES.P)
    public void testCanAuthenticate_ReturnsError_WhenUnsecured_CredentialAllowed_OnApi28AndBelow() {
        when(mFingerprintManager.isHardwareDetected()).thenReturn(false);
        when(mFingerprintManager.hasEnrolledFingerprints()).thenReturn(false);

        final BiometricManager biometricManager = new BiometricManager(
                new TestInjector.Builder(mContext)
                        .setFingerprintManager(mFingerprintManager)
                        .setDeviceSecurable(true)
                        .setFingerprintHardwarePresent(false)
                        .build());

        final int authenticators = Authenticators.BIOMETRIC_WEAK | Authenticators.DEVICE_CREDENTIAL;
        assertThat(biometricManager.canAuthenticate(authenticators))
                .isEqualTo(BIOMETRIC_ERROR_NONE_ENROLLED);
    }

    @Test
    @Config(sdk = Build.VERSION_CODES.Q)
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.Q)
    public void testCanAuthenticate_ReturnsSuccess_WhenDeviceCredentialAvailable_OnApi29() {
        final android.hardware.biometrics.BiometricManager frameworkBiometricManager =
                mock(android.hardware.biometrics.BiometricManager.class);
        when(frameworkBiometricManager.canAuthenticate()).thenReturn(BIOMETRIC_ERROR_NONE_ENROLLED);

        final BiometricManager biometricManager = new BiometricManager(
                new TestInjector.Builder(mContext)
                        .setBiometricManager(frameworkBiometricManager)
                        .setDeviceSecurable(true)
                        .setDeviceSecuredWithCredential(true)
                        .build());

        final int authenticators = Authenticators.BIOMETRIC_WEAK | Authenticators.DEVICE_CREDENTIAL;
        assertThat(biometricManager.canAuthenticate(authenticators)).isEqualTo(BIOMETRIC_SUCCESS);
    }

    @Test
    @Config(maxSdk = Build.VERSION_CODES.P)
    public void testCanAuthenticate_ReturnsSuccess_WhenDeviceCredentialAvailable_OnApi28AndBelow() {
        when(mFingerprintManager.isHardwareDetected()).thenReturn(false);
        when(mFingerprintManager.hasEnrolledFingerprints()).thenReturn(false);

        final BiometricManager biometricManager = new BiometricManager(
                new TestInjector.Builder(mContext)
                        .setFingerprintManager(mFingerprintManager)
                        .setDeviceSecurable(true)
                        .setDeviceSecuredWithCredential(true)
                        .build());

        final int authenticators = Authenticators.BIOMETRIC_WEAK | Authenticators.DEVICE_CREDENTIAL;
        assertThat(biometricManager.canAuthenticate(authenticators)).isEqualTo(BIOMETRIC_SUCCESS);
    }

    @Test
    @Config(sdk = Build.VERSION_CODES.Q)
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.Q)
    public void testCanAuthenticateStrong_ReturnsSuccess_WhenStrongBiometricGuaranteed_OnApi29() {
        final android.hardware.biometrics.BiometricManager frameworkBiometricManager =
                mock(android.hardware.biometrics.BiometricManager.class);
        when(frameworkBiometricManager.canAuthenticate()).thenReturn(BIOMETRIC_SUCCESS);
        when(mFingerprintManager.isHardwareDetected()).thenReturn(false);
        when(mFingerprintManager.hasEnrolledFingerprints()).thenReturn(false);

        final BiometricManager biometricManager = new BiometricManager(
                new TestInjector.Builder(mContext)
                        .setBiometricManager(frameworkBiometricManager)
                        .setFingerprintManager(mFingerprintManager)
                        .setDeviceSecurable(true)
                        .setDeviceSecuredWithCredential(true)
                        .setFingerprintHardwarePresent(true)
                        .setStrongBiometricGuaranteed(true)
                        .build());

        final int authenticators = Authenticators.BIOMETRIC_STRONG;
        assertThat(biometricManager.canAuthenticate(authenticators)).isEqualTo(BIOMETRIC_SUCCESS);
    }

    @Test
    @Config(sdk = Build.VERSION_CODES.Q)
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.Q)
    public void testCanAuthenticateStrong_ReturnsError_WhenWeakUnavailable_OnApi29() {
        final android.hardware.biometrics.BiometricManager frameworkBiometricManager =
                mock(android.hardware.biometrics.BiometricManager.class);
        when(frameworkBiometricManager.canAuthenticate()).thenReturn(BIOMETRIC_ERROR_NONE_ENROLLED);
        when(mFingerprintManager.isHardwareDetected()).thenReturn(false);
        when(mFingerprintManager.hasEnrolledFingerprints()).thenReturn(false);

        final BiometricManager biometricManager = new BiometricManager(
                new TestInjector.Builder(mContext)
                        .setBiometricManager(frameworkBiometricManager)
                        .setFingerprintManager(mFingerprintManager)
                        .setDeviceSecurable(true)
                        .setDeviceSecuredWithCredential(true)
                        .setFingerprintHardwarePresent(true)
                        .build());

        final int authenticators = Authenticators.BIOMETRIC_STRONG;
        assertThat(biometricManager.canAuthenticate(authenticators))
                .isEqualTo(BIOMETRIC_ERROR_NONE_ENROLLED);
    }

    @Test
    @Config(sdk = Build.VERSION_CODES.Q)
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.Q)
    public void testCanAuthenticateStrong_ReturnsSuccess_WhenFingerprintAvailable_OnApi29() {
        final android.hardware.biometrics.BiometricManager frameworkBiometricManager =
                mock(android.hardware.biometrics.BiometricManager.class);
        when(frameworkBiometricManager.canAuthenticate()).thenReturn(BIOMETRIC_SUCCESS);
        when(mFingerprintManager.isHardwareDetected()).thenReturn(true);
        when(mFingerprintManager.hasEnrolledFingerprints()).thenReturn(true);

        final BiometricManager biometricManager = new BiometricManager(
                new TestInjector.Builder(mContext)
                        .setBiometricManager(frameworkBiometricManager)
                        .setFingerprintManager(mFingerprintManager)
                        .setDeviceSecurable(true)
                        .setDeviceSecuredWithCredential(true)
                        .setFingerprintHardwarePresent(true)
                        .build());

        final int authenticators = Authenticators.BIOMETRIC_STRONG;
        assertThat(biometricManager.canAuthenticate(authenticators)).isEqualTo(BIOMETRIC_SUCCESS);
    }

    @Test
    @Config(maxSdk = Build.VERSION_CODES.P)
    public void testCanAuthenticate_ReturnsSuccess_WhenFingerprintAvailable_OnApi28AndBelow() {
        when(mFingerprintManager.isHardwareDetected()).thenReturn(true);
        when(mFingerprintManager.hasEnrolledFingerprints()).thenReturn(true);

        final BiometricManager biometricManager = new BiometricManager(
                new TestInjector.Builder(mContext)
                        .setFingerprintManager(mFingerprintManager)
                        .setDeviceSecurable(true)
                        .setDeviceSecuredWithCredential(true)
                        .setFingerprintHardwarePresent(true)
                        .build());

        final int authenticators = Authenticators.BIOMETRIC_STRONG;
        assertThat(biometricManager.canAuthenticate(authenticators)).isEqualTo(BIOMETRIC_SUCCESS);
    }

    @Test
    @Config(sdk = Build.VERSION_CODES.Q)
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.Q)
    public void testCanAuthenticate_ReturnsUnknown_WhenFingerprintUnavailable_OnApi29() {
        final android.hardware.biometrics.BiometricManager frameworkBiometricManager =
                mock(android.hardware.biometrics.BiometricManager.class);
        when(frameworkBiometricManager.canAuthenticate()).thenReturn(BIOMETRIC_SUCCESS);
        when(mFingerprintManager.isHardwareDetected()).thenReturn(false);
        when(mFingerprintManager.hasEnrolledFingerprints()).thenReturn(false);

        final BiometricManager biometricManager = new BiometricManager(
                new TestInjector.Builder(mContext)
                        .setBiometricManager(frameworkBiometricManager)
                        .setFingerprintManager(mFingerprintManager)
                        .setDeviceSecurable(true)
                        .setDeviceSecuredWithCredential(true)
                        .setFingerprintHardwarePresent(true)
                        .build());

        final int authenticators = Authenticators.BIOMETRIC_STRONG;
        assertThat(biometricManager.canAuthenticate(authenticators))
                .isEqualTo(BIOMETRIC_STATUS_UNKNOWN);
    }

    @Test
    @Config(sdk = Build.VERSION_CODES.P)
    public void testCanAuthenticate_ReturnsUnknown_WhenFingerprintUnavailable_OnApi28() {
        when(mFingerprintManager.isHardwareDetected()).thenReturn(false);
        when(mFingerprintManager.hasEnrolledFingerprints()).thenReturn(false);

        final BiometricManager biometricManager = new BiometricManager(
                new TestInjector.Builder(mContext)
                        .setFingerprintManager(mFingerprintManager)
                        .setDeviceSecurable(true)
                        .setDeviceSecuredWithCredential(true)
                        .setFingerprintHardwarePresent(true)
                        .build());

        final int authenticators = Authenticators.BIOMETRIC_STRONG;
        assertThat(biometricManager.canAuthenticate(authenticators))
                .isEqualTo(BIOMETRIC_STATUS_UNKNOWN);
    }

    @Test
    @Config(maxSdk = Build.VERSION_CODES.O_MR1)
    public void testCanAuthenticate_ReturnsError_WhenFingerprintUnavailable_OnApi27AndBelow() {
        when(mFingerprintManager.isHardwareDetected()).thenReturn(true);
        when(mFingerprintManager.hasEnrolledFingerprints()).thenReturn(false);

        final BiometricManager biometricManager = new BiometricManager(
                new TestInjector.Builder(mContext)
                        .setFingerprintManager(mFingerprintManager)
                        .setDeviceSecurable(true)
                        .setDeviceSecuredWithCredential(true)
                        .setFingerprintHardwarePresent(true)
                        .build());

        final int authenticators = Authenticators.BIOMETRIC_STRONG;
        assertThat(biometricManager.canAuthenticate(authenticators))
                .isEqualTo(BIOMETRIC_ERROR_NONE_ENROLLED);
    }

    @Test
    @Config(sdk = Build.VERSION_CODES.P)
    public void testCanAuthenticate_ReturnsError_WhenNoFingerprintHardware_OnApi28() {
        when(mFingerprintManager.isHardwareDetected()).thenReturn(false);
        when(mFingerprintManager.hasEnrolledFingerprints()).thenReturn(false);

        final BiometricManager biometricManager = new BiometricManager(
                new TestInjector.Builder(mContext)
                        .setFingerprintManager(mFingerprintManager)
                        .setDeviceSecurable(true)
                        .setDeviceSecuredWithCredential(true)
                        .build());

        final int authenticators = Authenticators.BIOMETRIC_STRONG;
        assertThat(biometricManager.canAuthenticate(authenticators))
                .isEqualTo(BIOMETRIC_ERROR_NO_HARDWARE);
    }

    @Test
    @Config(maxSdk = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    public void testCanAuthenticate_ReturnsError_WhenIdentityCheckIsNotAvailable_OnApi34AndBelow() {
        when(mFingerprintManager.isHardwareDetected()).thenReturn(true);
        when(mFingerprintManager.hasEnrolledFingerprints()).thenReturn(true);

        final BiometricManager biometricManager = new BiometricManager(
                new TestInjector.Builder(mContext)
                        .setFingerprintManager(mFingerprintManager)
                        .setDeviceSecurable(true)
                        .setDeviceSecuredWithCredential(true)
                        .setFingerprintHardwarePresent(true)
                        .build());

        final int authenticators = Authenticators.IDENTITY_CHECK;
        assertThat(biometricManager.canAuthenticate(authenticators))
                .isEqualTo(BIOMETRIC_ERROR_IDENTITY_CHECK_NOT_ACTIVE);
    }

    @Test
    @Config(minSdk = Build.VERSION_CODES.VANILLA_ICE_CREAM)
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.VANILLA_ICE_CREAM)
    public void testCanAuthenticate_ReturnsError_WhenIdentityCheckIsNotAvailable_OnApi35AndAbove() {
        final android.hardware.biometrics.BiometricManager frameworkBiometricManager =
                mock(android.hardware.biometrics.BiometricManager.class);
        when(frameworkBiometricManager.canAuthenticate()).thenReturn(
                BIOMETRIC_ERROR_IDENTITY_CHECK_NOT_ACTIVE);
        when(mFingerprintManager.isHardwareDetected()).thenReturn(true);
        when(mFingerprintManager.hasEnrolledFingerprints()).thenReturn(true);

        final BiometricManager biometricManager = new BiometricManager(
                new TestInjector.Builder(mContext)
                        .setFingerprintManager(mFingerprintManager)
                        .setDeviceSecurable(true)
                        .setDeviceSecuredWithCredential(true)
                        .setFingerprintHardwarePresent(true)
                        .build());

        final int authenticators = Authenticators.IDENTITY_CHECK;
        assertThat(biometricManager.canAuthenticate(authenticators))
                .isEqualTo(BIOMETRIC_ERROR_IDENTITY_CHECK_NOT_ACTIVE);
    }

    @Test
    @Config(minSdk = Build.VERSION_CODES.VANILLA_ICE_CREAM)
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.VANILLA_ICE_CREAM)
    public void testCanAuthenticate_ReturnsError_WhenIdentityCheckIsNotAvailableWithSecurityException_OnApi35AndAbove() {
        final android.hardware.biometrics.BiometricManager frameworkBiometricManager =
                mock(android.hardware.biometrics.BiometricManager.class);
        when(frameworkBiometricManager.canAuthenticate()).thenThrow(SecurityException.class);

        when(mFingerprintManager.isHardwareDetected()).thenReturn(true);
        when(mFingerprintManager.hasEnrolledFingerprints()).thenReturn(true);

        final BiometricManager biometricManager = new BiometricManager(
                new TestInjector.Builder(mContext)
                        .setFingerprintManager(mFingerprintManager)
                        .setDeviceSecurable(true)
                        .setDeviceSecuredWithCredential(true)
                        .setFingerprintHardwarePresent(true)
                        .build());

        final int authenticators = Authenticators.IDENTITY_CHECK;
        assertThat(biometricManager.canAuthenticate(authenticators))
                .isEqualTo(BIOMETRIC_ERROR_IDENTITY_CHECK_NOT_ACTIVE);
    }

    @Test
    public void testCanAuthenticate_ReturnsSuccess_WhenIdentityCheckIsNotOnly() {
        final int authenticators = Authenticators.IDENTITY_CHECK | Authenticators.BIOMETRIC_WEAK;
        android.hardware.biometrics.BiometricManager frameworkBiometricManager = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            frameworkBiometricManager = mock(android.hardware.biometrics.BiometricManager.class);
            when(frameworkBiometricManager.canAuthenticate()).thenReturn(BIOMETRIC_SUCCESS);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                when(frameworkBiometricManager.canAuthenticate(authenticators)).thenReturn(
                        BIOMETRIC_SUCCESS);
            }
        }

        when(mFingerprintManager.isHardwareDetected()).thenReturn(true);
        when(mFingerprintManager.hasEnrolledFingerprints()).thenReturn(true);

        final BiometricManager biometricManager = new BiometricManager(
                new TestInjector.Builder(mContext)
                        .setBiometricManager(frameworkBiometricManager)
                        .setFingerprintManager(mFingerprintManager)
                        .setDeviceSecurable(true)
                        .setDeviceSecuredWithCredential(true)
                        .setFingerprintHardwarePresent(true)
                        .build());

        assertThat(biometricManager.canAuthenticate(authenticators)).isEqualTo(BIOMETRIC_SUCCESS);
    }

    @Test
    @Config(minSdk = Build.VERSION_CODES.VANILLA_ICE_CREAM)
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.VANILLA_ICE_CREAM)
    public void testCanAuthenticate_ReturnsHardwareError_ForNotEnabledForApps_OnApi35AndAbove() {
        final android.hardware.biometrics.BiometricManager frameworkBiometricManager =
                mock(android.hardware.biometrics.BiometricManager.class);
        when(frameworkBiometricManager.canAuthenticate()).thenReturn(
                BIOMETRIC_ERROR_NOT_ENABLED_FOR_APPS);
        when(mFingerprintManager.isHardwareDetected()).thenReturn(true);
        when(mFingerprintManager.hasEnrolledFingerprints()).thenReturn(true);

        final BiometricManager biometricManager = new BiometricManager(
                new TestInjector.Builder(mContext)
                        .setFingerprintManager(mFingerprintManager)
                        .setDeviceSecurable(true)
                        .setDeviceSecuredWithCredential(true)
                        .setFingerprintHardwarePresent(true)
                        .build());

        final int authenticators = Authenticators.BIOMETRIC_WEAK;
        assertThat(biometricManager.canAuthenticate(authenticators)).isEqualTo(
                BIOMETRIC_ERROR_HW_UNAVAILABLE);
    }


    @Test
    @Config(minSdk = Build.VERSION_CODES.S)
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.S)
    public void testGetStrings_ReturnsFrameworkStrings_OnApi31AndAbove() {
        @AuthenticatorTypes final int authenticators =
                Authenticators.BIOMETRIC_STRONG | Authenticators.DEVICE_CREDENTIAL;
        final String buttonLabel = "buttonLabel";
        final String promptMessage = "promptMessage";
        final String settingName = "settingName";

        final android.hardware.biometrics.BiometricManager frameworkBiometricManager =
                mock(android.hardware.biometrics.BiometricManager.class);
        final android.hardware.biometrics.BiometricManager.Strings frameworkStrings =
                mock(android.hardware.biometrics.BiometricManager.Strings.class);
        when(frameworkBiometricManager.getStrings(authenticators)).thenReturn(frameworkStrings);
        when(frameworkStrings.getButtonLabel()).thenReturn(buttonLabel);
        when(frameworkStrings.getPromptMessage()).thenReturn(promptMessage);
        when(frameworkStrings.getSettingName()).thenReturn(settingName);

        final BiometricManager biometricManager = new BiometricManager(
                new TestInjector.Builder(mContext)
                        .setBiometricManager(frameworkBiometricManager)
                        .build());

        final BiometricManager.Strings strings = biometricManager.getStrings(authenticators);
        assertThat(strings).isNotNull();
        assertThat(strings.getButtonLabel()).isEqualTo(buttonLabel);
        assertThat(strings.getPromptMessage()).isEqualTo(promptMessage);
        assertThat(strings.getSettingName()).isEqualTo(settingName);
    }

    @Test
    @Config(sdk = Build.VERSION_CODES.R)
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.R)
    public void testGetStrings_WhenFingerprintAvailable_OnApi30() {
        final android.hardware.biometrics.BiometricManager frameworkBiometricManager =
                mock(android.hardware.biometrics.BiometricManager.class);
        when(mFingerprintManager.isHardwareDetected()).thenReturn(true);
        when(mFingerprintManager.hasEnrolledFingerprints()).thenReturn(true);
        when(frameworkBiometricManager.canAuthenticate(0)).thenReturn(BIOMETRIC_ERROR_NO_HARDWARE);
        when(frameworkBiometricManager.canAuthenticate(Authenticators.BIOMETRIC_WEAK))
                .thenReturn(BIOMETRIC_SUCCESS);
        when(frameworkBiometricManager.canAuthenticate(Authenticators.DEVICE_CREDENTIAL))
                .thenReturn(BIOMETRIC_SUCCESS);
        when(frameworkBiometricManager.canAuthenticate(
                Authenticators.BIOMETRIC_WEAK | Authenticators.DEVICE_CREDENTIAL))
                .thenReturn(BIOMETRIC_SUCCESS);

        final BiometricManager biometricManager = new BiometricManager(
                new TestInjector.Builder(mContext)
                        .setBiometricManager(frameworkBiometricManager)
                        .setDeviceSecurable(true)
                        .setDeviceSecuredWithCredential(true)
                        .setFingerprintHardwarePresent(true)
                        .setFaceHardwarePresent(false)
                        .setIrisHardwarePresent(false)
                        .build());

        final BiometricManager.Strings biometricStrings =
                biometricManager.getStrings(Authenticators.BIOMETRIC_WEAK);
        assertThat(biometricStrings).isNotNull();
        assertThat(biometricStrings.getButtonLabel())
                .isEqualTo(mContext.getString(R.string.use_fingerprint_label));
        assertThat(biometricStrings.getPromptMessage())
                .isEqualTo(mContext.getString(R.string.fingerprint_prompt_message));
        assertThat(biometricStrings.getSettingName())
                .isEqualTo(mContext.getString(R.string.use_fingerprint_label));

        final BiometricManager.Strings biometricOrCredentialStrings =
                biometricManager.getStrings(
                        Authenticators.BIOMETRIC_WEAK | Authenticators.DEVICE_CREDENTIAL);
        assertThat(biometricOrCredentialStrings).isNotNull();
        assertThat(biometricOrCredentialStrings.getButtonLabel())
                .isEqualTo(mContext.getString(R.string.use_fingerprint_label));
        assertThat(biometricOrCredentialStrings.getPromptMessage())
                .isEqualTo(mContext.getString(R.string.fingerprint_or_screen_lock_prompt_message));
        assertThat(biometricOrCredentialStrings.getSettingName())
                .isEqualTo(mContext.getString(R.string.use_fingerprint_or_screen_lock_label));

        final BiometricManager.Strings credentialStrings =
                biometricManager.getStrings(Authenticators.DEVICE_CREDENTIAL);
        assertThat(credentialStrings).isNotNull();
        assertThat(credentialStrings.getButtonLabel())
                .isEqualTo(mContext.getString(R.string.use_screen_lock_label));
        assertThat(credentialStrings.getPromptMessage())
                .isEqualTo(mContext.getString(R.string.screen_lock_prompt_message));
        assertThat(credentialStrings.getSettingName())
                .isEqualTo(mContext.getString(R.string.use_screen_lock_label));
    }

    @Test
    @Config(sdk = Build.VERSION_CODES.R)
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.R)
    public void testGetStrings_WhenFaceAvailable_OnApi30() {
        final android.hardware.biometrics.BiometricManager frameworkBiometricManager =
                mock(android.hardware.biometrics.BiometricManager.class);
        when(mFingerprintManager.isHardwareDetected()).thenReturn(false);
        when(mFingerprintManager.hasEnrolledFingerprints()).thenReturn(false);
        when(frameworkBiometricManager.canAuthenticate(0)).thenReturn(BIOMETRIC_ERROR_NO_HARDWARE);
        when(frameworkBiometricManager.canAuthenticate(Authenticators.BIOMETRIC_WEAK))
                .thenReturn(BIOMETRIC_SUCCESS);
        when(frameworkBiometricManager.canAuthenticate(Authenticators.DEVICE_CREDENTIAL))
                .thenReturn(BIOMETRIC_SUCCESS);
        when(frameworkBiometricManager.canAuthenticate(
                Authenticators.BIOMETRIC_WEAK | Authenticators.DEVICE_CREDENTIAL))
                .thenReturn(BIOMETRIC_SUCCESS);

        final BiometricManager biometricManager = new BiometricManager(
                new TestInjector.Builder(mContext)
                        .setBiometricManager(frameworkBiometricManager)
                        .setDeviceSecurable(true)
                        .setDeviceSecuredWithCredential(true)
                        .setFingerprintHardwarePresent(false)
                        .setFaceHardwarePresent(true)
                        .setIrisHardwarePresent(false)
                        .build());

        final BiometricManager.Strings biometricStrings =
                biometricManager.getStrings(Authenticators.BIOMETRIC_WEAK);
        assertThat(biometricStrings).isNotNull();
        assertThat(biometricStrings.getButtonLabel())
                .isEqualTo(mContext.getString(R.string.use_face_label));
        assertThat(biometricStrings.getPromptMessage())
                .isEqualTo(mContext.getString(R.string.face_prompt_message));
        assertThat(biometricStrings.getSettingName())
                .isEqualTo(mContext.getString(R.string.use_face_label));

        final BiometricManager.Strings biometricOrCredentialStrings =
                biometricManager.getStrings(
                        Authenticators.BIOMETRIC_WEAK | Authenticators.DEVICE_CREDENTIAL);
        assertThat(biometricOrCredentialStrings).isNotNull();
        assertThat(biometricOrCredentialStrings.getButtonLabel())
                .isEqualTo(mContext.getString(R.string.use_face_label));
        assertThat(biometricOrCredentialStrings.getPromptMessage())
                .isEqualTo(mContext.getString(R.string.face_or_screen_lock_prompt_message));
        assertThat(biometricOrCredentialStrings.getSettingName())
                .isEqualTo(mContext.getString(R.string.use_face_or_screen_lock_label));

        final BiometricManager.Strings credentialStrings =
                biometricManager.getStrings(Authenticators.DEVICE_CREDENTIAL);
        assertThat(credentialStrings).isNotNull();
        assertThat(credentialStrings.getButtonLabel())
                .isEqualTo(mContext.getString(R.string.use_screen_lock_label));
        assertThat(credentialStrings.getPromptMessage())
                .isEqualTo(mContext.getString(R.string.screen_lock_prompt_message));
        assertThat(credentialStrings.getSettingName())
                .isEqualTo(mContext.getString(R.string.use_screen_lock_label));
    }

    @Test
    @Config(sdk = Build.VERSION_CODES.R)
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.R)
    public void testGetStrings_WhenIrisAvailable_OnApi30() {
        final android.hardware.biometrics.BiometricManager frameworkBiometricManager =
                mock(android.hardware.biometrics.BiometricManager.class);
        when(mFingerprintManager.isHardwareDetected()).thenReturn(false);
        when(mFingerprintManager.hasEnrolledFingerprints()).thenReturn(false);
        when(frameworkBiometricManager.canAuthenticate(0)).thenReturn(BIOMETRIC_ERROR_NO_HARDWARE);
        when(frameworkBiometricManager.canAuthenticate(Authenticators.BIOMETRIC_WEAK))
                .thenReturn(BIOMETRIC_SUCCESS);
        when(frameworkBiometricManager.canAuthenticate(Authenticators.DEVICE_CREDENTIAL))
                .thenReturn(BIOMETRIC_SUCCESS);
        when(frameworkBiometricManager.canAuthenticate(
                Authenticators.BIOMETRIC_WEAK | Authenticators.DEVICE_CREDENTIAL))
                .thenReturn(BIOMETRIC_SUCCESS);

        final BiometricManager biometricManager = new BiometricManager(
                new TestInjector.Builder(mContext)
                        .setBiometricManager(frameworkBiometricManager)
                        .setDeviceSecurable(true)
                        .setDeviceSecuredWithCredential(true)
                        .setFingerprintHardwarePresent(false)
                        .setFaceHardwarePresent(false)
                        .setIrisHardwarePresent(true)
                        .build());

        final BiometricManager.Strings biometricStrings =
                biometricManager.getStrings(Authenticators.BIOMETRIC_WEAK);
        assertThat(biometricStrings).isNotNull();
        assertThat(biometricStrings.getButtonLabel())
                .isEqualTo(mContext.getString(R.string.use_biometric_label));
        assertThat(biometricStrings.getPromptMessage())
                .isEqualTo(mContext.getString(R.string.biometric_prompt_message));
        assertThat(biometricStrings.getSettingName())
                .isEqualTo(mContext.getString(R.string.use_biometric_label));

        final BiometricManager.Strings biometricOrCredentialStrings =
                biometricManager.getStrings(
                        Authenticators.BIOMETRIC_WEAK | Authenticators.DEVICE_CREDENTIAL);
        assertThat(biometricOrCredentialStrings).isNotNull();
        assertThat(biometricOrCredentialStrings.getButtonLabel())
                .isEqualTo(mContext.getString(R.string.use_biometric_label));
        assertThat(biometricOrCredentialStrings.getPromptMessage())
                .isEqualTo(mContext.getString(R.string.biometric_or_screen_lock_prompt_message));
        assertThat(biometricOrCredentialStrings.getSettingName())
                .isEqualTo(mContext.getString(R.string.use_biometric_or_screen_lock_label));

        final BiometricManager.Strings credentialStrings =
                biometricManager.getStrings(Authenticators.DEVICE_CREDENTIAL);
        assertThat(credentialStrings).isNotNull();
        assertThat(credentialStrings.getButtonLabel())
                .isEqualTo(mContext.getString(R.string.use_screen_lock_label));
        assertThat(credentialStrings.getPromptMessage())
                .isEqualTo(mContext.getString(R.string.screen_lock_prompt_message));
        assertThat(credentialStrings.getSettingName())
                .isEqualTo(mContext.getString(R.string.use_screen_lock_label));
    }

    @Test
    @Config(sdk = Build.VERSION_CODES.R)
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.R)
    public void testGetStrings_WhenFingerprintAndFaceAvailable_OnApi30() {
        final android.hardware.biometrics.BiometricManager frameworkBiometricManager =
                mock(android.hardware.biometrics.BiometricManager.class);
        when(mFingerprintManager.isHardwareDetected()).thenReturn(true);
        when(mFingerprintManager.hasEnrolledFingerprints()).thenReturn(true);
        when(frameworkBiometricManager.canAuthenticate(0)).thenReturn(BIOMETRIC_ERROR_NO_HARDWARE);
        when(frameworkBiometricManager.canAuthenticate(Authenticators.BIOMETRIC_WEAK))
                .thenReturn(BIOMETRIC_SUCCESS);
        when(frameworkBiometricManager.canAuthenticate(Authenticators.DEVICE_CREDENTIAL))
                .thenReturn(BIOMETRIC_SUCCESS);
        when(frameworkBiometricManager.canAuthenticate(
                Authenticators.BIOMETRIC_STRONG | Authenticators.DEVICE_CREDENTIAL))
                .thenReturn(BIOMETRIC_SUCCESS);

        final BiometricManager biometricManager = new BiometricManager(
                new TestInjector.Builder(mContext)
                        .setBiometricManager(frameworkBiometricManager)
                        .setDeviceSecurable(true)
                        .setDeviceSecuredWithCredential(true)
                        .setFingerprintHardwarePresent(true)
                        .setFaceHardwarePresent(true)
                        .setIrisHardwarePresent(false)
                        .build());

        final BiometricManager.Strings biometricStrings =
                biometricManager.getStrings(Authenticators.BIOMETRIC_WEAK);
        assertThat(biometricStrings).isNotNull();
        assertThat(biometricStrings.getButtonLabel())
                .isEqualTo(mContext.getString(R.string.use_biometric_label));
        assertThat(biometricStrings.getPromptMessage())
                .isEqualTo(mContext.getString(R.string.biometric_prompt_message));
        assertThat(biometricStrings.getSettingName())
                .isEqualTo(mContext.getString(R.string.use_biometric_label));

        final BiometricManager.Strings biometricOrCredentialStrings =
                biometricManager.getStrings(
                        Authenticators.BIOMETRIC_WEAK | Authenticators.DEVICE_CREDENTIAL);
        assertThat(biometricOrCredentialStrings).isNotNull();
        assertThat(biometricOrCredentialStrings.getButtonLabel())
                .isEqualTo(mContext.getString(R.string.use_biometric_label));
        assertThat(biometricOrCredentialStrings.getPromptMessage())
                .isEqualTo(mContext.getString(R.string.biometric_or_screen_lock_prompt_message));
        assertThat(biometricOrCredentialStrings.getSettingName())
                .isEqualTo(mContext.getString(R.string.use_biometric_or_screen_lock_label));

        final BiometricManager.Strings credentialStrings =
                biometricManager.getStrings(Authenticators.DEVICE_CREDENTIAL);
        assertThat(credentialStrings).isNotNull();
        assertThat(credentialStrings.getButtonLabel())
                .isEqualTo(mContext.getString(R.string.use_screen_lock_label));
        assertThat(credentialStrings.getPromptMessage())
                .isEqualTo(mContext.getString(R.string.screen_lock_prompt_message));
        assertThat(credentialStrings.getSettingName())
                .isEqualTo(mContext.getString(R.string.use_screen_lock_label));
    }

    @Test
    @Config(sdk = Build.VERSION_CODES.R)
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.R)
    public void testGetStrings_WhenBiometricsPresentButNotAvailable_OnApi30() {
        final android.hardware.biometrics.BiometricManager frameworkBiometricManager =
                mock(android.hardware.biometrics.BiometricManager.class);
        when(mFingerprintManager.isHardwareDetected()).thenReturn(true);
        when(mFingerprintManager.hasEnrolledFingerprints()).thenReturn(false);
        when(frameworkBiometricManager.canAuthenticate(0)).thenReturn(BIOMETRIC_ERROR_NO_HARDWARE);
        when(frameworkBiometricManager.canAuthenticate(Authenticators.BIOMETRIC_WEAK))
                .thenReturn(BIOMETRIC_ERROR_NONE_ENROLLED);
        when(frameworkBiometricManager.canAuthenticate(Authenticators.DEVICE_CREDENTIAL))
                .thenReturn(BIOMETRIC_SUCCESS);
        when(frameworkBiometricManager.canAuthenticate(
                Authenticators.BIOMETRIC_WEAK | Authenticators.DEVICE_CREDENTIAL))
                .thenReturn(BIOMETRIC_SUCCESS);

        final BiometricManager biometricManager = new BiometricManager(
                new TestInjector.Builder(mContext)
                        .setBiometricManager(frameworkBiometricManager)
                        .setDeviceSecurable(true)
                        .setDeviceSecuredWithCredential(true)
                        .setFingerprintHardwarePresent(true)
                        .setFaceHardwarePresent(true)
                        .setIrisHardwarePresent(true)
                        .build());

        final BiometricManager.Strings biometricOrCredentialStrings =
                biometricManager.getStrings(
                        Authenticators.BIOMETRIC_WEAK | Authenticators.DEVICE_CREDENTIAL);
        assertThat(biometricOrCredentialStrings).isNotNull();
        assertThat(biometricOrCredentialStrings.getButtonLabel())
                .isEqualTo(mContext.getString(R.string.use_screen_lock_label));
        assertThat(biometricOrCredentialStrings.getPromptMessage())
                .isEqualTo(mContext.getString(R.string.screen_lock_prompt_message));
        assertThat(biometricOrCredentialStrings.getSettingName())
                .isEqualTo(mContext.getString(R.string.use_biometric_or_screen_lock_label));

        final BiometricManager.Strings credentialStrings =
                biometricManager.getStrings(Authenticators.DEVICE_CREDENTIAL);
        assertThat(credentialStrings).isNotNull();
        assertThat(credentialStrings.getButtonLabel())
                .isEqualTo(mContext.getString(R.string.use_screen_lock_label));
        assertThat(credentialStrings.getPromptMessage())
                .isEqualTo(mContext.getString(R.string.screen_lock_prompt_message));
        assertThat(credentialStrings.getSettingName())
                .isEqualTo(mContext.getString(R.string.use_screen_lock_label));
    }

    @Test
    @Config(sdk = Build.VERSION_CODES.R)
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.R)
    public void testGetStrings_WhenOnlyScreenLockAvailable_OnApi30() {
        final android.hardware.biometrics.BiometricManager frameworkBiometricManager =
                mock(android.hardware.biometrics.BiometricManager.class);
        when(mFingerprintManager.isHardwareDetected()).thenReturn(false);
        when(mFingerprintManager.hasEnrolledFingerprints()).thenReturn(false);
        when(frameworkBiometricManager.canAuthenticate(0)).thenReturn(BIOMETRIC_ERROR_NO_HARDWARE);
        when(frameworkBiometricManager.canAuthenticate(Authenticators.BIOMETRIC_WEAK))
                .thenReturn(BIOMETRIC_ERROR_NO_HARDWARE);
        when(frameworkBiometricManager.canAuthenticate(Authenticators.DEVICE_CREDENTIAL))
                .thenReturn(BIOMETRIC_SUCCESS);
        when(frameworkBiometricManager.canAuthenticate(
                Authenticators.BIOMETRIC_WEAK | Authenticators.DEVICE_CREDENTIAL))
                .thenReturn(BIOMETRIC_SUCCESS);

        final BiometricManager biometricManager = new BiometricManager(
                new TestInjector.Builder(mContext)
                        .setBiometricManager(frameworkBiometricManager)
                        .setDeviceSecurable(true)
                        .setDeviceSecuredWithCredential(true)
                        .setFingerprintHardwarePresent(false)
                        .setFaceHardwarePresent(false)
                        .setIrisHardwarePresent(false)
                        .build());

        final BiometricManager.Strings biometricOrCredentialStrings =
                biometricManager.getStrings(
                        Authenticators.BIOMETRIC_WEAK | Authenticators.DEVICE_CREDENTIAL);
        assertThat(biometricOrCredentialStrings).isNotNull();
        assertThat(biometricOrCredentialStrings.getButtonLabel())
                .isEqualTo(mContext.getString(R.string.use_screen_lock_label));
        assertThat(biometricOrCredentialStrings.getPromptMessage())
                .isEqualTo(mContext.getString(R.string.screen_lock_prompt_message));
        assertThat(biometricOrCredentialStrings.getSettingName())
                .isEqualTo(mContext.getString(R.string.use_screen_lock_label));

        final BiometricManager.Strings credentialStrings =
                biometricManager.getStrings(Authenticators.DEVICE_CREDENTIAL);
        assertThat(credentialStrings).isNotNull();
        assertThat(credentialStrings.getButtonLabel())
                .isEqualTo(mContext.getString(R.string.use_screen_lock_label));
        assertThat(credentialStrings.getPromptMessage())
                .isEqualTo(mContext.getString(R.string.screen_lock_prompt_message));
        assertThat(credentialStrings.getSettingName())
                .isEqualTo(mContext.getString(R.string.use_screen_lock_label));
    }

    @Test
    @Config(sdk = Build.VERSION_CODES.Q)
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.Q)
    public void testGetStrings_WhenFingerprintAvailable_OnApi29() {
        final android.hardware.biometrics.BiometricManager frameworkBiometricManager =
                mock(android.hardware.biometrics.BiometricManager.class);
        when(mFingerprintManager.isHardwareDetected()).thenReturn(true);
        when(mFingerprintManager.hasEnrolledFingerprints()).thenReturn(true);
        when(frameworkBiometricManager.canAuthenticate()).thenReturn(BIOMETRIC_SUCCESS);

        final BiometricManager biometricManager = new BiometricManager(
                new TestInjector.Builder(mContext)
                        .setBiometricManager(frameworkBiometricManager)
                        .setFingerprintManager(mFingerprintManager)
                        .setDeviceSecurable(true)
                        .setDeviceSecuredWithCredential(true)
                        .setFingerprintHardwarePresent(true)
                        .build());

        final BiometricManager.Strings biometricStrings =
                biometricManager.getStrings(Authenticators.BIOMETRIC_WEAK);
        assertThat(biometricStrings).isNotNull();
        assertThat(biometricStrings.getButtonLabel())
                .isEqualTo(mContext.getString(R.string.use_fingerprint_label));
        assertThat(biometricStrings.getPromptMessage())
                .isEqualTo(mContext.getString(R.string.fingerprint_prompt_message));
        assertThat(biometricStrings.getSettingName())
                .isEqualTo(mContext.getString(R.string.use_fingerprint_label));

        final BiometricManager.Strings biometricOrCredentialStrings =
                biometricManager.getStrings(
                        Authenticators.BIOMETRIC_WEAK | Authenticators.DEVICE_CREDENTIAL);
        assertThat(biometricOrCredentialStrings).isNotNull();
        assertThat(biometricOrCredentialStrings.getButtonLabel())
                .isEqualTo(mContext.getString(R.string.use_fingerprint_label));
        assertThat(biometricOrCredentialStrings.getPromptMessage())
                .isEqualTo(mContext.getString(R.string.fingerprint_or_screen_lock_prompt_message));
        assertThat(biometricOrCredentialStrings.getSettingName())
                .isEqualTo(mContext.getString(R.string.use_fingerprint_or_screen_lock_label));
    }

    @Test
    @Config(sdk = Build.VERSION_CODES.Q)
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.Q)
    public void testGetStrings_WhenFaceAvailable_OnApi29() {
        final android.hardware.biometrics.BiometricManager frameworkBiometricManager =
                mock(android.hardware.biometrics.BiometricManager.class);
        when(mFingerprintManager.isHardwareDetected()).thenReturn(false);
        when(mFingerprintManager.hasEnrolledFingerprints()).thenReturn(false);
        when(frameworkBiometricManager.canAuthenticate()).thenReturn(BIOMETRIC_SUCCESS);

        final BiometricManager biometricManager = new BiometricManager(
                new TestInjector.Builder(mContext)
                        .setBiometricManager(frameworkBiometricManager)
                        .setFingerprintManager(mFingerprintManager)
                        .setDeviceSecurable(true)
                        .setDeviceSecuredWithCredential(true)
                        .setFingerprintHardwarePresent(false)
                        .setFaceHardwarePresent(true)
                        .setIrisHardwarePresent(false)
                        .build());

        final BiometricManager.Strings biometricStrings =
                biometricManager.getStrings(Authenticators.BIOMETRIC_WEAK);
        assertThat(biometricStrings).isNotNull();
        assertThat(biometricStrings.getButtonLabel())
                .isEqualTo(mContext.getString(R.string.use_face_label));
        assertThat(biometricStrings.getPromptMessage())
                .isEqualTo(mContext.getString(R.string.face_prompt_message));
        assertThat(biometricStrings.getSettingName())
                .isEqualTo(mContext.getString(R.string.use_face_label));

        final BiometricManager.Strings biometricOrCredentialStrings =
                biometricManager.getStrings(
                        Authenticators.BIOMETRIC_WEAK | Authenticators.DEVICE_CREDENTIAL);
        assertThat(biometricOrCredentialStrings).isNotNull();
        assertThat(biometricOrCredentialStrings.getButtonLabel())
                .isEqualTo(mContext.getString(R.string.use_face_label));
        assertThat(biometricOrCredentialStrings.getPromptMessage())
                .isEqualTo(mContext.getString(R.string.face_or_screen_lock_prompt_message));
        assertThat(biometricOrCredentialStrings.getSettingName())
                .isEqualTo(mContext.getString(R.string.use_face_or_screen_lock_label));
    }

    @Test
    @Config(sdk = Build.VERSION_CODES.Q)
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.Q)
    public void testGetStrings_WhenIrisAvailable_OnApi29() {
        final android.hardware.biometrics.BiometricManager frameworkBiometricManager =
                mock(android.hardware.biometrics.BiometricManager.class);
        when(mFingerprintManager.isHardwareDetected()).thenReturn(false);
        when(mFingerprintManager.hasEnrolledFingerprints()).thenReturn(false);
        when(frameworkBiometricManager.canAuthenticate()).thenReturn(BIOMETRIC_SUCCESS);

        final BiometricManager biometricManager = new BiometricManager(
                new TestInjector.Builder(mContext)
                        .setBiometricManager(frameworkBiometricManager)
                        .setFingerprintManager(mFingerprintManager)
                        .setDeviceSecurable(true)
                        .setDeviceSecuredWithCredential(true)
                        .setFingerprintHardwarePresent(false)
                        .setFaceHardwarePresent(false)
                        .setIrisHardwarePresent(true)
                        .build());

        final BiometricManager.Strings biometricStrings =
                biometricManager.getStrings(Authenticators.BIOMETRIC_WEAK);
        assertThat(biometricStrings).isNotNull();
        assertThat(biometricStrings.getButtonLabel())
                .isEqualTo(mContext.getString(R.string.use_biometric_label));
        assertThat(biometricStrings.getPromptMessage())
                .isEqualTo(mContext.getString(R.string.biometric_prompt_message));
        assertThat(biometricStrings.getSettingName())
                .isEqualTo(mContext.getString(R.string.use_biometric_label));

        final BiometricManager.Strings biometricOrCredentialStrings =
                biometricManager.getStrings(
                        Authenticators.BIOMETRIC_WEAK | Authenticators.DEVICE_CREDENTIAL);
        assertThat(biometricOrCredentialStrings).isNotNull();
        assertThat(biometricOrCredentialStrings.getButtonLabel())
                .isEqualTo(mContext.getString(R.string.use_biometric_label));
        assertThat(biometricOrCredentialStrings.getPromptMessage())
                .isEqualTo(mContext.getString(R.string.biometric_or_screen_lock_prompt_message));
        assertThat(biometricOrCredentialStrings.getSettingName())
                .isEqualTo(mContext.getString(R.string.use_biometric_or_screen_lock_label));
    }

    @Test
    @Config(sdk = Build.VERSION_CODES.Q)
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.Q)
    public void testGetStrings_WhenFingerprintAndFaceAvailable_OnApi29() {
        final android.hardware.biometrics.BiometricManager frameworkBiometricManager =
                mock(android.hardware.biometrics.BiometricManager.class);
        when(mFingerprintManager.isHardwareDetected()).thenReturn(true);
        when(mFingerprintManager.hasEnrolledFingerprints()).thenReturn(true);
        when(frameworkBiometricManager.canAuthenticate()).thenReturn(BIOMETRIC_SUCCESS);

        final BiometricManager biometricManager = new BiometricManager(
                new TestInjector.Builder(mContext)
                        .setBiometricManager(frameworkBiometricManager)
                        .setFingerprintManager(mFingerprintManager)
                        .setDeviceSecurable(true)
                        .setDeviceSecuredWithCredential(true)
                        .setFingerprintHardwarePresent(true)
                        .setFaceHardwarePresent(true)
                        .setIrisHardwarePresent(false)
                        .build());

        final BiometricManager.Strings biometricStrings =
                biometricManager.getStrings(Authenticators.BIOMETRIC_WEAK);
        assertThat(biometricStrings).isNotNull();
        assertThat(biometricStrings.getButtonLabel())
                .isEqualTo(mContext.getString(R.string.use_biometric_label));
        assertThat(biometricStrings.getPromptMessage())
                .isEqualTo(mContext.getString(R.string.biometric_prompt_message));
        assertThat(biometricStrings.getSettingName())
                .isEqualTo(mContext.getString(R.string.use_biometric_label));

        final BiometricManager.Strings biometricOrCredentialStrings =
                biometricManager.getStrings(
                        Authenticators.BIOMETRIC_WEAK | Authenticators.DEVICE_CREDENTIAL);
        assertThat(biometricOrCredentialStrings).isNotNull();
        assertThat(biometricOrCredentialStrings.getButtonLabel())
                .isEqualTo(mContext.getString(R.string.use_biometric_label));
        assertThat(biometricOrCredentialStrings.getPromptMessage())
                .isEqualTo(mContext.getString(R.string.biometric_or_screen_lock_prompt_message));
        assertThat(biometricOrCredentialStrings.getSettingName())
                .isEqualTo(mContext.getString(R.string.use_biometric_or_screen_lock_label));
    }

    @Test
    @Config(sdk = Build.VERSION_CODES.Q)
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.Q)
    public void testGetStrings_WhenBiometricsPresentButNotAvailable_OnApi29() {
        final android.hardware.biometrics.BiometricManager frameworkBiometricManager =
                mock(android.hardware.biometrics.BiometricManager.class);
        when(mFingerprintManager.isHardwareDetected()).thenReturn(true);
        when(mFingerprintManager.hasEnrolledFingerprints()).thenReturn(false);
        when(frameworkBiometricManager.canAuthenticate()).thenReturn(BIOMETRIC_ERROR_NONE_ENROLLED);

        final BiometricManager biometricManager = new BiometricManager(
                new TestInjector.Builder(mContext)
                        .setBiometricManager(frameworkBiometricManager)
                        .setFingerprintManager(mFingerprintManager)
                        .setDeviceSecurable(true)
                        .setDeviceSecuredWithCredential(true)
                        .setFingerprintHardwarePresent(true)
                        .setFaceHardwarePresent(true)
                        .setIrisHardwarePresent(true)
                        .build());

        final BiometricManager.Strings biometricOrCredentialStrings =
                biometricManager.getStrings(
                        Authenticators.BIOMETRIC_WEAK | Authenticators.DEVICE_CREDENTIAL);
        assertThat(biometricOrCredentialStrings).isNotNull();
        assertThat(biometricOrCredentialStrings.getButtonLabel())
                .isEqualTo(mContext.getString(R.string.use_screen_lock_label));
        assertThat(biometricOrCredentialStrings.getPromptMessage())
                .isEqualTo(mContext.getString(R.string.screen_lock_prompt_message));
        assertThat(biometricOrCredentialStrings.getSettingName())
                .isEqualTo(mContext.getString(R.string.use_biometric_or_screen_lock_label));
    }

    @Test
    @Config(sdk = Build.VERSION_CODES.Q)
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.Q)
    public void testGetStrings_WhenOnlyScreenLockAvailable_OnApi29() {
        final android.hardware.biometrics.BiometricManager frameworkBiometricManager =
                mock(android.hardware.biometrics.BiometricManager.class);
        when(mFingerprintManager.isHardwareDetected()).thenReturn(false);
        when(mFingerprintManager.hasEnrolledFingerprints()).thenReturn(false);
        when(frameworkBiometricManager.canAuthenticate()).thenReturn(BIOMETRIC_ERROR_NO_HARDWARE);

        final BiometricManager biometricManager = new BiometricManager(
                new TestInjector.Builder(mContext)
                        .setBiometricManager(frameworkBiometricManager)
                        .setFingerprintManager(mFingerprintManager)
                        .setDeviceSecurable(true)
                        .setDeviceSecuredWithCredential(true)
                        .setFingerprintHardwarePresent(false)
                        .setFaceHardwarePresent(false)
                        .setIrisHardwarePresent(false)
                        .build());

        final BiometricManager.Strings biometricOrCredentialStrings =
                biometricManager.getStrings(
                        Authenticators.BIOMETRIC_WEAK | Authenticators.DEVICE_CREDENTIAL);
        assertThat(biometricOrCredentialStrings).isNotNull();
        assertThat(biometricOrCredentialStrings.getButtonLabel())
                .isEqualTo(mContext.getString(R.string.use_screen_lock_label));
        assertThat(biometricOrCredentialStrings.getPromptMessage())
                .isEqualTo(mContext.getString(R.string.screen_lock_prompt_message));
        assertThat(biometricOrCredentialStrings.getSettingName())
                .isEqualTo(mContext.getString(R.string.use_screen_lock_label));
    }

    @Test
    @Config(maxSdk = Build.VERSION_CODES.P)
    public void testGetStrings_WhenFingerprintAvailable_OnApi28AndBelow() {
        when(mFingerprintManager.isHardwareDetected()).thenReturn(true);
        when(mFingerprintManager.hasEnrolledFingerprints()).thenReturn(true);

        final BiometricManager biometricManager = new BiometricManager(
                new TestInjector.Builder(mContext)
                        .setFingerprintManager(mFingerprintManager)
                        .setDeviceSecurable(true)
                        .setDeviceSecuredWithCredential(true)
                        .setFingerprintHardwarePresent(true)
                        .build());

        final BiometricManager.Strings biometricStrings =
                biometricManager.getStrings(Authenticators.BIOMETRIC_WEAK);
        assertThat(biometricStrings).isNotNull();
        assertThat(biometricStrings.getButtonLabel())
                .isEqualTo(mContext.getString(R.string.use_fingerprint_label));
        assertThat(biometricStrings.getPromptMessage())
                .isEqualTo(mContext.getString(R.string.fingerprint_prompt_message));
        assertThat(biometricStrings.getSettingName())
                .isEqualTo(mContext.getString(R.string.use_fingerprint_label));

        final BiometricManager.Strings biometricOrCredentialStrings =
                biometricManager.getStrings(
                        Authenticators.BIOMETRIC_WEAK | Authenticators.DEVICE_CREDENTIAL);
        assertThat(biometricOrCredentialStrings).isNotNull();
        assertThat(biometricOrCredentialStrings.getButtonLabel())
                .isEqualTo(mContext.getString(R.string.use_fingerprint_label));
        assertThat(biometricOrCredentialStrings.getPromptMessage())
                .isEqualTo(mContext.getString(R.string.fingerprint_or_screen_lock_prompt_message));
        assertThat(biometricOrCredentialStrings.getSettingName())
                .isEqualTo(mContext.getString(R.string.use_fingerprint_or_screen_lock_label));
    }

    @Test
    @Config(maxSdk = Build.VERSION_CODES.P)
    public void testGetStrings_WhenFingerprintPresentButNotAvailable_OnApi28AndBelow() {
        when(mFingerprintManager.isHardwareDetected()).thenReturn(true);
        when(mFingerprintManager.hasEnrolledFingerprints()).thenReturn(false);

        final BiometricManager biometricManager = new BiometricManager(
                new TestInjector.Builder(mContext)
                        .setFingerprintManager(mFingerprintManager)
                        .setDeviceSecurable(true)
                        .setDeviceSecuredWithCredential(true)
                        .setFingerprintHardwarePresent(true)
                        .build());

        final BiometricManager.Strings biometricOrCredentialStrings =
                biometricManager.getStrings(
                        Authenticators.BIOMETRIC_WEAK | Authenticators.DEVICE_CREDENTIAL);
        assertThat(biometricOrCredentialStrings).isNotNull();
        assertThat(biometricOrCredentialStrings.getButtonLabel())
                .isEqualTo(mContext.getString(R.string.use_screen_lock_label));
        assertThat(biometricOrCredentialStrings.getPromptMessage())
                .isEqualTo(mContext.getString(R.string.screen_lock_prompt_message));
        assertThat(biometricOrCredentialStrings.getSettingName())
                .isEqualTo(mContext.getString(R.string.use_fingerprint_or_screen_lock_label));
    }

    @Test
    @Config(maxSdk = Build.VERSION_CODES.P)
    public void testGetStrings_WhenOnlyScreenLockAvailable_OnApi28AndBelow() {
        when(mFingerprintManager.isHardwareDetected()).thenReturn(false);
        when(mFingerprintManager.hasEnrolledFingerprints()).thenReturn(false);

        final BiometricManager biometricManager = new BiometricManager(
                new TestInjector.Builder(mContext)
                        .setFingerprintManager(mFingerprintManager)
                        .setDeviceSecurable(true)
                        .setDeviceSecuredWithCredential(true)
                        .setFingerprintHardwarePresent(false)
                        .build());

        final BiometricManager.Strings biometricOrCredentialStrings =
                biometricManager.getStrings(
                        Authenticators.BIOMETRIC_WEAK | Authenticators.DEVICE_CREDENTIAL);
        assertThat(biometricOrCredentialStrings).isNotNull();
        assertThat(biometricOrCredentialStrings.getButtonLabel())
                .isEqualTo(mContext.getString(R.string.use_screen_lock_label));
        assertThat(biometricOrCredentialStrings.getPromptMessage())
                .isEqualTo(mContext.getString(R.string.screen_lock_prompt_message));
        assertThat(biometricOrCredentialStrings.getSettingName())
                .isEqualTo(mContext.getString(R.string.use_screen_lock_label));
    }

    /**
     * A configurable injector to be used for testing.
     */
    private static class TestInjector implements BiometricManager.Injector {
        private final @NonNull Context mContext;
        private final android.hardware.biometrics.@Nullable BiometricManager mBiometricManager;
        private final androidx.core.hardware.fingerprint.@Nullable FingerprintManagerCompat
                mFingerprintManager;
        private final boolean mIsDeviceSecurable;
        private final boolean mIsDeviceSecuredWithCredential;
        private final boolean mIsFingerprintHardwarePresent;
        private final boolean mIsFaceHardwarePresent;
        private final boolean mIsIrisHardwarePresent;
        private final boolean mIsStrongBiometricGuaranteed;

        private TestInjector(
                @NonNull Context context,
                android.hardware.biometrics.@Nullable BiometricManager biometricManager,
                androidx.core.hardware.fingerprint.@Nullable FingerprintManagerCompat
                        fingerprintManager,
                boolean isDeviceSecurable,
                boolean isDeviceSecuredWithCredential,
                boolean isFingerprintHardwarePresent,
                boolean isFaceHardwarePresent,
                boolean isIrisHardwarePresent,
                boolean isStrongBiometricGuaranteed) {
            mContext = context;
            mBiometricManager = biometricManager;
            mFingerprintManager = fingerprintManager;
            mIsDeviceSecurable = isDeviceSecurable;
            mIsDeviceSecuredWithCredential = isDeviceSecuredWithCredential;
            mIsFingerprintHardwarePresent = isFingerprintHardwarePresent;
            mIsFaceHardwarePresent = isFaceHardwarePresent;
            mIsIrisHardwarePresent = isIrisHardwarePresent;
            mIsStrongBiometricGuaranteed = isStrongBiometricGuaranteed;
        }

        @Override
        public @NonNull Resources getResources() {
            return mContext.getResources();
        }

        @Override
        public android.hardware.biometrics.@Nullable BiometricManager getBiometricManager() {
            return mBiometricManager;
        }

        @Override
        public androidx.core.hardware.fingerprint.@Nullable FingerprintManagerCompat
                getFingerprintManager() {
            return mFingerprintManager;
        }

        @Override
        public boolean isDeviceSecurable() {
            return mIsDeviceSecurable;
        }

        @Override
        public boolean isDeviceSecuredWithCredential() {
            return mIsDeviceSecuredWithCredential;
        }

        @Override
        public boolean isFingerprintHardwarePresent() {
            return mIsFingerprintHardwarePresent;
        }

        @Override
        public boolean isFaceHardwarePresent() {
            return mIsFaceHardwarePresent;
        }

        @Override
        public boolean isIrisHardwarePresent() {
            return mIsIrisHardwarePresent;
        }

        @Override
        public boolean isStrongBiometricGuaranteed() {
            return mIsStrongBiometricGuaranteed;
        }

        static final class Builder {
            private final @NonNull Context mContext;

            private android.hardware.biometrics.@Nullable BiometricManager mBiometricManager = null;
            private androidx.core.hardware.fingerprint.@Nullable FingerprintManagerCompat
                    mFingerprintManager = null;
            private boolean mIsDeviceSecurable = false;
            private boolean mIsDeviceSecuredWithCredential = false;
            private boolean mIsFingerprintHardwarePresent = false;
            private boolean mIsFaceHardwarePresent = false;
            private boolean mIsIrisHardwarePresent = false;
            private boolean mIsStrongBiometricGuaranteed = false;

            Builder(@NonNull Context context) {
                mContext = context;
            }

            Builder setBiometricManager(
                    android.hardware.biometrics.@Nullable BiometricManager biometricManager) {
                mBiometricManager = biometricManager;
                return this;
            }

            Builder setFingerprintManager(
                    androidx.core.hardware.fingerprint.@Nullable FingerprintManagerCompat
                            fingerprintManager) {
                mFingerprintManager = fingerprintManager;
                return this;
            }

            Builder setDeviceSecurable(boolean deviceSecurable) {
                mIsDeviceSecurable = deviceSecurable;
                return this;
            }

            Builder setDeviceSecuredWithCredential(boolean deviceSecuredWithCredential) {
                mIsDeviceSecuredWithCredential = deviceSecuredWithCredential;
                return this;
            }

            Builder setFingerprintHardwarePresent(boolean fingerprintHardwarePresent) {
                mIsFingerprintHardwarePresent = fingerprintHardwarePresent;
                return this;
            }

            Builder setFaceHardwarePresent(boolean faceHardwarePresent) {
                mIsFaceHardwarePresent = faceHardwarePresent;
                return this;
            }

            Builder setIrisHardwarePresent(boolean irisHardwarePresent) {
                mIsIrisHardwarePresent = irisHardwarePresent;
                return this;
            }

            Builder setStrongBiometricGuaranteed(boolean strongBiometricGuaranteed) {
                mIsStrongBiometricGuaranteed = strongBiometricGuaranteed;
                return this;
            }

            TestInjector build() {
                return new TestInjector(
                        mContext,
                        mBiometricManager,
                        mFingerprintManager,
                        mIsDeviceSecurable,
                        mIsDeviceSecuredWithCredential,
                        mIsFingerprintHardwarePresent,
                        mIsFaceHardwarePresent,
                        mIsIrisHardwarePresent,
                        mIsStrongBiometricGuaranteed);
            }
        }
    }
}
