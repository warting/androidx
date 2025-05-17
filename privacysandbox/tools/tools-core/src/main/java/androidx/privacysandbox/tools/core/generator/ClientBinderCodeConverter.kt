/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.privacysandbox.tools.core.generator

import androidx.privacysandbox.tools.core.generator.SdkActivityLauncherProxyGenerator.Companion.converterClassName
import androidx.privacysandbox.tools.core.generator.SpecNames.contextPropertyName
import androidx.privacysandbox.tools.core.generator.ValueConverterFileGenerator.Companion.toParcelableMethodName
import androidx.privacysandbox.tools.core.model.AnnotatedInterface
import androidx.privacysandbox.tools.core.model.AnnotatedValue
import androidx.privacysandbox.tools.core.model.ParsedApi
import androidx.privacysandbox.tools.core.model.getOnlyService
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.TypeName

class ClientBinderCodeConverter(api: ParsedApi) : BinderCodeConverter(api) {
    private val basePackageName = api.getOnlyService().type.packageName
    private val activityLauncherConverterClass = ClassName(basePackageName, converterClassName)

    override fun convertToInterfaceModelCode(
        annotatedInterface: AnnotatedInterface,
        expression: String,
    ): CodeBlock {
        if (annotatedInterface.inheritsUiAdapter) {
            return CodeBlock.of(
                "%T(%L.binder, %L.coreLibInfo)",
                annotatedInterface.clientProxyNameSpec(),
                expression,
                expression,
            )
        }
        return CodeBlock.of("%T(%L)", annotatedInterface.clientProxyNameSpec(), expression)
    }

    override fun convertToInterfaceBinderCode(
        annotatedInterface: AnnotatedInterface,
        expression: String,
    ): CodeBlock {
        if (annotatedInterface.inheritsUiAdapter) {
            return CodeBlock.builder().build {
                addNamed(
                    "%coreLibInfoConverter:T.%toParcelable:N(" +
                        "(%interface:L as %clientProxy:T).coreLibInfo, " +
                        "%interface:L.remote)",
                    hashMapOf<String, Any>(
                        "coreLibInfoConverter" to
                            ClassName(
                                annotatedInterface.type.packageName,
                                annotatedInterface.coreLibInfoConverterName(),
                            ),
                        "toParcelable" to toParcelableMethodName,
                        "interface" to expression,
                        "context" to contextPropertyName,
                        "clientProxy" to annotatedInterface.clientProxyNameSpec(),
                    ),
                )
            }
        }
        return CodeBlock.of(
            "(%L as %T).remote",
            expression,
            annotatedInterface.clientProxyNameSpec(),
        )
    }

    override fun convertToInterfaceBinderType(annotatedInterface: AnnotatedInterface): TypeName {
        if (annotatedInterface.inheritsUiAdapter) {
            return annotatedInterface.uiAdapterAidlWrapper().poetTypeName()
        }
        return annotatedInterface.aidlType().innerType.poetTypeName()
    }

    override fun convertToValueBinderCode(value: AnnotatedValue, expression: String): CodeBlock =
        CodeBlock.of(
            "%M(%L)",
            MemberName(
                value.converterNameSpec(),
                ValueConverterFileGenerator.toParcelableMethodName,
            ),
            expression,
        )

    override fun convertToValueModelCode(value: AnnotatedValue, expression: String): CodeBlock =
        CodeBlock.of(
            "%M(%L)",
            MemberName(
                value.converterNameSpec(),
                ValueConverterFileGenerator.fromParcelableMethodName,
            ),
            expression,
        )

    override fun convertToActivityLauncherBinderCode(expression: String): CodeBlock =
        CodeBlock.of("%M(%L)", MemberName(activityLauncherConverterClass, "toBinder"), expression)

    override fun convertToActivityLauncherModelCode(expression: String): CodeBlock =
        CodeBlock.of(
            "%M(%L)",
            MemberName(activityLauncherConverterClass, "getLocalOrProxyLauncher"),
            expression,
        )
}
