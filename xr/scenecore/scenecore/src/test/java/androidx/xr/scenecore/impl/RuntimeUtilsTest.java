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

import static androidx.xr.runtime.testing.math.MathAssertions.assertPose;
import static androidx.xr.runtime.testing.math.MathAssertions.assertVector3;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;

import android.util.Log;

import androidx.xr.runtime.internal.HitTestResult;
import androidx.xr.runtime.internal.InputEvent;
import androidx.xr.runtime.internal.PlaneSemantic;
import androidx.xr.runtime.internal.PlaneType;
import androidx.xr.runtime.internal.ResizeEvent;
import androidx.xr.runtime.internal.SpatialCapabilities;
import androidx.xr.runtime.internal.SpatialVisibility;
import androidx.xr.runtime.math.Matrix4;
import androidx.xr.runtime.math.Pose;
import androidx.xr.runtime.math.Quaternion;
import androidx.xr.runtime.math.Vector3;
import androidx.xr.scenecore.impl.perception.Plane;

import com.android.extensions.xr.VisibilityChangedEvent;
import com.android.extensions.xr.environment.EnvironmentVisibilityState;
import com.android.extensions.xr.environment.PassthroughVisibilityState;
import com.android.extensions.xr.environment.ShadowPassthroughVisibilityState;
import com.android.extensions.xr.node.Mat4f;
import com.android.extensions.xr.node.Vec3;
import com.android.extensions.xr.space.ShadowSpatialCapabilities;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.junit.rules.ExpectedLogMessagesRule;

import java.util.regex.Pattern;

@RunWith(RobolectricTestRunner.class)
public final class RuntimeUtilsTest {
    // TODO(b/402408284): Remove once the constants are available in the host version of ReformEvent
    public static final int REFORM_STATE_UNKNOWN = 0;
    public static final int REFORM_STATE_START = 1;
    public static final int REFORM_STATE_ONGOING = 2;
    public static final int REFORM_STATE_END = 3;

    @Rule
    public final ExpectedLogMessagesRule expectedLogMessagesRule = new ExpectedLogMessagesRule();

    @Test
    public void getPlaneTypeHorizontal_returnsHorizontal() {
        assertThat(RuntimeUtils.getPlaneType(PlaneType.HORIZONTAL))
                .isEqualTo(Plane.Type.HORIZONTAL_UPWARD_FACING);
    }

    @Test
    public void getPlaneTypeVertical_returnsVertical() {
        assertThat(RuntimeUtils.getPlaneType(PlaneType.VERTICAL)).isEqualTo(Plane.Type.VERTICAL);
    }

    @Test
    public void getPlaneTypeAny_returnsArbitrary() {
        assertThat(RuntimeUtils.getPlaneType(PlaneType.ANY)).isEqualTo(Plane.Type.ARBITRARY);
    }

    @Test
    public void getPlaneTypeHorizontalUpwardFacingFromPerception_returnsHorizontal() {
        assertThat(RuntimeUtils.getPlaneType(Plane.Type.HORIZONTAL_UPWARD_FACING))
                .isEqualTo(PlaneType.HORIZONTAL);
    }

    @Test
    public void getPlaneTypeHorizontalDownwardFacingFromPerception_returnsHorizontal() {
        assertThat(RuntimeUtils.getPlaneType(Plane.Type.HORIZONTAL_DOWNWARD_FACING))
                .isEqualTo(PlaneType.HORIZONTAL);
    }

    @Test
    public void getPlaneTypeVerticalFromPerception_returnsVertical() {
        assertThat(RuntimeUtils.getPlaneType(Plane.Type.VERTICAL)).isEqualTo(PlaneType.VERTICAL);
    }

    @Test
    public void getPlaneTypeArbitraryFromPerception_returnsAny() {
        assertThat(RuntimeUtils.getPlaneType(PlaneType.ANY)).isEqualTo(Plane.Type.ARBITRARY);
    }

    @Test
    public void getPlaneLabelWall_returnsWall() {
        assertThat(RuntimeUtils.getPlaneLabel(PlaneSemantic.WALL)).isEqualTo(Plane.Label.WALL);
    }

    @Test
    public void getPlaneLabelFloor_returnsFloor() {
        assertThat(RuntimeUtils.getPlaneLabel(PlaneSemantic.FLOOR)).isEqualTo(Plane.Label.FLOOR);
    }

