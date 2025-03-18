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

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.util.concurrent.Futures.immediateFuture;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.app.Activity;
import android.content.Context;
import android.hardware.display.DisplayManager;
import android.os.SystemClock;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup.LayoutParams;

import androidx.xr.runtime.math.Matrix4;
import androidx.xr.runtime.math.Pose;
import androidx.xr.scenecore.JxrPlatformAdapter.ActivityPanelEntity;
import androidx.xr.scenecore.JxrPlatformAdapter.ActivitySpace;
import androidx.xr.scenecore.JxrPlatformAdapter.AnchorEntity;
import androidx.xr.scenecore.JxrPlatformAdapter.CameraViewActivityPose;
import androidx.xr.scenecore.JxrPlatformAdapter.Dimensions;
import androidx.xr.scenecore.JxrPlatformAdapter.Entity;
import androidx.xr.scenecore.JxrPlatformAdapter.GltfEntity;
import androidx.xr.scenecore.JxrPlatformAdapter.GltfModelResource;
import androidx.xr.scenecore.JxrPlatformAdapter.HeadActivityPose;
import androidx.xr.scenecore.JxrPlatformAdapter.PanelEntity;
import androidx.xr.scenecore.JxrPlatformAdapter.PerceptionSpaceActivityPose;
import androidx.xr.scenecore.JxrPlatformAdapter.PixelDimensions;
import androidx.xr.scenecore.JxrPlatformAdapter.PlaneSemantic;
import androidx.xr.scenecore.JxrPlatformAdapter.PlaneType;
import androidx.xr.scenecore.impl.extensions.XrExtensionsProvider;
import androidx.xr.scenecore.impl.perception.PerceptionLibrary;
import androidx.xr.scenecore.impl.perception.Session;
import androidx.xr.scenecore.testing.FakeImpressApi;
import androidx.xr.scenecore.testing.FakeScheduledExecutorService;

import com.android.extensions.xr.XrExtensions;
import com.android.extensions.xr.node.Node;

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
public class EntityManagerTest {

    private static final int VGA_WIDTH = 640;
    private static final int VGA_HEIGHT = 480;
    private final XrExtensions mXrExtensions = XrExtensionsProvider.getXrExtensions();
    private final FakeImpressApi mFakeImpressApi = new FakeImpressApi();
    private final FakeScheduledExecutorService mFakeExecutor = new FakeScheduledExecutorService();
    private final PerceptionLibrary mPerceptionLibrary = mock(PerceptionLibrary.class);
    private final Session mSession = mock(Session.class);
    private final AndroidXrEntity mActivitySpaceRoot = mock(AndroidXrEntity.class);
    private final FakeScheduledExecutorService mExecutor = new FakeScheduledExecutorService();
    private final Node mPanelEntityNode = mXrExtensions.createNode();
    private final Node mAnchorEntityNode = mXrExtensions.createNode();
    private final EntityManager mEntityManager = new EntityManager();
    private final SplitEngineSubspaceManager mSplitEngineSubspaceManager =
            Mockito.mock(SplitEngineSubspaceManager.class);
    private final ImpSplitEngineRenderer mSplitEngineRenderer =
            Mockito.mock(ImpSplitEngineRenderer.class);
    private Node mContentLessEntityNode;
    private Node mGltfEntityNode;
    private Activity mActivity;
    private JxrPlatformAdapterAxr mPlatformAdapterAxr;
    private ActivitySpaceImpl mActivitySpace;

    @Before
    public void setUp() {
        try (ActivityController<Activity> activityController =
                Robolectric.buildActivity(Activity.class)) {
            mActivity = activityController.create().start().get();
        }
        when(mPerceptionLibrary.initSession(eq(mActivity), anyInt(), eq(mFakeExecutor)))
                .thenReturn(immediateFuture(mSession));
        when(mPerceptionLibrary.getActivity()).thenReturn(mActivity);
        mPlatformAdapterAxr =
                JxrPlatformAdapterAxr.create(
                        mActivity,
                        mFakeExecutor,
                        mXrExtensions,
                        mFakeImpressApi,
                        mEntityManager,
                        mPerceptionLibrary,
                        mSplitEngineSubspaceManager,
                        mSplitEngineRenderer,
                        /* useSplitEngine= */ false);
        Node taskNode = mXrExtensions.createNode();
        mActivitySpace =
                new ActivitySpaceImpl(
                        taskNode,
                        mXrExtensions,
                        mEntityManager,
                        () -> mXrExtensions.getSpatialState(mActivity),
                        mExecutor);
        long currentTimeMillis = 1000000000L;
        SystemClock.setCurrentTimeMillis(currentTimeMillis);

        // By default, set the activity space to the root of the underlying OpenXR reference space.
        mActivitySpace.setOpenXrReferenceSpacePose(Matrix4.Identity);
    }

    @After
    public void tearDown() {
        // Dispose the runtime between test cases to clean up lingering references.
        mPlatformAdapterAxr.dispose();
    }

