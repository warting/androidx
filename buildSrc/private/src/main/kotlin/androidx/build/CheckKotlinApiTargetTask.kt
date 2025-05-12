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
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

/** Check if the kotlin-stdlib transitive dependencies are the same as the project specified one. */
@DisableCachingByDefault(because = "not worth caching")
abstract class CheckKotlinApiTargetTask : DefaultTask() {

    @get:Input abstract val kotlinTarget: Property<KotlinVersion>

    @get:Internal val projectPath: String = project.path

    @get:Input
    val allDependencies: Provider<List<Pair<String, String>>> =
        project.provider {
            project.configurations
                .filter(project::shouldVerifyConfiguration)
                .filter { it.isCanBeResolved && it.isPublished() }
                .flatMap { config ->
                    config.incoming.resolutionResult.allComponents.mapNotNull { component ->
                        (component.id as? ModuleComponentIdentifier)?.let { id ->
                            "${id.module}:${id.version}" to config.name
                        }
                    }
                }
        }

    @get:OutputFile abstract val outputFile: RegularFileProperty

    @TaskAction
    fun check() {
        val incompatibleConfigurations =
            allDependencies
                .get()
                .asSequence()
                .filter { it.first.startsWith("kotlin-stdlib:") }
                .map { it.first.substringAfter(":") to it.second }
                .map { KotlinVersion.fromVersion(it.first.substringBeforeLast('.')) to it.second }
                .filter { it.first > kotlinTarget.get() }
                .map { "${it.second} (${it.first})" }
                .toList()

        val outputFile = outputFile.get().asFile
        outputFile.parentFile.mkdirs()

        if (incompatibleConfigurations.isNotEmpty()) {
            val errorMessage =
                incompatibleConfigurations.joinToString(
                    separator = "\n - ",
                    prefix =
                        "The project's kotlin-stdlib target is ${kotlinTarget.get()} but these " +
                            "configurations are pulling in higher versions of kotlin-stdlib:\n - ",
                    postfix =
                        "\n\nRun ./gradlew $projectPath:dependencies to see which dependency is " +
                            "pulling in the incompatible kotlin-stdlib"
                )
            outputFile.writeText("FAILURE: $errorMessage")
            throw IllegalStateException(errorMessage)
        }
    }

    companion object {
        const val TASK_NAME = "checkKotlinApiTarget"
    }
}
