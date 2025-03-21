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

package androidx.appfunctions.compiler.processors

import androidx.appfunctions.AppFunctionData
import androidx.appfunctions.compiler.core.AnnotatedAppFunctionSerializable
import androidx.appfunctions.compiler.core.AnnotatedAppFunctionSerializableProxy
import androidx.appfunctions.compiler.core.AnnotatedAppFunctionSerializableProxy.ResolvedAnnotatedSerializableProxies
import androidx.appfunctions.compiler.core.AppFunctionPropertyDeclaration
import androidx.appfunctions.compiler.core.AppFunctionTypeReference
import androidx.appfunctions.compiler.core.AppFunctionTypeReference.AppFunctionSupportedTypeCategory.PRIMITIVE_ARRAY
import androidx.appfunctions.compiler.core.AppFunctionTypeReference.AppFunctionSupportedTypeCategory.PRIMITIVE_LIST
import androidx.appfunctions.compiler.core.AppFunctionTypeReference.AppFunctionSupportedTypeCategory.PRIMITIVE_SINGULAR
import androidx.appfunctions.compiler.core.AppFunctionTypeReference.AppFunctionSupportedTypeCategory.SERIALIZABLE_LIST
import androidx.appfunctions.compiler.core.AppFunctionTypeReference.AppFunctionSupportedTypeCategory.SERIALIZABLE_PROXY_LIST
import androidx.appfunctions.compiler.core.AppFunctionTypeReference.AppFunctionSupportedTypeCategory.SERIALIZABLE_PROXY_SINGULAR
import androidx.appfunctions.compiler.core.AppFunctionTypeReference.AppFunctionSupportedTypeCategory.SERIALIZABLE_SINGULAR
import androidx.appfunctions.compiler.core.IntrospectionHelper.AppFunctionSerializableFactoryClass.FromAppFunctionDataMethod
import androidx.appfunctions.compiler.core.IntrospectionHelper.AppFunctionSerializableFactoryClass.FromAppFunctionDataMethod.APP_FUNCTION_DATA_PARAM_NAME
import androidx.appfunctions.compiler.core.IntrospectionHelper.AppFunctionSerializableFactoryClass.ToAppFunctionDataMethod.APP_FUNCTION_SERIALIZABLE_PARAM_NAME
import androidx.appfunctions.compiler.core.ProcessingException
import androidx.appfunctions.compiler.core.ensureQualifiedTypeName
import androidx.appfunctions.compiler.core.isOfType
import androidx.appfunctions.compiler.core.toTypeName
import com.google.devtools.ksp.symbol.KSTypeReference
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.STRING
import com.squareup.kotlinpoet.buildCodeBlock

/**
 * Wraps methods to build the [CodeBlock]s that make up the method bodies of the generated
 * AppFunctionSerializableFactory.
 */
