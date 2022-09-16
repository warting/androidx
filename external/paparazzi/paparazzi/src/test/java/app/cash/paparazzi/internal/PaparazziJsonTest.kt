/*
 * Copyright (C) 2019 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package app.cash.paparazzi.internal

import app.cash.paparazzi.TestName
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class PaparazziJsonTest {
  @Test
  fun testName() {
    val adapter = PaparazziJson.moshi.adapter(TestName::class.java)
    val testName = TestName("app.cash.paparazzi", "CelebrityTest", "testSettings")
    val json = "\"app.cash.paparazzi.CelebrityTest#testSettings\""
    assertThat(adapter.toJson(testName)).isEqualTo(json)
    assertThat(adapter.fromJson(json)).isEqualTo(testName)
  }
}
