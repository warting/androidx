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

package androidx.xr.scenecore.spatial.core;

import static androidx.xr.runtime.testing.math.MathAssertions.assertPose;
import static androidx.xr.runtime.testing.math.MathAssertions.assertVector3;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.util.concurrent.Futures.immediateFuture;

import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Activity;
import android.util.Size;

import androidx.xr.runtime.math.BoundingBox;
import androidx.xr.runtime.math.Matrix4;
import androidx.xr.runtime.math.Pose;
import androidx.xr.runtime.math.Quaternion;
import androidx.xr.runtime.math.Vector3;
import androidx.xr.runtime.testing.FakeSpatialModeChangeListener;
import androidx.xr.scenecore.impl.extensions.XrExtensionsProvider;
import androidx.xr.scenecore.impl.perception.PerceptionLibrary;
import androidx.xr.scenecore.impl.perception.Session;
import androidx.xr.scenecore.internal.ActivityPose.HitTestFilter;
import androidx.xr.scenecore.internal.ActivityPose.HitTestFilterValue;
import androidx.xr.scenecore.internal.ActivitySpace;
import androidx.xr.scenecore.internal.HitTestResult;
import androidx.xr.scenecore.internal.SceneRuntime;
import androidx.xr.scenecore.internal.Space;
import androidx.xr.scenecore.testing.FakeScheduledExecutorService;

import com.android.extensions.xr.ShadowXrExtensions;
import com.android.extensions.xr.XrExtensions;
import com.android.extensions.xr.environment.EnvironmentVisibilityState;
import com.android.extensions.xr.environment.PassthroughVisibilityState;
import com.android.extensions.xr.environment.ShadowEnvironmentVisibilityState;
import com.android.extensions.xr.environment.ShadowPassthroughVisibilityState;
import com.android.extensions.xr.node.Box3;
import com.android.extensions.xr.node.NodeRepository;
import com.android.extensions.xr.node.Vec3;
import com.android.extensions.xr.space.Bounds;
import com.android.extensions.xr.space.ShadowSpatialCapabilities;
import com.android.extensions.xr.space.ShadowSpatialState;
import com.android.extensions.xr.space.SpatialCapabilities;
import com.android.extensions.xr.space.SpatialState;

import com.google.common.util.concurrent.ListenableFuture;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;

@RunWith(RobolectricTestRunner.class)
public final class ActivitySpaceImplTest extends SystemSpaceEntityImplTest {
    // TODO(b/329902726): Move this boilerplate for creating a TestSceneRuntime into a test util
    private final ActivityController<Activity> mActivityController =
            Robolectric.buildActivity(Activity.class);
    private final Activity mActivity = mActivityController.create().start().get();
    private final FakeScheduledExecutorService mFakeExecutor = new FakeScheduledExecutorService();
    private final PerceptionLibrary mPerceptionLibrary = Mockito.mock(PerceptionLibrary.class);
    private final NodeRepository mNodeRepository = NodeRepository.getInstance();
    private XrExtensions mXrExtensions;
    private SceneRuntime mTestRuntime;
    private ActivitySpaceImpl mActivitySpace;

    private SceneRuntime createTestSceneRuntime(boolean unScaledGravityAlignedActivitySpace) {
        return SpatialSceneRuntime.create(
                mActivity,
                mFakeExecutor,
                mXrExtensions,
                new EntityManager(),
                mPerceptionLibrary,
                unScaledGravityAlignedActivitySpace);
    }

    @Before
    public void setUp() {
        mXrExtensions = XrExtensionsProvider.getXrExtensions();
        when(mPerceptionLibrary.initSession(eq(mActivity), anyInt(), eq(mFakeExecutor)))
                .thenReturn(immediateFuture(Mockito.mock(Session.class)));

        mTestRuntime = createTestSceneRuntime(/* unScaledGravityAlignedActivitySpace= */ false);

        mActivitySpace = (ActivitySpaceImpl) mTestRuntime.getActivitySpace();

        // This is slightly hacky. We're grabbing the singleton instance of the ActivitySpaceImpl
        // that was created by the RuntimeImpl. Ideally we'd have an interface to inject the
        // ActivitySpace for testing.  For now this is fine since there isn't an interface
        // difference (yet).
        assertThat(mActivitySpace).isNotNull();
    }

