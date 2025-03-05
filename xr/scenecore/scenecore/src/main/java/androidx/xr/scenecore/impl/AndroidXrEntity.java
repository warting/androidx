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
import androidx.xr.runtime.math.Pose;
import androidx.xr.runtime.math.Quaternion;
import androidx.xr.runtime.math.Vector3;
import androidx.xr.scenecore.JxrPlatformAdapter.ActivitySpace;
import androidx.xr.scenecore.JxrPlatformAdapter.Entity;
import androidx.xr.scenecore.JxrPlatformAdapter.InputEventListener;
import androidx.xr.scenecore.JxrPlatformAdapter.PerceptionSpaceActivityPose;
import androidx.xr.scenecore.JxrPlatformAdapter.PointerCaptureComponent;
import androidx.xr.scenecore.JxrPlatformAdapter.Space;
import androidx.xr.scenecore.JxrPlatformAdapter.SpaceValue;
import androidx.xr.scenecore.common.BaseEntity;

import com.android.extensions.xr.XrExtensions;
import com.android.extensions.xr.function.Consumer;
import com.android.extensions.xr.node.InputEvent;
import com.android.extensions.xr.node.Node;
import com.android.extensions.xr.node.NodeTransaction;
import com.android.extensions.xr.node.ReformEvent;
import com.android.extensions.xr.node.ReformOptions;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Implementation of a RealityCore Entity that wraps an android XR extension Node.
 *
 * <p>This should not be created on its own but should be inherited by objects that need to wrap an
 * Android extension node.
 */
@SuppressWarnings({"BanSynchronizedMethods", "BanConcurrentHashMap"})
abstract class AndroidXrEntity extends BaseEntity implements Entity {

    protected final Node mNode;
    protected final XrExtensions mExtensions;
    protected final ScheduledExecutorService mExecutor;
    // Visible for testing
    final ConcurrentHashMap<InputEventListener, Executor> mInputEventListenerMap =
            new ConcurrentHashMap<>();
    Optional<InputEventListener> mPointerCaptureInputEventListener = Optional.empty();
    Optional<Executor> mPointerCaptureExecutor = Optional.empty();
    final ConcurrentHashMap<Consumer<ReformEvent>, Executor> mReformEventConsumerMap =
            new ConcurrentHashMap<>();
    private final EntityManager mEntityManager;
    private ReformOptions mReformOptions;

    AndroidXrEntity(
            Node node,
            XrExtensions extensions,
            EntityManager entityManager,
            ScheduledExecutorService executor) {
        mNode = node;
        mExtensions = extensions;
        mEntityManager = entityManager;
        mExecutor = executor;
        mEntityManager.setEntityForNode(node, this);
    }

    @NonNull
    @Override
    public Pose getPose(@SpaceValue int relativeTo) {
        switch (relativeTo) {
            case Space.PARENT:
                return super.getPose(relativeTo);
            case Space.ACTIVITY:
                return getPoseInActivitySpace();
            case Space.REAL_WORLD:
                return getPoseInPerceptionSpace();
            default:
                throw new IllegalArgumentException("Unsupported relativeTo value: " + relativeTo);
        }
    }

    @Override
    public void setPose(@NonNull Pose pose, @SpaceValue int relativeTo) {
        // TODO: b/321268237 - Minimize the number of node transactions
        Pose localPose;
        switch (relativeTo) {
            case Space.PARENT:
                localPose = pose;
                break;
            case Space.ACTIVITY:
                localPose = getLocalPoseForActivitySpacePose(pose);
                break;
            case Space.REAL_WORLD:
                localPose = getLocalPoseForPerceptionSpacePose(pose);
                break;
            default:
                throw new IllegalArgumentException("Unsupported relativeTo value: " + relativeTo);
        }
        super.setPose(localPose, Space.PARENT);

        try (NodeTransaction transaction = mExtensions.createNodeTransaction()) {
            transaction
                    .setPosition(
                            mNode,
                            localPose.getTranslation().getX(),
                            localPose.getTranslation().getY(),
                            localPose.getTranslation().getZ())
                    .setOrientation(
                            mNode,
                            localPose.getRotation().getX(),
                            localPose.getRotation().getY(),
                            localPose.getRotation().getZ(),
                            localPose.getRotation().getW())
                    .apply();
        }
    }

