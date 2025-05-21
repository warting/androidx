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

package androidx.build

import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

/** Public-facing interface for the `androidx` configuration DSL. */
interface AndroidXConfiguration {
    /**
     * Target Kotlin API version passed to the Kotlin compiler.
     *
     * Specified using `kotlinTarget` in the `androidx` DSL.
     */
    val kotlinApiVersion: Provider<KotlinVersion>

    /**
     * Version of the Kotlin BOM used to resolve dependencies in the `org.jetbrains.kotlin` group.
     *
     * Specified using `kotlinTarget` in the `androidx` DSL.
     */
    val kotlinBomVersion: Provider<String>
}

enum class KotlinTarget(val apiVersion: KotlinVersion, val catalogVersion: String) {
    KOTLIN_2_0(KotlinVersion.KOTLIN_2_0, "kotlin20"),
    KOTLIN_2_1(KotlinVersion.KOTLIN_2_1, "kotlin21"),
    DEFAULT(KOTLIN_2_0),
    LATEST(KOTLIN_2_1);

    constructor(
        kotlinTarget: KotlinTarget
    ) : this(kotlinTarget.apiVersion, kotlinTarget.catalogVersion)
}

val Project.androidXConfiguration: AndroidXConfiguration
    get() = extensions.findByType(AndroidXConfiguration::class.java)!!
