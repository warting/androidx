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

package androidx.camera.testing.impl.fakes;

import static androidx.camera.core.CameraUnavailableException.CAMERA_ERROR;

import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import androidx.camera.core.CameraIdentifier;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.CameraUnavailableException;
import androidx.camera.core.Logger;
import androidx.camera.core.concurrent.CameraCoordinator;
import androidx.camera.core.impl.CameraFactory;
import androidx.camera.core.impl.CameraInternal;
import androidx.camera.core.impl.ConstantObservable;
import androidx.camera.core.impl.Observable;
import androidx.core.util.Pair;
import androidx.core.util.Preconditions;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

/**
 * A {@link CameraFactory} implementation that contains and produces fake cameras.
 *
 */
@RestrictTo(Scope.LIBRARY_GROUP)
public final class FakeCameraFactory implements CameraFactory {

    private static final String TAG = "FakeCameraFactory";

    private @Nullable Set<String> mCachedCameraIds;

    private final @Nullable CameraSelector mAvailableCamerasSelector;

    private @Nullable Object mCameraManager = null;

    private @NonNull CameraCoordinator mCameraCoordinator = new FakeCameraCoordinator();

    private @NonNull Observable<List<CameraIdentifier>> mCameraSourceObservable =
            ConstantObservable.withValue(new ArrayList<>());

    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    final Map<String, Pair<Integer, Callable<CameraInternal>>> mCameraMap = new HashMap<>();

    public FakeCameraFactory() {
        mAvailableCamerasSelector = null;
    }

    public FakeCameraFactory(@Nullable CameraSelector availableCamerasSelector) {
        mAvailableCamerasSelector = availableCamerasSelector;
    }

    @Override
    public @NonNull CameraInternal getCamera(@NonNull String cameraId)
            throws CameraUnavailableException {
        Pair<Integer, Callable<CameraInternal>> cameraPair = mCameraMap.get(cameraId);
        if (cameraPair != null) {
            try {
                Callable<CameraInternal> cameraCallable = Preconditions.checkNotNull(
                        cameraPair.second);
                return cameraCallable.call();
            } catch (Throwable t) {
                throw new CameraUnavailableException(CAMERA_ERROR, t);
            }
        }
        throw new IllegalArgumentException("Unknown camera: " + cameraId);
    }

    /**
     * Inserts a {@link Callable} for creating cameras with the given camera ID.
     *
     * @param cameraId       Identifier to use for the camera.
     * @param cameraInternal Callable used to provide the Camera implementation.
     */
    public void insertCamera(@CameraSelector.LensFacing int lensFacing, @NonNull String cameraId,
            @NonNull Callable<CameraInternal> cameraInternal) {
        // Invalidate caches
        mCachedCameraIds = null;

        mCameraMap.put(cameraId, Pair.create(lensFacing, cameraInternal));
    }

    /**
     * Inserts a camera and sets it as the default front camera.
     *
     * <p>This is a convenience method for calling
     * {@link #insertCamera(int, String, Callable)} with
     * {@link CameraSelector#LENS_FACING_FRONT} for all lens facing arguments.
     *
     * @param cameraId       Identifier to use for the front camera.
     * @param cameraInternal Camera implementation.
     */
    public void insertDefaultFrontCamera(@NonNull String cameraId,
            @NonNull Callable<CameraInternal> cameraInternal) {
        insertCamera(CameraSelector.LENS_FACING_FRONT, cameraId, cameraInternal);
    }

    /**
     * Inserts a camera and sets it as the default back camera.
     *
     * <p>This is a convenience method for calling
     * {@link #insertCamera(int, String, Callable)} with
     * {@link CameraSelector#LENS_FACING_BACK} for all lens facing arguments.
     *
     * @param cameraId       Identifier to use for the back camera.
     * @param cameraInternal Camera implementation.
     */
    public void insertDefaultBackCamera(@NonNull String cameraId,
            @NonNull Callable<CameraInternal> cameraInternal) {
        insertCamera(CameraSelector.LENS_FACING_BACK, cameraId, cameraInternal);
    }

    /**
     * Removes a camera with the given camera ID.
     *
     * <p>Subsequent calls to {@link #getAvailableCameraIds()} will no longer include this camera,
     * and {@link #getCamera(String)} will throw an {@link IllegalArgumentException} for it.
     *
     * @param cameraId Identifier of the camera to remove.
     * @return The {@link Callable} that was associated with the removed camera, or {@code null}
     * if the camera was not found.
     */
    public @Nullable Callable<CameraInternal> removeCamera(@NonNull String cameraId) {
        // Invalidate caches
        mCachedCameraIds = null;

        // Remove from the map and return the old value.
        Pair<Integer, Callable<CameraInternal>> removed = mCameraMap.remove(cameraId);
        if (removed != null) {
            return removed.second;
        }
        return null; // Not found
    }

    @Override
    public @NonNull Set<String> getAvailableCameraIds() {
        // Lazily cache the set of all camera ids. This cache will be invalidated anytime a new
        // camera is added.
        if (mCachedCameraIds == null) {
            if (mAvailableCamerasSelector == null) {
                mCachedCameraIds = Collections.unmodifiableSet(new HashSet<>(mCameraMap.keySet()));
            } else {
                mCachedCameraIds = Collections.unmodifiableSet(new HashSet<>(filteredCameraIds()));
            }
        }
        return mCachedCameraIds;
    }

    /** Returns a list of camera ids filtered with {@link #mAvailableCamerasSelector}. */
    private @NonNull List<String> filteredCameraIds() {
        Preconditions.checkNotNull(mAvailableCamerasSelector);
        final List<String> filteredCameraIds = new ArrayList<>();
        for (Map.Entry<String, Pair<Integer, Callable<CameraInternal>>> entry :
                mCameraMap.entrySet()) {
            final Callable<CameraInternal> callable = entry.getValue().second;
            if (callable == null) {
                continue;
            }
            try {
                final CameraInternal camera = callable.call();
                LinkedHashSet<CameraInternal> filteredCameraInternals =
                        mAvailableCamerasSelector.filter(
                                new LinkedHashSet<>(Collections.singleton(camera)));
                if (!filteredCameraInternals.isEmpty()) {
                    filteredCameraIds.add(entry.getKey());
                }
            } catch (Exception exception) {
                Logger.e(TAG, "Failed to get access to the camera instance.", exception);
            }
        }
        return filteredCameraIds;
    }

    @Override
    public @NonNull CameraCoordinator getCameraCoordinator() {
        return mCameraCoordinator;
    }

    public void setCameraCoordinator(@NonNull CameraCoordinator cameraCoordinator) {
        mCameraCoordinator = cameraCoordinator;
    }

    public void setCameraManager(@Nullable Object cameraManager) {
        mCameraManager = cameraManager;
    }

    @Override
    public @Nullable Object getCameraManager() {
        return mCameraManager;
    }

    @Override
    public @NonNull Observable<List<CameraIdentifier>> getCameraPresenceSource() {
        return mCameraSourceObservable;
    }

    public void setCameraPresenceSource(
            @NonNull Observable<List<CameraIdentifier>> cameraSourceObservable) {
        mCameraSourceObservable = cameraSourceObservable;
    }

    @Override
    public void onCameraIdsUpdated(@NonNull List<String> cameraIds) {

    }
}