    @After
    public void tearDown() {
        // Dispose the runtime between test cases to clean up lingering references.
        mTestRuntime.dispose();
    }

    @Override
    protected SystemSpaceEntityImpl getSystemSpaceEntityImpl() {
        return mActivitySpace;
    }

    @Override
    protected FakeScheduledExecutorService getDefaultFakeExecutor() {
        return mFakeExecutor;
    }

    @Override
    protected AndroidXrEntity createChildAndroidXrEntity() {
        return (AndroidXrEntity)
                mTestRuntime.createGroupEntity(new Pose(), "child", mActivitySpace);
    }

    @Override
    protected ActivitySpaceImpl getActivitySpaceEntity() {
        return mActivitySpace;
    }

    private SpatialState createSpatialState(Bounds bounds) {
        boolean isUnbounded =
                bounds.getWidth() == Float.POSITIVE_INFINITY
                        && bounds.getHeight() == Float.POSITIVE_INFINITY
                        && bounds.getDepth() == Float.POSITIVE_INFINITY;
        SpatialCapabilities capabilities =
                isUnbounded
                        ? ShadowSpatialCapabilities.createAll()
                        : ShadowSpatialCapabilities.create();
        return ShadowSpatialState.create(
                /* bounds= */ bounds,
                /* capabilities= */ capabilities,
                /* environmentVisibilityState= */ ShadowEnvironmentVisibilityState.create(
                        /* state= */ EnvironmentVisibilityState.INVISIBLE),
                /* passthroughVisibilityState= */ ShadowPassthroughVisibilityState.create(
                        /* state= */ PassthroughVisibilityState.DISABLED, /* opacity= */ 0.0f),
                /* isEnvironmentInherited= */ false,
                /* mainWindowSize= */ new Size(100, 100),
                /* preferredAspectRatio= */ 1.0f,
                /* sceneParentTransform= */ null);
    }

    // TODO: b/430219226 Remove getBounds and addBoundsChangedListener

    @Test
    public void removeOnBoundsChangedListener_happyPath() {
        ActivitySpace.OnBoundsChangedListener listener =
                Mockito.mock(ActivitySpace.OnBoundsChangedListener.class);

        mActivitySpace.addOnBoundsChangedListener(listener);
        mActivitySpace.removeOnBoundsChangedListener(listener);
        SpatialState spatialState =
                createSpatialState(/* bounds= */ new Bounds(100.0f, 200.0f, 300.0f));
        ShadowXrExtensions.extract(mXrExtensions).sendSpatialState(mActivity, spatialState);

        verify(listener, Mockito.never()).onBoundsChanged(Mockito.any());
    }

    @Test
    public void getPoseInActivitySpace_returnsIdentity() {
        ActivitySpaceImpl activitySpaceImpl = mActivitySpace;

        assertPose(activitySpaceImpl.getPoseInActivitySpace(), new Pose());
    }

    @Test
    public void getActivitySpaceScale_returnsUnitScale() {
        ActivitySpaceImpl activitySpaceImpl = mActivitySpace;
        activitySpaceImpl.setOpenXrReferenceSpacePose(Matrix4.fromScale(5f));
        assertVector3(activitySpaceImpl.getActivitySpaceScale(), new Vector3(1f, 1f, 1f));
    }

    @Test
    public void setScale_throwsException() throws Exception {
        Vector3 scale = new Vector3(1, 1, 9999);

        assertThrows(UnsupportedOperationException.class, () -> mActivitySpace.setScale(scale));
    }

