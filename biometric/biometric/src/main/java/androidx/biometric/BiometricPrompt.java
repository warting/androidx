/*
 * Copyright 2018 The Android Open Source Project
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

import static android.Manifest.permission.SET_BIOMETRIC_DIALOG_ADVANCED;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.os.Build;
import android.security.identity.IdentityCredential;
import android.security.identity.PresentationSession;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.DrawableRes;
import androidx.annotation.IntDef;
import androidx.annotation.RequiresApi;
import androidx.annotation.RequiresPermission;
import androidx.annotation.RestrictTo;
import androidx.annotation.VisibleForTesting;
import androidx.biometric.BiometricManager.Authenticators;
import androidx.biometric.utils.AuthenticatorUtils;
import androidx.biometric.utils.CryptoObjectUtils;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStoreOwner;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.ref.WeakReference;
import java.security.Signature;
import java.util.concurrent.Executor;

import javax.crypto.Cipher;
import javax.crypto.Mac;

/**
 * A class that manages a system-provided biometric prompt. On devices running Android 9.0 (API 28)
 * and above, this will show a system-provided authentication prompt, using one of the device's
 * supported biometric modalities (fingerprint, iris, face, etc) with Class 2 or Class 3 strength.
 * Prior to Android 9.0, this will instead show a custom fingerprint authentication dialog. The
 * prompt will persist across configuration changes unless explicitly canceled. For security
 * reasons, the prompt will be dismissed when the client application is no longer in the foreground.
 *
 * <p>To persist authentication across configuration changes, developers should (re)create the
 * prompt every time the activity/fragment is created. Instantiating the prompt with a new
 * callback early in the fragment/activity lifecycle (e.g. in {@code onCreate()}) will allow the
 * ongoing authentication session's callbacks to be received by the new fragment/activity instance.
 * Note that {@code cancelAuthentication()} should not be called, and {@code authenticate()} does
 * not need to be invoked during activity/fragment creation.
 *
 * <p>Note that if multiple instances of {@code BiometricPrompt} are created within a single
 * Fragment or Activity, only the callback registered with the last created instance will be
 * saved and receive authentication results. This behavior can lead to unexpected results if
 * multiple independent biometric authentication flows are attempted within the same Fragment or
 * Activity. It is highly recommended to avoid creating multiple BiometricPrompt instances in
 * this scenario.
 */
public class BiometricPrompt {
    private static final String TAG = "BiometricPromptCompat";

    /**
     * There is no error, and the user can successfully authenticate.
     */
    static final int BIOMETRIC_SUCCESS = 0;

    /**
     * The hardware is unavailable. Try again later.
     */
    public static final int ERROR_HW_UNAVAILABLE = 1;

    /**
     * The sensor was unable to process the current image.
     */
    public static final int ERROR_UNABLE_TO_PROCESS = 2;

    /**
     * The current operation has been running too long and has timed out.
     *
     * <p>This is intended to prevent programs from waiting for the biometric sensor indefinitely.
     * The timeout is platform and sensor-specific, but is generally on the order of ~30 seconds.
     */
    public static final int ERROR_TIMEOUT = 3;

    /**
     * The operation can't be completed because there is not enough device storage remaining.
     */
    public static final int ERROR_NO_SPACE = 4;

    /**
     * The operation was canceled because the biometric sensor is unavailable. This may happen when
     * the user is switched, the device is locked, or another pending operation prevents it.
     */
    public static final int ERROR_CANCELED = 5;

    /**
     * The operation was canceled because the API is locked out due to too many attempts. This
     * occurs after 5 failed attempts, and lasts for 30 seconds.
     */
    public static final int ERROR_LOCKOUT = 7;

    /**
     * The operation failed due to a vendor-specific error.
     *
     * <p>This error code may be used by hardware vendors to extend this list to cover errors that
     * don't fall under one of the other predefined categories. Vendors are responsible for
     * providing the strings for these errors.
     *
     * <p>These messages are typically reserved for internal operations such as enrollment but may
     * be used to express any error that is not otherwise covered. In this case, applications are
     * expected to show the error message, but they are advised not to rely on the message ID, since
     * this may vary by vendor and device.
     */
    public static final int ERROR_VENDOR = 8;

    /**
     * The operation was canceled because {@link #ERROR_LOCKOUT} occurred too many times. Biometric
     * authentication is disabled until the user unlocks with their device credential (i.e. PIN,
     * pattern, or password).
     */
    public static final int ERROR_LOCKOUT_PERMANENT = 9;

    /**
     * The user canceled the operation.
     *
     * <p>Upon receiving this, applications should use alternate authentication, such as a password.
     * The application should also provide the user a way of returning to biometric authentication,
     * such as a button.
     */
    public static final int ERROR_USER_CANCELED = 10;

    /**
     * The user does not have any biometrics enrolled.
     */
    public static final int ERROR_NO_BIOMETRICS = 11;

    /**
     * The device does not have the required authentication hardware.
     */
    public static final int ERROR_HW_NOT_PRESENT = 12;

    /**
     * The user pressed the negative button.
     */
    public static final int ERROR_NEGATIVE_BUTTON = 13;

    /**
     * The device does not have pin, pattern, or password set up.
     */
    public static final int ERROR_NO_DEVICE_CREDENTIAL = 14;

    /**
     * A security vulnerability has been discovered with one or more hardware sensors. The
     * affected sensor(s) are unavailable until a security update has addressed the issue.
     */
    public static final int ERROR_SECURITY_UPDATE_REQUIRED = 15;

    /**
     * The privacy setting has been enabled and will block use of the sensor.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public static final int ERROR_SENSOR_PRIVACY_ENABLED = 18;

    /**
     * Identity Check is currently not active.
     *
     * This device either doesn't have this feature enabled, or it's not considered in a
     * high-risk environment that requires extra security measures for accessing sensitive data.
     */
    public static final int ERROR_IDENTITY_CHECK_NOT_ACTIVE = 20;

