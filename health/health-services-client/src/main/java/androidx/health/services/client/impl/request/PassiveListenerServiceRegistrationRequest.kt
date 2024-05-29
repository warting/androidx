/*
 * Copyright (C) 2022 The Android Open Source Project
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

package androidx.health.services.client.impl.request

import android.os.Parcelable
import androidx.annotation.RestrictTo
import androidx.health.services.client.data.PassiveListenerConfig
import androidx.health.services.client.data.ProtoParcelable
import androidx.health.services.client.proto.RequestsProto

/** Request for background registration. */
@RestrictTo(RestrictTo.Scope.LIBRARY)
internal class PassiveListenerServiceRegistrationRequest(
    public val packageName: String,
    public val passiveListenerServiceClassName: String,
    public val passiveListenerConfig: PassiveListenerConfig,
) : ProtoParcelable<RequestsProto.PassiveListenerServiceRegistrationRequest>() {

    override val proto: RequestsProto.PassiveListenerServiceRegistrationRequest =
        RequestsProto.PassiveListenerServiceRegistrationRequest.newBuilder()
            .setPackageName(packageName)
            .setListenerServiceClass(passiveListenerServiceClassName)
            .setConfig(passiveListenerConfig.proto)
            .build()

    public companion object {
        @JvmField
        public val CREATOR: Parcelable.Creator<PassiveListenerServiceRegistrationRequest> =
            newCreator { bytes ->
                val proto = RequestsProto.PassiveListenerServiceRegistrationRequest.parseFrom(bytes)
                PassiveListenerServiceRegistrationRequest(
                    proto.packageName,
                    proto.listenerServiceClass,
                    PassiveListenerConfig(proto.config)
                )
            }
    }
}
