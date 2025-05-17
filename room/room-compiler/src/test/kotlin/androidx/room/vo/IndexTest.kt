/*
 * Copyright (C) 2017 The Android Open Source Project
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

package androidx.room.vo

import androidx.room.parser.SQLTypeAffinity
import mockElementAndType
import org.hamcrest.CoreMatchers
import org.hamcrest.MatcherAssert
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

private typealias IndexOrder = androidx.room.Index.Order

@RunWith(JUnit4::class)
class IndexTest {
    @Test
    fun createSimpleSQL() {
        val index = Index("foo", false, listOf(mockField("bar"), mockField("baz")), emptyList())
        MatcherAssert.assertThat(
            index.createQuery("my_table"),
            CoreMatchers.`is`("CREATE INDEX IF NOT EXISTS `foo` ON `my_table` (`bar`, `baz`)"),
        )
    }

    @Test
    fun createUnique() {
        val index = Index("foo", true, listOf(mockField("bar"), mockField("baz")), emptyList())
        MatcherAssert.assertThat(
            index.createQuery("my_table"),
            CoreMatchers.`is`(
                "CREATE UNIQUE INDEX IF NOT EXISTS `foo` ON `my_table` (`bar`, `baz`)"
            ),
        )
    }

    @Test
    fun createWithSortOrder() {
        val index =
            Index(
                name = "foo",
                unique = false,
                fields = listOf(mockField("bar"), mockField("baz")),
                orders = listOf(IndexOrder.ASC, IndexOrder.DESC),
            )
        MatcherAssert.assertThat(
            index.createQuery("my_table"),
            CoreMatchers.`is`(
                "CREATE INDEX IF NOT EXISTS `foo` ON `my_table` (`bar` ASC, `baz` DESC)"
            ),
        )
    }

    private fun mockField(columnName: String): Property {
        val (element, type) = mockElementAndType()
        return Property(
            element = element,
            name = columnName + "_field",
            affinity = SQLTypeAffinity.TEXT,
            type = type,
            columnName = columnName,
        )
    }
}
