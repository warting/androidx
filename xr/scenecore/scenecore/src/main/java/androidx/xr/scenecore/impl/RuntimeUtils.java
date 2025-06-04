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

package androidx.xr.scenecore.impl;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.xr.runtime.internal.ActivityPose.HitTestFilter;
import androidx.xr.runtime.internal.ActivityPose.HitTestFilterValue;
import androidx.xr.runtime.internal.CameraViewActivityPose.Fov;
import androidx.xr.runtime.internal.Entity;
import androidx.xr.runtime.internal.HitTestResult;
import androidx.xr.runtime.internal.InputEvent;
import androidx.xr.runtime.internal.InputEvent.Companion.HitInfo;
import androidx.xr.runtime.internal.KhronosPbrMaterialSpec;
import androidx.xr.runtime.internal.PixelDimensions;
import androidx.xr.runtime.internal.PlaneSemantic;
import androidx.xr.runtime.internal.PlaneType;
import androidx.xr.runtime.internal.ResizeEvent;
import androidx.xr.runtime.internal.SpatialCapabilities;
import androidx.xr.runtime.internal.SpatialPointerIcon;
import androidx.xr.runtime.internal.SpatialPointerIconType;
import androidx.xr.runtime.internal.SpatialVisibility;
import androidx.xr.runtime.internal.TextureSampler;
import androidx.xr.runtime.math.Matrix4;
import androidx.xr.runtime.math.Pose;
import androidx.xr.runtime.math.Quaternion;
import androidx.xr.runtime.math.Vector3;
import androidx.xr.scenecore.impl.perception.Plane;

import com.android.extensions.xr.XrExtensions;
import com.android.extensions.xr.environment.EnvironmentVisibilityState;
import com.android.extensions.xr.environment.PassthroughVisibilityState;
import com.android.extensions.xr.node.Mat4f;
import com.android.extensions.xr.node.NodeTransaction;
import com.android.extensions.xr.node.Quatf;
import com.android.extensions.xr.node.ReformEvent;
import com.android.extensions.xr.node.Vec3;
import com.android.extensions.xr.space.VisibilityState;

final class RuntimeUtils {
    private RuntimeUtils() {}

    /** Convert JXRCore PlaneType to a PerceptionLibrary Plane.Type. */
    static Plane.Type getPlaneType(PlaneType planeType) {
        switch (planeType) {
            case HORIZONTAL:
                // TODO: b/329888869 - Allow Horizontal to work as both upward and downward facing.
                // To do
                // this it would have to return a collection.
                return Plane.Type.HORIZONTAL_UPWARD_FACING;
            case VERTICAL:
                return Plane.Type.VERTICAL;
            case ANY:
                return Plane.Type.ARBITRARY;
        }
        return Plane.Type.ARBITRARY;
    }

    /** Convert a Perception Plane.Type to a JXRCore PlaneType. */
    static PlaneType getPlaneType(Plane.Type planeType) {
        switch (planeType) {
            case HORIZONTAL_UPWARD_FACING:
            case HORIZONTAL_DOWNWARD_FACING:
                return PlaneType.HORIZONTAL;
            case VERTICAL:
                return PlaneType.VERTICAL;
            default:
                return PlaneType.ANY;
        }
    }

    /** Convert a JXRCore PlaneSemantic to a PerceptionLibrary Plane.Label. */
    static Plane.Label getPlaneLabel(PlaneSemantic planeSemantic) {
        switch (planeSemantic) {
            case WALL:
                return Plane.Label.WALL;
            case FLOOR:
                return Plane.Label.FLOOR;
            case CEILING:
                return Plane.Label.CEILING;
            case TABLE:
                return Plane.Label.TABLE;
            case ANY:
                return Plane.Label.UNKNOWN;
        }
        return Plane.Label.UNKNOWN;
    }

    /** Convert a PerceptionLibrary Plane.Label to a JXRCore PlaneSemantic. */
    static PlaneSemantic getPlaneSemantic(Plane.Label planeLabel) {
        switch (planeLabel) {
            case WALL:
                return PlaneSemantic.WALL;
            case FLOOR:
                return PlaneSemantic.FLOOR;
            case CEILING:
                return PlaneSemantic.CEILING;
            case TABLE:
                return PlaneSemantic.TABLE;
            default:
                return PlaneSemantic.ANY;
        }
    }