    @Test
    public void creatingEntity_addsEntityToEntityManager() throws Exception {
        GltfEntity gltfEntity = createGltfEntity();
        PanelEntity panelEntity = createPanelEntity();
        Entity contentlessEntity = createContentlessEntity();
        AnchorEntity anchorEntity = createAnchorEntity();
        ActivityPanelEntity activityPanelEntity = createActivityPanelEntity();

        // Entity manager also contains the main panel entity and activity space, which are created
        // when
        // the runtime is created.
        assertThat(mEntityManager.getAllEntities().size()).isAtLeast(5);
        assertThat(mEntityManager.getAllEntities())
                .containsAtLeast(
                        gltfEntity,
                        panelEntity,
                        contentlessEntity,
                        anchorEntity,
                        activityPanelEntity);
    }

    @Test
    public void getEntityForNode_returnsEntity() throws Exception {
        GltfEntity gltfEntity = createGltfEntity();
        PanelEntity panelEntity = createPanelEntity();
        Entity contentlessEntity = createContentlessEntity();
        AnchorEntity anchorEntity = createAnchorEntity();
        Node testNode = mXrExtensions.createNode();

        assertThat(mEntityManager.getEntityForNode(mGltfEntityNode)).isEqualTo(gltfEntity);
        assertThat(mEntityManager.getEntityForNode(mPanelEntityNode)).isEqualTo(panelEntity);
        assertThat(mEntityManager.getEntityForNode(mContentLessEntityNode))
                .isEqualTo(contentlessEntity);
        assertThat(mEntityManager.getEntityForNode(mAnchorEntityNode)).isEqualTo(anchorEntity);
        assertThat(mEntityManager.getEntityForNode(testNode)).isNull();
    }

    @Test
    public void getEntityByType_returnsEntityOfType() throws Exception {
        GltfEntity gltfEntity = createGltfEntity();
        PanelEntity panelEntity = createPanelEntity();
        Entity contentlessEntity = createContentlessEntity();
        AnchorEntity anchorEntity = createAnchorEntity();
        ActivityPanelEntity activityPanelEntity = createActivityPanelEntity();

        assertThat(mEntityManager.getEntitiesOfType(GltfEntity.class)).containsExactly(gltfEntity);
        // MainPanel is also a PanelEntity.
        assertThat(mEntityManager.getEntitiesOfType(PanelEntity.class)).contains(panelEntity);
        // Base class of all entities.
        assertThat(mEntityManager.getEntitiesOfType(Entity.class)).contains(contentlessEntity);
        assertThat(mEntityManager.getEntitiesOfType(AnchorEntity.class))
                .containsExactly(anchorEntity);
        assertThat(mEntityManager.getEntitiesOfType(ActivityPanelEntity.class))
                .containsExactly(activityPanelEntity);
    }

    @Test
    public void removeEntity_removesFromEntityManager() throws Exception {
        GltfEntity gltfEntity = createGltfEntity();
        PanelEntity panelEntity = createPanelEntity();
        Entity contentlessEntity = createContentlessEntity();
        AnchorEntity anchorEntity = createAnchorEntity();
        ActivityPanelEntity activityPanelEntity = createActivityPanelEntity();

        assertThat(mEntityManager.getAllEntities().size()).isAtLeast(5);
        assertThat(mEntityManager.getAllEntities())
                .containsAtLeast(
                        gltfEntity,
                        panelEntity,
                        contentlessEntity,
                        anchorEntity,
                        activityPanelEntity);

        mEntityManager.removeEntityForNode(mContentLessEntityNode);

        assertThat(mEntityManager.getAllEntities().size()).isAtLeast(4);
        assertThat(mEntityManager.getAllEntities()).doesNotContain(contentlessEntity);
    }

    @Test
    public void disposeEntity_removesFromEntityManager() throws Exception {
        GltfEntity gltfEntity = createGltfEntity();
        PanelEntity panelEntity = createPanelEntity();
        Entity contentlessEntity = createContentlessEntity();
        AnchorEntity anchorEntity = createAnchorEntity();
        ActivityPanelEntity activityPanelEntity = createActivityPanelEntity();

        assertThat(mEntityManager.getAllEntities().size()).isAtLeast(5);
        assertThat(mEntityManager.getAllEntities())
                .containsAtLeast(
                        gltfEntity,
                        panelEntity,
                        contentlessEntity,
                        anchorEntity,
                        activityPanelEntity);

        contentlessEntity.dispose();

        assertThat(mEntityManager.getAllEntities().size()).isAtLeast(4);
        assertThat(mEntityManager.getAllEntities()).doesNotContain(contentlessEntity);
    }

    @Test
    public void getAllSystemSpaceActivityPoses_returnsAllSystemSpaceActivityPoses()
            throws Exception {
        assertThat(mEntityManager.getAllSystemSpaceActivityPoses().size()).isAtLeast(4);
        assertThat(mEntityManager.getAllSystemSpaceActivityPoses())
                .containsAtLeast(
                        mPlatformAdapterAxr.getActivitySpace(),
                        mPlatformAdapterAxr.getPerceptionSpaceActivityPose());
    }

