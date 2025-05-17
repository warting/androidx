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

package androidx.credentials.webauthn

import androidx.annotation.RestrictTo
import org.json.JSONObject

@RestrictTo(RestrictTo.Scope.LIBRARY)
class FidoPublicKeyCredential(
    val rawId: ByteArray,
    val response: AuthenticatorResponse,
    val authenticatorAttachment: String,
) {

    fun json(): String {
        // See RegistrationResponseJSON at
        // https://w3c.github.io/webauthn/#ref-for-dom-publickeycredential-tojson
        val encodedId = WebAuthnUtils.b64Encode(rawId)
        val ret = JSONObject()
        ret.put("id", encodedId)
        ret.put("rawId", encodedId)
        ret.put("type", "public-key")
        ret.put("authenticatorAttachment", authenticatorAttachment)
        ret.put("response", response.json())
        ret.put("clientExtensionResults", JSONObject())

        return ret.toString()
    }
}