    @Nullable
    @VisibleForTesting
    static HitInfo getHitInfo(
            com.android.extensions.xr.node.InputEvent.HitInfo xrHitInfo,
            EntityManager entityManager) {
        if (xrHitInfo == null
                || xrHitInfo.getInputNode() == null
                || xrHitInfo.getTransform() == null) {
            return null;
        }
        // TODO: b/377541143 - Replace instance equality check in EntityManager.
        Entity hitEntity = entityManager.getEntityForNode(xrHitInfo.getInputNode());
        if (hitEntity == null) {
            return null;
        }
        return new HitInfo(
                hitEntity,
                (xrHitInfo.getHitPosition() == null)
                        ? null
                        : getVector3(xrHitInfo.getHitPosition()),
                getMatrix(xrHitInfo.getTransform()));
    }

    /**
     * Converts an XR InputEvent to a JXRCore InputEvent.
     *
     * @param xrInputEvent an {@link com.android.extensions.xr.node.InputEvent} instance to be
     *     converted.
     * @param entityManager an {@link EntityManager} instance to look up entities.
     * @return a {@link androidx.xr.scenecore.JXRCoreRuntime.InputEvent} instance representing the
     *     input event.
     */
    static InputEvent getInputEvent(
            @NonNull com.android.extensions.xr.node.InputEvent xrInputEvent,
            @NonNull EntityManager entityManager) {
        Vector3 origin = getVector3(xrInputEvent.getOrigin());
        Vector3 direction = getVector3(xrInputEvent.getDirection());
        HitInfo hitInfo = null;
        HitInfo secondaryHitInfo = null;
        if (xrInputEvent.getHitInfo() != null) {
            hitInfo = getHitInfo(xrInputEvent.getHitInfo(), entityManager);
        }
        if (xrInputEvent.getSecondaryHitInfo() != null) {
            secondaryHitInfo = getHitInfo(xrInputEvent.getSecondaryHitInfo(), entityManager);
        }
        return new InputEvent(
                getInputEventSource(xrInputEvent.getSource()),
                getInputEventPointerType(xrInputEvent.getPointerType()),
                xrInputEvent.getTimestamp(),
                origin,
                direction,
                getInputEventAction(xrInputEvent.getAction()),
                hitInfo,
                secondaryHitInfo);
    }

    @InputEvent.Source
    static int getInputEventSource(int xrInputEventSource) {
        switch (xrInputEventSource) {
            case com.android.extensions.xr.node.InputEvent.SOURCE_UNKNOWN:
                return InputEvent.SOURCE_UNKNOWN;
            case com.android.extensions.xr.node.InputEvent.SOURCE_HEAD:
                return InputEvent.SOURCE_HEAD;
            case com.android.extensions.xr.node.InputEvent.SOURCE_CONTROLLER:
                return InputEvent.SOURCE_CONTROLLER;
            case com.android.extensions.xr.node.InputEvent.SOURCE_HANDS:
                return InputEvent.SOURCE_HANDS;
            case com.android.extensions.xr.node.InputEvent.SOURCE_MOUSE:
                return InputEvent.SOURCE_MOUSE;
            case com.android.extensions.xr.node.InputEvent.SOURCE_GAZE_AND_GESTURE:
                return InputEvent.SOURCE_GAZE_AND_GESTURE;
            default:
                throw new IllegalArgumentException(
                        "Unknown Input Event Source: " + xrInputEventSource);
        }
    }

    @InputEvent.PointerType
    static int getInputEventPointerType(int xrInputEventPointerType) {
        switch (xrInputEventPointerType) {
            case com.android.extensions.xr.node.InputEvent.POINTER_TYPE_DEFAULT:
                return InputEvent.POINTER_TYPE_DEFAULT;
            case com.android.extensions.xr.node.InputEvent.POINTER_TYPE_LEFT:
                return InputEvent.POINTER_TYPE_LEFT;
            case com.android.extensions.xr.node.InputEvent.POINTER_TYPE_RIGHT:
                return InputEvent.POINTER_TYPE_RIGHT;
            default:
                throw new IllegalArgumentException(
                        "Unknown Input Event Pointer Type: " + xrInputEventPointerType);
        }
    }

