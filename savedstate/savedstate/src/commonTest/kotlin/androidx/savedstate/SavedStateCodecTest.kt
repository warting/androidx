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

package androidx.savedstate

import androidx.kruth.assertThat
import androidx.kruth.assertThrows
import androidx.savedstate.SavedStateCodecTestUtils.encodeDecode
import androidx.savedstate.serialization.decodeFromSavedState
import androidx.savedstate.serialization.encodeToSavedState
import kotlin.jvm.JvmInline
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.serializer

@ExperimentalSerializationApi
internal class SavedStateCodecTest : RobolectricTest() {
    @Test
    fun primitives() {
        Byte.MIN_VALUE.encodeDecode {
            assertThat(size()).isEqualTo(1)
            assertThat(getInt("")).isEqualTo(Byte.MIN_VALUE.toInt())
        }
        Byte.MAX_VALUE.encodeDecode {
            assertThat(size()).isEqualTo(1)
            assertThat(getInt("")).isEqualTo(Byte.MAX_VALUE.toInt())
        }
        Short.MIN_VALUE.encodeDecode {
            assertThat(size()).isEqualTo(1)
            assertThat(getInt("")).isEqualTo(Short.MIN_VALUE.toInt())
        }
        Short.MAX_VALUE.encodeDecode {
            assertThat(size()).isEqualTo(1)
            assertThat(getInt("")).isEqualTo(Short.MAX_VALUE.toInt())
        }
        Int.MIN_VALUE.encodeDecode {
            assertThat(size()).isEqualTo(1)
            assertThat(getInt("")).isEqualTo(Int.MIN_VALUE)
        }
        Int.MAX_VALUE.encodeDecode {
            assertThat(size()).isEqualTo(1)
            assertThat(getInt("")).isEqualTo(Int.MAX_VALUE)
        }
        Long.MIN_VALUE.encodeDecode {
            assertThat(size()).isEqualTo(1)
            assertThat(source)
            assertThat(getLong("")).isEqualTo(Long.MIN_VALUE)
        }
        Long.MAX_VALUE.encodeDecode {
            assertThat(size()).isEqualTo(1)
            assertThat(getLong("")).isEqualTo(Long.MAX_VALUE)
        }
        Float.MIN_VALUE.encodeDecode {
            assertThat(size()).isEqualTo(1)
            assertThat(getFloat("")).isEqualTo(Float.MIN_VALUE)
        }
        Float.MAX_VALUE.encodeDecode {
            assertThat(size()).isEqualTo(1)
            assertThat(getFloat("")).isEqualTo(Float.MAX_VALUE)
        }
        Double.MIN_VALUE.encodeDecode {
            assertThat(size()).isEqualTo(1)
            assertThat(getDouble("")).isEqualTo(Double.MIN_VALUE)
        }
        Double.MAX_VALUE.encodeDecode {
            assertThat(size()).isEqualTo(1)
            assertThat(getDouble("")).isEqualTo(Double.MAX_VALUE)
        }
        Char.MIN_VALUE.encodeDecode {
            assertThat(size()).isEqualTo(1)
            assertThat(getChar("")).isEqualTo(Char.MIN_VALUE)
        }
        Char.MAX_VALUE.encodeDecode {
            assertThat(size()).isEqualTo(1)
            assertThat(getChar("")).isEqualTo(Char.MAX_VALUE)
        }
        false.encodeDecode {
            assertThat(size()).isEqualTo(1)
            assertThat(getBoolean("")).isEqualTo(false)
        }
        true.encodeDecode {
            assertThat(size()).isEqualTo(1)
            assertThat(getBoolean("")).isEqualTo(true)
        }
        ""
            .encodeDecode {
                assertThat(size()).isEqualTo(1)
                assertThat(getString("")).isEqualTo("")
            }
        "foo"
            .encodeDecode {
                assertThat(size()).isEqualTo(1)
                assertThat(getString("")).isEqualTo("foo")
            }
        MyEnum.A.encodeDecode {
            assertThat(size()).isEqualTo(1)
            assertThat(getInt("")).isEqualTo(0)
        }
        MyEnum.B.encodeDecode {
            assertThat(size()).isEqualTo(1)
            assertThat(getInt("")).isEqualTo(1)
        }
        MyEnum.C.encodeDecode {
            assertThat(size()).isEqualTo(1)
            assertThat(getInt("")).isEqualTo(2)
        }
    }

