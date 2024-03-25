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

package androidx.navigation

/**
 * Annotation indicating experimental API for new safe args component that enables navigating
 * with type safety through serialization.
 *
 * The Safe Args API surface is still under development and  incomplete at the moment.
 * Many specific features require multiple safe args APIs working
 * together, so individual APIs may not currently work as intended. As such, use of
 * APIs annotated with `ExperimentalSafeArgsAPI` requires opt-in
 */
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.CONSTRUCTOR, AnnotationTarget.FUNCTION)
@RequiresOptIn(level = RequiresOptIn.Level.WARNING)
public annotation class ExperimentalSafeArgsApi