    @InputEvent.Action
    static int getInputEventAction(int xrInputEventAction) {
        switch (xrInputEventAction) {
            case com.android.extensions.xr.node.InputEvent.ACTION_DOWN:
                return InputEvent.ACTION_DOWN;
            case com.android.extensions.xr.node.InputEvent.ACTION_UP:
                return InputEvent.ACTION_UP;
            case com.android.extensions.xr.node.InputEvent.ACTION_MOVE:
                return InputEvent.ACTION_MOVE;
            case com.android.extensions.xr.node.InputEvent.ACTION_CANCEL:
                return InputEvent.ACTION_CANCEL;
            case com.android.extensions.xr.node.InputEvent.ACTION_HOVER_MOVE:
                return InputEvent.ACTION_HOVER_MOVE;
            case com.android.extensions.xr.node.InputEvent.ACTION_HOVER_ENTER:
                return InputEvent.ACTION_HOVER_ENTER;
            case com.android.extensions.xr.node.InputEvent.ACTION_HOVER_EXIT:
                return InputEvent.ACTION_HOVER_EXIT;
            default:
                throw new IllegalArgumentException(
                        "Unknown Input Event Action: " + xrInputEventAction);
        }
    }

    @ResizeEvent.ResizeState
    static int getResizeEventState(int resizeState) {
        switch (resizeState) {
            case ReformEvent.REFORM_STATE_UNKNOWN:
                return ResizeEvent.RESIZE_STATE_UNKNOWN;
            case ReformEvent.REFORM_STATE_START:
                return ResizeEvent.RESIZE_STATE_START;
            case ReformEvent.REFORM_STATE_ONGOING:
                return ResizeEvent.RESIZE_STATE_ONGOING;
            case ReformEvent.REFORM_STATE_END:
                return ResizeEvent.RESIZE_STATE_END;
            default:
                throw new IllegalArgumentException("Unknown Resize State: " + resizeState);
        }
    }

    static Matrix4 getMatrix(Mat4f xrMatrix) {
        float[] matrixData = xrMatrix.getFlattenedMatrix();
        return new Matrix4(matrixData);
    }

    static Pose getPose(Vec3 position, Quatf quatf) {
        return new Pose(
                new Vector3(position.x, position.y, position.z),
                new Quaternion(quatf.x, quatf.y, quatf.z, quatf.w));
    }

    static Vector3 getVector3(Vec3 vec3) {
        return new Vector3(vec3.x, vec3.y, vec3.z);
    }

    static Quaternion getQuaternion(Quatf quatf) {
        return new Quaternion(quatf.x, quatf.y, quatf.z, quatf.w);
    }

    /**
     * Converts from a perception pose type.
     *
     * @param perceptionPose a {@code androidx.xr.scenecore.impl.perception.Pose} instance
     *     representing the pose.
     */
    static Pose fromPerceptionPose(androidx.xr.scenecore.impl.perception.Pose perceptionPose) {
        Vector3 translation =
                new Vector3(perceptionPose.tx(), perceptionPose.ty(), perceptionPose.tz());
        Quaternion rotation =
                new Quaternion(
                        perceptionPose.qx(),
                        perceptionPose.qy(),
                        perceptionPose.qz(),
                        perceptionPose.qw());
        return new Pose(translation, rotation);
    }

    /**
     * Converts from a pose to a perception pose type.
     *
     * @param pose a {@code androidx.xr.runtime.math.Pose} instance representing the pose.
     */
    static androidx.xr.scenecore.impl.perception.Pose poseToPerceptionPose(Pose pose) {
        return new androidx.xr.scenecore.impl.perception.Pose(
                pose.getTranslation().getX(),
                pose.getTranslation().getY(),
                pose.getTranslation().getZ(),
                pose.getRotation().getX(),
                pose.getRotation().getY(),
                pose.getRotation().getZ(),
                pose.getRotation().getW());
    }

    /**
     * Converts to a JXRCore FOV from a perception FOV type.
     *
     * @param perceptionFov a {@code androidx.xr.scenecore.impl.perception.Fov} instance
     *     representing the FOV.
     */
    static Fov fovFromPerceptionFov(androidx.xr.scenecore.impl.perception.Fov perceptionFov) {
        return new Fov(
                perceptionFov.getAngleLeft(),
                perceptionFov.getAngleRight(),
                perceptionFov.getAngleUp(),
                perceptionFov.getAngleDown());
    }