    /**
     * Biometrics is not allowed to verify the user in apps. It's for internal use only. This
     * error code, introduced in API 35, was previously covered by ERROR_HW_UNAVAILABLE and
     * doesn't need to be public. Therefore, for backward compatibility, this error will be
     * converted to ERROR_HW_UNAVAILABLE.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public static final int ERROR_NOT_ENABLED_FOR_APPS = 21;

    /**
     * The user pressed the more options button on {@link PromptContentViewWithMoreOptionsButton}
     * set by {@link PromptInfo.Builder#setContentView}
     */
    public static final int ERROR_CONTENT_VIEW_MORE_OPTIONS_BUTTON = 22;

    /**
     * An error code that may be returned during authentication.
     */
    @IntDef({
        ERROR_HW_UNAVAILABLE,
        ERROR_UNABLE_TO_PROCESS,
        ERROR_TIMEOUT,
        ERROR_NO_SPACE,
        ERROR_CANCELED,
        ERROR_LOCKOUT,
        ERROR_VENDOR,
        ERROR_LOCKOUT_PERMANENT,
        ERROR_USER_CANCELED,
        ERROR_NO_BIOMETRICS,
        ERROR_HW_NOT_PRESENT,
        ERROR_NEGATIVE_BUTTON,
        ERROR_NO_DEVICE_CREDENTIAL,
        ERROR_SECURITY_UPDATE_REQUIRED,
        ERROR_SENSOR_PRIVACY_ENABLED,
        ERROR_IDENTITY_CHECK_NOT_ACTIVE,
        ERROR_NOT_ENABLED_FOR_APPS,
        ERROR_CONTENT_VIEW_MORE_OPTIONS_BUTTON
    })
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @Retention(RetentionPolicy.SOURCE)
    public @interface AuthenticationError {}

    /**
     * Authentication type reported by {@link AuthenticationResult} when the user authenticated via
     * an unknown method.
     *
     * <p>This value may be returned on older Android versions due to partial incompatibility
     * with a newer API. It does NOT necessarily imply that the user authenticated with a method
     * other than those represented by {@link #AUTHENTICATION_RESULT_TYPE_DEVICE_CREDENTIAL} and
     * {@link #AUTHENTICATION_RESULT_TYPE_BIOMETRIC}.
     */
    public static final int AUTHENTICATION_RESULT_TYPE_UNKNOWN = -1;

    /**
     * Authentication type reported by {@link AuthenticationResult} when the user authenticated by
     * entering their device PIN, pattern, or password.
     */
    public static final int AUTHENTICATION_RESULT_TYPE_DEVICE_CREDENTIAL = 1;

    /**
     * Authentication type reported by {@link AuthenticationResult} when the user authenticated by
     * presenting some form of biometric (e.g. fingerprint or face).
     */
    public static final int AUTHENTICATION_RESULT_TYPE_BIOMETRIC = 2;

    /**
     * The authentication type that was used, as reported by {@link AuthenticationResult}.
     */
    @IntDef({
        AUTHENTICATION_RESULT_TYPE_UNKNOWN,
        AUTHENTICATION_RESULT_TYPE_DEVICE_CREDENTIAL,
        AUTHENTICATION_RESULT_TYPE_BIOMETRIC
    })
    @Retention(RetentionPolicy.SOURCE)
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public @interface AuthenticationResultType {}

    /**
     * Tag used to identify the {@link BiometricFragment} attached to the client activity/fragment.
     */
    @VisibleForTesting
    static final String BIOMETRIC_FRAGMENT_TAG = "androidx.biometric.BiometricFragment";

    /**
     * A wrapper class for the crypto objects supported by {@link BiometricPrompt}.
     */
    public static class CryptoObject {
        private final @Nullable Signature mSignature;
        private final @Nullable Cipher mCipher;
        private final @Nullable Mac mMac;
        private final android.security.identity.@Nullable IdentityCredential mIdentityCredential;
        private final android.security.identity.@Nullable PresentationSession mPresentationSession;
        private final long mOperationHandle;

        /**
         * Creates a crypto object that wraps the given signature object.
         *
         * @param signature The signature to be associated with this crypto object.
         */
        public CryptoObject(@NonNull Signature signature) {
            mSignature = signature;
            mCipher = null;
            mMac = null;
            mIdentityCredential = null;
            mPresentationSession = null;
            mOperationHandle = 0;
        }

        /**
         * Creates a crypto object that wraps the given cipher object.
         *
         * @param cipher The cipher to be associated with this crypto object.
         */
        public CryptoObject(@NonNull Cipher cipher) {
            mSignature = null;
            mCipher = cipher;
            mMac = null;
            mIdentityCredential = null;
            mPresentationSession = null;
            mOperationHandle = 0;
        }

        /**
         * Creates a crypto object that wraps the given MAC object.
         *
         * @param mac The MAC to be associated with this crypto object.
         */
        public CryptoObject(@NonNull Mac mac) {
            mSignature = null;
            mCipher = null;
            mMac = mac;
            mIdentityCredential = null;
            mPresentationSession = null;
            mOperationHandle = 0;
        }

        /**
         * Creates a crypto object that wraps the given identity credential object.
         *
         * @param identityCredential The identity credential to be associated with this crypto
         *                           object.
         * @deprecated Use {@link CryptoObject#CryptoObject(PresentationSession)} instead.
         */
        @RequiresApi(Build.VERSION_CODES.R)
        @Deprecated
        public CryptoObject(
                android.security.identity.@NonNull IdentityCredential identityCredential) {
            mSignature = null;
            mCipher = null;
            mMac = null;
            mIdentityCredential = identityCredential;
            mPresentationSession = null;
            mOperationHandle = 0;
        }

        /**
         * Creates a crypto object that wraps the given presentation session object.
         *
         * @param presentationSession The presentation session to be associated with this crypto
         *                           object.
         */
        @RequiresApi(Build.VERSION_CODES.TIRAMISU)
        public CryptoObject(
                android.security.identity.@NonNull PresentationSession presentationSession) {
            mSignature = null;
            mCipher = null;
            mMac = null;
            mIdentityCredential = null;
            mPresentationSession = presentationSession;
            mOperationHandle = 0;
        }

        /**
         * Create from an operation handle.
         * @see CryptoObject#getOperationHandle()
         *
         * @param operationHandle the operation handle associated with this object.
         */
        @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
        public CryptoObject(long operationHandle) {
            mSignature = null;
            mCipher = null;
            mMac = null;
            mIdentityCredential = null;
            mPresentationSession = null;
            mOperationHandle = operationHandle;
        }