    @Override
    public void setScale(@NonNull Vector3 scale, @SpaceValue int relativeTo) {
        super.setScale(scale, relativeTo);
        Vector3 localScale = super.getScale(Space.PARENT);
        try (NodeTransaction transaction = mExtensions.createNodeTransaction()) {
            transaction
                    .setScale(mNode, localScale.getX(), localScale.getY(), localScale.getZ())
                    .apply();
        }
    }

    /** Returns the pose for this entity, relative to the activity space root. */
    @Override
    public Pose getPoseInActivitySpace() {
        // TODO: b/355680575 - Revisit if we need to account for parent rotation when calculating
        // the
        // scale. This code might produce unexpected results when non-uniform scale is involved in
        // the
        // parent-child entity hierarchy.

        // Any parentless "space" entities (such as the root and anchor entities) are expected to
        // override this method non-recursively so that this error is never thrown.
        if (!(getParent() instanceof AndroidXrEntity)) {
            throw new IllegalStateException(
                    "Cannot get pose in Activity Space with a non-AndroidXrEntity parent");
        }
        AndroidXrEntity xrParent = (AndroidXrEntity) getParent();
        return xrParent.getPoseInActivitySpace()
                .compose(
                        new Pose(
                                getPose(Space.PARENT)
                                        .getTranslation()
                                        .times(xrParent.getActivitySpaceScale()),
                                getPose(Space.PARENT).getRotation()));
    }

    private Pose getPoseInPerceptionSpace() {
        PerceptionSpaceActivityPose perceptionSpaceActivityPose =
                mEntityManager
                        .getSystemSpaceActivityPoseOfType(PerceptionSpaceActivityPose.class)
                        .get(0);
        return transformPoseTo(new Pose(), perceptionSpaceActivityPose);
    }

    private Pose getLocalPoseForActivitySpacePose(Pose pose) {
        if (!(getParent() instanceof AndroidXrEntity)) {
            throw new IllegalStateException(
                    "Cannot get pose in Activity Space with a non-AndroidXrEntity parent");
        }
        AndroidXrEntity xrParent = (AndroidXrEntity) getParent();
        ActivitySpace activitySpace =
                mEntityManager.getSystemSpaceActivityPoseOfType(ActivitySpace.class).get(0);
        return activitySpace.transformPoseTo(pose, xrParent);
    }

    private Pose getLocalPoseForPerceptionSpacePose(Pose pose) {
        if (!(getParent() instanceof AndroidXrEntity)) {
            throw new IllegalStateException(
                    "Cannot get pose in Activity Space with a non-AndroidXrEntity parent");
        }
        AndroidXrEntity xrParent = (AndroidXrEntity) getParent();
        PerceptionSpaceActivityPose perceptionSpaceActivityPose =
                mEntityManager
                        .getSystemSpaceActivityPoseOfType(PerceptionSpaceActivityPose.class)
                        .get(0);
        return perceptionSpaceActivityPose.transformPoseTo(pose, xrParent);
    }

    // Returns the underlying extension Node for the Entity.
    public Node getNode() {
        return mNode;
    }

    @Override
    public void setParent(Entity parent) {
        if ((parent != null) && !(parent instanceof AndroidXrEntity)) {
            Log.e(
                    "RealityCoreRuntime",
                    "Cannot set non-AndroidXrEntity as a parent of a AndroidXrEntity");
            return;
        }
        super.setParent(parent);

        AndroidXrEntity xrParent = (AndroidXrEntity) parent;

        try (NodeTransaction transaction = mExtensions.createNodeTransaction()) {
            if (xrParent == null) {
                NodeTransaction unused =
                        transaction.setVisibility(mNode, false).setParent(mNode, null);
            } else {
                NodeTransaction unused = transaction.setParent(mNode, xrParent.getNode());
            }
            transaction.apply();
        }
    }