    /**
     * Converts to a perception FOV from a JXRCore FOV type.
     *
     * @param fov a {@code androidx.xr.runtime.internal.CameraViewActivityPose.Fov} instance
     *     representing the FOV.
     */
    static androidx.xr.scenecore.impl.perception.Fov perceptionFovFromFov(Fov fov) {
        return new androidx.xr.scenecore.impl.perception.Fov(
                fov.getAngleLeft(), fov.getAngleRight(), fov.getAngleUp(), fov.getAngleDown());
    }

    /**
     * Converts from the Extensions spatial capabilities to the runtime spatial capabilities.
     *
     * @param extCapabilities a {@link com.android.extensions.xr.space.SpatialCapabilities} instance
     *     to be converted.
     */
    static SpatialCapabilities convertSpatialCapabilities(
            com.android.extensions.xr.space.SpatialCapabilities extCapabilities) {
        int capabilities = 0;
        if (extCapabilities.get(
                com.android.extensions.xr.space.SpatialCapabilities.SPATIAL_UI_CAPABLE)) {
            capabilities |= SpatialCapabilities.SPATIAL_CAPABILITY_UI;
        }
        if (extCapabilities.get(
                com.android.extensions.xr.space.SpatialCapabilities.SPATIAL_3D_CONTENTS_CAPABLE)) {
            capabilities |= SpatialCapabilities.SPATIAL_CAPABILITY_3D_CONTENT;
        }
        if (extCapabilities.get(
                com.android.extensions.xr.space.SpatialCapabilities.PASSTHROUGH_CONTROL_CAPABLE)) {
            capabilities |= SpatialCapabilities.SPATIAL_CAPABILITY_PASSTHROUGH_CONTROL;
        }
        if (extCapabilities.get(
                com.android.extensions.xr.space.SpatialCapabilities.APP_ENVIRONMENTS_CAPABLE)) {
            capabilities |= SpatialCapabilities.SPATIAL_CAPABILITY_APP_ENVIRONMENT;
        }
        if (extCapabilities.get(
                com.android.extensions.xr.space.SpatialCapabilities.SPATIAL_AUDIO_CAPABLE)) {
            capabilities |= SpatialCapabilities.SPATIAL_CAPABILITY_SPATIAL_AUDIO;
        }
        if (extCapabilities.get(
                com.android.extensions.xr.space.SpatialCapabilities
                        .SPATIAL_ACTIVITY_EMBEDDING_CAPABLE)) {
            capabilities |= SpatialCapabilities.SPATIAL_CAPABILITY_EMBED_ACTIVITY;
        }

        return new SpatialCapabilities(capabilities);
    }

    /**
     * Converts from the Extensions perceived resolution to the runtime perceived resolution.
     *
     * @param extResolution a {@link com.android.extensions.xr.space.PerceivedResolution} instance
     *     to be converted.
     */
    static PixelDimensions convertPerceivedResolution(
            com.android.extensions.xr.space.PerceivedResolution extResolution) {
        return new PixelDimensions(extResolution.getWidth(), extResolution.getHeight());
    }

    /**
     * Converts from the Extensions spatial visibility to the runtime spatial visibility.
     *
     * @param extVisibility a {@link com.android.extensions.xr.space.VisibilityState.S} instance to
     *     be converted.
     */
    static SpatialVisibility convertSpatialVisibility(int extVisibility) {
        int visibility;
        switch (extVisibility) {
            case VisibilityState.UNKNOWN:
                visibility = SpatialVisibility.UNKNOWN;
                break;
            case VisibilityState.NOT_VISIBLE:
                visibility = SpatialVisibility.OUTSIDE_FOV;
                break;
            case VisibilityState.PARTIALLY_VISIBLE:
                visibility = SpatialVisibility.PARTIALLY_WITHIN_FOV;
                break;
            case VisibilityState.FULLY_VISIBLE:
                visibility = SpatialVisibility.WITHIN_FOV;
                break;
            default:
                throw new IllegalArgumentException("Unknown Spatial Visibility: " + extVisibility);
        }
        return new SpatialVisibility(visibility);
    }

    /**
     * Converts from the Extensions environment visibility state to the runtime environment
     * visibility state.
     *
     * @param environmentState a {@link
     *     com.android.extensions.xr.environment.EnvironmentVisibilityState} instance to be
     *     converted.
     */
    static boolean getIsSpatialEnvironmentPreferenceActive(int environmentState) {
        return environmentState == EnvironmentVisibilityState.APP_VISIBLE;
    }

