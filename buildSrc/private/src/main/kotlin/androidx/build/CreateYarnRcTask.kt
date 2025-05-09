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

package androidx.build

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault

/**
 * Creates an `.yarnrc` file in a specified directory. The `.yarnrc` file will contain the path to
 * the offline storage of the required dependencies.
 */
@DisableCachingByDefault(because = "not worth caching")
abstract class CreateYarnRcFileTask : DefaultTask() {

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.ABSOLUTE)
    abstract val offlineMirrorStorage: DirectoryProperty

    @get:OutputDirectory abstract val cacheStorage: DirectoryProperty

    @get:OutputFile abstract val yarnrcFile: RegularFileProperty

    @TaskAction
    fun createFile() {
        val offlineStoragePath = offlineMirrorStorage.get().asFile.absolutePath
        val cacheStoragePath = cacheStorage.get().asFile.absolutePath
        yarnrcFile.get().asFile.let {
            it.parentFile.mkdirs()
            it.writeText(
                """
                yarn-offline-mirror "$offlineStoragePath"
                cache-folder "$cacheStoragePath"
            """
                    .trimIndent()
            )
        }
    }
}