        /**
         * Gets the signature object associated with this crypto object.
         *
         * @return The signature, or {@code null} if none is associated with this object.
         */
        public @Nullable Signature getSignature() {
            return mSignature;
        }

        /**
         * Gets the cipher object associated with this crypto object.
         *
         * @return The cipher, or {@code null} if none is associated with this object.
         */
        public @Nullable Cipher getCipher() {
            return mCipher;
        }

        /**
         * Gets the MAC object associated with this crypto object.
         *
         * @return The MAC, or {@code null} if none is associated with this object.
         */
        public @Nullable Mac getMac() {
            return mMac;
        }

        /**
         * Gets the identity credential object associated with this crypto object.
         *
         * @return The identity credential, or {@code null} if none is associated with this object.
         * @deprecated Use {@link PresentationSession} instead of {@link IdentityCredential}.
         */
        @RequiresApi(Build.VERSION_CODES.R)
        @Deprecated
        public android.security.identity.@Nullable IdentityCredential getIdentityCredential() {
            return mIdentityCredential;
        }

        /**
         * Gets the presentation session object associated with this crypto object.
         *
         * @return The presentation session, or {@code null} if none is associated with this object.
         */
        @RequiresApi(Build.VERSION_CODES.TIRAMISU)
        public android.security.identity.@Nullable PresentationSession getPresentationSession() {
            return mPresentationSession;
        }

        /**
         * Returns the {@code operationHandle} associated with this object or 0 if none.
         * The {@code operationHandle} is the underlying identifier associated with
         * the {@code CryptoObject}.
         *
         * <p> The {@code operationHandle} can be used to reconstruct a {@code CryptoObject}
         * instance. This is useful for any cross-process communication as the {@code CryptoObject}
         * class is not {@link android.os.Parcelable}. Hence, if the {@code CryptoObject} is
         * constructed in one process, and needs to be propagated to another process,
         * before calling the {@code authenticate()} API in the second process, the
         * recommendation is to retrieve the {@code operationHandle} using this API, and then
         * reconstruct the {@code CryptoObject}using the constructor that takes in an {@code
         * operationHandle}, and pass that in to the {@code authenticate} API mentioned above.
         */
        @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
        public long getOperationHandle() {
            return CryptoObjectUtils.getOperationHandle(this);
        }

        /**
         * Returns the {@code operationHandle} from the constructor. This is only for wrapping
         * this {@link androidx.biometric.BiometricPrompt.CryptoObject} to
         * {@link android.hardware.biometrics.BiometricPrompt}.
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        public long getOperationHandleCryptoObject() {
            return mOperationHandle;
        }
    }

    /**
     * A container for data passed to {@link AuthenticationCallback#onAuthenticationSucceeded(
     * AuthenticationResult)} when the user has successfully authenticated.
     */
    public static class AuthenticationResult {
        private final CryptoObject mCryptoObject;
        @AuthenticationResultType private final int mAuthenticationType;

        @RestrictTo(RestrictTo.Scope.LIBRARY)
        public AuthenticationResult(@Nullable CryptoObject crypto,
                @AuthenticationResultType int authenticationType) {
            mCryptoObject = crypto;
            mAuthenticationType = authenticationType;
        }

        /**
         * Gets the {@link CryptoObject} associated with this transaction.
         *
         * @return The {@link CryptoObject} provided to {@code authenticate()}.
         */
        public @Nullable CryptoObject getCryptoObject() {
            return mCryptoObject;
        }

        /**
         * Gets the type of authentication (e.g. device credential or biometric) that was
         * requested from and successfully provided by the user.
         *
         * @return An integer representing the type of authentication that was used.
         *
         * @see #AUTHENTICATION_RESULT_TYPE_UNKNOWN
         * @see #AUTHENTICATION_RESULT_TYPE_DEVICE_CREDENTIAL
         * @see #AUTHENTICATION_RESULT_TYPE_BIOMETRIC
         */
        @AuthenticationResultType
        public int getAuthenticationType() {
            return mAuthenticationType;
        }
    }

    /**
     * A collection of methods that may be invoked by {@link BiometricPrompt} during authentication.
     */
    public abstract static class AuthenticationCallback {
        /**
         * Called when an unrecoverable error has been encountered and authentication has stopped.
         *
         * <p>After this method is called, no further events will be sent for the current
         * authentication session.
         *
         * @param errorCode An integer ID associated with the error.
         * @param errString A human-readable string that describes the error.
         */
        public void onAuthenticationError(
                @AuthenticationError int errorCode, @NonNull CharSequence errString) {}

        /**
         * Called when a biometric (e.g. fingerprint, face, etc.) is recognized, indicating that the
         * user has successfully authenticated.
         *
         * <p>After this method is called, no further events will be sent for the current
         * authentication session.
         *
         * @param result An object containing authentication-related data.
         */
        public void onAuthenticationSucceeded(@NonNull AuthenticationResult result) {}

        /**
         * Called when a biometric (e.g. fingerprint, face, etc.) is presented but not recognized as
         * belonging to the user.
         */
        public void onAuthenticationFailed() {}
    }

    /**
     * A set of configurable options for how the {@link BiometricPrompt} should appear and behave.
     */
    public static class PromptInfo {
        /**
         * A builder used to set individual options for the {@link PromptInfo} class.
         */
        public static class Builder {
            // Mutable options to be set on the builder.
            @DrawableRes private int mLogoRes = -1;
            private @Nullable Bitmap mLogoBitmap = null;
            private @Nullable String mLogoDescription = null;
            private @Nullable CharSequence mTitle = null;
            private @Nullable CharSequence mSubtitle = null;
            private @Nullable CharSequence mDescription = null;
            private @Nullable PromptContentView mPromptContentView = null;
            private @Nullable CharSequence mNegativeButtonText = null;
            private boolean mIsConfirmationRequired = true;
            private boolean mIsDeviceCredentialAllowed = false;
            @BiometricManager.AuthenticatorTypes private int mAllowedAuthenticators = 0;