    static float getPassthroughOpacity(PassthroughVisibilityState passthroughVisibilityState) {
        int passthroughState = passthroughVisibilityState.getCurrentState();
        if (passthroughState == PassthroughVisibilityState.DISABLED) {
            return 0.0f;
        } else {
            float opacity = passthroughVisibilityState.getOpacity();
            if (opacity > 0.0f) {
                return opacity;
            } else {
                // When passthrough is enabled, the opacity should be greater than zero.
                Log.e(
                        "RuntimeUtils",
                        "Passthrough is enabled, but active opacity value is "
                                + opacity
                                + ". Opacity should be greater than zero when Passthrough is"
                                + " enabled.");
                return 1.0f;
            }
        }
    }

    /**
     * Converts from JXR Core's TextureSampler to Impress' API bindings TextureSampler.
     *
     * @param sampler a {@link androidx.xr.scenecore.TextureSampler} instance to be converted.
     */
    static com.google.ar.imp.apibindings.TextureSampler getTextureSampler(
            @NonNull TextureSampler sampler) {
        return new com.google.ar.imp.apibindings.TextureSampler.Builder()
                .setMinFilter(getMinFilter(sampler.getMinFilter()))
                .setMagFilter(getMagFilter(sampler.getMagFilter()))
                .setWrapModeS(getWrapMode(sampler.getWrapModeS()))
                .setWrapModeT(getWrapMode(sampler.getWrapModeT()))
                .setWrapModeR(getWrapMode(sampler.getWrapModeR()))
                .setCompareMode(getCompareModeValue(sampler.getCompareMode()))
                .setCompareFunc(getCompareFuncValue(sampler.getCompareFunc()))
                .setAnisotropyLog2(sampler.getAnisotropyLog2())
                .build();
    }

    private static com.google.ar.imp.apibindings.TextureSampler.WrapMode getWrapMode(
            @TextureSampler.WrapMode int wrapMode) {
        switch (wrapMode) {
            case TextureSampler.CLAMP_TO_EDGE:
                return com.google.ar.imp.apibindings.TextureSampler.WrapMode.CLAMP_TO_EDGE;
            case TextureSampler.REPEAT:
                return com.google.ar.imp.apibindings.TextureSampler.WrapMode.REPEAT;
            case TextureSampler.MIRRORED_REPEAT:
                return com.google.ar.imp.apibindings.TextureSampler.WrapMode.MIRRORED_REPEAT;
            default:
                throw new IllegalArgumentException("Unknown WrapMode value: " + wrapMode);
        }
    }

    private static com.google.ar.imp.apibindings.TextureSampler.MinFilter getMinFilter(
            @TextureSampler.MinFilter int minFilter) {
        switch (minFilter) {
            case TextureSampler.NEAREST:
                return com.google.ar.imp.apibindings.TextureSampler.MinFilter.NEAREST;
            case TextureSampler.LINEAR:
                return com.google.ar.imp.apibindings.TextureSampler.MinFilter.LINEAR;
            case TextureSampler.NEAREST_MIPMAP_NEAREST:
                return com.google.ar.imp.apibindings.TextureSampler.MinFilter
                        .NEAREST_MIPMAP_NEAREST;
            case TextureSampler.LINEAR_MIPMAP_NEAREST:
                return com.google.ar.imp.apibindings.TextureSampler.MinFilter.LINEAR_MIPMAP_NEAREST;
            case TextureSampler.NEAREST_MIPMAP_LINEAR:
                return com.google.ar.imp.apibindings.TextureSampler.MinFilter.NEAREST_MIPMAP_LINEAR;
            case TextureSampler.LINEAR_MIPMAP_LINEAR:
                return com.google.ar.imp.apibindings.TextureSampler.MinFilter.LINEAR_MIPMAP_LINEAR;
            default:
                throw new IllegalArgumentException("Unknown MinFilter value: " + minFilter);
        }
    }

    private static com.google.ar.imp.apibindings.TextureSampler.MagFilter getMagFilter(
            @TextureSampler.MagFilter int magFilter) {
        switch (magFilter) {
            case TextureSampler.MAG_NEAREST:
                return com.google.ar.imp.apibindings.TextureSampler.MagFilter.NEAREST;
            case TextureSampler.MAG_LINEAR:
                return com.google.ar.imp.apibindings.TextureSampler.MagFilter.LINEAR;
            default:
                throw new IllegalArgumentException("Unknown MagFilter value: " + magFilter);
        }
    }

