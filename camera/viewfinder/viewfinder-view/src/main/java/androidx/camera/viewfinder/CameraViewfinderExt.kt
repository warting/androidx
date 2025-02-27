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

package androidx.camera.viewfinder

import androidx.camera.viewfinder.core.TransformationInfo
import androidx.camera.viewfinder.core.ViewfinderSurfaceRequest
import androidx.camera.viewfinder.core.ViewfinderSurfaceSession
import androidx.concurrent.futures.await

/**
 * Provides a suspending function of [CameraViewfinder.requestSurfaceSessionAsync] to request a
 * [ViewfinderSurfaceSession] by sending a [ViewfinderSurfaceRequest].
 */
suspend fun CameraViewfinder.requestSurfaceSession(
    viewfinderSurfaceRequest: ViewfinderSurfaceRequest,
    transformationInfo: TransformationInfo = TransformationInfo()
): ViewfinderSurfaceSession =
    requestSurfaceSessionAsync(viewfinderSurfaceRequest, transformationInfo).await()