            /**
             * Optional: Sets the drawable resource of the logo that will be shown on the prompt.
             *
             * <p> Note that using this method is not recommended in most scenarios because the
             * calling application's icon will be used by default. Setting the logo is intended
             * for large bundled applications that perform a wide range of functions and need to
             * show distinct icons for each function.
             *
             * @param logoRes A drawable resource of the logo that will be shown on the prompt.
             * @return This builder.
             */
            @RequiresPermission(SET_BIOMETRIC_DIALOG_ADVANCED)
            public @NonNull Builder setLogoRes(@DrawableRes int logoRes) {
                mLogoRes = logoRes;
                return this;
            }

            /**
             * Optional: Sets the bitmap drawable of the logo that will be shown on the prompt.
             *
             * <p> Note that using this method is not recommended in most scenarios because the
             * calling application's icon will be used by default. Setting the logo is intended
             * for large bundled applications that perform a wide range of functions and need to
             * show distinct icons for each function.
             *
             * @param logoBitmap A bitmap drawable of the logo that will be shown on the prompt.
             * @return This builder.
             */
            @RequiresPermission(SET_BIOMETRIC_DIALOG_ADVANCED)
            public @NonNull Builder setLogoBitmap(@NonNull Bitmap logoBitmap) {
                mLogoBitmap = logoBitmap;
                return this;
            }

            /**
             * Optional: Sets logo description text that will be shown on the prompt.
             *
             * <p> Note that using this method is not recommended in most scenarios because the
             * calling application's name will be used by default. Setting the logo description
             * is intended for large bundled applications that perform a wide range of functions
             * and need to show distinct description for each function.
             *
             * @param logoDescription The logo description text that will be shown on the prompt.
             * @return This builder.
             * @throws IllegalArgumentException If logo description is null or exceeds certain
             *                                  character limit.
             */
            @RequiresPermission(SET_BIOMETRIC_DIALOG_ADVANCED)
            public @NonNull Builder setLogoDescription(@NonNull String logoDescription) {
                mLogoDescription = logoDescription;
                return this;
            }

            /**
             * Required: Sets the title for the prompt.
             *
             * @param title The title to be displayed on the prompt.
             * @return This builder.
             */
            public @NonNull Builder setTitle(@NonNull CharSequence title) {
                mTitle = title;
                return this;
            }

            /**
             * Optional: Sets the subtitle for the prompt.
             *
             * @param subtitle The subtitle to be displayed on the prompt.
             * @return This builder.
             */
            public @NonNull Builder setSubtitle(@Nullable CharSequence subtitle) {
                mSubtitle = subtitle;
                return this;
            }

            /**
             * Optional: Sets a description that will be shown on the prompt.
             *
             * <p> Note that from API 35, the description set by
             * {@link Builder#setDescription(CharSequence)} will be overridden by
             * {@link Builder#setContentView(PromptContentView)}. The view provided to
             * {@link Builder#setContentView(PromptContentView)} will be used if both methods are
             * called.
             *
             * @param description The description to display.
             * @return This builder.
             */
            public @NonNull Builder setDescription(@Nullable CharSequence description) {
                mDescription = description;
                return this;
            }

            /**
             * Optional: Sets application customized content view that will be shown on the prompt.
             *
             * <p> Note that from API 35, the description set by
             * {@link Builder#setDescription(CharSequence)} will be overridden by
             * {@link Builder#setContentView(PromptContentView)}. The view provided to
             * {@link Builder#setContentView(PromptContentView)} will be used if both methods are
             * called.
             *
             * @param view The customized view information.
             * @return This builder.
             */
            public @NonNull Builder setContentView(@NonNull PromptContentView view) {
                mPromptContentView = view;
                return this;
            }

            /**
             * Required: Sets the text for the negative button on the prompt.
             *
             * <p>Note that this option is incompatible with device credential authentication and
             * must NOT be set if the latter is enabled via {@link #setAllowedAuthenticators(int)}
             * or {@link #setDeviceCredentialAllowed(boolean)}.
             *
             * @param negativeButtonText The label to be used for the negative button on the prompt.
             * @return This builder.
             */
            @SuppressWarnings("deprecation")
            public @NonNull Builder setNegativeButtonText(
                    @NonNull CharSequence negativeButtonText) {
                mNegativeButtonText = negativeButtonText;
                return this;
            }

            /**
             * Optional: Sets a system hint for whether to require explicit user confirmation after
             * a passive biometric (e.g. iris or face) has been recognized but before
             * {@link AuthenticationCallback#onAuthenticationSucceeded(AuthenticationResult)} is
             * called. Defaults to {@code true}.
             *
             * <p>Disabling this option is generally only appropriate for frequent, low-value
             * transactions, such as re-authenticating for a previously authorized application.
             *
             * <p>Also note that, as it is merely a hint, this option may be ignored by the system.
             * For example, the system may choose to instead always require confirmation if the user
             * has disabled passive authentication for their device in Settings. Additionally, this
             * option will be ignored on devices running OS versions prior to Android 10 (API 29).
             *
             * @param confirmationRequired Whether this option should be enabled.
             * @return This builder.
             */
            public @NonNull Builder setConfirmationRequired(boolean confirmationRequired) {
                mIsConfirmationRequired = confirmationRequired;
                return this;
            }

            /**
             * Optional: Sets whether the user should be given the option to authenticate with
             * their device PIN, pattern, or password instead of a biometric. Defaults to
             * {@code false}.
             *
             * <p>Note that this option is incompatible with
             * {@link PromptInfo.Builder#setNegativeButtonText(CharSequence)} and must NOT be
             * enabled if the latter is set.
             *
             * <p>Before enabling this option, developers should check whether the device is secure
             * by calling {@link android.app.KeyguardManager#isDeviceSecure()}. If the device is not
             * secure, authentication will fail with {@link #ERROR_NO_DEVICE_CREDENTIAL}.
             *
             * <p>On versions prior to Android 10 (API 29), calls to
             * {@link #cancelAuthentication()} will not work as expected after the
             * user has chosen to authenticate with their device credential. This is because the
             * library internally launches a separate activity (by calling
             * {@link android.app.KeyguardManager#createConfirmDeviceCredentialIntent(CharSequence,
             * CharSequence)}) that does not have a public API for cancellation.
             *
             * @param deviceCredentialAllowed Whether this option should be enabled.
             * @return This builder.
             *
             * @deprecated Use {@link #setAllowedAuthenticators(int)} instead.
             */
            @SuppressWarnings("deprecation")
            @Deprecated
            public @NonNull Builder setDeviceCredentialAllowed(boolean deviceCredentialAllowed) {
                mIsDeviceCredentialAllowed = deviceCredentialAllowed;
                return this;
            }

