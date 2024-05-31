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

package androidx.privacysandboxlibraryplugin

import java.io.FileNotFoundException
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.process.CommandLineArgumentProvider

internal abstract class FrameworkAidlInputs : CommandLineArgumentProvider {

    @get:Internal abstract val frameworkAidl: RegularFileProperty

    @get:Input abstract val platformSdk: Property<String>

    override fun asArguments(): Iterable<String> {
        val frameworkAidlFile = frameworkAidl.get().asFile
        val frameworkAidlPath = frameworkAidlFile.absolutePath
        if (!frameworkAidlFile.exists()) {
            throw FileNotFoundException("framework.aidl not found at $frameworkAidlPath")
        }
        return listOf("framework_aidl_path=$frameworkAidlPath")
    }
}