    @Test
    public void getPlaneLabelCeiling_returnsCeiling() {
        assertThat(RuntimeUtils.getPlaneLabel(PlaneSemantic.CEILING))
                .isEqualTo(Plane.Label.CEILING);
    }

    @Test
    public void getPlaneLabelTable_returnsTable() {
        assertThat(RuntimeUtils.getPlaneLabel(PlaneSemantic.TABLE)).isEqualTo(Plane.Label.TABLE);
    }

    @Test
    public void getPlaneLabelAny_returnsUnknown() {
        assertThat(RuntimeUtils.getPlaneLabel(PlaneSemantic.ANY)).isEqualTo(Plane.Label.UNKNOWN);
    }

    @Test
    public void getPlaneSemanticlWall_returnsWall() {
        assertThat(RuntimeUtils.getPlaneSemantic(Plane.Label.WALL)).isEqualTo(PlaneSemantic.WALL);
    }

    @Test
    public void getPlaneSemanticFloor_returnsFloor() {
        assertThat(RuntimeUtils.getPlaneSemantic(Plane.Label.FLOOR)).isEqualTo(PlaneSemantic.FLOOR);
    }

    @Test
    public void getPlaneSemanticCeiling_returnsCeiling() {
        assertThat(RuntimeUtils.getPlaneSemantic(Plane.Label.CEILING))
                .isEqualTo(PlaneSemantic.CEILING);
    }

    @Test
    public void getPlaneSemanticTable_returnsTable() {
        assertThat(RuntimeUtils.getPlaneSemantic(Plane.Label.TABLE)).isEqualTo(PlaneSemantic.TABLE);
    }

    @Test
    public void getPlaneSemanticUnknown_returnsAny() {
        assertThat(RuntimeUtils.getPlaneSemantic(Plane.Label.UNKNOWN)).isEqualTo(PlaneSemantic.ANY);
    }

    @Test
    public void getMatrix_returnsMatrix() {
        float[] expected = new float[] {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16};
        Mat4f matrix = new Mat4f(expected);

        assertThat(RuntimeUtils.getMatrix(matrix).getData())
                .usingExactEquality()
                .containsExactly(new Matrix4(expected).getData())
                .inOrder();
    }

    @Test
    public void fromPerceptionPose_returnsPose() {
        Pose expectedPose = new Pose(new Vector3(1, 2, 3), new Quaternion(0, 0, 0, 1));
        androidx.xr.scenecore.impl.perception.Pose perceptionPose =
                new androidx.xr.scenecore.impl.perception.Pose(1, 2, 3, 0, 0, 0, 1);

        assertPose(RuntimeUtils.fromPerceptionPose(perceptionPose), expectedPose);
    }

    @Test
    public void poseToPerceptionPose_returnsPerceptionPose() {
        androidx.xr.scenecore.impl.perception.Pose expectedPerceptionPose =
                new androidx.xr.scenecore.impl.perception.Pose(1, 2, 3, 0, 0, 0, 1);
        Pose pose = new Pose(new Vector3(1, 2, 3), new Quaternion(0, 0, 0, 1));

        assertThat(RuntimeUtils.poseToPerceptionPose(pose)).isEqualTo(expectedPerceptionPose);
    }

    @Test
    public void convertSpatialCapabilities_noCapabilities() {
        com.android.extensions.xr.space.SpatialCapabilities extensionCapabilities =
                ShadowSpatialCapabilities.create();
        SpatialCapabilities caps = RuntimeUtils.convertSpatialCapabilities(extensionCapabilities);
        assertThat(caps.hasCapability(SpatialCapabilities.SPATIAL_CAPABILITY_UI)).isFalse();
        assertThat(caps.hasCapability(SpatialCapabilities.SPATIAL_CAPABILITY_3D_CONTENT)).isFalse();
        assertThat(caps.hasCapability(SpatialCapabilities.SPATIAL_CAPABILITY_PASSTHROUGH_CONTROL))
                .isFalse();
        assertThat(caps.hasCapability(SpatialCapabilities.SPATIAL_CAPABILITY_APP_ENVIRONMENT))
                .isFalse();
        assertThat(caps.hasCapability(SpatialCapabilities.SPATIAL_CAPABILITY_SPATIAL_AUDIO))
                .isFalse();
        assertThat(caps.hasCapability(SpatialCapabilities.SPATIAL_CAPABILITY_EMBED_ACTIVITY))
                .isFalse();
    }

