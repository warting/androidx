/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.credentials.providerevents.playservices

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.SigningInfo
import android.os.CancellationSignal
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo
import androidx.core.os.OutcomeReceiverCompat
import androidx.credentials.CreateCredentialResponse
import androidx.credentials.exceptions.CreateCredentialException
import androidx.credentials.provider.CallingAppInfo
import androidx.credentials.providerevents.CredentialEventsProvider
import androidx.credentials.providerevents.service.CredentialProviderEventsService
import com.google.android.gms.common.wrappers.Wrappers
import com.google.android.gms.identitycredentials.CallingAppInfoParcelable
import com.google.android.gms.identitycredentials.CreateCredentialRequest
import com.google.android.gms.identitycredentials.provider.ICreateCredentialCallbacks
import com.google.android.gms.identitycredentials.provider.ICredentialProviderService
import java.lang.ref.WeakReference

@RestrictTo(RestrictTo.Scope.LIBRARY)
@RequiresApi(28)
public class CredentialEventsProviderPlayServices() : CredentialEventsProvider {

    override fun getStubImplementation(service: CredentialProviderEventsService): IBinder? {
        val binderInterface = ServiceWrapper(service, Handler(Looper.getMainLooper()))
        return binderInterface.asBinder()
    }

    private class ServiceWrapper(service: CredentialProviderEventsService, val handler: Handler) :
        ICredentialProviderService.Stub() {

        private fun convertToJetpackRequest(
            request: CreateCredentialRequest
        ): androidx.credentials.CreateCredentialRequest {
            return androidx.credentials.CreateCredentialRequest.Companion.createFrom(
                request.type,
                request.credentialData,
                request.candidateQueryData,
                false
            )
        }

        private fun convertToGmsResponse(
            response: CreateCredentialResponse?
        ): com.google.android.gms.identitycredentials.CreateCredentialResponse? {
            if (response != null) {
                return com.google.android.gms.identitycredentials.CreateCredentialResponse(
                    response.type,
                    response.data
                )
            }
            return response
        }

        private val context: Context = service.applicationContext

        var serviceRef: WeakReference<CredentialProviderEventsService> = WeakReference(service)

        @Suppress("RestrictedApiAndroidX")
        override fun onCreateCredentialRequest(
            request: CreateCredentialRequest,
            callingAppInfo: CallingAppInfoParcelable,
            callback: ICreateCredentialCallbacks
        ) {
            if (!isAuthorizedUid(getCallingUid())) {
                return
            }

            val jetpackRequest: androidx.credentials.CreateCredentialRequest =
                convertToJetpackRequest(request)
            // TODO(b/385394695): Fix being able to create CallingAppInfo with list of signatures
            val jetpackCallingAppInfo =
                CallingAppInfo.create(
                    callingAppInfo.packageName,
                    SigningInfo(),
                    callingAppInfo.origin
                )

            handler.post {
                val service = serviceRef.get()
                if (service == null) {
                    return@post
                }

                service.onCreateCredentialRequest(
                    jetpackRequest,
                    jetpackCallingAppInfo,
                    CancellationSignal(),
                    object :
                        OutcomeReceiverCompat<CreateCredentialResponse, CreateCredentialException> {
                        override fun onResult(result: CreateCredentialResponse?) {
                            val response = convertToGmsResponse(result)
                            if (response != null) {
                                // TODO(b/385394695): Remove place holder pending intent affter
                                //  exposing a callback method that does not need pending intent
                                val placeHolderPendingIntent =
                                    PendingIntent.getService(
                                        context,
                                        0,
                                        Intent(),
                                        PendingIntent.FLAG_IMMUTABLE
                                    )
                                callback.onSuccessV2(response, placeHolderPendingIntent)
                            } else {
                                callback.onFailure(
                                    com.google.android.gms.identitycredentials
                                        .CreateCredentialException
                                        .ERROR_TYPE_UNKNOWN,
                                    "Response could not be constructed"
                                )
                            }
                        }

                        override fun onError(error: CreateCredentialException) {
                            callback.onFailure(error.type, error.message.toString())
                        }
                    },
                )
            }
        }

        private fun isAuthorizedUid(callingUid: Int): Boolean {
            val packages = getPackageNameList(callingUid)
            for (pkg in packages) {
                if (pkg == GMS_PACKAGE_NAME) {
                    return true
                }
            }
            return false
        }

        fun getPackageNameList(callingUid: Int): List<String> {
            val packageNameList = mutableListOf<String>()
            val packageManager = Wrappers.packageManager(context)
            val packagesForUid: Array<String>? = packageManager.getPackagesForUid(callingUid)
            if (packagesForUid == null) {
                return packageNameList.toList()
            }

            for (i in packagesForUid.indices) {
                val pkg = packagesForUid[i]
                packageNameList.add(pkg)
            }
            return packageNameList
        }

        companion object {
            const val GMS_PACKAGE_NAME: String = "com.google.android.gms"
        }
    }
}
