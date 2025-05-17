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

package androidx.camera.viewfinder.compose

import android.os.Build
import android.util.Size
import android.view.Surface
import androidx.annotation.RequiresApi
import androidx.camera.viewfinder.core.ImplementationMode
import androidx.camera.viewfinder.core.TransformationInfo
import androidx.camera.viewfinder.core.ViewfinderSurfaceRequest
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class ViewfinderTest {
    @get:Rule val rule = createComposeRule()

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.M)
    @Test
    fun canRetrievePerformanceSurface() = runBlocking {
        assertCanRetrieveSurface(implementationMode = ImplementationMode.EXTERNAL)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.M)
    @Test
    fun canRetrieveCompatibleSurface() = runBlocking {
        assertCanRetrieveSurface(implementationMode = ImplementationMode.EMBEDDED)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.M)
    @Test
    fun coordinatesTransformationSameSizeNoRotation(): Unit = runBlocking {
        val coordinateTransformer = MutableCoordinateTransformer()

        rule.setContent {
            with(LocalDensity.current) {
                TestViewfinder(
                    modifier = Modifier.size(540.toDp(), 960.toDp()),
                    coordinateTransformer = coordinateTransformer,
                ) {}
            }
        }

        val expectedMatrix =
            Matrix(
                values =
                    floatArrayOf(1f, 0f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 0f, 1f)
            )

        assertThat(coordinateTransformer.transformMatrix.values).isEqualTo(expectedMatrix.values)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.M)
    @Test
    fun coordinatesTransformationSameSizeWithHalfCrop(): Unit = runBlocking {
        // Viewfinder size: 1080x1920
        // Surface size: 1080x1920
        // Crop rect size: 540x960

        val coordinateTransformer = MutableCoordinateTransformer()

        rule.setContent {
            with(LocalDensity.current) {
                TestViewfinder(
                    modifier = Modifier.size(540.toDp(), 960.toDp()),
                    surfaceRequest =
                        ViewfinderSurfaceRequest(
                            width = ViewfinderTestParams.Default.sourceResolution.width,
                            height = ViewfinderTestParams.Default.sourceResolution.height,
                            implementationMode = ImplementationMode.EXTERNAL,
                        ),
                    transformationInfo =
                        TransformationInfo(
                            sourceRotation = 0,
                            isSourceMirroredHorizontally = false,
                            isSourceMirroredVertically = false,
                            cropRectLeft = 0f,
                            cropRectTop = 0f,
                            cropRectRight = 270f,
                            cropRectBottom = 480f,
                        ),
                    coordinateTransformer = coordinateTransformer,
                ) {}
            }
        }

        val expectedMatrix =
            Matrix(
                values =
                    floatArrayOf(0.5f, 0f, 0f, 0f, 0f, 0.5f, 0f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 0f, 1f)
            )

        assertThat(coordinateTransformer.transformMatrix.values).isEqualTo(expectedMatrix.values)
    }

    @Test
    fun verifySurfacesAreReleased_surfaceRequestReleased_thenComposableDestroyed(): Unit =
        runBlocking {
            val surfaceDeferred = CompletableDeferred<Surface>()
            val sessionCompleteDeferred = CompletableDeferred<Unit>()

            val showViewfinder = mutableStateOf(true)

            rule.setContent {
                val showView by remember { showViewfinder }
                TestViewfinder(showViewfinder = showView) {
                    onSurfaceSession {
                        surfaceDeferred.complete(surface)
                        sessionCompleteDeferred.await()
                    }
                }
            }

            val surface = surfaceDeferred.await()
            assertThat(surface.isValid).isTrue()

            sessionCompleteDeferred.complete(Unit)
            rule.awaitIdle()
            assertThat(surface.isValid).isTrue()

            showViewfinder.value = false
            rule.awaitIdle()
            assertThat(surface.isValid).isFalse()
        }

    @Ignore("b/390508238: Surface release needs to be delayed by TextureView/SurfaceView ")
    @Test
    fun verifySurfacesAreReleased_composableDestroyed_thenSurfaceRequestReleased(): Unit =
        runBlocking {
            val surfaceDeferred = CompletableDeferred<Surface>()
            val sessionCompleteDeferred = CompletableDeferred<Unit>()

            val showViewfinder = mutableStateOf(true)

            rule.setContent {
                val showView by remember { showViewfinder }
                TestViewfinder(showViewfinder = showView) {
                    onSurfaceSession { surfaceDeferred.complete(surface) }
                }
            }

            val surface = surfaceDeferred.await()
            assertThat(surface.isValid).isTrue()

            showViewfinder.value = false
            rule.awaitIdle()
            assertThat(surface.isValid).isTrue()

            sessionCompleteDeferred.complete(Unit)
            rule.awaitIdle()
            assertThat(surface.isValid).isFalse()
        }

    @RequiresApi(Build.VERSION_CODES.M) // Needed for Surface.lockHardwareCanvas()
    private suspend fun assertCanRetrieveSurface(implementationMode: ImplementationMode) {
        val surfaceDeferred = CompletableDeferred<Surface>()
        val surfaceRequest =
            ViewfinderSurfaceRequest(
                width = ViewfinderTestParams.Default.sourceResolution.width,
                height = ViewfinderTestParams.Default.sourceResolution.height,
                implementationMode = implementationMode,
            )
        rule.setContent {
            TestViewfinder(surfaceRequest = surfaceRequest) {
                onSurfaceSession { surfaceDeferred.complete(surface) }
            }
        }

        val surface = surfaceDeferred.await()
        surface.lockHardwareCanvas().apply {
            try {
                assertThat(Size(width, height))
                    .isEqualTo(ViewfinderTestParams.Default.sourceResolution)
            } finally {
                surface.unlockCanvasAndPost(this)
            }
        }
    }
}

@Composable
fun TestViewfinder(
    modifier: Modifier = Modifier.size(ViewfinderTestParams.Default.viewfinderSize),
    showViewfinder: Boolean = true,
    transformationInfo: TransformationInfo = ViewfinderTestParams.Default.transformationInfo,
    surfaceRequest: ViewfinderSurfaceRequest = remember {
        ViewfinderSurfaceRequest(
            width = ViewfinderTestParams.Default.sourceResolution.width,
            height = ViewfinderTestParams.Default.sourceResolution.height,
            implementationMode = ImplementationMode.EXTERNAL,
        )
    },
    coordinateTransformer: MutableCoordinateTransformer? = null,
    onInit: ViewfinderInitScope.() -> Unit,
) {
    Column {
        if (showViewfinder) {
            Viewfinder(
                modifier = modifier,
                surfaceRequest = surfaceRequest,
                transformationInfo = transformationInfo,
                coordinateTransformer = coordinateTransformer,
                onInit = onInit,
            )
        }
    }
}