    @Test
    public void convertSpatialCapabilities_allCapabilities() {
        com.android.extensions.xr.space.SpatialCapabilities extensionCapabilities =
                ShadowSpatialCapabilities.createAll();
        SpatialCapabilities caps = RuntimeUtils.convertSpatialCapabilities(extensionCapabilities);
        assertThat(caps.hasCapability(SpatialCapabilities.SPATIAL_CAPABILITY_UI)).isTrue();
        assertThat(caps.hasCapability(SpatialCapabilities.SPATIAL_CAPABILITY_3D_CONTENT)).isTrue();
        assertThat(caps.hasCapability(SpatialCapabilities.SPATIAL_CAPABILITY_PASSTHROUGH_CONTROL))
                .isTrue();
        assertThat(caps.hasCapability(SpatialCapabilities.SPATIAL_CAPABILITY_APP_ENVIRONMENT))
                .isTrue();
        assertThat(caps.hasCapability(SpatialCapabilities.SPATIAL_CAPABILITY_SPATIAL_AUDIO))
                .isTrue();
        assertThat(caps.hasCapability(SpatialCapabilities.SPATIAL_CAPABILITY_EMBED_ACTIVITY))
                .isTrue();
    }

    @Test
    public void convertSpatialCapabilities_singleCapability() {
        // check conversions of a few different instances of the extensions SpatialCapabilities that
        // each have exactly one capability.
        com.android.extensions.xr.space.SpatialCapabilities extensionCapabilities =
                ShadowSpatialCapabilities.create(
                        com.android.extensions.xr.space.SpatialCapabilities.SPATIAL_UI_CAPABLE);
        SpatialCapabilities caps = RuntimeUtils.convertSpatialCapabilities(extensionCapabilities);
        assertThat(caps.hasCapability(SpatialCapabilities.SPATIAL_CAPABILITY_UI)).isTrue();
        assertThat(caps.hasCapability(SpatialCapabilities.SPATIAL_CAPABILITY_3D_CONTENT)).isFalse();
        assertThat(caps.hasCapability(SpatialCapabilities.SPATIAL_CAPABILITY_PASSTHROUGH_CONTROL))
                .isFalse();
        assertThat(caps.hasCapability(SpatialCapabilities.SPATIAL_CAPABILITY_APP_ENVIRONMENT))
                .isFalse();
        assertThat(caps.hasCapability(SpatialCapabilities.SPATIAL_CAPABILITY_SPATIAL_AUDIO))
                .isFalse();
        assertThat(caps.hasCapability(SpatialCapabilities.SPATIAL_CAPABILITY_EMBED_ACTIVITY))
                .isFalse();

        extensionCapabilities =
                ShadowSpatialCapabilities.create(
                        com.android.extensions.xr.space.SpatialCapabilities
                                .SPATIAL_3D_CONTENTS_CAPABLE);
        caps = RuntimeUtils.convertSpatialCapabilities(extensionCapabilities);
        assertThat(caps.hasCapability(SpatialCapabilities.SPATIAL_CAPABILITY_UI)).isFalse();
        assertThat(caps.hasCapability(SpatialCapabilities.SPATIAL_CAPABILITY_3D_CONTENT)).isTrue();
        assertThat(caps.hasCapability(SpatialCapabilities.SPATIAL_CAPABILITY_PASSTHROUGH_CONTROL))
                .isFalse();
        assertThat(caps.hasCapability(SpatialCapabilities.SPATIAL_CAPABILITY_APP_ENVIRONMENT))
                .isFalse();
        assertThat(caps.hasCapability(SpatialCapabilities.SPATIAL_CAPABILITY_SPATIAL_AUDIO))
                .isFalse();
        assertThat(caps.hasCapability(SpatialCapabilities.SPATIAL_CAPABILITY_EMBED_ACTIVITY))
                .isFalse();

        extensionCapabilities =
                ShadowSpatialCapabilities.create(
                        com.android.extensions.xr.space.SpatialCapabilities.SPATIAL_AUDIO_CAPABLE);
        caps = RuntimeUtils.convertSpatialCapabilities(extensionCapabilities);
        assertThat(caps.hasCapability(SpatialCapabilities.SPATIAL_CAPABILITY_UI)).isFalse();
        assertThat(caps.hasCapability(SpatialCapabilities.SPATIAL_CAPABILITY_3D_CONTENT)).isFalse();
        assertThat(caps.hasCapability(SpatialCapabilities.SPATIAL_CAPABILITY_PASSTHROUGH_CONTROL))
                .isFalse();
        assertThat(caps.hasCapability(SpatialCapabilities.SPATIAL_CAPABILITY_APP_ENVIRONMENT))
                .isFalse();
        assertThat(caps.hasCapability(SpatialCapabilities.SPATIAL_CAPABILITY_SPATIAL_AUDIO))
                .isTrue();
        assertThat(caps.hasCapability(SpatialCapabilities.SPATIAL_CAPABILITY_EMBED_ACTIVITY))
                .isFalse();
    }

