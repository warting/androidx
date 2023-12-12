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

package androidx.sqliteMultiplatform.driver

import androidx.sqliteMultiplatform.BaseConformanceTest
import androidx.sqliteMultiplatform.SQLiteDriver
import kotlin.test.Ignore

class AndroidSQLiteDriverTest : BaseConformanceTest() {

    override fun getDriver(): SQLiteDriver {
        return AndroidSQLiteDriver(":memory:")
    }

    @Ignore // TODO(b/304297717): Align exception checking test with native.
    override fun bindInvalidParam() {}

    @Ignore // TODO(b/304297717): Align exception checking test with native.
    override fun readInvalidColumn() {}

    @Ignore // TODO(b/304297717): Align exception checking test with native.
    override fun readColumnNameWithoutStep() {}
}
