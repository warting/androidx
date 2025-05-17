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

package androidx.wear.compose.material3.macrobenchmark.common

import android.os.SystemClock
import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ConfirmationDialogDefaults
import androidx.wear.compose.material3.FailureConfirmationDialog
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.confirmationDialogCurvedText

object FailureConfirmationBenchmark : MacrobenchmarkScreen {
    override val content: @Composable (BoxScope.() -> Unit)
        get() = {
            val showDialog = remember { mutableStateOf(false) }
            Column(
                Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Button(
                    onClick = { showDialog.value = true },
                    modifier = Modifier.semantics { contentDescription = SHOW_FAILURE_CONFIRMATION },
                ) {
                    Text("Open")
                }
            }
            val text = "Failure"
            val style = ConfirmationDialogDefaults.curvedTextStyle
            FailureConfirmationDialog(
                curvedText = { confirmationDialogCurvedText(text = text, style = style) },
                visible = showDialog.value,
                onDismissRequest = { showDialog.value = false },
                durationMillis = 2000,
            )
        }

    override val exercise: MacrobenchmarkScope.() -> Unit
        get() = {
            device
                .wait(Until.findObject(By.desc(SHOW_FAILURE_CONFIRMATION)), FIND_OBJECT_TIMEOUT_MS)
                .click()
            SystemClock.sleep(2_500L)
        }

    private const val SHOW_FAILURE_CONFIRMATION = "SHOW_FAILURE_CONFIRMATION"
}