    @Test
    public void convertSpatialCapabilities_mixedCapabilities() {
        // Check conversions for a couple of different combinations of capabilities.
        com.android.extensions.xr.space.SpatialCapabilities extensionCapabilities =
                ShadowSpatialCapabilities.create(
                        com.android.extensions.xr.space.SpatialCapabilities.SPATIAL_AUDIO_CAPABLE,
                        com.android.extensions.xr.space.SpatialCapabilities
                                .SPATIAL_3D_CONTENTS_CAPABLE);
        SpatialCapabilities caps = RuntimeUtils.convertSpatialCapabilities(extensionCapabilities);
        assertThat(caps.hasCapability(SpatialCapabilities.SPATIAL_CAPABILITY_UI)).isFalse();
        assertThat(caps.hasCapability(SpatialCapabilities.SPATIAL_CAPABILITY_3D_CONTENT)).isTrue();
        assertThat(caps.hasCapability(SpatialCapabilities.SPATIAL_CAPABILITY_PASSTHROUGH_CONTROL))
                .isFalse();
        assertThat(caps.hasCapability(SpatialCapabilities.SPATIAL_CAPABILITY_APP_ENVIRONMENT))
                .isFalse();
        assertThat(caps.hasCapability(SpatialCapabilities.SPATIAL_CAPABILITY_SPATIAL_AUDIO))
                .isTrue();
        assertThat(caps.hasCapability(SpatialCapabilities.SPATIAL_CAPABILITY_EMBED_ACTIVITY))
                .isFalse();

        extensionCapabilities =
                extensionCapabilities =
                        ShadowSpatialCapabilities.create(
                                com.android.extensions.xr.space.SpatialCapabilities
                                        .SPATIAL_UI_CAPABLE,
                                com.android.extensions.xr.space.SpatialCapabilities
                                        .PASSTHROUGH_CONTROL_CAPABLE,
                                com.android.extensions.xr.space.SpatialCapabilities
                                        .APP_ENVIRONMENTS_CAPABLE,
                                com.android.extensions.xr.space.SpatialCapabilities
                                        .SPATIAL_ACTIVITY_EMBEDDING_CAPABLE);
        caps = RuntimeUtils.convertSpatialCapabilities(extensionCapabilities);
        assertThat(caps.hasCapability(SpatialCapabilities.SPATIAL_CAPABILITY_UI)).isTrue();
        assertThat(caps.hasCapability(SpatialCapabilities.SPATIAL_CAPABILITY_3D_CONTENT)).isFalse();
        assertThat(caps.hasCapability(SpatialCapabilities.SPATIAL_CAPABILITY_PASSTHROUGH_CONTROL))
                .isTrue();
        assertThat(caps.hasCapability(SpatialCapabilities.SPATIAL_CAPABILITY_APP_ENVIRONMENT))
                .isTrue();
        assertThat(caps.hasCapability(SpatialCapabilities.SPATIAL_CAPABILITY_SPATIAL_AUDIO))
                .isFalse();
        assertThat(caps.hasCapability(SpatialCapabilities.SPATIAL_CAPABILITY_EMBED_ACTIVITY))
                .isTrue();
    }

