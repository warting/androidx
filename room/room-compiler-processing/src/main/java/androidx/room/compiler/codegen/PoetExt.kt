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

package androidx.room.compiler.codegen

typealias JCodeBlock = com.squareup.javapoet.CodeBlock

typealias JCodeBlockBuilder = com.squareup.javapoet.CodeBlock.Builder

typealias JAnnotationSpecBuilder = com.squareup.javapoet.AnnotationSpec.Builder

typealias JPropertySpec = com.squareup.javapoet.FieldSpec

typealias JPropertySpecBuilder = com.squareup.javapoet.FieldSpec.Builder

typealias JParameterSpec = com.squareup.javapoet.ParameterSpec

typealias JParameterSpecBuilder = com.squareup.javapoet.ParameterSpec.Builder

typealias JFunSpec = com.squareup.javapoet.MethodSpec

typealias JFunSpecBuilder = com.squareup.javapoet.MethodSpec.Builder

typealias JModifier = javax.lang.model.element.Modifier

typealias JTypeSpecBuilder = com.squareup.javapoet.TypeSpec.Builder

typealias JFileSpec = com.squareup.javapoet.JavaFile

typealias JFileSpecBuilder = com.squareup.javapoet.JavaFile.Builder

typealias KCodeBlock = com.squareup.kotlinpoet.CodeBlock

typealias KCodeBlockBuilder = com.squareup.kotlinpoet.CodeBlock.Builder

typealias KAnnotationSpecBuilder = com.squareup.kotlinpoet.AnnotationSpec.Builder

typealias KPropertySpec = com.squareup.kotlinpoet.PropertySpec

typealias KPropertySpecBuilder = com.squareup.kotlinpoet.PropertySpec.Builder

typealias KParameterSpec = com.squareup.kotlinpoet.ParameterSpec

typealias KParameterSpecBuilder = com.squareup.kotlinpoet.ParameterSpec.Builder

typealias KFunSpec = com.squareup.kotlinpoet.FunSpec

typealias KFunSpecBuilder = com.squareup.kotlinpoet.FunSpec.Builder

typealias KTypeSpecBuilder = com.squareup.kotlinpoet.TypeSpec.Builder

typealias KFileSpec = com.squareup.kotlinpoet.FileSpec

typealias KFileSpecBuilder = com.squareup.kotlinpoet.FileSpec.Builder

typealias KMemberName = com.squareup.kotlinpoet.MemberName

typealias JArrayTypeName = com.squareup.javapoet.ArrayTypeName

// TODO(b/127483380): Recycle to room-compiler?
internal val L = "\$L"
internal val T = "\$T"
internal val N = "\$N"
internal val S = "\$S"
internal val W = "\$W"