            /**
             * Optional: Specifies the type(s) of authenticators that may be invoked by
             * {@link BiometricPrompt} to authenticate the user. Available authenticator types are
             * defined in {@link Authenticators} and can be combined via bitwise OR. Defaults to:
             * <ul>
             *     <li>{@link Authenticators#BIOMETRIC_WEAK} for non-crypto authentication, or</li>
             *     <li>{@link Authenticators#BIOMETRIC_STRONG} for crypto-based authentication.</li>
             * </ul>
             *
             * <p>Note that not all combinations of authenticator types are supported prior to
             * Android 11 (API 30). Specifically, {@code DEVICE_CREDENTIAL} alone is unsupported
             * prior to API 30, and {@code BIOMETRIC_STRONG | DEVICE_CREDENTIAL} is unsupported on
             * API 28-29. Setting an unsupported value on an affected Android version will result in
             * an error when calling {@link #build()}.
             *
             * <p>This method should be preferred over {@link #setDeviceCredentialAllowed(boolean)}
             * and overrides the latter if both are used. Using this method to enable device
             * credential authentication (with {@link Authenticators#DEVICE_CREDENTIAL}) will
             * replace the negative button on the prompt, making it an error to also call
             * {@link #setNegativeButtonText(CharSequence)}.
             *
             * <p>If this method is used and no authenticator of any of the specified types is
             * available at the time {@code authenticate()} is called,
             * {@link AuthenticationCallback#onAuthenticationError(int, CharSequence)} will be
             * invoked with an appropriate error code.
             *
             * @param allowedAuthenticators A bit field representing all valid authenticator types
             *                              that may be invoked by the prompt.
             * @return This builder.
             */
            public @NonNull Builder setAllowedAuthenticators(
                    @BiometricManager.AuthenticatorTypes int allowedAuthenticators) {
                mAllowedAuthenticators = allowedAuthenticators;
                return this;
            }

            /**
             * Creates a {@link PromptInfo} object with the specified options.
             *
             * @return The {@link PromptInfo} object.
             *
             * @throws IllegalArgumentException If any required option is not set, or if any
             *                                  illegal combination of options is present.
             */
            public @NonNull PromptInfo build() {
                if (TextUtils.isEmpty(mTitle)) {
                    throw new IllegalArgumentException("Title must be set and non-empty.");
                }
                if (!AuthenticatorUtils.isSupportedCombination(mAllowedAuthenticators)) {
                    throw new IllegalArgumentException("Authenticator combination is unsupported "
                            + "on API " + Build.VERSION.SDK_INT + ": "
                            + AuthenticatorUtils.convertToString(mAllowedAuthenticators));
                }

                final boolean isDeviceCredentialAllowed = mAllowedAuthenticators != 0
                        ? AuthenticatorUtils.isDeviceCredentialAllowed(mAllowedAuthenticators)
                        : mIsDeviceCredentialAllowed;
                if (TextUtils.isEmpty(mNegativeButtonText) && !isDeviceCredentialAllowed) {
                    throw new IllegalArgumentException("Negative text must be set and non-empty.");
                }
                if (!TextUtils.isEmpty(mNegativeButtonText) && isDeviceCredentialAllowed) {
                    throw new IllegalArgumentException("Negative text must not be set if device "
                            + "credential authentication is allowed.");
                }

                return new PromptInfo(
                        mLogoRes,
                        mLogoBitmap,
                        mLogoDescription,
                        mTitle,
                        mSubtitle,
                        mDescription,
                        mPromptContentView,
                        mNegativeButtonText,
                        mIsConfirmationRequired,
                        mIsDeviceCredentialAllowed,
                        mAllowedAuthenticators);
            }
        }

        // Immutable fields for the prompt info object.
        @DrawableRes private int mLogoRes;
        private @Nullable Bitmap mLogoBitmap;
        private @Nullable String mLogoDescription;
        private final @NonNull CharSequence mTitle;
        private final @Nullable CharSequence mSubtitle;
        private final @Nullable CharSequence mDescription;
        private @Nullable PromptContentView mPromptContentView;
        private final @Nullable CharSequence mNegativeButtonText;
        private final boolean mIsConfirmationRequired;
        private final boolean mIsDeviceCredentialAllowed;
        @BiometricManager.AuthenticatorTypes private final int mAllowedAuthenticators;

        // Prevent direct instantiation.
        @SuppressWarnings("WeakerAccess") /* synthetic access */
        PromptInfo(
                int logoRes,
                @Nullable Bitmap logoBitmap,
                @Nullable String logoDescription,
                @NonNull CharSequence title,
                @Nullable CharSequence subtitle,
                @Nullable CharSequence description,
                @Nullable PromptContentView promptContentView,
                @Nullable CharSequence negativeButtonText,
                boolean confirmationRequired,
                boolean deviceCredentialAllowed,
                @BiometricManager.AuthenticatorTypes int allowedAuthenticators) {
            mLogoRes = logoRes;
            mLogoBitmap = logoBitmap;
            mLogoDescription = logoDescription;
            mTitle = title;
            mSubtitle = subtitle;
            mDescription = description;
            mPromptContentView = promptContentView;
            mNegativeButtonText = negativeButtonText;
            mIsConfirmationRequired = confirmationRequired;
            mIsDeviceCredentialAllowed = deviceCredentialAllowed;
            mAllowedAuthenticators = allowedAuthenticators;
        }

        /**
         * Gets the drawable resource of the logo for the prompt, as set by
         * {@link Builder#setLogoRes(int)}. Currently for system applications use only.
         *
         * @return The drawable resource of the logo, or -1 if the prompt has no logo resource set.
         */
        @RequiresPermission(SET_BIOMETRIC_DIALOG_ADVANCED)
        @DrawableRes
        public int getLogoRes() {
            return mLogoRes;
        }

