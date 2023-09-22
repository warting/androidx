/*
 * Copyright 2023 The Android Open Source Project
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
package androidx.annotation

/**
 * Denotes that the annotated element should only be called on the given Android API level or
 * higher.
 *
 * This is similar in purpose to the older `@TargetApi` annotation, but more clearly expresses that
 * this is a requirement on the caller, rather than being used to "suppress" warnings within the
 * method that exceed the `minSdkVersion`.
 *
 * For API requirements on SDK extensions, see the [androidx.annotation.RequiresExtension]
 * annotation.
 */
@MustBeDocumented
@Retention(AnnotationRetention.BINARY)
@Target(
    AnnotationTarget.ANNOTATION_CLASS,
    AnnotationTarget.CLASS,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER,
    AnnotationTarget.CONSTRUCTOR,
    AnnotationTarget.FIELD,
    AnnotationTarget.FILE
)
@Suppress("SupportAnnotationUsage")
@OptIn(ExperimentalMultiplatform::class)
@OptionalExpectation // Need to use expect due to Java-specific target annotations on the actual.
public expect annotation class RequiresApi(
    /** The API level to require. Alias for [.api] which allows you to leave out the `api=` part. */
    @IntRange(from = 1) val value: Int = 1,
    /** The API level to require */
    @IntRange(from = 1) val api: Int = 1
)
