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

package androidx.appfunctions.integration.testapp.library

import android.net.Uri
import android.util.Log
import androidx.appfunctions.AppFunctionContext
import androidx.appfunctions.service.AppFunction

@Suppress("UNUSED_PARAMETER")
class TestFunctions2 {
    /**
     * Concatenates the two given strings.
     *
     * @param str1 The first string.
     * @param str2 The second string.
     * @return The result of concatenating the two strings.
     */
    @AppFunction(isDescribedByKdoc = true)
    fun concat(appFunctionContext: AppFunctionContext, str1: String, str2: String) = str1 + str2

    @AppFunction
    fun logUri(appFunctionContext: AppFunctionContext, androidUri: Uri) {
        Log.d("TestFunctions2", "URI: $androidUri")
    }

    @AppFunction
    fun getUri(appFunctionContext: AppFunctionContext): Uri {
        return Uri.parse("https://www.google.com/")
    }
}
