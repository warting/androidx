/*
 * Copyright (C) 2021 The Android Open Source Project
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
package androidx.camera.integration.core

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Rect
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCaptureSession.CaptureCallback
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.TotalCaptureResult
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Pair
import android.util.Rational
import android.util.Size
import android.view.Surface
import androidx.annotation.MainThread
import androidx.annotation.OptIn
import androidx.camera.camera2.Camera2Config
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.camera2.pipe.integration.CameraPipeConfig
import androidx.camera.core.AspectRatio
import androidx.camera.core.Camera
import androidx.camera.core.CameraEffect.IMAGE_CAPTURE
import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraXConfig
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCapture.OUTPUT_FORMAT_JPEG
import androidx.camera.core.ImageCapture.OUTPUT_FORMAT_JPEG_ULTRA_HDR
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageCaptureLatencyEstimate
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.UseCase
import androidx.camera.core.UseCaseGroup
import androidx.camera.core.ViewPort
import androidx.camera.core.impl.CameraInfoInternal
import androidx.camera.core.impl.ImageCaptureConfig
import androidx.camera.core.impl.ImageOutputConfig
import androidx.camera.core.impl.ImageOutputConfig.OPTION_RESOLUTION_SELECTOR
import androidx.camera.core.impl.SessionConfig
import androidx.camera.core.impl.SessionProcessor
import androidx.camera.core.impl.utils.CameraOrientationUtil
import androidx.camera.core.impl.utils.Exif
import androidx.camera.core.internal.compat.quirk.SoftwareJpegEncodingPreferredQuirk
import androidx.camera.core.internal.compat.workaround.ExifRotationAvailability
import androidx.camera.core.internal.compat.workaround.InvalidJpegDataParser
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionFilter
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionSelector.PREFER_HIGHER_RESOLUTION_OVER_CAPTURE_RATE
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.integration.core.util.Camera2InteropUtil
import androidx.camera.integration.core.util.CameraInfoUtil
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.testing.impl.AndroidUtil.isEmulator
import androidx.camera.testing.impl.CameraAvailabilityUtil.assumeDeviceHasFrontCamera
import androidx.camera.testing.impl.CameraPipeConfigTestRule
import androidx.camera.testing.impl.CameraUtil
import androidx.camera.testing.impl.CoreAppTestUtil
import androidx.camera.testing.impl.CountdownDeferred
import androidx.camera.testing.impl.ExtensionsUtil
import androidx.camera.testing.impl.InternalTestConvenience.ignoreTestForCameraPipe
import androidx.camera.testing.impl.StreamSharingForceEnabledEffect
import androidx.camera.testing.impl.SurfaceTextureProvider
import androidx.camera.testing.impl.WakelockEmptyActivityRule
import androidx.camera.testing.impl.fakes.FakeLifecycleOwner
import androidx.camera.testing.impl.fakes.FakeOnImageCapturedCallback
import androidx.camera.testing.impl.fakes.FakeSessionProcessor
import androidx.camera.testing.impl.mocks.MockScreenFlash
import androidx.camera.video.Recorder
import androidx.camera.video.VideoCapture
import androidx.core.content.ContextCompat
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.test.rule.GrantPermissionRule
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.abs
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.After
import org.junit.Assume.assumeFalse
import org.junit.Assume.assumeNoException
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

private val DEFAULT_RESOLUTION = Size(640, 480)
private val BACK_SELECTOR = CameraSelector.DEFAULT_BACK_CAMERA
private val FRONT_SELECTOR = CameraSelector.DEFAULT_FRONT_CAMERA
private const val BACK_LENS_FACING = CameraSelector.LENS_FACING_BACK
private val CAPTURE_TIMEOUT = 15.seconds
private const val TOLERANCE = 1e-3f
private val EXIF_GAINMAP_PATTERNS =
    listOf(
        "xmlns:hdrgm=\"http://ns.adobe.com/hdr-gain-map/",
        "hdrgm:Version=",
        "Item:Semantic=\"GainMap\"",
    )

@LargeTest
@RunWith(Parameterized::class)
class ImageCaptureTest(private val implName: String, private val cameraXConfig: CameraXConfig) {

    @get:Rule
    val cameraPipeConfigTestRule =
        CameraPipeConfigTestRule(
            active = implName == CameraPipeConfig::class.simpleName,
        )

    @get:Rule
    val cameraRule =
        CameraUtil.grantCameraPermissionAndPreTestAndPostTest(
            CameraUtil.PreTestCameraIdList(cameraXConfig)
        )

    @get:Rule
    val externalStorageRule: GrantPermissionRule =
        GrantPermissionRule.grant(Manifest.permission.WRITE_EXTERNAL_STORAGE)

    @get:Rule
    val temporaryFolder =
        TemporaryFolder(ApplicationProvider.getApplicationContext<Context>().cacheDir)

    @get:Rule val wakelockEmptyActivityRule = WakelockEmptyActivityRule()

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data() =
            listOf(
                arrayOf(Camera2Config::class.simpleName, Camera2Config.defaultConfig()),
                arrayOf(CameraPipeConfig::class.simpleName, CameraPipeConfig.defaultConfig())
            )
    }

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val mainExecutor = ContextCompat.getMainExecutor(context)
    private val defaultBuilder = ImageCapture.Builder()
    private lateinit var cameraProvider: ProcessCameraProvider
    private lateinit var fakeLifecycleOwner: FakeLifecycleOwner

    @Before
    fun setUp(): Unit = runBlocking {
        CoreAppTestUtil.assumeCompatibleDevice()
        assumeTrue(CameraUtil.hasCameraWithLensFacing(BACK_LENS_FACING))
        createDefaultPictureFolderIfNotExist()
        ProcessCameraProvider.configureInstance(cameraXConfig)
        cameraProvider = ProcessCameraProvider.getInstance(context)[10, TimeUnit.SECONDS]
        withContext(Dispatchers.Main) {
            fakeLifecycleOwner = FakeLifecycleOwner()
            fakeLifecycleOwner.startAndResume()
        }
    }

    @After
    fun tearDown(): Unit = runBlocking {
        if (::cameraProvider.isInitialized) {
            withContext(Dispatchers.Main) { cameraProvider.shutdownAsync()[10, TimeUnit.SECONDS] }
        }
    }

    @Test
    fun capturedImageHasCorrectSize() {
        takeImageAndVerifySize()
    }

    @SdkSuppress(minSdkVersion = 34)
    @Test
    fun capturedImageHasCorrectSize_whenOutputFormatIsUltraHdr() {
        takeImageAndVerifySize(outputFormat = OUTPUT_FORMAT_JPEG_ULTRA_HDR)
    }

    @Suppress("DEPRECATION") // test for legacy resolution API
    private fun takeImageAndVerifySize(
        cameraSelector: CameraSelector = BACK_SELECTOR,
        outputFormat: @ImageCapture.OutputFormat Int = OUTPUT_FORMAT_JPEG,
    ): Unit = runBlocking {
        // Arrange.
        val useCaseBuilder =
            ImageCapture.Builder()
                .setTargetResolution(DEFAULT_RESOLUTION)
                .setTargetRotation(Surface.ROTATION_0)

        // Only test Ultra HDR on supported devices.
        if (outputFormat == OUTPUT_FORMAT_JPEG_ULTRA_HDR) {
            assumeUltraHdrSupported(cameraSelector)
            useCaseBuilder.setOutputFormat(OUTPUT_FORMAT_JPEG_ULTRA_HDR)
        }

        val useCase = useCaseBuilder.build()
        withContext(Dispatchers.Main) {
            cameraProvider.bindToLifecycle(fakeLifecycleOwner, cameraSelector, useCase)
        }

        // Act.
        val callback = FakeOnImageCapturedCallback(captureCount = 1)
        useCase.takePicture(mainExecutor, callback)

        // Assert.
        // Wait for the signal that the image has been captured.
        callback.awaitCapturesAndAssert(capturedImagesCount = 1)

        val imageProperties = callback.results.first().properties
        var sizeEnvelope = imageProperties.size

        // Some devices may not be able to fit the requested resolution. In this case, the returned
        // size should be able to enclose 640 x 480.
        if (sizeEnvelope != DEFAULT_RESOLUTION) {
            val rotationDegrees = imageProperties.rotationDegrees

            // If the image data is rotated by 90 or 270, we need to ensure our desired width fits
            // within the height of this image and our desired height fits in the width.
            if (rotationDegrees == 270 || rotationDegrees == 90) {
                sizeEnvelope = Size(sizeEnvelope!!.height, sizeEnvelope.width)
            }

            // Ensure the width and height can be cropped from the source image
            assertThat(sizeEnvelope!!.width).isAtLeast(DEFAULT_RESOLUTION.width)
            assertThat(sizeEnvelope.height).isAtLeast(DEFAULT_RESOLUTION.height)
        }
    }

    @MainThread
    private suspend fun assumeUltraHdrSupported(cameraSelector: CameraSelector) {
        withContext(Dispatchers.Main) {
            val camera = cameraProvider.bindToLifecycle(fakeLifecycleOwner, cameraSelector)
            val capabilities = ImageCapture.getImageCaptureCapabilities(camera.cameraInfo)
            assumeTrue(capabilities.supportedOutputFormats.contains(OUTPUT_FORMAT_JPEG_ULTRA_HDR))
        }
    }

    @Test
    fun canCaptureMultipleImages() {
        canTakeImages(defaultBuilder, numImages = 5)
    }

    @SdkSuppress(minSdkVersion = 34)
    @Test
    fun canCaptureMultipleImages_whenOutputFormatIsUltraHdr() {
        canTakeImages(defaultBuilder.setOutputFormat(OUTPUT_FORMAT_JPEG_ULTRA_HDR), numImages = 5) {
            assumeUltraHdrSupported(BACK_SELECTOR)
        }
    }

    @Test
    fun canCaptureMultipleImagesWithMaxQuality() {
        canTakeImages(
            ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY),
            numImages = 5
        )
    }

    @SdkSuppress(minSdkVersion = 34)
    @Test
    fun canCaptureMultipleImagesWithMaxQuality_whenOutputFormatIsUltraHdr() {
        val builder =
            ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                .setOutputFormat(OUTPUT_FORMAT_JPEG_ULTRA_HDR)
        canTakeImages(builder, numImages = 5) { assumeUltraHdrSupported(BACK_SELECTOR) }
    }

    @Test
    fun canCaptureImageWithFlashModeOn() {
        canTakeImages(defaultBuilder.setFlashMode(ImageCapture.FLASH_MODE_ON))
    }

    @Test
    fun canCaptureMaxQualityImageWithFlashModeOn() {
        canTakeImages(
            defaultBuilder
                .setFlashMode(ImageCapture.FLASH_MODE_ON)
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
        )
    }

    @Test
    fun canCaptureImageWithFlashModeOn_frontCamera() {
        assumeDeviceHasFrontCamera()

        // This test also wants to ensure that the image can be captured without the flash unit.
        // Front camera usually doesn't have a flash unit.
        canTakeImages(
            defaultBuilder.setFlashMode(ImageCapture.FLASH_MODE_ON),
            cameraSelector = FRONT_SELECTOR
        )
    }

    @Test
    fun canCaptureImageWithFlashModeOnAndUseTorch() {
        canTakeImages(
            defaultBuilder
                .setFlashType(ImageCapture.FLASH_TYPE_USE_TORCH_AS_FLASH)
                .setFlashMode(ImageCapture.FLASH_MODE_ON),
        )
    }

    @Test
    fun canCaptureImageWithFlashModeOnAndUseTorch_frontCamera() {
        assumeDeviceHasFrontCamera()

        // This test also wants to ensure that the image can be captured without the flash unit.
        // Front camera usually doesn't have a flash unit.
        canTakeImages(
            defaultBuilder
                .setFlashType(ImageCapture.FLASH_TYPE_USE_TORCH_AS_FLASH)
                .setFlashMode(ImageCapture.FLASH_MODE_ON),
            cameraSelector = FRONT_SELECTOR
        )
    }

    @Test
    fun canCaptureImageWithFlashModeScreen_frontCamera() {
        assumeDeviceHasFrontCamera()

        // Front camera usually doesn't have a flash unit. Screen flash will be used in such case.
        // Otherwise, physical flash will be used. But capture should be successful either way.
        canTakeImages(
            defaultBuilder.apply {
                setScreenFlash(MockScreenFlash())
                setFlashMode(ImageCapture.FLASH_MODE_SCREEN)
            },
            cameraSelector = FRONT_SELECTOR
        )
    }

    @Test
    fun canCaptureImageWithFlashModeScreenAndUseTorch_frontCamera() {
        assumeDeviceHasFrontCamera()

        // Front camera usually doesn't have a flash unit. Screen flash will be used in such case.
        // Otherwise, physical flash will be used as torch. Either way, capture should be successful
        canTakeImages(
            defaultBuilder.apply {
                setFlashType(ImageCapture.FLASH_TYPE_USE_TORCH_AS_FLASH)
                setScreenFlash(MockScreenFlash())
                setFlashMode(ImageCapture.FLASH_MODE_SCREEN)
            },
            cameraSelector = FRONT_SELECTOR
        )
    }

    private fun canTakeImages(
        builder: ImageCapture.Builder,
        cameraSelector: CameraSelector = BACK_SELECTOR,
        numImages: Int = 1,
        addSharedEffect: Boolean = false,
        runAtStart: suspend () -> Unit = {},
    ): Unit = runBlocking {
        runAtStart()

        // Arrange.
        val imageCapture = builder.build()
        val useCaseGroup =
            UseCaseGroup.Builder()
                .addUseCase(imageCapture)
                .apply {
                    if (addSharedEffect) {
                        addUseCase(VideoCapture.withOutput(Recorder.Builder().build()))
                        addEffect(StreamSharingForceEnabledEffect(IMAGE_CAPTURE))
                    }
                }
                .build()

        withContext(Dispatchers.Main) {
            cameraProvider.bindToLifecycle(fakeLifecycleOwner, cameraSelector, useCaseGroup)
        }

        // Act.
        val callback = FakeOnImageCapturedCallback(captureCount = numImages)
        repeat(numImages) { imageCapture.takePicture(mainExecutor, callback) }

        // Assert.
        callback.awaitCapturesAndAssert(
            timeout = CAPTURE_TIMEOUT.times(numImages),
            capturedImagesCount = numImages
        )
    }

    @Test
    fun canTakeImage_whenSessionErrorListenerReceivesError(): Unit = runBlocking {
        val imageCapture = ImageCapture.Builder().build()
        // Arrange.
        withContext(Dispatchers.Main) {
            cameraProvider.bindToLifecycle(
                fakeLifecycleOwner,
                CameraSelector.DEFAULT_BACK_CAMERA,
                imageCapture
            )
        }

        // Retrieves the initial session config
        val initialSessionConfig = imageCapture.sessionConfig

        // Checks that image can be taken successfully when onError is received.
        triggerOnErrorAndTakePicture(imageCapture, initialSessionConfig)

        if (CameraUtil.hasCameraWithLensFacing(CameraSelector.LENS_FACING_FRONT)) {
            withContext(Dispatchers.Main) {
                // Binds the ImageCapture use case to the other camera
                cameraProvider.unbind(imageCapture)
                cameraProvider.bindToLifecycle(
                    fakeLifecycleOwner,
                    CameraSelector.DEFAULT_FRONT_CAMERA,
                    imageCapture
                )
            }

            // Checks that image can be taken successfully when onError is received by the old
            // error listener.
            triggerOnErrorAndTakePicture(imageCapture, initialSessionConfig)
        }

        // Checks that image can be taken successfully when onError is received by the new
        // error listener.
        triggerOnErrorAndTakePicture(imageCapture, imageCapture.sessionConfig)
    }

    private suspend fun triggerOnErrorAndTakePicture(
        imageCapture: ImageCapture,
        sessionConfig: SessionConfig
    ) {
        withContext(Dispatchers.Main) {
            // Forces invoke the onError callback
            sessionConfig.errorListener!!.onError(
                sessionConfig,
                SessionConfig.SessionError.SESSION_ERROR_UNKNOWN
            )
        }

        // Act.
        val callback = FakeOnImageCapturedCallback()
        imageCapture.takePicture(mainExecutor, callback)

        // Assert.
        // Image can still be taken when an error reported to the session config error listener
        callback.awaitCapturesAndAssert(capturedImagesCount = 1)
    }

    @Test
    fun saveCanSucceed_withNonExistingFile() {
        val saveLocation = temporaryFolder.newFile("test${System.currentTimeMillis()}.jpg")

        // make sure file does not exist
        if (saveLocation.exists()) {
            saveLocation.delete()
        }
        assertThat(!saveLocation.exists())

        canSaveToFile(saveLocation)
    }

    @Test
    fun saveCanSucceed_withExistingFile() {
        val saveLocation = temporaryFolder.newFile("test.jpg")
        assertThat(saveLocation.exists())

        canSaveToFile(saveLocation)
    }

    @Test
    fun saveCanSucceed_toExternalStoragePublicFolderFile() {
        val pictureFolder =
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
        assumeTrue(pictureFolder.exists())
        val saveLocation = File(pictureFolder, "test.jpg")
        canSaveToFile(saveLocation)
        saveLocation.delete()
    }

    private fun canSaveToFile(saveLocation: File) = runBlocking {
        val useCase = defaultBuilder.build()
        withContext(Dispatchers.Main) {
            cameraProvider.bindToLifecycle(fakeLifecycleOwner, BACK_SELECTOR, useCase)
        }

        val callback = FakeImageSavedCallback(capturesCount = 1)
        useCase.takePicture(
            ImageCapture.OutputFileOptions.Builder(saveLocation).build(),
            mainExecutor,
            callback
        )

        // Wait for the signal that the image has been saved.
        callback.awaitCapturesAndAssert(savedImagesCount = 1)
    }

    @SdkSuppress(minSdkVersion = 34)
    @Test
    fun canSaveToFile_withGainmapInfoInMetadata_whenOutputFormatIsUltraHdr(): Unit = runBlocking {
        val cameraSelector = BACK_SELECTOR
        assumeUltraHdrSupported(cameraSelector)

        // Arrange.
        val useCase = ImageCapture.Builder().setOutputFormat(OUTPUT_FORMAT_JPEG_ULTRA_HDR).build()
        withContext(Dispatchers.Main) {
            cameraProvider.bindToLifecycle(fakeLifecycleOwner, cameraSelector, useCase)
        }

        // Act.
        val saveLocation = temporaryFolder.newFile("test.jpg")
        val outputFileOptions = ImageCapture.OutputFileOptions.Builder(saveLocation).build()
        val callback = FakeImageSavedCallback(capturesCount = 1)
        useCase.takePicture(outputFileOptions, mainExecutor, callback)

        // Assert.
        // Wait for the signal that the image has been saved.
        callback.awaitCapturesAndAssert(savedImagesCount = 1)

        // Retrieve the exif from the image and assert.
        val exifMetadata = Exif.createFromFile(saveLocation).metadata
        assertThat(exifMetadata).isNotNull()
        for (pattern in EXIF_GAINMAP_PATTERNS) {
            assertThat(exifMetadata).contains(pattern)
        }
    }

    @SdkSuppress(minSdkVersion = 34)
    @Test
    fun canSaveToFile_hasGainmap_whenOutputFormatIsUltraHdr(): Unit = runBlocking {
        // Emulator has issue on checking gainmap for saved to file.
        assumeFalse(isEmulator())

        val cameraSelector = BACK_SELECTOR
        assumeUltraHdrSupported(cameraSelector)

        // Arrange.
        val useCase = ImageCapture.Builder().setOutputFormat(OUTPUT_FORMAT_JPEG_ULTRA_HDR).build()
        withContext(Dispatchers.Main) {
            cameraProvider.bindToLifecycle(fakeLifecycleOwner, cameraSelector, useCase)
        }

        // Act.
        val saveLocation = temporaryFolder.newFile("test.jpg")
        val outputFileOptions = ImageCapture.OutputFileOptions.Builder(saveLocation).build()
        val callback = FakeImageSavedCallback(capturesCount = 1)
        useCase.takePicture(outputFileOptions, mainExecutor, callback)

        // Assert.
        // Wait for the signal that the image has been saved.
        callback.awaitCapturesAndAssert(savedImagesCount = 1)

        // Check gainmap is existed.
        val bitmap = BitmapFactory.decodeFile(saveLocation.absolutePath)
        assertThat(bitmap).isNotNull()
        assertThat(bitmap.hasGainmap()).isTrue()
    }

    @Test
    fun canSaveToUri() {
        saveToUri()
    }

    @SdkSuppress(minSdkVersion = 34)
    @Test
    fun canSaveToUri_whenOutputFormatIsUltraHdr() {
        saveToUri(outputFormat = OUTPUT_FORMAT_JPEG_ULTRA_HDR)
    }

    private fun saveToUri(
        cameraSelector: CameraSelector = BACK_SELECTOR,
        outputFormat: @ImageCapture.OutputFormat Int = OUTPUT_FORMAT_JPEG,
    ): Unit = runBlocking {
        // Arrange.
        val useCaseBuilder = defaultBuilder

        // Only test Ultra HDR on supported devices.
        if (outputFormat == OUTPUT_FORMAT_JPEG_ULTRA_HDR) {
            assumeUltraHdrSupported(cameraSelector)
            useCaseBuilder.setOutputFormat(OUTPUT_FORMAT_JPEG_ULTRA_HDR)
        }

        val useCase = defaultBuilder.build()
        withContext(Dispatchers.Main) {
            cameraProvider.bindToLifecycle(fakeLifecycleOwner, cameraSelector, useCase)
        }

        val contentValues = ContentValues()
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
        val outputFileOptions =
            ImageCapture.OutputFileOptions.Builder(
                    context.contentResolver,
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    contentValues
                )
                .build()

        val callback = FakeImageSavedCallback(capturesCount = 1)

        // Act.
        useCase.takePicture(outputFileOptions, mainExecutor, callback)

        // Assert: Wait for the signal that the image has been saved
        callback.awaitCapturesAndAssert(savedImagesCount = 1)

        // Verify save location Uri is available.
        val saveLocationUri = callback.results.first().savedUri
        assertThat(saveLocationUri).isNotNull()

        // Clean up.
        context.contentResolver.delete(saveLocationUri!!, null, null)
    }

    @Test
    fun canSaveToOutputStream() {
        saveToOutputStream()
    }

    @SdkSuppress(minSdkVersion = 34)
    @Test
    fun canSaveToOutputStream_whenOutputFormatIsUltraHdr() {
        saveToOutputStream(outputFormat = OUTPUT_FORMAT_JPEG_ULTRA_HDR)
    }

    private fun saveToOutputStream(
        cameraSelector: CameraSelector = BACK_SELECTOR,
        outputFormat: @ImageCapture.OutputFormat Int = OUTPUT_FORMAT_JPEG,
    ) = runBlocking {
        // Arrange.
        val useCaseBuilder = defaultBuilder

        // Only test Ultra HDR on supported devices.
        if (outputFormat == OUTPUT_FORMAT_JPEG_ULTRA_HDR) {
            assumeUltraHdrSupported(cameraSelector)
            useCaseBuilder.setOutputFormat(OUTPUT_FORMAT_JPEG_ULTRA_HDR)
        }

        val useCase = defaultBuilder.build()
        withContext(Dispatchers.Main) {
            cameraProvider.bindToLifecycle(fakeLifecycleOwner, cameraSelector, useCase)
        }

        val saveLocation = temporaryFolder.newFile("test.jpg")

        val callback = FakeImageSavedCallback(capturesCount = 1)

        FileOutputStream(saveLocation).use { outputStream ->
            // Act.
            useCase.takePicture(
                ImageCapture.OutputFileOptions.Builder(outputStream).build(),
                mainExecutor,
                callback
            )

            // Assert: Wait for the signal that the image has been saved.
            callback.awaitCapturesAndAssert(savedImagesCount = 1)
        }
    }

    @Test
    fun canSaveFile_withRotation() = runBlocking {
        // TODO(b/147448711) Add back in once cuttlefish has correct user cropping functionality.
        assumeFalse(
            "Cuttlefish does not correctly handle crops. Unable to test.",
            Build.MODEL.contains("Cuttlefish")
        )

        val useCase = ImageCapture.Builder().setTargetRotation(Surface.ROTATION_0).build()
        withContext(Dispatchers.Main) {
            cameraProvider.bindToLifecycle(fakeLifecycleOwner, BACK_SELECTOR, useCase)
        }

        val saveLocation = temporaryFolder.newFile("test.jpg")

        val callback = FakeImageSavedCallback(capturesCount = 1)

        useCase.takePicture(
            ImageCapture.OutputFileOptions.Builder(saveLocation).build(),
            mainExecutor,
            callback
        )

        // Wait for the signal that the image has been saved.
        callback.awaitCapturesAndAssert(savedImagesCount = 1)

        // Retrieve the exif from the image
        val exif = Exif.createFromFile(saveLocation)

        val saveLocationRotated90 = temporaryFolder.newFile("testRotated90.jpg")

        val callbackRotated90 = FakeImageSavedCallback(capturesCount = 1)

        useCase.targetRotation = Surface.ROTATION_90
        useCase.takePicture(
            ImageCapture.OutputFileOptions.Builder(saveLocationRotated90).build(),
            mainExecutor,
            callbackRotated90
        )

        // Wait for the signal that the image has been saved.
        callbackRotated90.awaitCapturesAndAssert(savedImagesCount = 1)

        // Retrieve the exif from the image
        val exifRotated90 = Exif.createFromFile(saveLocationRotated90)

        // Compare aspect ratio with a threshold due to floating point rounding. Can't do direct
        // comparison of height and width, because the rotated capture is scaled to fit within
        // the sensor region
        val aspectRatioThreshold = 0.01

        // If rotation is equal then buffers were rotated by HAL so the aspect ratio should be
        // rotated by 90 degrees. Otherwise the aspect ratio should be the same.
        if (exif.rotation == exifRotated90.rotation) {
            val aspectRatio = exif.height.toDouble() / exif.width
            val aspectRatioRotated90 = exifRotated90.width.toDouble() / exifRotated90.height
            assertThat(abs(aspectRatio - aspectRatioRotated90)).isLessThan(aspectRatioThreshold)
        } else {
            val aspectRatio = exif.width.toDouble() / exif.height
            val aspectRatioRotated90 = exifRotated90.width.toDouble() / exifRotated90.height
            assertThat(abs(aspectRatio - aspectRatioRotated90)).isLessThan(aspectRatioThreshold)
        }
    }

    @Test
    fun canSaveFile_flippedHorizontal() = runBlocking {
        // Use a non-rotated configuration since some combinations of rotation + flipping vertically
        // can be equivalent to flipping horizontally
        val configBuilder = ImageCapture.Builder.fromConfig(createNonRotatedConfiguration())

        val metadata = ImageCapture.Metadata()
        metadata.isReversedHorizontal = true

        canSaveFileWithMetadata(
            configBuilder = configBuilder,
            metadata = metadata,
            verifyExif = { exif -> assertThat(exif.isFlippedHorizontally).isTrue() }
        )
    }

    @Test
    fun canSaveFile_flippedVertical() = runBlocking {
        // Use a non-rotated configuration since some combinations of rotation + flipping
        // horizontally can be equivalent to flipping vertically
        val configBuilder = ImageCapture.Builder.fromConfig(createNonRotatedConfiguration())

        val metadata = ImageCapture.Metadata()
        metadata.isReversedVertical = true

        canSaveFileWithMetadata(
            configBuilder = configBuilder,
            metadata = metadata,
            verifyExif = { exif -> assertThat(exif.isFlippedVertically).isTrue() }
        )
    }

    // See b/263289024, writing location data might cause the output JPEG image corruption on some
    // specific Android 12 devices. This issue happens if:
    // 1. The image is not cropped from the original captured image (unnecessary Exif copy is done)
    // 2. The inserted location provider is FUSED_PROVIDER
    @Test
    fun canSaveFile_withFusedProviderLocation() {
        val latitudeValue = 50.0
        val longitudeValue = -100.0

        val metadata =
            ImageCapture.Metadata().apply {
                location =
                    Location(LocationManager.FUSED_PROVIDER).apply {
                        latitude = latitudeValue
                        longitude = longitudeValue
                    }
            }

        canSaveFileWithMetadata(
            defaultBuilder,
            metadata,
            verifyExif = { exif ->
                assertThat(exif.location).isNotNull()
                assertThat(exif.location!!.provider).isEqualTo(metadata.location!!.provider)
                assertThat(exif.location!!.latitude).isEqualTo(latitudeValue)
                assertThat(exif.location!!.longitude).isEqualTo(longitudeValue)
            }
        )
    }

    private fun canSaveFileWithMetadata(
        configBuilder: ImageCapture.Builder,
        metadata: ImageCapture.Metadata,
        verifyExif: (Exif) -> Unit
    ) = runBlocking {
        val useCase = configBuilder.build()
        withContext(Dispatchers.Main) {
            cameraProvider.bindToLifecycle(fakeLifecycleOwner, BACK_SELECTOR, useCase)
        }

        val saveLocation = temporaryFolder.newFile("test.jpg")
        val outputFileOptions =
            ImageCapture.OutputFileOptions.Builder(saveLocation).setMetadata(metadata).build()

        val callback = FakeImageSavedCallback(capturesCount = 1)

        useCase.takePicture(outputFileOptions, mainExecutor, callback)

        // Wait for the signal that the image has been saved.
        callback.awaitCapturesAndAssert(savedImagesCount = 1)

        // Retrieve the exif from the image
        val exif = Exif.createFromFile(saveLocation)
        verifyExif(exif)
    }

    @Test
    fun canSaveMultipleFiles() = runBlocking {
        val useCase = defaultBuilder.build()
        withContext(Dispatchers.Main) {
            cameraProvider.bindToLifecycle(fakeLifecycleOwner, BACK_SELECTOR, useCase)
        }

        val numImages = 5
        val callback = FakeImageSavedCallback(capturesCount = numImages)

        for (i in 0 until numImages) {
            val saveLocation = temporaryFolder.newFile("test$i.jpg")
            useCase.takePicture(
                ImageCapture.OutputFileOptions.Builder(saveLocation).build(),
                mainExecutor,
                callback
            )
        }

        // Wait for the signal that all the images have been saved.
        callback.awaitCapturesAndAssert(
            timeout = CAPTURE_TIMEOUT.times(numImages),
            savedImagesCount = numImages
        )
    }

    @Test
    fun saveWillFail_whenInvalidFilePathIsUsed() = runBlocking {
        val useCase = defaultBuilder.build()
        withContext(Dispatchers.Main) {
            cameraProvider.bindToLifecycle(fakeLifecycleOwner, BACK_SELECTOR, useCase)
        }

        // Note the invalid path
        val saveLocation = File("/not/a/real/path.jpg")
        val outputFileOptions = ImageCapture.OutputFileOptions.Builder(saveLocation).build()

        val callback = FakeImageSavedCallback(capturesCount = 1)

        useCase.takePicture(outputFileOptions, mainExecutor, callback)

        // Wait for the signal that saving the image has failed
        callback.awaitCapturesAndAssert(errorsCount = 1)

        val error = callback.errors.first().imageCaptureError
        assertThat(error).isEqualTo(ImageCapture.ERROR_FILE_IO)
    }

    @kotlin.OptIn(
        androidx.camera.camera2.pipe.integration.interop.ExperimentalCamera2Interop::class
    )
    @Test
    @OptIn(markerClass = [ExperimentalCamera2Interop::class])
    fun camera2InteropCaptureSessionCallbacks() = runBlocking {
        val stillCaptureCount = AtomicInteger(0)
        val captureCallback =
            object : CaptureCallback() {
                override fun onCaptureCompleted(
                    session: CameraCaptureSession,
                    request: CaptureRequest,
                    result: TotalCaptureResult
                ) {
                    super.onCaptureCompleted(session, request, result)
                    if (
                        request.get(CaptureRequest.CONTROL_CAPTURE_INTENT) ==
                            CaptureRequest.CONTROL_CAPTURE_INTENT_STILL_CAPTURE
                    ) {
                        stillCaptureCount.incrementAndGet()
                    }
                }
            }
        val builder = ImageCapture.Builder()
        Camera2InteropUtil.setCameraCaptureSessionCallback(implName, builder, captureCallback)

        val useCase = builder.build()

        withContext(Dispatchers.Main) {
            cameraProvider.bindToLifecycle(fakeLifecycleOwner, BACK_SELECTOR, useCase)
        }

        val callback = FakeOnImageCapturedCallback(captureCount = 1)

        useCase.takePicture(mainExecutor, callback)

        // Wait for the signal that the image has been captured.
        callback.awaitCapturesAndAssert(capturedImagesCount = 1)

        // Because interop listener will get both image capture and preview callbacks, ensure
        // that there is one CAPTURE_INTENT_STILL_CAPTURE from all onCaptureCompleted() callbacks.
        assertThat(stillCaptureCount.get()).isEqualTo(1)
    }

    @Test
    fun takePicture_OnImageCaptureCallback_startedBeforeSuccess() = runBlocking {
        // Arrange.
        var captured = false
        val useCase = ImageCapture.Builder().build()
        withContext(Dispatchers.Main) {
            cameraProvider.bindToLifecycle(fakeLifecycleOwner, BACK_SELECTOR, useCase)
        }

        // Act.
        val semaphore = Semaphore(0)
        val callback =
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureStarted() {
                    // Assert: onCaptureStarted should be invoked before onCaptureSuccess
                    assertThat(captured).isFalse()
                    semaphore.release()
                }

                override fun onCaptureSuccess(image: ImageProxy) {
                    captured = true
                }
            }
        useCase.takePicture(mainExecutor, callback)

        // Assert.
        val result = semaphore.tryAcquire(3, TimeUnit.SECONDS)
        assertThat(result).isTrue()
    }

    @Test
    fun takePicture_OnImageSaveCallback_startedBeforeSaved() = runBlocking {
        // Arrange.
        var captured = false
        val useCase = ImageCapture.Builder().build()
        withContext(Dispatchers.Main) {
            cameraProvider.bindToLifecycle(fakeLifecycleOwner, BACK_SELECTOR, useCase)
        }

        // Act.
        val semaphore = Semaphore(0)
        val callback =
            object : ImageCapture.OnImageSavedCallback {
                override fun onCaptureStarted() {
                    // Assert: onCaptureStarted should be invoked before onCaptureSuccess
                    assertThat(captured).isFalse()
                    semaphore.release()
                }

                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    captured = true
                }

                override fun onError(exception: ImageCaptureException) {}
            }
        val saveLocation = temporaryFolder.newFile("test.jpg")
        assertThat(saveLocation.exists())
        useCase.takePicture(
            ImageCapture.OutputFileOptions.Builder(saveLocation).build(),
            mainExecutor,
            callback
        )

        // Assert.
        val result = semaphore.tryAcquire(3, TimeUnit.SECONDS)
        assertThat(result).isTrue()
    }

    @SdkSuppress(minSdkVersion = 28)
    @Test(expected = IllegalArgumentException::class)
    fun constructor_withBufferFormatAndSessionProcessorIsSet_throwsException(): Unit = runBlocking {
        val sessionProcessor =
            FakeSessionProcessor(
                inputFormatPreview = null, // null means using the same output surface
                inputFormatCapture = ImageFormat.YUV_420_888
            )

        val imageCapture = ImageCapture.Builder().setBufferFormat(ImageFormat.RAW_SENSOR).build()
        val preview = Preview.Builder().build()
        withContext(Dispatchers.Main) {
            val cameraSelector =
                ExtensionsUtil.getCameraSelectorWithSessionProcessor(
                    cameraProvider,
                    BACK_SELECTOR,
                    sessionProcessor
                )
            cameraProvider.bindToLifecycle(
                fakeLifecycleOwner,
                cameraSelector,
                imageCapture,
                preview
            )
        }
    }

    @Test
    fun onStateOffline_abortAllCaptureRequests() = runBlocking {
        val imageCapture = ImageCapture.Builder().build()
        withContext(Dispatchers.Main) {
            cameraProvider.bindToLifecycle(fakeLifecycleOwner, BACK_SELECTOR, imageCapture)
        }

        // After the use case can be reused, the capture requests can only be cancelled after the
        // onStateAttached() callback has been received. In the normal code flow, the
        // onStateDetached() should also come after onStateAttached(). There is no API to
        // directly know  onStateAttached() callback has been received. Therefore, taking a
        // picture and waiting for the capture success callback to know the use case's
        // onStateAttached() callback has been received.
        val callback = FakeOnImageCapturedCallback(captureCount = 1)
        imageCapture.takePicture(mainExecutor, callback)

        // Wait for the signal that the image has been captured.
        callback.awaitCapturesAndAssert(capturedImagesCount = 1)

        val callback2 = FakeOnImageCapturedCallback(captureCount = 3)
        imageCapture.takePicture(mainExecutor, callback2)
        imageCapture.takePicture(mainExecutor, callback2)
        imageCapture.takePicture(mainExecutor, callback2)

        withContext(Dispatchers.Main) { imageCapture.onStateDetached() }

        callback2.awaitCaptures()
        assertThat(callback2.results.size + callback2.errors.size).isEqualTo(3)
        assertThat(callback2.errors.size).isAtLeast(1)

        for (error in callback2.errors) {
            assertThat(error.imageCaptureError).isEqualTo(ImageCapture.ERROR_CAMERA_CLOSED)
        }
    }

    @Test
    fun unbind_abortAllCaptureRequests() = runBlocking {
        val imageCapture = ImageCapture.Builder().build()
        withContext(Dispatchers.Main) {
            cameraProvider.bindToLifecycle(fakeLifecycleOwner, BACK_SELECTOR, imageCapture)
        }

        val callback = FakeOnImageCapturedCallback(captureCount = 3)
        imageCapture.takePicture(mainExecutor, callback)
        imageCapture.takePicture(mainExecutor, callback)
        imageCapture.takePicture(mainExecutor, callback)

        // Needs to run on main thread because takePicture gets posted on main thread if it isn't
        // running on the main thread. Which means the internal ImageRequests likely get issued
        // after ImageCapture is removed so errors out with a different error from
        // ERROR_CAMERA_CLOSED
        withContext(Dispatchers.Main) { cameraProvider.unbind(imageCapture) }

        // Wait for the signal that the image capture has failed.
        callback.awaitCapturesAndAssert(errorsCount = 3)

        assertThat(callback.results.size + callback.errors.size).isEqualTo(3)
        for (error in callback.errors) {
            assertThat(error.imageCaptureError)
                .isAnyOf(
                    ImageCapture.ERROR_CAMERA_CLOSED,
                    // If unbind() happens earlier than takePicture(), it gets ERROR_INVALID_CAMERA.
                    ImageCapture.ERROR_INVALID_CAMERA
                )
        }
    }

    @Test
    fun takePictureReturnsErrorNO_CAMERA_whenNotBound() = runBlocking {
        val imageCapture = ImageCapture.Builder().build()
        val callback = FakeOnImageCapturedCallback(captureCount = 1)

        imageCapture.takePicture(mainExecutor, callback)

        // Wait for the signal that the image capture has failed.
        callback.awaitCapturesAndAssert(errorsCount = 1)

        val error = callback.errors.first()
        assertThat(error.imageCaptureError).isEqualTo(ImageCapture.ERROR_INVALID_CAMERA)
    }

    @Test
    fun defaultAspectRatioWillBeSet_whenTargetResolutionIsNotSet() = runBlocking {
        val useCase = ImageCapture.Builder().build()
        withContext(Dispatchers.Main) {
            cameraProvider.bindToLifecycle(fakeLifecycleOwner, BACK_SELECTOR, useCase)
        }

        val config = useCase.currentConfig as ImageOutputConfig
        assertThat(config.targetAspectRatio).isEqualTo(AspectRatio.RATIO_4_3)
    }

    @Suppress("DEPRECATION") // test for legacy resolution API
    @Test
    fun defaultAspectRatioWillBeSet_whenRatioDefaultIsSet() = runBlocking {
        val useCase = ImageCapture.Builder().setTargetAspectRatio(AspectRatio.RATIO_DEFAULT).build()
        withContext(Dispatchers.Main) {
            cameraProvider.bindToLifecycle(fakeLifecycleOwner, BACK_SELECTOR, useCase)
        }

        val config = useCase.currentConfig as ImageOutputConfig
        assertThat(config.targetAspectRatio).isEqualTo(AspectRatio.RATIO_4_3)
    }

    @Suppress("DEPRECATION") // legacy resolution API
    @Test
    fun defaultAspectRatioWontBeSet_whenTargetResolutionIsSet() = runBlocking {
        val useCase = ImageCapture.Builder().setTargetResolution(DEFAULT_RESOLUTION).build()

        assertThat(
                useCase.currentConfig.containsOption(ImageOutputConfig.OPTION_TARGET_ASPECT_RATIO)
            )
            .isFalse()

        withContext(Dispatchers.Main) {
            cameraProvider.bindToLifecycle(fakeLifecycleOwner, BACK_SELECTOR, useCase)
        }

        assertThat(
                useCase.currentConfig.containsOption(ImageOutputConfig.OPTION_TARGET_ASPECT_RATIO)
            )
            .isFalse()
    }

    @Test
    fun targetRotationCanBeUpdatedAfterUseCaseIsCreated() {
        val imageCapture = ImageCapture.Builder().setTargetRotation(Surface.ROTATION_0).build()
        imageCapture.targetRotation = Surface.ROTATION_90
        assertThat(imageCapture.targetRotation).isEqualTo(Surface.ROTATION_90)
    }

    @Suppress("DEPRECATION") // test for legacy resolution API
    @Test
    fun targetResolutionIsUpdatedAfterTargetRotationIsUpdated() = runBlocking {
        val imageCapture =
            ImageCapture.Builder()
                .setTargetResolution(DEFAULT_RESOLUTION)
                .setTargetRotation(Surface.ROTATION_0)
                .build()
        withContext(Dispatchers.Main) {
            cameraProvider.bindToLifecycle(fakeLifecycleOwner, BACK_SELECTOR, imageCapture)
        }

        // Updates target rotation from ROTATION_0 to ROTATION_90.
        imageCapture.targetRotation = Surface.ROTATION_90

        val newConfig = imageCapture.currentConfig as ImageOutputConfig
        val expectedTargetResolution = Size(DEFAULT_RESOLUTION.height, DEFAULT_RESOLUTION.width)

        // Expected targetResolution will be reversed from original target resolution.
        assertThat(newConfig.targetResolution).isEqualTo(expectedTargetResolution)
    }

    @Suppress("DEPRECATION") // test for legacy resolution API
    @Test
    fun capturedImageHasCorrectCroppingSizeWithoutSettingRotation() {
        val useCase = ImageCapture.Builder().setTargetResolution(DEFAULT_RESOLUTION).build()

        capturedImageHasCorrectCroppingSize(
            useCase,
            rotateCropRect = { capturedImageRotationDegrees ->
                capturedImageRotationDegrees % 180 != 0
            }
        )
    }

    @Suppress("DEPRECATION") // test for legacy resolution API
    @Test
    fun capturedImageHasCorrectCroppingSizeSetRotationBuilder() {
        // Checks camera device sensor degrees to set correct target rotation value to make sure
        // that the initial set target cropping aspect ratio matches the sensor orientation.
        val sensorOrientation = CameraUtil.getSensorOrientation(BACK_LENS_FACING)
        val isRotateNeeded = sensorOrientation!! % 180 != 0
        val useCase =
            ImageCapture.Builder()
                .setTargetResolution(DEFAULT_RESOLUTION)
                .setTargetRotation(if (isRotateNeeded) Surface.ROTATION_90 else Surface.ROTATION_0)
                .build()

        capturedImageHasCorrectCroppingSize(
            useCase,
            rotateCropRect = { capturedImageRotationDegrees ->
                capturedImageRotationDegrees % 180 != 0
            }
        )
    }

    @Suppress("DEPRECATION") // test for legacy resolution API
    @Test
    fun capturedImageHasCorrectCroppingSize_setUseCaseRotation90FromRotationInBuilder() {
        // Checks camera device sensor degrees to set correct target rotation value to make sure
        // that the initial set target cropping aspect ratio matches the sensor orientation.
        val sensorOrientation = CameraUtil.getSensorOrientation(BACK_LENS_FACING)
        val isRotateNeeded = sensorOrientation!! % 180 != 0
        val useCase =
            ImageCapture.Builder()
                .setTargetResolution(DEFAULT_RESOLUTION)
                .setTargetRotation(if (isRotateNeeded) Surface.ROTATION_90 else Surface.ROTATION_0)
                .build()

        // Updates target rotation to opposite one.
        useCase.targetRotation = if (isRotateNeeded) Surface.ROTATION_0 else Surface.ROTATION_90

        capturedImageHasCorrectCroppingSize(
            useCase,
            rotateCropRect = { capturedImageRotationDegrees ->
                capturedImageRotationDegrees % 180 == 0
            }
        )
    }

    private fun capturedImageHasCorrectCroppingSize(
        useCase: ImageCapture,
        rotateCropRect: (Int) -> Boolean
    ) = runBlocking {
        withContext(Dispatchers.Main) {
            cameraProvider.bindToLifecycle(fakeLifecycleOwner, BACK_SELECTOR, useCase)
        }

        val callback = FakeOnImageCapturedCallback(captureCount = 1)
        useCase.takePicture(mainExecutor, callback)

        // Wait for the signal that the image has been captured.
        callback.awaitCapturesAndAssert(capturedImagesCount = 1)

        // After target rotation is updated, the result cropping aspect ratio should still the
        // same as original one.
        val expectedCroppingRatio = Rational(DEFAULT_RESOLUTION.width, DEFAULT_RESOLUTION.height)

        val imageProperties = callback.results.first().properties
        val cropRect = imageProperties.cropRect

        // Rotate the captured ImageProxy's crop rect into the coordinate space of the final
        // displayed image
        val resultCroppingRatio: Rational =
            if (rotateCropRect(imageProperties.rotationDegrees)) {
                Rational(cropRect!!.height(), cropRect.width())
            } else {
                Rational(cropRect!!.width(), cropRect.height())
            }

        assertThat(resultCroppingRatio.toFloat())
            .isWithin(TOLERANCE)
            .of(expectedCroppingRatio.toFloat())
        if (imageProperties.format == ImageFormat.JPEG && isRotationOptionSupportedDevice()) {
            assertThat(imageProperties.rotationDegrees).isEqualTo(imageProperties.exif!!.rotation)
        }
    }

    @Test
    fun capturedImageHasCorrectCroppingSize_setCropAspectRatioAfterBindToLifecycle() = runBlocking {
        val useCase = ImageCapture.Builder().build()
        withContext(Dispatchers.Main) {
            cameraProvider.bindToLifecycle(fakeLifecycleOwner, BACK_SELECTOR, useCase)
        }

        val callback = FakeOnImageCapturedCallback(captureCount = 1)

        // Checks camera device sensor degrees to set target cropping aspect ratio match the
        // sensor orientation.
        val sensorOrientation = CameraUtil.getSensorOrientation(BACK_LENS_FACING)
        val isRotateNeeded = sensorOrientation!! % 180 != 0

        // Set the default aspect ratio of ImageCapture to the target cropping aspect ratio.
        val targetCroppingAspectRatio = if (isRotateNeeded) Rational(3, 4) else Rational(4, 3)

        useCase.setCropAspectRatio(targetCroppingAspectRatio)
        useCase.takePicture(mainExecutor, callback)

        // Wait for the signal that the image has been captured.
        callback.awaitCapturesAndAssert(capturedImagesCount = 1)

        // After target rotation is updated, the result cropping aspect ratio should still the
        // same as original one.
        val imageProperties = callback.results.first().properties
        val cropRect = imageProperties.cropRect

        // Rotate the captured ImageProxy's crop rect into the coordinate space of the final
        // displayed image
        val resultCroppingRatio: Rational =
            if (imageProperties.rotationDegrees % 180 != 0) {
                Rational(cropRect!!.height(), cropRect.width())
            } else {
                Rational(cropRect!!.width(), cropRect.height())
            }

        if (imageProperties.format == ImageFormat.JPEG && isRotationOptionSupportedDevice()) {
            assertThat(imageProperties.rotationDegrees).isEqualTo(imageProperties.exif!!.rotation)
        }

        // Compare aspect ratio with a threshold due to floating point rounding. Can't do direct
        // comparison of height and width, because the target aspect ratio of ImageCapture will
        // be corrected in API 21 Legacy devices and the captured image will be scaled to fit
        // within the cropping aspect ratio.
        val aspectRatioThreshold = 0.01
        assertThat(abs(resultCroppingRatio.toDouble() - targetCroppingAspectRatio.toDouble()))
            .isLessThan(aspectRatioThreshold)
    }

    @Test
    fun capturedImageHasCorrectCroppingSize_viewPortOverwriteCropAspectRatio() = runBlocking {
        val sensorOrientation = CameraUtil.getSensorOrientation(BACK_LENS_FACING)
        val isRotateNeeded = sensorOrientation!! % 180 != 0

        val useCase =
            ImageCapture.Builder()
                .setTargetRotation(if (isRotateNeeded) Surface.ROTATION_90 else Surface.ROTATION_0)
                .build()

        // Sets a crop aspect ratio to the use case. This will be overwritten by the view port
        // setting.
        val useCaseCroppingAspectRatio = Rational(4, 3)
        useCase.setCropAspectRatio(useCaseCroppingAspectRatio)

        // Sets view port with different aspect ratio and then attach the use case
        val viewPortAspectRatio = Rational(2, 1)
        val viewPort =
            ViewPort.Builder(
                    viewPortAspectRatio,
                    if (isRotateNeeded) Surface.ROTATION_90 else Surface.ROTATION_0
                )
                .build()

        val useCaseGroup = UseCaseGroup.Builder().setViewPort(viewPort).addUseCase(useCase).build()

        withContext(Dispatchers.Main) {
            cameraProvider.bindToLifecycle(fakeLifecycleOwner, BACK_SELECTOR, useCaseGroup)
        }

        val callback = FakeOnImageCapturedCallback(captureCount = 1)

        useCase.takePicture(mainExecutor, callback)

        // Wait for the signal that the image has been captured.
        callback.awaitCapturesAndAssert(capturedImagesCount = 1)

        // After target rotation is updated, the result cropping aspect ratio should still the
        // same as original one.
        val imageProperties = callback.results.first().properties
        val cropRect = imageProperties.cropRect

        // Rotate the captured ImageProxy's crop rect into the coordinate space of the final
        // displayed image
        val resultCroppingRatio: Rational =
            if (imageProperties.rotationDegrees % 180 != 0) {
                Rational(cropRect!!.height(), cropRect.width())
            } else {
                Rational(cropRect!!.width(), cropRect.height())
            }

        if (imageProperties.format == ImageFormat.JPEG && isRotationOptionSupportedDevice()) {
            assertThat(imageProperties.rotationDegrees).isEqualTo(imageProperties.exif!!.rotation)
        }

        // Compare aspect ratio with a threshold due to floating point rounding. Can't do direct
        // comparison of height and width, because the target aspect ratio of ImageCapture will
        // be corrected in API 21 Legacy devices and the captured image will be scaled to fit
        // within the cropping aspect ratio.
        val aspectRatioThreshold = 0.01
        assertThat(abs(resultCroppingRatio.toDouble() - viewPortAspectRatio.toDouble()))
            .isLessThan(aspectRatioThreshold)
    }

    @Test
    fun useCaseConfigCanBeReset_afterUnbind() = runBlocking {
        val useCase = defaultBuilder.build()
        val initialConfig = useCase.currentConfig
        withContext(Dispatchers.Main) {
            cameraProvider.bindToLifecycle(fakeLifecycleOwner, BACK_SELECTOR, useCase)
        }

        withContext(Dispatchers.Main) { cameraProvider.unbind(useCase) }

        val configAfterUnbinding = useCase.currentConfig
        assertThat(initialConfig == configAfterUnbinding).isTrue()
    }

    @Test
    fun targetRotationIsRetained_whenUseCaseIsReused() = runBlocking {
        val useCase = defaultBuilder.build()
        withContext(Dispatchers.Main) {
            cameraProvider.bindToLifecycle(fakeLifecycleOwner, BACK_SELECTOR, useCase)
        }

        // Generally, the device can't be rotated to Surface.ROTATION_180. Therefore,
        // use it to do the test.
        useCase.targetRotation = Surface.ROTATION_180
        withContext(Dispatchers.Main) {
            // Unbind the use case.
            cameraProvider.unbind(useCase)
        }

        // Check the target rotation is kept when the use case is unbound.
        assertThat(useCase.targetRotation).isEqualTo(Surface.ROTATION_180)

        // Check the target rotation is kept when the use case is rebound to the
        // lifecycle.
        withContext(Dispatchers.Main) {
            cameraProvider.bindToLifecycle(fakeLifecycleOwner, BACK_SELECTOR, useCase)
        }
        assertThat(useCase.targetRotation).isEqualTo(Surface.ROTATION_180)
    }

    @Test
    fun cropAspectRatioIsRetained_whenUseCaseIsReused() = runBlocking {
        val useCase = defaultBuilder.build()
        val cropAspectRatio = Rational(1, 1)
        withContext(Dispatchers.Main) {
            cameraProvider.bindToLifecycle(fakeLifecycleOwner, BACK_SELECTOR, useCase)
        }

        useCase.setCropAspectRatio(cropAspectRatio)

        withContext(Dispatchers.Main) {
            // Unbind the use case.
            cameraProvider.unbind(useCase)
        }

        // Rebind the use case.
        withContext(Dispatchers.Main) {
            cameraProvider.bindToLifecycle(fakeLifecycleOwner, BACK_SELECTOR, useCase)
        }

        val callback = FakeOnImageCapturedCallback(captureCount = 1)
        useCase.takePicture(mainExecutor, callback)

        // Wait for the signal that the image has been captured.
        callback.awaitCapturesAndAssert(capturedImagesCount = 1)

        val imageProperties = callback.results.first().properties
        val cropRect = imageProperties.cropRect
        val cropRectAspectRatio = Rational(cropRect!!.height(), cropRect.width())

        // The crop aspect ratio could be kept after the use case is reused. So that the aspect
        // of the result cropRect is 1:1.
        assertThat(cropRectAspectRatio).isEqualTo(cropAspectRatio)
    }

    @Test
    fun useCaseCanBeReusedInSameCamera() = runBlocking {
        val useCase = defaultBuilder.build()
        withContext(Dispatchers.Main) {
            cameraProvider.bindToLifecycle(fakeLifecycleOwner, BACK_SELECTOR, useCase)
        }

        val saveLocation1 = temporaryFolder.newFile("test1.jpg")

        val callback = FakeImageSavedCallback(capturesCount = 1)

        useCase.takePicture(
            ImageCapture.OutputFileOptions.Builder(saveLocation1).build(),
            mainExecutor,
            callback
        )

        // Wait for the signal that the image has been saved.
        callback.awaitCapturesAndAssert(savedImagesCount = 1)

        withContext(Dispatchers.Main) {
            // Unbind the use case.
            cameraProvider.unbind(useCase)
        }

        // Rebind the use case to the same camera.
        withContext(Dispatchers.Main) {
            cameraProvider.bindToLifecycle(fakeLifecycleOwner, BACK_SELECTOR, useCase)
        }

        val saveLocation2 = temporaryFolder.newFile("test2.jpg")

        val callback2 = FakeImageSavedCallback(capturesCount = 1)

        useCase.takePicture(
            ImageCapture.OutputFileOptions.Builder(saveLocation2).build(),
            mainExecutor,
            callback2
        )

        // Wait for the signal that the image has been saved.
        callback2.awaitCapturesAndAssert(savedImagesCount = 1)
    }

    @Test
    fun useCaseCanBeReusedInDifferentCamera() = runBlocking {
        assumeTrue(CameraUtil.hasCameraWithLensFacing(CameraSelector.LENS_FACING_FRONT))

        val useCase = defaultBuilder.build()
        withContext(Dispatchers.Main) {
            cameraProvider.bindToLifecycle(fakeLifecycleOwner, BACK_SELECTOR, useCase)
        }

        val saveLocation1 = temporaryFolder.newFile("test1.jpg")

        val callback = FakeImageSavedCallback(capturesCount = 1)

        useCase.takePicture(
            ImageCapture.OutputFileOptions.Builder(saveLocation1).build(),
            mainExecutor,
            callback
        )

        // Wait for the signal that the image has been saved.
        callback.awaitCapturesAndAssert(savedImagesCount = 1)

        withContext(Dispatchers.Main) {
            // Unbind the use case.
            cameraProvider.unbind(useCase)
        }

        // Rebind the use case to different camera.
        withContext(Dispatchers.Main) {
            cameraProvider.bindToLifecycle(
                fakeLifecycleOwner,
                CameraSelector.DEFAULT_FRONT_CAMERA,
                useCase
            )
        }

        val saveLocation2 = temporaryFolder.newFile("test2.jpg")

        val callback2 = FakeImageSavedCallback(capturesCount = 1)

        useCase.takePicture(
            ImageCapture.OutputFileOptions.Builder(saveLocation2).build(),
            mainExecutor,
            callback2
        )

        // Wait for the signal that the image has been saved.
        callback2.awaitCapturesAndAssert(savedImagesCount = 1)
    }

    @Test
    fun returnValidTargetRotation_afterUseCaseIsCreated() {
        val imageCapture = ImageCapture.Builder().build()
        assertThat(imageCapture.targetRotation).isNotEqualTo(ImageOutputConfig.INVALID_ROTATION)
    }

    @Test
    fun returnCorrectTargetRotation_afterUseCaseIsAttached() = runBlocking {
        val imageCapture = ImageCapture.Builder().setTargetRotation(Surface.ROTATION_180).build()
        withContext(Dispatchers.Main) {
            cameraProvider.bindToLifecycle(fakeLifecycleOwner, BACK_SELECTOR, imageCapture)
        }
        assertThat(imageCapture.targetRotation).isEqualTo(Surface.ROTATION_180)
    }

    @Test
    fun returnDefaultFlashMode_beforeUseCaseIsAttached() {
        val imageCapture = ImageCapture.Builder().build()
        assertThat(imageCapture.flashMode).isEqualTo(ImageCapture.FLASH_MODE_OFF)
    }

    @Test
    fun returnCorrectFlashMode_afterUseCaseIsAttached() = runBlocking {
        val imageCapture = ImageCapture.Builder().setFlashMode(ImageCapture.FLASH_MODE_ON).build()
        withContext(Dispatchers.Main) {
            cameraProvider.bindToLifecycle(fakeLifecycleOwner, BACK_SELECTOR, imageCapture)
        }
        assertThat(imageCapture.flashMode).isEqualTo(ImageCapture.FLASH_MODE_ON)
    }

    @Test
    fun returnJpegImage_whenSoftwareJpegIsEnabled() = runBlocking {
        val builder = ImageCapture.Builder()

        val useCase = builder.build()
        var camera: Camera
        withContext(Dispatchers.Main) {
            camera =
                cameraProvider.bindToLifecycle(
                    fakeLifecycleOwner,
                    BACK_SELECTOR,
                    useCase,
                    Preview.Builder().build().apply {
                        setSurfaceProvider(SurfaceTextureProvider.createSurfaceTextureProvider())
                    }
                )
        }

        // TODO - Check before binding for further optimization
        camera.assumeSoftwareJpegEnabled()

        val callback = FakeOnImageCapturedCallback(captureCount = 1)
        useCase.takePicture(mainExecutor, callback)

        // Wait for the signal that the image has been captured.
        callback.awaitCapturesAndAssert(capturedImagesCount = 1)

        val imageProperties = callback.results.first().properties

        // Check the output image rotation degrees value is correct.
        assertThat(imageProperties.rotationDegrees)
            .isEqualTo(camera.cameraInfo.getSensorRotationDegrees(useCase.targetRotation))

        // Check the output format is correct.
        assertThat(imageProperties.format).isEqualTo(ImageFormat.JPEG)
    }

    @Test
    fun canSaveJpegFileWithRotation_whenSoftwareJpegIsEnabled() = runBlocking {
        val builder = ImageCapture.Builder()

        val useCase = builder.build()
        var camera: Camera
        withContext(Dispatchers.Main) {
            camera =
                cameraProvider.bindToLifecycle(
                    fakeLifecycleOwner,
                    BACK_SELECTOR,
                    useCase,
                    Preview.Builder().build().apply {
                        setSurfaceProvider(SurfaceTextureProvider.createSurfaceTextureProvider())
                    }
                )
        }

        // TODO - Check before binding for further optimization
        camera.assumeSoftwareJpegEnabled()

        val saveLocation = temporaryFolder.newFile("test.jpg")
        val callback = FakeImageSavedCallback(capturesCount = 1)
        useCase.takePicture(
            ImageCapture.OutputFileOptions.Builder(saveLocation).build(),
            mainExecutor,
            callback
        )

        // Wait for the signal that the image has been captured and saved.
        callback.awaitCapturesAndAssert(savedImagesCount = 1)

        // For YUV to JPEG case, the rotation will only be in Exif.
        val exif = Exif.createFromFile(saveLocation)
        assertThat(exif.rotation)
            .isEqualTo(camera.cameraInfo.getSensorRotationDegrees(useCase.targetRotation))
    }

    @Test
    fun returnYuvImage_withYuvBufferFormat() = runBlocking {
        val builder = ImageCapture.Builder().setBufferFormat(ImageFormat.YUV_420_888)
        val useCase = builder.build()
        var camera: Camera
        withContext(Dispatchers.Main) {
            camera =
                cameraProvider.bindToLifecycle(
                    fakeLifecycleOwner,
                    BACK_SELECTOR,
                    useCase,
                    Preview.Builder().build().apply {
                        setSurfaceProvider(SurfaceTextureProvider.createSurfaceTextureProvider())
                    }
                )
        }

        val callback = FakeOnImageCapturedCallback(captureCount = 1)
        useCase.takePicture(mainExecutor, callback)

        // Wait for the signal that the image has been captured.
        callback.awaitCapturesAndAssert(capturedImagesCount = 1)

        val imageProperties = callback.results.first().properties
        // Check the output image rotation degrees value is correct.
        assertThat(imageProperties.rotationDegrees)
            .isEqualTo(camera.cameraInfo.getSensorRotationDegrees(useCase.targetRotation))
        // Check the output format is correct.
        assertThat(imageProperties.format).isEqualTo(ImageFormat.YUV_420_888)
    }

    @Test
    fun returnYuvImage_whenSoftwareJpegIsEnabledWithYuvBufferFormat() = runBlocking {
        val builder = ImageCapture.Builder().setBufferFormat(ImageFormat.YUV_420_888)

        val useCase = builder.build()
        var camera: Camera
        withContext(Dispatchers.Main) {
            camera =
                cameraProvider.bindToLifecycle(
                    fakeLifecycleOwner,
                    BACK_SELECTOR,
                    useCase,
                    Preview.Builder().build().apply {
                        setSurfaceProvider(SurfaceTextureProvider.createSurfaceTextureProvider())
                    }
                )
        }

        // TODO - Check before binding for further optimization
        camera.assumeSoftwareJpegEnabled()

        val callback = FakeOnImageCapturedCallback(captureCount = 1)
        useCase.takePicture(mainExecutor, callback)

        // Wait for the signal that the image has been captured.
        callback.awaitCapturesAndAssert(capturedImagesCount = 1)

        val imageProperties = callback.results.first().properties

        // Check the output image rotation degrees value is correct.
        assertThat(imageProperties.rotationDegrees)
            .isEqualTo(camera.cameraInfo.getSensorRotationDegrees(useCase.targetRotation))
        // Check the output format is correct.
        assertThat(imageProperties.format).isEqualTo(ImageFormat.YUV_420_888)
    }

    @Test
    @SdkSuppress(minSdkVersion = 28)
    fun returnJpegImage_whenSessionProcessorIsSet() = runBlocking {
        implName.ignoreTestForCameraPipe(
            "TODO(b/275493663): Enable when camera-pipe has extensions support"
        )

        val builder = ImageCapture.Builder()
        val sessionProcessor =
            FakeSessionProcessor(
                inputFormatPreview = null, // null means using the same output surface
                inputFormatCapture = ImageFormat.YUV_420_888
            )

        val imageCapture = builder.build()
        val preview = Preview.Builder().build()

        var camera: Camera
        withContext(Dispatchers.Main) {
            preview.setSurfaceProvider(SurfaceTextureProvider.createSurfaceTextureProvider())
            val cameraSelector =
                ExtensionsUtil.getCameraSelectorWithSessionProcessor(
                    cameraProvider,
                    BACK_SELECTOR,
                    sessionProcessor,
                    outputYuvformatInCapture = true
                )
            camera =
                cameraProvider.bindToLifecycle(
                    fakeLifecycleOwner,
                    cameraSelector,
                    imageCapture,
                    preview
                )
        }

        val callback = FakeOnImageCapturedCallback(captureCount = 1)
        imageCapture.takePicture(mainExecutor, callback)

        // Wait for the signal that the image has been captured.
        callback.awaitCapturesAndAssert(capturedImagesCount = 1)

        val imageProperties = callback.results.first().properties

        // Check the output image rotation degrees value is correct.
        assertThat(imageProperties.rotationDegrees)
            .isEqualTo(camera.cameraInfo.getSensorRotationDegrees(imageCapture.targetRotation))
        // Check the output format is correct.
        assertThat(imageProperties.format).isEqualTo(ImageFormat.JPEG)
    }

    @Test
    @SdkSuppress(minSdkVersion = 28)
    fun returnJpegImage_whenSessionProcessorIsSet_outputFormatJpeg() = runBlocking {
        implName.ignoreTestForCameraPipe(
            "TODO(b/275493663): Enable when camera-pipe has extensions support"
        )

        assumeFalse(
            "Cuttlefish does not correctly handle Jpeg exif. Unable to test.",
            Build.MODEL.contains("Cuttlefish")
        )

        val sessionProcessor =
            FakeSessionProcessor(
                inputFormatPreview = null, // null means using the same output surface
                inputFormatCapture = null
            )

        val imageCapture = ImageCapture.Builder().build()
        val preview = Preview.Builder().build()

        withContext(Dispatchers.Main) {
            preview.setSurfaceProvider(SurfaceTextureProvider.createSurfaceTextureProvider())
            val cameraSelector =
                ExtensionsUtil.getCameraSelectorWithSessionProcessor(
                    cameraProvider,
                    BACK_SELECTOR,
                    sessionProcessor
                )
            cameraProvider.bindToLifecycle(
                fakeLifecycleOwner,
                cameraSelector,
                imageCapture,
                preview
            )
        }

        val callback = FakeOnImageCapturedCallback(captureCount = 1)
        imageCapture.takePicture(mainExecutor, callback)

        // Wait for the signal that the image has been captured.
        callback.awaitCapturesAndAssert(capturedImagesCount = 1)

        val imageProperties = callback.results.first().properties

        // Check the output image rotation degrees value is correct.
        if (isRotationOptionSupportedDevice()) {
            assertThat(imageProperties.rotationDegrees).isEqualTo(imageProperties.exif!!.rotation)
        }

        // Check the output format is correct.
        assertThat(imageProperties.format).isEqualTo(ImageFormat.JPEG)
    }

    @SdkSuppress(minSdkVersion = 34)
    @Test
    fun returnJpegrImageWithGainmap_whenOutputFormatIsUltraHdr() = runBlocking {
        val cameraSelector = BACK_SELECTOR
        assumeUltraHdrSupported(cameraSelector)

        // Arrange.
        val useCase = ImageCapture.Builder().setOutputFormat(OUTPUT_FORMAT_JPEG_ULTRA_HDR).build()
        withContext(Dispatchers.Main) {
            cameraProvider.bindToLifecycle(fakeLifecycleOwner, cameraSelector, useCase)
        }

        // Act.
        val latch = CountdownDeferred(1)
        var error: Exception? = null
        val callback =
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    val imageFormat = image.format
                    val bitmap = image.toBitmap()
                    image.close()

                    // Check the image format is correct and the gainmap exists.
                    if (imageFormat != ImageFormat.JPEG_R) {
                        error = Exception("INCORRECT_IMAGE_FORMAT")
                    } else if (!bitmap.hasGainmap()) {
                        error = Exception("NO_GAINMAP")
                    }

                    latch.countDown()
                }

                override fun onError(exception: ImageCaptureException) {
                    error = exception
                    latch.countDown()
                }
            }
        useCase.takePicture(mainExecutor, callback)

        // Assert.
        // Wait for the signal that the image has been captured.
        assertThat(withTimeoutOrNull(CAPTURE_TIMEOUT) { latch.await() }).isNotNull()
        assertThat(error).isNull()
    }

    @Test
    fun canCaptureImage_whenOnlyImageCaptureBound_withYuvBufferFormat() {
        val cameraHwLevel =
            CameraUtil.getCameraCharacteristics(CameraSelector.LENS_FACING_BACK)
                ?.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL)
        assumeTrue(
            "TODO(b/298138582): Check if MeteringRepeating will need to be added while" +
                " choosing resolution for ImageCapture",
            cameraHwLevel != CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY &&
                cameraHwLevel != CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED
        )

        canTakeImages(ImageCapture.Builder().apply { setBufferFormat(ImageFormat.YUV_420_888) })
    }

    @Test
    fun unbindPreview_imageCapturingShouldSuccess() = runBlocking {
        // Arrange.
        val imageCapture = ImageCapture.Builder().build()
        val previewStreamReceived = CompletableDeferred<Boolean>()
        val preview =
            Preview.Builder()
                .also {
                    Camera2InteropUtil.setCameraCaptureSessionCallback(
                        implName,
                        it,
                        object : CaptureCallback() {
                            override fun onCaptureCompleted(
                                session: CameraCaptureSession,
                                request: CaptureRequest,
                                result: TotalCaptureResult
                            ) {
                                previewStreamReceived.complete(true)
                            }
                        }
                    )
                }
                .build()
        withContext(Dispatchers.Main) {
            preview.setSurfaceProvider(SurfaceTextureProvider.createSurfaceTextureProvider())
            cameraProvider.bindToLifecycle(fakeLifecycleOwner, BACK_SELECTOR, imageCapture, preview)
        }
        assertWithMessage("Preview doesn't start")
            .that(previewStreamReceived.awaitWithTimeoutOrNull())
            .isTrue()

        // Act.
        val callback = FakeOnImageCapturedCallback(captureCount = 1)
        withContext(Dispatchers.Main) {
            // Test the reproduce step in b/235119898
            cameraProvider.unbind(preview)
            imageCapture.takePicture(mainExecutor, callback)
        }

        // Assert.
        callback.awaitCapturesAndAssert(capturedImagesCount = 1)
    }

    @Test
    fun unbindVideoCaptureWithoutStartingRecorder_imageCapturingShouldSuccess() = runBlocking {
        // Arrange.
        val imageCapture = ImageCapture.Builder().build()
        val videoCapture = VideoCapture.Builder<Recorder>(Recorder.Builder().build()).build()

        withContext(Dispatchers.Main) {
            cameraProvider.bindToLifecycle(
                fakeLifecycleOwner,
                BACK_SELECTOR,
                imageCapture,
                videoCapture
            )
        }

        // wait for camera to start by taking a picture
        val callback1 = FakeOnImageCapturedCallback(captureCount = 1)
        imageCapture.takePicture(mainExecutor, callback1)
        try {
            callback1.awaitCapturesAndAssert(capturedImagesCount = 1)
        } catch (e: AssertionError) {
            assumeNoException("image capture failed, camera might not have started yet", e)
        }

        // Act.
        val callback2 = FakeOnImageCapturedCallback(captureCount = 1)
        withContext(Dispatchers.Main) {
            cameraProvider.unbind(videoCapture)
            imageCapture.takePicture(mainExecutor, callback2)
        }

        // Assert.
        callback2.awaitCapturesAndAssert(capturedImagesCount = 1)
    }

    @Test
    fun capturedImage_withHighResolutionEnabled_imageCaptureOnly() = runBlocking {
        capturedImage_withHighResolutionEnabled()
    }

    @Test
    fun capturedImage_withHighResolutionEnabled_imageCapturePreviewImageAnalysis() = runBlocking {
        val preview =
            Preview.Builder().build().also {
                withContext(Dispatchers.Main) {
                    it.setSurfaceProvider(SurfaceTextureProvider.createSurfaceTextureProvider())
                }
            }
        val imageAnalysis =
            ImageAnalysis.Builder().build().also { imageAnalysis ->
                imageAnalysis.setAnalyzer(Dispatchers.Default.asExecutor()) { imageProxy ->
                    imageProxy.close()
                }
            }
        capturedImage_withHighResolutionEnabled(preview, imageAnalysis)
    }

    @Test
    @SdkSuppress(minSdkVersion = 28)
    fun getRealtimeCaptureLatencyEstimate_whenSessionProcessorSupportsRealtimeLatencyEstimate() =
        runBlocking {
            val expectedCaptureLatencyMillis = 1000L
            val expectedProcessingLatencyMillis = 100L
            val sessionProcessor =
                object :
                    SessionProcessor by FakeSessionProcessor(
                        inputFormatPreview = null, // null means using the same output surface
                        inputFormatCapture = null
                    ) {
                    override fun getRealtimeCaptureLatency(): Pair<Long, Long> =
                        Pair(expectedCaptureLatencyMillis, expectedProcessingLatencyMillis)
                }

            val imageCapture = ImageCapture.Builder().build()
            val preview = Preview.Builder().build()

            withContext(Dispatchers.Main) {
                preview.setSurfaceProvider(SurfaceTextureProvider.createSurfaceTextureProvider())
                val cameraSelector =
                    ExtensionsUtil.getCameraSelectorWithSessionProcessor(
                        cameraProvider,
                        BACK_SELECTOR,
                        sessionProcessor
                    )
                cameraProvider.bindToLifecycle(
                    fakeLifecycleOwner,
                    cameraSelector,
                    imageCapture,
                    preview
                )
            }

            val latencyEstimate = imageCapture.realtimeCaptureLatencyEstimate
            // Check the realtime latency estimate is correct.
            assertThat(latencyEstimate.captureLatencyMillis).isEqualTo(expectedCaptureLatencyMillis)
            assertThat(latencyEstimate.processingLatencyMillis)
                .isEqualTo(expectedProcessingLatencyMillis)
        }

    @Test
    @SdkSuppress(minSdkVersion = 28)
    fun getRealtimeCaptureLatencyEstimate_whenSessionProcessorNotSupportsRealtimeLatencyEstimate() =
        runBlocking {
            val sessionProcessor =
                object :
                    SessionProcessor by FakeSessionProcessor(
                        inputFormatPreview = null, // null means using the same output surface
                        inputFormatCapture = null
                    ) {
                    override fun getRealtimeCaptureLatency(): Pair<Long, Long>? = null
                }

            val imageCapture = ImageCapture.Builder().build()
            val preview = Preview.Builder().build()

            withContext(Dispatchers.Main) {
                preview.setSurfaceProvider(SurfaceTextureProvider.createSurfaceTextureProvider())
                val cameraSelector =
                    ExtensionsUtil.getCameraSelectorWithSessionProcessor(
                        cameraProvider,
                        BACK_SELECTOR,
                        sessionProcessor
                    )
                cameraProvider.bindToLifecycle(
                    fakeLifecycleOwner,
                    cameraSelector,
                    imageCapture,
                    preview
                )
            }

            assertThat(imageCapture.realtimeCaptureLatencyEstimate)
                .isEqualTo(ImageCaptureLatencyEstimate.UNDEFINED_IMAGE_CAPTURE_LATENCY)
        }

    @Test
    fun getRealtimeCaptureLatencyEstimate_whenNoSessionProcessor() = runBlocking {
        val imageCapture = ImageCapture.Builder().build()
        val preview = Preview.Builder().build()

        withContext(Dispatchers.Main) {
            preview.setSurfaceProvider(SurfaceTextureProvider.createSurfaceTextureProvider())
            cameraProvider.bindToLifecycle(fakeLifecycleOwner, BACK_SELECTOR, imageCapture, preview)
        }

        assertThat(imageCapture.realtimeCaptureLatencyEstimate)
            .isEqualTo(ImageCaptureLatencyEstimate.UNDEFINED_IMAGE_CAPTURE_LATENCY)
    }

    @Test
    fun resolutionSelectorConfigCorrectlyMerged_afterBindToLifecycle() = runBlocking {
        val resolutionFilter = ResolutionFilter { supportedSizes, _ -> supportedSizes }
        val useCase =
            ImageCapture.Builder()
                .setResolutionSelector(
                    ResolutionSelector.Builder()
                        .setResolutionFilter(resolutionFilter)
                        .setAllowedResolutionMode(PREFER_HIGHER_RESOLUTION_OVER_CAPTURE_RATE)
                        .build()
                )
                .build()
        withContext(Dispatchers.Main) {
            cameraProvider.bindToLifecycle(fakeLifecycleOwner, BACK_SELECTOR, useCase)
        }
        val resolutionSelector = useCase.currentConfig.retrieveOption(OPTION_RESOLUTION_SELECTOR)
        // The default 4:3 AspectRatioStrategy is kept
        assertThat(resolutionSelector!!.aspectRatioStrategy)
            .isEqualTo(AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY)
        // The default highest available ResolutionStrategy is kept
        assertThat(resolutionSelector.resolutionStrategy)
            .isEqualTo(ResolutionStrategy.HIGHEST_AVAILABLE_STRATEGY)
        // The set resolutionFilter is kept
        assertThat(resolutionSelector.resolutionFilter).isEqualTo(resolutionFilter)
        // The set allowedResolutionMode is kept
        assertThat(resolutionSelector.allowedResolutionMode)
            .isEqualTo(PREFER_HIGHER_RESOLUTION_OVER_CAPTURE_RATE)
    }

    @Test
    fun canCaptureImageWithCameraEffect() {
        canTakeImages(defaultBuilder, addSharedEffect = true)
    }

    private fun capturedImage_withHighResolutionEnabled(
        preview: Preview? = null,
        imageAnalysis: ImageAnalysis? = null
    ) = runBlocking {
        val cameraInfo =
            withContext(Dispatchers.Main) {
                cameraProvider
                    .bindToLifecycle(fakeLifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA)
                    .cameraInfo
            }
        val maxHighResolutionOutputSize =
            CameraInfoUtil.getMaxHighResolutionOutputSize(cameraInfo, ImageFormat.JPEG)
        // Only runs the test when the device has high resolution output sizes
        assumeTrue(maxHighResolutionOutputSize != null)

        val resolutionSelector =
            ResolutionSelector.Builder()
                .setAllowedResolutionMode(PREFER_HIGHER_RESOLUTION_OVER_CAPTURE_RATE)
                .setResolutionFilter { _, _ -> listOf(maxHighResolutionOutputSize) }
                .build()
        val sensorOrientation = CameraUtil.getSensorOrientation(BACK_SELECTOR.lensFacing!!)
        // Sets the target rotation to the camera sensor orientation to avoid the captured image
        // buffer data rotated by the HAL and impact the final image resolution check
        val targetRotation =
            if (sensorOrientation!! % 180 == 0) {
                Surface.ROTATION_0
            } else {
                Surface.ROTATION_90
            }
        val imageCapture =
            ImageCapture.Builder()
                .setResolutionSelector(resolutionSelector)
                .setTargetRotation(targetRotation)
                .build()

        val useCases = arrayListOf<UseCase>(imageCapture)
        preview?.let { useCases.add(it) }
        imageAnalysis?.let { useCases.add(it) }

        withContext(Dispatchers.Main) {
            cameraProvider.bindToLifecycle(
                fakeLifecycleOwner,
                BACK_SELECTOR,
                *useCases.toTypedArray()
            )
        }

        assertThat(imageCapture.resolutionInfo!!.resolution).isEqualTo(maxHighResolutionOutputSize)

        val callback = FakeOnImageCapturedCallback(captureCount = 1)
        imageCapture.takePicture(mainExecutor, callback)

        // Wait for the signal that the image has been captured.
        callback.awaitCapturesAndAssert(capturedImagesCount = 1)

        val imageProperties = callback.results.first().properties
        assertThat(imageProperties.size).isEqualTo(maxHighResolutionOutputSize)
    }

    /** See b/288828159 for the detailed info of the issue */
    @Test
    fun jpegImageZeroPaddingDataDetectionTest(): Unit = runBlocking {
        val imageCapture = ImageCapture.Builder().build()

        withContext(Dispatchers.Main) {
            cameraProvider.bindToLifecycle(fakeLifecycleOwner, BACK_SELECTOR, imageCapture)
        }

        val latch = CountdownDeferred(1)
        var errors: Exception? = null

        val callback =
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    val planes = image.planes
                    val buffer = planes[0].buffer
                    val data = ByteArray(buffer.capacity())
                    buffer.rewind()
                    buffer[data]

                    image.close()

                    val invalidJpegDataParser = InvalidJpegDataParser()

                    // Only checks the unnecessary zero padding data when the device is not included
                    // in
                    // the LargeJpegImageQuirk device list.
                    // InvalidJpegDataParser#getValidDataLength()
                    // should have returned the valid data length to avoid the extremely large JPEG
                    // file issue.
                    if (
                        invalidJpegDataParser.getValidDataLength(data) == data.size &&
                            containsZeroPaddingDataAfterEoi(data)
                    ) {
                        errors = Exception("UNNECESSARY_JPEG_ZERO_PADDING_DATA_DETECTED!")
                    }

                    latch.countDown()
                }

                override fun onError(exception: ImageCaptureException) {
                    errors = exception
                    latch.countDown()
                }
            }

        imageCapture.takePicture(mainExecutor, callback)

        // Wait for the signal that the image has been captured.
        assertThat(withTimeoutOrNull(CAPTURE_TIMEOUT) { latch.await() }).isNotNull()
        assertThat(errors).isNull()
    }

    /**
     * This util function is only used to detect the unnecessary zero padding data after EOI. It
     * will directly return false when it fails to parse the JPEG byte array data.
     */
    private fun containsZeroPaddingDataAfterEoi(bytes: ByteArray): Boolean {
        val jfifEoiMarkEndPosition = InvalidJpegDataParser.getJfifEoiMarkEndPosition(bytes)

        // Directly returns false when EOI mark can't be found.
        if (jfifEoiMarkEndPosition == -1) {
            return false
        }

        // Will check 1mb data to know whether unnecessary zero padding data exists or not.
        // Directly returns false when the data length is long enough
        val dataLengthToDetect = 1_000_000
        if (jfifEoiMarkEndPosition + dataLengthToDetect > bytes.size) {
            return false
        }

        // Checks that there are at least continuous 1mb of unnecessary zero padding data after EOI
        for (position in jfifEoiMarkEndPosition..jfifEoiMarkEndPosition + dataLengthToDetect) {
            if (bytes[position] != 0x00.toByte()) {
                return false
            }
        }

        return true
    }

    private fun createNonRotatedConfiguration(): ImageCaptureConfig {
        // Create a configuration with target rotation that matches the sensor rotation.
        // This assumes a back-facing camera (facing away from screen)
        val sensorRotation = CameraUtil.getSensorOrientation(BACK_LENS_FACING)
        val surfaceRotation = CameraOrientationUtil.degreesToSurfaceRotation(sensorRotation!!)
        return ImageCapture.Builder().setTargetRotation(surfaceRotation).useCaseConfig
    }

    /**
     * See ImageCaptureRotationOptionQuirk. Some real devices or emulator do not support the capture
     * rotation option correctly. The capture rotation option setting can't be correctly applied to
     * the exif metadata of the captured images. Therefore, the exif rotation related verification
     * in the tests needs to be ignored on these devices or emulator.
     */
    private fun isRotationOptionSupportedDevice() =
        ExifRotationAvailability().isRotationOptionSupported

    private fun Camera.assumeSoftwareJpegEnabled() {
        assumeTrue(
            "Software JPEG is not enabled in this device",
            (cameraInfo as CameraInfoInternal)
                .cameraQuirks
                .contains(SoftwareJpegEncodingPreferredQuirk::class.java)
        )
    }

    private class ImageProperties(
        val size: Size? = null,
        val format: Int = -1,
        val rotationDegrees: Int = -1,
        val cropRect: Rect? = null,
        val exif: Exif? = null,
    )

    private class FakeImageSavedCallback(capturesCount: Int) : ImageCapture.OnImageSavedCallback {

        private val latch = CountdownDeferred(capturesCount)
        val results = mutableListOf<ImageCapture.OutputFileResults>()
        val errors = mutableListOf<ImageCaptureException>()

        override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
            results.add(outputFileResults)
            latch.countDown()
        }

        override fun onError(exception: ImageCaptureException) {
            errors.add(exception)
            latch.countDown()
        }

        suspend fun awaitCapturesAndAssert(
            timeout: Duration = CAPTURE_TIMEOUT,
            savedImagesCount: Int = 0,
            errorsCount: Int = 0
        ) {
            assertThat(withTimeoutOrNull(timeout) { latch.await() }).isNotNull()
            assertThat(results.size).isEqualTo(savedImagesCount)
            assertThat(errors.size).isEqualTo(errorsCount)
        }
    }

    private suspend fun <T> Deferred<T>.awaitWithTimeoutOrNull(
        timeMillis: Long = TimeUnit.SECONDS.toMillis(5)
    ): T? {
        return withTimeoutOrNull(timeMillis) { await() }
    }
}

@Suppress("DEPRECATION")
fun createDefaultPictureFolderIfNotExist() {
    val pictureFolder =
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
    if (!pictureFolder.exists()) {
        pictureFolder.mkdir()
    }
}
