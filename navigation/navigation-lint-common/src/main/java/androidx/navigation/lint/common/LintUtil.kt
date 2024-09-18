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

package androidx.navigation.lint.common

import com.android.tools.lint.checks.infrastructure.LintDetectorTest.kotlin
import com.android.tools.lint.checks.infrastructure.TestFile
import com.android.tools.lint.checks.infrastructure.TestFiles
import com.intellij.psi.PsiClass
import com.intellij.psi.impl.source.PsiClassReferenceType
import java.util.Locale
import org.intellij.lang.annotations.Language
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.symbols.KtClassKind
import org.jetbrains.kotlin.analysis.api.symbols.KtClassOrObjectSymbol
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
    checkCompanion: Boolean = true
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
                as? KtClassOrObjectSymbol ?: return false to null

        ((checkClass && symbol.classKind.isClass) ||
            (checkInterface && symbol.classKind == KtClassKind.INTERFACE) ||
            (checkCompanion && symbol.classKind == KtClassKind.COMPANION_OBJECT)) to
            symbol.name?.asString()
    }
}

fun UExpression.getKClassType(): PsiClass? {
    // filter for KClass<*>
    if (this !is UClassLiteralExpression) return null

    val expressionType = getExpressionType() ?: return null
    if (expressionType !is PsiClassReferenceType) return null
    val typeArg = expressionType.typeArguments().firstOrNull() ?: return null
    return (typeArg as? PsiClassReferenceType)?.reference?.resolve() as? PsiClass
}

/**
 * Copied from compose.lint.test.stubs
 *
 * Utility for creating a [kotlin] and corresponding [bytecode] stub, to try and make it easier to
 * configure everything correctly.
 *
 * @param filename name of the Kotlin source file, with extension - e.g. "Test.kt". These should be
 *   unique across a test.
 * @param filepath directory structure matching the package name of the Kotlin source file. E.g. if
 *   the source has `package foo.bar`, this should be `foo/bar`. If this does _not_ match, lint will
 *   not be able to match the generated classes with the source file, and so won't print them to
 *   console.
 * @param source Kotlin source for the bytecode
 * @param bytecode generated bytecode that will be used in tests. Leave empty to generate the
 *   bytecode for [source].
 * @return a pair of kotlin test file, to bytecode test file
 */
fun kotlinAndBytecodeStub(
    filename: String,
    filepath: String,
    checksum: Long,
    @Language("kotlin") source: String,
    vararg bytecode: String
): KotlinAndBytecodeStub {
    val filenameWithoutExtension = filename.substringBefore(".").lowercase(Locale.ROOT)
    val kotlin = kotlin(source).to("$filepath/$filename")
    val bytecodeStub =
        TestFiles.bytecode("libs/$filenameWithoutExtension.jar", kotlin, checksum, *bytecode)
    return KotlinAndBytecodeStub(kotlin, bytecodeStub)
}

class KotlinAndBytecodeStub(val kotlin: TestFile, val bytecode: TestFile)

/**
 * Copied from compose.lint.test.stubs
 *
 * Utility for creating a [bytecode] stub, to try and make it easier to configure everything
 * correctly.
 *
 * @param filename name of the Kotlin source file, with extension - e.g. "Test.kt". These should be
 *   unique across a test.
 * @param filepath directory structure matching the package name of the Kotlin source file. E.g. if
 *   the source has `package foo.bar`, this should be `foo/bar`. If this does _not_ match, lint will
 *   not be able to match the generated classes with the source file, and so won't print them to
 *   console.
 * @param source Kotlin source for the bytecode
 * @param bytecode generated bytecode that will be used in tests. Leave empty to generate the
 *   bytecode for [source].
 */
fun bytecodeStub(
    filename: String,
    filepath: String,
    checksum: Long,
    @Language("kotlin") source: String,
    vararg bytecode: String
): TestFile = kotlinAndBytecodeStub(filename, filepath, checksum, source, *bytecode).bytecode
