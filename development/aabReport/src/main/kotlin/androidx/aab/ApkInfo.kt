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

import androidx.aab.AppMetadataPropsInfo.Companion.csvEntries
import androidx.aab.DexInfo.Companion.csvEntries
import androidx.aab.MappingFileInfo.Companion.csvEntries
import androidx.aab.ProfInfo.Companion.csvEntries
import androidx.aab.R8JsonFileInfo.Companion.csvEntries
import androidx.aab.SoInfo.Companion.csvEntries
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

/**
 * Container for all information extracted directly from the bundle, prior to any cross-reference
 * analysis.
 */
data class ApkInfo(
    val path: String,
    val profileInfo: ProfInfo?,
    val dexInfo: List<DexInfo>,
    val soInfo: List<SoInfo>,
    val dotVersionFiles: Map<String, String>, // map maven coordinates -> version number
    val appMetadataPropsInfoMetaInf: AppMetadataPropsInfo?,
) {
    fun csvEntries(): List<String> =
        listOf(path.substringAfterLast(File.separatorChar)) +
            profileInfo.csvEntries() +
            dexInfo.csvEntries() +
            soInfo.csvEntries() +
            appMetadataPropsInfoMetaInf.csvEntries()

    companion object {
        val CSV_TITLES =
            listOf("filename") +
                ProfInfo.CSV_TITLES +
                DexInfo.CSV_TITLES +
                SoInfo.CSV_TITLES +
                AppMetadataPropsInfo.CSV_TITLES_META_INF

        fun from(file: File): ApkInfo {
            return FileInputStream(file).use { from(file.path, it) }
        }

        fun from(path: String, inputStream: InputStream): ApkInfo {
            val dexInfo = mutableListOf<DexInfo>()
            val soInfo = mutableListOf<SoInfo>()
            val dotVersionFiles = mutableMapOf<String, String>()
            var profileInfo: ProfInfo? = null
            var appMetadataPropsInfoMetaInf: AppMetadataPropsInfo? = null
            ZipInputStream(inputStream).use { zis ->
                var entry: ZipEntry? = zis.nextEntry

                while (entry != null) {
                    when {
                        entry.name.endsWith(".dex") -> {
                            dexInfo.add(DexInfo.from(entry.name, entry.compressedSize, zis))
                        }

                        entry.name == ProfInfo.APK_LOCATION -> {
                            profileInfo = ProfInfo.readFromProfile(zis)
                        }

                        entry.name.endsWith(".version") && entry.name.startsWith("META-INF/") -> {
                            dotVersionFiles[entry.name] = zis.bufferedReader().readText().trim()
                        }

                        entry.name == AppMetadataPropsInfo.APK_LOCATION_META_INF -> {
                            appMetadataPropsInfoMetaInf = AppMetadataPropsInfo.from(zis)
                        }

                        entry.name.endsWith(".so") -> {
                            soInfo.add(SoInfo(bundlePath = entry.name, size = zis.countBytes()))
                        }
                    }
                    entry = zis.nextEntry
                }
            }

            return ApkInfo(
                path = path,
                profileInfo = profileInfo,
                dexInfo = dexInfo,
                soInfo = soInfo,
                dotVersionFiles = dotVersionFiles,
                appMetadataPropsInfoMetaInf = appMetadataPropsInfoMetaInf,
            )
        }
    }
}
