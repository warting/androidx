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
import static com.google.common.util.concurrent.Futures.immediateFuture;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Activity;
import android.util.Size;

import androidx.xr.runtime.internal.ActivityPose.HitTestFilter;
import androidx.xr.runtime.internal.ActivityPose.HitTestFilterValue;
import androidx.xr.runtime.internal.ActivitySpace;
import androidx.xr.runtime.internal.Dimensions;
import androidx.xr.runtime.internal.HitTestResult;
import androidx.xr.runtime.internal.JxrPlatformAdapter;
import androidx.xr.runtime.math.Matrix4;
import androidx.xr.runtime.math.Pose;
import androidx.xr.runtime.math.Vector3;
import androidx.xr.scenecore.impl.extensions.XrExtensionsProvider;
import androidx.xr.scenecore.impl.perception.PerceptionLibrary;
import androidx.xr.scenecore.impl.perception.Session;
import androidx.xr.scenecore.testing.FakeImpressApi;
import androidx.xr.scenecore.testing.FakeScheduledExecutorService;

import com.android.extensions.xr.ShadowXrExtensions;
import com.android.extensions.xr.XrExtensions;
import com.android.extensions.xr.environment.EnvironmentVisibilityState;
import com.android.extensions.xr.environment.PassthroughVisibilityState;
import com.android.extensions.xr.environment.ShadowEnvironmentVisibilityState;
import com.android.extensions.xr.environment.ShadowPassthroughVisibilityState;
import com.android.extensions.xr.node.Vec3;
import com.android.extensions.xr.space.Bounds;
import com.android.extensions.xr.space.ShadowSpatialCapabilities;
import com.android.extensions.xr.space.ShadowSpatialState;
import com.android.extensions.xr.space.SpatialCapabilities;
import com.android.extensions.xr.space.SpatialState;

import com.google.androidxr.splitengine.SplitEngineSubspaceManager;
import com.google.ar.imp.view.splitengine.ImpSplitEngineRenderer;
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
    // TODO(b/329902726): Move this boilerplate for creating a TestJxrPlatformAdapter into a test
    // util
    private final ActivityController<Activity> mActivityController =
            Robolectric.buildActivity(Activity.class);
    private final Activity mActivity = mActivityController.create().start().get();
    private final FakeScheduledExecutorService mFakeExecutor = new FakeScheduledExecutorService();
    private final PerceptionLibrary mPerceptionLibrary = Mockito.mock(PerceptionLibrary.class);
    private final SplitEngineSubspaceManager mSplitEngineSubspaceManager =
            Mockito.mock(SplitEngineSubspaceManager.class);
    private final ImpSplitEngineRenderer mSplitEngineRenderer =
            Mockito.mock(ImpSplitEngineRenderer.class);

    private XrExtensions mXrExtensions;
    private FakeImpressApi mFakeImpressApi;
    private JxrPlatformAdapter mTestRuntime;
    private ActivitySpace mActivitySpace;

    @Before
    public void setUp() {
        mXrExtensions = XrExtensionsProvider.getXrExtensions();
        mFakeImpressApi = new FakeImpressApi();
        when(mPerceptionLibrary.initSession(eq(mActivity), anyInt(), eq(mFakeExecutor)))
                .thenReturn(immediateFuture(Mockito.mock(Session.class)));

        mTestRuntime =
                JxrPlatformAdapterAxr.create(
                        mActivity,
                        mFakeExecutor,
                        mXrExtensions,
                        mFakeImpressApi,
                        new EntityManager(),
                        mPerceptionLibrary,
                        mSplitEngineSubspaceManager,
                        mSplitEngineRenderer,
                        /* useSplitEngine= */ false);

        mActivitySpace = mTestRuntime.getActivitySpace();

        // This is slightly hacky. We're grabbing the singleton instance of the ActivitySpaceImpl
        // that
        // was created by the RuntimeImpl. Ideally we'd have an interface to inject the
        // ActivitySpace
        // for testing.  For now this is fine since there isn't an interface difference (yet).
        assertThat(mActivitySpace).isInstanceOf(ActivitySpaceImpl.class);
        assertThat(mActivitySpace).isNotNull();
    }

    @After
    public void tearDown() {
        // Dispose the runtime between test cases to clean up lingering references.
        mTestRuntime.dispose();
    }

    @Override
    protected SystemSpaceEntityImpl getSystemSpaceEntityImpl() {
        return (SystemSpaceEntityImpl) mActivitySpace;
    }

    @Override
    protected FakeScheduledExecutorService getDefaultFakeExecutor() {
        return mFakeExecutor;
    }

    @Override
    protected AndroidXrEntity createChildAndroidXrEntity() {
        return (AndroidXrEntity) mTestRuntime.createEntity(new Pose(), "child", mActivitySpace);
    }

    @Override
    protected ActivitySpaceImpl getActivitySpaceEntity() {
        return (ActivitySpaceImpl) mActivitySpace;
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

    @Test
    public void getBounds_returnsBounds() {
        assertThat(mActivitySpace.getBounds().width).isPositiveInfinity();
        assertThat(mActivitySpace.getBounds().height).isPositiveInfinity();
        assertThat(mActivitySpace.getBounds().depth).isPositiveInfinity();

        SpatialState spatialState =
                createSpatialState(/* bounds= */ new Bounds(100.0f, 200.0f, 300.0f));
        ShadowXrExtensions.extract(mXrExtensions).sendSpatialState(mActivity, spatialState);

        assertThat(mActivitySpace.getBounds().width).isEqualTo(100f);
        assertThat(mActivitySpace.getBounds().height).isEqualTo(200f);
        assertThat(mActivitySpace.getBounds().depth).isEqualTo(300f);
    }

    @Test
    public void addBoundsChangedListener_happyPath() {
        ActivitySpace.OnBoundsChangedListener listener =
                Mockito.mock(ActivitySpace.OnBoundsChangedListener.class);

        SpatialState spatialState =
                createSpatialState(/* bounds= */ new Bounds(100.0f, 200.0f, 300.0f));
        mActivitySpace.addOnBoundsChangedListener(listener);
        ShadowXrExtensions.extract(mXrExtensions).sendSpatialState(mActivity, spatialState);

        verify(listener).onBoundsChanged(Mockito.refEq(new Dimensions(100.0f, 200.0f, 300.0f)));
    }

    @Test
    public void removeBoundsChangedListener_happyPath() {
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
        ActivitySpaceImpl activitySpaceImpl = (ActivitySpaceImpl) mActivitySpace;

        assertPose(activitySpaceImpl.getPoseInActivitySpace(), new Pose());
    }

    @Test
    public void getActivitySpaceScale_returnsUnitScale() {
        ActivitySpaceImpl activitySpaceImpl = (ActivitySpaceImpl) mActivitySpace;
        activitySpaceImpl.setOpenXrReferenceSpacePose(Matrix4.fromScale(5f));
        assertVector3(activitySpaceImpl.getActivitySpaceScale(), new Vector3(1f, 1f, 1f));
    }

    @Test
    public void setScale_doesNothing() throws Exception {
        Vector3 scale = new Vector3(1, 1, 9999);
        mActivitySpace.setScale(scale);

        // The returned scale(s) here should be the identity scale despite the setScale call.
        assertThat(mActivitySpace.getScale().getX()).isWithin(1e-5f).of(1.0f);
        assertThat(mActivitySpace.getScale().getY()).isWithin(1e-5f).of(1.0f);
        assertThat(mActivitySpace.getScale().getZ()).isWithin(1e-5f).of(1.0f);

        // Note that there's no exception thrown.
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
}
