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
package androidx.health.data.client.impl.converters.records

import androidx.health.platform.client.proto.DataProto
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.lang.RuntimeException
import kotlin.test.assertFailsWith
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RecordsTest {
    @Test
    fun unknownRecordType() {
        val proto =
            DataProto.DataPoint.newBuilder()
                .setDataType(DataProto.DataType.newBuilder().setName("UnknownName").build())
                .build()

        assertFailsWith<RuntimeException> { toRecord(proto) }
    }
}
