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

package androidx.appfunctions

import android.os.Bundle
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class AppFunctionAppExceptionsTest {
    @Test
    fun testErrorCategory_AppError() {
        assertThat(AppFunctionAppUnknownException(null, Bundle()).internalErrorCode)
            .isEqualTo(AppFunctionException.ERROR_APP_UNKNOWN_ERROR)
        assertThat(AppFunctionAppUnknownException(null, Bundle()).errorCategory)
            .isEqualTo(AppFunctionException.ERROR_CATEGORY_APP)

        assertThat(AppFunctionPermissionRequiredException(null, Bundle()).internalErrorCode)
            .isEqualTo(AppFunctionException.ERROR_PERMISSION_REQUIRED)
        assertThat(AppFunctionPermissionRequiredException(null, Bundle()).errorCategory)
            .isEqualTo(AppFunctionException.ERROR_CATEGORY_APP)

        assertThat(AppFunctionNotSupportedException(null, Bundle()).internalErrorCode)
            .isEqualTo(AppFunctionException.ERROR_NOT_SUPPORTED)
        assertThat(AppFunctionNotSupportedException(null, Bundle()).errorCategory)
            .isEqualTo(AppFunctionException.ERROR_CATEGORY_APP)
    }
}