    @Test
    fun valueClasses() {
        UByte.MIN_VALUE.encodeDecode {
            assertThat(size()).isEqualTo(1)
            assertThat(getInt("")).isEqualTo(0)
        }
        UByte.MAX_VALUE.encodeDecode {
            assertThat(size()).isEqualTo(1)
            assertThat(getInt("")).isEqualTo(-1)
        }
        UShort.MIN_VALUE.encodeDecode {
            assertThat(size()).isEqualTo(1)
            assertThat(getInt("")).isEqualTo(0)
        }
        UShort.MAX_VALUE.encodeDecode {
            assertThat(size()).isEqualTo(1)
            assertThat(getInt("")).isEqualTo(-1)
        }
        UInt.MIN_VALUE.encodeDecode {
            assertThat(size()).isEqualTo(1)
            assertThat(getInt("")).isEqualTo(0)
        }
        UInt.MAX_VALUE.encodeDecode {
            assertThat(size()).isEqualTo(1)
            assertThat(getInt("")).isEqualTo(-1)
        }
        ULong.MIN_VALUE.encodeDecode {
            assertThat(size()).isEqualTo(1)
            assertThat(getLong("")).isEqualTo(0L)
        }
        ULong.MAX_VALUE.encodeDecode {
            assertThat(size()).isEqualTo(1)
            assertThat(getLong("")).isEqualTo(-1L)
        }
        MyValueClassToString("foo").encodeDecode {
            assertThat(size()).isEqualTo(1)
            assertThat(getString("")).isEqualTo("foo")
        }
    }

    @Test
    fun builtInComposites() {
        Pair(3, "foo").encodeDecode {
            assertThat(size()).isEqualTo(2)
            assertThat(getInt("first")).isEqualTo(3)
            assertThat(getString("second")).isEqualTo("foo")
        }
        Triple(3, "foo", 3.14).encodeDecode {
            assertThat(size()).isEqualTo(3)
            assertThat(getInt("first")).isEqualTo(3)
            assertThat(getString("second")).isEqualTo("foo")
            assertThat(getDouble("third")).isEqualTo(3.14)
        }
        Duration.ZERO.encodeDecode {
            assertThat(size()).isEqualTo(1)
            assertThat(getString("")).isEqualTo(Duration.ZERO.toIsoString())
        }
        Duration.INFINITE.encodeDecode {
            assertThat(size()).isEqualTo(1)
            assertThat(getString("")).isEqualTo(Duration.INFINITE.toIsoString())
        }
        Unit.encodeDecode()
        MyObject.encodeDecode()
    }

    @Test
    fun list() {
        emptyList<Int>().encodeDecode()

        listOf(1, 2, 3).encodeDecode {
            assertThat(size()).isEqualTo(3)
            assertThat(getInt("0")).isEqualTo(1)
            assertThat(getInt("1")).isEqualTo(2)
            assertThat(getInt("2")).isEqualTo(3)
        }

        listOf(1, 2, null, 4, 5, null).encodeDecode {
            assertThat(size()).isEqualTo(6)
            assertThat(getInt("0")).isEqualTo(1)
            assertThat(getInt("1")).isEqualTo(2)
            assertThat(isNull("2")).isTrue()
            assertThat(getInt("3")).isEqualTo(4)
            assertThat(getInt("4")).isEqualTo(5)
            assertThat(isNull("5")).isTrue()
        }

        // List of list.
        listOf(listOf(1, 2), listOf(3, 4)).encodeDecode {
            assertThat(size()).isEqualTo(2)
            getSavedState("0").read {
                assertThat(size()).isEqualTo(2)
                assertThat(getInt("0")).isEqualTo(1)
                assertThat(getInt("1")).isEqualTo(2)
            }
            getSavedState("1").read {
                assertThat(size()).isEqualTo(2)
                assertThat(getInt("0")).isEqualTo(3)
                assertThat(getInt("1")).isEqualTo(4)
            }
        }

        // List of list of list.
        listOf(listOf(emptyList(), listOf(1, 2)), listOf(listOf(3, 4))).encodeDecode {
            assertThat(size()).isEqualTo(2)
            getSavedState("0").read {
                assertThat(size()).isEqualTo(2)
                getSavedState("0").read { assertThat(size()).isEqualTo(0) }
                getSavedState("1").read {
                    assertThat(size()).isEqualTo(2)
                    assertThat(getInt("0")).isEqualTo(1)
                    assertThat(getInt("1")).isEqualTo(2)
                }
            }
            getSavedState("1").read {
                assertThat(size()).isEqualTo(1)
                getSavedState("0").read {
                    assertThat(size()).isEqualTo(2)
                    assertThat(getInt("0")).isEqualTo(3)
                    assertThat(getInt("1")).isEqualTo(4)
                }
            }
        }

        // List in class in another class.
        @Serializable data class MyComponent(val list: List<String>)
        @Serializable data class MyContainer(val myComponent: MyComponent)
        MyContainer(MyComponent(listOf("foo", "bar"))).encodeDecode {
            assertThat(size()).isEqualTo(1)
            getSavedState("myComponent").read {
                assertThat(size()).isEqualTo(1)
                getSavedState("list").read {
                    assertThat(size()).isEqualTo(2)
                    assertThat(getString("0")).isEqualTo("foo")
                    assertThat(getString("1")).isEqualTo("bar")
                }
            }
        }

        // Custom list is not a list.
        val myDelegatedList = MyDelegatedList(arrayListOf(1, 3, 5))
        myDelegatedList.encodeDecode(serializer<MyDelegatedList<Int>>()) {
            assertThat(size()).isEqualTo(1)
            getSavedState("values").read {
                assertThat(size()).isEqualTo(3)
                assertThat(getInt("0")).isEqualTo(1)
                assertThat(getInt("1")).isEqualTo(3)
                assertThat(getInt("2")).isEqualTo(5)
            }
        }
        myDelegatedList.encodeDecode(serializer<List<Int>>()) {
            assertThat(size()).isEqualTo(3)
            assertThat(getInt("0")).isEqualTo(1)
            assertThat(getInt("1")).isEqualTo(3)
            assertThat(getInt("2")).isEqualTo(5)
        }
    }

