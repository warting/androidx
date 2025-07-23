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
import android.util.Log;

import androidx.annotation.RestrictTo;

import org.jspecify.annotations.NonNull;

import java.lang.ref.ReferenceQueue;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Manages the lifecycle of Impress objects by hooking into the JVM Garbage Collector using
 * PhantomReference objects.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public class BindingsResourceManager {
    private final String TAG = getClass().getSimpleName();
    private static final String RESOURCE_MANAGER_THREAD = "resource_manager_thread";
    private final Handler mainThreadHandler;
    private final ReferenceQueue<Object> queue = new ReferenceQueue<>();
    private final Set<BindingsObjectPhantomReference> phantomReferences =
            Collections.synchronizedSet(new HashSet<>());

    public BindingsResourceManager(@NonNull Handler mainThreadHandler) {
        this.mainThreadHandler = mainThreadHandler;
        Thread thread = new Thread(this::processQueue, RESOURCE_MANAGER_THREAD);
        thread.setDaemon(true);
        thread.start();
    }

    /**
     * Registers the resource object's PhantomReference in the BindingsResourceManager.
     *
     * @param object The resource object that is getting registered.
     * @param callback The callback that gets triggered when the object is garbage collected.
     */
    public void register(@NonNull Object object, @NonNull Runnable callback) {
        phantomReferences.add(new BindingsObjectPhantomReference(object, queue, callback));
    }

    private void processQueue() {
        while (true) {
            try {
                BindingsObjectPhantomReference ref =
                        (BindingsObjectPhantomReference) queue.remove();
                mainThreadHandler.post(
                        () -> {
                            ref.cleanup();
                            phantomReferences.remove(ref);
                        });
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                Log.w(TAG, "Queue processing thread was interrupted and is now terminating.");
                break;
            }
        }
    }
}
