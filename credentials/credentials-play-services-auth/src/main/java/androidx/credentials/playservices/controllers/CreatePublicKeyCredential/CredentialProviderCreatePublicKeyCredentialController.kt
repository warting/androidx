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

package androidx.credentials.playservices.controllers.CreatePublicKeyCredential

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.CancellationSignal
import android.os.Handler
import android.os.Looper
import android.os.ResultReceiver
import androidx.annotation.VisibleForTesting
import androidx.credentials.CreateCredentialResponse
import androidx.credentials.CreatePublicKeyCredentialRequest
import androidx.credentials.CreatePublicKeyCredentialResponse
import androidx.credentials.CredentialManagerCallback
import androidx.credentials.exceptions.CreateCredentialException
import androidx.credentials.exceptions.CreateCredentialUnknownException
import androidx.credentials.exceptions.publickeycredential.CreatePublicKeyCredentialEncodingException
import androidx.credentials.exceptions.publickeycredential.CreatePublicKeyCredentialUnknownException
import androidx.credentials.playservices.CredentialProviderPlayServicesImpl
import androidx.credentials.playservices.HiddenActivity
import androidx.credentials.playservices.controllers.CredentialProviderBaseController
import androidx.credentials.playservices.controllers.CredentialProviderController
import com.google.android.gms.fido.Fido
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredential
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredentialCreationOptions
import java.util.concurrent.Executor
import org.json.JSONException

/**
 * A controller to handle the CreatePublicKeyCredential flow with play services.
 *
 * @hide
 */
@Suppress("deprecation")
class CredentialProviderCreatePublicKeyCredentialController(private val activity: Activity) :
        CredentialProviderController<
            CreatePublicKeyCredentialRequest,
            PublicKeyCredentialCreationOptions,
            PublicKeyCredential,
            CreateCredentialResponse,
            CreateCredentialException>(activity) {

    /**
     * The callback object state, used in the protected handleResponse method.
     */
    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    private lateinit var callback: CredentialManagerCallback<CreateCredentialResponse,
        CreateCredentialException>

    /**
     * The callback requires an executor to invoke it.
     */
    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    private lateinit var executor: Executor

    /**
     * The cancellation signal, which is shuttled around to stop the flow at any moment prior to
     * returning data.
     */
    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    private var cancellationSignal: CancellationSignal? = null

    private val resultReceiver = object : ResultReceiver(
        Handler(Looper.getMainLooper())
    ) {
        public override fun onReceiveResult(
            resultCode: Int,
            resultData: Bundle
        ) {
            if (maybeReportErrorFromResultReceiver(resultData,
                    CredentialProviderBaseController
                        .Companion::createPublicKeyCredentialExceptionTypeToException,
                    executor = executor, callback = callback, cancellationSignal)) return
            handleResponse(resultData.getInt(ACTIVITY_REQUEST_CODE_TAG), resultCode,
                resultData.getParcelable(RESULT_DATA_TAG))
        }
    }

    override fun invokePlayServices(
        request: CreatePublicKeyCredentialRequest,
        callback: CredentialManagerCallback<CreateCredentialResponse, CreateCredentialException>,
        executor: Executor,
        cancellationSignal: CancellationSignal?
    ) {
        this.cancellationSignal = cancellationSignal
        this.callback = callback
        this.executor = executor
        val fidoRegistrationRequest: PublicKeyCredentialCreationOptions
        try {
            fidoRegistrationRequest = this.convertRequestToPlayServices(request)
        } catch (e: JSONException) {
            // TODO("Merge with updated error codes CL")
            cancelAndCallbackException(CreatePublicKeyCredentialEncodingException(e.message),
                cancellationSignal) { ex -> this.executor.execute { this.callback.onError(ex) } }
            return
        } catch (t: Throwable) {
            cancelAndCallbackException(CreateCredentialUnknownException(t.message),
                cancellationSignal) { e -> this.executor.execute { this.callback.onError(e) } }
            return
        }

        if (CredentialProviderPlayServicesImpl.cancellationReviewer(cancellationSignal)) {
            return
        }
        val hiddenIntent = Intent(activity, HiddenActivity::class.java)
        hiddenIntent.putExtra(REQUEST_TAG, fidoRegistrationRequest)
        generateHiddenActivityIntent(resultReceiver, hiddenIntent,
            CREATE_PUBLIC_KEY_CREDENTIAL_TAG)
        activity.startActivity(hiddenIntent)
    }

    internal fun handleResponse(uniqueRequestCode: Int, resultCode: Int, data: Intent?) {
        if (uniqueRequestCode != CONTROLLER_REQUEST_CODE) {
            return
        }
        if (maybeReportErrorResultCodeCreate(resultCode, TAG,
                { e, s, f -> cancelAndCallbackException(e, s, f) }, { e -> this.executor.execute {
                    this.callback.onError(e) } }, cancellationSignal)) return
        val bytes: ByteArray? = data?.getByteArrayExtra(Fido.FIDO2_KEY_CREDENTIAL_EXTRA)
        if (bytes == null) {
            if (CredentialProviderPlayServicesImpl.cancellationReviewer(cancellationSignal)) {
                return
            }
            this.executor.execute { this.callback.onError(
                CreatePublicKeyCredentialUnknownException(
                "Internal error fido module giving null bytes")
            ) }
            return
        }
        val cred: PublicKeyCredential = PublicKeyCredential.deserializeFromBytes(bytes)
        val exception =
            PublicKeyCredentialControllerUtility.publicKeyCredentialResponseContainsError(cred)
        if (exception != null) {
            cancelAndCallbackException(exception, cancellationSignal) { e ->
                this.executor.execute { this.callback.onError(e) } }
            return
        }
        try {
            val response = this.convertResponseToCredentialManager(cred)
            cancelAndCallbackResult(response, cancellationSignal) { e -> this.executor.execute {
                this.callback.onResult(e) } }
        } catch (e: JSONException) {
            cancelAndCallbackException(CreatePublicKeyCredentialEncodingException(e.message),
                cancellationSignal) { ex -> this.executor.execute { this.callback.onError(ex) } }
        } catch (t: Throwable) {
            cancelAndCallbackException(CreatePublicKeyCredentialUnknownException(t.message),
                cancellationSignal) { e -> this.executor.execute { this.callback.onError(e) } }
        }
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
    public override fun convertRequestToPlayServices(request: CreatePublicKeyCredentialRequest):
        PublicKeyCredentialCreationOptions {
        return PublicKeyCredentialControllerUtility.convert(request)
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
    public override fun convertResponseToCredentialManager(response: PublicKeyCredential):
        CreateCredentialResponse {
        return CreatePublicKeyCredentialResponse(PublicKeyCredentialControllerUtility
            .toCreatePasskeyResponseJson(response))
    }

    companion object {
        private val TAG = CredentialProviderCreatePublicKeyCredentialController::class.java.name
        private var controller: CredentialProviderCreatePublicKeyCredentialController? = null
        // TODO("Ensure this is tested for multiple calls")

        /**
         * This finds a past version of the
         * [CredentialProviderCreatePublicKeyCredentialController] if it exists, otherwise
         * it generates a new instance.
         *
         * @param activity the calling activity for this controller
         * @return a credential provider controller for CreatePublicKeyCredential
         */
        @JvmStatic
        fun getInstance(activity: Activity):
            CredentialProviderCreatePublicKeyCredentialController {
            if (controller == null) {
                controller = CredentialProviderCreatePublicKeyCredentialController(activity)
            }
            return controller!!
        }
    }
}