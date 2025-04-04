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

package androidx.xr.compose.subspace

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisallowComposableCalls
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.xr.compose.platform.LocalSession
import androidx.xr.compose.platform.disposableValueOf
import androidx.xr.compose.platform.getValue
import androidx.xr.compose.subspace.layout.CoreContentlessEntity
import androidx.xr.compose.subspace.layout.CoreMainPanelEntity
import androidx.xr.compose.subspace.layout.CorePanelEntity
import androidx.xr.compose.subspace.layout.CoreSurfaceEntity
import androidx.xr.compose.subspace.layout.SpatialShape
import androidx.xr.runtime.Session
import androidx.xr.scenecore.Entity
import androidx.xr.scenecore.PanelEntity
import androidx.xr.scenecore.SurfaceEntity

/**
 * Creates a [CoreContentlessEntity] that is automatically disposed of when it leaves the
 * composition.
 */
@Composable
@PublishedApi
internal fun rememberCoreContentlessEntity(
    entityFactory: @DisallowComposableCalls Session.() -> Entity
): CoreContentlessEntity {
    val session = checkNotNull(LocalSession.current) { "session must be initialized" }
    val coreEntity by remember {
        disposableValueOf(CoreContentlessEntity(session.entityFactory())) { it.dispose() }
    }
    return coreEntity
}

/** Creates a [CorePanelEntity] that is automatically disposed of when it leaves the composition. */
@Composable
internal inline fun rememberCorePanelEntity(
    shape: SpatialShape = SpatialPanelDefaults.shape,
    crossinline entityFactory: @DisallowComposableCalls Session.() -> PanelEntity,
): CorePanelEntity {
    val session = checkNotNull(LocalSession.current) { "session must be initialized" }
    val density = LocalDensity.current
    val coreEntity by remember {
        disposableValueOf(
            CorePanelEntity(session.entityFactory(), density).also { it.shape = shape }
        ) {
            it.dispose()
        }
    }
    LaunchedEffect(shape) { coreEntity.shape = shape }
    return coreEntity
}

/**
 * Creates a [CoreMainPanelEntity] that is automatically disposed of when it leaves the composition.
 */
@Composable
internal fun rememberCoreMainPanelEntity(
    shape: SpatialShape = SpatialPanelDefaults.shape
): CoreMainPanelEntity {
    val session = checkNotNull(LocalSession.current) { "session must be initialized" }
    val density = LocalDensity.current
    val coreEntity by remember {
        disposableValueOf(CoreMainPanelEntity(session, density).also { it.shape = shape }) {
            it.dispose()
        }
    }
    LaunchedEffect(shape) { coreEntity.shape = shape }
    return coreEntity
}

/**
 * Creates a [CoreSurfaceEntity] that is automatically disposed of when it leaves the composition.
 */
@Composable
internal inline fun rememberCoreSurfaceEntity(
    crossinline entityFactory: @DisallowComposableCalls Session.() -> SurfaceEntity
): CoreSurfaceEntity {
    val session = checkNotNull(LocalSession.current) { "session must be initialized" }
    val density = LocalDensity.current
    val coreEntity by remember {
        disposableValueOf(CoreSurfaceEntity(session.entityFactory(), density)) { it.dispose() }
    }
    return coreEntity
}

private var entityNamePart: Int = 0

/**
 * Creates a unique debugging name for an [Entity].
 *
 * @param name A context-specific name for the [Entity].
 */
@PublishedApi
internal fun entityName(name: String): String {
    return "$name-${entityNamePart++}"
}