    @Test
    public void getIsSpatialEnvironmentPreferenceActive_convertsFromExtensionState() {
        assertThat(
                        RuntimeUtils.getIsSpatialEnvironmentPreferenceActive(
                                EnvironmentVisibilityState.INVISIBLE))
                .isFalse();

        assertThat(
                        RuntimeUtils.getIsSpatialEnvironmentPreferenceActive(
                                EnvironmentVisibilityState.HOME_VISIBLE))
                .isFalse();

        assertThat(
                        RuntimeUtils.getIsSpatialEnvironmentPreferenceActive(
                                EnvironmentVisibilityState.APP_VISIBLE))
                .isTrue();
    }

    @Test
    public void getPassthroughOpacity_returnsZeroFromDisabledExtensionState() {
        PassthroughVisibilityState passthroughVisibilityState =
                ShadowPassthroughVisibilityState.create(PassthroughVisibilityState.DISABLED, 0.0f);
        assertThat(RuntimeUtils.getPassthroughOpacity(passthroughVisibilityState)).isEqualTo(0.0f);

        passthroughVisibilityState =
                ShadowPassthroughVisibilityState.create(PassthroughVisibilityState.DISABLED, 1.0f);
        assertThat(RuntimeUtils.getPassthroughOpacity(passthroughVisibilityState)).isEqualTo(0.0f);
    }

    @Test
    public void getPassthroughOpacity_convertsValidValuesFromExtensionState() {
        PassthroughVisibilityState passthroughVisibilityState =
                ShadowPassthroughVisibilityState.create(PassthroughVisibilityState.HOME, 0.5f);
        assertThat(RuntimeUtils.getPassthroughOpacity(passthroughVisibilityState)).isEqualTo(0.5f);

        passthroughVisibilityState =
                ShadowPassthroughVisibilityState.create(PassthroughVisibilityState.APP, 0.75f);
        assertThat(RuntimeUtils.getPassthroughOpacity(passthroughVisibilityState)).isEqualTo(0.75f);

        passthroughVisibilityState =
                ShadowPassthroughVisibilityState.create(PassthroughVisibilityState.SYSTEM, 1.0f);
        assertThat(RuntimeUtils.getPassthroughOpacity(passthroughVisibilityState)).isEqualTo(1.0f);
    }

    @Test
    public void getPassthroughOpacity_convertsInvalidValuesFromExtensionStateToOneAndLogsError() {
        PassthroughVisibilityState passthroughVisibilityState =
                ShadowPassthroughVisibilityState.create(PassthroughVisibilityState.HOME, 0.0f);
        assertThat(RuntimeUtils.getPassthroughOpacity(passthroughVisibilityState)).isEqualTo(1.0f);

        passthroughVisibilityState =
                ShadowPassthroughVisibilityState.create(
                        PassthroughVisibilityState.APP, -0.0000001f);
        assertThat(RuntimeUtils.getPassthroughOpacity(passthroughVisibilityState)).isEqualTo(1.0f);

        passthroughVisibilityState =
                ShadowPassthroughVisibilityState.create(PassthroughVisibilityState.SYSTEM, -1.0f);
        assertThat(RuntimeUtils.getPassthroughOpacity(passthroughVisibilityState)).isEqualTo(1.0f);

        expectedLogMessagesRule.expectLogMessagePattern(
                Log.ERROR,
                "RuntimeUtils",
                Pattern.compile(".* Opacity should be greater than zero.*"));
    }

    @Test
    public void getInputEventSource_convertsFromExtensionSource() {
        assertThat(
                        RuntimeUtils.getInputEventSource(
                                com.android.extensions.xr.node.InputEvent.SOURCE_UNKNOWN))
                .isEqualTo(InputEvent.SOURCE_UNKNOWN);
        assertThat(
                        RuntimeUtils.getInputEventSource(
                                com.android.extensions.xr.node.InputEvent.SOURCE_HEAD))
                .isEqualTo(InputEvent.SOURCE_HEAD);
        assertThat(
                        RuntimeUtils.getInputEventSource(
                                com.android.extensions.xr.node.InputEvent.SOURCE_CONTROLLER))
                .isEqualTo(InputEvent.SOURCE_CONTROLLER);
        assertThat(
                        RuntimeUtils.getInputEventSource(
                                com.android.extensions.xr.node.InputEvent.SOURCE_HANDS))
                .isEqualTo(InputEvent.SOURCE_HANDS);
        assertThat(
                        RuntimeUtils.getInputEventSource(
                                com.android.extensions.xr.node.InputEvent.SOURCE_MOUSE))
                .isEqualTo(InputEvent.SOURCE_MOUSE);
        assertThat(
                        RuntimeUtils.getInputEventSource(
                                com.android.extensions.xr.node.InputEvent.SOURCE_GAZE_AND_GESTURE))
                .isEqualTo(InputEvent.SOURCE_GAZE_AND_GESTURE);
    }