        /**
         * Gets the logo bitmap for the prompt, as set by {@link Builder#setLogoBitmap(Bitmap)}.
         * Currently for system applications use only.
         *
         * @return The logo bitmap of the prompt, or null if the prompt has no logo bitmap set.
         */
        @RequiresPermission(SET_BIOMETRIC_DIALOG_ADVANCED)
        public @Nullable Bitmap getLogoBitmap() {
            return mLogoBitmap;
        }

        /**
         * Gets the logo description for the prompt, as set by
         * {@link Builder#setLogoDescription(String)}.
         * Currently for system applications use only.
         *
         * @return The logo description of the prompt, or null if the prompt has no logo description
         * set.
         */
        @RequiresPermission(SET_BIOMETRIC_DIALOG_ADVANCED)
        public @Nullable String getLogoDescription() {
            return mLogoDescription;
        }

        /**
         * Gets the title for the prompt.
         *
         * @return The title to be displayed on the prompt.
         *
         * @see Builder#setTitle(CharSequence)
         */
        public @NonNull CharSequence getTitle() {
            return mTitle;
        }

        /**
         * Gets the subtitle for the prompt.
         *
         * @return The subtitle to be displayed on the prompt.
         *
         * @see Builder#setSubtitle(CharSequence)
         */
        public @Nullable CharSequence getSubtitle() {
            return mSubtitle;
        }

        /**
         * Gets the description for the prompt.
         *
         * @return The description to be displayed on the prompt.
         *
         * @see Builder#setDescription(CharSequence)
         */
        public @Nullable CharSequence getDescription() {
            return mDescription;
        }

        /**
         * Gets the content view for the prompt, as set by
         * {@link Builder#setContentView(PromptContentView)}.
         *
         * @return The content view for the prompt, or null if the prompt has no content view.
         */
        public @Nullable PromptContentView getContentView() {
            return mPromptContentView;
        }

        /**
         * Gets the text for the negative button on the prompt.
         *
         * @return The label to be used for the negative button on the prompt, or an empty string if
         * not set.
         *
         * @see Builder#setNegativeButtonText(CharSequence)
         */
        public @NonNull CharSequence getNegativeButtonText() {
            return mNegativeButtonText != null ? mNegativeButtonText : "";
        }

        /**
         * Checks if the confirmation required option is enabled for the prompt.
         *
         * @return Whether this option is enabled.
         *
         * @see Builder#setConfirmationRequired(boolean)
         */
        public boolean isConfirmationRequired() {
            return mIsConfirmationRequired;
        }

        /**
         * Checks if the device credential allowed option is enabled for the prompt.
         *
         * @return Whether this option is enabled.
         *
         * @see Builder#setDeviceCredentialAllowed(boolean)
         *
         * @deprecated Will be removed with {@link Builder#setDeviceCredentialAllowed(boolean)}.
         */
        @SuppressWarnings({"deprecation", "DeprecatedIsStillUsed"})
        @Deprecated
        public boolean isDeviceCredentialAllowed() {
            return mIsDeviceCredentialAllowed;
        }

        /**
         * Gets the type(s) of authenticators that may be invoked by the prompt.
         *
         * @return A bit field representing all valid authenticator types that may be invoked by
         * the prompt, or 0 if not set.
         *
         * @see Builder#setAllowedAuthenticators(int)
         */
        @BiometricManager.AuthenticatorTypes
        public int getAllowedAuthenticators() {
            return mAllowedAuthenticators;
        }
    }

    /**
     * A lifecycle observer that clears the client callback reference held by a
     * {@link BiometricViewModel} when the lifecycle owner is destroyed.
     */
    private static class ResetCallbackObserver implements DefaultLifecycleObserver {
        private final @NonNull WeakReference<BiometricViewModel> mViewModelRef;

        ResetCallbackObserver(@NonNull BiometricViewModel viewModel) {
            mViewModelRef = new WeakReference<>(viewModel);
        }

        @Override
        public void onDestroy(@NonNull LifecycleOwner owner) {
            if (mViewModelRef.get() != null) {
                mViewModelRef.get().resetClientCallback();
            }
        }
    }

    /**
     * The fragment manager that will be used to attach the prompt to the client activity.
     */
    private @Nullable FragmentManager mClientFragmentManager;
    private boolean mHostedInActivity;

    /**
     * Constructs a {@link BiometricPrompt}, which can be used to prompt the user to authenticate
     * with a biometric such as fingerprint or face. The prompt can be shown to the user by calling
     * {@code authenticate()} and persists across device configuration changes by default.
     *
     * <p>If authentication is in progress, calling this constructor to recreate the prompt will
     * also update the {@link AuthenticationCallback} for the current session. Thus, this method
     * should be called by the client activity each time the configuration changes
     * (e.g. in {@code onCreate()}).
     *
     * @param activity The activity of the client application that will host the prompt.
     * @param callback The object that will receive and process authentication events.
     *
     * @see #BiometricPrompt(Fragment, AuthenticationCallback)
     * @see #BiometricPrompt(FragmentActivity, Executor, AuthenticationCallback)
     * @see #BiometricPrompt(Fragment, Executor, AuthenticationCallback)
     */
    @SuppressWarnings("ConstantConditions")
    public BiometricPrompt(
            @NonNull FragmentActivity activity, @NonNull AuthenticationCallback callback) {

        if (activity == null) {
            throw new IllegalArgumentException("FragmentActivity must not be null.");
        }
        if (callback == null) {
            throw new IllegalArgumentException("AuthenticationCallback must not be null.");
        }

        final FragmentManager fragmentManager = activity.getSupportFragmentManager();
        final BiometricViewModel viewModel =
                new ViewModelProvider(activity).get(BiometricViewModel.class);
        init(true /* hostedInActivity */, fragmentManager, viewModel, null /* executor */,
                callback);
    }

