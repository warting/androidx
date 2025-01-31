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

package androidx.appfunctions.compiler.core

import androidx.appfunctions.compiler.core.IntrospectionHelper.AppFunctionAnnotation
import androidx.appfunctions.compiler.core.IntrospectionHelper.AppFunctionContextClass
import androidx.appfunctions.compiler.core.IntrospectionHelper.AppFunctionSchemaDefinitionAnnotation
import androidx.appfunctions.metadata.AppFunctionComponentsMetadataDocument
import androidx.appfunctions.metadata.AppFunctionDataTypeMetadata
import androidx.appfunctions.metadata.AppFunctionDataTypeMetadataDocument
import androidx.appfunctions.metadata.AppFunctionMetadataDocument
import androidx.appfunctions.metadata.AppFunctionPropertyMetadataDocument
import androidx.appfunctions.metadata.AppFunctionSchemaMetadataDocument
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFile
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSTypeReference
import com.google.devtools.ksp.symbol.KSValueParameter
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.LIST

/**
 * Represents a collection of functions within a specific class that are annotated as app functions.
 */
data class AnnotatedAppFunctions(
    /**
     * The [com.google.devtools.ksp.symbol.KSClassDeclaration] of the class that contains the
     * annotated app functions.
     */
    val classDeclaration: KSClassDeclaration,
    /**
     * The list of [com.google.devtools.ksp.symbol.KSFunctionDeclaration] that annotated as app
     * function.
     */
    val appFunctionDeclarations: List<KSFunctionDeclaration>
) {
    fun validate(): AnnotatedAppFunctions {
        validateFirstParameter()
        validateParameterTypes()
        return this
    }

    private fun validateFirstParameter() {
        for (appFunctionDeclaration in appFunctionDeclarations) {
            val firstParam = appFunctionDeclaration.parameters.firstOrNull()
            if (firstParam == null) {
                throw ProcessingException(
                    "The first parameter of an app function must be " +
                        "${AppFunctionContextClass.CLASS_NAME}",
                    appFunctionDeclaration
                )
            }
            if (!firstParam.type.isOfType(AppFunctionContextClass.CLASS_NAME)) {
                throw ProcessingException(
                    "The first parameter of an app function must be " +
                        "${AppFunctionContextClass.CLASS_NAME}",
                    firstParam
                )
            }
        }
    }

    private fun validateParameterTypes() {
        for (appFunctionDeclaration in appFunctionDeclarations) {
            for ((paramIndex, ksValueParameter) in appFunctionDeclaration.parameters.withIndex()) {
                if (paramIndex == 0) {
                    // Skip the first parameter which is always the `AppFunctionContext`.
                    continue
                }

                if (!ksValueParameter.type.isSupportedType()) {
                    throw ProcessingException(
                        "App function parameters must be one of the following " +
                            "primitive types or a list of these types:\n${
                                SUPPORTED_TYPES.joinToString(
                                    ",\n"
                                )
                            }, but found ${
                                ksValueParameter.resolveTypeReference().ensureQualifiedTypeName()
                                    .asString()
                            }",
                        ksValueParameter
                    )
                }
            }
        }
    }

    /**
     * Gets the identifier of an app functions.
     *
     * The format of the identifier is `packageName.className#methodName`.
     */
    fun getAppFunctionIdentifier(functionDeclaration: KSFunctionDeclaration): String {
        val packageName = classDeclaration.packageName.asString()
        val className = classDeclaration.simpleName.asString()
        val methodName = functionDeclaration.simpleName.asString()
        return "${packageName}.${className}#${methodName}"
    }

    /** Returns the file containing the class declaration and app functions. */
    fun getSourceFile(): KSFile? = classDeclaration.containingFile

    /** Gets the [classDeclaration]'s [ClassName]. */
    fun getEnclosingClassName(): ClassName {
        return ClassName(
            classDeclaration.packageName.asString(),
            classDeclaration.simpleName.asString()
        )
    }

    /** Creates [AppFunctionMetadataDocument] instances for each of [appFunctionDeclarations]. */
    fun createAppFunctionMetadataInstances(): List<AppFunctionMetadataDocument> =
        this.appFunctionDeclarations.map { fnDeclaration ->
            val appFunctionAnnotationProperties =
                computeAppFunctionAnnotationProperties(fnDeclaration)
            val schemaMetadata =
                if (appFunctionAnnotationProperties.schemaName != null) {
                    AppFunctionSchemaMetadataDocument(
                        schemaCategory =
                            checkNotNull(appFunctionAnnotationProperties.schemaCategory),
                        schemaName = checkNotNull(appFunctionAnnotationProperties.schemaName),
                        schemaVersion = checkNotNull(appFunctionAnnotationProperties.schemaVersion)
                    )
                } else {
                    null
                }
            AppFunctionMetadataDocument(
                id = this.getAppFunctionIdentifier(fnDeclaration),
                isEnabledByDefault = appFunctionAnnotationProperties.isEnabledByDefault,
                schema = schemaMetadata,
                // TODO: Handle non-primitive and collections.
                parameters = fnDeclaration.buildMetadataForParameters(),
                response =
                    AppFunctionDataTypeMetadataDocument(
                        type = checkNotNull(fnDeclaration.returnType?.toAppFunctionDataType()),
                        isNullable = fnDeclaration.returnType?.resolve()?.isMarkedNullable == true,
                    ),
                components = AppFunctionComponentsMetadataDocument(dataTypes = emptyList())
            )
        }

    private fun KSFunctionDeclaration.buildMetadataForParameters():
        AppFunctionDataTypeMetadataDocument {
        // TODO: Consider building the non-document classes first and only converting them to
        //  documents just before serializing them into XML.
        val properties =
            parameters
                .filter { !it.type.isOfType(AppFunctionContextClass.CLASS_NAME) }
                .map {
                    AppFunctionPropertyMetadataDocument(
                        name = checkNotNull(it.name?.asString()),
                        dataTypeMetadata =
                            AppFunctionDataTypeMetadataDocument(
                                type = it.type.toAppFunctionDataType(),
                            )
                    )
                }
        return AppFunctionDataTypeMetadataDocument(
            type = AppFunctionDataTypeMetadata.TYPE_OBJECT,
            properties = properties
        )
    }

    private fun KSTypeReference.toAppFunctionDataType(): Int =
        when (this.resolve().declaration.qualifiedName?.asString()) {
            "kotlin.String" -> AppFunctionDataTypeMetadata.TYPE_STRING
            "kotlin.Int" -> AppFunctionDataTypeMetadata.TYPE_INT
            "kotlin.Long" -> AppFunctionDataTypeMetadata.TYPE_LONG
            "kotlin.Float" -> AppFunctionDataTypeMetadata.TYPE_FLOAT
            "kotlin.Double" -> AppFunctionDataTypeMetadata.TYPE_DOUBLE
            "kotlin.Boolean" -> AppFunctionDataTypeMetadata.TYPE_BOOLEAN
            "kotlin.Byte" -> AppFunctionDataTypeMetadata.TYPE_BYTES
            "kotlin.Unit" -> AppFunctionDataTypeMetadata.TYPE_UNIT
            // TODO: Support converting other types.
            else -> AppFunctionDataTypeMetadata.TYPE_OBJECT
        }

    private fun computeAppFunctionAnnotationProperties(
        functionDeclaration: KSFunctionDeclaration
    ): AppFunctionAnnotationProperties {
        val appFunctionAnnotation =
            functionDeclaration.annotations.findAnnotation(AppFunctionAnnotation.CLASS_NAME)
                ?: throw ProcessingException(
                    "Function not annotated with @AppFunction.",
                    functionDeclaration
                )
        val enabled =
            appFunctionAnnotation.requirePropertyValueOfType(
                AppFunctionAnnotation.PROPERTY_IS_ENABLED,
                Boolean::class,
            )

        val rootInterfaceWithAppFunctionSchemaDefinition =
            findRootAppFunctionSchemaInterface(functionDeclaration)

        val schemaFunctionAnnotation =
            rootInterfaceWithAppFunctionSchemaDefinition
                ?.annotations
                ?.findAnnotation(AppFunctionSchemaDefinitionAnnotation.CLASS_NAME)
        val schemaCategory =
            schemaFunctionAnnotation?.requirePropertyValueOfType(
                AppFunctionSchemaDefinitionAnnotation.PROPERTY_CATEGORY,
                String::class,
            )
        val schemaName =
            schemaFunctionAnnotation?.requirePropertyValueOfType(
                AppFunctionSchemaDefinitionAnnotation.PROPERTY_NAME,
                String::class,
            )
        val schemaVersion =
            schemaFunctionAnnotation
                ?.requirePropertyValueOfType(
                    AppFunctionSchemaDefinitionAnnotation.PROPERTY_VERSION,
                    Int::class,
                )
                ?.toLong()

        return AppFunctionAnnotationProperties(enabled, schemaName, schemaVersion, schemaCategory)
    }

    private fun findRootAppFunctionSchemaInterface(
        function: KSFunctionDeclaration,
    ): KSClassDeclaration? {
        val parentDeclaration = function.parentDeclaration as? KSClassDeclaration ?: return null

        // Check if the enclosing class has the @AppFunctionSchemaDefinition
        val annotation =
            parentDeclaration.annotations.findAnnotation(
                AppFunctionSchemaDefinitionAnnotation.CLASS_NAME
            )
        if (annotation != null) {
            return parentDeclaration
        }

        val superClassFunction = (function.findOverridee() as? KSFunctionDeclaration) ?: return null
        return findRootAppFunctionSchemaInterface(superClassFunction)
    }

    /**
     * Resolves the type reference of a parameter.
     *
     * If the parameter type is a list, it will resolve the type reference of the list element.
     */
    private fun KSValueParameter.resolveTypeReference(): KSTypeReference {
        return if (type.isOfType(LIST)) {
            type.resolveListParameterizedType()
        } else {
            type
        }
    }

    private data class AppFunctionAnnotationProperties(
        val isEnabledByDefault: Boolean,
        val schemaName: String?,
        val schemaVersion: Long?,
        val schemaCategory: String?
    )
}