    @Override
    public void setAlpha(float alpha, @SpaceValue int relativeTo) {
        super.setAlpha(alpha, relativeTo);
        try (NodeTransaction transaction = mExtensions.createNodeTransaction()) {
            transaction.setAlpha(mNode, super.getAlpha(relativeTo)).apply();
        }
    }

    @Override
    public void setHidden(boolean hidden) {
        super.setHidden(hidden);

        try (NodeTransaction transaction = mExtensions.createNodeTransaction()) {
            if (mReformOptions != null) {
                if (hidden) {
                    // Since this entity is being hidden, disable reform and the highlights around
                    // the node.
                    NodeTransaction unused = transaction.disableReform(mNode);
                } else {
                    // Enables reform and the highlights around the node.
                    NodeTransaction unused = transaction.enableReform(mNode, mReformOptions);
                }
            }
            transaction.setVisibility(mNode, !hidden).apply();
        }
    }

    @Override
    public void addInputEventListener(
            @NonNull Executor executor, @NonNull InputEventListener eventListener) {
        maybeSetupInputListeners();
        mInputEventListenerMap.put(eventListener, executor == null ? mExecutor : executor);
    }

    /**
     * Request pointer capture for this Entity, using the given interfaces to propagate state and
     * captured input.
     *
     * <p>Returns true if a new pointer capture session was requested. Returns false if there is
     * already a previously existing pointer capture session as only one can be supported at a given
     * time.
     */
    public boolean requestPointerCapture(
            Executor executor,
            InputEventListener eventListener,
            PointerCaptureComponent.StateListener stateListener) {
        if (mPointerCaptureInputEventListener.isPresent()) {
            return false;
        }
        getNode()
                .requestPointerCapture(
                        (pcState) -> {
                            if (pcState == Node.POINTER_CAPTURE_STATE_PAUSED) {
                                stateListener.onStateChanged(
                                        PointerCaptureComponent.POINTER_CAPTURE_STATE_PAUSED);
                            } else if (pcState == Node.POINTER_CAPTURE_STATE_ACTIVE) {
                                stateListener.onStateChanged(
                                        PointerCaptureComponent.POINTER_CAPTURE_STATE_ACTIVE);
                            } else if (pcState == Node.POINTER_CAPTURE_STATE_STOPPED) {
                                stateListener.onStateChanged(
                                        PointerCaptureComponent.POINTER_CAPTURE_STATE_STOPPED);
                            } else {
                                Log.e("Runtime", "Invalid state received for pointer capture");
                            }
                        },
                        executor);

        addPointerCaptureInputListener(executor, eventListener);
        return true;
    }

    private void addPointerCaptureInputListener(
            Executor executor, InputEventListener eventListener) {
        maybeSetupInputListeners();
        mPointerCaptureInputEventListener = Optional.of(eventListener);
        mPointerCaptureExecutor = Optional.ofNullable(executor);
    }

    private void maybeSetupInputListeners() {
        if (mInputEventListenerMap.isEmpty() && mPointerCaptureInputEventListener.isEmpty()) {
            mNode.listenForInput(
                    (xrInputEvent) -> {
                        if (xrInputEvent.getDispatchFlags()
                                == InputEvent.DISPATCH_FLAG_CAPTURED_POINTER) {
                            mPointerCaptureInputEventListener.ifPresent(
                                    (listener) ->
                                            mPointerCaptureExecutor
                                                    .orElse(mExecutor)
                                                    .execute(
                                                            () ->
                                                                    listener.onInputEvent(
                                                                            RuntimeUtils
                                                                                    .getInputEvent(
                                                                                            xrInputEvent,
                                                                                            mEntityManager))));
                        } else {
                            mInputEventListenerMap.forEach(
                                    (inputEventListener, listenerExecutor) ->
                                            listenerExecutor.execute(
                                                    () ->
                                                            inputEventListener.onInputEvent(
                                                                    RuntimeUtils.getInputEvent(
                                                                            xrInputEvent,
                                                                            mEntityManager))));
                        }
                    },
                    mExecutor);
        }
    }