    /**
     * Constructs a {@link BiometricPrompt}, which can be used to prompt the user to authenticate
     * with a biometric such as fingerprint or face. The prompt can be shown to the user by calling
     * {@code authenticate()} and persists across device configuration changes by default.
     *
     * <p>If authentication is in progress, calling this constructor to recreate the prompt will
     * also update the {@link AuthenticationCallback} for the current session. Thus, this method
     * should be called by the client fragment each time the configuration changes
     * (e.g. in {@code onCreate()}).
     *
     * @param fragment The fragment of the client application that will host the prompt.
     * @param callback The object that will receive and process authentication events.
     *
     * @see #BiometricPrompt(FragmentActivity, AuthenticationCallback)
     * @see #BiometricPrompt(FragmentActivity, Executor, AuthenticationCallback)
     * @see #BiometricPrompt(Fragment, Executor, AuthenticationCallback)
     */
    @SuppressWarnings("ConstantConditions")
    public BiometricPrompt(@NonNull Fragment fragment, @NonNull AuthenticationCallback callback) {

        if (fragment == null) {
            throw new IllegalArgumentException("Fragment must not be null.");
        }
        if (callback == null) {
            throw new IllegalArgumentException("AuthenticationCallback must not be null.");
        }

        final FragmentManager fragmentManager = fragment.getChildFragmentManager();
        final BiometricViewModel viewModel =
                new ViewModelProvider(fragment).get(BiometricViewModel.class);
        addObservers(fragment, viewModel);
        init(false /* hostedInActivity */, fragmentManager, viewModel, null /* executor */,
                callback);
    }

    /**
     * Constructs a {@link BiometricPrompt}, which can be used to prompt the user to authenticate
     * with a biometric such as fingerprint or face. The prompt can be shown to the user by calling
     * {@code authenticate()} and persists across device configuration changes by default.
     *
     * <p>If authentication is in progress, calling this constructor to recreate the prompt will
     * also update the {@link Executor} and {@link AuthenticationCallback} for the current session.
     * Thus, this method should be called by the client activity each time the configuration changes
     * (e.g. in {@code onCreate()}).
     *
     * @param activity The activity of the client application that will host the prompt.
     * @param executor The executor that will be used to run {@link AuthenticationCallback} methods.
     * @param callback The object that will receive and process authentication events.
     *
     * @see #BiometricPrompt(FragmentActivity, AuthenticationCallback)
     * @see #BiometricPrompt(Fragment, AuthenticationCallback)
     * @see #BiometricPrompt(Fragment, Executor, AuthenticationCallback)
     */
    @SuppressLint("LambdaLast")
    @SuppressWarnings("ConstantConditions")
    public BiometricPrompt(
            @NonNull FragmentActivity activity,
            @NonNull Executor executor,
            @NonNull AuthenticationCallback callback) {

        if (activity == null) {
            throw new IllegalArgumentException("FragmentActivity must not be null.");
        }
        if (executor == null) {
            throw new IllegalArgumentException("Executor must not be null.");
        }
        if (callback == null) {
            throw new IllegalArgumentException("AuthenticationCallback must not be null.");
        }

        final FragmentManager fragmentManager = activity.getSupportFragmentManager();
        final BiometricViewModel viewModel =
                new ViewModelProvider(activity).get(BiometricViewModel.class);
        init(true /* hostedInActivity */, fragmentManager, viewModel, executor, callback);
    }

    /**
     * Constructs a {@link BiometricPrompt}, which can be used to prompt the user to authenticate
     * with a biometric such as fingerprint or face. The prompt can be shown to the user by calling
     * {@code authenticate()} and persists across device configuration changes by default.
     *
     * <p>If authentication is in progress, calling this constructor to recreate the prompt will
     * also update the {@link Executor} and {@link AuthenticationCallback} for the current session.
     * Thus, this method should be called by the client fragment each time the configuration changes
     * (e.g. in {@code onCreate()}).
     *
     * @param fragment The fragment of the client application that will host the prompt.
     * @param executor The executor that will be used to run {@link AuthenticationCallback} methods.
     * @param callback The object that will receive and process authentication events.
     *
     * @see #BiometricPrompt(FragmentActivity, AuthenticationCallback)
     * @see #BiometricPrompt(Fragment, AuthenticationCallback)
     * @see #BiometricPrompt(FragmentActivity, Executor, AuthenticationCallback)
     */
    @SuppressLint("LambdaLast")
    @SuppressWarnings("ConstantConditions")
    public BiometricPrompt(
            @NonNull Fragment fragment,
            @NonNull Executor executor,
            @NonNull AuthenticationCallback callback) {

        if (fragment == null) {
            throw new IllegalArgumentException("Fragment must not be null.");
        }
        if (executor == null) {
            throw new IllegalArgumentException("Executor must not be null.");
        }
        if (callback == null) {
            throw new IllegalArgumentException("AuthenticationCallback must not be null.");
        }

        final FragmentManager fragmentManager = fragment.getChildFragmentManager();
        final BiometricViewModel viewModel =
                new ViewModelProvider(fragment).get(BiometricViewModel.class);
        addObservers(fragment, viewModel);
        init(false /* hostedInActivity */, fragmentManager, viewModel, executor, callback);
    }

    /**
     * Initializes or updates the data needed by the prompt.
     *
     * @param fragmentManager The fragment manager that will be used to attach the prompt.
     * @param viewModel       A biometric view model tied to the lifecycle of the client activity.
     * @param executor        The executor that will be used to run callback methods, or
     *                        {@link null} if a default executor should be used.
     * @param callback        The object that will receive and process authentication events.
     */
    private void init(
            boolean hostedInActivity,
            @NonNull FragmentManager fragmentManager,
            @NonNull BiometricViewModel viewModel,
            @Nullable Executor executor,
            @NonNull AuthenticationCallback callback) {
        mHostedInActivity = hostedInActivity;
        mClientFragmentManager = fragmentManager;

        if (executor != null) {
            viewModel.setClientExecutor(executor);
        }
        viewModel.setClientCallback(callback);
    }

