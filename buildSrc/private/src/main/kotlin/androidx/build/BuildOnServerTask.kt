/*
 * Copyright 2019 The Android Open Source Project
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

import java.io.File
import java.io.FileNotFoundException
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

/**
 * Task for building all of Androidx libraries and documentation
 *
 * AndroidXImplPlugin configuration adds dependencies to BuildOnServer for all of the tasks that
 * produce artifacts that we want to build on server builds When BuildOnServer executes, it
 * double-checks that all expected artifacts were built
 */
@CacheableTask
open class BuildOnServerTask : DefaultTask() {

    init {
        group = "Build"
        description = "Builds all of the Androidx libraries and documentation"
    }

    @Internal lateinit var distributionDirectory: File

    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    fun getRequiredFiles(): List<File> {
        return mutableListOf("androidx_aggregate_build_info.txt").map { fileName ->
            File(distributionDirectory, fileName)
        }
    }

    @TaskAction
    fun checkAllBuildOutputs() {
        val missingFiles = mutableListOf<String>()
        getRequiredFiles().forEach { file ->
            if (!file.exists()) {
                missingFiles.add(file.path)
            }
        }

        if (missingFiles.isNotEmpty()) {
            val missingFileString = missingFiles.reduce { acc, s -> "$acc, $s" }
            throw FileNotFoundException("buildOnServer required output missing: $missingFileString")
        }
    }
}