    private static com.google.ar.imp.apibindings.TextureSampler.CompareMode getCompareModeValue(
            @TextureSampler.CompareMode int compareMode) {
        switch (compareMode) {
            case TextureSampler.NONE:
                return com.google.ar.imp.apibindings.TextureSampler.CompareMode.NONE;
            case TextureSampler.COMPARE_TO_TEXTURE:
                return com.google.ar.imp.apibindings.TextureSampler.CompareMode.COMPARE_TO_TEXTURE;
            default:
                throw new IllegalArgumentException("Unknown CompareMode value: " + compareMode);
        }
    }

    private static com.google.ar.imp.apibindings.TextureSampler.CompareFunc getCompareFuncValue(
            @TextureSampler.CompareFunc int compareFunc) {
        switch (compareFunc) {
            case TextureSampler.LE:
                return com.google.ar.imp.apibindings.TextureSampler.CompareFunc.LE;
            case TextureSampler.GE:
                return com.google.ar.imp.apibindings.TextureSampler.CompareFunc.GE;
            case TextureSampler.L:
                return com.google.ar.imp.apibindings.TextureSampler.CompareFunc.L;
            case TextureSampler.G:
                return com.google.ar.imp.apibindings.TextureSampler.CompareFunc.G;
            case TextureSampler.E:
                return com.google.ar.imp.apibindings.TextureSampler.CompareFunc.E;
            case TextureSampler.NE:
                return com.google.ar.imp.apibindings.TextureSampler.CompareFunc.NE;
            case TextureSampler.A:
                return com.google.ar.imp.apibindings.TextureSampler.CompareFunc.A;
            case TextureSampler.N:
                return com.google.ar.imp.apibindings.TextureSampler.CompareFunc.N;
            default:
                throw new IllegalArgumentException("Unknown CompareFunc value: " + compareFunc);
        }
    }

    /**
     * Converts from JXR Core's KhronosPbrMaterialSpec to Impress' API bindings
     * KhronosPbrMaterialSpec.
     *
     * @param spec a {@link com.google.vr.androidx.xr.core.KhronosPbrMaterialSpec} instance to be
     *     converted.
     */
    static com.google.ar.imp.apibindings.KhronosPbrMaterialSpec getKhronosPbrMaterialSpec(
            @NonNull KhronosPbrMaterialSpec spec) {
        return new com.google.ar.imp.apibindings.KhronosPbrMaterialSpec.Builder()
                .setLightingModel(getLightingModel(spec.getLightingModel()))
                .setBlendMode(getBlendMode(spec.getBlendMode()))
                .setDoubleSidedMode(getDoubleSidedMode(spec.getDoubleSidedMode()))
                .build();
    }

    private static com.google.ar.imp.apibindings.KhronosPbrMaterialSpec.LightingModel
            getLightingModel(@KhronosPbrMaterialSpec.LightingModel int lightingModel) {
        switch (lightingModel) {
            case KhronosPbrMaterialSpec.LIT:
                return com.google.ar.imp.apibindings.KhronosPbrMaterialSpec.LightingModel.LIT;
            case KhronosPbrMaterialSpec.UNLIT:
                return com.google.ar.imp.apibindings.KhronosPbrMaterialSpec.LightingModel.UNLIT;
            default:
                throw new IllegalArgumentException("Unknown LightingModel value: " + lightingModel);
        }
    }

    private static com.google.ar.imp.apibindings.KhronosPbrMaterialSpec.BlendMode getBlendMode(
            @KhronosPbrMaterialSpec.BlendMode int blendMode) {
        switch (blendMode) {
            case KhronosPbrMaterialSpec.OPAQUE:
                return com.google.ar.imp.apibindings.KhronosPbrMaterialSpec.BlendMode.OPAQUE;
            case KhronosPbrMaterialSpec.MASKED:
                return com.google.ar.imp.apibindings.KhronosPbrMaterialSpec.BlendMode.MASKED;
            case KhronosPbrMaterialSpec.TRANSPARENT:
                return com.google.ar.imp.apibindings.KhronosPbrMaterialSpec.BlendMode.TRANSPARENT;
            case KhronosPbrMaterialSpec.REFRACTIVE:
                return com.google.ar.imp.apibindings.KhronosPbrMaterialSpec.BlendMode.REFRACTIVE;
            default:
                throw new IllegalArgumentException("Unknown BlendMode value: " + blendMode);
        }
    }

