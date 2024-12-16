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

package androidx.privacysandbox.ads.adservices.adid

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresExtension
import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY)
@SuppressLint("NewApi")
@RequiresExtension(extension = Build.VERSION_CODES.S, version = 9)
class AdIdManagerApi31Ext9Impl(context: Context) :
    AdIdManagerImplCommon(android.adservices.adid.AdIdManager.get(context))