    @Test
    public void hitTest_returnsHitTest() throws Exception {
        float distance = 2.0f;
        Vec3 hitPosition = new Vec3(1.0f, 2.0f, 3.0f);
        Vec3 surfaceNormal = new Vec3(4.0f, 5.0f, 6.0f);
        int surfaceType = com.android.extensions.xr.space.HitTestResult.SURFACE_PANEL;
        @HitTestFilterValue int hitTestFilter = HitTestFilter.SELF_SCENE;

        com.android.extensions.xr.space.HitTestResult.Builder hitTestResultBuilder =
                new com.android.extensions.xr.space.HitTestResult.Builder(
                        distance, hitPosition, true, surfaceType);
        com.android.extensions.xr.space.HitTestResult extensionsHitTestResult =
                hitTestResultBuilder.setSurfaceNormal(surfaceNormal).build();
        ShadowXrExtensions.extract(mXrExtensions)
                .setHitTestResult(mActivity, extensionsHitTestResult);

        ListenableFuture<HitTestResult> hitTestResultFuture =
                mActivitySpace.hitTest(new Vector3(1, 1, 1), new Vector3(1, 1, 1), hitTestFilter);
        mFakeExecutor.runAll();
        HitTestResult hitTestResult = hitTestResultFuture.get();

        assertThat(hitTestResult.getDistance()).isEqualTo(distance);
        assertVector3(hitTestResult.getHitPosition(), new Vector3(1, 2, 3));
        assertVector3(hitTestResult.getSurfaceNormal(), new Vector3(4, 5, 6));
        assertThat(hitTestResult.getSurfaceType())
                .isEqualTo(HitTestResult.HitTestSurfaceType.HIT_TEST_RESULT_SURFACE_TYPE_PLANE);
    }

    @Test
    public void handleOriginUpdate_unscaledGravityAlignedFalse_handlerCalled() {
        FakeSpatialModeChangeListener handler = new FakeSpatialModeChangeListener();
        mActivitySpace.setSpatialModeChangeListener(handler);

        Quaternion initialRotation = Quaternion.fromEulerAngles(30, 0, 0);
        Vector3 initialScale = new Vector3(2.0f, 2.0f, 2.0f);
        Matrix4 newTransform = Matrix4.fromTrs(Vector3.Zero, initialRotation, initialScale);

        mActivitySpace.handleOriginUpdate(newTransform);

        assertThat(handler.getLastRecommendedPose()).isEqualTo(new Pose());
        assertThat(handler.getLastRecommendedScale()).isEqualTo(Vector3.One);
        assertThat(handler.getUpdateCount()).isEqualTo(1);
    }

    @Test
    public void
            handleOriginUpdate_unscaledGravityAlignedTrue_scaleAndRotationApplied_handlerCalled() {
        FakeSpatialModeChangeListener handler = new FakeSpatialModeChangeListener();
        mTestRuntime = createTestSceneRuntime(/* unScaledGravityAlignedActivitySpace= */ true);
        mActivitySpace = (ActivitySpaceImpl) mTestRuntime.getActivitySpace();
        mActivitySpace.setSpatialModeChangeListener(handler);

        Quaternion initialRotation = Quaternion.fromEulerAngles(45, 0, 0);
        Vector3 initialScale = new Vector3(2.0f, 2.0f, 2.0f);
        Matrix4 newTransform = Matrix4.fromTrs(Vector3.One, initialRotation, initialScale);

        mActivitySpace.handleOriginUpdate(newTransform);

        Vector3 activitySpaceScale =
                RuntimeUtils.getVector3(mNodeRepository.getScale(mActivitySpace.getNode()));
        assertVector3(
                activitySpaceScale,
                new Vector3(
                        1f / initialScale.getX(),
                        1f / initialScale.getY(),
                        1f / initialScale.getZ()));
        Quaternion activitySpaceRotation =
                RuntimeUtils.getQuaternion(
                        mNodeRepository.getOrientation(mActivitySpace.getNode()));
        Quaternion expectedRotation = initialRotation.getInverse();
        assertThat(activitySpaceRotation.getX()).isWithin(0.001f).of(expectedRotation.getX());
        assertThat(activitySpaceRotation.getY()).isWithin(0.001f).of(expectedRotation.getY());
        assertThat(activitySpaceRotation.getZ()).isWithin(0.001f).of(expectedRotation.getZ());
        assertThat(activitySpaceRotation.getW()).isWithin(0.001f).of(expectedRotation.getW());

        Pose expectedPose = new Pose(Vector3.Zero, initialRotation);
        assertThat(handler.getLastRecommendedPose()).isEqualTo(expectedPose);
        assertVector3(handler.getLastRecommendedScale(), initialScale);
        assertThat(handler.getUpdateCount()).isEqualTo(1);
    }