    @Test
    public void getInputEventSource_throwsExceptionForInvalidValue() {
        assertThrows(IllegalArgumentException.class, () -> RuntimeUtils.getInputEventSource(100));
    }

    @Test
    public void getInputEventPointerType_convertsFromExtensionPointerType() {
        assertThat(
                        RuntimeUtils.getInputEventPointerType(
                                com.android.extensions.xr.node.InputEvent.POINTER_TYPE_DEFAULT))
                .isEqualTo(InputEvent.POINTER_TYPE_DEFAULT);
        assertThat(
                        RuntimeUtils.getInputEventPointerType(
                                com.android.extensions.xr.node.InputEvent.POINTER_TYPE_LEFT))
                .isEqualTo(InputEvent.POINTER_TYPE_LEFT);
        assertThat(
                        RuntimeUtils.getInputEventPointerType(
                                com.android.extensions.xr.node.InputEvent.POINTER_TYPE_RIGHT))
                .isEqualTo(InputEvent.POINTER_TYPE_RIGHT);
    }

    @Test
    public void getInputEventPointerType_throwsExceptionForInvalidValue() {
        assertThrows(
                IllegalArgumentException.class, () -> RuntimeUtils.getInputEventPointerType(100));
    }

    @Test
    public void getInputEventAction_convertsFromExtensionAction() {
        assertThat(
                        RuntimeUtils.getInputEventAction(
                                com.android.extensions.xr.node.InputEvent.ACTION_DOWN))
                .isEqualTo(InputEvent.ACTION_DOWN);
        assertThat(
                        RuntimeUtils.getInputEventAction(
                                com.android.extensions.xr.node.InputEvent.ACTION_UP))
                .isEqualTo(InputEvent.ACTION_UP);
        assertThat(
                        RuntimeUtils.getInputEventAction(
                                com.android.extensions.xr.node.InputEvent.ACTION_MOVE))
                .isEqualTo(InputEvent.ACTION_MOVE);
        assertThat(
                        RuntimeUtils.getInputEventAction(
                                com.android.extensions.xr.node.InputEvent.ACTION_CANCEL))
                .isEqualTo(InputEvent.ACTION_CANCEL);
        assertThat(
                        RuntimeUtils.getInputEventAction(
                                com.android.extensions.xr.node.InputEvent.ACTION_HOVER_MOVE))
                .isEqualTo(InputEvent.ACTION_HOVER_MOVE);
        assertThat(
                        RuntimeUtils.getInputEventAction(
                                com.android.extensions.xr.node.InputEvent.ACTION_HOVER_ENTER))
                .isEqualTo(InputEvent.ACTION_HOVER_ENTER);
        assertThat(
                        RuntimeUtils.getInputEventAction(
                                com.android.extensions.xr.node.InputEvent.ACTION_HOVER_EXIT))
                .isEqualTo(InputEvent.ACTION_HOVER_EXIT);
    }

    @Test
    public void getInputEventAction_throwsExceptionForInvalidValue() {
        assertThrows(IllegalArgumentException.class, () -> RuntimeUtils.getInputEventAction(100));
    }

    @Test
    public void getResizeEventState_convertsFromExtensionResizeState() {
        assertThat(RuntimeUtils.getResizeEventState(REFORM_STATE_UNKNOWN))
                .isEqualTo(ResizeEvent.RESIZE_STATE_UNKNOWN);
        assertThat(RuntimeUtils.getResizeEventState(REFORM_STATE_START))
                .isEqualTo(ResizeEvent.RESIZE_STATE_START);
        assertThat(RuntimeUtils.getResizeEventState(REFORM_STATE_ONGOING))
                .isEqualTo(ResizeEvent.RESIZE_STATE_ONGOING);
        assertThat(RuntimeUtils.getResizeEventState(REFORM_STATE_END))
                .isEqualTo(ResizeEvent.RESIZE_STATE_END);
    }

