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

package androidx.build.binarycompatibilityvalidator

import androidx.build.metalava.checkEqual
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

@CacheableTask
abstract class CheckAbiEquivalenceTask : DefaultTask() {

    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:InputFile
    abstract var checkedInDump: Provider<RegularFileProperty>

    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:InputFile
    abstract var builtDump: Provider<RegularFileProperty>

    @get:Input abstract val shouldWriteVersionedApiFile: Property<Boolean>
    @get:Input abstract val version: Property<String>

    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:InputDirectory
    abstract val projectAbiDir: DirectoryProperty

    @TaskAction
    fun execute() {
        if (shouldWriteVersionedApiFile.get()) {
            val versionedFile = projectAbiDir.get().asFile.resolve("${version.get()}.txt")
            if (!versionedFile.exists()) {
                throw GradleException("Missing versioned api file: ${versionedFile.path}")
            }
        }
        checkEqual(checkedInDump.get().asFile.get(), builtDump.get().asFile.get(), "updateAbi")
    }
}
