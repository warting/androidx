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

package androidx.xr.projected.permissions

import android.app.ActivityOptions
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Display
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RestrictTo
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.xr.glimmer.GlimmerTheme
import androidx.xr.glimmer.Text
import androidx.xr.projected.R

/**
 * An activity to be displayed on the Projected device that requests the user to act on the
 * permission request on the host device. See [ProjectedPermissionsResultContract] for details.
 */
@Suppress("ForbiddenSuperClass")
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class GoToHostProjectedActivity :
    ComponentActivity(), PermissionResultReceiver.PermissionResultCallback {

    private lateinit var permissionResultReceiver: PermissionResultReceiver

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { GlimmerTheme { Ui() } }

        permissionResultReceiver = PermissionResultReceiver(Handler(Looper.getMainLooper()))
        permissionResultReceiver.localCallback = this
        val activityOptions =
            ActivityOptions.makeBasic().setLaunchDisplayId(Display.DEFAULT_DISPLAY)
        startActivity(
            Intent()
                .setClass(this, RequestPermissionsOnHostActivity::class.java)
                .putExtras(intent)
                .putExtra(EXTRA_RESULT_RECEIVER, permissionResultReceiver)
                .addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP
                ),
            activityOptions.toBundle(),
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        permissionResultReceiver.localCallback = null
    }

    // This is called when the permissionResultReceiver is invoked by the host activity.
    override fun onPermissionResult(permissionResults: Bundle) {
        setResult(
            RESULT_OK,
            Intent()
                .putExtra(
                    ProjectedPermissionsResultContract.EXTRA_PERMISSION_RESULTS,
                    permissionResults,
                ),
        )
        finish()
    }

    @Composable
    private fun Ui() {
        Text(
            text = stringResource(R.string.go_to_host_projected_activity_text),
            textAlign = TextAlign.Center,
        )
    }

    internal companion object {
        internal const val EXTRA_RESULT_RECEIVER =
            "androidx.xr.projected.permissions.extra.RESULT_RECEIVER"
    }
}
