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

package androidx.navigation.lint

import com.intellij.psi.PsiClass
import com.intellij.psi.impl.source.PsiClassReferenceType
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.symbols.KaClassKind
import org.jetbrains.kotlin.analysis.api.symbols.KaClassSymbol
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtReferenceExpression
import org.jetbrains.uast.UClassLiteralExpression
import org.jetbrains.uast.UExpression
import org.jetbrains.uast.UQualifiedReferenceExpression
import org.jetbrains.uast.USimpleNameReferenceExpression

/** Catches simple class/interface name reference */
fun UExpression.isClassReference(
    checkClass: Boolean = true,
    checkInterface: Boolean = true,
    checkCompanion: Boolean = true,
): Pair<Boolean, String?> {
    /**
     * True if:
     * 1. reference to object (i.e. val myStart = TestStart(), startDest = myStart)
     * 2. object declaration (i.e. object MyStart, startDest = MyStart)
     * 3. class reference (i.e. class MyStart, startDest = MyStart)
     *
     *    We only want to catch case 3., so we need more filters to eliminate case 1 & 2.
     */
    val isSimpleRefExpression = this is USimpleNameReferenceExpression

    /** True if nested class i.e. OuterClass.InnerClass */
    val isQualifiedRefExpression = this is UQualifiedReferenceExpression

    if (!(isSimpleRefExpression || isQualifiedRefExpression)) return false to null

    val sourcePsi = sourcePsi as? KtExpression ?: return false to null
    return analyze(sourcePsi) {
        val symbol =
            when (sourcePsi) {
                is KtDotQualifiedExpression -> {
                    val lastChild = sourcePsi.lastChild
                    if (lastChild is KtReferenceExpression) {
                        lastChild.mainReference.resolveToSymbol()
                    } else {
                        null
                    }
                }
                is KtReferenceExpression -> sourcePsi.mainReference.resolveToSymbol()
                else -> null
            }
                as? KaClassSymbol ?: return false to null

        ((checkClass && symbol.classKind.isClass) ||
            (checkInterface && symbol.classKind == KaClassKind.INTERFACE) ||
            (checkCompanion && symbol.classKind == KaClassKind.COMPANION_OBJECT)) to
            symbol.name?.asString()
    }
}

fun UExpression.getKClassType(): PsiClass? {
    // filter for KClass<*>
    if (this !is UClassLiteralExpression) return null

    val expressionType = getExpressionType() ?: return null
    if (expressionType !is PsiClassReferenceType) return null
    @Suppress("UnstableApiUsage")
    val typeArg = expressionType.typeArguments().firstOrNull() ?: return null
    return (typeArg as? PsiClassReferenceType)?.reference?.resolve() as? PsiClass
}

const val MIN_ANALYSIS_API = 14