    private static com.google.ar.imp.apibindings.KhronosPbrMaterialSpec.DoubleSidedMode
            getDoubleSidedMode(@KhronosPbrMaterialSpec.DoubleSidedMode int doubleSidedMode) {
        switch (doubleSidedMode) {
            case KhronosPbrMaterialSpec.SINGLE_SIDED:
                return com.google.ar.imp.apibindings.KhronosPbrMaterialSpec.DoubleSidedMode
                        .SINGLE_SIDED;
            case KhronosPbrMaterialSpec.DOUBLE_SIDED:
                return com.google.ar.imp.apibindings.KhronosPbrMaterialSpec.DoubleSidedMode
                        .DOUBLE_SIDED;
            default:
                throw new IllegalArgumentException(
                        "Unknown DoubleSidedMode value: " + doubleSidedMode);
        }
    }

    private static int getHitTestSurfaceType(int extSurfaceType) {
        switch (extSurfaceType) {
            case com.android.extensions.xr.space.HitTestResult.SURFACE_PANEL:
                return HitTestResult.HitTestSurfaceType.HIT_TEST_RESULT_SURFACE_TYPE_PLANE;
            case com.android.extensions.xr.space.HitTestResult.SURFACE_3D_OBJECT:
                return HitTestResult.HitTestSurfaceType.HIT_TEST_RESULT_SURFACE_TYPE_OBJECT;
            default:
                return HitTestResult.HitTestSurfaceType.HIT_TEST_RESULT_SURFACE_TYPE_UNKNOWN;
        }
    }

    /**
     * Converts from the Extensions hit test result to the platform adapter hit test result.
     *
     * @param hitTestResultExt a {@link com.android.extensions.xr.space.HitTestResult} instance to
     *     be converted.
     */
    static HitTestResult getHitTestResult(
            com.android.extensions.xr.space.HitTestResult hitTestResultExt) {
        Vector3 hitPosition =
                hitTestResultExt.getHitPosition() == null
                        ? null
                        : new Vector3(
                                hitTestResultExt.getHitPosition().x,
                                hitTestResultExt.getHitPosition().y,
                                hitTestResultExt.getHitPosition().z);
        Vector3 surfaceNormal =
                hitTestResultExt.getSurfaceNormal() == null
                        ? null
                        : new Vector3(
                                hitTestResultExt.getSurfaceNormal().x,
                                hitTestResultExt.getSurfaceNormal().y,
                                hitTestResultExt.getSurfaceNormal().z);
        int surfaceType = getHitTestSurfaceType(hitTestResultExt.getSurfaceType());
        return new HitTestResult(
                hitPosition, surfaceNormal, surfaceType, hitTestResultExt.getDistance());
    }

    static int getHitTestFilter(@HitTestFilterValue int hitTestFilter) {
        int hitTestFilterResult = 0;
        if ((hitTestFilter & HitTestFilter.SELF_SCENE) != 0) {
            hitTestFilterResult |= XrExtensions.HIT_TEST_FILTER_INCLUDE_INSIDE_ACTIVITY;
        }
        if ((hitTestFilter & HitTestFilter.OTHER_SCENES) != 0) {
            hitTestFilterResult |= XrExtensions.HIT_TEST_FILTER_INCLUDE_OUTSIDE_ACTIVITY;
        }
        return hitTestFilterResult;
    }

    static int convertSpatialPointerIconType(@SpatialPointerIconType int rtIconType) {
        switch (rtIconType) {
            case SpatialPointerIcon.TYPE_NONE:
                return NodeTransaction.POINTER_ICON_TYPE_NONE;
            case SpatialPointerIcon.TYPE_DEFAULT:
                return NodeTransaction.POINTER_ICON_TYPE_DEFAULT;
            case SpatialPointerIcon.TYPE_CIRCLE:
                return NodeTransaction.POINTER_ICON_TYPE_CIRCLE;
            default:
                Log.e("RuntimeUtils", "Unknown Spatial Pointer Icon Type: " + rtIconType);
                return NodeTransaction.POINTER_ICON_TYPE_DEFAULT;
        }
    }
}