    /**
     * Shows the biometric prompt to the user. The prompt survives lifecycle changes by default. To
     * cancel authentication and dismiss the prompt, use {@link #cancelAuthentication()}.
     *
     * <p>Calling this method invokes crypto-based authentication, which is incompatible with
     * <strong>Class 2</strong> (formerly <strong>Weak</strong>) biometrics and (prior to Android
     * 11) device credential. Therefore, it is an error for {@code info} to explicitly allow any
     * of these authenticator types on an incompatible Android version.
     *
     * @param info   An object describing the appearance and behavior of the prompt.
     * @param crypto A crypto object to be associated with this authentication.
     *
     * @throws IllegalArgumentException If any of the allowed authenticator types specified by
     *                                  {@code info} do not support crypto-based authentication.
     *
     * @see #authenticate(PromptInfo)
     * @see PromptInfo.Builder#setAllowedAuthenticators(int)
     */
    @SuppressWarnings("ConstantConditions")
    public void authenticate(@NonNull PromptInfo info, @NonNull CryptoObject crypto) {
        if (info == null) {
            throw new IllegalArgumentException("PromptInfo cannot be null.");
        }
        if (crypto == null) {
            throw new IllegalArgumentException("CryptoObject cannot be null.");
        }

        // Ensure that all allowed authenticators support crypto auth. |isIdentityCheckAvailable|
        // is not important for this check.
        @BiometricManager.AuthenticatorTypes final int authenticators =
                AuthenticatorUtils.getConsolidatedAuthenticators(info, crypto,
                        false /*isIdentityCheckAvailable*/);
        if (AuthenticatorUtils.isWeakBiometricAllowed(authenticators)) {
            throw new IllegalArgumentException("Crypto-based authentication is not supported for "
                    + "Class 2 (Weak) biometrics.");
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R
                && AuthenticatorUtils.isDeviceCredentialAllowed(authenticators)) {
            throw new IllegalArgumentException("Crypto-based authentication is not supported for "
                    + "device credential prior to API 30.");
        }

        authenticateInternal(info, crypto);
    }

    /**
     * Shows the biometric prompt to the user. The prompt survives lifecycle changes by default. To
     * cancel authentication and dismiss the prompt, use {@link #cancelAuthentication()}.
     *
     * @param info An object describing the appearance and behavior of the prompt.
     *
     * @see #authenticate(PromptInfo, CryptoObject)
     */
    @SuppressWarnings("ConstantConditions")
    public void authenticate(@NonNull PromptInfo info) {
        if (info == null) {
            throw new IllegalArgumentException("PromptInfo cannot be null.");
        }

        authenticateInternal(info, null /* crypto */);
    }

    /**
     * Shows the biometric prompt to the user and begins authentication.
     *
     * @param info   An object describing the appearance and behavior of the prompt.
     * @param crypto A crypto object to be associated with this authentication.
     */
    private void authenticateInternal(@NonNull PromptInfo info, @Nullable CryptoObject crypto) {
        if (mClientFragmentManager == null) {
            Log.e(TAG, "Unable to start authentication. Client fragment manager was null.");
            return;
        }
        if (mClientFragmentManager.isStateSaved()) {
            Log.e(TAG, "Unable to start authentication. Called after onSaveInstanceState().");
            return;
        }

        final BiometricFragment biometricFragment = findOrAddBiometricFragment();
        biometricFragment.authenticate(info, crypto);
    }

    /**
     * Cancels the ongoing authentication session and dismisses the prompt.
     *
     * <p>On versions prior to Android 10 (API 29), calling this method while the user is
     * authenticating with their device credential will NOT work as expected. See
     * {@link PromptInfo.Builder#setDeviceCredentialAllowed(boolean)} for more details.
     */
    public void cancelAuthentication() {
        if (mClientFragmentManager == null) {
            Log.e(TAG, "Unable to start authentication. Client fragment manager was null.");
            return;
        }

        final BiometricFragment biometricFragment = findBiometricFragment(mClientFragmentManager);
        if (biometricFragment == null) {
            Log.e(TAG, "Unable to cancel authentication. BiometricFragment not found.");
            return;
        }

        biometricFragment.cancelAuthentication(BiometricFragment.CANCELED_FROM_CLIENT);
    }

    /**
     * Gets the biometric view model instance using the host activity or fragment that was
     * given in the constructor.
     *
     * @param fragment The fragment hosting the prompt.
     * @param hostedInActivity If one of the activity-based constructors was used.
     * @return A biometric view model tied to the lifecycle owner of the fragment.
     */
    static @NonNull BiometricViewModel getViewModel(@NonNull Fragment fragment,
            boolean hostedInActivity) {
        ViewModelStoreOwner owner = hostedInActivity ? fragment.getActivity() : null;
        if (owner == null) {
            owner = fragment.getParentFragment();
        }
        if (owner == null) {
            throw new IllegalStateException("view model not found");
        }
        return new ViewModelProvider(owner).get(BiometricViewModel.class);
    }

    /**
     * Adds the necessary lifecycle observers to the given fragment host.
     *
     * @param fragment  The fragment of the client application that will host the prompt.
     * @param viewModel A biometric view model tied to the lifecycle of the client activity.
     */
    private static void addObservers(
            @NonNull Fragment fragment, @NonNull BiometricViewModel viewModel) {
        // Ensure that the callback is reset to avoid leaking fragment instances (b/167014923).
        fragment.getLifecycle().addObserver(new ResetCallbackObserver(viewModel));
    }

    /**
     * Searches for a {@link BiometricFragment} instance that has been added to an activity or
     * fragment.
     *
     * @param fragmentManager The fragment manager that will be used to search for the fragment.
     * @return An instance of {@link BiometricFragment} found by the fragment manager, or
     * {@code null} if no such fragment is found.
     */
    private static @Nullable BiometricFragment findBiometricFragment(
            @NonNull FragmentManager fragmentManager) {
        return (BiometricFragment) fragmentManager.findFragmentByTag(
                BiometricPrompt.BIOMETRIC_FRAGMENT_TAG);
    }

    /**
     * Returns a {@link BiometricFragment} instance that has been added to an activity or fragment,
     * adding one if necessary.
     *
     * @return An instance of {@link BiometricFragment} associated with the fragment manager.
     */
    private @NonNull BiometricFragment findOrAddBiometricFragment() {
        BiometricFragment biometricFragment = findBiometricFragment(mClientFragmentManager);

        // If the fragment hasn't been added before, add it.
        if (biometricFragment == null) {
            biometricFragment = BiometricFragment.newInstance(mHostedInActivity);
            mClientFragmentManager.beginTransaction()
                    .add(biometricFragment, BiometricPrompt.BIOMETRIC_FRAGMENT_TAG)
                    .commitAllowingStateLoss();

            // For the case when onResume() is being called right after authenticate,
            // we need to make sure that all fragment transactions have been committed.
            mClientFragmentManager.executePendingTransactions();
        }

        return biometricFragment;
    }
}
