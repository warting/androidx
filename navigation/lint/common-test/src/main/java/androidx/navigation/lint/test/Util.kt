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

package androidx.navigation.lint.test

import org.intellij.lang.annotations.Language

/**
 * Workaround for b/371463741
 *
 * Once the new lint common structure is merged, we should replace this helper with the
 * kotlinAndBytecodeStub helper from compose.lint.test.stubs
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
 * @return CompatKotlinAndBytecodeStub
 */
fun kotlinAndBytecodeStub(
    filename: String,
    filepath: String,
    checksum: Long,
    @Language("kotlin") source: String,
    vararg bytecode: String
) = CompatKotlinAndBytecodeStub(filename, filepath, checksum, source, *bytecode)

/**
 * Workaround for b/371463741
 *
 * Once the new lint common structure is merged, we should replace this helper with the
 * kotlinAndBytecodeStub helper from compose.lint.test.stubs
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
 * @return CompatKotlinAndBytecodeStub
 */
fun bytecodeStub(
    filename: String,
    filepath: String,
    checksum: Long,
    @Language("kotlin") source: String,
    vararg bytecode: String
) = CompatKotlinAndBytecodeStub(filename, filepath, checksum, source, *bytecode)

/**
 * Workaround for b/371463741
 *
 * Once the new lint common structure is merged, delete this compat class
 */
class CompatKotlinAndBytecodeStub(
    val filename: String,
    val filepath: String,
    val checksum: Long,
    @Language("kotlin") val source: String,
    vararg val bytecode: String
)
