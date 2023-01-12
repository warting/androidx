/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.privacysandbox.tools.apicompiler

import androidx.privacysandbox.tools.testing.CompilationTestHelper.assertThat
import androidx.privacysandbox.tools.testing.loadSourcesFromDirectory
import java.io.File
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

/**
 *  Test the Privacy Sandbox API Compiler with an SDK that defines an interface in another package.
 */
@RunWith(JUnit4::class)
class SdkWithPackagesTest {
    @Test
    fun compileServiceInterface_ok() {
        val inputTestDataDir = File("src/test/test-data/sdkwithpackages/input")
        val outputTestDataDir = File("src/test/test-data/sdkwithpackages/output")
        val inputSources = loadSourcesFromDirectory(inputTestDataDir)
        val expectedKotlinSources = loadSourcesFromDirectory(outputTestDataDir)

        val result = compileWithPrivacySandboxKspCompiler(inputSources)
        assertThat(result).succeeds()

        val expectedAidlFilepath = listOf(
            "com/myotherpackage/IMyOtherPackageInterface.java",
            "com/myotherpackage/ParcelableMyOtherPackageDataClass.java",
            "com/mysdk/ICancellationSignal.java",
            "com/mysdk/IListIntTransactionCallback.java",
            "com/mysdk/IMyOtherPackageDataClassTransactionCallback.java",
            "com/mysdk/IUnitTransactionCallback.java",
            "com/mysdk/IMyOtherPackageInterfaceTransactionCallback.java",
            "com/mysdk/IMySdk.java",
            "com/mysdk/ParcelableStackFrame.java",
            "com/mysdk/IStringTransactionCallback.java",
            "com/mysdk/IMyMainPackageInterfaceTransactionCallback.java",
            "com/mysdk/IMyMainPackageInterface.java",
            "com/mysdk/PrivacySandboxThrowableParcel.java"
        )
        assertThat(result).hasAllExpectedGeneratedSourceFilesAndContent(
            expectedKotlinSources,
            expectedAidlFilepath
        )
    }
}