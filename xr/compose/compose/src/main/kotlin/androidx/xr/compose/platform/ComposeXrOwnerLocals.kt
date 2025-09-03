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

package androidx.xr.compose.platform

import android.app.Activity
import android.view.View
import androidx.annotation.VisibleForTesting
import androidx.compose.runtime.CompositionLocal
import androidx.compose.runtime.compositionLocalWithComputedDefaultOf
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.xr.compose.R
import androidx.xr.compose.subspace.layout.CoreMainPanelEntity
import androidx.xr.runtime.Session
import androidx.xr.runtime.SessionCreateSuccess
import androidx.xr.scenecore.Entity
import androidx.xr.scenecore.GroupEntity
import androidx.xr.scenecore.scene

/**
 * Provides a the current [ComposeXrOwnerLocals], a storage container for singletons tied to the
 * current [Activity].
 */
internal val LocalComposeXrOwners: CompositionLocal<ComposeXrOwnerLocals?> =
    compositionLocalWithComputedDefaultOf {
        val activity = LocalContext.currentValue.getActivity()
        if (activity != null && LocalIsXrEnabled.currentValue) {
            activity.window.decorView.getOrCreateXrOwnerLocals(
                activity = activity,
                sessionFactory = LocalSessionFactory.currentValue,
            )
        } else {
            null
        }
    }

@VisibleForTesting
internal val LocalIsXrEnabled = compositionLocalWithComputedDefaultOf {
    SpatialConfiguration.hasXrSpatialFeature(LocalContext.currentValue.requireActivity())
}

@VisibleForTesting
internal val LocalSessionFactory = compositionLocalWithComputedDefaultOf {
    {
        (Session.create(LocalContext.currentValue.requireActivity()) as? SessionCreateSuccess)
            ?.session
    }
}

/**
 * A storage container for singletons tied to the current [Activity].
 *
 * This will be stored within the decorView of the main window.
 */
internal class ComposeXrOwnerLocals(
    val session: Session?,
    val spatialConfiguration: SpatialConfiguration,
    val spatialCapabilities: SpatialCapabilities,
    val coreMainPanelEntity: CoreMainPanelEntity,
    val subspaceRootNode: Entity,
    val dialogManager: DialogManager,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ComposeXrOwnerLocals

        if (session != other.session) return false
        if (spatialConfiguration != other.spatialConfiguration) return false
        if (spatialCapabilities != other.spatialCapabilities) return false
        if (coreMainPanelEntity != other.coreMainPanelEntity) return false
        if (subspaceRootNode != other.subspaceRootNode) return false

        return true
    }

    override fun hashCode(): Int {
        var result = session?.hashCode() ?: 0
        result = 31 * result + spatialConfiguration.hashCode()
        result = 31 * result + spatialCapabilities.hashCode()
        result = 31 * result + coreMainPanelEntity.hashCode()
        result = 31 * result + subspaceRootNode.hashCode()
        return result
    }
}

@VisibleForTesting
internal fun View.getOrCreateXrOwnerLocals(
    activity: Activity,
    sessionFactory: () -> Session?,
): ComposeXrOwnerLocals? = getXrOwnerLocals() ?: createXrOwnerLocals(activity, sessionFactory)

@VisibleForTesting
internal fun View.getXrOwnerLocals(): ComposeXrOwnerLocals? =
    getTag(R.id.compose_xr_owner_locals) as? ComposeXrOwnerLocals

private fun View.createXrOwnerLocals(
    activity: Activity,
    sessionFactory: () -> Session?,
): ComposeXrOwnerLocals? {
    val session = sessionFactory() ?: return null

    // When the owning lifecycle is destroyed, clear the cached  `ComposeXrOwnerLocals` from the
    // View's tag. This is critical to prevent crashes from using a stale `Session` after Activity
    // recreation, as `Session` lifecycle is currently tied to the lifecycle of an activity so that
    // it forces a fresh `Session` instance to be created on next access.
    if (activity is LifecycleOwner) {
        activity.lifecycle.addObserver(
            object : DefaultLifecycleObserver {
                override fun onDestroy(owner: LifecycleOwner) {
                    setTag(R.id.compose_xr_owner_locals, null)
                    owner.lifecycle.removeObserver(this)
                }
            }
        )
    }

    return ComposeXrOwnerLocals(
            session = session,
            spatialConfiguration = SessionSpatialConfiguration(session),
            spatialCapabilities = SessionSpatialCapabilities(session),
            coreMainPanelEntity = CoreMainPanelEntity(session),
            subspaceRootNode =
                GroupEntity.create(session, "SubspaceRootContainer").apply {
                    session.scene.keyEntity = this
                },
            dialogManager = DefaultDialogManager(),
        )
        .also { setTag(R.id.compose_xr_owner_locals, it) }
}