// TODO(b/392587953): extract common format maps
class AppFunctionSerializableFactoryCodeBuilder(
    val annotatedClass: AnnotatedAppFunctionSerializable,
    val resolvedAnnotatedSerializableProxies: ResolvedAnnotatedSerializableProxies
) {
    /**
     * Generates the method body of fromAppFunctionData for a non proxy serializable.
     *
     * This method uses [appendFromAppFunctionDataMethodBodyCommon] to generate the common code for
     * iterating through all the properties of a target serializable and extracting its
     * corresponding value from an [AppFunctionData]. It then returns the serializable itself.
     *
     * For example, given the following non proxy serializable class:
     * ```
     * @AppFunctionSerializable
     * class SampleSerializable(
     *     val longParam: Long,
     *     val doubleParam: Double,
     * )
     * ```
     *
     * The generated `fromAppFunctionData` method would look like:
     * ```
     * override fun fromAppFunctionData(appFunctionData: AppFunctionData) : SampleSerializable {
     *     val longParam = checkNotNull(appFunctionData.getLongOrNull("longParam"))
     *     val doubleParam = checkNotNull(appFunctionData.getDoubleOrNull("doubleParam"))
     *     val resultSampleSerializable = SampleSerializable(longParam, doubleParam)
     *     return resultSampleSerializable
     * }
     * ```
     */
    fun appendFromAppFunctionDataMethodBody(): CodeBlock {
        return buildCodeBlock {
            val getterResultName = getResultParamName(annotatedClass)
            add(appendFromAppFunctionDataMethodBodyCommon(getterResultName))
            addStatement(
                """
                return %L
                """
                    .trimIndent(),
                getterResultName
            )
        }
    }

    /**
     * Generates the method body of fromAppFunctionData for a proxy serializable.
     *
     * This method is similar to [appendFromAppFunctionDataMethodBody]. It uses
     * [appendFromAppFunctionDataMethodBodyCommon] to generate the common code for iterating through
     * all the properties of a target serializable and extracting its corresponding value from an
     * [AppFunctionData]. However, It then returns a proxy serializable target class instead of the
     * serializable itself.
     *
     * For example, given the following proxy serializable class:
     * ```
     * @AppFunctionSerializableProxy(targetClass = LocalDateTime::class)
     * class SampleSerializableProxy(
     *     val longParam: Long,
     *     val doubleParam: Double,
     * ) {
     *     public fun toLocalDateTime(): LocalDateTime {
     *         return LocalDateTime.of(...)
     *     }
     *
     *     public companion object {
     *         public fun fromLocalDateTime(localDateTIme: LocalDateTime) : SampleSerializableProxy
     *         {
     *             return SampleSerializableProxy(...)
     *         }
     *     }
     * }
     * ```
     *
     * The generated `fromAppFunctionData` method would look like:
     * ```
     * override fun fromAppFunctionData(appFunctionData: AppFunctionData) : LocalDateTime {
     *     val longParam = checkNotNull(appFunctionData.getLongOrNull("longParam"))
     *     val doubleParam = checkNotNull(appFunctionData.getDoubleOrNull("doubleParam"))
     *     val resultSampleSerializableProxy = SampleSerializableProxy(longParam, doubleParam)
     *     return resultSampleSerializableProxy.toLocalDateTime()
     * }
     * ```
     */
    fun appendFromAppFunctionDataMethodBodyForProxy(): CodeBlock {
        if (annotatedClass !is AnnotatedAppFunctionSerializableProxy) {
            throw ProcessingException(
                "Attempting to generate proxy getter for non proxy serializable.",
                annotatedClass.attributeNode
            )
        }
        return buildCodeBlock {
            val getterResultName = getResultParamName(annotatedClass)
            add(appendFromAppFunctionDataMethodBodyCommon(getterResultName))
            addStatement(
                """
                return %L.%L()
                """
                    .trimIndent(),
                getterResultName,
                annotatedClass.toTargetClassMethodName
            )
        }
    }

    /**
     * Generates common factory code for iterating through all the properties of a target
     * serializable and extracting its corresponding value from an [AppFunctionData].
     *
     * This function is used to build the `FromAppFunctionData` method of the generated
     * AppFunctionSerializableFactory.
     *
     * For example, given the following serializable class:
     * ```
     * @AppFunctionSerializable
     * class SampleSerializable(
     *     val longParam: Long,
     *     val doubleParam: Double,
     * )
     * ```
     *
     * The generated `fromAppFunctionData` method would look like:
     * ```
     * override fun fromAppFunctionData(appFunctionData: AppFunctionData) : SampleSerializable {
     *     val longParam = checkNotNull(appFunctionData.getLongOrNull("longParam"))
     *     val doubleParam = checkNotNull(appFunctionData.getDoubleOrNull("doubleParam"))
     *     val resultSampleSerializable = SampleSerializable(longParam, doubleParam)
     * }
     * ```
     *
     * Note that this method does not actually populate the value to be returned. It will only
     * handle extracting the relevant properties from the provided [AppFunctionData] to construct
     * the relevant [androidx.appfunctions.AppFunctionSerializable] data class. The caller will
     * append the actual return statement which could return the dataclass itself or a proxy target
     * class.
     */
    private fun appendFromAppFunctionDataMethodBodyCommon(getterResultName: String): CodeBlock {
        return buildCodeBlock {
            add(factoryInitStatements)
            for ((paramName, paramType) in annotatedClass.getProperties()) {
                appendGetterStatement(paramName, paramType)
            }
            appendGetterResultConstructorCallStatement(
                annotatedClass.originalClassName,
                annotatedClass.getProperties(),
                getterResultName
            )
            add("\n")
        }
    }

    /**
     * Generates the method body of toAppFunctionData for a non proxy serializable.
     *
     * This method uses [appendToAppFunctionDataMethodBodyCommon] to generate the common code for
     * iterating through all the properties of a target serializable and extracting its single
     * property values. It then returns an [AppFunctionData] instance with the extracted values.
     *
     * For example, given the following non proxy serializable class:
     * ```
     * @AppFunctionSerializable
     * class SampleSerializable(
     *     val longParam: Long,
     *     val doubleParam: Double,
     * )
     * ```
     *
     * The generated `toAppFunctionData` method would look like:
     * ```
     * override fun toAppFunctionData(appFunctionSerializable: SampleSerializable) : AppFunctionData {
     *     val sampleSerializable_appFunctionSerializable = appFunctionSerializable
     *     val longParam = sampleSerializable_appFunctionSerializable.longParam
     *     val doubleParam = sampleSerializable_appFunctionSerializable.doubleParam
     *     val builder = AppFunctionData.Builder("...")
     *     builder.setLong("longParam", longParam)
     *     builder.setDouble("doubleParam", doubleParam)
     *     return builder.build()
     * }
     * ```
     */
    fun appendToAppFunctionDataMethodBody(): CodeBlock {
        return buildCodeBlock {
            addStatement(
                """
                val %L = %L
                """
                    .trimIndent(),
                getSerializableParamName(annotatedClass),
                APP_FUNCTION_SERIALIZABLE_PARAM_NAME
            )
            add(appendToAppFunctionDataMethodBodyCommon())
        }
    }

    /**
     * Generates the method body of toAppFunctionData for a proxy serializable.
     *
     * This method is similar to [appendToAppFunctionDataMethodBody]. It uses
     * [appendToAppFunctionDataMethodBodyCommon] to generate the common code for iterating through
     * all the properties of a target serializable and extracting its single property values. It
     * then returns an [AppFunctionData] instance with the extracted values.
     *
     * The key difference from [appendToAppFunctionDataMethodBody] is the `toAppFunctionData`
     * factory method accepts the target class instead of an AppFunctionSerializable type directly.
     * The serializable type is obtained using the mandatory factory from the
     * [AnnotatedAppFunctionSerializableProxy].
     *
     * For example, given the following proxy serializable class:
     * ```
     * @AppFunctionSerializableProxy(targetClass = LocalDateTime::class)
     * class SampleSerializableProxy(
     *     val longParam: Long,
     *     val doubleParam: Double,
     * ) {
     *     public fun toLocalDateTime(): LocalDateTime {
     *         return LocalDateTime.of(...)
     *     }
     *
     *     public companion object {
     *         public fun fromLocalDateTime(localDateTIme: LocalDateTime) : SampleSerializableProxy
     *         {
     *             return SampleSerializableProxy(...)
     *         }
     *     }
     * }
     * ```
     *
     * The generated `toAppFunctionData` method would look like:
     * ```
     * override fun toAppFunctionData(appFunctionSerializable: LocalDateTime) : AppFunctionData {
     *     val localDateTime_appFunctionSerializable =
     *         SampleSerializableProxy.fromLocalDateTime(appFunctionSerializable)
     *     val longParam = localDateTime_appFunctionSerializable.longParam
     *     val doubleParam = localDateTime_appFunctionSerializable.doubleParam
     *     val builder = AppFunctionData.Builder("...")
     *     builder.setLong("longParam", longParam)
     *     builder.setDouble("doubleParam", doubleParam)
     *     return builder.build()
     * }
     * ```
     */
    fun appendToAppFunctionDataMethodBodyForProxy(): CodeBlock {
        if (annotatedClass !is AnnotatedAppFunctionSerializableProxy) {
            throw ProcessingException(
                "Attempting to generate proxy setter for non proxy serializable.",
                annotatedClass.attributeNode
            )
        }
        return buildCodeBlock {
            addStatement(
                """
                val %L = %T.%L(%L)
                """
                    .trimIndent(),
                getSerializableParamName(annotatedClass),
                annotatedClass.originalClassName,
                annotatedClass.fromTargetClassMethodName,
                APP_FUNCTION_SERIALIZABLE_PARAM_NAME
            )
            add(appendToAppFunctionDataMethodBodyCommon())
        }
    }

    /**
     * Generates common factory code for iterating through all the properties of an
     * [androidx.appfunctions.AppFunctionSerializable] to populate an [AppFunctionData] instance.
     *
     * This function is used to build the `toAppFunctionData` method of the generated
     * AppFunctionSerializableFactory.
     *
     * For example, given the following serializable class:
     * ```
     * @AppFunctionSerializable
     * class SampleSerializable(
     *     val longParam: Long,
     *     val doubleParam: Double,
     * )
     * ```
     *
     * The generated `toAppFunctionData` method would look like:
     * ```
     * override fun toAppFunctionData(sampleSerializable: SampleSerializable) : AppFunctionData {
     *     val longParam = sampleSerializable.longParam
     *     val doubleParam = sampleSerializable.doubleParam
     *     val builder = AppFunctionData.Builder("androidx.appfunctions.compiler.processors.SampleSerializable")
     *     builder.setLong("longParam", longParam)
     *     builder.setDouble("doubleParam", doubleParam)
     *     return builder.build()
     * }
     * ```
     *
     * Note that this method works directly with an [androidx.appfunctions.AppFunctionSerializable]
     * class. In a case where the factory is for a proxy, the caller is expected to add the
     * serializable representation of the proxy to the code block before calling this method.
     */
    private fun appendToAppFunctionDataMethodBodyCommon(): CodeBlock {
        return buildCodeBlock {
            add(factoryInitStatements)
            val qualifiedClassName = annotatedClass.qualifiedName
            addStatement("val builder = %T(%S)", AppFunctionData.Builder::class, qualifiedClassName)
            for (property in annotatedClass.getProperties()) {
                val afType = AppFunctionTypeReference(property.type)
                val formatStringMap =
                    mapOf<String, Any>(
                        "param_name" to property.name,
                        "annotated_class_instance" to getSerializableParamName(annotatedClass)
                    )
                addNamed(
                    "val %param_name:L = %annotated_class_instance:L.%param_name:L\n",
                    formatStringMap
                )
                if (afType.isNullable) {
                    appendNullableSetterStatement(property.name, afType)
                } else {
                    appendSetterStatement(property.name, afType)
                }
            }
            add("\nreturn builder.build()")
        }
    }

    private fun CodeBlock.Builder.appendGetterStatement(
        paramName: String,
        paramType: KSTypeReference
    ): CodeBlock.Builder {
        val afType = AppFunctionTypeReference(paramType)
        return when (afType.typeCategory) {
            PRIMITIVE_SINGULAR,
            PRIMITIVE_ARRAY,
            PRIMITIVE_LIST -> appendPrimitiveGetterStatement(paramName, afType)
            SERIALIZABLE_SINGULAR -> appendSerializableGetterStatement(paramName, afType)
            SERIALIZABLE_LIST ->
                appendSerializableListGetterStatement(
                    paramName,
                    afType,
                    afType.itemTypeReference.getTypeShortName()
                )
            SERIALIZABLE_PROXY_SINGULAR -> {
                val targetSerializableProxy =
                    resolvedAnnotatedSerializableProxies.getSerializableProxyForTypeReference(
                        afType
                    )
                appendSerializableGetterStatement(
                    paramName,
                    AppFunctionTypeReference(targetSerializableProxy.serializableReferenceType)
                )
            }
            SERIALIZABLE_PROXY_LIST -> {
                val targetSerializableProxy =
                    resolvedAnnotatedSerializableProxies.getSerializableProxyForTypeReference(
                        afType
                    )
                appendSerializableListGetterStatement(
                    paramName,
                    afType,
                    targetSerializableProxy.serializableReferenceType.getTypeShortName()
                )
            }
        }
    }

    private fun CodeBlock.Builder.appendPrimitiveGetterStatement(
        paramName: String,
        afType: AppFunctionTypeReference
    ): CodeBlock.Builder {
        val formatStringMap =
            mapOf<String, Any>(
                "param_name" to paramName,
                "app_function_data_param_name" to APP_FUNCTION_DATA_PARAM_NAME,
                "getter_name" to getAppFunctionDataGetterName(afType),
                "default_value_postfix" to getGetterDefaultValuePostfix(afType)
            )
        if (afType.isNullable) {
            addNamed(
                "val %param_name:L = %app_function_data_param_name:L.%getter_name:L(\"%param_name:L\")%default_value_postfix:L\n",
                formatStringMap
            )
        } else {
            addNamed(
                "val %param_name:L = checkNotNull(%app_function_data_param_name:L.%getter_name:L(\"%param_name:L\")%default_value_postfix:L)\n",
                formatStringMap
            )
        }
        return this
    }

    private fun CodeBlock.Builder.appendSerializableGetterStatement(
        paramName: String,
        afType: AppFunctionTypeReference
    ): CodeBlock.Builder {
        val typeName = afType.selfTypeReference.getTypeShortName()
        val formatStringMap =
            mapOf<String, Any>(
                "param_name" to paramName,
                "param_type" to afType.selfTypeReference.toTypeName(),
                "factory_name" to "${typeName}Factory".lowerFirstChar(),
                "app_function_data_param_name" to APP_FUNCTION_DATA_PARAM_NAME,
                "getter_name" to getAppFunctionDataGetterName(afType),
                "from_app_function_data_method_name" to FromAppFunctionDataMethod.METHOD_NAME,
                "serializable_data_val_name" to "${paramName}Data"
            )

        if (afType.isNullable) {
            addNamed(
                "val %serializable_data_val_name:L = %app_function_data_param_name:L.%getter_name:L(%param_name:S)\n",
                formatStringMap
            )
            return addNamed("var %param_name:L: %param_type:T = null\n", formatStringMap)
                .addNamed("if (%serializable_data_val_name:L != null) {\n", formatStringMap)
                .indent()
                .addNamed(
                    "%param_name:L = %factory_name:L.%from_app_function_data_method_name:L(%serializable_data_val_name:L)\n",
                    formatStringMap
                )
                .unindent()
                .addStatement("}")
        } else {
            addNamed(
                "val %serializable_data_val_name:L = checkNotNull(%app_function_data_param_name:L.%getter_name:L(%param_name:S))\n",
                formatStringMap
            )
            addNamed(
                "val %param_name:L = %factory_name:L.%from_app_function_data_method_name:L(%serializable_data_val_name:L)\n",
                formatStringMap
            )
        }
        return this
    }

    private fun CodeBlock.Builder.appendSerializableListGetterStatement(
        paramName: String,
        afType: AppFunctionTypeReference,
        parametrizedItemTypeName: String
    ): CodeBlock.Builder {
        val factoryName = parametrizedItemTypeName + "Factory"
        val factoryInstanceName = factoryName.lowerFirstChar()
        val formatStringMap =
            mapOf<String, Any>(
                "param_name" to paramName,
                "temp_list_name" to "${paramName}Data",
                "app_function_data_param_name" to APP_FUNCTION_DATA_PARAM_NAME,
                "factory_instance_name" to factoryInstanceName,
                "getter_name" to getAppFunctionDataGetterName(afType),
                "default_value_postfix" to getGetterDefaultValuePostfix(afType),
                "null_safe_op" to if (afType.isNullable) "?" else ""
            )

        addNamed(
                "val %temp_list_name:L = %app_function_data_param_name:L.%getter_name:L(\"%param_name:L\")%default_value_postfix:L\n",
                formatStringMap
            )
            .addNamed(
                "val %param_name:L = %temp_list_name:L%null_safe_op:L.map { data ->\n",
                formatStringMap
            )
            .indent()
            .addNamed("%factory_instance_name:L.fromAppFunctionData(data)\n", formatStringMap)
            .unindent()
            .addStatement("}")
        return this
    }

    private fun CodeBlock.Builder.appendGetterResultConstructorCallStatement(
        originalClassName: ClassName,
        properties: List<AppFunctionPropertyDeclaration>,
        getterResultName: String
    ): CodeBlock.Builder {
        val formatStringMap =
            mapOf<String, Any>(
                "original_class_name" to originalClassName,
                "params_list" to properties.joinToString(", ") { it.name },
                "getter_result_name" to getterResultName
            )

        addNamed(
            "\nval %getter_result_name:L = %original_class_name:T(%params_list:L)",
            formatStringMap
        )
        return this
    }

    private fun CodeBlock.Builder.appendNullableSetterStatement(
        paramName: String,
        afType: AppFunctionTypeReference
    ): CodeBlock.Builder {
        val formatStringMap =
            mapOf<String, Any>(
                "param_name" to paramName,
            )

        return addNamed("if (%param_name:L != null) {\n", formatStringMap)
            .indent()
            .appendSetterStatement(paramName, afType)
            .unindent()
            .addStatement("}")
    }

    private fun CodeBlock.Builder.appendSetterStatement(
        paramName: String,
        afType: AppFunctionTypeReference
    ): CodeBlock.Builder {
        return when (afType.typeCategory) {
            PRIMITIVE_SINGULAR,
            PRIMITIVE_ARRAY,
            PRIMITIVE_LIST -> appendPrimitiveSetterStatement(paramName, afType)
            SERIALIZABLE_SINGULAR -> appendSerializableSetterStatement(paramName, afType)
            SERIALIZABLE_LIST ->
                appendSerializableListSetterStatement(
                    paramName,
                    afType,
                    afType.itemTypeReference.getTypeShortName()
                )
            SERIALIZABLE_PROXY_SINGULAR -> {
                val targetSerializableProxy =
                    resolvedAnnotatedSerializableProxies.getSerializableProxyForTypeReference(
                        afType
                    )
                appendSerializableSetterStatement(
                    paramName,
                    AppFunctionTypeReference(targetSerializableProxy.serializableReferenceType)
                )
            }
            SERIALIZABLE_PROXY_LIST -> {
                val targetSerializableProxy =
                    resolvedAnnotatedSerializableProxies.getSerializableProxyForTypeReference(
                        afType
                    )
                appendSerializableListSetterStatement(
                    paramName,
                    afType,
                    targetSerializableProxy.serializableReferenceType.getTypeShortName()
                )
            }
        }
    }

    private fun CodeBlock.Builder.appendPrimitiveSetterStatement(
        paramName: String,
        afType: AppFunctionTypeReference
    ): CodeBlock.Builder {
        val formatStringMap =
            mapOf<String, Any>(
                "param_name" to paramName,
                "setter_name" to getAppFunctionDataSetterName(afType),
            )
        addNamed("builder.%setter_name:L(\"%param_name:L\", %param_name:L)\n", formatStringMap)
        return this
    }

    private fun CodeBlock.Builder.appendSerializableSetterStatement(
        paramName: String,
        afType: AppFunctionTypeReference
    ): CodeBlock.Builder {
        val typeName = afType.selfTypeReference.getTypeShortName()
        val formatStringMap =
            mapOf<String, Any>(
                "param_name" to paramName,
                "factory_name" to "${typeName}Factory".lowerFirstChar(),
                "setter_name" to getAppFunctionDataSetterName(afType),
            )

        addNamed(
            "builder.%setter_name:L(\"%param_name:L\", %factory_name:L.toAppFunctionData(%param_name:L))\n",
            formatStringMap
        )
        return this
    }

    private fun CodeBlock.Builder.appendSerializableListSetterStatement(
        paramName: String,
        afType: AppFunctionTypeReference,
        parametrizedItemTypeName: String
    ): CodeBlock.Builder {

        val formatStringMap =
            mapOf<String, Any>(
                "param_name" to paramName,
                "factory_name" to "${parametrizedItemTypeName}Factory".lowerFirstChar(),
                "setter_name" to getAppFunctionDataSetterName(afType),
                "lambda_param_name" to parametrizedItemTypeName.lowerFirstChar()
            )

        addNamed(
                "builder.%setter_name:L(\"%param_name:L\", " +
                    "%param_name:L" +
                    ".map{ %lambda_param_name:L ->\n",
                formatStringMap
            )
            .indent()
            .addNamed("%factory_name:L.toAppFunctionData(%lambda_param_name:L)\n", formatStringMap)
            .unindent()
            .addStatement("})")
        return this
    }

    private fun getAppFunctionDataGetterName(afType: AppFunctionTypeReference): String {
        val shortTypeName = afType.selfOrItemTypeReference.getTypeShortName()
        return when (afType.typeCategory) {
            PRIMITIVE_SINGULAR -> {
                if (afType.selfTypeReference.isOfType(STRING)) {
                    "getString"
                } else {
                    "get${shortTypeName}OrNull"
                }
            }
            PRIMITIVE_ARRAY -> "get$shortTypeName"
            SERIALIZABLE_PROXY_SINGULAR,
            SERIALIZABLE_SINGULAR -> "getAppFunctionData"
            SERIALIZABLE_PROXY_LIST,
            SERIALIZABLE_LIST -> "getAppFunctionDataList"
            PRIMITIVE_LIST -> "get${shortTypeName}List"
        }
    }

    // Missing list/array types default to an empty list/array; missing singular properties throw an
    // error; all nullable properties default to null.
    private fun getGetterDefaultValuePostfix(afType: AppFunctionTypeReference): String {
        return when (afType.typeCategory) {
            PRIMITIVE_SINGULAR,
            SERIALIZABLE_PROXY_SINGULAR,
            SERIALIZABLE_SINGULAR -> ""
            PRIMITIVE_ARRAY ->
                if (afType.isNullable) {
                    ""
                } else {
                    " ?: ${afType.selfOrItemTypeReference.getTypeShortName()}(0)"
                }
            PRIMITIVE_LIST,
            SERIALIZABLE_PROXY_LIST,
            SERIALIZABLE_LIST -> if (afType.isNullable) "" else " ?: emptyList()"
        }
    }

    private fun getAppFunctionDataSetterName(afType: AppFunctionTypeReference): String {
        return when (afType.typeCategory) {
            PRIMITIVE_SINGULAR,
            PRIMITIVE_ARRAY -> "set${afType.selfOrItemTypeReference.getTypeShortName()}"
            PRIMITIVE_LIST -> "set${afType.selfOrItemTypeReference.getTypeShortName()}List"
            SERIALIZABLE_SINGULAR,
            SERIALIZABLE_PROXY_SINGULAR -> "setAppFunctionData"
            SERIALIZABLE_PROXY_LIST,
            SERIALIZABLE_LIST -> "setAppFunctionDataList"
        }
    }

    private val factoryInitStatements = buildCodeBlock {
        val factoryInstanceNameToClassMap: Map<String, ClassName> = buildMap {
            for (serializableType in annotatedClass.getSerializablePropertyTypeReferences()) {
                val qualifiedName =
                    serializableType.selfOrItemTypeReference.ensureQualifiedTypeName()
                put(
                    "${qualifiedName.getShortName().lowerFirstChar()}Factory",
                    ClassName(
                        qualifiedName.getQualifier(),
                        "`\$${qualifiedName.getShortName()}Factory`"
                    )
                )
            }

            for (proxyTypeReference in
                annotatedClass.getSerializableProxyPropertyTypeReferences()) {
                val targetSerializableProxy =
                    resolvedAnnotatedSerializableProxies.getSerializableProxyForTypeReference(
                        proxyTypeReference
                    )
                put(
                    "${targetSerializableProxy.originalClassName.simpleName.lowerFirstChar()}Factory",
                    ClassName(
                        targetSerializableProxy.originalClassName.packageName,
                        "`\$${targetSerializableProxy.targetClassDeclaration.simpleName.asString()}Factory`"
                    )
                )
            }
        }
        for (entry in factoryInstanceNameToClassMap) {
            addStatement("val %L = %T()", entry.key, entry.value)
        }
        add("\n")
    }

    private fun KSTypeReference.getTypeShortName(): String {
        return this.ensureQualifiedTypeName().getShortName()
    }

    private fun String.lowerFirstChar(): String {
        return replaceFirstChar { it -> it.lowercase() }
    }

    private fun getResultParamName(annotatedClass: AnnotatedAppFunctionSerializable): String {
        return "result${annotatedClass.originalClassName.simpleName}"
    }

    private fun getSerializableParamName(annotatedClass: AnnotatedAppFunctionSerializable): String {
        return "${annotatedClass.originalClassName.simpleName.replaceFirstChar {
                it -> it.lowercase() }}_appFunctionSerializable"
    }
}
