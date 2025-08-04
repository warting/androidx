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

package androidx.aab

import java.io.InputStream

data class AppMetadataPropsInfo(
    val appMetadataVersion: String,
    val androidGradlePluginVersion: String,
) {
    fun agpAtLeast(targetMajor: Int, targetMinor: Int): Boolean {
        val components = androidGradlePluginVersion.split(".")
        val currentMajor = components[0].toInt()
        val currentMinor = components[1].toInt()
        return currentMajor > targetMajor ||
            (currentMajor == targetMajor && currentMinor >= targetMinor)
    }

    companion object {
        const val BUNDLE_LOCATION_META_INF =
            "base/root/META-INF/com/android/build/gradle/app-metadata.properties"
        const val BUNDLE_LOCATION_METADATA =
            "BUNDLE-METADATA/com.android.tools.build.gradle/app-metadata.properties"

        fun from(src: InputStream): AppMetadataPropsInfo {
            var appMetadataVersion = ""
            var androidGradlePluginVersion = ""
            src.bufferedReader().readText().lines().forEach {
                val entries = it.trim().split("=")
                if (entries.size == 2) {
                    if (entries[0] == "appMetadataVersion") {
                        appMetadataVersion = entries[1]
                    } else if (entries[0] == "androidGradlePluginVersion") {
                        androidGradlePluginVersion = entries[1]
                    }
                }
            }
            return AppMetadataPropsInfo(
                appMetadataVersion = appMetadataVersion,
                androidGradlePluginVersion = androidGradlePluginVersion,
            )
        }

        val CSV_TITLES_META_INF =
            listOf("appMetadataPropsLegacy_version", "appMetadataPropsLegacy_agpVerson")
        val CSV_TITLES_BUNDLE = listOf("appMetadataProps_version", "appMetadataProps_agpVerson")

        fun AppMetadataPropsInfo?.csvEntries(): List<String> {
            return if (this == null) {
                listOf("null", "null")
            } else {
                listOf(appMetadataVersion, androidGradlePluginVersion)
            }
        }
    }
}
