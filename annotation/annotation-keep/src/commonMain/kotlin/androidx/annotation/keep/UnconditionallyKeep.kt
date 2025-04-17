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

package androidx.annotation.keep

/**
 * Indicates code which is accessed by references from outside of the application code directly,
 * such as via JNI, reflection, or instantiation from platform framework code.
 *
 * NOTE: This keep rule is unconditional, meaning that the annotated class, method, or field will
 * always be preserved in the final application even if, for example, the surrounding code is never
 * used.
 *
 * If reflection or JNI access occurs inside the application, instead prefer the
 * `@UsesReflection***` annotations, as they will keep conditionally, rather than unconditionally as
 * this annotation does.
 *
 * @see UsesReflectionToConstruct
 * @see UsesReflectionToAccessMethod
 * @see UsesReflectionToAccessField
 */
@Retention(AnnotationRetention.BINARY)
@Target(
    AnnotationTarget.CLASS,
    AnnotationTarget.FIELD,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.CONSTRUCTOR,
)
public annotation class UnconditionallyKeep(
    /**
     * Should the name be preserved.
     *
     * Generally this is true if the reference is external, but this can be disabled if the name
     * isn't important.
     */
    val shouldPreserveName: Boolean = true
)