    @Test
    fun sets() {
        val list = (0..99).toList()

        // Sets from `setOf()` are ordered.
        setOf(*list.toTypedArray()).encodeDecode {
            assertThat(size()).isEqualTo(100)
            list.forEach { index -> assertThat(getInt(index.toString())).isEqualTo(index) }
        }

        // Sets from `hashSetOf()` are NOT ordered.
        hashSetOf(*list.toTypedArray()).encodeDecode {
            assertThat(size()).isEqualTo(100)
            val values = buildList { list.forEach { index -> add(getInt(index.toString())) } }
            assertThat(values.sorted()).isEqualTo(list)
        }

        // Duplicates are ignored
        assertThat(
                decodeFromSavedState<Set<Int>>(
                    savedState {
                        putInt("0", 1)
                        putInt("1", 3)
                        putInt("2", 3)
                    }
                )
            )
            .isEqualTo(setOf(1, 3))
    }

    @Test
    fun map() {
        emptyMap<Int, String>().encodeDecode()
        mapOf<Int, String>(123 to "foo", 456 to "bar").encodeDecode {
            assertThat(size()).isEqualTo(4)
            assertThat(getInt("0")).isEqualTo(123)
            assertThat(getString("1")).isEqualTo("foo")
            assertThat(getInt("2")).isEqualTo(456)
            assertThat(getString("3")).isEqualTo("bar")
        }
        mapOf<Int?, String?>(123 to null, null to "bar").encodeDecode {
            assertThat(size()).isEqualTo(4)
            assertThat(getInt("0")).isEqualTo(123)
            assertThat(isNull("1")).isTrue()
            assertThat(isNull("2")).isTrue()
            assertThat(getString("3")).isEqualTo("bar")
        }
    }

    @Test
    fun recursiveTypes() {
        MyTreeNode(3, MyTreeNode(5), MyTreeNode(7)).encodeDecode {
            assertThat(size()).isEqualTo(3)
            assertThat(getInt("value")).isEqualTo(3)
            getSavedState("left").read {
                assertThat(size()).isEqualTo(1)
                assertThat(getInt("value")).isEqualTo(5)
            }
            getSavedState("right").read {
                assertThat(size()).isEqualTo(1)
                assertThat(getInt("value")).isEqualTo(7)
            }
        }
    }

    @Test
    fun typeAliases() {
        MyTypeAliasToInt.MIN_VALUE.encodeDecode {
            assertThat(size()).isEqualTo(1)
            assertThat(getInt("")).isEqualTo(Int.MIN_VALUE)
        }
        MyNestedTypeAlias.MIN_VALUE.encodeDecode {
            assertThat(size()).isEqualTo(1)
            assertThat(getInt("")).isEqualTo(Int.MIN_VALUE)
        }
    }

