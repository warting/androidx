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

package androidx.xr.arcore.testapp.common

import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.xr.runtime.Config
import androidx.xr.runtime.Session
import androidx.xr.runtime.SessionConfigureConfigurationNotSupported
import androidx.xr.runtime.SessionConfigureGooglePlayServicesLocationLibraryNotLinked
import androidx.xr.runtime.SessionConfigurePermissionsNotGranted
import androidx.xr.runtime.SessionConfigureSuccess
import androidx.xr.runtime.SessionCreateApkRequired
import androidx.xr.runtime.SessionCreatePermissionsNotGranted
import androidx.xr.runtime.SessionCreateResult
import androidx.xr.runtime.SessionCreateSuccess
import androidx.xr.runtime.SessionCreateUnsupportedDevice

/**
 * Observer class to manage the lifecycle of the JXR Runtime Session based on the lifecycle owner
 * (activity).
 */
class SessionLifecycleHelper(
    val activity: ComponentActivity,
    val config: Config = Config(),
    val onSessionAvailable: (Session) -> Unit = {},
    val onSessionCreateActionRequired: (SessionCreateResult) -> Unit = {},
) {

    /** Accessed through the [onSessionAvailable] callback. */
    private lateinit var session: Session
    private lateinit var requestPermissionLauncher: ActivityResultLauncher<Array<String>>

    init {
        registerRequestPermissionLauncher(activity)
    }

    private fun registerRequestPermissionLauncher(activity: ComponentActivity) {
        requestPermissionLauncher =
            activity.registerForActivityResult(
                ActivityResultContracts.RequestMultiplePermissions()
            ) { permissions ->
                val allPermissionsGranted = permissions.all { it.value }
                if (!allPermissionsGranted) {
                    Toast.makeText(
                            activity,
                            "Required permissions were not granted, closing activity. ",
                            Toast.LENGTH_LONG,
                        )
                        .show()
                    activity.finish()
                } else {
                    activity.recreate()
                }
            }
    }

    internal fun tryCreateSession() {
        when (val result = Session.create(activity)) {
            is SessionCreateSuccess -> {
                session = result.session
                when (val configResult = session.configure(config)) {
                    is SessionConfigurePermissionsNotGranted -> {
                        requestPermissionLauncher.launch(configResult.permissions.toTypedArray())
                    }
                    is SessionConfigureConfigurationNotSupported -> {
                        showErrorMessage("Session configuration not supported.")
                        activity.finish()
                    }
                    is SessionConfigureGooglePlayServicesLocationLibraryNotLinked -> {
                        Log.e(
                            TAG,
                            "Google Play Services Location Library is not linked, this should not happen.",
                        )
                    }
                    is SessionConfigureSuccess -> {
                        onSessionAvailable(session)
                    }
                }
            }
            is SessionCreatePermissionsNotGranted -> {
                requestPermissionLauncher.launch(result.permissions.toTypedArray())
            }
            is SessionCreateApkRequired -> {
                onSessionCreateActionRequired(result)
            }
            is SessionCreateUnsupportedDevice -> {
                showErrorMessage("Session could not be created, device is Unsupported.")
                activity.finish()
            }
        }
    }

    companion object {
        private val TAG = this::class.simpleName
    }

    private fun <F> showErrorMessage(error: F) {
        Log.e(TAG, error.toString())
        Toast.makeText(activity, error.toString(), Toast.LENGTH_LONG).show()
    }
}
