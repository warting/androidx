/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.serialization.compiler.codegen.java

import androidx.serialization.EnumValue
import androidx.serialization.compiler.schema.Enum
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.JavaFile
import com.squareup.javapoet.TypeName
import java.lang.IllegalArgumentException
import javax.lang.model.element.Modifier.FINAL
import javax.lang.model.element.Modifier.PUBLIC
import javax.lang.model.element.Modifier.STATIC

/** Generate an enum serializer implementation. */
internal fun JavaGenerator.enumSerializer(enum: Enum): JavaFile {
    val enumClass = ClassName.get(enum.element)
    val serializer = serializerName(enumClass)

    val values = enum.values.sortedBy { it.id }

    return buildClass(serializer, enum.element, PUBLIC, FINAL) {
        addSuperinterface(ENUM_SERIALIZER.parameterized(enumClass))

        field(serializer.nonNull, "INSTANCE", PUBLIC, STATIC, FINAL) {
            initializer("new \$T()", serializer)
        }

        overrideMethod("encode", PUBLIC) {
            addParameter(enumClass.nonNull, "value")
            returns(TypeName.INT)

            controlFlow("switch (value)") {
                for (value in values) {
                    switchCase("\$N", value.element.simpleName) {
                        addStatement("return \$L", value.id)
                    }
                }

                switchDefault {
                    addStatement(
                        "throw new \$T(\"Enum value \" + value.toString() +\$W" +
                                "\" does not have a serialization ID.\")",
                        IllegalArgumentException::class.java
                    )
                }
            }
        }

        overrideMethod("decode", PUBLIC) {
            addParameter(TypeName.INT, "value")
            returns(enumClass.nonNull)

            controlFlow("switch (value)") {
                for (value in values.filterNot { it.id == EnumValue.DEFAULT }) {
                    switchCase("\$L", value.id) {
                        addStatement("return \$T.\$N", enumClass, value.element.simpleName)
                    }
                }

                switchDefault {
                    val value = values.first { it.id == EnumValue.DEFAULT }
                    addStatement("return \$T.\$N", enumClass, value.element.simpleName)
                }
            }
        }
    }
}