    @Override
    public void removeInputEventListener(@NonNull InputEventListener consumer) {
        mInputEventListenerMap.remove(consumer);
        maybeStopListeningForInput();
    }

    /** Stop any pointer capture requests on this Entity. */
    public void stopPointerCapture() {
        getNode().stopPointerCapture();
        mPointerCaptureInputEventListener = Optional.empty();
        mPointerCaptureExecutor = Optional.empty();
        maybeStopListeningForInput();
    }

    private void maybeStopListeningForInput() {
        if (mInputEventListenerMap.isEmpty() && mPointerCaptureInputEventListener.isEmpty()) {
            mNode.stopListeningForInput();
        }
    }

    @Override
    public void dispose() {
        mInputEventListenerMap.clear();
        mNode.stopListeningForInput();
        mReformEventConsumerMap.clear();
        try (NodeTransaction transaction = mExtensions.createNodeTransaction()) {
            NodeTransaction unused = transaction.disableReform(mNode);
        }

        // SystemSpaceEntityImpls (Anchors, ActivitySpace, etc) should have null parents.
        if (getParent() != null) {
            setParent(null);
        }
        mEntityManager.removeEntityForNode(mNode);
        super.dispose();
    }

    /**
     * Gets the reform options for this entity.
     *
     * @return The reform options for this entity.
     */
    public ReformOptions getReformOptions() {
        if (mReformOptions == null) {
            Consumer<ReformEvent> reformEventConsumer =
                    reformEvent -> {
                        if ((mReformOptions.getEnabledReform() & ReformOptions.ALLOW_MOVE) != 0
                                && (mReformOptions.getFlags()
                                                & ReformOptions.FLAG_ALLOW_SYSTEM_MOVEMENT)
                                        != 0) {
                            // Update the cached pose of the entity.
                            super.setPose(
                                    new Pose(
                                            new Vector3(
                                                    reformEvent.getProposedPosition().x,
                                                    reformEvent.getProposedPosition().y,
                                                    reformEvent.getProposedPosition().z),
                                            new Quaternion(
                                                    reformEvent.getProposedOrientation().x,
                                                    reformEvent.getProposedOrientation().y,
                                                    reformEvent.getProposedOrientation().z,
                                                    reformEvent.getProposedOrientation().w)),
                                    Space.PARENT);
                            // Update the cached scale of the entity.
                            super.setScaleInternal(
                                    new Vector3(
                                            reformEvent.getProposedScale().x,
                                            reformEvent.getProposedScale().y,
                                            reformEvent.getProposedScale().z));
                        }
                        mReformEventConsumerMap.forEach(
                                (eventConsumer, consumerExecutor) ->
                                        consumerExecutor.execute(
                                                () -> eventConsumer.accept(reformEvent)));
                    };
            mReformOptions = mExtensions.createReformOptions(reformEventConsumer, mExecutor);
        }
        return mReformOptions;
    }

    /**
     * Updates the reform options for this entity. Uses the same instance of [ReformOptions]
     * provided by {@link #getReformOptions()}.
     */
    public synchronized void updateReformOptions() {
        try (NodeTransaction transaction = mExtensions.createNodeTransaction()) {
            if (mReformOptions.getEnabledReform() == 0) {
                // Disables reform and the highlights around the node.
                NodeTransaction unused = transaction.disableReform(mNode);
            } else {
                // Enables reform and the highlights around the node.
                NodeTransaction unused = transaction.enableReform(mNode, mReformOptions);
            }
            transaction.apply();
        }
    }

    public void addReformEventConsumer(
            Consumer<ReformEvent> reformEventConsumer, Executor executor) {
        executor = (executor == null) ? mExecutor : executor;
        mReformEventConsumerMap.put(reformEventConsumer, executor);
    }

    public void removeReformEventConsumer(Consumer<ReformEvent> reformEventConsumer) {
        mReformEventConsumerMap.remove(reformEventConsumer);
    }
}