    @Test
    public void handleOriginUpdate_noHandler_doesNotCallHandler() {
        FakeSpatialModeChangeListener handler = new FakeSpatialModeChangeListener();
        mTestRuntime = createTestSceneRuntime(/* unScaledGravityAlignedActivitySpace= */ true);
        mActivitySpace = (ActivitySpaceImpl) mTestRuntime.getActivitySpace();
        mActivitySpace.setSpatialModeChangeListener(null);

        Quaternion initialRotation = Quaternion.fromEulerAngles(0, 0, 90);
        Vector3 initialScale = new Vector3(3.0f, 3.0f, 3.0f);
        Matrix4 newTransform = Matrix4.fromTrs(Vector3.Zero, initialRotation, initialScale);

        mActivitySpace.handleOriginUpdate(newTransform);

        Vector3 activitySpaceScale =
                RuntimeUtils.getVector3(mNodeRepository.getScale(mActivitySpace.getNode()));
        assertVector3(
                activitySpaceScale,
                new Vector3(
                        1.0f / initialScale.getX(),
                        1.0f / initialScale.getY(),
                        1.0f / initialScale.getZ()));
        Quaternion activitySpaceRotation =
                RuntimeUtils.getQuaternion(
                        mNodeRepository.getOrientation(mActivitySpace.getNode()));
        Quaternion expectedRotation = newTransform.unscaled().getRotation().getInverse();
        assertThat(activitySpaceRotation.getX()).isWithin(0.001f).of(expectedRotation.getX());
        assertThat(activitySpaceRotation.getY()).isWithin(0.001f).of(expectedRotation.getY());
        assertThat(activitySpaceRotation.getZ()).isWithin(0.001f).of(expectedRotation.getZ());
        assertThat(activitySpaceRotation.getW()).isWithin(0.001f).of(expectedRotation.getW());

        assertThat(handler.getUpdateCount()).isEqualTo(0);
    }

    @Test
    public void getRecommendedContentBoxInFullSpace_returnsCorrectlyConvertedBox() {
        Box3 box = new Box3(-1.73f / 2, -1.61f / 2, -0.5f / 2, 1.73f / 2, 1.61f / 2, 0.5f / 2);
        ShadowXrExtensions.extract(mXrExtensions).setRecommendedContentBoxInFullSpace(box);

        BoundingBox resultBox = mActivitySpace.getRecommendedContentBoxInFullSpace();

        assertThat(resultBox).isNotNull();
        BoundingBox expectedBox =
                new BoundingBox(
                        new Vector3(-1.73f / 2, -1.61f / 2, -0.5f / 2),
                        new Vector3(1.73f / 2, 1.61f / 2, 0.5f / 2));
        assertThat(resultBox).isEqualTo(expectedBox);
    }

    @Test
    public void activitySpaceSetPose_throwsException() throws Exception {
        Pose pose = new Pose();

        assertThrows(UnsupportedOperationException.class, () -> mActivitySpace.setPose(pose));
    }

    @Test
    public void getPoseRelativeToParentSpace_throwsException() throws Exception {
        assertThrows(
                UnsupportedOperationException.class, () -> mActivitySpace.getPose(Space.PARENT));
    }

    @Test
    public void getPoseRelativeToActivitySpace_returnsIdentity() {
        assertPose(mActivitySpace.getPose(Space.ACTIVITY), mActivitySpace.getPoseInActivitySpace());
    }

    // TODO: b/434230591 getPoseRelativeToRealWorldSpace_returnsPerceptionSpacePose is removed.

    @Test
    public void getScaleRelativeToParentSpace_throwsException() throws Exception {
        assertThrows(
                UnsupportedOperationException.class, () -> mActivitySpace.getScale(Space.PARENT));
    }

    @Test
    public void getScaleRelativeToActivitySpace_returnsActivitySpaceScale() {
        assertVector3(
                mActivitySpace.getScale(Space.ACTIVITY), mActivitySpace.getActivitySpaceScale());
    }

    @Test
    public void getScaleRelativeToRealWorldSpace_returnsVector3One() {
        assertVector3(mActivitySpace.getScale(Space.REAL_WORLD), new Vector3(1f, 1f, 1f));
    }
}