    @Test
    fun sealedClasses() {
        Node.Add(Node.Operand(3), Node.Operand(5)).encodeDecode {
            assertThat(size()).isEqualTo(2)
            getSavedState("lhs").read {
                assertThat(size()).isEqualTo(1)
                assertThat(getInt("value")).isEqualTo(3)
            }
            getSavedState("rhs").read {
                assertThat(size()).isEqualTo(1)
                assertThat(getInt("value")).isEqualTo(5)
            }
        }
    }

    @Test
    fun typesWithDefaultValuesAndNullables() {
        @Serializable data class A(val i: Int = 3)
        // We don't encode default values by default.
        A().encodeDecode()
        A(i = 5).encodeDecode {
            assertThat(size()).isEqualTo(1)
            assertThat(getInt("i")).isEqualTo(5)
        }

        // Nullable with default value.
        @Serializable data class B(val s: String? = "foo")
        B().encodeDecode()
        B(s = "bar").encodeDecode {
            assertThat(size()).isEqualTo(1)
            assertThat(getString("s")).isEqualTo("bar")
        }

        // Nullable without default value
        @Serializable data class C(val s: String?)
        C(s = "bar").encodeDecode {
            assertThat(size()).isEqualTo(1)
            assertThat(getString("s")).isEqualTo("bar")
        }

        // Default value is encoded with `@EncodeDefault`.
        @Serializable
        data class D(
            val i: Int = 3,
            @EncodeDefault(EncodeDefault.Mode.ALWAYS) val s: String? = "foo"
        )
        D(i = 5).encodeDecode {
            assertThat(size()).isEqualTo(2)
            assertThat(getInt("i")).isEqualTo(5)
            assertThat(getString("s")).isEqualTo("foo")
        }
    }

    @Test
    fun serialName() {
        @Serializable data class MyModel(@SerialName("foo") val i: Int)
        MyModel(3).encodeDecode {
            assertThat(size()).isEqualTo(1)
            assertThat(getInt("foo")).isEqualTo(3)
        }
    }

    // Users shouldn't do this. The test is just to document the behavior.
    @Test
    fun typeMismatchInDecodingWorks() {
        assertThat(decodeFromSavedState<Int>(savedState { putBoolean("", true) }))
            .isEqualTo(0) // This is the default value from `SavedStateReader.getInt(String)`.
        assertThrows(IllegalStateException::class) {
                decodeFromSavedState<String>(savedState { putBoolean("", true) })
            }
            .hasMessageThat()
            .contains(
                "The saved state value associated with the key '' is either null or not of the expected type. This might happen if the value was saved with a different type or if the saved state has been modified unexpectedly."
            )
        @Serializable data class Foo(val i: Int)
        @Serializable data class Bar(val i: Int)
        assertEquals(Bar(3), decodeFromSavedState<Bar>(encodeToSavedState(Foo(3))))
    }

    @Test
    fun decodeMissingKey() {
        assertThrows(IllegalArgumentException::class) { decodeFromSavedState<Int>(savedState()) }
            .hasMessageThat()
            .contains("No saved state was found associated with the key ''")
    }

    // This is not ideal. The test is just to document the behavior.
    @Test
    fun illegalWrite() {
        val savedState = encodeToSavedState(3)
        savedState.write { putString("", "foo") }
        // Got the default value of Int instead of 3 because the savedState got manipulated after
        // encoding.
        assertThat(decodeFromSavedState<Int>(savedState)).isEqualTo(0)
    }
}

@Serializable
data class MyTreeNode<T>(
    val value: T,
    val left: MyTreeNode<T>? = null,
    val right: MyTreeNode<T>? = null
)

// `@Serializable` is needed for using the enum as root in native and js.
@Serializable
enum class MyEnum {
    A,
    B,
    C
}

@Serializable @JvmInline private value class MyValueClassToString(val value: String)

private typealias MyTypeAliasToInt = Int

private typealias MyNestedTypeAlias = MyTypeAliasToInt

private sealed class Node {
    @Serializable data class Add(val lhs: Operand, val rhs: Operand) : Node()

    @Serializable data class Operand(val value: Int) : Node()
}

@Serializable
private class MyDelegatedList<E>(val values: ArrayList<E>) : MutableList<E> by values {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        other as MyDelegatedList<*>
        return values == other.values
    }

    override fun hashCode(): Int {
        return values.hashCode()
    }
}

@Serializable
object MyObject {
    val foo = "bar"
}
