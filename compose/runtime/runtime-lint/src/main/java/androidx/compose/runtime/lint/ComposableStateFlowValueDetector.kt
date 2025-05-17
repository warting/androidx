/*
 * Copyright 2021 The Android Open Source Project
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

@file:Suppress("UnstableApiUsage")

package androidx.compose.runtime.lint

import androidx.compose.lint.Name
import androidx.compose.lint.Package
import androidx.compose.lint.inheritsFrom
import androidx.compose.lint.isInvokedWithinComposable
import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import com.intellij.psi.PsiMethod
import com.intellij.psi.util.parentOfType
import java.util.EnumSet
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.symbols.KaClassSymbol
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.uast.USimpleNameReferenceExpression
import org.jetbrains.uast.tryResolve

/**
 * [Detector] that checks calls to StateFlow.value to make sure they don't happen inside the body of
 * a composable function / lambda.
 */
class ComposableStateFlowValueDetector : Detector(), SourceCodeScanner {
    override fun getApplicableUastTypes() = listOf(USimpleNameReferenceExpression::class.java)

    override fun createUastHandler(context: JavaContext) =
        object : UElementHandler() {
            override fun visitSimpleNameReferenceExpression(node: USimpleNameReferenceExpression) {
                // Look for a call to .value that comes from StateFlow
                if (node.identifier != "value") return
                val psiElement = node.tryResolve()
                val inheritsFromStateFlow =
                    when (psiElement) {
                        // PsiMethod is expected in Android/JVM source sets
                        is PsiMethod ->
                            psiElement.containingClass?.inheritsFrom(StateFlowName) == true
                        // KtProperty is expected in common source sets
                        is KtProperty -> {
                            val thisClass = psiElement.parentOfType<KtClass>() ?: return
                            analyze(thisClass) {
                                val symbol = thisClass.symbol as KaClassSymbol
                                val baseClassId = ClassId.topLevel(FqName(StateFlowName.javaFqn))
                                val baseClassSymbol = findClass(baseClassId)
                                symbol.isSubClassOf(baseClassSymbol ?: return@analyze false)
                            }
                        }
                        else -> false
                    }
                if (inheritsFromStateFlow) {
                    if (node.isInvokedWithinComposable()) {
                        context.report(
                            StateFlowValueCalledInComposition,
                            node,
                            context.getNameLocation(node),
                            "StateFlow.value should not be called within composition",
                            fix()
                                .replace()
                                .text("value")
                                .with("collectAsState().value")
                                .imports("androidx.compose.runtime.collectAsState")
                                .build(),
                        )
                    }
                }
            }
        }

    companion object {
        val StateFlowValueCalledInComposition =
            Issue.create(
                "StateFlowValueCalledInComposition",
                "StateFlow.value should not be called within composition",
                "Calling StateFlow.value within composition will not observe changes to the " +
                    "StateFlow, so changes might not be reflected within the composition. Instead " +
                    "you should use stateFlow.collectAsState() to observe changes to the StateFlow, " +
                    "and recompose when it changes.",
                Category.CORRECTNESS,
                3,
                Severity.ERROR,
                Implementation(
                    ComposableStateFlowValueDetector::class.java,
                    EnumSet.of(Scope.JAVA_FILE, Scope.TEST_SOURCES),
                ),
            )
    }
}

private val StateFlowPackageName = Package("kotlinx.coroutines.flow")
private val StateFlowName = Name(StateFlowPackageName, "StateFlow")