    @Test
    public void getResizeEventState_throwsExceptionForInvalidValue() {
        assertThrows(IllegalArgumentException.class, () -> RuntimeUtils.getResizeEventState(100));
    }

    @Test
    public void getHitTestResult_convertsFromExtensionHitTestResult() {
        float distance = 2.0f;
        Vec3 hitPosition = new Vec3(1.0f, 2.0f, 3.0f);
        Vec3 surfaceNormal = new Vec3(4.0f, 5.0f, 6.0f);
        int surfaceType = com.android.extensions.xr.space.HitTestResult.SURFACE_PANEL;

        com.android.extensions.xr.space.HitTestResult.Builder hitTestResultBuilder =
                new com.android.extensions.xr.space.HitTestResult.Builder(
                        distance, hitPosition, true, surfaceType);
        com.android.extensions.xr.space.HitTestResult extensionsHitTestResult =
                hitTestResultBuilder.setSurfaceNormal(surfaceNormal).build();

        HitTestResult hitTestResult = RuntimeUtils.getHitTestResult(extensionsHitTestResult);

        assertThat(hitTestResult.getDistance()).isEqualTo(distance);
        assertVector3(hitTestResult.getHitPosition(), new Vector3(1, 2, 3));
        assertVector3(hitTestResult.getSurfaceNormal(), new Vector3(4, 5, 6));
        assertThat(hitTestResult.getSurfaceType())
                .isEqualTo(HitTestResult.HitTestSurfaceType.HIT_TEST_RESULT_SURFACE_TYPE_PLANE);
    }

    @Test
    public void getHitTestResult_convertsFromExtensionHitTestResult_withNoHit() {
        float distance = Float.POSITIVE_INFINITY;
        Vec3 hitPosition = null;
        int surfaceType = com.android.extensions.xr.space.HitTestResult.SURFACE_UNKNOWN;

        com.android.extensions.xr.space.HitTestResult.Builder hitTestResultBuilder =
                new com.android.extensions.xr.space.HitTestResult.Builder(
                        distance, hitPosition, true, surfaceType);
        com.android.extensions.xr.space.HitTestResult extensionsHitTestResult =
                hitTestResultBuilder.build();

        HitTestResult hitTestResult = RuntimeUtils.getHitTestResult(extensionsHitTestResult);

        assertThat(hitTestResult.getDistance()).isEqualTo(distance);
        assertThat(hitTestResult.getHitPosition()).isNull();
        assertThat(hitTestResult.getSurfaceNormal()).isNull();
        assertThat(hitTestResult.getSurfaceType())
                .isEqualTo(HitTestResult.HitTestSurfaceType.HIT_TEST_RESULT_SURFACE_TYPE_UNKNOWN);
    }

    @Test
    public void convertSpatialVisibility_convertsFromExtensionVisibility() {
        assertThat(
                        RuntimeUtils.convertSpatialVisibility(
                                new VisibilityChangedEvent(VisibilityChangedEvent.VISIBLE)))
                .isEqualTo(new SpatialVisibility(SpatialVisibility.WITHIN_FOV));

        assertThat(
                        RuntimeUtils.convertSpatialVisibility(
                                new VisibilityChangedEvent(
                                        VisibilityChangedEvent.PARTIALLY_VISIBLE)))
                .isEqualTo(new SpatialVisibility(SpatialVisibility.PARTIALLY_WITHIN_FOV));

        assertThat(
                        RuntimeUtils.convertSpatialVisibility(
                                new VisibilityChangedEvent(VisibilityChangedEvent.OUTSIDE_OF_FOV)))
                .isEqualTo(new SpatialVisibility(SpatialVisibility.OUTSIDE_FOV));

        assertThat(
                        RuntimeUtils.convertSpatialVisibility(
                                new VisibilityChangedEvent(VisibilityChangedEvent.UNKNOWN)))
                .isEqualTo(new SpatialVisibility(SpatialVisibility.UNKNOWN));
    }

    @Test
    public void convertSpatialVisibility_throwsExceptionForInvalidValue() {
        assertThrows(
                IllegalArgumentException.class,
                () -> RuntimeUtils.convertSpatialVisibility(new VisibilityChangedEvent(100)));
    }
}
