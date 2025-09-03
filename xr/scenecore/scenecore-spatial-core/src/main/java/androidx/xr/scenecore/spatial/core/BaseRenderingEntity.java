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

import android.content.Context;

import androidx.xr.runtime.NodeHolder;
import androidx.xr.scenecore.internal.RenderingFeature;

import com.android.extensions.xr.XrExtensions;
import com.android.extensions.xr.node.Node;

import java.util.concurrent.ScheduledExecutorService;

/**
 * A base class for entities that rely on a [RenderingFeature] for rendering functionality.
 *
 * <p>This class provides common logic for managing the lifecycle of a [RenderingFeature], which
 * supplies the underlying rendering implementation and provides a [Node] for initialization.
 */
abstract class BaseRenderingEntity extends AndroidXrEntity {
    private final RenderingFeature mFeature;

    BaseRenderingEntity(
            Context context,
            RenderingFeature feature,
            XrExtensions extensions,
            EntityManager entityManager,
            ScheduledExecutorService executor) {
        super(
                context,
                NodeHolder.assertGetValue(feature.getNodeHolder(), Node.class),
                extensions,
                entityManager,
                executor);
        mFeature = feature;
    }

    @Override
    public void dispose() {
        mFeature.dispose();
        super.dispose();
    }
}
