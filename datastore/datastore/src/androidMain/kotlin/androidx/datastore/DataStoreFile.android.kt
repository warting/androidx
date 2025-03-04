/*
 * Copyright 2021 The Android Open Source Project
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

@file:JvmName("DataStoreFile") // Workaround for b/313964643

package androidx.datastore

import android.content.Context
import java.io.File

/**
 * Generate the File object for DataStore based on the provided context and name. The file is
 * generated by calling `File(context.applicationContext.filesDir, "datastore/$fileName")`. This is
 * public to allow for testing and backwards compatibility (e.g. if moving from the `dataStore`
 * delegate or context.createDataStore to DataStoreFactory).
 *
 * Do NOT use the file outside of DataStore.
 *
 * @this The context of the application used to get the files directory @fileName the file name
 */
public fun Context.dataStoreFile(fileName: String): File {
    return File(this.applicationContext.filesDir, "datastore/$fileName")
}