    @Test
    public void getSystemSpaceActivityPoseOfType_returnsSystemSpaceActivityPoseOfType()
            throws Exception {
        assertThat(mEntityManager.getSystemSpaceActivityPoseOfType(ActivitySpace.class).get(0))
                .isInstanceOf(ActivitySpaceImpl.class);
        assertThat(
                        mEntityManager
                                .getSystemSpaceActivityPoseOfType(PerceptionSpaceActivityPose.class)
                                .get(0))
                .isInstanceOf(PerceptionSpaceActivityPoseImpl.class);
        assertThat(mEntityManager.getSystemSpaceActivityPoseOfType(HeadActivityPose.class).get(0))
                .isInstanceOf(HeadActivityPoseImpl.class);
        assertThat(
                        mEntityManager
                                .getSystemSpaceActivityPoseOfType(CameraViewActivityPose.class)
                                .get(0))
                .isInstanceOf(CameraViewActivityPoseImpl.class);
    }

    @Test
    public void clearEntityManager_removesAllEntityFromEntityManager() throws Exception {
        GltfEntity gltfEntity = createGltfEntity();
        PanelEntity panelEntity = createPanelEntity();
        Entity contentlessEntity = createContentlessEntity();
        AnchorEntity anchorEntity = createAnchorEntity();
        ActivityPanelEntity activityPanelEntity = createActivityPanelEntity();

        assertThat(mEntityManager.getAllEntities().size()).isAtLeast(5);
        assertThat(mEntityManager.getAllEntities())
                .containsAtLeast(
                        gltfEntity,
                        panelEntity,
                        contentlessEntity,
                        anchorEntity,
                        activityPanelEntity);

        mEntityManager.clear();

        assertThat(mEntityManager.getAllEntities()).isEmpty();
        assertThat(mEntityManager.getAllSystemSpaceActivityPoses()).isEmpty();
    }

    private GltfEntity createGltfEntity() throws Exception {
        ListenableFuture<GltfModelResource> modelFuture =
                mPlatformAdapterAxr.loadGltfByAssetName("FakeAsset.glb");
        assertThat(modelFuture).isNotNull();
        GltfModelResource model = modelFuture.get();
        GltfEntityImpl gltfEntity =
                new GltfEntityImpl(
                        (GltfModelResourceImpl) model,
                        mActivitySpaceRoot,
                        mXrExtensions,
                        mEntityManager,
                        mExecutor);
        mGltfEntityNode = gltfEntity.getNode();
        mEntityManager.setEntityForNode(mGltfEntityNode, gltfEntity);
        return gltfEntity;
    }

    private PanelEntity createPanelEntity() {
        Display display = mActivity.getSystemService(DisplayManager.class).getDisplays()[0];
        Context displayContext = mActivity.createDisplayContext(display);
        View view = new View(displayContext);
        view.setLayoutParams(new LayoutParams(VGA_WIDTH, VGA_HEIGHT));
        PanelEntityImpl panelEntity =
                new PanelEntityImpl(
                        displayContext,
                        mPanelEntityNode,
                        view,
                        mXrExtensions,
                        mEntityManager,
                        new PixelDimensions(VGA_WIDTH, VGA_HEIGHT),
                        "panel",
                        mExecutor);
        mEntityManager.setEntityForNode(mPanelEntityNode, panelEntity);
        return panelEntity;
    }

    private Entity createContentlessEntity() {
        Entity contentlessEntity =
                mPlatformAdapterAxr.createEntity(
                        new Pose(), "testContentLess", mPlatformAdapterAxr.getActivitySpace());
        mContentLessEntityNode = ((AndroidXrEntity) contentlessEntity).getNode();
        mEntityManager.setEntityForNode(mContentLessEntityNode, contentlessEntity);
        return contentlessEntity;
    }

    private AnchorEntity createAnchorEntity() {
        AnchorEntityImpl anchorEntity =
                AnchorEntityImpl.createSemanticAnchor(
                        mAnchorEntityNode,
                        new Dimensions(1f, 1f, 1f),
                        PlaneType.VERTICAL,
                        PlaneSemantic.WALL,
                        null,
                        mActivitySpace,
                        mActivitySpaceRoot,
                        mXrExtensions,
                        mEntityManager,
                        mExecutor,
                        mPerceptionLibrary);
        mEntityManager.setEntityForNode(mAnchorEntityNode, anchorEntity);
        return anchorEntity;
    }

    private ActivityPanelEntity createActivityPanelEntity() {
        return mPlatformAdapterAxr.createActivityPanelEntity(
                new Pose(),
                new PixelDimensions(VGA_WIDTH, VGA_HEIGHT),
                "test",
                mActivity,
                mActivitySpace);
    }
}
