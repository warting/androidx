/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.credentials.registry.digitalcredentials.mdoc

import androidx.credentials.registry.provider.digitalcredentials.VerificationFieldDisplayProperties
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class MdocFieldTest {
    companion object {
        val FIELD_DISPLAY_DATA = VerificationFieldDisplayProperties("displayName1")
    }

    @Test
    fun construction_success() {
        val field = MdocField("org.iso.18013.5.1.age_over_21", true, setOf(FIELD_DISPLAY_DATA))

        assertThat(field.fieldName).isEqualTo("org.iso.18013.5.1.age_over_21")
        assertThat((field.fieldValue) as Boolean).isTrue()
        assertThat(field.fieldDisplayPropertySet).containsExactly(FIELD_DISPLAY_DATA)
    }
}
