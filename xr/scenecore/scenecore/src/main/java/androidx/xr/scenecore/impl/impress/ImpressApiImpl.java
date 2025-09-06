/*
 * Copyright 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.xr.scenecore.impl.impress;

import android.os.Handler;
import android.os.Looper;
import android.view.Surface;

import androidx.annotation.RestrictTo;
import androidx.concurrent.futures.CallbackToFutureAdapter;
import androidx.xr.scenecore.internal.KhronosPbrMaterialSpec;
import androidx.xr.scenecore.internal.TextureSampler;

import com.google.ar.imp.view.View;
import com.google.common.util.concurrent.ListenableFuture;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/** Implementation of the JNI API for communicating with the Impress Split Engine instance. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public final class ImpressApiImpl implements ImpressApi {
    private static final String TAG = ImpressApiImpl.class.getSimpleName();
    private View view;
    private BindingsResourceManager resourceManager;

    /*
     * This is mostly here to throw on unsupported values. The int cast works as long as
     * ImpressApi.StereoMode is in sync with imp::MediaStereoMode.
     */
    private int validateStereoMode(@StereoMode int stereoMode) {
        switch (stereoMode) {
            case StereoMode.MONO:
            case StereoMode.TOP_BOTTOM:
            case StereoMode.SIDE_BY_SIDE:
            case StereoMode.MULTIVIEW_LEFT_PRIMARY:
            case StereoMode.MULTIVIEW_RIGHT_PRIMARY:
                return stereoMode;
            default:
                throw new IllegalArgumentException(
                        "Unspported value for ImpressApi.StereoMode: " + stereoMode);
        }
    }

    /*
     * This is mostly here to throw on unsupported values. The int cast works as long as
     * ImpressApi.ContentSecurityLevel is in sync with imp::ContentSecurityLevel.
     */
    private int validateContentSecurityLevel(@ContentSecurityLevel int contentSecurityLevel) {
        switch (contentSecurityLevel) {
            case ContentSecurityLevel.NONE:
            case ContentSecurityLevel.PROTECTED:
                return contentSecurityLevel;
            default:
                throw new IllegalArgumentException(
                        "Unspported value for ImpressApi.ContentSecurityLevel: "
                                + contentSecurityLevel);
        }
    }

    /*
     * This is mostly here to throw on unsupported values. The int cast works as long as
     * ImpressApi.ColorSpace is in sync with the native counterpart.
     */
    private int validateColorSpace(@ColorSpace int colorSpace) {
        switch (colorSpace) {
            case ColorSpace.BT709:
            case ColorSpace.BT601_PAL:
            case ColorSpace.BT2020:
            case ColorSpace.BT601_525:
            case ColorSpace.DISPLAY_P3:
            case ColorSpace.DCI_P3:
            case ColorSpace.ADOBE_RGB:
                return colorSpace;
            default:
                throw new IllegalArgumentException(
                        "Unsupported value for ImpressApi.ColorSpace: " + colorSpace);
        }
    }

    /*
     * This is mostly here to throw on unsupported values. The int cast works as long as
     * ImpressApi.ColorTransfer is in sync with the native counterpart.
     */
    private int validateColorTransfer(@ColorTransfer int colorTransfer) {
        switch (colorTransfer) {
            case ColorTransfer.LINEAR:
            case ColorTransfer.SRGB:
            case ColorTransfer.SDR:
            case ColorTransfer.GAMMA_2_2:
            case ColorTransfer.ST2084:
            case ColorTransfer.HLG:
                return colorTransfer;
            default:
                throw new IllegalArgumentException(
                        "Unsupported value for ImpressApi.ColorTransfer: " + colorTransfer);
        }
    }

    /*
     * This is mostly here to throw on unsupported values. The int cast works as long as
     * ImpressApi.ColorRange is in sync with the native counterpart.
     */
    private int validateColorRange(@ColorRange int colorRange) {
        switch (colorRange) {
            case ColorRange.FULL:
            case ColorRange.LIMITED:
                return colorRange;
            default:
                throw new IllegalArgumentException(
                        "Unsupported value for ImpressApi.ColorRange: " + colorRange);
        }
    }

    private int validateMaxLuminance(int maxLuminance) {
        if (maxLuminance < 0 || maxLuminance > 65535) {
            throw new IllegalArgumentException(
                    "maxLuminance must be either 0 (unknown) or greater than 0 and smaller than"
                            + " 65536: "
                            + maxLuminance);
        }
        return maxLuminance;
    }

    @Override
    public void setup(@NonNull View view) {
        this.view = view;
        nSetup(getViewNativeHandle(view));
        resourceManager = new BindingsResourceManager(new Handler(Looper.getMainLooper()));
    }

    @Override
    public void onResume() {
        view.onResume();
    }

    @Override
    public void onPause() {
        view.onPause();
    }

    @Override
    @NonNull
    public BindingsResourceManager getBindingsResourceManager() {
        if (resourceManager == null) {
            throw new IllegalStateException("BindingsResourceManager is not initialized");
        }
        return resourceManager;
    }

    @Override
    public void releaseImageBasedLightingAsset(long iblToken) {
        nReleaseImageBasedLightingAsset(getViewNativeHandle(view), iblToken);
    }

    @Override
    @NonNull
    public ListenableFuture<Long> loadImageBasedLightingAsset(@NonNull String path) {
        return CallbackToFutureAdapter.getFuture(
                completer -> {
                    // TODO: b/374216912 - Add a cancellationListener to the completer here when the
                    // loading APIs support cancellation.
                    nLoadImageBasedLightingAssetFromPath(
                            getViewNativeHandle(view),
                            // The underlying C++ code will hold a reference to this (anoynomous)
                            // AssetLoader until the load is complete.
                            new AssetLoader() {

                                @Override
                                public void onSuccess(long value) {
                                    completer.set(value);
                                }

                                @Override
                                public void onFailure(@NonNull String message) {
                                    // We can safely check for the CANCELLED string here since we
                                    // know that the underlying absl Status code is being
                                    // translated to a java Exception and the message is being
                                    // propagated. Ideally the native code would generate a separate
                                    // signal call for this.
                                    // TODO: b/374217508 - Publish a more precisely typed Exception
                                    // interface for this.
                                    if (message.contains("CANCELLED")) {
                                        onCancelled(message);
                                    } else {
                                        completer.setException(new Exception(message));
                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull String message) {
                                    completer.setCancelled();
                                }
                            },
                            path);
                    return "LoadImageBasedLightingAsset Operation";
                });
    }

    @Override
    @NonNull
    public ListenableFuture<Long> loadImageBasedLightingAsset(
            byte @NonNull [] data, @NonNull String key) {
        return CallbackToFutureAdapter.getFuture(
                completer -> {
                    // TODO: b/374216912 - Add a cancellationListener to the completer here when the
                    // loading APIs support cancellation.
                    nLoadImageBasedLightingAssetFromByteArray(
                            getViewNativeHandle(view),
                            // The underlying C++ code will hold a reference to this (anoynomous)
                            // AssetLoader until the load is complete.
                            new AssetLoader() {

                                @Override
                                public void onSuccess(long value) {
                                    completer.set(value);
                                }

                                @Override
                                public void onFailure(@NonNull String message) {
                                    // We can safely check for the CANCELLED string here since we
                                    // know that the underlying absl Status code is being
                                    // translated to a java Exception and the message is being
                                    // propagated. Ideally the native code would generate a separate
                                    // signal call for this.
                                    // TODO: b/374217508 - Publish a more precisely typed Exception
                                    // interface for this.
                                    if (message.contains("CANCELLED")) {
                                        onCancelled(message);
                                    } else {
                                        completer.setException(new Exception(message));
                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull String message) {
                                    completer.setCancelled();
                                }
                            },
                            data,
                            key);
                    return "LoadImageBasedLightingAsset Operation";
                });
    }

    @Override
    @NonNull
    public ListenableFuture<Long> loadGltfAsset(@NonNull String path) {
        return CallbackToFutureAdapter.getFuture(
                completer -> {
                    // TODO: b/374216912 - Add a cancellationListener to the completer here when the
                    // loading APIs support cancellation.
                    nLoadGltfAssetFromPath(
                            getViewNativeHandle(view),
                            // The underlying C++ code will hold a reference to this (anoynomous)
                            // AssetLoader until the load is complete.
                            // TODO(b/394349866): Revisit the way C++ --> Java code is called back
                            // for the AssetLoader (proguard)
                            new AssetLoader() {

                                @Override
                                public void onSuccess(long value) {
                                    completer.set(value);
                                }

                                @Override
                                public void onFailure(@NonNull String message) {
                                    // We can safely check for the CANCELLED string here since we
                                    // know that the underlying absl Status code is being
                                    // translated to a java Exception and the message is being
                                    // propagated. Ideally the native code would generate a separate
                                    // signal call for this.
                                    // TODO: b/374217508 - Publish a more precisely typed Exception
                                    // interface for this.
                                    if (message.contains("CANCELLED")) {
                                        onCancelled(message);
                                    } else {
                                        completer.setException(new Exception(message));
                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull String message) {
                                    completer.setCancelled();
                                }
                            },
                            path);
                    return "LoadGltfAsset Operation";
                });
    }

    @Override
    @NonNull
    public ListenableFuture<Long> loadGltfAsset(byte @NonNull [] data, @NonNull String key) {
        return CallbackToFutureAdapter.getFuture(
                completer -> {
                    // TODO: b/374216912 - Add a cancellationListener to the completer here when the
                    // loading APIs support cancellation.
                    nLoadGltfAssetFromByteArray(
                            getViewNativeHandle(view),
                            // The underlying C++ code will hold a reference to this (anoynomous)
                            // AssetLoader until the load is complete.
                            new AssetLoader() {

                                @Override
                                public void onSuccess(long value) {
                                    completer.set(value);
                                }

                                @Override
                                public void onFailure(@NonNull String message) {
                                    // We can safely check for the CANCELLED string here since we
                                    // know that the underlying absl Status code is being
                                    // translated to a java Exception and the message is being
                                    // propagated.
                                    // TODO: b/374217508 - Publish a more precisely typed Exception
                                    // interface for this.
                                    if (message.contains("CANCELLED")) {
                                        onCancelled(message);
                                    } else {
                                        completer.setException(new Exception(message));
                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull String message) {
                                    completer.setCancelled();
                                }
                            },
                            data,
                            key);
                    return "LoadGltfAsset Operation";
                });
    }

    @Override
    public void releaseGltfAsset(long gltfToken) {
        nReleaseGltfAsset(getViewNativeHandle(view), gltfToken);
    }

    @Override
    @NonNull
    public ImpressNode instanceGltfModel(long gltfToken) {
        return new ImpressNode(
                nInstanceGltfModel(
                        getViewNativeHandle(view), gltfToken, /* enableCollider= */ false));
    }

    @Override
    @NonNull
    public ImpressNode instanceGltfModel(long gltfToken, boolean enableCollider) {
        return new ImpressNode(
                nInstanceGltfModel(getViewNativeHandle(view), gltfToken, enableCollider));
    }

    // TODO(b/376740308): Add support for toggling the collider on StereoSurface.
    @Override
    public void setGltfModelColliderEnabled(
            @NonNull ImpressNode impressNode, boolean enableCollider) {
        nSetGltfModelColliderEnabled(
                getViewNativeHandle(view), impressNode.getHandle(), enableCollider);
    }

    /**
     * Starts an animation on an instanced GLTFModel.
     *
     * @param impressNode The integer ID of the Impress node for the instance of the GLTF
     * @param animationName A nullable String which contains a requested animation to play. If null
     *     is provided, this will attempt to play the first animation it finds
     * @param looping True if the animation should loop. Note that if the animation is looped, the
     *     returned Future will never fire successfully.
     * @return a ListenableFuture which fires when the animation stops. It will return an exception
     *     if the animation can't play.
     */
    @Override
    @NonNull
    public ListenableFuture<Void> animateGltfModel(
            @NonNull ImpressNode impressNode, @Nullable String animationName, boolean looping) {

        return CallbackToFutureAdapter.getFuture(
                completer -> {
                    nAnimateGltfModel(
                            getViewNativeHandle(view),
                            impressNode.getHandle(),
                            animationName,
                            looping,
                            new AssetAnimator() {
                                // Hold a reference to the completer to ensure it isn't garbage
                                // collected until the C++ side releases the reference to the
                                // AssetAnimator. The future returned by
                                // CallbackToFutureAdapter.getFuture() aggressively tries to let the
                                // garbage collector clean up the completer as an optimization, we
                                // are concerned that this could cause the future to never fire, or
                                // cancel incorrectly and return an error, especially since the code
                                // that calls this simply allows the future to go out of scope
                                // without storing it. This might not actually be a problem, but
                                // this code shouldn't be harmful and should reduce the uncertainty.
                                // We should eventually have a different way of communicating
                                // animation completion back to the application. See b/362368652.
                                CallbackToFutureAdapter.Completer<Void> mCompleter = completer;

                                @Override
                                public void onComplete() {
                                    // Setting null here is required since we don't have a return
                                    // value.
                                    mCompleter.set(null);
                                }

                                @Override
                                public void onFailure(@NonNull String message) {
                                    // We can safely check for the CANCELLED string here since we
                                    // know that the underlying absl Status code is being
                                    // translated to a java Exception and the message is being
                                    // propagated. Ideally the native code would generate a separate
                                    // signal call for this.
                                    // TODO: b/374217508 - Publish a more precisely typed Exception
                                    // interface for this.
                                    if (message.contains("CANCELLED")) {
                                        onCancelled(message);
                                    } else {
                                        mCompleter.setException(new Exception(message));
                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull String message) {
                                    mCompleter.setCancelled();
                                }
                            });
                    return "AnimateGltfModel Operation";
                });
    }

    /**
     * Stops an animation on an instanced GLTFModel.
     *
     * @param impressNode The integer ID of the Impress node for the instance of the GLTF
     */
    @Override
    public void stopGltfModelAnimation(@NonNull ImpressNode impressNode) {
        nStopGltfModelAnimation(getViewNativeHandle(view), impressNode.getHandle());
    }

    @Override
    @NonNull
    public ImpressNode createImpressNode() {
        return new ImpressNode(nCreateImpressNode(getViewNativeHandle(view)));
    }

    @Override
    public void destroyImpressNode(@NonNull ImpressNode impressNode) {
        nDestroyImpressNode(getViewNativeHandle(view), impressNode.getHandle());
    }

    @Override
    public void setImpressNodeParent(
            @NonNull ImpressNode impressNodeChild, @NonNull ImpressNode impressNodeParent) {
        nSetImpressNodeParent(
                getViewNativeHandle(view),
                impressNodeChild.getHandle(),
                impressNodeParent.getHandle());
    }

    @Override
    @NonNull
    public ImpressNode createStereoSurface(@StereoMode int stereoMode) {
        return new ImpressNode(
                nCreateStereoSurfaceEntity(
                        getViewNativeHandle(view),
                        validateStereoMode(stereoMode),
                        ContentSecurityLevel.NONE,
                        /* useSuperSampling= */ false));
    }

    @Override
    @NonNull
    public ImpressNode createStereoSurface(
            @StereoMode int stereoMode, @ContentSecurityLevel int contentSecurityLevel) {
        return new ImpressNode(
                nCreateStereoSurfaceEntity(
                        getViewNativeHandle(view),
                        validateStereoMode(stereoMode),
                        validateContentSecurityLevel(contentSecurityLevel),
                        /* useSuperSampling= */ false));
    }

    @Override
    @NonNull
    public ImpressNode createStereoSurface(
            @StereoMode int stereoMode,
            @ContentSecurityLevel int contentSecurityLevel,
            boolean useSuperSampling) {
        return new ImpressNode(
                nCreateStereoSurfaceEntity(
                        getViewNativeHandle(view),
                        validateStereoMode(stereoMode),
                        validateContentSecurityLevel(contentSecurityLevel),
                        useSuperSampling));
    }

    @Override
    public void setStereoSurfaceEntityCanvasShapeQuad(
            @NonNull ImpressNode impressNode, float width, float height) {
        nSetStereoSurfaceEntityCanvasShapeQuad(
                getViewNativeHandle(view), impressNode.getHandle(), width, height);
    }

    @Override
    public void setStereoSurfaceEntityCanvasShapeSphere(
            @NonNull ImpressNode impressNode, float radius) {
        nSetStereoSurfaceEntityCanvasShapeSphere(
                getViewNativeHandle(view), impressNode.getHandle(), radius);
    }

    @Override
    public void setStereoSurfaceEntityCanvasShapeHemisphere(
            @NonNull ImpressNode impressNode, float radius) {
        nSetStereoSurfaceEntityCanvasShapeHemisphere(
                getViewNativeHandle(view), impressNode.getHandle(), radius);
    }

    @Override
    public void setStereoModeForStereoSurface(
            @NonNull ImpressNode panelImpressNode, @StereoMode int stereoMode) {
        nSetStereoModeForStereoSurfaceEntity(
                getViewNativeHandle(view),
                panelImpressNode.getHandle(),
                validateStereoMode(stereoMode));
    }

    @Override
    public void setContentColorMetadataForStereoSurface(
            @NonNull ImpressNode stereoSurfaceNode,
            @ColorSpace int colorSpace,
            @ColorTransfer int colorTransfer,
            @ColorRange int colorRange,
            int maxLuminance) {
        nSetContentColorMetadataForStereoSurfaceEntity(
                getViewNativeHandle(view),
                stereoSurfaceNode.getHandle(),
                validateColorSpace(colorSpace),
                validateColorTransfer(colorTransfer),
                validateColorRange(colorRange),
                validateMaxLuminance(maxLuminance));
    }

    @Override
    public void resetContentColorMetadataForStereoSurface(@NonNull ImpressNode stereoSurfaceNode) {
        nResetContentColorMetadataForStereoSurfaceEntity(
                getViewNativeHandle(view), stereoSurfaceNode.getHandle());
    }

    @Override
    public void setFeatherRadiusForStereoSurface(
            @NonNull ImpressNode panelImpressNode, float radiusX, float radiusY) {
        nSetFeatherRadiusForStereoSurfaceEntity(
                getViewNativeHandle(view), panelImpressNode.getHandle(), radiusX, radiusY);
    }

    @Override
    @NonNull
    public Surface getSurfaceFromStereoSurface(@NonNull ImpressNode panelImpressNode) {
        return nGetSurfaceFromStereoSurfaceEntity(
                getViewNativeHandle(view), panelImpressNode.getHandle());
    }

    @Override
    public void setPrimaryAlphaMaskForStereoSurface(
            @NonNull ImpressNode panelImpressNode, long alphaMask) {
        nSetPrimaryAlphaMaskForStereoSurfaceEntity(
                getViewNativeHandle(view), panelImpressNode.getHandle(), alphaMask);
    }

    @Override
    public void setAuxiliaryAlphaMaskForStereoSurface(
            @NonNull ImpressNode panelImpressNode, long alphaMask) {
        nSetAuxiliaryAlphaMaskForStereoSurfaceEntity(
                getViewNativeHandle(view), panelImpressNode.getHandle(), alphaMask);
    }

    @Override
    @NonNull
    public ListenableFuture<Texture> loadTexture(@NonNull String path) {
        return CallbackToFutureAdapter.getFuture(
                completer -> {
                    // TODO: b/374216912 - Add a cancellationListener to the completer here when the
                    // loading APIs support cancellation.
                    nLoadTexture(
                            getViewNativeHandle(view),
                            // The underlying C++ code will hold a reference to this (anoynomous)
                            // AssetLoader until the load is complete.
                            // TODO(b/394349866): Revisit the way C++ --> Java code is called back
                            // for the AssetLoader (proguard)
                            new AssetLoader() {

                                @Override
                                public void onSuccess(long value) {
                                    Texture texture =
                                            new Texture.Builder()
                                                    .setImpressApi(ImpressApiImpl.this)
                                                    .setNativeTexture(value)
                                                    .build();
                                    completer.set(texture);
                                }

                                @Override
                                public void onFailure(@NonNull String message) {
                                    // We can safely check for the CANCELLED string here since we
                                    // know that the underlying absl Status code is being
                                    // translated to a java Exception and the message is being
                                    // propagated. Ideally the native code would generate a separate
                                    // signal call for this.
                                    // TODO: b/374217508 - Publish a more precisely typed Exception
                                    // interface for this.
                                    if (message.contains("CANCELLED")) {
                                        onCancelled(message);
                                    } else {
                                        completer.setException(new Exception(message));
                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull String message) {
                                    completer.setCancelled();
                                }
                            },
                            path);
                    return "LoadTexture Operation";
                });
    }

    @Override
    @NonNull
    public Texture borrowReflectionTexture() {
        long textureHandle = nBorrowReflectionTexture(getViewNativeHandle(view));
        return new Texture.Builder()
                .setImpressApi(ImpressApiImpl.this)
                .setNativeTexture(textureHandle)
                .build();
    }

    @Override
    @NonNull
    public Texture getReflectionTextureFromIbl(long iblToken) {
        long textureHandle = nGetReflectionTextureFromIbl(getViewNativeHandle(view), iblToken);
        return new Texture.Builder()
                .setImpressApi(ImpressApiImpl.this)
                .setNativeTexture(textureHandle)
                .build();
    }

    @Override
    @NonNull
    public ListenableFuture<WaterMaterial> createWaterMaterial(boolean isAlphaMapVersion) {
        return CallbackToFutureAdapter.getFuture(
                completer -> {
                    // TODO: b/374216912 - Add a cancellationListener to the completer here when the
                    // loading APIs support cancellation.
                    nCreateWaterMaterial(
                            getViewNativeHandle(view),
                            // The underlying C++ code will hold a reference to this (anoynomous)
                            // AssetLoader until the load is complete.
                            // TODO(b/394349866): Revisit the way C++ --> Java code is called back
                            // for the AssetLoader (proguard)
                            new AssetLoader() {

                                @Override
                                public void onSuccess(long value) {
                                    WaterMaterial waterMaterial =
                                            new WaterMaterial.Builder()
                                                    .setImpressApi(ImpressApiImpl.this)
                                                    .setNativeMaterial(value)
                                                    .build();
                                    completer.set(waterMaterial);
                                }

                                @Override
                                public void onFailure(@NonNull String message) {
                                    // We can safely check for the CANCELLED string here since we
                                    // know that the underlying absl Status code is being
                                    // translated to a java Exception and the message is being
                                    // propagated. Ideally the native code would generate a separate
                                    // signal call for this.
                                    // TODO: b/374217508 - Publish a more precisely typed Exception
                                    // interface for this.
                                    if (message.contains("CANCELLED")) {
                                        onCancelled(message);
                                    } else {
                                        completer.setException(new Exception(message));
                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull String message) {
                                    completer.setCancelled();
                                }
                            },
                            isAlphaMapVersion);
                    return "CreateWaterMaterial Operation";
                });
    }

    @Override
    public void setReflectionMapOnWaterMaterial(
            long nativeWaterMaterial, long reflectionMap, @NonNull TextureSampler sampler) {
        nSetReflectionMapOnWaterMaterial(
                getViewNativeHandle(view),
                nativeWaterMaterial,
                reflectionMap,
                sampler.getMinFilter(),
                sampler.getMagFilter(),
                sampler.getWrapModeS(),
                sampler.getWrapModeT(),
                sampler.getWrapModeR(),
                sampler.getCompareMode(),
                sampler.getCompareFunc(),
                sampler.getAnisotropyLog2());
    }

    @Override
    public void setNormalMapOnWaterMaterial(
            long nativeWaterMaterial, long normalMap, @NonNull TextureSampler sampler) {
        nSetNormalMapOnWaterMaterial(
                getViewNativeHandle(view),
                nativeWaterMaterial,
                normalMap,
                sampler.getMinFilter(),
                sampler.getMagFilter(),
                sampler.getWrapModeS(),
                sampler.getWrapModeT(),
                sampler.getWrapModeR(),
                sampler.getCompareMode(),
                sampler.getCompareFunc(),
                sampler.getAnisotropyLog2());
    }

    @Override
    public void setNormalTilingOnWaterMaterial(long nativeWaterMaterial, float normalTiling) {
        nSetNormalTilingOnWaterMaterial(
                getViewNativeHandle(view), nativeWaterMaterial, normalTiling);
    }

    @Override
    public void setNormalSpeedOnWaterMaterial(long nativeWaterMaterial, float normalSpeed) {
        nSetNormalSpeedOnWaterMaterial(getViewNativeHandle(view), nativeWaterMaterial, normalSpeed);
    }

    @Override
    public void setAlphaStepMultiplierOnWaterMaterial(
            long nativeWaterMaterial, float alphaStepMultiplier) {
        nSetAlphaStepMultiplierOnWaterMaterial(
                getViewNativeHandle(view), nativeWaterMaterial, alphaStepMultiplier);
    }

    @Override
    public void setAlphaMapOnWaterMaterial(
            long nativeWaterMaterial, long alphaMap, @NonNull TextureSampler sampler) {
        nSetAlphaMapOnWaterMaterial(
                getViewNativeHandle(view),
                nativeWaterMaterial,
                alphaMap,
                sampler.getMinFilter(),
                sampler.getMagFilter(),
                sampler.getWrapModeS(),
                sampler.getWrapModeT(),
                sampler.getWrapModeR(),
                sampler.getCompareMode(),
                sampler.getCompareFunc(),
                sampler.getAnisotropyLog2());
    }

    @Override
    public void setNormalZOnWaterMaterial(long nativeWaterMaterial, float normalZ) {
        nSetNormalZOnWaterMaterial(getViewNativeHandle(view), nativeWaterMaterial, normalZ);
    }

    @Override
    public void setNormalBoundaryOnWaterMaterial(long nativeWaterMaterial, float normalBoundary) {
        nSetNormalBoundaryOnWaterMaterial(
                getViewNativeHandle(view), nativeWaterMaterial, normalBoundary);
    }

    @Override
    public void setAlphaStepUOnWaterMaterial(
            long nativeWaterMaterial, float x, float y, float z, float w) {
        throw new UnsupportedOperationException("Stub API to be removed.");
    }

    @Override
    public void setAlphaStepVOnWaterMaterial(
            long nativeWaterMaterial, float x, float y, float z, float w) {
        throw new UnsupportedOperationException("Stub API to be removed.");
    }

    @Override
    @NonNull
    public ListenableFuture<KhronosPbrMaterial> createKhronosPbrMaterial(
            @NonNull KhronosPbrMaterialSpec spec) {
        return CallbackToFutureAdapter.getFuture(
                completer -> {
                    // TODO: b/374216912 - Add a cancellationListener to the completer here when the
                    // loading APIs support cancellation.
                    nCreateGenericMaterial(
                            getViewNativeHandle(view),
                            // The underlying C++ code will hold a reference to this (anoynomous)
                            // AssetLoader until the load is complete.
                            // TODO(b/394349866): Revisit the way C++ --> Java code is called back
                            // for the AssetLoader (proguard)
                            new AssetLoader() {

                                @Override
                                public void onSuccess(long value) {
                                    KhronosPbrMaterial khronosPbrMaterial =
                                            new KhronosPbrMaterial.Builder()
                                                    .setImpressApi(ImpressApiImpl.this)
                                                    .setNativeMaterial(value)
                                                    .build();
                                    completer.set(khronosPbrMaterial);
                                }

                                @Override
                                public void onFailure(@NonNull String message) {
                                    // We can safely check for the CANCELLED string here since we
                                    // know that the underlying absl Status code is being
                                    // translated to a java Exception and the message is being
                                    // propagated. Ideally the native code would generate a separate
                                    // signal call for this.
                                    // TODO: b/374217508 - Publish a more precisely typed Exception
                                    // interface for this.
                                    if (message.contains("CANCELLED")) {
                                        onCancelled(message);
                                    } else {
                                        completer.setException(new Exception(message));
                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull String message) {
                                    completer.setCancelled();
                                }
                            },
                            spec.getLightingModel(),
                            spec.getBlendMode(),
                            spec.getDoubleSidedMode());
                    return "CreateKhronosPbrMaterial Operation";
                });
    }

    @Override
    public void setBaseColorTextureOnKhronosPbrMaterial(
            long nativeKhronosPbrMaterial, long baseColorTexture, @NonNull TextureSampler sampler) {
        nSetBaseColorTextureOnGenericMaterial(
                getViewNativeHandle(view),
                nativeKhronosPbrMaterial,
                baseColorTexture,
                sampler.getMinFilter(),
                sampler.getMagFilter(),
                sampler.getWrapModeS(),
                sampler.getWrapModeT(),
                sampler.getWrapModeR(),
                sampler.getCompareMode(),
                sampler.getCompareFunc(),
                sampler.getAnisotropyLog2());
    }

    @Override
    public void setBaseColorUvTransformOnKhronosPbrMaterial(
            long nativeKhronosPbrMaterial,
            float ux,
            float uy,
            float uz,
            float vx,
            float vy,
            float vz,
            float wx,
            float wy,
            float wz) {
        nSetBaseColorUvTransformOnGenericMaterial(
                getViewNativeHandle(view),
                nativeKhronosPbrMaterial,
                ux,
                uy,
                uz,
                vx,
                vy,
                vz,
                wx,
                wy,
                wz);
    }

    @Override
    public void setBaseColorFactorsOnKhronosPbrMaterial(
            long nativeKhronosPbrMaterial, float x, float y, float z, float w) {
        nSetBaseColorFactorsOnGenericMaterial(
                getViewNativeHandle(view), nativeKhronosPbrMaterial, x, y, z, w);
    }

    @Override
    public void setMetallicRoughnessTextureOnKhronosPbrMaterial(
            long nativeKhronosPbrMaterial,
            long metallicRoughnessTexture,
            @NonNull TextureSampler sampler) {
        nSetMetallicRoughnessTextureOnGenericMaterial(
                getViewNativeHandle(view),
                nativeKhronosPbrMaterial,
                metallicRoughnessTexture,
                sampler.getMinFilter(),
                sampler.getMagFilter(),
                sampler.getWrapModeS(),
                sampler.getWrapModeT(),
                sampler.getWrapModeR(),
                sampler.getCompareMode(),
                sampler.getCompareFunc(),
                sampler.getAnisotropyLog2());
    }

    @Override
    public void setMetallicRoughnessUvTransformOnKhronosPbrMaterial(
            long nativeKhronosPbrMaterial,
            float ux,
            float uy,
            float uz,
            float vx,
            float vy,
            float vz,
            float wx,
            float wy,
            float wz) {
        nSetMetallicRoughnessUvTransformOnGenericMaterial(
                getViewNativeHandle(view),
                nativeKhronosPbrMaterial,
                ux,
                uy,
                uz,
                vx,
                vy,
                vz,
                wx,
                wy,
                wz);
    }

    @Override
    public void setMetallicFactorOnKhronosPbrMaterial(long nativeKhronosPbrMaterial, float factor) {
        nSetMetallicFactorOnGenericMaterial(
                getViewNativeHandle(view), nativeKhronosPbrMaterial, factor);
    }

    @Override
    public void setRoughnessFactorOnKhronosPbrMaterial(
            long nativeKhronosPbrMaterial, float factor) {
        nSetRoughnessFactorOnGenericMaterial(
                getViewNativeHandle(view), nativeKhronosPbrMaterial, factor);
    }

    @Override
    public void setNormalTextureOnKhronosPbrMaterial(
            long nativeKhronosPbrMaterial, long normalTexture, @NonNull TextureSampler sampler) {
        nSetNormalTextureOnGenericMaterial(
                getViewNativeHandle(view),
                nativeKhronosPbrMaterial,
                normalTexture,
                sampler.getMinFilter(),
                sampler.getMagFilter(),
                sampler.getWrapModeS(),
                sampler.getWrapModeT(),
                sampler.getWrapModeR(),
                sampler.getCompareMode(),
                sampler.getCompareFunc(),
                sampler.getAnisotropyLog2());
    }

    @Override
    public void setNormalUvTransformOnKhronosPbrMaterial(
            long nativeKhronosPbrMaterial,
            float ux,
            float uy,
            float uz,
            float vx,
            float vy,
            float vz,
            float wx,
            float wy,
            float wz) {
        nSetNormalUvTransformOnGenericMaterial(
                getViewNativeHandle(view),
                nativeKhronosPbrMaterial,
                ux,
                uy,
                uz,
                vx,
                vy,
                vz,
                wx,
                wy,
                wz);
    }

    @Override
    public void setNormalFactorOnKhronosPbrMaterial(long nativeKhronosPbrMaterial, float factor) {
        nSetNormalFactorOnGenericMaterial(
                getViewNativeHandle(view), nativeKhronosPbrMaterial, factor);
    }

    @Override
    public void setAmbientOcclusionTextureOnKhronosPbrMaterial(
            long nativeKhronosPbrMaterial,
            long ambientOcclusionTexture,
            @NonNull TextureSampler sampler) {
        nSetAmbientOcclusionTextureOnGenericMaterial(
                getViewNativeHandle(view),
                nativeKhronosPbrMaterial,
                ambientOcclusionTexture,
                sampler.getMinFilter(),
                sampler.getMagFilter(),
                sampler.getWrapModeS(),
                sampler.getWrapModeT(),
                sampler.getWrapModeR(),
                sampler.getCompareMode(),
                sampler.getCompareFunc(),
                sampler.getAnisotropyLog2());
    }

    @Override
    public void setAmbientOcclusionUvTransformOnKhronosPbrMaterial(
            long nativeKhronosPbrMaterial,
            float ux,
            float uy,
            float uz,
            float vx,
            float vy,
            float vz,
            float wx,
            float wy,
            float wz) {
        nSetAmbientOcclusionUvTransformOnGenericMaterial(
                getViewNativeHandle(view),
                nativeKhronosPbrMaterial,
                ux,
                uy,
                uz,
                vx,
                vy,
                vz,
                wx,
                wy,
                wz);
    }

    @Override
    public void setAmbientOcclusionFactorOnKhronosPbrMaterial(
            long nativeKhronosPbrMaterial, float factor) {
        nSetAmbientOcclusionFactorOnGenericMaterial(
                getViewNativeHandle(view), nativeKhronosPbrMaterial, factor);
    }

    @Override
    public void setEmissiveTextureOnKhronosPbrMaterial(
            long nativeKhronosPbrMaterial, long emissiveTexture, @NonNull TextureSampler sampler) {
        nSetEmissiveTextureOnGenericMaterial(
                getViewNativeHandle(view),
                nativeKhronosPbrMaterial,
                emissiveTexture,
                sampler.getMinFilter(),
                sampler.getMagFilter(),
                sampler.getWrapModeS(),
                sampler.getWrapModeT(),
                sampler.getWrapModeR(),
                sampler.getCompareMode(),
                sampler.getCompareFunc(),
                sampler.getAnisotropyLog2());
    }

    @Override
    public void setEmissiveUvTransformOnKhronosPbrMaterial(
            long nativeKhronosPbrMaterial,
            float ux,
            float uy,
            float uz,
            float vx,
            float vy,
            float vz,
            float wx,
            float wy,
            float wz) {
        nSetEmissiveUvTransformOnGenericMaterial(
                getViewNativeHandle(view),
                nativeKhronosPbrMaterial,
                ux,
                uy,
                uz,
                vx,
                vy,
                vz,
                wx,
                wy,
                wz);
    }

    @Override
    public void setEmissiveFactorsOnKhronosPbrMaterial(
            long nativeKhronosPbrMaterial, float x, float y, float z) {
        nSetEmissiveFactorsOnGenericMaterial(
                getViewNativeHandle(view), nativeKhronosPbrMaterial, x, y, z);
    }

    @Override
    public void setClearcoatTextureOnKhronosPbrMaterial(
            long nativeKhronosPbrMaterial, long clearcoatTexture, @NonNull TextureSampler sampler) {
        nSetClearcoatTextureOnGenericMaterial(
                getViewNativeHandle(view),
                nativeKhronosPbrMaterial,
                clearcoatTexture,
                sampler.getMinFilter(),
                sampler.getMagFilter(),
                sampler.getWrapModeS(),
                sampler.getWrapModeT(),
                sampler.getWrapModeR(),
                sampler.getCompareMode(),
                sampler.getCompareFunc(),
                sampler.getAnisotropyLog2());
    }

    @Override
    public void setClearcoatNormalTextureOnKhronosPbrMaterial(
            long nativeKhronosPbrMaterial,
            long clearcoatNormalTexture,
            @NonNull TextureSampler sampler) {
        nSetClearcoatNormalTextureOnGenericMaterial(
                getViewNativeHandle(view),
                nativeKhronosPbrMaterial,
                clearcoatNormalTexture,
                sampler.getMinFilter(),
                sampler.getMagFilter(),
                sampler.getWrapModeS(),
                sampler.getWrapModeT(),
                sampler.getWrapModeR(),
                sampler.getCompareMode(),
                sampler.getCompareFunc(),
                sampler.getAnisotropyLog2());
    }

    @Override
    public void setClearcoatRoughnessTextureOnKhronosPbrMaterial(
            long nativeKhronosPbrMaterial,
            long clearcoatRoughnessTexture,
            @NonNull TextureSampler sampler) {
        nSetClearcoatRoughnessTextureOnGenericMaterial(
                getViewNativeHandle(view),
                nativeKhronosPbrMaterial,
                clearcoatRoughnessTexture,
                sampler.getMinFilter(),
                sampler.getMagFilter(),
                sampler.getWrapModeS(),
                sampler.getWrapModeT(),
                sampler.getWrapModeR(),
                sampler.getCompareMode(),
                sampler.getCompareFunc(),
                sampler.getAnisotropyLog2());
    }

    @Override
    public void setClearcoatFactorsOnKhronosPbrMaterial(
            long nativeKhronosPbrMaterial, float intensity, float roughness, float normal) {
        nSetClearcoatFactorsOnGenericMaterial(
                getViewNativeHandle(view), nativeKhronosPbrMaterial, intensity, roughness, normal);
    }

    @Override
    public void setSheenColorTextureOnKhronosPbrMaterial(
            long nativeKhronosPbrMaterial,
            long sheenColorTexture,
            @NonNull TextureSampler sampler) {
        nSetSheenColorTextureOnGenericMaterial(
                getViewNativeHandle(view),
                nativeKhronosPbrMaterial,
                sheenColorTexture,
                sampler.getMinFilter(),
                sampler.getMagFilter(),
                sampler.getWrapModeS(),
                sampler.getWrapModeT(),
                sampler.getWrapModeR(),
                sampler.getCompareMode(),
                sampler.getCompareFunc(),
                sampler.getAnisotropyLog2());
    }

    @Override
    public void setSheenColorFactorsOnKhronosPbrMaterial(
            long nativeKhronosPbrMaterial, float x, float y, float z) {
        nSetSheenColorFactorsOnGenericMaterial(
                getViewNativeHandle(view), nativeKhronosPbrMaterial, x, y, z);
    }

    @Override
    public void setSheenRoughnessTextureOnKhronosPbrMaterial(
            long nativeKhronosPbrMaterial,
            long sheenRoughnessTexture,
            @NonNull TextureSampler sampler) {
        nSetSheenRoughnessTextureOnGenericMaterial(
                getViewNativeHandle(view),
                nativeKhronosPbrMaterial,
                sheenRoughnessTexture,
                sampler.getMinFilter(),
                sampler.getMagFilter(),
                sampler.getWrapModeS(),
                sampler.getWrapModeT(),
                sampler.getWrapModeR(),
                sampler.getCompareMode(),
                sampler.getCompareFunc(),
                sampler.getAnisotropyLog2());
    }

    @Override
    public void setSheenRoughnessFactorOnKhronosPbrMaterial(
            long nativeKhronosPbrMaterial, float factor) {
        nSetSheenRoughnessFactorOnGenericMaterial(
                getViewNativeHandle(view), nativeKhronosPbrMaterial, factor);
    }

    @Override
    public void setTransmissionTextureOnKhronosPbrMaterial(
            long nativeKhronosPbrMaterial,
            long transmissionTexture,
            @NonNull TextureSampler sampler) {
        nSetTransmissionTextureOnGenericMaterial(
                getViewNativeHandle(view),
                nativeKhronosPbrMaterial,
                transmissionTexture,
                sampler.getMinFilter(),
                sampler.getMagFilter(),
                sampler.getWrapModeS(),
                sampler.getWrapModeT(),
                sampler.getWrapModeR(),
                sampler.getCompareMode(),
                sampler.getCompareFunc(),
                sampler.getAnisotropyLog2());
    }

    @Override
    public void setTransmissionUvTransformOnKhronosPbrMaterial(
            long nativeKhronosPbrMaterial,
            float ux,
            float uy,
            float uz,
            float vx,
            float vy,
            float vz,
            float wx,
            float wy,
            float wz) {
        nSetTransmissionUvTransformOnGenericMaterial(
                getViewNativeHandle(view),
                nativeKhronosPbrMaterial,
                ux,
                uy,
                uz,
                vx,
                vy,
                vz,
                wx,
                wy,
                wz);
    }

    @Override
    public void setTransmissionFactorOnKhronosPbrMaterial(
            long nativeKhronosPbrMaterial, float factor) {
        nSetTransmissionFactorOnGenericMaterial(
                getViewNativeHandle(view), nativeKhronosPbrMaterial, factor);
    }

    @Override
    public void setIndexOfRefractionOnKhronosPbrMaterial(
            long nativeKhronosPbrMaterial, float indexOfRefraction) {
        nSetIndexOfRefractionOnGenericMaterial(
                getViewNativeHandle(view), nativeKhronosPbrMaterial, indexOfRefraction);
    }

    @Override
    public void setAlphaCutoffOnKhronosPbrMaterial(
            long nativeKhronosPbrMaterial, float alphaCutoff) {
        nSetAlphaCutoffOnGenericMaterial(
                getViewNativeHandle(view), nativeKhronosPbrMaterial, alphaCutoff);
    }

    @Override
    public void destroyNativeObject(long nativeHandle) {
        nDestroyNativeObject(getViewNativeHandle(view), nativeHandle);
    }

    @Override
    public void setMaterialOverride(
            @NonNull ImpressNode impressNode,
            long nativeMaterial,
            @NonNull String nodeName,
            int primitiveIndex) {
        nSetMaterialOverride(
                getViewNativeHandle(view),
                impressNode.getHandle(),
                nativeMaterial,
                nodeName,
                primitiveIndex);
    }

    @Override
    public void clearMaterialOverride(
            @NonNull ImpressNode impressNode, @NonNull String nodeName, int primitiveIndex) {
        nClearMaterialOverride(
                getViewNativeHandle(view), impressNode.getHandle(), nodeName, primitiveIndex);
    }

    @Override
    public void setPreferredEnvironmentLight(long iblToken) {
        nSetEnvironmentLight(getViewNativeHandle(view), iblToken);
    }

    @Override
    public void clearPreferredEnvironmentIblAsset() {
        nClearEnvironmentLight(getViewNativeHandle(view));
    }

    @Override
    public void disposeAllResources() {
        nDisposeAllResources(getViewNativeHandle(view));
    }

    private long getViewNativeHandle(View view) {
        if (view != null) {
            return view.getNativeHandle();
        }
        return -1;
    }

    // returns the bridge handle after it's been initialized.
    private static native void nSetup(long view);

    private static native void nReleaseImageBasedLightingAsset(long view, long assetToken);

    private static native void nLoadImageBasedLightingAssetFromPath(
            long view, AssetLoader assetLoader, String path);

    private static native void nLoadImageBasedLightingAssetFromByteArray(
            long view, AssetLoader assetLoader, byte[] data, String key);

    private static native void nLoadGltfAssetFromPath(
            long view, AssetLoader assetLoader, String path);

    private static native void nLoadGltfAssetFromByteArray(
            long view, AssetLoader assetLoader, byte[] data, String key);

    private static native void nReleaseGltfAsset(long view, long gltfToken);

    private static native int nInstanceGltfModel(long view, long gltfToken, boolean enableCollider);

    private static native void nSetGltfModelColliderEnabled(
            long view, long gltfToken, boolean enableCollider);

    private static native void nAnimateGltfModel(
            long view,
            int impressNode,
            String animationName,
            boolean loop,
            AssetAnimator assetAnimator);

    private static native void nStopGltfModelAnimation(long view, int impressNode);

    private static native int nCreateImpressNode(long view);

    private static native void nDestroyImpressNode(long view, int impressNode);

    private static native void nSetImpressNodeParent(
            long view, int impressNodeChild, int impressNodeParent);

    private static native int nCreateStereoSurfaceEntity(
            long view, int stereoMode, int contentSecurityLevel, boolean useSuperSampling);

    private static native void nSetStereoSurfaceEntityCanvasShapeQuad(
            long view, int impressNode, float width, float height);

    private static native void nSetStereoSurfaceEntityCanvasShapeSphere(
            long view, int impressNode, float radius);

    private static native void nSetStereoSurfaceEntityCanvasShapeHemisphere(
            long view, int impressNode, float radius);

    private static native Surface nGetSurfaceFromStereoSurfaceEntity(
            long view, int panelImpressNode);

    private static native void nSetFeatherRadiusForStereoSurfaceEntity(
            long view, int panelImpressNode, float radiusX, float radiusY);

    private static native void nSetStereoModeForStereoSurfaceEntity(
            long view, int panelImpressNode, int stereoMode);

    private static native void nSetContentColorMetadataForStereoSurfaceEntity(
            long view,
            int stereoSurfaceNode,
            int colorSpace,
            int colorTransfer,
            int colorRange,
            int maxLuminance);

    private static native void nResetContentColorMetadataForStereoSurfaceEntity(
            long view, int stereoSurfaceNode);

    private static native void nSetPrimaryAlphaMaskForStereoSurfaceEntity(
            long view, int panelImpressNode, long alphaMask);

    private static native void nSetAuxiliaryAlphaMaskForStereoSurfaceEntity(
            long view, int panelImpressNode, long alphaMask);

    private static native void nLoadTexture(long view, AssetLoader assetLoader, String path);

    private static native long nBorrowReflectionTexture(long view);

    private static native long nGetReflectionTextureFromIbl(long view, long iblToken);

    private static native void nCreateWaterMaterial(
            long view, AssetLoader assetLoader, boolean isAlphaMapVersion);

    private static native void nSetReflectionMapOnWaterMaterial(
            long view,
            long nativeWaterMaterial,
            long reflectionMap,
            int minFilter,
            int magFilter,
            int wrapModeS,
            int wrapModeT,
            int wrapModeR,
            int compareMode,
            int compareFunc,
            int anisotropyLog2);

    private static native void nSetNormalMapOnWaterMaterial(
            long view,
            long nativeWaterMaterial,
            long normalMap,
            int minFilter,
            int magFilter,
            int wrapModeS,
            int wrapModeT,
            int wrapModeR,
            int compareMode,
            int compareFunc,
            int anisotropyLog2);

    private static native void nSetNormalTilingOnWaterMaterial(
            long view, long nativeWaterMaterial, float normalTiling);

    private static native void nSetNormalSpeedOnWaterMaterial(
            long view, long nativeWaterMaterial, float normalSpeed);

    private static native void nSetAlphaStepUOnWaterMaterial(
            long view, long nativeWaterMaterial, float x, float y, float z, float w);

    private static native void nSetAlphaStepVOnWaterMaterial(
            long view, long nativeWaterMaterial, float x, float y, float z, float w);

    private static native void nSetAlphaStepMultiplierOnWaterMaterial(
            long view, long nativeWaterMaterial, float alphaStepMultiplier);

    private static native void nSetAlphaMapOnWaterMaterial(
            long view,
            long nativeWaterMaterial,
            long alphaMap,
            int minFilter,
            int magFilter,
            int wrapModeS,
            int wrapModeT,
            int wrapModeR,
            int compareMode,
            int compareFunc,
            int anisotropyLog2);

    private static native void nSetNormalZOnWaterMaterial(
            long view, long nativeWaterMaterial, float normalZ);

    private static native void nSetNormalBoundaryOnWaterMaterial(
            long view, long nativeWaterMaterial, float normalBoundary);

    private native void nCreateGenericMaterial(
            long view,
            AssetLoader assetLoader,
            int lightingModel,
            int blendMode,
            int doubleSidedMode);

    private native void nSetBaseColorTextureOnGenericMaterial(
            long view,
            long nativeGenericMaterial,
            long baseColorTexture,
            int minFilter,
            int magFilter,
            int wrapModeS,
            int wrapModeT,
            int wrapModeR,
            int compareMode,
            int compareFunc,
            int anisotropyLog2);

    private native void nSetBaseColorUvTransformOnGenericMaterial(
            long view,
            long nativeGenericMaterial,
            float ux,
            float uy,
            float uz,
            float vx,
            float vy,
            float vz,
            float wx,
            float wy,
            float wz);

    private native void nSetBaseColorFactorsOnGenericMaterial(
            long view, long nativeGenericMaterial, float x, float y, float z, float w);

    private native void nSetMetallicRoughnessTextureOnGenericMaterial(
            long view,
            long nativeGenericMaterial,
            long metallicRoughnessTexture,
            int minFilter,
            int magFilter,
            int wrapModeS,
            int wrapModeT,
            int wrapModeR,
            int compareMode,
            int compareFunc,
            int anisotropyLog2);

    private native void nSetMetallicRoughnessUvTransformOnGenericMaterial(
            long view,
            long nativeGenericMaterial,
            float ux,
            float uy,
            float uz,
            float vx,
            float vy,
            float vz,
            float wx,
            float wy,
            float wz);

    private native void nSetMetallicFactorOnGenericMaterial(
            long view, long nativeGenericMaterial, float factor);

    private native void nSetRoughnessFactorOnGenericMaterial(
            long view, long nativeGenericMaterial, float factor);

    private native void nSetNormalTextureOnGenericMaterial(
            long view,
            long nativeGenericMaterial,
            long normalTexture,
            int minFilter,
            int magFilter,
            int wrapModeS,
            int wrapModeT,
            int wrapModeR,
            int compareMode,
            int compareFunc,
            int anisotropyLog2);

    private native void nSetNormalUvTransformOnGenericMaterial(
            long view,
            long nativeGenericMaterial,
            float ux,
            float uy,
            float uz,
            float vx,
            float vy,
            float vz,
            float wx,
            float wy,
            float wz);

    private native void nSetNormalFactorOnGenericMaterial(
            long view, long nativeGenericMaterial, float factor);

    private native void nSetAmbientOcclusionTextureOnGenericMaterial(
            long view,
            long nativeGenericMaterial,
            long ambientOcclusionTexture,
            int minFilter,
            int magFilter,
            int wrapModeS,
            int wrapModeT,
            int wrapModeR,
            int compareMode,
            int compareFunc,
            int anisotropyLog2);

    private native void nSetAmbientOcclusionUvTransformOnGenericMaterial(
            long view,
            long nativeGenericMaterial,
            float ux,
            float uy,
            float uz,
            float vx,
            float vy,
            float vz,
            float wx,
            float wy,
            float wz);

    private native void nSetAmbientOcclusionFactorOnGenericMaterial(
            long view, long nativeGenericMaterial, float factor);

    private native void nSetEmissiveTextureOnGenericMaterial(
            long view,
            long nativeGenericMaterial,
            long emissiveTexture,
            int minFilter,
            int magFilter,
            int wrapModeS,
            int wrapModeT,
            int wrapModeR,
            int compareMode,
            int compareFunc,
            int anisotropyLog2);

    private native void nSetEmissiveUvTransformOnGenericMaterial(
            long view,
            long nativeGenericMaterial,
            float ux,
            float uy,
            float uz,
            float vx,
            float vy,
            float vz,
            float wx,
            float wy,
            float wz);

    private native void nSetEmissiveFactorsOnGenericMaterial(
            long view, long nativeGenericMaterial, float x, float y, float z);

    private native void nSetClearcoatTextureOnGenericMaterial(
            long view,
            long nativeGenericMaterial,
            long clearcoatTexture,
            int minFilter,
            int magFilter,
            int wrapModeS,
            int wrapModeT,
            int wrapModeR,
            int compareMode,
            int compareFunc,
            int anisotropyLog2);

    private native void nSetClearcoatNormalTextureOnGenericMaterial(
            long view,
            long nativeGenericMaterial,
            long clearcoatNormalTexture,
            int minFilter,
            int magFilter,
            int wrapModeS,
            int wrapModeT,
            int wrapModeR,
            int compareMode,
            int compareFunc,
            int anisotropyLog2);

    private native void nSetClearcoatRoughnessTextureOnGenericMaterial(
            long view,
            long nativeGenericMaterial,
            long clearcoatRoughnessTexture,
            int minFilter,
            int magFilter,
            int wrapModeS,
            int wrapModeT,
            int wrapModeR,
            int compareMode,
            int compareFunc,
            int anisotropyLog2);

    private native void nSetClearcoatFactorsOnGenericMaterial(
            long view, long nativeGenericMaterial, float intensity, float roughness, float normal);

    private native void nSetSheenColorTextureOnGenericMaterial(
            long view,
            long nativeGenericMaterial,
            long sheenColorTexture,
            int minFilter,
            int magFilter,
            int wrapModeS,
            int wrapModeT,
            int wrapModeR,
            int compareMode,
            int compareFunc,
            int anisotropyLog2);

    private native void nSetSheenColorFactorsOnGenericMaterial(
            long view, long nativeGenericMaterial, float x, float y, float z);

    private native void nSetSheenRoughnessTextureOnGenericMaterial(
            long view,
            long nativeGenericMaterial,
            long sheenRoughnessTexture,
            int minFilter,
            int magFilter,
            int wrapModeS,
            int wrapModeT,
            int wrapModeR,
            int compareMode,
            int compareFunc,
            int anisotropyLog2);

    private native void nSetSheenRoughnessFactorOnGenericMaterial(
            long view, long nativeGenericMaterial, float factor);

    private native void nSetTransmissionTextureOnGenericMaterial(
            long view,
            long nativeGenericMaterial,
            long transmissionTexture,
            int minFilter,
            int magFilter,
            int wrapModeS,
            int wrapModeT,
            int wrapModeR,
            int compareMode,
            int compareFunc,
            int anisotropyLog2);

    private native void nSetTransmissionUvTransformOnGenericMaterial(
            long view,
            long nativeGenericMaterial,
            float ux,
            float uy,
            float uz,
            float vx,
            float vy,
            float vz,
            float wx,
            float wy,
            float wz);

    private native void nSetTransmissionFactorOnGenericMaterial(
            long view, long nativeGenericMaterial, float factor);

    private native void nSetIndexOfRefractionOnGenericMaterial(
            long view, long nativeGenericMaterial, float indexOfRefraction);

    private native void nSetAlphaCutoffOnGenericMaterial(
            long view, long nativeGenericMaterial, float alphaCutoff);

    private static native void nDestroyNativeObject(long view, long nativeHandle);

    private static native void nSetMaterialOverride(
            long view, int impressNode, long nativeMaterial, String nodeName, int primitive_index);

    private static native void nClearMaterialOverride(
            long view, int impressNode, String nodeName, int primitive_index);

    private static native void nSetEnvironmentLight(long view, long iblToken);

    private static native void nClearEnvironmentLight(long view);

    private static native void nDisposeAllResources(long view);
}
