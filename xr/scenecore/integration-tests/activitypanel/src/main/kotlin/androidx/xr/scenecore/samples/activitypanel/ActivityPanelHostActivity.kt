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

package androidx.xr.scenecore.samples.activitypanel

import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.ActivityPanelEntity
import androidx.xr.scenecore.Dimensions
import androidx.xr.scenecore.MovableComponent
import androidx.xr.scenecore.Session
import androidx.xr.scenecore.SpatialCapabilities
import androidx.xr.scenecore.getSpatialCapabilities
import androidx.xr.scenecore.samples.commontestview.CommonTestView

const val TAG = "ActivityPanelTest"

class ActivityPanelHostActivity : AppCompatActivity() {
    private lateinit var activityPanelEntity: ActivityPanelEntity
    private val session by lazy { Session.create(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activityPanelEntity =
            ActivityPanelEntity.create(session, Rect(0, 0, 1280, 800), "test_activity_panel")
        if (
            session
                .getSpatialCapabilities()
                .hasCapability(SpatialCapabilities.SPATIAL_CAPABILITY_EMBED_ACTIVITY)
        ) {
            val intent = Intent(this, CounterActivity::class.java)
            activityPanelEntity.launchActivity(intent)
            activityPanelEntity.setPose(Pose(Vector3(0.5f, 0.5f, 0.0f)))
            val movableComponent = MovableComponent.create(session)
            movableComponent.size = getSizeInLocalSpace(activityPanelEntity)
            @Suppress("UNUSED_VARIABLE")
            val unused = activityPanelEntity.addComponent(movableComponent)
        }
        setContentView(CommonTestView(this))
    }

    private fun getSizeInLocalSpace(activityPanelEntity: ActivityPanelEntity): Dimensions {
        val scaledSize = activityPanelEntity.getSize()
        val worldSpaceScale = activityPanelEntity.getWorldSpaceScale()
        return Dimensions(
            scaledSize.width / worldSpaceScale,
            scaledSize.height / worldSpaceScale,
            scaledSize.depth / worldSpaceScale,
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        activityPanelEntity.setParent(null)
        activityPanelEntity.dispose()
    }
}
