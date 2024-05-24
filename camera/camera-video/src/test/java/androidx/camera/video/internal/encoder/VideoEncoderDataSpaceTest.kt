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

package androidx.camera.video.internal.encoder

import android.media.MediaFormat
import android.os.Build
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

private const val TEST_COLOR_STANDARD = MediaFormat.COLOR_STANDARD_BT2020
private const val TEST_TRANSFER_FN = MediaFormat.COLOR_TRANSFER_HLG
private const val TEST_COLOR_RANGE = MediaFormat.COLOR_RANGE_LIMITED

@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
class VideoEncoderDataSpaceTest {

    @Test
    fun canRetrieveFields() {
        val dataSpace =
            VideoEncoderDataSpace.create(TEST_COLOR_STANDARD, TEST_TRANSFER_FN, TEST_COLOR_RANGE)

        assertThat(dataSpace.standard).isEqualTo(TEST_COLOR_STANDARD)
        assertThat(dataSpace.transfer).isEqualTo(TEST_TRANSFER_FN)
        assertThat(dataSpace.range).isEqualTo(TEST_COLOR_RANGE)
    }
}
